
package si.isystem.itest.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This is a base class for actions.
 */
public abstract class AbstractAction {

    private String m_cmdName;
    protected boolean m_isInGroup;

    public enum EFireEvent {EXEC, UNDO, REDO};
    private EnumSet<EFireEvent> m_fireEventTypes;

    private List<ModelChangedEvent> m_events;
    
    /**
     * Instantiates action.
     * 
     * @param cmdName
     */
    public AbstractAction(String cmdName) {
        m_cmdName = cmdName;
        m_fireEventTypes = EnumSet.noneOf(EFireEvent.class); 
    }

    public AbstractAction(String cmdName, CTestBase testBase) {
        String testSpecStr = ""; 
    
        if (testBase.getContainerTestNode() != null) {
            testSpecStr = testBase.getContainerTestNode().getUILabel();
        }
        
        m_cmdName = cmdName + " [" + testSpecStr + "  {" + testBase.getClass().getSimpleName() + "}]";
        m_fireEventTypes = EnumSet.noneOf(EFireEvent.class); 
    }

    /**
     * This method should return CTestSpecification, which is modified by the action.
     */
    public CTestTreeNode getContainerTreeNode() {
        return null;
    }
    
    /**
     * This method performs the action.
     */
    public abstract void exec();

    /**
     * Subclasses should override this method if it may happen that the value
     * they are going to set equals the existing one. 
     * @return this method always returns true
     */
    public boolean isModified() {
        return true;
    }

    /**
     * @return true, if action can be undone.
     */
    public boolean isUndoable() {
        return true;
    }

    /**
     * @return true, if action can be redone.
     */
    public boolean isRedoable() {
        return true;
    }

    /**
     * Reverts the action. Caller should verify with call to isUndoable() that
     * command can be undone. Derived classes should throw an exception, if this
     * method is called when isUndoable() returns false.
     * 
     * Event about changed data should be fired in this method, so that GUI
     * can show the changed data. This is important for undo/redo operations, while
     * the normal exec() should not select the changed node - the user has just 
     * selected another one after entering the data.
     */
    public abstract void undo();

    /**
     * Repeats the action. Caller should verify with call to isRedoable() that
     * command can be redone. Derived classes should throw an exception, if this
     * method is called when isRedoable() returns false.
     * 
     * Event about changed data should be fired in this method, so that GUI
     * can show the changed data. This is important for undo/redo operations, while
     * the normal exec() should not select the changed node - the user has just 
     * selected another one after entering the data.
     */
    public abstract void redo();
    

    public String getName() {
        return m_cmdName;
    }

    /**
     * Returns true, if this action is member of a group action.
     * 
     * @see #setInGroup(boolean)
     */
    public boolean isInGroup() {
        return m_isInGroup;
    }

    /** 
     * Sets flag, which can be used by derived actions to postpone some actions,
     * for example sending events to the model. Example: DeleteAction should
     * send TREE_STRUCTURE_CHANGED event only once per parent, not for each item deleted.
     */
    public void setInGroup(boolean isInGroup) {
        m_isInGroup = isInGroup;
    }

    
    /** If true, action should fire events. */
    private boolean isFireEvents(EFireEvent eventType) {
        // calls overridden method of GroupAction, when present 
        EnumSet<EFireEvent> eventTypes = getFireEventTypes();
                
        return !m_isInGroup  &&  eventTypes.contains(eventType);
    }

    
    /** If set to true, action should fire events regardless of in group or not. */
    public void addFireEventType(EFireEvent eventType) {
        m_fireEventTypes.add(eventType);
    }

    /** Convenient method, because it is used very often. */
    public void addFireEventTypes(EFireEvent eventType1, EFireEvent eventType2) {
        m_fireEventTypes.add(eventType1);
        m_fireEventTypes.add(eventType2);
    }

