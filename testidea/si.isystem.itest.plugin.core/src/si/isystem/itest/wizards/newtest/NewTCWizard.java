package si.isystem.itest.wizards.newtest;

import java.util.List;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.AutoIdGenerator;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.TSUtils;
import si.isystem.itest.handlers.NewBaseTestCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.AddTestTreeNodeAction;
import si.isystem.itest.wizards.newtest.NewTCWizardDataModel.FuncCfg;
import si.isystem.itest.wizards.newtest.NewTCWizardDataModel.SectionCfg;

public class NewTCWizard extends Wizard {
    
    public static final String DEFAULT_VAR_VALUE = "0";

    private NewTCWizardDataModel m_ntcModel; 
    
    private NewTCFirstPage m_firstPage;
    private NewTCFunctionsPage m_functionsPage;
    private NewTCVariablesPage m_varsPage;
    private NewTCExpressionsPage m_expressionsPage;
    

    public NewTCWizard(String defaultRetValName, String title, ETestScope testScope) {
        super();
        setNeedsProgressMonitor(false);
        setWindowTitle(title);

        m_ntcModel = new NewTCWizardDataModel(defaultRetValName, testScope); 
    }
    
    
    public NewTCWizardDataModel getNtcModel() {
        return m_ntcModel;
    }


    @Override
    public void addPages() {
        
        m_firstPage = new NewTCFirstPage(m_ntcModel);
        m_functionsPage = new NewTCFunctionsPage(m_ntcModel);
        m_varsPage = new NewTCVariablesPage(m_ntcModel);
        m_expressionsPage = new NewTCExpressionsPage(m_ntcModel);
        
        m_firstPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-testCasePage.png"));
        m_functionsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-funcPage.png"));
        m_varsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-varsPage.png"));
        m_expressionsPage.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/testGenWizard-expressionsPage.png"));

        addPage(m_firstPage);
        addPage(m_functionsPage);
        addPage(m_varsPage);
        addPage(m_expressionsPage);
    }    

    
    @Override
    public IWizardPage getNextPage(IWizardPage page) {

        ((GlobalsWizardDataPage)page).dataToModel();

        IWizardPage nextPage = super.getNextPage(page);
        if (nextPage != null) {
            ((GlobalsWizardDataPage)nextPage).dataFromModel();
        }
        
        return nextPage;
    }
    
    
    @Override
    public boolean canFinish() {
        // The Finish button is enabled when function name is not empty.
        // boolean canFinish = !m_ntcModel.m_funcUnderTestName.isEmpty();
                
        // It is always allowed to Finish the wizard.
        return true;
    }

    
    @Override
    public boolean performFinish() {

        try {
            // when user presses 'Finish' getPage is not called
            m_firstPage.dataToModel();
            m_functionsPage.dataToModel();
            m_varsPage.dataToModel();
            m_expressionsPage.dataToModel();
        } catch (Exception ex) {
            SExceptionDialog.open(getShell(), 
                                  "Error in test case generation", 
                                  ex);
            return false;
        } 
        
        return true;
    }


    public AbstractAction getNewTestCaseAction(NewTCWizardDataModel ntcModel, 
                                               CTestSpecification parentTestSpec, 
                                               TestSpecificationModel model, 
                                               boolean isParentExpectedSectionDefined) {
        
        if (parentTestSpec == null) {
            parentTestSpec = model.getRootTestSpecification();
        }
        
        CTestSpecification newTestSpec = createNewTestCaseFromFirstPage(ntcModel, 
                                                                        parentTestSpec, 
                                                                        model, 
                                                                        isParentExpectedSectionDefined);
        addFunctionsPageInfo(ntcModel, model, newTestSpec);
        addVariablesPageInfo(ntcModel, newTestSpec);
        addExpressionsPageInfo(ntcModel, newTestSpec);
        
        return new AddTestTreeNodeAction(model, parentTestSpec, -1, newTestSpec);
    }


