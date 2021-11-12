package si.isystem.itest.model.actions;

import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.TestSpecificationModel;

/**
 * This class add nes CTEstSpecification or CTestGroup to the given main application model.
 * @author markok
 *
 */
public class AddTestTreeNodeAction extends AbstractAction {

    private TestSpecificationModel m_model;

    private CTestTreeNode m_parentTestTreeNode;
    private CTestTreeNode m_newTestTreeNode;
    private int m_pasteIdx = -1;

    public AddTestTreeNodeAction(TestSpecificationModel model,
                                 CTestTreeNode parentTestTreeNode, 
                                 int pasteIdx, 
                                 CTestTreeNode newTestTreeNode) {
        
        super("New test tree node", newTestTreeNode);
        m_model = model;
        m_parentTestTreeNode = parentTestTreeNode;
        m_pasteIdx  = pasteIdx;
        m_newTestTreeNode = newTestTreeNode;
        m_newTestTreeNode.setParent(m_parentTestTreeNode);

        addEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED, 
                                       m_parentTestTreeNode, 
                                       m_newTestTreeNode));
    }
    
    
    @Override
    public void exec() {
        m_model.addTestTreeNode(m_parentTestTreeNode, m_pasteIdx, m_newTestTreeNode);
    }

    
    @Override
    public void undo() {
        m_model.deleteTreeNode(m_newTestTreeNode);
    }


    @Override
    public void redo() {
        exec(); 
    }
}
