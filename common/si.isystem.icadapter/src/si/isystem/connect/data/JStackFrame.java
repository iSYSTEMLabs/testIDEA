package si.isystem.connect.data;



/**
 * This class is immutable wrapper of IStackFrame.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JStackFrame {

    private final int m_level;
    private final String m_fileName;
    private final int m_lineNumber;
    private final int m_memoryArea;
    private final long m_address;
    private final JVariable[] m_localVars;
    private final JFunction m_function;


    /**
     * Constructor is used by code getting information from isystem.connect and
     * converting it to Java objects.
     * 
     * @param level level of this stack frame, top level function (the one currently
     * executed) has level 0.
     * @param fileName name of the source files, where the function is defined.
     * @param lineNumber function line number
     * @param memoryArea memory area of function start address
     * @param address function start address
     * @param function information about function with execution point 
     * @param localVars array of local function variables 
     */
    public JStackFrame(int level,
                       String fileName,
                       int lineNumber,
                       int memoryArea,
                       long address, 
                       JFunction function, 
                       JVariable[] localVars) {
        
        m_level = level;
        m_fileName = fileName;
        m_lineNumber = lineNumber;
        m_memoryArea = memoryArea;
        m_address = address;
        
        m_function = function;
        m_localVars = localVars;
    }

    /** Returns stack frame level. Top level function (the one currently
     * executed) has level 0.
     * */
    public int getLevel() {
        return m_level;
    }

    /** Returns the name of the source files, where the function is defined. */
    public String getFileName() {
        return m_fileName;
    }


    /** Returns function line number. */
    public int getLineNumber() {
        return m_lineNumber;
    }


    /** Returns memory area of function start address. */
    public int getMemoryArea() {
        return m_memoryArea;
    }


    /** Returns function start address. */
    public long getAddress() {
        return m_address;
    }

    
    /**
     * Returns list of local variables. Arguments are also in this list. To 
     * get list of arguments, call <code>getFunction().getParameters()</code>.
     * */
    public JVariable[] getLocalVars() {
        return m_localVars;
    }

    
    /** Returns information about the function with execution point. Null is a
     * valid return value, when information is not available. */
    public JFunction getFunction() {
        return m_function;
    }
}
