package si.isystem.swttableeditor;

/**
 * This interface should be implemented by classes, vhich verify consistency of
 * data entered by user in ITableEditorRowDialog.
 * 
 * @author markok
 *
 */
public interface ITextFieldVerifier {

    /**
     * Verifies user entries.
     * 
     * @param data strings entered by user to dialog controls
     * @return null if OK, string with error description otherwise
     */
    String verify(String[] data);
    
    /**
     * Formats user entries. Typical case is removing redundant spaces, formatting 
     * paths, ....
     * 
     * @param data array of strings to be formatted
     * 
     * @return null if there was no error, error description in case of error
     */
    String format(String[] data);
}
