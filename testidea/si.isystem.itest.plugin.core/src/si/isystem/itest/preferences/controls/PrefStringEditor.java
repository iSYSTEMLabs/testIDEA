package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.UiTools;

public class PrefStringEditor extends StringFieldEditor {

    private String m_layoutData;
    private boolean m_isInitialized = false;
    private String m_tooltip;

    public PrefStringEditor(int name, String labelText, 
                            int strategy, Composite parent, 
                            String layout,
                            String tooltip) {
        this(String.valueOf(name), labelText, strategy, parent, layout, tooltip);
    }
    
    public PrefStringEditor(String name, String labelText, 
                            int strategy, Composite parent, 
                            String layout,
                            String tooltip) {
        super(name, labelText, strategy, parent);
        
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
    

    /* public void setText(String text) {
        getTextControl().setText(text);
    } */
    
    
    @Override
    protected void doStore() {
        getPreferenceStore().setValue(getPreferenceName(), getStringValue());
    }
}
