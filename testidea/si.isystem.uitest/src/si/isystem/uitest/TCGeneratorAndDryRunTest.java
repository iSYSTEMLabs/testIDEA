package si.isystem.uitest;

import static org.junit.Assert.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestDryRun;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestStub;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrVector;
import si.isystem.swtbot.utils.KTableTestUtils;

/**
 * This class test Test Case Generator and dry run functionality.
 * @author markok
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class TCGeneratorAndDryRunTest {

    private static final String OPTION_URL = "/iOpen/option";
    private static final String HIL_OUT = "/HIL/dout1";
    private static final String STUBBED_FUNC = "stubbedFunc";
    private static final String TP_ID = "tpId";
    private static final int OCCURR_1_RADIO_IDX = 0;
    private static final int OCCURR_2_RADIO_IDX = 1;
    // private static final int OCCURR_3_RADIO_IDX = 2;
    private static final int OCCURR_N_TIMES_RADIO_IDX = 3;
    private static final int OCCURR_MAX_RADIO_IDX = 4;
    private static final int OCCURR_CUSTOM_RADIO_IDX = 5;

    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;
    private static KTableTestUtils m_ktableUtils;


    @BeforeClass
    public static void setup() {
        
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
    public void testAllPages() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();

        m_utils.createNewBaseUnitTest("add_int", "3, 7", "rv == 10", "rv", false);
        m_utils.setSectionStub(STUBBED_FUNC, "Yes", "", "srv", "", "", "", false);
        m_utils.setSectionTestPoint(TP_ID, "Yes", 0, "", "", false);
        
        String parentTestID = "/";
        String functionName = "add_int";
        m_utils.selectTestCase(parentTestID, functionName);
        
        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TOOLS, 
                                       UITestUtils.MENU_TOOLS__GEN_TEST_CASES, 
                                       UITestUtils.DIALOG_TC_GENERATOR_WIZARD);

        // clear any data persisting from previous tests
        m_utils.clickButton("Clear");
        m_utils.waitForShellAndClickOK("Clear wizard data", 2000, true);
        
        setFunctionParamsPage();
        m_utils.clickButton("Next >");

        setVarsPage();
        m_utils.clickButton("Next >");
        
        setStubsPage();
        m_utils.clickButton("Next >");
        
        setTestPointsPage();
        m_utils.clickButton("Next >");
        
        setHilPage();
        m_utils.clickButton("Next >");

        setOptionsPage();
        m_utils.clickButton("Next >");

        setScriptPage("sf1", "q, w");
        m_utils.clickButton("Next >");
        setScriptPage("sf1", "e, r");
        m_utils.clickButton("Next >");
        setScriptPage("sf1", "t, y");
        m_utils.clickButton("Next >");
        setScriptPage("sf1", "u, i");
        m_utils.clickButton("Next >");
        
        setTestCasePage();
        m_utils.clickButton("Next >");

        setExpectedPage();
        m_utils.clickButton("Next >");
        
        setDryRunPage();
        m_utils.clickButton("Finish");
        
        m_utils.selectTestCase(parentTestID, functionName);
        m_utils.copyToClipboard();
        CTestSpecification baseTestSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals(18, baseTestSpec.getNoOfDerivedSpecs());
        
        // test contents of the first, middle and the last derived test case
        CTestSpecification first = baseTestSpec.getDerivedTestSpec(0);
        
        checkTestSpec(first, 
                      "1", "10", 
                      "4", "10", 
                      "-1", "a", 
                      "x", "q", 
                      "4", "-10", 
                      "q", "q", "e", "e", "t", "t", "u", "u");
        checkExpected(first, "rv == 12", "g_char1 < 12",
                      "iCounter < 0", "g_Array[2] == 1", "g_int1 == 121");
        checkDryRun(first, "retVal", "g_var");
        
        CTestSpecification middle = baseTestSpec.getDerivedTestSpec(9);
        checkTestSpec(middle, 
                      "3", "14", 
                      "6", "10", 
                      "-2", "c", 
                      "z", "e", 
                      "5", "-8", 
                      "q", "q", "e", "e", "t", "t", "u", "i");
        checkExpected(middle, "rv == 12", "g_char1 < 12",
                      "iCounter < 0", "g_Array[2] == 1", "g_int1 == 121");
        checkDryRun(middle, "retVal", "g_var");

        CTestSpecification last = baseTestSpec.getDerivedTestSpec(17);
        checkTestSpec(last, 
                      "3", "10", 
                      "102", "10", 
                      "-1", "c", 
                      "x", "q", 
                      "6", "-6", 
                      "w", "q", "r", "e", "y", "t", "i", "u");
        checkExpected(last, "rv == 12", "g_char1 < 12",
                      "iCounter < 0", "g_Array[2] == 1", "g_int1 == 121");
        checkDryRun(last, "retVal", "g_var");
    }
    

    @Test
    public void testOptimization() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();

        String parentTestId = "/";
        String functionName = "complexFunction";
        m_utils.createNewBaseUnitTest(functionName, "0, 0, 0", "rv == ${retVal}", "rv", false);
        
        m_utils.setSectionAnalyzer("Start", "optimization.trd", "Write", 
                                   "No", "No", "No", "No", "", false);
        
        m_utils.setSectionCoverageTestCase("Yes", "", "", "",
                                   "No",
                                   null, null, 
                                   null, null, 
                                   null, null, 
                                   null, null, null);
        
        m_utils.setSectionCoverageStatistics(functionName, 0, 0, 0, 0, 0, 0);
        
        m_utils.setSectionDryRun(1, "${retVal}", "rv,d");
        m_utils.setSectionDryRunAnalyzer(ETristate.E_TRUE, ETristate.E_DEFAULT, 
                                         "", "");
        
        m_utils.selectTestCase(parentTestId, functionName);
        
        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TOOLS, 
                                       UITestUtils.MENU_TOOLS__GEN_TEST_CASES, 
                                       UITestUtils.DIALOG_TC_GENERATOR_WIZARD);
        // clear any data persisting from previous tests
        m_utils.clickButton("Clear");
        m_utils.waitForShellAndClickOK("Clear wizard data", 2000, true);
        m_utils.selectCheckBoxWText("Show all pages", false);

        // setFunctionParamsPage
        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_MAX_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        
        m_ktableUtils.clickCell(0,  1);
        setIdentifier(1, "a", "-1, 0, 2", "", "", "");
        setIdentifier(2, "b", "-5 ,0, 5", "", "", "");
        setIdentifier(3, "c", "-4, 0, 4, 5", "", "", "");
        
        m_utils.clickButton("Next >");
        m_utils.clickButton("Next >");
        m_utils.clickButton("Next >");
        m_utils.clickButton("Next >");
        
        m_utils.selectCheckBoxWText("Copy section 'Coverage' from base test case", true);
        m_utils.selectCheckBoxWText("Set open mode to 'Append'", true);

        m_utils.clickButton("Finish");

        m_utils.selectTestCase(parentTestId, functionName);
        m_utils.copyToClipboard();
        CTestSpecification baseTestSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals(36, baseTestSpec.getNoOfDerivedSpecs());
        
        m_utils.toolbarToggleDryRun();
        m_bot.sleep(200);
        m_utils.toolbarRunSelectedAndDerived();
        m_utils.waitForShellAndClickOK("Dry Run Mode", 3000, true);
        m_bot.sleep(200);
        m_utils.waitForProgressDialog();
        // check status view
        m_utils.checkStatusView(0, "Test report for selected editor, 37 test(s), 0 group(s):");
        
        m_utils.toolbarToggleDryRun();
        m_utils.toolbarRunSelectedAndDerived();
        m_utils.waitForProgressDialog();

        // All tests should be OK 
        m_utils.checkStatusView(0, "All tests for selected editor completed successfully!\n"
                                + "Number of tests: 37");
        
        // optimize
        m_utils.selectTestCase(parentTestId, functionName);
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__OPTIMIZE_TEST_VECTORS).click();
        
        // TODO delete
        m_utils.pressKey(SWT.DEL);
        
        m_utils.selectTestCase(parentTestId, functionName);
        m_utils.copyToClipboard();
        baseTestSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals(4, baseTestSpec.getNoOfDerivedSpecs());
    }
    
    
    private void checkTestSpec(CTestSpecification testSpec,
                               String fp1, String fp2, 
                               String value1, String value2,
                               String stubVal1, String stubVal2,
                               String tpVal1, String tpVal2,
                               String hilVal, String optVal,
                               String initTfp1, String initTfp2,
                               String initFp1, String initFp2,
                               String endFp1, String endFp2,
                               String restoreTFp1, String restoreTFp2)
    {
        checkFuncParams(testSpec.getFunctionUnderTest(true), fp1, fp2);
        
        StrStrMap initMap = new StrStrMap();
        testSpec.getInitMap(initMap);
        assertEquals(value1, initMap.get("var1"));
        assertEquals(value2, initMap.get("var2"));
        
        CTestStub stub = testSpec.getStub(STUBBED_FUNC);
        CTestBaseList steps = stub.getAssignmentSteps(true);
        CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(0));
        CMapAdapter assignments = step.getAssignments(true);
        assertEquals(stubVal1, assignments.getValue("srv1"));
        assertEquals(stubVal2, assignments.getValue("p1"));
        
        CTestPoint tp = testSpec.getTestPoint(TP_ID);
        steps = tp.getSteps(true);
        step = CTestEvalAssignStep.cast(steps.get(0));
        assignments = step.getAssignments(true);
        assertEquals(tpVal1, assignments.getValue("g_char1"));
        assertEquals(tpVal2, assignments.getValue("g_char2"));
        
        CTestHIL hil = testSpec.getHIL(true);
        initMap.clear();
        hil.getHILParamMap(initMap);
        assertEquals(hilVal, initMap.get(HIL_OUT));

        StrVector strVector = new StrVector();
        testSpec.getOptionKeys(strVector);
        assertEquals(OPTION_URL, strVector.get(0));
        testSpec.getOptionValues(strVector);
        assertEquals(optVal, strVector.get(0));
        
        CTestFunction tFunc = testSpec.getInitTargetFunction(true);
        checkFuncParams(tFunc, initTfp1, initTfp2);

        tFunc = testSpec.getInitFunction(true);
        checkFuncParams(tFunc, initFp1, initFp2);
        
        tFunc = testSpec.getEndFunction(true);
        checkFuncParams(tFunc, endFp1, endFp2);
        
        tFunc = testSpec.getRestoreTargetFunction(true);
        checkFuncParams(tFunc, restoreTFp1, restoreTFp2);
    }


    private void checkExpected(CTestSpecification testSpec,
                               String expectExpr1, String expectExpr2,
                               String precond,
                               String stubExp,
                               String tpExp) {
        StrVector expectedResults = new StrVector();
        testSpec.getExpectedResults(expectedResults);
        assertEquals(expectExpr1, expectedResults.get(0));
        assertEquals(expectExpr2, expectedResults.get(1));
        
        CTestAssert testAssert = testSpec.getPrecondition(true);
        testAssert.getExpressions(expectedResults);
        assertEquals(precond, expectedResults.get(0));
        
        CTestStub stub = testSpec.getStub(STUBBED_FUNC);
        CTestBaseList steps = stub.getAssignmentSteps(true);
        CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(0));
        CSequenceAdapter exprs = step.getExpectedExpressions(true);
        assertEquals(stubExp, exprs.getValue(0));
        
        CTestPoint tp = testSpec.getTestPoint(TP_ID);
        steps = tp.getSteps(true);
        step = CTestEvalAssignStep.cast(steps.get(0));
        exprs = step.getExpectedExpressions(true);
        assertEquals(tpExp, exprs.getValue(0));
    }


    private void checkDryRun(CTestSpecification testSpec,
                             String val1, String val2) {
        
        CTestDryRun dryRun = testSpec.getDryRun(true);
        CMapAdapter assignments = dryRun.getAssignments(true);
        assertEquals(val1, assignments.getValue("${rv}"));
        assertEquals(val2, assignments.getValue("${hostVar}"));
    }


    private void checkFuncParams(CTestFunction func, String ... values) {
        StrVector positionParams = new StrVector();
        func.getPositionParams(positionParams);
        
        int idx = 0;
        for (String val : values) {
            assertEquals(val, positionParams.get(idx));
            idx++;
        }
    }


    private void setFunctionParamsPage() {

        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_2_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        m_utils.selectCheckBoxWText("Show all pages", true);
        
        m_ktableUtils.selectKTable(0);
        
        m_ktableUtils.clickCell(0,  1);
        setIdentifier(1, "a", "1, 2, 3", "", "", "");
        setIdentifier(2, "b", "", "10", "15", "2");
    }
    
    
    private void setVarsPage() {

        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_N_TIMES_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        
        SWTBotText numOccurrs = m_bot.text(0);
        numOccurrs.setFocus();
        numOccurrs.setText("2");
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(0,  1);
        
        setIdentifier(1, "var1", "4, 5, 6", "100", "103", "");
        setIdentifier(2, "var2", "", "10", "15", "3");
    }
    
    
    private void setStubsPage() {

        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_MAX_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(0,  1);
        
        setIdentifier(1, "srv1", "-1, -2, -3", "", "", "");
        setIdentifier(2, "p1", "a, b, c", "", "", "");
        
        SWTBotCombo combo = m_bot.comboBoxWithLabel("Stubbed function:");
        combo.setText(STUBBED_FUNC);
        combo.setFocus();
        m_bot.textWithLabel("Step index:").setFocus();
    }
    

    private void setTestPointsPage() {
        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_CUSTOM_RADIO_IDX);  // Occurrs = Custom
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(0,  1);
        
        setIdentifier(1, "g_char1", "x, y, z", "", "", "");
        setIdentifier(2, "g_char2", "q, w, e", "2", "3", "");
        
        m_ktableUtils.setCellContent(6, 1, "2");
        m_ktableUtils.setCellContent(6, 2, "Custom");
        
        SWTBotCombo combo = m_bot.comboBoxWithLabel("Test point ID:");
        combo.setText(TP_ID);
        combo.setFocus();
        m_bot.textWithLabel("Step index:").setFocus();
        
        m_ktableUtils.clickCell(1, 4);
        m_ktableUtils.clickCell(1, 5);
        m_ktableUtils.setCellContent(2, 4, "Max");
        m_ktableUtils.setCellContent(2, 5, "2");
        m_ktableUtils.setCellContent(1, 6, "<other values>");
        m_ktableUtils.setCellContent(2, 6, "3");
    }


    private void setHilPage() {

        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_1_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(0,  1);
        
        setIdentifier(1, HIL_OUT, "4, 5, 6", "", "", "");
    }
    
    
    private void setOptionsPage() {

        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_1_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(0,  1);
        
        setIdentifier(1, OPTION_URL, "", "-10", "-4", "2");
    }
    
    
    private void setScriptPage(String paramName, String values) {

        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_2_RADIO_IDX);
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(0,  1);
        
        setIdentifier(1, paramName + '1', values, "", "", "");
        setIdentifier(2, paramName + '2', values, "", "", "");
    }


    private void setTestCasePage() {
        SWTBotRadio swtBotRadio = m_bot.radio(OCCURR_CUSTOM_RADIO_IDX); 
        m_utils.clickRadio(swtBotRadio);
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.setCellContent(3, 1, "2"); // set custom occurrence to 2
    }


    private void setExpectedPage() {
        
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(1, 1);
        m_ktableUtils.setCellContent(1, 1, "rv == 12"); // set custom occurrence to 2
        m_ktableUtils.clickCell(1, 2);
        m_ktableUtils.setCellContent(1, 2, "g_char1 < 12"); // set custom occurrence to 2

        m_ktableUtils.selectKTable(1);
        m_ktableUtils.clickCell(1, 1);
        m_ktableUtils.setCellContent(1, 1, "iCounter < 0"); // set custom occurrence to 2

        m_ktableUtils.selectKTable(2);
        m_ktableUtils.clickCell(1, 1);
        m_ktableUtils.setCellContent(1, 1, "g_Array[2] == 1"); // set custom occurrence to 2

        m_ktableUtils.selectKTable(3);
        m_ktableUtils.clickCell(1, 1);
        m_ktableUtils.setCellContent(1, 1, "g_int1 == 121"); // set custom occurrence to 2
    }


    private void setDryRunPage() {
        m_ktableUtils.selectKTable(0);
        m_ktableUtils.clickCell(1, 1);
        m_ktableUtils.setCellContent(1, 1, "${rv}"); // set custom occurrence to 2
        m_ktableUtils.setCellContent(2, 1, "retVal"); // set custom occurrence to 2
        
        m_ktableUtils.clickCell(1, 2);
        m_ktableUtils.setCellContent(1, 2, "${hostVar}"); // set custom occurrence to 2
        m_ktableUtils.setCellContent(2, 2, "g_var"); // set custom occurrence to 2
    }


    private void setIdentifier(int line,
                               String identifier, String values, String rangeStart,
                               String rangeEnd, String step) {
        
        m_ktableUtils.addRowsUntil(0, line + 1);
        
        if (!identifier.isEmpty()) {
            m_ktableUtils.setCellContent(0, line, identifier);
        }
        
        if (!values.isEmpty()) {
            m_ktableUtils.setCellContent(1, line, values);
        }
        
        if (!rangeStart.isEmpty()) {
            m_ktableUtils.setCellContent(3, line, rangeStart);
        }

        if (!rangeEnd.isEmpty()) {
            m_ktableUtils.setCellContent(4, line, rangeEnd);
        }
        
        if (!step.isEmpty()) {
            m_ktableUtils.setCellContent(5, line, step);
        }
        
    }
}
