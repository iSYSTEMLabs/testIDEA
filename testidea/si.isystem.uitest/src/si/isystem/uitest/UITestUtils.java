package si.isystem.uitest;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertContains;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TouchEvent;
import org.eclipse.swt.events.TouchListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.keyboard.Keyboard;
import org.eclipse.swtbot.swt.finder.keyboard.KeyboardFactory;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.waits.WaitForObjectCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarPushButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestDiagramConfig.EDiagType;
import si.isystem.connect.CTestDiagramConfig.EViewFormat;
import si.isystem.connect.CTestDiagramConfig.EViewerType;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.connect.connect;
import si.isystem.itest.common.Messages;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.handlers.FileOpenCmdHandler;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.swtbot.utils.KTableTestUtils;
import si.isystem.swtbot.utils.RegExWidgetMatcher;
import si.isystem.swtbot.utils.RegExWidgetMatcher.ETextType;
import si.isystem.swtbot.utils.SWTBotTristateCheckBox;
import si.isystem.swtbot.utils.SimpleBotButton;
import swtbot.addition.condition.ShellCondition;
import swtbot.addition.dnd.DndUtil;

/**
 * This class contains methods, which group several UI actions to perform
 * often executed actions in testIDEA UI. Examples of such actions are 
 * selection of derived test case, setting of test ID, ... 
 * 
 * @author markok
 *
 */
public class UITestUtils {

    private static final String NEW_DERIVED_TEST_CASE_WIZARD = "New derived test case wizard";
    private static final String NEW_TEST_CASE_WIZARD = "New test case wizard";
    final String RB_YES = "Yes";
    protected static final String RB_NO = "No";

    private static final String EXPORT_DLG_TITLE = "Export Test Cases";
    private static final String ECLIPSE_EXPORT_DLG_TITLE = "Export";

    private static final String IMPORT_DLG_TITLE = "Import Test Cases";
    private static final String ECLIPSE_IMPORT_DLG_TITLE = "Import";

    
    protected static final String TOOLTIP_CONNECT_TO_WIN_IDEA = "Connect to winIDEA.*";
    protected static final String TOOLTIP_REFRESH_SYMBOLS = "Refresh global .*";
    
    private static final String RET_VAL_NAME = "Ret. val. name:";

    private static final String DIALOG_TITLE_SAVE_RESOURCE = "Save Resource";

    public static final String ALL_TESTS_OK_PREFIX = 
            "All tests for selected editor completed successfully!\n" +
            "Number of tests: ";
    
    private static final String TEST_STATUS_VIEW_TITLE = "Test Status";
    
    // commands in editor tree context menu
    public static final String CMD_CUT_SECTION = "Cut";
    public static final String CMD_COPY_SECTION = "Copy";
    public static final String CMD_PASTE_SECTION = "Paste";
    public static final String CMD_CLEAR_SECTION = "Clear Section";
        
    public static final String TEST_TREE_CAPTION = "Outline";
    public static final String SEC_META = "Meta";
    public static final String SEC_FUNCTION = "Function";
    public static final String SEC_SYS_BEGIN_SCOND = "System init";
    public static final String SEC_SYS_END_SCOND = "Execute test";
    public static final String SEC_PRECONDITION = "Pre-conditions";
    public static final String SEC_EXPECTED = "Expected";
    public static final String SEC_VARIABLES= "Variables";
    public static final String SEC_PERSIST_VARIABLES= "Persistent variables";
    public static final String SEC_STUBS = "Stubs";
    public static final String SEC_USER_STUBS = "User Stubs";
    public static final String SEC_TEST_POINTS = "Test Points";
    public static final String SEC_ANALYZER = "Analyzer";
    public static final String SEC_COVERAGE = "Coverage";
    public static final String SEC_COVERAGE_STATS= "Statistics";
    public static final String SEC_PROFILER = "Profiler";
    public static final String SEC_PROFILER_CODE = "Code areas";
    public static final String SEC_PROFILER_DATA = "Data areas";
    public static final String SEC_TRACE = "Trace";
    public static final String SEC_HIL = "HIL";
    public static final String SEC_SCRIPTS = "Scripts";
    public static final String SEC_OPTIONS = "Options";
    public static final String SEC_DRY_RUN = "Dry run";
    public static final String SEC_DIAGRAMS = "Diagrams";

    public static final String SEC_GRP_FILTER = "Filter";
    public static final String SEC_GRP_COVERAGE_CONFIG = "Coverage config.";
    public static final String SEC_GRP_COVERAGE_RESULTS = "Coverage results";
    public static final String SEC_GRP_SCRIPTS = "Scripts";

    public static final String CTX_TREE_MENU_NEW_GROUP = "New &Group ...";
    public static final String CTX_TREE_MENU_NEW_SUB_GROUP = "New S&ub-Group ..."; // this is NOT regex
    public static final String CTX_TREE_MENU_SET_TEST_ID = "Set Test IDs";

    public static final String MENU_FILE = "File";
    private static final String MENU_FILE__NEW_TEST = "New &Test ...";  // NOT regex
    public static final String MENU_FILE__NEW_DERIVED_TEST = "New &Derived Test ...";
    public static final String MENU_FILE_SAVE_ALL = "Save All";
    public static final String MENU_FILE_CLOSE = "Close";
    public static final String MENU_FILE_CLOSE_ALL = "Close All";
    public static final String SAVE_ON_EXIT_DLG_TITLE = "Save resources";
    public static final String MENU_FILE__PROPERTIES = "Properties";


    public static final String MENU_TEST = "Test";
    private static final String MENU_TEST_CONFIGURATION = "Configuration...";
    
    public static final String MENU_TOOLS = "iTools";
    public static final String MENU_TOOLS__SET_ID_FORMAT = CTX_TREE_MENU_SET_TEST_ID;
    public static final String MENU_TOOLS__VERIFY_SYMBOLS = "Verify Symbols";
    public static final String MENU_TOOLS__RENAME = "Rename ...";
    public static final String MENU_TOOLS__SET_ANALYZER_FILE_NAMES = "Set Analyzer File Names";
    public static final String MENU_TOOLS__CREATE_GROUPS = "Create Groups ...";
    public static final String MENU_TOOLS__GEN_TEST_CASES = "Generate Test Cases ...";
    public static final String MENU_TOOLS__OPTIMIZE_TEST_VECTORS = "Optimize Test Vectors";
    public static final String MENU_GENERATE_SCRIPT = "Generate Test Script ...";
    public static final String MENU_SCRIPT_EXTENSIONS_WIZARD = "Script Extensions Wizard ...";

    private static final long DEFAULT_POLL_TIMEOUT = 100;

    public static final String DIALOG_TITLE_GENERATE_TEST_SCRIPT = "Generate Test Script";
    public static final String DIALOG_SCRIPT_EXTENSIONS_WIZARD = "Script Extensions Wizard";
    public static final String DIALOG_TC_GENERATOR_WIZARD = "Test Case Generator Wizard";
    public static final String DIALOG_TITLE_PROJECT_PROPERTIES = "Project properties";
    public static final String DIALOG_TITLE_TEST_CASE_FILTER = "Test case filter";
    
    protected static final String DEFAULT_TEST_IYAML_FILE_NAME = "swtbotTest.iyaml";
    protected static final String DEFAULT_TEST_IYAML_FILE_PATH = "d:/tmp/" + DEFAULT_TEST_IYAML_FILE_NAME;
    protected static final String SDK_TEST_PROJ_PATH = "d:/bb/trunk/sdk/targetProjects";
    private static final int SECTION_TREE_IDX = 1;

    static final String EMPTY_GROUP_POSTFIX = "(0) ///";
    static final String REPORT_SCHEMA_LOCATION = "../si.isystem.itest.plugin.core/templates/reports/itest_report_0.xsd";

    private SWTWorkbenchBot m_bot;
    private static DndUtil m_dnd;
    private static Keyboard m_keyboard;
    volatile private String m_yamlSpec;
    private KTableTestUtils m_ktableUtils;
    private boolean m_isRCP = false;

    public UITestUtils(SWTWorkbenchBot bot) {
        m_bot = bot;
        m_keyboard = KeyboardFactory.getSWTKeyboard();
        m_dnd = new DndUtil(SWTUtils.display());
        initKTableUtils();
        
        Bundle plugin = Platform.getBundle("si.isystem.itest.plugin.rcp");
        if (plugin != null) {
            //System.out.println("- bundle: " + plugin.getSymbolicName());
            m_isRCP = true;
        }
    }


    boolean isRCP() {
        return m_isRCP;
    }
    
    
    void openTestIDEAPerspective() {
        openPerspective("testIDEA");
    }

    
    void openPerspective(String perspectiveName) {
        if (!m_isRCP) {
            m_bot.menu("Window").menu("Perspective").menu("Open Perspective").menu("Other...").click();
            m_bot.table().select(perspectiveName);
            m_bot.button("OK").click();
        }
    }

    
    void deselectToolsPrefsAlwaysRunInitSeq() {
        m_bot.menu(MENU_TOOLS).menu("Preferences").click();
        m_bot.tree(0).select("testIDEA").expandNode("testIDEA", true).select("Initialization sequence");
        
        selectCheckBoxWText("Always run init sequence before run", false);
        
        m_bot.sleep(500);
        m_bot.button("OK").click();
    }


    void setAnalyzerDefaultName(String defaultFName) {
        m_bot.menu(MENU_FILE).menu("Properties").click();
        m_bot.tree(0).select("Tools configuration");
        
        enterTextWLabel("Analyzer doc. file name:", defaultFName);
        
        m_bot.sleep(500);
        m_bot.button("OK").click();
    }


