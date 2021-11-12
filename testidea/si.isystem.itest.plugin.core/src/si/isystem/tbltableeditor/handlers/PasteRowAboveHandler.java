package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Rectangle;

import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.tbltableeditor.ArrayTableModel;
import si.isystem.tbltableeditor.TestBaseListModel;
import de.kupzog.ktable.KTable;

/** Handler for context menu. */
public class PasteRowAboveHandler extends TableEditorHandlerBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        pasteInsertRows(false);
        return null;
    }

    
    protected void pasteInsertRows(boolean isInsertBelow) {
        
        KTable table = UiUtils.getKTableInFocus();
        if (table == null) {
            return;
        }
        
        TestBaseListModel kModel = (TestBaseListModel)table.getModel();
        ArrayTableModel arrayModel = kModel.getArrayModel();
        
        if (table.getCellSelection().length == 0) {
            throw new SIllegalStateException("Please select a starting row, " +
                                             "to paste contents from clipboard to.");
        }

        /* if (m_table.getSelectionMode() != ESelectionMode.ERow) {
            throw new SIllegalStateException("Please select a row, not column or cell to paste contents from clipboard to.");
        } */
        
        String yamlSpec = UiUtils.getYamlFromClipboard();

        if (!yamlSpec.startsWith(TestBaseListModel.TEST_IDEA_TABLE_TEST_BASE_CLIPBOARD_TAG)) {
            MessageDialog.openWarning(Activator.getShell(), 
                                      "Paste rows", 
                                      "Can't paste rows.\n" +
                                      "There is no information for table rows on clipboard.");
            return;
        }
        
        yamlSpec = yamlSpec.substring(TestBaseListModel.TEST_IDEA_TABLE_TEST_BASE_CLIPBOARD_TAG.length() + 1);
        
        Rectangle selRect = table.getSelectedRect();
        int pasteRowIndex = selRect.y - kModel.getFixedHeaderRowCount();
        if (isInsertBelow) {
            pasteRowIndex += 1 + selRect.height; // insert below selection
        }
        GroupAction groupAction = arrayModel.createInsertParsedListAction(yamlSpec, 
                                                                          pasteRowIndex);
        groupAction.addFireEventType(EFireEvent.REDO);
        groupAction.addFireEventType(EFireEvent.UNDO);
        
        execActionAndRefresh(groupAction, table);
    }
    
}

