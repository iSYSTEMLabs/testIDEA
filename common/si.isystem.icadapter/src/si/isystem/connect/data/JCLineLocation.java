package si.isystem.connect.data;

import si.isystem.connect.CLineLocation;

/**
 * This is immutable wrapper of CLineLocation.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCLineLocation {

    private final String m_fileName;
    private final long m_lineNumber;

    /**
     * Instantiates object and initializes it with data from <sode>lineLocation</code>
     * native object. 
     * 
     * @param lineLocation C++ object containing line location
     */
    public JCLineLocation(CLineLocation lineLocation) {
        m_fileName = lineLocation.getFileName();
        m_lineNumber = lineLocation.getLineNumber();
    }

    /** Returns name of the file, where the line is located. */
    public String getFileName() {
        return m_fileName;
    }

    /** Returns the line number in the file. */
    public long getLineNumber() {
        return m_lineNumber;
    }
}
