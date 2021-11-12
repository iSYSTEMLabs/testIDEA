package si.isystem.itest.model;

import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.ui.spec.TestSpecificationEditorView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class ModelChangedEvent {
    
    private CTestTreeNode m_oldSpec;

    private CTestTreeNode m_newSpec;
    
    private CTestTreeNode m_containerTestSpecOrGrp;

    private String m_actionName;
   
    public EventType m_eventType;

    private ENodeId m_nodeId;

    public enum EventType {MODEL_CHANGED,                    // model has changed, refresh of save status needed
                                                             // This event is automatically sent if TEST_SPEC_DATA_CHANGED
                                                             // or TEST_SPEC_TREE_STRUCTURE_CHANGED is sent. 
                           TEST_SPEC_TREE_SELECTION_CHANGED, // user clicked new node, display it on the right 
                           TEST_SPEC_DATA_CHANGED,           // modify selected test spec.
                           TEST_SPEC_TREE_STRUCTURE_CHANGED, // refresh the test spec. tree on the left 
                           NEW_MODEL,                        // model has completely changed - set input should be called.
                           UPDATE_TEST_RESULTS,              // refresh of Errors view is required
                           CONNECTION_ESTABLISHED,           // connection to winIDEA has been established, refresh globals
                           TEST_SPEC_TREE_REFRESH_REQUIRED}; // only test specification tree refresh is required, 
                                                             // no changes for other components
                                                             // This event was added because of unwanted triggering of 
                                                             // help asyst for function name in function spec editor,
                                                             // when focus was lost - method setTest() for combo
                                                             // openedthe content asyst, but focus was laready on
                                                             // other component (tab key was used, not mouse).
    
    /** 
     * Source type of event. If it is tree viewer itself, than it should not
     * update the selection, because it gets to endless recursive loop.
     * If source is OTHER_CONTROL, for example when user clicked error in Error view,
     * then the tree viewer MUST update the selection.
     *   
     * @author markok
     */
    public enum SourceType {TREE_VIEWER, 
                            OTHER_CONTROL};

    public ModelChangedEvent(EventType eventType) {
        m_eventType = eventType;
        m_oldSpec = null;
        m_newSpec = null;
    }

    public ModelChangedEvent(EventType eventType, TestSpecificationEditorView.ENodeId nodeId) {
        m_eventType = eventType;
        m_nodeId = nodeId;
        m_oldSpec = null;
        m_newSpec = null;
    }

    public ModelChangedEvent(EventType eventType, 
                             CTestTreeNode oldSpec, 
                             CTestTreeNode newSpec) {
        m_eventType = eventType;
        m_oldSpec = oldSpec;
        m_newSpec = newSpec;
    }
    
    public ModelChangedEvent(EventType eventType, 
                             CTestTreeNode oldSpec, 
                             CTestTreeNode newSpec,
                             TestSpecificationEditorView.ENodeId nodeId) {
        m_eventType = eventType;
        m_oldSpec = oldSpec;
        m_newSpec = newSpec;
        m_nodeId = nodeId;
    }
    
    
    public CTestTreeNode getOldSpec() {
        return m_oldSpec;
    }

    
    public CTestTreeNode getNewSpec() {
        return m_newSpec;
    }

    
    public CTestTreeNode getContainerTestSpec() {
        return m_containerTestSpecOrGrp;
    }

    
    public String getActionName() {
        return m_actionName;
    }

    
    public EventType getEventType() {
        return m_eventType;
    }

    
    public TestSpecificationEditorView.ENodeId getNodeId() {
        return m_nodeId;
    }
    
    
    public void setContainerTestSpec(CTestTreeNode containerTestSpecOrGrp) {
        m_containerTestSpecOrGrp = containerTestSpecOrGrp;
    }

    
    public void setActionName(String actionName) {
        m_actionName = actionName;
    }
}

