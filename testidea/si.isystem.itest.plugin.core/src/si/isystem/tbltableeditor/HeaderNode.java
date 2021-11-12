package si.isystem.tbltableeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.StrVector;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.exceptions.SIndexOutOfBoundsException;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;


/**
 * This class contains information for one table header cell, and children which
 * contain information for lower table cells. Tree hierarchy of 
 * these objects form data model for table header. 
 * 
 * @author markok
 *
 */
public class HeaderNode {

    final private HeaderNode m_parent; // null for the root parent
    final private String m_sectionName;
    final private int m_sectionId;
    final private ENodeId m_uiNodeId;
    final private ETestObjType m_hdrNodeType;

    private LinkedHashMap<String, HeaderNode> m_children;   // if null, this is leaf node 
    private String m_visibleName;
    
        
    private HeaderPath m_cachedAbstactPath;

    private boolean m_defaultBool; // default value for nodes of type EYAMLBool. 
                                   // Has meaning only for this type of nodes.
    private String[] m_enumItems;  // items for nodes of type EYAMLEnum. Not valid
                                   // for other nodes.
    private String m_defaultEnumValue; // valid for nodes of type EYAMLEnum only.
    
    // used for nodes with user specified mappings (CYAMLMap) only
    Map<String, Set<String>> m_varPredecessors = new TreeMap<>(); // contains vars which
        // succeed the assignment of var used as a key in this map. Used to detect invalid
        // assignment order in different steps, which can occur in manually edited test specs.
        // The algorithm checks for each var, that vars assigned before the var never appear
        // after the var. for example:
        // a b c d   ==> map {a: [], b: [a], c: [a, b], d: [a, b, c]
        // b         ==> map {a: [], b: [a], c: [a, b], d: [a, b, c] - no change from previous step
        // bc        ==> map {a: [], b: [a], c: [a, b], d: [a, b, c] - no change from previous step
        //               search is made for b, if 'c' which is on the right is in the list map[b]
        // da        ==> error because a is member of the list map[d]. We don't
        //               have to check if 'd' was on the right of a, because 
        //               the error is discovered when verifying the order for 'd'
        // ebc       ==> map {a: [], b: [a, e], c: [a, b, e], d: [a, b, c], e: []
        // aed       ==> map {a: [], b: [a, e], c: [a, b, e], d: [a, b, c, e], e: [a]
        // ce        ==> error, because the list map[c] already contains 'e', so 'e' should not be 
        //               on the right side of 'c'

    
    /** This ctor should be used only for root node. */
    public HeaderNode(ETestObjType hdrNodeType, String sectionName, ENodeId uiNodeId) {
        m_parent = null;
        m_sectionId = -1; // undefined for root node
        m_hdrNodeType = hdrNodeType;
        m_sectionName = sectionName;
        m_uiNodeId = uiNodeId;
    }
    
    
    private HeaderNode(HeaderNode parent, String name, int sectionId, 
                       ETestObjType hdrNodeType, ENodeId uiNodeId) {
        m_parent = parent;
        m_sectionName = name;
        m_sectionId = sectionId;
        m_hdrNodeType = hdrNodeType;
        m_uiNodeId = uiNodeId;
    }
    

    public void setVisibleName(String visibleName) {
        m_visibleName = visibleName;
    }


    public String getVisibleName() {
        return m_visibleName != null ? m_visibleName : m_sectionName;
    }



    public void setDefaultBool(boolean defaultBool) {
        m_defaultBool = defaultBool;        
    }


    public boolean getDefaultBool() {
        return m_defaultBool;
    }


    public void setEnumItems(String[] enumItems, String defaultEnumValue) {
        m_enumItems = enumItems;        
        m_defaultEnumValue = defaultEnumValue;
    }


    public String[] getEnumItems() {
        return m_enumItems;
    }


    public String getDefaultEnumValue() {
        return m_defaultEnumValue;
    }

 
    public ENodeId getUiNodeId() {
        return m_uiNodeId;
    }


