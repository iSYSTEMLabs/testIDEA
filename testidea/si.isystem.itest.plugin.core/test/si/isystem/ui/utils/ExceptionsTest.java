package si.isystem.ui.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import si.isystem.connect.connectJNI;

public class ExceptionsTest {

    @Before
    public void setUp() throws Exception {
        loadLibrary();
    }

    
    public final static void loadLibrary() {
        String architecture = System.getProperty("sun.arch.data.model");

        String libraryName;
        
        if (architecture.equals("64")) {
            libraryName = "../../common/si.isystem.icadapter/lib/IConnectJNIx64";
        } else if (architecture.equals("32")) {
            libraryName = "../../common/si.isystem.icadapter/lib/IConnectJNI";
        } else {
            throw new IllegalStateException("Unknown 32/64 bit architecture:" + architecture);
        }

        try {
            System.out.println("java.library.path = " + System.getProperty("java.library.path"));
            System.out.println("Loading native library: " + libraryName);
            System.loadLibrary(libraryName);
        } catch (Throwable thr) {
            System.err.println("Error loading library: " + libraryName);
            System.err.println("Error: " + thr.toString());
            thr.printStackTrace();
            return;
        }
        
        System.out.println("isystem.connect demo for Java version: " + 
                           si.isystem.connect.connectJNI.getModuleVersion());
    }

    
    @Test
    public void testExceptions() {
        int i = 0;
        final int NO_OF_EXCEPTIONS = 21;
        
        for (i = 0; i < NO_OF_EXCEPTIONS; i++) {
            try {
                // connectJNI.exceptionsTest(i);
            } catch (Exception ex) {
                System.out.println(i + ": " + ex.getMessage() + "  -->  " + ex.getClass().getSimpleName());
            }
        }
        
        assertEquals(NO_OF_EXCEPTIONS, i);
    }
    
}
