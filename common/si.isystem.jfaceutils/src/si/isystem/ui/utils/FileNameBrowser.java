package si.isystem.ui.utils;

import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;


/**
 * This class merges two controls often used together when users have to enter 
 * file name - a text input field and a browse button. This way it is easier to 
 * pass both controls between methods and both of them can be enabled/disabled 
 * with a single method call.
 * 
 * @author markok
 * @see KGUIBuilder#createFileNameInput(String, Object, Object, String, String[], boolean, int)
 *
 */
public class FileNameBrowser {

    private Text m_inputField;
    private Button m_button;

    /**
     * Use KGUIBuilder.createFileNameInput() to create instance of this class.
     * 
     * @param inputField text input field
     * @param button browse button
     */
    public FileNameBrowser(Text inputField, Button button) {
        m_inputField = inputField;
        m_button = button;
    }

    
    /** Enables/disables both controls. */
    public void setEnabled(boolean isEnabled) {
        m_inputField.setEnabled(isEnabled);
        m_button.setEnabled(isEnabled);
    }
    

    /** Sets text in the input field. */
    public void setText(String text) {
        m_inputField.setText(text);
    }
    
    
    /** Sets tooltip text of the input field. */
    public void setToolTipText(String tooltip) {
        UiTools.setToolTip(m_inputField, tooltip);
    }
    

    /** Returns current text from the input field. */
    public String getText() {
        return m_inputField.getText();
    }

    
    /** Adds focus listener to the input field. */
    public void addFocusListener(FocusListener listener) {
        m_inputField.addFocusListener(listener);
    }


    /** Adds key listener to the input field. */
    public void addKeyListener(KeyListener listener) {
        m_inputField.addKeyListener(listener);
    }

    
    /** Returns the input field. */
    public Text getInputField() {
        return m_inputField;
    }

    
    /** Returns the browse button. */
    public Button getButton() {
        return m_button;
    }
}
