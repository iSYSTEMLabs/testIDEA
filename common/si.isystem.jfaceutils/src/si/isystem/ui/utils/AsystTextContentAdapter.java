package si.isystem.ui.utils;

import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import si.isystem.exceptions.SIllegalArgumentException;

/**
 * No longer deprecated - the approach used in ContentProposalProvider (making
 * proposals contain only the missing part of the typed word) does not work with
 * proposals which contain the typed sequence of chars, not only begin with it. 
 * For example, it user types 'ch', and 'g_char1' is proposed, then ch should be 
 * REPLACED by 'g_char1', not only 'ar1' appended to it. Replacement is what this 
 * class does, appending is what ContentProposalProvider can do.
 * 
 * Used to be Deprecated: this functionality has been implemented in ContentProposalProvider.
 * 
 * Original comment:
 * This class handles insertion of text proposed by content proposal into control.
 * It replaces the already entered text with the string from content proposal.
 * For example, if the user entered 'fu', and then selects 'function' from 
 * content proposal, 'fu' is replaced by 'function'. By default 'function' is
 * simply appended, but 'fufunction' is not what the user expects.
 * 
 * @author markok
 *
 */
public class AsystTextContentAdapter extends TextContentAdapter {

    @Override
    public void insertControlContents(Control control, String insertedText,
                                      int cursorPosition) {

        if (insertedText.isEmpty()) {
            // do not replace last word with empty text - this is required to support 
            // functionality provided by m_isMakeDefaultSelectionInContentProposals 
            // in UiPrefsPage - empty item, which is added as first content proposal 
            // should be ignored.
            return; 
        }
        
        Point selection = getSelectionCoord(control);
        
        String existingText = getText(control);
        int indexOfLastWord = UiTools.getStartOfLastWord(existingText, selection.x);
        
        String textBeforeCurrentWord = existingText.substring(0, indexOfLastWord);
        String textAfterCurrentWord = existingText.substring(selection.x);

        String newText = textBeforeCurrentWord + insertedText + textAfterCurrentWord;
        // Position cursor at the end of inserted text.
        int newCursorPos = textBeforeCurrentWord.length() + insertedText.length();

        if (control instanceof Text) {
            
            Text txtControl = (Text) control;
            txtControl.setText(newText);
            if (newCursorPos <= newText.length()) {
                txtControl.setSelection(newCursorPos, newCursorPos);
            }
        } else {
            Combo comboControl = (Combo) control;
            comboControl.setText(newText);
            if (newCursorPos <= newText.length()) {
                selection = new Point(newCursorPos, newCursorPos);
                comboControl.setSelection(selection);
            }
        }
    }
    
    
    @Override
    public void setControlContents(Control control, String insertedText,
                                      int cursorPosition) {
        if (insertedText.isEmpty()) {
            // do not replace last word with empty text - this is required to support 
            // functionality provided by m_isMakeDefaultSelectionInContentProposals 
            // in UiPrefsPage - empty item, which is added as first content proposal 
            // should be ignored.
            return; 
        }
        
        super.setControlContents(control, insertedText, cursorPosition);
    }
    
    
    private Point getSelectionCoord(Control control) {
        if (control instanceof Text) {
            return ((Text)control).getSelection();
        } else if (control instanceof Combo) {
            return ((Combo)control).getSelection();
        } 
        
        throw new SIllegalArgumentException("Invalid control - should be Text or Combo "
                + "but it is: " + control.getClass().getSimpleName());
    }
    
    
    private String getText(Control control) {
        if (control instanceof Text) {
            return ((Text)control).getText();
        } else if (control instanceof Combo) {
            return ((Combo)control).getText();
        } 
        
        throw new SIllegalArgumentException("Invalid control - should be Text or Combo "
                + "but it is: " + control.getClass().getSimpleName());
    }
    

    @SuppressWarnings("unused")
	private void setText(Control control, String text) {
        if (control instanceof Text) {
            ((Text)control).setText(text);
        } else if (control instanceof Combo) {
            ((Combo)control).setText(text);
        } 
        
        throw new SIllegalArgumentException("Invalid control - should be Text or Combo "
                + "but it is: " + control.getClass().getSimpleName());
    }
    
    
    @SuppressWarnings("unused")
    private void setSelection(Control control, int start, int end) {
        if (control instanceof Text) {
            ((Text)control).setSelection(start, end);
        } else if (control instanceof Combo) {
            ((Combo)control).setSelection(new Point(start, end));
        } 
        
        throw new SIllegalArgumentException("Invalid control - should be Text or Combo "
                + "but it is: " + control.getClass().getSimpleName());
    }
}
