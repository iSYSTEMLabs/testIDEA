package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.model.TestSpecificationModel;

public class ToolsConnectToWinIDEACmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        
        boolean isShift = false;
        if (event.getTrigger() instanceof Event) {
            Event ev = (Event)event.getTrigger();
            if ((ev.stateMask & SWT.SHIFT) != 0) {
                isShift = true;
            }
        }
        
        JConnection jCon = connect(isShift);
        
        if (jCon != null  &&  jCon.isConnected()) {
            // load also symbols, but if loading fails, do not show error message -
            // user will have to press refresh button to load symbols again or see the error.
            // This way we do not annoy user, who only needs license info for advanced
            // functionality.
            new ToolsRefreshGlobalsCmdHandler().refreshSymbolsAndUpdateEditor(false);
        }
        
        return null;
    }


    public JConnection connect(boolean isAlwaysShowDialog) {
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestEnvironmentConfig envConfig = null;
        if (model != null) {
            envConfig = model.getCEnvironmentConfiguration();
        } else {
            envConfig = new CTestEnvironmentConfig(null);
        }
        
        JConnection jCon = ConnectionProvider.instance().connectToWinIdea(isAlwaysShowDialog, 
                                                                          envConfig);
        return jCon;
    }
    
    
    public static boolean refreshGlobals() {

        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();

        if (jCon != null) {
            new ToolsRefreshGlobalsCmdHandler().refreshSymbols(true);
            return true;
        }
        
        return false;
    }
}
