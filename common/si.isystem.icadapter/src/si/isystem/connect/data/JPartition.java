package si.isystem.connect.data;

import si.isystem.connect.IPartition;

public class JPartition implements Comparable<JPartition> {

    private final String m_name;
    private final String m_path;

    public JPartition(IPartition iPart) {
        if (iPart == null || iPart.Name() == null || iPart.Path() == null) {
            throw new NullPointerException();
        }
        
        m_name = iPart.Name();
        m_path = iPart.Path();
    }
    
    public String getName() {
        return m_name;
    }
    
    public String getPath() {
        return m_path;
    }

    @Override
    public int compareTo(JPartition p) {
        return m_path.compareTo(p.getPath());
    }
    
    @Override
    public int hashCode() {
        return m_path.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null  ||  !(obj instanceof JPartition)) {
            return false;
        }
        return m_path.equals(((JPartition)obj).getPath());
    }
}
