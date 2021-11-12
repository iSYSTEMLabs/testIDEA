package si.isystem.itest.ui.spec.group;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.spec.AbstractSectionEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

abstract public class GroupSectionEditor extends AbstractSectionEditor {

    protected CTestGroup m_testGroup;
    private ESectionCTestGroup[] m_groupSections;
    protected ENodeId m_nodeId;
    
    protected TestSpecificationModel m_model;

    
    public GroupSectionEditor(ENodeId nodeId, ESectionCTestGroup ... sectionId) {
        m_nodeId = nodeId;
        m_groupSections = sectionId;
    }

    
    @Override
    public void setInputGroup(TestSpecificationModel model, CTestGroup testGroup) {
        m_model = model;
        m_testGroup = testGroup;
    }
    

    @Override
    public void setInputTestSpec(TestSpecificationModel model,
                                 CTestSpecification testSpec, 
                                 CTestSpecification mergedTestSpec)
    {}
    
    
    @Override
    public boolean isEmpty() {
        if (m_testGroup == null) {
            return true;
        }
        
        // if any of test sections is not empty, section editor is not empty
        for (ESectionCTestGroup id : m_groupSections) {
            if (!m_testGroup.isSectionEmpty(id.swigValue())) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void copySection(CTestTreeNode destGroup) {
        
        if (m_testGroup == null) { // can happen if user executes export without file opened
            return;
        }
        
        for (ESectionCTestGroup id : m_groupSections) {
            int section = id.swigValue();
            CTestObject testObj = m_testGroup.getSectionValue(section, true);
            
            if (!testObj.isEmpty()) {
                destGroup.getSectionValue(section, false).assign(testObj);
            }
        }
    }
    
    
    @Override
    public void clearSection() {
        AbstractAction action = createClearGroupSectionAction();
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testGroup);
        TestSpecificationModel.getActiveModel().execAction(action);
    }
    
    
    protected AbstractAction createClearGroupSectionAction() {
        GroupAction grp = new GroupAction("Clear sections in node " + m_nodeId);
        
        for (ESectionCTestGroup id : m_groupSections) {
            SetTestObjectAction action = new SetTestObjectAction(m_testGroup, 
                                                             id.swigValue(), 
                                                             null, 
                                                             m_nodeId);
            grp.add(action);
        }
        
        return grp;
    }
    
    
    @Override
    public CTestTreeNode createTestTreeNode() {
        return new CTestGroup();
    }

    
    @Override
    public CTestTreeNode getTestTreeNode() {
        return m_testGroup;
    }
}
