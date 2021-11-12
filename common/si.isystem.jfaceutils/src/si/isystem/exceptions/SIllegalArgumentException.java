package si.isystem.exceptions;

public class SIllegalArgumentException extends SException {

    private static final long serialVersionUID = 1L;


    public SIllegalArgumentException() {
        super();
    }
    
    public SIllegalArgumentException(String s) {
        super(s);
    }
    
    public SIllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public SIllegalArgumentException(Throwable cause) {
        super(cause);
    }

    public SIllegalArgumentException(String message, String argName, Object argValue) {
        super(message);
        add("argument", argName);
        add("value", argValue);
    }
    
    public SIllegalArgumentException(String message, String argName, Object argValue, Object expected) {
        super(message);
        add("argument", argName);
        add("value", argValue);
        add("expected", expected);
    }
    
    public SIllegalArgumentException(String message, String argName, Object argValue, Throwable t) {
        super(message, t);
        add("argument", argName);
        add("value", argValue);
    }
    
    public SIllegalArgumentException(String message, String argName, Object argValue, Object expected, Throwable t) {
        super(message, t);
        add("argument", argName);
        add("value", argValue);
        add("expected", expected);
    }
    
    
}
