package si.isystem.cte;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBase.SpecDataType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestEvalAssignStep.EStepSectionIds;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestHIL.ETestHILSections;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.cte.CteExporter.ETagType;
import si.isystem.cte.model.Class;
import si.isystem.cte.model.Classification;
import si.isystem.cte.model.Composition;
import si.isystem.cte.model.CteObject;
import si.isystem.cte.model.Mark.Tag;
import si.isystem.cte.model.Mark.Tag.Content;
import si.isystem.cte.model.Marks;
import si.isystem.cte.model.TestGroup;
import si.isystem.cte.model.TestGroup.TestCase;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.AddTestTreeNodeAction;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.mk.utils.PlatformUtils;

/**
 * This class imports CTE XML files to testIDEA. Import is done in two phases -
 * first CTE classes (values) are stored into map with appropriate converter object.
 * Then CTE test steps are parsed - CTE classes and converters from the map are 
 * used to set values in test specifications.
 * 
 * @author markok
 *
 */
public class CteImporter {

    // Prefix used to indent comments set in CTE, to differ them from information
    // added by this class, for example names of classifications and classes.
    // Indentation with spaces does not work, because testIDEA trimms them from comments.
    private static final String CTE_DESC_PREFIX = "- ";
    private Shell m_shell;
    // TreeMap requires key objects to implement Comparable! 
    private Map<Class, Cte2ITestConverter> m_cteClassMap = new HashMap<>();
    
    private Set<String> m_itestTetSpecTagTypes = new TreeSet<>();

    
    public CteImporter() {
        m_itestTetSpecTagTypes.addAll(Arrays.asList(new String[] 
                                  {ETagType.meta.toString(), 
                                   ETagType.imports.toString(), 
                                   ETagType.function.toString(), 
                                   ETagType.persistentVars.toString(), 
                                   ETagType.variables.toString(), 
                                   ETagType.expected.toString(), 
                                   ETagType.stubs.toString(), 
                                   ETagType.userStubs.toString(), 
                                   ETagType.testPoints.toString(), 
                                   ETagType.analyzer.toString(), 
                                   ETagType.hil.toString(), 
                                   ETagType.scripts.toString(), 
                                   ETagType.options.toString()}));
    }
    
    
    public void importFromFile(CTestSpecification containerTestSpec,
                               String fileName) throws JAXBException, 
                                                       SAXException, 
                                                       IOException {
        
        if (containerTestSpec.getNoOfDerivedSpecs() > 1) {
            MessageDialog.openWarning(Activator.getShell(), "Error in selection", 
                                      containerTestSpec.getNoOfDerivedSpecs() + 
                                      " test specifications are selected, but only one can be " +
                                      "imported from one file.\n\nPlease make sure that " +
                                      "the 'Import data only to selected test specifications' " +
                                      "check box in the import dialog is checked.");
            return;
        }
        
        if (containerTestSpec.getNoOfDerivedSpecs() == 0) {
            MessageDialog.openError(Activator.getShell(), "Error in selection", 
                    "Please select test specification to import data into.");
            return;
        }
        
        CteObject cteObject = unmarshallFromFile(fileName);
        
        List<Object> testGroups = cteObject.getTestGroup().getTestGroupOrTestCaseOrTestSequence();
        
        if (testGroups.isEmpty()) {
            MessageDialog.openInformation(m_shell, "Import", "No test cases were found!");
            return;
        }
        
        if (testGroups.size() != 1) {
            throw new SIllegalArgumentException("Only one test group may be specified " +
            		"at the highest level.\n" +
                    "Move other groups below the top level group and retry import.");
        }
        
        Object mainTestGroupObj = testGroups.get(0);
        
        if (mainTestGroupObj instanceof TestGroup) {

            TestGroup mainTestGroup = (TestGroup)mainTestGroupObj;
            CTestSpecification mainTestSpec = containerTestSpec.getDerivedTestSpec(0);
            
            if (!mainTestGroup.getName().equals(mainTestSpec.getTestId())) {
                if (MessageDialog.openQuestion(m_shell, "Import", "Test ID of the selected test specification " +
                		"does not match the imported test ID. The selected test case and all its derived" +
                		"tests wil be deleted.\n\nDo you want to proceed with import?") == false) {
                    return;
                }
            }

            CTestSpecification newContainerTestSpec = new CTestSpecification();
            walkTestCases(newContainerTestSpec, mainTestGroup);
            
            execAction(mainTestSpec, newContainerTestSpec);

        } else {
            throw new SIllegalArgumentException("The main tree object should be Test " +
            		"Group, but it is " + mainTestGroupObj.getClass().getSimpleName());
        }
    }


