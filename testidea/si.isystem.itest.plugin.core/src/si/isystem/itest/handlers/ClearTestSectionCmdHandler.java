package si.isystem.itest.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;

public class ClearTestSectionCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            IStructuredSelection structSelection = UiUtils.getStructuredSelection();
            if (structSelection == null) {
                return null;
            }

            @SuppressWarnings("rawtypes")
            Iterator iter = structSelection.iterator();

            while (iter.hasNext()) {
                Object item = iter.next();
                if (item instanceof TreeNode) {
                    @SuppressWarnings("unchecked")
                    TreeNode<EditorSectionNode> node = (TreeNode<EditorSectionNode>)item;
                    
                    // this method submits asction to model's action queue
                    node.getData().getSectionEditor().clearSection();
                } else {
                    throw new SIllegalStateException("Invalid selection type! Only sections " +
                    		"of test specification are supported!").
                    		add("itemType", item.getClass().getSimpleName());
                }
            }
            
            // TestSpecificationEditorView editor = (TestSpecificationEditorView)(PlatformUI.getWorkbench().
            //        getActiveWorkbenchWindow().getActivePage().findView(TestSpecificationEditorView.ID));
            /* CTestSpecification testSpec = null; // editor.getInput();
            TestSpecificationModel.getInstance().fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_DATA_CHANGED, 
                                                                                 testSpec, 
                                                                                 testSpec));
                                                                                 */
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Clear command failed!", ex);
        }
        return null;
    }

}
