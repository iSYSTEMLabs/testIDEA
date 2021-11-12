package si.isystem.exceptions;


public class SIllegalStateException extends SException {

    private static final long serialVersionUID = 1L;


    public SIllegalStateException() {
        super();
    }
    
    public SIllegalStateException(String s) {
        super(s);
    }
    
    public SIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public SIllegalStateException(Throwable cause) {
        super(cause);
    }

    public SIllegalStateException(String message, String argName, Object argValue) {
        super(message);
        add("state", argName);
        add("value", argValue);
    }
    
    public SIllegalStateException(String message, String argName, Object argValue, Object expected) {
        super(message);
        add("state", argName);
        add("value", argValue);
        add("expected", expected);
    }
    
    public SIllegalStateException(String message, String argName, Object argValue, Throwable t) {
        super(message, t);
        add("state", argName);
        add("value", argValue);
    }
    
    public SIllegalStateException(String message, String argName, Object argValue, Object expected, Throwable t) {
        super(message, t);
        add("state", argName);
        add("value", argValue);
        add("expected", expected);
    }
}
