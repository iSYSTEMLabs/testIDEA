package si.isystem.swttableeditor;

import java.util.List;

public interface ITableEditorModel {

    /** 
     * Creates identical copy of this object. Rows get new instances, while
     * for listeners only references are copied. 
     */
    ITableEditorModel copy();
    
    /**
     * Adds model listener. If listener already exists, nothing is added. Listeners
     * are not allowed to throw exceptions on notifications!
     * @param listener  model listener to be added
     */
    void addModelChangedListener(ITableEditorModelChangedListener listener);

    /**
     * @return number of items in the model. One item maps to one row in table.
     */
    int size();

    /** Inserts the given item at the given index. */
    void insert(int selectedIdx, ITableEditorRow row);

    /** Returns index of the given item or -1 it the item was not found. */
    int find(ITableEditorRow row);

    /** Adds item to the model, at the end of the list. */
    void add(ITableEditorRow row);

    /** Removes item at selected index. Returns the item removed. */
    ITableEditorRow remove(int selectedIdx);

    /**
     * Returns item from the list.
     * 
     * @param i index in the list. If negative, items are counted from the end of 
     * the list. For example, i == -1 returns the last element in the list.
     * @return item at index i
     */
    ITableEditorRow get(int i);

    /** Returns true, if there is no data in the model. */
    boolean isEmpty();
    
    /**
     * Returns true, is the last empty row should be automatically added. This
     * enables users to enter new item without pressing Add button, but may
     * cause inconsistent data in the table in some cases (for example when
     * there are restrictions on valid entries). 
     * 
     * @return true, if empty rows are automatically added
     */
    boolean isAutoAddLastEmptyRow();
    
    /** Exchanges items at the given indices. */
    void swap(int first, int second);

    /** Returns all rows in a list. */
    List<ITableEditorRow> getRows();

    /** Called whenever the model changes. */
    void modelChanged(ITableEditorModelChangedListener.ChangeType changeType,
                      int columnIdx, int rowIdx, ITableEditorRow row, 
                      String newName);

    /**
     * Look for object by reference, not contents (operator == is used 
     * instead of method equals() 
    public int findByReference(ITableEditorRow row);
     */
    
}
