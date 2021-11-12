package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.UiTools;

public class PrefIntegerEditor extends IntegerFieldEditor {

    private boolean m_isInitialized = false;
    private String m_layoutData;
    private String m_tooltip;

    
    /**
     * Creates an integer field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     * @param textLimit the maximum number of characters in the text.
     */
    public PrefIntegerEditor(String name, String labelText, Composite parent,
                             int textLimit, String layout, String tooltip) {
        super(name, labelText, parent, textLimit);

        // workaround to prevent creation of controls before the layout can be set
        m_isInitialized = true;
        m_layoutData = layout;
        m_tooltip = tooltip;

        createControl(parent);
    }

    
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        throw new IllegalStateException("Method should not be called - Mig layout is used!");
    }
    
    @Override
    protected void createControl(Composite parent) {
        if (!m_isInitialized) {
            return;
        }
        getLabelControl(parent);

        Text textField = getTextControl(parent);
        textField.setLayoutData(m_layoutData);
        if (m_tooltip != null) {
            UiTools.setToolTip(textField, m_tooltip);
        }
    }
    
    
    public void setText(String text) {
        getTextControl().setText(text);
    }
    
}
