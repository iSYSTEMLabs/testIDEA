package si.isystem.itest.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EBreakpointsType;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.ETristate;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.handlers.FilePropertiesCmdHandler;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.TestSpecModelListenerAdapter;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.InitSequencePrefsPage;
import si.isystem.itest.preferences.PreferenceInitializer;
import si.isystem.itest.preferences.TestBasePreferenceStore;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.itest.preferences.controls.PrefsTBTableEditor;
import si.isystem.ui.utils.SelectionAdapter;

public class TestConfigurationTab extends AbstractLaunchConfigurationTab {

    private InitSequencePrefsPage m_page;
    private CTestEnvironmentConfig m_uiEnvConfig;
    private TestBasePreferenceStore m_uiPrefStore;
    

    @Override
    public void createControl(Composite parent) {

        /*
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
            if (m_isUseNewEnvConfig) {
                // make a copy, so that the original is not changed
                m_newEnvConfig = new CTestEnvironmentConfig(null);
                m_newEnvConfig.assign(envConfig);
                envConfig = m_newEnvConfig;
            }
        } */       

        m_uiEnvConfig = new CTestEnvironmentConfig(null);
        m_uiPrefStore = new TestBasePreferenceStore(m_uiEnvConfig);
        
        PreferenceInitializer prefInitializer = new PreferenceInitializer();
        PreferenceManager mgr = new PreferenceManager();

        m_page = FilePropertiesCmdHandler.createInitSequencePage(prefInitializer, 
                                                                 mgr, 
                                                                 m_uiPrefStore,
                                                                 m_uiEnvConfig);
        
        Control compositeControl = m_page.createContents(parent);
        
        addListenersToFields();
        setControl(compositeControl);
    }


    public void addListenersToFields() {
        
        PrefBooleanEditor isRunInitSeqCb = m_page.getIsRunInitSequence();
        isRunInitSeqCb.getCheckBoxControl().addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_page.getIsRunInitSequence().store();
                m_uiPrefStore.save();
                updateLaunchConfigurationDialog();
            }
        });

    
        PrefsTBTableEditor table = m_page.getInitSeqTable();
        table.addModifyListener(new TestSpecModelListenerAdapter() {
            
            @Override
            public void modelChanged(ModelChangedEvent event) {
                m_page.getInitSeqTable().store();
                m_uiPrefStore.save();
                updateLaunchConfigurationDialog();
            }
        });
            
        
        PrefBooleanEditor isCheckTargetState = m_page.getIsCheckTargetBeforeRun();
        isCheckTargetState.getCheckBoxControl().addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_page.getIsCheckTargetBeforeRun().store();
                m_uiPrefStore.save();
                updateLaunchConfigurationDialog();
            }
        });

        
        PrefBooleanEditor isVerifySymbolsBeforeRun = m_page.getIsVerifySymbolsBeforeRun();
        isVerifySymbolsBeforeRun.getCheckBoxControl().addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_page.getIsVerifySymbolsBeforeRun().store();
                m_uiPrefStore.save();
                updateLaunchConfigurationDialog();
            }
        });

        
        PrefBooleanEditor isDisableInterrupts = m_page.getIsDisableInterrupts();
        isDisableInterrupts.getCheckBoxControl().addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_page.getIsDisableInterrupts().store();
                m_uiPrefStore.save();
                updateLaunchConfigurationDialog();
            }
        });
        
        
