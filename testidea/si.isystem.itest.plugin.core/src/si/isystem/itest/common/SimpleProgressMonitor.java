package si.isystem.itest.common;

import org.eclipse.core.runtime.IProgressMonitor;

public class SimpleProgressMonitor implements IProgressMonitor {

    private boolean m_isCanceled = false;
    
    @Override
    public void beginTask(String name, int totalWork) {
        m_isCanceled = false;
    }

    @Override
    public void done() {}

    @Override
    public void internalWorked(double work) {}

    @Override
    public boolean isCanceled() {
        return m_isCanceled;
    }

    @Override
    public void setCanceled(boolean isCanceled) {
        m_isCanceled = isCanceled;        
    }

    @Override
    public void setTaskName(String name) {}

    @Override
    public void subTask(String name) {}

    @Override
    public void worked(int work) {}    
}