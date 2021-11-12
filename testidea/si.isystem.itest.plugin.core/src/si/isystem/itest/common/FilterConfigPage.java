package si.isystem.itest.common;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.ISysCommonConstants;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFilter.EFilterTypes;
import si.isystem.connect.CTestFilter.ETestFilterSectionIds;
import si.isystem.connect.CTestFilterController;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.ETristate;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.run.Script;
import si.isystem.itest.run.TestRunner;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.ActionExecutioner;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.AsystTextContentAdapter;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;


public class FilterConfigPage
{
    private TBControlRadio m_builtInScriptFilterRb;

    private TBControlText m_coreIdFilterTxt;
    private TBControlText m_partitionFilterTxt;
    private TBControlText m_moduleFilterTxt;
    
    private TBControlText m_includedFunctionsTxt;
    private TBControlText m_excludedFunctionsTxt;

    private TBControlText m_includedIdsTxt;
    private TBControlText m_excludedIdsTxt;
    
    private TBControlText m_mustHaveTagsTxt;
    private TBControlText m_mustHaveOneOfTagsTxt;
    private TBControlText m_mustNotHaveTagsTxt;
    private TBControlText m_mustNotHaveOneOfTagsTxt;

    private TBControlText m_scriptFunctionNameTxt;
    private TBControlText m_scriptFunctionParamsTxt;
    
    private StackLayout m_stackLayout;
    private Composite m_stackPanel;
    private Composite m_builtInParamsPanel;
    private Composite m_scriptParamsPanel;

    private Text m_resultList;

    private Script m_script;

    private IProgressMonitor m_progressMonitor = new SimpleProgressMonitor();

    private Job m_filterJob;
    
    private CTestSpecification m_containerTestSpec;

    private ContainerType m_containerType;

    public enum ContainerType {E_TREE, // walks test cases from the given root
                                       // to all derived ones. Used for normal
                                       // filtering of test cases to be executed.
                               E_REVERSE_LINEAR // walks test cases from the given one 
                                       // reverse, first its siblings, then parent's
                                       // siblings executed BEFORE it, and derived
                                       // tests executed before the current test case.
                                       // Used to filter test cases used for coverage 
                                       // merging, where only test cases executed so
                                       // far may be taken into account.
                               };

    // No longer used since group ID is no longer generate from core, partition 
    // and module items.
    private KeyListener m_keyListenerForCorePartitonModule;

    private TestSpecificationModel m_model;
    private IActionExecutioner m_actionExecutioner;

    private TBControlRadio m_operator1RB;
    private TBControlRadio m_operator2RB;
    private TBControlRadio m_operator3RB;

    private CTestFilter m_editedFilter;

    private String m_scriptFilterEnum;

    private AsystContentProposalProvider m_partitionProposals;

    private AsystContentProposalProvider m_coreIdsProposals;

    private AsystContentProposalProvider m_moduleProposals;

    private AsystContentProposalProvider m_tagsProposals;

    private AsystContentProposalProvider m_funcProposals;

    private AsystContentProposalProvider m_testIdsProposals;

    private String m_operatorOrEnum;

    private boolean m_isShowApplyButton;

    private Button m_applyBtn;

    private CTestFilter[] m_parentFilters;

    /**
     * 
     * @param containerType see enum ContainerType
     */
    public FilterConfigPage(ContainerType containerType, boolean isShowApplyButton) {
        
        m_containerType = containerType;
        m_isShowApplyButton = isShowApplyButton;
    }


