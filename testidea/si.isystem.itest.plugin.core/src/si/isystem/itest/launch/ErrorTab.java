package si.isystem.itest.launch;

import net.miginfocom.swt.MigLayout;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import si.isystem.ui.utils.KGUIBuilder;

public class ErrorTab extends AbstractLaunchConfigurationTab {

    private static final String LICENSE_ERROR_TITLE = "Launch configurations are available only in testIDEA Professional.";


    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        comp.setLayout(new MigLayout("", "[min]", "[min!][min!][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(comp);

        builder.label(LICENSE_ERROR_TITLE, "wrap");
        builder.label("Please make sure your emulator is turned on, and it contains testIDEA Pro license.",
                      "wrap");
        builder.label("Note: You can use simple run functionality in testIDEA perspective.",
                      "gaptop 10"); 
    }


    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        // TODO Auto-generated method stub

    }


    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        return false;
    }


    @Override
    public String getErrorMessage() {
        return LICENSE_ERROR_TITLE;
    }
    
    
    @Override
    public boolean canSave() {
        return false;
    }


    @Override
    public String getName() {
        return "Error";
    }


    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }
}
