package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.ETristate;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EDerivedTestResultStatus;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.handlers.FileExportCmdHandler;
import si.isystem.itest.handlers.FileImportCmdHandler;
import si.isystem.itest.handlers.ToggleDryRunHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecComparer;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.data.TestSpecificationTreeDragListener;
import si.isystem.itest.ui.spec.data.TestSpecificationTreeDropListener;
import si.isystem.ui.utils.ColorProvider;

/**
 * This class adapts TestCaseEditorPart to Outline view. When user selects 
 * test case editor, Eclipse standard Outline view shows test case hierarchy - 
 * the same as Test Tree view.
 * 
 * @author markok
 *
 */
public class TestTreeOutline extends ContentOutlinePage {

    private OutlineViewLabelProvider m_labelProvider;

    private TestSpecificationTreeDragListener m_treeDragListener;
    private TestSpecificationTreeDropListener m_treeDropListener;
    private boolean m_isForwardSelectionToEditor = true;
    private TestSpecificationModel m_model;
    private CTestTreeNode m_selectedTreeNode;
    private ExportAction m_exportAction;
    private ImportAction m_importAction;
    private ExpandSelectedAndDerivedAction m_expandAction;
    private CollapseAction m_collapseAction;

    @Override
    public void createControl(Composite parent) {
        
        Activator.log(Status.INFO, "TestTreeOutline - createControl()", null);

        super.createControl(parent);
        
        if (ToggleDryRunHandler.isDryRunMode()) {
            setBackgroundColor(ColorProvider.instance().getColor(ColorProvider.RED));
        }
        
        int operations = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transferTypes = new Transfer[]{TextTransfer.getInstance(),
                                                  FileTransfer.getInstance()};
        m_treeDragListener = new TestSpecificationTreeDragListener();
        TreeViewer treeViewer = getTreeViewer();
        treeViewer.addDragSupport(operations, transferTypes, m_treeDragListener);
        m_treeDropListener = new TestSpecificationTreeDropListener(treeViewer, m_treeDragListener);
        treeViewer.addDropSupport(operations, transferTypes, m_treeDropListener);
                              
        ColumnViewerToolTipSupport.enableFor(treeViewer);

        treeViewer.setComparer(new TestSpecComparer());
        
        treeViewer.setContentProvider(new OutlineViewContentProvider());
        
        m_labelProvider = new OutlineViewLabelProvider();
        treeViewer.setLabelProvider(m_labelProvider);

        treeViewer.addSelectionChangedListener(this);

        MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
        treeViewer.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                          SWTBotConstants.BOT_TEST_TREE_ID);
        
        IPageSite site = getSite();
        if (site != null) { // should never be null
            site.registerContextMenu("si.isystem.itest.ui.spec.TestTreeView", menuManager, treeViewer);
        }
        
