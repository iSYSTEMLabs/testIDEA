package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;


public class EditUndoCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            // TestSpecificationEditorView.saveGUIData() - executed in model.undo(); below
            
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            model.undo();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Undo failed!", ex);
        }
            
        return null;
    }
    
    @Override
    public boolean isEnabled() {
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            return model.isUndoable();
        }
        
        return false;
    }

}
