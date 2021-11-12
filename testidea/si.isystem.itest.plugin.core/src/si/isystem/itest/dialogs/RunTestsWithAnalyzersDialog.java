package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * If user has no license for testIDEA and selected tests contain analyzer 
 * sections, he is ashed to run tests but ignore analyzer.
 *  
 * @author markok
 *
 */
public class RunTestsWithAnalyzersDialog extends Dialog {

    public static final int RUN_ALL_BTN = 0;
    public static final int SKIP_TESTS_W_ANALYZER_BTN = 1;
    public static final int CANCEL_BTN = 2;
    private String m_message;


    protected RunTestsWithAnalyzersDialog(Shell parentShell, String message) {
        super(parentShell);
        
        m_message = message;
    }

    
    public static int open(String message) {
        RunTestsWithAnalyzersDialog dlg = new RunTestsWithAnalyzersDialog(Activator.getShell(), 
                                                                          message);
        return dlg.open();
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Run tests with active analyzer section");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        
        mainDlgPanel.setLayout(new MigLayout("fillx", ""));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        builder.label(m_message, "wrap");
        
        builder.separator("growx, gaptop 20", SWT.HORIZONTAL);

        return composite;
    }
    
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, RUN_ALL_BTN, "Run all tests",
                true);
        
        createButton(parent, SKIP_TESTS_W_ANALYZER_BTN, "Skip tests with analyzer",
                     false);
        
        createButton(parent, CANCEL_BTN,
                             IDialogConstants.CANCEL_LABEL, false);
                             
    }
    

    @Override
    protected void buttonPressed(int buttonId) {
        
        setReturnCode(buttonId);
        close();
    }
}
