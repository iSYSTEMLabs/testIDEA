package si.isystem.itest.model.actions;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CYAMLUtil;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * Copies value from src to dest.
 * Note: CTestBase is derived from CTestObject, so it's data can also be assigned.
 * 
 * @author markok
 *
 */
public class AssignTestObjectAction extends AbstractAction {

    protected CTestObject m_testObjDest;
    protected CTestObject m_testObjSrc;
    protected CTestObject m_testObjOldValue;

    protected ENodeId m_nodeId;
    

    /**
     * 
     * @param dest object to get the new value
     * @param src object containing the new value
     * @param nodeId
     */
    public AssignTestObjectAction(CTestObject dest, CTestObject src, ENodeId nodeId) {
        super("Assign test base action");
   
        m_testObjOldValue = dest.copy();
        m_testObjDest = dest;
        m_testObjSrc = src;
        m_nodeId = nodeId;
    }

    
    @Override
    public boolean isModified() {
        if (m_testObjDest instanceof CTestBase) {
            return !((CTestBase)m_testObjDest).equalsData((CTestBase)m_testObjSrc);
        } else if (m_testObjDest instanceof CTestBaseList) {
            return !((CTestBaseList)m_testObjDest).equalsData((CTestBaseList)m_testObjSrc);
        }

        return true; // CYAML object do not have method equalsData(), but use 
                     // operator overloading - implement equalsData() if needed
    }
    
    
    @Override
    public void exec() {
        assignButKeepParent(m_testObjSrc);
    }


    @Override
    public void undo() {
        assignButKeepParent(m_testObjOldValue);
    }

    
    @Override
    public void redo() {
        exec();
    }


    private void assignButKeepParent(CTestObject src) {

        m_testObjDest.assign(src);
        
        // Assign original parent to dest. Otherwise copying section from
        // base class to derived class sets parent of derived section
        // to bade parent, which is bad.
        if (CYAMLUtil.isInstanceOfCtb(m_testObjDest)) {
            CTestBase parent = CTestBase.cast(m_testObjDest).getParent();
            if (parent != null) {
                CTestBase.cast(m_testObjDest).setParent(parent);
            }
        } else if (CYAMLUtil.isInstanceOfCtbList(m_testObjDest)) {
            CTestBaseList destList = CTestBaseList.cast(m_testObjDest);
            CTestBase parent = destList.getParent();
            destList.setParentToElements(parent);
        }
    }
}    

