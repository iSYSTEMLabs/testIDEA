package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestCaseTargetInitConfig.ETestCaseTargetInitSectionIds;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.itest.preferences.controls.PrefStringEditor;
import si.isystem.ui.utils.KGUIBuilder;

public class TestCaseInitPrefsPage extends FieldEditorPreferencePage
                                   implements IWorkbenchPreferencePage {

    private Composite m_fieldEditorPanel;
    private Map<ETestCaseTargetInitSectionIds, String> m_sectionId2String;


    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public TestCaseInitPrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();

        setPreferenceStore(prefStore);

        setDescription("Settings on this page define target initialization steps\n" +
                "to be executed before EACH test case execution.");
    }


    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public TestCaseInitPrefsPage() {
        super(GRID);
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForPreferences();

        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(preferenceStore);

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
    public static Map<ETestCaseTargetInitSectionIds, String> mappingForPreferences() {

        Map<ETestCaseTargetInitSectionIds, String> sectionId2String = 
                new TreeMap<ETestCaseTargetInitSectionIds, String>();

        sectionId2String.put(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_DL_ON_TC_INIT, "runTC.isDownloadOnTCInit");
        sectionId2String.put(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RESET_ON_TC_INIT, "runTC.isResetOnTCInit");
        sectionId2String.put(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RUN_ON_TC_INIT, "runTC.isRunOnTCInit");
        sectionId2String.put(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_STOP_FUNC_ON_TC_INIT, "runTC.stopFunctionOnTCInit");

        return sectionId2String;
    }


    /**
     * This method should be called, when this class is used as properties page 
     * of the project or iyaml file resource. Section ids are used in this case, 
     * which modify setting in the currently opened project - they are saved
     * into iyaml file. 
     * @return 
     */
    public static Map<ETestCaseTargetInitSectionIds, String> mappingForProperties() {

        Map<ETestCaseTargetInitSectionIds, String> sectionId2String = 
                new TreeMap<ETestCaseTargetInitSectionIds, String>();

        putEnumAsIntStr(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_DL_ON_TC_INIT, sectionId2String);
        putEnumAsIntStr(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RESET_ON_TC_INIT, sectionId2String);
        putEnumAsIntStr(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RUN_ON_TC_INIT, sectionId2String); 
        putEnumAsIntStr(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_STOP_FUNC_ON_TC_INIT, sectionId2String); 

        return sectionId2String;
    }

    private static void putEnumAsIntStr(ETestCaseTargetInitSectionIds id, 
                                        Map<ETestCaseTargetInitSectionIds, String> sectionId2String) {
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

        createFieldEditors();

        initialize();
        checkState();

        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        KGUIBuilder builder = new KGUIBuilder(parent);

        KGUIBuilder initGroupParent = builder.group("Init sequence for each test case", 
                                              "wrap", 
                                              true, "fillx", "[fill]", "");
        
        // create another composite, as Prefs child components get font from
        // the parent, so they would be all shown in bold
        KGUIBuilder initGroup = initGroupParent.newPanel("", "", SWT.NONE);

        initGroup.label("Operations selected in this group are executed in the same " +
                "order as they appear below.", "span 2, gapbottom 5, wrap");

        addField(new PrefBooleanEditor(m_sectionId2String.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_DL_ON_TC_INIT), 
                                       "&Download",
                                       BooleanFieldEditor.DEFAULT,
                                       initGroup.getParent(),
                                       "wrap",
                "If checked, download is performed before test execution."));


        addField(new PrefBooleanEditor(m_sectionId2String.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RESET_ON_TC_INIT), 
                                       "&Reset",
                                       BooleanFieldEditor.DEFAULT,
                                       initGroup.getParent(),
                                       "wrap",
                "If checked, the target is reset before each test case."));


        PrefBooleanEditor runEditor = new PrefBooleanEditor(m_sectionId2String.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RUN_ON_TC_INIT), 
                                                            "R&un",
                                                            BooleanFieldEditor.DEFAULT,
                                                            initGroup.getParent(),
                                                            "wrap",
                "If checked, target is started before each test case.");

        runEditor.getCheckBoxControl();

        addField(runEditor);


        PrefStringEditor stopOnFunctionField = 
                new PrefStringEditor(m_sectionId2String.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_STOP_FUNC_ON_TC_INIT), 
                                     "&Stop on function:", 
                                     StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                     initGroup.getParent(),
                                     "growx, wrap", 
                                     "Name of a C function, where the target " +
                                     "execution should stop before running test case.\nUsually it is set to 'main'.");
        
        Text stopOnFunctionTxt = stopOnFunctionField.getTextControl(initGroup.getParent());

        GlobalsProvider funcGlobalsProvider = GlobalsConfiguration.instance().
                                 getGlobalContainer().getAllFunctionsProvider(); 

        ISysUIUtils.addContentProposalsAdapter(stopOnFunctionTxt, 
                                               funcGlobalsProvider.getCachedGlobals(), 
                                               funcGlobalsProvider.getCachedDescriptions(), 
                                               ContentProposalAdapter.PROPOSAL_REPLACE,
                                               UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        
        /* new AsystAutoCompleteField(stopOnFunctionTxt, 
                                    new AsystTextContentAdapter(), 
                                    funcGlobalsProvider.getCachedGlobals(), 
                                    funcGlobalsProvider.getCachedDescriptions(), 
                                    ContentProposalAdapter.PROPOSAL_REPLACE);
*/
        addField(stopOnFunctionField);
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
