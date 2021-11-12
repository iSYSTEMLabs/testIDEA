package si.isystem.tbltableeditor;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.WorkbenchPart;

import de.kupzog.ktable.ICommandListener;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.connect.CTestBase;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.handlers.DeleteColumnHandler;
import si.isystem.tbltableeditor.handlers.DeleteRowHandler;
import si.isystem.tbltableeditor.handlers.InsertColumnRightHandler;
import si.isystem.tbltableeditor.handlers.InsertRowAboveHandler;

/**
 * This class configures KTable and corresponding model. 
 * 
 * TODO: Optimizations:
 * 1. Currently ArrayTableCells are created for all table 
 *    cells, which may be problem for large tables, for example when thousands
 *    of complete test specifications are shown. KTable only renders visible cells,
 *    so many ArrayTableCells are created and never used. 
 *    Solution: Create ArrayTableCell instance or maybe even directly TextIconsContent
 *    only when content for a cell is required.
 * 2. Column widths are calculated by walking all table cells for each refresh. 
 *    Solution: Move column width calculation to new setInput() method, which
 *    performs this only when new model is set. Add also 'refresh' button to 
 *    table (maybe to cell at (0, 0)), and 'Auto-adjust column width' to column 
 *    context menu.
 * @author markok
 */
public class TestBaseListTable {

    enum EMoveType {EOneUp, EOneDown, EAskUser};

    private KTable m_kTable;
    private TestBaseListModel m_kModel;
    private IResultProvider m_testResultsProvider;
    private boolean m_isEditable = true;
    private TableMouseListener m_mouseListener;
    private IActionExecutioner m_actionExecutioner;
    private ISysModelChangedListener m_modelChangedListener;
    private Map<String, int[]> m_visibleSections;
    private boolean m_isEditingOutlineTree = false;
    private boolean m_isOptimizeForInheritance;
    
    public TestBaseListTable(IActionExecutioner actionExecutioner,
                             boolean isEditingOutlineTree) {
        m_actionExecutioner = actionExecutioner;
        m_isEditingOutlineTree = isEditingOutlineTree;
    }
    
    
    /**
     * 
     * @param isShowTooltips if true, then tooltips are shown, but they must be 
     *                        provided by the model.
     * @see #createControl(Composite, CTestBase, int, ENodeId, WorkbenchPart, boolean)
     */
    public Control createControl(Composite parent, CTestBase testBase, 
                                 int section, ENodeId nodeId, WorkbenchPart viewPart) {
        return createControl(parent, testBase, section, nodeId, viewPart, false);
    }
    
    
    /**
     * Creates table, which is initialized with contents of the CTestBaseList
     * elements from the given section of the given CTestBase. 
     * 
     * @param parent
     * @param testBase parent of the list, which contains elements to show 
     *                 in the table.
     * @param section is of the section in testBase, which contains the list
     * @param nodeId id of the tree editor node, which contains this table (used 
     *               for refresh)
     * @param viewPart used to register context menu. May be null.
     * 
     * @return control panel, which contains the table.
     */
    public Control createControl(Composite parent, CTestBase testBase, 
                                 int section, ENodeId nodeId, WorkbenchPart viewPart,
                                 boolean isShowTooltips) {
        
        Composite tablePanel = new Composite(parent, SWT.NONE);
        tablePanel.setLayout(new FillLayout());
        tablePanel.setLayoutData("push, grow, split, span, gapright 10, wmin 0, hmin 0, hmax 95%, wrap");
        
        m_kTable = new KTable(tablePanel, isShowTooltips, 
                              SWT.MULTI | SWT.V_SCROLL 
                              | SWT.H_SCROLL | SWTX.FILL_WITH_DUMMYCOL | SWTX.EDIT_ON_KEY |
                              SWTX.MARK_FOCUS_HEADERS);
        
        m_kModel = new TestBaseListModel(m_kTable, m_actionExecutioner,
                                         nodeId, m_isEditingOutlineTree);
        m_kModel.setOptimizeForInheritance(m_isOptimizeForInheritance);
        m_kModel.setModelData(testBase, section);
        m_kModel.setVisibleSections(m_visibleSections);
        m_kModel.setResultProvider(m_testResultsProvider);
        m_kModel.setModelChangedListener(m_modelChangedListener);
        m_kModel.init();
        
        
        m_kTable.setModel(m_kModel);
        m_kModel.setEditable(m_isEditable);
        
        m_kTable.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                         SWTBotConstants.BOT_TEST_KTABLE);
        
