package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;


/**
 * This class opens source file in winIDEA at line, where selected test 
 * specification starts. It works for YAML and C/C++ formats (for C/C++
 * format it opens the start of the test comment where test spec is located. 
 * However, if the user moves test specs around the tree and then saves the
 * data, line numbers in test specs in case of YAML format do not change, 
 * therefore this command will show invalid location. In case of C/C++ source
 * the location is updated on move, so it will be wrong before save, but OK 
 * after save. For consistent behavior update test spec locations for YAML
 * file on save. So in both cases the user will have to save the file to get 
 * correct mapping to source.
 * 
 * @author markok
 *
 */
public class ShowTestCaseInIYAMLSourceCmd extends AbstractHandler {

    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        CTestTreeNode selection = TestCaseEditorPart.getOutline().getTestSpecSelection();
        
        if (!selection.isGroup()) {
            
            CTestSpecification testSpec = CTestSpecification.cast(selection);
            
            if (testSpec != null  &&  testSpec.getSourceLineNumber() >= 0) {

                // +1 because it seems the parser passes previous line number 
                final int lineNumber = testSpec.getSourceLineNumber() + 1;
                final String fileName = TestSpecificationModel.getActiveModel().getModelFileName();
                JConnection jCon = ConnectionProvider.instance().getDefaultConnection();

                IIConnectOperation showSourceOperation = new IIConnectOperation() {

                    @Override
                    public void exec(JConnection jCon) {
                        WinIDEAManager.showSourceInEditor(jCon.getPrimaryCMgr(), 
                                                          fileName,
                                                          lineNumber);
                    }

                    @Override
                    public void setData(Object data) {}
                };


                ISysUIUtils.execWinIDEAOperation(showSourceOperation, Activator.getShell(), jCon);
            }
        }
        
        return null;
    }

}
