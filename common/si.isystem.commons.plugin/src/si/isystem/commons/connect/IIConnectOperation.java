package si.isystem.commons.connect;


/**
 * This interface should be implemented by classes, which perform operation
 * vie isystem.connect. 
 *  
 * @author markok
 *
 */
public interface IIConnectOperation {

    /** This method is called when successfully connected. */ 
    void exec(JConnection jcmgr);

    
    /**
     * Method for setting operation parameters, for example whether show source 
     * button should display function or file source.
     */
    void setData(Object data);
    
}
