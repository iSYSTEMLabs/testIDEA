package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.connect.CToolsConfig.EToolsConfigSections;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.itest.preferences.controls.PrefStringEditor;
import si.isystem.itest.ui.spec.AnalyzerEditor;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.ui.utils.KGUIBuilder;

public class ToolsConfigPrefsPage extends FieldEditorPreferencePage
                                  implements IWorkbenchPreferencePage {

    private static final String PREF_IS_SET_TEST_ID_ON_PASTE = "tools.isSetTestIdOnPaste";
    private static final String PREF_ANALYZER_F_NAME = "tools.analyzerFName";
    private static final String PREF_IS_AUTO_SET_ANALYZER_F_NAME = "tools.isAutoSetAnalyzerFName";
    
    private Composite m_fieldEditorPanel;
    private Map<EToolsConfigSections, String> m_sectionId2String;


    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public ToolsConfigPrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();

        setPreferenceStore(prefStore);

        setDescription("Settings on this page define behavior of wizards and\n" +
                "commands from iTools manu.");
    }


    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public ToolsConfigPrefsPage() {
        super(GRID);
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForPreferences();

        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(preferenceStore);

        setDescription("Specify configuration of wizards and commands from iTools manu to be\n" +
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
    public static Map<EToolsConfigSections, String> mappingForPreferences() {

        Map<EToolsConfigSections, String> sectionId2String = 
                new TreeMap<EToolsConfigSections, String>();

        sectionId2String.put(EToolsConfigSections.E_SECTION_IS_AUTO_SET_ANALYZER_FNAME, PREF_IS_AUTO_SET_ANALYZER_F_NAME);
        sectionId2String.put(EToolsConfigSections.E_SECTION_ANALYZER_FNAME, PREF_ANALYZER_F_NAME);
        sectionId2String.put(EToolsConfigSections.E_SECTION_IS_SET_TEST_ID_ON_PASTE, PREF_IS_SET_TEST_ID_ON_PASTE);

        return sectionId2String;
    }


    /**
     * This method should be called, when this class is used as properties page 
     * of the project or iyaml file resource. Section ids are used in this case, 
     * which modify setting in the currently opened project - they are saved
     * into iyaml file. 
     * @return 
     */
    public static Map<EToolsConfigSections, String> mappingForProperties() {

        Map<EToolsConfigSections, String> sectionId2String = 
                new TreeMap<EToolsConfigSections, String>();

        putEnumAsIntStr(EToolsConfigSections.E_SECTION_IS_AUTO_SET_ANALYZER_FNAME, sectionId2String);
        putEnumAsIntStr(EToolsConfigSections.E_SECTION_ANALYZER_FNAME, sectionId2String); 
        putEnumAsIntStr(EToolsConfigSections.E_SECTION_IS_SET_TEST_ID_ON_PASTE, sectionId2String);

        return sectionId2String;
    }

    private static void putEnumAsIntStr(EToolsConfigSections id, 
                                        Map<EToolsConfigSections, String> sectionId2String) {
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

        m_fieldEditorPanel.setLayout(new MigLayout("fillx", "[min!][fill]"));

        createFieldEditors();

        initialize();
        checkState();

        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        KGUIBuilder builder = new KGUIBuilder(parent);

        KGUIBuilder initGroupParent = builder.group("Default analyzer document name", 
                                              "span 2, growx, wrap",
                                              true,
                                              "fillx", "", "");
        // create another composite, as Prefs child components get font from
        // the parent, so they would be all shown in bold
        KGUIBuilder initGroup = initGroupParent.newPanel("fillx", "[min!][fill]", "", 
                                                         "growx", SWT.NONE);
        
        addField(new PrefBooleanEditor(m_sectionId2String.get(EToolsConfigSections.E_SECTION_IS_AUTO_SET_ANALYZER_FNAME), 
                                       "&Set analyzer document file name on analyzer activation",
                                       BooleanFieldEditor.DEFAULT,
                                       initGroup.getParent(),
                                       "span 2, gapbottom 10, wrap",
                "If checked, and analyzer document file name is empty, analyzer file "
                + "name is\nautomatically set when analyzer is activated."));

        String[] hostVars = HostVarsUtils.getHostVarsForAnalyzerFileName(); 

        String tooltip = HostVarsUtils.getTooltip("Default name for analyzer file name. It is used "
                + "when the above checkbox is set and analyzer section is activated.\n"
                + "The following host variables may be used:\n\n", 
                hostVars);
        
        PrefStringEditor analyzerFNameField = 
                new PrefStringEditor(m_sectionId2String.get(EToolsConfigSections.E_SECTION_ANALYZER_FNAME), 
                                     "&Analyzer doc. file name:", 
                                     StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                     initGroup.getParent(),
                                     "span 3, growx, wrap",
                                     tooltip.toString());
        
        final Text analyzerFNameTxt = analyzerFNameField.getTextControl(initGroup.getParent());

        HostVarsUtils.setContentProposals(analyzerFNameTxt, hostVars);
        
        analyzerFNameTxt.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                String ext = UiUtils.addExtension(analyzerFNameTxt.getText(), 
                                                  AnalyzerEditor.ANALYZER_DOC_EXTENSION, 
                                                  true, false);
                analyzerFNameTxt.setText(ext);
            }
            
            @Override public void focusGained(FocusEvent e) {}
        }); 

        addField(analyzerFNameField);
        
        initGroup.label("Example: ${testId}.trd", "skip");
        
        addField(new PrefBooleanEditor(m_sectionId2String.get(EToolsConfigSections.E_SECTION_IS_SET_TEST_ID_ON_PASTE), 
                                       "&Set test ID on Paste",
                                       BooleanFieldEditor.DEFAULT,
                                       builder.getParent(),
                                       "span 2, gaptop 10, wrap",
                "If checked, testIDEA sets test ID of test cases during Paste operation.\n"
                + "This way it is less likely to have test cases with duplicate test IDs.\n"
                + "See 'Properties | General' for Auto ID format definiton."));

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
