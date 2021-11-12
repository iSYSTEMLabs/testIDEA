package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;


/**
 * This class implements dialog for entering function name and id of a new test.
 * Both fields can be left empty, although it is highly recommended to enter at 
 * least test ID.
 * <p>
 * 
 * Example:
 * <pre>
 * NewTestDialog dlg = new NewTestDialog(getShell());
 * if (dlg.show()) {
 *     createNewTest(dlg.getTestId(), dlg.getFunctionName());
 * }
 * </pre>
 * 
 * @author markok
 *
 */
public class NewTestDialog extends Dialog {

    private static final int IDX_UNIT_TEST = 0;
    private static final int IDX_SYS_TEST = 1;
    private static final int IDX_DEFAULT = 2;
    
    private String m_function;
    private GlobalsSelectionControl m_functionTxt;
    
    private String m_parameters;
    private Text m_paramsTxt;

    private String m_expect;
    private Text m_expectTxt;
    private Text m_retValTxt;
    private String m_returnValueName;
    private String m_defaultFunctionName;
    private Button m_isAutoCreateIdCb;
    private boolean m_isDerivedTest;
    private Button[] m_scopeRBtns;
    private ETestScope m_testScope;
    private Combo m_coreIdCombo;
    private String m_coreId;
    private Text m_expValueTxt;
    private Button m_simpleResultRadio;
    private Button m_advancedResultRadio;
    private String m_valForDefaultExpr;
    private Text m_prototypeTxt;
    private static boolean m_isDefaultExpr; // remember user's selection between dialog invocations
    private static boolean m_isAutoCreateId = true;  // make this field session persistent
    private static String s_coreId = "";

    /**
     * Creates dialog.
     * 
     * @param parentShell parent shell
     * @param functionName 
     * @param title text displayed in dialog title bar
     */
    public NewTestDialog(Shell parentShell, 
                         String functionName,
                         String defaultReturnValueName,
                         boolean isDerivedTest,
                         ETestScope testScope) {
        super(parentShell);
        
        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        // m_baseTest = baseTest;
        m_defaultFunctionName = functionName;
        m_returnValueName = defaultReturnValueName;
        m_isDerivedTest = isDerivedTest;
        m_testScope = testScope;
    }
    

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText(m_isDerivedTest ? "New derived test" : "New test");

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
        
