package si.isystem.tbltableeditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.tbltableeditor.handlers.DeleteColumnHandler;
import si.isystem.tbltableeditor.handlers.DeleteRowHandler;
import si.isystem.tbltableeditor.handlers.InsertColumnRightHandler;
import si.isystem.tbltableeditor.handlers.InsertRowAboveHandler;
import si.isystem.tbltableeditor.handlers.SwapRowsHandler;

/**
 * Listener Class that implements mouse actions for icons in the corners.
 */
public class TableMouseListener extends KTableListenerForTooltips implements Listener {


    public TableMouseListener(KTable table) {
        super(table);
    }
    
    
    @Override
    protected void processClicksOnCellIcons(Event event) {
        Point cell = m_ktable.getCellForCoordinates(event.x, event.y);
        KTableModel kModel = m_ktable.getModel();
        try {
            if (cell.x >= 0  &&  cell.x < kModel.getColumnCount() 
                    && cell.y >= 0  &&  cell.y < kModel.getRowCount()) {

                if (cell.x >= kModel.getFixedHeaderColumnCount()  &&  
                        cell.y >= kModel.getFixedHeaderRowCount()) {
                    // only body cells may have comments
                    showCommentDialogOrAddLastRow(event, cell, kModel);
                } else {
                    // only header cells may have add/del icons
                    addDeleteRowOrColumnForClickedCell(event, cell, kModel);
                }
            }
        } catch (ExecutionException ex) {
            SExceptionDialog.open(Activator.getShell(), "Table operation failed!", ex);
        }
    }


    private void showCommentDialogOrAddLastRow(Event event, Point cell, 
                                               KTableModel kModel) throws ExecutionException {
        
        Object cellContent = kModel.getContentAt(cell.x, cell.y);
        if (cellContent instanceof TextIconsContent) {
            TextIconsContent cellTIContent = (TextIconsContent)cellContent;
            EIconPos iconPos = getIconPos(event, cell);
            Image icon = cellTIContent.getIcon(iconPos, true);

            if (icon != null) {
                if (iconPos == EIconPos.ETopLeft  &&  !cellTIContent.getText().isEmpty()) {
                    editComment(cell.x, cell.y, cellTIContent);
                } else if (iconPos == EIconPos.EMiddleMiddle  &&  cell.y == (kModel.getRowCount() - 1)) {
                    new InsertRowAboveHandler().execute(null);
                }
            }
        }
    }


    /** Applicable only to header column and row cells with  + and x icons on the right. 
     * @param kModel 
     * @param cell
     * @throws ExecutionException 
     */
    private void addDeleteRowOrColumnForClickedCell(Event event, Point cell, 
                                                    KTableModel kModel) throws ExecutionException {
        
        // column header
        if (cell.y < kModel.getFixedHeaderRowCount()) {
            
            Object cellContent = kModel.getContentAt(cell.x, cell.y);
            if (cellContent instanceof TextIconsContent) {
                TextIconsContent cellTIContent = (TextIconsContent)cellContent;
                EIconPos iconPos = getIconPos(event, cell);
                Image icon = cellTIContent.getIcon(iconPos, true);

                if (icon != null) {
                    switch (iconPos) {
                    case ETopLeft:
                        break;
                    case EBottomLeft:
                        break;
                    case ETopRight:
                        new InsertColumnRightHandler().insertColumn();
                        break;
                    case EBottomRight:
                        new DeleteColumnHandler().execute(null);
                        break;
                    default:
                        // ignore other icons
                        break;
                    }
                }
            }
            
        // row header
        } else if (cell.x < kModel.getFixedHeaderColumnCount()) {
            
            Object cellContent = kModel.getContentAt(cell.x, cell.y);
            if (cellContent instanceof TextIconsContent) {
                TextIconsContent cellTIContent = (TextIconsContent)cellContent;
                EIconPos iconPos = getIconPos(event, cell);
                Image icon = cellTIContent.getIcon(iconPos, true);

                if (icon != null) {
                    switch (iconPos) {
                    case ETopLeft:
                        new SwapRowsHandler(false).execute(null);
                        break;
                    case EBottomLeft:
                        new SwapRowsHandler(true).execute(null);
                        break;
                    case ETopRight:
                        new InsertRowAboveHandler().execute(null);
                        break;
                    case EBottomRight:
                        new DeleteRowHandler().execute(null);
                        break;
                    default:
                        // ignore other icons
                        break;
                    }
                }
            }
        }
    }

    
    @Override
    protected void setComment(int col,
                              int row,
                              String newNlComment,
                              String newEolComment) {
        
        IModelWithComment kModel = (IModelWithComment)m_ktable.getModel();
        kModel.createSetCommentAction(col, row, 
                                      newNlComment, 
                                      newEolComment,
                                      m_ktable);
    }


    @Override
    protected void mouseUp(Event e) {
    }
}
