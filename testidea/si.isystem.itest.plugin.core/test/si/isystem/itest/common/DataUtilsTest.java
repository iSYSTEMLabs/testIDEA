package si.isystem.itest.common;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class DataUtilsTest {

    @Test
    public void testSplit() {
        
        List<String> res = DataUtils.splitToList(""); // should return list with one empty string 
        assertEquals(0, res.size());
        
        res = DataUtils.splitToList("asd");  
        assertEquals("asd", res.get(0));
        assertEquals(1, res.size());
        
        res = DataUtils.splitToList("asd, 23");  
        assertEquals("asd", res.get(0));
        assertEquals("23", res.get(1));
        assertEquals(2, res.size());
        
        res = DataUtils.splitToList("asd, ");  
        assertEquals("asd", res.get(0));
        assertEquals("", res.get(1));
        assertEquals(2, res.size());
        
        res = DataUtils.splitToList(", 23");  
        assertEquals("", res.get(0));
        assertEquals("23", res.get(1));
        assertEquals(2, res.size());
        
        res = DataUtils.splitToList(",");  
        assertEquals("", res.get(0));
        assertEquals("", res.get(1));
        assertEquals(2, res.size());
        
        // test for quotes
        res = DataUtils.splitToList("\"asd\", '23'");  
        assertEquals("\"asd\"", res.get(0));
        assertEquals("'23'", res.get(1));
        assertEquals(2, res.size());
        
        res = DataUtils.splitToList("\"\", ''");  
        assertEquals("\"\"", res.get(0));
        assertEquals("''", res.get(1));
        assertEquals(2, res.size());
        
        res = DataUtils.splitToList("\"as, dd\\\", er\", '2, \\', 3'");  
        assertEquals("\"as, dd\\\", er\"", res.get(0));
        assertEquals("'2, \\', 3'", res.get(1));
        assertEquals(2, res.size());
    }
    
    @Test
    public void splitToListWithISysQualifiedNames() {
        // IMPORTANT: This is the test string as could be entered by the user. Copy-paste
        // it to string 'input' below (inside quotes) in Eclipse to escape it properly (must have
        // option in preferences (search for paste) enabled.
        // 23, "in, string", second, "main.c"#func,,sample.elf, "one,,two, \\\"three, four\"\\", " \\", "a \"b", ',', '\'', '.',
        String input =
           "23, \"in, string\", second, \"main.c\"#func,,sample.elf, \"one,,two, \\\\\\\"three, four\\\"\\\\\", \" \\\\\", \"a \\\"b\", ',', '\\'', '.',";

        List<String> result = DataUtils.splitToListWithISysQualifiedNames(input);
//        System.out.println(input);
//        for (String str : result) {
//            System.out.println(str);
//        }
        
        int idx = 0;
        assertEquals("23", result.get(idx++));
        assertEquals("\"in, string\"", result.get(idx++));
        assertEquals("second", result.get(idx++));
        assertEquals("\"main.c\"#func,,sample.elf", result.get(idx++));
        
        // "one,,two, \\\"three, four\"\\"
        assertEquals("\"one,,two, \\\\\\\"three, four\\\"\\\\\"", result.get(idx++));
        
        // " \\"
        assertEquals("\" \\\\\"", result.get(idx++));
        
        // "a \"b"
        assertEquals("\"a \\\"b\"", result.get(idx++));
        
        // ','
        assertEquals("','", result.get(idx++));
        
        // '\''
        assertEquals("'\\''", result.get(idx++));
        
        // '.'
        assertEquals("'.'", result.get(idx++));
        
        result = DataUtils.splitToListWithISysQualifiedNames("1, 2");
        idx = 0;
        assertEquals("1", result.get(idx++));
        assertEquals("2", result.get(idx++));
    }
    
}
