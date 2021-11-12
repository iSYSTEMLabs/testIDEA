package si.isystem.itest.common;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestPersistentVars.EPersistVarsSections;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.AsystContentProposalProvider;

/**
 * This class provides completion proposals for persistent variables. It searches
 * hierarchy from the given test case to the root test cases and adds/removes
 * persistent variables according to test case setting. Because of response
 * limitations max number of searched test cases is limited by the number of test cases
 * to search (approximate estimation without derived tests) and also the time 
 * allowed for search (users may have slow machines or the number of derived tests is 
 * large).
 * 
 * @author markok
 *
 */
public class PersistVarsProposalsProvider {

    private static final long MAX_SEARCH_TIME = 200000000; // in nanoseconds, 200 ms
    private static final int MONITOR_UPDATE_STEP = 5; // percentage of test cases worked when monitor is updated
    
    private boolean m_isCheckAllUnconditionally;
    private int m_numTestSpecs;
    private long m_startTime;
    private int m_numImmediatePredecessors;
    private int m_monitorUpdateStep;
    private int m_numWorkedImmediatePredecessors;
    private boolean m_isSearchTerminated;
    private static boolean m_isAlreadyShown = false;
    
    
    /**
     * After call to getPersistVarsProposals() this method returns true, if 
     * proposals are complete, false otherwise. Use this method to indicate 
     * incomplete proposals to user.
     * @return
     */
    public boolean isSearchTerminated() {
        return m_isSearchTerminated;
    }


