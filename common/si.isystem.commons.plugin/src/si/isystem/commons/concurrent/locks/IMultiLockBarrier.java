package si.isystem.commons.concurrent.locks;

public interface IMultiLockBarrier extends ILockedBarrier {
    int getLockCount();
    int getUnlockCount();
}
