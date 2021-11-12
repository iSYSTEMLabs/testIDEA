package si.isystem.connect.adapters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import si.isystem.connect.CDataController2;
import si.isystem.connect.CHILChannel;
import si.isystem.connect.CHILController;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CIDEController.EPathType;
import si.isystem.connect.CPropertyWrapper;
import si.isystem.connect.CRegisterInfo;
import si.isystem.connect.CSystemMemoryAreas;
import si.isystem.connect.CValueType;
import si.isystem.connect.EEndian;
import si.isystem.connect.EOptions;
import si.isystem.connect.HILChannelVector;
import si.isystem.connect.ICPUSFR;
import si.isystem.connect.IConfiguration;
import si.isystem.connect.IConnectDebug.EAccessFlags;
import si.isystem.connect.IConnectDebug.EEvaluate;
import si.isystem.connect.IConnectEclipse;
import si.isystem.connect.IConnectEclipse.EGetGlobalsFlags;
import si.isystem.connect.IDataDescriptor;
import si.isystem.connect.IDataDescriptor.EType;
import si.isystem.connect.IDescriptor;
import si.isystem.connect.IDisassemblyBlock;
import si.isystem.connect.IDisassemblyLine;
import si.isystem.connect.IEnumMap;
import si.isystem.connect.IExpressionType;
import si.isystem.connect.IFunction;
import si.isystem.connect.IGlobals;
import si.isystem.connect.IItemDescriptor;
import si.isystem.connect.IModule;
import si.isystem.connect.IPartition;
import si.isystem.connect.ISFR;
import si.isystem.connect.IStackFrame;
import si.isystem.connect.IStackFrameInfo;
import si.isystem.connect.IType;
import si.isystem.connect.ITypedef;
import si.isystem.connect.IVariable;
import si.isystem.connect.IVectorDisassemblyLines;
import si.isystem.connect.IVectorEnumMaps;
import si.isystem.connect.IVectorFunctions;
import si.isystem.connect.IVectorItemDescriptors;
import si.isystem.connect.IVectorModules;
import si.isystem.connect.IVectorPartitions;
import si.isystem.connect.IVectorSFRs;
import si.isystem.connect.IVectorStackFrames;
import si.isystem.connect.IVectorTypedefs;
import si.isystem.connect.IVectorTypes;
import si.isystem.connect.IVectorValueMap;
import si.isystem.connect.IVectorVariables;
import si.isystem.connect.SType;
import si.isystem.connect.SType2.EType2;
import si.isystem.connect.StrVector;
import si.isystem.connect.VariableVector;
import si.isystem.connect.VectorBYTE;
import si.isystem.connect.connect;
import si.isystem.connect.data.JCValueType;
import si.isystem.connect.data.JCompoundType;
import si.isystem.connect.data.JDataDescriptor;
import si.isystem.connect.data.JDisassemblyInstruction;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JHilChannel;
import si.isystem.connect.data.JModule;
import si.isystem.connect.data.JPartition;
import si.isystem.connect.data.JProperty;
import si.isystem.connect.data.JRegisterDescriptor;
import si.isystem.connect.data.JSFR;
import si.isystem.connect.data.JSType;
import si.isystem.connect.data.JSType2;
import si.isystem.connect.data.JStackFrame;
import si.isystem.connect.data.JTypeInfo;
import si.isystem.connect.data.JTypedef;
import si.isystem.connect.data.JVarAttribute;
import si.isystem.connect.data.JVariable;
import si.isystem.connect.data.JVersion;

/**
 * This class retrieves data from isystem.connect and copies these data to
 * pure Java classes.
 *  
 * @author markok
 *
 */
public class JDataController {

    // references to native objects
    private CHILController m_hilCtrl;
    private CIDEController m_ideCtrl;
    private CDataController2 m_dataCtrl2;
    
    private JVersion m_winIDEAVersion;

    public static final int PARTITION_DEFAULT = -1;
    public static final int PARTITION_CURRENT = -2;
    
    
    /**
     * Creates Java instance of data controller.
     * 
     * @param dataCtrl2 data controller
     * @param hilCtrl hil controller
     * @param ideCtrl ide controller
     * @param winIDEAVersion used to avoid calling iconnect functions, which
     *                       were not present in earlier versions of winIDEA. 
     */
    public JDataController(CDataController2 dataCtrl2, 
                           CHILController hilCtrl,
                           CIDEController ideCtrl) {
        m_dataCtrl2 = dataCtrl2;
        m_hilCtrl = hilCtrl;
        m_ideCtrl = ideCtrl;
        m_winIDEAVersion = new JVersion(ideCtrl.getWinIDEAVersion()); 
    }
  
    
    public int getNumberOfPartitions() {
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();

        m_dataCtrl2.release(configuration);

        return numPartitions;
    }
    
    
    public JPartition[] getPartitions() {
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions iPartitions = configuration.Partitions();
        int numPartitions = (int)iPartitions.size();
        JPartition[] jPartitions = new JPartition[numPartitions];
        
        for (int partition = 0; partition < numPartitions; partition++) {
            IPartition iPart = iPartitions.at(partition);
            jPartitions[partition] = new JPartition(iPart);
        }
        
        m_dataCtrl2.release(configuration);
        
        return jPartitions;
    }
    
    public int getPartitionIndex(String partitionName) {
        int partitionIdx = -1;
        
        // Partition is currently unknown with local variables
        if (partitionName != null  &&  partitionName.length() > 0) {
            IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
            IVectorPartitions partitions = configuration.Partitions();
            int numPartitions = (int)partitions.size();
            
            for (int i = 0; i < numPartitions; i++) {
                IPartition dlFile = partitions.at(i);
                if (dlFile.Name().equals(partitionName)) {
                    partitionIdx = i;
                    break;
                }
            }
            
            m_dataCtrl2.release(configuration);
        }
        
        return partitionIdx;
    }

    
    /**
     * Returns a list of source files for the given download file.
     * @param partitionIndex can be JDataController.PARTITION_DEAFAULT,
     *                       JDataController.PARTITION_CURRENT or any non-negative
     *                       value to be used as partition index. 
     */
    public JModule[] getModules(int partitionIndex) {

        IGlobals globals = m_dataCtrl2.getGlobals(
                EGetGlobalsFlags.gvfModules.swigValue() | 
                getPartitionFlag(partitionIndex).swigValue(),
                partitionIndex);

        JModule[] jModules = cModules2JModules(globals);
        m_dataCtrl2.release(globals);

        return jModules;
    }
    
    
    public String getModuleName(int partitionIndex, int moduleIndex) {

        IGlobals globals = m_dataCtrl2.getGlobals(
                EGetGlobalsFlags.gvfModules.swigValue() | 
                getPartitionFlag(partitionIndex).swigValue(),
                partitionIndex);

        String moduleName = "";
        IVectorModules modules = globals.Modules();
        if (modules.size() > moduleIndex) { 
            IModule module = modules.at(moduleIndex);
            moduleName = module.Name();
        }
        
        m_dataCtrl2.release(globals);

        return moduleName;
    }
    
    
    public int getModuleIndex(int partitionIndex, String moduleName) {
        int moduleIndex = -1;

        IGlobals globals = m_dataCtrl2.getGlobals(
                EGetGlobalsFlags.gvfModules.swigValue() | 
                getPartitionFlag(partitionIndex).swigValue(),
                partitionIndex);

        IVectorModules modulesVector = globals.Modules();
        int numModules = (int)modulesVector.size();
        for (int i = 0; i < numModules; i++) {
            IModule module = modulesVector.at(i);
            if (module.Name().equals(moduleName)) {
                moduleIndex = i;
                break;
            }
        }
        
        m_dataCtrl2.release(globals);

        return moduleIndex;
    }

    
    private JModule[] cModules2JModules(IGlobals globals) {
        
        IVectorModules modulesVector = globals.Modules();
        int numModules = (int)modulesVector.size();
        JModule[] modules = new JModule[numModules];
        for (int i = 0; i < numModules; i++) {
            IModule module = modulesVector.at(i);
            
            modules[i] = new JModule(module.Name(), module.Path());
        }
        
        return modules;
    }


