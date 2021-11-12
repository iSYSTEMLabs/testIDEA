package si.isystem.itest.run;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CAddress;
import si.isystem.connect.CDataController2;
import si.isystem.connect.CDebugFacade;
import si.isystem.connect.CEvaluatorConfig;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CInitSequenceAction;
import si.isystem.connect.CInitSequenceAction.EInitAction;
import si.isystem.connect.CInitSequenceAction.EInitSequenceSectionIds;
import si.isystem.connect.CMemAddress;
import si.isystem.connect.CMulticoreConnectionMgr;
import si.isystem.connect.CPUStatus;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CStackUsageConfig;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestCaseController;
import si.isystem.connect.CTestCaseTargetInitConfig;
import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagrams;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFilter.EFilterTypes;
import si.isystem.connect.CTestFilterController;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResultBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CValueType;
import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.ETristate;
import si.isystem.connect.IConnectDebug;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.CoreIdUtils;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.ISysDirs;
import si.isystem.itest.common.ISysPathFileUtils;
import si.isystem.itest.diagrams.DiagramUtils;
import si.isystem.itest.dialogs.TargetStateDialog;
import si.isystem.itest.handlers.KeepTestResultsCmdHandler;
import si.isystem.itest.handlers.TestSaveTestReportCmdHandler;
import si.isystem.itest.handlers.ToggleAnalyzerOnOffHandler;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelVerifier;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TBModelEventDispatcher;
import si.isystem.itest.model.TestSpecStatus;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.EmptyAction;
import si.isystem.itest.run.IDebugHandler.UserResponse;

/**
 * This class runs test as defined by run configuration and test specification.
 */
public class TestRunner {

    private static boolean ms_isSkipDiagramOverwriteMessage = false; 

    // It is important to have LinkedHashMap() so that the order of test 
    // results is the same as test execution order! - no longer true!
    private CTestReportContainer m_resultsContainer;
    // if UI option 'Keep results' is checked.
    
    private JConnection m_jConnectionMgr;
    private volatile boolean m_isMonitorCanceled;
    private volatile SException m_exception = null;
    private IDebugHandler m_debugHandler;
    private CTestFilter m_testFilter;
    private int m_currentTestNo;
    private Script m_script;
    private CTestCaseTargetInitConfig m_testCaseTargetInitConfig;
    private StrStrMap m_interruptStates = new StrStrMap();
    private Map<String, Boolean> m_testBatchStateMap = new TreeMap<>();
    
    private String m_coreIdInfo;
    private int m_actionIdx;
    private String m_actionName;
    
    private Stack<CTestHostVars> m_hostVarsStack;
    private static boolean m_isSkipDryRunWarning = false;
    boolean m_isDryRunChange;
    
    
    public TestRunner() {
        this(null);
    }
    

    public TestRunner(CTestFilter testFilter) {
        m_testFilter = testFilter;
    }


