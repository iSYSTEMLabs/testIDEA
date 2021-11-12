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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.editors.KTableCellEditorCombo;
import de.kupzog.ktable.editors.KTableCellEditorComboText;
import de.kupzog.ktable.editors.KTableCellEditorText;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;

/**
 * @author Friederich Kupzog
 */
public class TextModelExample extends KTableDefaultModel {

    private HashMap content = new HashMap();
    
    private final FixedCellRenderer m_fixedRenderer =
        new FixedCellRenderer(
            TextCellRenderer.INDICATION_FOCUS_ROW, false);
    
    private final TextCellRenderer m_textRenderer = 
        new TextCellRenderer(TextCellRenderer.INDICATION_FOCUS_ROW);

    /**
     * Initialize the base implementation.
     */
    public TextModelExample() {
        // before initializing, you probably have to set some member values
        // to make all model getter methods work properly.
        initialize();
        
        // we don't want the default foreground color on text cells,
        // so we change it:
        m_textRenderer.setForeground(
                Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
    }
    
    // Content:
    public Object doGetContentAt(int col, int row) {
        //System.out.println("col "+col+" row "+row);
        String erg = (String) content.get(col + "/" + row);
        if (erg != null)
            return erg;
        return col + "/" + row;
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
        return 100+getFixedRowCount();
    }

    public int getFixedHeaderRowCount() {
        return 2;
    }

    public int doGetColumnCount() {
        return 100+getFixedColumnCount();
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

