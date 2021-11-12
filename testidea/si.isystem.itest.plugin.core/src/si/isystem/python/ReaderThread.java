package si.isystem.python;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalStateException;

public class ReaderThread extends Thread {

    private static final String MULTILINE_PROMPT = "... ";
    public static final String PROMPT = ">>> ";
    public static final String STD_OUT_TERMINATON_STRING = "'__isys__'";

    static final long WAIT_FOR_QUEUE_EMPTY_TIMEOUT = 500;

    private final BufferedInputStream m_reader;
    private volatile boolean m_isTerminated = false;
    // private volatile boolean m_isReading = false; replaced by 'm_endOfStdOutDetected' 
    private volatile boolean m_isEndOfInteractiveStdOutDetected;
    private volatile boolean m_isEndOfFile;
    private Exception m_exception = null;
    private boolean m_isInteractive; // true, if testIDEA waits for prompt and
                                     // fills stdin.
                                     // false, if testIDEA runs the script and 
                                     // waits for it to terminate - stdin is not
                                     // filled by testIDEA, only stdout, stderr,
                                     // and return code are observed
    
    private final BlockingQueue<String> m_lines;
    private InStreamListener m_inStreamListener;

    private static final boolean IS_DEBUG = false;
    
    public ReaderThread(BufferedInputStream reader, boolean isInteractive) {
        m_reader = reader;
        m_isInteractive = isInteractive;
        m_lines = new ArrayBlockingQueue<String>(10000);
        
        // if multiline Python statement is sent, then prompt may be something like:
        // ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... >>> 
        //m_promptRegEx = Pattern.compile("(\\.\\.\\. )*>>>");
    }

    
    public void setInStreamListener(InStreamListener inStreamListener) {
        m_inStreamListener = inStreamListener;
    }


    @Override
    public void run() {
    
        while (!m_isTerminated) {
            try {
                StringBuilder line = new StringBuilder();
                do {
                    /* if (m_reader.available() == 0) {
                        m_isReading = false;
                    } */
                    int retVal = m_reader.read(); 
                    // m_isReading = true; 
                    if (m_isTerminated) {
                        // m_isReading = false;
                        break;
                    }
                    if (retVal == -1) {
                        m_isEndOfFile = true;
                        throw new SIOException("End of file detected on error stream from Python interpreter!");
                    }
                    if (retVal == '\r') {
                        // skip CR - it has to be followed by '\n' anyway
                        continue;
                    }
                    if (retVal == '\n') {
                        // end of line, add it to the list
                        break;
                    }
                    line.append((char)retVal);
                    
                    // when prompt is written by Python, it is not followed by '\n',
                    // so we must handle it specially
                    if (m_isInteractive  &&  line.length() == PROMPT.length()) {
                        String lineAsStr = line.toString();
                        if (lineAsStr.equals(PROMPT)) {
                            break;
                        }                         
                        if (lineAsStr.equals(MULTILINE_PROMPT)) {
                            // ignore '... ' prompts
                            line.delete(0, line.length());
                        }                        
                    }
                } while (true);
                
                if (IS_DEBUG) {
                    System.out.println("RT: " + line);
                }
                String lineStr = line.toString();
                if (m_isInteractive  && lineStr.equals(STD_OUT_TERMINATON_STRING)) {
                    m_isEndOfInteractiveStdOutDetected = true;
                } else {
                    
                    if (m_inStreamListener != null  &&  !lineStr.equals(PROMPT)) {
                        m_inStreamListener.setLine(lineStr);
                    }
                    
                    if (m_lines.offer(lineStr, 
                                  WAIT_FOR_QUEUE_EMPTY_TIMEOUT, 
                                  TimeUnit.MILLISECONDS) == false) {
                        // this should never happen, as caller should pull lines
                        // at a faster rate than this timeout!
                        throw new SIOException("Buffer is full when reading script stdout/stderr! "
                                + "Try to decrease the number of lines or slow down writing!");
                    }
                }
            } catch (Exception ex) {
                m_exception = ex;
                m_isTerminated = true;
            }
        }
    }
    
        
    public Exception getException() {
        return m_exception;
    }


    public void terminate() {
        // m_reader.close();  // hangs if another thread is already in read 
                              // statement of this stream
        m_isTerminated = true;
    }
    
    
    /**
     * This method should be called for thread, which reads stderr in interactive
     * mode only.
     * @param timeout timeout in milliseconds
     * @return
     */
    public List<String> waitForPrompt(long timeout) {
        List<String> stdErr = new ArrayList<String>();
        while (true) {
            try {
                String line = m_lines.poll(timeout, TimeUnit.MILLISECONDS);
                stdErr.add(line); // the last item is '>>>' if OK, null if timeout occurred
                
                if (line == null  ||  line.equals(PROMPT)) {
                    return stdErr;
                }
                
                /* if (line.startsWith("... ")) {
                    // check for prompt in case of multiline command sent to Python interpreter
                    Matcher m = m_promptRegEx.matcher(line);
                    if (m.matches()) {
                        return stdErr;
                    }
                } */
            } catch (InterruptedException ex) {
                throw new SIllegalStateException("Python reader thread interupted!", ex);
            }
        }
    }


    /**
     * This method should be called for thread, which reads stdout. It should be 
     * called after waitForPrompt() from stdErr reader thread returns. Otherwise
     * std out contents will accumulate.
     *  
     * @return
     */
    public List<String> getStdOut() {
        List<String> stdOut = new ArrayList<String>();
        m_lines.drainTo(stdOut);
        return stdOut;
    }

    
    /**
     * This method is intended for interactive output retrieval, so that
     * the user does not have to wait until the statement (function call) 
     * completes.
     * 
     * @return
    public List<String> peekOutput() {
        List<String> output = new ArrayList<String>();
        m_lines.addAll(output);
        return output;
    }
     */
    
    /**
     * This method returns true, if characters are still available in the input 
     * stream. This way the caller can wait to obtain all input, which has been 
     * printed by Python interpreter, but may have not been read by the stdout 
     * ReaderThread, while stderr Reader thread may have already encountered the
     * prompt.  
     * @return
    public boolean isReading() {
        return m_isReading;
    }
     */
    
    public boolean isEndOfInteractiveStdOutDetected() {
        return m_isEndOfInteractiveStdOutDetected;
    }


    public void setEndOfInteractiveStdOutDetected(boolean endOfStdOutDetected) {
        m_isEndOfInteractiveStdOutDetected = endOfStdOutDetected;
    }


    public boolean isTerminated() {
        return m_isTerminated;
    }


    public boolean isEndOfFile() {
        return m_isEndOfFile;
    }
}
