package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ui.spec.TestTreeOutline;

public class EditSelectAllCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        TestTreeOutline outline = TestCaseEditorPart.getOutline();
        if (outline != null) {
            outline.selectAll();
        }
        
        return null;
    }

}
