package si.isystem.tbltableeditor;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.editors.CellEditorComboBox;
import de.kupzog.ktable.editors.ContentProposalConfig;
import de.kupzog.ktable.editors.KTableCellEditorCombo;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.connect.utils.OsUtils;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SIndexOutOfBoundsException;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.IconProvider.EOverlayId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.IResultProvider.EResultStatus;
import si.isystem.tbltableeditor.handlers.InsertColumnHandler;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;

/**
 * This class adapts ArrayTableModel to KTableModel. It takes header columns 
 * and rows into account.
 */
public class TestBaseListModel implements KTableModel, IModelWithComment {

    public static final String CLIPBOARD_DATA_TAG = "data";
    public static final String CLIPBOARD_MODE_TAG = "mode";
    public static final String TEST_IDEA_TABLE_CLIPBOARD_TAG = "testIDEATable";
    public static final String TEST_IDEA_TABLE_TEST_BASE_CLIPBOARD_TAG = "testIDEATableTestBase:";
    private static final int SEQ_DATA_BKG_COLOR = 0xeeffee;
    private static final int MAPPING_DATA_BKG_COLOR = 0xeeffff;
    
    
    private ArrayTableModel m_arrayTableModel;
    private boolean m_isOptimizeForInheritance = false;
    private IResultProvider m_resultProvider;
    private Map<String /* section name from column header */, 
                IContentProposalProvider> m_contentProposalsMap = new TreeMap<>();
    private IActionExecutioner m_actionExecutioner;
    
    private int m_columnWidths[]; // stores pre-calculated widths of columns
    private Control m_control;
    private final int m_rowHeight;
    private int[] m_rowHeights; // if set with call to adjustRowHeights() these values
                                // are used instead of m_rowHeight 
    private KTableCellEditor m_textCellEditor;
    private CellEditorTristate m_tristateEditor;
    private KTableCellEditor m_emptyCellEditor; // for cells, which have no 
                                                // content in model - adds list or mapping item 
    private TestBaseTableSelectionProvider m_selectionProvider;
    
    // CellEditorComboBox does not open on Linux, user can not select arbitrary items.
    // KTableCellEditorCombo can be opened with mouse, but not with keyboard.
    private CellEditorComboBox m_comboEditorWin;
    private KTableCellEditorCombo m_comboEditorLinux;
    
    private String m_mainTooltip;
    private String[][] m_headerTooltips;
    private TextIconsContent m_cellWithPlusSign;
    private boolean m_isEditable;

    private ISysModelChangedListener m_modelChangedListener;

    private TextIconsContent m_noEditIconsRowColumnHeader= new TextIconsContent();
    private TextIconsContent m_secondDataRowColumnHeader= new TextIconsContent();
    private TextIconsContent m_columnHeaderContent = new TextIconsContent();
    private boolean m_isTestBaseListEditable = true;
        
    // this cell in header is editable - user mapping key
    private static TextIconsCellRenderer m_userMappingHeaderRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
    
    // this cell in header may create/remove additional columns - sequence columns 
    private static TextIconsCellRenderer m_sequenceHeaderRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
    
    private static TextIconsCellRenderer m_headerRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
    
