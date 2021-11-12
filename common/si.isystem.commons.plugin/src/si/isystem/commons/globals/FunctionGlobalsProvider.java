package si.isystem.commons.globals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.connect.adapters.JDataController;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JPartition;
import si.isystem.exceptions.SIllegalArgumentException;

public class FunctionGlobalsProvider extends GlobalsProvider {
    
    private IGlobalsConfiguration m_globalsCfg;

    // keys in this map are winIDEA qualified names, which are unique without redundancy
    protected Map<String, JFunction> m_cachedFunctionsFromAllFilesMap; 
    // Keys in this map are scoped func names, for example: 'Vehicle::getItems', 'getCount'
    // Values are arrays of functions, which have the same scoped name, but  
    // differ in other qualifiers.  
    protected Map<String, List<WeakFunctionName>> m_weakFuncNamesMap;
    
    private String m_defaultPartitionName;
    
    
    protected FunctionGlobalsProvider(IConnectionProvider conProvider,
                                      String coreId,
                                      String defaultPartitionName,
                                      IGlobalsConfiguration globalsCfg) {
        super(conProvider, coreId);
        m_defaultPartitionName = defaultPartitionName;
        m_globalsCfg = globalsCfg;

        m_cachedFunctionsFromAllFilesMap = new TreeMap<>();
        m_weakFuncNamesMap = new TreeMap<>();
    }

    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();

        if (m_conProvider == null  ||  m_globalsCfg == null) {
            return m_cachedGlobals;
        }
        
        m_cachedFunctionsFromAllFilesMap.clear();
        m_weakFuncNamesMap.clear();
        
        int idx = 0;
    
        m_cachedGlobals = getFunctionsForAllPartitions(m_cachedFunctionsFromAllFilesMap,
                                                       m_weakFuncNamesMap);
        
        // fill cached function descriptions 
        m_cachedDescriptions = new String[m_cachedGlobals.length];
        for (String funcName : m_cachedGlobals) {
            JFunction jFunc = m_cachedFunctionsFromAllFilesMap.get(funcName); 
            if (jFunc != null) {
                m_cachedDescriptions[idx++] = funcName + ":\n" + jFunc.getPrototype();
            }
        }
        
        return m_cachedGlobals;
    }
    
    
    public boolean isEmpty() {
        return m_cachedFunctionsFromAllFilesMap.isEmpty();
    }
    
    
    public JFunction getCachedFunction(String functionName) {
        JFunction func = m_cachedFunctionsFromAllFilesMap.get(functionName);

        // try to find function with weak name (not all components are specified 
        if (func == null) {
            StringBuilder module = new StringBuilder();
            StringBuilder scopedName = new StringBuilder();
            StringBuilder signature = new StringBuilder();
            StringBuilder partition = new StringBuilder();
            JFunction.parseQualifiedName(functionName, module, scopedName, 
                                         signature, partition);
            
            if (scopedName.length() != 0) {
                List<WeakFunctionName> functions = 
                                  m_weakFuncNamesMap.get(scopedName.toString());
                if (functions != null) {
                    if (partition.length() == 0) { // default partition name is optional
                        partition.append(m_defaultPartitionName);
                    }

                    // get array with all matches for scoped function name (for  
                    // example: 'Vehicle::getItem', 'getCount') 
                    WeakFunctionName weakFunctionName = new WeakFunctionName(module.toString(), 
                                                                             scopedName.toString(), 
                                                                             signature.toString(), 
                                                                             partition.toString(), 
                                                                             null);

                    // Search array for all possible matches. If there is more than one, 
                    // it is an error in test specification.
                    for (WeakFunctionName weakCachedFuncName : functions) { 
                        if (weakFunctionName.equals(weakCachedFuncName)) {
                            if (func != null) { // has been already found, error!
                                throw new SIllegalArgumentException("Function name is ambigous: '" + functionName + "'")
                                .add("match_1", func.getWinIDEAQualifiedName())
                                .add("match_2", weakCachedFuncName.getJFunc().getWinIDEAQualifiedName());
                            }
                            func = weakCachedFuncName.getJFunc(); 
                        }
                    }
                }
            }
        }

        return func;
    }


    /**
     * Returns true, if function with this name exists. 
     * @param funcName function name  
     */
    public boolean isFunctionExists(String funcName) {
        // funcName = getFunctionNameWithoutParamsAndDlFile(getFunctionNameWithoutModule(funcName));
        // return m_cachedFunctionsFromAllFilesMap.containsKey(funcName);
        return getCachedFunction(funcName) != null;
    }
    
    
    public static String getFunctionNameWithoutModule(String functionName) {
        // get also function name without module, for example:
        //   "main.c"#min_int,,rom.elf ==> min_int,,rom.elf
        // because user may have specified redundant module name. Since the function
        // with the module name does not exist in mappings, false warning would be issued
        // (See class common.GlobalsProvider, which prepends module names only for
        // duplicated function names.)
        
        int hashIndex = functionName.indexOf('#');
        String strippedFuncName = functionName;
        if (hashIndex >= 0) {
            strippedFuncName = functionName.substring(hashIndex + 1);
        }
        return strippedFuncName;
    }


