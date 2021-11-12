package si.isystem.itest.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.OverwriteTestAction;

public class EditPasteAndOverwriteCmdHandler extends EditPasteCmdHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            if (model == null) {
                return null;
            }

            String yamlSpec = readTestSpecFromClipboard();

            if (yamlSpec != null) {

                boolean isPastedMultiple = false;
                // it there is a list of test specs, prepend 'tests:'
                if (yamlSpec.charAt(0) == '-') {
                    yamlSpec = "tests:\n" + yamlSpec;
                    isPastedMultiple = true;
                }

                // paste test spec to model
                CTestBench pastedTestBench = UiUtils.parseTestCasesAndGroups(yamlSpec, isPastedMultiple);
                
                // First we check if there test specs to be pasted. Test spec and groups can not 
                // be overwritten at the same time, so groups should not be selected if test cases are
                // selected. Error will be reported later when pasting. 
                CTestTreeNode pastedNode = pastedTestBench.getTestSpecification(true);
                if (pastedNode.hasChildren()) {
                    pastedNode = CTestTreeNode.cast(pastedNode.getChildren(true).get(0));
                } else {
                    CTestGroup rootGroup = pastedTestBench.getGroup(true);
                    if (rootGroup.hasChildren()) {
                        pastedNode = CTestTreeNode.cast(rootGroup.getChildren(true).get(0));
                    } else {
                        throw new SIllegalArgumentException("No test item is selected.");
                    }
                }

                GroupAction groupAction = new GroupAction("Paste and overwrite");

                CTestBench destTB = UiUtils.getSelectedOutlineNodes(UiUtils.getStructuredSelection(), false);
                CTestTreeNode destNodesContainer = destTB.getTestSpecification(true); 
                if (!destNodesContainer.hasChildren()) {
                    destNodesContainer = destTB.getGroup(true);
                    if (!destNodesContainer.hasChildren()) {
                        throw new SIllegalArgumentException("No destination item for pasting selected!");
                    }
                }
                
                CTestBaseList destNodesList = destNodesContainer.getChildren(true);
                int noOfItems = (int) destNodesList.size();
                for (int idx = 0; idx < noOfItems; idx++) {
                    CTestTreeNode destNode = CTestTreeNode.cast(destNodesList.get(idx));
                    OverwriteTestAction overwriteTestAction = new OverwriteTestAction(destNode, 
                                                                                      pastedNode);
                    overwriteTestAction.addDataChangedEvent(null, destNode);
                    groupAction.add(overwriteTestAction);
                }
                groupAction.addTreeChangedEvent(null, null);
                groupAction.addAllFireEventTypes();
                model.execAction(groupAction);
            } 
        } catch (Exception ex) {
            // ignored - do nothing, if there is invalid test data on the clipboard
            SExceptionDialog.open(Activator.getShell(), "Paste Error!", ex);
        }

        return null;
    }

    
    @Override
    public boolean isEnabled() {
        TestCaseEditorPart editor = TestCaseEditorPart.getActive();
        return editor != null;
    }    
}
