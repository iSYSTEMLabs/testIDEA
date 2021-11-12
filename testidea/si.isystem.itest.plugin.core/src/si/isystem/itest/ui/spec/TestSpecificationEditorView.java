package si.isystem.itest.ui.spec;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.WorkbenchPart;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.TSUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelVerifier;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;
import si.isystem.itest.ui.spec.group.GroupCoverageConfigEditor;
import si.isystem.itest.ui.spec.group.GroupCoverageStatEditor;
import si.isystem.itest.ui.spec.group.GroupFilterEditor;
import si.isystem.itest.ui.spec.group.GroupFuncStatsEditor;
import si.isystem.itest.ui.spec.group.GroupGroupStatsEditor;
import si.isystem.itest.ui.spec.group.GroupMetaEditor;
import si.isystem.itest.ui.spec.group.GroupScriptEditor;
import si.isystem.itest.ui.spec.sections.SectionTreeModel;
import si.isystem.itest.ui.spec.sections.SectionTreeModel.ESectionTreeType;
import si.isystem.itest.ui.spec.sections.SectionTreeView;
import si.isystem.ui.utils.FDGUIBuilder;

/**
 * This class implements tree view of test specification sections, for example:
 * - Init, 
 * - Functions, 
 * - Trace, 
 * - Coverage, 
 * - Profiler
 * - ...
 *  
 * @author markok
 */
public class TestSpecificationEditorView extends EditorPart {

    public static final String ID = "si.isystem.itest.ui.spec.tesSpecEditorView";

	private Composite m_editPanel;

    private StackLayout m_editorStack;
    

    private MetaSpecEditor m_metaSpecEditor;
    private FunctionSpecEditor m_functionSpecEditor;
    private SysTestStopConditionEditor m_sysTestBeginStopCondEditor;
    private SysTestStopConditionEditor m_sysTestEndStopCondEditor;
    private PersistentVarsEditor m_persistVarsEditor;
    private VariablesSpecEditor m_varsSpecEditor;
    private PreConditionsEditor m_preConditionsEditor;
    private ExpectedEditor m_expectedEditor;
    private ScriptsSpecEditor m_scriptSpecEditor;
    private StubSpecEditor m_stubsSpecEditor;
    private UserStubEditor m_userStubsEditor;
    private TestPointEditor m_testPointsEditor;
    private AnalyzerEditor m_analyzerEditor;
    private AnalyzerTraceEditor m_traceEditor;
    private AnalyzerProfilerEditor m_profilerEditor;
    private AnalyzerCoverageEditor m_coverageEditor;
    private CoverageStatisticsEditor m_coverageStatisticsEditor;
    private ProfilerCodeAreaEditor m_profilerCodeAreaEditor;
    private ProfilerCodeAreaEditor m_profilerDataAreaEditor;
    private OptionsSpecEditor m_optionsEditor;
    private HilSpecEditor m_hilParametersEditor;
    private DryRunEditor m_dryRunEditor;
    private DiagramEditor m_diagramEditor;

    private GroupMetaEditor m_grpMetaEditor;
    private GroupFilterEditor m_grpFilterEditor;
    private GroupCoverageStatEditor m_grpCvrgStatEditor;
    private GroupCoverageConfigEditor m_grpCvrgConfigEditor;
    private GroupGroupStatsEditor m_grpGroupStatsEditor;
    private GroupFuncStatsEditor m_grpFuncStatsEditor;
    private GroupScriptEditor m_grpScriptEditor;
    
    // These enums enable selection of correct pane in editor view on Undo.
    public enum ENodeId {
        UNIT_TEST_ROOT_NODE("uTest"),
        SYSTEM_TEST_ROOT_NODE("sTest"),
        GROUP_ROOT_NODE("grp"),
        
