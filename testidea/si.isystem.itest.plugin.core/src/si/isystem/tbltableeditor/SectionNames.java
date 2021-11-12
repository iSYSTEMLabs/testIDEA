package si.isystem.tbltableeditor;

import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.EAnalyzerSectionId;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestProfilerTime;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStackUsage;
import si.isystem.connect.CTestStub;

/**
 * This class provides names of sections ot test specification classes.
 * 
 * @author markok
 *
 */
public class SectionNames {

    public static SectionNames INSTANCE = new SectionNames();
    
    public static final CTestSpecification TEST_SPEC = new CTestSpecification();
    private static final CTestGroup TEST_GROUP = new CTestGroup(); 
    private static final CTestFunction TEST_FUNC = new CTestFunction(); 
    private static final CTestAnalyzer TEST_ANALYZER = new CTestAnalyzer(null);
    private static final CTestAnalyzerCoverage TEST_COVERAGE = new CTestAnalyzerCoverage();
    private static final CTestAnalyzerProfiler TEST_PROFILER = new CTestAnalyzerProfiler();
    private static final CTestAssert TEST_ASSERT = new CTestAssert();
    private static final CTestStub TEST_STUB = new CTestStub();
    private static final CTestPoint TEST_POINT = new CTestPoint();
    private static final CTestEvalAssignStep TEST_ASSIGN_STEP = new CTestEvalAssignStep();
    private static final CTestStackUsage TEST_STACK_USAGE = new CTestStackUsage();
    private static final CTestCoverageStatistics TEST_CVRG_STAT = new CTestCoverageStatistics();
    private static final CTestProfilerStatistics TEST_PROF_STAT = new CTestProfilerStatistics();
    private static final CTestProfilerTime TEST_PROF_TIME = new CTestProfilerTime();
    
    public static final String TEST_SPEC_NODE_PATH = HeaderPath.SEPARATOR + TEST_SPEC.getClassName();
    public static final String TEST_GROUP_NODE_PATH = HeaderPath.SEPARATOR + TEST_GROUP.getClassName();

    public static final String FUNC_NODE_NAME = 
            TEST_FUNC.getTagName(CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue());
    
    public final static String ANALYZER_NODE_PATH = 
                 TEST_SPEC_NODE_PATH + HeaderPath.SEPARATOR + 
                 TEST_SPEC.getTagName(SectionIds.E_SECTION_ANALYZER.swigValue());
    
    public final static String COVERAGE_NODE_PATH =
            ANALYZER_NODE_PATH + HeaderPath.SEPARATOR +
            SectionNames.TEST_ANALYZER.getTagName(EAnalyzerSectionId.E_SECTION_COVERAGE.swigValue());
    
    public final static String PROFILER_NODE_PATH =
            ANALYZER_NODE_PATH + HeaderPath.SEPARATOR +
            SectionNames.TEST_ANALYZER.getTagName(EAnalyzerSectionId.E_SECTION_PROFILER.swigValue());
    
    public final static String CVRG_STATS_NODE_NAME = 
            TEST_COVERAGE.getTagName(CTestAnalyzerCoverage.ECoverageSectionId.E_SECTION_STATISTICS.swigValue());
    
    public final static String TRACE_NODE_NAME = 
            TEST_ANALYZER.getTagName(EAnalyzerSectionId.E_SECTION_TRACE.swigValue());
    
    public final static String PROFILER_NODE_NAME = 
            TEST_ANALYZER.getTagName(EAnalyzerSectionId.E_SECTION_PROFILER.swigValue());
    
    public final static String COVERAGE_NODE_NAME = 
            TEST_ANALYZER.getTagName(EAnalyzerSectionId.E_SECTION_COVERAGE.swigValue());

    public static final String EXPR_NODE_NAME = 
            TEST_ASSERT.getTagName(CTestAssert.ESectionAssert.E_SECTION_ASSERT_EXPRESSIONS.swigValue());
    
    public static final String TARGET_EXCEPTION_NODE_NAME = 
            TEST_ASSERT.getTagName(CTestAssert.ESectionAssert.E_SECTION_ASSERT_IS_EXPECT_EXCEPTION.swigValue());
    
    public static final String STACK_USAGE_MAX_LIMIT_NODE_NAME =
            TEST_STACK_USAGE.getTagName(CTestStackUsage.ETestStackUsageSections.E_SECTION_MAX_SIZE.swigValue());

    public static final String SCRIPT_FUNC = TEST_STUB.getTagName(CTestStub.EStubSectionIds.E_SECTION_SCRIPT_FUNCTION.swigValue());

    public static final String STUB_ASSIGN_STEPS = TEST_STUB.getTagName(CTestStub.EStubSectionIds.E_SECTION_ASSIGN_STEPS.swigValue());
    
    public static final String TEST_POINT_STEPS = TEST_POINT.getTagName(CTestPoint.ETestPointSections.E_SECTION_STEPS.swigValue());

    public static final String EXPECT = TEST_ASSIGN_STEP.getTagName(CTestEvalAssignStep.EStepSectionIds.E_SECTION_EXPECT.swigValue());

    public static final String CVRG_STATISTICS = TEST_COVERAGE.getTagName(CTestAnalyzerCoverage.ECoverageSectionId.E_SECTION_STATISTICS.swigValue());
    
    public static final String PROF_CODE_AREA = 
            TEST_PROFILER.getTagName(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_CODE_AREAS.swigValue());
    
    public static final String PROF_DATA_AREA = 
            TEST_PROFILER.getTagName(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_DATA_AREAS.swigValue());

    public String getTestSpecSectionName(SectionIds sectionId) {
        return TEST_SPEC.getTagName(sectionId.swigValue());
    }
    
    
    public String getCvrgStatSName(CTestCoverageStatistics.ECoverageStatSectionId sectionId) {
        return TEST_CVRG_STAT.getTagName(sectionId.swigValue());        
    }

    public String getProfStatScopeSName(CTestProfilerStatistics.EProfilerStatisticsSectionId sectionId) {
        return TEST_PROF_STAT.getTagName(sectionId.swigValue());        
    }

    public String getProfStatTimeSName(CTestProfilerTime.EProfilerTimeSectionId sectionId) {
        return TEST_PROF_TIME.getTagName(sectionId.swigValue());        
    }

}
