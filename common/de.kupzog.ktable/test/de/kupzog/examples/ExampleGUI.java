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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellResizeListener;
import de.kupzog.ktable.KTableCellSelectionAdapter;
import de.kupzog.ktable.KTableCellSelectionListener;
import de.kupzog.ktable.KTableSortComparator;
import de.kupzog.ktable.KTableSortOnClick;
import de.kupzog.ktable.KTableSortedModel;
import de.kupzog.ktable.SWTX;

/**
 * KTable example GUI.<p>
 * Demonstrates some usages of KTable.
 */

public class ExampleGUI {
	public static void main(String[] args) {
		// create a shell...
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("KTable examples");
		
		// put a tab folder in it...
		TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		
		createTextTable(tabFolder);
		createBooleanTable(tabFolder);
		createSpanTable(tabFolder);
		createSortableTable(tabFolder);
        createFixedWidthTable(tabFolder);
		createColorPalette(tabFolder);
		createTownTable(tabFolder);
	
		// display the shell...
		shell.setSize(600,600);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

    /**
     * Creates a small table showing some nice photos from germany.
     * Not too much special here.
     */
    private static void createTownTable(TabFolder tabFolder) {
        // Item 3: Town table
		TabItem item4 = new TabItem(tabFolder, SWT.NONE);
		item4.setText("Towns");
		Composite comp4 = new Composite(tabFolder, SWT.NONE);
		item4.setControl(comp4);
		comp4.setLayout(new FillLayout());
		
		// put a table in tabItem3...
		final KTable table4 = new KTable(comp4, false, SWT.FULL_SELECTION | SWTX.AUTO_SCROLL | SWTX.FILL_WITH_LASTCOL);
		table4.setModel(new TownExampleModel());
    }

    /**
     * Creates a simple table with differently colored cells.
     * Shows how to react on a cell selection event.
     */ 
    private static void createColorPalette(TabFolder tabFolder) {
		TabItem item3 = new TabItem(tabFolder, SWT.NONE);
		item3.setText("Color Palette");
		Composite comp3 = new Composite(tabFolder, SWT.NONE);
		item3.setControl(comp3);
		comp3.setLayout(new FillLayout());
		
		// put a table in tabItem2...
		final KTable table2 = new KTable(comp3, false, SWT.FLAT);
		table2.setModel(new PaletteExampleModel());
		final Label label = new Label(comp3, SWT.NONE);
		label.setText("Click a cell...");
		table2.addCellSelectionListener(
			new KTableCellSelectionAdapter()
			{
				public void cellSelected(int col, int row, int statemask) {
					RGB rgb = (RGB)table2.getModel().getContentAt(col, row);
					label.setText("R: "+rgb.red+"\nG: "+rgb.green+"\nB: "+rgb.blue);
				}
			}
		);
    }

    /**
     * Creates a table that shows how to use the cell span feature.
     */
    private static void createSpanTable(TabFolder tabFolder) {
		TabItem itemS = new TabItem(tabFolder, SWT.NONE);
		itemS.setText("Span Table");
		Composite compS = new Composite(tabFolder, SWT.NONE);
		itemS.setControl(compS);
		compS.setLayout(new FillLayout());
		final KTable sTable = new KTable(compS, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.HIDE_SELECTION);
		sTable.setModel(new SpanModelExample());
		sTable.addCellSelectionListener(
			new KTableCellSelectionListener()
			{
			    public void cellSelected(int col, int row, int statemask) {
					System.out.println("Cell ["+col+";"+row+"] selected.");
				}
				
				public void fixedCellSelected(int col, int row, int statemask) {
					System.out.println("Header ["+col+";"+row+"] selected.");
				}

			}
		);
	
		sTable.addCellResizeListener(
			new KTableCellResizeListener()
			{
				public void columnResized(int col, int newWidth) {
					System.out.println("Column "+col+" resized to "+newWidth);
				}
				public void rowResized(int row, int newHeight) {
					System.out.println("Row "+row+" resized to "+newHeight);
				}
			}
		);
    }

    /**
     * Creates a table that displays boolean values as checkboxes.
     * That means the content of a normal cell is an image of a ceckbox, 
     * and the celleditor is a KTableCellEditorCheckbox2 instance. This means
     * that not the whole cell area is sensible to an editor activation, but only
     * the area where the ceckbox image is shown.<p>
     * Note: This table has a lot of cells and shows that the time needed to draw
     * such a complex figure can be quite long and thus visible.
     */
    private static void createBooleanTable(TabFolder tabFolder) {
		TabItem item2 = new TabItem(tabFolder, SWT.NONE);
		item2.setText("Boolean Table");
		Composite comp2 = new Composite(tabFolder, SWT.NONE);
		item2.setControl(comp2);
		comp2.setLayout(new FillLayout());
		final KTable dTable = new KTable(comp2, false, SWT.V_SCROLL | SWT.H_SCROLL | SWTX.MARK_FOCUS_HEADERS);
		dTable.setModel(new BooleanModelExample());
		dTable.addCellSelectionListener(
			new KTableCellSelectionListener()
			{
			    public void cellSelected(int col, int row, int statemask) {
					System.out.println("Cell ["+col+";"+row+"] selected.");
				}
				
				public void fixedCellSelected(int col, int row, int statemask) {
					System.out.println("Header ["+col+";"+row+"] selected.");
				}

			}
		);
	
		dTable.addCellResizeListener(
			new KTableCellResizeListener()
			{
				public void columnResized(int col, int newWidth) {
					System.out.println("Column "+col+" resized to "+newWidth);
				}
				public void rowResized(int row, int newHeight) {
					System.out.println("Row "+row+" resized to "+newHeight);
				}
			}
		);
    }
    
    /**
     * Constructs a table that demonstrates the usage of the fixed width model.
     * This model prevents the table from exceeding the horizontal space on the screen 
     *  - thus never scrolls horizontally. If a resize of a col happens, the other
     *  columns are shrinked/enlarged as needed. 
     */
    private static void createFixedWidthTable(TabFolder tabFolder) {
        TabItem item1 = new TabItem(tabFolder, SWT.NONE);
        item1.setText("FixeWidth Table");
        Composite comp1 = new Composite(tabFolder, SWT.NONE);
        item1.setControl(comp1);
        comp1.setLayout(new FillLayout());
        final KTable table = new KTable(comp1, false, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL 
                | SWTX.EDIT_ON_KEY);
        table.setModel(new FixedWidthModelExample(table));
        table.addCellSelectionListener(
            new KTableCellSelectionListener()
            {
                public void cellSelected(int col, int row, int statemask) {
                    System.out.println("Cell ["+col+";"+row+"] selected.");
                }
                
                public void fixedCellSelected(int col, int row, int statemask) {
                    System.out.println("Header ["+col+";"+row+"] selected.");
                }
            }
        );
    
        table.addCellResizeListener(
            new KTableCellResizeListener()
            {
                public void columnResized(int col, int newWidth) {
                    System.out.println("Column "+col+" resized to "+newWidth);
                }
                public void rowResized(int row, int newHeight) {
                    System.out.println("Row "+row+" resized to "+newHeight);
                }
            }
        );
    }
    

    /**
     * Constructs a simple text table. 
     * The table model is directly created from the interface KTableModel.
     * Editors are KTableCellEditorText and KTableCellEditorCombo.<p>
     * NOTE that this shows how to set an Excel-like mouse cursor for the 
     * KTable. If setCursor() is used, the cursor is not preserved and will 
     * be swiched back to the default cursor when a resize cursor or something
     * else is shown. 
     */
    private static void createTextTable(TabFolder tabFolder) {
		TabItem item1 = new TabItem(tabFolder, SWT.NONE);
		item1.setText("Text Table");
		Composite comp1 = new Composite(tabFolder, SWT.NONE);
		item1.setControl(comp1);
		comp1.setLayout(new FillLayout());
		final KTable table = new KTable(comp1, false, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL 
				| SWT.H_SCROLL | SWTX.FILL_WITH_LASTCOL | SWTX.EDIT_ON_KEY);
		table.setModel(new TextModelExample());
		table.addCellSelectionListener(
			new KTableCellSelectionListener()
			{
			    public void cellSelected(int col, int row, int statemask) {
					System.out.println("Cell ["+col+";"+row+"] selected.");
				}
				
				public void fixedCellSelected(int col, int row, int statemask) {
					System.out.println("Header ["+col+";"+row+"] selected.");
				}
			}
		);
	
		table.addCellResizeListener(
			new KTableCellResizeListener()
			{
				public void columnResized(int col, int newWidth) {
					System.out.println("Column "+col+" resized to "+newWidth);
				}
				public void rowResized(int row, int newHeight) {
					System.out.println("Row "+row+" resized to "+newHeight);
				}
			}
		);
		
		/**
		 *  Set Excel-like table cursors
		 */

        if ( SWT.getPlatform().equals("win32") ) {
			
			// Cross

			Image crossCursor = SWTX.loadImageResource(table.getDisplay(), "/icons/cross_win32.gif");
			
			// Row Resize
			
			Image row_resizeCursor = SWTX.loadImageResource(table.getDisplay(), "/icons/row_resize_win32.gif");
			
			// Column Resize
			
			Image column_resizeCursor  = SWTX.loadImageResource(table.getDisplay(), "/icons/column_resize_win32.gif");
		
			// we set the hotspot to the center, so calculate the number of pixels from hotspot to lower border:
			
			Rectangle crossBound        = crossCursor.getBounds();
			Rectangle rowresizeBound    = row_resizeCursor.getBounds();
			Rectangle columnresizeBound = column_resizeCursor.getBounds();
			
			Point crossSize        = new Point(crossBound.width/2, crossBound.height/2);
			Point rowresizeSize    = new Point(rowresizeBound.width/2, rowresizeBound.height/2);
			Point columnresizeSize = new Point(columnresizeBound.width/2, columnresizeBound.height/2);
			
			table.setDefaultCursor(new Cursor(table.getDisplay(), crossCursor.getImageData(), crossSize.x, crossSize.y), crossSize);
			table.setDefaultRowResizeCursor(new Cursor(table.getDisplay(), row_resizeCursor.getImageData(), rowresizeSize.x, rowresizeSize.y));
			table.setDefaultColumnResizeCursor(new Cursor(table.getDisplay(), column_resizeCursor.getImageData(), columnresizeSize.x, columnresizeSize.y));

		} else {
		
			// Cross
		
			Image crossCursor      = SWTX.loadImageResource(table.getDisplay(), "/icons/cross.gif");
			Image crossCursor_mask = SWTX.loadImageResource(table.getDisplay(), "/icons/cross_mask.gif");
			
			// Row Resize
		
			Image row_resizeCursor      = SWTX.loadImageResource(table.getDisplay(), "/icons/row_resize.gif");
			Image row_resizeCursor_mask = SWTX.loadImageResource(table.getDisplay(), "/icons/row_resize_mask.gif");
		
			// Column Resize
		
			Image column_resizeCursor      = SWTX.loadImageResource(table.getDisplay(), "/icons/column_resize.gif");
			Image column_resizeCursor_mask = SWTX.loadImageResource(table.getDisplay(), "/icons/column_resize_mask.gif");
	
			// we set the hotspot to the center, so calculate the number of pixels from hotspot to lower border:
		
			Rectangle crossBound        = crossCursor.getBounds();
			Rectangle rowresizeBound    = row_resizeCursor.getBounds();
			Rectangle columnresizeBound = column_resizeCursor.getBounds();
		
			Point crossSize        = new Point(crossBound.width/2, crossBound.height/2);
			Point rowresizeSize    = new Point(rowresizeBound.width/2, rowresizeBound.height/2);
			Point columnresizeSize = new Point(columnresizeBound.width/2, columnresizeBound.height/2);
		
			table.setDefaultCursor(new Cursor(table.getDisplay(), crossCursor_mask.getImageData(), crossCursor.getImageData(), crossSize.x, crossSize.y), crossSize);
			table.setDefaultRowResizeCursor(new Cursor(table.getDisplay(), row_resizeCursor_mask.getImageData(), row_resizeCursor.getImageData(), rowresizeSize.x, rowresizeSize.y));
			table.setDefaultColumnResizeCursor(new Cursor(table.getDisplay(), column_resizeCursor_mask.getImageData(), column_resizeCursor.getImageData(), columnresizeSize.x, columnresizeSize.y));
		
		}
		
    }
    
    /**
     * Constructs a simple text table that demonstrates the creation of a sorted table.
     * <p>
     * Note that the only thing that is necessary to make the table itself sortable is
     * subclassing the <code>KTableSortedModel</code>. Then the sort() method is available
     * to custom handlers.<p>
     * This shows how to register some common listeners that make the table behave in an 
     * often seen way.
     */
    private static void createSortableTable(TabFolder tabFolder) {
		TabItem item1 = new TabItem(tabFolder, SWT.NONE);
		item1.setText("Sortable Table");
		Composite comp1 = new Composite(tabFolder, SWT.NONE);
		item1.setControl(comp1);
		comp1.setLayout(new FillLayout());
		final KTable table = new KTable(comp1, false, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		final KTableSortedModel model = new SortableModelExample();
		table.setModel(model);
		table.addCellSelectionListener(
			new KTableCellSelectionListener()
			{
			    public void cellSelected(int col, int row, int statemask) {
			    	// the idea is to map the row index back to the model index since the given row index
			    	// changes when sorting is done. 
			        int modelRow = model.mapRowIndexToModel(row);
					System.out.println("Cell ["+col+";"+row+"] selected. - Model row: "+modelRow);
				}
				
				public void fixedCellSelected(int col, int row, int statemask) {
					System.out.println("Header ["+col+";"+row+"] selected.");
				}
			}
		);
		// implement resorting when the user clicks on the table header:
		table.addCellSelectionListener(new KTableSortOnClick(table, 
				new SortComparatorExample(model, -1, KTableSortComparator.SORT_NONE)));
	
		table.addCellResizeListener(
			new KTableCellResizeListener()
			{
				public void columnResized(int col, int newWidth) {
					System.out.println("Column "+col+" resized to "+newWidth);
				}
				public void rowResized(int row, int newHeight) {
					System.out.println("Row "+row+" resized to "+newHeight);
				}
			}
		);
		
		/**
		 *  Set Excel-like table cursors
		 */
		
        if ( SWT.getPlatform().equals("win32") ) {
			
			// Cross

			Image crossCursor = SWTX.loadImageResource(table.getDisplay(), "/icons/cross_win32.gif");
			
			// Row Resize
			
			Image row_resizeCursor = SWTX.loadImageResource(table.getDisplay(), "/icons/row_resize_win32.gif");
			
			// Column Resize
			
			Image column_resizeCursor  = SWTX.loadImageResource(table.getDisplay(), "/icons/column_resize_win32.gif");
		
			// we set the hotspot to the center, so calculate the number of pixels from hotspot to lower border:
			
			Rectangle crossBound        = crossCursor.getBounds();
			Rectangle rowresizeBound    = row_resizeCursor.getBounds();
			Rectangle columnresizeBound = column_resizeCursor.getBounds();
			
			Point crossSize        = new Point(crossBound.width/2, crossBound.height/2);
			Point rowresizeSize    = new Point(rowresizeBound.width/2, rowresizeBound.height/2);
			Point columnresizeSize = new Point(columnresizeBound.width/2, columnresizeBound.height/2);
			
			table.setDefaultCursor(new Cursor(table.getDisplay(), crossCursor.getImageData(), crossSize.x, crossSize.y), crossSize);
			table.setDefaultRowResizeCursor(new Cursor(table.getDisplay(), row_resizeCursor.getImageData(), rowresizeSize.x, rowresizeSize.y));
			table.setDefaultColumnResizeCursor(new Cursor(table.getDisplay(), column_resizeCursor.getImageData(), columnresizeSize.x, columnresizeSize.y));

		} else {
		
			// Cross
		
			Image crossCursor      = SWTX.loadImageResource(table.getDisplay(), "/icons/cross.gif");
			Image crossCursor_mask = SWTX.loadImageResource(table.getDisplay(), "/icons/cross_mask.gif");
			
			// Row Resize
		
			Image row_resizeCursor      = SWTX.loadImageResource(table.getDisplay(), "/icons/row_resize.gif");
			Image row_resizeCursor_mask = SWTX.loadImageResource(table.getDisplay(), "/icons/row_resize_mask.gif");
		
			// Column Resize
		
			Image column_resizeCursor      = SWTX.loadImageResource(table.getDisplay(), "/icons/column_resize.gif");
			Image column_resizeCursor_mask = SWTX.loadImageResource(table.getDisplay(), "/icons/column_resize_mask.gif");
	
			// we set the hotspot to the center, so calculate the number of pixels from hotspot to lower border:
		
			Rectangle crossBound        = crossCursor.getBounds();
			Rectangle rowresizeBound    = row_resizeCursor.getBounds();
			Rectangle columnresizeBound = column_resizeCursor.getBounds();
		
			Point crossSize        = new Point(crossBound.width/2, crossBound.height/2);
			Point rowresizeSize    = new Point(rowresizeBound.width/2, rowresizeBound.height/2);
			Point columnresizeSize = new Point(columnresizeBound.width/2, columnresizeBound.height/2);
		
			table.setDefaultCursor(new Cursor(table.getDisplay(), crossCursor_mask.getImageData(), crossCursor.getImageData(), crossSize.x, crossSize.y), crossSize);
			table.setDefaultRowResizeCursor(new Cursor(table.getDisplay(), row_resizeCursor_mask.getImageData(), row_resizeCursor.getImageData(), rowresizeSize.x, rowresizeSize.y));
			table.setDefaultColumnResizeCursor(new Cursor(table.getDisplay(), column_resizeCursor_mask.getImageData(), column_resizeCursor.getImageData(), columnresizeSize.x, columnresizeSize.y));
		
		}
			
    }

}
