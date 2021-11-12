package si.isystem.itest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.VariablesGlobalsProvider;
import si.isystem.connect.CLineDescription.EMatchingType;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CStackUsageConfig;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerExportFormat;
import si.isystem.connect.CTestAnalyzerTrace;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCaseTargetInitConfig;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EDiagType;
import si.isystem.connect.CTestDiagrams;
import si.isystem.connect.CTestDryRun;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStackUsage;
import si.isystem.connect.CTestStopCondition;
import si.isystem.connect.CTestStopCondition.EStopType;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrStrMapIterator;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.itest.common.CoreIdUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.handlers.ToggleAnalyzerOnOffHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.ui.comp.TBControl;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.spec.ProfilerDataAreaEditor;
import si.isystem.itest.ui.spec.StatusView;

public class ModelVerifier {

    public static final ModelVerifier INSTANCE = new ModelVerifier();

    
    public class SectionStatus {
        int m_severity;  // should be one of constants defined in IStatus
        String m_description;

        public SectionStatus(int severity, String description) {
            super();
            m_severity = severity;
            m_description = description;
        }

        public int getSeverity() {
            return m_severity;
        }

        public String getDescription() {
            return m_description;
        }
        
    }
    
    Map<TBControl.EHControlId, SectionStatus> m_sectionStatusMap = 
                    new TreeMap<TBControl.EHControlId, SectionStatus>();
    private boolean m_isNotAllAnalyzerFilesSaved;
    
    
    
    private ModelVerifier() {
    }
    
    
    public static ModelVerifier instance() {
        return INSTANCE;
    }
    
    
    public SectionStatus getSectionStatus(TBControl.EHControlId testSpecSection) {
        return m_sectionStatusMap.get(testSpecSection);
    }
    

    /**
     * IMPORTANT: This functionality does not work for test cases, which have
     * analyzer section merged, and parent is not member of any group (is not executable, 
     * for example). 
     * 
     * @param model
     * @param parent
     * @return true, if model was modified
     */
    public boolean askForAutoSetSaveAnalyzerFile(TestSpecificationModel model, CTestGroup parent) {
        
        if (m_isNotAllAnalyzerFilesSaved) {
            boolean isApply =
            MessageDialog.openQuestion(Activator.getShell(), "Save analyzer files", 
                                       "To get coverage results on group, all analyzer files of member test cases must be saved. "
                                       + "See section 'Analyzer', setting 'Save after test'.\n\n"
                                       + "Click 'Yes', to automatically apply this setting to all test cases with active analyzer and members of group.");
            
            if (isApply) {
                model.setAnalyzerSaveAfterTest(parent);
            }
            
            return isApply;
        }
        
        return false;
    }
    
    public List<TestSpecStatus> verifyTestTreeNodeAndChildren(CTestTreeNode testTreeNode, 
                                                              int derivedLevel) {
        
        List<TestSpecStatus> errorsList = new ArrayList<TestSpecStatus>();
        TreeMap<String, CTestTreeNode> idsMap = new TreeMap<>();
        Stack<Boolean> analyzerActivatedStack = new Stack<>();
        m_isNotAllAnalyzerFilesSaved = false;

        verifyRecursively(testTreeNode, derivedLevel, idsMap, errorsList, analyzerActivatedStack);
        
        return errorsList;
    }
    
    
    public List<TestSpecStatus> verifyAll() {

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        List<TestSpecStatus> errorsList = new ArrayList<TestSpecStatus>();
        m_isNotAllAnalyzerFilesSaved = false;

        verifyTestTreeNodes(model.getRootTestGroup(), errorsList);
        
        verifyTestTreeNodes(model.getRootTestSpecification(), errorsList);
        
        StatusModel.instance().clear();
        
        return errorsList;
    }
    
    
    private void verifyTestTreeNodes(CTestTreeNode rootNode,
                                     List<TestSpecStatus> errorsList) {
        
        TreeMap<String, CTestTreeNode> idsMap = new TreeMap<>();
        Stack<Boolean> analyzerActivatedStack = new Stack<>();

        // Do not verify root test spec., but all its derived specs.
        CTestBaseList children = rootNode.getChildren(true);
        int numDerived = (int) children.size();
                
        for (int i = 0; i < numDerived; i++) {
            CTestTreeNode derived = CTestTreeNode.cast(children.get(i));
            verifyRecursively(derived, Integer.MAX_VALUE, idsMap, errorsList, analyzerActivatedStack);
        }
    }

    
    private void verifyRecursively(CTestTreeNode testTreeNode, 
                                   int derivedLevel,
                                   TreeMap<String, CTestTreeNode> idsMap, 
                                   List<TestSpecStatus> errorsList, 
                                   Stack<Boolean> analyzerActivatedStack) {

        if (testTreeNode.isGroup()) {
            verifyTestGroup(CTestGroup.cast(testTreeNode), idsMap, errorsList, analyzerActivatedStack);
        } else {
            verifyTestSpec(CTestSpecification.cast(testTreeNode), idsMap, errorsList);
        }

        if (derivedLevel > 0) {
            CTestBaseList children = testTreeNode.getChildren(true);
            int numDerived = (int) children.size();
            for (int i = 0; i < numDerived; i++) {
                CTestTreeNode derived = CTestTreeNode.cast(children.get(i));
                verifyRecursively(derived, --derivedLevel, idsMap, errorsList, analyzerActivatedStack);
            }
        }
        
        if (testTreeNode.isGroup()) {
            analyzerActivatedStack.pop();
        }
    }
    

//    private void verifyTestSpecs(CTestSpecification testSpec, 
//                                 List<TestSpecStatus> errorsList)
//    {
//        
//        TreeMap<String, CTestSpecification> idsMap = new TreeMap<String, CTestSpecification>();
//
//        // Do not verify root test spec., but all its derived specs.
//        int numDerived = testSpec.getNoOfDerivedSpecs();
//        for (int i = 0; i < numDerived; i++) {
//            CTestSpecification derivedTS = testSpec.getDerivedTestSpec(i);
//            verifyTestSpecsRecursively(derivedTS, Integer.MAX_VALUE, idsMap, errorsList);
//        }
//    }

    
//    private void verifyTestSpecsRecursively(CTestSpecification testSpec, 
//                                 int derivedLevel,
//                                 TreeMap<String, CTestSpecification> idsMap, 
//                                 List<TestSpecStatus> errorsList) {
//
//        verifyTestSpec(testSpec, idsMap, errorsList);
//        
//        if (derivedLevel > 0) {
//            int numDerived = testSpec.getNoOfDerivedSpecs();
//            for (int i = 0; i < numDerived; i++) {
//                CTestSpecification derivedTS = testSpec.getDerivedTestSpec(i);
//                verifyTestSpecsRecursively(derivedTS, --derivedLevel, idsMap, errorsList);
//            }
//        }
//    }

