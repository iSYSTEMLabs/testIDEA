package si.isystem.itest.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.AutoIdGenerator;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.wizards.newtest.NewTCWizard;

public class NewBaseTestCmdHandler extends AbstractHandler implements IHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        addNewTest(null, false, false);
        
        return null;
    }
    

    protected void addNewTest(CTestSpecification parentTestSpec, 
                              boolean isParentExpectedSectionDefined, 
                              boolean isDerived) {
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        
        try {
            String defaultRetValName = model.getCEnvironmentConfiguration().getDefaultReturnValueName();

            // copy test scope (UNIT or SYSTEM test) from parent test case, if available
            ETestScope testScope = null;
            if (parentTestSpec != null  &&  
                !parentTestSpec.isSectionEmpty(SectionIds.E_SECTION_TEST_SCOPE.swigValue())) {
                
                testScope = parentTestSpec.getTestScope(); 
            }
            
            NewTCWizard wizard = new NewTCWizard(defaultRetValName,
                                                 isDerived ? "New derived test case wizard" : "New test case wizard",
                                                 testScope);
            WizardDialog dlg = new WizardDialog(Activator.getShell(), wizard);
            dlg.setPageSize(980, 400);
            if (dlg.open() == Window.OK) {
                AbstractAction action = wizard.getNewTestCaseAction(wizard.getNtcModel(),
                                                                    parentTestSpec,
                                                                    model,
                                                                    isParentExpectedSectionDefined);
                action.addAllFireEventTypes();
                model.execAction(action);
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not add new base test specification!", ex);
        }
    }
    
    
    public static void autoGenerateTestId(CTestSpecification testSpec, AutoIdGenerator autoIdGenerator) {
        int derivedTestSpecIdx = 0;
        String nidPrefix = "";
        CTestSpecification parentTestSpec = testSpec.getParentTestSpecification();
        if (parentTestSpec != null) {
            derivedTestSpecIdx = parentTestSpec.getNoOfDerivedSpecs();
            nidPrefix = parentTestSpec.getTestId();
        }
        
        Map<String, String> vars = autoIdGenerator.createVars(testSpec, 
                                                              derivedTestSpecIdx, 
                                                              nidPrefix,
                                                              "");
        String format = 
            TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration().getAutoIdFormatString();
        String testId = autoIdGenerator.createTestId(format, vars);
        testSpec.setTestId(testId);
    }

    
    /**
     * @param functionName function given by winIDEA, if ever called  
     */
    public void setFunctionName(String functionName) {
        // It seems winIDEA never calls this. If it does, pass m_functionName to
        // NewTCWizard.
        StatusView.getView().setDetailPaneText(StatusType.ERROR, 
                                               "Creation of test cases from winIDEA failed.\n"
                                               + "Please contact support@isystem.com.");
        // m_functionName = functionName;
    }
}
