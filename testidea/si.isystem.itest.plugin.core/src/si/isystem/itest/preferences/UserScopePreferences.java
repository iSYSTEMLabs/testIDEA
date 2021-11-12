package si.isystem.itest.preferences;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.main.Activator;

/**
 * This class implements preferences, which are stored to XML files. By default
 * They are stored to user's home directory, but it is possible to flush(File), 
 * load(File) them from any file/directory.
 *   
 * @author markok
 */
@XmlRootElement(name="userScopePreferences")
public class UserScopePreferences implements IEclipsePreferences {

    private IEclipsePreferences m_parent;

    @XmlElement(name = "nodeName")
    protected String m_nodeName;
    
    @XmlElement(name="props")
    protected Map<String, String> m_props;

    @XmlElement(name="userScopePreferences")
    protected List<UserScopePreferences> m_nodes;
    
    private static final String PREFS_FILE_NAME = "testIDEAUserPrefs.xml";
    
    public UserScopePreferences() {
        m_parent = null;
        m_props = new TreeMap<String, String>();
        m_nodes = new ArrayList<>();
        m_nodeName = UserScope.SCOPE;
    }
    
    UserScopePreferences(IEclipsePreferences parent) {
        m_parent = parent;
        m_props = new TreeMap<String, String>();
        m_nodes = new ArrayList<>();
        m_nodeName = UserScope.SCOPE;
    } 

    UserScopePreferences(IEclipsePreferences parent, String nodeName) {
        m_parent = parent;
        m_props = new TreeMap<String, String>();
        m_nodes = new ArrayList<>();
        m_nodeName = nodeName;
    } 


    /** Performs shallow copy only. */
    private void assign(UserScopePreferences userPrefs) {
        m_parent = userPrefs.m_parent;
        m_nodeName = userPrefs.m_nodeName;
        m_props = userPrefs.m_props;
        m_nodes = userPrefs.m_nodes;
    }
    
    
    @Override
    public void put(String key, String value) {
        m_props.put(key, value);
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = m_props.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public void remove(String key) {
        m_props.remove(key);
    }
    
    @Override
    public void clear() throws BackingStoreException {
        m_props.clear();
    }

    @Override
    public void putInt(String key, int value) {
        m_props.put(key, String.valueOf(value));
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Integer.parseInt(val);
        } catch (NumberFormatException ex) {
            return defaultValue; 
        }
    }

