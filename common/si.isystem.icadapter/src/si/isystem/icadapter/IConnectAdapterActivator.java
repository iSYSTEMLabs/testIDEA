package si.isystem.icadapter;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import si.isystem.connect.CDebugFacade;
import si.isystem.connect.utils.OsUtils;

public class IConnectAdapterActivator extends Plugin {
    
    public static String PLUGIN_ID = "si.isystem.icadapter";
    
    private static IConnectAdapterActivator instance = null;
    
    public IConnectAdapterActivator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        loadLibraries();
    }
    
    private void loadLibraries() throws URISyntaxException {
        loadLibrary();     
    }

    
    private void loadLibrary() throws URISyntaxException {
        
        String architecture = System.getProperty("sun.arch.data.model");
        
        if (OsUtils.isWindows()) {
            loadWindowsDll(architecture);
        } else {
            loadLinuxSo(architecture);
        }
    }        
        
    
    private void loadWindowsDll(String architecture) {
 
        String libraryName = "";
        if (architecture.equals("64")) {
            libraryName = "lib/IConnectJNIx64";
        } else if (architecture.equals("32")) {
            libraryName = "lib/IConnectJNI";
        } else {
            throw new IllegalStateException("Unknown 32/64 bit architecture:" + architecture);
        }

        try {
            System.loadLibrary(libraryName);
        } catch (Throwable t) {
            log("Error loading library: %s", libraryName);
            log("Throwable: ", t.toString());
            log("java.library.path = %s", System.getProperty("java.library.path"));
            throw t;
        }
         
    }
    
    
    private void loadLinuxSo(String architecture) throws URISyntaxException {

        if (!architecture.equals("64")) {
            throw new IllegalStateException("Only 64-bit is currently supported. Contact iSYSTEM support for 32-bit version.");
        }

        String libraryName = "";
        try {
            // f. loadLibrary('yaml-0') triggers extraction of the libyaml-0.so from
            // icadapter jar file to dir 'configuration/...'. This way it is
            // available when libiconnectJava.so is loaded.
            // libraryName = "yaml-0";   commented 2021-04-29, since linking was changed to static
            // System.loadLibrary(libraryName);
            libraryName = "iconnectJava";
            System.loadLibrary(libraryName);
        } catch (Throwable t) {
            log("Error loading library: ", libraryName);
            log("Throwable: ", t.toString());
            log("java.library.path = ", System.getProperty("java.library.path"));
            throw t;
        }
    }

    
    /**
     * Returns directory of jar file containing this class. Native libraries (dll, so)
     * are expected in this directory.
     */
    @SuppressWarnings("unused")
    private String getJarFileDir() throws URISyntaxException {
        Path jarPath = Paths.get(CDebugFacade.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return jarPath.getParent().toAbsolutePath().toString();
    }

    
    private void log(String msg, String desc) {
    	
    	System.err.println(msg + ": " + desc);
//        getDefault().getLog().log(new Status(status, 
//                "Error", 
//                Status.OK, 
//                message, 
//                thr));
    }
    
    
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        instance = null;
    }
    
    public static IConnectAdapterActivator getPlugin() {
        return instance;
    }
}
