package si.isystem.itest.handlers;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestStub;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.wizards.ExtensionScriptWizard;

public class ToolsScriptExtensionsWizard  extends AbstractHandler {
    

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();

        try {
            List<CTestSpecification> testCases = UiUtils.getSelectedTestTreeSpecifications();
            Map<String, CTestStub> stubsMap = new TreeMap<>();
            Map<String, CTestPoint> testPointsMap = new TreeMap<>();
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (CTestSpecification testSpec : testCases) {
                if (i < 3) { // only the first three test cases are printed in the wizard,
                    // because MANY can be selected and there is no space to print them all. 
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(testSpec.getTestId());
                    sb.append(":");
                    sb.append(testSpec.getFunctionUnderTest(true).getName());
                    if (i > 2) {
                        sb.append(", ...");
                    }
                    i++;
                }
                CTestBaseList stubs = testSpec.getStubs(true);
                for (int j = 0; j < stubs.size(); j++) {
                    CTestStub stub = CTestStub.cast(stubs.get(j));
                    String stubbedFuncName = stub.getFunctionName();
                    if (!stubsMap.containsKey(stubbedFuncName)) {
                        stubsMap.put(stubbedFuncName, stub);
                    }
                }
                    
                CTestBaseList testPoints = testSpec.getTestPoints(true);
                for (int j = 0; j < testPoints.size(); j++) {
                    CTestPoint tp = CTestPoint.cast(testPoints.get(j));
                    String testPointId = tp.getId();
                    if (!testPointsMap.containsKey(testPointId)) {
                        testPointsMap.put(testPointId, tp);
                    }
                }
            }
            String selectedTests;
            selectedTests = sb.toString();

            // get Python module and class info
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            CScriptConfig cfg = model.getCEnvironmentConfiguration().getScriptConfig(true);
            
            WizardDialog dlg = new WizardDialog(shell, new ExtensionScriptWizard(selectedTests,
                                                                                 cfg.getExtensionClass(),
                                                                                 testCases,
                                                                                 stubsMap,
                                                                                 testPointsMap));
            dlg.open();
            // applySettings already performed in the wizard
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not generate script!", 
                                  ex);
        }
        
        return null;
    }
    
}
