package si.isystem.exceptions;

public class SIOException extends SException {

    private static final long serialVersionUID = 1L;


    public SIOException() {
        super();
    }
    
    public SIOException(String s) {
        super(s);
    }
    
    public SIOException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public SIOException(Throwable cause) {
        super(cause);
    }

    public SIOException(String message, int index, int maxIndex) {
        super(message);
        add("index", index);
        add("maxIndex", maxIndex);
    }
    
    public SIOException(String message, int index, int minIndex, int maxIndex) {
        super(message);
        add("index", index);
        add("minIndex", minIndex);
        add("maxIndex", maxIndex);
    }
    
    public SIOException(String message, int index, int maxIndex, Throwable t) {
        super(message, t);
        add("index", index);
        add("maxIndex", maxIndex);
    }
    
    public SIOException(String message, int index, int minIndex, int maxIndex, Throwable t) {
        super(message, t);
        add("index", index);
        add("minIndex", minIndex);
        add("maxIndex", maxIndex);
    }
}
