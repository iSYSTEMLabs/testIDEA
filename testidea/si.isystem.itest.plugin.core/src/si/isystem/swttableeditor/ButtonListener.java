package si.isystem.swttableeditor;

/**
 * This interface defines methods, which are called when user activates
 * one of table editing buttons, for example Up or Down buttons.
 */
public interface ButtonListener {

    /** 
     * Called, when the Up button is pressed.
     * @param rowIndex index of the row to be moved up
     * 
     * @return true, if move is allowed, false if move should be canceled
     */
    boolean upButtonPressed(int rowIndex);

    
    /** 
     * Called, when the Down button is pressed.
     * @param rowIndex index of the row to be moved down
     * 
     * @return true, if move is allowed, false if move should be canceled
     */
    boolean downButtonPressed(int rowIndex);
}
