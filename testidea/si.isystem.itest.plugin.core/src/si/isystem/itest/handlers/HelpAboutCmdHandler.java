/*
 * No longer neede, used default Eclipse About box. 
package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.itest.ui.spec.Activator;

public class HelpAboutCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        String version = "0.9";
        MessageDialog.openInformation(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), 
                                      "About isystem.test wizard", 
                                      "isystem.test Wizard\n" +
                                      "Version: " + version + "\n" +
                                      "\n" +
                                      "(c) iSYSTEM AG, 2010\n");
        
        return null;
    }

}
*/