    void closeActiveEditor() {
        m_bot.menu(MENU_FILE).menu(MENU_FILE_CLOSE).click();
        waitForShell(DIALOG_TITLE_SAVE_RESOURCE, 3000, true); // model will be modified by rename
        SWTBotShell shellbot = m_bot.shell(DIALOG_TITLE_SAVE_RESOURCE);
        shellbot.activate();
        m_bot.button(RB_NO).click();
    }
    
    
    void discardExistingAndCreateNewProject() throws Exception {
        discardExistingAndCreateNewProject(DEFAULT_TEST_IYAML_FILE_PATH);
    }
    
    
    void discardExistingAndCreateNewProject(final String iyamlFileName) throws Exception {

        saveAll();
        
        SWTBotMenu menu = m_bot.menu(MENU_FILE).menu(MENU_FILE_CLOSE_ALL);
        if (menu.isEnabled()) {
            menu.click();
        }
        
        Path path = Paths.get(iyamlFileName);
        if (Files.exists(path)) {
            Files.deleteIfExists(path); // clear previous tests
        }
        
        Files.createFile(path);
        
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    // file must be opened from UI thread
                    FileOpenCmdHandler.openEditor(iyamlFileName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        /* SWTBotToolbarPushButton saveBtn = getToolbarButton("Save.*");
        
        clickToolbarButton("New Project");

        if (saveBtn.isEnabled()) {
            waitForShell(UITestUtils.SAVE_ON_EXIT_DLG_TITLE, 3000, true);
            SWTBotShell shell = m_bot.shell(UITestUtils.SAVE_ON_EXIT_DLG_TITLE);
            shell.activate();
            m_bot.button("Discard").click();
        } */
    }


    public void saveAll() {

        // m_bot.saveAllEditors(); // throws exception, don't know why

        SWTBotMenu menu = m_bot.menu(MENU_FILE).menu(MENU_FILE_SAVE_ALL);
        if (menu.isEnabled()) {
            menu.click();
        }
    }


    private static TestSpecificationModel m_activeModel = null;
    
    public static TestSpecificationModel getActiveModel() {

        
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    // file must be opened from UI thread
                    m_activeModel = TestSpecificationModel.getActiveModel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        Display.getDefault().syncExec(runnable);
        
        return m_activeModel;
    }

    
    void connect() {
        clickToolbarButton(UITestUtils.TOOLTIP_CONNECT_TO_WIN_IDEA);
        waitForConnectionDialog();
        m_bot.sleep(500); // wait until connects and m_bot gets main window
    }

    void refreshSymbols() {
        clickToolbarButton(UITestUtils.TOOLTIP_REFRESH_SYMBOLS);
        m_bot.sleep(1500); // wait until connects and m_bot gets main window
    }

    private void initKTableUtils() {
        
        if (m_ktableUtils == null) {
            m_ktableUtils = new KTableTestUtils(m_bot, this);
        }
    }
    
    
    void selectKTableWithContent(int col, int row, String regEx) {
        m_ktableUtils.selectKTableWithContent(col, row, regEx);
    }
    
    
    void setKTableCellContent(int col, int row, String content) {
        m_ktableUtils.setCellContent(col, row, content);
    }
    
    
    /**
     * Sets init sequence for single core.
     */
    void setInitSequence() {
        
        // see also class MulticoreTester for init seq. example 
        openDialogWithMainMenu(MENU_TEST, MENU_TEST_CONFIGURATION, "Preferences");
        
        /* m_bot.menu(UITestUtils.MENU_TEST).menu(UITestUtils.MENU_TEST_CONFIGURATION).click();
        
        String shellTitle = "Preferences";
        waitForShell(shellTitle, 2000, true);
        SWTBotShell shellbot = m_bot.shell(shellTitle);
        shellbot.activate(); */

        m_ktableUtils.selectKTable(SWTBotConstants.INIT_SEQUENCE_KTABLE);

        m_ktableUtils.addRows(2);  
        
        m_ktableUtils.setInitSeqAction(0, "", "d");
        m_ktableUtils.setInitSeqAction(1, "", "rr", "main");
        
        m_bot.button("OK").click();

    }

    void waitForConnectionDialog() {
        waitForConnectionDialog(0);
    }
    
    void waitForConnectionDialog(int row) {
    
        /* if (waitForShell("Not connected", 2000, false)) { // may appear or not - continue anyway in 2 seconds
            m_bot.button("Connect Now").click();
        } */

        if (waitForShell("Connect to winIDEA?", 2000, false)) { // may appear or not - continue anyway in 2 seconds

            // The first dialog does not require activation!
            // SWTBotShell shellbot = m_bot.shell(title);
            // shellbot.activate();

            m_ktableUtils.selectKTable(0);
            m_ktableUtils.clickDataCell(1, row);
            //SWTBotTable table = m_bot.tableWithLabel("Running winIDEA instances:");
            //table.doubleClick(row, 0);

            m_bot.button("OK").click();
        }

    }
    
    
    /**
     * 
     * @param expectedStatusText for example: UITEstUtils.ALL_TESTS_OK_PREFIX + "2"
     *                           where "2" is number of tests.
     */
    void runAllTests(String expectedStatusText) {
        toolbarRunAllTests();

        waitForConnectionDialog();
        
        if (waitForShell("Init target state before test run", 2000, false)) { // may appear or not - continue anyway in 2 seconds
            m_bot.button("OK").click();
        }

        waitForProgressDialog();
        System.out.println("--- Progress dialog ended!");

        assertContains(expectedStatusText, getStatusViewText());
    }
    
    
    void runSelectedTest(String expectedStatusText) {
        toolbarRunSelectedTest();

        waitForConnectionDialog();
        
        if (waitForShell("Init target state before test run", 2000, false)) { // may appear or not - continue anyway in 2 seconds
            m_bot.button("OK").click();
        }

        waitForProgressDialog();
        System.out.println("--- Progress dialog ended!");

        assertContains(expectedStatusText, getStatusViewText());
    }
    
    
    void setPropertiesAutoIdFormat(String format) {

        openDialogWithMainMenu(MENU_FILE, MENU_FILE__PROPERTIES, DIALOG_TITLE_PROJECT_PROPERTIES);
        
        /*m_bot.menu(MENU_FILE).menu(MENU_FILE__PROPERTIES).click();

        waitForConnectionDialog();

        final String propsShellTitle = DIALOG_TITLE_PROJECT_PROPERTIES;
        waitForShell(propsShellTitle, 3000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(propsShellTitle);
        shellbot.activate(); */

        m_bot.tree(0).select("General");
        m_bot.textWithLabel("Auto ID Format:").setText(format);
        m_bot.sleep(500);
        m_bot.button("OK").click();
    }

    
    void setPropertiesEvaluator(boolean isOverride,
                                boolean isIntInHex,
                                boolean isMemAreaInPtrProto,
                                boolean isCharArrayAsStr,
                                boolean isDerefCharArray,
                                boolean isArrayAndStructValues,
                                String fpPrecision,
                                boolean isCharASCIIAndInt,  // can not select Integer
                                boolean isCharANSI,
                                boolean isHexFmtWPrefix,
                                boolean isBinFmtWB,
                                boolean isEnumAndInt // can not select Integer
                                ) {

        // make sure connection is established, otherwise progress dialog may blip
        // when license is checked and steal focus from this dialog.
        clickToolbarButton("Connect.*");
        waitForProgressDialog();

        openDialogWithMainMenu(MENU_FILE, MENU_FILE__PROPERTIES, DIALOG_TITLE_PROJECT_PROPERTIES);
        
        m_bot.tree(0).select("winIDEA evaluator");
        
        selectCheckBoxWText(Messages.PropEvaluator_isOverride, isOverride);
        
        if (!isOverride) {
            return;  // return, since all other controls are disabled
        }
        
        selectCheckBoxWText(Messages.PropEvaluator_isIntInHex, isIntInHex);
        selectCheckBoxWText(Messages.PropEvaluator_isMemAreaInPtrProto, isMemAreaInPtrProto);
        selectCheckBoxWText(Messages.PropEvaluator_isCharArrayAsStr, isCharArrayAsStr);
        selectCheckBoxWText(Messages.PropEvaluator_isDerefCharArray, isDerefCharArray);
        selectCheckBoxWText(Messages.PropEvaluator_isArrayAndStructValues, isArrayAndStructValues);
        enterTextWLabel(Messages.PropEvaluator_fpPrecision, fpPrecision);
        
        if (isCharASCIIAndInt) {
            clickRadioWLabel(Messages.PropEvaluator_ChrFmtASCIIAndInt);
        } else {
            clickRadioWLabel(Messages.PropEvaluator_ChrFmtASCII);
        }
        
        selectCheckBoxWText(Messages.PropEvaluator_isCharANSI, isCharANSI);
        
        if (isHexFmtWPrefix) {
            clickRadioWLabel(Messages.PropEvaluator_isHexFmtWPrefix);
        } else {
            clickRadioWLabel(Messages.PropEvaluator_isHexFmtWOPrefix);
        }
        
        if (isBinFmtWB) {
            clickRadioWLabel(Messages.PropEvaluator_isBinFmtWB);
        } else {
            clickRadioWLabel(Messages.PropEvaluator_isBinFmtWOB);
        }
        
        if (isEnumAndInt) {
            clickRadioWLabel(Messages.PropEvaluator_EnumEnumAndInt);
        } else {
            clickRadioWLabel(Messages.PropEvaluator_EnumEnumOnly);
        }
        
        
        
        m_bot.sleep(500);
        m_bot.button("OK").click();
    }

    
    void setPropertiesAnalDocFileName(String format) {
        
        openDialogWithMainMenu(MENU_FILE, MENU_FILE__PROPERTIES, 
                               DIALOG_TITLE_PROJECT_PROPERTIES);
        
        /* m_bot.menu(MENU_FILE).menu(MENU_FILE__PROPERTIES).click();

        waitForConnectionDialog();

        final String propsShellTitle = DIALOG_TITLE_PROJECT_PROPERTIES;
        waitForShell(propsShellTitle, 3000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(propsShellTitle);
        shellbot.activate(); */

        m_bot.tree(0).select("Tools configuration");
        m_bot.textWithLabel("Analyzer doc. file name:").setText(format);
        m_bot.sleep(500);
        m_bot.button("OK").click();
    }


    void wizardAnalyzerFileName(String newPattern, String scope, String oldValue, 
                                boolean isTrace, boolean isCoverage, boolean isProfiler) {
    
        String dialogTitle = "Set analyzer file names";
        openDialogWithMainMenu(MENU_TOOLS, MENU_TOOLS__SET_ANALYZER_FILE_NAMES, 
                               dialogTitle);
        
        if (newPattern != null) {
            clickButton("Modify");
            waitForShell(DIALOG_TITLE_PROJECT_PROPERTIES, 2000, true);
            SWTBotShell shellbot = m_bot.shell(DIALOG_TITLE_PROJECT_PROPERTIES);
            shellbot.activate();
            m_bot.textWithLabel("Analyzer doc. file name:").setText(newPattern);
            m_bot.sleep(500);
            m_bot.button("OK").click();
            
            // Reactivation REQUIRED, otherwise components are not found!
            shellbot = m_bot.shell(dialogTitle);
            shellbot.activate();
        }
        
        clickRadioWLabel(scope);
        clickRadioWLabel(oldValue);
        selectCheckBoxWText("Trace", isTrace);
        selectCheckBoxWText("Coverage", isCoverage);
        selectCheckBoxWText("Profiler", isProfiler);

        clickButton("OK");
    }
    
    
    public void openDialogWithMainMenu(String mainMenuCmd, String cmd, String dialogTitle) {
        
        m_bot.menu(mainMenuCmd).menu(cmd).click();

        waitForConnectionDialog();

        waitForShell(dialogTitle, 2000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(dialogTitle);
        shellbot.activate();
    }

    
    void createNewBaseUnitTest(String funcName, 
                               String params, 
                               String expr, 
                               String retValName,
                               boolean isCreateId) throws Exception {

        createNewBaseTest(false, "", funcName, params, expr, retValName, isCreateId);
    }


    void createNewBaseSystemTest(String coreId, 
                                 String expr, 
                                 boolean isCreateTestId) throws Exception {

        createNewBaseTest(true, coreId, null, null, expr, null, isCreateTestId);
    }


    private void createNewBaseTest(boolean isSystemTest,
                                   String coreId,
                                   String funcName, 
                                   String params, 
                                   String expr, 
                                   String retValName,
                                   boolean isCreateId) throws Exception {

        // m_bot.menu(MENU_FILE).menu(MEN_FILE__NEW_TEST).click();
        
        openNewTestWizard(false);
        fillDialogNewTestPage1(isSystemTest, coreId, funcName, params, expr, 
                               retValName, isCreateId, false, true);
    }


    void openNewTestWizard(boolean isDrivedTest) {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        
        String menuOption = MENU_FILE__NEW_TEST;
        if (isDrivedTest) {
            menuOption = MENU_FILE__NEW_DERIVED_TEST;
        }
        
        SWTBotMenu ctxMenu = new SWTBotMenu(ContextMenuHelper.contextMenu(tree, menuOption));
        ctxMenu.click();
    }


    void createNewDerivedTest(String parentIdFuncText,
                              String coreId,
                              String funcName, 
                              String params, 
                              String expr, 
                              String retValName,
                              boolean isCreateId) throws Exception {

        clickTestTreeContextMenu(SWTBotConstants.BOT_TEST_TREE_ID, 
                                 parentIdFuncText, 
                                 MENU_FILE__NEW_DERIVED_TEST);

        fillDialogNewTestPage1(false, coreId, funcName, params, expr, retValName, 
                               isCreateId, true, true);
    }


    void createNewDerivedSystemTest(String parentIdFuncText,
                                    String coreId,
                                    String expr, 
                                    boolean isCreateId) throws Exception {

        clickTestTreeContextMenu(SWTBotConstants.BOT_TEST_TREE_ID, 
                                 parentIdFuncText, 
                                 MENU_FILE__NEW_DERIVED_TEST);

        fillDialogNewTestPage1(true, coreId, null, null, expr, null, isCreateId, 
                               true, true);
    }


    void createNewDerivedDerivedTest(String parentOfParentIdFuncText,
                                     String parentIdFuncText,
                                     String coreId,
                                     String funcName, 
                                     String params, 
                                     String expr, 
                                     String retValName,
                                     boolean isCreateId) throws Exception {

        clickTestTreeContextMenu(SWTBotConstants.BOT_TEST_TREE_ID, 
                                 parentOfParentIdFuncText, 
                                 parentIdFuncText,
                                 MENU_FILE__NEW_DERIVED_TEST);

        fillDialogNewTestPage1(false, coreId, funcName, params, expr, retValName, 
                               isCreateId, true, true);
    }


    void createNewGroupWithContextCmd(String groupId, 
                                      String includedTestIds) {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        // SWTBotContextMenu ctxMenu = new SWTBotContextMenu(tree);
        SWTBotMenu ctxMenu = new SWTBotMenu(ContextMenuHelper.contextMenu(tree, CTX_TREE_MENU_NEW_GROUP));
        ctxMenu.click();
        
        enterTextWLabel("Group ID:", groupId);
        enterTextWLabel("  Test IDs:", includedTestIds);
        pressKey(SWT.TAB);  // to activate listener
        m_bot.sleep(1000);
        clickButton("OK");
    }

    
    void createNewSubGroupWithContextCmd(String parentGroupId, String parentGroupPostfix,
                                      String groupId, 
                                      String includedTestIds) {
        clickTestTreeContextMenu(SWTBotConstants.BOT_TEST_TREE_ID, 
                                 parentGroupId + " : " + parentGroupPostfix, 
                                 UITestUtils.CTX_TREE_MENU_NEW_SUB_GROUP);
        enterTextWLabel("Group ID:", groupId);
        enterTextWLabel("  Test IDs:", includedTestIds);
        pressKey(SWT.TAB);  // to activate listener
        m_bot.sleep(1000);
        clickButton("OK");
    }

    
    
    /**
     * This method creates test cases for report testing - they should contain
     * all items, which are present in test reports, for example expected
     * expressions, coverage, profiler and scripts sections. 
     * @throws Exception 
     */
    void createTestsForFunctionCoverageProfilerAndScripts() throws Exception {
        createNewBaseTest(false, "", "min_int", "3,  5", "rv == 3", "rv", true);
        selectTestSection(SEC_META);
        setTestDescription("Should pass.");  setTestID("OK-1");
        setTestCaseLogging("iCounter", "rv");
        setSectionPreCondition(new String[]{"g_char1 == 0", "g_char2 == 0"});
        
        // will fail, so that we can test error report
        createNewBaseTest(false, "", "min_int", "3,  5", "rv == 6", "rv", true); 
        selectTestSection(SEC_META);
        setTestDescription("Should FAIL in Expected section.");  setTestID("FAIL-2");

        // trace test
        createNewBaseTest(false, "", "add_int", "3,  5", "rv == 8", "rv", false); 
        selectTestSection(SEC_META);
        setTestDescription("Should pass.");  setTestID("OK-3");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionTrace(RB_YES, "XML", "exportTrace.xml");

        // coverage test which succeeds
        createNewBaseTest(false, "", "min_int", "50,  30", "rv == 30", "rv", false);
        selectTestSection(SEC_META);
        setTestDescription("Should pass.");  setTestID("OK-4");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionCoverageTestCase(RB_YES, 
                           "XML", "cvrgExport-1.xml", "", RB_NO,
                           RB_NO, RB_NO, RB_NO, 
                           RB_NO, RB_NO, RB_NO, 
                           RB_NO, "", "");
        
        setSectionCoverageStatistics("min_int", 50, 50, 50, 50, 0, 0);
        
        // coverage test which fails
        createNewBaseTest(false, "", "max_int", "30,  50", "rv == 30", "rv", false);
        selectTestSection(SEC_META);
        setTestDescription("Should FAIL in 'Expected' and 'Coverage stats' sections."); setTestID("FAIL-5");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionCoverageTestCase(RB_YES, 
                           "XML", "cvrgExport-2.xml", "", RB_NO,
                           RB_NO, RB_NO, RB_NO, 
                           RB_NO, RB_NO, RB_NO, 
                           RB_NO, "", "");
        
        setSectionCoverageStatistics("max_int", 100, 50, 50, 50, 50, 50);
        
        // profiler test w. function, variable and values, which succeeds
        createNewBaseTest(false, "", "max_int", "70,  750", "rv == 750", "rv", false);
        selectTestSection(SEC_META);
        setTestDescription("Should pass."); setTestID("OK-6");
        setVarInit(0, "iCounter", " 0");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionProfiler(RB_YES,  
                           "XML", "testProfilerExport.xml", 
                           RB_NO, RB_NO, RB_NO);
        setProfilerStatsForRealTestOK();
        
        // profiler test w. function, variable and values, which fails
        createNewBaseTest(false, "", "max_int", "70,  750", "rv == 750", "rv", false);
        selectTestSection(SEC_META);
        setTestDescription("Should FAIL in Profiler section."); setTestID("FAIL-7");
        setVarInit(0, " iCounter", " 0");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionProfiler(RB_YES,  
                           "XML", "testProfilerExport.xml", 
                           RB_NO, RB_NO, RB_NO);
        setProfilerStatsForRealTestFail();
            
        // script test which succeeds
        setPropertiesScriptConfig("sampleTestExtensions");        
        createNewBaseTest(false, "", "funcTestCharArray1", "localArray", "localArray[0] == 'w'", "", false);
        selectTestSection(SEC_META);
        setTestDescription("Should pass."); setTestID("OK-8");
        setVar(0, "char[20]", "localArray", "{9, 8, 7, 6, 5, 4, 3, 2}");
        setSectionScripts("initTarget", "", "preFuncCall",
                          "0",  // signal to script to succeed
                          "postFuncCall", "\"'a'\"", "restoreTarget", "", false);
        
        // script test which fails
        createNewBaseTest(false, "", "funcTestCharArray1", "localArray", "localArray[1] == 'a'", "", false);
        selectTestSection(SEC_META);
        setTestDescription("Should FAIL in Scripts section."); setTestID("FAIL-9");
        setVar(0, "char[20]", "localArray", "{9, 8, 7, 6, 5, 4, 3, 2}");
        setSectionScripts("initTarget", "", "preFuncCall", 
                          "1", // signal to script to fail 
                          "postFuncCall", "\"'a'\"", "restoreTarget", "",
                          false);
    }


    // test cases created here can be run with standard testIDEA sample
    void createTestsVerifySymbolsAndRename() throws Exception {
        createNewBaseTest(false, "", "min_int", "3,  5", "rv == 3", "rv", true);
        setTestID("test-0");

        createNewBaseTest(false, "", "add_int", "3,  5", "rv == 8", "rv", false);
        setTestID("test-1");
        setSectionStub("max_int", "Yes", "", "rVal", "preFuncCall", "0", "23", false);

        // coverage test 
        createNewBaseTest(false, "", "min_int", "50,  30", "rv == 30", "rv", false);
        setTestID("test-2");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionCoverageTestCase("Yes", 
                           "XML", "cvrgExport-1.xml", "", RB_NO,
                           RB_NO, RB_NO, RB_NO, 
                           RB_NO, RB_NO, RB_NO, 
                           RB_NO, "", "");
        
        setSectionCoverageStatistics("min_int", 50, 50, 50, 50, 0, 0);
        
        // profiler test w. function, variable and values
        createNewBaseTest(false, "", "min_int", "70,  750", "rv == 70", "rv", false);
        setTestID("test-3");
        setVarInit(0, "iCounter", " 0");
        setSectionAnalyzer("Start", "testTrace.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        setSectionProfiler(RB_NO,  
                           "XML", "testProfilerExport.xml", 
                           RB_NO, RB_NO, RB_NO);
        setProfilerStatsSimple("min_int", "iCounter");
        
        // script test
        createNewBaseTest(false, "", "funcTestCharArray1", "localArray", "localArray[0] == 'f'", "", false);
        setTestID("test-4");
        setVar(0, "char[20]", "localArray", "{9, 8, 7, 6, 5, 4, 3, 2}");
    }


    public void setProfilerStatsForRealTestFail() throws Exception {
        setSectionProfilerArea(true, "max_int", 
                               new String[]{"1000", "2000 s", "3000 ms", "4000 us", "5000ns", "6000", "7000", "8000"},
                               new String[]{"1001", "2001 s", "3001 ms", "4001 us", "5001ns", "6001", "7001", "8001"},
                               new String[]{"1002", "2002 s", "3002 ms", "4002 us", "5002ns", "6002", "7002", "8002"},
                               new String[]{"1003", "2003 s", "3003 ms", "4003 us", "5003ns", "6003", "7003", "8003"},
                               new String[]{"1004", "2004 s", "3004 ms", "4004 us", "5004ns", "6004", "7004", "8004"},
                               new String[]{"1005", "2005 s", "3005 ms", "4005 us", "5005ns", "6005", "7005", "8005"},
                               new String[]{"1006", "2006 s", "3006 ms", ""       , "5006ns", "6006", "7006", "8006"},
                               new String[]{"1007", "2007 s", "3007 ms", ""       , "5007ns", "6007", "7007", "8007"},
                               "123", "124");
        
        setSectionProfilerArea(false, "iCounter/1", 
                               new String[]{"1100", "2000 s", "3000 ms", "4000 us", "5000ns", "6000", "7000", "8000"},
                               new String[]{"1101", "2001 s", "3001 ms", "4001 us", "5001ns", "6001", "7001", "8001"},
                               new String[]{"1102", "2002 s", "3002 ms", "4002 us", "5002ns", "6002", "7002", "8002"},
                               new String[]{"1103", "2003 s", "3003 ms", "4003 us", "5003ns", "6003", "7003", "8003"},
                               new String[]{"1104", "2004 s", "3004 ms", "4004 us", "5004ns", "6004", "7004", "8004"},
                               new String[]{"1105", "2005 s", "3005 ms", "4005 us", "5005ns", "6005", "7005", "8005"},
                               new String[]{"1106", "2006 s", "3006 ms", ""       , "5006ns", "6006", "7006", "8006"},
                               new String[]{"1107", "2007 s", "3007 ms", ""       , "5007ns", "6007", "7007", "8007"},
                               "123", "124");
        
        setSectionProfilerArea(false, "iCounter/2", 
                               new String[]{"1200", "2000 s", "3000 ms", "4000 us", "5000ns", "6000", "7000", "8000"},
                               new String[]{"1201", "2001 s", "3001 ms", "4001 us", "5001ns", "6001", "7001", "8001"},
                               new String[]{"1202", "2002 s", "3002 ms", "4002 us", "5002ns", "6002", "7002", "8002"},
                               new String[]{"1203", "2003 s", "3003 ms", "4003 us", "5003ns", "6003", "7003", "8003"},
                               new String[]{"1204", "2004 s", "3004 ms", "4004 us", "5004ns", "6004", "7004", "8004"},
                               new String[]{"1205", "2005 s", "3005 ms", "4005 us", "5005ns", "6005", "7005", "8005"},
                               new String[]{"1206", "2006 s", "3006 ms", ""       , "5006ns", "6006", "7006", "8006"},
                               new String[]{"1207", "2007 s", "3007 ms", ""       , "5007ns", "6007", "7007", "8007"},
                               "123", "124");
        
        setSectionProfilerArea(false, "iCounter/3", 
                               new String[]{"1200", "2000 s", "3000 ms", "4000 us", "5000ns", "6000", "7000", "8000"},
                               new String[]{"1201", "2001 s", "3001 ms", "4001 us", "5001ns", "6001", "7001", "8001"},
                               new String[]{"1202", "2002 s", "3002 ms", "4002 us", "5002ns", "6002", "7002", "8002"},
                               new String[]{"1203", "2003 s", "3003 ms", "4003 us", "5003ns", "6003", "7003", "8003"},
                               new String[]{"1204", "2004 s", "3004 ms", "4004 us", "5004ns", "6004", "7004", "8004"},
                               new String[]{"1205", "2005 s", "3005 ms", "4005 us", "5005ns", "6005", "7005", "8005"},
                               new String[]{"1206", "2006 s", "3006 ms", ""       , "5006ns", "6006", "7006", "8006"},
                               new String[]{"1207", "2007 s", "3007 ms", ""       , "5007ns", "6007", "7007", "8007"},
                               "123", "124");
        
        setSectionProfilerArea(false, "iCounter", 
                               new String[]{"1300", "2000 s", "3000 ms", "4000 us", "5000ns", "6000", "7000", "8000"},
                               new String[]{"1301", "2001 s", "3001 ms", "4001 us", "5001ns", "6001", "7001", "8001"},
                               new String[]{"1302", "2002 s", "3002 ms", "4002 us", "5002ns", "6002", "7002", "8002"},
                               new String[]{"1303", "2003 s", "3003 ms", "4003 us", "5003ns", "6003", "7003", "8003"},
                               new String[]{"1304", "2004 s", "3004 ms", "4004 us", "5004ns", "6004", "7004", "8004"},
                               new String[]{"1305", "2005 s", "3005 ms", "4005 us", "5005ns", "6005", "7005", "8005"},
                               new String[]{"1306", "2006 s", "3006 ms", ""       , "5006ns", "6006", "7006", "8006"},
                               new String[]{"1307", "2007 s", "3007 ms", ""       , "5007ns", "6007", "7007", "8007"},
                               "123", "124");
    }
    
    
    public void setProfilerStatsForRealTestOK() throws Exception {
        // these values are set so that test passes on different targets - limits are very large
        setSectionProfilerArea(true, "max_int", 
                               new String[]{"0",   "0",     "0",    "0" , "4005", "0", "0", "0"},
                               new String[]{"8 s", "8 s"  ,  "8 s",     "8 s",    "8 s", "8 s", "8 s", "8 s"},
                               
                               new String[]{"0",      "0",     "0",         "0",        "4002ns", "4002", "0", "0"},
                               new String[]{"503 s", "8 s", "72003 ms",  "80003 ms", "8s", "8 s", "8s", "8 s"},
                               
                               new String[]{"0", "0",   "0",               "0",       "1004ns", "2004", "0", "0"},
                               new String[]{"8 s", "85 s", "5005 ms",  "400005 us", "8s", "8.2s", "8.5s", "8.7s"},
                               
                               new String[]{"0", "0 s", "0 ms",           ""       , "0ns", "0", "0", "0"},
                               new String[]{"1007", "2007 s", "3007 ms", ""        , "5ns", "6", "7", "9007"},
                               "1", "124");
        
        setSectionProfilerArea(false, "iCounter/1", 
                               new String[]{"500", "200",    "300",     "400 ns",   "1000ns", "2000", "3000", "3000"},
                               new String[]{"4101ms", "2001 s", "3001 ms", "90001 us",  "800ms",   "802ms", "803ms", "804ms"},
                               
                               new String[]{"1102", "202", "302", "402", "2002ns",   "2202",   "2222", "1232"},
                               new String[]{"3103", "2003 s", "3003 ms", "4003 us",  "17003ns", "18003", "17003", "18003"},
                               
                               new String[]{"0",    "0000 s", "0000 ms", "0",        "0", "0", "0", "0"},
                               new String[]{"1105", "2005 s", "3005 ms", "900 ms",  "1", "6005", "7005", "8005"},
                               
                               new String[]{"0",    "0",      "0",       ""       ,  "0", "0", "0", "0"},
                               new String[]{"1107", "2007 s", "3007 ms", ""       ,  "5", "6007", "7007", "8007"},
                               "1", "124");
        
        setSectionProfilerArea(false, "iCounter/2", 
                               new String[]{"0", "0",    "0",     "0 ns",     "0ns", "0", "0", "0"},
                               new String[]{"900 ms", "4001 s", "4001 ms", "4001000 us",    "900ms", "8001s", "18001s", "22001s"},
                               
                               new String[]{"0", "0",    "0",     "0",        "1ns",     "0",   "0", "0"},
                               new String[]{"3103", "4003 s", "4003 ms", "4003 us", "5003ns", "6003", "19003", "993 ms"},
                               
                               new String[]{"0",    "0 s",    "0 ms", "0",          "0", "0", "0", "0"},
                               new String[]{"1105", "2005 s", "3005 ms", "100 ms", "1", "6005", "7005", "8005"},
                               
                               new String[]{"0",    "0",      "0",    ""       ,    "0", "0", "0", "0"},
                               new String[]{"1107", "2007 s", "3007 ms", ""       , "5", "6007", "7007", "8007"},
                               "1", "1");
    }
    
    
    public void setProfilerStatsForEditorTest(String profilerFuncArea,
                                              String[][] funcTimes, 
                                              int hitsFuncLow, int hitsFuncHigh,
                                              String profilerDataArea,
                                              String[][] dataTimes,
                                              int hitsDataLow, int hitsDataHigh) throws Exception {
        setSectionProfilerArea(true, profilerFuncArea,
                               funcTimes[0], funcTimes[1],
                               funcTimes[2], funcTimes[3],
                               funcTimes[4], funcTimes[5],
                               funcTimes[6], funcTimes[7],

                               
                               /* new String[]{"1000", "2000 s", "3000 ms", "4000 us", "5000ns", "6000", "7000", "8000"},
                               new String[]{"1001", "2001 s", "3001 ms", "4001 us", "5001ns", "6001", "7001", "8001"},
                               new String[]{"1002", "2002 s", "3002 ms", "4002 us", "5002ns", "6002", "7002", "8002"},
                               new String[]{"1003", "2003 s", "3003 ms", "4003 us", "5003ns", "6003", "7003", "8003"},
                               new String[]{"1004", "2004 s", "3004 ms", "4004 us", "5004ns", "6004", "7004", "8004"},
                               new String[]{"1005", "2005 s", "3005 ms", "4005 us", "5005ns", "6005", "7005", "8005"},
                               new String[]{"1006", "2006 s", "3006 ms", ""       , "5006ns", "6006", "7006", "8006"},
                               new String[]{"1007", "2007 s", "3007 ms", ""       , "5007ns", "6007", "7007", "8007"}, */
                               String.valueOf(hitsFuncLow), String.valueOf(hitsFuncHigh));
        
        setSectionProfilerArea(false, profilerDataArea, 
                               dataTimes[0], dataTimes[1],
                               null, null, // gross time is not used in data profiler 
                               dataTimes[2], dataTimes[3],
                               dataTimes[4], dataTimes[5],
                               String.valueOf(hitsDataLow), String.valueOf(hitsDataHigh));
    }
    

    public void setProfilerStatsSimple(String functionName, String varName) throws Exception {
        if (functionName != null) {
            setSectionProfilerArea(true, functionName, 
                                   new String[]{"2001", "", "",     "" , "", "",  "", ""},
                                   new String[]{"8001", "", "",     "",  "", "",  "", ""},

                                   null,
                                   null,

                                   null,
                                   null,

                                   null,
                                   null,
                                   "1", "124");
        }
        
        if (varName != null) {
            setSectionProfilerArea(false, varName, 
                                   new String[]{"2001", "", "",     "" , "", "",  "", ""},
                                   new String[]{"8001", "", "",     "",  "", "",  "", ""},

                                   null,
                                   null,

                                   null,
                                   null,

                                   null,
                                   null,
                                   "1", "124");
            
        }
    }
    
    
    public void createSystemTestCaseWCoverage(String testId, String cvrgExportFile, boolean isFullExport) throws Exception {
        
        createNewBaseSystemTest("", "isDebugTest == 1", false);
        selectTestSection(SEC_META);
        setTestDescription("Should pass.");  setTestID(testId);
        setSectionEndStopCond("Stop", 2000, "", 0, "");
        setVarInit(0, "isDebugTest", "1");
        setSectionAnalyzer("Start", "testCvrg.trd", "Write", RB_NO, RB_NO, RB_NO, RB_NO, "", false);
        if (isFullExport) {
            setSectionCoverageTestCase("Yes", 
                               "XML", cvrgExportFile, "", "Yes",
                               "Yes", "Yes", "Yes",    // export all available info 
                               "Yes", "Yes", "Yes", 
                               RB_NO, "", "");
        } else {
            setSectionCoverageTestCase("Yes", 
                               "XML", cvrgExportFile, "",  RB_NO,
                               RB_NO, RB_NO, RB_NO,       // do not export extras
                               RB_NO, RB_NO, RB_NO, 
                               RB_NO, "", "");
        }
        
        setSectionCoverageStatistics("Type_Simple", 50, 50, 50, 0, 0, 10);
        setSectionCoverageStatistics("Func1", 50, 50, 50, 0, 0, 100);
    }
    
    
    void fillDialogNewTestPage1(boolean isSystemTest,
                                String coreId,
                                String funcName,
                                String params,
                                String expr,
                                String retValName,
                                boolean isCreateId,
                                boolean isDerivedTest,
                                boolean isClickFinish) {
        
        String title = isDerivedTest ? NEW_DERIVED_TEST_CASE_WIZARD : NEW_TEST_CASE_WIZARD;
        
        waitForShell(title, 3000, true); // model will be modified by rename
        SWTBotShell shellbot = m_bot.shell(title);
        
        shellbot.activate();

        if (isSystemTest) {
            clickRadioWLabel("System");
        } else {
            m_bot.comboBoxWithLabel("Function:").setText(funcName);
            m_bot.textWithLabel("Parameters:").setText(params);
            if (retValName != null) {
                m_bot.textWithLabel(RET_VAL_NAME).setText(retValName);
            }
        }

        m_bot.textWithLabel("Expression:").setText(expr);
        selectCheckBoxWText("Auto generate test ID", isCreateId);

        SWTBotCombo coreIdCombo = m_bot.comboBoxWithLabel("Core ID:");
        if (!coreId.isEmpty()  &&  !coreIdCombo.isEnabled()) {
            throw new IllegalStateException("Core ID is specified, but combo is not enabled!");
        }
        
        // When there are no core IDs specified in configuration, the combo will 
        // be disabled.
        if (coreIdCombo.isEnabled()) {
            coreIdCombo.setText(coreId);
        }
        
        if (isClickFinish) {
            m_bot.button("Finish").click();
        }
    }
    
    
    void fillDialogNewTestFuncRow(int dataRow,
                                  Boolean isStub, String stubRv,
                                  Boolean isUserStub, String replFunc,
                                  Boolean isTp, String tpId,
                                  Boolean isCvrg, String codeCvrg, String condCvrg,
                                  Boolean isProf, String minGross, String maxGross) {
        int row = dataRow + 2;
        if (isStub) {
            m_ktableUtils.clickCell(1, row);
        }
        if (stubRv != null) {
            m_ktableUtils.setCellContent(2, row, stubRv);
        }

        if (isUserStub) {
            m_ktableUtils.clickCell(3, row);
        }
        if (replFunc != null) {
            m_ktableUtils.setCellContent(4, row, replFunc);
        }

        if (isTp) {
            m_ktableUtils.clickCell(5, row);
        }
        if (tpId != null) {
            m_ktableUtils.setCellContent(6, row, tpId);
        }
        
        if (isCvrg) {
            m_ktableUtils.clickCell(7, row);
        }
        if (codeCvrg != null) {
            m_ktableUtils.setCellContent(8, row, codeCvrg);
        }
        if (condCvrg != null) {
            m_ktableUtils.setCellContent(9, row, condCvrg);
        }
        
        if (isProf) {
            m_ktableUtils.clickCell(10, row);
        }
        if (minGross != null) {
            m_ktableUtils.setCellContent(11, row, minGross);
        }
        if (maxGross != null) {
            m_ktableUtils.setCellContent(12, row, maxGross);
        }
    }
    
    
    /**
     * @param seqStartIdx radio for sequence start
     * @param i index of radio button in the type group
     * @param j index of radio button in the scope group
     */
    public void fillDialogAutoIdCommand(int seqStartIdx, int i, int j) {
        /* This workaround is required if you don't have fixed version of SWTBot,
         * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=344484
         * 
         * The disadvantage is that you have to know the previously selected 
         * radio button :-( 
        final SWTBotRadio radio = m_bot.radio(0);
        UIThreadRunnable.syncExec(new VoidResult() {
            public void run() {
                radio.widget.setSelection(false);
                }
        }); */
        clickRadio(m_bot.radio(seqStartIdx));
        clickRadio(m_bot.radio(i + 4));
        clickRadio(m_bot.radio(j + 8));

        m_bot.button("OK").click();
    }


    void selectTestSection(String sectionName) throws Exception {
        // m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        m_bot.tree(SECTION_TREE_IDX).select(sectionName);
    }


    void selectSubSection(String sectionName, String subSectionName) {
        // m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        m_bot.tree(SECTION_TREE_IDX).expandNode(sectionName, subSectionName).select();
    }


    void selectSubSubSection(String sectionName, String subSectionName, String subSubSectionName) {
        // m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        m_bot.tree(SECTION_TREE_IDX).expandNode(sectionName, 
                                                subSectionName, 
                                                subSubSectionName).select();
    }


    /*void selectTestCase(int idx) throws Exception {
        m_bot.tree(0).select(idx);
    } */

    void selectTestCase(int... idxPath) throws Exception {
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);

        SWTBotTreeItem treeItem = tree.getAllItems()[idxPath[0]];
                
        for (int i = 1; i < idxPath.length; i++) {
            int idx = idxPath[i];
            treeItem = treeItem.getNode(idx);
        }
        treeItem.select();
    }


