package si.isystem.commons.connect;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JVersion;
import si.isystem.connect.utils.WinIDEAEnumerator;
import si.isystem.connect.utils.WinIDEAEnumerator.WinIDEAInfo;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalStateException;

/**
 * This class creates connections to winIDEA. It has two sets of connect...()
 * methods. The first ones should be called from UI thread, since they may
 * pop-up a dialog to get the user an option to select winIDEA to connect to.
 * The headless versions of connect...() methods may be called from other threads.
 * 
 * This is the main class from this package to be used by UI applications.
 * 
 * @author markok
 *
 */
public class ConnectCommandHandler {

    private static final int WR_NO_INSTANCE = -1;
    private static final int WR_TO_MANY_INSTANCES = -2;

    
    /**
     * Connects to exiting winIDEA instance if exactly one matches given criteria 
     * (workspace and version), or starts a new instance if no running winIDEA
     * matches the criteria. If there is more than one winIDEA instance matching 
     * the criteria, an exception is thrown. Call this method when running from 
     * non-UI thread and user interaction is not desired - for example for batch 
     * runs.
     * 
     * @param cPool plug-in specific connection pool 
     * @param winIDEAWorkspace workspace file of winIDEA to try to connect to
     * @param testIDEAVersion version of winIDEA to try to connect to
     * @param loggingParams may be null for no logging, parameter at index 0 is 
     *                      logging file name, optional parameter at index 1 is
     *                      prefix for log statements - each connection should have
     *                      its distinct prefix.
     * @param coreIds application specific coreIds. These IDs are defined by caller,
     *               but the same core ID must be used when obtaining controllers
     *               from connection. The number of elements in this array also
     *               defined the number of cores to connect to.  
     * @param monitor monitor to show progress.
     * @param license this is a return parameter, which will contain license type.
     *                If null, license is not read from winIDEA.
     * @param isAllCoresConnected return value, true when all cores have been 
     *                            successfully connected.
     * 
     * @return valid connection
     * 
     * @exception if valid connection can not be established
     */
    public JConnection connectToWinIdeaHeadless(ConnectionPool cPool,
                                                String winIDEAWorkspace,
                                                JVersion testIDEAVersion,
                                                StrVector loggingParams,
                                                String[] coreIds,
                                                IProgressMonitor monitor,
                                                MutableBoolean isAllCoresConnected) {

        monitor.subTask("Enumerating winIDEAs. Check that all dialogs in winIDEA are closed.\n"
                      + "If this dialog does not close, try to switch off emulator or close winIDEA.");
        
        WinIDEAEnumerator enumerator = new WinIDEAEnumerator();
        List<WinIDEAInfo> instanceInfoList = enumerator.enumerate();

        int listIdx;
        
        if (!winIDEAWorkspace.isEmpty()) {
            listIdx = isWinIDEARunning(instanceInfoList, winIDEAWorkspace, testIDEAVersion);
        } else {
            listIdx = isOnlyOneWinIDEARunning(instanceInfoList, testIDEAVersion);
        }
        
        if (listIdx >= 0) {
            monitor.subTask("Connecting. Check that all dialogs in winIDEA are closed.\n"
                    + "If this dialog does not close, try to switch off emulator or close winIDEA.");
            boolean allConnected = connectHeadless(cPool, instanceInfoList.get(listIdx),
                                                   loggingParams, coreIds);
            isAllCoresConnected.setValue(allConnected);
        } else if (listIdx == WR_NO_INSTANCE) {
            monitor.subTask("Connecting. Check that all dialogs in winIDEA are closed.\n"
                    + "If this dialog does not close, try to switch off emulator or close winIDEA.");
            boolean allConnected = connectHeadless(cPool, null,
                                                   loggingParams, coreIds);
            isAllCoresConnected.setValue(allConnected);
        } else {
            throw new SIllegalStateException("Can not make deterministic connection to winIDEA, "
                    + "because to many winIDEA instances matching connection criteria are running!").
                    add("workspace", winIDEAWorkspace).
                    add("version", testIDEAVersion.toString());
        }

        JConnection jCon = cPool.getConnection(ConnectionPool.DEFAULT_CONNECTION);
        
        if (jCon.isConnected()) {
            
//            Display.getDefault().syncExec(new Runnable() {
//                @Override
//                public void run() {
//                    A.getStatusLine().setMessage(ITestStatusLine.StatusImageId.CONNECTED, "");
//                }});
            
            monitor.subTask("Reading license. Check that all dialogs in winIDEA are closed.\n"
                    + "If this dialog does not close, try to switch off emulator or close winIDEA.");
        }
        
        return jCon;
    }
    
    
    private boolean connectHeadless(ConnectionPool cPool,
                                    WinIDEAInfo winIDEAInfo,
                                    StrVector loggingParams,
                                    String[] coreIds) {
        
        String winIDEAWorkspace = winIDEAInfo != null ? winIDEAInfo.getWorkspace() : "";
        String winIDEAId= winIDEAInfo != null ? winIDEAInfo.getInstanceId() : null;
        
        JConnection jCon = createConnection(cPool, winIDEAWorkspace, winIDEAId, 
                                            loggingParams, coreIds);
        
        // connects to other cores if up and running, otherwise connections can 
        // be made during init sequence
        StringBuilder coresWithoutWinIDEALaunched = new StringBuilder();
        
        tryToConnectOtherCores(jCon, coresWithoutWinIDEALaunched, coreIds);
        
        return coresWithoutWinIDEALaunched.length() == 0;
    }
    
    
    /**
     * This method returns existing connection if it exists, otherwise it 
     * pops-up the connection dialog.
     * Call this method when running from UI thread and user interaction is 
     * desired to establish connection to the right winIDEA instance.
     * 
     * <p>
     * If multiple cores are used, this method tries to establish connections
     * only to already launched winIDEAs for other cores. It does not force
     * winIDEA launching, because sometimes download to primary core must be made
     * before other cores are launched, and that is job for init sequence.
     * Call connectToSecondaryCores() to create all connections.
     *
     * @param parentShell shell used as parent when opening a connection dialog 
     * @param cPool plug-in specific connection pool 
     * @param winIDEAWorkspace workspace file of winIDEA to try to connect to
     * @param testIDEAVersion version of winIDEA to try to connect to
q     * @param loggingParams may be null for no logging, parameter at index 0 is 
     *                      logging file name, optional parameter at index 1 is
     *                      prefix for log statements - each connection should have
     *                      its distinct prefix.
     * @param coreIds application specific coreIds. These IDs are defined by caller,
     *               but the same core ID must be used when obtaining controllers
     *               from connection. The number of elements in this array also
     *               defined the number of cores to connect to.  
     * @param isAutoConnectToWinIDEA if true, connection is done automatically
     * @param isConnectWithDialog if true, dialog with running winIDEAs is always shown,
     *                       regardless of current connection status. It is not shown 
     *                       only if application was started from winIDEA.
     * @param connectioinId id of created connection. Use ConnectionPool.DEFAULT_CONNECTION
     *                      if your application will support only one emulator. 
     * @param license this is a return parameter, which will contain license type.
     *                If null, license is not read from winIDEA.
     *
     * @return connection or null if user cancelled the connection. 
     *                    Callers should always check for null!                   
     */
    public JConnection connectToWinIdea(Shell parentShell,
                                        ConnectionPool cPool,
                                        String winIDEAWorkspace,
                                        JVersion testIDEAVersion,                                         
                                        StrVector loggingParams,
                                        String [] coreIds,
                                        boolean isConnectWithDialog,
                                        String connectionId) {
        
        JConnection jCon = cPool.getConnection(connectionId);
        
        // if started from winIDEA, IConnectClient detects env vars set by winIDEA
        // and ignores id and workspace name, but always connects to original winIDEA. 
        if (ConnectionMgr.isStartedFromWinIDEA()) {
        
            if (isConnectWithDialog) {
                // show this message to avoid confusion when testIDEA was started from winIDEA 
                // and user keeps pressing SHIFT. 
                MessageDialog.openInformation(parentShell, 
                                              "testIDEA started from winIDEA", 
                                              "This instance of testIDEA was started from winIDEA, so it always connects"
                                              + "to this winIDEA.\n"
                                              + "Connection dialog is therefore not available - SHIFT key has no effect.");
            }
            
            if (jCon == null  ||  !jCon.isConnected()) {
                cPool.disconnect(connectionId);
                
                jCon = connectWithProgressMonitor(parentShell,
                                                  cPool,
                                                  "", 
                                                  null,
                                                  loggingParams,
                                                  coreIds);
            }

        } else {
        
            try {
                WinIDEAEnumerator enumerator = new WinIDEAEnumerator();
                List<WinIDEAInfo> instanceInfoList = enumerator.enumerate();

                if (isConnectWithDialog) {
                    jCon = connectWithDialog(parentShell, cPool, 
                                             winIDEAWorkspace, testIDEAVersion,
                                             loggingParams, connectionId, coreIds);
                } else if (!winIDEAWorkspace.isEmpty()) {
                    int listIdx = isWinIDEARunning(instanceInfoList, winIDEAWorkspace, testIDEAVersion);
                    if (listIdx != WR_NO_INSTANCE  &&  listIdx != WR_TO_MANY_INSTANCES) {
                        jCon = connectWithProgressMonitor(parentShell, cPool, 
                                                          instanceInfoList.get(listIdx),
                                                          loggingParams,
                                                          coreIds);
                    } else {
                        jCon = connectWithDialog(parentShell, cPool, 
                                                 winIDEAWorkspace, testIDEAVersion,
                                                 loggingParams, connectionId, coreIds);
                    }
                } else {
                    int listIdx = isOnlyOneWinIDEARunning(instanceInfoList, testIDEAVersion);
                    if (listIdx != WR_NO_INSTANCE  &&  listIdx != WR_TO_MANY_INSTANCES) {
                        jCon = connectWithProgressMonitor(parentShell, cPool,
                                                          instanceInfoList.get(listIdx),
                                                          loggingParams,
                                                          coreIds);
                    } else {
                        jCon = connectWithDialog(parentShell, cPool,
                                                 winIDEAWorkspace, testIDEAVersion,
                                                 loggingParams, connectionId, 
                                                 coreIds);
                    }
                }
            } catch (Exception ex) {
                MessageDialog.openError(parentShell, 
                                        "Connection Error", 
                                        "Connection to winIDEA failed! Fix the problem, " +
                                                "then select 'Tools | Connect'!\n" + ex.getMessage());
            }
        }
      
        return jCon;
    }


