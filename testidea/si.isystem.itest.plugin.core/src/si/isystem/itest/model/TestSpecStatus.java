package si.isystem.itest.model;

import org.eclipse.core.runtime.IStatus;

import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.main.Activator;

public class TestSpecStatus implements IStatus {

    private Throwable m_exception;
    private int m_severity = 0;
    private CTestTreeNode m_testTreeNode;
    String m_message;

    public TestSpecStatus(Exception ex) {
        m_exception = ex;
        m_severity = IStatus.ERROR;
        m_testTreeNode = null;
        m_message = m_exception.getMessage();
    }

    
    public TestSpecStatus(int severity, CTestTreeNode testTreeNode, String message) {
        m_exception = null;
        m_severity = severity;
        m_testTreeNode = testTreeNode;
        m_message = message;
    }

    
    @Override
    public IStatus[] getChildren() {
        return null;
    }


    @Override
    public int getCode() {
        return 0;
    }


    @Override
    public Throwable getException() {
        return m_exception;
    }


    @Override
    public String getMessage() {
        return m_message;
    }


    @Override
    public String getPlugin() {
        return Activator.PLUGIN_ID;
    }


    @Override
    public int getSeverity() {
        return m_severity;
    }


    @Override
    public boolean isMultiStatus() {
        return false;
    }


    @Override
    public boolean isOK() {
        return m_severity == IStatus.OK;
    }


    @Override
    public boolean matches(int severityMask) {
        return (severityMask & m_severity) != 0;
    }

    
    public CTestTreeNode getTestTreeNode() {
        return m_testTreeNode;
    }
}
