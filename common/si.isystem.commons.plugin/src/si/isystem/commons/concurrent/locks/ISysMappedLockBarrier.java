package si.isystem.commons.concurrent.locks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A synchronization mechanism which can be used in a single thread 
 * to wait for multiple other threads to to stop processing (they call
 * unlock()).
 * 
 * Each object registers itself (creates its lock) and later de-registers
 * (unlocks its lock).
 * 
 * Locks are recognized using their hashes.
 * 
 * Can't be reused.
 * 
 * @author iztokv
 *
 */
public class ISysMappedLockBarrier<V> extends ISysLockBarrier implements IMultiLockBarrier 
{
    private Set<V> m_locks = new HashSet<>();
    private Set<V> m_unlocked = new HashSet<>();
    
    public ISysMappedLockBarrier() {
        this(null);
    }
    
    public ISysMappedLockBarrier(Collection<V> locks) {
        super();
        if (locks != null) {
            m_locks.addAll(locks);
        }

        log("with %s locks created", allLocksString());
    }
    
    public synchronized void addLock(V lockObject) throws LockException
    {
        // Each lock can be created only once
        if (m_locks.contains(lockObject)) {
            throw new LockException(String.format("Double lock: %s", lockString(lockObject)));
        }
        m_locks.add(lockObject);

        log("added lock %s", lockObject);
    }

    public synchronized void unlock(V lockObject) throws LockException 
    {
        // We can only unlock known locks
        if (!m_locks.contains(lockObject)) {
            throw new LockException(String.format("Unknown lock: %s. Known locks: %s", lockString(lockObject), allLocksString()));
        }
        // Each lock can be unlocked only once
        if (m_unlocked.contains(lockObject)) {
            throw new LockException(String.format("Double unlock: %s", lockString(lockObject)));
        }
        
        m_unlocked.add(lockObject);

        log("unlocked %s", lockObject);
        
        if (m_unlocked.size() >= m_locks.size()) {
            release();
        }
    }
    
    @Override
    public synchronized void reset() {
        super.reset();
        m_unlocked.clear();
    }
    
    @Override
    public int getLockCount() {
        return m_locks.size();
    }
    
    @Override
    public int getUnlockCount() {
        return m_unlocked.size();
    }
    
    public String lockString(V lockObject) {
        return String.format("%s#%d", lockObject.getClass().getSimpleName(), System.identityHashCode(lockObject));
    }

    private String allLocksString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (V lock : m_locks) {
            sb.append(lockString(lock)).append(", ");
        }
        sb.append(']');
        return sb.toString();
    }
}