    public void openWarningDialog(String message) {
        if (isSearchTerminated()  &&  !m_isAlreadyShown) {
            MessageDialog.openInformation(Activator.getShell(), "Proposals not complete", 
                                          message + 
                                          "\n\nThis dialog will not appear again during this session.");
            m_isAlreadyShown = true;
        }
    }
    
    
    /**
     * This method returns exact list of persistent variables, which exist and
     * are available for the given test case, usually the one that is selected
     * in testIDEA. This way users can see, if there are any variables to be
     * deleted. However, since the number of test cases may be really big, the
     * search time is limited if isCheckAllUnconditionally == false. In such
     * case the user has to trigger searching explicitly.
     * 
     * Note: This method searches all test cases executed before the selected 
     * test case, and adds/removes persist. variables according to test case contents.    
     * 
     * @param testSpec
     * @param contentProposals
     * @param isCheckAllUnconditionally if true, complete hierarchy is searched
     *                                  regardless of number of test cases or time spent.
     *                                  This way users can still get exact list of 
     *                                  proposals if they are ready to wait few seconds. 
     * 
     * @throws InterruptedException 
     * @throws InvocationTargetException 
     */
    public void getExactPersistVarsProposals(final CTestSpecification testSpec, 
                                             final AsystContentProposalProvider contentProposals,
                                             boolean isCheckAllUnconditionally) {

        m_startTime = System.nanoTime();
        m_isCheckAllUnconditionally = isCheckAllUnconditionally;
        m_numImmediatePredecessors = -1; // not initialized yet
        
        try {
            PlatformUI.getWorkbench().getProgressService()
            .busyCursorWhile(new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {

                    monitor.beginTask("Running tests", 100);

                    // variables from the current decl tab are added for consistency,
                    // even if it does not make 
                    // much sense to delete variable in the same test as it is created - test
                    // local variables should be used for such purpose!
                    Map<String, String> persistVarsExistingAtCurrentTestSpec = new TreeMap<>();
                    CTestSpecification parentTS = testSpec.getParentTestSpecification();
                    if (parentTS != null) {  // should never be null, as m_testSpec is never root test spec
                        int idx = parentTS.findDerivedTestSpec(testSpec);
                        m_numTestSpecs = idx;
                        getPersistVarsProposals(parentTS, idx, 
                                                persistVarsExistingAtCurrentTestSpec,
                                                monitor);
                    }

                    String[][] proposals = proposalsMapToArrays(persistVarsExistingAtCurrentTestSpec);

                    contentProposals.setProposals(proposals[0], proposals[1]);
                    monitor.done();
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Error searching for peristent variables test case hierarchy!", 
                                  ex);
            ex.printStackTrace();
        }

        // System.out.println("spent time[ms] = " + (System.nanoTime() - m_startTime) / 1_000_000);
    }
    
    
    private void getPersistVarsProposals(CTestSpecification testSpec, 
                                         int idx,
                                         Map<String, String> proposals, 
                                         IProgressMonitor monitor) {
        
        // Measurements (see the above method, commented println statement) show
        // that on one year old machine 10_000 test cases to search take about 800 ms,
        // so even on older machine MAX_TEST_SPECS_TO_PROCESS test cases search time should be
        // below 1s, which is acceptable for user.
        if (isTerminateSearch()) {
            return;
        }
        
        CTestSpecification parentTS = testSpec.getParentTestSpecification();
        if (parentTS != null) {
            int newIdx = parentTS.findDerivedTestSpec(testSpec);
            m_numTestSpecs += newIdx;
            getPersistVarsProposals(parentTS, newIdx, proposals, monitor);
        }
        
        // the number of test cases on direct path to root, without derived
        // test cases of test cases on this path
        if (m_numImmediatePredecessors == -1) {
            m_numImmediatePredecessors = m_numTestSpecs;
            m_monitorUpdateStep = m_numImmediatePredecessors * MONITOR_UPDATE_STEP / 100;
            m_monitorUpdateStep = Math.max(1, m_monitorUpdateStep);
            m_numWorkedImmediatePredecessors = 0;
        }
        
        for (int i = 0; i <= idx; i++) {
            if (isTerminateSearch()) {
                return; // do not process if limit is reached
            }
            CTestSpecification tsAtIdx = testSpec.getDerivedTestSpec(i);
            getProposalsForTestSpec(tsAtIdx, proposals,
                                    // the last test spec in hierarchy should not be 
                                    // searched recursively by getProposalsForTestSpec(), 
                                    // as it will be searched by this method
                                    i < idx);
            
            m_numWorkedImmediatePredecessors++;
            if (m_numWorkedImmediatePredecessors % m_monitorUpdateStep == 0) {
                // System.out.println("=> i = " + i);
                monitor.worked(5);
            }
        }
    }


    private void getProposalsForTestSpec(CTestSpecification testSpec,
                                         Map<String, String> proposals,
                                         boolean isSearchRecursively) {
        
        CTestSpecification mergedTS = null;
        if (testSpec.isSectionMerged(SectionIds.E_SECTION_PERSIST_VARS)) {
            mergedTS = testSpec.merge();
        } else {
            mergedTS = testSpec;
        }

        if (mergedTS.getRunFlag() != ETristate.E_FALSE) {
        
            CTestPersistentVars persistVars = testSpec.getPersistentVars(true);

            if (!persistVars.isDeleteAll()) { 
                copyPeristVarsFromTestSpecToProposalsMap(proposals, persistVars, true);
            } else {
                // no persist vars exist after this point
                proposals.clear();
            }
        }
        
        int numDerived = testSpec.getNoOfDerivedSpecs();
        m_numTestSpecs += numDerived;
        if (isTerminateSearch()) {
            return;
        }
        
        if (isSearchRecursively) {
            for (int idx = 0; idx < numDerived; idx++) {
                CTestSpecification derivedTS = testSpec.getDerivedTestSpec(idx);
                getProposalsForTestSpec(derivedTS, proposals, true);
            }
        }
    }


    private boolean isTerminateSearch() {
        // terminate search if it takes to long, or the number of test 
        // specifications to search indicates it will take too long
        if (m_isSearchTerminated) {
            return true;
        }
        
        m_isSearchTerminated = (System.nanoTime() - m_startTime  >  MAX_SEARCH_TIME)  &&  
                               !m_isCheckAllUnconditionally;
        
        return m_isSearchTerminated;
    }
    

    private void copyPeristVarsFromTestSpecToProposalsMap(Map<String, String> proposals,
                                                          CTestPersistentVars persistVars,
                                                          boolean isRemoveDeletedVars) {
        CMapAdapter declMap = new CMapAdapter(persistVars, 
                                              EPersistVarsSections.E_SECTION_DECL.swigValue(),
                                              true);
        StrVector keys = new StrVector();
        declMap.getKeys(keys);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            proposals.put(key, declMap.getValue(key));
        }

        if (isRemoveDeletedVars) {
            CSequenceAdapter deleteVars = new CSequenceAdapter(persistVars,
                                                               EPersistVarsSections.E_SECTION_DELETE.swigValue(),
                                                               true);
            for (int i = 0; i < deleteVars.size(); i++) {
                proposals.remove(deleteVars.getValue(i));
            }
        }
    }
    
    
    private String[][] proposalsMapToArrays(Map<String, String> persistVarsExistingAtCurrentTestSpec) {
        String[][] proposalsWDesc = new String[2][];
        proposalsWDesc[0] = new String[persistVarsExistingAtCurrentTestSpec.size()];
        proposalsWDesc[1] = new String[persistVarsExistingAtCurrentTestSpec.size()];

        int idx = 0;
        for (Map.Entry<String, String>entry : persistVarsExistingAtCurrentTestSpec.entrySet()) {
            proposalsWDesc[0][idx] = entry.getKey();
            proposalsWDesc[1][idx] = entry.getValue();
            idx++;
        }
        
        return proposalsWDesc;
    }

    
    /**
     * This method returns all persistent vars in model. This list is used for 
     * sections func params, Variables, Expected, and Assignment steps.
     */
    public String[][] getAllPersistVars() {
        Map<String, String> proposals = new TreeMap<>();
        
        CTestSpecification rootTS = 
                GlobalsConfiguration.instance().getActiveModel().getRootTestSpecification();
        
        getAllPersistVars(rootTS, proposals);
        
        return proposalsMapToArrays(proposals);
    }


    private void getAllPersistVars(CTestSpecification testSpec,
                                   Map<String, String> proposals) {

        CTestPersistentVars persistVars = testSpec.getPersistentVars(true);
        copyPeristVarsFromTestSpecToProposalsMap(proposals, persistVars, false);
        
        int numDerived = testSpec.getNoOfDerivedSpecs();
        for (int idx = 0; idx < numDerived; idx++) {
            CTestSpecification derivedTS = testSpec.getDerivedTestSpec(idx);
            getAllPersistVars(derivedTS, proposals);
        }
    }
}
