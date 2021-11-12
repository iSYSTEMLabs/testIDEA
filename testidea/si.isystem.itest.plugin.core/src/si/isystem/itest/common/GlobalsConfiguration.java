package si.isystem.itest.common;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.core.runtime.IProgressMonitor;

import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.globals.IGlobalsConfiguration;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestFilterController;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.model.TestSpecificationModel;

public class GlobalsConfiguration implements IGlobalsConfiguration {

    private GlobalsContainer m_globalsContainer;
    private TestSpecificationModel m_activeModel;

    private final static GlobalsConfiguration INSTANCE = new GlobalsConfiguration(); 

    
    
    public GlobalsConfiguration() {
        TagsGlobalsProvider tagsGP = new TagsGlobalsProvider();
        TestIdsGlobalsProvider testIDsGP = new TestIdsGlobalsProvider();
        
        m_globalsContainer = new GlobalsContainer(this, ConnectionProvider.instance());
        
        m_globalsContainer.addProvider(GlobalsContainer.GC_TAGS, tagsGP);
        m_globalsContainer.addProvider(GlobalsContainer.GC_TEST_IDS, testIDsGP);
    }


    public TestSpecificationModel getActiveModel() {
        return m_activeModel;
    }


    public void setActiveModel(TestSpecificationModel activeModel) {
        m_activeModel = activeModel;
    }


    @Override
    public boolean isUseQualifiedFuncNames() {
        return m_activeModel.isUseQualifiedFuncNames();
    }

    
    @Override
    public String [] getCoreIds() {
        return m_activeModel.getCoreIDs();
    }


    @Override
    public String[][] getCustomVars() {
        PersistVarsProposalsProvider persistPropsProvider = new PersistVarsProposalsProvider();
        return persistPropsProvider.getAllPersistVars();
    }
    
    
    public static GlobalsConfiguration instance() {
        return INSTANCE;
    }
    
    
    public GlobalsContainer getGlobalContainer() {
        return m_globalsContainer;
    }

    
    public void refreshHeadless(IProgressMonitor monitor,
                                JConnection defaultConnection,
                                TestSpecificationModel model) {

        CTestFilterController filterController = model.getFilterController();
        CTestEnvironmentConfig envConfig = model.getTestBench().getTestEnvironmentConfig(true);
        
        // this data may also be stale, clear it
        model.getRootTestSpecification().clearMergedFilterInfo(true);
        
        if (monitor != null) {
            monitor.subTask("Refreshing symbols in globals container ...");
        }
        
        m_globalsContainer.refreshHeadless(defaultConnection);

        if (monitor != null) {
            monitor.worked(1);
            monitor.subTask("Refreshing symbols in filter controller ...");
        }
        // System.out.print("Refreshing FilterCtrl ... ");
        StopWatch sw = new StopWatch(); sw.start();
        filterController.refreshSymbols(envConfig,
                                        m_globalsContainer.getCGlobalsContainer());
        sw.stop();
        System.out.println("FilterCtrl refreshed: " + sw);
        if (monitor != null) {
            monitor.worked(1);
        }
    }


    public CTestFilterController getActiveFilterController() {
        if (m_activeModel == null) {
            return null;
        }
        return m_activeModel.getFilterController();
    }
}


class TagsGlobalsProvider extends GlobalsProvider {
    
    protected TagsGlobalsProvider() {
        super(null, null);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        TestSpecificationModel model = GlobalsConfiguration.instance().getActiveModel();
        if (model != null) {
            m_cachedGlobals = model.getAutocompletionTags();
            m_cachedDescriptions = null;
        }

        return m_cachedGlobals;
    }
}


class TestIdsGlobalsProvider extends GlobalsProvider {
    
    protected TestIdsGlobalsProvider() {
        super(null, null);
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();
        
        TestSpecificationModel model = GlobalsConfiguration.instance().getActiveModel();
        if (model != null) {
            m_cachedGlobals = model.getAutocompletionIds();
            m_cachedDescriptions = null;
        }

        return m_cachedGlobals;
    }
}


