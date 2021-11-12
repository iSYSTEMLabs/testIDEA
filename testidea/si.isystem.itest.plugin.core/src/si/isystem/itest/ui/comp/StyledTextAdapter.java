package si.isystem.itest.ui.comp;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;

import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class adapts StyleText control so that it can be used in TBControlText. 
 *  
 * @author markok
 *
 */
public class StyledTextAdapter implements ITextControl {

    private StyledText m_styledText;

    StyledTextAdapter(KGUIBuilder builder, String tooltip, String migLayoutData,
                      int swtStyle) {
        
        m_styledText = builder.styledText(migLayoutData, swtStyle);
        UiTools.setToolTip(m_styledText, tooltip);
    }

    
    @Override
    public void setEditable(boolean isEditable) {
        m_styledText.setEditable(isEditable);
    }
    
    
    /** Returns text from control. */
    @Override
    public String getText() {

        String str = m_styledText.getText().trim();
        return TBControlText.fixLineEndings(str);
    }
    

    @Override
    public void setText(String contents) {
        m_styledText.setText(contents);
    }

    
    @Override
    public Control getControl() {
        return m_styledText;
    }


    @Override
    public int getCaretPosition() {
        return m_styledText.getCaretOffset();
    }


    @Override
    public void setSelection(int pos) {
        m_styledText.setSelection(pos);        
    }
    

    @Override
    public void setStyleRanges(StyleRange[] ranges) {
        m_styledText.setStyleRanges(ranges);
    }
    

    @Override
    public boolean isEditable() {
        return m_styledText.getEditable();
    }
}