//    private String getFunctionNameWithoutDLFile(String functionName) {
//        // get also function name without download file, for example:
//        //   "min_int,,rom.elf ==> min_int
//        
//        int separatorIdx = functionName.indexOf(",,");
//        String strippedFuncName = functionName;
//        if (separatorIdx >= 0) {
//            strippedFuncName = functionName.substring(0, separatorIdx);
//        }
//        return strippedFuncName;
//    }

    
    /**
     * Returns function name without params and download file, for example:
     *   "min_int(int, int),,rom.elf ==> min_int
     */
    @SuppressWarnings("unused")
	private String getFunctionNameWithoutParamsAndDlFile(String functionName) {
        
        int startIdx = 0;
        
// uncomment if function names with module should be handled        
//        int startIdx = functionName.indexOf('#');
//        if (startIdx < 0) {
//            startIdx = 0;
//        }
        
        int separatorIdx = functionName.indexOf('(', startIdx);
        if (separatorIdx >= 0) {
            // start from 1, since func names with params are quoted and the quote must be skipped
            return functionName.substring(1, separatorIdx);
        }
        return functionName;
    }
    
    
    /**
     * Returns list of functions in all download files. Function names are 
     * qualified - function name with download file is returned. If file
     * static functions with the same name exist in multiple files, they are
     * prepended also a source file name. Examples of qualified names:
     * myFunc,,sample.elf
     * "main.c"#myFunc,,sample.elf 
     * 
     * @param functionTypesMap if null, it is ignored, if not null, it is filled with
     *                         functions
     * @param funcsWScopedFName 
     * @return
     */
    protected String[] getFunctionsForAllPartitions(Map<String, JFunction> functionTypesMap,
                                                    Map<String, List<WeakFunctionName>> weakFunctionsMap) {
        
        if (functionTypesMap != null) {  // clear the map before returning String[0]
            functionTypesMap.clear();
        } else {
            // if not given, create a map - we'll need it later to verify that
            // there are no two file static functions with the name - if there are,
            // source file name should be prepended
            functionTypesMap = new TreeMap<>();
        }

        JDataController jDataCtrl = getJDataCtrl();

        if (jDataCtrl == null) {
            return new String[0];
        }
                
        JPartition[] partitions = jDataCtrl.getPartitions();
        
        JFunction[][] allFunctions = new JFunction[partitions.length][]; 
        int partitionIdx = 0;
        for (JPartition partition : partitions) {
            allFunctions[partitionIdx] = jDataCtrl.getGlobalFunctions(partitionIdx, partition.getName());
            partitionIdx++;
        }

        // Note 2016: duplicates should no longer be a problem if winIDEA Qualified name is used
        // use map of lists, to detect all functions, which have the same name,
        // but may differ in module, parameters (C++) or partition.
        // Map<String, List<JFunction>> jFunctionsMap = new TreeMap<>();
        
        for (JFunction[] functions : allFunctions) {
        
            if (functions != null) { // can happen if there are no symbols available in winIDEA
                
                for (JFunction func : functions) {
                    String scopedFuncName = func.getScopedFuncName();
                    if (scopedFuncName == null  ||  scopedFuncName.isEmpty()) {
                        continue; // sometimes winIDEA finds some items, which 
                        // might be functions, but have no name. Ignore them,
                        // they are not callable from testIDEA anyway.
                    }
                    
                    String funcName = func.getWinIDEAQualifiedName();
                    functionTypesMap.put(funcName, func);
                    List<WeakFunctionName> weakFuncs = weakFunctionsMap.get(scopedFuncName);
                    if (weakFuncs == null) {
                        weakFuncs = new ArrayList<>();
                        weakFunctionsMap.put(scopedFuncName, weakFuncs);
                    }
                    
                    weakFuncs.add(new WeakFunctionName(func.getModuleName(),
                                                       scopedFuncName,
                                                       func.getSignature(),
                                                       func.getPartitonName(), 
                                                       func));
                }
            }
        }
        
        return functionTypesMap.keySet().toArray(new String[0]);
    }
}        


/**
 * This class contains function name with weak comparison function. This means
 * that only non-null items are compared. For example, if other WeakFunction name
 * has only name specified, then only name will be compared.
 *  
 * @author markok
 */
class WeakFunctionName implements Comparable<WeakFunctionName> {
    private String m_module;
    private String m_scopedFuncName;
    private String m_signature;
    private String m_partition;
    private JFunction m_jFunc;
    
    WeakFunctionName(String module, String scopedFuncName, 
                     String signature, String partition,
                     JFunction jFunc) {
    
        // if module is empty, keep m_module set to null
        if (module != null  &&  !module.isEmpty()) { 
            m_module = module;
        }
        
        m_scopedFuncName = scopedFuncName;
        
        if (signature != null  &&  !signature.isEmpty()) {
            m_signature = signature;
        }
        
        if (partition != null  &&  !partition.isEmpty()) {
            m_partition = partition;
        }
        
        m_jFunc = jFunc;
        
        if (scopedFuncName == null  ||  scopedFuncName.isEmpty()) {
            throw new SIllegalArgumentException("Function name must be defined!")
            .add("data", toString());
        }
    }
    
    
    public JFunction getJFunc() {
        return m_jFunc;
    }


    @Override
    public int compareTo(WeakFunctionName other) {
    
        int eq = cmp(m_module, other.m_module); 
        if (eq != 0) {
            return eq;
        }
        
        eq = cmp(m_scopedFuncName, other.m_scopedFuncName); 
        if (eq != 0) {
            return eq;
        }

        eq = cmp(m_signature, other.m_signature); 
        if (eq != 0) {
            return eq;
        }

        return cmp(m_partition, other.m_partition); 
    }
    
    
    private int cmp(String ths, String other) {
        if (ths != null  &&  other != null) {
            return ths.compareTo(other);
        }
        return 0;
    }
    
    @Override 
    public boolean equals(Object o) {
        if (o instanceof WeakFunctionName) {
            return compareTo((WeakFunctionName)o) == 0;
        }
        
        return false;
    }
    
    
    @Override
    public String toString() {
        
        return "module: " + m_module
               + "\nfuncName: " + m_scopedFuncName
               + "\nparams: " + m_signature
               + "\npartition: " + m_partition;
    }
}