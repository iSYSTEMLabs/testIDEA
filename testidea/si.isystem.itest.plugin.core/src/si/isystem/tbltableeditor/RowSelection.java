package si.isystem.tbltableeditor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;

import de.kupzog.ktable.KTable;

/**
 * Selection active when row is selected. Context menu condition
 * depends on this type of selection. 
 */
public class RowSelection implements ISelection {

    private KTable m_table;

    RowSelection(KTable table) {
        m_table = table;
    }
    
    
    @Override
    public boolean isEmpty() {
        
        Point[] cellSelection = m_table.getCellSelection();
        return cellSelection == null  ||  cellSelection.length > 0;
    }
}
