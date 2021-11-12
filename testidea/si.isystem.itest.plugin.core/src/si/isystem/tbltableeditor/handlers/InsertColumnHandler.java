package si.isystem.tbltableeditor.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;

import de.kupzog.ktable.KTable;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.SimpleEntryDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.HeaderNode;
import si.isystem.tbltableeditor.TableEditorSelection;
import si.isystem.tbltableeditor.TestBaseListModel;
import si.isystem.tbltableeditor.TestBaseTableSelectionProvider;


public class InsertColumnHandler  extends TableEditorHandlerBase {

    public static final String LBL_IDENTIFIER = "Identifier:";
    public static final String ENTER_IDENTIFIER_NAME = "Enter identifier name";
    private static final boolean IS_USE_EMPTY_MAPPING_VALUES = true;


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        KTable table = getKTable();
        insertColumn(table, false);
        return null;
    }

    
    public KTable getKTable() {
    
        return UiUtils.getKTableInFocus();
    }

    
    public int insertColumn() {
        return insertColumn(getKTable(), null);
    }

    
    /**
     * Returns the number of rows added in header. This is used when adding
     * columns during cell editing.
     * 
     * @param isInsertRight if null, place of insertion should be deduced from
     *                      selection. This should be done when user clicks on
     *                      '+' icons in cells, not runs cmd from ctx menu.
     * @return
     */
    public int insertColumn(KTable table, Boolean isInsertRight) {
        
        if (table == null) {
            return -1;
        }
        
        TestBaseListModel kModel = (TestBaseListModel)table.getModel();

        TestBaseTableSelectionProvider selectionProvider = kModel.getSelectionProvider();
        
        HeaderNode parentNode = null;
        HeaderNode clickedNode = null;
        
        ISelection selection = selectionProvider.getSelection();
        
        if (selection instanceof TableEditorSelection) {
            TableEditorSelection tblEditorSelection = (TableEditorSelection)selection;
            parentNode = tblEditorSelection.getUserSeqOrMappingParent();
            clickedNode = tblEditorSelection.getClickedNode();
        }

        if (parentNode == null) {
            return -1; // it was not column selection
        }

        int numHeaderRows = kModel.getFixedHeaderRowCount();
        
        
        // for TestBase elements clicked node must not be null - clicking on an empty
        // cell does not automatically add a CTestBase to list - user must click '+' in column cell.
        if ((clickedNode != null  &&  clickedNode.isTestBaseList())  ||  parentNode.isTestBaseList()) {
            if (isInsertRight == null) {
                if (clickedNode != null) {
                    isInsertRight = new Boolean(clickedNode.isStructMapping());
                } else {
                    isInsertRight = new Boolean(parentNode.isStructMapping());
                }
            }
            insertTestBase(table, kModel, clickedNode, parentNode, isInsertRight.booleanValue());
            
        } else {
            
            // clickedNode may be null, when user clicks table body cell and then enters 
            // the value - this method is called from EmptyCellEditor.
            if (clickedNode != null) {
                if (isInsertRight == null) {
                    isInsertRight = clickedNode.isLeafNode();
                }

                if (parentNode.isUserMappingNode()  ||  clickedNode.isUserMappingNode()) {

                    insertUserMappingNode(table, kModel, clickedNode, parentNode, isInsertRight.booleanValue());

                } else if (parentNode.isUserSequenceNode()  ||  clickedNode.isUserSequenceNode()) {

                    insertUserSequenceNode(table, kModel, clickedNode, isInsertRight.booleanValue());
                }
                
            } else {
                
                if (isInsertRight == null) {
                    isInsertRight = false;
                }

                if (parentNode.isUserMappingNode()) {

                    insertUserMappingNode(table, kModel, parentNode, parentNode, isInsertRight.booleanValue());

                } else if (parentNode.isUserSequenceNode()) {

                    insertUserSequenceNode(table, kModel, parentNode, isInsertRight.booleanValue());
                }
            }
        }
        return kModel.getFixedHeaderRowCount() - numHeaderRows;
    }


    private void insertTestBase(KTable table, 
                                TestBaseListModel kModel, 
                                HeaderNode clickedNode, 
                                HeaderNode parentNode, 
                                boolean isInsertRight) {
        
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        int insertIdx = 0;
        
        if (clickedNode.isTestBaseList()) {
            parentNode = clickedNode;
        } else {
            insertIdx = parentNode.getChildIndex(clickedNode.getName());
            if (isInsertRight) {
                insertIdx++;
            }
        }
        
        AbstractAction action = arrayModel.createTestBaseInSubListAction(parentNode, insertIdx);
        
        execActionAndRefresh(action, table);
    }


    private void insertUserMappingNode(KTable table, 
                                       TestBaseListModel kModel, 
                                       HeaderNode clickedNode, 
                                       HeaderNode parentNode, 
                                       boolean isInsertRight) {
        
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        SimpleEntryDialog dlg = new SimpleEntryDialog(Activator.getShell(), 
                                                      ENTER_IDENTIFIER_NAME, 
                                                      LBL_IDENTIFIER, 
                                                      "Enter name of variable " +
                                                          "or other identifier to be used " +
                                                          "in assignments.");
        if (dlg.show()) {
            
            String identifierName = dlg.getData()[0].trim();
            
            if (!identifierName.isEmpty()) {
                // user mapping item is added to header only, since
                // it has no data at the beginning. Once the user enters
                // value in some cell, this item is added to the model.
                if (IS_USE_EMPTY_MAPPING_VALUES) {
                    
                    GroupAction groupAction = new GroupAction("Add user mapping column: " + identifierName);
                    List<String> predecessors = new ArrayList<String>();

                    if (clickedNode.isLeafNode()) {
                        predecessors = parentNode.getPredecessors(clickedNode.getName());
                        if (isInsertRight) {
                            predecessors.add(clickedNode.getName());
                        }
                    } else {
                        parentNode = clickedNode;
                    }

                    // If there are no rows, which means no CTestBases in CTestBaseList,
                    // add an empty row, because the body for loop following this 'if'
                    // will otherwise not be executed.
                    if (arrayModel.getRowCount() == 0) {
                        AbstractAction action = arrayModel.createAddRowAction(0);
                        execActionAndRefresh(action, table); // update the model
                    }
                    
                    for (int rowIdx = 0; rowIdx < arrayModel.getRowCount(); rowIdx++) {
                        InsertToUserMappingAction action = 
                                arrayModel.createAddUserMappingItemAction(parentNode, 
                                                                          rowIdx,
                                                                          predecessors,
                                                                          identifierName,
                                                                          "",
                                                                          "",
                                                                          "");
                        groupAction.add(action);
                    }
                    
                    execActionAndRefresh(groupAction, table);
                    
                } else {
                    if (!parentNode.insertUserMappingChild(identifierName, 
                                                                       clickedNode,
                                                                       isInsertRight)) {
                        throw new SIllegalArgumentException("Identifier with this name already exists: " + 
                                identifierName);
                    } else {
                        kModel.updateDataCells(); 
                        kModel.init();
                        table.redraw();
                    }
                }
            }
        }
    }


    private void insertUserSequenceNode(KTable table, 
                                        TestBaseListModel kModel, 
                                        HeaderNode clickedNode, 
                                        boolean isInsertRight) {
        
        ArrayTableModel arrayModel = kModel.getArrayModel();
        GroupAction groupAction = null; 

        if (clickedNode.isLeafNode()) { // clicked cell already contains sequence

            if (isInsertRight) {
                groupAction = arrayModel.createAddLeafSeqColumnAction(clickedNode, 0, 1);
            } else {
                // Insert left to the selected column.
                groupAction = arrayModel.createAddLeafSeqColumnAction(clickedNode, 0, 0);
            }
        } else { 
            // column of sequence without elements was clicked (greyed, non-editable)
            groupAction = arrayModel.createAddToSeqParentColumnAction(clickedNode, 0);
        }
        
        execActionAndRefresh(groupAction, table);
    }
}
