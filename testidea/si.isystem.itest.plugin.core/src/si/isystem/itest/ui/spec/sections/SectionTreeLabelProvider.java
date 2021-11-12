package si.isystem.itest.ui.spec.sections;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EEditorTreeIconId;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.ISectionEditor;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;

/**
 * The label provider class provides text and icons for Test Sections Tree
 * in test case editor.
 */
public class SectionTreeLabelProvider implements ILabelProvider {

    private TestSpecificationModel m_model;
    private CTestTreeNode m_inputTreeNode;


    public void setInput(TestSpecificationModel model, CTestTreeNode inputTreeNode) {
        m_model = model;
        m_inputTreeNode = inputTreeNode;
    }

    
    @SuppressWarnings("unchecked")
    @Override
    public String getText(Object element) {
        return ((TreeNode<EditorSectionNode>)element).getName();
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Image getImage(Object element) {
        @SuppressWarnings("unchecked")
        TreeNode<EditorSectionNode> node = ((TreeNode<EditorSectionNode>)element);
        ISectionEditor sectionEditor = node.getData().getSectionEditor();

        boolean isNodeMerged = sectionEditor.isMerged();
        boolean isNodeDataEmpty = sectionEditor.isEmpty();
        boolean isNodeActive = sectionEditor.isActive();

        IconProvider iconProvider = IconProvider.INSTANCE;
        if ((!isNodeMerged  &&  isNodeDataEmpty)  ||  m_inputTreeNode == null) {
            // there are no more icons to add - the section is empty and it 
            // inherits nothing from parents
            return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EEmpty);
        } 

        if (!isNodeActive) {
            // inactive sections never have results
            if (isNodeMerged) {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EInactiveMerged);
            } else {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EInactive);
            }
        }

        if (m_inputTreeNode.isGroup()) {
            CTestGroup group = CTestGroup.cast(m_inputTreeNode);
            if (group.isTestSpecOwner()) {
                return getTestSpecSectionIcon(group.getOwnedTestSpec(),
                                              sectionEditor,
                                              isNodeMerged,
                                              isNodeDataEmpty);
            } else {
                return getGroupSectionIcon(group,
                                           sectionEditor,
                                           isNodeDataEmpty);
            }
        } else {
            return getTestSpecSectionIcon(m_inputTreeNode,
                                          sectionEditor,
                                          isNodeMerged,
                                          isNodeDataEmpty);
        }
    }


    private Image getGroupSectionIcon(CTestGroup group,
                                      ISectionEditor sectionEditor,
                                      boolean isNodeDataEmpty) {
        CTestGroupResult groupResult = m_model.getGroupResult(group);
        IconProvider iconProvider = IconProvider.INSTANCE;

        if (isNodeDataEmpty) {
            return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EEmpty);
        }

        if (!sectionEditor.isActive()) {
            return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EInactive);
        }

        if (groupResult == null  ||  !sectionEditor.hasErrorStatus()  ||  
                groupResult.isException()) {

            // if there is no result for group there is supposed to 
            // be no error status (for example Coverage section), or an exception was thrown
            // during test, do not apply result icon. If exception was thrown, the test was not
            // executed, so we can not claim, for example, that Verification section failed.
            return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefined);

        } else {
            if (sectionEditor.isError(groupResult)) {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedErr);
            } else { 
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedOk);
            }
        }
    }


    private Image getTestSpecSectionIcon(CTestTreeNode nodeWResult,
                                         ISectionEditor sectionEditor,
                                         boolean isNodeMerged,
                                         boolean isNodeDataEmpty) {

        CTestResult testCaseResult = m_model.getResult(nodeWResult);
        IconProvider iconProvider = IconProvider.INSTANCE;

        if (testCaseResult == null  ||  !sectionEditor.hasErrorStatus()  ||  
                testCaseResult.isException()) {

            // if there is no result for test section or there is supposed to 
            // be no error status (for example Function section), or an exception was thrown
            // during test, do not apply result icon. If exception was thrown, the test was not
            // executed, so we can not claim, for example, that Verification section failed.
            if (isNodeDataEmpty) {
                if (isNodeMerged) {
                    return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EEmptyMerged);
                } else {
                    return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EEmpty);
                }
            } 

            if (isNodeMerged) {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedMerged);
            } else {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefined);
            }

        } else {
            if (sectionEditor.isError(testCaseResult)) {
                if (isNodeDataEmpty) {  
                    // only empty merged sections can have results 
                    return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EEmptyMergedErr);
                } 
                if (isNodeMerged) {
                    return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedMergedErr);
                } else {
                    return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedErr);
                }
            } 

            // OK
            if (isNodeDataEmpty) {  
                // only empty merged sections can have results 
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EEmptyMergedOk);
            }
            if (isNodeMerged) {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedMergedOk);
            } else {
                return iconProvider.getEditorTreeIcon(EEditorTreeIconId.EDefinedOk);
            }
        }
    }
}



