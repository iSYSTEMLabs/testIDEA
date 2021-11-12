package si.isystem.itest.ui.spec.sections;

import java.util.Map;
import java.util.TreeMap;

import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;

public class SectionTreeModel {

    public enum ESectionTreeType {
        EUNIT_TEST,
        ESYSTEM_TEST,
        EGROUP
    }
    
    // consider removing this map, since nodes are already in m_nodesMap.
    private Map<ESectionTreeType, TreeNode<EditorSectionNode>> m_sectionNodeTree = new TreeMap<>();
    private Map<ENodeId, TreeNode<EditorSectionNode>> m_nodesMap = new TreeMap<>();
    
    
    private SectionTreeLabelProvider m_sectionTreeLabelProvider;

    public SectionTreeModel() {
        
        m_sectionTreeLabelProvider = new SectionTreeLabelProvider();
        
        TreeNode<EditorSectionNode> newNode = new TreeNode<EditorSectionNode>(null, null, null);
        m_sectionNodeTree.put(ESectionTreeType.EUNIT_TEST, newNode);
        m_nodesMap.put(ENodeId.UNIT_TEST_ROOT_NODE, newNode);
        
        newNode = new TreeNode<EditorSectionNode>(null, null, null);
        m_sectionNodeTree.put(ESectionTreeType.ESYSTEM_TEST, newNode);
        m_nodesMap.put(ENodeId.SYSTEM_TEST_ROOT_NODE, newNode);

        newNode = new TreeNode<EditorSectionNode>(null, null, null);
        m_sectionNodeTree.put(ESectionTreeType.EGROUP, newNode);
        m_nodesMap.put(ENodeId.GROUP_ROOT_NODE, newNode);
        
        createNodes();
    }


    private void createNodes() {
        createTestCaseNodes();
        createGroupNodes();
    }
    
    
    private void createTestCaseNodes() {
    
        TreeNode<EditorSectionNode> unitTestTree = getTopNode(ESectionTreeType.EUNIT_TEST);
        TreeNode<EditorSectionNode> systemTestTree = getTopNode(ESectionTreeType.ESYSTEM_TEST);
        
        addNode(ENodeId.META_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.FUNCTION_NODE, unitTestTree);
        
        addNode(ENodeId.SYS_TEST_BEGIN_STOP_COND_NODE, systemTestTree);
        addNode(ENodeId.SYS_TEST_END_STOP_COND_NODE, systemTestTree);
        addNode(ENodeId.PERSISTENT_VARS_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.VARS_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.PRE_CONDITIONS_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.EXPECTED_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.STUBS_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.STUBS_USER_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.TEST_POINT_NODE, unitTestTree, systemTestTree);
        
        TreeNode<EditorSectionNode> analyzerNode =
            addNode(ENodeId.ANALYZER_NODE, unitTestTree, systemTestTree);
        
        TreeNode<EditorSectionNode> cvrgNode = 
            addNode(ENodeId.ANAL_COVERAGE_NODE, analyzerNode);
        
        addNode(ENodeId.COVERAGE_STATS_NODE, cvrgNode);

        TreeNode<EditorSectionNode> profNode = 
                addNode(ENodeId.ANAL_PROFILER_NODE, analyzerNode);

        addNode(ENodeId.PROFILER_CODE_AREAS_NODE, profNode);
        addNode(ENodeId.PROFILER_DATA_AREAS_NODE, profNode);

        addNode(ENodeId.ANAL_TRACE_NODE, analyzerNode);
        
        addNode(ENodeId.HIL_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.SCRIPT_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.OPTIONS_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.DRY_RUN_NODE, unitTestTree, systemTestTree);
        addNode(ENodeId.DIAGRAMS, unitTestTree, systemTestTree);
        
    }
    

    private void createGroupNodes() {
        TreeNode<EditorSectionNode> groupTree = getTopNode(ESectionTreeType.EGROUP);
        addNode(ENodeId.GRP_META, groupTree);
        addNode(ENodeId.GRP_FILTER, groupTree);
        addNode(ENodeId.GRP_GROUP_STATS, groupTree);
        addNode(ENodeId.GRP_FUNC_STATS, groupTree);
        addNode(ENodeId.GRP_CVRG_CONFIG, groupTree);
        addNode(ENodeId.GRP_CVRG_STAT, groupTree);
        addNode(ENodeId.GROUP_SCRIPT_NODE, groupTree);
    }

    
    @SafeVarargs
    final private TreeNode<EditorSectionNode> addNode(ENodeId nodeId,
                                                      TreeNode<EditorSectionNode> ... parentNodes) {
        
        TreeNode<EditorSectionNode> treeNode = parentNodes[0].addChild(nodeId.getUiName());
        m_nodesMap.put(nodeId, treeNode);

        for (int idx = 1; idx < parentNodes.length; idx++) {
            parentNodes[idx].addChild(treeNode);
        }
        
        return treeNode;
    }
    
    
    public TreeNode<EditorSectionNode> getTopNode(ESectionTreeType sectionTreeType) {
        return m_sectionNodeTree.get(sectionTreeType);
    }
    
    
    public TreeNode<EditorSectionNode> getNode(ENodeId nodeId) {
        return m_nodesMap.get(nodeId);
    }

    
    public void setLabelProviderInput(TestSpecificationModel model, CTestTreeNode treeNode) {
        m_sectionTreeLabelProvider.setInput(model, treeNode);
    }


    public SectionTreeLabelProvider getSectionTreeLabelProvider() {
        return m_sectionTreeLabelProvider;
    }
}
