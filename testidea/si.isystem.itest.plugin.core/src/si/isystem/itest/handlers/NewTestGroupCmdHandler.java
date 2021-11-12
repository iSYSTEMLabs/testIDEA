package si.isystem.itest.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.NewTestGroupDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.AddTestTreeNodeAction;

public class NewTestGroupCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        addNewGroup(null, -1);
        
        return null;
    }

    
    protected void addNewGroup(CTestGroup parentGroup, int idx) {
        
    TestSpecificationModel model = TestSpecificationModel.getActiveModel();
    if (parentGroup == null) {
        parentGroup = model.getRootTestGroup();
    }
    
    try {

        CTestSpecification containerTestSpec = TestSpecificationModel.getActiveModel().getRootTestSpecification();
        
        List<CTestFilter> filterList = new ArrayList<>();

        CTestGroup tmpGroup = parentGroup;
        while (tmpGroup != null) {
            filterList.add(tmpGroup.getFilter(true));
            tmpGroup = tmpGroup.getParentGroup();
        }
        
        NewTestGroupDialog dlg = new NewTestGroupDialog(Activator.getShell(),
                                                        containerTestSpec,
                                                        filterList.toArray(new CTestFilter[0]));

        if (dlg.show()) {
            CTestGroup newGroup = new CTestGroup(parentGroup);
            
            newGroup.setTagValue(ESectionCTestGroup.E_SECTION_GROUP_ID.swigValue(), 
                                 dlg.getGroupId());
            
            CTestFilter groupFilter = newGroup.getFilter(false);
            groupFilter.assign(dlg.getFilter());
            groupFilter.setParent(newGroup); // parent is assigned by assign(), restore it
            
            AddTestTreeNodeAction action = new AddTestTreeNodeAction(model,
                                                                     parentGroup, 
                                                                     idx, 
                                                                     newGroup);
            action.addAllFireEventTypes();
            model.execAction(action);
        }
    } catch (Exception ex) {
        SExceptionDialog.open(Activator.getShell(), "Can not add new base test specification!", ex);
    }
}
    
}
