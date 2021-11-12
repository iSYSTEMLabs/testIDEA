package si.isystem.commons.winidea.splashscreen;

public interface IConSplashScreenUpdater extends Runnable
{
    public void addProgress(long progress);
    public void setProgress(long progress);
    public void setFinished();
}
