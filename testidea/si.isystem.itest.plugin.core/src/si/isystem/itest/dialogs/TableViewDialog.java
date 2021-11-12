package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestResult;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.TestBaseListTable;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This class opens dialog with non-editable TestBaseListTable.
 * 
 * @author markok
 *
 */
public class TableViewDialog extends Dialog {

    
    private String m_titleText;
    private CTestResult m_testResult;
    private int m_sectionId;
    
    public TableViewDialog(Shell parentShell, String title, CTestResult testResult,
                           int section) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_titleText = title;
        m_testResult = testResult;
        m_sectionId = section;
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
        gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 800;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow][min!][min!]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        TestBaseListTable stepsTable = new TestBaseListTable(null, false);
        
        Control stepsTableControl = stepsTable.createControl(builder.getParent(), 
                                                             m_testResult,
                                                             m_sectionId,
                                                             ENodeId.TEST_POINT_NODE,
                                                             null);
        stepsTable.getKModel().adjustRowHeights();
        stepsTable.setEditable(false);
        stepsTableControl.setLayoutData("wmin 0, wrap, grow");
        stepsTable.setTooltip("This table shows stub or test point results for all hits.");
        
        // Label separator = new Label(mainDlgPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        // separator.setLayoutData("spanx 2, growx, gaptop 20");

        return composite;
    }
    
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
    }
}
