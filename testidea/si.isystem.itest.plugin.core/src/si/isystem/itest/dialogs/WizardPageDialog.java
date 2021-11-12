package si.isystem.itest.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import net.miginfocom.swt.MigLayout;
import si.isystem.itest.wizards.newtest.GlobalsWizardDataPage;

/**
 * This class can be used to show a dialog, which contains a page, which was 
 * originally implemented for a wizard.
 */
public class WizardPageDialog extends Dialog {

    private GlobalsWizardDataPage m_wizardPage;

    public WizardPageDialog(Shell parentShell, 
                            GlobalsWizardDataPage wizardPage) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_wizardPage = wizardPage;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        
        Composite composite = (Composite)super.createDialogArea(parent);
        getShell().setText(m_wizardPage.getTitle());

        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 1;
        gridData.heightHint = 400;  // sets initial dialog size
        gridData.widthHint = 950;
        mainDlgPanel.setLayoutData(gridData);
        mainDlgPanel.setLayout(new MigLayout("fill", "", ""));
        
        Composite page = m_wizardPage.createPage(mainDlgPanel);
        page.setLayoutData("wmin 0, grow"); // essential to prevent pane to extend 
           // past parent on resize, and grow is needed in spite of fill in MigLayout.
        m_wizardPage.dataFromModel();
        
        return composite;
    }
    
    
    @Override
    protected void okPressed() {
        m_wizardPage.dataToModel();
        
        super.okPressed();
    }
    
    
    public boolean show() {
        return open() == Window.OK;
    }
}
