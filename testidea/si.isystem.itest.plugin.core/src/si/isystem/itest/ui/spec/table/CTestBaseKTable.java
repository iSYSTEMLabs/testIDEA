package si.isystem.itest.ui.spec.table;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.WorkbenchPart;

import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.connect.CTestProfilerStatistics.EProfilerStatisticsSectionId;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStackUsage;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.IntVector;
import si.isystem.exceptions.SIOException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.TSUtils;
import si.isystem.itest.common.ktableutils.KTableEditorModel;
import si.isystem.itest.handlers.ToolsExecCustomScriptCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.run.TestScriptResult;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.xls.TableExporter;
import si.isystem.tbltableeditor.HeaderPath;
import si.isystem.tbltableeditor.SectionActionListener;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.tbltableeditor.SpreadsheetOperation;
import si.isystem.tbltableeditor.TestBaseListModel;
import si.isystem.tbltableeditor.TestBaseListTable;

/**
 * This class wraps table for editing data in dynamic classes derived from
 * CTestBase, and specializes for viewing of CTestSpecification and CTestGroup
 * objects. It also adds result viewer.
 * Columns and rows in that table are configured and set automatically
 * from meta data in the input class (CTestSpecification and CTestGroup).
 *  
 * @author markok
 *
 */
public class CTestBaseKTable {

    private static final int[] EMPTY_SECTIONS = new int[0];

    public static final String STEPS_TABLE_TOOLTIP = 
            "This table shows parent and derived test bases (test cases and groups).\n " +
            "Empty lines are ignored.\n\n" +

            KTableEditorModel.KTABLE_SHORTCUTS_TOOLTIP;

    private static String TEST_SPEC_TESTS_NODE_PATH;
    private static String TEST_GROUP_CHILDREN_NODE_PATH;

    private TestBaseListTable m_tbListTable;
    
    // private CTestTreeNode m_currentTestNode;
    private TestSpecificationModel m_model;

    private CTBTableResultProvider m_testResultsProvider = new CTBTableResultProvider();

    private Map<String, int[]> m_allVisibleSections;
    
    public CTestBaseKTable() {
        CTestSpecification ts = new CTestSpecification();
        CTestGroup grp = new CTestGroup();
        
        TEST_SPEC_TESTS_NODE_PATH = SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                                  ts.getTagName(SectionIds.E_SECTION_TESTS.swigValue());
        
        TEST_GROUP_CHILDREN_NODE_PATH = SectionNames.TEST_GROUP_NODE_PATH + HeaderPath.SEPARATOR + 
                     grp.getTagName(CTestGroup.ESectionCTestGroup.E_SECTION_CHILDREN.swigValue());
    }

    
    public void createControls(Composite parent, 
                               WorkbenchPart viewPart, // used to register context menu
                               ENodeId nodeId) {
        
        parent.setLayoutData("wmin 0, grow");
        m_tbListTable = new TestBaseListTable(m_model, true);
        m_tbListTable.setResultsProvider(m_testResultsProvider);
        m_tbListTable.setOptimizeForInheritance(true);
        
        Control stepsTableControl = m_tbListTable.createControl(parent, 
                                                                new CTestSpecification(),
                                                                SectionIds.E_SECTION_TESTS.swigValue(),
                                                                nodeId,
                                                                viewPart);
        stepsTableControl.setLayoutData("wmin 0, grow");
        
        m_tbListTable.setTooltip(STEPS_TABLE_TOOLTIP);
        m_allVisibleSections = getAllVisibleSections();
        Map<String, int[]> visibleSections = new TreeMap<>();
        
        // create a copy of arrays with section IDs
        for (Map.Entry<String, int[]> pair : m_allVisibleSections.entrySet()) {
            int[] value = pair.getValue();
            visibleSections.put(pair.getKey(), Arrays.copyOf(value, value.length));
        }
        
        visibleSections.put(SectionNames.TEST_SPEC_NODE_PATH, EMPTY_SECTIONS); // nothing is shown initially
        visibleSections.put(SectionNames.ANALYZER_NODE_PATH, EMPTY_SECTIONS);
        visibleSections.put(SectionNames.COVERAGE_NODE_PATH, EMPTY_SECTIONS);
        visibleSections.put(SectionNames.PROFILER_NODE_PATH, EMPTY_SECTIONS); 

        visibleSections.put(SectionNames.TEST_GROUP_NODE_PATH, EMPTY_SECTIONS); 

        m_tbListTable.setVisibleSections(visibleSections);

        SectionActionListener listener = new SectionActionListener() {
            
            @Override
            public void onTestBaseCellModified(AbstractAction action, CTestBase changedTb) {
                action.addAllFireEventTypes();
                action.addTreeChangedEvent(null, changedTb.getContainerTestNode());
            }
        };
        
        m_tbListTable.getKModel().addSectionListener(SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR +
                                                     SectionNames.INSTANCE.getTestSpecSectionName(SectionIds.E_SECTION_ID), 
                                                     listener);
        m_tbListTable.getKModel().addSectionListener(SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR
                                                     + SectionNames.INSTANCE.getTestSpecSectionName(SectionIds.E_SECTION_FUNC)
                                                     + HeaderPath.SEPARATOR + "func", 
                                                     listener);
    }
    

