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

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.editors.KTableCellEditorText;

/**
 * @author Friederich Kupzog
 */
public class SpanModelExample extends KTableDefaultModel {

    private HashMap content = new HashMap();

    /**
     * Initialize the underlying model
     */
    public SpanModelExample() {
        // before initializing, you probably have to set some member values
        // to make all model getter methods work properly.
        initialize();
    }

    public Object doGetContentAt(int col, int row) {
        if (isFixedCell(col, row)) 
            return "F"+col+"/"+row;
        
        if (((col-getFixedHeaderColumnCount()-getFixedSelectableColumnCount())%2==0 && (row-getFixedHeaderRowCount()-getFixedSelectableRowCount())%2==0))
            return "S"+col+"/"+row;
        
        // should never happen, since the spanning should prevent this:
        System.err.println("A content was requested that is invalid: "+col+"/"+row);
        return "Non-Valid: "+col+"/"+row;
        
    }

    /*
     * overridden from superclass
     */
    public KTableCellEditor doGetCellEditor(int col, int row) {
        if (isFixedCell(col, row)) 
            return null; // no editor
        
        if (((col-getFixedHeaderColumnCount()-getFixedSelectableColumnCount())%2==0 && (row-getFixedHeaderRowCount()-getFixedSelectableRowCount())%2==0))
            return new KTableCellEditorText();
        
        // should never happen:
        System.err.println("A cell editor was requested that is invalid: "+col+"/"+row);
        return null;
    }
    
    // Rendering
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        if (isFixedCell(col, row))
            return KTableCellRenderer.defaultRenderer;
        
        if (((col-getFixedHeaderColumnCount()-getFixedSelectableColumnCount())%2==0 && (row-getFixedHeaderRowCount()-getFixedSelectableRowCount())%2==0))
            return KTableCellRenderer.defaultRenderer;
        
        // should never happen:
        System.err.println("A cell renderer was requested that is invalid: "+col+"/"+row);
        return KTableCellRenderer.defaultRenderer;
    }

    /*
     * overridden from superclass
     */
    public void doSetContentAt(int col, int row, Object value) {
        System.out.println("Set val at: "+col+"/"+row+": "+value);
        content.put(col + "/" + row, value);
    }
    
    /**
     * Makes all cells span 2 rows and two colums.
     * This is just a simple example to demonstrate how to 
     * work with this feature.
     * @see de.kupzog.ktable.KTableDefaultModel#doBelongsToCell(int, int)
     */
    public Point doBelongsToCell(int col, int row) {
        if (isFixedCell(col, row)) 
            return new Point(col, row);

        col-=getFixedHeaderColumnCount()+getFixedSelectableColumnCount();
        row-=getFixedHeaderRowCount()+getFixedSelectableRowCount();
        
        if ((col==1 && row==1) && (col%2==0 && row%2==0))
            return new Point(col, row);
        
        // find supercell to merge with:
        int newCol = col;
        int newRow = row;
        if (col%2!=0)
            newCol--;
        if (row%2!=0)
            newRow--;
        
        return new Point(newCol+getFixedHeaderColumnCount()+getFixedSelectableColumnCount(), newRow+getFixedHeaderRowCount()+getFixedSelectableRowCount());
    }

    // number of cells:
    public int doGetRowCount() {
        return 10000000+getFixedRowCount();
    }
    
    public int doGetColumnCount() {
        return 10000000+getFixedColumnCount();
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
     * @see de.kupzog.ktable.KTableDefaultModel#getInitialRowHeight()
     */
    public int getInitialRowHeight(int row) {
       return 18;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableDefaultModel#doGetTooltipAt(int, int)
     */
    public String doGetTooltipAt(int col, int row) {
        return "Test-Tooltip";
    }
}

