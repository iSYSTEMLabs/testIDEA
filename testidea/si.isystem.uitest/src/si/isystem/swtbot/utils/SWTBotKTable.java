package si.isystem.swtbot.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.swt.finder.ReferenceBy;
import org.eclipse.swtbot.swt.finder.SWTBotWidget;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.results.IntResult;
import org.eclipse.swtbot.swt.finder.results.ListResult;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.StringResult;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.utils.StringUtils;
import org.eclipse.swtbot.swt.finder.utils.internal.Assert;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.hamcrest.SelfDescribing;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;


@SWTBotWidget(clasz = Table.class, preferredName = "table", referenceBy = { ReferenceBy.LABEL })
public class SWTBotKTable extends AbstractSWTBot<KTable> {

    /**
     * Constructs a new instance of this object.
     *
     * @param table the widget.
     * @throws WidgetNotFoundException if the widget is <code>null</code> or widget has been disposed.
     */
    public SWTBotKTable(KTable table) throws WidgetNotFoundException {
        this(table, null);
    }

    /**
     * Constructs a new instance of this object.
     *
     * @param table the widget.
     * @param description the description of the widget, this will be reported by {@link #toString()}
     * @throws WidgetNotFoundException if the widget is <code>null</code> or widget has been disposed.
     */
    public SWTBotKTable(KTable table, SelfDescribing description) throws WidgetNotFoundException {
        super(table, description);
    }

    /**
     * Gets the row count.
     *
     * @return the number of rows in the table
     */
    public int getRowCount() {
        return syncExec(new IntResult() {
            public Integer run() {
                return widget.getModel().getRowCount();
            }
        });
    }
     

    public int getHeaderRowCount() {
        return syncExec(new IntResult() {
            public Integer run() {
                return widget.getModel().getFixedHeaderRowCount();
            }
        });
    }
     

    /**
     * Gets the column count.
     *
     * @return the number of columns in the table
     */
    public int getColumnCount() {
        return syncExec(new IntResult() {
            public Integer run() {
                return widget.getModel().getColumnCount();
            }
        });
    }


    public int getHeaderColumnCount() {
        return syncExec(new IntResult() {
            public Integer run() {
                return widget.getModel().getFixedHeaderColumnCount();
            }
        });
    }

    
    public Object getContentAt(final int col, final int row) {
        return syncExec(new Result<Object>() {
            public Object run() {
                return widget.getModel().getContentAt(col, row);
            }
        });
    }

    
    /**
     * Gets the columns in this table.
     *
     * @return the list of columns in the table.
     */
    public List<String> columns() {
        return syncExec(new ListResult<String>() {
            public List<String> run() {
                ArrayList<String> result = new ArrayList<String>();

                KTableModel tableModel = widget.getModel();
                int numHeaderRows = tableModel.getFixedHeaderRowCount();
                int numColumns = getColumnCount();
                for (int column = 0; column < numColumns; column++) {
                    boolean isAdded = false;
                    for (int row = numHeaderRows - 1; row >= 0; row--) {
                        String contents = tableModel.getContentAt(column, row).toString();
                        if (contents != null  &&  !contents.isEmpty()) {
                            result.add(contents);
                            isAdded = true;
                            break;
                        }
                    }
                    
                    if (!isAdded) {
                        // should never happen
                        throw new IllegalStateException("Empty column header detected!");
                    }
                }

                return result;
            }
        });
    }
    

    /**
     * @param column the text on the column.
     * @return the index of the specified column.
     * @since 1.3
     */
    public int indexOfColumn(String column) {
        return columns().indexOf(column);
    }

    /**
     * Gets the column matching the given label.
     *
     * @param label the header text.
     * @return the header of the table.
     * @throws WidgetNotFoundException if the header is not found.
    public SWTBotTableColumn header(final String label) throws WidgetNotFoundException {
        TableColumn column = syncExec(new Result<TableColumn>() {
            public TableColumn run() {
                TableColumn[] columns = widget.getColumns();
                for (TableColumn column : columns) {
                    if (column.getText().equals(label))
                        return column;
                }
                return null;
            }
        });
        return new SWTBotTableColumn(column, widget);
    }
     */
    

