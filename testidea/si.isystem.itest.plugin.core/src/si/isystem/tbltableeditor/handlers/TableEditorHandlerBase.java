package si.isystem.tbltableeditor.handlers;

import org.eclipse.core.commands.AbstractHandler;

import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.tbltableeditor.TestBaseListModel;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableModel;

abstract public class TableEditorHandlerBase extends AbstractHandler {

    protected void execActionAndRedraw(AbstractAction action, KTable table) {
        
        KTableModel model = table.getModel();
        
        if (model instanceof TestBaseListModel) {
            TestBaseListModel tblModel = (TestBaseListModel) model;
            tblModel.execActionAndRedraw(action, table);
        } else {
            try {
                TestSpecificationModel.getActiveModel().execAction(action);
            } catch (SException ex) {
                table.redraw(); // show changes, because some actions may have been
                                // executed, when exception occurred.
                StatusView.getView().flashDetailPaneText(StatusType.FATAL, 
                                                         SEFormatter.getInfo(ex));
            } catch (Exception ex) {
                table.redraw(); // show changes, because some actions may have been
                                // executed, when exception occurred.
                StatusView.getView().flashDetailPaneText(StatusType.FATAL, 
                                                         ex.getMessage() + "'");
            }
        }
    }

    
    protected void execActionAndRefresh(AbstractAction action, KTable table) {

        KTableModel model = table.getModel();
        
        if (model instanceof TestBaseListModel) {
            TestBaseListModel tblModel = (TestBaseListModel) model;
            tblModel.execActionAndRefresh(action, table);
        } else {
            try {
                TestSpecificationModel.getActiveModel().execAction(action);
            } catch (SException ex) {
                table.redraw(); // show changes, because some actions may have been
                                // executed, when exception occurred.
                StatusView.getView().flashDetailPaneText(StatusType.FATAL, 
                                                         SEFormatter.getInfo(ex));
            } catch (Exception ex) {
                table.redraw(); // show changes, because some actions may have been
                                // executed, when exception occurred.
                StatusView.getView().flashDetailPaneText(StatusType.FATAL, 
                                                         ex.getMessage() + "'");
            }
        }
    }


    protected static void execAction(AbstractAction action, KTable table) {

        KTableModel model = table.getModel();
        
        if (model instanceof TestBaseListModel) {
            TestBaseListModel tblModel = (TestBaseListModel) model;
            tblModel.execAction(action);
        } else {
            TestSpecificationModel.getActiveModel().execAction(action);
        }
    }
}
