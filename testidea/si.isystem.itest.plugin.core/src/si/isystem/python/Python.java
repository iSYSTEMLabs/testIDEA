package si.isystem.python;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.core.runtime.IProgressMonitor;

import si.isystem.connect.utils.OsUtils;
import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CIDEController;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.STimeoutException;

/**
 * This class starts Python interpreter as external process and communicates
 * with it via stdio. Reasons to start external process instead of embedded
 * interpreter:
 * - better control over its lifetime - it is simple to kill process, but
 *   not thread. iconnect calls may block on winIDEA side. Athough they can be
 *   released by stopping target or closing winIDEA, this is not convenient.
 *   On the other hand, user's code may easily block.
 * - ability to run GUI Python applications. This may be especially handy with
 *   matplotlib, if customers want to plot some data.
 *   
 * Drawbacks of the current approach: performance. 
 * 
 * @author markok
 *
 */
public class Python {

    private Process m_process;
    private InputStream m_stdIn;
    private OutputStream m_stdOut;
    private InputStream m_stdErr;
    private BufferedWriter m_buffStdOut;
    private ReaderThread m_errReaderThread;
    private ReaderThread m_outReaderThread;
    private boolean m_isInteractive;
    
    private final static String PYTHON_EXECUTABLE_NO_EXT = "python";
    private final static String PYTHON_EXECUTABLE_EXE = "python.exe";
    private final static String PYTHON_EXECUTABLE_BAT = "python.bat";
    private final static String PYTHON_EXECUTABLE_SH = "python.sh";

    private final static String RET_VAR_NAME = "_isys_retVal";

    private static final boolean IS_DEBUG = false;
    
    
    /**
     * Instantiates this class, but does not start Python.
     */
    public Python() {
    }
    
    /**
     * This method returns instance of Python interpreter as it is configured 
     * in winIDEA.
     * 
     * @param jCon connection to winIDEA, must not be null and must be connected  
     * @return
     */      
    public static Python createInteractiveInstance(JConnection jCon, String workingDir) {
        
        Python interpreter = new Python();
        if (workingDir.trim().isEmpty()) {
            // if workingDir is empty, process start fails!
            workingDir = ".";
        }
        
        String pythonPath = getPythonPathFromWinIDEA(jCon);
        
        interpreter.startPythonInteractively(pythonPath, new File(workingDir));
        interpreter.waitForPrompt(10000, true); // wait until Python initializes

        return interpreter;
    }


    /**
     * Runs Python script in non-interactive mode, and waits until it exits. 
     * Input is specified with command line parameters, output is returned 
     * via stdout/stderr/return code. 
     * 
     * @param jCon connection to winIDEA needed to get location of Python 
     *             executable, which is configured there. 
     * @param workingDir
     * @param pythonArgs the first item is usually script name, other items are script args.
     */
    public int execScript(JConnection jCon,
                          IProgressMonitor monitor, 
                          String workingDir, 
                          String[] cmdLineArgs, 
                          long scriptTimeout, 
                          StringBuilder stdout,
                          StringBuilder stderr,
                          MutableBoolean isTimeout,
                          MutableBoolean isCanceled,
                          MutableObject<SException> exception) {
        
        exception.setValue(null);
        isTimeout.setFalse();
        isCanceled.setFalse();
        
        try {
            String pythonPath = getPythonPathFromWinIDEA(jCon);
            startPythonWStdio(pythonPath, new File(workingDir), false, cmdLineArgs);

            long startTime = System.currentTimeMillis();
            while (!waitForProcessEnd(1000, stdout, stderr)) {
                if (monitor != null  &&  monitor.isCanceled()) {
                    killProcess();
                    isCanceled.setTrue();
                    break;
                }

                if (scriptTimeout > 0  &&  
                        System.currentTimeMillis() - startTime > scriptTimeout) {
                    killProcess();
                    isTimeout.setTrue();
                    break;
                }
            }

        } catch (Exception ex) {
            exception.setValue(new SException("Diagram creation with Python script failed!", 
                                              ex));
            ex.printStackTrace();
        } finally {
            close();
        }
                    
        
        return getProcessReturnValue();
    }

    
    /**
     * This script starts Python process with the given cmd line args, which may
     * include script name and its arguments. The method returns immediately,
     * it does not wait for the script to end.
     *  
     * @param jCon
     * @param workingDir
     * @param pythonArgs
     */
    public Process execScriptAsync(JConnection jCon,
                                String workingDir,
                                boolean isStartInteractively,
                                String ... pythonArgs) {
        
        String pythonPath = getPythonPathFromWinIDEA(jCon);
        m_isInteractive = false;
        return startPythonProcess(pythonPath, 
                                  new File(workingDir), 
                                  true, // always open new window. <b>WARNING:</b> There 
                                  // is no reader attached to process by this method, so
                                  // the script producing too much data for stdout will 
                                  //block if this is set to 'false'.
                                  isStartInteractively, 
                                  pythonArgs);
    }
    
    
    /**
     * 
     * @param timeout timeout in milliseconds
     * @param stdout
     * @param stderr
     * @return
     */
    public boolean waitForProcessEnd(int timeout,
                                     StringBuilder stdout,
                                     StringBuilder stderr) {
            
        long endTime = System.currentTimeMillis() + timeout;
        while (!m_outReaderThread.isTerminated()  &&  System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(ReaderThread.WAIT_FOR_QUEUE_EMPTY_TIMEOUT / 2);
                // pump lines out of reader threads, since capacity of queue in 
                // ReaderThread is limited
                
                stdout.append(StringUtils.join(m_outReaderThread.getStdOut(), 
                                               "\n    ")); // indents all lines
                stderr.append(StringUtils.join(m_errReaderThread.getStdOut(), 
                                               "\n    ")); // indents all lines
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                throw new SIOException("Unexpected exception!", ex);
            }
        }
        
