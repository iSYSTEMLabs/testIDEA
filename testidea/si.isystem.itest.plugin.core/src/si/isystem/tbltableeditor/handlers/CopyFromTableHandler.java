package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Point;

import si.isystem.connect.CMapAdapter;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.HeaderNode;
import si.isystem.tbltableeditor.IModelWithComment;
import si.isystem.tbltableeditor.TestBaseListModel;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTable.ESelectionMode;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.renderers.TextIconsContent;

public class CopyFromTableHandler extends TableEditorHandlerBase {

    protected KTable m_table;


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // returned action is useless - cells should not be deleted on copy 
        copySelectionToClipboard(false);
        return null;
    }

    
    /** If icClearCells == true, clear action is returned, null otherwise. */
    protected AbstractAction copySelectionToClipboard(boolean isClearCells) {
        
        m_table = UiUtils.getKTableInFocus();
        if (m_table == null) {
            return null;
        }
        
        ESelectionMode selectionMode = m_table.getSelectionMode();
        Point[] selection = m_table.getCellSelection();
        CMapAdapter mainMap = new CMapAdapter();
        CMapAdapter metaMap = new CMapAdapter();
        mainMap.setValue(TestBaseListModel.TEST_IDEA_TABLE_CLIPBOARD_TAG, metaMap);
        KTableModel kModel = m_table.getModel();
        if (kModel instanceof TestBaseListModel) {
            metaMap.setValue(TestBaseListModel.CLIPBOARD_MODE_TAG, selectionMode.toString());
        } else {
            metaMap.setValue(TestBaseListModel.CLIPBOARD_MODE_TAG, ESelectionMode.ECell.toString());
        }
        CMapAdapter cellsMap = new CMapAdapter();
        metaMap.setValue(TestBaseListModel.CLIPBOARD_DATA_TAG, cellsMap);
        String textForClipboard = null;
        AbstractAction action = null;
        
        switch (selectionMode) {
        case ECell:
            // Disabled if, because then it is not possible to copy-paste comments for cell.
            // Users should press F2 and then Ctrl-C to copy value only.
            /*if (selection.length == 1) {
                // if there is only one cell selected, adding of position data makes no sense,
                // plus content can be then easily pasteed to other text controls or
                // applications - so copy only its text to clipboard. 
                Point pt = selection[0];
                TestBaseListModel kModel = (TestBaseListModel)m_table.getModel();
                TextIconsContent content = (TextIconsContent)kModel.getContentAt(pt.x, 
                                                                                 pt.y);
                textForClipboard = content.getText();
                break;
            } */
              
            action = getSelectedCellsAsClipboardString(selection, cellsMap, isClearCells);
            textForClipboard = mainMap.serialize();
            
            break;
            
        case EColumn:
            action = getSelectedColumnsAsClipboardString(selection, cellsMap, isClearCells);
            textForClipboard = mainMap.serialize();
            break;
        case ERow:
            if (kModel instanceof TestBaseListModel) {
                textForClipboard = getSelectedRowsAsClipboardString(selection);
                action = ((TestBaseListModel)kModel).createRemoveRowsAction(selection);
            } else {
                action = getSelectedRowsAsClipboardString(selection, cellsMap, isClearCells,
                                                          kModel);
                textForClipboard = mainMap.serialize();
            }
            break;
        default:
            throw new SIllegalStateException("Invalid table selection mode!").
                       add("selectionMode", selectionMode);
        }
        
        UiUtils.putYamlToClipboard(textForClipboard);
        
        return action;
    }
    
    
    private AbstractAction getSelectedCellsAsClipboardString(Point[] selection,
                                                             CMapAdapter cellsMap, 
                                                             boolean isClearCells) {
        
        KTableModel kModel = m_table.getModel();
        
        GroupAction groupAction = new GroupAction("Clear table cells");

        for (Point pt : selection) {
            String key = pt.x + "," + pt.y;
            Object contentObj = kModel.getContentAt(pt.x, pt.y);
        
            if (contentObj instanceof TextIconsContent) {
                TextIconsContent content = (TextIconsContent)contentObj;
                cellsMap.setValue(key, content.getText(), 
                                  content.getNlComment(),
                                  content.getEolComment());
            } else {
                cellsMap.setValue(key, contentObj.toString(), 
                                  "",
                                  "");
            }
            
            if (isClearCells) {
                if (kModel instanceof TestBaseListModel) {
                    ArrayTableModel arrayModel = ((TestBaseListModel)kModel).getArrayModel();

                    AbstractAction action = arrayModel.createSetContentAtAction(pt.x - 1, 
                                                                                pt.y - kModel.getFixedHeaderRowCount(), 
                                                                                "", "", "");
                    if (action.isModified()) {
                        groupAction.add(action);
                    }
                } else {
                    AbstractAction action = ((IModelWithComment)kModel).createSetContentAction(pt.x, 
                                                                                               pt.y, 
                                                                                               "", "", "");
                    if (action.isModified()) {
                        groupAction.add(action);
                    }
                }
            }
        }
        
        return groupAction;
    }

    
    private AbstractAction getSelectedColumnsAsClipboardString(Point[] selection,
                                                               CMapAdapter cellsMap, 
                                                               boolean isClearCells) {
        
        KTableModel modelObj = m_table.getModel();
        
        if (modelObj instanceof TestBaseListModel) {
            return getSelectedColumnsAsClipboardString(selection, 
                                                       cellsMap, 
                                                       isClearCells, 
                                                       (TestBaseListModel)modelObj);
        } else {
            return getSelectedColumnsAsClipboardString(selection, 
                                                       cellsMap, 
                                                       isClearCells, 
                                                       modelObj);
        }
   }
    
    
   private AbstractAction getSelectedColumnsAsClipboardString(Point[] selection,
                                                              CMapAdapter cellsMap, 
                                                              boolean isClearCells,
                                                              TestBaseListModel kModel) {
                
        ArrayTableModel arrayModel = kModel.getArrayModel();

        int numHeaderRows = kModel.getFixedHeaderRowCount();
        int numRows = kModel.getDataRowCount();
        GroupAction columnAction = new GroupAction("Copy table columns");
        for (Point pt : selection) {
            CMapAdapter columnMap = new CMapAdapter();
            int modelCol = pt.x - kModel.getFixedHeaderColumnCount();
            
            // search for the first non-empty header cell bottom-up
            HeaderNode header = arrayModel.getHeader();
            HeaderNode colNode = header.getFirstNonEmptyCellBottomUp(modelCol);
            String colHeaderTxt = colNode.getName();
            cellsMap.setValue(colHeaderTxt, columnMap);
            HeaderNode containerNode = colNode;
            if (colNode.isLeafNode()) {
                containerNode = colNode.getParent();
            }

            for (int rowIdx = numHeaderRows; rowIdx < numRows; rowIdx++) {
                TextIconsContent content = (TextIconsContent)kModel.getContentAt(pt.x, 
                                                                                 rowIdx);
                columnMap.setValue(String.valueOf(rowIdx), 
                                   content.getText(),
                                   content.getNlComment(),
                                   content.getEolComment());
                
                // cells, which are struct members may only be cleared, not removed
                if (isClearCells  &&  containerNode.isStructMapping()) {
                    AbstractAction action = arrayModel.createSetContentAtAction(modelCol, 
                                                                                rowIdx - kModel.getFixedHeaderRowCount(), 
                                                                                "", "", "");
                    if (action.isModified()) {
                        columnAction.add(action);
                    }
                }
            }
            
            if (isClearCells  &&  colNode.isLeafNode()) {
                // System.out.println("Remove user column");
                GroupAction action = arrayModel.createRemoveSeqOrUserMappingColumnAction(colNode);
                columnAction.add(action);
            }
        }
        

        return columnAction;
    }


   private AbstractAction getSelectedColumnsAsClipboardString(Point[] selection,
                                                              CMapAdapter cellsMap, 
                                                              boolean isClearCells,
                                                              KTableModel kModel) {
                
        int numHeaderRows = kModel.getFixedHeaderRowCount();
        int numRows = kModel.getRowCount();
        GroupAction columnAction = new GroupAction("Copy table columns as cells");
        for (Point pt : selection) {
            
            for (int rowIdx = numHeaderRows; rowIdx < numRows; rowIdx++) {
                TextIconsContent content = (TextIconsContent)kModel.getContentAt(pt.x, 
                                                                                 rowIdx);
                String key = pt.x + "," + rowIdx;

                cellsMap.setValue(key, 
                                  content.getText(),
                                  content.getNlComment(),
                                  content.getEolComment());
                
                // cells, which are struct members may only be cleared, not removed
                if (isClearCells) {
                    
                    AbstractAction action = 
                            ((IModelWithComment)kModel).createSetContentAction(pt.x, 
                                                                               rowIdx, 
                                                                               "", "" ,"");
                    if (action != null  &&  action.isModified()) {
                        columnAction.add(action);
                    }
                }
            }
        }

        return columnAction;
    }


    private String getSelectedRowsAsClipboardString(Point[] selection) {

        TestBaseListModel kModel = (TestBaseListModel)m_table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        // For rows copy complete CTestBase objects
        int rows[] = new int[selection.length];
        int idx = 0;
        
        for (Point pt : selection) {
            rows[idx++] = pt.y - kModel.getFixedHeaderRowCount(); 
        }
        
        String yamlSpec = TestBaseListModel.TEST_IDEA_TABLE_TEST_BASE_CLIPBOARD_TAG + "\n" + 
                          arrayModel.getRowsAsCTestBaseYAMLString(rows);

        return yamlSpec;
    }
    
    
    private AbstractAction getSelectedRowsAsClipboardString(Point[] selection,
                                                            CMapAdapter cellsMap, 
                                                            boolean isClearCells,
                                                            KTableModel kModel) {

        int numHeaderCols = kModel.getFixedHeaderColumnCount();
        int numCols = kModel.getColumnCount();
        GroupAction rowAction = new GroupAction("Copy table rows as cells");
        for (Point pt : selection) {
            
            for (int colIdx = numHeaderCols; colIdx < numCols; colIdx++) {
                TextIconsContent content = (TextIconsContent)kModel.getContentAt(colIdx,
                                                                                 pt.y);
                String key = colIdx + "," + pt.y;

                cellsMap.setValue(key, 
                                  content.getText(),
                                  content.getNlComment(),
                                  content.getEolComment());
                
                // cells, which are struct members may only be cleared, not removed
                if (isClearCells) {
                    
                    AbstractAction action = 
                            ((IModelWithComment)kModel).createSetContentAction(colIdx,
                                                                               pt.y,
                                                                               "", "", "");
                    if (action != null  &&  action.isModified()) {
                        rowAction.add(action);
                    }
                }
            }
        }

        return rowAction;
    }
}
