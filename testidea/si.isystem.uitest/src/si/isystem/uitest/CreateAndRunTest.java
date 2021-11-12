package si.isystem.uitest;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertContains;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertMatchesRegex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXParseException;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestLog.ESectionsLog;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.StrVector;
import si.isystem.connect.connect;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.dialogs.StructMembersSelectionDialog;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.LoggingControls;
import si.isystem.itest.ui.spec.MetaSpecEditor;
import si.isystem.itest.wizards.newtest.NewTCVariablesPage;
import si.isystem.swtbot.utils.KTableTestUtils;
import si.isystem.swtbot.utils.SWTBotKTable;

/**
 * This class implement UI tests for testIDEA product.
 * 
 * Groups of tests:
 * - fill all input fields, including comments
 * - test all menu options
 *   - test running of tests and reports
 *   - test exports/imports 
 *   - test all dialogs not tested so far
 * 
 * TODO:
 * 1. Replace all copy pasted strings from appl. in test proj.
 * 
 * @author markok
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CreateAndRunTest {

    private static final String DEFAULT_UUID = "be224ac6-28a2-4af1-ac8a-9880232799f1";
    public static final String TEST_OUT_DIR = "testOutput";
    
    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;
    private static KTableTestUtils m_ktableUtils;

    String m_yamlSpec;
    
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
        m_utils.deselectToolsPrefsAlwaysRunInitSeq();

        /* for (IBundleGroupProvider provider : Platform.getBundleGroupProviders()) {
            for (IBundleGroup feature : provider.getBundleGroups()) {
               final String providerName = feature.getProviderName();
               final String featureId = feature.getIdentifier();
               for (Bundle bundle : feature.getBundles()) {
                  System.out.println("bundle: " + bundle.getSymbolicName() + 
                                     "\n  fId: " + featureId + 
                                     "\n  pName: " + providerName);
               }
            }
         } */
        /* System.err.println("\n\n    ===========================================================================================\n" +
                           "    !!! IMPORTANT: These tests require updated version of SWTBot 2.0.5.                     !!!\n" +
                           "    !!! See file SWTBot-tips.txt for details about SWRBotRadio bug fix and building SWTBot. !!!\n" +
                           "    ===========================================================================================" +                         
                           "\n\n"); */
    }    

    
    @AfterClass
    public static void shutdown() {
        // m_utils.exitApplication(true); // Don't know why, but the method 
        // ApplicationWorkbenchWindowAdvisor::preWindowShellClose() is not called,
        // so the si.isystem.itest.ipc.ReceiverJob is still running and hence the message
    }
    

    @Before
    public void discardExistingAndCreateNewProject() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
    }
    
    
    // @Ignore("enable for complete tests, to much time consuming during development")
    @Test
    public void createAndRunTests() throws Exception {
        m_utils.createNewBaseUnitTest("min_int", "6, 9", "retVal == 6", "retVal", false);
        m_utils.setInitSequence();
        createTests();
        
        m_utils.toolbarInitTarget();
        m_utils.clickToolbarButton("Connect.*");  // refresh globals
        m_utils.toolbarRunAllTests();
        
        m_utils.waitForProgressDialog();
        m_bot.sleep(500);  // safety delay
        
        assertContains(UITestUtils.ALL_TESTS_OK_PREFIX, m_utils.getStatusViewText());
    }
    
    
    private void createTests() throws Exception {
        m_utils.clickToolbarButton(UITestUtils.TOOLTIP_CONNECT_TO_WIN_IDEA);
        m_utils.waitForConnectionDialog();
        System.out.println("Connected to winIDEA");
        m_bot.sleep(500);
        setFunctionSection();

        m_utils.createBaseTestWContextMenu("funcForIntStubTest", "", "retVal == 45", "retVal", false);
        m_utils.setSectionStub("stubbedFuncInt", "Yes", null, "s1RetVal", null, "1", "10", false);
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
        m_ktableUtils.setSectionStubAssignments(0, "s1RetVal", "45", new String[0]);

        m_utils.createBaseTestWContextMenu("Func1", "10", "rv == 20", "rv", false);
        m_utils.setSectionAnalyzer("Start", "func1.trd", "Write", "No", "No", "Yes", "Yes", "Coverage", false);
        m_utils.setSectionCoverageTestCase("Yes", "XML", "textCvrg.xml",  "", "Yes",
                                   "No", "No", "No", 
                                   "No", "No", "No", "No", "", "");
        m_utils.setSectionCoverageStatistics("Func1", 100, 100, 100, 0, 0, 100);
    }

    
    private void setFunctionSection() throws Exception {
        m_utils.createBaseTestWContextMenu("", "", "", null, false); // create empty test
        // now fill all fields
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestDescription("This sample demonstrates verification of coverage.");
        m_utils.setTestID("id_func1");
        m_utils.setTestTags("moduleA");
        
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_utils.setFUTName("Func1");
        m_utils.setFUTParams("11");
        m_utils.setFUTRetValName("retVal");
        
        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        m_utils.setVerificationExpr(0, "retVal == 21", true, 0);
    }
    
    
    /**
     * This method tests content proposals. 
     * IMPORTANT: testIDEA window must be large enough so that the expected table has at 
     * least 7 rows visible. Otherwise it may happen that the scrollers appear, which
     * hide half of the current row, so double click fails, ...
     * 
     * @throws Exception
     */
    // @Ignore ("enable for complete tests, to much time consuming during development")
    @Test
    public void testContentProposals() throws Exception {
        
        discardExistingAndCreateNewProject();
        m_utils.connect();
        
        m_utils.createNewBaseUnitTest("max_int", "7, 3", "x == 0", "retVal", false);
        
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_bot.textWithLabel("Params:").setFocus();
        
        final int FIRST_ROW = 1; // when the first row is empty (set in user preferences)
        m_utils.enterTextWithContentProposals(0, "g_co", FIRST_ROW);
        m_utils.enterTextWithContentProposals(-2, ".", FIRST_ROW + 3);
        m_utils.pressKey(',');        
        
        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        
        // test return value
        m_utils.setVerificationExpr(1, "r", true, FIRST_ROW); // tests for retVal
        m_utils.setVerificationExpr(1, " == 3", false, FIRST_ROW);  // tests for g_int1 - global var

        // test global variable
        m_utils.setVerificationExpr(2, "g_com", true, FIRST_ROW);  // tests for g_complexStruct - global var
        m_utils.setVerificationExpr(2, ".", false, FIRST_ROW);  
        m_utils.setVerificationExpr(2, " == 0", false, FIRST_ROW);  
        
        m_utils.setVerificationExpr(3, "g_com", true, FIRST_ROW); 
        m_utils.setVerificationExpr(3, ".m_", false, FIRST_ROW);  
        m_utils.setVerificationExpr(3, " != 2", false, FIRST_ROW);
        
        // test pointer
        m_utils.setVerificationExpr(4, "g_pcom", true, FIRST_ROW); 
        m_utils.setVerificationExpr(4, "->", false, FIRST_ROW);  
        m_utils.setVerificationExpr(4, " == 0", false, FIRST_ROW);
        
        m_utils.setVerificationExpr(5, "*g_pcom", true, FIRST_ROW); 
        m_utils.setVerificationExpr(5, ".", false, FIRST_ROW);  
        m_utils.setVerificationExpr(5, " == 0", false, FIRST_ROW);

        // test array
        m_utils.setVerificationExpr(6, "g_structAr", true, FIRST_ROW);
        m_utils.setVerificationExpr(6, "[1][2].", false, FIRST_ROW + 5);
        m_utils.setVerificationExpr(6, " < 33", false, FIRST_ROW);
        
        // test table contents or copy/paste text case and use API to test contents

        m_utils.selectTestCase("/", "max_int");
        m_utils.pressKey(SWT.CTRL, 'c');

        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        CTestFunction func = testSpec.getFunctionUnderTest(true);
        StrVector positionParams = new StrVector();
        func.getPositionParams(positionParams);
        assertEquals(3, positionParams.size());
        assertEquals("g_complexStruct.m_union", positionParams.get(0));
        assertEquals("7", positionParams.get(1));
        assertEquals("3", positionParams.get(2));
        
        StrVector expectedExprs = new StrVector();
        testSpec.getExpectedResults(expectedExprs);
        assertEquals("retVal == 3", expectedExprs.get(0));
        assertEquals("g_complexStruct.m_i == 0", expectedExprs.get(1));
        assertEquals("g_complexStruct.m_i != 2", expectedExprs.get(2));
        assertEquals("g_pcomplexStruct->m_i == 0", expectedExprs.get(3));
        assertEquals("*g_pcomplexStruct.m_i == 0", expectedExprs.get(4));
        assertEquals("g_structArray2D[1][2].m_ai < 33", expectedExprs.get(5));
      
        System.out.println("Done content proposals");
        m_bot.sleep(1000);
        // exitApplication(true);
    }


    // @Ignore
    @Test
    public void testContentProposalsforLocalVars() throws Exception {

        String funcName = initForContentProposalsTest();
        
        m_ktableUtils.selectKTable(SWTBotConstants.VARS_INIT_KTABLE);
        m_ktableUtils.clickCell(1, 1); // add row
        m_utils.setCellWithContentProposal(1, "myS", true, 1, SWTBotConstants.VARS_INIT_KTABLE);
        m_utils.setCellWithContentProposal(1, ".", false, 2, SWTBotConstants.VARS_INIT_KTABLE);
        m_ktableUtils.setDataCell(1, 0, "10");
        
        m_ktableUtils.clickCell(1, 2); // add row
        m_utils.setCellWithContentProposal(2, "myS", true, 1, SWTBotConstants.VARS_INIT_KTABLE);
        m_utils.setCellWithContentProposal(2, ".", false, 6, SWTBotConstants.VARS_INIT_KTABLE);
        m_ktableUtils.setDataCell(1, 1, "20");
        
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        StrVector keys = testSpec.getInitKeys();
        assertEquals("myStruct.m_i", keys.get(0));
        assertEquals("myStruct.m_ai", keys.get(1));
    }


    // @Ignore
    @Test
    public void testStructVarWizard() throws Exception {

        initForContentProposalsTest();
        
        m_bot.buttonWithId(SWTBotConstants.VAR_MEMBERS_WIZARD).click();
        m_utils.activateShell(StructMembersSelectionDialog.DLG_TITLE);
        m_utils.enterComboWLabel("Variable:", "myStruct");
        m_utils.pressKey(SWT.ARROW_RIGHT);
        
        m_bot.sleep(500); // don't know why this delay is required - without it
        // it seems m_bot tries to access trees in main window, which triggers an exception???
        // Combo box above is modified without problem, but tree can not be reached???
        // this does not help: ICondition condition = Conditions.shellIsActive(StructMembersSelectionDialog.DLG_TITLE);
        //                     m_utils.waitUntil(condition, 3000, true);
        
        SWTBotTree tree = m_bot.tree();
        SWTBotTreeItem subtree = tree.getTreeItem("myStruct: struct_td");
        subtree.getNode("m_ai: long [7]").toggleCheck();
        subtree.getNode("m_af: float [7]").toggleCheck();
        subtree.getNode("m_ad: double [7]").toggleCheck();
        
        m_bot.button("OK").click();
        
        m_utils.selectTestCase("/", "max_int");
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        StrVector keys = testSpec.getInitKeys();
        StrVector values = new StrVector();
        testSpec.getInitValues(values);
        
        assertEquals(keys.get(0), "myStruct.m_c");
        assertEquals(values.get(0), "0");
        
        assertEquals(keys.get(1), "myStruct.m_i");
        assertEquals(values.get(1), "0");
        
        assertEquals(keys.get(2), "myStruct.m_f");
        assertEquals(values.get(2), "0");
        
        assertEquals(keys.get(3), "myStruct.m_d");
        assertEquals(values.get(3), "0");
        
        assertEquals(keys.get(4), "myStruct.m_ac[0]");
        assertEquals(values.get(4), "0");
        
        assertEquals(keys.get(5), "myStruct.m_ac[1]");
        assertEquals(values.get(5), "0");        
        
        assertEquals(keys.get(14), "myStruct.m_pd");
        assertEquals(values.get(14), "0");                
    }
    
    
    protected String initForContentProposalsTest() throws Exception {
        discardExistingAndCreateNewProject();
        m_utils.clickToolbarButton(UITestUtils.TOOLTIP_CONNECT_TO_WIN_IDEA);
        m_utils.waitForConnectionDialog();
        m_bot.sleep(500); // wait until connects
        
        String funcName = "max_int";
        m_utils.createNewBaseUnitTest(funcName, "7, 3", "x == 0", "retVal", false);
        
        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        m_ktableUtils.selectKTable(SWTBotConstants.VARS_DECL_KTABLE);
        m_ktableUtils.clickCell(1, 1); // add row
        m_utils.setVarDeclaration(0, "myStruct", "struct_td");
        return funcName;
    }
     

    // @Ignore 
    @Test
    public void testDerivedFuncUndo() throws Exception {
        // create derived test with local var with name, which requires quoting, 
        // then modifyfunc name and undo
        m_utils.createNewBaseUnitTest("add_int", "26, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("base");

        m_utils.setVar(0, "int", "g_intArray1[0]", "0"); // this var requires quoting because of '[]'
        
        // 'Copy - Paste as Derived' function section to create a derived test without vars
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_bot.menu("Edit").menu("Copy").click();
        m_utils.selectTestTreeNode("base : add_int");
        m_bot.menu("Edit").menu("Paste As Derived").click();
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.fromDefaultToNotInherit(1);
        m_utils.setTestID("derived");
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_utils.setFUTName("min_int");

        // Undo & test
        m_utils.undo();
        assertEquals("add_int", m_utils.getFUTName());
        
        // in derived test edit types column and make sure the vals column is still derived - has
        // blue background. Reverse columns in the next test.
        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        m_utils.fromDefaultToNotInherit(0);
        m_utils.setVarType(0, "char");  // this statement defines 'locals' section in the derived test case
        
        m_utils.selectTestTreeNode("base : add_int");
        m_utils.copyToClipboard();
        CTestSpecification baseTestSpec = m_utils.getTestSpecFromClipboard();
        CTestSpecification derivedTestSpec = baseTestSpec.getDerivedTestSpec(0);
        assertTrue("Init section should remain derived, so it should be empty!", 
                   derivedTestSpec.isSectionEmpty(SectionIds.E_SECTION_INIT.swigValue()));
        assertFalse("Locals section should be defined in derived test case, so it should NOT be empty!", 
                   derivedTestSpec.isSectionEmpty(SectionIds.E_SECTION_LOCALS.swigValue()));

        // clear the section, then repeat the same for values column 
        m_utils.selectDerivedTestCase("base : add_int", "derived : add_int");
        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        
        m_utils.clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, "Variables", 
                                           "Clear Section");
        m_utils.fromDefaultToNotInherit(1);
        
        m_utils.setVarValue(0, "123456");   // this statement defines 'init' section 
                                            // in the derived test case
        
        m_bot.sleep(500);  // make it possible for human to see the change
        m_utils.selectTestTreeNode("base : add_int");
        m_utils.copyToClipboard();
        baseTestSpec = m_utils.getTestSpecFromClipboard();
        derivedTestSpec = baseTestSpec.getDerivedTestSpec(0);
        assertFalse("Init section should be defined in derived test case, so it should NOT be empty!", 
                   derivedTestSpec.isSectionEmpty(SectionIds.E_SECTION_INIT.swigValue()));
        assertTrue("Locals section should remain derived, so it should be empty!", 
                   derivedTestSpec.isSectionEmpty(SectionIds.E_SECTION_LOCALS.swigValue()));
    }
    
    
    // @Ignore 
    @Test
    public void testDragAndDrop() throws Exception {
        // 1 base test, 3 derived tests. Move 1 derived tests so it is derived 
        // from 3rd test. Undo. The moved test is moved back so they it is located 
        // beneath the base test and should not be located beneath 3rd test.

        m_utils.createNewBaseUnitTest("add_int", "26, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("testDragAndDrop");

        m_utils.selectTestTreeNode("testDragAndDrop : add_int");

        m_bot.menu("Edit").menu("Copy").click();
        m_bot.menu("Edit").menu("Paste As Derived").click();
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("derived1");
        
        m_utils.selectTestTreeNode("testDragAndDrop : add_int");
        m_bot.menu("Edit").menu("Paste As Derived").click();
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("derived2");
        
        m_utils.selectTestTreeNode("testDragAndDrop : add_int");
        m_bot.menu("Edit").menu("Paste As Derived").click();
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("derived3");
        
        // we have 3 derived tests now, let's move 'derived1' under 3rd derived test
        m_utils.dragAndDropDerivedTestCase("testDragAndDrop : add_int", 
                                           "derived1 : add_int", 
                                           "derived3 : add_int");

        m_utils.selectTestTreeNode("testDragAndDrop : add_int");
        m_utils.copyToClipboard();
        CTestSpecification baseTestSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("The base test spec should have two derived test specs!", 
                     2, baseTestSpec.getNoOfDerivedSpecs());
        assertEquals("Derived test case should have 1 derived test case!", 
                     1, baseTestSpec.getDerivedTestSpec(1).getNoOfDerivedSpecs());
        
        // undo
        m_utils.undo();
        
        m_utils.selectTestTreeNode("testDragAndDrop : add_int");
        
        m_utils.copyToClipboard();
        baseTestSpec = m_utils.getTestSpecFromClipboard();
        System.out.println("deri no = " + baseTestSpec.getNoOfDerivedSpecs());
        assertEquals("The base test spec should have three derived test specs!", 
                     3, baseTestSpec.getNoOfDerivedSpecs());
        System.out.println("deri no b = " + baseTestSpec.getDerivedTestSpec(2).getNoOfDerivedSpecs());
        assertEquals("Derived test case should have no derived test cases!", 
                     0, baseTestSpec.getDerivedTestSpec(2).getNoOfDerivedSpecs());
        
        System.out.println("drag and drop test end");
        // m_bot.sleep(5000);
    }

    
    // @Ignore 
    @Test
    public void testCutUndo() throws Exception {
        testCutDeleteUndo(false);
    }

    
    // @Ignore 
    @Test
    public void testDeleteUndo() throws Exception {
        testCutDeleteUndo(true);
    }


    private void testCutDeleteUndo(boolean isDelete) throws Exception {
        
        // Cuts several test specs at once, then performs undo. Test spec. count 
        // and order must be preserved. See also EditingTester.
        
        m_utils.createNewBaseUnitTest("add_int", "30, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo1");
        m_utils.createNewBaseUnitTest("add_int", "31, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo2");
        m_utils.createNewBaseUnitTest("add_int", "32, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo3");
        m_utils.createNewBaseUnitTest("add_int", "33, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo4");
        m_utils.createNewBaseUnitTest("add_int", "34, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo5");
        m_utils.createNewBaseUnitTest("add_int", "35, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo6");
        m_utils.createNewBaseUnitTest("add_int", "36, 362", "retVal == 388", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("cutUndo7");
        
        m_utils.selectTestCases("cutUndo2 : add_int", 
                                "cutUndo3 : add_int", 
                                "cutUndo4 : add_int", 
                                "cutUndo5 : add_int");
        
        if (isDelete) {
            m_utils.pressKey(SWT.DEL);
        } else {
            m_utils.cutToClipboard();
        }
        
        m_utils.selectAll();  // selects all test cases
        m_utils.copyToClipboard();
        
        CTestSpecification baseTestSpec = m_utils.getTestSpecFromClipboard();
        int numDerived = baseTestSpec.getNoOfDerivedSpecs();
        for (int i = 0; i < numDerived; i++) {
            CTestSpecification testCase = baseTestSpec.getDerivedTestSpec(i);
            if (testCase.getTestId() == "cutUndo1") {
                // test cases 2, 3, 4, and 5 were removed, so next test cases should be 6 and 7
                assertEquals("cutUndo6", baseTestSpec.getDerivedTestSpec(i + 1).getTestId());
                assertEquals("cutUndo7", baseTestSpec.getDerivedTestSpec(i + 2).getTestId());
                break;
            }
        }
        
        m_utils.undo();  // undo
        
        m_utils.selectAll();  // selects all test cases
        m_utils.copyToClipboard();
        baseTestSpec = m_utils.getTestSpecFromClipboard();
        numDerived = baseTestSpec.getNoOfDerivedSpecs();
        for (int i = 0; i < numDerived; i++) {
            CTestSpecification testCase = baseTestSpec.getDerivedTestSpec(i);
            if (testCase.getTestId() == "cutUndo1") {
                assertEquals("cutUndo2", baseTestSpec.getDerivedTestSpec(i + 1).getTestId());
                assertEquals("cutUndo3", baseTestSpec.getDerivedTestSpec(i + 2).getTestId());
                assertEquals("cutUndo4", baseTestSpec.getDerivedTestSpec(i + 3).getTestId());
                assertEquals("cutUndo5", baseTestSpec.getDerivedTestSpec(i + 4).getTestId());
                assertEquals("cutUndo6", baseTestSpec.getDerivedTestSpec(i + 5).getTestId());
                assertEquals("cutUndo7", baseTestSpec.getDerivedTestSpec(i + 6).getTestId());
                break;
            }
        }
        
        m_utils.redo();  // redo
        
        m_utils.selectTestCases("cutUndo1 : add_int", 
                                "cutUndo6 : add_int", 
                                "cutUndo7 : add_int");
        
        m_utils.pressKey(SWT.DEL);  // delete remaining test specs added by this test
        m_utils.selectAll();  // selects all test cases
        m_utils.copyToClipboard();
        baseTestSpec = m_utils.getTestSpecFromClipboard();
        numDerived = baseTestSpec.getNoOfDerivedSpecs();
        for (int i = 0; i < numDerived; i++) {
            CTestSpecification testCase = baseTestSpec.getDerivedTestSpec(i);
            assertFalse("All test specs with ids 'cutUndo*' should be removed!", 
                        testCase.getTestId().startsWith("cutUndo"));
        }
    }
    
    
    // @Ignore 
    @Test
    public void testMergedTable() throws Exception {
        // Base test should have two rows in the 'Expected' table. Then delete
        // the first row in derived test spec. Select other test case, then check
        // if expected was set properly in the first test case.
        
        m_utils.createNewBaseUnitTest("add_int", "77, 362", "retVal == 388, g_int1 == 12", "retVal", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("mergedExpectedTest");
        m_utils.createNewDerivedTest("mergedExpectedTest : add_int", "", "max_int", "", "", "", false);
        
        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        m_utils.fromDefaultToNotInherit(1);
        m_utils.deleteRowInExpectedTable(0);
        
        m_utils.selectTestTreeNode("mergedExpectedTest : add_int");
        m_utils.selectDerivedTestCase("mergedExpectedTest : add_int", "/ : max_int");
        m_utils.copyToClipboard();
        CTestSpecification testCase = m_utils.getTestSpecFromClipboard();
        StrVector expected = new StrVector();
        testCase.getExpectedResults(expected);
        assertEquals(1, expected.size());
        assertEquals("g_int1 == 12", expected.get(0));
        
    }

    // @Ignore 
    @Test
    public void testClearSectionAndUndo() throws Exception {
        
        // This test is also done in EditingTester for all sections.
        
        final String funcName = "add_int";
        final String testId = "clearAndUndo";
        
        m_utils.createNewBaseUnitTest(funcName, "177, 362", "retValue == 539", "retValue", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID(testId);
        final String testDesc = "This test clears the section and performs undo.";
        m_utils.setTestDescription(testDesc);
        m_utils.setTestTags("alpha, beta");

        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, UITestUtils.SEC_META, "Clear Section");

        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_utils.clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, UITestUtils.SEC_FUNCTION, "Clear Section");
        
        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        m_utils.clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, UITestUtils.SEC_EXPECTED, "Clear Section");
        
        SWTBotView view = m_bot.viewByTitle(UITestUtils.TEST_TREE_CAPTION);
        view.setFocus();
        m_utils.copyToClipboard();
        
        CTestSpecification testCase = m_utils.getTestSpecFromClipboard();
        assertEquals("Test ID should be empty!", "", testCase.getTestId());
        assertEquals("Test description should be empty but was!", "", testCase.getDescription());
        assertTrue("Test function should be empty but was: " + testCase.getFunctionUnderTest(true),
                   testCase.getFunctionUnderTest(true).isEmpty());
        
        StrVector tags = new StrVector();
        testCase.getTags(tags);
        assertEquals(0, tags.size());
        
        StrVector expected = new StrVector();
        testCase.getExpectedResults(expected);
        assertEquals(0, expected.size());

        // undo all clears
        m_utils.undo();
        m_utils.undo();  
        m_utils.undo();  
        m_utils.copyToClipboard();
        
        testCase = m_utils.getTestSpecFromClipboard();
        assertEquals(testId, testCase.getTestId());
        assertEquals(testDesc, testCase.getDescription());
        assertEquals(funcName, testCase.getFunctionUnderTest(true).getName());
        
        tags = new StrVector();
        testCase.getTags(tags);
        assertEquals(2, tags.size());
        
        expected = new StrVector();
        testCase.getExpectedResults(expected);
        assertEquals(1, expected.size());
    }

    
    // @Ignore 
    @Test
    public void testAutoIdSetting() throws Exception {
        // first init the model, test auto IDs when creating new test cases

        // set id format string used during test creation
        m_utils.setPropertiesAutoIdFormat("/${_seq}/-${_function}.${_params}.${_tags}");

        m_utils.createNewBaseUnitTest("add_int", "20, 45", "rv == 65", "rv", true);
        m_utils.createNewBaseUnitTest("min_int", "21, 46", "rv == 21", "rv", true);
        m_utils.createNewBaseUnitTest("max_int", "22, 47", "rv == 47", "rv", false);
        String expectedMinIntTreeText = "/1/-min_int.21-46. : min_int";
        m_utils.createNewDerivedTest(expectedMinIntTreeText, "", "Func1", "10", "retVal == 11", "retVal", true);
        m_utils.createNewDerivedTest(expectedMinIntTreeText, "", "Func2", "10, 20", "retVal == 11", "retVal", false);
        m_utils.createNewDerivedTest(expectedMinIntTreeText, "", "Func4", "10, 20", "retVal == 11", "retVal", true);

        m_utils.createNewDerivedDerivedTest(expectedMinIntTreeText, 
                                            "/3/-Func1.10. : Func1", "",
                                            "Func3", "33", "retVal == 66", "retVal", true);
        
        m_utils.selectAll();
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals(7, testSpec.getNoOfDerivedSpecs()); // there are 7 test specs selected

        CTestSpecification minIntTestSpec = m_utils.getTestSpecViaClipboard(expectedMinIntTreeText);
        assertEquals(3, minIntTestSpec.getNoOfDerivedSpecs());
        assertEquals(1, minIntTestSpec.getDerivedTestSpec(0).getNoOfDerivedSpecs());
        
        CTestSpecification addIntTestSpec = m_utils.getTestSpecViaClipboard("/0/-add_int.20-45. : add_int");
        CTestSpecification maxIntTestSpec = m_utils.getTestSpecViaClipboard("/ : max_int");
        assertEquals("/0/-add_int.20-45.", addIntTestSpec.getTestId());
        assertEquals("/1/-min_int.21-46.", minIntTestSpec.getTestId());
        assertEquals("", maxIntTestSpec.getTestId());
        
        assertEquals("/3/-Func1.10.", minIntTestSpec.getDerivedTestSpec(0).getTestId());
        assertEquals("", minIntTestSpec.getDerivedTestSpec(1).getTestId());
        assertEquals("/5/-Func4.10-20.", minIntTestSpec.getDerivedTestSpec(2).getTestId());
        
        CTestSpecification mostDerived = minIntTestSpec.getDerivedTestSpec(0).getDerivedTestSpec(0);
        assertEquals("/6/-Func3.33.", mostDerived.getTestId());

        // run context menu command
        m_utils.selectTestCase("/", "max_int");
        m_utils.clickTestTreeContextMenu(SWTBotConstants.BOT_TEST_TREE_ID, 
                                         "/ : max_int", 
                                         UITestUtils.CTX_TREE_MENU_SET_TEST_ID);
        /* if (m_utils.waitForShell("Connect to winIDEA?", 2000, false)) { // may appear or not - continue anyway in 2 seconds
            m_bot.button("OK").click();
        } */
        m_utils.waitForConnectionDialog();

        m_utils.fillDialogAutoIdCommand(2, 1, 2);
        
        // test the currently selected test spec, if the id was assigned
        m_utils.copyToClipboard();
        maxIntTestSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("/7/-max_int.22-47.", maxIntTestSpec.getTestId());
        // test one of the other test specs that id did not change
        
        minIntTestSpec = m_utils.getTestSpecViaClipboard(expectedMinIntTreeText);
        // the next line is actually redundant - if test spec was found, it has expected ID  
        assertEquals("/1/-min_int.21-46.", minIntTestSpec.getTestId());

        m_bot.sleep(2000);
        
        // open format dialog and set different combinations of fields and verify 
        // format and example strings
        m_bot.menu(UITestUtils.MENU_FILE).menu(UITestUtils.MENU_FILE__PROPERTIES).click();
        m_bot.tree(0).select("General");
        
        m_bot.textWithLabel("Auto ID Format:").setText("/${_uid}/${_function}.${_params}.${_tags}-");

        m_bot.button("Wizard...").click();
        
        final String wizardShellTitle = "Auto-ID Format";
        m_utils.waitForShell(wizardShellTitle, 3000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(wizardShellTitle);
        shellbot.activate();
        
        SWTBotText fmtString = m_bot.textWithLabel("Format string:");
        SWTBotText exampleString = m_bot.textWithLabel("Example:");

        // example strings must reflect setting in the Properties page
        assertEquals("/${_uid}/${_function}.${_params}.${_tags}-", fmtString.getText());
        assertEquals("/2x45gdf/min_int.20-30.alpha-beta-", exampleString.getText());
        
        
        m_utils.setAutoIdFormatWizardFields("a", 7, "b", 2, "c", 3, "d", 1, "e");
        assertEquals("a${_uid}b${_function}c${_params}d${_tags}e", fmtString.getText());
        assertEquals("a2x45gdfbmin_intc20-30dalpha-betae", exampleString.getText());

        m_utils.setAutoIdFormatWizardFields("", 2, ".", 3, ".", 1, "/", 4, "/");
        assertEquals("${_function}.${_params}.${_tags}/${_nid}/", fmtString.getText());
        assertEquals("min_int.20-30.alpha-beta/3f5gd4.3.2/", exampleString.getText());

        
        m_utils.setAutoIdFormatWizardFields("", 0, "", 0, "", 0, "", 0, "");
        assertEquals("", fmtString.getText());
        assertEquals("", exampleString.getText());

        m_utils.setAutoIdFormatWizardFields("", 0, "", 0, "", 8, "", 0, "");
        assertEquals("${_uuid}", fmtString.getText());

        assertEquals(DEFAULT_UUID, exampleString.getText());
        
        m_bot.button("OK").click();
        final String propsShellTitle = "Project properties";
        m_utils.waitForShell(propsShellTitle, 3000, true);
        shellbot = m_bot.shell(propsShellTitle);
        shellbot.activate();
        m_bot.button("OK").click();
        
        // open command dialog and test different combinations of radio buttons
        m_utils.setPropertiesAutoIdFormat("/${_nid}/${_params}");

        // all IDs of all test cases
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__SET_ID_FORMAT).click();
        m_utils.fillDialogAutoIdCommand(0, 0, 0);

        m_utils.selectTestCase(0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        String id = addIntTestSpec.getTestId(); 
        assertTrue("Expected reg. ex: '/.{11}/20-45', got: " + id,
                   id.matches("/.{12}/20-45"));
        
        m_utils.selectTestCase(1);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        assertTrue("Expected reg. ex: '/.{12}/21-46', got: " + addIntTestSpec.getTestId(), 
                   addIntTestSpec.getTestId().matches("/.{12}/21-46"));
        
        m_utils.selectTestCase(1, 0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertTrue("Expected reg. ex: '/.{12}\\.0/10', got: " + id,
                   id.matches("/.{12}\\.0/10"));
        
        m_utils.selectTestCase(1, 0, 0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertTrue("Expected reg. ex: '/.{12}\\.0\\.0/33', got: " + id,
                   id.matches("/.{12}\\.0\\.0/33"));
        
        // only UID of selected test cases
        m_utils.setPropertiesAutoIdFormat(""); // first rest all IDs to get selectable test cases
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__SET_ID_FORMAT).click();
        m_utils.fillDialogAutoIdCommand(0, 0, 0);
        
        m_utils.selectTestCases("/ : add_int", "/ : min_int");
        m_utils.setPropertiesAutoIdFormat("/${_nid}/${_params}");
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__SET_ID_FORMAT).click();
        m_utils.fillDialogAutoIdCommand(0, 1, 1);
        
        m_utils.selectTestCase(0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertTrue(id.matches("/.{12}/20-45"));
        
        m_utils.selectTestCase(1);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        assertTrue(addIntTestSpec.getTestId().matches("/.{12}/21-46"));
        
        m_utils.selectTestCase(1, 0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertEquals("", id); // not set, because the derived calss was not selected
        
        m_utils.selectTestCase(1, 0, 0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertEquals("", id);
        
        // only non-UID of selected and derived - test cases are still selected from 
        // the above test
        m_utils.setPropertiesAutoIdFormat("/${_nid}/${_function}");
        m_utils.selectTestCases(new int[]{0}, new int[]{1});
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__SET_ID_FORMAT).click();
        m_utils.fillDialogAutoIdCommand(0, 0, 2);

        m_utils.selectTestCase(0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertMatchesRegex("/.{12}/add_int", id);
        
        m_utils.selectTestCase(1);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        assertMatchesRegex("/.{12}/min_int", addIntTestSpec.getTestId());
        
        m_utils.selectTestCase(1, 0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertTrue(id.matches("/.{12}\\.0/Func1"));
        
        m_utils.selectTestCase(1, 0, 0);
        m_utils.copyToClipboard();
        addIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = addIntTestSpec.getTestId(); 
        assertTrue(id.matches("/.{12}\\.0\\.0/Func3"));

        // test changing only UID
        m_utils.setPropertiesAutoIdFormat("/${_nid}/${_params}");
        m_utils.selectTestCase(0);
        m_utils.copyToClipboard();
        maxIntTestSpec = m_utils.getTestSpecFromClipboard();
        id = maxIntTestSpec.getTestId();
        assertMatchesRegex("/.{12}/add_int", id);
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__SET_ID_FORMAT).click();
        m_utils.fillDialogAutoIdCommand(0, 2, 1);
        m_utils.copyToClipboard();
        maxIntTestSpec = m_utils.getTestSpecFromClipboard();
        String newid = maxIntTestSpec.getTestId();
        assertTrue(newid.matches("/.{12}/add_int")); // non-uid part must remain the same
        assertFalse(id.equals(newid)); // ID has changed
        
        // test changing only NON-UID part
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__SET_ID_FORMAT).click();
        m_utils.fillDialogAutoIdCommand(0, 3, 1);
        m_utils.copyToClipboard();
        maxIntTestSpec = m_utils.getTestSpecFromClipboard();
        String nonUidID = maxIntTestSpec.getTestId();
        assertTrue(nonUidID.matches("/.{12}/20-45")); // non-uid part has changed to params
        assertEquals(newid.substring(0, 13), nonUidID.substring(0, 13)); // uid has to be the same
    }


    // @Ignore 
    @Test
    public void testScriptGeneration() throws Exception
    {
        String defaultXslt = connect.getDEFAULT_XSLT_NAME();
        
        // copy XSLT files from si.isystem.itest/templates 
        Path src = Paths.get("../si.isystem.itest.plugin.core/templates/reports/" + defaultXslt);
        Path dest = Paths.get(TEST_OUT_DIR + "/" + defaultXslt);
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        
        src = Paths.get("../si.isystem.itest.plugin.core/templates/reports/blue.css");
        dest = Paths.get(TEST_OUT_DIR + "/blue.css");
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        
        m_utils.discardExistingAndCreateNewProject();
        
        m_utils.createNewBaseUnitTest("add_int", "3, 7", "rv == 10", "rv", false);
        m_utils.createNewBaseUnitTest("min_int", "2, 5", "rv == 2", "rv", false);
        TestSpecificationModel model = UITestUtils.getActiveModel();
        CTestReportConfig reportConfig = model.getTestReportConfig();
        // must be '\\' in path, otherwise Internet Explorer uses http, not file protocol 
        reportConfig.setFileName("testsForScriptTestReport-Python.xml");
        reportConfig.setXsltForFullReport(defaultXslt);
        model.saveModelAs(TEST_OUT_DIR + "/testsForScriptTest.iyaml");
        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TOOLS, 
                                       UITestUtils.MENU_GENERATE_SCRIPT, 
                                       UITestUtils.DIALOG_TITLE_GENERATE_TEST_SCRIPT);
        
        String scriptFile = "generatedScript.py";
        m_utils.setGenerateScriptDialog(scriptFile, true);
        
        // if the script file already exists, a 'Overwrite' question dialog will appear
        m_utils.waitForShellAndClickOK("Confirm file overwrite", 1500, false);

        // run Python
        Process proc = Runtime.getRuntime().exec("python " + scriptFile, 
                                                 null, 
                                                 new File(TEST_OUT_DIR));
        
        ExternalProcessStream outStream = new ExternalProcessStream(proc.getInputStream(), false);        
        ExternalProcessStream errorStream = new ExternalProcessStream(proc.getErrorStream(), true);
        outStream.start();
        errorStream.start();
                
        int exitVal = proc.waitFor();
        
        assertEquals("Generated Python test script did not execute successfully!", 0, exitVal);
        
        // generate Perl script
//        reportConfig.setFileName("testsForScriptTestReport-Perl.xml");
//        model.saveModelAs(TEST_OUT_DIR + "/testsForScriptTest.iyaml");
//        
//        m_bot.sleep(700);
//        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TOOLS, 
//                                       UITestUtils.MENU_GENERATE_SCRIPT, 
//                                       UITestUtils.DIALOG_TITLE_GENERATE_TEST_SCRIPT);
//        scriptFile = "generatedScript.pl";
//        m_utils.setGenerateScriptDialog(scriptFile, false);
//        m_utils.waitForShellAndClickOK("Confirm file overwrite", 1500, false);
//        
//        // run Perl
//        proc = Runtime.getRuntime().exec("/appsmk/perl-5.10/bin/perl.exe " + scriptFile,
//                                         null,
//                                         new File(TEST_OUT_DIR));
//        
//        outStream = new ExternalProcessStream(proc. getInputStream(), false);        
//        errorStream = new ExternalProcessStream(proc.getErrorStream(), true);
//        outStream.start();
//        errorStream.start();
//                
//        exitVal = proc.waitFor();
//        
//        assertEquals("Generated Perl test script did not execute successfully!", 0, exitVal);
    }
    
    
    // @Ignore
    @Test
    public void testXMLSchemaForCoverage() throws Exception {

        boolean isCreateExportFile = false; // set this to false when repeating 
        // the test and coverage export file can be reused.

        String cvrgExportFileBasic = "d:\\tmp\\coverageResultsBasic.xml";
        String cvrgExportFileFull = "d:\\tmp\\coverageResultsFull.xml";

        if (isCreateExportFile  ||  !m_utils.isFileCreatedToday(cvrgExportFileBasic)
                                ||  !m_utils.isFileCreatedToday(cvrgExportFileFull)) {
            m_utils.discardExistingAndCreateNewProject();
            m_utils.createSystemTestCaseWCoverage("basicCvrg", cvrgExportFileBasic, false);
            m_utils.createSystemTestCaseWCoverage("fullCvrg", cvrgExportFileFull, true);
            m_utils.setInitSequence();

            m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "2");

            // init target to be ready for unit tests in the following tests
            m_utils.clickToolbarButton("Init Target.*");
            m_utils.waitForProgressDialog();
        }
            
        try {
            m_utils.validateXMLSchema(cvrgExportFileBasic, 
                                      "d:\\bb\\trunk\\Utils\\Altova\\StyleVison\\Coverage\\analyzer_coverage_export_0.xsd");
            m_utils.validateXMLSchema(cvrgExportFileFull, 
                                      "d:\\bb\\trunk\\Utils\\Altova\\StyleVison\\Coverage\\analyzer_coverage_export_0.xsd");
            System.out.println("Coverage schema OK!");
        } catch (SAXParseException exception) {
            String msg = "Error: " + exception + '\n';
            msg += "  Line/Col: " + exception.getLineNumber() + " / " + exception.getColumnNumber();
            System.out.println(msg);
            assertTrue(msg, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(ex.getMessage(), false);
        }
    }


    // @Ignore
    @Test 
    public void testXMLSchemaForReports() throws Exception {
        
        // set it to false when report is saved but schema does not match -
        // you don't need to run all the tests for each schema fix
        boolean isCreateTests = false;
        boolean isRunTests = true;
        
        final String fileName = "D:/tmp/testIDEA-ui-test-temp-file.iyaml";
        if (isCreateTests  ||  !m_utils.isFileCreatedToday(fileName)) {
            m_utils.discardExistingAndCreateNewProject();
            m_utils.createTestsForFunctionCoverageProfilerAndScripts();
            UITestUtils.getActiveModel().saveModelAs(fileName);
        } else {
            m_utils.loadTestSpecFromFile(fileName);
        }
        
        if (isRunTests) {
            m_utils.clickToolbarButton("Run All.*");
            /* if (m_utils.waitForShell("Not connected", 2000, false)) { // may appear or not - continue anyway in 2 seconds
                m_bot.button("Connect Now").click();
            }

            if (m_utils.waitForShell("Connect to winIDEA?", 2000, false)) { // may appear or not - continue anyway in 2 seconds
                m_bot.button("OK").click();
            } */
            m_utils.waitForConnectionDialog();

            m_utils.waitForProgressDialog();
            System.out.println("--- Progress dialog ended!");

            assertContains("Test report for selected editor, 9 test(s), 0 group(s):\n" +
                    "- 5 tests (56%) completed successfully\n" +
                    "- 4 tests (44%) failed (invalid results)",
                    m_utils.getStatusViewText());
            m_utils.saveReport(true, "D:/tmp/reportFull.xml", true);
            m_utils.saveReport(false, "D:/tmp/reportErrorsOnly.xml", true);
        }
       
        m_utils.validateSchema(UITestUtils.REPORT_SCHEMA_LOCATION, 
                               "D:/tmp/reportFull.xml");
        m_utils.validateSchema(UITestUtils.REPORT_SCHEMA_LOCATION, 
                               "D:/tmp/reportErrorsOnly.xml");
    }
    
    
    // @Ignore
    @Test 
    public void testVerifySymbols() throws Exception {
        
        // set it to false when report is saved but schema does not match -
        // you don't need to run all the tests for each schema fix
        boolean isCreateTests = true;
        
        final String fileName = "D:/tmp/testIDEA-uiTest-symbols-temp-file.iyaml";
        if (isCreateTests  ||  !m_utils.isFileCreatedToday(fileName)) {
            m_utils.discardExistingAndCreateNewProject();
            m_utils.createTestsVerifySymbolsAndRename();
            UITestUtils.getActiveModel().saveModelAs(fileName);
        } else {
            m_utils.loadTestSpecFromFile(fileName);
        }
        
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__VERIFY_SYMBOLS).click();
        m_utils.waitForConnectionDialog();
        
        assertEquals("No problems found!", m_utils.getStatusViewText());
        
        m_utils.createNewBaseUnitTest("min_max", "3,  5", "rv == 3", "rv", true);

        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__VERIFY_SYMBOLS).click();
        assertEquals("ERROR: Function 'min_max' not found in the download file.", m_utils.getStatusViewText().trim());
        
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_utils.setFUTName("min_int");
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setTestID("test-0");
        
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__VERIFY_SYMBOLS).click();
        m_bot.sleep(1000); // wait until status view gets refreshed: TODO: should wait in a loop with timeout
        assertEquals("Duplicate ID found (1): test-0", m_utils.getStatusViewText());
    }
    
    
    // @Ignore
    @Test 
    public void testRename() throws Exception {
        
        // set it to false when report is saved but schema does not match -
        // you don't need to run all the tests for each schema fix
        boolean isCreateTests = false;
        
        final String fileName = "D:/tmp/testIDEA-uiTest-rename-temp-file.iyaml";
        if (isCreateTests  ||  !m_utils.isFileCreatedToday(fileName)) {
            m_utils.discardExistingAndCreateNewProject();
            m_utils.createTestsVerifySymbolsAndRename();
            UITestUtils.getActiveModel().saveModelAs(fileName);
        } else {
            m_utils.loadTestSpecFromFile(fileName);
        }
        
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__RENAME).click();
        m_utils.waitForConnectionDialog();
        
        m_utils.waitForShell("Rename", 2000, true);
        m_utils.enterComboWLabel("Old name:", "min_int");
        m_utils.enterComboWLabel("New name:", "max_int");
        m_bot.button("Find Next").click();
        m_bot.button("Rename").click();
        m_bot.button("Rename All").click();
        m_utils.waitForShellAndClickOK("Rename finished!", 2000, true);
        m_bot.button("Close").click();
        
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__RENAME).click();
        m_utils.waitForShell("Rename", 2000, true);
        m_bot.comboBoxWithLabel("Category:").setSelection(1);
        m_utils.enterComboWLabel("Old name:", "iCounter");
        m_utils.enterComboWLabel("New name:", "fValue");
        m_bot.button("Rename All").click();
        m_utils.waitForShellAndClickOK("Rename finished!", 2000, true);
        m_bot.button("Close").click();
        
        CTestSpecification ts0 = 
                m_utils.getTestSpecViaClipboard(TestSpecificationModel.getTestSpecificationName("test-0", 
                                                                                                "max_int"));
        assertEquals("max_int", ts0.getFunctionUnderTest(true).getName());
        
        CTestSpecification ts2 = 
                m_utils.getTestSpecViaClipboard(TestSpecificationModel.getTestSpecificationName("test-2", 
                                                                                                "max_int"));
        assertEquals("max_int", ts2.getFunctionUnderTest(true).getName());
        CTestCoverageStatistics stat = ts2.getAnalyzer(true).getCoverage(true).getStatistics(0);
        assertEquals("max_int", stat.getFunctionName());
        
        CTestSpecification ts3 = 
                m_utils.getTestSpecViaClipboard(TestSpecificationModel.getTestSpecificationName("test-3", 
                                                                                                "max_int"));
        assertEquals("max_int", ts3.getFunctionUnderTest(true).getName());
        CTestProfilerStatistics codeArea = ts3.getAnalyzer(true).getProfiler(true).getArea(EAreaType.CODE_AREA, 
                                                                                           0);
        assertEquals("max_int", codeArea.getAreaName());
        
        CTestProfilerStatistics dataArea = ts3.getAnalyzer(true).getProfiler(true).getArea(EAreaType.DATA_AREA, 
                                                                                           0);
        assertEquals("fValue", dataArea.getAreaName());

        // test stub rename
        m_bot.menu(UITestUtils.MENU_TOOLS).menu(UITestUtils.MENU_TOOLS__RENAME).click();
        m_utils.waitForShell("Rename", 2000, true);
        m_bot.comboBoxWithLabel("Category:").setSelection(0);
        m_utils.enterComboWLabel("Old name:", "max_int");
        m_utils.enterComboWLabel("New name:", "average_int");
        m_bot.button("Find Next").click();
        m_bot.button("Rename").click();
        m_bot.button("Rename All").click();
        m_utils.waitForShellAndClickOK("Rename finished!", 2000, true);
        m_bot.button("Close").click();
        
        CTestSpecification ts = 
                m_utils.getTestSpecViaClipboard(TestSpecificationModel.getTestSpecificationName("test-1", 
                                                                                                "add_int"));
        m_utils.closeActiveEditor(); // prevent it to be saved later
        assertEquals("average_int", ts.getStub("average_int").getFunctionName());

    }
    
    
    // @Ignore 
    @Test
    public void testExtensionScriptWizard() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        m_utils.createNewBaseUnitTest("add_int", "3, 7", "rv == 10", "rv", false);
        
        final String STUB_FUNCTION_NAME = "demoStub";
        final String TEST_POINT_ID = "myTestPoint";
        m_utils.selectTestCase("/", "add_int");
        
        m_utils.setSectionStub(STUB_FUNCTION_NAME, "Yes", "", "srv", "", "", "", false);
        m_utils.setSectionTestPoint(TEST_POINT_ID, "Yes", 0, "", "", false);
        
        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TOOLS, 
                                       UITestUtils.MENU_SCRIPT_EXTENSIONS_WIZARD, 
                                       UITestUtils.DIALOG_SCRIPT_EXTENSIONS_WIZARD);

        final String MODULE_NAME = "testTestExtensions"; 
        final String CLASS_NAME = "TestExtCls";
        final String ACTUAL_FILE_NAME = "d:\\bb\\trunk\\sdk\\targetProjects\\" + 
                                        MODULE_NAME + ".py";
        
        m_utils.enterTextWLabel("Script file:", ACTUAL_FILE_NAME);
        m_bot.sleep(100);
        m_utils.pressKey(SWT.ALT, 'o'); // workaround for bug in SWTBot radio
                                        // button handling - selection event is not generated!
        // m_utils.clickRadioWLabelIfEnabled("Overwrite existing file");
        
        m_bot.sleep(500);
        m_utils.enterTextWLabel("Class name:", CLASS_NAME);
        m_bot.sleep(100);
        m_utils.pressKey(SWT.SPACE); // triggers modify listener to enable wizard buttons
        m_utils.selectCheckBoxWText("Open the generated script in editor after this wizard closes", true);
        m_utils.enterTextWLabel("Script editor:", "D:\\apps\\Python27\\Lib\\idlelib\\idle.bat");
        
        m_utils.clickButton("Next >");
        
        m_utils.selectCheckBoxWText("Create script method", true);
        m_utils.selectCheckBoxWText("Generate method name", true);
        m_utils.selectCheckBoxWText("Use test ID in script method name", true);
        m_utils.selectCheckBoxWText("Use function name in script method name", true);
        
        m_utils.selectCheckBoxWText("Pass test specification as parameter", true);
        m_utils.selectCheckBoxWText("Evaluate variables, parameters, registers, ...", true);
        m_utils.selectCheckBoxWText("Modify variables, parameters, registers, ...", true);
        
        m_utils.clickButton("Next >");
        m_bot.sleep(500);
        
        m_utils.clickButton("Next >");  
        
        // stub script extension 
        m_utils.selectCheckBoxWText("Create script method", true);
        m_utils.selectCheckBoxWText("Generate method name", true);
        
        m_utils.selectCheckBoxWText("Pass test specification as parameter", true);
        m_utils.selectCheckBoxWText("Evaluate variables, parameters, registers, ...", true);
        m_utils.selectCheckBoxWText("Modify variables, parameters, registers, ...", true);
        m_utils.selectCheckBoxWText("Count calls", true);
        
        m_bot.sleep(500);
        m_utils.clickButton("Next >");
        
        // test point script extension 
        m_utils.selectCheckBoxWText("Create script method", true);
        m_utils.selectCheckBoxWText("Generate method name", true);
        
        m_utils.selectCheckBoxWText("Pass test specification as parameter", true);
        m_utils.selectCheckBoxWText("Evaluate variables, parameters, registers, ...", true);
        m_utils.selectCheckBoxWText("Modify variables, parameters, registers, ...", true);
        m_utils.selectCheckBoxWText("Count calls", true);
        
        m_bot.sleep(500);
        m_utils.clickButton("Next >");

        m_bot.sleep(500);
        m_utils.clickButton("Next >");
        m_bot.sleep(500);
        m_utils.clickButton("Next >");
        m_bot.sleep(500);
        m_utils.clickButton("Finish");
        
        System.out.println("The generated script should open IDLE editor!");
        
        // verify setting in script configuration
        CScriptConfig scriptCfg = UITestUtils.getActiveModel().getCEnvironmentConfiguration().getScriptConfig(true);
        assertEquals(MODULE_NAME + "." + CLASS_NAME, scriptCfg.getExtensionClass());
        StrVector modulesList = new StrVector();
        scriptCfg.getModules(modulesList);
        boolean isFound = false;
        for (int i = 0; i < modulesList.size(); i++) {
            if (modulesList.get(i).equals(MODULE_NAME)) {
                isFound = true;
                break;
            }
        }
        
        assertTrue("Module name not found in the list of python modules!", isFound);
        
        // verify setting in test spec
        m_utils.selectTestCase("/", "add_int");
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        CTestFunction tFunc = testSpec.getInitTargetFunction(true);
        assertEquals("__add_int__initTarget", tFunc.getName());
        
        tFunc = testSpec.getInitFunction(true);
        assertEquals("__add_int__initTest", tFunc.getName());
        
        CTestStub stub = testSpec.getStub(STUB_FUNCTION_NAME);
        String scriptFuncName = stub.getScriptFunctionName();
        assertEquals("stubScript_" + STUB_FUNCTION_NAME, scriptFuncName);
        
        CTestPoint testPoint = testSpec.getTestPoint(TEST_POINT_ID);
        scriptFuncName = testPoint.getScriptFunctionName();
        assertEquals("testPointScript_" + TEST_POINT_ID, scriptFuncName);
        
        // compare file with reference file
        String []diff = m_utils.compareFiles(TEST_OUT_DIR + "/testTestExtensionsExpected.py", ACTUAL_FILE_NAME);
        if (diff != null) {
            // if 'diff != null' if contains different lines, so the next assert
            // will terminate and show the difference at the same time!
            assertEquals(diff[0], diff[1]);
        }
    }

    
    /**
     * Creates simple test specification.
     * @throws Exception 
     */
    // // @Ignore 
    @Test
    public void testCTEExportSimple() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        // first create and export a simple test spec
        String funcName = "add_int";
        String testID = "cteExportTest";
        String cteExportImportFileName = "d:\\tmp\\test1.cte";
        
        m_utils.createNewBaseUnitTest(funcName, "-3, 7", "rv == 4", "rv", false);
        m_utils.selectTestCase("/", funcName);
        m_utils.setTestID(testID);
        m_utils.selectTestCase(testID, funcName);

        m_utils.openExportDialog();
       
        m_utils.setExportImportDialog(cteExportImportFileName, 2, false, false, true);

        // if the script file already exists, a 'Overwrite' question dialog will appear
        m_utils.waitForShellAndClickOK("Confirm file overwrite", 1500, false);


        // import 
        m_bot.sleep(700);
        m_utils.discardExistingAndCreateNewProject();
        m_utils.createNewBaseUnitTest(funcName, "", "", "", false);
        m_utils.setTestID(testID);
        m_utils.selectTestCase(testID, funcName);

        m_utils.openImportDialog();
        
        m_utils.setExportImportDialog(cteExportImportFileName, 2, false, null, false);

        m_utils.selectTestCase(testID, funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals(testID, testSpec.getTestId());
        assertEquals(0, testSpec.getNoOfDerivedSpecs());
        CTestFunction tFunc = testSpec.getFunctionUnderTest(true);
        assertEquals(funcName, tFunc.getName());
        assertEquals("rv", tFunc.getRetValueName());

        StrVector expectd = new StrVector();
        testSpec.getExpectedResults(expectd);
        assertEquals("rv == 4", CYAMLUtil.strVector2Str(expectd));
        
        StrVector params = new StrVector();
        testSpec.getPositionParams(params);
        assertEquals("-3, 7", CYAMLUtil.strVector2Str(params));
    }
    
    
    /**
     * Creates test specification with all sections with test input parameters
     * (which get exported to CTE as Classification Tree).
     * @throws Exception
     */
    
    // KEEP THIS TEST IGNORED, UNTIL WE HAVE AT LEAST ONE CUSTOMER CONFIRMED
    // TO USE testIDEA and CTE!
    // @Ignore    // fails on import of stub script params 
    @Test
    public void testCTEExportWAllSections() throws Exception {
        execExportImport("d:\\tmp\\test1.cte", 2, true, true);
    }

    
    @Test
    public void testXLSXExportImport() throws Exception {
    
        execExportImport("d:\\tmp\\regTest.xlsx", 0, false, true);
        execExportImport("d:\\tmp\\regTest.xlsx", 0, false, false);
    }
    
    
    // @param exportTypeRadioButtonIdx see export/import dialog for  
    //        value (currently 0 for XLSX, 2 for CTE)
    private void execExportImport(String exportFileName, 
                                  int exportTypeRadioButtonIdx,
                                  boolean isCreateParentTestCase,
                                  boolean isCreateUnitTest) throws Exception {

        // first create and export a simple test spec
        String testID = "exportedTest";
        
        CTestSpecification srcTestSpec;
        String funcName;
        if (isCreateUnitTest) {
            funcName = "add_int";
            srcTestSpec = createUnitTCsForExportImportTest(testID, funcName);
        } else {
            funcName = "/";
            srcTestSpec = createSystemTCsForExportImportTest(testID);
        }
        
        m_utils.openExportDialog();

        m_utils.setExportImportDialog(exportFileName, exportTypeRadioButtonIdx, false, false, true);

        // if the export file already exists, a 'Overwrite' question dialog will appear
        m_utils.waitForShellAndClickOK("Confirm file overwrite", 1500, false);
        m_bot.sleep(500); // without this sleep m_bot sometimes still refers to dialog shell,
                           // not the main window, so menu 'File' is not found.
        // import 
        m_utils.discardExistingAndCreateNewProject();

        if (isCreateParentTestCase) {
            // CTE can import only to an existing test case
            m_utils.createNewBaseUnitTest(funcName, "", "", "", false);
            m_utils.setTestID(testID);
            m_utils.selectTestCase(testID, funcName);
        }

        m_utils.openImportDialog();

        m_utils.setExportImportDialog(exportFileName, exportTypeRadioButtonIdx, 
                                      !isCreateParentTestCase, // If there is no parent, do not require selection
                                      null, false);

        m_utils.selectTestCase(testID, funcName);
        m_utils.copyToClipboard();

        CTestSpecification importedTestSpec = m_utils.getTestSpecFromClipboard();

        // test only the first test spec, as the second one has derived sections on export,
        // which became defined after import (test specs used for export/import tests are
        // not best suited for CTE tests).
        int numDerived = 1; // importedTestSpec.getNoOfDerivedSpecs();
        
        for (int tsIdx = 0; tsIdx < numDerived; tsIdx++) {
            
            CTestSpecification derivedSrcTs = srcTestSpec.getDerivedTestSpec(tsIdx);
            CTestSpecification derivedImpTs = importedTestSpec.getDerivedTestSpec(tsIdx);

            // clear an empty line from mapping, which is ignored on import
            CMapAdapter initVars = new CMapAdapter(derivedSrcTs, SectionIds.E_SECTION_INIT.swigValue(), true);
            initVars.removeEntry("");
            
            initVars = new CMapAdapter(derivedImpTs, SectionIds.E_SECTION_INIT.swigValue(), true);
            initVars.removeEntry("");
            
            assertTrue("Imported test spec differs from original one! idx = " + tsIdx, 
                       derivedSrcTs.equalsData(derivedImpTs));
        }
    }

    
    private CTestSpecification createUnitTCsForExportImportTest(String testID, 
                                                                String funcName) throws Exception 
    {
        boolean createFileUncond = true;
        String fileName = TEST_OUT_DIR + "/exportImportTest-" + funcName + ".iyaml";

        if (!createFileUncond  &&  m_utils.isFileCreatedToday(fileName)) {
            m_utils.loadTestSpecFromFile(fileName);
        } else {

            m_utils.discardExistingAndCreateNewProject();

            // function
            m_utils.createNewBaseUnitTest(funcName, "-3, 7", "rv == 4", "rv", false);
            m_utils.selectTestCase("/", funcName);

            // meta
            m_utils.setTestID(testID);
            m_utils.setTestDescription("This is test desc in\ntwo lines.");
            m_utils.setTestTags("tagA, tagB");

            // variables
            m_utils.setVarInit(0, "g_char1", "'x'");
            m_utils.setVarInit(1, "g_int1", "45");

            // stubs
            m_utils.setSectionStub("Func1", "Yes", "a,  b", "srv", "myStubScript", "2", "34", false);

            m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, "srv", "22", new String[]{"42", "43"});

            // user stubs
            m_utils.setSectionUserStubs("userStubbedFunc", "Yes", "Call target function", "replFunc", false);

            // test points
            m_utils.setSectionTestPoint("tesPointA", "Yes", 5, "i < 90", "tpScriptFunc", false);
            m_utils.setStubTestPointLogging("a < m, pp", "r != 0, a");
            m_utils.setSrcLocationDlg("File", "d:\\common\\main.c", 10, "Yes", 
                                      23, "Code", "Plain text", "humbleNundle", 12, 5);

            m_ktableUtils.selectKTable(SWTBotConstants.TEST_POINTS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, "val", "'t'", new String[]{"2", "3"});

            // analyzer
            m_utils.setSectionAnalyzer("Start", "analyzer.trd", "Update", "No", "No", "Yes", 
                                       "Default (No)", "myAnalTrigger", false);
            m_utils.setSectionCoverageTestCase("No", "XML", "cvrgExport.xml", "default",
                                       "No",
                                       "No", "No", 
                                       "No", "No", 
                                       "No", "No", 
                                       "No", "modulesFilter", "functionsFilter");
            m_utils.setSectionCoverageStatistics("coveredFunc", 88, 87, 86, 85, 84, 83);

            m_utils.setSectionProfiler("Yes", "CSV", "profilerExport.csv", "Yes", "Yes", "No");
            m_utils.setSectionProfilerArea(true, "profiledFunc", 
                                           new String[]{"12"},  // netLow 
                                           null, // netHigh, 
                                           null, // grossLow, 
                                           null, // grossHigh, 
                                           null, // callOrOutsideLow, 
                                           null, // callOrOutsideHigh, 
                                           null, // periodLow, 
                                           new String[]{"15"}, // periodHigh, 
                                           "45", // hitsLow, 
                                           "67" // hitsUp
                    );

            // trace
            m_utils.setSectionTrace("Default (No)", "Text", "traceEXport.txt");

            // HIL
            m_utils.setSectionHIL(0, "DOUT_1", "0", false);
            m_utils.setSectionHIL(1, "AOUT_1", "0.45", false);

            // scripts
            m_utils.setSectionScripts("initTargetFunc", "a, b", 
                                      "initFunc", "c, d", 
                                      "endFunc", "e, f", 
                                      "restoreTargetFunc", "g, h", 
                                      false);
            // options
            m_utils.setSectionOptions(0, "option\\one", "0", false);



            // now create two derived test specs - one with own values, the other one with inherited ones
            m_utils.selectTestCase(testID, funcName);
            m_utils.createNewDerivedTest(testID + " : " + funcName, "", "", "0, -5", "rv == -5", "", true);

            m_utils.setVarInit(0, "g_char1", "'y'", 0, 1);
            m_utils.setVarInit(1, "g_int1", "46");

            // stubs
            m_utils.setSectionStub("Func2", "Yes", "a,  b", "srv", "myStubScript", "5", "67", true);
            m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, "srv", "23", new String[]{"52", "43"});

            // user stubs
            m_utils.setSectionUserStubs("userStubbedFuncX", "Yes", "Call target function", "replFuncX", true);

            // test points
            m_utils.setSectionTestPoint("testPointAX", "Yes", 4, "ix < 80", "tpScriptFuncX", true);
            m_utils.setStubTestPointLogging("a < m, ppx", "r != 0, ax");
            m_utils.setSrcLocationDlg("File", "d:\\common\\mainx.c", 10, "Yes", 
                                      23, "Code", "Plain text", "humbleNundleX", 12, 5);

            m_ktableUtils.selectKTableWithContent(1, 0, "Test points");
            m_ktableUtils.selectCell(1, 2);
            m_ktableUtils.selectKTable(SWTBotConstants.TEST_POINTS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, "val", "'tx'", new String[]{"2x", "3x"});

            // analyzer
            m_utils.setSectionAnalyzer("Start", "analyzerX.trd", "Update", "No", "No", "Yes", 
                                       "Default (No)", "myAnalTriggerX", true);
            m_utils.setSectionCoverageTestCase("No", "XML", "cvrgExportX.xml", "default",
                                       "No",
                                       "No", "No", 
                                       "No", "No", 
                                       "No", "No", 
                                       "No", "modulesFilterX", "functionsFilterX");
            m_utils.setSectionCoverageStatistics("coveredFuncX", 88, 87, 86, 85, 84, 83);

            m_utils.setSectionProfiler("Yes", "CSV", "profilerExportX.csv", "Yes", "Yes", "No");
            m_utils.setSectionProfilerArea(true, "profiledFuncX", 
                                           new String[]{"12X"},  // netLow 
                                           null, // netHigh, 
                                           null, // grossLow, 
                                           null, // grossHigh, 
                                           null, // callOrOutsideLow, 
                                           null, // callOrOutsideHigh, 
                                           null, // periodLow, 
                                           new String[]{"15"}, // periodHigh, 
                                           "46", // hitsLow, 
                                           "68" // hitsUp
                    );

            // trace
            m_utils.setSectionTrace("Default (No)", "Text", "traceExportX.txt");

            // HIL
            m_utils.setSectionHIL(0, "DOUT_1", "1", true);
            m_utils.setSectionHIL(1, "AOUT_1", "0.48", false);

            // scripts
            m_utils.setSectionScripts("initTargetFunc", "a1, b1", 
                                      "initFunc", "c1, d1", 
                                      "endFunc", "e1, f1", 
                                      "restoreTargetFunc", "g1, h1",
                                      true);
            // options
            m_utils.setSectionOptions(0, "option\\one", "1", true);

            // the second derived test spec, which inherits values from the base test spec.
            m_utils.createNewDerivedTest(testID + " : " + funcName, "", "", "", 
                                         "rv == -6", "", true);

            TestSpecificationModel model = UITestUtils.getActiveModel();
            model.saveModelAs(fileName);
        }
        
        m_utils.selectTestCase(testID, funcName);
        
        m_utils.copyToClipboard();
        return m_utils.getTestSpecFromClipboard();
    }

    
    private CTestSpecification createSystemTCsForExportImportTest(String testID) throws Exception 
    {
        boolean createFileUncond = true;
        String fileName = TEST_OUT_DIR + "/exportImportTest_sys.iyaml";

        if (!createFileUncond  &&  m_utils.isFileCreatedToday(fileName)) {
            m_utils.loadTestSpecFromFile(fileName);
        } else {

            m_utils.discardExistingAndCreateNewProject();

            // function
            m_utils.createNewBaseSystemTest("", "rv == 4", false);
            m_utils.selectTestCase("/", "/");

            // meta
            m_utils.setTestID(testID);
            m_utils.setTestDescription("This is test desc.");

            // variables
            m_utils.setVarInit(0, "g_char1", "'x'");


            // now create two derived test specs - one with own values, the other one with inherited ones
            m_utils.selectTestCase(testID, "/");
            m_utils.createNewDerivedSystemTest(testID + " : /", "", "rv == -5", true);

            m_utils.setVarInit(1, "g_int1", "46");

            // stubs
            m_utils.setSectionStub("Func2", "Yes", "a,  b", "srv", "myStubScript", "6", "89", true);
            m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, "srv", "23", new String[]{"52", "43"});


            // the second derived test spec, which inherits values from the base test spec.
            m_utils.selectTestCase(testID, "/");
            m_utils.createNewDerivedSystemTest(testID + " : /", "", "rv == -6", true);

            TestSpecificationModel model = UITestUtils.getActiveModel();
            model.saveModelAs(fileName);
        }
        
        m_utils.selectTestCase(testID, "/");
        
        m_utils.copyToClipboard();
        return m_utils.getTestSpecFromClipboard();
    }

    
    // @Ignore
    @Test
    public void testStubsStepsTable() throws Exception {
    
        m_utils.discardExistingAndCreateNewProject();

        // first create and export a simple test spec
        String funcName = "add_int";
        
        // function
        m_utils.createNewBaseUnitTest(funcName, "-3, 7", "rv == 4", "rv", false);
        
        String stubbedFunc = "Func1";
        m_utils.setSectionStub(stubbedFunc, "Yes", "a", "srv", "stubScript", "", "", false);
        
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);

        setStepsTableContents();
        
        // set comments
        // for expect cell
        String expectNlComment = "nl comment A line 1\nline2";
        String expectEolComment = "eol comment A";
        m_ktableUtils.setComment(2, 3, expectNlComment, expectEolComment);

        // for assign cell
        String assignNlComment = "nl comment B line 1\nline2";
        String assignEolComment = "eol comment B";
        m_ktableUtils.setComment(3, 2, assignNlComment, assignEolComment);

        // move selection to make the first cells in a row visible
        m_utils.pressKey(SWT.END);
        m_bot.sleep(500); // make it easier to observe
        
        // for script params cell
        String scriptNlComment = "nl comment C line 1\nline2";
        String scriptEolComment = "eol comment C";
        m_ktableUtils.setComment(5, 3, scriptNlComment, scriptEolComment);
        
        // for 'next' cell
        String nextNlComment = "nl comment D line 1\nline2";
        String nextEolComment = "eol comment D";
        m_ktableUtils.setComment(7, 2, nextNlComment, nextEolComment);

        // verify
        m_utils.selectTestTreeNode("/ : add_int");
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();

        CTestBaseList steps = testSpec.getStub(stubbedFunc).getAssignmentSteps(true);
        CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(0));
        verifyStep(step, 
                   new String[]{"rv == 8", "ic == 'a'"}, 
                   new String[]{"rv", "g_char1"}, 
                   new String[]{"845", "&x"}, 
                   new String[]{"314", "-1"}, 
                   "1");
        verifyMapComment(step.getAssignments(true), "rv", assignNlComment, assignEolComment);
        String comment = step.getComment(CTestEvalAssignStep.EStepSectionIds.E_SECTION_NEXT_INDEX.swigValue(), 
                                         SpecDataType.KEY, 
                                         CommentType.NEW_LINE_COMMENT);
        nextNlComment = nextNlComment.replace("\n", "\n    # ");
        assertEquals("# " + nextNlComment, comment.trim());
        
        comment = step.getComment(CTestEvalAssignStep.EStepSectionIds.E_SECTION_NEXT_INDEX.swigValue(), 
                                  SpecDataType.VALUE, 
                                  CommentType.END_OF_LINE_COMMENT);
        assertEquals("# " + nextEolComment, comment.trim());

        
        step = CTestEvalAssignStep.cast(steps.get(1));
        verifyStep(step, 
                   new String[]{"rv == 9", "ic == &a"}, 
                   new String[]{"rv", "g_char1"}, 
                   new String[]{"846", "*counter"}, 
                   new String[]{"273", "-2"}, 
                   "2");
        verifySeqComment(step.getExpectedExpressions(true), 1, expectNlComment, expectEolComment);
        verifySeqComment(step.getScriptParams(true), 0, scriptNlComment, scriptEolComment);
        
        step = CTestEvalAssignStep.cast(steps.get(2));
        verifyStep(step, 
                   new String[]{"rv == 10", "ic, \"a\""}, 
                   new String[]{"rv", "g_char1"}, 
                   new String[]{"847", "6 * 9"}, 
                   new String[]{"1.41", "-3"}, 
                   "0");

        // return focus to table - it was given to test tree view
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
        m_ktableUtils.clickCell(5, 2);
        
        m_utils.pressKey(SWT.HOME);
        m_bot.sleep(500); // make it easier to observe
        
        // copy-paste single cell
        m_ktableUtils.clickCell(2, 3);
        m_utils.copyToClipboard();
        m_ktableUtils.clickCell(4, 4);
        m_bot.sleep(500);
        m_utils.pasteFromClipboard();
        m_bot.sleep(500);
        
        // copy-paste 2 cells
        m_utils.pressKey(SWT.SHIFT, SWT.ARROW_UP);
        m_bot.sleep(500);
        m_utils.copyToClipboard();
        m_bot.sleep(500);
        m_ktableUtils.clickCell(1, 2);
        m_bot.sleep(2000);
        m_utils.pasteFromClipboard();
        
        // cut-paste column
        m_bot.sleep(2000);
        m_ktableUtils.clickCell(2, 1);
        m_bot.sleep(500);
        m_utils.cutToClipboard();
        m_ktableUtils.clickCell(6, 1);
        m_bot.sleep(500);
        m_utils.pasteFromClipboard();
        
        // copy-delete-paste row
        m_bot.sleep(500);
        m_ktableUtils.clickCell(0, 4);
        m_ktableUtils.copySelection(0, 4);
        m_bot.sleep(500);
        m_ktableUtils.deleteRow(0, 4);
        m_ktableUtils.clickCell(0, 2);
        m_bot.sleep(500);
        m_ktableUtils.pasteRowsAbove(0, 2);
        m_bot.sleep(2000);
        
        // table contents should now be:
        /*
           0       1         2          3          4         5        6      
           
           
 0            | expect  | assign             | script params   | next
 1            |   0     |  rv      | g_char1 |   0     |    1  |
             -------------------------------------------------------------------------
 2         2  | rv == 10| 847      | ic == &a|  1.41   |  -3   |  ic, "a"  
 3         0  | *counter| 845      |  &x     |   314   |  -1   |  ic == 'a'
 4         1  | ic == &a| 846      | *counter|   273   |  -2   |  ic == &a 
          
        */

        m_utils.selectTestTreeNode("/ : add_int");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();

        steps = testSpec.getStub(stubbedFunc).getAssignmentSteps(true);
        step = CTestEvalAssignStep.cast(steps.get(0));
        verifyStep(step, 
                   new String[]{"rv == 10"}, 
                   new String[]{"rv", "g_char1"}, 
                   new String[]{"847", "ic == &a"}, 
                   new String[]{"1.41", "-3"}, 
                   "ic, \"a\"");
        
        step = CTestEvalAssignStep.cast(steps.get(1));
        verifyStep(step, 
                   new String[]{"*counter"}, 
                   new String[]{"rv", "g_char1"}, 
                   new String[]{"845", "&x"}, 
                   new String[]{"314", "-1"}, 
                   "ic == 'a'");
        
        step = CTestEvalAssignStep.cast(steps.get(2));
        verifyStep(step, 
                   new String[]{"ic == &a"}, 
                   new String[]{"rv", "g_char1"}, 
                   new String[]{"846", "*counter"}, 
                   new String[]{"273", "-2"}, 
                   "ic == &a");
        
        m_bot.sleep(500);
    }

    
    @Test
    public void testStubHitLimits() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();

        // first create and export a simple test spec
        String funcName = "funcForIntStubTest";
        
        // function
        m_utils.createNewBaseUnitTest(funcName, "", "retVal == 45", "retVal", false);
        
        String stubbedFunc1 = "stubbedFuncInt";
        m_utils.setSectionStub(stubbedFunc1, "Yes", "", "s1RetVal", "", 
                               "3", "3", false);
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
        m_ktableUtils.setSectionStubAssignments(0, "s1RetVal", "45", new String[0]);

        String stubbedFunc2 = "add_int";
        m_utils.setSectionStub(stubbedFunc2, "Yes", "", "", "", 
                               "", "", false);

        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "1");
        
        // under min limit 
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        m_ktableUtils.selectKTableWithContent(1, 0, "Stubbed functions");
        m_ktableUtils.selectDataCell(0, 0);
        
        m_utils.setMinMaxLimits("4", "6");
        m_utils.runAllTests("Test report for selected editor, 1 test(s), 0 group(s):\n" +
                            "- 1 test (100%) failed (invalid results)");
        
        // over max limit  
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        m_ktableUtils.selectKTableWithContent(1, 0, "Stubbed functions");
        m_ktableUtils.selectDataCell(0, 0);
        
        m_utils.setMinMaxLimits("1", "2");
        m_utils.runAllTests("Test report for selected editor, 1 test(s), 0 group(s):\n" +
                            "- 1 test (100%) failed (invalid results)");

        // limit for unhit stub set to 0 
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        m_ktableUtils.selectKTableWithContent(1, 0, "Stubbed functions");
        m_ktableUtils.selectDataCell(0, 0);
        
        m_utils.setMinMaxLimits("2", "4");
        m_utils.pressKey(SWT.TAB);

        m_ktableUtils.selectDataCell(0, 1);
        
        m_utils.setMinMaxLimits("0", "0");
        
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "1");

        //
        // limit for un-hit stub set to 1 
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        m_ktableUtils.selectKTableWithContent(1, 0, "Stubbed functions");

        m_ktableUtils.selectDataCell(0, 1);
        
        m_utils.setMinMaxLimits("1", "");
        
        m_utils.runAllTests("Test report for selected editor, 1 test(s), 0 group(s):\n" +
                            "- 1 test (100%) failed (invalid results)");
    }
    

    @Test
    public void testTestPointHitLimits() throws Exception {
        String testSpecStr = 
                        "  init:\n" +
                        "    g_char1: 0\n" +
                        "  func: [testPointTest, [11], rv]\n" +
                        "  testPoints:\n" +
                        "  - tpId: tp_id_4\n" +
                        "    location:\n" +
                        "      resourceName: testPointTest\n" +
                        "      line: 0\n" +
                        "      isSearch: true\n" +
                        "    hitLimits:\n" +
                        "      min: 1\n" +
                        "      max: 1\n" +
                        "    steps:\n" +
                        "    - expect:\n" +
                        "      - p1 == 11\n" +
                        "      assign:\n" +
                        "        g_char1: 12\n" +
                        "  - tpId: myComplexTestPoint\n" +
                        "    location:\n" +
                        "      resourceName: testPointTest\n" +
                        "      isSearch: true\n" +
                        "      searchContext: any\n" +
                        "      matchType: plain\n" +
                        "      pattern: myComplexTestPoint\n" +
                        "    hitLimits:\n" +
                        "      min: 9\n" +
                        "      max: 20\n" +
                        "    steps:\n" +
                        "    - expect:\n" +
                        "      - p1==11\n" +
                        "  - tpId: neverHitTestPoint\n" +
                        "    location:\n" +
                        "      resourceName: stubbedFuncInt\n" +
                        "    hitLimits:\n" +
                        "      min: 0\n" +
                        "  assert:\n" +
                        "    expressions:\n" +
                        "    - rv == 121\n" +
                        "    - g_char1 == 12\n";

        m_utils.discardExistingAndCreateNewProject();
        m_utils.copyTestSpecToClipboard(testSpecStr);
        m_utils.pasteClipboardToOverview();
        
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "1");

        m_utils.selectTestSection(UITestUtils.SEC_TEST_POINTS);
        m_utils.setMinMaxLimits("11", "12");

        m_utils.runAllTests("Test report for selected editor, 1 test(s), 0 group(s):\n" +
                "- 1 test (100%) failed (invalid results)");
    }

    
    @Test
    public void testLoggingWizardInMetaSection() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        // first create and export a simple test spec
        String funcName = "add_int";
        
        // function
        m_utils.createNewBaseUnitTest(funcName, "first, second", "retVal == 45", "retVal", false);
        m_utils.setVar(0, "int", "first", "41");
        m_utils.setVar(1, "int", "second", "4");
        
        m_utils.selectTestSection(UITestUtils.SEC_META);
        
        m_utils.clickButtonWTooltip(MetaSpecEditor.EDIT_LOG_ITEMS_BTN_TOOLTIP);

        m_utils.activateShell(MetaSpecEditor.LOG_ITEMS_DLG_TITLE);
        
        SWTBotTable table = m_bot.table();
        table.getTableItem(0).toggleCheck();
        
        m_utils.clickButton("OK");
        
        m_utils.selectTestCase("/", "add_int");
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        CTestLog log = testSpec.getLog(true);
        CSequenceAdapter exprs = log.getExpressions(ESectionsLog.E_SECTION_BEFORE, true);
        
        assertEquals(1, exprs.size());
        assertEquals("first", exprs.getValue(0));
        
        // remove existing and add next one 
        m_utils.clickButtonWTooltip(MetaSpecEditor.EDIT_LOG_ITEMS_BTN_TOOLTIP);

        m_utils.activateShell(MetaSpecEditor.LOG_ITEMS_DLG_TITLE);
        
        table = m_bot.table();
        table.getTableItem(0).toggleCheck();
        table.getTableItem(1).toggleCheck();
        
        m_utils.clickButton("OK");
        
        m_utils.selectTestCase("/", "add_int");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();
        
        log = testSpec.getLog(true);
        exprs = log.getExpressions(ESectionsLog.E_SECTION_BEFORE, true);
        
        assertEquals(1, exprs.size());
        assertEquals("second", exprs.getValue(0));
        
    }
    

    @Test
    public void testLoggingWizardInStubsAndTPSections() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();

        String testSpecStr =
                        " func: [funcForIntStubTest, [], retVal]\n" +
                        " stubs:\n" +
                        " - stubbedFunc: stubbedFuncInt\n" +
                        "   retValName: s1RetVal\n" +
                        "   log:\n" +
                        "     before:\n" +
                        "     - g_char1\n" +
                        "     after:\n" +
                        "     - iCounter\n" +
                        "   assignSteps:\n" +
                        "   - assign:\n" +
                        "       s1RetVal: 45\n" +
                        " testPoints:\n" +
                        " - tpId: tp_id_4\n" +
                        "   location:\n" +
                        "     resourceName: testPointTest\n" +
                        "     line: 0\n" +
                        "     isSearch: true\n" +
                        "   log:\n" +
                        "     before:\n" +
                        "     - p1\n" +
                        "     - x\n" +
                        "     after:\n" +
                        "     - p1\n" +
                        "   steps:\n" +
                        "   - expect:\n" +
                        "     - p1 == 11\n" +
                        "     assign:\n" +
                        "       g_char1: 12\n" +
                        " assert:\n" +
                        "   expressions:\n" +
                        "   - retVal == 45";

        m_utils.copyTestSpecToClipboard(testSpecStr);
        m_utils.pasteClipboardToOverview();

        // change logged items in stub
        m_utils.selectTestSection(UITestUtils.SEC_STUBS);
        
        m_utils.clickButtonWTooltip(LoggingControls.WIZARD_BTN_TOOLTIP);

        m_utils.activateShell(MetaSpecEditor.LOG_ITEMS_DLG_TITLE);
        
        SWTBotTable table = m_bot.table();
        // remove exiting and add step item
        table.getTableItem(0).toggleCheck();
        table.getTableItem(1).toggleCheck();
        
        m_utils.clickButton("OK");
        
        // change logged items in test point
        m_utils.selectTestSection(UITestUtils.SEC_TEST_POINTS);
        
        m_utils.clickButtonWTooltip(LoggingControls.WIZARD_BTN_TOOLTIP);

        m_utils.activateShell(MetaSpecEditor.LOG_ITEMS_DLG_TITLE);
        
        table = m_bot.table();
        table.getTableItem(1).toggleCheck(); // add the second item
        
        m_utils.clickButton("OK");

        m_utils.selectTestCase("/", "funcForIntStubTest");
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        CTestStub stub = testSpec.getStub("stubbedFuncInt"); 
        CTestLog log = stub.getLogConfig(true);
        CSequenceAdapter exprs = log.getExpressions(ESectionsLog.E_SECTION_AFTER, true);
        
        assertEquals(1, exprs.size());
        assertEquals("s1RetVal", exprs.getValue(0));

        CTestPoint tp = testSpec.getTestPoint("tp_id_4"); 
        log = tp.getLogConfig(true);
        exprs = log.getExpressions(ESectionsLog.E_SECTION_AFTER, true);
        
        assertEquals(2, exprs.size());
        assertEquals("p1", exprs.getValue(0));
        assertEquals("g_char1", exprs.getValue(1));
    }
    
    
    @Test
    public void testPersistentVars() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        // first create and export a simple test spec
        String funcName = "funcTestIntArray1";
        
        // function
        m_utils.createNewBaseUnitTest(funcName, "per_iarr", "rv", "rv", false);
        m_utils.setPersistentVar(0, "per_iarr", "int[10]");
        m_utils.setVar(0, "", "per_iarr[0]", "123");
        m_utils.setVar(1, "", "per_iarr[1]", "1823");
        m_utils.setSectionExpected("", null, "rv[0] == 1000000", "per_iarr[1] == 1823");
       
        m_utils.createNewBaseUnitTest("funcTestIntArray2", "per_iarr, per_iarr, per_iarr", 
                                      "rv", "rv", false);
        m_utils.setPersistentVarsInheritance();
        m_utils.setPersistentVar(0, "per_idx", "long");
        m_utils.setVar(0, "", "per_iarr[0]", "123");
        m_utils.setVar(1, "", "per_iarr[1]", "1823");
        m_utils.setVar(1, "", "per_idx", "-3");
        m_utils.setSectionExpected("", null, "rv[2] == 1000003", 
                                   "per_iarr[0] == 1000001",
                                   "per_iarr[1] == -1000002",
                                   "per_iarr[2] == 1000003");

        m_utils.createNewBaseUnitTest(funcName, "per_iarr", "rv", "rv", false);
        m_utils.setPersistentVarsInheritance();
        m_utils.setDeletedPersistVar(0, "per_idx");
        m_utils.setVar(0, "", "per_iarr[1]", "1825");
        m_utils.setSectionExpected("", null, 
                                   "rv[0] == 1000000", 
                                   "per_iarr[1] == 1825",
                                   "per_idx == -3");
       
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "3");
    }
    
    
    // tests logging of target variables (controls in Meta section).
    // @Ignore
    @Test
    public void testLogging() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        m_utils.setPropertiesEvaluator(true, true, false, true, true, true, "1e-5", 
                                       true, false, true, false, true);
        
        // first create and export a simple test spec
        String funcName = "add_int";
        
        // init global vars first, so that the test is repeateble and can be run in any order
        m_utils.createNewBaseUnitTest(funcName, "45, 78", "rv == 123", "rv", false);
        m_utils.setVarInit(0, "g_char1", "0"); 
        m_utils.setVarInit(1, "g_char2", "0");
        
        // function
        m_utils.createNewBaseUnitTest(funcName, "45, 78", "rv == 123", "rv", false);

        m_utils.setTestCaseLogging("g_char1, g_char2", "rv");
       
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "2");
        
        m_bot.button("View").click();
        
        m_utils.waitForShell("Log", 2000, true);
        
        // store data and compare alter, as possible assert error terminates 
        // test execution and then log dialog remains opened 
        SWTBotKTable ktable = m_ktableUtils.selectKTable(0);
        
        String varName_1 = ktable.cell(0, 1);
        String value_1 = ktable.cell(1, 1);

        String varName_2 = ktable.cell(0, 2);
        String value_2 = ktable.cell(1, 2);

        ktable = m_ktableUtils.selectKTable(1);
        String varNameT2_1 = ktable.cell(0, 1);
        String valueT2_1 = ktable.cell(1, 1);
        
        m_bot.button("OK").click();
        m_utils.waitWhileShellIsOpened("Log", 2000);
        
        assertEquals("g_char1", varName_1);
        assertEquals("\\x00 (0x00) (0)", value_1);
        
        assertEquals("g_char2", varName_2);
        assertEquals("\\x00 (0x00) (0)", value_2);
        
        assertEquals("rv", varNameT2_1);
        assertEquals("0x0000007B (123)", valueT2_1);
        
    }
    
    
    // @Ignore
    @Test
    public void testPreConditions() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        // first create and export a simple test spec
        String funcName = "add_int";
        
        // test for OK
        // init global vars first, so that the test is repeateble and can be run in any order
        m_utils.createNewBaseUnitTest(funcName, "45, 78", "rv == 123", "rv", false);
        m_utils.setVarInit(0, "g_char1", "0"); 
        m_utils.setVarInit(1, "g_char2", "0");
        
        m_utils.createNewBaseUnitTest(funcName, "45, 78", "rv == 123", "rv", false);

        m_utils.setSectionPreCondition(new String[]{"g_char1 == 0", "g_char2 == 0"});
       
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "2");

        // test for error
        m_utils.createNewBaseUnitTest(funcName, "45, 78", "rv == 123", "rv", false);

        m_utils.setSectionPreCondition(new String[]{"g_char1 == 0", "g_char2 == 2"});
        
        m_utils.runAllTests("Test report for selected editor, 3 test(s), 0 group(s):\n"
                            + "- 2 tests (67%) completed successfully\n"
                            + "- 1 test (33%) failed (invalid results)");
    }
    

    // @Ignore
    @Test
    public void testHostVars() throws Exception {
        //   default return value var
        //   values in variables section
        //   function parameters
        //   expressions
        m_utils.discardExistingAndCreateNewProject();

        String funcName = "add_int";
        
        // function
        m_utils.createNewBaseUnitTest(funcName, "${p1}, ${p2}", "${_rv} == 37", "", false);
        m_utils.setVar(0, "", "${p1}", "18");
        m_utils.setVar(1, "", "${p2}", "${p1} + 1");
        m_utils.setVar(2, "", "${stackPattern}", "0xa5");
        
        //   pre-condition exprs.
        //   stub parameters and stub steps (the same code is used for test-points)
        //   script parameters
        //   max stack used
        //   option values
        m_utils.createNewBaseUnitTest("funcForIntStubTest", 
                                      "", "", "", false);
        m_utils.setVar(0, "", "${expectedRetVal}", "45");
        m_utils.setVar(1, "", "${stackUsage}", "100");

        m_utils.setSectionPreCondition(new String[]{"${p2} == 19"});
        m_utils.setSectionStub("stubbedFuncInt", "Yes", "", "s1RetVal", "targetInitAction", "", "", false);
        m_utils.setStubTestPointLogging("g_char1", "iCounter");
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
        m_ktableUtils.setSectionStubAssignments(0, "s1RetVal", "${expectedRetVal}", 
                                                new String[]{"${stackPattern}"});
         m_utils.setSectionScripts("", "", 
                                  "targetInitAction", "${p2}", 
                                  "", "",  
                                  "", "",  
                                  false);
        m_utils.setSectionExpected("${stackUsage}", null, "${_rv} == ${expectedRetVal}");
        m_utils.setSectionOptions(0, "/IDE/Debug.StackUsage.Pattern", "${stackPattern}", false);
        
        // test env vars in analyzer doc and exports
        // test scope in derived tests
        m_utils.selectTestCase(0);
        m_utils.createNewDerivedTest("/ : " + funcName, "", "Func1", "${p1}", "${_rv} == 28", "", false);
        m_utils.setSectionAnalyzer("Start", "\\tmp\\${_env_OS}-cvrg.trd", "Write",
                                   "No", "No", "No", "No", 
                                   "", false);
        m_utils.setSectionCoverageTestCase("Yes", "HTML", "/tmp/cvrg_${_env_OS}.html", "",
                                   "No",
                                   "No", "No", 
                                   "Yes", "No", 
                                   "No", "No", 
                                   "No", "", "");
        m_utils.setSectionCoverageStatsMeasureAll(true);
        m_utils.setPropertiesScriptConfig("sampleTestExtensions");        
        
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "3");
        Files.exists(Paths.get("/tmp/Windows_NT-cvrg.trd"));
        Files.exists(Paths.get("/tmp/cvrg_Windows_NT.html"));
        
        // Errors
        // test assignment to reserved vars
        m_utils.selectTestCase(0);
        m_utils.setVar(0, "", "${_rv}", "18");
        m_utils.runSelectedTest("Test report for selected editor, 1 test(s), 0 group(s):\n"
                               + "- 1 test (100%) with error (failed execution)");
        
        m_utils.checkStatusView(1, "Description:\n"
                + "  class: IllegalArgumentException\n"
                + "  msg: Assignments to reserved host variables is not allowed! "
                  + "Remove leading underscore to create user-defined host variable.  [ctestcase.cpp, 942]\n"
                + "    hostVar: ${_rv}");
    }
    

    // @Ignore
    @Test
    public void testDiagrams() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();

        // configure report to define output dir for diagrams
        Path reportFile = Paths.get(TEST_OUT_DIR + "/diagTest.xml").toAbsolutePath();
        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TEST, 
                                       "Configure Test Report ...", 
                                       "Configure test reports");
        m_utils.waitForConnectionDialog();
        m_utils.setDialogSaveTestReport("XML", 
                                        connect.getDEFAULT_XSLT_NAME(),
                                        connect.getBUILT_IN_XSLT_PREFIX() + " " + "blue.css",
                                        false,  // no embedding of XSLT
                                        true, // full report
                                        reportFile.toString(),
                                        true, false,  
                                        "ic5000", "test of diagrams", true);

        // Func4 was a bad example, since floating point calls other functions
        // and labels depending on target (different for Cortec M3 and M4 for example)
        
        m_utils.createNewBaseUnitTest("CPU_Recursion", "", "g_enumA == TE_FINISHED", "", false);
        m_utils.setVar(0, "", "g_enumA", "TE_INIT");
        createAllDiagrams();
        
        // funcTestStubsNested
        m_utils.createNewBaseUnitTest("funcTestStubsNested", "", "rv == 456", "rv", false);
        createAllDiagrams();

        // replace extension svg with png to test also this type of images
        m_ktableUtils.clickDataCell(4,  1);
        m_utils.pressKey(SWT.F2);
        m_utils.pressKey(SWT.ARROW_RIGHT);
        m_utils.pressKey(SWT.BS);
        m_utils.pressKey(SWT.BS);
        m_utils.pressKey(SWT.BS);
        m_utils.typeText("png"); //('p');m_utils.pressKey('n');m_utils.pressKey('g');
        m_utils.clickButton("Create");
        m_utils.waitForProgressDialog();
        
        // special test script
        Path script = Paths.get("../si.isystem.uitest/testOutput/flowChartTest.py");
        Path absPath = script.toAbsolutePath();
        
        m_ktableUtils.addRowsUntil(6);
        m_ktableUtils.selectDataItemInCombo(1,  4, "cc");
        m_ktableUtils.setDataCell(2, 4, absPath.toString());
        m_utils.clickButton("Create");
        m_utils.waitForProgressDialog();

        String[] files = new String[]{"-CPU_Recursion-runtimeCallGraph.svg.call",
                        "-CPU_Recursion-staticCallGraph.svg.statcallg",
                        "-CPU_Recursion-flowChart.svg.flow",
                        "-CPU_Recursion-sequence.svg.seq",
                        "-funcTestStubsNested-runtimeCallGraph.svg.call",
                        "-funcTestStubsNested-staticCallGraph.svg.statcallg",
                        "-funcTestStubsNested-custom.svg.flow",
                        "-funcTestStubsNested-flowChart.svg.flow",
                        "-funcTestStubsNested-sequence.svg.seq"};

        // use unordered comparison, as set() in Python does not maintain order
        // between runs even for the same content.
        for (String file : files) {
            String referenceFile = file + ".reference";
            String []diff = m_utils.compareUnorderedFiles(TEST_OUT_DIR + "/" + file, 
                                                          TEST_OUT_DIR + "/" + referenceFile);
            if (diff != null) {
                System.out.println("File: " + file);
                System.out.println("Reference File: " + referenceFile);
                System.out.println("Diffs: " + StringUtils.join(diff));
                // if 'diff != null' it contains different lines, so the next assert
                // will terminate and show the difference at the same time!
                assertEquals(diff[0], diff[1]);
            }
        }
    }


    @Test
    public void testNewTestWizard() throws Exception {

        m_utils.discardExistingAndCreateNewProject();
        m_utils.setAnalyzerDefaultName("${testId}.trd");
        m_utils.connect();
        
        createNewTest(false);
        createNewTest(true);
    }
    
    private void createNewTest(boolean isDerivedTest) throws Exception {
        
        m_utils.openNewTestWizard(isDerivedTest);
        String funcName = "funcTestStubs";
        String retValName = "rv";
        String expr = "rv->m_pi == 1123";
        
        m_utils.fillDialogNewTestPage1(false, "", funcName, "23, name", 
                                       expr, retValName, false, isDerivedTest, false);
        
        m_utils.clickButton("Next >");  // Functions page
        m_bot.sleep(1000);
        m_utils.selectKTableWithContent(1, 0, "Stub");
 
        m_utils.fillDialogNewTestFuncRow(0, 
                                         false, null, 
                                         false, null, 
                                         true, "tp1", 
                                         true, "10", "20", 
                                         true, "10", "10s");
        m_utils.fillDialogNewTestFuncRow(4, 
                                         false, null, 
                                         true, "repl_stubbedFuncInt", 
                                         false, null, 
                                         false, null, null, 
                                         false, null, null);
        m_utils.fillDialogNewTestFuncRow(6, 
                                         true, "1123", 
                                         false, null, 
                                         false, null, 
                                         false, null, null, 
                                         false, null, null);
        
        m_utils.clickButton("Next >");
        m_utils.selectKTableWithContent(2, 0, "Create");
        
        m_utils.setKTableCellContent(1, 1, "char *");
        
        m_utils.clickButton("Next >");
        
        m_bot.sleep(500); // don't know why this delay is required - without it
        // it seems m_bot tries to access trees in main window, which triggers an exception???
        // Combo box above is modified without problem, but tree can not be reached???
        // this does not help: ICondition condition = Conditions.shellIsActive(StructMembersSelectionDialog.DLG_TITLE);
        //                     m_utils.waitUntil(condition, 3000, true);
        
        SWTBotTree tree = m_bot.tree();
        SWTBotTreeItem subtree = tree.getTreeItem("*rv: struct_td");
        subtree.toggleCheck();
        subtree.getNode("m_pi: long *").toggleCheck();
        
        m_bot.button("Finish").click();
        
        if (!isDerivedTest) {
            m_utils.selectTestCase("/", funcName);
        } else {
            m_utils.selectDerivedTestCase("/ : " + funcName, "/ : " + funcName);
        }
        
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals("locals:\n" +
                "  name: char *\n" +
                "init:\n" +
                "  '*name': 0\n" +
                "func:\n" +
                "  func: funcTestStubs\n" +
                "  params:\n" +
                "  - 23\n" +
                "  - name\n" +
                "  retVal: rv\n" +
                "stubs:\n" +
                "- stubbedFunc: stubbedFunc3\n" +
                "  retValName: stubRV\n" +
                "  assignSteps:\n" +
                "  - assign:\n" +
                "      stubRV: 1123\n" +
                "userStubs:\n" +
                "- func: stubbedFunc1\n" +
                "  replacementFunc: repl_stubbedFuncInt\n" +
                "testPoints:\n" +
                "- tpId: tp1\n" +
                "  location:\n" +
                "    resourceType: function\n" +
                "    resourceName: funcTestStubs\n" +
                "    isSearch: true\n" +
                "assert:\n" +
                "  expressions:\n" +
                "  - rv->m_pi == 1123\n" +
                "  - (*rv).m_pi\n" +
                "  - '*name'\n" +
                "analyzer:\n" +
                "  runMode: start\n" +
                "  document: ${testId}.trd\n" +
                "  coverage:\n" +
                "    isActive: true\n" +
                "    statistics:\n" +
                "    - func: funcTestStubs\n" +
                "      code: 10\n" +
                "      both: 20\n" +
                "  profiler:\n" +
                "    isActive: true\n" +
                "    codeAreas:\n" +
                "    - name: funcTestStubs\n" +
                "      grossTime:\n" +
                "        total:\n" +
                "        - 10\n" +
                "        - 10s\n",
          testSpec.toString());
    }

    
    @Test
    public void testScriptsFromToolsMenu() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject(UITestUtils.SDK_TEST_PROJ_PATH + 
                                                   "/" + UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        m_utils.connect();
        m_utils.setPropertiesScriptConfig("sampleTestExtensions");
        m_utils.refreshSymbols();
        
        m_bot.menu(UITestUtils.MENU_TOOLS).menu("- isys_cmd_printHi()").click();
        m_bot.sleep(1000);
        assertTrue(m_utils.getStatusViewText(),
                   m_utils.getStatusViewText().contains("\n  Script function type: customScriptMethod\n" +
                   "  stdout:\n" +
                   "    HI! Script method executed, parameter ="));
        
        m_bot.menu(UITestUtils.MENU_TOOLS).menu("simplePrintForTest.py").click();
        m_bot.sleep(1000);
        
        String result = m_utils.getStatusViewText();
        // String[] lines = result.split("\n");
        assertTrue(result.contains("  External script started"));
    }

    @Test
    public void testScriptForReportInfo() throws Exception {
        m_utils.discardExistingAndCreateNewProject(TEST_OUT_DIR + '/' +
                                                   UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        m_utils.connect();
        m_utils.setPropertiesScriptConfig("sampleTestExtensions");
        m_utils.refreshSymbols();
        
        m_utils.createNewBaseUnitTest("add_int", "9, 34", "rv == 43", "rv", false);
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "1");
        
        String reportFile = "reportWCustomData.xml";
        m_utils.saveReport(true, reportFile, false);
        
        Charset charset = Charset.forName("UTF-8");
        Path filePath = Paths.get(TEST_OUT_DIR + '/' + reportFile);
        boolean isBL = false, isAR = false;
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<pair><key>bootloaderRev</key><value>1.05g</value></pair>")) {
                    isBL = true;
                }
                if (line.contains("<pair><key>_appRev</key><value>12355</value></pair>")) {
                    isAR = true;
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }    
        
        assertTrue("Missing string 'bootloader' in report!", isBL);
        assertTrue("Missing string '_appRev' in report!", isAR);
        
        assertTrue("Missing text from script f. after report save!",
                   m_utils.getStatusViewText().contains("afterReportSave() called with reportConfig"));
    }
    
    
    @Test
    public void testWizardForLocalArrays() throws Exception {
        m_utils.discardExistingAndCreateNewProject(TEST_OUT_DIR + '/' +
                                                   UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        m_utils.connect();
        String funcName = "add_int";
        m_utils.createNewBaseUnitTest(funcName, "9, 34", "rv == 43", "rv", false);
        m_utils.setVarDeclaration(0, "arr", "int[3]");
        m_bot.buttonWithId(SWTBotConstants.VAR_MEMBERS_WIZARD).click();
        m_utils.activateShell(StructMembersSelectionDialog.DLG_TITLE);
        m_utils.enterComboWLabel("Variable:", "arr");
        m_bot.sleep(500); // wait until UI is refreshed
        m_utils.clickButton("OK");
        
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
    
        assertEquals("locals:\n" +
                "  arr: int[3]\n" +
                "init:\n" +
                "  arr[0]: 0\n" +
                "  arr[1]: 0\n" +
                "  arr[2]: 0\n" +
                "func:\n" +
                "  func: add_int\n" +
                "  params:\n" +
                "  - 9\n" +
                "  - 34\n" +
                "  retVal: rv\n" +
                "assert:\n" +
                "  expressions:\n" +
                "  - rv == 43\n", testSpec.toString());
    }
      
    
    @Test
    public void testInvalidCharReplacementInTestID() throws Exception {
        m_utils.discardExistingAndCreateNewProject(TEST_OUT_DIR + '/' +
                                                   UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        
        String funcName = "add_int";
        m_utils.createNewBaseUnitTest(funcName, "9, 34", "rv == 43", "rv", false);
        m_utils.selectTestSection(UITestUtils.SEC_META);
        m_utils.setSectionMeta(true, "Unit", "my id+3", "desc", "");
        
        String expectedId = "my_id_3";
        m_utils.selectTestCase(expectedId, funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals(expectedId, testSpec.getId());
    }
      
    @Test
    public void testVarsWizard() throws Exception {
        m_utils.discardExistingAndCreateNewProject(TEST_OUT_DIR + '/' +
                                                   UITestUtils.DEFAULT_TEST_IYAML_FILE_NAME);
        m_utils.connect();
        m_utils.refreshSymbols();
        
        String funcName = "Func4";
        m_utils.createNewBaseUnitTest(funcName, "9, pC, 1, 4", "rv == 43", "rv", false);
        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);

        m_bot.buttonWithId(SWTBotConstants.GLOBAL_FUNCTIONS_WIZARD).click();
        m_utils.activateShell(NewTCVariablesPage.PAGE_TITLE);
        m_utils.clickButton("OK");
        
        m_utils.selectTestCase("/", funcName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        
        assertEquals("locals:\n" +
                "  pC: unsigned char *\n" +
                "init:\n" +
                "  '*pC': 0\n" +
                "func:\n" +
                "  func: Func4\n" +
                "  params:\n" +
                "  - 9\n" +
                "  - pC\n" +
                "  - 1\n" +
                "  - 4\n" +
                "  retVal: rv\n" +
                "assert:\n" +
                "  expressions:\n" +
                "  - rv == 43\n", testSpec.toString());
    }
      
//    @Test
//    public void testExportOfSystemTest() throws Exception {
//      
//        m_utils.connect();
//        m_utils.discardExistingAndCreateNewProject();
//        m_utils.createNewBaseSystemTest("", "g_char1 == 0", true);
//        
//        m_utils.openExportDialog();
//        
//    }

    
    
    
    
    private void createAllDiagrams() throws Exception {
        // flow chart
        m_utils.selectTestSection(UITestUtils.SEC_DIAGRAMS);
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_DIAGRAMS_KTABLE);
        m_ktableUtils.addRowsUntil(2);
        m_ktableUtils.clickDataCell(1, 0);
        m_utils.clickButton("Create");
        // m_utils.waitForConnectionDialog();
        m_utils.waitForProgressDialog();

        // call graph
        m_ktableUtils.addRowsUntil(3);
        m_ktableUtils.selectDataItemInCombo(1,  1, "c");
        m_utils.clickButton("Auto-configure profiler");
        m_utils.waitForShellAndClickOK("Auto-configure profiler", 2000, true);
        m_utils.runSelectedTest("All tests for selected editor completed successfully!\n"
                              + "Number of tests: 1");
        m_ktableUtils.clickDataCell(0,  1);
        m_utils.clickButton("Create");
        m_utils.waitForProgressDialog();
        
        // sequence diagram
        m_ktableUtils.addRowsUntil(4);
        m_ktableUtils.selectDataItemInCombo(1,  2, "s");
        m_ktableUtils.clickDataCell(0,  2);
        m_utils.clickButton("Create");
        m_utils.waitForProgressDialog();
        
        // static call graph
        m_ktableUtils.addRowsUntil(5);
        m_ktableUtils.selectDataItemInCombo(1,  3, "ss");
        m_ktableUtils.clickDataCell(0,  3);
        m_utils.clickButton("Create");
        m_utils.waitForProgressDialog();
    }
    
    
    private void setStepsTableContents() {
        
        /*
 0            | expect             | assign             | script params   | next
 1            |   0     |  1       |  rv      | g_char1 |   0     |    1  |
             -------------------------------------------------------------------------
 2         0  | rv == 8 | ic == 'a'| 845      | &x      |   314   | -1    |  1
 3         1  | rv == 9 | ic == &a | 846      | *counter|   273   | -2    |  2
 4         2  | rv == 10| ic, "a"  | 847      | 6 * 9   |  1.41   | -3    |  0
          
                   0       1         2          3          4         5        6    
        */
        
        m_ktableUtils.addRowsUntil(4);
        // ROW 1
        // expect
        m_ktableUtils.setDataCell(0, 0, "rv == 8");
        // assign
        m_ktableUtils.addMapColumn(2, 0, "rv"); 
        m_ktableUtils.setDataCell(1, 0, "845"); 
        // script params
        m_ktableUtils.setDataCell(2, 0, "314");
        // next
        m_ktableUtils.setDataCell(3, 0, "1");
        
        // ROW 2
        // expect
        m_ktableUtils.setDataCell(0, 1, "rv == 9");
        // assign
        m_ktableUtils.setDataCell(1, 1, "846");
        // script params
        m_ktableUtils.setDataCell(2, 1, "273");
        // next
        m_ktableUtils.setDataCell(3, 1, "2");
        
        // ROW 3
        // expect
        m_ktableUtils.setDataCell(0, 2, "rv == 10");
        // assign
        m_ktableUtils.setDataCell(1, 2, "847");
        // script params
        m_ktableUtils.setDataCell(2, 2, "1.41");
        // next
        m_ktableUtils.setDataCell(3, 2, "3");
        

        // add expect column
        m_ktableUtils.addSeqColumn(1, 1); 
        m_ktableUtils.setDataCell(1, 0, "ic == 'a'");
        m_ktableUtils.setDataCell(1, 1, "ic == &a");
        m_ktableUtils.setDataCell(1, 2, "ic, \"a\"");
        
        // add assign column
        m_ktableUtils.addMapColumn(3, 1, "g_char1"); 
        m_ktableUtils.setDataCell(3, 0, "&x");
        m_ktableUtils.setDataCell(3, 1, "*counter");
        m_ktableUtils.setDataCell(3, 2, "6 * 9");

        // add script params column
        m_ktableUtils.addSeqColumn(5, 1); 
        m_bot.sleep(500);
        m_ktableUtils.setDataCell(5, 0, "-1");
        m_ktableUtils.setDataCell(5, 1, "-2");
        m_ktableUtils.setDataCell(5, 2, "-3");
        
        // columnm 'next'
        m_utils.pressKey(SWT.END);
        m_ktableUtils.setDataCell(6, 2, "0");
        
        m_bot.sleep(1000);

        // move selection to make the first cells in a row visible
        m_utils.pressKey(SWT.HOME);
        m_bot.sleep(500); // make it easier to observe
        
        // TODO test undo and copy/pasting of different types of columns
    }
    

    private void verifyStep(CTestEvalAssignStep step,
                            String[] expect,
                            String[] assignKeys,
                            String[] assignValues,
                            String[] scriptParams,
                            String next) {

        // expect
        CSequenceAdapter seq = step.getExpectedExpressions(true);
        assertEquals(expect.length, seq.size());
        for (int i = 0; i < expect.length; i++) {
            assertEquals(expect[i], seq.getValue(i));
        }

        // assignments
        CMapAdapter map = step.getAssignments(true);
        assertEquals(assignKeys.length, map.size());
        for (int i = 0; i < assignKeys.length; i++) {
            assertEquals(assignValues[i], map.getValue(assignKeys[i]));
        }

        // script params
        seq = step.getScriptParams(true);
        assertEquals(scriptParams.length, seq.size());
        for (int i = 0; i < scriptParams.length; i++) {
            assertEquals(scriptParams[i], seq.getValue(i));
        }

        // next
        assertEquals(next, step.getStepIdx());
    }


    private void verifySeqComment(CSequenceAdapter seq,
                                  int idx,
                                  String expectNlComment,
                                  String expectEolComment) {
        
        String nlComment = seq.getComment(CommentType.NEW_LINE_COMMENT, idx);
        expectNlComment = expectNlComment.replaceAll("\n", "\n    # ");
        assertEquals("    # " + expectNlComment + '\n', nlComment);
        
        String eolComment = seq.getComment(CommentType.END_OF_LINE_COMMENT, idx);
        assertEquals("    # " + expectEolComment + '\n', eolComment);
        
    }


    private void verifyMapComment(CMapAdapter map,
                                  String key,
                                  String expectNlComment,
                                  String expectEolComment) {
        
        String nlComment = map.getComment(CommentType.NEW_LINE_COMMENT, key);
        expectNlComment = expectNlComment.replaceAll("\n", "\n    # ");
        assertEquals("    # " + expectNlComment + '\n', nlComment);
        
        String eolComment = map.getComment(CommentType.END_OF_LINE_COMMENT, key);
        assertEquals("    # " + expectEolComment + '\n', eolComment);
        
    }


    private void verifyVars(CTestSpecification testSpec,
                            int rowIdx,
                            String varName,
                            String value) {
        StrVector keys = testSpec.getInitKeys(); 
        StrVector values = new StrVector();
        testSpec.getInitValues(values);
        assertEquals(varName, keys.get(rowIdx));
        assertEquals(value, values.get(rowIdx));
    }


    private CTestStub verifyStub(CTestSpecification testSpec,
                                 String stubbedFuncName,
                                 boolean isActive,
                                 String params,
                                 String retValName,
                                 String scriptFunc) {
        
        CTestStub stub = testSpec.getStub(stubbedFuncName);
        assertEquals(stub.isActive(), isActive);
        StrVector paramsV = new StrVector();
        stub.getParamNames(paramsV);
        assertEquals(params, CYAMLUtil.strVector2Str(paramsV));
        assertEquals(retValName, stub.getRetValName());
        assertEquals(scriptFunc, stub.getScriptFunctionName());

        return stub;
    }


    private void verifyHIL(CTestSpecification testSpec,
                           int rowIdx,
                           String param,
                           String value) {
        CTestHIL hil = testSpec.getHIL(true);
        StrVector keys = hil.getHILParamKeys();
        StrVector values = new StrVector();
        hil.getHILParamValues(values);
        assertEquals(param, keys.get(rowIdx));
        assertEquals(value, values.get(rowIdx));
    }


    // @Ignore
    @Test
    public void testAnalyzerFileNameWizard() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();
        m_utils.setInitSequence();

        m_utils.setPropertiesAutoIdFormat("test-${_seq}");
        // set analyzer doc file name pattern
        String analDocFileName = "${_testId}-${_uid}.trd";
        m_utils.setPropertiesAnalDocFileName(analDocFileName);
        
        String funcName = "complexFunction";
        
        // create test caases
        // test-0
         m_utils.createNewBaseUnitTest(funcName, "0, 0, 0", "${_rv} == 0", "", true);
//        m_utils.activateAnalyzer();
//        m_utils.activateCoverage();
        
        // test-1
        String baseIdFuncName = "test-0 : " + funcName;
        m_utils.createNewDerivedTest(baseIdFuncName, "", 
                                     funcName, "0, 1, 0", "${_rv} == 1", "", true);
        m_utils.activateAnalyzer();
        m_utils.setSectionAnalyzerSaveAfterTest(true);
        m_utils.activateCoverage();
        m_utils.setSectionCoverageStatsMeasureAll(true);

        // test-2
        m_utils.createNewDerivedTest(baseIdFuncName, "", funcName, "1, 0, 0", "${_rv} == 4", "", true);
//        m_utils.activateAnalyzer();
//        m_utils.activateCoverage();

        // test-3
        String derivedIdFuncName = "test-2 : " + funcName;
        m_utils.createNewDerivedDerivedTest(baseIdFuncName, derivedIdFuncName, "",
                                            funcName, "1, 1, 0", "${_rv} == 4", "", true);
        m_utils.activateAnalyzer();
        m_utils.setSectionAnalyzerSaveAfterTest(true);
        m_utils.activateCoverage();
        m_utils.setSectionCoverageStatsMeasureAll(true);
        
        // test-4  - will derive analyzer section
        m_utils.createNewDerivedDerivedTest(baseIdFuncName, derivedIdFuncName, "",
                                      funcName, "1, 1, 0", "${_rv} == 4", "", true);

        // test-5
        m_utils.createNewDerivedDerivedTest(baseIdFuncName, derivedIdFuncName, "",
                                      funcName, "1, 1, 0", "${_rv} == 4", "", true);
        m_utils.activateAnalyzer();
        m_utils.setSectionAnalyzerSaveAfterTest(true);
        m_utils.activateCoverage();
        m_utils.setSectionCoverageStatsMeasureAll(true);
        
        // test-6
        m_utils.createNewDerivedTest(baseIdFuncName, "", funcName, "1, 1, 4", 
                                     "${_rv} == 3000 @@ d", "", true);
        m_utils.activateAnalyzer();
        m_utils.setSectionAnalyzerSaveAfterTest(true);
        m_utils.activateCoverage();
        m_utils.setSectionCoverageStatsMeasureAll(true);

        // if we'd activate analyzer above, then this section gets inherited, so
        // we could not test setting of analyzer file name on activation. Therefore,
        // activate analyzer and cvrg of base test-2 now
        m_utils.selectDerivedTestCase(baseIdFuncName, derivedIdFuncName);
        m_utils.activateAnalyzer();
        m_utils.setSectionAnalyzerSaveAfterTest(true);
        m_utils.activateCoverage();
        m_utils.setSectionCoverageStatsMeasureAll(true);
        
        m_utils.selectTestCase("test-0", funcName);
        m_utils.activateAnalyzer();
        m_utils.setSectionAnalyzerSaveAfterTest(true);
        m_utils.activateCoverage();
        m_utils.setSectionCoverageStatistics(funcName, 53, 60, 75, 75, 0, 0);

        // test analyzer file names
        verifyAnalyzerFileNames(baseIdFuncName, analDocFileName);
        
        // set file names with wizard
        analDocFileName = "${_testId}_${_function}.trd";
        m_utils.wizardAnalyzerFileName(analDocFileName, 
                                       "All test cases", 
                                       "Empty or defined", false, true, false);
        
        // test new analyzer file names
        verifyAnalyzerFileNames(baseIdFuncName, analDocFileName);
        
        // configure merge, test filter dialog - exclude test without coverage
        m_utils.selectDerivedTestCase(baseIdFuncName, "test-6 : " + funcName);
        m_utils.setSectionCoverageMerge("Siblings and parent");
        m_utils.clickButton("Edit");
        m_utils.setFilterForTestCases("", "", "", "test-4");
        m_bot.sleep(500); // wait until the event sent in the dialog propagates to update the preview panel
        SWTBotText testCases = m_bot.textWithLabel("Test cases which pass filter");
        assertEquals("test-0 : " + funcName + "\n"
                     + "test-1 : " + funcName + "\n"
                     + "test-2 : " + funcName + "\n"
                     + "test-3 : " + funcName + "\n"
                     + "test-5 : " + funcName + "\n", 
                     testCases.getText());
        
        m_utils.clickButton("OK");
        m_utils.setSectionCoverageStatistics(funcName, 100, 100, 100, 0, 0, 0);
        
        // run tests and check test results - 100% coverage expected, all tests should pass
        m_utils.runAllTests(UITestUtils.ALL_TESTS_OK_PREFIX + "7");
    }


    private void verifyAnalyzerFileNames(String baseIdFuncName,
                                         String analDocFileName) throws Exception {
        m_utils.selectTestTreeNode(baseIdFuncName);
        m_utils.copyToClipboard();
        
        CTestSpecification baseTestSpec = m_utils.getTestSpecFromClipboard();
        assertEquals(analDocFileName, baseTestSpec.getAnalyzer(true).getDocumentFileName());
        
        {
            CTestSpecification derivedTestSpec = baseTestSpec.getDerivedTestSpec(0);
            assertEquals(analDocFileName, derivedTestSpec.getAnalyzer(true).getDocumentFileName());

            derivedTestSpec = baseTestSpec.getDerivedTestSpec(1);
            assertEquals(analDocFileName, derivedTestSpec.getAnalyzer(true).getDocumentFileName());

            {
                CTestSpecification derivedDerivedTestSpec = derivedTestSpec.getDerivedTestSpec(0);
                assertEquals(analDocFileName, derivedDerivedTestSpec.getAnalyzer(true).getDocumentFileName());

                derivedDerivedTestSpec = derivedTestSpec.getDerivedTestSpec(1);
                assertEquals("", derivedDerivedTestSpec.getAnalyzer(true).getDocumentFileName());

                derivedDerivedTestSpec = derivedTestSpec.getDerivedTestSpec(2);
                assertEquals(analDocFileName, derivedDerivedTestSpec.getAnalyzer(true).getDocumentFileName());
            }
            derivedTestSpec = baseTestSpec.getDerivedTestSpec(2);
            assertEquals(analDocFileName, derivedTestSpec.getAnalyzer(true).getDocumentFileName());
        }
    }
}


