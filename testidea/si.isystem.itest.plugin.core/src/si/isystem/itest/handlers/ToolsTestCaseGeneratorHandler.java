package si.isystem.itest.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBaseList.InsertToTestBaseListAction;
import si.isystem.itest.model.actions.testBaseList.RemoveFromTestBaseListAction;
import si.isystem.itest.wizards.TestCaseGeneratorWizard;

public class ToolsTestCaseGeneratorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Shell shell = Activator.getShell();

        try {
            List<CTestSpecification> testCases = UiUtils.getSelectedTestTreeSpecifications();
            
            if (testCases.isEmpty()) {
                throw new IllegalArgumentException("No test case is selected. Please select "
                        + "one test case before running this wizard!");
            }
            
            if (testCases.size() > 1) {
                throw new IllegalArgumentException("To many test cases are selected. Please select "
                        + "one test case before running this wizard!");
            }
            
            // test case generator wizard now takes merged test case as a 
            // template when creating new sections. Otherwise is creates empty
            // sections, it its parent has the sections inherited from top 
            // parent, for example: 
            //    A->B->'generated tests' 
            // If test case B has section stubs merged, then generated
            // test cases should use stubs section from test case A.            
            CTestSpecification selectedTestCase = testCases.get(0);
            CTestSpecification mergedTestCase = selectedTestCase.merge(); 
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            String coreId = mergedTestCase.getCoreId();
            if (model != null) {
                coreId = model.getConfiguredCoreID(coreId);
            }

            TestCaseGeneratorWizard wizard = new TestCaseGeneratorWizard(selectedTestCase,
                                                                         mergedTestCase,
                                                                         coreId);
            WizardDialog dlg = new WizardDialog(shell, wizard);
            dlg.setPageSize(800, 800);
            if (dlg.open() == Window.OK) {

                // applySettings already performed in the wizard
                GroupAction group = new GroupAction("Add Generated Test Cases");
                CTestBaseList tbList = 
                        selectedTestCase.getTestBaseList(SectionIds.E_SECTION_TESTS.swigValue(), 
                                                         false);
                
                if (wizard.isDeleteExistingDerivedTestCases()) {
                    int numDerived = selectedTestCase.getNoOfDerivedSpecs();
                    for (int idx = numDerived - 1; idx >= 0; idx--) {
                        RemoveFromTestBaseListAction action = new RemoveFromTestBaseListAction(tbList, idx);
                        group.add(action);
                    }
                }
                
                
                List<CTestSpecification> generatedTestCases = wizard.getGeneratedTestCases();
                
                for (CTestSpecification genTC : generatedTestCases) {
                    
                    InsertToTestBaseListAction action = 
                        new InsertToTestBaseListAction(tbList, genTC, -1);
                    group.add(action);
                }
                
                group.addAllFireEventTypes();
                group.addTreeChangedEvent(selectedTestCase.getParentTestSpecification(), selectedTestCase);
                TestSpecificationModel.getActiveModel().execAction(group);
            }

        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not generate test cases!", 
                                  ex);
        }

        return null;
    }

}
