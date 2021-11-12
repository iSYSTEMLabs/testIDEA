package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.ETristate;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;


/**
 * This class implements dialog for helping user to get winIDEA and target into
 * right state before running the test.
 * <p>
 * 
 * Example:
 * <pre>
 * TargetStateDialog dlg = new TargetStateDialog(getShell());
 * if (dlg.show()) {
 *     // store settings;
 * }
 * </pre>
 * 
 * @author markok
 *
 */
public class TargetStateDialog extends Dialog {

    private Button m_runInitSequenceRb;
    private Button m_stopTargetRb;
    private Button m_runTestAnywayRb;
    private boolean m_isRunInitSequence;
    private boolean m_isStopTarget;
    private String m_statusText;
    private boolean m_isShowStopOption;
    private Button m_doNotShowThisDlgCb;

    /**
     * Creates dialog.
     * 
     * @param parentShell parent shell
     * @param title text displayed in dialog title bar
     */
    public TargetStateDialog(Shell parentShell, String statusText, boolean isShowStopOption) {
        super(parentShell);
        
        m_statusText = statusText;
        m_isShowStopOption = isShowStopOption;
        
        // make dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Init target state before test run");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout());
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        builder.label(m_statusText, "wrap");
        KGUIBuilder radioGroup = builder.group("Available actions", "wmin 0, wrap");
        m_runInitSequenceRb = radioGroup.radio("Run init sequence (see menu 'Run | Configuration')", "wrap");
        UiTools.setToolTip(m_runInitSequenceRb, "If selected, init sequence is executed before running test.");

        if (m_isShowStopOption) {
            m_stopTargetRb = radioGroup.radio("Stop target", "wrap");
            UiTools.setToolTip(m_stopTargetRb, "If selected, target / core is stopped before running test.\n" +
            "Not recommended, but may work if we know what we are doing.");
        }
        
        m_runTestAnywayRb = radioGroup.radio("Do nothing, just run the test (not recommended)", "wrap");
        UiTools.setToolTip(m_runTestAnywayRb, "If selected, nothing is done before running test.\n" +
        		"Test execution is likely to fail.");

        m_doNotShowThisDlgCb = builder.checkBox("Do not show this dialog in the future (open 'Test | Configuration' to change this setting).",
                                     "gaptop 10, wrap");
        UiTools.setToolTip(m_doNotShowThisDlgCb, "Select 'Run | Configuration ... | Check target state before run' to re-enable this dialog.");
        CTestEnvironmentConfig runCfg = TestSpecificationModel.getActiveModel().getTestEnvConfig();
        m_doNotShowThisDlgCb.setSelection(!runCfg.isCheckTargetStateBeforeRun());
        
        Label separator = new Label(mainDlgPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData("spanx 2, growx, gaptop 20");

        fillControls();
        
        return composite;
    }
    
    
    private void fillControls() {
        m_runInitSequenceRb.setSelection(true);  // default is to run init seq.
    }
    
    
    @Override
    protected void okPressed() {
        m_isRunInitSequence = m_runInitSequenceRb.getSelection();
        if (m_stopTargetRb != null) {
            m_isStopTarget = m_stopTargetRb.getSelection();
        }
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestEnvironmentConfig runCfg = model.getTestEnvConfig();
        boolean isCheckTargetState = !m_doNotShowThisDlgCb.getSelection();
        if (isCheckTargetState != runCfg.isCheckTargetStateBeforeRun()) {
            runCfg.setCheckTargetStateBeforeRun(isCheckTargetState ? ETristate.E_TRUE : 
                                                                     ETristate.E_FALSE);
            model.setModelDirty(true);
        }
            
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }

    
    public boolean isRunInitSequence() {
        return m_isRunInitSequence;
    }
    
    public boolean isStopTarget() {
        return m_isStopTarget;
    }
}

