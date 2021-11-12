package si.isystem.itest.common;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import si.isystem.exceptions.SIOException;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.ExtensionScriptInfo;

/**
 * This class reads custom methods from Python extensions script, and adds
 * them to iTools menu.
 * It is singleton, and maintains menu IDs of previously added menu options,
 * so that it can remove existing options before adding new ones.  
 */
@SuppressWarnings("restriction")
public class CustomScriptsMenuOptions {

    // commandId, see plugin.xml
    private static final String COMMAND_ID = "si.isystem.itest.plugin.core.execCustomPyScript";

    public static final CustomScriptsMenuOptions INSTANCE = new CustomScriptsMenuOptions();

    public static final String SCRIPT_OR_METHOD_NAME = "si.isystem.itest.plugin.core.exeCustomPyScript.scriptOrMethod";
    public static final String IS_STANDALONE_SCRIPT_CMD_PARAM_ID = "si.isystem.itest.plugin.core.exeCustomPyScript.isStandaloneScript";

    private static final String ITOOLS_MENU_ID = "si.isystem.itest.mainMenu.itools";
    private static final String MENU_ITEM_ID_PREFIX = "custom.cmd.id.";

    private static final int MAX_SCRIPT_ITEMS_IN_MENU = 10;

    private List<String> m_idsOfExistingMenuItems = new ArrayList<>();

    
    private CustomScriptsMenuOptions() {}
    

    public void addPyMethodsAndScriptsToMenu(ExtensionScriptInfo extensionInfo) {
    
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        if(window instanceof WorkbenchWindow) {
            MenuManager mainMenuManager = ((WorkbenchWindow)window).getMenuManager();
            
            MenuManager itoolsMenuMgr = (MenuManager)mainMenuManager.find(ITOOLS_MENU_ID);
            removeOldItemsFromMenu(itoolsMenuMgr);
            addCustomMethodsToMenu(extensionInfo, itoolsMenuMgr, window);
            addPyScriptsToMenu(itoolsMenuMgr, window);
            // itoolsMenuMgr.setVisible(true); // that's a trick to make update of menu text visible
        }
    }
    
    
    /**
     * Opens testIDEA Python extension script file, and searches for methods 
     * with specific prefix string in name. All found methods are added to iTools 
     * menu.
     * @param itoolsMenuMgr 
     * @param window 
     */
    public void addCustomMethodsToMenu(ExtensionScriptInfo extensionInfo, 
                                       MenuManager itoolsMenuMgr, 
                                       IWorkbenchWindow window) {

        List<String> customMethods = extensionInfo.getCustomMethods();
        
        for (String customMethodName : customMethods) {

            String menuItemId = MENU_ITEM_ID_PREFIX + customMethodName;
            m_idsOfExistingMenuItems.add(menuItemId);

            Map<String, String> params = new TreeMap<>();
            params.put(SCRIPT_OR_METHOD_NAME, customMethodName);
            params.put(IS_STANDALONE_SCRIPT_CMD_PARAM_ID, Boolean.FALSE.toString());

            CommandContributionItemParameter commandContributionItemParameter =
                    new CommandContributionItemParameter(window, 
                                                         menuItemId,// menu item id

                                                         COMMAND_ID,

                                                         params,    // parameters, 
                                                         null,      // icon, 
                                                         null,      // disabledIcon, 
                                                         null,      // hoverIcon, 
                                                         "- " + customMethodName + "()", // label 
                                                         null,      // mnemonic 
                                                         null,      // tooltip 
                                                         SWT.NONE,  // style 
                                                         null,      // helpContextId
                                                         true);     // setVisibleEnabled

            itoolsMenuMgr.add(new CommandContributionItem(commandContributionItemParameter));
        }
    }

    
    /** 
     * Scans dir of iyaml file for Python scripts and adds them to iTools menu.  
     * @param window2 
     * @param itoolsMenuMgr2 
     */
    public void addPyScriptsToMenu(MenuManager itoolsMenuMgr, IWorkbenchWindow window) {

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model == null) {
            return;
        }
        
        String iyamlFName = model.getModelFileName();
        Path iyamlPath = Paths.get(iyamlFName);
        Path iyamlDir = iyamlPath.getParent();
        List<String> customScripts = listPyScriptsInDir(iyamlDir);
        
        for (String customScriptName : customScripts) {

            String menuItemId = MENU_ITEM_ID_PREFIX + customScriptName;
            m_idsOfExistingMenuItems.add(menuItemId);

            Map<String, String> params = new TreeMap<>();
            params.put(SCRIPT_OR_METHOD_NAME, customScriptName);
            params.put(IS_STANDALONE_SCRIPT_CMD_PARAM_ID, Boolean.TRUE.toString());

            CommandContributionItemParameter commandContributionItemParameter =
                    new CommandContributionItemParameter(window, 
                                                         menuItemId,// menu item id

                                                         COMMAND_ID,

                                                         params,    // parameters, 
                                                         null,      // icon, 
                                                         null,      // disabledIcon, 
                                                         null,      // hoverIcon, 
                                                         customScriptName, // label 
                                                         null,      // mnemonic 
                                                         null,      // tooltip 
                                                         SWT.NONE,  // style 
                                                         null,      // helpContextId
                                                         true);     // setVisibleEnabled

            itoolsMenuMgr.add(new CommandContributionItem(commandContributionItemParameter));
        }
    }


    private List<String> listPyScriptsInDir(Path iyamlDir) {
        List<String> customScripts = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(iyamlDir, "*.py")) {
            for (Path entry: stream) {
                customScripts.add(entry.getFileName().toString());
            }
        } catch (IOException ex) {
            throw new SIOException("Can not list python scripts!", ex)
                  .add("path", iyamlDir);
        }

        Collections.sort(customScripts);
        
        // remove scripts, if they'd make menu too long 
        for (int idx = MAX_SCRIPT_ITEMS_IN_MENU; idx < customScripts.size(); idx++) {
            customScripts.remove(idx);
        }
        
        return customScripts;
    }

    
    private void removeOldItemsFromMenu(MenuManager itoolsMenuMgr) {
        
        for (String oldMenuItemId : m_idsOfExistingMenuItems) {
            itoolsMenuMgr.remove(oldMenuItemId);
        }
        
        m_idsOfExistingMenuItems.clear();
    }

    
    
}
