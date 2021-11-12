package si.isystem.commons.connect;

import java.util.Map;
import java.util.TreeMap;

import si.isystem.connect.CMulticoreConnectionMgr;
import si.isystem.connect.StrVector;

/**
 * This class stores connections to winIDEAs. It contains instances 
 * of JConnection class. It provides low level methods for connecting to winIDEA.
 * Each plug-in using this class should have one and only one instance of it.
 *  
 * For usage in UI application, please use class ConnectCommandHandler. 
 * 
 * @author markok
 */
public class ConnectionPool {

    Map<String, JConnection> m_connections = new TreeMap<>();

    public final static String DEFAULT_CONNECTION = "_default_";
    /**
     * This method connects to winIDEA, which controls primary core on target.
     *  
     * @param connectionId recommended pluginID + connectionID. 
     *                     Use ConnectionPool.DEFAULT_CONNECTION
     *                     if your application will support only one emulator.
     * @param winIDEAWorkspace name of winIDEA workspace file. If not empty, this defines
     *                         winIDEA instance to which we want to connect.
     * @param winIDEAId ID of winIDEA instance which we want to connect to
     * @param primaryCoreId application specific ID of primary core. May be empty string.
     * @param loggingParams may be null or empty. Otherwise the first element (at index 0)
     *                      specified name log file for logging isystem.connect calls, 
     *                      and optional second element specifies prefix to distinguish
     *                      isystem.connect calls from different connections.
     * @return new connection
     */
    public JConnection connect(String connectionId,
                        String winIDEAWorkspace, 
                        String winIDEAId,
                        String primaryCoreId,
                        StrVector loggingParams) {
        
        if (loggingParams == null) {
            loggingParams = new StrVector();
        }
        CMulticoreConnectionMgr mccMgr = new CMulticoreConnectionMgr();
        mccMgr.connectPrimaryCore(loggingParams, 
                                  winIDEAWorkspace, 
                                  winIDEAId != null, 
                                  winIDEAId != null ? winIDEAId : "", 
                                  primaryCoreId);
        
        JConnection jCon = new JConnection(mccMgr);
        m_connections.put(connectionId, jCon);
        
        return jCon;
    }
    
    
    /**
     * Returns true, if connection object for the given ID exists and is connected
     * to winIDEA.
     * 
     * @param connectionId recommended pluginID + connectionID. 
     *                     Use ConnectionPool.DEFAULT_CONNECTION
     *                     if your application will support only one emulator.
     */
    public boolean isConnected(String connectionId) {
        JConnection jCon = m_connections.get(connectionId);
        return jCon != null  &&  jCon.isConnected();
    }
    
    
    /**
     * @param connectionId recommended pluginID + connectionID. 
     *                     Use ConnectionPool.DEFAULT_CONNECTION
     *                     if your application will support only one emulator.
     * 
     * @return connection for the given ID, or null if no connection for the 
     *                    given ID exists.
     */
    public JConnection getConnection(String connectionId) {
        return m_connections.get(connectionId);
    }
    

    /**
     * Disconnects connection with the given ID and removes connection object 
     * from this container.
     *  
     * @param connectionId recommended pluginID + connectionID. 
     *                     Use ConnectionPool.DEFAULT_CONNECTION
     *                     if your application will support only one emulator.
     * @return true if connection object existed, false otherwise
     */
    public boolean disconnect(String connectionId) {
        JConnection jCon = m_connections.get(connectionId);
        
        if (jCon != null) {
            jCon.disconnectAll();
            m_connections.remove(connectionId);
            return true;
        }
        
        return false;
    }
}
