package si.isystem.connect.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import si.isystem.connect.CUtil;
import si.isystem.connect.IVariable;
import si.isystem.connect.IVariable.EType;

/**
 * This class contains immutable variable description.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JVariable implements Comparable<JVariable> {

    private final static Pattern m_qualifiedVarNameRegEx = 
            Pattern.compile(CUtil.getG_qualifiedVarNamePattern());

    
    private final static Pattern m_patternForArrays = 
            Pattern.compile("(\\W*)(.*)"); // skip prefix, for example '(', or '*('
    
    private final String m_name;  // contains printable name of var (the one the user sees)
    private final String m_baseName; // contains name of var, but without module and partition. 
    // Introduced to clearly separate real name from printable name. The difference is
    // important for array elements, where the user wants to see [0], not myarray[0] in the list
    // of array elements.
    private final String m_qualifiedName;
    private final String m_varTypeName;
    private final int m_numBytes;
    private final EType m_type;
    private final long m_arrayLen;
    private final int m_moduleIdx;
    private final String m_partition;
    private final int m_partitionIdx;
    private final String m_scope;
    
    
    /**
     * Copies data from native object to this one.
     * 
     * @param var native object with variable description
     * 
     * @param isParseName normally should be true, but when workaround is used to get
     * type info (size, array dim, ...) the qualified name does not represent var name,
     * for example: *(int *)0
     * Parsing will fail in such case, and the name is also not needed because it is
     * not shown to the user.
     */
    public JVariable(IVariable var, String partitionName, int partitionIdx, boolean isParseName) {

        String qualifiedName = var.QualifiedName();
        
        if (isParseName) {
            // the first item holds parent info
            StringBuilder tmp1 = new StringBuilder();
            StringBuilder name = new StringBuilder();
            StringBuilder tmp2 = new StringBuilder();

            try {
                String nameStr = var.Name();
                // Some compilers do not provide names of function parameters,
                // but only type - skip parsing in such cases.
                if (!nameStr.isEmpty()) {
                    parseQualifiedName(qualifiedName, tmp1, name, tmp2);
                }
            } catch (Exception ex) {
                System.out.println("JVarible: Parsing of qualified name failed: " + qualifiedName + '\n' + ex);
                throw ex;
            }
            m_baseName = name.toString();
        } else {
            m_baseName = qualifiedName;
        }
        
        m_name = var.Name();
        m_qualifiedName = qualifiedName;
        m_varTypeName = var.TypeName();
        m_numBytes = (int)var.NumBytes();
        m_type = EType.swigToEnum(var.Type());
        m_arrayLen = var.ArrayDimension();
        m_moduleIdx = var.Module();
        m_partition = partitionName;
        m_partitionIdx = partitionIdx;
        m_scope = var.Scope();
    }
    
    /**
     * Used for local variables and arguments.
     * 
     * @param name variable name
     * @param qualifiedName full variable name
     * @param varTypeName type of the variable
     * @param numBytes number of bytes occupied by the variable
     * @param arrayLen dimension of array, 0 for all other types
     */
    public JVariable(String name,
                     String qualifiedName,
                     String varTypeName,
                     int numBytes,
                     int type,
                     long arrayLen) {
        
        m_name = name;
        m_baseName = name;
        m_qualifiedName = qualifiedName;
        m_varTypeName = varTypeName;
        m_numBytes = numBytes;
        m_type = EType.swigToEnum(type);
        m_arrayLen = arrayLen;
        m_moduleIdx = -1;
        m_partition = "";
        m_partitionIdx = -1;
        m_scope = "";
    }

    
    /**
     * Used for global variables.
     * 
     * @param name variable name
     * @param qualifiedName full variable name
     * @param varTypeName type of the variable
     * @param numBytes number of bytes occupied by the variable
     * @param arrayLen dimension of array, 0 for all other types
     * @param type type of variable as IVariable::EType enumeration type.
     * @param moduleIdx module index
     * @param partition name of download file, where this var is located
     * @param variable's scope, for example class name.
     */
    public JVariable(String name,
                     String qualifiedName,
                     String varTypeName,
                     int numBytes,
                     int type,
                     long arrayLen,
                     int moduleIdx,
                     String partition,
                     int partitionIdx,
                     String scope) {
        
        m_name = name;
        m_baseName = name;
        m_qualifiedName = qualifiedName;
        m_varTypeName = varTypeName;
        m_numBytes = numBytes;
        m_type = EType.swigToEnum(type);
        m_arrayLen = arrayLen;
        m_moduleIdx = moduleIdx;
        m_partition = partition;
        m_partitionIdx = partitionIdx;
        m_scope = scope;
    }

    
    /**
     * Used for global variables.
     * 
     * @param name variable name
     * @param baseName 
     * @param qualifiedName full variable name
     * @param varTypeName type of the variable
     * @param numBytes number of bytes occupied by the variable
     * @param arrayLen dimension of array, 0 for all other types
     * @param type type of variable as IVariable::EType enumeration type.
     * @param moduleIdx module index
     * @param partition name of download file, where this var is located
     * @param variable's scope, for example class name.
     */
    public JVariable(String name,
                     String baseName,
                     String qualifiedName,
                     String varTypeName,
                     int numBytes,
                     int type,
                     long arrayLen,
                     int moduleIdx,
                     String partition,
                     int partitionIdx,
                     String scope) {
        
        m_name = name;
        m_baseName = baseName;
        m_qualifiedName = qualifiedName;
        m_varTypeName = varTypeName;
        m_numBytes = numBytes;
        m_type = EType.swigToEnum(type);
        m_arrayLen = arrayLen;
        m_moduleIdx = moduleIdx;
        m_partition = partition;
        m_partitionIdx = partitionIdx;
        m_scope = scope;
    }

    /** Returns name of the variable. */
    public String getName() {
        return m_name;
    }

    public String getBaseName() {
        return m_baseName;
    }

    /** Returns qualified name of the variable. */
    public String getQualifiedName() {
        return m_qualifiedName;
    }

    /** Returns type of the variable. */
    public String getVarTypeName() {
        return m_varTypeName;
    }

    /** Returns the number of bytes used by the variable. */
    public int getNumBytes() {
        return m_numBytes;
    }

    /** Returns length of array if variable is array, 0 for all other types. */
    public long getArrayLen() {
        return m_arrayLen;
    }

    /** Returns type as enumeration. */
    public EType getType() {
        return m_type;
    }

    /** Returns the name of source file, where variable is defined. */ 
    public int getModule() {
        return m_moduleIdx;
    }

    /** Returns the name of binary file, where variable is located. */
    public String getPartition() {
        return m_partition;
    }

    /** Returns the index of the binary file, where variable is located. */
    public int getPartitionIndex() {
        return m_partitionIdx;
    }

    /** Returns variable's scope, for example class name. */
    public String getScope() {
        return m_scope;
    }
    
    /**
     * Get simple name from qualified name
     * @param qName
     * @return
     */
    public static String getSimpleName(String qName) {
    	String name = qName;
    	// Remove module
    	int idx1 = name.indexOf("#");
		if (idx1 != -1) {
    		name = name.substring(idx1 + 1);
    	}
		// Remove partition
    	int idx2 = name.indexOf(",,");
		if (idx2 != -1) {
    		name = name.substring(0, idx2);
    	}
    	return name;
    }
    
    /**
     * Returns components of qualified name. This method is needed, because iConnect
     * returns name of the variable's children the same as it was called - if it
     * was called with qualified name, qualified name is returned.
     * See JUnit tests in iConnectDebug for usage examples.
     * 
     * @param qName
     * @return
     */
    public static void parseQualifiedName(String qName, 
                                          StringBuilder module, 
                                          StringBuilder name, 
                                          StringBuilder partition) {
        
        StringBuilder signature = new StringBuilder();
        JFunction.parseQualifiedName(qName, module, name, signature, partition, 
                                     m_qualifiedVarNameRegEx);
        
//        module.delete(0, module.length());
//        name.delete(0, name.length());
//        partition.delete(0, partition.length());
//
//        String regExp;
//        Pattern pattern;
//        Matcher matcher;
//
//        // extract module name
//        regExp = "(.*)\"(.*)\"#(.*)";
//        pattern = Pattern.compile(regExp);
//        matcher = pattern.matcher(qName);
//        // Removes module name and forwards qName as prefix+name+",,"+partitionName
//        if (matcher.matches()) {
//            /*System.out.println(regExp + "\nno of groups: " + matcher.groupCount());
//            for (int i = 0; i <= matcher.groupCount(); i++) {
//                System.out.println("grp" + i + ": " + matcher.group(i));
//            } */
//            String g1 = matcher.group(1);
//            String g2 = matcher.group(2);
//            String g3 = matcher.group(3);
//            
//            // prefix = matcher.group(1);
//            module.append(g2);
//            qName =  g1 + g3;
//            
//            // Like an assert for prefixes
//            if (g1 != null  &&  
//                g1.length() > 0  &&
//                !g1.startsWith("(")) {
//            }
//        }        
//        
//        // extract partition name
//        int ccIndex = qName.indexOf(",,");
//        if (ccIndex >= 0) {
//            partition.append(qName.substring(ccIndex + 2));
//            qName = qName.substring(0, ccIndex);
//        }
//        
//        // extract variable name
//        regExp = "(.*)#+(.*)";
//        pattern = Pattern.compile(regExp);
//        matcher = pattern.matcher(qName);
//        if (matcher.matches()) {
//            /* System.out.println(regExp + "\nno of groups: " + matcher.groupCount());
//            for (int i = 0; i <= matcher.groupCount(); i++) {
//                System.out.println("grp" + i + ": " + matcher.group(i));
//            } */
//            name.append(matcher.group(1));
//            name.append(matcher.group(2));
//        } else {
//            name.append(qName); // there wass no '#'
//        }
    }
    
    
    /**
     * Builds qualified name for global arrays.
     * 
     * @param module module name
     * @param index array index
     */
    public String buildGlobalQualifiedArrayName(String module, int index) {
        return buildGlobalQualifiedArrayName(module, m_baseName, m_partition, index);
    }

    
    /**
     * Builds qualified name for global pointers.
     */
    public String buildGlobalQualifiedPointerName() {
        String qualifiedName = "*" + m_qualifiedName; 

        return qualifiedName;
    }

    
    /**
     * Builds qualified name for global variables.
     * 
     * @param moduleName
     * @param variableName
     * @param partition
     * @return
     */
    static public String buildGlobalQualifiedName(String moduleName, String scope, String variableName, String partition) {
        StringBuilder sb = new StringBuilder();
        
        if (!moduleName.isEmpty()) {
            sb.append('"').append(moduleName).append("\"#");
        }
        
        if (scope != null  &&  !scope.isEmpty()) {
            sb.append(scope).append("::");
        }
        
        sb.append(variableName);
        
        if (!partition.isEmpty()) {
            sb.append(",,").append(partition);
        }

        return sb.toString();
    }
    
    
    static public String buildGlobalQualifiedArrayName(String module, 
                                                       String baseName, 
                                                       String partition,
                                                       int index) {
        
        Matcher matcher = m_patternForArrays.matcher(baseName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid variable base name: " + baseName);
        }
        
        String prefix = matcher.group(1);
        String varName = matcher.group(2);
        
        StringBuilder qualifiedName = new StringBuilder();
        qualifiedName.append(prefix);
        
        if (!module.isEmpty()) {
            qualifiedName.append('"' + module + "\"#");
        }
        
        qualifiedName.append(varName).append('[').append(index).append(']');

        if (!partition.isEmpty()) {
            qualifiedName.append(",,").append(partition);
        }

        return qualifiedName.toString();
    }

    public String getDescription() {
        return getDescription(null);
    }
    
    
    public String getDescription(String moduleName) {
        return String.format("%s %s%s%s", 
                this.getVarTypeName(),
                this.getName(),
                (getPartition() != null  &&  getPartition().trim().length() > 0 ? String.format("\nPartition: %s", getPartition().trim()) : ""),
                (moduleName != null &&  moduleName.trim().length() > 0? String.format("\nModule: %s", moduleName.trim()) : ""));
    }
    
    @Override
    public int hashCode() {
        return m_qualifiedName.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj != null  &&
                obj instanceof JVariable  &&
                ((JVariable)obj).getQualifiedName().equals(this.m_qualifiedName);
    }

    @Override
    public String toString() {
        return m_qualifiedName;
    }

    @Override
    public int compareTo(JVariable o) {
        int c = this.getBaseName().compareTo(o.getBaseName());
        if (c == 0) {
            this.getName().compareTo(o.getName());
        }
        if (c == 0) {
            this.getQualifiedName().compareTo(o.getQualifiedName());
        }
        return c;
    }
}