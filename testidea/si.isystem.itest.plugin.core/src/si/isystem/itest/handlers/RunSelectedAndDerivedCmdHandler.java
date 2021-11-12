package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


public class RunSelectedAndDerivedCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        RunSelectedCmdHandler runHandler = new RunSelectedCmdHandler();
        runHandler.setRunDerived(true);
        
        return runHandler.execute(event);
    }

}
