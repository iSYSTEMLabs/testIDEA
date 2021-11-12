package si.isystem.itest.ui.spec.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.widgets.Control;

import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.preferences.UIPrefsPage;

public class HostVarsUtils {

    private static Map<String, String> m_descriptions;
    
    public static final char HOST_VARX_PREFIX = '$';
    
    public static final String $_TEST_ID = CTestHostVars.getRESERVED_TEST_ID();
    public static final String $_TAGS = CTestHostVars.getRESERVED_TAGS();
    public static final String $_FUNCTION = CTestHostVars.getRESERVED_FUNCTION();
    public static final String $_PARAMS = CTestHostVars.getRESERVED_PARAMS();
    public static final String $_CORE_ID = CTestHostVars.getRESERVED_CORE_ID();

    public static final String $_DATE = CTestHostVars.getRESERVED_UID();
    public static final String $_TIME = CTestHostVars.getRESERVED_UID();
    public static final String $_UID = CTestHostVars.getRESERVED_UID();
    
    public static final String $_ISO_TIME = CTestHostVars.getRESERVED_ISO_TIME();
    public static final String $_USER_NAME = CTestHostVars.getRESERVED_USER();
    public static final String $_SVN_REVISION = CTestHostVars.getRESERVED_SVN_REVISION();
    
    
    public static final String $_BATCH_DATE = CTestHostVars.getRESERVED_BATCH_DATE();
    public static final String $_BATCH_TIME = CTestHostVars.getRESERVED_BATCH_TIME();
    public static final String $_BATCH_UID = CTestHostVars.getRESERVED_BATCH_UID();

    public static final String $_WINIDEA_WORKSPACE_DIR = CTestHostVars.getRESERVED_WINIDEA_WORKSPACE_DIR();
    public static final String $_WINIDEA_WORKSPACE_FILE = CTestHostVars.getRESERVED_WINIDEA_WORKSPACE_FILE();
    public static final String $_DEFAULT_DOWNLOAD_DIR = CTestHostVars.getRESERVED_DEFAULT_DL_DIR();
    public static final String $_DEFAULT_DOWNLOAD_FILE = CTestHostVars.getRESERVED_DEFAULT_DL_FILE();
    public static final String $_IYAML_DIR = CTestHostVars.getRESERVED_IYAML_DIR();
    public static final String $_REPORT_DIR = CTestHostVars.getRESERVED_REPORT_DIR();
    
    // UI specific host vars (for Auto ID setting)
    public static final String $_NID = CTestHostVars.getRESERVED_NID();
    public static final String $_DID = CTestHostVars.getRESERVED_DID();
    public static final String $_UUID = CTestHostVars.getRESERVED_UUID();
    public static final String $_SEQ = CTestHostVars.getRESERVED_SEQ();
    
    public static final String $_ENV_PREFIX = CTestHostVars.getRESERVED_ENV_PREFIX();


    // core IDs are currently disabled to be part of test IDs, since it is 
    // difficult to maintain test ID consistency - it should not change automatically,
    // because user may use them in filters or links to another SW, but if core
    // ID changes in test spec but not in test ID, info displayed in test tree
    // is confusing. Solution is to introduce filters for coreID, presence of stack usage,
    // analyzer, ... and then test cases with filter evaluated to true get some
    // kind of check mark or text color    
    public static final boolean IS_CORE_ID_PART_OF_TEST_ID = false;

    
    public static String[] getHostVarsForAnalyzerFileName() {
        StrVector hostVarsVector = new StrVector();
        CTestHostVars.getHostVarsForAnalyzerFileName(hostVarsVector);
        return DataUtils.strVector2StringArray(hostVarsVector);
    }


    public static String[] getHostVarsForGroupAnalyzerFileName() {
        StrVector hostVarsVector = new StrVector();
        CTestHostVars.getHostVarsForGroupAnalyzerFileName(hostVarsVector);
        return DataUtils.strVector2StringArray(hostVarsVector);
    }


    public static String[] getHostVarsForAutoTestID(boolean isForProposals) {
        StrVector hostVarsVector = new StrVector();
        CTestHostVars.getHostVarsForAutoTestId(hostVarsVector, 
                                               IS_CORE_ID_PART_OF_TEST_ID, 
                                               isForProposals);
        return DataUtils.strVector2StringArray(hostVarsVector);
    }