    // call this from dialogs, to update globals before the dialog opens
    public void refreshGlobals() {

        /*
        m_coreIdsProvider.refreshGlobals();
        m_tagsProvider.refreshGlobals();
        m_testIDsProvider.refreshGlobals();

        final IIConnectOperation refreshOperation = new IIConnectOperation() {

            @Override
            public void exec(JConnection jCon) {
                // refresh all global items, so that user doesn't have to click Refresh 
                // several times. If it will be a performance problem in the future,
                // fine tune it.
                m_functionsProvider.refreshGlobals();
                m_partitionsProvider.refreshGlobals();
                m_moduleProvider.refreshGlobals();
            }
            
            @Override
            public void setData(Object data) {}
        };
        
        ISysUIUtils.execWinIDEAOperation(refreshOperation, Activator.getShell(),
                                         ConnectionProvider.instance().getDefaultConnection());
        */
        
        GlobalsContainer globalContainer = GlobalsConfiguration.instance().getGlobalContainer();

        setProposals(m_coreIdsProposals, globalContainer.getCoreIdsGlobalsProvider());
        setProposals(m_partitionProposals, globalContainer.getAllPartitionsProvider());
        setProposals(m_moduleProposals, globalContainer.getAllModulesProvider(true));
        setProposals(m_tagsProposals, globalContainer.getTagsGlobalsProvider());
        setProposals(m_testIdsProposals, globalContainer.getTestIdsGlobalsProvider());
        setProposals(m_funcProposals, globalContainer.getAllFunctionsProvider());
    }
    
    
    private void setProposals(AsystContentProposalProvider proposals, GlobalsProvider provider) {
        proposals.setProposals(provider.getCachedGlobals(), 
                               provider.getCachedDescriptions());    
    }
    
    
    public void createMainPanel(Composite parent) {

        Composite mainPanel = new Composite(parent, SWT.BORDER);
        mainPanel.setLayoutData("growx, growy, wmin 0"); 
        mainPanel.setLayout(new MigLayout("fill"));

        SashForm sash = new SashForm(mainPanel, SWT.HORIZONTAL);
        sash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        sash.setLayoutData("grow, w 0:300:, hmin 0");
        
        Composite filterConfigPanel = new Composite(sash, SWT.NONE);
        filterConfigPanel.setLayoutData("grow, wmin 0"); 
        filterConfigPanel.setLayout(new MigLayout("fill", "",
                                                  // radio btns, separator, sash, empty fill
                                                  "[min!][min!][min!][fill]"));
        
        KGUIBuilder mainBuilder = new KGUIBuilder(filterConfigPanel);
        final CTestFilter filt = new CTestFilter(null);
        
        m_scriptFilterEnum = filt.enum2Str(ETestFilterSectionIds.E_SECTION_FILTER_TYPE.swigValue(), 
                                         EFilterTypes.SCRIPT_FILTER.swigValue());
        
        m_operatorOrEnum = EBool.tristate2Str(ETristate.E_TRUE);
        
        String radiosLayoutData = "wrap";
        if (m_isShowApplyButton) {
            radiosLayoutData = "split 2";
        }
        m_builtInScriptFilterRb = // mainBuilder.checkBox("Use built-in filter", "wrap");
            new TBControlRadio(mainBuilder, 
                               new String[]{"Built-in filter", "Script filter", "Default (Built-in)"}, 
                               new String[]{"Built-in filter will be used for test case filtering.",
                                            "Script will be called for test case filtering.",
                                            "This setting is not saved, which means that default will be used for execution."},
                               new String[]{
                                  filt.enum2Str(ETestFilterSectionIds.E_SECTION_FILTER_TYPE.swigValue(), EFilterTypes.BUILT_IN_FILTER.swigValue()),
                                  filt.enum2Str(ETestFilterSectionIds.E_SECTION_FILTER_TYPE.swigValue(), EFilterTypes.SCRIPT_FILTER.swigValue()),
                                  ""},
                               radiosLayoutData, 
                               ETestFilterSectionIds.E_SECTION_FILTER_TYPE.swigValue(), 
                               ENodeId.GRP_FILTER,
                               null);

        m_builtInScriptFilterRb.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                if (isUseBuiltInfilter()) {
                    m_stackLayout.topControl = m_builtInParamsPanel;
                    m_stackPanel.layout();
                } else {
                    m_stackLayout.topControl = m_scriptParamsPanel;
                    m_stackPanel.layout();
                }
                refreshFilterOutput(null);
            }
        
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        if (m_isShowApplyButton) {
            m_applyBtn = mainBuilder.button("Apply", "gapleft:push, wrap");
            UiTools.setToolTip(m_applyBtn, "Press this button to update test cases in groups.");
        }
        
        mainBuilder.separator("growx, gaptop 10, gapbottom 15, wrap", SWT.HORIZONTAL);
        
        m_stackPanel = new Composite(filterConfigPanel, SWT.NONE);
        m_stackLayout = new StackLayout();
        m_stackPanel.setLayout(m_stackLayout);
        m_stackPanel.setLayoutData("growx");

        m_builtInParamsPanel = createBuiltInParamsPanel(m_stackPanel);
        createPanelForScriptFunction(m_stackPanel);
        
        addChangeListeners();
        
        m_stackLayout.topControl = m_builtInParamsPanel;
        m_stackPanel.layout();

        Composite resultListPanel = new Composite(sash, SWT.NONE);
        resultListPanel.setLayoutData("hmin 0");
        resultListPanel.setLayout(new MigLayout("fill", "[fill]", "[min!][fill]"));
        