    private CteObject unmarshallFromFile(String fileName) throws JAXBException,
                                                                 SAXException,
                                                                 IOException {
        JAXBContext jc = JAXBContext.newInstance(CteExporter.ISYSTEM_CTE_MODEL_PACKAGE_NAME);

        Unmarshaller unmarshaller = jc.createUnmarshaller();
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(PlatformUtils.getURLFromPluginPath(CteExporter.CTE_SCHEMA_LOCATION));
        unmarshaller.setSchema(schema);

        // Note that a Collection here is a test.jaxb.Collection, not a java.util.Collection.
        CteObject cteObject= (CteObject)unmarshaller.unmarshal(new File(fileName));

        List<Object> mainCompositions = cteObject.getTree().getCompositionOrClassificationOrClazz();
        m_shell = Activator.getShell();
        if (mainCompositions.isEmpty()) {
            throw new SIllegalArgumentException("No compositions found, nothing will be imported.");
        }

        if (mainCompositions.size() != 1) {
            throw new SIllegalArgumentException("Only one root composition is allowed in CTE model. Nothing will be imported.");
        }

        createCteClassPathMap(mainCompositions.get(0));
        return cteObject;
    }


    private void walkTestCases(CTestSpecification testSpec, TestGroup testGroup) {

        // get test spec from tags 
        CTestSpecification derivedBaseTestSpec = copyYamlFromCte2CTestSpec(testGroup.getTagGroup(), 
                                                                           testGroup.getName());
        testSpec.addChildAndSetParent(-1, derivedBaseTestSpec);
        
        List<Object> testItems = testGroup.getTestGroupOrTestCaseOrTestSequence();
        for (Object testGroupOrTestCaseObj : testItems) {
            
            if (testGroupOrTestCaseObj instanceof TestGroup) {

                walkTestCases(derivedBaseTestSpec, (TestGroup)testGroupOrTestCaseObj);

            } else if (testGroupOrTestCaseObj instanceof TestCase) {
                TestCase testCase = (TestCase) testGroupOrTestCaseObj;

                // get test spec from tags 
                CTestSpecification derivedTestSpec = 
                        copyYamlFromCte2CTestSpec(testCase.getTagGroup(), testCase.getName());

                // important to add as child before importing marks, so that empty 
                // sections can get merged 
                derivedBaseTestSpec.addChildAndSetParent(-1, derivedTestSpec);
                
                // now update test spec according to Test Case marks
                copyMarksFromCteToCTestSpec(testCase, derivedTestSpec);  
                
            } else {
                throw new SIllegalArgumentException("The object should be Test Group or TestCase, but it is " + 
                        testGroupOrTestCaseObj.getClass().getSimpleName()); 
            }
        }
    }

    
    private CTestSpecification copyYamlFromCte2CTestSpec(List<Tag> tags, String testSpecId) {
        StringBuilder sb = new StringBuilder();
        
        for (Tag tag : tags) {
            String tagType = tag.getType();
            if (m_itestTetSpecTagTypes.contains(tagType)) {
                List<Content> contentList = tag.getContent();
                // it is possible to delete contents of properties tab in CTE,
                // but the tab remains - there is no Content XML element in such case. 
                if (contentList.size() >= 1) {
                    sb.append(contentList.get(0).getValue());
                }
            }
        }
        
        CTestSpecification testSpec = CTestSpecification.parseTestSpec(sb.toString());
        // CTE creates test cases with spaces in names, but spaces are not allowed
        // in testIDEA test ID, so replace them with '_'. See mail from AM, 18.3.2014
        testSpecId = testSpecId.replace(' ', '_');
        testSpec.setTestId(testSpecId);
        return testSpec;
    }


    private void copyMarksFromCteToCTestSpec(TestCase testCase,
                                             CTestSpecification testSpec) {
        List<Marks> marks = testCase.getMarks();
        for (Marks mark : marks) {
            List<Object> cteClassObjects = mark.getTrue();
            for (Object cteClassObj : cteClassObjects) {
                if (!(cteClassObj instanceof Class)) {
                    throw new SIllegalArgumentException("Marks should contain only " +
                       "references to CTE Class-es, but contains " + 
                            cteClassObj.getClass().getSimpleName());
                }
                Class cteClass = (Class) cteClassObj;
                Cte2ITestConverter converter = m_cteClassMap.get(cteClass);
                if (converter == null) {
                    throw new SIllegalArgumentException("Internal error - no parent for class '"
                                                        + cteClass.getName() + "' found.");
                }
                converter.cte2TestSpecSection(testSpec, cteClass);
            }
        }
    }


