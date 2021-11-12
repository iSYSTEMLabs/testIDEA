package si.isystem.itest.ui.spec.table;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;

import si.isystem.connect.CProfilerTestResult;
import si.isystem.connect.CProfilerTestResult.ProfilerErrCode;
import si.isystem.connect.CStackUsageResult;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageResult;
import si.isystem.connect.CTestCoverageResult.ESectionCoverageResult;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestCoverageStatistics.ECoverageStatSectionId;
import si.isystem.connect.CTestExprResult;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestProfilerStatistics.EProfilerStatisticsSectionId;
import si.isystem.connect.CTestProfilerTime;
import si.isystem.connect.CTestProfilerTime.EProfilerTimeSectionId;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResult.ETestResultSection;
import si.isystem.connect.CTestResultBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.StrCoverageTestResultsMap;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.comp.TestPointResultProvider;
import si.isystem.itest.ui.spec.ListEditorBase;
import si.isystem.itest.ui.spec.data.CvrgStatControls;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.tbltableeditor.HeaderPath;
import si.isystem.tbltableeditor.IResultProvider;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.tbltableeditor.TestBaseListModel;

/**
 * This class provides results for CTEstSpecifiaction-s or CTestGroup-s in
 * table editor. It is not that performance critical, since methods are
 * called only for the rendered cells.  
 * 
 * @author markok
 *
 */
public class CTBTableResultProvider implements IResultProvider {

    private TestSpecificationModel m_model;
    private TestBaseListModel m_tblModel;
    
    private static Map<String, ICellResultProvider> m_resultProviders = null;

    private interface ICellResultProvider {
        /**
         * 
         * @param itemIndex index of item in the list, for example index of expression
         *                  or stub
         * @param sb
         * @return
         */
        EResultStatus getCellResult(CTestResult result, int[] listIndices, 
                                    int itemIndex, StringBuilder sb);
    }