    /**
     * Returns list of global functions for the default download file.
     */
    public JFunction[] getGlobalFunctions() {

        IGlobals globals = m_dataCtrl2.getGlobals(
                EGetGlobalsFlags.gvfFunctions.swigValue() |
                EGetGlobalsFlags.gvfSortAddress.swigValue() |
                EGetGlobalsFlags.gvfPartitionDefault.swigValue(),
                0);

                JModule[] modules = getModules(PARTITION_DEFAULT);
        JFunction[] jFunctions= cFunctions2JFunctions(globals, 
                                                      modules, 
                                                      getDefaultDownloadFileName());
        m_dataCtrl2.release(globals);
        
        return jFunctions;
    }

    
    /**
     * Returns list of global functions for the given download file.
     */
    public JFunction[] getGlobalFunctions(int partitionIndex, String partitionName) {

        JModule[] modules = getModules(partitionIndex);
        
        IGlobals globals = m_dataCtrl2.getGlobals(
                EGetGlobalsFlags.gvfFunctions.swigValue() |
                EGetGlobalsFlags.gvfSortAddress.swigValue() |
                getPartitionFlag(partitionIndex).swigValue(),
                partitionIndex);

        JFunction[] jFunctions= cFunctions2JFunctions(globals, 
                                                      modules, 
                                                      partitionName);
        m_dataCtrl2.release(globals);

        return jFunctions;
    }


    private JFunction[] cFunctions2JFunctions(IGlobals globalsForPartition, 
                                              JModule [] modulesForPartition, 
                                              String partitionName) {
        IVectorFunctions functionsVector = globalsForPartition.Functions();
        int numFunctions = (int)functionsVector.size();
        JFunction[] functions = new JFunction[numFunctions];
        
        for (int i = 0; i < numFunctions; i++) {
            IFunction function = functionsVector.at(i);
            
            String moduleName = getModuleName(function, modulesForPartition, partitionName); 
            functions[i] = cFunction2JFunction(function, moduleName, partitionName);
        }
        
        return functions;
    }


    private JFunction cFunction2JFunction(IFunction function, 
                                          String moduleName, 
                                          String partitionName) {
        
        if (function == null) {
            return null;
        }

        JVariable[] paramsArray = cVariables2JVariables(function.Parameters(), "", -1);
        JVariable[] localVarsArray = cVariables2JVariables(function.Variables(), "", -1);
        
        long[] callsFrom = JFunction.getCallsFromFunc(function);        
        
        return new JFunction(function.Name(),
                             function.Scope(),
                             function.ReturnTypeName(),
                             function.SignatureName(),
                             function.Module(),
                             moduleName,
                             function.QualifiedName(),
                             partitionName,
                             paramsArray,
                             localVarsArray,
                             function.HasVarParams(),
                             callsFrom);
    }


    private JVariable[] cVariables2JVariables(IVectorVariables vars, String partitionName, int partitionIdx) {
        JVariable[] paramsArray;
        int varSize = (int)vars.size();
        paramsArray = new JVariable[varSize]; 
        for (int varIdx = 0; varIdx < varSize; varIdx++) {
            paramsArray[varIdx] = new JVariable(vars.at(varIdx), partitionName, partitionIdx, true); 
        }
        return paramsArray;
    }
    

    /** 
     * Returns list of global variables as array of strings. Each string 
     * contains variable name followed by ' - ' and type name, for example:
     * 'iCounter - int'.
     */
    public String[] getGlobalVarsAsStringArray() {
        
        JVariable[] vars = getGlobalVariables();
        String[] varsAsStrArray = new String[vars.length];
        
        int idx = 0;
        for (JVariable var : vars) {
            StringBuilder varName = new StringBuilder();
            StringBuilder varPartition = new StringBuilder();
            StringBuilder varModule = new StringBuilder();
            JVariable.parseQualifiedName(var.getQualifiedName(), varModule, varName, varPartition);
            
            varsAsStrArray[idx++] = varName + "     - " + var.getVarTypeName() + 
                                    ", " + varPartition + ", " + varModule;
        }
        return varsAsStrArray;
    }

    
    /**
     * Returns array of all global variables in the system.
     */
    public JVariable[] getGlobalVariables() {
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();
        List<JVariable> jVariables = new ArrayList<>();
        
        for (int partitionIdx = 0; partitionIdx < numPartitions; partitionIdx++) {
            IGlobals globals = 
                m_dataCtrl2.getGlobals(
                        EGetGlobalsFlags.gvfVariables.swigValue() | 
                        EGetGlobalsFlags.gvfSortName.swigValue() |
                        EGetGlobalsFlags.gvfModules.swigValue(), 
                        partitionIdx);
        
            IVectorModules modules = globals.Modules();
                
            IVectorVariables cVariables = globals.Variables();

            int numVariables = (int)cVariables.size();

            for (int idx = 0;idx < numVariables; idx++) {
                IVariable cVar = cVariables.at(idx);
                String partitionName = partitions.at(partitionIdx).Name();
                String scope = cVar.Scope();
                int moduleIdx = cVar.Module();
                String moduleName = modules.at(moduleIdx).Name();

                String qualifiedName = JVariable.buildGlobalQualifiedName(moduleName, scope, cVar.Name(), partitionName);
                
                jVariables.add(new JVariable(cVar.Name(), 
                                             qualifiedName,
                                             cVar.TypeName(),
                                             (int)cVar.NumBytes(),
                                             cVar.Type(),
                                             cVar.ArrayDimension(),
                                             moduleIdx,
                                             partitionName,
                                             partitionIdx,
                                             scope));
            }

            m_dataCtrl2.release(globals);
        }

        m_dataCtrl2.release(configuration);
        
        return jVariables.toArray(new JVariable[0]);
    }
    

    /** 
     * Returns list of macros as array of strings. Each string 
     * contains macro name.
     * 
     * @param values list to receive values of macros, may be null.
     */
    public String[] getMacros(List<String> valuesList) {

        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();
        List<String> macros = new ArrayList<>();
        
        for (int partition = 0; partition < numPartitions; partition++) {
            StrVector names = new StrVector();
            StrVector values = new StrVector();
        
            m_dataCtrl2.getMacros(partition, names, values);
        
            int numMacros = (int) names.size();
            for (int idx = 0; idx < numMacros; idx++) {
                macros.add(names.get(idx));
                if (valuesList != null) {
                    valuesList.add(values.get(idx));
                }
            }
        }
        
        m_dataCtrl2.release(configuration);
        
        return macros.toArray(new String[0]);
    }

    
    public Set<String> getMacroNames(int partitionIndex) {
        StrVector names = new StrVector();
        StrVector values = new StrVector();
        m_dataCtrl2.getMacros(partitionIndex, names, values);

        Set<String> set = new HashSet<>();

        for (int i = 0; i < names.size(); i++) {
            set.add(names.get(i));
        }
        
        return set;
    }


    /** Returns code labels for all partitions. */
    
