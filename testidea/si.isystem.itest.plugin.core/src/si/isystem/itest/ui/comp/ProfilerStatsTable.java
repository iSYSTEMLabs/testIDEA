/*
package si.isystem.itest.ui.comp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestPoint.ETestPointSections;
import si.isystem.connect.CTestPointResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.EScriptFunctionType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tableeditor.IResultProvider;
import si.isystem.tableeditor.IResultProvider.EResultStatus;
import si.isystem.tableeditor.TestBaseListTable;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

/
 * This class wraps table for editing data in dynamic classes derived from
 * CTestBase. Columns and rows in that table are configured and set automatically
 * from meta data in the input class (derived from CTestBase).
 *  
 * Usage examples: steps for stubs and testpoints.
 * 
 * @author markok
 *
 /
public class ProfilerStatsTable {

    private String m_caption;
    private TestBaseListTable m_stepsTable;
    private ProfilerStatsResultProvider m_testResultsProvider;
    // private Button m_viewAllResultsBtn;
    private CTestSpecification m_currentTestSpec;
    private int m_resultSectionId;
    
    public ProfilerStatsTable(String caption, int resultSectionId) {
        m_caption = caption;
        m_resultSectionId = resultSectionId;
    }
    
    
    public void createControls(KGUIBuilder builder, ViewPart parentView, 
                               final String resultsDialogTitle) {
        
        KGUIBuilder captionBuilder = builder.newPanel("fill", "span 2, growx, wrap", SWT.NONE);
        Label actionsLbl = captionBuilder.label(m_caption, "bottom");
        actionsLbl.setFont(FontProvider.instance().getBoldControlFont(actionsLbl));
        
        
        m_testResultsProvider = new ProfilerStatsResultProvider();
        m_stepsTable = new TestBaseListTable();
        m_stepsTable.setResultsProvider(m_testResultsProvider);
        
        Control stepsTableControl = m_stepsTable.createControl(builder.getParent(), 
                                                               new CTestPoint(),
                                                               ETestPointSections.E_SECTION_STEPS.swigValue(),
                                                               ENodeId.TEST_POINT_NODE,
                                                               parentView);
        stepsTableControl.setLayoutData("wmin 0, span 2, grow");
        m_stepsTable.setTooltip("This table defines tests and assignments to be performed when " +
                           "test point is hit. Empty lines are ignored.\n\n" +

                           "The 'expect' columns define expressions, which must evaluate to true, otherwise the test will fail.\n\n" +
                           
                           "The 'assign' section of columns contains assignments for local variables, global variables,and\n" +
                           "return value. We can add/remove/rename variables here.\n\n" +
                           
                           "The 'scriptParams' section contains values for script parameters, if script function is defined.\n" +
                           "Parameters are positional, and do not have names. Header row shows their indices\n\n" +
                           "The 'next' column can contain indices of next steps. If the cell is " +
                           "empty, the assignments from the next row (step),\n" +
                           "are executed on next test point hit. " +
                           "If the last line has no next step index set, it is repeated for all\n" +
                           "remaining test point hits. Step numbers are shown in the leftmost column.\n\n" +
                           "Shortcuts:\n" +
                           "F2 - edit\n" +
                           "Enter - edit" +
                           "Esc - revert editing\n" +
                           "Del - delete cell contents\n" +
                           "Ctrl + num + - add column or row if column or row is selected\n" +
                           "Ctrl + num - - delete selected column or row\n" +
                           "Backspace - clear cell and start editing\n" +
                           "Ctrl + Space - column selection mode\n" +
                           "Ctrl + Space - row selection mode\n" +
                           "Ctrl + C, Ctrl + X,  Ctrl + V - standard clipboard shortcuts");
    }
    

    public void fillControls(CTestBaseList testBaselist, boolean enableViewResultsButton) {

     //   m_viewAllResultsBtn.setEnabled(enableViewResultsButton);
        
        // m_currentTestSpec = testBase.getContainerTestSpec();
        
//        CTestBaseList steps = testBase.getTestBaseList(section, true);
     //   m_testResultsProvider.setInput(TestSpecificationModel.getInstance().getResult(m_currentTestSpec), 
     //                                  itemId, steps);

  //      m_stepsTable.refresh(testBase, section);
    }


    public void clear(CTestBase testBase, int section) {
        // m_viewAllResultsBtn.setEnabled(false);
        m_stepsTable.refresh(testBase, section);
    }


    public void setEnabled(boolean isEnabled) {
        m_stepsTable.setEnabled(isEnabled);        
    }
    
    
    public Control getControl() {
        return m_stepsTable.getControl();
    }
    
    
    public EResultStatus getTPOrStubResult(String itemId, int sectionId) {

        if (m_currentTestSpec == null) {
            return EResultStatus.NO_RESULT;
        }
        
        CTestResult result = TestSpecificationModel.getInstance().getResult(m_currentTestSpec);
        
        if (result == null) {
            return EResultStatus.NO_RESULT;
        }
        
        CTestBaseList tpResults = result.getTestBaseList(sectionId, true);
        
        EResultStatus status = EResultStatus.OK;
        int numResults = (int) tpResults.size();
        for (int resultIdx = 0; resultIdx < numResults; resultIdx++) {
            CTestBase tBase = tpResults.get(resultIdx);
            CTestPointResult tpResult = CTestPointResult.cast(tBase);
            
            if (tpResult.getId().equals(itemId)) {
                if (tpResult.isError()) {
                    // as soon as there is an error in one test point or stub hit, 
                    // mark it as failed
                    status = EResultStatus.ERROR;
                    break;
                }
            }
        }
        
        
        if (result.isScriptError(EScriptFunctionType.SE_STUB)) {
            // currently it is not possible to get info which script failed!
            status = EResultStatus.NO_RESULT; // EResultStatus.ERROR;
        }
        
        return status;
    }
}


class ProfilerStatsResultProvider implements IResultProvider 
{

    @Override
    public EResultStatus getCellResult(int col, int row, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EResultStatus getColumnResult(int col, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EResultStatus getRowResult(int row, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EResultStatus getTableResult(StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }
    
}

*/