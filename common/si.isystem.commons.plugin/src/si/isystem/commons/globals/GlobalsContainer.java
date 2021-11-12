package si.isystem.commons.globals;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CTestGlobalsContainer;
import si.isystem.exceptions.SIllegalArgumentException;

/**
 * This class maintains collection of global providers. Its main advantage
 * is common refresh method for all target globals.
 * 
 * Note: Keeping global symbols storage for all opened editors will behave strangely,
 * when there are connections made to several winIDEAs, possibly with different
 * projects - symbols will be valid only for iyaml file, which was active when Refresh 
 * operation was executed last. However, since this is unlikely scenario, and 
 * refreshing symbols for each iyaml file is not user friendly, we keep symbols 
 * globally. It is much more likely that user has opened several iyaml files
 * connected to the same winIDEA than to several winIDEAs. 
 * Proper solution: Maintain symbols in map with winIDEA connection as key. 
 * 
 * @author markok
 */
public class GlobalsContainer {

    // Constants for accessing containers. All constants predefined in this
    // class start with '_'. Custom defined constants should NOT start with '_'.
    // Providers for tags and testIDs should be added by client application,
    // see si.isystem.itest.common.GlobalsConfiguration for an example.
    public static final String GC_TAGS = "_tags";
    public static final String GC_TEST_IDS = "_testIds";
    public static final String GC_CORE_IDS = "_coreIds";

    /** Note: HIL parameters are in general independent of core (bound to emulator).
     * However, if testIDEA will be expanded to support connections to
     * multiple winiDEAs each one with its own HW, then this data structure
     * should be core dependent.
     */
    public static final String GC_HIL = "_hil";

    private static final String GC_ALL_PARTITIONS = "_allPartitions";
    public static final String GC_ALL_MODULES = "_allModules";
    public static final String GC_ALL_FUNCTIONS = "_allFunctions";
    public static final String GC_ALL_VARS = "_allVars";
    public static final String GC_ALL_TYPES = "_allTypes";

    public static final String GC_CODE_LABELS = "_codeLabels";

    /** This one does not need connection - it is intended to be used for
     * specific sets of proposals for which it makes no sense to create
     * global provider.
     */
    public static final String GC_CUSTOM_PROVIDER = "_custom";

    // globals specific for each core/app
    public static final String GC_PARTITIONS = "_partitions";
    public static final String GC_MODULES = "_modules";
    public static final String GC_FUNCTIONS = "_functions";
    public static final String GC_VARS = "_vars";
    public static final String GC_VARS_AND_MACROS = "_vars_and_macros";
    public static final String GC_TYPES = "_types";

    private Map<String, GlobalsProvider> m_providers = new TreeMap<>();
    // this should be the only instance in testIDEA!
    private CTestGlobalsContainer m_cGlobalsContainer = new CTestGlobalsContainer();
    
    /**
     * This class contains providers, which contain globals specific for a core
     * or application.
     */
    private class CoreGlobalsProvider {
        
        Map<String, GlobalsProvider> m_coreProviders = new TreeMap<>();
        
        public void addCoreProvider(String providerId, GlobalsProvider provider) {
            m_coreProviders.put(providerId, provider);
        }
        
        
        public GlobalsProvider getCoreProvider(String providerId) {
            return m_coreProviders.get(providerId);
        }

        
        void refresh() {
            for (Map.Entry<String, GlobalsProvider> pair : m_coreProviders.entrySet()) {
                pair.getValue().refreshGlobals();
            }
        }
    }

    
    private Map<String, CoreGlobalsProvider> m_coreProviders = new TreeMap<>();
    
    private GlobalsProvider m_emptyGlobalsProvider;

    private String m_primaryCoreId = "";
    private IGlobalsConfiguration m_globalsCfg;
    private IConnectionProvider m_conProvider;

    
    
    public GlobalsContainer(IGlobalsConfiguration globalsCfg, IConnectionProvider conProvider) {
        m_globalsCfg = globalsCfg;
        m_conProvider = conProvider;
        m_emptyGlobalsProvider = new GlobalsProvider(m_conProvider, null);
    }


