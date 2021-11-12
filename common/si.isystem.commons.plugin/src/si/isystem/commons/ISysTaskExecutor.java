package si.isystem.commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Display;

import si.isystem.commons.log.ISysCustomLogLevel;
import si.isystem.commons.log.ISysLog;
import si.isystem.commons.tuple.IPair;
import si.isystem.commons.tuple.Pair;
import si.isystem.commons.utils.ISysLogUtil;
import si.isystem.exceptions.SException;

/**
 * @author Iztokv
 *
 * Primary goal is to provide tracking of executable entities. With the logging
 * enabled we get warnings for unexpected long executions. This help us with 
 * blocking calls that never return.
 * 
 */
public class ISysTaskExecutor {
    private static ISysLog s_log = ISysLog.instance();
    
    private static final ExecutorService s_worker = Executors.newSingleThreadExecutor();
    private static final ExecutorService s_poolWorker = Executors.newCachedThreadPool();

    //
    // Logging
    //
    
    private static final long OLD_IN_MS = 500L;
    private static final long OLD_EXECUTABLE_PRINT_INTERVAL = 2000L;
    
    private static AtomicBoolean s_isLoggingInitialized = new AtomicBoolean(false);
    private static AtomicLong s_nextCallIndex = new AtomicLong(0);
    private static final Map<Long, RunnableExecution> s_calls = new HashMap<>();
    
    private static class RunnableExecution {
        private final long m_idx;
        private final String m_stackTrace;
        private final long m_startTime;
        
        RunnableExecution(long idx, String stackTrace, long startTime) {
            m_idx = idx;
            m_stackTrace = stackTrace;
            m_startTime = startTime;
        }

        @SuppressWarnings("unused")
        public long getIdx() {
            return m_idx;
        }

        public String getStackTrace() {
            return m_stackTrace;
        }

        @SuppressWarnings("unused")
        public long getStartTime() {
            return m_startTime;
        }
        
        public boolean isOld(long timestampNow) {
            final long age = timestampNow - m_startTime;
            return age > OLD_IN_MS;
        }
        