    // returns new or existing node for the given section name.
    public HeaderNode add(String name, int sectionId, 
                          ETestObjType hdrNodeType, ENodeId uiNodeId) {
        // System.out.println("HN.add: " + m_sectionName + " <- " + name);
        if (m_children == null) {
            m_children = new LinkedHashMap<>();
        }
        
        if (m_children.containsKey(name)) {
            return m_children.get(name);
        } else {
            HeaderNode node = new HeaderNode(this, name, sectionId, 
                                             hdrNodeType, uiNodeId);
            m_children.put(name, node);
            return node;
        }
    }
 

    void removeChild(String name) {
        m_children.remove(name);
    }
    
    
    public boolean insertUserMappingChild(String identifierName,
                                          HeaderNode clickedNode,
                                          boolean isInsertRight) {
        
        if (m_children == null) {
            m_children = new LinkedHashMap<>();
        }
        
        // clicked node should always be null when there are no children, just for safety
        if (m_children.isEmpty() ||  clickedNode == null) {
            // there is only one possibility to insert item into empty container 
            m_children.put(identifierName, new HeaderNode(this, identifierName,
                                                          m_sectionId,
                                                          ETestObjType.EYAMLScalar,
                                                          m_uiNodeId));
            return true;
        }
        
        if (m_children.containsKey(identifierName)) {
            return false;
        } else {
            // insert new node into linked map - there is no other way but to create
            // a new map and copy all items :-(
            LinkedHashMap<String, HeaderNode> newChildren = new LinkedHashMap<>();
            for (Map.Entry<String, HeaderNode> entry : m_children.entrySet()) {
                
                if (isInsertRight) {
                    newChildren.put(entry.getKey(), entry.getValue());
                }
                
                if (entry.getValue() == clickedNode) { // header nodes are supposed to be 
                                                       // single instances, so == is OK here
                    HeaderNode newNode = new HeaderNode(this, identifierName, m_sectionId, 
                                                        ETestObjType.EYAMLScalar,
                                                        m_uiNodeId);
                    newChildren.put(identifierName, newNode);
                }
                
                if (!isInsertRight) {
                    newChildren.put(entry.getKey(), entry.getValue());
                }
            }
            
            m_children = newChildren;
            
            return true;
        }
    }

    
    public HeaderNode getParent() {
        return m_parent;
    }

    
    public String getName() {
        return m_sectionName;
    }

    
    /** 
     * Returns / separated list of all parents, for example: 
     *    /analyzer/profiler/codeAreas/`LIST_NODE`/netTime. '*' is used instead of test base list index,
     *  so these paths can be used for removal of sections in list children.  
     */
    public String getPath() {
        
        return getAbstractPath().getAbstractPath();
    }
    
    
    public HeaderPath getAbstractPath() {

        if (m_cachedAbstactPath == null) {

            StringBuilder path = new StringBuilder();
            List<Integer> listIndices = new ArrayList<>(3);
            MutableInt seqIdx = new MutableInt(-1);
            MutableObject<String> mapKey = new MutableObject<>();

            getPathRecursively(this, path, listIndices, seqIdx, mapKey);

            int [] indices = new int[listIndices.size()];
            for (int idx = 0; idx < listIndices.size(); idx++) {
                indices[idx] = listIndices.get(idx).intValue();
            }

            m_cachedAbstactPath = new HeaderPath(path.toString(),
                                                 indices,
                                                 seqIdx.getValue().intValue(),
                                                 mapKey.getValue());
        }
        
        // System.out.println("path = " + m_cachedAbstactPath.toString());
        
        return m_cachedAbstactPath;
    }
    
    
    private void getPathRecursively(HeaderNode node, StringBuilder path, 
                                    List<Integer> listIndices, 
                                    MutableInt seqIdx,
                                    MutableObject<String> mapKey) {
        if (node.m_parent != null) {
            getPathRecursively(node.m_parent, path, listIndices, seqIdx, mapKey);
        }
        
        if (node.m_parent != null) {
            if (node.m_parent.isTestBaseList()) {
                listIndices.add(new Integer(node.m_sectionName));
                path.append(HeaderPath.SEPARATOR).append(HeaderPath.LIST_MARK); // make TestBaseList indices abstract, see method comment above
                return;
            } else if (node.isLeafNode()) {
                if (node.m_parent.isUserSequenceNode()) {
                    seqIdx.setValue(Integer.parseInt(node.m_sectionName));
                    path.append(HeaderPath.SEPARATOR).append(HeaderPath.SEQ_MARK); // make TestBaseList indices abstract, see method comment above 
                    return;
                } else if (node.m_parent.isUserMappingNode()) {
                    mapKey.setValue(node.m_sectionName);
                    path.append(HeaderPath.SEPARATOR).append(HeaderPath.MAP_MARK); // make TestBaseList indices abstract, see method comment above 
                    return;
                }
            }
        } 

        path.append('/').append(node.m_sectionName);
    }
    
    
    /**
     * @param sectionName name of child for which to get index
     * 
     * @return absolute index of child, where also children of parents are 
     *          taken into account 
     */
    int getChildColumnIndex(String sectionName) {
        
        int colIndex = 0;
        // climb up the hierarchy. Section names, which define path to caller 
        // node are on the stack.
        if (m_parent != null) {
            colIndex = m_parent.getChildColumnIndex(m_sectionName);
        }
        
        // get number of leafs for all nodes preceding this one in the list
        for (Map.Entry<String, HeaderNode> entry : m_children.entrySet()) {
            if (!entry.getKey().equals(sectionName)) {
                colIndex += entry.getValue().getRequiredNumOfHeaderCells();
            } else {
                break;
            }
        }
        
        return colIndex;
    }


