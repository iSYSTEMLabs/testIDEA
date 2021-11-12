package si.isystem.itest.common;

/**
 * This is struct class containing absolute paths for directories used by 
 * testIDEA.   
 * 
 * @author markok
 *
 */
public class ISysDirs {

    String m_winIDEAWorkspaceDir;
    String m_winIDEAWorkspaceFile;
    String m_defaultDownloadFile;
    String m_iyamlDir;
    String m_reportDir;
    String m_dotExeDir;
    
    public ISysDirs(String winIDEAWorkspaceDir,
                    String winIDEAWorkspaceFile,
                    String defaultDownloadFile,
                    String iyamlDir,
                    String reportDir, 
                    String dotExeDir) {
        
        m_winIDEAWorkspaceDir = winIDEAWorkspaceDir;
        m_winIDEAWorkspaceFile = winIDEAWorkspaceFile;
        m_defaultDownloadFile = defaultDownloadFile;

        m_iyamlDir = iyamlDir;
        m_reportDir = reportDir;
        m_dotExeDir = dotExeDir;
    }

    
    public String getWinIDEAWorkspaceDir() {
        return m_winIDEAWorkspaceDir;
    }


    public String getWinIDEAWorkspaceFile() {
        return m_winIDEAWorkspaceFile;
    }


    public String getDefaultDownloadFile() {
        return m_defaultDownloadFile;
    }


    public String getIyamlDir() {
        return m_iyamlDir;
    }


    public String getReportDir() {
        return m_reportDir;
    }

    
    public String getDotExeDir() {
        return m_dotExeDir;
    }
}
