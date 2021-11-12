package si.isystem.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import si.isystem.connect.CLineDescription.EFileLocation;
import si.isystem.connect.CLineDescription.EMatchingType;
import si.isystem.connect.CLineDescription.EResourceType;
import si.isystem.connect.CLineDescription.ESearchContext;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.EOpenMode;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerCoverage.EMergeScope;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerExportFormat;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFilter.EFilterTypes;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestFunction.ESection;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestLog.ESectionsLog;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestPersistentVars.EPersistVarsSections;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestProfilerStatistics.EProfilerStatisticsSectionId;
import si.isystem.connect.CTestProfilerTime;
import si.isystem.connect.CTestProfilerTime.EProfilerTimeSectionId;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.swtbot.utils.KTableTestUtils;

/**
 * This class tests editing in table in testIDEA.
 * 
 * @author markok
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class TableEditorTester {

    private static final String TOGGLE_INHERITANCE_BTN_TOOLTIP_REGEX = "Toggle inheritance of.*";
    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;
    private static KTableTestUtils m_ktableUtils;

    @BeforeClass
    public static void setup() throws Exception {
        
        SWTBotPreferences.TIMEOUT = 15000; // timeout for finding widgets in ms, default is 5 s
        // unfortunately this is also time of wait for widgets, for example for progress dialog to
        // close, which may take more than 5 seconds if there are many tests running.
        SWTBotPreferences.PLAYBACK_DELAY = 0; // playback timeout in ms - defines delay
                                              // between test calls
        m_bot = new SWTWorkbenchBot();
        m_utils = new UITestUtils(m_bot);
        m_ktableUtils = new KTableTestUtils(m_bot, m_utils);

        m_utils.openTestIDEAPerspective();
   }    

    
    @Test
    public void sectionBySection() throws Exception {

        m_utils.discardExistingAndCreateNewProject();
        m_utils.createNewBaseUnitTest("add_int", "11, 22", "rv == 33", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_ktableUtils.selectKTable(0);
        
        m_utils.selectTestSection(UITestUtils.SEC_META);
        String testId = "tid1";
        String desc = "test description";
        String [] tags = new String[]{"tag1", "tag2"};
        m_ktableUtils.setDataRowContent(0, 0, testId, desc, tags[0]);
        m_ktableUtils.addSeqColumn(3, 1);
        m_ktableUtils.setDataCell(3, 0, tags[1]);
        
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        String funcName = "max_int";
        String retVal = "retVal";
        String [] params = new String[]{"-10", "-9", "-8"};
        String timeout = "1000";
        String coreId = "coreX";
        m_ktableUtils.setDataRowContent(0, 0, funcName, params[0], params[1], retVal, timeout, coreId);
        m_ktableUtils.addSeqColumn(3, 2);
        m_ktableUtils.setDataCell(3, 0, params[2]);
        
        m_utils.selectTestSection(UITestUtils.SEC_PERSIST_VARIABLES);
        String persistVarName = "persistV1";
        String persistVarValue = "345";
        m_ktableUtils.addMapColumn(1, 1, persistVarName);
        m_ktableUtils.setDataCell(0, 0, persistVarValue);
        
        m_ktableUtils.addSeqColumn(2, 1); // add 'delete' col
        m_ktableUtils.setDataCell(1, 0, persistVarName + 'x');
        
        m_ktableUtils.clickDataCell(2,  0);
        m_ktableUtils.clickDataCell(2,  0);  // set to true

        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        String [] varsNames = new String[]{"c1", "c2"}; 
        String [] varsTypes = new String[]{"int", "MyStruct"}; 
        String [] varsVals = new String[]{"44", "67"};
        // decl locals
        m_ktableUtils.addMapColumnWValue(0, 0, varsNames[0], varsTypes[0]);
        m_ktableUtils.addMapColumn(1, 1, varsNames[1]);
        m_ktableUtils.setDataCell(1, 0, varsTypes[1]);

        // init
        m_ktableUtils.addMapColumnWValue(2, 0, varsNames[0], varsVals[0]);
        m_ktableUtils.addMapColumn(3, 1, varsNames[1]);
        m_ktableUtils.setDataCell(3, 0, varsVals[1]);

        m_utils.selectTestSection(UITestUtils.SEC_PRECONDITION);
        String[] precond = new String[]{"g_char1 < 90", "180 > g_char1"};
        m_ktableUtils.setDataCell(0, 0, precond[0]);                
        m_ktableUtils.addSeqColumn(1, 2);
        m_ktableUtils.setDataCell(1, 0, precond[1]);                

        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        m_ktableUtils.clickDataCell(0, 0);
        m_ktableUtils.clickDataCell(0, 0);  // set to true
        String exprs [] = new String[]{"retVal == 0", "0 == retVal"};
        String maxStackLimit = "0x12345";
        m_ktableUtils.setDataRowContent(1, 0, exprs[0], maxStackLimit);
        m_ktableUtils.addSeqColumn(2, 2);
        m_ktableUtils.setDataCell(2, 0, exprs[1]);
        
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        m_ktableUtils.addSeqColumn(1, 0);
        String stubbedF1 = "stubbedFuncInt";
        ETristate isActive = ETristate.E_TRUE;
        ETristate isCustomActivation = ETristate.E_FALSE;
        String stubParams = "p1";
        String rvName = "srv";
        String scriptFunc = "pyFunc";
        String limitsMin = "34";
        String limitsMax = "94";
        String logBefore = "x";
        String logAfter = "y";
        m_ktableUtils.setDataCell(0, 0, stubbedF1);
        m_ktableUtils.setTristateCheckBox(1, 0, isActive);
        m_ktableUtils.setTristateCheckBox(2, 0, isCustomActivation);
        m_ktableUtils.setDataRowContent(3, 0, stubParams, rvName, scriptFunc, limitsMin, limitsMax, logBefore, 
                                        logAfter);
        m_ktableUtils.addSeqColumn(11, 2);
        String stubExpect = "x == y";
        String stubAssignVar = "y";
        String stubAssignVal = "12";
        String stubScriptParams = "'f'";
        String stubNext = "5";
                
        m_ktableUtils.setDataRowContent(10, 0, stubExpect);
        m_ktableUtils.addMapColumnWValue(11, 0, stubAssignVar, stubAssignVal);
        m_ktableUtils.setDataRowContent(12, 0, stubScriptParams, stubNext);
        
        m_utils.selectTestSection(UITestUtils.SEC_USER_STUBS);
        m_ktableUtils.addSeqColumn(1, 0);
        String userStubbedF1 = "userStubbedFunc";
        String replFunc = "replF";
        m_ktableUtils.setDataCell(0, 0, userStubbedF1);
        m_ktableUtils.setTristateCheckBox(1, 0, isActive);
        m_ktableUtils.setDataCell(2, 0, replFunc);
        
        // test point
        m_utils.selectTestSection(UITestUtils.SEC_TEST_POINTS);
        m_ktableUtils.addSeqColumn(1, 0);
        String tpId = "tp_4";
        String tpCondCount = "3";
        String tpCondExpr = "cnt < 45";
        String tpScriptFunc = "tpScriptFunc";
        m_ktableUtils.setDataCell(0, 0, tpId);
        m_ktableUtils.setTristateCheckBox(1, 0, isActive);
        m_ktableUtils.setTristateCheckBox(2, 0, isCustomActivation);
        m_ktableUtils.setDataRowContent(3, 0, tpCondCount, tpCondExpr, tpScriptFunc);
        
        m_ktableUtils.selectDataItemInCombo(6, 0, "ff");  // resource type 'file'
        String tpLocResource = "src.cxx";
        m_ktableUtils.setDataCell(7, 0, tpLocResource);
        m_ktableUtils.selectDataItemInCombo(8, 0, "w"); // srcFileLocation
        String tpLine = "29";
        m_ktableUtils.setDataCell(9, 0, tpLine);
        ETristate tpIsSearch = ETristate.E_TRUE;
        m_ktableUtils.setTristateCheckBox(10, 0, tpIsSearch);
        String tpLinesRange = "123";
        m_ktableUtils.setDataCell(11, 0, tpLinesRange);
        m_ktableUtils.selectDataItemInCombo(12, 0, "c"); // search context 'code'
        m_ktableUtils.selectDataItemInCombo(13, 0, "r"); // match type 'regex'
        String tpSearchPattern = "c.*";
        String tpLineOffset = "4";
        String tpNumSteps = "99";
        
        m_ktableUtils.setDataRowContent(14, 0, tpSearchPattern, tpLineOffset, tpNumSteps, 
                                        logBefore, logAfter, limitsMin, limitsMax);

        m_ktableUtils.addSeqColumn(22, 2);
        // reuse stub data
        m_ktableUtils.setDataRowContent(21, 0, stubExpect);
        m_ktableUtils.addMapColumnWValue(22, 0, stubAssignVar, stubAssignVal);
        m_ktableUtils.setDataRowContent(23, 0, stubScriptParams, stubNext);
        
        
        // analyzer
        m_utils.selectTestSection(UITestUtils.SEC_ANALYZER);
        
        String analDocName = "anal.trd";
        m_ktableUtils.setDataCell(1, 0, analDocName);
        m_ktableUtils.selectDataItemInCombo(0, 0, "s"); // runMode 'start'
        m_ktableUtils.selectDataItemInCombo(2, 0, "a"); // openMode 'append'
        m_ktableUtils.setTristateCheckBox(3,  0, ETristate.E_TRUE);  // slow run
        String analTriggerName = "myTrigger";
        m_ktableUtils.setDataCell(4, 0, analTriggerName);
        m_ktableUtils.setTristateCheckBox(5,  0, ETristate.E_TRUE);  // predef. trigger
        m_ktableUtils.setTristateCheckBox(6,  0, ETristate.E_TRUE);  // save after test
        m_ktableUtils.setTristateCheckBox(7,  0, ETristate.E_TRUE);  // close after test

        // analyzer - coverage
        m_utils.selectSubSection(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_COVERAGE);
        m_ktableUtils.setTristateCheckBox(0,  0, ETristate.E_TRUE); // isActive
        m_ktableUtils.setTristateCheckBox(1,  0, ETristate.E_TRUE); // isMeasureAllFunctions
        m_ktableUtils.setTristateCheckBox(2,  0, ETristate.E_TRUE); // ignore non-reachable code
        m_ktableUtils.selectDataItemInCombo(3, 0, "s"); // mergeScope 'siblings only'
        String cvrgFilterName = "myCFilter";
        m_ktableUtils.setDataCell(4, 0, cvrgFilterName);
        m_ktableUtils.selectDataItemInCombo(5, 0, "s"); // filter type 'script'
        String cvrgFilePartitions = "dl.elf";
        m_ktableUtils.selectDataItemInCombo(7, 0, cvrgFilePartitions); // filter partition
        // other items are not tested, as code is the same for all of them
        
        // analyzer - coverage - statistics
        m_utils.selectSubSubSection(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_COVERAGE, UITestUtils.SEC_COVERAGE_STATS);
        m_ktableUtils.addSeqColumn(1, 2);
        String cvrgFuncName = "average";
        String cvrgCode = "98.45";
        String cvrgSrcLines = "45.6";
        String cvrgBranches = "34";
        String cvrgTaken = "55";
        String cvrgNotTaken = "0";
        String cvrgBoth = "100";
        String cvrgExecCount = "";
        m_ktableUtils.setDataRowContent(0, 0, cvrgFuncName, cvrgCode, cvrgSrcLines, cvrgBranches,
                                        cvrgTaken, cvrgNotTaken, cvrgBoth, cvrgExecCount);
        
        // analyzer profiler
        m_utils.selectSubSection(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_PROFILER);
        m_ktableUtils.setTristateCheckBox(0,  0, ETristate.E_TRUE); // isActive
        m_ktableUtils.setTristateCheckBox(1,  0, ETristate.E_TRUE); // isMeasureAllFunctions
        m_ktableUtils.selectDataItemInCombo(2, 0, "t"); // export fmt 'text'
        String profExportFile = "profExport.txt";
        m_ktableUtils.setDataCell(3, 0, profExportFile);
        m_ktableUtils.setTristateCheckBox(4,  0, ETristate.E_TRUE); // export active only
        m_ktableUtils.setTristateCheckBox(5,  0, ETristate.E_TRUE); // profile AUX
        m_ktableUtils.setTristateCheckBox(6,  0, ETristate.E_TRUE); // save history
        
        m_utils.selectSubSubSection(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_PROFILER, UITestUtils.SEC_PROFILER_CODE);
        m_ktableUtils.addSeqColumn(1, 2);
        String profFuncName = "fact";
        m_ktableUtils.setDataCell(0, 0, profFuncName);
        m_ktableUtils.addSeqColumn(2, 5);
        String profMinL = "23 us";
        String profMinH = "53 ms";
        m_ktableUtils.setDataCell(1, 0, profMinL);
        m_ktableUtils.addSeqColumn(2, 6);
        m_ktableUtils.setDataCell(2, 0, profMinH);

        m_utils.selectSubSubSection(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_PROFILER, UITestUtils.SEC_PROFILER_DATA);
        m_ktableUtils.addSeqColumn(1, 2);
        String profDataName = "mode";
        String profValue = "61";
        m_ktableUtils.setDataRowContent(0, 0, profDataName, profValue);
        m_ktableUtils.addSeqColumn(3, 5);
        String profMaxL = "123 ns";
        String profMaxH = "253 ns";
        m_ktableUtils.setDataCell(2, 0, profMaxL);
        m_ktableUtils.addSeqColumn(3, 6);
        m_ktableUtils.setDataCell(3, 0, profMaxH);

        
        // get test case
        m_utils.selectTestCase(testId, funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        
        
        // meta
        assertEquals(testId, testSpec.getTestId());
        assertEquals(desc, testSpec.getDescription());
        StrVector vec = new StrVector();
        testSpec.getTags(vec);
        assertEquals(StringUtils.join(tags, ", "), CYAMLUtil.strVector2Str(vec));
        
        // function
        assertEquals(timeout, Integer.toString(testSpec.getTestTimeout()));
        assertEquals(coreId, testSpec.getCoreId());
        m_utils.verifyFunc(testSpec.getFunctionUnderTest(true), 
                           funcName, StringUtils.join(params, ", "), retVal);
        
        // persist vars
        CTestPersistentVars persistVars = testSpec.getPersistentVars(true);
        CMapAdapter persistDecls = new CMapAdapter(persistVars, 
                                                   EPersistVarsSections.E_SECTION_DECL.swigValue(), 
                                                   true);
        assertEquals(1, persistDecls.size());
        assertEquals(persistVarName, persistDecls.getKey(0));
        assertEquals(persistVarValue, persistDecls.getValue(persistVarName));

        CSequenceAdapter persistDel = new CSequenceAdapter(persistVars, 
                                                           EPersistVarsSections.E_SECTION_DELETE.swigValue(),
                                                           true);
        assertEquals(1, persistDel.size());
        assertEquals(persistVarName + 'x', persistDel.getValue(0));
        
        assertEquals(true, persistVars.isDeleteAll());
        
        // locals and init
        StrStrMap localVars = new StrStrMap();
        testSpec.getLocalVariables(localVars);
        assertEquals(varsTypes[0], localVars.get(varsNames[0]));
        assertEquals(varsTypes[1], localVars.get(varsNames[1]));
        
        StrStrMap initMap = new StrStrMap();
        testSpec.getInitMap(initMap);
        assertEquals(varsVals[0], initMap.get(varsNames[0]));
        assertEquals(varsVals[1], initMap.get(varsNames[1]));
        
        // precond
        CTestAssert precondAssert = testSpec.getPrecondition(true);
        StrVector precondExprs = new StrVector();
        precondAssert.getExpressions(precondExprs);
        assertEquals(2, precondExprs.size());
        assertEquals(precond[0], precondExprs.get(0));
        assertEquals(precond[1], precondExprs.get(1));
        
        // expected
        CTestAssert expectAssert = testSpec.getAssert(true);
        StrVector expectExprs = new StrVector();
        expectAssert.getExpressions(expectExprs);
        assertEquals(2, expectExprs.size());
        assertEquals(exprs[0], expectExprs.get(0));
        assertEquals(exprs[1], expectExprs.get(1));
        
        // stub
        CTestStub stub = testSpec.getStub(stubbedF1);
        assertEquals(stubbedF1, stub.getFunctionName());
        assertEquals(true, stub.isActive());
        assertEquals(false, stub.isCustomActivation());
        StrVector stubParamsList = new StrVector();
        stub.getParamNames(stubParamsList);
        assertEquals(stubParams, stubParamsList.get(0));
        
        assertEquals(rvName, stub.getRetValName());
        assertEquals(scriptFunc, stub.getScriptFunctionName());
        assertEquals(limitsMin, Integer.toString(stub.getHitLimits(true).getMin()));
        assertEquals(limitsMax, Integer.toString(stub.getHitLimits(true).getMax()));
        CTestLog stubLogCfg = stub.getLogConfig(true);
        CSequenceAdapter stubLogBefore = stubLogCfg.getExpressions(ESectionsLog.E_SECTION_BEFORE, true);
        assertEquals(logBefore, stubLogBefore.getValue(0));
        CSequenceAdapter stubLogAfter = stubLogCfg.getExpressions(ESectionsLog.E_SECTION_AFTER, true);
        assertEquals(logAfter, stubLogAfter.getValue(0));
        
        CTestBaseList stubStepList = stub.getAssignmentSteps(true);
        CTestEvalAssignStep step = CTestEvalAssignStep.cast(stubStepList.get(0));
        CSequenceAdapter stubExpectedSA = step.getExpectedExpressions(false);
        assertEquals(stubExpect, stubExpectedSA.getValue(0));
        
        CMapAdapter stubAssignMA = step.getAssignments(false);
        assertEquals(stubAssignVal, stubAssignMA.getValue(stubAssignVar));
        
        CSequenceAdapter stubScriptParamsSA = step.getScriptParams(false);
        assertEquals(stubScriptParams, stubScriptParamsSA.getValue(0));
        assertEquals(stubNext, step.getStepIdx());
        
        // user stub
        CTestUserStub userStub = testSpec.getUserStub(userStubbedF1);
        assertEquals(userStubbedF1, userStub.getFunctionName());
        assertEquals(isActive, userStub.isActive());
        assertEquals(replFunc, userStub.getReplacementFuncName());
        
        // test point
        CTestPoint tp = testSpec.getTestPoint(tpId);
        assertEquals(tpId, tp.getId());
        assertEquals(ETristate.E_TRUE, tp.isActive());
        assertEquals(false, tp.isCustomActivation());
        assertEquals(tpCondCount, Integer.toString(tp.getConditionCount()));
        assertEquals(tpCondExpr, tp.getConditionExpr());
        assertEquals(tpScriptFunc, tp.getScriptFunctionName());
        CTestLocation tpLoc = tp.getLocation(true);
        assertEquals(EResourceType.E_RESOURCE_FILE, tpLoc.getResourceType());
        assertEquals(tpLocResource, tpLoc.getResourceName());
        assertEquals(EFileLocation.EWinIDEAHost, tpLoc.getSrcFileLocation());
        assertEquals(tpLine, Integer.toString(tpLoc.getLine()));
        assertEquals(tpIsSearch, tpLoc.isSearch());
        assertEquals(tpLinesRange, Integer.toString(tpLoc.getLinesRange()));
        assertEquals(ESearchContext.E_SEARCH_CODE, tpLoc.getSearchContext());
        assertEquals(EMatchingType.E_MATCH_REG_EX, tpLoc.getMatchingType());
        
        assertEquals(tpSearchPattern, tpLoc.getSearchPattern());
        assertEquals(tpLineOffset, Integer.toString(tpLoc.getLineOffset()));
        assertEquals(tpNumSteps, Integer.toString(tpLoc.getNumSteps()));
  
        CTestLog tpLogConfig = tp.getLogConfig(false);
        CSequenceAdapter tpLogBefore = tpLogConfig.getExpressions(ESectionsLog.E_SECTION_BEFORE, false);
        assertEquals(logBefore, tpLogBefore.getValue(0));
        CSequenceAdapter tpLogAfter = tpLogConfig.getExpressions(ESectionsLog.E_SECTION_AFTER, false);
        assertEquals(logAfter, tpLogAfter.getValue(0));
        assertEquals(limitsMin, Integer.toString(tp.getHitLimits(false).getMin())); 
        assertEquals(limitsMax, Integer.toString(tp.getHitLimits(false).getMax()));

        CTestBaseList stepsList = stub.getAssignmentSteps(true);
        step = CTestEvalAssignStep.cast(stepsList.get(0));
        stubExpectedSA = step.getExpectedExpressions(false);
        assertEquals(stubExpect, stubExpectedSA.getValue(0));
        
        stubAssignMA = step.getAssignments(false);
        assertEquals(stubAssignVal, stubAssignMA.getValue(stubAssignVar));
        
        stubScriptParamsSA = step.getScriptParams(false);
        assertEquals(stubScriptParams, stubScriptParamsSA.getValue(0));
        assertEquals(stubNext, step.getStepIdx());
        
        // analyzer
        CTestAnalyzer anal = testSpec.getAnalyzer(true);
        assertEquals(ERunMode.M_START, anal.getRunMode());
        assertEquals(analDocName, anal.getDocumentFileName());
        assertEquals(EOpenMode.EAppend, anal.getOpenMode());
        assertEquals(ETristate.E_TRUE, anal.isSlowRun());
        assertEquals(analTriggerName, anal.getTriggerName());
        assertEquals(ETristate.E_TRUE, anal.isPredefinedTrigger());
        assertEquals(ETristate.E_TRUE, anal.isSaveAfterTest());
        assertEquals(ETristate.E_TRUE, anal.isCloseAfterTest());
        
        // analyzer - coverage
        CTestAnalyzerCoverage cvrg = anal.getCoverage(true);
        assertEquals(ETristate.E_TRUE, cvrg.isActive());
        assertEquals(ETristate.E_TRUE, cvrg.isMeasureAllFunctions());
        assertEquals(ETristate.E_TRUE, cvrg.isIgnoreNonReachableCode());
        assertEquals(EMergeScope.ESiblingsOnly, cvrg.getMergeScope());
        CTestFilter acFilter = cvrg.getMergeFilter(true);
        assertEquals(cvrgFilterName, acFilter.getFilterId());
        assertEquals(EFilterTypes.SCRIPT_FILTER, acFilter.getFilterType());
        StrVector partitions = new StrVector();
        acFilter.getPartitions(partitions);
        assertEquals(1, partitions.size());
        assertEquals(cvrgFilePartitions, partitions.get(0));
        
        // analyzer - coverage - stats
        CTestCoverageStatistics cStats = cvrg.getStatistics(0);
        assertEquals(cvrgFuncName, cStats.getFunctionName());
        assertEquals(cvrgCode, cStats.getBytesExecutedText());
        assertEquals(cvrgSrcLines, cStats.getSourceLinesExecutedText());
        assertEquals(cvrgBranches, cStats.getBranchExecutedText());
        assertEquals(cvrgTaken, cStats.getBranchTakenText());
        assertEquals(cvrgNotTaken, cStats.getBranchNotTakenText());
        assertEquals(cvrgBoth, cStats.getBranchBothText());
        assertEquals(0, cStats.getExecutionCount());

        // analyzer - profiler
        CTestAnalyzerProfiler prof = anal.getProfiler(true);
        assertEquals(ETristate.E_TRUE, prof.isActive());
        assertEquals(ETristate.E_TRUE, prof.isMeasureAllFunctions());
        assertEquals(EProfilerExportFormat.EProfilerAsText, prof.getExportFormat());
        assertEquals(profExportFile, prof.getExportFileName());        
        assertEquals(ETristate.E_TRUE, prof.isExportActiveAreasOnly());
        assertEquals(ETristate.E_TRUE, prof.isProfileAUX());
        assertEquals(ETristate.E_TRUE, prof.isSaveHistory());
        
        // analyzer - profiler - code
        CTestProfilerStatistics pCode = CTestProfilerStatistics.cast(prof.getCodeAreas(true).get(0));
        assertEquals(profFuncName, pCode.getAreaName());
        CTestProfilerTime pTime = pCode.getTime(EProfilerStatisticsSectionId.E_SECTION_NET_TIME, true);
        assertEquals(profMinL, pTime.getTime(EProfilerTimeSectionId.E_SECTION_MIN_TIME, 0));
        assertEquals(profMinH, pTime.getTime(EProfilerTimeSectionId.E_SECTION_MIN_TIME, 1));
        
        // analyzer - profiler - data
        CTestProfilerStatistics pData = CTestProfilerStatistics.cast(prof.getDataAreas(true).get(0));
        assertEquals(profDataName, pData.getAreaName());
        assertEquals(profValue, pData.getAreaValue());
        pTime = pData.getTime(EProfilerStatisticsSectionId.E_SECTION_NET_TIME, true);
        assertEquals(profMaxL, pTime.getTime(EProfilerTimeSectionId.E_SECTION_MIN_TIME, 0));
        assertEquals(profMaxH, pTime.getTime(EProfilerTimeSectionId.E_SECTION_MIN_TIME, 1));
    }

    
    @Test
    // select all sections, enter all the data. Simplified, because the same code is used in 
    // all cases, while on the other hand this mode is not very user friendly as the table
    // is too wide.
    public void dataToAllSection() throws Exception {

        m_utils.discardExistingAndCreateNewProject();
        String funcName = "add_int";
        m_utils.createNewBaseUnitTest(funcName, "11, 22", "rv == 33", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_ktableUtils.selectKTable(0);

        m_utils.clickToolbarButton("Select / deselect all sections.");
        String testId = "t23";
        String desc = "testing test case";
        String tag = "mode3";
        String optName1 = "/iopen/hw";
        String optVal1 = "itag";
        String optName2 = "/iopen/hw/ic";
        String optVal2 = "ic5000";
        m_ktableUtils.setDataCell(0, 0, testId);
        m_ktableUtils.setDataCell(3, 0, desc);
        m_ktableUtils.setDataCell(4, 0, tag);
        m_ktableUtils.addMapColumn(6, 0, optName1);
        m_ktableUtils.setDataCell(5, 0, optVal1);
        m_ktableUtils.addUserMappingItem(6, 1, 0, optName2, optVal2);

        // get test case
        m_utils.selectTestCase(testId, funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals(testId, testSpec.getTestId());
        assertEquals(desc, testSpec.getDescription());
        StrVector tags = new StrVector();
        testSpec.getTags(tags);
        assertEquals(tag, tags.get(0));
        CMapAdapter optsMA = new CMapAdapter(testSpec, SectionIds.E_SECTION_OPTIONS.swigValue(), true);
        assertEquals(optVal1, optsMA.getValue(optName1));
        assertEquals(optVal2, optsMA.getValue(optName2));
    }

    
    // select Function section, enter 3 params, then interpolate params in section func
    // extrapolate params in section Func
    @Test
    public void interpolation() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        String funcName = "add_int";
        m_utils.createNewBaseUnitTest(funcName, "11, 22", "rv == 33", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_ktableUtils.selectKTable(0);
        
        m_ktableUtils.addRows(5);
        
        // toggle inheritance
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        for (int i = 1; i < 6; i++) {
            m_ktableUtils.selectDataCell(1, i);
            m_utils.clickToolbarButton(TOGGLE_INHERITANCE_BTN_TOOLTIP_REGEX);
        }
        
        // params of base test
        int iStart0 = 12;
        int iStart1 = 100;
        m_ktableUtils.setDataCell(1, 0, Integer.toString(iStart0));
        m_ktableUtils.setDataCell(2, 0, Integer.toString(iStart1));
        
        // param of first derived for extrapolation
        int iSecond = 15;
        m_ktableUtils.setDataCell(1,  1, Integer.toString(iSecond));
        
        // param of last derived for interpolation
        int iEnd1 = 150;
        m_ktableUtils.setDataCell(2,  5, Integer.toString(iEnd1));

        // extrapolate
        m_ktableUtils.selectDataCells(1, 0, 2, 6);
        m_utils.clickToolbarButton("Extrapolate first two.*");

        // interpolate
        m_ktableUtils.selectDataCells(2, 0, 3, 6);
        m_utils.clickToolbarButton("Interpolate between first.*");

        // test comments
        String nlComment1 = "important function";
        String eolComment1 = "many tests";
        m_ktableUtils.setDataCellComment(0, 0, nlComment1, eolComment1);
        String nlComment2 = "next test";
        String eolComment2 = "some value";
        m_ktableUtils.setDataCellComment(1, 1, nlComment2, eolComment2);
        
        // get test case
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        StrVector params = new StrVector();
        testSpec.getPositionParams(params);
        assertEquals(2, params.size());
        assertEquals(Integer.toString(iStart0), params.get(0));
        assertEquals(Integer.toString(iStart1), params.get(1));
        
        checkCommentOnFuncName(nlComment1, eolComment1, testSpec);
        checkCommentOnParam(nlComment2, eolComment2, testSpec.getDerivedTestSpec(0));
        
        int noOfDerived = testSpec.getNoOfDerivedSpecs();
        for (int idx = 0; idx < noOfDerived; idx++) {
            CTestSpecification derived = testSpec.getDerivedTestSpec(idx);
            derived.getPositionParams(params);
            assertEquals(iStart0 + (iSecond - iStart0) * (idx + 1), Integer.parseInt(params.get(0)));
            assertEquals(iStart1 + (iEnd1 - iStart1) / 5 * (idx + 1), Integer.parseInt(params.get(1)));
        }
    }


    private void checkCommentOnFuncName(String nlComment1, String eolComment1, CTestSpecification testSpec) {
        CTestFunction func = testSpec.getFunctionUnderTest(true);
        String comment = func.getComment(CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue(), 
                                         SpecDataType.KEY, CommentType.NEW_LINE_COMMENT);
        assertEquals("# " + nlComment1, comment.trim());
        comment = func.getComment(CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue(), 
                                  SpecDataType.VALUE, CommentType.END_OF_LINE_COMMENT);
        assertEquals("# " + eolComment1, comment.trim());
    }


    private void checkCommentOnParam(String nlComment, String eolComment, CTestSpecification testSpec) {
        CTestFunction func = testSpec.getFunctionUnderTest(true);
        String comment = func.getCommentForSeqElement(ESection.E_SECTION_PARAMS.swigValue(), 
                                                      0, 
                                                      CommentType.NEW_LINE_COMMENT);
        assertEquals("# " + nlComment, comment.trim());
        
        comment = func.getCommentForSeqElement(ESection.E_SECTION_PARAMS.swigValue(), 
                                               0, 
                                               CommentType.END_OF_LINE_COMMENT);
        assertEquals("# " + eolComment, comment.trim());        
    }


    @Test
    public void runScript() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        String funcName = "add_int";
        m_utils.createNewBaseUnitTest(funcName, "11, 22", "rv == 33", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_ktableUtils.selectKTable(0);
        
        m_ktableUtils.addRows(5);
        
        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        int firstVal = 3;
        int secondVal = 6;
        String varName = "g_char1";
        m_ktableUtils.addMapColumnWValue(1, 0, varName, Integer.toString(firstVal));
        
        m_ktableUtils.setDataCell(1, 1, Integer.toString(secondVal));

        m_ktableUtils.selectDataCells(1, 0, 2, 6);
        
        m_utils.connect();
        m_utils.setPropertiesScriptConfig("sampleTestExtensions");
        m_utils.refreshSymbols();
        
        SWTBotMenu menuItem = m_bot.toolbarDropDownButtonWithTooltip(SWTBotConstants.SCRIPT_BTN_TOOLTIP)
                                   .menuItem("isys_table_geometric()");
        menuItem.click();
        m_utils.pressKey(SWT.ESC); // make menu opened above disappear, seems bug in
                                   // SWTBot, see:
                                   // https://www.eclipse.org/forums/index.php/t/159133/

        // get test case
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        StrStrMap initMap = new StrStrMap();
        testSpec.getInitMap(initMap);
        
        assertEquals(1, initMap.size());
        assertEquals(Integer.toString(firstVal), initMap.get(varName));
        
        int noOfDerived = testSpec.getNoOfDerivedSpecs();
        int k = secondVal / firstVal;
        int prevVal = firstVal;
        
        for (int idx = 0; idx < noOfDerived; idx++) {
            CTestSpecification derived = testSpec.getDerivedTestSpec(idx);
            derived.getInitMap(initMap);
            assertEquals(prevVal * k, Float.parseFloat(initMap.get(varName)), 1e-6);
            prevVal *= k;
        }
    }
    
    
    @Test
    public void derivedEmptyList() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        
        // Base test wo stubs, create 2 derived, create stub in D2
        // Edit stub in Base, delete it, Remove empty sections.
        
        String funcName = "min_int";
        m_utils.createNewBaseUnitTest(funcName, "11, 22", "rv == 11", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_ktableUtils.selectKTable(0);
        
        m_ktableUtils.addRows(2); // add two derived test cases
        String derivedId1 = "d1";
        m_ktableUtils.setDataCell(0, 1, derivedId1);
        String derivedId2 = "d2";
        m_ktableUtils.setDataCell(0, 2, derivedId2);
        
        m_utils.selectFormEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_utils.selectDerivedTestCase("/ : " + funcName, derivedId2 + " : /");
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        m_utils.setSectionStub("stubbedFuncName", "Yes", "", "retValName", "", "", "", false);

        checkNumStubs(funcName, 0, 0, 1);
        
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);

        // The base test case has no stubs yet. By entering a stubbed function name, 
        // one will be added.
        m_ktableUtils.setDataCell(0, 0, "baseStub");

        checkNumStubs(funcName, 1, 0, 1);

        // clear stub in base
        m_ktableUtils.clickDataCell(0, 0);  // move focus from Overview to table
        m_ktableUtils.clickDataCell(0, 0);  // SWTBot requires to click twice ???
        m_utils.pressKey(SWT.DEL);
        m_bot.menu("Test").menu("Remove Empty Sections").click();
        
        checkNumStubs(funcName, 0, 0, 1);
        
       
        // remove column in sequence when some test case has inherited section
        m_ktableUtils.setDataCell(0, 0, "baseStub");
        m_ktableUtils.selectDataCell(0, 1);
        m_utils.clickToolbarButton(TOGGLE_INHERITANCE_BTN_TOOLTIP_REGEX);
        String derivedStubName = "derivedStub";
        m_ktableUtils.setDataCell(0, 1, derivedStubName);
        m_utils.selectTestCase("/", funcName); // table was changed to derived test case
        
        // set stub params
        int colParam1 = 3;
        int colParam2 = colParam1 + 1;
        m_ktableUtils.setDataCell(colParam1, 0, "a");
        m_ktableUtils.setDataCell(colParam1, 0, "a"); // repeat entry, otherwise it is lost - SWTBot bug
        m_ktableUtils.setDataCell(colParam1, 1, "b");
        m_ktableUtils.setDataCell(colParam1, 2, "c");
        m_ktableUtils.addSeqColumn(4, 3);
        m_ktableUtils.setDataCell(colParam2, 0, "aa");
        m_ktableUtils.setDataCell(colParam2, 1, "bb");
        m_ktableUtils.setDataCell(colParam2, 2, "cc");
        m_ktableUtils.selectDataCell(colParam2, 1);
        m_utils.pressKey(SWT.DEL); // one test has less params then other two - cell is grey
        
        StrVector stubParams = getStubParams(funcName, derivedStubName);
        assertEquals(1, stubParams.size());
        assertEquals("b", stubParams.get(0));

        m_ktableUtils.clickCell(5, 3);  // make sure the table has focus
        m_ktableUtils.deleteColumnWIcon(5, 3);
        m_ktableUtils.deleteColumnWIcon(4, 3);

        // we should get here without exception
        stubParams = getStubParams(funcName, derivedStubName);
        assertEquals(0, stubParams.size()); // all params were cleared
        
        // test undo/redo
        m_utils.undo();
        m_utils.undo();
        m_utils.undo();
        stubParams = getStubParams(funcName, derivedStubName);
        assertEquals(2, stubParams.size()); // all params were restored

        // undo until only the base test case remains
        for (int i = 0; i < 22; i++) {
            m_utils.undo();
        }
        
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals(0, testSpec.getNoOfDerivedSpecs());

        for (int i = 0; i < 22; i++) {
            m_utils.redo();
        }

        stubParams = getStubParams(funcName, derivedStubName);
        assertEquals(2, stubParams.size()); // all params were restored
    }


    private StrVector getStubParams(String funcName, String derivedStubName) throws Exception {
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        StrVector stubParams = new StrVector();
        testSpec.getDerivedTestSpec(0).getStub(derivedStubName).getParamNames(stubParams);
        return stubParams;
    }


    private void checkNumStubs(String funcName, 
                               int noOfStubsInBase, 
                               int noOfStubsInDerived0, 
                               int noOfStubsInDerived1) throws Exception {
        // get test case
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        int numDerived = testSpec.getNoOfDerivedSpecs();
        assertEquals(2, numDerived);
        assertEquals(noOfStubsInBase, testSpec.getStubs(true).size());
        
        CTestSpecification derived_0 = testSpec.getDerivedTestSpec(0);
        assertEquals(noOfStubsInDerived0, derived_0.getStubs(true).size());
        
        CTestSpecification derived_1 = testSpec.getDerivedTestSpec(1);
        assertEquals(noOfStubsInDerived1, derived_1.getStubs(true).size());
    }
    
    
    @Test
    public void showSeveralSelectedTests() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        
        // check IDs of two unrelated test cases
        
        String funcName1 = "min_int";
        m_utils.createNewBaseUnitTest(funcName1, "11, 22", "rv == 11", "rv", false);
        String funcName2 = "add_int";
        m_utils.createNewBaseUnitTest(funcName2, "3, 4", "rv == 7", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);

        m_utils.selectAll();
        m_ktableUtils.selectKTable(0);

        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        assertEquals(funcName1, m_ktableUtils.getDataCellAsString(0, 0));
        assertEquals(funcName2, m_ktableUtils.getDataCellAsString(0, 1));
    }
    
    
    @Test
    public void groups() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        
        String grpId = "g1";
        m_utils.createNewGroupWithContextCmd(grpId, "t1");
        String grpId2 = "g2";
        m_utils.createNewSubGroupWithContextCmd(grpId, "(0) ///", grpId2, "");
        
        m_utils.selectTestGroup(grpId, "(0) ///");
        
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        m_ktableUtils.selectKTable(0);
        
        m_utils.selectTestSection(UITestUtils.SEC_GRP_FILTER);
        
        String filterId0 = "filter0";
        m_ktableUtils.setDataCell(0, 0, filterId0);
        String filterId1 = "filter1";
        m_ktableUtils.setDataCell(0, 1, filterId1);
        
        m_utils.selectTestGroup(grpId, "(0) ///");
        m_utils.copyToClipboard();
        CTestGroup grpContainer = m_utils.getTestGroupFromClipboard();
        CTestBaseList children = grpContainer.getChildren(true);
        CTestGroup grp = CTestGroup.cast(children.get(0));
        assertEquals(filterId0, grp.getFilter(true).getFilterId());
        children = grp.getChildren(true);
        CTestGroup subGrp = CTestGroup.cast(children.get(0));
        assertEquals(filterId1, subGrp.getFilter(true).getFilterId());
    }

    
    @Test
    public void testResultIcons() throws Exception {
        // verify result icons in table
        
        m_utils.discardExistingAndCreateNewProject();
        
        // check IDs of two unrelated test cases
        
        String funcName1 = "min_int";
        m_utils.createNewBaseUnitTest(funcName1, "11, 22", "rv == 11", "rv", false);
        String funcName2 = "add_int";
        m_utils.createNewBaseUnitTest(funcName2, "3, 4", "rv == 8", "rv", false);
        m_utils.selectTableEditor(UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);

        m_ktableUtils.selectKTable(0);
        
        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        m_utils.selectTestCase("/", funcName1); // Outline view should have focus
        m_utils.selectAll();
        
        m_utils.runAllTests("Test report for selected editor, 2 test(s), 0 group(s):\n"
                            + "- 1 test (50%) completed successfully\n"
                            + "- 1 test (50%) failed (invalid results)");
        
        // unfortunately there is no easy way to check icon image. It contains
        // image file (URL) name in private member, so reflection could be used
        // if required.
        // We only test for presence of result icons.
        TextIconsContent tableCell = (TextIconsContent)m_ktableUtils.getDataCellAt(1, 0);
        assertEquals("rv == 11\n    rv = 0x0000000B (11)", tableCell.getTooltip(EIconPos.EBottomLeft).trim());
        Image okIcon = tableCell.getIcon(EIconPos.EBottomLeft, true);
        assertTrue("Icons should contain result!", okIcon != null);
        
        tableCell = (TextIconsContent)m_ktableUtils.getDataCellAt(1, 1);
        assertEquals("rv == 8\n    rv = 0x00000007 (7)", tableCell.getTooltip(EIconPos.EBottomLeft).trim());
        Image failIcon = tableCell.getIcon(EIconPos.EBottomLeft, true);
        assertTrue("Icons should contain result!", failIcon != null);
    }
}
