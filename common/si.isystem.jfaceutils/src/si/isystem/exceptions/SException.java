package si.isystem.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The base class for extended exceptions, which support context information about
 * the cause of exception. The context data should be used only for logging and
 * reporting errors to users, while the program flow should not depend on it,
 * unless it is specifically mentioned in the API documentation of the library using
 * these exceptions. The reason is, that compiler can not check if the data is really
 * provided, so there will be a runtime failure in case of changed context data.
 */
public class SException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private Map<String, Object> m_contextData;
    
    public SException() { super(); }

    public SException(String s) { 
        super(s);
    }

    public SException(String s, Throwable t) { 
        super(s, t);
    }
    
    public SException(Throwable t) { 
        super(t);
    }
    
    /**
     * This method adds context information as (name, value) pair. 
     * Auto-boxing can make this method applicable for all types of values.
     */
    public SException add(String name, Object value) {
        if (m_contextData == null) {
            m_contextData = new LinkedHashMap<>();
        }
        m_contextData.put(name, value);
        
        return this; // to be returned to throw statement
    }

    
    /**
     * Returns context data set by <i>add()</i> method. Use this method only if
     * APi documentation states, that some data is intended for such purpose.
     * Otherwise it may happen that future versions of the library will not contain
     * the required data.
     * 
     * @param attr name of the attribute to retrieve
     * @return attribute's value or null if the attribute was not set
     */
    public Object getData(String attr) {
        return m_contextData.get(attr);
    }
    
    
    /**
     * Returns context data set by <i>add()</i> method. Use this method only if
     * APi documentation states, that some data is intended for such purpose.
     * Otherwise it may happen that future versions of the library will not contain
     * the required data.
     * 
     * @return reference to member or null, if no data was set
     */
    public Map<String, Object> getData() {
        return m_contextData;
    }

}
