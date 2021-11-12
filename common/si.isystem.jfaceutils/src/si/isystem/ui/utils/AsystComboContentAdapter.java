package si.isystem.ui.utils;

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.swt.widgets.Control;

public class AsystComboContentAdapter extends ComboContentAdapter{

    private AsystTextContentAdapter m_contentAdapter = new AsystTextContentAdapter();
    
    @Override
    public void insertControlContents(Control control, 
                                      String insertedText,
                                      int cursorPosition) {
        
        m_contentAdapter.insertControlContents(control, insertedText, cursorPosition);
    }

    
    @Override
    public void setControlContents(Control control, String insertedText,
                                      int cursorPosition) {
        
        // Can not use m_contentAdapter here, because it calls method in super. 
        if (insertedText.isEmpty()) {
            // do not replace last word with empty text - this is required to support 
            // functionality provided by m_isMakeDefaultSelectionInContentProposals 
            // in UiPrefsPage - empty item, which is added as first content proposal 
            // should be ignored.
            return; 
        }
        
        super.setControlContents(control, insertedText, cursorPosition);
    }
}
