package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.swttableeditor.ITableEditorRowDialog;
import si.isystem.swttableeditor.ITextFieldVerifier;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * Opens dialog with label and text field.
 * @author markok
 *
 */
public class SimpleEntryDialog extends Dialog implements ITableEditorRowDialog {

    
    private String m_titleText;
    private String m_label;
    private String m_tooltip;
    private Text m_inputField;
    private ITextFieldVerifier m_verifier;
    private String[] m_data;
    
    public SimpleEntryDialog(Shell parentShell, 
                             String title, 
                             String label, 
                             String tooltip) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_titleText = title;
        m_label = label;
        m_tooltip = tooltip;
        m_data = new String[1];
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);

        composite.getShell().setText(m_titleText);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 600;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fillx", "[min!][grow]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        builder.label(m_label);
        
        // String proposals[] = m_globalsProvider.getCachedGlobals();
        m_inputField = builder.text("width 100%, wrap");
        m_inputField.setToolTipText(m_tooltip);

        builder.separator("spanx 2, growx, gaptop 20", SWT.HORIZONTAL);

        return composite;
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
    protected void okPressed() {

        m_data[0] = m_inputField.getText();;
        
        if (m_verifier != null) {
            try {
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
            } catch (Exception ex) {
                SExceptionDialog.open(getParentShell(), "Invalid data", ex);
                return;
            }
        }
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    
    
    @Override
    public String[] getData() {
        return m_data;
    }

    @Override
    public boolean show() {
        return open() == Window.OK;
    }
}
