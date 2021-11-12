package si.isystem.itest.model;

import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestTreeNode;


public class StatusTableLine {

    // enum items must be in ascending criticality order, as ordinal() is used on this type. 
    public enum StatusType {OK, INFO, WARNING, ERROR, FATAL};
    
    private StatusType m_statusType;
    private String m_fileName;
    private String m_testId;
    private String m_functionName;
    private String m_message;
    private CTestResult m_result;
    private CTestTreeNode m_testTreeNode;
    private CTestGroupResult m_groupResult;

    
    public StatusTableLine(StatusType statusType,
                           String fileName,
                           String testId,
                           String functionName,
                           String message,
                           CTestResult result) {
        super();
        m_statusType = statusType;
        m_fileName = fileName;
        m_testId = testId;
        m_functionName = functionName;
        m_message = message;
        m_result = result;
        m_testTreeNode = null;
    }

    
    public StatusTableLine(StatusType statusType,
                           String fileName,
                           String testId,
                           String functionName,
                           CTestGroupResult result,
                           String message) {
        super();
        m_statusType = statusType;
        m_fileName = fileName;
        m_testId = testId;
        m_functionName = functionName;
        m_message = message;
        m_groupResult = result;
        m_testTreeNode = null;
    }

    
    public StatusTableLine(StatusType statusType,
                           String fileName,
                           String testId,
                           String functionName,
                           CTestTreeNode testSpec,
                           String message) {
        super();
        m_statusType = statusType;
        m_fileName = fileName;
        m_testId = testId;
        m_functionName = functionName;
        m_message = message;
        m_result = null;
        m_testTreeNode = testSpec;
    }

    
    public StatusType getStatusType() {
        return m_statusType;
    }

    
    public String getFileName() {
        return m_fileName;
    }


    public String getTestId() {
        return m_testId;
    }

    public String getFunctionName() {
        return m_functionName;
    }

    public String getMessage() {
        return m_message;
    }
    
    public CTestResult getResult() {
        return m_result;
    }

    public CTestGroupResult getGroupResult() {
        return m_groupResult;
    }

    public CTestTreeNode getTestTreeNode() {
        return m_testTreeNode;
    }

}
