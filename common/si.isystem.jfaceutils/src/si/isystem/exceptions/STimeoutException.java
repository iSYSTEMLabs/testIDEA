package si.isystem.exceptions;


public class STimeoutException extends SException {

    private static final long serialVersionUID = 1L;


    public STimeoutException() {
        super();
    }
    
    public STimeoutException(String s) {
        super(s);
    }
    
    public STimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public STimeoutException(Throwable cause) {
        super(cause);
    }

    public STimeoutException(String message, int index, int maxIndex) {
        super(message);
        add("index", index);
        add("maxIndex", maxIndex);
    }
    
    public STimeoutException(String message, int index, int minIndex, int maxIndex) {
        super(message);
        add("index", index);
        add("minIndex", minIndex);
        add("maxIndex", maxIndex);
    }
    
    public STimeoutException(String message, int index, int maxIndex, Throwable t) {
        super(message, t);
        add("index", index);
        add("maxIndex", maxIndex);
    }
    
    public STimeoutException(String message, int index, int minIndex, int maxIndex, Throwable t) {
        super(message, t);
        add("index", index);
        add("minIndex", minIndex);
        add("maxIndex", maxIndex);
    }
}
