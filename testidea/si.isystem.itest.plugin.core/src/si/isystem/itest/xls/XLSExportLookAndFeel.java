package si.isystem.itest.xls;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class XLSExportLookAndFeel {

    private short m_identifiersTextAngle;
    private boolean m_isUseColors;
    private boolean m_isFreezeHeaderRows;
    private boolean m_isFreezeTestIdColumn;
    private boolean m_isUseBottomCellBorder;
    private int m_cellBorderRowStep; // each n-th row has border
    private HSSFColorTableModel m_visibilityAndColors;
    
    private static final String IDENTIFIER_TEXT_ANGLE = "si.isystem.itest.xls.HeaderTextAngle";
    private static final String IS_USE_COLORS = "si.isystem.itest.xls.isUseColors";
    private static final String IS_FREEZE_ROWS = "si.isystem.itest.xls.isFreezeRows";
    private static final String IS_FREEZE_COLUMN = "si.isystem.itest.xls.isFreezeColumn";
    private static final String IS_USE_BOTTOM_CELL_BORDER = "si.isystem.itest.xls.isUseBottomCellBorder";
    private static final String BORDER_ROW_STEP = "si.isystem.itest.xls.borderRowStep";
    
    public XLSExportLookAndFeel(short identifiersTextAngle,
                                boolean isUseColors,
                                boolean isFreezeHeaderRows,
                                boolean isFreezeTestIdColumn,
                                boolean isUseBottomCellBorder,
                                int cellBorderRowStep) {
        super();
        m_identifiersTextAngle = identifiersTextAngle;
        m_isUseColors = isUseColors;
        m_isFreezeHeaderRows = isFreezeHeaderRows;
        m_isFreezeTestIdColumn = isFreezeTestIdColumn;
        m_isUseBottomCellBorder = isUseBottomCellBorder;
        m_cellBorderRowStep = cellBorderRowStep;
    }
    
    
    public short getIdentifiersTextAngle() {
        return m_identifiersTextAngle;
    }
    
    
    public String getIdentifiersTextAngleStr() {
        return String.valueOf(m_identifiersTextAngle);
    }
    
    
    public boolean isUseColors() {
        return m_isUseColors;
    }
    
    
    public boolean isFreezeHeaderRows() {
        return m_isFreezeHeaderRows;
    }
    
    
    public boolean isFreezeTestIdColumn() {
        return m_isFreezeTestIdColumn;
    }
    
    
    public boolean isUseBottomCellBorder() {
        return m_isUseBottomCellBorder;
    }
    
    
    public int getCellBorderRowStep() {
        return m_cellBorderRowStep;
    }


    public HSSFColorTableModel getVisibilityAndColors() {
        return m_visibilityAndColors;
    }


    public void getFromPrefs(IEclipsePreferences prefs) {
        m_identifiersTextAngle = (short)prefs.getInt(IDENTIFIER_TEXT_ANGLE, 90);
        m_isUseColors = prefs.getBoolean(IS_USE_COLORS, false);
        m_isFreezeHeaderRows = prefs.getBoolean(IS_FREEZE_ROWS, false);
        m_isFreezeTestIdColumn = prefs.getBoolean(IS_FREEZE_COLUMN, false);
        m_isUseBottomCellBorder = prefs.getBoolean(IS_USE_BOTTOM_CELL_BORDER, false);
        m_cellBorderRowStep = prefs.getInt(BORDER_ROW_STEP, 5);
    }
    

    public void storeToPrefs(IEclipsePreferences prefs) {
        prefs.putInt(IDENTIFIER_TEXT_ANGLE, m_identifiersTextAngle);
        prefs.putBoolean(IS_USE_COLORS, m_isUseColors);
        prefs.putBoolean(IS_FREEZE_ROWS, m_isFreezeHeaderRows);
        prefs.putBoolean(IS_FREEZE_COLUMN, m_isFreezeTestIdColumn);
        prefs.putBoolean(IS_USE_BOTTOM_CELL_BORDER, m_isUseBottomCellBorder);
        prefs.putInt(BORDER_ROW_STEP, m_cellBorderRowStep);
    }


    public void setIdentifiersTextAngleStr(String text) {
        if (text.trim().isEmpty()) {
            m_identifiersTextAngle = 0;
        } else {
            m_identifiersTextAngle = Short.parseShort(text);
        }
    }


    public void setUseColors(boolean isUseColors) {
        m_isUseColors = isUseColors;
    }


    public void setFreezeHeaderRows(boolean isFreezeHeaderRows) {
        m_isFreezeHeaderRows = isFreezeHeaderRows;
    }


    public void setFreezeTestIdColumn(boolean isFreezeTestIdColumn) {
        m_isFreezeTestIdColumn = isFreezeTestIdColumn;
    }


    public void setUseBottomCellBorder(boolean isUseBottomCellBorder) {
        m_isUseBottomCellBorder = isUseBottomCellBorder;
    }


    public void setCellBorderRowStep(String rowStep) {
        m_cellBorderRowStep = Short.parseShort(rowStep);
    }
    
    
    public void setVisibilityAndColors(HSSFColorTableModel visibilityAndColors) {
        m_visibilityAndColors = visibilityAndColors;
    }
}
