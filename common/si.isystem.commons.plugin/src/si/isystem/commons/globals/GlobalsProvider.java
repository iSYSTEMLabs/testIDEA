package si.isystem.commons.globals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.utils.ISysFileUtils;
import si.isystem.connect.adapters.JDataController;
import si.isystem.connect.data.JModule;
import si.isystem.exceptions.SIOException;


/**
 * This class provides common interface for all providers of global types and 
 * identifiers.
 */
public class GlobalsProvider {

    protected String[] m_cachedGlobals;
    protected String[] m_cachedDescriptions;

    protected IConnectionProvider m_conProvider;
    private String m_coreId;

    
    /**
     * Base class for all providers of content proposals.
     * 
     * @param connectionProvider
     * @param coreId should be null for providers, which are not core dependent,
     *               for example core Id provider.
     */
    protected GlobalsProvider(IConnectionProvider connectionProvider, String coreId) {
        m_conProvider = connectionProvider;
        m_coreId = coreId;
        
        m_cachedGlobals = new String[0];
        m_cachedDescriptions = new String[0];
    }
    

    protected JDataController getJDataCtrl() {
        
        if (m_conProvider == null) {
            return null;
        }
        
        JConnection jCon = m_conProvider.getDefaultConnection();
        
        if (jCon == null) {
            return null;
        }

        try {
            if (m_coreId == null) {
                return jCon.getJDataController("");
            } else {
                return jCon.getJDataController(m_coreId);
            }
        } catch (Exception ex) {
            throw new SIOException("Can not get connection, please reconnect (iTools | Connect To winIDEA)!", ex);
            // return null;
        }
    }
    
    
    /** Returns file name without path. */ 
    public String getDefaultDownloadFileName() {
        
        JDataController jDataCtrl = getJDataCtrl();

        if (jDataCtrl == null) {
            return "";
        }
        
        return jDataCtrl.getDefaultDownloadFileName();
    }
    
    
    /**
     * Derived class should override this method and call it whenever globals 
     * change.
     * @return always null
     */
    public String[] refreshGlobals() {

        m_cachedGlobals = new String[0];
        m_cachedDescriptions = new String[0];
    
        return null;
    }


    /**
     * @return globals read on previous call to refresh()
     */
    public String[] getCachedGlobals() {
        return m_cachedGlobals;
    }
    

    /**
     * @return descriptions of globals read on previous call to refresh()
     */
    public String[] getCachedDescriptions() {
        return m_cachedDescriptions;
    }


    /** 
     * Sets proposals directly - use this method if you'd like to provide
     * custom proposals, which are not available as target. globals items
     * 
     * @param proposals content proposals
     * @param descriptions descriptions of content proposals 
     */
    public void setProposals(String [] proposals, String [] descriptions) {
        m_cachedGlobals = proposals;
        m_cachedDescriptions = descriptions;
    }
}


/**
 * This class aggregates several content assist providers, for example 
 * function providers from all cores.
 * 
 * @author markok
 */
class AggregateGlobalsProvider extends GlobalsProvider {
    
    private List<GlobalsProvider> m_providers = new ArrayList<>();
    
    
    protected AggregateGlobalsProvider() {
        super(null, null);
    }

    
    public void addGlobalsProvider(GlobalsProvider provider) {
        m_providers.add(provider);
    }
    
    
    /**
     * Aggregated providers should be refreshed prior to this call. Otherwise
     * time-expensive retrieval of data from winIDEA is unnecessarily
     * duplicated. 
     */
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();

        List<String>proposals = new ArrayList<>();
        List<String>descriptions = new ArrayList<>();
        for (GlobalsProvider provider : m_providers) {
            // String[] globals = provider.refreshGlobals();  // time consuming
            String[] globals = provider.getCachedGlobals();
            proposals.addAll(Arrays.asList(globals));
            
            String[] globalDesc = provider.getCachedDescriptions();
            if (globalDesc == null) {
                // to maintain alignment of descriptions with globals
                globalDesc = new String[globals.length];
            } 
            descriptions.addAll(Arrays.asList(globalDesc));
        }
        
        m_cachedGlobals = proposals.toArray(new String[0]);
        
        m_cachedDescriptions = descriptions.toArray(new String[0]);
        
        return m_cachedGlobals;
    }
    
    
    public List<GlobalsProvider> getProviders() {
        return m_providers;
    }
}


/** This class provides proposals for target global variables and macros. */
class VarsAndMacrosGlobalsProvider extends VariablesGlobalsProvider {
    
    protected VarsAndMacrosGlobalsProvider(IConnectionProvider conProvider,
                                           String coreId,
                                           IGlobalsConfiguration globalsCfg) {
        super(conProvider, coreId, globalsCfg);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();

        
        getGlobalVariables();
        appendPersistentVars();
        List<String> values = new ArrayList<>();
        
        JDataController jDataCtrl = getJDataCtrl();

        if (jDataCtrl == null) {
            return new String[0];
        }
        
        String [] macros = jDataCtrl.getMacros(values);
        m_cachedGlobals = ArrayUtils.addAll(m_cachedGlobals, macros);
        m_cachedDescriptions = (String [])ArrayUtils.addAll(m_cachedDescriptions, values.toArray()); 

        return m_cachedGlobals;
    }
}


/** This class provides proposals for target types. */
class TypesGlobalsProvider extends GlobalsProvider {
    
    private FunctionGlobalsProvider m_funcProvider;

