package si.isystem.itest.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;

public class NewDerivedTestCmdHandler extends NewBaseTestCmdHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            IStructuredSelection selection = UiUtils.getStructuredSelection();
            CTestTreeNode selectedTB = null;
            if (selection != null  &&  selection.size() > 0) {
                selectedTB = (CTestTreeNode)selection.getFirstElement();
                CTestSpecification selectedTS = null;
                
                if (selectedTB.isGroup()) {
                    // these leaf groups are rendered as test cases in group tree
                    CTestGroup group = CTestGroup.cast(selectedTB);
                    if (group.isTestSpecOwner()) {
                        selectedTS = group.getOwnedTestSpec();
                    }
                } else {
                    selectedTS = CTestSpecification.cast(selectedTB);
                }
                
                if (selectedTS != null) {
                    CTestSpecification mergedTestSpec = selectedTS.merge();
                    addNewTest(selectedTS, 
                               !mergedTestSpec.isSectionEmpty(SectionIds.E_SECTION_ASSERT.swigValue()),
                               true);
                }
            }

        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not add new derived test!", ex);
        }
        return null;
    }

}
