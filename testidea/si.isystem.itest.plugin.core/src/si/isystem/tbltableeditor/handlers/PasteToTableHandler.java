package si.isystem.tbltableeditor.handlers;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Rectangle;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.IModelWithComment;
import si.isystem.tbltableeditor.TestBaseListModel;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTable.ESelectionMode;
import de.kupzog.ktable.KTableModel;

public class PasteToTableHandler extends TableEditorHandlerBase {

    protected KTable m_table;
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            pasteReplace();
        } catch (Exception ex) {
            // if shell is not null, but Activator.getShell(), then table context menus no 
            // longer show after this error dialog opened
            SExceptionDialog.open(null, "Paste failed!", ex);
        }
        return null;
    }
    
    
    private void pasteReplace() {

        m_table = UiUtils.getKTableInFocus();
        if (m_table == null) {
            return;
        }
        
        if (m_table.getCellSelection().length == 0) {
            throw new SIllegalStateException("Please select a starting cell, " +
                                             "to paste contents from clipboard to.");
        }

        Rectangle selRect = m_table.getSelectedRect();
        
        String yamlSpec = UiUtils.getYamlFromClipboard();

        if (yamlSpec == null) {
            return;
        }
        
        if (yamlSpec.startsWith(TestBaseListModel.TEST_IDEA_TABLE_CLIPBOARD_TAG + ":")) {
            CMapAdapter map = new CMapAdapter();
            map.parse(yamlSpec);
            
            KTableModel kModel = m_table.getModel();
            
            int numColumnsInModel = kModel.getColumnCount() - kModel.getFixedHeaderColumnCount();
            CMapAdapter metaMap = map.getMapAdapter(TestBaseListModel.TEST_IDEA_TABLE_CLIPBOARD_TAG);
            String mode = metaMap.getValue(TestBaseListModel.CLIPBOARD_MODE_TAG);
            CMapAdapter dataMap = metaMap.getMapAdapter(TestBaseListModel.CLIPBOARD_DATA_TAG);
            StrVector keys = new StrVector();
            dataMap.getKeys(keys);
        
            ESelectionMode eMode = ESelectionMode.valueOf(mode);
            GroupAction groupAction = null;
            
            switch (eMode) {
            case ECell:
                if (kModel instanceof TestBaseListModel) {
                    groupAction = pasteReplaceCells(selRect, dataMap, keys,
                                                    numColumnsInModel);
                } else {
                    groupAction = pasteReplaceRawCells(selRect, dataMap, keys,
                                                       numColumnsInModel);
                }
                execActionAndRedraw(groupAction, m_table);
            break;
            case EColumn: 
                groupAction = pasteReplaceColumns(selRect, dataMap, keys,
                                                  numColumnsInModel);
                execActionAndRefresh(groupAction, m_table);
            break;
            case ERow: 
                // insert cells as raw rows - not used at the moment, since
                // this pasting has a problem when user mappings or user sequences
                // length do not match existing ones - it is easier to copy-paste
                // complete CTestBase object in such case, see below
            break;
            default:
                throw new SIllegalStateException("Selection mode not supported for paste!")
                                                  .add("selectionMode", eMode);
            }
        } else if (yamlSpec.startsWith(TestBaseListModel.TEST_IDEA_TABLE_TEST_BASE_CLIPBOARD_TAG)) {
            pasteReplaceRows(selRect, yamlSpec);
        } else {
            pasteReplaceCellsWithRawText(selRect, yamlSpec);
        }
    }


    private GroupAction pasteReplaceCells(Rectangle selRect,
                                          CMapAdapter dataMap,
                                          StrVector keys,
                                          int numColumnsInModel) {
        
        if (m_table.getSelectionMode() != ESelectionMode.ECell) {
            throw new SIllegalStateException("Please select a cell, not column or row to paste contents from clipboard.");
        }
        
        // cells on clipboard are sorted on rows and columns and then 
        // pasted with empty cells between removed - cell coords on 
        // clipboard determine their relative position, not absolute, for
        // example if cells (4, 5) and (7, 5) are pasted to cell
        // (3, 2), cell (7,5) from clipboard will be pasted to cell (4, 2)
        Map<Integer, Map<Integer, String[]>> newCells = new TreeMap<>();
        
        int numPastedCells = (int)keys.size();
        for (int i = 0; i < numPastedCells; i++) {
            String cellCoord = keys.get(i);
            String value = dataMap.getValue(cellCoord);
            String nlComment = dataMap.getComment(CommentType.NEW_LINE_COMMENT, cellCoord);
            String eolComment = dataMap.getComment(CommentType.END_OF_LINE_COMMENT, cellCoord);
            
            String[] coords = cellCoord.split(",");
            if (coords.length != 2) {
                throw new SIllegalStateException("Cell coordinate on " +
                                                 "clipboard should have exactly 2 values!").
                                                 add("coord", cellCoord);
            }
            int col = Integer.parseInt(coords[0]);
            int row = Integer.parseInt(coords[1]);
            
            if (!newCells.containsKey(row)) {
                newCells.put(row, new TreeMap<Integer, String[]>());
            }
            
            newCells.get(row).put(col, new String[]{value, nlComment, eolComment});
        }
        
        GroupAction groupAction = new GroupAction("Paste cells");
        TestBaseListModel kModel = (TestBaseListModel)m_table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();

        int insertionRow = selRect.y - kModel.getFixedHeaderRowCount();
        addMissingRows(null, insertionRow, newCells.size(), true, arrayModel, m_table);
        
        // now add action for setting cells
        for (Map<Integer, String[]> rowCells : newCells.values()) {
            int columnIdx = selRect.x - kModel.getFixedHeaderColumnCount();
            for (String[] valueAndComment : rowCells.values()) {
                // non-editable columns with empty data (empty sequences and mappings)
                // are skipped
                while (numColumnsInModel > columnIdx  &&  
                       !arrayModel.isCellEditable(columnIdx, insertionRow)) {
                    columnIdx++;
                }
                
                if (numColumnsInModel > columnIdx) {
                    AbstractAction action = arrayModel.createSetContentAtAction(columnIdx++, 
                                                                                insertionRow, 
                                                                                valueAndComment[0],
                                                                                valueAndComment[1],
                                                                                valueAndComment[2]);
                    groupAction.add(action);
                } else {
                    break; // we've reached the and of table, 
                           // ignore the remaining pasted cells for that row
                }
            }
            insertionRow++;
        }
        return groupAction;
    }
    
    
    /**
     * This method replaces cells in a general KTable, which does not contain
     * TestBaseListModel, but KTableModel which implements IModelWithcomment.
     *  
     * @param selRect
     * @param dataMap
     * @param keys
     * @param numColumnsInModel
     * @return
     */
    private GroupAction pasteReplaceRawCells(Rectangle selRect,
                                             CMapAdapter dataMap,
                                             StrVector keys,
                                             int numColumnsInModel) {
        
        int numPastedCells = (int)keys.size();
        if (numPastedCells == 0) {
            return new GroupAction("No cells pasted");
        }
        
        int destCol, destRow;
        KTableModel kModel = m_table.getModel();
       
        switch (m_table.getSelectionMode()) {
        case ECell:
            destCol = selRect.x;
            destRow = selRect.y;
            destCol = Math.max(destCol, kModel.getFixedHeaderColumnCount());
            destRow = Math.max(destRow, kModel.getFixedHeaderRowCount());
            break;
        case EColumn:
            destCol = selRect.x;
            destRow = kModel.getFixedHeaderRowCount();
            break;
        case ERow:
            destCol = kModel.getFixedHeaderColumnCount();
            destRow = selRect.y;
            break;
        default:
            throw new SIllegalStateException("Unknown selection mode in table!").
            add("selectionMode", m_table.getSelectionMode());
        }
        
        // cells on clipboard are sorted on rows and columns and then 
        // pasted with empty cells between removed - cell coords on 
        // clipboard determine their relative position, not absolute, for
        // example if cells (4, 5) and (7, 5) are pasted to cell
        // (3, 2), cell (7,5) from clipboard will be pasted to cell (4, 2)
        
        GroupAction groupAction = new GroupAction("Paste cells");
        MutableInt srcCol = new MutableInt(); 
        MutableInt srcRow = new MutableInt();
        parseCoord(keys.get(0), srcCol, srcRow);
        int offsetCol = srcCol.intValue() - destCol;
        int offsetRow = srcRow.intValue() - destRow;
        
        for (int i = 0; i < numPastedCells; i++) {
            String cellCoord = keys.get(i);
            String value = dataMap.getValue(cellCoord);
            String nlComment = dataMap.getComment(CommentType.NEW_LINE_COMMENT, cellCoord);
            String eolComment = dataMap.getComment(CommentType.END_OF_LINE_COMMENT, cellCoord);

            MutableInt clipboardCol = new MutableInt(); 
            MutableInt clipboardRow = new MutableInt();
            parseCoord(cellCoord, clipboardCol, clipboardRow);

            int col = clipboardCol.intValue() - offsetCol;
            int row = clipboardRow.intValue() - offsetRow;

            // allow rows beyond current table size - they will be added
            if (col < kModel.getColumnCount()) {
                AbstractAction action = 
                        ((IModelWithComment)kModel).createSetContentAction(col, 
                                                                           row, 
                                                                           value,
                                                                           nlComment,
                                                                           eolComment);
                groupAction.add(action);
            }
        }
        
        groupAction.addAllFireEventTypes();

        return groupAction;
    }
    
    
    private void parseCoord(String cellCoord,
                            MutableInt srcCol,
                            MutableInt srcRow) {
        
        String[] coords = cellCoord.split(",");
        if (coords.length != 2) {
            throw new SIllegalStateException("Cell coordinate on " +
                    "clipboard should have exactly 2 values!").
                    add("coord", cellCoord);
        }
        
        srcCol.setValue(Integer.parseInt(coords[0]));
        srcRow.setValue(Integer.parseInt(coords[1]));
    }


    private GroupAction pasteReplaceColumns(Rectangle selRect,
                                            CMapAdapter dataMap,
                                            StrVector keys,
                                            int numColumnsInModel) {
        GroupAction groupAction;
        if (m_table.getSelectionMode() != ESelectionMode.EColumn) {
            throw new SIllegalStateException("Clipboard contains data for table columns.\n" + 
              "Please select a column, not a cell or row to paste contents from clipboard to.");
        }

        TestBaseListModel kModel = (TestBaseListModel)m_table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        groupAction = new GroupAction("Paste columns");
        int insertionCol = selRect.x - kModel.getFixedHeaderColumnCount();
        int insertionRow = 0; // should always be 0 for column insertion 
        int numPastedColumns = (int)keys.size();
        
        // adjust the number of rows (CTestBase objects) in model
        if (numPastedColumns > 0) {
            // all columns have all row cells on clipboard, even empty cells,
            // so the number of rows is the same for all columns
            int numInsertedRows = (int)dataMap.getMapAdapter(keys.get(0)).size();
            addMissingRows(null, insertionRow, numInsertedRows, true, arrayModel, m_table);
        }
        
        for (int pastedColIdx = 0; pastedColIdx < numPastedColumns; pastedColIdx++) {
            String columnName = keys.get(pastedColIdx);
            CMapAdapter rowsInCol = dataMap.getMapAdapter(columnName);
            
            StrVector rowKeys = new StrVector();
            rowsInCol.getKeys(rowKeys);
            int numPastedRows = (int)rowKeys.size();
            
            insertionRow = 0;
            boolean isPasted = false;
            for (int j = 0; j < numPastedRows; j++) {
                String cellRow = rowKeys.get(j);
                String value = rowsInCol.getValue(cellRow);
                String nlComment = rowsInCol.getComment(CommentType.NEW_LINE_COMMENT, cellRow);
                String eolComment = rowsInCol.getComment(CommentType.END_OF_LINE_COMMENT, cellRow);

                // skips non-editable columns - user seq or mapping without content
                while (insertionRow < arrayModel.getRowCount()  &&
                       !arrayModel.isCellEditable(insertionCol, insertionRow)) {
                    insertionRow++;
                }
                
                if (insertionRow < arrayModel.getRowCount()) {
                    AbstractAction action = 
                            arrayModel.createSetContentAtAction(insertionCol, 
                                                                  insertionRow++, 
                                                                  value, 
                                                                  nlComment, 
                                                                  eolComment);
                    groupAction.add(action);
                    isPasted = true; // at least one element from the column has been pasted
                } else {
                    break; // ignore rows that fall beyond rows in model
                }
                
            }

            if (!isPasted) {
                pastedColIdx--;
            }
            
            insertionCol++;
            
            if (insertionCol >= numColumnsInModel) {
                // ignore columns that fall beyond columns in model 
                break;
            }
        }
        return groupAction;
    }


    private void pasteReplaceRows(Rectangle selRect, String yamlSpec) {
        
        if (m_table.getSelectionMode() != ESelectionMode.ERow) {
            throw new SIllegalStateException("Please select a row, not column or cell to paste contents from clipboard to.");
        }

        TestBaseListModel kModel = (TestBaseListModel)m_table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        yamlSpec = yamlSpec.substring(TestBaseListModel.TEST_IDEA_TABLE_TEST_BASE_CLIPBOARD_TAG.length() + 1);
        
        GroupAction groupAction = kModel.createRemoveRowsAction(m_table.getCellSelection());
        
        GroupAction action = arrayModel.createInsertParsedListAction(yamlSpec, 
                                                                     selRect.y - kModel.getFixedHeaderRowCount());
        groupAction.add(action);
        
        List<ModelChangedEvent> events = action.getEvents();
        for (ModelChangedEvent event : events) {
            groupAction.addEvent(event);
        }
        groupAction.addFireEventType(EFireEvent.UNDO);
        groupAction.addFireEventType(EFireEvent.REDO);
        
        execActionAndRefresh(groupAction, m_table);
    }

    
    private void pasteReplaceCellsWithRawText(Rectangle selRect, String yamlSpec) {
        // there is only unformatted string on clipboard, maybe even 
        // from other application - paste it to the selected cell.
        // If comment exists for the cell, it is cleared - users have 
        // go to edit mode to modify cell content only
        if (m_table.getSelectionMode() != ESelectionMode.ECell) {
            throw new SIllegalStateException("There is unformatted data on the clopboard.\n" +
                "Please select a cell, not column or row to paste contents from clipboard to.");
        }
        
        String value = yamlSpec;
        // use string only up to the first newline
        int nlIdx = yamlSpec.indexOf('\n');
        if (nlIdx >= 0) {
            value = value.substring(0, nlIdx);
        }
        
        KTableModel kModel = m_table.getModel();
        
        AbstractAction action = ((IModelWithComment)kModel).createSetContentAction(selRect.x, 
                                                                                   selRect.y, 
                                                                                   value,
                                                                                   "",
                                                                                   "");
        if (action != null) {
            action.addFireEventType(EFireEvent.EXEC);
            execAction(action, m_table);
        }
    }
    
    
    /**
     * This method adds rows immediately, since the table model must be modified
     * before set actions are applied (CTestBase objects created).
     * 
     * @param groupAction group action with possible actions to execute before 
     *                    adding, for example removing existing rows 
     * @param insertionRow row where paste will start - used also for calculation
     *                     of rows to append so this value is important also 
     *                     if isAppendAtEnd == false
     * @param numPastedRows
     * @param isAppendAtEnd if true, rows are appended at end, else at insertion row
     * @param arrayModel 
     */
    static void addMissingRows(GroupAction groupAction, int insertionRow, 
                               int numPastedRows, boolean isAppendAtEnd, 
                               ArrayTableModel arrayModel,
                               KTable table) {
        
        groupAction = createAddMissingRowsAction(groupAction,
                                                 insertionRow,
                                                 numPastedRows,
                                                 isAppendAtEnd,
                                                 arrayModel);
        
        if (!groupAction.isEmpty()) {
            execAction(groupAction, table);
            // refresh model only - table will get redrawn on subsequent pending 
            // actions such as paste.
            arrayModel.refresh();
        }
    }


    // do not call this method directly - see comment to addMissingRows() above!
    private static GroupAction createAddMissingRowsAction(GroupAction groupAction,
                                                  int insertionRow,
                                                  int numPastedRows,
                                                  boolean isAppendAtEnd,
                                                  ArrayTableModel arrayModel) {
        if (groupAction == null) {
            groupAction = new GroupAction("Add missing rows for paste");
        }
        
        // if there are more rows pasted than there are rows (CTestBase objects)
        // in model, create new ones in model
        int numExistingRows = arrayModel.getRowCount();
         
        for (int deltaRows = insertionRow + numPastedRows - numExistingRows; 
                 deltaRows > 0; deltaRows--) {
            
            if (isAppendAtEnd) {
                groupAction.add(arrayModel.createAddRowAction(numExistingRows));
            } else {
                groupAction.add(arrayModel.createAddRowAction(insertionRow));
            }
        }
        
        return groupAction;
    }
    
    
    /*static GroupAction createAddMissingRowsAction(GroupAction groupAction, int insertionRow, 
                               int numPastedRows, boolean isAppendAtEnd, 
                               ArrayTableModel arrayModel) {
        
    }*/
}