    private void execAction(CTestSpecification mainTestSpec,
                            CTestSpecification newContainerTestSpec) {
        
        if (newContainerTestSpec.getNoOfDerivedSpecs() != 1) {
            throw new SIllegalArgumentException("There may be only one top level " +
                    "parent group in CTE, but " + newContainerTestSpec.getNoOfDerivedSpecs() +
                    " were found.");
        }
        
        // import has been successful, let's delete old test specs and add new ones
        GroupAction importAction = new GroupAction("Import");
        importAction.add(new DeleteTestTreeNodeAction(mainTestSpec));

        CTestSpecification parentTestSpec = mainTestSpec.getParentTestSpecification();
        int mainTestSpecIdx = parentTestSpec.findDerivedTestSpec(mainTestSpec);
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        importAction.add(new AddTestTreeNodeAction(model,
                                                   parentTestSpec, 
                                                   mainTestSpecIdx, 
                                                   newContainerTestSpec.getDerivedTestSpec(0)));
        importAction.addAllFireEventTypes();
        model.execAction(importAction);
    }
    
    
    private void createCteClassPathMap(Object composition) {
        
        if (!(composition instanceof Composition)) {
            throw new SIllegalArgumentException("Invalid type of root object - " +
            		"should be composition, but it is " + composition.getClass().getSimpleName());
            
        }
        Composition mainComposition = (Composition) composition;
        
        List<Object> sectionCompositions = mainComposition.getCompositionOrClassification();
        
        for (Object sectionCompositionObj : sectionCompositions) {
            if (!(sectionCompositionObj instanceof Composition)) {
                throw new SIllegalArgumentException("Invalid type of test section object - " +
                        "should be composition, but it is " + composition.getClass().getSimpleName());
            }
            
            Composition sectionComposition = (Composition)sectionCompositionObj;
            switch(sectionComposition.getName()) {
            case CteExporter.PARAMS_COMPOSITION_NAME:
                ParamsConverter converter = new ParamsConverter();
                cteComposition2ClassMap(sectionComposition, converter);
                break;
            case CteExporter.VARS_COMPOSITION_NAME:
                VarsConverter varsConverter = new VarsConverter();
                cteComposition2ClassMap(sectionComposition, varsConverter);
                break;
            case CteExporter.STUBS_COMPOSITION_NAME:
                List<Object> stubCompositions = sectionComposition.getCompositionOrClassification();
                for (Object stubbedFuncCompositionObj : stubCompositions) {
                    if (!(stubbedFuncCompositionObj instanceof Composition)) {
                        throw new SIllegalArgumentException("Stubs composition " +
                        		"may contain only compositions, but it contains " +
                        		stubbedFuncCompositionObj.getClass().getSimpleName());
                    }
                    Composition stubbedFuncComposition = (Composition)stubbedFuncCompositionObj;
                    List<Object> stubStepsComposList = stubbedFuncComposition.getCompositionOrClassification();
                    int stepIdx = 0;
                    for (Object stubStepCompositionObj : stubStepsComposList) {

                        Composition stubStepComposition = (Composition)stubStepCompositionObj;
                        
                        List<Object> assignmentParamsNextStepList = stubStepComposition.getCompositionOrClassification();
                        
                        for (Object assignmentParamsNextStepObj : assignmentParamsNextStepList) {
                            
                            if (assignmentParamsNextStepObj instanceof Classification) {
                                // it is next step index classification
                                StubStepIndexConverter idxConverter = new StubStepIndexConverter();
                                idxConverter.setStubbedFuncName(stubbedFuncComposition.getName());
                                idxConverter.setStepIdx(stepIdx);
                                cteVarOrParamComposition2Map(idxConverter, 
                                                             assignmentParamsNextStepObj);
                                continue;
                            }
                            
                            if (!(assignmentParamsNextStepObj instanceof Composition)) {
                                throw new SIllegalArgumentException("Stubbed function composition " +
                                        "may contain only compositions, but it contains " +
                                        assignmentParamsNextStepObj.getClass().getSimpleName());
                            }
                            Composition assignmentAndOrParamsComposition = (Composition)assignmentParamsNextStepObj;
                            String assignmentOrParamsCompositionName = assignmentAndOrParamsComposition.getName();
                            switch (assignmentOrParamsCompositionName) {
                            case CteExporter.COMPOS_ASSIGNMENTS:
                                StubAssignmentsConverter stubsAssConverter = new StubAssignmentsConverter();
                                stubsAssConverter.setStubbedFuncName(stubbedFuncComposition.getName());
                                stubsAssConverter.setStepIdx(stepIdx);
                                cteComposition2ClassMap(assignmentAndOrParamsComposition, stubsAssConverter);
                                break;
                            case CteExporter.COMPOS_SCRIPT_PARAMS:
                                StubSParamsConverter stubSParamsConverter = new StubSParamsConverter();
                                stubSParamsConverter.setStubbedFuncName(stubbedFuncComposition.getName());
                                stubSParamsConverter.setStepIdx(stepIdx);
                                cteComposition2ClassMap(assignmentAndOrParamsComposition, stubSParamsConverter);
                                break;
                            default:
                                throw new SIllegalArgumentException("Stubbed function composition may only contain '" +
                                        CteExporter.COMPOS_ASSIGNMENTS + "' or '" +
                                        CteExporter.COMPOS_SCRIPT_PARAMS + "' compositions, but it contains '" +
                                        assignmentOrParamsCompositionName + "' composition.");
                            }
                        }
                        stepIdx++;
                    }
                }
                break;
            case CteExporter.HIL_COMPOSITION_NAME:
                HilConverter hilConverter = new HilConverter();
                cteComposition2ClassMap(sectionComposition, hilConverter);
                break;
            case CteExporter.SCRIPT_COMPOSITION_NAME:
                List<Object> scriptFuncCompositions = sectionComposition.getCompositionOrClassification();
                for (Object scriptFuncCompositionObj : scriptFuncCompositions) {
                    if (!(scriptFuncCompositionObj instanceof Composition)) {
                        throw new SIllegalArgumentException("Stubs composition " +
                                "may contain only compositions, but it contains " +
                                scriptFuncCompositionObj.getClass().getSimpleName());
                    }
                    Composition scriptFuncComposition = (Composition)scriptFuncCompositionObj;
                    ScriptsConverter scriptConverter = new ScriptsConverter(scriptFuncComposition.getName());
                    cteComposition2ClassMap(scriptFuncComposition, scriptConverter);
                }
                break;
            default:
                throw new SIllegalArgumentException("Invalid name of section composition." +
                		"Should be one of: " + CteExporter.PARAMS_COMPOSITION_NAME + ", " +
                		CteExporter.VARS_COMPOSITION_NAME + ", " +
                		CteExporter.STUBS_COMPOSITION_NAME + ", " +
                		CteExporter.HIL_COMPOSITION_NAME + ", " +
                		CteExporter.SCRIPT_COMPOSITION_NAME + ",\n" +
                		"but it is " + sectionComposition.getName());
            }
        }
        
        
    }

    
    /**
     * Stores to global map the given cteClass and appropriate converter. This 
     * converter is called later when CTE test cases are copied to testIDEA test specs.  
     * 
     * @param sectionComposition maps to testIDEA sections f. params, variables, stubs, hil, ...
     *                           Immediate children are Classifications with names of 
     *                           parameters (param_0, ...), variables, ...
     *                           Additional Classification children may follow,
     *                           but are ignored, except for comments (Description in CTE). 
     * @param converter
     * @return comment, currently not used, see comment in cteClasses2Map() below.
     */
    private void cteComposition2ClassMap(Composition sectionComposition,
                                         Cte2ITestConverter converter) {
        
        String cteComment = converter.getCteComment(sectionComposition, false);
        String sectionComment = cteComment.isEmpty() ? "" : cteComment + '\n';
        converter.setSectionComment(sectionComment);
        
        List<Object> classifications = sectionComposition.getCompositionOrClassification();
        for (Object classificationObj : classifications) {
            if (!(classificationObj instanceof Classification)) {
                throw new SIllegalArgumentException("Object should be of type classification," +
                		"but it is " + classificationObj.getClass().getSimpleName()).
                		add("parent", sectionComposition.getName());
            }
             
            cteVarOrParamComposition2Map(converter, 
                                         classificationObj);
        }
    }


