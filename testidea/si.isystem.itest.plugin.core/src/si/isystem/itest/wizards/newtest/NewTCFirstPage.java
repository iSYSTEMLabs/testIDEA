package si.isystem.itest.wizards.newtest;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.data.JFunction;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class NewTCFirstPage extends GlobalsWizardDataPage {
    
    private static final String PAGE_TITLE = "New test case wizard";

    private static final int IDX_UNIT_TEST = 0;
    private static final int IDX_SYS_TEST = 1;
    private static final int IDX_DEFAULT = 2;

    private Button[] m_scopeRBtns;
    private Button m_isAutoCreateIdCb;
    private Combo m_coreIdCombo;

    private GlobalsSelectionControl m_functionGSC;
    private Text m_prototypeTxt;
    private Text m_paramsTxt;

    private Button m_simpleResultRadio;
    private Button m_advancedResultRadio;
    private Text m_defaultExpValueTxt;
    private Text m_expectExprTxt;
    private Text m_retValTxt;


    public NewTCFirstPage(NewTCWizardDataModel model) {
        super(PAGE_TITLE);
        setTitle(PAGE_TITLE);
        setDescription("Enter basic test case information. Button 'Next' is enabled only for unit tests if function name is defined and symbols are loaded.");
        
        m_ntcModel = model;
    }
   

    @Override
    public void createControl(Composite parent) {
        setControl(createPage(parent));
    }
    
    
    @Override
    public Composite createPage(Composite parent) {

        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new MigLayout("fillx", "[min!][fill]"));

        KGUIBuilder builder = new KGUIBuilder(container);
        
        Composite scopeRadioGrp = createScopeRadios(builder);
        
        m_isAutoCreateIdCb = builder.checkBox("&Auto generate test ID", "gapleft rel:push, wrap");
        UiTools.setToolTip(m_isAutoCreateIdCb, "If checked, test ID is automatically generated " +
                "according to setting of auto ID format in 'File | Properties | General'.");
        m_isAutoCreateIdCb.setSelection(NewTCWizardDataModel.m_isAutoCreateId);
        
        builder.label("Core ID:").setToolTipText("CoreIDS are configured in File | Properties | Multicore");
        final String[] coreIds = TestSpecificationModel.getActiveModel().getCoreIDs();
        m_coreIdCombo = builder.combo(coreIds, "gapleft 6, w 200:200:200, gapright rel:push, wrap", SWT.NONE);
        m_coreIdCombo.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_functionGSC.setGlobalsProvider(GlobalsContainer.GC_FUNCTIONS,
                                                 m_coreIdCombo.getText());
            }
        });
        
        m_coreIdCombo.setText(m_ntcModel.m_coreId);
        
        UiTools.setToolTip(m_coreIdCombo, "Select core Id for which to create test case.");
        if (coreIds.length == 1 &&  coreIds[0].isEmpty()) {
            m_coreIdCombo.setEnabled(false);
        }

        builder.label("&Function:");
        m_functionGSC = new GlobalsSelectionControl(container, 
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
        
        m_functionGSC.setToolTipText("Name of a C function, which we want to test.");
        
        // allow quick move from function to parameters, most often needed
        m_functionGSC.reverseFocusOrder();

        m_prototypeTxt = builder.text("skip, gapleft 6, gapbottom 10, wrap", SWT.BORDER);
        m_prototypeTxt.setEditable(false);
        
        addFuncChangeListeners();
        
        builder.label("&Parameters:");
        m_paramsTxt = builder.text("gapleft 6, wrap", SWT.BORDER);
        UiTools.setToolTip(m_paramsTxt, "Function parameters, for example: 10, 30, 'c'");

        
        KGUIBuilder resultGroup = builder.group("Expected result", "span 2, gaptop 15, growx, wrap", 
                                                true, "fill", "[min!][fill]", "");
        
        m_simpleResultRadio = resultGroup.radio("Default expression for function return value test", 
                                                "span 2, wrap");
        
        resultGroup.label(CTestCase.getISystemRetValName() + "  == ", "gapleft 20");
        
        m_defaultExpValueTxt = resultGroup.text("gapleft 6, width 30%, wrap", SWT.BORDER);
        UiTools.setToolTip(m_defaultExpValueTxt, "Enter expected function return value. This value "
                + "will be used to automatically generate expression '_isys_rv == <value>'\n"
                + "in section 'Expected'. For example, if you enter:\n"
                + "    10\n"
                + "expression '_isys_rv == 10' will be automatically generated. This feature can only be used for\n"
                + "scalar types (char, int, ...). For complex types specify Ret. val. name and expression below.\n"
                + "Additional expressions can later be entered in section 'Variables'.");
        
        // builder.separator("span 2, grow, gaptop 10, gapbottom 10, wrap", SWT.HORIZONTAL);
        
        m_advancedResultRadio = resultGroup.radio("Custom expression and function return value name", 
                                                       "span2, gaptop 10, wrap");
        
        m_simpleResultRadio.setSelection(NewTCWizardDataModel.m_isDefaultExpr);
        m_advancedResultRadio.setSelection(!NewTCWizardDataModel.m_isDefaultExpr);
        
        m_simpleResultRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NewTCWizardDataModel.m_isDefaultExpr = m_simpleResultRadio.getSelection();
                enableControls(true);
            }
        });
        
        resultGroup.label("&Expression:", "gapleft 20");
        m_expectExprTxt = resultGroup.text("gapleft 6, wrap", SWT.BORDER);
        UiTools.setToolTip(m_expectExprTxt, "C-expression, which is expected to return non-zero value when test passes.\n" +
                "Example: rv < 50  ||  rv > 100\n\n"
                + "Enter this expression if simple equality comparison for function return value\n"
                + "is not enough, or you want to test other variables. Additional expressions can\n"
                + "later be entered in section 'Variables'.");

        resultGroup.label("&Ret. val. name:", "gapleft 20");
        m_retValTxt = resultGroup.text("gapleft 6, width 30%, wrap", SWT.BORDER);
        UiTools.setToolTip(m_retValTxt, "Name of variable to store function return value. This entry is optional.\n" +
                                   "If empty, function return value can still be accessed with reserved name " + 
                                    CTestCase.getISystemRetValName());
        m_retValTxt.setText(m_ntcModel.m_retValVarName);        

        // let function name field have focus, because this is most often edited field
        m_functionGSC.getControl().setFocus();

        container.setTabList(new Control[]{scopeRadioGrp,
                                           m_isAutoCreateIdCb,
                                           m_coreIdCombo,
                                           m_functionGSC.getPanel(), 
                                           m_paramsTxt, 
                                           resultGroup.getParent()});
        
        fillControls();
        
        return container;
    }


    private void addFuncChangeListeners() {
        // refresh status of Next button after every key-press
        // Use ModifyListener instead of KeyListener, because KeyListener is
        // not called when user double clicks on proposal.
        m_functionGSC.getControl().addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                // update 'Next' button state and func. declaration
                String functionName = m_functionGSC.getControl().getText().trim();
                verifyFuncName(functionName);
            }
        });
        
        // refresh status of Next button after symbols refresh
        m_functionGSC.getRefreshButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // update 'Next' button state and func. declaration
                String functionName = m_functionGSC.getControl().getText().trim();
                verifyFuncName(functionName);
            }
        });
    }
    
    
    private Composite createScopeRadios(KGUIBuilder builder) {
        
        builder.label("Scope:");
        Composite scopeRadioGrp = builder.composite(SWT.NONE, null);

        m_scopeRBtns = builder.radio(new String[]{"Unit", "System", "Default (Unit)"}, 
                                     scopeRadioGrp, "split 2");
        m_scopeRBtns[IDX_UNIT_TEST].addSelectionListener(new ScopeRBListener(true));
        m_scopeRBtns[IDX_SYS_TEST].addSelectionListener(new ScopeRBListener(false));
        m_scopeRBtns[IDX_DEFAULT].addSelectionListener(new ScopeRBListener(true));
        if (m_ntcModel.m_testScope == null) {
            m_scopeRBtns[IDX_DEFAULT].setSelection(true);
        } else {
            switch (m_ntcModel.m_testScope) {
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
        
        return scopeRadioGrp;
    }
    

    @Override
    public boolean canFlipToNextPage() {
        return super.canFlipToNextPage()  &&  !m_scopeRBtns[IDX_SYS_TEST].getSelection();
    }

    
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }
    
    
    @Override
    public void dataToModel() {
        NewTCWizardDataModel.m_isAutoCreateId = m_isAutoCreateIdCb.getSelection();  
        m_ntcModel.m_coreId = m_coreIdCombo.getText().trim();
        
        if (m_scopeRBtns[IDX_UNIT_TEST].getSelection()) {
            m_ntcModel.m_testScope = ETestScope.E_UNIT_TEST;
        } else if (m_scopeRBtns[IDX_SYS_TEST].getSelection()) {
            m_ntcModel.m_testScope = ETestScope.E_SYSTEM_TEST;
        } else {
            m_ntcModel.m_testScope = null;
        }
        
        m_ntcModel.m_funcUnderTestName = m_functionGSC.getControl().getText().trim();
        m_ntcModel.m_parameters = m_paramsTxt.getText();
        m_ntcModel.m_retValVarName = m_retValTxt.getText().trim();
        NewTCWizardDataModel.m_isDefaultExpr = m_simpleResultRadio.getSelection();
        m_ntcModel.m_valForDefaultExpr = m_defaultExpValueTxt.getText().trim();
        m_ntcModel.m_expectExpr = m_expectExprTxt.getText().trim();
    }
    

    @Override
    public void dataFromModel() {
        // nothing to do - there is no data for new test.
    }
    
    
    // sets func header test and state of buttons - it is allowed to go to next
    // page only if there is symbol info for function available.
    private void verifyFuncName(String functionName) {
        
        if (functionName.isEmpty()) {
            setPageComplete(false);
            return;
        }
        
        String coreId = m_coreIdCombo.getText();
        FunctionGlobalsProvider globalFuncProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getFuncGlobalsProvider(coreId);
        
        if (globalFuncProvider == null) {
            if (coreId.isEmpty()) {
                m_prototypeTxt.setText("WARNING: No symbols found! Click button 'Refresh globals'!");
            } else {
                m_prototypeTxt.setText("WARNING: No symbols found! Click button 'Refresh globals', or change core ID!");
            }
            setPageComplete(false);
            
        } else {
            try {
                JFunction jFunc = globalFuncProvider.getCachedFunction(functionName);
                if (jFunc != null) {
                    UiUtils.setFuncParams(m_prototypeTxt, globalFuncProvider, functionName);
                    setPageComplete(true);
                } else {
                    if (globalFuncProvider.getCachedGlobals().length == 0) {
                        m_prototypeTxt.setText("WARNING: No symbols found! Click button 'Refresh globals'!");
                    } else {
                        m_prototypeTxt.setText("WARNING: Function not found in symbols!");
                    }
                    setPageComplete(false);
                }
            } catch (SException ex) {
                m_prototypeTxt.setText("ERROR: " + SEFormatter.getInfo(ex));
            }
        }
    }
    
    
    private void fillControls() {
        m_functionGSC.setText(m_ntcModel.m_funcUnderTestName);

        enableControls(m_ntcModel.m_testScope != ETestScope.E_SYSTEM_TEST);
        setPageComplete(!m_ntcModel.m_funcUnderTestName.isEmpty());
    }
    
    
    private void enableControls(boolean isEnableUnitTestControls) {
        m_functionGSC.getControl().setEnabled(isEnableUnitTestControls);
        m_paramsTxt.setEnabled(isEnableUnitTestControls);

        m_simpleResultRadio.setEnabled(isEnableUnitTestControls);
        m_advancedResultRadio.setEnabled(isEnableUnitTestControls);
        m_defaultExpValueTxt.setEnabled(isEnableUnitTestControls  &&  
                                        NewTCWizardDataModel.m_isDefaultExpr);
        m_expectExprTxt.setEnabled(!NewTCWizardDataModel.m_isDefaultExpr);
        m_retValTxt.setEnabled(isEnableUnitTestControls  &&  
                               !NewTCWizardDataModel.m_isDefaultExpr);
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
            setErrorMessage(null); // this call refreshes 'Next' button, which 
                                   // should be disabled for system tests
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {}
        
    }
}


