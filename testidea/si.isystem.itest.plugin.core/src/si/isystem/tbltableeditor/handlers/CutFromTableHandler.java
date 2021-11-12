package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.tbltableeditor.TestBaseListModel;

public class CutFromTableHandler extends CopyFromTableHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        AbstractAction action = copySelectionToClipboard(true);
        
        if (action != null) {
            if (m_table.getModel() instanceof TestBaseListModel) {
                execActionAndRefresh(action, m_table);
            } else {
                action.addAllFireEventTypes();
                TestSpecificationModel.getActiveModel().execAction(action);
                m_table.redraw();
            }
        }
        
        return null;
    }
}
