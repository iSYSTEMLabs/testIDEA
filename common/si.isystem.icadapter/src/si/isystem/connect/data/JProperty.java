package si.isystem.connect.data;

import si.isystem.connect.CPropertyWrapper;

public class JProperty {

    private static StringBuilder m_indent;
    private String m_key;
    private String m_value;
    
    private JProperty[] m_properties;
    
    public JProperty(CPropertyWrapper property) {
        m_key = property.Key();
        m_value = property.Value();
        
        int numProps = (int) property.NumProperties();
        m_properties = new JProperty[numProps];
        
        for (int idx = 0; idx < numProps; idx++) {
            m_properties[idx] = new JProperty(property.Property(idx));
        }
    }
    

    public String getKey() {
        return m_key;
    }
    
    
    public String getValue() {
        return m_value;
    }
    
    
    public JProperty[] getSubProperties() {
        return m_properties;
    }
    
    
    @Override
    public String toString() {
        boolean isTopLevel = false;
        if (m_indent == null) {
            isTopLevel = true;
            m_indent = new StringBuilder();
        }
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("key: '").append(m_key).
           append("',  value: ").append(m_value).append("'\n");
        
        m_indent.append("  ");
        for (int idx = 0; idx < m_properties.length; idx++) {
            sb.append("  ");
            sb.append(m_properties[idx].toString());
        }
        m_indent.delete(m_indent.length() - 2, m_indent.length());
        
        if (isTopLevel) {
            m_indent = null;
        }
        return sb.toString();
    }
}
