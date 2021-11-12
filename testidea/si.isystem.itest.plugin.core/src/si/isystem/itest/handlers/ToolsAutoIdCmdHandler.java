package si.isystem.itest.handlers;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.AutoIdGenerator;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.AutoIDCommandDialog;
import si.isystem.itest.dialogs.AutoIDCommandDialog.ESequenceStart;
import si.isystem.itest.dialogs.AutoIDCommandDialog.ETestIdSettingScope;
import si.isystem.itest.dialogs.AutoIDCommandDialog.ETestIdSettingType;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.HostVarsUtils;

public class ToolsAutoIdCmdHandler extends AbstractHandler {

    private AutoIDCommandDialog m_dlg;
    private Pattern m_idValuePattern = Pattern.compile("(/.*/)");        //  "/ert3tg/
    private Pattern m_idVarPattern = Pattern.compile("/(\\$\\{.*\\})/"); //  "/${uid}/"
    private AutoIdGenerator m_autoIdGenerator;
    private GroupAction m_groupAction;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        m_autoIdGenerator = new AutoIdGenerator();
        
        if (m_dlg == null) {
            m_dlg = new AutoIDCommandDialog(Activator.getShell());
        }
        
        if (m_dlg.show()) {
            setIDs(m_dlg.getTestIdSettingType(), m_dlg.getTestIdSettingScope(), 
                   m_dlg.getSequenceStartType(), m_dlg.getCustomStart());
        }

