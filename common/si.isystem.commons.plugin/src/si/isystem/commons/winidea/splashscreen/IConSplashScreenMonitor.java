package si.isystem.commons.winidea.splashscreen;

import si.isystem.connect.data.JVersion;

public interface IConSplashScreenMonitor
{
    public long getFullWork();
    public long getProgress();
    public long getPercent();
    public long getSleepTimeMs();
    public boolean isFinished();
    public String getProgressCaption();
    public String getDialogTitle();
    public JVersion getWinIdeaVersion();
    public boolean isWinIdeaOpen();
    public boolean isCancelable();
    public void cancel();
    public boolean wasCancelled();
}