        KGUIBuilder resultBuilder = new KGUIBuilder(resultListPanel);
        resultBuilder.label("Test cases which pass filter", "wrap");
        m_resultList = resultBuilder.text("wmin 30, hmin 0, hmax 90%, grow", 
                                          SWT.BORDER | SWT.MULTI |  
                                          SWT.V_SCROLL | SWT.H_SCROLL);
        m_resultList.setEditable(false);
        
        
        sash.setWeights(new int[]{7, 3});
    }

    
    private Composite createBuiltInParamsPanel(Composite parent) {
        
        Composite builtInParamsPanel = new Composite(parent, SWT.NONE);
        builtInParamsPanel.setLayoutData("wrap");
        builtInParamsPanel.setLayout(new MigLayout("fill", "[min!][fill]"));
        
        KGUIBuilder builder = new KGUIBuilder(builtInParamsPanel);

        Label infoLbl = builder.label("", "wrap");
        infoLbl.setImage(IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_16x16));
        UiTools.setToolTip(infoLbl, 
                           "Filtering is performed from top to bottom with the following algorithm:\n\n"
                           + "    if (testCaseCoreId matches filterCoreId):\n"
                           + "        if (functionPartition matches filterPartition):\n"
                           + "            if (functionModule matches filterModule):\n"
                           + "                if (isFunctionIncluded(functionName)):\n"
                           // + "\n"
                           + "                    if (isFunctionExcluded(functionName)):\n"
                           + "                        return false\n"
                           // + "\n"
                           + "                    if (isIdIncluded(testId)):\n"
                           // + "\n"
                           + "                        if (isIdExcluded(testId)):\n"
                           + "                            return false\n"
                           // + "\n"
                           + "                        return filterTags(tags)\n"
                           // + "\n"
                           + "    return false\n\n"
                           + "Empty core ID, partition, modules, and tags filter fields always evaluate to true.");
        
        builder.label("Core ID:");
        m_coreIdFilterTxt = TBControlText.createForMixed(builder, 
                                                         "ID of core from which functions should be used.\n"
                                                         + "Empty field means default core.\n" 
                                                         + "Example:\n"
                                                         + "    core0\n\n", 
                                                         "wrap", 
                                                         CTestFilter.ETestFilterSectionIds.E_SECTION_CORE_ID.swigValue(),
                                                         ENodeId.GRP_FILTER, 
                                                         null, 
                                                         SWT.BORDER);
        
        m_coreIdsProposals = ISysUIUtils.addContentProposalsAdapter(m_coreIdFilterTxt.getControl(),
                                                                    ContentProposalAdapter.PROPOSAL_INSERT,
                                                                    UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()); 
        m_coreIdFilterTxt.setTestTreeRefreshNeeded(true);
        
        builder.label("Partitions:");
        m_partitionFilterTxt = TBControlText.createForList(builder, 
                                                            "List of partitions (download files) from which functions should be used.\n"
                                                            + "Empty field means no filtering, all partitions are included.\n"
                                                            + "Example:\n"
                                                            + "    boot.elf, app.elf\n\n"
                                                            + ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX, 
                                                            "wrap", 
                                                            CTestFilter.ETestFilterSectionIds.E_SECTION_PARTITIONS.swigValue(),
                                                            ENodeId.GRP_FILTER, 
                                                            null, 
                                                            SWT.BORDER);

        m_partitionProposals = ISysUIUtils.addContentProposalsAdapter(m_partitionFilterTxt.getControl(),
                                                                      ContentProposalAdapter.PROPOSAL_INSERT,
                                                                      UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()); 
        m_partitionFilterTxt.setTestTreeRefreshNeeded(true);

        builder.label("Modules:");
        m_moduleFilterTxt = TBControlText.createForList(builder, 
                                                         "List of modules as regex (source files) from which functions should be used.\n"
                                                         + "Empty field means no filtering, all modules from filtered partitions are included.\n"
                                                         + "Example:\n"
                                                         + "    main.c, common/utils.c\n\n"
                                                         + ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX, 
                                                         "wrap", 
                                                         CTestFilter.ETestFilterSectionIds.E_SECTION_MODULES.swigValue(),
                                                         ENodeId.GRP_FILTER, 
                                                         null, 
                                                         SWT.BORDER);

        m_moduleProposals = ISysUIUtils.addContentProposalsAdapter(m_moduleFilterTxt.getControl(),
                                                                   ContentProposalAdapter.PROPOSAL_INSERT,
                                                                   UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()); 
        m_moduleFilterTxt.setTestTreeRefreshNeeded(true);
        
        KGUIBuilder exceptionsBuilder = builder.group("Function and Test ID filter", "growx, gapright 5, gaptop 20, gapbottom 15, span 2, wrap",
                                                      true, "fill", "[min!][fill]", "");
        UiTools.setToolTip(exceptionsBuilder.getParent(), "Fields in this group are used for filtering before tags specified below.\n"
                + "If a field is empty, it is not used for filtering.");

        String textLayoutData = "wrap";

        exceptionsBuilder.label("Functions:");
        m_includedFunctionsTxt = TBControlText.createForList(exceptionsBuilder, 
                                                             "Specify list of functions as regular expressions, for example:\n"
                                                             + "  setGlobals, init.*\n"
                                                             + "Empty field means no filtering, all test cases are included.\n\n"
                                                             + ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX, 
                                                             textLayoutData, 
                                                             CTestFilter.ETestFilterSectionIds.E_SECTION_INCLUDED_FUNCTIONS.swigValue(), 
                                                             ENodeId.GRP_FILTER, 
                                                             null, 
                                                             SWT.BORDER);
        
        // exceptionsBuilder.text("growx, wrap", SWT.BORDER);
        createAutoCompleteForFunctions(m_includedFunctionsTxt.getControl());

        exceptionsBuilder.label("  Excluded functions:");
        m_excludedFunctionsTxt = TBControlText.createForList(exceptionsBuilder, 
                                                             "Specify list of functions as regular expressions, for example:\n"
                                                             + "  setGlobals, init.*\n"
                                                             + "Empty field means no test cases are excluded.\n\n"
                                                             + ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX,
                                                             textLayoutData, 
                                                             CTestFilter.ETestFilterSectionIds.E_SECTION_EXCLUDED_FUNCTIONS.swigValue(), 
                                                             ENodeId.GRP_FILTER, 
                                                             null, 
                                                             SWT.BORDER);
                // exceptionsBuilder.text("growx, wrap", SWT.BORDER);
        addAutoComplete(m_excludedFunctionsTxt.getControl(), m_funcProposals);
        
        exceptionsBuilder.label("  Test IDs:");
        m_includedIdsTxt = TBControlText.createForList(exceptionsBuilder, 
                                                       "Specify list of test IDs as regular expressions, for example:\n"
                                                       + "  test-01, test-2[5-9]\n"
                                                       + "Empty field means no test ID filtering is performed.\n\n"
                                                       + ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX,
                                                       textLayoutData, 
                                                       CTestFilter.ETestFilterSectionIds.E_SECTION_INCLUDED_IDS.swigValue(), 
                                                       ENodeId.GRP_FILTER, 
                                                       null, 
                                                       SWT.BORDER);
        // exceptionsBuilder.text("growx, wrap", SWT.BORDER);
        createAutoCompleteForTestIds(m_includedIdsTxt.getControl());

        exceptionsBuilder.label("    Excluded test IDs:");
        m_excludedIdsTxt = TBControlText.createForList(exceptionsBuilder, 
                                                       "Specify list of test IDs as regular expressions, for example:\n"
                                                       + "  test-01, test-2[5-9]\n"
                                                       + "Empty field means no test ID filtering is performed.\n\n"
                                                       + ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX,
                                                       textLayoutData, 
                                                       CTestFilter.ETestFilterSectionIds.E_SECTION_EXCLUDED_IDS.swigValue(), 
                                                       ENodeId.GRP_FILTER, 
                                                       null, 
                                                       SWT.BORDER);
        addAutoComplete(m_excludedIdsTxt.getControl(), m_testIdsProposals);
        
        // 'gapright 5' is required to see the right border of the group
        KGUIBuilder tagsBuilder = builder.group("Tags filter", "growx, gapright 5, span 2, wrap",
                                                true, "fill", "[min!][fill]", "");
        UiTools.setToolTip(tagsBuilder.getParent(), "If there is no match in 'Function and Test ID "
                                                    + "filter' above, tags criteria must be "
                                                    + "satisfied for test case to be used.");
        
        
        tagsBuilder.label("Must have all tags:");
        m_mustHaveTagsTxt = TBControlText.createForList(tagsBuilder, 
                                                        "All of tags specified here MUST be present in test case to be used.",
                                                        textLayoutData, 
                                                        CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_HAVE_ALL_TAGS.swigValue(), 
                                                        ENodeId.GRP_FILTER, 
                                                        null, 
                                                        SWT.BORDER); 
        createAutoCompleteForTags(m_mustHaveTagsTxt.getControl());

        m_operator1RB = createAndOrRadios(tagsBuilder, ETestFilterSectionIds.E_SECTION_IS_OR_TAGS_1);

        tagsBuilder.label("Must have at least one of tags:");
        m_mustHaveOneOfTagsTxt = TBControlText.createForList(tagsBuilder, 
                                                             "At least one of tags specified here MUST be present in test case to be used.",
                                                             textLayoutData, 
                                                             CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_HAVE_ONE_OF_TAGS.swigValue(), 
                                                             ENodeId.GRP_FILTER, 
                                                             null, 
                                                             SWT.BORDER);
        addAutoComplete(m_mustHaveOneOfTagsTxt.getControl(), m_tagsProposals);

        m_operator2RB = createAndOrRadios(tagsBuilder, ETestFilterSectionIds.E_SECTION_IS_OR_TAGS_2);
        
        tagsBuilder.label("Must NOT have any of tags:");
        m_mustNotHaveTagsTxt = TBControlText.createForList(tagsBuilder, 
                                                           "Any of tags specified here must NOT be present in test case to be used.",
                                                           textLayoutData, 
                                                           CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_NOT_HAVE_ALL_TAGS.swigValue(), 
                                                           ENodeId.GRP_FILTER, 
                                                           null, 
                                                           SWT.BORDER); 

        addAutoComplete(m_mustNotHaveTagsTxt.getControl(), m_tagsProposals);

        m_operator3RB = createAndOrRadios(tagsBuilder, ETestFilterSectionIds.E_SECTION_IS_OR_TAGS_3); 
        
        tagsBuilder.label("Must NOT have at least one of tags:");
        m_mustNotHaveOneOfTagsTxt = TBControlText.createForList(tagsBuilder, 
                                                                "At least one of tags specified here must NOT be present in test case to be used.",
                                                                textLayoutData, 
                                                                CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_NOT_HAVE_ONE_OF_TAGS.swigValue(), 
                                                                ENodeId.GRP_FILTER, 
                                                                null, 
                                                                SWT.BORDER);
        addAutoComplete(m_mustNotHaveOneOfTagsTxt.getControl(), m_tagsProposals);

        return builtInParamsPanel;
    }

    
    /** Call this method only if true was specified for the apply btn in ctor. */ 
    public void addApplyBtnListener(SelectionListener listener) {
        m_applyBtn.addSelectionListener(listener);
    }
    
    
    public void addKeyListenerForCorePartitionModule(KeyListener listener) {
        m_keyListenerForCorePartitonModule = listener;
    }

    
    private void addChangeListeners() {
        
        // built-in filter
        KeyListener modifyListener = new KeyListener() {
            
            @Override
            public void keyPressed(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {
                refreshFilterOutput(e);
            }
        };
            
        m_coreIdFilterTxt.addKeyListener(modifyListener);
        m_partitionFilterTxt.addKeyListener(modifyListener);
        m_moduleFilterTxt.addKeyListener(modifyListener);
        
        m_includedIdsTxt.addKeyListener(modifyListener);
        m_excludedIdsTxt.addKeyListener(modifyListener);
        
        m_includedFunctionsTxt.addKeyListener(modifyListener);
        m_excludedFunctionsTxt.addKeyListener(modifyListener);

        m_mustHaveTagsTxt.addKeyListener(modifyListener);
        m_mustHaveOneOfTagsTxt.addKeyListener(modifyListener);
        m_mustNotHaveTagsTxt.addKeyListener(modifyListener);
        m_mustNotHaveOneOfTagsTxt.addKeyListener(modifyListener);

        // script filter
        /* FocusListener focusListener = new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                refreshFilterOutput();
            }
            
            @Override public void focusGained(FocusEvent e) {}
        };
        m_scriptFunctionNameTxt.addFocusListener(focusListener);
        m_scriptFunctionParamsTxt.addFocusListener(focusListener); */
        m_scriptFunctionNameTxt.addKeyListener(modifyListener);
        m_scriptFunctionParamsTxt.addKeyListener(modifyListener);

        SelectionAdapter selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshFilterOutput(null);
            }
        };

        m_operator1RB.addSelectionListener(selectionListener);
        m_operator2RB.addSelectionListener(selectionListener);
        m_operator3RB.addSelectionListener(selectionListener);
        
    }


    private void createAutoCompleteForFunctions(Control control) {
        m_funcProposals = ISysUIUtils.addContentProposalsAdapter(control,
                                                                 ContentProposalAdapter.PROPOSAL_INSERT,
                                                                 UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()); 
    }


    private void createAutoCompleteForTestIds(Control control) {
        m_testIdsProposals = ISysUIUtils.addContentProposalsAdapter(control,
                                                                    ContentProposalAdapter.PROPOSAL_INSERT,
                                                                    UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()); 
    }
    

    private void createAutoCompleteForTags(Control control) {
        m_tagsProposals = ISysUIUtils.addContentProposalsAdapter(control,
                                                                 ContentProposalAdapter.PROPOSAL_INSERT,
                                                                 UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()); 
    }

    
    private void addAutoComplete(Control control, IContentProposalProvider proposals) {
        ISysUIUtils.addContentProposalsAdapter(control,
                                               proposals,
                                               new AsystTextContentAdapter(),
                                               ContentProposalAdapter.PROPOSAL_INSERT,
                                               UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
    }
    
    
    private TBControlRadio createAndOrRadios(KGUIBuilder tagsBuilder, ETestFilterSectionIds filterSectionId) {
        
        return 
           new TBControlRadio(tagsBuilder, 
                              new String[]{"And", "Or", "Default (And)"}, 
                              new String[]{"", "",
                                           "This setting is not saved, which means that default will be used for execution."},
                              "wrap", 
                              filterSectionId.swigValue(), 
                              ENodeId.GRP_FILTER,
                              null);

//        Composite radioComposite = new Composite(tagsBuilder.getParent(), SWT.NONE);
//        radioComposite.setLayoutData("span 2, wrap");
//        radioComposite.setLayout(new MigLayout());
//        KGUIBuilder radio1Group = new KGUIBuilder(radioComposite);
//        
//        return radio1Group;
    }

    
    private void createPanelForScriptFunction(Composite parent) {
        m_scriptParamsPanel = new Composite(parent, SWT.NONE);
        m_scriptParamsPanel.setLayoutData("growx, wrap");
        m_scriptParamsPanel.setLayout(new MigLayout("fillx", "[min!][fill]"));
        
        KGUIBuilder builder = new KGUIBuilder(m_scriptParamsPanel);


        builder.label("Script function:");
        m_scriptFunctionNameTxt = 
            TBControlText.createForMixed(builder, 
                                         "Name of the script function to use for test filtering.\n"
                                         + "The function should return 'true' if test case should be used.", 
                                         "wrap", 
                                         ETestFilterSectionIds.E_SECTION_SCRIPT_FUNCTION.swigValue(), 
                                         ENodeId.GRP_FILTER, 
                                         null, 
                                         SWT.BORDER);

        builder.label("Script function parameters:");
        m_scriptFunctionParamsTxt = 
            TBControlText.createForList(builder, 
                                        "Comma separated list of parameters for the script function.",
                                        "wrap", 
                                        ETestFilterSectionIds.E_SECTION_SCRIPT_FUNCTION_PARAMS.swigValue(), 
                                        ENodeId.GRP_FILTER, 
                                        null, 
                                        SWT.BORDER);

        Button reloadBtn = builder.button("Reload script");
        reloadBtn.setImage(IconProvider.INSTANCE.getIcon(IconProvider.EIconId.ERefresh));
        UiTools.setToolTip(reloadBtn, "Reloads Python script. Press this button if you have modified filter script function.");
        reloadBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    if (m_script != null) {
                        m_script.close();
                        m_script = null;
                    } 
                    refreshFilterOutput(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    m_script = null;
                    m_resultList.setText("Can't close existing script!\n\n" +
                            SEFormatter.getInfoWithStackTrace(ex, 3));
                }
            }
        });
    }
    
    
    /**
     * 
     * @param containerTestSpec test case, which contains all test cases to
     *                          be filtered. To filter all test cases from model,
     *                          pass root test case.
     * @param editedFilter filter configuration 
     * @param actionExecutioner if null, internal action executioner is created. 
     * This should be specified when this page is used in a dialog, where changes
     * must not propagate to the main application undo/redo queue. Otherwise
     * specify the application action executioner.        
     */
    public void setInput(CTestSpecification containerTestSpec, 
                         CTestFilter editedFilter,
                         IActionExecutioner actionExecutioner,
                         CTestFilter ... parentFilters) {

        m_containerTestSpec = containerTestSpec;
        
        if (actionExecutioner == null) {
            m_actionExecutioner = new ActionExecutioner();
        } else {
            m_actionExecutioner = actionExecutioner;
        }
        
        m_editedFilter  = editedFilter;
        m_parentFilters = parentFilters;
    }
    
    
    public void fillControls() {
        
        if (m_editedFilter == null) {
            
            m_coreIdFilterTxt.setInput(null, false);
            m_partitionFilterTxt.setInput(null, false);
            m_moduleFilterTxt.setInput(null, false);
            
            m_mustHaveTagsTxt.setInput(null, false);
            m_mustHaveOneOfTagsTxt.setInput(null, false);
            m_mustNotHaveTagsTxt.setInput(null, false);
            m_mustNotHaveOneOfTagsTxt.setInput(null, false);
            
            m_operator1RB.setInput(null, false);
            m_operator2RB.setInput(null, false);
            m_operator3RB.setInput(null, false);
            
            m_includedIdsTxt.setInput(null, false);
            m_excludedIdsTxt.setInput(null, false);
            m_includedFunctionsTxt.setInput(null, false);
            m_excludedFunctionsTxt.setInput(null, false);
            m_scriptFunctionNameTxt.setInput(null, false);
            m_scriptFunctionParamsTxt.setInput(null, false);
            m_builtInScriptFilterRb.setInput(null, false);

            setEnabledFilterControls(false);
            return;
        }

        // create a copy, so that original filter is not changed if user cancels the dialog.

        setEnabledFilterControls(true);

        m_coreIdFilterTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_partitionFilterTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_moduleFilterTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        
        m_includedFunctionsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_excludedFunctionsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_includedIdsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_excludedIdsTxt.setInput(m_editedFilter, false, m_actionExecutioner);

        m_mustHaveTagsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_mustHaveOneOfTagsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_mustNotHaveTagsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_mustNotHaveOneOfTagsTxt.setInput(m_editedFilter, false, m_actionExecutioner);

        m_operator1RB.setInput(m_editedFilter, false, m_actionExecutioner);
        m_operator2RB.setInput(m_editedFilter, false, m_actionExecutioner);
        m_operator3RB.setInput(m_editedFilter, false, m_actionExecutioner);
        
        m_scriptFunctionNameTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        m_scriptFunctionParamsTxt.setInput(m_editedFilter, false, m_actionExecutioner);
        
        m_builtInScriptFilterRb.setInput(m_editedFilter, false, m_actionExecutioner);
        
        // clear stale data - if user modified test ID or function name, ...
        // Performance is lost only when user clicks section or opens a dialog.
        // It is still OK for refreshing filter view.
        TestSpecificationModel.getActiveModel().getRootTestSpecification().clearMergedFilterInfo(true);

        EFilterTypes filterType = m_editedFilter.getFilterType();
        if (filterType == EFilterTypes.BUILT_IN_FILTER) {
            m_stackLayout.topControl = m_builtInParamsPanel;
            m_stackPanel.layout();
        } else {
            m_stackLayout.topControl = m_scriptParamsPanel;
            m_stackPanel.layout();
        }
        
        updateFilterOutput(m_editedFilter);
    }


    public boolean isUseBuiltInfilter() {
        return !m_builtInScriptFilterRb.getSelection().equals(m_scriptFilterEnum);
    }
    
    
    private void setEnabledFilterControls(boolean isEnabled) {
        
        m_coreIdFilterTxt.setEnabled(isEnabled);
        m_partitionFilterTxt.setEnabled(isEnabled);
        m_moduleFilterTxt.setEnabled(isEnabled);
        
        m_mustHaveTagsTxt.setEnabled(isEnabled);
        m_mustHaveOneOfTagsTxt.setEnabled(isEnabled);
        m_mustNotHaveTagsTxt.setEnabled(isEnabled);
        m_mustNotHaveOneOfTagsTxt.setEnabled(isEnabled);
        
        m_operator1RB.setEnabled(isEnabled);
        m_operator2RB.setEnabled(isEnabled);
        m_operator3RB.setEnabled(isEnabled);
        
        m_includedIdsTxt.setEnabled(isEnabled);
        m_excludedIdsTxt.setEnabled(isEnabled);
        m_includedFunctionsTxt.setEnabled(isEnabled);
        m_excludedFunctionsTxt.setEnabled(isEnabled);
        m_scriptFunctionNameTxt.setEnabled(isEnabled);
        m_scriptFunctionParamsTxt.setEnabled(isEnabled);
        m_builtInScriptFilterRb.setEnabled(isEnabled);
    }