    public CTBTableResultProvider() {
        initPaths();
    }
    
    
    private void initPaths() {
        if (m_resultProviders == null) {
            m_resultProviders = new TreeMap<>();
            SectionNames sn = SectionNames.INSTANCE;
            String tsPath = SectionNames.TEST_SPEC_NODE_PATH;
            
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_ASSERT)
                                  + HeaderPath.SEPARATOR + SectionNames.EXPR_NODE_NAME
                                  + HeaderPath.SEPARATOR + HeaderPath.SEQ_MARK, 
                                  new ExpressionsResultProvider());
            
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_PRE_CONDITION)
                                  + HeaderPath.SEPARATOR + SectionNames.EXPR_NODE_NAME
                                  + HeaderPath.SEPARATOR + HeaderPath.SEQ_MARK, 
                                  new PreconditionsResultProvider());
            
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_STACK_USAGE)
                                  + HeaderPath.SEPARATOR + SectionNames.STACK_USAGE_MAX_LIMIT_NODE_NAME,
                                  new StackUsageResultProvider());

            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_ASSERT)
                                  + HeaderPath.SEPARATOR + SectionNames.TARGET_EXCEPTION_NODE_NAME,
                                  new TargetExceptionResultProvider());

            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_STUBS)
                                  + HeaderPath.SEPARATOR + SectionNames.TARGET_EXCEPTION_NODE_NAME,
                                  new TargetExceptionResultProvider());

            // stub script
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_STUBS)
                                  + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK 
                                  + HeaderPath.SEPARATOR + SectionNames.SCRIPT_FUNC,
                                  new StubTestPoint_ScriptResultProvider(ETestResultSection.E_SECTION_STUB_RESULTS));
            //stub steps
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_STUBS)
                                  + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK 
                                  + HeaderPath.SEPARATOR + SectionNames.STUB_ASSIGN_STEPS
                                  + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK 
                                  + HeaderPath.SEPARATOR + SectionNames.EXPECT
                                  + HeaderPath.SEPARATOR + HeaderPath.SEQ_MARK,
                                  new StubTestPoint_StepResultProvider(ETestResultSection.E_SECTION_STUB_RESULTS));
            // test point script
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_TEST_POINTS)
                                  + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK
                                  + HeaderPath.SEPARATOR + SectionNames.SCRIPT_FUNC,
                                  new StubTestPoint_ScriptResultProvider(ETestResultSection.E_SECTION_TEST_POINT_RESULTS));
            // testpoint steps
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_TEST_POINTS)
                                  + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK 
                                  + HeaderPath.SEPARATOR + SectionNames.TEST_POINT_STEPS
                                  + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK 
                                  + HeaderPath.SEPARATOR + SectionNames.EXPECT
                                  + HeaderPath.SEPARATOR + HeaderPath.SEQ_MARK,
                                  new StubTestPoint_StepResultProvider(ETestResultSection.E_SECTION_TEST_POINT_RESULTS));
            // Coverage
            String cvrgPath = tsPath + HeaderPath.SEPARATOR
                    + sn.getTestSpecSectionName(SectionIds.E_SECTION_ANALYZER)
                    + HeaderPath.SEPARATOR + SectionNames.COVERAGE_NODE_NAME 
                    + HeaderPath.SEPARATOR + SectionNames.CVRG_STATISTICS
                    + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK 
                    + HeaderPath.SEPARATOR;
            
            m_resultProviders.put(cvrgPath + sn.getCvrgStatSName(ECoverageStatSectionId.E_SECTION_BYTES_EXECUTED),
                                  new Coverage_Code_ResultProvider());
            
            m_resultProviders.put(cvrgPath + sn.getCvrgStatSName(ECoverageStatSectionId.E_SECTION_SOURCE_LINES_EXECUTED),
                                  new Coverage_Lines_ResultProvider());
            
            m_resultProviders.put(cvrgPath + sn.getCvrgStatSName(ECoverageStatSectionId.E_SECTION_BRANCH_EXECUTED),
                                  new Coverage_BranchesAll_ResultProvider());
            
            m_resultProviders.put(cvrgPath + sn.getCvrgStatSName(ECoverageStatSectionId.E_SECTION_BRANCH_TAKEN),
                                  new Coverage_BranchesTaken_ResultProvider());
            
            m_resultProviders.put(cvrgPath + sn.getCvrgStatSName(ECoverageStatSectionId.E_SECTION_BRANCH_NOT_TAKEN),
                                  new Coverage_BranchesNotTaken_ResultProvider());
            
            m_resultProviders.put(cvrgPath + sn.getCvrgStatSName(ECoverageStatSectionId.E_SECTION_BRANCH_BOTH),
                                  new Coverage_BranchesBoth_ResultProvider());
            
            // Profiler
            EnumSet<EProfilerTimeSectionId> profilerTimes = EnumSet.allOf(EProfilerTimeSectionId.class);
            EnumSet<EProfilerStatisticsSectionId> codeAreaScopes = EnumSet.of(EProfilerStatisticsSectionId.E_SECTION_CALL_TIME,
                                                                                EProfilerStatisticsSectionId.E_SECTION_GROSS_TIME,
                                                                                EProfilerStatisticsSectionId.E_SECTION_NET_TIME,
                                                                                EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME);
            
            EnumSet<EProfilerStatisticsSectionId> dataAreaScopes = EnumSet.of(EProfilerStatisticsSectionId.E_SECTION_OUTSIDE_TIME,
                                                                                EProfilerStatisticsSectionId.E_SECTION_NET_TIME,
                                                                                EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME);
            final String profilerCodePath = SectionNames.PROFILER_NODE_PATH 
                    + HeaderPath.SEPARATOR + SectionNames.PROF_CODE_AREA 
                    + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK
                    + HeaderPath.SEPARATOR;
            
            final String profilerDataPath = SectionNames.PROFILER_NODE_PATH 
                    + HeaderPath.SEPARATOR + SectionNames.PROF_DATA_AREA 
                    + HeaderPath.SEPARATOR + HeaderPath.LIST_MARK
                    + HeaderPath.SEPARATOR;

            for (EProfilerStatisticsSectionId scope : codeAreaScopes) {
                String scopePath = profilerCodePath + sn.getProfStatScopeSName(scope)
                                   + HeaderPath.SEPARATOR;  
                for (EProfilerTimeSectionId pTime : profilerTimes) {

                    String path = scopePath + sn.getProfStatTimeSName(pTime) 
                                  + HeaderPath.SEPARATOR + HeaderPath.SEQ_MARK;
                    m_resultProviders.put(path, new ProfilerResultProvider(EAreaType.CODE_AREA, 
                                                                           scope, 
                                                                           pTime));
                }
            }

            for (EProfilerStatisticsSectionId scope : dataAreaScopes) {
                
                String scopePath = profilerDataPath + sn.getProfStatScopeSName(scope)
                                   + HeaderPath.SEPARATOR;
                
                for (EProfilerTimeSectionId pTime : profilerTimes) {
                    
                    String path = scopePath + sn.getProfStatTimeSName(pTime)
                                  + HeaderPath.SEPARATOR + HeaderPath.SEQ_MARK;
                    m_resultProviders.put(path, new ProfilerResultProvider(EAreaType.DATA_AREA, 
                                                                           scope, 
                                                                           pTime));
                }
            }

            // Scripts
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_INIT_TARGET)
                                  + HeaderPath.SEPARATOR + SectionNames.FUNC_NODE_NAME,
                                  new ScriptResultProvidder(CTestResultBase.getSE_INIT_TARGET()));
            
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_INITFUNC)
                                  + HeaderPath.SEPARATOR + SectionNames.FUNC_NODE_NAME,
                                  new ScriptResultProvidder(CTestResultBase.getSE_INIT_FUNC()));
            
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_ENDFUNC)
                                  + HeaderPath.SEPARATOR + SectionNames.FUNC_NODE_NAME,
                                  new ScriptResultProvidder(CTestResultBase.getSE_END_FUNC()));
            
            m_resultProviders.put(tsPath + HeaderPath.SEPARATOR
                                  + sn.getTestSpecSectionName(SectionIds.E_SECTION_RESTORE_TARGET)
                                  + HeaderPath.SEPARATOR + SectionNames.FUNC_NODE_NAME,
                                  new ScriptResultProvidder(CTestResultBase.getSE_RESTORE_TARGET()));
            
            // Group scripts
