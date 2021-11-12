package si.isystem.itest.ui.spec.sections;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TreeNode;


/**
 * The content provider class provides objects for Test Sections Tree.
 */
public class SectionTreeContentProvider implements ITreeContentProvider {
    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    @Override
    public void dispose() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object parent) {

        return ((TreeNode<EditorSectionNode>)parent).getChildrenAsArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object[] getChildren(Object parent) {
        return ((TreeNode<EditorSectionNode>)parent).getChildrenAsArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getParent(Object element) {
        return ((TreeNode<EditorSectionNode>)element).getParent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean hasChildren(Object element) {
        return ((TreeNode<EditorSectionNode>)element).hasChildren();
    }
}


