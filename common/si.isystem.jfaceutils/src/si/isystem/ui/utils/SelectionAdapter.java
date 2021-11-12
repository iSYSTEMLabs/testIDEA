package si.isystem.ui.utils;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * Use this class instead of SelectionListener when method widgetDefaultSelected() 
 * does not need to be implemented.
 *  
 * Advantage of this class over the one provided by Eclipse is automatic generation of
 * the missing method by Eclipse editor.
 * 
 * @author markok
 *
 */
abstract public class SelectionAdapter implements SelectionListener {

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