    public CTestReportContainer getResultsMap() {
        return m_resultsContainer;
    }

    
    private boolean initHeadless(CTestEnvironmentConfig envConfig, 
                                 IProgressMonitor monitor, 
                                 String modelFileName) {
    
        MutableBoolean isAllCoresConnected = new MutableBoolean(false);
        m_jConnectionMgr = ConnectionProvider.instance().
                                   connectToWinIdeaHeadless(envConfig, 
                                                            isAllCoresConnected,
                                                            monitor);

        CScriptConfig scriptConfig = envConfig.getScriptConfig(true);
        m_script = new Script(m_jConnectionMgr, scriptConfig, modelFileName);

        if (envConfig.isAlwaysRunInitSeqBeforeRun()) {
            if (!executeInitSequenceHeadless(envConfig, monitor)) {                    
                return false;  // user canceled operation in the monitor
            }
        } else {
            // make sure all connections are established
            if (!isAllCoresConnected.booleanValue()) {
                if (!executeInitSequenceHeadless(envConfig, monitor)) {                    
                    return false;  // user canceled operation in the monitor
                }
            }
        }
        
        return true;
    }
    
    
    /**
     * Initializes target according to runConfiguration. This method partially duplicates
     * method CTestBench::executeInitAction(), but adds more user friendly features,
     * like progress monitor, possibility for cancellation and warnings in case
     * of inconsistent target state.  
     * 
     * @param envConfig contains parameters for target initialization
     * @param modelFileName 
     * @param isRunInitSeq if true, init sequence is run. When called from 
     *                     InitTargetCmdHandler(), it should be true, when called 
     *                     from from runTest(), it should be set to 
     *                     runConfiguration.isAlwaysInitBeforeRun().
     * @return true, if target is in stopped state, false if the user decided to
     *               cancel tests.
     * @throws IOException 
     */
    public boolean init(CTestEnvironmentConfig envConfig, String modelFileName) {

        m_jConnectionMgr = ConnectionProvider.instance().getDefaultConnection();
        
        if (m_jConnectionMgr == null  ||  !m_jConnectionMgr.isConnected()) {
            // try to connect automatically if possible, otherwise dialog is shown.
            m_jConnectionMgr = ConnectionProvider.instance().connectToWinIdea(false, 
                                                                              envConfig);
            // user canceled connection
            if (m_jConnectionMgr == null  ||  !m_jConnectionMgr.isConnected()) {
                throw new SIllegalStateException("No connection to winIDEA! Please use menu iTools to connect to winIDEA!");
            }
        }
        
        CScriptConfig scriptConfig = envConfig.getScriptConfig(true);
        m_script = new Script(m_jConnectionMgr, scriptConfig, modelFileName);

        // Set breakpoint mode for primary core here. For other cores it is set in 
        // CTestBench, when connectToCore init action is executed.
        String primaryCoreId = CoreIdUtils.getConfiguredCoreID(envConfig, "");
        CIDEController primaryIdeCtrl = m_jConnectionMgr.getCIDEController(primaryCoreId);
        try {
            CTestBench.configureBreakpointsMode(primaryIdeCtrl, envConfig.getBreakpointType(), 
                                                0, true);
        } catch (UnsupportedOperationException ex) {
            throw new IllegalStateException(ex.getMessage() + 
                                            " Try to select 'File | Properties | "
                    + "Run Configuration | Keep winIDEA setting' for breakpoint type.");
        }
        
        if (envConfig.isAlwaysRunInitSeqBeforeRun()) {
            if (!runInitSequence(envConfig)) {                    
                return false;  // user canceled the sequence
            }
        } else {
            // make sure all connections are established
            String unconnectedCores = 
                    ConnectionProvider.instance().connectToSecondaryCores(m_jConnectionMgr, 
                                                                          envConfig);
            
            if (!unconnectedCores.isEmpty()) {
                String msg = "Not all cores are connected: " + unconnectedCores
                        + "\nIt is highly recommended to run init sequence!";
                TargetStateDialog dlg = new TargetStateDialog(Activator.getShell(), msg, false);
                if (dlg.show()) {
                    if (dlg.isRunInitSequence()) {
                        if (!runInitSequence(envConfig)) {                    
                            return false;  // user canceled the sequence
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        int coreIdx = 0;
        String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);
        
        for (String coreId : coreIds) {
            
            if (!m_jConnectionMgr.isConnected(coreId)) {
                continue; // init sequence may not initialize all cores with IDs set 
            }
            
            CDebugFacade debug = m_jConnectionMgr.getCDebugFacade(coreId);
            CIDEController ideCtrl = m_jConnectionMgr.getCIDEController(coreId);
            CDataController2 dataCtrl = m_jConnectionMgr.getCDataController(coreId);
            ConnectionMgr connectionMgr = m_jConnectionMgr.getMccMgr().getConnectionMgr(coreId);
            
            if (!checkCoreStateAndHelpUser(coreId, envConfig, debug)) {
                return false; // state is not valid and user canceled init sequence
            }

            // refresh controllers, since init sequence may have been run in checkCoreStateAndHelpUser()
            // and it may include connection refresh
            debug = m_jConnectionMgr.getCDebugFacade(coreId);
            ideCtrl = m_jConnectionMgr.getCIDEController(coreId);
            dataCtrl = m_jConnectionMgr.getCDataController(coreId);
            connectionMgr = m_jConnectionMgr.getMccMgr().getConnectionMgr(coreId);
            
            // Stack usage must be configured AFTER init sequence - winIDEA for the
            // core must be launched. However, it should be configured BEFORE
            // download for the core is executed. Current workaround is to run 
            // init sequence twice, until single winIDEA instance supports 
            // multiple cores. See also comment to the other calls to this method.
            // Users can also save this setting with winIDEA workspace, in which case
            // it will work for the first time.
            CTestBench.configureStackUsage(dataCtrl, envConfig, coreId);
            
            CTestBench.configureBreakpointsMode(ideCtrl, envConfig.getBreakpointType(), 
                                                coreIdx++, false);
            
            // configure evaluator
            CEvaluatorConfig evalCfg = envConfig.getEvaluatorConfig(true);
            if (evalCfg.isOverrideWinIDEASettings()) {
                evalCfg.applySettingsToWinIDEA(ideCtrl);
            }
            
            // reset testing subsystem in winIDEA
            CTestCaseController.clearAllTests(connectionMgr);
        }
        
        return true;
    }
    
          
    /** Tries to be smart and detect winIDEA status. Offers possible options to the user. */
    private boolean checkCoreStateAndHelpUser(String coreId,
                                              CTestEnvironmentConfig runConfiguration,
                                              CDebugFacade debug) {
        try {
            CPUStatus status = debug.getCPUStatus();
            if (!status.isStopped()) {
                String msg;

                if (!coreId.isEmpty()) {
                    msg = " Core '" + coreId + "'";
                } else {
                    msg = "Target";
                }

                if (status.isRunning()) {
                    msg += " is not in stopped state!";
                } else {
                    msg += " is not initialized!";
                }

                TargetStateDialog dlg = new TargetStateDialog(Activator.getShell(), 
                                                              msg,
                                                              true);
                if (dlg.show()) {
                    if (dlg.isRunInitSequence()) {
                        try {
                            if (!runInitSequence(runConfiguration)) {
                                // user canceled the sequence
                                return false;
                            }
                        } catch (Exception ex) {
                            SExceptionDialog.open(Activator.getShell(), 
                                                  "Can not run init sequence, which is defined " +
                                                          "in 'Run | Configuration...' dialog!", 
                                                          ex);
                        }
                    } else if (dlg.isStopTarget()) {
                        debug.stop();
                    }
                } else {
                    return false;
                }
            } 

            status = debug.getCPUStatus();
            if (status.isStopped()) {
                String stopFunction = getStopFunction(runConfiguration, coreId);
                // verify, if target is stopped at 'stop' function specified in Run Configuration 
                if (!stopFunction.isEmpty()  &&  runConfiguration.isCheckTargetStateBeforeRun()) {
                    long currentAddr = status.getExecutionPoint();
                    short memArea = status.getExecutionArea();
                    try {
                        String stopFuncExpr = debug.adjustAddressExpression(stopFunction);
                        // use 'try' block to be able to write detailed error message to the user 
                        CAddress addr;
                        try {
                            CValueType stopFuncAddr = debug.evaluate(IConnectDebug.EAccessFlags.fMonitor,
                                                                     stopFuncExpr);
                            addr = stopFuncAddr.getAddress();
                        } catch (Exception ex) {
                            throw new SIllegalArgumentException("The expression given as stop function does not evaluate to an address." +
                                    " Check 'Stop function/expr.' in 'Run | Configuration...' dialog!", ex).add("Invalid function/expr.", stopFunction);
                        }

                        if (currentAddr != addr.getM_aAddress()  ||  
                                memArea != addr.getM_iArea()) {

                            TargetStateDialog dlg = new TargetStateDialog(Activator.getShell(), 
                                                                          "Target is stopped, but not at function '" + 
                                                                                  stopFunction + 
                                                                                  "' as defined in 'Run | Configuration...' dialog.\n",
                                                                                  false);
                            if (dlg.show()) {
                                if (dlg.isRunInitSequence()) {
                                    if (!runInitSequence(runConfiguration)) {
                                        // user canceled the sequence
                                        return false;
                                    }
                                }
                            } else {
                                return false;
                            }
                        }
                    } catch (Exception ex) { // getFunctionAddress(stopFunction) may fail
                        SExceptionDialog.open(Activator.getShell(), 
                                              "Can not get address of function/expr. '" + stopFunction + "', which is defined " 
                                              + "in 'Run | Configuration...' dialog!\n"
                                              + "Make sure the function exists and the code is downloaded to the target!", 
                                              ex);
                        return false;
                    }
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Error when initializing and checking core status. Core: '" + coreId + "'.", 
                                  ex);
            return false;
        }
        
        return true;
    }

    
    // searches all init action for the given core ID and action Run. Returns parameter
    // of this action - if not empty string, it is function or address expression
    private String getStopFunction(CTestEnvironmentConfig runConfiguration, String coreId) {
        CTestBaseList initSeq = 
            runConfiguration.getTestBaseList(EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                             true);
        
        int numInitSteps = (int) initSeq.size();
        // search in reverse - if there is more than one Run action return the last stop function
        for (int stepIdx = numInitSteps - 1; stepIdx >= 0; stepIdx--) {
            CInitSequenceAction action = CInitSequenceAction.cast(initSeq.get(stepIdx));
            String configuredCoreId = CoreIdUtils.getConfiguredCoreID(runConfiguration, 
                                                                      action.getCoreId());
            if (coreId.equals(configuredCoreId)  &&  action.getAction() == EInitAction.EIARun) {                
                CSequenceAdapter params = 
                        new CSequenceAdapter(action, 
                                             EInitSequenceSectionIds.E_INIT_SEQ_PARAMS.swigValue(),
                                             true);
                if (params.size() > 0) {
                    return params.getValue(0);
                }
            }
        }
        
        return "";
    }

        
    private boolean runInitSequence(final CTestEnvironmentConfig runConfiguration) {

        m_exception = null;
        m_coreIdInfo = null; // if we get here, error is no longer 
        // related to specific core
        m_actionIdx = 0;
        m_actionName = null;
        
        CTestBaseList initSeq = 
                runConfiguration.getTestBaseList(EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                                 false);
        
        if (initSeq.isEmpty()) {
            MessageDialog.openWarning(Activator.getShell(), 
                   "Empty init sequence", 
                   "Init sequence is empty - please run command 'Test | Init target' to create default one,\n"
                   + "or set it with 'Test | Configuration ...'");
            // It is OK to continue testing - user was warned. If we return here,
            // then testing is not possible, even if winIDEA is in proper state.
            // Annoying, when 'Always run init seq before run' in prefs is checked,
            // but the sequence is empty. It should be OK to 'run' empty init sequence.
        }               
        
        
        try {
            PlatformUI.getWorkbench().getProgressService()
            .busyCursorWhile(new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {

                    monitor.beginTask("Initializing target", IProgressMonitor.UNKNOWN);
                    m_isMonitorCanceled = false;

                    try {
                        executeInitSequenceHeadless(runConfiguration, monitor);
                        
                        if (monitor.isCanceled()) {
                            m_isMonitorCanceled = true;
                            return;
                        }

                    } catch (Exception ex) {
                        // can not report ex here, because this is non-gui thread.
                        m_isMonitorCanceled = true;
                        m_exception = new SException("Init sequence failed!", ex);
                    } finally {
                        monitor.done();
                    }
                }
            });

        } catch (InvocationTargetException ex) {
            SExceptionDialog.open(Activator.getShell(), "Target init failed! Check settings in 'Run | Configuration...' dialog!", ex); 
        } catch (InterruptedException ex) {
            MessageDialog.openError(Activator.getShell(), "Canceled", "Operation has been canceled!"); 
        }

        if (m_exception != null) {
            String msg;
            if (m_coreIdInfo == null) {
                msg = "Target initialization failed! Check settings in 'Run | Configuration...' dialog!";
            } else {
                msg = "Init action '" + m_actionName + "' failed! Core '" + m_coreIdInfo
                      + "', action index "
                      + m_actionIdx + ". Check settings in 'Run | Configuration...' dialog!";
            }
            SExceptionDialog.open(Activator.getShell(), msg, m_exception);
        }
        
        return !m_isMonitorCanceled;
    }
    
    
    private boolean executeInitSequenceHeadless(final CTestEnvironmentConfig envConfig,
                                                IProgressMonitor monitor) {
        
        // configure stack usage at least for primary core - the proper way would
        // be to configure it after winIDEA for the core launches, but this 
        // complicates the code and will be redundant after one winIDEA
        // instance will support multiple cores.
        CDataController2 dataCtrl = m_jConnectionMgr.getCDataController("");
        CTestBench.configureStackUsage(dataCtrl, envConfig, "");
        
        CTestBaseList initSeq = 
                envConfig.getTestBaseList(EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                                 true);
        int numInitSteps = (int) initSeq.size();
        for (int stepIdx = 0; stepIdx < numInitSteps  && !monitor.isCanceled(); stepIdx++) {
            CInitSequenceAction action = CInitSequenceAction.cast(initSeq.get(stepIdx));
            String coreId = action.getCoreId();
            m_coreIdInfo = coreId;
            m_actionIdx = stepIdx;

            m_actionName = action.getActionName();
            CMulticoreConnectionMgr mccMgr = m_jConnectionMgr.getMccMgr();
            
            switch(action.getAction()) {
            case EIAConnectToCore:
                printSubtaskDesc(monitor, coreId, "Connecting ...");
                CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                break;

            case EIACallScriptFunction:
                String [] scriptParams =  DataUtils.getArray(action, 
                                                             EInitSequenceSectionIds.E_INIT_SEQ_PARAMS.swigValue());
                if (scriptParams.length == 0) {
                    throw new SIllegalArgumentException("Missing script function name in init sequence!").
                    add("actionName", m_actionName).
                    add("actionIdx", stepIdx).
                    add("coreId", coreId);
                }
                String functionName = scriptParams[0];

                printSubtaskDesc(monitor, coreId, "Calling script function ..."); 
                // remove function name from params array
                scriptParams = Arrays.copyOfRange(scriptParams, 1, scriptParams.length);

                final TestScriptResult scriptResult = m_script.callFunction(null, 
                                                                            CTestResultBase.getSE_INIT_SEQ(), 
                                                                            functionName, 
                                                                            scriptParams);
                // Show output in Status view
                // final StringBuilder sb = UiUtils.scriptResult2StatusViewText(scriptResult);
                
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        StatusType status = scriptResult.isError() ? StatusType.ERROR : StatusType.INFO;
                        StatusModel.instance().appendDetailPaneText(status, 
                                                                    scriptResult.toUIString());
                    }});

                break;

            case EIACallTargetFunction:
                printSubtaskDesc(monitor, coreId, "Calling target function ..."); 
                CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                break;

            case EIADeleteAllBreakpoints:
                printSubtaskDesc(monitor, coreId, "Deleting breakpoints ...");
                CTestBench.executeInitAction(envConfig, mccMgr, 
                                             action, stepIdx, false, 0);
                break;

            case EIADownload:
                printSubtaskDesc(monitor, coreId, "Downloading ...\n"
                        + "(Check winIDEA status if download does not finish on time.)");
                CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                break;

            case EIAReset:
                printSubtaskDesc(monitor, coreId, "Resetting ...");
                CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                CDebugFacade debug = m_jConnectionMgr.getCDebugFacade(coreId);
                waitUntilStopped(debug, monitor);
                break;
            case EIARun:
                debug = m_jConnectionMgr.getCDebugFacade(coreId);
                CSequenceAdapter runParams = new CSequenceAdapter(action,
                                                                  EInitSequenceSectionIds.E_INIT_SEQ_PARAMS.swigValue(),
                                                                  true);
                if (runParams.size() > 0  &&  !runParams.getValue(0).isEmpty()) {
                    String stopFunctionName = runParams.getValue(0);
                    printSubtaskDesc(monitor, coreId, 
                                     "Running until function/expr. '" + stopFunctionName + "' ...");

                    checkIfAlreadyStoppedOnMain(stopFunctionName, debug);
                } else {
                    printSubtaskDesc(monitor, coreId, 
                                     "Running ...");
                }
                
                if (!m_isMonitorCanceled) {
                    CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                    waitUntilStopped(debug, monitor);
                }
                break;
            case EIALoadSymbolsOnly:
                printSubtaskDesc(monitor, coreId, "Initializing hardware and loading symbols  ...");
                CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                break;
            case EIAWaitUntilStopped:
                printSubtaskDesc(monitor, coreId, "Waiting until core stops  ...");
                CTestBench.executeInitAction(envConfig, mccMgr, action, stepIdx, false, 0);
                break;
            default:
                throw new SIllegalArgumentException("Unknown target initialization action!").
                                                    add("action", action.getAction());
            }
        }
        
        return !monitor.isCanceled();
    }


    private void printSubtaskDesc(IProgressMonitor monitor,
                                  String coreId,
                                  String msg) {
        if (coreId.isEmpty()) {
            monitor.subTask(msg);
        } else {
            monitor.subTask("Core '" + coreId + "': " + msg);
        }
    }


//    private void configureStackUsage(final CTestEnvironmentConfig runConfiguration,
//                                     String coreId, 
//                                     final CDataController2 dataCtrl) {
//
//        CTestBaseList stackUsageList = runConfiguration.getStackUsageOptions(true);
//        int numItems = (int) stackUsageList.size();
//
//        for (int idx = 0; idx < numItems; idx++) {
//            
//            CStackUsageConfig stackUsageCfg = CStackUsageConfig.cast(stackUsageList.get(idx));
//            if (!stackUsageCfg.isEmpty()) {
//                String stackCoreId = stackUsageCfg.getCoreId();
//                stackCoreId = CoreIdUtils.getConfiguredCoreID(runConfiguration,
//                                                              stackCoreId);
//                
//                if (coreId.equals(stackCoreId)) {
//                    if (stackUsageCfg.isActive()) {
//                        dataCtrl.configureStackUsage(stackUsageCfg.getBaseExpr(), 
//                                                     stackUsageCfg.getEndExpr(),
//                                                     (short)stackUsageCfg.getPattern());
//                    } else {
//                        dataCtrl.configureStackUsage("", "", (short)0);
//                    }
//                }
//            }
//        }
//    }
    

    private void checkIfAlreadyStoppedOnMain(final String stopFunctionName, CDebugFacade debug) {
        
        CMemAddress stopFuncAddr = debug.getFunctionAddress(stopFunctionName);
        short currentMemArea = debug.getCPUStatus().getExecutionArea();
        long currentAddr = debug.getCPUStatus().getExecutionPoint();

        if (stopFuncAddr.getMemArea() == currentMemArea  &&
                stopFuncAddr.getAddress() == currentAddr) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    m_isMonitorCanceled = 
                            !MessageDialog.openConfirm(Activator.getShell(), 
                                                       "Warning!", 
                                                       "Target execution point is already at function '" + stopFunctionName + "()'. If you continue with testing,\n"
                                                       + "the target may never stop.\n\n"
                                                       + "The reason for this warning is usually usage of both options:\n"
                                                       + "- 'Debug | Files for download | Options | Run Until ...'  in winIDEA\n"
                                                       + "- 'Run | Configuration | Initialization sequence | action | run " + stopFunctionName + "' in testIDEA\n\n"
                                                       + "Please configure only one of these two options.\n\n"
                                                       + "Press 'OK' to continue, 'Cancel' to stop testing.");
                }
            });
        }
    }
    

    // may be run only in Professional version
    public boolean runTestHeadless(final CTestEnvironmentConfig envConfig, 
                                   CTestBench testBench,
                                   final String modelFileName,
                                   final CTestSpecification testSpec, 
                                   final int derivedLevel,
                                   boolean isDebugMode,
                                   IProgressMonitor monitor) {
        
        int noOfTestsToRun = countExecutableTests(testSpec,
        		                                  derivedLevel, 
        		                                  null, 
        		                                  testBench.getFilterController()); 
        if (noOfTestsToRun < 1) {
            return false; // no license, no tests
        }
        
        if (!initHeadless(envConfig, monitor, modelFileName)) {
            return false; // user canceled operation in the monitor
        }
        
        CTestReportConfig reportCfg = testBench.getTestReportConfig(false);
        String reportFile = reportCfg.getFileName();
        String iyamlDir = new File(modelFileName).getParent();
        reportFile = ISysPathFileUtils.getAbsPathFromDir(iyamlDir, reportFile);
        ISysDirs dirs = ISysPathFileUtils.getISysDirs(modelFileName, reportFile);
        m_resultsContainer = testBench.getTestReportContainer();
        m_resultsContainer.clearResults();
        CTestFilterController filterCtrl = testBench.getFilterController();
        filterCtrl.clear();  // to indicate refresh is needed

        runTestMain(envConfig,
                    testBench,
                    testSpec,
                    null,
                    derivedLevel,
                    isDebugMode,
                    false,  // is quick debug
                    true,   // is run analyzers
                    true,   // is Pro version
                    false,  // is Dry run
                    noOfTestsToRun,
                    monitor,
                    dirs);

        return true;
    }
    
    
    /**
     * Runs tests - this method should be called from UI thread, because
     * user is offered some options when connection/license are not OK.   
     * 
     * @param runConfiguration contains target initialization steps.
     * 
     * @param testSpec test specification to execute (may and in most cases does 
     *                                                contain derived test specs)
     * @param empty group which contains only one level of child groups, for which 
     *              scripts should be executed.                                                
     *                                                
     * @param derivedLevel how deep to run tests on derived test specs of the
     *                     <code>testSpec</code>. If testSpec is only a container
     *                     and has all test specs to be run as children, this
     *                     parameter should be 1. Integer.MAX_VALUE runs all
     *                     derived test specs.
     *                     
     * @param isDebugMode  if true, and execution stops unexpectedly, then testIDEA
     *                     pops-up a dialog and waits for the user to debug the
     *                     code.
     *                                          
     * @param isQuickDebug is true, then execution is stopped at function entry,
     *                     and then testIDEA waits for user to debug the function 
     *                     in winIDEA.
     * 
     * @throws IOException 
     */
    public void runTestUI(final TestSpecificationModel model, 
                          final CTestSpecification testSpec, 
                          final CTestGroup rootGroup,
                          final int derivedLevel,
                          final boolean isDebugMode,
                          final boolean isQuickDebug,
                          final boolean isDryRun) {

        if (isDryRun  &&  !m_isSkipDryRunWarning) {
            MessageDialogWithToggle dialog =
            MessageDialogWithToggle.openOkCancelConfirm(Activator.getShell(), 
                                                "Dry Run Mode", 
                                                "Your test cases will be changed! This operation is NOT undoable!\n"
                                                + "Are you sure you want to run tests in 'Dry Run' mode?", 
                                                "Do not show this dialog this session.", 
                                                m_isSkipDryRunWarning, 
                                                null, 
                                                null);
            
            if (dialog.getReturnCode() != Window.OK) {
                return;
            }
            
            m_isSkipDryRunWarning = dialog.getToggleState();
        }
        m_isDryRunChange = false;
        
        if (askForOverwriteDiagramImages(testSpec) == false) {
            return;
        }
        
        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
        
        if (envConfig.isVerifySymbolsBeforeRun()) {

            if (!verifySymbols(model, testSpec, derivedLevel)) {
                return;
            }

            if (rootGroup.hasChildren()) {
                if (!verifySymbols(model, rootGroup, Integer.MAX_VALUE)) {
                    return;
                }
            }
        }
                
        clearStatusView();
        
        m_resultsContainer = model.getTestReportContainer();
        final boolean isKeepResults = KeepTestResultsCmdHandler.isKeepResults();
        if (!isKeepResults) {
            m_resultsContainer.clearResults(); // reset results so that the user does not get 
                                               // misguided in case of internal testIDEA error 
        }
        
        // init() initializes also m_script, which is used by verifyTestIDEALicense()
        // if script test case filtering is used, so call it before
        // verifyTestIDEALicense()
        if (!init(envConfig, model.getModelFileName())) {
            return; // user has canceled the test in one of dialogs in init function
        }

        final int noOfTestsToRun = countExecutableTests(testSpec,
                                                        derivedLevel, 
                                                        m_testFilter, 
                                                        model.getFilterController());  
        if (noOfTestsToRun < 1) {
            return; 
        }
        
        final boolean isInitAnalyzers = ToggleAnalyzerOnOffHandler.isAnalyzerOn();
        
        /*String reportAbsFile =
            UiUtils.getAbsPathFromWorkingDir(model.getTestReportConfig().getFileName());
        String reportDir = Paths.get(reportAbsFile).getParent().toString();
        String dotExeDir = UiUtils.getDotExeDir(); */
        final ISysDirs dirs = ISysPathFileUtils.getISysDirs();
        
        m_exception = null;
        
        try {
            PlatformUI.getWorkbench().getProgressService()
            .busyCursorWhile(new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {        


                    try {
                        runTestMain(model.getTestEnvConfig(), 
                                    model.getTestBench(),
                                    testSpec, 
                                    rootGroup,
                                    derivedLevel, 
                                    isDebugMode, 
                                    isQuickDebug,
                                    isInitAnalyzers,
                                    true,
                                    isDryRun,
                                    noOfTestsToRun,
                                    monitor,
                                    dirs);
                    } catch (Exception ex) {
                        m_exception = new SException("Test run failed!", ex);
                        ex.printStackTrace();
                        monitor.done();
                    }
                }
            });

        } catch (InvocationTargetException ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Invocation exception - Operation failed!",
                                  ex); 
        } catch (InterruptedException ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Operation has been canceled!",
                                  ex); 
        }
        
        // if result is set after tests are finished, then menu option 
        // File | Save Test Report is not enabled, because status is changed
        // to late - see commented line below: model.setResults(m_resultsMap);
