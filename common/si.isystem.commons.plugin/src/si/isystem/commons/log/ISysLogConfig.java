package si.isystem.commons.log;

import java.util.Arrays;

public class ISysLogConfig {
    private ISysLogLevel m_level;
    private boolean[] m_customLevels;
    private boolean m_isImmediateFlush;
    
    public ISysLogConfig() {
        this(ISysLogLevel.Error, null, false);
    }
    
    public ISysLogConfig(ISysLogConfig c) {
        this(c.m_level, c.m_customLevels, c.m_isImmediateFlush);
    }
    
    public ISysLogConfig(ISysLogLevel level, boolean[] customLevels, boolean isImmediateFlush) {
        m_level = level;
        if (customLevels != null) {
            m_customLevels = new boolean[customLevels.length];
            System.arraycopy(customLevels, 0, m_customLevels, 0, customLevels.length);
        }
        else {
            m_customLevels = new boolean[ISysCustomLogLevel.values().length];
            Arrays.fill(m_customLevels, false);
                    
        }
        m_isImmediateFlush = isImmediateFlush;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ISysCustomLogLevel cl : ISysCustomLogLevel.values()) {
            if (isLogSet(cl)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(cl.getReadable());
            }
        }
        
        return String.format("Log: %s, Custom logs: [%s], Immediate flushing: %b",
                m_level.toString(),
                sb.toString(),
                m_isImmediateFlush);
    }

    //
    // Error, Warning, Info, ...
    //
    
    public void setLevel(ISysLogLevel level) {
        m_level = level;
    }
    
    public ISysLogLevel getLevel() {
        return m_level;
    }
    
    //
    // Custom log levels
    //
    
    public void clearCustomLevels() {
        Arrays.fill(m_customLevels, false);
    }
    
    public void setEnableCustomLevel(ISysCustomLogLevel level, boolean isEnabled) {
        m_customLevels[level.getIndex()] = isEnabled;
    }
    
    public void toggleCustomLevel(ISysCustomLogLevel level) {
        setEnableCustomLevel(level, !isLogSet(level));
    }
    
    //
    // Immediate flush
    //
    
    public void setImmediateFlush(boolean isImmediateFlush) {
        m_isImmediateFlush = isImmediateFlush;
    }

    public boolean isImmediateFlush() {
        return m_isImmediateFlush;
    }
    
    //
    // Utility
    //
    
    public boolean isLogSet(ISysLogLevel level) {
        return m_level.getSeverity() >= level.getSeverity();
    }
    
    public boolean isLogSet(ISysCustomLogLevel level) {
        return m_customLevels[level.getIndex()];
    }

}
