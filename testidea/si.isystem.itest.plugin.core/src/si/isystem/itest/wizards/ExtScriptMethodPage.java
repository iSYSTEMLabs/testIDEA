package si.isystem.itest.wizards;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.run.Script;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class implements one page for Script extensions wizard. This page enables 
 * user to create one script method.
 * 
 * @author markok
 */
public class ExtScriptMethodPage extends WizardPage implements IExtScriptPage {

    protected static final String VAR_TEST_ID = "${testID}";
    protected static final String VAR_FUNC_NAME = "${funcName}";
    
    private static final String PREFS_IS_CREATE = "isCreate";
    private static final String PREFS_METHOD_NAME = "methodName";
    private static final String PREFS_IS_GEN_METHOD_NAME = "isGenMethodName";
    private static final String PREFS_IS_USE_TEST_ID = "isUseTestId";
    private static final String PREFS_IS_USE_FUNC_NAME = "isUseFuncName";
    private static final String PREFS_IS_EVAL = "isEval";
    private static final String PREFS_IS_MODIFY = "isModify";
    private static final String PREFS_IS_COUNT_CALLS = "isCountCalls";
    private static final String PREFS_IS_PASS_TEST_SPEC = "isPassTestSpec";
    
    protected final static String DEFAULT_INIT_TARGET_SCRIPT_METHOD_NAME = "initTarget";
    protected final static String DEFAULT_INIT_TEST_SCRIPT_METHOD_NAME = "initTest";
    protected final static String DEFAULT_END_TEST_SCRIPT_METHOD_NAME = "endTest";
    protected final static String DEFAULT_RESTORE_TARGET_SCRIPT_METHOD_NAME = "restoreTarget";
    
    private String m_selectedTestCaseId;
    private String m_stubbedFuncNameOrTpId;
    private CTestBaseList m_stubOrTpSteps;
    private String m_methodPostFix;
    private CTestSpecification m_sampleTestSpec; // this test spec is used only 
    private String[] m_stubAndTpCounterNames;
    private int m_stubOrTpIndex;
    private boolean m_isShowOpenScriptControl;
    
    private Button m_isCreateMethodCb;
    private Text m_methodNameTxt;
    private Button m_isGenerateMethodNameCb;
    private Button m_isUseTestIdCb;
    private Button m_isUseFuncNameCb;

    private Button m_isEvalCb;
    private Button m_isModifyCb;
    private Button m_isCountCallsCb;
    private Button m_isPassTestSpecCb;
    
    private Text m_previewTxt;
    private Label m_methodNameLbl;
                                                 // to generate sample methods
    private boolean m_isInitTestScript = false;
    private boolean m_isInitTargetScript = false;
    private String m_reservedVarName;

    protected ExtScriptMethodPage(String selectedTestCaseId,
                                  CTestSpecification testSpec,
                                  String methodPostFix,
                                  String reservedVarName,
                                  String description,
                                  boolean isShowOpenScriptControl) {
        
        this(selectedTestCaseId, null, testSpec, null, null, -1, methodPostFix, 
             reservedVarName, description, isShowOpenScriptControl);
    }
    
    
    protected ExtScriptMethodPage(String selectedTestCaseId,
                                  String stubbedFuncNameOrTpId,
                                  CTestSpecification testSpec,
                                  CTestBaseList stubOrTpSteps,
                                  String[] stubAndTpCounterNames,
                                  int stubOrTpIndex,
                                  String methodPostFix,
                                  String reservedVarName,
                                  String description,
                                  boolean isShowOpenScriptControl) {
        super("Create script method");
        setTitle("Script Extension Method - " + methodPostFix);
        setDescription(description);
        
        // setControl(newControl);
        m_sampleTestSpec = testSpec;
        m_selectedTestCaseId = selectedTestCaseId;
        m_stubbedFuncNameOrTpId = stubbedFuncNameOrTpId;
        m_stubOrTpSteps = stubOrTpSteps;
        m_methodPostFix = methodPostFix;
        m_reservedVarName = reservedVarName;

        m_isShowOpenScriptControl = isShowOpenScriptControl;
        m_stubAndTpCounterNames = stubAndTpCounterNames;
        
        // stub indices start from 0, test point indices start after last stub index.
        m_stubOrTpIndex = stubOrTpIndex;
        
        if (m_stubOrTpSteps == null  &&  stubAndTpCounterNames != null) {
            m_isInitTestScript  = true;
        }
        
        m_isInitTargetScript = methodPostFix.equals(DEFAULT_INIT_TARGET_SCRIPT_METHOD_NAME);
    }

    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        container.setLayout(new MigLayout("fillx", "[min!][fill][min!]"));

