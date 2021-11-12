package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;


public class FileExitCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            HandlerUtil.getActiveWorkbenchWindow(event).close();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Exit failed!", ex);
        }
        
        return null;
    }
}
