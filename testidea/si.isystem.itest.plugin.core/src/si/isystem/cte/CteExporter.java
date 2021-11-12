package si.isystem.cte;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.xml.sax.SAXException;

import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.connect.CStringStream;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestEvalAssignStep.EStepSectionIds;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.ETristate;
import si.isystem.connect.EmitterFactory;
import si.isystem.connect.IEmitter;
import si.isystem.connect.IEmitter.EYamlStyle;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.cte.model.Class;
import si.isystem.cte.model.Classification;
import si.isystem.cte.model.Composition;
import si.isystem.cte.model.CteObject;
import si.isystem.cte.model.CteObject.Tree;
import si.isystem.cte.model.Mark.Tag;
import si.isystem.cte.model.Mark.Tag.Content;
import si.isystem.cte.model.Marks;
import si.isystem.cte.model.ObjectFactory;
import si.isystem.cte.model.TestGroup;
import si.isystem.cte.model.TestGroup.TestCase;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.handlers.ToolsConnectToWinIDEACmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.mk.utils.PlatformUtils;

/**
 * This class exports isystem.test test specifications to CTE format. Parameters,
 * which are test inputs (func. params, vars, stubs, HIL, scripts) are exported
 * as compositions/classifications/classes, while other test spec. settings, like 
 * expected results, coverage, profiler, options, ... are exported as YAML
 * strings and attached to CTE test cases. iTest test specifications, which
 * have derived tests, should be abstract, because they are not considered as
 * separate test case when exporting to CTE format - they are exported 
 * as CTE Test Groups. 
 * 
 * CTE Test Steps are not supported by this class, neither on import - error is reported.
 * 
 * Non-input test settings (coverage, profiler, ...) are exported as YAML string 
 * instead as key/value CTE tags for the following reasons:
 * - comments are multiline strings and can not be moved to CTE as key/value pair
 * - much less code in Java, thus less error prone to future test spec modifications.
 * - existing serialization code can be reused
 * - simpler editing for users - it is a text editor instead of table editor, which is
 *   pretty inconvenient, especially in CTE (click for each cell to be modified)
 * - sequences, like for example expected expressions can not be naturally 
 *   mapped to key/value pairs.
 * The disadvantage is a possibility for user error, which makes YAML syntax invalid.
 *
 * For easier understanding of this class, it is highly recommended to see image
 * 'doc/isystemCteTree.png'.
 * 
 * @author markok
 */

public class CteExporter {
    
    public static final String ISYSTEM_CTE_MODEL_PACKAGE_NAME = "si.isystem.cte.model";
    public static final String CTE_SCHEMA_LOCATION = "resources/cte.xsd";
    
    // values for CTE test results (tag TestResult).
    private static final String TEST_RESULT_PASSED = "Passed";
    private static final String TEST_RESULT_FAILED = "Failed";
    private static final String TEST_RESULT_NOT_COMPLETED = "Not Completed";
    private static final String TEST_RESULT_NO_RUN = "No Run";
    private static final String TEST_RESULT_N_A = "N/A";

    // this value was obtained with observing CTE output file. The number string 
    // at the end encodes required/allowed/forbidden flags for tags for each CTE
    // object. See CTE Tools | Tags dialog.
    private static final String CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC = 
                         "com.berner_mattner.cte.impl.tags.TextualTag:000011000";
    
    // this tag is added to Classifications to store index of func. parameter.
    // This way we do not depend on connection to winIDEA on import.
    private static final String CTE_TAG_FOR_PARAM_IDX = 
                         "com.berner_mattner.cte.impl.tags.AttributesTag:001000000";

    
    protected static final String RESTORE_TARGET_COMPOS_PREFIX = "restoreTarget";
    protected static final String END_TEST_COMPOS_PREFIX = "endTest";
    protected static final String INIT_TEST_COMPOS_PREFIX = "initTest";
    protected static final String INIT_TARGET_COMPOS_PREFIX = "initTarget";
    protected static final String COMPOS_SCRIPT_PARAMS = "scriptParams";
    protected static final String COMPOS_ASSIGNMENTS = "assignments";
    protected static final String COMPOS_STUB_STEP_IDX = "nextStep";
    
    // private static final int RANGE_X = 1000;
    // private static final int RANGE_Y = 500;
    // private static final int RANGE_Y_SPACING = RANGE_Y / 7;
    
    protected static final String PARAMS_COMPOSITION_NAME = "params";
    protected static final String VARS_COMPOSITION_NAME = "variables";
    protected static final String STUBS_COMPOSITION_NAME = "stubs";
    protected static final String HIL_COMPOSITION_NAME = "HIL";
    protected static final String SCRIPT_COMPOSITION_NAME = "scripts";
    
    protected static final String FUNC_PARAM_IDX = "paramIdx";
    
    List<Map<String, si.isystem.cte.model.Class>> m_paramsValuesMaps = new ArrayList<>();
    Map<String, Map<String, si.isystem.cte.model.Class>> m_varsValuesMaps = new TreeMap<>();
    List<Map<String, si.isystem.cte.model.Class>> m_stubsValuesMaps = new ArrayList<>();
    List<Map<String, si.isystem.cte.model.Class>> m_hilValuesMaps = new ArrayList<>();
    List<Map<String, si.isystem.cte.model.Class>> m_scriptValuesMaps = new ArrayList<>();
    
    Map<String, Composition> m_mainCompositions = new TreeMap<>();;

