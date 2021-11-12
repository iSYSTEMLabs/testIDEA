package si.isystem.itest.ui.spec.sections;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPartSite;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;
import si.isystem.itest.ui.spec.sections.SectionTreeModel.ESectionTreeType;

public class SectionTreeView {

    public static final int TOP_INSET = 5;
    
    private TreeViewer m_editorTreeViewer;
    private Map<Object, ISelection> m_sectionsTreeSelections = new TreeMap<>();
    

    public SectionTreeView(Composite treePanel, boolean isCheckBoxViewer, IBaseLabelProvider sectionTreeLabelProvider) {
        if (isCheckBoxViewer) {
            m_editorTreeViewer = new CheckboxTreeViewer(treePanel, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        } else {
            m_editorTreeViewer = new TreeViewer(treePanel, SWT.H_SCROLL | SWT.V_SCROLL);
        }
        m_editorTreeViewer.setContentProvider(new SectionTreeContentProvider());
        m_editorTreeViewer.setLabelProvider(sectionTreeLabelProvider);
        m_editorTreeViewer.setAutoExpandLevel(4); // always expand tree, so that user do not have to 
                                                  // expand Analyzer twice to see statistics node
    }

    
    public void addContextMenu(IWorkbenchPartSite site, String swtbotId) {
        MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(m_editorTreeViewer.getControl());
        m_editorTreeViewer.getControl().setMenu(menu);
        m_editorTreeViewer.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, swtbotId);

        if (site != null) { // should never be null
            site.registerContextMenu("si.isystem.itest.testEditorSectionCtxMenu", 
                                     menuManager, 
                                     m_editorTreeViewer);
        }
    }
    

//    public TreeNode<EditorSectionNode> addNode(EditorSectionNode editorNode,
//                                               ENodeId nodeId,
//                                               TreeNode<EditorSectionNode> ... parentNodes) {
//        
//        TreeNode<EditorSectionNode> treeNode = parentNodes[0].addChild(nodeId.getUiName(), 
//                                                                       editorNode);
//        m_nodesMap.put(nodeId, treeNode);
//
//        for (int idx = 1; idx < parentNodes.length; idx++) {
//            parentNodes[idx].addChild(treeNode);
//        }
//        
//        return treeNode;
//    }

    
    public void setLayoutData(Object layoutData) {
        m_editorTreeViewer.getControl().setLayoutData(layoutData);
        
    }
    
    
    public ISelectionProvider getSelectionProvider() {
        return m_editorTreeViewer;
    }
    
    
    public void setInput(Object input) {
        m_editorTreeViewer.setInput(input);
    }


    public void setEnabled(boolean isEnabled) {
        
        m_editorTreeViewer.getControl().setEnabled(isEnabled);        
    }


    public void refresh() {
        m_editorTreeViewer.refresh();        
    }


    public void setFocus() {
        
        m_editorTreeViewer.getControl().setFocus();        
    }


    public void setSelection(StructuredSelection structuredSelection) {
        m_editorTreeViewer.setSelection(structuredSelection);
    }


    public void setSelection(ISelection structuredSelection, boolean reveal) {
        m_editorTreeViewer.setSelection(structuredSelection, reveal);
    }


    public Object getInput() {
        return m_editorTreeViewer.getInput();
    }


    public ISelection getSelection() {
        return m_editorTreeViewer.getSelection();
    }


    /** Returns checked state of the given element. */
    public boolean isChecked(TreeNode<EditorSectionNode> element) {
        return ((CheckboxTreeViewer)m_editorTreeViewer).getChecked(element);
    }
    
    
    /** Returns checked state of the given element. */
    public Object[] getAllChecked() {
        return ((CheckboxTreeViewer)m_editorTreeViewer).getCheckedElements();
    }
    
    
    /** Returns checked state of the given element. */
    public void setAllChecked(boolean state) {
        
        Object input = m_editorTreeViewer.getInput();
        
        // have to walk all children, as 'input' node is not element of tree 
        // (it is root node, has no corresponding TreeItem). See source of
        // setSubtreeChecked() for details.
        if (input instanceof TreeNode<?>) {
            @SuppressWarnings("unchecked")
            TreeNode<EditorSectionNode> treeNode = (TreeNode<EditorSectionNode>)m_editorTreeViewer.getInput();
            TreeNode<EditorSectionNode>[] children = treeNode.getChildrenAsArray();
            for (TreeNode<EditorSectionNode> child : children) {
                ((CheckboxTreeViewer)m_editorTreeViewer).setSubtreeChecked(child, 
                                                                           state);
            }
        }
    }
    
    
    public void addSelectionChangedListener(ISelectionChangedListener iSelectionChangedListener) {
        
        m_editorTreeViewer.addSelectionChangedListener(iSelectionChangedListener);        
    }
    
    
    public ESectionTreeType getSectionTreeType(CTestTreeNode testNode) {
        
        if (testNode == null) {
            return null;
        }
        
        CTestSpecification testSpec = null;
        
        if (testNode instanceof CTestGroup) {
            CTestGroup group = (CTestGroup)testNode;
            if (!group.isTestSpecOwner()) {
                // it is filter group, show it as group
                return ESectionTreeType.EGROUP;
            } else {  
                testSpec = group.getOwnedTestSpec();
            }
        } else {
            testSpec = (CTestSpecification)testNode;  // unit or sys tree
        }
        
        if (testSpec != null) {
            if (testSpec.getMergedTestScope() == CTestSpecification.ETestScope.E_UNIT_TEST) {
                return ESectionTreeType.EUNIT_TEST;
            } else {
                return ESectionTreeType.ESYSTEM_TEST;
            }
        }
        
        return null;
    }
    
    
    @SuppressWarnings("unchecked")
    //         TreeNode<EditorSectionNode> topNode = getTopNode(nodeId);
    public EditorSectionNode setVisibleSectionsTree(TreeNode<EditorSectionNode> topNode, 
                                                    boolean isRestoreSelection) {

        Object sectionsTreeInput = m_editorTreeViewer.getInput();
        
        // call setInput() only when really necessary, since it collapses the tree
        if (sectionsTreeInput != topNode) { 
            // save selection in current tree
            ISelection selection = m_editorTreeViewer.getSelection();
            m_sectionsTreeSelections.put(sectionsTreeInput, selection);
            m_editorTreeViewer.setInput(topNode);
            
            if (isRestoreSelection) {
                
                // restore selection in visible tree
                TreeSelection newSelection = (TreeSelection)m_sectionsTreeSelections.get(topNode);
                
                if (newSelection == null  ||  newSelection.isEmpty()) {
                    
                    // if nothing was selected previously, select the first node.
                    // normally this code is executed only immediately after app startup,
                    // when there are no selections in the map. 
                    newSelection = new TreeSelection(new TreePath(new Object[]{topNode, 
                                                       topNode.getChildrenAsArray()[0]}));
                }
                
                m_editorTreeViewer.setSelection(newSelection, true);
                Object selectedNode = newSelection.getFirstElement();
                
                if (selectedNode != null  &&  selectedNode instanceof TreeNode<?>) {
                    return ((TreeNode<EditorSectionNode>)selectedNode).getData();
                }
            }
        } else {
            m_editorTreeViewer.refresh();
        }
        return null;
    }
}
