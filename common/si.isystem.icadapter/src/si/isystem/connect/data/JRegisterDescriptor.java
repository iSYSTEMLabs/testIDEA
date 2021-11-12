package si.isystem.connect.data;


/**
 * This class is immutable wrapper of SRegisterInfo.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JRegisterDescriptor {

    private String m_id;
    private JSType m_type;
    
    public JRegisterDescriptor(String name, JSType type) {
        m_id = name;
        m_type = type;
    }
    
    public String getId() {
        return m_id;
    }
    
    public JSType getType() {
        return m_type;
    }
    
    @Override
    public int hashCode() {
        return m_type.hashCode() ^ m_id.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj != null  &&  obj instanceof JRegisterDescriptor) {
            JRegisterDescriptor reg = (JRegisterDescriptor) obj;
            return (m_type.equals(reg.getType()))  &&  m_id.equals(reg.getId());
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s, name = %s)", this.getClass().getSimpleName(), m_type.toString(), m_id);
    }
}
