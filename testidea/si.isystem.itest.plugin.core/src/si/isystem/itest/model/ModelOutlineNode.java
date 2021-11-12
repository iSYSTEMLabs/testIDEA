package si.isystem.itest.model;

import org.eclipse.swt.graphics.Image;

import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EDerivedTestResultStatus;

/**
 * This class contains outline of the model to be shown in Project Explorer.
 * Because Project Explorer may show outline before editor is opened, it could
 * be problematic to maintain complete models (with CTestSpecification hieararchy)
 * for files, which will not even be opened. 
 *  
 * @author markok
 *
 */
public class ModelOutlineNode {

    private String m_testId;
    private String m_function;
    private boolean m_isRunnable;
    private int m_hierarchyLevel;
    private int m_seqNo;
    private ETestScope m_scope;

    private ModelOutlineNode[] m_children;

    /**
     * 
     * @param testId
     * @param function
     * @param isRunnable
     * @param scope
     * @param seqNo sequence number in hierarchy tree used for sorting in 
     *              ProjectExplorer.
     * @param noOfChildren
     */
    public ModelOutlineNode(String testId, String function, 
                        boolean isRunnable, ETestScope scope,
                        int level,
                        int seqNo,
                        int noOfChildren) {
        m_testId = testId;
        m_function = function;
        m_isRunnable = isRunnable;
        m_scope = scope;
        m_hierarchyLevel = level;
        m_seqNo = seqNo;
        m_children = new ModelOutlineNode[noOfChildren];
    }
    
    
    public void setChild(int idx, ModelOutlineNode child) {
        m_children[idx] = child;
    }
    
    
    public Image getIcon() {
        return IconProvider.INSTANCE.getTreeViewIcon(m_isRunnable,
                                                     m_scope,
                                                     null,
                                                     EDerivedTestResultStatus.NO_RESULTS);
    }
    
    
    public String getLabel() {
        return TestSpecificationModel.getTestSpecificationName(m_testId, m_function);
    }


    public ModelOutlineNode[] getChildren() {
        return m_children;
    }
    

    public int getSeqNo() {
        return m_seqNo;
    }

/*
    public Object getParent() {
        return m_parent;
    }
*/

    @Override
    public String toString() {
        return getLabel();
    }
    
    
    /**
     * Used for reveling selection when link button in Project explorer
     * is selected.  
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (obj instanceof ModelOutlineNode) {
            ModelOutlineNode node = (ModelOutlineNode) obj;
            return m_seqNo == node.m_seqNo  &&  m_hierarchyLevel == node.m_hierarchyLevel;
        }
        
        return false;
    }
}
