package si.isystem.itest.ui.spec;

import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerCoverage.ECoverageSectionId;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageResult;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestCoverageStatistics.ECoverageStatSectionId;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrCoverageTestResultsMap;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.comp.TBControlCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.CvrgStatControls;
import si.isystem.tbltableeditor.HeaderPath;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.ui.utils.KGUIBuilder;

public class CoverageStatisticsEditor extends ListEditorBase {

    public static final String MEASURE_ALL_FUNCTIONS = "Measure all functions";

    private TBControlCheckBox m_measureAllFunctionsCb;

    private CvrgStatControls m_statControls;
    // disabled until we implement real MC/DC
    // private TBControlCheckBox m_isMcDcRequiredHC;
    // private Label m_mcDcResultLbl;
    
    private ISectionEditor m_parentEditor;
    
    
    CoverageStatisticsEditor() {
        super(ENodeId.COVERAGE_STATS_NODE, SectionIds.E_SECTION_ANALYZER);

        setListCommentSectionId(ECoverageSectionId.E_SECTION_STATISTICS.swigValue());

        NODE_PATH = SectionNames.COVERAGE_NODE_PATH + HeaderPath.SEPARATOR +
                    SectionNames.CVRG_STATS_NODE_NAME;

        CTestBaseIdAdapter adapter = 
                new CTestBaseIdAdapter(ECoverageStatSectionId.E_SECTION_FUNC_NAME.swigValue(),
                                       ENodeId.COVERAGE_STATS_NODE) {
            
            
            @Override
            public String getId(CTestBase testBase) {
                return CTestCoverageStatistics.cast(testBase).getFunctionName();
            }
            
            
            @Override
            public CTestBase createNew(CTestBase parentTestBase) {
                return new CTestCoverageStatistics(parentTestBase);
            }

            
            @Override
            public CTestBaseList getItems(boolean isConst) {
                if (m_currentTestSpec == null) {
                    return EMPTY_CTB_LIST;
                }
                return m_currentTestSpec.getAnalyzer(isConst)
                              .getCoverage(isConst).getStatisticsList(isConst);
            }

            
            @Override
            public Boolean isError(int dataRow) {
                return isErrorInCvrgStat(dataRow);
            }
        };
        
        setIdAdapter(adapter);
    }

    
    public void setParentEditor(ISectionEditor specEditor) {
        m_parentEditor = specEditor;
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        
        m_uiStrings.m_tableTitle = "Covered functions";
        m_uiStrings.m_tableTooltip = "Names of functions to be measured for coverage. "
                                     + "Select function to view\n" 
                                     + "settings on the right.";
        m_uiStrings.m_funcNameLabel = "Function:";
        
        Composite mainPanel = createScrollable(parent);
        mainPanel.setLayout(new MigLayout("fill"));

        Composite statsPanel = super.createPartControl(mainPanel, -1, -1, ""); 
        statsPanel.setLayoutData("growx, growy");
            
        statsPanel.setLayout(new MigLayout("fill", 
            "[min!][min!][min!][min!][min!] [min!][min!][min!][grow]",
            "[min!][min!][min!][min!][min!] [min!][min!][min!][min!][min!] [min!][min!][grow]"));
        
        KGUIBuilder builder = new KGUIBuilder(statsPanel);

        m_statControls = new CvrgStatControls();
        m_statControls.createControls(builder, m_nodeId);
        
        /* disabled until we implement true MC/DC
        
        builder.label("MC / DC:", "alignx right, gapright 10, wrap").
         
            setFont(FontProvider.instance().getBoldControlFont(m_funcNameLabel));
        
        builder.label("State:", "alignx right, gapright 10");
    
        Button mcDcCBox = builder.checkBox("");
        UiTools.setToolTip(mcDcCBox, "This check box has three states:\n\n" +
        		"- not checked: MC/DC coverage IS NOT required for test to pass\n\n" +
        		"- intemediate: default value, not explicitly specified in test specification.\n" +
        		"               MC/DC coverage IS NOT required for test to pass.\n\n" +
        		"- checked: MC/DC coverage IS required for test to pass");
         
        ValueAndCommentEditor tagEditor = ValueAndCommentEditor.newMixed(ECoverageStatSectionId.E_SECTION_MC_DC.swigValue(), 
                                                                         mcDcCBox);
        m_isMcDcRequiredHC = new TBControlCheckBox(mcDcCBox, tagEditor,
                                                          ENodeId.COVERAGE_STATS_NODE);
        m_isMcDcRequiredHC.setTristateLabels("No", "Default (No)", "Yes");
        
        m_mcDcResultLbl = builder.label("", "skip, growx, wrap", SWT.BORDER | SWT.CENTER);
        m_mcDcResultLbl.setAlignment(SWT.CENTER);
        UiTools.setToolTip(m_mcDcResultLbl, "MC/DC result");
        */
        
        m_statControls.setEnabled(false);
        
        return getScrollableParent(mainPanel);
    }


