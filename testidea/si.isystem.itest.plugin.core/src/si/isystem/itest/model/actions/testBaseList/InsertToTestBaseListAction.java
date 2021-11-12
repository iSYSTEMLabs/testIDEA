package si.isystem.itest.model.actions.testBaseList;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;

/**
 * This class is used to add items from CTestBaseList, for example test points,
 * stubs, stub steps, profiler and coverage areas, ...
 * 
 * @author markok
 */
public class InsertToTestBaseListAction extends AbstractAction {

    private final CTestBaseList m_tbList;
    private final CTestBase m_insertedTb;
    private final int m_idx;
    
    /**
     * 
     * @param tbList
     * @param insertedTb
     * @param idx element index, may be -1 for insert at end of list
     */
    public InsertToTestBaseListAction(CTestBaseList tbList, CTestBase insertedTb, int idx) {
        super("Insert to test base list");
        m_tbList = tbList;
        m_insertedTb = insertedTb;
        m_idx = idx;
    }
    
    
    @Override
    public void exec() {
        m_tbList.add(m_idx, m_insertedTb);        
    }

    
    @Override
    public void undo() {
        
        m_tbList.remove(m_idx);
    }


    @Override
    public void redo() {
        exec();
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_insertedTb.getContainerTestNode();
    }
}
