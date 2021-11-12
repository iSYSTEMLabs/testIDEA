package si.isystem.itest.ui.spec;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.CTestUserStub.EUserStubSections;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlCombo;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;


public class UserStubEditor extends ListEditorBase {

    private GlobalsSelectionControl m_replacementFuncNameGHC;
    private TBControlCombo m_replacementFuncNameHC;
    private Text m_replFuncParamsText;
    private FunctionGlobalsProvider m_globalFunctionsProvider;
    private Button m_callBtn;
    private Button m_skipBtn;

    
    public UserStubEditor() {
        super(ENodeId.STUBS_USER_NODE, SectionIds.E_SECTION_USER_STUBS);
        
        CTestBaseIdAdapter adapter = 
                new CTestBaseIdAdapter(EUserStubSections.E_SECTION_STUBBED_FUNC_NAME.swigValue(),
                                       ENodeId.STUBS_USER_NODE) {
            
            @Override
            public String getId(CTestBase testBase) {
                return CTestUserStub.cast(testBase).getFunctionName();
            }
            
            
            @Override
            public CTestBase createNew(CTestBase parentTestBase) {
                return new CTestUserStub(parentTestBase);
            }

            
            @Override
            public CTestBaseList getItems(boolean isConst) {
                if (m_currentTestSpec == null) {
                    return EMPTY_CTB_LIST;
                }
                return m_currentTestSpec.getUserStubs(isConst);
            }


            @Override
            public Boolean isError(int dataRow) {
                return null; // user stubs have no results
            }
        };
        
        setIdAdapter(adapter);
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {

        m_uiStrings.m_isActiveBtn_false = "Function will NOT be stubbed during test run.";
        m_uiStrings.m_isActiveBtn_true = "Function will be stubbed during test run.";
        m_uiStrings.m_isActiveBtn_default = "Test specification does not contain this tag, so default value is used - stub is active.";
        m_uiStrings.m_tableTitle = "User Stubs";
        m_uiStrings.m_tableTooltip = "Name of function to be stubbed. Select stub function to view\n" +
                                "settings on the right. Use buttons to add/delete stubs.";
        
        Composite mainPanel = createScrollable(parent);
        mainPanel.setLayout(new MigLayout("fill"));
        
        Composite stubsPanel = 
                super.createPartControl(mainPanel, 
                                        CTestUserStub.EUserStubSections.E_SECTION_IS_ACTIVE.swigValue(),
                                        -1,
                                        "[min!][min!][min!][min!][min!][min!][fill]");
        stubsPanel.setLayoutData("growx, growy");

        KGUIBuilder builder = new KGUIBuilder(stubsPanel);

        // these two buttons have no tag in YAML spec - they only control enablement
        // of replacement func. control
        builder.label("Stub type:");
        KGUIBuilder stubTypeGrp = new KGUIBuilder(new Composite(stubsPanel, SWT.NONE));
        stubTypeGrp.getParent().setLayout(new MigLayout("fillx"));
        stubTypeGrp.getParent().setLayoutData("wrap");

        m_callBtn = stubTypeGrp.radio("Call target function", "");
        m_skipBtn = stubTypeGrp.radio("Skip stub", "");
        UiTools.setToolTip(m_callBtn, "Call to stubbed function is replaced with call to another function on the target.");
        UiTools.setToolTip(m_skipBtn, "Call to stubbed function is skipped - the stubbed function immediately returns.\n" +
        		                    "This approach may not be suitable for stubbed functions which return values, have\n" +
        		                    "output parameters, or make other side effect. Use with care!");
        
        builder.label("Replacement f.:");
        
        m_replacementFuncNameGHC = new GlobalsSelectionControl(builder.getParent(), 
                                            "split, span, growx, wrap",
                                            null,
                                            null,
                                            SWT.NONE,
                                            GlobalsContainer.GC_ALL_FUNCTIONS,
                                            null,
                                            true,
                                            true,
                                            ContentProposalAdapter.PROPOSAL_REPLACE,
                                            UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                            GlobalsConfiguration.instance().getGlobalContainer(),
                                            ConnectionProvider.instance());

        m_replacementFuncNameGHC.getControl().addVerifyListener(new VerifyListener() {
            
            @Override
            public void verifyText(VerifyEvent e) {
                e.doit = true;
                String functionName = e.text;
                UiUtils.setFuncParams(m_replFuncParamsText, m_globalFunctionsProvider, 
                                      functionName);
            }
        });
        
        m_replacementFuncNameHC = new TBControlCombo(m_replacementFuncNameGHC.getControl(), 
                                                     "Name of a target function, which will be called " +
                                                             "instead of stubbed function. If empty, skip stub is used.", 
                                                             EUserStubSections.E_SECTION_REPLACEMENT_FUNC_NAME.swigValue(), 
                                                     m_nodeId, 
                                                     EHControlId.EStubReplFuncName);
        
        m_replFuncParamsText = builder.text("skip, span 3, h min:min:min, growx, gapbottom 10, wrap", SWT.BORDER);
        m_replFuncParamsText.setEditable(false);

        m_callBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_replacementFuncNameHC.setEnabled(true);
            }
            
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        m_skipBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_replacementFuncNameHC.setTextInControl("");
                m_replacementFuncNameHC.setEnabled(false);
                m_replacementFuncNameHC.sendSetSectionAction();
            }
            
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        return getScrollableParent(mainPanel);
    }
    
    
    @Override
    protected void createItemIdControls(KGUIBuilder builder) {
        createFuncNameControls(builder);
    }

    
    @Override
    protected void fillSectionControls() {
        super.fillSectionControls();
        m_listCommentEditor.updateValueAndCommentFromTestBase(m_currentTestSpec);
    }
    

    @Override
    protected void fillListItemControls(CTestBase testBase) {
        super.fillListItemControls(testBase);

        m_commentChangedListener.setTestBase(m_currentTestSpec);
        
        CTestUserStub stub = CTestUserStub.cast(testBase);
        
        m_globalFunctionsProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getFuncGlobalsProvider(m_currentCoreId);
        
        m_replacementFuncNameGHC.setGlobalsProvider(GlobalsContainer.GC_FUNCTIONS,
                                                    m_currentCoreId);

        m_replacementFuncNameHC.setInput(stub, m_isInherited);
        
        String replFuncName = stub.getReplacementFuncName();
        
        if (replFuncName.isEmpty()) {
            m_callBtn.setSelection(false);
            m_skipBtn.setSelection(true);
            m_replacementFuncNameHC.setEnabled(false);
        } else {
            m_callBtn.setSelection(true);
            m_skipBtn.setSelection(false);
            if (!m_isInherited) {
                m_replacementFuncNameHC.setEnabled(true);
            }
        }
    }
    
    
    @Override
    protected void clearListItemControls() {
        super.clearListItemControls();
        
        m_replacementFuncNameHC.clearInput();
        m_replFuncParamsText.setText("");
    }
    
    
    @Override
    protected void enableListItemControls(boolean isEnabled) {
        super.enableListItemControls(isEnabled);

        m_replacementFuncNameHC.setEnabled(isEnabled);
        m_callBtn.setEnabled(isEnabled);
        m_skipBtn.setEnabled(isEnabled);
    }
    
    
    @Override
    public boolean isActive() {
        
        if (m_testSpec == null) {
            return false;
        }

        CTestBaseList list = m_testSpec.getUserStubs(true);
        int numItems = (int)list.size();
        for (int i = 0; i < numItems; i++) {
            CTestUserStub stub = CTestUserStub.cast(list.get(i));
            if (stub.isActive() != ETristate.E_FALSE) {
                return true; // if one stub is active, mark section as active
            }
        }
        
        return false;
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_USER_STUBS.swigValue()};
    }
}
