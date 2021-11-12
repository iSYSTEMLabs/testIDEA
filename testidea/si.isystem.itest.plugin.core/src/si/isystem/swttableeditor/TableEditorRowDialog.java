package si.isystem.swttableeditor;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.KGUIBuilder;


/**
 * This class implements the default dialog, where all input fields are 
 * presented by Text fields.<p>
 * 
 * Example:
 * <pre>
 *     TableEditorRowDialog addDlg = new TableEditorRowDialog(parent.getShell(), 
 *                                                            new String[]{"Name:"},
 *                                                            new String[]{"The name of customer."});
 *     ITextFieldVerifier verifier = new ITextFieldVerifier() {
 *           
 *         @Override
 *         public String verify(String[] data) {
 *             if (data[0].trim().length() == 0) {
 *                 return "Customer name must not be empty!";
 *             }
 *             return null;
 *         }
 *     };
 *       
 *     addDlg.setVerifier(verifier);
 *       
 *     m_customerTableEditor.setAddDialog(addDlg);
 * </pre>
 * 
 * @author markok
 *
 */
public class TableEditorRowDialog extends Dialog implements ITableEditorRowDialog {

    private String m_titleText;
    private String[] m_labels;
    private Text[] m_inputFields;
    private String[] m_tooltips;
    private ITextFieldVerifier m_verifier;
    private String[] m_data;

    /**
     * Created dialog.
     * 
     * @param parentShell parent shell
     * @param title text displayed in dialog title bar
     * @param labels text written left to text input fields
     * @param tooltips tooltips for input fields
     */
    public TableEditorRowDialog(Shell parentShell, String title, String []labels, String []tooltips) {
        super(parentShell);
    
        if (labels.length != tooltips.length) {
            throw new IllegalArgumentException("'labels' and 'tooltips' arrays must have the same size!");
        }
        
        m_titleText = title;
        m_labels = labels;
        m_tooltips = tooltips;
        m_inputFields = new Text[m_labels.length];
    }

    /**
     * Sets verifier for user data. If this method is not called, or verifier == null,
     * user data is not verified.
     * 
     * @param verifier class to verify the data
     */
    @Override
    public void setVerifier(ITextFieldVerifier verifier) {
        m_verifier = verifier;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText(m_titleText);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        
        mainDlgPanel.setLayout(new MigLayout("fillx", "[min!][250]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        int idx = 0;
        for (String label : m_labels) {
            builder.label(label);
            m_inputFields[idx] = builder.text("width 100%, wrap", SWT.BORDER);
            m_inputFields[idx].setToolTipText(m_tooltips[idx]);
            idx++;
        }

        Label separator = new Label(mainDlgPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData("spanx 2, growx, gaptop 20");

        return composite;
    }
    
    
    /* protected Control createButtonBar(Composite parent) {
        
    } */
    
    // saves text data when it is still available
    private void saveTextData() {
        m_data = new String[m_inputFields.length];
        
        int idx = 0;
        for (Text inputField : m_inputFields) {
            m_data[idx++] = inputField.getText();
        }
    }
    
    @Override
    public String[] getData() {
        return m_data;
    }


    @Override
    protected void okPressed() {
        saveTextData();
        
        if (m_verifier != null) {
            String result = m_verifier.format(m_data);
            
            if (result != null) {
                MessageDialog.open(MessageDialog.ERROR, getParentShell(), 
                                   "Invalid data", result, SWT.NONE);
                return;
            }

            result = m_verifier.verify(m_data);
            
            if (result != null) {
                MessageDialog.open(MessageDialog.ERROR, getParentShell(), 
                                   "Invalid data", result, SWT.NONE);
                return;
            }
        }
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    @Override
    public boolean show() {
        return open() == Window.OK;
    }

}
