package si.isystem.commons.lambda;

public abstract class ISysNamed implements INamed {
    
    private String m_name;

    public ISysNamed() {
        this("");
    }

    public ISysNamed(String name) {
        setName(name);
    }

    public void setName(String name) {
        m_name = name;
    }
    
    @Override
    public String getName() {
        return m_name;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
