package si.isystem.commons.log;

import java.util.HashMap;
import java.util.Map;

public enum ISysLogLevel {
        Error   (0, "ERROR",   "e", "Error"  ),
        Warn    (1, "WARN",    "w", "Warning"),
        Config  (2, "CONFIG",  "c", "Config" ),
        Info    (3, "INFO",    "i", "Info"   ),
        Verbose (4, "VERBOSE", "v", "Verbose"),
        Debug   (5, "DEBUG",   "d", "Debug"  );
        
        private static final Map<String, ISysLogLevel> s_cmdToLevelMap = new HashMap<>();

        static {
            for (ISysLogLevel level : values()) {
                s_cmdToLevelMap.put(level.getTag(), level);
            }
        }
        
        public static ISysLogLevel getByCmd(String tok) {
            return s_cmdToLevelMap.get(tok);
        }
        
        private final int m_severity;
        private final String m_tag;
        private final String m_readable;
        private final String m_signature;
        
        ISysLogLevel(int severity, String signature, String tag, String readable) {
            m_severity = severity;
            m_signature = signature;
            m_tag = tag;
            m_readable = readable;
        }
        
        public int getSeverity() {
            return m_severity;
        }

        public String getSignature() {
            return m_signature;
        }
        
        public String getTag() {
            return m_tag;
        }

        public String getName() {
            return m_readable;
        }
    }