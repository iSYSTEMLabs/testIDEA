package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.TestRunner;


public class RunSelectedInDebugModeHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            CTestSpecification containerTestSpec = UiUtils.getDirectTestsAndInSelectedGroups(false);

            if (containerTestSpec.getNoOfDerivedSpecs() == 0) {
                MessageDialog.openError(Activator.getShell(), "Can not run tests!", 
                "No test case is selected, please select one!");
                return null;
            }
            
            if (containerTestSpec.getNoOfDerivedSpecs() > 1) {
                MessageDialog.openError(Activator.getShell(), 
                                        "Too many test cases selected!", 
                                        "Please select only one test case!");
                return null;
            }
            
            int derivedLevel = 1; // no derived test specs. will be run, only the selected one
            TestRunner runner = new TestRunner();
            runner.runTestUI(TestSpecificationModel.getActiveModel(), 
                           containerTestSpec,
                           new CTestGroup(),   // groups can not be run in debug mode 
                           derivedLevel, 
                           true,
                           true,
                           ToggleDryRunHandler.isDryRunMode());
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not execute tests!", ex);
        }
        
        return null;
    }

}
