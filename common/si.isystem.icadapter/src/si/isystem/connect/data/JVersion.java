package si.isystem.connect.data;

import java.util.Objects;

import si.isystem.connect.CWinIDEAVersion;

/**
 * This class is Java version of CWinIDEAVersion. It is immutable.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JVersion implements Comparable<JVersion> {

    private final int m_major;
    private final int m_minor;
    private final int m_build;

    public JVersion(int major, int minor, int build) {
        m_major = major;
        m_minor = minor;
        m_build = build;
    }

    public JVersion(CWinIDEAVersion version) {
        m_major = version.getMajor();
        m_minor = version.getMinor();
        m_build = version.getBuild();
    }

    public int getMajor() {
        return m_major;
    }

    public int getMinor() {
        return m_minor;
    }

    public int getBuild() {
        return m_build;
    }

    
    /* public static long ver2Long(int verMajor, int verMinor, int verBuild) {
        if (verMinor > 999  ||  verBuild > 9999) {
            m_statusText.setBackground(ColorProvider.instance().getErrorColor());
            m_statusText.setText("Internal error - check version number range.");
        }
        return verMajor * 10_000_000L + verMinor * 10_000L + verBuild;
    } */

    
    /**
     * Compares this object with the specified object for order. Returns -1, 
     * 0, or 1 if this object is less 
     * than, equal to, or greater than the specified object.      
     *  
     * @param version
     * @return
     */
    @Override
    public int compareTo(JVersion version) {
       
        
        if (m_major > version.m_major) {
            return 1;
        } else if (m_major == version.m_major) {
            
            if (m_minor > version.m_minor) {
                return 1;
            } else if (m_minor == version.m_minor) {
                if (m_build > version.m_build) {
                    return 1;
                } else if (m_build == version.m_build) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_major, m_minor, m_build);
    }

    @Override 
    public boolean equals(Object other) {
        
        if (other == null) {
            return false;
        }
        
        if (!(other instanceof JVersion)) {
            return false;
        }
        
        return compareTo((JVersion)other) == 0;
    }
    
    
    @Override
    public String toString() {
        return m_major + "." + m_minor + "." + m_build;
    }
}
