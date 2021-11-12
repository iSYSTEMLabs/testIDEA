package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestFunction.ESection;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResultBase;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.run.Script;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.ui.utils.KGUIBuilder;

public class ScriptsSpecEditor extends SectionEditorAdapter {

    class ControlsForScript {
        TBControlText m_funcNameTBTxt;
        TBControlText m_paramsTBTxt;
        ValueAndCommentEditor m_tagEditor;
        StatusDecoration m_statusDec;
        TBControlTristateCheckBox m_isInheritTB;
        
        void setEnabled(boolean isEnabled) {
            m_funcNameTBTxt.setEnabled(isEnabled);
            m_paramsTBTxt.setEnabled(isEnabled);
            m_tagEditor.setEnabled(isEnabled);
            m_isInheritTB.setEnabled(isEnabled);
        }
        
        void clearInput() {
            m_funcNameTBTxt.clearInput();
            m_paramsTBTxt.clearInput();
            m_statusDec.setDescriptionText("", EStatusType.INFO);
            setInputForInheritCb(null, m_isInheritTB);
        }

        public void setInput(SectionIds section) {
            
            
            setCurrentTS(section);
            
            CTestBase funcTB = m_currentTestSpec.getTestBase(section.swigValue(), false);
            
            m_funcNameTBTxt.setInput(funcTB, m_isInherited);
            m_paramsTBTxt.setInput(funcTB, m_isInherited);

            setInputForInheritCb(section, m_isInheritTB);
            m_tagEditor.updateValueAndCommentFromTestBase(m_currentTestSpec);
        }
    }
    
    
    private ControlsForScript m_initTargetFuncCtls = new ControlsForScript();
    private ControlsForScript m_initFuncCtls = new ControlsForScript();
    private ControlsForScript m_endFuncCtls = new ControlsForScript();
    private ControlsForScript m_restoreFuncCtls = new ControlsForScript();

    
    public ScriptsSpecEditor() {
        super(ENodeId.SCRIPT_NODE, 
              SectionIds.E_SECTION_INIT_TARGET,
              SectionIds.E_SECTION_INITFUNC,
              SectionIds.E_SECTION_ENDFUNC,
              SectionIds.E_SECTION_RESTORE_TARGET);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        Composite scriptsPanel = new Composite(scrolledPanel, SWT.NONE);

        scriptsPanel.setLayout(new MigLayout("fillx", "[fill]", ""));
        
        KGUIBuilder builder = new KGUIBuilder(scriptsPanel);

        // init target func
        createControllsForScript(builder, 
                                 SectionIds.E_SECTION_INIT_TARGET, 
                                 m_initTargetFuncCtls, 
                                 "Init target script function", 
                                 "Name of script function to be called before any test initialization is done.\n"
                                 + "The stack is not modified yet. Use this function to initialize target.\n"
                                 + "Reserved script variable for test report info: self._isys_initTargetInfo\n"
                                 + "Example:\n"
                                 + "    myInitTargetFunc", 
                                 EHControlId.EScriptInitTargetFName, 
                                 "initTargetFuncName", 
                                 EHControlId.EScriptInitTargetParams, 
                                 "initTargetFuncParams",
                                 "\n\nIMPORTANT: Host variables used as parameters for this script function\n"
                                 + "    must be defined in test case which executes before this one,\n"
                                 + "    because Variables section is processed AFTER this script function.\n"
                                 + "    is called.");
        
        
        // init func
        createControllsForScript(builder, 
                                 SectionIds.E_SECTION_INITFUNC, 
                                 m_initFuncCtls, 
                                 "Init test script function", 
                                 "Name of script function to be called just before function under test is called.\n"
                                 + "Stack with initialized local variables is ready.\n" 
                                 + "Use this function for advanced initialization of variables.\n" 
                                 + "Reserved script variable for test report info: self._isys_initFuncInfo\n"
                                 + "Example:\n" 
                                 + "    myInitFunc", 
                                 EHControlId.EScriptInitFuncFName, 
                                 "initFuncName", 
                                 EHControlId.EScriptInitFuncParams, 
                                 "initFuncParams",
                                 "");
        
        // end func
        createControllsForScript(builder, 
                                 SectionIds.E_SECTION_ENDFUNC, 
                                 m_endFuncCtls, 
                                 "End test script function", 
                                 "Name of script function to be called just after the function under test executes.\n" 
                                 + "Stack with local variables and return value is still there.\n" 
                                 + "Use this function for advanced verification of variables and return value.\n" 
                                 + "Reserved script variable for test report info: self._isys_endFuncInfo\n"
                                 + "Example:\n" 
                                 + "    myEndFunc", 
                                 EHControlId.EScriptEndFuncFName, 
                                 "endFuncName", 
                                 EHControlId.EScriptEndFuncParams, 
                                 "endFuncParams",
                                 "");
        
        // restore target func
        createControllsForScript(builder, 
                                 SectionIds.E_SECTION_RESTORE_TARGET, 
                                 m_restoreFuncCtls, 
                                 "Restore target script function", 
                                 "Name of script function to be called after the test completes.\n" 
                                 + "Stack is already restored to pre-test state at this point.\n" 
                                 + "Use this function to restore target state modified in init target script function.\n" 
                                 + "Reserved script variable for test report info: self._isys_restoreTargetInfo\n"
                                 + "Example:\n" 
                                 + "    myRestoreTargetFunc", 
                                 EHControlId.EScriptRestoreTargetFName, 
                                 "restoreTargetFuncName", 
                                 EHControlId.EScriptRestoreTargetParams, 
                                 "restoreTargetFuncParams",
                                 "");
        
        // Use non-editable Text instead of Label, so users can select and copy name 
        // of the parameter. 
        Text hint = builder.text("gapleft 10");
        hint.setText("Hint: Use ' " + Script.RESERVED_TEST_SPEC_PARAM + 
                     " ' as name of the first parameter to pass test specification to script function.");
        hint.setEditable(false);
        
        return configureScrolledComposite(scrolledPanel, scriptsPanel);
    }

    
    private void createControllsForScript(KGUIBuilder builder, 
                                          SectionIds sectionId,
                                          ControlsForScript ctrls,
                                          String groupTitle,
                                          
                                          String funcTooltip,
                                          EHControlId funcCtrlId, 
                                          String funcSwtBotId,
                                          
                                          EHControlId paramsCtrlId,
                                          String paramsSwtBotId,
                                          String paramsToolTipExtension) {
        
        KGUIBuilder initTargetBuilder = builder.group(groupTitle, 
                                                      "wmin 0, gaptop 20, wrap", 
                                                      new MigLayout("fillx", "[min!][fill][min!]"), SWT.NONE);
        initTargetBuilder.label("Script function:");
        
        ctrls.m_funcNameTBTxt = TBControlText.createForMixed(initTargetBuilder, 
                                               funcTooltip, 
                                               "gapleft 10, growx", 
                                               ESection.E_SECTION_FUNC_NAME.swigValue(), 
                                               m_nodeId, 
                                               funcCtrlId, 
                                               SWT.BORDER);
        
        ctrls.m_funcNameTBTxt.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                                   funcSwtBotId);
        
