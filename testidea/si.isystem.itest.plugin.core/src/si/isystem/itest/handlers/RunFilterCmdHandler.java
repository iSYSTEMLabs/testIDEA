package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.TestExecutionFilterDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.EmptyAction;
import si.isystem.itest.run.TestRunner;

public class RunFilterCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
 
        CTestSpecification testSpec = null;
        try {
//            TestSpecificationEditorView.saveGUIData();

            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            testSpec = model.getRootTestSpecification();

            CTestBaseList oldFilters = model.getTestFilters();
            CTestBaseList testFilters = new CTestBaseList();
            testFilters.assign(oldFilters);
            TestExecutionFilterDialog dlg = new TestExecutionFilterDialog(Activator.getShell(), 
                                                                          testFilters);
            
            if (dlg.show()) {
                // save any possible changes
                int noOfFilters = (int)testFilters.size();
                int noOfOldFilters = (int)oldFilters.size();
                boolean isEqual = true;
                if (noOfFilters == noOfOldFilters) {
                    for (int i = 0; i < noOfFilters; i++) {
                        CTestFilter filter = CTestFilter.cast(testFilters.get(i));
                        CTestFilter oldFilter = CTestFilter.cast(oldFilters.get(i));
                        if (!filter.equalsData(oldFilter)) {
                            isEqual = false;
                            break;
                        }
                    }
                } else {
                    isEqual = false;
                }
                
                if (!isEqual) {
                    model.setTestFilters(testFilters);
                    model.execAction(new EmptyAction("Test Filter - not undoable!"));
                }
                
                CTestFilter testFilter = dlg.getSelectedFilter();
                CTestGroup rootGroup = model.getRootTestGroup();
                TestRunner runner = new TestRunner(testFilter);
                runner.runTestUI(model, 
                               testSpec,
                               rootGroup,
                               Integer.MAX_VALUE,
                               ToggleDebugModeHandler.isDebugMode(),
                               false,
                               ToggleDryRunHandler.isDryRunMode());  // run also derived test specs - all test specs 
                // are derived from root test spec
            }
            return null;
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not execute test!\n  Test ID: " + 
                                 (testSpec != null ? testSpec.getTestId() : "") + 
                                  "\n  Function: '" + 
                                  (testSpec != null ? testSpec.getFunctionUnderTest(true).getName() : "") 
                                  + "'", ex);
        }
        
        return null;
    }

}
