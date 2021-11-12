package si.isystem.itest.diagrams;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.connect.ConnectionPool;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EDiagType;
import si.isystem.connect.CTestDiagramConfig.EViewerType;
import si.isystem.connect.CTestDiagrams;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.ISysPathFileUtils;
import si.isystem.itest.editors.ImageEditorPart;
import si.isystem.itest.editors.MultiImageEditorInput;
import si.isystem.itest.handlers.ToolsRefreshGlobalsCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.python.Python;

// TODO:
// - define output folder (default 'diagrams' in winIDEA workspace, else
//                         'diagrams' in Eclipse project). Add this option
//                         to preferences.props config.
// - Python script should be extracted from plug-in in tmp workspace folder, or
//   if configured into user defined folder (for customization purposes)
// - add graphwiz dot and seqdiag to winIDEA distribution / testIDEA plug-in
// - add debug mode (see Python.java.IS_DEBUG), which starts Python in separate 
//   terminal window in interactive mode.
// Known issue:
// If sequence diagram is zoomed IN to much, then an exception is thrown in 
// Batik and the diagram disappears. Zoom-out or 'R' (reset) restores the diagram.
// The issue can be fixed by removing 'filter-blur' from SVG file. If we change
// function 
//
//   Python33/Lib/site-packages/blockdiag-1.4.6-py3.3.egg/blockdiag/imagedraw/svg.py::style()
//
// to return None:
//   def style(name):
//       return None
//
// Then shades are not blurred, but solid rectangles. Not that nice, but zooming 
// is no longer a problem for Batik.
// Working solution would be to actually change svg.py as proposed above, but
// this solution would not work for custom Python installations (not many, if any
// customer uses this). Since this appears only at large zoom factor only, the
// fix won't be applied.


/**
 * This class runs Python script and opens generated image in editor.
 * 
 * @author markok
 */
public class DiagramUtils {

    public static final String DIAGRAM_SRC_DIR = "templates/diagramScripts";
    protected static final int TIMEOUT_1s = 1000;

    public enum EDiagramType {SEQUENCE_DIAG, CALL_GRAPH};
    

    /**
     * Images should be saved to the same folder as report is saved.
     * 
     * @param testSpec
     * @param idx
     * @return
     */
    public static String getAbsImageFileName(CTestSpecification testSpec,
                                             int idx) {

        String workingDir = ISysPathFileUtils.getIYAMLDir();

        CTestDiagrams diagrams = testSpec.getDiagrams(true);

        CTestBaseList diagConfigsList = diagrams.getConfigurations(true);
        int numDiagConfigs = (int) diagConfigsList.size();
        if (idx >= 0  &&  idx < numDiagConfigs) {
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigsList.get(idx));
            
            String imageFileName = getAbsImageFileFromReportDir(testSpec,
                                                                 diagConfig);

            Path path = Paths.get(imageFileName);
            if (!path.isAbsolute()) {
                path = Paths.get(workingDir, imageFileName);
                imageFileName = path.toString();
            }

            return imageFileName;
        }
        
        return null;
    }


    public static String getAbsImageFileFromReportDir(CTestSpecification testSpec,
                                                      CTestDiagramConfig diagConfig) {
        
        String imageFileName = CTestHostVars.getDiagramFileName(testSpec, 
                                                                diagConfig);
        
        imageFileName = ISysPathFileUtils.getAbsPathFromDir(ISysPathFileUtils.getAbsReportDir(), 
                                                        imageFileName);
        return imageFileName;
    }


