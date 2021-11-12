package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestGroup;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.GroupCreationDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.AddTestTreeNodeAction;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;

public class ToolsCreateGroupsCmdHandler extends AbstractHandler {

    GroupCreationDialog m_dlg;
    private CTestGroup m_groupToBeRevealdInOutlineView;
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Shell shell = Activator.getShell();
        
        long noOfGroups = 0;
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestGroup rootGrp = model.getRootTestGroup();
        noOfGroups = rootGrp.getNoOfAllChildrenInHierarchy();

        try {
            m_dlg = new GroupCreationDialog(shell, noOfGroups);
            
            if (m_dlg.show()) {
                CTestGroup containerGrp = m_dlg.getData();
                CTestBaseList addedGroups = containerGrp.getChildren(true);
                int numAddedGroups = (int) addedGroups.size();
                if (numAddedGroups == 0) {
                    return null;
                }
                
                GroupAction gAction = new GroupAction("Create groups with wizard"); 
                for (int idx = 0; idx < numAddedGroups; idx++) {
                    CTestGroup addedGroup = CTestGroup.cast(addedGroups.get(idx));
                    AbstractAction action = new AddTestTreeNodeAction(model, rootGrp, -1, addedGroup);
                    gAction.add(action);
                }

                gAction.addAllFireEventTypes();
                if (numAddedGroups > 0) {
                    CTestGroup newGrp = CTestGroup.cast(addedGroups.get(0));
                    gAction.addTreeChangedEvent(null, newGrp);
                }
                model.execAction(gAction);

                // show test cases in groups immediately
                model.refreshGroups();
                
                gAction = new GroupAction("Delete test group(s)");
                if (!m_dlg.isCreateEmptyGroups()) {
                    m_groupToBeRevealdInOutlineView = null;
                    removeNewAndEmptyGroups(containerGrp, gAction);
                    
                    if (!gAction.isEmpty()) {
                        gAction.addTreeChangedEvent(null, m_groupToBeRevealdInOutlineView);
                        model.execAction(gAction);
                    }
                }
                
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not create groups!", 
                                  ex);

        }
        
        return null;
    }

    
    private void removeNewAndEmptyGroups(CTestGroup group, GroupAction gAction) {
        
        if (!group.hasChildren()  &&  !group.hasTestSpecs()) {
            AbstractAction delAction = new DeleteTestTreeNodeAction(group);
            gAction.add(delAction);
        } else if (m_groupToBeRevealdInOutlineView == null  &&  group.getParentNode() != null) {
            m_groupToBeRevealdInOutlineView = group;
        }

        CTestBaseList childGroups = group.getChildren(true);
        int numGroups = (int) childGroups.size();
        for (int idx = 0; idx < numGroups; idx++) {
            removeNewAndEmptyGroups(CTestGroup.cast(childGroups.get(idx)), gAction);
        }        
    }
}