    /**
     * 
     * @param paths array of integer arrays, where each array represents index 
     *        path from root to the item to be selected
     * @throws Exception
     */
    void selectTestCases(int[]...paths) throws Exception {
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        List<SWTBotTreeItem> treeItems = new ArrayList<SWTBotTreeItem>();

        for (int [] idxPath : paths) {

            SWTBotTreeItem treeItem = tree.getAllItems()[idxPath[0]];

            for (int i = 1; i < idxPath.length; i++) {
                int idx = idxPath[i];
                treeItem = treeItem.getNode(idx);
            }
            treeItems.add(treeItem);
        }
        tree.select(treeItems.toArray(new SWTBotTreeItem[0]));
    }


    void selectTestTreeNode(String testSpecIdFunc) throws Exception {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        tree.select(testSpecIdFunc);
    }


    void selectTestCase(String testId, String funcName) throws Exception {
        selectTestTreeNode(testId + " : " + funcName);
    }


    void selectTestGroup(String groupId, String groupInfo) throws Exception {
        selectTestTreeNode(groupId + " : " + groupInfo);
    }


    void selectTestCases(String ... testSpecIdFuncs) throws Exception {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        tree.select(testSpecIdFuncs);
    }

    void selectDerivedTestCase(String testSpecIdFunc, String derivedIdFunc) throws Exception {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        SWTBotTreeItem item = tree.getTreeItem(testSpecIdFunc);
        item.expand();
        item.select(derivedIdFunc);
    }