    public void addAllFireEventTypes() {
        m_fireEventTypes.add(EFireEvent.EXEC);
        m_fireEventTypes.add(EFireEvent.UNDO);
        m_fireEventTypes.add(EFireEvent.REDO);
    }

    
    public EnumSet<EFireEvent> getFireEventTypes() {
        return m_fireEventTypes;
    }
    
    
    public List<ModelChangedEvent> getEvents() {
        if (m_events != null) {
            return m_events;
        }
        return new ArrayList<ModelChangedEvent>();
    }
    
    
    public void addDataChangedEvent(ENodeId nodeId, CTestBase testBase) {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                                        null,
                                                        testBase.getContainerTestNode(), 
                                                        nodeId);
        addEvent(event);
    }
    
    
    public void addTreeChangedEvent(CTestTreeNode parentSpec, CTestTreeNode changedSpec) {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                        parentSpec,
                                                        changedSpec);
        addEvent(event);
    }
    
    /**
     * Sets custom configured event. Can be used for example to set test spec.
     * to null (no refresh of test tree for data changed events, or some other
     * instance to be selected in case of data changed event (see 
     * TestSpecificationTreeView.testSpecDataChanged().
     * */
    public void addEvent(ModelChangedEvent event) {
        if (m_events == null) {
            m_events = new ArrayList<>();
        }
        m_events.add(event);
    }

    
    // these three methods should be called from TestSpecificationModel.execAction() only  
    public void fireEventsOnExec(IEventDispatcher dispatcher) {
        if (isFireEvents(EFireEvent.EXEC)) {
            fireEvents(dispatcher);
        }
    }
    
    public void fireEventsOnUndo(IEventDispatcher dispatcher) {
        if (isFireEvents(EFireEvent.UNDO)) {
            fireEvents(dispatcher);
        }
    }
    
    public void fireEventsOnRedo(IEventDispatcher dispatcher) {
        if (isFireEvents(EFireEvent.REDO)) {
            fireEvents(dispatcher);
        }
    }
    
    protected void fireEvents(IEventDispatcher dispatcher) {
        
        CTestTreeNode firstParent = null;
        CTestTreeNode newTestSpec = null;
        boolean isRefreshRoot = false;
        boolean isTestSpecTreeStructChangedEvent = false;
        ModelChangedEvent dataChangedEvent = null;
        
        List<ModelChangedEvent> events = getEvents();
        for (ModelChangedEvent event : events) {
            
            CTestTreeNode parent = event.getOldSpec();

            // data changed events should be sent only once, because one refersh
            // is enough. If necessary, make a set of events with different
            // test specs and node IDs.
            if (event.getEventType() == EventType.TEST_SPEC_DATA_CHANGED) {
                dataChangedEvent = event;
                
            } else if (event.getEventType() == EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED) {

                if (firstParent != null  &&  parent != null  &&  !parent.equalsData(firstParent)) {
                    isRefreshRoot = true;
                }

                isTestSpecTreeStructChangedEvent = true;
            }
            
            if (parent != null  &&  firstParent == null) {
                firstParent = parent;
                newTestSpec = event.getNewSpec();
            }
        }
        
        
        if (isRefreshRoot) {
            dispatcher.fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                       null, null));
        } else {
            if (isTestSpecTreeStructChangedEvent) {
                dispatcher.fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                           firstParent, newTestSpec));
            }
        }
        
        if (dataChangedEvent != null) {
            dispatcher.fireEvent(dataChangedEvent);
        }
        
        // model changed event should be fired only once
        if (isTestSpecTreeStructChangedEvent  ||  dataChangedEvent != null) {
            dispatcher.fireEvent(new ModelChangedEvent(EventType.MODEL_CHANGED));
        }
    }

    
    /*
    protected void fireDataChangedEvent(TBModelEventDispatcher dispatcher,
                                        CTestSpecification testSpec, 
                                        TestSpecificationEditorView.ENodeId nodeId) {
        // at the time of thins function implementation no listener required
        // test specification data
        dispatcher.
            fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                            null,
                                            testSpec,
                                            nodeId));
    }
    
    
    protected void fireTreeStructureChangedEvent(TBModelEventDispatcher dispatcher,
                                                 CTestSpecification parentSpec, 
                                                 CTestSpecification changedSpec) {
        TestSpecificationModel.getInstance().
            fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                            parentSpec,
                                            changedSpec));
    }

    
    protected void fireTestStatusChangedEvent(TBModelEventDispatcher dispatcher) {
        TestSpecificationModel.getInstance().
            fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.UPDATE_TEST_RESULTS,
                                            null,
                                            null));
    }
*/
}
