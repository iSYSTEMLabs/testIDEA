package si.isystem.itest.model;

/**
 * Most model listeners need to implement only one ot two methods, so this adapter
 * simplifies them.
 */
abstract public class TestSpecModelListenerAdapter implements ITestSpecModelListener {

    @Override
    public void testSpecTreeStructureChanged(ModelChangedEvent event) {
    }


    @Override
    public void testSpecTreeSelectionChanged(ModelChangedEvent event) {
    }


    @Override
    public void testSpecDataChanged(ModelChangedEvent event) {
    }


    @Override
    public void newInput(ModelChangedEvent event) {
    }


    @Override
    public void updateTestResults(ModelChangedEvent event) {
    }


    @Override
    public void connectionEstablished(ModelChangedEvent event) {
    }


    @Override
    public void testSpecTreeRefreshRequired(ModelChangedEvent event) {
    }
}
