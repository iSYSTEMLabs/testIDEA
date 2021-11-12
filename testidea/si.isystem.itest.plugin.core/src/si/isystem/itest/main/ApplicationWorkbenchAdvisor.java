package si.isystem.itest.main;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import si.isystem.exceptions.SEFormatter;


public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = //"si.isystem.analyzer.perspective";
	                                             "si.isystem.itest.perspective";
	
	public ApplicationWorkbenchAdvisor(String[] commandLineArgs) {
	    if (commandLineArgs.length > 0  &&  commandLineArgs[0].equals("-version")) {
	        // if this product has been started by SWTBot, ignore cmd line args
	        commandLineArgs = new String[0];
	        Activator.ms_isSWTBotTest = true;
	    }
	    Activator.getDefault().setCmdLineArgs(commandLineArgs);
    }

	
    @Override
    public void initialize(IWorkbenchConfigurer wbConfigurer) {
        super.initialize(wbConfigurer);
        // To remember the user's layout and window size for the next time application
        // is started
        wbConfigurer.setSaveAndRestore(true);        
    }
	
    
    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

    
	@Override
    public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
	
	
	// this method is called by org.eclipse.ui.internal.ExceptionHandler.
	@Override
    public void eventLoopException(Throwable exception) {
	    super.eventLoopException(exception);
	    
	    // if exception occurs during application startup, see error log in
	    // .metadata for details about exception.
	    System.out.println("Exception in iSYSTEM ApplicationWorkbenchAdvisor!\n" + 
	                       SEFormatter.getInfoWithStackTrace(exception, 10));
	}

}
