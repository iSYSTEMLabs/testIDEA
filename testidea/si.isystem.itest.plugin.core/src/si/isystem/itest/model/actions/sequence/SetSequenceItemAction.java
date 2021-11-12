package si.isystem.itest.model.actions.sequence;

import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This action _sets_ item in user sequence. If index of the new element is -1
 * or greater than the existing number of elements in the sequence, then element 
 * is added to the sequence. Otherwise the existing element is overwritten with
 * new data.  
 * Note: If the sequence does not have enough
 * items to set one at the given index, that many empty items are added to the 
 * sequence. 
 *  
 * @author markok
 */
public class SetSequenceItemAction extends AbstractAction {

    protected CTestBase m_testBase;

    private YamlScalar m_valueNew;
    private YamlScalar m_valueOld;
    
    protected ENodeId m_nodeId;

    private int m_originalSize = -1;

    /**
     * 
     * @param testBase
     * @param nodeId
     * @param value should be created with YamlScalar.newListElement()
     */
    public SetSequenceItemAction(CTestBase testBase,
                                 ENodeId nodeId,
                                 YamlScalar value) {
        super("Set sequence item in section: " + value.getSectionId(), testBase);
        
        m_testBase = testBase;
        m_valueNew = value.copy();
        m_nodeId = nodeId;
        m_valueOld = m_valueNew.copy(); // copy section id
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }
    
    
    @Override
    public boolean isModified() {
        return true; // if multiple actions will be added as part of the
        // same GroupAction, it is not possible or very difficult to know what the
        // model will look like when this action will be executed - other actions
        // may change the sequence!
    }
    
    
    @Override
    public void exec() {
        int idx = m_valueNew.getIndex();
        CSequenceAdapter seqAdapter = new CSequenceAdapter(m_testBase, 
                                                           m_valueNew.getSectionId(), 
                                                           false);
        int seqSize = (int)seqAdapter.size();
        
        if (m_valueNew.getValue().isEmpty()) {
            // if cell is deleted (empty string written), then remove all empty 
            // cells in the tail of sequence
            
            if (idx < seqSize) {
                
                m_valueOld.dataFromTestSpec(m_testBase);
                m_valueNew.dataToTestSpec(m_testBase); // clear the cell
                
                int ridx = seqSize - 1;
                for (;ridx >= 0; ridx--) { // remove empty cells
                    if (!seqAdapter.getValue(ridx).isEmpty()) {
                        break;
                    }
                }
                
                if (ridx < seqSize - 1) {
                    m_originalSize = seqSize;
                    seqAdapter.resize(ridx + 1);
                    // refresh of ArrayTableModel is needed, since seq size has changed
                    addAllFireEventTypes(); 
                }
            } else {
                m_valueOld = null; // nothing to do when writing empyy content 
                                   // over the seq boundaries
            }
        } else {
            if (seqSize <= idx) {
                m_originalSize = (int)seqAdapter.size();
                seqAdapter.resize(idx + 1);
                // refresh of ArrayTableModel is needed, since seq size has changed
                addAllFireEventTypes();
            }
//            int numItemsAdded = idx - m_originalSize + 1;
//            for (int i = 0; i < numItemsAdded; i++) {
//                seqAdapter.add(-1, ""); // can not use resize, because it 
//                                        // initializes with NULL shared_ptr
//            }

            m_valueOld.dataFromTestSpec(m_testBase);
            m_valueNew.dataToTestSpec(m_testBase);
        }
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
        // fireEventsOnExec();
    }

    
    @Override
    public void undo() {
        CSequenceAdapter seqAdapter = new CSequenceAdapter(m_testBase, 
                                                           m_valueNew.getSectionId(), 
                                                           false);
        if (m_originalSize >= 0) {
            seqAdapter.resize(m_originalSize);
        } 

        // if old value existed at old sequence size index  
        if (m_valueOld != null  &&  seqAdapter.size() > m_valueOld.getIndex()) {
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
    
    
    public void addTreeChangedEvent() {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                        null,
                                                        m_testBase.getContainerTestNode(), 
                                                        m_nodeId);
        addEvent(event);
    }
}