    /**
     * 
     * @param testIdFunc string in format: 'testID : funcName'. Use 
     *        TestSpecificationModel.getTestSpecificationName() for formatting.
     * @return
     * @throws Exception
     */
    CTestSpecification getTestSpecViaClipboard(String testIdFunc) throws Exception {
        selectTestTreeNode(testIdFunc);
        copyToClipboard();
        return getTestSpecFromClipboard();
    }


    void dragAndDropDerivedTestCase(String parentTestSpecIdFunc, 
                                    String srcDerivedIdFunc,
                                    String destDerivedIdFunc) throws Exception {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        SWTBotTreeItem parentItem = tree.getTreeItem(parentTestSpecIdFunc);

        SWTBotTreeItem srcDerivedItem = parentItem.getNode(srcDerivedIdFunc);
        SWTBotTreeItem destDerivedItem = parentItem.getNode(destDerivedIdFunc);

        m_dnd.dragAndDrop(srcDerivedItem, destDerivedItem);
    }


    void setPropertiesScriptConfig(String extensionModule) {

        openDialogWithMainMenu(MENU_FILE, MENU_FILE__PROPERTIES, DIALOG_TITLE_PROJECT_PROPERTIES);
        
        /* m_bot.menu(MENU_FILE).menu(MENU_FILE__PROPERTIES).click();

        waitForConnectionDialog();

        final String propsShellTitle = DIALOG_TITLE_PROJECT_PROPERTIES;
        waitForShell(propsShellTitle, 3000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(propsShellTitle);
        shellbot.activate(); */

        m_bot.tree(0).select("Scripts");
        m_bot.textWithLabel("Script working folder:").setText("D:\\bb\\trunk\\sdk\\targetProjects");
        m_bot.textWithLabel("Imported modules:").setText(extensionModule);
        m_bot.textWithLabel("Extension class:").setText("sampleTestExtensions.SampleExtension");
        m_bot.textWithLabel("Timeout:").setText("10");
        m_bot.sleep(500);
        m_bot.button("OK").click();
    }


    
    
    public String getFUTName() throws Exception {
        return m_bot.comboBoxWithLabel("Function:").getText();
    }


    public void pressKey(String key) throws ParseException {
        m_keyboard.pressShortcut(KeyStroke.getInstance(key));
    }


    public void pressKey(char chr) {
        m_keyboard.pressShortcut(KeyStroke.getInstance(chr));
    }


    /**
     * 
     * @param keyCode for example SWT.HOME
     */
    public void pressKey(int keyCode) {
        m_keyboard.pressShortcut(KeyStroke.getInstance(keyCode));
    }


    public void pressKey(int modifier, char chr) {
        m_keyboard.pressShortcut(modifier, chr);
        // alternatives for more complex cases
        // m_keyboard.pressShortcut(Keystrokes.CTRL, KeyStroke.getInstance(0, 'c'));
        // KeyboardFactory.getAWTKeyboard().pressShortcut(Keystrokes.CTRL, KeyStroke.getInstance(0, 'X'));
    }

    public void pressKey(int modifier, int keyCode) {
        m_keyboard.pressShortcut(modifier, keyCode, (char)0);
    }

    public void cutToClipboard() {
        pressKey(SWT.CONTROL, 'x');
    }


    /** Copies selected item to clipboard. */
    public void copyToClipboard() {
        pressKey(SWT.CONTROL, 'c');
        m_bot.sleep(500);
    }


    public void pasteFromClipboard() {
        pressKey(SWT.CONTROL, 'v');
    }


    public void undo() {
        pressKey(SWT.CONTROL, 'z');
    }


    public void redo() {
        pressKey(SWT.CONTROL, 'y');
    }