    /**
     * @param childIdx index of child for which to get index
     * 
     * @return absolute index of child, where also children of parents are 
     *         taken into account 
     */
    int getChildColumnIndex(int childIdx) {
        
        int colIndex = 0;
        // climb up the hierarchy. Section names, which define path to caller 
        // node are on the stack.
        if (m_parent != null) {
            colIndex = m_parent.getChildColumnIndex(m_sectionName);
        }
        
        // get number of leafs for all nodes preceding this one in the list
        int idx = 0;
        for (Map.Entry<String, HeaderNode> entry : m_children.entrySet()) {
            if (idx != childIdx) {
                colIndex += entry.getValue().getRequiredNumOfHeaderCells();
            } else {
                break;
            }
            idx++;
        }
        
        return colIndex;
    }

    /** Returns the number of immediate children. */
    public int getNumChildren() {
        if (m_children == null) {
            return 0;
        }
        return m_children.size();
    }
    
    
    /** 
     * Returns child at the given index or null, if there is no child with the 
     * given name.
     * 
     * @param childName
     */
    public HeaderNode getChild(String childName) {
        return m_children.get(childName);
    }
    
    
    /** @return index of child in this node. */
    public int getChildIndex(String sectionName) {
        Set<String> sections = m_children.keySet();
        int idx = 0;
        for (String section : sections) {
            if (section.equals(sectionName)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }
    

    /**
     * @return depth of this subtree (number of rows in a table + 1 because root 
     * node is counted, see also getRowCount()).
     */
    public int getDepth() {
        int depth = 1;
        if (m_children != null) {
            int maxDepth = 0;
            for (HeaderNode child : m_children.values()) {
                maxDepth = Math.max(maxDepth, child.getDepth());
            }
            depth += maxDepth;
        }
        return depth;
    }

    /*
     *  
    // Used when user clicks + in header to show him list of nodes to add.
    // Currently all nodes will be shown, even empty ons. User can collapse them
    // in UI.
    // private List<String> m_allowedChildren;

    List<String> getAllowedChildren() {
    
        if (m_childrenType != EChildrenType.EPredefined) {
            throw new SIllegalArgumentException("This node does not have " +
            		                           "predefined names for children!")
            .add("nodeName", m_sectionName)
            .add("childrenType", m_childrenType);
        }
        
        Set<String> allKeys = new TreeSet<String>(m_allowedChildren);
        
        if (m_children != null) {
            List<String> existingKeys = new ArrayList<>();
            for (HeaderNode child : m_children.values()) {
                existingKeys.add(child.getName());
            }
            
            allKeys.removeAll(existingKeys);
        }
        
        return new ArrayList<String>(allKeys);
    
    }
     
    public void setAllowedChildren(List<String> allowedChildren) {
        m_allowedChildren = allowedChildren;
    } 
    */
    
    
    /**
     * @return the number of cells (rows or columns) required by this node.
     */
    public int getRequiredNumOfHeaderCells() {
        if (m_children == null  ||  m_children.isEmpty()) {
            return 1;  // we need at least one header cell for parent
        }

        int len = 0;
        for (HeaderNode child : m_children.values()) {
            len += child.getRequiredNumOfHeaderCells();
        }
        // System.out.println("HeaderNode: " + m_sectionName + "  len: " + len);
        return len;
    }
    

    /**
     * This method is called for HeaderNodes which contain CYAMLMap only.
     * It records the order of keys, so that multiple mappings can have
     * different assignments to different or same keys. However, the order
     * must be the same for all mappings. See comment for m_varSucessors.
     */
    boolean recordKeyOrder(StrVector keyVector) {
        
        boolean hasChanged = false;
        Set<String> predecessorsInStep = new TreeSet<>();
        int numVars = (int)keyVector.size();
        for (int varIdx = 0; varIdx < numVars; varIdx++) { 
            String varName = keyVector.get(varIdx);
            if (!m_varPredecessors.containsKey(varName)) {
                // insert new list if it does not exist
                m_varPredecessors.put(varName, new TreeSet<String>());
                hasChanged = true;
            } 
            if (!predecessorsInStep.isEmpty()) {
                // each list for 'varName' gets a list of successors at the given var index 
                hasChanged |= m_varPredecessors.get(varName).addAll(predecessorsInStep);
            }
            predecessorsInStep.add(varName); // add successor
        }
        
        if (hasChanged) {
            putMappingKeysInCorrectOrder();
        }
        return hasChanged;
    }
    
    /**
     * This method is used only for HeaderNodes, which contain data of CYAMLMap,
     * where items must have proper key order.
     * Reorders list of varNames so that they will match assignment
     * order in all steps. Good indication is the number of successors in a map -
     * the first item has the largest number of successors, the last one has 0 
     * of them. However, it may happen, that the order of assignments for table 
     * view is not fully defined. For example, consider the following steps:
     *
     *          a   b         
     * ---------------
     * step 0:  3 
     * step 1:      1
    
     * The following table view is equivalent:
     *
     *          b   a
     * ---------------
     * step 0:      3
     * step 1:  1
     *
     * In such case the number of successors is the same for both vars -
     * we have to apply additional verification - see the 'while' loop
     * below.
     */
    private void putMappingKeysInCorrectOrder() {
        int numVars = m_children.size();
        String varNames [] = new String[numVars];
        // System.out.println(toString());
        // set items in varName in correct order
        for (Map.Entry<String, HeaderNode> entry : m_children.entrySet()) {
            // StringBuilder varName = m_varNames.get(varIdx);
            String varName = entry.getKey();
            int varNameIdx = m_varPredecessors.get(varName).size();
            while (varNameIdx < numVars  &&  varNames[varNameIdx] != null) {
                varNameIdx++;
            }
            
            if (varNameIdx >= numVars) {
                throw new SIllegalStateException("Mapping keys are specified in mixed order and can not be arranged in ordered sequence!")
                .add("varName", varName)
                .add("array", Arrays.toString(varNames));
            }
            
            varNames[varNameIdx] = new String(varName);
        }
        
        // since we can not change the order in linked list, let's reinsert elements
        LinkedHashMap<String, HeaderNode> tmpChildren = 
                                 new LinkedHashMap<String, HeaderNode>(numVars);
        
        for (String varName : varNames) {
            tmpChildren.put(varName, m_children.get(varName));
        }
        m_children = tmpChildren;
    }


    /**
     * This function returns header node at the given position in table,
     * or null, if there is no header node at the given 
     * position. 
     * 
     * @param column column index, 0 based
     * @param depth row index, 0 based (root node has depth -1)
     * @return
     */
    public HeaderNode getNode(int column, int depth) {
    
        if (m_children == null) {
            return null;
        }
        
        int childIdx = 0;
        for (HeaderNode child : m_children.values()) {
            if (depth > 0) {
                HeaderNode node = child.getNode(column, depth - 1);
                if (node != null) {
                    return node;
                }
            } else {
                int childColumn = getChildColumnIndex(childIdx);
                if (column < childColumn) {
                    return null; // there are empty cells between child columns in this row
                }
                if (column == childColumn) {
                    return child;
                }
            }
            childIdx++;
        }
        
        return null;
    }
    
    
    /**
     * Returns child at the given index.
     * 
     * @param idx
     * @return
     */
    public HeaderNode getChild(int idx) {
        if (idx < 0  ||  idx >= m_children.size()) {
            throw new SIndexOutOfBoundsException("Illegal index for header node!").
               add("index", idx).
               add("numElements", m_children.size()).
               add("headerNodeName", m_sectionName);
        }
        
        return m_children.values().toArray(new HeaderNode[0])[idx];
    }

    
    /** 
     * Returns child index of this node in parent node. 
     */ 
    public int getIndex() {
        if (m_parent == null) {
            throw new SIllegalArgumentException("Node has no parent!").
            add("nodeName", m_sectionName);
        }
        
        return m_parent.getChildIndex(m_sectionName);
    }
    
    
    /** Returns list of names of children before the given one in the list. */
    public List<String> getPredecessors(String name) {
        int idx = getChildIndex(name);
        List<String> predecessors = new ArrayList<>();
        for (HeaderNode child : m_children.values()) {
            if (idx == 0) {
                break;
            }
            predecessors.add(child.getName());
            idx--;
        }
        return predecessors;
    }

    
    /** 
     * Returns YamlScalar for correct type of cell, or null if the given column
     * has no leaf nodes - this may happen in case of empty user mapping or
     * list nodes.
     * 
     * Scans header table column bottom up until non-empty cell is found.
     * 
     * @param columnIndex
     */
    public YamlScalar getYamlScalar(int columnIndex) {

        HeaderNode node = getFirstNonEmptyCellBottomUp(columnIndex);
        
        if (node != null) { // the first node from bottom up is leaf node,
            // or node without values, for example empty 
            // list or user mapping
            return node.getYamlScalar();
        }
        
        return null;
    }


    /**
     * 
     * @return configured instance of YAMLScalar for leaf nodes, user mappings,
     *         and user seq. nodes.
     */
    public YamlScalar getYamlScalar() {
        ETestObjType nodeType = m_hdrNodeType;
        
        // returns scalar for both leaf nodes and user mappings and sequences
        if (isLeafNode()) {
            nodeType = m_parent.m_hdrNodeType;
        }
        
        switch (nodeType) {
        case EYAMLMap:
            return YamlScalar.newUserMapping(m_sectionId, 
                                             getName());
        case EYAMLSeqence:
            return YamlScalar.newListElement(m_sectionId, 
                                             Integer.parseInt(getName()));
        case ETestBase:
            return YamlScalar.newMixed(m_sectionId);
        case EYAMLBool:
        case EYAMLEnum:
        case EYAMLScalar:
        case ETestBaseList:
        default:
            throw new SIllegalArgumentException("Invalid children type in table HeaderNode.")
            .add("nodeType", nodeType)
            .add("parent.nodeType", m_parent.m_hdrNodeType);
        }
    }
    
    
    /** 
     * Returns name (contents) of the first cell in the given column, which
     * is not empty. Search starts at the bottom and moves up.
     * Call this method only on root node. 
     * 
     * @param columnIndex
     */
    public HeaderNode getFirstNonEmptyCellBottomUp(int columnIndex) {
        int numRows = getRowCount();
        for (int row = numRows - 1; row >= 0; row--) {
            HeaderNode node = getNode(columnIndex, row);
            if (node != null) { 
                return node;
            }
        }
        
        return null;
    }


    /**
     * Returns node immediately below root test base (which contains data for one row),
     * to which the given column belongs. For example, if CTestSpecifications
     * are shown in rows, and coverage statistics column is given, this method 
     * returns node for CTestAnalyzer in CTestSpecification.
     *  
     * @param columnIndex index of the column in table.
     */
    public HeaderNode getRootTestBaseNode(int columnIndex) {

        HeaderNode node = getFirstNonEmptyCellBottomUp(columnIndex);
        HeaderNode prevNode = node; // top level node has no related test base,
        // it is only container of all nodes, so we need one below it.
        
        node = node.getParent();
        
        while (node.getParent() != null) {
            prevNode = node;
            node = node.getParent();
        }
        
        return prevNode;
    }
    
    
    /**
     * If the given cell is empty, searches for the first non-empty cell to the 
     * left and returns its column index. If the cell is not empty, the given
     * column index is returned. This is used to merge cells in KTable.
     */

    public int getParentColumn(int columnIndex, int rowIndex) {
        int numRows = getRowCount();
        for (int row = 0; row < numRows; row++) {
            HeaderNode node = getNode(columnIndex, row);
            if (node != null) { 
                 if (row <= rowIndex) {
                     return columnIndex; // empty cells below node are never joined 
                 }
              
                 for (int diffRows = row - rowIndex; diffRows > 0; diffRows--) {
                     node = node.getParent();
                     if (node == null  ||  node.getParent() == null) {
                         throw new SIllegalStateException("Internal error - non-null parent should be found!")
                         .add("colIdx", columnIndex)
                         .add("rowIdx", rowIndex)
                         .add("row", row)
                         .add("node", node);
                     }
                 }
                 return node.getParent().getChildColumnIndex(node.getName());
            }
        }
        return columnIndex;
    }
    
            
    /**
     * Returns value at the given position as a string. Empty string is returned 
     * for empty cells.
     */
    public String getDataValue(int columnIndex, int rowIndex) {
        HeaderNode node = getNode(columnIndex, rowIndex);
        String retVal = node != null ? node.getName() : "";
        // System.out.println("retVal = " + retVal + " / " + columnIndex + " , " + rowIndex);
        return retVal;
    }


    public int getColumnCount() {
        return getRequiredNumOfHeaderCells();
    }


    public int getRowCount() {
        return getDepth() - 1; // -1 because we don't want to show the root node
    }


    /**
     * @return the sectionId
     */
    public int getSectionId() {
        return m_sectionId;
    }


    /**
     * Returns CTestBase object, which contains data for this header node.
     *
     * @param testBase member of test base list, parent of all items in one row
     * 
     * @return child test base, which defines this header node 
     */
    public CTestBase getTestBase(CTestBase testBase, boolean isConst) {

        Stack<HeaderNode> pathToRoot = new Stack<>();
        pathToRoot.push(this);
        HeaderNode parent = m_parent;
        while (parent != null) {
            pathToRoot.push(parent);
            parent = parent.m_parent;
        }
         
        pathToRoot.pop(); // pop the root HeaderNode, because 'testBase' refers to it 
        
        while (!pathToRoot.isEmpty()) {
            HeaderNode node = pathToRoot.pop();
            
            switch (testBase.getSectionType(node.m_sectionId)) {
            case ETestBase:
                testBase = testBase.getTestBase(node.m_sectionId, isConst);
                break;
            case ETestBaseList:
                CTestBaseList tbList = testBase.getTestBaseList(node.m_sectionId, isConst);
                if (pathToRoot.isEmpty()) {
                    return testBase; // return parent of this CTestBaseList
                }
                node = pathToRoot.pop();
                int childIdx = node.m_parent.getChildIndex(node.m_sectionName);
                
                // Add as many list items as needed to be able to return valid
                // member. This functionality is needed when parsing imported tables, for example 
                // Excel table (see XlsImporter). 
                while(tbList.size() <= childIdx) {
                    tbList.add(-1, testBase.createTestBase(node.m_sectionId));
                }
                testBase = tbList.get(childIdx);
                break;
            case EYAMLBool:
            case EYAMLEnum:
            case EYAMLMap:
            case EYAMLScalar:
            case EYAMLSeqence:
            default:
            }
        }
        
        return testBase;
    }

    
    public boolean isBoolNode() {
        return m_hdrNodeType == ETestObjType.EYAMLBool;
    }
    
    
    public boolean isEnumNode() {
        return m_hdrNodeType == ETestObjType.EYAMLEnum;
    }
    
    
    public boolean isScalarNode() {
        return m_hdrNodeType == ETestObjType.EYAMLScalar;
    }
    
    
    /** Returns true, if this is bool, enum, or scalar node. */
    public boolean isLeafNode() {
        return m_hdrNodeType == ETestObjType.EYAMLBool  ||
               m_hdrNodeType == ETestObjType.EYAMLEnum  ||
               m_hdrNodeType == ETestObjType.EYAMLScalar;
    }

    
    public boolean isUserMappingLeafNode() {
        return isLeafNode()  &&  m_parent != null  &&  m_parent.isUserMappingNode();
    }
    
    
    public boolean isUserSeqenceLeafNode() {
        return isLeafNode()  &&  m_parent != null  &&  m_parent.isUserSequenceNode();
    }
    
    
    public boolean isUserMappingNode() {
        return m_hdrNodeType == ETestObjType.EYAMLMap;
    }
    
    
    public boolean isUserSequenceNode() {
        return m_hdrNodeType == ETestObjType.EYAMLSeqence;
    }


    public boolean isTestBaseList() {
        return m_hdrNodeType == ETestObjType.ETestBaseList;
    }
    
    
    public boolean isStructMapping() {
        return m_hdrNodeType == ETestObjType.ETestBase;
    }

    
    /** Returns true, if this is user mapping, sequence or test base list node. */
    public boolean isDynamicContainer() {
        return isUserMappingNode()  ||  isUserSequenceNode()  ||  isTestBaseList();
    }

    
    /** Returns this node and its children as table. */ 
    public String toTable() {
        StringBuilder sb = new StringBuilder();
        final int MAX_NODE_STR_LEN = 20;

        
        int numRows = getRowCount();
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < getColumnCount(); column++) {
                HeaderNode node = getNode(column, row);
                if (node != null) {
                    String nodeStr = node.m_sectionId + "/" + node.m_hdrNodeType.name().substring(1, 8) +"/" + node.m_sectionName + "                ";
                    sb.append(nodeStr.substring(0, MAX_NODE_STR_LEN));
                } else {
                    sb.append("                              ".substring(0, MAX_NODE_STR_LEN));
                }
            }
            sb.append('\n');
        }
        
        return sb.toString();
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HeaderNode:\n  m_parent: ");
        builder.append(m_parent == null ? "null" : m_parent.m_sectionName);
        builder.append("\n  m_children: ");
        builder.append(m_children != null ? m_children.keySet() : "/");
        builder.append("\n  m_sectionName: ");
        builder.append(m_sectionName);
        builder.append("\n  m_sectionId: ");
        builder.append(m_sectionId);
        builder.append("\n  m_nodeType: ");
        builder.append(m_hdrNodeType);
        builder.append("\n  m_varSucessors: ");
        builder.append(m_varPredecessors);
        builder.append("\n");
        return builder.toString();
    }
}