    private void cteVarOrParamComposition2Map(Cte2ITestConverter converter,
                                              Object classificationObj) {
        
        Classification varOrParamClassification = (Classification) classificationObj;
        String varOrParamName = varOrParamClassification.getName();
        
        // only params classifications have idx, vars classifications will return null
        String paramIdx = getParamPosFromClassification(varOrParamClassification);
        
        cteClasses2Map(varOrParamClassification, converter, varOrParamName,
                       paramIdx, "");
    }
    

    /**
     * Fills classes to global map, which is used later when creating test cases
     * according to Marks.
     *  
     * @param converter
     * @param varOrParamName name of variable or parameter got from immediate 
     *                        child of composition 
     * @param paramIdx null for variables, index for parameters
     * @param classificationObj intermediate 
     * @return classification comment. This comment is currently not used, but
     * should be set on tag 'params' for function. For variables it is already used
     * as a comment for assignment.
     */
    private void cteClasses2Map(Classification classification,
                                Cte2ITestConverter converter,
                                String varOrParamName, 
                                String paramIdx,
                                String comment) {
        
        String cteComment = converter.getCteComment(classification, false);
        if (!cteComment.isEmpty()) {
            comment += "  # " + classification.getName() + ":\n" + cteComment + '\n';
        }
        
        List<Class> cteClasses = classification.getClazz();

        // create new converter instance per classification - paramIdx is stored there
        converter = converter.copy();
        
        // Classification can only have classes as children, however classes may
        // have compositions and classifications as children. The rule used in 
        // this import: If CTE class has children, then traverse children until classes
        // without children are found. These are then used as values. Intemediate
        // classes, classifications, and compositions are ignored. Example:
        //            testCase
        //           /
        //        params          <- Composition
        //        /    \
        //     param_0 param_1    <- Classification, used to create params and vars in testIDEA
        //     /
        //   Below Threshold      <- Class, optional, ignored, not created on export
        //     |
        // Possibilities          <- Composition, optional, ignored, not created on export
        //     |
        // AnotherClassification  <- Classification, optional, ignored, not created on export
        //     |
        //     0                  <- value used in testIDEA
        //
        // In CTE the following is possible: 
        // - Composition may contain (Compositions, Classifications)
        // - Classification may contain (Classes)
        // - Class may contain (Compositions, Classifications)

        for (Class cteClass : cteClasses) {
            List<Object> compositionsOrClassifications = cteClass.getCompositionOrClassification();
            int numChildren = compositionsOrClassifications.size();
            if (numChildren != 0) {
                String cteComment2 = converter.getCteComment(cteClass, false);
                if (!cteComment2.isEmpty()) {
                    comment += "# " + cteClass.getName() + ": \n" + cteComment2 + '\n';
                }
                // These compositions and classifications may be added by users, and are ignored by testIDEA,
                // because testIDEA has no equivalent data structure. They are lost 
                // in case of round-trip.
                cteWalkCompositionsOrClassifications(compositionsOrClassifications,
                                                     converter,
                                                     varOrParamName,
                                                     paramIdx,
                                                     comment);
                // throw new SIllegalArgumentException("No children are allowed for CTE classes. " +
                //		"Class " + cteClass.getName() + " has " + numChildren + " children.");
            } else {
                converter.setIdentifier(varOrParamName, paramIdx, comment);
                m_cteClassMap.put(cteClass, converter);
            }
        }
    }

    
    private void cteWalkCompositionsOrClassifications(List<Object> compositionsOrClassifications, 
                                                      Cte2ITestConverter converter, 
                                                      String varOrParamName, 
                                                      String paramIdx,
                                                      String comment) {
        
        for (Object compositionOrClassification : compositionsOrClassifications) {
            
            if (compositionOrClassification instanceof Composition) {
                
                Composition composition = (Composition)compositionOrClassification;
                String cteComment = converter.getCteComment(composition, false);
                if (!cteComment.isEmpty()) {
                    comment += "  # " + composition.getName() + ":\n" + cteComment + '\n';
                }
                
                List<Object> childComposOrClassifications = composition.getCompositionOrClassification();
                cteWalkCompositionsOrClassifications(childComposOrClassifications,
                                                     converter,
                                                     varOrParamName,
                                                     paramIdx,
                                                     comment); 

            } else if (compositionOrClassification instanceof Classification) {
                
                Classification classification = (Classification)compositionOrClassification;
                cteClasses2Map(classification, converter, varOrParamName, paramIdx, comment);
            }
        }
    }

    
    private String getParamPosFromClassification(Classification classification) {
        
        String paramsIdx = null;
        
        List<Tag> tags = classification.getTagGroup();
        for (Tag tag : tags) {
            if (tag.getType().equals(CteExporter.FUNC_PARAM_IDX)) {
                List<Content> contentList = tag.getContent();
                if (contentList.size() != 1) {
                    throw new SIllegalArgumentException("The tag of type '" + 
                            CteExporter.FUNC_PARAM_IDX + "' tag should have exactly 1 " +
                            "key/value pair, but it has " + contentList.size() + "of them.");
                }
                paramsIdx = contentList.get(0).getValue();
            }
        }

        return paramsIdx;
    }
    


