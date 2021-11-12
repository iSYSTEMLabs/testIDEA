package si.isystem.swttableeditor;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import si.isystem.ui.utils.UiTools;


/**
 * This class encapsulates all functionality available for table cell, for
 * example contents, colors, and tooltips. This class can be used as argument
 * to TableViewer.setLabelProvider() in which case we don't have the tooltip
 * functionlity.
 * If we use it as an argument to TableViewerColumn.setLabelProvider(), we
 * have to call setColumnIndex() for each column to get contents and tooltips
 * for each column.
 * 
 * @author markok
 *
 */
public class TableEditorLabelProvider extends CellLabelProvider 
                          implements ITableLabelProvider, ITableColorProvider {

    private int m_columnIndex = 0;

    public TableEditorLabelProvider(int columnIndex)
    {
        m_columnIndex = columnIndex;
    }
    
    
    /**
     * Utility method for creating an array of providers. 
     * @param numTableColumns number of columns in a table, where this providers
     * will be used. 
     */
    public static TableEditorLabelProvider[] createProviders(int numTableColumns) {
        TableEditorLabelProvider[] providers = new TableEditorLabelProvider[numTableColumns];
        for (int i = 0; i < providers.length; i++) {
            providers[i] = new TableEditorLabelProvider(i);
        }
        return providers;
    }
    
    
    @Override
    public String getToolTipText(Object element) {
        ITableEditorRow row = (ITableEditorRow)element;
        return row.getToolTip(m_columnIndex);
    }

    
    @Override
    public Point getToolTipShift(Object object) {
        return new Point(5, 5);
    }

    
    @Override
    public int getToolTipDisplayDelayTime(Object object) {
        return 0;
    }

    
    @Override
    public int getToolTipTimeDisplayed(Object object) {
        return UiTools.DEFAULT_TOOLTIP_DELAY;
    }

    
    @Override
    public void update(ViewerCell cell) {
        ITableEditorRow row = (ITableEditorRow)cell.getElement();
        cell.setText(getColumnText(row, m_columnIndex));
        cell.setImage(getColumnImage(row, m_columnIndex));
        cell.setForeground(getForeground(row, m_columnIndex));
        cell.setBackground(getBackground(row, m_columnIndex));
    }    

    
    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        ITableEditorRow row = (ITableEditorRow)element;
        return row.getImage(columnIndex);
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        
        ITableEditorRow row = (ITableEditorRow)element;
        
        return row.getItem(columnIndex);
    }

    
    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    
    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Color getForeground(Object element, int columnIndex) {
        ITableEditorRow row = (ITableEditorRow)element;
        
        return row.getForegroundColor(columnIndex);
    }

    @Override
    public Color getBackground(Object element, int columnIndex) {
        ITableEditorRow row = (ITableEditorRow)element;
        
        return row.getBackgroundColor(columnIndex);
    }
}
