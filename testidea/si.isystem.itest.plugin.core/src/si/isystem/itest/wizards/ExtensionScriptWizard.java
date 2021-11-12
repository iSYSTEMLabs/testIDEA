package si.isystem.itest.wizards;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestEvalAssignStep.EStepSectionIds;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResultBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestStub.EStubSectionIds;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.ISysPathFileUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.model.actions.testBaseList.InsertToTestBaseListAction;
import si.isystem.itest.run.Script;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This class implements wizard for script functions. There is one page class
 * ExtScriptMethodPage for all script methods. Script methods for stubs and 
 * test points may contain hit counters, in which case testInit script method
 * initializes the counters. 
 * 
 * @author markok
 */
public class ExtensionScriptWizard extends Wizard {

    private String m_selectedTestCaseId;
    private String m_extensionModuleAndClass;

    protected ExtScriptFilePage m_filePage;
    protected ExtScriptMethodPage m_targetInitPage;
    private ExtScriptMethodPage m_testInitPage;
    private ExtScriptMethodPage m_testEndPage;
    private ExtScriptMethodPage m_targetRestorePage;
    private ExtScriptToolbarPage m_toolbarScriptsPage;
    
    private List<ExtScriptMethodPage> m_stubPages;
    private List<ExtScriptMethodPage> m_testPointPages;
    private Map<String, CTestStub> m_stubsMap;
    private Map<String, CTestPoint> m_testPointsMap;
    private List<CTestSpecification> m_testCases;
    private CTestSpecification m_sampleTestCase;
    private String[] m_stubCountersNamesArray;
    private boolean m_canFinish = false;

    public int DEFAULT_SCRIPT_TIMEOUT_IN_SECONDS = 10;
    
