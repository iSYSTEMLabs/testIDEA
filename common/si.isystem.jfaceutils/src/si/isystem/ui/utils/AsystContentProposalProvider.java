package si.isystem.ui.utils;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;


/**
 * This class maps a static list of Strings to content proposals. Its main 
 * difference to org.eclipse.jface.fieldassist.SimpleContentProposalProvider
 * is possibility to specify descriptions, which are displayed in a tool-tip
 * like box next to selected item in drop-down box, and better behavior
 * in INSERT mode - only missing part of the word is added.
 *
 * This class replaces also AsystTextContentAdapter, because it creates
 * proposals which finish the currently entered string, for example, is
 * user typed 'g_c', proposal returned is 'har1' to complete string to 'g_char1'.
 * 
 * @see IContentProposalProvider
 */
public class AsystContentProposalProvider implements IContentProposalProvider {

    /* This field is used as data holder when passing this class to TableAdapter. */ 
    private int m_proposalsAcceptanceStyle = ContentProposalAdapter.PROPOSAL_REPLACE;
    

    /*
     * The proposals provided.
     */
    private String[] m_proposals;

    /*
     * The proposals mapped to IContentProposal. Cached for speed in the case
     * where filtering is not used.
     */
    private IContentProposal[] m_contentProposals;

    /*
     * Boolean that tracks whether filtering is used.
     */
    private boolean m_filterProposals = false;

    private String[] m_descriptions;


    /**
     * This setting is usually application wide, so it is OK to set it once
     * for all instances. This way it is also possible to set it immediately for 
     * all existing instances.
     */
    private static boolean m_isMakeDefaultSelectionInContentProposals = true;


    /**
     * Creates content proposals provider with empty proposals. They can be set 
     * later with setProposals().
     */
    public AsystContentProposalProvider() {
        this(new String[0], null);
    }
    /**
     * Construct a SimpleContentProposalProvider whose content proposals are
     * always the specified array of Objects.
     * 
     * @param proposals
     *            the array of Strings to be returned whenever proposals are
     *            requested.
     * @param descriptions descriptions, which show in tooltip next to selected item.
     * May be null or shorter than proposals - missing descriptions are not shown.
     */
    public AsystContentProposalProvider(String[] proposals, 
                                        String[]descriptions) {
        super();
        m_proposals = proposals;
        m_descriptions = descriptions;
    }

    /**
     * Return an array of Objects representing the valid content proposals for a
     * field. 
     * 
     * @param contents
     *            the current contents of the field (only consulted if filtering
     *            is set to <code>true</code>)
     * @param position
     *            the current cursor position within the field
     * @return the array of Objects that represent valid proposals for the field
     *         given its current content.
     */
    @Override
    public IContentProposal[] getProposals(String contents, int position) {
        if (m_filterProposals) {
            String strBeforePos = contents.substring(0, position);
            
            // If the character before position is not part of identifier or one
            // of '.>, ', then do not show proposals, for example after 'a[3]'
            // This way there is no proposal box when it makes no sense and the user can
            // advance to the next field with TAB. (see mail 4.4.2012 14:10).
            // '{' was added to support host variables.
            if (strBeforePos.length() > 0) {
                char lastCh = strBeforePos.charAt(position - 1);
                if (!Character.isJavaIdentifierPart(lastCh)  &&  lastCh != '.'
                        &&  lastCh != '>'  && lastCh != ','  &&  lastCh != ' '
                        &&  lastCh != '{') {
                    return new IContentProposal[0];
                }
            }
            
            int lastTagStartIdx = UiTools.getStartOfLastWord(strBeforePos);
            
            String filterString = strBeforePos.substring(lastTagStartIdx);
            
            return proposalsToArray(m_proposals, filterString);
        } 
        
        if (m_contentProposals == null) {
            m_contentProposals = new IContentProposal[m_proposals.length];
            for (int i = 0; i < m_proposals.length; i++) {
                m_contentProposals[i] = makeContentProposal(m_proposals, m_descriptions, i, 0);
            }
        }
        
        return m_contentProposals;
    }

    
    protected IContentProposal[] proposalsToArray(String [] proposals, String filterString) {
        ArrayList<IContentProposal> allProposals = new ArrayList<>();
        ArrayList<IContentProposal> startWithFilterProposals = new ArrayList<>();
        
        for (int i = 0; i < proposals.length; i++) {
            if (StringUtils.containsIgnoreCase(proposals[i], filterString)) {
                if (StringUtils.startsWithIgnoreCase(proposals[i], filterString)) {
                    startWithFilterProposals.add(makeContentProposal(proposals, 
                                                                     m_descriptions, 
                                                                     i, 
                                                                     0)); // see desc. of this param in 'makeContentProposal()'
                                                                          // to understand why it is set to  0.
                } else {
                    allProposals.add(makeContentProposal(proposals, 
                                                         m_descriptions, 
                                                         i, 
                                                         0));
                }
            }
        }
        
        // put all proposals that start with the typed string at the front of the list 
        allProposals.addAll(0, startWithFilterProposals);

        if (!m_isMakeDefaultSelectionInContentProposals  &&  !allProposals.isEmpty()) {
            // empty proposal will not modify text if user simply presses enter.
            allProposals.add(0, new ContentProposal("", null));
        }
        
        
        return allProposals.toArray(new IContentProposal[allProposals.size()]);
    }

    
    
