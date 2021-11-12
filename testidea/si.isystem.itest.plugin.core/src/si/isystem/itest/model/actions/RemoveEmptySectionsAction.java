package si.isystem.itest.model.actions;

import java.util.ArrayList;
import java.util.List;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.connect.IntVector;
import si.isystem.itest.model.AbstractAction;

/**
 * This action checks the given CTestBase for sections, which have object assigned, 
 * but this object is empty. For example, there may be empty CTestStub object in 
 * the list, but if it empty it is ignored. This situation is likely to occur in
 * table editor, if user accidentally sets a value, but then decides the stub, 
 * test-point, ..., is not needed. 
 * 
 * See also comment in handler for this action.
 * 
 * @author markok
 *
 */
public class RemoveEmptySectionsAction extends AbstractAction {

    private static final int INVALID_IDX = -1;


    class RemovedListTB {
        
        public RemovedListTB(int section, int idx, CTestBase removedTb) {
            super();
            m_section = section;
            m_idx = idx;
            m_removedTb = removedTb;
        }
        
        int m_section;
        int m_idx;
        CTestBase m_removedTb;
    }
    
    private CTestBase m_parentNode;
    private List<RemovedListTB> m_removedSections = new ArrayList<>();

    public RemoveEmptySectionsAction(CTestBase parentNode) {
        super("RemoveEmptySections", parentNode);
        
        m_parentNode = parentNode;
        
        IntVector sections = new IntVector(); 
        m_parentNode.getNonNullEmptySections(sections);
        
        for (int sectionIdx = 0; sectionIdx < sections.size(); sectionIdx++) {
            int section = sections.get(sectionIdx);
            
            if (m_parentNode.getSectionType(section) == ETestObjType.ETestBase) {
                
                CTestBase emptyNonNullTestBase = m_parentNode.getTestBase(section, true);
                m_removedSections.add(new RemovedListTB(section, INVALID_IDX, emptyNonNullTestBase));
                
            } else if (m_parentNode.getSectionType(section) == ETestObjType.ETestBaseList) {
                
                CTestBaseList tbl = m_parentNode.getTestBaseList(section, false);
                int numItems = (int)tbl.size();
                for (int ti = 0; ti < numItems; ti++) {
                    CTestBase tb = tbl.get(ti);
                    if (tb.isEmpty()) {
                        m_removedSections.add(new RemovedListTB(section, ti, tb));
                    }
                }
            }
        }
    }
    
    
    @Override
    public void exec() {
        for (RemovedListTB removedItem : m_removedSections) {
            if (removedItem.m_idx == INVALID_IDX) {
                // it is member CTestBase
                m_parentNode.setSectionValue(removedItem.m_section, null);
            } else {
                // it is CTestBase in a list
                CTestBaseList tbl = m_parentNode.getTestBaseList(removedItem.m_section, false);
                tbl.remove(removedItem.m_idx);
            }
        }
    }

    
    @Override
    public void undo() {
        for (RemovedListTB removedItem : m_removedSections) {
            if (removedItem.m_idx == INVALID_IDX) {
                // it is member CTestBase
                m_parentNode.setSectionValue(removedItem.m_section, removedItem.m_removedTb);
            } else {
                // it is CTestBase in a list
                CTestBaseList tbl = m_parentNode.getTestBaseList(removedItem.m_section, false);
                tbl.add(removedItem.m_idx, removedItem.m_removedTb);
            }
        }
    }

    
    @Override
    public void redo() {
        exec();        
    }
    
    
    @Override
    public boolean isModified() {
        return !m_removedSections.isEmpty();
    }
}
