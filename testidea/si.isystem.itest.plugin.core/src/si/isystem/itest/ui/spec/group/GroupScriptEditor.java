package si.isystem.itest.ui.spec.group;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestFunction.ESection;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestResultBase;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.run.Script;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.ScriptsSpecEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.ui.utils.KGUIBuilder;

public class GroupScriptEditor extends GroupSectionEditor {

    class ControlsForScript {
        TBControlText m_funcNameTBTxt;
        TBControlText m_paramsTBTxt;
        ValueAndCommentEditor m_tagEditor;
        StatusDecoration m_statusDec;
        
        void setEnabled(boolean isEnabled) {
            m_funcNameTBTxt.setEnabled(isEnabled);
            m_paramsTBTxt.setEnabled(isEnabled);
            m_tagEditor.setEnabled(isEnabled);
        }
        
        void clearInput() {
            m_funcNameTBTxt.clearInput();
            m_paramsTBTxt.clearInput();
            m_statusDec.setDescriptionText("", EStatusType.INFO);
        }

        public void setInput(ESectionCTestGroup section) {
            
            CTestBase funcTB = m_testGroup.getTestBase(section.swigValue(), false);
            
            m_funcNameTBTxt.setInput(funcTB, false);
            m_paramsTBTxt.setInput(funcTB, false);

            m_tagEditor.updateValueAndCommentFromTestBase(m_testGroup);
        }
    }
    
    
    private ControlsForScript m_initFuncCtls = new ControlsForScript();
    private ControlsForScript m_endFuncCtls = new ControlsForScript();
    
    
    public GroupScriptEditor() {
        super(ENodeId.GROUP_SCRIPT_NODE, ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT, 
                                         ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT);
    }

    @Override
    public Composite createPartControl(Composite parent) {
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        
        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);

        MigLayout mig = new MigLayout("fillx", "[fill]", "");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        // init func
        createControllsForScript(builder, 
                                 ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT, 
                                 m_initFuncCtls, 
                                 "Init group script function", 
                                 "Name of script function to be called before\n"
                                 + "execution of test cases starts.\n"
                                 + "This script is called before any test case is executed, not just before "
                                 + "test cases from a group start execution." 
                                 + "Example:\n"
                                 + "    myGroupInitFunc", 
                                 EHControlId.EScriptGroupInitFuncName, 
                                 SWTBotConstants.GRP_INIT_SCRIPT_FUNC, 
                                 null, 
                                 SWTBotConstants.GRP_INIT_SCRIPT_PARAMS);
        
        // end func
        createControllsForScript(builder, 
                                 ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT, 
                                 m_endFuncCtls, 
                                 "End group script function", 
                                 "Name of script function to be called after test case execution.\n"
                                 + "This script is called after all test cases are executed, not only test cases "
                                 + "from a group." 
                                 + "Example:\n" 
                                 + "    myGroupEndFunc", 
                                 EHControlId.EScriptGroupEndFuncName, 
                                 SWTBotConstants.GRP_END_SCRIPT_FUNC, 
                                 null, 
                                 SWTBotConstants.GRP_END_SCRIPT_PARAMS);
        
        // Use non-editable Text instead of Label, so users can select and copy name 
        // of the parameter. 
        Text hint = builder.text("gapleft 10, wrap");
        hint.setText("Hint: Use ' " + Script.RESERVED_TEST_GROUP_PARAM + 
                     " ' as name of the first parameter to pass");
        hint.setEditable(false);
        
        hint = builder.text("gapleft 10");
        hint.setText("test group specification to script function.");
        hint.setEditable(false);
        
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }

    
    private void createControllsForScript(KGUIBuilder builder, 
                                          ESectionCTestGroup sectionId,
                                          ControlsForScript ctrls,
                                          String groupTitle,
                                          
                                          String funcTooltip,
                                          EHControlId funcCtrlId, 
                                          String funcSwtBotId,
                                          
                                          EHControlId paramsCtrlId,
                                          String paramsSwtBotId) {
        
        KGUIBuilder initTargetBuilder = builder.group(groupTitle, 
                                                      "wmin 0, gaptop 20, wrap", 
                                                      new MigLayout("fillx", "[min!][fill][min!]"), SWT.NONE);
        initTargetBuilder.label("Script function:");
        
        ctrls.m_funcNameTBTxt = TBControlText.createForMixed(initTargetBuilder, 
                                               funcTooltip, 
                                               "gapleft 10, growx, wrap", 
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
                     + "Examples:\n"
                     + "    30, \"abc\"\n" 
                     + "    counter = 23, file = 'log.txt'\n"
                     + "It is also recommended to store description of script parameters into comment ('i' icon on the right).",
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
        boolean isEnabled = m_testGroup != null;
        m_initFuncCtls.setEnabled(isEnabled);
        m_endFuncCtls.setEnabled(isEnabled);
    
        if (m_testGroup == null) {

            m_initFuncCtls.clearInput();
            m_endFuncCtls.clearInput();
            
            return;            
        }

        m_initFuncCtls.setInput(ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT);
        m_endFuncCtls.setInput(ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT);
        

        CTestGroupResult result = m_model.getGroupResult(m_testGroup);
        ScriptsSpecEditor.setStatusText(m_initFuncCtls.m_statusDec, result, CTestResultBase.getSE_GROUP_INIT_FUNC());
        ScriptsSpecEditor.setStatusText(m_endFuncCtls.m_statusDec, result, CTestResultBase.getSE_GROUP_END_FUNC());
    }
    

    @Override
    public boolean isError(CTestGroupResult result) {
        return result.isScriptError();  
    }
    
    
    @Override
    public boolean hasErrorStatus() {
        return !isEmpty();
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{ESectionCTestGroup.E_SECTION_GROUP_INIT_SCRIPT.swigValue(),
                ESectionCTestGroup.E_SECTION_GROUP_END_SCRIPT.swigValue()};
    }
}
