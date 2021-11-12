package si.isystem.itest.ui.comp;

import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.WorkbenchPart;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestPoint.ETestPointSections;
import si.isystem.connect.CTestPointResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResultBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.common.ktableutils.KTableEditorModel;
import si.isystem.itest.dialogs.DynamicTableDialog;
import si.isystem.itest.dialogs.TableViewDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.IResultProvider.EResultStatus;
import si.isystem.tbltableeditor.TestBaseListTable;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

/**
 * This class wraps table for editing data in dynamic classes derived from
 * CTestBase, and adds result viewer.
 * Columns and rows in that table are configured and set automatically
 * from meta data in the input class (derived from CTestBase).
 *  
 * Usage examples: steps for stubs and testpoints.
 * 
 * @author markok
 *
 */
public class DynamicTable {

    public static final String STEPS_TABLE_TOOLTIP = "This table defines tests and assignments to be performed when " +
                       "test point or stub is hit. Empty lines are ignored.\n\n" +

                       "The 'expect' columns define expressions, which must evaluate to true, otherwise the test will fail.\n\n" +
                       
                       "The 'assign' section of columns contains assignments for local variables, global variables,and\n" +
                       "return value. We can add/remove/rename variables here.\n\n" +
                       
                       "The 'scriptParams' section contains values for script parameters, if script function is defined.\n" +
                       "Parameters are positional, and do not have names. Header row shows their indices\n\n" +
                       "The 'next' column can contain indices of next steps. If the cell is " +
                       "empty, the assignments from the next row (step),\n" +
                       "are executed on next hit. " +
                       "If the last line has no next step index set, it is repeated for all\n" +
                       "remaining hits. Step numbers are shown in the leftmost column.\n\n" +
                       KTableEditorModel.KTABLE_SHORTCUTS_TOOLTIP;
    
