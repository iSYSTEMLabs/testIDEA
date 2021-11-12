package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.TestRunner;


/**
 * This command handler runs all tests, which have faild on previous run.
 * See also RunSelectedCmdHandler()
 */
public class RunFailedCmdHandler extends AbstractHandler {

    private int m_derivedLevel = 1;  // run one level below containerTestSpec, 
    // which contains selected test specifications
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        CTestSpecification containerTestSpec = new CTestSpecification();
        
        try {
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            CTestReportContainer testResults = model.getTestReportContainer();

            if (testResults.getNoOfTestResults() == 0  &&  testResults.getNoOfTestGroupResults() == 0) {
                MessageDialog.openError(Activator.getShell(), "Nothing to run", 
                "No tests which have failed on previous run found!");
                return null;
            }
            
            testResults.resetTestResultIterator();
            while (testResults.hasNextTestResult()) {
                CTestResult testResult = testResults.nextTestResult();
                if (testResult.isError()) {
                    CTestSpecification testSpec = testResult.getTestSpecification();
                    // Result has reference to merged test spec. Parent of merged test spec.
                    // is it's original test specification.
                    containerTestSpec.addDerivedTestSpec(-1, testSpec.getParentTestSpecification());
                }
            }

            if (containerTestSpec.getNoOfDerivedSpecs() == 0) {
                MessageDialog.openError(Activator.getShell(), "Nothing to run", 
                "No tests which have failed on previous run found!");
                return null;
            }
            
            CTestGroup rootGroup = model.getRootTestGroup();
            CTestGroup containerGroup = new CTestGroup();
            getGroupsWithFailedResults(rootGroup, containerGroup.getChildren(false), testResults);
            
            TestRunner runner = new TestRunner();
            runner.runTestUI(TestSpecificationModel.getActiveModel(), 
                             containerTestSpec,
                             containerGroup,
                             m_derivedLevel,
                             ToggleDebugModeHandler.isDebugMode(),
                             false,
                             ToggleDryRunHandler.isDryRunMode());
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not execute tests!", ex);
        }
        
        return null;
    }

    
    private CTestGroup getGroupsWithFailedResults(CTestGroup group, 
                                                  CTestBaseList containerList, 
                                                  CTestReportContainer testResults) {
        
        CTestGroupResult groupResult = testResults.getGroupResult(group);
        
        if (groupResult != null  &&  groupResult.isError()) {
            containerList.add(-1, group);
        }
        
        CTestBaseList children = group.getChildren(true);
        int numChildren = (int) children.size();
        for (int idx = 0; idx < numChildren; idx++) {
            CTestGroup childGrp = CTestGroup.cast(children.get(idx));
            getGroupsWithFailedResults(childGrp, containerList, testResults);
        }
        
        return null;
    }
}

 