    /**
     * Gets the cell data for the given row/column index.
     *
     * @param row the row in the table.
     * @param column the column in the table.
     * @return the cell at the location specified by the row and column
     */
    public String cell(final int column, final int row) {
        assertIsLegalCell(column, row);

        return syncExec(new StringResult() {
            public String run() {
                String item = widget.getModel().getContentAt(column, row).toString();
                return item;
            }
        });
    }

    /**
     * Gets the cell data for the given row and column label.
     *
     * @param row the row in the table
     * @param columnName the column title.
     * @return the cell in the table at the specified row and columnheader
     */
    public String cell(String columnName, int row) {
        Assert.isLegal(columns().contains(columnName), "The column `" + columnName + "' is not found."); //$NON-NLS-1$ //$NON-NLS-2$
        List<String> columns = columns();
        int columnIndex = columns.indexOf(columnName);
        if (columnIndex == -1)
            return ""; //$NON-NLS-1$
        return cell(row, columnIndex);
    }

    /**
     * Gets the selected item count.
     *
     * @return the number of selected items.
     */
    public int selectionCount() {
        return syncExec(new IntResult() {
            public Integer run() {
                // ESelectionMode selectionMode = widget.getSelectionMode();
                Point[] selection = widget.getCellSelection();

                return selection.length;
            }
        });
    }

    /**
     * Gets the selected items.
     *
     * @return the selection in the table
     */
    public Point[] selection() {
        return syncExec(new Result<Point []>() {
            public Point[] run() {
                final Point[] selection = widget.getCellSelection();
                return selection;
            }
        });
    }

    
    private void assertIsLegalRowIndex(final int rowIndex) {
        Assert.isLegal(rowIndex < getRowCount(), "The row number: " + rowIndex + " does not exist in the table"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    
    /**
     * Sets the selection to the given items.
     *
     * @param items the items to select in the table.
     * @since 1.0
    public void select(final String... items) {
        waitForEnabled();
        setFocus();
        int[] itemIndicies = new int[items.length];
        for(int i = 0; i < items.length; i++) {
            itemIndicies[i] = indexOf(items[i]);
            Assert.isLegal(itemIndicies[i] >= 0, "Could not find item:" + items[i] + " in table"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        select(itemIndicies);
        notifySelect();
    }
     */
    

    /**
     * Gets the index of the item matching the given item.
     * 
     * @param item the item in the table.
     * @return the index of the specified item in the table, or -1 if the item does not exist in the table.
     * @since 1.0
    public int indexOf(final String item) {
        return syncExec(new IntResult() {
            public Integer run() {
                TableItem[] items = widget.getItems();
                for (int i = 0; i < items.length; i++) {
                    TableItem tableItem = items[i];
                    if (tableItem.getText().equals(item))
                        return i;
                }
                return -1;
            }
        });
    }
     */

    /**
     * @param item the item in the table.
     * @return <code>true</code> if the table contains the specified item, <code>false</code> otherwise.
    public boolean containsItem(final String item) {
        return indexOf(item) != -1;
    }
     */

    /**
     * Gets the index of the item matching the given item and the given column.
     * 
     * @param item the index of the item in the table, or -1 if the item does not exist in the table.
     * @param column the column for which to get the index of.
     * @return the index of the specified item and of the specified column in the table.
     * @since 1.3
     
    public int indexOf(final String item, final int column) {
        return syncExec(new IntResult() {
            public Integer run() {
                TableItem[] items = widget.getItems();
                for (int i = 0; i < items.length; i++) {
                    TableItem tableItem = items[i];
                    if (tableItem.getText(column).equals(item))
                        return i;
                }
                return -1;
            }
        });
    }
     */

    
    /**
     * Gets the index of the item matching the given item and the given column.
     *
     * @param item the index of the item in the table, or -1 if the item does not exist in the table.
     * @param column the column for which to get the index of.
     * @return the index of the specified item and of the specified column in the table.
     * @since 1.3

    public int indexOf(final String item, final String column) {
        return indexOf(item, indexOfColumn(column));
    }
     */

    /**
     * Removes selection from all cells.
     */
    public void unselect() {
        waitForEnabled();
        setFocus();
        syncExec(new VoidResult() {
            @Override
            public void run() {
                log.debug(MessageFormat.format("Unselecting all in {0}", widget)); //$NON-NLS-1$
                widget.setSelection(new Point[0], false);
            }
        });
        notifySelect();
    }

    /**
     * Selects the given index items.
     *
     * @param rowIndices the row indices to select in the table.
     */
    public void selectRows(final int... rowIndices) {
        waitForEnabled();
        if (rowIndices.length > 1) {
            assertMultiSelect();
        }
        setFocus();
        log.debug(MessageFormat.format("Selecting rows {0} in table {1}", StringUtils.join(rowIndices, ", "), this)); //$NON-NLS-1$ //$NON-NLS-2$
        unselect();
        Point [] selection = new Point[rowIndices.length];
        for (int i = 0; i < rowIndices.length; i++) {
            selection[i] = new Point(i, -1);
        }
        widget.setSelection(selection, true);
    }


    /**
     * Selects cells in the given rectangle.
     * 
     * @param leftCol left column, inclusive
     * @param topRow top row, inclusive
     * @param rightCol right column, not included
     * @param bottomRow bottom row, not included
     */
    public void selectCells(int leftCol, int topRow, int rightCol, int bottomRow) {
        waitForEnabled();
        assertMultiSelect();
        setFocus();
        unselect();
        
        int numCols = rightCol - leftCol;
        int numRows = bottomRow - topRow;
        
        final Point [] selection = new Point[numCols * numRows];
        
        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
                selection[colIdx * numRows + rowIdx] = new Point(leftCol + colIdx, 
                                                                 topRow + rowIdx);
            }
        }
        
        syncExec(new VoidResult() {
            @Override
            public void run() {
                widget.setSelection(selection, true);
            }
        });
        notifySelect();
    }

