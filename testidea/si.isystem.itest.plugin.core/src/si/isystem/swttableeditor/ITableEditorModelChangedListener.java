package si.isystem.swttableeditor;


public interface ITableEditorModelChangedListener {

    enum ChangeType {/** 
                      * Meaning of parameters of method modelChanged(): 
                      * 'idx1' identifies the index of inserted row, idx2 is index of column,
                      * 'row' is the old row before modification.  
                      */
                     CELL_CHANGED, 
                     /** 
                      * Meaning of parameters of method modelChanged(): 
                      * 'idx1' identifies the index of inserted row, idx2 is not used,
                      * 'row' is the inserted row.  
                      */
                     ROW_ADDED,
                     /** 
                      * Meaning of parameters of method modelChanged(): 
                      * 'idx1' identifies the first row, idx2 is not used,
                      * 'row' is the removed row.  
                      */
                     ROW_REMOVED, 
                     /** 
                      * Meaning of parameters of method modelChanged(): 
                      * 'idx1' identifies the first row, idx2 identifies the  
                      * second row swapped, 'row' is not used. 
                      */
                     ROWS_SWAPPED};
    
    void modelChanged(ChangeType changeType, int rowIndex, int columnIndex, 
                      ITableEditorRow row);
}
