package si.isystem.itest.run;

import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.data.JCoverageStatistics;

public class JCoverageResult {

    private float m_bytesAll;
    private float m_branchesAll;

    private float m_bytesExecutedMeasured;
    private float m_branchesExecutedMeasured;
    private float m_branchesTakenMeasured;
    private float m_branchesNotTakenMeasured;
    private float m_branchesBothMeasured;

    private JCoverageStatistics m_statExpected;


    public JCoverageResult(CTestCoverageStatistics stat,
                           float bytesAll,
                           float bytesExecutedMeasured,
                           float branchesAll,
                           float branchesExecutedMeasured,
                           float branchesTakenMeasured,
                           float branchesNotTakenMeasured,
                           float branchesBothMeasured) {
        
        m_statExpected = new JCoverageStatistics(stat);
        
        m_bytesAll = bytesAll;
        m_branchesAll = branchesAll;
        m_bytesExecutedMeasured = bytesExecutedMeasured;
        m_branchesExecutedMeasured = branchesExecutedMeasured;
        m_branchesTakenMeasured = branchesTakenMeasured;
        m_branchesNotTakenMeasured = branchesNotTakenMeasured;
        m_branchesBothMeasured = branchesBothMeasured;
    }


    public String getAreaName() {
        return m_statExpected.getFunctionName();
    }

    public JCoverageStatistics getStatExpected() {
        return m_statExpected;
    }
    

    public float getBytesExecutedMeasured() {
        return m_bytesExecutedMeasured / m_bytesAll * 100;
    }


    public float getBranchesExecutedMeasured() {
        return m_branchesExecutedMeasured / m_branchesAll * 100;
    }


    public float getBranchesTakenMeasured() {
        return m_branchesTakenMeasured / m_branchesAll * 100;
    }


    public float getBranchesNotTakenMeasured() {
        return m_branchesNotTakenMeasured / m_branchesAll * 100;
    }


    public float getBranchesBothMeasured() {
        return m_branchesBothMeasured / m_branchesAll * 100;
    }


    public boolean isError() {
        return isBytesExecutedError() || isBranchesExecutedError() ||
               isBranchesTakenError() || isBranchesNotTakenError() ||
               isBranchesBothError();
    }
    
    public boolean isBytesExecutedError() {
        if (m_bytesAll == 0) {
            return false;
        }
        return m_statExpected.getBytesExecuted() > getBytesExecutedMeasured();
    }
    
    public boolean isBranchesExecutedError() {
        if (m_branchesAll == 0) {
            return false;
        }
        return m_statExpected.getBranchesExecuted() > getBranchesExecutedMeasured();
    }
    
    public boolean isBranchesTakenError() {
        if (m_branchesAll == 0) {
            return false;
        }
        return m_statExpected.getBranchesTaken() > getBranchesTakenMeasured();
    }
    
    public boolean isBranchesNotTakenError() {
        if (m_branchesAll == 0) {
            return false;
        }
        return m_statExpected.getBranchesNotTaken() > getBranchesNotTakenMeasured();
    }
    
    public boolean isBranchesBothError() {
        if (m_branchesAll == 0) {
            return false;
        }
        return m_statExpected.getBranchesBoth() > getBranchesBothMeasured();
    }
}
