package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.connect.CTestHIL.ETestHILSections;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

class HilSpecEditor extends SectionEditorAdapter {

    // TBControlForTable m_tableHierarchyControl;
    private TBControlTristateCheckBox m_isInheritTBCheckBox;

    private AsystContentProposalProvider m_paramNameContentProposalProvider;
    private MappingTableEditor m_hilTable;

    
    HilSpecEditor() {
        super(ENodeId.HIL_NODE, SectionIds.E_SECTION_HIL);
    }


    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        Composite mainPanel = new Composite(scrolledPanel, SWT.BORDER);

        MigLayout mig = new MigLayout("fill", "[min!][min!]", "[min!][fill]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);

        Label lbl = builder.label("HIL outputs:", "gaptop 3, gapbottom 5");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        
        m_isInheritTBCheckBox = createTristateInheritanceButton(builder, "gapright 10, wrap");
        
        m_hilTable = new MappingTableEditor(mainPanel, 
                                             "grow, span 2", 
                                             SWTBotConstants.BOT_HIL_TABLE, 
                                             ENodeId.HIL_NODE, 
                                             ETestHILSections.E_SECTION_HIL_PARAMS.swigValue(), 
                                             m_isInheritTBCheckBox, 
                                             new InheritedActionProvider(SectionIds.E_SECTION_HIL),
                                             new String[]{"HIL Parameter Path", "Value"});
     
        m_hilTable.setTooltip("This table defines HIL output values, which are applied before the test.\n\n"
                        + "The first column contains output path, for example 'DIN.DIN0'.\n"
                        + "The second column defines output value, for example '1'.\n"
                        + "Consult your HW manual for available parameters and values.");
     
        m_paramNameContentProposalProvider = new AsystContentProposalProvider();
        m_hilTable.setContentProposals(m_paramNameContentProposalProvider, null);
        
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }


    @Override
    public void fillControlls() {
        
        if (m_testSpec == null) {
            m_isInheritTBCheckBox.clearInput();
            m_isInheritTBCheckBox.setEnabled(false);
            m_hilTable.setInput(null, false);
            return;
        } 

        setCurrentTS(SectionIds.E_SECTION_HIL);
        setInputForInheritCb(SectionIds.E_SECTION_HIL, m_isInheritTBCheckBox);
        m_hilTable.setInput(m_currentTestSpec.getHIL(false), m_isInherited);
    }
    

    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        m_hilTable.setSelection(lineNo);
    }
    
    
    public void refreshGlobals() {
        
        if (m_paramNameContentProposalProvider == null) {
            return; // does not exist yet because of lazy init.
        }
        
        GlobalsProvider provider = GlobalsConfiguration.instance().
                getGlobalContainer().getHilGlobalsProvider();
        
        m_paramNameContentProposalProvider.setProposals(provider.getCachedGlobals(), provider.getCachedDescriptions());
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestSpecification.SectionIds.E_SECTION_HIL.swigValue()};
    }
}


