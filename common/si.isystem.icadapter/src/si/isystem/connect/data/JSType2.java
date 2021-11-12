package si.isystem.connect.data;

import si.isystem.connect.SType2;

public class JSType2 extends JSType {
    
    short m_byType2;
    // long m_dw1;
    // long m_dw2;
    
    public JSType2(SType2 type) {
        super(type);
    
        m_byType2 = type.getM_byType2();
        // m_dw1 = type.getM_dw1();
        // m_dw2 = type.getM_dw2();
    }
    
    public JSType2(short bitSize, short type) {
        super(bitSize, type);
    }

    public short getType2() {
        return m_byType2;
    }

    public SType2.EType2 getType2AsEnum() {
        return SType2.EType2.swigToEnum(m_byType2);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj) &&
               ((JSType2)obj).getType2() == this.getType2();
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode() + m_byType2;
    }
    
    @Override
    public String toString()
    {
        return String.format("%s(0x%x)", JSType2.class.getSimpleName(), m_byType2);
    }
}