    private int isWinIDEARunning(List<WinIDEAInfo> instanceInfoList,
                                 String configWS,
                                 JVersion testIDEAVersion) {

        int matchedIdx = WR_NO_INSTANCE;
        int idx = 0;
        for (WinIDEAInfo info : instanceInfoList) {
            JVersion winIDEAVer = new JVersion(info.getVerMajor(), 
                                               info.getVerMinor(), 
                                               info.getVerBuild());
            String winIDEAWS = info.getWorkspace();
            Path winIDEAPath = Paths.get(winIDEAWS);
            Path configPath =  Paths.get(configWS);
            
            // prefer winIDEA with the same version and workspace
            if (winIDEAVer.equals(testIDEAVersion)  &&  (configWS.isEmpty()  ||  configPath.equals(winIDEAPath))) {
                if (matchedIdx != WR_NO_INSTANCE) {
                    return WR_TO_MANY_INSTANCES; // more than one matching winIDEA is running
                }
                matchedIdx = idx;
            }
            idx++;
        }
        
        return matchedIdx;
    }


    /**
     * @param instanceInfoList
     * @param testIDEAVersion
     * @return index of winIDEA if there is only one of the selected version, or
     * -1 if there are no winIDEA or more than one with the selected version
     */
    private int isOnlyOneWinIDEARunning(List<WinIDEAInfo> instanceInfoList, 
                                        JVersion testIDEAVersion) {
        int idx = 0;
        int matchedIdx = WR_NO_INSTANCE;
        for (WinIDEAInfo info : instanceInfoList) {
            JVersion winIDEAVer = new JVersion(info.getVerMajor(), 
                                               info.getVerMinor(), 
                                               info.getVerBuild());
            
            // prefer winIDEA with the same version and workspace
            if (winIDEAVer.equals(testIDEAVersion)) {
                if (matchedIdx != WR_NO_INSTANCE) {
                    return WR_TO_MANY_INSTANCES;
                }
                matchedIdx = idx;
            }
            idx++;
        }
        
        return matchedIdx;
    }


