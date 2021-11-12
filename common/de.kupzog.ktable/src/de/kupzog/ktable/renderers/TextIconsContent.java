package de.kupzog.ktable.renderers;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import de.kupzog.ktable.renderers.BorderPainter.BorderAttrs;
import de.kupzog.ktable.renderers.BorderPainter.BorderLines;

public class TextIconsContent {

    public enum EIconPos {
        ETopLeft, 
        ETopLeft_R,  // right to the ETopLEft 
        
        ETopRight,   // the rightmost 
        ETopRight_L, // left to the ETopRight
        
        /** @deprecated there is usually not enough space in table cell to show three icons vertically. */
        EMiddleLeft, 
        /** @deprecated there is usually not enough space in table cell to show three icons vertically. */
        EMiddleRight,
        
        EBottomLeft, 
        EBottomLeft_R,  // right to EBottomLeft 
        EBottomRight, 
        EBottomRight_L,  // left to EBottomRight 
        EMiddleMiddle
    };
    
    public enum EBorderLocation {
        ETop,
        ELeft,
        EBottom,
        ERight,
        /* EAll,
        EVertical,  // left and right
        EHorizontal // top and bottom */
    }
    
    /**
     * Edit type to be used for a cell. Later it may be necessary to add
     * other editors, for example color editor, dialog editor, ... 
     * @author markok
     */
    public enum EEditorType {ETristate, ECombo, EText};
    
    private Map<EIconPos, Image> m_enabledImageMap;
    private Map<EIconPos, Image> m_disabledImageMap;
    private EEditorType m_editorType;
    private String m_text;
    private String m_nlComment;
    private String m_eolComment;
    // private String[] m_comboItems;
    private boolean m_defaultForTristate;
    private String m_tristateValue; // stores strings E_TRUE, E_FALSE, E_DEFAULT 
                                    // for simpler conversion to ETristate value
    private String m_defaultEnumValue;
    
    private Map<EIconPos, String> m_tooltips;
    private boolean m_isEditable = false;
    private Color m_bkgColor;
    
    private BorderLines m_borderLines;
    
    public Image getIcon(EIconPos iconPos, boolean isEnabled) {
        
        if (iconPos == null) {
            return null;
        }
        
        if (isEnabled) {
            if (m_enabledImageMap == null) {
                return null;
            }
            return m_enabledImageMap.get(iconPos);
        } 
            
        if (m_disabledImageMap == null) {
            return null;
        }
        return m_disabledImageMap.get(iconPos);
    }
    
    
    public void setIcon(EIconPos iconPos, Image icon, boolean isEnabled) {
        if (isEnabled) {
            if (m_enabledImageMap == null) {
                m_enabledImageMap = new TreeMap<TextIconsContent.EIconPos, Image>();
            }
            m_enabledImageMap.put(iconPos, icon);
        }

        if (m_disabledImageMap == null) {
            m_disabledImageMap = new TreeMap<TextIconsContent.EIconPos, Image>();
        }
        m_disabledImageMap.put(iconPos, icon);
    }
    
    
    /**
     * Currently returns at least one, even if there are no icons. If empty space
     * is a problem, update these two methods. 
     */
    public int getNoOfLeftIcons() {
        if (getIcon(EIconPos.ETopLeft_R, true) == null  &&
            getIcon(EIconPos.ETopLeft_R, false) == null  &&
            getIcon(EIconPos.EBottomLeft_R, true) == null  &&
            getIcon(EIconPos.EBottomLeft_R, false) == null) {
            return 1;
        }
        
        return 2;
    }
    
    
    public int getNoOfRightIcons() {
        if (getIcon(EIconPos.ETopRight_L, true) == null  &&
            getIcon(EIconPos.ETopRight_L, false) == null  &&
            getIcon(EIconPos.EBottomRight_L, true) == null  &&
            getIcon(EIconPos.EBottomRight_L, false) == null) {
            return 1;
        }
        
        return 2;
    }
    
    
    public String getTooltip(EIconPos iconPos) {
        if (m_tooltips == null  ||  iconPos == null) {
            return null;
        }
        return m_tooltips.get(iconPos);
    }
    
    
    public void setTooltip(EIconPos iconPos, String tooltip) {
        if (m_tooltips == null) {
            m_tooltips = new TreeMap<>();
        }
        m_tooltips.put(iconPos, tooltip);
    }
    
    
    public String getText() {
        return m_text == null ? "" : m_text;
    }
    
    
    public void setText(String text) {
        m_text = text;
    }
    
    
    /**
     * @param isEditable the isEditable to set
     */
    public void setEditable(boolean isEditable) {
        m_isEditable = isEditable;
    }


