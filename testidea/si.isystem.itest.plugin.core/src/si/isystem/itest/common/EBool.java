package si.isystem.itest.common;

import si.isystem.connect.ETristate;
import si.isystem.exceptions.SIllegalArgumentException;

/**
 * This class contains utility methods for ETristate, which represents boolean 
 * with three values - true, false, and 
 * default. The 'default' value may mean true or false, depending on the 
 * application. In serialized format it is usually represented as not set
 * value.
 *  
 * @author markok
 *
 */
public class EBool {
    // @deprecated - to be replaced by ETristate from C++ lib.
    // make toString() methods return strings as expected by iconnect
    // EFalse {public String toString() { return "false";}},
    // ETrue {public String toString() { return "true";}},
    // EDefault {public String toString() { return "";}};
    
    /**
     * This m. converts the given string to enumeration value according to the 
     * following rules:
     * - if strBool is empty string or null, Default is returned
     * - if strBool is 'true', or '1', or 'yes', True is returned
     * - if strBool is 'false', or '0', or 'no', False is returned
     * - exception is thrown for any other string contents
     * 
     * @throws SIllegalArgumentException if strBool has invalid value
     */
    public static ETristate strToTristate(String strBool) {
        if (strBool == null  ||  strBool.isEmpty()) {
            return ETristate.E_DEFAULT;
        }
        
        if (strBool.equals("true")  ||  strBool.equals("1")  || strBool.equals("yes")) {
            return ETristate.E_TRUE;
        }
        
        if (strBool.equals("false")  ||  strBool.equals("0")  || strBool.equals("no")) {
            return ETristate.E_FALSE;
        }
        
        throw new SIllegalArgumentException("Invalid boolean value! Should be one of 'true', " +
        		"'false', '0', '1', 'yes', or 'no'!").add("value", strBool);
    }
    
    
    public static String tristate2Str(ETristate value) {
        switch (value) {
        case E_DEFAULT:
            return "";
        case E_FALSE:
            return "false";
        case E_TRUE:
            return "true";
        default:
            throw new SIllegalArgumentException("Invalid boolean value! Should be true, " +
                    "false, or default!").add("value", value);
        }
    }
    
    
    public static String tristate2Str(boolean value, boolean defaultValue) {
            if (value == defaultValue) {
                return "";
            }

            return value ? "true" : "false";
    }
    
    
    /** Returns true or false if value is True or False respectively, throws
     * exception on Default value!
    public boolean bool2Value() {
        if (this == True) {
            return true;
        }
        if (this == False) {
            return false;
        }
        throw new SIllegalStateException("Enum should value True or False for successful conversion to boolean!");
    }
     */
}
