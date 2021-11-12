package si.isystem.itest.model.actions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;

public class GroupAction extends AbstractAction {

    private ArrayList<AbstractAction> m_actions = new ArrayList<AbstractAction>();
    
    public GroupAction(String actionName) {
        super(actionName);
    }
    
    
    public GroupAction(String actionName, CTestSpecification testSpec) {
        super(actionName, testSpec);
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        if (!m_actions.isEmpty()) {
            return m_actions.get(0).getContainerTreeNode();
        }
        
        return null;
    }
    
    
    public void add(AbstractAction action) {
        if (action != null) {
            action.setInGroup(true);
            m_actions.add(action);
        }
    }
    
    
    @Override
    public void exec() {
        for (AbstractAction action : m_actions) {
            action.exec();
        }
    }

    
    @Override
    public void undo() {
        ListIterator<AbstractAction> it = m_actions.listIterator(m_actions.size());
        
        while (it.hasPrevious()) {
            AbstractAction action = it.previous();
            action.undo();
        }
    }
    

    @Override
    public void redo() {
        exec(); 
    }
    

    /**
     * Returns events of this and all contained actions in hierarchy.
     */
    @Override
    public List<ModelChangedEvent> getEvents() {
        
        List<ModelChangedEvent> events = new ArrayList<ModelChangedEvent>();
        // in undo to drag&drop it happens, (and can happen in drag & drop
        // in general), that actions have different parents. In such case we
        // have to refresh all parents.
        for (AbstractAction action : m_actions) {
            events.addAll(action.getEvents());
        }
        
        events.addAll(super.getEvents());
            
        return events;
    }
    

    @Override
    public EnumSet<EFireEvent> getFireEventTypes() {
        EnumSet<EFireEvent> eventTypes = EnumSet.noneOf(EFireEvent.class); 

        for (AbstractAction action : m_actions) {
            eventTypes.addAll(action.getFireEventTypes());
        }

        eventTypes.addAll(super.getFireEventTypes());
        
        return eventTypes;
    }

/*  fireEvents in base class is now universal.
 *   
    protected void fireEvents(TBModelEventDispatcher dispatcher) {
        // in undo to drag&drop it happens, (and can happen in drag & drop
        // in general), that actions have different parents. In such case we
        // have to refresh all parents.
        List<ModelChangedEvent> events = getEvents();
        
        CTestSpecification prevParent = null;
        boolean isRefreshRoot = false;
        ModelChangedEvent testSpecDataChangedEvent = null;
        boolean isTestSpecTreeStructChangedEvent = false;
        
        for (ModelChangedEvent event : events) {
            CTestSpecification parent = event.getOldSpec();
            // if two parents are not equal, root will be refreshed
            if (prevParent != null  &&  !parent.equalsData(prevParent)) {
                isRefreshRoot = true;
            }
            
            // Only one event is sent. If necessary, make a set of events with different
            // test specs and node IDs.
            if (event.getEventType() == EventType.TEST_SPEC_DATA_CHANGED) {
                testSpecDataChangedEvent = event;
            }
            
            if (event.getEventType() == EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED) {
                isTestSpecTreeStructChangedEvent = true;
            }
        }
        
        if (isRefreshRoot) {
            dispatcher.fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                       null, null));
        } else {
            if (isTestSpecTreeStructChangedEvent) {
                dispatcher.fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                           prevParent, prevParent));
            }
        }
        
        if (testSpecDataChangedEvent != null) {
            dispatcher.fireEvent(testSpecDataChangedEvent);
        }
    }
  */  
    
    /** Returns subactions. Do not modify the returned list!. */
    public List<AbstractAction> getActions() {
        return m_actions;
    }

    public boolean isEmpty() {
        return m_actions.isEmpty();
    }
}
