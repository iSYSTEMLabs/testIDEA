package si.isystem.itest.common;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.osgi.service.prefs.BackingStoreException;

import si.isystem.itest.main.Activator;
import si.isystem.mk.utils.PlatformUtils;

/**
 * This class fills up a dynamic menu list, which contains the recently opened files.
 *  
 * @author markok
 */
public class FileOpenRecentDynamicMenu extends CompoundContributionItem {

    public final static String PREFS_NODE_RECENT_FILES = "recentFiles";
    private static final int MAX_RECENT_FILE_LIST_SIZE = 16;
    
    public FileOpenRecentDynamicMenu() {
    }


    public FileOpenRecentDynamicMenu(String id) {
        super(id);
    }

    
    public static void addFile(String fileName) {
        try {
            PlatformUtils.addToListOfPreferences(PREFS_NODE_RECENT_FILES, 
                                                 fileName, 
                                                 MAX_RECENT_FILE_LIST_SIZE);
        } catch (BackingStoreException ex) {
            // ignore - user will not see recent files, which is still better than 
            // exception in GUI code
            Activator.log(Status.ERROR, "Can't add file to the list of recent files!", ex);
        }
    }
    
    
    public static String getLastAddedFile() {
        
        String[] recentFiles;
        try {
            recentFiles = PlatformUtils.getListOfPreferences(PREFS_NODE_RECENT_FILES);
            if (recentFiles.length > 0) {
                return recentFiles[recentFiles.length - 1];
            }
        } catch (BackingStoreException ex) {
            return "";
        }
        return "";
    }
    
    
    @Override
    protected IContributionItem[] getContributionItems() {
        
        // Here's where we dynamically generate the list
        
        IWorkbenchWindow mainWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        try {
            String[] recentFiles = PlatformUtils.getListOfPreferences(PREFS_NODE_RECENT_FILES);
            IContributionItem[] list = new IContributionItem[recentFiles.length];
            int idx = recentFiles.length - 1; // revert the order, so that newer 
                                              // files are on the top in the menu
            
            for (String recentFile : recentFiles) {
                
                Map<String, String> params = new HashMap<String, String>();
                params.put("si.isystem.itest.commands.openRecentParams", recentFile);

                CommandContributionItemParameter menuItem = 
                    new CommandContributionItemParameter(mainWindow, 
                                                         null, // menu item ID
                                                         "si.isystem.itest.commands.fileOpenRecent",  // command id
                                                         params,
                                                         null,  // icon
                                                         null,  // disabled icon
                                                         null,  // hover icon
                                                         String.valueOf(idx + 1) + "  " + recentFile,
                                                         String.valueOf(idx + 1), // mnemonic
                                                         null, // tooltip
                                                         CommandContributionItem.STYLE_PUSH,
                                                         null,
                                                         true);

                list[idx] = new CommandContributionItem(menuItem);
                idx--;
            }
            
            return list;
        } catch (BackingStoreException ex) {
            // ignore - user will not see recent files, which is still better than 
            // exception in GUI building
            
            Activator.log(Status.ERROR, "Can't get the list of recent files!", ex);
            
            IContributionItem[] list = new IContributionItem[0];
            return list;
        }
    }

}
