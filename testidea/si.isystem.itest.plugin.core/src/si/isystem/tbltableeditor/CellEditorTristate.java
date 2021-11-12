/*
 * Copyright (C) 2013 iSYSTEM AG
 * 
 * Author: markok 
*/
package si.isystem.tbltableeditor;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import si.isystem.connect.ETristate;
import si.isystem.itest.common.UiUtils;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.renderers.TextIconsContent;

/**
 * A cell editor that expects a ETristate cell value
 * and simply switches this value. It has no control, it
 * just changes the value in the model and forces a cell
 * redraw.<p>
 * NOTE: This implementation makes the whole cell area sensible. 
 * It is activated by a RETURN, a SPACE or a single mouse click.
 * <p>
 * Note: If you need this behavior, but wish to have only a part
 * of the cell area that is sensible (like the checkbox that must
 * be clicked, independently of how big the cell area is), look at
 * KTableCellEditorCheckbox2.
 * 
 * @see de.kupzog.ktable.editors.KTableCellEditorCheckbox2
 * @see de.kupzog.ktable.cellrenderers.CheckableCellRenderer
 */
public class CellEditorTristate extends KTableCellEditor {

    private Point m_checkBoxSize;
    private int m_hAlign;
    private int m_vAlign;
    
    public CellEditorTristate(Point checkBoxSize, int hAligh, int vAlign) {
        m_checkBoxSize = checkBoxSize;
        m_hAlign = hAligh;
        m_vAlign = vAlign;
    }
    
    
    /**
	 * Activates the editor at the given position.
	 * Instantly closes the editor and switch the boolean content value.
	 * @param row
	 * @param col
	 * @param rect
	 */
	@Override
    public void open(KTable table, int col, int row, Rectangle rect) {
		m_Table = table;
		m_Model = table.getModel();
		m_Rect = rect;
		m_Row = row;
		m_Col = col;
		
		close(true);
		
		GC gc = new GC(m_Table);
		m_Table.updateCell(m_Col, m_Row);
		gc.dispose();
	}
	
	
   /*
    * Is called when an activation is triggered via a mouse click.<p> 
    * If false is returned, the editor does not get activated.<p>
    * All coordinates must be relative to the KTable.
    * @param clickLocation The point where the mouseclick occured.
    * @return Returns true if the editor activation should happen.
    */
   @Override
   public boolean isApplicable(int eventType, KTable table, int col, int row, 
           Point clickLocation, String keyInput, int stateMask) {
       
       if (eventType == SINGLECLICK) {
           // compute active location inside the cellBoundary:
           Rectangle active = new Rectangle(0, 0, m_checkBoxSize.x, m_checkBoxSize.y);
           Rectangle cellBoundary = table.getCellRect(col, row);
           if (cellBoundary.width < active.width) {
               active.width = cellBoundary.width;
           }
           if (cellBoundary.height < active.height) {
               active.height = cellBoundary.height;
           }
           
           if (m_hAlign == SWTX.ALIGN_HORIZONTAL_LEFT)
               active.x = cellBoundary.x;
           else if (m_hAlign == SWTX.ALIGN_HORIZONTAL_RIGHT)
               active.x = cellBoundary.x + cellBoundary.width - active.width;
           else // center
               active.x = cellBoundary.x + (cellBoundary.width - active.width)/2;
           
           if (m_vAlign == SWTX.ALIGN_VERTICAL_TOP)
               active.y = cellBoundary.y;
           else if (m_vAlign == SWTX.ALIGN_VERTICAL_BOTTOM) 
               active.y = cellBoundary.y + cellBoundary.height - active.height;
           else 
               active.y = cellBoundary.y + (cellBoundary.height - active.height)/2;
           
           // check if clickLocation is inside the specified active area:
           if (active.contains(clickLocation))
               return true;
           return false;
       } else {
           return true;
       }
    }	
	
   
	/**
	 * Simply switches the boolean value in the model!
	 */
	@Override
    public void close(boolean save) {
	    if (save) {
	        Object o = m_Model.getContentAt(m_Col, m_Row);
	        if (!(o instanceof TextIconsContent)) {
	            throw new ClassCastException("CheckboxCellEditor needs a ETristate content!");
	        }
	        
	        TextIconsContent content = (TextIconsContent)o;
	        
	        boolean defaultForTristate = content.getDefaultForTristate();
	        ETristate value = ETristate.valueOf(content.getTristateValue());
	        value = getNextToggleState(value, defaultForTristate);
	        
	        m_Model.setContentAt(m_Col, m_Row, UiUtils.tristate2String(value));
	    }
	    super.close(save);
	}
    
	
	private ETristate getNextToggleState(ETristate state, boolean isDefaultTrue) {

	    if (isDefaultTrue) {
	        switch (state) {
	        case E_DEFAULT:
	            return ETristate.E_TRUE;
	        case E_FALSE:
                return ETristate.E_DEFAULT;
	        case E_TRUE:
                return ETristate.E_FALSE;
	        }
	    }
	    
	    switch (state) {
        case E_DEFAULT:
            return ETristate.E_FALSE;
        case E_FALSE:
            return ETristate.E_TRUE;
        case E_TRUE:
            return ETristate.E_DEFAULT;
	    }

	    return ETristate.E_DEFAULT;
	}
	
	
    /**
     * This editor does not have a control, it only switches 
     * the boolean value on activation!
     * @see de.kupzog.ktable.KTableCellEditor#createControl()
     */
    @Override
    protected Control createControl() {
		return null;
	}

    /**
     * This implementation does nothing!
     * @see de.kupzog.ktable.KTableCellEditor#setContent(java.lang.Object)
     */
    @Override
    public void setContent(Object content) {
    }

    /**
	 * @return Returns a value indicating on which actions 
	 * this editor should be activated.
	 */
	@Override
    public int getActivationSignals() {
	    return SINGLECLICK | KEY_RETURN_AND_SPACE;
	}
}
