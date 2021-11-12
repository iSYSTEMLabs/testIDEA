package si.isystem.tbltableeditor;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.connect.ETristate;

/**
 * Renders tristate cell with states default, false and true. The 'default' value
 * is rendered as grayed false or true, depending on the default value. 
 *
 * <p>
 * It accepts the following style bits:
 * <ul>
 * <li><b>INDICATION_CLICK</b> shows a visible feedback when the
 * user clicks the cell. This feedback respects the checked state.</li>
 * <li><b>INDICATION_FOCUS</b> causes the cell that has focus to be 
 * drawn with another background color and a focus-style border.</li>
 * <li><b>INDICATION_FOCUS_ROW</b> causes the cell that has focus to be 
 * drawn in a dark color and its content bright.</li>
 * <li><b>INDICATION_COMMENT</b> makes the renderer paint a little
 * triangle in the upper right corner of the cell as a sign that 
 * additional information is available.</li>
 * <li><b>SIGN_IMAGE</b> forces the drawing of images that are defined in 
 * the icon folder. This makes the rendering of a cell 2-3 times slower. Overwrites
 * all other SIGN_* style bits.</li>
 * <li><b>SIGN_X</b> makes the 'true' symbol be an X. This is only valid if SIGN_IMAGE
 * is not given, and it overwrites SIGN_CHECK</li>
 * <li><b>SIGN_CHECK</b> makes the 'true' symbol a check. THIS IS DEFAULT.</li>
 * </ul>
 * 
 * @see de.kupzog.ktable.editors.KTableCellEditorCheckbox
 * @see de.kupzog.ktable.editors.KTableCellEditorCheckbox2
 * 
 * @author Lorenz Maierhofer <lorenz.maierhofer@logicmindguide.com>, markok
 *
 */
public class TristateCellRenderer extends DefaultCellRenderer {

    /** 
     * Style bit that forces that the renderer paints images 
     * instead of directly painting. The images used for painting
     * can be found in the folder /icons/ and are named 
     * checked.gif, unchecked.gif, checked_clicked.gif and unchecked_clicked.gif.
     * <p>
     * Note that when using images, drawing is 2-3 times slower than when 
     * using direct painting. It might be visible if many cells should be 
     * rendered that way. So this is not default.
     */ 
    public static final int SIGN_IMAGE = 1<<31;

    /**
     * Makes the renderer draw an X as the symbol that signals the value
     * is true. This has only an effect if the style SIGN_IMAGE is not active.
     */
    public static final int SIGN_X = 1<<30;
    /**
     * Makes the renterer draw a check sign as the symbol that signals the value
     * true. THIS IS DEFAULT.
     */
    public static final int SIGN_CHECK = 1<<29;

    /** Indicator for a checked entry / true boolean decision */
    public static final Image IMAGE_CHECKED = SWTX.loadImageResource(Display.getCurrent(), 
            "/icons/checked.gif");

    /** Indicator for an unchecked entry / false boolean decision */
    public static final Image IMAGE_UNCHECKED = SWTX.loadImageResource(Display.getCurrent(), 
            "/icons/unchecked.gif");

    /** Indicator for an checked entry / true boolean decision that is currently clicked.*/
    public static final Image IMAGE_CHECKED_CLICKED = SWTX.loadImageResource(Display.getCurrent(), 
            "/icons/checked_clicked.gif");

    /** Indicator for an unchecked entry / false boolean decision that is currently clicked.*/
    public static final Image IMAGE_UNCHECKED_CLICKED = SWTX.loadImageResource(Display.getCurrent(), 
            "/icons/unchecked_clicked.gif");

    public static final Color COLOR_FILL = new Color(Display.getDefault(), 206, 206, 206);
    public static final Color BORDER_DARK = new Color(Display.getDefault(), 90, 90, 57);
    public static final Color BORDER_LIGHT = new Color(Display.getDefault(), 156, 156, 123);
    public static final Color DEFAULT_STATE_COLOR = new Color(Display.getDefault(), 0xb0, 0xb0, 0xb0);
    public static final Color TRUE_STATE_COLOR = new Color(Display.getDefault(), 0x0, 0x78, 0x0);
    public static final Color FALSE_STATE_COLOR = new Color(Display.getDefault(), 0x78, 0x0, 0x0);

