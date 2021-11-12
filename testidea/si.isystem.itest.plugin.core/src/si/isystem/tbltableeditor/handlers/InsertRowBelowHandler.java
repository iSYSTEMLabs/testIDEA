package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


/** Handler for context menu. */
public class InsertRowBelowHandler extends InsertRowAboveHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        insertRow(true);
        return null;
    }

}
