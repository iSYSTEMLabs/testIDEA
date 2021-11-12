package si.isystem.itest.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.actions.ActionFactory;

import si.isystem.connect.CSourceCodeFile;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestFilterController;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.connect.connect;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.CoreIdUtils;
import si.isystem.itest.common.FileOpenRecentDynamicMenu;
import si.isystem.itest.common.ReferenceStorage;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.handlers.EditRedoCmdHandler;
import si.isystem.itest.handlers.TestSaveTestReportCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.main.ApplicationWorkbenchWindowAdvisor;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.preferences.PreferenceInitializer;
import si.isystem.itest.preferences.TestBasePreferenceStore;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class TestSpecificationModel implements IActionExecutioner {

    public enum SrcFileFormat {SRC_YAML, SRC_C_CPP};
    
    private CTestBench m_testBench;
    private CTestSpecification m_rootTestSpecification;

    private TBModelEventDispatcher m_eventDispatcher = new TBModelEventDispatcher();
    private ActionQueue m_actionQueue = new ActionQueue(m_eventDispatcher);
    
    private String m_oldFileName;
    private SrcFileFormat m_srcFileFormat = SrcFileFormat.SRC_YAML;
    private CSourceCodeFile m_cCppSource;

    // This map stores results of tests after execution
    // private Map<Long, CTestResult> m_testResultsMap = null;

    // this set stores all test IDs from the current model. It is initialized on
    // model load, but later IDs are only added when user enters new one, but
    // IDs deleted by user are not removed from this set. These IDs are used by 
    // auto-completion.
    private Set<String> m_autocompletionIds = new TreeSet<String>();
    
    // similar as m_autocompletionIds above
    private Set<String> m_autocompletionTags = new TreeSet<String>();
    
    private boolean m_isSectionEditorDirty;
    private boolean m_isModelDirty; // this flag indicates, that some data, which is not
    // in action queue, such as configuration set with dialogs, is modified
    
    private long m_fileLength = 0;
    private long m_fileModificationTime = 0;

    
    public TestSpecificationModel() {
        m_testBench = new CTestBench();
        m_rootTestSpecification = m_testBench.getTestSpecification(false);
    }

    
    static public TestSpecificationModel getActiveModel() {
        return TestCaseEditorPart.getActiveModel();
    }

    
    /* for testing only 
     private void openTestSpec(String yamlSpec) {
     
        
        m_srcFileFormat = SrcFileFormat.SRC_YAML;

        try {
            m_testBench = CTestBench.parse(yamlSpec);
            m_rootTestSpecification = m_testBench.getTestSpecification();
            extractIdsAndTags();
            resetModel();

        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not parse test specification", 
                                  ex);
        }
    } */

    
    public static SrcFileFormat getFileFormatFromFileName(String fileName) {
        
        if (fileName.toUpperCase().endsWith("IYAML")) {
            return SrcFileFormat.SRC_YAML;
        } 
          
        return SrcFileFormat.SRC_C_CPP;
    }

    
    public void openTestSpec(String fileName, int filePos) {

        m_srcFileFormat = getFileFormatFromFileName(fileName);
        switch (m_srcFileFormat) {
        case SRC_YAML:
            m_testBench = CTestBench.load(fileName, filePos);
            break;
        case SRC_C_CPP:
            CSourceCodeFile cCppSource = new CSourceCodeFile();
            CTestBench testBench = cCppSource.load(fileName);
            
            // only if loading succeeded (there was no exception), update members
            if (m_cCppSource != null) {
                m_cCppSource.close();
            }
            
            m_cCppSource = cCppSource;
            m_testBench = testBench;
            break;
        default:
            throw new SIllegalArgumentException("Unknown source file format!")
                      .add("fileName", fileName)
                      .add("format", m_srcFileFormat.toString());
        }

        // load must be the first statement, because it can throw an exception
        FileOpenRecentDynamicMenu.addFile(fileName);
        updateFileAttributes();
        
        m_rootTestSpecification = m_testBench.getTestSpecification(false);
        extractIdsAndTags();
        resetModel();
        
        // Do not refresh here, as this triggers chain of events, and Outline 
        // view treee may not exists yet to be refreshed, if 'Close All' was executed.
//        JConnection jCon = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);
//        if (jCon != null  &&  jCon.isConnected()) {
//            new ToolsRefreshGlobalsCmdHandler().refresh(false);
//                // GlobalsConfiguration.instance().refresh();
//        }
        
        
        // make sure the root test spec has nothing but derived test specs -
        // otherwise it may not be hidden
        /* this test is redundant, because in CTestBench there may be no other tags
         * but 'tests'.
         * 
         * if (!m_rootTestSpecification.isEmptyExceptDerived()) {
                CTestSpecification spec = m_rootTestSpecification; 
                m_rootTestSpecification = new CTestSpecification();
                m_rootTestSpecification.addDerivedTestSpec(0, spec);
            } */

        /* if (m_rootTestSpecification.hasDerivedSpecs()) {
            m_selectedTestSpec = m_rootTestSpecification.getDerivedTestSpec(0);
        } */

        m_eventDispatcher.fireEvent(new ModelChangedEvent(EventType.NEW_MODEL, null, null));
        
        String warnings = m_testBench.getWarnings();
        if (!warnings.isEmpty()) {
            throw new SIllegalStateException("Test specification has been loaded with warnings!")
                                             .add("warnings", warnings);
        }
    }

    
    /**
     * If the specified file exists, it is parsed, otherwise the model is cleared 
     * and the given file name is kept, and later used for save.
     * 
     * @param yamlFile file to open or create
     * @param i file offset
     * @throws IOException if file exists, but opening or parsing of file fails. 
     */
    public void openOrCreateTestSpec(String yamlFile, int i) throws IOException {
        File file = new File(yamlFile);
        if (file.exists()) {
            openTestSpec(yamlFile, i);
        } else {
            clearModel();
            m_testBench.setFileName(yamlFile);
        }
    }

    
    public void reload() throws IOException {
        openTestSpec(m_testBench.getFileName(), 0);
    }

    
    public void saveModel() {

        saveModelAs(m_testBench.getFileName(), m_srcFileFormat);
    }

    
    public void saveModelAs(String fileName) {
        SrcFileFormat fileFormat = getFileFormatFromFileName(fileName);
        if (fileFormat == SrcFileFormat.SRC_YAML) {
            if (m_cCppSource != null) {
                m_cCppSource.close();
                m_cCppSource = null; // release resources, which are no longer accessible
            }
        }
        saveModelAs(fileName, fileFormat);
    }
    
    
    public void saveModelAs(String fileName, SrcFileFormat srcFileFormat) {

        try {
            if (m_srcFileFormat == SrcFileFormat.SRC_YAML  &&  srcFileFormat == SrcFileFormat.SRC_C_CPP) {
                throw new SIllegalArgumentException("Can not write YAML file as C/C++ source code " + 
                        "file, because there is no C/C++ source available.")
                .add("fileName", fileName);
            }

            switch (srcFileFormat) {
            case SRC_YAML:
                m_testBench.save(fileName, false, true);
                break;
            case SRC_C_CPP:
                if (m_cCppSource != null) {
                    m_cCppSource.saveAs(fileName, m_testBench, true);
                } else {
                    throw new SIllegalArgumentException("Can not write YAML file as C/C++ source code " + 
                            "file, because there is no C/C++ source available.")
                    .add("fileName", fileName);
                }
                break;
            default:
                throw new SIllegalArgumentException("Unknown file format for saving test specifications with new file name!")
                .add("format", m_srcFileFormat)
                .add("fileName", fileName);
            }

            m_srcFileFormat = srcFileFormat;
            m_actionQueue.rememberSavePoint();
            m_isModelDirty = false;
            m_isSectionEditorDirty = false;

            m_oldFileName = fileName;

            updateFileAttributes();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Saving failed! Try to copy-paste test cases to text editor to preserve your work, and contact iSYSTEM support!", ex);
        }
    }

    
   /* private  void saveAsYAML(String fileName) {
        try {
            m_testBench.save(fileName, false);
            m_actionQueue.rememberSavePoint();
            m_isSectionEditorDirty = false;
            ReferenceStorage.getFileSaveCmdHandler().fireEnabledStateChangedEvent();
            m_fileName = fileName;
            Discovery.INSTANCE.register(m_fileName);
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not save test specification", 
                                  ex);
        }
    } */


    public String getModelFileName() {
        return m_testBench.getFileName();
    }

    
    public SrcFileFormat getSrcFileFormat() {
        return m_srcFileFormat;
    }

    
    public void clearModel() {
        
        m_oldFileName = m_testBench.getFileName();

        m_testBench = new CTestBench();
        m_rootTestSpecification = m_testBench.getTestSpecification(false);
        extractIdsAndTags();
        initProjectProperties(m_testBench.getTestEnvironmentConfig(false));

        resetModel();

        m_eventDispatcher.fireEvent(new ModelChangedEvent(EventType.NEW_MODEL, 
                                                          null, null));
    }

    
    public static String createDefaultTestBenchAsString() {
        
        CTestBench testBench = new CTestBench();
        initProjectProperties(testBench.getTestEnvironmentConfig(false));
        
        CTestReportConfig reportCfg = testBench.getTestReportConfig(false);
        reportCfg.setXsltForFullReport(connect.getBUILT_IN_XSLT_PREFIX() + " " + connect.getDEFAULT_XSLT_NAME());
        reportCfg.setTagValue(CTestReportConfig.ETestReportConfigSectionIds.E_SECTION_CSS_FILE.swigValue(), 
                              connect.getBUILT_IN_XSLT_PREFIX() + " " + connect.getDEFAULT_CSS_NAME());
        
        return testBench.toString();
    }
    
    
    public static void initProjectProperties(CTestEnvironmentConfig envConfig) {

        try {
            // set default values in project properties
            PreferenceInitializer prefInitializer = new PreferenceInitializer();
            prefInitializer.initializeDefaultPreferences();
            TestBasePreferenceStore envPrefStore = new TestBasePreferenceStore(envConfig);
            TestBasePreferenceStore stackUsagePrefStore = new TestBasePreferenceStore(envConfig);
            TestBasePreferenceStore scriptPrefStore = new TestBasePreferenceStore(envConfig.getScriptConfig(false));
            TestBasePreferenceStore toolsPrefStore = new TestBasePreferenceStore(envConfig.getToolsConfig(false));
            TestBasePreferenceStore evaluatorPrefStore = new TestBasePreferenceStore(envConfig.getEvaluatorConfig(false));
            TestBasePreferenceStore testCaseInitPrefStore = new TestBasePreferenceStore(envConfig.getTestCaseTargetInitConfig(false));

            // two pages for the same CTestBase object can use the same prefs
            prefInitializer.initializeEnvConfigPreferences(envPrefStore, false);
            prefInitializer.initializeInitSequencePreferences(envPrefStore, false);
            prefInitializer.initializeRunConfigPreferences(envPrefStore, false);
            prefInitializer.initializeMulticorePreferences(envPrefStore, false);
            prefInitializer.initializeStackUsageConfigPreferences(stackUsagePrefStore, false);
            prefInitializer.initializeScriptConfigPreferences(scriptPrefStore, false);
            prefInitializer.initializeToolsConfigPreferences(toolsPrefStore, false);
            prefInitializer.initializeEvaluatorConfigPreferences(evaluatorPrefStore, false);
            prefInitializer.initializeTestCaseInitConfigPreferences(testCaseInitPrefStore, false);
            
            envPrefStore.save();
            stackUsagePrefStore.save();
            scriptPrefStore.save();
            evaluatorPrefStore.save();
            testCaseInitPrefStore.save();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not initialize default values for project configuration.\n" +
                    "Open project properties (File | Properties) and set them manually if required.", ex);
        }
    }

    
    private void resetModel() {
        m_actionQueue.clear();
        setSectionEditorDirty(false);
        setModelDirty(false);
        TestSaveTestReportCmdHandler saveReportCmdHandler = ReferenceStorage.getSaveReportCmdHandler();
        if (saveReportCmdHandler != null) {
            saveReportCmdHandler.fireEnabledStateChangedEvent();
        }
        
        m_eventDispatcher.fireEvent(new ModelChangedEvent(EventType.UPDATE_TEST_RESULTS));
    }

    
    public ActionQueue getActionQueue() {
        return m_actionQueue;
    }
    
    
    public CTestSpecification getRootTestSpecification() {
        return m_rootTestSpecification;
    }

    
    public CTestGroup getRootTestGroup() {
        return m_testBench.getGroup(false);
    }

    
    public void addListener(ITestSpecModelListener listener) {
        m_eventDispatcher.addListener(listener);
    }


    public TBModelEventDispatcher getEventDispatcher() {
        return m_eventDispatcher;
    }
    
    
    public CTestEnvironmentConfig getTestEnvConfig() {
        CTestEnvironmentConfig ec = m_testBench.getTestEnvironmentConfig(false);
        return ec;
    }

    
    /**
     * Use this method to get separator between data area name and value when
     * encoded in single string, for example: 'my_var / 3'. 
     * @return
     */
    public static String getDataAreaValueSeparator() {
        return "/";
    }
    
    // utility methods
    public static String getFullProfilerAreaName(CTestProfilerStatistics area,
                                                 EAreaType areaType) {
        
        if (area.isAreaValueSet()  &&  areaType == CTestAnalyzerProfiler.EAreaType.DATA_AREA) {
            return getFullProfilerAreaName(area.getAreaName(), area.getAreaValue(), areaType);
        }
        
        return area.getAreaName();
    }

    
    public static String getFullProfilerAreaName(String areaName, String value, EAreaType areaType) {
        if (areaType == CTestAnalyzerProfiler.EAreaType.DATA_AREA) {
            return areaName + " " + getDataAreaValueSeparator() + " " + value;
        }
        return areaName;
    }

    
    public void setResults() {
        
        // getTestReportContainer().assign(resultsMap);
        TestSaveTestReportCmdHandler saveReportCmdHandler = ReferenceStorage.getSaveReportCmdHandler();
        if (saveReportCmdHandler != null) {
            saveReportCmdHandler.fireEnabledStateChangedEvent();
        }
    }
