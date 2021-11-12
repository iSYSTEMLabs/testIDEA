package si.isystem.itest.ui.spec;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestPersistentVars.EPersistVarsSections;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.PersistVarsProposalsProvider;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.ui.comp.TBControlCheckBox;
import si.isystem.itest.ui.comp.TBControlFor_K_Table;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.KTableModelForSequence;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class PersistentVarsEditor  extends SectionEditorAdapter {

    private static final String PROPOSALS_UPDATED = "Proposals updated";

    private static final String REFRESH_DELETE_PROPOSALS = "Refresh delete proposals";

    private TBControlTristateCheckBox m_isInheritTBCheckBox;

    private MappingTableEditor m_declTable;
    private TBControlCheckBox m_isDeleteAllPersistVarsTCb;
    
    private KTableModelForSequence m_deleteTableModel;
    private TBControlFor_K_Table m_deleteTable;
    
    private VariablesContentProposal m_typeNameProposalProvider;
    private VariablesContentProposal m_deletedVarsContentProposalProvider;

    private Button m_refreshDeleteProposalsBtn;

    public final static int DECL_TABLE_ID = 0;
    public final static int DELETE_TABLE_ID = 1;

    private static final int VAR_NAME_COLUMN = 1;
    
    PersistentVarsEditor() {
        super(ENodeId.PERSISTENT_VARS_NODE, SectionIds.E_SECTION_PERSIST_VARS);
    }


    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);
        
        MigLayout mig = new MigLayout("fill", "[grow][min][min]", "[min][grow][min][grow]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);

        m_typeNameProposalProvider = createVarsProposalProvider();
        m_deletedVarsContentProposalProvider = createVarsProposalProvider();
        
        Label lbl = builder.label("Declarations of persistent variables", "gaptop 10, gapbottom 4");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));

        m_isInheritTBCheckBox = createTristateInheritanceButton(builder, "gaptop 10, skip, al right, wrap");
        m_isInheritTBCheckBox.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_PERSIST_VARS));
        
        
        m_declTable = new MappingTableEditor(builder.getParent(), 
                                             "grow, span, wrap", 
                                             SWTBotConstants.BOT_PERSIST_VARS_TABLE, 
                                             ENodeId.PERSISTENT_VARS_NODE, 
                                             EPersistVarsSections.E_SECTION_DECL.swigValue(), 
                                             m_isInheritTBCheckBox, 
                                             new InheritedActionProvider(SectionIds.E_SECTION_PERSIST_VARS),
                                             // keep the same order as in persistent vars
                                             new String[]{"Variable name", "Variable type"});
        
        m_declTable.setTooltip("This table declares persistent variables, which can be used in more than one test.\nThey are not deleted after test ends.\n\n"
                + "The first column contains name of persistent variable, for example 'counter', 'mode', ...\n"
                + "The second column defines type of persistent variable, for example 'int', 'char *', 'MyStruct', ...\n"
                + "If the name is the same as name of a global variable, the global variable is hidden!");
        
        m_declTable.setContentProposals(null, m_typeNameProposalProvider);
        
        m_declTable.addModelListener(new IKTableModelChangedListener() {
            
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                // update vars proposal for each declaration added
                String coreId = getCoreId();
                refreshGlobals(coreId);
            }
        });

        
        lbl = builder.label("Deleted persistent variables", "gaptop 20");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        
        m_isDeleteAllPersistVarsTCb = 
                new TBControlCheckBox(builder, 
                                      "Delete all persistent variables",
                                      "If checked, all persistent variables are deleted after test completes.",
                                      "gapleft 7, gaptop 20, gapright 10",
                                      EPersistVarsSections.E_SECTION_IS_DELETE_ALL.swigValue(),
                                      ENodeId.PERSISTENT_VARS_NODE,
                                      null);

        m_isDeleteAllPersistVarsTCb.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_deleteTable.setEnabled(!m_isDeleteAllPersistVarsTCb.isSelected());
                m_refreshDeleteProposalsBtn.setEnabled(!m_isDeleteAllPersistVarsTCb.isSelected());
            }
        });

        m_refreshDeleteProposalsBtn = builder.button(REFRESH_DELETE_PROPOSALS, 
                                                     "gaptop 20, wrap");
        UiTools.setToolTip(m_refreshDeleteProposalsBtn, 
                           "This button fills content proposals for the table below with persistent variables,\n"
                         + "which exist at this point.\n\n"
                         + "If the button is disabled, list of persistent variables was obtained automatically.\n\n"
                         + "When there are to many test cases to search (search would take to long),\n"
                         + "the button is enabled and you can trigger the action manually when needed.");
        m_refreshDeleteProposalsBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                PersistVarsProposalsProvider propProvider = new PersistVarsProposalsProvider();
                propProvider.getExactPersistVarsProposals(m_testSpec, 
                                                          m_deletedVarsContentProposalProvider,
                                                          true);
            }
        });
        
        // create table for deleted variables
        m_deleteTableModel = new KTableModelForSequence(EPersistVarsSections.E_SECTION_DELETE.swigValue(), 
                                                        ENodeId.PERSISTENT_VARS_NODE, 
                                                        "Variable name");
        
        m_deleteTableModel.setMainTooltip("Name of persistent variable, which will be deleted after the test completes.");
        m_deleteTableModel.setAutoCompleteProposals(VAR_NAME_COLUMN,
                                                    m_deletedVarsContentProposalProvider,
                                                    ContentProposalAdapter.PROPOSAL_INSERT);
        
        m_deleteTable = new TBControlFor_K_Table(builder.getParent(),
                                                 SWTBotConstants.BOT_DEL_PERSIST_VARS_TABLE,
                                                 EPersistVarsSections.E_SECTION_DELETE.swigValue(),
                                                 "span, grow",
                                                 ENodeId.PERSISTENT_VARS_NODE, 
                                                 m_deleteTableModel);
        
        return getScrollableParent(mainPanel);
    }

    
    private VariablesContentProposal createVarsProposalProvider() {
        VariablesContentProposal varProposalProvider = new VariablesContentProposal(new String[0], 
                                                                                    null); 
        varProposalProvider.setFiltering(true);
        varProposalProvider.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
        return varProposalProvider;
    }


    @Override
    public void fillControlls() {
        
        if (m_testSpec == null) {
            m_declTable.setInput(null, false);
            m_deleteTable.setInput(null, false);
            m_isDeleteAllPersistVarsTCb.setInput(null, false);
            return;
        } 

        
        setCurrentTS(SectionIds.E_SECTION_PERSIST_VARS);
        setInputForInheritCb(SectionIds.E_SECTION_PERSIST_VARS, m_isInheritTBCheckBox);
        
        CTestPersistentVars persistVars = m_currentTestSpec.getPersistentVars(false);
        m_declTable.setInput(persistVars, m_isInherited);
        
        m_isDeleteAllPersistVarsTCb.setInput(persistVars, m_isInherited);
        
        m_deleteTableModel.setData(persistVars);
        m_deleteTable.setInput(persistVars, m_isInherited);
        m_deleteTable.setEnabled(!persistVars.isDeleteAll()  &&  !m_isInherited);
        
        if (!persistVars.isDeleteAll()) {

            PersistVarsProposalsProvider propProvider = new PersistVarsProposalsProvider();
            propProvider.getExactPersistVarsProposals(m_testSpec, 
                                                      m_deletedVarsContentProposalProvider,
                                                      false);
            propProvider.openWarningDialog("List of proposals for persitent variables is not complete, because there are too many "
                    + "test cases to search.\n\n"
                    + "Press the 'Proposals' button to get complete list of proposals. "
                    + "Operation may take few seconds.");

            if (propProvider.isSearchTerminated()) {
                m_refreshDeleteProposalsBtn.setText(REFRESH_DELETE_PROPOSALS);
                m_refreshDeleteProposalsBtn.setEnabled(true);
            } else {
                m_refreshDeleteProposalsBtn.setText(PROPOSALS_UPDATED);
                m_refreshDeleteProposalsBtn.setEnabled(false);
            }
        } else {
            m_deletedVarsContentProposalProvider.setProposals(new String[0], new String[0]);
            m_refreshDeleteProposalsBtn.setText(PROPOSALS_UPDATED);
            m_refreshDeleteProposalsBtn.setEnabled(false);
        }
    }
    

    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        if (tableId == DECL_TABLE_ID) {
            m_declTable.setSelection(lineNo);
        } else {
            m_deleteTable.setSelection(lineNo);
        }
    }


    public void refreshGlobals(String coreId) {
        GlobalsContainer globalsContainer = GlobalsConfiguration.instance().getGlobalContainer();

        GlobalsProvider provider = globalsContainer.getTypesGlobalsProvider(coreId);
        
        m_typeNameProposalProvider.setProposals(provider.getCachedGlobals(), provider.getCachedDescriptions());
        
        // m_deletedVarsContentProposalProvider is set in fillControls()
        
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_PERSIST_VARS.swigValue()};
    }
}
