package si.isystem.connect.data;

import java.util.HashMap;
import java.util.Map;

public class JHilChannel {

    public enum JHilType {
        dIn(0, "Digital Input"),
        dOut(1, "Digital Output"),
        aIn(2, "Analog Input"),
        aOut(3, "Analog Output");
        
        private final int m_id;
        private final String m_name;
        
        private JHilType(int id, String name) {
            m_id = id;
            m_name = name;
        }

        public int getId() {
            return m_id;
        }

        public String getName() {
            return m_name;
        }

        // Utility
        private static final Map<Integer, JHilType> s_byId = new HashMap<>();
        
        static {
            for (JHilType c : values()) {
                s_byId.put(c.getId(), c);
            }
        }
        
        public static JHilType get(long id) {
            return s_byId.get((int)id);
        }
        
        @Override
        public String toString() {
            return "JHilType." + getName();
        }
    }
    
    private final String m_name;
    private final String m_qualifiedName;
    private final JHilType m_type;
    private final boolean m_isAvailable;
    private final String m_unit;
    private final double m_min;
    private final double m_max;
    private final long m_index;

    public JHilChannel(String name, String qualifiedName, long type, boolean isAvailable, String unit, double min, double max, long index) {
        m_name = name;
        m_qualifiedName = qualifiedName;
        m_type = JHilType.get(type);
        m_isAvailable = isAvailable;
        m_unit = unit;
        m_min = min;
        m_max = max;
        m_index = index;
    }

    public String getName() {
        return m_name;
    }

    public String getQualifiedName() {
        return m_qualifiedName;
    }

    public JHilType getType() {
        return m_type;
    }

    public boolean isAvailable() {
        return m_isAvailable;
    }

    public String getUnit() {
        return m_unit;
    }

    public double getMin() {
        return m_min;
    }

    public double getMax() {
        return m_max;
    }

    public long getIndex() {
        return m_index;
    }
    
    @Override
    public String toString() {
        return String.format("JHilChannel(%s, %s, %s, %s, %s, %f, %f, %d)", 
                getName(), getQualifiedName(), getType().getName(), 
                isAvailable() ? "available" : "unavailable", 
                getUnit(),
                getMin(), getMax(), getIndex());    
    }
}