    private void createCoreSpecificProviders() {

        String[] coreIds;
        if (m_globalsCfg != null) {
            coreIds = m_globalsCfg.getCoreIds();
        } else {
            coreIds = new String[]{""};
        }

        m_primaryCoreId = coreIds[0];
        String defaultDlFile = m_emptyGlobalsProvider.getDefaultDownloadFileName();
        
        AggregateGlobalsProvider allModulesGP = new AggregateGlobalsProvider();
        AggregateGlobalsProvider allFuncGP = new AggregateGlobalsProvider();
        AggregateGlobalsProvider allVarsGP = new AggregateGlobalsProvider();
        AggregateGlobalsProvider allTypesGP = new AggregateGlobalsProvider();
        AggregateGlobalsProvider allPartitionsGP = new AggregateGlobalsProvider();
        
        for (String coreId : coreIds) {
            
            CoreGlobalsProvider cgp = new CoreGlobalsProvider();
            FunctionGlobalsProvider funcProvider = new FunctionGlobalsProvider(m_conProvider, 
                                                                               coreId, 
                                                                               defaultDlFile, 
                                                                               m_globalsCfg);
            cgp.addCoreProvider(GC_FUNCTIONS, funcProvider);
            cgp.addCoreProvider(GC_VARS, new VariablesGlobalsProvider(m_conProvider, coreId, m_globalsCfg));
            cgp.addCoreProvider(GC_VARS_AND_MACROS, new VarsAndMacrosGlobalsProvider(m_conProvider,  coreId, m_globalsCfg));
            cgp.addCoreProvider(GC_TYPES, new TypesGlobalsProvider(m_conProvider,  coreId, funcProvider));
            cgp.addCoreProvider(GC_MODULES, new ModulesGlobalsProvider(m_conProvider, coreId));
            cgp.addCoreProvider(GC_PARTITIONS, new PartitionsGlobalsProvider(m_conProvider, coreId));
            cgp.refresh();
            
            m_coreProviders.put(coreId, cgp);
            
            allPartitionsGP.addGlobalsProvider(cgp.getCoreProvider(GC_PARTITIONS));
            allModulesGP.addGlobalsProvider(cgp.getCoreProvider(GC_MODULES));
            allFuncGP.addGlobalsProvider(cgp.getCoreProvider(GC_FUNCTIONS));
            allVarsGP.addGlobalsProvider(cgp.getCoreProvider(GC_VARS));
            allTypesGP.addGlobalsProvider(cgp.getCoreProvider(GC_TYPES));
        }
        
        m_providers.put(GC_ALL_PARTITIONS, allPartitionsGP);
        m_providers.put(GC_ALL_MODULES, allModulesGP);
        m_providers.put(GC_ALL_FUNCTIONS, allFuncGP);
        m_providers.put(GC_ALL_VARS, allVarsGP);
        m_providers.put(GC_ALL_TYPES, allTypesGP);
    }
    
    
    private void createGeneralProviders() {
   
        HilGlobalsProvider hilGP = new HilGlobalsProvider(m_conProvider);
        LabelsGlobalsProvider labelsGP = new LabelsGlobalsProvider(m_conProvider);
        CoreIdGlobalsProvider coreIDsGP = new CoreIdGlobalsProvider(m_globalsCfg);
        
        m_providers.put(GC_HIL, hilGP);
        m_providers.put(GC_CODE_LABELS, labelsGP);
        m_providers.put(GC_CORE_IDS, coreIDsGP);
        m_providers.put(GC_CUSTOM_PROVIDER, new CustomGlobalsProvider());
    }
    
    
    /**
     * Call this method to add custom proposal provider. It's
     * refresh method will be called on refresh.
     * 
     * @param providerId id of provider - must NOT start with '_'
     * @param provider the provider
     */
    public void addProvider(String providerId, GlobalsProvider provider) {
        m_providers.put(providerId, provider);
    }
    
    
    /**
     * Call this method to add core/application specific custom proposal 
     * provider. It's refresh method will be called on refresh.
     * 
     * @param providerId id of provider - must NOT start with '_'
     * @oaram coreId Id of core or application this provider is for
     * @param provider the provider
     */
    public void addProvider(String providerId, String coreId, GlobalsProvider provider) {
        CoreGlobalsProvider coreGlobalsProvider = m_coreProviders.get(coreId);
        if (coreGlobalsProvider == null) {
            coreGlobalsProvider = new CoreGlobalsProvider();
            m_coreProviders.put(coreId, coreGlobalsProvider);
        }
        coreGlobalsProvider.addCoreProvider(providerId, provider);
    }
    
    
    public CTestGlobalsContainer getCGlobalsContainer() {
        return m_cGlobalsContainer;
    }


