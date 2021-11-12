package si.isystem.itest.model.actions;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.TestSpecificationModel;

public class DeleteTestTreeNodeAction extends AbstractAction {

    private CTestTreeNode m_testSpecOrGroup;
    private int m_index = -1;
    private TestSpecificationModel m_model;
    
    /**
     * @param testSpecOrGroup test specification to delete, or test specification which
     *        children should be deleted. See isDeleteChildren.
     * @param isDeleteChildren if false then testSpec is deleted. If true, then
     *        testSpec is only a container of test specifications, which should
     *        be deleted.
     */
    public DeleteTestTreeNodeAction(CTestTreeNode testSpecOrGroup) {
        super("Delete", testSpecOrGroup);
        m_testSpecOrGroup = testSpecOrGroup;
        m_model = TestSpecificationModel.getActiveModel();

        addEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED, 
                                       m_testSpecOrGroup.getParentNode(), 
                                       m_testSpecOrGroup));
        
    }
    
    
    /**
     * Fills group action with DeleteActions for all children of the parent test spec.
     * 
     * @param group
     * @param container
     */
    public static void fillGroupAction(GroupAction group, CTestBench container) {
        
        fillGroupAction(group, container.getTestSpecification(true));
        fillGroupAction(group, container.getGroup(true));
        
        group.addEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED, 
                                             null, null));
        group.addEvent(new ModelChangedEvent(EventType.UPDATE_TEST_RESULTS, 
                                             null, null));
        group.addAllFireEventTypes();
    }


    private static void fillGroupAction(GroupAction group, CTestTreeNode parentNode) {
        CTestBaseList children = parentNode.getChildren(true);
        int numDerived = (int) children.size();
        for (int i = 0; i < numDerived; i++) {
            group.add(new DeleteTestTreeNodeAction(CTestTreeNode.cast(children.get(i))));
        }
    }    
    
//    public static void fillGroupAction(GroupAction group, CTestSpecification parentTestSpec) {
//        int numDerived = parentTestSpec.getNoOfDerivedSpecs();
//        for (int i = 0; i < numDerived; i++) {
//            group.add(new DeleteTestSpecAction(parentTestSpec.getDerivedTestSpec(i)));
//        }
//        group.addEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED, 
//                                             parentTestSpec, null));
//        group.addEvent(new ModelChangedEvent(EventType.UPDATE_TEST_RESULTS, 
//                                             null, null));
//        group.addAllFireEventTypes();
//    }
    
    
    @Override
    public void exec() {
        m_index = m_model.deleteTreeNode(m_testSpecOrGroup);
    }

    
    @Override
    public void undo() {
        CTestTreeNode parent = m_testSpecOrGroup.getParentNode();
        m_model.addTestTreeNode(parent, m_index, m_testSpecOrGroup);
    }

    
    @Override
    public void redo() {
        exec();
    }
}
