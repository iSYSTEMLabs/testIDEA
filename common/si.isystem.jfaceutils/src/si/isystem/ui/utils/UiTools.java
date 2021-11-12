package si.isystem.ui.utils;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * This class contains static methods which are used from several other classes,
 * and are not tied to any internal state.
 *  
 * @author markok
 *
 */
public class UiTools {
    
    public static final int DEFAULT_TOOLTIP_DELAY = 15000; // 15 seconds
    
    
    /**
     * This method returns index of the start of the last word. Word is a string
     * of identifier characters, for example the string "one,_two three"
     * contains words 'one', '_two', and 'three'. This method was implemented to 
     * be used for auto-completion in multi-item text fields. 
     * 
     * @param contents string to be searched
     * 
     * @return index of the first character of the identifier before 'position',
     * or contents.size() if there is no word before 'position' 
     */
    public static int getStartOfLastWord(String contents) {
        return getStartOfLastWord(contents, contents.length());
    }


    /**
     * This method returns index of the start of the word before pos. Word is a string
     * of identifier characters, for example the string "one,_two three"
     * contains words 'one', '_two', and 'three'. This method was implemented to 
     * be used for auto-completion in multi-item text fields. 
     * 
     * @param contents string to be searched
     * @param pos position in string after the character to start searching 
     *            backwards (usually the cursor position).
     * 
     * @return index of the first character of the identifier before 'position',
     * or contents.size() if there is no word before 'position' 
     */
    public static int getStartOfLastWord(String contents, int pos) {
        
        for (int lastTagStartIdx = pos - 1; lastTagStartIdx >= 0; --lastTagStartIdx) {
            
            if (!Character.isJavaIdentifierPart(contents.charAt(lastTagStartIdx)) &&  
                    contents.charAt(lastTagStartIdx) != '{') {  // for host vars
                
                return lastTagStartIdx + 1; // move to the first non-separator char or break;
            }
            
            // '$' may only be the first char in host var name
            if (contents.charAt(lastTagStartIdx) == '$') {
                return lastTagStartIdx;
            }
            
        }
        
        return 0;
    }


    /**
     * Sets tooltip for the given control. These tooltips are JFace specific and
     * have configurable visibility time, in contrast to tooltips created 
     * with Control.setTooltip(), which are OS tooltips and are hidden so quickly,
     * that users can't read longer descriptions in tooltips.     *  
     * 
     * @param control control to receive the tooltip
     * @param text tooltip text
     */
    public static void setToolTip(Control control, String text) {
        DefaultToolTip tooltip = new DefaultToolTip(control);
        tooltip.setText(text);
        tooltip.setShift(new Point(10, 10));
        tooltip.setHideDelay(UiTools.DEFAULT_TOOLTIP_DELAY);
    }
    
    /**
     * Places the child shell on the center of the parent and leaving its size 
     * as-is.
     * @param parent
     * @param child
     */
    public static void placeShellOverShell(Shell parent, Shell child)
    {
        placeShellOverShell(parent, child, null);
    }

    /**
     * Places the child shell on the center of the parent and resizes it to the specified size.
     * 
     * @param parent
     * @param child
     * @param size
     */
    public static void placeShellOverShell(Shell parent, Shell child, Point size) {
        if (size == null) {
            size = child.getSize();
        }
        
        Point activeShellSize, activeShellLocation;
        // If parent is null the just take the whole display bounds
        if (parent != null) {
            activeShellSize = parent.getSize();
            activeShellLocation = parent.getLocation();
        }
        else {
            Rectangle b = Display.getDefault().getBounds();
            activeShellSize = new Point(b.width, b.height);
            activeShellLocation = new Point(b.x, b.y);
        }
        
        int cx = activeShellLocation.x + activeShellSize.x / 2;
        int cy = activeShellLocation.y + activeShellSize.y / 2;
        Point location = new Point(cx - size.x / 2, cy - size.y / 2);

        child.setSize(size);
        child.setLocation(location);
    }
}
