package si.isystem.itest.handlers;

import java.util.Arrays;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.dialogs.PropertyDialog;

import si.isystem.commons.connect.ConnectionPool;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CInitSequenceAction;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.EvaluatorPrefsPage;
import si.isystem.itest.preferences.GeneralPrefsPage;
import si.isystem.itest.preferences.InitSequencePrefsPage;
import si.isystem.itest.preferences.PreferenceInitializer;
import si.isystem.itest.preferences.RunPrefsPage;
import si.isystem.itest.preferences.ScriptPrefsPage;
import si.isystem.itest.preferences.StackUsagePrefsPage;
import si.isystem.itest.preferences.TargetConfigPrefsPage;
import si.isystem.itest.preferences.TestBasePreferenceStore;
import si.isystem.itest.preferences.TestCaseInitPrefsPage;
import si.isystem.itest.preferences.ToolsConfigPrefsPage;
import si.isystem.itest.ui.spec.data.ITestStatusLine;

/** Derive just to set custom dialog title (used to be Preferences, which is confusing). */ 
@SuppressWarnings("restriction")  // PropertyDialog is an internal class.
class TIPropertyDialog extends PropertyDialog {
    TIPropertyDialog(Shell parentShell, PreferenceManager mng, ISelection selection) {
        super(parentShell, mng, selection);
    }
    
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Project properties");
    }
}


@SuppressWarnings("restriction")  // PropertyDialog is an internal class.
public class FilePropertiesCmdHandler extends AbstractHandler {

    private static final String TEST_CASE_INIT__PREFS_PAGE_ID = "testCaseInitPrefsPage";
    private static final String EVALUATOR_PREFS_PAGE_ID = "evaluatorPrefsPage";
    private static final String RUN_CONFIG_PREFS_PAGE_ID = "runConfigPrefsPage";
    private static final String STACK_USAGE_CONFIG_PREFS_PAGE_ID = "stackUsageConfigPrefsPage";
    private static final String SCRIPT_PREFS_PAGE_ID = "scripPrefsPage";
    public static final String TOOLS_PREFS_PAGE_ID = "toolsPrefsPage";
    private static final String TARGET_CONFIG_PREFS_PAGE_ID = "targetConfigPrefsPage";
    public static final String ENV_PREFS_PAGE_ID = "envPrefsPage";
    private String m_defaultPageId;

    public FilePropertiesCmdHandler() {
        m_defaultPageId = null;
    }

