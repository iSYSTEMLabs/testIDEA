/**
 * This package contains classes for establishing and maintaining connections 
 * to winIDEA. Client plug-ins should use it in the following way:
 * 
 *  1. Create one instance of class ConnectionPool in your application, for
 *     example as a member of some singleton or plug-in Activator.
 *  2. Create a class which implements 
 *     interface {@link IConnectionProvider}. An example of its methods is shown 
 *     below (it contains methods from the interface and two methods to be called
 *     from plug-in when connection is needed):
 * 
 *  <pre>
 *    //@Override
 *    public JConnection getDefaultConnection() {
 *        // instance of ConnectionPool in client's plug-in
 *        {@link ConnectionPool} cPool = Activator.CP;
 *        
 *        // if connection already exists, return it
 *        {@link JConnection} jCon = cPool.getConnection(ConnectionPool.DEFAULT_CONNECTION);
 *        if (jCon != null) {
 *            return jCon;
 *        }
 *
 *        // get connection configuration from client's model and connect
 *        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
 *        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
 *        return connectToWinIdea(false, false, envConfig);
 *    }
 *
 *    
 *    //@Override
 *    public JConnection getConnectionForId(String connectionId) {
 *        ConnectionPool cPool = Activator.CP;
 *        JConnection jCon = cPool.getConnection(connectionId);
 *        if (jCon != null) {
 *            return jCon;
 *        }
 *
 *        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
 *        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
 *        return connectToWinIdea(false, false, envConfig);
 *    }
 *    
 *
 *    // This is hint for method in 'Connect' command handler.
 *    // Parameter isConnectWithDialog is set if SHIFT key is pressed:
 *    //
 *    // public Object execute(ExecutionEvent event) {
 *    //
 *    //      boolean isConnectWithDialog = false;
 *    //      if (event.getTrigger() instanceof Event) {
 *    //          Event ev = (Event)event.getTrigger();
 *    //          if ((ev.stateMask & SWT.SHIFT) != 0) {
 *    //                 isConnectWithDialog = true;
 *    //          }
 *    //      }
 *    // ...
 * 
 *    /**
 *     * This method should be called only from 'Connect' command handler, 
 *     * because user requested explicit connection, and
 *     * from this class. It converts plug-in specific parameters to parameters
 *     * required by ConnectCommandHandler.
 *    * /
 *    public {@link JConnection} connectToWinIdea(boolean isConnectWithDialog,
 *                                                CTestEnvironmentConfig envConfig) {
 *
 *        StrVector loggingParams = new StrVector();
 *        
 *        // Get logging parameters from your configuration, or leave them empty
 *        // (but not null). The first item is log file name, the second is log
 *        // prefix. Both are optional.
 *        envConfig.getLoggingParameters(loggingParams);
 *        
 *        // Get core IDs from your configuration. Required if you want to make
 *        // connection to multiple cores.
 *        final String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);
 *
 *        // Get workspace of winIDEA to connect to and current application version. 
 *        String winIDEAWorkspace = Activator.getDefault().getConnectionWinIDEAWorkspaceName(envConfig);
 *        JVersion testIDEAVersion = Activator.getVersion();
 *
 *        // this one will contain testIDEA license after return. 
 *        MutableObject<ELicenseType> license = new MutableObject<>();
 *        
 *        JConnection jCon = new ConnectCommandHandler().connectToWinIdea(Activator.getShell(), 
 *                                                                        Activator.CP, 
 *                                                                        winIDEAWorkspace, 
 *                                                                        testIDEAVersion, 
 *                                                                        loggingParams, 
 *                                                                        coreIds, 
 *                                                                        isConnectWithDialog,
 *                                                                        ConnectionPool.DEFAULT_CONNECTION,
 *                                                                        license);
 *
 *        // update UI according to connection status
 *        Activator.setApplicationTitle(license.getValue());
 *
 *        if (jCon.isConnected()) {
 *            Activator.getStatusLine().setMessage(ITestStatusLine.StatusImageId.CONNECTED,
 *                                                 "");
 *        } else {
 *            Activator.getStatusLine().setMessage(ITestStatusLine.StatusImageId.DISCONNECTED,
 *                                                 "");
 *        }
 *        
 *        return jCon;
 *    }
 *
 *
 *    public String connectToSecondaryCores(final JConnection jCon, 
 *                                          final CTestEnvironmentConfig envConfig) {
 *                                                 
 *        // Get core IDs from your configuration. 
 *        final String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);
 *        
 *        return new ConnectCommandHandler().connectToSecondaryCores(Activator.getShell(), 
 *                                                                   jCon, 
 *                                                                   coreIds);
 *    }
 *
 *  </pre>
 */

package si.isystem.commons.connect;

