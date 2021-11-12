package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import si.isystem.ui.utils.UiTools;

public class PrefColorEditor extends ColorFieldEditor {

    private String m_layoutData;
    private boolean m_isInitialized = false;
    private String m_tooltip;
    
    /**
     * Creates a color field editor.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param parent
     *            the parent of the field editor's control
     */
    public PrefColorEditor(String name, String labelText, Composite parent, 
                           String layoutData, String tooltip) {
        super(name, labelText, parent);
        m_isInitialized = true;
        m_layoutData = layoutData;
        m_tooltip = tooltip;
        createControl(parent);
    }
    
    
    @Override
    protected void createControl(Composite parent) {
        if (!m_isInitialized) {
            return;
        }

        Control control = getLabelControl(parent);
        control.setLayoutData("");
        if (m_tooltip !=  null) {
            UiTools.setToolTip(control, m_tooltip);
        }

        Button colorButton = getChangeControl(parent);
        colorButton.setLayoutData(m_layoutData);
        
    }

    // convenience method for CTestBase preferences, which have int ids.
    public void setPreferenceName(int id) {
        super.setPreferenceName(String.valueOf(id));
    }
}
