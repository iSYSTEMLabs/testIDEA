/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 
 Authors: 
 Friederich Kupzog,  fkmk@kupzog.de, www.kupzog.de/fkmk
 Lorenz Maierhofer, lorenz.maierhofer@logicmindguide.com
 Marko Klopcic, marko.klopcic@isystem.si
 
  TODO: Add keyboard shortcuts Ctrl+Space and Shift+Space to toggle column
  or row selection mode
 */

package de.kupzog.ktable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.TextCellRenderer;



/**
 * Custom drawn tabel widget for SWT GUIs.
 * <p>
 * The idea of KTable is to have a flexible grid of cells to display data in it.
 * The class focuses on displaying data and not on collecting the data to
 * display. The latter is done by the <code>KTableModel</code> which has to be implemented
 * for each specific case. Some default tasks are done by a base implementation
 * called <code>KTableDefaultModel</code>. Look also into <code>KTableSortedModel</code> that provides 
 * a transparent sorting of cells.<br>
 * The table asks the table model for the amount of
 * columns and rows, the sizes of columns and rows and for the content of the
 * cells which are currently drawn. Even if the table has a million rows, it
 * won't get slower because it only requests those cells it currently draws.
 * Only a bad table model can influence the drawing speed negatively.
 * <p>
 * When drawing a cell, the table calls a <code>KTableCellRenderer</code> to do this work.
 * The table model determines which cell renderer is used for which cell. A
 * default renderer is available (<code>KTableCellRenderer.defaultRenderer</code>), but the
 * creation of self-written renderers for specific purposes is assumed. 
 * Some default renderers are available in the package <code>de.kupzog.ktable.cellrenderers.*</code>.
 * <p>
 * KTable allows to F columns and rows. Each column can have an individual
 * size while the rows are all of the same height except the first row. Multiple
 * column and row headers are possible. These "fixed" cells will not be scrolled
 * out of sight. The column and row count always starts in the upper left corner
 * with 0, independent of the number of column headers or row headers.
 * <p>
 * It is also possible to span cells over several rows and/or columns. The KTable
 * asks the model do provide this information via <code>belongsToCell(col, row)</code>.
 * This method must return the cell the given cell should be merged with.
 * <p>
 * Changing of model values is possible by implementations of <code>KTableCellEditor</code>.
 * Again the KTable asks the model to provide an implementation. Note that there
 * are multiple celleditors available in the package <code>de.kupzog.ktable.editors</code>!
 * 
 * @author Friederich Kupzog
 * @see de.kupzog.ktable.KTableModel
 * @see de.kupzog.ktable.KTableDefaultModel
 * @see de.kupzog.ktable.KTableSortedModel
 * @see de.kupzog.ktable.KTableCellRenderer
 * @see de.kupzog.ktable.KTableCellEditor
 * @see de.kupzog.ktable.KTableCellSelectionListener
 *  
 */
public class KTable extends Canvas {    
    
    
    // Data and data editing facilities:
    protected KTableModel m_model;
    protected KTableCellEditor m_cellEditor;
    protected ICommandListener m_commandListener;
    
    // current visible:
    protected int m_TopRow;
    protected int m_LeftColumn;

    // Selection - see also description of method focusCell()
    public enum 
        ESelectionMode {ECell, // cell selection mode - more than one cell is 
                               // selected
                        EColumn, // column selection mode, at least on column is already selected
                        ERow, // row selection mode, at least on row is already selected
                        };
    private ESelectionMode m_selectionMode = ESelectionMode.ECell; 
    protected Set<SelectionItem> m_selections;
    protected int m_focusRow;
    protected int m_focusCol;
    protected int m_mainFocusRow; 
    protected int m_mainFocusCol;
    protected int m_clickColumnIndex;
    protected int m_clickRowIndex;
    private int m_style=SWT.NONE;
    
    // important measures
    protected int m_rowsVisible;
    protected int m_rowsFullyVisible;
    protected int m_columnsVisible;
    protected int m_columnsFullyVisible;

    // column sizes
    protected int m_resizeColumnIndex;
    protected int m_resizeColumnLeft;
    protected int m_resizeRowIndex;
    protected int m_resizeRowTop;
    protected boolean m_capture = false;

	// colors
	protected Color m_colorTopBorder;
	protected Color m_colorLeftBorder;
	protected Color m_colorRightBorder;

	// resize area
    final protected int m_resizeAreaSize = 8;
    
    // diverse
    protected Display m_display;
    protected ArrayList<KTableCellSelectionListener> m_cellSelectionListeners;
    protected ArrayList<KTableCellDoubleClickListener> m_cellDoubleClickListeners;
    protected ArrayList<KTableCellResizeListener> m_cellResizeListeners;
    protected ArrayList<KTableClickInterceptionListener> m_cellClickListeners;
    protected Cursor m_defaultCursor;
    protected Point  m_defaultCursorSize;
    protected Cursor m_defaultRowResizeCursor;
    protected Cursor m_defaultColumnResizeCursor;
    protected String m_nativTooltip;
    protected KTableCursorProvider m_cursorProvider;
    protected int m_numRowsVisibleInPreferredSize = 1;
    protected int m_numColsVisibleInPreferredSize = 1;
    protected int m_preferredSizeDefaultRowHeight = 0;
    private boolean m_isShowTooltips;
    
    /*
     * this number of rows is examined in addition to the visible area when optimizing
     * column width; counts both upwards and downwards.
     */
    private static final int INVISIBLE_ROWS_TO_REGARD_DURING_WIDTH_OPTIMIZATION = 50;

    //////////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR & DISPOSE
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new KTable.
     * 
     * @param isShowTooltips if true, internal tooltip listener is initialized
     *                       and shows tooltips returned by KModel set by setModel().
     *                       If you intend to provide your own tooltip listener, set this to false. 
     * possible styles: 
     * <ul>
     * <li><b>SWT.V_SCROLL</b> - show vertical scrollbar and allow vertical scrolling by arrow keys</li>
     * <li><b>SWT.H_SCROLL</b> - show horizontal scrollbar and allow horizontal scrolling by arrow keys</li>
     * <li><b>SWTX.AUTO_SCROLL</b> - Dynamically shows vertical and horizontal scrollbars when they are necessary.</li>
     * <li><b>SWTX.FILL_WITH_LASTCOL</b> - Makes the table enlarge the last column to always fill all space.</li>
     * <li><b>SWTX.FILL_WITH_DUMMYCOL</b> - Makes the table fill any remaining space with dummy columns to fill all space.</li>
     * <li><b>SWT.FLAT</b> - Does not paint a dark outer border line.</li>
     * <li><b>SWT.MULTI</b> - Sets the "Multi Selection Mode".
     * In this mode, more than one cell or row can be selected.
     * The user can achieve this by shift-click and ctrl-click.
     * The selected cells/rows can be scattored ofer the complete table.
     * If you pass false, only a single cell or row can be selected.
     * This mode can be combined with the "Row Selection Mode".</li>
     * <li><b>SWT.FULL_SELECTION</b> - Sets the "Full Selection Mode".
     * In the "Full Selection Mode", the table always selects a complete row.
     * Otherwise, each individual cell can be selected.
     * This mode can be combined with the "Multi Selection Mode".</li>
     * <li><b>SWTX.EDIT_ON_KEY</b> - Activates a possibly present cell editor
     * on every keystroke. (Default: only ENTER). However, note that editors
     * can specify which events they accept.</li>
     * <li><b>SWTX.MARK_FOCUS_HEADERS</b> - Makes KTable draw left and top header cells
     * in a different style when the focused cell is in their row/column.
     * This mimics the MS Excel behavior that helps find the currently
     * selected cell(s).</li>
     * <li><b>SWT.HIDE_SELECTION</b> - Hides the selected cells when the KTable
     * looses focus.</li>
     * After creation a table model should be added using setModel().
     */
    public KTable(Composite parent, boolean isShowTooltips, int style) {
        // Initialize canvas to draw on.
        super(parent, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE | style);
        
        m_isShowTooltips = isShowTooltips;
        m_style = style;
 
        //inits
        m_display = Display.getCurrent();
        m_selections = new TreeSet<>();  // we want to have selections sorted
        m_cellEditor = null;

        m_TopRow = 0;
        m_LeftColumn = 0;
        m_focusRow = 0;
        m_focusCol = 0;
        m_rowsVisible = 0;
        m_rowsFullyVisible = 0;
        m_columnsVisible = 0;
        m_columnsFullyVisible = 0;
        m_resizeColumnIndex = -1;
        m_resizeRowIndex = -1;
        m_resizeRowTop = -1;
        m_resizeColumnLeft = -1;
        m_clickColumnIndex = -1;
        m_clickRowIndex = -1;

        m_cellSelectionListeners = new ArrayList<>(10);
        m_cellDoubleClickListeners = new ArrayList<>(10);
        m_cellResizeListeners = new ArrayList<>(10);
        m_cellClickListeners = new ArrayList<>(10);

        // Listener creation
        createListeners();
        
        // handle tooltip initialization:
	    m_nativTooltip = super.getToolTipText();
	    super.setToolTipText("");
	    
	    // apply various style bits:
	    if ((style & SWTX.AUTO_SCROLL) == SWTX.AUTO_SCROLL) {
	        addListener(SWT.Resize, new Listener() {
	            public void handleEvent(Event event) {
	                updateScrollbarVisibility();
	            }
	        });
            addCellResizeListener(new KTableCellResizeListener() {
                public void rowResized(int row, int newHeight) {
                    updateScrollbarVisibility();
                }
                public void columnResized(int col, int newWidth) {
                    updateScrollbarVisibility();   
                    }
            });
	    }	    

		// determine default border colors:
		if ((getStyle() & SWT.FLAT) == 0)
			m_colorTopBorder = m_display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
		else
			m_colorTopBorder = m_display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		m_colorRightBorder = m_colorLeftBorder = m_colorTopBorder;
    }

    
    @Override
    public void dispose() {

    	if (m_defaultCursor != null)
            m_defaultCursor.dispose();
        
    	if (m_defaultRowResizeCursor != null)
    		m_defaultRowResizeCursor.dispose();
    	
       	if (m_defaultColumnResizeCursor != null)
    		m_defaultColumnResizeCursor.dispose();

       	super.dispose();

    }

