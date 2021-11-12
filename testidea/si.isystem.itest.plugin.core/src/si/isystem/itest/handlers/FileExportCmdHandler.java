package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.TestTreeOutline;
import si.isystem.itest.wizards.ExportWizard;

public class FileExportCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Shell shell = Activator.getShell();
        
        // get selection directly from the tree view, even if the view currently 
        // does not have focus.
        TestTreeOutline outline = TestCaseEditorPart.getOutline();
        if (outline == null) {
            MessageDialog.openInformation(shell, 
                "Nothing to export!", 
                "Please, select iSYSTEM Test Tase Editor and at least one test specification in Outline view.");
            return null;            
        }
        
        ITreeSelection selection = (ITreeSelection)outline.getSelection();
        
        ExportWizard exportWizard = new ExportWizard();
        exportWizard.init(PlatformUI.getWorkbench(), selection);
        
        WizardDialog wizardDlg = new WizardDialog(shell, exportWizard);
        wizardDlg.open();

        return null;
    }
}
