package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

public class KeepTestResultsCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // see ToggelDebugModeHandler.java for comment on how to use command states.

        Command command = event.getCommand();
        HandlerUtil.toggleCommandState(command); // return old value
        
        return null;
    }

    
    public static boolean isKeepResults() {
        
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
        ICommandService commandService = (ICommandService)activeWorkbenchWindow.getService(ICommandService.class);
        
        Command command = commandService.getCommand("si.isystem.itest.commands.keepTestResults");
        
        Object state = command.getState(RegistryToggleState.STATE_ID).getValue();
        
        return ((Boolean)state).booleanValue();
    }
}