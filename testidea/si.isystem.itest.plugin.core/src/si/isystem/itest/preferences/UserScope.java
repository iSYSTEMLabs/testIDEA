package si.isystem.itest.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScope;
import org.osgi.service.prefs.BackingStoreException;

import si.isystem.exceptions.SIOException;

/**
 * This class creates preferences, which are stored into user's home
 * folder. It is hard to find in documentation, but it must implement the IScope
 * interface (see topic 
 * "Platform Plug-in Developer Guide > Programmer's Guide > Resources overview > 
 *  Project-scoped preferences").
 * 
 * @author markok
 *
 */
public class UserScope implements IScope {
    
    public static final String SCOPE = "userScope"; // Should be the same name 
                                                    // as used for scope in extension
                                                    // point.
    
    
    @Override
    public IEclipsePreferences create(IEclipsePreferences parent, String name) {
        if (name.equals(SCOPE)) {
            try {
                UserScopePreferences prefs = new UserScopePreferences(parent);
                prefs.load();
                return prefs;
            } catch (BackingStoreException ex) {
                throw new SIOException(ex);
            }
        } 

        return null;
    }

}
