package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.connect.CEvaluatorConfig;
import si.isystem.connect.CEvaluatorConfig.EAddressDisplay;
import si.isystem.connect.CEvaluatorConfig.EBinaryDisplay;
import si.isystem.connect.CEvaluatorConfig.ECharDisplay;
import si.isystem.connect.CEvaluatorConfig.EEnumDisplay;
import si.isystem.connect.CEvaluatorConfig.ETestEvaluatorConfigSectionIds;
import si.isystem.itest.common.Messages;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.itest.preferences.controls.PrefRadioGroupEditor;
import si.isystem.itest.preferences.controls.PrefStringEditor;

public class EvaluatorPrefsPage extends FieldEditorPreferencePage
                                          implements IWorkbenchPreferencePage {

    private static final String EVALUATOR_IS_OVERRIDE_WIN_IDEA_SETTINGS = "evaluator.isOverrideWinIDEASettings";
    private Composite m_fieldEditorPanel;
    private Map<ETestEvaluatorConfigSectionIds, String> m_sectionId2String;
    private PrefBooleanEditor m_isHexEditor;
    private FieldEditor m_isDisplayMemArea;
    private FieldEditor m_isCharArrayAsString;

    private PrefBooleanEditor m_isOverrideWinIDEASettings;
    private PrefBooleanEditor m_isDereferenceStringPointer;
    private PrefBooleanEditor m_isDisplayArrayStructValue;
    private PrefStringEditor m_vagueFloatPrecision;
    private PrefRadioGroupEditor m_charDisplay;
    private PrefBooleanEditor m_isAnsiFormat;
    private PrefRadioGroupEditor m_addressDisplay;
    private PrefRadioGroupEditor m_binaryDisplay;
    private PrefRadioGroupEditor m_enumDisplay; 

    private final static String PAGE_DESCRIPTION = 
            "Settings on this page define settings for winIDEA evaluator. They define " +
            "format for values in winIDEA Watch window and testIDEA expression evaluation.\n";

    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public EvaluatorPrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        // mapping used in properties page: <enum, enumValueAsString>,
        // for example <enum, "12">. Enum values are section IDs in CTestBase
        m_sectionId2String = mappingForProperties();
        
        setPreferenceStore(prefStore);
        
        setDescription(PAGE_DESCRIPTION +
                       "\nSettings can be modified only in the professional version of testIDEA!");
    }
    

    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public EvaluatorPrefsPage() {
        super(GRID);
        // mapping used in preferences page: <enum, keyString>,
        // for example <enum, "evaluator.isHex">
        m_sectionId2String = mappingForPreferences();
        
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        
        setDescription(PAGE_DESCRIPTION +
                       "\nThese settings are used as defaults when new projects are created. " +
                       "Select 'File | Properties' to edit configuration of the active project!\n\n" +
                       "Settings can be modified only in the professional version of testIDEA!");
    }

    
    /**
     * This method should be called when this class is used as preferences page 
     * - settings are stored into Eclipse PreferenceStore and are used as defaults
     * for all testIDEA files. This way the same string IDs are used for 
     * preferences even if section IDs in the code change. 
     *
     * @return mapping of CTestBase section id to strings used as keys for preferences.
     * 
     * @see mappingForProperties
     */
    public static Map<ETestEvaluatorConfigSectionIds, String> mappingForPreferences() {
        
        Map<ETestEvaluatorConfigSectionIds, String> sectionId2String = 
                new TreeMap<ETestEvaluatorConfigSectionIds, String>();
        
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_IS_OVERRIDE_WINIDEA_SETTINGS,
                             EVALUATOR_IS_OVERRIDE_WIN_IDEA_SETTINGS);
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_DISPLAY,
                "evaluator.charDisplay");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ANSI,
                "evaluator.ansi");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_HEX,
                "evaluator.hex");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_BINARY_DISPLAY,
                "evaluator.binaryDisplay");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_POINTER_MEM_AREA,
                "evaluator.displayPointerMemArea");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_ARRAY_AS_STRING,
                "evaluator.charArrayAsString");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DEREFERENCE_STRING_POINTERS,
                "evaluator.dereferenceStringPointers");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ADDRESS_DISPLAY,
                "evaluator.addressDisplay");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ENUM_DISPLAY,
                "evaluator.enumDisplay");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_COLLAPSED_ARRAY_STRUCT,
                "evaluator.displayCollapsedArrayStruct");
        sectionId2String.put(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_VAGUE_FLOAT_PRECISION,
                "evaluator.vagueFloatPrecision");

        return sectionId2String;
    }

    
    /**
     * This method should be called, when this class is used as properties page 
     * of the project or iyaml file resource. Section ids are used in this case, 
     * which modify setting in the currently opened project - they are saved
     * into iyaml file. 
     * @return 
     */
    public static Map<ETestEvaluatorConfigSectionIds, String> mappingForProperties() {
        
        Map<ETestEvaluatorConfigSectionIds, String> sectionId2String = 
            new TreeMap<ETestEvaluatorConfigSectionIds, String>();

        for (ETestEvaluatorConfigSectionIds sectionId : ETestEvaluatorConfigSectionIds.values()) {
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
        
        createFieldEditors();

        initialize();
        checkState();
        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        // see the doc of FieldEditor class for the list of all available field editors 
        
        // builder.separator("span 3, growx, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);
        
        m_isOverrideWinIDEASettings = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_IS_OVERRIDE_WINIDEA_SETTINGS), 
                                           Messages.PropEvaluator_isOverride,
                                           BooleanFieldEditor.DEFAULT,
                                           parent,
                                           "wrap",
                                           "If checked, then settings on this page are not applied - the default \n" +
                                           "winIDEA evaluator settings are used.");
        m_isOverrideWinIDEASettings.getCheckBoxControl().addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                enableComponents(((Button)e.getSource()).getSelection());
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        addField(m_isOverrideWinIDEASettings);
        
        m_isHexEditor = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_HEX), 
                                               Messages.PropEvaluator_isIntInHex,
                                               BooleanFieldEditor.DEFAULT,
                                               parent,
                                               "wrap",
                                               "If checked, integers are displayed in hex format."); 
        addField(m_isHexEditor);
        
        m_isDisplayMemArea = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_POINTER_MEM_AREA), 
                                                   Messages.PropEvaluator_isMemAreaInPtrProto,
                                                   BooleanFieldEditor.DEFAULT,
                                                   parent,
                                                   "wrap",
                                                   "If checked, memory area is displayed in pointer prototypes."); 
        addField(m_isDisplayMemArea);
        
        m_isCharArrayAsString = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_ARRAY_AS_STRING), 
                                                      Messages.PropEvaluator_isCharArrayAsStr,
                                                      BooleanFieldEditor.DEFAULT,
                                                      parent,
                                                     "wrap",
                                                     "If checked, char arrays are displayed as zero terminated strings.");
        addField(m_isCharArrayAsString);
        
        m_isDereferenceStringPointer = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DEREFERENCE_STRING_POINTERS), 
                                                             Messages.PropEvaluator_isDerefCharArray,
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "wrap",
                                       "If checked, dereferences 'char *' automatically.");
        addField(m_isDereferenceStringPointer);

        m_isDisplayArrayStructValue = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_COLLAPSED_ARRAY_STRUCT), 
                                                            Messages.PropEvaluator_isArrayAndStructValues,
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "wrap",
                                       "If checked, array and structured type values are displayed.");
        addField(m_isDisplayArrayStructValue);

        m_vagueFloatPrecision = new PrefStringEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_VAGUE_FLOAT_PRECISION), 
                                                     Messages.PropEvaluator_fpPrecision, 
                                     StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                     parent,
                                     "growx, wrap", 
                                     "Floating point numbers with difference less than this " +
                                     "value are considered equal in testIDEA expressions.");
        addField(m_vagueFloatPrecision);
        
        m_charDisplay = new PrefRadioGroupEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_DISPLAY),
                                          "Charaters format", 3,
                                          new String [][] {
                                              {Messages.PropEvaluator_ChrFmtASCII, 
                                                  CEvaluatorConfig.charDisplayEnum2Str(ECharDisplay.ECDAscii)}, 
                                              {Messages.PropEvaluator_ChrFmtInt, CEvaluatorConfig.charDisplayEnum2Str(ECharDisplay.ECDInteger)},
                                              {Messages.PropEvaluator_ChrFmtASCIIAndInt, CEvaluatorConfig.charDisplayEnum2Str(ECharDisplay.ECDBoth)}},
                                          parent,
                                          true,
                                          "gapright 25",
                                          "This setting defines display format for variables of type 'char'.",
                                          false, null);
        addField(m_charDisplay);

        m_isAnsiFormat = new PrefBooleanEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ANSI), 
                                               Messages.PropEvaluator_isCharANSI,
                                       BooleanFieldEditor.DEFAULT,
                                       parent,
                                       "wrap",
                                       "If checked, ANSI chars are displayed.");
        addField(m_isAnsiFormat);

        
        m_addressDisplay = new PrefRadioGroupEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ADDRESS_DISPLAY),
                                          "Address format", 3,
                                          new String [][] {
                                              {Messages.PropEvaluator_isHexFmtWOPrefix, 
                                                  CEvaluatorConfig.addressDisplayEnum2Str(EAddressDisplay.EADHexNoPrefix)}, 
                                              {Messages.PropEvaluator_isHexFmtWPrefix, 
                                                  CEvaluatorConfig.addressDisplayEnum2Str(EAddressDisplay.EADHexPrefix)}},
                                          parent,
                                          true,
                                          "wrap, gaptop 25",
                                          "This setting defines address format.",
                                          false, null);
        addField(m_addressDisplay);

        m_binaryDisplay = new PrefRadioGroupEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_BINARY_DISPLAY),
                                          "Binary format", 3,
                                          new String [][] {
                                              {Messages.PropEvaluator_isBinFmtWOB, 
                                                  CEvaluatorConfig.binaryDisplayEnum2Str(EBinaryDisplay.EBDBlanks)}, 
                                              {Messages.PropEvaluator_isBinFmtWB, 
                                                  CEvaluatorConfig.binaryDisplayEnum2Str(EBinaryDisplay.EBDNoBlanksTrailingB)}},
                                          parent,
                                          true,
                                          "wrap, gaptop 25",
                                          "This setting defines binary format.",
                                          false, null);
        addField(m_binaryDisplay);

        m_enumDisplay = new PrefRadioGroupEditor(m_sectionId2String.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ENUM_DISPLAY),
                                          "Enum format", 3,
                                          new String [][] {
                                              {Messages.PropEvaluator_EnumEnumOnly, CEvaluatorConfig.enumDisplayEnum2Str(EEnumDisplay.EEDEnum)}, 
                                              {Messages.PropEvaluator_EnumIntOnly, CEvaluatorConfig.enumDisplayEnum2Str(EEnumDisplay.EEDInteger)}, 
                                              {Messages.PropEvaluator_EnumEnumAndInt, CEvaluatorConfig.enumDisplayEnum2Str(EEnumDisplay.EEDBoth)}},
                                          parent,
                                          true,
                                          "wrap, gaptop 25",
                                          "This setting defines enum format.",
                                          false, null);
        addField(m_enumDisplay);


        IPreferenceStore prefStore = getPreferenceStore();
        boolean isOverrideWinIDEASettings;
        if (prefStore instanceof TestBasePreferenceStore) {
            // properties use ints as strings as keys
            isOverrideWinIDEASettings = prefStore.getBoolean(String.valueOf(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_IS_OVERRIDE_WINIDEA_SETTINGS.swigValue()));
        } else {
            // preferences use strings as keys
            isOverrideWinIDEASettings = prefStore.getBoolean(EVALUATOR_IS_OVERRIDE_WIN_IDEA_SETTINGS);
        }

        enableComponents(isOverrideWinIDEASettings);
        
        m_isOverrideWinIDEASettings.setEnabled(true, parent);
    }


    private void enableComponents(boolean isEnabled) {
        final Composite parent = getFieldEditorParent();
        m_isHexEditor.setEnabled(isEnabled, parent);
        m_isDisplayMemArea.setEnabled(isEnabled, parent);
        m_isCharArrayAsString.setEnabled(isEnabled, parent);
        m_isDereferenceStringPointer.setEnabled(isEnabled, parent);
        m_isDisplayArrayStructValue.setEnabled(isEnabled, parent);
        m_vagueFloatPrecision.setEnabled(isEnabled, parent);
        m_charDisplay.setEnabled(isEnabled, parent);
        m_isAnsiFormat.setEnabled(isEnabled, parent);
        m_addressDisplay.setEnabled(isEnabled, parent);
        m_binaryDisplay.setEnabled(isEnabled, parent);
        m_enumDisplay.setEnabled(isEnabled, parent);
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