/*        PrefRadioGroupEditor bpTypeRadios = m_page.getBreakpointsType();
        // workaround with Composite, since radio buttons are private in 
        // the editor and have no accessor method.
        Composite composite = bpTypeRadios.getRadioBox();
        Control[] radioButtons = composite.getChildren();
        for (Control btn : radioButtons) {
            if (btn instanceof Button) {
                ((Button) btn).addSelectionListener(new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        m_page.getBreakpointsType().store();
                        m_uiPrefStore.save();
                        updateLaunchConfigurationDialog();
                    }
                });
            }
        } */
    }

    
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        System.out.println("TestConfigurationTab.setDefaults");
        TestSpecificationModel model = TestCaseEditorPart.getActiveModel();
        
        CTestEnvironmentConfig envConfig;
        
        if (model != null) { 
            // initialize run configuration with run configuration from the
            // currently selected editor
            envConfig = model.getCEnvironmentConfiguration();
        } else {
            // use default values 
            envConfig = new CTestEnvironmentConfig(null);
        }

        copyTestEnv2LaunchConfig(envConfig, configuration);
        
    }


    public void copyTestEnv2LaunchConfig(CTestEnvironmentConfig envConfig,
                                         ILaunchConfigurationWorkingCopy configuration) {
        
        configuration.setAttribute(envConfig.getTagName(EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ.swigValue()), 
                                   envConfig.isAlwaysRunInitSeqBeforeRun());

        int section = EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue();
        configuration.setAttribute(envConfig.getTagName(section), 
                                   envConfig.getTestBaseList(section, true).toString());
        
        configuration.setAttribute(envConfig.getTagName(EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN.swigValue()), 
                                   envConfig.isCheckTargetStateBeforeRun());

        configuration.setAttribute(envConfig.getTagName(EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN.swigValue()), 
                                   envConfig.isCheckTargetStateBeforeRun());

        configuration.setAttribute(envConfig.getTagName(EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS.swigValue()), 
                                   envConfig.isDisableInterrupts());

        configuration.setAttribute(envConfig.getTagName(EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE.swigValue()), 
                                   envConfig.getBreakpointType().toString());
    }
    
    
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        
        try {
            boolean isAlwaysRunInitSeq = isAlwaysRunInitSeq(m_uiEnvConfig, configuration);
            
            setInitSeqFromConfig(m_uiEnvConfig, configuration);
            
            boolean isCheckTargetState =  
              configuration.getAttribute(m_uiEnvConfig.getTagName(EEnvConfigSections.E_SECTION_CHECK_TARGET_STATE_BEFORE_RUN.swigValue()), 
                                         m_uiEnvConfig.isCheckTargetStateBeforeRun());

            boolean isVerifySymbolsBeforeRun =  
                    configuration.getAttribute(m_uiEnvConfig.getTagName(EEnvConfigSections.E_SECTION_VERIFY_SYMBOLS_BEFORE_RUN.swigValue()), 
                                               m_uiEnvConfig.isVerifySymbolsBeforeRun());

            boolean isDisableInterrupts = 
              configuration.getAttribute(m_uiEnvConfig.getTagName(EEnvConfigSections.E_SECTION_DISABLE_INTERRUPTS.swigValue()), 
                                         m_uiEnvConfig.isDisableInterrupts());

            String bpType = 
              configuration.getAttribute(m_uiEnvConfig.getTagName(EEnvConfigSections.E_SECTION_BREAKPOINTS_TYPE.swigValue()), 
                                         m_uiEnvConfig.getBreakpointType().toString());
            
            m_uiEnvConfig.setAlwaysRunInitSeqBeforeRun(isAlwaysRunInitSeq ? ETristate.E_TRUE : ETristate.E_FALSE);
            m_uiEnvConfig.setCheckTargetStateBeforeRun(isCheckTargetState ? ETristate.E_TRUE : ETristate.E_FALSE);
            m_uiEnvConfig.setVerifySymbolsBeforeRun(isVerifySymbolsBeforeRun ? ETristate.E_TRUE : ETristate.E_FALSE);
            m_uiEnvConfig.setDisableInterrupts(isDisableInterrupts ? ETristate.E_TRUE : ETristate.E_FALSE);
            m_uiEnvConfig.setBreakpointsType(EBreakpointsType.valueOf(bpType));

            m_page.getIsRunInitSequence().load();
            m_page.getInitSeqTable().load();
            m_page.getIsCheckTargetBeforeRun().load();
            m_page.getIsDisableInterrupts().load();
            // m_page.getBreakpointsType().load();
        } catch (CoreException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        
        m_page.setPreferenceStore(m_uiPrefStore);
    }


    public static void setInitSeqFromConfig(CTestEnvironmentConfig envConfig, 
                                     ILaunchConfiguration configuration) throws CoreException {
        int initSeqSection = EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue();
        String initSequence =
          configuration.getAttribute(envConfig.getTagName(initSeqSection), 
                                     envConfig.getTestBaseList(initSeqSection, true).toString());
        CYAMLUtil.parseTestBaseList(initSequence, envConfig, initSeqSection);
    }


    public static boolean isAlwaysRunInitSeq(CTestEnvironmentConfig envConfig,
                                              ILaunchConfiguration configuration) throws CoreException {
        return configuration.getAttribute(envConfig.getTagName(EEnvConfigSections.E_SECTION_ALWAYS_RUN_INIT_SEQ.swigValue()), 
                                          envConfig.isAlwaysRunInitSeqBeforeRun());
    }

    
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        copyTestEnv2LaunchConfig(m_uiEnvConfig, configuration);
    }

    
    @Override
    public String getName() {
        return "Test init";
    }

/*

    @Override
    public String getErrorMessage() {
        return null;
    }


    @Override
    public String getMessage() {
        return null;
    }


    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        return false;
    }


    @Override
    public boolean canSave() {
        return false;
    }


    @Override
    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
    }


    @Override
    public void launched(ILaunch launch) {
    }


    @Override
    public Image getImage() {
        return null;
    }


    @Override
    public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
    }


    @Override
    public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
    }
*/
}