    /** Returns string with list of unconnected cores or null if connection failed. */
    private JConnection connectWithDialog(Shell parentShell,
                                          ConnectionPool cPool, 
                                          String winIDEAWorkspace, 
                                          JVersion testIDEAVersion,
                                          StrVector loggingParams,
                                          String connectionId,
                                          String[] coreIds) {
        
        ConnectionDialog dlg = new ConnectionDialog(parentShell, 
                                                    winIDEAWorkspace,
                                                    testIDEAVersion);
        if (dlg.show()) {
            int selectedIdx = dlg.getSelectedIdx();
            List<WinIDEAInfo> instanceInfoList = dlg.getInfoList();
            if (selectedIdx >= 0) {
                WinIDEAInfo winIDEAInfo = instanceInfoList.get(selectedIdx);
                cPool.disconnect(connectionId);
                return connectWithProgressMonitor(parentShell, cPool, winIDEAInfo,
                                                  loggingParams, coreIds);
            } else {
                // should never happen
                MessageDialog.openError(parentShell, "Internal error!", "No instance was selected!");
            }
        } else {
            // if user canceled the dialog, keep connection
            return cPool.getConnection(connectionId);
        }
        
        return null;
    }
    

    /** Returns string with list of unconnected cores. */
    private JConnection connectWithProgressMonitor(Shell parentShell, 
                                                   ConnectionPool cPool, 
                                                   WinIDEAInfo info,
                                                   StrVector loggingParams,
                                                   String[] coreIds) {
                                                   
        String winIDEAWorkspace = info.getWorkspace();
        String winIDEAId= info.getInstanceId();
        return connectWithProgressMonitor(parentShell, cPool, winIDEAWorkspace, winIDEAId,
                                          loggingParams, coreIds);
    }
    
    
    /**
     * Shows progress dialog on connect. Unfortunately the progress bar is not
     * shown on application start (empty area, called from ApplicationWorkbenchWindowAdvisor), 
     * while it is shown if connecting 
     * later. If this is very annoying, try SWT progress dialog... 
     *   
     * @param parentShell
     * @param cPool
     * @param winIDEAWorkspace
     * @param winIDEAId
     * @param loggingParams
     * @param coreIds
     * @return
     */
    private JConnection connectWithProgressMonitor(Shell parentShell, 
                                                   final ConnectionPool cPool, 
                                                   final String winIDEAWorkspace, 
                                                   final String winIDEAId,
                                                   final StrVector loggingParams,
                                                   final String[] coreIds) {
        
        final StringBuilder coresWithoutWinIDEALaunched = new StringBuilder();
        final MutableObject<Exception> connectExMO = new MutableObject<>();
        final MutableObject<JConnection> jConMO = new MutableObject<>();
        
        try {
            new ProgressMonitorDialog(parentShell).run(true, false,
                                                                new IRunnableWithProgress() {
                
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                                                         InterruptedException {
                    
                    monitor.beginTask("Connecting to winIDEA ...", IProgressMonitor.UNKNOWN);
                    monitor.subTask("Check that all dialogs in winIDEA are closed.\n"
                            + "If this dialog does not close, try to switch off emulator or close winIDEA.");
                    try {
                        jConMO.setValue(createConnection(cPool, 
                                                         winIDEAWorkspace, 
                                                         winIDEAId,
                                                         loggingParams,
                                                         coreIds));

                        tryToConnectOtherCores(jConMO.getValue(), 
                                               coresWithoutWinIDEALaunched, 
                                               coreIds);
                        
                    } catch (Exception ex) {
                        connectExMO.setValue(ex);
                        ex.printStackTrace();
                    } finally {
                        monitor.done();
                    }
                }
            });
            
        } catch (InvocationTargetException ex) {
            SExceptionDialog.open(parentShell, 
                                  "Can't connect to winIDEA - invocation exception!", 
                                  ex);
        } catch (InterruptedException ex) {
            MessageDialog.openError(parentShell, "Canceled", 
                                    "Operation has been canceled!"); 
        } catch (Exception ex) {
            SExceptionDialog.open(parentShell, 
                                  "Can't connect to winIDEA! If this instance was started from winIDEA, which has been closed, restart testIDEA!  ", 
                                  ex);
        }
        if (connectExMO.getValue() != null) {
            SExceptionDialog.open(parentShell, 
                                  "Can't connect to winIDEA! If this testIDEA was started from winIDEA, which has been closed, restart testIDEA!  ", 
                                  connectExMO.getValue());
        }
        
        return jConMO.getValue();
    }
    

