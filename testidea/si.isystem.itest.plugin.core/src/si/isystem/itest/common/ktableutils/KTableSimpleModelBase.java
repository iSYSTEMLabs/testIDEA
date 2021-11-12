package si.isystem.itest.common.ktableutils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import de.kupzog.ktable.ICommandListener;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.editors.ContentProposalConfig;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import si.isystem.connect.CTestBase;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.IconProvider.EOverlayId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.tbltableeditor.KTableListenerForTooltips;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;

/**
 * This class can be used as a base class for simple table, where the first 
 * column is header column with row numbers and up/down/add/remove icons. 
 * It has one row for column headers, and the last row contains '+' icon for 
 * adding items. The number of data columns is defined by setting column headers
 * with method setColumnHeaders(String[numDataColumns]).
 * 
 * @author markok
 *
 */
abstract public class KTableSimpleModelBase extends KTableModelAdapter {

    public static final String KTABLE_SHORTCUTS_TOOLTIP =
            "Shortcuts:\n" +
            "F2 - edit\n" +
            "Ctrl + Space - open list of propsals (if available)\n" +
            // "F4 - edit with proposals (if available)\n" +
            // "Enter - edit\n" +  currently ENTER does not enter 
            // edit mode - in Excel it moves selection to cell below,
            //             and in dialogs ENTER should be propagated to dialog
            "Esc - revert editing\n\n" +
            "Del - delete cell contents\n" +
            "Backspace - clear cell and start editing\n\n" +
            "Shift + Space - row selection mode\n" +
            "Shift + Ctrl + Space - column selection mode\n\n" +
            "Ctrl + num + - add column or row if column or row is selected\n" +
            "Ctrl + num - - delete selected column or row\n\n" +
            "Home, End, Ctrl-Home, Ctrl-End - move to first/last column, first/last row" +
            "Ctrl + C, Ctrl + X,  Ctrl + V - standard clipboard shortcuts";
    
    private static final int HEADER_BKG_COLOR = 0xd0d0ff;
    protected static final int NUM_HDR_COLS = 1;
    public static final int NUM_HDR_ROWS = 1;
    public static final int NUM_TAIL_ROWS = 1; // last row contains '+' icon for adding new rows
    public static final int DATA_COL_IDX = 1;  // column 0 is used for line numbers

    protected static final int HEADER_COL_IDX = 0;
    protected static final int HEADER_ROW_IDX = 0;
    protected static final int DEFAULT_HEADER_COLUMN_WIDTH = 40;
    protected static final int DEFAULT_DATA_COLUMN_WIDTH = 150;



    protected TextIconsCellRenderer m_tableHeaderRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
    
