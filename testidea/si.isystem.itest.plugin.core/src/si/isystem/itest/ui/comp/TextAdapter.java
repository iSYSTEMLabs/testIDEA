package si.isystem.itest.ui.comp;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class TextAdapter implements ITextControl {

    private Text m_text;

    TextAdapter(KGUIBuilder builder, String tooltip, String migLayoutData, 
                int swtStyle) {
        
        m_text = builder.text(migLayoutData, swtStyle);
        UiTools.setToolTip(m_text, tooltip);
    }
    
//    @Override
//    public void createControl(KGUIBuilder builder, String tooltip, String migLayoutData, int swtStyle) {
//        m_text = builder.text(migLayoutData, swtStyle);
//        UiTools.setToolTip(m_text, tooltip);
//    }

    @Override
    public void setEditable(boolean isEditable) {
        m_text.setEditable(isEditable);
        m_text.setEnabled(isEditable);
    }

    @Override
    public String getText() {
        return m_text.getText();
    }

    @Override
    public void setText(String contents) {
        m_text.setText(contents);
    }

    @Override
    public Control getControl() {
        return m_text;
    }

    @Override
    public int getCaretPosition() {
        return m_text.getCaretPosition();
    }

    @Override
    public void setSelection(int pos) {
        m_text.setSelection(pos);
    }

    @Override
    public void setStyleRanges(StyleRange[] array) {
        // ignored, Text does not support styles
    }
    

    @Override
    public boolean isEditable() {
        return m_text.getEditable();
    }
}
