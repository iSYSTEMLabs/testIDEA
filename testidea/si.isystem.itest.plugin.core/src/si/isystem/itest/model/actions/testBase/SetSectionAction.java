package si.isystem.itest.model.actions.testBase;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This action is useful for sections, where entire section contains one
 * string, for example description, tags, testId, ...
 * This class does not fire 'data change' event in exec(), because in some cases
 * it updates controls prematurely, for example when inserting lines in stubs 
 * assignments table. 
 * 
 * @author markok
 */
public class SetSectionAction extends AbstractAction {

    protected CTestBase m_testBase;

    private YamlScalar m_valueNew;
    private YamlScalar m_valueOld;
    
    protected ENodeId m_nodeId;

    
    public SetSectionAction(CTestBase testBase,
                            ENodeId nodeId,
                            YamlScalar value) {
        
        super("Set section: " + value.getSectionId() + " = '" + value.getValue() + '\'', testBase);
        
        m_testBase = testBase;
        m_valueNew = value.copy();
        m_nodeId = nodeId;
        
        m_valueOld = value.copy(); // copy section id
        m_valueOld.dataFromTestSpec(m_testBase);
    }
    
    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }
    
    
    @Override
    public boolean isModified() {
        return !m_valueOld.equals(m_valueNew);
    }
    
    
    @Override
    public void exec() {
        m_valueNew.dataToTestSpec(m_testBase);
        
        // be careful when updating events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
    }

    
    @Override
    public void undo() {
        m_valueOld.dataToTestSpec(m_testBase);
    }

    
    @Override
    public void redo() {
        exec();
    }
    
    
    public void addDataChangedEvent() {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                                        null,
                                                        m_testBase.getContainerTestNode(), 
                                                        m_nodeId);
        addEvent(event);
    }
    
    
    public void addTreeChangedEvent() {
        ModelChangedEvent event = new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED,
                                                        null,
                                                        m_testBase.getContainerTestNode(), 
                                                        m_nodeId);
        addEvent(event);
    }
}
