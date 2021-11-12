package si.isystem.itest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles events triggered by Action-s - it calls all registered 
 * listeners. Normally this class is used by main app. model, but when editing
 * CTestBase hierarchy in dialogs, a temporary instance of this calss may be 
 * instantiated and listeners registered. 
 * 
 * @author markok
 *
 */
public class TBModelEventDispatcher implements IEventDispatcher {

    private List<ITestSpecModelListener> m_listeners = new ArrayList<ITestSpecModelListener>();

    
    public void addListener(ITestSpecModelListener listener) {
        m_listeners.add(listener);
    }

    
    @Override
    public void fireEvent(ModelChangedEvent event) {

        for (ITestSpecModelListener listener : m_listeners) {
            switch (event.getEventType()) {
            case TEST_SPEC_TREE_SELECTION_CHANGED:
                listener.testSpecTreeSelectionChanged(event);
                break;
            case TEST_SPEC_TREE_STRUCTURE_CHANGED:
                listener.testSpecTreeStructureChanged(event);
                break;
            case TEST_SPEC_DATA_CHANGED:
                listener.testSpecDataChanged(event);
                break;
            case NEW_MODEL:
                listener.newInput(event);
                break;
            case UPDATE_TEST_RESULTS:
                listener.updateTestResults(event);
                break;
            case CONNECTION_ESTABLISHED:
                listener.connectionEstablished(event);
                break;
            case TEST_SPEC_TREE_REFRESH_REQUIRED:
                listener.testSpecTreeRefreshRequired(event);
                break;
            case MODEL_CHANGED:
                listener.modelChanged(event);
            default:
                // ignore - event will not be dispatched
            }
        }
    }


}
