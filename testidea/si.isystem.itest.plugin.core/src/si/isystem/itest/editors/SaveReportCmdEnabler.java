package si.isystem.itest.editors;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import si.isystem.itest.model.TestSpecificationModel;

public class SaveReportCmdEnabler extends AbstractSourceProvider {

    private static final String RESULTS_AVAILABLE = "ResultsAvailable";
    public final static String VAR_NAME = "si.isystem.itest.isSaveTestReportActive";
    
    public SaveReportCmdEnabler() {
    }


    @Override
    public void dispose() {
    }


    @Override
    public Map<String, String> getCurrentState() {
        
        // System.out.println("getCurrentState() {");
        Map<String, String> states = new TreeMap<>();
        
        TestSpecificationModel model = TestCaseEditorPart.getActiveModel();
        
        if (model != null  &&  model.getTestReportContainer().getNoOfTestResults() > 0) {
            states.put(VAR_NAME, RESULTS_AVAILABLE);
        } else {
            states.put(VAR_NAME, "");
        }
        
        return states;
    }


    @Override
    public String[] getProvidedSourceNames() {
        return new String[]{VAR_NAME};
    }
    
    
    public void sourceChanged(boolean isReportAvailable) {
        fireSourceChanged(ISources.WORKBENCH, 
                          VAR_NAME, 
                          isReportAvailable ? RESULTS_AVAILABLE : "");
    }

}
