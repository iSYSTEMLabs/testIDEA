package si.isystem.connect.data;

import java.util.Map;

import si.isystem.connect.ISFR;

/**
 * This class is immutable wrapper of ISFR.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JSFR {
    private final String m_name;
    private final long m_handle;
    private final long m_address;
    private final int m_bitSize;
    private final int m_bitOffset;
    private final boolean m_hasValue;
    private final long m_properties;
    private final Map<Long, String> m_valueMap;
    private final String m_description;
    private final JSFR[] m_children;
    private final int m_hashCode;

    public enum Radix {BINARY, OCTAL, DECIMAL, HEX};
    
    public JSFR(String name,
                long handle,
                int bitSize,
                boolean hasValue,
                long properties, // not enum type, because properties may be OR-ed together
                Map<Long, String> valueMap,
                JSFR [] children) {
                
        m_name = name;
        m_handle = handle;
        m_address = -1;
        m_bitSize = bitSize;
        m_bitOffset = -1;
        m_hasValue = hasValue;
        m_properties = properties;
        m_valueMap = valueMap;
        m_description = "";
        m_children  = children;
        m_hashCode = createHash();
    }
    
    public JSFR(String name,
                long handle,
                long address,
                int bitSize,
                int bitOffset,
                boolean hasValue,
                long properties, // not enum type, because properties may be OR-ed together
                Map<Long, String> valueMap,
                String description,
                JSFR [] children) {
                
        m_name = name;
        m_handle = handle;
        m_address = address;
        m_bitSize = bitSize;
        m_bitOffset = bitOffset;
        m_hasValue = hasValue;
        m_properties = properties;
        m_valueMap = valueMap;
        m_description = description;
        m_children  = children;
        m_hashCode = createHash();
    }
    
    private int createHash()
    {
        int hash = 0;
        
        hash += (m_name != null ? m_name.hashCode() : 0);
        hash += ((int)m_handle) ^ (m_bitSize << 20);
        hash += ((m_hasValue ? 1 : 0) << 19);
        hash += m_properties;
        
        for (Long key : m_valueMap.keySet()) {
            String val = m_valueMap.get(key);
            hash += key.intValue() + val.hashCode();
        }
        
        for (JSFR child : m_children) {
            hash += child.hashCode();
        }

        return hash;
    }

    public String getName() {
        return m_name;
    }

    public long getHandle() {
        return m_handle;
    }

    public long getAddress() {
        return m_address;
    }

    public int getBitSize() {
        return m_bitSize;
    }

    public int getBitOffset() {
        return m_bitOffset;
    }

    public boolean hasValue() {
        return m_hasValue;
    }

    public long getProperties() {
        return m_properties;
    }

    public Map<Long, String> getValueMaps() {
        return m_valueMap;
    }

    public String getDescription() {
        return m_description;
    }

    public boolean hasChildren() {
        return m_children.length > 0;
    }
    
    public JSFR[] getChildren() {
        return m_children;
    }

    public boolean isFloat() {
        return ((m_properties & ISFR.EProperties.pFloat.swigValue()) > 0)  &&
               m_bitSize == 32;
    }
    
    public boolean isDouble() {
        return ((m_properties & ISFR.EProperties.pFloat.swigValue()) > 0)  &&
               m_bitSize == 64;
    }
    
    public boolean isUnsigned() {
        return ((m_properties & ISFR.EProperties.pFloat.swigValue()) == 0);
    }

    public boolean isWritable() {
        return ((m_properties & ISFR.EProperties.pWrite.swigValue()) > 0);
    }
    
    public boolean isReadable() {
        return ((m_properties & ISFR.EProperties.pRead.swigValue()) > 0);
    }
 
    /**
     * Returns string representation of value of this SFR, but only for integral 
     * values. Floats and doubles are not supported by this method. Hex values are 
     * prepended with leading zeroes if necessary.
     * 
     * @param radix only values 2, 8, 10, and 16 are supported 
     * @param isMapped if true, and value map for the current value exists, then
     * the mapped value is returned. Otherwise number is returned (as string).
     * @param isLong if this bit is true, and long description exists, it is returned.
     * Short and long description are separated by '\n' (winIDEA functionality).  
     * @return
     */
    public String formatValue(long value, Radix radix, boolean isMapped, boolean isLong) {
        if (!isUnsigned()) {
            throw new IllegalStateException("Can not format non-integral SFR value. SFR: " + 
                                            m_name);
        }

        if (isMapped) {
            if (m_valueMap.containsKey(value)) {
                String valueDescription = m_valueMap.get(value);
                int idx = valueDescription.indexOf("\\n");
                if (idx > -1) {
                    String shortDesc = valueDescription.substring(0, idx);
                    String longDesc =  valueDescription.substring(idx + 2);
                    // it may happen, that short desc is followed by '\n' and empty string,
                    // so check for length also
                    if (isLong  &&  longDesc.length() > shortDesc.length()) {
                        return longDesc;
                    }
                    return shortDesc;
                }
                return valueDescription;
            }
        }
        
        String strValue = null;
        switch (radix) {
        case BINARY:
            strValue = Long.toBinaryString(value);
            break;
        case OCTAL:
            strValue = Long.toOctalString(value);
            break;
        case DECIMAL:
            strValue = Long.toString(value);
            break;
        case HEX:
            strValue = Long.toHexString(value);
            int noOfDigits = m_bitSize / 4 + (((m_bitSize % 4) > 0) ? 1 : 0);
            // add leading zeroes if required
            if (noOfDigits > strValue.length()) {
                strValue = "00000000000000000000000000000".substring(0, noOfDigits - strValue.length()) + strValue;
            }
        }
        

        return strValue;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null  &&  obj instanceof JSFR) {
            JSFR sfr = (JSFR)obj;
            return sfr.m_handle == this.m_handle &&
                   sfr.m_bitSize == this.m_bitSize &&
                   sfr.m_name.equals(this.m_name);
        }
        
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return m_hashCode;
    }
    
    @Override
    public String toString()
    {
        return String.format("JSFR(%s, handle=%d, size=%dbit, props=0x%x, child-count=%d)", 
                m_name, m_handle, m_bitSize, m_properties, m_children.length);
    }
}
