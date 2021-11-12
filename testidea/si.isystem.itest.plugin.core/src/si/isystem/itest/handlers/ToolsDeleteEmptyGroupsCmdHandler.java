package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestGroup;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;

/**
 * Removes all groups, which have no child groups and no test cases. Has to
 * be execute more than once if user wants to get rid of all groups, which have 
 * no test cases, but have children - only bottom level children are removed on 
 * each call.   
 * 
 * @author markok
 *
 */
public class ToolsDeleteEmptyGroupsCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestGroup rootGrp = model.getRootTestGroup();
        
        GroupAction gAction = new GroupAction("Create groups with wizard");
        removeGroupsWithoutChildrenAndTestCases(rootGrp, gAction);
        
        gAction.addAllFireEventTypes();
        gAction.addTreeChangedEvent(null, null);
        model.execAction(gAction);
        
        return null;
    }


    private void removeGroupsWithoutChildrenAndTestCases(CTestGroup group, GroupAction gAction) {
        
        if (!group.hasChildren()  &&  !group.hasTestSpecs()  &&  group.getParent() != null) {
            AbstractAction delAction = new DeleteTestTreeNodeAction(group);
            gAction.add(delAction);
        } 

        CTestBaseList childGroups = group.getChildren(true);
        int numGroups = (int) childGroups.size();
        for (int idx = 0; idx < numGroups; idx++) {
            removeGroupsWithoutChildrenAndTestCases(CTestGroup.cast(childGroups.get(idx)), gAction);
        }        
    }
}
