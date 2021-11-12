package si.isystem.tbltableeditor;

import de.kupzog.ktable.KTable;

public class CellSelection extends TableEditorSelection {

    CellSelection(KTable table, HeaderNode clickedNode, HeaderNode userSeqOrMappingParent) {
        super(table, clickedNode, userSeqOrMappingParent);
    }
}
