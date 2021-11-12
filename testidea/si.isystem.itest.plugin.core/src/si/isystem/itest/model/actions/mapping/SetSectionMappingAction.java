package si.isystem.itest.model.actions.mapping;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This action is useful for sections, where section contains mapping with 
 * custom keys, for example local vars, stub step assignments, ...
 * It sets value for the given key in the mapping. 
 *
 * @author markok
 */
public class SetSectionMappingAction extends AbstractAction {

    protected CTestBase m_testBase;

    private YamlScalar m_valueNew;
    private YamlScalar m_valueOld;
    
    protected ENodeId m_nodeId; // id of spec editor node

    /**
     * 
     * @param testBase
     * @param nodeId
     * @param value must be created as YamlScalar.newUserMapping(sectionId, key);
     * @param isFireUpdate
     */
    public SetSectionMappingAction(CTestBase testBase,
                                   ENodeId nodeId,
                                   YamlScalar value) {
        
        super("SetSectionMappingAction: " + value.getSectionId(), testBase);
        
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
        // Be careful when to fire events here - any refresh in Tree or Editor views confuses
        // table focus. The 'merged' icon is not updated on the fly, but that's not
        // a critical issue! If really required, try to modify the SWT Tree
        // control - maybe it does not change focus.
        // fireEventsOnExec();
    }

    
    @Override
    public void undo() {
        m_valueOld.dataToTestSpec(m_testBase);
    }

    
    @Override
    public void redo() {
        exec();
    }
}
