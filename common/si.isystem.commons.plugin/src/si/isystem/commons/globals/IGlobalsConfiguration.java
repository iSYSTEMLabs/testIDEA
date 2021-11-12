package si.isystem.commons.globals;


/**
 * This interface should be implemented by clients of si.isystem.commons.globals
 * package. It provides client specific information to global providers.
 * 
 * @author markok
 *
 */
public interface IGlobalsConfiguration {

    /**
     * @return true, if function names in proposals should be in qualified form. 
     */
    public boolean isUseQualifiedFuncNames();
    
    
    /**
     * @return application specific set of core IDs.
     */
    public String [] getCoreIds();

    
    /**
     * If you'd like to add variables specific to your application, you can 
     * returns them there. The first array should contain proposals, the second 
     * one their descriptions, for example:
     * 
     *   String[][] proposalsWDesc = new String[2][];
     *   proposalsWDesc[0] = new String[]{"tm", "cyc"};
     *   proposalsWDesc[1] = new String[]{"contains current time", "counts cycles"};
     *   return proposalsWDesc;
     */
    public String[][] getCustomVars();
}