    public String[] getCodeLabels() {
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();
        List<String> labelsList = new ArrayList<>();
        
        for (int partition = 0; partition < numPartitions; partition++) {
            labelsList.addAll(getCodeLabels(partition));
        }
        
        m_dataCtrl2.release(configuration);
        
        return labelsList.toArray(new String[0]);
    }
    
    
    /** 
     * Returns list of code labels for the given partition.
     */
    public List<String> getCodeLabels(int partition) {

        List<String> labelsList = new ArrayList<>();

        VariableVector labels = new VariableVector();
        m_dataCtrl2.getLabels(partition, labels);
    
        int numlabels = (int) labels.size();
        for (int idx = 0; idx < numlabels; idx++) {
            labelsList.add(labels.get(idx).getName());
        }

        return labelsList;
    }

    
    /** 
     * This method returns all data types in the executable. Function types are not 
     * included, but typedefs referring to data types are. 
     */
    public Set<String> getDataTypes() {
        Set<String> typeNames = new TreeSet<>();
        
        JTypeInfo[] realTypes = getTypes();
        JTypedef[] typedefs = getTypedefs();
        
        if (typedefs == null || realTypes == null) {
            return new TreeSet<>();  // feature not supported in winIDEA older than 9.11.15
        }
        
        for (JTypeInfo type : realTypes) {
            
            String typeName = type.getTypeName().trim();
            
            if (typeName.isEmpty()) {
                continue;  // workaround for bug in winIDEA (5554) - there is one empty item 
            }
            
            if (type.getType().getTypeAsEnum() != SType.EType.tCompound) {
                typeNames.add(typeName);
                
            } else {
                
                EType2 type2 = type.getType().getType2AsEnum();
                
                if (type2 == EType2.t2Class  ||  type2 == EType2.t2Struct  ||
                    type2 == EType2.t2Union  ||  type2 == EType2.t2Enum) {

                    typeNames.add(typeName);
                }
            }
        }
        
        for (JTypedef typedef : typedefs) {
                typeNames.add(typedef.getTypedefName());
        }
        
        return typeNames;
    }
    
    
    /**
     * Returns array of all 'real' (without typedefs) types in the system, or 
     * null if winIDEA is older than 9.11.15
     * 
     * @see getTypedefs
     */
    public JTypeInfo[] getTypes() {
        
        if (m_winIDEAVersion.compareTo(new JVersion(9, 11, 15)) < 0) {
             return null;
        }
        
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();
        List<JTypeInfo> jTypes = new ArrayList<>();
        
        for (int partition = 0; partition < numPartitions; partition++) {
            IGlobals globals = 
                m_dataCtrl2.getGlobals(
                        EGetGlobalsFlags.gvfTypes.swigValue(), 
                        partition);
        
            IVectorTypes types = globals.Types();
            int numTypes = (int)types.size();
            for (int typeIdx = 0; typeIdx < numTypes; typeIdx++) {
                IType type = types.at(typeIdx);
                jTypes.add(new JTypeInfo(type.TypeName(), new JSType2(type.Type())));
            }
            
            m_dataCtrl2.release(globals);
        }

        m_dataCtrl2.release(configuration);
        
        return jTypes.toArray(new JTypeInfo[0]);
    }
    
    
    /**
     * Returns array of all typedefs in the system, or null if winIDEA is
     * older than 9.11.15
     * 
     * @see getTypes
     */
    public JTypedef[] getTypedefs() {

        if (m_winIDEAVersion.compareTo(new JVersion(9, 11, 15)) < 0) {
            return null;
        }
        
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();
        List<JTypedef> jTypes = new ArrayList<>();
        
        for (int partition = 0; partition < numPartitions; partition++) {
            IGlobals globals = 
                m_dataCtrl2.getGlobals(
                        EGetGlobalsFlags.gvfTypedefs.swigValue(), 
                        partition);
        
            IVectorTypedefs types = globals.Typedefs();
            int numTypes = (int)types.size();
            for (int typeIdx = 0; typeIdx < numTypes; typeIdx++) {
                ITypedef type = types.at(typeIdx);
                jTypes.add(new JTypedef(type.Name(), type.TypeName()));
            }
            
            m_dataCtrl2.release(globals);
        }

        m_dataCtrl2.release(configuration);
        
        return jTypes.toArray(new JTypedef[0]);
    }
    
    
    /**
     * Returns members of complex data types, for example structure members.
     * 
     * @param typeName name of data type, usually structure or union.
     * @return
    public String [] getExpressionChildren(String expression) {
        IExpressionType iExpr = m_dataCtrl2.getExpressionType(0, expression);
        
        IVectorVariables children = iExpr.Children();
        int numChildren = (int)children.size();
        String []members = new String[numChildren];
        
        for (int i = 0; i < numChildren; i++) {
            IVariable var = children.at(i);
            members[i] = var.Name();
        }
        
        return members;
    }
     */
    
    
    /**
     * Returns type info for the given C expression or variable name, including 
     * type info for all of its children.
     * 
     * @param expression C expression, which type info we want to get
     * 
     * @param partitionName name of the partition this expression (variable)
     * belongs to. May be empty string, if not known.
     * 
     * @param partitionIdx index of the partition this expression (variable)
     * belongs to. Set it to -1, if not known.
     *   
     * @return object containing information about the given expression
     * and all it's children. <b>IMPORTANT:</b> Partition name and index in the
     * returned object are set according to parameters given to this method.
     *  
     */
    public JCompoundType getExpressionCompoundTypeInfo(String expression, 
                                                       String partitionName,
                                                       int partitionIdx) {

        IExpressionType iExpr = m_dataCtrl2.getExpressionType(0, expression);

        JCompoundType type = new JCompoundType(iExpr, partitionName, partitionIdx);
        
        m_dataCtrl2.release(iExpr);

        return type;
    }
    
    
    /**
     * Returns type info for the given C expression or variable name.
     * 
     * @param expression C expression, which type info we want to get
     * 
     * @param partitionName name of the partition this expression (variable)
     * belongs to. May be empty string, if not known.
     * 
     * @param partitionIdx index of the partition this expression (variable)
     * belongs to. Set it to -1, if not known.
     *   
     * @return object containing information about the given expression.
     * <b>IMPORTANT:</b> Partition name and index in the
     * returned object are set according to parameters given to this method.
     *  
     */
    public JVariable getExpressionTypeInfo(String expression, 
                                           String partitionName,
                                           int partitionIdx) {

        IExpressionType iExpr = m_dataCtrl2.getExpressionType(0, expression);

        // see also JCompundType
        JVariable jvar = new JVariable(iExpr.Expression(), partitionName, partitionIdx, true);
        
        m_dataCtrl2.release(iExpr);

        return jvar;
    }
    
    /**
     * This method returns array of available HIL parameters as strings. 
     * 
     * @param hilParameters map to be filled with HIL parameters. May be null, if
     * only return value is needed. 
     * @return
     */
    public String[] getHilParametersAsPaths(Map<String, JDataDescriptor> hilParameters) {
        
        JDataDescriptor[] params = getHilParameters();
        
        if (hilParameters == null) {
            hilParameters = new TreeMap<>();
        }
        
        for (JDataDescriptor descriptor : params) {
            dumpDataDescriptorsAsPaths(hilParameters, "", descriptor);
        }
        
        String strPaths[] = new String[hilParameters.size()];
        int idx = 0;
        for (String path : hilParameters.keySet()) {
            strPaths[idx++] = path;
        }
        
        return strPaths;
    }

    
    /**
     * This method returns array of HIL parameter descriptors.
     */
    public JDataDescriptor[] getHilParameters() {
        try {
            IVectorItemDescriptors varDescriptors = m_hilCtrl.getDescriptors();

            int numDescriptors = (int)varDescriptors.size();
            JDataDescriptor[] hilParams = new JDataDescriptor[numDescriptors];

            for (int descriptorIdx = 0; descriptorIdx < numDescriptors; descriptorIdx++) {
                hilParams[descriptorIdx] = dataDescriptor2Java(varDescriptors.at(descriptorIdx));
            }

            return hilParams;
        } catch (IllegalArgumentException ex) {
            // if there is no hil module, empty array is returned
            return new JDataDescriptor[0];
        }
    }
    
    
    /** 
     * @deprecated channels are no longer available, use getHilParameters() 
     *             instead.
     */
    public JHilChannel[] getHilChannels() {
        HILChannelVector vec = new HILChannelVector();
        m_hilCtrl.getChannels(vec);
        JHilChannel[] jv = new JHilChannel[(int)(vec.size())];
        for (int hi = 0; hi < vec.size(); hi++) {
            CHILChannel c = vec.get(hi);
            jv[hi] = new JHilChannel(
                    c.getName(),
                    c.getQualifiedName(),
                    c.getType(),
                    c.isAvailable(),
                    c.getUnit(),
                    c.getAMin(),
                    c.getAMax(),
                    c.getIndex());
        }
        return jv;
    }
    

