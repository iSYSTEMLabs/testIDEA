package si.isystem.itest.ui.spec.data;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.handlers.FileOpenCmdHandler;
import si.isystem.itest.main.Activator;


public class TestStatusViewDropListener extends ViewerDropAdapter {

    private enum ETransferType {ETestSpecOrGroup, EFile};
    private ETransferType m_transferType;

    public TestStatusViewDropListener(StructuredViewer viewer) {
        super(viewer);
    }

    /**
     * This method is called on drop action. The super.drop() method then calls
     * performDrop(), which is implemented below.  
     */
    @Override
    public void drop(DropTargetEvent event) {
        try {
            super.drop(event);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not perform drag and drop operation!", ex);
        }
    }

    // This method performs the actual drop
    // We simply add the String we receive to the model and trigger a refresh of the 
    // viewer by calling its setInput method.
    @Override
    public boolean performDrop(Object data) {
        switch (m_transferType) {
        case EFile:
            return dropFile((String[])data);
        default:
            SExceptionDialog.open(Activator.getShell(), 
                                  "Invalid type of dropped object!", 
                                  new Exception());
            break;
        }
        return false;
    }
    
    
    private boolean dropFile(String[] data) {
        if (data.length != 1) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Only one file may be dropped, " + data.length + 
                                  " files were dropped!", new Exception());
            return false;
        }
        
        String fileName = data[0];
        try {
            try {
                FileOpenCmdHandler.openEditor(fileName);

                Activator.setTitle(fileName);

            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), 
                                      "Can not open test specification file: " + fileName, 
                                      ex);
            }
                
            return true;
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not open file!", ex);
        }
        
        return false;
    }

    
    @Override
    public boolean validateDrop(Object target, int operation, TransferData transferData) {
        if (FileTransfer.getInstance().isSupportedType(transferData)) {
            m_transferType = ETransferType.EFile;
            return true;
        }
        return false;
    }
}
