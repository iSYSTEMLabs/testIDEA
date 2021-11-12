package si.isystem.commons.winidea.splashscreen;

import si.isystem.connect.data.JVersion;


abstract public class IConAbstractSplashScreenMonitor implements IConSplashScreenMonitor, IConSplashScreenUpdater
{
    private long m_progress;
    final private long m_fullWork;
    final private long m_sleepTimeMs;
    final private JVersion m_winIdeaVersion;
    final private boolean m_isWinIdeaOpen;
    
    
    public IConAbstractSplashScreenMonitor(JVersion version, boolean isWinIdeaOpen, long work)
    {
        this(version, isWinIdeaOpen, work, 100);
    }

    public IConAbstractSplashScreenMonitor(JVersion version, boolean isWinIdeaOpen, long work, long sleepTimeMs)
    {
        m_fullWork = work;
        m_progress = 0;
        m_sleepTimeMs = sleepTimeMs;
        m_winIdeaVersion = version;
        m_isWinIdeaOpen = isWinIdeaOpen;
    }

    @Override
    synchronized public long getFullWork()
    {
        return m_fullWork;
    }
    
    @Override
    synchronized public void addProgress(long progress)
    {
        m_progress += progress;
    }

    @Override
    synchronized public void setProgress(long progress)
    {
        m_progress = progress;
    }

    @Override
    synchronized public long getProgress()
    {
        return m_progress;
    }

    @Override
    synchronized public long getPercent()
    {
        if (m_fullWork == 0)
            return 0;
        
        long percent = (100 * m_progress) / m_fullWork;
        if (percent < 1  &&  m_progress > 0)
            percent = 1;
        if (percent > 99  &&  m_progress < m_fullWork)
            percent = 99;
        
        return percent;
    }

    @Override
    synchronized public boolean isFinished()
    {
        return m_progress >= m_fullWork;
    }

    @Override
    public long getSleepTimeMs()
    {
        return m_sleepTimeMs;
    }

    @Override
    public void setFinished()
    {
        setProgress(getFullWork());
    }

    @Override
    public JVersion getWinIdeaVersion()
    {
        return m_winIdeaVersion;
    }

    @Override
    public boolean isWinIdeaOpen()
    {
        return m_isWinIdeaOpen;
    }
}
