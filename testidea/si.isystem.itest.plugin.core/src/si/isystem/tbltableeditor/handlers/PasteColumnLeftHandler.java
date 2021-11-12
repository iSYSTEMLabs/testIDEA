package si.isystem.tbltableeditor.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.ColumnSelection;
import si.isystem.tbltableeditor.EmptyColumnSelection;
import si.isystem.tbltableeditor.HeaderNode;
import si.isystem.tbltableeditor.TableEditorSelection;
import si.isystem.tbltableeditor.TestBaseListModel;
import si.isystem.tbltableeditor.TestBaseTableSelectionProvider;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTable.ESelectionMode;

/** Handler for context menu. */
public class PasteColumnLeftHandler extends TableEditorHandlerBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        pasteInsertColumns(false);
        return null;
    }

    
    protected void pasteInsertColumns(boolean isInsertRight) {
        
        KTable table = UiUtils.getKTableInFocus();
        
        if (table == null) {
            return;
        }
        
        TestBaseListModel kModel = (TestBaseListModel)table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();

        TestBaseTableSelectionProvider selectionProvider = kModel.getSelectionProvider();
        
        if (table.getCellSelection().length == 0) {
            throw new SIllegalStateException("Please select a starting cell, " +
                                             "to paste contents from clipboard to.");
        }

        String yamlSpec = UiUtils.getYamlFromClipboard();

        if (!yamlSpec.startsWith(TestBaseListModel.TEST_IDEA_TABLE_CLIPBOARD_TAG + ":")) {
            throw new SIllegalStateException("Clipboard does not contain table column data!");
        }
        
        CMapAdapter map = new CMapAdapter();
        map.parse(yamlSpec);

        CMapAdapter metaMap = map.getMapAdapter(TestBaseListModel.TEST_IDEA_TABLE_CLIPBOARD_TAG);
        String mode = metaMap.getValue(TestBaseListModel.CLIPBOARD_MODE_TAG);
        CMapAdapter dataMap = metaMap.getMapAdapter(TestBaseListModel.CLIPBOARD_DATA_TAG);
        StrVector keys = new StrVector();
        dataMap.getKeys(keys);

        ESelectionMode eMode = ESelectionMode.valueOf(mode);

        if (eMode != ESelectionMode.EColumn) {
            MessageDialog.openError(Activator.getShell(), 
                                    "Paste to table", 
                                    "Clipboard does not contain column table data!");
            return;
        }

        HeaderNode clickedNode;
        HeaderNode userSeqOrMappingParent;
        ISelection selection = selectionProvider.getSelection();

        if (selection instanceof ColumnSelection  ||  selection instanceof EmptyColumnSelection) {
            TableEditorSelection tblEditorSelection = (TableEditorSelection)selection;
            userSeqOrMappingParent = tblEditorSelection.getUserSeqOrMappingParent();
            clickedNode = tblEditorSelection.getClickedNode();

        } else {
            return;
        }
        
        if (!userSeqOrMappingParent.isUserMappingNode()  &&  
                !userSeqOrMappingParent.isUserSequenceNode()) {
            MessageDialog.openWarning(Activator.getShell(), "Paste", 
                    "Can't paste insert to fixed columns!");
            return;
        }

        GroupAction groupAction = new GroupAction("Paste insert columns");

        int numPastedColumns = (int)keys.size();

        // adjust the number of rows (CTestBase objects) in model
        if (numPastedColumns > 0) {
            // all columns have all row cells on clipboard, even empty cells,
            // so the number of rows is the same for all columns
            int numInsertedRows = (int)dataMap.getMapAdapter(keys.get(0)).size();
            PasteToTableHandler.addMissingRows(null, 0, numInsertedRows, true, 
                                               arrayModel, table);
        }

        for (int pastedColIdx = 0; pastedColIdx < numPastedColumns; pastedColIdx++) {
            String columnName = keys.get(pastedColIdx);
            CMapAdapter rowsInCol = dataMap.getMapAdapter(columnName);

            StrVector rowKeys = new StrVector();
            rowsInCol.getKeys(rowKeys);
            int numPastedRows = (int)rowKeys.size();
            Map<String, Boolean> responses = new TreeMap<>();

            for (int rowIdx = 0; rowIdx < numPastedRows; rowIdx++) {

                String rowKey = rowKeys.get(rowIdx);
                String strValue = rowsInCol.getValue(rowKey);
                String nlComment = rowsInCol.getComment(CommentType.NEW_LINE_COMMENT, rowKey);
                String eolComment = rowsInCol.getComment(CommentType.END_OF_LINE_COMMENT, rowKey);

                if (userSeqOrMappingParent.isUserSequenceNode()) {
                    int seqIdx = 0;
                    if (clickedNode != null) {
                        seqIdx = userSeqOrMappingParent.getChildIndex(clickedNode.getName());
                        if (isInsertRight) {
                            seqIdx++;
                        }
                    }
                    groupAction.add(arrayModel.createAddSeqItemAction(userSeqOrMappingParent, 
                                                                      rowIdx,
                                                                      seqIdx,
                                                                      strValue,
                                                                      nlComment,
                                                                      eolComment));
                } else { // it is user mapping node
                    List<String> predecessors = new ArrayList<String>();
                    if (clickedNode != null) {
                        predecessors = userSeqOrMappingParent.getPredecessors(clickedNode.getName());

                        if (isInsertRight) {
                            predecessors.add(clickedNode.getName());
                        }
                    }

                    InsertToUserMappingAction action = 
                            arrayModel.createAddUserMappingItemAction(userSeqOrMappingParent, 
                                                                      rowIdx,
                                                                      predecessors,
                                                                      columnName,
                                                                      strValue,
                                                                      nlComment,
                                                                      eolComment);

                    if (responses.containsKey(columnName)) {
                        if (responses.get(columnName)) {
                            groupAction.add(action);
                        }
                    } else {
                        if (action.isNewItemAlreadyExist()) {
                            boolean answer = MessageDialog.openQuestion(Activator.getShell(), 
                                          "Item already exists", 
                                          "Column '" + columnName + 
                                          "' already exists. Would you like to overwrite it?");
                            responses.put(columnName, answer);
                            if (answer) {
                                groupAction.add(action);
                            }

                        } else {
                            groupAction.add(action);
                        }
                    }
                }
            }                
        }

        execActionAndRefresh(groupAction, table);
    }
}
