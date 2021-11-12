package si.isystem.itest.ui.spec;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStopCondition;
import si.isystem.connect.CTestStopCondition.EStopCondSections;
import si.isystem.connect.CTestStopCondition.EStopType;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.FDGUIBuilder;
import si.isystem.ui.utils.KGUIBuilder;

public class SysTestStopConditionEditor extends SectionEditorAdapter {

    private TBControlTristateCheckBox m_isInheritTBCheckBox;
    private TBControlRadio m_stopTypeTBC;
    private TBControlText m_timeoutTBC;
    private TBControlText m_rtExpressionTBC;
    
    private BPLogLocationControls m_commonControls;
    private TBControlTristateCheckBox m_isInheritTimeoutTB;
    private TBControlText m_timeoutTBCtrl;
    
    private TBControlTristateCheckBox m_isInheritCoreIdTB;
    private TBControlText m_coreIdTBCtrl;
    private AsystContentProposalProvider m_coreIdProposals;
    
    
    public SysTestStopConditionEditor(SectionIds sectionId, ENodeId nodeId) {
        super(nodeId, sectionId);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        Composite metaPanel = new Composite(parent, SWT.NONE);
        FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(metaPanel);

        MigLayout mig = new MigLayout("fillx", "[min!][min!][min!][min!][fill]");
        metaPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(metaPanel);
        String noRunRbTooltip, defaultRbTooltip, noRunRbText, defaultRbText;
        if (m_testSpecSectionIds[0] == SectionIds.E_SECTION_BEGIN_STOP_CONDITION) {
            builder.label("This page can define position of target execution point (address in PC) before the test starts.\n"
                    + "If stop type is defined, target is run until stop condition is met. This happens\n"
                    + "before variables, analyzer, stubs, ... are initialized or configured.", "gapbottom 15, span 2, wrap");
            noRunRbText = "No init";
            defaultRbText = "Default (No init)";
            noRunRbTooltip = "Target will not be run to reach start point for system test. Select this\n"
                    + "option, when target execution point is already at start position.";
            defaultRbTooltip = "The same as 'No init'.";
        } else {
            builder.label("This page defines if target is started by testIDEA for system test. Stop condition defines where or when\n"
                    + "the target should stop to end the test. If 'No exec' is selected, then the test must be executed by script\n"
                    + "function 'Init test'. After the target stops, test results are verified.", "gapbottom 15, span 2, wrap");
            noRunRbText = "No exec";
            defaultRbText = "Default (No exec)";
            noRunRbTooltip = "Target will not be started by testIDEA to perform test. When this\n"
                    + "option is selected, script function 'Init test' must be used to start and stop the target.";
            defaultRbTooltip = "The same as 'No exec'.";
        }
        
        m_isInheritTBCheckBox = createTristateInheritanceButton(builder, "gapleft 8, wrap");
        m_isInheritTBCheckBox.setActionProvider(new InheritedActionProvider(m_testSpecSectionIds[0]));

        builder.label("Stop type:");
        
        m_stopTypeTBC = new TBControlRadio(builder, 
                                           new String[]{"Breakpoint", "Stop", "Real-time expr.", noRunRbText, defaultRbText},
                                           
                                           new String[]{"Target will be running until the breakpoint specified below is hit.",
                                                        "Target will be running until timeout expires.",
                                                        "Target will be running until real-time expression evaluates to true.\n" +
                                                        "Your target must support real-time access for this mode.",
                                                        noRunRbTooltip,
                                                        defaultRbTooltip},
                                                        
                                           new String[]{"breakpoint", "stop", "rtExpression", "noRun", ""},
                                           
                                           "wmax pref, wrap", 
                                           CTestStopCondition.EStopCondSections.E_SECTION_STOP_TYPE.swigValue(),
                                           m_nodeId, 
                                           null);
        
        m_stopTypeTBC.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                CTestStopCondition cond = 
                        CTestStopCondition.cast(m_testSpec.getTestBase(m_testSpecSectionIds[0].swigValue(), 
                                                                       true));
                setEditable(cond.getStopType(), false);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        builder.label("Timeout:");
        
        m_timeoutTBC = TBControlText.createForMixed(builder, 
                                         "Timeout specifies time to wait until stop action is exeuted, depending on stop type.\n" +
                                                 "If stop type is set to 'Breakpoint', then breakpoint is set after this timeout expires.\n" +
                                                 "If stop type is set to 'Stop', then target is stopped after this timeout.\n" +
                                                 "If stop type is set to 'Real-time expr.', then expression evaluation starts after this timeout.", 
                                         "w 100!, split 2", 
                                         EStopCondSections.E_SECTION_TIMEOUT.swigValue(), 
                                         m_nodeId, 
                                         null,
                                         SWT.BORDER);
    
        builder.label("ms", "gapleft 10, wrap");
        builder.label("Real-time expr:", "");

        m_rtExpressionTBC = TBControlText.createForMixed(builder, 
                                              "Realtime expression to be evaluated when stop type is set to 'Real-time expr.'.", 
                                              "wmin 100, span 5, growx, wrap",
                                              EStopCondSections.E_SECTION_RT_EXPRESSION.swigValue(), 
                                              m_nodeId, 
                                              null,
                                              SWT.BORDER);
        
        m_commonControls = new BPLogLocationControls();
        
        KGUIBuilder group = builder.group("Breakpoint settings", 
                                          "gaptop 15, span 5, growx, wrap", true, 
                                          "fillx", "[min!][fill]", null);
        
        m_commonControls.createBPConditionControlsTPC(group, m_nodeId,
                    EStopCondSections.E_SECTION_BP_CONDITION_COUNT.swigValue(),
                    EStopCondSections.E_SECTION_BP_CONDITION_EXPR.swigValue());
        
        m_commonControls.createLocationControls(group, m_nodeId);
        
        if (m_testSpecSectionIds[0] == SectionIds.E_SECTION_END_STOP_CONDITION) {        
            
            builder.label("Test exec. timeout:", "gaptop 15");
            
            m_timeoutTBCtrl = TBControlText.createForMixed(builder, 
                  "Test case timeout (in milliseconds) specifies when to terminate test case if it does not end normally.\n"
                  + "If this value is 0, infinite timeout is used. If this value is not set (the field is empty),\n"
                  + "then global test timeout is used (see 'File | Properties | Run Configuration' dialog).",
                  "span 2, w 200, gaptop 15, split 2",
                  SectionIds.E_SECTION_TIMEOUT.swigValue(), 
                  m_nodeId,
                  null,
                  SWT.BORDER);
            
            m_isInheritTimeoutTB = createTristateInheritanceButton(builder, 
                                                                   "gapleft 20, gaptop 15, w min:pref:pref, wrap");
            m_isInheritTimeoutTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_TIMEOUT));

