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

import java.util.HashMap;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.editors.KTableCellEditorCheckbox2;
import de.kupzog.ktable.renderers.CheckableCellRenderer;
import de.kupzog.ktable.renderers.FixedCellRenderer;

/**
 * @author Friederich Kupzog
 */
public class BooleanModelExample extends KTableDefaultModel {

    private HashMap content = new HashMap();
    private KTableCellRenderer m_CheckableRenderer = 
        new CheckableCellRenderer(
                CheckableCellRenderer.INDICATION_CLICKED | 
                CheckableCellRenderer.INDICATION_FOCUS);
    private KTableCellRenderer m_FixedRenderer = 
        new FixedCellRenderer(
                FixedCellRenderer.INDICATION_FOCUS, true);

    /**
     * Initialize the underlying model
     */
    public BooleanModelExample() {
        // before initializing, you probably have to set some member values
        // to make all model getter methods work properly.
        initialize();
    }

    public Object doGetContentAt(int col, int row) {
        //System.out.println("col "+col+" row "+row);
        Boolean val = (Boolean ) content.get(col + "/" + row);
        if (val != null)
            return val;
        
        if ((col+row)%2==1)
            val = new Boolean(true);
        else 
            val = new Boolean(false);
        content.put(col+"/"+row, val);
        return val;
    }

    /*
     * overridden from superclass
     */
    public KTableCellEditor doGetCellEditor(int col, int row) {
        // make only the image active for the checkbox editor
        Rectangle imgBounds = CheckableCellRenderer.IMAGE_CHECKED.getBounds();
        Point sensible = new Point(imgBounds.width, imgBounds.height);
        return new KTableCellEditorCheckbox2(sensible, SWTX.ALIGN_HORIZONTAL_CENTER, SWTX.ALIGN_VERTICAL_CENTER);
    }
    
    // Rendering
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        if (isFixedCell(col, row))
            return m_FixedRenderer;
        else
            return m_CheckableRenderer;
    }

    /*
     * overridden from superclass
     */
    public void doSetContentAt(int col, int row, Object value) {
        System.out.println("Set val at: "+col+"/"+row+": "+value);
        content.put(col + "/" + row, value);
    }

    // Umfang

    public int doGetRowCount() {
        return 1000000+getFixedRowCount();
    }
    
    public int doGetColumnCount() {
        return 1000000+getFixedRowCount();
    }


    public int getFixedHeaderRowCount() {
        return 1;
    }

    public int getFixedHeaderColumnCount() {
        return 1;
    }
    
    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#getFixedSelectableRowCount()
     */
    public int getFixedSelectableRowCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#getFixedSelectableColumnCount()
     */
    public int getFixedSelectableColumnCount() {
        return 0;
    }

    public boolean isColumnResizable(int col) {
        return true;
    }

    public int getInitialFirstRowHeight() {
        return 22;
    }

    public boolean isRowResizable(int row) {
        return true;
    }

    public int getRowHeightMinimum() {
        return 18;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableDefaultModel#getInitialColumnWidth(int)
     */
    public int getInitialColumnWidth(int column) {
        return 35;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableDefaultModel#getInitialRowHeight(int)
     */
    public int getInitialRowHeight(int row) {
       return 18;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableDefaultModel#doGetTooltipAt(int, int)
     */
    public String doGetTooltipAt(int col, int row) {
        return "This cell ("+col+"/"+row+") has the value '"+
        getContentAt(col, row)+"'!";
    }
}

