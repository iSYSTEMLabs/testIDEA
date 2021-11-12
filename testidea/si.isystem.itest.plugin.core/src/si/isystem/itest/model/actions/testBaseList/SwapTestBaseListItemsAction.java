package si.isystem.itest.model.actions.testBaseList;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.itest.model.AbstractAction;

/**
 * This class moves selected block of CTestBase classes down or up for one row -
 * it actually moves the row below or above the selected block.
 *  
 * @author markok
 *
 */
public class SwapTestBaseListItemsAction extends AbstractAction {

    private CTestBaseList m_tbList;
    private int m_startIdx;
    private int m_numItems;
    private boolean m_isMoveDown;

    private int m_srcIdx;
    private int m_destIdx;
    
    
    public SwapTestBaseListItemsAction(CTestBaseList tbList, 
                                       int startIdx,
                                       int numItems,
                                       boolean isMoveDown) {
        super("SwapTestBaseListItemsAction");
        m_tbList = tbList;
        m_startIdx = startIdx;
        m_numItems = numItems;
        m_isMoveDown = isMoveDown;
    }

    
    @Override
    public void exec() {
        
        m_destIdx = m_startIdx; 
                
        if (m_isMoveDown) {
            // move row up, to move selected block down
            m_srcIdx = m_startIdx + m_numItems;
            if (m_srcIdx >= m_tbList.size()) {
                return;
            }
            
        } else {
            // move row down, to move selected block up
            m_srcIdx = m_startIdx - 1;
            if (m_srcIdx < 0) {
                return;
            }

            m_destIdx += m_numItems - 1;
        }
        
        CTestBase movedTestBase = m_tbList.get(m_srcIdx);
        m_tbList.remove(m_srcIdx);
        m_tbList.add(m_destIdx, movedTestBase);
    }

    
    @Override
    public void undo() {
        if (m_srcIdx < 0  ||  m_srcIdx >= m_tbList.size()) {
            return;
        }
    
        CTestBase movedTestBase = m_tbList.get(m_destIdx);
        m_tbList.remove(m_destIdx);
        m_tbList.add(m_srcIdx, movedTestBase);
    }

    
    @Override
    public void redo() {
        exec();
    }

}