/*    private static String getDiagramFileNameFromTestSpec(CTestSpecification testSpec,
                                             CTestDiagramConfig diagConfig) {
        
        String outFile = diagConfig.getOutputFileName();
        CTestHostVars hostVars = CTestHostVars.createForDiagrams(testSpec, diagConfig);
        outFile = hostVars.replaceHostVars(outFile);
        return outFile;
    } */

    
    public static CTestDiagramConfig getDiagConfig(CTestSpecification testSpec,
                                                   int idx) {

        CTestDiagrams diagrams = testSpec.getDiagrams(true);

        CTestBaseList diagConfigsList = diagrams.getConfigurations(true);
        int numDiagConfigs = (int) diagConfigsList.size();
        if (idx >= 0  &&  idx < numDiagConfigs) {
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigsList.get(idx));
            return diagConfig;
        }
        
        return null;
    }
    
    
    public static void createDiagramsInTestRuntime(String wiWorkspaceDir,
                                                   String reportDir,
                                                   String dotExeDir,
                                                   CTestSpecification testSpec,
                                                   CTestResult testResult,
                                                   long scriptTimeout, 
                                                   IProgressMonitor monitor) {
        
        CTestDiagrams diagrams = testSpec.getDiagrams(true);
        CTestBaseList diagConfigsList = diagrams.getConfigurations(true);
        int numDiagConfigs = (int) diagConfigsList.size();
        
        int numDiagramsToCreate = 0;
        for (int idx = 0; idx < numDiagConfigs; idx++) {
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigsList.get(idx));
            if (diagConfig.isActive() == ETristate.E_TRUE) {
                numDiagramsToCreate++;
            }
        }
        
        for (int idx = 0; idx < numDiagConfigs; idx++) {
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigsList.get(idx));
            
            if (diagConfig.isActive() == ETristate.E_TRUE) {
                monitor.subTask("Creating diagram no. " + (idx + 1) + " / " + numDiagramsToCreate);
                createDiagram(wiWorkspaceDir,
                              reportDir,
                              dotExeDir, 
                              testSpec, diagConfig, testResult, scriptTimeout, monitor, true);
            }
        }
    }


    // called to create diagram on user's action
    public static void createDiagram(final String absWiWorkspaceDir,
                                     final String absReportDir,
                                     final CTestSpecification testSpec,
                                     final CTestDiagramConfig diagConfig,
                                     final CTestResult testResult,
                                     final long scriptTimeout) {
        
        EDiagType diagramType = diagConfig.getDiagramType();
        if (diagramType != EDiagType.EFlowChart  &&  diagramType != EDiagType.EStaticCallGraph
                &&  testResult == null) {
            throw new SIllegalStateException("There is no result for test case! Please run the test.")
                .add("testCase", testSpec.getUILabel());
        }

        try {
            final JConnection cmgr = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);
            if (cmgr == null  ||  !cmgr.isConnected()) {
                new ToolsRefreshGlobalsCmdHandler().refreshSymbols(false);
            }
            
            final String dotExeDir = ISysPathFileUtils.getDotExeDir();
            
            PlatformUI.getWorkbench().getProgressService()
                .busyCursorWhile(new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                                                                 InterruptedException {        

                    monitor.beginTask("Running Python script for diagram", 
                                      IProgressMonitor.UNKNOWN);
                    try {
                        createDiagram(absWiWorkspaceDir,
                                      absReportDir,
                                      dotExeDir, 
                                      testSpec, diagConfig, testResult,
                                      scriptTimeout, monitor,
                                      false);
                    } finally {
                        monitor.done();
                    }
                }
            });

        } catch (InvocationTargetException ex) {
            SExceptionDialog.open(Activator.getShell(), 
                      "Invocation exception - execution of Python script failed!",
                      ex); 
        } catch (InterruptedException ex) {
            MessageDialog.openError(Activator.getShell(), 
                        "Canceled", 
                        "Diagram creation with Python script has been canceled!"); 
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not create one or more diagrams for test case " 
                                  + testSpec.getTestId(),
                                  ex); 
        }
    }

    
    public static void createDiagram(String wiWorkspaceDir,
                                     String reportDir,
                                     String dotExeDir,
                                     CTestSpecification testSpec,
                                     CTestDiagramConfig diagConfig,
                                     CTestResult testResult,
                                     long scriptTimeout, 
                                     IProgressMonitor monitor, 
                                     boolean isTestRuntime) {
        
        Path reportPath = Paths.get(reportDir);
        if (!Files.exists(reportPath)) {
            try {
                Files.createDirectories(reportPath);
            } catch (IOException ex) {
                throw new SIOException("Output directory for reports and diagram files does not exists and its creation failed!!").
                     add("directory", reportDir);
            }
        }
        
        String outFile = CTestHostVars.getDiagramFileName(testSpec, diagConfig);
        outFile = ISysPathFileUtils.getAbsPathFromDir(reportDir, outFile);
        
        // delete output file first, to prevent outdated images being shown and
        // included in reports in case of silent script error (empty except block,
        // for example)
        try {
            Files.deleteIfExists(Paths.get(outFile));
        } catch (IOException ex) {
            throw new SIOException("Can not delete old diagram image file!").
                add("fileName", outFile);
        }
        
        String profilerExportFile = "";
        if (testResult != null) { 
            profilerExportFile = testResult.getProfilerExportFileName();
        }
        
        StrVector args = new StrVector();
        diagConfig.getScriptCmdLineArgs(testSpec, outFile, profilerExportFile, dotExeDir, args);
        if (diagConfig.getDiagramType() != EDiagType.ECustom  &&  diagConfig.getDiagramType() != EDiagType.ECustomAsync) {
            args.add(0, "-m");  // runs module as script
        }
        String []cmdLineArgs = DataUtils.strVector2StringArray(args);
        

        EDiagType diagType = diagConfig.getDiagramType();
        if (diagType == EDiagType.ECustomAsync) {
            createDiagramWithScriptAsync(cmdLineArgs, wiWorkspaceDir);
        } else {
            createDiagramWithScript(cmdLineArgs, wiWorkspaceDir, scriptTimeout, monitor,
                                    isTestRuntime);
        }
    }


    /**
     * 
     * @param cmdLineArgs
     * @param workingDir
     * @param scriptTimeout in milliseconds, 0 for no timeout.
     * @param monitor 
     */
    private static void createDiagramWithScript(final String [] cmdLineArgs,
                                                final String workingDir,
                                                long scriptTimeout, 
                                                IProgressMonitor monitor,
                                                final boolean isTestRuntime) {

        final JConnection jCon = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);

        final StringBuilder stdout = new StringBuilder();
        final StringBuilder stderr = new StringBuilder();
        MutableBoolean isTimeout = new MutableBoolean();
        MutableBoolean isCanceled = new MutableBoolean();
        MutableObject<SException> exception = new MutableObject<>();

        Python python = new Python();
        int processRetVal = python.execScript(jCon, monitor, workingDir, cmdLineArgs, 
                                              scriptTimeout, 
                                              stdout, stderr, isTimeout, 
                                              isCanceled, exception);
        
        
        final StringBuilder statusText = new StringBuilder();
        
        statusText.append(stdout);

        final StatusType status = isCanceled.booleanValue() ? StatusType.WARNING : StatusType.OK;
        
        if (isCanceled.booleanValue()) {
            statusText.append("Diagram creation canceled by user!");
        }
        
        if (statusText.length() > 0) {
            statusText.append('\n');
            
            Display.getDefault().syncExec(new Runnable() {
                
                @Override
                public void run() {
                    if (isTestRuntime) { // test runtime output should be saved
                        StatusModel.instance().appendDetailPaneText(status, 
                                                                    statusText.toString());
                    } else {
                        // If diagrams are created interactively, they should write to 
                        // status pane directly, not to be logged by StatusModel. 
                        StatusView.getView().setDetailPaneText(status, 
                                                               statusText.toString());
                    }
                }
            });
        }
        
        if (stderr.length() > 0  ||  isTimeout.booleanValue()  ||  processRetVal != 0) {
            
            if (isTimeout.booleanValue()) {
                stderr.insert(0, "  Timeout!");
            }
            
            stderr.insert(0, "Error reported by script:\n");
            stderr.insert(0, "Error code returned by script: " + processRetVal + '\n');
            stderr.insert(0, "Script command: " + StringUtils.join(cmdLineArgs, ' ') + '\n');
            stderr.insert(0, "Script dir: " + workingDir + '\n');

            if (exception.getValue() == null) {
                throw new SIOException("Error when running external script!").
                    add("error", stderr.toString()).
                    add("stdout", stdout.toString());
            } else {
                throw new SIOException("Error when running external script!", exception.getValue()).
                    add("error", stderr.toString()).
                    add("stdout", stdout.toString());
            }
        }

        // Check for exception at end, so that any captured output is printed in 
        // status view.
        if (exception.getValue() != null) {
            throw exception.getValue();
        }
    }
    
    
    public static void createDiagramWithScriptAsync(final String [] cmdLineArgs,
                                                    final String workingDir) {

        final JConnection jCon = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);

        Python python = new Python();
        try {
            python.execScriptAsync(jCon, workingDir, false, cmdLineArgs);
        } catch (Exception ex) {
            SException exception = new SException("Diagram creation with Python script failed!", 
                                                  ex);
            ex.printStackTrace();
            throw exception;
        }
    }
    
    
    public static void openDiagram(CTestSpecification testSpec, 
                                   CTestDiagramConfig diagConfig,
                                   String reportDir) {
        
        EViewerType viewerType = diagConfig.getViewerType();
        switch (viewerType) {
        case ENone:
            MessageDialog.openWarning(Activator.getShell(), 
                                      "Can not open selected diagram!", 
                                      "Please specify a viewer in column 'viewer' in the table below!");
            break;
        case ESinglePage:
        case EMultipage:
            openDiagramInEditor(testSpec, diagConfig, reportDir);
            break;
        case EExternal:
            openExternalViewer(testSpec, diagConfig);
            break;
        default:
            break;
        
        }
    }


    private static void openExternalViewer(CTestSpecification testSpec,
                                           CTestDiagramConfig diagConfig) {
        
        String externalViewer = diagConfig.getExternalViewerName();
        String workingDirectory = ISysPathFileUtils.getIYAMLDir();
        String imageFileName = getAbsImageFileFromReportDir(testSpec, diagConfig);
        String [] cmdLine;
        
        if (externalViewer.isEmpty()) {
            // start default system viewer for the given file type
            
            cmdLine = new String[]{"cmd.exe", "/C", imageFileName};

            // strangely enough, but this does not work for PNG images :-(
            // Desktop.getDesktop().open(new File("D:\\Razno\\Power8side-q20b.png"));
            
        } else {
            cmdLine = new String[]{externalViewer, imageFileName};
        }
        
        try {
            if (workingDirectory == null) {
                Runtime.getRuntime().exec(cmdLine, null);
            } else {
                Runtime.getRuntime().exec(cmdLine, 
                                          null, 
                                          new File(workingDirectory));
            }
        } catch (Exception ex) {
            throw new SException("Can not open file with external viewer!", ex).
                add("cmdLine", StringUtils.join(cmdLine, " "));
        }
        
    }
    
    
    private static void openDiagramInEditor(CTestSpecification testSpec, 
                                            CTestDiagramConfig diagConfig,
                                            String workingDir) {
    
        try {
            
            String imageFileName = getAbsImageFileFromReportDir(testSpec, diagConfig);

            // filePath = "D:/Razno/G-dickens-2012-HPx.jpg";
            try {
                URI uri;
                Path path = Paths.get(imageFileName);
                
                // if imageFileName is absolute, do not specify working dir
                if (path.isAbsolute()) {
                    uri = new URI("file", "//" + imageFileName.replace('\\', '/'), ""); 
                } else {
                    uri = new URI("file", 
                                  "//" + workingDir.replace('\\', '/') + "/" + imageFileName, 
                            "");
                }

                
                if (!URIUtil.toFile(uri).exists()) {
                    throw new SIOException("Cannot open diagram in viewer! File does not exist:\n"
                            + "  " + imageFileName);
                }
                

                IWorkbench wb = Activator.getDefault().getWorkbench();
                IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
                IWorkbenchPage page = window.getActivePage();
                
                /*
                 * always use MultiImageEditorInput as it contains diag config.
                 * 
                if (diagConfig.getViewerType() == EViewerType.ESinglePage) {
                    IFileStore imageFileStore = EFS.getStore(uri);
                    page.openEditor(new FileStoreEditorInput(imageFileStore), 
                                    ImageEditorPart.ID);
                } else { */
                    IEditorInput mpei = new MultiImageEditorInput(testSpec.getTestId(), 
                                                                  uri,
                                                                  diagConfig);
                    IEditorPart editor = page.openEditor(mpei, ImageEditorPart.ID);
                    
                    if (editor != null  &&  editor instanceof ImageEditorPart) {
                        
                        ImageEditorPart imageEditor = (ImageEditorPart)editor;
                        imageEditor.addImagePage(uri, diagConfig);
                    }
                // }
                
            } catch (CoreException ex) {
                SExceptionDialog.open(Activator.getShell(), 
                                      "Can not open sequence diagram image!", 
                                      ex);
                ex.printStackTrace();
            } catch (URISyntaxException ex) {
                SExceptionDialog.open(Activator.getShell(), 
                                      "Can not open sequence diagram image from URL!", 
                                      ex);
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not open diagram!", 
                                  ex);
        }
    }
}
