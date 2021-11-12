package si.isystem.mk.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.UserScope;

/**
 * This class contains misc utils related to Eclipse RCP platform.
 *  
 * @author markok
 */
public class PlatformUtils {


    /**
     * This utility class has only static methods and should not be 
     * instantiated. 
     */
    private PlatformUtils() {}
    

    /**
     * This method lists all files with the given pattern in the given path.
     * For example, to get a list of all files in plugin folder 'templates',
     * with extension xslt, we call this method as:
     * <pre>
     * listTemplatesInPlugin("templates", "*.xslt", true);
     * </pre>
     *  
     * @param path plugin folder to be searched
     * @param pattern file name pattern, for example "*.txt"
     * @param recurse true if subfolders are to be searched
     * @return
     */
    public static List<URL> listTemplatesInPlugin(String path, String pattern, boolean recurse) {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        Enumeration<URL> entries = bundle.findEntries(path, pattern, recurse);
        
        List<URL> urls = new ArrayList<URL>();
        
        if (entries != null) {
            while (entries.hasMoreElements()) {
                urls.add((URL)entries.nextElement());
            }
        }
        
        return urls;
    }
    
    
    public static String[] getListOfPreferences(String nodeId) throws BackingStoreException {
        
        Preferences userPrefs = getUserPrefs();

        Preferences node = userPrefs.node(nodeId);

        getMRUFileListFromConfigScope(nodeId, node);
        
        String [] keys = node.keys();
        Arrays.sort(keys, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                long left = Long.parseLong(o1);
                long right = Long.parseLong(o2);
                
                return (left < right) ? -1 : (left == right ? 0 : 1); 
            }
        }); 

        String [] values = new String[keys.length];
        
        int idx = 0;
        for (String item : keys) {
            values[idx++] = node.get(item, "");
        }

        return values;
    }


    public static Preferences getUserPrefs() {
        IPreferencesService prefsSrevice = Platform.getPreferencesService();
        IEclipsePreferences root = prefsSrevice.getRootNode();
        Preferences userPrefs = root.node(UserScope.SCOPE);
        return userPrefs;
    }


    // for debugging purposes
    static public void printPrefs(Preferences prefs) {
        
        try {
            String[] children = prefs.childrenNames();
            for (String child : children) {
                Preferences childNode = prefs.node(child);
                System.out.println("child: " + child + " = " + childNode.get(child, ""));
                printPrefs(childNode);
            }
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }
    

    /** Returns prefs common to different workspaces. */
    static public IEclipsePreferences getConfigScopePrefs() {
        return ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
    }
    

    /** Returns workspace specific prefs. */
    static public IEclipsePreferences getInstanceScopePrefs() {
        return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
    }

    
    private static void getMRUFileListFromConfigScope(String nodeId,
                                                      Preferences node) throws BackingStoreException {
        // if user preferences do not exist yet they are empty - in such case
        // copy the possibly existing MRU list from Configuration scope
        if (node.keys().length == 0) {
            Preferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
            // on new installations this node does not exist, no copying necessary
            if (prefs.nodeExists(nodeId)) { 
                Preferences cfgScopeNode = prefs.node(nodeId);
                String [] keys = cfgScopeNode.keys();
                for (int i = 0; i < keys.length; i++) {
                    String oldValue = cfgScopeNode.get(keys[i], "");
                    node.put(keys[i], oldValue);
                }
            }
        }
    }
    
    /**
     * 
     * @param nodeId id of the preferences node
     * @param newItem value to store to preferences. Any older instances of this value
     *             are removed from the list
     * @param maxSize max size of list - if max size is exceeded, the oldest item is removed
     *                from the list. If the list is already bigger, it is not truncated to
     *                maxSize. This value must be > 0.
     * @return
     */
    public static void addToListOfPreferences(String nodeId, 
                                              String newItem, 
                                              int maxSize) throws BackingStoreException {
        
        Preferences userPrefs = getUserPrefs();
        
        Preferences node = userPrefs.node(nodeId);

        getMRUFileListFromConfigScope(nodeId, node);
        
        // remove all values equal to the newItem
        String [] keys = node.keys();
        Arrays.sort(keys, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                long left = Long.parseLong(o1);
                long right = Long.parseLong(o2);
                
                return (left < right) ? -1 : (left == right ? 0 : 1); 
            }
        }); 
        
        int numItems = keys.length;
        
        long maxKey = 0;
        for (int i = 0; i < keys.length; i++) {
            String oldValue = node.get(keys[i], "");

            if (oldValue.equals(newItem)) {
                node.remove(keys[i]);
                numItems--;
                
            }
            maxKey = Math.max(maxKey, Long.parseLong(keys[i]));
        }
        
        // remove the oldest item, but only if none of the equal items has already
        // been removed
        if (numItems >= keys.length  &&  numItems >= maxSize) {
            node.remove(keys[0]);
        }
        
        node.put(String.valueOf(maxKey + 1), newItem);
        
        node.flush();
    }
    
    /**
     * This method returns stream from resource file, which is located in 
     * plug-in jar file.
     * 
     * @param fileName file name relative to plug-in root folder, for example
     *        'resources/myFile.txt'
     * @return input stream from file
     * @throws IOException 
     */
    public static FileInputStream openFileInPlugin(String fileName) throws IOException {
        
        IPath path = new Path(fileName);
        
        // Also works: URL url = Activator.getDefault().getBundle().getEntry("/" + fileName); 
        URL url = FileLocator.find(Activator.getDefault().getBundle(), path, null);
        
        if (url == null) {
            throw new IOException("Can not convert path to URL: '" + fileName + "'");
        }
        
        String file = FileLocator.toFileURL(url).getFile();
        
        return new FileInputStream(file);
    }
    
    
    public static URL getURLFromPluginPath(String fileName) throws IOException {
        
        IPath path = new Path(fileName);
        
        // Also works: URL url = Activator.getDefault().getBundle().getEntry("/" + fileName); 
        URL url = FileLocator.find(Activator.getDefault().getBundle(), path, null);
        
        if (url == null) {
            throw new IOException("Can not convert path to URL: '" + fileName + "'");
        }
        
        return FileLocator.toFileURL(url);
    }
    
}