    public JDataDescriptor[] getDataDescriptors(String url) {

        checkIdeCtrl();
        
        IDescriptor descriptor = m_ideCtrl.getOptionDataDescriptor(url);
        IVectorItemDescriptors varDescriptors = descriptor.VariableDescriptors();

        int numDescriptors = (int)varDescriptors.size();
        JDataDescriptor[] dataDescriptors = new JDataDescriptor[numDescriptors];

        for (int descriptorIdx = 0; descriptorIdx < numDescriptors; descriptorIdx++) {
            dataDescriptors[descriptorIdx] = dataDescriptor2Java(varDescriptors.at(descriptorIdx));
        }

        m_ideCtrl.release(descriptor);
        
        return dataDescriptors;
    }


    private void checkIdeCtrl() {
        if (m_ideCtrl == null) {
            throw new IllegalStateException("m_ideCtrl == null, instantiate JDataController " +
            		       "with proper ctor and initialized IDE controller!");
        }
    }
    
    
    /**
     * Returns the number of available stack frames. This method is much faster 
     * than calling getStackFrames(int, int), because it does not copy all stack
     * frames from C++ structures to Java structures. 
     *   
     * @return the number of available stack frames.
     */
    public int getStackFramesCount() {
        IStackFrameInfo stackFrameinfo = m_dataCtrl2.getStackFrameInfo(false, true);

        int stackFramesCount = (int)stackFrameinfo.StackFrames().size();
        
        m_dataCtrl2.release(stackFrameinfo);
        
        return stackFramesCount;
    }
    

    /**
     * Returns requested stack frames. Active stack frame has index 0. Note that 
     * the stack trace retrieval depend on the available debug info, and that it
     * is not always available or 100% accurate.
     * 
     * @param lowIdx index of the first stack frame to be returned
     * @param len the number of frames to return. If -1, all frames from the given
     * index on are returned. If the number of available stack fames is less than n,
     * only the available stack frames are returned - no exception is thrown.
     * @param isActiveFrameOnly if true, only the currently active stack frame is returned 
     * 
     * @throws IndexOutOfBoundsException if lowIdx falls out of range.
     */
    public JStackFrame[] getStackFrames(int lowIdx, int len, boolean isActiveFrameOnly) {
        
        IStackFrameInfo stackFrameinfo = m_dataCtrl2.getStackFrameInfo(isActiveFrameOnly,
        															   true);

        IVectorStackFrames cStackFrames = stackFrameinfo.StackFrames();
                                          
        int numStackFrames = (int)cStackFrames.size();
        
        if (lowIdx + len > numStackFrames) {
            m_dataCtrl2.release(stackFrameinfo);
            throw new IndexOutOfBoundsException("Stack frame index out range. " +
                                                "(requestedIdx, requestedLen, availableLen): " 
                                                + lowIdx + ", " + len + ", " + numStackFrames);
        }
        
        if (len != -1) {
            if (lowIdx + len > numStackFrames) {
                len = numStackFrames - lowIdx;
            }
        } else {
            len = numStackFrames - lowIdx;
        }
        
        JStackFrame[] frames = new JStackFrame[len];
        JPartition[] partitions = getPartitions();
        int prevPartitionIdx = -1;
        JModule[] modules = null;
        
        for (int i = 0; i < len; i++) {
            IStackFrame cFrame = cStackFrames.at(lowIdx + i);
            
            int partitionIdx = (int)cFrame.Partition();
            String partitionName = partitions[partitionIdx].getName();

            // optimization - do not reload modules if already available
            if (prevPartitionIdx != partitionIdx  ||  modules == null) {
                modules = getModules(partitionIdx);
            }
            prevPartitionIdx = partitionIdx;
                    
            IFunction cFunction = cFrame.Function();
            JFunction jFunction = null;
            
            if (cFunction != null) {
                String moduleName = getModuleName(cFunction, modules, partitionName);
                jFunction = cFunction2JFunction(cFunction, moduleName, partitionName);
            }
            
            JVariable[] vars = getVariables(cFrame.Variables(), partitionName, partitionIdx);
            
            frames[i] = new JStackFrame(lowIdx + i, 
                                        cFrame.FileName(), 
                                        (int)cFrame.LineNumber(), 
                                        cFrame.MemoryArea(),
                                        cFrame.Address(),
                                        jFunction, 
                                        vars);
        }
        
        m_dataCtrl2.release(stackFrameinfo);
        
        return frames;
    }


    private String getModuleName(IFunction cFunction,
                                 JModule[] modules,
                                 String partitionIdx) {
        int moduleIdx = cFunction.Module();
        String moduleName;
        if (moduleIdx >= modules.length) {
            moduleName = "invalid_module_" + moduleIdx + "_" + 
                                        partitionIdx + "_" + modules.length;
        } else {
            moduleName = modules[moduleIdx].getName();
        }
        return moduleName;
    }
    

    private JVariable[] getVariables(IVectorVariables vars, String partitionName, int partitionIdx) {
        
        int varSize = (int)vars.size();
        JVariable[] varsArray = new JVariable[varSize]; 

        for (int varIdx = 0; varIdx < varSize; varIdx++) {
            varsArray[varIdx] = new JVariable(vars.at(varIdx), partitionName, partitionIdx, true); 
        }
        
        return varsArray;
    }
    
    
    private void dumpDataDescriptorsAsPaths(Map<String, JDataDescriptor> strPaths,
                                            String parentPath,
                                            JDataDescriptor descriptor) {
     
        String path = parentPath + descriptor.getName();
        
        if (descriptor.getType() != EType.tSTRUCT) {
            strPaths.put(path, descriptor);
        } else {

            JDataDescriptor[] children = descriptor.getChildren();

            for (JDataDescriptor childDescriptor : children) {
                dumpDataDescriptorsAsPaths(strPaths, path + connect.PathSeparator('\0'), childDescriptor);
            }
        }
    }
    

