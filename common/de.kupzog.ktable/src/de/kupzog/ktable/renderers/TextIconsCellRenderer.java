package de.kupzog.ktable.renderers;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.renderers.TextIconsContent.EEditorType;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;

/**
 * Renders text so that there is still space for icons on the left and right 
 * edge of the cell.
 * 
 * @author markok
 *
 */
public class TextIconsCellRenderer extends DefaultCellRenderer {

    protected Display m_Display;
    private final boolean m_isHeaderCell;
    private boolean m_isEditable;

    private static final int ICON_BORDER = 2;
    private static final int OVERLAY_ICON_W = 7;    // width of overlay icon
    public static final int ICON_SPACE = ICON_BORDER + OVERLAY_ICON_W + ICON_BORDER;
    
    // public static final int ICON_LR_SPACE = ICON_LEFT_SPACE + ICON_RIGHT_SPACE;
    public static final int DIST_TO_BORDER = 2;

    public static final Color COMBO_DEFAULT_ITEM_FG_COLOR = new Color(Display.getDefault(), 150, 150, 150);

    // how many pixels are used to detect column resize operation 
    public static final int RESIZE_COLUMN_AREA = 2;

    /**
     * 
     * @param style SWT.NONE or DefaultCellRenderer.INDICATION_FOCUS if you want 
     *              cell to indicate focus (recommended)
     * @param isHeaderCell set it to true, if this is cell in table header.
     */
    public TextIconsCellRenderer(int style, boolean isHeaderCell) 
    {
        super(style);
        m_isHeaderCell = isHeaderCell;
        m_Display = Display.getCurrent();
    }

    
    @Override
    public int getOptimalWidth(GC gc, 
                               int col, 
                               int row, 
                               Object content, 
                               boolean fixed, 
                               KTableModel model)
    {
        return super.getOptimalWidth(gc, col, row, content, fixed, model) + 
               getIconsSpace(content);
    }

    
    private int getLeftIconsSpace(Object content) {
        int width = ICON_SPACE;
        
        if (content instanceof TextIconsContent) {
            width = ((TextIconsContent)content).getNoOfLeftIcons() * ICON_SPACE;
        }
        
        return width;
    }
    
    
    private int getRightIconsSpace(Object content) {
        int width = ICON_SPACE;
        
        if (content instanceof TextIconsContent) {
            width = ((TextIconsContent)content).getNoOfRightIcons() * ICON_SPACE;
        } 
        
        return width;
    }
    
    
    public int getIconsSpace(Object content) 
    {
        return getLeftIconsSpace(content) + getRightIconsSpace(content);
    }

    
    @Override
    public void drawCell(GC gc, 
                         Rectangle rect, 
                         int col, 
                         int row, 
                         Object content, 
                         boolean focus, 
                         boolean fixed,
                         boolean clicked, 
                         KTableModel model)
    {
        applyFont(gc);
        String text = null;
        TextIconsContent tiContent = null;
        m_isEditable = false;

        // KTable calls this method also with 'content' of type String
        if (content instanceof TextIconsContent) {
            tiContent = (TextIconsContent)content;
            m_isEditable = tiContent.isEditable();
            text = tiContent.getText();
            Color bkgColor = tiContent.getBackground();
            if (bkgColor != null) {
                setBackground(bkgColor);
            }
        } else {
            text = content.toString();
        }
        
        text = (text == null) ? "" : text;
        
        Color foreground = getForeground();

        if (m_isHeaderCell) {
            drawHeaderCell(gc, rect, text, tiContent, focus);
        } else {
            if (tiContent != null  &&  tiContent.getEditorType() == EEditorType.ECombo  &&  text.isEmpty()) {
                foreground = COMBO_DEFAULT_ITEM_FG_COLOR;
                text = tiContent.getDefaultEnumValue();
            }
            rect = drawBodyCell(gc, rect, text, tiContent, foreground, focus);
        }
        
        resetFont(gc);
        
        if (tiContent == null) {
            return;
        }
        
        // combo box items are written as underlined text
        if (tiContent.getEditorType() == EEditorType.ECombo) {
            int lineLen = gc.textExtent(text).x + 7;
            lineLen = Math.min(lineLen, rect.width - getIconsSpace(tiContent));
            Color fg = gc.getForeground();
            gc.setForeground(foreground);
            // draw line under text
            gc.drawLine(rect.x + getLeftIconsSpace(tiContent), 
                        rect.y + rect.height - 3, 
                        rect.x + getLeftIconsSpace(tiContent) + lineLen, 
                        rect.y + rect.height - 3);
            gc.setForeground(fg);
        }
        
        drawIcons(gc, tiContent, rect, m_isEnabled);
    }
    
    
    public static void drawIcons(GC gc, TextIconsContent tiContent, Rectangle rect, boolean isEnabled)
    {
        Image icon = tiContent.getIcon(EIconPos.ETopLeft, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, rect.x + DIST_TO_BORDER, rect.y + 1);
        }

