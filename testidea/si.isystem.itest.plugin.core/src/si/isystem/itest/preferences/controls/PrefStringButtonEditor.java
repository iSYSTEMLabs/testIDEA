package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.UiTools;


public class PrefStringButtonEditor extends StringButtonFieldEditor {


    private boolean m_isInitialized;
    private String m_layoutData;
    private String m_tooltip;
    private Composite m_parentOfControls;
    private FieldContentProvider m_fieldContentProvider;

    public PrefStringButtonEditor(String prefName, 
                                  String labelText, 
                                  Composite parent,
                                  String layout,
                                  String tooltip,
                                  String buttonText,
                                  FieldContentProvider fieldContentProvider) {
        super(prefName, labelText, parent);
        init(prefName, labelText);
        
        setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
        
        // workaround to prevent creation of controls before the layout can be set
        m_isInitialized = true;
        m_layoutData = layout;
        m_tooltip = tooltip;
        m_fieldContentProvider = fieldContentProvider;
        createControl(parent);
        setChangeButtonText(buttonText);
    }

    
    @Override
    protected String changePressed() {
        return m_fieldContentProvider.getValue();
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
    
    
    // methods, which override StringButtonFieldEditor
    
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
