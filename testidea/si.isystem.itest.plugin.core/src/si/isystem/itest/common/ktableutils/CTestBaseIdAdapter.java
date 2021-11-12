package si.isystem.itest.common.ktableutils;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.SetCommentAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * Base class for elements of list tables (stubs, test points, cvrg, prof.).
 * Derived classes specialize for each type of elements (see class hierarchy). 
 * @author markok
 */
public abstract class CTestBaseIdAdapter {

    protected static final CTestBaseList EMPTY_CTB_LIST = new CTestBaseList();
    
    private int m_sectionId;
    private ENodeId m_nodeId;
    
    protected CTestBaseIdAdapter(int sectionId, ENodeId nodeId) {
        m_sectionId = sectionId;
        m_nodeId = nodeId;
    }
    
    
    /**
     * Sets ID of list item, for example name of stubbed function.
     * @param testBase element of list table, for example CTestStub, CTestPoint, ...
     */
    protected AbstractAction createSetIdAction(CTestBase testBase, 
                                               String newId) {
        
        YamlScalar value = YamlScalar.newMixed(m_sectionId);
        value.setValue(newId);
        return new SetSectionAction(testBase, m_nodeId, value);
    }

    
    protected AbstractAction createSetCommentAction(CTestBase testBase, 
                                                    String nlComment,
                                                    String eolComment) {
        YamlScalar value = YamlScalar.newMixed(m_sectionId);
        value.setNewLineComment(nlComment);
        value.setEndOfLineComment(eolComment);
        return new SetCommentAction(testBase, m_nodeId, value);

    }
    
    
    public String[] getComment(CTestBase testBase, int dataCol) {
        String [] comments = new String[2];
        
        if (dataCol == 0) {
            comments[0] = testBase.getComment(m_sectionId,
                                              SpecDataType.KEY,
                                              CommentType.NEW_LINE_COMMENT);
            comments[1] = testBase.getComment(m_sectionId,
                                              SpecDataType.VALUE,
                                              CommentType.END_OF_LINE_COMMENT);
        }
        return comments;
    }
    
    
    abstract public String getId(CTestBase testBase);
    abstract public CTestBase createNew(CTestBase parentTestBase);
    
    abstract public CTestBaseList getItems(boolean isConst);
    abstract public Boolean isError(int dataRow);
}
