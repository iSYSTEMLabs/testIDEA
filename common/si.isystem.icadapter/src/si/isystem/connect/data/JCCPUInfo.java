package si.isystem.connect.data;

import si.isystem.connect.CCPUInfo;

/** 
 * This class is immutable wrapper of CCPUInfo. 
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCCPUInfo {

    private final int m_cpu;
    private final int m_variant;

    /**
     * Instantiates object and initializes it with data from <sode>cpuInfo</code>
     * native object. 
     * 
     * @param cpuInfo C++ object containing address information
     * 
     * @see CCPUInfo in WCommon\globdefs.h for avaialble CPU types and variants.
     */
    public JCCPUInfo(CCPUInfo cpuInfo) {
        m_cpu = cpuInfo.getM_wCPU();
        m_variant = cpuInfo.getM_wVariant();
    }

    /** Returns CPU type. 
     * 
     * @see CCPUInfo in WCommon\globdefs.h for avaialble CPU types and variants.
     */
    public int getCpu() {
        return m_cpu;
    }

    /** Returns CPU variant.
     * 
     * @see CCPUInfo in WCommon\globdefs.h for avaialble CPU types and variants.
     */
    public int getVariant() {
        return m_variant;
    }
}
