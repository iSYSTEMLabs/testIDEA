package si.isystem.itest.run;

import java.util.List;

import si.isystem.python.ScriptResult;

/**
 * This class contains the following data obtained from script function execution:
 * 
 * Note: There is no stderr, because it means execution error and terminates the
 *       test earlier.
 *       
 * @author markok
 */
public class TestScriptResult {

    private String m_funcType;
    
//    List<String> m_stdout;  // only written to status view, not stored to test 
//                            // results, can be used for debugging
    List<String> m_funcInfo; // information (some measurement for example) that
                             // should be stored in test report. It is stored to
                             // object attribute with reserved name
//    List<String> m_funcRetVal; // function return value - if not empty, there was
//                               // some unexpected result on the target
    String m_metaData = "";  // if not null stores meta info about script 
                             // function, for example name of the stubbed function
    
    private ScriptResult m_scriptResult;
    
    public TestScriptResult(String funcType,
                            ScriptResult result,
                            List<String> funcInfo) {
        m_funcType = funcType;
        m_scriptResult = result;
        m_funcInfo = funcInfo;
    }

    public String getFuncType() {
        return m_funcType;
    }

    public ScriptResult getScriptResult() {
        return m_scriptResult;
    }
    
    
    public List<String> getStdout() {
        return m_scriptResult.getStdout();
    }

    
    public List<String> getFuncRetVal() {
        return m_scriptResult.getFuncRetVal();
    }

    
    /**
     * @return value of reserved variable set in script method, for example '_isys_tableInfo'.
     */
    public List<String> getFuncInfo() {
        return m_funcInfo;
    }

    
    public String getMetaData() {
        return m_metaData;
    }

    
    public void setMetaData(String metaData) {
        m_metaData = metaData;
    }

    
    /**
     * By definition testIDEA script extension function should return None, if
     * there was no error, error description as string otherwise. Any output
     * other than prompt on stderr also means error. 
     */
    public boolean isError() {
        return m_scriptResult.isError()  ||  m_scriptResult.isFunctionReturendValue();
    }
    
    
    public String toUIString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  Script function type: ").append(m_funcType.toString()).append('\n');
        if (m_metaData != null  &&  !m_metaData.isEmpty()) {
            sb.append("  Metadata: [");
            sb.append(m_metaData);
            sb.append("]\n");
        } 
        
        sb.append(m_scriptResult.toUIString());
        
        if (m_funcInfo != null  &&  !m_funcInfo.isEmpty()) {
            sb.append("\n  Script info:");
            for (String item : m_funcInfo) {
                sb.append("\n      ").append(item);
            }
            sb.append("\n");
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n    Script function type: ").append(m_funcType.toString()).append('\n');
        sb.append("    Metadata: [");
        if (m_metaData != null) {
            sb.append(m_metaData);
            sb.append("]");
        } else {
            sb.append("<null>]");
        }
        
        sb.append("\n    ]");
        sb.append(m_scriptResult.toString());
        sb.append("\n    ]");
        
        sb.append("\n    Script info: [");
        if (m_funcInfo != null  &&  !m_funcInfo.isEmpty()) {
            for (String item : m_funcInfo) {
                sb.append("\n      ").append(item);
            }
        }
        sb.append("\n    ]");
        
        sb.append('\n');
        
        return sb.toString();
    }
}
