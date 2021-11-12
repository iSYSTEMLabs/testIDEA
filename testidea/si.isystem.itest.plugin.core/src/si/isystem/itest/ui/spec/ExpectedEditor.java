package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CStackUsageResult;
import si.isystem.connect.CTestAssert.ESectionAssert;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStackUsage.ETestStackUsageSections;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.itest.wizards.newtest.NewTCExpressionsPage;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class ExpectedEditor extends SectionEditorAdapter {

    private ExpressionsPanel m_exprPanel;
    
    protected TBControlTristateCheckBox m_isInheritStackTB;
    private TBControlText m_stackUsageTBControl;
    private StatusDecoration m_stackUsageStatusDec;
    private Text m_measuredStackUsageTxt;

    private TBControlTristateCheckBox m_isExcpectTargetException;

    
    ExpectedEditor() {
        super(ENodeId.EXPECTED_NODE, SectionIds.E_SECTION_ASSERT, SectionIds.E_SECTION_STACK_USAGE);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);

        MigLayout mig = new MigLayout("fill", 
                                      "[min!][min!][min!][min!][fill]",
                                      "[min!][min!][min!][fill][min!]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        builder.label("Max stack used:");
        
        m_stackUsageTBControl = TBControlText.createForMixed(builder,
                                                  "Maximum amount of stack to be used. If this value is not empty, make sure \n" +
                                                       "that stack usage monitoring is configured in File | Properties | Stack usage.\n" +
                                                       "If more stack is used than specified here, test fails. Actual value is shown in test report.\n" +
                                                       "Example:    100",
                                                       "w 100::, gapleft 3",              
                                                  ETestStackUsageSections.E_SECTION_MAX_SIZE.swigValue(),
                                                  m_nodeId,
                                                  EHControlId.EMaxStackUsage, 
                                                  SWT.BORDER);
        
        m_stackUsageStatusDec = new StatusDecoration(m_stackUsageTBControl.getControl(),
                                                     SWT.LEFT | SWT.BOTTOM);

        m_isInheritStackTB = createTristateInheritanceButton(builder, "gapleft 10, gapright 30");
        m_isInheritStackTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_STACK_USAGE));

        builder.label("Measured stack usage:");
        m_measuredStackUsageTxt = builder.text("w 100::, gapleft 3, wrap", SWT.BORDER);
        m_measuredStackUsageTxt.setEditable(false);
        UiTools.setToolTip(m_measuredStackUsageTxt, "Actual value measured during test run. Please make sure that stack usage\n" +
                                                    "monitoring is configured in File | Properties | Stack usage.\n" +
                                                    "The three numbers have the following meaning:\n"
                                                    + "b - usage before test\n"
                                                    + "tI - used by testIDEA for test local variables and call stack\n"
                                                    + "app - used by application code on target during run.\n");
        
        builder.separator("span 5, gaptop 10, gapbottom 10, growx, wrap", SWT.HORIZONTAL);
        
        m_isExcpectTargetException = 
                new TBControlTristateCheckBox(builder, 
                                              "Expect target exception", 
                                              "checked - exception thrown during test on target is a valid test output\n"
                                              + "unchecked - exception should not be thrown on target\n"
                                              + "intermediate - default, same as unchecked, no entry in YAML file.\n\n"
                                              + "Exception variable, which can be used for exception verification is 'isystem_test_exception'.\n"
                                              + "For example, when exception of type 'int' is thrown, we can write expr. 'isystem_test_exception == 23'\n"
                                              + "into the table below.", 
                                              "span 2, gapleft 7", 
                                              ESectionAssert.E_SECTION_ASSERT_IS_EXPECT_EXCEPTION.swigValue(), 
                                              ENodeId.EXPECTED_NODE, 
                                              null);
        
        createWizardBtn(builder, 
            "gaptop 5, gapright 7, w 0:pref:pref, skip 2, split 2, gapleft push, alignright",
            "Opens wizard for adding output parameters, return value and global\n"
            + "variables modified in function under test and called functions.",
            ENodeId.EXPECTED_NODE,
            new NewTCExpressionsPage(null));
        
        TBControlTristateCheckBox m_inheritBtn = createTristateInheritanceButton(builder, 
                                                   "gapleft 3, w 0:pref:pref, alignright, wrap");

        m_exprPanel = new ExpressionsPanel(builder.getParent(),
                                           "span 5, gaptop 10, wmin 0, hmin 120, growx, wrap",
                                           SWTBotConstants.BOT_EXPECTED_EXPR_TABLE,
                                           ENodeId.EXPECTED_NODE,
                                           m_inheritBtn,
                                           new InheritedActionProvider(SectionIds.E_SECTION_ASSERT));
        
        m_exprPanel.setTooltip("Enter expressions, which must return true for test to succeed.\n" +
                "Examples:\n    retVal == 0\n    g_counter < 1020  &&&&  g_counter > 910\n" +
                "To specify format, append '@@ <format specifier>', for example:\n" +
                "    rv == 4  ||  g_cnt == 300  ||  i < 2 @@ d h b\n" +
                "writes 'rv' in decimal, 'g_cnt' in hex format and 'i' in binary format.\n" +
                "Use prefix '@' for registers, for example: @R1 ==42\n" +
                "Use prefix '`' (backquote) for IO module ports, for example: `DigitalIn.DIN0 == 1\n");
        
        return getScrollableParent(mainPanel);
    }


    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testSpec != null;
        m_stackUsageTBControl.setEnabled(isEnabled);
        
        if (!isEnabled) {
            setInputForInheritCb(null, m_isInheritStackTB);
            setInputForInheritCb(null, m_exprPanel.getInheritExprTB());

            m_stackUsageTBControl.clearInput();
            m_isExcpectTargetException.clearInput();
            m_exprPanel.setInput(null, null, false, false);
            m_stackUsageStatusDec.setDescriptionText("", EStatusType.INFO);

            return;            
        }

        setInputForInheritCb(SectionIds.E_SECTION_STACK_USAGE, m_isInheritStackTB);
        setInputForInheritCb(SectionIds.E_SECTION_ASSERT, m_exprPanel.getInheritExprTB());
        
        setCurrentTS(SectionIds.E_SECTION_STACK_USAGE);
        m_stackUsageTBControl.setInput(m_currentTestSpec.getStackUsage(false),
                                       m_isInherited);
        m_isExcpectTargetException.setInput(m_currentTestSpec.getAssert(false), 
                                            m_isInherited);
        
        m_exprPanel.setVarsContentProposals(m_testSpec, m_currentCoreId);
        
        setCurrentTS(SectionIds.E_SECTION_ASSERT);
        m_exprPanel.setInput(m_testSpec,
                             m_currentTestSpec.getAssert(false),
                             true,
                             m_isInherited);
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestResult result = model.getResult(m_testSpec);
        setStackUsageResult(result, !m_testSpec.getStackUsage(true).isEmpty());
        
        m_exprPanel.setCoreId(m_currentCoreId);
    }


    @Override
    public boolean isError(CTestResult result) {
        return result.isExpressionError()  ||  result.isStackUsageError();
    }

    @Override
    public boolean hasErrorStatus() {
        return !isEmpty()  ||  isMerged();
    }
    
    
    
    
    private void setStackUsageResult(CTestResult result, boolean isStackUsageDefinedInTestSpec) {
        
        if (result != null) {
            
            CStackUsageResult stackUsageResult = result.getStackUsageResult(true);
            
            if (isStackUsageDefinedInTestSpec) {
                if (stackUsageResult.isError()) {
                    m_stackUsageStatusDec.setDescriptionText(stackUsageResult.toString(),
                                                             EStatusType.ERROR);
                } else {
                    m_stackUsageStatusDec.setDescriptionText(stackUsageResult.toString(),
                                                             EStatusType.INFO);
                }
            } else {
                m_stackUsageStatusDec.setDescriptionText("", EStatusType.INFO);
            }
            
            if (!stackUsageResult.isEmpty()) {
                m_measuredStackUsageTxt.setText("b: " + stackUsageResult.getUsageBeforeTest() + 
                                                ",  tI: " + stackUsageResult.getTestIDEAUsage() +
                                                ",  app: " + stackUsageResult.getApplicationUsage());
            } else {
                m_measuredStackUsageTxt.setText("");
            }
        } else {
            m_stackUsageStatusDec.setDescriptionText("", EStatusType.INFO);
            m_measuredStackUsageTxt.setText("");
        }
    }


    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        m_exprPanel.selectLineInTable(tableId, lineNo);
    }


    public void refreshGlobals(String coreId) {
        
        if (m_exprPanel == null) {
            return; // does not exist yet because of lazy init.
        }
        
        m_exprPanel.refreshGlobals(m_currentTestSpec, coreId);
    }    


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestSpecification.SectionIds.E_SECTION_ASSERT.swigValue(),
                         CTestSpecification.SectionIds.E_SECTION_STACK_USAGE.swigValue()};
    }
}
