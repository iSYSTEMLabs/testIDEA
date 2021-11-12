package si.isystem.python;

/**
 * This interface should be used by clients, who want to receive script output
 * immediately, not only when the script finishes.  
 * 
 * @author markok
 *
 */
public interface InStreamListener {

    void setLine(String text);
}
