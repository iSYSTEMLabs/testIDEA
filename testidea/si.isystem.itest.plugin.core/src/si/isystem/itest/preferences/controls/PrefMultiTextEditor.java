package si.isystem.itest.preferences.controls;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.UiTools;

public class PrefMultiTextEditor extends FieldEditor {


    private boolean m_isInitialized = false;
    private String m_buttonLayoutData;
    private String m_textLayoutData;
    private String m_tooltip;
    private FieldContentProvider m_fieldContentProvider;
    private Text m_text;
    private String m_buttonText;

    public PrefMultiTextEditor(String prefName, 
                               String labelText, 
                               Composite parent,
                               String textMigLayout,
                               String buttonMigLayout,
                               String tooltip,
                               String buttonText,
                               FieldContentProvider fieldContentProvider) {
        super(prefName, labelText, parent);
        
        // workaround to prevent creation of controls before the layout can be set
        m_isInitialized = true;
        m_buttonLayoutData = buttonMigLayout;
        m_textLayoutData = textMigLayout;
        m_tooltip = tooltip;
        m_fieldContentProvider = fieldContentProvider;
        m_buttonText = buttonText;
        createControl(parent);
    }

    /** Provides value when the button is pressed. */
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
        getLabelControl(parent);

        getTextControl(parent);
        
        if (m_fieldContentProvider != null) {
            // create button
            Button button = new Button(parent, SWT.PUSH);
            button.setText(m_buttonText);
            button.setLayoutData(m_buttonLayoutData);
            button.addSelectionListener(new SelectionListener() {
                
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String path = changePressed();
                    if (path != null) {
                        m_text.setText(m_text.getText() + path + "\r\n");
                    }
                }
                
                @Override
                public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
    }
    
    
/*    public Text getTextControl() {
        return m_text;
    }
*/    
    
    private Text getTextControl(Composite parent) {
        if (m_text == null) {
            m_text= new Text(parent, SWT.MULTI | SWT.BORDER);
            m_text.setLayoutData(m_textLayoutData);
            if (m_tooltip != null) {
                UiTools.setToolTip(m_text, m_tooltip);
            }
        }
        
        return m_text;
    }
    
    
    /* public void setText(String text) {
        getTextControl().setText(text);
    } */
    

    // convenience method for CTestBase preferences, which have int ids.
    /* public void setPreferenceName(int id) {
        super.setPreferenceName(String.valueOf(id));
    } */

    @Override
    protected void adjustForNumColumns(int numColumns) {
        if (numColumns > 1) {
            m_text.setLayoutData("span " + numColumns);
        }
    }

    @Override
    protected void doLoad() {
        if (m_text != null) {
            String s = getPreferenceStore().getString(getPreferenceName());
            m_text.setText(s);
        }
    }


    @Override
    protected void doLoadDefault() {
        if (m_text != null) {
            m_text.setText("");
            String s = getPreferenceStore().getDefaultString(getPreferenceName());
            m_text.setText(s);
        }
    }

    
    @Override
    protected void doStore() {
        String s = m_text.getText().trim();
        getPreferenceStore().setValue(getPreferenceName(), s);
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }
}
