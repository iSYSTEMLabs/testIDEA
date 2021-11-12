package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.ReferenceStorage;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;


public class EditRedoCmdHandler extends AbstractHandler {

    public EditRedoCmdHandler() {
        ReferenceStorage.setEditRedoCmdHandler(this);
    }

    
    public void fireEnabledStateChangedEvent() {
        fireHandlerChanged(new HandlerEvent(this, true, true));
    }
    
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
//            TestSpecificationEditorView.saveGUIData();
            
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            model.redo();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Redo failed!", ex);
        }
        return null;
    }
    
    @Override
    public boolean isEnabled() {
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            return model.isRedoable()  && !model.isSectionEditorDirty();
        }
        
        return false;
    }
}
