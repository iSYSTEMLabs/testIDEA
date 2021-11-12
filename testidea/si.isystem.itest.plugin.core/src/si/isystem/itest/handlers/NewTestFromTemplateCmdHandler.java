package si.isystem.itest.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.TemplateSelectionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;

public class NewTestFromTemplateCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {
            TemplateSelectionDialog dlg = new TemplateSelectionDialog(Activator.getShell());

            if (dlg.show()) {
                // this class contains methods for adding test specs to test tree
                EditPasteCmdHandler pastingClass = new EditPasteCmdHandler();

                List<CTestSpecification> selectedTestSpecs = UiUtils.getSelectedTestTreeSpecifications();
                CTestSpecification selectedTS;
                int sameLevelPasteIdx = -1;
                if (selectedTestSpecs.isEmpty()) {
                    selectedTS = TestSpecificationModel.getActiveModel().getRootTestSpecification();
                } else {
                    selectedTS = selectedTestSpecs.get(0);
                    CTestSpecification selectedParentTestSpec = selectedTS.getParentTestSpecification();
                    sameLevelPasteIdx = selectedParentTestSpec.findDerivedTestSpec(selectedTS) + 1;
                }
                
                if (dlg.isAddAsDerived()) {
                    
                    int pasteIdx = selectedTS.getNoOfDerivedSpecs();

                    pastingClass.pasteTestSpec(dlg.getSelectedTestSpecifications(), 
                                               pasteIdx, 
                                               selectedTS, 
                                               true);
                } else {
                    pastingClass.pasteTestSpec(dlg.getSelectedTestSpecifications(), 
                                               sameLevelPasteIdx, 
                                               selectedTS, 
                                               true);
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can create new test from template!", ex);
        }
        
        return null;
    }

    @Override 
    public boolean isEnabled() {
        return true;
    }
}
