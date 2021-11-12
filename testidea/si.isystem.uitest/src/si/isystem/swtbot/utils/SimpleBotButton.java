package si.isystem.swtbot.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;


/**
 * This class overrides the click method and generates only the necessary events.
 * It is used on application exit, when the button disappears immediately after 
 * mouseUp event, so we get an exception with SWTBotButton.click()
 * @author markok
 */
public class SimpleBotButton extends SWTBotButton {

    public SimpleBotButton(Button button) {
        super(button);
    }

    
    public SWTBotButton click() {
        log.debug(MessageFormat.format("Clicking on {0}", SWTUtils.getText(widget))); //$NON-NLS-1$
        waitForEnabled();
        notify(SWT.MouseEnter);
        notify(SWT.MouseMove);
        notify(SWT.Activate);
        notify(SWT.FocusIn);
        notify(SWT.MouseDown);
        notify(SWT.MouseUp);
        notify(SWT.Selection);
        log.debug(MessageFormat.format("Clicked on {0}", SWTUtils.getText(widget))); //$NON-NLS-1$
        return this;
    }
}
