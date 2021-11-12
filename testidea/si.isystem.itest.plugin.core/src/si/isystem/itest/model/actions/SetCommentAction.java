package si.isystem.itest.model.actions;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This action is useful for sections, where entire section contains one
 * string, for example description, tags, testId, ...
 * This class does not fire 'data change' event in exec(), because in some cases
 * it updates controls prematurely, for example when inserting lines in stubs 
 * assignments table. 
 * 
 * @author markok
 */
public class SetCommentAction extends AbstractAction {

    protected CTestBase m_testBase;

    private YamlScalar m_valueNew;
    private YamlScalar m_valueOld;
    
    protected ENodeId m_nodeId;

    /**
     * 
     * @param testBase
     * @param nodeId
     * @param scalar value part is ignored, only comments are set.
     */
    public SetCommentAction(CTestBase testBase,
                            ENodeId nodeId,
                            YamlScalar scalar) {
        
        super("Set section: " + scalar.getSectionId(), testBase);
        
        m_testBase = testBase;
        m_valueNew = scalar.copy();
        m_nodeId = nodeId;
        
        m_valueOld = scalar.copy(); // copy section id
        m_valueOld.dataFromTestSpec(m_testBase);
        
        // addDataChangedEvent(m_nodeId, m_testBase);
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }
    
    
    @Override
    public boolean isModified() {
        return !m_valueOld.equals(m_valueNew);
    }
    
    
    @Override
    public void exec() {
        m_valueNew.commentToTestSpec(m_testBase);
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
    }

    
    @Override
    public void undo() {
        m_valueOld.commentToTestSpec(m_testBase);
    }

    
    @Override
    public void redo() {
        exec();
    }
    
    
    /* private void addDataChangedEvent() {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                                        null,
                                                        m_testBase.getContainerTestSpec(), 
                                                        m_nodeId);
        addEvent(event);
    } */
}