    /**
     * 
     * @return true, if the cell is editable
     */
    public boolean isEditable() {
        return m_isEditable;
    }
    
    
    /**
     * @return the new line comment
     */
    public String getNlComment() {
        return m_nlComment == null ? "" : m_nlComment;
    }


    /**
     * @param nlComment the new line comment to set
     */
    public void setNlComment(String nlComment) {
        m_nlComment = nlComment;
    }


    /**
     * @return the end of line comment
     */
    public String getEolComment() {
        return m_eolComment == null ? "" : m_eolComment;
    }


    /**
     * @param eolComment the end of line comment to set
     */
    public void setEolComment(String eolComment) {
        m_eolComment = eolComment;
    }

    
    /**
     * @return the editorType
     */
    public EEditorType getEditorType() {
        return m_editorType;
    }


    /**
     * @param editorType the editorType to set
     */
    public void setEditorType(EEditorType editorType) {
        m_editorType = editorType;
    }


    /**
     * @return the comboItems
     */
    /* public String[] getComboItems() {
        return m_comboItems;
    } */


    /**
     * @param comboItems the comboItems to set
    public void setComboItems(String[] comboItems) {
        m_comboItems = comboItems;
    }
     */


    /**
     * @return the defaultForTristate
     */
    public boolean getDefaultForTristate() {
        return m_defaultForTristate;
    }


    /**
     * @param defaultForTristate the defaultForTristate to set
     */
    public void setDefaultForTristate(boolean defaultForTristate) {
        m_defaultForTristate = defaultForTristate;
    }


    /**
     * @return the defaultEnumValue
     */
    public String getDefaultEnumValue() {
        return m_defaultEnumValue;
    }


    /**
     * @param defaultEnumValue the defaultEnumValue to set
     */
    public void setDefaultEnumValue(String defaultEnumValue) {
        m_defaultEnumValue = defaultEnumValue;
    }


    /**
     * @return the boolEnumValue
     */
    public String getTristateValue() {
        return m_tristateValue;
    }


    /**
     * @param boolEnumValue the boolEnumValue to set
     */
    public void setTristateValue(String boolEnumValue) {
        m_tristateValue = boolEnumValue;
    }

    
    public void setBackground(Color bkgColor) {
        m_bkgColor = bkgColor;
    }
    
    
    public Color getBackground() {
        return m_bkgColor;
    }
    

    public int getBorderWidth(EBorderLocation location) {
        switch (location) {
        case EBottom:
            return m_borderLines.m_bottom.m_width;
        case ELeft:
            return m_borderLines.m_left.m_width;
        case ERight:
            return m_borderLines.m_right.m_width;
        case ETop:
            return m_borderLines.m_top.m_width;
        }
        throw new IllegalArgumentException("Invalid border location: " + location);
    }


    public void setBorder(EBorderLocation location, Color borderColor, int borderWidth) {
        
        if (m_borderLines == null) {
            m_borderLines = new BorderLines();
        }
        
        switch (location) {
        case EBottom:
            m_borderLines.m_bottom = new BorderAttrs(borderColor, borderWidth);
            break;
        case ELeft:
            m_borderLines.m_left = new BorderAttrs(borderColor, borderWidth);
            break;
        case ERight:
            m_borderLines.m_right = new BorderAttrs(borderColor, borderWidth);
            break;
        case ETop:
            m_borderLines.m_top = new BorderAttrs(borderColor, borderWidth);
            break;
        default:
            throw new IllegalArgumentException("Invalid border location: " + location);
        }
    }

    
    public BorderLines getBorder() {
        return m_borderLines;
    }


    /**
     * This method is called by KTableCellEditorText2.getEditorContent(), so
     * it should not be modified for debugging purposes, or implement your own
     * table cell editor.
     */
    @Override
    public String toString() {
        return m_text == null ? "" : m_text;
    }
}
