package si.isystem.ui.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * This singleton class provides fonts used in other parts of an application. It keeps
 * references to created fonts and disposes them when <code>dispose()</code>
 * is called. Typically fonts created here exist for the entire application 
 * lifetime, so disposing should be done just before application exits.
 * 
 * @author markok
 *
 */
public class FontProvider {

    private Font m_boldControlFont;
    private Font m_italicControlFont;
    private Font m_fixedWidthControlFont;
    private Font m_fixedWidthControlFontBold;
    private Font m_fixedWidthControlFontItalic;
    private Font m_fixedWidthControlFontBoldItalic;
    private final static FontProvider INSTANCE = new FontProvider();

    
    private FontProvider()
    {}
    
    
    /** Use this method to get a single instance of this class. */
    public static FontProvider instance() {
        return INSTANCE;
    }
    
    
    /**
     * Returns new font which gets properties from font used in the given 
     * control, but its style is set to bold.
     *  
     * @param control control, which contains font to be used as a template.
     * 
     * @return original font, but style set to SWT.BOLD.
     */
    public Font getBoldControlFont(Control control) {
        if (m_boldControlFont == null) {
            FontData[] fontData = control.getFont().getFontData();
            if (fontData.length > 0) {
                fontData[0].setStyle(SWT.BOLD);
                m_boldControlFont = new Font(Display.getDefault(), fontData);
            } else {
                throw new IllegalStateException("No font returned by control!");
            }
        }
        
        return m_boldControlFont;        
    }
    
    
    /**
     * Returns new font which gets properties from font used in the given 
     * control, but its style is set to italic.
     *  
     * @param control control, which contains font to be used as a template.
     * 
     * @return original font, but style set to SWT.ITALIC.
     */
    public Font getItalicControlFont(Control control) {
        if (m_italicControlFont == null) {
            FontData[] fontData = control.getFont().getFontData();
            if (fontData.length > 0) {
                fontData[0].setStyle(SWT.ITALIC);
                m_italicControlFont = new Font(Display.getDefault(), fontData);
            } else {
                throw new IllegalStateException("No font returned by control!");
            }
        }
        
        return m_italicControlFont;        
    }
    
    
    /**
     * Returns new font which gets properties from font used in the given 
     * control, but its name is set to 'courier'.
     *  
     * @param control control, which contains font to be used as a template.
     * 
     * @return original font, but style set to SWT.ITALIC.
     */
    public Font getFixedWidthControlFont(Control control) {
        return getFixedWidthControlFont(control, false, false);        
    }

    
    public Font getFixedWidthControlFont(Control control, boolean isBold, boolean isItalic) {

        // if font is created, return it immediately
        Font retFont = null;
        if (isBold) {
            if (isItalic) {
                retFont = m_fixedWidthControlFontBoldItalic;
            } else {
                retFont = m_fixedWidthControlFontBold;
            }
        } else if(isItalic) {
            retFont = m_fixedWidthControlFontItalic;
        } else {
            retFont = m_fixedWidthControlFont;
        }
        
        if (retFont != null) {
            return retFont;
        }
        
        if (control == null) { 
            return null; // used during unit testing, when there are no controls available
        }
        
        FontData[] fontData = control.getFont().getFontData();
        if (fontData.length > 0) {
            int fontStyle = 0;
            if (isBold || isItalic) {
                fontStyle |= isBold ? SWT.BOLD : 0;
                fontStyle |= isItalic ? SWT.ITALIC : 0;
            } else {
                fontStyle = SWT.NORMAL;
            }
            fontData[0].setStyle(fontStyle);
            fontData[0].setName("courier");
        } else {
            throw new IllegalStateException("No font returned by control!");
        }

        if (isBold) {
            if (isItalic) {
                if (m_fixedWidthControlFontBoldItalic == null) {
                    m_fixedWidthControlFontBoldItalic = new Font(Display.getDefault(), fontData);
                }
                return m_fixedWidthControlFontBoldItalic;
            } else {
                if (m_fixedWidthControlFontBold == null) {
                    m_fixedWidthControlFontBold = new Font(Display.getDefault(), fontData);
                }
                return m_fixedWidthControlFontBold;
            }
        } else if(isItalic) {
            if (m_fixedWidthControlFontItalic == null) {
                m_fixedWidthControlFontItalic = new Font(Display.getDefault(), fontData);
            }
            return m_fixedWidthControlFontItalic;
        } else {
            if (m_fixedWidthControlFont == null) {
                m_fixedWidthControlFont = new Font(Display.getDefault(), fontData);
            }
        
            return m_fixedWidthControlFont;
        }
    }

    
    /**
     * Returns height of default font in pixels. This font is used for widgets 
     * with default font.
     * 
     * If widget like <i>Label</i> is available and specific text extents are
     * required, use the code:
     * <pre>   
     *   System.out.println("height1 = " + new GC(label).textExtent("Hj").x);
     * </pre>   
     * or
     * <pre>   
     *   label.getFont().getFontData()[0].getHeight() * device.getDPI().x / 72);
     * </pre>   
     */
    public int getDefaultFontHeight(Shell shell) {
        
        Device device = shell.getDisplay();
        return device.getSystemFont().getFontData()[0].getHeight() * device.getDPI().x / 72;
    }
    

    /**
     * Returns default row height to be used in a table.
     * @param shell
     */
    public int getDefaultTableRowHeight(Shell shell) {
        int fontHeight = getDefaultFontHeight(shell);
        return (int)(fontHeight * 1.3 + 6);
    }
    
    
    /** 
     * Returns width of text in pixels. 
     * WARNING: this method creates GC for each call - use 
     * override <code>getTextWidth(GC, text)</code>
     * for frequent calls, for example all cells in a table. It may be up to 50% faster.
     */
    public int getTextWidth(Control control, String text) {
        
        if (text == null) {
            return 0;
        }
        
        GC gc = new GC(control);
        int w = gc.textExtent(text).x;
        gc.dispose();
        return w;
    }
    

    /** 
     * Returns width of text in pixels. Since <i>gc</i> given as parameter is used, 
     * it is faster then  <code>getTextWidth(Control, String )</code>. If you have
     * widget available, you can create <i>gc</i> as follows:
     * <pre>
     *   GC gc = new GC(control);
     * </pre>
     */ 
    public int getTextWidth(GC gc, String text) {
        
        if (text == null) {
            return 0;
        }
        
        return gc.textExtent(text).x;
    }
    

    /**
     * Disposes all created fonts. Call this method when fonts are no longer 
     * needed, or at application exit.
     */
    public void dispose() {
        
        if (m_boldControlFont != null) {
            m_boldControlFont.dispose();
        }
        
        if (m_italicControlFont != null) {
            m_italicControlFont.dispose();
        }
        
        if (m_fixedWidthControlFont != null) {
            m_fixedWidthControlFont.dispose();
        }
    }
}
