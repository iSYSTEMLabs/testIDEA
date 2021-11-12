package si.isystem.itest.model.actions.mapping;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.AbstractAction;

public class ReplaceMappingKeyAction extends AbstractAction {

    private CTestBase m_testBase;
    private int m_section;
    private String m_oldKey;
    private String m_newKey;


    public ReplaceMappingKeyAction(CTestBase testBase,
                                   int section,
                                   String oldKey,
                                   String newKey) {
        super("ReplaceMappingKeyAction", testBase);
        
        m_testBase = testBase;
        m_section = section;
        
        m_oldKey = oldKey;
        m_newKey = newKey;
    }

    
    @Override
    public CTestTreeNode getContainerTreeNode() {
        return m_testBase.getContainerTestNode();
    }

    
    @Override
    public boolean isModified() {
        return !m_oldKey.equals(m_newKey);
    }

    
    @Override
    public void exec() {
        CMapAdapter mapAdapter = new CMapAdapter(m_testBase, m_section, false);
        mapAdapter.renameKey(m_oldKey, m_newKey);
    }


    @Override
    public void undo() {
        CMapAdapter mapAdapter = new CMapAdapter(m_testBase, m_section, false);
        mapAdapter.renameKey(m_newKey, m_oldKey);
    }


    @Override
    public void redo() {
        exec();
    }
}
