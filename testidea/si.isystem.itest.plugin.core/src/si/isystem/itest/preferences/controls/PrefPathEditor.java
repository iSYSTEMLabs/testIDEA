package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.PathEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;

import si.isystem.ui.utils.UiTools;

/**
 * This class displays a list of paths in a list box, but stores them to 
 * preferences as a single string with path separators.
 * 
 * @author markok
 */
public class PrefPathEditor extends PathEditor {

    
    private boolean m_isInitialized = false;
    private String m_layoutData;
    private String m_tooltip;
    
    /**
     * Creates a path field editor.
     * 
     * @param prefName the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param dirChooserLabelText the label text displayed for the directory 
     *                            chooser dialog
     * @param parent the parent of the field editor's control
     */
    public PrefPathEditor(String prefName, String labelText,
            String dirChooserLabelText, Composite parent, 
            String layout, String tooltip) {
        
        super(prefName, labelText, dirChooserLabelText, parent);
        
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

        Control control = getLabelControl(parent);
        control.setLayoutData("wrap");

        List list = getListControl(parent);
        list.setLayoutData("spanx 2, grow");
        UiTools.setToolTip(list, m_tooltip);

        Composite buttonBox = getButtonBoxControl(parent);
        buttonBox.setLayoutData("w min!, alignx right, " + m_layoutData);
    }
    
    
    /* (non-Javadoc)
     * Method declared on ListEditor.
     * Creates a single string from the given array by separating each
     * string with the YAML list separator (comma).
    @Override
    protected String createList(String[] items) {
        StringBuffer path = new StringBuffer("");

        for (int i = 0; i < items.length; i++) {
            if (path.length() > 0) {
                path.append(", ");
            }
            path.append(StringValidator.quoteIfRequired(items[i]));
        }
        return path.toString();
    }
     */

    
    /* (non-Javadoc)
     * Splits string on YAML list separator (comma).
     */
    @Override
    protected String[] parseString(String stringList) {
        return stringList.split(", ");
    }
}
