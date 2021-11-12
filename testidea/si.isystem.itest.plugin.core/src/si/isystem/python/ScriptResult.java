package si.isystem.python;

import java.util.ArrayList;
import java.util.List;

import si.isystem.exceptions.SIllegalStateException;

/**
 * This class contains output produced by executed script statement - text
 * printed to stderr and text printed to stdout.
 */
public class ScriptResult {

    private final List<String> m_stdout;  // output printed during function execution (print 
                                    // statements, for example)
    private List<String> m_stderr;  // errors
    private List<String> m_funcRetVal = null; // value returned by the function
    private boolean m_isCanceled = false;

    public ScriptResult(List<String> stdout, List<String> stderr) {
        m_stdout = stdout;
        m_stderr = stderr;
    }

    public ScriptResult() {
        m_stdout = new ArrayList<>();
        m_stderr = new ArrayList<>();
    }

    public List<String> getStdout() {
        return m_stdout;
    }

    public List<String> getStderr() {
        return m_stderr;
    }
    
    public List<String> getFuncRetVal() {
        return m_funcRetVal;
    }
    
    
    /** 
     * Returns true if script function returned something else than None.  
     */
    public boolean isFunctionReturendValue() {
        return m_funcRetVal != null;
    }
    
    
    public boolean isTimeout() {
        return isTimeout(m_stderr);
    }
    

    public boolean isError() {
        return isError(m_stderr);
    }
    
    
    /** Returns true, if the last line in lines is null, false otherwise. */
    protected static boolean isTimeout(List<String> lines) {
        if (lines.isEmpty()) {
            throw new SIllegalStateException("Can't verify stderr, threre are no lines!");
        }
        
        String lastLine = lines.get(lines.size() - 1);
        if (lastLine == null) { // null instead of prompt means timeout
            return true;
        }
        
        return false;
    }

    
    protected static boolean isError(final List<String> lines) {
        
        if (lines == null) {
            return false;
        }
        
        if (lines.isEmpty()) {
            throw new SIllegalStateException("Can't verify stderr, there are no lines!");
        }

        final String lastLine = lines.get(lines.size() - 1);
        
        // if there is anything else on stderr than one line with prompt, there was an error
        if (lines.size() > 1  ||  lastLine == null  ||  !lastLine.equals(ReaderThread.PROMPT)) {
            return true;
        }

        return false;
    }
    
    
    public boolean isCanceled() {
        return m_isCanceled;
    }

    
    public void setCanceled(boolean isCanceled) {
        m_isCanceled = isCanceled;
    }


    public String toUIString() {
        
        StringBuilder sb = new StringBuilder('\n');
        
        if (!m_stdout.isEmpty()) {
            sb.append("  stdout:\n");
            for (String out : m_stdout) {
                sb.append("    ").append(out).append('\n');
            }
        }
        
        if (isError()) {
            sb.append("  stderr:\n");
            for (String out : m_stderr) {
                sb.append("    ").append(out).append('\n');
            }
        }
        
        if (m_funcRetVal != null  &&  !m_funcRetVal.isEmpty()) {
            sb.append("  retVal:\n");
            for (String out : m_funcRetVal) {
                sb.append("    ").append(out).append('\n');
            }
        }
        
        return sb.toString();
    }

    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder('\n');
        
        if (!m_stdout.isEmpty()) {
            sb.append("  stdout:\n");
            for (String out : m_stdout) {
                sb.append("    ").append(out).append('\n');
            }
        }
        
        if (!m_stderr.isEmpty()) {
            sb.append("  stderr:\n");
            for (String out : m_stderr) {
                sb.append("    ").append(out).append('\n');
            }
        }
        
        if (m_funcRetVal != null  &&  !m_funcRetVal.isEmpty()) {
            sb.append("  retVal:\n");
            for (String out : m_funcRetVal) {
                sb.append("    ").append(out).append('\n');
            }
        }
        
        return sb.toString();
    }

    
    public void appendStdErr(final List<String> stderr) {
        if (m_stderr == null) {
            m_stderr = new ArrayList<String>();
        }
        m_stderr.addAll(stderr);
    }

    public void setFuncRetVal(final List<String> stdout) {
        m_funcRetVal = new ArrayList<String>(stdout);
    }
}
