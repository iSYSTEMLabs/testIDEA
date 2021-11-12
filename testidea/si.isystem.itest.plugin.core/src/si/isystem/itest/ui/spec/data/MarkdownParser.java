package si.isystem.itest.ui.spec.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;

import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.ui.utils.FontProvider;

/**
 * This class parses Markdown text and outputs StyleRange-s for SWT's StyledText
 * control. Only the following syntax is currently supported:
 * 
 * ** - bold, for example  "example of **bold** word and le**t**ter".
 * __ - bold, but only if not surrounded by letters or digits, for example:
 *      "__bold__, but__this is not__bold. [__this is bold__]".
 * * - italic 
 * _ - italic, but only if not surrounded by letters or digits
 * ` - (backquote) code, for example: "function  `main()`". '**', '*', '__' and '_'
 *     have no special meaning inside code blocks.
 *     
 * All three character ('*', '_', and '`') can be escaped with '\', for example '\*'.
 * 
 * See unit test for examples. As a reference GuitHub parser was used:
 * https://jbt.github.io/markdown-editor/
 * 
 * @author markok
 *
 */
public class MarkdownParser {

    // whitespaces at start to detect __ at start of string
    private char ch1 = ' ', ch2 = ' ', ch3 = ' ', ch4 = ' ';
    private int m_srcTxtIdx = 0;
    private boolean m_isInBoldArea;
    private boolean m_isInItalicArea;
    private boolean m_isInCodeArea;
    private String m_text;
    private Control m_controlForFont;

    private enum EMarkdownToken {

        BOLD_ASTERISK_S(2, "**"), BOLD_UNDERSCORE_S(2, "__"),
        BOLD_ASTERISK_E(2, "**"), BOLD_UNDERSCORE_E(2, "__"), 

        ITALIC_ASTERISK_S(1, "*"), ITALIC_UNDERSCORE_S(1, "_"), 
        ITALIC_ASTERISK_E(1, "*"), ITALIC_UNDERSCORE_E(1, "_"), 
        
        CODE_S(1, "`"), CODE_E(1, "`"),
        
        ESCAPE_ASTERISK(2, "*"), ESCAPE_UNDERSCORE(2, "_"), ESCAPE_CODE(2, "`"), 
        TEXT(0, ""), ESCAPE_BACKSLASH(2, "\\");

        int m_charsInToken;
        String m_tokenStr;

        EMarkdownToken(int charsInToken, String tokenStr) {
            m_charsInToken = charsInToken;
            m_tokenStr = tokenStr;
        }

        
        int getSize() {
            return m_charsInToken;
        }
        
        @Override
        public String toString() {
            return super.toString() + ", " + m_tokenStr;
        }
    };

    
    private class MarkdownToken {
        int m_pos;
        private int m_size;
        EMarkdownToken m_ttype;
        boolean m_isIgnored = false;

        MarkdownToken(int pos, EMarkdownToken ttype) {
            m_pos = pos;
            m_ttype = ttype;
        }
        
        MarkdownToken(int pos, int size) {
            m_pos = pos;
            m_size = size;
            m_ttype = EMarkdownToken.TEXT;
        }
        
        int getSize() {
            if (m_ttype == EMarkdownToken.TEXT) {
                return m_size;
            }
            return m_ttype.getSize();
        }

        public void setIgnored(boolean isIgnored) {
            m_isIgnored = isIgnored;
        }

        public String getTokenStr() {
            return m_ttype.m_tokenStr;
        }

