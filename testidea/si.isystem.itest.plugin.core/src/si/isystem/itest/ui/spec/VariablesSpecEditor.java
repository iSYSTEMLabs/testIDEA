package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.globals.VariablesGlobalsProvider;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.IVariable.EType;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JCompoundType;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SEFormatter;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.dialogs.StructMembersSelectionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.itest.wizards.newtest.NewTCVariablesPage;
import si.isystem.itest.wizards.newtest.NewTCWizard;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class VariablesSpecEditor extends SectionEditorAdapter {

    private VariablesContentProposal m_typeNameProposalProvider;
    private VariablesContentProposal m_varAndValueProposalProvider;
    
    private TBControlTristateCheckBox m_isInheritDeclarationsTB;
    private TBControlTristateCheckBox m_isInheritInitTB;
    private MappingTableEditor m_declTable;
    private MappingTableEditor m_initTable;
    private Button m_copyBtn;
    private Button m_addComplexVarsBtn;
    private Button m_varsWizardBtn;

    public final static int DECL_TABLE_ID = 1;
    public final static int INIT_TABLE_ID = 2;
    
    VariablesSpecEditor() {
        super(ENodeId.VARS_NODE, SectionIds.E_SECTION_LOCALS, SectionIds.E_SECTION_INIT);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);
        
        mainPanel.setLayout(new MigLayout("fill"));

        createProposalProviders();
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        SashForm sash = new SashForm(builder.getParent(), SWT.VERTICAL);
        sash.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_GRAY));
        
        // wizard dialog size is set in handler
        sash.setLayoutData("grow");

        Composite declPanel = new Composite(sash, SWT.NONE);
        KGUIBuilder declBuilder = new KGUIBuilder(declPanel);
        declPanel.setLayout(new MigLayout("fill", "[grow][min]", "[min][grow]"));
        declPanel.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        Label lbl = declBuilder.label("Declarations of test local variables", "gaptop 3, gapbottom 5");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));

        m_isInheritDeclarationsTB = createTristateInheritanceButton(declBuilder, "gapright 10, wrap");
        
        m_declTable = new MappingTableEditor(declBuilder.getParent(), 
                                                "grow, span 2", 
                                                SWTBotConstants.VARS_DECL_KTABLE, 
                                                ENodeId.VARS_NODE, 
                                                SectionIds.E_SECTION_LOCALS.swigValue(), 
                                                m_isInheritDeclarationsTB, 
                                                new InheritedActionProvider(SectionIds.E_SECTION_LOCALS),
                                                // keep the same order as in persistent vars
                                                new String[]{"Variable name", "Type"});
        
        m_declTable.setTooltip("This table declares test local variables, which can be used as function parameters.\n\n"
                          + "The first column contains variable name, for example 'counter', 'mode', ...\n"
                          + "The second column defines variable type, for example 'int', 'char *', 'MyStruct', ...\n"
                          + "If the name is the same as name of a global variable, the global variable is hidden!\n\n"
                          + "Variables, which will be used as function parameters can also be declared as:\n"
                          + "  decltype(<funcName>##<N>)\n"
                          + "  decltype( * <funcName>##<N>)\n"
                          + "  decltype( * <funcName>##<N>)[<K>]\n"
                          + "  decltype_ref(<funcName>##<N>)\n"
                          + "where:\n"
                          + "- N should be 0 for function return type, 1 for the first parameter, ...\n"
                          + "- K is declared array size\n"
                          + "See Help, section 'Concepts | Test Case Editor | Variables' for details.");
        
        m_declTable.setContentProposals(null, m_typeNameProposalProvider);
        
        m_declTable.addModelListener(new IKTableModelChangedListener() {
            
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                // update vars proposal for each declaration added
                String coreId = getCoreId();
                refreshGlobals(coreId);
            }
        });
        
        Composite initPanel = new Composite(sash, SWT.NONE);
        KGUIBuilder initBuilder = new KGUIBuilder(initPanel);
        initPanel.setLayout(new MigLayout("fill", "[grow][min][min][min][min][min]", "[min][grow]"));
        initPanel.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        lbl = initBuilder.label("Initialization of local and global variables", "gaptop 5");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        
        m_varsWizardBtn = createWizardBtn(initBuilder, "gaptop 5, gapright 7", 
                                          "Opens wizard for adding parameters and global variables\n"
                                          + "used in function under test and called functions.",
                                          ENodeId.VARS_NODE,
                                          new NewTCVariablesPage(null));
        
        createCopyVarBtn(initBuilder);
        createAddComplexVarsBtn(initBuilder);

        m_isInheritInitTB = createTristateInheritanceButton(initBuilder, "gapright 10, gaptop 5, wrap");
        m_initTable = new MappingTableEditor(initBuilder.getParent(), 
                                                "grow, span 5", 
                                                SWTBotConstants.VARS_INIT_KTABLE, 
                                                ENodeId.VARS_NODE, 
                                                SectionIds.E_SECTION_INIT.swigValue(), 
                                                m_isInheritInitTB, 
                                                new InheritedActionProvider(SectionIds.E_SECTION_INIT),
                                                new String[]{"Variable", "Value"});
        
        m_initTable.setTooltip("This table icontains initializations of local and global variables.\n"
                + "The first column contains variable name, for example 'counter', 'mode', ...\n"
                + "The second column contains value of a variable. Expressions are allowed. Examples:\n" 
                + "20, \"my text\" (for arrays and pointers), " 
                + "&&globalStruct, 2 + 3 * 4, ...\n\n" 
                + "Write <functionName>##<staticVarName> for function static variables, for example: myFunc##myStaticVar.\n"
                + "Fully qualified name of a variable is defined as \"<moduleName>\"#<varName>,,<downloadFileName>,\n"
                + "for example: \"main.c\"#iCounter,,executable.elf");
        
        m_initTable.setContentProposals(m_varAndValueProposalProvider, m_varAndValueProposalProvider);
        
        return getScrollableParent(mainPanel);
    }


    private void createCopyVarBtn(KGUIBuilder initBuilder) {
        m_copyBtn = initBuilder.button("", "gaptop 5, gapright 7");
        m_copyBtn.setImage(IconProvider.INSTANCE.getIcon(IconProvider.EIconId.EDownArrow));
        UiTools.setToolTip(m_copyBtn, "Copies variables declared above to initialization table below.\n"
                + "If variable already exists in the bottom table, it is not copied again.");
        m_copyBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyDeclarationsToInit();
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
    }


    private void createAddComplexVarsBtn(KGUIBuilder initBuilder) {
        m_addComplexVarsBtn = initBuilder.button("", "gaptop 5, gapright 20");
        m_addComplexVarsBtn.setImage(IconProvider.INSTANCE.getIcon(IconProvider.EIconId.EListItems));
        UiTools.setToolTip(m_addComplexVarsBtn, "Opens wizard for adding children of complex types.\n"
                + "For example, if variable 'x' is of type struct, the wizard can add initializations for\n"
                + "all members of the struct:\n    x.name\n    x.address\n    ...");
        m_addComplexVarsBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                addInitItemsWithComplexVarsDlg();
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        m_addComplexVarsBtn.setData(SWTBotConstants.SWT_BOT_ID_KEY, SWTBotConstants.VAR_MEMBERS_WIZARD);
    }


    protected void createProposalProviders() {
        m_varAndValueProposalProvider = createVarsProposalProvider();
        m_typeNameProposalProvider = createVarsProposalProvider();
    }
    
    
    private VariablesContentProposal createVarsProposalProvider() {
        VariablesContentProposal varProposalProvider = new VariablesContentProposal(new String[0], 
                                                                                    null); 
        varProposalProvider.setFiltering(true);
        varProposalProvider.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
        return varProposalProvider;
    }


    private void copyDeclarationsToInit() {
        
        setCurrentTS(SectionIds.E_SECTION_LOCALS);
        
        VariablesGlobalsProvider typesGlobalsProvider = 
                GlobalsConfiguration.instance().getGlobalContainer().getVarsGlobalsProvider(getCoreId());
        
        CMapAdapter declMap = new CMapAdapter(m_currentTestSpec, 
                                              SectionIds.E_SECTION_LOCALS.swigValue(),
                                              true);

        List<String> localVars = new ArrayList<>();
        StrVector declVars = new StrVector();
        declMap.getKeys(declVars);
        
        int numVars = (int) declVars.size();
        // add only pointers and simple types
        
        for (int idx = 0; idx < numVars; idx++) {
            String varName = declVars.get(idx);
            String varType = declMap.getValue(varName);
            
            // handle all pointers as single value types
            // getExpressionTypeInfo() below fails for such types
            if (varType.endsWith("*")) {   
                localVars.add(varName);
                continue;
            }

            varType = DataUtils.createDummyVarFromType(varType);

            try {
                JCompoundType compoundInfo = typesGlobalsProvider.getExpressionTypeInfo(varType);
                if (compoundInfo == null) {
                    continue; // symbols are not loaded
                }
                JVariable exprInfo = compoundInfo.getExpressionInfo();
                EType typeInfo = exprInfo.getType();
                if (typeInfo == EType.tPointer  ||  typeInfo == EType.tSimple) {  // 'struct
                    localVars.add(varName);
                }
            } catch (Exception ex) {
                // ignore - variable with unknown types are not added to init table
                System.out.println("Unknown type: " + varType + "\n" + SEFormatter.getInfoWithStackTrace(ex, 2));
            }
                
        }

        addInitVars(localVars);
    }

    
    private void addInitItemsWithComplexVarsDlg() {

        setCurrentTS(SectionIds.E_SECTION_LOCALS);
        StructMembersSelectionDialog dlg = new StructMembersSelectionDialog(Activator.getShell(),
                                                                            getCoreId(),
                                                                            m_currentTestSpec);
        if (dlg.show()) {
            List<String> structVars = dlg.getInitVars();
            addInitVars(structVars);
        }
    }

    
    private void addInitVars(List<String> newVars) {
        
        if (newVars.isEmpty()) {
            return;
        }
        
        setCurrentTS(SectionIds.E_SECTION_INIT);
        CMapAdapter initMap = new CMapAdapter(m_currentTestSpec, 
                                              SectionIds.E_SECTION_INIT.swigValue(),
                                              true);
        
        GroupAction grpAction = new GroupAction("Add vars to init table");
        
        StrVector initVars = new StrVector();
        initMap.getKeys(initVars);
        // add new items after existing items in init section
        List<String> predecessors = null;

        for (String newVar : newVars) {
            // System.out.println(newVar);
            if (!initMap.contains(newVar)) {
                YamlScalar initVar = YamlScalar.newUserMapping(SectionIds.E_SECTION_INIT.swigValue(),
                                                               newVar);
                initVar.setValue(NewTCWizard.DEFAULT_VAR_VALUE);
                AbstractAction action = new InsertToUserMappingAction(m_currentTestSpec, 
                                                                      initVar, 
                                                                      predecessors);
                grpAction.add(action);
            }
        }
        
        grpAction.addDataChangedEvent(ENodeId.VARS_NODE, m_currentTestSpec);
        grpAction.addAllFireEventTypes();
        TestSpecificationModel.getActiveModel().execAction(grpAction);
    }

    
    @Override
    public void fillControlls() {

        if (m_testSpec == null) {
            setInputForInheritCb(null, m_isInheritDeclarationsTB);
            setInputForInheritCb(null, m_isInheritInitTB);
            m_declTable.setInput(null,  false);
            return;
        }

        setCurrentTS(SectionIds.E_SECTION_FUNC);
        // this one also disables m_varsWizardBtn button in case of system tests
        boolean isFuncNameDefined = !m_currentTestSpec.getFunctionUnderTest(true).getName().isEmpty();

        setCurrentTS(SectionIds.E_SECTION_LOCALS);
        m_varAndValueProposalProvider.setTestSpec(m_currentTestSpec);
        setInputForInheritCb(SectionIds.E_SECTION_LOCALS, m_isInheritDeclarationsTB);
        m_declTable.setInput(m_currentTestSpec, m_isInherited);
        boolean isDeclInherited = m_isInherited;
        
        
        setCurrentTS(SectionIds.E_SECTION_INIT);
        setInputForInheritCb(SectionIds.E_SECTION_INIT, m_isInheritInitTB);
        m_initTable.setInput(m_currentTestSpec, m_isInherited);
        
        m_varsWizardBtn.setEnabled(isFuncNameDefined  &&  (!m_isInherited  ||  !isDeclInherited));
        m_copyBtn.setEnabled(!m_isInherited); // disable it if init section is inherited (not writable)
        m_addComplexVarsBtn.setEnabled(!m_isInherited);
        
        setProposals();
    }
    
    
    private void setProposals() {

        String coreId = getCoreId();

        refreshGlobals(coreId);
    }


    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        switch (tableId) {
        case DECL_TABLE_ID:
            m_declTable.setSelection(lineNo);
            break;
        case INIT_TABLE_ID:
            m_initTable.setSelection(lineNo);
            break;
        default:
            // do not report error to the user, as it not critical internal error
            String msg = "Invalid table ID in Variables section: " + tableId;
            Activator.log(Status.ERROR, msg, new Throwable());
        }
    }

    
    public void refreshGlobals(String coreId) {
        
        if (m_declTable == null) {
            return; // does not exist yet because of lazy init.
        }
        
        GlobalsContainer globalsContainer = GlobalsConfiguration.instance().getGlobalContainer();

        GlobalsProvider provider = globalsContainer.getTypesGlobalsProvider(coreId);
        m_typeNameProposalProvider.setProposals(provider.getCachedGlobals(), 
                                                provider.getCachedDescriptions());

        ExpressionsPanel.setVarsContentProposals(m_currentTestSpec, 
                                                 coreId, 
                                                 m_varAndValueProposalProvider, 
                                                 false);
        m_varAndValueProposalProvider.setCoreId(coreId);
        m_varAndValueProposalProvider.setTestSpec(m_currentTestSpec);
    }    

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_LOCALS.swigValue(),
                         SectionIds.E_SECTION_INIT.swigValue()};
    }
}
