package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.tbltableeditor.TestBaseListModel;
import de.kupzog.ktable.KTable;

public class DeleteRowHandler extends TableEditorHandlerBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        KTable table = UiUtils.getKTableInFocus();
        if (table == null) {
            return null;
        }
        
        TestBaseListModel kModel = (TestBaseListModel)table.getModel();
        GroupAction removeAction = kModel.createRemoveRowsAction(table.getCellSelection());
        
        execActionAndRefresh(removeAction, table);
        
        return null;
    }
}
