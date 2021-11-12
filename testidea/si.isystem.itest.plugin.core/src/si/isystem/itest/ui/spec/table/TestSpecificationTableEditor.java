package si.isystem.itest.ui.spec.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.WorkbenchPart;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.TSUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.ISectionEditor;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;
import si.isystem.itest.ui.spec.sections.SectionTreeModel;
import si.isystem.itest.ui.spec.sections.SectionTreeModel.ESectionTreeType;
import si.isystem.itest.ui.spec.sections.SectionTreeView;
import si.isystem.tbltableeditor.HeaderPath;
import si.isystem.ui.utils.SelectionAdapter;

/**
 * 
 * @author markok
 *
 */
public class TestSpecificationTableEditor extends EditorPart {

    private static final String MENU_DATA_FUNC_NAME = "funcName";

    private WorkbenchPart m_testCaseEditorPart;

    private TestSpecificationModel m_model;
    
    private SectionTreeView m_sectionTreeView;
    private SectionTreeModel m_sectionTreeModel;
    private CTestBaseKTable m_sectionsTable;
    private CTestTreeNode m_inputTreeNode;

    private Menu m_scriptRangeFuncMenu;
    private String m_lastExecutedScriptMethod = null;

    private MenuItem m_checkedMenuItem;

    
    public TestSpecificationTableEditor(WorkbenchPart testCaseEditorPart, 
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
        MigLayout mainLayout = new MigLayout("fill, ins 0 0 0 0");
        parent.setLayout(mainLayout);

        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setLayoutData("grow, hmin 0");
        
        sash.SASH_WIDTH = 3;
        Composite treePanel = new Composite(sash, SWT.BORDER);
        treePanel.setLayout(new MigLayout("fill, ins " + SectionTreeView.TOP_INSET + " 0 0 0",
                            "", "[min!][fill]"));

        createTableToolbar(treePanel);
        
        m_sectionTreeView = createSectionsTreeControl(treePanel);
        
        getSite().setSelectionProvider(m_sectionTreeView.getSelectionProvider());
        hookClickCommand();
        setTreeSelection(ESectionTreeType.EUNIT_TEST);
        
        Composite editPanel = new Composite(sash, SWT.BORDER);
        editPanel.setLayout(new MigLayout("fill"));
        
        m_sectionsTable = new CTestBaseKTable();
        m_sectionsTable.createControls(editPanel, m_testCaseEditorPart, null);

        // must be last to have effect
        sash.setWeights(new int[]{20, 80});
    }


