package si.isystem.itest.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;

import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.ui.spec.data.TreeNode;

public class DeleteTestSpecsCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            
            IStructuredSelection selection = UiUtils.getStructuredSelection();
            
            if (selection != null  &&  selection.size() > 0) {
                
                if (selection.getFirstElement() instanceof CTestTreeNode) {
                    // outline view is active
                    deleteTestTreeNodesFromOutlineView(selection);
                } else if (selection.getFirstElement() instanceof TreeNode) {
                    // editor section is active
                    // deleteTestCaseActiveInFormEditor(testSpec);
                    new ClearTestSectionCmdHandler().execute(null);
                }
            }
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not delete test specification!", ex);
        }

        return null;
    }

    /*
    private  void deleteTestCaseActiveInFormEditor(CTestSpecification testSpec) {
        TestCaseEditorPart editor = TestCaseEditorPart.getActive();
        if (editor != null) {
            CTestSpecification ts = editor.getActiveTestCase();
            
            DeleteTestSpecAction action = new DeleteTestSpecAction(testSpec);
            action.addEvent(new ModelChangedEvent(EventType.UPDATE_TEST_RESULTS, 
                                                       null, null));
            action.addAllFireEventTypes();
            
            TestSpecificationModel.getActiveModel().execAction(action);
        }
    } */

    
    private void deleteTestTreeNodesFromOutlineView(IStructuredSelection selectedTests) {
        CTestTreeNode testTreeNode;
            
        @SuppressWarnings("rawtypes")
        Iterator it = selectedTests.iterator();
        GroupAction groupAction = new GroupAction("Delete test specification(s)");

        while (it.hasNext()) {
            Object selectedItem = it.next();

            if (selectedItem instanceof CTestTreeNode) {
                testTreeNode = (CTestTreeNode)(selectedItem);

                groupAction.add(new DeleteTestTreeNodeAction(testTreeNode));
                
            } // ignored, there is ClearTestSectionCmdHandler for sections 
            // else if (selectedItem instanceof TreeNode) {}
        } 

        groupAction.addEvent(new ModelChangedEvent(EventType.UPDATE_TEST_RESULTS, 
                                                   null, null));
        groupAction.addAllFireEventTypes();

        TestSpecificationModel.getActiveModel().execAction(groupAction);
    }

}
