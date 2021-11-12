package si.isystem.ui.utils;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;


/**
 * This class can be used as text editor in JFace TableViewer class.<br>
 * Example:
 * <pre>
            private IContentProposalProvider[] m_autoCompleters;
            ...
  
            int numColumns = <number of columns in the table>
            m_textCellEditors = new TextCellEditor[numColumns];
            for (int i = 0; i < numColumns; i++) {
                if (m_autoCompleters != null  &&  m_autoCompleters[i] != null) {
                    m_textCellEditors[i] = new TextCellEditorWithAutoCompletion(table, 
                                                                                m_autoCompleters[i], 
                                                                                null, 
                                                                                null);
                } else {
                    m_textCellEditors[i] = new TextCellEditor(table);
                }
            }
            
            m_tableViewer.setCellEditors(m_textCellEditors);
        
            ColumnViewerEditorActivationStrategy editorActivationStrategy = new
            ColumnViewerEditorActivationStrategy(m_tableViewer) { 
                // this method is required because of F2 editing activation key
                protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
                    return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL || 
                    event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION || 
                    (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && 
                            (event.keyCode == SWT.CR  ||  event.keyCode == SWT.F2)) || 
                            event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC; }
            };
 * </pre>
 * 
 * @see TableViewer
 * @see TextCellEditor
 * 
 * @author markok
 */
public class TextCellEditorWithAutoCompletion extends TextCellEditor {

    private ContentProposalAdapter m_contentProposalAdapter;
    private boolean m_popupOpen = false; // true, if pop-up is currently open

    /**
     * Creates cell editor. 
     */
    public TextCellEditorWithAutoCompletion(Composite parent, 
                                            IContentProposalProvider contentProposalProvider,
                                            KeyStroke keyStroke, 
                                            char[] autoActivationCharacters) {
        super(parent);

        enableContentProposal(contentProposalProvider, 
                              keyStroke, 
                              autoActivationCharacters);
    }

    
    private void enableContentProposal(IContentProposalProvider contentProposalProvider, 
                                       KeyStroke keyStroke,
                                       char[] autoActivationCharacters) {
        
        if (contentProposalProvider instanceof AsystContentProposalProvider) {
            
            AsystContentProposalProvider proposal = (AsystContentProposalProvider) contentProposalProvider;
            if (proposal.getProposalsAcceptanceStyle() == ContentProposalAdapter.PROPOSAL_INSERT) {
                m_contentProposalAdapter = new ContentProposalAdapter(text, 
                                                                    new AsystTextContentAdapter(),
                                                                    contentProposalProvider, 
                                                                    keyStroke, 
                                                                    autoActivationCharacters);
            } else {
                m_contentProposalAdapter = new ContentProposalAdapter(text, 
                                                                    new TextContentAdapter(),
                                                                    contentProposalProvider, 
                                                                    keyStroke, 
                                                                    autoActivationCharacters);
            }
            
            m_contentProposalAdapter.setProposalAcceptanceStyle(proposal.getProposalsAcceptanceStyle());
            
        } else {
            
            m_contentProposalAdapter = new ContentProposalAdapter(text, 
                                                                new TextContentAdapter(),
                                                                contentProposalProvider, 
                                                                keyStroke, 
                                                                autoActivationCharacters);
            
            m_contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
        }
        
        m_contentProposalAdapter.setPropagateKeys(true);

        // Listen for popup open/close events to be able to handle focus events correctly
        m_contentProposalAdapter.addContentProposalListener(new IContentProposalListener2() {

            @Override
            public void proposalPopupClosed(ContentProposalAdapter adapter) {
                m_popupOpen = false;
            }

            @Override
            public void proposalPopupOpened(ContentProposalAdapter adapter) {
                m_popupOpen = true;
            }
        });
    }

    
    /**
     * Return the {@link ContentProposalAdapter} of this cell editor.
     *
     * @return the {@link ContentProposalAdapter}
     */
    public ContentProposalAdapter getContentProposalAdapter() {
        return m_contentProposalAdapter;
    }

    @Override
    protected void focusLost() {
        if (!m_popupOpen) {
            // Focus lost deactivates the cell editor.
            // This must not happen if focus lost was caused by activating
            // the completion proposal popup.
            super.focusLost();
        }
    }

    @Override
    protected boolean dependsOnExternalFocusListener() {
        // Always return false;
        // Otherwise, the ColumnViewerEditor will install an additional focus listener
        // that cancels cell editing on focus lost, even if focus gets lost due to
        // activation of the completion proposal popup. See also bug 58777.
        return false;
    }

    /**
     * This method is implemented here, because when autocompletion is used,
     * ESC should only close the auto completion box, not also cancel the new 
     * value in table cell.
     */
    @Override
    protected void keyReleaseOccured(KeyEvent keyEvent) {
        if (keyEvent.character != SWT.ESC) { // ignore Escape key
            super.keyReleaseOccured(keyEvent);
        }
    }
}
