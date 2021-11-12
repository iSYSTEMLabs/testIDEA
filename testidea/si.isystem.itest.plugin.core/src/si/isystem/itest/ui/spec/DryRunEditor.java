package si.isystem.itest.ui.spec;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.connect.CTestDryRun;
import si.isystem.connect.CTestDryRun.EDryRunSectionIds;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.itest.wizards.TCGenDryRunPage;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;

public class DryRunEditor extends SectionEditorAdapter {

    protected TBControlTristateCheckBox m_isInheritTB;
    private TBControlTristateCheckBox m_isUpdateCvrgOnDryRunCb;
    private TBControlTristateCheckBox m_isUpdateProfilerOnDryRunCb;
//    private TBControlForTable m_assignTBCtrl;
    private VariablesContentProposal m_varsContentProposalProvider;
    private TBControlText m_profilerStatMultiplierTb;
    private TBControlText m_profilerStatOffsetTb;
    private MappingTableEditor m_dryRunTable;


    public DryRunEditor() {
        super(ENodeId.DRY_RUN_NODE, 
              SectionIds.E_SECTION_DRY_RUN);
    }


    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);

        MigLayout mig = new MigLayout("fill", 
                                      "[min!][fill][min!]",
                                      "[min!][fill][min!][min!]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        Label lbl = builder.label("Assignments performed during dry run:", "gaptop 3, gapbottom 5, span 2");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        m_isInheritTB = createTristateInheritanceButton(builder, 
                                                        "gapright 10, wrap");
        
        createTable(mainPanel);
        
        m_isUpdateCvrgOnDryRunCb = new TBControlTristateCheckBox(builder,
                               "Update required coverage statistics during dry run. (Default - no update)",
                               "Checked - dry run updates statistic items, which are already defined.\n"
                               + "          Statistic items, which are not defined, are not changed.\n"
                               + "Unchecked - no change is done during dry run.\n"
                               + "Grayed - default setting, no change.",                    
                               "gapleft 10, wrap",
                               EDryRunSectionIds.E_SECTION_UPDATE_COVERAGE.swigValue(),
                               ENodeId.DRY_RUN_NODE,
                               null);
        m_isUpdateProfilerOnDryRunCb = new TBControlTristateCheckBox(builder,
                               "Update required profiler statistics during dry run. (Default - no update)",
                               "Checked - dry run updates statistic items, which are already defined.\n"
                               + "          Statistic items, which are not defined, are not changed.\n"
                               + "Unchecked - no change is done during dry run.\n"
                               + "Grayed - default setting, no change.",                    
                               "gapleft 10, wrap",
                               EDryRunSectionIds.E_SECTION_UPDATE_PROFILER.swigValue(),
                               ENodeId.DRY_RUN_NODE,
                               null);

        m_isUpdateProfilerOnDryRunCb.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                enableControls();
            }
        });

        
        builder.label("Profiler statistic multiplier:", "gapleft 25, split");
        m_profilerStatMultiplierTb = TBControlText.createForMixed(builder, 
                               TCGenDryRunPage.PROFILER_DRY_RUN_MULTIPLIER_TOOLTIP, 
                               "wmin 100, wrap", 
                               EDryRunSectionIds.E_SECTION_PROFILER_MULTIPLIER.swigValue(), 
                               ENodeId.DRY_RUN_NODE, 
                               EHControlId.EDryRunProfMultiplier, 
                               SWT.BORDER);
        
        builder.label("Profiler statistic offset:", "gapleft 25, split");
        m_profilerStatOffsetTb = TBControlText.createForMixed(builder, 
                               TCGenDryRunPage.PROFILER_DRY_RUN_OFFSET_TOOLTIP, 
                               "wmin 100", 
                               EDryRunSectionIds.E_SECTION_PROFILER_OFFSET.swigValue(), 
                               ENodeId.DRY_RUN_NODE, 
                               EHControlId.EDryRunProfOffset, 
                               SWT.BORDER);
                                         
        builder.label("ns", "gapleft 8");

        return getScrollableParent(mainPanel);
    }


    private void createTable(Composite mainPanel) {
        
        m_dryRunTable = new MappingTableEditor(mainPanel, 
                                               "grow, span", 
                                               SWTBotConstants.BOT_DRY_RUN_TABLE, 
                                               ENodeId.DRY_RUN_NODE, 
                                               EDryRunSectionIds.E_SECTION_ASSIGN.swigValue(), 
                                               m_isInheritTB, 
                                               new InheritedActionProvider(SectionIds.E_SECTION_DRY_RUN),
                                               new String[]{"Variables", "Assigned expressions"});
    
        m_dryRunTable.setTooltip("This table defines assignments, which will be copied to section Variables\n\n"
                       + "during dry run. Before copying expressions are evaluated.\n");
    
        m_varsContentProposalProvider = new VariablesContentProposal(new String[0], null); 
        m_varsContentProposalProvider.setFiltering(true);
        m_varsContentProposalProvider.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
        
        m_dryRunTable.setContentProposals(m_varsContentProposalProvider, 
                                          m_varsContentProposalProvider);
    }


    @Override
    public void fillControlls() {

        boolean isEnabled = m_testSpec != null;
        m_isUpdateCvrgOnDryRunCb.setEnabled(isEnabled);
        m_isUpdateProfilerOnDryRunCb.setEnabled(isEnabled);
        m_profilerStatMultiplierTb.setEnabled(isEnabled);
        m_profilerStatOffsetTb.setEnabled(isEnabled);
        
        if (!isEnabled) {
            setInputForInheritCb(null, m_isInheritTB);
        
            m_isUpdateCvrgOnDryRunCb.clearInput();
            m_isUpdateProfilerOnDryRunCb.clearInput();
            m_dryRunTable.setInput(null, false);
            m_profilerStatMultiplierTb.setInput(null, false);
            m_profilerStatOffsetTb.setInput(null, false);
            return;
        }
        
        setCurrentTS(SectionIds.E_SECTION_DRY_RUN);
        setInputForInheritCb(SectionIds.E_SECTION_DRY_RUN, m_isInheritTB);
        CTestDryRun dryRun = m_currentTestSpec.getDryRun(false);
        m_dryRunTable.setInput(dryRun, m_isInherited);

        m_isUpdateCvrgOnDryRunCb.setInput(dryRun, m_isInherited);
        m_isUpdateProfilerOnDryRunCb.setInput(dryRun, m_isInherited);

        m_profilerStatMultiplierTb.setInput(dryRun, false);
        m_profilerStatOffsetTb.setInput(dryRun, false);
        
        ExpressionsPanel.setVarsContentProposals(m_testSpec, m_currentCoreId, 
                                                 m_varsContentProposalProvider,
                                                 true);
        setCurrentTS(SectionIds.E_SECTION_LOCALS);
        m_varsContentProposalProvider.setTestSpec(m_currentTestSpec);

        m_varsContentProposalProvider.setCoreId(m_currentCoreId);
        enableControls();
    }

    
    private void enableControls() {
        
        boolean isUpdateProfiler = 
            m_currentTestSpec.getDryRun(true).isUpdateProfiler() == ETristate.E_TRUE;
        
        m_profilerStatMultiplierTb.setEnabled(isUpdateProfiler);
        m_profilerStatOffsetTb.setEnabled(isUpdateProfiler);
    }
    
    
    public void refreshGlobals(String coreId) {
        
        if (m_varsContentProposalProvider == null) {
            return; // does not exist yet because of lazy init.
        }
        
        GlobalsProvider varsProvider = GlobalsConfiguration.instance().
                   getGlobalContainer().getVarsAndMacrosGlobalsProvider(coreId);
        
        m_varsContentProposalProvider.setProposals(varsProvider.getCachedGlobals(), 
                                                   varsProvider.getCachedDescriptions());
    }    


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestSpecification.SectionIds.E_SECTION_DRY_RUN.swigValue()};
    }
}
