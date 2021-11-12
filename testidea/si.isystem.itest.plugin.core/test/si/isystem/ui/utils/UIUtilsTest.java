package si.isystem.ui.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import si.isystem.itest.common.UiUtils;

public class UIUtilsTest {

    @Test
    public void testAddExtension() {
        String fName = UiUtils.addExtension("d:/sdff.ccv/text", "ccv", true, true);
        assertEquals("d:/sdff.ccv/text.ccv", fName);
        
        fName = UiUtils.addExtension("d:/sdff.ccv/text.txt", "ccv", true, true);
        assertEquals("d:/sdff.ccv/text.txt.ccv", fName);
        
        fName = UiUtils.addExtension("d:/sdff.ccv/text.txt", "ccv", false, true);
        assertEquals("d:/sdff.ccv/text.txt", fName);
        
        fName = UiUtils.addExtension("d:/sdff.ccv/text.CCV", "ccv", true, false);
        assertEquals("d:/sdff.ccv/text.CCV", fName);
        
        fName = UiUtils.addExtension("d:/sdff.ccv/text.txt", "ccv", false, false);
        assertEquals("d:/sdff.ccv/text.txt", fName);
        
    }

    
    @Test 
    public void testGetExtension() {
        String extension = UiUtils.getExtension("d:/sdff.ccv/text");
        assertEquals("", extension);
        
        extension = UiUtils.getExtension("d:/sdff.ccv/text.");
        assertEquals("", extension);
        
        extension = UiUtils.getExtension("d:/sdff.ccv/text.txt");
        assertEquals("txt", extension);
        
        extension = UiUtils.getExtension("text.CCV");
        assertEquals("CCV", extension);
        
        extension = UiUtils.getExtension("text");
        assertEquals("", extension);
    }
    
    @Test
    public void testReplaceNonAlphanumChars() {
        String str = UiUtils.replaceNonAlphanumChars("func,,sample.elf");
        assertEquals("func__sample_elf", str);
        
        str = UiUtils.replaceNonAlphanumChars("func_sample_elf");
        assertEquals("func_sample_elf", str);
    }
}
