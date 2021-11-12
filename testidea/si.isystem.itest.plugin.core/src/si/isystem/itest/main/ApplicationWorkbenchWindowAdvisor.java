package si.isystem.itest.main;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.handlers.FileOpenCmdHandler;
import si.isystem.itest.handlers.ToolsResetPerspectiveHandler;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    protected static final String ITB_FILE_EXTENSION = "iyaml";
    static private IMenuManager m_mainMenuManager;

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    
    @Override
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        m_mainMenuManager = getWindowConfigurer().getActionBarConfigurer().getMenuManager();
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    public static IMenuManager getMainMenuManager() {
        return m_mainMenuManager;
    }

    @Override
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        
        if (Activator.ms_isSWTBotTest) { // make window big enough - see 
                                         // CreateAndRunTest.testContentProposals()
                                         // and KTable in stubs and test points also has to
                                         // be wide enough
            configurer.setInitialSize(new Point(1400, 1000));
        } else {
            configurer.setInitialSize(new Point(1600, 1024));
        }
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
    }

    
    // changed from preWindowShellClose() to postWindowClose(), since otherwise
    // IPC classes were released to early - user might later decide to cancel
    // close operation, if some editors were not saved. Since the shell is already
    // closed when this method is called, errors are logged, not shown in pop-up.
    @Override
    public void postWindowClose() {
    }
    
    
    /**
     * This method opens data file.
     * 
     *   If yaml file with winIDEA workspace name and extension '.iyaml' exists:
     *       open it.
     *   else if other iyaml files in the same folder exist:
     *       present user file open *.iyaml dialog.
     *   else 
     *       ask user to create workspaceFile.iyaml file or not.
     *
     * Then:
     * - Start IPC receiver 
     * - register IPC Server
     */
    @Override
    public void postWindowOpen() {

        ToolsResetPerspectiveHandler.removeCoolbarButtonsFromEclipsePlugins();
        
        Activator activator = Activator.getDefault();

        try {
            activator.parseCmdLineArgs();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not parse command line arguments!", 
                                  ex);
        }
        
        String iyamlFile = activator.getCmdLineOptIYamlFile();
        try {

            // if testIDEA data file is specified in command line, open it
            if (iyamlFile != null  &&  !iyamlFile.isEmpty()) {
                FileOpenCmdHandler.openEditorWCreate(iyamlFile);
            } else {
                String wsFilePath = activator.getCmdLineWorkspaceName();

                if (wsFilePath != null  &&  !wsFilePath.isEmpty()) {
                    openYamlFileFromWorkspace(wsFilePath);
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "There were problems when opening test specification file: " + iyamlFile, ex);
        }
        
        TestCaseEditorPart editor = TestCaseEditorPart.getActive();
        if (editor != null) {
            editor.setFocus(); // refresh file already loaded
        }
        
        // do not connect at startup, only when winIDEA is needed (globals, test run, ...)
        
        // ToolsConnectToWinIDEACmdHandler handler = new ToolsConnectToWinIDEACmdHandler();
        // handler.connect(isConnectUnconditionally);

        /*if (Activator.connectToWinIdea(isConnectUnconditionally).isConnected()) {
            try {
                TestSpecificationModel.getInstance().fireEvent(new ModelChangedEvent(EventType.CONNECTION_ESTABLISHED));
            } catch (Exception ex) {
                // ignore exception, because we do not want to annoy users with
                // non-critical error messages at startup. The exception occurs,
                // when connection to winIDEA succeeds, but the file is not downloaded to
                // the target so winIDEA has not loaded debug info yet. 
            }
        }*/
        
        PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
            
            @Override
            public void windowOpened(IWorkbenchWindow window) {
            }
            
        
            @Override
            public void windowDeactivated(IWorkbenchWindow window) {
            }
            
        
            @Override
            public void windowClosed(IWorkbenchWindow window) {
            }
            
        
            @Override
            public void windowActivated(IWorkbenchWindow window) {
                // System.err.println("window activated");
                UiUtils.checkForReload();
            }
        });
    }

    
    private void openYamlFileFromWorkspace(String wsFilePath) throws IOException, 
                                                                        PartInitException, 
                                                                        CoreException {
        
        String iyamlFile = UiUtils.replaceOrAddExtension(wsFilePath, ITB_FILE_EXTENSION);
        
        if (new File(iyamlFile).exists()) {
            FileOpenCmdHandler.openEditor(iyamlFile);
            return;
        } 
        
        // search for other iyaml files
        File winIDEAWorkspace = new File(wsFilePath);
        String wsDir = winIDEAWorkspace.getParent();

        File[] itbFiles = UiUtils.listdir(wsDir, ITB_FILE_EXTENSION, false);

        if (itbFiles.length > 0) {
            // if found, prompt for selection

            String fileName = UiUtils.showOpenIYamlFileDialog(Activator.getShell(), 
                                                              wsDir,
                                                              null,
                                                              false);
            if (fileName != null) {
                FileOpenCmdHandler.openEditor(fileName);
            } else {
                // use default yaml file name based on workspace file name
                FileOpenCmdHandler.openEditorWCreate(iyamlFile);
            }
        } else {
            // dialog was commented, because clicking Cancel opens testIDEA with
            // error: 'Can not open file! File does not exist: d:\bb\trunk\sdk\targetProjects\SampleSTM32.iyaml'
            // in editor, and user has to close it, and create a new file.
            // Furthermore, support noticed that this dialog sometimes opens in the
            // background, and it is very difficult to put it in front (min all, max winIDEA, min winIDEA).
//            // if none is found, prompt for create
//            boolean retVal = 
//                MessageDialog.openConfirm(Activator.getShell(),
//                                          "Test specification file", 
//                                          "Default test specification file '" + 
//                                          iyamlFile + "' does not exist.\n" +
//                "Do you want to create it?");
//            if (retVal) {
//                FileOpenCmdHandler.openEditorWCreate(iyamlFile);
//                return true;
//            }
            
            FileOpenCmdHandler.openEditorWCreate(iyamlFile);
        }
    }
}
