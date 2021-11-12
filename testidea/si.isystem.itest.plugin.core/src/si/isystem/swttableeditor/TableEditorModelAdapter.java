package si.isystem.swttableeditor;

import java.util.ArrayList;
import java.util.List;


public class TableEditorModelAdapter implements ITableEditorModel {

    private List<ITableEditorRow> m_rows;
    private boolean m_isAutoAddLastEmptyRow = false;
    private List<ITableEditorModelChangedListener> m_listeners = 
                                new ArrayList<ITableEditorModelChangedListener>();

    /**
     * 
     * @param isAutoAddLastEmptyRow if true, the last row in the model is always
     *                              empty, so that it is editable in the table
     *                              and the user can easily enter new text
     */
    public TableEditorModelAdapter(boolean isAutoAddLastEmptyRow) {
        m_rows = new ArrayList<ITableEditorRow>();
        m_isAutoAddLastEmptyRow = isAutoAddLastEmptyRow;
    }

    /** Creates identical copy of this object. Rows get new instances, while
     * for listeners only references are copied. */
    @Override
    public TableEditorModelAdapter copy() {
        TableEditorModelAdapter newModel = new TableEditorModelAdapter(m_isAutoAddLastEmptyRow);
        
        for (ITableEditorModelChangedListener listener : m_listeners) {
            newModel.m_listeners.add(listener);
        }
        
        for (ITableEditorRow row : m_rows) {
            newModel.m_rows.add(row.createCopy(newModel));
        }
        return newModel;
    }

    
    @Override
    public void addModelChangedListener(ITableEditorModelChangedListener listener) {
        m_listeners.add(listener);
    }
    
    
    /**
     * This method adds item to model, but does not call modelChanged() method.
     * Use insert() method to notify listeners. This method is intended to
     * init the model with data before it is given to SWT control.  
     */
    @Override
    public void add(ITableEditorRow row) {
        if (m_rows.contains(row)) {
            System.out.println("ERROR in TableEditorModelAdapter: Duplicate elements are not allowed: " + row.toString());
            throw new IllegalArgumentException("Duplicate elements are not allowed: " + row.toString());
        }
        
        m_rows.add(row);
        row.setParent(this);
    }


    /**
     * @return index of the given item, or -1 if item was not found
     * 
     */
    @Override
    public int find(ITableEditorRow item) {
        return m_rows.indexOf(item);
    }

    
   /* @Override
    public int findByReference(ITableEditorRow row) {
        for (int i = 0; i < m_rows.size(); i++) {
            if (m_rows.get(i) == row) {
                return i;
            }
        }
        return -1;
    } */


    @Override
    public ITableEditorRow get(int i) {
        if (i < 0) {
            return m_rows.get(m_rows.size() + i);
        }
        
        return m_rows.get(i);
    }

    @Override
    public boolean isEmpty() {
        for (ITableEditorRow row : m_rows) {
            if (!row.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean isAutoAddLastEmptyRow() {
        return m_isAutoAddLastEmptyRow;    
    }
    

    @Override
    public List<ITableEditorRow> getRows() {
        return m_rows;
    }


    @Override
    public int size() {
        return m_rows.size();
    }


    @Override
    public void modelChanged(ITableEditorModelChangedListener.ChangeType changeType,
                             int idx1, int idx2,
                             ITableEditorRow row, String newName) {
        
        if (m_isAutoAddLastEmptyRow) {
            if (m_rows.size() == 0) {
                add(row.createEmpty());
            }

            ITableEditorRow lastRow = m_rows.get(m_rows.size() - 1);
            if (!lastRow.isEmpty()) {
                add(row.createEmpty());
            }
        }
        
        for (ITableEditorModelChangedListener listener : m_listeners) {
            listener.modelChanged(changeType, idx1, idx2, row);
        }
    }


    /**
     * @param selectedIdx to insert row element to. If negative, the row is appended to 
     *              the end of the list.
     */
    @Override
    public void insert(int selectedIdx, ITableEditorRow row) {
        /* duplicate row should not be prevented in the general TableEditorModelAdapter.
         * This prevented empty rows to be inserted, while it did not prevent typing duplicate rows.
         * if (m_rows.contains(row)) {  
            throw new IllegalArgumentException("List element already exists: '" + row.toString() + 
                                               "'\nPlease specify a new one.");
        } */
        
        if (selectedIdx >= 0) {
            m_rows.add(selectedIdx, row);
        } else {
            selectedIdx = size();
            m_rows.add(row);
        }
        row.setParent(this);
        
        modelChanged(ITableEditorModelChangedListener.ChangeType.ROW_ADDED, selectedIdx, 0, row, null);
    }


    @Override
    public ITableEditorRow remove(int selectedIdx) {
        ITableEditorRow row = m_rows.remove(selectedIdx);
        modelChanged(ITableEditorModelChangedListener.ChangeType.ROW_REMOVED, 
                     selectedIdx, 0, row, null);
        return row;
    }


    @Override
    public void swap(int first, int second) {
        ITableEditorRow row = m_rows.get(first);
        m_rows.set(first, m_rows.get(second));
        m_rows.set(second, row);
        modelChanged(ITableEditorModelChangedListener.ChangeType.ROWS_SWAPPED, 
                     first, second, null, null);
    }
}
