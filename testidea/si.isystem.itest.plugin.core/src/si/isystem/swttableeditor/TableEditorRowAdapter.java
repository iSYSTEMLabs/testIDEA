package si.isystem.swttableeditor;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;



public class TableEditorRowAdapter implements ITableEditorRow {

    private ITableEditorModel m_parent;

    private String[]m_items;

    private String[] m_columnProperties;
    
    private Color m_foregroundColors[];
    private Color m_backgroundColors[];
    
    private Image[] m_images;
    
    private Object m_data;

    private String[]m_toolTips;
    
    /**
     * 
     * @param items are copied
     * @param columnProperties not copied, array should not be modified
     */
    public TableEditorRowAdapter(String []items, String []columnProperties) {
        
        if (items.length != columnProperties.length) {
            throw new IllegalArgumentException("Dimension of row elements and column " +
            		"properties do not match, no of row elements: " + items.length +
            		"  no of columns: " + columnProperties.length);
        }
        
        int idx = 0;
        m_items = new String[items.length];
        
        for (String item : items) {
            m_items[idx++] = item.trim();
        }
        
        m_columnProperties = columnProperties;
    }
    
    /** Creates empty row - all strings are set to "". */
    public TableEditorRowAdapter(String [] columnProperties) {
        int idx = 0;
        m_items = new String[columnProperties.length];
        
        for (int i = 0; i < m_items.length; i++) {
            m_items[idx++] = "";
        }
        
        m_columnProperties = columnProperties;
    }

    /** 
     * Clones the object - only items get a new copy, only references are copied for
     * other attributes.
     */
    @Override
    public ITableEditorRow createCopy(ITableEditorModel parentModel) {
        TableEditorRowAdapter row = new TableEditorRowAdapter(m_items, m_columnProperties);
        row.setParent(parentModel);
        
        if (m_images != null) {
            row.m_images = new Image[m_images.length];
            int idx = 0;
            for (Image image : m_images) {
                row.m_images[idx++] = image;
            }
        }
        
        row.m_data = m_data;
        return row;
    }
    
    
    @Override
    public ITableEditorRow createEmpty() {
        return new TableEditorRowAdapter(m_columnProperties);
    }

    @Override
    public boolean isEmpty() {
        
        for (String item : m_items) {
            if (!item.isEmpty()) {
                return false;
            }
        }

        return true;
    }
    
    @Override
    public ITableEditorModel getParent() {
        return m_parent;
    }

    
    @Override
    public void setParent(ITableEditorModel parent) {
        m_parent = parent;
    }

    
    @Override
    public String getItem(String key) {
        for (int i = 0; i < m_columnProperties.length; i++) {
            if (m_columnProperties[i].equals(key)) {
                return m_items[i];
            }
        }
        
        throw new IllegalArgumentException("Item with the given key not found: " + key);
    }

    @Override
    public void setItem(String key, String item) {
        for (int i = 0; i < m_columnProperties.length; i++) {
            if (m_columnProperties[i].equals(key)) {
                ITableEditorRow oldRow = createCopy(m_parent); 
                m_items[i] = item;
                if (m_parent != null) {
                    m_parent.modelChanged(ITableEditorModelChangedListener.ChangeType.CELL_CHANGED,
                                          i,
                                          m_parent.find(this),  // this index will be set by model 
                                          oldRow, item);
                }
                return;
            }
        }
        throw new IllegalArgumentException("Item with the given key not found: " + key);
    }

    @Override
    public String getItem(int index) {
        return m_items[index];
    }

    @Override
    public void setItem(int index, String item) {
        m_items[index] = item;
    }

    
    @Override 
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Checks for equality of items only. Parent and column names should be 
     * the same for all rows from the same model anyway.
     */
    @Override 
    public boolean equals(Object obj) {
        /* String objStr = obj.toString();
        if (obj instanceof TableEditorRowAdapter) {
            objStr = ((TableEditorRowAdapter)obj).toStr();
        }
        System.out.println("TableEditorRowAdapter - equals: " + toStr() + " / " + 
                           objStr + " / this = " + super.toString() + "  other = " + obj.toString() + 
                           "    == " + (obj == this)); */
        return obj == this;
        /* commented, because TableViewer searches item based on this method,
         * and it should be exactly the row selected, not the one which has 
         * the same contents. 
        if (obj == this) {
            return true;
        }
        
        if (obj == null  ||  !(obj instanceof TableEditorRowAdapter)) {
            return false;
        }
        
        TableEditorRowAdapter row = (TableEditorRowAdapter)obj;

        if (row.m_items.length != m_items.length) {
            return false;
        }
        
        for (int i = 0; i < m_items.length; i++) {
            if (!m_items[i].equals(row.m_items[i])) {
                return false;
            }
        }
        return true; */
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String item : m_items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }
        
        return sb.toString();
    }


    @Override
    public void setImage(int columnIndex, Image image) {
        if (m_images == null) {
            m_images = new Image[m_columnProperties.length];
        }
        m_images[columnIndex] = image;
    }
    
    
    @Override
    public Image getImage(int columnIndex) {
        if (m_images != null  &&  m_images.length > columnIndex) {
            return m_images[columnIndex];
        }
        return null;
    }


    /**
     * @return data object set by setData()
     * @see #setData(Object)
     */
    @Override
    public Object getData() {
        return m_data;
    }

    /**
     * Sets custom data. This object is not used internally, but can be retrieved
     * by application with getData().
     * 
     * @param data custom data
     * @see #getData()
     */
    @Override
    public void setData(Object data) {
        m_data = data;
    }

    @Override
    public void setForegroundColor(int columnIndex, Color foregroundColor) {
        if (m_foregroundColors == null) {
            m_foregroundColors = new Color[m_columnProperties.length];
        }
        m_foregroundColors[columnIndex] = foregroundColor;
    }

    @Override
    public Color getForegroundColor(int columnIndex) {
        if (m_foregroundColors != null  &&  m_foregroundColors.length > columnIndex) {
            return m_foregroundColors[columnIndex];
        }
        return null;
    }

    @Override
    public void setBackgroundColor(int columnIndex, Color backgroundColor) {
        if (m_backgroundColors == null) {
            m_backgroundColors = new Color[m_columnProperties.length];
        }
        m_backgroundColors[columnIndex] = backgroundColor;
    }

    @Override
    public Color getBackgroundColor(int columnIndex) {
        if (m_backgroundColors != null  &&  m_backgroundColors.length > columnIndex) {
            return m_backgroundColors[columnIndex];
        }
        return null;
    }
    
    @Override
    public String getToolTip(int index) {
        if (m_toolTips != null  &&  index < m_toolTips.length) {
            return m_toolTips[index];
        }
        return null;
    }

    @Override
    public void setToolTip(int index, String toolTip) {
        if (m_toolTips == null) {
            m_toolTips = new String[m_columnProperties.length]; 
        }
        if (toolTip.isEmpty()) {
            m_toolTips[index] = null; // empty tooltips should not show even a small tooltip box
        } else {
            m_toolTips[index] = toolTip;
        }
    }
}
