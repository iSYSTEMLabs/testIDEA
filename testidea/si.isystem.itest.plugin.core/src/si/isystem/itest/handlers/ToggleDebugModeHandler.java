package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;

/*
 * Other option is creating check box as window control contribution:
public class DebugModeOnOffBtn extends WorkbenchWindowControlContribution {
    public DebugModeOnOffBtn() { }
    public DebugModeOnOffBtn(String id) { super(id); }
    @Override
    protected Control createControl(Composite parent) {
        Button button = new Button(parent, SWT.CHECK);
        button.setText("Debug mode");
        return button;
    }
}
 */


public class ToggleDebugModeHandler extends AbstractHandler {

    private static boolean m_isDebugMode = false;

    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        /**
         * Toggle commands MUST have state associated with them. It is also important 
         * to use the right class for this state, which should be either 
         *   org.eclipse.jface.commands.ToggleState
         * or
         *   org.eclipse.ui.handlers.RegistryToggleState:false
         * The later enables setting of initial value and persisting state value 
         * across sessions (<class='org.eclipse.ui.handlers.RegistryToggleState'><parameter
               name="persisted" value="false"> </parameter> </class>

         * Command ID MUST be 'org.eclipse.ui.commands.toggleState', so that
         * the 'HandlerUtil.toggleCommandState()' method finds it.
         * 
         * Note: The state does not contain button state (I didn't find out how to
         * read it), but command's state, which must be modified by this handler!
         *  
         * See also:
         * http://blog.eclipse-tips.com/2009/03/commands-part-6-toggle-radio-menu.html
         * http://wiki.eclipse.org/Menu_Contributions/Toggle_Button_Command
         * http://eclipsesource.com/blogs/2009/01/15/toggling-a-command-contribution/
         * 
         * To get command state value:
         * command.getState(RegistryToggleState.STATE_ID).getValue();
         * 
         * See also KeepTestResultsCmdHandler for usage of persistent toggle menu 
         * options.
         */
        try {
            Command command = event.getCommand();
            boolean oldValue = HandlerUtil.toggleCommandState(command);         

            m_isDebugMode = !oldValue;
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not toggle debug mode!", ex);
        }
        
        return null;
    }

    public static boolean isDebugMode() {
        return m_isDebugMode;
    }
}