        icon = tiContent.getIcon(EIconPos.ETopLeft_R, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, rect.x + ICON_SPACE, rect.y + 1);
        }
        
        icon = tiContent.getIcon(EIconPos.ETopRight, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + rect.width - icon.getBounds().width - DIST_TO_BORDER, 
                         rect.y + 1);
        }
        
        icon = tiContent.getIcon(EIconPos.ETopRight_L, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + rect.width - ICON_SPACE * 2, 
                         rect.y + 1);
        }
/*
        icon = tiContent.getIcon(EIconPos.EMiddleLeft);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + DIST_TO_BORDER, 
                         rect.y + (rect.height - icon.getBounds().height) / 2);
        }

        icon = tiContent.getIcon(EIconPos.EMiddleRight);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + rect.width - icon.getBounds().width - DIST_TO_BORDER, 
                         rect.y + (rect.height - icon.getBounds().height) / 2);
        }
*/
        icon = tiContent.getIcon(EIconPos.EMiddleMiddle, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + (rect.width - icon.getBounds().width) / 2, 
                         rect.y + (rect.height - icon.getBounds().height) / 2);
        }
        
        icon = tiContent.getIcon(EIconPos.EBottomLeft, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + DIST_TO_BORDER, 
                         rect.y  + rect.height - icon.getBounds().height - 1);
        }

        icon = tiContent.getIcon(EIconPos.EBottomLeft_R, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + ICON_SPACE, 
                         rect.y  + rect.height - icon.getBounds().height - 1);
        }

        icon = tiContent.getIcon(EIconPos.EBottomRight, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + rect.width - icon.getBounds().width - DIST_TO_BORDER, 
                         rect.y + rect.height - icon.getBounds().height - 1);
        }
        
        icon = tiContent.getIcon(EIconPos.EBottomRight_L, isEnabled);
        if (icon != null) {
            gc.drawImage(icon, 
                         rect.x + rect.width - ICON_SPACE, 
                         rect.y + rect.height - icon.getBounds().height - 1);
        }
    }


    private Rectangle drawBodyCell(GC gc,
                                   Rectangle rect,
                                   String text,
                                   TextIconsContent tiContent, 
                                   Color foreground,
                                   boolean focus) {
        // draw focus sign:
        if (focus && (m_Style & INDICATION_FOCUS) != 0) {
            rect = drawBorder(gc, rect, tiContent, COLOR_LINE_LIGHTGRAY, COLOR_LINE_LIGHTGRAY);
            drawCellContent(gc, rect, text, tiContent, null, foreground, COLOR_BGFOCUS);
            gc.drawFocus(rect.x, rect.y, rect.width, rect.height);
            
        } else if (focus && (m_Style & INDICATION_FOCUS_ROW) != 0) {
            rect = drawBorder(gc, rect, tiContent, COLOR_BGROWFOCUS, COLOR_BGROWFOCUS);
            drawCellContent(gc, rect, text, tiContent, null, COLOR_FGROWFOCUS, COLOR_BGROWFOCUS);
            
        } else {
            rect = drawBorder(gc, rect, tiContent, COLOR_LINE_LIGHTGRAY, COLOR_LINE_LIGHTGRAY);
            
            Color bkgColor = getBackground();          

            // first check if caller specified bkg color - if it did, do not override setting!
            if (tiContent == null ||  tiContent.getBackground() == null) {
                if (!m_isHeaderCell  &&  !m_isEditable) {
                    bkgColor = COLOR_LINE_LIGHTGRAY;
                }

                if (!m_isEnabled) {
                    bkgColor = COLOR_DISABLED_BACKGROUND;
                    foreground = COLOR_DISABLED_FOREGROUND;
                }
            } else {
                if (!m_isEnabled) {
                    foreground = COLOR_DISABLED_FOREGROUND;
                }
            }

            drawCellContent(gc, rect, text, tiContent, null, foreground, bkgColor);
        }
        return rect;
    }
    
    
    private void drawCellContent(GC gc, Rectangle rect, String text, 
                                 TextIconsContent tiContent, Image img, 
                                 Color textColor, Color backColor) {
        // clear background and paint content:
        gc.setBackground(backColor);
        gc.setForeground(textColor);
        gc.fillRectangle(rect);
        int xpos = rect.x + getLeftIconsSpace(tiContent); // move text to the right to make space for icons 
        int width = rect.width - getIconsSpace(tiContent); // do not overwrite icons with text

        SWTX.drawTextImage(gc, text, getAlignment(), 
                           img, getAlignment(), xpos + 3, rect.y+2,
                           width - 6, rect.height-4);
    }

    
    private void drawHeaderCell(GC gc, 
                                Rectangle rect, 
                                String content, 
                                TextIconsContent tiContent, 
                                boolean focus) {
        // set up the colors:
        Color bgColor;
        Color bottomBorderColor;
        Color rightBorderColor;
        Color fgColor;
        
        if (focus && (m_Style & INDICATION_FOCUS_ROW) != 0) {
            bgColor = COLOR_BGROWFOCUS;
            bottomBorderColor = COLOR_BGROWFOCUS;
            rightBorderColor = COLOR_BGROWFOCUS;
            fgColor = COLOR_FGROWFOCUS;
        } else if (focus && (m_Style & INDICATION_FOCUS) != 0) { 
            bgColor = COLOR_FIXEDHIGHLIGHT;
            bottomBorderColor = COLOR_TEXT;
            rightBorderColor = COLOR_TEXT;
            fgColor = getForeground();
        } else {
            bgColor = getHeaderBackground();
            bottomBorderColor = COLOR_LINE_DARKGRAY;
            rightBorderColor = COLOR_LINE_DARKGRAY;
            fgColor = getForeground();
        }
        
               
        // STYLE_FLAT:
        rect = drawBorder(gc, rect, tiContent, bottomBorderColor, rightBorderColor);

        // draw content:
        drawCellContent(gc, rect, content, tiContent, null, fgColor, bgColor);
    }
    
    
    protected Rectangle drawBorder(GC gc, Rectangle rect,
                                   TextIconsContent tiContent,
                                   Color vBorderColor, Color hBorderColor) {
        
        if (tiContent != null  && tiContent.getBorder() != null) {
            return BorderPainter.drawSolidBorder(gc, rect, 
                                                 tiContent.getBorder(), 
                                                 vBorderColor, hBorderColor);
        } else {
            return BorderPainter.drawDefaultSolidCellLine(gc, rect, vBorderColor, hBorderColor);
        }
    }

    
    public Color getHeaderBackground() {
        if (m_bgColor != null) {
            return m_bgColor;
        }
        return FixedCellRenderer.COLOR_FIXEDBACKGROUND;
    }
    
    
    /**
     * Returns icon position for the given coordinate.
     * 
     * @param x x coord in the cell
     * @param y y coord in the cell
     */
    public static EIconPos getIconPos(Rectangle cell, int x, int y) {
        
        // Rectangle rect = m_table.getCellRect(cell.x, cell.y);
        int cellX = x - cell.x;
        int cellY = y - cell.y;
        int cellW = cell.width;
        int cellH = cell.height;

        EIconPos iconPos = null;
        // middle left is never used, as cell height is usually to small to 
        // show three icons
        if (cellX > RESIZE_COLUMN_AREA  &&  cellX < TextIconsCellRenderer.ICON_SPACE) {
            if (cellY < cellH / 2) {
                iconPos = EIconPos.ETopLeft;
            // } else if (cellY < 2 * cellH / 3) {
            //    iconPos = EIconPos.EMiddleLeft;
            } else if (cellY < cellH) {
                iconPos = EIconPos.EBottomLeft;
            }
        } else if (cellX > TextIconsCellRenderer.ICON_SPACE  &&  cellX < 2*TextIconsCellRenderer.ICON_SPACE) { 
            if (cellY < cellH / 2) {
                iconPos = EIconPos.ETopLeft_R;
            } else if (cellY < cellH) {
                iconPos = EIconPos.EBottomLeft_R;
            }
        } else if (cellX > cellW - TextIconsCellRenderer.ICON_SPACE  &&  cellX < (cellW - RESIZE_COLUMN_AREA)) {
            if (cellY < cellH / 2) {
                iconPos = EIconPos.ETopRight;
            // } else if (cellY < 2 * cellH / 3) {
            //     iconPos = EIconPos.EMiddleRight;
            } else if (cellY < cellH) {
                iconPos = EIconPos.EBottomRight;
            }
        } else if (cellX > cellW - 2 * TextIconsCellRenderer.ICON_SPACE  &&  cellX < (cellW - TextIconsCellRenderer.ICON_SPACE)) {
            if (cellY < cellH / 2) {
                iconPos = EIconPos.ETopRight_L;
            } else if (cellY < cellH) {
                iconPos = EIconPos.EBottomRight_L;
            }
        } else {
            iconPos = EIconPos.EMiddleMiddle;
        }
        return iconPos;
    }
}
