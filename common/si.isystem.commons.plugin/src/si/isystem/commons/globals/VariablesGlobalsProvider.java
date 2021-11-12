package si.isystem.commons.globals;

import java.util.Map;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;

import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.connect.adapters.JDataController;
import si.isystem.connect.data.JCompoundType;
import si.isystem.connect.data.JVariable;


/** This class provides proposals for target global variables. */
public class VariablesGlobalsProvider extends GlobalsProvider {
    
    private IGlobalsConfiguration m_globalsCfg;
    protected Map<String, JVariable> m_cachedVariablesMap;
    


    protected VariablesGlobalsProvider(IConnectionProvider conProvider,
                                       String coreId,
                                       IGlobalsConfiguration globalsCfg) {
        super(conProvider, coreId);
        
        m_globalsCfg = globalsCfg;
        m_cachedVariablesMap = new TreeMap<>();
    }


    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        m_cachedVariablesMap.clear();
        
        if (m_conProvider == null  ||  m_globalsCfg == null) {
            return m_cachedGlobals;
        }
        
        getGlobalVariables();
        appendPersistentVars();
        
        return m_cachedGlobals;
    }

    
    public JCompoundType getExpressionTypeInfo(String expression) {
        JDataController jDataCtrl = getJDataCtrl();
        if (jDataCtrl == null) {
            return null;
        }
        return jDataCtrl.getExpressionCompoundTypeInfo(expression, "", -1);
    }
    
    
    /**
     * @param varOrTypeName name of target variable or type 
     * @return children of the given variable or type.
     */
    public String[] getChildren(String varOrTypeName) {

        JCompoundType compoundType = getExpressionTypeInfo(varOrTypeName);
        
        if (compoundType == null) {
            return new String[0];
        }
        
        return compoundType.getChildrenNames();
    }

    
    public Map<String, JVariable> getCachedVariablesMap() {
        return m_cachedVariablesMap;
    }


    protected void getGlobalVariables() {
        int idx = 0;
        m_cachedGlobals = getVariables(m_cachedVariablesMap);
        m_cachedDescriptions = new String[m_cachedGlobals.length];
        for (String varName : m_cachedGlobals) {
            JVariable jvar = m_cachedVariablesMap.get(varName);
            StringBuilder varNameSb = new StringBuilder();
            StringBuilder varPartition = new StringBuilder();
            StringBuilder varModule = new StringBuilder();
            JVariable.parseQualifiedName(jvar.getQualifiedName(), varModule, 
                                         varNameSb, varPartition);
            m_cachedDescriptions[idx++] = jvar.getVarTypeName() + "  " + varName + 
                                     "  (" + varPartition + ", " + varModule + ")";
        }
    }

    
    private String[] getVariables(Map<String, JVariable> varsMap) {

        JDataController jDataCtrl = getJDataCtrl();
        
        if (jDataCtrl == null) {
            return new String[0];
        }
        
        JVariable[] vars = jDataCtrl.getGlobalVariables();
        // Can't be array, because if vars contain invalid identifier name, 
        // returned array is shorter (cotains null-s) than vars (see 
        // comment in catch block below).
        ArrayList<String> varsAsStrArray = new ArrayList<String>();

        for (JVariable var : vars) {
            
            StringBuilder varName = new StringBuilder();
            StringBuilder varPartition = new StringBuilder();
            StringBuilder varModule = new StringBuilder();
            try {
                JVariable.parseQualifiedName(var.getQualifiedName(), varModule, varName, varPartition);
            
                varsAsStrArray.add(varName.toString());
            
                varsMap.put(varName.toString(), var);
            } catch (IllegalArgumentException ex) {
                // ignore parsing errors, since some compilers produce symbols 
                // with names which are illegal C identifiers. winIDEA tolerates
                // them, but they can't be used in watch expressions, so it makes 
                // no sense to include them in code proposals. 
                // See [B029203], May 2021:
                // __Description__:
                //    __class__: IllegalArgumentException
                //    __msg__: Invalid format of qualified symbol name: "CC26X2R1_LAUNCHXL_fxns.c"#$P$T0$1,,testIDEA.out
            }
        }
        return varsAsStrArray.toArray(new String[0]);
    }

    
    protected void appendPersistentVars() {

        String[][] props = m_globalsCfg.getCustomVars();
        
        if (props != null) {
            m_cachedGlobals = ArrayUtils.addAll(m_cachedGlobals, props[0]);
            m_cachedDescriptions = ArrayUtils.addAll(m_cachedDescriptions, props[1]);
        }
    }
}