        ctrls.m_tagEditor = ValueAndCommentEditor.newKey(sectionId.swigValue(), 
                                                         ctrls.m_funcNameTBTxt.getControl(),
                                                         SWT.LEFT | SWT.TOP);
        
        ctrls.m_statusDec = new StatusDecoration(ctrls.m_funcNameTBTxt.getControl(), 
                                                 SWT.LEFT | SWT.BOTTOM);

        ctrls.m_isInheritTB = createTristateInheritanceButton(initTargetBuilder, 
                                                              "gapleft 30, align right, gapright 10, wrap");
        ctrls.m_isInheritTB.setActionProvider(new InheritedActionProvider(sectionId));


        // No longer needed after fixed double sending of Data Changed events.
        // 
        // Workaround for refresh bug in Mig layout for decorations inside groups.
        // To reproduce, remove this label, then resize tesIDEA window left-right. 
        // The right decoration icon will disappear or multiply. This seems to 
        // be a problem of Mig layout combined with text controls with decorations 
        // inside groups. This solution is not 100% (there are still visible some 
        // artifacts on right side decorations on window resize, but have no
        // better solution at the moment, and this is not critical. To remove artifacts
        // click some other test section and back to scripts.

        // initTargetBuilder.label(" ", "gapleft 8, gapbottom push, growx, wrap"); 