    /**
     * Creates a cell renderer that shows tristate values with the given style.<p>
     * @param style 
     * Honored style bits are:<br>
     * - INDICATION_CLICKED<br>
     * - INDICATION_FOCUS<br>
     * - INDICATION_FOCUS_ROW<br>
     * - INDICATION_COMMENT<p>
     * Styles that influence the sign painted when cell value is true:<br>
     * - SIGN_IMAGE<br>
     * - SIGN_X<br>
     * - SIGN_CHECK (default)<br>
     */
    public TristateCellRenderer(int style) {
        super(style);
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableCellRenderer#getOptimalWidth(org.eclipse.swt.graphics.GC, int, int, java.lang.Object, boolean)
     */
    @Override
    public int getOptimalWidth(GC gc, int col, int row, Object content, boolean fixed, KTableModel model) {
        return IMAGE_CHECKED.getBounds().x + 6;
    }

    /** 
     * Paint a box with or without a checked symbol.
     * 
     * @see de.kupzog.ktable.KTableCellRenderer#drawCell(GC, Rectangle, int, int, Object, boolean, boolean, boolean, KTableModel)
     */
    @Override
    public void drawCell(GC gc, 
                         Rectangle rect, 
                         int col, 
                         int row, 
                         Object content, 
                         boolean focus, 
                         boolean fixed, 
                         boolean clicked, 
                         KTableModel model) {

        // draw focus sign:
        if (focus && (m_Style & INDICATION_FOCUS) != 0) {
            rect = drawDefaultSolidCellLine(gc, rect, COLOR_LINE_LIGHTGRAY, COLOR_LINE_LIGHTGRAY);
            drawCheckableImage(gc, rect, content, COLOR_BGFOCUS, clicked);
            gc.drawFocus(rect.x, rect.y, rect.width, rect.height);

        } else if (focus && (m_Style & INDICATION_FOCUS_ROW) != 0) {

            rect = drawDefaultSolidCellLine(gc, rect, COLOR_BGROWFOCUS, COLOR_BGROWFOCUS);
            drawCheckableImage(gc, rect, content, COLOR_BGROWFOCUS, clicked);

        } else {
            rect = drawDefaultSolidCellLine(gc, rect, COLOR_LINE_LIGHTGRAY, COLOR_LINE_LIGHTGRAY);

            Color bkgColor = m_isEnabled ? getBackground() : COLOR_DISABLED_BACKGROUND; 

            if (content instanceof TextIconsContent) {
                TextIconsContent tiContent = (TextIconsContent)content;
                if (tiContent.getBackground() != null) {
                    bkgColor = tiContent.getBackground();
                } else if (m_isEnabled  &&  !tiContent.isEditable()) {
                    bkgColor = COLOR_LINE_LIGHTGRAY;
                }
            }
            
            drawCheckableImage(gc, rect, content, bkgColor, clicked);
        }

        if ((m_Style & INDICATION_COMMENT) != 0) {
            drawCommentSign(gc, rect);
        }
        
        if (content instanceof TextIconsContent) {
            TextIconsCellRenderer.drawIcons(gc, (TextIconsContent)content, 
                                            rect, m_isEnabled);
        }
    }

    /**
     * This method is responsible for calling the actual paint method.
     * Note that there currently exist two versions: 
     * One that uses the images defined in this renderer, and anotherone
     * painting directly.<p> 
     * NOTE: Default is drawing directly (no images used) because this seems
     * to be at least 2-3 times faster than painting an image! (tested: WinXP)
     */
    protected void drawCheckableImage(GC gc, Rectangle rect, Object content, Color bgColor, boolean clicked) {

        if (!(content instanceof TextIconsContent)) {
            drawCellContent(gc, rect, "?", null, getForeground(), bgColor);
            return;
        }

        TextIconsContent value = (TextIconsContent)content;
        ETristate state = ETristate.valueOf(value.getTristateValue());
        boolean isDefaultTrue = value.getDefaultForTristate();
        boolean checked = (state == ETristate.E_TRUE)  ||  
                          (state == ETristate.E_DEFAULT  &&  isDefaultTrue);
        boolean isDrawGreyed = state == ETristate.E_DEFAULT;

        // draw content as image:
        if ((m_Style & SIGN_IMAGE)!=0) {
            if (checked) {
                if (clicked && (m_Style & INDICATION_CLICKED)!=0)
                    drawImage(gc, rect, IMAGE_CHECKED_CLICKED, bgColor);
                else
                    drawImage(gc, rect, IMAGE_CHECKED, bgColor);
            }
            else {
                if (clicked && (m_Style & INDICATION_CLICKED)!=0)
                    drawImage(gc, rect, IMAGE_UNCHECKED_CLICKED, bgColor);
                else
                    drawImage(gc, rect, IMAGE_UNCHECKED, bgColor);
            }
        }
        else { // draw image directly:
            if (clicked && (m_Style & INDICATION_CLICKED)!=0) {
                drawCheckedSymbol(gc, rect, checked, bgColor, COLOR_FILL, isDrawGreyed);
            } else {
                drawCheckedSymbol(gc, rect, checked, bgColor, bgColor, isDrawGreyed);
            }
        }
    }

    /**
     * 
     * @param gc
     * @param rect
     * @param image
     * @param backgroundColor
     */
    protected void drawImage(GC gc, Rectangle rect, Image image, Color backgroundColor) {
        gc.setBackground(backgroundColor);
        gc.setForeground(backgroundColor);
        gc.fillRectangle(rect);
        SWTX.drawTextImage(gc, "", getAlignment(), 
                           image, getAlignment(), rect.x + 3, rect.y,
                           rect.width - 3, rect.height);
    }

    /**
     * @param value If true, the comment sign is painted. 
     * Else it is omitted.
     */
    public void setCommentIndication(boolean value) {
        if (value)
            m_Style = m_Style | INDICATION_COMMENT;
        else 
            m_Style = m_Style & ~INDICATION_COMMENT;
    }
    /**
     * Manually paints the checked or unchecked symbol. This provides a fast replacement
     * for the variant that paints the images defined in this class. <p>
     * The reason for this is that painting manually is 2-3 times faster than painting the
     * image - which is very notable if you have a completely filled table! (see example!)
     * @param gc The GC to use when dawing
     * @param rect The cell ara where the symbol should be painted into.
     * @param checked Wether the symbol should be the checked or unchecked
     * @param bgColor The background color of the cell.
     * @param fillColor The color of the box drawn (with of without checked mark). Used
     * when a click indication is desired.
     */
    protected void drawCheckedSymbol(GC gc, Rectangle rect, boolean checked, 
                                     Color bgColor, Color fillColor, boolean isDrawGreyed) {
        // clear background:
        gc.setBackground(bgColor);
        gc.fillRectangle(rect);

        //paint rectangle: 
        Rectangle bound = getAlignedLocation(rect, IMAGE_CHECKED);

        gc.setForeground(BORDER_LIGHT);
        gc.drawLine(bound.x, bound.y, bound.x+bound.width, bound.y);
        gc.drawLine(bound.x, bound.y, bound.x, bound.y+bound.height);
        gc.setForeground(BORDER_DARK);
        gc.drawLine(bound.x+bound.width, bound.y+1, bound.x+bound.width, bound.y+bound.height-1);
        gc.drawLine(bound.x, bound.y+bound.height, bound.x+bound.width, bound.y+bound.height);

        if (!bgColor.equals(fillColor)) {
            gc.setBackground(fillColor);
            gc.fillRectangle(bound.x+1, bound.y+1, bound.width-1, bound.height-1);
        }

        if (checked) { // draw a check symbol:            
            drawCheckSymbol(gc, bound, isDrawGreyed);
        } else {
            drawUncheckedSymbol(gc, bound, isDrawGreyed);
        }                
    }


    /**
     * Draws a X as a sign that the cell value is true.
     * @param gc The gc to use when painting
     * @param bound
     */
    private void drawCheckSymbol(GC gc, Rectangle bound, boolean isDrawGreyed) {
        if ((m_Style & SIGN_X)!=0) { // Draw a X
            gc.setForeground(BORDER_LIGHT);

            gc.drawLine(bound.x+3, bound.y+2, bound.x-2+bound.width, bound.y-3+bound.height);
            gc.drawLine(bound.x+2, bound.y+3, bound.x-3+bound.width, bound.y-2+bound.height);

            gc.drawLine(bound.x+3, bound.y-2+bound.height, bound.x-2+bound.width, bound.y+3);
            gc.drawLine(bound.x+2, bound.y-3+bound.height, bound.x-3+bound.width, bound.y+2);

            gc.setForeground(COLOR_TEXT);

            gc.drawLine(bound.x+2, bound.y+2, bound.x-2+bound.width, bound.y-2+bound.height);
            gc.drawLine(bound.x+2, bound.y-2+bound.height, bound.x-2+bound.width, bound.y+2);
        } else { // Draw a check sign
            if (isDrawGreyed) {
                gc.setForeground(DEFAULT_STATE_COLOR);
            } else {
                gc.setForeground(TRUE_STATE_COLOR);
            }

            // draw left line of check-mark
            gc.drawLine(bound.x+2, bound.y+bound.height-4, bound.x+4, bound.y+bound.height-2);
            gc.drawLine(bound.x+2, bound.y+bound.height-5, bound.x+5, bound.y+bound.height-3);
            gc.drawLine(bound.x+2, bound.y+bound.height-6, bound.x+4, bound.y+bound.height-4);

            // draw right line of check-mark
            for (int i = 1; i < 4; i++) {
                gc.drawLine(bound.x+2+i, bound.y+bound.height-3, bound.x+bound.width-2, bound.y+1+i);
            }
        }
    }

    
    private void drawUncheckedSymbol(GC gc, Rectangle bound, boolean isDrawGreyed) {
        if (isDrawGreyed) {
            gc.setForeground(DEFAULT_STATE_COLOR);
        } else {
            gc.setForeground(FALSE_STATE_COLOR);
        }


        for (int i = 1; i < 3; i++) {
            gc.drawLine(bound.x + 2, 
                        bound.y + bound.height/2 -2 + i, 
                        bound.x+bound.width-2, 
                        bound.y + bound.height/2 -2 + i);
        }
    }

    
    /**
     * Returns the location where the checked symbol should be painted.<p>
     * Note that this is only a subarea of the area covered by an image, since
     * the image contains a border area that is not needed here.
     * @param rect The cell area
     * @param img The image to take the size of the checked symbol from.
     * @return Returns the area that should be filled with a checked/unchecked symbol.
     */
    protected Rectangle getAlignedLocation(Rectangle rect, Image img) {
        
        Rectangle bounds = img.getBounds();
        bounds.x -= 2;
        bounds.y -= 2;
        bounds.height -= 4;
        bounds.width -= 4;

        if ((getAlignment() & SWTX.ALIGN_HORIZONTAL_CENTER) != 0) {
            bounds.x = rect.x + (rect.width - bounds.width)/2;
        } else if ((getAlignment() & SWTX.ALIGN_HORIZONTAL_RIGHT)!=0) {
            bounds.x = rect.x + rect.width - bounds.width-2;
        } else {
            bounds.x = rect.x+2;
        }
        
        if ((getAlignment() & SWTX.ALIGN_VERTICAL_CENTER)!=0) {
            bounds.y = rect.y+(rect.height-bounds.height)/2;
        } else if ((getAlignment() & SWTX.ALIGN_VERTICAL_BOTTOM)!=0) {
            bounds.y = rect.y+rect.height-bounds.height-2;
        } else {
            bounds.y = rect.y+2;
        }

        return bounds;
    }
}

