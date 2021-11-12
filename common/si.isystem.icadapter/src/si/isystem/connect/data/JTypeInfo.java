package si.isystem.connect.data;

/** This class contains information about C/C++ type. */
public class JTypeInfo {

    private String m_typeName;
    private JSType2 m_type;
    
    
    public JTypeInfo(String typeName, JSType2 type) {
        
        m_typeName = typeName;
        m_type = type;
    }


    public String getTypeName() {
        return m_typeName;
    }


    public JSType2 getType() {
        return m_type;
    }
}
