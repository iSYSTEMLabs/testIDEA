package si.isystem.itest.ui.comp;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Control;

/**
 * This interface is common denominator for Text and StyledText controls. 
 * @author markok
 *
 */
public interface ITextControl {

    Control getControl();
    
    void setEditable(boolean isEditable);
    
    
    /** Returns text from control. */
    String getText();
    
    
    public void setText(String contents);

    int getCaretPosition();

    void setSelection(int pos);

    void setStyleRanges(StyleRange[] array);
    
    public boolean isEditable();
}
