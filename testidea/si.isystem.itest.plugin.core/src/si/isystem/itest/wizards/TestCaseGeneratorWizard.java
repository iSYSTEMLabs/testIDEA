package si.isystem.itest.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.EOpenMode;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestDryRun;
import si.isystem.connect.CTestDryRun.EDryRunSectionIds;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestPoint.ETestPointSections;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestStub.EStubSectionIds;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.main.Activator;
import si.isystem.itest.wizards.TCGenIdentifiersPage.TCGenPageType;
import si.isystem.ui.utils.SelectionAdapter;

/**
 * Design decisions
 * There were two possibilities concerned about persistence of generator configuration:
 * 1. Create model in C++ with CTestBase classes (at least 8 new classes for all 
 *    sections would be needed). 
 *    Pros: 
 *    - could use DynamicTable in GUI for Occurs table
 *    - could easily save generator data to iyaml file and report, which would be
 *      user friendly
 *    Cons:
 *    - generator data in no way guarantees that derived set of tests matches
 *      the generator data. User may later add (also with generator) or delete
 *      groups of test cases, modify values, ... So this data may be more misleading
 *      than helpful
 *    - SDK API would contain classes not useful in scripting (better methods for 
 *      test generation exist in scripts). 
 *     
 * 2. Create model in Java
 *    Pros:
 *    - faster implementation with less overhead.
 *   
 * I've decided for Java model, and to eventually implement reverse engineering 
 * algorithm, which would fill generator model out of existing test cases. May 
 * not be exact, but definitely
 * more reliable than some old generator config., which was used to add the last 
 * 10 test cases perhaps.
 * 
 * Note: All classes except this one are private to this package, but are 
 * declared as public because of accessibility from SWTBot. 
 * 
 * @author markok
 *
 */
public class TestCaseGeneratorWizard extends Wizard {

    
    private static final String SEC_NAME_FUNCTION = "Function";
    private static final String SEC_NAME_VARIABLES = "Variables";
    private static final String SEC_NAME_STUBS = "Stubs";
    private static final String SEC_NAME_TEST_POINTS = "Test Points";
    private static final String SEC_NAME_HIL = "HIL";
    private static final String SEC_NAME_OPTIONS = "Options";
    private static final String SEC_NAME_INIT_TARGET_SCRIPT = "Init Target Script";
    private static final String SEC_NAME_INIT_FUNCTION_SCRIPT = "Init Function Script";
    private static final String SEC_NAME_END_FUNCTION_SCRIPT = "End Function Script";
    private static final String SEC_NAME_RESTORE_TARGET_SCRIPT = "Restore Target Script";
    
    private CTestSpecification m_selectedTestSpec; 
    private CTestSpecification m_mergedTestSpec;
    
    private static TCGenDataModel m_dataModel = new TCGenDataModel();
    private static boolean m_isAskForClearConfirmation = true;

    private TCGenIdentifiersPage m_funcParamsPage;
    private TCGenIdentifiersPage m_varsPage;
    private TCGenIdentifiersPage m_stubsPage;
    private TCGenIdentifiersPage m_testPointsPage;
    private TCGenIdentifiersPage m_hilPage;
    private TCGenIdentifiersPage m_optionsPage;
    private TCGenIdentifiersPage m_initTargetScriptPage;
    private TCGenIdentifiersPage m_initFuncScriptPage;
    private TCGenIdentifiersPage m_endFuncScriptPage;
    private TCGenIdentifiersPage m_restoreTargetScriptPage;
    private TCGenIdentifiersPage m_testCasePage;
    private TCGen_PreCond_Stub_TP_ExpressionsPage m_assertPreCondStubsTPPage;
    private TCGenDryRunPage m_dryRunPage;
    
    private List<TCGenIdentifiersPage> m_pages = new ArrayList<>();
    private String m_coreId;
    private String[] m_funcAutoCompleteProposals = new String[0];

    private List<CTestSpecification> m_generatedTestCases = new ArrayList<>();
    
    static SelectionAdapter m_resetButtonSelectionListener;


