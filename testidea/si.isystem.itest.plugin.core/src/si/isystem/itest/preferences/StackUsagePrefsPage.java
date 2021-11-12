package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.part.ViewPart;

import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CStackUsageConfig;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.controls.IValidator;
import si.isystem.itest.preferences.controls.PrefsTBTableEditor;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

public class StackUsagePrefsPage extends FieldEditorPreferencePage
                            implements IWorkbenchPreferencePage {

    private Composite m_fieldEditorPanel;
    private Map<EEnvConfigSections, String> m_sectionId2String;
    private ViewPart m_viewPart;

    /* private PrefStringEditor m_stackUsageBaseAddr;
    private PrefStringEditor m_stackUsageEndAddr;
    private PrefStringEditor m_stackUsagePattern; */

    
    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public StackUsagePrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        m_viewPart = StatusView.getView();
        
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForStackUsageProperties();
        
        setPreferenceStore(prefStore);
        
        setDescription("Settings on this page define intialization for stack usage measurement.\n" + 
                       "This page can be accessed with command 'File | Properties'.");
    }
    
    
    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public StackUsagePrefsPage() {
        super(GRID);

        m_viewPart = StatusView.getView();
        
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForStackUsagePreferences();
        
        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(preferenceStore);
        
        setDescription("Specify stack measurement values, which will be " +
                       "used as defaults when new projects are created.\n" +
                       "Select 'File | Properties' to edit configuration of the active project!");
    }

    
    public static Map<EEnvConfigSections, String> mappingForStackUsagePreferences() {
        
        Map<EEnvConfigSections, String> sectionId2String = new TreeMap<>();

        /* sectionId2String.put(EStackUsageConfigSections.E_SECTION_IS_ACTIVE, "run.stackUsage.isActive");
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_BASE, "run.stackUsage.baseAddr");
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_END, "run.stackUsage.endAddr");
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_PATTERN, "run.stackUsage.pattern");
        */
        sectionId2String.put(EEnvConfigSections.E_SECTION_STACK_USAGE, "stackUsage");

        return sectionId2String;
    }
    

    public static Map<EEnvConfigSections, String> mappingForStackUsageProperties() {
        
        Map<EEnvConfigSections, String> sectionId2String = new TreeMap<>();
        
        sectionId2String.put(EEnvConfigSections.E_SECTION_STACK_USAGE, 
                             String.valueOf(EEnvConfigSections.E_SECTION_STACK_USAGE.swigValue()));
        
        /*
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_IS_ACTIVE, 
                             String.valueOf(EStackUsageConfigSections.E_SECTION_IS_ACTIVE.swigValue()));
        
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_BASE, 
                             String.valueOf(EStackUsageConfigSections.E_SECTION_BASE.swigValue()));
        
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_END, 
                             String.valueOf(EStackUsageConfigSections.E_SECTION_END.swigValue()));
        
        sectionId2String.put(EStackUsageConfigSections.E_SECTION_PATTERN, 
                             String.valueOf(EStackUsageConfigSections.E_SECTION_PATTERN.swigValue()));
                             */
        
        return sectionId2String;
    }
    
    
    @Override
    protected Control createContents(Composite parent) {
        
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
        
        m_fieldEditorPanel.setLayout(new MigLayout("fill", "[fill][min!]", "[fill][min!]"));
        
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

        KGUIBuilder stackUsageGroup = builder.group("Stack usage measurement", 
                                                    "gaptop 15, wmin 400, wrap",
                                                    false, "fill", "[min!][fill]", "[fill][min!]");
        
        Map<String, IContentProposalProvider> contentProvidersMap = new TreeMap<>();
        GlobalsContainer globalContainer = GlobalsConfiguration.instance().getGlobalContainer();
        GlobalsProvider labelsGlobalsProvider = globalContainer.getCodeLabelsGlobalsProvider();
        AsystContentProposalProvider provider = ISysUIUtils.createContentProposalsAdapter(labelsGlobalsProvider, 
                                                                                          true);
        // see CTestEnvironmentConfig.cpp
        contentProvidersMap.put("baseAddr", provider);
        contentProvidersMap.put("endAddr", provider);
        
        CTestEnvironmentConfig envConfig = TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration();
        PrefsTBTableEditor stackTable = new PrefsTBTableEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_STACK_USAGE), 
                                        "", 
                                        stackUsageGroup.getParent(),
                                        envConfig,
                                        EEnvConfigSections.E_SECTION_STACK_USAGE.swigValue(),
                                        "wmin 0, hmin 180, grow, wrap",
                                        "This table defines stack usage measurement configuration.\n\n"
                                        + "Description of columns:\n"
                                        + "coreId: ID of the core for which to apply settings. Empty core ID means core ID\n"
                                        + "    with index 0, which is also the only core in single-core systems.\n"
                                        + "isActive: If checked, stack area defined with base and end addresses below, is seeded with pattern before test run.\n"
                                        + "    This option must be selected, if you want to measure stack usage during tests.\n"
                                        + "    If it is not selected, winIDEA configuration is used (Debug | Debug Options ... | tab 'Stack Usage'),\n"
                                        + "    and stack usage is only measured for test cases, which specify the limit.\n"
                                        + "baseAddr: The low address of stack area, the first one to be written with pattern.\n"
                                        + "    Enter hex value, decimal value, or label here.\n"
                                        + "endAddr: The high address of stack area + 1. It is no longer seeded with pattern: stack size = endAddr - baseAddr.\n"
                                        + "    Enter hex value, decimal value, or label here.\n"
                                        + "pattern: 8-bit value, which is written to stack area defined with base and end addresses above.\n"
                                        + "    Stack area, which contains other values after test, is considered by testIDEA as used stack memory.\n"
                                        + "    Enter hex or decimal value here.",
                                        m_viewPart, 
                                        contentProvidersMap);
        
        stackTable.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY,
                                        SWTBotConstants.STACK_USAGE_KTABLE); // for unit testing

        addField(stackTable);

        stackTable.setValidator(new IValidator() {
            
            @Override
            public String validate(CTestBase testBase, int section) {
                CTestBaseList stackUsageOpts = testBase.getTestBaseList(section, true);
                int numStackUsageOpts = (int) stackUsageOpts.size();
                for (int idx = 0; idx < numStackUsageOpts; idx++) {
                    CStackUsageConfig suCfg = CStackUsageConfig.cast(stackUsageOpts.get(idx));
                    try {
                        int pattern = suCfg.getPattern();
                        if (pattern < 0  ||  pattern > 255) {
                            return "Pattern is out of allowed range 0-0xff in line " + (idx + 1);
                        }
                    } catch (Exception ex) {
                        return ex.getMessage() + " Line: " + idx;
                    }
                }
                
                return null;
            }
        });
        
        /*
        addField(new PrefBooleanEditor(m_sectionId2String.get(EStackUsageConfigSections.E_SECTION_IS_ACTIVE), 
                                       "Is active",
                                       BooleanFieldEditor.DEFAULT,
                                       stackUsageGroup.getParent(),
                                       "gaptop 15, gapbottom 15, wrap",
                                       "If checked, stack area defined with base and end addresses below, is seeded with pattern before test run.\n" +
                                       "This option must be selected, if you want to measure stack usage during tests.\n" +
                                       "If it is not selected, winIDEA configuration is used (Debug | Debug Options ... | tab 'Stack Usage'),\n" +
                                       "and stack usage is only measured for test cases, which specify the limit."));

        m_stackUsageBaseAddr = new PrefStringEditor(m_sectionId2String.get(EStackUsageConfigSections.E_SECTION_BASE), 
                                                     "Stack &base address:", 
                                                     StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                                     stackUsageGroup.getParent(),
                                                     "growx, wrap", 
                                                     "The low address of stack area, the first one to be written with pattern.\n" +
                                                     "Enter hex value, decimal value, or label here.");
        addField(m_stackUsageBaseAddr);

        m_stackUsageEndAddr = new PrefStringEditor(m_sectionId2String.get(EStackUsageConfigSections.E_SECTION_END), 
                                                   "Stack e&nd address:", 
                                                   StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                                   stackUsageGroup.getParent(),
                                                   "growx, wrap", 
                                                   "The high address of stack area + 1. It is no longer seeded with pattern: stack size = endAddr - baseAddr.\n" +
                                                   "Enter hex value, decimal value, or label here.");
        addField(m_stackUsageEndAddr);

        m_stackUsagePattern = new PrefStringEditor(m_sectionId2String.get(EStackUsageConfigSections.E_SECTION_PATTERN), 
                                                   "Seeding &pattern:", 
                                                   StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                                   stackUsageGroup.getParent(),
                                                   "growx, wrap", 
                                                   "8-bit value, which is written to stack area defined with base and end addresses above.\n" +
                                                   "Stack area, which contains other values after test, is considered by testIDEA as used stack memory.\n" +
                                                   "Enter hex or decimal value here.");
        addField(m_stackUsagePattern);
*/        
        stackUsageGroup.getParent().setFont(FontProvider.instance().getBoldControlFont(stackUsageGroup.getParent()));


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
    
}
