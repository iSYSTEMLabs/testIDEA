package si.isystem.itest.ui.spec;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.WorkbenchPart;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CLineDescription.EResourceType;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestMinMax;
import si.isystem.connect.CTestLog.ESectionsLog;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestPoint.ETestPointSections;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResult.ETestResultSection;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.ui.comp.DynamicTable;
import si.isystem.itest.ui.comp.HitLimits;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.ui.utils.KGUIBuilder;


class TestPointEditor extends ListEditorBase {

    private WorkbenchPart m_parentView;
    
    private TBControlText m_scriptFuncNameHC;
    private StatusDecoration m_scriptStatusDecoration;
    private HitLimits m_hitLimitsControls;
    private BPLogLocationControls m_commonControls;

    private DynamicTable m_dynamicTable;

    TestPointEditor(WorkbenchPart parentView) {
        super(ENodeId.TEST_POINT_NODE, SectionIds.E_SECTION_TEST_POINTS);

        m_parentView = parentView;
        
        CTestBaseIdAdapter adapter = 
                new CTestBaseIdAdapter(ETestPointSections.E_SECTION_TEST_POINT_ID.swigValue(),
                                       ENodeId.TEST_POINT_NODE) {
            
            @Override
            public String getId(CTestBase testBase) {
                return CTestPoint.cast(testBase).getId();
            }
            
            
            @Override
            public CTestBase createNew(CTestBase parentTestBase) {
                return new CTestPoint(parentTestBase);
            }

            
            @Override
            public CTestBaseList getItems(boolean isConst) {
                if (m_currentTestSpec == null) {
                    return EMPTY_CTB_LIST;
                }
                return m_currentTestSpec.getTestPoints(isConst);
            }


            @Override
            public Boolean isError(int dataRow) {
                return isErrorInStubOrTpResult(dataRow, false);
            }
            
        };
        
        setIdAdapter(adapter);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        m_uiStrings.m_isActiveBtn_false = "Test point will NOT be active during test run.";
        m_uiStrings.m_isActiveBtn_true = "Test point will be active during test run.";
        m_uiStrings.m_isActiveBtn_default = "Test specification does not contain this tag, so default value is used - test point is active.";
        
        m_uiStrings.m_isCustomActivationBtn_false = "Test point will be activated by testIDEA.";
        m_uiStrings.m_isCustomActivationBtn_true = "Test point should be activated by custom script.";
        m_uiStrings.m_isCustomActivationBtn_default = "Test point will be activated by testIDEA.";
        
        m_uiStrings.m_tableTitle = "Test points";
        m_uiStrings.m_tableTooltip = "IDs of test points. Select test point ID to view settings\n" +
                                "on the right.";

        m_commonControls = new BPLogLocationControls();
        
        Composite mainPanel = createScrollable(parent);
        mainPanel.setLayout(new MigLayout("fill"));
        
        Composite testPointPanel = 
                super.createPartControl(mainPanel,
                                        CTestPoint.ETestPointSections.E_SECTION_IS_ACTIVE.swigValue(),
                                        CTestPoint.ETestPointSections.E_SECTION_IS_CUSTOM_ACTIVATION.swigValue(),
                                        "[min!][min!][min!][min!][min!][min!][min!][min!][min!][min!][grow]");
        testPointPanel.setLayoutData("growx, growy");

        KGUIBuilder builder = new KGUIBuilder(testPointPanel);

        m_commonControls.createBPConditionControls(builder, ENodeId.TEST_POINT_NODE);

        builder.label("Script func.:");
        
        m_scriptFuncNameHC = TBControlText.createForMixed(builder, 
                                                          "Name of the script function to execute when test point is hit.\n"
                                                          + "Reserved script variable for test report info: self._isys_testPointInfo\n"
                                                          + "Example:\n" 
                                                          + "    testPointFunc\n\n" 
                                                          + "IMPORTANT: Script function is called AFTER the " 
                                                          + "assignments from the table below are done and expected expressions are evaluated!\n", 
                                                          "wmin 100, span, growx, wrap", 
                                                          ETestPointSections.E_SECTION_SCRIPT_FUNC.swigValue(), 
                                                          m_nodeId, 
                                                          EHControlId.EStubScriptFuncName, 
                                                          SWT.BORDER);

        m_scriptStatusDecoration = new StatusDecoration(m_scriptFuncNameHC.getControl(), 
                                                        SWT.LEFT | SWT.BOTTOM);
        
        m_hitLimitsControls = new HitLimits();
        m_hitLimitsControls.createHitLimitsControl(builder, ENodeId.TEST_POINT_NODE);
        
        m_commonControls.createLocationControls(builder, ENodeId.TEST_POINT_NODE);
        m_commonControls.createLogControls(builder, m_nodeId);
        
        m_dynamicTable = new DynamicTable("Actions when test point is hit:",
                                          ETestResultSection.E_SECTION_TEST_POINT_RESULTS.swigValue(),
                                          CTestPoint.ETestPointSections.E_SECTION_STEPS.swigValue());
        
        m_dynamicTable.createControls(builder, 
                                      m_parentView, 
                                      "Test point results",
                                      ENodeId.TEST_POINT_NODE);

        m_dynamicTable.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                            SWTBotConstants.TEST_POINTS_STEPS_KTABLE);

