package si.isystem.itest.ipc;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import si.isystem.commons.connect.ConnectCommandHandler;
import si.isystem.commons.connect.ConnectionPool;
import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JVersion;
import si.isystem.itest.common.CoreIdUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.data.ITestStatusLine;

public class ConnectionProvider implements IConnectionProvider {

    private static final ConnectionProvider INSTANCE = new ConnectionProvider();
    
    
    
    public static ConnectionProvider instance() {
        return INSTANCE;
    }


    @Override
    public JConnection getDefaultConnection() {
        ConnectionPool cPool = Activator.CP;
        JConnection jCon = cPool.getConnection(ConnectionPool.DEFAULT_CONNECTION);
        if (jCon != null) {
            return jCon;
        }

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
        return connectToWinIdea(false, envConfig);
    }

    
    @Override
    public JConnection getConnectionForId(String connectionId) {
        ConnectionPool cPool = Activator.CP;
        JConnection jCon = cPool.getConnection(connectionId);
        if (jCon != null) {
            return jCon;
        }

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
        return connectToWinIdea(false, envConfig);
    }
    
    /**
     * Connects to exiting winIDEA instance if exactly one matches given criteria 
     * (workspace and version), or starts a new instance if no running winIDEA
     * matches the criteria. If there is more than one winIDEA instance matching 
     * the criteria, an exception is thrown.
     * 
     * @param envConfig
     * @return valid connection
     * 
     * @exception if valid connection can not be established
     */
    public JConnection connectToWinIdeaHeadless(CTestEnvironmentConfig envConfig,
                                                MutableBoolean isAllCoresConnected,
                                                IProgressMonitor monitor) {

        StrVector loggingParams = new StrVector();
        envConfig.getLoggingParameters(loggingParams);
        final String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);

        String winIDEAWorkspace = Activator.getDefault().getConnectionWinIDEAWorkspaceName(envConfig);
        JVersion testIDEAVersion = Activator.getVersion();
        
        JConnection jCon = 
                new ConnectCommandHandler().connectToWinIdeaHeadless(Activator.CP,
                                                                     winIDEAWorkspace,
                                                                     testIDEAVersion,
                                                                     loggingParams,
                                                                     coreIds,
                                                                     monitor,
                                                                     isAllCoresConnected);
                                                                     
        if (jCon.isConnected()) {
            
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    Activator.getStatusLine().setMessage(ITestStatusLine.StatusImageId.CONNECTED, "");
                }});
        }

        return jCon;
    }


    /**
     * Connects to winIDEA. The way connection is performed depends on:
     * - winIDEA workspace file, given either as command line or in YAML file
     * - auto-connect flag

     * @param isConnectWithDialog if true, dialog with running winIDEAs is always shown.
     *                        
     * @return connection manager or null if user cancelled the connection. 
     *                    Callers should always check for null!                   
     */
    public JConnection connectToWinIdea(boolean isConnectWithDialog,
                                        CTestEnvironmentConfig envConfig) {

        StrVector loggingParams = new StrVector();
        envConfig.getLoggingParameters(loggingParams);
        final String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);

        String winIDEAWorkspace = Activator.getDefault().getConnectionWinIDEAWorkspaceName(envConfig);
        JVersion testIDEAVersion = Activator.getVersion();

        JConnection jCon = new ConnectCommandHandler().connectToWinIdea(Activator.getShell(), 
                                                                        Activator.CP, 
                                                                        winIDEAWorkspace, 
                                                                        testIDEAVersion, 
                                                                        loggingParams, 
                                                                        coreIds, 
                                                                        isConnectWithDialog,
                                                                        ConnectionPool.DEFAULT_CONNECTION);
        
        if (jCon != null  &&  jCon.isConnected()) {
            Activator.getStatusLine().setMessage(ITestStatusLine.StatusImageId.CONNECTED,
                                                 "");
        } else {
            Activator.getStatusLine().setMessage(ITestStatusLine.StatusImageId.DISCONNECTED,
                                                 "");
        }
        
        return jCon;
    }

    
    public String connectToSecondaryCores(final JConnection jCon, 
                                          final CTestEnvironmentConfig envConfig) {

        final String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);
        
        return new ConnectCommandHandler().connectToSecondaryCores(Activator.getShell(), 
                                                                   jCon, 
                                                                   coreIds);
    }
}