//            m_resultProviders.put(grpPath + HeaderPath.SEPARATOR
//                                  + sn.getGroupSectionName(ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT),
//                                  new ScriptResultProvidder(CTestResultBase.getSE_GROUP_INIT_FUNC()));
//            
//            m_resultProviders.put(grpPath + HeaderPath.SEPARATOR
//                                  + sn.getGroupSectionName(ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT),
//                                  new ScriptResultProvidder(CTestResultBase.getSE_GROUP_END_FUNC()));
        }
    }
    
    
    public void setInput(TestSpecificationModel model, TestBaseListModel tblModel) {
        m_model = model;
        m_tblModel = tblModel;
    }
    
    
    @Override
    public EResultStatus getCellResult(int dataCol, int dataRow, StringBuilder sb) {
        
        if (m_model == null  ||  m_tblModel == null) {
            return EResultStatus.NO_RESULT;
        }

        HeaderPath path = m_tblModel.getHeaderPath(dataCol);
        if (path != null) {
            
            ICellResultProvider cellResultProvider = m_resultProviders.get(path.getAbstractPath());
            
            if (cellResultProvider != null) {
                
                CTestTreeNode treeNode = CTestTreeNode.cast(m_tblModel.getTestBase(dataRow));
                CTestResult result = m_model.getResult(treeNode);
                
                if (result == null) {
                    return EResultStatus.NO_RESULT;
                }

                return cellResultProvider.getCellResult(result, path.getListIndices(), path.getSeqIndex(), sb);
            }
            
            // System.out.println("getCellResult(), path = (" + dataCol + ", " + dataRow +"): "+ path);
        }
        
        return EResultStatus.NO_RESULT;
    }

    
    @Override
    public EResultStatus getColumnResult(int dataCol, StringBuilder sb) {
        return EResultStatus.NO_RESULT;
    }

    
    @Override
    public EResultStatus getRowResult(int dataRow, StringBuilder sb) {
        
        if (m_model == null  ||  m_tblModel == null) {
            return EResultStatus.NO_RESULT;
        }
        
        CTestTreeNode treeNode = CTestTreeNode.cast(m_tblModel.getTestBase(dataRow));
        CTestResult result = m_model.getResult(treeNode);
        
        if (result == null) {
            return EResultStatus.NO_RESULT;
        }

        sb.delete(0, sb.length());

        if (result.isError()) {
            sb.append(result.toUIString());
            return EResultStatus.ERROR;
        } else {
            return EResultStatus.OK;
        }
    }

    
    @Override
    public EResultStatus getTableResult(StringBuilder sb) {
        return EResultStatus.NO_RESULT;
    }
    
    
