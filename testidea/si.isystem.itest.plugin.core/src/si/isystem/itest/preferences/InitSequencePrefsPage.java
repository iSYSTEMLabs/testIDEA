package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.part.WorkbenchPart;

import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ActionQueue;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.IEventDispatcher;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.itest.preferences.controls.PrefsTBTableEditor;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.KGUIBuilder;

public class InitSequencePrefsPage extends FieldEditorPreferencePage
                              implements IWorkbenchPreferencePage, IActionExecutioner,
                                         IEventDispatcher {

    private Composite m_fieldEditorPanel;
    private Map<EEnvConfigSections, String> m_sectionId2String;
    
    private ActionQueue m_actionQueue = new ActionQueue(this);
    private WorkbenchPart m_viewPart;
    private PrefBooleanEditor m_isRunInitSequence;
    private PrefsTBTableEditor m_initSeqTable;
    private PrefBooleanEditor m_isCheckTargetBeforeRun;
    private PrefBooleanEditor m_isVerifySymbolsBeforeRun; 
    private PrefBooleanEditor m_isDisableInterrupts;
    private CTestEnvironmentConfig m_testBase;
    
    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public InitSequencePrefsPage(IPreferenceStore prefStore, CTestEnvironmentConfig testBase) {
        super(GRID);

        m_viewPart = StatusView.getView();
        
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();
        
        setPreferenceStore(prefStore);
        m_testBase = testBase;
        
        setDescription("Settings on this page define target initialization steps. " +
                       "They are used for test execution, and are saved to project file.\n" +
                       "This page can be accessed with commands 'File | Properties' or 'Test | Init sequence'.");
    }
    

    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public InitSequencePrefsPage() {
        super(GRID);
        
        m_viewPart = StatusView.getView();
        
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForPreferences();
        
        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(preferenceStore);
        m_testBase = new CTestEnvironmentConfig(null);
        
        setDescription("Specify target initialization steps, which will be " +
                       "used as defaults when new projects are created.\n" +
                       "Select 'File | Properties' to edit configuration of the active project!");
    }

    
    /**
     * This method should be called when this class is used as preferences page 
     * - settings are stored into Eclipse PreferenceStore and are used as defaults
     * for all testIDEA files. This way the same string IDs are used for 
     * preferences even if section IDs in the code change. 
     * @return 
     * 
     * @see mappingForProperties
     */
    public static Map<EEnvConfigSections, String> mappingForPreferences() {
        
        Map<EEnvConfigSections, String> sectionId2String = new TreeMap<>();
        
        sectionId2String.put(EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ, "run.alwaysRunInitSeq");
        sectionId2String.put(EEnvConfigSections.E_SECTION_INIT_SEQUENCE, "run.initSeq");
        sectionId2String.put(EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN, "run.checkTargetState");
        sectionId2String.put(EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN, "run.verifySymbolsBeforeRun");
        sectionId2String.put(EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS, "run.disableInterrupts");
        
        return sectionId2String;
    }


    /**
     * This method should be called, when this class is used as properties page 
     * of the project or iyaml file resource. Section ids are used in this case, 
     * which modify setting in the currently opened project - they are saved
     * into iyaml file. 
     * @return 
     */
    public static Map<EEnvConfigSections, String> mappingForProperties() {
        
        Map<EEnvConfigSections, String> sectionId2String = new TreeMap<>();
        
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_INIT_SEQUENCE, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS, sectionId2String);
        
        return sectionId2String;
    }

    private static void putEnumAsIntStr(EEnvConfigSections id, 
                                 Map<EEnvConfigSections, String> sectionId2String) {
        sectionId2String.put(id, String.valueOf(id.swigValue()));
    }


    @Override
    public Control createContents(Composite parent) {
        
        m_fieldEditorPanel = new Composite(parent, SWT.BORDER);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        gridData.widthHint = 400;
        m_fieldEditorPanel.setLayoutData(gridData);
        
        m_fieldEditorPanel.setLayout(new MigLayout("fill", "[fill][min!]",
                                                   "[min!][fill][min!][min!]"));
        
        // m_fieldEditorPanel.setFont(parent.getFont());

        createFieldEditors();

        initialize();
        checkState();
        
        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        KGUIBuilder builder = new KGUIBuilder(parent);
        
        // see the doc of FieldEditor class for the list of all available field editors 
        m_isRunInitSequence = new PrefBooleanEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ), 
                                         "&Always run init sequence before run",
                                         BooleanFieldEditor.DEFAULT,
                                         parent,
                                         "gaptop 10, gapbottom 10, wrap",
                                         "If checked, init sequence configured below is automatically run when we start a test.");
        addField(m_isRunInitSequence);
        
        KGUIBuilder initGroup = builder.group("Init sequence", 
                                              "growy, wrap", 
                                              new String[]{"fill", "[min!][fill]" ,"[min!][fill][min!]"});
        
        initGroup.label("Operations selected in this group are executed in the same " +
                          "order as they appear below.", "span 2, gapbottom 5, wrap");
        
        Map<String, IContentProposalProvider> contentProvidersMap = new TreeMap<>();
        GlobalsContainer globalContainer = GlobalsConfiguration.instance().getGlobalContainer();
        GlobalsProvider coreIdsGlobalsProvider = globalContainer.getCoreIdsGlobalsProvider();
        AsystContentProposalProvider coreIdProvider = ISysUIUtils.createContentProposalsAdapter(coreIdsGlobalsProvider, 
                                                                                                true); 
        contentProvidersMap.put("coreId", coreIdProvider);
        
        GlobalsProvider funcProvider = globalContainer.getAllFunctionsProvider();
        AsystContentProposalProvider funcContentProvider = ISysUIUtils.createContentProposalsAdapter(funcProvider, 
                                                                                                     true); 
        contentProvidersMap.put("params", funcContentProvider);

        // Init sequence table
         m_initSeqTable = new PrefsTBTableEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_INIT_SEQUENCE), 
                                        "", 
                                        initGroup.getParent(),
                                        m_testBase,
                                        EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(),
                                        "wmin 0, hmin 180, grow, wrap",
                                        "This table defines initializtion actions to be performed before "
                                        + "tests are started. Empty lines are ignored.\n\n"
                                        + "Description of columns:\n"
                                        + "coreId: ID of the core for which to apply settings. Empty core ID means core ID\n"
                                        + "    with index 0, which is also the only core in single-core systems.\n"
                                        + "action: Action to be performed. Can be one of:\n"
                                        + "    download: download the executable to target. No parameters.\n"
                                        + "    reset: reset the target. No parameters.\n"
                                        + "    run: run the target. Stop function or expression may be specified as parameter.\n"
                                        + "        If parameter is not specified, target must stop some other way, for example on a\n"
                                        + "        breakpoint. Usually this parameter is set to 'main'.\n"
                                        + "    delAllBreakpoints: delete all breakpoints, so that test will not be stopped by\n"
                                        + "        some forgotten breakpoint. No parameters.\n"
                                        + "    connectToCore: testIDEA conencts to winIDEA instance controlling the core with the given ID.\n"
                                        + "        If winIDEA is not running, it is lauched.\n"
                                        + "    callTargetFunction: calls function on a target, usually for initialization purposes.\n"
                                        + "        At least function name is required as parameter, remaining parameters are passed to\n"
                                        + "        the called function.\n"
                                        + "    callScriptFunction: call script function, usually for initialization purposes.\n"
                                        + "        At least function name is required as parameter, remaining parameters are passed to\n"
                                        + "        the called function.\n"
                                        + "    loadSymbolsOnly: initializes iSYSTEM hardware and loads symbols to winIDEA. Use this action\n"
                                        + "        when code is already downloaded to the target. No parameters.\n"
                                        + "    waitUntilStopped: waits until target stops, for example  on function or breakpoint\n"
                                        + "params: Action parameters. See above for parameters of each action.",
                                        m_viewPart,
                                        contentProvidersMap);
        
        m_initSeqTable.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY,
                                            SWTBotConstants.INIT_SEQUENCE_KTABLE); // for unit testing
        addField(m_initSeqTable);
        
        m_isCheckTargetBeforeRun = new PrefBooleanEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN), 
                                       "Check target state before run",
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "gaptop 7, split 2",
                                       "If checked, and stop function is specified in init sequence, testIDEA issues a warning \n"
                                       + "if target is not stopped at the function before tests are executed.");
        addField(m_isCheckTargetBeforeRun);
        
        m_isVerifySymbolsBeforeRun = new PrefBooleanEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN), 
                                                           "Verify symbols before run",
                                                           BooleanFieldEditor.DEFAULT,
                                                           parent,
                                                           "gaptop 7, wrap",
                                                           "If checked, testIDEA verifies that all symbols in test cases (functions tested, stubs, ...)\n"
                                                           + "exist on target. If not all of them exist, a warning dialog is popped-up.");
        addField(m_isVerifySymbolsBeforeRun);
                
        // disable all interrupts
        m_isDisableInterrupts = new PrefBooleanEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS), 
                                       "Disable interrupts",
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "gaptop 15, wrap",
                                       "If checked, interrupts on target are disabled during test run. After test run, the previous enabled/disabled state is restored.\n" +
                                       "Use this option when interrupts could change target state so that test results are unpredictable.");
        addField(m_isDisableInterrupts);
    }


    @Override
    public void init(IWorkbench workbench) {
    }

    
    @Override
    protected Composite getFieldEditorParent() {
        return m_fieldEditorPanel;
    }
    
    @Override
    protected void performApply() {
        super.performApply();
        // apply button should save data to CTestBase objects
        IPreferenceStore prefStore = getPreferenceStore();
        if (prefStore instanceof TestBasePreferenceStore) {
            TestBasePreferenceStore tbPrefStore = (TestBasePreferenceStore) prefStore;
            tbPrefStore.save();
        }
    }
    
    @Override
    protected void performDefaults() {
        super.performDefaults();
    }


    @Override
    public void execAction(AbstractAction action) {
        m_actionQueue.exec(action);
    }


    @Override
    public void fireEvent(ModelChangedEvent event) {
    }


    public PrefBooleanEditor getIsRunInitSequence() {
        return m_isRunInitSequence;
    }


    public PrefsTBTableEditor getInitSeqTable() {
        return m_initSeqTable;
    }


    public PrefBooleanEditor getIsCheckTargetBeforeRun() {
        return m_isCheckTargetBeforeRun;
    }

    
    public PrefBooleanEditor getIsVerifySymbolsBeforeRun() {
        return m_isVerifySymbolsBeforeRun;
    }


    public PrefBooleanEditor getIsDisableInterrupts() {
        return m_isDisableInterrupts;
    }
}
