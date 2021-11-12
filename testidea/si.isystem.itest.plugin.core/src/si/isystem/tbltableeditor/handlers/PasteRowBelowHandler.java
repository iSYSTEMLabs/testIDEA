package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Handler for context menu. */
public class PasteRowBelowHandler  extends PasteRowAboveHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        pasteInsertRows(true);
        return null;
    }

}
