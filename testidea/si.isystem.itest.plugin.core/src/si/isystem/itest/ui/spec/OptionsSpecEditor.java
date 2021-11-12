package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

class OptionsSpecEditor extends SectionEditorAdapter {

//    TBControlForTable m_tableHierarchyControl;
    private TBControlTristateCheckBox m_isInheritTBCheckBox;
    private MappingTableEditor m_optionsTable;


    public OptionsSpecEditor() {
        super(ENodeId.OPTIONS_NODE, SectionIds.E_SECTION_OPTIONS);
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        Composite mainPanel = new Composite(scrolledPanel, SWT.BORDER);
        
        MigLayout mig = new MigLayout("fill", "[min!][min!]", "[min!][fill]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);

        Label lbl = builder.label("winIDEA configuration options:", "gaptop 3, gapbottom 5");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        
        m_isInheritTBCheckBox = createTristateInheritanceButton(builder, "gapright 10, wrap");
        
        m_optionsTable = new MappingTableEditor(mainPanel, 
                                                "grow, span 2", 
                                                SWTBotConstants.BOT_OPTIONS_TABLE, 
                                                ENodeId.OPTIONS_NODE, 
                                                SectionIds.E_SECTION_OPTIONS.swigValue(), 
                                                m_isInheritTBCheckBox, 
                                                new InheritedActionProvider(SectionIds.E_SECTION_OPTIONS),
                                                new String[]{"winIDEA Option Path", "Value"});
     
        m_optionsTable.setTooltip("This table defines configuration for winIDEA, which is applied before the test.\n\n"
                        + "The first column contains option path, for example 'Debug.DownloadFiles.File[0].Options.LoadCode'.\n"
                        + "The second column defines option value, for example '0'.\n"
                        + "See winIDEA 'Help | Display options ...' for the list of available options.\n\n"
                        + "IMPORTANT: Host variables used as parameters for this section\n"
                        + "    must be defined in test case which executes before this one,\n"
                        + "    because Variables section is processed AFTER options are set.\n");
     
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }


    @Override
    public void fillControlls() {
        
        if (m_testSpec == null) {
            m_isInheritTBCheckBox.clearInput();
            m_isInheritTBCheckBox.setEnabled(false);
            m_optionsTable.setInput(null, false);
            return;
        } 

        setCurrentTS(SectionIds.E_SECTION_OPTIONS);
        setInputForInheritCb(SectionIds.E_SECTION_OPTIONS, m_isInheritTBCheckBox);
        m_optionsTable.setInput(m_currentTestSpec, m_isInherited);
    }
    

    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        m_optionsTable.setSelection(lineNo);
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_OPTIONS.swigValue()};
    }
}


