package si.isystem.tbltableeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;

import de.kupzog.ktable.editors.CellEditorComboBox;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestImportSources;
import si.isystem.connect.CTestImportSources.ESectionSources;
import si.isystem.connect.CTestImports;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.IntVector;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.itest.model.actions.mapping.RemoveFromUserMappingAction;
import si.isystem.itest.model.actions.sequence.InsertToSequenceAction;
import si.isystem.itest.model.actions.sequence.RemoveFromSeqAction;
import si.isystem.itest.model.actions.sequence.SetSequenceItemAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.model.actions.testBaseList.InsertToTestBaseListAction;
import si.isystem.itest.model.actions.testBaseList.RemoveFromTestBaseListAction;
import si.isystem.itest.model.actions.testBaseList.SwapTestBaseListItemsAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;


/**
 * This table model takes CTestBaseList from CTestBase as a data source, and 
 * transforms it in a
 * way to get one row per one list element. Since list elements may be 
 * hierarchical structures, this structure is reflected in the table header.
 * 
 * This class supports vertical and horizontal orientation (list elements in
 * rows and list elements in columns).
 *
 * See unit tests for usage examples.
 *
 * Column and row indices always refer to data portion of the table, they never 
 * include header columns or rows. 
 *  
 * @author markok
 *
 */
public class ArrayTableModel {

    private HeaderNode m_header;
    private List<ArrayTableCell[]> m_data; // List<rows>, where row = List<String>
    private enum EOrientation {EHeaderTop,  // header items define columns, data is added to rows 
                               EHEaderLeft  // header items define rows, data is added to columns
                              };
    private EOrientation m_orientation = EOrientation.EHeaderTop;
    private ENodeId m_nodeId;
    private CTestBase m_containerTestBase;
    private CTestBase m_firstRowTestBase = null; // if not null, it is shown in the first table row
    private int m_sectionId;
    private CTestBaseList m_testBaseList; // member of m_containerTestBase 
    private CTestBaseList m_viewTestBaseList; // may be a copy without parent, when composed of 
                                              // of test bases not in the same list
    private boolean m_isEditingOutlineTree = false;
    
    // maps, which control visibility of sections. Deprecated sections should never be visible,
    
    // for example. There are also sections, which exist in CTestBaseClass, but is not used
    // in some sections where the class is used, for example CTestFunction is used for script
    // functions, but script functions do not use return value name.
    // The third case is selected/unselected sections in UI tree.
    private Map<String, int[]> m_visibleSections; // preferred and only map to be used. It is 
    // configured by the caller, keys are paths to nodes, for example:
    //     /class isys::CTestSpecification/analyzer/coverage
    // This is much better than name of C++ class only (CAssert is used in expected and preconditions 
    // sections, for example. and preconditions do not have 'Expected Exception' setting.
    
    // Maps used by TableExporter. Will be replaced by m_visibleSections above, 
    // when TableExporter is refactored. 
    private Map<String, int[]> m_unconditionallyRemovedSections;
    private Map<String, int[]> m_removedSectionsIfEmpty;
    private Map<String, int[]> m_orderedSections;

    private Map<String, int[]> m_removedSections; // Used internally, not directly setable by callers.
                                                  // Contains all m_unconditionallyRemovedSections and
                                                  // m_removedSectionsIfEmpty which are empty in 
                                                  // all CTestBases
    // mapping of <sectionPath, listener>
    private Map<String, SectionActionListener> m_sectionListeners;
    