    private static KTableCellRenderer m_textIconsCellRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);

    private static TristateCellRenderer m_tristateCellRenderer = 
            new TristateCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);

    static {
        m_userMappingHeaderRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
        m_sequenceHeaderRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_GREEN));
    }
    
    
    /**
     * 
     * @param dataProvider
     * @param control used only for calculation of column widths
     * @param nodeId 
     * @param m_isEditingOutlineTree 
     */
    public TestBaseListModel(Control control, IActionExecutioner actionExecutioner, 
                             ENodeId nodeId, boolean m_isEditingOutlineTree) {
        
        m_arrayTableModel = new ArrayTableModel(nodeId, m_isEditingOutlineTree);

        m_control = control;
        m_actionExecutioner = actionExecutioner;

        FontProvider fontProvider = FontProvider.instance();
        int fontHeight = fontProvider.getDefaultFontHeight(Activator.getShell());
        m_rowHeight = (int)(fontHeight * 1.3 + 6);
 
        m_textCellEditor = new KTableCellEditorText2();
        m_emptyCellEditor = new EmptyCellEditor();
        Rectangle bounds = TristateCellRenderer.IMAGE_CHECKED.getBounds();
        m_tristateEditor = new CellEditorTristate(new Point(bounds.width, 
                                                            bounds.height), // approx. size 
                                                  SWTX.ALIGN_HORIZONTAL_CENTER, 
                                                  SWTX.ALIGN_VERTICAL_CENTER);
        m_comboEditorLinux = new KTableCellEditorCombo();
        m_comboEditorWin = new CellEditorComboBox();
        
        m_cellWithPlusSign = new TextIconsContent();
        m_cellWithPlusSign.setTooltip(EIconPos.EMiddleMiddle, 
                                      "Click or press Enter to add new element to the table.");
        m_cellWithPlusSign.setIcon(EIconPos.EMiddleMiddle, 
                                   IconProvider.INSTANCE.getIcon(EIconId.EAddItem),
                                   true);
        m_cellWithPlusSign.setIcon(EIconPos.EMiddleMiddle, 
                                   IconProvider.INSTANCE.getIcon(EIconId.EAddItemDisabled),
                                   false);
        m_cellWithPlusSign.setEditable(true);
        
        configureColumnHeader(m_secondDataRowColumnHeader, false);
        configureColumnHeader(m_columnHeaderContent, true);
    }
    
    
    private void configureColumnHeader(TextIconsContent columnHeaderContent, boolean isMoveUpVisible) {
        columnHeaderContent.setIcon(EIconPos.ETopRight, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EAddTableColumn),
                                    true);
        columnHeaderContent.setIcon(EIconPos.EBottomRight, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EDeleteTableColumn),
                                    true);
        columnHeaderContent.setIcon(EIconPos.ETopRight, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EEmptyOverlay),
                                    false);
        columnHeaderContent.setIcon(EIconPos.EBottomRight, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EEmptyOverlay),
                                    false);
        columnHeaderContent.setTooltip(EIconPos.ETopRight, "Add row above");
        columnHeaderContent.setTooltip(EIconPos.EBottomRight, "Delete row");

        /* add row up-down arrows */

        if (isMoveUpVisible) {
            columnHeaderContent.setIcon(EIconPos.ETopLeft, 
                                        IconProvider.INSTANCE.getIcon(EIconId.EUpInTable), 
                                        true);
            columnHeaderContent.setIcon(EIconPos.ETopLeft, 
                                        IconProvider.INSTANCE.getIcon(EIconId.EEmptyOverlay), 
                                        false);
            columnHeaderContent.setTooltip(EIconPos.ETopLeft, "Move row up");
        }

        columnHeaderContent.setIcon(EIconPos.EBottomLeft, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EDownInTable), 
                                    true);
        columnHeaderContent.setIcon(EIconPos.EBottomLeft, 
                                    IconProvider.INSTANCE.getIcon(EIconId.EEmptyOverlay), 
                                    false);

        columnHeaderContent.setTooltip(EIconPos.EBottomLeft, "Move row down");
    }


    public void setModelData(CTestBase testBase, int section) {
        m_arrayTableModel.setModelData(testBase, section);
    }
    
    
    public void setModelData(CTestBase testBase, int section, CTestBase firstRow) {
        
        // clear cached merged test specs from previous input
        CTestBase oldInput = getModelData();
        if (oldInput != null  &&  oldInput instanceof CTestSpecification) {
            clearCachedMergedTestSpecs((CTestSpecification)oldInput);
        }        
        
        if (m_isOptimizeForInheritance  &&  testBase instanceof CTestSpecification) {
            cacheMergedTestSpecs((CTestSpecification)testBase);
        }
        m_arrayTableModel.setModelData(testBase, section, firstRow);
    }
    
    
    public void setVisibleSections(Map<String, int[]> m_visibleSections) {
        m_arrayTableModel.setVisibleSections(m_visibleSections);
    }
        
    
    public void setOptimizeForInheritance(boolean isOptimizeForInheritance) {
        m_isOptimizeForInheritance = isOptimizeForInheritance;
    }


    /**
     * Call this method when structure of the model has changed, for example
     * when rows/columns are added/removed. Call also KTable.redraw() to update
     * UI. 
     */
    public void init() {
        
        m_columnWidths = new int[m_arrayTableModel.getColumnCount() + 1];  // +1 for header column
        
        FontProvider fontProvider = FontProvider.instance();
        int numRows = getRowCount();
        
        GC gc = new GC(m_control);
        TextIconsCellRenderer renderer = new TextIconsCellRenderer(0, false);
        
        for (int column = 0; column < m_columnWidths.length; column++) {
            int maxColWidth = 30;
            for (int row = 0; row < numRows; row++) {
                TextIconsContent tiContent = (TextIconsContent)getContentAt(column, row);
                String content = tiContent.getText();
                int width = fontProvider.getTextWidth(gc, content) + renderer.getIconsSpace(tiContent);
                maxColWidth = Math.max(maxColWidth, 
                                       width);
            }
            
            m_columnWidths[column] = (int)(maxColWidth * 1.3);
        }
        
        gc.dispose();
        
        m_headerRenderer.setAlignment(SWTX.ALIGN_HORIZONTAL_CENTER | SWTX.ALIGN_VERTICAL_CENTER);
    }


    CTestBase getModelData() {
        return m_arrayTableModel.getModelData();
    }
    

    public void refresh() {
        m_arrayTableModel.refresh();
    }
    
    
    public void refreshStructureChange(KTable table) {
        CTestBase testBase = getModelData();
        if (m_isOptimizeForInheritance  &&  testBase instanceof CTestSpecification) {
            cacheMergedTestSpecs((CTestSpecification)testBase);
        }
        m_arrayTableModel.refresh(); // model has changed, so a refresh is needed
        init();
        table.redraw();  // time spent here was always 0 ms   
    }

    
    /**
     * This method checks contents of all cells and adjusts row height if multiline
     * string are present in the row. Currently this method must be called after each change
     * in model, but in the future heights will be adjusted only for rows which
     * are added/removed/content changed. 
     */
    public void adjustRowHeights() {
        int numDataRows = m_arrayTableModel.getRowCount();
        int numDataColumns = m_arrayTableModel.getColumnCount();
        m_rowHeights = new int[numDataRows];
        
        for (int row = 0; row < numDataRows; row++) {
            m_rowHeights[row] = m_rowHeight;
            for (int column = 0; column < numDataColumns; column++) {
                ArrayTableCell cell = m_arrayTableModel.getDataValue(column, row);
                if (cell != null  &&  cell.existsInModel()) {
                    String text = cell.getValue();
                    m_rowHeights[row] *= text.split("\n").length;
                }
            }
        }
    }
    
    
    /**
     * 
     * @param sectionName name of a CTestBase section (iyaml tag name). See also
     *                    doc for getTextCellEditor() in this class. 
     * @param provider content provider
     */
    public void addContentProvider(String sectionName, IContentProposalProvider provider) {
        m_contentProposalsMap.put(sectionName, provider);
    }
    
    
    public void setResultProvider(IResultProvider resultProvider) {
        m_resultProvider = resultProvider;
    }


    public void setModelChangedListener(ISysModelChangedListener listener) {
        m_modelChangedListener = listener;
    }
    
    
    public void addSectionListener(String sectionPath, SectionActionListener listener) {
        m_arrayTableModel.addSectionListener(sectionPath, listener);
    }
    
    
    public void updateDataCells() {
        m_arrayTableModel.tbList2DataCells();
    }
    
    
    /**
     * This method may be called only if CTestSpecification-s are edited.
     * 
     * @param col
     * @param row
     * @return
     */
    public SetSectionAction toggleInheritance(int col, int row) {
        int dataCol = col - getFixedHeaderColumnCount();
        int dataRow = row - getFixedHeaderRowCount();
        
        if (dataCol < 0  ||  dataRow < 0  ||  row == (getRowCount() - 1)) {
            return null; // ignore header cells and last row with '+'
        }
        
        int sectionId = m_arrayTableModel.getRootTestBaseSection(dataCol);
        
        // workaround for params in CTestFunction have separate inheritance setting
        if (sectionId == SectionIds.E_SECTION_FUNC.swigValue()) {
            HeaderNode selectedNode = m_arrayTableModel.getHeader().getFirstNonEmptyCellBottomUp(dataCol);
            if (selectedNode.getSectionId() == CTestFunction.ESection.E_SECTION_PARAMS.swigValue()) {
                sectionId = SectionIds.E_SECTION_PARAMS_PRIVATE.swigValue();
            }
        }
        
        return m_arrayTableModel.createSetInheritanceAction(dataRow, sectionId, null);
    }
    
    
    @Override
    public int getColumnCount() {
        return m_arrayTableModel.getColumnCount() + 1; // +1 for header column
    }

    
    /**
     * Use this method, when data in the table is handled, because the last row
     * contains editing controls when editable, not data.
     * @return
     */
    public int getDataRowCount() {
        return m_arrayTableModel.getRowCount() + 
               m_arrayTableModel.getHeader().getRowCount();
    }
    
    
    @Override
    public int getRowCount() {
        int rowCount = m_arrayTableModel.getRowCount() + 
                       m_arrayTableModel.getHeader().getRowCount();
        
        if (m_isEditable) {
            rowCount++;  // for last row with 'add' icons
        }
        
        return rowCount; 
    }

    
    @Override
    public Object getContentAt(int column, int row) {
        // try catch block was added to provide better diagnostic on rare 
        // errors noticed by customers. It should also prevent table from crashing
        // and the need for testIDEA restart. B015227, jan 2015
        
        // 2015-07-07: Fix does not work as expected, because table repaint is executed
        // before the dialog closes, so we get in catch again before the dialog closes.
        // Removed opening of a dialog, returns error string now.
        // How to reproduce: create test case WO stubs, execute it, add TWO stubs,
        // select the second one, click + in bottom table of a row - error.
        //
        // 30.jul.2015 Fixed properly. See svn.
        try {
            return doGetContentAt(column, row);
        } catch (Exception ex) {
           // If dialog is opened, table repaint is issued before the dialog closes,
           // so we end in endless loop here. 
           // SExceptionDialog.open(Activator.getShell(), 
           //                       "Internal error in TestBaseListModel.getContentAt()!", 
           //                       ex);
            TextIconsContent value = new TextIconsContent();
            value.setText("ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return value;
        }
    }
    
    
    private Object doGetContentAt(int column, int row) {
        //System.out.println("content: [" + column + ", " + row + "]: " + m_tableModel.getDataValue(column, row));
        HeaderNode header = m_arrayTableModel.getHeader();
        int numHeaderRows = header.getRowCount();
        int numDataRows = m_arrayTableModel.getRowCount();
        int modelColumn = column - getFixedHeaderColumnCount();
        int modelRow = row - numHeaderRows;

        TextIconsContent value = new TextIconsContent();
        
        if (row < numHeaderRows) {
            if (column == 0) {
                // left top corner
                value.setTooltip(EIconPos.ETopLeft, m_mainTooltip);
                value.setIcon(EIconPos.ETopLeft, 
                              IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_10x10),
                              true);
            } else {
                // header
                HeaderNode headerNode = header.getNode(modelColumn, row);
                
                if (headerNode != null) {
                    value.setText(headerNode.getName());

                    if (m_isEditable) {
                        
                        // set add/del column icons
                        boolean isSeqOrMapping = headerNode.isUserMappingNode()  ||  
                                headerNode.isUserSequenceNode();

                        boolean isChildOfSeqOrMapping = (headerNode.getParent().isUserSequenceNode()  ||
                                headerNode.getParent().isUserMappingNode())  &&
                                headerNode.isLeafNode();

                        boolean isTestBaseList = headerNode.isTestBaseList();

                        boolean isChildOfTestBaseList = headerNode.getParent().isTestBaseList();

                        // There is always possible to add node. New node 
                        // is always added at index 0. This way user can add nodes anywhere: 
                        // '+' in index nodes adds item to the right, '+' in header node adds 
                        // node left to the leftmost item (index 0).
                        if (isSeqOrMapping  ||  isChildOfSeqOrMapping  ||  isTestBaseList  ||  isChildOfTestBaseList) {

                            value.setIcon(EIconPos.ETopRight, 
                                          IconProvider.INSTANCE.getIcon(EIconId.EAddTableColumn),
                                          true);
                            if (isChildOfSeqOrMapping  ||  isChildOfTestBaseList) {
                                value.setTooltip(EIconPos.ETopRight, "Add column to right");
                            } else {
                                value.setTooltip(EIconPos.ETopRight, "Add the first column");
                            }
                        }

                        if (isChildOfSeqOrMapping  ||  isChildOfTestBaseList) {
                            value.setIcon(EIconPos.EBottomRight, 
                                          IconProvider.INSTANCE.getIcon(EIconId.EDeleteTableColumn),
                                          true);
                            value.setTooltip(EIconPos.EBottomRight, "Delete column");
                        } 
                    }
                }
                
                StringBuilder result = new StringBuilder();
                EResultStatus status = m_resultProvider != null ? 
                                         m_resultProvider.getColumnResult(modelColumn, 
                                                                          result) :
                                         EResultStatus.NO_RESULT;
                
                setResult(value, status, result, EIconPos.EBottomLeft);
                
                // header cells do _not_ have comments in table, because they
                // may differ for each CTestBase in a table. Also, if they don't
                // differ, it makes no sense to maintain the same comment in all
                // derived test specs. User should edit comments in the parent
                // test spec.
            }
            
        } else if (row < numHeaderRows + numDataRows) {
            
            if (column == 0) {
                
                TextIconsContent colHeader = m_columnHeaderContent;
                
                if (m_isTestBaseListEditable) {
                    // first row is special when it contains additional test base not in original list,
                    // for example when it contain parent test case fo test cases in next rows. 
                    // Movement of this row is not possible, as it is not member of the list, so hide
                    // icons in header column.
                    if (m_arrayTableModel.isFirstRowSpecial()) {
                        if (modelRow == 0) {
                            colHeader = m_noEditIconsRowColumnHeader;
                        } else if (modelRow == 1) {
                            colHeader = m_secondDataRowColumnHeader;
                        }
                    }
                } else {
                    colHeader = m_noEditIconsRowColumnHeader;
                }
                
                // row header (contains row numbers)
                colHeader.setText(String.valueOf(modelRow));
                colHeader.setEditable(m_isEditable);

                StringBuilder result = new StringBuilder();
                EResultStatus status = m_resultProvider != null ?
                                         m_resultProvider.getRowResult(modelRow, 
                                                                       result) :
                                         EResultStatus.NO_RESULT;
                setResult(colHeader, status, result, EIconPos.EBottomLeft_R);
                return colHeader;
                
            } else {
                
                // table body
                ArrayTableCell cellData = m_arrayTableModel.getDataValue(modelColumn, 
                                                                         modelRow);
                arrayCell2TableCell(value, cellData, header, modelColumn);
               
                StringBuilder result = new StringBuilder();
                EResultStatus status = m_resultProvider != null ?
                                         m_resultProvider.getCellResult(modelColumn, 
                                                                        modelRow, 
                                                                        result) :
                                         EResultStatus.NO_RESULT;
                setResult(value, status, result, EIconPos.EBottomLeft);
            }
        } else {
            if (column > 0) {
                // the last row with 'add' icons to add a new list item
                value = m_cellWithPlusSign;
                value.setBackground(ColorProvider.instance().getBkgColor());

            }
        }
        
        return value;
    }


    
    private void setResult(TextIconsContent value,
                           EResultStatus status,
                           StringBuilder result,
                           EIconPos iconPos) {

        Image icon = null;
        switch (status) {
        case ERROR:
            icon = IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY);
            break;
        case OK:
            icon = IconProvider.getOverlay(EOverlayId.TEST_OK_OVERLAY);
            break;
        case NO_RESULT:
        default:
            break;
        }
        
        value.setIcon(iconPos, icon, true);
        value.setTooltip(iconPos, result.toString());
    }


    private void arrayCell2TableCell(TextIconsContent tiCell,
                                     ArrayTableCell cellData, 
                                     HeaderNode header, 
                                     int modelColumn) {
        
        if (cellData == null) {
            tiCell.setBackground(ColorProvider.instance().getBkgNoneditableColor());
            tiCell.setEditable(false);
            return;
        }

        tiCell.setDefaultForTristate(cellData.getDefaultForTristate());
        tiCell.setEditorType(cellData.getEditorType());
        // tiCell.setComboItems(cellData.getComboItems());
        tiCell.setDefaultEnumValue(cellData.getDefaultEnumValue());
        
        if (cellData.existsInModel()) {
            String text = cellData.getValue();
            tiCell.setText(text);

            tiCell.setTristateValue(cellData.getTristateValue());
            
            String comment = cellData.getComment();
            tiCell.setTooltip(EIconPos.ETopLeft, comment);
            
            String[] comments = cellData.getComments();
            tiCell.setNlComment(comments[0]);
            tiCell.setEolComment(comments[1]);
            
            if (cellData.isReadOnlyCell()) {
                tiCell.setBackground(ColorProvider.instance().getColor(ColorProvider.MERGED_BKG_COLOR));
            } else {
                if (cellData.isUserSeqCell()) {
                    tiCell.setBackground(ColorProvider.instance().getColor(SEQ_DATA_BKG_COLOR));
                } else if (cellData.isUserMappingCell()) {
                    tiCell.setBackground(ColorProvider.instance().getColor(MAPPING_DATA_BKG_COLOR));
                } else {
                    tiCell.setBackground(ColorProvider.instance().getBkgColor());
                }
            }
            
            if (!text.isEmpty()) {
                if (!comment.isEmpty()) {
                    tiCell.setIcon(EIconPos.ETopLeft, 
                                  IconProvider.getOverlay(EOverlayId.EDITABLE_INFO),
                                  true);
                } else {
                    tiCell.setIcon(EIconPos.ETopLeft, 
                                  IconProvider.getOverlay(EOverlayId.EDITABLE_NO_INFO),
                                  true);
                }
            } else {
                tiCell.setIcon(EIconPos.ETopLeft, 
                              IconProvider.getOverlay(EOverlayId.NONEDITABLE_NO_INFO),
                              true);
            }
            
            tiCell.setEditable(!cellData.isReadOnlyCell());
            
        } else {
            tiCell.setText("");
            tiCell.setTristateValue(cellData.getDefaultForTristate() ? ETristate.E_TRUE.toString() :
                                                                       ETristate.E_FALSE.toString());
            
            if (cellData.isReadOnlyCell()) {
                tiCell.setBackground(ColorProvider.instance().getColor(ColorProvider.MERGED_BKG_COLOR));
            } else {
                tiCell.setBackground(ColorProvider.instance().getBkgNoneditableColor());
            }
            
            // if cellData does not exist in model, it may still be editable if it is an empty
            // sequence or mapping cell
            HeaderNode firstNonEmptyNode = header.getFirstNonEmptyCellBottomUp(modelColumn);
            if (firstNonEmptyNode != null  &&  firstNonEmptyNode.isLeafNode()) {
                tiCell.setEditable(true);
            }
        }
    }

    
    @Override
    public String getTooltipAt(int col, int row) {
        
        if (m_headerTooltips != null  && 
                row < m_headerTooltips.length  &&  
                col < m_headerTooltips[row].length) {
            return m_headerTooltips[row][col];
        }
        
        return null;
    }

    
    public int getVisibleColumn(String sectionTreePath) {
        return m_arrayTableModel.getColumnOfHeaderNode(sectionTreePath) + 
               getFixedHeaderColumnCount();    
    }
    
    
    @Override
    public int getFixedHeaderColumnCount() {
        return 1;
    }

    
    @Override
    public int getFixedHeaderRowCount() {
        return m_arrayTableModel.getHeader().getRowCount();
    }

    
    @Override
    public int getFixedSelectableColumnCount() {
        return 0;
    }

    
    @Override
    public int getFixedSelectableRowCount() {
        return 0;
    }

    
    @Override
    public int getColumnWidth(int column) {
        return m_columnWidths[column];
    }


    @Override
    public int getRowHeight(int row) {
        row = row - getFixedHeaderRowCount();
        if (m_rowHeights != null  &&  row >= 0  &&  row < m_rowHeights.length) {
                return m_rowHeights[row];
        }
        return m_rowHeight;
    }

    
    @Override
    public int getRowHeightMinimum() {
        return 10;
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
    public Point belongsToCell(int column, int row) {
        HeaderNode header = m_arrayTableModel.getHeader();
        int numHeaderRows = header.getRowCount();
        
        if (row < numHeaderRows) {
            if (column == 0) {
                return new Point(0, 0);
            }
            return new Point(header.getParentColumn(column - 1, row) + 1, row);
        } else if (m_isEditable  &&  row == (getRowCount() - 1)  &&  column > 0) {
            return new Point(1, row); // one data cell in the last line with + sign
        }
        return null;
    }

    
    @Override
    public KTableCellEditor getCellEditor(int column, int row) {
        
        if (!m_isEditable) {
            return null;
        }
        
        int modelColumn = column - getFixedHeaderColumnCount();
        int modelRow = row - getFixedHeaderRowCount();
        
        if (modelColumn >= 0  &&  
            modelRow >= 0  &&  modelRow < m_arrayTableModel.getRowCount()) {
            
            ArrayTableCell cellData = m_arrayTableModel.getDataValue(modelColumn, 
                                                                     modelRow);
            // cellData should never be null!
            if (cellData == null  ||  cellData.isReadOnlyCell()) {
                return null;
            }
            
            if (cellData.existsInModel()) {
                
                return getEditorForCell(cellData, modelColumn);
            } else {
                // if cellData does not exist in model, it may still  
                // be editable, if it does not represent empty  
                // CTestBaseList (for example empty stubs list). 
                if (cellData.isTestBaseList()  ||  cellData.isTestBase()) {
                    return null;
                }
                HeaderNode header = m_arrayTableModel.getHeader();
                HeaderNode firstNonEmptyNode = header.getFirstNonEmptyCellBottomUp(modelColumn);

                if (firstNonEmptyNode != null) {
                    if (firstNonEmptyNode.isLeafNode()) {
                        return getEditorForCell(cellData, modelColumn);
                    } else if (firstNonEmptyNode.isUserSequenceNode()  ||  firstNonEmptyNode.isUserMappingNode()) {
                        return m_emptyCellEditor;
                    }
                }
            }
            
            StatusView.getView().setDetailPaneText(StatusType.WARNING,
                                      "The cell is not editable - please add list item first.\n" +
                                      "Click '+' in header cell or select the cell and press 'Ctrl - num +'.");
        }
        
        if (row < getFixedHeaderRowCount()) {
            HeaderNode header = m_arrayTableModel.getHeader();
            HeaderNode node = header.getNode(modelColumn, row);
            if (node != null  &&  node.isLeafNode()) {
                if (node.getParent().isUserMappingNode()) {
                    // only user mapping leaf nodes are editable - to enable in-table
                    // renaming of variable names
                    return getTextEditorWProposals(modelColumn);
                }
            }
        }
        
        return null;
    }


    private KTableCellEditor getEditorForCell(ArrayTableCell cellData, int modelColumn) {
        
        switch (cellData.getEditorType()) {
        case ECombo:
        	if (OsUtils.isLinux()) {
        		m_comboEditorLinux.setItems(cellData.getComboItems());
                return m_comboEditorLinux;
        	} else {
        		m_comboEditorWin.setItems(cellData.getComboItems(), 
		                  cellData.getDefaultEnumValue());
                return m_comboEditorWin;
        	}
        case EText:
            break;
        case ETristate:
            return m_tristateEditor;
        default:
            break;
        }
        
        return getTextEditorWProposals(modelColumn);
    }

    
    @Override
    public KTableCellRenderer getCellRenderer(int column, int row) {
        
        HeaderNode header = m_arrayTableModel.getHeader();
        int numHeaderRows = header.getRowCount();
        
        int fixedHeaderColumnCount = getFixedHeaderColumnCount();
        
        if (row < numHeaderRows  &&  column >= fixedHeaderColumnCount) {
            HeaderNode node = header.getNode(column - fixedHeaderColumnCount, row);
            
            if (node != null  &&  node.isLeafNode()) {
                HeaderNode parent = node.getParent();
                if (parent.isUserMappingNode()) {
                    // System.out.println("childrenType = " + (column - 1) + " / " + row);
                    return m_userMappingHeaderRenderer;
                } else if (parent.isUserSequenceNode()) {
                    return m_sequenceHeaderRenderer;
                }  
            }
            return m_headerRenderer;
        } 
        
        if (column < fixedHeaderColumnCount) {
            return m_headerRenderer;
        }

        row -= numHeaderRows;
        column -= fixedHeaderColumnCount;
        ArrayTableCell cellData = null;
        
        if (row < m_arrayTableModel.getRowCount()) {
            cellData = m_arrayTableModel.getDataValue(column, row);
        }

        if (cellData != null  &&  cellData.existsInModel()) {
            switch (cellData.getEditorType()) {
            case ECombo:
            case EText:
                break;
            case ETristate:
                return m_tristateCellRenderer;
            default:
                break;
            
            }
        }
        return m_textIconsCellRenderer;
    }

    
    @Override
    public void setColumnWidth(int column, int width) {
        // System.out.println("width: " + column);
        m_columnWidths[column] = width;
    }

    
    @Override
    public void setContentAt(int column, int row, Object value) {
        int numHeaderRows = m_arrayTableModel.getHeader().getRowCount();
        int modelColumn = column - getFixedHeaderColumnCount();
        
        if (row >= numHeaderRows  &&  column > 0) {
            
            try {
                int dataRow = row - numHeaderRows;
                String[] valAndComments = m_arrayTableModel.getComments(column - 1, 
                                                                        dataRow);
                AbstractAction action = m_arrayTableModel.createSetContentAtAction(column - 1, 
                                                                                   dataRow, 
                                                                                   value.toString(),
                                                                                   valAndComments[0],
                                                                                   valAndComments[1]);
                if (action.isModified()) {
                    execAction(action);
                }
                
            } catch (Exception ex) {
                // may not open a dialog here, because then KTable editor can not close
                // properly - exception is thrown. 
                // SExceptionDialog.open(Activator.getShell(), "Can not set value!", ex);
                System.out.println(SEFormatter.getInfoWithStackTrace(ex, 10));
                // ex.printStackTrace();
            }
        } else if (m_arrayTableModel.isHeaderUserMappingKey(modelColumn, row)) {
            GroupAction action = m_arrayTableModel.createRenameUserMappingKeyAction(modelColumn, 
                                                                                    (String)value);
            execAction(action);
            m_arrayTableModel.refresh();
        }
    }

    
    @Override
    public void setRowHeight(int arg0, int arg1) {
    }


    public ArrayTableModel getArrayModel() {
        return m_arrayTableModel;
    }
    
    
    public HeaderPath getHeaderPath(int dataCol) {
        
        HeaderNode rootHeaderNode = m_arrayTableModel.getHeader();
        HeaderNode leafHeader = rootHeaderNode.getFirstNonEmptyCellBottomUp(dataCol);
        
        if (leafHeader != null) {
            return leafHeader.getAbstractPath();
        }
        
        return null;
    }
    
    
    public CTestBase getTestBase(int dataRow) {
        return m_arrayTableModel.getTestBase(dataRow);
    }
    

    /** 
     * This method is specific for CTestSpecification, which can be merged
     * and therefore needs optimization. This method clears cached data to
     * save memory.
     * 
     * @param testSpec
     */
    private void clearCachedMergedTestSpecs(CTestSpecification testSpec) {
        testSpec.setCachedMergedTestSpec(null);
        int noOfDerived = testSpec.getNoOfDerivedSpecs();
        for (int idx = 0; idx < noOfDerived; idx++) {
            CTestSpecification derivedTs = testSpec.getDerivedTestSpec(idx);
            derivedTs.setCachedMergedTestSpec(null);
        }
    }


    /** 
     * This method is specific for CTestSpecification, which can be merged
     * and therefore needs optimization. Call this method always, when model
     * structure changes, for example column is added/removed.
     * @param testSpec
     */
    private void cacheMergedTestSpecs(CTestSpecification testSpec) {

        testSpec.setCachedMergedTestSpec(testSpec.merge());
        int noOfDerived = testSpec.getNoOfDerivedSpecs();
        for (int idx = 0; idx < noOfDerived; idx++) {
            CTestSpecification derivedTs = testSpec.getDerivedTestSpec(idx);
            derivedTs.setCachedMergedTestSpec(derivedTs.merge());
        }
    }

    
    /** Content providers are defined per section name. If two CTestBase
     * classes have the same section name, they have to share content 
     * providers. If this will become a problem, we'll have to define 
     * content providers with path from root test base, to the most bottom
     * contained test base, for example analyzer/coverage/stats/lineCoverage.
     */
    private KTableCellEditor getTextEditorWProposals(int column) {
        

        if (m_isEditable) {

            String sectionName = m_arrayTableModel.getSectionName(column);
            //System.out.println("section name = " + sectionName);
            
            IContentProposalProvider contentProposalProvider = m_contentProposalsMap.get(sectionName);
        
            if (contentProposalProvider != null) { // not all sections may have content provider
                ContentProposalConfig cfg = new ContentProposalConfig(new String[0]);
                UiUtils.setContentProposalsConfig(cfg);
                cfg.setProposalProvider(contentProposalProvider);
                cfg.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);

                return new KTableCellEditorText2(cfg);
            }
        }
        
        return m_textCellEditor;  // default cell editor without content proposals
    }

    
    /** Creates action, which will delete rows given in selection. */
    public GroupAction createRemoveRowsAction(Point [] selection) {
        GroupAction removeAction = new GroupAction("Remove rows");

        // remove rows starting at largest indices, because positions
        // of row change after removal of rows with lower indices
        int rows[] = new int[selection.length];
        int idx = 0;
        for (Point pt : selection) {
            rows[idx++] = pt.y - getFixedHeaderRowCount();
        }           

        Arrays.sort(rows);
        
        for (int i = rows.length - 1; i >= 0 ; i--) {
            AbstractAction action = m_arrayTableModel.createRemoveTestBaseAction(rows[i]);
            removeAction.add(action);
        }
        return removeAction;
    }


    public void setSelectionProvider(TestBaseTableSelectionProvider selProvider) {
        m_selectionProvider = selProvider;
    }


    /**
     * @return the selectionProvider
     */
    public TestBaseTableSelectionProvider getSelectionProvider() {
        return m_selectionProvider;
    }