    ///////////////////////////////////////////////////////////////////////////
    
    
    
    abstract class Cte2ITestConverter {

        protected String m_varOrParamName;
        protected String m_paramsIdxStr;
        protected String m_sectionComment = "";
        protected String m_parentComment;

        public void assign(Cte2ITestConverter parent) {
            m_varOrParamName = parent.m_varOrParamName;
            m_paramsIdxStr = parent.m_paramsIdxStr;
            m_sectionComment = parent.m_sectionComment;
            m_parentComment = parent.m_parentComment;
        }
        
        
        public void setIdentifier(String varOrParamName,
                                  String paramsIdxStr, 
                                  String parentComment) {
            m_varOrParamName = varOrParamName;
            m_paramsIdxStr = paramsIdxStr;
            m_parentComment = parentComment;
        }

        
        public void setSectionComment(String sectionComment) {
            m_sectionComment = sectionComment;
        }


        abstract Cte2ITestConverter copy();
        
        abstract void cte2TestSpecSection(CTestSpecification testSpec, 
                                 si.isystem.cte.model.Class cteClass);

        /**
         * Reads tag 'Description' from the given CTE class and returns it as
         * a comment.  
         * 
         * @param isEolComment if true, ell new lines in the CTE comment are
         * replaced with '#' signs, because EOL comments are always single line 
         * comments.
         */
        protected String getCteComment(Object cteObject, boolean isEolComment) {
            String comment = "";
            List<Tag> tags;
            if (cteObject instanceof Class) {
                tags = ((Class)cteObject).getTagGroup();
            } else if (cteObject instanceof Classification) {
                tags = ((Classification)cteObject).getTagGroup();
            } else {
                tags = ((Composition)cteObject).getTagGroup();
            }
            for (Tag tag : tags) {
                if (tag.getType().equals("Description")) {
                    List<Content> contents = tag.getContent();
                    for (Content content : contents) {
                        comment = "  # " + CTE_DESC_PREFIX + content.getValue().trim();
                        
                        if (isEolComment) {
                            // Convert multiline comment to single line comment,
                            // as EOL comments in YAML can only be in single line. '#' is used as separator
                            // instead of '\n'. If required, convert it back to '\n' on CTE export.
                            comment = StringUtils.replace(comment, "\n", "  # ");
                        } else {
                            // indent all lines to distinguish CTE comment from content added by importer.
                            comment = StringUtils.replace(comment, "\n", "\n# " + CTE_DESC_PREFIX);
                        }
                    }
                }
            }
            return comment;
        }


