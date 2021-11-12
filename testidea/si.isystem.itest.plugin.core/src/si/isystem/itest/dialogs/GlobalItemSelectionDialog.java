package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.swttableeditor.ITableEditorRowDialog;
import si.isystem.swttableeditor.ITextFieldVerifier;
import si.isystem.ui.utils.KGUIBuilder;

public class GlobalItemSelectionDialog extends Dialog implements ITableEditorRowDialog {

    
    private String m_titleText;
    private String m_label;
    private String m_tooltip;
    private GlobalsSelectionControl m_inputField;
    private ITextFieldVerifier m_verifier;
    private String[] m_data;
    private boolean m_isShowSourceButton;
    private String m_globalsProviderId;
    private String m_coreId;
    
    public GlobalItemSelectionDialog(Shell parentShell, 
                                     String title, 
                                     String label, 
                                     String tooltip, 
                                     boolean isShowSourceButton,
                                     String globalsProviderId,
                                     String coreId) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_titleText = title;
        m_label = label;
        m_tooltip = tooltip;
        m_data = new String[1];
        m_isShowSourceButton = isShowSourceButton;
        m_globalsProviderId = globalsProviderId;
        m_coreId = coreId;
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
        
        m_inputField = new GlobalsSelectionControl(mainDlgPanel, 
                                                   "width 100%, wrap",
                                                   null,
                                                   null,
                                                   SWT.NONE,
                                                   m_globalsProviderId,
                                                   m_coreId,
                                                   true,
                                                   m_isShowSourceButton,
                                                   ContentProposalAdapter.PROPOSAL_REPLACE,
                                                   UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                   GlobalsConfiguration.instance().getGlobalContainer(),
                                                   ConnectionProvider.instance());
        m_inputField.setToolTipText(m_tooltip);

        Label separator = new Label(mainDlgPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData("spanx 2, growx, gaptop 20");

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


    public void setGlobalsProvider(String globalsProviderId, String coreId) {
        m_globalsProviderId = globalsProviderId;
        m_coreId = coreId;
    }


    @Override
    protected void okPressed() {
        // String globalName = m_inputField.getControl().getText(); 
        // String functionName = m_inputField.parseNameWithType(globalName, null)[0]; 

        m_data[0] = m_inputField.getControl().getText().trim();
        
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


    public void refreshProposals() {
        m_inputField.refreshProposals();
    }

}
