package si.isystem.itest.views;

import java.util.Comparator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import si.isystem.connect.CTestSpecification;
import si.isystem.itest.model.ModelOutlineNode;

public class ProjectExplorerSorter extends ViewerComparator {

    public ProjectExplorerSorter() {
        super();
    }

    
    public ProjectExplorerSorter(Comparator<? super String> comparator) {
        super(comparator);
    }


    @Override
    /**
     * Sorts nodes based on their sequence number, not label strings. This way
     * the same order as in Outline view is preserved (execution order).
     */
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof ModelOutlineNode  &&  e2 instanceof ModelOutlineNode) {
            ModelOutlineNode left = (ModelOutlineNode) e1;
            ModelOutlineNode right = (ModelOutlineNode) e1;
            
            int lSeq = left.getSeqNo();
            int rSeq = right.getSeqNo();
            return lSeq == rSeq ? 0 : (lSeq < rSeq ? -1 : 1);
            
        } else if (e1 instanceof CTestSpecification  &&  e2 instanceof CTestSpecification) {
            
            CTestSpecification left = (CTestSpecification)e1;
            CTestSpecification right = (CTestSpecification)e2;
            CTestSpecification parent = left.getParentTestSpecification();

            int lSeq = parent.findDerivedTestSpec(left);
            int rSeq = parent.findDerivedTestSpec(right);
            return lSeq == rSeq ? 0 : (lSeq < rSeq ? -1 : 1);
        }
        
        return super.compare(viewer, e1, e2);
    }
}
