package si.isystem.itest.xls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.util.HSSFColor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.renderers.CheckableCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;

public class HSSFColorTableModel extends KTableModelAdapter {
    
    final public static int COL_SECTIONS = 0;
    final public static int COL_IS_VISIBLE = 1;
    final public static int COL_COLOR = 2;
    
    private String [] m_header = new String[]{"", "Visible", "Color"};
    private List<String> m_sections = new ArrayList<>();   // column 0
    private List<Boolean> m_isVisible = new ArrayList<>(); // column 1
    private List<HSSFColor> m_colors = new ArrayList<>();     // column 2
    private int m_rowHeight;
    private int[] m_columnWidths;
    private KTableCellRenderer m_checkableRenderer;
    private TextCellRenderer m_colorRenderer = new TextCellRenderer(SWT.NONE);
    private int m_colorsWidth;    

    public HSSFColorTableModel() {
        FontProvider fontProvider = FontProvider.instance();
        int fontHeight = fontProvider.getDefaultFontHeight(Activator.getShell());
        m_rowHeight = (int)(fontHeight * 1.3 + 6);
        m_checkableRenderer = new CheckableCellRenderer(CheckableCellRenderer.INDICATION_FOCUS);
    }        
     

    /**
     * Sorts items according to comma separated section names in parameter.
     *  
     * @param order
     */
    public void sort(String order) {
        String[] orderList = order.split(",");
        int destIdx = 0;
        
        for (String orderedSection : orderList) {
            for (int idx = 0; idx < m_sections.size(); idx++) {
                if (m_sections.get(idx).equals(orderedSection)) {
                    swap(destIdx, idx);
                    destIdx++;
                }
            }
        }
    }


    public int getWidth() {
        return m_columnWidths[COL_SECTIONS] + m_columnWidths[COL_IS_VISIBLE] + 
               m_columnWidths[COL_COLOR];
    }

    
    /**
     * Call this method after model contents is set.
     * 
     * @param control used only to create GC for measuring font.
     */
    public void initColumnWidths(Control control) {
        m_columnWidths = new int[3];

        int numRows = getRowCount();

        GC gc = new GC(control);
        FontProvider fontProvider = FontProvider.instance();

        int sectionsColWidth = 30;
        for (int row = 0; row < numRows; row++) {
            String content = (String)getContentAt(COL_SECTIONS, row);
            int width = fontProvider.getTextWidth(gc, content);
            sectionsColWidth = Math.max(sectionsColWidth, width);
        }

        int colorsColWidth = 30;
        Map<Integer, HSSFColor> hssfColorMap = HSSFColor.getIndexHash();
        for (Map.Entry<Integer, HSSFColor> entry : hssfColorMap.entrySet()) {
            HSSFColor color = entry.getValue();
            int width = fontProvider.getTextWidth(gc, color.getClass().getSimpleName());
            colorsColWidth = Math.max(colorsColWidth, width);
        }

        m_colorsWidth = colorsColWidth + colorsColWidth / 10;
        m_columnWidths[0] = (int)(sectionsColWidth * 1.1);
        m_columnWidths[1] = fontProvider.getTextWidth(gc, "__Visible__");
        m_columnWidths[2] = m_colorsWidth;

        gc.dispose();
        
    }
    
    
    public int getColorsColumnWidth() {
        return m_colorsWidth;
    }


    public void addRow(String sectionName, boolean isVisible, HSSFColor colorName) {
        m_sections.add(sectionName);
        m_isVisible.add(isVisible);
        m_colors.add(colorName);
    }
    
    
    public String getSectionName(int row) {
        return m_sections.get(row);
    }
    
    
    public Boolean isVisible(int row) {
        return m_isVisible.get(row);
    }
    
    
    public HSSFColor getColor(int row) {
        return m_colors.get(row);
    }
    
    
    /** Swaps items at the index given and the next one. */ 
    public void swap(int idx1) {
        swap(idx1, idx1 + 1);
    }
    
    
    public void swap(int idx1, int idx2) {
        if (idx1 < 0  ||  idx2 >= m_sections.size()) {
            return; // can't swap the first item up or the last one down
        }
        
        String first = m_sections.get(idx1);
        m_sections.set(idx1, m_sections.get(idx2));
        m_sections.set(idx2, first);
        
        Boolean firstV = m_isVisible.get(idx1);
        m_isVisible.set(idx1, m_isVisible.get(idx2));
        m_isVisible.set(idx2, firstV);
        
        HSSFColor firstC = m_colors.get(idx1);
        m_colors.set(idx1, m_colors.get(idx2));
        m_colors.set(idx2, firstC);
        
    }
    
    
    @Override
    public int doGetRowCount() {
        return m_sections.size() + 1;
    }
    
    
    @Override
    public int doGetColumnCount() {
        return 3;
    }
    

    @Override
    public int getInitialRowHeight(int row) {
        return m_rowHeight;
    }

    
    @Override
    public int getInitialColumnWidth(int column) {
        return m_columnWidths[column];
    }

    
    @Override
    public Object doGetContentAt(int col, int row) {
        if (row == 0) {
            return m_header[col];
        }
        row--;
        switch (col) {
        case COL_SECTIONS:
            return m_sections.get(row);
        case COL_IS_VISIBLE:
            return m_isVisible.get(row);
        case COL_COLOR:
            return m_colors.get(row).getClass().getSimpleName();
        default:
            break;
        }
        return null;
    }


    @Override
    public void doSetContentAt(int col, int row, Object value) {
        if (row < 1  ||  row > m_sections.size()) {
            return; // ignore invalid index
        }
        
        row--; // table coord to model coord
        
        switch (col) {
        case COL_SECTIONS:
            m_sections.set(row, (String)value);
            break;
        case COL_IS_VISIBLE:
            m_isVisible.set(row, (Boolean)value);
            break;
        case COL_COLOR:
            m_colors.set(row, (HSSFColor)value);
            break;
        }
    }


    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        
        if (col == COL_IS_VISIBLE  &&  row > 0) {
            return m_checkableRenderer;
        }
        
        if (col == COL_COLOR  &&  row > 0) {
            HSSFColor color = m_colors.get(row - 1);
            short[] triplet = color.getTriplet();
            Color bgcolor = ColorProvider.instance().getColor(triplet[0], triplet[1], triplet[2]);
            m_colorRenderer.setBackground(bgcolor);
            return m_colorRenderer;
        }
        
        return super.doGetCellRenderer(col, row);
    }
}
