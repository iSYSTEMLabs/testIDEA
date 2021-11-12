package si.isystem.commons.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;

import si.isystem.commons.log.ISysLog;

public class ISysUiJobUtils 
{
    private static final ISysLog s_log = ISysLog.instance();
    private static final int TOTAL_WORK = 1000;

    /**
     * Opens dialog with fake progress for given runnable.
     * NOTE: runnable is executed inside a regular worker thread.
     * 
     * @param shell
     * @param title
     * @param runnable
     * @param maxTime
     * @param refreshMillis
     */
    public static void runDialogWithFakeProgress(
            final Shell shell,
            final String title, 
            final Runnable runnable, 
            final long maxTime, 
            final int refreshMillis) {
        
        ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell);
        progressDialog.setCancelable(false);
        
        try {
            progressDialog.run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor) 
                        throws InvocationTargetException, InterruptedException {
                    doWorkWithFakeProgress(monitor, title, runnable, maxTime, refreshMillis);
                }
            });
        }
        catch (InvocationTargetException | InterruptedException e) {
            s_log.e(e);
        }
    }
    
    /**
     * Periodically increases work done for the given runnable 
     * NOTE: runnable is executed inside a regular worker thread.
     * 
     * @param shell
     * @param title
     * @param runnable
     * @param maxTime
     * @param refreshMillis
     */
    public static void doWorkWithFakeProgress(
            final IProgressMonitor monitor,
            final String title, 
            final Runnable runnable, 
            final long maxTime,
            final int refreshMillis) {
        long m_startTime = System.currentTimeMillis();
        int m_lastWorkDone = 0;
        
        monitor.beginTask(title, TOTAL_WORK);
        final AtomicBoolean isRunning = new AtomicBoolean(true);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                
                // Shutdown updater thread
                isRunning.set(false);
                synchronized (isRunning) {
                    isRunning.notify();
                }
            }
        }, "iSYSTEM Work with fake progress.");
        thread.start();
        
        while (isRunning.get()) {
            long t = System.currentTimeMillis();
            final int totalWorkDone = (int)((t-m_startTime)*TOTAL_WORK/maxTime);
            final int addedWork = totalWorkDone - m_lastWorkDone;
            monitor.worked(addedWork);
            m_lastWorkDone += addedWork;
            try {
                synchronized (isRunning) {
                    isRunning.wait(refreshMillis);
                }
            }
            catch (Throwable e) {
                e.printStackTrace(System.out);
            }
        }
        monitor.done();
    }

    /**
     * Starts an UIJob that executes the given runnable and adds
     * fake progress indication to the jobs progress bar. 
     * NOTE: runnable is executed inside a regular worker thread.
     * 
     * @param shell
     * @param title
     * @param runnable
     * @param maxTime
     * @param refreshMillis
     */
    public static UIJob runUiJobWithFakeProgress(
            final String title, 
            final Runnable runnable, 
            final long maxTime, 
            final int refreshMillis) 
    {
        UIJob job = new UIJob(Display.getCurrent(), title) {
            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
                doWorkWithFakeProgress(monitor, title, runnable, maxTime, refreshMillis);
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        return job;
    }
}