        META_NODE("Meta"), 
        FUNCTION_NODE("Function"), 
        SYS_TEST_BEGIN_STOP_COND_NODE("System init"), 
        SYS_TEST_END_STOP_COND_NODE("Execute test"), 
        PERSISTENT_VARS_NODE("Persistent variables"), 
        VARS_NODE("Variables"), 
        PRE_CONDITIONS_NODE("Pre-conditions"), 
        EXPECTED_NODE("Expected"), 
        STUBS_NODE("Stubs"), 
        STUBS_USER_NODE("User Stubs"), 
        TEST_POINT_NODE("Test Points"),
        ANALYZER_NODE("Analyzer"), 
        ANAL_COVERAGE_NODE("Coverage"), 
        COVERAGE_STATS_NODE("Statistics"), 
        ANAL_PROFILER_NODE("Profiler"),
        PROFILER_CODE_AREAS_NODE("Code areas"), 
        PROFILER_DATA_AREAS_NODE("Data areas"),
        ANAL_TRACE_NODE("Trace"), 
        HIL_NODE("HIL"), 
        SCRIPT_NODE("Scripts"), 
        OPTIONS_NODE("Options"), 
        DRY_RUN_NODE("Dry run"), 
        DIAGRAMS("Diagrams"),
        GRP_META("Meta"),
        GRP_FILTER("Filter"),
        GRP_CVRG_STAT("Coverage results"),
        GRP_GROUP_STATS("Group statistics"),    
        GRP_CVRG_CONFIG("Coverage config."), 
        GRP_FUNC_STATS("Function statistics"),  
        GROUP_SCRIPT_NODE("Scripts");
        
        String m_uiName;
        
        private ENodeId(String uiName) {
            m_uiName = uiName;
        }

        
        public String getUiName() {
            return m_uiName;
        }        
    }
    
    private CTestTreeNode m_inputTreeNode;
    private CTestSpecification m_mergedTestSpec;

    private ISectionEditor m_currentEditor;

    private WorkbenchPart m_testCaseEditorPart;

    private TestSpecificationModel m_model;

    private SectionTreeView m_sectionTreeView;
    private SectionTreeModel m_sectionTreeModel;
    
	public TestSpecificationEditorView(WorkbenchPart testCaseEditorPart, 
	                                   SectionTreeModel sectionTreeModel) {
	    
	    m_testCaseEditorPart = testCaseEditorPart; // needed to register context menus in KTables
	    m_sectionTreeModel = sectionTreeModel;
	}
	
	
    /**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
    @Override
	public void createPartControl(Composite parent) {
        Activator.log(Status.INFO, "TestSpecificationEditorView - createPartControl() S", null);
        
        FormLayout mainLayout = new FormLayout();
        parent.setLayout(mainLayout);

        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(sash);
        
        sash.SASH_WIDTH = 3;
        
        Composite treePanel = new Composite(sash, SWT.BORDER);
        m_editPanel = new Composite(sash, SWT.BORDER);
        
        treePanel.setLayout(new FormLayout());
        m_sectionTreeView = new SectionTreeView(treePanel, false, 
                                                m_sectionTreeModel.getSectionTreeLabelProvider());

        FormData fd = new FormData();
        fd.left = new FormAttachment(treePanel, 0);
        fd.top = new FormAttachment(0, SectionTreeView.TOP_INSET);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        m_sectionTreeView.setLayoutData(fd);

        m_editorStack = new StackLayout();
        m_editPanel.setLayout(m_editorStack);
        
        m_metaSpecEditor = new MetaSpecEditor();
        m_currentEditor = m_functionSpecEditor = new FunctionSpecEditor();
        m_sysTestBeginStopCondEditor = 
                new SysTestStopConditionEditor(SectionIds.E_SECTION_BEGIN_STOP_CONDITION,
                                               ENodeId.SYS_TEST_BEGIN_STOP_COND_NODE);
        m_sysTestEndStopCondEditor = 
                new SysTestStopConditionEditor(SectionIds.E_SECTION_END_STOP_CONDITION,
                                               ENodeId.SYS_TEST_END_STOP_COND_NODE);
        m_persistVarsEditor = new PersistentVarsEditor();
        m_varsSpecEditor = new VariablesSpecEditor();
        m_preConditionsEditor = new PreConditionsEditor();
        m_expectedEditor = new ExpectedEditor();
        m_scriptSpecEditor = new ScriptsSpecEditor();
        m_stubsSpecEditor = new StubSpecEditor(m_testCaseEditorPart);
        m_userStubsEditor = new UserStubEditor();
        m_testPointsEditor = new TestPointEditor(m_testCaseEditorPart);
        /* m_traceSpecEditor = new TraceSpecEditor(TraceSpecEditor.createControlIdMapForTrace(),
                                                ENodeId.TRACE_NODE,
                                                SectionIds.E_SECTION_TRACE); */
        m_analyzerEditor = new AnalyzerEditor();
        m_traceEditor = new AnalyzerTraceEditor();
        m_coverageEditor = new AnalyzerCoverageEditor();
        m_profilerEditor = new AnalyzerProfilerEditor();
        
