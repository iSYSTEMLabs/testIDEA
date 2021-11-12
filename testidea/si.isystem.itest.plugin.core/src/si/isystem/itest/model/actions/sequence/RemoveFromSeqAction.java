package si.isystem.itest.model.actions.sequence;

import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class RemoveFromSeqAction  extends AbstractAction {

    protected CTestBase m_testBase;

    private int m_idx;
    
    protected ENodeId m_nodeId;

    private int m_sectionId;

    private YamlScalar m_oldValue;

    
    public RemoveFromSeqAction(CTestBase testBase, int sectionIdx, int idx) {
        
        super("Remove from sequence: " + idx, testBase);
        
        m_testBase = testBase;
        m_sectionId = sectionIdx;
        m_idx = idx;
        
        CSequenceAdapter seq = new CSequenceAdapter(m_testBase, 
                                                    m_sectionId, 
                                                    true);
        m_oldValue = YamlScalar.newListElement(m_sectionId, m_idx);
        m_oldValue.setValue(seq.getValue(m_idx));
        m_oldValue.setNewLineComment(seq.getComment(CommentType.NEW_LINE_COMMENT, m_idx));
        m_oldValue.setEndOfLineComment(seq.getComment(CommentType.END_OF_LINE_COMMENT, m_idx));
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }

    
    @Override
    public void exec() {
        
        CSequenceAdapter seq = new CSequenceAdapter(m_testBase, 
                                                    m_sectionId, 
                                                    false);
        
        seq.remove(m_idx);
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
    }

    
    @Override
    public void undo() {
        
        CSequenceAdapter seq = new CSequenceAdapter(m_testBase, 
                                                    m_sectionId, 
                                                    false);
        seq.add(m_idx, m_oldValue.getValue());
        seq.setComment(m_idx, m_oldValue.getNewLineComment(), m_oldValue.getEndOfLineComment());
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

