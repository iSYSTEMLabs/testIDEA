package si.isystem.itest.common;

import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.exceptions.SIllegalArgumentException;

/**
 * This class has utility methods for manipulation of target core IDs.
 * 
 * @author markok
 *
 */
public class CoreIdUtils {

    /**
     * @return core IDs as set in env. part of test spec., or at least an 
     *         array with empty string. Returned array always has at least one 
     *         element.
     */
    public static String [] getCoreIDs(CTestEnvironmentConfig envConfig) {
        String[] coreIds = DataUtils.getArray(envConfig, 
                              EEnvConfigSections.E_SECTION_CORE_IDS.swigValue());
        if (coreIds.length > 0) {
            return coreIds;
        }
        
        // if coreIDs are not defined, we have only one core with empty ID 
        return new String[]{""};
    }

    
    /**
     * Returns index of the given coreId, or -1 if the given coreId is not found
     * in the list of coreIDs.
     * 
     * @param coreId ID of the core to get index for
     * @param isThrow if true, exception is thrown on error, if false -1 is returned on error.
     * @return
     */
    public static int getCoreIdIndex(CTestEnvironmentConfig envConfig,
                                     String coreId, 
                                     boolean isThrow) {
        
        String[] coreIds = getCoreIDs(envConfig);
        
        if (coreId.isEmpty()) {
            return 0;
        }
        
        int coreIdx = 0;
        for (String configuredCoreId : coreIds) {
            if (configuredCoreId.equals(coreId)) {
                break;
            }
            coreIdx++;
        }
        
        if (coreIdx >= coreIds.length) {
            if (isThrow) {
                throw new SIllegalArgumentException("Unknown 'coreID'!" + 
                        " Make sure the ID is set in dialog 'File | Properties | Multicore'.").
                        add("coreId", coreId);
            } else {
                return -1;
            }
        }
            
        return coreIdx;
    }

    
    /**
     * If coreId is empty, returns ID of the primary core if defined. Otherwise
     * original core ID is returned.
     * 
     * @param coreId
     * @return
     */
    public static String getConfiguredCoreID(CTestEnvironmentConfig envConfig, 
                                             String coreId) {

        return envConfig.getConfiguredCoreID(coreId);
    }
}
