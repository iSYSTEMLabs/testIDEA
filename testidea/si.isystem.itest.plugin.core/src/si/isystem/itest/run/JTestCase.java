package si.isystem.itest.run;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import si.isystem.commons.connect.ConnectionPool;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CAddressController;
import si.isystem.connect.CDebugFacade;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CLineLocation;
import si.isystem.connect.CMemAddress;
import si.isystem.connect.CPUStatus;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestCaseController;
import si.isystem.connect.CTestDiagrams;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestPointResult;
import si.isystem.connect.CTestPointResult.EExecStatus;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResultBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStopCondition;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CValueType;
import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.ETristate;
import si.isystem.connect.IConnectDebug;
import si.isystem.connect.IConnectTest.EState;
import si.isystem.connect.ITestCaseController;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.ISysDirs;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.diagrams.DiagramUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.run.IDebugHandler.UserResponse;
import si.isystem.python.InStreamListener;


/**
 * This class is Java implementation of CTestCase. It contains no state from
 * one test case to another, so it safe to reuse it.
 */
public class JTestCase implements InStreamListener {

    private CTestResult m_testResult;
    private boolean m_isAppendedToStatusView;
    private boolean m_isTestBatchOn;

    private final static int CANCEL_READ_INTERVAL = 2000; // if testIDEA detects
                                                          // Cancel button in 2 s, it's OK
    
    
    /**
     * This function executes one test case.
     * 
     * @param testSpec
     * @param connectionMgr
     * @param debugHandler if null it is normal run, otherwise it is debug run
     * @param isQuickDebugMode
     * @param script
     * @param isMeasureStackUsage 
     * @param monitor
     * @param isDryRun 
     */
    public boolean runTest(CTestSpecification testSpec,
                           // Java specific parameters
                           ConnectionMgr connectionMgr,
                           ISysDirs dirs,
                           IDebugHandler debugHandler,
                           int testGlobalTimeout,
                           boolean isQuickDebugMode,
                           Script script, 
                           boolean isInitAnalyzers,
                           boolean isMeasureStackUsage, 
                           IProgressMonitor monitor,
                           boolean isTestBatchOn,
                           CTestHostVars hostVars, 
                           boolean isDryRun,
                           StrVector analyzerFilesToMerge) {

        m_isAppendedToStatusView = false;  // detect if some script output has been appended
        m_isTestBatchOn = isTestBatchOn;
        boolean isDebug = debugHandler != null;

        TestScriptResult scriptInitFunc = null; 
        TestScriptResult scriptEndFunc = null;
        TestScriptResult scriptInitTarget = null;
        
        CTestCase testCase  = new CTestCase(connectionMgr, hostVars);
        try {

            script.setInStreamListener(this);

            CTestFunction initTargetFunction = testSpec.getInitTargetFunction(true);
            if (m_isTestBatchOn  &&  !initTargetFunction.getName().isEmpty()) {
                // if user defined init target function, this function may execute operation, which
                // invalidates caching of registers (download, reset, ...). To prevent itest error
                // in such cases, always restore the registers before calling this script function. 
                // Another solution would be additional config. item for storing registers in iyaml, but 
                // this would be hard to understand option exposing testIDEA internals.
                CTestCaseController.setTestBatch(connectionMgr, false);
                m_isTestBatchOn = false;
            }
            
            scriptInitTarget = script.callCTestFunction(testSpec,
                                                        testCase.getHostVars(),
                                                        CTestResultBase.getSE_INIT_TARGET(),
                                                        initTargetFunction);
            
            List<TestScriptResult> stubScriptResults = new ArrayList<TestScriptResult>();

            // this method is not in try/catch block, because all cleanup in case of 
            // exceptions is done in C++ code. The cleanup depends on location of
            // the exception, for example if testCtrl.init() is called or not
            m_isTestBatchOn = testCase.runTest_init_target(testSpec, isDebug, 
                                                           m_isTestBatchOn);

            runUntilStopPoint(testCase,
                              testSpec.getBeginStopCondition(true), 
                              testSpec.getTestId(), 
                              testSpec.getTestScope() != ETestScope.E_SYSTEM_TEST,
                              monitor,
                              isDebug);

            int testCaseTimeout = testSpec.getTestTimeout();
            if (testCaseTimeout == -1) {
                testCaseTimeout = testGlobalTimeout;
            }

            appendToStatusView(isDebug, "Initializing test - stack measurement, options, "
                    + "perist. var, local vars, stubs, test points, and analyzer");
        
            testCase.runTest_init_test(isInitAnalyzers, 
                                       isMeasureStackUsage,
                                       testCaseTimeout);

            if (!testCase.getTestResults().isPreConditionError()) {

                appendToStatusView(isDebug, "test initialized");

                try {
                    script.setTestHandle(testCase.getTestController().getTestCaseHandle());

                    appendToStatusView(isDebug && !testSpec.isSectionEmpty(SectionIds.E_SECTION_INITFUNC),
                            "Calling script 'Init' function ...");
                    scriptInitFunc = script.callCTestFunction(testSpec,
                                                              testCase.getHostVars(),
                                                              CTestResultBase.getSE_INIT_FUNC(), 
                                                              testSpec.getInitFunction(true));
                    appendToStatusView(isDebug && !testSpec.isSectionEmpty(SectionIds.E_SECTION_INITFUNC), 
                            "Script 'Init' function returned.");

                    stopOnFuncStartIfQuickDebug(testCase, testSpec, isQuickDebugMode,
                                                debugHandler, monitor);

                    appendToStatusView(isDebug, "Getting stack info.");
                    testCase.runTest_exec_begin();
                    boolean isResumeCoverage = false;
                    boolean isStubOrTestPoint = false;

                    appendToStatusView(isDebug, "Starting test.");
                    do {
                        testCase.runTest_exec_loopStart(testSpec, isResumeCoverage, isStubOrTestPoint);
                        // wait for stop or user cancel
                        appendToStatusView(isDebug, "Waiting for target to stop.");
                        while (!testCase.runTest_exec_waitForStop(testSpec, 1000)) { // 1s timeout for unit tests
                            // user can cancel test execution
                            if (checkCancelButton(testCase, monitor)) {
                                throw new SIOException("Test was interrupted by user!");
                            }
                        }

                        appendToStatusView(isDebug, "Handling stop state ...");

                        isStubOrTestPoint = runTest_exec_langSpecific(testCase, testSpec, script,
                                                                      debugHandler, stubScriptResults,
                                                                      isDebug,
                                                                      testCaseTimeout);

                        appendToStatusView(isDebug  &&  isStubOrTestPoint, 
                                "Continuing run from stub or test point.");
                        isResumeCoverage = true;
                    } while (isStubOrTestPoint);

                    appendToStatusView(isDebug, "Test execution stopped.");
                    testCase.runTest_exec_end();

                    appendToStatusView(isDebug && !testSpec.isSectionEmpty(SectionIds.E_SECTION_ENDFUNC),
                            "Calling script 'End' function ...");
                    scriptEndFunc = script.callCTestFunction(testSpec,
                                                             testCase.getHostVars(),
                                                             CTestResultBase.getSE_END_FUNC(),
                                                             testSpec.getEndFunction(true));
                    appendToStatusView(isDebug && !testSpec.isSectionEmpty(SectionIds.E_SECTION_INITFUNC), 
                            "Script 'End' function returned.");

                    StrVector differences = new StrVector(); // ignored by testIDEA, gets diffs from test result
                    appendToStatusView(isDebug, "Getting analyzer results.");

                    testCase.runTest_finalize(differences, isDryRun, analyzerFilesToMerge);

                } catch (Exception ex) {
                    try {
                        testCase.handleException();
                    } catch (Exception exhe) {
                        ex.addSuppressed(exhe);
                    }

                    // if assert fails, some information is already available, for
                    // example Dry Run assignments, which can be applied later 
                    m_testResult = testCase.getTestResults();

                    try {
                        if (m_isTestBatchOn) {
                            // toggle to restore registers in case of exception
                            testCase.getTestController().setTestBatchNS(false);
                            testCase.getTestController().setTestBatchNS(true);
                        }
                    } catch (Exception exhe) {
                        ex.addSuppressed(exhe);
                    }

                    throw ex;
                }
            }
        } finally {
            // Persistent vars must be deleted regardless of test cases result,
            // even if test case fails because function under test is not found.
            // This makes test execution more robust - for example, if function
            // was removed or renamed, persist vars must be deleted even if test 
            // init fails, otherwise next test case may also fail if it creates 
            // persist var with the same name. 
            testCase.deletePersistentVars(testSpec.getPersistentVars(true));
        }
        
        appendToStatusView(isDebug, "Storing test result.");
        
        getResults_langSpecific(testCase, testSpec,
                                dirs,
                                debugHandler, script,
                                scriptInitTarget, scriptInitFunc, 
                                scriptEndFunc,
                                monitor);
        
        appendToStatusView(isDebug, "Test case ended. isTestBatchOn = " + m_isTestBatchOn);
        
        return m_isTestBatchOn;
    }
        

