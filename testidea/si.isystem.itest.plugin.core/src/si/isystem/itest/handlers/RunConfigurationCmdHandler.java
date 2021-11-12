package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.dialogs.PropertyDialog;

import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.PreferenceInitializer;
import si.isystem.itest.preferences.TestBasePreferenceStore;

@SuppressWarnings("restriction")
public class RunConfigurationCmdHandler extends AbstractHandler {
    

    private boolean m_isUseNewEnvConfig;
    private CTestEnvironmentConfig m_newEnvConfig;


    public RunConfigurationCmdHandler() {
        m_isUseNewEnvConfig = false;
    }

    
    /**
     * Call this method, when you don't want to modify env config. from project.
     */
    public void useNewEnvConfig() {
        m_isUseNewEnvConfig = true;
    }


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
//            TestSpecificationEditorView.saveGUIData();

            Shell shell = Activator.getShell();
            PreferenceInitializer prefInitializer = new PreferenceInitializer();
            
            PreferenceManager mgr = new PreferenceManager();

            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
            if (m_isUseNewEnvConfig) {
                // make a copy, so that the original is not changed
                m_newEnvConfig = new CTestEnvironmentConfig(null);
                m_newEnvConfig.assign(envConfig);
                envConfig = m_newEnvConfig;
            }
            
            TestBasePreferenceStore prefStore = new TestBasePreferenceStore(envConfig);
            
            FilePropertiesCmdHandler.createInitSequencePage(prefInitializer, mgr, 
                                                            prefStore,
                                                            envConfig);
            FilePropertiesCmdHandler.createRunConfigPage(prefInitializer, mgr, 
                                                         prefStore);
            
            // the selection is not used in our case, but it must not be null
            ISelection selection = UiUtils.getStructuredSelection();
            
            PropertyDialog dialog = new PropertyDialog(shell, mgr, selection);
            dialog.create();
            dialog.setMessage("Project properties");
            if (dialog.open() == PropertyDialog.OK) {
                prefStore.save();
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can't modify configuration!", ex);
        }
        
        return null;
    }

    
    public CTestEnvironmentConfig getEnvConfig() {
        return m_newEnvConfig;
    }
}