    /**
     * Does not clear previous selections.
    private void additionalSelectAndNotify(final int j) {
        assertIsLegalRowIndex(j);
        syncExec(new VoidResult() {
            public void run() {
                Point[] oldSelection = widget.getCellSelection();
                Point [] newSelection = new Point[oldSelection.length + 1];
                newSelection[oldSelection.length] = new Point(j, -1);
                widget.setSelection(newSelection, true);
            }
        });
        notifySelect();
    }
     */

    private void assertMultiSelect() {
        Assert.isLegal(hasStyle(widget, SWT.MULTI), "Table does not support multi selection."); //$NON-NLS-1$
    }

    /**
     * Notifies the selection.
     */
    protected void notifySelect() {
        notify(SWT.MouseEnter);
        notify(SWT.MouseMove);
        notify(SWT.Activate);
        notify(SWT.FocusIn);
        notify(SWT.MouseDown);
        notify(SWT.Selection, selectionEvent());
        notify(SWT.MouseUp);
        notify(SWT.MouseHover);
        notify(SWT.MouseMove);
        notify(SWT.MouseExit);
        notify(SWT.Deactivate);
        notify(SWT.FocusOut);
    }

    
    private Event selectionEvent() {
        Event createEvent = createEvent();
        createEvent.item = widget;
        return createEvent;
    }