//    public String[] getComments(int col, int row) {
//        
//        col = col - getFixedHeaderColumnCount();
//        row = row - getFixedHeaderRowCount();
//        
//        if (col >= 0  &&  col < m_arrayTableModel.getColumnCount()  &&
//            row >= 0  &&  row < m_arrayTableModel.getRowCount()) {
//            
//            ArrayTableCell cell = m_arrayTableModel.getDataValue(col, row);
//            return cell.getComments();
//        }
//        
//        return new String[]{"", ""};
//    }
//    
    
    @Override
    public AbstractAction createSetContentAction(int col, int row, String value, String nlComment, String eolComment) {

        col = col - getFixedHeaderColumnCount();
        row = row - getFixedHeaderRowCount();
        
        if (col >= 0  &&  col < m_arrayTableModel.getColumnCount()  &&
            row >= 0  &&  row < m_arrayTableModel.getRowCount()) {
            AbstractAction action = m_arrayTableModel.createSetContentAtAction(col, 
                                                                               row, 
                                                                               value,
                                                                               nlComment,
                                                                               eolComment);
            return action;
        }
        
        throw new SIndexOutOfBoundsException("Cell index out of bounds.")
        .add("col", col)
        .add("row", row);
    }
    

    @Override
    public void createSetCommentAction(int col, int row, 
                                      String nlComment, String eolComment,
                                      KTable table) {
        col = col - getFixedHeaderColumnCount();
        row = row - getFixedHeaderRowCount();
        
        if (col >= 0  &&  col < m_arrayTableModel.getColumnCount()  &&
            row >= 0  &&  row < m_arrayTableModel.getRowCount()) {
            AbstractAction action = m_arrayTableModel.createSetCommentAction(col, row, nlComment, eolComment);
            execActionAndRefresh(action, table);
            // TestSpecificationModel.getInstance().setSectionEditorDirty(true);
        }
    }


    /**
     * @param mainTooltip the mainTooltip to set
     */
    public void setMainTooltip(String mainTooltip) {
        m_mainTooltip = mainTooltip;
    }


    /**
     * Sets header tooltips. If there are merged cells in header, this array 
     * must contain elements also for merged cells, but may be empty.
     */
    public void setHeaderTooltips(String[][] headerTooltips) {
        m_headerTooltips = headerTooltips;
    }
    
    
    public void setEditable(boolean isEditable) {
        m_isEditable = isEditable;        
    }
    
    
    public boolean isCellEditable(int column, int row) {

        if (!m_isEditable) {
            return false;
        }

        int dataColumn = column - getFixedHeaderColumnCount();
        int dataRow = row - getFixedHeaderRowCount();

        if (dataColumn >= 0  &&  
                dataRow >= 0  &&  dataRow < m_arrayTableModel.getRowCount()) {

            ArrayTableCell cellData = m_arrayTableModel.getDataValue(dataColumn, 
                                                                     dataRow);
            if (cellData == null  ||  cellData.isReadOnlyCell()) {
                return false;
            }
        }

        return true;
    }
    
    
    /**
     * @param isEditable if true, then Up/Down and Add/Remove icons are shown
     * in the first (header) column. Set it to false, when the list is just view
     * of CTestBase-s from other lists
     */
    public void setTestBaseListEditable(boolean isEditable) {
        m_isTestBaseListEditable = isEditable;        
    }
    
    
    public void setActionExecutioner(IActionExecutioner actionExecutioner) {
        m_actionExecutioner = actionExecutioner;
    }


    public void execAction(AbstractAction action) {
        
        if (m_actionExecutioner != null) {
        
            m_actionExecutioner.execAction(action);
            
            if (m_modelChangedListener != null) {
                m_modelChangedListener.modelChanged();
            }
        }
    }
    
    
    /** Redraws table after action is executed, model is not refreshed. */
    public void execActionAndRedraw(AbstractAction action, KTable table) {

        if (action == null) {
            return;
        }

        execAction(action);
        table.redraw();
    }
    
    
    /** Refreshes model and redraws table after the action is executed. */
    public void execActionAndRefresh(AbstractAction action, KTable table) {
        
        if (action == null) {
            return;
        }
        
        execAction(action);
        
        // performance monitoring
        /*
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            model.getArrayModel().refresh(); // model has changed, so a refresh is needed
            model.init();
        }
        System.out.println("time = " + (System.currentTimeMillis() - start));
        */
        refreshStructureChange(table);
    }
}


/**
 * This editor is used when user double clicks on a cell in user mapping or 
 * sequence, which does not contain any column with index or variable. Column with 
 * index (for seq. nodes) or name (mapping) is added automatically by this class.   
 * @author markok
 *
 */
class EmptyCellEditor extends KTableCellEditorText2 {

    

    private InsertColumnHandler m_handler;
    private KTable m_table;
    private boolean m_isClosed;


    @Override
    protected Control createControl() {
        m_isClosed = false;
        m_handler = new InsertColumnHandler();
        m_table = m_handler.getKTable();
        return super.createControl();
    } 

    @Override
    public void setContent(Object content) {
        super.setContent(content);
    }
    
    
    @Override
    public void close(boolean save) {
        if (save  &&  !m_isClosed) {
            
            // this method may be called twice - once on ENTER and the second time 
            // on focus lost. To be even more tricy, focusLost event is triggered when 
            // dialog for entering user mapping var name opend in insertColumn() below,
            // so this method must be reentrant.
            m_isClosed = true;
            
            int numAddedHeaderRows = m_handler.insertColumn(m_table, false);
            if (numAddedHeaderRows > 0) {
                m_Row += numAddedHeaderRows;
            }
            super.close(save);
        }
    }
    
}