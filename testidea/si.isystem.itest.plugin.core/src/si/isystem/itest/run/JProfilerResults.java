package si.isystem.itest.run;

import si.isystem.connect.CProfilerStatistic;

/**
 * This class contains results of profiler session for one area. It is stored in 
 * model.TestCaseResult for display of errors. 
 * 
 * @author markok
 *
 */
public class JProfilerResults {

    public enum ProfilerErrCode {ERR_NONE, ERR_MIN, ERR_MAX, 
                                 ERR_BOTH};  // used for data values without value specified,
                                 // which means that multiple values and therefore results
                                 // map to the same specification, so both boundaries 
                                 // may be exceeded
    
    private String m_areaName;

    private long m_minTime;
    private long m_maxTime;
    private long m_totalTime;
    private long m_numHits;
    private long m_value;

    private boolean m_isError = false;
    private ProfilerErrCode m_minTimeError = ProfilerErrCode.ERR_NONE;
    private ProfilerErrCode m_maxTimeError = ProfilerErrCode.ERR_NONE;
    private ProfilerErrCode m_totalTimeError = ProfilerErrCode.ERR_NONE;
    private ProfilerErrCode m_numHitsError = ProfilerErrCode.ERR_NONE;

    private boolean m_isDataValueSpecified; // true if data value in test specification was specified, eg. "myVar, 5" 
    
    JProfilerResults(String areaName, CProfilerStatistic stat) {
        m_areaName = areaName;
        m_minTime = stat.getMinTime();
        m_maxTime = stat.getMaxTime();
        m_totalTime = stat.getTotalTime();
        m_numHits = stat.getNumHits();
        
        m_value = stat.getValue();
    }

    /** This ctor is used, when there is no statistics provided for area.
     * Members are set to invalid values.
     * @param areaName
     */
    JProfilerResults(String areaName) {
        m_areaName = areaName;
        m_minTime = -1;
        m_maxTime = -1;
        m_totalTime = -1;
        m_numHits = -1;
        
        m_value = 0;
    }
    
    public String getAreaName() {
        return m_areaName;
    }

    public long getMinTime() {
        return m_minTime;
    }

    public long getMaxTime() {
        return m_maxTime;
    }

    public long getTotalTime() {
        return m_totalTime;
    }

    public long getNumHits() {
        return m_numHits;
    }

    public long getValue() {
        return m_value;
    }

    public boolean isError() {
        return m_isError;
    }

    public void setError(boolean isError) {
        m_isError = isError;
    }

    public ProfilerErrCode getMinTimeError() {
        return m_minTimeError;
    }

    public void setMinTimeError(ProfilerErrCode minTimeError) {
        setErrFlag(minTimeError);
        m_minTimeError = minTimeError;
    }

    public ProfilerErrCode getMaxTimeError() {
        return m_maxTimeError;
    }

    public void setMaxTimeError(ProfilerErrCode maxTimeError) {
        setErrFlag(maxTimeError);
        m_maxTimeError = maxTimeError;
    }

    public ProfilerErrCode getTotalTimeError() {
        return m_totalTimeError;
    }

    public void setTotalTimeError(ProfilerErrCode totalTimeError) {
        setErrFlag(totalTimeError);
        m_totalTimeError = totalTimeError;
    }

    public ProfilerErrCode getNumHitsError() {
        return m_numHitsError;
    }

    public void setNumHitsError(ProfilerErrCode numHitsError) {
        setErrFlag(numHitsError);
        m_numHitsError = numHitsError;
    }

    private void setErrFlag(ProfilerErrCode errorCode) {
        if (errorCode != ProfilerErrCode.ERR_NONE) {
            m_isError = true;
        }
    }

    public void setIsDataValueSpecified(boolean b) {
        m_isDataValueSpecified = b;
        
    }

    public boolean isDataValueSpecified() {
        return m_isDataValueSpecified;
    }
}
