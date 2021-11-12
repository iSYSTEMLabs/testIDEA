package si.isystem.itest.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import argparser.ArgParser;
import argparser.StringHolder;
import si.isystem.commons.connect.ConnectionPool;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.connectJNI;
import si.isystem.connect.data.JVersion;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.data.ITestStatusLine;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "si.isystem.itest.plugin.core";

	// Consider moving this item to TestSpecificationModel, so each document
	// can have its own connection, if required.
	public static final ConnectionPool CP = new ConnectionPool(); 
	
	/** 1 second timeout between checking monitor status for user cancellation. */
	public static final int DEFAULT_WAIT_FOR_TARGET_DELAY = 1000;    

    public enum EFont {FontBold, FontItalic};
    private Map<EFont, Font> m_fonts;

	// The shared instance
    private String[] m_cmdLineArgs; // args as specified in command line, no parsing has been done
    StringHolder m_cmdLineOptWorkspaceName = new StringHolder();
    StringHolder m_cmdLineOptIYamlhFile = new StringHolder();

    
	private static Activator plugin;

    private static ITestStatusLine m_statusLine;

    private static Shell m_shell;

    public static boolean ms_isSWTBotTest;

    
    
    private Image m_testOkImg;
    private Image m_testErrorImg;

    private Properties m_osgiI18nProps;

    private Boolean m_isRCP = null;
    
    static private long m_nanoTimeAtStart;
	
	/**
	 * The constructor.
	 */
	public Activator() {
        ImageDescriptor descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin("si.isystem.itest",
                                                       "icons/test_ok_overlay.gif");

        if (descriptor != null) {
            m_testOkImg = descriptor.createImage();
        }
        
        
        descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin("si.isystem.itest",
                                                       "icons/test_err_overlay.gif");

        if (descriptor != null) {
            m_testErrorImg = descriptor.createImage();
        }
        
        m_nanoTimeAtStart = System.nanoTime();
}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
        SExceptionDialog.setPrintToStdOut(true);

		Bundle bundle = context.getBundle();
		URL propsURL = bundle.getEntry("OSGI-INF/l10n/bundle.properties");
		m_osgiI18nProps = new Properties();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(propsURL.openConnection().getInputStream()))) {
		    m_osgiI18nProps.load(reader);
		}
	}

    
    public static Properties getOSGI_I18nPropeties() {
        return Activator.getDefault().m_osgiI18nProps;
    }

    
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;

		if (m_fonts != null) {
		    for (Font font : m_fonts.values()) {
		        font.dispose();
		    }
		}
		
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	
	public boolean isRCP() {
	    if (m_isRCP == null) {
	        Bundle plugin = Platform.getBundle("si.isystem.itest.plugin.rcp");
	        if (plugin != null) {
	            //System.out.println("- bundle: " + plugin.getSymbolicName());
	            m_isRCP = new Boolean(true);
	        } else {
                m_isRCP = new Boolean(false);
	        }
	    }
	    
	    return m_isRCP.booleanValue();
	}
	
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	
	// convenience method, added by MK.
    public static Shell getShell() {
        if (m_shell == null) {
            m_shell = getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
        }
        return m_shell;
    }


    public Font getFont(Shell shell, EFont font) {
        // these fonts are disposed in stop() 
        if (m_fonts == null) {
            m_fonts = new TreeMap<EFont, Font>();
            m_fonts.put(EFont.FontBold, new Font(shell.getDisplay(), "Arial", 9, SWT.BOLD));
            m_fonts.put(EFont.FontItalic, new Font(shell.getDisplay(), "Arial", 9, SWT.ITALIC));
        }
        return m_fonts.get(font);
    }
    
    // replace this method with ControllerPool.getConnectionManager()
    /* public static JConnectionMgr getConnectionManager() {
        return ControllerPool.instance().getConnectionManager();
    } */

    
    public static ITestStatusLine getStatusLine() {
        if (m_statusLine == null) {
            m_statusLine = new ITestStatusLine();
        }
        
        return m_statusLine;
    }

    
    public static Command getCommand(String cmdId) {
    	ICommandService cmdService = 
    		(ICommandService)PlatformUI.getWorkbench().getActiveWorkbenchWindow().
    		getService(ICommandService.class);
    	
    	return cmdService.getCommand(cmdId);
        // ICommandService service = (ICommandService) getSite().getService(ICommandService.class);
    }
    
    
    public Image getTestOkOverlayImg() {
        return m_testOkImg;
    }
    
    
    public Image getTestErrorOverlayImg() {
        return m_testErrorImg;
    }

    
    public static void setTitle(String fileName) {
        
        if (getDefault().isRCP()) {  // do not set title for Eclipse, as it manages its title
            
            IProduct product = Platform.getProduct();
            if (product != null) { // null during testing with SWTBot
                String title = product.getName();

                if (fileName != null  &&  !fileName.isEmpty()) {
                    title += " - " + fileName;
                }
                getShell().setText(title);
            }
        }
    }
    
    /**
     * 
     * @param status should be one of Status.OK, .INFO, WARNING, .ERROR, .CANCEL constants. 
     * @param message log message
     * @param thr exception or null
     */
    public static void log(int status, String message, Throwable thr) {
        getDefault().getLog().log(new Status(status, 
                                             "isysLog", 
                                             Status.OK, 
                                             message, 
                                             thr));
    }
    
    
    public static void printMemoryStatus() {
        System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
        System.out.println("Total memory: " + Runtime.getRuntime().totalMemory());
        System.out.println("Max memory: " + Runtime.getRuntime().maxMemory());
    }
    
    
    public static String getTimeFromStart() {
        long time = System.nanoTime() - m_nanoTimeAtStart;
        DecimalFormat fmt = (DecimalFormat)NumberFormat.getNumberInstance();
        
        return fmt.format(time);
    }
    
    
    /**
     * Currently returns iconnect SDK version, which should be the same as 
     * testIDEA version in all regular builds. 
     * Alternatively we could use:
     *     Plugin plugin = Platform.getPlugin(Activator.PLUGIN_ID);
     *     System.out.println(plugin.getDescriptor().getVersionIdentifier().toString());
     *
     * @return
     */
    public static JVersion getVersion() {
        String ver = connectJNI.getModuleVersion();
        String [] items = ver.split("\\.");
        if (items.length < 3) {
            throw new SIllegalArgumentException("Invalid version of iconnect SKD - " +
                                                "expected 3 numbers separated by dots!")
                                               .add("version", ver);
        }
        
        int major = Integer.parseInt(items[0]);
        int minor = Integer.parseInt(items[1]);
        int build = Integer.parseInt(items[2]);
        
        return new JVersion(major, minor, build);
    }
    

    
    public static void setApplicationTitle() {
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        
        if (model != null) {
            setTitle(model.getModelFileName());
        } else {
            setTitle(null);
        }
    }
    

    public String[] getCmdLineArgs() {
        return m_cmdLineArgs;
    }


    /**
     * This method only sets raw cmd line args, NO parsing is performed. Also no
     * error is returned. Call method parseCmdLineArgs(), when application is
     * ready to report exceptions to user.
     * 
     * @param cmdLineArgs
     */
    public void setCmdLineArgs(String[] cmdLineArgs) {
        m_cmdLineArgs = cmdLineArgs;
    }
    
    /**
     * Parses cmdline args. Results are stored to attributes 
     * m_cmdLineOptWorkspaceName, ... 
     * 
     * @throws SIllegalArgumentException in case of error in cmd line args 
     */
    public void parseCmdLineArgs() {
        ArgParser parser = new ArgParser("iSystem_testIDEA <options> <iYAML fileName>");

        parser.addOption("-isys_ws %s #name of the winIDEA workspace", m_cmdLineOptWorkspaceName);
        parser.addOption("-isys_open %s #name of the iSYSTEM Test Bench test specification file", 
                         m_cmdLineOptIYamlhFile);

        String unmatched[] = parser.matchAllArgs(m_cmdLineArgs, 0, 0);
        
        if (unmatched != null) {
            if (unmatched.length > 1) {
                throw new SIllegalArgumentException("Error in command line arguments, more than one unmatched item found!")
                                    .add("error", parser.getErrorMessage())
                                    .add("invalidArg", unmatched[0])
                                    .add("", parser.getHelpMessage());
            } else {
                if (m_cmdLineOptIYamlhFile.value != null) {
                    throw new SIllegalArgumentException("File name and option -isys_open must not be specified at the same time!")
                        .add("", parser.getHelpMessage());
                }
                m_cmdLineOptIYamlhFile = new StringHolder(unmatched[0]);
            }
        }
    }


    public String getCmdLineWorkspaceName() {
        return m_cmdLineOptWorkspaceName.value;
    }


    public String getCmdLineOptIYamlFile() {
        return m_cmdLineOptIYamlhFile.value;
    }

    
    /**
     * Returns workspace path to be used to connect to winIDEA. If it is not 
     * specified in cmd line or iyaml config file, empty string is returned, which
     * means connect to the MRU instance of winIDEA.
     * 
     * The following precedence rules apply:  
     * 
     * is cmd line specified  |  is config specified | connect to
     * -----------------------------------------------------------
     * no                     |  no                  | '' - MRU
     * no                     |  yes                 | config ws
     * yes                    |  no                  | cmd line ws
     * yes                    |  yes                 | cmd line ws
     */
    public String getConnectionWinIDEAWorkspaceName(CTestEnvironmentConfig envConfigWinIDEAWsName) {
        
        if (getCmdLineWorkspaceName() != null) {
            return getCmdLineWorkspaceName();
        }
        
        if (envConfigWinIDEAWsName.getWorkspace() != null) {
            return envConfigWinIDEAWsName.getWorkspace();
        }
        
        return "";
    }
}
