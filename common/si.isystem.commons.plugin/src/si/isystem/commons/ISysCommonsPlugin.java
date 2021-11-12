package si.isystem.commons;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class ISysCommonsPlugin extends AbstractUIPlugin {
    
    public static String PLUGIN_ID = "si.isystem.commons.plugin";
    
    private static ISysCommonsPlugin instance = null;
    
    
    public ISysCommonsPlugin() {
    }

    
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }
    

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        instance = null;
    }

    
    public static ISysCommonsPlugin getPlugin() {
        return instance;
    }
}
