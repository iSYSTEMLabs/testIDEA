package si.isystem.swttableeditor;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.TableItem;

import si.isystem.exceptions.SIllegalStateException;



public class TableEditorCellModifier implements ICellModifier {
    private TableViewer m_tableViewer;
    private boolean m_isInModifyCycle = false;
    private boolean [] m_colEditableState;

    public TableEditorCellModifier(TableViewer tableViewer) {
        m_tableViewer = tableViewer;
    }
    
    @Override
    public boolean canModify(Object element, String property) {
        if (m_colEditableState != null) {
            String[]props = property.split(TableEditorPanel.COLUMN_PROP_PREFIX);
            if (props.length == 2) {
                int colIdx = Integer.parseInt(props[1]);
                if (colIdx < m_colEditableState.length) {
                    return m_colEditableState[colIdx];
                }
                throw new SIllegalStateException("Can not return editable status for column!")
                .add("columnProperty", property)
                .add("propsLen", props.length)
                .add("colIdx", colIdx)
                .add("colEditableStateLen", m_colEditableState.length);
            }
            throw new SIllegalStateException("Can not return editable status for column!")
            .add("columnProperty", property)
            .add("propsLen", props.length);
        }
        return true;
    }

    
    public void initColumnEditableStates(int numColumns, boolean defaultValue) {
        m_colEditableState = new boolean[numColumns];
        for (int i = 0; i < m_colEditableState.length; i++) {
            m_colEditableState[i] = defaultValue;
        }
    }
    
    
    public void setColEditableState(int idx, boolean colEditableState) {
        m_colEditableState[idx] = colEditableState;
    }

    
    @Override
    public Object getValue(Object element, String property) {
        m_isInModifyCycle = true;
        
        ITableEditorRow row = (ITableEditorRow)element;
        
        return row.getItem(property);
    }

    /**
     * Flag for modify cycle means, that the cell is going to be edited. This
     * flag can be examined in table focus listener, because Focus lost event
     * is sent for table, when its cell editor starts editing (the cell editor 
     * gets Focus gained event after that). This is particularly annoying, when
     * table's focus listener should perform action when the table really looses
     * focus (component outside of the table gets it). For example, table's model
     * may not be changed in focus listener when editing starts, but only when 
     * component outside of the table is selected.
     * If model is set when cell editor is active, you get exception in ColumnVieverEditor.
     * 
     * Flag for modify cycle is set automatically when editing starts. Call this 
     * method, when you are sure that the editing stopped, for example from 
     * table focus listener, when the focus is gained. Note that the modify() 
     * method of this object is NOT called, if user presses ESC. 
     */
    public void resetModifyCycle() {
        m_isInModifyCycle = false;
    }

    /**
     * Call this method from listeners, which have to know whether the table 
     * really lost to focus or only editing was started.
     *  
     * @see #resetModifyCycle()
     */
    public boolean isModifyCycle() {
        return m_isInModifyCycle;
    }
    
    
    @Override
    public void modify(Object element, String property, Object value) {
        if (value == null) {
            return;
        }
        
        ITableEditorRow row = null;
        if (element == null) {
            // there is a hard to reproduce bug somewhere in Eclipse or isystem's code
            // so that element == null. It can be reproduced when you modify contents 
            // several times in a row: F2 or ENTER, enter new value, F2 or ENTER, enter new value, ...
            // After 2 or 3 repetitions the value no longer changes since the element == null.
            // If you edit cell with double click, everything is OK. The reason seems to be
            // in SWTFocusCellManager, which keeps reference to the old row element.
            System.err.println("ERROR: TableEditorCellModifier.modify, element == null");
            
            // workaround
            ISelection selection = m_tableViewer.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSel = (IStructuredSelection)selection;
                if (!structuredSel.isEmpty()) {
                    element = structuredSel.getFirstElement();
                    if (element instanceof ITableEditorRow) {
                        row = (ITableEditorRow)element;
                    } else {
                        System.err.println("ERROR: TableEditorCellModifier.modify, element == null, not ITableEditorRow instance");
                        return; // workaround failed
                    }
                } else {
                    System.err.println("ERROR: TableEditorCellModifier.modify, element == null, empty selection");
                    return; // workaround failed
                }
            } else {
                System.err.println("ERROR: TableEditorCellModifier.modify, element == null, not structured selection");
                return; // workaround failed
            }
        } else {
            // this is a normal flow
            row = (ITableEditorRow)((TableItem)element).getData();
        }
        
        row.setItem(property, (String)value);
        
        // must refresh the table, otherwise the data from editor is stored to
        // the model, but it is not propagated to the table, which shows old value
        // after the editor field disappears.
        m_tableViewer.refresh();
    }
}