    protected TypesGlobalsProvider(IConnectionProvider conProvider,
                                   String coreId,
                                   FunctionGlobalsProvider funcProvider) {
        super(conProvider, coreId);
        //super(conProvider, coreId, globalsCfg);
        m_funcProvider = funcProvider;
    }


    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();

        m_cachedGlobals = getDataTypes();
        m_cachedDescriptions = null; 
        
        return m_cachedGlobals;
    }
    
    
    /**
     * Returns all data types found in executable, including all function names.
     */
    private String[] getDataTypes() {
        
        JDataController jDataCtrl = getJDataCtrl();

        if (jDataCtrl == null) {
            return new String[0];
        }
        
        Set<String> dataTypes = jDataCtrl.getDataTypes();

        // add next items for auto-completion of types referring to function parameter types
        dataTypes.add("decltype"); 
        dataTypes.add("decltype(<funcName>##<N>)"); 
        dataTypes.add("decltype(*<funcName>##<N>)");
        dataTypes.add("decltype(*<funcName>##<N>)[<K>]");
        dataTypes.add("decltype_ref(<funcName>##<N>)"); 
        
        String[] functions = m_funcProvider.getCachedGlobals();
        dataTypes.addAll(Arrays.asList(functions));
        
        return dataTypes.toArray(new String[0]);
    }
}


    
/** This class provides proposals for target source files. */
class ModulesGlobalsProvider extends GlobalsProvider {
    
    private boolean m_isUseInRegEx = true; // make it configurable by the caller,
    // or create a special module provider class with paths escaped for reg exs. 


    protected ModulesGlobalsProvider(IConnectionProvider conProvider,
                                     String coreId) {
        super(conProvider, coreId);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        JDataController jDataCtrl = getJDataCtrl();
        
        if (jDataCtrl == null) {
            return new String[0];
        }

        int numPartitions = jDataCtrl.getNumberOfPartitions();
        Set<String> moduleNames = new TreeSet<>(); // use set to avoid duplicates
        
        for (int i = 0; i < numPartitions; i++) {
            JModule[] modules = jDataCtrl.getModules(i);
            for (JModule module : modules) {
                String modulePath = module.getPath();
                if (m_isUseInRegEx) {
                    modulePath = ISysFileUtils.pathForRegEx(modulePath);
                }
                moduleNames.add(modulePath);
            }
        }

        m_cachedGlobals = moduleNames.toArray(new String[0]);
        m_cachedDescriptions = null; 

        return m_cachedGlobals;
    }


    /*
     * @param isUseInRegEx if true, then '\' is replaced with '/', other chars
     * used in reg ex and appear in filename, are escaped.
     */
//    public void setMode(boolean isUseInRegEx) {
//        m_isUseInRegEx = isUseInRegEx;
//    }
    
    /* private String getModulePrefix(JFunction jFunc, JModule[] modules) {
    return '"' + modules[jFunc.getModule()].getName() + "\"#";
}
*/

/**
 * 
 * @return array of modules for all partitions (download files). The first 
 * dimension equals number of partitions, the second equals the number of modules
 * in partition (may not be equal for all partitions). 
protected JModule[][] getModulesForAllPartitions() {

    if (m_jdataCtrl == null) {
        return new JModule[0][0];
    }

    int numPartitions = m_jdataCtrl.getNumberOfPartitions();
    
    JModule partitionModules[][] = new JModule[numPartitions][];
    
    for (int i = 0; i < numPartitions; i++) {
        JModule[] modules = m_jdataCtrl.getModules(i);
        partitionModules[i] = modules;
    }

    return partitionModules;
}
 */
}


/** This class provides proposals for code labels. */
class LabelsGlobalsProvider extends GlobalsProvider {
    
    protected LabelsGlobalsProvider(IConnectionProvider conProvider) {
        super(conProvider, null);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        JDataController jDataCtrl = getJDataCtrl();
        
        if (jDataCtrl == null) {
            return new String[0];
        }

        m_cachedGlobals = jDataCtrl.getCodeLabels();
        m_cachedDescriptions = null; 

        return m_cachedGlobals;
    }
}

    
/** This class provides proposals for HIL inputs. */
class HilGlobalsProvider extends GlobalsProvider {
    
    protected HilGlobalsProvider(IConnectionProvider conProvider) {
        super(conProvider, null);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        JDataController jDataCtrl = getJDataCtrl();
        
        if (jDataCtrl == null) {
            return new String[0];
        }
        
        try {
            m_cachedGlobals = jDataCtrl.getHilParametersAsPaths(null);
            m_cachedDescriptions = null; // are there any available???
        } catch (Exception ex) {
            m_cachedGlobals = new String[0];
            // ignore exception (confirmed by JU) - we can not reproduce the 
            // problem, when 
            // iconnect returns E_FAIL (IC5000 without IO module).
        }

        return m_cachedGlobals;
    }
}


/** This class provides proposals for core IDs. */
class CoreIdGlobalsProvider extends GlobalsProvider {
    
    private IGlobalsConfiguration m_globalsCfg;


    protected CoreIdGlobalsProvider(IGlobalsConfiguration globalsCgf) {
        super(null, null);
        
        m_globalsCfg = globalsCgf;
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        m_cachedGlobals = m_globalsCfg.getCoreIds();
        m_cachedDescriptions = null; // could provide iyaml comments one day...

        return m_cachedGlobals;
    }
}