        @Override
        public String toString() {
            return "pos : " + m_pos
                    + "\nsize: " + m_size
                    + "\ntype: " + m_ttype
                    + "\nignored: " + m_isIgnored;
        }
    }

    
    public MarkdownParser(Control controlForFont) {
        m_controlForFont = controlForFont;
    }
    
    
    public StringBuilder markdown2StyleRanges(String text, List<StyleRange> styles) {

        ch1 = ch2 = ch3 = ch4 = ' ';
        m_srcTxtIdx = 0;
        m_isInCodeArea = m_isInBoldArea = m_isInItalicArea = false;
        m_text = text;
        styles.clear();
        List<MarkdownToken> tokens = new ArrayList<>();
        int startIdx = m_srcTxtIdx;
        MarkdownToken token = nextToken();
        
        while (token != null) {
            
            addTextToken(tokens, startIdx, token.m_pos);
            tokens.add(token);
            
            startIdx = token.m_pos + token.getSize();
            token = nextToken();
        }
        
        addTextToken(tokens, startIdx, text.length());  // text after the last token
        
        normalizeTokens(tokens);
        
        boolean isCode = false;
        boolean isBoldAsterisk = false, isBoldUnderscore = false;
        boolean isItalicAsterisk = false, isItalicUnderscore = false;

        StringBuilder sb = new StringBuilder();
        
        for (MarkdownToken tok : tokens) {

            int styleStart = sb.length();
            boolean isBold = isBoldAsterisk  ||  isBoldUnderscore; 
            boolean isItalic = isItalicAsterisk  ||  isItalicUnderscore;
            
            if (tok.m_isIgnored) {
                appendTextAndStyle(sb, tok.getTokenStr(), styles, styleStart, 
                                   isBold, isItalic, isCode);
                continue;
            }
            
            switch (tok.m_ttype) {
            case TEXT:
                appendTextAndStyle(sb, m_text.substring(tok.m_pos, tok.m_pos + tok.getSize()),
                                   styles, styleStart, isBold, isItalic, isCode);
                break;
            case BOLD_ASTERISK_S:
                isBoldAsterisk = true;
                break;
            case BOLD_ASTERISK_E:
                isBoldAsterisk = false;
                break;
            case BOLD_UNDERSCORE_S:
                isBoldUnderscore = true;
                break;
            case BOLD_UNDERSCORE_E:
                isBoldUnderscore = false;
                break;
                
            case ITALIC_ASTERISK_S:
                isItalicAsterisk = true;
                break;
            case ITALIC_ASTERISK_E:
                isItalicAsterisk = false;
                break;
            case ITALIC_UNDERSCORE_S:
                isItalicUnderscore = true;
                break;
            case ITALIC_UNDERSCORE_E:
                isItalicUnderscore = false;
                break;
                
            case CODE_S:
                isCode = true;
                break;
            case CODE_E:
                isCode = false;
                break;
                
            case ESCAPE_ASTERISK:
            case ESCAPE_CODE:
            case ESCAPE_UNDERSCORE:
            case ESCAPE_BACKSLASH:
                appendTextAndStyle(sb, tok.getTokenStr(),
                                   styles, styleStart, isBold, isItalic, isCode);
                break;
            default:
                throw new SIllegalArgumentException("Invalid token in Markdown text!")
                          .add("token", tok.m_ttype);
            }
        }

        return sb;
    }

    /**
     * '*' and '_' are both used for italic and bold tokens, but have slightly
     * different behavior. '*' may be specified in the middle of word, while '_'
     * should have whitespace before (start) or after (end). Example:
     *  
     * a*b*c  - b is written in bold
     * a_b_c  - result is a_b_c 
     * 
     * @return
     */
    public MarkdownToken nextToken() {
        
        MarkdownToken token = null;
        
        for (; m_srcTxtIdx < m_text.length() + 1; m_srcTxtIdx++) {
            ch1 = ch2;
            ch2 = ch3;
            ch3 = ch4;
            
            if (m_srcTxtIdx < m_text.length()) {
                ch4 = m_text.charAt(m_srcTxtIdx);
            } else {
                ch4 = ' ';  // simulate additional space to enable processing of __ at end 
            }
            
//            if (m_srcTxtIdx + 1 < m_text.length()) {
//                ch2 = m_text.charAt(m_srcTxtIdx + 1);
//            }
//            if (m_srcTxtIdx + 2 < m_text.length()) {
//                ch3 = m_text.charAt(m_srcTxtIdx + 2);
//            }
//            if (m_srcTxtIdx + 3 < m_text.length()) {
//                ch4 = m_text.charAt(m_srcTxtIdx + 3);
//            }
            
            // All special markdown characters are ignored inside code area, 
            // check only for end of code area
            if (m_isInCodeArea) {
                if (ch3 == '`') {
                    token = new MarkdownToken(m_srcTxtIdx - 1, EMarkdownToken.CODE_E);
                    m_isInCodeArea = false;
                    m_srcTxtIdx++; // inc idx, otherwise we repeat the same char next time
                    return token;
                }
                continue;
            }

            // handle 2 character tokens first
            if (!isWs(ch1)  &&  ch2 == '*'  &&  ch3 == '*'  &&  !isWs(ch4)) {
                // '**' in the middle of string (xx**xx) can be either start or end - it toggles current state
                token = new MarkdownToken(m_srcTxtIdx - 2, 
                    m_isInBoldArea ? EMarkdownToken.BOLD_ASTERISK_E : EMarkdownToken.BOLD_ASTERISK_S);
                m_isInBoldArea = !m_isInBoldArea;

            } else if (!isWs(ch1)  &&  ch2 == '*'  &&  ch3 == '*') {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.BOLD_ASTERISK_E);
                m_isInBoldArea = false;
                
            } else if (ch2 == '*'  &&  ch3 == '*'  &&  !isWs(ch4)) {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.BOLD_ASTERISK_S);
                m_isInBoldArea = true;
                
            } else if (!isWs(ch1)  &&  ch2 == '_'  &&  ch3 == '_'  &&  isWsOrSym(ch4)) {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.BOLD_UNDERSCORE_E);
                