//
//    public void mergeResults(Map<Long, CTestResult> resultsMap) {
//        if (m_testResultsMap == null) {
//            setResults(resultsMap);
//        } else {
//            m_testResultsMap.putAll(resultsMap);
//        }
//    }
    
/*    public void clearTestCaseResults() {
        m_testBench.clearTestCaseResults();
    }
    
    
    public void putTestResult(CTestSpecification testSpec, CTestResult result) {
        m_testBench.putTestResult(testSpec, result);
    }

    
    public int getNoOfTestResults() {
        return m_testBench.getNoOfTestResults();
    }
    
    
    public void resetTestResultIterator() {
        m_testBench.resetTestResultIterator();
    }
    
    
    public boolean hasNextTestResult() {
        return m_testBench.hasNextTestResult();
    }
    
    
    public CTestResult nextTestResult() {
        return m_testBench.nextTestResult();
    }
  */
    
    public CTestReportContainer getTestReportContainer() {
        return m_testBench.getTestReportContainer();
    }

    
    public CTestFilterController getFilterController() {
        return m_testBench.getFilterController();
    }
    
    
    public CTestResult getResult(CTestTreeNode testSpec) {
        return getTestReportContainer().getTestResult(testSpec);
    }
    
    public CTestGroupResult getGroupResult(CTestTreeNode testGroup) {
        return getTestReportContainer().getGroupResult(testGroup);
    }
    
    /** Returns result for the given test specification (CTestSpecification.hashCodeAsPtr()). */