        m_coverageStatisticsEditor = new CoverageStatisticsEditor();
        //m_coverageSpecEditor = new CoverageSpecEditor(m_coverageStatisticsEditor);
        m_coverageStatisticsEditor.setParentEditor(m_coverageEditor);
        
        m_profilerCodeAreaEditor = new ProfilerCodeAreaEditor();
        m_profilerDataAreaEditor = new ProfilerDataAreaEditor();
        // m_profilerSpecEditor = new ProfilerSpecEditor(m_profilerCodeAreaEditor, m_profilerDataAreaEditor);
        m_profilerCodeAreaEditor.setParentEditor(m_profilerEditor);
        m_profilerDataAreaEditor.setParentEditor(m_profilerEditor);
        
        m_optionsEditor = new OptionsSpecEditor();
        m_hilParametersEditor = new HilSpecEditor();
        m_dryRunEditor = new DryRunEditor();
        m_diagramEditor = new DiagramEditor(m_testCaseEditorPart);

        Activator.log(Status.INFO, "TestSpecificationEditorView - createPartControl() M1", null);
        
        setEditor(ENodeId.META_NODE, m_metaSpecEditor);
        setEditor(ENodeId.FUNCTION_NODE, m_functionSpecEditor);

        setEditor(ENodeId.SYS_TEST_BEGIN_STOP_COND_NODE, m_sysTestBeginStopCondEditor);
        setEditor(ENodeId.SYS_TEST_END_STOP_COND_NODE, m_sysTestEndStopCondEditor);
        setEditor(ENodeId.PERSISTENT_VARS_NODE, m_persistVarsEditor);
        setEditor(ENodeId.VARS_NODE, m_varsSpecEditor);
        setEditor(ENodeId.PRE_CONDITIONS_NODE, m_preConditionsEditor);
        setEditor(ENodeId.EXPECTED_NODE, m_expectedEditor);
        setEditor(ENodeId.STUBS_NODE, m_stubsSpecEditor);
        setEditor(ENodeId.STUBS_USER_NODE, m_userStubsEditor);
        setEditor(ENodeId.TEST_POINT_NODE, m_testPointsEditor);

        setEditor(ENodeId.ANALYZER_NODE, m_analyzerEditor);

        setEditor(ENodeId.ANAL_COVERAGE_NODE, m_coverageEditor);

        setEditor(ENodeId.COVERAGE_STATS_NODE, m_coverageStatisticsEditor);

        setEditor(ENodeId.ANAL_PROFILER_NODE, m_profilerEditor);

        setEditor(ENodeId.PROFILER_CODE_AREAS_NODE, m_profilerCodeAreaEditor);
        setEditor(ENodeId.PROFILER_DATA_AREAS_NODE, m_profilerDataAreaEditor);

        setEditor(ENodeId.ANAL_TRACE_NODE, m_traceEditor);

        setEditor(ENodeId.HIL_NODE, m_hilParametersEditor);
        setEditor(ENodeId.SCRIPT_NODE, m_scriptSpecEditor);
        setEditor(ENodeId.OPTIONS_NODE, m_optionsEditor);
        setEditor(ENodeId.DRY_RUN_NODE, m_dryRunEditor);
        setEditor(ENodeId.DIAGRAMS, m_diagramEditor);

        Activator.log(Status.INFO, "TestSpecificationEditorView - createPartControl() M2", null);

        createGroupEditorTree();

        m_sectionTreeView.addContextMenu(getSite(), SWTBotConstants.BOT_EDITOR_TREE_ID);
//        MenuManager menuManager = new MenuManager();
//        Menu menu = menuManager.createContextMenu(m_editorTreeViewer.getControl());
//        m_editorTreeViewer.getControl().setMenu(menu);
//        m_editorTreeViewer.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, SWTBotConstants.BOT_EDITOR_TREE_ID);
//
//        IWorkbenchPartSite site = getSite();
//        if (site != null) { // should never be null
//            site.registerContextMenu("si.isystem.itest.testEditorSectionCtxMenu", 
//                                     menuManager, 
//                                     m_editorTreeViewer);
//        }
        
        m_sectionTreeView.setInput(m_sectionTreeModel.getTopNode(ESectionTreeType.EUNIT_TEST));
        // m_editorStack.topControl = funcSpecPage;
        // m_editPanel.layout();
        