    private void createTableToolbar(Composite treePanel) {
        ToolBar toolBar = new ToolBar(treePanel, SWT.FLAT);
        toolBar.setLayout(new MigLayout());
        toolBar.setLayoutData("wrap");
        
        ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
        toolItem.setImage(IconProvider.INSTANCE.getIcon(EIconId.ESelectDeselectAll));
        toolItem.setToolTipText("Select / deselect all sections.");
        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Object[] checkedItems = m_sectionTreeView.getAllChecked();
                if (checkedItems.length == 0) {
                    m_sectionTreeView.setAllChecked(true);
                    m_sectionsTable.restoreAllVisibleSections();
                } else {
                    m_sectionTreeView.setAllChecked(false);
                    m_sectionsTable.clearAllVisibleSections();
                }
                m_sectionsTable.refresh();
            }
        });
        
        toolItem = new ToolItem(toolBar, SWT.PUSH);
        toolItem.setImage(IconProvider.INSTANCE.getIcon(EIconId.EToggleInherit));
        toolItem.setToolTipText("Toggle inheritance of selected test case and section.");
        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    m_sectionsTable.toggleInheritanceOfSelectedSection();
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not toggle inheritance!", ex);
                }                
            }
        });
        
        new ToolItem(toolBar, SWT.SEPARATOR);
        
        toolItem = new ToolItem(toolBar, SWT.PUSH);
        toolItem.setImage(IconProvider.INSTANCE.getIcon(EIconId.EExtrapolate));
        toolItem.setToolTipText("Extrapolate first two cells in selected region of table column.");
        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    m_sectionsTable.execSpreadsheetOperation((tblTable, tmp1, tmp2, isExistReadOnlyCells) -> 
                                                             tblTable.extrapolate(isExistReadOnlyCells));
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not extrapolate!", ex);
                }
            }
        });
        
        toolItem = new ToolItem(toolBar, SWT.PUSH);
        toolItem.setImage(IconProvider.INSTANCE.getIcon(EIconId.EInterpolate));
        toolItem.setToolTipText("Interpolate between first and last cell in selected region of table column.");
        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    m_sectionsTable.execSpreadsheetOperation((tblTable, tmp1, tmp2, isExistReadOnlyCells) -> 
                                                             tblTable.interpolate(isExistReadOnlyCells));
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not interpolate!", ex);
                }
            }
        });
        
        final ToolItem scriptToolbarBtn = new ToolItem(toolBar, SWT.DROP_DOWN);
        scriptToolbarBtn.setImage(IconProvider.INSTANCE.getIcon(EIconId.EExternalTools));
        scriptToolbarBtn.setToolTipText(SWTBotConstants.SCRIPT_BTN_TOOLTIP);
        scriptToolbarBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (event.detail == SWT.ARROW) {
                    Rectangle rect = scriptToolbarBtn.getBounds();
                    Point pt = new Point(rect.x, rect.y + rect.height);
                    pt = toolBar.toDisplay(pt);
                    m_scriptRangeFuncMenu.setLocation(pt.x, pt.y);
                    m_scriptRangeFuncMenu.setVisible(true);
                } else {
                    if (m_lastExecutedScriptMethod != null) {
                        try {
                            m_sectionsTable.execTableScriptFunc(m_lastExecutedScriptMethod);
                        } catch (Exception ex) {
                            SExceptionDialog.open(Activator.getShell(), 
                                                  "Error when calling script extension!", 
                                                  ex);
                        }
                        
                    }
                }
            }
        });

        configureScriptFunctionsMenu(new ArrayList<>());
    }    
    
    
    public void configureScriptFunctionsMenu(List<String> funcNames) {
        
        if (m_scriptRangeFuncMenu != null) {
            m_scriptRangeFuncMenu.dispose();
        }
        
        m_scriptRangeFuncMenu = new Menu(Activator.getShell(), SWT.POP_UP);

//        m_scriptToolbarBtn.addListener(SWT.Selection, new Listener() {
//          public void handleEvent(Event event) {
//            if (event.detail == SWT.ARROW) {
//              Rectangle rect = m_scriptToolbarBtn.getBounds();
//              Point pt = new Point(rect.x, rect.y + rect.height);
//              pt = m_toolBar.toDisplay(pt);
//              m_scriptRangeFuncMenu.setLocation(pt.x, pt.y);
//              m_scriptRangeFuncMenu.setVisible(true);
//            }
//          }
//        });        

        if (funcNames.isEmpty()) {
            MenuItem item = new MenuItem(m_scriptRangeFuncMenu, SWT.PUSH);
            item.setText("<Click refresh toolbar button>");
            item.addSelectionListener(new SelectionAdapter() {
                
                @Override
                public void widgetSelected(SelectionEvent e) {
                    MessageDialog.openInformation(Activator.getShell(), "Help", 
                                                  "To get the list of custom functions in this menu:\n"
                                                  + "- Configure scripts in 'File | Properties | Scrips'\n"
                                                  + "- Write script function with prefix '" + CScriptConfig.getEXT_METHOD_TABLE_PREFIX() + "'\n"
                                                  + "- Click 'Refresh' main toolbar button\n\n"
                                                  + "You can also use 'iTools | Script Extension Wizard' to configure scripts.");
                }
            });
            
            return;
        }
        
        boolean isMethodInMenu = false;
        
        for (String funcName : funcNames) {
            MenuItem item = new MenuItem(m_scriptRangeFuncMenu, SWT.CHECK);
            item.setText(funcName + "()");
            item.setData(MENU_DATA_FUNC_NAME, funcName);
            item.addSelectionListener(new SelectionAdapter() {
                
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        if (m_checkedMenuItem != null) {
                            m_checkedMenuItem.setSelection(false);
                        }
                        MenuItem menuItem = (MenuItem)e.widget;
                        String scriptMethod = (String)menuItem.getData(MENU_DATA_FUNC_NAME);
                        m_lastExecutedScriptMethod = scriptMethod;
                        m_checkedMenuItem = menuItem;
                        m_checkedMenuItem.setSelection(true);
                        m_sectionsTable.execTableScriptFunc(scriptMethod);
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), 
                                              "Error when calling script extension!", 
                                              ex);
                    }
                }
            });
            
            if (funcName.equals(m_lastExecutedScriptMethod)) {
                // keep selection on refresh
                isMethodInMenu = true;
                m_checkedMenuItem = item;
                m_checkedMenuItem.setSelection(true);
            }
        }
        
        // reset default script method if it is not available after refresh. 
        if (!isMethodInMenu) {
            m_lastExecutedScriptMethod = null;
        }
    }
    
    
    @Override
    public void doSave(IProgressMonitor monitor) {
        // implemented in TestSpecificationEditorView
    }

    @Override
    public void doSaveAs() {
        // implemented in TestSpecificationEditorView
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty() {
        // implemented in TestSpecificationEditorView
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        // implemented in TestSpecificationEditorView
        return false;
    }

    @Override
    public void setFocus() {
        m_sectionTreeView.setFocus();
    }


    private void setTreeSelection(ESectionTreeType topNode) {
        TreeNode<EditorSectionNode> node = m_sectionTreeModel.getTopNode(topNode);
        m_sectionTreeView.setSelection(new StructuredSelection(node), false);
    }
    
    
    public void setTreeSelection(ISelection selection) {
        m_sectionTreeView.setSelection(selection, true);
    }
    
    
    public ISelection getTreeSelection() {
        return m_sectionTreeView.getSelection();
    }
    
    
    private SectionTreeView createSectionsTreeControl(Composite parent) {
        
//        CheckboxTreeViewer treeViewer = new CheckboxTreeViewer(parent);
//        treeViewer.getTree().setLayoutData("grow, wmin 0, hmin 0");
//        m_treeContentProvider = new SectionTreeContentProvider();
//        treeViewer.setContentProvider(m_treeContentProvider);
//        treeViewer.setLabelProvider(new SectionTreeLabelProvider());
//        treeViewer.setAutoExpandLevel(4); // always expand tree, so that user do not have to 
//                                          // expand Analyzer twice to see statistics node
        SectionTreeView sectionTreeView = new SectionTreeView(parent, true, 
                                                          m_sectionTreeModel.getSectionTreeLabelProvider());
        
        sectionTreeView.setLayoutData("grow, hmin 100");
        sectionTreeView.addContextMenu(getSite(), SWTBotConstants.BOT_TABLE_EDITOR_TREE_ID);
        
        // when user clicks a checkbox in the tree, check/uncheck all of its children
        ((CheckboxTreeViewer)sectionTreeView.getSelectionProvider()).addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                // check/uncheck all children
                Object element = event.getElement();
                if (element instanceof TreeNode<?>) {
                    @SuppressWarnings("unchecked")
                    TreeNode<EditorSectionNode> node = (TreeNode<EditorSectionNode>)element;
                    updateVisibleSections(node, event.getChecked());
                    m_sectionsTable.refresh();
                }
            }
        });
        
        sectionTreeView.setInput(m_sectionTreeModel.getTopNode(ESectionTreeType.EUNIT_TEST));
        
        return sectionTreeView;
    }

    
    public void setInput(TestSpecificationModel model, CTestTreeNode treeNode) {
        
        m_model = model;
        m_sectionTreeModel.setLabelProviderInput(m_model, treeNode);
        m_inputTreeNode = treeNode;
        
        if (treeNode == null) {
            
            m_sectionsTable.setInput(null, model, false, true);
        } else {
            m_sectionsTable.setInput(treeNode, model, true, true);
            
            treeNode = TSUtils.castToType(treeNode);

            selectSectionsTree(treeNode, true);
        }
    }
    
    
    /**
     * Sets input as a list of CTestBase objects, which may not be stored in 
     * the same list or are only subset of a list. For example, when user
     * selects few test cases from one larger list or from several lists 
     * (derived tests cases of multiple test cases). Reordering, insertion
     * and removal are not available for this input.
     * 
     * @param list
     */
    public void setInput(TestSpecificationModel model,
                         TreeSelection selection) {
        
        CTestTreeNode treeNode = (CTestTreeNode)selection.getFirstElement();
        m_model = model;
        m_sectionTreeModel.setLabelProviderInput(m_model, treeNode);
        m_inputTreeNode = treeNode;
        
        treeNode = TSUtils.castToType(treeNode);
        boolean isGroup = treeNode.isGroup();
        int sectionId = CTestSpecification.SectionIds.E_SECTION_TESTS.swigValue();
        if (isGroup) {
            sectionId = CTestGroup.ESectionCTestGroup.E_SECTION_CHILDREN.swigValue();
        } 

        selectSectionsTree(treeNode, true);

        // create dummy CTestBase and fill its children to be shown in the table
        CTestTreeNode containerTb = CTestTreeNode.cast(treeNode.createInstance(null));
        CTestBaseList list = containerTb.getTestBaseList(sectionId, false);
        Iterator<?> it = selection.iterator();
        while (it.hasNext()) {
            CTestTreeNode testNode = (CTestTreeNode)it.next();
            if (TSUtils.isFilterGroup(testNode) != isGroup) {
                throw new SIllegalArgumentException("Mixed types selected!\n"
                    + "All selected items must be of the same type - either test cases or test groups.");
            }
            list.add(-1, testNode);
        }
        
        m_sectionsTable.setInput(containerTb, model, false, false);
    }
    

    public CTestTreeNode getInput() {
        return m_inputTreeNode;
    }


    private void selectSectionsTree(CTestTreeNode testNode, boolean isRestoreSelection) {

        ESectionTreeType treeeType = m_sectionTreeView.getSectionTreeType(testNode);
        
        if (treeeType == null) {
            setSectionTreeInput(ESectionTreeType.EUNIT_TEST, false);
            return;
        } 
        
        setSectionTreeInput(treeeType, isRestoreSelection);
    }    

    
    private EditorSectionNode setSectionTreeInput(ESectionTreeType treeType, 
                                                  boolean isRestoreSelection) {
        
        TreeNode<EditorSectionNode> node = m_sectionTreeModel.getTopNode(treeType);
        return m_sectionTreeView.setVisibleSectionsTree(node, isRestoreSelection);
    }
    

    private void hookClickCommand() {
        m_sectionTreeView.addSelectionChangedListener(new ISelectionChangedListener() {
            
            private TreeNode<EditorSectionNode> m_prevSelectedNode;

            // private boolean m_isExceptionThrown = false;

            @SuppressWarnings("unchecked")
            @Override
            public void selectionChanged(SelectionChangedEvent event) {

                TreeSelection selection =  (TreeSelection)event.getSelection();
                if (!selection.isEmpty()) {
                    
                    TreeNode<EditorSectionNode> selectedNode = 
                            (TreeNode<EditorSectionNode>)selection.getFirstElement();
                    
                    // remove previously selected node, if not checked
                    if (m_prevSelectedNode != null) {
                        if (!m_sectionTreeView.isChecked(m_prevSelectedNode)) {
                            updateVisibleSections(m_prevSelectedNode, false);
                        }
                    }
                    
                    // if selected node is not checked, show it in table
                    if (!m_sectionTreeView.isChecked(selectedNode)) {
                        updateVisibleSections(selectedNode, true);
                    } 

                    m_sectionsTable.refreshStructure(); // structure may have changed when 
                                                        // switching from normal editor
                    // scroll to selected section
                    ISectionEditor editor = selectedNode.getData().getSectionEditor();
                    String nodePath = editor.getNodePath();
                    int [] sections = editor.getSectionIdsForTableEditor();
                    m_sectionsTable.scrollToVisibleColumn(nodePath, sections);
                    
                    m_prevSelectedNode = selectedNode; 
                }
            }
        });
    }


    public void updateVisibleSections(TreeNode<EditorSectionNode> node, boolean isChecked) {
        
        ISectionEditor editor = node.getData().getSectionEditor();
        
        if (editor.getTestTreeNode() == null) {
            // this happens, when user deletes test case in Outline view (no
            // test case is selected there), then clicks section in Editor 
            // Sections Tree. 
            return;
        }

        boolean isAnyOfNodeOrChildrenChecked = isChecked;
        String nodePath = editor.getNodePath();
        TreeNode<EditorSectionNode>[] childNodes = node.getChildrenAsArray();
        int [] sections = new int[0];
        
        if (nodePath != null) {  // editors with scalar sections, for example in Meta, have no path
            if (isChecked) {
                // Get sections of unchecked subnode paths and remove them to make nodes invisible.
                // Subnode path is unchecked, if no node in the path is checked. 
                // For example, if Analyzer node is checked, Coverage node is not, but Statistics
                // node is, then Coverage node must be visible in Analyzer's list of sections.
                m_sectionsTable.restoreVisibleSections(nodePath);
                
                for (TreeNode<EditorSectionNode> childNode : childNodes) {
                    
                    boolean isChildSubtreeVisible = isAnyChildChecked(childNode); 
                    if (!isChildSubtreeVisible) {
                        ISectionEditor childNodeEditor = childNode.getData().getSectionEditor();
                        int[] childSections = childNodeEditor.getSectionIdsForTableEditor();
                        int newSections[] = DataUtils.appendNewItems(sections, childSections);
                        if (newSections != null) {
                            sections = newSections;
                        }
                    }
                }
                m_sectionsTable.removeVisibleSections(nodePath, sections);

            } else {

                m_sectionsTable.clearVisibleSections(nodePath);
                
                for (TreeNode<EditorSectionNode> childNode : childNodes) {

                    boolean isChildSubtreeVisible = isAnyChildChecked(childNode);
                    isAnyOfNodeOrChildrenChecked |= isChildSubtreeVisible;

                    if (isChildSubtreeVisible) {
                        ISectionEditor childNodeEditor = childNode.getData().getSectionEditor();
                        int[] childSections = childNodeEditor.getSectionIdsForTableEditor();
                        int [] newSections = DataUtils.appendNewItems(sections, childSections);
                        if (newSections != null) {
                            sections = newSections;
                        }
                    }
                }
                m_sectionsTable.addVisibleSections(nodePath, sections);
            }
        }


        // Update visibility of child nodes in all parent nodes from clicked node up to root.
        // For example, if Coverage Statistics node is clicked, Coverage and Analyzer nodes
        // must have corresponding sections in their parent nodes visible.
        TreeNode<EditorSectionNode> parentNode = node.getParent();
        while (parentNode != null  &&  editor != null) {
            
            String parentNodePath;
            ISectionEditor parentEditor = null;
            if (parentNode.getData() == null) { // top level node
                parentNodePath = HeaderPath.SEPARATOR + editor.getTestTreeNode().getClassName();
            } else {
                parentEditor = parentNode.getData().getSectionEditor();
                parentNodePath = parentEditor.getNodePath();
            }
            
            if (isAnyOfNodeOrChildrenChecked) {
                m_sectionsTable.addVisibleSections(parentNodePath, 
                                                   editor.getSectionIdsForTableEditor());
            } else {
                m_sectionsTable.removeVisibleSections(parentNodePath, 
                                                      editor.getSectionIdsForTableEditor());
            }
            
            isAnyOfNodeOrChildrenChecked |= m_sectionTreeView.isChecked(parentNode)  ||  
                                            // there are other sub-nodes visible, for example Profiler 
                                            // when coverage is unchecked
                                            m_sectionsTable.hasVisibleSections(parentNodePath);
            
            node = parentNode;
            parentNode = parentNode.getParent();
            editor = parentEditor;
        }
    }

    
    private boolean isAnyChildChecked(TreeNode<EditorSectionNode> node) {
        if (m_sectionTreeView.isChecked(node)) {
            return true;
        }
        
        TreeNode<EditorSectionNode>[] childNodes = node.getChildrenAsArray();
        for (TreeNode<EditorSectionNode> childNode : childNodes) {
            if (isAnyChildChecked(childNode)) {
                return true;
            }
        }
        
        return false;
    }


    public void refreshGlobals() {
    }


    public void testSpecDataChanged() {
        
        m_sectionsTable.refresh();
    }


    public void testSpecTreeStructureChanged(@SuppressWarnings("unused") ModelChangedEvent event) {
        m_sectionsTable.refreshStructure();
        
    }
}