    /**
     * Returns map of (cppClassName, array of sections). This may not be enough, if the same class
     * is used for more than one purpose, but not all sections are used always, for example
     * return value in script functions. However, this should be solved in iyaml model.
     * Another option would be to specify paths (see  HeaderNode.getPath()) for all sections,
     * but then reg ex should be used to compare paths, because lists have indices, for example:
     * //analyzer/profiler/codeAreas/0/netTime  ==> //analyzer/profiler/codeAreas/\d+
     * @return
     */
    private Map<String, int[]> getAllVisibleSections() {

        // define redundant sections 
        CTestSpecification testSpec = new CTestSpecification();
        
        Map<String, int[]> visibleSections = new TreeMap<>();
        getVisibleSections(testSpec, visibleSections, SectionNames.TEST_SPEC_NODE_PATH);

        // remove items, which should never be visible
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH, 
                                         TableExporter.REMOVED_TEST_SPEC_SECTIONS);
        
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH, 
                                         new int[]{SectionIds.E_SECTION_IMPORTS.swigValue()});

        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                                         testSpec.getTagName(SectionIds.E_SECTION_PRE_CONDITION.swigValue()), 
                                         new int[]{CTestAssert.ESectionAssert.E_SECTION_ASSERT_IS_EXPECT_EXCEPTION.swigValue()});

        // deprecated profiler Outside time in Code areas
        TestBaseListTable.removeSections(visibleSections, SectionNames.PROFILER_NODE_PATH
                                         + HeaderPath.SEPARATOR + SectionNames.PROF_CODE_AREA 
                                         + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK,
                                         new int[]{EProfilerStatisticsSectionId.E_SECTION_OUTSIDE_TIME.swigValue()});

        // deprecated profiler Gross and Call times in Data areas
        TestBaseListTable.removeSections(visibleSections, SectionNames.PROFILER_NODE_PATH
                                         + HeaderPath.SEPARATOR + SectionNames.PROF_DATA_AREA
                                         + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK,
                                         new int[]{EProfilerStatisticsSectionId.E_SECTION_GROSS_TIME.swigValue(),
                                                   EProfilerStatisticsSectionId.E_SECTION_CALL_TIME.swigValue()});

        // code profiler has no value item
        TestBaseListTable.removeSections(visibleSections, SectionNames.PROFILER_NODE_PATH 
                                         + HeaderPath.SEPARATOR + SectionNames.PROF_CODE_AREA
                                         + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK,
                                         
                                         new int[]{EProfilerStatisticsSectionId.E_SECTION_AREA_VALUE.swigValue()});
        
        // only max limit is shown in form editor
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR 
                                         + testSpec.getTagName(SectionIds.E_SECTION_STACK_USAGE.swigValue()), 
                                         new int[]{CTestStackUsage.ETestStackUsageSections.E_SECTION_MIN_SIZE.swigValue()});
        
        // script functions have no ret val variable
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                                         testSpec.getTagName(SectionIds.E_SECTION_INIT_TARGET.swigValue()), 
                                         new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                                         testSpec.getTagName(SectionIds.E_SECTION_INITFUNC.swigValue()),
                                         new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                                         testSpec.getTagName(SectionIds.E_SECTION_ENDFUNC.swigValue()), 
                                         new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                                         testSpec.getTagName(SectionIds.E_SECTION_RESTORE_TARGET.swigValue()), 
                                         new int[]{CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue()});
        
        
        // recursively walk CTestGroup tree and fill sections
        CTestGroup grp = new CTestGroup();
        getVisibleSections(grp, visibleSections, SectionNames.TEST_GROUP_NODE_PATH);

        TestBaseListTable.removeSections(visibleSections, SectionNames.TEST_GROUP_NODE_PATH,
                                         new int[]{ESectionCTestGroup.E_SECTION_CHILDREN.swigValue()});
        
        return visibleSections;
    }
    
    
    /** Recursively walks CTestSpecification tree and fills sections. */
    private void getVisibleSections(CTestBase tb, 
                                    Map<String, int[]> visibleSections, 
                                    String nodePath) {

        // System.out.println("getVisibleSections() path = " + nodePath);
        
        IntVector sectionIds = new IntVector();
        tb.getSectionIds(sectionIds);
        
        int[] intSectionIds = DataUtils.intVectorToArray(sectionIds);
        visibleSections.put(nodePath, intSectionIds);
        
        for (int section  : intSectionIds) {
            
            ETestObjType sectionType = tb.getSectionType(section);
            
            if (sectionType == ETestObjType.ETestBase  ||  
                    sectionType == ETestObjType.ETestBaseList) {
                
                String subnodePath = nodePath + HeaderPath.SEPARATOR + tb.getTagName(section);
                CTestBase childTb = tb.createTestBase(section);
                // System.out.println("testSpecSectionPath: " + subnodePath);
                
                if (!subnodePath.equals(TEST_SPEC_TESTS_NODE_PATH)  &&  
                        !subnodePath.equals(TEST_GROUP_CHILDREN_NODE_PATH)) {
                    
                    if (sectionType == ETestObjType.ETestBaseList) {
                        subnodePath = subnodePath + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK;
                    } 
                    getVisibleSections(childTb, visibleSections, subnodePath);
                }
            }
        }
    }
    
    
    /**
     * Adds content provider.
     * 
     * @param sectionName name og iyaml section, for example 'params', 'expect', ...
     * @param provider content proposals provider
     */
    public void addContentProvider(String sectionName, IContentProposalProvider provider) {
        m_tbListTable.addContentProvider(sectionName, provider);
    }
    
    
    /**
     * 
     * @param testBase
     * @param section
     * @param itemId
     * @param isEnableViewResultsButton
     * @param actionExecutioner must be the current model 
     */
    public void setInput(CTestTreeNode testBase, TestSpecificationModel model, 
                         boolean isShowParentTestBase, boolean isTestBaseListEditable) {

        m_model = model;
        m_tbListTable.setActionExecutioner(model);
        m_tbListTable.getKModel().setTestBaseListEditable(isTestBaseListEditable);
        m_testResultsProvider.setInput(m_model, m_tbListTable.getKModel());
        
        // show new input
        int sectionId = CTestSpecification.SectionIds.E_SECTION_TESTS.swigValue();
        if (testBase != null) {
            
            testBase = TSUtils.castToType(testBase);

            if (testBase.isGroup()) {
                sectionId = CTestGroup.ESectionCTestGroup.E_SECTION_CHILDREN.swigValue();
            }
            
            if (isShowParentTestBase) {
                m_tbListTable.setInput(testBase, sectionId, testBase);
            } else {
                m_tbListTable.setInput(testBase, sectionId, null);
            }
        } else {
            m_tbListTable.setInput(new CTestSpecification(), sectionId);
        }
    }


    public void scrollToVisibleColumn(String sectionTreePath, int [] sectionIds) {
        
        if (sectionIds.length == 0) {
            return;
        }
        
        // section editors (MetaEditor, ... ) have paths adapted for map of
        // section indices, because there is no direct mapping of visible
        // tree node and sections shown in this node. For example, Meta and
        // Function section tree nodes show several sections of CTestSpecification.
        if (sectionTreePath == null) {
            sectionTreePath = SectionNames.TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR +
                    SectionNames.TEST_SPEC.getTagName(sectionIds[0]);
        }

        m_tbListTable.scrollToVisibleSections(sectionTreePath); 
    }


    public void setEnabled(boolean isEnabled) {
        m_tbListTable.setEnabled(isEnabled);        
    }
    
    
    public Control getControl() {
        return m_tbListTable.getControl();
    }


    public void restoreVisibleSections(String nodePath) {
        m_tbListTable.setVisibleSections(nodePath, m_allVisibleSections.get(nodePath));
    }
    
    
    public void restoreAllVisibleSections() {
        String[] sectionTreeNodePaths = getMainSectionTreeNodePaths();
        for (String nodePath : sectionTreeNodePaths) {
            restoreVisibleSections(nodePath);
        }
    }
    
    
    public void clearVisibleSections(String nodePath) {
        m_tbListTable.setVisibleSections(nodePath, EMPTY_SECTIONS);
    }
    
    
    private String [] getMainSectionTreeNodePaths() { 
        return new String[]{SectionNames.TEST_SPEC_NODE_PATH,
                            SectionNames.ANALYZER_NODE_PATH,
                            SectionNames.COVERAGE_NODE_PATH,
                            SectionNames.PROFILER_NODE_PATH,
                                                  
                            SectionNames.TEST_GROUP_NODE_PATH};
    }
    
    
    public void clearAllVisibleSections() {

        String[] sectionTreeNodePaths = getMainSectionTreeNodePaths();
        for (String nodePath : sectionTreeNodePaths) {
            clearVisibleSections(nodePath);
        }
    }
    
    
    public void addVisibleSections(String nodePath, int[] sections) {
        m_tbListTable.addVisibleSections(nodePath, sections);
    }
    
    
    public void removeVisibleSections(String nodePath, int[] sections) {
        m_tbListTable.removeVisibleSections(nodePath, sections);
    }
    
    
    public boolean hasVisibleSections(String nodePath) {
        return m_tbListTable.hasVisibleSections(nodePath);
    }
    

    public void refresh() {
        m_tbListTable.refresh();
    }


    public void refreshStructure() {
        m_tbListTable.refreshStructure();
        
    }


    public void toggleInheritanceOfSelectedSection() {
        Point[] selectedCells = m_tbListTable.getControl().getCellSelection();
        if (selectedCells.length == 0) {
            throw new SIOException("Please select at least one table cell in the section which "
                    + "inheritance you want to change.");
        }
//        if (selectedCells.length > 1) {
//            throw new SIOException("Too many cells selected in table!\nPlease select one "
//                    + "table cell in the section which inheritance you want to change.");
//        }

        GroupAction grpAction = new GroupAction("toggle inheritance group");
        Map<Long, CTestSpecification> changedTestSpecs = new TreeMap<>();
        
        for (Point cell : selectedCells) {
            SetSectionAction action = m_tbListTable.toggleInheritance(cell.x, 
                                                                      cell.y);
            if (action == null) {
                continue;
            }
            
            grpAction.add(action);
            
            // store changed test specs to later refresh cached data. Use Map
            // to avoid duplicates.
            CTestTreeNode testTreeNode = action.getContainerTreeNode();
            if (testTreeNode.isTestSpecification()) {
                CTestSpecification testSpec = CTestSpecification.cast(testTreeNode);
                changedTestSpecs.put(testSpec.hashCodeAsPtr(), testSpec);
            }
        }
        
        // first execute action to modify the data ... 
        m_model.execAction(grpAction);

        // ... then refresh cached data
        for (Entry<Long, CTestSpecification> pair : changedTestSpecs.entrySet()) {
            CTestSpecification testSpec = pair.getValue();
            testSpec.setCachedMergedTestSpec(testSpec.merge());
        }
        
        // refresh the table
        m_tbListTable.refresh();
    }


    public void execSpreadsheetOperation(SpreadsheetOperation sop) {
        execSpreadsheetOperation(sop, null, null);
    }
    
        
    public void execSpreadsheetOperation(SpreadsheetOperation sop, Point[]cells, String[] contents) {
        
        MutableBoolean isExistReadOnlyCells = new MutableBoolean();
        
        GroupAction grpAction = sop.op(m_tbListTable, cells, contents, isExistReadOnlyCells);
        m_model.execAction(grpAction);
        m_tbListTable.refresh();
        
        if (isExistReadOnlyCells.booleanValue()) {
            MessageDialog.openInformation(Activator.getShell(), "Modification info",
                "Some cells are read only and were not modified. Remove inheritance "
                + "flag to make them writable.");
        }
    }

    
    public void execTableScriptFunc(String scriptMethod) {

        String existingCellContents = getTableDataAsPyMap();
        TestScriptResult scriptResult = 
                ToolsExecCustomScriptCmdHandler.runMethodInExtensionScript(scriptMethod,
                                                                           CScriptConfig.getEXT_METHOD_TABLE_TYPE(),
                                                                           existingCellContents);

        if (scriptResult == null) {
            return;
        }
        
        String newCellContents = StringUtils.join(scriptResult.getFuncInfo(), '\n');
        List<String[]> contentAsTuples = DataUtils.string2Tuples(newCellContents);

        Point[] cells = new Point[contentAsTuples.size()];
        String[] cellContents = new String[contentAsTuples.size()];
        int idx = 0;

        for (String[] tuple : contentAsTuples) {
            int column = Integer.parseInt(tuple[0]);
            int row = Integer.parseInt(tuple[1]);
            cells[idx] = new Point(column, row);
            cellContents[idx] = tuple[2];
            idx++;
        }

        execSpreadsheetOperation((tblTable, selectedCells, contents, isExistReadOnlyCells) -> 
                                     tblTable.applyCellContent(selectedCells, contents, isExistReadOnlyCells), 
                                 cells, cellContents);
    }

    
    private String getTableDataAsPyMap() {
        
        Point[] selectedCells = m_tbListTable.getControl().getCellSelection();
        TestBaseListModel kModel = m_tbListTable.getKModel();
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (Point cell : selectedCells) {
            
            TextIconsContent cellContentObj = 
                         (TextIconsContent)kModel.getContentAt(cell.x, cell.y);
            
            String cellContent = cellContentObj.getText();
            cellContent = cellContent.replaceAll("'", "\\\\'");
            
            sb.append('(').append(cell.x).append(',').append(cell.y)
              .append(",'")
              .append(cellContent)
              .append("'),");
        }
        sb.append("]");
        
        return sb.toString();
    }
}