    void selectAll() {
        pressKey(SWT.CONTROL, 'a');
    }

    
    void clearSection(String sectionLabel) throws Exception {
        selectTestSection(sectionLabel);
        clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, sectionLabel, CMD_CLEAR_SECTION);
    }

    
    void contextMenu(String sectionLabel, String command) throws Exception {
        // System.out.println("context Menu: " + sectionLabel + " / " + command);
        selectTestSection(sectionLabel);
        clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, sectionLabel, command);
    }


    void contextMenu(String sectionLabel, String subSectionLabel, String command) throws Exception {
        // System.out.println("context Menu: " + sectionLabel + " / " + command);
        selectSubSection(sectionLabel, subSectionLabel);
        clickTestEditorContextMenu(SWTBotConstants.BOT_EDITOR_TREE_ID, sectionLabel, subSectionLabel, command);
    }


    /**
     * Sets text in Text component so that all listeners are triggered and also derived
     * test specs override the base settings.
     * 
     * @param label label before the text component to set test to
     * @param newText text to set
     * @param labelOfNextFocus label of the next field to receive focus so that 
     *        set action is triggered
     * 
     * @throws ParseException
     */
    void setTextFieldForDerived(String label, 
                                String newText, 
                                String labelOfNextFocus) throws ParseException {
        SWTBotText widget = m_bot.textWithLabel(label);
        widget.setFocus();
        pressKey(" "); // this way derived field is no longer derived
        pressKey(SWT.BS); // delete the above space
        widget.setText(newText);
        m_bot.textWithLabel(labelOfNextFocus).setFocus();
    }

    
    void setTextFieldForDerivedWId(String componentId, 
                                   String newText, 
                                   String labelOfNextFocus) throws ParseException {
        SWTBotText widget = m_bot.textWithId(componentId);
        widget.setFocus();
        pressKey(" "); // this way derived field is no longer derived
        pressKey(SWT.BS); // delete the above space
        widget.setText(newText);
        m_bot.textWithLabel(labelOfNextFocus).setFocus();
    }
    
    
    void setTestID(String id) throws Exception {
        selectTestSection(SEC_META);
        setTextFieldForDerived("ID:", id, "Tags:");
    }

    
    void setTestDescription(String desc) throws Exception {
        m_bot.styledTextWithLabel("Description:").setFocus();
        m_bot.styledTextWithLabel("Description:").setText(desc);
        m_bot.textWithLabel("Tags:").setFocus();
    }

    
    void setTestTags(String tags) throws Exception {
        m_bot.textWithLabel("Tags:").setFocus();
        m_bot.textWithLabel("Tags:").setText(tags);
        m_bot.styledTextWithLabel("Description:").setFocus();
    }

    
    void setFUTName(String functionName) throws Exception {
        m_bot.comboBoxWithLabel("Function:").setFocus();
        m_bot.comboBoxWithLabel("Function:").setText(functionName);
        m_bot.textWithLabel(RET_VAL_NAME).setFocus();
    }


    void setFUTParams(String params) throws Exception {
        m_bot.textWithLabel("Params:").setFocus();
        m_bot.textWithLabel("Params:").setText(params);
        m_bot.textWithLabel(RET_VAL_NAME).setFocus();
    }

    void setFUTRetValName(String returnValueName) throws Exception {
        m_bot.textWithLabel(RET_VAL_NAME).setFocus();
        m_bot.textWithLabel(RET_VAL_NAME).setText(returnValueName);
        m_bot.textWithLabel("Params:").setFocus();
    }


    void setSectionMeta(boolean isExecute,
                        String testScope,
                        String id, String description, 
                        String tags) throws Exception {
        
        selectTestSection(SEC_META);
        
        if (isExecute) {
            m_bot.checkBox("Execute").select();
        } else {
            m_bot.checkBox("Execute").deselect();
        }
        
        clickRadioWLabel(testScope);
        enterTextWLabel("ID:", id);
        enterStyledTextWLabel("Description:", description);
        enterTextWLabel("Tags:", tags);
    }
    
    
    void setSectionFunction(String funcName, 
                            String params, 
                            String retVal) throws Exception {
        selectTestSection(SEC_FUNCTION);
        
        enterComboWLabel("Function:", funcName);
        enterTextWLabel("Params:", params);
        enterTextWLabel(RET_VAL_NAME, retVal);
    }
    
    
    void setSectionBeginStopCond(String stopType, 
                                 int timeout,
                                 String realTimeExpr, 
                                 int condCount, 
                                 String condExpr) throws Exception {
        
        setSectionStopCond(SEC_SYS_BEGIN_SCOND, stopType, timeout, realTimeExpr, 
                           condCount, condExpr);
    }
    
    
    void setSectionEndStopCond(String stopType, 
                               int timeout,
                               String realTimeExpr, 
                               int condCount, 
                               String condExpr) throws Exception {
        setSectionStopCond(SEC_SYS_END_SCOND, stopType, timeout, realTimeExpr, 
                           condCount, condExpr);
    }
    
    
    final String STOP_COND_BREAKPOINT = "Breakpoint";
    final String STOP_COND_STOP = "Stop";
    final String STOP_COND_RT_EXPR = "Real-time expr.";

    
    /** For location call method setLocationDialog(). */
    void setSectionStopCond(String sectionLabel, 
                            String stopType, 
                            int timeout,
                            String realTimeExpr, 
                            int condCount, 
                            String condExpr) throws Exception {

        selectTestSection(sectionLabel);

        clickRadioWLabel(stopType);
        
        if (stopType.equals(STOP_COND_BREAKPOINT)) {
            enterTextWLabel("Timeout:", String.valueOf(timeout));
            enterTextWLabel("Hit count:", String.valueOf(condCount));
            enterTextWLabel("Cond. expr.:", condExpr);
        } else if (stopType.equals(STOP_COND_STOP)) {
            enterTextWLabel("Timeout:", String.valueOf(timeout));
        } else if (stopType.equals(STOP_COND_RT_EXPR)) {
            enterTextWLabel("Timeout:", String.valueOf(timeout));
            enterTextWLabel("Real-time expr:", realTimeExpr);
        } else {
            throw new IllegalArgumentException("Unknown stop type label: " + stopType);
        }
        
        m_bot.sleep(750);
        pressKey(SWT.TAB);
        m_bot.sleep(200);
    }
    
    
    void setSectionPreCondition(String [] expressions) throws Exception {
        selectTestSection(SEC_PRECONDITION);
        
        int row = 1;
        for (String expr : expressions) {
            setCellWithContentProposal(row++, expr, true, 0, 
                                SWTBotConstants.BOT_PRE_CONDITIONS_TABLE);
        }
    }
    
    
    void setSectionExpected(String maxStackUsed,
                            ETristate isExpectTargetException,
                            String ... expressions) throws Exception {
        
        selectTestSection(SEC_EXPECTED);
        
        enterTextWLabel("Max stack used:", maxStackUsed);

        if (isExpectTargetException != null) { // old tests do not set this value
            selectTristateCheckBoxWText("Expect target exception", 
                                        isExpectTargetException);
        }
        
        int row = 1;
        for (String expr : expressions) {
            setCellWithContentProposal(row++, expr, true, 0, 
                                SWTBotConstants.BOT_EXPECTED_EXPR_TABLE);
        }
    }
    
    
    void setVerificationExpr(int row, String expression, 
                             boolean isReplace, int contentRow) throws Exception {
    
        setCellWithContentProposal(row, expression, isReplace, contentRow, 
                            SWTBotConstants.BOT_EXPECTED_EXPR_TABLE);
    }
    
    
    /**
     * 
     * @param row number of table row
     * @param expression text to write into the selected row
     * @param isReplace if true, the existing text is replaced, otherwise the
     * @param contentProposalsRow index of content proposal to select 
     * new text is appended 
     */
    void setCellWithContentProposal(int row, String expression, 
                                    boolean isReplace, int contentProposalsRow,
                                    String tableId) throws Exception {
        
        m_ktableUtils.selectKTable(tableId);
        
        m_ktableUtils.setCellContent(1, row, expression, !isReplace, contentProposalsRow);

        /*
        SWTBotTable table = m_bot.tableWithId(tableId);
        table.doubleClick(row, 0);
        m_bot.sleep(100); 

        if (!isReplace) {
            // if there was already text in the table, do not replace it, because it is 
            // selected after the double click
            pressKey(IKeyLookup.ESC_NAME); // close content proposals box
            pressKey(IKeyLookup.END_NAME); // go to the end of the line to keep existing text
            m_bot.sleep(100);
        }

        m_keyboard.typeText(expression);
        // System.out.println("expression: " + expression);
        m_bot.sleep(200); // some delay is needed for content proposals to show up
        if (contentProposalsRow > 0) {
            for (int i = 0; i < contentProposalsRow; i++) {
                pressKey(IKeyLookup.ARROW_DOWN_NAME);
            }
        }
        pressKey(SWT.CR);  // use the selected proposal
        */
    }
    
    
    void setFilterForTestCases(String includedFunctions, 
                               String excludedFunctions, 
                               String includedTestIds, 
                               String excludedTestIds) {
        waitForShell(DIALOG_TITLE_TEST_CASE_FILTER, 2000, true);
        SWTBotShell shellbot = m_bot.shell(DIALOG_TITLE_TEST_CASE_FILTER);
        shellbot.activate();

        setFilterFunctionsAndIds(includedFunctions,
                                 excludedFunctions,
                                 includedTestIds,
                                 excludedTestIds);
        pressKey(SWT.HOME);  // key to trigger on-the-fly filtering
    }


    private void setFilterFunctionsAndIds(String includedFunctions,
                                            String excludedFunctions,
                                            String includedTestIds,
                                            String excludedTestIds) {
        enterTextWLabel("Functions:", includedFunctions);
        enterTextWLabel("  Excluded functions:", excludedFunctions);
        enterTextWLabel("  Test IDs:", includedTestIds);
        enterTextWLabel("    Excluded test IDs:", excludedTestIds);
    }
    
    /**
     * 
     * @param textPos position of cursor in text before typing new text. -1 for end
     *                of existing text in control, -2 for no change of cursor position
     * @param expression
     * @param contentRow row in content proposals of proposal to select.
     * @throws ParseException 
     */
    void enterTextWithContentProposals(int textPos, String expression, int contentRow) throws ParseException {
        
        if (textPos != -2) {

            if (textPos == -1) {
                pressKey(IKeyLookup.END_NAME);
            } else {
                pressKey(IKeyLookup.HOME_NAME);
                while (textPos > 0) {
                    pressKey(IKeyLookup.ARROW_RIGHT_NAME);
                    textPos--;
                }
            }
        }
        
        m_keyboard.typeText(expression);
        // System.out.println("expression: " + expression);
        m_bot.sleep(200); // some delay is needed for content proposals to show up
        if (contentRow > 0) {
            for (int i = 0; i < contentRow; i++) {
                pressKey(IKeyLookup.ARROW_DOWN_NAME);
            }
        }
        
        pressKey(SWT.CR);  // use the selected proposal
    }
    
    
    void setSectionStub(String stubbedFuncName, 
                        String isActive, String params, 
                        String retValName, String scriptFunc,
                        String minHits, String maxHits,
                        boolean isChangeInheritState) throws Exception {
        
        selectTestSection(SEC_STUBS);
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }

        m_ktableUtils.addLineToListTable("Stubbed functions", stubbedFuncName);

        clickRadioWLabel(isActive);
        enterTextWLabel("Parameters:", params);
        enterTextWLabel("Ret. var. name:", retValName);
        enterTextWLabel("Script func.:", scriptFunc);
        setMinMaxLimits(minHits, maxHits);
    }

    
    void setMinMaxLimits(String minHits, String maxHits) {
        enterTextWLabel("Hits:", minHits);
        enterTextWLabel("<=", 1, maxHits);
    }

    public void setSectionUserStubs(String stubbedFuncName,
                                    String isActive,
                                    String stubType,
                                    String replFunc, 
                                    boolean isChangeInheritState) throws Exception {

        selectTestSection(SEC_USER_STUBS);
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }
        
        m_ktableUtils.addLineToListTable("User Stubs", stubbedFuncName);

        clickRadioWLabel(isActive);
        clickRadioWLabel(stubType);
        enterComboWLabel("Replacement f.:", replFunc);
    }


    public void setSectionTestPoint(String testPointName,
                                    String isActive,
                                    int condCount,
                                    String condExpr,
                                    String tpScriptFunc,
                                    boolean isChangeInheritState) throws Exception {
        
        selectTestSection(SEC_TEST_POINTS);
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }
        
        m_ktableUtils.addLineToListTable("Test points", testPointName);

        clickRadioWLabel(isActive);
        enterTextWLabel("Hit count:", String.valueOf(condCount));
        enterTextWLabel("Cond. expr.:", condExpr);
        enterTextWLabel("Script func.:", tpScriptFunc);
    }
    
    
    public void setSectionGroupMeta(String grpId, String desc) throws Exception {
        selectTestSection(SEC_META);
        
        // ID should be set first, so that TAB after this method changes focus and so data is saved.
        // TAB in multiline text control does not move the focus - it is taken literally.
        enterStyledTextWLabel("Description:", desc);
        enterTextWLabel("ID:", grpId);
    }


    void setSectionGroupFilter(String filterType, 
                               String coreId,
                               String partitions,
                               String modules,
                               
                               String includedFunctions, 
                               String excludedFunctions, 
                               String includedTestIds, 
                               String excludedTestIds,
                               
                               String mustHaveAll,
                               String isAndOr1,
                               String mustHaveOneOf,
                               String isAndOr2,
                               String mustNotHaveAnyOf,
                               String isAndOr3,
                               String mustNotHaveAtLeastOneOf) throws Exception {
            
        selectTestSection(SEC_GRP_FILTER);
            
        clickRadioWLabel(filterType);
        enterTextWLabel("Core ID:", coreId);
        enterTextWLabel("Partitions:", partitions);
        enterTextWLabel("Modules:", modules);

        setFilterFunctionsAndIds(includedFunctions,
                                 excludedFunctions,
                                 includedTestIds,
                                 excludedTestIds);

        enterTextWLabel("Must have all tags:", mustHaveAll);
        clickRadioWLabel(isAndOr1, 0);
        enterTextWLabel("Must have at least one of tags:", mustHaveOneOf);
        clickRadioWLabel(isAndOr2, 1);
        enterTextWLabel("Must NOT have any of tags:", mustNotHaveAnyOf);
        clickRadioWLabel(isAndOr3, 2);
        enterTextWLabel("Must NOT have at least one of tags:", mustNotHaveAtLeastOneOf);

        pressKey(SWT.HOME);  // key to trigger on-the-fly filtering
    }

        
    void setSectionGroupScripts(String initFunc, String initFuncParams,
                                String endFunc, String endFuncParams) throws Exception {
        selectTestSection(SEC_GRP_SCRIPTS);
        
        enterTextWLabel("Script function:", 0, initFunc);
        enterTextWLabel("Parameters:", 0, initFuncParams);
        
        enterTextWLabel("Script function:", 2, endFunc);
        enterTextWLabel("Parameters:", 2, endFuncParams);
    }

    

    public void setSrcLocationDlg(String resType, String resName, int line,
                                  String searchLine, int linesRange, 
                                  String searchContext, String matchType,
                                  String searchPattern, int lineOffset,
                                  int numSteps) {
        
        m_bot.button("Edit").click();

        clickRadioWLabel(resType);
        enterComboWLabel(resType + " name:", resName);
        enterTextWLabel("Line:", String.valueOf(line));
        
        clickRadioWLabel(searchLine);
        enterTextWLabel("Lines range:", String.valueOf(linesRange));
        
        clickRadioWLabel(searchContext);
        clickRadioWLabel(matchType);
        
        enterTextWLabel("Search pattern:", searchPattern);
        
        enterTextWLabel("Line offset:", String.valueOf(lineOffset));
        enterTextWLabel("Num. steps:", String.valueOf(numSteps));

        m_bot.sleep(1500);
        pressKey(SWT.TAB);
        m_bot.sleep(500);
        
        m_bot.button("OK").click();
    }
    

    public void setStubTestPointLogging(String before, String after) {

        enterTextWLabel("Before assignments:", before);
        enterTextWLabel("After assignments:", after);
    }

    
    public void setTestCaseLogging(String before, String after) throws Exception {
        
        selectTestSection(SEC_META);
        
        enterTextWLabel("Log before:", before);
        enterTextWLabel("Log after:", after);
    }

    
    /**
     * 
     * @param section 'Coverage' or 'Profiler' or 'Trace'
     * @param runMode one of radio buttons labels
     * @param fileName
     * @param openMode
     * @param saveAfterTest
     * @param closeAfterTest
     * @param exportFmt
     * @param exportFile
     * @param decisionCvrg
     * @param bitmapFile
     * @throws Exception
    private void setSectionAnalyzerCommon(String section,
    		                        String runMode, String fileName, 
                                    String openMode, String isSlowRun, 
                                    String saveAfterTest, String closeAfterTest,  
                                    String exportFmt, String exportFile) throws Exception {

        selectTestSection(section);

        clickRadioWLabel(runMode);
        enterTextWLabel("Document file:", fileName);
        clickRadioWLabel(openMode);
        clickRadioWLabel(isSlowRun, 0);
        clickRadioWLabel(saveAfterTest, 1);
        clickRadioWLabel(closeAfterTest, 2);
    }
     */

    public void activateAnalyzer() throws Exception {
        selectTestSection("Analyzer");
        
        clickRadioWLabel("Start");
    }

    
    void setSectionAnalyzer(String runMode, 
                            String fileName, 
                            String openMode,
                            String isUsePredefTrigger,
                            String isSlowRun, 
                            String saveAfterTest, String closeAfterTest,
                            String triggerName, 
                            boolean isChangeInheritState) throws Exception {

        selectTestSection("Analyzer");
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }

        clickRadioWLabel(runMode);
        enterTextWLabel("Document file:", fileName);
        clickRadioWLabel(openMode);
        enterTextWLabel("Trigger name:", triggerName);
        clickRadioWLabel(isUsePredefTrigger, 0);
        clickRadioWLabel(isSlowRun, 1);
        clickRadioWLabel(saveAfterTest, 2);
        clickRadioWLabel(closeAfterTest, 3);
    }

    
    void setSectionAnalyzerSaveAfterTest(boolean isSave) throws Exception {
        selectTestSection("Analyzer");
        clickRadioWLabel(isSave ? "Yes" : RB_NO, 2);
    }
    
    
    void setSectionTrace(String isActive,  
    				     String exportFmt, String exportFile) throws Exception {

        selectSubSection("Analyzer", "Trace");
        
        clickRadioWLabel(isActive);
        enterComboWLabel("Export format:", exportFmt);
        enterTextWLabel("Export file:", exportFile);
    }

    
    public void activateCoverage() throws Exception {
        selectSubSection("Analyzer", "Coverage");

        clickRadioWLabel("Yes", 0);
    }

    
    void setSectionCoverageTestCase(String isActive, 
                            String exportFmt, String exportFile,
                            String variant,
                            String isIgnoreUnreachableCode,
                            String isAssemblerInfo, String isModuleLines,
                            String isFunctionLines, String isSources,
                            String isAsm, String isRanges,
                            String isLaunchViewer, String modulesFilter,
                            String functionsFilter) throws Exception {

        selectSubSection("Analyzer", "Coverage");
        final int rbIdx = 1;
        setSectionCoverage(isActive, 
                           exportFmt, exportFile, 
                           variant,
                           rbIdx,
                           isIgnoreUnreachableCode, 
                           isAssemblerInfo, isModuleLines, 
                           isFunctionLines, isSources, 
                           isAsm, isRanges, 
                           isLaunchViewer, 
                           modulesFilter, functionsFilter);
    }
    
    
    void setSectionCoverageConfigGroup(String isActive, 
                                    String exportFmt, String exportFile,
                                    String variant,
                                    String isIgnoreUnreachableCode,
                                    String isAssemblerInfo, String isModuleLines,
                                    String isFunctionLines, String isSources,
                                    String isAsm, String isRanges,
                                    String isLaunchViewer, String modulesFilter,
                                    String functionsFilter) throws Exception {
        
        selectTestSection(SEC_GRP_COVERAGE_CONFIG);
        final int rbIdx = 2; // to skip the 'Close' radio buttons.
        setSectionCoverage(isActive, 
                           exportFmt, exportFile, 
                           variant,
                           rbIdx,
                           isIgnoreUnreachableCode, 
                           isAssemblerInfo, isModuleLines, 
                           isFunctionLines, isSources, 
                           isAsm, isRanges, 
                           isLaunchViewer, 
                           modulesFilter, functionsFilter);
    }

    
    private void setSectionCoverage(String isActive, 
                            String exportFmt, String exportFile,
                            String variant,
                            int rbIdx,
                            String isIgnoreUnreachableCode,
                            String isAssemblerInfo, String isModuleLines,
                            String isFunctionLines, String isSources,
                            String isAsm, String isRanges,
                            String isLaunchViewer, String modulesFilter,
                            String functionsFilter) throws Exception {
    
        clickRadioWLabel(isActive, 0);

        enterComboWLabel("Export format:", exportFmt);
        enterTextWLabel("Export file:", exportFile);
        enterTextWLabel("Variant:", variant);
        
        clickRadioWLabel(isIgnoreUnreachableCode, rbIdx++);
        clickRadioWLabel(isAssemblerInfo, rbIdx++);
        clickRadioWLabel(isModuleLines, rbIdx++);
        clickRadioWLabel(isFunctionLines, rbIdx++);
        clickRadioWLabel(isSources, rbIdx++);
        clickRadioWLabel(isAsm, rbIdx++);
        clickRadioWLabel(isRanges, rbIdx++);
        clickRadioWLabel(isLaunchViewer, rbIdx++);

        enterTextWLabel("Modules filter:", modulesFilter);
        enterTextWLabel("Functions filter:", functionsFilter);
    }
    
    
    void setSectionCoverageMerge(String mergeScope) throws Exception {

        selectSubSection("Analyzer", "Coverage");
        clickRadioWLabel(mergeScope);
    }

    
    void setSectionCoverageStatistics(String funcName,
                               int statementCvrg,
                               int sourceCvrg,
                               int branchesExec,
                               int branchesTaken,
                               int branchesNotTaken,
                               int branchesBoth) {

        selectSubSubSection(SEC_ANALYZER, SEC_COVERAGE, SEC_COVERAGE_STATS);

        m_ktableUtils.addLineToListTable("Covered functions", funcName);
        
        setCoverageStatistics(0, 
                              statementCvrg, sourceCvrg, branchesExec, 
                              branchesTaken, branchesNotTaken, branchesBoth);
    }

    
    protected void setCoverageStatistics(int controlIdx,
                                       int statementCvrg,
                                       int sourceCvrg,
                                       int branchesExec,
                                       int branchesTaken,
                                       int branchesNotTaken,
                                       int branchesBoth) {
        
        enterTextWLabel("Object code (bytes):", controlIdx, String.valueOf(statementCvrg));
        enterTextWLabel("Source code (lines):", controlIdx, String.valueOf(sourceCvrg));
        enterTextWLabel("Condition any:", controlIdx, String.valueOf(branchesExec));
        enterTextWLabel("Cond. true only:", controlIdx, String.valueOf(branchesTaken));
        enterTextWLabel("Cond. false only:", controlIdx, String.valueOf(branchesNotTaken));
        enterTextWLabel("Condition both:", controlIdx, String.valueOf(branchesBoth));
    }

    
    public void setSectionCoverageStatsMeasureAll(boolean isSelection) {
        selectSubSubSection("Analyzer", "Coverage", "Statistics");

        selectCheckBoxWText("Measure all functions", isSelection);
    }
    
    
    void setSectionProfiler(String isActive,  
                            String exportFmt, String exportFile,
                            String isSaveTimeline, 
                            String isProfileAux,
                            String isExportActOnly) throws Exception {

        selectSubSection(SEC_ANALYZER, SEC_PROFILER);

        clickRadioWLabel(isActive, 0);
        clickRadioWLabel(isProfileAux , 1);

        enterComboWLabel("Export format:", exportFmt);
        enterTextWLabel("Export file:", exportFile);

        clickRadioWLabel(isSaveTimeline, 2);
        clickRadioWLabel(isExportActOnly, 3);
    }


    void setSectionProfilerArea(boolean isCodeArea, String functionOrVar, 
                         String netLow[], String netHigh[],
                         String grossLow[], String grossHigh[],
                         String callOrInactiveLow[], String callOrInactiveHigh[],
                         String periodLow[], String periodHigh[],
                         String hitsLow, String hitsUp) throws Exception
    {
        if (isCodeArea) {
            selectSubSubSection(SEC_ANALYZER, SEC_PROFILER, SEC_PROFILER_CODE);
        } else {
            selectSubSubSection(SEC_ANALYZER, SEC_PROFILER, SEC_PROFILER_DATA);
        }
        
        if (isCodeArea) {
            m_ktableUtils.addLineToListTable("Functions", functionOrVar);
        } else {
            m_ktableUtils.addLineToListTable("Variables", functionOrVar);
        }
        
        KTableTestUtils m_ktableUtils = new KTableTestUtils(m_bot, this);
        m_ktableUtils.selectKTable(SWTBotConstants.PROFILER_STATS_KTABLE);
        
        setProfilerStatsTableRow(0, netLow, m_ktableUtils);
        setProfilerStatsTableRow(2, netHigh, m_ktableUtils);
        if (isCodeArea) {
            setProfilerStatsTableRow(3, grossLow, m_ktableUtils);
            setProfilerStatsTableRow(5, grossHigh, m_ktableUtils);
            setProfilerStatsTableRow(6, callOrInactiveLow, m_ktableUtils);
            setProfilerStatsTableRow(8, callOrInactiveHigh, m_ktableUtils);
            setProfilerStatsTableRow(9, periodLow, m_ktableUtils);
            setProfilerStatsTableRow(11, periodHigh, m_ktableUtils);
        } else {
            // gross times have no meaning in data profiler
            setProfilerStatsTableRow(3, callOrInactiveLow, m_ktableUtils);
            setProfilerStatsTableRow(5, callOrInactiveHigh, m_ktableUtils);
            setProfilerStatsTableRow(6, periodLow, m_ktableUtils);
            setProfilerStatsTableRow(8, periodHigh, m_ktableUtils);
        }

        
        enterTextWLabel("Lower limit:", hitsLow);
        enterTextWLabel("Upper limit:", hitsUp); 
    }


    private void setProfilerStatsTableRow(int row, String[] times,
                                          KTableTestUtils m_ktableUtils) {
        
        if (times == null) {
            return;
        }
        
        int col = 0;
        // the first cell in row gets scrolled to visible area
        pressKey(SWT.HOME);
        
        for (String time : times) {
            if (!time.isEmpty()) {
                // m_ktableUtils.selectCell(col, row);
                m_ktableUtils.setDataCell(col, row, time);
            }
            col++;
        }
    }
    

    public void setProfilerStatsTableComment(int col, int row,
                                             String nlComment, String eolComment,
                                             KTableTestUtils ktableUtils) {

        // the cell gets scrolled to visible area
        pressKey(SWT.HOME);
        ktableUtils.selectCell(col, row);
        
        ktableUtils.setComment(col, row, nlComment, eolComment);
    }
    

    void setSectionHIL(int row, String path, String value,
                       boolean isChangeInheritState) throws Exception {
        selectTestSection("HIL");
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }
        
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_HIL_TABLE);
        m_ktableUtils.setDataCell(0, row, path);
        m_ktableUtils.setDataCell(1, row, value);
    }
    
    
    void setSectionScripts(String initTargetFunc, String initTargetFuncParams,
                           String initFunc, String initFuncParams,
                           String endFunc, String endFuncParams,
                           String restoreTargetFunc, String restoreTargetFuncParams, 
                           boolean isChangeInheritState) throws Exception {
        selectTestSection("Scripts");
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
            fromDefaultToNotInherit(1);
            fromDefaultToNotInherit(2);
            fromDefaultToNotInherit(3);
        }
        
        setTextFieldForDerivedWId("initTargetFuncName", initTargetFunc, "Parameters:");
        setTextFieldForDerivedWId("initTargetFuncParams", initTargetFuncParams, "Script function:");
        
        setTextFieldForDerivedWId("initFuncName", initFunc, "Parameters:");
        setTextFieldForDerivedWId("initFuncParams", initFuncParams, "Script function:");
        
        setTextFieldForDerivedWId("endFuncName", endFunc, "Parameters:");
        setTextFieldForDerivedWId("endFuncParams", endFuncParams, "Script function:");
        
        setTextFieldForDerivedWId("restoreTargetFuncName", restoreTargetFunc, "Parameters:");
        setTextFieldForDerivedWId("restoreTargetFuncParams", restoreTargetFuncParams, "Script function:");
    }

    
    void setSectionOptions(int row, String path, String value, boolean isChangeInheritState) throws Exception {
        selectTestSection("Options");
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }
        
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_OPTIONS_TABLE);
        m_ktableUtils.setDataCell(0, row, path);
        m_ktableUtils.setDataCell(1, row, value);
    }

    
    public void setSectionDryRunTable(int row,
                                      String hostVar,
                                      String hotVarValue,
                                      boolean isChangeInheritState) throws Exception {
        selectTestSection("Dry run");
        
        if (isChangeInheritState) {
            fromDefaultToNotInherit(0);
        }
        
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_DRY_RUN_TABLE);
        m_ktableUtils.setDataCell(0, row, hostVar);
        m_ktableUtils.setDataCell(1, row, hotVarValue);
    }


    public void setSectionDryRun(ETristate isUpdateCvrg,
                                 ETristate isUpdateProfiler,
                                 String profilerStatMultiplier,
                                 String profilerStatOffset) throws Exception {
        selectTestSection("Dry run");
        
        selectTristateCheckBoxWText("Update required coverage statistics during dry run. (Default - no update)",
                                    isUpdateCvrg);
        selectTristateCheckBoxWText("Update required profiler statistics during dry run. (Default - no update)", 
                                    isUpdateProfiler);
        enterTextWLabel("Profiler statistic multiplier:", profilerStatMultiplier);
        enterTextWLabel("Profiler statistic offset:", profilerStatOffset);
    }
    

    public void setSectionDiagrams(int dataRow, ETristate isActive, EDiagType diagType, String script,
                                   String params, String outFile, ETristate isAddToReport, 
                                   EViewerType viewer, EViewFormat dataFormat, 
                                   String externalViewer) throws Exception {
        
        selectTestSection("Diagrams");
        
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_DIAGRAMS_KTABLE);
        m_ktableUtils.setTristateCheckBox(0, dataRow, isActive);
        switch (diagType) {
        case ERuntimeCallGraph:
            m_ktableUtils.selectDataItemInCombo(1, dataRow, "c");
            break;
        case ECustom:
            m_ktableUtils.selectDataItemInCombo(1, dataRow, "cc");
            break;
        case ECustomAsync:
            m_ktableUtils.selectDataItemInCombo(1, dataRow, "ccc");
            break;
        case EFlowChart:
            m_ktableUtils.selectDataItemInCombo(1, dataRow, "f");
            break;
        case ESequenceDiagram:
            m_ktableUtils.selectDataItemInCombo(1, dataRow, "s");
            break;
        default:
            break;
        
        }
        
        m_ktableUtils.setDataCell(2, dataRow, script);
        m_ktableUtils.setDataCell(3, dataRow, params);
        m_ktableUtils.setDataCell(4, dataRow, outFile);
        m_ktableUtils.setTristateCheckBox(5, dataRow, isAddToReport);
        pressKey(SWT.END); // scroll to the end of diagrams table
        
        switch (viewer) {
        case EExternal:
            m_ktableUtils.selectDataItemInCombo(6, dataRow, "e");
            break;
        case EMultipage:
            m_ktableUtils.selectDataItemInCombo(6, dataRow, "m");
            break;
        case ENone:
            m_ktableUtils.selectDataItemInCombo(6, dataRow, "n");
            break;
        case ESinglePage:
            m_ktableUtils.selectDataItemInCombo(6, dataRow, "s");
            break;
        }
        
        switch (dataFormat) {
        case EBitmap:
            m_ktableUtils.selectDataItemInCombo(7, dataRow, "bb");
            break;
        case EByExtension:
            m_ktableUtils.selectDataItemInCombo(7, dataRow, "b");
            break;
        case ESVG:
            m_ktableUtils.selectDataItemInCombo(7, dataRow, "s");
            break;
        default:
            break;
        
        }
        
        m_ktableUtils.setDataCell(8, dataRow, externalViewer);
    }

    
    void enterTextWID(String id, String value) {
        if (value != null) {
            m_bot.textWithId(id).setFocus();
            m_bot.textWithId(id).setText(value);
        }
    }


    void enterTextWLabel(String label, String value) {
        if (value != null) {
            SWTBotText text = m_bot.textWithLabel(label);
            text.setFocus();
            text.setText(value);
        }
    }


    void enterTextWLabel(String label, int textIndex, String value) {
        if (value != null) {
            m_bot.textWithLabel(label, textIndex).setFocus();
            m_bot.textWithLabel(label, textIndex).setText(value);
        }
    }


    void enterTextWLabel(String group, String label, String value) {
        if (value != null) {
            m_bot.textWithLabelInGroup(label, group).setFocus();
            m_bot.textWithLabelInGroup(label, group).setText(value);
        }
    }


    void enterStyledTextWLabel(String label, String value) {
        if (value != null) {
            SWTBotStyledText text = m_bot.styledTextWithLabel(label);
            text.setFocus();
            text.setText(value);
        }
    }


    void enterComboWLabel(String label, String value) {
        if (value != null) {
        m_bot.comboBoxWithLabel(label).setFocus();
            m_bot.comboBoxWithLabel(label).setText(value);
        }
    }


    void clickRadio(SWTBotRadio swtBotRadio) {
        swtBotRadio.click();

        // this is a workaround for events missing in SWTBotRadio.click()
        sendFocusEvent(swtBotRadio); // for focus event
        swtBotRadio.pressShortcut(0, SWT.SPACE); // for selection event
    }
    
    
    void clickRadioWLabel(final String label) {
        if (label != null) {
            SWTBotRadio swtBotRadio = m_bot.radio(label);
            clickRadio(swtBotRadio);
        }
    }

    
    void clickRadioWLabel(String label, int index) {
        if (label != null) {
            SWTBotRadio swtBotRadio = m_bot.radio(label, index);
            swtBotRadio.click();
            // this is a workaround for event missing in SWTBotRadio.click()
            sendFocusEvent(swtBotRadio);
        }
    }


    public void clickRadioWLabelIfEnabled(String label) {
        if (label != null) {
            SWTBotRadio rb = m_bot.radio(label);
            if (rb.isEnabled()) {
                rb.click();
                // this is a workaround for event missing in SWTBotRadio.click()
                sendFocusEvent(rb);
            }
        }
    }

    
    private void sendFocusEvent(final SWTBotRadio radio) {
        
        // this is a workaround for event missing in SWTBotRadio.click()
        /* UIThreadRunnable.syncExec(new VoidResult() {

            public void run() {
                Event event = createEvent(SWT.FocusIn, radio.widget, radio.display);
                radio.widget.notifyListeners(SWT.FocusIn, event);
            }
        }
        ); */
        radio.setFocus();
        
        m_bot.sleep(250);
    }


   /* private Event createEvent(int eventType, Widget widget, Display display) {
        Event event = new Event();
        event.type = eventType;
        event.time = (int) System.currentTimeMillis();
        event.widget = widget;
        event.display = display;
        return event;
    } */
    
    
    void setPersistentVarsInheritance() throws Exception {
        fromDefaultToNotInherit(0);
    }
    
    /**
     * 
     * @param type variable type, must not be null
     * @param varName variable type, must not be null
     * @param value variable init value, if null it is not set
     * @throws Exception
     */
    void setPersistentVar(int dataRow, String varName, String type) throws Exception {
        selectTestSection(SEC_PERSIST_VARIABLES);
        
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_PERSIST_VARS_TABLE);
        m_ktableUtils.setDataCell(0, dataRow, varName);
        m_ktableUtils.setDataCell(1, dataRow, type);
    }

    
    void setDeletedPersistVar(int rowNumber, String deletedVarName) throws Exception {
        selectTestSection(SEC_PERSIST_VARIABLES);
        m_ktableUtils.selectKTable(SWTBotConstants.BOT_DEL_PERSIST_VARS_TABLE);
        m_ktableUtils.setDataCell(0, rowNumber, deletedVarName);
    }
    
    
    void setIsDeleteAllPersistVars(boolean isDeleteAllVars) throws Exception {

        selectTestSection(SEC_PERSIST_VARIABLES);
        
        SWTBotCheckBox checkBox = m_bot.checkBox("Delete all persistent variables");
        if (checkBox.isChecked() != isDeleteAllVars) {
            checkBox.click();
        }
    }

    
    /**
     * 
     * @param type variable type, must not be null
     * @param name variable type, must not be null
     * @param value variable init value, if null it is not set
     * @throws Exception
     */
    void setVar(int rowNumber, String type, String name, String value) throws Exception {
        selectTestSection(SEC_VARIABLES);
        
        if (type != null  &&  !type.isEmpty()) {
            m_ktableUtils.selectKTable(SWTBotConstants.VARS_DECL_KTABLE);
            m_ktableUtils.setDataCell(0, rowNumber, name);
            m_ktableUtils.setDataCell(1, rowNumber, type);
        }
        
        m_ktableUtils.selectKTable(SWTBotConstants.VARS_INIT_KTABLE);
        m_ktableUtils.setDataCell(0, rowNumber, name);
        
        if (value != null) {
            m_ktableUtils.setDataCell(1, rowNumber, value);
        }
        
//        SWTBotTable table = m_bot.table(1);
//
//        table.doubleClick(rowNumber, 0);
//        m_bot.sleep(100);  // otherwise the first letter is missed sometimes        
//        m_keyboard.typeText(type);
//        pressKey(SWT.ESC); // if content assist appears, ESC cancels it
//        pressKey(SWT.CR);
//
//        table.doubleClick(rowNumber, 1);
//        m_bot.sleep(100);  // otherwise the first letter is missed sometimes        
//        m_keyboard.typeText(name);
//        pressKey(SWT.CR);
//
//        if (value != null) {
//            table.doubleClick(rowNumber, 2);
//        m_bot.sleep(100);  // otherwise the first letter is missed sometimes        
//            m_keyboard.typeText(value);
//            pressKey(SWT.CR);
//        }
    }


    void setVarDeclaration(int rowNumber, String varName, String type) throws Exception {
        m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        m_bot.tree(SECTION_TREE_IDX).select("Variables");
        
        m_ktableUtils.selectKTable(SWTBotConstants.VARS_DECL_KTABLE);
        m_ktableUtils.setDataCell(0, rowNumber, varName);
        m_ktableUtils.setDataCell(1, rowNumber, type);
        
//        SWTBotTable table = m_bot.table(1);
//
//        table.doubleClick(rowNumber, 1);
//        m_keyboard.typeText(varName);
//        pressKey(SWT.CR);
//
//        table.doubleClick(rowNumber, 0);
//        m_keyboard.typeText(type);
//        pressKey(SWT.CR);

    }


    void setVarInit(int dataRowNumber, String varName, String value, int ... inheritIndices) throws Exception {
        // m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        m_bot.tree(SECTION_TREE_IDX).select(SEC_VARIABLES);
        
        for (int i : inheritIndices) {
            fromDefaultToNotInherit(i);
        }

        m_ktableUtils.selectKTable(SWTBotConstants.VARS_INIT_KTABLE);
        m_ktableUtils.addRows(1);
        m_ktableUtils.setDataCell(0, dataRowNumber, varName);
        m_ktableUtils.setDataCell(1, dataRowNumber, value);
        
//        SWTBotTable table = m_bot.table(1);
//
//        table.doubleClick(rowNumber, 1);
//        m_keyboard.typeText(" "); pressKey(SWT.BS); // otherwise the first char is lost sometimes
//        m_keyboard.typeText(varName);
//        pressKey(SWT.CR);
//
//        table.doubleClick(rowNumber, 2);
//        m_keyboard.typeText(" "); pressKey(SWT.BS); // otherwise the first char is lost sometimes
//        m_keyboard.typeText(value);
//
//        pressKey(SWT.CR);
    }


    void setVarType(int rowNumber, String type) throws Exception {
        
        m_bot.tree(SECTION_TREE_IDX).select("Variables");
        
        m_ktableUtils.selectKTable(SWTBotConstants.VARS_DECL_KTABLE);
        m_ktableUtils.setDataCell(1, rowNumber, type);
        
//        SWTBotTable table = m_bot.table(1);
//        table.doubleClick(rowNumber, 0);
//        m_keyboard.typeText(type);
//        pressKey(SWT.CR);
    }


    void setVarValue(int rowNumber, String value) throws Exception {

        m_bot.tree(SECTION_TREE_IDX).select("Variables");
        
        m_ktableUtils.selectKTable(SWTBotConstants.VARS_INIT_KTABLE);
        m_ktableUtils.setDataCell(1, rowNumber, value);
        
//        SWTBotTable table = m_bot.table(1);
//        table.doubleClick(rowNumber, 2);
//        m_keyboard.typeText(value);
//        pressKey(SWT.CR);
    }


    void selectRowInExpectedTable(int rowNo) {
        // m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        m_bot.tree(SECTION_TREE_IDX).select(SEC_EXPECTED);

        m_ktableUtils.selectKTable(SWTBotConstants.BOT_EXPECTED_EXPR_TABLE);
        m_ktableUtils.selectCell(1, rowNo + 1);
        
        //SWTBotTable table = m_bot.tableWithId(SWTBotConstants.BOT_EXPECTED_EXPR_TABLE);
        //table.click(rowNo, 0);
    }


    void deleteRowInExpectedTable(int rowNo) {
        m_bot.tree(SECTION_TREE_IDX).select(SEC_EXPECTED);

        m_ktableUtils.selectKTable(SWTBotConstants.BOT_EXPECTED_EXPR_TABLE);
        m_ktableUtils.deleteRowWClick(0, rowNo + 1);
    }
    
    
    String getStatusViewText() {
        SWTBotStyledText botText = m_bot.styledTextWithId(SWTBotConstants.BOT_STATUS_TEXT);
        return botText.getText();
    }


    /**
     * 
     * @param row row in table pane on the left to display text in description 
     *            pane on the right. 
     * @param expectedText
     */
    void checkStatusView(int row, String expectedText) {
        m_bot.viewByTitle(TEST_STATUS_VIEW_TITLE).bot().table().click(row, 0);
        assertEquals(expectedText, getStatusViewText().substring(0, expectedText.length()));
    }

    
    void createBaseTestWContextMenu(String funcName, String params, 
                                    String expr, String retValName,
                                    boolean isCreateId) {
        
        createBaseTestWContextMenu(funcName, params, expr, retValName, 
                                   isCreateId, "");
    }


    void createBaseTestWContextMenu(String funcName, String params, 
                                    String expr, String retValName,
                                    boolean isCreateId,
                                    String coreId) {
        openNewTestWizard(false);
        fillDialogNewTestPage1(false, coreId, funcName, params, expr, retValName, 
                               isCreateId, false, true);
    }


    void clickTestTreeContextMenu(String componentId, 
                                  String treeItemText, 
                                  String menuText) {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(componentId);
        SWTBotTreeItem treeItem = tree.getTreeItem(treeItemText);
        SWTBotMenu contextMenu = treeItem.contextMenu(menuText);
        contextMenu.click();
    }


    void clickTestTreeContextMenu(String componentId, 
                                  String parentOfParentTreeItemText,
                                  String parentTreeItemText,
                                  String menuText) {
        SWTBotView view = m_bot.viewByTitle(TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(componentId);
        SWTBotTreeItem item = tree.getTreeItem(parentOfParentTreeItemText);
        item.getNode(parentTreeItemText).contextMenu(menuText).click();
    }


    void clickTestEditorContextMenu(String componentId, 
                                    String treeItemText, 
                                    String menuText) {
        SWTBotEditor editor = m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        editor.setFocus();
        SWTBotTree tree = m_bot.treeWithId(componentId);
        tree.select(treeItemText).contextMenu(menuText).click();
    }


    void clickTestEditorContextMenu(String componentId, 
                                    String treeItemText, 
                                    String treeSubItemText, 
                                    String menuText) {
        SWTBotEditor editor = m_bot.editorByTitle(DEFAULT_TEST_IYAML_FILE_NAME);
        editor.setFocus();
        SWTBotTree tree = m_bot.treeWithId(componentId);
        tree.expandNode(treeItemText, treeSubItemText).contextMenu(menuText).click();
    }


    void toolbarInitTarget() {
        clickToolbarButton("Init Target.*");
        waitForProgressDialog();
        /* ShellCondition condition = new ShellCondition("Progress Information", true);
      condition.init(m_bot);
      System.out.println("waiting for progress monitor");
      m_bot.waitUntil(condition);
      System.out.println("waiting while progress monitor");
      m_bot.waitWhile(condition); */
    }


    void toolbarRunAllTests() {
        clickToolbarButton("Run All.*");
    }

    void toolbarRunSelectedTest() {
        clickToolbarButton("Run Selected Tests.*");
    }

    void toolbarRunSelectedAndDerived() {
        clickToolbarButton("Run Selected And Derived.*");
    }

    void toolbarToggleDryRun() {
        // m_bot.toolbarToggleButtonWithTooltip(tooltip);
        getToggleButton("Dry run mode.*").click();
        // clickToolbarButton();
    }

    void clickToolbarButton(String tooltipRegEx) {
        getToolbarButton(tooltipRegEx).click();
    }


    SWTBotToolbarPushButton getToolbarButton(String tooltipRegEx) {
        @SuppressWarnings("unchecked")
        Matcher<Widget> matcher = 
        WidgetMatcherFactory.allOf(WidgetMatcherFactory.widgetOfType(Widget.class), 
                                   new RegExWidgetMatcher<Widget>(tooltipRegEx, ETextType.GET_TOOLTIP), 
                                   WidgetMatcherFactory.withStyle(SWT.PUSH, "SWT.PUSH"));
        return new SWTBotToolbarPushButton((ToolItem)m_bot.widget(matcher, 0), matcher);
    }


    SWTBotToolbarToggleButton getToggleButton(String tooltipRegEx) {
        @SuppressWarnings("unchecked")
        Matcher<Widget> matcher = 
        WidgetMatcherFactory.allOf(WidgetMatcherFactory.widgetOfType(Widget.class), 
                                   new RegExWidgetMatcher<Widget>(tooltipRegEx, ETextType.GET_TOOLTIP), 
                                   WidgetMatcherFactory.withStyle(SWT.CHECK, 
                                                                  "SWT.CHECK"));
        return new SWTBotToolbarToggleButton((ToolItem)m_bot.widget(matcher, 0), matcher);
    }


    // WARNING: Works only if native tooltip is set. Does not work for
    // tooltip set with UITools!!! (getToolTipText() returns null) Not tested yet!
    SWTBotButton getButtonWTooltipRexEx(String tooltipRegEx) {
        @SuppressWarnings("unchecked")
        Matcher<Widget> matcher = 
        WidgetMatcherFactory.allOf(WidgetMatcherFactory.widgetOfType(Widget.class), 
                                   new RegExWidgetMatcher<Widget>(tooltipRegEx, 
                                                                  ETextType.GET_TOOLTIP));

        return new SWTBotButton((Button)m_bot.widget(matcher, 0), matcher);
    }


    void copyTestSpecToClipboard(String testSpec) {
        
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    // clipboard must be accessed from UI thread
                    Clipboard cb = new Clipboard(Display.getDefault());
                    TextTransfer transfer = TextTransfer.getInstance();
                    cb.setContents(new Object[]{testSpec}, 
                                   new Transfer[]{transfer});
                    cb.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
    }
    
    
    public void pasteClipboardToOverview() {
        SWTBotView view = m_bot.viewByTitle(UITestUtils.TEST_TREE_CAPTION);
        view.setFocus();
        SWTBotTree tree = m_bot.treeWithId(SWTBotConstants.BOT_TEST_TREE_ID);
        tree.contextMenu("Paste").click();
    }

    
    CTestSpecification getTestSpecFromClipboard() {
        
        m_bot.sleep(250);
        
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    // clipboard must be accessed from UI thread
                    Clipboard cb = new Clipboard(Display.getDefault());
                    TextTransfer transfer = TextTransfer.getInstance();
                    m_yamlSpec = (String)cb.getContents(transfer);
                    cb.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // if multiple test specs were cut, add them all to single container test spec.
        // The caller must know what to expect.
        if (m_yamlSpec.charAt(0) == '-') {
            m_yamlSpec = "tests:\n" + m_yamlSpec;
        }
        CTestSpecification testSpec = CTestSpecification.parseTestSpec(m_yamlSpec);

        return testSpec;
    }


    CTestGroup getTestGroupFromClipboard() {
        
        m_bot.sleep(250);
        
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    // clipboard must be accessed from UI thread
                    Clipboard cb = new Clipboard(Display.getDefault());
                    TextTransfer transfer = TextTransfer.getInstance();
                    m_yamlSpec = (String)cb.getContents(transfer);
                    cb.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        CTestBench testBench = new CTestBench();
        CYAMLUtil.parseTestBase(m_yamlSpec, testBench);

        return testBench.getGroup(false);
    }


    void exitApplication(boolean isWaitForSaveDialog) {
        m_bot.menu(MENU_FILE).click().menu("Exit").click();

        if (isWaitForSaveDialog) {
            ShellCondition condition = new ShellCondition("Save resources", true);
            condition.init(m_bot);
            System.out.println("waiting for save dialog");
            m_bot.waitUntil(condition);
            System.out.println("save dialog found");
            // m_bot.widget(new MyMatcher<Button>());

            SWTBotButton botButton = m_bot.button("Discard");
            SimpleBotButton simpleButton = new SimpleBotButton(botButton.widget);
            simpleButton.click();
            System.out.println("button pressed");
        }
    }

    
    public boolean waitForProgressDialog() {
        return waitForDialogAndWait("Progress Information", 3000);
    }
    
    

    /**
     * This method waits for the shell with the given title to appear, and waits 
     * wile it is present. It is useful for waiting until the progress bar 
     * disappears. If the progress bar dialog does not appear in the timeout specified,
     * function also returns, because progress bar dlg does not appear for short
     * operations.
     * 
     * @param shellTitle
     * @return true if the shell appeared, false if ended because of the timeout.
     */
    public boolean waitForDialogAndWait(String shellTitle, int timeout) {
        // alternative condition:
        // ShellCondition condition = new ShellCondition(shellTitle, true);

        Matcher<Shell> matcher = withText(shellTitle);
        WaitForObjectCondition<Shell> condition = Conditions.waitForShell(matcher);
        condition.init(m_bot);
       
        if (waitUntil(condition, timeout, false)) {
            m_bot.waitWhile(condition, 200_000); // tests may take more time than the default timeout
        }
        
       /*// this works for dialogs but not for ProgressMonitor - seems to be Wrong Thread?
        if (waitUntil(condition, timeout, false)) {
            // if the dialog appeared, wait until it disappears
            m_bot.waitWhile(condition);
        } */
        return true;
    }


    public void waitWhileShellIsOpened(String shellTitle, int timeout) {
        Matcher<Shell> matcher = withText(shellTitle);
        WaitForObjectCondition<Shell> condition = Conditions.waitForShell(matcher);
        condition.init(m_bot);
       
        m_bot.waitWhile(condition, timeout); 
    }
    
    /** 
     * Waits until shell with the specified title appears.
     * 
     * @param shellTitle the shell title
     * @param timeout im milliseconds
     * @param isThrow if false, the method returns true if condition is met, 
     *                false otherwise.
     *                if true,  the method returns true if condition is met, 
     *                throws exception otherwise.
     * @return true if shell appears, false otherwise
     * @throws TimeoutException
     */
    public boolean waitForShell(String shellTitle, int timeout, boolean isThrow) {

        Matcher<Shell> matcher = withText(shellTitle);
        WaitForObjectCondition<Shell> condition = Conditions.waitForShell(matcher);

        return waitUntil(condition, timeout, isThrow);
    }


    public boolean waitForShellAndClickOK(String shellTitle, int timeout, boolean isThrow) {
        if (waitForShell(shellTitle, timeout, isThrow) == false) {
            return false;
        }
        
        SWTBotShell shellbot = m_bot.shell(shellTitle);
        shellbot.activate();
        m_bot.button("OK").click();
        return true;
    }


    /** 
     * Waits until condition is met for the specified timeout.
     * 
     * @param condition the condition
     * @param timeout im milliseconds
     * @param isThrow if false, the method returns true if condition is met, 
     *                false otherwise.
     *                if true,  the method returns true if condition is met, 
     *                throws exception otherwise.
     * @return
     * @throws TimeoutException if isThrow == true and timeout expires
     */
    public boolean waitUntil(ICondition condition, long timeout, boolean isThrow) throws TimeoutException {

        long limit = System.currentTimeMillis() + timeout;
        condition.init(m_bot);
        while (true) {
            try {
                if (condition.test()) {
                    return true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            m_bot.sleep(DEFAULT_POLL_TIMEOUT);

            if (System.currentTimeMillis() > limit) {
                if (isThrow) {
                    throw new TimeoutException("Timeout after: " + timeout + " ms.: " + condition.getFailureMessage()); //$NON-NLS-1$ //$NON-NLS-2$
                } else { 
                    return false;
                }
            }
        }
    }


    public void setAutoIdFormatWizardFields(String const0,
                                            int combo0,
                                            String const1,
                                            int combo1,
                                            String const2,
                                            int combo2,
                                            String const3,
                                            int combo3,
                                            String const4) {
        m_bot.text(0).setText(const0);
        m_bot.comboBox(0).setSelection(combo0);

        m_bot.text(1).setText(const1);
        m_bot.comboBox(1).setSelection(combo1);

        m_bot.text(2).setText(const2);
        m_bot.comboBox(2).setSelection(combo2);

        m_bot.text(3).setText(const3);
        m_bot.text(4).setText(const4);
        m_bot.comboBox(3).setSelection(combo3);

        // pressKey(SWT.DEL); // just to simulate
    }
    
    
    public void setGenerateScriptDialog(String scriptFile,
                                        boolean isPython) throws IOException, InterruptedException {
        
        enterTextWLabel("Generated script file:", scriptFile);
        if (isPython) {
            // clickRadioWLabel("Python");
            selectCheckBoxWText("Use progress monitor", true);
        } else {
            // clickRadioWLabel("Perl");
            throw new IllegalArgumentException("Perl is no longer supported!");
        }
        selectCheckBoxWText("Use custom script template:", false);
        selectCheckBoxWText("Use custom test spec. file:", false);
        selectCheckBoxWText("Use custom winIDEA worksp.:", false);
        selectCheckBoxWText("Use custom init sequence", false);
        selectCheckBoxWText("Use filter       ID:", false);
        selectCheckBoxWText("Use default monitor", true);
        selectCheckBoxWText("Save test report in testIDEA format", true);
        selectCheckBoxWText("Save test report in JUnit format (for Jenkins)", true);
        selectCheckBoxWText("Open report in browser", false);
        selectCheckBoxWText("Use custom report configuration", false);
        
        m_bot.button("OK").click();
    }


    public void selectCheckBoxWText(String text, boolean isSelection) {
        SWTBotCheckBox cb = m_bot.checkBox(text);
        cb.setFocus(); // IMPORTANT - if focus is not taken from previous
        // control, which may have been Text control, then it does not
        // commit data to object (focus listener is not called)!
        
        if (cb.isChecked() != isSelection) {
            cb.click();
        }
    }


    public void selectTristateCheckBoxWText(String text, ETristate state) {
        SWTBotTristateCheckBox cb = getTristateCheckBox(text, 0);
        cb.setFocus(); // IMPORTANT - if focus is not taken from previous
        // control, which may have been Text control, then it does not
        // commit data to object (focus listener is not called)!
        
        switch (state) {
        case E_DEFAULT:
            cb.setGrayed();
            break;
        case E_FALSE:
            cb.deselect();
            break;
        case E_TRUE:
            cb.select();
            break;
        default:
            throw new IllegalStateException("Invalid state!");
        }
    }

    
    public SWTBotTristateCheckBox getTristateCheckBox(String mnemonicText, int index) {
        @SuppressWarnings("unchecked")
        Matcher<? extends Widget> matcher = 
                WidgetMatcherFactory.allOf(WidgetMatcherFactory.widgetOfType(Button.class), 
                                           WidgetMatcherFactory.withMnemonic(mnemonicText), 
                                           WidgetMatcherFactory.withStyle(SWT.CHECK, "SWT.CHECK"));
        return new SWTBotTristateCheckBox((Button) m_bot.widget(matcher, index), matcher);
    }


    public void saveReport(boolean isFullReport, String outFile, boolean isAddCustomData) {

        openDialogWithMainMenu(UITestUtils.MENU_TEST, "Save Test Report ...", "Save Test Report");

        setDialogSaveTestReport("XML", 
                                connect.getBUILT_IN_XSLT_PREFIX() + " " + connect.getDEFAULT_XSLT_NAME(),
                                connect.getBUILT_IN_XSLT_PREFIX() + " " + connect.getDEFAULT_CSS_NAME(),
                                false,
                                isFullReport,
                                outFile,
                                true, false, 
                                "ic5000", "test of XSD", isAddCustomData);
    }

    
    public void validateSchema(String schemaLocation, 
                               String outFile) {
    
        try {
            validateXMLSchema(outFile, schemaLocation);
        } catch (SAXParseException exception) {
            String msg = "Error: " + exception + '\n';
            msg += "  Line/Col: " + exception.getLineNumber() + " / " + exception.getColumnNumber();
            System.out.println(msg);
            assertTrue(msg, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(ex.getMessage(), false);
        }
        m_bot.sleep(1000);
    }
    
    
    public void setDialogSaveTestReport(String outFormat,
                                        String xslt,
                                        String css,
                                        boolean isEmbeddXsltAndCss,
                                        boolean isAllResultsInHTML,
                                        String outFile,
                                        boolean isIncludeTestSpec,
                                        boolean isOpenDefaultBrowserAfterSave,
                                        String testEnvHW,
                                        String testDesc,
                                        boolean isAddCustomData) {
        // clickRadioWLabel(reportContents);
        clickRadioWLabel(outFormat);
        enterComboWLabel("XSLT:", xslt);
        enterComboWLabel("CSS:", css);
        enterTextWLabel("Logo image URL:", "file://D:\\bb\\trunk\\Eclipse\\testidea\\si.isystem.itest.plugin.core\\icons\\itest_window_128.gif");
        enterTextWLabel("Report title:", "iSYSTEM Tests");
        selectCheckBoxWText("Embed XSLT/CSS", isEmbeddXsltAndCss);
        enterComboWLabel("HTML content:", isAllResultsInHTML ? "All results" : "Not passed results only");
        enterTextWLabel("Output file:", outFile);
        selectCheckBoxWText("Use absolute links to export files", true);
        selectCheckBoxWText("Include test specifications", isIncludeTestSpec);
        selectCheckBoxWText("Open default browser after save", isOpenDefaultBrowserAfterSave);
        
        if (isAddCustomData) {
            m_ktableUtils.selectKTable(0);
            m_ktableUtils.addRows(1);
            m_ktableUtils.setDataCell(0, 0, "Tester");
            m_ktableUtils.setDataCell(1, 0, "${_user}");

            m_ktableUtils.addRows(1);
            m_ktableUtils.setDataCell(0, 1, "Time");
            m_ktableUtils.setDataCell(1, 1, "${_isoTime}");

            m_ktableUtils.addRows(1);
            m_ktableUtils.setDataCell(0, 2, "Date");
            m_ktableUtils.setDataCell(1, 2, "${_date}");

            m_ktableUtils.addRows(1);
            m_ktableUtils.setDataCell(0, 3, "HW version");
            m_ktableUtils.setDataCell(1, 3, testEnvHW);

            m_ktableUtils.addRows(1);
            m_ktableUtils.setDataCell(0, 4, "Description");
            m_ktableUtils.setDataCell(1, 4, testDesc);
        }
        
        m_bot.sleep(1000); // so that the user is able to quickly see settings
        m_bot.button("OK").click();
    }


    public void setExportImportDialog(String fileName,
                                      int exportTypeIdx,
                                      boolean isCreateNewTestCases,
                                      Boolean isOpenDefaultAppOrImportOnlyToSelected,
                                      boolean isExport) {
        
        SWTBotRadio swtBotRadio = m_bot.radio(exportTypeIdx);
        clickRadio(swtBotRadio);
        //m_bot.radio(exportTypeIdx).click();
        m_bot.text(exportTypeIdx).setText(fileName);
        if (isExport) {
            selectCheckBoxWText("Open with default application", 
                                isOpenDefaultAppOrImportOnlyToSelected.booleanValue());
        } else {
            if (isCreateNewTestCases) {
                clickRadioWLabel("Create new test cases");
            }
            if (isOpenDefaultAppOrImportOnlyToSelected != null  &&  isOpenDefaultAppOrImportOnlyToSelected.booleanValue()) {
                clickRadioWLabel("Import data only to selected test specifications");
            }
        }
        m_bot.sleep(1000); // so that tester is able to quickly see settings
        m_bot.button("Finish").click();
    }
    
    
    /** Validates xmlFile against xmlSchema. 
     * @throws SAXException 
     * @throws IOException */
    void validateXMLSchema(String xmlFile, String xmlSchema) throws SAXException, IOException {

        System.out.println("# Schema validation, file: '" + xmlFile + "',  schema: '" + xmlSchema + "'");
        
        // define the type of schema - we use W3C:
        String schemaLang = XMLConstants.W3C_XML_SCHEMA_NS_URI; // "http://www.w3.org/2001/XMLSchema";
        // schemaLang = XMLConstants.XML_DTD_NS_URI;

        // get validation driver:
        SchemaFactory factory = SchemaFactory.newInstance(schemaLang);

        // create schema by reading it from an XSD file:
        Schema schema = factory.newSchema(new StreamSource(xmlSchema));
        Validator validator = schema.newValidator();

        // at last perform validation:
        validator.validate(new StreamSource(xmlFile));
    }


    public void clickButton(String textOnButton) {
        m_bot.button(textOnButton).click();        
    }
    

    /**
     * IMPORTANT:
     * component's method setTooltipText(). If DefaultTooltip() class from JFace is used,
     * component is NOT found!
     * 
     * @param tooltip
     */
    public void clickButtonWTooltip(String tooltip) {
        m_bot.buttonWithTooltip(tooltip).click();        
    }
    

    /**
     * Compares the given two files. Returns null if files are equal, String[2]
     * with the first different lines from each file if files differ.
     * 
     * @param expected
     * @param actual
     * @return
     * @throws IOException
     */
    public String[] compareFiles(String expected, String actual) throws IOException {
        Path expPath = Paths.get(expected);
        BufferedReader expReader = Files.newBufferedReader(expPath, Charset.forName("UTF-8"));
        
        Path actPath = Paths.get(actual);
        BufferedReader actReader = Files.newBufferedReader(actPath, Charset.forName("UTF-8"));
        
        do {
            String expLine = expReader.readLine();
            String actLine = actReader.readLine();
            
            if (expLine == null &&  actLine == null) { // end of both files, OK
                return null;
            }

            if (expLine == null ||  actLine == null  ||  !expLine.equals(actLine)) { // premature end of one of files
                return new String[]{expLine, actLine};
            }

        } while (true);
    }


    /**
     * Checks that both files have the same lines, but order is not important.
     * @param expected
     * @param actual
     * @return
     * @throws IOException
     */
    public String[] compareUnorderedFiles(String expected, String actual) throws IOException {
        Path expPath = Paths.get(expected);
        BufferedReader expReader = Files.newBufferedReader(expPath, Charset.forName("UTF-8"));
        
        Path actPath = Paths.get(actual);
        BufferedReader actReader = Files.newBufferedReader(actPath, Charset.forName("UTF-8"));

        Set<String> expectedLines = new TreeSet<>();
        Set<String> actualLines = new TreeSet<>();
        do {
            String expLine = expReader.readLine();
            String actLine = actReader.readLine();
            
            if (expLine == null &&  actLine == null) { // end of both files, OK
                break;
            }

            if (expLine == null ||  actLine == null) { // premature end of one of files
                return new String[]{"", "Number of lines differs!"};
            }

            expectedLines.add(expLine);
            actualLines.add(actLine);
            
        } while (true);
        
        for (String eLine : expectedLines) {
            if (!actualLines.remove(eLine)) {
                return new String[]{eLine, " This line was not found in expected output! Check first if addresses changed!"};
            }
        }
        
        if (!actualLines.isEmpty()) {
            return new String[]{"More expected than actual lines!", "Num remaining lines:" + actualLines.size()};
        }
        
        return null;
    }



    public void typeText(String text) {
        m_keyboard.typeText(text);
    }



    public void fromDefaultToNotInherit(int index) {
        m_bot.checkBox("Inherit", index).click();
        m_bot.checkBox("Inherit", index).click();
    }
    
    
    protected void verifyStubAssignments(CTestBaseList steps,
                                         String[] expectedExp,
                                         String varName, 
                                         String varValue,
                                         String[] scriptParamsExp) {
        
        assertEquals(1, steps.size());
        CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(0));
        
        // expected
        CSequenceAdapter expected = step.getExpectedExpressions(true);
        assertEquals(expectedExp.length, expected.size());
        for (int i = 0; i < expectedExp.length; i++) {
            assertEquals(expectedExp[i], expected.getValue(i));
        }
        
        // assignments
        CMapAdapter assignments = step.getAssignments(true);
        StrVector keys = new StrVector();
        assignments.getKeys(keys);
        assertEquals(1, keys.size());
        assertEquals(varName, keys.get(0));
        assertEquals(varValue, assignments.getValue(keys.get(0)));

        // script params
        CSequenceAdapter scriptParams = step.getScriptParams(true);
        assertEquals(scriptParamsExp.length, scriptParams.size());
        for (int i = 0; i < scriptParamsExp.length; i++) {
            assertEquals(scriptParamsExp[i], scriptParams.getValue(i));
        }
    }
    
    
    public void loadTestSpecFromFile(final String fileName) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOpenCmdHandler.openEditor(fileName);
                    // TestSpecificationModel.getActiveModel().openTestSpec(fileName, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    
    public boolean isFileCreatedToday(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        if (Files.exists(path)) {
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            long fileDay = lastModifiedTime.to(TimeUnit.DAYS);
            FileTime currentTime = FileTime.fromMillis(System.currentTimeMillis());
            long currentDay = currentTime.to(TimeUnit.DAYS);
            if (fileDay == currentDay) {
                return true;
            }
        }
        
        return false;
    }


    public void activateShell(String dialogTitle) {
        
        final String propsShellTitle = dialogTitle;
        waitForShell(propsShellTitle, 3000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(propsShellTitle);
        shellbot.activate();
    }


    public void openExportDialog() {
        if (isRCP()) {
            openDialogWithMainMenu("File", "Export", EXPORT_DLG_TITLE);
        } else {
            openExportImportDialog(EXPORT_DLG_TITLE, "Export...", 
                                   ECLIPSE_EXPORT_DLG_TITLE);
        }
    }

    
    public void openImportDialog() {
        if (isRCP()) {
            openDialogWithMainMenu("File", "Import", IMPORT_DLG_TITLE);
        } else {
            openExportImportDialog(IMPORT_DLG_TITLE, "Import...", 
                                           ECLIPSE_IMPORT_DLG_TITLE);
        }
    }
    
    
    private void openExportImportDialog(String exportDlgTitle,
                                        String exportImportMenuOption,
                                        String eclipseExportImportDlgTitle) {
        
        // opening Export dialog in Eclipse requires few more steps ... 
        openDialogWithMainMenu("File", exportImportMenuOption, eclipseExportImportDlgTitle);
        m_bot.tree(0).select("testIDEA").expandNode("testIDEA", true).select("Test Cases");
        m_bot.button("Next >").click();
        waitForConnectionDialog();

        waitForShell(exportDlgTitle, 2000, true);
        // Activation REQUIRED, otherwise components are not found!
        SWTBotShell shellbot = m_bot.shell(exportDlgTitle);
        shellbot.activate();
    }
    
    
    /**
     * Use this method for debugging SWTBot tests, especially when you suspect 
     * problems with events. 
     * 
     * @param ctrl
     */
    public static void addListeners(final Control ctrl) {
        
        ctrl.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                System.out.println("focusLost - " + ctrl.getClass().getSimpleName());
            }
            
            
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println("focusGained - " + ctrl.getClass().getSimpleName());
            }
        });
        ctrl.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseUp(MouseEvent e) {
                System.out.println("mouseUp - " + ctrl.getClass().getSimpleName());
            }
            
            
            @Override
            public void mouseDown(MouseEvent e) {
                System.out.println("mouseDown - " + ctrl.getClass().getSimpleName());
            }
            
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                System.out.println("mouseDoubleClick - " + ctrl.getClass().getSimpleName());
            }
        });
        
        ctrl.addGestureListener(new GestureListener() {
            
            @Override
            public void gesture(GestureEvent e) {
                System.out.println("gesture - " + ctrl.getClass().getSimpleName());
            }
        });
        
        ctrl.addMouseMoveListener(new MouseMoveListener() {
            
            @Override
            public void mouseMove(MouseEvent e) {
                System.out.println("mouseMove - " + ctrl.getClass().getSimpleName());
            }
        });
        
        ctrl.addMouseTrackListener(new MouseTrackListener() {
            
            @Override
            public void mouseHover(MouseEvent e) {
                System.out.println("mouseHover - " + ctrl.getClass().getSimpleName());
            }
            
            @Override
            public void mouseExit(MouseEvent e) {
                System.out.println("mouseExit - " + ctrl.getClass().getSimpleName());
            }
            
            @Override
            public void mouseEnter(MouseEvent e) {
                System.out.println("mouseEnter - " + ctrl.getClass().getSimpleName());
            }
        });
        ctrl.addTraverseListener(new TraverseListener() {
            
            @Override
            public void keyTraversed(TraverseEvent e) {
                System.out.println("keyTraversed - " + ctrl.getClass().getSimpleName());
            }
        });
        
        ctrl.addTouchListener(new TouchListener() {
            
            @Override
            public void touch(TouchEvent e) {
                System.out.println("touch - " + ctrl.getClass().getSimpleName());
            }
        });
        
        if (ctrl instanceof Button) {
            Button button = (Button)ctrl;
            button.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    System.out.println("widgetSelected - " + ctrl.getClass().getSimpleName());
                }


                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    System.out.println("widgetDefaultSelected - " + ctrl.getClass().getSimpleName());
                }
            });
        }
        
        if (ctrl instanceof Text) {
            final Text text = (Text) ctrl;
            text.addModifyListener(new ModifyListener() {
                
                @Override
                public void modifyText(ModifyEvent e) {
                    System.out.println("modifyText - " + ctrl.getClass().getSimpleName());
                    System.out.println("  " + e.getSource());
                    System.out.println("  " + e.data);
                    System.out.println("  " + text.getText());
                }
            });
        }
    }


    public void setSectionDryRun(int rowNumber, String varName, String expr) throws Exception {
        
            selectTestSection(SEC_DRY_RUN);
            m_ktableUtils.selectKTableWithContent(1, 0, "Variables");
            m_ktableUtils.setCellContent(1, rowNumber, varName);
            m_ktableUtils.setCellContent(2, rowNumber, expr);
    }


    public void setSectionDryRunAnalyzer(ETristate isUpdateCoverage,
                                         ETristate isUpdateProfiler,
                                         String multiplier,
                                         String offset) {
        
        selectTristateCheckBoxWText("Update required coverage statistics during dry run. (Default - no update)",
                                    isUpdateCoverage);
        selectTristateCheckBoxWText("Update required profiler statistics during dry run. (Default - no update)",
                                    isUpdateProfiler);
        
     // it is not possible to set text of disabled widget
        if (isUpdateProfiler == ETristate.E_TRUE) { 
            enterTextWLabel("Profiler statistic multiplier:", multiplier);
            enterTextWLabel("Profiler statistic offset:", offset);
        }
    }
    
    
    public String translateMsg(String cmd) {
        Properties props = si.isystem.itest.main.Activator.getOSGI_I18nPropeties();
        String menutItemText = props.getProperty(cmd);
        if (menutItemText == null) {
            throw new IllegalStateException("Command not found in l10n file!");
        }
        return menutItemText;
    }


    public void createUnitTestsForListUtils() throws Exception {
        
        setPropertiesAnalDocFileName("${_testId}-${_function}.trd");
        setPropertiesAutoIdFormat("test-${_seq}");
        
        createUnitTestForListUtils("maxElement", "0", "{3, 7, 2, 9, 1, 6}", "-2147483648");
        createUnitTestForListUtils("maxElement", "6", "{3, 7, 2, 9, 1, 6}", "9");
        createUnitTestForListUtils("maxElement", "6", "{3, 7, 2, -9, 1, 6}", "7");
        createUnitTestForListUtils("maxElement", "6", "{-3, -7, -2, -9, -1, -6}", "-1");
        
        // this test should fail, actual result = 0x7fff'ffff
        createUnitTestForListUtils("minElement", "0", "{3, 7, 2, -9, 1, 6}", "-2147483647");
        
        createUnitTestForListUtils("minElement", "6", "{3, 7, 2, -9, 1, 6}", "-9");
        createUnitTestForListUtils("minElement", "6", "{-3, -7, -2, 9, -1, -6}", "-7");
        
        createUnitTestForListUtils("sumList", "0", "{3, 7, 2, 9, 1, 6}", "0");
        createUnitTestForListUtils("sumList", "6", "{3, 7, 2, -9, 1, 6}", "10");
        createUnitTestForListUtils("sumList", "6", "{-3, -7, -2, 9, -1, -6}", "-10");
        
        createUnitTestForListUtils("countGreaterThan", "6, 5", "{3, 7, 2, 9, 1, 6}", "3");
        createNewDerivedTest("test-10 : countGreaterThan", "", "", "intList, 6, 2", 
                             "_isys_rv == 4", "", true);
        createNewDerivedTest("test-10 : countGreaterThan", "", "", "intList, 6, 10", 
                             "_isys_rv == 0", "", true);
    }
    
    
    private void createUnitTestForListUtils(String funcName, String count, String array, String result) throws Exception {
        createNewBaseUnitTest(funcName, "intList, " + count, 
                              "_isys_rv ==  " + result, "", true);
        setVar(0, "int[6]", "intList", array);
        setSectionAnalyzer("Start", null, null, null, null, "Yes", RB_NO, null, false);
        setSectionCoverageTestCase("Yes", null, null, null, null, null, null, null, null, null, null, null, null, null);
        setSectionCoverageStatistics(funcName, 60, 0, 50, 0, 0, 0);
    }
    
    
    public void setGroupResultComment(String groupId, 
                                      String groupInfo,
                                      String commentText) throws Exception {
        
        selectTestGroup(groupId, groupInfo);
        selectTestSection("Meta");
        enterTextWLabel("Result comment:", commentText);
    }


    public void setGroupCoverage(String groupId, String groupInfo,
                                 int[] allCodeCvrg, int[] testedCodeCvrg) throws Exception {
        
        selectTestGroup(groupId, groupInfo);
        selectTestSection(SEC_GRP_COVERAGE_CONFIG);
        clickRadioWLabel("Yes");
        m_bot.sleep(200);
        
        setSectionCoverageResults(allCodeCvrg, testedCodeCvrg);
    }
    
    
    public void setSectionCoverageResults(int[] allCodeCvrg, int[] testedCodeCvrg) throws Exception {
        selectTestSection("Coverage results");

        setCoverageStatistics(0, allCodeCvrg[0], allCodeCvrg[1],
                              allCodeCvrg[2], allCodeCvrg[3], allCodeCvrg[4], allCodeCvrg[5]);
        
        setCoverageStatistics(6, testedCodeCvrg[0], testedCodeCvrg[1],
                              testedCodeCvrg[2], testedCodeCvrg[3], testedCodeCvrg[4], testedCodeCvrg[5]);
        
    }


    public void selectFormEditor(String iyamlFileName) {
        SWTBotMultiPageEditor editor = m_bot.multipageEditorByTitle(iyamlFileName);
        editor.activatePage("Form");
    }


    public void selectTableEditor(String iyamlFileName) {
        SWTBotMultiPageEditor editor = m_bot.multipageEditorByTitle(iyamlFileName);
        editor.activatePage("Table");
    }
    
    
    public void verifyFunc(CTestFunction func, String funcName, String params, String retVal) {
        
        assertEquals(funcName, func.getName());
        
        StrVector paramsV = new StrVector();
        func.getPositionParams(paramsV);
        assertEquals(params, CYAMLUtil.strVector2Str(paramsV));
        
        assertEquals(retVal, func.getRetValueName());
    }
}


