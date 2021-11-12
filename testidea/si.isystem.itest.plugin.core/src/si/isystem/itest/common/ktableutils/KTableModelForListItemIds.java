package si.isystem.itest.common.ktableutils;

import org.eclipse.swt.graphics.Image;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EOverlayId;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.actions.testBaseList.InsertToTestBaseListAction;
import si.isystem.itest.model.actions.testBaseList.RemoveFromTestBaseListAction;
import si.isystem.itest.model.actions.testBaseList.SwapTestBaseListItemsAction;
import si.isystem.tbltableeditor.IModelWithComment;

public class KTableModelForListItemIds extends KTableSimpleModelBase implements IModelWithComment {

    private CTestBaseIdAdapter m_adapter;
    private CTestBase m_parentTestBase;


    public KTableModelForListItemIds(CTestBaseIdAdapter adapter) {

        m_adapter = adapter;
    }


    /** 
     * Sets data to be shown in table. 
     * @param data
     */
    public void setData(CTestBase listParent) {

        m_parentTestBase = listParent;
    }
    

    public String verifyModel() {
        
        CTestBaseList tbList = m_adapter.getItems(true);
        int nulItems = (int) tbList.size();
        for (int idx = 0; idx < nulItems; idx++) {
            CTestBase tb = tbList.get(idx);
            if (m_adapter.getId(tb).isEmpty()) {
                return "Please specify item in line: " + idx;
            }
        }
        return null;
    }


    @Override
    public int doGetRowCount() {
        return (int)m_adapter.getItems(true).size() + NUM_HDR_ROWS + NUM_TAIL_ROWS; 
    }

    
    @Override
    public Object doGetContentAt(int col, int row) {
        
        Object content = super.doGetContentAt(col, row);
        if (content != null) {
            return content;
        }
        
        int dataRow = row - NUM_HDR_ROWS;
        
        // text in cell
        CTestBaseList tbList = m_adapter.getItems(true);
        CTestBase testBase = tbList.get(dataRow);
        m_editableCellContent.setText(m_adapter.getId(testBase));
        
        // comment
        String[] comments = m_adapter.getComment(testBase, col - NUM_HDR_COLS);
        doGetComment(m_editableCellContent, comments[0], comments[1]);
        
        // result icon
        Boolean isError = m_adapter.isError(dataRow);
        m_editableCellContent.setIcon(EIconPos.EBottomLeft, getResultIcon(isError), true);
        
        return m_editableCellContent;
    }
    
    
    protected Image getResultIcon(Boolean isError) {
        
        if (isError != null) {
            if (isError.booleanValue()) {
                return IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY);
            } else {
                return IconProvider.getOverlay(EOverlayId.TEST_OK_OVERLAY);
            }
        }
        
        return null;
    }


    @Override
    public void doSetContentAt(int col, int row, Object value) {

        // System.out.println("doSetContentAt");
        if (row == 0  ||  row == getRowCount() - 1) {
            return;
        }

        String strVal = value == null ? "" : value.toString().trim();

        int dataRow = row - NUM_HDR_ROWS;

        CTestBaseList tbList = m_adapter.getItems(false);
        CTestBase testBase = tbList.get(dataRow);

        AbstractAction action = m_adapter.createSetIdAction(testBase, strVal);
        
        notifyListeners(action, testBase, false);
    }

    
    @Override
    public void addRow(int row) {
        CTestBase testBase = m_adapter.createNew(m_parentTestBase);
        int dataRow = row - NUM_HDR_ROWS;
        InsertToTestBaseListAction action = 
            new InsertToTestBaseListAction(m_adapter.getItems(false), testBase, dataRow);
       
        notifyListeners(action, m_parentTestBase, true);
    }
    
    
    @Override 
    public void removeRow(int row) {
        int dataRow = row - NUM_HDR_ROWS;
        RemoveFromTestBaseListAction action = 
                new RemoveFromTestBaseListAction(m_adapter.getItems(false), dataRow);

        notifyListeners(action, m_parentTestBase, true);
    }
        
    
    @Override
    public void swapRows(int first, int second) {
        CTestBaseList tbList = m_adapter.getItems(false);
        first -= NUM_HDR_ROWS;
        second -= NUM_HDR_ROWS;
        
        long listSize = tbList.size();
        if (first >= 0  &&  first < listSize  &&  second >= 0  &&  second < listSize) {

            boolean isMoveDown = second > first;
            SwapTestBaseListItemsAction action = 
                    new SwapTestBaseListItemsAction(tbList, first, 1, isMoveDown);
        
            notifyListeners(action, m_parentTestBase, true);
        }
    }

    
    @Override
    protected void setCellComment(int col, 
                                  int row,
                                  String newNlComment,
                                  String newEolComment) {
        if (col != DATA_COL_IDX  ||  row <= 0  ||  row >= getRowCount() - 1) {
            return;
        }

        int dataRow = row - NUM_HDR_ROWS;
        CTestBase testBase = m_adapter.getItems(false).get(dataRow);
        AbstractAction action = m_adapter.createSetCommentAction(testBase, 
                                                                 newNlComment,
                                                                 newEolComment);
        notifyListeners(action, testBase, true);
    }
    
    
    @Override
    public void createSetCommentAction(int col,
                                       int row,
                                       String nlComment,
                                       String eolComment,
                                       KTable table) {
    }


    @Override
    public AbstractAction createSetContentAction(int col,
                                                 int row,
                                                 String value,
                                                 String nlComment,
                                                 String eolComment) {

        doSetContentAt(col, row, value);
        return null;
    }
}    