    /**
     * Reads all data from varDescriptor and copies it to JDataDescriptor, including
     * children. Class hierarchy is slightly different from C++ one - only one
     * class (JDataDescriptor) is used for parent and children.
     * 
     * @param varDescriptor variable descriptor to be copied to Java class
     * @return
     */
    private JDataDescriptor dataDescriptor2Java(IItemDescriptor varDescriptor) {
        
        IDataDescriptor cDataDescriptor = varDescriptor.DataDescriptor();
        
        // enum mappings for enums (scalar enums and enum elements in arrays)
        List<JDataDescriptor.EnumProperties> enumList = new ArrayList<>();
        if (cDataDescriptor.Type() == IDataDescriptor.EType.tENUM) {
            IVectorEnumMaps enumMaps = cDataDescriptor.Enum();
            int noOfEnums = (int)enumMaps.size();
            for (int i = 0; i < noOfEnums; i++) {

                IEnumMap enumMapping = enumMaps.at(i);
                long enumOptionsAsLong = enumMapping.Options();

                EnumSet<EOptions> enumOptions = dataDescriptorEOptions2EnumSet(enumOptionsAsLong);

                enumList.add(new JDataDescriptor.EnumProperties(enumMapping.Name(), 
                                                                enumMapping.Value(), 
                                                                enumMapping.Info(), 
                                                                enumOptions));
            }
        }
        
        // enum mapping for array indices - arrays may be indexed by integers
        // or enum constants. Mapping from enum strings to integers is provided by
        // this mapping
        List<JDataDescriptor.EnumProperties> enumIndexList = new ArrayList<>();
        if (varDescriptor.Dimension() > 0) {  // varDescriptor describes array
            IVectorEnumMaps enumMaps = varDescriptor.ArrayIndex();
            int noOfEnums = (int)enumMaps.size();
            for (int i = 0; i < noOfEnums; i++) {

                IEnumMap enumMapping = enumMaps.at(i);
                long enumOptionsAsLong = enumMapping.Options();

                EnumSet<EOptions> enumOptions = dataDescriptorEOptions2EnumSet(enumOptionsAsLong);

                enumIndexList.add(new JDataDescriptor.EnumProperties(enumMapping.Name(), 
                                                                     enumMapping.Value(), 
                                                                     enumMapping.Info(), 
                                                                     enumOptions));
            }
        }
        
        
        // adds children recursively
        JDataDescriptor structChildren[] = new JDataDescriptor[0];
        if (cDataDescriptor.Type() == IDataDescriptor.EType.tSTRUCT) {
            IVectorItemDescriptors children = cDataDescriptor.Struct();
            int noOfChildren = (int) children.size();
            structChildren = new JDataDescriptor[noOfChildren];
            for (int i = 0; i < noOfChildren; i++) {
                structChildren[i] = dataDescriptor2Java(children.at(i));
            }
        }
        
        // create
        JDataDescriptor descriptor = 
            new JDataDescriptor(0, // address or offset
                                varDescriptor.Name(), 
                                (int)varDescriptor.Dimension(), 
                                varDescriptor.Info(), 
                                dataDescriptorEOptions2EnumSet(varDescriptor.Options()), 
                                cDataDescriptor.Type(), 
                                cDataDescriptor.Size(), 
                                enumList,
                                enumIndexList,
                                cDataDescriptor.Container(),
                                structChildren);

        return descriptor;
    }


    private EnumSet<EOptions> dataDescriptorEOptions2EnumSet(long enumOptionsAsLong) {
        EnumSet<EOptions> optionsSet = EnumSet.noneOf(EOptions.class);
        
        // handle options that are marked by single bit
        EOptions[] allEnumValues = EOptions.values();
        long bitMaskValues = enumOptionsAsLong & 0xff00;
        for (EOptions e : allEnumValues) {
            if ((e.swigValue() & bitMaskValues) != 0) {
                optionsSet.add(e);
            }
        }
        
        // handle options specified as values in part of flags
        long kindEnums = enumOptionsAsLong & EOptions.optKindMask.swigValue();
        for (EOptions e : allEnumValues) {
            if (e.swigValue() == kindEnums) {
                optionsSet.add(e);
            }
        }
        
        return optionsSet;
    }

    /**
     * All members of return value are set. Works only for simple types. For
     * complex types throws IOException.
     */
    public JCValueType evaluate(EAccessFlags accessFlags,
                                EEvaluate evaluateFlags,
                                String expression) {
        CValueType valType = m_dataCtrl2.evaluate(accessFlags, 
                                                  evaluateFlags, 
                                                  expression);
        return new JCValueType(valType);
    }

    public JCValueType evaluate(
                EAccessFlags accessFlags,
                String expression) {
        return evaluate(accessFlags, EEvaluate.efNoMemAreaDisplay, expression);
    }
    
    public JDisassemblyInstruction[] getDisassembly(long flags, 
                                                    String fileName, 
                                                    int lineNumber, 
                                                    int numLines,
                                                    int codeMemArea) {
        
        IDisassemblyBlock disassemblyBlock = m_dataCtrl2.getDisassembly(flags,
                                                                        fileName, 
                                                                        lineNumber, 
                                                                        numLines);
        IVectorDisassemblyLines lines = disassemblyBlock.Lines();
        int retLines = (int)lines.size();
        JDisassemblyInstruction[] result = new JDisassemblyInstruction[retLines];
        
        for (int i = 0; i < retLines; i++) {
            IDisassemblyLine line = lines.at(i); 
            
            result[i] = new JDisassemblyInstruction(line.FileName(),
                                                    (int)line.LineNumber(),
                                                    line.FunctionName(),
                                                    codeMemArea,
                                                    line.Address(),
                                                    line.Length(),
                                                    line.OpCode(),
                                                    line.OpCodeArgs(),
                                                    "",
                                                    line.FunctionOffset());
        }
        
        m_dataCtrl2.release(disassemblyBlock);
        return result;
    }
    
    public JDisassemblyInstruction[] getDisassembly(long flags,
                                                    short memArea,
                                                    long startAddress, 
                                                    long endAddress) {
        long numMAUs = endAddress - startAddress;

        IDisassemblyBlock disassemblyBlock = m_dataCtrl2.getDisassembly(flags,
                                                                        memArea,
                                                                        startAddress,
                                                                        numMAUs);

        IVectorDisassemblyLines lines = disassemblyBlock.Lines();

        int retLines = (int)lines.size();
        
        JDisassemblyInstruction[] result = new JDisassemblyInstruction[retLines];
        
        for (int i = 0; i < retLines; i++) {
            IDisassemblyLine line = lines.at(i);
            
            result[i] = new JDisassemblyInstruction(line.FileName(),
                                                    (int)line.LineNumber(),
                                                    line.FunctionName(),
                                                    memArea,
                                                    line.Address(),
                                                    line.Length(),
                                                    line.OpCode(),
                                                    line.OpCodeArgs(),
                                                    line.OpCode() + "  " + line.OpCodeArgs(),
                                                    line.FunctionOffset());
        }
        
        m_dataCtrl2.release(disassemblyBlock);
        return result;
    }
    

    /**
     * Returns information provided by the following MI commands:
     * -stack-select-frame
     * -stack-list-locals 0
     *     no values are returned
     *     
     * Values can be obtained via IConnectDebug::Evaluate(). 
     *     
     * @param stackFrameLevel
     * @return
     */        

    public JVariable[] getLocalVariables(int stackFrameLevel) {
        
        IStackFrameInfo stackFrameinfo = getCStackFrame(stackFrameLevel); 
        IStackFrame cFrame = stackFrameinfo.StackFrames().at(stackFrameLevel);

        int partitionIdx = (int) cFrame.Partition();
        String partitionName = getPartitions()[partitionIdx].getName();
        
        IVectorVariables cVariables = cFrame.Variables();
        
        JVariable[] vars = cVariables2jVariables(cVariables, partitionName, partitionIdx);
        
        m_dataCtrl2.release(stackFrameinfo);
        return vars;
    }
    

