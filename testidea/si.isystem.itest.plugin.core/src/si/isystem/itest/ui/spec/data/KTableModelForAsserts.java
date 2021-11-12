package si.isystem.itest.ui.spec.data;

import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestExprResult;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.IModelWithComment;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;

public class KTableModelForAsserts extends KTableModelForSequence implements IModelWithComment{

    private CTestBaseList m_results;
    private boolean m_isException;
    
    public KTableModelForAsserts(int sectionId,
                                 ENodeId nodelId) {
        super(sectionId, nodelId, "Expressions");
    }

    
    @Override
    public TextIconsContent getBodyCellContent(int col, int row) {

        super.getBodyCellContent(col, row);
        
        // add results if available
        String diff = null;
        Boolean isOK = null;
        if (m_results != null  &&  m_results.size() > row) {
                
            CTestBase tbExprResult = m_results.get(row);
            CTestExprResult exprResult = CTestExprResult.cast(tbExprResult);
                
            diff = exprResult.toUIString();

            isOK = !exprResult.isError()  &&  !m_isException;
        }
            
        if (diff != null) {
            m_editableCellContent.setTooltip(EIconPos.EBottomLeft, diff);
        } else {
            m_editableCellContent.setTooltip(EIconPos.EBottomLeft, "");
        }

        m_editableCellContent.setIcon(EIconPos.EBottomLeft, 
                                      getResultIcon(diff != null, isOK), 
                                      true);
        
        return m_editableCellContent;
    }

    
    public void setData(CTestAssert tAssert, 
                        CTestBaseList results, 
                        boolean isException) {
        super.setData(tAssert);
        m_results = results;
        m_isException = isException;
    }
}