        setViewerInput(true);
    }


    @Override
    public void init(IPageSite pageSite) {
        super.init(pageSite);
        pageSite.setSelectionProvider(this);
    }

    
    @Override  // override to provide public access
    public TreeViewer getTreeViewer() {
        return super.getTreeViewer();
    }
    
    
    public void refresh() {
        
        TreeViewer treeViewer = getTreeViewer();
        
        if (treeViewer != null) {
            treeViewer.refresh();
        }
    }
    
    
    public void refresh(CTestTreeNode testSpecOrGrp) {
        
        TreeViewer treeViewer = getTreeViewer();
        
        if (treeViewer != null) {
            treeViewer.refresh(testSpecOrGrp);
        }
    }

    
    public void setInput(TestSpecificationModel model, 
                         CTestTreeNode selectedTestSpec) {
        boolean isRefreshRoot = model != m_model;
        setInput(model, selectedTestSpec, isRefreshRoot);
    }


    // if model gets reloaded, because file was changed by external application,
    // then isRefreshRoot must be true - the other overload is not useful as 
    // model object reference is still the same.
    public void setInput(TestSpecificationModel model, 
                         CTestTreeNode selectedTestSpec,
                         boolean isRefreshRoot) {
        m_model = model;
        m_selectedTreeNode = selectedTestSpec;
                
        setViewerInput(isRefreshRoot);
    }

    
    public TestSpecificationModel getInput() {
        return m_model;
    }
    

    private void setViewerInput(boolean isRefreshRoot) {
        
        TreeViewer treeViewer = getTreeViewer();
        if (treeViewer != null) {
            if (isRefreshRoot) {
                CTestBench testBench = null;
                if (m_model != null) {
                    testBench = m_model.getTestBench();
                    // System.out.println("root ts = " + rootTS.hashCode() + " / " + rootTS.hashCodeAsPtr());
                }
                // do not forward selection events as selection is made in the next 
                // step
                m_isForwardSelectionToEditor = false;
                if (!treeViewer.getControl().isDisposed()) { // happens on exit from application
                    treeViewer.setInput(testBench);
                }
                m_isForwardSelectionToEditor = true;
            }
            
            setSelection(m_model, m_selectedTreeNode);
        }
    }
    
    
    @Override
    public void setActionBars(IActionBars actionBars) {
     
        // enable edit menu commands when this outline is active 
        // TestCaseEditorPart tsEditor = TestCaseEditorPart.getActive();
        // CommandActionHandler.setActionHandlers(actionBars, tsEditor);
        
        IToolBarManager toolbar = actionBars.getToolBarManager();

        if (m_expandAction == null) {
            m_expandAction = new ExpandSelectedAndDerivedAction();
        }
        toolbar.add(m_expandAction);
        
        if (m_collapseAction == null) {
            m_collapseAction = new CollapseAction();
        }
        toolbar.add(m_collapseAction);
        
        if (m_exportAction == null) {
            m_exportAction = new ExportAction();
        }
        toolbar.add(m_exportAction);
        
        if (m_importAction == null) {
            m_importAction = new ImportAction();
        }
        toolbar.add(m_importAction);
        
        
    }

    
    /**
     * @param testNode test case to be selected. If null, the first test case in
     * the model is selected.
     */
    public void setSelection(TestSpecificationModel model, CTestTreeNode testNode) {

        if (model == null) {
            return;
        }
        
        if (testNode == null  ||  model.isRootTreeNode(testNode)) {
            // when base test spec is deleted, its parent is invisible root test spec,
            // so select one of visible specs.
            testNode = model.getFirstSelection();
        }
        
        // when testSpec == null, when there are no test specifications in the model
        TreeViewer treeViewer = getTreeViewer();
        if (testNode != null  &&  treeViewer != null) { 
            treeViewer.setSelection(new StructuredSelection(testNode), 
                                    true);  // reveal selection
        }
    }


    public void setBackgroundColor(Color color) {
        getControl().setBackground(color);
    }
    
    
    @Override
    public ISelection getSelection() {

        TreeViewer treeViewer = getTreeViewer();

        if (treeViewer != null) {
            return treeViewer.getSelection();
        }
        return StructuredSelection.EMPTY;
    }
    
    
    public CTestTreeNode getTestSpecSelection() {
        ITreeSelection selection = (ITreeSelection)getSelection();
        return (CTestTreeNode)selection.getFirstElement();
    }
    
    
    private void notifyActiveTestEditor(SelectionChangedEvent event) {
        
        TreeSelection selection = (TreeSelection)event.getSelection();
        
        TestCaseEditorPart editor = TestCaseEditorPart.getActive();

//        Activator.log(Status.INFO, 
//                      "notifyActiveTestEditor: editor " + (editor == null ? "== null" : "!= null"), 
//                      null);
        
        if (editor != null) {
            editor.setFormInputList(selection);
        }
    }
    
    
    @Override
    public void selectionChanged(SelectionChangedEvent selEvent) {

        super.selectionChanged(selEvent);
        
        final SelectionChangedEvent event = new SelectionChangedEvent(this,
                                                      selEvent.getSelection());
        if (m_isForwardSelectionToEditor) {
            notifyActiveTestEditor(event);
        }
    }


    public void selectAll() {
        
        TreeViewer treeViewer = getTreeViewer();
        
        if (treeViewer != null) {
            treeViewer.getTree().selectAll();
        }
    }
    
    enum EExpandMode {ALL, SELECTED_FULL, SELECTED_AND_FIRST_LEVEL};
    
    public void expandNodes(EExpandMode expandMode) {
        TreeViewer treeViewer = getTreeViewer();
        
        if (treeViewer != null) {
            List<CTestTreeNode> selectedNodes = UiUtils.getSelectedTestTreeNodes();

            switch (expandMode) {
            case ALL:
                treeViewer.expandAll();
                break;
            case SELECTED_AND_FIRST_LEVEL:
                for (CTestTreeNode node : selectedNodes) {
                    treeViewer.expandToLevel(node, 2);
                }
                break;
            case SELECTED_FULL:
                for (CTestTreeNode node : selectedNodes) {
                    treeViewer.expandToLevel(node, TreeViewer.ALL_LEVELS);
                }
                break;
            default:
                System.err.println("Unknown expand mode: " + expandMode);
                break;
            }
        }        
    }
    

    public void collapseAll() {
        TreeViewer treeViewer = getTreeViewer();
        
        if (treeViewer != null) {
            treeViewer.collapseAll();
        }
    }
    
    
    class ExpandSelectedAndDerivedAction extends Action {
        
        private ImageDescriptor m_imgDescriptor;

        ExpandSelectedAndDerivedAction() {
            m_imgDescriptor = 
                    AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                            "icons/expandall.png");

            if (m_imgDescriptor != null) {
                setImageDescriptor(m_imgDescriptor);
            }
            
            setText("Expand");
            setToolTipText("Expands all nodes.\n"
                    + "CTRL - expands selected nodes\n"
                    + "SHIFT - expands selected and first level derived nodes");
        }
        
        
        @Override
        public void runWithEvent(Event event) {
            try {
                EExpandMode expandMode = EExpandMode.ALL;

                int accelerator = event.stateMask;
                if (accelerator == SWT.CTRL) {
                    expandMode = EExpandMode.SELECTED_FULL;
                }
                if (accelerator == SWT.SHIFT) {
                    expandMode = EExpandMode.SELECTED_AND_FIRST_LEVEL;
                }

                expandNodes(expandMode);
                
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), "Expand failed!", ex);
            }
        }
    }


    class CollapseAction extends Action {
        
        private ImageDescriptor m_imgDescriptor;

        CollapseAction() {
            m_imgDescriptor = 
                    AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                            "icons/collapseall.png");

            if (m_imgDescriptor != null) {
                setImageDescriptor(m_imgDescriptor);
            }
            
            setText("Collapse");
            setToolTipText("Collapses all nodes");
        }
        
        
        @Override
        public void run() {
            try {
                collapseAll();
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), "Collapse failed!", ex);
            }
        }
    }    
}