        KGUIBuilder builder = new KGUIBuilder(container);

        builder.label("Selected test cases:");
        Label lbl = builder.label(m_selectedTestCaseId, "wrap", SWT.BORDER);
        if (m_selectedTestCaseId.isEmpty()) {
            lbl.setFont(FontProvider.instance().getItalicControlFont(lbl));
            lbl.setText("No test cases are selected in Test Tree");
        }

        
        builder.separator("span 3, growx, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);

        // I've decided for this warning instead of automatic activation of
        // controls, because this increased code complexity much more than it was 
        // the benefit to the customer - probability for bugs was to high. MK, Sept.2012
        // One of the problems was if user added and then removed counters, but testInit
        // method was already enabled - to disable it or not? What about if the user 
        // manually selected it without adding any contents?  
        // Search for code with commented 'm_isTestInitScript' for enabling components. 
        if (m_isInitTestScript) {
            builder.label("This method may be created even when not explicitly selected here, " + 
                          "if stub script methods will contain call counters.", 
                          "gapbottom 15, span 3, wrap");
        }
        
        m_isCreateMethodCb = builder.checkBox("Create script method", "gapbottom 15, wrap");
        // m_isCreateMethodCb.setFont(FontProvider.instance().getBoldControlFont(m_isCreateMethodCb));
        
        KGUIBuilder methodNameGrp = builder.group("Script method name", 
                                                  "span 3, grow x, wrap", true,
                                                  "fillx", "[min!][fill][min!]", "");
        m_methodNameLbl = methodNameGrp.label("Method name:", "");
        m_methodNameTxt = methodNameGrp.text("wrap", SWT.BORDER);
        UiTools.setToolTip(m_methodNameTxt, "Enter name of the script method. Use variables ${testID} and ${funcName} for \n" +
                                            "automatic method name generation for each of the selected test cases.");

        m_isGenerateMethodNameCb = methodNameGrp.checkBox("Generate method name", "skip, wrap");
        UiTools.setToolTip(m_isGenerateMethodNameCb, "Generates script method name according to state of the above checkboxes.");
        m_isUseTestIdCb = methodNameGrp.checkBox("Use test ID in script method name", "skip, gapleft 15, wrap");
        m_isUseFuncNameCb = methodNameGrp.checkBox("Use function name in script method name", "skip, gapleft 15, wrap");

        
        KGUIBuilder methodFunctionalityGrp = builder.group("Method functionality", 
                                                           "span 3, grow x, gaptop 15, wrap",
                                                           true, "fillx", "[min!]", "");

        m_isPassTestSpecCb = methodFunctionalityGrp.checkBox("Pass test specification as parameter", 
                                                            "span 2, wrap");
        UiTools.setToolTip(m_isPassTestSpecCb, "If checked, then the current test specification is given " +
                                               "as parameter to test method. It is object of type " +
                                               "CTestSpecification.");
        m_isEvalCb = methodFunctionalityGrp.checkBox("Evaluate variables, parameters, registers, ...",
                                                    "span 2, wrap");
        UiTools.setToolTip(m_isEvalCb, "If checked, few lineas are added to the method, " + 
                                       "which demonstrate how to access target variables.");

        m_isModifyCb = methodFunctionalityGrp.checkBox("Modify variables, parameters, registers, ...",
                                                      "span 2, wrap");
        UiTools.setToolTip(m_isEvalCb, "If checked, few lineas are added to the method, " + 
                                       "which demonstrate how to modify target variables.");
        
        if (m_stubOrTpSteps != null) {
            m_isCountCallsCb = methodFunctionalityGrp.checkBox("Count calls", "span 2, wrap");
            UiTools.setToolTip(m_isCountCallsCb, "If checked, few lineas are added to the method, " + 
                    "which demonstrate how to count calls of the stub script method.\n" +
                    "The script function action may depend on call number. Counter initialization is\n" +
                    "added to the test init script method.");
        }


        builder.label("The generated method", "gaptop 15, wrap");
        m_previewTxt = builder.text("span 3, grow, pushy, w 300::, h 100:100:, wrap", 
                                    SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | 
                                    SWT.V_SCROLL);
        m_previewTxt.setEditable(false);
        m_previewTxt.setFont(FontProvider.instance().getFixedWidthControlFont(m_previewTxt));
        UiTools.setToolTip(m_previewTxt, "This area contains preview of the method, which will be generated.");

        if (m_isShowOpenScriptControl) { // stub method is the last page
            Button openEditorBtn = builder.checkBox("Open script in editor after the 'Finish' button is pressed", "gaptop 10, wrap");
            UiTools.setToolTip(openEditorBtn, "Opens the existing script in external editor.");
        }
        
        // listeners
        m_isCreateMethodCb.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCreateMethod = m_isCreateMethodCb.getSelection();
                
                /* if (!isCreateMethod  &&  m_isTestInitScript  &&  !m_stubCounters.isEmpty()) {
                    setDescription("This method MUST be created, because stub call counter(s) must be initialized there.\n");
                    m_isCreateMethodCb.setSelection(true);
                    isCreateMethod = true;
                } */

                if (!isCreateMethod  &&  m_stubOrTpSteps != null) {
                    // remove counter if method is not to be created
                    m_stubAndTpCounterNames[m_stubOrTpIndex] = null;
                }
                enableControls(isCreateMethod);
                generateSampleMethod();
                setPageComplete(isMyPageComplete());
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        
        m_methodNameTxt.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                generateSampleMethod();
                setPageComplete(isMyPageComplete());
                if (m_isCountCallsCb != null  &&  m_isCountCallsCb.getSelection()) {
                    m_stubAndTpCounterNames[m_stubOrTpIndex] = getCallCounterName();
                }
            }
        });
        
        
        SelectionListener generateMethodNameListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isGenCbEnabled = m_isGenerateMethodNameCb.getSelection();
                m_isUseTestIdCb.setEnabled(isGenCbEnabled );
                m_isUseFuncNameCb.setEnabled(isGenCbEnabled);
                if (isGenCbEnabled) {
                    generateGenericScriptMethodName();
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        };
        
        m_isGenerateMethodNameCb.addSelectionListener(generateMethodNameListener);
        m_isUseTestIdCb.addSelectionListener(generateMethodNameListener);
        m_isUseFuncNameCb.addSelectionListener(generateMethodNameListener);
        SelectionListener generatePreviewMethodListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                generateSampleMethod();
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        };
        
        m_isEvalCb.addSelectionListener(generatePreviewMethodListener);
        m_isModifyCb.addSelectionListener(generatePreviewMethodListener);
        m_isPassTestSpecCb.addSelectionListener(generatePreviewMethodListener);
        
        if (m_isCountCallsCb != null) {
            m_isCountCallsCb.addSelectionListener(generatePreviewMethodListener);

            m_isCountCallsCb.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    String counterName = getCallCounterName();
                    if (m_isCountCallsCb.getSelection()) {
                        m_stubAndTpCounterNames[m_stubOrTpIndex] = counterName;
                    } else {
                        m_stubAndTpCounterNames[m_stubOrTpIndex] = null;
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }

        initControls();
        
        enableControls(m_isCreateMethodCb.getSelection());
        
        generateSampleMethod();
        
        setControl(container);
        setPageComplete(isMyPageComplete());
    }


    private void initControls() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        
        String prefix = "itestScriptExt." + m_methodPostFix + ".";
        
        m_isCreateMethodCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_CREATE));
        m_methodNameTxt.setText(prefs.getString(prefix + PREFS_METHOD_NAME));
        m_isGenerateMethodNameCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_GEN_METHOD_NAME));
        m_isUseTestIdCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_USE_TEST_ID));
        m_isUseFuncNameCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_USE_FUNC_NAME));

        m_isEvalCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_EVAL));
        m_isModifyCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_MODIFY));
        if (m_isCountCallsCb != null) {
            m_isCountCallsCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_COUNT_CALLS));
            if (m_isCountCallsCb.getSelection()) {
                m_stubAndTpCounterNames[m_stubOrTpIndex] = getCallCounterName();
            } else {
                m_stubAndTpCounterNames[m_stubOrTpIndex] = null;
            }
        }
        m_isPassTestSpecCb.setSelection(prefs.getBoolean(prefix + PREFS_IS_PASS_TEST_SPEC));
        
    }


    @Override
    // called whenever wizard page is switched to update state of the testInint
    // page regarding stub call counters
    public boolean canFlipToNextPage() {
        // update testInit page only, if selected by the user
        if (m_isInitTestScript  &&  m_isCreateMethodCb.getSelection()) {
            generateSampleMethod();
        }
        
        return super.canFlipToNextPage();
    }

    
    public void saveToPreferences() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        
        // this postfix is a kind of memory leak for stubs, because stub name is
        // part of postfix. Assuming there is less than 1k used by each stub,
        // then 1000 defined stubs use less than 1M of disk space, which can be
        // ignored, even if everything is loaded to RAM.
        String prefix = "itestScriptExt." + m_methodPostFix + ".";
        
        prefs.setValue(prefix + PREFS_IS_CREATE, m_isCreateMethodCb.getSelection());
        prefs.setValue(prefix + PREFS_METHOD_NAME, m_methodNameTxt.getText());
        prefs.setValue(prefix + PREFS_IS_GEN_METHOD_NAME, m_isGenerateMethodNameCb.getSelection());
        prefs.setValue(prefix + PREFS_IS_USE_TEST_ID, m_isUseTestIdCb.getSelection());
        prefs.setValue(prefix + PREFS_IS_USE_FUNC_NAME, m_isUseFuncNameCb.getSelection());

        prefs.setValue(prefix + PREFS_IS_EVAL, m_isEvalCb.getSelection());
        prefs.setValue(prefix + PREFS_IS_MODIFY, m_isModifyCb.getSelection());
        if (m_isCountCallsCb != null) {
            prefs.setValue(prefix + PREFS_IS_COUNT_CALLS, m_isCountCallsCb.getSelection());
        }
        prefs.setValue(prefix + PREFS_IS_PASS_TEST_SPEC, m_isPassTestSpecCb.getSelection());
    }

    
    private boolean isMyPageComplete() {
        return !m_isCreateMethodCb.getSelection() ||
               !m_methodNameTxt.getText().trim().isEmpty();
    }
    
    
    private void enableControls(boolean isEnabled) {
        m_methodNameLbl.setEnabled(isEnabled);
        m_methodNameTxt.setEnabled(isEnabled);
        m_isGenerateMethodNameCb.setEnabled(isEnabled);
        m_isUseTestIdCb.setEnabled(isEnabled);
        m_isUseFuncNameCb.setEnabled(isEnabled);

        m_isEvalCb.setEnabled(isEnabled);
        m_isModifyCb.setEnabled(isEnabled);
        
        if (m_isCountCallsCb != null) {
            m_isCountCallsCb.setEnabled(isEnabled);
        }
        m_isPassTestSpecCb.setEnabled(isEnabled);
        m_previewTxt.setEnabled(isEnabled);
    }

    
    private void generateSampleMethod() {
        String methodCode = generateScriptMethod(m_sampleTestSpec);
        m_previewTxt.setText(methodCode);
    }


    @Override
    public String generateScriptMethod(CTestSpecification testSpec) {

        String methodName = generateScriptMethodName(testSpec);
        
        StringBuilder counterInitSb = new StringBuilder();
        // initTest method may be created even if not selected, if counters for
        // script ext. methods must be initialized
        if (m_isInitTestScript) {
            for (String counterName : m_stubAndTpCounterNames) {
                if (counterName != null) {
                    counterInitSb.append("        ").append(counterName).append(" = 0\n");
                }
            }
            
            if (counterInitSb.length() > 0) {
                counterInitSb.append("\n");
            }
        }
        
        
        if (!m_isCreateMethodCb.getSelection()  &&  counterInitSb.length() == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("    def ").append(methodName).append("(self");
        if (m_isPassTestSpecCb.getSelection()) {
            sb.append(", testSpec");
        } 
            
        // add stub parameters with generic names - the same number as param
        // values specified in testIDEA stub script params field
        if (m_stubOrTpSteps != null) {
            int numSteps = (int) m_stubOrTpSteps.size();
            // get max number of parameters in all steps
            long maxParams = 0;
            for (int stepIdx = 0; stepIdx < numSteps; stepIdx++) {
                CTestEvalAssignStep step = CTestEvalAssignStep.cast(m_stubOrTpSteps.get(stepIdx));  

                CSequenceAdapter assignments = 
                        new CSequenceAdapter(step, 
                                             CTestEvalAssignStep.EStepSectionIds.E_SECTION_SCRIPT_PARAMS.swigValue(), 
                                             false);

                if (m_isPassTestSpecCb.getSelection()) {
                    StrVector stubScriptParams = new StrVector();
                    assignments.getStrVector(stubScriptParams);
                    
                    if (stubScriptParams.size() > 0) {
                        String param0 = stubScriptParams.get(0);
                        // If the first parameter is test specification, than the number
                        // of remaining params with generic names is one less than 
                        // the number of all params
                        if (param0.equals(Script.RESERVED_TEST_SPEC_PARAM)) {
                            maxParams = Math.max(assignments.size() - 1, maxParams);
                        } else {
                            // Script.RESERVED_TEST_SPEC_PARAM will be inserted 
                            // to the list of params by Action 
                            maxParams = Math.max(assignments.size(), maxParams);
                        }
                    }
                } else {
                    maxParams = Math.max(assignments.size(), maxParams);
                }
            }

            for (int i = 0; i < maxParams; i++) {
                sb.append(", ").append("param").append(i);
            }
        }
            
        sb.append("):\n\n");
        
        if (m_isPassTestSpecCb.getSelection()) {
            sb.append("        print('Test case ID: ', testSpec.getTestId())\n\n");
        }

        sb.append(counterInitSb);
        
        if (m_isEvalCb.getSelection()  ||  m_isModifyCb.getSelection()) {
            if (!m_isInitTargetScript) {  // test case controller is not available yet 
                sb.append("        self.testCtrl = self.__getTestCaseCtrl()\n\n");
            }

        }
        
        if (m_isEvalCb.getSelection()) {
            sb.append("        # examples for variable evaluation\n");
            if (m_isInitTargetScript) {
                sb.append("        varAsString = self.debug.evaluate(ic.IConnectDebug.fMonitor, '<enter your var name here>').getResult()\n");
                sb.append("        varAsInt = self.debug.evaluate(ic.IConnectDebug.fMonitor, '<enter your int var name here>,d').getInt()\n\n");
            } else {
                sb.append("        varAsString = self.testCtrl.evaluate('<enter your var name here>')\n");
                sb.append("        varAsInt = int(self.testCtrl.evaluate('<enter your int var name here>,d'))\n\n");
            }
        }
        
        if (m_isModifyCb.getSelection()) {
            sb.append("        # examples for variable modification\n");
            if (m_isInitTargetScript) {
                sb.append("        self.debug.modify(ic.IConnectDebug.fMonitor, '<enter your var name here>', '<enter new value here>')\n");
                sb.append("        self.debug.modify(ic.IConnectDebug.fMonitor, 'iCounter', '42')\n\n");
            } else {
                sb.append("        self.testCtrl.modify('<enter your var name here>', '<enter new value here>')\n");
                sb.append("        self.testCtrl.modify('iCounter', '42')\n\n");
            }
        }

        if (m_isCountCallsCb != null  &&  m_isCountCallsCb.getSelection()) {
            sb.append("        # increment stub call counter\n");
            sb.append("        ").append(getCallCounterName()).append(" += 1\n\n");
        }
        
        sb.append("        ").append(m_reservedVarName).append(" = 'This text will appear in testIDEA Status view AND test report'\n\n");

        sb.append("        print('This text will appear in testIDEA Status view but NOT in test report')\n\n");

        sb.append("        return None  # in case of error return error description string\n\n\n");

        return sb.toString();
    }


    // generates method names like: ${testID}_${funcName}_testInit
    private void generateGenericScriptMethodName() {
        StringBuilder sb = new StringBuilder();
        if (m_isUseTestIdCb.getSelection()) {
            sb.append(VAR_TEST_ID);
        }
        if (m_isUseFuncNameCb.getSelection()) {
            if (sb.length() > 0) {
                sb.append("__"); // insert underscore to separate test ID from func name 
            }
            sb.append(VAR_FUNC_NAME);
        }
        
        if (sb.length() > 0) {
            sb.append("__"); // insert underscore to separate 
        }
        sb.append(UiUtils.replaceNonAlphanumChars(m_methodPostFix));
        m_methodNameTxt.setText(sb.toString());
    }


    // generates method names like: test01_myFunc_testInit
    @Override
    public String generateScriptMethodName(CTestSpecification testSpec) {
        
        String testCaseId = "";
        String cmethodName = "";
        
        if (testSpec != null) {
            CTestSpecification mergedTS = testSpec.merge(); 
            testCaseId = mergedTS.getTestId();
            cmethodName = mergedTS.getFunctionUnderTest(true).getName();
        }
        
        String pattern = m_methodNameTxt.getText().trim();
        String methodName = pattern.replace(VAR_TEST_ID, testCaseId)
                                   .replace(VAR_FUNC_NAME, cmethodName);
        methodName = UiUtils.replaceNonAlphanumChars(methodName);


        if (methodName.isEmpty()  &&  isCreateInitTestScript()) {
            methodName = testCaseId + "__" + cmethodName + "__" + m_methodPostFix;
            methodName = UiUtils.replaceNonAlphanumChars(methodName);
        }
        
        return methodName;
    }
    

    private String getCallCounterName() {
        return "self.__counterFor_" + 
                             generateScriptMethodName(m_sampleTestSpec);
    }


    public boolean isCreateMethod() {
        return m_isCreateMethodCb.getSelection()  ||  isCreateInitTestScript();
    }


    // initTest script must be created also when there are stub counters to be 
    // initialized
    private boolean isCreateInitTestScript() {
        boolean isCounter = false;
        
        if (m_stubAndTpCounterNames != null) {
            for (String counterName : m_stubAndTpCounterNames) {
                if (counterName != null) {
                    isCounter = true;
                    break;
                }
            }
        }
        return isCounter  &&  m_isInitTestScript;
    }


    public String getMethodName() {
        return m_methodNameTxt.getText();
    }


    public String getStubbedFuncNameOrTpId() {
        return m_stubbedFuncNameOrTpId;
    }
    

    public CTestBaseList getAssignmentSteps() {
        return m_stubOrTpSteps;
    }
    
    
    public boolean isPassTestSpecAsParam() {
        return m_isPassTestSpecCb.getSelection();
    }
}