    private JConnection createConnection(ConnectionPool cPool,
                                         String winIDEAWorkspace,
                                         String winIDEAId,
                                         StrVector loggingParams,
                                         String[] coreIds) {
        
        if (coreIds == null  ||  coreIds.length == 0) {
            coreIds = new String[]{""};
        }
        
        return cPool.connect(ConnectionPool.DEFAULT_CONNECTION,
                             winIDEAWorkspace, 
                             winIDEAId, 
                             coreIds[0], 
                             loggingParams);
    }

    
    /**
     * This method should be used when connection to primary winIDEA is already 
     * established, but connections to other cores may not have been established.
     * If they already exist, this method does nothing. Note that only connections 
     * to already launched winIDEAs are made. If winIDEA for some core is not
     * launched, then no connection for that core is made. 
     * 
     * For example, because connections to secondary 
     * cores may NOT have been established in advance but only during init sequence,
     * it may happen, that all winIDEAs are already initialized so the user skips
     * init sequence and connections are not established. In such case 
     * connections are initialized here. If winIDEAs are not initialized and
     * init sequence is skipped, this method will launch secondary winIDEAs without
     * initialization, but it is user's fault to skip init sequence.
     * 
     * @param parentShell shell used as parent when opening a connection dialog
     * @param jCon exiting connection to primary core. 
     * @param coreIds application specific coreIds. These IDs are defined by caller,
     *               but the same core ID must be used when obtaining controllers
     *               from connection. The number of elements in this array also
     *               defined the number of cores to connect to.  
     */