    @Override
    protected void createSectionSpecificCheckBox(KGUIBuilder builder) {
        m_measureAllFunctionsCb = new TBControlCheckBox(builder, 
                                                        MEASURE_ALL_FUNCTIONS, 
                                                        "If checked all functions are measured,\n"
                                                        + "if unchecked, only functions listed below are measured.\n"
                                                        + "Disabled, when predefined trigger is used (see section 'Analyzer').", 
                                                        "gapleft 8, gaptop 3, gapbottom 5, split 2", 
                                                        CTestAnalyzerCoverage.ECoverageSectionId.E_SECTION_IS_MEASURE_ALL_FUNCTIONS.swigValue(), 
                                                        ENodeId.COVERAGE_STATS_NODE, null);  
    }

    
    @Override
    protected void createItemIdControls(KGUIBuilder builder) {
        createFuncNameControls(builder);
    }

    
    @Override
    protected void fillSectionControls() {
        super.fillSectionControls();
        
        CTestAnalyzer analyzerCfg = m_currentTestSpec.getAnalyzer(false);
        CTestAnalyzerCoverage cvrg = analyzerCfg.getCoverage(false);
        // this check-box should be enabled regardless of statistics section
        if (analyzerCfg.isPredefinedTrigger() == ETristate.E_TRUE) {
            m_measureAllFunctionsCb.setInput(null, false);
        } else {
            m_measureAllFunctionsCb.setInput(cvrg, m_isInherited);
        }
        
        m_listCommentEditor.updateValueAndCommentFromTestBase(m_currentTestSpec.getAnalyzer(false).getCoverage(false));
    }

    
    @Override
    protected void fillListItemControls(CTestBase testBase) {
        super.fillListItemControls(testBase);
        
        m_commentChangedListener.setTestBase(m_currentTestSpec.getAnalyzer(false).getCoverage(false));
        
        CTestCoverageStatistics stat = CTestCoverageStatistics.cast(testBase);
        
        m_statControls.setInput(stat, m_isInherited);
        m_statControls.setEnabled(true);
        
        String functionName = stat.getFunctionName();
        UiUtils.setFuncParams(m_functionHeaderTxt, m_globalFunctionsProvider, functionName);
        
        // now set results, if available
        CTestResult testCaseResult = m_model.getResult(m_testSpec);

        boolean isResultSet = false;
        
        if (testCaseResult != null) {
            StrCoverageTestResultsMap coverageResults = new StrCoverageTestResultsMap();
            testCaseResult.getCoverageResults(coverageResults);

            if (coverageResults.containsKey(functionName)) {
                CTestCoverageResult cvrgResult = coverageResults.get(functionName);
                if (cvrgResult != null) {
                    
                    CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
                    m_statControls.setMeasuredValues(cvrgResult, measuredCvrgStat);

                    isResultSet = true;
                }
            }
        }
        
        if (!isResultSet) {
            m_statControls.clearResults();
        }
    }
    
    
    @Override
    protected void clearSectionControls() {
        super.clearSectionControls();
        m_measureAllFunctionsCb.setInput(null, false);
    }
    
    
    @Override
    protected void clearListItemControls() {
        super.clearListItemControls();
        
        m_statControls.setInput(null, false);
        m_functionHeaderTxt.setText("");
    }
    
    
    @Override
    protected void enableSectionControls(boolean isEnabled) {
        super.enableSectionControls(isEnabled);
        m_measureAllFunctionsCb.setEnabled(isEnabled);
    }
    
    
    @Override
    protected void enableListItemControls(boolean isEnabled) {
        super.enableListItemControls(isEnabled);
        m_statControls.setEnabled(isEnabled);
    }
    
    
    private Boolean isErrorInCvrgStat(int dataRow) {
        
        CTestResult result = m_model.getResult(m_testSpec);
        
        if (result == null) {
            return null;
        }
        
        Boolean retVal = null;
        
        CTestBase cvrgStatTB = m_currentTestSpec.getAnalyzer(true).getCoverage(true).getStatistics(dataRow);
        CTestCoverageStatistics cvrgStat = CTestCoverageStatistics.cast(cvrgStatTB);
        String itemId = cvrgStat.getFunctionName();
        
        StrCoverageTestResultsMap cvrgResults = new StrCoverageTestResultsMap();
        result.getCoverageResults(cvrgResults);
        if (cvrgResults.containsKey(itemId)) {
            CTestCoverageResult statResult = cvrgResults.get(itemId);
            if (statResult.isError()) {  
                retVal = Boolean.TRUE;    // error
            } else {
                retVal = Boolean.FALSE; // no error
            }
        }
        
        return retVal; // can be null, if no result for this function point was found 
    }

    
    @Override
    public boolean isActive() {
        return m_parentEditor.isActive();
    }
    
    
    @Override
    public boolean isEmpty() {
        if (m_testSpec == null) {
            return true;
        }

        CTestAnalyzerCoverage cvrg = m_testSpec.getAnalyzer(true).getCoverage(true);
        CTestBaseList stats = cvrg.getStatisticsList(true);
        return stats.size() == 0;
    }

    @Override
    public boolean hasErrorStatus() {
        return isActive()  &&  !isEmpty();
    }
    
    @Override
    public boolean isError(CTestResult result) {
        return result.isCodeCoverageError();
    }
    
    
    @Override
    public void copySection(CTestTreeNode testSpec) {
        CTestAnalyzerCoverage srcCvrg = m_testSpec.getAnalyzer(true).getCoverage(true);
        CTestAnalyzerCoverage destCvrg = CTestSpecification.cast(testSpec).getAnalyzer(false).getCoverage(false);
        
        destCvrg.assignStatistics(srcCvrg);
    }

    @Override
    public void clearSection() {
        SetTestObjectAction action = new SetTestObjectAction(m_testSpec.getAnalyzer(false).getCoverage(false), 
                                                             ECoverageSectionId.E_SECTION_STATISTICS.swigValue(), 
                                                             null, 
                                                             m_nodeId);
        
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);

        m_model.execAction(action);
        m_testSpec.clearEmptySections();
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestAnalyzerCoverage.ECoverageSectionId.E_SECTION_STATISTICS.swigValue()};
    }
}
