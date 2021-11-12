package si.isystem.connect.data;

/** 
 * This class contains information about C/C++ typedef. Use type name to get
 * the name of the 'real' type.
 */
public class JTypedef {

    private String m_typedefName;
    private String m_realTypeName;
    
    public JTypedef(String typedefName, String realTypeName) {
        m_typedefName = typedefName;
        m_realTypeName = realTypeName;
    }

    public String getTypedefName() {
        return m_typedefName;
    }

    public String getRealTypeName() {
        return m_realTypeName;
    }
}
