package si.isystem.python;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import si.isystem.exceptions.SException;

public class PythonTest {
    @Test
    public void testPython() {
        
        // test python on path in current dir
        Python py = new Python();
        py.startPythonInteractively("d:\\apps\\Python27", new File("."));
        py.waitForPrompt(10000, true);
        
        ScriptResult result = py.callFunction("dir", new String[]{}, 3000, true);
        List<String> retVal = result.getFuncRetVal();
        Assert.assertEquals(1, retVal.size());
        Assert.assertEquals("['__builtins__', '__doc__', '__name__', '__package__']", retVal.get(0));
        
        result = py.execStatements(3000, true, "import isystem.connect").get(0);
        List<String> stdOut = result.getStdout();
        Assert.assertEquals(0, stdOut.size());
        
        result = py.execStatements(3000, true, "3*45").get(0);
        stdOut = result.getStdout();
        Assert.assertEquals(1, stdOut.size());
        Assert.assertEquals("135", stdOut.get(0));
        
        String[] prg = new String[4];
        prg[0] = "45/ 9";
        prg[0] = "l = ['a', 'b']";
        prg[0] = "len(l)";
        prg[0] = "print 'hi\\nho'";
        List<ScriptResult> results = py.execStatements(3000, true, prg);
        Assert.assertEquals(4, results.size());
        stdOut = result.getStdout();
        Assert.assertEquals(1, results.get(0).getStdout().size());
        Assert.assertEquals(0, results.get(1).getStdout().size());
        Assert.assertEquals(1, results.get(2).getStdout().size());
        Assert.assertEquals(2, results.get(3).getStdout().size());
        
        Assert.assertEquals("5", results.get(0).getStdout().get(0));
        Assert.assertEquals("2", results.get(2).getStdout().get(0));
        Assert.assertEquals("hi", results.get(3).getStdout().get(0));
        Assert.assertEquals("ho", results.get(3).getStdout().get(1));

        result = py.execStatements(3000, false, "err = 'p").get(0);
        Assert.assertEquals(true, result.isError());
        Assert.assertEquals(0, result.getStdout().size());
        Assert.assertEquals("  File \"<stdin>\", line 1", result.getStderr().get(0));
        Assert.assertEquals("    err = 'p", result.getStderr().get(1));
        Assert.assertEquals("           ^", result.getStderr().get(2));
        Assert.assertEquals("SyntaxError: EOL while scanning string literal", result.getStderr().get(3));
        
        try {
            result = py.execStatements(3000, true, "print 23; errx(3)").get(0);
            Assert.fail("Exception expected!");
        } catch (SException ex) {
        }
        
        result = py.execStatements(3000, false, "print 23; errx(3)").get(0);
        stdOut = result.getStdout();
        List<String> stdErr = result.getStderr();
        Assert.assertEquals(1, stdOut.size());
        Assert.assertEquals(4, stdErr.size());
        Assert.assertEquals("23", stdOut.get(0));
        Assert.assertEquals("Traceback (most recent call last):", stdErr.get(0));
        Assert.assertEquals("  File \"<stdin>\", line 1, in <module>", stdErr.get(1));
        Assert.assertEquals("NameError: name 'errx' is not defined", stdErr.get(2));
        Assert.assertEquals(ReaderThread.PROMPT, stdErr.get(3));
        
        py.execStatements(3000, true, "s = 'gun'");
        result = py.callFunction("s.replace", new String[]{"'g'", "'r'"}, 1000, true);
        stdOut = result.getFuncRetVal();
        Assert.assertEquals(1, stdOut.size());
        Assert.assertEquals("'run'", stdOut.get(0));

        
        /*
         * The following indented code is not supported yet - 
         * the '...' prompt is not detected!
         * 
        py.execStatement("def adder(a, b):", 3000, true);
        py.execStatement("    return a + b\n", 3000, true);
        
        result = py.callFunction("adder", new String[]{"5", "9"}, 3000, true);
        Assert.assertEquals(1, stdOut.size());
        Assert.assertEquals("14", stdOut.get(0));
        */

        py.close();
    }
    
}