    /**
     * Click on the table on given cell. This can be used to activate a cellEditor on a cell.
     *
     * @param row the row in the table.
     * @param column the column in the table.
     * @since 1.2
     */
    public void click(final int column, final int row) {
        assertIsLegalCell(column, row);
        setFocus();
        // select(row);
        syncExec(new VoidResult() {
            public void run() {
                Rectangle cellBounds = widget.getCellRect(column, row);
                clickXY(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
            }
        });
    }

    
    public void clickDecoration(final int column, final int row, final TextIconsContent.EIconPos iconPos) {
        assertIsLegalCell(column, row);
        setFocus();
        // should be asyncExec, otherwise when dialog opens syncExec() waits until
        // the dialog gets closed ==> deadlock
        asyncExec(new VoidResult() {
            public void run() {
                Rectangle cellBounds = widget.getCellRect(column, row);
                switch (iconPos) {
                case ETopLeft:
                    clickXY(cellBounds.x + TextIconsCellRenderer.DIST_TO_BORDER + 4, 
                            cellBounds.y + TextIconsCellRenderer.DIST_TO_BORDER + 4);
                    break;
                case EBottomLeft:
                    clickXY(cellBounds.x + TextIconsCellRenderer.DIST_TO_BORDER + 2, 
                            cellBounds.y + cellBounds.height - TextIconsCellRenderer.DIST_TO_BORDER - 2);
                    break;
                case EMiddleMiddle:
                    clickXY(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
                    break;
                case ETopRight:
                    clickXY(cellBounds.x + cellBounds.width - TextIconsCellRenderer.RESIZE_COLUMN_AREA - 2, 
                            cellBounds.y + TextIconsCellRenderer.DIST_TO_BORDER + 2);
                    break;
                case EBottomRight:
                    clickXY(cellBounds.x + cellBounds.width - TextIconsCellRenderer.RESIZE_COLUMN_AREA - 2, 
                            cellBounds.y + cellBounds.height - TextIconsCellRenderer.DIST_TO_BORDER - 2);
                default:
                    break;
                }
            }
        });
    }

    
    /**
     * Click on the table on given cell. This can be used to activate a cellEditor on a cell.
     *
     * @param row the row in the table.
     * @param column the column in the table.
     * @since 1.2
     */
    public void doubleClick(final int column, final int row) {
        assertIsLegalCell(column, row);
        setFocus();
        syncExec(new VoidResult() {
            public void run() {
                Rectangle cellBounds = widget.getCellRect(column, row);
                // for some reason, it does not work without setting selection first
                // widget.setSelection(row);
                doubleClickXY(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
            }
        });
    }


    /**
     * Does not work for empty (null) cells. Use KTableTestUtils.setCell() instead.
     * 
     * @param column
     * @param row
     * @param value
    public void setContentAt(final int column, final int row, final String value) {
        assertIsLegalCell(column, row);
        setFocus();
        syncExec(new VoidResult() {
            public void run() {
                KTableModel model = widget.getModel();
                model.setContentAt(column, row, value);
                
                //Rectangle cellBounds = widget.getCellRect(column, row);
            }
        });
    }
     */

/* Does not work, because context menu is shown by viewPart based on current selection,
 * not the table itself. See  KTableTestUtils.deleteColumn() for an example.     
 
    public SWTBotMenu contextMenu(final int column, final int row, final String menuText) {

        waitForEnabled();
        
        final ArrayList<Rectangle> boundsList = new ArrayList<Rectangle>();
        
        syncExec(new VoidResult() {
            public void run() {
        
                Rectangle cellBounds = widget.getCellRect(column, row);
                // Rectangle tableBounds = widget.getShell().getBounds();
                // mouse coordinates should be absolute to the display
                //cellBounds.x += tableBounds.x;
                //cellBounds.y += tableBounds.y;
                boundsList.add(cellBounds);
            }});
        
        Rectangle cellBounds = boundsList.get(0);
        
        notify(SWT.MouseDown, createMouseEvent(cellBounds.x + cellBounds.width / 2,
                                               cellBounds.y + cellBounds.height / 2, 
                                               3, 0, 1), widget);

        notify(SWT.MouseUp, createMouseEvent(cellBounds.x + cellBounds.width / 2,
                                             cellBounds.y + cellBounds.height / 2,
                                             3, 0, 1), widget);

        notify(SWT.MenuDetect);
        
        
        return super.contextMenu(widget, menuText);
    }
 */   

    
    public void selectCell(final int column, final int row) {

        waitForEnabled();
        
        final ArrayList<Rectangle> boundsList = new ArrayList<Rectangle>();
        
        syncExec(new VoidResult() {
            public void run() {
        
                Rectangle cellBounds = widget.getCellRect(column, row);
                boundsList.add(cellBounds);
            }});
        
        Rectangle cellBounds = boundsList.get(0);

        int button = 1;  // 1 = left mouse button, 3 = right mouse button
        notify(SWT.MouseDown, createMouseEvent(cellBounds.x + cellBounds.width / 2,
                                               cellBounds.y + cellBounds.height / 2, 
                                               button, 0, 1), widget);

        notify(SWT.MouseUp, createMouseEvent(cellBounds.x + cellBounds.width / 2,
                                             cellBounds.y + cellBounds.height / 2,
                                             button, 0, 1), widget);
    }
    
    
    /**
     * Asserts that the row and column are legal for this instance of the table.
     *
     * @param row the row number
     * @param column the column number
     * @since 1.2
     */
    protected void assertIsLegalCell(final int column, final int row) {
        int rowCount = getRowCount();
        int columnCount = widget.getModel().getColumnCount(); // 0 if no TableColumn has been created by user

        Assert.isLegal(row < rowCount, "The row number (" + row + ") is more than the number of rows(" + rowCount + ") in the table."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        Assert.isLegal((column < columnCount) || ((columnCount == 0) && (column == 0)), "The column number (" + column //$NON-NLS-1$
                + ") is more than the number of column(" + columnCount + ") in the table."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Gets the table item matching the given name.
     *
     * @param itemText the text on the node.
     * @return the table item with the specified text.
     * @throws WidgetNotFoundException if the node was not found.
     * @since 1.3
    public SWTBotTableItem getTableItem(final String itemText) throws WidgetNotFoundException {
        try {
            new SWTBot().waitUntil(new DefaultCondition() {
                public String getFailureMessage() {
                    return "Could not find node with text " + itemText; //$NON-NLS-1$
                }

                public boolean test() throws Exception {
                    return getItem(itemText) != null;
                }
            });
        } catch (TimeoutException e) {
            throw new WidgetNotFoundException("Timed out waiting for table item " + itemText, e); //$NON-NLS-1$
        }
        return new SWTBotTableItem(getItem(itemText));
    }
     */

    
    /**
     * Gets the item matching the given name.
     *
     * @param itemText the text on the node.
     * @return the table item with the specified text.
    private TableItem getItem(final String itemText) {
        return syncExec(new WidgetResult<TableItem>() {
            public TableItem run() {
                TableItem[] items = widget.getItems();
                for (int i = 0; i < items.length; i++) {
                    TableItem item = items[i];
                    if (item.getText().equals(itemText))
                        return item;
                }
                return null;
            }
        });
    }
     */

    
    /**
     * Gets the table item matching the given row number.
     *
     * @param row the row number.
     * @return the table item with the specified row.
     * @throws WidgetNotFoundException if the node was not found.
     * @since 2.0
    public SWTBotTableItem getTableItem(final int row) throws WidgetNotFoundException {
        try {
            new SWTBot().waitUntil(new DefaultCondition() {
                public String getFailureMessage() {
                    return "Could not find table item for row " + row; //$NON-NLS-1$
                }

                public boolean test() throws Exception {
                    return getItem(row) != null;
                }
            });
        } catch (TimeoutException e) {
            throw new WidgetNotFoundException("Timed out waiting for table item in row " + row, e); //$NON-NLS-1$
        }
        return new SWTBotTableItem(getItem(row));
    }
     */

    /**
     * Gets the item matching the given row number.
     *
     * @param row the row number.
     * @return the table item with the specified row.
    private TableItem getItem(final int row) {
        return syncExec(new WidgetResult<TableItem>() {
            public TableItem run() {
                return widget.getItem(row);
            }

        });
    }
     */

    
}
