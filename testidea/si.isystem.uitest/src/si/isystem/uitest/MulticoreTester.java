package si.isystem.uitest;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertContains;
import static org.junit.Assert.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import si.isystem.connect.CTestSpecification;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.swtbot.utils.KTableTestUtils;

/**
 * This class implements tests for multi-core configuration.
 * 
 * P R E C O N D I T I O N:
 * ======================== 
 * - open winIDEA with sdk/mpc56Sample-multicore/sample-mpc56.xjrf,
 * - start PDUControl and lock target 'Leopard SPC56EL70 + iC5000'. Check IP in winIDEA.
 * - build the project (binaries are not in svn), first for primary core, then
 *   manually start secondary core, build also this one. 
 * - start testIDEA from this winIDEA and run init sequence. 
 * - close testIDEA and start this test.
 *  
 *  
 * @author markok
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class MulticoreTester {

    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;
    private static KTableTestUtils m_ktableUtils;

    final static String TMP_SPEC_FILE_NAME = "D:/tmp/testIDEA-ui-multicore-test-temp-file.iyaml";
    
    @BeforeClass
    public static void setup() {
        
        SWTBotPreferences.TIMEOUT = 15000; // timeout for finding widgets in ms, default is 5 s
        // unfortunately this is also time of wait for widgets, for example for progress dialog to
        // close, which may take more than 5 seconds if there are many tests running.
        SWTBotPreferences.PLAYBACK_DELAY = 100; // playback timeout in ms - defines delay
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


    public void setProjectProperties() {
        
        m_bot.menu(UITestUtils.MENU_FILE).menu(UITestUtils.MENU_FILE__PROPERTIES).click();
        
        // Auto ID format
        m_bot.tree(0).select("General");
        m_bot.textWithLabel("Auto ID Format:").setText("${_coreId}_${_seq}");

        // Core IDs
        m_bot.tree(0).select("Multicore configuration");
        m_utils.waitForConnectionDialog(1);
         
        m_utils.activateShell("Project properties"); // required if connection dialog appears!
        m_utils.enterTextWLabel("Core IDs:", "core-0, e200z4");
     
        // Run configuration
        m_bot.tree(0).select("Initialization sequence");
        m_utils.selectCheckBoxWText("Always run init sequence before run", false);
        
        m_ktableUtils.selectKTable(SWTBotConstants.INIT_SEQUENCE_KTABLE);
        m_ktableUtils.setInitSeqAction(0, "core-0", "d");     // download
        m_ktableUtils.setInitSeqAction(1, "", "rr", "main");  // run
        m_ktableUtils.setInitSeqAction(2, "", "dd");          // del all BPs
        m_ktableUtils.setInitSeqAction(3, "e200z4", "c");     // connectToCore
        m_ktableUtils.setInitSeqAction(4, "e200z4", "d");     // download     
        m_ktableUtils.setInitSeqAction(5, "", "cc", "initSecondCore"); // call target f.
        m_ktableUtils.setInitSeqAction(6, "e200z4", "rr", "main");     // run

        // m_bot.tree(0).select("Run configuration");
        
        m_utils.selectCheckBoxWText("Check target state before run", true);
        m_utils.selectCheckBoxWText("Disable interrupts", true);

        // Stack usage
        m_bot.tree(0).select("Stack usage");
        m_ktableUtils.selectKTable(SWTBotConstants.STACK_USAGE_KTABLE);
        m_ktableUtils.setStackUsageParams(0, "", true, "0x4000c000", "0x4000dff0", "0x55");
        m_utils.pressKey(SWT.HOME);
        m_ktableUtils.setStackUsageParams(1, "e200z4", true, "0x50006000", "0x50007ff0", "0xaa");
      
        m_bot.sleep(200);
        m_bot.button("OK").click();
        
        // because core IDs have changed, testIDEA automatically disconnects
        m_utils.waitForShellAndClickOK("Disconnected!", 1000, false);
    }

    
    public void createTestCases() throws Exception {

        // c-0
        m_utils.createBaseTestWContextMenu("Func1", "10", "rv == 30", "rv", true);
        m_utils.setSectionAnalyzer("Start", "func1.trd", "Write", "No", "No", "Yes", "Yes", "", false);
        m_utils.setSectionCoverageTestCase("Yes", "XML", "textCvrg1.xml",  "", "No",
                                   "No", "No", "No", 
                                   "No", "No", "No", "No", "", "");
        m_utils.setSectionCoverageStatistics("Func1", 100, 100, 100, 0, 0, 100);

        // core-0
        m_utils.createBaseTestWContextMenu("Type_FunctionPointer", "", "rv == 29", 
                                           "rv", true, "core-0");
        m_utils.setSectionAnalyzer("Start", "funcPtr.trd", "Write", "No", "No", "Yes", "Yes", "", false);
        m_utils.setSectionCoverageTestCase("Yes", "XML", "textCvrg2.xml",  "", "No",
                                   "No", "No", "No", 
                                   "No", "No", "No", "No", "", "");
        m_utils.setSectionCoverageStatistics("Type_FunctionPointer", 100, 100, 0, 0, 0, 0);

        // e200z4, stubbed
        m_utils.createBaseTestWContextMenu("Type_FunctionPointer", "", "rv == 333", 
                                           "rv", true, "e200z4");
        m_utils.setSectionAnalyzer("Start", "funcPtr.trd", "Write", "No", "No", "Yes", "Yes", 
                                   "", false);
        m_utils.setSectionCoverageTestCase("Yes", "XML", "textCvrg3.xml",  "", "No",
                                   "No", "No", "No", 
                                   "No", "No", "No", "No", "", "");
        m_utils.setSectionCoverageStatistics("Type_FunctionPointer", 100, 100, 0, 0, 0, 0);

        m_utils.setSectionStub("Mult", "Yes", null, "sRetVal", null, "", "", false);
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
        m_ktableUtils.setSectionStubAssignments(0, "sRetVal", "333", new String[0]);

        // core-0, stubbed
        m_utils.createBaseTestWContextMenu("Type_FunctionPointer", "", "rv == 338", 
                                           "rv", true, "core-0");
        m_utils.setSectionAnalyzer("Start", "funcPtr.trd", "Write", "No", "No", "Yes", "Yes", 
                                   "", false);
        m_utils.setSectionCoverageTestCase("Yes", "XML", "textCvrg.xml",  "", "No",
                                   "No", "No", "No", 
                                   "No", "No", "No", "No", "", "");
        m_utils.setSectionCoverageStatistics("Type_FunctionPointer", 100, 100, 0, 0, 0, 0);

        m_utils.setSectionStub("Mult", "Yes", null, "sRetVal", null, "", "", false);
        m_ktableUtils.selectKTable(SWTBotConstants.STUBS_STEPS_KTABLE);
        m_ktableUtils.setSectionStubAssignments(0, "sRetVal", "333", new String[0]);

        // e200z4
        m_utils.createBaseTestWContextMenu("Mult", "11, 8", "rv == -88", "rv", true); // core ID is specified later
        m_utils.selectTestSection(UITestUtils.SEC_FUNCTION);
        m_utils.enterTextWLabel("Core ID:", "e200z4");
        m_utils.setSectionExpected("40", null, new String[0]);

        // core-0
        m_utils.createBaseTestWContextMenu("Mult", "11, 11", "rv == 121", "rv", true, "core-0");
        m_utils.setSectionExpected("50", null, new String[0]);
    }

    
    public void verifyResults() throws Exception {

        // test for core ID setting
        m_utils.selectTestCase("core-0_0", "Func1");
        m_utils.copyToClipboard();
        CTestSpecification testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("", testSpec.getCoreId());
        assertEquals("Func1", testSpec.getFunctionUnderTest(true).getName());

        m_utils.selectTestCase("core-0_1", "Type_FunctionPointer");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("core-0", testSpec.getCoreId());
        assertEquals("Type_FunctionPointer", testSpec.getFunctionUnderTest(true).getName());

        m_utils.selectTestCase("e200z4_2", "Type_FunctionPointer");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("e200z4", testSpec.getCoreId());
        assertEquals("Type_FunctionPointer", testSpec.getFunctionUnderTest(true).getName());

        m_utils.selectTestCase("core-0_3", "Type_FunctionPointer");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("core-0", testSpec.getCoreId());
        assertEquals("Type_FunctionPointer", testSpec.getFunctionUnderTest(true).getName());

        // Because core ID was set later than in new test dialog, core ID in 
        // test ID does not match the real coreID. See FuncSpecEditor, after 
        // creation of 'm_coreIdTBCtrl' why core ID is not updated on the fly. 
        m_utils.selectTestCase("core-0_4", "Mult");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("e200z4", testSpec.getCoreId());
        assertEquals("Mult", testSpec.getFunctionUnderTest(true).getName());

        m_utils.selectTestCase("core-0_5", "Mult");
        m_utils.copyToClipboard();
        testSpec = m_utils.getTestSpecFromClipboard();
        assertEquals("core-0", testSpec.getCoreId());
        assertEquals("Mult", testSpec.getFunctionUnderTest(true).getName());

        // test that all tests succeeded 
        m_utils.toolbarRunAllTests();
        m_utils.waitForConnectionDialog(1);
        
        m_utils.waitForProgressDialog();
        m_bot.sleep(500);  // safety delay
        
        assertContains(UITestUtils.ALL_TESTS_OK_PREFIX, m_utils.getStatusViewText());
    }
    
    
    @Test
    public void runTests() throws Exception {
        m_utils.discardExistingAndCreateNewProject();

        String fileName = CreateAndRunTest.TEST_OUT_DIR + "/multicoreTest2.iyaml";

        if (m_utils.isFileCreatedToday(fileName)) {
            m_utils.loadTestSpecFromFile(fileName);
            
        } else {
            setProjectProperties();
            createTestCases();
            TestSpecificationModel model = UITestUtils.getActiveModel();
            model.saveModelAs(fileName);
        }
        
       verifyResults();
    }
}