        TestBaseTableSelectionProvider selProvider = new TestBaseTableSelectionProvider();
        m_kModel.setSelectionProvider(selProvider);
        m_kTable.addCellSelectionListener(new CellSelectionListener(m_kTable, selProvider));
        if (viewPart != null) {
            MenuManager menuManager = new MenuManager();
            Menu menu = menuManager.createContextMenu(m_kTable);
            m_kTable.setMenu(menu);
            viewPart.getSite().registerContextMenu("si.isystem.itest.testBaseTableCtxMenu", 
                                                   menuManager, 
                                                   selProvider);
        }
        
        m_kTable.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        
        m_mouseListener = new TableMouseListener(m_kTable);
        m_mouseListener.setEditable(m_isEditable);
        
        m_kTable.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                         SWTBotConstants.BOT_EDITOR_TREE_ID);

        m_kTable.setCommandListener(new CommandListener());
        
        return tablePanel;
    }

    
    public TestBaseListModel getKModel() {
        return m_kModel;
    }


    /**
     * Sets sections, which should be shown in the table. 
     * @param visibleSections
     */
    public void setVisibleSections(Map<String, int []> visibleSections) {
        m_visibleSections = visibleSections;
        m_kModel.setVisibleSections(m_visibleSections);
    }

    
    public void setVisibleSections(String sectionPath, int[] sections) {
        m_visibleSections.put(sectionPath, sections);
    }
    
    
    /**
     * Refresh with new test base.
     * 
     * @param testBase
     * @param section
     */
    public void setInput(CTestBase testBase, int section) {
        m_kModel.setModelData(testBase, section);
        m_kModel.init(); 
        m_kTable.redraw();        
    }
    
    /**
     * This method was created to be able to show the parent test base in 
     * exported tables.
     *      
     * It is caller's responsibility to provide the second test base 
     * of the same derived type as test bases in the list of the first 
     * test base.
     * 
     * * @param testBase test base which contains list of CTestBase objects
     * @param section saction in testBase which contains list of CTestBase objects to show
     * @param firstRow test base to show in the first row
     */
    public void setInput(CTestBase testBase, int section, CTestBase firstRow) {
        m_kModel.setModelData(testBase, section, firstRow);
        m_kModel.init(); 
        m_kTable.redraw();        
    }
    
    
    /**
     * Refresh with existing data classes but different contents.
     */
    public void refresh() {
        m_kModel.refreshStructureChange(m_kTable);
    }
    
    
    public void refreshStructure() {
        m_kModel.refreshStructureChange(m_kTable);        
    }
    
    
    public void setEnabled(boolean isEnabled) {
        m_kTable.setEnabled(isEnabled);
        if (!isEnabled) {
            // clear selection on disabled table - it gives wrong visual  
            // impression that selected cell is editable.
            m_kTable.setSelection(null, true);
        }
    }

    
    public void setTooltip(String tooltip) {
        m_kModel.setMainTooltip(tooltip);
    }


    /**
     * 
     * @param sectionName name of a CTestBase section (iyaml tag name). See also
     *                    doc for getTextCellEditor() in this class. 
     * @param provider content provider
     */
    public void addContentProvider(String sectionName, IContentProposalProvider provider) {
        if (m_kModel != null) {
            m_kModel.addContentProvider(sectionName, provider);
        }
    }
    

    public void setResultsProvider(IResultProvider testResultsProvider) {
        m_testResultsProvider = testResultsProvider;
        if (m_kModel != null) {
            m_kModel.setResultProvider(m_testResultsProvider);
        }
    }


    public void setModelChangedListener(ISysModelChangedListener listener) {
        m_modelChangedListener = listener;
        if (m_kModel != null) {
            m_kModel.setModelChangedListener(m_modelChangedListener);
        }
    }

    
    public void setEditable(boolean isEditable) {
        m_isEditable = isEditable;
        if (m_mouseListener != null) {
            m_mouseListener.setEditable(isEditable);
            m_kModel.setEditable(isEditable);
        }
    }


    /** KTable is derived from Control. */
    public KTable getControl() {
        return m_kTable;
    }


    public void setActionExecutioner(IActionExecutioner actionExecutioner) {
        m_kModel.setActionExecutioner(actionExecutioner);
    }


    public void addVisibleSections(String nodePath, int[] sections) {
        addSections(m_visibleSections, nodePath, sections);
        
    }


    public void removeVisibleSections(String nodePath, int[] sections) {
        removeSections(m_visibleSections, nodePath, sections);
    }
    
    
    public boolean hasVisibleSections(String className) {
        int[] visibleSections = m_visibleSections.get(className);
        
        return visibleSections == null ? false : visibleSections.length > 0;
    }
    
    
    public static void addSections(Map<String, int[]> visibleSectionsMap,
                                   String nodePath,
                                   int[] addedSections) {
        
        int[] visibleSections = visibleSectionsMap.get(nodePath);

        if (visibleSections != null) {
            int[] updatedVisibleSections = DataUtils.appendNewItems(visibleSections, 
                                                                    addedSections);

            if (updatedVisibleSections != null) {
                Arrays.sort(updatedVisibleSections); // sort sections to keep order regardless of clicking sequence
                visibleSectionsMap.put(nodePath, updatedVisibleSections);
            }
        }
    }
    
    
    public static void removeSections(Map<String, int[]> visibleSectionsMap,
                                      String nodePath,
                                      int[] removedSections) {
        
        int[] visibleSections = visibleSectionsMap.get(nodePath);

        if (visibleSections != null) {
            int [] updatedVisibleSections = DataUtils.removeNewItems(visibleSections, 
                                                                     removedSections);

            if (updatedVisibleSections != null) {
                visibleSectionsMap.put(nodePath, updatedVisibleSections);
            }
        }
    }


    public void scrollToVisibleSections(String sectionTreePath) {
        
        int row = m_kTable.getVisibleCells().y;
        int col = m_kModel.getVisibleColumn(sectionTreePath);
        if (col >= 0) {
            m_kTable.scroll(col, row);
        }
    }


    public SetSectionAction toggleInheritance(int col, int row) {
        return m_kModel.toggleInheritance(col, row);
    }


    public GroupAction extrapolate(MutableBoolean isExistReadOnlyCells) {
        
        Point[] selectedCells = getControl().getCellSelection();

        String firstCellStr = getFirstCellString(selectedCells);
        
        if (firstCellStr != null) {
            // copy string in the first cell to all other cells
            GroupAction grpAction = new GroupAction("extrapolate strings");
            for (int idx = 1; idx < selectedCells.length; idx++) {
                int col = selectedCells[idx].x;
                int row = selectedCells[idx].y;
                if (!m_kModel.isCellEditable(col, row)) {
                    isExistReadOnlyCells.setTrue();
                }
                
                AbstractAction action = m_kModel.createSetContentAction(col, 
                                                                        row, 
                                                                        firstCellStr,
                                                                        "", "");
                grpAction.add(action);
            }
            
            return grpAction;
        } 
        
        // extrapolate numbers
        Number num1 = getFirstCellAndCheckRange(selectedCells);
        Number num2 = getNumber(selectedCells[1].x, selectedCells[1].y);
        
        boolean isNum2Empty = false;
        if (num2 == null) {
            num2 = num1; // if only the first cell is defined, create uniform list
            isNum2Empty = true;
        }

        int startIdx = isNum2Empty ? 1 : 2;
        
        if (num1 instanceof Double  ||  num2 instanceof Double) {
            return evaluateRange(startIdx, selectedCells.length, selectedCells, 
                                 num1, num2.doubleValue() - num1.doubleValue(), 
                                 (startVal, step, idx) -> Double.toString(startVal.doubleValue() + step.doubleValue() * idx),
                                 isExistReadOnlyCells);
        } else {
            return evaluateRange(startIdx, selectedCells.length, selectedCells, 
                                 num1, num2.longValue() - num1.longValue(), 
                                 (startVal, step, idx) -> Long.toString(startVal.longValue() + step.longValue() * idx),
                                 isExistReadOnlyCells);
            
        }
    }


    public GroupAction interpolate(MutableBoolean isExistReadOnlyCells) {

        Point[] selectedCells = getControl().getCellSelection();

        Number num1 = getFirstCellAndCheckRange(selectedCells);

        Point lastCell = selectedCells[selectedCells.length - 1];
        Number num2 = getNumber(lastCell.x, lastCell.y);
        
        boolean isNum2Empty = false;
        if (num2 == null) {
            num2 = num1; // if only the first cell is defined, create uniform list
            isNum2Empty = true;
        }

        // step of type double prevents loosing of range because of truncated
        // division
        double stepVal = (num2.doubleValue() - num1.doubleValue()) / (selectedCells.length - 1);

        int endIdx = isNum2Empty ? selectedCells.length : selectedCells.length - 1;
        
        if (num1 instanceof Double  ||  num2 instanceof Double) {
            return evaluateRange(1, endIdx, selectedCells, 
                                 num1, stepVal, 
                                 (startVal, step, idx) -> Double.toString(startVal.doubleValue() + step.doubleValue() * idx),
                                 isExistReadOnlyCells);
        } else {
            return evaluateRange(1, endIdx, selectedCells, 
                                 num1, stepVal, 
                                 (startVal, step, idx) -> Long.toString(startVal.longValue() + (long)Math.round(step.doubleValue() * idx)),
                                 isExistReadOnlyCells);
        }
    }
    
    
    private GroupAction evaluateRange(int startIdx, int endIdx, Point[] selectedCells, 
                                      Number value1, Number step,
                                      RangeOperator evaluator,
                                      MutableBoolean isExistReadOnlyCells) {
        
        GroupAction grpAction = new GroupAction("extrapolate");
        for (int idx = startIdx; idx < endIdx; idx++) {
            int col = selectedCells[idx].x;
            int row = selectedCells[idx].y;
            if (!m_kModel.isCellEditable(col, row)) {
                isExistReadOnlyCells.setTrue();
            }
            
            String newVal = evaluator.apply(value1, step, idx);
            AbstractAction action = m_kModel.createSetContentAction(col, 
                                                                    row, 
                                                                    newVal,
                                                                    "", "");
            grpAction.add(action);
        }
        
        return grpAction;
    }
    
    
    private String getFirstCellString(Point[] selectedCells) {

        checkRange(selectedCells, 2);
        
        Object val = m_kModel.getContentAt(selectedCells[0].x, selectedCells[0].y);
        if (val instanceof TextIconsContent) {
            String valStr = ((TextIconsContent) val).getText();
            if (valStr.isEmpty()) {
                return null;
            }
            if (Character.isDigit(valStr.charAt(0))) {
                return null;
            }
            
            return valStr;
        }        
        
        return null;
    }


    private Number getFirstCellAndCheckRange(Point[] selectedCells) {

        checkRange(selectedCells, 3);

        Number num1 = getNumber(selectedCells[0].x, selectedCells[0].y);
        
        if (num1 == null) {
            throw new SIllegalArgumentException("There must be a number in the first cell!");
        }
        return num1;
    }


    private void checkRange(Point[] selectedCells, int minRequiredCells) {
        
        if (selectedCells.length < minRequiredCells) {
            throw new SIOException(String.format("Please select at least %d table cells.", minRequiredCells));
        }
        
        int col = selectedCells[0].x;
        int row = selectedCells[0].y;
        boolean isColumnRange = true;
        boolean isRowRange = true;
        
        for (Point cell : selectedCells) {
            if (col != cell.x) {
                isColumnRange = false;
            }
            if (row != cell.y) {
                isRowRange = false;
            }
        }
        
        if (!isRowRange  &&  !isColumnRange) {
            throw new SIOException("Please select table cells in row or column, but not both.");
        }
    }


    private Number getNumber(int col, int row) {
        Object val = m_kModel.getContentAt(col, row);
        if (val instanceof TextIconsContent) {
            String valStr = ((TextIconsContent) val).getText();
            if (valStr.isEmpty()) {
                return null;
            }
            if (valStr.contains(".")) {
                return Double.parseDouble(valStr);
            } else {
                return Long.parseLong(valStr);
            }
        } else {
            throw new SIllegalArgumentException("Expected a number, but it is: " + val.toString())
                .add("col", col)
                .add("row", row);
        }
    }
    
    
    /**
     * Size of both input arrays must be the same.
     * 
     * @param selectedCells
     * @param cellContents
     * @param isExistReadOnlyCells
     * @return
     */
    public GroupAction applyCellContent(Point[] selectedCells,
                                        String[] cellContents,
                                        MutableBoolean isExistReadOnlyCells) {
        
        GroupAction grpAction = new GroupAction("extrapolate");
        for (int idx = 0; idx < selectedCells.length; idx++) {
            int col = selectedCells[idx].x;
            int row = selectedCells[idx].y;
            if (!m_kModel.isCellEditable(col, row)) {
                isExistReadOnlyCells.setTrue();
            }
            
            String newVal = cellContents[idx];
            AbstractAction action = m_kModel.createSetContentAction(col, 
                                                                    row, 
                                                                    newVal,
                                                                    "", "");
            grpAction.add(action);
        }
        
        return grpAction;
    }


    public void setOptimizeForInheritance(boolean isOptimizeForInheritance) {
        m_isOptimizeForInheritance = isOptimizeForInheritance;
        if (m_kModel != null) {
            m_kModel.setOptimizeForInheritance(isOptimizeForInheritance);
        }
    }
}


class CommandListener implements ICommandListener {

    @Override
    public void insertColumn() {
        try {
            new InsertColumnRightHandler().execute(null);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void insertRow() {
        try {
            new InsertRowAboveHandler().execute(null);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void deleteColumn() {
        try {
            new DeleteColumnHandler().execute(null);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void deleteRow() {
        try {
            new DeleteRowHandler().execute(null);
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void ctrlPlusPressed() {
        // impl. not needed for tbListTable
    }

    @Override
    public void ctrlMinusPressed() {
        // impl. not needed for tbListTable
    }
}