    private CTestSpecification createNewTestCaseFromFirstPage(NewTCWizardDataModel ntcModel, 
                                                              CTestSpecification parentTestSpec, 
                                                              TestSpecificationModel model, 
                                                              boolean isParentExpectedSectionDefined) {

        CTestSpecification newTestSpec = new CTestSpecification(parentTestSpec);
        
        boolean isSystemTest = ntcModel.m_testScope == ETestScope.E_SYSTEM_TEST; 
        if (isSystemTest) {
            newTestSpec.setTestScope(ETestScope.E_SYSTEM_TEST);
        } else {
            if (ntcModel.m_funcUnderTestName != null  &&  !ntcModel.m_funcUnderTestName.isEmpty()) {
                newTestSpec.getFunctionUnderTest(false).setName(ntcModel.m_funcUnderTestName);
            }

            if (!ntcModel.m_parameters.isEmpty()) {
                CTestFunction testFunc = newTestSpec.getFunctionUnderTest(false);
                CSequenceAdapter params = new CSequenceAdapter(testFunc, 
                                                               CTestFunction.ESection.E_SECTION_PARAMS.swigValue(),
                                                               false);
                List<String> list = DataUtils.splitToList(ntcModel.m_parameters);
                for (String param : list) {
                    params.add(-1, param);
                }
            }
            if (ntcModel.m_testScope != null) {
                newTestSpec.setTestScope(ETestScope.E_UNIT_TEST);
            }
        } 
        
        String expression, retValName;
        if (NewTCWizardDataModel.m_isDefaultExpr) {
            expression = CTestCase.getISystemRetValName() + " == " + ntcModel.m_valForDefaultExpr;
            retValName = "";
        } else {
            expression = ntcModel.m_expectExpr;
            retValName = ntcModel.m_retValVarName;
        }

        if (!expression.isEmpty()  ||  isParentExpectedSectionDefined) {
            StrVector vector = DataUtils.splitToVectorWithISysQualifiedNames(expression);
            newTestSpec.setExpectedResults(vector);
            
            // set return value name only if there are expected results. To be exact,
            // it should also check function return type - if void, no return value
            // is needed. However, this would not work if connection to winIDEA
            // is off. If function name is empty, return value is also not specified,
            // this is useful for derived specifications, where only parameters change
            if (!isSystemTest  &&  !retValName.isEmpty()  &&  
                 ntcModel.m_funcUnderTestName != null  &&  !ntcModel.m_funcUnderTestName.isEmpty()) {
                
                newTestSpec.getFunctionUnderTest(false).setRetValueName(retValName);
            }
        }
        
        newTestSpec.setCoreId(ntcModel.m_coreId);
        
        if (NewTCWizardDataModel.m_isAutoCreateId) {
            // let the test sequence start from max number, so that probability
            // of duplicated ID is smaller.
            int noOfTestCases = model.getNoOfTestCases();
            AutoIdGenerator autoIdGen = new AutoIdGenerator();
            autoIdGen.setTestCounter(noOfTestCases);
            NewBaseTestCmdHandler.autoGenerateTestId(newTestSpec, autoIdGen);
        }

        return newTestSpec;
    }


    private void addFunctionsPageInfo(NewTCWizardDataModel ntcModel,
                                      TestSpecificationModel model,
                                      CTestSpecification newTestSpec) {
        setStubs(newTestSpec, ntcModel);
        setUserStubs(newTestSpec, ntcModel);
        setTestPoints(newTestSpec, ntcModel);
        setCoverage(newTestSpec, ntcModel, model);
        setProfiler(newTestSpec, ntcModel, model);
    }
    
    
    private void setStubs(CTestSpecification newTestSpec, NewTCWizardDataModel ntcModel) {
        for (FuncCfg funcCfg : ntcModel.m_functionConfigs) {
            
            SectionCfg sectionCfg = funcCfg.m_stub;
            
            if (sectionCfg.m_isCreated) {
                CTestStub stub = TSUtils.createStub(newTestSpec, 
                                                    funcCfg.m_functionName, 
                                                    sectionCfg.m_itemParams);
                
                newTestSpec.getStubs(false).add(-1, stub);
            }
        }
    }


    private void setUserStubs(CTestSpecification newTestSpec, NewTCWizardDataModel ntcModel) {
        for (FuncCfg funcCfg : ntcModel.m_functionConfigs) {
            SectionCfg sectionCfg = funcCfg.m_userStub;
            if (sectionCfg.m_isCreated) {
                CTestUserStub stub = TSUtils.createUserStub(newTestSpec, 
                                                            funcCfg.m_functionName, 
                                                            sectionCfg.m_itemParams);
                newTestSpec.getUserStubs(false).add(-1, stub);
            }
        }
    }


