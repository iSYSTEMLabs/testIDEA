package si.isystem.itest.launch;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.ETristate;
import si.isystem.connect.connect;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.handlers.TestSaveTestReportCmdHandler;
import si.isystem.itest.launch.FilesListTableModel.ETestInitSource;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.TestRunner;

public class TestIDEALaunchDelegate implements ILaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration,
                       String mode,
                       ILaunch launch,
                       IProgressMonitor monitor) throws CoreException {
        
        // System.out.println("Launching tests!");
        boolean isDebug = false;
        if (mode.equals("debug")) {
            isDebug = true;
        }
        
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                StatusModel.instance().clear();
            }});
        
        String rootName = configuration.getAttribute(FileListTab.ATTR_IYAML_FILE_ROOT, 
                                                     FileListTab.WORKSPACE_ROOT);
        
        List<String> fileList = configuration.getAttribute(FileListTab.ATTR_YAML_FILE_LIST, 
                                                           new ArrayList<>());
        
        List<String> initSrcList = configuration.getAttribute(FileListTab.ATTR_TEST_INIT_SOURCE, 
                                                              new ArrayList<>());

        boolean isOpenReportInBrowser = configuration.getAttribute(FileListTab.ATTR_IS_OPEN_REPORT_IN_BROWSER, 
                                                                   false);

        boolean isAbsPath = FileListTab.isAbsPaths(rootName);
        boolean isWorkspaceRoot = FileListTab.isWorkspaceRoot(rootName);

        CTestEnvironmentConfig launchRunConfig = new CTestEnvironmentConfig(null);
        boolean isAlwaysRunInitSeq = TestConfigurationTab.isAlwaysRunInitSeq(launchRunConfig, configuration);
        launchRunConfig.setAlwaysRunInitSeqBeforeRun(isAlwaysRunInitSeq ? ETristate.E_TRUE : ETristate.E_FALSE);
        TestConfigurationTab.setInitSeqFromConfig(launchRunConfig, configuration);
        
        int idx = 0;
        for (String fileName : fileList) {
            String initSrc = initSrcList.get(idx);
            ETestInitSource initSrcE = ETestInitSource.valueOf(initSrc);
            
            String file = getAbsPath(fileName, 
                                     (isAbsPath || isWorkspaceRoot) ? null : rootName);
            execTests(file, initSrcE, launchRunConfig, isOpenReportInBrowser, 
                      isDebug, monitor);
        }
    }

    
    private void execTests(final String modelFileName, 
                           ETestInitSource initSrcE,
                           CTestEnvironmentConfig launchRunConfig, 
                           boolean isOpenReportInBrowser,
                           boolean isDebug, 
                           IProgressMonitor monitor) throws CoreException {
        
        CTestSpecification testSpec = null;
        
        try {
            final CTestBench testBench = CTestBench.load(modelFileName);
            testSpec = testBench.getTestSpecification(true);
            // run config may be modified, because it will not be saved
            CTestEnvironmentConfig envConfig = testBench.getTestEnvironmentConfig(false);
            
            switch (initSrcE) {
            case EFile:
                // do nothing, run Config is already as loaded from file
                break;
            case ELaunchConfig:
                copyRunConfig(launchRunConfig, envConfig);
                break;
            case ENoInit:
                clearRunConfig(envConfig);
                break;
            default:
                throw new SIllegalStateException("Unknown enum value!").
                                             add("value", initSrcE);
            }

            TestRunner runner = new TestRunner();
            runner.runTestHeadless(envConfig,
                                   testBench,
                                   modelFileName,
                                   testSpec,
                                   Integer.MAX_VALUE,  // run also derived tests
                                   isDebug,
                                   monitor);
            
            monitor.worked(1);
            
            final CTestReportContainer resultsMap = runner.getResultsMap();
            
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        setTestResultsInActiveEditors(modelFileName, testBench);
                        StatusModel.instance().appendFileResults(modelFileName, resultsMap);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        StatusModel.instance().appendFileMessage(modelFileName, 
                                                                StatusType.ERROR,
                                                                SEFormatter.getInfoWithStackTrace(ex, 10));
                    }
                }});

            saveResultsToReportFile(testBench, 
                                    isOpenReportInBrowser);
            
        } catch (Exception ex) {
            if (testSpec == null) {
                testSpec = new CTestSpecification();    // dummy test case 
                testSpec.setTestId("testSpec == null"); // just for support info
            }
            
            String message = SEFormatter.getInfoWithStackTrace(ex, 30);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, ex));
            
            /* throw new SIOException("Can not execute test!\n  Test ID: " + 
                                  testSpec.getTestId() + 
                                  "\n  Function: '" + testSpec.getFunctionUnderTest(true).getName() + "'", ex);
                                  */
        }
    }


    private void setTestResultsInActiveEditors(String modelFileName,
                                               CTestBench testBench) 
                                                       throws URISyntaxException,
                                                              CoreException {
        
        IEditorPart editor = UiUtils.findAndActivateEditorForFile(modelFileName);
        boolean isMissingTCAlreadyReported = false;
        
        if (editor != null) {
            if (editor instanceof TestCaseEditorPart) {
                TestCaseEditorPart tcEditor = (TestCaseEditorPart) editor;
                // keepTestResults is not respected here - if user wants to 
                // keep results from previous run, he has to run test manually.
                TestSpecificationModel editorModel = tcEditor.getModel();
                // Map<Long, CTestResult> modelResultsMap = new TreeMap<>();
                
                CTestReportContainer runResults = testBench.getTestReportContainer();
                CTestReportContainer editorResults = editorModel.getTestReportContainer();
                editorResults.clearResults();
                
                runResults.resetTestResultIterator();
                while (runResults.hasNextTestResult()) {
                    
                    CTestResult result = runResults.nextTestResult();
                    CTestSpecification mergedTestSpec = result.getTestSpecification();
                    CTestSpecification parentTestSpec = mergedTestSpec.getParentTestSpecification();
                    
                    CTestSpecification modelTestSpec = editorModel.findTestSpecEqualsData(parentTestSpec);
                    
                    if (modelTestSpec == parentTestSpec) {
                        SExceptionDialog.open(Activator.getShell(), 
                                              "Can't match test case from file to test case from "
                                              + "editor, to many equal test cases found: " + 
                                                      parentTestSpec.getTestId() +  " / " + 
                                                      parentTestSpec.getFunctionUnderTest(true).getName(), 
                                                      new Exception("Can't match test case from file to test case from "
                                                                  + "editor, to many equal test cases found. Make sure "
                                                                  + "testIDs are different for all test cases (run iTools | Verify Symbols), "
                                                                  + "and the file is saved."));
                        // preserve test spec for status view
                        editorResults.putTestResult(mergedTestSpec, result);
                        continue;
                    }
                    
                    if (modelTestSpec == null) {
                        // if there were many test case removed (let's say 100) it is
                        // NOT user friendly if this dialog displays 100 times.
                        if (!isMissingTCAlreadyReported) {
                            SExceptionDialog.open(Activator.getShell(), 
                                              "Can't match test case from file to test case from "
                                              + "editor, no test case found: " + 
                                              (parentTestSpec != null ? parentTestSpec.getTestId() : "null") +  " / " + 
                                              (parentTestSpec != null ? parentTestSpec.getFunctionUnderTest(true).getName() : "null"), 
                                              new Exception("Test case does not exist or is not equal in editor and file. "
                                                            + "Make sure your editor is saved."));
                            isMissingTCAlreadyReported = true;
                        }
                        // preserve test spec for status view
                        editorResults.putTestResult(mergedTestSpec, result);
                        continue;
                    }
                    
                    mergedTestSpec.setParent(modelTestSpec);
                    editorResults.putTestResult(modelTestSpec, result);
                }
                
                // if editor is found, replace all results with map keys indicating
                // objects from editor's model
                //? resultsMap.clear();
                //? resultsMap.putAll(modelResultsMap);

                TestRunner.sendNotifications(editorModel);
            }
        }
    }


    // copies init sequence from launchRunConfig to execRunConfig
    private void copyRunConfig(CTestEnvironmentConfig launchRunConfig,
                               CTestEnvironmentConfig execRunConfig) {
        
        execRunConfig.setAlwaysRunInitSeqBeforeRun(launchRunConfig.isAlwaysRunInitSeqBeforeRun() ? 
                                               ETristate.E_TRUE : ETristate.E_FALSE);
        int initSeqSection = EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue();

        CTestBaseList srcInitSeq = launchRunConfig.getTestBaseList(initSeqSection, false);
        CTestBaseList destInitSeq = execRunConfig.getTestBaseList(initSeqSection, false);
        
        destInitSeq.assign(srcInitSeq);
    }


    private void clearRunConfig(CTestEnvironmentConfig runConfig) {
        runConfig.setAlwaysRunInitSeqBeforeRun(ETristate.E_FALSE);
        CTestBaseList initSeq = 
                runConfig.getTestBaseList(EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                          true);
        initSeq.clear();
    }


    private void saveResultsToReportFile(CTestBench testBench,
                                         boolean isOpenReportBrowser) {

        try {
            final String modelFileName = testBench.getFileName();
            TestSaveTestReportCmdHandler handler = new TestSaveTestReportCmdHandler();
            CTestReportConfig reportConfig = new CTestReportConfig();
            CTestReportConfig originalReportConfig = testBench.getTestReportConfig(false);
            reportConfig.assign(originalReportConfig);
            
            // generate default report and template file names if not present in report config
            if (reportConfig.getFileName().isEmpty()) {
                final String reportFileName = UiUtils.replaceExtension(modelFileName, "xml");
                reportConfig.setFileName(reportFileName);
                
                if (reportConfig.getXsltForFullReport().isEmpty()) {
                    reportConfig.setXsltForFullReport(connect.getBUILT_IN_XSLT_PREFIX() + " " + 
                                                      connect.getDEFAULT_XSLT_NAME());
                }
                
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        StatusModel.instance().appendFileMessage(modelFileName, 
                                                                StatusTableLine.StatusType.WARNING,
                                                                "Report file name is not specified in report configuration (see Test | Save test report)!\n"
                                                                + "Report saved as " + reportFileName);
                    }});
            }
            
            handler.saveTestReportWithScripts(isOpenReportBrowser, 
                                              testBench.getTestReportContainer(), 
                                              reportConfig, 
                                              testBench.getTestEnvironmentConfig(true).getScriptConfig(true),
                                              modelFileName);
            
        } catch (Exception ex) {
            throw new SIOException("Can not save test report or open viewer!", ex);
        }
    }


    private String getAbsPath(String fileName, String projName) {
        
        if (fileName.charAt(1) == ':') {
            return fileName; // windows path with drive letter - it is definitely absolute
        }
            
        if (fileName.startsWith(FileListTab.WORKSPACE_LOC_VAR)) {
            String wsAbsPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
            return fileName.replace(FileListTab.WORKSPACE_LOC_VAR, wsAbsPath);
        }
        
        if (fileName.startsWith(FileListTab.PROJECT_LOC_VAR)) {

            if (projName != null) {
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
                String projAbsPath = project.getLocation().toOSString();
                return fileName.replace(FileListTab.PROJECT_LOC_VAR, projAbsPath);
            } else {
                // If abs paths or ws relative paths are selected it is beter to 
                // report an error than using currently selected project by default -
                // this may be to confusing. Use case may be to have the same iyaml 
                // file name in all projects, and then have one launch config 
                // for all of them - but this is much less likely scenario than 
                // confusion in case of currently selected project.
                //
                // IProject proj = UiUtils.getSelectedProjectInProjectExplorer();
                
                throw new SIllegalArgumentException("File with project relative path "
                        + "is found in launch configuration with workspace relative or "
                        + "absolute path").
                        add("fileName", fileName);
            }
        }
        
        return fileName;
    }
}
