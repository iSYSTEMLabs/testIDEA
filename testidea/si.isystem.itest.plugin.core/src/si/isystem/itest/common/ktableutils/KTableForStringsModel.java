package si.isystem.itest.common.ktableutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This model supports KTable with List<String[]>, where each String[] defines one 
 * table row. There is one header row with custom column titles, and one 
 * header column with line number and +-v^ icons.
 * 
 *  Example:
 *  model.setMainTooltip("This is awesome table.");
 *  model.setColumnTitles(new String[]{"First column"});
 *  model.setData(new ArrayList<String[]>{new String[]{"someValue"}})
 *  model.setColumnWidths(new int[]{100})
 *  model.setModelChangedListener(new ...);
 *  model.setAutoCompleteProposals(1, ...);
 *  
 */
public class KTableForStringsModel extends KTableSimpleModelBase {

//    private static final int COL_IDX = 0;
//    public static final int DEFAULT_HEADER_COLUMN_WIDTH = 40;

//    private String m_mainTooltip;

//    protected TextIconsCellRenderer m_tableHeaderRenderer = 
//            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
//    
//    protected TextIconsCellRenderer m_editableCellRenderer = 
//            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);
//
//    protected TextIconsCellRenderer m_lastRowRenderer = 
//            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);
//
//    protected TextIconsContent m_addRowCellContent = new TextIconsContent();
//    // cells with '+' and 'x' icons (ident. name and custom value name)
//    protected TextIconsContent m_colHeaderCell = new TextIconsContent();
//    // other editable cells
//    protected TextIconsContent m_editableCellContent = new TextIconsContent();

//    private int[] m_colWidths;
//    private String[] m_columnTitles;
//    
//    private IKTableModelChangedListener m_modelChangedListener;
//
//    protected AsystContentProposalProvider [] m_contentProposals;

    private List<String[]> m_lines;

    public KTableForStringsModel() {

//        m_tableHeaderRenderer.setBackground(ColorProvider.instance().getColor(0xd0d0ff));

//        Image icon = IconProvider.INSTANCE.getIcon(EIconId.EAddItem);
//        m_addRowCellContent.setIcon(EIconPos.EMiddleMiddle, icon, true);
//        m_addRowCellContent.setEditable(true);  // to get white background

//        m_colHeaderCell.setIcon(EIconPos.ETopRight, 
//                                         IconProvider.INSTANCE.getIcon(EIconId.EAddTableColumn), 
//                                         true);
//        m_colHeaderCell.setIcon(EIconPos.EBottomRight, 
//                                         IconProvider.INSTANCE.getIcon(EIconId.EDeleteTableColumn), 
//                                         true);
//        m_colHeaderCell.setIcon(EIconPos.ETopLeft, 
//                                         IconProvider.INSTANCE.getIcon(EIconId.EUpInTable), 
//                                         true);
//        m_colHeaderCell.setIcon(EIconPos.EBottomLeft, 
//                                         IconProvider.INSTANCE.getIcon(EIconId.EDownInTable), 
//                                         true);
//        m_colHeaderCell.setEditable(false); // used for header column with line numbers

//        m_editableCellContent.setEditable(true);
        
        m_lines = new ArrayList<>();
    }


    /** 
     * Sets data to be shown in table. String arrays in list must have the same
     * dimension as the titles. This setting is mandatory.
     * @param lines
     */
    public void setData(List<String[]> lines) {

        m_lines = lines;
    }

    
    public String verifyModel() {
        
        int lineIdx = 0;
        
        for (String[] line : m_lines) {
            
            for (int dataColIDx = 0; dataColIDx < line.length; dataColIDx++) {
                
                if (line[dataColIDx].isEmpty()) {
                    return "Please specify item in line: " + lineIdx + 
                            ", column: '" + m_columnTitles[dataColIDx] + "'";
                }
            }
            
            lineIdx++;
        }
        return null;
    }


