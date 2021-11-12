package si.isystem.itest.model.actions;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.IntVector;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.model.AbstractAction;

/**
 * This action overwrites selected test spec with pasted one, but only sections 
 * which are not empty in the pasted test spec. This way we can easily paste only
 * single sections to existing test specifications. 
 * 
 * @author markok
 *
 */
public class OverwriteTestAction extends AbstractAction {

    private CTestTreeNode m_selectedNode;
    private CTestTreeNode m_pastedNode;
    private CTestTreeNode m_originalNode;

    
    public OverwriteTestAction(CTestTreeNode selectedNode,
                               CTestTreeNode pastedNode) {
        super("Overwrite test");
        
        if (selectedNode.isGroup()  &&  pastedNode.isGroup()) {
            m_originalNode = new CTestGroup(CTestGroup.cast(m_selectedNode));
        } else if (!selectedNode.isGroup()  &&  !pastedNode.isGroup()) { 
            m_originalNode = new CTestSpecification(CTestSpecification.cast(m_selectedNode));
        } else {
            if (selectedNode.isGroup()  &&  !pastedNode.isGroup()) {
                throw new SIllegalArgumentException("Can not paste test case to section of test group!\n"
                        + "Select test group or test case in Outline view as paste destination.");
            }
            if (!selectedNode.isGroup()  &&  pastedNode.isGroup()) {
                throw new SIllegalArgumentException("Can not paste test group to section of test case!\n"
                        + "Select test group or test case in Outline view as paste destination.");
            }
            throw new SIllegalArgumentException("Pasted node and destination node must be of the same type  - either test case or group!");
        }
            
        m_selectedNode = selectedNode;
        m_pastedNode = pastedNode;
        m_originalNode.assign(m_selectedNode);
        m_originalNode.getChildren(true).clear(); // we don't need derived specs, save some memory
    }

    
    @Override
    public void exec() {
        assignNonEmptySections(m_pastedNode, m_selectedNode);
    }

    
    @Override
    public void undo() {
        assignNonEmptySections(m_originalNode, m_selectedNode);
    }
    
    
    @Override
    public void redo() {
        exec(); 
    }

    
    private void assignNonEmptySections(CTestBase src, CTestBase dest) {
        IntVector sectionIds = new IntVector();
        src.getSectionIds(sectionIds);
        int numSections = (int) sectionIds.size();
        for (int i = 0; i < numSections; i++) {
            int sectionId = sectionIds.get(i);
                    
            if (!m_pastedNode.isSectionEmpty(sectionId)) {
                CTestObject srcValue = src.getSectionValue(sectionId, true);
                srcValue = srcValue.copy();
                dest.setSectionValue(sectionId, srcValue);
                DataUtils.copyComment(src, sectionId, dest);
            }
        }
    }
}