    public ExtensionScriptWizard(String selectedTestCaseId,
                                 String extensionModuleAndClass,
                                 List<CTestSpecification> testCases,
                                 Map<String, CTestStub> stubsMap,
                                 Map<String, CTestPoint> testPointsMap) {
      super();
      setNeedsProgressMonitor(true);
      
      m_selectedTestCaseId = selectedTestCaseId;
      m_extensionModuleAndClass = extensionModuleAndClass;
      m_stubsMap = stubsMap;
      m_testPointsMap = testPointsMap;
      
      m_testCases = testCases;
      if (!m_testCases.isEmpty()) {
          m_sampleTestCase = m_testCases.get(0);
      } else {
          m_sampleTestCase = null;
      }
      
      setWindowTitle("Script Extensions Wizard");
    }

    
    @Override
    public void addPages() {
      m_filePage = new ExtScriptFilePage(m_extensionModuleAndClass);
      addPage(m_filePage);
      
      m_targetInitPage = new ExtScriptMethodPage(m_selectedTestCaseId,
                                                 m_sampleTestCase,
                                                 ExtScriptMethodPage.DEFAULT_INIT_TARGET_SCRIPT_METHOD_NAME,
                                                 "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_INIT_TARGET()),
                                                 "Create script method, which will be called before test case creation.\n" +
                                                 "Test local variables and parameters are NOT accesible at this point.",
                                                 false);
      m_targetInitPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                                          "icons/extensionWizard-TargetInit.png"));
      addPage(m_targetInitPage);

      // if there are no known stubbed methods, one page is shown by default 
      m_stubCountersNamesArray = new String[Math.max(m_stubsMap.size() + m_testPointsMap.size(), 1)];
      m_testInitPage = new ExtScriptMethodPage(m_selectedTestCaseId,
                                               null,
                                               m_sampleTestCase,
                                               null,
                                               m_stubCountersNamesArray,
                                               -1,
                                               ExtScriptMethodPage.DEFAULT_INIT_TEST_SCRIPT_METHOD_NAME,
                                               "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_INIT_FUNC()),
                                               "Create script method, which will be called just BEFORE the function under test is started. Test local variables \n" +
                                               "and parameters are accesible at this point. This method will be created also if stub scripts require counters.",
                                               false);
      m_testInitPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
              "icons/extensionWizard-TestInit.png"));
      addPage(m_testInitPage);
      
      
      m_stubPages = new ArrayList<>();
      int namesArrayIdx = 0;
      for (CTestStub testStub : m_stubsMap.values()) {
          ExtScriptMethodPage page = new ExtScriptMethodPage(m_selectedTestCaseId,
                                                             testStub.getFunctionName(),
                                                             m_sampleTestCase,
                                                             testStub.getAssignmentSteps(true),
                                                             m_stubCountersNamesArray,
                                                             namesArrayIdx++,
                                                             "stubScript_" + testStub.getFunctionName(),
                                                             "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_STUB()),
                                                             "Create script method, which will be called when a stub is hit.\n" +
                                                             "Test local variables and parameters are NOT accesible at this point.",
                                                             false);
          
          page.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                  "icons/extensionWizard-Stub.png"));
          m_stubPages.add(page);
          addPage(page);
      }
      
      if (m_stubsMap.isEmpty()  &&  m_selectedTestCaseId.isEmpty()) {
          // if there are no test specs selected, add one stub page
          ExtScriptMethodPage page = new ExtScriptMethodPage(m_selectedTestCaseId,
                                                             "",
                                                             m_sampleTestCase,
                                                             new CTestBaseList(),
                                                             m_stubCountersNamesArray,
                                                             namesArrayIdx++,
                                                             "stubScript",
                                                             "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_STUB()),
                                                             "Create script method, which will be called when a stub is hit.\n" +
                                                             "Test local variables and parameters are NOT accesible at this point.",
                                                             false);
          page.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                  "icons/extensionWizard-Stub.png"));
          m_stubPages.add(page);
          addPage(page);
      }

      m_testPointPages = new ArrayList<>();
      for (CTestPoint testPoint : m_testPointsMap.values()) {
          ExtScriptMethodPage page = new ExtScriptMethodPage(m_selectedTestCaseId,
                                                             testPoint.getId(),
                                                             m_sampleTestCase,
                                                             testPoint.getSteps(true),
                                                             m_stubCountersNamesArray,
                                                             namesArrayIdx++,
                                                             "testPointScript_" + testPoint.getId(),
                                                             "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_TEST_POINT()),
                                                             "Create script method, which will be called when a stub is hit.\n" +
                                                             "Test local variables and parameters are NOT accesible at this point.",
                                                             false);
          
          page.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                  "icons/extensionWizard-Stub.png"));
          m_testPointPages.add(page);
          addPage(page);
      }

      m_testEndPage = new ExtScriptMethodPage(m_selectedTestCaseId,
                                              m_sampleTestCase,
                                              ExtScriptMethodPage.DEFAULT_END_TEST_SCRIPT_METHOD_NAME,
                                              "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_END_FUNC()),
                                              "Create script method, which will be called just AFTER the function under test ends.\n" +
                                              "Test local variables, parameters and return value are accesible at this point.",
                                              false);
      m_testEndPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
              "icons/extensionWizard-TestEnd.png"));
      addPage(m_testEndPage);
      
      m_targetRestorePage = new ExtScriptMethodPage(m_selectedTestCaseId, 
                                                    m_sampleTestCase,
                                                    ExtScriptMethodPage.DEFAULT_RESTORE_TARGET_SCRIPT_METHOD_NAME,
                                                    "self." + CTestResult.funcType2PyVarName(CTestResultBase.getSE_RESTORE_TARGET()),
                                                    "Create script method, which will be called after the test is destroyed.\n" +
                                                    "Test local variables and parameters are NOT accesible at this point.",
                                                    false);
      m_targetRestorePage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
              "icons/extensionWizard-TargetRestore.png"));
      addPage(m_targetRestorePage);
      
      m_toolbarScriptsPage = new ExtScriptToolbarPage();
      m_toolbarScriptsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
              "icons/extensionWizard-ExtTools.png"));
      addPage(m_toolbarScriptsPage);
    }

    
    @Override
    public boolean canFinish() {
        // Allow finish only if the last page was shown at least once.
        // This way user has to see all pages - this is required, because of persistence -
        // some pages may already be set to create a method.
        if (!m_canFinish) {
            m_canFinish = getContainer().getCurrentPage() == m_toolbarScriptsPage;
        }
        return super.canFinish()  &&  m_canFinish;
    }
    
    
    @Override
    public boolean performFinish() {

        m_filePage.saveToPreferences();
        m_targetInitPage.saveToPreferences();
        m_testInitPage.saveToPreferences();
        m_testEndPage.saveToPreferences();
        m_targetRestorePage.saveToPreferences();
        m_toolbarScriptsPage.saveToPreferences();
        
        for (ExtScriptMethodPage page : m_stubPages) {
            page.saveToPreferences();
        }
        
        for (ExtScriptMethodPage page : m_testPointPages) {
            page.saveToPreferences();
        }
        
        String absScriptFileName = 
                ISysPathFileUtils.getAbsPathFromWorkingDir(m_filePage.getScriptFileName()); 
                
        try (PrintWriter writer = generateScriptHeader(m_filePage.isOverwriteFile(), 
                                                       absScriptFileName,
                                                       m_filePage.getClassName());) {
            
            generateScriptMethods(writer, m_testCases);

        } catch (IOException ex) {
            SExceptionDialog.open(getShell(), "Can not generate extension script file!", ex);
        }

        if (m_filePage.isOpenScriptInEditor()) {
            try {
                openScriptInEditor(absScriptFileName);
            } catch (IOException ex) {
                SExceptionDialog.open(getShell(), "Can not start editor!", ex);
            }
        }
        return true;
    }

    
    private PrintWriter generateScriptHeader(boolean isOverwriteFile,
                                                String scriptFileName,
                                                String className) throws IOException {
        
        OpenOption truncateOrAppend = isOverwriteFile ? StandardOpenOption.TRUNCATE_EXISTING :
                                                        StandardOpenOption.APPEND;
        
        Path scriptPath = Paths.get(scriptFileName);
        
        // do not generate class def if script already exists
        boolean isGenerateHeader = isOverwriteFile  ||  !scriptPath.toFile().exists();
        
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(scriptPath, 
                                                                     Charset.forName("UTF-8"), 
                                                                     StandardOpenOption.CREATE, 
                                                                     StandardOpenOption.WRITE, 
                                                                     truncateOrAppend));

        if (!isGenerateHeader) {
            return writer;
        }
        
        writer.println("# This script was generated with testIDEA script extensions wizard.");
        writer.println("# Customize it according to your needs.");
        writer.println();
        writer.println();
        writer.println("import isystem.connect as ic"); 
        writer.println();
        writer.println("class " + className + ":");  
        writer.println();
        writer.println("    def __init__(self, mccMgr = None):"); 
        writer.println("        \"\"\""); 
        writer.println("        Normally we'll connect to winIDEA, which is running the test, so that");
        writer.println("        target state can be accessed/modified by this script.");
        writer.println("        \"\"\"");
        writer.println();
        writer.println("        if mccMgr == None:");
        writer.println("            # Executed when called from testIDEA.");
        writer.println("            # Connection can't be passed between processes.");
        writer.println("            self.mccMgr = None");
        writer.println("            self.connectionMgr = ic.ConnectionMgr()");
        writer.println("            self.connectionMgr.connectMRU()");
        writer.println("            self.debug = ic.CDebugFacade(self.connectionMgr)");
        writer.println("        else:");
        writer.println("            # Executed when called from generated script - connection is reused.");
        writer.println("            self.mccMgr = mccMgr");
        writer.println("            self.connectionMgr = mccMgr.getConnectionMgr('')");
        writer.println("            self.debug = mccMgr.getCDebugFacade('')");
        writer.println();
        writer.println("        self.testCtrl = None");
        writer.println();
        writer.println();
        writer.println("    def __getTestCaseCtrl(self):");
        writer.print(  "        if self.testCtrl == None  or  self.testCtrl.getTestCaseHandle() != self.");
        writer.print(Script.TEST_CASE_HANDLE_VAR); writer.println(":");
        writer.print(  "            self.testCtrl = ic.CTestCaseController(self.connectionMgr, self.");
        writer.print(Script.TEST_CASE_HANDLE_VAR); writer.println(")");
        writer.println(  "        return self.testCtrl");

        writer.println("");
        writer.println("");

        return writer;
    }
    
    
    private void generateScriptMethods(PrintWriter writer,
                                       List<CTestSpecification> testCases) throws IOException {
        
        GroupAction grpAction = new GroupAction("Set names of script functions");
        
        setScriptConfig();

        // assign method names to test cases
        for (CTestSpecification testCase : testCases) {
            createScriptFuncNameAction(grpAction, testCase, m_targetInitPage, 
                                       testCase.getInitTargetFunction(false));
            createScriptFuncNameAction(grpAction, testCase, m_testInitPage, 
                                       testCase.getInitFunction(false));

            for (ExtScriptMethodPage page : m_stubPages) {
                String stubbedFuncName = page.getStubbedFuncNameOrTpId();
                CTestBaseList stubs = testCase.getStubs(true);
                
                for (int i = 0; i < stubs.size(); i++) {
                    CTestStub stub = CTestStub.cast(stubs.get(i));
                    if (stub.getFunctionName().equals(stubbedFuncName)) {
                        createStubScriptFuncAction(grpAction, testCase, page, stub);
                        break; // there is only one stub object for each stubbed function
                    }
                }
            }

            for (ExtScriptMethodPage page : m_testPointPages) {
                String tpId = page.getStubbedFuncNameOrTpId();
                CTestBaseList testPoints = testCase.getTestPoints(true);
                
                for (int i = 0; i < testPoints.size(); i++) {
                    CTestPoint testPoint = CTestPoint.cast(testPoints.get(i));
                    if (testPoint.getId().equals(tpId)) {
                        createStubScriptFuncAction(grpAction, testCase, page, testPoint);
                        break; // there is only one object for each test point 
                    }
                }
            }

            createScriptFuncNameAction(grpAction, testCase, m_testEndPage, 
                                       testCase.getEndFunction(false));
            createScriptFuncNameAction(grpAction, testCase, m_targetRestorePage, 
                                       testCase.getRestoreTargetFunction(false));
        }
        
        grpAction.addAllFireEventTypes();
        TestSpecificationModel.getActiveModel().execAction(grpAction);
        
        // write methods to file
        Set<String> createdMethods = new TreeSet<>();
        if (testCases.isEmpty()) {
            generateScriptMethods(writer, new CTestSpecification(), createdMethods);
        } else {
            for (CTestSpecification testSpec : testCases) {
                generateScriptMethods(writer, testSpec, createdMethods);
            }                
        }
    }

    
    private void generateScriptMethods(PrintWriter writer, 
                                       CTestSpecification testSpec, 
                                       Set<String> createdMethods) throws IOException {
        
        generateScriptMethodForPage(writer, testSpec, createdMethods, m_targetInitPage);
        generateScriptMethodForPage(writer, testSpec, createdMethods, m_testInitPage);
        for (ExtScriptMethodPage page : m_stubPages) {
            generateScriptMethodForPage(writer, testSpec, createdMethods, page);
        }
        for (ExtScriptMethodPage page : m_testPointPages) {
            generateScriptMethodForPage(writer, testSpec, createdMethods, page);
        }
        generateScriptMethodForPage(writer, testSpec, createdMethods, m_testEndPage);
        generateScriptMethodForPage(writer, testSpec, createdMethods, m_targetRestorePage);
        
        generateScriptMethodForPage(writer, testSpec, createdMethods, m_toolbarScriptsPage);
    }

    
    private void generateScriptMethodForPage(PrintWriter writer, 
                                             CTestSpecification testSpec,
                                             Set<String> createdMethods,
                                             IExtScriptPage page) throws IOException {
        String methodName = page.generateScriptMethodName(testSpec);
        // if selected test cases have the same ID or function name or the user
        // did not select ${} vars for generic script method names, then duplicate
        // methods could appear.
        if (!createdMethods.contains(methodName)) {
            
            createdMethods.add(methodName);
            
            String scriptMethodText = page.generateScriptMethod(testSpec);
            scriptMethodText = scriptMethodText.replace("\n", System.lineSeparator());
            writer.print(scriptMethodText);
        }
    }

    
    private void setScriptConfig() {
        Path p = Paths.get(m_filePage.getScriptFileName());
        String moduleName = p.getFileName().toString();
        if (moduleName.endsWith(".py")) {
            moduleName = moduleName.substring(0, moduleName.length() - 3);
        }
        
        String scriptClassName = m_filePage.getClassName();
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CScriptConfig scriptCfg = model.getCEnvironmentConfiguration().getScriptConfig(false);
        scriptCfg.setExtensionClass(moduleName + "." + scriptClassName);
        
        boolean isModuleFound = false;
        StrVector modulesList = new StrVector();
        scriptCfg.getModules(modulesList);
        for (int i = 0; i < modulesList.size(); i++) {
            if (moduleName.equals(modulesList.get(i))) {
                isModuleFound = true;
                break;
            }
        }
        
        if (!isModuleFound) {
            modulesList.add(0, moduleName);
            scriptCfg.setModules(modulesList);
        }
        
        if (scriptCfg.getTimeout() == 0) {
            scriptCfg.setTimeout(DEFAULT_SCRIPT_TIMEOUT_IN_SECONDS);
        }
    }

    
    private void createScriptFuncNameAction(GroupAction grpAction,
                                            CTestSpecification testSpec,
                                            ExtScriptMethodPage page,
                                            CTestFunction testBase) throws IOException {

        if (page.isCreateMethod()) {
            YamlScalar value = YamlScalar.newValue(CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue());
            value.setValue(page.generateScriptMethodName(testSpec));
            SetSectionAction action = new SetSectionAction(testBase, 
                                                           ENodeId.SCRIPT_NODE,
                                                           value);
            action.addDataChangedEvent();
            grpAction.add(action);
            
            if (page.isPassTestSpecAsParam()) {
                value = YamlScalar.newList(CTestFunction.ESection.E_SECTION_PARAMS.swigValue());
                value.setValue(Script.RESERVED_TEST_SPEC_PARAM);
                action = new SetSectionAction(testBase, 
                                              ENodeId.SCRIPT_NODE,
                                              value);
                action.addDataChangedEvent();
                grpAction.add(action);
            }
        }
    }


    private void createStubScriptFuncAction(GroupAction grpAction,
                                            CTestSpecification testCase,
                                            ExtScriptMethodPage page,
                                            CTestBase stubOrTp) throws IOException {

        if (page.isCreateMethod()) {
            YamlScalar value = 
                YamlScalar.newValue(EStubSectionIds.E_SECTION_SCRIPT_FUNCTION.swigValue());

            value.setValue(page.generateScriptMethodName(testCase));
            SetSectionAction action = new SetSectionAction(stubOrTp, 
                                                           ENodeId.STUBS_NODE,
                                                           value);
            action.addDataChangedEvent();
            grpAction.add(action);
            
            value = YamlScalar.newList(EStepSectionIds.E_SECTION_SCRIPT_PARAMS.swigValue());

            if (page.isPassTestSpecAsParam()) {
                
                CTestBaseList steps = page.getAssignmentSteps();
                if (steps != null) {
                    int numSteps = (int) steps.size();
                    if (numSteps > 0) {
                        for (int stepIdx = 0; stepIdx < numSteps; stepIdx++) {

                            CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(stepIdx)); // all script method calls 
                            // should have the same number of parameters

                            CSequenceAdapter assignments = 
                                    new CSequenceAdapter(step, 
                                                         EStepSectionIds.E_SECTION_SCRIPT_PARAMS.swigValue(), 
                                                         false);
                            StrVector stubScriptParams = new StrVector();
                            assignments.getStrVector(stubScriptParams);

                            StringBuilder sb = new StringBuilder(CYAMLUtil.strVector2Str(stubScriptParams));
                            // If testSpec as param is not specified yet, add it now
                            if (!sb.toString().startsWith(Script.RESERVED_TEST_SPEC_PARAM)) {  
                                sb.insert(0, Script.RESERVED_TEST_SPEC_PARAM + ", ");
                            }

                            value.setValue(sb.toString());
                            action = new SetSectionAction(step, 
                                                          ENodeId.STUBS_NODE,
                                                          value);
                            action.addDataChangedEvent();
                            grpAction.add(action);
                        }
                    } else {
                        // add one step with parameter
                        CTestEvalAssignStep step = new CTestEvalAssignStep(stubOrTp);
                        InsertToTestBaseListAction addAction = 
                                new InsertToTestBaseListAction(steps, step, -1);
                        grpAction.add(addAction);
                        value.setValue(Script.RESERVED_TEST_SPEC_PARAM);
                        action = new SetSectionAction(step, ENodeId.STUBS_NODE, value);
                        action.addDataChangedEvent();
                        grpAction.add(action);
                    }
                }
            }
        }
    }


    private void openScriptInEditor(String scriptFileName) throws IOException {
        
        ProcessBuilder proc = new ProcessBuilder(m_filePage.getPythonEditorPath(), scriptFileName);
        proc.start();
    }
} 
