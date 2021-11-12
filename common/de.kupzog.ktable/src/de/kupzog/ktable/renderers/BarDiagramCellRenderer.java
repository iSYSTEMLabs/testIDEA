/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */
package de.kupzog.ktable.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import de.kupzog.ktable.KTableModel;

/**
 * 
 * @author Lorenz Maierhofer <lorenz.maierhofer@logicmindguide.com>
 */
public class BarDiagramCellRenderer extends DefaultCellRenderer {

    public enum BarBorderStyle {
        SOLID,
        DOTTED
    };
    
    /**
     * @param style The style bits to use.
     * Currently supported are:<br>
     * - INDICATION_FOCUS<br>
     * - INDICATION_FOCUS_ROW<br>
     * - INDICATION_GRADIENT
     */
    public BarDiagramCellRenderer(int style) {
        super(style);
    }
    
    
    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableCellRenderer#getOptimalWidth(org.eclipse.swt.graphics.GC, int, int, java.lang.Object, boolean)
     */
    public int getOptimalWidth(GC gc, int col, int row, Object content, boolean fixed, KTableModel model) {
        return 20;
    }

    
    /** 
     * @param content The content is expected to be a Float value between 0 and 1 that represents
     * the fraction of the cell width that should be used for the bar.
     * @see de.kupzog.ktable.KTableCellRenderer#drawCell(GC, Rectangle, int, int, Object, boolean, boolean, boolean, KTableModel)
     */
    @Override
    public void drawCell(GC gc, Rectangle rect, int col, int row, Object content, 
            boolean focus, boolean fixed, boolean clicked, KTableModel model) {
        
        if (focus && (m_Style & INDICATION_FOCUS)!=0) {
            rect = drawDefaultSolidCellLine(gc, rect, COLOR_LINE_LIGHTGRAY, COLOR_LINE_LIGHTGRAY);
            drawBar(gc, rect, content, COLOR_BGFOCUS, getForeground());            
            gc.drawFocus(rect.x, rect.y, rect.width, rect.height);
            
        } else if (focus && (m_Style & INDICATION_FOCUS_ROW)!=0) {
            rect = drawDefaultSolidCellLine(gc, rect, COLOR_BGROWFOCUS, COLOR_BGROWFOCUS);
            Color defaultBg = COLOR_BACKGROUND;
            setDefaultBackground(COLOR_BGROWFOCUS);
            drawBar(gc, rect, content, getBackground(), getForeground());
            setDefaultBackground(defaultBg);
            
        } else {
            rect = drawDefaultSolidCellLine(gc, rect, COLOR_LINE_LIGHTGRAY, COLOR_LINE_LIGHTGRAY);
            drawBar(gc, rect, content, getBackground(), getForeground());
        }
    }

    
    /**
     * @param gc
     * @param rect
     * @param m_fraction
     * @param background
     */
    protected void drawGradientBar(GC gc, Rectangle rect, float m_fraction, Color background, Color foreground) {
        int barWidth = Math.round(rect.width*m_fraction);
        gc.setForeground(background);
        gc.setBackground(foreground);
        gc.fillGradientRectangle(rect.x, rect.y, barWidth, rect.height, false);
        gc.setBackground(COLOR_BACKGROUND);
        gc.fillRectangle(rect.x+barWidth, rect.y, rect.width-barWidth, rect.height);
    }

    
    /**
     * @param gc
     * @param rect
     * @param cellBackground
     * @param barColor
     * @param m_fraction
     */
    protected void drawNormalBar(GC gc, Rectangle rect, 
                                 Color cellBackground, 
                                 Color []barColors,
                                 float []fractions,
                                 Color borderColor,
                                 BarBorderStyle barBorderStyle,
                                 int offset,
                                 String text) {

        // clear the cell first
        gc.setBackground(cellBackground);
        gc.fillRectangle(rect.x, rect.y, rect.width, rect.height);

        int barXPos = rect.x + offset;
        int barYPos = rect.y + offset;
        int barWidth = rect.width - 2 * offset;
        int barHeight = rect.height - 2 * offset;

        int colorIdx = 0;
        int fractionXPos = barXPos;
        
        for (float fraction : fractions) {
            int fractionWidth = Math.round(barWidth * fraction);

            gc.setBackground(barColors[colorIdx++]);
            gc.fillRectangle(fractionXPos, 
                             barYPos, 
                             fractionWidth, 
                             barHeight);
            
            fractionXPos += fractionWidth;
        }
        
        if (barBorderStyle != null) {
            int lw = gc.getLineWidth();
            int ls = gc.getLineStyle();
            Color oldColor = null;
            
            if (borderColor != null) {
                oldColor = gc.getForeground();
                gc.setForeground(borderColor);
            }
                
            gc.setLineWidth(1);

            switch (barBorderStyle) {
            case DOTTED:
                gc.setLineStyle(SWT.LINE_DOT);
                break;
            case SOLID:
                gc.setLineStyle(SWT.LINE_SOLID);
                break;
            default:
                break;
            }
            
            gc.drawRectangle(barXPos, 
                             barYPos,
                             barWidth, barHeight);
            
            gc.setLineWidth(lw);
            gc.setLineStyle(ls);
            if (borderColor != null) {
                gc.setForeground(oldColor);
            }
        }
        
        if (text != null) {
            gc.setForeground(COLOR_TEXT);
            Point textWH = gc.textExtent(text);
            gc.drawText(text, 
                        rect.x + (rect.width - textWH.x) / 2, 
                        rect.y + (rect.height - textWH.y) / 2 + 1,
                        true);
        }
    }
    
    
    /**
     * @param gc
     * @param rect
     * @param m_fraction
     * @param background
     */
    protected void drawBar(GC gc, Rectangle rect, Object content, Color background, Color foreground) {
        
        float [] fractions = new float[1];
        BarBorderStyle barBorderStyle = BarBorderStyle.DOTTED;
        int offset = 3;
        String text = "";
        Color borderColor = COLOR_LINE_DARKGRAY;
        Color [] barColors = new Color[1];
        barColors[0] = foreground;
        
        if (content instanceof Float) {
            fractions[0] = ((Float)content).floatValue();
            
        } else if (content instanceof Double) {
            fractions[0] = ((Double)content).floatValue();
            
        } else if (content instanceof BarDiagramContent) {
            BarDiagramContent barContent = (BarDiagramContent)content;
            
            fractions = barContent.getFractions();
            barBorderStyle = barContent.getBorderStyle();
            offset = barContent.getPadding();
            text = barContent.getText();
            borderColor = barContent.getBorderColor();
            if (barContent.getBarColors() != null) {
                barColors = barContent.getBarColors();
            }
            
        } else if (content instanceof IPercentage) {
            fractions[0] = ((IPercentage)content).getPercentage();
            
        } else {
            fractions[0] = 0;
        }
        
        float sum = 0;
        for (int idx = 0; idx < fractions.length; idx++) {
            if (fractions[idx] < 0) {
                throw new IllegalArgumentException("Fraction in bar cell renderer may not be less than 0! "
                        + "idx = " + idx + ",  fraction[idx] = " + fractions[idx]);
            }
            sum += fractions[idx];
        }
        
        // normalize values
        if (sum > 1) {
            for (int idx = 0; idx < fractions.length; idx++) {
                fractions[idx] /= sum;
            }
        }        
        
        if ((m_Style & INDICATION_GRADIENT) != 0)
            drawGradientBar(gc, rect, fractions[0], background, foreground);
        else
            drawNormalBar(gc, rect, background, barColors, fractions,  
                          borderColor, barBorderStyle, offset, text);
    }
}
