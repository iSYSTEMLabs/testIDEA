package si.isystem.connect.data;

import si.isystem.connect.CModule;

/**
 * This is immutable data class, which contains information about source file.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JModule implements Comparable<JModule> {
    private final String m_name;
    private final String m_path;

    public JModule(CModule cModule) {
        m_name = cModule.getName();
        m_path = cModule.getPath();
    }

    /** Initializes the object with the given file name and path. */
    public JModule(String name, String path) {
        m_name = name;
        m_path = path;
    }

    /** Returns the name of the source file. */
    public String getName() {
        return m_name;
    }

    /** Returns the path of the source file. */
    public String getPath() {
        return m_path;
    }

    @Override
    public int compareTo(JModule p) {
        return m_path.compareTo(p.getPath());
    }
    
    @Override
    public int hashCode() {
        return m_path.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null  ||  !(obj instanceof JModule)) {
            return false;
        }
        return m_path.equals(((JModule)obj).getPath());
    }
}
