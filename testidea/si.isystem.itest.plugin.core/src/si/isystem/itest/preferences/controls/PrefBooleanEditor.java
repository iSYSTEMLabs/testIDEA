package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import si.isystem.ui.utils.UiTools;

public class PrefBooleanEditor extends BooleanFieldEditor {

    private String m_layoutData;
    private boolean m_isInitialized = false;
    private String m_tooltip;
    private int m_style;
    private Composite m_parent;


    public PrefBooleanEditor(int name, String labelText, int style, Composite parent,
                             String layout, String tooltip) {
        this(String.valueOf(name), labelText, style, parent, layout, tooltip);
    }

    /**
     * Creates a boolean field editor in the given style.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param style
     *            the style, either <code>DEFAULT</code> or
     *            <code>SEPARATE_LABEL</code>
     * @param parent
     *            the parent of the field editor's control
     * @see #DEFAULT
     * @see #SEPARATE_LABEL
     */
    public PrefBooleanEditor(String name, String labelText, int style, Composite parent,
                             String layout, String tooltip) {
        super(name, labelText, style, parent);
        
        // workaround to prevent creation of controls before the layout can be set
        m_isInitialized = true;
        
        m_layoutData = layout;
        m_tooltip = tooltip;
        m_style = style;
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

        m_parent = parent;
        String text = getLabelText();
        switch (m_style) {
        case SEPARATE_LABEL:
            getLabelControl(parent);
            text = null;
            //$FALL-THROUGH$
        default:
            Button checkBox = getChangeControl(parent);
            checkBox.setLayoutData(m_layoutData);
            if (text != null) {
                checkBox.setText(text);
            }
            if (m_tooltip != null) {
                UiTools.setToolTip(checkBox, m_tooltip);
            }
        }
    }
    
    // convenience method for CTestBase preferences, which have int ids.
    public void setPreferenceName(int id) {
        super.setPreferenceName(String.valueOf(id));
    }
    
    public Button getCheckBoxControl() {
        return getChangeControl(m_parent);
    }
}
