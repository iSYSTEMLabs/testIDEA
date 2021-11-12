package de.kupzog.ktable;

public interface ICommandListener {

    void insertColumn();
    void insertRow();
    void deleteColumn();
    void deleteRow();
    
    /**
     * Keys Ctrl and Num Plus have been pressed. It is up to listener what
     * happens, but normally it should add either row or colum. This method
     * should be implemented by tables, where only rows or column may be added,
     * so column or row selection mode is not required. For example, if only 
     * rows may be added, then row can be added on Ctrl-Num+ even if single cell 
     * is selected.
     */
    void ctrlPlusPressed();
    
    /**
     * Should be used for deleting rows.
     * @see ctrlPlusPressed
     */
    void ctrlMinusPressed();
}
