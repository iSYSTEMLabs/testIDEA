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


public class RunSelectedCmdHandler extends AbstractHandler {

    private boolean m_isAddDerived = false;  // run one level below containerTestSpec, 
    // which contains selected test specifications
    
    public void setRunDerived(boolean isAddDerived) {
        m_isAddDerived = isAddDerived;
    }
    
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            TestSpecificationModel activeModel = TestSpecificationModel.getActiveModel();
            if (activeModel == null) {
                SExceptionDialog.open(Activator.getShell(), "No testIDEA editor is selected! Please select editor with testIDEA file.", new Exception());
                return null;
            }
            
            CTestSpecification containerTestSpec = UiUtils.getDirectTestsAndInSelectedGroups(m_isAddDerived);
            CTestGroup containerGroup = UiUtils.getSelectedGroups();
            
            if (!containerTestSpec.hasChildren()  &&  !containerGroup.hasChildren()) {
                MessageDialog.openError(Activator.getShell(), "Can not run selected test", 
                "No test case or group is selected, please select one!");
                return null;
            }
            
            TestRunner runner = new TestRunner();
            // symbols and groups are refreshed only if empty in runner
            runner.runTestUI(activeModel, 
                             containerTestSpec,
                             containerGroup,
                             1,
                             ToggleDebugModeHandler.isDebugMode(),
                             false,
                             ToggleDryRunHandler.isDryRunMode());
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not execute tests!", ex);
        }
        
        // for memory usage (leaks) info
        /*
        System.out.println("CTestResult counters: ctor = " + CTestResult.getM_ctorCounter() + 
                           "   dtor = " + CTestResult.getM_dtorCounter());
        Activator.printMemoryStatus();
        */
        return null;
    }
}
