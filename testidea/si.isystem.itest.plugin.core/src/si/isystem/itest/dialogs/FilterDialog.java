package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestSpecification;
import si.isystem.itest.common.FilterConfigPage;
import si.isystem.itest.common.FilterConfigPage.ContainerType;

public class FilterDialog extends Dialog {

    private FilterConfigPage m_filterConfigPage;
    private CTestFilter m_filter;
    private CTestSpecification m_containerTestSpec;

    
    /**
     * 
     * @param containerTestSpec test case, which contains all test cases to
     *                          be filtered. To filter all test cases from model,
     *                          pass root test case.
     */
    public FilterDialog(Shell parentShell, 
                        CTestSpecification containerTestSpec, 
                        CTestFilter filter) {
        
        super(parentShell);
        
        m_containerTestSpec = containerTestSpec;
        m_filter = filter;
        
        // make dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    public boolean isResizable() {
        return true;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        
        Composite mainPanel = (Composite) super.createDialogArea(parent);

        mainPanel.getShell().setText("Test case filter");
        
        Composite mainDlgPanel = new Composite(mainPanel, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 610;  // sets initial dialog size
        gridData.widthHint = 700;
        mainDlgPanel.setLayoutData(gridData);

        mainDlgPanel.setLayout(new MigLayout("fill"));

        // int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        m_filterConfigPage = new FilterConfigPage(ContainerType.E_REVERSE_LINEAR,
                                                  false);
        m_filterConfigPage.createMainPanel(mainDlgPanel);
        m_filterConfigPage.setInput(m_containerTestSpec, m_filter, null);
        m_filterConfigPage.refreshGlobals();
        m_filterConfigPage.fillControls();

        return mainPanel;
    }
    
    
    @Override
    protected void okPressed() {
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }
}

