package si.isystem.mk.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class PlatformUtilsTest {

    String m_prefsNodeId = "unit.test.node";

    @Before
    public void setUp() throws Exception {
        String[] prefs = PlatformUtils.getListOfPreferences(m_prefsNodeId);
        if (prefs.length == 0) {
            PlatformUtils.addToListOfPreferences(m_prefsNodeId, "B", 3);
            PlatformUtils.addToListOfPreferences(m_prefsNodeId, "C", 3);
            PlatformUtils.addToListOfPreferences(m_prefsNodeId, "A", 3);
        }
    }


    @Test
    public void testPreferencs() throws BackingStoreException {
        String[] prefs = PlatformUtils.getListOfPreferences("unit.test.node");

        assertEquals(3, prefs.length);
        assertEquals("B", prefs[0]);
        assertEquals("C", prefs[1]);
        assertEquals("A", prefs[2]);
        
        PlatformUtils.addToListOfPreferences(m_prefsNodeId, "F", 3);
        
        prefs = PlatformUtils.getListOfPreferences("unit.test.node");
        assertEquals(3, prefs.length);
        assertEquals("C", prefs[0]);
        assertEquals("A", prefs[1]);
        assertEquals("F", prefs[2]);

        PlatformUtils.addToListOfPreferences(m_prefsNodeId, "C", 3);
        
        prefs = PlatformUtils.getListOfPreferences("unit.test.node");
        assertEquals(3, prefs.length);
        assertEquals("A", prefs[0]);
        assertEquals("F", prefs[1]);
        assertEquals("C", prefs[2]);

        PlatformUtils.addToListOfPreferences(m_prefsNodeId, "F", 3);
        
        prefs = PlatformUtils.getListOfPreferences("unit.test.node");
        assertEquals(3, prefs.length);
        assertEquals("A", prefs[0]);
        assertEquals("C", prefs[1]);
        assertEquals("F", prefs[2]);
    }
}
