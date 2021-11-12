package si.isystem.connect.data;

import si.isystem.connect.CTestCoverageStatistics;

/**
 * This is immutable wrapper for CCoverageStatistics.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCoverageStatistics {

    private String m_functionName; 
    private float m_bytesExecuted;
    private float m_branchesExecuted;
    private float m_branchesTaken;
    private float m_branchesNotTaken;
    private float m_branchesBoth;
    
    /**
     * Instantiates object and initializes it with data from <sode>stat</code>
     * native object. 
     * 
     * @param stat C++ object containing coverage statistics information.
     */
    public JCoverageStatistics(CTestCoverageStatistics stat) {
        super();
        m_functionName = stat.getFunctionName();
        m_bytesExecuted = stat.getBytesExecuted();
        m_branchesExecuted = stat.getBranchExecuted();
        m_branchesTaken = stat.getBranchTaken();
        m_branchesNotTaken = stat.getBranchNotTaken();
        m_branchesBoth = stat.getBranchBoth();
    }
    
    /** Returns the name of the function for which the statistics was measured. */
    public String getFunctionName() {
        return m_functionName;
    }

    /** Returns the number of bytes executed in the function. */
    public float getBytesExecuted() {
        return m_bytesExecuted;
    }
    
    /** Returns the number of branches executed. */
    public float getBranchesExecuted() {
        return m_branchesExecuted;
    }
    
    /** Returns the number of branches taken. */
    public float getBranchesTaken() {
        return m_branchesTaken;
    }
    
    /** Returns the number of branches not taken. */
    public float getBranchesNotTaken() {
        return m_branchesNotTaken;
    }
    
    /** Returns the number of branches taken both ways. */
    public float getBranchesBoth() {
        return m_branchesBoth;
    }
    
}
