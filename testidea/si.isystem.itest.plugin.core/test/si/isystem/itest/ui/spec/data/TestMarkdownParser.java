package si.isystem.itest.ui.spec.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;
import org.junit.Test;


public class TestMarkdownParser {

    @Test
    public void testMarkdown() {
        MarkdownParser parser = new MarkdownParser(null);
        List<StyleRange> styles = new ArrayList<>();
        
        StringBuilder mdText = parser.markdown2StyleRanges("", styles);
        assertEquals("", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("normal", styles);
        assertEquals("normal", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("*", styles);
        assertEquals("*", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("**", styles);
        assertEquals("**", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("_", styles);
        assertEquals("_", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("__", styles);
        assertEquals("__", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("_*", styles);
        assertEquals("_*", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("*_", styles);
        assertEquals("*_", mdText.toString());
        assertTrue(styles.isEmpty());
        
        mdText = parser.markdown2StyleRanges("\\*", styles);
        assertEquals("*", mdText.toString());
        assertTrue(styles.isEmpty());
        
        String text = "**bold**";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("bold", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = "**bold**normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("boldnormal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = "**bold** normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("bold normal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = "__bold__normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("__bold__normal", mdText.toString());
        assertEquals(0, styles.size());
        
        text = "__bold__ normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("bold normal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);

        text = "__bold__normal__";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("bold__normal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(12, styles.get(0).length);
        
        text = "*italic*";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("italic", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        
        text = "*italic*normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("italicnormal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        
        text = "*italic* normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("italic normal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(6, styles.get(0).length);

        text = "_italic_normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("_italic_normal", mdText.toString());
        assertEquals(0, styles.size());

        text = "_italic_ normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("italic normal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        
        text = "`code`";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("code", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = "`code`normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("codenormal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = "`code` normal";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("code normal", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = "op_yu_rt";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertEquals(0, styles.size());
        
        text = "op*yu*rt";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("opyurt", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(2, styles.get(0).start);
        assertEquals(2, styles.get(0).length);

        text = "op__yu__rt";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertEquals(0, styles.size());
        
        text = "op**yu**rt";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("opyurt", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(2, styles.get(0).start);
        assertEquals(2, styles.get(0).length);

        text = "op_yu_rt\n**bold**\n_italic_\nnormal\n__bold__\n*italic*\n`code`\n";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("op_yu_rt\nbold\nitalic\nnormal\nbold\nitalic\ncode\n", mdText.toString());
        assertEquals(5, styles.size());
        
        assertEquals(9, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        assertEquals(14, styles.get(1).start);
        assertEquals(6, styles.get(1).length);
        
        assertEquals(28, styles.get(2).start);
        assertEquals(4, styles.get(2).length);
        
        assertEquals(33, styles.get(3).start);
        assertEquals(6, styles.get(3).length);
        
        assertEquals(40, styles.get(4).start);
        assertEquals(4, styles.get(4).length);

        text = "*ptr * 3";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertTrue(styles.isEmpty());
        
        text = "my\\*car*is *ptr";  
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("my*car*is *ptr", mdText.toString());
        assertTrue(styles.isEmpty());
        
        text = "my\\\\*car*is *ptr";   // '\\' is '\'  
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("my\\caris *ptr", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(3, styles.get(0).start);
        assertEquals(3, styles.get(0).length);
        
        text = "__ptr = __exe";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertTrue(styles.isEmpty());
        
        text = "j_tr"; 
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertTrue(styles.isEmpty());

        text = "j`tr"; 
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertTrue(styles.isEmpty());

        text = "inside `code **section** __bold__ *and* _italic_ are not` visible";        
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("inside code **section** __bold__ *and* _italic_ are not visible", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(7, styles.get(0).start);
        assertEquals(48, styles.get(0).length);
        
        text = "__r **t** p__";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("r t p", mdText.toString());
        assertEquals(3, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(2, styles.get(0).length);
        assertEquals(2, styles.get(1).start);
        assertEquals(1, styles.get(1).length);
        assertEquals(3, styles.get(2).start);
        assertEquals(2, styles.get(2).length);

        text = "_o*p*iu_";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("opiu", mdText.toString());
        assertEquals(3, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(1, styles.get(0).length);
        assertEquals(1, styles.get(1).start);
        assertEquals(1, styles.get(1).length);
        assertEquals(2, styles.get(2).start);
        assertEquals(2, styles.get(2).length);
        
        text = "*o_p_iu*";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("o_p_iu", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(6, styles.get(0).length);

        text = "**r__t__ p**";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("r__t__ p", mdText.toString());
        assertEquals(3, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        assertEquals(4, styles.get(1).start);
        assertEquals(2, styles.get(1).length);
        assertEquals(6, styles.get(2).start);
        assertEquals(2, styles.get(2).length);
        
        text = "__we__r__ertet__";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("we__r__ertet", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(12, styles.get(0).length);

        text = "we__r__ertet";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(text, mdText.toString());
        assertEquals(0, styles.size());

        text = "**rt**w**p**";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("rtwp", mdText.toString());
        assertEquals(2, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(2, styles.get(0).length);
        assertEquals(3, styles.get(1).start);
        assertEquals(1, styles.get(1).length);

        text = "**asd*op*sd**";  // this one behaves strange in Git Hub - five 
                                 // letters in italic, 'sd' normal style followed by **,
                                 // equivalent to *asdop*sd** 
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("asdopsd", mdText.toString());
        assertEquals(3, styles.size());
        assertEquals(0, styles.get(0).start);
        assertEquals(3, styles.get(0).length);
        assertEquals(3, styles.get(1).start);
        assertEquals(2, styles.get(1).length);
        assertEquals(5, styles.get(2).start);
        assertEquals(2, styles.get(2).length);
        
        
        text = "mixed **tokens__ and __again**.";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("mixed tokens__ and __again.", mdText.toString());
        assertEquals(5, styles.size());
        assertEquals(6, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        assertEquals(12, styles.get(1).start);
        assertEquals(2, styles.get(1).length);
        assertEquals(14, styles.get(2).start);
        assertEquals(5, styles.get(2).length);
        
        assertEquals(19, styles.get(3).start);
        assertEquals(2, styles.get(3).length);
        assertEquals(21, styles.get(4).start);
        assertEquals(5, styles.get(4).length);

        text = "mixed **tokens\\_\\_ and \\__again**.";  
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("mixed tokens__ and __again.", mdText.toString());
        assertEquals(6, styles.size());
        assertEquals(6, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        assertEquals(12, styles.get(1).start);
        assertEquals(1, styles.get(1).length);

        assertEquals(13, styles.get(2).start);
        assertEquals(1, styles.get(2).length);
        assertEquals(14, styles.get(3).start);
        assertEquals(5, styles.get(3).length);

        assertEquals(19, styles.get(4).start);
        assertEquals(1, styles.get(4).length);
        assertEquals(20, styles.get(5).start);
        assertEquals(6, styles.get(5).length);

        
        text = "mixed **tokens\\_\\_ and \\_\\_again**.";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("mixed tokens__ and __again.", mdText.toString());
        assertEquals(7, styles.size());
        assertEquals(6, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        assertEquals(12, styles.get(1).start);
        assertEquals(1, styles.get(1).length);

        assertEquals(13, styles.get(2).start);
        assertEquals(1, styles.get(2).length);
        assertEquals(14, styles.get(3).start);
        assertEquals(5, styles.get(3).length);

        assertEquals(19, styles.get(4).start);
        assertEquals(1, styles.get(4).length);
        assertEquals(20, styles.get(5).start);
        assertEquals(1, styles.get(5).length);
        
        assertEquals(21, styles.get(6).start);
        assertEquals(5, styles.get(6).length);

        
        text = "typical _text_ in **bold** and __bold w spaces__ and `func()` in *italic* and _normal* and *normal_.";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("typical text in bold and bold w spaces and func() in italic and normal* and *normal.", mdText.toString());
        assertEquals(10, styles.size());
        assertEquals(8, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        assertEquals(16, styles.get(1).start);
        assertEquals(4, styles.get(1).length);

        assertEquals(25, styles.get(2).start);   // bold and bold w spaces
        assertEquals(13, styles.get(2).length);
        assertEquals(43, styles.get(3).start);   // func()
        assertEquals(6, styles.get(3).length);

        assertEquals(53, styles.get(4).start);   // *italic*
        assertEquals(6, styles.get(4).length);
        assertEquals(64, styles.get(5).start);
        assertEquals(6, styles.get(5).length);

        assertEquals(70, styles.get(6).start);
        assertEquals(1, styles.get(6).length);
        assertEquals(71, styles.get(7).start);
        assertEquals(5, styles.get(7).length);

        assertEquals(76, styles.get(8).start);
        assertEquals(1, styles.get(8).length);
        assertEquals(77, styles.get(9).start);
        assertEquals(6, styles.get(9).length);

        
        text = ",_italic_.";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(",italic.", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(1, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        
        text = "=__bold__=";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("=bold=", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(1, styles.get(0).start);
        assertEquals(4, styles.get(0).length);
        
        text = ",_.italic._.";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(",_.italic._.", mdText.toString());
        assertEquals(0, styles.size());
        
        text = "=__=bold=__=";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("=__=bold=__=", mdText.toString());
        assertEquals(0, styles.size());
        
        text = ",*.italic.*.";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals(",.italic..", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(1, styles.get(0).start);
        assertEquals(8, styles.get(0).length);
        
        text = "=**[bold]**=";
        mdText = parser.markdown2StyleRanges(text, styles);
        assertEquals("=[bold]=", mdText.toString());
        assertEquals(1, styles.size());
        assertEquals(1, styles.get(0).start);
        assertEquals(6, styles.get(0).length);
        
    }
}
