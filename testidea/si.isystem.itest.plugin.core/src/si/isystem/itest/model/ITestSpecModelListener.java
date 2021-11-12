package si.isystem.itest.model;


public interface ITestSpecModelListener {

    void testSpecTreeStructureChanged(ModelChangedEvent event);
    void testSpecTreeSelectionChanged(ModelChangedEvent event);
    void testSpecDataChanged(ModelChangedEvent event);
    
    /** Event must specify the new test specification. */
    void newInput(ModelChangedEvent event);
    
    void updateTestResults(ModelChangedEvent event);
    void connectionEstablished(ModelChangedEvent event);
    void testSpecTreeRefreshRequired(ModelChangedEvent event);
    
    void modelChanged(ModelChangedEvent event);
}