        return null;
    }

    
    private void setIDs(ETestIdSettingType testIdSettingType, ETestIdSettingScope eTestIdSettingScope, 
                        ESequenceStart eSequenceStart, String seqStartStr) {

        final TestSpecificationModel model = TestSpecificationModel.getActiveModel();

        int seqStart = 0;
        switch (eSequenceStart) {
        case EZero:
            seqStart = 0; 
            break;
        case EOne:
            seqStart = 1; 
            break;
        case ENoOfTestCases:
            seqStart = model.getNoOfTestCases();
            break;
        case ECustom:
            seqStart = Integer.parseInt(seqStartStr);
            break;
        default:
            seqStart = 0; 
        }

        m_autoIdGenerator.setTestCounter(seqStart); 

        m_groupAction = new GroupAction("Set Auto IDs");
        
        CTestSpecification testSpec = null;
        int depth = 0;
        
        switch (eTestIdSettingScope) {
        case EAll:
            testSpec = model.getRootTestSpecification();
            depth = -1;
            break;
        case ESelected:
            testSpec = UiUtils.getSelectedTestSpecifications();
            depth = 1;
            break;
        case ESelectedAndDerived:
            testSpec = UiUtils.getSelectedTestSpecifications();
            depth = -1;
            break;
        default:
            throw new SIllegalStateException("The specified auto ID assignement " +
            		"scope is not supported!").add("scope", eTestIdSettingScope);
        }
        
        if (testSpec == null) {
            MessageDialog.openInformation(Activator.getShell(), 
                                          "Information", "No test IDs were set, because there are no selected test cases!");
            return; 
        }
        
        String formatString = model.getCEnvironmentConfiguration().getAutoIdFormatString();
        
        try {
            assignTestIDs(testSpec, formatString, true, depth, testIdSettingType);
            m_groupAction.addTreeChangedEvent(null, null);
            m_groupAction.addAllFireEventTypes();
            model.execAction(m_groupAction);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Automatic setting of IDs failed!\n" +
                                  "  formatString: " + formatString, 
                                  ex);
        }
    }
    
    
    /**
     * 
     * @param testSpec
     * @param formatString 
     * @param isContainer if true, then the given test spec is container, so it does not
     *                    get ID assigned (for example root test spec, or container of
     *                    selected test specs). Only children get IDs assigned. If false,
     *                    also the given testSpec gets ID assigned.
     * @param depth defines how many levels of derived test specs. get ID assigned. Value 0
     *                      means only the given test spec, value -1 means all
     *                      test specs. If isContainer == true, then
     *                      depth should be at least one.
     * @param testIdSettingType 
     */
    private void assignTestIDs(CTestSpecification testSpec, String formatString, boolean isContainer, 
                              int depth, ETestIdSettingType testIdSettingType) {
        
        Matcher varMatcher = m_idVarPattern.matcher(formatString);
        
        String uidName = ""; // empty value means no uid variable is specified in the format string.  
        if (varMatcher.find()) {
            uidName = varMatcher.group(1); // uses the first group found. Multiple Ids are not supported
                                          // because they don't make much sense. It is also user's
                                          // responsibility to use '/' only for uids.
        } 

        String nidPrefix = "";
        
        if (!isContainer) {
            Map<String, String> vars = m_autoIdGenerator.createVars(testSpec, 0, nidPrefix, "");
            nidPrefix = m_autoIdGenerator.createTestId(formatString, vars);
            setTestId(testSpec, vars.get(uidName), nidPrefix, testIdSettingType);
        }
            
        if (depth > 0  ||  depth == -1) {
            int numDerived = testSpec.getNoOfDerivedSpecs();
            for (int i = 0; i < numDerived; i++) {
                CTestSpecification derived = testSpec.getDerivedTestSpec(i);
                assignTestIDs(derived, formatString, nidPrefix, i, depth - 1, "", testIdSettingType,
                              uidName);
            }
        }
    }

    
    private void assignTestIDs(CTestSpecification testSpec, String formatString, String nidPrefix, 
                               int testNo, int depth, String didPrefix, ETestIdSettingType testIdSettingType,
                               String uidName) {
        
        Map<String, String> vars = m_autoIdGenerator.createVars(testSpec, testNo, nidPrefix, didPrefix);
        String newId = m_autoIdGenerator.createTestId(formatString, vars);
        
        setTestId(testSpec, vars.get(uidName), newId, testIdSettingType);
        
        nidPrefix = vars.get(HostVarsUtils.$_NID);
        didPrefix = vars.get(HostVarsUtils.$_DID);

        if (depth > 0  ||  depth < 0) {
            int numDerived = testSpec.getNoOfDerivedSpecs();
            for (int i = 0; i < numDerived; i++) {
                CTestSpecification derived = testSpec.getDerivedTestSpec(i);
                assignTestIDs(derived, formatString, nidPrefix, i, depth - 1, didPrefix, 
                              testIdSettingType, uidName);
            }
        }
    }
    

    private void setTestId(CTestSpecification testSpec, 
                           String newUid,
                           String id,
                           ETestIdSettingType testIdSettingType) {
        
        YamlScalar idScalar = YamlScalar.newMixed(SectionIds.E_SECTION_ID.swigValue());
        idScalar.dataFromTestSpec(testSpec);
        SetSectionAction action = null;

        switch (testIdSettingType) {
        case EAllIds:
            idScalar.setValue(id);
            action = new SetSectionAction(testSpec, ENodeId.META_NODE, idScalar);
            break;
        case EOnlyEmpty:
            if (testSpec.getTestId().isEmpty()) {
                idScalar.setValue(id);
                action = new SetSectionAction(testSpec, ENodeId.META_NODE, idScalar);
            }
            break;
        case EOnlyUid: {
            if (newUid == null) {
                throw new SIllegalStateException("No 'uid' variable specified in format string! Please open " +
                        "'File | Properties | General' and define proper format string!");
            }
            
            String existingIdValue = testSpec.getTestId();
            Matcher valueMatcher = m_idValuePattern.matcher(existingIdValue);
            if (valueMatcher.find()) {   // "/id/" was found in the existing id
                String newId = existingIdValue.substring(0, valueMatcher.start() + 1) +
                               newUid + 
                               existingIdValue.substring(valueMatcher.end() - 1); 
                idScalar.setValue(newId);
                action = new SetSectionAction(testSpec, ENodeId.META_NODE, idScalar);
            } // else id is not modified 
        } break;
        case EOnlyNonUid: {
            if (newUid == null) {
                throw new SIllegalStateException("No 'uid' variable specified in format string! Please open " +
                        "'File | Properties | General' and define proper format string!");
            }
            
            String existingIdValue = testSpec.getTestId();
            Matcher valueMatcher = m_idValuePattern.matcher(existingIdValue);
            if (valueMatcher.find()) {   // "/id/" was found in the existing id
                String oldUid = valueMatcher.group();
                Matcher newUidMatcher = m_idValuePattern.matcher(id);
                if (newUidMatcher.find()) {
                    // compose uid of current testID and non-uid part of the new test id
                    String newId = id.substring(0, newUidMatcher.start()) +
                                   oldUid + 
                                   id.substring(newUidMatcher.end()); 
                    idScalar.setValue(newId);
                    action = new SetSectionAction(testSpec, ENodeId.META_NODE, idScalar);
                }
            } // else id is not modified 
            } break;
        default:
            throw new SIllegalStateException("Invalid test ID setting mode!").
                      add("testIdSettingMode", testIdSettingType);
        }
        
        if (action != null) {
            action.addTreeChangedEvent(); // to update tree
            // update meta section, but do not select test spec. in tree view.
            action.addEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                                  null, null, ENodeId.META_NODE)); 
            m_groupAction.add(action);
        }
    }
}