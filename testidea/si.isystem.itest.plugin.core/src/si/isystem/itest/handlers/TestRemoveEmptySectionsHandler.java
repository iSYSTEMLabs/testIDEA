package si.isystem.itest.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.IntVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.RemoveEmptySectionsAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.ui.spec.TestTreeOutline;

/**
 * Removes empty sections in the selected test case. Consider expanding it to
 * remove also empty sections in derived test cases.
 * 
 * @author markok
 *
 */
public class TestRemoveEmptySectionsHandler  extends AbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model == null) {
            SExceptionDialog.open(Activator.getShell(), "No testIDEA editor is selected! Please select editor with testIDEA file.", new Exception());
            return null;
        }

        TestTreeOutline outlineView = TestCaseEditorPart.getOutline();
        IStructuredSelection structSelection = (IStructuredSelection)outlineView.getSelection();

        if (structSelection == null) {
            return null;
        }
            
        GroupAction group = new GroupAction("Remove empty sections");
        Iterator<?> iter = structSelection.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof CTestTreeNode) {
                
                CTestTreeNode testNode = (CTestTreeNode)item;

                addActionsRecursively(group, testNode);
            }
        }
        
        group.addAllFireEventTypes();
        group.addTreeChangedEvent(null, null);
        model.execAction(group);

        return null;
    }

    
    private void addActionsRecursively(GroupAction group, CTestBase testNode) {
        // clear section, in which user has cleared all items. This way user has 
        // the ability to remove sections also in table editor, otherwise he has to
        // go to normal editor to remove it, for example empty stub or test point.
        RemoveEmptySectionsAction action = new RemoveEmptySectionsAction(testNode);
        if (action.isModified()) {
            group.add(action);
        }
        
        IntVector sections = new IntVector();
        testNode.getSectionIds(sections);
        
        for (int sectionIdx = 0; sectionIdx < sections.size(); sectionIdx++) {
            int section = sections.get(sectionIdx);
            
            if (testNode.getSectionType(section) == ETestObjType.ETestBase) {
                
                CTestBase childTestBase = testNode.getTestBase(section, true);
                if (!childTestBase.isEmpty()) {
                    addActionsRecursively(group, childTestBase);
                }
                
            } else if (testNode.getSectionType(section) == ETestObjType.ETestBaseList) {
                CTestBaseList tbl = testNode.getTestBaseList(section, true);
                for (int tidx = 0; tidx < tbl.size(); tidx++) {
                    CTestBase listTb = tbl.get(tidx);
                    if (!listTb.isEmpty()) {
                        addActionsRecursively(group, listTb);
                    }
                }
            }
        }
    }
}
