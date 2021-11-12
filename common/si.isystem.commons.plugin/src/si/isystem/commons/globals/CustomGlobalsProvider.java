package si.isystem.commons.globals;

/**
 * This class can be used by caller to specify its own subset of proposals, 
 * for example all functions stubbed in a test case. Call base method
 * setProposals() to define proposals and descriptions.
 */
public class CustomGlobalsProvider extends GlobalsProvider {
    
    protected CustomGlobalsProvider() {
        super(null, null);
    }


    @Override
    public String[] refreshGlobals() {
        
        return m_cachedGlobals;
    }
}