//    public CTestResult getResult(long hashCode) {
//        if (m_testResultsMap == null) {
//            return null;
//        }
//        return m_testResultsMap.get(Long.valueOf(hashCode));
//    }

    
/**    public Map<Long, CTestResult> getResultsMap() {
        return m_testResultsMap;
    }
   */ 
    
    /** 
     * Returns all test case results, which were caused by exception - this usually
     * means, that the test didn't execute.
     * 
     * @return
     */
    public CTestResult[] getExceptions() {
        
        List<CTestResult> list = new ArrayList<CTestResult>();
        
        if (m_testBench != null) {
            CTestReportContainer testReport = getTestReportContainer();
            testReport.resetTestResultIterator();
            
            while (testReport.hasNextTestResult()) {
                CTestResult result = testReport.nextTestResult();
                
                if (result.isException()) {
                    list.add(result);
                }
            }
        }
        
        return list.toArray(new CTestResult[list.size()]);
    }

    /**
     * Returns true, if the given test specification is the root one - the one, 
     * that is not shown in the tree.
     * 
     * @param testSpecOrGrp
     * @return
     */
    public boolean isRootTreeNode(CTestTreeNode testSpecOrGrp) {
        long hashCodeAsPtr = testSpecOrGrp.hashCodeAsPtr();
        return hashCodeAsPtr == m_rootTestSpecification.hashCodeAsPtr() ||
                hashCodeAsPtr  ==  getRootTestGroup().hashCodeAsPtr();
    }

    /**
     * This method should be used, when there is no better candidate for selection,
     * for example when base test spec is deleted.
     * 
     * @return the first child of root test spec
     */
    public CTestTreeNode getFirstSelection() {
        CTestGroup rootGroup = m_testBench.getGroup(true);
        if (rootGroup.hasChildren()) {
            return CTestTreeNode.cast(rootGroup.getChildren(true).get(0));
        }
        if (m_rootTestSpecification.hasChildren()) {
            return m_rootTestSpecification.getDerivedTestSpec(0);
        }
        return null;
    }

    
    /**
     * Utility method, which returns items in input vector as comma separated 
     * item in string. 
     */ 
    public static String getCommaSeparatatedStrings(StrVector vector) {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < vector.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(vector.get(i));
        }
        return sb.toString();
    }


    public CTestBench getTestBench() {
        return m_testBench;
    }


    public CTestEnvironmentConfig getCEnvironmentConfiguration() {
        return m_testBench.getTestEnvironmentConfig(false);
    }

    
    public CTestReportConfig getTestReportConfig() {
        return m_testBench.getTestReportConfig(false);
    }


    public CTestBaseList getTestFilters() {
        return m_testBench.getTestFilters(false);
    }

    
    public void setTestFilters(CTestBaseList testFilters) {
        m_testBench.setSectionValue(CTestBench.ETestBenchSectionIds.E_SECTION_FILTERS.swigValue(), 
                                    testFilters);
    }


    public void addTestTreeNode(CTestTreeNode parent,
                                int idx,
                                CTestTreeNode testTreeNode) {
        parent.addChildAndSetParent(idx, testTreeNode);
        if (m_srcFileFormat == SrcFileFormat.SRC_C_CPP  &&  m_cCppSource != null  &&  !testTreeNode.isGroup()) {
            CTestSpecification testSpec = CTestSpecification.cast(testTreeNode);
            m_cCppSource.assignCommentLocation(testSpec);
        }
    }


    public int deleteTreeNode(CTestTreeNode node) {
        CTestTreeNode parent = node.getParentNode();
        
        if (parent != null) {
            int idx = parent.deleteChild(node);
            return idx;
        }
        
        return -2;
    }