        initTargetBuilder.label("Parameters:");
        
        ctrls.m_paramsTBTxt = TBControlText.createForList(initTargetBuilder, 
                     "Values of parameters for the script function, separated by commas.\n"
                     + "Specify reserved name '_isys_testSpec' as first parameter to pass test case specification to script function.\n"
                     + "Examples:\n"
                     + "    30, \"abc\"\n" 
                     + "    counter = 23, file = 'log.txt'\n"
                     + "    _isys_testSpec, ${MY_HOST_VARIABLE}\n\n"
                     + "It is also recommended to store description of script parameters into comment ('i' icon on the right)."
                     + paramsToolTipExtension, 
                     "gapleft 10, span 2, wrap", 
                     ESection.E_SECTION_PARAMS.swigValue(), 
                     m_nodeId, 
                     paramsCtrlId, 
                     SWT.BORDER);
                                           
        ctrls.m_paramsTBTxt.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                                 paramsSwtBotId);
        
        ctrls.m_funcNameTBTxt.setMainTagEditor(ctrls.m_tagEditor);
    }
    
    
    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testSpec != null;
        m_initTargetFuncCtls.setEnabled(isEnabled);
        m_initFuncCtls.setEnabled(isEnabled);
        m_endFuncCtls.setEnabled(isEnabled);
        m_restoreFuncCtls.setEnabled(isEnabled);
    
        if (m_testSpec == null) {

            m_initTargetFuncCtls.clearInput();
            m_initFuncCtls.clearInput();
            m_endFuncCtls.clearInput();
            m_restoreFuncCtls.clearInput();
            
            return;            
        }

        m_initTargetFuncCtls.setInput(SectionIds.E_SECTION_INIT_TARGET);
        m_initFuncCtls.setInput(SectionIds.E_SECTION_INITFUNC);
        m_endFuncCtls.setInput(SectionIds.E_SECTION_ENDFUNC);
        m_restoreFuncCtls.setInput(SectionIds.E_SECTION_RESTORE_TARGET);
        

        CTestResult result = m_model.getResult(m_testSpec);
        setStatusText(m_initTargetFuncCtls.m_statusDec, result, CTestResultBase.getSE_INIT_TARGET());
        setStatusText(m_initFuncCtls.m_statusDec, result, CTestResultBase.getSE_INIT_FUNC());
        setStatusText(m_endFuncCtls.m_statusDec, result, CTestResultBase.getSE_END_FUNC());
        setStatusText(m_restoreFuncCtls.m_statusDec, result, CTestResultBase.getSE_RESTORE_TARGET());
    }
    
    
    @Override
    public boolean isError(CTestResult result) {
        return result.isScriptError();  
    }
    
    
    @Override
    public boolean hasErrorStatus() {
        return !isEmpty();
    }
    
    
    public static void setStatusText(StatusDecoration decoration,
                                     CTestResultBase result,
                                     String scriptFuncType) {
        if (result != null) {
            if (result.isScriptError(scriptFuncType)) {
                decoration.setDescriptionText(result.getScriptOutput(scriptFuncType) +
                                              "\n-----------\n" + //$NON-NLS-1$
                                              result.getScriptError(scriptFuncType),
                                              EStatusType.ERROR);
            } else {
                decoration.setDescriptionText(result.getScriptOutput(scriptFuncType),
                                              EStatusType.INFO);
            }
        } else {
            decoration.setDescriptionText("", EStatusType.INFO); //$NON-NLS-1$
        }
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_INIT_TARGET.swigValue(),
                SectionIds.E_SECTION_INITFUNC.swigValue(),
                SectionIds.E_SECTION_ENDFUNC.swigValue(),
                SectionIds.E_SECTION_RESTORE_TARGET.swigValue()};
    }
}
