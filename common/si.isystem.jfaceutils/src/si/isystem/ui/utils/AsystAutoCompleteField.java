// package si.isystem.ui.utils;

// import org.eclipse.jface.bindings.keys.KeyStroke;

/**
 * This class attempts to auto-complete a user's
 * keystrokes by activating a popup that filters a list of proposals according
 * to the content typed by the user. Its main difference to AutoCompleteField
 * is possibility to add descriptions and to specify proposalAcceptanceStyle.
 * 
 * @see AsystContentProposalProvider
 * 
 * @since 3.3
 */
//public class AsystAutoCompleteField {
//
//    private AsystContentProposalProvider m_proposalProvider;
//
//    // Uncomment keystroke and activation chars to have Eclipse-like behavior - no
//    // proposals pop-up by default, but only on request (Ctrl + Space) or activation
//    // char (. or > (for C pointer deref ->)).
//    private static final KeyStroke m_activateionKeyStroke = null; 
//                                    // KeyStroke.getInstance(SWT.CTRL, SWT.SPACE);
//
//    private static final char [] m_activationChars = null; //new char[]{'.', '>'};
    /**
     * Construct a CustomAutoComplete field on the specified control, whose
     * completions are characterized by the specified array of Strings.
     * Example:
     * <pre>
     *   m_autocompleter = new CustomAutoCompleteField(m_comboBox, 
     *                                                 new ComboContentAdapter(), 
     *                                                 contents,
     *                                                 descriptions,
     *                                                 ContentProposalAdapter.PROPOSAL_REPLACE); 
     * </pre>
     * 
     * @param control
     *            the control for which autocomplete is desired, for example Text. May not be
     *            <code>null</code>.
     * @param controlContentAdapter
     *            the <code>IControlContentAdapter</code> used to obtain and
     *            update the control's contents. May not be <code>null</code>.
     * @param proposals
     *            the array of Strings representing valid content proposals for
     *            the field.
     * @param proposalAcceptanceStyle a constant indicating how an accepted proposal should affect
     *            the control's content. Should be one of
     *            <code>ContentProposalAdapter.PROPOSAL_INSERT</code>, 
     *            <code>ContentProposalAdapter.PROPOSAL_REPLACE</code>,
     *            or <code>ContentProposalAdapter.PROPOSAL_IGNORE</code>
    public AsystAutoCompleteField(Control control,
                                   IControlContentAdapter controlContentAdapter, 
                                   String[] proposals,
                                   String[] descriptions,
                                   int proposalAcceptanceStyle) {
        
        m_proposalProvider = new AsystSimpleContentProposal(proposals, descriptions);
        m_proposalProvider.setFiltering(true);
        AsystContentProposalAdapter adapter = new AsystContentProposalAdapter(control, 
                                                                    controlContentAdapter,
                                                                    m_proposalProvider,
                                                                    m_activateionKeyStroke, 
                                                                    m_activationChars);
        adapter.setPropagateKeys(true);
        adapter.setProposalAcceptanceStyle(proposalAcceptanceStyle);
    }
     */

/*    
    public AsystAutoCompleteField(Control control,
                                  IControlContentAdapter controlContentAdapter,
                                  AsystSimpleContentProposal proposalProvider,
                                  int proposalAcceptanceStyle) {
       
       m_proposalProvider = proposalProvider;
       m_proposalProvider.setFiltering(true);
       AsystContentProposalAdapter adapter = new AsystContentProposalAdapter(control, 
                                                                   controlContentAdapter,
                                                                   m_proposalProvider,
                                                                   m_activateionKeyStroke,
                                                                   m_activationChars);
       adapter.setPropagateKeys(true);
       adapter.setProposalAcceptanceStyle(proposalAcceptanceStyle);
       // adapter.setFilterStyle(ContentProposalAdapter.FILTER_CHARACTER);
       // adapter.setProposalPopupFocus();
   }
*/
   
    /**
     * Set the Strings to be used as content proposals.
     * 
     * @param proposals
     *            the array of Strings to be used as proposals.
     * @param descriptions text to show in pop-up. May be null.
    public void setProposals(String[] proposals, String[] descriptions) {
        m_proposalProvider.setProposals(proposals, descriptions);
    }
     */
// }
