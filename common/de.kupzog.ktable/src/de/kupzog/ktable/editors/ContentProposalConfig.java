package de.kupzog.ktable.editors;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;

/**
 * This class contains configuration for content proposals.
 *  
 * @author markok
 *
 */
public class ContentProposalConfig {

    IContentProposalProvider m_proposalProvider; 
    IControlContentAdapter m_controlContentAdapter;
    int m_proposalsAcceptanceStyle;
    KeyStroke m_keyStroke;
    char[] m_autoActivationCharacters;
    

    /**
     * 
     */
    public ContentProposalConfig(IContentProposalProvider proposalProvider, 
                                 IControlContentAdapter controlContentAdapter) {

        m_proposalProvider = proposalProvider;
        m_controlContentAdapter = controlContentAdapter;
        
        m_proposalsAcceptanceStyle = ContentProposalAdapter.PROPOSAL_REPLACE;
        m_keyStroke = null;
        m_autoActivationCharacters = null;
    }
    
    
    public ContentProposalConfig(String [] proposals) {
        m_proposalProvider = new SimpleContentProposalProvider(proposals);
        m_proposalsAcceptanceStyle = ContentProposalAdapter.PROPOSAL_REPLACE;
        m_controlContentAdapter = new TextContentAdapter();
        m_keyStroke = null;
        m_autoActivationCharacters = null;
    }
    
    
    public IContentProposalProvider getProposalProvider() {
        return m_proposalProvider;
    }
    
    
    public void setProposalProvider(IContentProposalProvider proposalProvider) {
        m_proposalProvider = proposalProvider;
    }
    
    
    public int getProposalsAcceptanceStyle() {
        return m_proposalsAcceptanceStyle;
    }
    
    
    public void setProposalsAcceptanceStyle(int proposalsAcceptanceStyle) {
        m_proposalsAcceptanceStyle = proposalsAcceptanceStyle;
    }
    
    
    public IControlContentAdapter getControlContentAdapter() {
        return m_controlContentAdapter;
    }
    
    
    public void setControlContentAdapter(IControlContentAdapter controlContentAdapter) {
        m_controlContentAdapter = controlContentAdapter;
    }
    
    
    public KeyStroke getKeyStroke() {
        return m_keyStroke;
    }
    
    
    public void setKeyStroke(KeyStroke keyStroke) {
        m_keyStroke = keyStroke;
    }
    
    
    public char[] getAutoActivationCharacters() {
        return m_autoActivationCharacters;
    }
    
    
    public void setAutoActivationCharacters(char[] autoActivationCharacters) {
        m_autoActivationCharacters = autoActivationCharacters;
    }
    
    
    /*
     * @param proposalProvider may not be null
     * @param proposalsAcceptanceStyle one of ContentProposalAdapter.PROPOSAL_REPLACE or
     *                                        ContentProposalAdapter.PROPOSAL_INSERT
     */
}