    public String connectToSecondaryCores(final Shell parentShell,
                                          final JConnection jCon, 
                                          final String[] coreIds) {
        
        final StringBuilder coresWithoutWinIDEALaunched = new StringBuilder();

        final MutableObject<Exception> connectExMO = new MutableObject<>();
        
        if (!jCon.isConnected()) {
            throw new SIOException("Primary core must be connected when connecting "
                                   + "to secondary cores!");
        }
        
        try {
            new ProgressMonitorDialog(parentShell).run(true, false,
                                                                new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                                                         InterruptedException {
                    
                    monitor.beginTask("Connecting to secondary winIDEAs ...", coreIds.length);
                    monitor.subTask("Check that all dialogs in winIDEA are closed.\n"
                            + "If this dialog does not close, try to switch off emulator or close winIDEA.");
                    try {
                        tryToConnectOtherCores(jCon,
                                               coresWithoutWinIDEALaunched,
                                               coreIds); 
                    } catch (Exception ex) {
                        connectExMO.setValue(ex);
                        ex.printStackTrace();
                    } finally {
                        monitor.done();
                    }
                }
            });
            
        } catch (InvocationTargetException ex) {
            SExceptionDialog.open(parentShell, 
                                  "Can't connect to secondary winIDEA - invocation exception!", 
                                  ex);
        } catch (InterruptedException ex) {
            MessageDialog.openError(parentShell, "Connection to secondary core canceled", 
                                    "Operation has been canceled!"); 
        } catch (Exception ex) {
            SExceptionDialog.open(parentShell, 
                                  "Can't connect to secondary winIDEA!", 
                                  ex);
        }
        
        if (connectExMO.getValue() != null) {
            SExceptionDialog.open(parentShell, 
                                  "Can't connect to secondary winIDEA!", 
                                  connectExMO.getValue());
        }
        
        return coresWithoutWinIDEALaunched.toString();
    }
    
    
    /*
     * Connects only to cores, which are already launched here, since download must
     * be done on primary core before you open winIDEAs for
     * other cores :-( 
     */  
    private void tryToConnectOtherCores(final JConnection jCon,
                                        final StringBuilder coresWithoutWinIDEALaunched,
                                        final String[] coreIds) {
        
        if (coreIds == null) {
            return;
        }
        
        int coreIndex = 0;
        for (String coreId : coreIds) {
            if (!jCon.getMccMgr().isConnected(coreId)) {
                if (jCon.getPrimaryCMgr().isCoreLaunched(coreIndex)) {
                    jCon.connectCore(coreIndex, coreId);
                } else {
                    if (coresWithoutWinIDEALaunched.length() > 0) {
                        coresWithoutWinIDEALaunched.append(", ");
                    }
                    coresWithoutWinIDEALaunched.append(coreId);
                }
            }
            coreIndex++;
        }
    }
}
