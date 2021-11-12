package si.isystem.connect.data;

import si.isystem.connect.IExpressionType;
import si.isystem.connect.IVariable;
import si.isystem.connect.IVectorVariables;

public class JCompoundType {

    private JVariable m_expressionInfo;
    private JVariable [] m_children;
    
    public JCompoundType(IExpressionType expressionTypeInfo, String partitionName, int partitionIdx) {
        IVariable expression = expressionTypeInfo.Expression();
        m_expressionInfo = new JVariable(expression, partitionName, partitionIdx, false);
        
        IVectorVariables children = expressionTypeInfo.Children();
        int numChildren = (int)children.size();
        m_children = new JVariable[numChildren];
        
        for (int i = 0; i < numChildren; i++) {
            IVariable var = children.at(i);
            m_children[i] = new JVariable(var, partitionName, partitionIdx, false);
        }
    }

    
    public JVariable getExpressionInfo() {
        return m_expressionInfo;
    }
    
    
    public JVariable[] getChildren() {
        return m_children;
    }
    

    public String[] getChildrenNames() {

        String []members = new String[m_children.length];

        int idx = 0;
        for (JVariable var : m_children) {
            members[idx++] = var.getName();
        }

        return members;
    }
}
