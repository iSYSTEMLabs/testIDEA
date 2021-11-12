package si.isystem.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import si.isystem.connect.CLineDescription;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerCoverage.ECoverageExportFormat;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerExportFormat;
import si.isystem.connect.CTestAnalyzerTrace;
import si.isystem.connect.CTestAnalyzerTrace.ETraceExportFormat;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EDiagType;
import si.isystem.connect.CTestDiagramConfig.EViewFormat;
import si.isystem.connect.CTestDiagramConfig.EViewerType;
import si.isystem.connect.CTestDiagrams;
import si.isystem.connect.CTestDryRun;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CLineDescription.EResourceType;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestMinMax;
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
import si.isystem.connect.CTestStopCondition;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.swtbot.utils.KTableTestUtils;

/**
 * This class tests editing functionality of testIDEA. It fills all fields in
 * all sections, and tests editing commands like Cut, Copy, Paste, Undo, 
 * Redo, ...
 * No test specs are executed in these tests.
 * 
 * @author markok
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class EditingTester {

    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;
    private static KTableTestUtils m_ktableUtils;

    final static String SPEC_FILE_NAME = "D:/tmp/testIDEA-editor-test-temp-file.iyaml";
    final static String SPEC_FILE_NAME_FOR_CTX_MENU = "testOutput/testIDEA-cut-paste-temp-file.iyaml";
    
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

    
    @AfterClass
    public static void shutdown() {
        // m_utils.exitApplication(true); // Don't know why, but the method 
        // ApplicationWorkbenchWindowAdvisor::preWindowShellClose() is not called,
        // so the si.isystem.itest.ipc.ReceiverJob is still running and hence the message
    }
    

    // @Ignore 
    @Test
    public void testAllFieldsInUnitTest() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();
        m_utils.createNewBaseUnitTest("", "", "", "", true);

        List<SectionTest> sectionTests = createSectionsListForUnitTtest();
        
        testListOfSections(sectionTests, MetaSection.TEST_ID, 
                           FunctionSection.FUNC_NAME);
    }
    
    
    // @Ignore 
    @Test
    public void testAllFieldsInSysTest() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();
        m_utils.createNewBaseUnitTest("", "", "", "", true);
        List<SectionTest> sectionTests = new ArrayList<SectionTest>();
    
        MetaSection metaSection = new MetaSection();
        metaSection.setTestScope("System");
        StopConditionSection beginStopCondSection = new StopConditionSection(SectionIds.E_SECTION_BEGIN_STOP_CONDITION);
        StopConditionSection endStopCondSection = new StopConditionSection(SectionIds.E_SECTION_END_STOP_CONDITION);

        sectionTests.add(metaSection);
        sectionTests.add(beginStopCondSection);
        sectionTests.add(endStopCondSection);
        
        testListOfSections(sectionTests, MetaSection.TEST_ID, "/");
    }
    

    // @Ignore 
    @Test
    public void testAllClearSectionAction() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();
        List<SectionTest> sectionTests = createSectionsListForUnitTtest();

        initializeTestSpec(sectionTests);

        for (SectionTest test : sectionTests) {

            test.contextMenu(UITestUtils.CMD_CLEAR_SECTION);
            
            if (test instanceof MetaSection) {
                // test ID is cleared with Meta section
                m_utils.selectTestCase("/", FunctionSection.FUNC_NAME); 
            } else if (test instanceof FunctionSection) {
                // func name  is cleared with Function section
                m_utils.selectTestCase(MetaSection.TEST_ID, "/"); 
            } else {
                m_utils.selectTestCase(MetaSection.TEST_ID, FunctionSection.FUNC_NAME); 
            }
            
            m_utils.copyToClipboard();
            CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
            
            test.areSectionsEmpty(testSpec);
            
            m_bot.sleep(200); // just to make it easier for humans to observe
            m_utils.undo();
            m_bot.sleep(200);
        }
    }


    // @Ignore 
    @Test
    public void testCopyPasteForAllSections() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();
        List<SectionTest> sectionTests = createSectionsListForUnitTtest();

        initializeTestSpec(sectionTests);

        // final String DEST_FUNC_NAME = "encodeMsg";
        m_utils.createNewBaseUnitTest("", "", "", "", false);

        for (SectionTest test : sectionTests) {

            m_utils.selectTestCase(MetaSection.TEST_ID, FunctionSection.FUNC_NAME);
            m_bot.sleep(500);
            
            test.contextMenu(UITestUtils.CMD_COPY_SECTION);

            m_utils.selectTestCase("/", "/");
            m_bot.sleep(500);

            test.contextMenu(UITestUtils.CMD_PASTE_SECTION);
            
            m_bot.sleep(500);
            
            m_utils.copyToClipboard();
            CTestSpecification destTestSpec = m_utils.getTestSpecFromClipboard();

            test.verifyPastedSection(destTestSpec, false);
            
            m_bot.sleep(200); // just to make it easier for humans to observe
            m_utils.undo();
            m_bot.sleep(200);
        }
    }
    

    // @Ignore 
    @Test
    public void testCutPasteForAllSections() throws Exception {
        
        m_utils.discardExistingAndCreateNewProject();
        List<SectionTest> sectionTests = createSectionsListForUnitTtest();

        initializeTestSpec(sectionTests);

        // final String DEST_FUNC_NAME = "encodeMsg";
        m_utils.createNewBaseUnitTest("", "", "", "", false);

        for (SectionTest test : sectionTests) {

            m_utils.selectTestCase(MetaSection.TEST_ID, FunctionSection.FUNC_NAME); 
            m_bot.sleep(500);
            
            test.contextMenu(UITestUtils.CMD_CUT_SECTION);

            // check that pasted sections are NOT empty in dest test spec
            m_utils.selectTestCase("/", "/");
            m_bot.sleep(500);

            test.contextMenu(UITestUtils.CMD_PASTE_SECTION);
            m_bot.sleep(500);

            if (test instanceof MetaSection) {
                // test ID is cleared with Meta section
                m_utils.selectTestCase("/", FunctionSection.FUNC_NAME); 
            } else if (test instanceof FunctionSection) {
                // func name  is cleared with Function section
                m_utils.selectTestCase(MetaSection.TEST_ID, "/"); 
            } else {
                m_utils.selectTestCase(MetaSection.TEST_ID, FunctionSection.FUNC_NAME); 
            }

            m_bot.sleep(250);
            // check that cut sections are empty in src test spec
            m_utils.copyToClipboard();
            CTestSpecification srcTestSpec = m_utils.getTestSpecFromClipboard();
            
            test.areSectionsEmpty(srcTestSpec);
            
            // check that pasted sections are NOT empty in dest test spec
            if (test instanceof MetaSection) {
                // test ID is cleared with Meta section
                m_utils.selectTestCase(MetaSection.TEST_ID, "/"); 
            } else if (test instanceof FunctionSection) {
                // func name  is cleared with Function section
                m_utils.selectTestCase("/", FunctionSection.FUNC_NAME); 
            } else {
                m_utils.selectTestCase("/", "/"); 
            }
            
            m_bot.sleep(200);
            m_utils.copyToClipboard();
            CTestSpecification destTestSpec = m_utils.getTestSpecFromClipboard();
            
            // System.out.println(test.getClass().getSimpleName() + "\n" + destTestSpec + "\n--------\n\n");
            test.verifyPastedSection(destTestSpec, false);
            
            m_bot.sleep(200); // just to make it easier for humans to observe
            m_utils.undo();  // undo paste on dest
            m_bot.sleep(200);
            m_utils.undo();  // undo cut on src
        }
    }


    // tests pasting of several lines to empty tables - lines should be added 
    @Test
    public void testPaste1ColLinesToListTable() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        
        m_utils.createNewBaseUnitTest("myfuncCopy", "", "", "", false);
        m_utils.setSectionExpected("10", ETristate.E_FALSE, 
                                   "i == 0", "j == 1", "k == 2");
        m_ktableUtils.selectKTableWithContent(1, 0, "Expressions");
        m_ktableUtils.selectCell(0, 1);
        m_utils.pressKey(SWT.SHIFT, SWT.ARROW_DOWN);
        m_utils.pressKey(SWT.SHIFT, SWT.ARROW_DOWN);
        m_utils.copyToClipboard();

        final String pastedFuncName = "myfuncPaste";
        m_utils.createNewBaseUnitTest(pastedFuncName, "", "", "", false);
        m_utils.selectTestSection(UITestUtils.SEC_EXPECTED);
        m_ktableUtils.selectKTableWithContent(1, 0, "Expressions");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_PERSIST_VARIABLES);
        m_ktableUtils.selectKTableWithContent(2, 0, "Variable type");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        m_ktableUtils.selectCell(2, 0);
        m_utils.pasteFromClipboard();
        
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_DEL_PERSIST_VARS_TABLE);
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();

        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        m_ktableUtils.selectKTableWithContent(1, 0, "Variable name");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        m_ktableUtils.selectCell(2, 0);
        m_utils.pasteFromClipboard();
        
        m_ktableUtils.selectKTableWithContent(1, 0, "Variable");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        m_ktableUtils.selectCell(2, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_PRECONDITION);
        m_ktableUtils.selectKTableWithContent(1, 0, "Expressions");
        m_ktableUtils.selectCell(0, 1);
        m_utils.pasteFromClipboard();

        m_utils.selectTestSection(UITestUtils.SEC_HIL);
        m_ktableUtils.selectKTableWithContent(1, 0, "HIL Parameter.*");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        m_ktableUtils.selectCell(2, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_OPTIONS);
        m_ktableUtils.selectKTableWithContent(1, 0, "winIDEA Option.*");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        m_ktableUtils.selectCell(2, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_DRY_RUN);
        m_ktableUtils.selectKTableWithContent(2, 0, "Assigned expr.*");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        m_ktableUtils.selectCell(2, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestCase("/", pastedFuncName);
        m_utils.pressKey(SWT.CTRL, 'c');

        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        String tsStr = testSpec.toString();
        String expected = "options:\n" +
                "  i == 0: i == 0\n" +
                "  j == 1: j == 1\n" +
                "  k == 2: k == 2\n" +
                "persistVars:\n" +
                "  decl:\n" +
                "    i == 0: i == 0\n" +
                "    j == 1: j == 1\n" +
                "    k == 2: k == 2\n" +
                "  delete:\n" +
                "  - i == 0\n" +
                "  - j == 1\n" +
                "  - k == 2\n" +
                "locals:\n" +
                "  i == 0: i == 0\n" +
                "  j == 1: j == 1\n" +
                "  k == 2: k == 2\n" +
                "init:\n" +
                "  i == 0: i == 0\n" +
                "  j == 1: j == 1\n" +
                "  k == 2: k == 2\n" +
                "func:\n" +
                "  func: myfuncPaste\n" +
                "preCondition:\n" +
                "  expressions:\n" +
                "  - i == 0\n" +
                "  - j == 1\n" +
                "  - k == 2\n" +
                "assert:\n" +
                "  expressions:\n" +
                "  - i == 0\n" +
                "  - j == 1\n" +
                "  - k == 2\n" +
                "hil:\n" +
                "  params:\n" +
                "    i == 0: i == 0\n" +
                "    j == 1: j == 1\n" +
                "    k == 2: k == 2\n" +
                "dryRun:\n" +
                "  assign:\n" +
                "    i == 0: i == 0\n" +
                "    j == 1: j == 1\n" +
                "    k == 2: k == 2\n";
        
        assertEquals(expected, tsStr);
    }
    
    
    // tests pasting to tables with two columns (mapping tables)
    @Test
    public void testPaste2ColLinesToListTable() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        
        m_utils.createNewBaseUnitTest("myfuncCopy", "", "", "", false);
        m_utils.setPersistentVar(0, "a", "int");
        m_utils.setPersistentVar(1, "b", "char");
        m_utils.setPersistentVar(2, "c", "double");
        m_ktableUtils.selectKTableWithContent(2, 0, "Variable type");
        m_ktableUtils.selectCell(0, 1);
        m_utils.pressKey(SWT.SHIFT, SWT.ARROW_DOWN);
        m_utils.pressKey(SWT.SHIFT, SWT.ARROW_DOWN);
        m_utils.copyToClipboard();

        final String pastedFuncName = "myfuncPaste";
        m_utils.createNewBaseUnitTest(pastedFuncName, "", "", "", false);
        
        m_utils.selectTestSection(UITestUtils.SEC_PERSIST_VARIABLES);
        m_ktableUtils.selectKTableWithContent(2, 0, "Variable type");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_VARIABLES);
        m_ktableUtils.selectKTableWithContent(1, 0, "Variable name");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_ktableUtils.selectKTableWithContent(1, 0, "Variable");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_HIL);
        m_ktableUtils.selectKTableWithContent(1, 0, "HIL Parameter.*");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_OPTIONS);
        m_ktableUtils.selectKTableWithContent(1, 0, "winIDEA Option.*");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestSection(UITestUtils.SEC_DRY_RUN);
        m_ktableUtils.selectKTableWithContent(2, 0, "Assigned expr.*");
        m_ktableUtils.selectCell(1, 0);
        m_utils.pasteFromClipboard();
        
        m_utils.selectTestCase("/", pastedFuncName);
        m_utils.pressKey(SWT.CTRL, 'c');

        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        String tsStr = testSpec.toString();
        String expected = "options:\n" +
                "  a: int\n" +
                "  b: char\n" +
                "  c: double\n" +
                "persistVars:\n" +
                "  decl:\n" +
                "    a: int\n" +
                "    b: char\n" +
                "    c: double\n" +
                "locals:\n" +
                "  a: int\n" +
                "  b: char\n" +
                "  c: double\n" +
                "init:\n" +
                "  a: int\n" +
                "  b: char\n" +
                "  c: double\n" +
                "func:\n" +
                "  func: myfuncPaste\n" +
                "hil:\n" +
                "  params:\n" +
                "    a: int\n" +
                "    b: char\n" +
                "    c: double\n" +
                "dryRun:\n" +
                "  assign:\n" +
                "    a: int\n" +
                "    b: char\n" +
                "    c: double\n";
        
        assertEquals(expected, tsStr);
    }
    
    
    private void initializeTestSpec(List<SectionTest> sectionTests) throws Exception {
        // if file exists, use it, because it is much faster, otherwise create
        // test spec section by section
        File file = new File(SPEC_FILE_NAME_FOR_CTX_MENU);
        boolean isCreateTestSpec = false;
        if (file.exists()  &&  !isCreateTestSpec) {
            System.out.println("=====\nLoading test spec from file! If the file is old, delete it!\n=====\nfile: " +
                               SPEC_FILE_NAME_FOR_CTX_MENU);
            m_utils.loadTestSpecFromFile(SPEC_FILE_NAME_FOR_CTX_MENU);
        } else {
            m_utils.createNewBaseUnitTest("", "", "", "", true);
            for (SectionTest test : sectionTests) {
                test.setSection();
            }
            UITestUtils.getActiveModel().saveModelAs(SPEC_FILE_NAME_FOR_CTX_MENU);
        }
        
        m_utils.selectTestCase(MetaSection.TEST_ID, FunctionSection.FUNC_NAME);
    }

    
    private List<SectionTest> createSectionsListForUnitTtest() {
        List<SectionTest> sectionTests = new ArrayList<SectionTest>();

        MetaSection metaSection = new MetaSection();
        FunctionSection functionSection = new FunctionSection();
        
        sectionTests.add(metaSection);
        sectionTests.add(functionSection);
        sectionTests.add(new PreConditionSection());
        sectionTests.add(new ExpectedSection());
        sectionTests.add(new PersistentVarsSection());
        sectionTests.add(new VarsSection());
        sectionTests.add(new StubsSection());
        sectionTests.add(new UserStubsSection());
        sectionTests.add(new TestPointsSection());
        sectionTests.add(new AnalyzerSection());
        sectionTests.add(new CoverageSection());
        sectionTests.add(new ProfilerSection());
        sectionTests.add(new TraceSection());
        sectionTests.add(new HILSection());
        sectionTests.add(new ScriptsSection());
        sectionTests.add(new OptionsSection());
        sectionTests.add(new DryRunSection());
        sectionTests.add(new DiagramsSection());
        
        return sectionTests;
    }
    
    
    private void testListOfSections(List<SectionTest> sectionTests, 
                                    String mainTestId, String functionName) throws Exception {
        
        for (SectionTest test : sectionTests) {
            test.setSection();
        }
        
        // Save and load the test spec, so that we also test serialization and parsing.
        
        // m_utils.getToolbarButton("Save").click(); // can not enter file name because SWT does not support native dialogs.
        UITestUtils.getActiveModel().saveModelAs(SPEC_FILE_NAME);
        m_utils.discardExistingAndCreateNewProject();
        m_utils.loadTestSpecFromFile(SPEC_FILE_NAME);
        
        m_utils.selectTestCase(mainTestId, functionName);
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();

        for (SectionTest test : sectionTests) {
            test.testSection(testSpec);
        }
    }


    abstract class SectionTest {
        abstract void setSection() throws Exception;
        abstract void testSection(CTestSpecification testSpec) throws Exception;
        abstract void contextMenu(String command) throws Exception;
        abstract Set<SectionIds> getSectionIds();
        
        void areSectionsEmpty(CTestSpecification testSpec) throws Exception {
            
            SectionIds[] allIds = getIdsToBeTested();
            Set<SectionIds> subIds = getSectionIds();
            for (SectionIds id : allIds) {
                System.out.println("- id: "+ id);
                if (subIds.contains(id)) {
                    assertTrue("Cleared section is not empty: " + id.toString(), testSpec.isSectionEmpty(id));
                } else {
                    if (testSpec.isSectionEmpty(id)) {
                        assertFalse("Non-cleared section is empty: " + id.toString(), true);
                    }
                }
            }
        }

        
        void verifyPastedSection(CTestSpecification testSpec, boolean isAnalyzerSubSection) {
            
            SectionIds[] allIds = getIdsToBeTested();
            Set<SectionIds> testSectionsInEditor = getSectionIds();
            
            for (SectionIds id : allIds) {
                
                if (testSectionsInEditor.contains(id)  ||  (id == SectionIds.E_SECTION_ANALYZER  &&  isAnalyzerSubSection)) {
                    assertFalse("Pasted section is empty: " + id.toString() + "\n" + testSpec, 
                                testSpec.isSectionEmpty(id.swigValue()));
                } else {
                    assertTrue("Non-pasted section is not empty: " + id.toString() + "\n" + testSpec, 
                               testSpec.isSectionEmpty(id.swigValue()));
                }
            }
        }
        
        
        private SectionIds[] getIdsToBeTested() {
            SectionIds[] allIds = new SectionIds[]{            
                    SectionIds.E_SECTION_ID, 
                    SectionIds.E_SECTION_TEST_SCOPE, 

                    SectionIds.E_SECTION_DESC, 
                    SectionIds.E_SECTION_TAGS, 
                    SectionIds.E_SECTION_OPTIONS,

                    SectionIds.E_SECTION_LOCALS,
                    SectionIds.E_SECTION_INIT,
                    SectionIds.E_SECTION_FUNC,

                    SectionIds.E_SECTION_INIT_TARGET,
                    SectionIds.E_SECTION_INITFUNC,
                    SectionIds.E_SECTION_ENDFUNC,
                    SectionIds.E_SECTION_RESTORE_TARGET,

                    SectionIds.E_SECTION_STUBS,
                    SectionIds.E_SECTION_USER_STUBS,
                    SectionIds.E_SECTION_TEST_POINTS,

                    SectionIds.E_SECTION_ASSERT,

                    SectionIds.E_SECTION_ANALYZER,
                    // SectionIds.E_SECTION_TRACE,
                    // SectionIds.E_SECTION_COVERAGE,
                    // SectionIds.E_SECTION_PROFILER,

                    SectionIds.E_SECTION_HIL,
                    SectionIds.E_SECTION_STACK_USAGE};
            return allIds;
        }
    }
    
    
    class MetaSection extends SectionTest {
        String m_testScope = "Unit";
        final static String TEST_ID = "integrTest";
        String testDescription = "This test has all fields defined!";
        String testTag0 = "Ljubljana";
        String testTag1 = "Bezigrad";
        
        void setTestScope(String testScope) {
            m_testScope = testScope;
        }
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionMeta(true, m_testScope, TEST_ID, testDescription, 
                                   testTag0 + ", " + testTag1);
        }
        
        
        @Override
        public void testSection(CTestSpecification testSpec) {
            assertEquals(TEST_ID, testSpec.getTestId());
            assertEquals(testDescription, testSpec.getDescription());
            
            StrVector tags = new StrVector();
            testSpec.getTags(tags);
            assertEquals(testTag0, tags.get(0));
            assertEquals(testTag1, tags.get(1));
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_TEST_SCOPE);
            ids.add(SectionIds.E_SECTION_ID);
            ids.add(SectionIds.E_SECTION_DESC);
            ids.add(SectionIds.E_SECTION_TAGS);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_META, command);
        }
    }
    
    
    class FunctionSection extends SectionTest {

        final static String FUNC_NAME = "sub_int";
        String futParam0 = "142", futParam1 = "564";
        String futRetVal = "retVal";
        
        @Override
        public void setSection() throws Exception {

            m_utils.setSectionFunction(FUNC_NAME, 
                                       futParam0 + ", " + futParam1, 
                                       futRetVal);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            assertEquals(FUNC_NAME, testSpec.getFunctionUnderTest(true).getName());
            
            StrVector params = new StrVector();
            testSpec.getPositionParams(params);
            assertEquals(futParam0, params.get(0));
            assertEquals(futParam1, params.get(1));
            
            assertEquals(futRetVal, testSpec.getFunctionUnderTest(true).getRetValueName());
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_FUNC);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_FUNCTION, command);
        }
    }    
    
    
    class StopConditionSection extends SectionTest {

        String bscStopType = "Breakpoint";
        int bscTimeout = 1234;
        String bscRealTimeExpr = "yy < 45";
        int bscCondCount = 3;
        String bscCondExpr = "timer > 890.34";

        String bscResType = "File";
        String bscResName = "d:\\common\\main.c";
        int bscLine = 0;
        String bscSearchLine = "Yes";
        int bscLinesRange = 330; 
        String bscSearchContext = "Comment (// only)";
        String bscMatchType = "Reg. exp.";
        String bscSearchPattern = "Reg. exp.";
        int bscLineOffset = 0;
        int bscNumSteps = 0;
        
        private SectionIds m_sectionId;
        
        StopConditionSection(SectionIds sectionId) {
            m_sectionId = sectionId;
            
            if (sectionId == SectionIds.E_SECTION_END_STOP_CONDITION) {
                bscStopType = "Real-time expr.";
                bscTimeout = 91234;
                bscRealTimeExpr = "yyz < cnt";
                bscCondCount = 991;
                bscCondExpr = "m_timer > 1890.34";

                bscResType = "File";
                bscResName = "\\common\\main.c";
                bscLine = 100;
                bscSearchLine = "Yes";
                bscLinesRange = 330; 
                bscSearchContext = "Code";
                bscMatchType = "Plain text";
                bscSearchPattern = "ice";
                bscLineOffset = 1;
                bscNumSteps = 2;
            }
        }
        
        @Override
        public void setSection() throws Exception {
        
            if (m_sectionId == SectionIds.E_SECTION_BEGIN_STOP_CONDITION) {
                m_utils.setSectionBeginStopCond(bscStopType, bscTimeout, bscRealTimeExpr,
                                                bscCondCount, bscCondExpr);

                m_utils.setSrcLocationDlg(bscResType, bscResName, bscLine, 
                                          bscSearchLine, bscLinesRange, 
                                          bscSearchContext, bscMatchType, 
                                          bscSearchPattern, bscLineOffset, 
                                          bscNumSteps);
            } else {
                m_utils.setSectionEndStopCond(bscStopType, bscTimeout, bscRealTimeExpr,
                                              bscCondCount, bscCondExpr);
            }
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            
            CTestBase tb = testSpec.getTestBase(m_sectionId.swigValue(), true);
            CTestStopCondition stopCond = CTestStopCondition.cast(tb);
            
            if (bscStopType.equals("Breakpoint")) {
                assertEquals(CTestStopCondition.EStopType.E_BREAKPOINT, stopCond.getStopType());
                assertEquals(bscTimeout, stopCond.getTimeout());

                assertEquals(bscCondCount, stopCond.getConditionCount()); 
                assertEquals(bscCondExpr, stopCond.getConditionExpr());
            
                CTestLocation loc = stopCond.getBreakpointLocation(true);
                assertEquals(EResourceType.E_RESOURCE_FILE, loc.getResourceType());
                assertEquals(bscResName, loc.getResourceName());
                assertEquals(bscLine, loc.getLine());
                assertEquals(ETristate.E_TRUE, loc.isSearch());
                assertEquals(CLineDescription.ESearchContext.E_SEARCH_COMMENT, loc.getSearchContext());
                assertEquals(CLineDescription.EMatchingType.E_MATCH_REG_EX, loc.getMatchingType());
                assertEquals(bscSearchPattern, loc.getSearchPattern());
            
                assertEquals(bscLineOffset, loc.getLineOffset());
                assertEquals(bscNumSteps, loc.getNumSteps());
            } else {
                assertEquals(CTestStopCondition.EStopType.E_RT_EXPRESSION, stopCond.getStopType());
                assertEquals(bscTimeout, stopCond.getTimeout());
                assertEquals(bscRealTimeExpr, stopCond.getRtExpression()); 
            }
            
        }
        
        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(m_sectionId);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            if (m_sectionId == SectionIds.E_SECTION_BEGIN_STOP_CONDITION) {
                m_utils.contextMenu(UITestUtils.SEC_SYS_BEGIN_SCOND, command);
            } else {
                m_utils.contextMenu(UITestUtils.SEC_SYS_END_SCOND, command);
            }
        }
    }
    
    
    class PersistentVarsSection extends SectionTest {

        String m_varName = "persist_a";
        String m_varType = "double";
        String m_deletedVarName = "persist_b";
        boolean m_isDeleteAllVars = true;

        
        @Override
        void setSection() throws Exception {
            m_utils.setPersistentVar(0, m_varName, m_varType);
            m_utils.setDeletedPersistVar(0, m_deletedVarName);
            m_utils.setIsDeleteAllPersistVars(m_isDeleteAllVars);
        }

        
        @Override
        void testSection(CTestSpecification testSpec) throws Exception {
            CTestPersistentVars persistVars = testSpec.getPersistentVars(true);
            
            CMapAdapter persistVarsMap = new CMapAdapter(persistVars,
                                                      EPersistVarsSections.E_SECTION_DECL.swigValue(),
                                                      true);
            assertEquals(m_varType, persistVarsMap.getValue(m_varName));
            
            CSequenceAdapter deletedPeristsVars = new CSequenceAdapter(persistVars, 
                                                                       EPersistVarsSections.E_SECTION_DELETE.swigValue(),
                                                                       true);
            assertEquals(m_deletedVarName, deletedPeristsVars.getValue(0));
        }

        
        @Override
        void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_PERSIST_VARIABLES, command);
        }
        

        @Override
        Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_PERSIST_VARS);
            return ids;
        }
        
    }
    
    
    class VarsSection extends SectionTest {

        String varType = "int";
        String varName = "street";
        String varValue = "1001";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setVar(0, varType, varName, varValue);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {

            StrStrMap localVars = new StrStrMap();
            testSpec.getLocalVariables(localVars);
            assertEquals(varType, localVars.get(varName));
            
            StrVector initValues = new StrVector();
            testSpec.getInitValues(initValues);
            assertEquals(varValue, initValues.get(0));
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_LOCALS);
            ids.add(SectionIds.E_SECTION_INIT);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_VARIABLES, command);
        }
    }
    

    class PreConditionSection extends SectionTest {

        String [] m_expressions = new String[]{"g_char0 == 0", "g_char1 == 0"};
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionPreCondition(m_expressions);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            StrVector expressions = new StrVector();
            testSpec.getPrecondition(true).getExpressions(expressions);
            
            for (int idx = 0; idx < expressions.size(); idx++) {
                assertEquals(m_expressions[idx], expressions.get(idx));
            }            
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_PRE_CONDITION);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_PRECONDITION, command);
        }
    }
    
    
    class ExpectedSection extends SectionTest {

        String m_maxStackUsed = "45";
        ETristate m_isExpectTargetException = ETristate.E_TRUE;
        String m_expression = "retVal == -7654";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionExpected(m_maxStackUsed, 
                                       m_isExpectTargetException, 
                                       m_expression);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            StrVector expressions = new StrVector();
            testSpec.getExpectedResults(expressions);
            assertEquals(m_expression, expressions.get(0));
            
            assertEquals(m_isExpectTargetException, 
                         testSpec.getAssert(true).isExpectException());
            
            assertEquals(m_maxStackUsed,
                         testSpec.getStackUsage(true).getMaxUsedSize()); 
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_STACK_USAGE);
            ids.add(SectionIds.E_SECTION_ASSERT);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_EXPECTED, command);
        }
    }
    
    
    class StubsSection extends SectionTest {

        String stubbedFunc = "myStubbedFunc";
        String stubParam0 = "a";
        String stubParam1 = "b";
        String stubParam2 = "c";
        String stubRv = "stubRv";
        String stubScript = "pyStub";
        String stubMinHits = "2";
        String stubMaxHits = "56";

        String stubScriptParams = "None";
        String stubAssignmentVar = "xy";
        String stubAssignmentVal = "9";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionStub(stubbedFunc, "Yes", 
                                   stubParam0 + ", " + stubParam1 + ", " + stubParam2, 
                                   stubRv, stubScript, 
                                   stubMinHits,
                                   stubMaxHits, false);
            
            m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, stubAssignmentVar, stubAssignmentVal, new String[]{stubScriptParams});
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestStub stub = testSpec.getStub(stubbedFunc);
            assertTrue(stub.isActive());

            StrVector stubParams = new StrVector();
            stub.getParamNames(stubParams);
            assertEquals(stubParam0, stubParams.get(0));
            assertEquals(stubParam1, stubParams.get(1));
            assertEquals(stubParam2, stubParams.get(2));
            
            assertEquals(stubRv, stub.getRetValName());
            assertEquals(stubScript, stub.getScriptFunctionName());
            
            CTestMinMax limits = stub.getHitLimits(true);
            assertEquals(stubMinHits, Integer.toString(limits.getMin()));
            assertEquals(stubMaxHits, Integer.toString(limits.getMax()));
            
            m_utils.verifyStubAssignments(stub.getAssignmentSteps(true), 
                                  new String[0], stubAssignmentVar, 
                                  stubAssignmentVal, new String[]{stubScriptParams});
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_STUBS);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_STUBS, command);
        }
    }
    
    
    class UserStubsSection extends SectionTest {

        String userStubName = "userStub";
        String replFunc = "replFunction";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionUserStubs(userStubName, "Yes", "Call target function", replFunc, false);        
        }

        
        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {

            CTestUserStub userStub = testSpec.getUserStub(userStubName);
            assertTrue(userStub.isActive() == ETristate.E_TRUE);
            assertEquals(replFunc, userStub.getReplacementFuncName());        
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_USER_STUBS);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_USER_STUBS, command);
        }
    }
    
    
    class TestPointsSection extends SectionTest {

        String testPointName = "testPT";
        int tpCondCount = 67;
        String tpCondExpr = "x < 45";
        String tpScriptFunc = "tpPyFunc";

        String tpResType = "Function";
        String tpResName = "root";
        int tpLine = 34;
        String tpSearchLine = "Yes";
        int tpLinesRange = 33; 
        String tpSearchContext = "Any";
        String tpMatchType = "Plain text";
        String tpSearchPattern = "myIdTxt";
        int tpLineOffset = 9;
        int tpNumSteps = 22;

        String tpLogBefore = "x, y";
        String tpLogAfter = "counter, anIn";

        String tpScriptParams = "myTestSpec";
        String tpAssignmentVar = "digOut_1";
        String tpAssignmentVal = "'a'";
        String minHits = "44"; 
        String maxHits = "78";
        
        @Override
        public void setSection() throws Exception {
            
            m_utils.setSectionTestPoint(testPointName, "Yes", tpCondCount, tpCondExpr, tpScriptFunc, false);
            
            m_utils.setSrcLocationDlg(tpResType, tpResName, tpLine, 
                                      tpSearchLine, tpLinesRange, 
                                      tpSearchContext, tpMatchType, 
                                      tpSearchPattern, tpLineOffset, 
                                      tpNumSteps);
            m_utils.setMinMaxLimits(minHits, maxHits);
            m_utils.setStubTestPointLogging(tpLogBefore, tpLogAfter);
            
            m_ktableUtils.selectKTable(SWTBotConstants.TEST_POINTS_STEPS_KTABLE);
            m_ktableUtils.setSectionStubAssignments(0, tpAssignmentVar, tpAssignmentVal, new String[]{tpScriptParams});
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestPoint tp = testSpec.getTestPoint(testPointName);
            assertTrue(tp.isActive() == ETristate.E_TRUE);

            assertEquals(tpCondCount, tp.getConditionCount());
            assertEquals(tpCondExpr, tp.getConditionExpr());
            assertEquals(tpScriptFunc, tp.getScriptFunctionName());
            
            CTestLocation loc = tp.getLocation(true);
            assertEquals(EResourceType.E_RESOURCE_FUNCTION, loc.getResourceType());
            assertEquals(tpResName, loc.getResourceName());
            assertEquals(tpLine, loc.getLine());
            assertEquals(ETristate.E_TRUE, loc.isSearch());
            assertEquals(CLineDescription.ESearchContext.E_SEARCH_ANY, loc.getSearchContext());
            assertEquals(CLineDescription.EMatchingType.E_MATCH_PLAIN, loc.getMatchingType());
            assertEquals(tpSearchPattern, loc.getSearchPattern());
            
            assertEquals(tpLineOffset, loc.getLineOffset());
            assertEquals(tpNumSteps, loc.getNumSteps());

            CTestLog tpLog = tp.getLogConfig(true);
            CSequenceAdapter seqBefore = tpLog.getExpressions(ESectionsLog.E_SECTION_BEFORE, true);
            CSequenceAdapter seqAfter = tpLog.getExpressions(ESectionsLog.E_SECTION_AFTER, true);
            assertEquals(tpLogBefore, seqBefore.getValue(0) + ", " + seqBefore.getValue(1)); 
            assertEquals(tpLogAfter, seqAfter.getValue(0) + ", " + seqAfter.getValue(1)); 
            
            CTestMinMax limits = tp.getHitLimits(true);
            assertEquals(minHits, Integer.toString(limits.getMin()));
            assertEquals(maxHits, Integer.toString(limits.getMax()));

            m_utils.verifyStubAssignments(tp.getSteps(true), 
                                          new String[0], tpAssignmentVar, 
                                          tpAssignmentVal, new String[]{tpScriptParams});
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_TEST_POINTS);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_TEST_POINTS, command);
        }
    }
    
    
    class AnalyzerSection extends SectionTest {
        ERunMode m_runMode = ERunMode.M_START; 
        String m_fileName = "analyzer.trd";
        String m_openMode = "Write";
        String m_triggerName = "analyzerTrigger";
        String m_useSlowRun = "No";
        String m_saveAfterTest = "Yes";
        String m_closeAfterTest = "Default (No)";
        
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionAnalyzer(m_runMode == ERunMode.M_START ? "Start" : "Off", 
                                       m_fileName, m_openMode, 
                                       "No", m_useSlowRun, m_saveAfterTest, 
                                       m_closeAfterTest, m_triggerName, false);
        }
        
        
        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            
            assertEquals(m_runMode, analyzer.getRunMode());
            assertEquals(m_fileName, analyzer.getDocumentFileName());
            assertEquals("E" + m_openMode, analyzer.getOpenMode().toString());
            
            assertEquals(m_triggerName, analyzer.getTriggerName());
            
            assertEquals(ETristate.E_FALSE, analyzer.isSlowRun());
            assertEquals(ETristate.E_TRUE, analyzer.isSaveAfterTest());
            assertEquals(ETristate.E_DEFAULT, analyzer.isCloseAfterTest());
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_ANALYZER, command);
        }
        
        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_ANALYZER);
            return ids;
        }
    }
    
    
    class CoverageSection extends SectionTest {

        boolean m_isActive = true;
        String m_coverageExportFileName = "cvrgExport.xml";
        String m_coverageExportFormat = "XML";
        String m_variant = "someXml";
        String m_modulesFilter = "modName";
        String m_functionsFilter = "funcFilt";

        String m_coverageStatisticFunc = "setGlobals";
        int [] m_coverageExpected = {90, 80, 70, 60, 50, 40};
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionCoverageTestCase("Yes", m_coverageExportFormat, 
                                       m_coverageExportFileName, m_variant,
                                       "Yes", "Yes", "Yes", "No", "Yes", "Yes", "No", "Yes",
                                       m_modulesFilter, m_functionsFilter);

            m_utils.setSectionCoverageStatistics(m_coverageStatisticFunc, 
                                                 m_coverageExpected[0], m_coverageExpected[1], 
                                                 m_coverageExpected[2], m_coverageExpected[3], 
                                                 m_coverageExpected[4], m_coverageExpected[5]);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            CTestAnalyzerCoverage cvrg = analyzer.getCoverage(true);
            assertTrue(cvrg.isActive() == ETristate.E_TRUE);
            assertEquals(ECoverageExportFormat.EX_CCAsXML, cvrg.getExportFormat());
            assertEquals(m_coverageExportFileName, cvrg.getExportFileName());
            assertEquals(m_variant, cvrg.getExportFormatVariant());
            assertTrue(cvrg.isProvideAssemblerInfo() == ETristate.E_TRUE);
            assertTrue(cvrg.isExportModuleLines() == ETristate.E_TRUE);
            assertTrue(cvrg.isExportFunctionLines() == ETristate.E_FALSE);
            assertTrue(cvrg.isExportSources() == ETristate.E_TRUE);
            assertTrue(cvrg.isExportAsm() == ETristate.E_TRUE);
            assertTrue(cvrg.isExportRanges() == ETristate.E_FALSE);
            assertTrue(cvrg.isLaunchViewer() == ETristate.E_TRUE);
            assertEquals(m_modulesFilter, cvrg.getExportModulesFilter());
            assertEquals(m_functionsFilter, cvrg.getExportFunctionsFilter());

            CTestCoverageStatistics stat = cvrg.getStatistics(0);
            assertEquals(m_coverageStatisticFunc, stat.getFunctionName());
            assertEquals(String.valueOf(m_coverageExpected[0]), stat.getBytesExecutedText());
            assertEquals(String.valueOf(m_coverageExpected[1]), stat.getSourceLinesExecutedText());
            assertEquals(String.valueOf(m_coverageExpected[2]), stat.getBranchExecutedText());
            assertEquals(String.valueOf(m_coverageExpected[3]), stat.getBranchTakenText());
            assertEquals(String.valueOf(m_coverageExpected[4]), stat.getBranchNotTakenText());
            assertEquals(String.valueOf(m_coverageExpected[5]), stat.getBranchBothText());
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            // ids.add(SectionIds.E_SECTION_COVERAGE);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_COVERAGE, command);
        }

        
        @Override
        void areSectionsEmpty(CTestSpecification testSpec) throws Exception {
            super.areSectionsEmpty(testSpec);
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            assertTrue("Coverage section should be empty!", analyzer.getCoverage(true).isEmpty());
        }
        

        @Override
        void verifyPastedSection(CTestSpecification testSpec, boolean isAnalyzerSubSection) {
            super.verifyPastedSection(testSpec, true);
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            assertFalse("Pasted coverage section should not be empty!", analyzer.getCoverage(true).isEmpty());
            assertTrue("Profiler section should be empty!", analyzer.getProfiler(true).isEmpty());
            assertTrue("Trace section should be empty!", analyzer.getTrace(true).isEmpty());
        }        
    }
    
    
    class ProfilerSection extends SectionTest {

        boolean m_isActive = true;
        String m_profilerExportFileName = "profExport.xml";
        String m_profilerExportFormat = "CSV";
        boolean m_isSaveTimeline = true;
        boolean m_isProfilerAux = false;
        
        String m_profilerFuncArea = "FuncX";
        String m_profilerDataArea = "varY/33";
        String [][] m_funcTimes = new String[8][8];
        String [][] m_dataTimes = new String[8][8];
        int m_hitsFuncLow = 123;
        int m_hitsFuncHigh= 1234;
        int m_hitsDataLow = 345;
        int m_hitsDataHigh= 46;
        String m_nlComment1 = "nl one";
        String m_eolComment1 = "eol one";
        String m_nlComment2 = "nl two";
        String m_eolComment2 = "eol two";
        
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionProfiler("Yes",  
                                       m_profilerExportFormat, 
                                       m_profilerExportFileName, 
                                       m_isSaveTimeline ? "Yes" : "No", 
                                       m_isProfilerAux ? "Yes" : "No", 
                                       "Default (No)");

            for (int timeScope = 0; timeScope < m_funcTimes.length; timeScope++) {
                for (int timeType = 0; timeType < m_funcTimes[0].length; timeType++) {
                    m_funcTimes[timeScope][timeType] = String.valueOf(timeScope * 10 + timeType);
                    m_dataTimes[timeScope][timeType] = String.valueOf(600 + timeScope * 10 + timeType);
                }
            }
            
            // clear total time for Period time, because it is not defined in winIDEA
            m_funcTimes[6][3] = "";
            m_funcTimes[7][3] = "";
            m_dataTimes[4][3] = "";
            m_dataTimes[5][3] = "";
            
            m_utils.setProfilerStatsForEditorTest(m_profilerFuncArea,
                                                  m_funcTimes,
                                                  m_hitsFuncLow, m_hitsFuncHigh,
                                                  m_profilerDataArea,
                                                  m_dataTimes,
                                                  m_hitsDataLow, m_hitsDataHigh);
            
            m_ktableUtils.selectKTable(SWTBotConstants.PROFILER_STATS_KTABLE);

            // Min time of Net time, lower bound
            m_utils.setProfilerStatsTableComment(2, 1, m_nlComment1, m_eolComment1, m_ktableUtils);
            
            // min start of Period time, upper bound
            m_utils.setProfilerStatsTableComment(6, 9, m_nlComment2, m_eolComment2, m_ktableUtils);
        }

        
        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            CTestAnalyzerProfiler prof = analyzer.getProfiler(true);
            assertTrue(prof.isActive() == ETristate.E_TRUE);
            assertEquals(EProfilerExportFormat.EProfilerAsCSV, prof.getExportFormat());
            assertEquals(m_profilerExportFileName, prof.getExportFileName());
            assertEquals(m_isSaveTimeline, prof.isSaveHistory() == ETristate.E_TRUE);
            assertEquals(m_isProfilerAux, prof.isProfileAUX() != ETristate.E_FALSE);
            assertEquals(ETristate.E_DEFAULT, prof.isExportActiveAreasOnly());
            
            CTestProfilerStatistics codeStat = prof.getArea(CTestAnalyzerProfiler.EAreaType.CODE_AREA, 0);
            assertEquals(m_profilerFuncArea, codeStat.getAreaName());

            EProfilerStatisticsSectionId [] scopes = {EProfilerStatisticsSectionId.E_SECTION_NET_TIME,
                                                      EProfilerStatisticsSectionId.E_SECTION_GROSS_TIME,
                                                      EProfilerStatisticsSectionId.E_SECTION_CALL_TIME,
                                                      EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME};
            
            EProfilerTimeSectionId [] types = {EProfilerTimeSectionId.E_SECTION_MIN_TIME,
                                               EProfilerTimeSectionId.E_SECTION_MAX_TIME,
                                               EProfilerTimeSectionId.E_SECTION_AVERAGE_TIME,
                                               EProfilerTimeSectionId.E_SECTION_TOTAL_TIME,
                                               EProfilerTimeSectionId.E_SECTION_MIN_START_TIME,
                                               EProfilerTimeSectionId.E_SECTION_MIN_END_TIME,
                                               EProfilerTimeSectionId.E_SECTION_MAX_START_TIME,
                                               EProfilerTimeSectionId.E_SECTION_MAX_END_TIME};
            
            verifyTimeLimits(m_funcTimes, scopes, types, codeStat);
            
            
            assertEquals(String.valueOf(m_hitsFuncLow), codeStat.getHits(0));
            assertEquals(String.valueOf(m_hitsFuncHigh), codeStat.getHits(1));

            
            CTestProfilerStatistics dataStat = prof.getArea(CTestAnalyzerProfiler.EAreaType.DATA_AREA, 0);
            assertEquals(m_profilerDataArea, dataStat.getAreaName() + '/' + dataStat.getAreaValue());
            
            EProfilerStatisticsSectionId [] dataScopes = {EProfilerStatisticsSectionId.E_SECTION_NET_TIME,
                                                          EProfilerStatisticsSectionId.E_SECTION_OUTSIDE_TIME,
                                                          EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME};
            verifyTimeLimits(m_dataTimes, dataScopes, types, dataStat);
            
            assertEquals(String.valueOf(m_hitsDataLow), dataStat.getHits(0));
            assertEquals(String.valueOf(m_hitsDataHigh), dataStat.getHits(1));
            
            CTestProfilerTime netTime = dataStat.getTime(EProfilerStatisticsSectionId.E_SECTION_NET_TIME, true);
            String nlComment = netTime.getCommentForSeqElement(EProfilerTimeSectionId.E_SECTION_MIN_TIME.swigValue(), 0, CommentType.NEW_LINE_COMMENT);
            String eolComment = netTime.getCommentForSeqElement(EProfilerTimeSectionId.E_SECTION_MIN_TIME.swigValue(), 0, CommentType.END_OF_LINE_COMMENT);
            assertEquals("    # " + m_nlComment1 + '\n', nlComment);
            assertEquals("    # " + m_eolComment1 + '\n', eolComment);
            
            CTestProfilerTime outsideTime = dataStat.getTime(EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME, true);
            nlComment = outsideTime.getCommentForSeqElement(EProfilerTimeSectionId.E_SECTION_MIN_START_TIME.swigValue(), 1, CommentType.NEW_LINE_COMMENT);
            eolComment = outsideTime.getCommentForSeqElement(EProfilerTimeSectionId.E_SECTION_MIN_START_TIME.swigValue(), 1, CommentType.END_OF_LINE_COMMENT);
            assertEquals("    # " + m_nlComment2 + '\n', nlComment);
            assertEquals("    # " + m_eolComment2 + '\n', eolComment);
        }

        
        private void verifyTimeLimits(String[][]times,
                                      EProfilerStatisticsSectionId[] scopes,                                      
                                      EProfilerTimeSectionId[] types,
                                      CTestProfilerStatistics stat) {
            int scopeIdx = 0;
            for (EProfilerStatisticsSectionId scope : scopes) {
                CTestProfilerTime timeScope = stat.getTime(scope, true);
                int timeIdx = 0;
                for (EProfilerTimeSectionId type : types) {
                    
                    // skip total time of period time
                    if (!times[scopeIdx][timeIdx].isEmpty()) {
                        assertEquals(times[scopeIdx][timeIdx], timeScope.getTime(type, 0));
                        assertEquals(times[scopeIdx + 1][timeIdx], timeScope.getTime(type, 1));
                    }
                    timeIdx++;
                }
                
                scopeIdx += 2;
            }
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            // ids.add(SectionIds.E_SECTION_PROFILER);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_PROFILER, command);
        }
        

        @Override
        void areSectionsEmpty(CTestSpecification testSpec) throws Exception {
            super.areSectionsEmpty(testSpec);
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            assertTrue("Coverage section should be empty!", analyzer.getProfiler(true).isEmpty());
        }
        
        
        @Override
        void verifyPastedSection(CTestSpecification testSpec, boolean isAnalyzerSubSection) {
            super.verifyPastedSection(testSpec, true);
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            assertTrue("Coverage section should be empty!", analyzer.getCoverage(true).isEmpty());
            assertFalse("Profiler section should NOT be empty!", analyzer.getProfiler(true).isEmpty());
            assertTrue("Trace section should be empty!", analyzer.getTrace(true).isEmpty());
        }        
    }
    
    
    class TraceSection extends SectionTest {

        boolean m_isActive = true;
        String m_traceExportFormat = "XML";
        String m_traceExportFile = "traceExport.xml";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionTrace("Yes", m_traceExportFormat, m_traceExportFile);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true); 
            CTestAnalyzerTrace trace = analyzer.getTrace(true);
            assertTrue(trace.isActive() == ETristate.E_TRUE);
            assertEquals(ETraceExportFormat.EX_TrcAsXML, trace.getExportFormat());
            assertEquals(m_traceExportFile, trace.getExportFileName());
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            // ids.add(SectionIds.E_SECTION_TRACE);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_ANALYZER, UITestUtils.SEC_TRACE, command);
        }
        
        
        @Override
        void areSectionsEmpty(CTestSpecification testSpec) throws Exception {
            super.areSectionsEmpty(testSpec);
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            assertTrue("Coverage section should be empty!", analyzer.getTrace(true).isEmpty());
        }
        
        
        @Override
        void verifyPastedSection(CTestSpecification testSpec, boolean isAnalyzerSubSection) {
            super.verifyPastedSection(testSpec, true);
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            assertTrue("Coverage section should be empty!", analyzer.getCoverage(true).isEmpty());
            assertTrue("Profiler section should be empty!", analyzer.getProfiler(true).isEmpty());
            assertFalse("Trace section should NOT be empty!", analyzer.getTrace(true).isEmpty());
        }        
    }
    
    
    class HILSection extends SectionTest {

        String hilPath = "digit/out";
        String hilValue = "0";

        @Override
        public void setSection() throws Exception {
            m_utils.setSectionHIL(0, hilPath, hilValue, false);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestHIL hil = testSpec.getHIL(true);
            StrVector hilKeysVector = new StrVector();
            hil.getHILParamKeys(hilKeysVector);
            assertEquals(hilPath, hilKeysVector.get(0));
            
            StrVector hilValuesVector = new StrVector();
            hil.getHILParamValues(hilValuesVector);
            assertEquals(hilValue, hilValuesVector.get(0));
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_HIL);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_HIL, command);
        }
    }
    
    
    class ScriptsSection extends SectionTest {

        String initTargetFunc = "initTargetFunc";
        String initTargetFuncParams = "initTargetFuncParams";
        String initFunc = "initFunc";
        String initFuncParams = "initFuncParams";
        String endFunc = "endFunc";
        String endFuncParams = "endFuncParams";
        String restoreTargetFunc = "restoreTargetFunc";
        String restoreTargetFuncParams = "restoreTargetFuncParams";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionScripts(initTargetFunc, initTargetFuncParams, initFunc, initFuncParams, 
                                      endFunc, endFuncParams, restoreTargetFunc, restoreTargetFuncParams,
                                      false);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestFunction initTF = testSpec.getInitTargetFunction(true);
            assertEquals(initTargetFunc, initTF.getName());
            StrVector initTFParams = new StrVector();
            initTF.getPositionParams(initTFParams);
            assertEquals(initTargetFuncParams, initTFParams.get(0));
            
            CTestFunction initF = testSpec.getInitFunction(true);
            assertEquals(initFunc, initF.getName());
            StrVector initFParams = new StrVector();
            initF.getPositionParams(initFParams);
            assertEquals(initFuncParams, initFParams.get(0));
            
            CTestFunction endF = testSpec.getEndFunction(true);
            assertEquals(endFunc, endF.getName());
            StrVector endFParams = new StrVector();
            endF.getPositionParams(endFParams);
            assertEquals(endFuncParams, endFParams.get(0));
            
            CTestFunction restoreTF = testSpec.getRestoreTargetFunction(true);
            assertEquals(restoreTargetFunc, restoreTF.getName());
            StrVector restoreTFParams = new StrVector();
            restoreTF.getPositionParams(restoreTFParams);
            assertEquals(restoreTargetFuncParams, restoreTFParams.get(0));
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_INIT_TARGET);
            ids.add(SectionIds.E_SECTION_INITFUNC);
            ids.add(SectionIds.E_SECTION_ENDFUNC);
            ids.add(SectionIds.E_SECTION_RESTORE_TARGET);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_SCRIPTS, command);
        }
    }
    
    
    class OptionsSection extends SectionTest {

        String optionPath = "/IDE/opt1";
        String optionValue = "Off";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionOptions(0, optionPath, optionValue, false);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            StrVector optKeys = new StrVector();
            testSpec.getOptionKeys(optKeys);
            assertEquals(optionPath, optKeys.get(0));
                    
            StrVector optValues = new StrVector();
            testSpec.getOptionValues(optValues);
            assertEquals(optionValue, optValues.get(0));
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_OPTIONS);
            return ids;
        }

        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_OPTIONS, command);
        }
    }
    
    
    class DryRunSection extends SectionTest {

        String m_hostVar = "${expectedVal}";
        String m_hotVarValue = "3598";
        ETristate m_isUpdateCvrg = ETristate.E_TRUE;
        ETristate m_isUpdateProfiler = ETristate.E_TRUE;
        String m_profilerStatMultiplier = "0.34";
        String m_profilerStatOffset = "25.7";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionDryRunTable(0, m_hostVar, m_hotVarValue, false);
            m_utils.setSectionDryRun(m_isUpdateCvrg, m_isUpdateProfiler, 
                                     m_profilerStatMultiplier, m_profilerStatOffset);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestDryRun dryRun = testSpec.getDryRun(true);
            CMapAdapter assignments = dryRun.getAssignments(true);
            assertEquals(m_hotVarValue, assignments.getValue(m_hostVar));
            
            assertEquals(m_isUpdateCvrg, dryRun.isUpdateCoverage());
            assertEquals(m_isUpdateProfiler, dryRun.isUpdateProfiler());
            assertEquals(m_profilerStatMultiplier, dryRun.getProfilerMultiplier());
            assertEquals(m_profilerStatOffset, dryRun.getProfilerOffset());
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_DRY_RUN);
            return ids;
        }

        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_DRY_RUN, command);
        }
    }
    
    
    class DiagramsSection extends SectionTest {

        ETristate m_isActive = ETristate.E_TRUE;
        EDiagType m_diagType = EDiagType.ESequenceDiagram; //"sequenceDiagram";
        String m_script = "diagScript";
        String m_params = "a";
        String m_outFile = "diagFile.svg";
        ETristate m_isAddToReport = ETristate.E_TRUE;
        EViewerType m_viewer = EViewerType.ESinglePage;
        EViewFormat m_dataFormat = EViewFormat.ESVG;
        String m_externalViewer = "explorer";
        
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionDiagrams(0, m_isActive, m_diagType, m_script, m_params,
                                       m_outFile, m_isAddToReport, m_viewer,
                                       m_dataFormat, m_externalViewer);
        }

        @Override
        public void testSection(CTestSpecification testSpec) throws Exception {
            CTestDiagrams diagrams = testSpec.getDiagrams(true);
            CTestBaseList diagCfgs = diagrams.getConfigurations(true);
            assertEquals(1, diagCfgs.size());

            CTestDiagramConfig diagCfg = CTestDiagramConfig.cast(diagCfgs.get(0));
            assertEquals(m_isActive, diagCfg.isActive());
            assertEquals(m_diagType, diagCfg.getDiagramType());
            assertEquals(m_script, diagCfg.getScriptName());
            
            CSequenceAdapter params = diagCfg.getParameters(true);
            assertEquals(1, params.size());
            assertEquals(m_params, params.getValue(0));
            assertEquals(m_outFile, diagCfg.getOutputFileName());
            assertEquals(m_isAddToReport, diagCfg.isAddToReport());
            assertEquals(m_viewer, diagCfg.getViewerType());
            assertEquals(m_dataFormat, diagCfg.getViewFormat());
            assertEquals(m_externalViewer, diagCfg.getExternalViewerName());
        }

        
        @Override
        public Set<SectionIds> getSectionIds() {
            TreeSet<SectionIds> ids = new TreeSet<SectionIds>();
            ids.add(SectionIds.E_SECTION_DIAGRAMS);
            return ids;
        }

        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_DIAGRAMS, command);
        }
    }
}


