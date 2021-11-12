package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.TestTreeOutline;
import si.isystem.ui.utils.ColorProvider;

public class ToggleDryRunHandler extends AbstractHandler {

    private static boolean m_isDryRunMode = false;

    
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

            m_isDryRunMode = !oldValue;
            
            TestTreeOutline outline = TestCaseEditorPart.getOutline();
            if (outline != null) {
                if (m_isDryRunMode) {
                    outline.setBackgroundColor(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
                } else {
                    outline.setBackgroundColor(ColorProvider.instance().getBkgColor());
                }
            }
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not toggle Dry Run mode!", ex);
        }
        
        return null;
    }

    
    public static boolean isDryRunMode() {
        return m_isDryRunMode;
    }
}