		getSite().setSelectionProvider(m_sectionTreeView.getSelectionProvider());
		
		hookClickCommand();
        sash.setWeights(new int[]{20, 80});
		
        setSelection(ENodeId.FUNCTION_NODE);
        
       /* List<CTestSpecification> selectedTestSpecs = UiUtils.getSelectedTestTreeSpecifications();
        if (selectedTestSpecs.size() == 1) {
            // show data in section editor view only when there is exactly one test spec selected
            // if more test specs are selected, and only one is shown, it may confuse the user  
            setInput(selectedTestSpecs.get(0));
        }
        
        setInput(null); */
        Activator.log(Status.INFO, "TestSpecificationEditorView - createPartControl() E", null);
	}

    
    private void setEditor(ENodeId nodeId, AbstractSectionEditor sectionEditor) {
        try {
            EditorSectionNode editorNode = new EditorSectionNode(sectionEditor);
            m_sectionTreeModel.getNode(nodeId).setData(editorNode);
        } catch (Exception ex) {
            // make sure that creation error does not crash the application, and
            // provide error info
            ex.printStackTrace();
            SExceptionDialog.open(Activator.getShell(), "Can not open editor!", ex);
        }
    }
    
    
    private void createGroupEditorTree() {

        m_grpMetaEditor = new GroupMetaEditor(ENodeId.GRP_META, 
                                              ESectionCTestGroup.E_SECTION_GROUP_ID,
                                              ESectionCTestGroup.E_SECTION_DESCRIPTION);
        
        m_grpFilterEditor = new GroupFilterEditor(ENodeId.GRP_FILTER, 
                                                  ESectionCTestGroup.E_SECTION_FILTER); 

        m_grpCvrgConfigEditor = new GroupCoverageConfigEditor(); 

        m_grpCvrgStatEditor = new GroupCoverageStatEditor(ENodeId.GRP_CVRG_STAT, 
                                                          ESectionCTestGroup.E_SECTION_COVERAGE_ALL_CODE_IN_GROUP,
                                                          ESectionCTestGroup.E_SECTION_COVERAGE_TEST_CASES_ONLY); 

        m_grpGroupStatsEditor = new GroupGroupStatsEditor(ENodeId.GRP_GROUP_STATS);
        
        m_grpFuncStatsEditor = new GroupFuncStatsEditor(ENodeId.GRP_FUNC_STATS);
        
        m_grpScriptEditor = new GroupScriptEditor();
        
        setEditor(ENodeId.GRP_META, m_grpMetaEditor);
        setEditor(ENodeId.GRP_FILTER, m_grpFilterEditor);
        setEditor(ENodeId.GRP_GROUP_STATS, m_grpGroupStatsEditor);
        setEditor(ENodeId.GRP_FUNC_STATS, m_grpFuncStatsEditor);
        setEditor(ENodeId.GRP_CVRG_CONFIG, m_grpCvrgConfigEditor);
        setEditor(ENodeId.GRP_CVRG_STAT, m_grpCvrgStatEditor);
        setEditor(ENodeId.GROUP_SCRIPT_NODE, m_grpScriptEditor);
    }


    public CTestTreeNode getInput() {
        return m_inputTreeNode;
    }
    
    
	public void setInput(TestSpecificationModel model, CTestTreeNode treeNode) {
	    
	    m_model = model;
	    m_sectionTreeModel.setLabelProviderInput(m_model, treeNode);
	    
	    // treeNode == null when user selects more than one node in Overview
	    // view - testIDEA disables the editor in such cases. Use
	    // m_inputTreeNode to disable UI controls in the right editor.
	    if (m_inputTreeNode != null  &&  treeNode == null) {
	        
            boolean isFilterGroup = TSUtils.isFilterGroup(m_inputTreeNode);
            m_inputTreeNode = null;
            if (isFilterGroup) {
                setInputGroup(model, null, null);
            } else {
                setInputTestSpec(model, null, null);
            }
            
	    } else {
	        m_inputTreeNode = treeNode;

	        if (treeNode != null) {
	            if (TSUtils.isFilterGroup(treeNode)) {
	                setInputGroup(model, treeNode, null);
	            } else {
	                setInputTestSpec(model, 
	                                 TSUtils.getTestSpec(treeNode), 
	                                 null);
	            }
	        }
	    }
	}

	
	private void setInputGroup(TestSpecificationModel model, 
	                           CTestTreeNode treeNode, 
	                           ENodeId nodeId) {
	    
        CTestGroup testGroup = CTestGroup.cast(treeNode);
	    m_grpMetaEditor.setInputGroup(model, testGroup);
        m_grpFilterEditor.setInputGroup(model, testGroup);
        m_grpCvrgStatEditor.setInputGroup(model, testGroup);
        m_grpCvrgConfigEditor.setInputGroup(model, testGroup);
        m_grpGroupStatsEditor.setInputGroup(model, testGroup);
        m_grpFuncStatsEditor.setInputGroup(model, testGroup);
        m_grpScriptEditor.setInputGroup(model, testGroup);

        setTreeSelection(testGroup, nodeId);
        m_sectionTreeView.setEnabled(true);
        
        try {
            m_currentEditor.fillControlls();
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "System error: Can not retrieve data from specification!", ex);
        }
        
    }


    private void setInputTestSpec(TestSpecificationModel model, 
                                  CTestTreeNode treeNode,  // must be instance of CTestSpecification
                                  ENodeId nodeId) {

        m_mergedTestSpec = null;
        
        CTestSpecification testSpec = CTestSpecification.cast(treeNode);
        
        if (m_inputTreeNode != null) {
            CTestSpecification parent = testSpec.getParentTestSpecification(); 
            if (parent != null) {
                m_mergedTestSpec = parent.merge();
            }
        }
        
        m_metaSpecEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_functionSpecEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_sysTestBeginStopCondEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_sysTestEndStopCondEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_persistVarsEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_varsSpecEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_preConditionsEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_expectedEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_scriptSpecEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_stubsSpecEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_userStubsEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_testPointsEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_analyzerEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_traceEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_coverageEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_coverageStatisticsEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_profilerEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_profilerCodeAreaEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_profilerDataAreaEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_optionsEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_hilParametersEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_dryRunEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        m_diagramEditor.setInputTestSpec(model, testSpec, m_mergedTestSpec);
        
        if (testSpec != null) {
            try {
                ModelVerifier.INSTANCE.verifyTestTreeNodeAndSetStatus(testSpec.merge());
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), 
                                      "Verification failed!", ex);
            }
	        
            setTreeSelection(testSpec, nodeId);
	        
            m_sectionTreeView.setEnabled(true);
	    } else {
	        m_sectionTreeView.refresh(); // refresh tree icons, which depend on m_inputTestSpec
            // setTreeSelection(null, ENodeId.FUNCTION_NODE); commented to keep the current editor
            m_sectionTreeView.setEnabled(false);
	    }
	    
	    try {
	        
            // System.out.println("setInput 1, main: " + Activator.getTimeFromStart());
	        
	        m_currentEditor.fillControlls();
	        
            // System.out.println("setInput 2, main: " + Activator.getTimeFromStart());
            
	    } catch (Exception ex) {
	        SExceptionDialog.open(Activator.getShell(), "System error: Can not retrieve data from specification!", ex);
	    }
	}
	

    public void testSpecTreeStructureChanged(TestSpecificationModel model) {
        m_sectionTreeView.refresh();

        if (m_inputTreeNode != null) { // can be null if 0 or more than one test case is selected
            
            // refreshes results at the end of test
            if (TSUtils.isFilterGroup(m_inputTreeNode)) {
                m_currentEditor.setInputGroup(model, CTestGroup.cast(m_inputTreeNode));
            } else {
                CTestSpecification testSpec = TSUtils.getTestSpec(m_inputTreeNode);
                m_currentEditor.setInputTestSpec(model, 
                                                 testSpec, 
                                                 m_mergedTestSpec);
            }
        }
        
        m_currentEditor.fillControlls();
    }
    

    public void testSpecDataChanged(TestSpecificationModel model, ModelChangedEvent event) {
        
        CTestTreeNode treeNode = event.getNewSpec();
        m_inputTreeNode = treeNode;
        m_sectionTreeModel.setLabelProviderInput(m_model, treeNode);

        if (treeNode != null) {
            if (TSUtils.isFilterGroup(treeNode)) {
                setInputGroup(model, treeNode, event.getNodeId());
            } else {
                CTestSpecification testSpec = TSUtils.getTestSpec(treeNode);
                setInputTestSpec(model, testSpec, event.getNodeId());
            }
        }
        
        // m_editorTreeViewer.refresh();
        
        // If node is selected here, then when user enters text into for example
        // test id, and clicks for example node Trace while test id text field
        // still has focus, then the transition to Trace node is not made because
        // of this selection! And that's annoying.
        // Added later: On the other hand, this statement selects the right pane on undo
        // actions.
        // setTreeSelection(event.getNodeId(), sectionsTree);
    }
    
    
    /**
     * If the tree corresponding to node type is not visible:
     * - saves selection in the current tree 
     * - makes the tree for node type visible.
     * 
     * Unconditionally selects the given node. 
     * 
     * @param testNode Must be instance of CTestGroup or CTestSpecification in JAVA domain!!!
     * @param nodeId if null, selects previously stored selection for the tree, 
     *               which was made visible. If not null, the given node is selected.

     */
    public void setTreeSelection(CTestTreeNode testNode, ENodeId nodeId) {

        selectSectionsTree(testNode, nodeId == null);
        selectSectionNode(nodeId);
    }
    
    
    private void selectSectionsTree(CTestTreeNode testNode, boolean isRestoreSelection) {

        ESectionTreeType treeeType = m_sectionTreeView.getSectionTreeType(testNode);
        
        if (treeeType == null) {
            setSectionTreeInput(ESectionTreeType.EUNIT_TEST, false);
            return;
        } 
        
        EditorSectionNode selectedSection = setSectionTreeInput(treeeType, 
                                                                isRestoreSelection);
        if (selectedSection != null) {
            m_currentEditor = selectedSection.getSectionEditor(m_editPanel);
        }
    }
    
    
    private EditorSectionNode setSectionTreeInput(ESectionTreeType treeType, 
                                                  boolean isRestoreSelection) {
        
        TreeNode<EditorSectionNode> node = m_sectionTreeModel.getTopNode(treeType);
        return m_sectionTreeView.setVisibleSectionsTree(node, isRestoreSelection);
    }

    
    private void selectSectionNode(ENodeId nodeId) {
        
        if (nodeId == null) {
            return;
        }
        
        TreeNode<EditorSectionNode> node = m_sectionTreeModel.getNode(nodeId);
        m_sectionTreeView.setSelection(new StructuredSelection(node));
        m_currentEditor = node.getData().getSectionEditor(m_editPanel);
    }
    

    private void setSelection(ENodeId nodeId) {
        TreeNode<EditorSectionNode> node = m_sectionTreeModel.getNode(nodeId);
        m_sectionTreeView.setSelection(new StructuredSelection(node));
    }


    public void setSelection(ENodeId nodeId, boolean reveal) {
        TreeNode<EditorSectionNode> node = m_sectionTreeModel.getNode(nodeId);
        m_sectionTreeView.setSelection(new StructuredSelection(node), reveal);
    }

    
    public void setTreeSelection(ISelection selection) {
        m_sectionTreeView.setSelection(selection, true);
    }
    
    
    public ISelection getTreeSelection() {
        return m_sectionTreeView.getSelection();
    }
    
    /**
     * 
     * @param topNode
     * @param isRestoreSelection if true, and other tree input is selected as currently
     * visible, then selection stored when this input was hidden is restored. This way
     * user can switch test cases / groups and always see the same node.
     */
