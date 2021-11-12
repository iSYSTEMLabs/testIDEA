package si.isystem.connect.data;

import java.util.Objects;

import si.isystem.connect.CPUStatus;

/**
 * This class is immutable wrapper of CPUStatus.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCPUStatus {
    private int m_executionArea;
    private long m_executionPoint;

    private boolean m_isMustInit; 
    private boolean m_isStopped;
    private boolean m_isRunning;
    private boolean m_isReset; 
    private boolean m_isHalted; 
    private boolean m_isWaiting; 
    private boolean m_isAttach; 
    private boolean m_isIdle; 
    private boolean m_isStopReasonExplicit; 
    private boolean m_isStopReasonBP; 
    private boolean m_isStopReasonStep; 
    private boolean m_isStopReasonHW; 


    /**
     * Constructs object with undefined state - used for initialization, when the 
     * status is not known yet.
     */
    public JCPUStatus() {
        m_executionArea = -1;
        m_executionPoint = -1;
    }
    
    
    /**
     * Instantiates object and initializes it with data from <sode>cpuStatus</code>
     * native object. 
     * 
     * @param cpuStatus C++ object containing CPU status.
     */
    public JCPUStatus(CPUStatus cpuStatus) {
        
        m_isMustInit = cpuStatus.isMustInit(); 
        m_isStopped = cpuStatus.isStopped();
        m_isRunning = cpuStatus.isRunning();
        m_isReset = cpuStatus.isReset(); 
        m_isHalted = cpuStatus.isHalted(); 
        m_isWaiting = cpuStatus.isWaiting(); 
        m_isAttach = cpuStatus.isAttach(); 
        m_isIdle = cpuStatus.isIdle(); 
        m_isStopReasonExplicit = cpuStatus.isStopReasonExplicit(); 
        m_isStopReasonBP = cpuStatus.isStopReasonBP(); 
        m_isStopReasonStep = cpuStatus.isStopReasonStep(); 
        m_isStopReasonHW = cpuStatus.isStopReasonHW(); 
        
        m_executionArea = cpuStatus.getExecutionArea();
        m_executionPoint = cpuStatus.getExecutionPoint();
    }

    /** Returns true, if CPU is in MUST INIT state. */
    public boolean isMustInit() {
        return m_isMustInit;
    }
    
    /** Returns true, if CPU is not running. */
    public boolean isStopped() {
        return m_isStopped;
    }
    
    /** Returns true, if CPU is running. */
    public boolean isRunning() {
        return m_isRunning;
    }
 
    /** Returns true, if CPU is in RESET state. */
    public boolean isReset() {
        return m_isReset;
    }
 
    /** Returns true, if CPU is in HALTED state. */
    public boolean isHalted() {
        return m_isHalted;
    }
 
    /** Returns true, if CPU is in WAITING state. */
    public boolean isWaiting() {
        return m_isWaiting;
    }
 
    /** Returns true, if CPU is in ATTACH state. */
    public boolean isAttach() {
        return m_isAttach;
    }
 
    /** Returns true, if CPU is IDLE state. */
    public boolean isIdle() {
        return m_isIdle;
    }
 
    /** Returns true, if CPU was stopped on request. */
    public boolean isStopReasonExplicit() {
        return m_isStopReasonExplicit;
    }
 
    /** Returns true, if CPU was stopped on breskpoint. */
    public boolean isStopReasonBP() {
        return m_isStopReasonBP;
    }
 
    /** Returns true, if CPU was stopped after stepping. */
    public boolean isStopReasonStep() {
        return m_isStopReasonStep;
    }
 
    /** Returns true, if CPU was stopped on hardware breakpoint. */
    public boolean isStopReasonHW() {
        return m_isStopReasonHW;
    }

    /** Returns memory area of the address where the execution stopped. */
    public int getExecutionArea() {
        return m_executionArea;
    }

    /** Returns address where the execution stopped. */
    public long getExecutionPoint() {
        return m_executionPoint;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(m_executionArea, m_executionPoint);
    }
    
    /** Returns true, if objects are equal (address and area are the same). */ 
    @Override
    public boolean equals(Object status) {
        
        if (status == null) {
            return false;
        }
        
        // TODO: IV or MK: Should we also check all boolean fields (also for hashCode)?
        if (status instanceof JCPUStatus) {
            JCPUStatus cpuStatus = (JCPUStatus) status;
            return 
                   m_executionArea == cpuStatus.getExecutionArea()  &&
                   m_executionPoint == cpuStatus.getExecutionPoint();        
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "(execPoint = " + m_executionPoint + ", execArea = " + m_executionArea + ")";
    }
} 