/** 
 * This class can be used to print all components as seen by SWTBot.
 * Usage:
 *     m_bot.widget(new DebugMatcher<Tree>()); // prints all widgets, not only Trees
 *
 * @author markok
 */
class DebugMatcher<T> extends AbstractMatcher<T> {

    private int m_textIdx = 0;

    @Override
    public void describeTo(Description description) {
        description.appendText("found by MyMatcher");
    }

    @Override
    protected boolean doMatch(Object item) {

        if (item instanceof Button) {
            Button btn = (Button)item;
            System.out.println("~~~ Button: " + btn.getText());

        } else if (item instanceof ToolItem) {
            ToolItem tool = (ToolItem)item;
            System.out.println("### Tool: " + tool.getToolTipText());

        } else if (item instanceof Label) {
            Label label = (Label)item;
            System.out.println("@@@ Label: " + label.getText());

        } else if (item instanceof Tree) {
            Tree tree= (Tree)item;
            System.out.println("|_|_ Tree: " + tree.getItemCount());

        } else if (item instanceof Shell) {
            Shell shell = (Shell)item;
            System.out.println("()() Shell: " + shell.getText());

        } else if (item instanceof Text) {
            Text textCtrl = (Text)item;
            /* String botId = (String)textCtrl.getData(SWTBotPreferences.DEFAULT_KEY);
         if (botId.equals("bot_status_text")) {
         // background change is visible only after the test is finished :-( 
             textCtrl.setBackground(new Color(Display.getCurrent(), 255, 0, 0));
         } */

            System.out.println("--- Text " + m_textIdx + ": " + textCtrl.getText());
            m_textIdx++;
        } else {
            System.out.println(">>> Widget: " + item.getClass().getSimpleName());
        }

        return false;
    }
}


// Reads std and err output streams of external process and prints them 
// to (Eclipse) console.
class ExternalProcessStream extends Thread
{
    private InputStream m_is;
    private boolean m_isErrorStream;
    
    ExternalProcessStream(InputStream is, boolean isErrorStream)
    {
        m_is = is;
        m_isErrorStream = isErrorStream;
    }
    
    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(m_is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            
            while ((line = br.readLine()) != null) {
                if (m_isErrorStream) {
                    System.err.println(line);
                } else {
                    System.out.println(line);
                }    
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}