//    public int deleteTestGroup(CTestGroup groupToBeDeleted) {
//        CTestGroup parent = CTestGroup.cast(groupToBeDeleted.getParent());
//        
//        if (parent != null) {
//            CTestBaseList children = parent.getChildren(false);
//            int idx = children.find(groupToBeDeleted);
//            if (idx >= 0) {
//                children.remove(idx);
//            }
//            return idx;
//        }
//        
//        return -2;
//    }

    
    /**
     * Returns test spec. in the model which has the same data as the given one.
     * 
     * @param testSpec
     * @return null if test spec not found, test spec from model if exactly one 
     *              found, the given test spec if more than one was found. 
     */
    public CTestSpecification findTestSpecEqualsData(CTestSpecification testSpec) {
        List<CTestSpecification> matches = new ArrayList<>();
        
        findTestSpecs(m_rootTestSpecification,
                      testSpec.getTestId(), 
                      testSpec.getFunctionUnderTest(true).getName(),
                      matches);

        List<CTestSpecification> matchesEquals = new ArrayList<>();
        
        for (CTestSpecification testSpecMatch : matches) {
            if (testSpec.equalsData(testSpecMatch)) {
                matchesEquals.add(testSpecMatch);
            }
        }
        
        if (matchesEquals.size() == 0) {
            return null;
        }
        
        if (matchesEquals.size() == 1) {
            return matchesEquals.get(0); // exactly one found
        }
        
        return testSpec;
    }
    
    
    /**
     * Returns list of test specs matching the given test ID and function, empty list
     * if no such test spec exists.
     *  
     * @param winIdeaFunctionName
     * @return
     */
    public void findTestSpecs(CTestSpecification testSpec,
                              String testID, 
                              String functionName,
                              List<CTestSpecification> matches) {
        
        if (testSpec == null) {
            return;
        }
        
        
        int noOfDerivedTestSpecs = testSpec.getNoOfDerivedSpecs();
        
        for (int i = 0; i < noOfDerivedTestSpecs; i++) {
            CTestSpecification derivedTestSpec = testSpec.getDerivedTestSpec(i);

            if (derivedTestSpec.getFunctionUnderTest(true).getName().equals(functionName)  &&
                derivedTestSpec.getTestId().equals(testID)) {
                matches.add(derivedTestSpec);
            }
            
            findTestSpecs(derivedTestSpec, testID, functionName, matches);
        }
    }


    /**
     * Returns the first test spec for the given function found, or null if
     * no such test spec exists.
     *  
     * @param winIdeaFunctionName
     * @return
     */
    public CTestSpecification findTestSpecForFunction(String winIdeaFunctionName) {
        
        if (m_rootTestSpecification != null) {
            return findTestSpecForFunction(m_rootTestSpecification, winIdeaFunctionName);
        }
        
        return null;
    }


    private CTestSpecification findTestSpecForFunction(CTestSpecification testSpec, 
                                                       String winIdeaFunctionName) {
        
        int noOfDerivedTestSpecs = testSpec.getNoOfDerivedSpecs();
        
        for (int i = 0; i < noOfDerivedTestSpecs; i++) {
            CTestSpecification derivedTestSpec = testSpec.getDerivedTestSpec(i);

            if (derivedTestSpec.getFunctionUnderTest(true).getName().equals(winIdeaFunctionName)) {
                return derivedTestSpec;
            }
            
            CTestSpecification match = findTestSpecForFunction(derivedTestSpec, winIdeaFunctionName);
            if (match != null) {
                return match;
            }
        }
        return null;
    }
    

    /**
     * Returns 'name' of the given testSpec as 'testId : funcName' string.
     */