    private String m_caption;
    private TestBaseListTable m_stepsTable;
    private TestPointResultProvider m_testResultsProvider;
    private Button m_viewAllResultsBtn;
    private CTestTreeNode m_currentTestNode;
    private int m_resultSectionId;
    private TestSpecificationModel m_model;
    private Button m_dialogBtn;
    private CTestBase m_testBase;
    private int m_stepsSectionId;
    private CTestSpecification m_testSpecForResult;

    
    /**
     * 
     * @param caption
     * @param resultSectionId
     */
    public DynamicTable(String caption, int resultSectionId, int stepsSectionId) {
        
        m_caption = caption;
        m_resultSectionId = resultSectionId;
        m_stepsSectionId = stepsSectionId;
    }
    
    
    public void createControls(KGUIBuilder builder, WorkbenchPart parentView, 
                               final String resultsDialogTitle,
                               ENodeId nodeId) {
        
        final KGUIBuilder captionBuilder = builder.newPanel("fill", "[grow][min!][min!]", "",
                                                      "span 3, growx, wrap", SWT.NONE);
        Label actionsLbl = captionBuilder.label(m_caption, "bottom");
        actionsLbl.setFont(FontProvider.instance().getBoldControlFont(actionsLbl));
        
        m_dialogBtn = captionBuilder.button("Dialog");
        UiTools.setToolTip(m_dialogBtn, "Opens the table shown below in a dialog, so that\n"
                + "there is more space for editing.");
        m_dialogBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                DynamicTableDialog dlg = 
                        new DynamicTableDialog(captionBuilder.getParent().getShell(), 
                                               "Steps editor",
                                               m_model,
                                               m_testBase,
                                               m_stepsSectionId);
                dlg.open();
                m_stepsTable.refresh();
            }
        });
        
        m_viewAllResultsBtn = captionBuilder.button("Results ...", "al right");
        m_viewAllResultsBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                CTestResult testResult = m_model.getResult(m_testSpecForResult);
                if (testResult != null) {
                    TableViewDialog tableDialog = new TableViewDialog(Activator.getShell(), 
                                                                      resultsDialogTitle,
                                                                      testResult,
                                                                      m_resultSectionId);
                    tableDialog.open();
                }
            }
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        m_testResultsProvider = new TestPointResultProvider(m_resultSectionId);
        m_stepsTable = new TestBaseListTable(m_model, false);
        m_stepsTable.setResultsProvider(m_testResultsProvider);
        
        // CTestBaseList emptyInitList = new CTestBaseList();
        // emptyInitList.add(-1, new CTestPoint());
        // emptyInitList.add(-1, initDemoTestPoint("tp1", 0));
        // emptyInitList.add(-1, initDemoTestPoint("tp2", 10));
        // emptyInitList.add(-1, initDemoTestPoint("tp3", 100));
        // CTestPoint tp1 = initDemoTestPoint("tp1", 0);
        //System.out.println(tp1.toString());
        //tp1 = initDemoTestPoint("tp2", 10);
        //System.out.println(tp1.toString());
        //tp1 = initDemoTestPoint("tp3", 100);
        //System.out.println(tp1.toString()); 
        
        
        Control stepsTableControl = m_stepsTable.createControl(builder.getParent(), 
                                                               new CTestPoint(),
                                                               ETestPointSections.E_SECTION_STEPS.swigValue(),
                                                               nodeId,
                                                               parentView);
        stepsTableControl.setLayoutData("wmin 0, span 3, grow");
        m_stepsTable.setTooltip(STEPS_TABLE_TOOLTIP);
    }
    

    /**
     * Adds content provider.
     * 
     * @param sectionName name og iyaml section, for example 'params', 'expect', ...
     * @param provider content proposals provider
     */
    public void addContentProvider(String sectionName, IContentProposalProvider provider) {
        m_stepsTable.addContentProvider(sectionName, provider);
    }
    
    
    /**
     * 
     * @param testBase container test base, for example CTestStub
     * @param section section containing list of CTestBase objects, one row each, 
     *                for example stub steps
     * @param itemId used by result provider, for example stub func. name or test point ID
     * @param isEnableViewResultsButton
     * @param actionExecutioner must be the current model 
     */
    public void fillControls(CTestBase testBase,
                             CTestSpecification testSpecForResult,
                             int section, String itemId,
                             boolean isEnableDialogButon,
                             boolean isEnableViewResultsButton,
                             TestSpecificationModel model) {

        m_model = model;
        m_testBase =  testBase;
        m_testSpecForResult = testSpecForResult;
        m_stepsTable.setActionExecutioner(model);
        
        m_dialogBtn.setEnabled(isEnableDialogButon);
        m_viewAllResultsBtn.setEnabled(isEnableViewResultsButton);
        
        m_currentTestNode = testBase.getContainerTestNode();
        
        CTestBaseList steps = testBase.getTestBaseList(section, true);
        m_testResultsProvider.setInput(model.getResult(m_currentTestNode), 
                                       itemId, steps);

        m_stepsTable.setInput(testBase, section);
    }


    public void clear(CTestBase testBase, int section) {
        m_viewAllResultsBtn.setEnabled(false);
        m_dialogBtn.setEnabled(false);
        m_stepsTable.setInput(testBase, section);
    }


    public void setEnabled(boolean isEnabled) {
        m_stepsTable.setEnabled(isEnabled);        
    }
    
    
    public Control getControl() {
        return m_stepsTable.getControl();
    }
    
    
    public EResultStatus getTPOrStubResult(String itemId, int sectionId) {

        if (m_currentTestNode == null) {
            return EResultStatus.NO_RESULT;
        }
        
        CTestResult result = m_model.getResult(m_currentTestNode);
        
        if (result == null) {
            return EResultStatus.NO_RESULT;
        }
        
        CTestBaseList tpResults = result.getTestBaseList(sectionId, true);
        
        EResultStatus status = EResultStatus.NO_RESULT;
        int numResults = (int) tpResults.size();
        for (int resultIdx = 0; resultIdx < numResults; resultIdx++) {
            CTestBase tBase = tpResults.get(resultIdx);
            CTestPointResult tpResult = CTestPointResult.cast(tBase);
            
            if (tpResult.getId().equals(itemId)) {
                if (tpResult.isError()) {
                    // as soon as there is an error in one test point or stub hit, 
                    // mark it as failed
                    status = EResultStatus.ERROR;
                } else {
                    status = EResultStatus.OK;
                }
                break;
            }
        }
        
        
        if (result.isScriptError(CTestResultBase.getSE_STUB())) {
            // currently it is not possible to get info which script failed!
            status = EResultStatus.NO_RESULT; // EResultStatus.ERROR;
        }
        
        return status;
    }
}