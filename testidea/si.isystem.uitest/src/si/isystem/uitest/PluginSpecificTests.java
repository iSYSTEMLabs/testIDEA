package si.isystem.uitest;

import java.util.List;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For this test to work, you have to:
 * 1. Close rcp plugin and feature and open eclipse plug-in and feature in Eclipse workspace
 * 2. Enter correct plug-in in uitest's MANIFEST.MF:
 *      si.isystem.itest.plugin.eclipse
 *    and close (context menu): 
 *      si.isystem.itest.plugin.rcp.
 * 3. In project uitest | Properties check Java Build Path, tab projects.
 *   
 * Restore these settings after test.
 * 
 * @author markok
 *
 */

@RunWith(SWTBotJunit4ClassRunner.class)
public class PluginSpecificTests {

    private static final String PERSPECTIVE_RESOURCE = "Resource (default)";

    final static String BOT_PROJ_NAME = "BotProject";

    private static SWTWorkbenchBot m_bot;
    private static UITestUtils m_utils;

    @BeforeClass
    public static void setup() {
        
        SWTBotPreferences.TIMEOUT = 15000; // timeout for finding widgets in ms, default is 5 s
        // unfortunately this is also time of wait for widgets, for example for progress dialog to
        // close, which may take more than 5 seconds if there are many tests running.
        SWTBotPreferences.PLAYBACK_DELAY = 0; // playback timeout in ms - defines delay
                                              // between test calls
        m_bot = new SWTWorkbenchBot();
        m_utils = new UITestUtils(m_bot);

        m_utils.openTestIDEAPerspective();
        m_utils.deselectToolsPrefsAlwaysRunInitSeq();
    }    

    
    @AfterClass
    public static void shutdown() {
        // m_utils.exitApplication(true); // Don't know why, but the method 
        // ApplicationWorkbenchWindowAdvisor::preWindowShellClose() is not called,
        // so the si.isystem.itest.ipc.ReceiverJob is still running and hence the message
    }

    
    /**
     * This test opens default XML browser (for example Internet Explorer on the top of
     * Eclipse, so there is code snippet, which brings Eclipse back to top. For this
     * to work, you have to enable set foreground in Windows, by setting
     * HKEY_CURRENT_USER\Control Panel\Desktop\ForegroundLockTimeout to 0 and restarting
     * machine for this setting to take effect. 
     *  
     * @throws Exception
     */
    
    @Test
    public void testCreateNewFileAndRun() throws Exception {
        
        boolean isCreateNewFiles = true; // set to true when debugging tests, 
                                          // should remain false for testing 
        
        createProjectIfItDoesNotExist();
        m_bot.sleep(2000);
        SWTBotShell myShell = m_bot.activeShell();
        
        System.out.println("shel = " + myShell);
        
        // create two new files
        if (createNewFile("testPlugin1.iyaml", isCreateNewFiles)) {
            /* m_bot.viewByTitle("Outline").setFocus();
            m_utils.selectAll();
            m_utils.pressKey(SWT.DEL); */
            m_utils.createTestsVerifySymbolsAndRename(); 
        }
        
        if (createNewFile("testPlugin2.iyaml", isCreateNewFiles)) {
            /* m_bot.viewByTitle("Outline").setFocus();
               m_utils.selectAll();
               m_utils.pressKey(SWT.DEL); */
            m_utils.createTestsVerifySymbolsAndRename();
        }
        
        m_utils.saveAll();
        
        // run the first file with context command
        m_utils.openPerspective(PERSPECTIVE_RESOURCE);
        
        SWTBotView view = m_bot.viewByTitle("Project Explorer");
        SWTBotTreeItem projNode = view.bot().tree().select(BOT_PROJ_NAME).expandNode(BOT_PROJ_NAME);
        projNode.select("testPlugin1.iyaml");
        projNode.getNode("testPlugin1.iyaml").contextMenu("Run As").menu("1 testIDEA launch").click();
        // SWTBotShell[] shells = m_bot.shells();
        m_utils.waitForProgressDialog();
        // shells[0].setFocus();
        // shells[0].activate();
        
        // bring Eclipse to top after report has been opened in Internet Explorer.
        // See comment of this test method for how to enable this in Windows.
        SWTBotShell shell = m_bot.shell("junit-workspace - Resource - BotProject/testPlugin1.iyaml - ");
        shell.setFocus();
        shell.activate();
        
        m_utils.openTestIDEAPerspective();
        
        m_utils.checkStatusView(0, "All tests for 'testPlugin1.iyaml' completed successfully!\n"
                + "Number of tests: 5");
        /*
        SWTBotView statusView = m_bot.viewByTitle(TEST_STATUS_VIEW_TITLE);
        statusView.bot().table().click(0, 1);
        
        assertEquals("All tests for 'testPlugin1.iyaml' completed successfully!\n"
                   + "Number of tests: 5", m_utils.getStatusViewText());
        */
        createLauncConfigurationAndRun();
    }


