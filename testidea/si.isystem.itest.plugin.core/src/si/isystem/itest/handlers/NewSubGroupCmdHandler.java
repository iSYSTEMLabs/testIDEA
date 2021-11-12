package si.isystem.itest.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;

public class NewSubGroupCmdHandler extends NewTestGroupCmdHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {
            IStructuredSelection selection = UiUtils.getStructuredSelection();
            CTestTreeNode selectedTN = null;
            if (selection != null  &&  selection.size() > 0) {
                selectedTN = (CTestTreeNode)selection.getFirstElement();
                if (selectedTN.isGroup()) {
                    CTestGroup parentGroup = CTestGroup.cast(selectedTN);
                    CTestGroup parentOfParent = CTestGroup.cast(parentGroup.getParent());
                    int idx = -1;
                    if (parentOfParent != null) {
                        CTestBaseList childrenList = parentOfParent.getChildren(false);
                        idx = childrenList.find(parentGroup);
                    }
                    addNewGroup(parentGroup, idx);
                } else {
                    MessageDialog.openError(Activator.getShell(), 
                                            "Can not add group to test case!", 
                                            "Test cases may not contain groups. Please select group node to add a subgroup.");
                }
            } else {
                MessageDialog.openError(Activator.getShell(), 
                                        "Can not add sub-group!", 
                                        "Please select group node to add a subgroup.");
            }

        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not add sub-group!", ex);
        }
        
        return null;
    }

}
