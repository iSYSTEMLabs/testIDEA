package si.isystem.exceptions;

public class SIndexOutOfBoundsException extends SException {

    private static final long serialVersionUID = 1L;


    public SIndexOutOfBoundsException() {
        super();
    }
    
    public SIndexOutOfBoundsException(String s) {
        super(s);
    }
    
    public SIndexOutOfBoundsException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public SIndexOutOfBoundsException(Throwable cause) {
        super(cause);
    }

    public SIndexOutOfBoundsException(String message, int index, int maxIndex) {
        super(message);
        add("index", index);
        add("maxIndex", maxIndex);
    }
    
    public SIndexOutOfBoundsException(String message, int index, int minIndex, int maxIndex) {
        super(message);
        add("index", index);
        add("minIndex", minIndex);
        add("maxIndex", maxIndex);
    }
    
    public SIndexOutOfBoundsException(String message, int index, int maxIndex, Throwable t) {
        super(message, t);
        add("index", index);
        add("maxIndex", maxIndex);
    }
    
    public SIndexOutOfBoundsException(String message, int index, int minIndex, int maxIndex, Throwable t) {
        super(message, t);
        add("index", index);
        add("minIndex", minIndex);
        add("maxIndex", maxIndex);
    }
    
}