            // there is only one coreID per test case, so init and exec test run on the same core.
            String ttipText = "In multi-core target configuration enter core ID where this test should be run.\n" +
                    "Available core IDs can be configured in 'File | Properties' dialog.";
            builder.label("Core ID:", "").setToolTipText(ttipText);

            m_coreIdTBCtrl = TBControlText.createForMixed(builder, 
                                                          ttipText, 
                                                          "span 2, w 200, split 2", 
                                                          SectionIds.E_SECTION_CORE_ID.swigValue(), 
                                                          m_nodeId, 
                                                          EHControlId.ECoreId, 
                                                          SWT.BORDER);

            m_coreIdProposals = 
                    ISysUIUtils.addContentProposalsAdapter(m_coreIdTBCtrl.getControl(),
                                                           ContentProposalAdapter.PROPOSAL_REPLACE,
                                                           UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());

            m_isInheritCoreIdTB = createTristateInheritanceButton(builder, "gapleft 20, w min:pref:pref, wrap");
            m_isInheritCoreIdTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_CORE_ID));
        }   

        
        return metaPanel;
    }


    @Override
    public void fillControlls() {
        
        if (m_testSpec == null) {
            setInputForInheritCb(null, m_isInheritTBCheckBox);
            if (m_isInheritTimeoutTB != null) {
                setInputForInheritCb(null, m_isInheritTimeoutTB);
                setInputForInheritCb(null, m_isInheritCoreIdTB);
            }
            


            m_timeoutTBC.clearInput();
            m_rtExpressionTBC.clearInput();
            if (m_timeoutTBCtrl != null) {
                m_timeoutTBCtrl.clearInput();
                m_coreIdTBCtrl.clearInput();
            }

            setEnabled(false);
            return;
        } 

        setInputForInheritCb(m_testSpecSectionIds[0], m_isInheritTBCheckBox);
        if (m_isInheritTimeoutTB != null) {
            setInputForInheritCb(SectionIds.E_SECTION_TIMEOUT, m_isInheritTimeoutTB);
            setInputForInheritCb(SectionIds.E_SECTION_CORE_ID, m_isInheritCoreIdTB);
        }
        
        setCurrentTS(m_testSpecSectionIds[0]);
        
        CTestBase testBase = m_currentTestSpec.getTestBase(m_testSpecSectionIds[0].swigValue(), false);
        CTestStopCondition stopCond = CTestStopCondition.cast(testBase);
        
        m_stopTypeTBC.setInput(stopCond, m_isInherited);
        m_timeoutTBC.setInput(stopCond, m_isInherited);
        m_rtExpressionTBC.setInput(stopCond, m_isInherited);
        
        CTestLocation location = stopCond.getBreakpointLocation(false);
        m_commonControls.setCoreId(m_currentCoreId);
        m_commonControls.setBPInput(stopCond, m_isInherited);

        LocationIDProvider locProvider = new LocationIDProvider() {
            @Override
            public String getId() {
                return m_testSpec.getId();
            }
        };
        m_commonControls.setLocationInput(location, locProvider);
        
        setEditable(stopCond.getStopType(), m_isInherited);
        
        setCurrentTS(SectionIds.E_SECTION_TIMEOUT);
        if (m_timeoutTBCtrl != null) {
            m_timeoutTBCtrl.setInput(m_currentTestSpec, m_isInherited);
        
            setCurrentTS(SectionIds.E_SECTION_CORE_ID);
            m_coreIdTBCtrl.setInput(m_currentTestSpec, m_isInherited);
            // disable control if there are no core IDs defined in CTestEnvConfig, and
            // core ID of test spec is empty. If not empty, user has to have the ability
            // to delete it.
            String[] coreIds = m_model.getCoreIDs();
            String coreId = m_currentTestSpec.getCoreId();
            m_coreIdTBCtrl.setEnabled(coreIds.length > 1  ||  
                                      (coreIds.length == 1  &&  !coreIds[0].isEmpty()) ||
                                      !coreId.isEmpty());

            GlobalsProvider coreIdsGlobalsProvider = GlobalsConfiguration.instance().getGlobalContainer().
                    getCoreIdsGlobalsProvider();
            String[] proposals = coreIdsGlobalsProvider.getCachedGlobals();
            m_coreIdProposals.setProposals(proposals, coreIdsGlobalsProvider.getCachedDescriptions());
        }
    }
    

    @Override
    public boolean isActive() {
        if (m_testSpec == null) {
            return false;
        }

        SectionIds sectionId = m_testSpecSectionIds[0];
        CTestBase stopCondTB = m_testSpec.isInheritSection(sectionId) ?
                m_mergedTestSpec.getTestBase(sectionId.swigValue(), true) :
                m_testSpec.getTestBase(sectionId.swigValue(), true);
                
        CTestStopCondition stopCond = CTestStopCondition.cast(stopCondTB);
        return stopCond.getStopType() != EStopType.E_NO_RUN;
    }    
    
    
    private void setEnabled(boolean isEnabled) {
        m_isInheritTBCheckBox.setEnabled(isEnabled);
        m_stopTypeTBC.setEnabled(isEnabled);
        m_timeoutTBC.setEnabled(isEnabled);
        m_rtExpressionTBC.setEnabled(isEnabled);
    }


    private void setEditable(CTestStopCondition.EStopType stopType, boolean isInherited) {

        if (isInherited) {
            m_stopTypeTBC.setEditable(false);
            m_timeoutTBC.setEditable(false);
            m_rtExpressionTBC.setEditable(false);
            return;
        }

        switch (stopType) {
        case E_BREAKPOINT:
            m_timeoutTBC.setEnabled(true);
            m_timeoutTBC.setEditable(true);
            m_rtExpressionTBC.setEnabled(false);
            m_commonControls.setEnabled(true);
            m_commonControls.setEditable(true);
            break;
        case E_NO_RUN:
            m_timeoutTBC.setEnabled(false);
            m_rtExpressionTBC.setEnabled(false);
            m_commonControls.setEnabled(false);
            break;
        case E_RT_EXPRESSION:
            m_timeoutTBC.setEnabled(true);
            m_timeoutTBC.setEditable(true);
            m_rtExpressionTBC.setEnabled(true);
            m_rtExpressionTBC.setEditable(true);
            
            m_commonControls.setEnabled(false);
            break;
        case E_STOP:
            m_timeoutTBC.setEnabled(true);
            m_timeoutTBC.setEditable(true);
            m_rtExpressionTBC.setEnabled(false);
            m_commonControls.setEnabled(false);
            break;
        default:
            // it is no harm to enable more in case of unexpected value
            m_timeoutTBC.setEnabled(true);
            m_rtExpressionTBC.setEnabled(true);
            m_commonControls.setEnabled(true);
            
            m_timeoutTBC.setEditable(true);
            m_rtExpressionTBC.setEditable(true);
            m_commonControls.setEditable(true);
            break;
        }
    }


    @Override
    public int [] getSectionIdsForTableEditor() { // TODO pass section Id as parameter
        return new int[]{m_testSpecSectionIds[0].swigValue()};
    }
}