        protected void setFuncParam(Class cteClass, CTestFunction scriptFunc) {
            
            CSequenceAdapter seqParams = new CSequenceAdapter(scriptFunc, 
                                                              CTestFunction.ESection.E_SECTION_PARAMS.swigValue(),
                                                              false);
            
            int paramsIdx = Integer.valueOf(m_paramsIdxStr);
            // params may not be set in order (classifications may not be ordered
            // the same as func params), so we have to allocate place for them
            // By assigning '' to unallocated params we make sure, evaluator will
            // report an error, if param will not be assigned later. 
            while (paramsIdx >= seqParams.size()) { 
                seqParams.add(-1, "''");
            }

            String value = cteClass.getName();
            seqParams.setValue(paramsIdx, value);

            // Add comment from CTE. It is merged from all values and params to CTestFunction
            // params section, but it can not be split on export. 

            int section = CTestFunction.ESection.E_SECTION_PARAMS.swigValue();
            String oldComment = scriptFunc.getComment(section, 
                                                      SpecDataType.KEY, 
                                                      CommentType.NEW_LINE_COMMENT);
            String comment = buildComment(cteClass, oldComment);
            
            if (!comment.isEmpty()) {
                scriptFunc.setComment(section, 
                                      SpecDataType.KEY, 
                                      CommentType.NEW_LINE_COMMENT, 
                                      comment);
            }
        }


        protected String buildComment(Class cteClass, String oldComment) {
            
            String value = cteClass.getName();
            String cteComment = getCteComment(cteClass, false);
            
            if (!cteComment.isEmpty()  ||  !m_parentComment.isEmpty()  ||  !m_sectionComment.isEmpty()) {
                
                String prefixComment = "";
                if (m_paramsIdxStr != null  &&  !m_paramsIdxStr.isEmpty()) {
                    prefixComment = "  # p_" + m_paramsIdxStr + ", "; 
                } else {
                    prefixComment = "  # ";
                }
                cteComment = prefixComment + value + ":\n" 
                             + m_parentComment + "\n" + cteComment;
 
                // prepend old comment
                if (!oldComment.isEmpty()) {
                    cteComment = oldComment + '\n' + cteComment;
                } else {
                    // add section comment only once
                    if (!m_sectionComment.isEmpty()) { 
                        cteComment = m_sectionComment + '\n' + cteComment;
                    }
                }
            }
            
            return cteComment + '\n';
        }
        
        
        /* protected void setStrVectorItem(Class cteClass, StrVector params) {
            if (m_paramsIdxStr == null) {
                throw new SIllegalArgumentException("Missing tag with type '" + 
                        CteExporter.FUNC_PARAM_IDX + "' in CTE class '" + cteClass.getName() + 
                        "'.");
            }
            
            int paramsIdx = Integer.valueOf(m_paramsIdxStr);

            // params may not be set in order (classifications may not be ordered
            // the same as func params), so we have to allocate place for them
            // By assigning '' to unallocated params we make sure, evaluator will
            // report an error, if param will not be assigned later. 
            while (paramsIdx >= params.size()) { 
                params.add("''");
            }
            
            String value = cteClass.getName();
            params.set(paramsIdx, value);
        } */
    }
    
    
    class ParamsConverter extends Cte2ITestConverter {

        @Override
        Cte2ITestConverter copy() {
            ParamsConverter converter = new ParamsConverter();
            converter.assign(this);
            return converter;
        }
        
        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {

            if (m_paramsIdxStr == null) {
                throw new SIllegalArgumentException("Missing tag with type '" + 
                        CteExporter.FUNC_PARAM_IDX + "' in CTE class '" + cteClass.getName() + 
                        "'.");
            }
            
            setFuncParam(cteClass, testSpec.getFunctionUnderTest(false));
        }
    }
    
    
    class VarsConverter extends Cte2ITestConverter {

        @Override
        Cte2ITestConverter copy() {
            VarsConverter converter = new VarsConverter();
            converter.assign(this);
            return converter;
        }
        
        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {

            CTestSpecification mergedTestSpec = testSpec;
            if (testSpec.isSectionEmpty(SectionIds.E_SECTION_INIT.swigValue())) {
                mergedTestSpec = testSpec.merge(); // empty sections get merged from parent and then assigned in derived
            }
            CMapAdapter initMap = new CMapAdapter(mergedTestSpec,
                                                  SectionIds.E_SECTION_INIT.swigValue(),
                                                  false);

            // keep the existing NL comment
            String oldComment = initMap.getComment(CommentType.NEW_LINE_COMMENT, 
                                                   m_varOrParamName);
            String eolComment = initMap.getComment(CommentType.END_OF_LINE_COMMENT, 
                                                   m_varOrParamName);
            
            String nlComment = buildComment(cteClass, oldComment);
            String value = cteClass.getName();
            
            initMap.setValue(m_varOrParamName, value, nlComment, eolComment);
        }
    }
    
    
    abstract class StubConverterBase extends Cte2ITestConverter {
        protected String m_stubbedFuncName;
        protected int m_stepIdx;
        
