package si.isystem.itest.model;

import org.eclipse.jface.viewers.IElementComparer;

import si.isystem.connect.CTestTreeNode;

public class TestSpecComparer implements IElementComparer {
    
    
    @Override
    public boolean equals(Object a, Object b) {
        if (a instanceof CTestTreeNode  &&  b instanceof CTestTreeNode) {
            boolean eq = ((CTestTreeNode)a).compare((CTestTreeNode)b);
            return eq;
        }

        return a.equals(b);
    }

    
    @Override
    public int hashCode(Object element) {
        if (element instanceof CTestTreeNode) {
            return (int)((CTestTreeNode)element).hashCodeAsPtr();
        }

        return element.hashCode();
    }
}