    /** 
     * Sets tooltip text to be displayed on mouse hover of help icon in top 
     * left cell. If not set, the top left cell is empty.
     */
//    public void setMainTooltip(String mainTooltip) {
//        m_mainTooltip = mainTooltip;
//    }


//    /**
//     * Sets contents of header row. Length of this array defines the number of 
//     * columns in the table. This setting is mandatory. 
//     * @param columnTitles
//     */
//    public void setColumnTitles(String[] columnTitles) {
//        m_columnTitles = columnTitles;
//    }
    

//    public void setModelChangedListener(IKTableModelChangedListener modelChangedListener) {
//        m_modelChangedListener = modelChangedListener;        
//    }
//
//
//    /**
//     * This array defines widths of columns in pixels. This setting is mandatory.
//     * @param widths array with one element more than the number of data 
//     * columns (columnTitles.length + 1).
//     * The first element specifies the width of the header column with row numbers. 
//     */
//    public void setColumnWidths(int[] widths) {
//        m_colWidths = widths;
//    }
//    
//
//    /**
//     * Sets content proposals for the specified column.
//     * 
//     * @param colIdx
//     * @param proposals
//     * @param descriptions
//     * @param proposalsAcceptanceStyle should be either 
//     *        ContentProposalAdapter.PROPOSAL_REPLACE or
//     *        ContentProposalAdapter.PROPOSAL_INSERT.   
//     *   
//     */
//    public void setAutoCompleteProposals(int colIdx, 
//                                         String [] proposals, 
//                                         String [] descriptions,
//                                         int proposalsAcceptanceStyle) {
//        if (m_contentProposals == null) {
//            if (m_columnTitles == null) {
//                throw new SIllegalStateException("Column titles must be set before proposals are set! "
//                        + "Call method setColumnTtles() before this method.");
//            }
//            m_contentProposals = new AsystContentProposalProvider[m_columnTitles.length];
//        }
//        m_contentProposals[colIdx] = new AsystContentProposalProvider(proposals, descriptions);
//        m_contentProposals[colIdx].setFiltering(true);
//        m_contentProposals[colIdx].setProposalsAcceptanceStyle(proposalsAcceptanceStyle);
//    }


//    @Override
//    public int doGetColumnCount() {
//        return m_columnTitles.length + 1; // + header column
//    }


//    @Override
//    public int getInitialColumnWidth(int column) {
//        if (m_colWidths == null) {
//            return DEFAULT_HEADER_COLUMN_WIDTH;
//        }
//        return m_colWidths[column];
//    }
//
//
//    @Override
//    public int getInitialRowHeight(int row) {
//        return FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
//    }
//
//
//    @Override
//    public Point belongsToCell(int col, int row) {
//        // the last row has only header column and all other cells with + sign
//        if (row == getRowCount() - 1  &&  col  > COL_IDX) {
//            return new Point(COL_IDX + 1, row);
//        }
//
//        return null;
//    }


    @Override
    public int doGetRowCount() {
        return m_lines.size() + NUM_HDR_ROWS + NUM_TAIL_ROWS;
    }

    
    @Override
    public Object doGetContentAt(int col, int row) {
        
//        if (row == 0) {
//            if (col == 0) {
//                // left top corner cell
//                TextIconsContent value = new TextIconsContent();
//                
//                if (m_mainTooltip != null) {
//                    value.setTooltip(EIconPos.ETopLeft, m_mainTooltip);
//                    value.setIcon(EIconPos.ETopLeft, 
//                                  IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_10x10),
//                                  true);
//                }
//                return value;
//            }
//            return m_columnTitles[col - 1];
//        }

//        if (col == 0) {
//            if (row == getRowCount() - 1) {
//                return ""; // header column in the last row (with '+' sign)
//            }
//            
//            m_colHeaderCell.setText(String.valueOf(row - 1));
//            return m_colHeaderCell;
//        }

//        if (row == getRowCount() - 1) {
//            return m_addRowCellContent;
//        }

        Object content = super.doGetContentAt(col, row);
        if (content != null) {
            return content;
        }
        
        m_editableCellContent.setText(m_lines.get(row - 1)[col - 1]);
        return m_editableCellContent;
    }


//
//    @Override
//    public KTableCellRenderer doGetCellRenderer(int col, int row) {
//
//        if (row == 0) {
//            return m_tableHeaderRenderer;                    
//        }
//
//        if (row == getRowCount() - 1) {
//            return m_lastRowRenderer;
//        }
//
//        if (col == 0) {
//            return m_editableCellRenderer;
//        }
//        
//        return m_editableCellRenderer;
//    }
//

