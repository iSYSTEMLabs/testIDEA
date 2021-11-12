package si.isystem.itest.handlers;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.CustomScriptsMenuOptions;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.ExtensionScriptInfo;
import si.isystem.itest.run.Script;
import si.isystem.itest.ui.spec.StatusView;

public class ToolsRefreshGlobalsCmdHandler extends AbstractHandler {

    private Exception m_exception;
    
    final IIConnectOperation refreshOperation = new IIConnectOperation() {

        @Override
        public void exec(JConnection jCon) {
            // refresh all global items, so that user doesn't have to click Refresh 
            // several times. If it will be a performance problem in the future,
            // fine tune it.
//            GlobalsConfiguration.instance().refresh();
//            TestCaseEditorPart activeEditor = TestCaseEditorPart.getActive();
//            if (activeEditor != null) {
//                activeEditor.refreshGlobals();
//            }
        }
        
        @Override
        public void setData(Object data) {}
    };

    
    @Override
    public Object execute(ExecutionEvent event) {

        refreshCustomScriptMethods();
        refreshSymbolsAndUpdateEditor(true);
        
        return null;
    }


    public void refreshSymbolsAndUpdateEditor(final boolean isShowErrorMessage) {
        refreshSymbols(isShowErrorMessage);
        System.out.println("Symbols are refreshed, refreshing active editor.");
        TestCaseEditorPart activeEditor = TestCaseEditorPart.getActive();
        if (activeEditor != null) {
            activeEditor.refreshGlobals();
        }
    }

    
    private void refreshCustomScriptMethods() {
        try {
            Script script = UiUtils.initScript(null);
            String extClass = script.getExtensionClass();
            if (extClass.isEmpty()) {
                return; // Not configured, do not write annoying warnings to users who
                        // do not use scripts at all.
            }
                
            ExtensionScriptInfo extensionInfo = script.getExtScriptInfo();

            CustomScriptsMenuOptions.INSTANCE.addPyMethodsAndScriptsToMenu(extensionInfo);
            TestCaseEditorPart.getActive().setTableEditorScriptMenu(extensionInfo.getRangeMethods());
            
        } catch (Exception ex) {
            // msg dialog is to annoying, because it appears for empty projects
            // or project which do not use scripts.
            StatusView.getView().setDetailPaneText(StatusType.WARNING, 
                                                   "Can not get custom Python scripts or methods in extension script\n"
                                                   + "to show them in menu 'iTools'!\n"
                                                   + SEFormatter.getInfo(ex));
        }
    } 

    
    public void refreshSymbols(final boolean isShowErrorMessage) {
        
        m_exception = null;
        final TestSpecificationModel model = GlobalsConfiguration.instance().getActiveModel();
        JConnection defConnection = ConnectionProvider.instance().getDefaultConnection();
        
        if (!checkCoreIds(model, defConnection)) {
            defConnection = new ToolsConnectToWinIDEACmdHandler().connect(false);
        }
        
        final JConnection connection = defConnection;
        
        try {
            PlatformUI.getWorkbench().getProgressService()
            .busyCursorWhile(new IRunnableWithProgress() {
                
                @Override
                public void run(IProgressMonitor monitor) {
                    
                    try {
                        refresh(monitor, connection, model);
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        Activator.log(IStatus.ERROR, "Can not refresh symbols!", 
                                      ex);
                        m_exception = ex;
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            SExceptionDialog.open(Activator.getShell(), 
                                  "Refresh operation failed!",
                                  ex); 
        }
        
        if (m_exception != null  &&  isShowErrorMessage) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Refresh operation failed!",
                                  m_exception); 
        }
        
        model.getEventDispatcher().fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED));
    }

    
    /**
     * Core IDs may differ if user switches editors, which has different core IDs.
     * This should normally not happen - all iyaml files for one target should have
     * the same coreIDs.  
     *  
     * @param model
     * @param connection
     */
    protected boolean checkCoreIds(final TestSpecificationModel model,
                                final JConnection connection) {
        StrVector configuredCoreIds = new StrVector();
        model.getCEnvironmentConfiguration().getCoreIds(configuredCoreIds);
        StrVector connectedCoreIDs = connection.getMccMgr().getConnectedCoreIDs();
        
        // a special case - if no core IDs are configured, then connection is made to core ID with empty string
        if (configuredCoreIds.size() == 0  &&  connectedCoreIDs.size() == 1  &&  connectedCoreIDs.get(0).isEmpty()) {
            return true;
        }
    
        if (configuredCoreIds.size() != connectedCoreIDs.size()) {
            return false;
        }
        
        int numItems = (int) configuredCoreIds.size();
        for (int idx = 0; idx < numItems; idx++) {
            if (!configuredCoreIds.get(idx).equals(connectedCoreIDs.get(idx))) {
                return false;
            }
        }
        
        return true;
    }
    
    
    private void refresh(IProgressMonitor monitor,
                        JConnection defaultConnection,
                        TestSpecificationModel model) {
//        ISysUIUtils.execWinIDEAOperation(refreshOperation, Activator.getShell(), 
//                                         isShowErrorMessage,
//                                         ConnectionProvider.instance().getDefaultConnection());
        monitor.beginTask("Refreshing symbols", 3);
        
        GlobalsConfiguration.instance().refreshHeadless(monitor, 
                                                        defaultConnection,
                                                        model);  // worked(2)
        
        monitor.subTask("Refreshing groups ...");
        StopWatch sw = new StopWatch(); sw.start();
        model.refreshGroups();
        sw.stop();
        System.out.println("Groups refreshed: " + sw);
        
        monitor.worked(1);
    } 
}
