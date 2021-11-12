package si.isystem.commons.concurrent.locks;

/**
 * A synchronization mechanism which can be used in a single thread 
 * to wait for multiple other threads to to stop processing (they call
 * unlock()).
 * 
 * Can't be reused.
 * 
 * @author iztokv
 *
 */
public class ISysMultiLockBarrier extends ISysLockBarrier implements IMultiLockBarrier 
{
    private int m_lockCount;
    private int m_unlockedCount;
    
    public ISysMultiLockBarrier(int lockCount) {
        m_lockCount = lockCount;
        m_unlockedCount = 0;
        log("created with %d locks", m_lockCount);
    }
    
    public synchronized void addLock() {
        addLocks(1);
    }
    
    public synchronized void addLocks(int count) {
        m_lockCount += count;
        log("added %d locks, now has %d locks (%d unlocked)", count, m_lockCount, m_unlockedCount);
    }

    public synchronized void unlock() {
        unlock(1);
    }
    
    public synchronized void unlock(int unlockCount) {
        m_unlockedCount += unlockCount;
        log("single unlock (all together %d unlocked)", m_unlockedCount);
        if (m_unlockedCount >= m_lockCount) {
            release();
        }
    }
    
    @Override
    public synchronized int getLockCount() {
        return m_lockCount;
    }
    
    @Override
    public synchronized int getUnlockCount() {
        return m_unlockedCount;
    }
    
    @Override
    public synchronized void reset() {
        super.reset();
        m_unlockedCount = 0;
    }
}
