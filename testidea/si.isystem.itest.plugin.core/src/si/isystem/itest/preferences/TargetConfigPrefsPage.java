package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.PrefListEditor;

public class TargetConfigPrefsPage extends FieldEditorPreferencePage
                                   implements IWorkbenchPreferencePage{

    private Composite m_fieldEditorPanel;
    private Map<EEnvConfigSections, String> m_sectionId2String;

    class CoreIdValidator implements IPropertyChangeListener 
    {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            System.out.println("newValue: " + event.getNewValue());
            
        }
        
    }    
    
    /**
     * This ctor initializes the page from testIDEA model - it is called
     * when user edits project properties.
     */
    public TargetConfigPrefsPage(IPreferenceStore prefStore) {
        super(GRID);

        // default ID mapping used in preferences page
        m_sectionId2String = mappingForProperties();
        
        setPreferenceStore(prefStore);
        
        setDescription("Settings on this page define target core configuration. " +
                       "They are saved to project file.\n" +
                       "This page can be accessed with command 'File | Properties'.");
    }
    

    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public TargetConfigPrefsPage() {
        super(GRID);
        // default ID mapping used in preferences page
        m_sectionId2String = mappingForPreferences();
        
        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(preferenceStore);
        
        setDescription("Specify target configuration, which will be " +
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
        
        sectionId2String.put(EEnvConfigSections.E_SECTION_CORE_IDS, "targetConfig.coreIds");

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
        
        putEnumAsIntStr(EEnvConfigSections.E_SECTION_CORE_IDS, sectionId2String);
        
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
        
        m_fieldEditorPanel.setLayout(new MigLayout("fill", "[min!][fill][min!]",
                                                   "[min!][fill]"));
        
        createFieldEditors();

        initialize();
        checkState();
        
        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // see the doc of FieldEditor class for the list of all available field editors 
        final PrefListEditor coreIds = new PrefListEditor(m_sectionId2String.get(EEnvConfigSections.E_SECTION_CORE_IDS), 
                                    "&Core IDs:",
                                    StringFieldEditor.VALIDATE_ON_KEY_STROKE,
                                    parent,
                                    "gaptop 10, gapbottom 10, growx",
                                    "Enter core IDs separated with commas.\n" +
                                    "Order is important - core ID index equals core index in winIDEA (see menu Debug | Core).\n" +
                                    "The same restriction as for test ID apply - only letters, numbers and symbols _-./: are allowed.\n" +
                                    "Example:\n\n    core-0, core-1");
        addField(coreIds);
        
        // This button provides easy way to get core IDs from winIDEA. However,
        // user may still specify his own IDs. The use case for this is when
        // the same code is used on two different targets which have different 
        // core IDs in winIDEA. If testIDE would use winIDEA core IDs unconditionally,
        // then core ID in functions would not match for the second target. Furthermore,
        // this way it is possible to specify test cases without winIDEA running.
        // And last but not least, multicore support in winIDEA is not finished yet, so API will change.
        Button btn = new Button(parent, SWT.PUSH);
        btn.setLayoutData("w min:min:min, gaptop 10, gapbottom 10, wrap");
        btn.setText("Get from winIDEA");
        btn.setToolTipText("Reads names of cores as defined in winIDEA in main menu 'Debug | Core'.");
        btn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                JConnection connection = ConnectionProvider.instance().getDefaultConnection();
                CIDEController ide = connection.getCIDEController("");
                String ids = ide.serviceCall("/IDE/GetCoreInfo", "");
                
                // example of string to be parsed:
                // Core: , Core[0].Name: TC277TF, Core[1].Name: CPU1, Core[2].Name: CPU2
                int idx = ids.indexOf("Core[0]");
                if (idx < 0) {
                    MessageDialog.openError(Activator.getShell(), "Can not get core Ids from winIDEA", 
                                            "Error when parsing string: " + ids);
                }
                ids = ids.substring(idx);  // removes prefix 'Core: , '
                ids = ids.replaceAll("Core\\[\\d+\\]\\.Name: ", ""); // removes everything but core names
                coreIds.setStringValue(ids);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
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
