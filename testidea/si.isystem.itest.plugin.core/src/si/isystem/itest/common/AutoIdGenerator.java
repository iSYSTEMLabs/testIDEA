package si.isystem.itest.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.StrVector;
import si.isystem.connect.connectJNI;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.data.HostVarsUtils;

/**
 * This class contains data and methods common to all classes, which deal with 
 * automatic setting of test IDs. 
 *  
 * @author markok
 *
 */
public class AutoIdGenerator {

    // this var is used to make IDs unique even if generated inside the same sys 
    // timer interval
    private int m_testCounter = 0;
    private int m_baseTestCounter = 0;

    
    public AutoIdGenerator() {
    }
    

    public void setTestCounter(int counter) {
        m_testCounter = counter;
        m_baseTestCounter = counter;
    }
    
    
    /**
     * Replaces variables of format ${<varName>} with strings in the given mapping.
     * 
     * @param format
     * @param variables
     * @return
     */
    public String createTestId(String format, Map<String, String> variables) {

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            format = format.replace(entry.getKey(), entry.getValue());
        }
        
        if (format.contains("${")) {
            throw new SIllegalArgumentException("Unknown variable in format string!\n"
                    + "Please note, that since version 9.12.234 all reserved host variables\n"
                    + "must start with underscore ('_') character.").
                    add("createdTestId", format);
        }
        
        return format;
    }
    
    
    public Map<String, String> createVars(CTestSpecification testSpec, 
                                          int derivedTestIdx, 
                                          String nidPrefix,
                                          String didPrefix) {
        
        TreeMap<String, String> idVars = new TreeMap<String, String>();
        idVars.put(HostVarsUtils.$_UUID, UUID.randomUUID().toString());        
        idVars.put(HostVarsUtils.$_UID, connectJNI.getUID());
        
        if (nidPrefix.isEmpty()) {
            idVars.put(HostVarsUtils.$_NID, connectJNI.getUID());
            idVars.put(HostVarsUtils.$_DID, String.valueOf(m_baseTestCounter++));
        } else {
            idVars.put(HostVarsUtils.$_NID, nidPrefix + "." + derivedTestIdx);
            idVars.put(HostVarsUtils.$_DID, didPrefix + "." + derivedTestIdx);
        }
        
        idVars.put(HostVarsUtils.$_SEQ, String.valueOf(m_testCounter++));        

        CTestSpecification mergedTs = testSpec.merge();
        String coreId = mergedTs.getCoreId();
        coreId = TestSpecificationModel.getActiveModel().getConfiguredCoreID(coreId);
        idVars.put(HostVarsUtils.$_CORE_ID, replaceInvalidChars(coreId));
        
        idVars.put(HostVarsUtils.$_FUNCTION, replaceInvalidChars(mergedTs.getFunctionUnderTest(true).getName()));
        
        StrVector params = new StrVector();
        mergedTs.getPositionParams(params);
        idVars.put(HostVarsUtils.$_PARAMS, list2IdFmt(params));
        
        StrVector tags = new StrVector();
        mergedTs.getTags(tags);
        idVars.put(HostVarsUtils.$_TAGS, list2IdFmt(tags));        

        return idVars;
    }
    
    
    public String list2IdFmt(StrVector params) {
        StringBuilder sb = new StringBuilder();
        
        int numParams = (int)params.size();
        for (int i = 0; i < numParams; i++) {
            if (sb.length() > 0) {
                sb.append(CTestHostVars.LIST_SEPARATOR);
            }
            String param = params.get(i);
            param = replaceInvalidChars(param);
            sb.append(param);
        }
        
        return sb.toString();
    }


    private String replaceInvalidChars(String param) {
        // see also CYAMLReceivers.h, CLimitedScalarValidator
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < param.length(); i++) {
            Character c = param.charAt(i);
            
            // the '/' char is also replaced by '_', so that it does not
            // appear in qualified function names, where it can be part of module path,
            // or in string parameters. This way it can be used for uid separator.
            if (CYAMLUtil.isAllowedCharForTestId(c)  &&  c != '/') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        
        return sb.toString();
    }
 
 /*   
    static int m_uidCounter = 0;
    static long m_oldTimeMillis = 0;
    static final int COUNTER_BITS = 12;
    
    private String getUID() {
        // in about 100 years System.currentTimeMillis() will return a 42 bit number,
        // so we have more than 20 bits left. If we'd use 10 bits only, we could generate 1024
        // IDs per second, which is important when generating IDs for all tests in one run.
        
        long timeMillis = System.currentTimeMillis();
        
        while (timeMillis == m_oldTimeMillis  &&  m_uidCounter >= (Math.pow(2, COUNTER_BITS) - 2)) {
            timeMillis = System.currentTimeMillis(); // wait for the next millisecond(s)
        }
        
        long uid = timeMillis << COUNTER_BITS;
        if (timeMillis == m_oldTimeMillis) {
            uid += ++m_uidCounter;
        } else {
            m_uidCounter = 0;
        }
        
        m_oldTimeMillis = timeMillis;
        
        // System.out.println("uid = " + uid + "   counter = " + m_uidCounter);
        // use MAX_RADIX to make strings shorter
        return Long.toString(uid, Character.MAX_RADIX);
    }
    
    */
}