//    private void setVisibleSectionsTree(ESectionTreeType nodeId, 
//                                        boolean isRestoreSelection) {
//
//        TreeNode<EditorSectionNode> topNode = m_sectionTree.getTopNode(nodeId);
//        Object sectionsTreeInput = m_sectionTree.getInput();
//        
//        // call setInput() only when really necessary, since it collapses the tree
//        if (sectionsTreeInput != topNode) { 
//            // save selection in current tree
//            ISelection selection = m_sectionTree.getSelection();
//            m_sectionsTreeSelections.put(sectionsTreeInput, selection);
//            m_sectionTree.setInput(topNode);
//            
//            if (isRestoreSelection) {
//                // restore selection in visible tree
//                TreeSelection newSelection = (TreeSelection)m_sectionsTreeSelections.get(topNode);
//                if (newSelection != null) {
//                    m_sectionTree.setSelection(newSelection, true);
//                    Object selectedNode = newSelection.getFirstElement();
//                    if (selectedNode != null  &&  selectedNode instanceof TreeNode<?>) {
//                        m_currentEditor = ((TreeNode<EditorSectionNode>)selectedNode).getData().getSectionEditor();
//                    }
//                }
//            }
//        } else {
//            m_sectionTree.refresh();
//        }
//    }
    
    
	// see javadoc for TreeViewer.setSelection()