    /**
     * Calls refresh() on all contained providers - performs isystem.connect 
     * calls to get data from winIDEA.
     */
    public void refresh() {

        JConnection connection = m_conProvider.getDefaultConnection();
        refreshHeadless(connection);
    }
    
    
    public void refreshHeadless(JConnection connection) {

        if (connection == null) {
            return;
        }
        // System.out.print("  Refresh C++ Container ... ");
        // StopWatch sw = new StopWatch(); sw.start();
        m_cGlobalsContainer.refresh(connection.getMccMgr(), false);
        // sw.stop();
        // System.out.println("Done: " + sw);
        // System.out.println("Stats:\n" + m_cGlobalsContainer.toString());
        
        // always recreate, as coreIds may be changed by the user editing Properties
        createCoreSpecificProviders();
        
        // lazy creation
        if (!m_providers.containsKey(GC_HIL)) {
            createGeneralProviders();
        }

        for (Entry<String, GlobalsProvider> pair : m_providers.entrySet()) {
            pair.getValue().refreshGlobals();
        }
    }

    
    public FunctionGlobalsProvider getFuncGlobalsProvider(String coreId) {
        GlobalsProvider provider = getProvider(GC_FUNCTIONS, coreId);
        if (provider instanceof FunctionGlobalsProvider) {
            return (FunctionGlobalsProvider)provider;
        }
        return new FunctionGlobalsProvider(null, coreId, "", null); 
    }
    
    
    public VariablesGlobalsProvider getVarsGlobalsProvider(String coreId) {
        GlobalsProvider provider = getProvider(GC_VARS, coreId);
        if (provider instanceof VariablesGlobalsProvider) {
            return (VariablesGlobalsProvider)provider;
        }
        return new VariablesGlobalsProvider(null, coreId, null);
    }
    
    
    public GlobalsProvider getVarsAndMacrosGlobalsProvider(String coreId) {
        return getProvider(GC_VARS_AND_MACROS, coreId);
    }
    
    
    public GlobalsProvider getTypesGlobalsProvider(String coreId) {
        return getProvider(GC_TYPES, coreId);
    }

    
    public GlobalsProvider getModulesGlobalsProvider(String coreId) {
        return getProvider(GC_MODULES, coreId);
    }
    
    
    private CoreGlobalsProvider getCoreGlobalsProvider(String coreId) {
        if (coreId.isEmpty()) {
            coreId = m_primaryCoreId;
        }
        CoreGlobalsProvider coreGP = m_coreProviders.get(coreId);
        return coreGP;
    }
    
    
    public GlobalsProvider getTagsGlobalsProvider() {
        return getProvider(GC_TAGS, null);
    }
    
    
    public GlobalsProvider getTestIdsGlobalsProvider() {
        return getProvider(GC_TEST_IDS, null);
    }
    
    
    public GlobalsProvider getCoreIdsGlobalsProvider() {
        return getProvider(GC_CORE_IDS, null);
    }
    
    
    public GlobalsProvider getHilGlobalsProvider() {
        return getProvider(GC_HIL, null);
    }
    
    
    public GlobalsProvider getCodeLabelsGlobalsProvider() {
        return getProvider(GC_CODE_LABELS, null);
    }
    
    
    public GlobalsProvider getCustomGlobalsProvider() {
        return getProvider(GC_CUSTOM_PROVIDER, null);
    }
    
    
    /** Returns provider for all modules on all cores. 
     * @param isUseInRegEx */
    public GlobalsProvider getAllModulesProvider(boolean isUseInRegEx) {
        
        /* AggregateGlobalsProvider provider = (AggregateGlobalsProvider) getProvider(GC_ALL_MODULES, null);
        List<GlobalsProvider> moduleProviders = provider.getProviders();
        for (GlobalsProvider moduleProvider : moduleProviders) {
            ((ModulesGlobalsProvider)moduleProvider).setMode(isUseInRegEx);
        } */
        
        return getProvider(GC_ALL_MODULES, null);
    }
    
    
    /** Returns provider for partitions on all cores. */
    public GlobalsProvider getAllPartitionsProvider() {
        return getProvider(GC_ALL_PARTITIONS, null);
    }
    
    
    /** Returns provider for all functions on all cores. */
    public GlobalsProvider getAllFunctionsProvider() {
        return getProvider(GC_ALL_FUNCTIONS, null);
    }
    
    
    /** Returns provider for all variables on all cores. */
    public GlobalsProvider getAllVarsProvider() {
        return getProvider(GC_ALL_VARS, null);
    }

    
    /** Returns provider for all types on all cores. */
    public GlobalsProvider getAllTypesProvider() {
        return getProvider(GC_ALL_TYPES, null);
    }
    
    
    /**
     * General method to get a provider - use it for custom added providers. 
     * 
     * @param providerId provider ID
     * @param coreId core ID or null if the requested provider is not core 
     *               specific.
     * @return requested provider or empty provider if requested provider is not found.
     */
    public GlobalsProvider getProvider(String providerId, String coreId) {

        if (isCoreSpecificProvider(providerId)) {

            if (coreId == null) {
                throw new SIllegalArgumentException("CoreID is null for core specific global provider which depends on core ID!")
                    .add("providerId", providerId);
            }

            CoreGlobalsProvider coreGP = getCoreGlobalsProvider(coreId);
            if (coreGP == null) {
                return m_emptyGlobalsProvider;
            }
            GlobalsProvider provider = coreGP.getCoreProvider(providerId);
            return provider == null ? m_emptyGlobalsProvider : provider;
            
        } else {
            
            if (coreId != null) {
                throw new SIllegalArgumentException("CoreID is not null for global provider which does NOT depend on core ID!")
                .add("providerId", providerId);
            }
            
            GlobalsProvider provider = m_providers.get(providerId);
            return provider == null ? m_emptyGlobalsProvider : provider;
        }
    }


    protected boolean isCoreSpecificProvider(String providerId) {
        return providerId.equals(GC_PARTITIONS)  ||
               providerId.equals(GC_FUNCTIONS)  ||
               providerId.equals(GC_VARS)  ||
               providerId.equals(GC_VARS_AND_MACROS)  ||
               providerId.equals(GC_TYPES)  ||
               providerId.equals(GC_MODULES);
    }
}