    private IStackFrameInfo getCStackFrame(int stackFrameLevel) {
        
        boolean isActiveFrameOnly = false;
                
        if (stackFrameLevel == 0) {
            isActiveFrameOnly = true;
        }
        
        IStackFrameInfo stackFrameinfo = m_dataCtrl2.getStackFrameInfo(isActiveFrameOnly, 
                                                                            true);
        
        IVectorStackFrames cStackFrames = stackFrameinfo.StackFrames();
        
                                          
        int numStackFrames = (int)cStackFrames.size();

        if (stackFrameLevel >= numStackFrames) {
            throw new IllegalArgumentException("Stack frame index is out of range. " +
                    "(requested / available): " + stackFrameLevel + " / " + numStackFrames);
        }
        
        return stackFrameinfo;
    }

    
    /**
     * 
     * @param var if not null it contains info about expression. It is set
     *        as the first item in the resulting array. If null, it is ignored.
     * @param cVariables
     * @return
     */
    private JVariable[] cVariables2jVariables(IVectorVariables cVariables, String partitionName, int partitionIdx) {
        
        int numVariables = (int)cVariables.size();

        JVariable[] jVariables = new JVariable[numVariables];
        
        for (int idx = 0;idx < numVariables; idx++) {
            IVariable cVar = cVariables.at(idx);
            
            String qualifiedName = buildQualifiedName(cVar);
            
            jVariables[idx] = new JVariable(cVar.Name(), 
                                          qualifiedName,
                                          cVar.TypeName(),
                                          (int)cVar.NumBytes(),
                                          cVar.Type(),
                                          cVar.ArrayDimension(),
                                          cVar.Module(),
                                          partitionName,
                                          partitionIdx,
                                          cVar.Scope());
        }

        return jVariables;
    }
   
    
    /**
     * For top level vars qname may be empty, while for members of structs, for example,
     * it contains also struct name.
     */
    private String buildQualifiedName(IVariable cVar) {
        String qualifiedName = cVar.QualifiedName().trim();
        if (qualifiedName.length() == 0) {
            qualifiedName = cVar.Name();
        } 
        
        return qualifiedName;
    }
    
    
    public JRegisterDescriptor[] getRegisterDescriptors() {

        int numRegisters = (int)m_dataCtrl2.getTargetInfo().getM_dwNumRegisters();
        
        JRegisterDescriptor[] descriptors = new JRegisterDescriptor[numRegisters];
        for (int i = 0; i < numRegisters; i++) {
            CRegisterInfo registerInfo = m_dataCtrl2.getRegisterInfo(i);
            
            descriptors[i] = new JRegisterDescriptor(registerInfo.getRegisterName(),
                                                     new JSType(registerInfo.getType()));
        /* simulation:
         * SType type = new SType();
        type.setM_byBitSize((short)32);
        type.setM_byType((short)SType.EType.tUnsigned.swigValue()); 

        if (groupName.equals("General")) {
            return new JRegisterDescriptor[]{new JRegisterDescriptor("R0", type),
                                             new JRegisterDescriptor("R1", type)};
            
        } else if (groupName.equals("Bank0")) {
            return new JRegisterDescriptor[]{new JRegisterDescriptor("R2", type),
                                             new JRegisterDescriptor("R3", type), 
                                             new JRegisterDescriptor("R4", type)};
            
        } else if (groupName.equals("Bank1")) {
            return new JRegisterDescriptor[]{new JRegisterDescriptor("R5", type),
                                             new JRegisterDescriptor("R6", type), 
                                             new JRegisterDescriptor("R7", type)}; 
        } 

        return new JRegisterDescriptor[]{new JRegisterDescriptor("R14", type),
                                         new JRegisterDescriptor("R15", type)};
                                         */ 
        }
        
        return descriptors;
    }    
    
    
    public String[] getRegisterGroups() {
    
        /* iCon --> Java
         * vector<std::string> getRegisterGroups()  - returns ids of all register groups
         * vector<CRegister> getRegisterDescriptors(const std::string &groupId)
         * vector<longlong> getRegisterValues(const std::string &groupId) - returns values
         * of registers in the same order as descriptors were returned
         * 
         * class CRegisterDescriptor { -- see class JRegisterDescriptor for interface
         * private:
         *   string m_registerId;
         *   enum {STRING, UINT, ...} m_type;
         *   int bitSize;
         *   
         * public:
         *   std::string getRegisterId();
         *   getType();
         *   int getBitSize();
         * };
         */
        
        // currently no groups are available via iConnect.
        return new String[]{"General"};
    }
    
    
    public JSFR[] getSFRDescriptors() {
        ICPUSFR cpuSFRs = m_dataCtrl2.getCPUSFRs(IConnectEclipse.EGetCPUSFR.gcsSFRs.swigValue());
        
        // String cpuName = cpuSFRs.CPUName(); not needed
        IVectorSFRs sfrs = cpuSFRs.SFRs();

        JSFR[] jSFRs = getSFRs(sfrs);
        
        m_dataCtrl2.release(cpuSFRs);
        
        return jSFRs;
    }

    public JProperty getProperties(IConnectEclipse.EGetCPUSFR props) {
        ICPUSFR cpuSFRs = m_dataCtrl2.getCPUSFRs(props.swigValue());
        CPropertyWrapper property = new CPropertyWrapper(cpuSFRs.Property());
        JProperty prop = new JProperty(property);
        m_dataCtrl2.release(cpuSFRs);
        return prop;
    }

    public JProperty getSFRProperties() {
        return getProperties(IConnectEclipse.EGetCPUSFR.gcsSFRs);
    }

    public JProperty getGPRProperties() {
        return getProperties(IConnectEclipse.EGetCPUSFR.gcsGPRs);
    }

    public JProperty getAreaProperties() {
        return getProperties(IConnectEclipse.EGetCPUSFR.gcsAreas);
    }

    
    private JSFR[] getSFRs(IVectorSFRs sfrs) {
        
        int noOfSFRs = (int)sfrs.size();
        JSFR[] jSFRs = new JSFR[noOfSFRs];
        for (int i = 0; i < noOfSFRs; i++) {
            ISFR sfr = sfrs.at(i);
            
            Map<Long, String> valueMap = getValueMap(sfr);
            
            JSFR[] children = getSFRs(sfr.SFRs());
            
            // Address, bit offset and description were added in 9.12.167
            if (m_winIDEAVersion.compareTo(new JVersion(9, 12, 167)) >= 0) {
                jSFRs[i] = new JSFR(sfr.Name(),
                        sfr.Handle(),
                        sfr.Address(),
                        (int)sfr.BitSize(),
                        (int)sfr.BitOffset(),
                        sfr.HasValue() > 0,
                        sfr.Properties(),
                        valueMap,
                        sfr.Description(),
                        children);
            }
            else {
                jSFRs[i] = new JSFR(sfr.Name(),
                        sfr.Handle(),
                        (int)sfr.BitSize(),
                        sfr.HasValue() > 0,
                        sfr.Properties(),
                        valueMap,
                        children);
            }

        }
        
        return jSFRs;
    }

    
    public JCValueType readSFR(JSFR sfr) {
        SType sType = new SType();
        sType.setM_byBitSize((short) sfr.getBitSize());
        if (sfr.isFloat() ||  sfr.isDouble()) {
            sType.setM_byType((short) SType.EType.tFloat.swigValue());
        } else {
            sType.setM_byType((short) SType.EType.tUnsigned.swigValue());
        }
        
        CValueType retVal = m_dataCtrl2.readSFR(sfr.getHandle(), sType);
        /*
        JCValueType jRetVal = null;
        
        switch (sfrType.getTypeAsEnum()) {
        case tFloat:
            if (sfr.getBitSize() == 32) {
                jRetVal = new JCValueType(sfrType, retVal.getFloat());
            } else {
                jRetVal = new JCValueType(sfrType, retVal.getDouble());
            }
            break;
        case tUnsigned:
            if (sfr.getBitSize() <= 32) {
                jRetVal = new JCValueType(sfrType, retVal.getInt());
            } else {
                jRetVal = new JCValueType(sfrType, retVal.getLong());
            }
            break;
        default:
            throw new IllegalStateException("Invalid type or bitsize of SFR. Should be tFloat or tUnsigned. SFR: " + sfr);
        } */
        
        return new JCValueType(retVal); 
    }


