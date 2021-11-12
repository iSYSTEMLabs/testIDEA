package si.isystem.connect.utils;

import java.util.ArrayList;
import java.util.List;

import si.isystem.connect.CConnectionConfig;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CWinIDEAVersion;
import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.VectorWinIDEAInstanceInfo;
import si.isystem.connect.WinIDEAInstanceInfo;

/**
 * This class enumerates running winIDEA instances and collects their workspace 
 * and version info.
 * 
 * @author markok
 */
public class WinIDEAEnumerator {

    public class WinIDEAInfo {
        int m_verMajor;
        int m_verMinor;
        int m_verBuild;

        String m_workspace;
        String m_instanceId;
        
        public WinIDEAInfo(int verMajor,
                           int verMinor,
                           int verBuild,
                           String workspace,
                           String instanceId) {
            
            m_verMajor = verMajor;
            m_verMinor = verMinor;
            m_verBuild = verBuild;
            m_workspace = workspace;
            m_instanceId = instanceId;
        }

        public int getVerMajor() {
            return m_verMajor;
        }

        public int getVerMinor() {
            return m_verMinor;
        }

        public int getVerBuild() {
            return m_verBuild;
        }

        public String getWorkspace() {
            return m_workspace;
        }

        public String getInstanceId() {
            return m_instanceId;
        }
    }
    
    
    public List<WinIDEAInfo> enumerate() {
        
        // this cmgr has nothing to do with existing connection to winIDEA - it
        // is used only to enumerate running winIDEAs.
        ConnectionMgr cmgr = new ConnectionMgr();
        
        String hostAddress = "";  // enumerate instances on local host. You may 
                                  // also specify remote host here, for example 
                                  // as IP address: '10.1.2.91'
        CConnectionConfig connectionConfig = new CConnectionConfig();
        VectorWinIDEAInstanceInfo instances = new VectorWinIDEAInstanceInfo();
        cmgr.enumerateWinIDEAInstances(hostAddress, connectionConfig, instances);
        
        ArrayList<WinIDEAInfo> winIDEAInfoList = new ArrayList<>();
        
        int numInstances = (int) instances.size();
        for (int i = 0; i < numInstances; i++) {
            WinIDEAInstanceInfo instanceInfo = instances.get(i);
            
            try {
                cmgr.connectMRU(instanceInfo.getWorkspace(), instanceInfo.getInstanceId());
                CIDEController ideCtrl = new CIDEController(cmgr);
                CWinIDEAVersion version = ideCtrl.getWinIDEAVersion();
                cmgr.disconnect();
                WinIDEAInfo info = new WinIDEAInfo(version.getMajor(),
                                                   version.getMinor(),
                                                   version.getBuild(),
                                                   instanceInfo.getWorkspace(),
                                                   instanceInfo.getInstanceId());

                winIDEAInfoList.add(info);
            } catch (Exception ex) {
                System.err.println("Can not connect to winIDEA to get version, workspace, and id info! " + ex.getMessage());
                ex.printStackTrace();
                // Ignore exception here, since some zombie winIDEAs (frozen on closing) may be listed, 
                // but do not allow connection. Exception here skips also available winIDEAs.
            }
        }
        
        return winIDEAInfoList;
    }
}