    protected void createListeners() {

        addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent event) {
                onPaint(event);
            }
        });

        addControlListener(new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                redraw();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                onMouseDown(e);
            }

            public void mouseUp(MouseEvent e) {
                onMouseUp(e);
            }

            public void mouseDoubleClick(MouseEvent e) {
                onMouseDoubleClick(e);
            }
        });

        addMouseMoveListener(new MouseMoveListener() {
            public void mouseMove(MouseEvent e) {
                onMouseMove(e);
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                onKeyDown(e);
            }
        });
        
        /*
         * MK: Did not find why this listener is needed (cells repaint nicely
         * also without this listener). Uncomment if needed. 
        addCellSelectionListener(new KTableCellSelectionAdapter() {
            
            private Point[] oldSelections;
            
            public void cellSelected(int col, int row, int statemask) {
                if (isHighlightSelectionInHeader() && (statemask & SWT.SHIFT) == 0) {
					Point[] selections = getCellSelection();
					GC gc = new GC(KTable.this);

					repaintRelevantCells(gc, oldSelections);
					repaintRelevantCells(gc, selections);
					
					gc.dispose();
					oldSelections = selections;
                }
        	}
            
			private void repaintRelevantCells(GC gc, Point[] selections) {
			    
			    if (selections == null) {
			        return;
			    }
			    
			    Rectangle bounds = getClientArea();
                Rectangle oldClipping = gc.getClipping();
                int fixedWidth =  0;
                int fixedHeight = 0;
                
                for (int k = 0; k < m_model.getFixedHeaderColumnCount(); k++) {
                    fixedWidth += getCellRectIgnoreSpan(k, 0).width + 1;
                }
                
                for (int k = 0; k < m_model.getFixedHeaderRowCount(); k++) {
                    fixedHeight += getCellRectIgnoreSpan(0, k).height + 1;
                }
                
                for (int i = 0; i < selections.length; i++) {
			        int col = selections[i].x; 
                    int row = selections[i].y;
                    for (int j = 0; j < getModel().getFixedHeaderColumnCount(); j++) {
                        Point valid = getValidCell(j, row);
                        // allow painting of GC only on columns, not on rows:
                        Rectangle rowClip = new Rectangle(1, 
                                                          1 + fixedHeight,
                                                          fixedWidth,
                                                          bounds.height - 1 - fixedHeight);                        
                        rowClip.intersect(oldClipping);
                        gc.setClipping(rowClip);
                        drawCell(gc, valid.x, valid.y);
                    }
                    for (int j = 0; j < getModel().getFixedHeaderRowCount(); j++) {
                        Point valid = getValidCell(col, j);
                        // allow painting of GC only on rows, not on cols:
                        Rectangle rowClip = new Rectangle(1 + fixedWidth,
                                                          1,
                                                          bounds.width - 1 - fixedWidth,
                                                          fixedHeight);                        
                       
                        rowClip.intersect(oldClipping);
                        gc.setClipping(rowClip);
                        drawCell(gc, valid.x, valid.y);
                    }
                    gc.setClipping(oldClipping);
			    }
			}
        }); */
        
        addFocusListener(new FocusListener() {
            
		    private Point[] oldSelection;
            public void focusGained(FocusEvent e) {
               if (!isShowSelectionWithoutFocus()  &&  oldSelection != null) {
                   setSelection(oldSelection, false);
                   for (int i = 0; i < oldSelection.length; i++) 
                       updateCell(oldSelection[i].x, oldSelection[i].y);
                   oldSelection = null;
               }
            }

            public void focusLost(FocusEvent e) {
                if (!isShowSelectionWithoutFocus()) {
                    oldSelection = getCellSelection();
                    clearSelection();
                    if (oldSelection != null) {
                        for (int i = 0; i < oldSelection.length; i++) { 
                            updateCell(oldSelection[i].x, oldSelection[i].y);
                        }
                    }
                }
            }
		    
		});
        
        TooltipListener tooltipListener = null;
        if (m_isShowTooltips) {
            tooltipListener = new TooltipListener();
            addListener (SWT.Dispose, tooltipListener);
            addListener (SWT.KeyDown, tooltipListener);
            addListener (SWT.MouseDown, tooltipListener);
            addListener (SWT.MouseDoubleClick, tooltipListener);
            addListener (SWT.MouseMove, tooltipListener);
            addListener (SWT.MouseHover, tooltipListener);
            addListener (SWT.MouseExit, tooltipListener);
        }
    	
    	if (getVerticalBar() != null) {
    	    getVerticalBar().addSelectionListener(new SelectionAdapter() {
    	        public void widgetSelected(SelectionEvent e) {
    	            int oldTopRow = m_TopRow;
    	            m_TopRow = getVerticalBar().getSelection() + getFixedRowCount();
    	            if (oldTopRow != m_TopRow) {
    	                redraw();
    	            }
    	        }
    	        
    	    });
    	    if (tooltipListener != null) {
    	        getVerticalBar().addListener(SWT.Selection, tooltipListener);
    	    }
    	}
    	
    	if (getHorizontalBar() != null) {
    	    
    	    getHorizontalBar().addSelectionListener(new SelectionAdapter() {
    	        
    	        public void widgetSelected(SelectionEvent e) {
    	            int oldLeftCol = m_LeftColumn;
    	            m_LeftColumn = getHorizontalBar().getSelection() + getFixedColumnCount();
    	            if (oldLeftCol!=m_LeftColumn)
    	                redraw();
    	        }
    	    });
    	    
            if (tooltipListener != null) {
                getHorizontalBar().addListener(SWT.Selection, tooltipListener);
            }
    	}
    }
    
    //////////////////////////////////////////////////////////////////////////////
    // CALCULATIONS
    //////////////////////////////////////////////////////////////////////////////

    protected int getFixedWidth() {
        int width = 0;
        for (int i = 0; i < getFixedColumnCount(); i++) {
            width += getColumnWidth(i);
        }
        return width;
    }
    
    
    protected int getHeaderWidth() {
        int width = 0;
        for (int i = 0; i < m_model.getFixedHeaderColumnCount(); i++) {
            width += getColumnWidth(i);
        }
        return width;
    }

    
    protected int getColumnLeft(int index) {
        
        if (index < getFixedColumnCount()) {
            int x = 0;
            for (int i = 0; i < index; i++) {
                x += getColumnWidth(i);
            }
            return x;
        }
        
        // regular data area:
		if (index < m_LeftColumn) {
			return getFixedWidth();
		}
		
		if (index > m_LeftColumn + m_columnsVisible) {
			return -1;
		}
		
        int x = getFixedWidth();
        
        for (int i = m_LeftColumn; i < index; i++) {
            x += getColumnWidth(i);
        }
        
        return x;
    }

    
    protected int getColumnRight(int index) {
        
        if (index < 0) {
            return 0;
        }
        
        return getColumnLeft(index) + getColumnWidth(index);
    }
    
    
    protected int getRowBottom(int index) {

    	if (index < 0) {
    		return 0;
    	}
    	
    	int y = getFixedHeight();
    	for ( int i = m_TopRow; i <= index; i++) {
    	   y += m_model.getRowHeight(i);
    	}
    	
    	return y;

    }
    private int getFixedHeight() {
    	int height = 1;
    	for (int i=0; i<getFixedRowCount(); i++) {
    		height+=m_model.getRowHeight(i);
    	}
    	return height;
    }
    
    /**
     * @return Returns the number of visible rows in the table.
     * This always contains the fixed rows, and adds the number of 
     * rows that fit in the rest based on the currently shown top-row.
     */
    public int getVisibleRowCount() {
        if (m_model==null)
            return 0;
        return getFixedRowCount()+getFullyVisibleRowCount(getFixedHeight());
    }
   
    private int getFullyVisibleRowCount(int fixedHeight) {
    	Rectangle rect = getClientArea();
        
    	int count = 0;
    	int heightSum = fixedHeight;
    	for (int i=m_TopRow; heightSum<rect.height && i < m_model.getRowCount(); i++) {
			int rowHeight = m_model.getRowHeight(i);
			if (heightSum + rowHeight <= rect.height) {
				count++;
				heightSum += rowHeight;
			} else {
				break;
			}
    	}
    	return count;
    }

	private int getFullyVisibleRowCountAtEndOfTable() {
		Rectangle rect = getClientArea();

		int count = 0;
		int heightSum = getFixedHeight();
		for (int i = m_model.getRowCount() - 1; heightSum < rect.height && i >= getFixedRowCount(); i--) {
			int rowHeight = m_model.getRowHeight(i);
			if (heightSum + rowHeight <= rect.height) {
				count++;
				heightSum += rowHeight;
			} else {
				break;
			}
		}
		return count;
	}

	private int getFullyVisibleColCountAtEndOfTable() {
		Rectangle rect = getClientArea();

		int count = 0;
		int widthSum = getFixedWidth();
		for (int i = m_model.getColumnCount() - 1; widthSum < rect.width && i >= getFixedColumnCount(); i--) {
			int colWidth = m_model.getColumnWidth(i);
			if (widthSum + colWidth <= rect.width) {
				count++;
				widthSum += colWidth;
			} else {
				break;
			}
		}
		return count;
	}

	protected void doCalculations() {
        if (m_model == null) {
            ScrollBar sb = getHorizontalBar();
            if (sb != null) {
                sb.setMinimum(0);
                sb.setMaximum(1);
                sb.setPageIncrement(1);
                sb.setThumb(1);
                sb.setSelection(1);
            }
            sb = getVerticalBar();
            if (sb != null) {
                sb.setMinimum(0);
                sb.setMaximum(1);
                sb.setPageIncrement(1);
                sb.setThumb(1);
                sb.setSelection(1);
            }
            return;
        }

        Rectangle rect = getClientArea();
        if (m_LeftColumn < getFixedColumnCount()) {
            m_LeftColumn = getFixedColumnCount();
        }
        if (m_LeftColumn > m_model.getColumnCount())
            m_LeftColumn=0;

        if (m_TopRow < getFixedRowCount()) {
            m_TopRow = getFixedRowCount();
        }
        if (m_TopRow > m_model.getRowCount()) 
            m_TopRow = 0;
        
        /*
		 * If all columns or all rows are fully visible, we must make sure that the table is scrolled completely to
		 * the left and top. Otherwise, it can happen that the user scrolls to the right / bottom, then increases
		 * window size which leads to the scrollbars being deactivated but the table still scrolled to the right /
		 * bottom with no way to access the left / top because the scrollbars are deactivated.
		 */
        boolean allColumnsFullyVisible = getFullyVisibleColCountAtEndOfTable() == m_model.getColumnCount()
				- getFixedColumnCount();
        boolean allRowsFullyVisible = getFullyVisibleRowCountAtEndOfTable() == m_model.getRowCount()
				- getFixedRowCount();
        if (allColumnsFullyVisible) {
        	m_LeftColumn = getFixedColumnCount();
        }
        if (allRowsFullyVisible) {
        	m_TopRow = getFixedRowCount();
        }

        int fixedHeight = getFixedHeight();
        m_columnsVisible = 0;
        m_columnsFullyVisible = 0;

        if (m_model.getColumnCount() > getFixedColumnCount()) {
            int runningWidth = getColumnLeft(m_LeftColumn);
            for (int col = m_LeftColumn; col < m_model.getColumnCount(); col++) {
                if (runningWidth < rect.width + rect.x)
                    m_columnsVisible++;
                runningWidth += getColumnWidth(col);
                if (runningWidth < rect.width + rect.x)
                    m_columnsFullyVisible++;
                else
                    break;
            }
        }

        ScrollBar sb = getHorizontalBar();
        if (sb != null) {
            if (m_model.getColumnCount() <= getFixedColumnCount() || allColumnsFullyVisible) {
                sb.setMinimum(0);
                sb.setMaximum(1);
                sb.setPageIncrement(1);
                sb.setThumb(1);
                sb.setSelection(1);
            } else {
            	/*
				 * Always set minimum to 0 to avoid scrollbar bug on Windows XP where the scrollbar would be
				 * scrolled to the right despite the minimum being equal to the selection.
				 */
                sb.setMinimum(0);
                sb.setMaximum(m_model.getColumnCount() - getFullyVisibleColCountAtEndOfTable() + 5
						- getFixedColumnCount());
                sb.setIncrement(1);
                sb.setPageIncrement(1);
                sb.setThumb(5);
                sb.setSelection(m_LeftColumn - getFixedColumnCount());
            }
        }

        m_rowsFullyVisible = getFullyVisibleRowCount(fixedHeight);
        m_rowsFullyVisible = Math.min(m_rowsFullyVisible, m_model.getRowCount()
                - getFixedRowCount());
        m_rowsFullyVisible = Math.max(0, m_rowsFullyVisible);

        m_rowsVisible = m_rowsFullyVisible + 1;

        if (m_TopRow + m_rowsFullyVisible > m_model.getRowCount()) {
            m_TopRow = Math.max(getFixedRowCount(), 
                    m_model.getRowCount()- m_rowsFullyVisible);
        }

        if (m_TopRow + m_rowsFullyVisible >= m_model.getRowCount()) {
            m_rowsVisible--;
        }

        sb = getVerticalBar();
        if (sb != null) {
            if (m_model.getRowCount() <= getFixedRowCount() || allRowsFullyVisible) {
                sb.setMinimum(0);
                sb.setMaximum(1);
                sb.setPageIncrement(1);
                sb.setThumb(1);
                sb.setSelection(1);
            } else {
            	/*
				 * Always set minimum to 0 to avoid scrollbar bug on Windows XP where the scrollbar would be
				 * scrolled to the right despite the minimum being equal to the selection.
				 */
                sb.setMinimum(0);
                sb.setMaximum(m_model.getRowCount() - getFullyVisibleRowCountAtEndOfTable() + 5 - getFixedRowCount());
                sb.setPageIncrement(m_rowsVisible - getFixedRowCount());
                sb.setIncrement(1);
                sb.setThumb(5);
                sb.setSelection(m_TopRow - getFixedRowCount());
            }
        }
    }

    /**
     * Returns the area that is occupied by the given cell. Does not
     * take into account any cell span.
     * @param col
     * @param row
     * @return Rectangle
     */
    protected Rectangle getCellRectIgnoreSpan(int col, int row) {
        return getCellRectIgnoreSpan(col, row, getColumnLeft(col) + 1);
    }
    
    /**
     * Returns the area that is occupied by the given cell. Does not
     * take into account any cell span.<p>
     * This version is an optimization if the x value is known. Use the
     * version with just col and row as parameter if this is not known.
     * @param col The column index
     * @param row The row index
     * @param x_startValue The right horizontal start value - if this is not 
     * known, there is a version without this parameter! Note that this is the
     * end value of the last cell + 1 px border.  
     * @return Returns the area of a cell
     */
    protected Rectangle getCellRectIgnoreSpan(int col, int row, int x_startValue) {
        if ((col < 0) || (col >= m_model.getColumnCount()))
            return new Rectangle(-1, -1, 0, 0);

        int x = x_startValue;
        int y;

        
        y = getYforRow(row);
        if (row>=getFixedRowCount() && row<m_TopRow) {
			y = getFixedHeight();
        }
        // take into account 1px bottom and right border that
        // belongs to the cell and is rendered by the cell renderer
        // TODO: Make lines separately configured and separately drawn items
        int width = getColumnWidth(col) - 1;
        int height = m_model.getRowHeight(row) - 1;
               
        return new Rectangle(x, y, width, height);
    }
    
    private int getRowForY(int y) {
    	int rowSum = 1;
    	for (int i = 0; i < getFixedRowCount(); i++) {
    		int height = m_model.getRowHeight(i); 
    		if (rowSum <= y  &&  (rowSum + height) > y) {
    		    return i;
    		}
    		rowSum += height;
    	}
    	for (int i = m_TopRow; i < m_model.getRowCount(); i++) {
    		int height = m_model.getRowHeight(i); 
    		if (rowSum <= y && (rowSum + height) > y) {
    		    return i;
    		}
    		rowSum += height;
    	}
    	return -1;
    }
    
    private int getYforRow(int row) {
    	if (row == 0)
    		return 1;
    	
		int y;
		if (row < getFixedRowCount()) {
			y = 1;
			for (int i = 0; i < row; i++) {
				y += m_model.getRowHeight(i);
			}
    	}
    	else {
    		y = getFixedHeight();
    		for (int i=m_TopRow; i<row; i++) {
    			y+=m_model.getRowHeight(i);
    		}
    	}
    	return y;
    }
    
    
    private Rectangle getCellRect(int col, int row, int left_X) {
        Rectangle bound = getCellRectIgnoreSpan(col, row, left_X);

        // determine the cells that are contained:
        bound.width += calculateExtraCellWidth(col, row, /* in pixels */true);
		bound.height += calculateExtraCellHeight(col, row, /* in pixels */true);
		
		// limit cell area to the visible table area
// we should do this but it is too slow...
//		bound.intersect(getClientArea());
		
        return bound;
    }
    
    /**
     * Returns the area that is occupied by the given cell. 
     * Respects cell span.
     * @param col the column index
     * @param row the row index
     * @return returns the area the cell content is drawn at.
     * @throws IllegalArgumentException if the given cell is not a cell
     * that is visible, but overlapped by a spanning cell. Call getValidCell()
     * first to ensure it is a visible cell.
     */
    public Rectangle getCellRect(int col, int row) {
        checkWidget();
        
        // be sure that it is a valid cell that is not overlapped by some other:
        Point valid = getValidCell(col, row);
        if(valid.x!=col || row!=valid.y)
            return new Rectangle(0,0,0,0);
            //throw new IllegalArgumentException("The cell bounds of an overlapped, " +
            //		"non-visible cell were requested: "+col+", "+row);
        
        Rectangle bound = getCellRectIgnoreSpan(col, row);
        // determine the cells that are contained:
		bound.width += calculateExtraCellWidth(col, row, /* in pixels */true);
		bound.height += calculateExtraCellHeight(col, row, /* in pixels */true);
		
		// limit cell area to the visible table area
// we should do this but it is too slow...
//		bound.intersect(getClientArea());
		
        return bound;
    }

	/**
	 * Calculates the extra <i>visible</i> width of the cell due to cell merging, both in pixels and in number of
	 * columns.
	 * 
	 * @param col
	 *            The column index of the cell.
	 * @param row
	 *            The row index of the cell.
	 * @param resultInPixels
	 *            If <code>true</code>, returns the width in pixels. If <code>false</code>, returns the result in
	 *            number of columns.
	 * @return The width, unit depending on the parameter <code>resultInPixels</code>.
	 */
	private int calculateExtraCellWidth(int col, int row, boolean resultInPixels) {
		// check: only the root cells of merged cells are actually drawn, so all other cells have span width of 0
		Point valid = getValidCell(col, row);
		if (valid.x != col || valid.y != row) {
			return 0;
		}

		// we determine the width of the cell both in pixels and in number of columns
		int widthInPx = 0, widthInColumns = 0;

		// check adjacent cells (towards the right) to see if they are merged with this cell
		int i;
		for (i = 1; col + i < m_model.getColumnCount() && getValidCell(col + i, row).equals(new Point(col, row)); i++) {
			/*
			 * if we get to the border of the fixed cell block, we must stop counting; otherwise, we run into problems
			 * when cells are merged across this border and we start scrolling
			 */
			if (col < getFixedColumnCount() && col + i >= getFixedColumnCount()) {
				break;
			}
			widthInPx += getColumnWidth(col + i);
			widthInColumns = i;
		}

		/*
		 * if we did stop counting at the border of the fixed cell block above, we must now continue the count at the
		 * first visible cell after the break. this takes into account that a middle part of the merged cell is not
		 * visible anymore because the user has scrolled, while the first and last parts are still visible (the first
		 * part is visible because it is in the fixed cell block, while the last part is still visible because the user
		 * has not scrolled very far).
		 */
		if (col < getFixedColumnCount() && col + i >= getFixedColumnCount()) {
			for (int j = 0; getValidCell(m_LeftColumn + j, row).equals(new Point(col, row)); j++) {
				widthInPx += getColumnWidth(m_LeftColumn + j);
				widthInColumns = m_LeftColumn + j - row;
			}
		}

		/*
		 * another special case: if a merged cell is only partially visible (because of scrolling), we first determine
		 * the theoretical width of the cell, disregarding that some parts are not visible. therefore, we must now
		 * subtract the width of all those columns that are not visible anymore because the user has already scrolled
		 * away.
		 */
		if (col >= getFixedColumnCount() && col - m_LeftColumn < 0) {
			for (int k = 0; k < Math.abs(col - m_LeftColumn); k++) {
				widthInPx -= getColumnWidth(col + k);
			}
		}

		if (resultInPixels) {
			return widthInPx;
		} else {
			return widthInColumns;
		}
	}

	/**
	 * Calculates the extra <i>visible</i> height of the cell due to cell merging, both in pixels and in number of
	 * rows.
	 * 
	 * @param col
	 *            The column index of the cell.
	 * @param row
	 *            The row index of the cell.
	 * @param resultInPixels
	 *            If <code>true</code>, returns the height in pixels. If <code>false</code>, returns the result in
	 *            number of rows.
	 * @return The height, unit depending on the parameter <code>resultInPixels</code>.
	 */
	private int calculateExtraCellHeight(int col, int row, boolean resultInPixels) {
		// check: only the root cells of merged cells are actually drawn, so all other cells have span width of 0
		Point valid = getValidCell(col, row);
		if (valid.x != col || valid.y != row) {
			return 0;
		}

		// we determine the height of the cell both in pixels and in number of rows
		int heightInPx = 0, heightInRows = 0;

		// check adjacent cells (towards the bottom) to see if they are merged with this cell
		int i;
		for (i = 1; row + i < m_model.getRowCount() && getValidCell(col, row + i).equals(new Point(col, row)); i++) {
			/*
			 * if we get to the border of the fixed cell block, we must stop counting; otherwise, we run into problems
			 * when cells are merged across this border and we start scrolling
			 */
			if (row < getFixedRowCount() && row + i >= getFixedRowCount()) {
				break;
			}
			heightInPx += m_model.getRowHeight(row + i);
			heightInRows = i;
		}

		/*
		 * if we did stop counting at the border of the fixed cell block above, we must now continue the count at the
		 * first visible cell after the break. this takes into account that a middle part of the merged cell is not
		 * visible anymore because the user has scrolled, while the first and last parts are still visible (the first
		 * part is visible because it is in the fixed cell block, while the last part is still visible because the user
		 * has not scrolled very far).
		 */
		if (row < getFixedRowCount() && row + i >= getFixedRowCount()) {
			for (int j = 0; getValidCell(col, m_TopRow + j).equals(new Point(col, row)); j++) {
				heightInPx += m_model.getRowHeight(m_TopRow + j);
				heightInRows = m_TopRow + j - row;
			}
		}

		/*
		 * another special case: if a merged cell is only partially visible (because of scrolling), we first determine
		 * the theoretical height of the cell, disregarding that some parts are not visible. therefore, we must now
		 * subtract the height of all those rows that are not visible anymore because the user has already scrolled
		 * away.
		 */
		if (row >= getFixedRowCount() && row - m_TopRow < 0) {
			for (int k = 0; k < Math.abs(row - m_TopRow); k++) {
				heightInPx -= m_model.getRowHeight(row + k);
			}
		}

		if (resultInPixels) {
			return heightInPx;
		} else {
			return heightInRows;
		}
	}
	
	/**
	 * Calculates the number of extra columns this cell occupies due to cell merging. For example, if the cell does not
	 * span any columns, this method will return 0. If the cell spans across two columns, it will return one. However,
	 * if parts of the cell are not visible (for example, because of a combination of fixed columns and scrolling), then
	 * these parts are not counted here. This way, the caller can determine where the boundary of a cell is, taking into
	 * account the current scrolling position.
	 * 
	 * @param col
	 *            The column index.
	 * @param row
	 *            The row index.
	 * @return The number of extra columns this cell occupies due to cell merging.
	 */
	public int getVisibleCellSpanWidth(int col, int row) {
		return calculateExtraCellWidth(col, row, /* in pixels */false);
	}

	/**
	 * Calculates the number of extra rows this cell occupies due to cell merging. For example, if the cell does not
	 * span any rows, this method will return 0. If the cell spans across two rows, it will return one. However, if
	 * parts of the cell are not visible (for example, because of a combination of fixed rows and scrolling), then these
	 * parts are not counted here. This way, the caller can determine where the boundary of a cell is, taking into
	 * account the current scrolling position.
	 * 
	 * @param col
	 *            The column index.
	 * @param row
	 *            The row index.
	 * @return The number of extra rows this cell occupies due to cell merging.
	 */
	public int getVisibleCellSpanHeight(int col, int row) {
		return calculateExtraCellHeight(col, row, /* in pixels */false);
	}
	
	/**
	 * Calculates the area (in terms of columns and rows) that a certain cell occupies due to cell spanning. The return
	 * value is a {@link Rectangle}, where {@link Rectangle#x} is the column index of the upper-left corner of the cell,
	 * {@link Rectangle#y} is the row index of the upper-left corner of the cell, {@link Rectangle#width} is the width
	 * of the cell (in number of columns) and {@link Rectangle#height} is the height of the cell (in number of rows).
	 * The width and height of a cell are always at least one if the cell is visible. A cell that does not span any other
	 * cells has height and width equal to 1.
	 * 
	 * @param col The column index.
	 * @param row The row index.
	 * @return The area that a certain cell occupies due to cell spanning.
	 */
	public Rectangle getCellSpanExtent(int col, int row) {
		Point valid = getValidCell(col, row);
		Rectangle extent = new Rectangle(valid.x, valid.y, 1, 1);
		
		for(int i = extent.width; getValidCell(valid.x + i, valid.y).equals(valid); i++) {
			extent.width++;
		}
		
		for (int j = extent.height; getValidCell(valid.x, valid.y + j).equals(valid); j++) {
			extent.height++;
		}
		
		return extent;
	}

	protected boolean canDrawCell(Rectangle r, Rectangle clipRect) {
        if (r.height==0 || r.width==0)
            return false;
        if (r.y + r.height < clipRect.y)
            return false;
        if (r.y > clipRect.y + clipRect.height)
            return false;
        if (r.x + r.width < clipRect.x)
            return false;
        if (r.x > clipRect.x + clipRect.width)
            return false;
        return true;
    }

    //////////////////////////////////////////////////////////////////////////////
    // PAINTING & MORE
    //////////////////////////////////////////////////////////////////////////////

    // Paint-Result
    protected void onPaint(PaintEvent event) {
        
        Rectangle rect = getClientArea();
        GC gc = event.gc;

        doCalculations();

        if (m_model != null) {
            
            // be sure that clipping cuts oft all the unneccessary part of a spanned cell:
            Rectangle oldClipping = setContentAreaClipping(gc);
            
            // draw data cells
            drawCells(gc, oldClipping, m_LeftColumn, m_model
                    .getColumnCount(), m_TopRow, m_TopRow + m_rowsVisible);

            // draw row headers on the left
            // content has been drawn, so set back clipping:
            setTopAreaClipping(gc, oldClipping);
            drawCells(gc, gc.getClipping(), 0, getFixedColumnCount(),
                    m_TopRow, m_TopRow + m_rowsVisible);
            
            // draw header columns
            setLeftAreaClipping(gc, oldClipping);
            drawCells(gc, gc.getClipping(), m_LeftColumn, m_model
                    .getColumnCount(), 0, getFixedRowCount());
            
            // draw top left header region
            gc.setClipping(oldClipping);
            drawCells(gc, gc.getClipping() , 0, getFixedColumnCount(),
                    0, getFixedRowCount());
            
           drawBottomSpace(gc);
        } else {
            gc.fillRectangle(rect);
        }
    }
    
    private void setTopAreaClipping(GC gc, Rectangle oldClipping) {
        Rectangle contentClip = getClientArea();
        
        contentClip.x=1;
        contentClip.y=1;
        contentClip.width-=1;
        contentClip.height-=1;
                
       
        for (int i=0; i<getFixedRowCount(); i++) {
            int height = getCellRectIgnoreSpan(0, i).height;
            contentClip.y+=height+1;
            contentClip.height-=height+1;
        }
        
        contentClip.intersect(oldClipping);
        gc.setClipping(contentClip);
    }
    
    private void setLeftAreaClipping(GC gc, Rectangle oldClipping) {
        Rectangle contentClip = getClientArea();
        
        contentClip.x=1;
        contentClip.y=1;
        contentClip.width-=1;
        contentClip.height-=1;
                
        for (int i = 0; i < getFixedColumnCount(); i++){
            int width = getCellRectIgnoreSpan(i, 0).width;
            contentClip.x += width+1;
            contentClip.width -= width+1;
        }
        
        contentClip.intersect(oldClipping);
        gc.setClipping(contentClip);
    }
    
    /**
     * This sets the clipping area of the GC to the content 
     * area of the table, ignoring all header cells. This
     * has the result that fixed cells are not drawn by this gc!
     * @param gc The gc to manipulate.
     * @return The old clipping area. It you want to paint fixed cells
     * with this GC, re-set this with gc.setClipping();
     */
    private Rectangle setContentAreaClipping(GC gc) {
        Rectangle oldClipping = gc.getClipping();
        Rectangle contentClip = getClientArea();
        
        contentClip.x=1;
        contentClip.y=1;
        contentClip.width-=1;
        contentClip.height-=1;
                
        for (int i=0; i<getFixedColumnCount(); i++){
            int width = getCellRectIgnoreSpan(i, 0).width;
            contentClip.x+=width+1;
            contentClip.width-=width+1;
        }
        
        for (int i=0; i<getFixedRowCount(); i++) {
            int height = getCellRectIgnoreSpan(0, i).height;
            contentClip.y+=height+1;
            contentClip.height-=height+1;
        }
        
        contentClip.intersect(oldClipping);
        gc.setClipping(contentClip);
        return oldClipping;
    }

    
    /**
     * @param commandListener the commandListener to set
     */
    public void setCommandListener(ICommandListener commandListener) {
        m_commandListener = commandListener;
    }

    
    // Bottom-Space
    protected void drawBottomSpace(GC gc) {
        Rectangle r = getClientArea();
        if (m_model.getRowCount() > 0) {
        	r.y = getFixedHeight();
        	for (int i=0; i<m_rowsVisible; i++) {
        		r.y+= m_model.getRowHeight(i+m_TopRow);
        	}
        }
        
        int lastColRight = getColumnRight(
                Math.min(m_LeftColumn+m_columnsVisible, m_model.getColumnCount()-1));
        
        // implement the behavior that we fill the remaining space with an additional empty column. 
        if ((getStyle() & SWTX.FILL_WITH_DUMMYCOL)!=0) {
            lastColRight--;
            int lastCol = m_model.getColumnCount()-1;
            
            //int defaultrowheight = m_Model.getRowHeight()-1;
            int ystart = 1;
            for (int row=0; row<getFixedRowCount(); row++) {
                Point last = getValidCell(lastCol, row);
                KTableCellRenderer fixedRenderer = m_model.getCellRenderer(last.x, last.y);
                
                int rowheight=m_model.getRowHeight(row);
                fixedRenderer.drawCell(gc, 
                        new Rectangle(lastColRight + 2, ystart, r.width-1, rowheight-1),
                        -1, -1, "", false, true, false, m_model);
                
                ystart += rowheight;
            }
            TextCellRenderer defaultRenderer = new TextCellRenderer(SWT.NONE);
            for (int row=m_TopRow; row<m_TopRow+m_rowsVisible+1; row++) {
            	int rowHeight = m_model.getRowHeight(row);
                defaultRenderer.drawCell(gc, 
                        new Rectangle(lastColRight + 2, ystart, r.width-1, rowHeight-1),
                        -1, -1, "", false, false, false, m_model);
                ystart += rowHeight;
            }
            
            // draw bottom areas
            gc.setBackground(getBackground());
            gc.fillRectangle(r);
            
            // draw outer lines:
            Rectangle clientArea = getClientArea();
            gc.setForeground(m_display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            gc.drawLine(1, r.y , clientArea.x+clientArea.width -1,  r.y );
            gc.drawLine(clientArea.x+clientArea.width -1, 0, clientArea.x+clientArea.width -1, r.y-1);
            
            drawBorderLines(gc, clientArea);            
        } else if ((getStyle() & SWTX.FILL_WITH_LASTCOL) != 0) {
            gc.setBackground(getBackground());
            gc.fillRectangle(r);
            
            // draw outer lines:
            Rectangle clientArea = getClientArea();
            gc.setForeground(m_display.getSystemColor(SWT.COLOR_WHITE));
            gc.drawLine(1, r.y , clientArea.x+clientArea.width -1,  r.y );
            if (m_LeftColumn+m_columnsVisible == m_model.getColumnCount())
                gc.drawLine(clientArea.x+clientArea.width -1, 0, clientArea.x+clientArea.width -1, r.y-1);
            
            drawBorderLines(gc, clientArea);
        } else {
            // draw simple background colored areas
            gc.setBackground(getBackground());
            gc.fillRectangle(r);
            gc.fillRectangle(lastColRight + 2, 0, r.width, r.height); 
            
            gc.setForeground(m_display.getSystemColor(SWT.COLOR_WHITE));
            gc.drawLine(1, r.y , lastColRight + 1, r.y);
            gc.drawLine(lastColRight + 1, 0, lastColRight + 1, r.y-1);
            
            drawBorderLines(gc, r);
        }
        
       
    }

	private void drawBorderLines(GC gc, Rectangle clientArea) {
		if (m_model.getRowCount() > 0) {
			// first draw top border
			gc.setForeground(m_colorTopBorder);
			gc.drawLine(0, 0, clientArea.x + clientArea.width, 0);

			// then draw left border
			gc.setForeground(m_colorLeftBorder);
			gc.drawLine(0, 0, 0, clientArea.y + clientArea.height - 1);

			// and if we're scrolled all the way to the right, also draw right border
			if (m_LeftColumn + m_columnsVisible == m_model.getColumnCount()) {
				gc.setForeground(m_colorRightBorder);
				gc.drawLine(clientArea.x + clientArea.width - 1, 0, clientArea.x + clientArea.width - 1, clientArea.y
						+ clientArea.height);
			}
		}
	}

    // Cells
    /**
     * Redraws the the cells only in the given area.
     * 
     * @param cellsToRedraw
     *            Defines the area to redraw. The rectangles elements are not
     *            pixels but cell numbers.
     */
    public void redraw(Rectangle cellsToRedraw) {
        checkWidget();
        redraw(cellsToRedraw.x, cellsToRedraw.y, cellsToRedraw.width,
                cellsToRedraw.height);
    }

    /**
     * Redraws the the cells only in the given area.
     * 
     * @param firstCol the first column to draw. 
     * @param firstRow the first row to draw. (Map if you use a sorted model!)
     * @param numOfCols the number of columns to draw.
     * @param numOfRows the number of rows to draw.
     */
    public void redraw(int firstCol, int firstRow, int numOfCols, int numOfRows) {
        checkWidget();
        boolean redrawFixedRows = false;
        if (firstRow < getFixedRowCount()) {
            firstRow = m_TopRow;
            redrawFixedRows = true;
        }
        boolean redrawFixedCols = false;
        if (firstCol < getFixedColumnCount()) {
            firstCol = m_LeftColumn;
            redrawFixedCols = true;
        }
        
        Rectangle clipRect = getClientArea();
        GC gc = new GC(this);
        Rectangle oldClip = setContentAreaClipping(gc);
        drawCells(gc, 
                  clipRect, 
                  firstCol, 
                  Math.min(firstCol + numOfCols, m_model.getColumnCount()), 
                  firstRow, 
                  Math.min(firstRow + numOfRows, m_model.getRowCount()));
        
        gc.setClipping(oldClip);
        
        if (redrawFixedRows)
            drawCells(gc, gc.getClipping(), m_LeftColumn, m_model
                    .getColumnCount(), 0, getFixedRowCount());
        if (redrawFixedCols)
            drawCells(gc, gc.getClipping(), 0, getFixedColumnCount(),
                    m_TopRow, m_TopRow + m_rowsVisible);
        if (redrawFixedCols || redrawFixedRows) {
            drawCells(gc, gc.getClipping() , 0, getFixedColumnCount(),
                    0, getFixedRowCount());
            drawBottomSpace(gc);
        }
        gc.dispose();
    }

    protected void drawCells(GC gc, Rectangle clipRect, 
                             int fromCol, int toCol, 
                             int fromRow, int toRow) {
        Rectangle r;
        
        if (m_cellEditor != null) {
            if (!isCellVisible(m_cellEditor.m_Col, m_cellEditor.m_Row)) {
                Rectangle hide = new Rectangle(-101, -101, 100, 100);
                m_cellEditor.setCellRect(hide);
            } else {
                m_cellEditor.setCellRect(getCellRect(m_cellEditor.m_Col,
                        m_cellEditor.m_Row));
            }
        }

        int fromCol_X = getCellRectIgnoreSpan(fromCol, fromRow).x;
        for (int row = fromRow; row < toRow; row++) {
            // skipping non-visible:
            r = getCellRectIgnoreSpan(fromCol, row, fromCol_X);
//			if (r.y + r.height < clipRect.y && !haveToPaintRowBefore)
//				continue;
            if (r.y > clipRect.y + clipRect.height) {
                break;
            }
            
            // the right cell border is cached to avoid the expensive col loop.
            int right_border_x = r.x;
            for (int col = fromCol; col < toCol; col++) {
                r = getCellRect(col, row, right_border_x);
                right_border_x += getColumnWidth(col);
                
                if (r.x > clipRect.x + clipRect.width) 
                    break;
                if (r.y > clipRect.y + clipRect.height)
                    return;
                
				// check if it is an overlapped cell
				Point valid = getValidCell(col, row);
				if (valid != null && (valid.x != col || valid.y != row)) {
					// is this a partially visible merged cell? then we still have to draw it!
					if ((valid.x < col && col - 1 < m_LeftColumn && col > getFixedColumnCount())
							|| (valid.y < row && row - 1 < m_TopRow && row > getFixedRowCount())) {
						drawCell(gc, valid.x, valid.y);
					}
					// merged cell that is not partially visible -> don't draw
                    continue;
				}
                
                // perform real work:
                if (canDrawCell(r, clipRect))
                    drawCell(gc, col, row, r);
            }
        }
    }

    /**
     * Looks into the model to determine if the given cell is overlapped by 
     * a cell that spans several columns/rows. In that case the index of the
     * cell responsible for the content (in the left upper corner) is returned.
     * Otherwise the given cell is returned.
     * 
     * @param colToCheck The column index of the cell to check. 
     * @param rowToCheck The row index of the cell to check. 
     *      (as seen by the KTable. Map if you use a sorted model!)
     * @return returns the cell that overlaps the given cell, or the given
     *         cell if no cell overlaps it.
     * @throws IllegalArgumentException If the model returns cells on <code>
     * Model.belongsToCell()</code> that are on the right or below the given cell.
     * @see KTableSortedModel#mapToTable(int);
     */
    public Point getValidCell(int colToCheck, int rowToCheck) {
        
        checkWidget();
        
        Point found = new Point(colToCheck, rowToCheck);

        // check input parameters
        if (colToCheck >= m_model.getColumnCount() || rowToCheck >= m_model.getRowCount() || 
      		  colToCheck < 0 || rowToCheck < 0) {
        	return found;
        }

        Point lastFound = null;
        
        while (!found.equals(lastFound)) {
            
            lastFound = found;
            found = m_model.belongsToCell(found.x, found.y);
            
            if (found != null  &&  (found.x > lastFound.x  ||  found.y > lastFound.y)) {
                throw new IllegalArgumentException("When spanning over several cells, " +
                		"supercells that determine the content of the large cell must " +
                		"always be in the left upper corner!");
            }
            
            if (found == null) {
             	return lastFound;
            }
        }
        return found;
    }

    /**
     * Call when a manual redraw on a cell should be performed.
     * In case headers should be updated to reflect a focus change, this is performed.
     * @param gc
     * @param col
     * @param row
     */
    protected void drawCell(GC gc, int col, int row) {
        
        drawCell(gc, col, row, getCellRect(col, row));
        
        Rectangle oldClip = gc.getClipping();
        gc.setClipping(getClientArea());
        
        if (isHighlightSelectionInHeader()) {
            
            if (row >= m_TopRow) {
                for (int i = 0; i < m_model.getFixedHeaderColumnCount(); i++) {
                    drawCell(gc, i, row, getCellRect(i, row));
                }
                
                for (int i = 0; i < m_model.getFixedHeaderRowCount(); i++) {
                    drawCell(gc, col, i, getCellRect(col, i));
                }
            }
        }
        
        gc.setClipping(oldClip);
    }
    
    protected void drawCell(GC gc, int col, int row, Rectangle rect) {
        if ((row < 0) || (row >= m_model.getRowCount())) {
            return;
        }

        if (rect.width==0 || rect.height==0)
            return;
        
        // set up clipping so that the renderer is only
        // allowed to paint in his area:
        Rectangle oldClip = gc.getClipping();
        Rectangle newClip = new Rectangle(rect.x, rect.y, rect.width+1, rect.height+1);
        newClip.intersect(oldClip);
        gc.setClipping(newClip);
        
        Point validClickedCell = getValidCell(m_clickColumnIndex, m_clickRowIndex);
        
        KTableCellRenderer cellRenderer = m_model.getCellRenderer(col, row);
        cellRenderer.setEnabled(isEnabled());
        
        boolean isShowAsSelected = showAsSelected(col, row); 
        boolean isHighlightSelectedRowCol = highlightSelectedRowCol(col, row);
        // System.out.println("draw cell: (" + col + ", " + row + "): show: " + isShowAsSelected + ", highlight: " + isHighlightSelectedRowCol);
        cellRenderer.drawCell(
        		gc,
        		rect,
        		col,
        		row,
        		m_model.getContentAt(col, row),
        		isShowAsSelected || isHighlightSelectedRowCol,
        		isHeaderCell(col, row),
        		col == validClickedCell.x && row == validClickedCell.y,
        		m_model);
        
        gc.setClipping(oldClip);
    }
    
    /**
     * Interface method to update the content of a cell.<p>
     * Don't forget to map the row index if a sorted model is used.
     * @param col The column index
     * @param row The row index.
     * @see KTableSortedModel#mapRowIndexToTable(int)
     */
    public void updateCell(int col, int row) {
        checkWidget();
        if ((row < 0) || (row >= m_model.getRowCount()) || 
            (col < 0) || (col >= m_model.getColumnCount()))
            return;

        // be sure it is a valid cell if cells span 
        Point valid = getValidCell(col, row);
        // update it:
        GC gc = new GC(this);
        drawCell(gc, valid.x, valid.y);
        gc.dispose();
    }

    /**
     * @param col The column index
     * @param row The row index
     * @return Returns true if the given cell is a fixed cell,
     * that is a header cell. Returns false otherwise.
     */
    public boolean isFixedCell(int col, int row) {
        return col < getFixedColumnCount()
        || row < getFixedRowCount();
    }
    
    /**
     * @param col The column index
     * @param row The row index
     * @return Returns true if the given cell is within the region specified by the model 
     * using the methods <code>getFixedHeaderColumnCount()</code> and <code>getFixedHeaderRowCount()</code>
     */
    public boolean isHeaderCell(int col, int row) {
        // col and rown may be -1, since getCellForCoordinates() returns (-1, -1)
        // in no cell was clicked, so check lower boundary also.
        return (col >= 0  &&  col < m_model.getFixedHeaderColumnCount()) 
        || (row >= 0  &&  row < m_model.getFixedHeaderRowCount());
    }
    
    protected boolean showAsSelected(int col, int row) {
        // A cell with an open editor should be drawn without focus
        if (m_cellEditor != null) {
            if (col == m_cellEditor.m_Col && row == m_cellEditor.m_Row 
            		&& m_cellEditor.getControl() != null)
                return false;
        }
        return isCellSelected(col, row) && (isFocusControl() || isShowSelectionWithoutFocus());
    }

    
    protected void drawRow(GC gc, int row) {
        drawCells(gc, getClientArea(), 0, getFixedColumnCount(), row, row + 1);
        drawCells(gc, getClientArea(), m_LeftColumn, m_model.getColumnCount(), row, row + 1);
    }

    
    protected void drawCol(GC gc, int col) {
        if (col < getFixedColumnCount()) {
            drawCells(gc, getClientArea(), col, col + 1, 0, m_TopRow + m_rowsVisible);
        } else {
            
            // draw column header
            drawCells(gc, getClientArea(), col, col + 1, 0, getFixedRowCount());
            Rectangle oldClip = setContentAreaClipping(gc);
            
            // draw data cells
            drawCells(gc, gc.getClipping(), col, col + 1, m_TopRow, m_TopRow + m_rowsVisible);
            gc.setClipping(oldClip);    
        }
    }

    /**
     * Sets the default cursor to the given cursor. This instance is saved
     * internally and displayed whenever no linecursor or resizecursor is shown.
     * <p>
     * The difference to setCursor is that this cursor will be preserved over
     * action cursor changes.
     * 
     * @param cursor
     *            The cursor to use, or <code>null</code> if the OS default
     *            cursor should be used.
     * @param size_below_hotspot The number of pixels that are needed to paint the 
     * 	      cursor below and right of the cursor hotspot (that is the actual location the cursor
     *        is pointing to).<p>
     *        NOTE that this is just there to allow better positioning of tooltips.
     * 	      Currently SWT does not provide an API to get the size of the cursor. So
     *        these values are taken to calculate the position of the tooltip. The
     *        the tooltip is placed pt.x pixels left and pt.y pixels below the mouse location.<br>
     *        If you don't know the size of the cursor (for example you use a default one), set 
     *        <code>null</code> or <code>new Point(-1, -1)</code>. 
     */
    public void setDefaultCursor(Cursor cursor, Point size_below_hotspot) {
        checkWidget();
        if (m_defaultCursor != null)
            m_defaultCursor.dispose();
        m_defaultCursor = cursor;
        m_defaultCursorSize = size_below_hotspot;
        setCursor(cursor);
    }
    
    public void setDefaultRowResizeCursor(Cursor cursor) {
    	
    	checkWidget();
    	
    	if ( m_defaultRowResizeCursor != null ) {
    		m_defaultRowResizeCursor.dispose();
    	}
    	
    	m_defaultRowResizeCursor = cursor;
    	
    }
    
    public void setDefaultColumnResizeCursor(Cursor cursor) {
    	
    	checkWidget();
    	
    	if ( m_defaultColumnResizeCursor != null ) {
    		m_defaultColumnResizeCursor.dispose();
    	}
    	
    	m_defaultColumnResizeCursor = cursor;

    }

    //////////////////////////////////////////////////////////////////////////////
    // REACTIONS ON USER ACTIONS
    //////////////////////////////////////////////////////////////////////////////

    private int getHeaderHeight() {
    	int height = 1;
    	for (int i=0; i<m_model.getFixedHeaderRowCount(); i++)
    		height+=m_model.getRowHeight(i);
    	return height;
    }
    
    /* returns the index of a model column */
    protected int getColumnForResize(int x, int y) {

    	if (m_model == null || y <= 0 || y > getClientArea().y + getClientArea().height
				|| (y >= getHeaderHeight() & (getStyle() & SWTX.RESIZE_ON_ENTIRE_COLUMN) == 0)) {
			return -1;
		}

        if ( x < getFixedWidth() + m_resizeAreaSize / 2 ) {
        	
            for (int i = 0; i < getFixedColumnCount(); i++)
            	
                if (Math.abs(x - getColumnRight(i)) < m_resizeAreaSize / 2) {
                    
                	if (m_model.isColumnResizable(i)) {
                        return i;
                    }
                    
                    return -1;
                    
                }
            
        }

        int left = getColumnLeft(m_LeftColumn);
        // -1
        for (int i = m_LeftColumn; i < (m_LeftColumn + m_columnsVisible + 1) && i<m_model.getColumnCount(); i++) {
        	
            if ( i >= m_model.getColumnCount() ) {
            	return -1;
            }
            
            int right = left + getColumnWidth(i);
            
            if ( Math.abs(x - right) < m_resizeAreaSize / 2 ) {
            	
                if (m_model.isColumnResizable(i)) {
                    return i;
                }
                
                return -1;
                
            }
            
            if ( (x >= left + m_resizeAreaSize / 2) &&
                 (x <= right - m_resizeAreaSize / 2) ) {
                break;
            }
            
            left=right;
        }
        
        return -1;
        
    }

    /* returns the number of a row in the view (!) */
    protected int getRowForResize(int x, int y) {
        
    	if ( m_model == null || x <= 0 || x >= getHeaderWidth() ) {
            return -1;
        }

       if ( y < m_model.getRowHeight(0) + m_resizeAreaSize / 2 ) {
    	   
           // allow first row height resize?
    	   
           	if ( Math.abs(m_model.getRowHeight(0) - y) < m_resizeAreaSize / 2 &&
           	     m_model.isRowResizable(0) ) {
           		
           	    return 0;
           	    
           	}
           	
            return -1;
            
       }

        int row        = getRowForY(y);
        int row_y      = getYforRow(row);
        int next_row_y = getYforRow(row+1);

        if ( ( Math.abs(y - next_row_y) < m_resizeAreaSize / 2 ) &&
        	 m_model.isRowResizable(row) ) {
            return row;
        } else if ( ( Math.abs(y - row_y) < m_resizeAreaSize / 2 ) &&
        		    m_model.isRowResizable(row-1) ) {
        	return row-1;
        }
        
        return -1;
        
    }
    
    /**
     * Returns the number of the column that is present at position x or -1, if
     * out of area. When cells are spanned, returns the address of the spanned cell.
     * @param x the x location of an event in table coordinates
     * @param y The y location of an event in table coordinates
     * @deprecated Use {@link #getCellForCoordinates(int, int) instead.
     * @return Returns the point where x is the column index, y is the row index.
     */
    public Point calcColumnNum(int x, int y) {
        if (m_model == null)
            return new Point(-1, -1);
        
        Point toFind = new Point(x, y);
        Point valid = getValidCell(m_LeftColumn, m_TopRow);
        
        // at first iterate over the fixed columns, that are always shown:
        Point found = checkIfMatchesInColumns(0, getFixedRowCount(), 
                0, getFixedColumnCount(), toFind, true);
        if (found!=null) return found;
        found = checkIfMatchesInColumns(valid.y, m_TopRow + m_rowsVisible, 
                0, getFixedColumnCount(), toFind, true);
        if (found!=null) return found;

        found = checkIfMatchesInColumns(0, getFixedRowCount(), 
                valid.x, m_LeftColumn+m_columnsVisible, toFind, true);
        if (found!=null) return found;
        found = checkIfMatchesInColumns(valid.y, m_TopRow + m_rowsVisible, 
                valid.x, m_LeftColumn+m_columnsVisible, toFind, true);
        if (found!=null) return found;
        
        return new Point(-1, -1);
        
    }
    
    protected Point calcNonSpanColumnNum(int x, int y) {
        if (m_model == null)
            return new Point(-1, -1);
        
        Point toFind = new Point(x, y);
        Point valid = new Point(m_LeftColumn, m_TopRow);
        
        // at first iterate over the fixed columns, that are always shown:
        Point found = checkIfMatchesInColumns(0, getFixedRowCount(), 
                0, getFixedColumnCount(), toFind, false);
        if (found!=null) return found;
        found = checkIfMatchesInColumns(valid.y, m_TopRow + m_rowsVisible, 
                0, getFixedColumnCount(), toFind, false);
        if (found!=null) return found;

        found = checkIfMatchesInColumns(0, getFixedRowCount(), 
                valid.x, m_LeftColumn+m_columnsVisible, toFind, false);
        if (found!=null) return found;
        found = checkIfMatchesInColumns(valid.y, m_TopRow + m_rowsVisible, 
                valid.x, m_LeftColumn+m_columnsVisible, toFind, false);
        if (found!=null) return found;
        
        return new Point(-1, -1);
    }
    
    /**
     * Checks for the event location in table coordinates within the region covered
     * by the columns beginning by startCol and ending by endCol.
     * @param span Set to true if for spanning cells we just want to have the left-upper-most cell.
     */
    protected Point checkIfMatchesInColumns(int startRow, int endRow, int startCol, int endCol, Point toFind, boolean span) {
        
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                
                Rectangle rect = getCellRectIgnoreSpan(col, row);
                // take into account the 1px right and bottom border
                rect.width+=1;
                rect.height+=1;
                if (rect.contains(toFind))
                    if (span)
                    // take into account the spanning when reporting a match:
                        return getValidCell(col, row);
                    else
                        return new Point(col, row);
            }
        }
        return null;
    }

    public boolean isCellVisible(int col, int row) {
        checkWidget();
        if (m_model == null)
            return false;
        return ((col >= m_LeftColumn && col < m_LeftColumn + m_columnsVisible) || col < getFixedColumnCount())
                && ((row >= m_TopRow && row < m_TopRow + m_rowsVisible) || row < getFixedRowCount());
    }

    public boolean isCellFullyVisible(int col, int row) {
        checkWidget();
        if (m_model == null)
            return false;
        return ((col >= m_LeftColumn && col < m_LeftColumn + m_columnsFullyVisible) || col < getFixedColumnCount())
				&& ((row >= m_TopRow && row < m_TopRow + m_rowsFullyVisible) || row < getFixedRowCount());
    }

    /**
     * @param row The row index AS SEEN BY KTABLE. If you use a sorted model, don't forget
     * to map the index.
     * @return Returns true if the row is visible.
     */
    public boolean isRowVisible(int row) {
        checkWidget();
        if (m_model == null)
            return false;
        return ((row >= m_TopRow && row < m_TopRow + m_rowsVisible) || 
                row < (getFixedRowCount()));

    }

    /**
     * @param row the row index to check - as seen by ktable. If you use a 
     * sorted model, don't forget to map.
     * @return Returns true if the row is fully visible.
     */
    public boolean isRowFullyVisible(int row) {
        checkWidget();
        if (m_model == null)
            return false;
        return ((row >= m_TopRow && row < m_TopRow + m_rowsFullyVisible) 
                || row <getFixedRowCount());
    }

    
    /*
     * Focuses the given Cell. Assumes that the given cell is in the viewable
     * area. Does all necessary redraws.
     * This is the most important method for managing selections - based on user 
     * input (mouse or keys) determines which cells are selected / deselected.
     * Depending on selection mode selections are stored in map m_seletions,
     * or as cell coordinates in m_FocusRow and m_FocusCol.
     * 
     * This information (m_selections, m_FocusRow, m_FocusCol and selection mode
     * is then used when drawing cells - see drawCell(), and isCellSelected(). 
     */
    protected void focusCell(int col, int row, int stateMask) {
        // assure it is a valid cell:
        Point orig = new Point(col, row);
        Point valid = getValidCell(col, row);
        
        if (valid != null) {
            col = valid.x;
            row = valid.y;
        }
        
        GC gc = new GC(this);
        
        // close cell editor if active
        closeCellEditor();

        // no modifier keys, make the first selection
        if ((stateMask & SWT.CTRL) == 0 && (stateMask & SWT.SHIFT) == 0  ||  !isMultiSelectMode()) {
            
            if (isRowSelectMode()) {
                m_selectionMode = ESelectionMode.ERow;
            } else {
                
                if (isTopLeftHeaderArea(col, row)) {
                    m_selectionMode = ESelectionMode.ECell;
                    
                } else if (isDataCell(col, row)) {
                    m_selectionMode = ESelectionMode.ECell;
                    
                } else if (isRowHeaderCell(col, row)) {
                    m_selectionMode = ESelectionMode.ERow;
                    
                } else if (isColumnHeaderCell(col, row)) {
                    m_selectionMode = ESelectionMode.EColumn;
                }
            }

            if (!isTopLeftHeaderArea(col, row)) {
                selectWhenNoModifierKey(col, row, gc);
            }
            fireCellSelection(orig.x, orig.y, stateMask);
            
        } else if ((stateMask & SWT.CTRL) != 0) {
            boolean success = toggleSelection(col, row);  
            if (success) {
                m_focusCol = col;
                m_focusRow = row;
            }

            switch (m_selectionMode) {
            case ECell:
                drawCell(gc, col, row);
                break;
            case EColumn:
                drawCol(gc, col);
                break;
            case ERow:
                drawRow(gc, row);
                break;
            default:
                break;
            
            }
            
            // notify non-fixed cell listeners
            if (success) {
                fireCellSelection(m_focusCol, m_focusRow, stateMask);
            }
            
        } else if ((stateMask & SWT.SHIFT) != 0) {
                // System.out.println("SHIFT sel mode col = " + col);
                switch (m_selectionMode) {
                case ERow:
                    selectRowsBetweenClickedRows(row);
                    fireCellSelection(orig.x, orig.y, stateMask);
                    break;
                case EColumn:
                    selectColumnsBetweenClickedColumns(col);
                    fireCellSelection(orig.x, orig.y, stateMask);
                    break;
                case ECell:
                    boolean selectionChanged = selectCellsBetweenClickedCells(col,
                                                                              row);
                    if (selectionChanged) {
                        fireCellSelection(orig.x, orig.y, stateMask);
                    }
                    break;
                }
                
        } 
                 
        gc.dispose();
    }

    
    private void selectWhenNoModifierKey(int col, int row, GC gc) {
        boolean redrawAll = !m_selections.isEmpty();
        int oldFocusRow = m_focusRow;
        int oldFocusCol = m_focusCol;                
        
        clearSelectionWithoutRedraw();
        addToSelectionWithoutRedraw(col, row);
        m_focusRow = row;
        m_focusCol = col;
        m_mainFocusRow = row;
        m_mainFocusCol = col;

        if (redrawAll) {
            redraw();
        } else {
            
            switch (m_selectionMode) {
            case ECell:
                Rectangle origClipping = null;
                if (!isFixedCell(oldFocusCol, oldFocusRow)) {
                    origClipping = setContentAreaClipping(gc);
                }
                
                // redraw previously focused cell, if there was one
                if (oldFocusCol >= 0 && oldFocusRow >= 0) {
                  drawCell(gc, oldFocusCol, oldFocusRow);
                }
                
                if (origClipping!=null) {
                    gc.setClipping(origClipping);
                }
                
                if (!isFixedCell(m_focusCol, m_focusRow)) {
                    origClipping = setContentAreaClipping(gc);
                }
                
                drawCell(gc, m_focusCol, m_focusRow);
                break;
            case EColumn:
                // redraw previously focused column, if there was one
                if (oldFocusCol >= 0) {
                    drawCol(gc, oldFocusCol);
                }
                
                drawCol(gc, m_focusCol);
                break;
            case ERow:
                // redraw previously focused row, if there was one
                if (oldFocusRow >= 0) {
                    drawRow(gc, oldFocusRow);
                }
                
                drawRow(gc, m_focusRow);
                break;
            default:
                break;
            
            }
        }
    }

    
    private boolean selectCellsBetweenClickedCells(int col, int row) {
        boolean selectionChanged = false;
        Point[] sel = getCellSelection();
        Point min= new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Point max = new Point(-1,-1);
        
        boolean containsCell = false;
        
        for (int i = 0; i < sel.length; i++) {
        	max.x = Math.max(sel[i].x, max.x);
            max.y = Math.max(sel[i].y, max.y);
            min.x = Math.min(sel[i].x, min.x);
            min.y = Math.min(sel[i].y, min.y);
        	if (!containsCell && sel[i].x == col  &&  sel[i].y == row) {
        		containsCell = true;
        	}
        }
        
        // when user clicked on cell on the other side of originally clicked cell,
        // move selection area to occupy range between original cell (m_mainFocus*)
        // and currently clicked cell
        if (col < m_mainFocusCol  &&  max.x > m_mainFocusCol) {
        	min.x = col; 
        	max.x = m_mainFocusCol;
        } else if (col > m_mainFocusCol  &&  min.x < m_mainFocusCol) {
        	min.x = m_mainFocusCol; 
        	max.x = col;
        }
        
        if (row < m_mainFocusRow  &&  max.y > m_mainFocusRow) {
        	min.y=row; 
        	max.y=m_mainFocusRow;
        } else if (row > m_mainFocusRow  &&  min.y < m_mainFocusRow) {
        	min.y = m_mainFocusRow; 
        	max.y = row;
        }
        
        Set<SelectionItem> oldSelection = new TreeSet<>(m_selections);
        if (containsCell) {
        	clearSelectionWithoutRedraw();
        	// m_focus* contains last clicked cell - not the start of
        	// multi selection when SHIFT was pressed, but the second
        	// corner ofthe region
        	if (max.x == m_focusCol) max.x = col;
        	if (max.y == m_focusRow) max.y = row;
        	if (min.x == m_focusCol) min.x = col;
        	if (min.y == m_focusRow) min.y = row;
        	
        	// set selection:
        	for (int r = min.y; r <= max.y; r++) {
        		for (int c = min.x; c <= max.x; c++) {
        			addToSelectionWithoutRedraw(c, r);
        		}
        	}
        	
        	if (!oldSelection.equals(m_selections)) {
        		redraw();
        		// notify non-fixed cell listeners
                selectionChanged = true;
        	}
        } else {           			
        	
        	if (col > max.x) max.x = col;
        	if (row > max.y) max.y = row;
        	if (col < min.x) min.x = col;
        	if (row < min.y) min.y = row;
        

        	for (int r = min.y; r <= max.y; r++) {
        		for (int c = min.x; c <= max.x; c++) {
        			addToSelectionWithoutRedraw(c, r);
        		}
        		
        	}

        	// If this is multi selection mode and the highlight
        	// selection header style bit is set then we want
        	// to force a redraw of the fixed rows and columns.
        	// This makes sure that the highlighting of those
        	// fixed rows and columns happens.
        	
        	if (isHighlightSelectionInHeader()) {
        		redraw(-1, row, 1, 1);
        		redraw(col, -1, 1, 1);
        	}
        	
        	if (!oldSelection.equals(m_selections)) {
        		redraw(min.x, min.y, max.x-min.x+1, max.y-min.y+1);
        		// notify non-fixed cell listeners
        		selectionChanged = true;
        	}
        }
        	
         m_focusRow = row;
         m_focusCol = col;
        return selectionChanged;
    }

    
    private void selectColumnsBetweenClickedColumns(int column) {

        Set<SelectionItem> oldSelection = new TreeSet<>(m_selections);
        int tmpCol = column;
        
        // When SHIFT is pressed selection is always between 
        // originally selected column and the last clicked column.
        // For example, if user clicked col. 11, holds SHIFT down,
        // then clicks col 8 and then column 14, only columns 11-14
        // should be selected.
        m_selections.clear();
        // add originally selected column
        m_selections.add(new SelectionItem(m_mainFocusCol, -1));
        
        // backward selection
        while (tmpCol < m_mainFocusCol) {
            addToSelectionWithoutRedraw(tmpCol++, m_focusRow);
        }

        // forward selection
        while (tmpCol > m_mainFocusCol) {
            addToSelectionWithoutRedraw(tmpCol--, m_focusRow);
        }
        
        if (!oldSelection.equals(m_selections)) {
            // redraw all visually modified cells
            oldSelection.addAll(m_selections);
            Iterator<SelectionItem> it = oldSelection.iterator();
            int min = 0, max = 0;
            
            if (it.hasNext()) {
                min = it.next().m_column;
                max = min;
            }
            
            while (it.hasNext()) {
                 int r = it.next().m_column;
                 min = Math.min(r, min);
                 max = Math.max(r, max);
            }
            
            redraw(min, 0, max - min + 1, m_model.getRowCount());
        }
        m_focusCol = column;
    }
    
    
    private void selectRowsBetweenClickedRows(int row) {

        Set<SelectionItem> oldSelection = new TreeSet<>(m_selections);
        int tmpRow = row;
        
        // When SHIFT is pressed selection is always between 
        // originally selected column and the last clicked column.
        // For example, if user clicked col. 11, holds SHIFT down,
        // then clicks col 8 and then column 14, only columns 11-14
        // should be selected.
        m_selections.clear();
        // add originally selected row
        m_selections.add(new SelectionItem(-1, m_mainFocusRow));
        
        // backward selection
        while (tmpRow < m_mainFocusRow) {
            addToSelectionWithoutRedraw(m_focusCol, tmpRow++);
        }

        // forward selection
        while (tmpRow > m_mainFocusRow) {
            addToSelectionWithoutRedraw(m_focusCol, tmpRow--);
        }
        
        if (!oldSelection.equals(m_selections)) {
            
        	oldSelection.addAll(m_selections);
        	Iterator<SelectionItem> rowIt = oldSelection.iterator();
        	int min=0, max=0;
        	
        	if (rowIt.hasNext()) {
        		min = rowIt.next().m_row;
        		max = min;
        	}
        	
        	while (rowIt.hasNext()) {
        		 int r = rowIt.next().m_row;
        		 min = Math.min(r, min);
        		 max = Math.max(r, max);
        	}
        	
        	redraw(0, min, m_model.getColumnCount(), max - min + 1);
        }
        m_focusRow = row;
    }

    
   /* private void addCellToSelection(int col, int row) {
        m_selections.add(new SelectionItem(col, row));
    } */

    
    private boolean isTopLeftHeaderArea(int col, int row) {
        return row < m_model.getFixedHeaderRowCount()  && 
               col < m_model.getFixedHeaderColumnCount();
    }
    
    
    private boolean isDataCell(int col, int row) {
        return row >= m_model.getFixedHeaderRowCount()  && 
            col >= m_model.getFixedHeaderColumnCount();
    }

    
    /** Returns true for all column header cells but top left. */ 
    private boolean isColumnHeaderCell(int col, int row) {
        return row < m_model.getFixedHeaderRowCount()  && 
               col >= m_model.getFixedHeaderColumnCount();
    }

    
    /** Returns true for all row header cells but top left. */ 
    private boolean isRowHeaderCell(int col, int row) {
        return row >= m_model.getFixedHeaderRowCount()  && 
                   col < m_model.getFixedHeaderColumnCount();
    }

    /**
     * Closes the cell editor, if one is open.
     */
	public void closeCellEditor() {
		if (m_cellEditor != null)
			m_cellEditor.close(true);
	}

    protected void onMouseDown(MouseEvent e) {
    	// is the click intercepted? then stop processing!
    	Point spanCell = getCellForCoordinates(e.x, e.y);
    	Rectangle spanCellRect = getCellRect(spanCell.x, spanCell.y);
    	if (fireCellClicked(spanCell.x, spanCell.y, spanCellRect, e.x, e.y, e.button)) {
    	    m_clickColumnIndex = spanCell.x;
    	    m_clickRowIndex = spanCell.y;
    	    
    	    closeCellEditor();
    	    redraw();
     	    return;
    	}
	
        if (e.button == 1) {
            setCapture(true);
            m_capture = true;

            // Resize column?
            int columnIndex = getColumnForResize(e.x, e.y);
            if (columnIndex >= 0) {
                m_resizeColumnIndex = columnIndex;
                m_resizeColumnLeft  = getColumnLeft(columnIndex);
                return;
            }

            // Resize row?
            int rowIndex = getRowForResize(e.x, e.y);
            if (rowIndex>=0) {
            	m_resizeRowIndex = rowIndex;
            	m_resizeRowTop   = getYforRow(rowIndex);
            	return;
            }
        }
        
        if (e.button == 1  /* || isMultiSelectMode() */ ) { 
            
            //focus change
            Point cell = calcNonSpanColumnNum(e.x, e.y);
            if (cell.x == -1 || cell.y == -1) {
            	closeCellEditor();
                return;
            }
            m_clickColumnIndex = cell.x;
            m_clickRowIndex = cell.y;
            
            focusCell(cell.x, cell.y, e.stateMask);
        }
        
        if (e.button == 3) {
            setFocus(); // otherwise focus remains on previous component
                        // and UIUtils.getKTableSelection() returns null.
        }
    }
    
    protected boolean clickInSelectedCells(Point click) {
        
        Point[] selection = getCellSelection();
        
        if (selection == null) {
            return false;
        }
        
        Point cell = getCellForCoordinates(click.x, click.y);
        
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].y == -1) { // column is selected
                if (cell.x == selection[i].x) {
                    return true;
                } 
            } else if (selection[i].x == -1) { // row is selected
                if (cell.y == selection[i].y) {
                    return true;
                } 
            } else if (selection[i].x == cell.x  &&  selection[i].y == cell.y) {
                return true;
            }
        }
        return false;
    }

    protected void onMouseMove(MouseEvent e) { 
        if (m_model == null)
            return;

        // show resize cursor?
        checkCursorToDisplay(e.x, e.y);
        
        if (e.stateMask == SWT.BUTTON1 && m_cellEditor == null && m_resizeColumnIndex == -1) {            
            // extend selection?
            if (m_clickColumnIndex != -1 && isMultiSelectMode()) {
                
                Point cell = calcNonSpanColumnNum(e.x, e.y);
                if (cell.x==-1 || cell.y == -1)
                    return;
                if (cell.y >= m_model.getFixedHeaderRowCount()
                        && cell.x >= m_model.getFixedHeaderColumnCount()) {
                	
                	if (cell.x != m_clickColumnIndex || cell.y != m_clickRowIndex) {
                		m_clickColumnIndex = cell.x;
                		m_clickRowIndex = cell.y;
                    
                		focusCell(cell.x, cell.y, (e.stateMask | SWT.SHIFT));
                	}
                }
            }
            
        }
        // column resize?
        if (m_resizeColumnIndex != -1) {
        	
            Rectangle rect = getClientArea();

            if (e.x > rect.x + rect.width - 1) {
                e.x = rect.x + rect.width - 1;
            }
            
            int newSize = e.x - m_resizeColumnLeft;
            
            if (newSize < 5) {
                newSize = 5;
            }
            
            m_model.setColumnWidth(m_resizeColumnIndex, newSize);
            fireColumnResize(m_resizeColumnIndex, m_model.getColumnWidth(m_resizeColumnIndex));
            redraw();
        }
        
        // row resize?
        if (m_resizeRowIndex != -1) {
        	
            Rectangle rect = getClientArea();
            
            // calculate new size
            if (e.y > rect.y + rect.height - 1)
                e.y = rect.y + rect.height - 1;
            int newSize = e.y - m_resizeRowTop;
            if (newSize < m_model.getRowHeightMinimum())
            	newSize = m_model.getRowHeightMinimum();
            
            m_model.setRowHeight(m_resizeRowIndex, newSize);
            fireRowResize(m_resizeRowIndex, newSize);
            redraw();
            
        }
    }
    
    private void checkCursorToDisplay(int x, int y) {
		if ((m_resizeColumnIndex != -1) || (getColumnForResize(x, y) >= 0)) {

			if (m_defaultColumnResizeCursor == null) {
				setCursor(new Cursor(m_display, SWT.CURSOR_SIZEWE));
			} else {
				setCursor(m_defaultColumnResizeCursor);
			}

		} else if ((m_resizeRowIndex != -1) || (getRowForResize(x, y) >= 0)) {

			if (m_defaultRowResizeCursor == null) {
				setCursor(new Cursor(m_display, SWT.CURSOR_SIZENS));
			} else {
				setCursor(m_defaultRowResizeCursor);
			}

		} else if (m_cursorProvider != null) { // check cursor provider
			Point cell = getCellForCoordinates(x, y);
			Cursor cursor = m_cursorProvider.getCursor(cell.x, cell.y, x, y);
			if (cursor == null) {
				setCursor(m_defaultCursor);
			} else {
				setCursor(cursor);
			}
		} else { // show default cursor:
			setCursor(m_defaultCursor);
		}
	}

    protected void onMouseUp(MouseEvent e) {
       // if (e.button == 1)
        {
            if (m_model == null)
                return;

            setCapture(false);
            m_capture = false;

            // do resize:
            
            if ( m_resizeColumnIndex!=-1 || m_resizeRowIndex != -1 ) {
                
            	// Done resizing
            	
               	m_resizeColumnIndex = -1;
               	m_resizeRowIndex = -1;
               	checkCursorToDisplay(e.x, e.y);
                redraw();
                
            } else if (e.count == 1) { // check if we have to edit:
                Point click = new Point(e.x, e.y);
                Point cell = getCellForCoordinates(e.x, e.y);
                if (cell.x >= 0 && cell.y >= 0 && cell.x < m_model.getColumnCount() 
                		&& cell.y < m_model.getRowCount()) {
                    if (isHeaderCell(cell.x, cell.y)) {
                        KTableCellEditor editor = m_model.getCellEditor(cell.x, cell.y);
                        if (editor != null && 
                                ((editor.getActivationSignals() & KTableCellEditor.SINGLECLICK) != 0)  && 
                                editor.isApplicable(KTableCellEditor.SINGLECLICK, this, cell.x, cell.y, click, null, e.stateMask)) {
                            int oldFocusCol = m_focusCol;
                            int oldFocusRow = m_focusRow;
                            m_focusCol=cell.x; m_focusRow=cell.y;
                            openEditorInFocus(editor);
                            m_focusCol=oldFocusCol;
                            m_focusRow=oldFocusRow;
                        }
                    } else {
                        if (m_focusCol >= 0 && m_focusRow >= 0) {
                        	KTableCellEditor editor = m_model.getCellEditor(m_focusCol, m_focusRow);
                            Rectangle rect = getCellRect(m_focusCol, m_focusRow);
                            if (editor != null  &&  rect.contains(click)  && 
                                    (editor.getActivationSignals() & KTableCellEditor.SINGLECLICK)!=0 && 
                                    editor.isApplicable(KTableCellEditor.SINGLECLICK, this, m_focusCol, m_focusRow, click, null, e.stateMask)) {
                                openEditorInFocus(editor);
                            }
                        }
                    }
                }
            }

//            if (m_ClickColumnIndex != -1) {
//                int col = m_ClickColumnIndex;
//                int row = m_ClickRowIndex;
//                m_ClickColumnIndex = -1;
//                m_ClickRowIndex = -1;
//                if (m_CellEditor == null) {
//                    GC gc = new GC(this);
//                    if (!isFixedCell(col, row) && !isRowSelectMode())
//                        setContentAreaClipping(gc);
//                    Point valid = getValidCell(col, row);
//                    drawCell(gc, valid.x, valid.y);
//                    gc.dispose();
//                }
//            }
        }
    }
    
    /**
     * Finds and returns the cell that is below the control coordinates.
     * @param x The x coordinate in the ktable control
     * @param y The y coordinate in the ktable control
     * @return Returns the cell under the given point, or (-1, -1).
     * @since Public since March 2006 based on a newsgroup request.
     * @throws IllegalArgumentException If there is something wrong 
     *   with the spanning values given in the model. 
     */
    public Point getCellForCoordinates(int x, int y) {
        Point cell = new Point(-1,-1);
        cell.y = getRowForY(y);
        
        int colSum = 0;
		for (int i = 0; i < getFixedColumnCount(); i++) {
			int width = m_model.getColumnWidth(i);
			if (colSum <= x && colSum + width > x) {
				cell.x = i;
				return getValidCell(cell.x, cell.y);
			}
			colSum += width;
		}
		for (int i = m_LeftColumn; i < m_model.getColumnCount(); i++) {
			int width = m_model.getColumnWidth(i);
			if (colSum <= x && colSum + width > x) {
				cell.x = i;
				return getValidCell(cell.x, cell.y);
			}
			colSum += width;
		}

		// if FILL_WITH_LASTCOL is set, we must take into account that the column expands all the way to the right
		if (x >= colSum && x < getClientArea().x + getClientArea().width && (getStyle() & SWTX.FILL_WITH_LASTCOL) != 0) {
			cell.x = m_model.getColumnCount() - 1;
		}

        return getValidCell(cell.x, cell.y);
    }
    
    protected void onKeyDown(KeyEvent e) {
        boolean focusChanged = false;
        int newFocusRow = m_focusRow;
        int newFocusCol = m_focusCol;
        // System.out.println("code: " + e.keyCode + "   state: " + e.stateMask);
        if (m_model == null)
            return;
        
        if ((e.character == ' ') || (e.character == '\r')) {
            KTableCellEditor editor = m_model.getCellEditor(m_focusCol, m_focusRow);
            if (editor!=null && (editor.getActivationSignals() & KTableCellEditor.KEY_RETURN_AND_SPACE)!=0 &&
            		editor.isApplicable(KTableCellEditor.KEY_RETURN_AND_SPACE, this, m_focusCol, m_focusRow, null, e.character+"", e.stateMask )) {
                openEditorInFocus(editor);
                return;
            }
        }
        
        GC gc;
        
        if (e.keyCode == SWT.HOME  &&  (e.stateMask & SWT.CTRL) != 0) {
            // goto first row and column
            newFocusCol = m_model.getFixedHeaderColumnCount();
            newFocusRow = m_model.getFixedHeaderRowCount();
            focusChanged = true;
            
        } else if (e.keyCode == SWT.END  &&  (e.stateMask & SWT.CTRL) != 0) {
            // goto last row an column
            newFocusCol = m_model.getColumnCount() - 1;
            newFocusRow = m_model.getRowCount() - 1;
            focusChanged = true;
            
        } else if (e.keyCode == SWT.HOME) {
            newFocusCol = m_model.getFixedHeaderColumnCount();
            if (newFocusRow == -1)
                newFocusRow = m_model.getFixedHeaderRowCount();
            focusChanged = true;
        } else if (e.keyCode == SWT.END) {
            newFocusCol = m_model.getColumnCount() - 1;
            if (newFocusRow == -1)
                newFocusRow = m_model.getFixedHeaderRowCount();
            focusChanged = true;
        } else if (e.keyCode == SWT.ARROW_LEFT || 
                (e.keyCode==SWT.TAB && (e.stateMask & SWT.SHIFT) != 0)) {
            if (!isRowSelectMode()) {
                if (newFocusCol > m_model.getFixedHeaderColumnCount()) {
                    Point current = m_model.belongsToCell(m_focusCol, m_focusRow);
                    if (current==null) current = new Point(m_focusCol, m_focusRow);
                    Point newPt = m_model.belongsToCell(current.x-1, current.y);
                    if (newPt==null) newPt = new Point(current.x-1, current.y);
                    newFocusCol=newPt.x;
                    newFocusRow=newPt.y;                    
                } else if (newFocusCol==m_model.getFixedHeaderColumnCount() && 
                        newFocusRow>=m_model.getFixedHeaderRowCount()
                        && e.keyCode==SWT.TAB) {
                    // wrap around when traversing:
                    newFocusCol = m_model.getColumnCount()-1;
                    newFocusRow--;
                }
            }
            focusChanged = true;
        } else if (e.keyCode == SWT.ARROW_RIGHT || 
                (e.keyCode==SWT.TAB && (e.stateMask & SWT.SHIFT)==0)) {
            if (!isRowSelectMode()) {
                if (newFocusCol == -1) {
                    newFocusCol = m_model.getFixedHeaderColumnCount();
                    newFocusRow = m_model.getFixedHeaderRowCount();
                    
                } else if (newFocusCol == m_model.getColumnCount() - 1  && 
                           newFocusRow == m_model.getRowCount() - 1) {
                    // last cell selected, proceed to the next control:
                    Composite current = this;
                    boolean newFocusSet = false;
                    while (current.getParent()!=null && !newFocusSet) {
                        Composite parent = current.getParent();
                        for (int i=0; i<parent.getTabList().length; i++)
                            if (parent.getTabList()[i] == current && i<parent.getTabList().length-1) {
                                Control newFocus = parent.getTabList()[i+1];
                                while (newFocus instanceof Composite && ((Composite)newFocus).getTabList()!=null && ((Composite)newFocus).getTabList().length>0)
                                    newFocus = ((Composite)newFocus).getTabList()[0];
                                this.setSelection(new Point[]{}, false);
                                newFocus.setFocus();
                                newFocusSet=true;
                            }
                        current = parent;
                    }
                    
                } else if (newFocusCol < m_model.getColumnCount() - 1) {
                    Point old = new Point(m_focusCol, m_focusRow);
                    newFocusCol++;
                    Point next = m_model.belongsToCell(newFocusCol, newFocusRow);
                    
                    if (next == null) {
                        next = new Point(newFocusCol, newFocusRow);
                    }
                    
                    while (next.equals(old)  &&  newFocusCol < m_model.getColumnCount() - 1) { 
                        newFocusCol++;
                        next = m_model.belongsToCell(newFocusCol, newFocusRow);
                        if (next == null) {
                            next = new Point(newFocusCol, newFocusRow);
                        }
                    }
                    newFocusCol = next.x;
                    
                } else if (m_focusCol == m_model.getColumnCount() - 1  && 
                        m_focusRow != m_model.getRowCount() - 1  && 
                        e.keyCode == SWT.TAB) {
                    newFocusCol = m_model.getFixedHeaderColumnCount();
                    newFocusRow++;
                }
            }
            focusChanged = true;
        } else if (e.keyCode == SWT.ARROW_DOWN) {
            if (newFocusRow == -1) {
                newFocusRow = m_model.getFixedHeaderRowCount();
                newFocusCol = m_model.getFixedHeaderColumnCount();
            } else if (newFocusRow < m_model.getRowCount() - 1) {
                Point old = new Point(m_focusCol, m_focusRow);
                newFocusRow++;
                Point next = m_model.belongsToCell(newFocusCol, newFocusRow);
                if (next==null) next = new Point(newFocusCol, newFocusRow);
                while (next.equals(old)) { 
                    newFocusRow++;
                    next = m_model.belongsToCell(newFocusCol, newFocusRow);
                    if (next==null) next = new Point(newFocusCol, newFocusRow);
                }
            }
            focusChanged = true;
        } else if (e.keyCode == SWT.ARROW_UP) {
            if (newFocusRow > m_model.getFixedHeaderRowCount()) {
                Point current = m_model.belongsToCell(m_focusCol, m_focusRow);
                if (current==null) current = new Point(m_focusCol, m_focusRow);
                newFocusCol=current.x;
                newFocusRow=current.y-1;
            }
            focusChanged = true;
        } else if (e.keyCode == SWT.PAGE_DOWN) {
            newFocusRow += m_rowsVisible - 1;
            if (newFocusRow >= m_model.getRowCount())
                newFocusRow = m_model.getRowCount() - 1;
            if (newFocusCol == -1)
                newFocusCol = m_model.getFixedHeaderColumnCount();
            focusChanged = true;
        } else if (e.keyCode == SWT.PAGE_UP) {
            newFocusRow -= m_rowsVisible - 1;
            if (newFocusRow < m_model.getFixedHeaderRowCount())
                newFocusRow = m_model.getFixedHeaderRowCount();
            if (newFocusCol == -1)
                newFocusCol = m_model.getFixedHeaderColumnCount();
            focusChanged = true;
        } else if (e.keyCode == SWT.DEL) {
            Point[] selection = getCellSelection();
            
            for (Point cellCoord : selection) {
                if (cellCoord.x == -1) {
                    for (int colIdx = m_model.getFixedHeaderColumnCount(); colIdx < m_model.getColumnCount(); colIdx++) {
                        m_model.setContentAt(colIdx, cellCoord.y, "");
                        updateCell(colIdx, cellCoord.y);
                    }
                } if (cellCoord.y == -1) {
                    for (int rowIdx = m_model.getFixedHeaderRowCount(); rowIdx < m_model.getRowCount(); rowIdx++) {
                        m_model.setContentAt(cellCoord.x, rowIdx, "");
                        updateCell(cellCoord.x, rowIdx);
                    }
                } else {
                    m_model.setContentAt(cellCoord.x, cellCoord.y, "");
                    updateCell(cellCoord.x, cellCoord.y);
                }
            }

        } else if (e.keyCode == SWT.SPACE  &&  
                   (e.stateMask & SWT.CTRL) != 0  &&
                   (e.stateMask & SWT.SHIFT) != 0  &&
                   !isRowSelectMode()) {
            // Ctrl + SHIFT + SPACE - switch to column selection
            m_selectionMode = ESelectionMode.EColumn;
            gc = new GC(this);
            selectWhenNoModifierKey(m_focusCol, m_focusRow, gc);
            gc.dispose();

        } else if (e.keyCode == SWT.SPACE  &&  ((e.stateMask & SWT.SHIFT) != 0)) {
            
            // SHIFT + SPACE - switch to row selection
            m_selectionMode = ESelectionMode.ERow;
            gc = new GC(this);
            selectWhenNoModifierKey(m_focusCol, m_focusRow, gc);
            gc.dispose();
            
        } else if (e.keyCode == SWT.KEYPAD_ADD  &&  ((e.stateMask & SWT.CTRL) != 0)) {
            
            // insert column or row
            if (m_commandListener != null) {
                
                m_commandListener.ctrlPlusPressed();
                
                if (m_selectionMode == ESelectionMode.EColumn) {
                    m_commandListener.insertColumn();
                } else if (m_selectionMode == ESelectionMode.ERow) {
                    m_commandListener.insertRow();
                }
            }
            // do nothing if individual cells are selected
        } else if (e.keyCode == SWT.KEYPAD_SUBTRACT  &&  ((e.stateMask & SWT.CTRL) != 0)) {
            
            // delete selected columns or rows
            if (m_commandListener != null) {
                
                m_commandListener.ctrlMinusPressed();
                
                if (m_selectionMode == ESelectionMode.EColumn) {
                    m_commandListener.deleteColumn();
                } else if (m_selectionMode == ESelectionMode.ERow) {
                    m_commandListener.deleteRow();
                }
            }            
        } else if (isEditOnKeyEvent()) {
            // check if a key was pressed that forces editing:
            if (e.keyCode == SWT.BS)  {
            	KTableCellEditor editor = m_model.getCellEditor(m_focusCol, m_focusRow);
            	if (editor!=null && (editor.getActivationSignals() & KTableCellEditor.KEY_ANY)!=0 && 
            			editor.isApplicable(KTableCellEditor.KEY_ANY, this, m_focusCol, m_focusRow, null, e.character+"", e.stateMask )) {
            		openEditorInFocus(editor);
            		if (m_cellEditor!=null) {
            			m_cellEditor.setContent("");
            		}
            	}
                return;
            } else if ((Character.isLetterOrDigit(e.character) || 
                        e.keyCode > 32 && e.keyCode < 254 && e.keyCode != 127 ||
                        e.keyCode == SWT.F2 ||   // allow editing on F2 pressed
                        e.keyCode == SWT.F4 ||   // edit and drop down combo on F4 pressed
                        isCtrlSpace(e)) &&
                        e.keyCode != SWT.CTRL  &&  e.keyCode != SWT.ALT  && 
                       ((e.stateMask & SWT.CONTROL) == 0   ||  isCtrlSpace(e))  && 
                       (e.stateMask & SWT.ALT) == 0) {
                
                KTableCellEditor editor = m_model.getCellEditor(m_focusCol, m_focusRow);
                if (editor != null  && 
                    (editor.getActivationSignals() & KTableCellEditor.KEY_ANY) != 0  && 
                    editor.isApplicable(KTableCellEditor.KEY_ANY, this, 
                                        m_focusCol, m_focusRow, 
                                        null, e.character + "", e.stateMask)) {
                    
                    openEditorInFocus(editor);
                    
                    // The next condition was commented out by MK, 29.1.2015,
                    // because I couldn't find a use case when expanding combo
                    // box would be wrong. In fact, it seems to be always beneficial for 
                    // user to see other available options.
                    // if (e.keyCode == SWT.F4  ||  isCtrlSpace(e)) {  // drop down combo on F4 
                        m_cellEditor.showList();  
                    //}
                        
                        
                    // do not reset editor contents on F2 or F4 or Ctrl-Space -
                    // otherwise combo selects the first item, but we want the selection
                    // to stay on currently selected item.
                    if (m_cellEditor != null  &&  e.keyCode != SWT.F2  
                        &&  e.keyCode != SWT.F4  &&  !isCtrlSpace(e)) { 
                        m_cellEditor.setContent(e.character + "");
                        
                        // sending of this event is needed to trigger
                        // content-assist already when first key is pressed. 
                        if (m_cellEditor instanceof KTableCellEditorText2) {
                            // we have to create Event from KeyEvent, because
                            // notifyListeners() below accepts only Event.
                            Event ev= new Event();
                            
                            ev.character = e.character;
                            ev.keyCode = e.keyCode;
                            ev.keyLocation = e.keyLocation;
                            ev.stateMask = e.stateMask;
                            ev.doit = e.doit;

                            ev.display = e.display;
                            ev.widget = e.widget;
                            ev.time = e.time;
                            ev.data = e.data;

                            ev.type = SWT.KeyDown;
                            ((KTableCellEditorText2)m_cellEditor).notifyListeners(ev.type, 
                                                                                  ev);
                        }
                    }
                    return;
                }
            }
        }

        if (focusChanged) {
            // make sure it is a valid, visible cell and not overlapped:
            Point valid = m_model.belongsToCell(newFocusCol, newFocusRow);
            if (valid!=null) {
                newFocusCol = valid.x;
                newFocusRow = valid.y;
            }
            focusCell(newFocusCol, newFocusRow, e.stateMask);
            if (!isCellFullyVisible(m_focusCol, m_focusRow))
                scrollToFocus();
        }
    }

    
    private boolean isCtrlSpace(KeyEvent e) {
        return e.keyCode == SWT.SPACE  &&  (e.stateMask & SWT.CONTROL) != 0;
    }

    
    protected void onMouseDoubleClick(MouseEvent e) {
        if (m_model == null)
            return;
        if (e.button == 1) {

        	if ((e. y > 0 && e.y < getHeaderHeight()) || (getStyle() & SWTX.RESIZE_ON_ENTIRE_COLUMN) != 0) {
        		/*
        		 * automatic resize happens: on double click on cell boundary in header; or on all cell boundaries
        		 * when the appropriate style flag has been set
        		 */
                int columnIndex = getColumnForResize(e.x, e.y);
                m_resizeColumnIndex=-1;
                if (resizeColumnOptimal(columnIndex) != -1)
                	return;
        	}
        	
            /* If this code is active, it never gets to isHeaderCell() below.
             * if (e.y < getHeaderHeight()) {
                
                // double click in header area
                Point cell = getCellForCoordinates(e.x, e.y);
                checkCursorToDisplay(e.x, e.y);
                fireFixedCellDoubleClicked(cell.x, cell.y, e.stateMask);
                return;
                
            } else {
            */
        	
        	Point click = new Point(e.x, e.y);
        	Point cell = getCellForCoordinates(e.x, e.y);
        	if (cell.x < 0  ||  cell.y < 0) {
        	    // getCellForCoordinates() above returns (-1, -1) if clicked outside table
        	    return;
        	}
        	if (isHeaderCell(cell.x, cell.y)) {
        	    KTableCellEditor editor = m_model.getCellEditor(cell.x, cell.y);
        	    if (editor != null && 
        	            (editor.getActivationSignals() & KTableCellEditor.DOUBLECLICK) != 0 && 
        	            editor.isApplicable(KTableCellEditor.DOUBLECLICK, 
        	                                this, cell.x, cell.y, 
        	                                click, null, e.stateMask)) {

        	        int oldFocusCol = m_focusCol;
        	        int oldFocusRow = m_focusRow;

        	        m_focusCol = cell.x; 
        	        m_focusRow = cell.y;

        	        openEditorInFocus(editor);
        	        m_cellEditor.showList();  // drop down combo  

        	        m_focusCol = oldFocusCol;
        	        m_focusRow = oldFocusRow;
        	    }
        	    fireFixedCellDoubleClicked(cell.x, cell.y, e.stateMask);
        	} else {
        	    if (m_focusCol >= 0 && m_focusRow >= 0) {

        	        KTableCellEditor editor = m_model.getCellEditor(m_focusCol, m_focusRow);

        	        Rectangle rect = getCellRect(m_focusCol, m_focusRow);

        	        if (editor != null  &&  rect.contains(click)  &&
        	                (editor.getActivationSignals() & KTableCellEditor.DOUBLECLICK)!=0 && 
        	                editor.isApplicable(KTableCellEditor.DOUBLECLICK, this, m_focusCol, m_focusRow, click, null, e.stateMask)) {

        	            openEditorInFocus(editor);
        	            m_cellEditor.showList();  // drop down combo  
        	        }
        	        fireCellDoubleClicked(cell.x, cell.y, e.stateMask);
        	    }
        	}
        }
    }

    
    /**
     * Listener Class that implements fake tooltips. The tooltip content
     * is retrieved from the tablemodel.
     */
    class TooltipListener implements Listener {
        Shell tip = null;
        Label label = null;
        
    	final Listener labelListener = new Listener () {
    		public void handleEvent (Event event) {
    			Label label = (Label)event.widget;
    			Shell shell = label.getShell ();
    			// forward mouse events directly to the underlying KTable
    			switch (event.type) {
    				case SWT.MouseDown:
    					Event e = new Event ();
    					e.item = KTable.this;
    					e.button = event.button;
    					e.stateMask = event.stateMask;
    					notifyListeners (SWT.MouseDown, e);
    					// fall through
    				default:
    					shell.dispose ();
    					break;
    			}
    		}
    	};
    	
        public void handleEvent (Event event) {
            switch (event.type) {
                case SWT.Dispose:
                case SWT.KeyDown:
                case SWT.MouseDown:
                case SWT.MouseDoubleClick:
                case SWT.MouseMove: 
                case SWT.Selection: // scrolling
                case SWT.MouseExit:               
                {
                    if (tip == null) break;
                    tip.dispose ();
                    tip = null;
                    label = null;
                    break;
                }
                case SWT.MouseHover: {
                    if (tip != null  && !tip.isDisposed ()) tip.dispose ();
                    
                    Point cell = getCellForCoordinates(event.x, event.y);
                    String tooltip = null;
                    if (cell.x >= 0 && cell.x < m_model.getColumnCount() 
                    		&& cell.y >= 0 && cell.y < m_model.getRowCount())
                    	tooltip = m_model.getTooltipAt(cell.x, cell.y);
                    
                    // check if there is something to show, and abort otherwise:
                    if (((tooltip == null || tooltip.isEmpty()) && 
                            (m_nativTooltip == null || m_nativTooltip.isEmpty())) 
                            || (cell == null || cell.x == -1 || cell.y == -1)) {
                        tip = null;
                        label = null;
                        return;
                    }
                    
                    tip = new Shell (getShell(), SWT.ON_TOP);
                    GridLayout gl = new GridLayout();
                    gl.marginWidth=2;
                    gl.marginHeight=2;
                    tip.setLayout (gl);
                    tip.setBackground(getDisplay().getSystemColor (SWT.COLOR_INFO_BACKGROUND));
                    label = new Label (tip, SWT.NONE);                    
                    label.setLayoutData(new GridData(GridData.FILL_BOTH));
                    label.setForeground (getDisplay().getSystemColor (SWT.COLOR_INFO_FOREGROUND));
                    label.setBackground (getDisplay().getSystemColor (SWT.COLOR_INFO_BACKGROUND));
                    if (tooltip!=null && !tooltip.equals(""))
                        label.setText (tooltip);
                    else
                        label.setText(m_nativTooltip);
                    label.addListener (SWT.MouseExit, labelListener);
                    label.addListener (SWT.MouseDown, labelListener);
                    label.addListener(SWT.MouseMove, labelListener);
                    Point size = tip.computeSize (SWT.DEFAULT, SWT.DEFAULT);
                    
                    int y = 15; // currently the windows default???
                    int x = 10;
                    
                    /*  see TableMouseListener.showTooltipShell() for comment
                    
                    if (m_defaultCursorSize!=null && 
                            m_defaultCursorSize.x>=0 && 
                            m_defaultCursorSize.y>=0) {
                        y = m_defaultCursorSize.y+1;
                        x = -m_defaultCursorSize.x;
                    }
                    
                    // place the shell under the mouse, but check that the
                    // bounds of the table are not overlapped.
                    Rectangle tableBounds = KTable.this.getBounds();
                    
                    if (event.x + x + size.x > tableBounds.x + tableBounds.width) {
                        event.x -= event.x + x + size.x - tableBounds.x - tableBounds.width;
                    }
                    
                    if (event.y + y + size.y > tableBounds.y + tableBounds.height) {
                        event.y -= event.y + y + size.y - tableBounds.y - tableBounds.height;
                    } */
                    
                    Point pt = toDisplay (event.x + x, event.y + y);
                    setShellBounds(tip, pt.x, pt.y, size.x, size.y);
                    tip.setVisible (true);
                }
            }
        }
        
        private void setShellBounds(Shell shell, int x, int y, int w, int h) {
            Rectangle b = shell.getDisplay().getBounds();
            if (x + w >= b.x + b.width) {
                x = b.x + b.width - w;
            }
            if (y + h >= b.y + b.height) {
                y = b.y + b.height - h;
            }
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            shell.setBounds(x, y, w, h);
        }
    }
    
    /**
     * Sets the global tooltip for the whole table.<br>
     * Note that this is only shown if the cell has no tooltip set.
     * For tooltips on cell level (that overwrite this value), look
     * for the method <code>getTooltipText()</code>.
     * @see de.kupzog.ktable.KTableModel#getTooltipAt(int, int)
     * @see de.kupzog.ktable.KTable#getToolTipText()
     * @param tooltip The global tooltip for the table.
     */
    @Override
    public void setToolTipText(String tooltip) {
        m_nativTooltip = tooltip;
    }
    
    /**
     * Returns the global tooltip for the whole table.<br>
     * Note that this is not shown when there is a non-empty tooltip
     * for the cell.
     * @see de.kupzog.ktable.KTable#setToolTipText(String)
     * @see de.kupzog.ktable.KTableModel#getTooltipAt(int, int)
     */
    @Override
    public String getToolTipText() {
        return m_nativTooltip;
    }
    
    /**
     * Resizes the given column to its optimal width.
     * 
     * Is also called if user doubleclicks in the resize area of a resizable
     * column.
     * 
     * The optimal width is determined by asking the CellRenderers for the
     * visible cells of the column for the optimal with and taking the minimum
     * of the results. Note that the optimal width is only determined for the
     * visible area of the table because otherwise this could take very long
     * time.
     * 
     * @param column
     *            The column to resize
     * @return int The optimal with that was determined or -1, if column out of
     *         range.
     */
    public int resizeColumnOptimal(int column) {
        checkWidget();
        if (column >= 0 && column < m_model.getColumnCount()) {
            int optWidth = 5;
            GC gc = new GC(this);
			for (int i = 0; i < m_model.getFixedHeaderRowCount(); i++) {
				Point valid = getValidCell(column, i);
				if (valid.x == column && valid.y == i) {
					int width = m_model.getCellRenderer(column, i).getOptimalWidth(gc, column, i,
							m_model.getContentAt(column, i), true, m_model);
					if (width > optWidth)
						optWidth = width;
				}
			}
			for (int i = Math.max(0, m_TopRow - INVISIBLE_ROWS_TO_REGARD_DURING_WIDTH_OPTIMIZATION); i < Math.min(
					m_TopRow + m_rowsVisible + INVISIBLE_ROWS_TO_REGARD_DURING_WIDTH_OPTIMIZATION, m_model
							.getRowCount()); i++) {
				Point valid = getValidCell(column, i);
				if (valid.x == column && valid.y == i) {
					int width = m_model.getCellRenderer(column, i).getOptimalWidth(gc, column, i,
							m_model.getContentAt(column, i), true, m_model);
					if (width > optWidth)
						optWidth = width;
				}
			}
            gc.dispose();
            m_model.setColumnWidth(column, optWidth);
            redraw();
            return optWidth;
        }
        return -1;        
    }

    /**
     * This method activates the cell editor on the current focus cell, if the
     * table model allows cell editing for this cell.
     */
    public void openEditorInFocus(KTableCellEditor editor) {
        checkWidget();
        // should be opened before this call
        // m_cellEditor = m_model.getCellEditor(m_focusCol, m_focusRow);
        m_cellEditor = editor;
        if (m_cellEditor != null) {
            scrollToFocus();
            Rectangle r = getCellRect(m_focusCol, m_focusRow);
            m_cellEditor.open(this, m_focusCol, m_focusRow, r);
        }
    }

    /**
     * Checks whether an editor is currently open at the specified position.
     * 
     * @param col The column index.
     * @param row The row index.
     * @return Whether an editor is currently open.
     */
    public boolean isEditorOpen(int col, int row) {
    	Point valid = getValidCell(col, row);
    	return m_cellEditor != null && m_cellEditor.m_Col == valid.x && m_cellEditor.m_Row == valid.y;
    }

    /**
     * Scrolls the table so that the given cell is top left.
     * @param col The column index.
     * @param row The row index.
     */
    public void scroll(int col, int row) {
        if (col<0 || col>=m_model.getColumnCount() ||
            row<0 || row>=m_model.getRowCount())
            return;
        
        m_TopRow = row;
        m_LeftColumn=col;
        redraw();
    }
    
    protected void scrollToFocus() {
    	if (m_focusCol < 0 || m_focusRow < 0 
    			|| m_focusCol >= m_model.getColumnCount() || m_focusRow >= m_model.getRowCount()) {
    		return;
    	}
    	
        boolean change = false;

        // vertical scroll allowed?
        if (getVerticalBar() != null) {
            if (m_focusRow < m_TopRow && !(m_focusRow<getFixedRowCount())) {
                m_TopRow = m_focusRow;
                change = true;
            }

            if (m_focusRow >= m_TopRow + m_rowsFullyVisible) {
                m_TopRow = Math.min(m_model.getRowCount() - 1, m_focusRow - m_rowsFullyVisible + 1);
                change = true;
            }
        }

        // horizontal scroll allowed?
        if (getHorizontalBar() != null) {
            if (m_focusCol < m_LeftColumn && !(m_focusCol < getFixedColumnCount())) {
                m_LeftColumn = m_focusCol;
                change = true;
            }

            if (m_focusCol >= m_LeftColumn + m_columnsFullyVisible) {
                int oldLeftCol = m_LeftColumn;
                Rectangle rect = getClientArea();
                int leftColX = getColumnLeft(m_focusCol);
                int focusCellWidth = getCellRect(m_focusCol, m_focusRow).width;
                while (m_LeftColumn < m_focusCol && m_LeftColumn < m_model.getColumnCount()
                		&& ( leftColX<0 || leftColX + focusCellWidth > rect.width + rect.x)) {
                	m_LeftColumn++;
                	leftColX = getColumnLeft(m_focusCol);
                }
                change |= (oldLeftCol != m_LeftColumn);
            }
        }

        if (change)
            redraw();
    }

    protected void fireCellSelection(int col, int row, int statemask) {
    	Point validCell = getValidCell(col, row);
        for (int i = 0; i < m_cellSelectionListeners.size(); i++) {
            ((KTableCellSelectionListener) m_cellSelectionListeners.get(i))
                    .cellSelected(validCell.x, validCell.y, statemask);
        }
    }
    
    protected boolean fireCellClicked(int col, int row, Rectangle cellRect, int x, int y, int button) {
		if (col < 0 || row < 0 || col >= m_model.getColumnCount() || row >= m_model.getRowCount()) {
			return false;
		}
		Point validCell = getValidCell(col, row);
		for (int i = 0; i < m_cellClickListeners.size(); i++) {
			if (m_cellClickListeners.get(i).cellClicked(validCell.x, validCell.y,
					cellRect, x, y, button, this)) {
				return true;
			}
		}
		return false;
	}
    
    protected void fireCellDoubleClicked(int col, int row, int statemask) {
    	Point validCell = getValidCell(col, row);
        for (int i = 0; i < m_cellDoubleClickListeners.size(); i++) {
            m_cellDoubleClickListeners.get(i)
                    .cellDoubleClicked(validCell.x, validCell.y, statemask);
        }
    }
    
    protected void fireFixedCellDoubleClicked(int col, int row, int statemask) {
    	Point validCell = getValidCell(col, row);
        for (int i = 0; i < m_cellDoubleClickListeners.size(); i++) {
            m_cellDoubleClickListeners.get(i)
                    .fixedCellDoubleClicked(validCell.x, validCell.y, statemask);
        }
    }
    
    protected void fireFixedCellSelection(int col, int row, int statemask) {
    	Point validCell = getValidCell(col, row);
        for (int i = 0; i < m_cellSelectionListeners.size(); i++) {
            m_cellSelectionListeners.get(i)
                    .fixedCellSelected(validCell.x, validCell.y, statemask);
        }
    }

    protected void fireColumnResize(int col, int newSize) {
        for (int i = 0; i < m_cellResizeListeners.size(); i++) {
            m_cellResizeListeners.get(i)
                    .columnResized(col, newSize);
        }
    }

    protected void fireRowResize(int row, int newSize) {
        for (int i = 0; i < m_cellResizeListeners.size(); i++) {
            m_cellResizeListeners.get(i)
                    .rowResized(row, newSize);
        }
    }

    /**
     * Adds a listener that is notified when a cell is selected.
     * 
     * This can happen either by a click on the cell or by arrow keys. Note that
     * the listener is not called for each cell that the user selects in one
     * action using Shift+Click. To get all these cells use the listener and
     * getCellSelecion() or getRowSelection().
     * 
     * @param listener
     */
    public void addCellSelectionListener(KTableCellSelectionListener listener) {
        m_cellSelectionListeners.add(listener);
    }

    /**
     * Adds a listener that is notified when a cell is resized.
     * This happens when the mouse button is released after a resizing.
     * @param listener
     */
    public void addCellResizeListener(KTableCellResizeListener listener) {
        m_cellResizeListeners.add(listener);
    }
    
    /**
     * Adds a listener that is notified when a cell is doubleClicked.
     *
     * @param listener
     */
    public void addCellDoubleClickListener(KTableCellDoubleClickListener listener) {
        m_cellDoubleClickListeners.add(listener);
    }
    
    /**
     * Adds a listener that can intercept clicks on a cell (called on Mouse 
     * Down event).
     * 
     * @param listener
     */
    public void addClickInterceptionListener(KTableClickInterceptionListener listener) {
    	m_cellClickListeners.add(listener);
    }

    /**
     * Removes the listener if present. 
     * Returns true, if found and removed from the list of listeners.
     */
    public boolean removeCellSelectionListener(KTableCellSelectionListener listener) {
        return m_cellSelectionListeners.remove(listener);
    }

    /**
     * Removes the listener if present. 
     * Returns true, if found and removed from the list of listeners.
     */
    public boolean removeCellResizeListener(KTableCellResizeListener listener) {
        return m_cellResizeListeners.remove(listener);
    }
    
    /**
     * Removes the listener if present.
     * Returns true, if found and removed from the list of listeners.
     */
    public boolean removeDoubleClickListener(KTableCellDoubleClickListener listener) {
        return m_cellDoubleClickListeners.remove(listener);
    }
    
    /**
     * Removes the listener if present.
     * Returns true, if found and removed from the list of listeners.
     */
    public boolean removeClickInterceptionListener(KTableClickInterceptionListener listener) {
    	return m_cellClickListeners.remove(listener);
    }

    //////////////////////////////////////////////////////////////////////////////
    // SELECTION
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Returns true if in "Full Selection Mode" 
     * Mode is determined by style bits in the constructor or 
     * by <code>setStyle()</code>. (style: SWT.FULL_SELECTION)
     * @return boolean
     */
    public boolean isRowSelectMode() {
        return (getStyle() & SWT.FULL_SELECTION) == SWT.FULL_SELECTION;
    }

    
    /**
     * Returns true if in "Multi Selection Mode".
     * Mode is determined by style bits in the constructor (SWT.MULTI)
     * or by <code>getStyle()</code>.
     */
    public boolean isMultiSelectMode() {
        return (getStyle() & SWT.MULTI) == SWT.MULTI;
    }
    
    /**
     * @return Returns true if the selection is kept & shown
     * even if the KTable has no focus.
     * @see #setShowSelectionWithoutFocus(boolean)
     */
    protected boolean isShowSelectionWithoutFocus() {
        return (getStyle() & SWT.HIDE_SELECTION) != SWT.HIDE_SELECTION;
    }
    
    /**
     * @return Returns true if selections are also shown in 
     * the vertical and horizontal table headers.
     * @see #setHighlightSelectionInHeader(boolean)
     */
    protected boolean isHighlightSelectionInHeader() {
        return (getStyle() & SWTX.MARK_FOCUS_HEADERS) == SWTX.MARK_FOCUS_HEADERS;
    }
    
    /**
     * @return Returns wether the celleditor is
     * activated on a (every) keystroke - like in Excel - 
     * and not only by ENTER. Activated by style <code>SWTX.EDIT_ON_KEY</code><p>
     * However, note that every cell editor can specify 
     * the event types it accepts.
     * @see #setEditOnKeyEvent(boolean)
     */
    protected boolean isEditOnKeyEvent() {
    	return (getStyle() & SWTX.EDIT_ON_KEY) == SWTX.EDIT_ON_KEY;
    }

    protected void clearSelectionWithoutRedraw() {
        m_selections.clear();
    }

    /**
     * Clears the current selection (in all selection modes).
     */
    public void clearSelection() {
        checkWidget();
        clearSelectionWithoutRedraw();
        m_focusCol = -1;
        m_focusRow = -1;
        redraw();
    }

    /**
     * Works in both modes: Cell and Row Selection.
     * Has no redraw functionality!<p>
     * 
     * Returns true, if added to selection.
     */
    protected boolean toggleSelection(int col, int row) {

        if (isMultiSelectMode()) {
            
            SelectionItem selItem = null;
            
            // allow toggling only of selection type currently active            
            switch (m_selectionMode) {
            case ECell:
                if (isDataCell(col, row)) { 
                    selItem = new SelectionItem(col, row);
                }
                break;
            case EColumn:
                if (isColumnHeaderCell(col, row)) {
                    selItem = new SelectionItem(col, -1);
                }
                break;
            case ERow:
                if (isRowHeaderCell(col, row)) {
                    selItem = new SelectionItem(-1, row);
                }
                break;
            default:
                break;
            }
            
            // if did not exist (remove() returned false), add it
            if (selItem != null  && !m_selections.remove(selItem)) {
                m_selections.add(selItem);
                return true;
            }
        }
        return false;
    }

    /**
     * Works in both modes: Cell and Row Selection.
     * Has no redraw functionality!
     */
    protected void addToSelectionWithoutRedraw(int col, int row) {
        
        switch (m_selectionMode) {
        case ECell:
            m_selections.add(new SelectionItem(col, row));
            break;
        case EColumn: {
            Point spannedCell = getValidCell(col, row);
            // select all columns below spanned cell - even if not visible
            for (int colIdx = spannedCell.x; colIdx < m_model.getColumnCount(); colIdx++) {
                Point nextCell = getValidCell(colIdx, row);
                if (nextCell.x == spannedCell.x) {
                    m_selections.add(new SelectionItem(colIdx, -1));
                } else {
                    break; // we've reached the end of the spanned cell
                }
            }
        } break;
        case ERow: {
            Point spannedCell = getValidCell(col, row);
            // select all columns below spanned cell - even if not visible
            for (int rowIdx = spannedCell.y; rowIdx < m_model.getRowCount(); rowIdx++) {
                Point nextCell = getValidCell(col, rowIdx);
                if (nextCell.y == spannedCell.y) {
                    m_selections.add(new SelectionItem(-1, rowIdx));
                } else {
                    break;  // we've reached the end of the spanned cell
                }
            }
        } break;
        default:
            break;
        }
    }

    /**
     * Selects the given cell. If scroll is true, 
     * it scrolls to show this cell if neccessary.
     * In Row Selection Mode, the given row is selected
     * and a scroll to the given column is done.
     * Does nothing if the cell does not exist. <p>
     * Note that if you use a sorted model, don't forget to map the row index!
     * @param col
     * @param row
     * @param scroll
     */
    public void setSelection(int col, int row, boolean scroll) {
        checkWidget();
        
        if (col < m_model.getFixedHeaderColumnCount()) {
            // if header cell is given, select row
            m_selectionMode = ESelectionMode.ERow;
            GC gc = new GC(this);
            m_focusCol = col;
            m_focusRow = row;
            selectWhenNoModifierKey(m_focusCol, m_focusRow, gc);
            gc.dispose();
            
        } else if (col < m_model.getColumnCount()
                && col >= m_model.getFixedHeaderColumnCount()
                && row < m_model.getRowCount()
                && row >= m_model.getFixedHeaderRowCount()) {
            // if body cell is given, select the cell
            focusCell(col, row, 0);
            if (scroll) {
                scrollToFocus();
            }
        }
    }
    
    /**
     * Selects the given cells - Point.x is column index, Point.y is
     * row index. If scroll is true, scrolls so that the first of the 
     * given cell indexes becomes fully visible.<p>
     * Don't forget to map the row index if a sorted model is used.
     * @param selections The selected cells as points. If <code>null</code>
     * or an empty array, clears the selection.
     * @param scroll Wether it is reuqested to scroll to the first cell
     * given as selection.
     * @see KTableSortedModel#mapRowIndexToTable(int)
     */
    public void setSelection(Point[] selections, boolean scroll) {
        checkWidget();
        if (selections == null || selections.length < 1) {
            clearSelection();
            return;
            
        } else if (isMultiSelectMode()){
            try {
                this.setRedraw(false);
                for (int i = 0; i < selections.length; i++) {
                    int col = selections[i].x; 
                    int row = selections[i].y;
                    if (col < m_model.getColumnCount()
                            && col >= m_model.getFixedHeaderColumnCount()
                            && row < m_model.getRowCount()
                            && row >= m_model.getFixedHeaderRowCount())
                        if (i==0)
                            focusCell(col, row, SWT.CTRL);
                        else 
                            addToSelectionWithoutRedraw(col, row);
                }
                
                if (scroll) {
                    scrollToFocus();
                }
            } finally {
                this.setRedraw(true);
            }
        } else {
            setSelection(selections[0].x, selections[0].y, scroll);
        }
    }

    /**
     * Returns true, if the given cell is selected.
     * Works also in Row Selection Mode.
     * @param col the column index.
     * @param row the row index.
     * @return boolean Returns true if the given cell is selected.
     */
    public boolean isCellSelected(int col, int row) {
        checkWidget();
        Point v = getValidCell(col, row);
        col = v.x;
        row = v.y;
        if (!isMultiSelectMode()) {
            switch (m_selectionMode) {
            case ECell:
                return (col == m_focusCol && row == m_focusRow);
            case EColumn:
                return (col == m_focusCol);
            case ERow:
                return (row == m_focusRow);
            default:
                break;
            }
            return false;
        }

        switch (m_selectionMode) {
        case ECell:
            return m_selections.contains(new SelectionItem(col, row));
        case EColumn:
            return m_selections.contains(new SelectionItem(col, -1));
        case ERow:
            return m_selections.contains(new SelectionItem(-1, row));
        default:
            break;
        }

        return false;
    }

    /**
     * Returns true, if the given row is selected.
     * Returns always false if not in Row Selection Mode!<p>
     * If you use a sorted model, don't forget to map the row index first.
     * @param row The row index as seen by the KTable.
     * @return boolean returns true if the row is selected at the moment.
     */
    public boolean isRowSelected(int row) {
        return m_selections.contains(new SelectionItem(-1, row));
    }

    /**
     * Returns an array of the selected row numbers.
     * Returns null if not in Row Selection Mode.
     * Returns an array with one or none element if not in Multi Selection Mode.<p>
     * NOTE: This returns the cell indices as seen by the KTable. If you use a sorting model,
     * don't forget to map the indices properly.
     * @return int[] Returns an array of rows that are selected. 
     * @see KTableSortedModel#mapRowIndexToModel(int)
     */
    public int[] getRowSelection() {
        
        checkWidget();
        
        if (m_selectionMode != ESelectionMode.ERow) {
            return null;
        }
        
        if (!isMultiSelectMode()) {
            
            if (m_focusRow < 0) {
                return new int[0];
            }
            
            int[] tmp = new int[1];
            tmp[0] = m_focusRow;
            return tmp;
        }

        int[] rows = new int[m_selections.size()];
        int idx = 0;
        
        for (SelectionItem selItem : m_selections) {
            rows[idx++] = selItem.m_row;
        }
        return rows;
    }

    /**
     * Returns an array of the selected cells as Point[].
     * The columns are stored in the x fields, rows in y fields.
     * Returns null if in Row Selection Mode. <br>
     * Returns an array with one or none element if not in Multi Selection Mode.<p>
     * NOTE: This returns the cell indices as seen by the KTable. If you use a sorting model,
     * don't forget to map the indices properly.
     * @return int[] Returns an array of points that are selected. 
     * These are no copies, so don't directly manipulate them...
     * 
     * @see KTableSortedModel#mapRowIndexToModel(int)
     */
    public Point[] getCellSelection() {
        checkWidget();
        
        /* if (m_selectionMode != ESelectionMode.ECell) {
            return null;
        } */
        
        if (!isMultiSelectMode()) {
            if (m_focusRow < 0 || m_focusCol < 0)
                return new Point[0];
            Point[] tmp = new Point[1];
            tmp[0] = new Point(m_focusCol, m_focusRow);
            return tmp;
        }

        Point[] rows = new Point[m_selections.size()];
        int idx = 0;
        
        for (SelectionItem selItem : m_selections) {
            rows[idx++] = new Point(selItem.m_column, selItem.m_row);
        }
        return rows;
    }
    
    
    /**
     * 
     * @return rectangualr area, which includes all selected cell, but some cells
     * int the area may not be selected.
     */
    public Rectangle getSelectedRect()
    {
        int minx = Integer.MAX_VALUE;
        int maxx = 0;
        int miny = Integer.MAX_VALUE;
        int maxy = 0;
        
        for (SelectionItem item : m_selections) {
            switch (m_selectionMode) {
            case ECell:
                minx = Math.min(minx, item.m_column);
                maxx = Math.max(maxx, item.m_column);
                miny = Math.min(miny, item.m_row);
                maxy = Math.max(maxy, item.m_row);
                break;
            case EColumn:
                minx = Math.min(minx, item.m_column);
                maxx = Math.max(maxx, item.m_column);
                miny = 0;
                maxy = m_model.getRowCount();
                break;
            case ERow:
                minx = 0;
                maxx = m_model.getColumnCount();
                miny = Math.min(miny, item.m_row);
                maxy = Math.max(maxy, item.m_row);
                break;
            default:
                throw new IllegalStateException("Invalid cell selection mode in table: " +
                                                m_selectionMode);
            }
        }
            
        return new Rectangle(minx, miny, maxx - minx, maxy - miny);
    }
    
    
    /**
     * @return Returns the non-fixed cells that are currently visible.
     * The returned rectangle has the columns on the x / width coordinates
     * and the rows at the y / height coordinates.
     */
    public Rectangle getVisibleCells() {
        return new Rectangle(m_LeftColumn, m_TopRow, 
                m_columnsVisible, m_rowsVisible);
    }
    
    /**
     * Internal helper method to determine wether the cell at the 
     * given position is to be highlighted because it is a header cell
     * that corresponds to a selected cell.
     * @param col The column index
     * @param row The row index
     * @return true if the cell should be highlighted.
     */
    private boolean highlightSelectedRowCol(int col, int row) {
        
        if (!isHighlightSelectionInHeader() || !isHeaderCell(col, row)) {
            return false;
        }
        
        Point[] sel = getCellSelection();
        if (sel != null) {
            for (int i = 0; i < sel.length; i++) {
                
                if (isRowHeaderCell(col, row)) {
                    Point leftTopRendered = getValidCell(col, row);
                    Point leftTopSelectionInHeader = getValidCell(col, sel[i].y);
                    if (leftTopRendered.equals(leftTopSelectionInHeader)) {
                        return true;
                    }
                }

                if (isColumnHeaderCell(col, row)) { 
                    Point leftTopRendered = getValidCell(col, row);
                    Point leftTopSelectionInHeader = getValidCell(sel[i].x, row);
                    if (leftTopRendered.equals(leftTopSelectionInHeader)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////
    // MODEL
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Sets the table model.
     * The table model provides data to the table.
     * @see de.kupzog.ktable.KTableModel for more information.
     * @param model The KTableModel instance that provides the table with all 
     * necessary data!
     */
    public void setModel(KTableModel model) {
        checkWidget();
        m_model = model;
        m_focusCol = -1;
        m_focusRow = -1;
        clearSelectionWithoutRedraw();
        
        // implement autoscrolling if needed:
        if ((getStyle() & SWTX.AUTO_SCROLL) == SWTX.AUTO_SCROLL)
            updateScrollbarVisibility();
        
        redraw();
    }

    /**
     * returns the current table model
     * @return KTableModel 
     */
    public KTableModel getModel() {
        return m_model;
    }

    
    /**
     * @return the selectionMode
     */
    public ESelectionMode getSelectionMode() {
        return m_selectionMode;
    }

    
    /**
     * Helper method to quickly get the number of fixed columns.
     * @return
     */
    protected int getFixedColumnCount() {
        return m_model.getFixedHeaderColumnCount()+m_model.getFixedSelectableColumnCount();
    }
    
    
    /**
     * Helper method to quickly get the number of fixed rows.
     * @return
     */
    protected int getFixedRowCount() {
        return m_model.getFixedHeaderRowCount()+m_model.getFixedSelectableRowCount();
    }
    

    protected void updateScrollbarVisibility() {
        try {
            KTableModel model = getModel();
            Rectangle actualSize = getClientArea();
            // vertical:
            boolean showVertBar = false;
            int theoreticalHeight = 1;
            for (int i=0; i<model.getRowCount(); i++) {
                theoreticalHeight+=model.getRowHeight(i);
                if (theoreticalHeight>actualSize.height) {
                    showVertBar=true;
                    break;
                }
            }
            getVerticalBar().setVisible(showVertBar);
            // horizontal: 
            int theoreticalWidth = 0;
            for (int i = 0; i < model.getColumnCount(); i++)
                theoreticalWidth += model.getColumnWidth(i);

            //getHorizontalBar().setVisible(actualSize.width < theoreticalWidth);

            // scroll back to the left when table is resized!
            boolean horVisible = actualSize.width < theoreticalWidth;
            if (getHorizontalBar().isVisible() != horVisible){
                getHorizontalBar().setVisible(horVisible);
                // Scroll the table back to the left
                if (!horVisible){
                    getHorizontalBar().setSelection(0);
                    m_LeftColumn = 0;
                }
            }            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        redraw();
    }
    
    
    private int getColumnWidth(int col) {
        if (col==m_model.getColumnCount()-1 && (getStyle()&SWTX.FILL_WITH_LASTCOL)!=0) {
            // expand the width to grab all the remaining space with this last col.            
            Rectangle cl = getClientArea();
            int remaining = cl.x+cl.width-2-getColumnLeft(col);
            return Math.max(remaining, m_model.getColumnWidth(col));
        } else
            return m_model.getColumnWidth(col);
    }
    
    /*
     *  (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#getStyle()
     */
    @Override
    public int getStyle() {
    	return m_style;
    }
    
    /**
     * Overwrites the style bits that determine certain behavior of KTable.
     * Note that not all style bits can be changed after KTable is created.
     * @param style The updated style bits (ORed together). Possibilities:
     * <ul>
     * <li><b>SWTX.FILL_WITH_LASTCOL</b> - Makes the table enlarge the last column to always fill all space.</li>
     * <li><b>SWTX.FILL_WITH_DUMMYCOL</b> - Makes the table fill any remaining space with dummy columns to fill all space.</li>
     * <li><b>SWT.FLAT</b> - Does not paint a dark outer border line.</li>
     * <li><b>SWT.MULTI</b> - Sets the "Multi Selection Mode".
     * In this mode, more than one cell or row can be selected.
     * The user can achieve this by shift-click and ctrl-click.
     * The selected cells/rows can be scattored ofer the complete table.
     * If you pass false, only a single cell or row can be selected.
     * This mode can be combined with the "Row Selection Mode".</li>
     * <li><b>SWT.FULL_SELECTION</b> - Sets the "Full Selection Mode".
     * In the "Full Selection Mode", the table always selects a complete row.
     * Otherwise, each individual cell can be selected.
     * This mode can be combined with the "Multi Selection Mode".</li>
     * <li><b>SWTX.EDIT_ON_KEY</b> - Activates a possibly present cell editor
     * on every keystroke. (Default: only ENTER). However, note that editors
     * can specify which events they accept.</li>
     * <li><b>SWT.HIDE_SELECTION</b> - Hides the selected cells when the KTable
     * looses focus.</li>
     * <li><b>SWTX.MARK_FOCUS_HEADERS</b> - Makes KTable draw left and top header cells
     * in a different style when the focused cell is in their row/column.
     * This mimics the MS Excel behavior that helps find the currently
     * selected cell(s).</li>
     * </ul>
     */
    public void setStyle(int style) {
    	m_style = style;
    }
	/**
	 * Gets the color of the top border.
	 * 
	 * @return The color of the top border.
	 */
	public Color getColorTopBorder() {
		return m_colorTopBorder;
	}

	/**
	 * Gets the color of the left border.
	 * 
	 * @return The color of the left border.
	 */
	public Color getColorLeftBorder() {
		return m_colorLeftBorder;
	}

	/**
	 * Gets the color of the right border.
	 * 
	 * @return The color of the right border.
	 */
	public Color getColorRightBorder() {
		return m_colorRightBorder;
	}

	/**
	 * Sets the color of the top border.
	 * 
	 * @param colorTopBorder
	 *            The color of the top border.
	 */
	public void setColorTopBorder(Color colorTopBorder) {
		m_colorTopBorder = colorTopBorder;
	}

	/**
	 * Sets the color of the left border.
	 * 
	 * @param colorLeftBorder
	 *            The color of the left border.
	 */
	public void setColorLeftBorder(Color colorLeftBorder) {
		m_colorLeftBorder = colorLeftBorder;
	}

	/**
	 * Sets the color of the right border (if one is drawn).
	 * 
	 * @param colorRightBorder
	 *            The color of the right border.
	 */
	public void setColorRightBorder(Color colorRightBorder) {
		m_colorRightBorder = colorRightBorder;
	}

	/**
	 * Sets the cursor provider. If no cursor provider was specified or the specified cursor provider is 
	 * <code>null</code>, the default cursor will be displayed. If a cursor provider was specified, however, it will
	 * be asked to provide a cursor for the current mouse hover position every time the mouse is moved.
	 * 
	 * @param cursorProvider The cursor provider to set.
	 */
	public void setCursorProvider(KTableCursorProvider cursorProvider) {
		m_cursorProvider = cursorProvider;
	}

	/**
	 * @return The number of rows visible in the preferred size.
	 */
	public int getNumRowsVisibleInPreferredSize() {
		return m_numRowsVisibleInPreferredSize;
	}

	/**
	 * @return The number of columns visible in the preferred size.
	 */
	public int getNumColsVisibleInPreferredSize() {
		return m_numColsVisibleInPreferredSize;
	}

	/**
	 * Sets the number of rows that are visible in the preferred size of the KTable (as calculated by 
	 * {@link #computeSize(int, int, boolean)}).
	 * 
	 * @param numRowsVisibleInPreferredSize The number of rows visible in the preferred size.
	 */
	public void setNumRowsVisibleInPreferredSize(int numRowsVisibleInPreferredSize) {
		m_numRowsVisibleInPreferredSize = numRowsVisibleInPreferredSize;
	}
	
	/**
	 * Sets the number of columns that are visible in the preferred size of the KTable (as calculated by 
	 * {@link #computeSize(int, int, boolean)}).
	 * 
	 * @param numColsVisibleInPreferredSize The number of columns visible in the preferred size.
	 */
	public void setNumColsVisibleInPreferredSize(int numColsVisibleInPreferredSize) {
		m_numColsVisibleInPreferredSize = numColsVisibleInPreferredSize;
	}

	/**
	 * @return The row height that is used for calculating the preferred size when a certain row should be visible
	 *         but is currently not filled with data.
	 */
	public int getPreferredSizeDefaultRowHeight() {
		return m_preferredSizeDefaultRowHeight;
	}

	/**
	 * @param preferredSizeDefaultRowHeight
	 *            The row height that is used for calculating the preferred size when a certain row should be
	 *            visible but is currently not filled with data.
	 */
	public void setPreferredSizeDefaultRowHeight(int preferredSizeDefaultRowHeight) {
		m_preferredSizeDefaultRowHeight = preferredSizeDefaultRowHeight;
	}

	/**
	 * Calculates the preferred size of the KTable. The preferred size of a 
	 * KTable is calculated such that there is enough space available to show 
	 * all header columns and rows, as well as a configurable number of data 
	 * columns and rows.
	 * 
	 * @param wHint The width hint (can be <code>SWT.DEFAULT</code>)
	 * @param hHint The height hint (can be <code>SWT.DEFAULT</code>)
	 * @param changed <code>true</code> if the control's contents have changed, 
	 *                and <code>false</code> otherwise
	 * @return the preferred size of the control.
	 * @see #setNumColsVisibleInPreferredSize(int)
	 * @see #setNumRowsVisibleInPreferredSize(int)
	 */
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		// start with margins
		int height = 1;
		int width = 1;

		if (m_model != null) {
			// Determine height of header rows
			for (int i = 0; i < m_model.getFixedHeaderRowCount(); i++) {
				height += m_model.getRowHeight(i);
			}

			// Add height of data rows to display
			int rowsVisible = 0;
			for (int i = m_model.getFixedHeaderRowCount(); i < m_model.getFixedHeaderRowCount()
					+ m_numRowsVisibleInPreferredSize
					&& i < m_model.getRowCount(); i++) {
				height += m_model.getRowHeight(i);
				rowsVisible++;
			}
			
			// Make sure that there is room for m_numRowsVisibleInPreferredSize rows, even if there are not that
			// many data rows currently available
			for (int i = rowsVisible; i < m_numRowsVisibleInPreferredSize; i++) {
				height += m_preferredSizeDefaultRowHeight;
			}

			// Determine width of header columns
			for (int i = 0; i < m_model.getFixedHeaderColumnCount(); i++) {
				width += m_model.getColumnWidth(i);
			}

			// Add width of data columns to display
			for (int i = m_model.getFixedHeaderColumnCount(); i < m_model.getFixedHeaderColumnCount()
					+ m_numColsVisibleInPreferredSize
					&& i < m_model.getColumnCount(); i++) {
				width += m_model.getColumnWidth(m_model.getFixedHeaderColumnCount());
			}
		}

		// Take scrollbars into account
		if (getHorizontalBar() != null) {
			height += getHorizontalBar().getSize().y;
		}
		if (getVerticalBar() != null) {
			width += getVerticalBar().getSize().x;
		}

		// System.out.println("w,h = " + width + ", " + height);
		return new Point(width, height);
	}

}


/**
 * This class stores coordinates of selected cell(s). If column or row is
 * selected, then the other coordinate value is -1.
 *  
 * Point can't be used instead of this class, because it doesn't implement 
 * interface Comparable.
 */ 
class SelectionItem implements Comparable<SelectionItem> {
    
    int m_column;
    int m_row;
    
    SelectionItem(int column, int row) {
        m_column = column;
        m_row = row;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
            
        if (!(other instanceof SelectionItem)) {
            return false;
        }
        
        return compareTo((SelectionItem)other) == 0;
    }

    
    @Override 
    public int hashCode() {
        return (31*17 + m_row) * 31 + m_column;
    }
    
    
    @Override
    public int compareTo(SelectionItem other) {
        
        if (m_row < other.m_row) {
            return -1;
        }
        
        if (m_row > other.m_row) {
            return 1;
        }

        // rows are equal, compare columns
        if (m_column < other.m_column) {
            return -1;
        }
        
        if (m_column > other.m_column) {
            return 1;
        }
        
        return 0;
    }

    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SelectionItem [m_column=");
        builder.append(m_column);
        builder.append(", m_row=");
        builder.append(m_row);
        builder.append("]");
        return builder.toString();
    }
}
