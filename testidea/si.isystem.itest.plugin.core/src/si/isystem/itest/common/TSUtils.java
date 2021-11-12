package si.isystem.itest.common;

import si.isystem.connect.CLineDescription.EResourceType;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestProfilerStatistics.EProfilerStatisticsSectionId;
import si.isystem.connect.CTestProfilerTime;
import si.isystem.connect.CTestProfilerTime.EProfilerTimeSectionId;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.ETristate;

/**
 * This class contains utility functions for handling test specification
 * and its sections.
 *    
 * @author markok
 */
public class TSUtils {

    private static final String DEFAULT_STUB_RET_VAL_VAR_NAME = "stubRV";

    public static CTestStub createStub(CTestSpecification newTestSpec, 
                                 String funcName, 
                                 String [] params) {
        
        CTestStub stub = new CTestStub(newTestSpec);
        stub.setFunctionName(funcName);
    
        if (params.length > 0  &&  !params[0].isEmpty()) {
            stub.setRetValName(DEFAULT_STUB_RET_VAL_VAR_NAME);
    
            CTestBaseList steps = stub.getAssignmentSteps(false);
            CTestEvalAssignStep step = new CTestEvalAssignStep(stub);
            step.setTagValue(CTestEvalAssignStep.EStepSectionIds.E_SECTION_ASSIGN.swigValue(),
                             DEFAULT_STUB_RET_VAL_VAR_NAME, params[0]);
            steps.add(-1, step);
        }
        return stub;
    }

    public static CTestUserStub createUserStub(CTestSpecification newTestSpec, 
                                         String stubbedFuncName, 
                                         String[] params) {
        CTestUserStub stub = new CTestUserStub(newTestSpec);
        stub.setFunctionName(stubbedFuncName);
        if (params.length > 0  &&  !params[0].isEmpty()) {
            stub.setReplacementFuncName(params[0]);
        }
        return stub;
    }

    public static CTestPoint createTestPoint(CTestSpecification newTestSpec, 
                                             String tpFuncName, 
                                             String [] params) {
        CTestPoint testPoint = new CTestPoint(newTestSpec);
        CTestLocation loc = testPoint.getLocation(false);
        loc.setResourceName(tpFuncName);
        loc.setResourceType(EResourceType.E_RESOURCE_FUNCTION);
        if (params.length > 0  &&  !params[0].isEmpty()) {
            loc.setSearch(ETristate.E_TRUE);
            testPoint.setId(params[0]);
        }
        return testPoint;
    }
    
    
    public static CTestCoverageStatistics createCoverageStats(CTestAnalyzerCoverage coverage,
                                                              String cvrgFuncNAme,
                                                              String[] itemParams) {
        CTestCoverageStatistics stat = new CTestCoverageStatistics(coverage);
        stat.setFunctionName(cvrgFuncNAme);
        
        if (itemParams.length > 0) {
            stat.setCodeExecuted(itemParams[0]);
        }
        if (itemParams.length > 1) {
            stat.setBranchBoth(itemParams[1]);
        }
        return stat;
    }

    
    public static CTestProfilerStatistics createProfilerStats(CTestAnalyzerProfiler profiler,
                                                              String profFuncName,
                                                              String[] itemParams) {
        CTestProfilerStatistics stat = new CTestProfilerStatistics(profiler);
        stat.setAreaName(profFuncName);
        
        CTestProfilerTime grossTime = stat.getTime(EProfilerStatisticsSectionId.E_SECTION_GROSS_TIME,
                                                   false);
        if (itemParams.length > 0) {
            grossTime.setTime(EProfilerTimeSectionId.E_SECTION_TOTAL_TIME, 
                              itemParams[0], 0);
        }
        if (itemParams.length > 1) {
            grossTime.setTime(EProfilerTimeSectionId.E_SECTION_TOTAL_TIME, 
                              itemParams[1], 1);
        }
        return stat;
    }

    
    public static boolean isFilterGroup(CTestTreeNode node) {
        if (node.isGroup()) {
            return !CTestGroup.cast(node).isTestSpecOwner();
        }
        return false;
    }
    
    
    public static CTestSpecification getTestSpec(CTestTreeNode node) {
        
        if (node == null) {
            return null;
        }
        
        if (node.isGroup()) {
            CTestGroup group = CTestGroup.cast(node);
            if (group.isTestSpecOwner()) {
                return group.getOwnedTestSpec();
            }
            return null;
        }
        
        return CTestSpecification.cast(node);
    }
    
    
    /**
     * Needed to get correct object in Java, because inheritance does not work
     * through SWIG - for example, if C++ method returns type CTreeNode, but
     * the actual type is CTestGroup, then SWIG will still create CTreeNode object 
     * in Java, which means RTTI is not possible. Use the info provided by 
     * CTreeNode classes to cast to correct Java type. 
     * 
     * Additionally, if it is a container group, return contained test spec.
     *  
     * @param node
     * @return
     */
    public static CTestTreeNode castToType(CTestTreeNode node) {
        
        if (node == null) {
            return null;
        }
        
        if (node.isGroup()) {
            CTestGroup group = CTestGroup.cast(node);
            if (group.isTestSpecOwner()) {
                return group.getOwnedTestSpec();  // returns CTestSpecification
            }
            return group;
        }
        
        return CTestSpecification.cast(node);
    }    
}
