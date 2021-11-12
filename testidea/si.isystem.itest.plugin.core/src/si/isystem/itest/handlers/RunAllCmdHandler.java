package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.TestRunner;


public class RunAllCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
 
        CTestSpecification testSpec = null;

        try {
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            
            // make sure symbols and groups are always refreshed, so that 
            // results reflect current state.
            refreshTestCasesInGroups(model);
            
            testSpec = model.getRootTestSpecification();
            CTestGroup rootGroup = model.getRootTestGroup();

            TestRunner runner = new TestRunner();
            runner.runTestUI(model, 
                             testSpec,
                             rootGroup,
                             Integer.MAX_VALUE,
                             ToggleDebugModeHandler.isDebugMode(),
                             false,
                             ToggleDryRunHandler.isDryRunMode());  // run also derived test specs - all test specs 
                                   // are derived from root test spec

        } catch (Exception ex) {
            if (testSpec == null) {
                testSpec = new CTestSpecification();    // dummy test case 
                testSpec.setTestId("testSpec == null"); // just for support info
            }
            SExceptionDialog.open(Activator.getShell(), "Can not execute test!\n  Test ID: " + 
                                  testSpec.getTestId() + 
                                  "\n  Function: '" + testSpec.getFunctionUnderTest(true).getName() + "'", ex);
        }

        return null;
    }

    
    protected void refreshTestCasesInGroups(TestSpecificationModel activeModel) {
        
        CTestBench testBench = activeModel.getTestBench();
        testBench.getFilterController().clear(); // indicate refresh is needed
        
        JConnection jConnectionMgr = ConnectionProvider.instance().getDefaultConnection();
        TestRunner.refreshSymbolsAndGroups(jConnectionMgr, 
                                           testBench.getTestEnvironmentConfig(true), 
                                           testBench);
    }
}
