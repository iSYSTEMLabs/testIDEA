package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestReportConfig;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.SaveTestReportDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;

public class TestConfigureReportHandler extends AbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();

        try {
            
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            CTestReportConfig modelReportConfig = model.getTestReportConfig();

            SaveTestReportDialog dlg = new SaveTestReportDialog(shell,
                                                                "Configure test reports",
                                                                modelReportConfig);
            if (dlg.show()) {
                
                CTestReportConfig newReportConfig = dlg.getTestReportConfig();
                if (!modelReportConfig.equalsData(newReportConfig)) {
                    modelReportConfig.assign(newReportConfig);
                    model.setModelDirty(true);
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(shell, "Can not save test report!", ex);
        }

        return null;
    }
}