    public TestCaseGeneratorWizard(CTestSpecification selectedTestSpec, 
                                   CTestSpecification mergedTestSpec, 
                                   String coreId) {
        super();
        setNeedsProgressMonitor(true);

        setWindowTitle("Test Case Generator Wizard");

        m_selectedTestSpec = selectedTestSpec;
        m_mergedTestSpec = mergedTestSpec;
        m_coreId = coreId;
        
        m_resetButtonSelectionListener = new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isClear = true;
                
                if (m_isAskForClearConfirmation) {
                    MessageDialogWithToggle dialog = 
                            MessageDialogWithToggle.openOkCancelConfirm(getShell(), 
                                                                        "Clear wizard data", 
                                                                        "Do your really want to clear the wizard?",
                                                                        "Do not show this dialog in this session",
                                                                        !m_isAskForClearConfirmation,
                                                                        null, null);
                    isClear = dialog.getReturnCode() == Window.OK;
                    if (isClear) {
                        m_isAskForClearConfirmation = !dialog.getToggleState();
                    }
                }
                
                if (isClear) {
                    m_dataModel = new TCGenDataModel();
                    initFunctionSection();
                    m_funcParamsPage.setNewData(m_dataModel.getFunctionSection());
                    m_varsPage.setNewData(m_dataModel.getVarsSection());
                    m_stubsPage.setNewData(m_dataModel.getStubsSection());
                    m_testPointsPage.setNewData(m_dataModel.getTestPointsSection());
                    m_hilPage.setNewData(m_dataModel.getHil());
                    m_optionsPage.setNewData(m_dataModel.getOptions());
                    m_initTargetScriptPage.setNewData(m_dataModel.getInitTargetScript());
                    m_initFuncScriptPage.setNewData(m_dataModel.getInitFunctionScript());
                    m_endFuncScriptPage.setNewData(m_dataModel.getEndFunctionScript());
                    m_restoreTargetScriptPage.setNewData(m_dataModel.getRestoreTargetScript());
                    m_testCasePage.setNewData(m_dataModel.getTestCaseVectorsSection());
                    m_assertPreCondStubsTPPage.setNewData(m_dataModel.getAsserts());
                    m_dryRunPage.setNewData(m_dataModel.getDryRunConfig());
                }
            }
        };        
    }

    @Override
    public void addPages() {
        
        initFunctionSection();
        
        m_funcParamsPage = new TCGenIdentifiersPage("Function parameters",
                                                         "Define values for each of function parameters.",
                                                         m_mergedTestSpec,
                                                         m_dataModel.getFunctionSection(),
                                                         TCGenPageType.FUNCTION);
        
        m_funcParamsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-funcPage.png"));
        
        m_varsPage = new TCGenIdentifiersPage(SEC_NAME_VARIABLES,
                                                   "Define values for global, test, persistent, and host variables.",
                                                         m_mergedTestSpec,
                                                         m_dataModel.getVarsSection());
        
        m_varsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-varsPage.png"));
        
        m_stubsPage = new TCGenIdentifiersPage(SEC_NAME_STUBS,
                                                    "Define values for stub assignments.",
                                                   m_mergedTestSpec,
                                                   m_dataModel.getStubsSection(), 
                                                   TCGenPageType.STUBS);
  
        m_stubsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-stubsPage.png"));

        m_testPointsPage = new TCGenIdentifiersPage("Test points",
                                                         "Define values for test point assignments.",
                                                   m_mergedTestSpec,
                                                   m_dataModel.getTestPointsSection(), 
                                                   TCGenPageType.TEST_POINTS);
  
        m_testPointsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-testPointPage.png"));

        m_hilPage = new TCGenIdentifiersPage(SEC_NAME_HIL, 
                                                  "Define values for HIL outputs.",
                                                  m_mergedTestSpec,
                                                  m_dataModel.getHil());
        m_hilPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-hilPage.png"));

        m_optionsPage = new TCGenIdentifiersPage(SEC_NAME_OPTIONS, 
                                                      "Define settings for winIDEA Options.",
                                                      m_mergedTestSpec,
                                                      m_dataModel.getOptions());
        m_optionsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-optionsPage.png"));

        m_initTargetScriptPage = new TCGenIdentifiersPage("Init Target Script Function", 
                                                               "Define parameters for Init Target script function.",
                                                               m_mergedTestSpec,
                                                               m_dataModel.getInitTargetScript());
        m_initTargetScriptPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/extensionWizard-TargetInit.png"));

        m_initFuncScriptPage = new TCGenIdentifiersPage("Init Function Script Function", 
                                                             "Define parameters for Init Function script function.",
                                                             m_mergedTestSpec,
                                                             m_dataModel.getInitFunctionScript());

        m_initFuncScriptPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/extensionWizard-TestInit.png"));

        m_endFuncScriptPage = new TCGenIdentifiersPage("End Function Script Function", 
                                                            "Define parameters for End Function script function.",
                                                            m_mergedTestSpec,
                                                            m_dataModel.getEndFunctionScript());

        m_endFuncScriptPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/extensionWizard-TestEnd.png"));

        m_restoreTargetScriptPage = new TCGenIdentifiersPage("Restore Target Script Function", 
                                                                  "Define parameters for Restore Targetscript function.",
                                                                  m_mergedTestSpec,
                                                                  m_dataModel.getRestoreTargetScript());

        m_restoreTargetScriptPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/extensionWizard-TargetRestore.png"));

        m_testCasePage = new TCGenIdentifiersPage("Test Case", 
                                                       "Define vectors for final test case out of section vectors generated on previous pages.",
                                                       m_mergedTestSpec,
                                                       m_dataModel.getTestCaseVectorsSection(),
                                                       TCGenPageType.TEST_CASES);

        m_testCasePage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-testCasePage.png"));

        m_assertPreCondStubsTPPage = new TCGen_PreCond_Stub_TP_ExpressionsPage(m_dataModel.getAsserts());
        m_assertPreCondStubsTPPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-expressionsPage.png"));
        
        
        m_dryRunPage = new TCGenDryRunPage("Dry Run", 
                                           "Define variables to set during dry run.",
                                           m_dataModel.getDryRunConfig());
        m_dryRunPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-dryRunPage.png"));

        
        m_pages = Arrays.asList(m_funcParamsPage, m_varsPage, m_stubsPage, m_testPointsPage,
                                m_hilPage, m_optionsPage, m_initTargetScriptPage,
                                m_initFuncScriptPage, m_endFuncScriptPage,
                                m_restoreTargetScriptPage, m_testCasePage);

        // spaces are used to make columns wider by default
        m_funcParamsPage.setColumn0Title("Parameters       ");
        m_varsPage.setColumn0Title("Variables                     ");
        m_stubsPage.setColumn0Title("Variables                     ");
        m_testPointsPage.setColumn0Title("Variables                     ");
        m_hilPage.setColumn0Title("HIL outputs");
        m_optionsPage.setColumn0Title("Option URLs                    ");
        m_initTargetScriptPage.setColumn0Title("Script parameters");
        m_initFuncScriptPage.setColumn0Title("Script parameters");
        m_endFuncScriptPage.setColumn0Title("Script parameters");
        m_restoreTargetScriptPage.setColumn0Title("Script parameters"); 
        m_testCasePage.setColumn0Title("Sections");
        
        setAutoCompleteProposals();
        
        for (TCGenIdentifiersPage page : m_pages) {
            page.setCoreId(m_coreId);
            addPage(page);
        }
        
        addPage(m_assertPreCondStubsTPPage);
        addPage(m_dryRunPage);
    }


    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        
        // asserts and Dry run sections are not shown in basic view
        if (page == m_testCasePage  &&  !m_funcParamsPage.isShowAllPages()) {
            return null;
        }
        
        page = super.getNextPage(page);
        
        if (page == m_hilPage  &&  !m_funcParamsPage.isShowAllPages()) {
            page = m_testCasePage;
        }
        
        if (page == m_testCasePage) {
            initTestCasePageModel();
        }
        
        return page;
    }


    @Override
    public IWizardPage getPage(String name) {
        IWizardPage page = super.getPage(name);
        
        if (page == m_testCasePage) {
            initTestCasePageModel();
        }
        return page;
    }
    
    
    private void setAutoCompleteProposals() {
        
        m_funcParamsPage.setAutoCompleteProposals(m_funcAutoCompleteProposals, null);
        
        GlobalsProvider globalsProvider = GlobalsConfiguration.instance().
                          getGlobalContainer().getVarsGlobalsProvider(m_coreId);
        
        String[] proposals = globalsProvider.getCachedGlobals();

        if (proposals == null) {
            proposals = new String[0];
        }

        StrVector locals = new StrVector(); 
        m_mergedTestSpec.getLocalVariablesKeys(locals);
        
        ArrayList<String> proposalsList = new ArrayList<String>();
        
        for (int idx = 0; idx < locals.size(); idx++) {
            proposalsList.add(locals.get(idx));
        }
        
        if (proposals.length > 0) {
            proposalsList.add("--- global variables ---");
            proposalsList.addAll(Arrays.asList(proposals));
        }
        
        String[] proposalsArray = proposalsList.toArray(new String[0]);
        m_varsPage.setAutoCompleteProposals(proposalsArray, null);
        m_stubsPage.setAutoCompleteProposals(proposalsArray, null);
        m_testPointsPage.setAutoCompleteProposals(proposalsArray, null);
        m_assertPreCondStubsTPPage.setAutoCompleteProposals(proposalsArray, null);
        m_dryRunPage.setAutoCompleteProposals(proposalsArray, null);
        
        globalsProvider = GlobalsConfiguration.instance().
                                  getGlobalContainer().getHilGlobalsProvider();
        
        proposals = globalsProvider.getCachedGlobals();
        String[] descriptions = globalsProvider.getCachedDescriptions();

        if (proposals == null) {
            proposals = new String[0];
        }

        m_hilPage.setAutoCompleteProposals(proposals, descriptions);
    }

    @Override
    public boolean canFinish() {
        // The Finish button is enabled only on the last page.
        IWizardPage currentPage = getContainer().getCurrentPage();
        boolean canFinish = currentPage == m_testCasePage  ||
                            currentPage == m_assertPreCondStubsTPPage  ||
                            currentPage == m_dryRunPage;
                
        return super.canFinish()  &&  canFinish;
    }

    
    @Override
    public boolean performFinish() {

        try {
            m_stubsPage.pageSpecificDataToModel();
            m_testPointsPage.pageSpecificDataToModel();
            m_testCasePage.pageSpecificDataToModel();
            m_dryRunPage.pageSpecificDataToModel();
            generateTestCases();
        } catch (Exception ex) {
            SExceptionDialog.open(getShell(), 
                                  "Error in test case generation", 
                                  ex);
            return false;
        } 
        
        return true;
    }

    
    public boolean isDeleteExistingDerivedTestCases() {
        return m_testCasePage.isDeleteExistingDerivedTestCases();
    }
    
    
    public List<CTestSpecification> getGeneratedTestCases() {
        return m_generatedTestCases;
    }
    
    
    private void generateTestCases() {
        
        m_generatedTestCases.clear(); 
                
        List<String[]> vectors = m_testCasePage.getVectors();
        TCGenSection testCaseVectorsSection = m_dataModel.getTestCaseVectorsSection();
        String[] sectionNames = testCaseVectorsSection.getIdentifierNames();
        TCGenAsserts asserts = m_dataModel.getAsserts();
        
        for (String[] vector : vectors) {

            if (vector.length  !=  sectionNames.length) {
                throw new SIllegalArgumentException("Internal error - size of vectors "
                        + "and identifier names do not match.").
                        add("vectors.size", vectors.size()).
                        add("identifiers.size", sectionNames.length);
            }

            CTestSpecification newTestSpec = new CTestSpecification(m_selectedTestSpec);
            m_generatedTestCases.add(newTestSpec);

            for (int idx = 0; idx < vector.length; idx++) {
                
                switch (sectionNames[idx]) {
                case SEC_NAME_FUNCTION:
                    generateFunction(newTestSpec.getFunctionUnderTest(false), vector, idx);
                    break;
                case SEC_NAME_VARIABLES:
                    generateTestVars(newTestSpec, vector, idx);
                    break;

                case SEC_NAME_STUBS:
                    generateStub(newTestSpec, vector, idx, 
                                 asserts.getStubExpressions());
                    break;
                case SEC_NAME_TEST_POINTS:
                    generateTestPoint(newTestSpec, vector, idx, 
                                      asserts.getTestPointExpressions());
                    break;

                case SEC_NAME_HIL:
                    generateHIL(newTestSpec, vector, idx);
                    break;
                case SEC_NAME_OPTIONS:
                    generateOptions(newTestSpec, vector, idx);
                    break;

                case SEC_NAME_INIT_TARGET_SCRIPT: {
                    CTestFunction scriptFunc = newTestSpec.getInitTargetFunction(false);
                    generateFunction(scriptFunc, vector, idx);
                    scriptFunc.setName(m_selectedTestSpec.getInitTargetFunction(true).getName());
                } break;
                case SEC_NAME_INIT_FUNCTION_SCRIPT: { 
                    CTestFunction scriptFunc = newTestSpec.getInitFunction(false);
                    generateFunction(scriptFunc, vector, idx);
                    scriptFunc.setName(m_selectedTestSpec.getInitFunction(true).getName());
                } break;
                case SEC_NAME_END_FUNCTION_SCRIPT: { 
                    CTestFunction scriptFunc = newTestSpec.getEndFunction(false);
                    generateFunction(scriptFunc, vector, idx);
                    scriptFunc.setName(m_selectedTestSpec.getEndFunction(true).getName());
                } break;
                case SEC_NAME_RESTORE_TARGET_SCRIPT: { 
                    CTestFunction scriptFunc = newTestSpec.getRestoreTargetFunction(false);
                    generateFunction(scriptFunc, vector, idx);
                    scriptFunc.setName(m_selectedTestSpec.getRestoreTargetFunction(true).getName());
                } break;
                default:
                    throw new SIllegalStateException("Unsupported case value!").
                    add("value", sectionNames[idx]);    
                }
            }
            
            if (!asserts.getPreConditionExpressions().isEmpty()) {
                setAsserts(newTestSpec.getPrecondition(false),
                           asserts.getPreConditionExpressions());
            }
            
            if (!asserts.getExpressions().isEmpty()) {
                setAsserts(newTestSpec.getAssert(false),
                           asserts.getExpressions());
            }

            setAnalyzerSections(testCaseVectorsSection, newTestSpec);
            
            setDryRun(newTestSpec);
        }
    }


    private void generateFunction(CTestFunction testFunc,
                                  String[] vector,
                                  int idx) {
        
        String valuesList = vector[idx];
        // remove '{' and '}'
        valuesList = valuesList.substring(1, valuesList.length() - 1);
        StrVector values = DataUtils.splitToVectorWithISysQualifiedNames(valuesList);
        testFunc.setPositionParameters(values);
    }

    
    private void generateTestVars(CTestSpecification testSpec,
                                  String[] vector,
                                  int idx) {
        CMapAdapter baseVarsInit = new CMapAdapter(m_mergedTestSpec, 
                                                   SectionIds.E_SECTION_INIT.swigValue(),
                                                   false);
        CMapAdapter varsInit = new CMapAdapter(testSpec, 
                                               SectionIds.E_SECTION_INIT.swigValue(),
                                               false);
        varsInit.assign(baseVarsInit); // copy base init to generated one
        
        String[] varNames = m_dataModel.getVarsSection().getIdentifierNames();
        List<String> varValues = strVector2List(vector, idx);
        for (int varIdx = 0; varIdx < varNames.length; varIdx++) {
            varsInit.setValue(varNames[varIdx], varValues.get(varIdx));
        }
    }


    private void generateStub(CTestSpecification newTestSpec,
                              String[] assignValuesVector,
                              int idx, 
                              List<String[]> assertExpressions) {
        
        TCGenSection stubsSection = m_dataModel.getStubsSection();
        String functionName = stubsSection.getStubbedFuncOrTestPointId();
        CTestStub parentStub = m_mergedTestSpec.getStub(functionName);
        if (parentStub == null) {
            throw new IllegalArgumentException("Stub for function '" + functionName +
                                               "' not found in base test case!");
        }
        
        CTestStub stub = new CTestStub(newTestSpec);
        stub.assign(parentStub);
        newTestSpec.getStubs(false).add(-1, stub);
        
        CTestBaseList steps = 
            stub.getTestBaseList(EStubSectionIds.E_SECTION_ASSIGN_STEPS.swigValue(), 
                                 false);
        
        String stepIdx = stubsSection.getStubOrTpStepIndex();
        modifyStep(idx, assignValuesVector, stubsSection, stub, steps, 
                   stepIdx, assertExpressions);
    }
    

    private void generateTestPoint(CTestSpecification newTestSpec,
                                   String[] vector,
                                   int idx, 
                                   List<String[]> assertExpressions) {
        
        TCGenSection tpSection = m_dataModel.getTestPointsSection();
        String testPointId = tpSection.getStubbedFuncOrTestPointId();
        CTestPoint parentTp = m_mergedTestSpec.getTestPoint(testPointId);
        if (parentTp == null) {
            throw new IllegalArgumentException("Test point with ID '" + testPointId +
                                               "' not found in base test case!");
        }
        
        CTestPoint newTestPoint = new CTestPoint(newTestSpec);
        newTestPoint.assign(parentTp);
        newTestSpec.getTestPoints(false).add(-1, newTestPoint);
        
        CTestBaseList tpSteps = 
                newTestPoint.getTestBaseList(ETestPointSections.E_SECTION_STEPS.swigValue(), 
                                          false);
        
        String stepIdx = tpSection.getStubOrTpStepIndex();
        modifyStep(idx, vector, tpSection, newTestPoint, tpSteps, 
                   stepIdx ,assertExpressions);
    }


    private void generateHIL(CTestSpecification testSpec,
                             String[] vector,
                             int idx) {
        CTestHIL hil = testSpec.getHIL(false);
        String[] ioNames = m_dataModel.getHil().getIdentifierNames();

        String valuesList = vector[idx];
        // remove { and }
        valuesList = valuesList.substring(1, valuesList.length() - 1);
        List<String> hilValues = DataUtils.splitToList(valuesList);
        
        for (int hilIdx = 0; hilIdx < ioNames.length; hilIdx++) {
            hil.setParam(ioNames[hilIdx], hilValues.get(hilIdx));
        }
    }

    
    private void generateOptions(CTestSpecification testSpec,
                                 String[] vector,
                                 int idx) {
        String[] optURLs = m_dataModel.getOptions().getIdentifierNames();

        String valuesList = vector[idx];
        // remove { and }
        valuesList = valuesList.substring(1, valuesList.length() - 1);
        List<String> optValues = DataUtils.splitToList(valuesList);
        
        CMapAdapter options = new CMapAdapter(testSpec, 
                                              SectionIds.E_SECTION_OPTIONS.swigValue(), 
                                              false);
        for (int optIdx = 0; optIdx < optURLs.length; optIdx++) {
            options.setValue(optURLs[optIdx], optValues.get(optIdx));
        }
    }

    
    private void modifyStep(int vectorIdx,
                            String[] vector,
                            TCGenSection stubsSection,
                            CTestBase parent,
                            CTestBaseList steps,
                            String stepIdx,
                            List<String[]> assertExpressions) {
        
        String[] stepVarNames = stubsSection.getIdentifierNames();
        
        List<String> stepVarValues = strVector2List(vector, vectorIdx);
        
        CTestEvalAssignStep step;
        if (steps.isEmpty()) {
            step = new CTestEvalAssignStep(parent);
            steps.add(-1, step);
        } else {
            // invalid index means the last element. This way '-1' can be used
            // to indicate the last element in the list
            int sIdx = NumberUtils.toInt(stepIdx, (int)steps.size() - 1);
            if (sIdx < 0  ||  sIdx >= steps.size()) {
                sIdx = (int)steps.size() - 1;
            }
            step = CTestEvalAssignStep.cast(steps.get(sIdx));
        }
        
        CMapAdapter assignments = step.getAssignments(false);
        
        for (int varIdx = 0; varIdx < stepVarNames.length; varIdx++) {
            assignments.setValue(stepVarNames[varIdx], stepVarValues.get(varIdx));
        }
        
        CSequenceAdapter stubAsserts = step.getExpectedExpressions(false);
        for(String[] expr : assertExpressions) {
            // expr. has always only one element - see comment in TCGenAsserts
            stubAsserts.add(-1, expr[0]);
        }
    }
    

    private List<String> strVector2List(String[] vector, int idx) {
        String valuesList = vector[idx];
        // remove '{' and '}'
        valuesList = valuesList.substring(1, valuesList.length() - 1);
        List<String> stepVarValues = DataUtils.splitToListWithISysQualifiedNames(valuesList);
        return stepVarValues;
    }

    
    private void setAsserts(CTestAssert testAsserts, 
                                  List<String[]> expressions) {
        // array String[] has always only one element - see comment in TCGenAsserts
        
        if (expressions.isEmpty()) {
            return;
        }
        
        CSequenceAdapter asserts = 
                new CSequenceAdapter(testAsserts,
                                     CTestAssert.ESectionAssert.E_SECTION_ASSERT_EXPRESSIONS.swigValue(),
                                     false);
        
        for (String[] expr : expressions) {
            asserts.add(-1, expr[0]);
        }
    }


    private void setAnalyzerSections(TCGenSection testCaseVectorsSection,
                                     CTestSpecification newTestSpec) {
        
        if (testCaseVectorsSection.isCopyCoverage()  ||
                testCaseVectorsSection.isCopyProfiler()  ||
                testCaseVectorsSection.isCopyTrace()) {
            
            CTestAnalyzer parentAnalyzer = m_mergedTestSpec.getAnalyzer(true);
            CTestAnalyzer newAnalyzer = newTestSpec.getAnalyzer(false);
            newAnalyzer.assign(parentAnalyzer);
            
            setClearCoverage(testCaseVectorsSection, newTestSpec.getAnalyzer(false));
            setClearProfiler(testCaseVectorsSection, newTestSpec.getAnalyzer(false));
            setClearTrace(testCaseVectorsSection, newTestSpec.getAnalyzer(false));
        }
                
    }

    
    private void setClearCoverage(TCGenSection testCaseVectorsSection,
                             CTestAnalyzer analyzer) {
        
        if (testCaseVectorsSection.isCopyCoverage()) {
            if (testCaseVectorsSection.isAppendModeOnCopy()) {
                analyzer.setOpenMode(EOpenMode.EAppend);
            }
        } else {
            analyzer.setSectionValue(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_COVERAGE.swigValue(), 
                                     null);
        }
    }


    private void setClearProfiler(TCGenSection testCaseVectorsSection,
                             CTestAnalyzer analyzer) {
        if (!testCaseVectorsSection.isCopyProfiler()) {
            
            analyzer.setSectionValue(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_PROFILER.swigValue(), 
                                     null);
        }
    }


    private void setClearTrace(TCGenSection testCaseVectorsSection,
                          CTestAnalyzer analyzer) {
        if (!testCaseVectorsSection.isCopyTrace()) {
            
            analyzer.setSectionValue(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_TRACE.swigValue(), 
                                     null);
        }
    }

    
    private void setDryRun(CTestSpecification newTestSpec) {
        TCGenDryRun dryRunConfig = m_dataModel.getDryRunConfig();
        
        if (!dryRunConfig.isEmpty()) {
            
            CTestDryRun dryRun = newTestSpec.getDryRun(false);
            
            List<String[]> newAssignments = dryRunConfig.getVarAssignments();
            if (!newAssignments.isEmpty()) {
                CMapAdapter assignmentsTB = dryRun.getAssignments(false);
                for (String[] newAssignment : newAssignments) {
                    assignmentsTB.setValue(newAssignment[0], newAssignment[1]);
                }
            }
            
            dryRun.setUpdateCoverage(dryRunConfig.isUpdateCoverage() ? 
                                     ETristate.E_TRUE : ETristate.E_DEFAULT);
            
            dryRun.setUpdateProfiler(dryRunConfig.isUpdateProfiler() ? 
                                     ETristate.E_TRUE : ETristate.E_DEFAULT);
            
            dryRun.setTagValue(EDryRunSectionIds.E_SECTION_PROFILER_MULTIPLIER.swigValue(), 
                               dryRunConfig.getProfilerStatsMultiplier());
            
            dryRun.setTagValue(EDryRunSectionIds.E_SECTION_PROFILER_OFFSET.swigValue(), 
                               dryRunConfig.getProfilerStatsOffset());
        }
    }

    
    private void initFunctionSection() {
        TCGenSection functionSection = m_dataModel.getFunctionSection();

        String funcName = m_mergedTestSpec.getFunctionUnderTest(true).getName();
        String coreId = m_mergedTestSpec.getCoreId();
        
        FunctionGlobalsProvider funcTypeProvider = GlobalsConfiguration.instance().
                           getGlobalContainer().getFuncGlobalsProvider(coreId);
        
        JFunction funcType = null;
        try {
            funcType = funcTypeProvider.getCachedFunction(funcName);
        } catch (SException ex) {
            // ignore, there will be no parameters in table, but user can see error
            // message also in StatusView and in Function section.
            // If we set error in wizard, user has no option to finish it with OK. 
        }
        
        if (funcType != null) {
            
            JVariable[] params = funcType.getParameters();
            m_funcAutoCompleteProposals = new String[params.length];
            
            int idx = 0;
            List<TCGenIdentifier> identifiers = functionSection.getIdentifiers();
            
            for (JVariable param : params) {
                
                String identifierName = param.getVarTypeName() + " " + param.getName();
                m_funcAutoCompleteProposals[idx] = identifierName;
                
                if (idx < identifiers.size()) { 
                    // overwrite identifiers if exist from previous generation
                    TCGenIdentifier ident = identifiers.get(idx);
                    ident.setIdentifierName(identifierName);
                } else {
                    functionSection.addIdentifier(-1, identifierName);
                }
                idx++;
            }
        }

    }
    
    
    private void initTestCasePageModel() {

        try {
            TCGenSection testCaseSection = m_dataModel.getTestCaseVectorsSection();
            List<TCGenIdentifier> oldIdentifiers = new ArrayList<>(); 
            oldIdentifiers.addAll(testCaseSection.getIdentifiers());
            
            testCaseSection.getIdentifiers().clear();

            sectionToTestCaseModel(testCaseSection, m_dataModel.getFunctionSection(), SEC_NAME_FUNCTION);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getVarsSection(), SEC_NAME_VARIABLES);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getStubsSection(), SEC_NAME_STUBS);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getTestPointsSection(), SEC_NAME_TEST_POINTS);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getHil(), SEC_NAME_HIL);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getOptions(), SEC_NAME_OPTIONS);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getInitTargetScript(), SEC_NAME_INIT_TARGET_SCRIPT);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getInitFunctionScript(), SEC_NAME_INIT_FUNCTION_SCRIPT);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getEndFunctionScript(), SEC_NAME_END_FUNCTION_SCRIPT);
            sectionToTestCaseModel(testCaseSection, m_dataModel.getRestoreTargetScript(), SEC_NAME_RESTORE_TARGET_SCRIPT);

            copyOccurValues(testCaseSection, oldIdentifiers);
            
            m_testCasePage.setNewData(testCaseSection);
        } catch (Exception ex) {        
            m_testCasePage.setErrorMessage(ex.getMessage());
        }
    }

    
    private void sectionToTestCaseModel(TCGenSection tcGenModel,
                                        TCGenSection section, String identName) {
        TCGenVectorsTableModel vectorsModel = new TCGenVectorsTableModel();
        vectorsModel.setData(section);
        List<String[]> vectors = vectorsModel.generateVectors();

        List<String> identValues = new ArrayList<>();
        for (Object[] vector : vectors) {
            String value = "{" + StringUtils.join(vector, ", ") + "}";
            identValues.add(value);
        }

        if (!identValues.isEmpty()) {
//            TCGenIdentifier identifier = null;

            // try to use existing identifier - this way persistence for values
            // occurrences is maintained when user goes back to previous wizard pages.
            List<TCGenIdentifier> idents = tcGenModel.getIdentifiers();
//            for (TCGenIdentifier ident : idents) {
//                if (ident.getIdentifierName().equals(identName)) {
//                    identifier = ident;
//                    identifier.getValues().clear();
//                }
//            }

            // if no identifier exists for the given section, create a new one
//            if (identifier == null) {
            TCGenIdentifier identifier = new TCGenIdentifier(identName);
                idents.add(identifier);
//            }

            identifier.getValues().addAll(identValues);
        }
    }
    
    
    /*
     * This m. tries its best to copy old occurrences to newly generated 
     * identifiers for test case.
     * 
     * If some identifier (test case section in this case) is missing the new
     * list of identifier, then its occurrence is lost.     * 
     * 
     * If some custom values are missing from the 
     * current identifier (it was present in previous test case vectors), 
     * then it is not added to the current list of custom values - it is lost. 
     *  
     */
    private void copyOccurValues(TCGenSection testCaseSection,
                                  List<TCGenIdentifier> oldIdents) {
        
        List<TCGenIdentifier> newIdents = testCaseSection.getIdentifiers();
        
        for (TCGenIdentifier newIdent : newIdents) {
            for (TCGenIdentifier oldIdent : oldIdents) {
                if (newIdent.getIdentifierName().equals(oldIdent.getIdentifierName())) {
                    TCGenOccur oldOccur = oldIdent.getOccurrence();
                    newIdent.getOccurrence().assign(oldOccur);
                    
                    // copy also occurrences of values, which existed in previous generation
                    Set<String> newValues = new TreeSet<>(newIdent.getAllValues());
                    List<TCGenValue> oldCustomOccurs = oldIdent.getCustomValueOccurrences();
                    
                    for (TCGenValue customValue : oldCustomOccurs) {
                        if (newValues.contains(customValue.getValue())) {
                            newIdent.addValueOccurrence(new TCGenValue(customValue));
                        }
                    }
                    
                }
            }
        }
    }
}
