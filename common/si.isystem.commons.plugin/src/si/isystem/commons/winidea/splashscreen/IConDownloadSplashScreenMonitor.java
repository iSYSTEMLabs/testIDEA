package si.isystem.commons.winidea.splashscreen;

import si.isystem.connect.data.JVersion;

public class IConDownloadSplashScreenMonitor extends IConAbstractSplashScreenMonitor
{
    public IConDownloadSplashScreenMonitor(JVersion version, boolean isWinIdeaOpen, long work)
    {
        super(version, isWinIdeaOpen, work, 200);
    }

    public IConDownloadSplashScreenMonitor(JVersion version, boolean isWinIdeaOpen, long work, long sleepTimeMs)
    {
        super(version, isWinIdeaOpen, work, sleepTimeMs);
    }

    @Override
    public void run()
    {
        // Advance the progress bar randomly until session is initialized or
        // initialization is cancelled.
        try
        {
            while (!this.isFinished())
            {
                // 1% of work left every 200ms
                float newWork = (getFullWork() - getProgress()) * 0.01f;
                addProgress((long) newWork);

                Thread.sleep(getSleepTimeMs() / 2);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public String getProgressCaption()
    {
        return "Download to target in progress...";
    }

    @Override
    public String getDialogTitle()
    {
        return String.format("Download to target");
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public void cancel() {
    }
    
    @Override
    public boolean wasCancelled() {
        return false;
    }
}
