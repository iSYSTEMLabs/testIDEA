package si.isystem.itest.handlers;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;


public class FileOpenCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            // file open dialog starts in the same folder as the currently 
            // opened test case editor.
            TestSpecificationModel model = TestCaseEditorPart.getActiveModel();
            String primaryOpenPath = null;
            String secondaryOpenPath = null;
            
            if (model != null) {
                primaryOpenPath = model.getModelFileName();
                secondaryOpenPath = model.getOldModelFileName();
            }
            
            String fileName = UiUtils.showOpenIYamlFileDialog(Activator.getShell(), 
                                                              primaryOpenPath,
                                                              secondaryOpenPath,
                                                              true);
            if (fileName != null) {
                try {
                    // openEditor("D:\\bb\\trunk\\sdk\\mpc5554Sample\\Sample5554.iyaml");
                    openEditor(fileName);

                    // model.openTestSpec(fileName, 0);
                    // Activator.setTitle(fileName, model.getLicenseType());
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not open test specification file: " + fileName, 
                                          ex);
                }
            }
            
            /*
            TestSpecificationModel model = TestSpecificationModel.getInstance();

            int answer = UiUtils.askForModelSave();

            if (answer != 2) {
                String fileName = UiUtils.showOpenIYamlFileDialog(Activator.getShell(), 
                                                                  model.getModelFileName(),
                                                                  model.getOldModelFileName(),
                                                                  true);
                if (fileName != null) {
                    try {
                        model.openTestSpec(fileName, 0);
                        Activator.setTitle(fileName, model.getLicenseType());
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), 
                                              "Can not open test specification: " + fileName, 
                                              ex);
                    }
                }
            }
            */
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "File open failed!", ex);
        }

        
        return null;
    }

    
    public static void openEditorWCreate(String filePath) throws CoreException,
                                                                 PartInitException {
        if (!Files.exists(Paths.get(filePath))) {
            Path fPath = Paths.get(filePath);
            try {
                Files.createFile(fPath);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new SIOException("Creation of '" + filePath + "' failed!", 
                                       ex);
            }
        }
        
        openEditor(filePath);
    }
    
    
    public static void openEditor(String filePathStr) throws CoreException,
                                                          PartInitException {
        
        Path filePath = Paths.get(filePathStr);
        if (!Files.exists(filePath)) {
            throw new SIOException("File does not exist!").
                               add("fileName", filePathStr);
        }
        
        IWorkbench wb = Activator.getDefault().getWorkbench();
        IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        /* IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = workspaceRoot.getProject("ProjectArtifacts");
        if (!project.exists()) {
           project.create(null);
        }
        
        if (!project.isOpen()) {
           project.open(null);
        }
        
        IPath location = new Path(filePath);
        IFile itestFile = project.getFile(location.lastSegment());
        if (!itestFile.exists()) {
            itestFile.createLink(location, IResource.NONE, null);
        }
        
        // IFile itestFile = workspaceRoot.getFileForLocation(location);
        // IFile file = workspaceRoot.getFile(applicationControllerPath); */    
        
        try {
            String absURIPrefix = "";
            if (filePath.isAbsolute()) {
                absURIPrefix = "//";
            }
            IFileStore fileStore = EFS.getStore(new URI("file", absURIPrefix + filePathStr, ""));
            Activator.log(Status.INFO, "FileOpenCmdHandler - openEditor() S", null);
            page.openEditor(new FileStoreEditorInput(fileStore), TestCaseEditorPart.ID);
            Activator.log(Status.INFO, "FileOpenCmdHandler - openEditor() E", null);
        
        } catch (URISyntaxException ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not open test file: " + filePathStr, 
                                  ex);
        }
    }
}

/*
class MyInput implements IEditorInput
{

    @Override
    public Object getAdapter(Class adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "mw editr name";
    }

    @Override
    public IPersistableElement getPersistable() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getToolTipText() {
        // TODO Auto-generated method stub
        return "my edirt tooltip";
    }    
}
*/