package si.isystem.itest.ui.spec.group;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestCoverageResult;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.ETristate;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.CvrgStatControls;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class GroupCoverageStatEditor extends GroupSectionEditor {

    private CvrgStatControls m_allCodeStats = new CvrgStatControls();
    private CvrgStatControls m_testCaseOnlyStats = new CvrgStatControls();
    
    public GroupCoverageStatEditor(ENodeId nodeId, ESectionCTestGroup ... sectionId) {
        super(nodeId, sectionId);
    }

    
    @Override
    public boolean hasErrorStatus() {
        return m_testGroup.getCoverageExportConfig(true).isActive() == ETristate.E_TRUE;
    }

    
    @Override
    public boolean isError(CTestGroupResult result) {
        return result.isError();
    }

    
    @Override
    public boolean isActive() {
        return m_testGroup.getCoverageExportConfig(true).isActive() == ETristate.E_TRUE;
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        
        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);

        MigLayout mig = new MigLayout("fill", "[min!][fill]", "[min!][min!][min!][min!][min!][min!][fill]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
//        createMainResultsControls(builder);

        createCoverageStatsPanel(builder, "Coverage of all code in group", m_allCodeStats);
        createCoverageStatsPanel(builder, "Coverage of tested functions", m_testCaseOnlyStats);
        
        // createExtInfoDlgButtons(builder);  IMPL. postponed

        return configureScrolledComposite(scrolledPanel, mainPanel);
    }

    
    @SuppressWarnings("unused")
    private void createExtInfoDlgButtons(KGUIBuilder parentBuilder) {
        
        Button srcCodeCvrgBtn = parentBuilder.button("Show coverage with source code", "skip, w ::pref, gaptop 12, split 2");
        UiTools.setToolTip(srcCodeCvrgBtn, "Opens dialog with coverage markers for source and assembly code.");
    }

    
    private void createCoverageStatsPanel(KGUIBuilder parentBuilder, 
                                          String groupTitle,
                                          CvrgStatControls statControls) {

        
        KGUIBuilder builder = parentBuilder.group(groupTitle, 
                                                  "gaptop 15, growx, span 2, wrap",
                                                  true,
                                                  "", 
                                                  "[min!][min!][min!][min!][min!]",
                                                  "[min!][min!][min!][min!][min!][min!]");

        statControls.createControls(builder, m_nodeId);
    }

    
    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testGroup != null;
        
        if (isEnabled) {
            isEnabled = m_testGroup.getCoverageExportConfig(true).isActive() == ETristate.E_TRUE  &&
                        !m_testGroup.getMergedAnalyzerFileName().isEmpty();
        }
        
        m_allCodeStats.setEnabled(isEnabled);
        m_testCaseOnlyStats.setEnabled(isEnabled);

        if (!isEnabled) {
            m_allCodeStats.setInput(null, false);
            m_testCaseOnlyStats.setInput(null, false);
            return;            
        }

        m_allCodeStats.setInput(m_testGroup.getCoverageStatForAllCodeInGroup(false), false);
        m_testCaseOnlyStats.setInput(m_testGroup.getCoverageStatForTestCasesOnly(false), false);

        
        CTestGroupResult groupResult = m_model.getGroupResult(m_testGroup); 
        if (groupResult != null) {
            CTestCoverageResult cvrgResult = groupResult.getCoverageResultForAllCode(true);
            CTestCoverageStatistics measuredStat = cvrgResult.getMeasuredCoverage(true);
            m_allCodeStats.setMeasuredValues(cvrgResult, measuredStat);
            
            cvrgResult = groupResult.getCoverageResultForTestedCode(true);
            measuredStat = cvrgResult.getMeasuredCoverage(true);
            m_testCaseOnlyStats.setMeasuredValues(cvrgResult, measuredStat);
        } else {
            m_allCodeStats.clearResults();
            m_testCaseOnlyStats.clearResults();
        }
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{ESectionCTestGroup.E_SECTION_COVERAGE_ALL_CODE_IN_GROUP.swigValue(),
                         ESectionCTestGroup.E_SECTION_COVERAGE_TEST_CASES_ONLY.swigValue()};
    }
}


