package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.UiTools;

public class PrefDirectoryEditor extends DirectoryFieldEditor {

    private String m_layoutData;
    private boolean m_isInitialized = false;
    private String m_tooltip;
    private Composite m_parentOfControls;

    /**
     * Creates a directory field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param validationStrategy either {@link StringButtonFieldEditor#VALIDATE_ON_KEY_STROKE}
     *  to perform on the fly checking, or {@link StringButtonFieldEditor#VALIDATE_ON_FOCUS_LOST}
     *  (the default) to perform validation only after the text has been typed in
     * @param parent the parent of the field editor's control.
     * @since 3.4
     * @see StringButtonFieldEditor#VALIDATE_ON_KEY_STROKE
     * @see StringButtonFieldEditor#VALIDATE_ON_FOCUS_LOST
     */
    public PrefDirectoryEditor(String name, String labelText, Composite parent,
            int validationStrategy, String layout, String tooltip) {
        super(name, labelText, parent);

        setValidateStrategy(validationStrategy);
        
        // workaround to prevent creation of controls before the layout can be set
        m_isInitialized = true;
        m_layoutData = layout;
        m_tooltip = tooltip;
        createControl(parent);
        // setChangeButtonText("Browse");
    }
    
    // methods, which override StringFieldEditor
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        throw new IllegalStateException("Method should not be called - Mig layout is used!");
    }
    
    @Override
    protected void createControl(Composite parent) {
        if (!m_isInitialized) {
            return;
        }
        m_parentOfControls = parent;
        getLabelControl(parent);

        Text textField = getTextControl(parent);
        textField.setLayoutData("");
        if (m_tooltip != null) {
            UiTools.setToolTip(textField, m_tooltip);
        }
        // create button
        Button button = getChangeControl(parent);
        button.setLayoutData(m_layoutData);
    }
    
    
    public void setText(String text) {
        getTextControl().setText(text);
    }
    
    
    // methods, which override StringButtnonFieldEditor
    
    /**
     * Sets the text of the change button.
     *
     * @param text the new text
     */
    @Override
    public void setChangeButtonText(String text) {
        
        Button changeButton = null;
    
        if (m_isInitialized) {
            changeButton = getChangeControl(m_parentOfControls); // button should've  been
                                                   // created before this call
        }
        
        if (changeButton != null) {
        
            // preserve layout data
            Object layoutData = changeButton.getLayoutData();
            // set griddata layout so that cast in superclass will not fail 
            changeButton.setLayoutData(new GridData());
            super.setChangeButtonText(text);
            // restore layout
            changeButton.setLayoutData(layoutData);
        }
    }

    
    // convenience method for CTestBase preferences, which have int ids.
    public void setPreferenceName(int id) {
        super.setPreferenceName(String.valueOf(id));
    }
}