    @Override
    public void putLong(String key, long value) {
        m_props.put(key, String.valueOf(value));
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Long.parseLong(val);
        } catch (NumberFormatException ex) {
            return defaultValue; 
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        m_props.put(key, String.valueOf(value));
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Boolean.parseBoolean(val);
        } catch (NumberFormatException ex) {
            return defaultValue; 
        }
    }

    @Override
    public void putFloat(String key, float value) {
        m_props.put(key, String.valueOf(value));
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Float.parseFloat(val);
        } catch (NumberFormatException ex) {
            return defaultValue; 
        }
    }

    @Override
    public void putDouble(String key, double value) {
        m_props.put(key, String.valueOf(value));
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            return defaultValue; 
        }
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        throw new SIllegalArgumentException("Method UserScope.putByteArray() not implemented!");
    }

    @Override
    public byte[] getByteArray(String key, byte[] def) {
        throw new SIllegalArgumentException("Method UserScope.getByteArray() not implemented!");
    }

    @Override
    public String[] keys() throws BackingStoreException {
        
        Set<String> keys = m_props.keySet();
        String [] keysArray = new String[keys.size()];
        int idx = 0;
        
        for (Object key : keys) {
            String keyStr = (String)key;
            keysArray[idx++] = keyStr;
        }
        
        return keysArray;
    }

    @Override
    public String[] childrenNames() throws BackingStoreException {
        
        int idx = 0;
        String [] children = new String[m_nodes.size()];
        
        for (UserScopePreferences node : m_nodes) {
            children[idx++] = node.name();
        }
        return children;
    }

    @Override
    public Preferences parent() {
        return m_parent;
    }

    @Override
    public boolean nodeExists(String pathName) throws BackingStoreException {
        for (UserScopePreferences node : m_nodes) {
            if (node.name().equals(pathName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return m_nodeName;
    }

    @Override
    public String absolutePath() {
        if (m_parent == null) {
            // bug B10596, m_parent = null when: Help -> About -> Installation Details -> Configuration
            return m_nodeName;
        }
        return m_parent.absolutePath() + "/" + m_nodeName;
    }

    public void load() throws BackingStoreException {
        try {
            load(getUserPrefsFile(PREFS_FILE_NAME));
        } catch (IOException ex1) {
            Activator.log(Status.INFO, "Can not create folder for user preferences!",
                          ex1);
        }
    }

    public void load(File userPrefsFile) throws BackingStoreException {


        if (userPrefsFile.exists()) {
            // create JAXB context and instantiate marshaller
            try {
                JAXBContext context = JAXBContext.newInstance(UserScopePreferences.class);
                Unmarshaller um = context.createUnmarshaller();
                try (FileReader fileReader = new FileReader(userPrefsFile)) {
                    UserScopePreferences userPrefs = 
                            (UserScopePreferences) um.unmarshal(fileReader);
                    userPrefs.setParents();
                    assign(userPrefs);
                }
            } catch (FileNotFoundException ex) {
                Activator.log(Status.ERROR, 
                              "Can not load user preferences: " +
                                  userPrefsFile, 
                              ex);
            } catch (JAXBException | IOException ex) {
                Activator.log(Status.ERROR, 
                              "Can not load user preferences or close the file: " +
                                  userPrefsFile, 
                              ex);
            }
        } else {
            Activator.log(Status.INFO, "User preferences do not exist: " + userPrefsFile.toString(),
                          new Throwable());
        }

    }

    
    private void setParents() {
        for (UserScopePreferences node : m_nodes) {
            node.m_parent = this;
            node.setParents();
        }
    }
    
    
    @Override
    /** Flushes to the default file name to user's home folder. */
    public void flush() throws BackingStoreException {
        try {
            flush(getUserPrefsFile(PREFS_FILE_NAME));
        } catch (IOException ex) {
            Activator.log(Status.ERROR, "Can not save user preferences!", ex);
        }
    }


    /** Flushes the top parent of this class to the given file. */
    public void flush(File userPrefsFile) throws BackingStoreException {

        if (m_parent != null  &&  m_parent instanceof UserScopePreferences) {
            m_parent.flush();
        } else {
            try {
                JAXBContext context = JAXBContext.newInstance(UserScopePreferences.class);
                Marshaller m = context.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                // Write to File
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(userPrefsFile)))) {
                    m.marshal(this, userPrefsFile);
                }
            } catch (JAXBException ex) {
                Activator.log(Status.ERROR, "Can not save user preferences!", ex);
            } catch (IOException ex) {
                Activator.log(Status.ERROR, "Can not close file with user preferences!", ex);
            }
        }
    }

    
    @Override
    public void sync() throws BackingStoreException {
        load();
    }

    @Override
    public void addNodeChangeListener(INodeChangeListener listener) {
        throw new SIllegalArgumentException("Method UserScope.addNodeChangeListener() not implemented!");
    }

    @Override
    public void removeNodeChangeListener(INodeChangeListener listener) {
        throw new SIllegalArgumentException("Method UserScope.removeNodeChangeListener() not implemented!");
    }

    @Override
    public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        throw new SIllegalArgumentException("Method UserScope.addPreferenceChangeListener() not implemented!");
    }

    @Override
    public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
        throw new SIllegalArgumentException("Method UserScope.removePreferenceChangeListener() not implemented!");
    }

    
    @Override
    public void removeNode() throws BackingStoreException {
        // this node should not be removable, because user prefs should always 
        // be preserved
    }

    
    @Override
    public Preferences node(String path) {
        
        if (path.isEmpty()) { // see org.eclipse.core.internal.preferences.EclipsePreferences
            return this;
        }
        
        if (path.charAt(0) == '/') {
            if (m_parent != null) {
                return m_parent.node(path);
            } else {
                throw new SIllegalArgumentException("Parent of the node is null!");
            }
            
        }
        
        // it is relative path
        String [] segments = path.split("/");
        UserScopePreferences node = this;
        for (String segment : segments) {
            node = node.findOrCreateNode(segment);
        }

        return node;
    }

    
    /** @param nodeName must not be path */
    private UserScopePreferences findOrCreateNode(String nodeName) {
        for (UserScopePreferences node : m_nodes) {
            if (node.name().equals(nodeName)) {
                return node;
            }
        }
        
        UserScopePreferences prefs = new UserScopePreferences(this, nodeName);
        m_nodes.add(prefs);
        return prefs;
    }
    
    
    @Override
    public void accept(IPreferenceNodeVisitor visitor) throws BackingStoreException {
        if (visitor.visit(this)) {
            for (UserScopePreferences prefs : m_nodes) {
                prefs.accept(visitor);
            }
        }
    }

    
    private File getUserPrefsFile(String prefsFileName) throws IOException {
        Map<String, String> env = System.getenv();
        String appData = env.get("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        
        File testIdeaPrefsFolder = new File(appData, "testIDEA");
        if (!testIdeaPrefsFolder.exists()) {
            Path testIDEAPrefsPath = Paths.get(testIdeaPrefsFolder.getAbsolutePath());
            Files.createDirectory(testIDEAPrefsPath);
        }
        return new File(testIdeaPrefsFolder, prefsFileName);
    }
}
