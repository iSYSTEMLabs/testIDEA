package si.isystem.ui.utils;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * This class provides common colors, which are used in the application. Once 
 * the color is created, it is cached so that new resources are no used for
 * new instances of the same color. Since application uses only a few colors for 
 * UI, and uses them through all of its life time, no releasing is needed.
 * 
 * This class is a singleton - call method instance() to get an instance.
 * 
 * @author markok
 *
 */
public class ColorProvider {

    // color constants - add them as required
    public static final int BLACK = 0x000000;
    public static final int LIGHT_RED = 0xffd0d0;
    public static final int RED = 0xff8080;
    public static final int GREEN = 0x00ff00;
    public static final int LIGHT_GREEN = 0xe0ffe0;
    public static final int MERGED_BKG_COLOR = 0xd1ecf1;
    public static final int STRONG_ERROR_COLOR = 0xffd0d0;
    public static final int LIGHT_ORANGE = 0xffe6af;
    public static final int DISABLED_ITEM_FG_COLOR = 0x808080;
    public static final int LIGHT_BLUE = 0xf0f0ff;
    public static final int DARK_BLUE = 0x0000d0;
    public static final int BLUE = 0x0000FF;
    public static final int BLUE_D0 = 0x00D0FF;
    public static final int DARK_DARK_BLUE = 0x000086;
    public static final int DARK_GREY = 0x404040;
    public static final int GRAY = 0x808080;
    public static final int LIGHT_GRAY = 0xd0d0d0;
    public static final int VERY_LIGHT_GRAY = 0xf8f8f8;
    public static final int LIGHT_CYAN = 0xe0ffff;
    public static final int WHITE = 0xffffff;
    
    private static ColorProvider m_instance = new ColorProvider();
    
    private final Map<Integer, Color> m_colorMap = new TreeMap<>();
    
    
    /** Returns the only instance of this class. */
    public static ColorProvider instance() {
        return m_instance;
    }
    
    
    /**
     * @return color to be used for widget background, when it displays
     * <b>information</b> text, as a result of an operation, for example result of
     * test.
     */
    public Color getInfoColor() {
        return getColor(LIGHT_BLUE);     
    }

    /**
     * @return color to be used for widget background, when it displays
     * <b>warning</b> text, as a result of an operation, for example result of
     * test.
     */
    public Color getWarningColor() {
        return getColor(LIGHT_ORANGE);     
    }

    
    /**
     * @return color to be used for widget background, when it displays
     * <b>error</b> text, as a result of an operation, for example result of
     * test.
     */
    public Color getErrorColor() {
        return getColor(LIGHT_RED);    
    }

    /**
     * @return color similar to getErrorColor(), but a bit stronger to be
     * used for critical errors.
     */
    public Color getStrongErrorColor() {
        return getColor(STRONG_ERROR_COLOR);
    }


    /**
     * @return default widget background color.
     */
    public Color getBkgColor() {
        return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);    
    }

    /**
     * @return returns default widget background color when it is not editable.
     */
    public Color getBkgNoneditableColor() {
        return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);    
    }


    /**
     * @return backgorund color used to mark selection, for example background 
     *         color of a selected table cell
     */
    public Color getSelectionColor() {
        return getColor(LIGHT_GREEN);
    }

    
    /**
     * This method creates color and stores it into cache. Do not dispose colors 
     * created by this method, because they'll get disposed by method dispose() 
     * of this class. This method should be called by GUI thread. Use it for colors, 
     * which are used often in the application and exist for longer period of time.
     * Do not misuse it for creation
     * of temporarily used colors, as application will run out of resources. 
     * 
     * @param rgb integer containing R, G, and B color components in each byte.
     * The most significant byte must be 0. 
     * @return color representing the given rgb value. 
     */
    public Color getColor(int rgb) {
        return getColor((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
    }

    
    /**
     * @return foreground color of a disabled item. 
     */
    public Color getDisabledItemFgColor() {
        return getColor(DISABLED_ITEM_FG_COLOR);
    }
    
    /**
     * This method creates color and stores it into cache. Do not dispose colors 
     * created by this method, because they'll get disposed by method dispose() 
     * of this class. This method should be called by GUI thread. Use it for colors, 
     * which are used often in the application and exist for longer period of time.
     * 
     * @param r red color component
     * @param g green color component
     * @param b blue color component
     * @return cached or new color, if it does not exist yet.
     */
    public Color getColor(int r, int g, int b) {
        if (r < 0  ||  g < 0  ||  b < 0  ||  r > 255  ||  g > 255 ||  b > 255) {
            throw new IllegalArgumentException("Color components must be in range 0..255: r = " +
            		r + ",  g = " + g + ",  b = " + b);
        }
        
        Integer colorId = Integer.valueOf(r << 16 | g << 8 | b);
        Color color = m_colorMap.get(colorId); 
        if (color != null) {
            return color;
        }
        
        color = new Color(Display.getCurrent(), r, g, b);
        m_colorMap.put(colorId, color);
        return color;
    }
    
    /**
     * Just an utility that uses the SWT RGB as the color parameter.
     * @param rgb
     * @return
     */
    public Color getColor(RGB rgb) {
        return getColor(rgb.red, rgb.green, rgb.blue);
    }
    
    /** This method should be called, when cached colors are no longer used. */
    public void dispose() {
        for (Color color : m_colorMap.values()) {
            color.dispose();
        }
        m_colorMap.clear();
    }

}
