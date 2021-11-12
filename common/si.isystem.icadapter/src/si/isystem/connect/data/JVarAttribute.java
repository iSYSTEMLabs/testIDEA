package si.isystem.connect.data;

public class JVarAttribute {

    private boolean m_isEditable;
    
    public JVarAttribute(boolean isEditable) {
        m_isEditable = isEditable;
    }

    
    public boolean isEditable() {
        return m_isEditable;
    }
}
