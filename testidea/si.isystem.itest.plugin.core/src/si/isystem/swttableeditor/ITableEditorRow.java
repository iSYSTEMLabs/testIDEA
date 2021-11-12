package si.isystem.swttableeditor;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;



public interface ITableEditorRow {

    /** Creates empty row with all strings empty. */
    ITableEditorRow createEmpty();
    
    /** Returns true, if all strings are empty (length() == 0). */
    boolean isEmpty();

    /** Returns parent model. */
    ITableEditorModel getParent();

    /** Sets parent model. */
    void setParent(ITableEditorModel parent);

    /** Sets item for the given index (column). */
    void setItem(int index, String item);
    
    /** Returns item for the given index (column). */
    String getItem(int index);
    
    
    /** Sets item for the given key. This method is called by TableEditor when
     * the user modified contents of the cell.
     */
    void setItem(String key, String item);
    
    /** Returns item for the given key. */
    String getItem(String key);

    /** Sets icon for element at the given column index. */
    void setImage(int columnIndex, Image image);
    
    /** Returns image for the given column. May return null if there is no image. */
    Image getImage(int columnIndex);

    /** Sets text color. 
     * @param color text color, if null the default text color is used. 
     */
    void setForegroundColor(int columnIndex, Color color);
    
    /** Returns item color, may be null in which case the default color should be used. */
    Color getForegroundColor(int columnIndex);
    
    
    /** Sets text color. 
     * @param color text color, if null the default text color is used. 
     */
    void setBackgroundColor(int columnIndex, Color color);
    
    /** Returns item color, may be null in which case the default color should be used. */
    Color getBackgroundColor(int columnIndex);
    
    
    /** Returns identical copy of the object. */
    ITableEditorRow createCopy(ITableEditorModel parentModel);

    
    /**
     * @return data object set by setData()
     * @see #setData(Object)
     */
    Object getData();

    /**
     * Sets custom data. This object is not used internally, but can be retrieved
     * by application with getData().
     * 
     * @param data custom data
     * @see #getData()
     */
    void setData(Object data);

    /**
     * Returns tooltip for the given cell.
     * @param columnIndex index of table column
     */
    String getToolTip(int columnIndex);
    
    /**
     * Sets tooltip for the given cell.
     * @param columnIndex index of table column for which to set the tool-tip.
     * @param toolTip tool-tip text
     */
    void setToolTip(int columnIndex, String toolTip);
    
}

