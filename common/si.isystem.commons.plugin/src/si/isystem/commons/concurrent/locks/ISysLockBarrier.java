package si.isystem.commons.concurrent.locks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import si.isystem.commons.log.ISysLog;

public class ISysLockBarrier implements ILockedBarrier 
{
    private static final ISysLog s_log = ISysLog.instance();
    private final String m_logSignature;
    
    private final Object m_mutex = new Object();
    private final AtomicBoolean m_isUnlocked = new AtomicBoolean(false);
    
    /**
     * How many threads are currently waiting.
     */
    private final AtomicInteger m_threadWaitCount = new AtomicInteger(0);
    
    public ISysLockBarrier() {
        log("created");
        m_logSignature = String.format("%s[%d]", 
                getClass().getSimpleName(), 
                System.identityHashCode(this));
    }
    
    @Override
    public final void release() {
        log("releasing"); 
        m_isUnlocked.set(true);
        synchronized (m_mutex) {
            m_mutex.notifyAll();
        }
        log("released");
    }
    
    @Override
    public boolean isReleased() {
        return m_isUnlocked.get();
    }
    
    @Override
    public final void waitForPass() {
        waitForPass(0);
    }
    
    @Override
    public final void waitForPass(long timeoutMillis) {
        waitForPass(timeoutMillis, 0);
    }
    
    @Override
    public final void waitForPass(long timeoutMillis, int timeoutNanos) 
    {
        if (m_isUnlocked.get()) {
            log("waiting skipped - already released");
            return;
        }
        
        try {
            int threadCount = m_threadWaitCount.incrementAndGet();
            synchronized (m_mutex) {
                log("waiting for pass (along with %d other threads)", threadCount);
                if (timeoutMillis > 0  ||  timeoutNanos > 0) {
                    m_mutex.wait(timeoutMillis, timeoutNanos);
                }
                else {
                    m_mutex.wait();
                }
            }
        }
        catch (Exception e) {
            s_log.e(e);
        }       
        finally {
            int threadCount = m_threadWaitCount.decrementAndGet();
            log("waiting for pass finished (%d threads still waiting)", threadCount);
        }
    }
    
    @Override
    public int getBlockedThreadsCount() {
        return m_threadWaitCount.get();
    }

    @Override
    public synchronized void reset() {
        m_isUnlocked.set(false);
    }
    
    void log(String format, Object... params) {
        s_log.cConcurr("%s %s @ %,d,%d", 
                m_logSignature,
                String.format(format, params),
                System.currentTimeMillis(),
                System.nanoTime() % 1_000_000);
    }
}
