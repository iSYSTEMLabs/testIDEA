package si.isystem.tbltableeditor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;

import de.kupzog.ktable.KTable;

public class TableEditorSelection implements ISelection{

    private KTable m_table;
    protected HeaderNode m_clickedNode;
    protected HeaderNode m_userSeqOrMappingNode;

    
    TableEditorSelection(KTable table, HeaderNode clickedNode, HeaderNode userSeqOrMappingParent) {
        m_table = table;
        m_clickedNode = clickedNode; 
        m_userSeqOrMappingNode = userSeqOrMappingParent;
    }
    
    
    @Override
    public boolean isEmpty() {
        
        Point[] cellSelection = m_table.getCellSelection();
        return cellSelection == null  ||  cellSelection.length > 0;
    }

    
    /**
     * @return the clickedNode
     */
    public HeaderNode getClickedNode() {
        return m_clickedNode;
    }


    /**
     * @return the userSeqOrMappingParent
     */
    public HeaderNode getUserSeqOrMappingParent() {
        return m_userSeqOrMappingNode;
    }

    
    
}
