/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
    
    Author: Friederich Kupzog  
    fkmk@kupzog.de
    www.kupzog.de/fkmk
*/
package de.kupzog.examples;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModel;
/**
 * @author Friederich Kupzog
 */
public class PaletteExampleModel 
implements KTableModel {

	/* 
	 * overridden from superclass
	 */
	public Object getContentAt(int col, int row) {
		return new RGB(col*16,row*16,(col+row)*8);
	}

	/* 
	 * overridden from superclass
	 */
	public KTableCellEditor getCellEditor(int col, int row) {
		return null;
	}

	/* 
	 * overridden from superclass
	 */
	public void setContentAt(int col, int row, Object value) {
	}

	/* 
	 * overridden from superclass
	 */
	public int getRowCount() {
		return 16;
	}

	/* 
	 * overridden from superclass
	 */
	public int getFixedHeaderRowCount() {
		return 0;
	}

	/* 
	 * overridden from superclass
	 */
	public int getColumnCount() {
		return 16;
	}

	/* 
	 * overridden from superclass
	 */
	public int getFixedHeaderColumnCount() {
		return 0;
	}

	/* 
	 * overridden from superclass
	 */
	public int getColumnWidth(int col) {
		return 10;
	}

	/* 
	 * overridden from superclass
	 */
	public boolean isColumnResizable(int col) {
		return false;
	}

	/* 
	 * overridden from superclass
	 */
	public void setColumnWidth(int col, int value) {
	}

	/* 
	 * overridden from superclass
	 */
	public int getRowHeight(int row) {
		return 10;
	}

	/* 
	 * overridden from superclass
	 */
	public boolean isRowResizable(int row) {
		return false;
	}
	
	public void setFirstRowHeight(int value) {
	    // ignore since row resize is not allowed anyway.
	}

	/* 
	 * overridden from superclass
	 */
	public int getRowHeightMinimum() {
		return 10;
	}

	/* 
	 * overridden from superclass
	 */
	public void setRowHeight(int row, int value) {
	}

	private static KTableCellRenderer myRenderer = new PaletteExampleRenderer();
	/* 
	 * overridden from superclass
	 */
	public KTableCellRenderer getCellRenderer(int col, int row) {
		return myRenderer;
	}

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#belongsToCell(int, int)
     */
    public Point belongsToCell(int col, int row) {
       return new Point(col, row);
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#getTooltipAt(int, int)
     */
    public String getTooltipAt(int col, int row) {
        return (col*16)+" / "+ (row*16) +" / "+ ((col+row)*8);
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#getFixedSelectableRowCount()
     */
    public int getFixedSelectableRowCount() {
        // all fixed rows are non-selectable.
        return 0;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#getFixedSelectableColumnCount()
     */
    public int getFixedSelectableColumnCount() {
        // all fixed columns are non-selectable.
        return 0;
    }

}
