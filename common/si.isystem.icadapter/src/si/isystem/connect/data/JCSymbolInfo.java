package si.isystem.connect.data;

import si.isystem.connect.CSymbolInfo;

/**
 * This is immutable wrapper of CSymbolInfo.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCSymbolInfo {
    
    private final JCAddress m_address;
    private final JSType m_type;
    private final long m_sizeMAUs;

    /**
     * Instantiates object and initializes it with data from <sode>symbolInfo</code>
     * native object. 
     * 
     * @param symbolInfo information about symbol
     */
    public JCSymbolInfo(CSymbolInfo symbolInfo) {
        m_address = new JCAddress(symbolInfo.getMemArea(), symbolInfo.getAddress());
        m_type = new JSType(symbolInfo.getMType());
        m_sizeMAUs = symbolInfo.getSizeMAUs();
    }

    /** Returns address of the symbol location in memory. */
    public JCAddress getAddress() {
        return m_address;
    }

    /** Returns symbol type. */
    public JSType getType() {
        return m_type;
    }

    /** Retursn size of the symbol in memory addressable units (bytes for most CPUs).
     */
    public long getSizeMAUs() {
        return m_sizeMAUs;
    }
    
    
    @Override
    public String toString() {
        return getClass().getName() + "(address = " + m_address + ",\ntype = " + 
               m_type + ",\nsize MAUs:" + m_sizeMAUs + ")";
    }
}