    public FilePropertiesCmdHandler(String defaultPageId) {
        m_defaultPageId = defaultPageId;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            Shell shell = Activator.getShell();
            PreferenceInitializer prefInitializer = new PreferenceInitializer();
            
            PreferenceManager mgr = new PreferenceManager();
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
            TestBasePreferenceStore prefStore = new TestBasePreferenceStore(envConfig);
            TestBasePreferenceStore initSeqPrefStore = new TestBasePreferenceStore(envConfig);
            TestBasePreferenceStore runPrefStore = new TestBasePreferenceStore(envConfig);
            TestBasePreferenceStore targetConfigPrefStore = new TestBasePreferenceStore(envConfig); 
            TestBasePreferenceStore stackUsagePrefStore = new TestBasePreferenceStore(envConfig);
            TestBasePreferenceStore scriptPrefStore = new TestBasePreferenceStore(envConfig.getScriptConfig(false));
            TestBasePreferenceStore toolsPrefStore = new TestBasePreferenceStore(envConfig.getToolsConfig(false));
            TestBasePreferenceStore evaluatorPrefStore = new TestBasePreferenceStore(envConfig.getEvaluatorConfig(false));
            TestBasePreferenceStore testCaseinitPrefStore = new TestBasePreferenceStore(envConfig.getTestCaseTargetInitConfig(false));
            
            createEnvConfigPage(prefInitializer, mgr, prefStore);

            createInitSequencePage(prefInitializer, mgr, initSeqPrefStore, 
                                   model.getCEnvironmentConfiguration());

            createRunConfigPage(prefInitializer, mgr, runPrefStore); 
            
            createTargetConfigPage(prefInitializer, mgr, targetConfigPrefStore);
            
            createStackUsageConfigPage(prefInitializer, mgr, stackUsagePrefStore);

            createScriptConfigPage(prefInitializer, mgr, scriptPrefStore);
            
            createToolsConfigPage(prefInitializer, mgr, toolsPrefStore);
            
            createEvaluatorConfigPage(prefInitializer, mgr, evaluatorPrefStore);
                        
            createTestCaseInitConfigPage(prefInitializer, mgr, testCaseinitPrefStore);
            
            // the selection is not used in our case, but it must not be null
            ISelection selection = UiUtils.getStructuredSelection();
            PropertyDialog dialog = new TIPropertyDialog(shell, mgr, selection);
            dialog.create();
            dialog.setMessage("Project properties");
            
            if (m_defaultPageId != null) {
                dialog.setCurrentPageId(m_defaultPageId);
            }
            
            String workspace = envConfig.getWorkspace();
            String address = envConfig.getWorkspace();
            int port = envConfig.getPort();
            String[] coreIDs = model.getCoreIDs();
            
            if (dialog.open() == PropertyDialog.OK) {
                prefStore.save();
                initSeqPrefStore.save();
                runPrefStore.save();
                targetConfigPrefStore.save();
                stackUsagePrefStore.save();
                scriptPrefStore.save();
                toolsPrefStore.save();
                evaluatorPrefStore.save();
                testCaseinitPrefStore.save();
                
                // make input sanitation
                CTestBaseList initSeq = envConfig.getTestBaseList(CTestEnvironmentConfig.EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                                                  true);
                for (int idx = 0; idx < initSeq.size(); idx++) {
                    CInitSequenceAction action = CInitSequenceAction.cast(initSeq.get(idx));
                    action.setCoreId(action.getCoreId().trim());
                }
                
                JConnection jCon = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);
                
                // if connection related info changes, disconnect!
                if (jCon != null  &&  jCon.isConnected()  &&
                    (!workspace.equals(envConfig.getWorkspace())  ||
                     !address.equals(envConfig.getWorkspace())  ||
                     port != envConfig.getPort()  ||
                     !Arrays.equals(coreIDs, model.getCoreIDs()))) {
                    
                    jCon.disconnectAll();
                    Activator.getStatusLine().setMessage(ITestStatusLine.StatusImageId.DISCONNECTED,
                                                         "");
                    MessageDialog.openInformation(shell, 
                        "Disconnected!", 
                        "Connection information has changed (workspace, address, port, or core IDs),\n"
                        + "so testIDEA has been disconnected from winIDEA.");
                }

                GlobalsConfiguration.instance().getGlobalContainer().getCoreIdsGlobalsProvider().refreshGlobals();
            }
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not modify configuration!", ex);
        }

        return null;
    }


    private GeneralPrefsPage createEnvConfigPage(PreferenceInitializer prefInitializer,
                                                       PreferenceManager mgr,
                                                       IPreferenceStore prefStore) {
        
        // default values are the ones set by the user in preferences
        prefInitializer.initializeEnvConfigPreferences(prefStore, true);

        GeneralPrefsPage page = new GeneralPrefsPage(prefStore);
        
        page.setTitle("General");
        IPreferenceNode node = new PreferenceNode(ENV_PREFS_PAGE_ID, page);
        mgr.addToRoot(node);
        return page;
    }


    static void createTargetConfigPage(PreferenceInitializer prefInitializer,
                                       PreferenceManager mgr,
                                       IPreferenceStore prefStore) {
        // default values are the ones set by the user in preferences
        prefInitializer.initializeMulticorePreferences(prefStore, true);

        TargetConfigPrefsPage scriptsPage = new TargetConfigPrefsPage(prefStore);

        scriptsPage.setTitle("Multicore configuration");
        IPreferenceNode node = new PreferenceNode(TARGET_CONFIG_PREFS_PAGE_ID, scriptsPage);
        mgr.addToRoot(node);
    }


    static void createScriptConfigPage(PreferenceInitializer prefInitializer,
                                        PreferenceManager mgr,
                                        IPreferenceStore scriptPrefStore) {
        // default values are the ones set by the user in preferences
        prefInitializer.initializeScriptConfigPreferences(scriptPrefStore, true);
        
        ScriptPrefsPage scriptsPage = new ScriptPrefsPage(scriptPrefStore);
        
        scriptsPage.setTitle("Scripts");
        IPreferenceNode node = new PreferenceNode(SCRIPT_PREFS_PAGE_ID, scriptsPage);
        mgr.addToRoot(node);
    }

    
    static void createToolsConfigPage(PreferenceInitializer prefInitializer,
                                     PreferenceManager mgr,
                                     IPreferenceStore scriptPrefStore) {
       // default values are the ones set by the user in preferences
       prefInitializer.initializeToolsConfigPreferences(scriptPrefStore, true);
       
       ToolsConfigPrefsPage scriptsPage = new ToolsConfigPrefsPage(scriptPrefStore);
       
       scriptsPage.setTitle("Tools configuration");
       IPreferenceNode node = new PreferenceNode(TOOLS_PREFS_PAGE_ID, scriptsPage);
       mgr.addToRoot(node);
   }

   
    public static InitSequencePrefsPage createInitSequencePage(PreferenceInitializer prefInitializer,
                                                      PreferenceManager mgr,
                                                      IPreferenceStore prefStore,
                                                      CTestEnvironmentConfig testConfig) {
        
        // default values are the ones set by the user in preferences
        prefInitializer.initializeInitSequencePreferences(prefStore, true);

        InitSequencePrefsPage page = new InitSequencePrefsPage(prefStore, testConfig);
        
        page.setTitle("Initialization sequence");
        IPreferenceNode node = new PreferenceNode(RUN_CONFIG_PREFS_PAGE_ID, page);
        mgr.addToRoot(node);
        return page;
    }

    
    public static RunPrefsPage createRunConfigPage(PreferenceInitializer prefInitializer,
                                                   PreferenceManager mgr,
                                                   IPreferenceStore prefStore) {
        
        // default values are the ones set by the user in preferences
        prefInitializer.initializeRunConfigPreferences(prefStore, true);

        RunPrefsPage page = new RunPrefsPage(prefStore);
        
        page.setTitle("Run configuration");
        IPreferenceNode node = new PreferenceNode(RUN_CONFIG_PREFS_PAGE_ID, page);
        mgr.addToRoot(node);
        return page;
    }

    
    static StackUsagePrefsPage createStackUsageConfigPage(PreferenceInitializer prefInitializer,
                                                   PreferenceManager mgr,
                                                   IPreferenceStore prefStore) {
       
       // default values are the ones set by the user in preferences
       prefInitializer.initializeStackUsageConfigPreferences(prefStore, true);

       StackUsagePrefsPage page = new StackUsagePrefsPage(prefStore);
       
       page.setTitle("Stack usage");
       IPreferenceNode node = new PreferenceNode(STACK_USAGE_CONFIG_PREFS_PAGE_ID, page);
       mgr.addToRoot(node);
       return page;
   }

   
    static EvaluatorPrefsPage createEvaluatorConfigPage(PreferenceInitializer prefInitializer,
                                                         PreferenceManager mgr,
                                                         TestBasePreferenceStore prefStore) {
        
        // default values are the ones set by the user in preferences
        prefInitializer.initializeEvaluatorConfigPreferences(prefStore, true);

        EvaluatorPrefsPage page = new EvaluatorPrefsPage(prefStore);
        
        page.setTitle("winIDEA evaluator");
        IPreferenceNode node = new PreferenceNode(EVALUATOR_PREFS_PAGE_ID, page);
        mgr.addToRoot(node);
        return page;
    }

    
    static TestCaseInitPrefsPage createTestCaseInitConfigPage(PreferenceInitializer prefInitializer,
                                                           PreferenceManager mgr,
                                                           TestBasePreferenceStore prefStore) {
       
       // default values are the ones set by the user in preferences
       prefInitializer.initializeTestCaseInitConfigPreferences(prefStore, true);

       TestCaseInitPrefsPage page = new TestCaseInitPrefsPage(prefStore);
       
       page.setTitle("Target Initialization Before Each Test Case");
       IPreferenceNode node = new PreferenceNode(TEST_CASE_INIT__PREFS_PAGE_ID, page);
       mgr.addToRoot(node);
       return page;
   }



    
}