    private void setTestPoints(CTestSpecification newTestSpec, NewTCWizardDataModel ntcModel) {
        for (FuncCfg funcCfg : ntcModel.m_functionConfigs) {
            SectionCfg sectionCfg = funcCfg.m_testPoint;
            if (sectionCfg.m_isCreated) {
                CTestPoint testPoint = TSUtils.createTestPoint(newTestSpec, 
                                                               funcCfg.m_functionName, 
                                                               sectionCfg.m_itemParams);
                newTestSpec.getTestPoints(false).add(-1, testPoint);
            }
        }
    }


    private void setCoverage(CTestSpecification newTestSpec, 
                             NewTCWizardDataModel ntcModel, 
                             TestSpecificationModel model) {
        
        for (FuncCfg funcCfg : ntcModel.m_functionConfigs) {
            SectionCfg sectionCfg = funcCfg.m_coverage;
            if (sectionCfg.m_isCreated) {
                CTestAnalyzer analyzer = newTestSpec.getAnalyzer(false);
                analyzer.setRunMode(ERunMode.M_START);
                String analyzerDocFName = model.getCEnvironmentConfiguration().
                        getToolsConfig(true).getAnalyzerFName();
                analyzer.setDocumentFileName(analyzerDocFName);
                
                CTestAnalyzerCoverage coverage = analyzer.getCoverage(false);
                coverage.setActive(ETristate.E_TRUE);
                CTestCoverageStatistics stat = TSUtils.createCoverageStats(coverage, 
                                                                           funcCfg.m_functionName, 
                                                                           sectionCfg.m_itemParams);
                coverage.getStatisticsList(false).add(-1, stat);
            }
        }
    }


    private void setProfiler(CTestSpecification newTestSpec, 
                             NewTCWizardDataModel ntcModel, 
                             TestSpecificationModel model) {
        
        for (FuncCfg funcCfg : ntcModel.m_functionConfigs) {
            SectionCfg sectionCfg = funcCfg.m_profiler;
            if (sectionCfg.m_isCreated) {
                CTestAnalyzer analyzer = newTestSpec.getAnalyzer(false);
                analyzer.setRunMode(ERunMode.M_START);
                String analyzerDocFName = model.getCEnvironmentConfiguration().
                        getToolsConfig(true).getAnalyzerFName();
                analyzer.setDocumentFileName(analyzerDocFName);
                
                CTestAnalyzerProfiler profiler = analyzer.getProfiler(false);
                profiler.setActive(ETristate.E_TRUE);
                CTestProfilerStatistics stat = TSUtils.createProfilerStats(profiler,
                                                                           funcCfg.m_functionName, 
                                                                           sectionCfg.m_itemParams);
                profiler.getCodeAreas(false).add(-1, stat);
            }
        }
    }


    private void addVariablesPageInfo(NewTCWizardDataModel ntcModel,
                                      CTestSpecification newTestSpec) {
        
        // declarations
        int idx = 0;
        CMapAdapter declarations = new CMapAdapter(newTestSpec, 
                                                   SectionIds.E_SECTION_LOCALS.swigValue(),
                                                   false);
        for (String varName : ntcModel.m_varNames) {
            if (ntcModel.m_isDeclVar.get(idx)) {
                declarations.setValue(varName, ntcModel.m_varTypes.get(idx));
            }
            idx++;
        }

        // initializations
        CMapAdapter inits = new CMapAdapter(newTestSpec, 
                                            SectionIds.E_SECTION_INIT.swigValue(),
                                            false);
        for (String initVar : ntcModel.m_initVars) {
            inits.setValue(initVar, DEFAULT_VAR_VALUE);
        }
    }


    private void addExpressionsPageInfo(NewTCWizardDataModel ntcModel,
                                        CTestSpecification newTestSpec) {
        
        CTestAssert testAssert = newTestSpec.getAssert(false);
        StrVector expressionsFirstPage = new StrVector();
        testAssert.getExpressions(expressionsFirstPage); // get expressions set in first page
        
        StrVector expressions = DataUtils.listToStrVector(ntcModel.m_varsForExpressions);
        for (int idx = 0; idx < expressions.size(); idx++) {
            expressionsFirstPage.add(expressions.get(idx));
        }
        
        testAssert.setExpressions(expressionsFirstPage);
    }
}