        mainDlgPanel.setLayout(new MigLayout("fillx", "[min!][fill]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        builder.label("Scope:");
        Composite scopeRadioGrp = builder.composite(SWT.NONE, null);
        m_scopeRBtns = builder.radio(new String[]{"Unit", "System", "Default (Unit)"}, 
                                     scopeRadioGrp, "split 2");
        m_scopeRBtns[IDX_UNIT_TEST].addSelectionListener(new ScopeRBListener(true));
        m_scopeRBtns[IDX_SYS_TEST].addSelectionListener(new ScopeRBListener(false));
        m_scopeRBtns[IDX_DEFAULT].addSelectionListener(new ScopeRBListener(true));
        if (m_testScope == null) {
            m_scopeRBtns[IDX_DEFAULT].setSelection(true);
        } else {
            switch (m_testScope) {
            case E_UNIT_TEST:
                m_scopeRBtns[IDX_UNIT_TEST].setSelection(true);
                break;
            case E_SYSTEM_TEST:
                m_scopeRBtns[IDX_SYS_TEST].setSelection(true);
                break;
            default:
                m_scopeRBtns[IDX_DEFAULT].setSelection(true);
            }
        }
        
        m_isAutoCreateIdCb = builder.checkBox("&Auto generate test ID", "gapleft rel:push, wrap");
        UiTools.setToolTip(m_isAutoCreateIdCb, "If checked, test ID is automatically generated " +
                "according to setting of auto ID format in 'File | Properties | General'.");
        m_isAutoCreateIdCb.setSelection(m_isAutoCreateId);
        
        builder.label("Core ID:").setToolTipText("CoreIDS are configured in File | Properties | Multicore");
        final String[] coreIds = TestSpecificationModel.getActiveModel().getCoreIDs();
        m_coreIdCombo = builder.combo(coreIds, "gapleft 6, w 200:200:200, gapright rel:push, wrap", SWT.NONE);
        m_coreIdCombo.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_functionTxt.setGlobalsProvider(GlobalsContainer.GC_FUNCTIONS,
                                                 m_coreIdCombo.getText());
            }
        });
        
        m_coreIdCombo.setText(s_coreId);
        
        UiTools.setToolTip(m_coreIdCombo, "Select core Id for which to create test case.");
        if (coreIds.length == 1 &&  coreIds[0].isEmpty()) {
            m_coreIdCombo.setEnabled(false);
        }

        builder.label("&Function:");
        m_functionTxt = new GlobalsSelectionControl(mainDlgPanel, 
                                                    "wrap", 
                                                    null, 
                                                    null,
                                                    SWT.NONE, 
                                                    GlobalsContainer.GC_FUNCTIONS,
                                                    m_coreIdCombo.getText(),
                                                    true,
                                                    true,
                                                    ContentProposalAdapter.PROPOSAL_REPLACE,
                                                    UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                    GlobalsConfiguration.instance().getGlobalContainer(),
                                                    ConnectionProvider.instance());
        
        if (m_defaultFunctionName != null) {
            m_functionTxt.setText(m_defaultFunctionName);
        }
            //builder.text("width 100%, wrap", SWT.BORDER);
        m_functionTxt.setToolTipText("Name of a C function, which we want to test.");
        // allow quick move from function to parameters, most often needed
        m_functionTxt.reverseFocusOrder();

        m_prototypeTxt = builder.text("skip, gapleft 6, gapbottom 10, wrap", SWT.BORDER);
        m_prototypeTxt.setEditable(false);
        
        m_functionTxt.getControl().addVerifyListener(new VerifyListener() {
            
            @Override
            public void verifyText(VerifyEvent e) {
                e.doit = true;
                String functionName = e.text;
                
                FunctionGlobalsProvider globalFuncProvider = GlobalsConfiguration.instance().
                        getGlobalContainer().getFuncGlobalsProvider(m_coreIdCombo.getText());
                
                UiUtils.setFuncParams(m_prototypeTxt, globalFuncProvider, functionName);
            }
        });

        
        
        builder.label("&Parameters:");
        m_paramsTxt = builder.text("gapleft 6, wrap", SWT.BORDER);
        UiTools.setToolTip(m_paramsTxt, "Function parameters, for example: 10, 30, 'c'");

        
        KGUIBuilder resultGroup = builder.group("Expected result", "span 2, gaptop 15, growx, wrap", 
                                                true, "fill", "[min!][fill]", "");
        
        m_simpleResultRadio = resultGroup.radio("Default expression for function return value test", 
                                                           "span 2, wrap");
        
        resultGroup.label(CTestCase.getISystemRetValName() + "  == ", "gapleft 20");
        
        m_expValueTxt = resultGroup.text("gapleft 6, width 30%, wrap", SWT.BORDER);
        UiTools.setToolTip(m_expValueTxt, "Enter expected function return value. This value "
                + "will be used to automatically generate expression '_isys_rv == <value>'\n"
                + "in section 'Expected'. For example, if you enter:\n"
                + "    10\n"
                + "expression '_isys_rv == 10' will be automatically generated. This feature can only be used for\n"
                + "scalar types (char, int, ...). For complex types specify Ret. val. name and expression below.\n"
                + "Additional expressions can later be entered in section 'Variables'.");
        
        // builder.separator("span 2, grow, gaptop 10, gapbottom 10, wrap", SWT.HORIZONTAL);
        
        m_advancedResultRadio = resultGroup.radio("Custom expression and function return value name", 
                                                       "span2, gaptop 10, wrap");
        
        m_simpleResultRadio.setSelection(m_isDefaultExpr);
        m_advancedResultRadio.setSelection(!m_isDefaultExpr);
        
        m_simpleResultRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_isDefaultExpr = m_simpleResultRadio.getSelection();
                enableControls(true);
            }
        });
        
        resultGroup.label("&Expression:", "gapleft 20");
        m_expectTxt = resultGroup.text("gapleft 6, wrap", SWT.BORDER);
        UiTools.setToolTip(m_expectTxt, "C-expression, which is expected to return non-zero value when test passes.\n" +
        		"Example: rv < 50  ||  rv > 100\n\n"
        		+ "Enter this expression if simple equality comparison for function return value\n"
        		+ "is not enough, or you want to test other variables. Additional expressions can\n"
        		+ "later be entered in section 'Variables'.");

        resultGroup.label("&Ret. val. name:", "gapleft 20");
        m_retValTxt = resultGroup.text("gapleft 6, width 30%, wrap", SWT.BORDER);
        UiTools.setToolTip(m_retValTxt, "Name of variable to store function return value. This entry is optional.\n" +
        		                   "If empty, function return value can still be accessed with reserved name " + 
                                    CTestCase.getISystemRetValName());
        m_retValTxt.setText(m_returnValueName);

        Label separator = new Label(mainDlgPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData("spanx 2, growx, gaptop 20");

        enableControls(m_testScope == null  ||  
                       m_testScope == ETestScope.E_UNIT_TEST);
        
        // let function name field have focus, because this is most often edited field
        m_functionTxt.getControl().setFocus();

        mainDlgPanel.setTabList(new Control[]{scopeRadioGrp,
                                              m_isAutoCreateIdCb,
                                              m_coreIdCombo,m_functionTxt.getPanel(), 
                                              m_paramsTxt, 
                                              resultGroup.getParent()});
        
        return composite;
    }
    

    // saves the data when it is still available
    private void saveTextData() {
        
        m_coreId = m_coreIdCombo.getText();
        m_function = m_functionTxt.getControl().getText().trim();
        m_parameters = m_paramsTxt.getText().trim();
        m_valForDefaultExpr = m_expValueTxt.getText().trim();
        m_expect = m_expectTxt.getText().trim();
        m_returnValueName = m_retValTxt.getText().trim();
        m_isAutoCreateId = m_isAutoCreateIdCb.getSelection();
        
        m_testScope = null;
        
        if (m_scopeRBtns[IDX_SYS_TEST].getSelection()) {
            m_testScope = ETestScope.E_SYSTEM_TEST;
        } else if (m_scopeRBtns[IDX_UNIT_TEST].getSelection()) {
            m_testScope = ETestScope.E_UNIT_TEST;
        }
    }
    
    
    @Override
    protected void okPressed() {
        saveTextData();
        
        /* try {
            if (!m_testId.isEmpty()) { 
                StringValidator.isAlphanum(m_testId, true);
            }
        } catch (IllegalArgumentException ex) {
            MessageDialog.open(MessageDialog.ERROR, getParentShell(), 
                               "Invalid test ID!", ex.getMessage(), SWT.NONE);
            return;
        } */
            
        // Function name validation is no longer done, because it may also contain
        // types. If required, split it on ' - ' and verify the first part.
        // m_function = m_functionTxt.parseNameWithType(m_function, null)[0]; 
/*        try {
            if (!m_function.isEmpty()) {
                StringValidator.isAlphanum(m_function, true);
            }
        } catch (IllegalArgumentException ex) {
            MessageDialog.open(MessageDialog.ERROR, getParentShell(), 
                               "Invalid function name!", ex.getMessage(), SWT.NONE);
            return;
        } */
            
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }

    
    public ETestScope getTestScope() {
        return m_testScope;
    }


    public String getCoreId() {
        return m_coreId;
    }
    

    public boolean isAutoCreateId() {
        return m_isAutoCreateId;
    }

    
    public String getFunction() {
        return m_function;
    }
    
    
    public String getParameters() {
        return m_parameters;
    }


    public boolean isDefaultExpression() {
        return m_isDefaultExpr;
    }
    
    
    public String getValueForDefaultExpression() {
        return m_valForDefaultExpr;
    }
    
    
    public String getCustomExpression() {
        return m_expect;
    }

    
    public String getReturnValueName() {
        return m_returnValueName;
    }

    
    private void enableControls(boolean isEnableUnitTestControls) {
        m_functionTxt.getControl().setEnabled(isEnableUnitTestControls);
        m_paramsTxt.setEnabled(isEnableUnitTestControls);

        m_simpleResultRadio.setEnabled(isEnableUnitTestControls);
        m_advancedResultRadio.setEnabled(isEnableUnitTestControls);
        m_expValueTxt.setEnabled(isEnableUnitTestControls  &&  m_isDefaultExpr);
        m_expectTxt.setEnabled(!m_isDefaultExpr);
        m_retValTxt.setEnabled(isEnableUnitTestControls  &&  !m_isDefaultExpr);
    }

    
    class ScopeRBListener implements SelectionListener {

        private boolean m_isEnableUnitTestControls;

        ScopeRBListener(boolean isEnableUnitTestControls) {
            m_isEnableUnitTestControls = isEnableUnitTestControls;
        }
        
        @Override
        public void widgetSelected(SelectionEvent e) {
            if (!m_isEnableUnitTestControls) {
                // for system tests select the advanced radio button, since 
                // expressions are valid entry for system tests.
                m_simpleResultRadio.setSelection(false);
                m_advancedResultRadio.setSelection(true); // simple results have
                // no meaning in system tests - there is no ret val defined
                m_simpleResultRadio.notifyListeners(SWT.Selection, new Event());
            }
            enableControls(m_isEnableUnitTestControls);
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {}
        
    }
}