    private Map<Long, String> getValueMap(ISFR sfr) {
        Map<Long, String> valueMap = new TreeMap<>();
        IVectorValueMap vectorValueMap = sfr.ValueMaps();
        int noOfValueMaps = (int)vectorValueMap.size();
        for (int j = 0; j < noOfValueMaps; j++) {
            valueMap.put(vectorValueMap.at(j).Value(), vectorValueMap.at(j).String());
        }
        return valueMap;
    }
    
    
    /**
     * Returns information provided by the following MI commands:
     * -stack-select-frame
     * -stack-list-arguments 0 
     *     no values are returned
     *
     * Values can be obtained via IConnectDebug::Evaluate(). 
     *     
     * @param stackFrameLevel
     * @return
     */
    public JVariable[] getArguments(int stackFrameLevel) {

        IStackFrameInfo stackFrameinfo = getCStackFrame(stackFrameLevel); 
        IStackFrame cFrame = stackFrameinfo.StackFrames().at(stackFrameLevel);
        
        IVectorVariables cVariables = cFrame.Arguments();
        
        int partitionIdx = (int) cFrame.Partition();
        String partitionName = getPartitions()[partitionIdx].getName();
        
        JVariable[] vars = cVariables2jVariables(cVariables, partitionName, partitionIdx);
        
        m_dataCtrl2.release(stackFrameinfo);
        return vars;
    }

    
    /**
     * Returns information provided by the following MI commands:
     * -symbol-list-variables -- [String name, SType type, size?]
     *      List all the global and static variable names.
     * 
     * Not strictly required - it would be possible to use GDB implementation, which relies 
     * on AST to provide list of globals.Not reliable, but simple to implement.
     * However, if IConTarget2 implements ICDITarget2 instead of ICDITarget,
     * this method is used.
     */
    public List<JVariable> getGlobalAndStaticVars() {
        
        IConfiguration configuration = m_dataCtrl2.getConfiguration(0);
        
        IVectorPartitions partitions = configuration.Partitions();
        int numPartitions = (int)partitions.size();
        List<JVariable> jVariables = new ArrayList<>();
        
        for (int partitionIdx = 0; partitionIdx < numPartitions; partitionIdx++) {
            IGlobals globals = 
                    m_dataCtrl2.getGlobals(
                            EGetGlobalsFlags.gvfVariables.swigValue() |
                            EGetGlobalsFlags.gvfSortName.swigValue() |
                            EGetGlobalsFlags.gvfModules.swigValue() | 
                            EGetGlobalsFlags.gvfPartitionSpecify.swigValue(),
                            partitionIdx);
        
            IVectorModules modules = globals.Modules();
                
            IVectorVariables cVariables = globals.Variables();

            int numVariables = (int)cVariables.size();

            for (int idx = 0;idx < numVariables; idx++) {
                IVariable cVar = cVariables.at(idx);
                String partitionName = partitions.at(partitionIdx).Name();
                int moduleIdx = cVar.Module();
                String moduleName = modules.at(moduleIdx).Name();
                String scope = cVar.Scope();
                
                String qualifiedName = JVariable.buildGlobalQualifiedName(moduleName, scope, cVar.Name(), partitionName);
                                
                jVariables.add(new JVariable(cVar.Name(), 
                                             qualifiedName,
                                             cVar.TypeName(),
                                             (int)cVar.NumBytes(),
                                             cVar.Type(),
                                             cVar.ArrayDimension(),
                                             moduleIdx,
                                             partitionName,
                                             partitionIdx,
                                             cVar.Scope()));
            }

            m_dataCtrl2.release(globals);
        }

        m_dataCtrl2.release(configuration);
        
        return jVariables;
    }
    
    
    /** 
     * Returns information provided by the following MI commands:
     * -var-list-children --> [name, type, size?]
     * 
     * Do we need stack level here?
     */
    public JVariable[] getVariableChildren(String expression) {

        // m_log.debug("getVariableChildren: expression = '%s'", expression);
        IExpressionType exprType = m_dataCtrl2.getExpressionType(0, expression);
        
        if (exprType == null) {
            return new JVariable[0];
        }
        
        IVectorVariables cvariables = exprType.Children();
        IVariable exprInfo = exprType.Expression();
        
        
        StringBuilder module = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder partition = new StringBuilder();
        JVariable.parseQualifiedName(expression, module, name, partition);
        JVariable vars[] = cVariables2jVariables(exprInfo, 
                                                 cvariables, 
                                                 module.toString(),
                                                 partition.toString());
        
        m_dataCtrl2.release(exprType);
        
        return vars;
    }
    
    public int getVariableChildrenCount(String expression) {

        // m_log.debug("getVariableChildren: expression = '%s'", expression);
        IExpressionType exprType = m_dataCtrl2.getExpressionType(0, expression);
        
        if (exprType == null) {
            return 0;
        }
        
        int rv = (int)exprType.Children().size();
        
        m_dataCtrl2.release(exprType);
        
        return rv;
    }
    
    /**
     * This method packs parent 'var' and children 'cVariables' into one array, 
     * where the first element represents parent and remaining contain children.
     * 
     * @param var parent var
     * @param cVariables children
     * @param partition transferred from parent, because there is a bug in iConnect,
     * so that ',,partition' is not returned. See JTrac ICONN-16.
     * @return 
     */
    private JVariable[] cVariables2jVariables(IVariable var, 
                                              IVectorVariables cVariables,
                                              String module,
                                              String partition) {
        int numVariables = (int)cVariables.size();
        
        JVariable[] jVariables = new JVariable[numVariables + 1];

        // the first item holds parent info
        StringBuilder tmp1 = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder tmp2 = new StringBuilder();
        String qualifiedName = var.QualifiedName();
        JVariable.parseQualifiedName(qualifiedName, tmp1, name, tmp2);
        if (!partition.isEmpty()) {
            qualifiedName += ",," + partition; // iConnect does not return partition, so add it here, see ICONN-16
        }
        
        int partitionIdx = getPartitionIndex(partition);
        int moduleIdx = getModuleIndex(partitionIdx, module);
        
        // System.out.println("23456789:" + qualifiedName + "/" + var.Name() + "/ name = " + name);
        jVariables[0] = new JVariable(name.toString(), /*var.Name() - returns '*"module"#varName' for pointers */
                                      name.toString(), 
                                      qualifiedName,
                                      var.TypeName(),
                                      (int)var.NumBytes(),
                                      var.Type(),
                                      var.ArrayDimension(),
                                      moduleIdx,
                                      partition,
                                      partitionIdx, 
                                      var.Scope());
        
        // remaining items contain children
        for (int idx = 0; idx < numVariables; idx++) {
            IVariable cVar = cVariables.at(idx);
            qualifiedName = cVar.QualifiedName();
            JVariable.parseQualifiedName(qualifiedName, tmp1, name, tmp2);
            if (!partition.isEmpty()) {
                qualifiedName += ",," + partition; // iConnect does not return partition, so add it here, see ICONN-16
            }
            // System.out.println("2345678:" + qualifiedName + "/" + cVar.Name() + "/ name = " + name);
            jVariables[idx + 1] = new JVariable(cVar.Name(),
                                                name.toString(),
                                                qualifiedName,
                                                cVar.TypeName(),
                                                (int)cVar.NumBytes(),
                                                cVar.Type(),
                                                cVar.ArrayDimension(),
                                                moduleIdx,
                                                partition,
                                                partitionIdx,
                                                cVar.Scope());
        }

        
        return jVariables;
    }

    /**
     * 
     * @param sfr
     * @param value value to be written to the register. Type info of this parameter 
     * must match SFR type. Use method #getSFRType() when setting this parameter.
     * @param isWriteThenRead
     * @param isNoIDERefresh do not refresh IDE after write. Set this flag to true,
     * if there are many successive writes is a short time interval. The last write 
     * should have this flag reset, so that IDE is refreshed.
     * 
     * @return Value and type of the SFR. Value is set only if parameter 'isWriteThenRead'
     * is set. Type is always set. 
     */
    public JCValueType writeSFR(JSFR sfr, 
                                JCValueType value, 
                                boolean isWriteThenRead, 
                                boolean isNoIDERefresh) {

        long flags = 0;
        if (isWriteThenRead) {
            flags |= IConnectEclipse.ESFRValue.svWriteThenRead.swigValue();
        }
        if (isNoIDERefresh) {
            flags |= IConnectEclipse.ESFRValue.svNoRefresh.swigValue();
        }
        
        CValueType retValue = m_dataCtrl2.writeSFR(flags, 
                                                   sfr.getHandle(), 
                                                   value.getCValueType());
        
        return new JCValueType(retValue);
    }
    