    // getter, since in case of exception the state of testBatch is not returned
    public boolean isTestBatchOn() {
        return m_isTestBatchOn;
    }


    /**
     * This method should be implemented in each language, because Python scripts 
     * can not be called from C++ code, and reports need to be accumulated. 
     * 
     * @param testCase
     * @param testSpec
     * @param script
     * @param debugHandler
     * @param stubScriptResults
     * @param isDebug 
     * @return true if stopped on stub or test point, false otherwise
     */
    private boolean runTest_exec_langSpecific(CTestCase testCase, CTestSpecification testSpec,
                                              Script script, IDebugHandler debugHandler, 
                                              List<TestScriptResult> stubScriptResults, 
                                              boolean isDebug,
                                              int timeout) {
        
        ITestCaseController itestCaseCtrl = testCase.getTestController();
        EState status = itestCaseCtrl.getStatus();

        testCase.waitForAnalyzerToDownloadData(timeout);

        
        switch (status) {
        case stateStub:  
            appendToStatusView(isDebug, "Stub hit.");
            
            callStubs(testSpec, testCase, itestCaseCtrl, script, stubScriptResults);
            
            if (stubScriptResults.size() > 0) {
                appendToStatusView(isDebug, stubScriptResults.get(stubScriptResults.size() - 1).toUIString());
            }
            return true;
        case stateUnexpectedStop:  
            boolean isTestPoint = execTestPoints(testSpec, testCase, 
                                                 itestCaseCtrl, 
                                                 script, 
                                                 stubScriptResults);
            
            appendToStatusView(isDebug, isTestPoint ? "Test point hit." : "Unexpected stop");

            if (stubScriptResults.size() > 0) {
                appendToStatusView(isDebug, stubScriptResults.get(stubScriptResults.size() - 1).toUIString());
            }
            
            // show dialog in case of unexpected stop in debug mode 
            boolean isSystemTestStopped = testCase.isSystemTestStopOnBP();
            if (!isTestPoint  &&  !isSystemTestStopped) {
                // throws exception if not in debug mode or user cancels
                consultUser(debugHandler, status); 
                return true; // continue the loop
            }
            return isTestPoint;
            
        case stateException:
            appendToStatusView(isDebug, "Exception caught.");

            // must overwrite flag, which is set in lang specific code 
            testCase.setTargetException(true);
            return false;
            
        case stateAborted:
        case stateEnded:
        case stateInitialized:
        case stateOffline:
        case statePaused:
        case statePersistentReady:
        case stateRunning:
        case stateStop:
        case stateSystemTest:
        default:
            // for all other status values false should be returned
        }

        return false;
    }


