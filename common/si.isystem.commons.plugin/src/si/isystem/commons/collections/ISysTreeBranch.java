
package si.isystem.commons.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Branch is the tree element holding the items together. Each branch has a
 * parent, multiple children and the item it is holding.
 * 
 * @author iztokv
 *
 */
public class ISysTreeBranch<T> {
    private T m_item;
    private ISysTreeBranch<T> m_parentBranch = null;
    private List<ISysTreeBranch<T>> m_childBranches = null;
    
    public ISysTreeBranch() {
        this(null);
    }
    
    public ISysTreeBranch(T item) {
        m_item = item;
    }
    
    public T getItem() {
        return m_item;
    }
    
    public ISysTreeBranch<T> getParentBranch() {
        return m_parentBranch;
    }

    public void addBranch(ISysTreeBranch<T> childBranch) {
        addBranch(getBranchCount(), childBranch);
    }
    
    public void addBranch(int idx, ISysTreeBranch<T> childBranch) {
        if (childBranch.m_parentBranch != null) {
            childBranch.m_parentBranch.removeBranch(childBranch);
        }
        childBranch.m_parentBranch = this;

        if (m_childBranches == null) {
            m_childBranches = new ArrayList<>();
        }
        m_childBranches.add(idx, childBranch);
    }

    public boolean removeBranch(ISysTreeBranch<T> childBranch) {
        int idx = indexOf(childBranch);
        if (idx >= 0) {
            ISysTreeBranch<T> removed = removeBranch(idx);
            return (removed != null);
        }
        else {
            return false;
        }
    }

    public ISysTreeBranch<T> removeBranch(int idx) {
        if (idx == -1) {
            return null;
        }
        if (m_childBranches == null) {
            return null;
        }
        if (idx < 0  ||  idx >= m_childBranches.size()) {
            return null;
        }
        
        ISysTreeBranch<T> childBranch = m_childBranches.remove(idx);

        if (childBranch.m_parentBranch != null) {
            childBranch.m_parentBranch.removeBranch(childBranch);
        }
        childBranch.m_parentBranch = null;
        return childBranch;
    }

    public boolean hasBranches() {
        if (m_childBranches == null) {
            return false;
        }
        
        return (m_childBranches.size() > 0);
    }
    
    public int getBranchCount() {
        if (m_childBranches == null) {
            return 0;
        }
        
        return m_childBranches.size();
    }
    
    public ISysTreeBranch<T> getBranch(int idx) {
        if (m_childBranches == null) {
            return null;
        }
        if (idx < 0) {
            return null;
        }
        if (idx >= m_childBranches.size()) {
            return null;
        }
        return m_childBranches.get(idx);
    }
    
    public int indexOf(ISysTreeBranch<T> childBranch) {
        if (m_childBranches == null) {
            return -1;
        }
        if (childBranch.m_parentBranch != this) {
            return -1;
        }
        return m_childBranches.indexOf(childBranch);
    }
    
    public int index() {
        if (m_parentBranch == null) {
            return 0;
        }
        
        return m_parentBranch.indexOf(this);
    }
    
    public int depth() {
        if (m_parentBranch == null) {
            return 0;
        }

        int depth = 0;
        ISysTreeBranch<T> b = m_parentBranch;
        
        while (true) {
            b = b.m_parentBranch;
            depth++;
            if (b == null) {
                return depth;
            }
        }
    }
    
    public boolean isSiblingOf(ISysTreeBranch<T> reqAncestor) {
        if (reqAncestor == null) {
            return true;
        }
        ISysTreeBranch<T> ancestor = m_parentBranch;
        while (ancestor != null) {
            if (ancestor == reqAncestor) {
                return true;
            }
            ancestor = ancestor.m_parentBranch;
        }
        return false;
    }
    
    public boolean isAncestorOf(ISysTreeBranch<T> sibling) {
        return sibling.isSiblingOf(this);
    }
    
    /**
     * Sorts the branch and all child branches by the values of their items with the given comparator.
     * 
     * @param comparator
     * @return returns true if any changes were made.
     */
    public boolean sort(final Comparator<T> comparator) {
        if (!hasBranches()) {
            return false;
        }

        boolean changesMade = false;
        for (int i = 0; i < m_childBranches.size(); i++) {
            ISysTreeBranch<T> b = m_childBranches.get(i);
            changesMade |= b.sort(comparator);
        }

        // Checking for changes only if none were made until now (BLOCK 1)
        @SuppressWarnings("rawtypes") ISysTreeBranch[] b1 = new ISysTreeBranch[0];
        @SuppressWarnings("rawtypes") ISysTreeBranch[] b2 = new ISysTreeBranch[0];
        if (!changesMade) {
            b1 = m_childBranches.toArray(new ISysTreeBranch[m_childBranches.size()]);
        }

        Collections.sort(m_childBranches, new Comparator<ISysTreeBranch<T>>() {
            @Override
            public int compare(ISysTreeBranch<T> branch1, ISysTreeBranch<T> branch2) {
                return comparator.compare(branch1.getItem(), branch2.getItem());
            }
        });

        // Checking for changes only if none were made until now (BLOCK 2)
        if (!changesMade) {
            b2 = m_childBranches.toArray(new ISysTreeBranch[m_childBranches.size()]);
            changesMade |= (b1.length != b2.length);
            if (!changesMade) {
                for (int i = 0; i < b1.length; i++) {
                    if (b1[i] != b2[i]) {
                        changesMade = true;
                        break;
                    }
                }
            }
        }

        return changesMade;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), m_item);
    }
    
    private static final String PREFIX = " ";
    
    public String deepToString() {
        StringBuilder sb = new StringBuilder();
        deepToString(sb, "");
        return sb.toString();
    }
    
    private void deepToString(StringBuilder sb, String prefix) {
        ISysTreeBranch<T> parentBranch = getParentBranch();
        sb.append(String.format("%s%s -> %s\n", 
                prefix, 
                m_item, 
                (parentBranch != null ? parentBranch.getItem() : null)));
        if (hasBranches()) {
            prefix = PREFIX + prefix;
            for (ISysTreeBranch<T> c : m_childBranches) {
                c.deepToString(sb, prefix);
            }
        }
    }
}