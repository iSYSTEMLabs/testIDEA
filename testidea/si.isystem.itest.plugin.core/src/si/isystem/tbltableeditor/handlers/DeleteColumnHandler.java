package si.isystem.tbltableeditor.handlers;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;

import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.HeaderNode;
import si.isystem.tbltableeditor.TableEditorSelection;
import si.isystem.tbltableeditor.TestBaseListModel;
import si.isystem.tbltableeditor.TestBaseTableSelectionProvider;
import de.kupzog.ktable.KTable;

public class DeleteColumnHandler extends TableEditorHandlerBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        removeColumns();
        return null;
    }

    
    private void removeColumns() {

        KTable table = UiUtils.getKTableInFocus();
        if (table == null) {
            System.out.println("KTable is not selected!");
            return;
        }
        
        TestBaseListModel kModel = (TestBaseListModel)table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        GroupAction removeAction = handleTestBaseList(kModel, arrayModel);
        
        if (removeAction.isEmpty()) {
            
            removeAction = new GroupAction("Remove sequence or mapping columns");
            
            Point[] selection = table.getCellSelection();
            HeaderNode header = arrayModel.getHeader(); 
            int fixedHeaderColumnCount = kModel.getFixedHeaderColumnCount();

            int[] cols = new int[selection.length];
            int idx = 0;
            for (Point pt  : selection) {
                cols[idx++] = pt.x;
            }

            // sort and remove in reverse order, otherwise removal indices of columns 
            // after the already removed ones should be decremented
            Arrays.sort(cols);

            for (int colIdx = cols.length - 1; colIdx >= 0; colIdx--) {
                int modelCol = cols[colIdx] - fixedHeaderColumnCount;
                HeaderNode node = header.getFirstNonEmptyCellBottomUp(modelCol);
                removeAction.add(arrayModel.createRemoveSeqOrUserMappingColumnAction(node));
            }
        }

        if (!removeAction.isEmpty()) {
            execActionAndRefresh(removeAction, table);
        }
    }


    private GroupAction handleTestBaseList(TestBaseListModel kModel, ArrayTableModel arrayModel) {
        
        GroupAction removeAction = new GroupAction("Remove CTestBase column");
        
        TestBaseTableSelectionProvider selectionProvider = kModel.getSelectionProvider();
        
        HeaderNode parentNode = null;
        HeaderNode clickedNode = null;
        
        ISelection selection = selectionProvider.getSelection();

        if (selection instanceof TableEditorSelection) {
            TableEditorSelection tblEditorSelection = (TableEditorSelection)selection;
            clickedNode = tblEditorSelection.getClickedNode();
            if (clickedNode == null) {
                return removeAction; 
            }
            parentNode = clickedNode.getParent();
        }

        if (clickedNode != null  &&  parentNode != null  &&
                clickedNode.isStructMapping()  &&  parentNode.isTestBaseList()) {
            
            removeAction.add(arrayModel.createRemoveTestBaseFromSubListAction(clickedNode));
        }    
        
        return removeAction;
    }
}
