package si.isystem.itest.model.actions.sequence;

import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This action _inserts_ new element into sequence. After this operation sequence
 * always has one element more.
 *  
 * Note: If the sequence does not have enough
 * items to set one at the given index, that many empty items are added to the 
 * sequence.
 *  
 * @author markok
 *
 */
public class InsertToSequenceAction  extends AbstractAction {

    protected CTestBase m_testBase;

    private YamlScalar m_valueNew;
    
    protected ENodeId m_nodeId;

    private int m_numMissingItems;


    /**
     * 
     * @param testBase
     * @param value
     * @param value should be created with YamlScalar.newListElement()
     */
    public InsertToSequenceAction(CTestBase testBase,
                                  YamlScalar value) {
        
        super("Insert To Sequence Action: " + value.getSectionId(), testBase);
        
        m_testBase = testBase;
        m_valueNew = value.copy();
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }

    
    @Override
    public void exec() {
        
        CSequenceAdapter seq = new CSequenceAdapter(m_testBase, 
                                                    m_valueNew.getSectionId(), 
                                                    false);
        
        int indexOfNewEl = m_valueNew.getIndex();
        if (indexOfNewEl == -1) {
            indexOfNewEl = (int)seq.size();
        }
        m_numMissingItems = indexOfNewEl - (int)seq.size();
        for (int seqIdx = 0; seqIdx < m_numMissingItems; seqIdx++) {
            seq.add(-1, "");
        }
        
        seq.add(indexOfNewEl, m_valueNew.getValue());
        seq.setComment(indexOfNewEl, 
                       m_valueNew.getNewLineComment(), 
                       m_valueNew.getEndOfLineComment());
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
    }

    
    @Override
    public void undo() {
        
        CSequenceAdapter seq = new CSequenceAdapter(m_testBase, 
                                                    m_valueNew.getSectionId(), 
                                                    false);
        seq.remove(m_valueNew.getIndex());
        for (int seqIdx = 0; seqIdx < m_numMissingItems; seqIdx++) {
            seq.remove((int)seq.size() - 1);
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