    /**
     * Set the Strings to be used as content proposals.
     * 
     * @param items the array of Strings to be used as proposals.
     * @param descriptions descriptions for each item in proposals
     */
    public void setProposals(String[] items, String [] descriptions) {
        m_proposals = items;
        m_descriptions = descriptions;
        m_contentProposals = null;
    }

    
    /**
     * Set the boolean that controls whether proposals are filtered according to
     * the current field content.
     * 
     * @param isFilterProposals
     *            <code>true</code> if the proposals should be filtered to
     *            show only those that match the current contents of the field,
     *            and <code>false</code> if the proposals should remain the
     *            same, ignoring the field content.
     */
    public void setFiltering(boolean isFilterProposals) {
        m_filterProposals = isFilterProposals;
        // Clear any cached proposals.
        m_contentProposals = null;
    }

    
    /**
     * Makes an IContentProposal showing item at the specified index.
     * 
     * @param proposals the array of Strings to be used as proposals.
     * @param descriptions descriptions for each item in proposals
     * @param idx index of item to be originally selected
     * @param filterStringLength can be used to create proposals which are appended
     * to currently entered string. For example, if user entered 'g_c', we set
     * filterStringLength = len('g_c') and proposal will contain 'har1' to
     * get 'g_char1'. However, this does not work is proposal may contain the 
     * entered string, not only start with it. For example, if user types 'ch',
     * valid proposal is also 'myChar'. In this case we can not provide
     * proposal 'ar', but 'ch' must be replaced with 'myChar'. This
     * method must therefore provide complete proposals, and replacement is done in 
     * AsystTextContentAdapter.
     */
    protected IContentProposal makeContentProposal(String []proposals, 
                                                   String[]descriptions, 
                                                   int idx, 
                                                   int filterStringLength) {
        String desc = null;
        if (descriptions != null  &&  descriptions.length > idx  && descriptions[idx] != null) {
            desc = descriptions[idx];
        }
        
        if (m_proposalsAcceptanceStyle == ContentProposalAdapter.PROPOSAL_REPLACE) {
            return new ContentProposal(proposals[idx], desc);
        }
        
        return new ContentProposal(proposals[idx].substring(filterStringLength), 
                                   proposals[idx], 
                                   desc);
    }

    
    /**
     *  @return proposal acceptance style, which is one of ContentProposalAdapter.PROPOSAL_REPLACE
     * or ContentProposalAdapter.PROPOSAL_INSERT
     */
    public int getProposalsAcceptanceStyle() {
        return m_proposalsAcceptanceStyle;
    }

    /**
     * By default proposal acceptance style is set to ContentProposalAdapter.PROPOSAL_REPLACE.
     * 
     * @param proposalsAcceptanceStyle one of ContentProposalAdapter.PROPOSAL_REPLACE
     * or ContentProposalAdapter.PROPOSAL_INSERT.
     */
    public void setProposalsAcceptanceStyle(int proposalsAcceptanceStyle) {
        m_proposalsAcceptanceStyle = proposalsAcceptanceStyle;
    }
    
    
    public static void setMakeDefaultSelectionInContentProposals(boolean isMakeDefaultSelectionInContentProposals) {
        m_isMakeDefaultSelectionInContentProposals = isMakeDefaultSelectionInContentProposals;
    }
}
