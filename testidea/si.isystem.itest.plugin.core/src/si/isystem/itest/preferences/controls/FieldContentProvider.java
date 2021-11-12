package si.isystem.itest.preferences.controls;

/**
 * Classes implementing this interface are used when user presses button in
 * PrefStringButtonEditor. Normally they open a dialog and return user supplied 
 * data.
 *    
 * @author markok
 */
public interface FieldContentProvider {

    /**
     * @return string to be used for control setting, or null if there should be
     * no change, for example if user canceled the dialog.
     */
    String getValue();
}
