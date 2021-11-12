package si.isystem.itest.model.actions.testBaseList;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;

/**
 * This class is used to remove items from CTEstBaseList, for example test points,
 * stubs, stub steps, ...
 * 
 * @author markok
 */
public class RemoveFromTestBaseListAction extends AbstractAction {

    private CTestBaseList m_tbList;
    private int m_idx;
    private CTestBase m_oldValue;

    public RemoveFromTestBaseListAction(CTestBaseList tbList, int idx) {
        super("Remove from test base list");
        m_tbList = tbList;
        m_idx = idx;
        
        m_oldValue = tbList.get(m_idx);
    }
    

    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_oldValue.getContainerTestNode();
    }

    
    @Override
    public void exec() {
        m_tbList.remove(m_idx);        
    }

    
    @Override
    public void undo() {
        
        m_tbList.add(m_idx, m_oldValue);
    }

    @Override
    public void redo() {
        exec();
    }
}