//    private CTestSpecification getTestSpecForRow(int dataRow) {
//        return CTestSpecification.cast(m_arrayModel.getTestBase(dataRow));
//    }
//    
    
    class ExpressionsResultProvider implements ICellResultProvider {

        CTestBaseList getResultList(CTestResult result) {
            return result.getAssertResults(true);
        }
        
        @Override
        public EResultStatus getCellResult(CTestResult result, 
                                           int [] listIndices,
                                           int itemIndex, 
                                           StringBuilder sb) {
            
            CTestBaseList assertResults = getResultList(result);

            // test case may have less expressions than there are columns
            // in the table, because some other test case may have more expressions,
            // or test fails before evaluation of expressions,
            // (for example exception, preconditions) in which case there are also 
            // no results).
            if (itemIndex >= assertResults.size()) {
                return EResultStatus.NO_RESULT;
            }
            
            CTestExprResult exprRes = CTestExprResult.cast(assertResults.get(itemIndex));
            sb.append(exprRes.toUIString());
            if (exprRes.isError()) {
                return EResultStatus.ERROR;
            } else {
                return EResultStatus.OK;
            }
        }
    }
    
    
    class PreconditionsResultProvider extends ExpressionsResultProvider {

        @Override
        CTestBaseList getResultList(CTestResult result) {
            return result.getPreConditionResults(true);
        }
    }
    
    
    class StackUsageResultProvider implements ICellResultProvider {

        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int [] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {
            CStackUsageResult stackResult = result.getStackUsageResult(true);
            if (!stackResult.isEmpty()) {

                sb.append(stackResult.toString());
                return stackResult.isError() ? EResultStatus.ERROR : EResultStatus.OK;
            }
                
            return EResultStatus.NO_RESULT;
        }
        
    }
    
    
    class TargetExceptionResultProvider implements ICellResultProvider {

        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int [] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {
            
            return result.isTargetExceptionError() ? EResultStatus.ERROR : 
                                                     EResultStatus.NO_RESULT;
        }
    }
    
    
    class StubTestPoint_ScriptResultProvider implements ICellResultProvider {

        protected ETestResultSection m_section;

        /**
         * @param section should be E_SECTION_STUB_RESULTS or E_SECTION_TEST_POINT_RESULTS
         */
        StubTestPoint_ScriptResultProvider(ETestResultSection section) {
            m_section = section;
        }
        
        
        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int [] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {
            
            if (listIndices.length != 1) {
                return EResultStatus.NO_RESULT;
            }
            
            int itemIdx = listIndices[0];
            
            MutableBoolean isScriptFunc = new MutableBoolean();
            String stubFuncOrTpId = getStubFuncOrTpId(result, itemIdx, isScriptFunc);
            
            if (stubFuncOrTpId == null  ||  isScriptFunc.isFalse()) {
                return EResultStatus.NO_RESULT;
            }
            
            MutableObject<EStatusType> status = new MutableObject<>();
            sb.append(ListEditorBase.extractStubTPScriptResult(result, 
                                                               stubFuncOrTpId, 
                                                               m_section, 
                                                               status));
            
            return status.getValue() == EStatusType.ERROR ? EResultStatus.ERROR : EResultStatus.OK;
        }


        protected String getStubFuncOrTpId(CTestResult result, int itemIdx, MutableBoolean isScriptFunc) {
            
            String stubFuncOrTpId = null; // indicates no script is called
            
            if (m_section == ETestResultSection.E_SECTION_STUB_RESULTS) {
                CTestBaseList stubs = result.getTestSpecification().getStubs(true);
                // this test case may have less than max number of stubs defined
                if (itemIdx < stubs.size()) {
                    CTestStub stub = CTestStub.cast(stubs.get(itemIdx));
                    stubFuncOrTpId = stub.getFunctionName();
                    isScriptFunc.setValue(!stub.getScriptFunctionName().isEmpty());
                }
            } else {
                CTestBaseList testPoints = result.getTestSpecification().getTestPoints(true);
                // this test case may have less than max number of tp-s defined
                if (itemIdx < testPoints.size()) {
                    CTestPoint tp = CTestPoint.cast(testPoints.get(itemIdx));
                    stubFuncOrTpId = tp.getId();
                    isScriptFunc.setValue(!tp.getScriptFunctionName().isEmpty());
                }
            }
            
            return stubFuncOrTpId;
        }
        
    }
    
    
    class StubTestPoint_StepResultProvider extends StubTestPoint_ScriptResultProvider {

        StubTestPoint_StepResultProvider(ETestResultSection section) {
            super(section);
        }
        

        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int[] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {
            
            if (listIndices.length != 2) {  // stub index and step index
                return EResultStatus.NO_RESULT;
            }
            
            int stubOrTpIdx = listIndices[0];
            int stepIdx = listIndices[1];
            
            TestPointResultProvider tpResProv = new TestPointResultProvider(m_section);
            MutableBoolean isScriptFunc = new MutableBoolean();
            String stubFuncOrTpId = getStubFuncOrTpId(result, stubOrTpIdx, isScriptFunc);
            
            if (stubFuncOrTpId == null) {
                return EResultStatus.NO_RESULT;
            }

            CTestBaseList steps = null;
            if (m_section == ETestResultSection.E_SECTION_STUB_RESULTS) {
                CTestBaseList stubs = result.getTestSpecification().getStubs(true);
                if (stubOrTpIdx < stubs.size()) {
                    CTestStub stub = CTestStub.cast(stubs.get(stubOrTpIdx));
                    steps = stub.getAssignmentSteps(true);
                }
            } else {
                CTestBaseList tps = result.getTestSpecification().getTestPoints(true);
                if (stubOrTpIdx < tps.size()) {
                    CTestPoint tp = CTestPoint.cast(tps.get(stubOrTpIdx));
                    steps = tp.getSteps(true);
                }
            }
            
            if (steps == null) {
                return EResultStatus.NO_RESULT;
            }

            tpResProv.setInput(result, stubFuncOrTpId, steps);

            return tpResProv.getCellResult(itemIndex, stepIdx, sb);
        }
    }
    
    
    class Coverage_Code_ResultProvider implements ICellResultProvider {

        protected float m_executed;
        protected int m_all;
        protected boolean m_isError;
        protected ECoverageStatSectionId m_statSectionId;

        Coverage_Code_ResultProvider() {
            m_statSectionId = ECoverageStatSectionId.E_SECTION_BYTES_EXECUTED;
        }
        
        protected void getResultForStatItem(CTestCoverageResult cvrgResult) {
            
            CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
            
            m_executed = measuredCvrgStat.getBytesExecuted(); 
            m_all = cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_BYTES_ALL);
            m_isError = cvrgResult.isBytesExecutedError();
        }
        
        
        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int[] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {

            if (listIndices.length != 1) {  // stat index
                return EResultStatus.NO_RESULT;
            }
            
            int statIdx = listIndices[0];
            CTestBaseList statList = result.getTestSpecification().getAnalyzer(true)
                                           .getCoverage(true).getStatisticsList(true);
            
            if (statIdx >= statList.size()) {
                return EResultStatus.NO_RESULT;
            }
            
            CTestCoverageStatistics cvrgStat = CTestCoverageStatistics.cast(statList.get(statIdx));
            String functionName = cvrgStat.getFunctionName();
            
            String limit = cvrgStat.getTagValue(m_statSectionId.swigValue());
            if (limit.isEmpty()) { // no setting of item by user means no result
                return EResultStatus.NO_RESULT;
            }
            
            StrCoverageTestResultsMap coverageResults = new StrCoverageTestResultsMap();
            result.getCoverageResults(coverageResults);
            
            if (coverageResults.containsKey(functionName)) {
                CTestCoverageResult cvrgResult = coverageResults.get(functionName);
                if (cvrgResult != null) {
                    
                    getResultForStatItem(cvrgResult);
                    String[] res = CvrgStatControls.ExpectedMeasuredStatUITuple
                                            .getResultAsStr(m_executed, m_all);
                    
                    sb.append(res[0]).append(" : ").append(res[1]);

                    return m_isError ? EResultStatus.ERROR : EResultStatus.OK;
                }
            }
            
            return EResultStatus.NO_RESULT;
        }
        
    }
    
    
    class Coverage_Lines_ResultProvider extends Coverage_Code_ResultProvider {
        
        Coverage_Lines_ResultProvider() {
            m_statSectionId = ECoverageStatSectionId.E_SECTION_SOURCE_LINES_EXECUTED;
        }
        
        
        @Override
        protected void getResultForStatItem(CTestCoverageResult cvrgResult) {
            
            CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
            
            m_executed = measuredCvrgStat.getSourceLinesExecuted();
            m_all = cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_LINES_ALL);
            m_isError = cvrgResult.isSourceLinesExecutedError();
        }
    }


    class Coverage_BranchesAll_ResultProvider extends Coverage_Code_ResultProvider {

        Coverage_BranchesAll_ResultProvider() {
            m_statSectionId = ECoverageStatSectionId.E_SECTION_BRANCH_EXECUTED;
        }
        
        @Override
        protected void getResultForStatItem(CTestCoverageResult cvrgResult) {
            
            CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
            
            m_executed = measuredCvrgStat.getBranchExecuted();
            m_all = cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL);
            m_isError = cvrgResult.isBranchesExecutedError();
        }
    }

    
    class Coverage_BranchesTaken_ResultProvider extends Coverage_Code_ResultProvider {

        Coverage_BranchesTaken_ResultProvider () {
            m_statSectionId = ECoverageStatSectionId.E_SECTION_BRANCH_TAKEN;
        }
        
        @Override
        protected void getResultForStatItem(CTestCoverageResult cvrgResult) {
            
            CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
            
            m_executed = measuredCvrgStat.getBranchTaken();
            m_all = cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL);
            m_isError = cvrgResult.isBranchesTakenError();
        }
    }


    class Coverage_BranchesNotTaken_ResultProvider extends Coverage_Code_ResultProvider {

        Coverage_BranchesNotTaken_ResultProvider() {
            m_statSectionId = ECoverageStatSectionId.E_SECTION_BRANCH_NOT_TAKEN;
        }
        
        @Override
        protected void getResultForStatItem(CTestCoverageResult cvrgResult) {
            
            CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
            
            m_executed = measuredCvrgStat.getBranchNotTaken();
            m_all = cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL);
            m_isError = cvrgResult.isBranchesNotTakenError();
        }
    }


    class Coverage_BranchesBoth_ResultProvider extends Coverage_Code_ResultProvider {

        Coverage_BranchesBoth_ResultProvider() {
            m_statSectionId = ECoverageStatSectionId.E_SECTION_BRANCH_BOTH;
        }
        
        @Override
        protected void getResultForStatItem(CTestCoverageResult cvrgResult) {
            
            CTestCoverageStatistics measuredCvrgStat = cvrgResult.getMeasuredCoverage(true);
            
            m_executed = measuredCvrgStat.getBranchBoth();
            m_all = cvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_CONDITIONS_ALL);
            m_isError = cvrgResult.isBranchesBothError();
        }
    }
    
    
    class ProfilerResultProvider implements ICellResultProvider {

        protected EAreaType m_areaType;
        protected EProfilerStatisticsSectionId m_scope;
        protected EProfilerTimeSectionId m_timeType;
        
        
        public ProfilerResultProvider(EAreaType areaType,
                                      EProfilerStatisticsSectionId scope,
                                      EProfilerTimeSectionId timeType) {
            m_areaType = areaType;
            m_scope = scope;
            m_timeType = timeType;
        }


        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int[] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {

            if (listIndices.length != 1) {  
                return EResultStatus.NO_RESULT;
            }
            
            CTestSpecification testSpec = result.getTestSpecification();
            CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
            
            if (analyzer.getRunMode() != ERunMode.M_START) {
                return EResultStatus.NO_RESULT;
            }
            
            CTestAnalyzerProfiler prof = analyzer.getProfiler(true);
            CTestProfilerStatistics area = prof.getArea(m_areaType, listIndices[0]);
            
            String areaName = area.getQualifiedAreaName(result.getDefaultDownloadFile());
            CTestProfilerTime timeLimits = area.getTime(m_scope, true);
            String limitStr = timeLimits.getTime(m_timeType, itemIndex);
            
            if (limitStr.isEmpty()) {
                return EResultStatus.NO_RESULT; // no limit was set by user
            }

            CProfilerTestResult profResult = result.getProfilerCodeResult(areaName);
            
            if (profResult != null) {
                
                ProfilerErrCode errCode = profResult.validateError(m_scope, m_timeType);

                sb.append("Measured: ").append(profResult.getMeasuredTime(m_scope, m_timeType))
                  .append(" ns");
                
                return errCode == ProfilerErrCode.ERR_NONE ? 
                                    EResultStatus.OK : EResultStatus.ERROR;
            }

            return EResultStatus.NO_RESULT;
        }
    }
    
    
    class ScriptResultProvidder implements ICellResultProvider {
        
        private String m_scriptFuncType;
        
        public ScriptResultProvidder(String scriptFuncType) {
            m_scriptFuncType = scriptFuncType;
        }


        @Override
        public EResultStatus getCellResult(CTestResult result,
                                           int[] listIndices,
                                           int itemIndex,
                                           StringBuilder sb) {
           
            String scriptOutput = result.getScriptOutput(m_scriptFuncType);
            
            if (result.isScriptError(m_scriptFuncType)) {
                sb.append(scriptOutput);
                sb.append("\n-----------\n");
                sb.append(result.getScriptError(m_scriptFuncType));
                
                return EResultStatus.ERROR;
            } else {
                sb.append(scriptOutput);
                if (scriptOutput.isEmpty()) {
                    // No decoration means there was no error, but also no script  
                    // output (reserved var was not set).
                    return EResultStatus.NO_RESULT; 
                } else {
                    return EResultStatus.OK;
                }
            }
        }
    }
}