    protected TextIconsCellRenderer m_editableCellRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);

    protected TextIconsCellRenderer m_lastRowRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);

    // top left cell with question mark icon with table tooltip
    private TextIconsContent m_tooltipCellContent = new TextIconsContent();
    // the last line in table which contains + sign
    private TextIconsContent m_addRowCellContent = new TextIconsContent();
    // cells with '+' and 'x' icons (ident. name and custom value name)
    private TextIconsContent m_colHeaderCell = new TextIconsContent();
    // other editable cells
    protected TextIconsContent m_editableCellContent = new TextIconsContent();

    private int[] m_columnWidths;
    protected String[] m_columnTitles;
    
    protected List<IKTableModelChangedListener> m_modelChangedListeners = new ArrayList<>();

    protected AsystContentProposalProvider [] m_contentProposals;
    
    protected boolean m_isEnabled = false;

    private KTableListenerForTooltips m_tooltipListener;
    
    
    public KTableSimpleModelBase() {
        
        m_tooltipCellContent.setIcon(EIconPos.ETopLeft, 
                                     IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_10x10),
                                     true);

        m_tableHeaderRenderer.setBackground(ColorProvider.instance().getColor(HEADER_BKG_COLOR));
        
        m_colHeaderCell.setIcon(EIconPos.ETopRight, 
                                IconProvider.INSTANCE.getIcon(EIconId.EAddTableColumn), 
                                true);
        m_colHeaderCell.setTooltip(EIconPos.ETopRight, "Add row above");

        m_colHeaderCell.setIcon(EIconPos.EBottomRight, 
                                IconProvider.INSTANCE.getIcon(EIconId.EDeleteTableColumn), 
                                true);
        m_colHeaderCell.setTooltip(EIconPos.EBottomRight, "Delete row");

        m_colHeaderCell.setIcon(EIconPos.ETopLeft, 
                                IconProvider.INSTANCE.getIcon(EIconId.EUpInTable), 
                                true);
        m_colHeaderCell.setTooltip(EIconPos.ETopLeft, "Move row up");

        m_colHeaderCell.setIcon(EIconPos.EBottomLeft, 
                                IconProvider.INSTANCE.getIcon(EIconId.EDownInTable), 
                                true);
        m_colHeaderCell.setTooltip(EIconPos.EBottomLeft, "Move row down");

        m_colHeaderCell.setEditable(false);
        
        m_addRowCellContent.setIcon(EIconPos.EMiddleMiddle, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EAddItem),
                                    true);
        m_addRowCellContent.setIcon(EIconPos.EMiddleMiddle, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EAddItemDisabled),
                                    false);
        
        m_addRowCellContent.setEditable(true);  // to get white background
        
        m_editableCellContent.setEditable(true);
    }
    
    
    public void setMainTooltip(String tooltip) {
        m_tooltipCellContent.setTooltip(EIconPos.ETopLeft, tooltip);
    }
    

    /**
     * Sets contents of header row. Length of this array defines the number of 
     * data (non-header) columns in the table. This setting is mandatory. 
     */
    public void setColumnTitles(String[] columnTitles) {
        m_columnTitles = columnTitles;
    }


    /**
     * This array defines widths of columns in pixels. This setting is mandatory.
     * @param widths array with one element more than the number of data 
     * columns (columnTitles.length + 1).
     * The first element specifies the width of the header column with row numbers. 
     */
    public void setColumnWidths(int[] widths) {
        m_columnWidths = widths;
    }
    

    public void setEnabled(boolean isEnabled) {
        m_isEnabled = isEnabled;
        m_tooltipListener.setEditable(isEnabled);
    }

    
    public void addModelChangedListener(IKTableModelChangedListener listener) {
        m_modelChangedListeners.add(listener);
    }

    
    protected void notifyListeners(AbstractAction action, 
                                   CTestBase testBase,
                                   boolean isRedrawNeeded) {
        
        for (IKTableModelChangedListener listener : m_modelChangedListeners) {
            listener.modelChanged(action, testBase, isRedrawNeeded);
        }
    }


    /**
     * Sets content proposals for the specified column.
     * 
     * @param colIdx
     * @param proposals
     * @param descriptions
     * @param proposalsAcceptanceStyle should be either 
     *        ContentProposalAdapter.PROPOSAL_REPLACE or
     *        ContentProposalAdapter.PROPOSAL_INSERT.   
     * @param proposalProvider can be either new AsystContentProposalProvider()
     *                         or one of derived classes. No data need to be set.
     *                         May be null if no proposals for column exist.
     */
    public void setAutoCompleteProposals(int colIdx, 
                                         AsystContentProposalProvider proposalProvider,
                                         int proposalsAcceptanceStyle) {
        if (m_contentProposals == null) {
            if (m_columnTitles == null) {
                throw new SIllegalStateException("Column titles must be set before proposals are set! "
                        + "Call method setColumnTtles() before this method.");
            }
            m_contentProposals = new AsystContentProposalProvider[m_columnTitles.length];
        }
        
        if (proposalProvider != null) {
            proposalProvider.setFiltering(true);
            proposalProvider.setProposalsAcceptanceStyle(proposalsAcceptanceStyle);
        }
        int dataCol = colIdx - NUM_HDR_COLS;
        m_contentProposals[dataCol] = proposalProvider;
    }


    @Override
    public int getInitialColumnWidth(int column) {
        if (m_columnWidths == null) {
            if (column == 0) {
                return DEFAULT_HEADER_COLUMN_WIDTH;
            }
            return DEFAULT_DATA_COLUMN_WIDTH;
        }
        
        return m_columnWidths[column];
    }


    @Override
    public int doGetColumnCount() {
        return m_columnTitles.length + NUM_HDR_COLS;
    }

    @Override
    public int getInitialRowHeight(int row) {
        return FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
    }


    @Override
    public Point belongsToCell(int col, int row) {
        // the last row has only header column and all other cells with + sign
        if (row == getRowCount() - 1  &&  col  > HEADER_COL_IDX) {
            return new Point(HEADER_COL_IDX + 1, row);
        }

        return null;
    }


    
    @Override
    public Object doGetContentAt(int col, int row) {
        
        if (row == 0) {  // header row
            if (col == 0) {
                // top left table cell
                return m_tooltipCellContent;
            }
            // column headers
            return m_columnTitles[col - NUM_HDR_COLS];
        }
        
        if (row == getRowCount() - 1) {
            if (col == 0) {
                // bottom left cell should be empty
                return new TextIconsContent();
            }
            setBackground(m_addRowCellContent, m_isEnabled);
            return m_addRowCellContent;
        }
        
        int dataRow = row - NUM_HDR_ROWS;

        if (col == 0) {
            if (row == getRowCount() - 1) {
                return ""; // header column in the last row (with '+' sign)
            }
            
            if (m_isEnabled) {
                m_colHeaderCell.setText(String.valueOf(dataRow));
                return m_colHeaderCell;
            }
            return String.valueOf(row - 1);
        }
        
        return null; // derived class should provide content
    }

    
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {

        if (row == 0) {
            return m_tableHeaderRenderer;                    
        }

        if (row == getRowCount() - 1) {
            return m_lastRowRenderer;
        }

        if (col == 0) {
            return m_editableCellRenderer;
        }
        
        return m_editableCellRenderer;
    }


    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {

        if (m_isEnabled  &&  isCellEditable(col, row)) {
            int dataColIdx = col - NUM_HDR_COLS;
            if (m_contentProposals != null  &&  m_contentProposals[dataColIdx] != null) {
                ContentProposalConfig cfg = new ContentProposalConfig(new String[0]);
                UiUtils.setContentProposalsConfig(cfg);
                cfg.setProposalProvider(m_contentProposals[dataColIdx]);
                cfg.setProposalsAcceptanceStyle(m_contentProposals[dataColIdx].getProposalsAcceptanceStyle());
                
                return new KTableCellEditorText2(cfg);
            } 
            return new KTableCellEditorText2();
        }

        return null;
    }

    
    protected boolean isCellEditable(int col, int row) {
        // header and last row (with + icon) and first column (with row numbers)
        // are not editable
        return !(row == 0  ||  row == getRowCount() - 1  ||  col == 0);
    }
    
    
    @Override
    public String doGetTooltipAt(int col, int row) {
        if (col == 0  &&  row == 0) {
            return m_tooltipCellContent.getTooltip(EIconPos.ETopLeft);
        }
        return null;
    }


    protected void doGetComment(TextIconsContent cellContent, 
                                String nlComment,
                                String eolComment) {
        
        cellContent.setNlComment(nlComment);
        cellContent.setEolComment(eolComment);

        boolean isCommentSet = !(nlComment + eolComment).trim().isEmpty();
        cellContent.setIcon(EIconPos.ETopLeft, 
                            getCommentIcon(cellContent.getText(), isCommentSet), 
                            true);

        setTooltipFromComment(cellContent, nlComment, eolComment, isCommentSet);
    }


    private void setTooltipFromComment(TextIconsContent cellContent,
                                         String nlComment,
                                         String eolComment,
                                         boolean isCommentSet) {
        if (isCommentSet) {
            String tooltip = YamlScalar.getToolTipText(nlComment, eolComment);
            cellContent.setTooltip(EIconPos.ETopLeft, tooltip);
        } else {
            cellContent.setTooltip(EIconPos.ETopLeft, "");
        }
    }

    
    private Image getCommentIcon(String text, boolean isCommentSet) {
        
        if (!text.isEmpty()) {
            if (isCommentSet) {
                if (m_isEnabled) {
                    return IconProvider.getOverlay(EOverlayId.EDITABLE_INFO);
                } else {
                    return IconProvider.getOverlay(EOverlayId.NONEDITABLE_INFO);
                }
            } else {
                if (m_isEnabled) {
                    return IconProvider.getOverlay(EOverlayId.EDITABLE_NO_INFO);
                } else {
                    return IconProvider.getOverlay(EOverlayId.NONEDITABLE_NO_INFO);
                }
            }
        } 
        
        return IconProvider.getOverlay(EOverlayId.NONEDITABLE_NO_INFO);
    }
    

    /**
     * This implementation can be used in tables where all data columns have 
     * comments.
     * @return true, if the cell has comment, false otherwise
     */
    protected boolean isCellHasComment(int col, int row) {
        return col > HEADER_COL_IDX  &&  
                row > HEADER_ROW_IDX  &&  
                row < getRowCount() - NUM_TAIL_ROWS; 
    }

    
    // TODO check if this one can be removed
    protected void setBackground(TextIconsContent cellContent,
                                 boolean isEditable) {
        if (isEditable) {
            cellContent.setBackground(ColorProvider.instance().getBkgColor());
        } else {
            cellContent.setBackground(ColorProvider.instance().getBkgNoneditableColor());
        }
    }


    
    public void addAllDefaultListeners(KTable table) {
//        addDefaultMouseListener(table);
        addDefaultKeyListener(table);
        addDefaultTooltipListener(table);
    }
    
    
    /**
     * Adds listener, which calls actions depending on clicked icon in the first
     * column (add/remove/swap rows) or last row (add row).
     *  
     * @param table the table to get listener assigned.
     */
    public void addDefaultMouseListener(final KTable table) {
        
//        table.addMouseListener(new MouseListener() {
//
//            @Override
//            public void mouseDoubleClick(MouseEvent e) {}
//
//            @Override
//            public void mouseDown(MouseEvent e) {
//            }
//
//            @Override
//            public void mouseUp(MouseEvent e) {
//                Point selection = table.getCellForCoordinates(e.x, e.y);
//                int col = selection.x;
//                int row = selection.y;
//
//                if (row == 0) {
//                    return;
//                }
//
//                if (row == getRowCount() - 1) { // add identifier is clicked
//                    addRow(row);
//                    return;
//                }
//
//                Rectangle cellRect = table.getCellRect(selection.x, selection.y);
//                EIconPos iconPos = TextIconsCellRenderer.getIconPos(cellRect, e.x, e.y);
//                if (col == HEADER_COL_IDX) {
//                    switch (iconPos) {
//                    case ETopRight:
//                        addRow(row);
//                        break;
//                    case EBottomRight:
//                        removeRow(row);
//                        break;
//                    case ETopLeft:
//                        swapRows(row, row - 1);
//                        break;
//                    case EBottomLeft:
//                        swapRows(row, row + 1);
//                        break;
//                    default:
//                        // ignore, unused location was clicked
//                    } 
//                } else {
//                    switch (iconPos) {
//                    case ETopLeft:
//                        setCellComment(col, row, );
//                        break;
//                    default:
//                        // ignore, unused location was clicked
//                    } 
//                }
//            }
//        });
    }

    
    /**
     * Adds default key listener, which adds/removes row if Ctrl-NumPad-+ or
     * Ctrl-NumPad-- is pressed.
     * 
     * @param table the table to get listener assigned.
     */
    public void addDefaultKeyListener(final KTable table) {
        
        table.setCommandListener(new ICommandListener() {
            
            @Override
            public void insertColumn() {}

            @Override
            public void insertRow() {}

            @Override
            public void deleteColumn() {}

            @Override
            public void deleteRow() {}

            @Override
            public void ctrlPlusPressed() {
                Point[] selection = table.getCellSelection();

                if (selection.length == 0) {
                    return;
                }
                
                if (selection[0].y > getRowCount()) {
                    return;
                }

                int row = selection[0].y;

                if (row < NUM_HDR_ROWS) { 
                    return;    // column header is selected, no adding
                }

                addRow(row);
            }

            
            @Override
            public void ctrlMinusPressed() {
                Point[] selection = table.getCellSelection();

                for (Point cell : selection) {

                    if (cell.y >= getRowCount() - 1) {
                        continue; // can't delete last row with +
                    }

                    int row = cell.y;

                    if (row < NUM_HDR_ROWS) { 
                        continue;    // column header is selected, no removal
                    }

                    removeRow(row);
                }
           }
        });
    }
    
    
    public void addDefaultTooltipListener(final KTable table) {
        m_tooltipListener = new KTableListenerForTooltips(table) {
            
            @Override
            protected void setComment(int col, int row, String newNlComment, String newEolComment) {
                setCellComment(col, row, 
                               newNlComment, newEolComment);
            }
            
            
            @Override
            protected void processClicksOnCellIcons(Event event) {
                // empty, event is handled in mouseUp() in mouse listener above
            }
            
            
            @Override
            public void mouseUp(Event e) {
                Point selection = table.getCellForCoordinates(e.x, e.y);
                int col = selection.x;
                int row = selection.y;

                if (row == 0) {
                    return;
                }

                if (row == getRowCount() - 1) { // add identifier is clicked
                    addRow(row);
                    return;
                }

                Rectangle cellRect = table.getCellRect(selection.x, selection.y);
                EIconPos iconPos = TextIconsCellRenderer.getIconPos(cellRect, e.x, e.y);
                if (iconPos != null) {
                    if (col == HEADER_COL_IDX) {
                        switch (iconPos) {
                        case ETopRight:
                            addRow(row);
                            break;
                        case EBottomRight:
                            removeRow(row);
                            break;
                        case ETopLeft:
                            swapRows(row, row - 1);
                            break;
                        case EBottomLeft:
                            swapRows(row, row + 1);
                            break;
                        default:
                            // ignore, unused location was clicked
                        } 
                    } else {
                        switch (iconPos) {
                        case ETopLeft:
                            Object cellContent = getContentAt(col, row);
                            if (cellContent instanceof TextIconsContent) {
                                TextIconsContent cellTIContent = (TextIconsContent)cellContent;

                                if (isCellHasComment(col, row)) {
                                    if (!cellTIContent.getText().isEmpty()) {
                                        editComment(col, row, cellTIContent);
                                    }
                                }
                            }
                            break;
                        default:
                            // ignore, unused location was clicked
                        } 
                    }
                }
            }
        };
    }
    
    
    abstract public void addRow(int row);
    abstract public void removeRow(int row);
    abstract public void swapRows(int rowFirst, int rowSecond);
    abstract protected void setCellComment(int col, 
                                           int row,
                                           String newNlComment,
                                           String newEolComment);
}
