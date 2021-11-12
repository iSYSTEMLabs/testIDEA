package si.isystem.commons.winidea.splashscreen;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.ISysCommonsPlugin;
import si.isystem.commons.ISysTaskExecutor;
import si.isystem.commons.log.ISysLog;
import si.isystem.commons.utils.ISysResourceUtils;

public class ISystemSplashScreen {
    
    private static ISysLog s_log = ISysLog.instance();
    
    public static final String RESOURCES_ICONS_ISYSTEM_LOGO = "/resources/images/iSYSTEM_logo.png";
    public static final String RESOURCES_ICONS_WINIDEA_LOGO = "/resources/images/winIDEA_logo.png";

    private final IConSplashScreenMonitor m_monitor;

    private final Shell m_shell;
    private Image m_iSystemImage = null;
    private Image m_winIdeaImage = null;
    private ProgressBar m_progressBar;
    IWorkbenchWindow m_activeWorkbenchWindow;
    private Color m_colorWhite;
    private Label m_versionLabel;
    private Button m_cancelButton;

    private Thread m_progressBarThread;

    private ISystemSplashScreen(IConSplashScreenMonitor monitor)
    {
        m_monitor = monitor;
        m_shell = new Shell((Shell)null, SWT.APPLICATION_MODAL | SWT.TITLE);
        m_shell.setText(monitor.getDialogTitle());

        m_colorWhite = new Color(m_shell.getDisplay(), 255, 255, 255);

        this.createContents();
        this.prepareWorkerThread();
    }

    public static ISystemSplashScreen showAsync(final IConSplashScreenMonitor monitor)
    {
        final AtomicReference<ISystemSplashScreen> splashScreen = new AtomicReference<>(null);

        ISysTaskExecutor.syncExecSWT(new Runnable()
        {
            @Override
            public void run()
            {
                ISystemSplashScreen ss = new ISystemSplashScreen(monitor);
                ss.open();
                splashScreen.set(ss);
            }
        });
        
        return splashScreen.get();
    }

    private void createContents()
    {
        GridData d;

        //
        // Load the iSYSTEM background image
        //
        try
        {
            String iSystemLogoFileName = ISysResourceUtils.getResourceFromBundle(
                    RESOURCES_ICONS_ISYSTEM_LOGO, ISysCommonsPlugin.PLUGIN_ID);
            m_iSystemImage = new Image(m_shell.getDisplay(), iSystemLogoFileName);
            String winIdeaLogoFileName = ISysResourceUtils.getResourceFromBundle(
                    RESOURCES_ICONS_WINIDEA_LOGO, ISysCommonsPlugin.PLUGIN_ID);
            m_winIdeaImage = new Image(m_shell.getDisplay(), winIdeaLogoFileName);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        

        //
        // Set the splash screen position to the center of the Eclipse workspace or center of primary monitor
        //
        m_activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        Rectangle r = null;

        if (m_activeWorkbenchWindow != null)
        {
            // Make the splash-screen follow the center of the workbench window
            r = m_activeWorkbenchWindow.getShell().getBounds();
        }
        else
        {
            r = m_shell.getDisplay().getPrimaryMonitor().getBounds();
        }

        //
        // Setup GUI
        //

        // Fill shell with single composite
        m_shell.setLayout(new GridLayout(2, false));
        m_shell.setBackground(m_colorWhite);

        // iSYSTEM logo on the left side
        d = new GridData();
        d.grabExcessHorizontalSpace = true;
        d.grabExcessVerticalSpace = true;
        d.horizontalAlignment = SWT.CENTER;
        d.verticalAlignment = SWT.END;
        d.horizontalSpan = 1;

        Label logoISystem = new Label(m_shell, SWT.NONE);
        logoISystem.setImage(m_iSystemImage);
        logoISystem.setBackground(m_colorWhite);
        logoISystem.setLayoutData(d);

        // winIDEA logo on the right side
        d = new GridData();
        d.grabExcessHorizontalSpace = true;
        d.grabExcessVerticalSpace = true;
        d.horizontalAlignment = SWT.CENTER;
        d.verticalAlignment = SWT.END;
        d.horizontalSpan = 1;

        Label logoWinIDEA = new Label(m_shell, SWT.NONE);
        logoWinIDEA.setImage(m_winIdeaImage);
        logoWinIDEA.setBackground(m_colorWhite);
        logoWinIDEA.setLayoutData(d);

        // Comment about using the winIDEA OPEN - buy the real one!!!
        if (m_monitor.isWinIdeaOpen())
        {
            d = new GridData();
            d.grabExcessHorizontalSpace = true;
            d.horizontalAlignment = SWT.CENTER;
            d.horizontalSpan = 2;
            d.horizontalIndent = 2;

            Link link = new Link(m_shell, SWT.NONE);
            String message = "You are currently using winIDEA OPEN. For regular winIDEA please visit " + 
                    "<a href=\"http://www.isystem.com/products/software/winidea\">www.iSYSTEM.com</a>";
            link.setText(message);
            link.setBackground(m_colorWhite);
            link.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    try
                    {
                        // Open default external browser
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
                    }
                    catch (PartInitException ex)
                    {
                        ex.printStackTrace();
                    }
                    catch (MalformedURLException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            });
            link.setLayoutData(d);
        }

        // Progress bar
        d = new GridData();
        d.grabExcessHorizontalSpace = true;
        d.horizontalAlignment = SWT.FILL;
        d.horizontalSpan = 2;

        m_progressBar = new ProgressBar(m_shell, SWT.SMOOTH);
        m_progressBar.setLayoutData(d);

        // progress label
        d = new GridData();
        d.grabExcessHorizontalSpace = true;
        d.horizontalAlignment = SWT.FILL;
        d.horizontalSpan = 1;
        d.horizontalIndent = 2;
        
        m_versionLabel = new Label(m_shell, SWT.SHADOW_OUT);

        String version = "winIDEA version: " + m_monitor.getWinIdeaVersion();
        version += m_monitor.isWinIdeaOpen() ? " Open" : "";
        ISystemSplashScreen.this.m_versionLabel.setText(version);

        m_versionLabel.setBackground(m_colorWhite);
        m_versionLabel.setLayoutData(d);

        // Cancel button
        if (m_monitor.isCancelable()) {
            d = new GridData();
            d.grabExcessHorizontalSpace = true;
            d.horizontalAlignment = SWT.RIGHT;
            d.horizontalSpan = 1;
            d.horizontalIndent = 2;

            m_cancelButton = new Button(m_shell, SWT.NONE);
            m_cancelButton.setText("Cancel");
            m_cancelButton.setLayoutData(d);
            m_cancelButton.addSelectionListener(new SelectionListener() {
                public void handle() {
                    m_monitor.cancel();
                }

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    handle();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    handle();
                }
            });
        }
        
