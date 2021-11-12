package si.isystem.itest.model.actions.mapping;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class RemoveFromUserMappingAction extends AbstractAction {

    protected CTestBase m_testBase;
    private int m_sectionId;
    private String m_key;

    private YamlScalar m_valueOld = null;
    
    protected ENodeId m_nodeId;
    private String m_predecessor = null;

    
    public RemoveFromUserMappingAction(CTestBase testBase,
                                       int section, 
                                       String key) {
        
        super("Remove From User Mapping Action: " + key, testBase);
        
        m_testBase = testBase;
        m_sectionId = section; 
        m_key = key;
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        if (userMapping.contains(m_key)) {
            m_valueOld = YamlScalar.newUserMapping(m_sectionId, m_key);
            m_valueOld.dataFromTestSpec(testBase);
            StrVector keys = new StrVector();
            userMapping.getKeys(keys);
            for (int keyIdx = 0; keyIdx < keys.size(); keyIdx++) {
                if (keys.get(keyIdx).equals(m_key)) {
                    if (keyIdx > 0) {
                        m_predecessor  = keys.get(keyIdx - 1);
                    }
                    break;
                }
            }
            // if m_predecessor == null now, it means the key was removed at index 0 
        } else {
            throw new SIllegalStateException("Can not remove item from user mapping - it does not exist!").
            add("key", m_key);
        }
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }
    
    
    @Override
    public void exec() {
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        userMapping.removeEntry(m_key);
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
    }

    
    @Override
    public void undo() {
        
        // there was no such key before removal, do nothing
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId, 
                                                  false);
        StrVector cPredecessors = new StrVector();
        if (m_predecessor != null) {
            cPredecessors.add(m_predecessor);
        }   
        userMapping.insertKey(m_key, cPredecessors);
        m_valueOld.dataToTestSpec(m_testBase);
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