        @Override
        public void assign(Cte2ITestConverter parent) {
            StubConverterBase stubParent = (StubConverterBase)parent;
            super.assign(parent);
            m_stubbedFuncName = stubParent.m_stubbedFuncName;
            m_stepIdx = stubParent.m_stepIdx;
        }
        
        
        public void setStubbedFuncName(String stubbedFuncName) {
            m_stubbedFuncName = stubbedFuncName;
        }


        public void setStepIdx(int stepIdx) {
            m_stepIdx = stepIdx;
        }
    }
    
    
    class StubAssignmentsConverter extends StubConverterBase {

        @Override
        Cte2ITestConverter copy() {
            StubAssignmentsConverter converter = new StubAssignmentsConverter();
            converter.assign(this);
            return converter;
        }

        
        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {
            
            CTestStub stub = getMergedStub(testSpec, m_stubbedFuncName);
            CTestBaseList stubSteps = stub.getAssignmentSteps(false);
            while (stubSteps.size() <= m_stepIdx) {
                stubSteps.add(-1, new CTestEvalAssignStep(stub));
            }
            
            CTestEvalAssignStep step = CTestEvalAssignStep.cast(stubSteps.get(m_stepIdx));
            
            EStepSectionIds assignSection = CTestEvalAssignStep.EStepSectionIds.E_SECTION_ASSIGN;
            
            String value = cteClass.getName();
            
            CMapAdapter assignmentsMap = new CMapAdapter(step, assignSection.swigValue(), false);
            
            String oldComment = "";
            String eolComment = "";
            if (assignmentsMap.contains(m_varOrParamName)) {
                oldComment = assignmentsMap.getComment(CommentType.NEW_LINE_COMMENT, 
                                                       m_varOrParamName);
                eolComment = assignmentsMap.getComment(CommentType.END_OF_LINE_COMMENT, 
                                                       m_varOrParamName);
            }
            String nlComment = buildComment(cteClass, oldComment);
            
            assignmentsMap.setValue(m_varOrParamName, value, nlComment, eolComment);
        }
    }
    
    
    class StubSParamsConverter extends StubConverterBase {

        @Override
        Cte2ITestConverter copy() {
            StubSParamsConverter copy = new StubSParamsConverter();
            copy.assign(this);
            return copy;
        }
        

        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {
            CTestStub stub = getMergedStub(testSpec, m_stubbedFuncName);
            CTestBaseList stubSteps = stub.getAssignmentSteps(false);
            while (stubSteps.size() <= m_stepIdx) {
                stubSteps.add(-1, new CTestEvalAssignStep(stub));
            }
            
            CTestEvalAssignStep step = CTestEvalAssignStep.cast(stubSteps.get(m_stepIdx));

            int section = CTestEvalAssignStep.EStepSectionIds.E_SECTION_SCRIPT_PARAMS.swigValue();
            
            CSequenceAdapter params = new CSequenceAdapter(step, section, false);
            if (m_paramsIdxStr == null) {
                throw new SIllegalArgumentException("Missing tag with type '" + 
                        CteExporter.FUNC_PARAM_IDX + "' in CTE class '" + cteClass.getName() + 
                        "'.");
            }
            
            int paramsIdx = Integer.valueOf(m_paramsIdxStr);

            if (params.size() <= paramsIdx) {
                params.resize(paramsIdx + 1);
            }
            
            String value = cteClass.getName();
            params.setValue(paramsIdx, value);
            
            // Stub step script params have comments visible and settable in KTable,
            // while function params have tag comment visible and editable.
            // This is inconsistency, which has to be fixed. TODO 
            String oldComment = params.getComment(CommentType.NEW_LINE_COMMENT,
                                                  paramsIdx);
            String nlComment = buildComment(cteClass, oldComment);
            
            if (!nlComment.isEmpty()) {
                String eolComment = params.getComment(CommentType.END_OF_LINE_COMMENT,
                                                      paramsIdx);
                params.setComment(paramsIdx, 
                                  nlComment,
                                  eolComment);
            }
        }
    }
    
    
    class StubStepIndexConverter extends StubConverterBase {

        @Override
        Cte2ITestConverter copy() {
            StubStepIndexConverter copy = new StubStepIndexConverter();
            copy.assign(this);
            return copy;
        }
        

        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {
            CTestStub stub = getMergedStub(testSpec, m_stubbedFuncName);
            CTestBaseList stubSteps = stub.getAssignmentSteps(false);
            while (stubSteps.size() <= m_stepIdx) {
                stubSteps.add(-1, new CTestEvalAssignStep(stub));
            }
            
            CTestEvalAssignStep step = CTestEvalAssignStep.cast(stubSteps.get(m_stepIdx));

            int section = CTestEvalAssignStep.EStepSectionIds.E_SECTION_NEXT_INDEX.swigValue();
            
            
            String value = cteClass.getName();
            
            step.setTagValue(section, value);
            
            String oldComment = step.getComment(section, 
                                                SpecDataType.KEY, 
                                                CommentType.NEW_LINE_COMMENT);
            String nlComment = buildComment(cteClass, oldComment);
            
            step.setComment(section, SpecDataType.KEY,
                            CommentType.NEW_LINE_COMMENT, 
                            nlComment);
        }
    }

    
    
