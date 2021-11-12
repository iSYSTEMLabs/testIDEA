package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.RenameDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;

public class ToolsRenameCmdHandler extends AbstractHandler {

    private static RenameDialog m_dlg;
    
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();

        try {

            if (m_dlg == null) {
                // lazy initialization, keeps changes in dialog persistent
                m_dlg = new RenameDialog(shell);
            }
            
            CTestSpecification testSpec = TestSpecificationModel.getActiveModel().getRootTestSpecification();
            m_dlg.setTestSpec(testSpec);
            m_dlg.show();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not generate script!", 
                                  ex);
        }
        
        return null;
    }
}
