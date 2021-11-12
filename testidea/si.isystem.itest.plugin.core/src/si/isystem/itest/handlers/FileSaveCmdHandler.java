package si.isystem.itest.handlers;
import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import si.isystem.commons.connect.ConnectionPool;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CIDEController.EPathType;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;

/**
 * IMPORTANT:
 * 
 * This handler is tied to the standard commandId="org.eclipse.ui.file.save"
 * command, otherwise there were problems with defining two commands for
 * the same shortcut Ctrl+S, which seems to be tied by default to the standard
 * save commend by the platform.
 * To always use this handler, an 'activeWhen' (which always evaluates to true)
 * condition has been added to this
 * handles in the plugin.xml, which makes it more specific than the default 
 * handler, and therefore selected by the platform.
 * 
 * @author markok
 *
 */
public class FileSaveCmdHandler extends AbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            TestCaseEditorPart editor = TestCaseEditorPart.getActive();
            editor.doSave(null);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Save failed!", ex);
        }
        
        return null;
    }


    /**
     * This method throws exceptions in case of failed save - this is important
     * for callers, which may cancel action, which depends on save, for example
     * exiting from application.
     * 
     * @return true if the model was saved, false otherwise
     */
    public boolean save() {
        boolean isSaved = false;
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        String fileName = model.getModelFileName();

//        TestSpecificationEditorView.saveGUIData();

        Shell shell = Activator.getShell();
        if (fileName == null  ||  fileName.isEmpty()) {

            FileDialog fd = new FileDialog(shell, SWT.SAVE);
            fd.setText("Save");
            
            String oldFileName = model.getModelFileName();
            if (oldFileName != null  &&  !oldFileName.isEmpty()) {
                File ffile = new File(oldFileName);
                fd.setFilterPath(ffile.getParent());
            } else {
                oldFileName = model.getOldModelFileName();
                if (oldFileName != null) {
                    File ffile = new File(oldFileName);
                    fd.setFilterPath(ffile.getParent());
                } else {
                    // get working dir from winIDEA, if connected
                    JConnection jCon = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);

                    if (jCon.isConnected()) {
                        try {
                            CIDEController ide = new CIDEController(jCon.getPrimaryCMgr());
                            String winIDEAWorkspaceDir = ide.getPath(EPathType.WORKSPACE_DIR);
                            
                            String workspaceFName = ide.getPath(EPathType.WORKSPACE_FILE_NAME);
                            workspaceFName = UiUtils.replaceExtension(workspaceFName, "iyaml");
                            
                            fd.setFileName(workspaceFName);
                            fd.setFilterPath(winIDEAWorkspaceDir);
                        } catch (Exception ex) {
                            // ignore - if anything goes wrong, user will be presented 
                            // a dialog without path/filename presets, which is the
                            // best we can do anyway
                        }
                    }
                }
            }
            
            FileSaveAsCmdHandler saveAsCmd = new FileSaveAsCmdHandler();
            isSaved = saveAsCmd.openDialogAndSave(model, fd);
        } else {
            // see also UIUtils.isReloadRequired()
            if (model.hasFileChanged()) {
                boolean ans = 
                    MessageDialog.openQuestion(shell, 
                                               "testIDEA", 
                                               "This file has been modified outside of testIDEA.\n" +
                                               "Do you want to save it anyway?");
                if (!ans) {
                    return false;
                }
            }
            
            model.saveModel();
            isSaved = true;
        }
        
        return isSaved;
    }
    
    
    public void fireEnabledStateChangedEvent() {
        fireHandlerChanged(new HandlerEvent(this, true, true));
    }
    
    @Override
    public boolean isEnabled() {
        // enabling/disabling of the save icon and command
        // the problem with the next line is that every control needs key listener to 
        // update toolbar icon on the fly.
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            return model.isModelDirty();
        }
        return false;
    }
    
    
    @Override
    public boolean isHandled() {
        // enabling/disabling of the save icon and command
        // the problem with the next line is that every control needs key listener to 
        // update toolbar icon on the fly.
        // return TestSpecificationModel.getInstance().getActionQueue().isModified();
        //System.err.println("is save handled " + TestSpecificationModel.getInstance().isModelDirty());
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            return model.isModelDirty();
        }
        return false;
    }
    
}