    private void createProjectIfItDoesNotExist() {
        m_utils.openPerspective(PERSPECTIVE_RESOURCE);
        
        // check if the project already exists
        SWTBotView projExplorer = m_bot.viewByTitle("Project Explorer");
        SWTBotTree projTree = projExplorer.bot().tree();
        SWTBotTreeItem[] items = projTree.getAllItems();
        boolean projExists = false;
        if (items.length > 0) {
            SWTBotTreeItem item0 = items[0];
            String projName = item0.getText();
            if (projName.equals(BOT_PROJ_NAME)) {
                projExists = true;
            }
        }
        
        // create new project
        if (!projExists) {
            m_bot.menu("File").menu("New").menu("Project...").click();
            m_utils.waitForShell("New Project", 2000, true);
            m_bot.activeShell();
            m_bot.tree().select("General").expandNode("General").select("Project");
            m_bot.button("Next >").click();
            m_utils.enterTextWLabel("Project name:", BOT_PROJ_NAME);
            m_bot.button("Finish").click();
        }
    }

    
    private boolean createNewFile(String fileName, boolean isCreateNewFiles) {

        m_utils.openPerspective(PERSPECTIVE_RESOURCE);
        // first delete file if it already exists
        SWTBotView view = m_bot.viewByTitle("Project Explorer");
        SWTBotTree projTree = view.bot().tree();
        SWTBotTreeItem projNode = projTree.select(BOT_PROJ_NAME).expandNode(BOT_PROJ_NAME);
        List<String> fileNodes = projNode.getNodes();

        if (fileNodes.contains(fileName)  &&  !isCreateNewFiles) {
            return false;
        }
        
        if (fileNodes.contains(fileName)) {
            projNode.select(fileName); // select returns this (projNode), not selected node!!!
            SWTBotTreeItem fileNode = projNode.getNode(fileName);
            fileNode.contextMenu("Delete").click();
            // m_bot.checkBox().click();
            m_utils.waitForShellAndClickOK("Delete Resources", 2000, true);
            m_bot.sleep(1000); // wait until the dialog disappears
        }
        
        m_utils.openTestIDEAPerspective();
        m_bot.menu("File").menu("New").menu("Test Specification file").click();
        m_utils.waitForShell("", 2000, true);
        m_utils.enterTextWLabel("Folder or project:", "/" + BOT_PROJ_NAME);
        m_utils.enterTextWLabel("File name:", fileName);
        m_bot.button("Finish").click();
        
        return true;
    }

    
    private void createLauncConfigurationAndRun() {
        m_utils.openPerspective("Java");
        m_bot.viewByTitle("Package Explorer").bot().tree().select(BOT_PROJ_NAME);
        m_bot.menu("Run").menu("Run Configurations...").click();
        m_bot.tree().select("iSYSTEM Test").expandNode("iSYSTEM Test");
        m_bot.toolbarButtonWithTooltip("New launch configuration").click();
        m_bot.button("  Add all (project)   ").click();
        m_bot.sleep(1000); // make the state observable for humans
        m_bot.button("Run").click();
        m_bot.sleep(1000); // make the state observable for humans
        
        // no progress dialog is shown now - user can see Progress view, but we'll
        // wait for the right message in Test Status view
        m_utils.openTestIDEAPerspective();

        m_bot.sleep(30000); // measured value
        
        m_utils.checkStatusView(1, "All tests for 'testPlugin1.iyaml' completed successfully!\n"
                + "Number of tests: 5");
        m_utils.checkStatusView(0, "All tests for 'testPlugin2.iyaml' completed successfully!\n"
                + "Number of tests: 5");
        
        /*
        m_bot.viewByTitle(TEST_STATUS_VIEW_TITLE).bot().table().click(0, 1);
        assertEquals("All tests for 'testPlugin2.iyaml' completed successfully!\n"
                + "Number of tests: 5", m_utils.getStatusViewText());

        m_bot.viewByTitle(TEST_STATUS_VIEW_TITLE).bot().table().click(2, 1);
        assertEquals("All tests for 'testPlugin1.iyaml' completed successfully!\n"
                + "Number of tests: 5", m_utils.getStatusViewText());
         */
    }
}
