package si.isystem.itest.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelVerifier;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecStatus;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.StatusView;

public class ToolsVerifyCmdHandler extends AbstractHandler {

    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        ToolsRefreshGlobalsCmdHandler cmd = new ToolsRefreshGlobalsCmdHandler();
        cmd.refreshSymbols(true);
        
        try {
            final ModelVerifier modelVerifier = ModelVerifier.instance();
            List<TestSpecStatus> statusList = modelVerifier.verifyAll();
            
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            if (modelVerifier.askForAutoSetSaveAnalyzerFile(model, model.getRootTestGroup())) {
                // if model was changed, repeat symbol analysis
                statusList = modelVerifier.verifyAll();
            }

            StatusModel.instance().setTestSpecStatus(statusList);
            
            if (statusList.isEmpty()) {
                StatusView.getView().setDetailPaneText(StatusType.OK, "No problems found!");
            } else {
                StatusModel.instance().updateTestResults(null);
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Symbol verification failed!", ex);
        }
        
        return null;
    }
}
