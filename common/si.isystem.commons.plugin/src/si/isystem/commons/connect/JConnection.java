package si.isystem.commons.connect;

import java.util.Map;
import java.util.TreeMap;

import si.isystem.connect.CAddressController;
import si.isystem.connect.CDataController2;
import si.isystem.connect.CDebugFacade;
import si.isystem.connect.CExecutionController;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CLoaderController;
import si.isystem.connect.CMulticoreConnectionMgr;
import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.adapters.JDataController;

/**
 * This class manages controllers of one multicore 
 * connection. It manages native controllers and Java specific controllers.
 * 
 * @author markok
 *
 */
public class JConnection {

    private Map<String, JDataController> m_jDataControllers = new TreeMap<>();

    private CMulticoreConnectionMgr m_mccMgr;
    
    /**
     * This class should be instantiated only by ConnectionPool.
     *  
     * @param mccMgr
     */
    JConnection(CMulticoreConnectionMgr mccMgr) {
        m_mccMgr = mccMgr;
    }
    

    /**
     * Launches winIDEa if it does not already exist and connects to it.
     * 
     * @param coreIndex
     * @param coreId
     */
    public void connectCore(int coreIndex, String coreId) {

        m_mccMgr.connectCore(coreIndex, coreId);
    }

    
    /** 
     * Returns controller object for the given coreId.
     * 
     * Do not store reference to returned class, but call this method each time,
     * because connection can get broken anytime!
     * 
     * @param coreId application specific core ID, if null empty string is used.
     */
    public CDebugFacade getCDebugFacade(String coreId) {
        if (coreId == null) {
            coreId = "";
        }
        return m_mccMgr.getCDebugFacade(coreId);
    }

    
    /** 
     * Returns controller object for the given coreId.
     * 
     * Do not store reference to returned class, but call this method each time,
     * because connection can get broken anytime!
     * 
     * @param coreId application specific core ID, if null empty string is used.
     */
    public CAddressController getAddrController(String coreId) {
        if (coreId == null) {
            coreId = "";
        }
        return getCDebugFacade(coreId).getAddressController();
    }
    
    
    /** 
     * Returns controller object for the given coreId.
     * 
     * Do not store reference to returned class, but call this method each time,
     * because connection can get broken anytime!
     * 
     * @param coreId application specific core ID, if null empty string is used.
     */
    public CIDEController getCIDEController(String coreId) {
        if (coreId == null) {
            coreId = "";
        }
        return m_mccMgr.getCIDEController(coreId);
    }
    
    public CExecutionController getCExecController(String coreId) {
        if (coreId == null) {
            coreId = "";
        }
        return m_mccMgr.getCExecutionController(coreId);
    }
    
    public CLoaderController getCLoaderController(String coreId) {
        if (coreId == null) {
            coreId = "";
        }
        return m_mccMgr.getCLoaderController(coreId);
    }
    
    
    /** 
     * Returns controller object for the given coreId.
     * 
     * Do not store reference to returned class, but call this method each time,
     * because connection can get broken anytime!
     * 
     * @param coreId application specific core ID, if null empty string is used.
     */
    public CDataController2 getCDataController(String coreId) {
        if (coreId == null) {
            coreId = "";
        }
        return m_mccMgr.getCDataEController2(coreId);
    }
    
    
    /** 
     * Returns controller object for the given coreId.
     * 
     * Do not store reference to returned class, but call this method each time,
     * because connection can get broken anytime!
     * 
     * @param coreId application specific core ID, if null empty string is used.
     */
    public JDataController getJDataController(String coreId) {
        if (coreId == null) {
            coreId = "";
        }

        JDataController ctrl = m_jDataControllers.get(coreId);
        // lazy creation is used for Java controllers, so that createContr...()
        // needs not to be called when connection is made.
        if (ctrl == null) {
            createControllersForCore(coreId);
        }
        return m_jDataControllers.get(coreId);
    }

    
    private void createControllersForCore(String coreId) {
        if (coreId == null) {
            coreId = "";
        }

        CIDEController ideCtrl = m_mccMgr.getCIDEController(coreId);
        
        m_jDataControllers.put(coreId, new JDataController(m_mccMgr.getCDataEController2(coreId), 
                                                           m_mccMgr.getCHILController(), 
                                                           ideCtrl));
    }
    

    /**
     * @return true, if connection to primary core is established.
     */
    public boolean isConnected() {
        return m_mccMgr.isConnected("");
    }
    
    
    public boolean isConnected(String coreId) {
        return m_mccMgr.isConnected(coreId);
    }
    
    
    /**
     * @return native C++ object dealing with connections to single or multi-core targets. 
     */
    public CMulticoreConnectionMgr getMccMgr() {
        return m_mccMgr;
    }


    /**
     * @return native C++ object connected to primary target core.
     */
    public ConnectionMgr getPrimaryCMgr() {
        return m_mccMgr.getConnectionMgr("");
    }


    /**
     * Disconnects connections to all cores of one target.
     */
    public void disconnectAll() {
        m_mccMgr.disconnectAll();
    }
}
