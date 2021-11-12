package si.isystem.uitest;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerCoverage.ECoverageExportFormat;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFilter.EFilterTypes;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.DataUtils;
import si.isystem.swtbot.utils.KTableTestUtils;

@RunWith(SWTBotJunit4ClassRunner.class)
public class GroupsTester {

    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;
    private static KTableTestUtils m_ktableUtils;


    final static String SPEC_FILE_NAME = "D:/tmp/testIDEA-group-test-temp-file.iyaml";
    private static final String GROUP_ID = "grp_id";
    private static final String GROUP_POSTFIX = "(0) coreX/partitionA+sample.elf/main.c/min_int+max_int";
    
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
    }
    
    
    @Test
    public void testCreateAndRunGroups() throws Exception {
        
        boolean isCreateTests = false;
        
        final String fileName = "D:/tmp/testIDEA-groups-test-temp-file.iyaml";
        if (isCreateTests  ||  !m_utils.isFileCreatedToday(fileName)) {
            m_utils.discardExistingAndCreateNewProject();
            m_utils.createUnitTestsForListUtils();
            UITestUtils.getActiveModel().saveModelAs(fileName);
        } else {
            m_utils.loadTestSpecFromFile(fileName);
        }
        
        m_utils.clickToolbarButton(UITestUtils.TOOLTIP_CONNECT_TO_WIN_IDEA);
        m_utils.waitForConnectionDialog();
        m_bot.sleep(500); // wait until connects
        
        createGroupsWithWizard();

        m_utils.createNewGroupWithContextCmd("customGroup", "test-2, test-5, test-8");
        m_utils.createNewSubGroupWithContextCmd("customGroup", UITestUtils.EMPTY_GROUP_POSTFIX,
                                                "customSubGroup", 
                "test-8");
        m_bot.sleep(1000); // refreshing too fast cancels changes introduced by the last dialog
        m_utils.clickToolbarButton(UITestUtils.TOOLTIP_REFRESH_SYMBOLS);

        setGroupsCoverage();
        
        runTestsInGroup();
        m_utils.setGroupResultComment("g0", 
                                      "(13) //src\\\\listUtils.c/",
                                      "This group of tests was executed on full moon!");
        
        verifyGroupStatistics();
        verifyFunctionStatistics();
        
        // check also for group result!
        saveReportAndVerifySchema();
    }

    
    private void verifyGroupStatistics() throws Exception {
        m_utils.selectTestSection("Group statistics");
        m_ktableUtils.selectKTable(0);
        assertEquals("13", m_ktableUtils.getContentAsString(1, 1)); 
        assertEquals("5", m_ktableUtils.getContentAsString(1, 2)); 
        assertEquals("1", m_ktableUtils.getContentAsString(1, 3)); 
        assertEquals("1", m_ktableUtils.getContentAsString(1, 4));
        
        assertEquals("/", m_ktableUtils.getContentAsString(2, 1)); 
        assertEquals("80.0% (4/5)", m_ktableUtils.getContentAsString(2, 2)); 
        assertEquals("100.0% (1/1)", m_ktableUtils.getContentAsString(2, 3)); 
        assertEquals("100.0% (1/1)", m_ktableUtils.getContentAsString(2, 4));
        
        assertEquals("/", m_ktableUtils.getContentAsString(3, 1)); 
        assertEquals("1", m_ktableUtils.getContentAsString(3, 2)); 
        assertEquals("0", m_ktableUtils.getContentAsString(3, 3)); 
        assertEquals("0", m_ktableUtils.getContentAsString(3, 4));
        
        assertEquals("/", m_ktableUtils.getContentAsString(4, 1)); 
        assertEquals("2.6", m_ktableUtils.getContentAsString(4, 2)); 
        assertEquals("13.0", m_ktableUtils.getContentAsString(4, 3)); 
        assertEquals("13.0", m_ktableUtils.getContentAsString(4, 4));


        m_ktableUtils.selectKTable(1);
        assertEquals("92.3% (12/13)", m_ktableUtils.getContentAsString(1, 1)); 
        assertEquals("60.0% (3/5)", m_ktableUtils.getContentAsString(1, 2)); 
        assertEquals("0.0% (0/1)", m_ktableUtils.getContentAsString(1, 3)); 
        assertEquals("0.0% (0/1)", m_ktableUtils.getContentAsString(1, 4));
        
        assertEquals("7.7% (1/13)", m_ktableUtils.getContentAsString(2, 1)); 
        assertEquals("20.0% (1/5)", m_ktableUtils.getContentAsString(2, 2)); 
        assertEquals("100.0% (1/1)", m_ktableUtils.getContentAsString(2, 3)); 
        assertEquals("100.0% (1/1)", m_ktableUtils.getContentAsString(2, 4));
        
        assertEquals("0.0% (0/13)", m_ktableUtils.getContentAsString(3, 1)); 
        assertEquals("0.0% (0/5)", m_ktableUtils.getContentAsString(3, 2)); 
        assertEquals("0.0% (0/1)", m_ktableUtils.getContentAsString(3, 3)); 
        assertEquals("0.0% (0/1)", m_ktableUtils.getContentAsString(3, 4));
    }


    private void verifyFunctionStatistics() throws Exception {
        m_utils.selectTestSection("Function statistics");
        m_ktableUtils.selectKTable(0);
        
        assertEquals("countGreaterThan", m_ktableUtils.getContentAsString(2, 1)); 
        assertEquals("countLessThan", m_ktableUtils.getContentAsString(2, 2)); 
        assertEquals("maxElement", m_ktableUtils.getContentAsString(2, 3)); 
        assertEquals("minElement", m_ktableUtils.getContentAsString(2, 4));
        assertEquals("sumList", m_ktableUtils.getContentAsString(2, 5));
        
        assertEquals("3", m_ktableUtils.getContentAsString(3, 1)); 
        assertEquals("0", m_ktableUtils.getContentAsString(3, 2)); 
        assertEquals("4", m_ktableUtils.getContentAsString(3, 3)); 
        assertEquals("3", m_ktableUtils.getContentAsString(3, 4));
        assertEquals("3", m_ktableUtils.getContentAsString(3, 5));
        
        assertEquals("3 / 0 / 0", m_ktableUtils.getContentAsString(4, 1)); 
        assertEquals("0 / 0 / 0", m_ktableUtils.getContentAsString(4, 2)); 
        assertEquals("4 / 0 / 0", m_ktableUtils.getContentAsString(4, 3)); 
        assertEquals("2 / 1 / 0", m_ktableUtils.getContentAsString(4, 4));
        assertEquals("3 / 0 / 0", m_ktableUtils.getContentAsString(4, 5));

        assertEquals("100.0% (72/72)", m_ktableUtils.getContentAsString(5, 1)); 
        assertEquals("0.0% (0/72)", m_ktableUtils.getContentAsString(5, 2)); 
        assertEquals("100.0% (72/72)", m_ktableUtils.getContentAsString(5, 3)); 
        assertEquals("100.0% (72/72)", m_ktableUtils.getContentAsString(5, 4));
        assertEquals("100.0% (64/64)", m_ktableUtils.getContentAsString(5, 5));
        
        assertEquals("100.0% (0f, 0t, 2b) / 4", m_ktableUtils.getContentAsString(6, 1)); 
        assertEquals("0.0% (0f, 0t, 0b) / 4", m_ktableUtils.getContentAsString(6, 2)); 
        assertEquals("100.0% (0f, 0t, 2b) / 4", m_ktableUtils.getContentAsString(6, 3)); 
        assertEquals("100.0% (0f, 0t, 2b) / 4", m_ktableUtils.getContentAsString(6, 4));
        assertEquals("100.0% (0f, 0t, 1b) / 2", m_ktableUtils.getContentAsString(6, 5));

        assertEquals("3", m_ktableUtils.getContentAsString(7, 1)); 
        assertEquals("0", m_ktableUtils.getContentAsString(7, 2)); 
        assertEquals("4", m_ktableUtils.getContentAsString(7, 3)); 
        assertEquals("3", m_ktableUtils.getContentAsString(7, 4));
        assertEquals("3", m_ktableUtils.getContentAsString(7, 5));
    }


    private void saveReportAndVerifySchema() throws Exception {
        // let's first modify filter, so that all fields get saved. 
        m_utils.selectTestGroup("customGroup", "(3) ///");
        m_utils.selectTestSection("Filter");
        
        m_utils.setSectionGroupFilter("Built-in filter",
                                  "core_0",
                                  "atm33.elf",
                                  "main.c, test.c",
                                      
                                  "func1, func2",
                                  "func3",
                                  "id1, id2, id3",
                                  "id4",
                                      
                                  "tag1, tag2, tag3, tag4",
                                  "And",
                                      
                                  "t1",
                                  "Or",
                                      
                                  "t7",
                                  "And",
                                  "tag7, tag8");
        
        String outFile = "D:/tmp/reportFull.xml";
        m_utils.saveReport(true, outFile, true);
        m_utils.validateSchema(UITestUtils.REPORT_SCHEMA_LOCATION, outFile);
    }


    private void setGroupsCoverage() throws Exception {
        m_utils.setGroupCoverage("g0", "(13) //src\\\\listUtils.c/", 
                                 new int[]{60, 0, 0, 0, 0, 0}, 
                                 new int[]{60, 0, 0, 0, 0, 0});
        
        m_utils.setGroupCoverage("customGroup", "(3) ///", 
                                 new int[]{1, 0, 0, 0, 0, 0}, 
                                 new int[]{60, 0, 0, 0, 0, 0});
    }


    private void runTestsInGroup() throws Exception {
        m_utils.selectTestGroup("g0", "(13) //src\\\\listUtils.c/");
        
        m_utils.toolbarRunSelectedAndDerived();
        m_utils.waitForProgressDialog();
        
        String statusText = m_utils.getStatusViewText();
        assertContains("Test report for selected editor, 13 test(s), 7 group(s):\n" +
                       "- 12 tests (92%) completed successfully\n" +
                       "- 1 test (8%) failed (invalid results)\n" +
                       
                       // remove the next two lines and uncomment one, when testOverloads()
                       // will be fixed in coverage XML export.
                       "- 6 groups (86%) completed successfully\n" +
                       "- 1 group (14%) with error (failed evaluation)",        
                       // "- 7 groups (100%) completed successfully",
                       statusText);
    }


    private void createGroupsWithWizard() {
        m_utils.openDialogWithMainMenu(UITestUtils.MENU_TOOLS, 
                                       UITestUtils.MENU_TOOLS__CREATE_GROUPS, 
                                       "Create groups of test cases");
        
        m_bot.buttonWithTooltip("Refresh symbols").click();
        
        SWTBotTree tree = m_bot.tree();
        tree.setFocus();
        SWTBotTreeItem subtree = tree.getTreeItem("<default core>");
        SWTBotTreeItem subsubtree = subtree.getNode("stm32.elf");
        final SWTBotTreeItem listUtilsNode = subsubtree.getNode("src\\\\listUtils.c");
        listUtilsNode.toggleCheck();
        
        m_bot.button("OK").click();

    }


    @Test
    public void testAllFields() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        
        createGroupsWithWizard();
        
        testListOfSections(GROUP_ID, GROUP_POSTFIX);       
    }

    
    private void testListOfSections(String groupId, String postFix) throws Exception {
        
        List<GroupSectionTest> sectionTests = createSectionsListForUnitTtest();
        
        for (GroupSectionTest sectionTest : sectionTests) {
            sectionTest.setSection();
        }
        
        // Save and load the group, so that we also test serialization and parsing.
        
        // m_utils.getToolbarButton("Save \\(.*").click();
        m_utils.pressKey(SWT.TAB);
        UITestUtils.getActiveModel().saveModelAs(SPEC_FILE_NAME);
        m_utils.discardExistingAndCreateNewProject();
        m_utils.loadTestSpecFromFile(SPEC_FILE_NAME);
        
        m_utils.selectTestGroup(groupId, postFix);
        m_utils.copyToClipboard();
        CTestGroup testGroup = CTestGroup.cast(m_utils.getTestGroupFromClipboard().getChildren(true).get(0));

        for (GroupSectionTest sectionTest : sectionTests) {
            sectionTest.testSection(testGroup);
        }
    }


    @Test
    public void testClearSections() throws Exception {

        List<GroupSectionTest> sectionTests = initModel();

        for (GroupSectionTest test : sectionTests) {

            test.contextMenu(UITestUtils.CMD_CLEAR_SECTION);
            
            if (test instanceof MetaSection) {
                // test ID is cleared with Meta section
                m_utils.selectTestGroup("/", GROUP_POSTFIX); 
            } else if (test instanceof FilterSection) {
                // postfix is cleared with Filter section
                m_utils.selectTestGroup(GROUP_ID, UITestUtils.EMPTY_GROUP_POSTFIX); 
            } else {
                m_utils.selectTestGroup(GROUP_ID, GROUP_POSTFIX); 
            }
            
            m_utils.copyToClipboard();
            CTestGroup testGroup = CTestGroup.cast(m_utils.getTestGroupFromClipboard().getChildren(true).get(0));
            
            test.areSectionsEmpty(testGroup);
            
            m_bot.sleep(200); // just to make it easier for humans to observe
            m_utils.undo();
            m_bot.sleep(200);
        }
    }


    protected List<GroupSectionTest> initModel() throws Exception {
        m_utils.discardExistingAndCreateNewProject();
        List<GroupSectionTest> sectionTests = createSectionsListForUnitTtest();
        createGroupsWithWizard();
        for (GroupSectionTest sectionTest : sectionTests) {
            sectionTest.setSection();
        }
        return sectionTests;
    }
    
    
    @Test
    public void testCopyPasteSections() throws Exception {
        
        List<GroupSectionTest> sectionTests = initModel();

        m_utils.createNewGroupWithContextCmd("", "");

        for (GroupSectionTest test : sectionTests) {

            m_utils.selectTestGroup(GROUP_ID, GROUP_POSTFIX);
            m_bot.sleep(500);
            
            test.contextMenu(UITestUtils.CMD_COPY_SECTION);

            m_utils.selectTestGroup("/", UITestUtils.EMPTY_GROUP_POSTFIX);
            m_bot.sleep(500);

            test.contextMenu(UITestUtils.CMD_PASTE_SECTION);
            
            m_bot.sleep(500);
            
            m_utils.copyToClipboard();
            CTestGroup destTestGroup = CTestGroup.cast(m_utils.getTestGroupFromClipboard().getChildren(true).get(0));

            test.verifyPastedSection(destTestGroup);
            
            m_bot.sleep(200); // just to make it easier for humans to observe
            m_utils.undo();
            m_bot.sleep(200);
        }
    }

    
    @Test
    public void testCutPasteSections() throws Exception {
        
        List<GroupSectionTest> sectionTests = initModel();
        m_utils.createNewGroupWithContextCmd("", "");

        for (GroupSectionTest test : sectionTests) {
            m_utils.selectTestGroup(GROUP_ID, GROUP_POSTFIX); 
            m_bot.sleep(500);
            
            test.contextMenu(UITestUtils.CMD_CUT_SECTION);

            // check that pasted sections are NOT empty in dest test spec
            m_utils.selectTestGroup("/", UITestUtils.EMPTY_GROUP_POSTFIX);
            m_bot.sleep(500);

            test.contextMenu(UITestUtils.CMD_PASTE_SECTION);
            m_bot.sleep(500);

            if (test instanceof MetaSection) {
                // group ID is cleared with Meta section
                m_utils.selectTestGroup("/", GROUP_POSTFIX); 
            } else if (test instanceof FilterSection) {
                // postfix is cleared with Filter section
                m_utils.selectTestGroup(GROUP_ID, UITestUtils.EMPTY_GROUP_POSTFIX); 
            } else {
                m_utils.selectTestGroup(GROUP_ID, GROUP_POSTFIX); 
            }

            m_bot.sleep(250);
            // check that cut sections are empty in src test spec
            m_utils.copyToClipboard();
            CTestGroup srcTestGroup = CTestGroup.cast(m_utils.getTestGroupFromClipboard().getChildren(true).get(0));
            
            test.areSectionsEmpty(srcTestGroup);
            
            // check that pasted sections are NOT empty in dest test spec
            if (test instanceof MetaSection) {
                // test ID is pasted in Meta section
                m_utils.selectTestGroup(GROUP_ID, UITestUtils.EMPTY_GROUP_POSTFIX); 
            } else if (test instanceof FilterSection) {
                // postfix is pasted in Filter section
                m_utils.selectTestGroup("/", GROUP_POSTFIX); 
            } else {
                m_utils.selectTestGroup("/", UITestUtils.EMPTY_GROUP_POSTFIX); 
            }
            
            m_utils.copyToClipboard();
            CTestGroup destTestGroup = CTestGroup.cast(m_utils.getTestGroupFromClipboard().getChildren(true).get(0));
            
            test.verifyPastedSection(destTestGroup);
            
            m_bot.sleep(200); // just to make it easier for humans to observe
            m_utils.undo();  // undo paste on dest
            m_bot.sleep(200);
            m_utils.undo();  // undo cut on src
        }
    }
    

    private List<GroupSectionTest> createSectionsListForUnitTtest() {
        List<GroupSectionTest> sectionTests = new ArrayList<GroupSectionTest>();

        sectionTests.add(new MetaSection());
        sectionTests.add(new FilterSection());
        sectionTests.add(new CoverageConfigSection());
        sectionTests.add(new CoverageResultsSection());
        sectionTests.add(new ScriptsSection());
        
        return sectionTests;
    }
    
    
    
    abstract class GroupSectionTest {
        
        abstract void setSection() throws Exception;
        abstract void testSection(CTestGroup testSpec) throws Exception;
        abstract void contextMenu(String command) throws Exception;
        abstract Set<ESectionCTestGroup> getSectionIds();
        
        void areSectionsEmpty(CTestGroup testGroup) throws Exception {
            
            ESectionCTestGroup[] allIds = getIdsToBeTested();
            Set<ESectionCTestGroup> subIds = getSectionIds();
            for (ESectionCTestGroup id : allIds) {
                System.out.println("- id: "+ id);
                if (subIds.contains(id)) {
                    assertTrue("Cleared section is not empty: " + id.toString(), testGroup.isSectionEmpty(id.swigValue()));
                } else {
                    if (testGroup.isSectionEmpty(id.swigValue())) {
                        assertFalse("Non-cleared section is empty: " + id.toString(), true);
                    }
                }
            }
        }

        
        void verifyPastedSection(CTestGroup testGroup) {
            
            ESectionCTestGroup[] allIds = getIdsToBeTested();
            Set<ESectionCTestGroup> testSectionsInEditor = getSectionIds();
            
            for (ESectionCTestGroup id : allIds) {
                
                if (testSectionsInEditor.contains(id)) {
                    assertFalse("Pasted section is empty: " + id.toString(), testGroup.isSectionEmpty(id.swigValue()));
                } else {
                    assertTrue("Non-pasted section is not empty: " + id.toString(), testGroup.isSectionEmpty(id.swigValue()));
                }
            }
        }

        
        protected ESectionCTestGroup[] getIdsToBeTested() {
            ESectionCTestGroup[] allIds = new ESectionCTestGroup[] {            
                    ESectionCTestGroup.E_SECTION_GROUP_ID, 
                    // ESectionCTestGroup.E_SECTION_IS_EXECUTE, not used at the moment
                    ESectionCTestGroup.E_SECTION_DESCRIPTION, 
                    ESectionCTestGroup.E_SECTION_FILTER,

                    ESectionCTestGroup.E_SECTION_MERGED_ANALYZER_FILE,
                    ESectionCTestGroup.E_SECTION_CLOSE_ANALYZER_FILE,
                    ESectionCTestGroup.E_SECTION_COVERAGE_EXPORT,

                    ESectionCTestGroup.E_SECTION_COVERAGE_ALL_CODE_IN_GROUP,
                    ESectionCTestGroup.E_SECTION_COVERAGE_TEST_CASES_ONLY,

                    ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT,
                    ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT,

                    };
            return allIds;
        }
    }
    
    
    class MetaSection extends GroupSectionTest {

        private String m_grpId = GROUP_ID;
        private String m_desc = "This is typical desc.";
        
        @Override
        void setSection() throws Exception {
            m_utils.setSectionGroupMeta(m_grpId, m_desc);
        }

        @Override
        void testSection(CTestGroup testGroup) throws Exception {
            assertEquals(m_grpId, testGroup.getId());
            assertEquals(m_desc, testGroup.getDescription());
        }

        @Override
        void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_META, command);
        }

        @Override
        Set<ESectionCTestGroup> getSectionIds() {
            TreeSet<ESectionCTestGroup> ids = new TreeSet<ESectionCTestGroup>();
            ids.add(ESectionCTestGroup.E_SECTION_GROUP_ID);
            ids.add(ESectionCTestGroup.E_SECTION_DESCRIPTION);
            return ids;
        }
    }
    

    class FilterSection extends GroupSectionTest {

        private String m_core = "coreX";
        private String m_partitions = "partitionA, sample.elf";
        private String m_modules = "main.c";
        private String m_funcs = "min_int, max_int";
        private String m_exclFuncs = "func1";
        private String m_ids = "test-1";
        private String m_exclIds = "test-2";
        private String m_mustHaveAllTags = "tag1, tag2";
        private String m_mustHaveAtLeastOneOfTags = "tagY";
        private String m_mustNotHaveAnyOfTags = "tagZ";
        private String m_mustNotHaveAtLEastOneOfTags = "tagABC";
        
        @Override
        void setSection() throws Exception {
            m_utils.setSectionGroupFilter("Built-in filter",
                                      m_core,
                                      m_partitions,
                                      m_modules,

                                      m_funcs,
                                      m_exclFuncs,
                                      m_ids,
                                      m_exclIds,

                                      m_mustHaveAllTags, "And",
                                      m_mustHaveAtLeastOneOfTags, "Or",
                                      m_mustNotHaveAnyOfTags, "And",
                                      m_mustNotHaveAtLEastOneOfTags);
        }

        @Override
        void testSection(CTestGroup testGroup) throws Exception {
            CTestFilter filter = testGroup.getFilter(true);
            assertEquals(EFilterTypes.BUILT_IN_FILTER, filter.getFilterType());
            assertEquals(m_core, filter.getCoreId());
            
            StrVector items = new StrVector();
            filter.getPartitions(items);
            assertEquals(m_partitions, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getModules(items);
            assertEquals(m_modules, DataUtils.listToString(DataUtils.strVectorToList(items)));

            filter.getIncludedFunctions(items);
            assertEquals(m_funcs, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getExcludedFunctions(items);
            assertEquals(m_exclFuncs, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getIncludedIds(items);
            assertEquals(m_ids, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getExcludedIds(items);
            assertEquals(m_exclIds, DataUtils.listToString(DataUtils.strVectorToList(items)));

            filter.getMustHaveAllTags(items);
            assertEquals(m_mustHaveAllTags, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getMustHaveOneOfTags(items);
            assertEquals(m_mustHaveAtLeastOneOfTags, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getMustNotHaveAllTags(items);
            assertEquals(m_mustNotHaveAnyOfTags, DataUtils.listToString(DataUtils.strVectorToList(items)));
            filter.getMustNotHaveOneOfTags(items);
            assertEquals(m_mustNotHaveAtLEastOneOfTags, DataUtils.listToString(DataUtils.strVectorToList(items)));

            assertFalse(filter.isOrTags1());
            assertTrue(filter.isOrTags2());
            assertFalse(filter.isOrTags3());
        }

        @Override
        void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_GRP_FILTER, command);
        }

        @Override
        Set<ESectionCTestGroup> getSectionIds() {
            TreeSet<ESectionCTestGroup> ids = new TreeSet<ESectionCTestGroup>();
            ids.add(ESectionCTestGroup.E_SECTION_FILTER);
            return ids;
        }

    }
    
    
    class CoverageConfigSection extends GroupSectionTest {

        String m_exportFmt = "XML";
        private String m_cvrgExportFile = "file.xml";
        private String m_variant = "xml2";
        private String m_modFilter = "utils.c";
        private String m_funcFilter = "fx";
        private String m_analFile = "analyzer.trd";        
        
        @Override
        void setSection() throws Exception {
            m_utils.setSectionCoverageConfigGroup("Yes", 
                                                  m_exportFmt, m_cvrgExportFile, 
                                                  m_variant,  
                                                  "No", "Yes",
                                                  "No", "Yes", "No",       
                                                  "No", "Yes", "No", 
                                                  m_modFilter, m_funcFilter);
            
            m_utils.enterTextWLabel("Merged analyzer file:", m_analFile);
            m_utils.clickRadioWLabel("Yes", 1); // close after test 
        }

        
        @Override
        void testSection(CTestGroup testGroup) throws Exception {
            CTestAnalyzerCoverage cvrgCfg = testGroup.getCoverageExportConfig(true);
            assertEquals(ETristate.E_TRUE, cvrgCfg.isActive());
            assertEquals(ECoverageExportFormat.EX_CCAsXML, cvrgCfg.getExportFormat());
            assertEquals(m_cvrgExportFile, cvrgCfg.getExportFileName());
            assertEquals(m_variant, cvrgCfg.getExportFormatVariant());
            assertEquals(m_modFilter, cvrgCfg.getExportModulesFilter());
            assertEquals(m_funcFilter, cvrgCfg.getExportFunctionsFilter());
            
            assertEquals(ETristate.E_FALSE, cvrgCfg.isIgnoreNonReachableCode());
            assertEquals(ETristate.E_TRUE, cvrgCfg.isProvideAssemblerInfo());
            
            assertEquals(ETristate.E_FALSE, cvrgCfg.isExportModuleLines());
            assertEquals(ETristate.E_TRUE, cvrgCfg.isExportFunctionLines());
            assertEquals(ETristate.E_FALSE, cvrgCfg.isExportSources());
            
            assertEquals(ETristate.E_FALSE, cvrgCfg.isExportAsm());
            assertEquals(ETristate.E_TRUE, cvrgCfg.isExportRanges());
            assertEquals(ETristate.E_FALSE, cvrgCfg.isLaunchViewer());
            
            assertEquals(true, testGroup.isCloseAfterTest());
            assertEquals(m_analFile, testGroup.getMergedAnalyzerFileName());
        }

        
        @Override
        void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_GRP_COVERAGE_CONFIG, command);
        }

        
        @Override
        Set<ESectionCTestGroup> getSectionIds() {
            TreeSet<ESectionCTestGroup> ids = new TreeSet<ESectionCTestGroup>();
            ids.add(ESectionCTestGroup.E_SECTION_COVERAGE_EXPORT);
            ids.add(ESectionCTestGroup.E_SECTION_MERGED_ANALYZER_FILE);
            ids.add(ESectionCTestGroup.E_SECTION_CLOSE_ANALYZER_FILE);
            return ids;
        }
    }
    
    
    class CoverageResultsSection extends GroupSectionTest {

        private int [] m_all = new int[]{1, 2, 3, 4, 5, 6}; 
        private int [] m_tested = new int[]{10, 20, 30, 40, 50, 60}; 
        
        
        @Override
        void setSection() throws Exception {
            m_utils.setSectionCoverageResults(m_all, m_tested);
        }

        
        @Override
        void testSection(CTestGroup testGroup) throws Exception {
            CTestCoverageStatistics cvrgRes = testGroup.getCoverageStatForAllCodeInGroup(true);
            verifyCoverageStat(m_all, cvrgRes);
            
            cvrgRes = testGroup.getCoverageStatForTestCasesOnly(true);
            verifyCoverageStat(m_tested, cvrgRes);            
        }

        
        protected void verifyCoverageStat(int stats[], CTestCoverageStatistics cvrgRes) {
            assertEquals(Integer.toString(stats[0]), cvrgRes.getBytesExecutedText());
            assertEquals(Integer.toString(stats[1]), cvrgRes.getSourceLinesExecutedText());
            assertEquals(Integer.toString(stats[2]), cvrgRes.getBranchExecutedText());
            assertEquals(Integer.toString(stats[3]), cvrgRes.getBranchTakenText());
            assertEquals(Integer.toString(stats[4]), cvrgRes.getBranchNotTakenText());
            assertEquals(Integer.toString(stats[5]), cvrgRes.getBranchBothText());
        }

        
        @Override
        void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_GRP_COVERAGE_RESULTS, command);
        }

        
        @Override
        Set<ESectionCTestGroup> getSectionIds() {
            TreeSet<ESectionCTestGroup> ids = new TreeSet<ESectionCTestGroup>();
            ids.add(ESectionCTestGroup.E_SECTION_COVERAGE_ALL_CODE_IN_GROUP);
            ids.add(ESectionCTestGroup.E_SECTION_COVERAGE_TEST_CASES_ONLY);
            return ids;
        }
    }
    
    
    class ScriptsSection extends GroupSectionTest {

        String m_initFunc = "initFunc";
        String m_initFuncParams = "a, b";
        String m_endFunc = "endFunc";
        String m_endFuncParams = "2, 3";
        
        @Override
        public void setSection() throws Exception {
            m_utils.setSectionGroupScripts(m_initFunc, m_initFuncParams, 
                                           m_endFunc, m_endFuncParams);
        }

        @Override
        public void testSection(CTestGroup testGroup) throws Exception {
            CTestFunction initF = testGroup.getScriptFunction(ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT, 
                                                               true);
            assertEquals(m_initFunc, initF.getName());
            StrVector initFParams = new StrVector();
            initF.getPositionParams(initFParams);
            assertEquals(m_initFuncParams, DataUtils.listToString(DataUtils.strVectorToList(initFParams)));
            
            CTestFunction endF = testGroup.getScriptFunction(ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT, 
                                                             true);
           assertEquals(m_endFunc, endF.getName());
           StrVector endFParams = new StrVector();
           endF.getPositionParams(endFParams);
           assertEquals(m_endFuncParams, DataUtils.listToString(DataUtils.strVectorToList(endFParams)));
        }

        
        @Override
        public Set<ESectionCTestGroup> getSectionIds() {
            TreeSet<ESectionCTestGroup> ids = new TreeSet<>();
            ids.add(ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT);
            ids.add(ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT);
            return ids;
        }
        
        
        @Override
        public void contextMenu(String command) throws Exception {
            m_utils.contextMenu(UITestUtils.SEC_GRP_SCRIPTS, command);
        }
    }
    
}


