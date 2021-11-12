package si.isystem.itest.launch;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.itest.main.Activator;

public class TestIDEALaunchConfigTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = null;

        if (mode.equals(ILaunchManager.DEBUG_MODE)) {
            
            tabs = new ILaunchConfigurationTab[] {
                    new FileListTab(),
                    new TestConfigurationTab(),
                    new CommonTab()
            };
        }
        
        else if (mode.equals(ILaunchManager.RUN_MODE)) {
            
            tabs = new ILaunchConfigurationTab[] {
                    new FileListTab(),
                    new TestConfigurationTab(),
                    new CommonTab()
            };
        }
        else
        {
            MessageDialog.openError(Activator.getShell(), 
                                    "Invalid launch mode",
                                    "Invalid launch mode '" + mode + "'.");
                                    
            Activator.log(IStatus.ERROR,
                          "Invalid launch mode '" + mode + "' - aborting.",
                          new Throwable());
        }

        setTabs(tabs);
    }

}