        addGlobalsProvider(m_dynamicTable);
        
        enableListItemControls(false);

        return getScrollableParent(mainPanel);
    }
    

    @Override
    protected void createItemIdControls(KGUIBuilder builder) {
        builder.label("Test point ID:", "gapright 5"); 
        m_listItemIdLabel = builder.text("w 100::, span, growx, wrap", 
                                         SWT.BORDER);
        m_listItemIdLabel.setText(SELECT_ITEM_ON_THE_LEFT); 
        m_listItemIdLabel.setEditable(false);
    }

    
    @Override
    public boolean isError(CTestResult result) {
        return result.isTestPointError();
    }

    
    @Override
    public boolean hasErrorStatus() {
        return isActive();
    }

    
    @Override
    public boolean isActive() {
        
        if (m_testSpec == null) {
            return false;
        }
        
        CTestBaseList list = m_testSpec.getTestPoints(true);
        int numItems = (int)list.size();
        for (int i = 0; i < numItems; i++) {
            CTestPoint tp = CTestPoint.cast(list.get(i));
            if (tp.isActive() != ETristate.E_FALSE) {
                return true; // if one test point is active, mark section as active
            }
        }
        
        return false;
    }

    
    @Override
    protected void fillSectionControls() {
        super.fillSectionControls();
        m_listCommentEditor.updateValueAndCommentFromTestBase(m_currentTestSpec);
    }
    

    @Override
    protected void fillListItemControls(CTestBase testBase) {
        super.fillListItemControls(testBase);
        
        m_commentChangedListener.setTestBase(m_currentTestSpec);
        
        final CTestPoint testPoint = CTestPoint.cast(testBase);        
        
        m_scriptFuncNameHC.setInput(testPoint, m_isInherited);

        CTestLocation location = testPoint.getLocation(false);
        m_commonControls.setCoreId(m_currentCoreId);
        m_commonControls.setBPInput(testPoint, m_isInherited);
        m_commonControls.setLoggingInput(testPoint.getLogConfig(false), m_isInherited);
        String testPointId = testPoint.getId();
        LocationIDProvider locProvider = new LocationIDProvider() {
            @Override
            public String getId() {
                return testPointId;
            }
        };
        m_commonControls.setLocationInput(location, locProvider);

        CTestResult result = m_model.getResult(m_testSpec);

        setScriptStatusAndGetStepsStatus(m_scriptStatusDecoration,
                                             !testPoint.getScriptFunctionName().isEmpty(),
                                             result,
                                             testPointId, 
                                             ETestResultSection.E_SECTION_TEST_POINT_RESULTS);

        // function under test is the best guess we can make for local vars
        // code completion, if resource is address or file.
        String funcName = m_currentTestSpec.getFunctionUnderTest(true).getName();
        if (location.getResourceType() == EResourceType.E_RESOURCE_FUNCTION) {
            funcName = location.getResourceName();
        }
        m_commonControls.fillAutoCompleteFields(m_currentTestSpec,
                                                funcName,
                                                m_currentCoreId);
        
        List<String> wizInput = getWizardInputFromStepAssignments(testPoint.getSteps(true));
        m_commonControls.setWizardInput(ESectionsLog.E_SECTION_AFTER, wizInput);

        CTestMinMax hitLimits = testPoint.getHitLimits(false);
        CTestBaseList tpResults = result == null ? null : 
                                                   result.getTestPointResults(true);
        
        setHitLimitControlsInput(m_hitLimitsControls, testPointId, hitLimits, tpResults);
        
        m_dynamicTable.fillControls(testPoint, 
                                    m_testSpec,
                                    CTestPoint.ETestPointSections.E_SECTION_STEPS.swigValue(),
                                    testPointId,
                                    !m_isInherited,
                                    result != null  &&  result.getTestPointResults(true).size() > 0,
                                    m_model);
    }
    
    
    @Override
    protected void clearListItemControls() {
        super.clearListItemControls();
        
        m_scriptFuncNameHC.clearInput();
        m_commonControls.clearInput();
        m_scriptStatusDecoration.setDescriptionText("", EStatusType.INFO);
        m_dynamicTable.clear(new CTestPoint(), 
                             CTestPoint.ETestPointSections.E_SECTION_STEPS.swigValue());
        m_hitLimitsControls.clearInput();
    }
    
    
    @Override
    protected void enableListItemControls(boolean isEnabled) {
        super.enableListItemControls(isEnabled);

        m_commonControls.setEnabled(isEnabled);
        m_scriptFuncNameHC.setEnabled(isEnabled);
        m_dynamicTable.setEnabled(isEnabled);
        m_hitLimitsControls.setEnabled(isEnabled);
    }
    
    
    @Override
    public void refreshGlobals() {
        // empty, there are no meaningful proposals for test point IDs.
    }    

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_TEST_POINTS.swigValue()};
    }
}

