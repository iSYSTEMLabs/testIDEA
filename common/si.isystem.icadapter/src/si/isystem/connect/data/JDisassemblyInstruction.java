package si.isystem.connect.data;

/**
 * This class is immutable wrapper of IDisassemblyLine.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JDisassemblyInstruction {

    private final String m_functionName;
    private final long m_address;
    private final short m_length;
    private final int m_memArea;
    private final String m_opcode;
    private final String m_opcodeArgs;
    private final String m_instruction;
    private final long m_instructionOffset;
    private String m_fileName;
    private int m_lineNumber;


    public JDisassemblyInstruction(String fileName,
                                   int lineNumber,
                                   String functionName,
                                   int memArea,
                                   long address,
                                   short length,
                                   String opcode,
                                   String opcodeArgs,
                                   String instruction,
                                   long instructionOffset) {
        m_fileName = fileName;
        m_lineNumber = lineNumber;
        m_functionName = functionName;
        m_memArea = memArea;
        m_address = address;
        m_length = length;
        m_opcode = opcode;
        m_opcodeArgs = opcodeArgs;
        m_instruction = instruction;
        m_instructionOffset = instructionOffset;
    }


    public int getMemArea() {
        return m_memArea;
    }

    
    public long getAddress() {
        return m_address;
    }


    public String getArgs() {
        return m_opcodeArgs;
    }

    
    public short getLength() {
        return m_length;
    }


    public String getFuntionName() {
        return m_functionName;
    }


    public String getInstruction() {
        return m_instruction;
    }


    public long getOffset() {
        return m_instructionOffset;
    }


    public String getOpcode() {
        return m_opcode;
    }


    public String getFileName() {
        return m_fileName;
    }


    public int getLineNumber() {
        return m_lineNumber;
    }
    
    @Override
    public String toString()
    {
            return String.format(" - %s.%s():%d [%x+%x:%d] '%s':'%s'(%s)\n", 
                    m_fileName, m_functionName, m_lineNumber,
                    m_address, m_instructionOffset, m_memArea,
                    m_instruction, m_opcode, m_opcodeArgs);
    }
    
}
