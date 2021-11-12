package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import si.isystem.connect.CInitSequenceAction;
import si.isystem.connect.CInitSequenceAction.EInitAction;
import si.isystem.connect.CInitSequenceAction.EInitSequenceSectionIds;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.ETristate;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.TestRunner;


public class InitTargetCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {
            TestSpecificationModel activeModel = TestSpecificationModel.getActiveModel();
            if (activeModel == null) {
                MessageDialog.openError(Activator.getShell(), "Can not initialize target!", "No init sequence is specified. "
                        + "Please open iyaml file.");
                return null;
            }
            
            CTestEnvironmentConfig envConfig = activeModel.getTestEnvConfig();
            
            CTestBaseList initSeq = 
                    envConfig.getTestBaseList(EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                              false);
            
            if (initSeq.isEmpty()) {
                if (MessageDialog.openQuestion(Activator.getShell(), 
                       "Empty init sequence", 
                       "Init sequence is empty - do you want to create a default one (download, run until main(), delete breakpoints)?\n"
                       + "You can later modify it with menu 'Test | Configuration ...'")) {
                    
                    CInitSequenceAction action = new CInitSequenceAction(envConfig);
                    action.setAction(EInitAction.EIADownload);
                    initSeq.add(-1, action);
                    
                    action = new CInitSequenceAction(envConfig);
                    action.setAction(EInitAction.EIARun);
                    CSequenceAdapter runParams = new CSequenceAdapter(action,
                                                                      EInitSequenceSectionIds.E_INIT_SEQ_PARAMS.swigValue(),
                                                                      false);
                    runParams.add(0, "main");
                    initSeq.add(-1, action);
                    
                    action = new CInitSequenceAction(envConfig);
                    action.setAction(EInitAction.EIADeleteAllBreakpoints);
                    initSeq.add(-1, action);
                }
                
            }
            
            CTestEnvironmentConfig newRunConfig = CTestEnvironmentConfig.cast(CYAMLUtil.cto2ctb(envConfig.copy()));
            newRunConfig.setAlwaysRunInitSeqBeforeRun(ETristate.E_TRUE);

            TestRunner runner = new TestRunner();
            runner.init(newRunConfig, activeModel.getModelFileName());
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not initialize target!", ex);
        }
            
        return null;
    }
    
}
