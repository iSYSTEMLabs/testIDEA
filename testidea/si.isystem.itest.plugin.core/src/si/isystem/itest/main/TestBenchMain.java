package si.isystem.itest.main;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import si.isystem.exceptions.SExceptionDialog;


/**
 * This class controls all aspects of the application's execution.
 */
public class TestBenchMain implements IApplication {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	@Override
    public Object start(IApplicationContext context) {
	    SExceptionDialog.setPrintToStdOut(true);
		Display display = PlatformUI.createDisplay();
		try {
		    String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor(args));
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			
			if (returnCode != PlatformUI.RETURN_OK) {
			    MessageDialog.openError(display.getShells()[0], "Error running the application", 
			                            "See '.log' file in '.metadata' directory and contact iSYSTEM support team.\nError code: " + returnCode);
			}
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	@Override
    public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) {
			return;
		}
		
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			@Override
            public void run() {
				if (!display.isDisposed()) {
					workbench.close();
				}
			}
		});
	}
}
