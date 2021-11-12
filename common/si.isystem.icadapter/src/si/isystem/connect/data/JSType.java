package si.isystem.connect.data;

import si.isystem.connect.SType;

/**
 * This class is immutable wrapper of SType structure.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JSType {

    private final short m_bitSize;
    private final short m_type;

    public JSType(SType type) {
        m_bitSize = type.getM_byBitSize();
        m_type = type.getM_byType();
    }

    
    public JSType(short bitSize, short type) {
        m_bitSize = bitSize;
        m_type = type;
    }
    
    
    public short getBitSize() {
        return m_bitSize;
    }

    
    public short getType() {
        return m_type;
    }
    
    
    public SType.EType getTypeAsEnum() {
        return SType.EType.swigToEnum(m_type);
    }
    
    @Override
    public int hashCode() {
        return ((m_type << 16) + m_bitSize);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj != null  &&  
            obj instanceof JSType) {
            JSType type = (JSType) obj;
            return type.getBitSize() == m_bitSize  &&
                   type.getType() == m_type;
        }
        return false;
    }
    
    
    @Override
    public String toString() {
        return String.format("%s(type = %d,  bitSize = %d)", 
                JSType.class.getSimpleName(), m_type, m_bitSize);
    }
}
