package si.isystem.swttableeditor;


/**
 * If table does not support adding of empty rows, then a dialog is
 * recommended for adding a row. The class implementing the dialg should
 * implement this interface. 
 * 
 * @see TableEDitorPanel#setAddDialog
 * @author markok
 */
public interface ITableEditorRowDialog {

    /**
     * Displays the dialog.
     * 
     * @return true, if user pressed the OK button. 
     */
    boolean show();
    
    /** Returns data from dialog, one GUI component is stored to one element in array. */
    String []getData();

    /** Sets object, which verifies dialog data. */
    void setVerifier(ITextFieldVerifier verifier);
}
