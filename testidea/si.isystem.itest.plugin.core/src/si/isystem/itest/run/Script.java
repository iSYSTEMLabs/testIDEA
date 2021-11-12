package si.isystem.itest.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.python.InStreamListener;
import si.isystem.python.Python;
import si.isystem.python.ScriptResult;

public class Script {
    
    private static final String ISYS_PREFIX = "_isys_";
    private static final String TEST_EXTENSION_OBJ = ISYS_PREFIX + "testExtension";
    public static final String TEST_CASE_HANDLE_VAR = ISYS_PREFIX + "testCaseHandle";
    public static final String RESERVED_TEST_SPEC_PARAM = ISYS_PREFIX + "testSpec";
    public static final String RESERVED_TEST_GROUP_PARAM = ISYS_PREFIX + "testGroup";

    private Python m_interpreter; // never access directly - always call getInterpreter() 
    private int m_scriptTimeout;

    private JConnection m_jCon;
    private CScriptConfig m_scriptConfig;

    private long m_testCaseHandle = -1;
    private boolean m_isISystemConnectImported = false;
    private String m_modelFileName;
    private InStreamListener m_inStreamListener;
    
    
    public Script(JConnection jCon, 
                  CScriptConfig scriptConfig, 
                  String modelFileName) {
        
        m_jCon = jCon;
        m_scriptConfig = scriptConfig;
        m_modelFileName = modelFileName;
    }

    
    public String getExtensionClass() {
        return m_scriptConfig.getExtensionClass();
    }
    
    
    /**
     * Returns info about implemented reserved methods in extension script.
     */
    public ExtensionScriptInfo getExtScriptInfo() {
        
        String extensionClass = m_scriptConfig.getExtensionClass();
        Python python = getInterpreter();
        List<ScriptResult> results = python.execStatements(m_scriptTimeout, true, 
                                       "allMembers = dir(" + extensionClass +")\n"
                                       + "public = [x for x in allMembers if not x.startswith('_')]\n"
                                       + "for s in public: print(s)\n");
        // check for error
        for (ScriptResult result : results) {
            if (result.isError()) {
                throw new SIllegalStateException("Error when obtaining extension script info!").
                    add("stderr", StringUtils.join(result.getStderr(), '\n')); 
            }
        }
        
        ScriptResult listOfMembers = results.get(results.size() - 1);
        List<String> members = listOfMembers.getStdout();
        boolean isBeforeTestMethod = false;
        boolean isGetTestReportCustomDataMethod = false;
        boolean isAfterReportSaveMethod = false;
        List<String> customMethods = new ArrayList<>();
        List<String> rangeMethods = new ArrayList<>();
        
        for (String member : members) {
            if (member.equals(CScriptConfig.getEXT_METHOD_BEFORE_TESTS())) {
                isBeforeTestMethod = true;
            } else if (member.equals(CScriptConfig.getEXT_METHOD_GET_TEST_REPORT_CUSTOM_DATA())) {
                isGetTestReportCustomDataMethod = true;
            } else if (member.equals(CScriptConfig.getEXT_METHOD_AFTER_REPORT_SAVE())) {
                isAfterReportSaveMethod = true;
            } else if (member.startsWith(CScriptConfig.getEXT_METHOD_CUSTOM_PREFIX())) {
                customMethods.add(member);
            } else if (member.startsWith(CScriptConfig.getEXT_METHOD_TABLE_PREFIX())) {
                rangeMethods.add(member);
            } // else ignore other methods
        }
        
        return new ExtensionScriptInfo(isBeforeTestMethod, 
                                       isGetTestReportCustomDataMethod, 
                                       isAfterReportSaveMethod, 
                                       customMethods,
                                       rangeMethods);
    }
    
    
    /**
     * Calls python method specified in instance of CTestFunction.  
     * 
     * @param testNode
     * @param hostVars
     * @param cTestFunction
     * @return script result or null if cTestFunction has empty name
     */
    TestScriptResult callCTestFunction(CTestTreeNode testNode, 
                                       CTestHostVars hostVars, 
                                       String funcType, 
                                       CTestFunction cTestFunction) {
        
        if (cTestFunction.getName().isEmpty()) {
            return null;
        }
        
        String funcName = cTestFunction.getName();
        
        StrVector scriptParams = new StrVector();
        cTestFunction.getPositionParams(scriptParams);
        
        String[] paramsStrArray = new String[(int)scriptParams.size()];
        for (int idx = 0; idx < paramsStrArray.length; idx++) {
            paramsStrArray[idx] = hostVars.replaceHostVars(scriptParams.get(idx));
        }

        return callFunction(testNode, funcType, funcName, paramsStrArray);
    }
    

    /**
     * 
     * @param testNode
     * @param funcType
     * @param functionName
     * @param params must have host variables in params already replaced
     * @return
     */
    public TestScriptResult callFunction(CTestTreeNode testNode, 
                                         String funcType, 
                                         String functionName, 
                                         String[] params) {
        
        if (functionName.isEmpty()) {
            return null;
        }
        
        // set test specification variable if the user specified the first parameter as
        // RESERVED_TEST_SPEC_PARAM
        if (testNode != null  &&  params.length > 0) {
            if (testNode.isGroup()) {
                setReservedParam(testNode, params, RESERVED_TEST_GROUP_PARAM);
            } else {
                setReservedParam(testNode, params, RESERVED_TEST_SPEC_PARAM);
            }
        }
        
        String qualifiedFunctionName = TEST_EXTENSION_OBJ + "." + functionName;
        
        try {
            ScriptResult result = getInterpreter().callFunction(qualifiedFunctionName, 
                                                                params, 
                                                                m_scriptTimeout,
                                                                true);
            
            List<String> scriptInfo = getScriptInfo(funcType);
            
            return new TestScriptResult(funcType, result, scriptInfo);
            
        } catch (Exception ex) {
            ex.printStackTrace(); // print stack trace here, because it is not 
            // given in test result (it is of no use to the user there) 
            throw new SIllegalStateException("Error when calling script function!", ex)
                                             .add("scriptFunction", functionName);
        }
    }