class OutlineViewContentProvider implements ITreeContentProvider {

    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getElements(Object input) {

        CTestBench testBench = (CTestBench)input;
        CTestSpecification rootTS = testBench.getTestSpecification(true);
        CTestGroup rootTG = testBench.getGroup(true);
        
        CTestBaseList groupsList = rootTG.getChildren(true);
        int noOfGroups = (int) groupsList.size();
        int noOfTestSpecs = rootTS.getNoOfDerivedSpecs();
        Object []children = new Object[noOfTestSpecs + noOfGroups];
        
        for (int idx = 0; idx < noOfGroups; idx++) {
            children[idx] = CTestTreeNode.cast(groupsList.get(idx));
        }
        
        for (int idx = 0; idx < noOfTestSpecs; idx++) {
            children[noOfGroups + idx] = CTestTreeNode.cast(rootTS.getDerivedTestSpec(idx));
        }
        
        return children;
    }


    @Override
    public Object[] getChildren(Object parent) {
        
        CTestTreeNode testNode = (CTestTreeNode)parent;

        CTestBaseList groupsList = testNode.getChildren(true);
        int noOfGroups = (int) groupsList.size();
        int nodesSize = (int) noOfGroups;
        
        // add also number of groups owning test cases, which are not 'normal' children
        int testSpecsListSize = 0;
        CTestGroup group = null;
        if (testNode.isGroup()) {
            group = CTestGroup.cast(testNode);
            testSpecsListSize = (int) group.getTestOwnerGroupsSize();
            nodesSize += testSpecsListSize;
        }
        
        Object []children = new Object[nodesSize];
        
        for (int idx = 0; idx < noOfGroups; idx++) {
            children[idx] = CTestTreeNode.cast(groupsList.get(idx));
        }
        
        if (group != null) {
            for (int idx = 0; idx < testSpecsListSize; idx++) {
                children[noOfGroups + idx] = group.getTestOwnerGroup(idx);
            }
        }
        
        return children;
    }

    
    @Override
    public Object getParent(Object element) {
//        System.out.println("class = " + element.getClass().getSimpleName()); // + '\n' + 
//                           // ((CTestBase)element).getParent().toString());
        return CTestTreeNode.cast( ((CTestBase)element).getParent() );
    }

    
    @Override
    public boolean hasChildren(Object element) {
        CTestTreeNode treeNode = (CTestTreeNode)element;
        if (treeNode.isGroup()) {
            CTestGroup group = CTestGroup.cast(treeNode);
            return group.hasChildren()  ||  group.hasTestSpecs();
        } else {
            return treeNode.hasChildren();
        }
    }
}