//    public static String getTestSpecificationName(CTestSpecification testSpec) {
//        
//        if (testSpec == null) {
//            return "<null>";
//        }
//        
//        String functionName = testSpec.getFunctionUnderTest(true).getName();
//        String testId = testSpec.getTestId();
//        
//        return getTestSpecificationName(testId, functionName);
//    }


    public static String getTestSpecificationName(String testId,
                                                  String functionName) {
        if (functionName.isEmpty()) {
            functionName = "/";
        }
        if (testId.isEmpty()) {
            testId = "/";
        }
        
        return testId + " : " + functionName;
    }

    
    /**
     * Returns model outline tree - see comment of ModelOutline 
     * @return
     */
    public ModelOutlineNode getOutline() {
        ModelOutlineNode rootNode = new ModelOutlineNode("", "", false, null, 0, 0,
                                                         m_rootTestSpecification.getNoOfDerivedSpecs());
        
        fillModelOutline(rootNode, m_rootTestSpecification, 1);
        
        return rootNode;
    }
    
    private void fillModelOutline(ModelOutlineNode parentNode,
                                  CTestSpecification testSpec,
                                  int hierarchyLevel) {
        int numDerived = testSpec.getNoOfDerivedSpecs();
        
        for (int i = 0; i < numDerived; i++) {
            CTestSpecification derivedTS = testSpec.getDerivedTestSpec(i);
            ModelOutlineNode childNode = new ModelOutlineNode(derivedTS.getTestId(), 
                                              derivedTS.getFunctionUnderTest(true).getName(), 
                                              derivedTS.getRunFlag() != ETristate.E_FALSE, 
                                              derivedTS.getMergedTestScope(),
                                              hierarchyLevel,
                                              i,
                                              derivedTS.getNoOfDerivedSpecs());
            parentNode.setChild(i, childNode);
            fillModelOutline(childNode, derivedTS, hierarchyLevel + 1);
        }
    }


    /**
     * @see CoreIdUtils#getCoreIDs(CTestEnvironmentConfig)
     */
    public String [] getCoreIDs() {

        return CoreIdUtils.getCoreIDs(getCEnvironmentConfiguration());
    }
    
    
    /**
     * @see CoreIdUtils#getCoreIdIndex(CTestEnvironmentConfig, String, boolean)
     */
    public int getCoreIdIndex(String coreId, boolean isThrow) {
        
        return CoreIdUtils.getCoreIdIndex(getCEnvironmentConfiguration(), coreId, isThrow);
    }

    
    /**
     * @see CoreIdUtils#getConfiguredCoreID(CTestEnvironmentConfig, String)
     */
    public String getConfiguredCoreID(String coreId) {
        
        return CoreIdUtils.getConfiguredCoreID(getCEnvironmentConfiguration(), 
                                               coreId);
    }
    
    
    public void undo() {
        
        m_isSectionEditorDirty = false;

        m_actionQueue.undo();
        setUndoRedoMenuText(m_actionQueue.getUndoActionName(), m_actionQueue.getRedoActionName());
    }


    public void redo() {
        m_actionQueue.redo();
        setUndoRedoMenuText(m_actionQueue.getUndoActionName(), m_actionQueue.getRedoActionName());
    }
    

    @Override
    public void execAction(AbstractAction action) {
        
            m_actionQueue.exec(action);

            // if exception is thrown (action failed), skip GUI changes 
            
            // action has been submitted, so section is no longer dirty - changes
            // can be undone
            setSectionEditorDirty(false);
            
            setUndoRedoMenuText(action.getName(), null);
            
            CTestTreeNode testTreeNode = action.getContainerTreeNode();
            try {
                ModelVerifier.INSTANCE.verifyTestTreeNodeAndSetStatus(testTreeNode);
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), 
                                      "Verification failed!", ex);
            }
    }

    
    /**
     * This method sets dirty flag. It is called from listeners of input controls,
     * because they detect future changes in the model BEFORE actions are
     * sent to action queue. For example, as soon as the user types a character
     * in description field, the data is modified and can be saved, so Save
     * menu option should be enabled. But action will be pushed to the queue only
     * when user selects another section, for example.
     *   
     * @param isSectionEditorDirty
     */
    public void setSectionEditorDirty(boolean isSectionEditorDirty) {
        m_isSectionEditorDirty = isSectionEditorDirty;
        m_eventDispatcher.fireEvent(new ModelChangedEvent(EventType.MODEL_CHANGED, 
                                                          null, null));
        
        EditRedoCmdHandler redoCmdHandler = ReferenceStorage.getEditRedoCmdHandler();
        if (redoCmdHandler != null) {
            redoCmdHandler.fireEnabledStateChangedEvent();
        }
    }

    
    public boolean isSectionEditorDirty() {
        return m_isSectionEditorDirty;
    }
    
    
    public void setModelDirty(boolean isModelDirty) {
        m_isModelDirty = isModelDirty;
        m_eventDispatcher.fireEvent(new ModelChangedEvent(EventType.MODEL_CHANGED, 
                                                          null, null));
    }


    /** @see #setSectionEditorDirty(boolean) */
    public boolean isModelDirty() {
        return m_isModelDirty || m_isSectionEditorDirty  ||  m_actionQueue.isModified();
    }

    
    private void setUndoRedoMenuText(String undoInfo, String redoInfo) {

        // plug-in only
        ReferenceStorage rs = ReferenceStorage.instance();
        
        Action action = rs.getAction(ActionFactory.UNDO.getId());
        if (action != null) {
            action.setEnabled(undoInfo != null);
        }
        
        action = rs.getAction(ActionFactory.REDO.getId());
        if (action != null) {
            action.setEnabled(redoInfo != null);
        }
        // end of plug-in only
        
        
        // the implementation in commented lines for setting text below has three 
        // problems: 
        // - when testIDEA gets focus,
        // menus are refreshed by framework, so no text info is appended. 
        // - keyboard shortcuts are not displayed by this method, but this is 
        // easy to fix by appending 'Ctrl-Z' to string in this method
        // - If the user enters something into text field, previous action is still
        // displayed in menu option, because the current change in the text filed will
        // be submitted as an action later. This could be fixed by removing special text
        // on such user action.
        
        IMenuManager menuManager = ApplicationWorkbenchWindowAdvisor.getMainMenuManager();
        // IContributionItem[] contributions = menuManager.getItems(); // we have main menu items now
        // MenuManager editMenuMgr = (MenuManager)contributions[1];

        if (menuManager == null) {
            return;
        }
        
        MenuManager editMenuMgr = (MenuManager)menuManager.find("edit");
        
        Menu menu = editMenuMgr.getMenu();
        
        if (menu.getItemCount() < 2) { // menu items are created only when the menu
                                       // is shown for the first time!
            return;
        }

        if (undoInfo != null) {  
            editMenuMgr.setVisible(true); // that's a trick to make update of menu text visible
            MenuItem editItem = menu.getItem(0);
            // editItem.setText("&Undo " + undoInfo);
            editItem.setEnabled(true);
        } else {
            editMenuMgr.setVisible(true); // that's a trick to make update of menu text visible
            MenuItem editItem = menu.getItem(0);
            // editItem.setText("&Undo");
            editItem.setEnabled(false);
        }

        if (redoInfo != null) { // menu items are created only when the menu 
            // is shown for the first time!
            editMenuMgr.setVisible(true); // that's a trick to make update of menu text visible
            MenuItem editItem = menu.getItem(1);
            // editItem.setText("&Redo " + redoInfo);
            editItem.setEnabled(true);
        } else {
            editMenuMgr.setVisible(true); // that's a trick to make update of menu text visible
            MenuItem editItem = menu.getItem(1);
            // editItem.setText("&Redo");
            editItem.setEnabled(false);
        }
    }

    
    private void extractIdsAndTags() {
        m_autocompletionIds.clear();
        m_autocompletionTags.clear();
        
        if (m_rootTestSpecification != null) {
            extractIdsAndTags(m_rootTestSpecification);
        }
    }

    
    private void extractIdsAndTags(CTestSpecification testSpec) {
        
        String id = testSpec.getTestId();
        if (!id.isEmpty()) {
            m_autocompletionIds.add(id);
        }
        
        StrVector tags = new StrVector();
        testSpec.getTags(tags);
        for (int j = 0; j < tags.size(); j++) {
            m_autocompletionTags.add(tags.get(j));
        }
        
        int noOfDerived = testSpec.getNoOfDerivedSpecs();
        for (int i = 0; i < noOfDerived; i++) {
            CTestSpecification derivedTestSpec = testSpec.getDerivedTestSpec(i);
            extractIdsAndTags(derivedTestSpec);
        }
    }
    
    
    public void addAutocompletionId(String id) {
        if (!id.isEmpty()) {
            m_autocompletionIds.add(id);
        }
    }
    
    
    public void addAutocompletionTags(StrVector tags) {
        for (int j = 0; j < tags.size(); j++) {
            m_autocompletionTags.add(tags.get(j));
        }
    }
    
    
    public String[] getAutocompletionIds() {
        return m_autocompletionIds.toArray(new String[0]);
    }
    
    
    public String[] getAutocompletionTags() {
        return m_autocompletionTags.toArray(new String[0]);
    }
    
    
    public boolean hasFileChanged() {
        
        String fileName = m_testBench.getFileName();
        
        if (fileName == null  ||  fileName.isEmpty()) {
            return false; // there is no file defined, so nothing could change
        }
        
        File file = new File(fileName);
        
        long len = file.length();
        long modificationTime = file.lastModified();
        
        return (!(len == m_fileLength  &&  modificationTime == m_fileModificationTime));
    }
    
    
    public void updateFileAttributes() {
        
        String fileName = m_testBench.getFileName();

        if (fileName == null  ||  fileName.isEmpty()) {
            return; // there is no file defined, so nothing could change
        }
        
        File file = new File(fileName);
        
        m_fileLength = file.length();
        m_fileModificationTime = file.lastModified();
    }


    /* Returns indices of derived test specs upt to a root test spec.
     * @see getTestSpecification(idxPath)
     */
    public List<Integer> getIndexPath(CTestTreeNode testSpec) {
        
        List<Integer> idxPath = new ArrayList<Integer>();
        while (true) {
            CTestTreeNode parent = CTestTreeNode.cast(testSpec.getParent());
            if (parent == null) {
                break;
            }
            idxPath.add(0, parent.getChildren(true).find(testSpec));
            testSpec = parent;
        }
        
        return idxPath;
    }

    /** Returns test specification at the specified path. If the path does not
     * exist, test spec on the path, which exists is returned.
     * @param idxPath
     * @return
     */
    public CTestSpecification getTestSpec(List<Integer> idxPath) {
        CTestSpecification testSpec = m_rootTestSpecification;
        if (testSpec == null) {
            return null;
        }
        
        for (Integer idx : idxPath) {
            if (idx >= testSpec.getNoOfDerivedSpecs()) {
                break;
            }
            testSpec = testSpec.getDerivedTestSpec(idx);
        }
        
        return testSpec;
    }


    public int getNoOfTestCases() {
        
        CTestSpecification testSpec = m_rootTestSpecification;
        if (testSpec == null) {
            return 0;
        }
        
        // -1 because m_rootTestSpecification does not count - not visible to the user
        return m_rootTestSpecification.getNoOfTests(false) - 1;
    }

    
    /** Returns name of the previous file used, useful when user execute New
     * command, but expects the new model to be saved to the same location, for
     * example. 
     * @return
     */
    public String getOldModelFileName() {
        return m_oldFileName;
    }


    public boolean isUndoable() {
        return m_actionQueue.isUndoable();
    }
    
    
    public boolean isRedoable() {
        return m_actionQueue.isRedoable();
    }

    
    public boolean isUseQualifiedFuncNames() {
        return m_testBench.getTestEnvironmentConfig(true).isUseQualifiedFuncNames();
    }


    public void clearUndoRedoHistory() {
        m_actionQueue.clear();
    }


    public void refreshGroups() {
        
        CTestFilterController filterCtrl = getFilterController();
        m_testBench.assignTestCasesToGroups(filterCtrl);
    }


    /**
     * Utility function to set all Save After Test and Close After Test bits
     * in Analyzer sections of test specs members of the given parent group.
     * This is needed when coverage is active in groups, and to prevent crashing of
     * winIDEA because of too many opened analyzer files.
     */
    public void setAnalyzerSaveAfterTest(CTestGroup group) {
        GroupAction groupAction = new GroupAction("Set analyzer save after test to true");
        YamlScalar saveAfterTestValue = 
                YamlScalar.newMixed(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_IS_SAVE_AFTER_TEST.swigValue());
        saveAfterTestValue.setValue("true");
        YamlScalar closeAfterTestValue = 
                YamlScalar.newMixed(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_IS_CLOSE_AFTER_TEST.swigValue());
        closeAfterTestValue.setValue("true");
        
        setAnalyzerSaveAfterTest(group, groupAction, saveAfterTestValue, closeAfterTestValue);
        
        groupAction.addAllFireEventTypes();
        
        execAction(groupAction);
    }
    
    
    private void setAnalyzerSaveAfterTest(CTestGroup group, 
                                          GroupAction groupAction, 
                                          YamlScalar saveAfterTestValue, 
                                          YamlScalar closeAfterTestValue) {
        
        if (group.isTestSpecOwner()) {
            CTestSpecification testSpec = group.getOwnedTestSpec();
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            if (analyzer.getRunMode() == ERunMode.M_START) {
                if (analyzer.isSaveAfterTest() != ETristate.E_TRUE) {
                    SetSectionAction action = new SetSectionAction(analyzer, 
                                                                   ENodeId.ANALYZER_NODE, 
                                                                   saveAfterTestValue);
                    groupAction.add(action);
                    
                    action = new SetSectionAction(analyzer, 
                                                  ENodeId.ANALYZER_NODE, 
                                                  closeAfterTestValue);
                    groupAction.add(action);
                }
            }
        }
        
        int numTestOwners = (int) group.getTestOwnerGroupsSize();
        for (int idx = 0; idx < numTestOwners; idx++) {
            CTestGroup childGrp = group.getTestOwnerGroup(idx);
            setAnalyzerSaveAfterTest(childGrp);
        }
        
        CTestBaseList children = group.getChildren(true);
        int numGroupChildren = (int) children.size();
        for (int idx = 0; idx < numGroupChildren; idx++) {
            CTestGroup grp = CTestGroup.cast(children.get(idx));
            setAnalyzerSaveAfterTest(grp);
        }
    }
}
