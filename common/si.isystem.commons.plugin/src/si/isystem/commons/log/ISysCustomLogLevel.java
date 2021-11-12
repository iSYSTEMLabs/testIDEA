package si.isystem.commons.log;

import java.util.HashMap;
import java.util.Map;

public enum ISysCustomLogLevel { 
        Workers     (0,  "WORKERS",        "workers",    "Workers"                       ),
        View        (1,  "VIEW",           "view",       "View"                          ),
        Editor      (2,  "EDITOR",         "editor",     "Editor"                        ),
        Tree        (3,  "TREE",           "tree",       "Tree"                          ),
        Input       (4,  "INPUT",          "input" ,     "Input"                         ),
        Gui         (5,  "GUI",            "gui",        "GUI"                           ),
        IDaemon     (6,  "IDAEMON",        "iDaemon",    "iDaemon"                       ),
        IConn       (7,  "ICONN",          "iConn",      "iConnect"                      ),
        Concurrency (8,  "CONCURRENCY",    "concur",     "Concurrency"                   ),
        ieCalls     (9,  "IE_CALLS",       "ieCall" ,    "iEngine calls"                 ),
        ieConfig    (10, "IE_CONFIG",      "ieCfg" ,     "iEngine config"                ),
        ieCore      (11, "IE_CORE",        "ieCore" ,    "iEngine core"                  ),
        ieTimeline  (12, "IE_TIMELINE",    "ieTimeline", "iEngine timeline"              ),
        ieStat      (13, "IE_STAT",        "ieStat",     "iEngine statistics"            ),
        ieTiming    (14, "IE_TIMING",      "ieTime",     "iEngine timing"                ),
        ieDocNot    (15, "IE_DOC_NOT",     "ieDocNot",   "iEngine document notifications"),
        ieFind      (16, "IE_FIND",        "ieFind",     "iEngine find"                  ),
        ieReqTrace  (17, "IE_REQ_TRACE",   "ieReqTrace", "iEngine request trace"         ); 

        private static final Map<String, ISysCustomLogLevel> s_cmdToLevelMap = new HashMap<>();

        static {
            for (ISysCustomLogLevel level : values()) {
                s_cmdToLevelMap.put(level.getTag().toLowerCase(), level);
            }
        }
        
        public static ISysCustomLogLevel getByTag(String tok) {
            return s_cmdToLevelMap.get(tok.toLowerCase());
        }

        private final int m_idx;
        private final String m_tag;
        private final String m_signature;
        private final String m_readable;
        
        ISysCustomLogLevel(int shift, String signature, String tag, String readable) {
            m_idx = shift;
            m_readable = readable;
            m_signature = signature;
            m_tag = tag;
        }

        public int getIndex() {
            return m_idx;
        }

        public String getTag() {
            return m_tag;
        }

        public String getSignature() {
            return m_signature;
        }

        public String getReadable() {
            return m_readable;
        }
    }