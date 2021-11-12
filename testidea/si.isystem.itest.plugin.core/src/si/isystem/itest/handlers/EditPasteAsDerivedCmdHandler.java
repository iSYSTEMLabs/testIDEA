package si.isystem.itest.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;

public class EditPasteAsDerivedCmdHandler extends EditPasteCmdHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {
//            TestSpecificationEditorView.saveGUIData();
            
            CTestTreeNode selectedTestNode = getNodesSelectedInTestTree();

            if (selectedTestNode == null) {
                MessageDialog.openError(Activator.getShell(), "No base test specification", 
                "Please select a test specification when pasting derived test specifications!");
                return null;
            } 
            
            int pasteIdx = (int) selectedTestNode.getChildren(true).size();

            pasteFromClipboard(selectedTestNode, pasteIdx);
            
        } catch (Exception ex) {
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
