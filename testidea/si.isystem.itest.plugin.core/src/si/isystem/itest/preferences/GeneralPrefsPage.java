package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.FieldContentProvider;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.itest.preferences.controls.PrefFileEditor;
import si.isystem.itest.preferences.controls.PrefStringButtonEditor;
import si.isystem.itest.preferences.controls.PrefStringEditor;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page with little code, which knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 * 
 * MK:
 * This class is used in double roles - one if for preference pages as described 
 * above. In this role this class stores/retrieves values from preferences managed 
 * by RCP. Values stored there are used as init values when creating new test cases
 * or itest files.  
 * 
 * In the second role this class is used in property page. In this case prefs store
 * is attched to CTestEnvironmentConfig class and modifies values there - in the
 * model. 
 */

public class GeneralPrefsPage extends FieldEditorPreferencePage
	                             implements IWorkbenchPreferencePage {

	private static final String PREF_LOG_PARAMS = "env.logParams";
	private static final String PREF_AUTO_ID_FORMAT = "env.autoIdFormat";
	private static final String PREF_IS_AUTO_CONNECT = "env.autoConnect";
    private static final String PREF_WI_PORT = "env.wiPort";
    private static final String PREF_WI_ADDRESS = "env.wiAddress";
    private static final String PREF_DEFAULT_RET_VAL_NAME = "env.defaultRetValName";
    private static final String PREF_USE_QUALIFIED_FUNC_NAME = "env.useQualifiedFuncName";
    private static final String PREF_WI_WORKSPACE = "env.wiWorkspace";
    
    private Composite m_fieldEditorPanel;
    private PrefFileEditor m_wsPathEditor;
    private Map<EEnvConfigSections, String> m_sectionId2String;
    private PrefStringButtonEditor m_autoIdEditor;

    
    /**
     * This ctor initializes the page from testIDEA model - it is called
     * by when user edits project properties.
     */
    public GeneralPrefsPage(IPreferenceStore prefStore) { 
        super(GRID);

        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();
        
        setPreferenceStore(prefStore);
        
        setDescription("Settings on this page define test environment.\n" +
                       "They are used for test execution, and are saved to project file.");
    }
    

    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public GeneralPrefsPage() {
		super(GRID);

		// default ID mapping used in preferences page
		m_sectionId2String = mappingForPreferences();
        
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        
        setDescription("These preferences are used as defaults, when new " +
                       "projects are created.\n" +
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
        Map<EEnvConfigSections, String> sectionId2String = new TreeMap<EEnvConfigSections, String>();
        sectionId2String.put(EEnvConfigSections.E_SECTION_WINIDEA_WORKSPACE_FILE_NAME, PREF_WI_WORKSPACE);
        sectionId2String.put(EEnvConfigSections.E_SECTION_USE_QUALIFIED_FUNC_NAME, PREF_USE_QUALIFIED_FUNC_NAME);
        sectionId2String.put(EEnvConfigSections.E_SECTION_DEFAULT_RET_VAL_NAME, PREF_DEFAULT_RET_VAL_NAME);
        sectionId2String.put(EEnvConfigSections.E_SECTION_WINIDEA_ADDRESS, PREF_WI_ADDRESS); 
        sectionId2String.put(EEnvConfigSections.E_SECTION_WINIDEA_PORT, PREF_WI_PORT); 
        sectionId2String.put(EEnvConfigSections.E_SECTION_IS_AUTO_CONNECT, PREF_IS_AUTO_CONNECT);
        sectionId2String.put(EEnvConfigSections.E_SECTION_AUTO_ID_FORMAT_STRING, PREF_AUTO_ID_FORMAT);
        sectionId2String.put(EEnvConfigSections.E_SECTION_LOG_PARAMETERS, PREF_LOG_PARAMS);
        
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
        Map<EEnvConfigSections, String> sectionId2String = 
            new TreeMap<EEnvConfigSections, String>();
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_WINIDEA_WORKSPACE_FILE_NAME, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_USE_QUALIFIED_FUNC_NAME, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_DEFAULT_RET_VAL_NAME, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_WINIDEA_ADDRESS, sectionId2String); 
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_WINIDEA_PORT, sectionId2String); 
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_IS_AUTO_CONNECT, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_AUTO_ID_FORMAT_STRING, sectionId2String);
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_LOG_PARAMETERS, sectionId2String);
        
        return sectionId2String;
    }

    
    private static void putEnumAsIntStr(EEnvConfigSections id, 
                                        Map<EEnvConfigSections, String> sectionId2String) {
        sectionId2String.put(id, String.valueOf(id.swigValue()));
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
        
        m_fieldEditorPanel.setLayout(new MigLayout("fillx", "[min!][fill][min!]"));
        
        // m_fieldEditorPanel.setFont(parent.getFont());

        createFieldEditors();

        initialize();
        checkState();
        return m_fieldEditorPanel;
    }


	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	@Override
    public void createFieldEditors() {
	    
        Composite parent = getFieldEditorParent();

        KGUIBuilder builder = new KGUIBuilder(parent);
        builder.label("Workspace file (cmd. line): ");
        
        final String cmdLineWsPath = Activator.getDefault().getCmdLineWorkspaceName();
        if (cmdLineWsPath != null) {
            Label lbl = builder.label(cmdLineWsPath, "", SWT.BORDER);
            UiTools.setToolTip(lbl, "winIDEA workspace file as given in command line.");
        } else {
            Label lbl = builder.label("Not specified!", "", SWT.BORDER);
            UiTools.setToolTip(lbl, "winIDEA workspace file as given in command line.");
        }
        
        Button copyButton = builder.button("To test spec.", "wrap");
        UiTools.setToolTip(copyButton, "Copies winIDEA workspace path given as testIDEA command line\n" +
                                  "parameter to test specification field below.");
        copyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                m_wsPathEditor.setText(cmdLineWsPath);
            }
        });        
        if (cmdLineWsPath == null) {
            copyButton.setEnabled(false);
        }
        
        // see the doc of FieldEditor class for the list of all available field editors 
        m_wsPathEditor = new PrefFileEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_WINIDEA_WORKSPACE_FILE_NAME), 
                                            "&Workspace file (test spec):",
                                            false,
                                            StringButtonFieldEditor.VALIDATE_ON_FOCUS_LOST,
                                            false,
                                            parent,
                                            "wrap",
                                            "winIDEA workspace file to be opened when running tests from script. If empty, the last opened\n"
                                            + "workspace will be used (winIDEA Tools | Options | tab Environment, 'Reload last workspace at startup'\n"
                                            + "must be selected in this case).");
        // workspacePathEditor.setEmptyStringAllowed(false);
        addField(m_wsPathEditor);

        
        addField(new PrefStringEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_DEFAULT_RET_VAL_NAME),
                                      "Default &ret. val. name:", 
                                      StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                      parent, "wrap",
                                      "Default name of a variable, which stores function return value.\n" +
                                      "Used when creating new test."));

        builder.label("If both Address and Port fields below are empty, then connection\n" +
                      "is made to the most recently used instance of winIDEA.", "gaptop 15, gapbottom 5 , span2, wrap");
        
        addField(new PrefStringEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_WINIDEA_ADDRESS), 
                                       "&Address:", 
                                       StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                       parent, "wrap", 
                                       "The IP address of the machine where winIDEA is running. This can be either \n" +
                                       "dotted decimal or an URL. If it is an empty string, the local host is considered."));
        addField(new PrefStringEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_WINIDEA_PORT), 
                                       "&Port:", 
                                       StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                       parent, "wrap", 
                                       "The TCP port to use. This should be the same value as configured in winIDEA\n" +
                                       "to which the connection is attempted. Default port value is 5315, and can\n" +
                                       "be set in winIDEA menu 'Tools | Options | isystem.connect | TCP Connection Port Range'."));

        /* No longer used in connection algorithm.
        addField(new PrefBooleanEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_IS_AUTO_CONNECT),
                                       "C&onnect automatically",
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "wrap, gaptop 10", 
                                       "If checked, connection is establised automatically when\n" +
                                       "lists of global variables or functions are needed. Otherwise use\n" +
                                       "menu option 'Tools | Connect'."));
        */
        
        builder.separator("span 3, growx, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);
        
        addField(new PrefBooleanEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_USE_QUALIFIED_FUNC_NAME),
                                       "&Use qualified function names",
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "span 2, wrap", 
                                       "If checked, then testIDEA provides qualified function names\n" +
                                       "in content proposals. Qualified function names are followed by download file,\n" +
                                       "for example:\n\n    evaluate,,sample.elf\n"));
        
        builder.separator("span 3, growx, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);

        m_autoIdEditor = new PrefStringButtonEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_AUTO_ID_FORMAT_STRING),
                                                    "Auto ID &Format:", 
                                                    parent, "wrap",
                                                    "Format string used for automatic ID assignment with "
                                                    + "command 'Tools | Set Test IDs'.\n"
                                                    + UiUtils.TEST_ID_ALLOWED_CHARS
                                                    + "For available host variables type '$'.",
                                                    "Wi&zard...",
                                                    new AutoIdChangeBtnDlg());
        
        final Text autoIDTxt = m_autoIdEditor.getTextControl(parent);

        HostVarsUtils.setContentProposals(autoIDTxt, 
                                          HostVarsUtils.getHostVarsForAutoTestID(true));
        
        addField(m_autoIdEditor);
        

        builder.separator("span 3, growx, gaptop 15, wrap", SWT.HORIZONTAL);

        
        builder.label("Set log file only when instructed by iSYSTEM support.\nExecute command 'Connect to winIDEA' to create log file!", "span, gaptop 10, wrap");

        addField(new PrefFileEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_LOG_PARAMETERS),
                                    "&Log file:", 
                                    false,
                                    StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                    false,
                                    parent,
                                    "wrap", 
                                    "Path and name of file for logging test actions. If empty, logging is off.\n\n" +
                                    "IMPORTANT: This setting has effect only when new connection to winIDEA is created.\n"));
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
    
    class AutoIdChangeBtnDlg implements FieldContentProvider {

        @Override
        public String getValue() {
            try {
                final Text autoIdTxt = m_autoIdEditor.getTextControl(getFieldEditorParent());
                AutoIdFormatDialog dlg = new AutoIdFormatDialog(Activator.getShell(),
                                                                autoIdTxt.getText());

                if (dlg.show()) {
                    return dlg.getFormat();
                }
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), "Can not set new auto ID string!", ex);
            }
            return null; 
        }

    }
}   