//    public void getData(CTestFilter testFilter) {
//        
//        if (testFilter == null) {
//            throw new SIllegalStateException("Internal error: testFilter == null!");
//        }
//        copyGUIDataToFilter(m_dlgFilter); // make sure all data is retrieved from UI
//        testFilter.assign(m_dlgFilter);
//    }
//    
    
    /**
     * This method copies data directly to local CTestFilter class, so that it 
     * can be used for the on-the-fly filtering on each key-press.
     * 
     * @param testFilter
     */
    private void copyGUIDataToFilter(CTestFilter testFilter) {
        
        if (testFilter == null) {
            return;
        }
        
        if (isUseBuiltInfilter()) {
            testFilter.setFilterType(CTestFilter.EFilterTypes.BUILT_IN_FILTER);
        } else {
            testFilter.setFilterType(CTestFilter.EFilterTypes.SCRIPT_FILTER);
        }

        testFilter.setScriptFunction(m_scriptFunctionNameTxt.getText());
        setList(testFilter, ETestFilterSectionIds.E_SECTION_SCRIPT_FUNCTION_PARAMS, m_scriptFunctionParamsTxt);

        testFilter.setTagValue(CTestFilter.ETestFilterSectionIds.E_SECTION_CORE_ID.swigValue(), 
                               m_coreIdFilterTxt.getText());
        
        setList(testFilter, ETestFilterSectionIds.E_SECTION_PARTITIONS, m_partitionFilterTxt);
        setList(testFilter, ETestFilterSectionIds.E_SECTION_MODULES, m_moduleFilterTxt);
        
        setList(testFilter, ETestFilterSectionIds.E_SECTION_INCLUDED_FUNCTIONS, m_includedFunctionsTxt);
        setList(testFilter, ETestFilterSectionIds.E_SECTION_EXCLUDED_FUNCTIONS, m_excludedFunctionsTxt);
        
        setList(testFilter, ETestFilterSectionIds.E_SECTION_INCLUDED_IDS, m_includedIdsTxt);
        setList(testFilter, ETestFilterSectionIds.E_SECTION_EXCLUDED_IDS, m_excludedIdsTxt);
        
        setList(testFilter, ETestFilterSectionIds.E_SECTION_MUST_HAVE_ALL_TAGS, m_mustHaveTagsTxt);
        setList(testFilter, ETestFilterSectionIds.E_SECTION_MUST_HAVE_ONE_OF_TAGS, m_mustHaveOneOfTagsTxt);
        setList(testFilter, ETestFilterSectionIds.E_SECTION_MUST_NOT_HAVE_ALL_TAGS, m_mustNotHaveTagsTxt);
        setList(testFilter, ETestFilterSectionIds.E_SECTION_MUST_NOT_HAVE_ONE_OF_TAGS, m_mustNotHaveOneOfTagsTxt);
        
        testFilter.setOrTags1(m_operator1RB.getSelection().equals(m_operatorOrEnum));
        testFilter.setOrTags2(m_operator2RB.getSelection().equals(m_operatorOrEnum));
        testFilter.setOrTags3(m_operator3RB.getSelection().equals(m_operatorOrEnum));
    }


    private void setList(CTestBase testBase,
                         ETestFilterSectionIds eSectionScriptFunctionParams,
                         TBControlText listTxt) {
        
        YamlScalar scalar = YamlScalar.newList(eSectionScriptFunctionParams.swigValue());
        scalar.setValue(listTxt.getText());
        scalar.dataToTestSpec(testBase);
    }


    private void refreshFilterOutput(KeyEvent e) {
        try {
            CTestFilter filter = new CTestFilter(null);
            copyGUIDataToFilter(filter);
            updateFilterOutput(filter);
            
            if (m_keyListenerForCorePartitonModule != null) {
                m_keyListenerForCorePartitonModule.keyPressed(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            m_resultList.setText("Invalid data in dialog!\n\n" +
                    SEFormatter.getInfoWithStackTrace(ex, 3));
        }
    }
    
    
    private void updateFilterOutput(final CTestFilter editedFilter) {
        m_model = TestSpecificationModel.getActiveModel();
        
        if (m_model == null) {
            m_resultList.setText("No testIDEA file is selected!");
            return;
        }
        
        final CTestFilter [] filters = ArrayUtils.add(m_parentFilters, editedFilter);

        for (CTestFilter filter : filters) {
            if (filter.getFilterType() == EFilterTypes.SCRIPT_FILTER) {
                m_script = UiUtils.initScript(m_model);
                break;
            }
        }
        
        // cancel previous job if exists
        if (m_filterJob != null) {
            m_progressMonitor.setCanceled(true);
            while (m_filterJob.getState() != Job.NONE);
        }
        
        // create a new job with progress monitor
        m_filterJob = new Job("Test Case Filter Job") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                
                try {
                    
                    final StringBuilder sb = new StringBuilder();

                    // long start = System.currentTimeMillis();
                    switch (m_containerType) {
                    case E_REVERSE_LINEAR:
                        filterReverseLinear(m_containerTestSpec, sb, monitor, 
                                            m_model.getFilterController(),
                                            filters);
                        break;
                    case E_TREE:
                        filterRecursive(m_containerTestSpec, sb, monitor,
                                        m_model.getFilterController(),
                                        filters);
                        break;
                    default:
                        throw new SIllegalStateException("Invalid enum value! Contact iSYSTEM support!")
                            .add("value", m_containerType);
                    }
                    // System.out.println("filter time = " + (System.currentTimeMillis() - start));
                
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            m_resultList.setText(sb.toString());
                        }
                    }); 
                
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            m_resultList.setText("Error in test case filter!\n\n" +
                                                 SEFormatter.getInfoWithStackTrace(ex, 3));
                        }
                    });
                }
                
                return Status.OK_STATUS;
            }
        };

        IJobManager manager = Job.getJobManager();
        
        ProgressProvider provider = new ProgressProvider() {
            @Override
            public IProgressMonitor createMonitor(Job job) {
                return m_progressMonitor;
            }
        };
          
        manager.setProgressProvider(provider);
        m_progressMonitor.setCanceled(false);
        
        m_filterJob.schedule();
        
        // uncomment the next 2 lines and comment the above line (m_filterJob.schedule())
        // to process filtering in UI thread.
        // getFilterOuputRecursive(rootTestSpec, filter, sb, m_progressMonitor);
        // m_resultList.setText(sb.toString());
    }


    /*
     * Walks tree of test specs from root down - in the order of execution.
     * Used to filter test cases from model to get the ones to be executed.
     */
    private void filterRecursive(CTestSpecification testSpec,
                                StringBuilder sb, 
                                IProgressMonitor monitor,
                                CTestFilterController filterCtrl,
                                CTestFilter ... filters) {

        if (monitor.isCanceled()) {
            return;
        }
        
        filterTestSpec(testSpec, sb, filterCtrl, filters);
        
        int numSpecs = testSpec.getNoOfDerivedSpecs();
        for (int idx = 0; idx < numSpecs; idx++) {
            filterRecursive(testSpec.getDerivedTestSpec(idx), sb, monitor, filterCtrl, filters);
        }
    }


    /*
     * Walks first level derived test specs in reverse order. Used to filter test 
     * specs used for coverage, as not all derived test specs should be used.
     * The caller should create container testSpec which contains all potential
     * candidates for filtering on the first level. 
     */
    private void filterReverseLinear(CTestSpecification testSpec,
                                     StringBuilder sb, 
                                     IProgressMonitor monitor,
                                     CTestFilterController filterCtrl,
                                     CTestFilter ... filters) {

        int numSpecs = testSpec.getNoOfDerivedSpecs();
        for (int idx = numSpecs - 1; idx >= 0; idx--) {
//        for (int idx = 0; idx < numSpecs; idx++) {
            if (monitor.isCanceled()) {
                return;
            }

            filterTestSpec(testSpec.getDerivedTestSpec(idx), sb, filterCtrl, filters);
         }
     }


    private void filterTestSpec(CTestSpecification testSpec,
                                StringBuilder sb,
                                CTestFilterController filterCtrl,
                                CTestFilter ... filters) {
        
        StringBuilder debugInfo = new StringBuilder();
        boolean isPassed = true;
        for (CTestFilter filter : filters) {
            isPassed &= TestRunner.isTestExecutable(testSpec, 
                                                    filter, 
                                                    m_script,
                                                    false,
                                                    filterCtrl,
                                                    debugInfo);
            if (!isPassed) {
                break;
            }
        }
        
//        if (filter.getFilterType() == EFilterTypes.BUILT_IN_FILTER) {
//            isPassed = filter.mergeAndfilterTestSpec(testSpec);
//        } else {
//            TestScriptResult filterResult = TestRunner.filterWithScript(testSpec.merge(), 
//                                                                        filter, 
//                                                                        m_script);
//            List<String> funcRetVal = filterResult.getFuncRetVal();
//            isPassed = funcRetVal != null  &&  !funcRetVal.isEmpty();
//            debugInfo = StringUtils.join(filterResult.getStdout(), "\n  ");
//            debugInfo += filterResult.getMetaData();
//        }
        
        if (isPassed) {
            sb.append(testSpec.getUILabel()).
               append('\n');
            
            if (debugInfo.length() > 0) {
                sb.append("  ")
                  .append(debugInfo) 
                  .append('\n');
            }
        }
    }
}
