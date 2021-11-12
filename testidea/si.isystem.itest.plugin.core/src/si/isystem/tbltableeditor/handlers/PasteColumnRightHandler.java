package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Handler for context menu. */
public class PasteColumnRightHandler extends PasteColumnLeftHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        pasteInsertColumns(true);
        return null;
    }

}
