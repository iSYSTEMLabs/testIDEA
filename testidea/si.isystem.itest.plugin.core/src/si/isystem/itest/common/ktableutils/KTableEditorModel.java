package si.isystem.itest.common.ktableutils;

import org.eclipse.swt.graphics.Image;

import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EOverlayId;

/**
 * Base class intended to be extended by concrete model. Provides icons for
 * adding, removing and moving rows.
 * @author markok
 *
 */
abstract public class KTableEditorModel extends KTableSimpleModelBase {

    public KTableEditorModel() {
 
    }

    
    @Override
    public int doGetRowCount() {
        return getBodyRowCount() + NUM_HDR_ROWS + NUM_TAIL_ROWS;
    }

    
    @Override
    public Object doGetContentAt(int col, int row) {
        
        Object content = super.doGetContentAt(col, row);
        if (content != null) {
            return content;
        }
        
        return getBodyCellContent(col - 1, row - 1);
    }

    
    @Override
    public void doSetContentAt(int col, int row, Object value) {
        
        if (isCellEditable(col, row)) {
            setBodyContentAt(col - 1, row - 1, (String)value);
        }
    }
    
        
    protected Image getResultIcon(boolean isResult, Boolean isOK) {
        
        if (isResult  &&  isOK != null) {
            if (isOK.booleanValue()) {
                return IconProvider.getOverlay(EOverlayId.TEST_OK_OVERLAY);
            } else {
                return IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY);
            }
        }
        
        return null;
    }
    
    
    /**
     * Should return content of the table body cell - non-header cells
     * @param bodyCol table body column, starts with 0 (table column 1)
     * @param bodyRow table body row, starts with 0 (table column 1)
     */
    abstract public TextIconsContent getBodyCellContent(int bodyCol, int bodyRow);
    /**
     * @return number of rows for table body, without header
     */
    abstract protected int getBodyRowCount();
    abstract protected void setBodyContentAt(int bodyCol, int bodyRow, String value);
}