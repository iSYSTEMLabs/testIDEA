package si.isystem.itest.dialogs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RenameDialogTest {


    @Test
    public void testReplaceVar() {
        
        String res = RenameDialog.replaceVar("baloon", "baloon", "off");
        assertEquals("off", res);
        
        res = RenameDialog.replaceVar("baloon", "on", "off");
        assertEquals(null, res);
        
        res = RenameDialog.replaceVar("baloon", "bal", "off");
        assertEquals(null, res);
        
        res = RenameDialog.replaceVar("baloon", "lo", "off");
        assertEquals(null, res);
        

        res = RenameDialog.replaceVar("*baloon", "baloon", "off");
        assertEquals("*off", res);
        
        res = RenameDialog.replaceVar("baloon[1]", "baloon", "off");
        assertEquals("off[1]", res);
        
        res = RenameDialog.replaceVar("*baloon[1]", "baloon", "off");
        assertEquals("*off[1]", res);
        
        res = RenameDialog.replaceVar("*(baloon + 1)", "baloon", "off");
        assertEquals("*(off + 1)", res);
    }
}
