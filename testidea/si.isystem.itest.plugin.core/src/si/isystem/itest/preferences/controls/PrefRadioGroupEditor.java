package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

import si.isystem.ui.utils.UiTools;


public class PrefRadioGroupEditor extends RadioGroupFieldEditor {

    private boolean m_isInitialized = false;
    private String m_tooltip;
    private boolean m_isUseGroup;
    private String m_groupLayout;
    private boolean m_isUseFont;
    private Font m_groupTitleFont;
    private Composite m_control;
    
    /**
     * Creates a radio group field editor.
     * <p>
     * Example usage:
     * <pre>
     *      RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
     *          "GeneralPage.DoubleClick", resName, 1,
     *          new String[][] {
     *              {"Open Browser", "open"},
     *              {"Expand Tree", "expand"}
     *          },
     *          parent,
     *          true);  
     * </pre>
     * </p>
     * 
     * @param prefName the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param numColumns the number of columns for the radio button presentation
     * @param labelAndValues list of radio button [label, value] entries;
     *  the value is returned when the radio button is selected
     * @param parent the parent of the field editor's control
     * @param useGroup whether to use a Group control to contain the radio buttons
     */
    public PrefRadioGroupEditor(String prefName, String labelText, int numColumns,
            String[][] labelAndValues, Composite parent, boolean useGroup, 
            String groupLayout, String tooltip, boolean isUseFont, Font groupTitleFont) {

        super(prefName, labelText, numColumns, labelAndValues, parent, useGroup);
        m_isInitialized = true;
        m_isUseGroup = useGroup;
        m_groupLayout = groupLayout;
        m_tooltip = tooltip;
        m_isUseFont = isUseFont;
        m_groupTitleFont = groupTitleFont;
        
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
        
        m_control = null;
        if (!m_isUseGroup) {
            // probably bug - should create radio buttons!
            // m_control = getLabelControl(parent);
            m_control = getRadioBoxControl(parent);
        } else {
            m_control = getRadioBoxControl(parent);
            if (m_isUseFont) {
                m_control.setFont(m_groupTitleFont);
            } 
        }
        m_control.setLayoutData(m_groupLayout);
        UiTools.setToolTip(m_control, m_tooltip);
    }
    
    
    public Composite getRadioBox() {
        return m_control;
    }
}