    private void verifyTestGroup(CTestGroup testGroup, 
                                 TreeMap<String, CTestTreeNode> idsMap, 
                                 List<TestSpecStatus> errorsList, 
                                 Stack<Boolean> analyzerActivatedStack) {

        verifyDuplicateId(testGroup, idsMap, errorsList);
        
        verifyGroupCoverage(testGroup, errorsList, analyzerActivatedStack);
    }
    
    
    private void verifyGroupCoverage(CTestGroup testGroup,
                                     List<TestSpecStatus> errorsList, 
                                     Stack<Boolean> analyzerActivatedStack) {
        
        if (testGroup.getCoverageExportConfig(true).isActive() == ETristate.E_TRUE) {
            String analyzerFile = testGroup.getMergedAnalyzerFileName();
            if (analyzerFile.isEmpty()) {
                storeStatus("Coverage in group is active, but analyzer file name is not set!", 
                            "Please define analyzer file name.", 
                            testGroup, 
                            null, 
                            errorsList);
            }
            
            analyzerActivatedStack.push(Boolean.TRUE);
        } else {
            // if one of parent groups has coverage active, all member tests must save file
            if (analyzerActivatedStack.isEmpty()) {
                analyzerActivatedStack.push(Boolean.FALSE);
            } else {
                analyzerActivatedStack.push(analyzerActivatedStack.peek());
            }
        }
        
        if (analyzerActivatedStack.peek().booleanValue()) {
            checkSaveAfterTestInChildren(testGroup, errorsList);
        }
    }
    

    private void checkSaveAfterTestInChildren(CTestGroup testGroup,
                                              List<TestSpecStatus> errorsList) {
        
        int numTsGroups = (int) testGroup.getTestOwnerGroupsSize();
        
        for (int idx = 0; idx < numTsGroups; idx++) {
            
            CTestGroup tsGroup = testGroup.getTestOwnerGroup(idx);
            CTestSpecification testSpec = tsGroup.getOwnedTestSpec();
            
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            
            if (analyzer.getRunMode() == ERunMode.M_START  &&
                    analyzer.isSaveAfterTest() != ETristate.E_TRUE) {
                
                storeStatus("Coverage in group is active, but analyzer file in member test case is not saved!", 
                            "Set option 'Save after test' in section 'Analyzer' to 'Yes'.", 
                            testSpec, 
                            null, 
                            errorsList);
                m_isNotAllAnalyzerFilesSaved = true;
            }
            
            checkSaveAfterTestInChildren(tsGroup, errorsList);
        }
    }


    private void verifyTestSpec(CTestSpecification testSpec, 
                                 TreeMap<String, CTestTreeNode> idsMap, 
                                 List<TestSpecStatus> errorsList) {

        m_sectionStatusMap.clear();
        String coreId = testSpec.getCoreId();
        FunctionGlobalsProvider funcProvider = GlobalsConfiguration.instance().
                            getGlobalContainer().getFuncGlobalsProvider(coreId);
        VariablesGlobalsProvider varsProvider = GlobalsConfiguration.instance().
                            getGlobalContainer().getVarsGlobalsProvider(coreId);
        
        verifyFunctionsUnderTest(testSpec, errorsList, funcProvider);
        
        verifyMetaSection(testSpec, errorsList);
        
        verifyVariablesSection(testSpec, errorsList);

        verifyPersistVars(testSpec, errorsList);
        
        verifyHiddenGlobals(testSpec, errorsList, varsProvider);

        verifyStackUsageSection(testSpec, errorsList);
        
        verifyExpectedSection(testSpec, errorsList);
        
        verifyFunctionsInStubs(testSpec, errorsList, funcProvider);

        verifyFunctionsInUserStubs(testSpec, errorsList, funcProvider);
        
        compareStubsAndUserStubs(testSpec, errorsList);
        
        verifyFunctionsInCoverage(testSpec, errorsList, funcProvider);

        verifyFunctionsInProfiler(testSpec, errorsList, funcProvider);

        verifyUndefinedVariables(testSpec, errorsList);
        
        verifyDuplicateId(testSpec, idsMap, errorsList);
        
        verifyVariablesInProfiler(testSpec, errorsList, varsProvider);
        
        verifyAnalyzerSection(testSpec, errorsList);
        
        verifyCoverageSection(testSpec, errorsList);
        
        verifyProfilerSection(testSpec, errorsList);

        verifyTraceSection(testSpec);

        verifyStubsSection(testSpec, errorsList);
        
        verifyTestPointsSection(testSpec, errorsList);
        
        verifyScriptsSection(testSpec, errorsList);
        
        verifySystemTests(testSpec, errorsList);
        
        verifyDryRunSection(testSpec, errorsList);
        
        verifyDiagrams(testSpec, errorsList);
    }


    private void verifyPersistVars(CTestSpecification testSpec,
                                   List<TestSpecStatus> errorsList) {
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        
        CTestCaseTargetInitConfig targetInitCfg = 
                model.getCEnvironmentConfiguration().getTestCaseTargetInitConfig(true);
        
        CTestPersistentVars persisVars = testSpec.getPersistentVars(true);
        
        if ((targetInitCfg.isDownloadOnTCInit()  ||
                targetInitCfg.isResetOnTCInit()  ||
                targetInitCfg.isRunOnTCInit())  &&  !persisVars.isEmpty()) {
            storeStatus("It is not possible to use persistent variables and initialize target before each test case!",
                        "See section 'Persistent variables' and 'File | Properties | Target Initialization Before Each Test Case'!", 
                        testSpec, null, 
                        errorsList);
        }
        
    }