//        if (isKeepResults) {
//            m_modelResults.merge(m_resultsContainer);
//        } else {
//            model.setResults(m_resultsContainer);
//        }
     
        model.setResults();
        
        if (m_isDryRunChange) {
            // notify dirty model
            model.execAction(new EmptyAction("Dry run change"));
        }
        
        if (testSpec.hasChildren()) {
            sendNotifications(model);
        }

        if (m_exception != null) {
            throw m_exception;
        }
    }

    
    // Returns true, if there are no diagrams or user decided to continue 
    private boolean askForOverwriteDiagramImages(CTestSpecification testSpec) {
        
        if (ms_isSkipDiagramOverwriteMessage) {
            return true;
        }
        
        List<String> images = new ArrayList<>(); 
        getDiagramImages(testSpec, images);
        
        final int MAX_FILES_IN_TEXT = 5;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < images.size()  &&  i < MAX_FILES_IN_TEXT; i++) {
            sb.append("    ").append(images.get(i)).append('\n');
        }
        
        if (images.size() > MAX_FILES_IN_TEXT) {
            sb.append("...\n");
        }
        sb.append("\n");
        
        if (images.size() > 0) {
            MessageDialogWithToggle dlg = 
                    MessageDialogWithToggle.openYesNoQuestion(Activator.getShell(), 
                                                "Overwrite files?", 
                                                "Test cases to be run will generate files with diagrams:\n"
                                                + sb.toString()
                                                + "If files with these names already exist, they will be overwritten.\n"
                                                + "Do you want to continue?", 
                                                "Do not show this message in this session", 
                                                ms_isSkipDiagramOverwriteMessage, 
                                                null, 
                                                null);
            
            ms_isSkipDiagramOverwriteMessage = dlg.getToggleState();
            
            final int RET_CODE_YES = 2;
            
            return dlg.getReturnCode() == RET_CODE_YES;
        }
        
        return true;
    }
    
    
    private void getDiagramImages(CTestSpecification testSpec, List<String> images) {
        CTestDiagrams diagrams = testSpec.getDiagrams(true);

        if (diagrams.isActive() == ETristate.E_TRUE) {
            CTestBaseList diagConfigsList = diagrams.getConfigurations(true);
            int numDiagConfigs = (int) diagConfigsList.size();
            for (int idx = 0; idx < numDiagConfigs; idx++) {
                CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigsList.get(idx));

                if (diagConfig.isActive() == ETristate.E_TRUE) {
                    String imageFileName = DiagramUtils.getAbsImageFileFromReportDir(testSpec, diagConfig);
                    images.add(imageFileName);
                }
            }
        }
        
        int numDerived = testSpec.getNoOfDerivedSpecs();
        for (int tsIdx = 0; tsIdx < numDerived; tsIdx++) {
            CTestSpecification derivedTs = testSpec.getDerivedTestSpec(tsIdx);
            getDiagramImages(derivedTs, images);
        }
    }

    
    public static void sendNotifications(final TestSpecificationModel model) {
        
        TBModelEventDispatcher dispatcher = model.getEventDispatcher();
        
        
        dispatcher.fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED, 
                                                   null, 
                                                   null));

        // model now contains also results - update editor view
        // This was commented out, because it is difficult to preserve selection -
        // - executed test cases are not good candidates for selection, as group 
        // may also be selected. The editor is refreshed anyway, so this line was removed. 