//    private void setTreeSelection(TreeNode<EditorSectionNode> topNode) {
//        m_sectionTree.setSelection(new StructuredSelection(topNode));
//    }


    private void hookClickCommand() {
        m_sectionTreeView.addSelectionChangedListener(new ISelectionChangedListener() {
            
            // private boolean m_isExceptionThrown = false;

            @SuppressWarnings("unchecked")
            @Override
            public void selectionChanged(SelectionChangedEvent event) {

                TreeSelection selection =  (TreeSelection)event.getSelection();
                if (!selection.isEmpty()) {
                    
                    // get the new selected node
                    TreeNode<EditorSectionNode> node = (TreeNode<EditorSectionNode>)selection.getFirstElement();
                    
                    if (node.getName().equals("Function")  &&  m_inputTreeNode != null  &&  
                            !TSUtils.isFilterGroup(m_inputTreeNode)) {
                        
                        CTestSpecification treeNode = TSUtils.getTestSpec(m_inputTreeNode);
                        m_functionSpecEditor.fillParamsAutoCompleteField(treeNode);
                    }

                    m_currentEditor = node.getData().getSectionEditor(m_editPanel);
                    m_currentEditor.fillControlls();
                    
                    Composite page = node.getData().getPanel(m_editPanel);
                    if (page != null) {
                        m_editorStack.topControl = page;
                        m_editPanel.layout();
                    }
                    m_sectionTreeView.refresh();
                }
            }
        });
    }

	
	/**
	 * Passing the focus request to the viewer's control.
	 */
    @Override
	public void setFocus() {
	    m_sectionTreeView.setFocus();
	}
	
	
	/** 
	 * Returns reference to instance of this view or null if the view is not
 	 * visible. 
	 */