class OutlineViewLabelProvider extends CellLabelProvider implements ILabelProvider {

    private List<ILabelProviderListener> m_listeners = new ArrayList<ILabelProviderListener>();
    
    @Override
    public String getText(Object element) {
        return ((CTestTreeNode)element).getUILabel();
    } 

    /**
     * This method currently updates only text and image. For other attributes (font, colors, ...)
     * see org.eclipse.jface.viewers.WrappedViewerLabelProvider and implement the features here.
     */
    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        cell.setText(((CTestTreeNode)element).getUILabel());
        cell.setImage(getImage(element));
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        m_listeners.add(listener);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        m_listeners.remove(listener);
    }

    @Override
    public Image getImage(Object element) {

        CTestTreeNode treeNode = (CTestTreeNode)element;
        
        if (treeNode.isGroup()) {
            CTestGroup group = CTestGroup.cast(treeNode);
            if (group.isTestSpecOwner()) {
                if (group.isBelongsToFilterGroup()) {
                    return getGroupOwnedTestSpecImage(group);
                    // return getTestSpecImage(group.getOwnedTestSpec());
                } else {
                    return IconProvider.INSTANCE.getIcon(EIconId.ETestSpecNotInGroup);
                }
            } else {
                return getGroupImage(group);
            }
        } else {
            return getTestSpecImage(CTestSpecification.cast(treeNode));
        }
    }
    
    
    private Image getGroupImage(CTestGroup group) {
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            CTestReportContainer reportContainer = model.getTestReportContainer();
            CTestGroupResult result = reportContainer.getGroupResult(group);
            Boolean isGroupError = null;
            if (result != null) {
                isGroupError = result.isError();
                EDerivedTestResultStatus childStatus = EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_HAS_RESULT;
                if (result.isErrorInChildTestCases()  ||  result.isChildGroupError()) {
                    childStatus = EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_FAILED;
                }
                return IconProvider.INSTANCE.getGroupIcon(isGroupError, childStatus);
            } 
        }

        return IconProvider.INSTANCE.getGroupIcon(null, 
                                                  EDerivedTestResultStatus.NO_RESULTS);
    }
    

    private Image getGroupOwnedTestSpecImage(CTestGroup group) {
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        
        EDerivedTestResultStatus derivedTestResultStatus = 
                isErrorInDerivedOwnerGroups(model, group, EDerivedTestResultStatus.NO_RESULTS);
        
        return getTestSpecImage(model, group.getOwnedTestSpec(), derivedTestResultStatus);
    }
    
    
    private Image getTestSpecImage(CTestSpecification testSpec) {
    
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        // ImageDescriptor currentTestResultStatusIconTopLeft = null;
        // ImageDescriptor derivedTestsResultStatusIconTopRight = null;

        // status of derived tests is obtained here, to set icon in the top right corner
        // if this is going to be to slow for large test specifications with many
        // derived tests, store the status of derived tests into test specification.
        // CTestResult is not appropriate for storing of this information, since not
        // each test spec may be executed, either because it is abstract or only derived
        // tests were selected.
        EDerivedTestResultStatus derivedTestResultStatus = 
                   isErrorInDerivedTests(model, testSpec, EDerivedTestResultStatus.NO_RESULTS);
        
        return getTestSpecImage(model, testSpec, derivedTestResultStatus);
    }
    
    
    private Image getTestSpecImage(TestSpecificationModel model,
                                   CTestSpecification testSpec, 
                                   EDerivedTestResultStatus derivedTestResultStatus) {

        
        boolean isRunnable = testSpec.getRunFlag() != ETristate.E_FALSE;
        Image overlayIcon;
        
        if (isRunnable) {
            CTestResult result = model != null ? model.getResult(testSpec) : null;

            overlayIcon = IconProvider.INSTANCE.getTreeViewIcon(isRunnable, 
                                                 testSpec.getMergedTestScope(),
                                                 result == null ? null : Boolean.valueOf(result.isError()),  
                                                 derivedTestResultStatus);
        } else {
            overlayIcon = IconProvider.INSTANCE.getTreeViewIcon(isRunnable, 
                                                 testSpec.getMergedTestScope(),
                                                 null,  
                                                 derivedTestResultStatus);
        }
        
        return overlayIcon;
    }

    
    private EDerivedTestResultStatus isErrorInDerivedTests(TestSpecificationModel model,
                                                           CTestSpecification testSpec, 
                                                           EDerivedTestResultStatus testResultStatus) {
        if (model == null) {
            return testResultStatus;
        }
        
        int noOFDerivedTests = testSpec.getNoOfDerivedSpecs();
        
        for (int i = 0; i < noOFDerivedTests; i++) {
            CTestSpecification ts = testSpec.getDerivedTestSpec(i);
            
            testResultStatus = isErrorInDerivedTests(model, ts, testResultStatus);
            
            if (testResultStatus == EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_FAILED) {
                break;  // we've found error, no need to continue searching
            }
            
            CTestResult result = model.getResult(ts);
            if (result != null) {
                testResultStatus = EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_HAS_RESULT;
                if (result.isError()) {
                    testResultStatus = EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_FAILED;
                    break;
                }
            }
        }
        
        return testResultStatus;
    }

    
    private EDerivedTestResultStatus isErrorInDerivedOwnerGroups(TestSpecificationModel model,
                                                                 CTestGroup group, 
                                                                 EDerivedTestResultStatus testResultStatus) {
        if (model == null) {
            return testResultStatus;
        }
        
        int noOFDerivedTests = (int) group.getTestOwnerGroupsSize();

        for (int i = 0; i < noOFDerivedTests; i++) {
            CTestGroup childGroup = group.getTestOwnerGroup(i);

            testResultStatus = isErrorInDerivedOwnerGroups(model, childGroup, testResultStatus);

            if (testResultStatus == EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_FAILED) {
                break;  // we've found error, no need to continue searching
            }

            if (childGroup.isBelongsToFilterGroup()) {
                CTestSpecification ts = childGroup.getOwnedTestSpec();
                CTestResult result = model.getResult(ts);
                if (result != null) {
                    testResultStatus = EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_HAS_RESULT;
                    if (result.isError()) {
                        testResultStatus = EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_FAILED;
                        break;
                    }
                }
            }
        }

        return testResultStatus;
    }

                 
    public void notifyListeners() {
        for (ILabelProviderListener listener : m_listeners) {
            LabelProviderChangedEvent event = new LabelProviderChangedEvent(this);
            listener.labelProviderChanged(event);
        }
    }

    
    @Override
    public String getToolTipText(Object element) {
        
        if (element instanceof CTestTreeNode) {
            return ((CTestTreeNode)element).toUIString();
        }
        return "";
    }
    
    
    @Override
    public Point getToolTipShift(Object object) {
        return new Point(12,12);
    }

    @Override
    public int getToolTipDisplayDelayTime(Object object) {
        return 500;
    }

    @Override
    public int getToolTipTimeDisplayed(Object object) {
        return 15000;
    }
}


class ExportAction extends Action {
 
    private ImageDescriptor m_imgDescriptor;

    public ExportAction() {
        m_imgDescriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                    IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/enabled/export_wiz.gif");

        if (m_imgDescriptor != null) {
            setImageDescriptor(m_imgDescriptor);
        }
        setText("Export");
        setToolTipText("Opens Export wizard for selected test cases");
   }
    
    
    @Override
    public void run() {
        FileExportCmdHandler handler = new FileExportCmdHandler();
        try {
            handler.execute(null);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Export failed!", ex);
        }
    }
}


class ImportAction extends Action {
    
    private ImageDescriptor m_imgDescriptor;

    ImportAction() {
        m_imgDescriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                		IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/enabled/import_wiz.gif");

        if (m_imgDescriptor != null) {
            setImageDescriptor(m_imgDescriptor);
        }
        
        setText("Import");
        setToolTipText("Opens Import wizard");
    }
    
    
    @Override
    public void run() {
        FileImportCmdHandler handler = new FileImportCmdHandler();
        try {
            handler.execute(null);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Import failed!", ex);
        }
    }
}


