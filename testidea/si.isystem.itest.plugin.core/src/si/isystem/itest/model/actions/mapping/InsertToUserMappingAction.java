package si.isystem.itest.model.actions.mapping;

import java.util.List;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.StrVector;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This class inserts new pair into mapping. If the key already exists, only value
 * is modified.
 * 
 * @author markok
 *
 */
public class InsertToUserMappingAction extends AbstractAction {

    protected CTestBase m_testBase;
    List<String> m_predecessors;

    private YamlScalar m_valueNew;
    private YamlScalar m_valueOld = null;
    
    protected ENodeId m_nodeId;

    /**
     * 
     * @param testBase
     * @param value
     * @param predecessors list of keys, which should be in mapping before the 
     * pair inserted with this action, if the key does not exist in the map.
     * If null, and the key does not exist, then it is added to the end of the 
     * mapping. If the key exists, only value is replaced. 
     */
    public InsertToUserMappingAction(CTestBase testBase,
                                     YamlScalar value, // should be created with YamlScalar.newUserMapping() 
                                     List<String> predecessors) {
        
        super("Insert To User Mapping Action: " + value.getSectionId(), testBase);
        
        m_testBase = testBase;
        m_valueNew = value.copy();
        m_predecessors = predecessors;
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_valueNew.getSectionId(), 
                                                  true);
        
        if (userMapping.contains(value.getKey())) {
            m_valueOld = value.copy(); // copy section id
            m_valueOld.dataFromTestSpec(testBase);
        }
    }
    
    public boolean isNewItemAlreadyExist() {
        return m_valueOld != null;
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }
    
    
    @Override
    public boolean isModified() {
        return m_valueOld == null  ||  !m_valueOld.equals(m_valueNew);
    }
    
    
    @Override
    public void exec() {
        
        CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                  m_valueNew.getSectionId(), 
                                                  false);

        String newKey = m_valueNew.getKey();
        if (m_predecessors != null) {
            StrVector cPredecessors = new StrVector();
            for (String p : m_predecessors) {
                cPredecessors.add(p);
            }

            userMapping.insertKey(newKey, cPredecessors);

            m_valueNew.dataToTestSpec(m_testBase);
        } else {
            userMapping.setValue(newKey, m_valueNew.getValue());
        }
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
    }

    
    @Override
    public void undo() {
        
        if (m_valueOld == null) {
            // there was no such key before insertion, remove it
            CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                      m_valueNew.getSectionId(), 
                                                      false);
            userMapping.removeEntry(m_valueNew.getKey());
        } else {
            // restore the value
            m_valueOld.dataToTestSpec(m_testBase);
        }
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
