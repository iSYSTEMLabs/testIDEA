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

public class SwapRowsHandler extends TableEditorHandlerBase {

    private boolean m_isMoveDown;


    public SwapRowsHandler(boolean isMoveDown) {
        m_isMoveDown = isMoveDown;
    }
    
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        swapRows(m_isMoveDown);
        return null;
    }

    
    protected void swapRows(boolean isMoveDown) {
        try {
            KTable table = UiUtils.getKTableInFocus();
            if (table == null) {
                return;
            }
            
            TestBaseListModel kModel = (TestBaseListModel)table.getModel();
            ArrayTableModel arrayModel = kModel.getArrayModel();
            
            Rectangle selRect = table.getSelectedRect();
            int startRowIndex = selRect.y - kModel.getFixedHeaderRowCount();
            
            AbstractAction action = arrayModel.createSwapRowsAction(startRowIndex,
                                                                    selRect.height + 1, 
                                                                    isMoveDown);
            execActionAndRefresh(action, table);
        } catch (Exception ex) {
            // if shell is not null, but Activator.getShell(), then table context menus no 
            // longer show after this error dialog opened
            SExceptionDialog.open(null, "Swap error", ex);
        }
    }
}
