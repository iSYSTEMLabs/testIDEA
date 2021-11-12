package si.isystem.tbltableeditor;

import de.kupzog.ktable.KTable;


/**
 * Selection active when non-empty column is selected. Context menu condition
 * depends on this type of selection.
 *  
 * @author markok
 */
public class ColumnSelection extends TableEditorSelection {

    ColumnSelection(KTable table, HeaderNode clickedNode, HeaderNode userSeqOrMappingParent) {
        super(table, clickedNode, userSeqOrMappingParent);
    }
}
