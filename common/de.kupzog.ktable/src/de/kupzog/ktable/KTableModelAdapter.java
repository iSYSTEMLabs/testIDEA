package de.kupzog.ktable;

import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;

/**
 * This class overrides all methods that have meaningful default implementation.
 * The created table has one header row and one header column. Columns are 
 * resizable by default, while rows are not.
 * 
 * @author markok
 */
abstract public class KTableModelAdapter extends KTableDefaultModel {

    protected TextCellRenderer m_textCellRenderer = 
            new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
    
    protected FixedCellRenderer m_headerCellRenderer = 
            new FixedCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);

    @Override
    public int getFixedHeaderRowCount() {
        return 1;
    }

    @Override
    public int getFixedHeaderColumnCount() {
        return 1;
    }

    @Override
    public int getFixedSelectableRowCount() {
        return 0;
    }

    @Override
    public int getFixedSelectableColumnCount() {
        return 0;
    }

    @Override
    public boolean isColumnResizable(int col) {
        return true;
    }

    @Override
    public boolean isRowResizable(int row) {
        return false;
    }

    @Override
    public int getRowHeightMinimum() {
        return 10;
    }

    @Override
    public int getInitialColumnWidth(int column) {
        return 50;
    }

    @Override
    public int getInitialRowHeight(int row) {
        return 15;
    }

    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {
        return null;
    }

    @Override
    public void doSetContentAt(int col, int row, Object value) {
        // the table is not editable by default
    }

    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        // System.out.println("renderer: " + col + ", " + row);
        if (col < getFixedHeaderColumnCount()  ||  row < getFixedHeaderRowCount()) {
            return m_headerCellRenderer;
        }
        return m_textCellRenderer;
    }
}
