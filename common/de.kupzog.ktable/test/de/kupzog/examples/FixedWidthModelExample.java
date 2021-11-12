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

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableNoScrollModel;
import de.kupzog.ktable.editors.KTableCellEditorCombo;
import de.kupzog.ktable.editors.KTableCellEditorComboText;
import de.kupzog.ktable.editors.KTableCellEditorText;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;

/**
 * @author Friederich Kupzog
 */
public class FixedWidthModelExample extends KTableNoScrollModel {

    private HashMap content = new HashMap();
    
    private final FixedCellRenderer m_fixedRenderer =
        new FixedCellRenderer(TextCellRenderer.INDICATION_FOCUS_ROW, true);
    
    private final TextCellRenderer m_textRenderer = 
        new TextCellRenderer(TextCellRenderer.INDICATION_FOCUS_ROW);

    /**
     * Initialize the base implementation.
     * Needs the table instance since it must compute the available
     * space and adapt appropriately.
     */
    public FixedWidthModelExample(KTable table) {
        super(table);
        // before initializing, you probably have to set some member values
        // to make all model getter methods work properly.
        initialize();
    }
    
    // Content:
    public Object doGetContentAt(int col, int row) {
        if (col==1 && row==1) return "Resize Columns";
        String erg = (String) content.get(col + "/" + row);
        if (erg != null)
            return erg;
        return "C"+col+", r"+row;
    }

    /*
     * overridden from superclass
     */
    public KTableCellEditor doGetCellEditor(int col, int row) {
    	if (col<getFixedColumnCount() || row<getFixedRowCount())
    		return null;
        if (col % 3 == 1) 
        {
            KTableCellEditorCombo e = new KTableCellEditorCombo();
            e.setItems(new String[] { "First text", "Second text",
                            "third text" });
            return e;
        }
        else if (col % 3 == 2) 
        {
                KTableCellEditorComboText e = new KTableCellEditorComboText();
                e.setItems(new String[] { "You choose", "or type",
                                "a new content." });
                return e;
        }
        else
        {
            return new KTableCellEditorText();
        }
    }

    /*
     * overridden from superclass
     */
    public void doSetContentAt(int col, int row, Object value) {
        content.put(col + "/" + row, value);
    }

    // Table size:
    public int doGetRowCount() {
        return 30+getFixedRowCount();
    }

    public int getFixedHeaderRowCount() {
        return 1;
    }

    public int doGetColumnCount() {
        return 5+getFixedColumnCount();
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

    public boolean isRowResizable(int row) {
        return true;
    }

    public int getRowHeightMinimum() {
        return 18;
    }
    
    // Rendering
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        if (isFixedCell(col, row))
            return m_fixedRenderer;
        
        return m_textRenderer;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableModel#belongsToCell(int, int)
     */
    public Point doBelongsToCell(int col, int row) {
        // no cell spanning:
        return null;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableDefaultModel#getInitialColumnWidth(int)
     */
    public int getInitialColumnWidth(int column) {
        // this is just a weight - and does not necessarily corresponds 
        // to the pixel size of the row!
        return 90;
    }

    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableDefaultModel#getInitialRowHeight(int)
     */
    public int getInitialRowHeight(int row) {
    	if (row==0) return 22;
    	return 18;
    }
}

