package si.isystem.commons.globals;

import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.connect.adapters.JDataController;
import si.isystem.connect.data.JModule;
import si.isystem.connect.data.JPartition;

/** This class provides proposals for target source files. */
public class PartitionsGlobalsProvider extends GlobalsProvider {
    
    protected PartitionsGlobalsProvider(IConnectionProvider conProvider,
                                     String coreId) {
        super(conProvider, coreId);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        JDataController jDataCtrl = getJDataCtrl();
        
        if (jDataCtrl == null) {
            return new String[0];
        }

        JPartition[] partitions = jDataCtrl.getPartitions();
        
        String[] partitionNames = new String[partitions.length]; // use set to avoid duplicates
        
        int idx = 0;
        for (JPartition partition : partitions) {
            partitionNames[idx++]  = partition.getName();
        }

        m_cachedGlobals = partitionNames;
        m_cachedDescriptions = null; 

        return m_cachedGlobals;
    }
    
    
    public String[] getModulesWPaths(int partitionIdx) {
        
        JDataController jDataCtrl = getJDataCtrl();
        
        if (jDataCtrl == null) {
            return new String[0];
        }

        JModule[] jModules = jDataCtrl.getModules(partitionIdx);
        String [] modules = new String[jModules.length];
        int idx = 0;
        for (JModule jModule : jModules) {
            modules[idx++] = jModule.getPath(); 
        }
        
        return modules;
    }
}    

