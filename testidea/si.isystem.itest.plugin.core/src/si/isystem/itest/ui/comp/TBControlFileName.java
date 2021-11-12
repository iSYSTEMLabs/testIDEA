package si.isystem.itest.ui.comp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This class creates text input field with Browse button next to it.
 * 
 * @author markok
 *
 */
public class TBControlFileName {

    private TBControlText m_textField;
    private Button m_browseBtn;

    
    public TBControlFileName(KGUIBuilder builder, 
                             String buttonText, 
                             String tooltip,
                             int section, 
                             String textFieldLayoutData,
                             String buttonLayoutData,
                             ENodeId nodeId,
                             final String browseDialogTitle, 
                             final String []filterExtensions,
                             boolean isAutoAddExtension,
                             TBControl.EHControlId controlId, 
                             int swtSaveOrSwtOpenStyle) {
    
        m_textField = TBControlText.createForMixed(builder, 
                                                   tooltip, 
                                                   textFieldLayoutData, 
                                                   section, 
                                                   nodeId, 
                                                   controlId, 
                                                   SWT.BORDER);
        
        m_browseBtn = builder.createBrowseButton(buttonText,
                                                 buttonLayoutData,
                                                 browseDialogTitle,
                                                 filterExtensions,
                                                 (Text)m_textField.getControl(),
                                                 isAutoAddExtension,
                                                 swtSaveOrSwtOpenStyle);
    }


    public TBControlText getTextField() {
        return m_textField;
    }


    public Button getBrowseBtn() {
        return m_browseBtn;
    }

    
    
}