    public ArrayTableModel(ENodeId nodeId, boolean isEditingOutlineTree) {
        m_nodeId = nodeId;
        m_isEditingOutlineTree = isEditingOutlineTree;
    }

    
    /**
     * Creates model for editable table.
     *  
     * @param containterTestBase
     * @param section section of the list to show in table in containterTestBase
     */
    public void setModelData(CTestBase containterTestBase, int section) {
        setModelData(containterTestBase, section, null);
    }

    
    /**
     * Creates model for editable table, the first row contains test base given 
     * as the second parameter. It is caller's responsibility to provide the second
     * test base of the same derived type as test bases in the list of the first 
     * test base.
     * This method was created to be able to show the parent test base in 
     * exported tables, but this functionality is now also used in table editor. 
     *  
     * @param containterTestBase
     * @param section section of the list to show in table in containterTestBase
     */
    public void setModelData(CTestBase containterTestBase, int section, CTestBase firstRowTestBase) {

        m_containerTestBase = containterTestBase;
        m_sectionId = section;
        m_firstRowTestBase = firstRowTestBase;
        
        m_testBaseList = containterTestBase.getTestBaseList(section, false);
        
        if (firstRowTestBase != null) {
            m_viewTestBaseList = new CTestBaseList();
            int listSize = (int) m_testBaseList.size();
            m_viewTestBaseList.add(-1, firstRowTestBase);
            for (int idx = 0; idx < listSize; idx++) {
                m_viewTestBaseList.add(-1, m_testBaseList.get(idx));
            }
        } else {
            m_viewTestBaseList = m_testBaseList;
        }
        
        setModelData(containterTestBase.getClassName()); // by providing class name  
                    // instead of section ID we make sure paths of different classes do not overlap
                    // This is important for filtering of section IDs (see m_visibleSections).
    }

    
    /**
     * Use the method above, otherwise some actions, like creating
     * new list elements, is not possible. Use this method for non-editable tables
     * only.
     * 
     * @param testBaseList contains list of CTestBase classes of the same type.
     * Each class will get one row  of values data. Header will show hierarchical
     * structure of all classes in the list. If some class does not have all values set,
     * they will be represented as empty cells in table rows.
     * 
     * @param parentId name of parent, which contains testBaseList. Currently this
     *                 is used to make sure that paths from CTestGroup and CTestSpecification
     *                 do not accidentally overlap.
     */
    private void setModelData(String parentId) {
        // System.out.println("ArrayTableModel.setModelData()");
        
        m_removedSections = composeRemovedSections(m_viewTestBaseList);
        
        m_header = new HeaderNode(ETestObjType.ETestBase, parentId, m_nodeId);
        
        testBaseListToHeaderNodes(m_viewTestBaseList, m_header);
        
        testBaseListToDataValues(m_viewTestBaseList);
    }
    
    
    CTestBase getModelData() {
        return m_containerTestBase;
    }
    
    
    /**
     * Filters can be used to hide some sections. Usually we specify only one of the
     * parameters, and set the other one as null. 
     * 
     * This method MUST be called before method setModelData().
     * 
     * @param unconditionallyRemovedSections sections to be removed unconditionally, for
     *                                       example deprecated sections
     *                                       
     * @param customRemovedSectionsIfEmpty these sections are not visible only if all CTestBases
     *                                     have this section empty. This is important for
     *                                     example for Excel exports, because no data may be lost
     *                                     on export/import operation.
     *                                     
     * @param orderedSections order of sections to be shown, may be null
     */
    public void setSubtractiveFilters(Map<String, int []> unconditionallyRemovedSections, 
                                      Map<String, int []> customRemovedSectionsIfEmpty,
                                      Map<String, int []> orderedSections) {
        
        m_unconditionallyRemovedSections = unconditionallyRemovedSections;
        m_removedSectionsIfEmpty = customRemovedSectionsIfEmpty;
        m_orderedSections = orderedSections;
    }
    
    
    /**
     * Sets sections, which should be shown in the table. This method overrides settings set with
     * setSubtractiveFilters() - if visibleSections != null, then subtractive filters are ignored.
     * @param visibleSections mapping with keys names of CTestBase classes, and values int[] of sections
     *                        for the given class.
     */
    public void setVisibleSections(Map<String, int []> visibleSections) {
        m_visibleSections = visibleSections;
    }
    
    
    public void addSectionListener(String sectionPath, SectionActionListener listener) {
        
        if (m_sectionListeners == null) {
            m_sectionListeners = new TreeMap<>();
        }
        
        m_sectionListeners.put(sectionPath, listener);
    }
    
    
    /**
     * Removes sections which do not match filter - this way it is possible to show tables
     * with some columns hidden.
     * 
     * @param testBaseList used to find non-empty sections in test bases
     * 
     */
    private Map<String, int[]> composeRemovedSections(CTestBaseList testBaseList) {

        if (m_removedSectionsIfEmpty == null) {
            return m_unconditionallyRemovedSections;
        }
        
        
        // check for non-empty sections in all test bases and invalidate their removal
        
        final int INVALID_SECTION_ID = -1;
        
        int[] customRemovedSections = m_removedSectionsIfEmpty.get(""); // test only top level sections
        
        int numTestBases = (int) testBaseList.size();
        for (int tbIdx = 0; tbIdx < numTestBases; tbIdx++) {
            
            CTestBase tb = testBaseList.get(tbIdx);
            for (int idx = 0; idx < customRemovedSections.length; idx ++) {
                
                int sectionId = customRemovedSections[idx];
                
                if (sectionId != INVALID_SECTION_ID) {
                    
                    if (!tb.isSectionEmpty(sectionId)) {
                        // mark as not empty, will not be removed
                        customRemovedSections[idx] = INVALID_SECTION_ID; 
                    }
                }
            }
        }

        
        // merge remaining sections to be removed with uncond. removed sections
        
        int[] unconditionallyRemoved = m_unconditionallyRemovedSections.get("");
        
        // the next line creates a fixed sized list 
        List<Integer> allRemovedSections = Arrays.asList(ArrayUtils.toObject(unconditionallyRemoved));
        // let's create a dynamic sized list
        allRemovedSections = new ArrayList<>(allRemovedSections);
        
        for (int sectionId : customRemovedSections) {
            if (sectionId != INVALID_SECTION_ID) {
                allRemovedSections.add(sectionId);
            }
        }
        
        Map<String, int[]> removedSections = new TreeMap<>(m_unconditionallyRemovedSections);
        removedSections.put(SectionNames.TEST_SPEC.getClassName(), 
                            ArrayUtils.toPrimitive(allRemovedSections.toArray(new Integer[0])));
        return removedSections;
    }


