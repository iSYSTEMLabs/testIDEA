package si.isystem.itest.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageResult;
import si.isystem.connect.CTestCoverageResult.ESectionCoverageResult;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrCoverageTestResultsMap;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.TestTreeOutline;

public class ToolsTestVectorOptimizerHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();
        
        // get derived tests and results
        List<CTestSpecification> testSpecs = UiUtils.getSelectedTestTreeSpecifications();
        
        if (testSpecs.isEmpty()) {
            MessageDialog.openWarning(shell, 
                "Nothing to optimize!", 
                "Please, select iSYSTEM test case editor "
                + "and at least one test specification with derived test cases in Outline view.");
            return null;            
        }
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model == null) {
            MessageDialog.openWarning(shell, 
                "Model not found!", 
                "Please, select iSYSTEM test case editor "
                + "and at least one test specification with derived test cases in Outline view.");
            return null;            
        }
        
        List<CTestSpecification> redundantTestSpecs = new ArrayList<>();
        
        for (CTestSpecification testSpec : testSpecs) {
            try {
                filterTestSpecs(model, null, testSpec, redundantTestSpecs);
            } catch (Exception ex) {
                SExceptionDialog.open(shell, "Can not optimize set of test cases!", ex);
            }
        }
        
        // select tcs from the list
        if (!redundantTestSpecs.isEmpty()) {
            TestTreeOutline outline = TestCaseEditorPart.getOutline();
            StructuredSelection selection = new StructuredSelection(redundantTestSpecs);
            outline.setSelection(selection);        
        } else {
            MessageDialog.openInformation(Activator.getShell(), 
                                          "Nothing to optimize", 
                                          "No test cases with coverage equal to previous test case were found.");
        }
        
        return null;
    }
    
    
    private CTestResult filterTestSpecs(TestSpecificationModel model, 
                                 CTestResult prevResult, 
                                 CTestSpecification testSpec, 
                                 List<CTestSpecification> redundantTestSpecs) {
        
        if (testSpec.getRunFlag() != ETristate.E_FALSE) {

            CTestResult result = model.getResult(testSpec);
            if (result == null) {
                throw new SIllegalArgumentException("Can not perform optimization, "
                        + "if some test cases have no test results. Please execute "
                        + "all selected and their derived tests.").
                        add("testCaseWithoutResult", testSpec.getUILabel());
            }

            if (prevResult != null) {
                // if all items are the same - add to selection
                if (coverageEquals(testSpec, prevResult, result)) {
                    redundantTestSpecs.add(testSpec);
                }
            }
            
            prevResult = result;
        }
        
        int numDerived = testSpec.getNoOfDerivedSpecs();
        for (int idx = 0; idx < numDerived; idx++) {
            prevResult = filterTestSpecs(model, 
                                         prevResult, 
                                         testSpec.getDerivedTestSpec(idx),
                                         redundantTestSpecs);
        }
        
        return prevResult;
    }


    private boolean coverageEquals(CTestSpecification testSpec,
                                   CTestResult prevResult,
                                   CTestResult currResult) {
        
        StrCoverageTestResultsMap prevResultMap = new StrCoverageTestResultsMap();
        prevResult.getCoverageResults(prevResultMap);
        
        StrCoverageTestResultsMap currResultMap = new StrCoverageTestResultsMap();
        currResult.getCoverageResults(currResultMap);

        CTestAnalyzerCoverage cvrg = testSpec.getCoverage(true);
        CTestBaseList statList = cvrg.getStatisticsList(true);
        if (statList.isEmpty()) {
            throw new SIllegalStateException("No coverage defined!"
                    + " Please make sure that all test cases have coverage section defined!").
                    add("testCaseWithoutCoverage", testSpec.getUILabel());
        }
        
        for (int idx = 0; idx < statList.size(); idx++) {
            
            CTestCoverageStatistics expectedStat = CTestCoverageStatistics.cast(statList.get(idx));
            String funcName = expectedStat.getFunctionName();
            
            if (!prevResultMap.containsKey(funcName)) {
                throw new SIllegalArgumentException("Coverage measurements for the "
                        + "given function were not found in previous test case!\n"
                        + "Please make sure that all test cases measure coverage of all functions!").
                add("functionName", funcName);
            }
            
            CTestCoverageResult prevCvrgResult = prevResultMap.get(funcName);
            CTestCoverageResult currCvrgResult = currResultMap.get(funcName);

            CTestCoverageStatistics prevMeasuredStat = prevCvrgResult.getMeasuredCoverage(true);
            CTestCoverageStatistics currMeasuredStat = currCvrgResult.getMeasuredCoverage(true);

            if (expectedStat.getBytesExecuted() + expectedStat.getSourceLinesExecuted() + 
                    expectedStat.getBranchExecuted() + expectedStat.getBranchTaken() + 
                    expectedStat.getBranchNotTaken() + expectedStat.getBranchBoth() == 0) {
                throw new SIllegalStateException("No coverage defined!"
                            + " Please make sure that all test cases have at least one item coverage statistics section defined!").
                            add("testCaseWithoutCoverage", testSpec.getUILabel());
            }

            int prevBytesAll = prevCvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_BYTES_ALL);
            int currBytesAll = currCvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_BYTES_ALL);
            if (prevBytesAll != currBytesAll) {
                throw new SIllegalStateException("The number of bytes in areas is not the same!"
                        + " Please make sure that all test cases were executed on the same code!").
                        add("numBytesInPrevResult", prevBytesAll).
                        add("numBytesInCurrResult", currBytesAll).
                        add("testCase", testSpec.getUILabel());
            }

            
            int prevLinesAll = prevCvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_LINES_ALL);
            int currLinesAll = currCvrgResult.getMeasured(ESectionCoverageResult.E_SECTION_LINES_ALL);
            if (prevLinesAll != currLinesAll) {
                throw new SIllegalStateException("The number of lines in areas is not the same!"
                        + " Please make sure that all test cases were executed on the same code!").
                        add("numLinesInPrevResult", prevLinesAll).
                        add("numLinesInCurrResult", currLinesAll).
                        add("testCase", testSpec.getUILabel());
            }

            // equalsData() call does not work here, because it compares also items not set in expected stat. 
            
            if (expectedStat.getBytesExecuted() > 0) {
                if (!statsItemEquals(testSpec, "bytesExec",
                                     prevMeasuredStat.getBytesExecuted(),
                                     currMeasuredStat.getBytesExecuted())) {
                    return false;
                }
            }
            
            if (expectedStat.getSourceLinesExecuted() > 0) {
                if (!statsItemEquals(testSpec, "sourceLines",
                                     prevMeasuredStat.getSourceLinesExecuted(),
                                     currMeasuredStat.getSourceLinesExecuted())) {
                    return false;
                }
            }
            
            if (expectedStat.getBranchExecuted() > 0) {
                if (!statsItemEquals(testSpec, "condExec",
                                     prevMeasuredStat.getBranchExecuted(),
                                     currMeasuredStat.getBranchExecuted())) {
                    return false;
                }
            }
            
            if (expectedStat.getBranchNotTaken() > 0) {
                if (!statsItemEquals(testSpec, "condFalse",
                                     prevMeasuredStat.getBranchNotTaken(),
                                     currMeasuredStat.getBranchNotTaken())) {
                    return false;
                }
            }
            
            if (expectedStat.getBranchTaken() > 0) {
                if (!statsItemEquals(testSpec, "condTrue",
                                     prevMeasuredStat.getBranchTaken(),
                                     currMeasuredStat.getBranchTaken())) {
                    return false;
                }
            }
            
            if (expectedStat.getBranchBoth() > 0) {
                if (!statsItemEquals(testSpec, "condBoth",
                                     prevMeasuredStat.getBranchBoth(),
                                     currMeasuredStat.getBranchBoth())) {
                    return false;
                }
            }
        }
        
        return true;
    }


    private boolean statsItemEquals(CTestSpecification testSpec,
                                    String itemName,
                                    float previous, float current) {
        if ((current - previous) < -1e-5) {
            throw new SIllegalStateException("Coverage of previous test case is greater "
                    + "than the coverage of this test case. Please make "
                    + "sure that test cases were run with analyzer open mode set to Append."). 
            add(itemName + "CvrgPrev", previous).
            add(itemName + "CvrgCurrent", current).
            add("testCase", testSpec.getUILabel());
        }
        
        if ((current - previous) > 1e-5) {
            return false;
        }
        
        return true;
    }
}
