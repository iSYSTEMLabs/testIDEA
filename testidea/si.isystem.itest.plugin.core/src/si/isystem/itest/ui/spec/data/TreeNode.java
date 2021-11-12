package si.isystem.itest.ui.spec.data;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a general class for forming tree structure.
 * 
 * @param <T> data in node
 */
public class TreeNode<T> implements Comparable<TreeNode<T>>{

    final private String m_name;
    final private List<TreeNode<T>> m_children;
    final private TreeNode<T> m_parent;
    private T m_data;
    

    public TreeNode(TreeNode<T> parent, String nodeName) {
        m_parent = parent;
        m_name = nodeName;
        m_data = null;
        m_children = new ArrayList<TreeNode<T>>();
    }
    
    
    public TreeNode(TreeNode<T> parent, String nodeName, T data) {
        m_parent = parent;
        m_name = nodeName;
        m_data = data;
        m_children = new ArrayList<TreeNode<T>>();
    }
    
    
    public TreeNode<T> addChild(String nodeName, T data) {
        TreeNode<T> newNode = new TreeNode<T>(this, nodeName, data);
        m_children.add(newNode);
        return newNode;
    }
    
    
    public TreeNode<T> addChild(String nodeName) {
        TreeNode<T> newNode = new TreeNode<T>(this, nodeName);
        m_children.add(newNode);
        return newNode;
    }
    
    
    public void addChild(TreeNode<T> node) {
        m_children.add(node);
    }
    
    
    public boolean hasChildren() {
        return m_children.size() > 0;
    }
    
    
    @SuppressWarnings("unchecked")
    public TreeNode<T>[] getChildrenAsArray() {
        return m_children.toArray(new TreeNode[]{});
    }

    
    public String getName() {
        return m_name;
    }


    public TreeNode<T> getParent() {
        return m_parent;
    }

    
    public T getData() {
        return m_data;
    }


    public void setData(T data) {
        m_data = data;
    }


    @Override
    public int compareTo(TreeNode<T> o) {
        
        if (hashCode() == o.hashCode()) {
            return 0;
        }
        return hashCode() < o.hashCode() ? -1 : 1;
    }
}
