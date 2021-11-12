package si.isystem.itest.ui.spec.data;

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import si.isystem.connect.CTestCoverageResult;
import si.isystem.connect.CTestCoverageResult.ESectionCoverageResult;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestCoverageStatistics.ECoverageStatSectionId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class CvrgStatControls {
    
    static public class ExpectedMeasuredStatUITuple {

        private static final DecimalFormat m_percentageFormat = new DecimalFormat("###.##");
        
        private TBControlText m_expected;
        private Label m_measuredRel;
        private Label m_coveredDividedByAll;
        
        
        public void createControlsForOneTuple(KGUIBuilder builder, 
                                              String label, 
                                              int testSectionId, 
                                              ENodeId nodeId) {
            createControlsForOneTuple(builder, label, testSectionId, nodeId, false);
        }
        
        
        public void createControlsForOneTuple(KGUIBuilder builder, 
                                              String label, 
                                              int testSectionId, 
                                              ENodeId nodeId,
                                              boolean isSkip) {
            if (isSkip) {
                builder.label(label, "skip, alignx right, gapright 10");
            } else {
                builder.label(label, "alignx right, gapright 10");
            }

            String txtLayout = "wmin 60, gapright 3";

            m_expected = TBControlText.createForMixed(builder, 
                                                      "Expected coverage. Measured value must be higher or equal.", 
                                                      txtLayout, 
                                                      testSectionId, 
                                                      nodeId, 
                                                      null, 
                                                      SWT.BORDER | SWT.RIGHT);

            builder.label("%", "gapright 20");

            m_measuredRel = builder.label("", "growx", SWT.BORDER);
            m_measuredRel.setAlignment(SWT.RIGHT);
            UiTools.setToolTip(m_measuredRel, "Measured value");
            builder.label("%", "gapright 20");

            m_coveredDividedByAll = builder.label("", "growx, wrap", SWT.BORDER);
            m_coveredDividedByAll.setAlignment(SWT.RIGHT);
            UiTools.setToolTip(m_coveredDividedByAll, "Num. of covered items / all items");
        }
        
        
        public void setEnabled(boolean isEnabled) {
            m_expected.setEnabled(isEnabled);
        }
        
        
        public void clear() {
            m_expected.clearInput();
            m_expected.markAsNoneditable();
            m_measuredRel.setText("");
            m_measuredRel.setBackground(ColorProvider.instance().getBkgNoneditableColor());
            m_coveredDividedByAll.setText("");
        }

        
        public void clearResults() {
            m_measuredRel.setText("");
            m_measuredRel.setBackground(ColorProvider.instance().getBkgNoneditableColor());
            m_coveredDividedByAll.setText("");
        }
        
        
        public void setResult(float measuredRel,
                              double measuredAll,
                              boolean isError) {
            if (isError) {
                m_measuredRel.setBackground(ColorProvider.instance().getStrongErrorColor());
            } else {
                m_measuredRel.setBackground(ColorProvider.instance().getBkgNoneditableColor());
            }
            
            String[] res = getResultAsStr(measuredRel, measuredAll);
            
            m_measuredRel.setText(res[0]);
            m_coveredDividedByAll.setText(res[1]);
        }


        public static String[] getResultAsStr(float measuredRel, double measuredAll) {
            int measuredAbs = -1;
            String[] res = new String[2];
            
            if (measuredRel >= 0) {
                measuredAbs = (int)Math.round(measuredRel * measuredAll / 100);
                res[0] = fmtPercentage(measuredRel) + " ";
                res[1] = measuredAbs + " / " + (int)Math.round(measuredAll) + " ";
            } else {
                res[0] = "/ ";
                res[1] = measuredAbs + " / " + measuredAll + " ";
            }
            
            return res;
        }
        
        
        private static String fmtPercentage(double number) {
            if (Double.isNaN(number)) {
                return "/";   // this happens when the number of bytes or branches 
                              // in function is 0
            }
            return m_percentageFormat.format(number);
        }
    }
    
    
    
    
    ExpectedMeasuredStatUITuple m_objCodeTuple;
    ExpectedMeasuredStatUITuple m_srcCodeTuple;
    ExpectedMeasuredStatUITuple m_conditionsAllTuple;
    ExpectedMeasuredStatUITuple m_conditionsTrueTuple;
    ExpectedMeasuredStatUITuple m_conditionsFalseTuple;
    ExpectedMeasuredStatUITuple m_conditionsBothTuple;
    
    
    public void createControls(KGUIBuilder builder, ENodeId nodeId) {
        
        Label lbl = builder.label("Expected   ", "skip, alignx center, gaptop 20");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        builder.label("   Result    ", "skip, alignx center, gaptop 20").
        setFont(FontProvider.instance().getBoldControlFont(lbl));
        builder.label("Covered / all", "skip, wrap, alignx center, gaptop 20").
        setFont(FontProvider.instance().getBoldControlFont(lbl));
    
        builder.label("Statement coverage:", "gapright 10, wrap").
        setFont(FontProvider.instance().getBoldControlFont(lbl));
        
        m_objCodeTuple = new ExpectedMeasuredStatUITuple();
        m_objCodeTuple. 
                createControlsForOneTuple(builder, "Object code (bytes):", 
                                          ECoverageStatSectionId.E_SECTION_BYTES_EXECUTED.swigValue(),
                                          nodeId);

        m_srcCodeTuple = new ExpectedMeasuredStatUITuple(); 
        m_srcCodeTuple.
                createControlsForOneTuple(builder, "Source code (lines):", 
                                          ECoverageStatSectionId.E_SECTION_SOURCE_LINES_EXECUTED.swigValue(),
                                          nodeId);

        builder.label("Condition coverage:", "alignx right, gapright 10, wrap").
            setFont(FontProvider.instance().getBoldControlFont(lbl));

        m_conditionsAllTuple = new ExpectedMeasuredStatUITuple();
        m_conditionsAllTuple. 
                createControlsForOneTuple(builder, "Condition any:", 
                                          ECoverageStatSectionId.E_SECTION_BRANCH_EXECUTED.swigValue(),
                                          nodeId);

        m_conditionsTrueTuple = new ExpectedMeasuredStatUITuple();
        m_conditionsTrueTuple. 
                createControlsForOneTuple(builder, "Cond. true only:", 
                                          ECoverageStatSectionId.E_SECTION_BRANCH_TAKEN.swigValue(),
                                          nodeId);

        m_conditionsFalseTuple = new ExpectedMeasuredStatUITuple();
        m_conditionsFalseTuple. 
                createControlsForOneTuple(builder, "Cond. false only:", 
                                          ECoverageStatSectionId.E_SECTION_BRANCH_NOT_TAKEN.swigValue(),
                                          nodeId);

        m_conditionsBothTuple = new ExpectedMeasuredStatUITuple();
        m_conditionsBothTuple.
                createControlsForOneTuple(builder, "Condition both:", 
                                          ECoverageStatSectionId.E_SECTION_BRANCH_BOTH.swigValue(),
                                          nodeId);
    }
    
    
    public void setEnabled(boolean isEnabled) {
        m_objCodeTuple.setEnabled(isEnabled);
        m_srcCodeTuple.setEnabled(isEnabled);
        m_conditionsAllTuple.setEnabled(isEnabled);
        m_conditionsTrueTuple.setEnabled(isEnabled);
        m_conditionsFalseTuple.setEnabled(isEnabled);
        m_conditionsBothTuple.setEnabled(isEnabled);
        
        // m_isMcDcRequiredHC.setEnabled(isEnabled);
    }
    
    
    public void setInput(CTestCoverageStatistics input, boolean isMerged) {
        
        if (input == null) {
            m_objCodeTuple.clear();
            m_srcCodeTuple.clear();
            m_conditionsAllTuple.clear();
            m_conditionsTrueTuple.clear();
            m_conditionsFalseTuple.clear();
            m_conditionsBothTuple.clear();
            return;
        }
        
        m_objCodeTuple.m_expected.setInput(input, isMerged);
        m_srcCodeTuple.m_expected.setInput(input, isMerged);
        m_conditionsAllTuple.m_expected.setInput(input, isMerged);
        m_conditionsTrueTuple.m_expected.setInput(input, isMerged);
        m_conditionsFalseTuple.m_expected.setInput(input, isMerged);
        m_conditionsBothTuple.m_expected.setInput(input, isMerged);
    }
    
    
    public void setMeasuredValues(CTestCoverageResult cvrgResult,
                                  CTestCoverageStatistics measuredCvrgStat) {
        m_objCodeTuple.setResult( 
                  measuredCvrgStat.getBytesExecuted(), 
                  cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_BYTES_ALL), 
                  cvrgResult.isBytesExecutedError());

        m_srcCodeTuple.setResult(
                  measuredCvrgStat.getSourceLinesExecuted(),
                  cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_LINES_ALL), 
                  cvrgResult.isSourceLinesExecutedError());

        m_conditionsAllTuple.setResult( 
                  measuredCvrgStat.getBranchExecuted(), 
                  cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL), 
                  cvrgResult.isBranchesExecutedError());

        m_conditionsTrueTuple.setResult(
                  measuredCvrgStat.getBranchTaken(),
                  cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL), 
                  cvrgResult.isBranchesTakenError());

        m_conditionsFalseTuple.setResult( 
                  measuredCvrgStat.getBranchNotTaken(),
                  cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL), 
                  cvrgResult.isBranchesNotTakenError());

        m_conditionsBothTuple.setResult( 
                  measuredCvrgStat.getBranchBoth(),
                  cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL), 
                  cvrgResult.isBranchesBothError());
        
        /* disabled until we implement real MC/DC
        
        if (cvrgResult.isMcDcError()) {
            m_mcDcResultLbl.setBackground(ColorProvider.instance().getStrongErrorColor());
        } else {
            m_mcDcResultLbl.setBackground(ColorProvider.instance().getBkgNoneditableColor());
        }
        
        if (cvrgResult.isMcDc()) {
            m_mcDcResultLbl.setText("YES");
        } else {
            m_mcDcResultLbl.setText("NO");
        }
        */
    }


    public void clearResults() {
        m_objCodeTuple.clearResults();
        m_srcCodeTuple.clearResults();
        
        m_conditionsAllTuple.clearResults();
        m_conditionsTrueTuple.clearResults();
        m_conditionsFalseTuple.clearResults();
        m_conditionsBothTuple.clearResults();
        
        // m_mcDcResultLbl.setText("");
    }
}