/*	public static TestSpecificationEditorView getView() {
	    TestSpecificationEditorView testSpecView = (TestSpecificationEditorView) 
	    Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().
	    getActivePage().findView(TestSpecificationEditorView.ID);

	    return testSpecView;
	}
*/	
	
	/**
	 * Saves data from GUI controls to selected test configuration.
	public static void saveGUIData() {

        TestSpecificationEditorView testSpecView = TestSpecificationEditorView.getView();
        
        if (testSpecView != null) {  // if the view is visible, save the GUI data
            List<CTestSpecification> selectedTestSpecs = UiUtils.getSelectedTestTreeSpecifications();
            if (selectedTestSpecs.size() == 1) {
                CTestSpecification testSpec = selectedTestSpecs.get(0);
                testSpecView.copyGUIDataToTestSpec(testSpec);
            }
        }
	}
     */
	
    public void refreshGlobals() {

        CTestSpecification testSpec = TSUtils.getTestSpec(m_inputTreeNode);
        
        String coreId = "";
        if (testSpec != null) {
            coreId = testSpec.getCoreId();
        }
        
        if (m_model != null) {
            coreId = m_model.getConfiguredCoreID(coreId);
            m_functionSpecEditor.refreshGlobals(coreId);
            m_varsSpecEditor.refreshGlobals(coreId);
            m_expectedEditor.refreshGlobals(coreId);
            m_preConditionsEditor.refreshGlobals(coreId);
            m_stubsSpecEditor.refreshGlobals();
            m_userStubsEditor.refreshGlobals();
            m_coverageStatisticsEditor.refreshGlobals();
            m_profilerCodeAreaEditor.refreshGlobals();
            m_profilerDataAreaEditor.refreshGlobals();
            m_testPointsEditor.refreshGlobals();
            m_hilParametersEditor.refreshGlobals();
            m_dryRunEditor.refreshGlobals(coreId);
        }

    }


    public void selectLineInTable(int tableId, int lineNo) {
        if (m_currentEditor != null) {
            m_currentEditor.selectLineInTable(tableId, lineNo);
        }
    }


    @Override
    public void doSave(IProgressMonitor monitor) {
        System.out.println("EIP save");
    }


    @Override
    public void doSaveAs() {
        System.out.println("EIP save as");
    }


    @Override
    public boolean isDirty() {
        return m_model.isModelDirty();
    }


    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }


    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }   
}
