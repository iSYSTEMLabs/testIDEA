package si.isystem.itest.model.actions.mapping;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class MovePairAction extends AbstractAction {

    protected CTestBase m_testBase;
    private int m_sectionId;
    private String m_key;
    
    private String m_oldPredecessor = null;
    
    protected ENodeId m_nodeId;
    private String m_predecessor;

    
    public MovePairAction(CTestBase testBase,
                           int section, 
                           String key,
                           int newIndex) {
        
        super("Moves mapping pair in ordered map action: " + key + ", " + newIndex, 
              testBase);
        
        m_testBase = testBase;
        m_sectionId = section; 
        m_key = key;
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        int keyIdx = userMapping.getKeyIndex(key);

        if (keyIdx > 0) {
            m_oldPredecessor = userMapping.getKey(keyIdx - 1);
        } else {
            m_oldPredecessor = null;  // move the pair with the given key to beginning 
        }
        
        if (keyIdx >= 0) {
            
            if (newIndex > 0) {
                m_predecessor = userMapping.getKey(newIndex < keyIdx ? newIndex - 1 : newIndex);
            } else {
                m_predecessor = null;  // move the pair with the given key to beginning 
            }
            
            // if m_predecessor == null now, it means the key was moved to index 0 
        } else {
            throw new SIllegalStateException("Can not remove item from user mapping - it does not exist!").
            add("key", m_key);
        }
    }
    
    // not tested
    /**
     * 
     * @param testBase
     * @param section
     * @param key key of the pair to move
     * @param immediateSuccessor pair with 'key' will be moved before this key.  
     */
    public MovePairAction(CTestBase testBase,
                           int section, 
                           String key,
                           String immediateSuccessor) {
        
        super("Moves mapping pair in ordered map action: " + key + ", " + immediateSuccessor, 
              testBase);
        
        m_testBase = testBase;
        m_sectionId = section; 
        m_key = key;
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        if (userMapping.contains(m_key)) {
            
            int successorIndex = userMapping.getKeyIndex(immediateSuccessor);
            
            if (successorIndex >= 0) {
                if (successorIndex > 0) {
                    m_predecessor = userMapping.getKey(successorIndex - 1);
                } else {
                    m_predecessor = null;  // move the pair with the given key to beginning 
                }
            } else {
                throw new SIllegalStateException("Can not move item in user mapping - destination key does not exist!").
                add("key", m_key).
                add("successor", immediateSuccessor);
            }
            // if m_predecessor == null now, it means the key was removed at index 0 
        } else {
            throw new SIllegalStateException("Can not move item from user mapping - it does not exist!").
                add("key", m_key).
                add("successor", immediateSuccessor);
        }
    }
    
    
    @Override
    public void exec() {
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        StrVector predecessor = new StrVector();
        if (m_predecessor != null) {
            predecessor.add(m_predecessor);
        }
       
        userMapping.moveKey(m_key, predecessor);
    }

    
    @Override
    public void undo() {
        
        // there was no such key before removal, do nothing
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        StrVector predecessor = new StrVector();
        if (m_oldPredecessor != null) {
            predecessor.add(m_oldPredecessor);
        }
       
        userMapping.moveKey(m_key, predecessor);
    }

    
    @Override
    public void redo() {
        exec();
    }
    
    
    public void addDataChangedEvent() {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                                        null,
                                                        m_testBase.getContainerTestNode(), 
                                                        m_nodeId);
        addEvent(event);
    }
    
}
