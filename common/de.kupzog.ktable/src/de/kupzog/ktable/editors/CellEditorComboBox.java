/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
    
    Authors: Friederich Kupzog
             Lorenz Maierhofer
             markok
    fkmk@kupzog.de
    www.kupzog.de/fkmk
*/

package de.kupzog.ktable.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;

/**
 * A combobox cell editor. Uses standard SWT combo, has improved event handling
 * for tables used in dialogs (see TraverseListener).
 * 
 * @author Friederich Kupzog, markok
 */
public class CellEditorComboBox extends KTableCellEditor {
    private Cursor m_ArrowCursor;
    private Combo m_Combo;
    private String m_items[];
    private String m_defaultValue;
    
    
    private KeyAdapter keyListener = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            try {
                onKeyPressed(e);
            } catch (Exception ex) {
                System.err.println("Error in CellEditorComboBox: " + ex);
                ex.printStackTrace();
            }
       }
    };
    
    
    private TraverseListener travListener = new TraverseListener() {
        public void keyTraversed(TraverseEvent e) {
            if (e.keyCode == SWT.CR  ||  e.keyCode == SWT.KEYPAD_CR  ||  e.keyCode == SWT.ESC) {
                onTraverse(e);
                // when table is opened in dialog, prevent ENTER propagating to dialog,
                // because it closes the dialog! 
                e.doit = false;
            } else {
                // handle the event within the combo widget!
                onTraverse(e);
            }
        }
    };


    // ATTENTION: Before the col/row parameters were mis-labeled.
    @Override
    public void open(KTable table, int col, int row, Rectangle rect) {
        m_ArrowCursor = new Cursor(Display.getDefault(), SWT.CURSOR_ARROW);
        super.open(table, col, row, rect);
        String content = m_Model.getContentAt(m_Col, m_Row).toString();
        if (m_defaultValue != null  &&  content != null  &&  content.isEmpty()) {
            content = decorateDefaultValue(m_defaultValue);
        }
        m_Combo.setText(content);
        m_Combo.setSelection(new Point(0, content.length()));
    }


    static public String decorateDefaultValue(String defaultValue) {
        return "__" + defaultValue + "__";
    }

    
    @Override
    public void showList() {
        m_Combo.setListVisible(true);
    }

    
    @Override
    public void close(boolean save) {
        
        if (save) {
            String text = m_Combo.getText();
            if (m_defaultValue != null  &&  m_Combo.getSelectionIndex() == 0) { // is default value?
                text = "";
            }
            m_Model.setContentAt(m_Col, m_Row, text);
        }
        
        m_Combo.removeKeyListener(keyListener);
        m_Combo.removeTraverseListener(travListener);
        super.close(save);
        m_Combo = null;
        m_ArrowCursor.dispose();
    }

    
    protected Control createControl() {
        m_Combo = new Combo(m_Table, SWT.READ_ONLY | SWT.DROP_DOWN);
        m_Combo.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        
        if (m_items != null) {
            m_Combo.setItems(m_items);
        }
        
        m_Combo.addKeyListener(keyListener);        
        m_Combo.addTraverseListener(travListener);
        
        m_Combo.setCursor(m_ArrowCursor);
        return m_Combo;
    }
    
    /**
     * Overwrite the onTraverse method to ignore arrowup and arrowdown
     * events so that they get interpreted by the editor control.<p>
     * Comment that out if you want the up and down keys move the editor.<br>
     * Hint by David Sciamma.
     */
    protected void onTraverse(TraverseEvent e)
    {
        // set selection to the appropriate next element:
        switch (e.keyCode)
        {
            case SWT.ARROW_UP: // Go to previous item
            case SWT.ARROW_DOWN: // Go to next item
            {
                // Just don't treat the event
                break;
            }
            default: {
                super.onTraverse(e);
                break;
            }
        }
    } 
    
    
    public void setBounds(Rectangle rect) 
    {
        super.setBounds(new Rectangle(rect.x, rect.y+1,
                                      rect.width, rect.height-2));
    }


    /**
     * 
     * @param items combo box items
     * @param defaultValue if not null, default value is decorated with double
     * underscores at start and end of string, and if it is selected in combobox,
     * an empty string is returned.
     */
    public void setItems(String items[], String defaultValue) {
        m_items = items;
        m_defaultValue = defaultValue;
    }

    
    /* (non-Javadoc)
     * @see de.kupzog.ktable.KTableCellEditor#setContent(java.lang.String)
     */
    public void setContent(Object content) {
       if (content instanceof Integer) {
           m_Combo.select(((Integer)content).intValue());
       } else if (content instanceof String) {
           setSelectionToClosestMatch((String)content);
       } else {
           setSelectionToClosestMatch(content.toString());
       }
    }

    
    private void setSelectionToClosestMatch(String content) {
        content = content.toLowerCase();
        
        String[] citems = m_Combo.getItems();
        String[] items = new String[citems.length];
        for (int i=0; i<citems.length; i++)
            items[i] = citems[i].toLowerCase();
        
        for (int length=content.length(); length>=0; length--) {
            String part = content.substring(0, length);
            for (int i=0; i<items.length; i++) 
                if (items[i].startsWith(part)) {
                    m_Combo.select(i);
                    return;
                }
        }
    }
}
