package si.isystem.itest.handlers;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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


public class FileNewCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();
        
        try {
            FileDialog fd = new FileDialog(shell, SWT.SAVE);
            fd.setText("Enter file name ...");

            TestSpecificationModel model = TestSpecificationModel.getActiveModel();

            if (model != null) {
                String oldFileName = model.getModelFileName();
                if (oldFileName != null  &&  !oldFileName.isEmpty()) {
                    File ffile = new File(oldFileName);
                    fd.setFilterPath(ffile.getParent());
                }
            }

            String[] filter = {"*.iyaml", "*.*" };

            fd.setFilterExtensions(filter);

            String fileName = fd.open();

            if (fileName != null) {
                
                boolean shouldOverwrite = true;
                Path filePath = Paths.get(fileName);
                
                if (Files.exists(filePath)) {
                    shouldOverwrite = 
                            MessageDialog.openQuestion(fd.getParent(), "File already exists", "File '" + 
                                    fileName + "' already exists.\n" +
                                    "Do you want to overwrite it?");
                    if (shouldOverwrite) {
                        Files.deleteIfExists(filePath);
                    }
                }

                if (shouldOverwrite) {
                    // similar initialization is done in NewITestFileWizard 
                    String contents = TestSpecificationModel.createDefaultTestBenchAsString();
                    
                    try (BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.defaultCharset())) {
                        writer.write(contents);
                    }
                    
                    FileOpenCmdHandler.openEditor(fileName);
                    
                    FileOpenRecentDynamicMenu.addFile(fileName);

                    Activator.setTitle(fileName);

                }
            } 
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not create new test specification file!", ex);
        }

        return null;
    }

}
