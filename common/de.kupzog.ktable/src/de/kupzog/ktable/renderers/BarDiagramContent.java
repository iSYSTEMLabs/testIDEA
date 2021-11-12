package de.kupzog.ktable.renderers;

import de.kupzog.ktable.renderers.BarDiagramCellRenderer.BarBorderStyle;
import org.eclipse.swt.graphics.Color;

public class BarDiagramContent {

    public final static int DEFAULT_PADDING = 3;
    private String m_text = "";
    private int m_padding = DEFAULT_PADDING;
    private BarBorderStyle m_borderStyle = BarBorderStyle.SOLID;
    private Color m_borderColor = DefaultCellRenderer.COLOR_LINE_DARKGRAY;
    
    private float [] m_fractions;
    private Color [] m_barColors;

 
    public BarDiagramContent(float fraction) {
        m_fractions = new float[1];
        m_fractions[0] = fraction;
    }
    
    
    public BarDiagramContent(float fraction,
                             String text) {
        this(fraction);
        
        m_text = text;
    }


    /**
     * 
     * @param fractions
     * @param colors if there are more colors then fractions, and sum of fractions < 1,
     *               then the remaining of bar will be colored with the first 
     *               'redundant' color in this array.
     */
    public BarDiagramContent(float []fractions, Color[] colors) {
        m_fractions = fractions;
        m_barColors = colors;
    }
    

    public String getText() {
        return m_text;
    }
    
    
    public void setText(String text) {
        m_text = text;
    }


    public float [] getFractions() {
        return m_fractions;
    }

    
    public void setFractions(float[] fractions) {
        m_fractions = fractions;
    }


    /** Returns distance between bar and cell border. */
    public int getPadding() {
        return m_padding;
    }

    
    public void setPadding(int padding) {
        m_padding = padding;
    }


    public void setBorderStyle(BarBorderStyle borderStyle) {
        m_borderStyle = borderStyle;
    }

    
    public BarBorderStyle getBorderStyle() {
        return m_borderStyle;
    }


    /** Returns bar color. This setting overrides color from cell renderer. */
    public Color [] getBarColors() {
        return m_barColors;
    }


    /** Sets bar color. This setting overrides color from cell renderer. */
    public void setBarColors(Color [] barColors) {
        m_barColors = barColors;
    }


    public Color getBorderColor() {
        return m_borderColor;
    }


    public void setBorderColor(Color borderColor) {
        m_borderColor = borderColor;
    }
    
    
    @Override
    public String toString() {
        return m_text;
    }
}
