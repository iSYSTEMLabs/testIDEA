package si.isystem.itest.run;

import java.util.List;

public class ExtensionScriptInfo {
    
    private boolean m_isBeforeTestMethod;
    private boolean m_isGetTestReportCustomDataMethod;
    private boolean m_isAfterReportSaveMethod;
    private List<String> m_customMethods;
    private List<String> m_rangeMethods;

    public ExtensionScriptInfo(boolean isBeforeTestImpl, boolean isGetTestReportCustomDataImpl,
                               boolean isAfterReportSaveMethod, List<String> customMethods,
                               List<String> rangeMethods) {
        
        m_isBeforeTestMethod = isBeforeTestImpl;
        m_isGetTestReportCustomDataMethod = isGetTestReportCustomDataImpl;
        m_isAfterReportSaveMethod = isAfterReportSaveMethod;
        m_customMethods = customMethods;
        m_rangeMethods = rangeMethods;
    }
    
    
    // This feature is currently not implemented in testIDEA - scripts can be run
    // from init sequence.
    public boolean isBeforeTestsImplemented() {
        return m_isBeforeTestMethod;
    }


    public boolean isGetTestReportCustomDataImplemented() {
        return m_isGetTestReportCustomDataMethod;
    }


    public boolean isAfterTestImplemented() {
        return m_isAfterReportSaveMethod;
    }


    public List<String> getCustomMethods() {
        return m_customMethods;
    }


    public List<String> getRangeMethods() {
        return m_rangeMethods;
    }
}