    // this method is called interactively during editing - set status view only
    public void verifyTestTreeNodeAndSetStatus(CTestTreeNode testNode) {
        
        if (testNode == null) {
            return;
        }

        List<TestSpecStatus> errorsList = new ArrayList<TestSpecStatus>();
        
        if (testNode.isGroup()) {
            Stack<Boolean> analyzerActivatedStack = new Stack<>();
            CTestGroup testGroup = CTestGroup.cast(testNode);
            verifyTestGroup(testGroup, null, errorsList, analyzerActivatedStack);
        } else {
            CTestSpecification testSpec = CTestSpecification.cast(testNode);
            verifyTestSpec(testSpec, null, errorsList);
        }

        StringBuilder sb = new StringBuilder();

        int severity = statusList2String(errorsList, sb, -1);
        StatusView statusView = StatusView.getView();

        if (statusView != null) { 
            switch (severity) {
            case IStatus.ERROR:
                statusView.setDetailPaneText(StatusType.ERROR, sb.toString());
                break;
            case IStatus.WARNING:
                statusView.setDetailPaneText(StatusType.WARNING, sb.toString());
                break;
            default: // info status
            statusView.setDetailPaneText(StatusType.INFO, sb.toString());
            }
        }
    }
    
    
    private void verifyMetaSection(CTestSpecification testSpec,
                                   List<TestSpecStatus> errorsList) {

        // test id
        String msg = verifyRestrictedTextScalar(testSpec.getTestId());

        storeStatus("Test ID is invalid!", msg, testSpec, EHControlId.ETestId, 
                    errorsList);


        // tags
        StrVector tags = new StrVector();
        testSpec.getTags(tags);
        int numTags = (int)tags.size();

        for (int i = 0; i < numTags; i++) {
            msg = verifyRestrictedTextScalar(tags.get(i));
            storeStatus("Tags are invalid!", msg, testSpec, EHControlId.ETags, errorsList);
        }
    }

    
    private void verifyFunctionsUnderTest(CTestSpecification testSpec,
                                         List<TestSpecStatus> errorsList,
                                         FunctionGlobalsProvider funcProvider) {
        
        if (funcProvider.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }

        CTestSpecification mergedTestSpec = testSpec.merge();

        CTestFunction testFunc = mergedTestSpec.getFunctionUnderTest(true);
        
        String funcName = testFunc.getName();
        
        if (funcName.isEmpty()) {  // do not write warnings for not yet entered names 
            return;
        }
        
        try {
            JFunction jFunc = funcProvider.getCachedFunction(funcName);

            if (jFunc != null) {
                // do not verify parameters on non-executable classes, as they may be 
                // overridden in derived classes.
                if (testSpec.getRunFlag() != ETristate.E_FALSE) {
                    int numParams = jFunc.getParameters().length;

                    StrVector positionParams = new StrVector();
                    mergedTestSpec.getPositionParams(positionParams);

                    if (numParams != positionParams.size()  &&  !jFunc.isVariadicParams()) {
                        errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                          testSpec, 
                                                          "The numbers of function parameters in test specification and executable do not match!\n" +
                                                                  "No of params in test spec:  " + positionParams.size() + "\n" +
                                                                  "No of params in debug info: " + numParams));
                    }
                    
                    StrStrMap testLocalVars = new StrStrMap();
                    mergedTestSpec.getLocalVariables(testLocalVars);
                    // list of function local vars also contains function parameters
                    verifyShadowedTestLocalVars(testLocalVars, jFunc.getLocalVars(), 
                                                testSpec, errorsList);
                }
            } else {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Function '" + funcName + "' not found in the download file."));
            }
        } catch (SException ex) {
            errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                              testSpec, 
                                              "Section 'Function': " + SEFormatter.getInfo(ex)));
        }
        
        // ret val name
        String msg = verifyAlphanumericScalar(testFunc.getRetValueName());
 
        storeStatus("The name of return value is invalid!", msg,
                    testSpec, EHControlId.EFuncUnderTestRetValName,
                    errorsList);
    }
    

    private void verifyShadowedTestLocalVars(StrStrMap testLocalVars, 
                                             JVariable[] funcVars, 
                                             CTestSpecification testSpec, 
                                             List<TestSpecStatus> errorsList) {
        
        for (JVariable funcVar : funcVars) {
            if (testLocalVars.containsKey(funcVar.getName())) {
                errorsList.add(new TestSpecStatus(IStatus.WARNING,
                                                  testSpec,
                                                  "Test local variable '" + funcVar.getName() 
                                                  + "' is hidden by function parameter or local variable. "
                                                  + "If you want the test local variable to be accessible by testIDEA "
                                                  + "during test point or stub execution or debugging, rename it."));
            }
        }
    }


    private void verifyVariablesSection(CTestSpecification testSpec,
                                        List<TestSpecStatus> errorsList) {
        StrStrMap initMap = new StrStrMap();
        testSpec.getInitMap(initMap);
        
        StrStrMap localVars = new StrStrMap();
        testSpec.getLocalVariables(localVars);
        
        verifyNamesOfDeclaredVars(testSpec, errorsList);
        
        StrVector keys = new StrVector();
        testSpec.getInitKeys(keys);
        
        Set<String> alreadyInitializedVars = new TreeSet<>();
        
        Pattern pattern = Pattern.compile("\\w+");
        
        int numVars = (int) keys.size();
        for (int idx = 0; idx < numVars; idx++) {
            String key = keys.get(idx);
            String value = initMap.get(key);
            
            // Check if all global vars (those not declared in this section) have
            // value assigned.
            // Local variables may not be initialized, if they are used as out parameter.
            if (value.isEmpty()  &&  !localVars.containsKey(key)) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Variable '" + key + "' in section 'Variables' has no type or value defined.\n"
                                                  + "It will cause runtime error."));
            }
            
            // Check if all local vars are initialized before they are used as RValues.
            if (UiUtils.isVarName(value)) {
                if (!alreadyInitializedVars.contains(value)  &&  localVars.containsKey(value)) {
                    errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                      testSpec, 
                                                      "Variable '" + value + "' in section 'Variables' is used as r-value before it was initialized.\n"
                                                      + "This may cause unpredictable test outcome."));
                }
            }

            alreadyInitializedVars.add(key);
            
            // strip all symbols, like (), or (*varName).x from var name, since
            // structures may be initialized via members, not direct assignment.
            // Warning should not be issued in such cases.
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                alreadyInitializedVars.add(matcher.group(0));
                // System.out.println("matcher = " + matcher.group(0));
            }
        }
    }


    private void verifyNamesOfDeclaredVars(CTestSpecification testSpec,
                                           List<TestSpecStatus> errorsList) {
        StrVector vars = new StrVector();
        testSpec.getLocalVariablesKeys(vars);
        for (int i = 0; i < vars.size(); i++) {
            String var = vars.get(i);
            // some users declare host variables there
            if (var.startsWith("${")) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Variable '" + var + "' in section 'Variables' is host variable.\n"
                                                  + "Host variables can not be declared - they can only have value assigned."));
            }
        }
    }


    private void verifyStackUsageSection(CTestSpecification testSpec,
                                         List<TestSpecStatus> errorsList) {
        CTestStackUsage testSpecStackUsage = testSpec.getStackUsage(true);
        if (!testSpecStackUsage.isEmpty()) {
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            if (model != null) {
                CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
                CTestBaseList stackUsageList = envConfig.getStackUsageOptions(true);

                String testCaseCoreId = 
                        CoreIdUtils.getConfiguredCoreID(envConfig, testSpec.getCoreId());
                
                for (int idx = 0; idx < stackUsageList.size(); idx++) {
                    CStackUsageConfig stackUsageCfg = 
                            CStackUsageConfig.cast(stackUsageList.get(idx));
                    
                    String stackCfgCoreId = 
                            CoreIdUtils.getConfiguredCoreID(envConfig, stackUsageCfg.getCoreId());
                    
                    // there is a test case with stack usage specified, but stack measurement
                    // is not active.
                    if (testCaseCoreId.equals(stackCfgCoreId)) {
                        if (!stackUsageCfg.isActive()) {
                            errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                            testSpec, 
                                            "Test case has stack usage specified (see section Expected), but stack \n"
                                            + "measurement is not configured nor not active - see File | Properties | Stack usage."));
                        }
                        break;
                    }
                }
            }
        }
    }


    private void verifyExpectedSection(CTestSpecification testSpec, 
                                       List<TestSpecStatus> errorsList) {
        
        StrVector expectedResults = new StrVector();
        testSpec.getExpectedResults(expectedResults);
        
        int numItems = (int) expectedResults.size();
        for (int i = 0; i < numItems; i++) {
            String expr = expectedResults.get(i).trim();
            
            if (expr.length() > 0) {
                boolean isExprOK = false;
                // check if it is only a number - makes no sense to specify it
                for (int j = 0; j < expr.length(); j++) {
                    if (!Character.isDigit(expr.charAt(j))) {
                        isExprOK = true;
                    }
                }

                if (!isExprOK) {
                    errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                      testSpec, 
                                                      "Expression is single number independent of test result: " + expr));
                }
            } 
        }
        
    }
    
    
    private void verifyFunctionsInStubs(CTestSpecification testSpec,
                                       List<TestSpecStatus> errorsList,
                                       FunctionGlobalsProvider funcProvider) {

        if (funcProvider.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }
        
        CTestBaseList stubs = testSpec.getStubs(true);
        int numStubs = (int) stubs.size();
        
        for (int i = 0; i < numStubs; i++) {
            CTestStub stub = CTestStub.cast(stubs.get(i));
            String functionName = stub.getFunctionName();
            
            try {
                if (!functionExists(funcProvider, functionName)) {
                    errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                      testSpec, 
                                                      "Function '" + functionName + 
                            "' from stub specification not found in executable."));
                }
            } catch (SException ex) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Section 'Stubs': " + SEFormatter.getInfo(ex)));
            }
            
        }
    }


    private void verifyFunctionsInUserStubs(CTestSpecification testSpec,
                                       List<TestSpecStatus> errorsList,
                                       FunctionGlobalsProvider funcProvider) {

        if (funcProvider.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }
        
        CTestBaseList stubs = testSpec.getUserStubs(true);
        int numStubs = (int) stubs.size();
        
        for (int i = 0; i < numStubs; i++) {
            CTestUserStub stub = CTestUserStub.cast(stubs.get(i));
            String stubbedFuncName = stub.getFunctionName();
            
            try {
                if (!functionExists(funcProvider, stubbedFuncName)) {
                    errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                      testSpec, 
                                                      "Stubbed function '" + stubbedFuncName + 
                            "' from user stub specification not found in executable!"));
                    continue;
                }
            } catch (SException ex) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Section 'User stubs': " + SEFormatter.getInfo(ex)));
            }
            
            
            JFunction jStubbedFunc = null;
            try {
                jStubbedFunc = getJFunc(funcProvider, stubbedFuncName);
            } catch (SException ex) {
                errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                  testSpec,
                                                  "Section 'User stubs': " + SEFormatter.getInfo(ex)));
            }

            if (jStubbedFunc == null) {
                errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                        testSpec, 
                        "Stubbed function '" + stubbedFuncName + 
                        "' from user stub specification not found in executable."));
                continue;
            }
            
            String replacementFuncName = stub.getReplacementFuncName();
            
            if (!replacementFuncName.isEmpty()) {  // it is not skip stub

                JFunction jReplFunc = null;
                try {
                    jReplFunc = getJFunc(funcProvider, replacementFuncName);
                } catch (SException ex) {
                    errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                      testSpec,
                                                      "Section 'User stubs': " + SEFormatter.getInfo(ex)));
                }

                if (jReplFunc == null) {
                    errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                            testSpec, 
                            "Replacement function '" + replacementFuncName + 
                            "' from user stub specification not found in executable."));
                } else {

                    String stubbedFuncPrototype = jStubbedFunc.getPrototype();
                    String replFuncPrototype = jReplFunc.getPrototype();

                    if (!stubbedFuncPrototype.equals(replFuncPrototype)) {
                        errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                            testSpec, 
                                            "Prototypes of stubbed and replacement functions must be the same!\n" +
                                            "Stubbed '" + stubbedFuncName + "':        " + stubbedFuncPrototype + 
                                            "\nReplacement '" + replacementFuncName + "': " + replFuncPrototype));
                    }
                }
            }
        }
    }


    /**
     * Checks if the same function is stubbed in normal and user stubs. 
     */
    private void compareStubsAndUserStubs(CTestSpecification testSpec,
                                          List<TestSpecStatus> errorsList) {
        
        CTestBaseList stubs = testSpec.getUserStubs(true);
        int numStubs = (int) stubs.size();
        Set<String> userStubs = new TreeSet<>();
        
        for (int i = 0; i < numStubs; i++) {
            CTestUserStub stub = CTestUserStub.cast(stubs.get(i));
            userStubs.add(stub.getFunctionName());
        }
        
        stubs = testSpec.getStubs(true);
        numStubs = (int) stubs.size();
        
        for (int i = 0; i < numStubs; i++) {
            CTestStub stub = CTestStub.cast(stubs.get(i));
            String stubbedFuncName = stub.getFunctionName();
            if (userStubs.contains(stubbedFuncName)) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                   testSpec, 
                                   "Function '" + stubbedFuncName + 
                                   "' must not be stubbed as normal stub and user stub\n" +
                                   "in the same test case!"));
            }
        }
    }
    
        
    private void verifyFunctionsInCoverage(CTestSpecification testSpec,
                                          List<TestSpecStatus> errorsList,
                                          FunctionGlobalsProvider funcProvider) {

        if (funcProvider.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }

        CTestAnalyzerCoverage cvrg = testSpec.getAnalyzer(true).getCoverage(true);
        CTestBaseList stats = cvrg.getStatisticsList(true);
        int numStats = (int)stats.size();
        
        for (int i = 0; i < numStats; i++) {
            CTestCoverageStatistics stat = cvrg.getStatistics(i);
            String functionName = stat.getFunctionName();
            
            try {
                if (!functionExists(funcProvider, functionName)) {
                    errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                      testSpec, 
                                                      "Function '" + functionName + 
                            "' from coverage specification not found in executable."));
                }
            } catch (SException ex) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Section 'Coverage': " + SEFormatter.getInfo(ex)));
            }
        }
    }

    
    private void verifyFunctionsInProfiler(CTestSpecification testSpec,
                                          List<TestSpecStatus> errorsList,
                                          FunctionGlobalsProvider funcProvider) {

        if (funcProvider.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }

        CTestAnalyzerProfiler profiler = testSpec.getAnalyzer(true).getProfiler(true);
        CTestBaseList areas = profiler.getAreas(EAreaType.CODE_AREA, true);
        int numAreas = (int) areas.size();
        
        for (int i = 0; i < numAreas; i++) {
            CTestProfilerStatistics area = profiler.getArea(EAreaType.CODE_AREA, i);
            String areaName = area.getAreaName();
            try {
                if (!functionExists(funcProvider, areaName)) {
                    errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                      testSpec, 
                                                      "Function '" + areaName + 
                            "' from profiler specification not found in executable."));
                }
            } catch (SException ex) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Section 'Profiler': " + SEFormatter.getInfo(ex)));
            }
        }
    }


    private JFunction getJFunc(FunctionGlobalsProvider funcProvider,
                               String functionName) {
         JFunction jStubbedFunc = funcProvider.getCachedFunction(functionName);
         if (jStubbedFunc == null  &&  functionName.length() > 0  &&  functionName.charAt(0) == '"') {
             String strippedFuncName = FunctionGlobalsProvider.getFunctionNameWithoutModule(functionName);
             jStubbedFunc = funcProvider.getCachedFunction(strippedFuncName);
         }
         return jStubbedFunc;
    }


    private boolean functionExists(FunctionGlobalsProvider funcProvider,
                                   String functionName) {
        
        return funcProvider.isFunctionExists(functionName);
    }


    /*
     * Each variable must have either type or value defined. If it has type 
     * defined, and matches name of global variable, a warning is issued.
     */
    private void verifyUndefinedVariables(CTestSpecification testSpec,
                                List<TestSpecStatus> errorsList) {

        // first verify if each var has at least one of type or value defined
        StrStrMap declarations = new StrStrMap();
        StrVector initValues = new StrVector();
        StrVector initKeys = testSpec.getInitKeys();
        testSpec.getLocalVariables(declarations);
        testSpec.getInitValues(initValues);
        
        int numInits = (int)initKeys.size();
        for (int i = 0; i < numInits; i++) {
            String varName = initKeys.get(i);
            String value = initValues.get(i);
            
            if (value.equals("''")  &&  !declarations.containsKey(varName)) {
                errorsList.add(new TestSpecStatus(IStatus.WARNING,
                                                  testSpec,
                                                  "Variable '" + varName +
                                                  "' from 'Variables' section has neither " +
                                                  "type or value specified!"));
            }
        }
    }
    
    
    private void verifyVariablesInProfiler(CTestSpecification testSpec,
                                           List<TestSpecStatus> errorsList,
                                           VariablesGlobalsProvider varsProvider) {

        Map<String, JVariable> variablesMap = varsProvider.getCachedVariablesMap();
        if (variablesMap.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }
        
        CTestAnalyzerProfiler profiler = testSpec.getAnalyzer(true).getProfiler(true);
        int numAreas = (int) profiler.getAreas(EAreaType.DATA_AREA, true).size();
        for (int i = 0; i < numAreas; i++) {
            CTestProfilerStatistics area = profiler.getArea(EAreaType.DATA_AREA, i);
            String areaName = ProfilerDataAreaEditor.splitAreaName(area.getAreaName())[0];
            
            if (!variablesMap.containsKey(areaName)) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Variable '" + areaName + 
                                                  "' from profiler specification not found in executable."));
            }
        }
    }
    
    
    private void verifyHiddenGlobals(CTestSpecification testSpec,
                                     List<TestSpecStatus> errorsList,
                                     VariablesGlobalsProvider varsProvider) {

        Map<String, JVariable> variablesMap = varsProvider.getCachedVariablesMap();
        if (variablesMap.isEmpty()) {
            return;  // if symbols were not read from winIDEA, skip verification
        }
        StrStrMap localVars = new StrStrMap();
        testSpec.getLocalVariables(localVars);
        StrStrMapIterator iter = new StrStrMapIterator(localVars);
        
        while (iter.isValid()) {
            String key = iter.key();
            if (variablesMap.containsKey(key)) {
                errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                  testSpec, 
                                                  "Variable '" + key + 
                                                  "', which is declared in test specification hides global variable.\n" +
                                                  "        Remove declaration in testIDEA if you want to access the global variable."));
            }
            iter.inc();
        }
    }
    
    /** Verified duplicate IDs for complete model. */
    public List<TestSpecStatus> verifyDuplicateIds() {
        CTestSpecification testSpec = TestSpecificationModel.getActiveModel().getRootTestSpecification();
        TreeMap<String, CTestTreeNode> idsMap = new TreeMap<>();
        List<TestSpecStatus> statusList = new ArrayList<TestSpecStatus>();
        
        verifyDuplicateIds(testSpec, idsMap, statusList);
        
        return statusList;
    }
    
    
    private void verifyDuplicateIds(CTestTreeNode testTreeNode,
                                    TreeMap<String, CTestTreeNode> idsMap,
                                    List<TestSpecStatus> statusList) {
    
        verifyDuplicateId(testTreeNode, idsMap, statusList);
        
        CTestBaseList children = testTreeNode.getChildren(true);
        int numDerived = (int) children.size();
        for (int i = 0; i < numDerived; i++) {
            CTestTreeNode derivedTS = CTestTreeNode.cast(children.get(i));
            verifyDuplicateIds(derivedTS, idsMap, statusList);
        }
    }

    
    /** Verified duplicate ID for the given test spec. */
    private void verifyDuplicateId(CTestTreeNode testTreeNode,
                                   TreeMap<String, CTestTreeNode> idsMap,
                                   List<TestSpecStatus> statusList) {

        String id = testTreeNode.getId();
        if (!id.isEmpty()  &&  idsMap != null) {
            if (idsMap.containsKey(id)) {
                TestSpecStatus status = new TestSpecStatus(IStatus.WARNING, 
                                                           idsMap.get(id), 
                                                           "Duplicate ID found (1): " + id);
                statusList.add(status);
                status = new TestSpecStatus(IStatus.WARNING, 
                                            testTreeNode, 
                                            "Duplicate  ID found (2): " + id);
                statusList.add(status);
            } else {
                idsMap.put(id, testTreeNode);
            }
        }
    }

    
    private void storeStatus(String description, String message, 
                             CTestTreeNode testTreeNode, 
                             TBControl.EHControlId hControlId,
                             List<TestSpecStatus> errorsList) {
        
        storeStatus(description, message, testTreeNode, hControlId, errorsList, 
                    IStatus.WARNING);
    }
    
    
    private void storeStatus(String description, String message, 
                             CTestTreeNode testTreeNode, 
                             TBControl.EHControlId hControlId,
                             List<TestSpecStatus> errorsList,
                             int status) 
    {
        if (message != null) {
            message = description + '\n' + message;
            if (hControlId != null) {
                m_sectionStatusMap.put(hControlId,
                                       new SectionStatus(status, message));
            }

            errorsList.add(new TestSpecStatus(status, 
                                              testTreeNode, 
                                              message));
        }
    }

    
    private void verifyScriptsSection(CTestSpecification testSpec,
                                      List<TestSpecStatus> errorsList) {
        String msg = verifyScriptFuncName(testSpec.getInitTargetFunction(true).getName());
        storeStatus("Name of script function for target initialization is not valid!", 
                    msg, testSpec, EHControlId.EScriptInitTargetFName,
                    errorsList);
        
        msg = verifyScriptFuncName(testSpec.getInitFunction(true).getName());
        storeStatus("Name of script function for test initialization is not valid!",
                    msg, testSpec, EHControlId.EScriptInitFuncFName,
                    errorsList);
        
        msg = verifyScriptFuncName(testSpec.getEndFunction(true).getName());
        storeStatus("Name of script function for test end is not valid!",
                    msg, testSpec, EHControlId.EScriptEndFuncFName,
                    errorsList);
        
        msg = verifyScriptFuncName(testSpec.getRestoreTargetFunction(true).getName());
        storeStatus("Name of script function for target restore is not valid!",
                    msg, testSpec, EHControlId.EScriptRestoreTargetFName,
                    errorsList);
        
    }

    
    private void verifyAnalyzerSection(CTestSpecification testSpec,
                                       List<TestSpecStatus> errorsList) {
        CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
        if (analyzer.getRunMode() != ERunMode.M_OFF) {
            if (analyzer.getDocumentFileName().isEmpty()) {
                TestSpecStatus status = new TestSpecStatus(IStatus.ERROR, 
                        testSpec, "Section 'Analyzer': If analyzer should be started, the 'Document " +
                            "file' should be specified!");
                    errorsList.add(status);
            }
            
            if (!ToggleAnalyzerOnOffHandler.isAnalyzerOn()) {
                TestSpecStatus status = new TestSpecStatus(IStatus.WARNING, 
                                                           testSpec, "Section 'Analyzer' is active, but analyzer is globally disabled, " +
                                                           "see main menu 'Test | Disable Analyzer'!");
                                                       errorsList.add(status);
            }
        }
    }


    private void verifyCoverageSection(CTestSpecification testSpec,
                                                        List<TestSpecStatus> errorsList) {
        CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
        CTestAnalyzerCoverage cvrg = analyzer.getCoverage(true);
        if (!cvrg.isEmpty()  &&  cvrg.isMeasureAllFunctions() != ETristate.E_TRUE) {
            if (analyzer.getRunMode() != ERunMode.M_OFF  &&  cvrg.isActive() == ETristate.E_TRUE) {
                if (cvrg.getStatisticsList(true).size() == 0) {
                    TestSpecStatus status = new TestSpecStatus(IStatus.ERROR, 
                                   testSpec, "If coverage should be recorded, specify " +
                                   "at least one function in coverage statistics section!");
                    errorsList.add(status);
                }
            }
        }
    }


    private void verifyProfilerSection(CTestSpecification testSpec,
                                       List<TestSpecStatus> errorsList) {
        CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
        CTestAnalyzerProfiler profiler = analyzer.getProfiler(true);
        if (profiler.isActive() == ETristate.E_TRUE  &&  analyzer.getRunMode() != ERunMode.M_OFF) {

            if (profiler.getAreas(EAreaType.CODE_AREA, true).size() == 0  && 
                    profiler.isMeasureAllFunctions() != ETristate.E_TRUE  &&
                    profiler.getAreas(EAreaType.DATA_AREA, true).size()  == 0) {
                
                TestSpecStatus status = new TestSpecStatus(IStatus.ERROR, 
                                                           testSpec, "Because profiler is active, you should do at least one of:\n" +
                          "- specify at least one function in code areas section\n"
                        + "- select checbox 'Measure all functions' in code areas section\n"
                        + "- add data item(s) in data areas section!");
                errorsList.add(status);
            }
        }
    }


    private void verifyTraceSection(CTestSpecification testSpec) {
        CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
        CTestAnalyzerTrace trace = analyzer.getTrace(true);
        if (trace.isActive() == ETristate.E_TRUE) {
            // no tests 
        }
    }


    private void verifyStubsSection(CTestSpecification testSpec,
                                    List<TestSpecStatus> errorsList) {
        CTestBaseList stubs = testSpec.getStubs(true);
        int numStubs = (int)stubs.size();
        
        for (int i = 0; i < numStubs; i++) {
            CTestStub stub = CTestStub.cast(stubs.get(i));

            String functionName = stub.getFunctionName();
            if (functionName.isEmpty()) {
                errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                                  testSpec, 
                                                  "Stubs with empty function name are ignored."));
            }
            
            StrVector stubParams = new StrVector();
            stub.getParamNames(stubParams);
            for (int pIdx = 0; pIdx < stubParams.size(); pIdx++) {
                String stubFuncParam = stubParams.get(pIdx);
                String msg = verifyAlphanumericScalar(stubFuncParam);
                storeStatus("Name of stub function parameter is not valid!" +
                            "\n  stub: " + stub.getFunctionName() +
                            "\n  param: " + stubFuncParam,
                            msg, testSpec, EHControlId.EStubParams,
                            errorsList, IStatus.ERROR);
            }
            
            String retValName = stub.getRetValName();
            
            CTestBaseList steps = stub.getAssignmentSteps(true);
            for (int stepIdx = 0; stepIdx < steps.size(); stepIdx++) {
                CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(stepIdx));
                CMapAdapter assignments = new CMapAdapter(step, 
                    CTestEvalAssignStep.EStepSectionIds.E_SECTION_ASSIGN.swigValue(), 
                                                          true);
                StrVector keys = new StrVector();
                assignments.getKeys(keys);
                
                if (!retValName.isEmpty()) {

                    verifyStubAssignments(testSpec,
                                          errorsList,
                                          stub,
                                          retValName,
                                          keys);
                }
            }
            
            // stub script function
            String msg = verifyScriptFuncName(stub.getScriptFunctionName());
            storeStatus("Name of stub script function is not valid!" +
                        "\n  stub: " + stub.getFunctionName(),
                        msg, testSpec, EHControlId.EStubScriptFuncName,
                        errorsList);
            
        }
    }


    private void verifyStubAssignments(CTestSpecification testSpec,
                                       List<TestSpecStatus> errorsList,
                                       CTestStub stub,
                                       String retValName,
                                       StrVector keys) {
        boolean isRetValAssigned = false;
        String retValNameDot = retValName + ".";
        int numKeys = (int)keys.size();
        
        // Examples of invalid assignments: 
        // rv   counter
        //  4   5
        // 
        // rv.a  rv.b  counter
        //  4    5     6
        
        // Examples of valid assignments: 
        // counter   rv
        //  4        5
        // 
        // counter  rv.a  rv.b 
        //  4       5     6
        
        for (int j = 0; j < numKeys; j++) {
            String lvalue = keys.get(j);
            
            if (!isRetValAssigned) {
                if (lvalue.equals(retValName)  ||  lvalue.startsWith(retValNameDot)) {
                    isRetValAssigned = true;
                }
            } else {
                if (!(lvalue.equals(retValName)  ||  lvalue.startsWith(retValNameDot))) {
                    storeStatus("Variable with stub return value should be the last " +
                            "in assignments list, \n" +
                            "because some compilers use the same registers for " +
                            "function parameters!" +
                            "\n  stub: " + stub.getFunctionName(), "", 
                            testSpec, EHControlId.EStubAssignments,
                            errorsList);
                }
            }
        }

        if (!isRetValAssigned  &&   // the above loop does not check the last assignment  
            stub.getScriptFunctionName().isEmpty()) {  // if script is defined, 
                               // assignment of retVal is not mandatory in test spec. itself,
                               // because the script can and will likely assign a value
            String lvalue = "";
            
            if (numKeys > 0) {
                lvalue = keys.get(numKeys - 1);
            }

            // the last expression should match either 'rv = 3' or 'rv.m_counter = 3'
            if (numKeys == 0 || !(lvalue.equals(retValName)  ||  lvalue.startsWith(retValNameDot))) {
                    storeStatus("Name of variable for stub return value is specified, but it " +
                        "is not assigned any value!\n" +
                        "Delete variable for stub return value, or assign it a value in the 'Assignments' table!" +
                        "\n  stub: " + stub.getFunctionName(), "",                        
                        testSpec, EHControlId.EStubAssignments,
                        errorsList);
                }
        }

        // stub ret val
        String msg = verifyAlphanumericScalar(retValName);
        storeStatus("Name of variable for stub return value is not valid!" +
                "\n  stub: " + stub.getFunctionName(), msg,                        
                testSpec, EHControlId.EStubRetValName,
                errorsList);
        
        // this warning was added because of customer complaint B008041,
        // when assignments have changed script output.
        if (!keys.isEmpty()  &&  !stub.getScriptFunctionName().isEmpty()) {
            storeStatus("Stub script function AND assignments " +
                    "are specified. Please be aware that\n" +
                    "assignments are executed BEFORE the script function is called!" +
                    "\n  stub: " + stub.getFunctionName(), "",                        
                    testSpec, EHControlId.EStubAssignments,
                    errorsList, IStatus.INFO);
        }
    }

    
    private void verifyTestPointsSection(CTestSpecification testSpec,
                                         List<TestSpecStatus> errorsList) {
        CTestBaseList testPoints = testSpec.getTestPoints(true);
        int numTPs = (int)testPoints.size();
        
        for (int i = 0; i < numTPs; i++) {
            CTestPoint testPoint = CTestPoint.cast(testPoints.get(i));

            String tpId = testPoint.getId();
            if (tpId.isEmpty()) {
                errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                  testSpec, 
                                                  "Test points with empty ID are not allowed!"));
            }
        
            CTestLocation loc = testPoint.getLocation(true);
            verifyLocation(testSpec, errorsList, loc, 
                           "Section 'Test Points', id = " + testPoint.getId());
            
            // stub script function
            String msg = verifyScriptFuncName(testPoint.getScriptFunctionName());
            storeStatus("Name of test point script function is not valid!" +
                    "\n  testPoint: " + testPoint.getId(),
                    msg, testSpec, EHControlId.EStubScriptFuncName,
                    errorsList);
        }
    }


    private void verifyLocation(CTestSpecification testSpec,
                                List<TestSpecStatus> errorsList,
                                CTestLocation loc,
                                String msg) {
        if (loc.getResourceName().isEmpty()) {
            errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                              testSpec, 
                                              msg + " Location must have "
                                              + "resource name (function or file or address) defined!"));
        }
        
        if (loc.isSearch() == ETristate.E_TRUE) {
            if (loc.getMatchingType() == EMatchingType.E_MATCH_PLAIN  ||  
                    loc.getMatchingType() == EMatchingType.E_MATCH_REG_EX) {
                if (loc.getSearchPattern().isEmpty()) {
                    errorsList.add(new TestSpecStatus(IStatus.ERROR, 
                                                      testSpec, 
                                                      msg + " Location has search for text "
                                                      + "or regex specified, but "
                                                      + "pattern is empty!"));
                }
            }
        }
    }

    
    private void verifyDryRunSection(CTestSpecification testSpec,
                                     List<TestSpecStatus> errorsList) {
        CTestDryRun dryRun = testSpec.getDryRun(true);
        String multiplier = dryRun.getProfilerMultiplier().trim();
        String offset = dryRun.getProfilerOffset().trim();
        
        if (!multiplier.isEmpty()  &&  !NumberUtils.isNumber(multiplier)) {
            storeStatus("Profiler statistics multiplier should be number in Dry run section.", 
                        "", testSpec, 
                        EHControlId.EDryRunProfMultiplier, errorsList);
        }
        
        if (!offset.isEmpty()  &&  !NumberUtils.isNumber(offset)) {
            storeStatus("Profiler statistics offset should be number in Dry run section.", 
                        "", testSpec, 
                        EHControlId.EDryRunProfOffset, errorsList);
        }
    }

    
    private void verifyDiagrams(CTestSpecification testSpec,
                                List<TestSpecStatus> errorsList) {
        
        Map<String, Integer> fileNames = new TreeMap<>();
        
        CTestDiagrams diagrams = testSpec.getDiagrams(true);
        
        CTestBaseList diagConfigsList = diagrams.getConfigurations(true);
        int numDiagConfigs = (int) diagConfigsList.size();
        for (int idx = 0; idx < numDiagConfigs; idx++) {
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigsList.get(idx));
        
            CTestHostVars hostVars = CTestHostVars.createDiagramVars(testSpec, diagConfig);
            String fileName = hostVars.replaceHostVars(diagConfig.getOutputFileName());
            if (fileNames.containsKey(fileName)) {
                storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                            "  Output file name '" + fileName + "' is already used in line " + 
                            fileNames.get(fileName) + ".\n"
                            + "  Please change file name in column 'outFile'. Example: '${_testID}-${_funcUnderTest}-${_diagramType}-1.png'.", 
                            testSpec, 
                            EHControlId.EDiagramsTable, errorsList);
            } else {
                fileNames.put(fileName, idx);
            }
            
            
            EDiagType diagType = diagConfig.getDiagramType();
            String scriptName = diagConfig.getScriptName();
            
            if (diagType == EDiagType.ERuntimeCallGraph  ||  
                    diagType == EDiagType.EStaticCallGraph  ||
                    diagType == EDiagType.EFlowChart  ||
                    diagType == EDiagType.ESequenceDiagram) {

                if (!scriptName.isEmpty()) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Script name is ignored for all diagram types but 'custom'.", 
                                testSpec, 
                                EHControlId.EDiagramsTable, errorsList);
                }
            }

            if (diagType == EDiagType.ECustomAsync) {
                if (diagConfig.isAddToReport() == ETristate.E_TRUE) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Diagrams of type 'custom-async' can not be added to reports.\n"
                                + "  Please uncheck the 'isAddToReport' check-box.", 
                                testSpec, 
                                EHControlId.EDiagramsTable, errorsList);
                }
            }

            /* Empty script name means image was generated by script extensions, or
             * by stub or test point scripts.
              if (diagType == EDiagType.ECustom  ||  
            
                    diagType == EDiagType.ECustomAsync) {

                if (scriptName.isEmpty()) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                " Script name is missing. It is mandatory for custom diagrams.", 
                                testSpec, 
                                EHControlId.EDiagramsTable, errorsList);
                }
            } */

            if (diagType == EDiagType.ERuntimeCallGraph  ||  
                    diagType == EDiagType.ESequenceDiagram) {

                CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
                if (analyzer.getRunMode() != CTestAnalyzer.ERunMode.M_START) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Sequence diagrams and call graphs require active analyzer,\n"
                                        + "  but it is currently set to OFF.", 
                                        testSpec, 
                                        EHControlId.EDiagramsTable, errorsList);
                }


                CTestAnalyzerProfiler profiler = analyzer.getProfiler(true);
                if (profiler.isActive() != ETristate.E_TRUE) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Sequence diagrams and call graphs require active profiler,\n"
                                        + "  but it is currently disabled.", 
                                        testSpec, 
                                        EHControlId.EDiagramsTable, errorsList);
                }

                EProfilerExportFormat exportFmt = profiler.getExportFormat();
                if (exportFmt != EProfilerExportFormat.EProfilerAsXML &&
                        exportFmt != EProfilerExportFormat.EProfilerAsXMLBinaryTimeline) {

                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Sequence diagrams and call graphs require profiler export in,\n"
                                        + "  XML format, but it is set to " + exportFmt, 
                                        testSpec, 
                                        EHControlId.EDiagramsTable, errorsList);
                }

                String exportFile = profiler.getExportFileName();
                if (exportFile.isEmpty()) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Sequence diagrams and call graphs require profiler export to file,\n"
                                        + "  but export file name is empty.", 
                                        testSpec, 
                                        EHControlId.EDiagramsTable, errorsList);
                }

                boolean isTimeline = profiler.isSaveHistory() == ETristate.E_TRUE;
                if (!isTimeline) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Sequence diagrams and call graphs require timeline recorded by profiler,\n"
                                        + "  but it is currently not saved (see section 'Analyzer | Profiler').", 
                                        testSpec, 
                                        EHControlId.EDiagramsTable, errorsList);
                }

                ETristate isMeasureAllFuncs = profiler.isMeasureAllFunctions();
                CTestBaseList codeAreas = profiler.getCodeAreas(true);
                if (isMeasureAllFuncs != ETristate.E_TRUE  &&  codeAreas.size() < 2) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                "  Sequence diagrams and call graphs require recording of more than one function to be useful.\n"
                                        + "  Add additional functions to profiler section 'Code areas', or select chckbox\n"
                                        + "  'Measure All Functions' in the same section.", 
                                        testSpec, 
                                        EHControlId.EDiagramsTable, errorsList);
                }
                
                CTestBaseList stubs = testSpec.getStubs(true);
                int numStubs = (int) stubs.size();
                for (int stubIdx = 0; stubIdx < numStubs; stubIdx++) {
                    CTestStub stub = CTestStub.cast(stubs.get(stubIdx));
                    if (stub.isActive()) {
                        storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                    "  Sequence diagrams and call graphs require complete profiler recording. Stubs\n"
                                            + "  truncate the recording, so information for diagrams may not be complete.\n"
                                            + "  Remove stubs or make them inactive.", 
                                            testSpec, 
                                            EHControlId.EDiagramsTable, errorsList);
                    }
                }
                
                CTestBaseList testPoints = testSpec.getTestPoints(true);
                int numTestPoints = (int) testPoints.size();
                for (int tpIdx = 0; tpIdx < numTestPoints; tpIdx++) {
                    CTestPoint testPoint = CTestPoint.cast(testPoints.get(tpIdx));
                    if (testPoint.isActive() != ETristate.E_FALSE) {
                        storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                    "  Sequence diagrams and call graphs require complete profiler recording. Test points\n"
                                            + "  truncate the recording, so information for diagrams may not be complete.\n"
                                            + "  Remove test points or make them inactive.", 
                                            testSpec, 
                                            EHControlId.EDiagramsTable, errorsList);
                    }
                }
            }
            
            // check that diagram is active if isAddToReport is checked
            if (diagConfig.isAddToReport() == ETristate.E_TRUE  &&  diagType != EDiagType.ECustomAsync) {
                if (diagConfig.isActive() != ETristate.E_TRUE) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                        "Diagram is not active, so it will NOT be added to test report! Modify column 'isActive'.", 
                        testSpec, 
                        EHControlId.EDiagramsTable, errorsList);
                }
                
                if (diagrams.isActive() != ETristate.E_TRUE) {
                    storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                        "Diagrams section is not active, so diagrams will NOT be added to test report!\n"
                        + "Modify checkbox 'Is active' above the table.",
                            testSpec, 
                            EHControlId.EDiagramsTable, errorsList);
                }
                
                if (diagConfig.isActive() == ETristate.E_TRUE  &&  diagrams.isActive() == ETristate.E_TRUE) {
                    TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                    if (model.getTestReportConfig().getFileName().isEmpty()) {
                        storeStatus("Section 'Diagrams', diagram in line " + idx + ".",
                                    "Diagrams are marked to be added to test report, but test report output file is not specified.\n"
                                    + "Please use command 'Test | Configure Test Report' to define report output file,\n"
                                    + "so that diagram images will be saved to correct directory for report during test run.",
                                    testSpec, 
                                    EHControlId.EDiagramsTable, errorsList);
                    }
                }
            }
        }
    }

    
    public static int statusList2String(List<TestSpecStatus> errorsList, 
                                        StringBuilder sb,
                                        int maxMessages) {

        int severity = IStatus.INFO;
        
        int counter = 0; 
        for (TestSpecStatus status : errorsList) {
            // add an empty line after each message
            int statusSeverity = status.getSeverity();
            
            String severityStr = "INFO";
            switch (statusSeverity) {
            case IStatus.ERROR:
                severity = IStatus.ERROR;
                severityStr = "ERROR";
                break;
            case IStatus.WARNING:
                severityStr = "WARNING";
                if (severity == IStatus.INFO) {
                    severity = IStatus.WARNING;
                }
                break;
            case IStatus.OK:
                severityStr = "OK";
            }
            
            sb.append("\t__").append(severityStr).append("__: ").append(status.getMessage()).append("\n\n");
            
            if (maxMessages > 0  &&  counter++ > maxMessages) { 
                sb.append("\n...\n\n"
                        + "There are more messages (" + errorsList.size() + 
                        "). Run command 'iTools | Verify Symbols' to see them in Status view.");
                break;
            }
        }
        
        return severity;
    }
    
    
    private String verifyRestrictedTextScalar(String scalar) {
        
        String error = CYAMLUtil.verifyTestId(scalar, "");
        
        if (error.isEmpty()) {
            return null;
        }
        
        return error;
    }

    
    private String verifyAlphanumericScalar(String scalar) {

        if (scalar.isEmpty()) {
            return null;
        }
        
        if (!Character.isLetter(scalar.charAt(0))  &&  (scalar.charAt(0) != '_')) {
            return("The first character must be an alphabetic letter or underscore!\n" +
                   "  item: " + scalar);
        }

        for (int i = 1; i < scalar.length(); i++) {

            Character chr = scalar.charAt(i);

            if (!Character.isLetter(chr)  && !Character.isDigit(chr)  &&  (chr != '_')) {
                return ("Non-alphanumeric characters are not allowed here!" +
                "\n  invalidString: " + scalar +
                "\n  forbiddenChar: '" + chr + "'");
            }
        }
        
        return null;
    }


    private String verifyScriptFuncName(String scalar) {

        if (scalar.isEmpty()) {
            return null;
        }
        
        if (!Character.isLetter(scalar.charAt(0))  &&  (scalar.charAt(0) != '_')) {
            return("The first character must be an alphabetic letter or underscore!\n" +
                   "  item: " + scalar);
        }

        for (int i = 1; i < scalar.length(); i++) {

            Character chr = scalar.charAt(i);

            if (!Character.isLetter(chr)  && !Character.isDigit(chr)  &&  (chr != '_') &&  (chr != '.')) {
                return ("Invalid character in script function name!" +
                "\n  invalidString: " + scalar +
                "\n  invalidChar: '" + chr + "'");
            }
        }
        
        return null;
    }
    

    private void verifySystemTests(CTestSpecification testSpec,
                                   List<TestSpecStatus> errorsList) {
        
        if (testSpec.getTestScope() != ETestScope.E_SYSTEM_TEST) {
            return;
        }
        
        if (!testSpec.isSectionEmpty(SectionIds.E_SECTION_LOCALS)) {
            errorsList.add(new TestSpecStatus(IStatus.WARNING, 
                                              testSpec, 
                                              "This system test declares test local variables in section 'Variables'.\n"
                                              + "Test local variables are ignored for system tests as they modify stack!\n"
                                              + "Use peristent variables if you need test specific variables,\n"
                                              + "and be sure to you know what you are doing (create them in safe memory area, not on stack)!"));
        }
        
        CTestStopCondition beginStopCond = testSpec.getBeginStopCondition(true);
        if (beginStopCond.getStopType() == EStopType.E_BREAKPOINT) {
            CTestLocation loc = beginStopCond.getBreakpointLocation(true);
            verifyLocation(testSpec, errorsList, loc, "Section 'System init'.");
        }
        
        CTestStopCondition endStopCond = testSpec.getEndStopCondition(true);
        if (endStopCond.getStopType() == EStopType.E_BREAKPOINT) {
            CTestLocation loc = endStopCond.getBreakpointLocation(true);
            verifyLocation(testSpec, errorsList, loc, "Section 'Execute test'.");
        }
        // it would be a good idea to issue also a warning if persistent variables 
        // are created on stack, but for this we'd need connection to winIDEA,
        // which may not always be present. Consider adding this option (location
        // of persist. vars) also to testIDEA.
    }


    
    
/*
    private String verifyGeneralTextScalar(String scalar) {
        try {
            CYAMLUtil.verifyScalar('"' + scalar + '"');
            return null;
        } catch (Exception ex) {
            return SEFormatter.exMsg2YAML(ex);
        }
    }
    
    
    private String verifyYamlList(String scalar) {
        try {
            CYAMLUtil.verifyList('[' + scalar + ']');
            return null;
        } catch (Exception ex) {
            return SEFormatter.exMsg2YAML(ex);
        }
    }

    
    private String verifyListOfRestrictedScalars(String scalar) {
        try {
            CYAMLUtil.verifyListOfLimitedScalars('[' + scalar + ']', "");
            return null;
        } catch (Exception ex) {
            return SEFormatter.exMsg2YAML(ex);
        }
    } */

    
}
