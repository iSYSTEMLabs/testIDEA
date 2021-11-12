package si.isystem.mk.utils;

import org.eclipse.jface.dialogs.IInputValidator;

/**
 * This class validates a String. It makes sure that the String contains
 * itneger number.
 */
public class IntegerValidator implements IInputValidator {
    /**
     * Validates the String. Returns null for no error, or an error message.
     * 
     * @param newText the String to validate
     * @return String
     */
    @Override
    public String isValid(String newText) {

        newText = newText.trim();
        
        try {
            Integer.parseInt(newText);
        } catch (Exception ex) {
            return "The entered text is not an integer.";
        }

        return null;  // input is OK
    }
}
