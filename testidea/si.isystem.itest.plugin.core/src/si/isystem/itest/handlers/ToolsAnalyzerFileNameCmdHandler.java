package si.isystem.itest.handlers;

import java.util.EnumSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.AnalyzerFileNameSetDialog;
import si.isystem.itest.dialogs.AnalyzerFileNameSetDialog.EAnalyzerType;
import si.isystem.itest.dialogs.AnalyzerFileNameSetDialog.EExistingState;
import si.isystem.itest.dialogs.AnalyzerFileNameSetDialog.EScope;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class ToolsAnalyzerFileNameCmdHandler extends AbstractHandler {

    private AnalyzerFileNameSetDialog m_dlg;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        if (m_dlg == null) {
            m_dlg = new AnalyzerFileNameSetDialog(Activator.getShell());
        }
        
        if (m_dlg.show()) {
            setAnalyzerFileNames(m_dlg.getExistingStateCondition(), 
                                 m_dlg.getScope(),
                                 m_dlg.getAnalyzerType());
        }

        return null;
    }
    

    private void setAnalyzerFileNames(EExistingState existingStateCondition,
                                      EScope scope,
                                      EnumSet<EAnalyzerType> analyzerType) {
        
        final TestSpecificationModel model = TestSpecificationModel.getActiveModel();

        GroupAction groupAction = new GroupAction("Set Auto IDs");
        
        CTestSpecification testSpec = null;
        int depth = 0;
        
        switch (scope) {
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
            throw new SIllegalStateException("The specified scope is not supported!")
                               .add("scope", scope);
        }
        
        if (testSpec == null) {
            MessageDialog.openInformation(Activator.getShell(), 
                                          "Information", 
                                          "No analyzer file names were set, because "
                                          + "there are no selected test cases!");
            return; 
        }
        
        String docFileName = model.getCEnvironmentConfiguration().getToolsConfig(true).getAnalyzerFName();
        
        try {
            assignFileNames(testSpec, docFileName, depth, existingStateCondition, 
                            analyzerType, groupAction);

            groupAction.addAllFireEventTypes();
            groupAction.addEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_DATA_CHANGED,
                                                       null, null, 
                                                       ENodeId.ANALYZER_NODE));
            if (!groupAction.isEmpty()) {
                model.execAction(groupAction);
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Setting of analyzer file names failed!\n" +
                                  "  fileName: " + docFileName, 
                                  ex);
        }
    }


    private void assignFileNames(CTestSpecification testSpec,
                                 String docFileName,
                                 int depth,
                                 EExistingState existingStateCondition,
                                 EnumSet<EAnalyzerType> analyzerType, 
                                 GroupAction groupAction) {
        
        setAnalyzerFileName(testSpec, docFileName, existingStateCondition, 
                            analyzerType, groupAction);
        
        if (depth > 0  ||  depth < 0) {
            int numDerived = testSpec.getNoOfDerivedSpecs();
            for (int i = 0; i < numDerived; i++) {
                CTestSpecification derived = testSpec.getDerivedTestSpec(i);
                assignFileNames(derived, docFileName, depth - 1, 
                                existingStateCondition, analyzerType,
                                groupAction);
            }
        }
    }


    private void setAnalyzerFileName(CTestSpecification testSpec,
                                     String docFileName,
                                     EExistingState existingStateCondition,
                                     EnumSet<EAnalyzerType> analyzerType, 
                                     GroupAction groupAction) {
        
        CTestAnalyzer analyzer = testSpec.getAnalyzer(true);
        if (!analyzer.isEmpty()  &&  analyzer.getRunMode() == ERunMode.M_START) {
            String existingFName = analyzer.getDocumentFileName();
            if (existingStateCondition == EExistingState.EOnlyEmpty) {
                if (existingFName.isEmpty()) {
                    setFileName(analyzer, docFileName, analyzerType, groupAction);
                }
            } else {
                setFileName(analyzer, docFileName, analyzerType, groupAction);
            }
        }
    }


    private void setFileName(CTestAnalyzer analyzer,
                             String docFileName,
                             EnumSet<EAnalyzerType> analyzerType, 
                             GroupAction groupAction) {
        
        if (analyzerType.contains(EAnalyzerType.ETraceActive)  &&  !analyzer.getTrace(true).isEmpty()  ||
                analyzerType.contains(EAnalyzerType.ECoverageActive)  &&  !analyzer.getCoverage(true).isEmpty()  ||
                analyzerType.contains(EAnalyzerType.EProfilerActrive)  &&  !analyzer.getProfiler(true).isEmpty()) {

            YamlScalar idScalar = YamlScalar.newMixed(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_DOC_FILE_NAME.swigValue());
            idScalar.dataFromTestSpec(analyzer);
            
            idScalar.setValue(docFileName);
            SetSectionAction action = new SetSectionAction(analyzer, 
                                                           ENodeId.ANALYZER_NODE, 
                                                           idScalar);
            groupAction.add(action);
        }
    }
}