    // TODO write method, which will automatically retrieve registerInfo, and set
    // register value accordingly (all bits set to the given value, if value does not
    // fit, throw exception)
    public void writeRegister(EAccessFlags accessFlags,
                              String registerName,
                              JCValueType registerInfo) {

        /* SType type = new SType(); 
        type.setM_byBitSize(registerInfo.getType().getBitSize());
        type.setM_byType(registerInfo.getType().getType()); */
        
        m_dataCtrl2.writeRegister(accessFlags, 
                                  registerName, 
                                  registerInfo.getCValueType());
    }

    /**
     * 
     * @param accessFlags
     * @param memArea
     * @param aAddress
     * @param aNumMAUs
     * @param bytesPerMAU
     * @param data
     * @param accessInfo if not null, this array must be initialized to numMAU
     * bytes. On return it contains access info values. 
     * See ACCESS_OK and ACCESS_FAIL in globdefs.h for possible values.
     */
    public void writeMemory(EAccessFlags accessFlags,
                            short memArea,
                            long aAddress,
                            long aNumMAUs,
                            short bytesPerMAU,
                            short[] data,
                            short[] accessInfo) {
        
        VectorBYTE vectorData = new VectorBYTE(data.length, (short)0);
        for (int i = 0; i < data.length; i++) {
            vectorData.set(i, data[i]); 
        }
        
        VectorBYTE accInfo = m_dataCtrl2.writeMemory(accessFlags,
                                                             memArea,
                                                             aAddress,
                                                             aNumMAUs,
                                                             bytesPerMAU,
                                                             vectorData);

        if (accessInfo != null) {
            for (int i = 0; i < aNumMAUs; i++) {
                accessInfo[i] = accInfo.get(i);
            }
        }
    }
    
    /**
     * 
     * @param accessFlags
     * @param memArea
     * @param address
     * @param numMAUs
     * @param bytesPerMAU
     * @param accessInfo if not null, this array must be initialized to numMAU
     * bytes. On return it contains access info values. 
     * See ACCESS_OK and ACCESS_FAIL in globdefs.h for possible values.
     * @return
     */
    public short[] readMemory(EAccessFlags accessFlags,
                              short memArea,
                              long address,
                              long numMAUs,
                              short bytesPerMAU,
                              short [] accessInfo) {

        VectorBYTE data = m_dataCtrl2.readMemory(accessFlags,
                                                      memArea,
                                                      address,
                                                      numMAUs,
                                                      bytesPerMAU);

        int memBlockSize = (int)numMAUs * bytesPerMAU;
        
        short[] result = new short[memBlockSize];

        for (int i = 0; i < memBlockSize; i++) {
            result[i] = data.get(i);
        }

        if (accessInfo != null) {
            for (int i = 0; i < numMAUs; i++) {
                accessInfo[i] = data.get(memBlockSize + i);
            }
        }
        
        return result;
    }
    
    
    public JCValueType readRegister(EAccessFlags accessFlags, String registerName) {
        return new JCValueType(m_dataCtrl2.readRegister(accessFlags, registerName));
    }
    
    public String modify(EAccessFlags accessFlags,
                         String pszExpression,
                         JCValueType value) {
        
        return modify(accessFlags,
                EEvaluate.efNoMemAreaDisplay,
                pszExpression, 
                value);
    }
    
    public String modify(EAccessFlags accessFlags, 
                         EEvaluate evaluateFlags, 
                         String pszExpression, 
                         JCValueType value) {

        return m_dataCtrl2.modify(accessFlags, 
                                  evaluateFlags, 
                                  pszExpression, 
                                  value.getCValueType());
    }
    
    public String modify(EAccessFlags accessFlags,
                         String expression, 
                         String value) {
        return modify(accessFlags, 
                      EEvaluate.efNoMemAreaDisplay, 
                      expression, 
                      value);
    }

    public String modify(EAccessFlags accessFlags, 
                         EEvaluate evaluateFlags, 
                         String expression, 
                         String value) {
        return m_dataCtrl2.modify(accessFlags, 
                                  evaluateFlags, 
                                  expression, 
                                  value);
    }

    public JVarAttribute getVariableAttributes() {
        return new JVarAttribute(true);
    }
    
    public String getWorkspaceFileName()
    {
        checkIdeCtrl();
        String wsFileName = m_ideCtrl.getPath(EPathType.WORKSPACE_FILE_NAME);
        return (wsFileName == null ? "" : wsFileName.trim());
    }

    
    public JCValueType readValue(EAccessFlags accessFlags,
                                short memArea,
                                long aAddress,
                                JSType type) {
        SType cType = new SType(); 
        cType.setM_byBitSize(type.getBitSize());
        cType.setM_byType(type.getType());
        
        return new JCValueType(m_dataCtrl2.readValue(accessFlags, 
                                                          memArea, 
                                                          aAddress, 
                                                          cType));

    }

    public void writeValue(EAccessFlags accessFlags,
                           short memArea,
                           long aAddress,
                           JCValueType value) {
        m_dataCtrl2.writeValue(accessFlags, 
                                    memArea, 
                                    aAddress, 
                                    value.getCValueType());
    }

    /**
     * This returns the required partition flags if partition index is < 0.
     * This simulates behavior of the getFunctions, getVariables which have the
     * flag manipulation built in.
     * @param partitionIndex if PARTTITION_DEFAULT, gvfPartitionDefault is returned, 
     *                       if PARTTITION_CURRENT, gvfPartitionCurrent is returned, 
     *                       gvfPartitionSpecify is returned for all other values. 
     * @return
     */
    private EGetGlobalsFlags getPartitionFlag(int partitionIndex) {
        EGetGlobalsFlags partitionFlag;
        if (partitionIndex == -1) {
            partitionFlag = EGetGlobalsFlags.gvfPartitionDefault;
        } else if (partitionIndex == -2) {
            partitionFlag = EGetGlobalsFlags.gvfPartitionCurrent;
        } else {
            partitionFlag = EGetGlobalsFlags.gvfPartitionSpecify;
        }
        return partitionFlag;
    }


    public EEndian getDefaultEndian() {
        return m_dataCtrl2.getDefaultEndian();
    }

    public CSystemMemoryAreas getSystemMemoryAreas() {
        return m_dataCtrl2.getSystemMemoryAreas();
    }


    public void setStackFrameContext(int currentStackLevel) {
        m_dataCtrl2.setStackFrameContext(0, currentStackLevel);
    }


    public int getMemoryAreaBytesPerMAU(int memArea) {
        return m_dataCtrl2.getMemoryAreaBytesPerMAU(memArea);
    }


    public boolean catUseRealTimeReadAccess() {
        return m_dataCtrl2.canAccessMemory().canReadInRealtime();
    }
    
    
    /** Returns file name with path, for example: '../../bin/sample.elf' */ 
    public String getDefaultDownloadFilePath() {
        checkIdeCtrl();
        return m_ideCtrl.getDefaultDownloadFile();
    }
    
    
    /** Returns file name without path, for example: 'sample.elf' */ 
    public String getDefaultDownloadFileName() {
        String filePath = getDefaultDownloadFilePath();
        Path path = Paths.get(filePath);
        
        return path.getFileName().toString();
    }
    
    
//    public StrStrMap getFunctionsToModulesMap() {
//        StrStrMap functionsToModulesMap = new StrStrMap();
//        m_dataCtrl2.getFunctionToModulesMap(functionsToModulesMap);
//        return functionsToModulesMap;
//    }
//    
//    
//    public StrVector getFunctionsForModule(int partitionIdx, int moduleIdx) {
//        StrVector functions = new StrVector();
//        m_dataCtrl2.getFunctionsForModule(partitionIdx, moduleIdx, functions);
//        return functions;
//    }
}
