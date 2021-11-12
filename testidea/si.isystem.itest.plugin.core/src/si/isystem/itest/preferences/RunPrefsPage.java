package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.PrefRadioGroupEditor;
import si.isystem.itest.preferences.controls.PrefStringEditor;
import si.isystem.ui.utils.FontProvider;

public class RunPrefsPage extends FieldEditorPreferencePage
                          implements IWorkbenchPreferencePage {

    private Composite m_fieldEditorPanel;
    private Map<EEnvConfigSections, String> m_sectionId2String;
    
    private PrefStringEditor m_testTimeout;
    private PrefRadioGroupEditor m_breakpointsType;


    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public RunPrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();
        
        setPreferenceStore(prefStore);
        
        setDescription("Settings on this page define configuration for test execution. " +
                       "They are are saved to project file.\n" +
                       "This page can be accessed with commands 'File | Properties' or 'Test | Configuration'.");
    }
    

    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public RunPrefsPage() {
        super(GRID);
        
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForPreferences();
        
        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(preferenceStore);
        
        setDescription("Specify test configuration, which will be used as defaults " +
                       "when new projects are created.\n" +
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
        
        sectionId2String.put(EEnvConfigSections.E_SECTION_TEST_TIMEOUT, "run.testTimeout");
        sectionId2String.put(EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE, "run.breakpointsType");
        
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
        
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_TEST_TIMEOUT, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE, sectionId2String);
        
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
        
        m_fieldEditorPanel.setLayout(new MigLayout("fill", "[min!][min!][fill]",
                                                   "[min!][min!][fill]"));
        
        // m_fieldEditorPanel.setFont(parent.getFont());

        createFieldEditors();

        initialize();
        checkState();
        
        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // test timeout
        m_testTimeout = new PrefStringEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_TEST_TIMEOUT), 
                                       "&Test execution timeout:",
                                       StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                       parent,
                                       "wmin 100",
                                       "If defined and greater than 0, then test is terminated after this amount of milliseconds.\n" +
                                       "This setting is not used if timeout in test case is specified.");
        addField(m_testTimeout);

        Label millisLbl = new Label(parent, SWT.NONE);
        millisLbl.setText("ms");
        millisLbl.setLayoutData("wrap");
        
        
        // breakpoints control
        
        // PrefRadioGroupEditor gets font from parent, so replace it temporary to get bold 
        // group title
       //  Font originalFont = parent.getFont();
        // parent.setFont();
        
        Font boldFont = FontProvider.instance().getBoldControlFont(parent.getParent());
        m_breakpointsType = new PrefRadioGroupEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE),
                                          "Type of Breakpoints", 1,
                                          new String [][] {
                                              {"Keep winIDEA setting", "keepWinIDEASetting"}, 
                                              {"Use hardware breakpoints", "useHWBreakpoints"},
                                              {"Use software breakpoints", "useSWBreakpoints"},
                                              {"Use hardware breakpoints during target init, software breakpoints during testing", "useHWBPsForInitThenSWBPs"}},
                                          parent,
                                          true,
                                          "span 3, wrap, gaptop 15",
                                          "This setting defines how testIDEA uses target breakpoints.",
                                          true,
                                          boldFont);
        addField(m_breakpointsType);
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

    public PrefStringEditor getTestTimeout() {
        return m_testTimeout;
    }


    public PrefRadioGroupEditor getBreakpointsType() {
        return m_breakpointsType;
    }
}