    private FunctionGlobalsProvider m_funcProvider;
    private int m_idCounter = 1;
    private ObjectFactory m_factory;

    
    /**
     *  Defines tag types from CTE file. Names of enums must match strings from CTE.
     */
    enum ETagType {Description, Attributes, TagManager, TestResult, // CTE types
        meta, imports, function, variables, persistentVars, stubs, userStubs, testPoints, expected, 
        analyzer, hil,
        scripts, options, paramIdx, stepIdx};
    
    
    public CteExporter() {
        m_factory = new ObjectFactory();
    }

    
    public void export(CTestSpecification containerTestSpec, 
                       String cteFileName)
    
                     throws JAXBException, SAXException, IOException {

        if (containerTestSpec.getNoOfDerivedSpecs() > 1) {
            MessageDialog.openWarning(Activator.getShell(), "To many selected", 
            "More than one test specification is selected, but only one can be exported to one file.");
        }

        CTestSpecification testSpec = containerTestSpec.getDerivedTestSpec(0);
        // all tests are supposed to run on the same core, as they test the same function
        String coreId = testSpec.getCoreId();
        coreId = TestSpecificationModel.getActiveModel().getConfiguredCoreID(coreId);
        m_funcProvider = GlobalsConfiguration.instance().getGlobalContainer().getFuncGlobalsProvider(coreId);
        
        if (m_funcProvider.getCachedGlobals().length == 0) {
            if (!ToolsConnectToWinIDEACmdHandler.refreshGlobals()) {
                if (!MessageDialog.openConfirm(Activator.getShell(), "Function info not available!",
                "Debug information is not available! Please connect to winIDEA and download executable!\n\n" +
                "If you continue with export, names of function parameters will not be available in export file.")) {
                    return;
                }
            }
        }
        
        JAXBContext jaxbContext = JAXBContext.newInstance(ISYSTEM_CTE_MODEL_PACKAGE_NAME);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
        
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(PlatformUtils.getURLFromPluginPath(CTE_SCHEMA_LOCATION));
        marshaller.setSchema(schema);

        Path path = Paths.get(cteFileName);
        String mainTreeName = path.getFileName().toString();

        CteObject cteObject = m_factory.createCteObject();
        cteObject.setId(getNextId());

        TestGroup testGroup = m_factory.createTestGroup();
        testGroup.setId(getNextId());
        testGroup.setName("");
        cteObject.setTestGroup(testGroup);
        
        testSpec2CteObject(testSpec, cteObject, mainTreeName);
        
        try (FileOutputStream fileOutputStream = new FileOutputStream(cteFileName)) {
            marshaller.marshal(cteObject, fileOutputStream);
        } 
    }

    
    private void testSpec2CteObject(CTestSpecification testSpec,
                                    CteObject cteObject, 
                                    String mainTreeName) {

        Tree mainTree = m_factory.createCteObjectTree();
        mainTree.setId(getNextId());
        mainTree.setName(mainTreeName);
        cteObject.setTree(mainTree);
        
        m_mainCompositions.put(PARAMS_COMPOSITION_NAME, createParamsComposition(testSpec));
        m_mainCompositions.put(VARS_COMPOSITION_NAME, createVarsComposition(testSpec));
        m_mainCompositions.put(STUBS_COMPOSITION_NAME, createStubsComposition(testSpec));
        m_mainCompositions.put(HIL_COMPOSITION_NAME, createHILComposition(testSpec));
        m_mainCompositions.put(SCRIPT_COMPOSITION_NAME, createScriptsComposition(testSpec));
        
        Composition rootComposition = createRootComposition(testSpec);
        List<Object> rootChildren = rootComposition.getCompositionOrClassification();
        
        addIfNotNull(rootChildren, m_mainCompositions.get(PARAMS_COMPOSITION_NAME));
        addIfNotNull(rootChildren, m_mainCompositions.get(VARS_COMPOSITION_NAME));
        addIfNotNull(rootChildren, m_mainCompositions.get(STUBS_COMPOSITION_NAME));
        addIfNotNull(rootChildren, m_mainCompositions.get(HIL_COMPOSITION_NAME));
        addIfNotNull(rootChildren, m_mainCompositions.get(SCRIPT_COMPOSITION_NAME));
        
        TestGroup mainTestCaseGrp = m_factory.createTestGroup();
        mainTestCaseGrp.setId(getNextId());
        mainTestCaseGrp.setName("");
        cteObject.setTestGroup(mainTestCaseGrp);

        getClassesAndTestCases(testSpec, 
                               mainTestCaseGrp);
        
        addISystemTagManager(cteObject);
        
        final int INIT_XPOS = 0;
        final int DELTA_X = 60;
        final int INIT_YPOS = 10;
        final int DELTA_Y = 50;
        arrange(rootComposition, INIT_XPOS, DELTA_X, INIT_YPOS, DELTA_Y);
        
        // it is crucial to set object for xsd:IDREF attribute, not string, otherwise
        // mashaller reports: 
        // 'Object "c4" is found in an IDREF property but this object doesn't have an ID'
        mainTree.setRoot(rootComposition);
        
        mainTree.getCompositionOrClassificationOrClazz().add(rootComposition);
    }


