package si.isystem.itest.preferences;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.connect.CScriptConfig.ETestScriptConfigSectionIds;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.FieldContentProvider;
import si.isystem.itest.preferences.controls.PrefDirectoryEditor;
import si.isystem.itest.preferences.controls.PrefIntegerEditor;
import si.isystem.itest.preferences.controls.PrefMultiTextEditor;
import si.isystem.itest.preferences.controls.PrefStringEditor;
import si.isystem.ui.utils.KGUIBuilder;

public class ScriptPrefsPage extends FieldEditorPreferencePage
                             implements IWorkbenchPreferencePage {

    private Composite m_fieldEditorPanel;
    private Map<ETestScriptConfigSectionIds, String> m_sectionId2String;

    private static String m_lastPath;


    /**
     * This ctor initializes the page from testIDEA model - it is called
     * by when user edits project properties.
     */
    public ScriptPrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();
        
        setPreferenceStore(prefStore);
        
        setDescription("Settings on this page define environment for execution of Python scripts.\n" +
                       "They are used for test execution, and are saved to project file.\n" +
                       "Note: Location of Python interpreter is configured in winIDEA.");
    }
    

    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public ScriptPrefsPage() {
        super(GRID);
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForPreferences();
        
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        
        setDescription("Specify settings for execution of Python scripts, which will be\n" +
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
    public static Map<ETestScriptConfigSectionIds, String> mappingForPreferences() {
        Map<ETestScriptConfigSectionIds, String> sectionId2String = new TreeMap<ETestScriptConfigSectionIds, String>();
        sectionId2String.put(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_WORKING_DIR, "script.workingDir");
        sectionId2String.put(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_SYS_PATH, "script.sysPath");
        sectionId2String.put(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_MODULES, "script.modules"); 
        sectionId2String.put(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_EXTENSION_CLASS, "script.callbackClass"); 
        sectionId2String.put(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_TIMEOUT, "script.timeout");
        
        return sectionId2String;
    }

    
    /**
     * This method should be called, when this class is used as properties page 
     * of the project or iyaml file resource. Section ids are used in this case, 
     * which modify setting in the currently opened project - they are saved
     * into iyaml file. 
     * @return 
     */
    public static Map<ETestScriptConfigSectionIds, String> mappingForProperties() {
        Map<ETestScriptConfigSectionIds, String> sectionId2String = 
            new TreeMap<ETestScriptConfigSectionIds, String>();
        
        for (ETestScriptConfigSectionIds sectionId : ETestScriptConfigSectionIds.values()) {
            sectionId2String.put(sectionId, String.valueOf(sectionId.swigValue()));
        }
        
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
        
        m_fieldEditorPanel.setLayout(new MigLayout("fillx", "[min!][fill][min!]"));
        
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
        addField(new PrefDirectoryEditor(m_sectionId2String.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_WORKING_DIR), 
                                         "&Script working folder:",
                                         parent,
                                         PrefDirectoryEditor.VALIDATE_ON_KEY_STROKE,
                                         "wrap",
                                         "The working folder of Python interpreter, which runs test " +
                                         "scripts. If empty, the folder of currently opened test specification file is used."));
        // workspacePathEditor.setEmptyStringAllowed(false);
        
        addField(new PrefStringEditor(m_sectionId2String.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_MODULES),
                                      "&Imported modules:", 
                                      StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                      parent, "wrap",
                                      "The list of Python modules, which should be imported by testIDEA when running test scripts.\n" +
                                      "Example: sys, myModule."));
        
        addField(new PrefStringEditor(m_sectionId2String.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_EXTENSION_CLASS), 
                                      "&Extension class:", 
                                      StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                      parent, "wrap", 
                                      "Name of the class which contains functions to be called during tests.\n" +
                                      "The name should contain module name, for example 'myModule.myClass'." + 
                                      "This is the singleton class for all test cases in the file."));

        addField(new PrefIntegerEditor(m_sectionId2String.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_TIMEOUT), 
                                       "&Timeout:", 
                                       parent,
                                       10,
                                       "split 2, wmax 100", 
                                       "Defines how long testIDEA waits for script functions to return, in seconds.\n" +
                                       "If any of the script functions does not return on time, test is considered as failed."));

        builder.label("seconds", "wrap");
        builder.separator("span 3, growx, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);
        
        FieldContentProvider contentProvider = new FieldContentProvider() {
            
            @Override
            public String getValue() {

                DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SHEET);
                dialog.setMessage("Select path with Python modules:");
                if (m_lastPath != null) {
                    if (new File(m_lastPath).exists()) {
                        dialog.setFilterPath(m_lastPath);
                    }
                }
                
                String dir = dialog.open();
                if (dir != null) {
                    dir = dir.trim();
                    if (dir.length() == 0) {
                        return null;
                    }
                    m_lastPath = dir;
                }
                return dir;
            }
        };
        
        addField(new PrefMultiTextEditor(m_sectionId2String.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_SYS_PATH), 
                                         "&Sys paths:",
                                         parent,
                                         "h 120::, growy",
                                         "wrap",
                                         "List of paths to add to Python's sys.path() for module loading. Required\n" +
                                                 "only if custom modules are used, which are not in one of Python's module search paths.\n" +
                                                 "Examples:\n    /home/mycripts\n    d:/common/utils",
                                         "Browse",
                                         contentProvider));
        /*
        addField(new PrefPathEditor(m_sectionId2String.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_SYS_PATH), 
                                    "&Sys paths:",
                                    "Select path with Python modules:",
                                    parent, "wrap", 
                                    "List of paths to add to Python's sys.path() for module loading. Required\n" +
                                    "only if custom modules are used, which are not in one of Python's module search paths.\n" +
                                    "If paths contain spaces or commas, they should be quoted!\n" + 
                                    "Example:\n    /home/mycripts, d:/common/utils"));
                                    */
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
}   
