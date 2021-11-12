package si.isystem.itest.model.actions.testBase;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestObject;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * Assigns new section value to CTestBase.
 * 
 * @author markok
 *
 */
public class SetTestObjectAction extends AbstractAction {

    protected CTestBase m_parent;
    private int m_sectionId;
    protected CTestObject m_newValue;
    protected CTestObject m_oldValue;

    protected ENodeId m_nodeId;
    

    /**
     * 
     * @param dest object to get the new value
     * @param src object containing the new value
     * @param nodeId
     */
    public SetTestObjectAction(CTestBase parent, int sectionId, CTestObject value, ENodeId nodeId) {
        super("Assign test base action");
   
        m_parent = parent;
        m_sectionId = sectionId;
        m_newValue = value;
        m_nodeId = nodeId;
        
        m_oldValue = parent.getSectionValue(sectionId, true);
        
        // always added where action is created! addDataChangedEvent(m_nodeId, parent);
    }

    
    @Override
    public boolean isModified() {
        if (m_newValue instanceof CTestBase) {
            return !((CTestBase)m_parent).equalsData((CTestBase)m_newValue);
        } 

        return true; // CYAML object do not have method equalsData(), but use 
                     // operator overloading - implement equalsData() if needed
    }
    
    
    @Override
    public void exec() {
        m_parent.setSectionValue(m_sectionId, m_newValue);
    }

    @Override
    public void undo() {
        m_parent.setSectionValue(m_sectionId, m_oldValue);
    }

    @Override
    public void redo() {
        exec();
    }

}