    private void addISystemTagManager(CteObject cteObject) {
        Tag tagManager = m_factory.createMarkTag();
        tagManager.setId(getNextId());
        tagManager.setType(ETagType.TagManager.toString());
        
        addContentToTag(tagManager, ETagType.meta.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.imports.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.function.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.persistentVars.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.variables.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.stubs.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.userStubs.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.testPoints.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.expected.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.analyzer.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        // addContentToTag(tagManager, ETagType.coverage.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        // addContentToTag(tagManager, ETagType.profiler.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        // addContentToTag(tagManager, ETagType.trace.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.hil.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.scripts.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);
        addContentToTag(tagManager, ETagType.options.toString(), CTE_TAG_GROUPS_FOR_ITEST_TEST_SPEC);

        addContentToTag(tagManager, ETagType.paramIdx.toString(), CTE_TAG_FOR_PARAM_IDX);
        
        cteObject.getTagGroup().add(tagManager);
    }


    /**
     * Creates root composition, which contains test ID of the selected test spec.
     *  
     * @param testSpec test spec. with derived specs to be used for tree creation
     * @return CTE Composition
     */
    private Composition createRootComposition(CTestSpecification testSpec) {
        
        String testSpecName = testSpec.getUILabel();

        String testId = testSpec.getTestId();
        if (testId.isEmpty()) {
            throw new SIllegalArgumentException("Test Id should not be empty!").
                         add("testSpec", testSpecName);
        }
        
        Composition rootComposition = createComposition(testSpecName);
        
        return rootComposition;
    }
    
    
    /**
     * Creates composition with name 'params', which contains names of function 
     * parameters as classifications.
     */
    private Composition createParamsComposition(CTestSpecification testSpec) {
        
        // add function parameters
        Composition paramsComposition = createComposition(PARAMS_COMPOSITION_NAME);
        
        List<Object> paramsClassifications = paramsComposition.getCompositionOrClassification();
        
        String functionName = testSpec.getFunctionUnderTest(true).getName();

        JVariable[] paramInfo = null;
        
        try {
            JFunction funcInfo = m_funcProvider.getCachedFunction(functionName);
            if (funcInfo != null) {
                paramInfo = funcInfo.getParameters();
            }
        } catch (SException ex) {
            // ignore, there is fallback in 'else' of the next if statement
        }
        
        // we have debug info, let's use it 
        if (paramInfo != null) {
            for (int i = 0; i < paramInfo.length; i++) {
                String paramName = paramInfo[i].getVarTypeName() + "\n" + 
                        paramInfo[i].getName();
                Classification paramClassification = createClassification(paramName);
                addTag(paramClassification.getTagGroup(), ETagType.paramIdx, 
                       "idx", String.valueOf(i));
                paramsClassifications.add(paramClassification);
            }
        } else { // we'll generate the same number of param names as found in test spec
            StrVector params = new StrVector();
            testSpec.getPositionParams(params);
            for (int i = 0; i < params.size(); i++) {
                String paramName = "param_" + i;
                Classification paramClassification = createClassification(paramName);
                addTag(paramClassification.getTagGroup(), ETagType.paramIdx, 
                       "idx", String.valueOf(i));
                paramsClassifications.add(paramClassification);
            }
        }

        return paramsComposition;
    }


    /**
     * Creates composition with name 'variables', which contains names of variables 
     * as classifications.
     */
    private Composition createVarsComposition(CTestSpecification testSpec) {
        Composition varsComposition = null;
        
        StrVector varNames = new StrVector();
        testSpec.getInitKeys(varNames);
        
        if (varNames.size() > 0) {
            varsComposition = createComposition(VARS_COMPOSITION_NAME);
            
            for (int i = 0; i < varNames.size(); i++) {
                Classification classification = createClassification(varNames.get(i));
                varsComposition.getCompositionOrClassification().add(classification);
            }
        }
        
        return varsComposition;
    }


    /**
     * Creates composition with name 'stubs', which contains stubbed functions 
     * as compositions.
     */
    private Composition createStubsComposition(CTestSpecification testSpec) {
        
        Composition stubsComposition = null;
        CTestBaseList stubs = testSpec.getStubs(true);

        for (int i = 0; i < stubs.size(); i++) {
            CTestStub stub = CTestStub.cast(stubs.get(i));
            Composition oneStubComposition = null;
            
            CTestBaseList stubSteps = stub.getAssignmentSteps(true);
            int numSteps = (int)stubSteps.size();

            for (int stepIdx = 0; stepIdx < numSteps; stepIdx++) {

                // var assignments
                CTestEvalAssignStep step = CTestEvalAssignStep.cast(stubSteps.get(stepIdx));
                Composition assignComposition = null;

                // assignments table must specify vars to be assigned
                EStepSectionIds assignSection = CTestEvalAssignStep.EStepSectionIds.E_SECTION_ASSIGN;
                StrVector assignedVars = DataUtils.getKeys(step, assignSection.swigValue());
                if (assignedVars.size() > 0) {
                    assignComposition = createComposition(COMPOS_ASSIGNMENTS);

                    for (int varIdx = 0; varIdx < assignedVars.size(); varIdx++) {
                        String varName = assignedVars.get(varIdx);
                        Classification classification = createClassification(varName);
                        assignComposition.getCompositionOrClassification().add(classification);
                    }
                }

                // script params
                Composition scriptParamsComposition = null;
                EStepSectionIds scriptSection = CTestEvalAssignStep.EStepSectionIds.E_SECTION_SCRIPT_PARAMS;
                StrVector scriptParams = DataUtils.getVector(step, scriptSection.swigValue());
                if (scriptParams.size() > 0) {
                    scriptParamsComposition = createComposition(COMPOS_SCRIPT_PARAMS);
                    for (int paramIdx = 0; paramIdx < scriptParams.size(); paramIdx++) {
                        // names of script params are not known in test spec, only values,
                        // so create artificial name
                        String classificationName = createScriptParamClassificationName(paramIdx);
                        Classification classification = createClassification(classificationName);
                        addTag(classification.getTagGroup(), ETagType.paramIdx, 
                               "idx", String.valueOf(paramIdx));
                        scriptParamsComposition.getCompositionOrClassification().add(classification);
                    }
                }                    
                
                // next step index
                Classification nextStepIdxClassification = null;
                if (!step.getStepIdx().isEmpty()) {
                    nextStepIdxClassification = createClassification(COMPOS_STUB_STEP_IDX);
                }
                
                // create composition
                Composition stubStepComposition = createComposition("step-" + stepIdx);
                List<Object> stubCompositionList = stubStepComposition.getCompositionOrClassification();
                addIfNotNull(stubCompositionList, assignComposition);
                addIfNotNull(stubCompositionList, scriptParamsComposition);
                addIfNotNull(stubCompositionList, nextStepIdxClassification);

                if (oneStubComposition == null) {
                    oneStubComposition = createComposition(stub.getFunctionName());
                }
                oneStubComposition.getCompositionOrClassification().add(stubStepComposition);
            }
            
            if (oneStubComposition != null) {
                if (stubsComposition == null) {
                    stubsComposition = createComposition(STUBS_COMPOSITION_NAME);
                }
                stubsComposition.getCompositionOrClassification().add(oneStubComposition);
            }
        }
        
        return stubsComposition;
    }


    /**
     * Creates composition with name 'HIL', which contains HIL parameters 
     * as classifications.
     */
    private Composition createHILComposition(CTestSpecification testSpec) {
        Composition hilComposition = null;
        
        CTestHIL hil = testSpec.getHIL(true);
        StrVector hilOutputs = hil.getHILParamKeys();
        
        if (hilOutputs.size() > 0) {
            hilComposition = createComposition(HIL_COMPOSITION_NAME);
            
            for (int i = 0; i < hilOutputs.size(); i++) {
                Classification classification = createClassification(hilOutputs.get(i));
                hilComposition.getCompositionOrClassification().add(classification);
            }
        }
        
        return hilComposition;
    }


    /**
     * Creates composition with name 'scripts', which contains script functions 
     * as compositions.
     */
    private Composition createScriptsComposition(CTestSpecification testSpec) {
        Composition mainScriptsComposition = null;
        
        mainScriptsComposition = createScriptFuncComposition(testSpec.getInitTargetFunction(true), 
                                                         mainScriptsComposition, INIT_TARGET_COMPOS_PREFIX);
        mainScriptsComposition = createScriptFuncComposition(testSpec.getInitFunction(true), 
                                                         mainScriptsComposition, INIT_TEST_COMPOS_PREFIX);
        mainScriptsComposition = createScriptFuncComposition(testSpec.getEndFunction(true), 
                                                         mainScriptsComposition, END_TEST_COMPOS_PREFIX);
        mainScriptsComposition = createScriptFuncComposition(testSpec.getRestoreTargetFunction(true), 
                                                         mainScriptsComposition, RESTORE_TARGET_COMPOS_PREFIX);
            
        return mainScriptsComposition;
    }


    /**
     * Creates composition for one script function. It contains 
     * parameters as classifications.
     */
    private Composition createScriptFuncComposition(CTestFunction scriptFunc,
                                                    Composition mainScriptsComposition,
                                                    String prefix) {
        StrVector scriptParams = new StrVector();
        scriptFunc.getPositionParams(scriptParams);
        long numParams = scriptParams.size();
        
        if (numParams > 0) {
            if (mainScriptsComposition == null) {
                mainScriptsComposition = createComposition(SCRIPT_COMPOSITION_NAME);
            }
            
            Composition scriptFuncComposition = 
                    createComposition(createScriptFuncCompositionName(scriptFunc, prefix));
            
            for (int i = 0; i < numParams; i++) {
                Classification classification = 
                        createClassification(createScriptParamClassificationName(i));
                
                addTag(classification.getTagGroup(), ETagType.paramIdx, 
                       "idx", String.valueOf(i));
                
                scriptFuncComposition.getCompositionOrClassification().add(classification);
            }
            mainScriptsComposition.getCompositionOrClassification().add(scriptFuncComposition);
        }
        
        return mainScriptsComposition;
    }

    
    /**
     * This is the main method for attaching classes to classifications created
     * above. For example, if function under test contains parameters 'a',
     * and the set of values for parameter 'a' found in the derived test specs
     * is {1, 4, 7, 9}, then these values are added to classification 'a' as
     * CTE classes. Furthermore, for each derived test spec one CTE Test Case
     * is created.
     * 
     * @param testSpec
     * @param parentGroup
     */
    private void getClassesAndTestCases(CTestSpecification testSpec, 
                                        TestGroup parentGroup) {
        
        TestGroup testCaseGrp = createTestGroup(testSpec.getTestId());
        parentGroup.getTestGroupOrTestCaseOrTestSequence().add(testCaseGrp);
        
        addTestSpecToTestCase(testSpec, testCaseGrp.getTagGroup());
        
        int numDerived = testSpec.getNoOfDerivedSpecs();
        
        for (int tsIdx = 0; tsIdx < numDerived; tsIdx++) {
            CTestSpecification derived = testSpec.getDerivedTestSpec(tsIdx);
            List<Class> testCaseMarks = new ArrayList<si.isystem.cte.model.Class>();
            
            // we should export values as they are executed, which means merged ones
            CTestSpecification mergedTestSpec = derived.merge();
            getParamsClassesAndMarks(mergedTestSpec, testCaseMarks);
            
            getVarsClassesAndMarks(mergedTestSpec, testCaseMarks);
            getStubsClassesAndMarks(mergedTestSpec, testCaseMarks);
            getHILClassesAndMarks(mergedTestSpec, testCaseMarks);
            getScriptsClassesAndMarks(mergedTestSpec, testCaseMarks);
            
            // create test case and add marks to it
            TestCase testCase = createTestCase(derived.getTestId());
            
            Marks marks = m_factory.createMarks();
            marks.getTrue().addAll(testCaseMarks);
            testCase.getMarks().add(marks);
            
            addTestSpecToTestCase(derived, testCase.getTagGroup());
            
            testCaseGrp.getTestGroupOrTestCaseOrTestSequence().add(testCase);
            
            // call recursively for nested test specs / groups
            if (derived.getNoOfDerivedSpecs() > 0) {
                getClassesAndTestCases(derived, testCaseGrp);
            }
        }
    }


    /**
     * This method copies all sections from test spec. to CTE Test Group or
     * CTE Test Case, including test result. Sections are copied as YAML text.
     * See comment of this class for reasoning of YAML vs. attributes.
     * 
     * @param testSpec
     * @param tags
     */
    private void addTestSpecToTestCase(CTestSpecification testSpec, List<Tag> tags) {
        
        // meta
        addTag(tags, ETagType.Description, "textcontent", testSpec.getDescription()); 
        
        // testID is already serialized as Core attribute
        // Desc. is serialized as CTE Description tag above. 
        addTestSpecSectionAsCteTag(testSpec, tags, 
                                   ETagType.meta,
                                   new SectionIds [] {SectionIds.E_SECTION_RUN, 
                                                      SectionIds.E_SECTION_TAGS});
        
        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.imports, 
                                   new SectionIds [] {SectionIds.E_SECTION_IMPORTS}); 
        
        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.function, 
                                   new SectionIds [] {SectionIds.E_SECTION_FUNC}); 
        
        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.persistentVars, 
                                   new SectionIds [] {SectionIds.E_SECTION_PERSIST_VARS}); 
        
        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.variables, 
                                   new SectionIds [] {SectionIds.E_SECTION_LOCALS,
                                                      SectionIds.E_SECTION_INIT}); 
        
        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.expected, 
                                   new SectionIds [] {SectionIds.E_SECTION_ASSERT}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.stubs, 
                                   new SectionIds [] {SectionIds.E_SECTION_STUBS}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.userStubs, 
                                   new SectionIds [] {SectionIds.E_SECTION_USER_STUBS}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.testPoints, 
                                   new SectionIds [] {SectionIds.E_SECTION_TEST_POINTS}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.analyzer, 
                                   new SectionIds [] {SectionIds.E_SECTION_ANALYZER}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.hil, 
                                   new SectionIds [] {SectionIds.E_SECTION_HIL}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.scripts, 
                                   new SectionIds [] {SectionIds.E_SECTION_INIT_TARGET,
                                                      SectionIds.E_SECTION_INITFUNC, 
                                                      SectionIds.E_SECTION_ENDFUNC,
                                                      SectionIds.E_SECTION_RESTORE_TARGET}); 

        addTestSpecSectionAsCteTag(testSpec, tags, ETagType.options, 
                                   new SectionIds [] {SectionIds.E_SECTION_OPTIONS}); 

        // when test spec is exported as CTE Test Group, it may not have a result,
        // otherwise CTE loading fails with NullPointerException.
        // add also test result tag
        CTestResult result = TestSpecificationModel.getActiveModel().getResult(testSpec);
        String cteResult = TEST_RESULT_N_A;
        if (testSpec.getRunFlag() == ETristate.E_FALSE) {
            cteResult = TEST_RESULT_NO_RUN;
        }
        if (result != null) {
            if (result.isException()) {
                cteResult = TEST_RESULT_NOT_COMPLETED;
            } if (result.isError()) {
                cteResult = TEST_RESULT_FAILED;
            } else {
                cteResult = TEST_RESULT_PASSED;
            }
        }
        addTag(tags, ETagType.TestResult, "result", cteResult);
    }


    private void addTestSpecSectionAsCteTag(CTestSpecification testSpec,
                                            List<Tag> tags,
                                            ETagType tagType,
                                            SectionIds [] sections) {

        boolean isAllEmpty = true;
        for (SectionIds section : sections) {
            if (!testSpec.isSectionEmpty(section.swigValue())) {
                isAllEmpty = false;
                break;
            }
        }
        
        if (isAllEmpty) {
            return; // do nothing for empty group of sections
        }
        
        CStringStream out;
        IEmitter emitter;
        out = new CStringStream();
        emitter = EmitterFactory.createYamlEmitter(out);
        emitter.startStream();
        emitter.startDocument(true);
        emitter.mapStart(EYamlStyle.EYAML_BLOCK_STYLE);
        
        for (SectionIds section : sections) {
            testSpec.serializeMember(emitter, section);
        }
        
        emitter.mapEnd();
        emitter.endDocument(true);
        emitter.endStream();
        
        addTag(tags, tagType, "textcontent", out.getString());
    }


    /** Collects set of different param values and adds it as classes to classifications. */
    private void getParamsClassesAndMarks(CTestSpecification testSpec,
                                          List<Class> testCaseMarks) {
        
        Composition paramsComposition = m_mainCompositions.get(PARAMS_COMPOSITION_NAME);
        if (paramsComposition == null) {
            return;
        }
        
        List<Object> paramClassifications = 
                paramsComposition.getCompositionOrClassification();
        
        StrVector params = new StrVector();
        testSpec.getPositionParams(params);

        int numParams = (int)params.size();
        
        if (numParams != paramClassifications.size()) {
            throw new SIllegalArgumentException("The number of classifications is " +
            		"different from the number of function parameters!")
                      .add("numClassifications", paramClassifications.size())
                      .add("numParams", numParams);
        }
        
        
        if (m_paramsValuesMaps.isEmpty()) {  // lazy init
            for (int i = 0; i < numParams; i++) {
                m_paramsValuesMaps.add(new TreeMap<String, si.isystem.cte.model.Class>());
            }
        }
        
        if (params.size() != numParams) {
            throw new SIllegalStateException("Number of parameters differ between " +
            		"selected and derived test specification!").
                add("baseNumParams", numParams).add("derivedNumParams", params.size())
                .add("derivedTestSpecId", testSpec.getTestId());
        }
        
        
        // each unique parameter value is stored as pair<value, cteClass>.
        // The same cteClass is added to paramClassifications, and later
        // the cteClass is also put into test case marks - depending on value. 
        for (int i = 0; i < numParams; i++) {
            String value = params.get(i);
            Map<String, si.isystem.cte.model.Class> valuesMap = m_paramsValuesMaps.get(i);
            if (!valuesMap.containsKey(value)) {
                Class cteClass = createClass(value);
                valuesMap.put(value, cteClass);
                ((Classification)paramClassifications.get(i)).getClazz().add(cteClass);
            }
            testCaseMarks.add(valuesMap.get(value));
        }
    }


    /** Collects set of different var values and adds it as classes to classifications. */
    private void getVarsClassesAndMarks(CTestSpecification testSpec,
                                        List<Class> testCaseMarks) {

        StrStrMap vars = new StrStrMap(); 
        testSpec.getInitMap(vars);

        // prefix should be empty, because there is no need for special qualified names 
        getClassesAndMarksForStrStrMap(testCaseMarks,
                                       m_mainCompositions.get(VARS_COMPOSITION_NAME),
                                       vars,
                                       ""); 
    }


    /** Collects set of different stub assignment values and params and adds it as 
     * classes to classifications. */
    private void getStubsClassesAndMarks(CTestSpecification testSpec,
                                         List<Class> testCaseMarks) {

        CTestBaseList stubs = testSpec.getStubs(true);
        
        Composition mainStubsComposition = m_mainCompositions.get(STUBS_COMPOSITION_NAME);
        if (mainStubsComposition == null) {
            return;
        }
        
        List<Object> stubbedFunctionsCompositions = mainStubsComposition.getCompositionOrClassification();
        
        for (Object stubCompositionObj : stubbedFunctionsCompositions) {
            Composition stubComposition = (Composition)stubCompositionObj;
            for (int i = 0; i < stubs.size(); i++) {
                CTestStub stub = CTestStub.cast(stubs.get(i));
                
                // find a stub matching the stubComposition
                if (stub.getFunctionName().equals(stubComposition.getName())) {
                    
                    CTestBaseList assignSteps = stub.getAssignmentSteps(true);
                    List<Object> stubStepsComposList = stubComposition.getCompositionOrClassification();
                    int stepIdx = 0;
                    for (Object stubStepComposObj : stubStepsComposList) {

                        Composition stubStepCompos = (Composition)stubStepComposObj;
                        CTestEvalAssignStep stubStep = CTestEvalAssignStep.cast(assignSteps.get(stepIdx));
                    
                        List<Object> assignScriptComposList = stubStepCompos.getCompositionOrClassification();
                        for (Object assignScriptComposObj : assignScriptComposList) {
                            
                            if (assignScriptComposObj instanceof Classification) {
                                // it is next step index
                                StrStrMap stepIdxMap = new StrStrMap();
                                String stepIdxStr = stubStep.getStepIdx();
                                if (!stepIdxStr.isEmpty()) {
                                    stepIdxMap.put(COMPOS_STUB_STEP_IDX, stepIdxStr);
                                    getClassesAndMarksForStrStrMap(testCaseMarks,
                                                                   stubStepCompos,
                                                                   stepIdxMap,
                                                                   COMPOS_STUB_STEP_IDX + 
                                                                   '_' + stepIdx + '_' + stub.getFunctionName());
                                }
                                continue;
                            }
                            
                            // if it is composition, we have assignments or script params
                            Composition assignOrScriptCompos = (Composition)assignScriptComposObj;
                            String composName = assignOrScriptCompos.getName();

                            switch (composName) {
                            case COMPOS_ASSIGNMENTS:
                                int assignSection = EStepSectionIds.E_SECTION_ASSIGN.swigValue();
                                StrStrMap assignMap = DataUtils.getMap(stubStep, assignSection);
                                getClassesAndMarksForStrStrMap(testCaseMarks,
                                                               assignOrScriptCompos,
                                                               assignMap,
                                                               COMPOS_ASSIGNMENTS + 
                                                               '_' + stepIdx + '_' + stub.getFunctionName());
                                break;
                            case COMPOS_SCRIPT_PARAMS:

                                StrStrMap scriptParamsMap = new StrStrMap(); 

                                int scriptParamsSection = EStepSectionIds.E_SECTION_SCRIPT_PARAMS.swigValue();
                                StrVector scriptParams = DataUtils.getVector(stubStep, scriptParamsSection);

                                for (int paramIdx = 0; paramIdx < scriptParams.size(); paramIdx++) {
                                    scriptParamsMap.put(createScriptParamClassificationName(paramIdx),
                                                        scriptParams.get(paramIdx));
                                }

                                getClassesAndMarksForStrStrMap(testCaseMarks,
                                                               assignOrScriptCompos,
                                                               scriptParamsMap,
                                                               COMPOS_SCRIPT_PARAMS + 
                                                               '_' + stepIdx + '_' + stub.getFunctionName());
                                break;
                            default:
                                throw new SIllegalStateException("Invalid composition found in stub!")
                                .add("testSpec", testSpec.getTestId())
                                .add("stub", stub.getFunctionName())
                                .add("invalidComposition", composName); 
                            }
                        }
                        
                        stepIdx++;
                    }
                }
            }
        }
    }


    /** Collects set of different HIL values and adds it as classes to classifications. */
    private void getHILClassesAndMarks(CTestSpecification testSpec,
                                       List<Class> testCaseMarks) {

        CTestHIL hil = testSpec.getHIL(true);

        StrStrMap hilParams = new StrStrMap(); 
        hil.getHILParamMap(hilParams);
        
        // prefix should be empty, because there is no need for special qualified names 
        getClassesAndMarksForStrStrMap(testCaseMarks,
                                       m_mainCompositions.get(HIL_COMPOSITION_NAME),
                                       hilParams,
                                       ""); 
    }


    /** Collects set of different stub param values and adds it as classes to classifications. */
    private void getScriptsClassesAndMarks(CTestSpecification testSpec,
                                           List<Class> testCaseMarks) {

        Composition mainScriptsComposition = m_mainCompositions.get(SCRIPT_COMPOSITION_NAME);
        if (mainScriptsComposition == null) {
            return;
        }
        
        createScriptFuncClassesAndMarks(testCaseMarks,
                                        mainScriptsComposition,
                                        testSpec.getInitTargetFunction(true),
                                        INIT_TARGET_COMPOS_PREFIX);
        
        createScriptFuncClassesAndMarks(testCaseMarks,
                                        mainScriptsComposition,
                                        testSpec.getInitFunction(true),
                                        INIT_TEST_COMPOS_PREFIX);
        
        createScriptFuncClassesAndMarks(testCaseMarks,
                                        mainScriptsComposition,
                                        testSpec.getEndFunction(true),
                                        END_TEST_COMPOS_PREFIX);
        
        createScriptFuncClassesAndMarks(testCaseMarks,
                                        mainScriptsComposition,
                                        testSpec.getRestoreTargetFunction(true),
                                        RESTORE_TARGET_COMPOS_PREFIX);
    }


    /** Collects set of different param values for one script function. */
    private void createScriptFuncClassesAndMarks(List<Class> testCaseMarks,
                                                 Composition mainScriptsComposition,
                                                 CTestFunction scriptFunc,
                                                 String prefix) {
        String scriptFuncCompositionName = 
                createScriptFuncCompositionName(scriptFunc, prefix);
        
        List<Object> scriptFuncCompositions = mainScriptsComposition.getCompositionOrClassification();
        
        for (Object compositionObj : scriptFuncCompositions) {
            Composition funcComposition = (Composition)compositionObj;
            
            if (scriptFuncCompositionName.equals(funcComposition.getName())) {

                // prefix must be specified to get unique class (var) name for script functions
                // with the same name - if the user has the same script func. for initTarget
                // and initFunc, they are represented as two distinct classifications in CTE.
                StrVector params = new StrVector();
                scriptFunc.getPositionParams(params);
                
                StrStrMap scriptParamsMap = new StrStrMap();
                for (int i = 0; i < params.size(); i++) {
                    scriptParamsMap.put(createScriptParamClassificationName(i), params.get(i));
                }
                
                getClassesAndMarksForStrStrMap(testCaseMarks,
                                               funcComposition,
                                               scriptParamsMap,
                                               ""); 
            }
        }
    }


    private void getClassesAndMarksForStrStrMap(List<Class> testCaseMarks,
                                                Composition assignmentComposition,
                                                StrStrMap assignMap,
                                                String qualifiedPrefix) {
        
        if (assignmentComposition == null) {
            return;  // there is no composition for vars, stubs, hil, ... in root test spec
        }
        
        List<Object> classifications = assignmentComposition.getCompositionOrClassification();
        
        for (Object classificationObj : classifications) {
            
            if (classificationObj instanceof Composition) {
                // some compositions, for example stubStepComposition consist
                // of Compositions (assignments and scriptParams) AND 
                // Classifications (nextStepIdx). Compositions are ignored here.
                continue;
            }
            
            Classification classification = (Classification)classificationObj;

            String varName = classification.getName();
            String qualifiedVarName = qualifiedPrefix + varName;
            
            String value = null;
            // SWIG inconsistency - throws exception on non-existent key
            if (assignMap.containsKey(varName)) {  
                value = assignMap.get(varName);
            }
            
            if (value != null) { // if test spec may not have assignment for all vars
                Map<String, si.isystem.cte.model.Class> valuesMap = getOrCreateClassMap(m_varsValuesMaps,
                                                                                        qualifiedVarName);
                if (!valuesMap.containsKey(value)) {
                    Class cteClass = createClass(value);
                    valuesMap.put(value, cteClass);
                    classification.getClazz().add(cteClass);
                }
                testCaseMarks.add(valuesMap.get(value));
            }
        }
    }

 
    /**
     * Auto arranges elements in CTE graph. Left edges are arranged (x-pos) 
     * because width of strings in CTE is not known in testIDEA (no font info, ...).
     * Could be improved, but it requires work for something which is already 
     * done in CTE - RMI API call would be preferred.
     * 
     * @param composition
     * @param xPos x-pos of the leftmost class
     * @param deltaX distance between left edges of classes
     * @param yPos vertical pos of the topmost item (root composition)
     * @param deltaY distance between top edges of classes
     * 
     * @return x-pos of the item placed
     */
    private int arrange(Composition composition, int xPos, int deltaX, int yPos, int deltaY) {
        
        int startXPos = xPos;
        int endXPos = xPos;
        composition.setY(yPos);
        yPos += deltaY;
        
        List<Object> children = composition.getCompositionOrClassification();
        for (Object child : children) {
            if (child instanceof Composition) {
                Composition childComposition = (Composition)child;
                xPos = arrange(childComposition, xPos, deltaX, yPos, deltaY);
            } else {
                Classification childClassification = (Classification)child;
                xPos = arrangeClasses(childClassification, xPos, deltaX, yPos, deltaY);
            }
            endXPos = xPos;
            xPos += deltaX;
        }

        int middlePos = startXPos + (endXPos - startXPos)/2;
        composition.setX(middlePos);
        return endXPos;
    }
    
 
    private int arrangeClasses(Classification childClassification, 
                               int xPos, int deltaX, int yPos, int deltaY) {
        int startXPos = xPos;
        int endXPos = xPos;
        childClassification.setY(yPos);
        yPos += deltaY;

        List<Class> classes = childClassification.getClazz();
        for (Class childClass : classes) {
            childClass.setX(xPos);
            childClass.setY(yPos);
            endXPos = xPos;
            xPos += deltaX;
        }
        
        int middlePos = startXPos + (endXPos - startXPos)/2;
        childClassification.setX(middlePos);
        return endXPos;
    }
    
    
    // misc factory and utility methods are following

    private Map<String, si.isystem.cte.model.Class> 
        getOrCreateClassMap(Map<String, Map<String, si.isystem.cte.model.Class>> valuesMaps, 
                            String varName) {
        
        Map<String, si.isystem.cte.model.Class> valuesMap = valuesMaps.get(varName);
        if (valuesMap == null) {
            valuesMap = new TreeMap<String, si.isystem.cte.model.Class>();
            valuesMaps.put(varName, valuesMap);
        }
        return valuesMap;
    }


    private String createScriptFuncCompositionName(CTestFunction scriptFunc,
                                                   String prefix) {
        return prefix + ":" + scriptFunc.getName();
    }


    private String createScriptParamClassificationName(int paramIdx) {
        return "p_" + paramIdx;
    }


    private TestCase createTestCase(String testId) {
        TestCase testCase = m_factory.createTestGroupTestCase();
        testCase.setId(getNextId());
        testCase.setName(testId);
        return testCase;
    }


    private TestGroup createTestGroup(String testId) {
        TestGroup testCaseGrp = m_factory.createTestGroup();
        testCaseGrp.setId(getNextId());
        testCaseGrp.setName(testId);
        return testCaseGrp;
    }


    private si.isystem.cte.model.Class createClass(String value) {
        si.isystem.cte.model.Class cteClass = m_factory.createClass();
        cteClass.setId(getNextId());
        cteClass.setName(value);
        return cteClass;
    }
    

    private Classification createClassification(String name) {
        Classification classification = m_factory.createClassification();
        classification.setId(getNextId());
        classification.setName(name);
        return classification;
    }


    private Composition createComposition(String name) {
        Composition composition = m_factory.createComposition();
        composition.setId(getNextId());
        composition.setName(name);
        return composition;
    }

    
    private void addIfNotNull(List<Object> parentChildren,
                              Composition paramsComposition) {
        if (paramsComposition != null) {
            parentChildren.add(paramsComposition);
        }
    }
    

    private void addIfNotNull(List<Object> parentChildren,
                              Classification classification) {
        if (classification != null) {
            parentChildren.add(classification);
        }
    }
    

    /* Adds tag to List<Tag> from the given CTestBase. Currently not used
     * 
    private void addTag(List<Tag> tags,
                        ETagType tagType,
                        CTestBase testBase,
                        int testSectionId) {
        
        Tag tag = m_factory.createMarkTag();
        
        tag.setType(tagType.toString());
        tag.setId(getNextId());
        
        List<Content> content = tag.getContent();
        Content pair = m_factory.createMarkTagContent();
        
        pair.setKey(testBase.getTagName(testSectionId));
        pair.setValue(testBase.getTagValue(testSectionId));
        
        content.add(pair);
        tags.add(tag);
    } */


    private void addTag(List<Tag> tags,
                        ETagType tagType,
                        String attributeName,
                        String attributeValue) {
        
        Tag tag = m_factory.createMarkTag();
        
        tag.setType(tagType.toString());
        tag.setId(getNextId());
        
        addContentToTag(tag, attributeName, attributeValue);
        tags.add(tag);
    }


    private void addContentToTag(Tag tag,
                                 String attributeName,
                                 String attributeValue) {
        
        List<Content> content = tag.getContent();
        Content pair = m_factory.createMarkTagContent();
        
        pair.setKey(attributeName);
        pair.setValue(attributeValue);
        
        content.add(pair);
    }


    private String getNextId() {
        return "c" + m_idCounter++;
    }
}