package si.isystem.itest.xls;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import si.isystem.exceptions.SException;


public class CsvImporterTest {

    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testParseLine() {
        CsvImporter importer = new CsvImporter();
        
        List<String> tokens = importer.parseLine("");
        
        assertEquals(0, tokens.size());
        
        tokens = importer.parseLine("\"ab\", \"c, d \", \"\"\"\", \"\"\"xy\"\"z\"\"\"");
        assertEquals(4, tokens.size());
        assertEquals("ab", tokens.get(0));
        assertEquals("c, d ", tokens.get(1));
        assertEquals("\"", tokens.get(2));
        assertEquals("\"xy\"z\"", tokens.get(3));

        try {
            // should throw an exception because of char 5 not in quotes 
            importer.parseLine("\"ab\", 5 \"c, d \", \"\"\"\", \"\"\"xy\"\"z\"\"\", ");
            assertEquals(0, 1);
        } catch (SException ex) {
            // Printer.print(SEFormatter.ex2Yaml(ex, 5));
            assertEquals('5', ex.getData().get("invalidChar"));
        }
    }

}
