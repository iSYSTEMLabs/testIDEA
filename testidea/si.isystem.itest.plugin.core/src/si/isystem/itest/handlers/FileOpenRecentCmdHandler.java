package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;

public class FileOpenRecentCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {

            String fileName = event.getParameter("si.isystem.itest.commands.openRecentParams");
            if (fileName != null) {

                try {

                    FileOpenCmdHandler.openEditor(fileName);

                    Activator.setTitle(fileName);

                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not open test specification file!", 
                                          ex);
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "File open failed!", ex);
        }
        
        return null;
    }

}