            // underscore has a bit stricter rule - it must be preceeded by ws for start and followed by ws for end
            } else if (isWsOrSym(ch1)  &&  ch2 == '_'  &&  ch3 == '_'  &&  !isWs(ch4)) {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.BOLD_UNDERSCORE_S);
                
            } else if (ch2 == '\\'  &&  ch3 == '*') {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.ESCAPE_ASTERISK);
                
            } else if (ch2 == '\\'  &&  ch3 == '_') {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.ESCAPE_UNDERSCORE);
                
            } else if (ch2 == '\\'  &&  ch3 == '`') {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.ESCAPE_CODE);
                
            } else if (ch2 == '\\'  &&  ch3 == '\\') {
                token = new MarkdownToken(m_srcTxtIdx - 2, EMarkdownToken.ESCAPE_BACKSLASH);
                ch3 = 'a'; // should be non-whitespace char, but not '\', because it is part of
                           // previous token
                
            // single character tokens
            } else if (!isWs(ch2)  &&  ch2 != '*'  &&  ch3 == '*'  &&  !isWs(ch4)  &&  ch4 != '*') {
                // '*' in the middle of string (xx*xx) can be either start or end - it toggles current state 
                token = new MarkdownToken(m_srcTxtIdx - 1, 
                    m_isInItalicArea ? EMarkdownToken.ITALIC_ASTERISK_E : EMarkdownToken.ITALIC_ASTERISK_S);
                m_isInItalicArea = !m_isInItalicArea;
                
            } else if (!isWs(ch2)  &&  ch2 != '*'  &&  ch3 == '*'  &&  ch4 != '*') {
                token = new MarkdownToken(m_srcTxtIdx - 1, EMarkdownToken.ITALIC_ASTERISK_E);
                m_isInItalicArea = false;

            } else if (ch3 == '*'  &&  !isWs(ch4)  &&  ch4 != '*') {
                token = new MarkdownToken(m_srcTxtIdx - 1, EMarkdownToken.ITALIC_ASTERISK_S);
                m_isInItalicArea = true;

            } else if (!isWs(ch2)  &&  ch2 != '_'  &&  ch3 == '_'  && isWsOrSym(ch4)) {
                token = new MarkdownToken(m_srcTxtIdx - 1, EMarkdownToken.ITALIC_UNDERSCORE_E);

            } else if (isWsOrSym(ch2)  &&  ch3 == '_'  &&  !isWs(ch4)  &&  ch4 != '_') {
                token = new MarkdownToken(m_srcTxtIdx - 1, EMarkdownToken.ITALIC_UNDERSCORE_S);

            } else if (ch3 == '`') {   // no requirement for non-whitespace around
                token = new MarkdownToken(m_srcTxtIdx - 1, 
                                          m_isInCodeArea ? EMarkdownToken.CODE_E : EMarkdownToken.CODE_S);
                m_isInCodeArea = !m_isInCodeArea;
            } 
            
            if (token != null) {
                m_srcTxtIdx++; // inc idx, otherwise we repeat the same char next time
                return token;
            }
        }
        
        return null; // end of text reached 
    }
    
    
    private StyleRange getStyle(int styleStart, int length, boolean isBold, boolean isItalic, boolean isCode) {

        StyleRange styleRange;
        if (isCode) {
            Font font = FontProvider.instance().getFixedWidthControlFont(m_controlForFont, 
                                                                         isBold, 
                                                                         isItalic);
            styleRange = new StyleRange(styleStart, length, null, null);
            styleRange.font = font;
        } else {
            int fontStyle = isBold ? SWT.BOLD : 0;
            fontStyle |= isBold ? SWT.BOLD : 0;
            fontStyle |= isItalic ? SWT.ITALIC : 0;
        
            styleRange = new StyleRange(styleStart, length, null, null, fontStyle);
        }
        
        return styleRange;
    }


    private boolean isWs(char ch) {
        // '\' is not ws, but it should either not be treated 
        return ch == ' '  ||  ch == '\n'  || ch == '\t';
    }

    
    private boolean isWsOrSym(char ch) {
        // Not a letter or digit or '_' as it is part of token.
        return !(Character.isAlphabetic(ch)  ||  Character.isDigit(ch)  
                 ||  ch =='_'  ||  ch == '\\');
    }

    
    public static String escapeMarkdownChars(String str) {
        StringBuilder sb = new StringBuilder();
        
        for (int idx = 0; idx < str.length(); idx++) {
            char ch = str.charAt(idx);
            if (ch == '*'  ||  ch == '_'  ||  ch == '`') {
                sb.append('\\');
            }
            sb.append(ch);
        }
        
        return sb.toString();
        
    }
    
    
    /**
     * Removes tokens without pairs. 
     * @param tokens
     */
    private void normalizeTokens(List<MarkdownToken> tokens) {
        Stack<MarkdownToken> boldAster = new Stack<>();
        Stack<MarkdownToken> boldUnder = new Stack<>();
        Stack<MarkdownToken> italicAster = new Stack<>();
        Stack<MarkdownToken> italicUnder = new Stack<>();
        Stack<MarkdownToken> code = new Stack<>();
        
        for (MarkdownToken token : tokens) {
            switch (token.m_ttype) {
            case BOLD_ASTERISK_S:
                boldAster.push(token);
                break;
            case BOLD_ASTERISK_E:
                matchPairOnStack(boldAster, EMarkdownToken.BOLD_ASTERISK_S, token);
                break;
            case BOLD_UNDERSCORE_S:
                boldUnder.push(token);
                break;
            case BOLD_UNDERSCORE_E:
                matchPairOnStack(boldUnder, EMarkdownToken.BOLD_UNDERSCORE_S, token);
                break;
            case ITALIC_ASTERISK_S:
                italicAster.push(token);
                break;
            case ITALIC_ASTERISK_E:
                matchPairOnStack(italicAster, EMarkdownToken.ITALIC_ASTERISK_S, token);
                break;
            case ITALIC_UNDERSCORE_S:
                italicUnder.push(token);
                break;
            case ITALIC_UNDERSCORE_E:
                matchPairOnStack(italicUnder, EMarkdownToken.ITALIC_UNDERSCORE_S, token);
                break;
            case CODE_S:
                code.push(token);
                break;
            case CODE_E:
                matchPairOnStack(code, EMarkdownToken.CODE_S, token);
                break;
                // ignore tokens without pairs
            case TEXT:
            case ESCAPE_ASTERISK:
            case ESCAPE_CODE:
            case ESCAPE_UNDERSCORE:
            default:
                break;
            }
        }
            
        markTokensAsIgnored(boldAster);
        markTokensAsIgnored(boldUnder);
        markTokensAsIgnored(italicAster);
        markTokensAsIgnored(italicUnder);
        markTokensAsIgnored(code);
    }


    private void markTokensAsIgnored(Stack<MarkdownToken> tokenStack) {
        for (MarkdownToken token : tokenStack) {
            token.setIgnored(true);
        }
        
    }


    private void matchPairOnStack(Stack<MarkdownToken> stack, 
                                    EMarkdownToken tokenType, 
                                    MarkdownToken token) {
        if (!stack.isEmpty()  &&  stack.peek().m_ttype == tokenType) {
            stack.pop();
        } else {
            stack.push(token); // end token without start token
        }
    }


    private void addTextToken(List<MarkdownToken> tokens, int startIdx, int pos) {
        if (pos > startIdx) {
            tokens.add(new MarkdownToken(startIdx, pos - startIdx)); // text token
        }
    }
    
    
    private void  appendTextAndStyle(StringBuilder sb, String text, 
                                     List<StyleRange> styles, 
                                     int styleStart,
                                     boolean isBold, boolean isItalic, boolean isCode) {
        sb.append(text);
        if (isBold  || isItalic  ||  isCode) {
            styles.add(getStyle(styleStart, sb.length() - styleStart, 
                                isBold, isItalic, isCode));
        }
    }
}