//        dispatcher.fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED, 
//                                                   selectedNode, 
//                                                   selectedNode));
        
        // Update results view last, so that deatiledPaneText is not overwritten by 
        // editor view calling ModelVerifier on structure changed event above.
        // For example, when tests finish, detailed pane text should present contents 
        // of the first line in table. Had put this at start of this function two weeks ago,
        // but then detailed pane was empty after tests finished - don't remember why I put this
        // at start of this function?
        dispatcher.fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.UPDATE_TEST_RESULTS, 
                                                   null, 
                                                   null));
    }

    
    private void runTestMain(final CTestEnvironmentConfig envConfig,
                             CTestBench testBench,
                             final CTestSpecification testSpec, 
                             CTestGroup rootGroup,
                             final int derivedLevel,
                             boolean isDebugMode,
                             final boolean isQuickDebug, 
                             final boolean isInitAnalyzers,
                             final boolean isRunTestWAnalyzers,
                             boolean isDryRun,
                             final int noOfTestsToRun,
                             IProgressMonitor monitor,
                             ISysDirs dirs) {

        // store version of winIDEA used for test execution
        CIDEController ideCtrl = m_jConnectionMgr.getCIDEController(null);
        
        CTestReportConfig reportCfg = testBench.getTestReportConfig(false);
        reportCfg.setWinIDEAVersion(ideCtrl.getWinIDEAVersion().toString());
        
        try {
            refreshSymbolsAndGroups(m_jConnectionMgr, envConfig, testBench);
        } catch (Exception ex) {
            if (!(ex instanceof FileNotFoundException)) {
                throw ex;
            }
            // FNFE is thrown only when there are no download files with symbols, or
            // `Load Symbols` in winIDEA dialog 'Files for download' is not checked.
            // However, user may still want to run system tests with Python script
            // to get nice testIDEA reports :-| B023891.
        }
        
        m_testCaseTargetInitConfig = envConfig.getTestCaseTargetInitConfig(true);

        m_debugHandler = null;
        
        
        if (isDebugMode) {
            m_debugHandler = new TestDebugHandler(); 
        }
        
        if (rootGroup == null) {
            rootGroup = testBench.getGroup(true);
        }
        
        final JTestCase jTestCase  = new JTestCase();
        
        m_currentTestNo = 1;
        
        // Store for usage in test report. This way we no longer need a connection
        // when test report is saved - see B013581, 9.7.2014, couldn't save test 
        // report when winIDEA crashed.
        TestSaveTestReportCmdHandler.setLastTestRunEnv(dirs.getWinIDEAWorkspaceDir(),
                                                       dirs.getWinIDEAWorkspaceFile(),
                                                       dirs.getDefaultDownloadFile());
        configureHostVars(dirs, envConfig);
        

        try {
            // disable interrupts if configured
            CTestBench.configureInterrupts(envConfig, 
                                           m_jConnectionMgr.getMccMgr(),
                                           m_interruptStates);
            
            runGroupScripts(rootGroup, 
                            ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT,
                            CTestResultBase.getSE_GROUP_INIT_FUNC(),
                            derivedLevel, envConfig, monitor);
            
            monitor.beginTask("Running tests", noOfTestsToRun);

            try {
                runTestsRecursive(jTestCase, testSpec, 
                                  derivedLevel, monitor,
                                  dirs,
                                  m_debugHandler,
                                  noOfTestsToRun, isQuickDebug,
                                  isInitAnalyzers,
                                  isRunTestWAnalyzers,
                                  envConfig,
                                  testBench.getFilterController(),
                                  isDryRun);
            } catch (OperationCanceledException ex) {
                // exception was thrown to unwind the stack if derived tests were
                // executed when user pressed the cancel button in the Progress
                // Monitor dialog. However, execution should continue normally
                // so that results get stored and displayed.
            }

        } catch (final Exception ex) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    SExceptionDialog.open(Activator.getShell(), "Test Execution Error!", ex); 
                }

            });
        } finally {
            try {
                String[] coreIds = CoreIdUtils.getCoreIDs(envConfig);
                
                // if flag is ON from the last test, restore registers
                for (String coreId : coreIds) {
                    Boolean isRestoreTestBatch = m_testBatchStateMap.get(coreId);
                    if (isRestoreTestBatch != null  &&  isRestoreTestBatch.booleanValue()) {
                        ConnectionMgr connectionMgr = m_jConnectionMgr.getMccMgr().getConnectionMgr(coreId);
                        CTestCaseController.setTestBatch(connectionMgr, false);
                        (new CTestCaseController(connectionMgr, 0)).cleanPersistentVars();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // ignored exception, because it occurs only on 
                // very bad state, for example when user switches off
                // emulator - no need to inform him about error
            }
            CTestBench.restoreInterrupts(envConfig, 
                                         m_jConnectionMgr.getMccMgr(),
                                         m_interruptStates);

            monitor.beginTask("Evaluating group results ...", IProgressMonitor.UNKNOWN);
            testBench.calculateGroupResults(m_jConnectionMgr.getMccMgr(), envConfig);
            
            runGroupScripts(rootGroup, 
                            ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT, 
                            CTestResultBase.getSE_GROUP_END_FUNC(), 
                            derivedLevel, envConfig, monitor);
            
            monitor.done();
        }
        
        m_script.close();
    }


    private void runGroupScripts(CTestGroup rootGroup,
                                 ESectionCTestGroup groupSection,
                                 String scriptFuncType,
                                 int derivedLevel,
                                 CTestEnvironmentConfig envConfig, IProgressMonitor monitor) {
        
        monitor.beginTask("Executing group scripts...", IProgressMonitor.UNKNOWN);
        CTestHostVars hostVars = new CTestHostVars();

        
        CTestBaseList childGroups = rootGroup.getChildren(true);
        int numGroups = (int) childGroups.size();
        for (int idx = 0; idx < numGroups; idx++) {
            CTestGroup childGroup = CTestGroup.cast(childGroups.get(idx));
            CTestFunction scriptFunc = childGroup.getScriptFunction(groupSection, true);

            if (!scriptFunc.getName().isEmpty()) {

                hostVars.initTestGroupVars(childGroup, envConfig);

                final TestScriptResult scriptResult = 
                        m_script.callCTestFunction(childGroup,
                                                   hostVars,
                                                   scriptFuncType, 
                                                   scriptFunc);

                CTestResultBase groupResult = m_resultsContainer.getGroupResult(childGroup);
                if (groupResult == null) {
                    groupResult = new CTestGroupResult(childGroup);
                    m_resultsContainer.putTestResult(childGroup, groupResult);
                }
                JTestCase.setScriptResult(groupResult, scriptResult);
                // Show output in Status view
                // final StringBuilder sb = UiUtils.scriptResult2StatusViewText(scriptResult);

                JTestCase.appendToStatusView(scriptResult.toUIString());
            }
            
            if (derivedLevel > 0) {
                runGroupScripts(childGroup, groupSection, scriptFuncType, 
                                --derivedLevel, envConfig, monitor);
            }
        }
    }


    /*
     * Refreshes symbols and groups, but only if they are empty - they were not
     * initialized at all. If they have been initialized since program start,
     * they are not refreshed here - possibility for stale data. However, this should
     * not be a problem, as running selected test cases does not require groups refreshed.
     * If user is running test cases in a group, it is his responsibility to 
     * click the refresh command. Another side effect is when user selects test case
     * in a group, which is actually a test case owning group. On refresh these groups are
     * recreated, so selection is lost even if we run single test.  
     *  
     * @param jConnectionMgr
     * @param envConfig
     * @param testBench
     */
    public static void refreshSymbolsAndGroups(JConnection jConnectionMgr,
                                               final CTestEnvironmentConfig envConfig,
                                               CTestBench testBench) {

        try {
            testBench.refreshSymbolsAndGroupsIfEmpty(jConnectionMgr.getMccMgr(), envConfig);
        } catch (Exception ex) {
            if (!(ex instanceof FileNotFoundException)) {
                throw ex;
            }
            // Ignore FNFE, which  is thrown only when there are no download files with symbols, or
            // `Load Symbols` in winIDEA dialog 'Files for download' is not checked.
            // User may still want to run system tests in such case 
            // to get nice testIDEA reports :-| B023891.
        }
    }


    private void configureHostVars(ISysDirs dirs, 
                                   CTestEnvironmentConfig envConfig) {
        
        CTestHostVars rootHostVars = new CTestHostVars();
        rootHostVars.setDirs(dirs.getWinIDEAWorkspaceDir(), 
                             dirs.getIyamlDir(), 
                             dirs.getReportDir());
        
        rootHostVars.setDefaultCoreId(envConfig.getConfiguredCoreID(""));
        rootHostVars.initEnvVars();
        rootHostVars.initBatchVars();
        
        m_hostVarsStack = new Stack<CTestHostVars>();
        m_hostVarsStack.push(rootHostVars);
    }

    
    public void clearStatusView() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                StatusModel.instance().clear();
            }});
    }

    
    /* private void disableInterrupts(String [] coreIds) {
        
        m_interruptEnableStatusMap.clear();
        
        for (String coreId : coreIds) {
            CIDEController ideCtrl = ControllerPool.instance().getCIDEController(coreId);
            int interruptsDisableState = ideCtrl.setInterruptsDisable();
            m_interruptEnableStatusMap.put(coreId, interruptsDisableState);
        }
    }
    
    
    private void restoreInterrupts(String [] coreIds) {
        
        for (String coreId : coreIds) {
            CIDEController ideCtrl = ControllerPool.instance().getCIDEController(coreId);
            ideCtrl.setInterruptsDisable(m_interruptEnableStatusMap.get(coreId));
        }
    } */
    
    
    private void runTestsRecursive(JTestCase jTestCase, 
                                   CTestSpecification testSpec, 
                                   int derivedLevel,
                                   IProgressMonitor monitor,
                                   ISysDirs dirs,
                                   IDebugHandler debugHandler, 
                                   int noOfTestsToRun,
                                   boolean isQuickDebug, 
                                   boolean isInitAnalyzers, 
                                   boolean isRunTestWAnalyzers,
                                   CTestEnvironmentConfig runConfiguration,
                                   CTestFilterController filterCtrl,
                                   boolean isDryRun) {

        // do not run specs which have explicitly set run flag, or specs which
        // are used as containers of other specs only.
        CTestSpecification mergedTestSpec = testSpec.merge();
        
        if (testSpec.getRunFlag() != ETristate.E_FALSE  &&  !mergedTestSpec.isEmptyExceptDerived()) {
            
            if(monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            String coreId = testSpec.getCoreId();
            coreId = CoreIdUtils.getConfiguredCoreID(runConfiguration, coreId);
            
            ConnectionMgr connectionMgr = m_jConnectionMgr.getMccMgr().getConnectionMgr(coreId);

            CDebugFacade debug = m_jConnectionMgr.getCDebugFacade(coreId);
            
            boolean isMeasureStackUsage = isMeasureStackUsage(runConfiguration, 
                                                              coreId);   
            
            if(monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            
            Boolean isTestBatchOn = m_testBatchStateMap.get(coreId);
            if (isTestBatchOn == null) {
                isTestBatchOn = Boolean.FALSE;
            }
            
            try {
                CTestResult result = null;
                if (isTestExecutable(mergedTestSpec, m_testFilter, m_script, true, filterCtrl, null)) {
                    
                    String subTaskDesc = mergedTestSpec.getTestId() + " / " + 
                            mergedTestSpec.getFunctionUnderTest(true).getName() + 
                            "    (" + m_currentTestNo + " / " + noOfTestsToRun + ")"; 
                    monitor.subTask(subTaskDesc);
                    m_currentTestNo++;

                    boolean isAnalyzerOff = 
                        mergedTestSpec.getAnalyzer(true).getRunMode() == CTestAnalyzer.ERunMode.M_OFF;
                    
                    if (isRunTestWAnalyzers  ||  isAnalyzerOff) {

                        isTestBatchOn = execTestCaseInitSequence(monitor, connectionMgr, 
                                                                 debug, isTestBatchOn, 
                                                                 subTaskDesc);
                        
                        CTestHostVars testCaseHostVars = m_hostVarsStack.peek();
                        testCaseHostVars.initTestCaseVars(mergedTestSpec, null);
                        
                        JTestCase.appendToStatusView(testSpec.getUILabel() + '\n');
                        StrVector analyzerFilesToMerge = getFilesForCvrgMerge(mergedTestSpec, filterCtrl);
                        
                        isTestBatchOn = 
                        jTestCase.runTest(mergedTestSpec,
                                          connectionMgr, 
                                          dirs,
                                          debugHandler,
                                          runConfiguration.getTestTimeout(),
                                          isQuickDebug,
                                          m_script,
                                          isInitAnalyzers,
                                          isMeasureStackUsage,
                                          monitor,
                                          isTestBatchOn,
                                          testCaseHostVars,
                                          isDryRun,
                                          analyzerFilesToMerge);
                        
                        m_testBatchStateMap.put(coreId, isTestBatchOn);
                        result = jTestCase.getTestResult();
                        if (isDryRun) {
                            m_isDryRunChange = CTestCase.applyDryRun(testSpec, 
                                                                     mergedTestSpec, 
                                                                     result);
                        }
                    }
                    monitor.worked(1);
                    
                    storeResult(testSpec, result);
                }
            } catch (Exception ex) {

                if (isDryRun) {
                    try {
                        CTestResult result = jTestCase.getTestResult();
                        m_isDryRunChange = CTestCase.applyDryRun(testSpec, 
                                                                 mergedTestSpec, 
                                                                 result);
                    } catch (Exception drex) {
                        // ignore exception here - we just tried to apply possible dry
                        // run values. If failed, we are in exception handling part anyway.
                    }
                }
                
                m_testBatchStateMap.put(coreId, jTestCase.isTestBatchOn());
                JTestCase.appendToStatusView("  [ERROR]\n");

                monitor.worked(1);
                StringBuilder sb = new StringBuilder();
                // Stack trace is confusing for normal users more often than helpful
                // so set stack level to 0 
                //sb.append("----------- Stack trace -----------\n\n");
                if (ex instanceof SException) {
                    // SEXception and derived exceptions contain meaningful error 
                    // messages and occur usually because of user's fault. Because 
                    // of error message their location is easy to find, so we don't need stack trace. 
                    sb.append(SEFormatter.getInfo(ex, true));
                } else {
                    // Standard exceptions should not be thrown - they indicate error
                    // in testIDEA program code - user should be able to report it.
                    // For example, getInfo(ex) returns only 'null' for NullPointerException,
                    // or -1 for IndexOutOfBoundsException.
                    sb.append(SEFormatter.getInfoWithStackTrace(ex, 5));
                }
                
                CTestResult result = new CTestResult(mergedTestSpec,
                                                     sb.toString());
                storeResult(testSpec, result);
            } 
        }
        
        if (derivedLevel > 0) {
            int numDerivedTestSpecs = testSpec.getNoOfDerivedSpecs();
            m_hostVarsStack.push(new CTestHostVars(m_hostVarsStack.peek()));
            for (int idx = 0; idx < numDerivedTestSpecs; idx++) {

                if (m_debugHandler != null  &&  
                        m_debugHandler.getLastResponse() == UserResponse.SKIP_CLEANUP_AND_STOP) {
                    return;
                }
                
                runTestsRecursive(jTestCase, testSpec.getDerivedTestSpec(idx), 
                        --derivedLevel, monitor, dirs, debugHandler, noOfTestsToRun,
                        isQuickDebug, isInitAnalyzers, isRunTestWAnalyzers, runConfiguration,
                        filterCtrl,
                        isDryRun);
            }
            m_hostVarsStack.pop();
        }
    }

    
    private StrVector getFilesForCvrgMerge(CTestSpecification mergedTestSpec,
                                           CTestFilterController filterCtrl) {
        
        CTestSpecification container = 
                CTestBench.getCvrgFilterCandidates(mergedTestSpec);
                
        StrVector trdFileList = new StrVector();
        
        CTestAnalyzer analyzer = mergedTestSpec.getAnalyzer(true);
        CTestAnalyzerCoverage cvrg = analyzer.getCoverage(true);
        
        CTestFilter filter = cvrg.getMergeFilter(true);
        int numTests = container.getNoOfDerivedSpecs();
        CTestSpecification testSpec;

        for (int idx = 0; idx < numTests; idx++) {
            
            testSpec = container.getDerivedTestSpec(idx);
            
            CTestSpecification mergedDerivedTs = testSpec.merge();
            
            // Only cvrg of test cases, which pass filter specified in Coverage 
            // section is merged.
            if (isTestExecutable(mergedDerivedTs, filter, m_script, false, filterCtrl, null)) {
                
                CTestResult testResult = m_resultsContainer.getTestResult(testSpec); 
                if (testResult == null) {
                    // we could try to get analyzer file name from test case, but this is 
                    // tricky, since analyzer file names may contain date/time/uid
                    // components, and this approach would not work in these cases. 
                    // Furthermore, information in these trd files may be 
                    // outdated. If users want to test one test case which merges
                    // coverage info, they can select 'Test | Keep Test Results'.
                    throw new SIllegalStateException("Test case has no result to use for coverage merging!")
                    .add("testCaseWithMergedCoverage", mergedTestSpec.getTestId())
                    .add("testCaseWithMissingResult", testSpec.getTestId());
                }
                
                
                String analyzerFileName = testResult.getAnalyzerFileName();
                if (!analyzerFileName.isEmpty()) {
                    trdFileList.add(analyzerFileName);
                    
                    if (mergedDerivedTs.getAnalyzer(true).isSaveAfterTest() != ETristate.E_TRUE) {
                        throw new SIllegalArgumentException("All files must be saved when merging coverage! "
                            + "Set option 'Save after test' in Analyzer section!").
                              add("Test case performing merge" , mergedTestSpec.getTestId()).
                              add("Test case without saved analyzer file", testSpec.getTestId());
                    }
                }
            }
        }
        
        return trdFileList;
    }


    /**
     * Returns siblings 
     * @param container
     * @param originalTestSpec
     */
//    private void getPrecedingSiblings(CTestSpecification container,
//                                       CTestSpecification originalTestSpec) {
//        
//        CTestSpecification parent = originalTestSpec.getParentTestSpecification();
//        int endIdx = parent.findDerivedTestSpec(originalTestSpec);
//        for (int idx = 0; idx < endIdx; idx++) {
//            CTestSpecification precedingSiblingTestSpec = parent.getDerivedTestSpec(idx);
//            container.addDerivedTestSpec(-1, precedingSiblingTestSpec);
//            // add derived tests only for preceding siblings, because all their
//            // derived tests are executed up to this point. This is not true for
//            // parent - only its derived tests up to the current test have been 
//            //executed.
//            addDerived(container, precedingSiblingTestSpec);
//        }
//    }
//    
//
//    private void addDerived(CTestSpecification container,
//                            CTestSpecification preceedingSiblingTestSpec) {
//        int numDerived = preceedingSiblingTestSpec.getNoOfDerivedSpecs();
//        for (int idx = 0; idx < numDerived; idx++) {
//            CTestSpecification derivedTest = preceedingSiblingTestSpec.getDerivedTestSpec(idx);
//            container.addDerivedTestSpec(-1, derivedTest);
//            addDerived(container, derivedTest);
//        }        
//    }


    private boolean isMeasureStackUsage(CTestEnvironmentConfig runConfig, 
                                        String coreId) {

        CTestBaseList stackUsageList = runConfig.getStackUsageOptions(true);
        
        int coreIdx = CoreIdUtils.getCoreIdIndex(runConfig,
                                                 coreId, 
                                                 true);
        
        int numItems = (int) stackUsageList.size();

        if (numItems > coreIdx) {
            CStackUsageConfig stackUsageCfg = CStackUsageConfig.cast(stackUsageList.get(coreIdx));
            return !stackUsageCfg.isEmpty()  &&  stackUsageCfg.isActive();
        }
        
        return false;
    }

    
    private void storeResult(CTestSpecification testSpec, CTestResult result) {
        if (result != null) {
            m_resultsContainer.putTestResult(testSpec, result);
            appendToStatusModel(result);
        }
    }


    private void appendToStatusModel(final CTestResult result) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                StatusModel.instance().appendResult(result);
            }
        });
    }

    
    // This method is duplicated in CTestBench::execTestCaseInitSequence(), but
    // this one contains user interaction.
    private boolean execTestCaseInitSequence(IProgressMonitor monitor,
                                             ConnectionMgr connectionMgr,
                                             CDebugFacade debug, 
                                             boolean isTestBatchOn,
                                             String subTaskDesc) {

        m_exception = null;
        
        if (m_testCaseTargetInitConfig.isDownloadOnTCInit()  ||  
                m_testCaseTargetInitConfig.isResetOnTCInit()  ||
                m_testCaseTargetInitConfig.isRunOnTCInit()) {
            
            // restore registers in case of init BEFORE target state is modified, otherwise
            // restoring the registers corrupts target state after download/reset/run.
            if (isTestBatchOn) {
                CTestCaseController.setTestBatch(connectionMgr, false);
                isTestBatchOn = false;
            }
        }

        if (m_testCaseTargetInitConfig.isDownloadOnTCInit()) {
            monitor.subTask(subTaskDesc + " / Downloading ...\n" +
                    "(Check winIDEA status if download does not finish on time.)");
            debug.download();
        }

        if (monitor.isCanceled()) {
            return isTestBatchOn;
        }

        if (m_testCaseTargetInitConfig.isResetOnTCInit()) {
            monitor.subTask(subTaskDesc + " / Resetting ...");
            debug.reset();
        }

        if (monitor.isCanceled()) {
            return isTestBatchOn;
        }

        if (m_testCaseTargetInitConfig.isRunOnTCInit()) {

            String functionName = m_testCaseTargetInitConfig.getStopFunctionOnTCInit();

            monitor.subTask(subTaskDesc + " / Running until function '" + functionName + "' ...");

            if (functionName.isEmpty()) {
                debug.run(); // target should stop on some breakpoint
            } else {
                debug.runUntilExpression(functionName);
            }

            monitor.subTask(subTaskDesc + " / Waiting for target to stop ...");
            waitUntilStopped(debug, monitor);
        }
        
        monitor.subTask(subTaskDesc);
        
        return isTestBatchOn;
    }

    
    // These two methods are duplicated in Java, since the C++ code can't execute 
    // scripts and therefore can not handle CTestFilter with script.  
    private int countExecutableTests(CTestSpecification testSpec, 
                                     int derivedLevel, 
                                     CTestFilter testFilter,
                                     CTestFilterController filterCtrl) {
        
        int count = 0;
        
        CTestSpecification mergedTestSpec = testSpec.merge();
        mergedTestSpec.setRunFlag(testSpec.getRunFlag()); // this one is not merged
        
        if (isTestExecutable(mergedTestSpec, testFilter, m_script, true, filterCtrl, null)) {
            count++;
        }
        
        if (derivedLevel > 0) {
            int numDerivedTestSpecs = testSpec.getNoOfDerivedSpecs();
            for (int idx = 0; idx < numDerivedTestSpecs; idx++) {
                count += countExecutableTests(testSpec.getDerivedTestSpec(idx), 
                                              --derivedLevel, 
                                              testFilter, 
                                              filterCtrl);
            }
        }
        
        return count;
    }

    
    public static boolean isTestExecutable(CTestSpecification testSpec, 
                                           CTestFilter testFilter,
                                           Script script,
                                           boolean isTestSpecMerged,
                                           CTestFilterController filterCtrl,
                                           StringBuilder scriptOutput) {
        
        if (testSpec.getRunFlag() == ETristate.E_FALSE) {
            return false;
        }
        
        if (testSpec.isEmptyExceptDerived()) {
            return false;
        }
        
        if (testFilter == null) {
            return true;
        }
        
        if (testFilter.getFilterType() == EFilterTypes.SCRIPT_FILTER) {
            TestScriptResult scriptResult = filterWithScript(testSpec, 
                                                             testFilter, 
                                                             script);
            if (scriptResult != null) {
                List<String> funcRetVal = scriptResult.getFuncRetVal();

                if (scriptOutput != null) {
                    // scriptOutput.append(UiUtils.scriptResult2StatusViewText(scriptResult));
                    scriptOutput.append(scriptResult.toString());
                }

                return (funcRetVal != null  &&  !funcRetVal.isEmpty());
            } else {
                // if scriptResult == null, then script function name is not defined, pass all until it is
                return true; 
            }
        } else {
            // CTestFilterController filterCtrl = GlobalsConfiguration.instance().getFilterController();
            if (filterCtrl == null) {
                return false;
            }
            return filterCtrl.filterTestSpec(testSpec, isTestSpecMerged, testFilter);
        }
    }


    public static TestScriptResult filterWithScript(CTestSpecification testSpec,
                                                    CTestFilter testFilter,
                                                    Script script) {
        StrVector scriptFuncParams = new StrVector();
        testFilter.getScriptFunctionParams(scriptFuncParams);
        String[] scriptFuncParamsArray = 
            DataUtils.strVector2StringArray(scriptFuncParams);
        
        try {
            TestScriptResult filterResult = 
                script.callFunction(testSpec,
                                    CTestResultBase.getSE_FILTER(), 
                                    testFilter.getScriptFunction(), 
                                    scriptFuncParamsArray);
            
            return filterResult;
            
        } catch (SIOException ex) {
            throw new SException("Calling of script function for test filtering failed!", ex).
                      add("scriptFunction", testFilter.getScriptFunction());
        }
    }

    
    /** Returns true if monitor was canceled. */
    private boolean waitUntilStopped(CDebugFacade debug, IProgressMonitor monitor) {
        while (!debug.waitUntilStopped(Activator.DEFAULT_WAIT_FOR_TARGET_DELAY)) {
            if (monitor.isCanceled()) {
                m_isMonitorCanceled = true;
                return true;
            }
        }
        return false;
    }
    
    
    private boolean verifySymbols(TestSpecificationModel model, CTestTreeNode testTreeNode, int derivedLevel) {
        
        final int MAX_MESSAGES = 5;
        
        try {
            CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
            JConnection jConnectionMgr = ConnectionProvider.instance().getDefaultConnection();
            if (jConnectionMgr == null) {
                return false; // user clicked cancel in connect dialog
            }
            
            refreshSymbolsAndGroups(jConnectionMgr, envConfig, model.getTestBench());
            
            final ModelVerifier modelVerifier = ModelVerifier.instance();
            List<TestSpecStatus> statusList = 
                    modelVerifier.verifyTestTreeNodeAndChildren(testTreeNode, 
                                                                derivedLevel);
            
            if (testTreeNode.isGroup()) {
                CTestGroup group = CTestGroup.cast(testTreeNode);
                if (modelVerifier.askForAutoSetSaveAnalyzerFile(model, group)) {
                    // if model was changed, repeat symbol analysis
                    statusList = modelVerifier.verifyTestTreeNodeAndChildren(testTreeNode, 
                                                                             derivedLevel);
                }
            }
            
            if (!statusList.isEmpty()) {
                
                StatusModel.instance().setTestSpecStatus(statusList);
                StatusModel.instance().updateTestResults(null);
                
                StringBuilder sb = new StringBuilder();
                ModelVerifier.statusList2String(statusList, sb, MAX_MESSAGES);

                return MessageDialog.openConfirm(Activator.getShell(), 
                                                 "Verify Symbols",
                                                 "The following warnings were found:\n" 
                                                 + "==================================\n\n"
                                                 + sb.toString()
                                                 + "\n\nSee details in status view. Do you want to continue?");
            }
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Symbol verification failed!", ex);
        }
        
        return true; // execute tests also if exception was thrown - missing 
        // symbols is not critical error - some test may still be executed.
    }
}