    class HilConverter extends Cte2ITestConverter {
        
        @Override
        Cte2ITestConverter copy() {
            HilConverter converter = new HilConverter();
            converter.assign(this);
            return converter;
        }
        
        
        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {
            CTestHIL hil;
            
            if (testSpec.isSectionEmpty(SectionIds.E_SECTION_HIL.swigValue())) {
                // empty sections get merged from parent and then assigned in derived
                CTestSpecification mergedTestSpec = testSpec.merge(); 
                CTestHIL mergedHil = mergedTestSpec.getHIL(true);
                hil = testSpec.getHIL(false);
                hil.assign(mergedHil);
            } else {
                hil = testSpec.getHIL(true);
            }
            
            CMapAdapter hilMap = new CMapAdapter(hil, 
                                                 ETestHILSections.E_SECTION_HIL_PARAMS.swigValue(),
                                                 false);
            
            String oldComment = hilMap.getComment(CommentType.NEW_LINE_COMMENT, 
                                                   m_varOrParamName);
            String eolComment = hilMap.getComment(CommentType.END_OF_LINE_COMMENT, 
                                                   m_varOrParamName);
            
            String nlComment = buildComment(cteClass, oldComment);
            String value = cteClass.getName();
            
            hilMap.setValue(m_varOrParamName, value, nlComment, eolComment);
        }
    }
    
    
    class ScriptsConverter extends Cte2ITestConverter {
        
        String m_scriptFuncType;

        public ScriptsConverter(String scriptFuncType) {
            m_scriptFuncType = scriptFuncType;
        }

        
        @Override
        Cte2ITestConverter copy() {
            ScriptsConverter converter = new ScriptsConverter(m_scriptFuncType);
            converter.assign(this);
            return converter;
        }

        
        @Override
        public void cte2TestSpecSection(CTestSpecification testSpec,
                                        Class cteClass) {
            
            CTestFunction scriptFunc, mergedScriptFunc;
            
            if (m_scriptFuncType.startsWith(CteExporter.INIT_TARGET_COMPOS_PREFIX)) {
                scriptFunc = testSpec.getInitTargetFunction(false);
                if (testSpec.isSectionEmpty(SectionIds.E_SECTION_INIT_TARGET.swigValue())) {
                    mergedScriptFunc = testSpec.merge().getInitTargetFunction(true);
                    scriptFunc.assign(mergedScriptFunc);
                }
            } else if (m_scriptFuncType.startsWith(CteExporter.INIT_TEST_COMPOS_PREFIX)) {
                scriptFunc = testSpec.getInitFunction(false);
                if (testSpec.isSectionEmpty(SectionIds.E_SECTION_INITFUNC.swigValue())) {
                    mergedScriptFunc = testSpec.merge().getInitFunction(true);
                    scriptFunc.assign(mergedScriptFunc);
                }
            } else if (m_scriptFuncType.startsWith(CteExporter.END_TEST_COMPOS_PREFIX)) {
                scriptFunc = testSpec.getEndFunction(false);
                if (testSpec.isSectionEmpty(SectionIds.E_SECTION_ENDFUNC.swigValue())) {
                    mergedScriptFunc = testSpec.merge().getEndFunction(true);
                    scriptFunc.assign(mergedScriptFunc);
                }
            } else if (m_scriptFuncType.startsWith(CteExporter.RESTORE_TARGET_COMPOS_PREFIX)) {
                scriptFunc = testSpec.getRestoreTargetFunction(false);
                if (testSpec.isSectionEmpty(SectionIds.E_SECTION_RESTORE_TARGET.swigValue())) {
                    mergedScriptFunc = testSpec.merge().getRestoreTargetFunction(true);
                    scriptFunc.assign(mergedScriptFunc);
                }
            } else {
                throw new SIllegalArgumentException("Unknown script function: " + m_scriptFuncType);
            }
            
            setFuncParam(cteClass, scriptFunc);
        }
    }


    public CTestStub getMergedStub(CTestSpecification testSpec, String stubbedFuncName) {
        
        CTestStub stub = testSpec.getStub(stubbedFuncName);
        
        if (stub == null) { // derived ts may have no stubs - they are inherited
            CTestSpecification mergedTs = testSpec.merge();
            stub = mergedTs.getStub(stubbedFuncName);
            if (stub == null) {
                stub = new CTestStub(testSpec);
                stub.setFunctionName(stubbedFuncName);
            } else {
                stub.setParent(testSpec);
                stub.getAssignmentSteps(false).clear();
            }
            CTestBaseList stubs = testSpec.getStubs(false);
            stubs.add(-1, stub);
        }
        
        return stub;
    }
}
