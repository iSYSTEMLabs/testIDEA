package si.isystem.commons.concurrent.locks;

public interface ILockedBarrier {
    /**
     * Removes the barrier (the wait() call returns without waiting for anything).
     */
    public void release();
    
    /**
     * Is the barrier already released?
     * @return
     */
    public boolean isReleased();
    
    /**
     * Re-enstates the barrier to block execution of wait() methods.
     */
    public void reset();
    
    /**
     * Waits for the barrier to be released for as long as it takes
     */
    public void waitForPass();
    
    /**
     * Waits fo the barrier to be released for the specified maximum time.
     * @param timeoutMillis
     */
    public void waitForPass(long timeoutMillis);
    
    /**
     * Waits fo the barrier to be released for the specified maximum time.
     * @param timeoutMillis
     * @param timeoutNanos
     */
    public void waitForPass(long timeoutMillis, int timeoutNanos);
    
    /**
     * How many threads are currently waiting for the lock release.
     * @return
     */
    public int getBlockedThreadsCount();
}
