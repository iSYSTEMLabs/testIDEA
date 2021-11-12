package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestFunction.ESection;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrStrMapIterator;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.handlers.ToolsRefreshGlobalsCmdHandler;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.AssignTestObjectAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlCombo;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.AsystTextContentAdapter;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class FunctionSpecEditor extends SectionEditorAdapter {

    private GlobalsSelectionControl m_funcText;
    private ValueAndCommentEditor m_funcTagCommentEditor; // comment for tag 'func'
    
    private Text m_funcParamsText;
    private TBControlCombo m_funcNameControl;
    
    private TBControlText m_paramsControl;
    private TBControlText m_retValControl;

    // private ContentProposalProvider m_paramsAutoComplete;
    private RefreshBtnListener m_refreshListener;
    private Shell m_shell;
    private TBControlTristateCheckBox m_isInheritFuncTB;
    private TBControlTristateCheckBox m_isInheritParamsTB;
    private VariablesContentProposal m_varsProposalProvider;
    private TBControlText m_coreIdTBCtrl;
    private TBControlTristateCheckBox m_isInheritCoreIdTB;
    private TBControlText m_timeoutTBCtrl;
    private TBControlTristateCheckBox m_isInheritTimeoutTB;
    private AsystContentProposalProvider m_coreIdProposals;


    public FunctionSpecEditor() {
        super(ENodeId.FUNCTION_NODE, SectionIds.E_SECTION_FUNC);
        
        m_refreshListener = new RefreshBtnListener();
    }


    @Override
    public Composite createPartControl(Composite parent) {
        m_shell = parent.getShell();
        
        Composite functionPanel = createScrollable(parent);

        functionPanel.setLayout(new MigLayout("fillx", "[min!][grow][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(functionPanel);
        
        m_isInheritFuncTB = createTristateInheritanceButton(builder, "skip, w min:pref:pref, gaptop 15, wrap");

        // since function and params are part of the same section, we have to clear each
        // one separately - cannot clear the section as a whole
        m_isInheritFuncTB.setActionProvider(new BoolActionProvider() {

            @Override
            public AbstractAction getClearAction() {
                GroupAction grp = new GroupAction("Clear func name and ret val");
                YamlScalar value = YamlScalar.newMixed(ESection.E_SECTION_FUNC_NAME.swigValue());
                value.setValue("");
                SetSectionAction action = new SetSectionAction(m_testSpec.getFunctionUnderTest(false), 
                                                               m_nodeId, 
                                                               value);
                grp.add(action);
                
                value = YamlScalar.newMixed(ESection.E_SECTION_RET_VAL_NAME.swigValue());
                value.setValue("");
                action = new SetSectionAction(m_testSpec.getFunctionUnderTest(false), 
                                              m_nodeId, 
                                              value);
                grp.add(action);
                grp.addAllFireEventTypes();
                grp.addDataChangedEvent(m_nodeId, m_testSpec);
                return grp;
            }

            @Override
            public AbstractAction getCopyAction() {
                GroupAction grp = new GroupAction("Copy func name and ret val");

                AbstractAction action = 
                    new AssignTestObjectAction(m_testSpec.getFunctionUnderTest(false).getSectionValue(ESection.E_SECTION_FUNC_NAME.swigValue(), false), 
                                               m_mergedTestSpec.getFunctionUnderTest(true).getSectionValue(ESection.E_SECTION_FUNC_NAME.swigValue(), false),
                                               m_nodeId);
                grp.add(action);
                
                action = 
                    new AssignTestObjectAction(m_testSpec.getFunctionUnderTest(false).getSectionValue(ESection.E_SECTION_RET_VAL_NAME.swigValue(), false), 
                                               m_mergedTestSpec.getFunctionUnderTest(true).getSectionValue(ESection.E_SECTION_RET_VAL_NAME.swigValue(), false),
                                               m_nodeId);
                grp.add(action);
                
                grp.addAllFireEventTypes();
                grp.addDataChangedEvent(m_nodeId, m_testSpec);
                return grp;
            }
        });
        
        Font font = Activator.getDefault().getFont(parent.getShell(), Activator.EFont.FontBold);
        builder.label("Function:").setFont(font);

        m_funcText = new GlobalsSelectionControl(builder.getParent(), 
                                                 "pushx, growx, span 2, wrap",
                                                 null,
                                                 null,
                                                 SWT.NONE,
                                                 GlobalsContainer.GC_FUNCTIONS,
                                                 "",
                                                 true,
                                                 true,
                                                 ContentProposalAdapter.PROPOSAL_REPLACE,
                                                 UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                 GlobalsConfiguration.instance().getGlobalContainer(),
                                                 ConnectionProvider.instance());

                                                       
        m_funcText.getControl().addVerifyListener(new VerifyListener() {
            
            @Override
            public void verifyText(VerifyEvent e) {
                e.doit = true;
                String functionName = e.text; //m_funcText.getControl().getText();
                
                FunctionGlobalsProvider globalFuncProvider = GlobalsConfiguration.instance().
                        getGlobalContainer().getFuncGlobalsProvider(m_currentCoreId);
                UiUtils.setFuncParams(m_funcParamsText, globalFuncProvider, functionName);
            }
        });
        
        m_funcTagCommentEditor = ValueAndCommentEditor.newKey(SectionIds.E_SECTION_FUNC.swigValue(), 
                                                              m_funcText.getControl(),
                                                              SWT.LEFT);
        
        m_funcNameControl = new TBControlCombo(m_funcText.getControl(),
                                               "Name of a C function which we want to test.\n"
                                               + "Example:\n    func1",
                                               ESection.E_SECTION_FUNC_NAME.swigValue(),
                                               m_nodeId,
                                               EHControlId.EFuncUnderTestName);
        
        m_funcNameControl.setTestTreeRefreshNeeded(true);
        
        m_funcParamsText = builder.text("skip, span 2, pushx, growx, gapbottom 10, wrap", SWT.BORDER);
        m_funcParamsText.setEditable(false);
        
        m_isInheritParamsTB = createTristateInheritanceButton(builder, "skip, gaptop 15, w min:pref:pref, wrap");
        m_isInheritParamsTB.setActionProvider(new BoolActionProvider() {

            @Override
            public AbstractAction getClearAction() {
                YamlScalar value = YamlScalar.newList(ESection.E_SECTION_PARAMS.swigValue());
                value.setValue("");
                SetSectionAction action = new SetSectionAction(m_testSpec.getFunctionUnderTest(false), 
                                                               m_nodeId, 
                                                               value);
                action.addAllFireEventTypes();
                action.addDataChangedEvent(m_nodeId, m_testSpec);
                return action;
            }

            @Override
            public AbstractAction getCopyAction() {
                AbstractAction action = 
                    new AssignTestObjectAction(m_testSpec.getFunctionUnderTest(false).getSectionValue(ESection.E_SECTION_PARAMS.swigValue(), false), 
                                               m_mergedTestSpec.getFunctionUnderTest(true).getSectionValue(ESection.E_SECTION_PARAMS.swigValue(), false),
                                               m_nodeId);
                
                action.addAllFireEventTypes();
                action.addDataChangedEvent(m_nodeId, m_testSpec);
                return action;
            }
            
        });

        
        builder.label("Params:");
        
        m_paramsControl = 
                TBControlText.createForList(builder, 
                                            "Values of function parameters separated by commas.\n" +
                                                    "Example:\n    10, 20, 's', &&x\n" +
                                                    "    Note: 'x' must be declared in 'Variables' section.", 
                                            "pushx, growx", 
                                            CTestFunction.ESection.E_SECTION_PARAMS.swigValue(), 
                                            m_nodeId, 
                                            EHControlId.EFuncUnderTestParams, 
                                            SWT.BORDER);

        m_varsProposalProvider = new VariablesContentProposal(new String[0], null);
        m_varsProposalProvider.setFiltering(true);
        m_varsProposalProvider.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);

        /*
        m_paramsAutoComplete = new AsystAutoCompleteField(m_paramsControl.getControl(), 
                                                          new AsystTextContentAdapter(), 
                                                          m_varsProposalProvider, 
                                                          ContentProposalAdapter.PROPOSAL_INSERT);
                                                          
        
        m_paramsAutoComplete = new VariablesContentProposal(new String[0], null);
        m_paramsAutoComplete.setFiltering(true);
        m_paramsAutoComplete.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT); 
        */
        ISysUIUtils.addContentProposalsAdapter(m_paramsControl.getControl(),
                                               m_varsProposalProvider,
                                               new AsystTextContentAdapter(),
                                               ContentProposalAdapter.PROPOSAL_INSERT,
                                               UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        
        AsystContentProposalProvider.setMakeDefaultSelectionInContentProposals(UIPrefsPage.isMakeDefaultSelectionInContentProposals());
        
        Button refreshParametersBtn = builder.button(IconProvider.INSTANCE.getIcon(EIconId.ERefresh), 
                                                     "gapleft 10, wrap");
        refreshParametersBtn.addSelectionListener(m_refreshListener);
        UiTools.setToolTip(refreshParametersBtn, "Refresh globals.\nPress this button to get the latest list of variables from winIDEA.\n" +
                                                 "Press it also after recompiling the changed source code.");
        
        builder.label("Ret. val. name:", "gaptop 15");
        
        m_retValControl = 
                TBControlText.createForMixed(builder, 
                                             "Name of variable used to store function return value.\n" +
                                             "This name can later be used in verification expressions.\n\n" +
                                             "Note: If this field is empty, and function under test returns non-void type,\n" +
                                             "    then function return value is stored into variable _isys_rv. If by coincidence\n"
                                             + "    you have global variable with the same name on target, and you want to evaluate\n"
                                             + "    it, then specify your own return value name to prevent hiding of the global variable.\n\n"
                                             + "Example:\n    retVal", 
                                             "w 100:30%:50%, pushx, growx, gaptop 15, split 3, wrap", 
                                             CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue(), 
                                             m_nodeId, 
                                             EHControlId.EFuncUnderTestRetValName, 
                                             SWT.BORDER);
        
        builder.separator("span 2, growx, gaptop 20, gapbottom 20, wrap", SWT.HORIZONTAL);

        m_isInheritTimeoutTB = createTristateInheritanceButton(builder, "skip, gaptop 15, w min:pref:pref, wrap");
        m_isInheritTimeoutTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_TIMEOUT));
        
        String ttipText = "Test case timeout (in milliseconds) specifies when to terminate test case if it does not end normally.\n"
            + "If this value is 0, infinite timeout is used. If this value is not set (the field is empty),\n"
            + "then global test timeout is used (see 'File | Properties | Run Configuration' dialog).";
        builder.label("Test exec. timeout:", "").setToolTipText(ttipText);

        m_timeoutTBCtrl = TBControlText.createForMixed(builder, 
                                                       ttipText, 
                                                       "wmax 300, pushx, growx, split 2", 
                                                       SectionIds.E_SECTION_TIMEOUT.swigValue(), 
                                                       ENodeId.FUNCTION_NODE, 
                                                       EHControlId.ETestCaseTimeout, 
                                                       SWT.BORDER);
        
        builder.label("ms", "gapleft 7, wrap");
        
        m_isInheritCoreIdTB = createTristateInheritanceButton(builder, "skip, gaptop 15, w min:pref:pref, wrap");
        m_isInheritCoreIdTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_CORE_ID));


        ttipText = "In multi-core target configuration enter core ID where this test should be run.\n" +
                   "Available core IDs can be configured in 'File | Properties' dialog.";
        builder.label("Core ID:", "").setToolTipText(ttipText);
        
        m_coreIdTBCtrl = TBControlText.createForMixed(builder, 
                                                      ttipText, 
                                                      "wmax 300, pushx, growx, wrap", 
                                                      SectionIds.E_SECTION_CORE_ID.swigValue(), 
                                                      ENodeId.FUNCTION_NODE, 
                                                      EHControlId.ECoreId, 
                                                      SWT.BORDER);

        m_coreIdProposals = 
                ISysUIUtils.addContentProposalsAdapter(m_coreIdTBCtrl.getControl(),
                                                       ContentProposalAdapter.PROPOSAL_REPLACE,
                                                       UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        /* m_coreIdAutoComplete = 
                new AsystAutoCompleteField(m_coreIdTBCtrl.getControl(), 
                                           new TextContentAdapter(), 
                                           , 
                                           ContentProposalAdapter.PROPOSAL_REPLACE); */
        
        // change in core ID is not updated immediately in testID, since IDs
        // may change only on explicit user request. The problem is that 
        // also other ID variables like derived index change constantly, so
        // test IDs would be to indeterministic. Maybe we can add config option,
        // where IDs would be updated on the fly.

        m_funcNameControl.setMainTagEditor(m_funcTagCommentEditor);
        
//       Button createFlowChartBtn = builder.button("Flow chart", "skip, w 100:100:100, gaptop 20");
//        createFlowChartBtn.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                DiagramUtils.openFlowChart(m_funcNameControl.getText(), true);
//            }
//        });
        
        return getScrollableParent(functionPanel);
    }


    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testSpec != null;
        
        m_funcNameControl.setEnabled(isEnabled);
        m_funcTagCommentEditor.setEnabled(isEnabled);
        m_paramsControl.setEnabled(isEnabled);
        m_retValControl.setEnabled(isEnabled);
        
        if (!isEnabled) {
            setInputForInheritCb(null, m_isInheritFuncTB);
            setInputForInheritCb(null, m_isInheritParamsTB);
            setInputForInheritCb(null, m_isInheritCoreIdTB);
            setInputForInheritCb(null, m_isInheritTimeoutTB);
            
            m_funcText.getControl().setText("");
            m_paramsControl.clearInput();
            m_retValControl.clearInput();
            m_coreIdTBCtrl.clearInput();
            m_timeoutTBCtrl.clearInput();

            return;            
        }

        setInputForInheritCb(SectionIds.E_SECTION_FUNC, m_isInheritFuncTB);
        setInputForInheritCb(SectionIds.E_SECTION_PARAMS_PRIVATE, m_isInheritParamsTB);
        setInputForInheritCb(SectionIds.E_SECTION_CORE_ID, m_isInheritCoreIdTB);
        setInputForInheritCb(SectionIds.E_SECTION_TIMEOUT, m_isInheritTimeoutTB);

        setCurrentTS(SectionIds.E_SECTION_LOCALS);
        m_varsProposalProvider.setTestSpec(m_currentTestSpec);

        // The following lines are a workaround for bug in ContentProposalAdapter(),
        // because of which content proposal window pops up on setText(). If we
        // set it to empty string first, it does not pop-up. To reproduce:
        // comment the next line, then select function name with keyboard, and
        // while focus (cursor) is till in the function combo, click test spec in the
        // test spec tree. See also ContentProposalAdapter, line 1794 (case SWT.Modify:)
        m_paramsControl.clearInput(); // see comment before: ((Combo)m_funcHierachyControl.getControl()).setText("");
        m_funcText.getControl().setText("");

        setCurrentTS(SectionIds.E_SECTION_FUNC);
        m_funcTagCommentEditor.updateValueAndCommentFromTestBase(m_currentTestSpec);
        m_funcTagCommentEditor.getValueAndUpdateDecoration();
        
        m_funcText.setGlobalsProvider(GlobalsContainer.GC_FUNCTIONS, m_currentCoreId);
        
        m_funcNameControl.setInput(m_currentTestSpec.getFunctionUnderTest(false),
                                        m_isInherited);
        
        m_retValControl.setInput(m_currentTestSpec.getFunctionUnderTest(false),
                                          m_isInherited);
        
        setCurrentTS(SectionIds.E_SECTION_PARAMS_PRIVATE);
        m_paramsControl.setInput(m_currentTestSpec.getFunctionUnderTest(false), 
                                          m_isInherited);
        
        fillParamsAutoCompleteField(m_testSpec);
        
        m_varsProposalProvider.setCoreId(m_currentCoreId);
        
        setCurrentTS(SectionIds.E_SECTION_CORE_ID);
        m_coreIdTBCtrl.setInput(m_currentTestSpec, m_isInherited);
        String coreId = m_currentTestSpec.getCoreId();
        m_funcText.setCoreId(coreId);
        
        GlobalsProvider coreIdsGlobalsProvider = GlobalsConfiguration.instance().getGlobalContainer().
                                                 getCoreIdsGlobalsProvider();
        String[] proposals = coreIdsGlobalsProvider.getCachedGlobals();
        m_coreIdProposals.setProposals(proposals, coreIdsGlobalsProvider.getCachedDescriptions());
        
        // disable control if there are no core IDs defined in CTestEnvConfig, and
        // core ID of test spec is empty. If not empty, user has to have the ability
        // to delete it.
        String[] coreIds = m_model.getCoreIDs();
        m_coreIdTBCtrl.setEnabled(coreIds.length > 1  ||  
                                  (coreIds.length == 1  &&  !coreIds[0].isEmpty()) ||
                                  !coreId.isEmpty());
        
        setCurrentTS(SectionIds.E_SECTION_TIMEOUT);
        m_timeoutTBCtrl.setInput(m_currentTestSpec, m_isInherited);
    }


    protected void fillParamsAutoCompleteField(CTestSpecification testSpec) {

        GlobalsProvider globalVarsProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getVarsAndMacrosGlobalsProvider(m_currentCoreId);
        
        String[] proposals = globalVarsProvider.getCachedGlobals();
        String[] descriptions = globalVarsProvider.getCachedDescriptions();

        if (proposals == null) {
            proposals = new String[0];
        }
        
        // now add local variables from the 'locals' section
        ArrayList<String> proposalsList = new ArrayList<String>(Arrays.asList(proposals));
        if (testSpec != null) {
            StrStrMap localVars = new StrStrMap(); 
            testSpec.getLocalVariables(localVars);
            StrStrMapIterator iter = new StrStrMapIterator(localVars);
            while (iter.isValid()) {
                String key = iter.key();
                proposalsList.add(key);
                iter.inc();
            }
        }
        
        m_varsProposalProvider.setProposals(proposalsList.toArray(new String[0]), 
                                          descriptions);
    }
    

    public void refreshGlobals(String coreId) {
        
        if (m_funcText == null) {
            return; // does not exist yet because of lazy init.
        }
        
        m_funcText.setGlobalsProvider(GlobalsContainer.GC_FUNCTIONS, m_currentCoreId);
        m_funcText.refreshProposals();
        
        GlobalsProvider varsProvider = GlobalsConfiguration.instance(). 
                  getGlobalContainer().getVarsAndMacrosGlobalsProvider(coreId);
        
        m_varsProposalProvider.setProposals(varsProvider.getCachedGlobals(), 
                                          varsProvider.getCachedDescriptions());
    }
    
    
    class RefreshBtnListener implements SelectionListener {

        final IIConnectOperation refreshOperation = new IIConnectOperation() {

            @Override
            public void exec(JConnection jCon) {
                // refresh all global items, so that user doesn't have to click Refresh 
                // several times. If it will be a performance problem in the future,
                // fine tune it.
                ToolsRefreshGlobalsCmdHandler refreshGlobalsCmdHandler = new ToolsRefreshGlobalsCmdHandler();
                try {
                    refreshGlobalsCmdHandler.execute(null);
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), "Refresh failed!", ex);
                }
            }

            @Override
            public void setData(Object data) {}
        };
        
        @Override
        public void widgetSelected(SelectionEvent e) {
            ISysUIUtils.execWinIDEAOperation(refreshOperation, 
                                             m_shell,
                                             ConnectionProvider.instance().getDefaultConnection());
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }
    
    
    @Override
    public void clearSection() {
        AbstractAction action = createClearSectionAction(m_testSpecSectionIds);
        action.addAllFireEventTypes();
        action.addTreeChangedEvent(null, null);
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_FUNC.swigValue(),
                         SectionIds.E_SECTION_CORE_ID.swigValue(),
                         SectionIds.E_SECTION_TIMEOUT.swigValue()};
    }
}

