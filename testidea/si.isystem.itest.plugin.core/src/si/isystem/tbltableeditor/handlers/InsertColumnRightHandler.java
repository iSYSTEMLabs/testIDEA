package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.kupzog.ktable.KTable;

/** Handler for context menu. */
public class InsertColumnRightHandler extends InsertColumnHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        KTable table = getKTable();
        insertColumn(table, true);
        return null;
    }

}