        // get any output written in the last moment
        stdout.append(StringUtils.join(m_outReaderThread.getStdOut(), 
                                       "\n    ")); // indents all lines
        stderr.append(StringUtils.join(m_errReaderThread.getStdOut(), 
                                       "\n    ")); // indents all lines
        
        if (m_outReaderThread.isTerminated()) {
            if (m_outReaderThread.isEndOfFile()) {
                close();
                return true;
            } else {
                Exception ex = m_outReaderThread.getException();
                if (ex == null) {
                    throw new SIOException("Process terminated unexpectedly!");
                } else {
                    throw new SIOException("Process terminated unexpectedly!", ex);
                }
            }
        }
        
        return false;
    }


    public int getProcessReturnValue() {
        
        if (m_process != null) {
            return m_process.exitValue();
        } 
        
        return -9999; // return NOT OK when process has not been even been started, m_process == NULL
    }

    
    private static String getPythonPathFromWinIDEA(JConnection jCon) {
        CIDEController ide = new CIDEController(jCon.getPrimaryCMgr());
        String pythonPath = "";
        String pythonSource = ide.getOptionStr("/IDE/Options.Environment.PythonSource");
        switch (pythonSource) {
        case "Internal":
            pythonPath = ide.getOptionStr("/IDE/Workspace.PythonPathExt");
            break;
        case "PATH":
            break;
        case "User":
            pythonPath = ide.getOptionStr("/IDE/Workspace.PythonPathExt");
            break;
        }
        
        return pythonPath.replace('\\', '/'); // path with '/' works on Win and Lx
    }
    
    
    /**
     * Starts Python interpreter in interactive mode.
     * 
     * @param pythonBinFolder folder where python.exe is located. If empty string,
     * then the location of python.exe should be specified in PATH env. var.
     *  
     * @param workingDirectory working directory for python. If it is a current
     * directory, specify it as <code>null</code>, or <code>new File(".")</code>.
     * Do not specify it as <code>new File("")</code>, because process start will fail.
     *   
     * @return instance of the new process.
     */
    public Process startPythonInteractively(String pythonBinFolder, 
                                            File workingDirectory) {
    
        return startPythonWStdio(pythonBinFolder, workingDirectory, true, "-i");
    }
    
    
    /**
     * Starts Python process and attaches readers to it. Call waitForProcessEnd()
     * and close() after calling this method. Console window is not opened.
     *  
     * @param pythonBinFolder
     * @param workingDirectory
     * @param cmdLineArgs
     * @param isInteractive if true, Python is started with option -i, so 
     *                      commands may be given via its stdin.
     * @return
     */
    private Process startPythonWStdio(String pythonBinFolder, 
                                      File workingDirectory,
                                      boolean isInteractive,
                                      String ... cmdLineArgs) {
        
        m_isInteractive = isInteractive;

        startPythonProcess(pythonBinFolder, workingDirectory, false, false, cmdLineArgs);
        
        m_stdIn = m_process.getInputStream();
        m_stdOut = m_process.getOutputStream();
        m_stdErr = m_process.getErrorStream();
        
        m_buffStdOut = new BufferedWriter(new OutputStreamWriter(m_stdOut));

        BufferedInputStream buffStdIn = new BufferedInputStream(m_stdIn);
        BufferedInputStream buffStdErr = new BufferedInputStream(m_stdErr);

        m_errReaderThread = new ReaderThread(buffStdErr, isInteractive);
        m_outReaderThread = new ReaderThread(buffStdIn, isInteractive);
        m_errReaderThread.start();
        m_outReaderThread.start();
        
        return m_process;
    }

    
    public void setInStreamListener(InStreamListener listener) {
        m_outReaderThread.setInStreamListener(listener);
        m_errReaderThread.setInStreamListener(listener);
    }

    
    /**
     * Starts a Python process with the given command line arguments. 
     * Caller may attach reader/writer to this process.
     * WARNING: This method does not attach reader for script's stdout - if the
     * script produces more output than can fit into system buffer, the script 
     * will block!
     *  
     * @param pythonBinFolder
     * @param workingDirectory
     * @param cmdLineArgs
     * @return
     */
    private Process startPythonProcess(String pythonBinFolder,
                                 File workingDirectory,
                                 boolean isStartInNewTerminalWindow,
                                 boolean isStartInteractively,
                                 String... cmdLineArgs) {

        if (workingDirectory != null  &&  workingDirectory.toString().isEmpty()) {
            throw new SIllegalArgumentException("Working directory must not be empty string, it should contain at leat '.'!");
        }
        
        String cmd = getPythonCmd(pythonBinFolder);

        List<String> items = new ArrayList<>(1 + cmdLineArgs.length);
        
        if (IS_DEBUG  ||  isStartInNewTerminalWindow ||  isStartInteractively) {
            // start python in new terminal window
            items.add("cmd.exe");
            items.add("/c");
            items.add("start");
        }
        
        items.add(cmd);
        
        if (IS_DEBUG  ||  isStartInteractively) { // start python interactively
            items.add("-i");
        }
        
        for (String arg : cmdLineArgs) {
            items.add(arg);
        }
        // Use string array instead of simple string, because single string gets
        // split at spaces by StreamTokenizer, so spaces in paths may cause problems.
        final String [] cmdLine = items.toArray(new String[0]);
        
        // we'll not set env vars. If env vars need to be added, use ProcessBuilder
        // to obtain existing env vars.
        try {
            if (workingDirectory == null) {
                m_process = Runtime.getRuntime().exec(cmdLine, null);
            } else {
                m_process = Runtime.getRuntime().exec(cmdLine, null, workingDirectory);
            }
        } catch (Exception ex) {
            throw new SException("Can not create process!", ex).
                add("cmdLine", StringUtils.join(cmdLine, " "));
        }
        
        return m_process;
    }


    private String getPythonCmd(String pythonBinFolder) {
        StringBuilder cmdLine = new StringBuilder();

        if (pythonBinFolder.length() > 0) {
            Path p = Paths.get(pythonBinFolder);
            if (OsUtils.isWindows()) {
                // From https://docs.microsoft.com/en-us/previous-versions/windows/it-pro/windows-server-2012-r2-and-2012/cc753427(v%3Dws.11)
                // The Windows operating system searches for a file by using 
                // default file name extensions in the following order of 
                // precedence: .exe, .com, .bat, and .cmd.
                Path exe = p.resolve(PYTHON_EXECUTABLE_EXE);
                if (Files.exists(exe)) {
                    cmdLine.append(exe);  
                } else {
                    Path bat = p.resolve(PYTHON_EXECUTABLE_BAT);
                    if (Files.exists(bat)) {
                        cmdLine.append(bat);
                    } else {
                        // keep the old default behavior
                        cmdLine.append(p.resolve(PYTHON_EXECUTABLE_NO_EXT));
                    }
                }
            } else {  // Linux, check for python.sh first, because it may configure env. for Python 
                Path sh= p.resolve(PYTHON_EXECUTABLE_SH);
                if (Files.exists(sh)) {
                    cmdLine.append(sh);  
                } else {
                    cmdLine.append(p.resolve(PYTHON_EXECUTABLE_NO_EXT));
                }
            }
        } else {
            cmdLine.append(PYTHON_EXECUTABLE_NO_EXT); // python should be on 
            // system path, exe on windows, no ext on Linux. 
        }
        return cmdLine.toString();
    }   
 
    
    /**
     * Sends exit() to Python interpreter, terminates reading threads, and 
     * releases resources by calling killProcess().
     * 
     * @throws IOException
     */
    public void close() {

        if (m_errReaderThread != null) {
            m_errReaderThread.terminate();
        }
        
        if (m_outReaderThread != null) {
            m_outReaderThread.terminate();
        }
        
        try {
            if (m_buffStdOut != null  &&  m_isInteractive) {
                m_buffStdOut.write("exit()\n");
                m_buffStdOut.flush();
            }
        } catch (IOException ex) {
            throw new SIOException("Can't send 'exit()' to interpreter!", ex);
        }
        try {
            Thread.sleep(500); // give Python some time to exit nicely
        } catch (InterruptedException ex) {
            // ignore exception, the process will be killed anyway
        } 
        
        killProcess();     // make sure the process dies and cleans up resources 
    }
    
    
    /** Kills process and releases resources. */ 
    public void killProcess() {
        if (m_process == null) {
            return;
        }
        m_process.destroy();
        // close streams also to avoid resource leaks
        try {
            m_stdIn.close();
            m_stdOut.close();
            m_stdErr.close();   
        } catch (IOException ex) {
            throw new SIOException("Can't close I/O streams when killing script interpreter!", ex);
        }
        // Some resources on the internet claim, that destroy() should be called
        // again after closing streams, to really release resources. Anyway,
        // it doesn't seem to do any harm.
        m_process.destroy();
    }
    

    /**
     * 
     * @param jCon
     * @param monitor
     * @param workingDir
     * @param statementTimeout must be greater then 0
     * @param isThrowException
     * @param statements
     * @return
     */
    public static List<ScriptResult> execStatements(JConnection jCon,
                                                    IProgressMonitor monitor, 
                                                    String workingDir,
                                                    long statementTimeout, // timeout for one statement 
                                                    boolean isThrowException,
                                                    MutableInt processRetVal,
                                                    String ... statements) {
        
        Python python = Python.createInteractiveInstance(jCon, workingDir);
        
        List<ScriptResult> retVals = new ArrayList<ScriptResult>(statements.length);
        
        for (String statement : statements) {
            
            if (monitor != null  &&  monitor.isCanceled()) {
                ScriptResult scriptResult = new ScriptResult();
                scriptResult.setCanceled(true);
                retVals.add(scriptResult);
                break;
            }
            
            retVals.add(python.execStatement(statement, statementTimeout, isThrowException));
        }

        python.close();
        processRetVal.setValue(python.getProcessReturnValue());
        
        return retVals;
    }
    
    
    /**
     * Executes the given Python statements. Python interpreter must first be
     * started with call to createInteractiveInstance().  
     * 
     * @param timeout
     * @param isThrowException
     * @param statements
     * @return
     */
    public List<ScriptResult> execStatements(int timeout, boolean isThrowException, String ... statements) {
        
        List<ScriptResult> retVals = new ArrayList<ScriptResult>(statements.length);
        
        for (String statement : statements) {
            retVals.add(execStatement(statement, timeout, isThrowException));
        }
        
        return retVals;
    }
    
    
    /**
     * Executes python statement given as parameter by writing is to Python stdin.
     * Waits for <code>timeout</code> milliseconds for prompt. 
     *  
     * @param statement python statement to execute
     * @param timeout how many milliseconds to wait for prompt, which means 
     *                that the statement execution has finished.
     * @param isThrowException if true, exception is thrown in case of a timeout 
     *                         or an error.
     * @return
     */
    private ScriptResult execStatement(String statement, long timeout, boolean isThrowException) {

        // System.out.println("statement = " + statement);

        List<String> stdout;
        List<String> stderr;
        try {
            m_outReaderThread.setEndOfInteractiveStdOutDetected(false);
            
            if (IS_DEBUG) {
                System.out.println("~~~" + statement);
            }
            m_buffStdOut.write(statement);
            m_buffStdOut.write('\n');
            m_buffStdOut.flush();
            
            stderr = waitForPrompt(timeout, true);

            if (IS_DEBUG) {
                System.out.println("~~~" + ReaderThread.STD_OUT_TERMINATON_STRING);
            }
            m_buffStdOut.write(ReaderThread.STD_OUT_TERMINATON_STRING);
            m_buffStdOut.write('\n');
            m_buffStdOut.flush();
            List<String> termStringErr = waitForPrompt(timeout, true);
            
            if (ScriptResult.isError(termStringErr)) {
                stderr.addAll(termStringErr);
                StringBuilder sb = new StringBuilder();
                for (String s : termStringErr) {
                    sb.append(s).append('\n');
                }
                throw new SIOException("Script execution error when waiting for end of stdout marker!")
                                       .add("statement", statement)
                                       .add("error", sb.toString()); 
            }

            // Wait until stdout is exhausted. If this approach will not work, 
            // each statement will have to be followed by print '@#@#@#' to
            // stdout, so that we'll detect end of text on std out. Now we
            // determine end of output on stderr, when Python prompt is printed,
            // but it is not synchronized with stdout and may be detected 
            // earlier than text on stdout.
            /* do {
                Thread.yield();
            } while (m_outReaderThread.isReading()); */
            long startTime = System.currentTimeMillis();
            while (!m_outReaderThread.isEndOfInteractiveStdOutDetected()) {
                if ((startTime + timeout) < System.currentTimeMillis()) {
                    throw new STimeoutException("Timeout expired before end of script execution! See also File | Properties | script!")
                    .add("statement", statement)
                    .add("timeout", timeout);
                }
            }
            
            stdout = m_outReaderThread.getStdOut();
        } catch (IOException ex) {
            stdout = m_outReaderThread.getStdOut();
            throw new SIOException("Error occurred when executing script statement!", ex)
                                  .add("statement", statement)
                                  .add("stdOut", stdout);
        }
        
        ScriptResult result = new ScriptResult(stdout, stderr);
        if (isThrowException) {
            if (result.isTimeout()) {
                throw new STimeoutException("Timeout when executing script!")
                .add("statement", statement)
                .add("timeout", timeout)
                .add("result", result);
            }
            if (result.isError()) {
                throw new SIOException("Error when executing script!")
                .add("statement", statement)
                .add("timeout", timeout)
                .add("result", result);
            }
        }
        
        return result;
    }
    
    
    /** 
     * This method is called to execute script functions as given in test specification.
     * 
     * @param functionName the name of Python function to call. Can also be method 
     *                     of an object, for example 'calc.add'
     *                     
     * @param params function parameters as they would appear in script. For example
     *               parameters for function with int and string param: new String[]{"23", "'name'"}
     *                
     */ 
    public ScriptResult callFunction(String functionName, String[] params, 
                                     int timeout, boolean isThrowException) {
        
        StringBuilder sb = new StringBuilder(RET_VAR_NAME);
        sb.append(" = ").append(functionName).append('(');
        String comma = "";
        
        if (params != null) {
            for (String param : params) {
                sb.append(comma).append(param);
                comma = ", ";
            }
        }
        
        sb.append(")");
        
        ScriptResult result = execStatement(sb.toString(), timeout, isThrowException);
        
        if (result.isError()) {
            return result;
        }
        
        ScriptResult retVal = execStatement(RET_VAR_NAME, timeout, isThrowException);
        if (retVal.isError()) {
            result.appendStdErr(retVal.getStderr());
            return result;
        }
        
        if (retVal.getStdout().size() > 0) {
            result.setFuncRetVal(retVal.getStdout());
        }
        
        return result;
    }


    /**
     * Waits for prompt written by Python interpreter to stderr, which means the
     * interpreter is ready for new input.
     * 
     * @param timeout time in millisecond to wait for prompt
     * @param isThrowTimeoutException if true, and timeout error is detected, an 
     *                         exception is thrown.
     *                         Otherwise caller has to verify returned value for errors.
     * @return list of strings printed to stderr. The last one is either null if
     * timeout occurred, ot prompt if there was not timeout. If there is more than
     * one line, something was printed to stderr ==> there was an error!
     */
    public List<String> waitForPrompt(long timeout, boolean isThrowTimeoutException) {
        List<String> stdErr = m_errReaderThread.waitForPrompt(timeout);
        
        if (isThrowTimeoutException  &&  ScriptResult.isTimeout(stdErr)) {
            if (timeout == 0) {
                throw new SIOException("Timeout expired when executing script! Set value greater than 0 in 'File | Properties | Script'.")
                .add("timeout", timeout)
                .add("errLines", stdErr);
            }
            
            throw new SIOException("Timeout expired when executing script! See setting in 'File | Properties | Script'.")
            .add("timeout", timeout)
            .add("errLines", stdErr);
        }

        return stdErr;
    }
}
