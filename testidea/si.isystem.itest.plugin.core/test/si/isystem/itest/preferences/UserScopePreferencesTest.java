package si.isystem.itest.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This class tests user preferences.  
 * 
 * @author markok
 *
 */
public class UserScopePreferencesTest {

    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testPrefs() {
        
        UserScopePreferences prefs = new UserScopePreferences();
        prefs.put("str", "strPref");
        prefs.putBoolean("bool", true);
        prefs.putDouble("dbl", 3.45);
        prefs.putFloat("float", 2.34f);
        prefs.putInt("int", 12345);
        prefs.putLong("long", 1_234_567_898_765L);
        
        Preferences node = prefs.node("myplugin.node");
        node.put("nodeKeyStr", "nodeValue");
        node.putInt("nodeKeyInt", -45);

        node = prefs.node("myplugin.X");
        node.put("nodeKeyStrX", "nodeValueX");
        node.putInt("nodeKeyIntX", -46);
        
        node = prefs.node("hisPlugin/nodX");
        node.put("X", "Y");

        try {
            prefs.flush(new File("test.xml"));
            
            prefs= new UserScopePreferences();
            prefs.load(new File("test.xml"));
            
            assertEquals("strPref", prefs.get("str", ""));
            assertTrue(prefs.getBoolean("bool", false));
            assertEquals(3.45, prefs.getDouble("dbl", 0), 0.0001);
            assertEquals(2.34f, prefs.getFloat("float", 0), 0.0001);
            assertEquals(12345, prefs.getInt("int", 0));
            assertEquals(1_234_567_898_765L, prefs.getLong("long", 0));
            
            node = prefs.node("myplugin.node");
            
            assertEquals("nodeValue", node.get("nodeKeyStr", ""));
            assertEquals(-45, node.getInt("nodeKeyInt", 0));
            
            node = prefs.node("myplugin.X");
            
            assertEquals("nodeValueX", node.get("nodeKeyStrX", ""));
            assertEquals(-46, node.getInt("nodeKeyIntX", 0));
            
            node = prefs.node("hisPlugin");
            node = node.node("nodX");
            assertEquals("Y", node.get("X", ""));
            
        } catch (BackingStoreException ex) {
            assertTrue("Unexpected exception when testing preferences: " + ex, true);
        }
        
    }
}
