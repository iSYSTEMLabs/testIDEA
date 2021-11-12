package si.isystem.tbltableeditor;

import si.isystem.itest.model.AbstractAction;
import de.kupzog.ktable.KTable;

/**
 * This interface adds cell comments to KTable model. It is implemented by
 * models, which support cell comments.
 * 
 * @author markok
 *
 */
public interface IModelWithComment {

    /**
     * Sets comment for the given cell. 
     * 
     * @param col abs col. number, including header columns
     * @param row abs row. number, including row columns
     * @param value new cell value
     * 
     * @return action to be executed to set the value
     */
    public void createSetCommentAction(int col, int row, 
                                       String nlComment, String eolComment,
                                       KTable table);

    /**
     * Creates action for setting content of the given cell. If the cell is not 
     * editable (getCellEditor() returns null), then null is returned. 
     * 
     * @param col abs col. number, including header columns
     * @param row abs row. number, including row columns
     * @param value new cell value
     * 
     * @return action to be executed to set the value
     */
    AbstractAction createSetContentAction(int col, int row, String value,
                                          String nlComment, String eolComment);
}