        updateBoundsToCenter(r);
    }

    public void open()
    {
        m_shell.open();
        startProgressBar();
    }

    
    private void prepareWorkerThread()
    {
        m_progressBarThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // Advance the progress bar randomly until session is initialized or
                // initialization is cancelled.
                while (!m_monitor.isFinished())
                {
                    final MutableInt i = new MutableInt(0);
                    if (!m_shell.isDisposed() && !m_shell.getDisplay().isDisposed())
                    {
                        ISysTaskExecutor.syncExecSWT(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!m_progressBar.isDisposed())
                                {
                                    i.increment();
                                    m_progressBar.setSelection((int)m_monitor.getPercent());
                                    m_versionLabel.setText(m_monitor.getProgressCaption());
                                }
                            }
                        });
                    }
                    else
                    {
                        // If display was disposed for some reason then stop updating it
                        return;
                    }

                    try
                    {
                        Thread.sleep(m_monitor.getSleepTimeMs());
                    }
                    catch (InterruptedException ie)
                    {
                        ie.printStackTrace();
                    }
                }

                if (!m_shell.getDisplay().isDisposed())
                {
                    // Close the shell
                    ISysTaskExecutor.syncExecSWT(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!m_progressBar.isDisposed() && !m_shell.isDisposed())
                            {
                                ISystemSplashScreen.this.m_shell.close();
                                ISystemSplashScreen.this.m_colorWhite.dispose();
                            }
                        }
                    });
                }
            }
        }, "iSYSTEM download splash screen progress bar thread.");
    }

    private void startProgressBar()
    {
        m_progressBar.setMinimum(0);
        m_progressBar.setMaximum(100);
        m_progressBar.setSelection(0);

        m_progressBarThread.start();
    }

    private void updateBoundsToCenter(Rectangle r)
    {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;

        Point size = this.m_shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int w = size.x + 15;
        int h = size.y + 15;

        m_shell.setBounds(cx - w / 2, cy - h / 2, w, h);
    }
    
    public void waitUntilClosed()
    {
        try
        {
            m_progressBarThread.join();
        }
        catch (InterruptedException e)
        {
            s_log.w("Failed to wait for splash screen to close");
        }
        finally
        {
            s_log.w("%s dialog closed.", m_monitor.getDialogTitle());
        }
    }
}