    @Override
    public void doSetContentAt(int col, int row, Object value) {

        if (isCellEditable(col, row)) {

            String strVal = value == null ? "" : value.toString().trim();

            int varIdx = row - 1;
            int colIdx = col - 1;

            m_lines.get(varIdx)[colIdx] = strVal;

            notifyListeners(null, null, false);
        }
    }


//    /**
//     * Adds or removes identifier or value-occurrence, depending on the cell
//     * and area inside cell clicked.
//     * 
//     * @param col cell column
//     * @param row cell row
//     * @param cellRect cell coordinates in pixels
//     * @param x click coordinate
//     * @param y click coordinate
//     * 
//     * @return false if model was not changed - no redraw needed, true if table
//     *         redraw is required
//     */
//    public void addRemoveItem(int col,
//                              int row,
//                              Rectangle cellRect,
//                              int x,
//                              int y) {
//
//        if (row == 0) {
//            return;
//        }
//
//        if (row == getRowCount() - 1) { // add identifier is clicked
//            String[] line = new String[m_columnTitles.length];
//            Arrays.fill(line, "");
//            m_lines.add(line);
//            notifyListeners(null, null, true);
//            return;
//        }        
//
//        boolean isModelChanged = true;
//
//        if (col == HEADER_COL_IDX) {
//            EIconPos iconPos = TextIconsCellRenderer.getIconPos(cellRect, x, y);
//            int idx = row - 1;
//            switch (iconPos) {
//            case ETopRight:
//                String[] line = new String[m_columnTitles.length];
//                Arrays.fill(line, "");
//                m_lines.add(idx, line);
//                break;
//            case EBottomRight:
//                m_lines.remove(idx);
//                break;
//            case ETopLeft:
//                swapListElements(idx, idx - 1);
//                break;
//            case EBottomLeft:
//                swapListElements(idx, idx + 1);
//                break;
//            default:
//                isModelChanged = false;
//            } 
//        } 
//
//        if (isModelChanged) {
//            notifyListeners(null, null, true);
//        }
//    }

    @Override
    public void addRow(int row) {
        String[] line = new String[m_columnTitles.length];
        Arrays.fill(line, "");
        int dataRow = row - NUM_HDR_ROWS;
        m_lines.add(dataRow, line);
        notifyListeners(null, null, true);
    }
    
    
    @Override
    public void removeRow(int row) {
        int dataRow = row - NUM_HDR_ROWS;
        m_lines.remove(dataRow);
        notifyListeners(null, null, true);
    }
    
    
    @Override
    public void swapRows(int first, int second) {
        first -= NUM_HDR_ROWS;
        second -= NUM_HDR_ROWS;
        if (first >= 0  &&  second >= 0  &&  
                first < m_lines.size()  &&  second < m_lines.size()) {

            String[] firstEl = m_lines.get(first);
            m_lines.set(first, m_lines.get(second));
            m_lines.set(second, firstEl);
            notifyListeners(null, null, true);
        }
    }
    
    
    @Override
    protected void setCellComment(int bodyCol, 
                                  int bodyRow,
                                  String newNlComment,
                                  String newEolComment) {
        // string list does not have comments
    }
}    

