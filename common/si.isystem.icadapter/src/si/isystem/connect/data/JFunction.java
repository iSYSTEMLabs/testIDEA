package si.isystem.connect.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import si.isystem.connect.CUtil;
import si.isystem.connect.ICallee;
import si.isystem.connect.IFunction;
import si.isystem.connect.IVectorCallees;
import si.isystem.connect.IVectorVariables;


/**
 * This is immutable data class, which contains information about C function on
 * a target.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JFunction implements Comparable<JFunction> {

    private final static Pattern m_qualifiedFuncNameRegEx = 
            Pattern.compile(CUtil.getG_qualifiedFuncNamePattern());

    private final String m_name;
    private final String m_scope;
    private final String m_returnTypeName;
    private final String m_signature; // could also be deduced from list of parameters
    private final int m_moduleIndex;
    private final String m_moduleName;
    private final String m_partitonName;
    private final JVariable[] m_parameters;
    private final JVariable[] m_localVars;
    private final String m_winIDEAQualifiedName;
    private final String m_fullyQualifiedName;
    private final boolean m_isVariadicParams;
    private final long[] m_callsFrom;
    
    /**
     * 
     * @param name function name
     * @param scope function's scope, for example a class name
     * @param returnTypeName type of the function return value
     * @param module module, where function is defined
     * @param parameters function parameters
     * @param localVars array of function local variables, regardless of scope
     */
    public JFunction(String name,
                     String scope,
                     String returnTypeName,
                     String signature,
                     int module,
                     String moduleName,
                     String winIDEAQualifiedName,
                     String partitonName,
                     JVariable[] parameters, 
                     JVariable[] localVars,
                     boolean isVariadicParams,
                     long [] callsFrom) {
        m_name = name;
        m_scope = scope;
        m_returnTypeName = returnTypeName;
        m_signature = signature;
        m_moduleIndex = module;
        m_moduleName = moduleName;
        m_winIDEAQualifiedName = winIDEAQualifiedName;
        m_partitonName = partitonName;
        m_parameters = parameters;
        m_localVars = localVars;
        m_fullyQualifiedName = createQualifiedName();
        m_isVariadicParams = isVariadicParams;
        m_callsFrom = callsFrom;
    }

    
    public JFunction(IFunction iFunc, 
                     String partitionName, 
                     int partitionIdx, 
                     String moduleName) {
        m_name = iFunc.Name();
        m_scope = iFunc.Scope();
        m_returnTypeName = iFunc.ReturnTypeName();
        m_signature = iFunc.SignatureName();
        m_moduleIndex = iFunc.Module();
        m_moduleName = moduleName;
        m_winIDEAQualifiedName = iFunc.QualifiedName();
        m_partitonName = partitionName;
        m_parameters = toJVariables(iFunc.Parameters(), partitionName, partitionIdx);
        m_localVars = toJVariables(iFunc.Variables(), partitionName, partitionIdx);
        m_fullyQualifiedName = createQualifiedName();
        m_isVariadicParams = iFunc.HasVarParams();
        m_callsFrom = getCallsFromFunc(iFunc);
    }

    
    private static JVariable[] toJVariables(IVectorVariables iVectorVariables, 
                                            String partitionName, 
                                            int partitionIdx) {
        
        JVariable[] res = new JVariable[(int) iVectorVariables.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = new JVariable(iVectorVariables.at(i), partitionName, partitionIdx, true);
        }
        return res;
    }

    /** For internal use only. */
    public static long[] getCallsFromFunc(IFunction iFunc) {
        IVectorCallees iCallsFromFunc = iFunc.Callees();
        int ncalls = (int) iCallsFromFunc.size();
        long[] callsFrom = new long[ncalls];
        for (int idx = 0; idx < ncalls; idx++) {
            ICallee callFrom = iCallsFromFunc.at(idx);
            // store direct and indirect calls
            callsFrom[idx] = callFrom.Callee();
        }
        return callsFrom;
    }

    
    /** Returns function name. */
    public String getName() {
        return m_name;
    }

    
    /** Returns function qualified name. Usually it is the same as name.*/
    public String getScope() {
        return m_scope;
    }

    
    public String getScopedFuncName() {
        
        StringBuilder funcBuilder = new StringBuilder();
        
        if (!m_scope.isEmpty()) {
            funcBuilder.append(m_scope).append("::");
        }
        
        funcBuilder.append(m_name);
        
        return funcBuilder.toString();
    }
    
    
    /**
     * Returns function return type, for example: <code>float</code>
     */
    public String getReturnTypeName() {
        return m_returnTypeName;
    }


    /**
     * Returns function signature as a list of parameter types, for example:
     * '(int, char *). Names of parameters are not given.
     */
    public String getSignature() {
        return m_signature;
    }


    /** Returns name of partition where function is defined. */
    public String getPartitonName() {
        return m_partitonName;
    }


    /**
     * @return function prototype with return type and parameters (types and 
     * names), but without function name, for example: 'int (char c, int *p)'.  
     */
    public String getPrototype() {
        StringBuilder sb = new StringBuilder(m_returnTypeName);
        
        String comma = "";
        sb.append(" (");
        
        for (JVariable param : m_parameters) {
            sb.append(comma);
            sb.append(param.getVarTypeName()).append(" ");
            sb.append(param.getName());
            comma = ", ";
        }
        
        sb.append(")");
        
        return sb.toString();
    }
    
    
    /**
     * Returns fully qualified name with module name, scope, function name, signature, and
     * partition, for example: "main.c"#"Adder::add(int, int)",,sample.elf
     * If any of these items is empty, it is not included in qualified name.
     */
    public String getQualifiedName() {
        return m_fullyQualifiedName;
    }
    
    
    /**
     * Returns function name qualified according to winIDEA setting, by default
     * it is just enough to be unique.
     * 
     * @return
     */
    public String getWinIDEAQualifiedName() {
        return m_winIDEAQualifiedName;
    }
    
    
    /** Returns module index, where function is located. */
    public int getModule() {
        return m_moduleIndex;
    }

    
    public String getModuleName() {
        return m_moduleName;
    }


    /** Returns function parameters. */
    public JVariable[] getParameters() {
        return m_parameters;
    }

    
    /** Returns function local variables. */
    public JVariable[] getLocalVars() {
        return m_localVars;
    }
    
    
    public boolean isVariadicParams() {
        return m_isVariadicParams;
    }


    /** Returns addresses of functions called from this function. */
    public long[] getCalledFunctions() {
        return m_callsFrom;
    }
    
    
    private String createQualifiedName() {
        StringBuilder sb = new StringBuilder();
        
        if (!m_moduleName.isEmpty()) {
            sb.append('"').append(m_moduleName).append("\"#");
        }
        
        if (!m_signature.isEmpty()  &&  !m_signature.equals("()")) {
            sb.append('"');
            
            if (!m_scope.isEmpty()) {
                sb.append(m_scope).append("::");
            }
            sb.append(m_name).append(m_signature).append('"');
        } else {
            if (!m_scope.isEmpty()) {
                sb.append(m_scope).append("::");
            }
            sb.append(m_name);
        }
        
        if (!m_partitonName.isEmpty()) {
            sb.append(",,").append(m_partitonName);
        }

        return sb.toString();
    }

    public String getDescription() {
        StringBuilder s = new StringBuilder();
        s.append(m_returnTypeName).append(' ').append(m_name).append('(');
        if (m_parameters != null  &&  m_parameters.length > 0) {
            s.append(m_parameters[0].getVarTypeName());
            for (int i = 1; i < m_parameters.length; i++) {
                s.append(", ").append(m_parameters[i].getVarTypeName());
            }
        }
        s.append(')');
        
        return String.format("%s\nModule: %s\nPartition: %s\n", 
                s, 
                m_moduleName,
                m_partitonName);
    }

    
    /**
     * Returns components of qualified name. This method is a copy of
     * CUtil::parseQualifiedFuncName(), because it is not trivial to 
     * pass several strings across language border.
     * 
     * @param qName
     */
    public static void parseQualifiedName(String qName, 
                                          StringBuilder module, 
                                          StringBuilder scopedName,
                                          StringBuilder signature,
                                          StringBuilder partition) {
        
        parseQualifiedName(qName, module, scopedName, signature, partition, 
                           m_qualifiedFuncNameRegEx);
    }
    
    
    static void parseQualifiedName(String qName, 
                                   StringBuilder module, 
                                   StringBuilder scopedName,
                                   StringBuilder signature,
                                   StringBuilder partition,
                                   Pattern regEx) {
            
        module.delete(0, module.length());
        scopedName.delete(0, scopedName.length());
        signature.delete(0, signature.length());
        partition.delete(0, partition.length());

        if (qName.isEmpty()) {
            return;
        }
        
        Matcher matcher = regEx.matcher(qName);
        
        // groupCount() does not include group 0 in the count 
        if (matcher.matches()) { 

            String moduleGrp = matcher.group(CUtil.getMODULE_GRP_IDX());
            module.append(moduleGrp == null ? "" : moduleGrp);
            
            String symbolNameGrp = matcher.group(CUtil.getSYM_NAME_GRP_IDX());
            scopedName.append(symbolNameGrp == null ? "" : symbolNameGrp);
            
            String signatureGrp = matcher.group(CUtil.getSIGNATURE_GRP_IDX());
            signature.append(signatureGrp == null ? "" : signatureGrp);
            
            String partitionGrp = matcher.group(CUtil.getDOWNLOAD_FILE_GRP_IDX());
            partition.append(partitionGrp == null ? "" : partitionGrp);
        } else {
            throw new IllegalArgumentException("Invalid format of qualified symbol name: " + qName);
        }
        
        
//        int funcIdx = qName.indexOf('#');
//        
//        if (funcIdx > -1) {
//            if (funcIdx > 2) {
//                if (qName.charAt(0) != '"'  ||  qName.charAt(funcIdx - 1) != '"') {
//                    throw new IllegalStateException("Module name must be specified with double quotes: " + qName);
//                }
//                module.append(qName.substring(1, funcIdx - 1));
//            }
//            funcIdx++;
//        } else {
//            funcIdx = 0;
//        }
//        
//        // skip quote if present
//        if (qName.length() > funcIdx) {   
//            if (qName.charAt(funcIdx) == '"') {  
//                funcIdx++;   
//            }
//        }
//        
//        int partitionIdx = qName.indexOf(",,", funcIdx);
//        
//        if (partitionIdx > -1) {
//            partition.append(qName.substring(partitionIdx + 2));
//            // remove ending quote if present
//            if (partition.length() > 0  &&  partition.charAt(partition.length() - 1) == '"') {
//                partition.deleteCharAt(partition.length() - 1);
//            }
//        } else {
//            partitionIdx = qName.length();
//            // remove ending quote if present
//            if (partitionIdx > 0  &&  qName.charAt(partitionIdx - 1) == '"') {
//                partitionIdx--;
//            }
//        }
//        
//        String scopedFuncNameWSig = qName.substring(funcIdx, partitionIdx);
//        
//        int sigIdx = scopedFuncNameWSig.indexOf('(');
//        if (sigIdx > -1) {
//            signature.append(scopedFuncNameWSig.substring(sigIdx));
//            scopedName.append(scopedFuncNameWSig.substring(0, sigIdx));
//        } else {
//            scopedName.append(scopedFuncNameWSig);
//        }
    }

    
    @Override
    public String toString() {
        return m_fullyQualifiedName;
    }

    @Override
    public int compareTo(JFunction p) {
        int c;
        
        c = m_name.toLowerCase().compareTo(p.getName().toLowerCase());
        if (c != 0) {
            return c;
        }
        
        return m_fullyQualifiedName.toLowerCase().compareTo(p.getQualifiedName().toLowerCase());
    }
    
    @Override
    public int hashCode() {
        return m_fullyQualifiedName.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null  ||  !(obj instanceof JFunction)) {
            return false;
        }
        return m_fullyQualifiedName.equals(((JFunction)obj).getQualifiedName());
    }
}
