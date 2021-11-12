package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Rectangle;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.AbstractAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.TestBaseListModel;
import de.kupzog.ktable.KTable;

/** Handler for context menu. */
public class InsertRowAboveHandler extends TableEditorHandlerBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        insertRow(false);
        return null;
    }

    protected void insertRow(boolean isInsertBelow) {
        try {
            KTable table = UiUtils.getKTableInFocus();
            if (table == null) {
                return;
            }
            
            TestBaseListModel kModel = (TestBaseListModel)table.getModel();
            ArrayTableModel arrayModel = kModel.getArrayModel();
            
            Rectangle selRect = table.getSelectedRect();
            int pasteRowIndex = selRect.y - kModel.getFixedHeaderRowCount();
            if (isInsertBelow) {
                pasteRowIndex += 1 + selRect.height; // insert below selection
            }
            
            AbstractAction action = arrayModel.createAddRowAction(pasteRowIndex);
            execActionAndRefresh(action, table);
        } catch (Exception ex) {
            // if shell is not null, but Activator.getShell(), then table context menus no 
            // longer show after this error dialog opened
            SExceptionDialog.open(null, "Clipboard operation error", ex);
        }
    }
}