    protected void setReservedParam(CTestTreeNode testNode,
                                    String[] params,
                                    String reservedParamName) {
        
        if (params[0].equals(reservedParamName)) {

            String testNodeStr = testNode.toString();
            if (!m_isISystemConnectImported) {
                // lazy import
                getInterpreter().execStatements(m_scriptTimeout,
                                                true,
                        "import isystem.connect");
                m_isISystemConnectImported = true;
            }

            getInterpreter().execStatements(m_scriptTimeout,
                                            true,
                                            reservedParamName + " = " 
                                            + "isystem.connect.CTestSpecification.parseTestSpec(\n"
                                            // make the string 'raw', since there may be '\' in paths to 
                                            // analyzer export and other files. See B018744. 
                                            // \\ is OK in path.
                                            + "r\"\"\"\n" + testNodeStr + "\n\"\"\")");
        }
    }


    /** 
     * This method takes care about lazy initialization of script interpreter.
     * It is initialized only when needed.
     * 
     * @return
     */
    private Python getInterpreter() {
        if (m_interpreter == null) {

            String extensionClass = m_scriptConfig.getExtensionClass();
            if (extensionClass.trim().isEmpty()) {
                throw new SIllegalArgumentException("Scripting is not properly configured - at least extension class name "
                        + "should be specified!\nSee 'File | Properties | Scripts'.");
            }

            StrVector modules = new StrVector();
            m_scriptConfig.getModules(modules);
            int numModules = (int)modules.size();
            if (numModules == 0) {
                throw new SIllegalArgumentException("Scripting is not properly configured - at least module with extension class "
                        + "should be specified!\nSee 'File | Properties | Scripts'.");
            }
            
            
            String workingDir = m_scriptConfig.getWorkingDir();
            if (workingDir.trim().isEmpty()) {
                // if workingDir is empty, it means 
                // user wants to start python in the dir of test spec file
                workingDir = new File(m_modelFileName).getParent();
                if (workingDir.trim().isEmpty()) {
                    // if workingDir is empty, process start fails!
                    workingDir = ".";
                }
            }
            
            m_interpreter = Python.createInteractiveInstance(m_jCon,
                                                             workingDir);
            
            m_scriptTimeout = m_scriptConfig.getTimeout() * 1000; // convert to milliseconds

            // set sys.path
            StrVector sysPaths = new StrVector();
            m_scriptConfig.getSysPaths(sysPaths);
            int numPaths = (int)sysPaths.size();
            if (numPaths > 0) {
                m_interpreter.execStatements(m_scriptTimeout, true, "import sys");
            }
            for (int i = 0; i < numPaths; i++) {
                String sysPath = sysPaths.get(i);
                m_interpreter.callFunction("sys.path.append", 
                                           new String[]{"r'" + sysPath + "'"}, 
                                           m_scriptTimeout, true);
            }
                
            // import modules
            // m_interpreter.execStatement("import os; os.getcwd()", m_scriptTimeout, true);
            for (int i = 0; i < numModules; i++) {
                String module = modules.get(i);
                m_interpreter.execStatements(m_scriptTimeout, true, "import " + module);
            }
            
            // instantiate callback class
            m_interpreter.execStatements(m_scriptTimeout, true,
                                         TEST_EXTENSION_OBJ + " = " + extensionClass + "()");
        }

        if (m_testCaseHandle != -1) {
            String mangledPrivateVarName = TEST_EXTENSION_OBJ + "." +  
                                           TEST_CASE_HANDLE_VAR;
            m_interpreter.execStatements(m_scriptTimeout, true,
                                         mangledPrivateVarName + " = " + m_testCaseHandle);
            m_testCaseHandle = -1; // set the handle only once
        }
        
        m_interpreter.setInStreamListener(m_inStreamListener);
        
        return m_interpreter;
    }


    public void setInStreamListener(InStreamListener listener) {
        m_inStreamListener = listener;
    }
    
    
    /** 
     * Executes the given script statement.
     * 
     * @param statement script statement to execute
     * @return execution result
     */
    public ScriptResult execStatement(String statement) {
        return getInterpreter().execStatements(m_scriptTimeout, true, statement).get(0);
    }

    
    /**
     * This method returns contents of reserved variable, which contains 
     * data produced by script extension method.
     *   
     * @param funcType
     * @return
     */
    private List<String> getScriptInfo(String funcType) {
        
        String varName = CTestResult.funcType2PyVarName(funcType);
        
        // the trailing '\n' ends interactive code block (if statement)
        String qVarName = TEST_EXTENSION_OBJ + "." + varName;
        ScriptResult result = execStatement("if hasattr(" + TEST_EXTENSION_OBJ + ", '" + 
                                            varName + "')  and (" + qVarName + " != None): print(" + 
                                            qVarName + ")\n");
        execStatement(TEST_EXTENSION_OBJ + "." + varName + " = None");  // will clear the var,
        // because the next time some other function will get called as for 
        // example initTarget() script, ant the user will easily forget to reset 
        // the var set in some function called before. 

        return result.getStdout();
    }

    
    public void close() {
        if (m_interpreter != null) {
            m_interpreter.close();
        }
    }


    public void setTestHandle(long testCaseHandle) {
        m_testCaseHandle = testCaseHandle;
    }
}
