package si.isystem.itest.ui.comp;

import si.isystem.connect.CLogResult;
import si.isystem.connect.CLogResult.ETestResultSections;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestPointResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResult.ETestResultSection;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.UiUtils;
import si.isystem.tbltableeditor.IResultProvider;

/**
 * Provides test result for test points in string form suitable to be shown in 
 * tooltips.
 * 
 * @author markok
 *
 */
public class TestPointResultProvider implements IResultProvider {

    private CTestResult m_result;
    private String m_stubFuncOrTestPointId;
    private ETestResultSection m_resultSectionId;
    private CTestBaseList m_steps;

    
    public TestPointResultProvider(ETestResultSection resultSectionId) {
        m_resultSectionId = resultSectionId;
    }


    public TestPointResultProvider(int resultSectionId) {
        m_resultSectionId = ETestResultSection.swigToEnum(resultSectionId);
    }


    public void setInput(CTestResult result,
                         String stubFuncOrTestPointId,
                         CTestBaseList steps) {
        m_result = result;
        m_stubFuncOrTestPointId = stubFuncOrTestPointId;
        m_steps = steps;
    }
    
    
    @Override
    public EResultStatus getCellResult(int col, int row, StringBuilder sb) {
        
        if (m_result == null) {
            return EResultStatus.NO_RESULT;
        }

        sb.delete(0, sb.length());

        if (m_steps == null  ||  row >= m_steps.size()) {
            return EResultStatus.NO_RESULT;
        }
        CTestEvalAssignStep step = CTestEvalAssignStep.cast(m_steps.get(row));
        CSequenceAdapter expected = step.getExpectedExpressions(true);
        if (col >= expected.size()) {
            return EResultStatus.NO_RESULT; // for other columns than expressions
                                            // there is no status info 
        }
        String expectedExpr = expected.getValue(col);
        
        CTestBaseList tpResults = m_result.getTestBaseList(m_resultSectionId.swigValue(), 
                                                           true);        
        EResultStatus status = EResultStatus.NO_RESULT; // if there is no error info, 
                                                 // expr. evaluated to TRUE
        int numResults = (int)tpResults.size();
        
        for (int idx = 0; idx < numResults; idx++) {
            
            CTestPointResult tpResult = CTestPointResult.cast(tpResults.get(idx));
            
            if (tpResult.getId().equals(m_stubFuncOrTestPointId)  &&  tpResult.getStepIdx() == row) {
                if (tpResult.isExprError()) {

                    StrVector resultExpressions = new StrVector();
                    StrVector results = new StrVector();
                    
                    tpResult.getExpressionErrors(resultExpressions, results);

                    int numResultExpres = (int) resultExpressions.size();
                    for (int resExprIdx = 0; resExprIdx < numResultExpres; resExprIdx++) {
                        if (resultExpressions.get(resExprIdx).equals(expectedExpr)) {
                            sb.append("----------\n");
                            sb.append("Hit: ").append(tpResult.getHitNo()).append("\n");
                            sb.append("    ").append(resultExpressions.get(resExprIdx)).append('\n');
                            sb.append(UiUtils.indentMultilineString(results.get(resExprIdx), 8)).append('\n');
                            status = EResultStatus.ERROR; // error is in this column
                        }
                    }
                } 
                
                if (status != EResultStatus.ERROR) { // keep error if one of expressions  
                                                     // was in error in one of steps
                    status = EResultStatus.OK;
                }
                
                if (sb.length() > 500) {
                    sb.append("\n\nMore data exists, see test report (File | Save test report) ...");
                    break;
                }
            }
        }
        
        return status;
    }

    
    @Override
    public EResultStatus getColumnResult(int col, StringBuilder sb) {
        // no column results at the moment - they may not be necessary 
        return EResultStatus.NO_RESULT;
    }

    
    @Override
    public EResultStatus getRowResult(int row, StringBuilder sb) {
        
        // CTestResult result = TestSpecificationModel.getInstance().getResult(m_result);
        
        if (m_result == null) {
            return EResultStatus.NO_RESULT;
        }
        
        sb.delete(0, sb.length());
        
        CTestBaseList tpResults = m_result.getTestBaseList(m_resultSectionId.swigValue(), 
                                                           true);                
        EResultStatus status = EResultStatus.NO_RESULT;
        int numResults = (int)tpResults.size();
        for (int idx = 0; idx < numResults; idx++) {
            CTestPointResult tpResult = CTestPointResult.cast(tpResults.get(idx));
            if (tpResult.getId().equals(m_stubFuncOrTestPointId)  &&  tpResult.getStepIdx() == row) {
                if (tpResult.isError()) {
                    status = EResultStatus.ERROR;
                } else {
                    // once set to error (in one hit), it should remain in error state
                    if (status != EResultStatus.ERROR) {  
                        status = EResultStatus.OK;
                    }
                }
                sb.append("---------------\n");
                sb.append("Hit: ").append(tpResult.getHitNo()).append('\n');
                String scriptInfoVar = tpResult.getScriptInfoVar();
                if (!scriptInfoVar.isEmpty()) {
                    sb.append("Script info:\n")
                      .append(UiUtils.indentMultilineString(scriptInfoVar, 4));
                }
                String scriptRetVal = tpResult.getScriptRetVal();
                if (!scriptRetVal.isEmpty()) {
                    sb.append("\nScript error:\n")
                      .append(UiUtils.indentMultilineString(scriptRetVal, 4));
                }
                
                StrVector expressions = new StrVector();
                StrVector results = new StrVector();
                tpResult.getExpressionErrors(expressions, results);
                expressions2Str(sb, expressions, results, "Expression errors:", false);
                
                CLogResult log = tpResult.getLogResult(true);
                log.getLog(expressions, results, ETestResultSections.E_SECTION_BEFORE_ASSIGN);
                expressions2Str(sb, expressions, results, "Logs before assignments:", true);
                
                log.getLog(expressions, results, ETestResultSections.E_SECTION_AFTER_ASSIGN);
                expressions2Str(sb, expressions, results, "Logs after assignments:", true);
                
                if (sb.length() > 500) {
                    sb.append("\n\nMore data exists, see dialog with results or test report (File | Save test report) ...");
                    break;
                }
            }
        }
        
        return status;
    }

    
    private void expressions2Str(StringBuilder sb,
                                 StrVector expressions,
                                 StrVector results,
                                 String sectionName,
                                 boolean isSingleLine) {
        int numExprs = (int) expressions.size();
        if (numExprs > 0) {
            sb.append('\n').append(sectionName).append("\n");
        }
        
        if (isSingleLine) {
            for (int exprIdx = 0; exprIdx < numExprs; exprIdx++) {
                sb.append("    ");
                sb.append(expressions.get(exprIdx)).append(": ");
                sb.append(results.get(exprIdx)).append('\n');
            }

        } else {
        
            for (int exprIdx = 0; exprIdx < numExprs; exprIdx++) {
                sb.append(UiUtils.indentMultilineString(expressions.get(exprIdx), 4));
                sb.append('\n').append(UiUtils.indentMultilineString(results.get(exprIdx), 8)).append('\n');
            } 
        }
    }

    
    @Override
    public EResultStatus getTableResult(StringBuilder sb) {
        // table result is currently displayed in a separate dialog with a table,
        // because it is to complex for a simple string.
        return EResultStatus.NO_RESULT;
    }
    
}