        @Override
        public String toString() {
            return String.format("[%d] (%dms)", m_idx, System.currentTimeMillis() - m_startTime);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void printRunnableState() {
        final Map.Entry<Long, RunnableExecution>[] es;
        synchronized (s_calls) {
            final Set<Entry<Long, RunnableExecution>> calls = s_calls.entrySet();
            es = calls.toArray(new Map.Entry[calls.size()]);
        }
        
        long timestampNow = System.currentTimeMillis();
        int oldCount = 0;
        for (Map.Entry<Long, RunnableExecution> e : es) {
            if (e.getValue().isOld(timestampNow)) {
                oldCount++;
            }
        }
        
        if (oldCount > 0) {
            s_log.cWork("\n\nOld Runnables [%d/%d]:\n\n", oldCount, es.length);
            for (Map.Entry<Long, RunnableExecution> e : es) {
                final RunnableExecution runExec = e.getValue();
                if (runExec.isOld(timestampNow)) {
                    s_log.cWork(runExec.getStackTrace(), "%s", runExec);
                }
            }
        }
    }
    
    private static void initializeLogging() {
        if (s_isLoggingInitialized.get()) {
            return;
        }
        // Print all runnables not yet finished
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    printRunnableState();
                    sleep(OLD_EXECUTABLE_PRINT_INTERVAL);
                }
            }
        }, String.format("iSYSTEM %s stale runnable logger", ISysTaskExecutor.class.getSimpleName())).start();
        s_isLoggingInitialized.set(true);
    }

    private static String produceStackTrace() {
        return ISysLogUtil.getStackTrace("si.isystem.", "si.isystem.commons.ISysTaskExecutor.");
    }
    
    private static void startLog(long idx, String stackTrace) {
        if (!s_isLoggingInitialized.get()) {
            initializeLogging();
        }
        RunnableExecution re = new RunnableExecution(idx, stackTrace, System.currentTimeMillis());
        synchronized (s_calls) {
            s_calls.put(idx, re);
        }
    }
    
    private static void endLog(long idx) {
        if (!s_isLoggingInitialized.get()) {
            return;
        }
        synchronized (s_calls) {
            s_calls.remove(idx);
        }
    }
    
    //
    // Execution
    //
    
    private static void runWithLogging(
            final Runnable runnable, 
            final long idx, 
            final String stackTrace)
    {
        startLog(idx, stackTrace);
        try {
            runnable.run();
        }
        catch (Throwable t) {
            s_log.e(t);
            throw new SException(t);
        }
        finally {
            endLog(idx);
        }
    }
    
    public static void sleep(long timeMs) {
        try {
            Thread.sleep(timeMs);
        }
        catch (InterruptedException e) {
            s_log.e(e, "Thread sleep interrupted.");
        }
    }
    
    /**
     * Calling asynchronous execution usually means that source of calling is lost. This can be
     * avoided by using this wrapper method. Unfortunately this creates a certain overhead as
     * the before-the-call exception must always be created even if it's never used.
     */
    private static boolean ENABLE_ASYNCH_EXCEPTION_FORWARDING = false;
    
    public static Runnable wrapWithExceptionHandling(Runnable runnable) {
        if (!ENABLE_ASYNCH_EXCEPTION_FORWARDING) {
            return runnable;
        }
        
        final SException e = new SException("Suppressed exception via asynch. exec.");
        Runnable wrappedRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                }
                catch (Throwable t) {
                    e.addSuppressed(t);
                    throw e;
                }
            }
        };
        return wrappedRunnable;
    }
    
    //
    //
    // Executable methods
    //
    //
    
    /**
     * Wrapper for Display.getDefault().syncExec(Runnable r).
     * 
     * @param runnable
     */
    public static void syncExecSWT(final Runnable runnable) {
        if (!s_log.isEnabled(ISysCustomLogLevel.Workers)) {
            Display.getDefault().syncExec(runnable);
        }
        else {
            final long idx = s_nextCallIndex.incrementAndGet();
            final String stackTrace = produceStackTrace();
            Display.getDefault().syncExec(wrapWithExceptionHandling(new Runnable() {
                @Override
                public void run() {
                    runWithLogging(runnable, idx, stackTrace);
                }
            }));
        }
    }
    
    /**
     * Wrpper for Display.getDefault().asyncExec(Runnable r).
     * @param runnable
     */
    public static void asyncExecSWT(final Runnable runnable) {
        if (!s_log.isEnabled(ISysCustomLogLevel.Workers)) {
            Display.getDefault().asyncExec(wrapWithExceptionHandling(runnable));
        }
        else {
            final long idx = s_nextCallIndex.incrementAndGet();
            final String stackTrace = produceStackTrace();
            Display.getDefault().asyncExec(wrapWithExceptionHandling(new Runnable() {
                @Override
                public void run() {
                    runWithLogging(runnable, idx, stackTrace);
                }
            }));
        }
    }
    
    /**
     * Wrapper for Display.getDefault().asyncExec(Runnable r).
     * @param runnable
     */
    public static void delayExecSWT(final int millisDelay, final Runnable runnable) {
        if (!s_log.isEnabled(ISysCustomLogLevel.Workers)) {
            Display.getDefault().timerExec(millisDelay, wrapWithExceptionHandling(runnable));
        }
        else {
            final long idx = s_nextCallIndex.incrementAndGet();
            final String stackTrace = produceStackTrace();
            Display.getDefault().timerExec(millisDelay, wrapWithExceptionHandling(new Runnable() {
                @Override
                public void run() {
                    runWithLogging(runnable, idx, stackTrace);
                }
            }));
        }
    }
    
    
    /**
     * Runnable is executed on a predefine thread so we can have
     * thread safety within a closed circle of entities.
     * 
     * @param runnable
     */
    public static void asyncExecWorker(final Runnable runnable) {
        asyncExec(s_worker, runnable);
    }
    
    /**
     * Used for short frequent runnables with the required thread safety
     * being a part of their implementation..
     * 
     * @param runnable
     */
    public static void asyncExecPool(final Runnable runnable) {
        asyncExec(s_poolWorker, runnable);
    }
    
    public static void asyncExec(ExecutorService executor, final Runnable runnable) {
        if (!s_log.isEnabled(ISysCustomLogLevel.Workers)) {
            executor.execute(wrapWithExceptionHandling(runnable));
        }
        else {
            final long idx = s_nextCallIndex.incrementAndGet();
            String stackTrace = produceStackTrace();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runWithLogging(wrapWithExceptionHandling(runnable), idx, stackTrace);
                }
            });
        }
    }
    
    /**
     * Just runs the runnable with the logging added.
     * @param runnable
     */
    public static void syncExec(final Runnable runnable) {
        try {
            if (!s_log.isEnabled(ISysCustomLogLevel.Workers)) {
                runnable.run();
            }
            else {
                final long idx = s_nextCallIndex.incrementAndGet();
                final String stackTrace = produceStackTrace();
                runWithLogging(runnable, idx, stackTrace);
            }
        }
        catch (Throwable t) {
            throw new SException(t);
        }
    }
    
    //
    //
    // Call source statistics
    //
    //
    
    private static final Map<String, Map<String, Integer>> s_traceMap = new HashMap<>();
    private static final List<IPair<String, String>> s_traceList = new ArrayList<>();

    public static void clearCallMark(String signature) {
        s_traceMap.remove(signature);
    }
    
    public static void markCall(String signature) {
        final String stackTrace = ISysLogUtil.getStackTrace("si.isystem", "si.isystem.commons.ISysTaskExecutor");
        Map<String, Integer> calls = s_traceMap.get(signature);
        if (calls == null) {
            calls = new HashMap<>();
            s_traceMap.put(signature, calls);
        }
        final Integer count = calls.get(stackTrace);
        if (count == null) {
            calls.put(stackTrace, new Integer(1));
        } else {
            calls.put(stackTrace, new Integer(count.intValue() + 1));
        }
    }

    public static String getCallStatistics() {
        return getCallStatistics(1);
    }
    
    public static String getCallStatistics(int minOccurence) {
        int titleWidth = 80;
        String borderStr = String.format("%s\n", StringUtils.repeat("#", titleWidth));

        StringBuilder sb = new StringBuilder();
        
        sb.append(borderStr);
        sb.append(borderStr);
        
        for (Entry<String, Map<String, Integer>> entry : s_traceMap.entrySet()) {
            String title = entry.getKey();
            Map<String, Integer> calls = entry.getValue();
            
            int sigLen = title.length();
            int space = titleWidth - sigLen - 2;
            int left = space / 2;
            int right = space - left;
            
            sb.append("\n");
            sb.append(String.format("%s %s %s\n", StringUtils.repeat("#", left), title, StringUtils.repeat("#", right)));
            
            for (Entry<String, Integer> e : calls.entrySet()) {
                final Integer count = e.getValue();
                if (count.intValue() >= minOccurence) {
                    final String stackTrace = e.getKey();
                    sb.append("\nCOUNT: ").append(count.intValue()).append("\n");
                    sb.append(stackTrace).append("\n");
                }
            }
        }
        
        sb.append(borderStr);
        sb.append(borderStr);
        return sb.toString();
    }

    public static void markOrderedCall(String comment) {
        final String stackTrace = ISysLogUtil.getStackTrace("si.isystem", "si.isystem.commons.ISysTaskExecutor");
        s_traceList.add(new Pair<>(comment, stackTrace));
    }

    public static String getOrderedCallStatistics() {
        int titleWidth = 80;
        String borderStr = String.format("%s\n", StringUtils.repeat("#", titleWidth));

        StringBuilder sb = new StringBuilder();
        
        sb.append(borderStr);
        sb.append(borderStr);
        
        for (IPair<String, String> entry : s_traceList) {
            String comment = entry.getLeft();
            String trace = entry.getRight();
            sb.append(comment).append('\n').append(trace).append('\n');
        }
        
        sb.append(borderStr);
        sb.append(borderStr);
        return sb.toString();
    }

    public static void clearAllMarks() {
        s_traceList.clear();
    }
}
