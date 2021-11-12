package si.isystem.tbltableeditor;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellSelectionListener;

/**
 * This class is registered with KTable and receives selection events. Base on 
 * selected sells it sets current selection in TestBaseTableSelectionProvider.
 * 
 * @see TestBaseTableSelectionProvider
 * @see TestBaseListTable.createControl
 * @author markok
 *
 */
public class CellSelectionListener implements KTableCellSelectionListener {

    private TestBaseTableSelectionProvider m_selectionProvider;
    private KTable m_table;


    public CellSelectionListener(KTable table, 
                                 TestBaseTableSelectionProvider selProvider) {
        m_table = table;
        m_selectionProvider = selProvider;
    }
    
    @Override
    public void cellSelected(int col, int row, int statemask) {
        
        // System.out.println("celSelected: " + row + " / " + col);
        
        TestBaseListModel kModel = (TestBaseListModel)m_table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        if (col == 0  &&  row == 0) {
            // top left corner cell, may later add cut/copy/paste/clear commands
            // for complete table here
            m_selectionProvider.setSelection(null);
        } else if (col == 0) {
            // rows header was clicked
            m_selectionProvider.setSelection(new RowSelection(m_table));
            
        } else if (col >= kModel.getFixedHeaderColumnCount()  &&  col < kModel.getColumnCount() 
                && row >= 0 && row < kModel.getRowCount()) {
            
            int modelCol = col - kModel.getFixedHeaderColumnCount();
            HeaderNode header = arrayModel.getHeader();
            int numHeaderRows = header.getRowCount();
            
            if (row < numHeaderRows) { // click in table header
                
                HeaderNode clickedNode = header.getNode(modelCol, row);
                
                if (clickedNode == null) {
                    clickedNode = header.getFirstNonEmptyCellBottomUp(modelCol);
                }
                
                HeaderNode parentNode = clickedNode.getParent();

                if (clickedNode.isDynamicContainer()  ||  parentNode.isDynamicContainer()) {  
                    
                    if (clickedNode.isDynamicContainer()  &&  clickedNode.getNumChildren() == 0) {
                        // type of selection is indicator for context menu to contain proper commands
                        m_selectionProvider.setSelection(new EmptyColumnSelection(m_table,
                                                                                  clickedNode,
                                                                                  parentNode));
                    } else {
                        m_selectionProvider.setSelection(new ColumnSelection(m_table, 
                                                                             clickedNode, 
                                                                             parentNode));
                    }
                } else {
                    // fixed struct column was clicked
                    m_selectionProvider.setSelection(new CellSelection(m_table, null, null));
                }
            } else {
                
                // data cell was clicked
                HeaderNode clickedNode = header.getFirstNonEmptyCellBottomUp(modelCol);
                
                if (clickedNode != null) { // happens when there are no items(headers) in the table

                    CellSelection selection;
                    HeaderNode userSeqOrMappingParent = clickedNode.getParent();

                    if (clickedNode.isUserMappingNode()  || clickedNode.isUserSequenceNode()) {

                        userSeqOrMappingParent = clickedNode;
                        clickedNode = null;
                        selection = new CellSelection(m_table, clickedNode, userSeqOrMappingParent);
                        
                    } else if (userSeqOrMappingParent.isUserMappingNode()  ||
                               userSeqOrMappingParent.isUserSequenceNode()) {
                        
                        selection = new CellSelection(m_table, clickedNode, userSeqOrMappingParent);
                        
                    } else {
                        selection = new CellSelection(m_table, null, null);
                    }

                    m_selectionProvider.setSelection(selection);
                }
            }
        } 
    }


    // never called in current implementation of KTable, since fixed cell are 
    // not selected - columns are 
    @Override
    public void fixedCellSelected(int col, int row, int statemask) {
        // System.out.println("fixed celSelected");
    }

}
