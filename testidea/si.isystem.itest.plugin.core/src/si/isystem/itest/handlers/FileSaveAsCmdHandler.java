package si.isystem.itest.handlers;
import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.FileOpenRecentDynamicMenu;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.TestSpecificationModel.SrcFileFormat;


public class FileSaveAsCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        Shell shell = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();

        saveModelAs(model, shell);

        return null;
    }


    public void saveModelAs(TestSpecificationModel model, Shell shell) {
        try {
            FileDialog fd = new FileDialog(shell, SWT.SAVE);
            fd.setText("Save As...");

            String oldFileName = model.getModelFileName();
            if (oldFileName != null  &&  !oldFileName.isEmpty()) {
                File ffile = new File(oldFileName);
                fd.setFilterPath(ffile.getParent());
            }

            openDialogAndSave(model, fd);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Save failed!", ex);
        }
    }

    
    /**
     * @return true if the model was saved, false otherwise
     */
    public boolean openDialogAndSave(TestSpecificationModel model,
                                     FileDialog fd) {

        boolean isSaved = false;
        TestSpecificationModel.SrcFileFormat fileFormat = model.getSrcFileFormat();
        String[] filterExt;
        if (fileFormat == SrcFileFormat.SRC_YAML) {
            String[] filter = {"*.iyaml", "*.*" };
            filterExt = filter;
        } else {
            String[] filter = {"*.c;*.cpp;*.h;*.hpp", "*.iyaml", "*.*" };
            filterExt = filter;
        }
        
        fd.setFilterExtensions(filterExt);
        
        String fileName = fd.open();

        if (fileName != null) {
            File file = new File(fileName);
            boolean shouldSave = true;
            if (file.exists()) {
                shouldSave = 
                    MessageDialog.openQuestion(fd.getParent(), "File already exists", "File '" + 
                                               fileName + "' already exists.\n" +
                                               "Do you want to overwrite it?");
            }
            
            if (shouldSave) {
                try {
                    model.saveModelAs(fileName);
                    isSaved = true;
                    
                    FileOpenRecentDynamicMenu.addFile(fileName);
                    
                    Activator.setTitle(fileName);
                    
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not save test specification", 
                                          ex);
                }
            }
        } 
        
        return isSaved;
    }
}
