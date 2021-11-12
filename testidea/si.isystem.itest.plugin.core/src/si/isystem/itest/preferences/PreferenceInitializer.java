package si.isystem.itest.preferences;

import java.util.Map;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import si.isystem.connect.CEvaluatorConfig;
import si.isystem.connect.CEvaluatorConfig.EAddressDisplay;
import si.isystem.connect.CEvaluatorConfig.EBinaryDisplay;
import si.isystem.connect.CEvaluatorConfig.ECharDisplay;
import si.isystem.connect.CEvaluatorConfig.EEnumDisplay;
import si.isystem.connect.CEvaluatorConfig.ETestEvaluatorConfigSectionIds;
import si.isystem.connect.CScriptConfig.ETestScriptConfigSectionIds;
import si.isystem.connect.CTestCaseTargetInitConfig.ETestCaseTargetInitSectionIds;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.connect.CToolsConfig.EToolsConfigSections;
import si.isystem.itest.main.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {


    private static final String DEFAULT_FLOAT_PRECISION = "1e-5";
    private static final String DEFAULT_ANALYZER_FILE_NAME = "${_testId}-${_function}.trd";
    private static final String DEFAULT_SCRIPT_TIMEOUT_IN_SECONDS = "100";
    private static final String DEFAULT_BREAKPOINT_TYPE = "keepWinIDEASetting";
    private static final String DEFAULT_TEST_ID_FORMAT = "${_uid}";
    private static final String DEFAULT_RETURN_VALUE_NAME = "rv";


    /*
	 * Called by RCP framework before instantiating preferences pages, and when
	 * the model is cleared. 
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
    public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		Map<EEnvConfigSections, String> map = GeneralPrefsPage.mappingForPreferences();
		// most users test functions from a single download file
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_USE_QUALIFIED_FUNC_NAME), 
                         false);
		store.setDefault(map.get(EEnvConfigSections.E_SECTION_DEFAULT_RET_VAL_NAME), 
		                 DEFAULT_RETURN_VALUE_NAME);
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_AUTO_ID_FORMAT_STRING), 
                         DEFAULT_TEST_ID_FORMAT);
		
        // It is not a good user experience if 
        // app starts to complain before it even started if cant't start winIDEA
        // or elf is not downloaded. 
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_IS_AUTO_CONNECT), 
                                 false);
        
        // init sequence
        map = InitSequencePrefsPage.mappingForPreferences();
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ), 
                         true);
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_INIT_SEQUENCE), 
                         "");
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN), 
                         true);
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN), 
                         false);
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS), 
                         false);

        // run config
        map = RunPrefsPage.mappingForPreferences();
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_TEST_TIMEOUT), 
                         "");
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE), 
                         DEFAULT_BREAKPOINT_TYPE);

        // multicore config
        map = TargetConfigPrefsPage.mappingForPreferences();
        store.setDefault(map.get(EEnvConfigSections.E_SECTION_CORE_IDS), 
                         "");
        
        // stack usage config
        Map<EEnvConfigSections, String> stackUsageMap = 
                         StackUsagePrefsPage.mappingForStackUsagePreferences();
        store.setDefault(stackUsageMap.get(EEnvConfigSections.E_SECTION_STACK_USAGE),
                         "");
        
        // script config
        Map<ETestScriptConfigSectionIds, String> scriptMap = ScriptPrefsPage.mappingForPreferences();
        store.setDefault(scriptMap.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_MODULES), 
                         "");
        store.setDefault(scriptMap.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_EXTENSION_CLASS), 
                         "");
        store.setDefault(scriptMap.get(ETestScriptConfigSectionIds.E_SCRIPT_SECTION_TIMEOUT), 
                         DEFAULT_SCRIPT_TIMEOUT_IN_SECONDS);

        // tools config
        Map<EToolsConfigSections, String> toolsMap = ToolsConfigPrefsPage.mappingForPreferences();
        store.setDefault(toolsMap.get(EToolsConfigSections.E_SECTION_IS_AUTO_SET_ANALYZER_FNAME), 
                         true);
        store.setDefault(toolsMap.get(EToolsConfigSections.E_SECTION_ANALYZER_FNAME), 
                         DEFAULT_ANALYZER_FILE_NAME);
        store.setDefault(toolsMap.get(EToolsConfigSections.E_SECTION_IS_SET_TEST_ID_ON_PASTE), 
                         true);
        
        // evaluator config
        Map<ETestEvaluatorConfigSectionIds, String> evaluatorMap = EvaluatorPrefsPage.mappingForPreferences();
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_IS_OVERRIDE_WINIDEA_SETTINGS), 
                         true);
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_DISPLAY), 
                CEvaluatorConfig.charDisplayEnum2Str(ECharDisplay.ECDBoth));
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ANSI), 
                         true);
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_HEX), 
                         true);
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_BINARY_DISPLAY), 
                         CEvaluatorConfig.binaryDisplayEnum2Str(EBinaryDisplay.EBDBlanks));
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_POINTER_MEM_AREA), 
                         true);
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_ARRAY_AS_STRING), 
                         true);
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DEREFERENCE_STRING_POINTERS), 
                         true);
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ADDRESS_DISPLAY), 
                         CEvaluatorConfig.addressDisplayEnum2Str(EAddressDisplay.EADHexPrefix));
        
        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ENUM_DISPLAY), 
                         CEvaluatorConfig.enumDisplayEnum2Str(EEnumDisplay.EEDBoth));

        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_COLLAPSED_ARRAY_STRUCT), 
                         true);

        store.setDefault(evaluatorMap.get(ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_VAGUE_FLOAT_PRECISION), 
                         DEFAULT_FLOAT_PRECISION);
        
        // init before test case
        Map<ETestCaseTargetInitSectionIds, String> testCaseInitMap = TestCaseInitPrefsPage.mappingForPreferences();
        
        store.setDefault(testCaseInitMap.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_DL_ON_TC_INIT), 
                         false);
        store.setDefault(testCaseInitMap.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RESET_ON_TC_INIT), 
                         false);
        store.setDefault(testCaseInitMap.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RUN_ON_TC_INIT), 
                         false);
        store.setDefault(testCaseInitMap.get(ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_STOP_FUNC_ON_TC_INIT), 
                         "");
	}

	
    /**
     * Copies settings from preferences to the given project configuration.
     * Values or defaults are set, depending on the flag <code>isSetDefault</code>.
     *  
     * @param projCfgPrefStore project configuration
     * @param isSetDefault if true, the given values are used for setting defaults,
     *                     otherwise they are used to set values in the destination prefs.
     */
    public void initializeEnvConfigPreferences(IPreferenceStore projConfig,
                                               boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<EEnvConfigSections, String> prefsMap = GeneralPrefsPage.mappingForPreferences();
        Map<EEnvConfigSections, String> projMap = GeneralPrefsPage.mappingForProperties();
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        EEnvConfigSections.E_SECTION_WINIDEA_WORKSPACE_FILE_NAME, isSetDefault);

        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_USE_QUALIFIED_FUNC_NAME, isSetDefault);

        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        EEnvConfigSections.E_SECTION_WINIDEA_ADDRESS, isSetDefault);
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap,
                        
                        EEnvConfigSections.E_SECTION_WINIDEA_PORT, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        EEnvConfigSections.E_SECTION_DEFAULT_RET_VAL_NAME, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        EEnvConfigSections.E_SECTION_IS_AUTO_CONNECT, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        EEnvConfigSections.E_SECTION_LOG_PARAMETERS, isSetDefault);
         
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_AUTO_ID_FORMAT_STRING, isSetDefault);
    }
    
    
    /**
     * Copies settings from preferences to the given project configuration.
     * Values or defaults are set, depending on the flag <code>isSetDefault</code>.
     *  
     * @param projCfgPrefStore project configuration
     * @param isSetDefault if true, the given values are used for setting defaults,
     *                     otherwise they are used to set values in the destination prefs.
     */
    public void initializeScriptConfigPreferences(IPreferenceStore projConfig,
                                                  boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<ETestScriptConfigSectionIds, String> prefsMap = ScriptPrefsPage.mappingForPreferences();
        Map<ETestScriptConfigSectionIds, String> projMap = ScriptPrefsPage.mappingForProperties();
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        ETestScriptConfigSectionIds.E_SCRIPT_SECTION_WORKING_DIR, isSetDefault);

        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        ETestScriptConfigSectionIds.E_SCRIPT_SECTION_SYS_PATH, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        ETestScriptConfigSectionIds.E_SCRIPT_SECTION_MODULES, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        ETestScriptConfigSectionIds.E_SCRIPT_SECTION_EXTENSION_CLASS, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     ETestScriptConfigSectionIds.E_SCRIPT_SECTION_TIMEOUT, isSetDefault);
        
    }

    
    public void initializeToolsConfigPreferences(IPreferenceStore projConfig,
                                                 boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<EToolsConfigSections, String> prefsMap = ToolsConfigPrefsPage.mappingForPreferences();
        Map<EToolsConfigSections, String> projMap = ToolsConfigPrefsPage.mappingForProperties();
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     EToolsConfigSections.E_SECTION_IS_AUTO_SET_ANALYZER_FNAME, isSetDefault);

        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     EToolsConfigSections.E_SECTION_ANALYZER_FNAME, isSetDefault);

        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     EToolsConfigSections.E_SECTION_IS_SET_TEST_ID_ON_PASTE, isSetDefault);
    }


    /**
     * Copies settings from preferences to the given project configuration.
     * Values or defaults are set, depending on the flag <code>isSetDefault</code>.
     *  
     * @param projCfgPrefStore project configuration
     * @param isSetDefault if true, the given values are used for setting defaults,
     *                     otherwise they are used to set values in the destination prefs.
     */
    public void initializeInitSequencePreferences(IPreferenceStore projCfgPrefStore,
                                                  boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<EEnvConfigSections, String> prefsMap = InitSequencePrefsPage.mappingForPreferences();
        Map<EEnvConfigSections, String> projMap = InitSequencePrefsPage.mappingForProperties();
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                        EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ, isSetDefault);
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_INIT_SEQUENCE, isSetDefault);
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN, isSetDefault);
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN, isSetDefault);
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS, isSetDefault);
    }
    

    public void initializeRunConfigPreferences(IPreferenceStore projCfgPrefStore,
                                               boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<EEnvConfigSections, String> prefsMap = RunPrefsPage.mappingForPreferences();
        Map<EEnvConfigSections, String> projMap = RunPrefsPage.mappingForProperties();
        
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_TEST_TIMEOUT, isSetDefault);
        
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE, isSetDefault);

    }
    
    public void initializeMulticorePreferences(IPreferenceStore projCfgPrefStore,
                                               boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<EEnvConfigSections, String> prefsMap = TargetConfigPrefsPage.mappingForPreferences();
        Map<EEnvConfigSections, String> projMap = TargetConfigPrefsPage.mappingForProperties();
        setPrefValue(projCfgPrefStore, rcpPrefs, prefsMap, projMap, 
                     EEnvConfigSections.E_SECTION_CORE_IDS, isSetDefault);
    }
    
    
    public void initializeStackUsageConfigPreferences(IPreferenceStore projCfgPrefStore,
                                                      boolean isSetDefault) {
        
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        Map<EEnvConfigSections, String> stackUsagePrefsMap = StackUsagePrefsPage.mappingForStackUsagePreferences();
        Map<EEnvConfigSections, String> stackUsageProjMap = StackUsagePrefsPage.mappingForStackUsageProperties();

        setPrefValue(projCfgPrefStore, rcpPrefs, stackUsagePrefsMap, stackUsageProjMap, 
                     EEnvConfigSections.E_SECTION_STACK_USAGE, isSetDefault);
    }
    

    public void initializeEvaluatorConfigPreferences(TestBasePreferenceStore prefStore,
                                                     boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<ETestEvaluatorConfigSectionIds, String> prefsMap = EvaluatorPrefsPage.mappingForPreferences();
        Map<ETestEvaluatorConfigSectionIds, String> projMap = EvaluatorPrefsPage.mappingForProperties();
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_IS_OVERRIDE_WINIDEA_SETTINGS, 
                     isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_DISPLAY, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ANSI, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_HEX, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_BINARY_DISPLAY, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_POINTER_MEM_AREA, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_CHAR_ARRAY_AS_STRING, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DEREFERENCE_STRING_POINTERS, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ADDRESS_DISPLAY, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_ENUM_DISPLAY, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_DISPLAY_COLLAPSED_ARRAY_STRUCT, isSetDefault);
        setPrefValue(prefStore, rcpPrefs, prefsMap, projMap,
                     ETestEvaluatorConfigSectionIds.E_EVALUATOR_SECTION_VAGUE_FLOAT_PRECISION, isSetDefault);
    }


    /**
     * Copies settings from preferences to the given project configuration.
     * Values or defaults are set, depending on the flag <code>isSetDefault</code>.
     *  
     * @param projCfgPrefStore project configuration
     * @param isSetDefault if true, the given values are used for setting defaults,
     *                     otherwise they are used to set values in the destination prefs.
     */
    public void initializeTestCaseInitConfigPreferences(IPreferenceStore projConfig,
                                                        boolean isSetDefault) {
        IPreferenceStore rcpPrefs = Activator.getDefault().getPreferenceStore();
        
        Map<ETestCaseTargetInitSectionIds, String> prefsMap = TestCaseInitPrefsPage.mappingForPreferences();
        Map<ETestCaseTargetInitSectionIds, String> projMap = TestCaseInitPrefsPage.mappingForProperties();
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                        ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_DL_ON_TC_INIT, isSetDefault);

        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RESET_ON_TC_INIT, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_IS_RUN_ON_TC_INIT, isSetDefault);
        
        setPrefValue(projConfig, rcpPrefs, prefsMap, projMap, 
                     ETestCaseTargetInitSectionIds.E_TEST_CASE_SECTION_STOP_FUNC_ON_TC_INIT, isSetDefault);
    }


    private <T> void setPrefValue(IPreferenceStore projConfig,
                                  IPreferenceStore rcpPrefs,
                                  Map<T, String> prefsMap,
                                  Map<T, String> projMap,
                                  T sectionId,
                                  boolean isSetDefault) {
        
        if (isSetDefault) {
            projConfig.setDefault(projMap.get(sectionId), 
                                  rcpPrefs.getString(prefsMap.get(sectionId)));
        } else {
            projConfig.setValue(projMap.get(sectionId), 
                                rcpPrefs.getString(prefsMap.get(sectionId)));
        }
    }
}
