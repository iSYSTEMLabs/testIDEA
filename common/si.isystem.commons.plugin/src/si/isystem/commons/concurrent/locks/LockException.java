package si.isystem.commons.concurrent.locks;

public class LockException extends Exception 
{
    private static final long serialVersionUID = 5199195006149366871L;
    
    public LockException(String msg) {
        super(msg);
    }
}
