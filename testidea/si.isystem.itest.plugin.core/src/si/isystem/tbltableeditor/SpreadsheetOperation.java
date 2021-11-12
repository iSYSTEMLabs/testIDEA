package si.isystem.tbltableeditor;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.swt.graphics.Point;

import si.isystem.itest.model.actions.GroupAction;

public interface SpreadsheetOperation {

    GroupAction op(TestBaseListTable tblTable,
                   Point[] selectedCells,
                   String[] contents,  // may be null if it is calculated in this method 
                   MutableBoolean isExistReadOnlyCells);
}