    public static String[] getHostVarsDescriptions(String [] hostVars) {
    
        ArrayList<String> descs = new ArrayList<>();
        
        for (String hostVarName : hostVars) {
            descs.add(getDesc(hostVarName));
        }
        
        return descs.toArray(new String[0]);
    }
    
    
    /**
     * Returns description for the given host var.
     * 
     * @param hostVarName
     * @return
     */
    public static String getDesc(String hostVarName) {

        if (m_descriptions == null) {
            m_descriptions = new TreeMap<>();

            m_descriptions.put("", "");

            m_descriptions.put($_TEST_ID, "Test case ID.");
            m_descriptions.put($_FUNCTION, "Name of tested function.");
            
            m_descriptions.put($_PARAMS, "Values of parameters separated by '-' sign. "
                + "All characters, which are not allowed in resulting string are replaced with '_'.");
            
            m_descriptions.put($_TAGS, "Test tags separated by '_'.");

            m_descriptions.put($_CORE_ID, "Core ID used to run a test.");

            m_descriptions.put($_DATE, 
                               "Date in ISO format when this file was created, for example 2015-04-03.");
            m_descriptions.put($_TIME, 
                               "Time in 24 hour format, when this file was created, for example: 17_48_23. Can be used in file names.");

            m_descriptions.put($_ISO_TIME, 
                               "Time in 24 hour format, when this file was created, for example: 17:48:23");
            
            m_descriptions.put($_UID, 
                               "Generates a string ID, which is unique in one testIDEA/script instance.");

            m_descriptions.put($_BATCH_DATE, 
                               "Date in ISO format when batch of test cases started execution, for example 2015-04-03.");
            m_descriptions.put($_BATCH_TIME, 
                               "Time in 24 hour format, when batch of test cases started execution, for example 17_48_23.");
            m_descriptions.put($_BATCH_UID, 
                               "Generates a string ID at test batch start, which is unique in one testIDEA/script instance.");
            
            m_descriptions.put($_ENV_PREFIX, "Environment variables prefix, for excample ${_env_HOME}.");
            
            m_descriptions.put($_NID, "Generates an ID with nested numbers for derived tests, for example 'x345s.2.4'. May not be unique.");
            m_descriptions.put($_DID, "Generates a derived ID as nested numbers for derived tests, for example '2.4'. May not be unique.");
            
            m_descriptions.put($_UUID, "Generates a UUID");
            
            m_descriptions.put($_SEQ, "Generates IDs, which are the same as test case sequence number. May not be unique.");

            m_descriptions.put($_USER_NAME, "Name of current user as known by computer.");
            m_descriptions.put($_WINIDEA_WORKSPACE_DIR, "Directory of winIDEA workspace file.");
            m_descriptions.put($_WINIDEA_WORKSPACE_FILE, "Name of winIDEA workspace file.");
            m_descriptions.put($_DEFAULT_DOWNLOAD_DIR, "Directory of default download file relative to winIDEA workspace directory.");
            m_descriptions.put($_DEFAULT_DOWNLOAD_FILE, "Name of default download file as configured in winIDEA.");
            m_descriptions.put($_SVN_REVISION, "Subversion revision number, if available."); 
        };
        
        String desc = m_descriptions.get(hostVarName);
        
        if (desc == null) {
            assert(true);
            desc = "";  // missing description is not critical for program operation
        }
        
        return desc;
    }
    
    
    public static String getTooltip(String tooltipPrefix, String [] hostVars) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append(tooltipPrefix);
        tooltip.append("\nThe following host variables may be used:\n\n");

        for (String hostVar : hostVars) {
            tooltip.append("    ").append(hostVar).append(" - ").append(getDesc(hostVar)).append('\n');
        }
        
        return tooltip.toString();
    }
    
    
    public static void setContentProposals(Control swtComponent, String[] hostVars) {
        
        ISysUIUtils.addContentProposalsAdapter(swtComponent,
                                               hostVars,
                                               getHostVarsDescriptions(hostVars),
                                               ContentProposalAdapter.PROPOSAL_INSERT,
                                               UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
    }
}
