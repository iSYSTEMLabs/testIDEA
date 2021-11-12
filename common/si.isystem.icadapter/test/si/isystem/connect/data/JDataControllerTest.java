package si.isystem.connect.data;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import si.isystem.connect.CDataController2;
import si.isystem.connect.CDebugFacade;
import si.isystem.connect.CExecutionController.ETimeoutMode;
import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.IConnectDebug.EAccessFlags;
import si.isystem.connect.adapters.JDataController;


public class JDataControllerTest {

    private JDataController m_jdataCtrl;
    private CDebugFacade m_dbg;

    @Before
    public void setUp() throws Exception {
        
        String architecture = System.getProperty("osgi.arch");
        
        if (architecture == null) {  // happens during testing
            architecture = System.getProperty("os.arch");
            
            if (architecture == null) {  // should not happen
                architecture = "x86"; // default is the most common platform
            }
        }
        
        String libraryName;
        
        if (architecture.equals("x86_64")) {
            libraryName = "../si.isystem.itest/lib/IConnectJNIx64";
        } else if (architecture.equals("x86")) {
            libraryName = "D:/bb/trunk/Eclipse/proj/si.isystem.itest/lib/IConnectJNI.dll";
        } else {
            throw new IllegalStateException("Unknown 32/64 bit architecture:" + architecture);
        }

        System.load(libraryName);
        
        
        ConnectionMgr cmgr = new ConnectionMgr();
        cmgr.connectMRU();
        
        CDataController2 dataCtrl = new CDataController2(cmgr);
        
        m_jdataCtrl = new JDataController(dataCtrl, null, null);
        
        m_dbg = new CDebugFacade(cmgr);
        m_dbg.download();
        m_dbg.runUntilFunction("main", ETimeoutMode.TOUT_10s);
        
        m_dbg.modify(EAccessFlags.fMonitor, "isDebugTest", "1");
        
        m_dbg.runUntilFunction("min_int", ETimeoutMode.TOUT_10s);
        
        m_dbg.stepOverHigh();
    }
    
    
    @Test
    public void testStack() {
        JStackFrame [] frames = m_jdataCtrl.getStackFrames(0, -1, false);
    
        Assert.assertEquals(3, frames.length);

        JStackFrame frame = frames[0];
        Assert.assertEquals("common\\debug.c", frame.getFileName());
        Assert.assertEquals("min_int", frame.getFunction().getName());
        Assert.assertEquals(0, frame.getLevel());
        JVariable[] vars = frame.getLocalVars();
        Assert.assertEquals(2, vars.length);
        Assert.assertEquals("a", vars[0].getName());
        Assert.assertEquals("b", vars[1].getName());
        Assert.assertEquals("long (long a, long b)", frame.getFunction().getPrototype());
        JVariable[] args = frame.getFunction().getParameters();
        Assert.assertEquals(2, args.length);
        Assert.assertEquals("a", args[0].getName());
        Assert.assertEquals("b", args[1].getName());
        


        frame = frames[1];
        Assert.assertEquals("common\\debug.c", frame.getFileName());
        Assert.assertEquals("debugMain", frame.getFunction().getName());
        Assert.assertEquals(1, frame.getLevel());

        frame = frames[2];
        Assert.assertEquals("common\\main.c", frame.getFileName());
        Assert.assertEquals("main", frame.getFunction().getName());
        Assert.assertEquals(2, frame.getLevel());

        
    }
}