    public void refresh() {
        setModelData(m_containerTestBase, m_sectionId, m_firstRowTestBase);
    }
    
    
    public ArrayTableCell getDataValue(int columnIndex, int rowIndex) {
        ArrayTableCell cellValue = null;
        if (m_orientation == EOrientation.EHeaderTop) {
            cellValue = getDataItem(columnIndex, rowIndex);
        } else {
            cellValue =  getDataItem(rowIndex, columnIndex);
        }
        
        return cellValue; 
    }
    
    
    public int getRootTestBaseSection(int columnIndex) {
        HeaderNode testBaseNode = m_header.getRootTestBaseNode(columnIndex);
        return testBaseNode.getSectionId();
    }
    
    
    /**
     * Returns true, if first row is not member of the same list as other rows.
     * For example, if table shows derived test cases of a test case, and
     * also shows the parent test case in the first row, the parent test case is not
     * member of the list of derived test cases. It can not be moved up and down
     * the list. 
     * @return
     */
    public boolean isFirstRowSpecial() {
        return m_firstRowTestBase != null;
    }
    
    
    public boolean isCellEditable(int columnIndex, int rowIndex) {
        
        ArrayTableCell cell = getDataValue(columnIndex, rowIndex);
        
        if (cell != null  &&  cell.existsInModel()) {
            return true;
        }
        
        // cells of sequences which already contain some items, should also be 
        // editable, although the sequence contains less items then sequences
        // in other test specs.
        HeaderNode node = m_header.getFirstNonEmptyCellBottomUp(columnIndex);
        if (node.isLeafNode()  &&  node.getParent().isUserSequenceNode()) {
            return true;
        }

        return false;
    }
    
    
    String[] getComments(int columnIndex, int rowIndex) {

        ArrayTableCell cell = getDataValue(columnIndex, rowIndex);
        // preserve cell comments
        if (cell != null  &&  cell.existsInModel()) {
            return cell.getComments();
        }

        return new String[]{"", ""};
    }
    
    
    public boolean isHeaderUserMappingKey(int col, int headerRow) {
        
        if (headerRow < m_header.getDepth()) {
            HeaderNode node = m_header.getNode(col, headerRow);
            if (node != null  &&  node.isLeafNode()) {
                if (node.getParent().isUserMappingNode()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    /** Returns the number of data columns, without header columns. */
    public int getColumnCount() {
        if (m_orientation == EOrientation.EHeaderTop) {
            return getNumHeaderCells();
        }
        return getNumDataSets();
    }
    
    
    /** Returns the number of data rows, without header rows. */
    public int getRowCount() {
        if (m_orientation == EOrientation.EHeaderTop) {
            return getNumDataSets();
        }
        return getNumHeaderCells();
    }

    
    public EOrientation getOrientation() {
        return m_orientation;
    }

    
    public HeaderNode getHeader() {
        return m_header;
    }

    
    /**
     * Returns column belonging to path, or -1 if column was not found (for
     * example because it is not visible).
     * 
     * @param sectionTreePath
     * @return
     */
    public int getColumnOfHeaderNode(String sectionTreePath) {
        
        String [] pathNodes = sectionTreePath.split("/");
        HeaderNode hNode = m_header;
        
        // the first item is empty string because of leading '/', the second 
        // should always be top node.
        for (int idx = 2; idx < pathNodes.length; idx++) {
            String pathNode = pathNodes[idx];
            int numChildren = hNode.getNumChildren();
            
            for (int childIdx = 0; childIdx < numChildren; childIdx++) {
                
                HeaderNode child = hNode.getChild(childIdx);
                
                if (child.getName().equals(pathNode)) {
                    hNode = child;
                    break;
                }
            }
        }
        
        // return data column index of the last node in its parent 
        HeaderNode parentNode = hNode.getParent();
        if (parentNode == null) {
            return -1;
        }
        return parentNode.getChildColumnIndex(pathNodes[pathNodes.length - 1]);
    }

    
    /**
     * Returns name of section (iyaml tag name) for the given column. If it is
     * a sequence or map, then it does not return mapping key or index, but
     * name of iyaml mapping section (iyaml tag).
     * 
     * @param column column in data model, in range [0..numCols)
     * @return
     */
    public String getSectionName(int columnIndex) {
        
        HeaderNode node = m_header.getFirstNonEmptyCellBottomUp(columnIndex);
        HeaderNode parent = node.getParent();
        
        if (parent != null) {
            if (parent.isUserMappingNode() || parent.isUserSequenceNode()) {
                return parent.getName();
            }
            
            return node.getName();
        }
        
        return "";
    }
    
    
    public void setOrientation(EOrientation orientation) {
        m_orientation = orientation;
    }

    
    public void tbList2DataCells() {
        testBaseListToDataValues(m_viewTestBaseList);
    }


    private void testBaseListToHeaderNodes(CTestBaseList testBaseList,
                                           HeaderNode currentHNode) {
        // add the empty CTestBaseObject to serve as a template when the list is empty
        
        CTestBase tb = m_containerTestBase.createTestBase(m_sectionId);
        testBaseToHeaderNode(tb, currentHNode);
        
        // Merge members of classes in the list to one hierarchical header. 
        // Walk over all test bases since length of user lists (seq, mapping, test base)
        // may not be the same in all test bases.
        int size = (int)testBaseList.size();
        for (int idx = 0; idx < size; idx++) {
            tb = testBaseList.get(idx);
            testBaseToHeaderNode(tb, currentHNode);
        }
    }
    
    
    private void testBaseToHeaderNode(CTestBase tb, HeaderNode currentHNode) {

        if (tb.isTestSpecification()) {
            // header should be created for merged test cases, as it may contain
            // more columns than non-merged ones.
            CTestSpecification ts = CTestSpecification.cast(tb);
            CTestBase mergedTs = ts.getCachedMergedTestSpec();
            if (mergedTs != null) {
                tb = mergedTs;
            }
        }
        
        HeaderNode childNode = null;
        // System.out.println("--> HeaderNodePath --> " + currentHNode.getPath());
        int [] sections = getFilteredAndSortedSections(tb, currentHNode);

        for (int sectionId : sections) {
            
            String sectionName = tb.getTagName(sectionId);
            ETestObjType sectionType = tb.getSectionType(sectionId);
            childNode = currentHNode.add(sectionName, sectionId, sectionType, m_nodeId);

            switch (sectionType) {

            case EYAMLBool:
                boolean defaultBool = tb.getDefaultValue(sectionId) == 1 ? true : false;
                childNode.setDefaultBool(defaultBool);
                break;
            case EYAMLEnum:
                StrVector enumValues = new StrVector();
                String defaultValue = tb.getEnumValues(sectionId, enumValues);
                enumValues.add(0, CellEditorComboBox.decorateDefaultValue(defaultValue));
                String[] optionItems = DataUtils.strVector2StringArray(enumValues);
                childNode.setEnumItems(optionItems, defaultValue);
                break;
            case EYAMLScalar:
                break;

            case EYAMLMap:
                CMapAdapter map = new CMapAdapter(tb, sectionId, true);
                StrVector keys = new StrVector();
                map.getKeys(keys);
                
                int numKeys = (int)keys.size();
                for (int keyIdx = 0; keyIdx < numKeys; keyIdx++) {
                    String key = keys.get(keyIdx);
                    // mapping items refer to the same section as parent sequence 
                    // object
                    childNode.add(key, sectionId, ETestObjType.EYAMLScalar, m_nodeId);
                }
                
                childNode.recordKeyOrder(keys);
                
                break;

            case EYAMLSeqence:
                CSequenceAdapter seq = new CSequenceAdapter(tb, sectionId, true);
                // add number of nodes so that it matches the longest one of all instances
                for (int seqIdx = childNode.getNumChildren(); seqIdx < seq.size(); seqIdx++) {
                    childNode.add(String.valueOf(seqIdx), sectionId, // seq. items refer
                                  // to the same section as parent sequence object
                                  ETestObjType.EYAMLScalar, m_nodeId);
                }
                break;

            case ETestBase:
                CTestBase childTb = tb.getTestBase(sectionId, true);
                // childNode.setAllowedChildren(tagNames); 
                testBaseToHeaderNode(childTb, childNode);
                break;
                
            case ETestBaseList:
                CTestBaseList tbList = tb.getTestBaseList(sectionId, true);
                for (int i = 0; i < tbList.size(); i++) {
                   HeaderNode arrayElemNode = 
                           childNode.add(String.valueOf(i), 
                                         sectionId, // seq. items refer
                                         // to the same section as parent sequence object
                                         ETestObjType.ETestBase, m_nodeId);
                   testBaseToHeaderNode(tbList.get(i), arrayElemNode);
                }
                break;
            default:
                throw new IllegalStateException("Internal error: Invalid text section ID: " + sectionType);
            }
        }
    }


    private int [] getFilteredAndSortedSections(CTestBase testBase,
                                                HeaderNode currentHNode) {
        int [] sections = null;
        
        if (m_visibleSections != null) {
            
            sections = m_visibleSections.get(currentHNode.getPath());
            
        } else {
            
            IntVector sectionsVec = new IntVector(); 
            testBase.getSectionIds(sectionsVec);
            
            String nodeName = currentHNode.getName();
            filterSections(nodeName, sectionsVec);
            sortSections(nodeName, sectionsVec);
            
            sections = DataUtils.intVectorToArray(sectionsVec);
        }
        
        return sections;
    }

    
    /** 
     * Filters out sections set in filter. This feature can be used to not show
     * some sections in a table, for example the Imports section has not much sense
     * in Excel output. 
     */
    private void filterSections(String sectionName, IntVector sections) {
         
        if (m_removedSections != null  &&  m_removedSections.containsKey(sectionName)) {
            
            int [] removedSections = m_removedSections.get(sectionName);
            
            for (int i = 0; i < removedSections.length; i++) {

                int removedSection = removedSections[i];

                for (int j = 0; j < sections.size(); j++) {
                    if (sections.get(j)  == removedSection) {
                        sections.remove(j);
                        j--;
                        break; // each section should be in the list at most once
                    }
                }
            }
        }
    }

    
    /** Sorts sections based on order configured with call to setSubtractiveFilters(). */
    private void sortSections(String sectionName, IntVector tbSections) {
        
        if (m_orderedSections == null) {
            return;
        }
        
        int orderedSections[] = m_orderedSections.get(sectionName);
        
        if (orderedSections == null) {
            return;
        }
        
        int destIdx = 0;
        for (int orderedSection : orderedSections) {
            
            for (int idx = 0; idx < tbSections.size(); idx++) {
                if (tbSections.get(idx) == orderedSection) {
                    // swap tbSection(idx, destIdx):
                    int tmp = tbSections.get(idx);
                    tbSections.set(idx, tbSections.get(destIdx));
                    tbSections.set(destIdx, tmp);
                    destIdx++;
                    break;
                }
            }
        }
    }

    
    
    /** remove - does not work, because header was not created with info from added test base,
     * may not match it
     * This method adds additional test base to values list at the given index.
     * It allows display of parent test case in exported Excel table.   
     * @param testBase
    public void addTestBaseToDataValues(int idx, CTestBase testBase) {
        
        ArrayTableCell valuesList[] = new ArrayTableCell[getNumHeaderCells()];
        m_data.add(idx, valuesList);
        try {
            testBaseToDataValues(testBase, m_header, valuesList);
        } catch (Exception ex) {
            throw new SIllegalStateException("Can not convert list of test cases to table.", ex);
        }
    }
     */

    
    private void testBaseListToDataValues(CTestBaseList testBaseList) {

        m_data = new ArrayList<>();
        int size = (int)testBaseList.size();

        for (int idx = 0; idx < size; idx++) {
            // create on e row of data for one list member
            ArrayTableCell valuesList[] = new ArrayTableCell[getNumHeaderCells()];
            m_data.add(valuesList);
            CTestBase testBase = testBaseList.get(idx);
            try {
                testBaseToDataValues(testBase, m_header, valuesList, false);
                fillNullElements(testBase, valuesList);
            } catch (Exception ex) {
                throw new SIllegalStateException("Can not convert list of test cases to table.", ex);
            }
        }
    }

    
    private void testBaseToDataValues(CTestBase tb, HeaderNode currentHNode, 
                                      ArrayTableCell[] valuesList,
                                      boolean isReadOnly) {

        HeaderNode childNode = null;
        int [] sections = getFilteredAndSortedSections(tb, currentHNode);
        boolean isReadOnlyTbSection = isReadOnly;

        for (int sectionId : sections) {

            CTestBase mergedTb = tb;
            if (tb.isTestSpecification()) {
                mergedTb = tb.getCachedMergedTestSpec(sectionId);

                // The following 'if' statement is a workaround, see below
                if (sectionId == SectionIds.E_SECTION_FUNC.swigValue()) {
                    // if 'func' is not merged, check if 'func/params' is
                    if (mergedTb == null) {
                        mergedTb = tb.getCachedMergedTestSpec(SectionIds.E_SECTION_PARAMS_PRIVATE.swigValue());
                    }
                }
                
                isReadOnly = mergedTb != null;
                if (mergedTb == null) {
                    mergedTb = tb;
                }

            } else if (isReadOnlyTbSection) {
                // this is workaround for error in data design. Currently
                // inheritance is implemented for top level sections of CTestSpecification,
                // __except__ for parameters in CTestFunction. 
                // See Jira, TESTIDEA-67 for impl. of more consistent inheritance.
                
                // Get original test spec (parent of CTestFunction is merged  
                // CTestSpecification, of which original CTestSpecification is parent).
                CTestBase parent = tb.getParent().getParent();
                if (parent != null  &&  parent.isTestSpecification()) {
                    if (currentHNode.getSectionId() == SectionIds.E_SECTION_FUNC.swigValue()) {
                        
                        // CTestSpecification parentTs = CTestSpecification.cast(parent);
                        
                        if (sectionId == CTestFunction.ESection.E_SECTION_PARAMS.swigValue()) {
                            CTestBase mergedTestSpec = parent.getCachedMergedTestSpec(SectionIds.E_SECTION_PARAMS_PRIVATE.swigValue());
                            isReadOnly = mergedTestSpec != null;
                            if (mergedTestSpec == null) {
                                mergedTb = parent.getTestBase(SectionIds.E_SECTION_FUNC.swigValue(), false);
                            } else {
                                mergedTb = mergedTestSpec.getTestBase(SectionIds.E_SECTION_FUNC.swigValue(), true);
                            }
                        } else {
                            CTestBase mergedTestSpec = parent.getCachedMergedTestSpec(SectionIds.E_SECTION_FUNC.swigValue());
                            isReadOnly = mergedTestSpec != null;
                            if (mergedTestSpec == null) {
                                mergedTb = parent.getTestBase(SectionIds.E_SECTION_FUNC.swigValue(), false);
                            } else {
                                mergedTb = mergedTestSpec.getTestBase(SectionIds.E_SECTION_FUNC.swigValue(), true);
                            }
                        }
                    }
                }
            }
            

            String sectionName = mergedTb.getTagName(sectionId);
            ETestObjType sectionType = mergedTb.getSectionType(sectionId);
            int cellIdx;
            childNode = currentHNode.getChild(sectionName);
            if (childNode == null) {
                throw new SIllegalStateException("Internal error: Table Header Node not found for section '" +
                                                  sectionName + "'");
            }

            switch (sectionType) {

            case EYAMLBool:
                cellIdx = currentHNode.getChildColumnIndex(sectionName);
                valuesList[cellIdx] = new ArrayTableCell(mergedTb, childNode, isReadOnly);
                break;

            case EYAMLEnum:
                cellIdx = currentHNode.getChildColumnIndex(sectionName);
                valuesList[cellIdx] = new ArrayTableCell(mergedTb, childNode, isReadOnly);
                break;

            case EYAMLScalar:
                cellIdx = currentHNode.getChildColumnIndex(sectionName);
                valuesList[cellIdx] = new ArrayTableCell(mergedTb, childNode, isReadOnly);
                break;

            case EYAMLMap:
                int numMappingItems = childNode.getNumChildren();
                for (int keyIdx = 0; keyIdx < numMappingItems; keyIdx++) {
                    // HeaderNode mappingNode = childNode.getChild(keyIdx);
                    cellIdx = childNode.getChildColumnIndex(keyIdx);
                    HeaderNode mappingNode = childNode.getChild(keyIdx);
                    valuesList[cellIdx] = new ArrayTableCell(mergedTb, 
                                                             mappingNode,
                                                             isReadOnly);
                }

                break;

            case EYAMLSeqence:
                int numSeqItems = childNode.getNumChildren();
                // add number of nodes so that it matches the longest one of all instances
                for (int seqIdx = 0; seqIdx < numSeqItems; seqIdx++) {
                    
                    String key = String.valueOf(seqIdx); 
                    HeaderNode seqHeader = childNode.getChild(seqIdx);
                    cellIdx = childNode.getChildColumnIndex(key);
                    valuesList[cellIdx] = new ArrayTableCell(mergedTb, 
                                                             seqHeader,
                                                             isReadOnly);
                }
                break;

            case ETestBase:
                childNode = currentHNode.getChild(sectionName);
                // must not be const since ref, is stored to ArrayTableCell and
                // user may enter data
                CTestBase childTb = mergedTb.getTestBase(sectionId, false);
                testBaseToDataValues(childTb, childNode, valuesList, isReadOnly);
                break;
                
            case ETestBaseList:
                childNode = currentHNode.getChild(sectionName);
                CTestBaseList tbList = mergedTb.getTestBaseList(sectionId, true);
                for (int i = 0; i < tbList.size(); i++) {
                    HeaderNode arrayElemNode = childNode.getChild(String.valueOf(i));
                    testBaseToDataValues(tbList.get(i), arrayElemNode, valuesList, isReadOnly);
                 }
                break;
            default:
                break;
            }
        }
    }

    
    /**
     * If the number of elements in CTestBaseList-s or user Mappings and Sequences
     * is not the same in all test cases in the list, then some cells are not set.
     * This functions sets also such cells, but with testBase == null.
     *    
     * @param testBase
     * @param headerRoot
     * @param valuesList
     */
    private void fillNullElements(CTestBase testBase, ArrayTableCell[] valuesList) {
        
        for (int col = 0; col < valuesList.length; col++) {
            if (valuesList[col] == null) {
                HeaderNode headerNode = m_header.getFirstNonEmptyCellBottomUp(col);
                if (headerNode == null) { // this happens when there is no data in the table, 
                                          // usually at app. start, when nothing is selected
                    return;
                }
                
                CTestTreeNode treeNode = testBase.getContainerTestNode();
                
                boolean isReadOnly = false;
                
                if (treeNode != null  &&  treeNode.isTestSpecification()) {
                    CTestSpecification testSpec = CTestSpecification.cast(treeNode);
                    
                    HeaderNode rootTestBaseNode = m_header.getRootTestBaseNode(col);
                    int sectionId = rootTestBaseNode.getSectionId();
                    if (testSpec.isInheritSection(SectionIds.values()[sectionId])) {
                        isReadOnly = true;
                    }
                }
                
                valuesList[col] = new ArrayTableCell(null, headerNode, isReadOnly);
            }
        }
    }


    private int getNumHeaderCells() {
        return m_header.getRequiredNumOfHeaderCells();
    }
    
    
    private int getNumDataSets() {
        return m_data.size();
    }
    
    
    private ArrayTableCell getDataItem(int headerIndex, int dataSetIndex) {
        
        ArrayTableCell cell = m_data.get(dataSetIndex)[headerIndex];

        /* 
         * creating of non-null cells in place of empty data in model does not work,
         * because indices or assignment var name should be added to header also,
         * when mapping or sequence are empty
         * 
         * if (cell == null) {
            System.out.println("col, row = " + headerIndex + ", " + dataSetIndex);
            // Cell does not exist for the given testBase. It happens when the testBase
            // does not have some elements in a user sequence or user mapping, so 
            // we have to add one

            HeaderNode node = m_header.getFirstNonEmptyCellBottomUp(headerIndex);
            CTestBase tb = m_testBaseList.get(dataSetIndex);
            HeaderNode parent = node.getParent();
            int seqIdx = parent.getChildIndex(node.getName());

            if (node.getChildrenType() == EChildrenType.EScalarSequence) {
                // seqIdx is larger than num of elements in the list. Any action
                // accessing the element must take this into account and add missing
                // elements and remove them on undo
                // cell = new ArrayTableCell(tb, node.getSectionId(), m_nodeId, seqIdx);
            } else if (node.getChildrenType() == EChildrenType.EUserMapping) {

                List<String> predecessors = new ArrayList<>();
                for (int i = 0; i < seqIdx; i++) {
                    predecessors.add(parent.getChild(i).getName());
                }
                String key = node.getName();
                // cell = new ArrayTableCell(tb, node.getSectionId(), m_nodeId, key, predecessors);
            }
            
            m_data.get(dataSetIndex)[headerIndex] = cell;
        } */

        return cell;
    }

    
    public String getRowsAsCTestBaseYAMLString(int rows[]) {
        if (m_data.isEmpty()) {
            return "";
        }
        
        CTestBaseList list = new CTestBaseList();
        for (int row : rows) {
            list.add(-1, m_viewTestBaseList.get(row));
        }
        
        return list.toString();
    }
    
    
    /**
     * Creates action to set contents at the given position.
     * 
     * @param columnIndex
     * @param dataRow
     * @param newValue must have 3 elements: [0] - value, [1] - nl comment, [2] - eol comment
     * @return
     */
    public AbstractAction createSetContentAtAction(int columnIndex, int dataRow, 
                                                   String newValue, String nlComment, String eolComment) {
        
        ArrayTableCell cell = getDataValue(columnIndex, dataRow);
        
        // null when user cancels map key dialog when adding mapping column
        if (cell == null  ||  cell.isReadOnlyCell()) { 
            return new GroupAction("Empty action, read only table cell");
        }
        
        if (cell.existsInModel()) {
            
            AbstractAction action = createSetAction(cell, columnIndex, dataRow, 
                                                    newValue, nlComment, eolComment);
            
            if (dataRow == 0) {
                // If there are derived test cases, then change in the parent test case 
                // must be reflected in them, which means total refresh is needed.
                CTestTreeNode containerTestNode = cell.getTestBase().getContainerTestNode();
                action.addAllFireEventTypes();
                action.addTreeChangedEvent(containerTestNode, containerTestNode);
            }

            if (action != null) {
                return action;
            }
        } else {
            // there is no data for the given cell in model:
            // - if rows display CTestBaseList items, some rows may have less items
            //   than the other rows (may be even completely empty). In this case
            //   we create insert actions here
            // - it may be empty item in user sequence or mapping, for example: 
            //   one test base has sequence of 2 elements, but the 
            //   sequence is empty in the next row.
            HeaderNode bottomHNode = m_header.getFirstNonEmptyCellBottomUp(columnIndex);
            HeaderNode parentHNode = bottomHNode.getParent();
            if (bottomHNode.isLeafNode()) {
                if (parentHNode.isUserSequenceNode()) {

                    CTestBase rootTestBase = m_viewTestBaseList.get(dataRow);
                    CTestBase testBase = bottomHNode.getTestBase(rootTestBase, false);

                    YamlScalar scalar = YamlScalar.newListElement(bottomHNode.getSectionId(),
                                                                  Integer.parseInt(bottomHNode.getName()));
                    setValueAndComment(scalar, newValue, nlComment, eolComment);
                    SetSequenceItemAction action = new SetSequenceItemAction(testBase, 
                                                                             m_nodeId, 
                                                                             scalar);
                    action.addDataChangedEvent();  // will have to refresh the table,
                    // because model structure changed
                    action.addAllFireEventTypes();
                    return action;
                    
                } else if (parentHNode.isUserMappingNode()) {
                    
                    // setting of mapping is also done in this m. 
                    AbstractAction action = createSetAction(cell, columnIndex, dataRow, 
                                                            newValue, nlComment, eolComment);
                    if (action != null) {
                        return action;
                    }
                } else if (parentHNode.isStructMapping()) {
                    // This adds test base automatically, as it retrieves it as non-const.
                    // This operation is not undoable, however, if the object remains empty,
                    // it will not be saved anyway - only fields in table are editable instead of grey.
                    // Tip: If you want to make it undoable, then remove this CTestBase from the 
                    // container list at the given index, and add a new one with action. Currently
                    // the object added by side effect is not worth the effort.
                    CTestBase testBase = bottomHNode.getTestBase(m_viewTestBaseList.get(dataRow), 
                                                                     false);

                    YamlScalar scalar = bottomHNode.getYamlScalar();
                    scalar.setValue(newValue);
                    scalar.setNewLineComment(nlComment);
                    scalar.setEndOfLineComment(eolComment);
                    SetSectionAction action = new SetSectionAction(testBase, m_nodeId, scalar);
                    action.addDataChangedEvent(m_nodeId, testBase);
                    CTestTreeNode containerTestNode = testBase.getContainerTestNode();
                    if (dataRow > 0) {
                        // keep selection on parent node, if CTestBase in child is created.
                        // It is annoying, if the derived test base is selected after this
                        // operation. Test: Create test with derived test with empty stub (grey cells).
                        // Then enter stubbed function name into grey cell.
                        containerTestNode = containerTestNode.getParentNode();
                    }
                    action.addTreeChangedEvent(containerTestNode, containerTestNode);
                    action.addAllFireEventTypes();
                    
                    return action;
                }
                
            }
        } 
        
        throw new SIllegalStateException("Internal error - undefined table cell not allowed!")
            .add("columnIndex", columnIndex)
            .add("rowIndex", dataRow)
            .add("value", newValue);
    }

    
    private AbstractAction createSetAction(ArrayTableCell cell, 
                                           int columnIndex, 
                                           int rowIndex, 
                                           String newValue, 
                                           String nlComment, String eolComment) {
        
        HeaderNode node = m_header.getFirstNonEmptyCellBottomUp(columnIndex);
        if (node != null) {
            YamlScalar scalar = node.getYamlScalar();
            
            if (scalar != null) {

                SectionActionListener sectionListener = null;
                if (m_sectionListeners != null) {
                    sectionListener = m_sectionListeners.get(node.getPath());
                }
                
                if (node.isLeafNode()) {
                    node = node.getParent();
                }
                
                List<String> predecessors = null;
                if (node.isUserMappingNode()) {
                    predecessors = node.getPredecessors(cell.getKey());
                }
                setValueAndComment(scalar, newValue, nlComment, eolComment);
                AbstractAction action = cell.createSetValueAction(scalar, predecessors);
                
                if (sectionListener != null) {
                    sectionListener.onTestBaseCellModified(action, cell.getTestBase());
                }
                
                return action;
            } else {
                throw new SIllegalStateException("Internal error - no scalar defined for table cell!")
                .add("columnIndex", columnIndex)
                .add("rowIndex", rowIndex)
                .add("newValue", newValue);
            }
        }
        
        return null;
    }
        
    private void setValueAndComment(YamlScalar scalar, 
                                    String newValue, String nlComment, String eolComment) {
        
        scalar.setValue(newValue);
        
        if (nlComment != null) {
            scalar.setNewLineComment(nlComment);
        }
        
        if (eolComment != null) {
            scalar.setEndOfLineComment(eolComment);
        }
    }
    

    public GroupAction createInsertParsedListAction(String testBaseListYAML, int insertionIdx) {

        GroupAction groupAction = new GroupAction("Insert parsed test base list"); 
        CTestBase tmpTestBase = m_containerTestBase.createInstance(null);
        CYAMLUtil.parseTestBaseList(testBaseListYAML, tmpTestBase, m_sectionId);
        
        CTestBaseList srcList = tmpTestBase.getTestBaseList(m_sectionId, true);
        CTestBaseList destList = m_containerTestBase.getTestBaseList(m_sectionId, false);
        
        int numItems = (int)srcList.size();
        for (int i = 0; i < numItems; i++) {
            CTestBase testBase = srcList.get(i);
            testBase.setParent(m_containerTestBase);
            InsertToTestBaseListAction action = new InsertToTestBaseListAction(destList, 
                                                                               testBase, 
                                                                               insertionIdx++);
            groupAction.add(action);
        }

        groupAction.addDataChangedEvent(m_nodeId, m_containerTestBase);
        
        return groupAction;
    }
    
    
    /**
     * @param col
     * @return Returns group action which removes the given item from all CTEstBases in 
     * the list.
     */
    public GroupAction createRemoveSeqOrUserMappingColumnAction(HeaderNode node) {
        
        HeaderNode parentNode = node.getParent();
        if (parentNode.isUserSequenceNode()  ||  parentNode.isUserMappingNode()) {

            GroupAction groupAction = new GroupAction("Remove table column");
            for (int rowIdx = 0; rowIdx < m_data.size(); rowIdx++) {
                
                CTestBase testBase = node.getTestBase(m_viewTestBaseList.get(rowIdx), 
                                                      false);
                
                if (parentNode.isUserMappingNode()) {
                    CMapAdapter userMapping = new CMapAdapter(testBase, 
                                                              node.getSectionId(), 
                                                              true);
                    if (userMapping.contains(node.getName())) { // not all mappings have all keys
                        groupAction.add(new RemoveFromUserMappingAction(testBase, 
                                                                        node.getSectionId(), 
                                                                        node.getName()));
                    }
                } else if (parentNode.isUserSequenceNode()) {
                    CSequenceAdapter seq = new CSequenceAdapter(testBase, 
                                                                node.getSectionId(), 
                                                                true);
                    // some sequences may not have all items populated
                    if (seq.size() > node.getIndex()) {
                        groupAction.add(new RemoveFromSeqAction(testBase, 
                                                                node.getSectionId(), 
                                                                node.getIndex()));
                    }
                }                
                
                /* ArrayTableCell rowCell = m_data.get(rowIdx)[col];
                if (rowCell.existsInModel()) {
                    AbstractAction action = rowCell.createRemoveAction();
                    groupAction.add(action);
                } */
            }

            groupAction.addDataChangedEvent(m_nodeId, m_containerTestBase);
            addTreeStructChangedEventForParent(groupAction);
            
            return groupAction;
        }
        
        return null;
    }

    
    public GroupAction createAddLeafSeqColumnAction(HeaderNode seqLeafNode, int selectedCol, int offset) {
        GroupAction groupAction = new GroupAction("Add table seq column");

        int childIdx = seqLeafNode.getParent().getChildIndex(seqLeafNode.getName());
        int insertedCol = childIdx + offset;
        HeaderNode parentNode = seqLeafNode.getParent();
        
        if (!parentNode.isUserSequenceNode()) {
            throw new SIllegalStateException("Can not insert sequence column to " +
                    "non-sequence node!").add("selectedCol", selectedCol);
        }
        
        for (int rowIdx = 0; rowIdx < m_data.size(); rowIdx++) {
            groupAction.add(createAddSeqItemAction(parentNode, rowIdx, 
                                                   insertedCol, "", "", ""));
        }

        return groupAction;
    }
    
    
    public GroupAction createAddToSeqParentColumnAction(HeaderNode seqParentNode, int selectedCol) {
        GroupAction groupAction = new GroupAction("Add table seq column");


        if (!seqParentNode.isUserSequenceNode()) {
            throw new SIllegalStateException("Can not insert sequence column to " +
            		"non-sequence node!").add("selectedCol", selectedCol);
        }
        
        for (int rowIdx = 0; rowIdx < m_data.size(); rowIdx++) {
            int insertedCol = 0;
            groupAction.add(createAddSeqItemAction(seqParentNode, rowIdx, 
                                                   insertedCol, "", "", ""));
        }

        return groupAction;
    }
    
    
    public AbstractAction createAddSeqItemAction(HeaderNode seqOrLeafNode, 
                                                 int rowIdx,
                                                 int insertedCol, 
                                                 String strValue,
                                                 String nlComment,
                                                 String eolComment) {

        YamlScalar value = YamlScalar.newListElement(seqOrLeafNode.getSectionId(), 
                                                     insertedCol);
        value.setValue(strValue);
        value.setNewLineComment(nlComment);
        value.setEndOfLineComment(eolComment);
        
        CTestBase testBase = seqOrLeafNode.getTestBase(m_viewTestBaseList.get(rowIdx), 
                                                       false);
        
        InsertToSequenceAction action = new InsertToSequenceAction(testBase, value);
        action.addDataChangedEvent();
        // new column has to be added to table, so table structure changes 
        CTestTreeNode containerTestNode = testBase.getContainerTestNode();
        CTestTreeNode parentNode = containerTestNode;
        // for init sequence there is no containerTestNode - it is child of test config! 
        if (containerTestNode != null) {
            parentNode = containerTestNode.getParentNode();
        }
        
        action.addTreeChangedEvent(containerTestNode, 
                                   parentNode);
        action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
        
        return action;
    }
    
    
    public InsertToUserMappingAction createAddUserMappingItemAction(HeaderNode userMapNode,
                                                                    int rowIdx,
                                                                    List<String> predecessors,
                                                                    String key,
                                                                    String strValue,
                                                                    String nlComment,
                                                                    String eolComment) {

        YamlScalar value = YamlScalar.newUserMapping(userMapNode.getSectionId(), 
                                                     key);
        value.setValue(strValue);
        value.setNewLineComment(nlComment);
        value.setEndOfLineComment(eolComment);
        
        CTestBase testBase = userMapNode.getTestBase(m_viewTestBaseList.get(rowIdx), 
                                                     false);

        InsertToUserMappingAction action = new InsertToUserMappingAction(testBase, 
                                                                         value, 
                                                                         predecessors);
        action.addDataChangedEvent();
        // new column has to be added to table, so table structure changes 
        action.addTreeChangedEvent(testBase.getContainerTestNode(), 
                                   testBase.getContainerTestNode().getParentNode());
        action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
        return action;
    }

    
    /**
     * Creates CTestBase for one of sections in the table (not top level CTestBase 
     * which represents one row). For example Coverage statistics item in KTable 
     * with CTestSpecification-s as rows.  
     *  
     * @param userMapNode
     * @return
     */
    public AbstractAction createTestBaseInSubListAction(HeaderNode tbListNode, 
                                                        int insertIdx) {

        GroupAction groupAction = new GroupAction("Add test base to sub-test base list");
        int sectionId = tbListNode.getSectionId();
        
        for (int rowIdx = 0; rowIdx < m_data.size(); rowIdx++) {
        
            CTestBase testBase = tbListNode.getTestBase(m_viewTestBaseList.get(rowIdx), 
                                                        false);

            CTestBaseList tbList = testBase.getTestBaseList(sectionId, false);
            
            CTestBase newTestBase = testBase.createTestBase(sectionId);
            groupAction.add(new InsertToTestBaseListAction(tbList, newTestBase, insertIdx));
        }

        addTreeStructChangedEventForParent(groupAction);
        
        return groupAction;
    }


    public AbstractAction createRemoveTestBaseFromSubListAction(HeaderNode tbNode) {
        GroupAction groupAction = new GroupAction("Remove test base from sub-test base list");
        HeaderNode testBaseListNode = tbNode.getParent();
        
        int sectionId = testBaseListNode.getSectionId();
        int tbIndex = tbNode.getIndex();
        
        for (int rowIdx = 0; rowIdx < m_data.size(); rowIdx++) {
        
            CTestBase testBase = testBaseListNode.getTestBase(m_viewTestBaseList.get(rowIdx), 
                                                              false);

            CTestBaseList tbList = testBase.getTestBaseList(sectionId, false);
            
            // create remove actions only for lists, which contain data. For example, it
            // may happen, that test case A has 3 stubs defined, derived test case D1 inherits
            // stubs, and derived test case D2 defines 3 stubs:
            // A:  s1 s2
            // D1:              // no stubs, they are inherited
            // D2: s1 s2 s3
            // When removing stubs column[2], RemoveFromTestBaseListAction should be 
            // created only for D2.
            if (tbList.size() > tbIndex) {
                groupAction.add(new RemoveFromTestBaseListAction(tbList, tbIndex));
            }
        }
        
        addTreeStructChangedEventForParent(groupAction);
        
        return groupAction;
    }
    
    
    private void addTreeStructChangedEventForParent(GroupAction groupAction) {
        
        groupAction.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);

        if (!m_viewTestBaseList.isEmpty()) {
            CTestBase testBase = m_viewTestBaseList.get(0);
            // table structure changes
            groupAction.addTreeChangedEvent(testBase.getContainerTestNode(), 
                                            testBase.getContainerTestNode());
        }
    }
    
    
    public AbstractAction createRemoveTestBaseAction(int tbIndex) {

        if (m_firstRowTestBase != null) { // view list has the first row more than container list
            tbIndex--;
        }
        
        RemoveFromTestBaseListAction action = new RemoveFromTestBaseListAction(m_testBaseList, tbIndex);
        
        setActionEvents(action);

        return action;
    }

    
    public AbstractAction createAddRowAction(int tbIndex) {
        // CTestBase templateTb = m_testBaseList.get(0);
        // CTestBase newInstance = templateTb.createInstance(templateTb.getParent());
        CTestBase newInstance = m_containerTestBase.createTestBase(m_sectionId);
        
        if (m_firstRowTestBase != null) { // view list has the first row more than container list
            tbIndex--;
        }
        
        InsertToTestBaseListAction action = new InsertToTestBaseListAction(m_testBaseList, newInstance, tbIndex);

        setActionEvents(action);
        
        return action;
    }

    
    public AbstractAction createSwapRowsAction(int startIdx, int numRows, boolean isMoveDown) {

        if (m_firstRowTestBase != null) { // view list has the first row more than container list
            startIdx--;
        }
        
        SwapTestBaseListItemsAction action = new SwapTestBaseListItemsAction(m_testBaseList, 
                                                                             startIdx, 
                                                                             numRows, 
                                                                             isMoveDown);
        
        setActionEvents(action);
        
        return action;
    }

    
    /**
     * 
     * @param row
     * @param sectionId section in CTestSpecification, for which inheritance has to be set
     * @param isInherited if null, toggle inheritance
     * @return
     */
    public SetSectionAction createSetInheritanceAction(int dataRow, 
                                                       int sectionId, 
                                                       Boolean isInherited) {
        
        CTestBase testBase = m_viewTestBaseList.get(dataRow);
        
        if (testBase.isTestSpecification()) {
            CTestSpecification testSpec = CTestSpecification.cast(testBase);
            CTestImports imports = testSpec.getImports(false);
            SectionIds eSectionId = SectionIds.swigToEnum(sectionId);
            CTestImportSources sources = imports.getSectionSources(eSectionId, false);
            YamlScalar scalar = YamlScalar.newMixed(ESectionSources.E_SECTION_IS_INHERIT.swigValue());
            
            if (isInherited == null) {
                // toggle
                boolean isInherit = testSpec.isInheritSection(eSectionId);
                scalar.setValue(isInherit ? "false" : "true");
            } else {
                // set
                scalar.setValue(isInherited ? "true" : "false");
            }
            
            SetSectionAction action = new SetSectionAction(sources, m_nodeId, scalar);
            
            action.addAllFireEventTypes();
            action.addDataChangedEvent(m_nodeId, testBase);
            
            return action;
        }
        
        throw new SIllegalArgumentException("Selected cell does not belong to CTestSpecification!");
    }


    private void setActionEvents(AbstractAction action) {
        if (m_isEditingOutlineTree) {
            action.addTreeChangedEvent(m_containerTestBase.getParent().getContainerTestNode(), 
                                       m_containerTestBase.getContainerTestNode());
            action.addAllFireEventTypes();
        } else {
            action.addDataChangedEvent(m_nodeId, m_containerTestBase);
            action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
        }
    }

    
    public GroupAction createRenameUserMappingKeyAction(int col, String newKey) {
        GroupAction groupAction = new GroupAction("Rename user mapping key");
        
        for (int rowIdx = 0; rowIdx < m_data.size(); rowIdx++) {
            ArrayTableCell[] rowCells = m_data.get(rowIdx);
            if (rowCells[col] != null  &&  rowCells[col].existsInModel()) {
                AbstractAction action = rowCells[col].createRenameAction(newKey);
                if (action != null) {
                    groupAction.add(action);
                }
            }
        }

        groupAction.addDataChangedEvent(m_nodeId, m_containerTestBase);
        groupAction.addFireEventType(EFireEvent.UNDO);
        groupAction.addFireEventType(EFireEvent.REDO);
        return groupAction;
    }
    
    
    public AbstractAction createSetCommentAction(int col, int row, String nlComment, String eolComment) {
        ArrayTableCell cell = getDataValue(col, row);
        return cell.createSetCommentAction(nlComment, eolComment);
    }


    public CTestBase getTestBase(int dataRow) {
        
        CTestBase rowTestBase = m_viewTestBaseList.get(dataRow);
        
        return rowTestBase;
    }
}