class TestDebugHandler implements IDebugHandler
{
    private UserResponse m_lastResponse;
    @Override
    public UserResponse handleException(final Exception ex) {
        
        Display.getDefault().syncExec(new Runnable() {
            
            @Override
            public void run() {
                boolean isContinue = MessageDialog.openConfirm(Activator.getShell(), 
                                                               "Test execution error", 
                                                               "Test error:\n" + ex.getMessage() + 
                                                               "\n\nYou can analyze target state in winIDEA.\n" +
                                                               "When finished, click:\n" + 
                                                               "- 'OK' to continue with other tests\n" +
                                                               "- 'Cancel' to stop testing.");
                if (isContinue) {
                    m_lastResponse = IDebugHandler.UserResponse.CLEANUP_AND_CONTINUE;
                } else {
                    m_lastResponse = IDebugHandler.UserResponse.SKIP_CLEANUP_AND_STOP;
                }
            }
        });
        
        return m_lastResponse;
    }

    @Override
    public UserResponse waitForCleanup() {
        // not used at the moment, provided for future use, if multiple
        // debug levels will be introduced
        return null;
    }

    @Override
    public UserResponse getLastResponse() {
        return m_lastResponse;
    }

    @Override
    public UserResponse handleUnexpectedStop() {
        
        Display.getDefault().syncExec(new Runnable() {
            
            @Override
            public void run() {
                boolean isContinue = MessageDialog.openConfirm(Activator.getShell(), 
                                                               "Test execution stopped! ",
                                                               "Test execution stopped. " +
                                                               "You can debug target code in winIDEA.\n" +
                                                               "When finished, click:\n" + 
                                                               "- 'OK' to continue with this test\n" +
                                                               "- 'Cancel' to stop testing.");
                if (isContinue) {
                    m_lastResponse = IDebugHandler.UserResponse.CLEANUP_AND_CONTINUE;
                } else {
                    m_lastResponse = IDebugHandler.UserResponse.SKIP_CLEANUP_AND_STOP;
                }
            }
        });
        
        return m_lastResponse;
    }
}