    /** Stops target and returns true, if user presses 'Cancel' in progress monitor. */
    private boolean checkCancelButton(CTestCase testCase, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            testCase.stopTest();
            return true;
        }
        return false;
    }
    

    /**
     * This method stops on tested function start if quick debug button is 
     * pressed in testIDEA and unit test is run. Otherwise target is not run, 
     * but user gets opportunity to observer target state before the tests is 
     * started (after BEGIN stop condition is met for system tests).\  
     * @param testCase
     * @param testSpec
     * @param isQuickDebugMode
     * @param debugHandler
     * @param monitor
     */
    void stopOnFuncStartIfQuickDebug(CTestCase testCase,
                                     CTestSpecification testSpec,
                                     boolean isQuickDebugMode,
                                     IDebugHandler debugHandler,
                                     IProgressMonitor monitor) {        
        UserResponse response = UserResponse.CLEANUP_AND_CONTINUE;
        if (isQuickDebugMode) {
            if (testSpec.getTestScope() == ETestScope.E_UNIT_TEST) {
                CTestFunction testFunc = testSpec.getFunctionUnderTest(true);
                runUntilFunctionStart(testCase, testFunc.getName(), monitor, 
                                      testCase.getTestController());
            } // no running in case of system tests - target has already been run at start of this m.

            response = debugHandler.handleUnexpectedStop();

            if (response == UserResponse.SKIP_CLEANUP_AND_STOP) {
                testCase.clearTest();
                m_testResult = new CTestResult(testSpec, "Quick debugging terminated!");
                return;
            }
        }
    }

    
    void getResults_langSpecific(CTestCase testCase, 
                                 CTestSpecification testSpec, 
                                 ISysDirs dirs,
                                 IDebugHandler debugHandler,
                                 Script script, 
                                 TestScriptResult scriptInitTarget, 
                                 TestScriptResult scriptInitFunc, 
                                 TestScriptResult scriptEndFunc, 
                                 IProgressMonitor monitor) {
        try {
            if (debugHandler != null) {
                debugHandler.waitForCleanup();
            }

            m_testResult = testCase.getTestResults();
            
            TestScriptResult scriptRestoreTarget = script.callCTestFunction(testSpec,
                                                                            testCase.getHostVars(),
                                                                            CTestResultBase.getSE_RESTORE_TARGET(),  
                                                                            testSpec.getRestoreTargetFunction(true));
            
            setScriptResultForTest(m_testResult, scriptInitTarget);
            setScriptResultForTest(m_testResult, scriptInitFunc);

            // script results are now stored in script results vector
//            for (TestScriptResult scriptStubResult : scriptStubResults) {
//                setScriptResultForTest(m_testResult, scriptStubResult);
//            }
            setScriptResultForTest(m_testResult, scriptEndFunc);
            setScriptResultForTest(m_testResult, scriptRestoreTarget);
            
            drawDiagrams(testSpec, dirs, monitor);
            
            if (m_testResult.isError()) {
                appendToStatusView("  [FAILED]\n");
            } else {
                appendToStatusView("  [OK]\n");
            }
            
            if (m_isAppendedToStatusView) {
                appendToStatusView("---------\n");
            } 

        } catch (Exception ex) {
            try {
                if (debugHandler != null) {
                    UserResponse response = debugHandler.handleException(ex);
                    if (response == UserResponse.CLEANUP_AND_CONTINUE) {
                        testCase.clearTest();
                    }
                } // else clearTest() was called in C++ code
                
            } catch (Exception ex2) {
                throw new SException("Test execution failed!")
                                .add("originalError", ex.getMessage())
                                .add("cleanupError", ex2.getMessage());
            }
            throw new SException("Test execution failed!", ex);
        }
    }

    
    static void setScriptResult(CTestResultBase cResult, 
                                TestScriptResult scriptResult) {
        
        if (scriptResult != null) {
            String funcType = scriptResult.getFuncType();
            List<String> funcInfo = scriptResult.getFuncInfo();
            List<String> funcOutList = scriptResult.getFuncRetVal();

            if (funcInfo != null  &&  !funcInfo.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(UiUtils.list2Str(funcInfo, "    "));
                String infoStr = sb.toString();
                cResult.appendScriptOutput(funcType, infoStr);
            }
            
            if (funcOutList != null  &&  !funcOutList.isEmpty()) {
                String retValStr = UiUtils.list2Str(funcOutList, "    "); 
                cResult.appendScriptError(funcType, retValStr);
            }
            
        }
    }
    
    
    private void setScriptResultForTest(CTestResultBase cResult, 
                                        TestScriptResult scriptResult) {
        
        if (scriptResult != null) {
            setScriptResult(cResult, scriptResult);
            // This information is written twice when Debug mode is on. Fix it later
            // if there is any problem.
            appendToStatusView(scriptResult.toUIString());
            m_isAppendedToStatusView = true;
        }
    }
    
    
    static public void appendToStatusView(final String text) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                StatusModel.instance().appendDetailPaneText(StatusType.INFO, text);
            }});
    }


    public void appendToStatusView(boolean isDebug, final String text) {
        if (isDebug) {
            appendToStatusView("    DBG: " + text + '\n');
        }
    }


    private void drawDiagrams(CTestSpecification testSpec, 
                              ISysDirs dirs, 
                              IProgressMonitor monitor) {

        CTestDiagrams diagrams = testSpec.getDiagrams(true);
        if (diagrams.isActive() != ETristate.E_TRUE) {
            return;
        }
        
        // currently there is no timeout, as scripts should be written so that
        // they always return.
        DiagramUtils.createDiagramsInTestRuntime(dirs.getWinIDEAWorkspaceDir(),
                                                 dirs.getReportDir(), 
                                                 dirs.getDotExeDir(),
                                                 testSpec,
                                                 m_testResult,
                                                 0, 
                                                 monitor);
    }


    /**
     * Throws exception, if user canceled the operation. This method duplicates
     * similar method in C++, because it also monitors the Cancel button.
     * @param isDebug 
     */
    void runUntilStopPoint(CTestCase testCase,
                           CTestStopCondition stopCondition,
                           String testPointOrTestCaseId,
                           boolean isUnitTest, 
                           IProgressMonitor monitor, 
                           boolean isDebug)
    {
        if (isUnitTest ||  stopCondition.isEmpty()) {
            return; // normal return, no cancel
        }

        appendToStatusView(isDebug, "Run until 'Init test' condition is met ...");

        CDebugFacade debugFacade = testCase.getDebugFacade();
        CAddressController addressCtrl = debugFacade.getAddressController();
        
        CTestLocation bpLocation = stopCondition.getBreakpointLocation(true);
        switch (stopCondition.getStopType()) {
        case E_BREAKPOINT: {

            if (bpLocation.isEmpty()) {
                throw new SIllegalArgumentException("Stop condition for system test " +
                    "is set to breakpoint, but section 'stop point' is empty!\n" +
                    "Please specify stop point.")
                    .add("stopCondition", stopCondition.toString());
            }

            int timeout = stopCondition.getTimeout();
            if (timeout > 0) {
                debugFacade.run();
                sleepWithPolling(debugFacade, monitor, timeout);
            }
            CLineLocation lineLoc = addressCtrl.getSourceLocation(bpLocation.getLineDescription(),
                                                                    testPointOrTestCaseId);
            debugFacade.setBP((int)lineLoc.getLineNumber(), 
                                 lineLoc.getFileName(),
                                 stopCondition.getConditionCount(), 
                                 stopCondition.getConditionExpr());

            if (timeout == 0) {
                debugFacade.run();
            }

            
            while (!debugFacade.waitUntilStopped(CANCEL_READ_INTERVAL)) {
                if (monitor.isCanceled()) {
                    debugFacade.stop();
                    throw new SIOException("Test was interrupted by user!");
                }
            }
            debugFacade.deleteBP(lineLoc.getFileName(), (int)lineLoc.getLineNumber());
            } 
            break;
        case E_STOP:
            debugFacade.run();
            sleepWithPolling(debugFacade, monitor, stopCondition.getTimeout());
            debugFacade.stop();
            break;
        case E_RT_EXPRESSION: {

            debugFacade.run();
            int timeout = stopCondition.getTimeout();
            if (timeout > 0) {
                sleepWithPolling(debugFacade, monitor, timeout);
            }

            String rtExpression = stopCondition.getRtExpression();
            String result;
            long lResult = 0;
            do {
                CValueType value = debugFacade.evaluate(IConnectDebug.EAccessFlags.fRealTime, 
                                                        IConnectDebug.EEvaluate.efVagueFloatEqual,
                                                        rtExpression);
                result = value.getResult();
                if (monitor.isCanceled()) {
                    debugFacade.stop();
                    throw new SIOException("Test was interrupted by user!");
                }
                if (result.startsWith("0x")) {
                    lResult = Long.valueOf(result.substring(2), 16);
                } else {
                    lResult = Long.parseLong(result);
                }
            } while (lResult == 0);
            debugFacade.stop();
            }
            break;
        case E_NO_RUN:
            break;
        default:
            throw new SIllegalArgumentException("Invalid stop condition type!").
                add("stopConditionType", stopCondition.getStopType());
        }
        
        appendToStatusView(isDebug, "'Init/Stop test' condition met.");
    }


    private void sleepWithPolling(CDebugFacade debugFacade,
                                  IProgressMonitor monitor, int timeout) {
        
        try {
            long currentTime = System.currentTimeMillis();
            long endTime = currentTime + timeout;
            while (currentTime < endTime) {
                if (endTime - currentTime > CANCEL_READ_INTERVAL) {
                    Thread.sleep(CANCEL_READ_INTERVAL);
                } else {
                    Thread.sleep(endTime - currentTime);
                }
                if (monitor.isCanceled()) {
                    debugFacade.stop();
                    throw new SIOException("Test was interrupted by user!");
                }
                currentTime = System.currentTimeMillis();
            }
        } catch (InterruptedException ex) {
            // something is wrong, cancel the test!
            debugFacade.stop();
            throw new SIOException("Waiting thread was unexpectedly interrupted!");
        }
    }
    
    
    
    private void runUntilFunctionStart(CTestCase testCase,
                                       String functionName, 
                                       IProgressMonitor monitor,
                                       ITestCaseController testCaseCtrl) {
        CDebugFacade dbg = testCase.getDebugFacade();
        CMemAddress addr = dbg.getFunctionAddress(functionName);
        CPUStatus status = dbg.getCPUStatus();
        
        // Some architectures do not support code execution on stack, so the
        // execution point is already set to the start of the function.
        // For other architectures move the exec point to the start of the function. 
        if (addr.getMemArea() != status.getExecutionArea()  ||  
            addr.getAddress()  != status.getExecutionPoint()) {

            dbg.waitUntilStopped(2000, 50);
            dbg.setBP(functionName);
            // runUntilExpression() has been replaced with CTestCaseController.run()
            // because there are some additional actions on some targets which are
            // performed by CTestCaseController.
            testCaseCtrl.run();
            // dbg.runUntilExpression(functionName, ETimeoutMode.TOUT_10s);
            
            boolean isTimeout;
            do {
                isTimeout = !testCaseCtrl.waitUntilStopped(1000, 50);
                if (monitor.isCanceled()) {
                    testCase.stopTest();
                    break;
                }
            } while (isTimeout); // wait until the test completes or user presses 
                                 // the cancel button
            
            dbg.deleteBP(functionName);
        }
        
        JConnection jCon = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION);
        CIDEController ide = jCon.getCIDEController(null);
        ide.bringWinIDEAToTop();
    }
    
    
    void consultUser(IDebugHandler debugHandler, EState targetStatus) {
        
        if (debugHandler != null) {
            // it could be user's breakpoint in which case we should wait and ask
            UserResponse response = debugHandler.handleUnexpectedStop();
            if (response == UserResponse.CLEANUP_AND_CONTINUE) {
                return; // not a real stub, but let's make the loop repeat
            } else {
                throw new SIllegalStateException("Test stopped on user's request after unexpected stop!");
            }
        }
        
        throw new SIllegalStateException("Unexpected test stop!")
                  .add("state", targetStatus);
    }
    
    
    void callStubs(CTestSpecification testSpec, 
                   CTestCase testCase, 
                   ITestCaseController testCaseCtrl, 
                   Script script, 
                   List<TestScriptResult> scriptResults) 
    {
        StrVector scriptParams = new StrVector();
        CTestPointResult tpResult = new CTestPointResult(); 
        
        CTestStub stub = testCase.callStubs(false, 
                                            scriptParams,
                                            tpResult);
        
        String stubScriptFunc = stub.getScriptFunctionName();

        String metaData = "Stubbed function: '" + stub.getFunctionName() + 
                          "',  Script function: '" + stubScriptFunc + '\'';

        runScriptWTPResult(testSpec, testCase, script, stubScriptFunc, scriptParams, 
                           metaData, CTestResultBase.getSE_STUB(), scriptResults, 
                           tpResult);
        
        testCase.logStatus(testCaseCtrl, 
                           stub.getLogConfig(true), 
                           CTestLog.ESectionsLog.E_SECTION_AFTER.swigValue(),
                           tpResult.getLogResult(false));
    }


    boolean execTestPoints(CTestSpecification testSpec,
                           CTestCase testCase, 
                           ITestCaseController testCaseCtrl,
                           Script script,
                           List<TestScriptResult> scriptResults) {
        
        StrVector scriptParams = new StrVector();
        CTestPointResult tpResult = new CTestPointResult();
        
        // adds tpResult to the list of test point results in CTestCase C++ class
        CTestPoint testPoint = testCase.execTestPointEvalAssign(testCaseCtrl, 
                                                                scriptParams,
                                                                tpResult);

        if (testPoint == null) {
            return false;
        }
        
        // run script only if hit count and condition are true
        if (tpResult.getExecStatus() == EExecStatus.EXECUTED) {

            String scriptFunc = testPoint.getScriptFunctionName();
            String metaData = "Test point ID: '" + testPoint.getId() + 
                    "',   Script function: '" + scriptFunc + '\'';

            runScriptWTPResult(testSpec, testCase, script, scriptFunc, scriptParams, 
                               metaData, CTestResultBase.getSE_TEST_POINT(), 
                               scriptResults, tpResult);
        }

        // log always, even if hit count or condition were false, to provide info 
        // for debugging
        testCase.logStatus(testCaseCtrl, 
                           testPoint.getLogConfig(true), 
                           CTestLog.ESectionsLog.E_SECTION_AFTER.swigValue(),
                           tpResult.getLogResult(false));
        return true;
    }


    private void runScriptWTPResult(CTestSpecification testSpec,
                                    CTestCase testCase,
                                    Script script,
                                    String scriptFunc,
                                    StrVector scriptParams,
                                    String metaData,
                                    String scriptFuncType, 
                                    List<TestScriptResult> scriptResults,
                                    CTestPointResult tpResult) {
        if (!(scriptFunc.isEmpty())) {
            String[] paramsStrArray = new String[(int)scriptParams.size()];
            for (int idx = 0; idx < paramsStrArray.length; idx++) {
                paramsStrArray[idx] = testCase.replaceHostVariables(scriptParams.get(idx));
            }

            TestScriptResult scriptResult = script.callFunction(testSpec,
                                                                scriptFuncType,
                                                                scriptFunc,
                                                                paramsStrArray);

            if (scriptResult != null) {
                // this will go to test report
                tpResult.setScriptRetVal(UiUtils.list2Str(scriptResult.getFuncRetVal(), ""));
                tpResult.setScriptInfoVar(UiUtils.list2Str(scriptResult.getFuncInfo(), ""));

                // this will go to status view - contains also script output (print statements)
                scriptResult.setMetaData(metaData);
                scriptResults.add(scriptResult);
            }
        }
    }

    
    public CTestResult getTestResult() {
        return m_testResult;
    }


    @Override  // for interface InStreamListener
    public void setLine(String text) {
        appendToStatusView(text + '\n');
    }
}
