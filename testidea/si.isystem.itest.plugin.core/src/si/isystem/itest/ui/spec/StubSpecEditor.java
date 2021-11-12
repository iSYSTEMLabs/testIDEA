package si.isystem.itest.ui.spec;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.WorkbenchPart;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestLog.ESectionsLog;
import si.isystem.connect.CTestMinMax;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestResult.ETestResultSection;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestStub.EStubSectionIds;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.itest.common.Messages;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.ui.comp.DynamicTable;
import si.isystem.itest.ui.comp.HitLimits;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class StubSpecEditor extends ListEditorBase {

    private TBControlText m_stubParamNamesHC;
    private TBControlText m_retValNameHC;
    private TBControlText m_scriptFuncNameHC;

    private LoggingControls m_loggingControls;
    private DynamicTable m_dynamicTable;

    private StatusDecoration m_scriptStatusDecoration;

    private WorkbenchPart m_parentView;
    private HitLimits m_hitLimitsControls;

    
    public StubSpecEditor(WorkbenchPart parentView) {
        super(ENodeId.STUBS_NODE, SectionIds.E_SECTION_STUBS);
        
        m_parentView = parentView;
        
        CTestBaseIdAdapter adapter = 
                new CTestBaseIdAdapter(EStubSectionIds.E_SECTION_STUBBED_FUNC.swigValue(),
                                       ENodeId.STUBS_NODE) {

            @Override
            public String getId(CTestBase testBase) {
                return CTestStub.cast(testBase).getFunctionName();
            }
            
            
            @Override
            public CTestBase createNew(CTestBase parentTestBase) {
                return new CTestStub(parentTestBase);
            }

            
            @Override
            public CTestBaseList getItems(boolean isConst) {
                if (m_currentTestSpec == null) {
                    return EMPTY_CTB_LIST;
                }
                return m_currentTestSpec.getStubs(isConst);
            }


            @Override
            public Boolean isError(int dataRow) {
                return isErrorInStubOrTpResult(dataRow, true);
            }
        };
        
        setIdAdapter(adapter);
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);
        mainPanel.setLayout(new MigLayout("fill"));
        
        Composite stubsPanel = 
            super.createPartControl(mainPanel,
                                    EStubSectionIds.E_SECTION_IS_ACTIVE.swigValue(),
                                    EStubSectionIds.E_SECTION_IS_CUSTOM_ACTIVATION.swigValue(),
                                    "[min!][min!][min!][min!][min!][min!][min!][min!][min!][min!][min!][grow]"); //$NON-NLS-1$
        stubsPanel.setLayoutData("growx, growy");
        
        KGUIBuilder builder = new KGUIBuilder(stubsPanel);

        builder.label(Messages.StubSpecEditor__Parameters);
        
        m_stubParamNamesHC = TBControlText.createForList(builder, 
                                                         "Names of function input parameters. Define only if\n" + //$NON-NLS-1$
                                                                 "some of them are pointers or references, which you\n" + //$NON-NLS-1$
                                                                 "need for value assignment. Not all params need to be specified.\n" + //$NON-NLS-1$
                                                                 "Example:\n" + //$NON-NLS-1$
                                                                 "    namePtr, counter, addressPtr\n" + //$NON-NLS-1$
                                                                 "    Note: 'counter' parameter will not be used, but since\n" + //$NON-NLS-1$
                                                                 "    we need to name 'addressPtr', we must specify it.", 
                                                         "wmin 100, split, span, growx", 
                                                         EStubSectionIds.E_SECTION_PARAM_NAMES.swigValue(), 
                                                         m_nodeId, 
                                                         EHControlId.EStubParams, 
                                                         SWT.BORDER);
        
        Button autoSetParametersBtn = builder.button("A", "wmin 0, gapleft 10, wrap"); //$NON-NLS-1$ //$NON-NLS-2$
        UiTools.setToolTip(autoSetParametersBtn, "Automatically sets parameter names as specified " + //$NON-NLS-1$
        		                              "in the debug info of download file.\n" + //$NON-NLS-1$
        		                              "Connection to winIDEA is required for this functionality."); //$NON-NLS-1$
        autoSetParametersBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                CTestStub stub = CTestStub.cast(getSelectedItem(true));
                if (stub != null) {
                    String functionName = stub.getFunctionName();
                    try {
                        JFunction function = m_globalFunctionsProvider.getCachedFunction(functionName);
                        if (function != null) {
                            StringBuilder sb = new StringBuilder();
                            JVariable[] vars = function.getParameters();
                            for (JVariable var : vars) {
                                if (sb.length() > 0) {
                                    sb.append(", ");                             //$NON-NLS-1$
                                }
                                sb.append(var.getName());
                            }
                            m_stubParamNamesHC.setTextInControl(sb.toString());
                            m_stubParamNamesHC.setFocus();
                        } else {
                            StatusView.getView().setDetailPaneText(StatusType.ERROR,
                                                                   "Can't find parameters for function '" +  //$NON-NLS-1$
                                                                           functionName + "'.\nPlease chek that the function is defined in winIDEA.\n"); //$NON-NLS-1$
                        }
                    } catch (SException ex) {
                        StatusView.getView().setDetailPaneText(StatusType.ERROR,
                                                               SEFormatter.getInfo(ex)); //$NON-NLS-1$
                    }
                }
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        builder.label(Messages.StubSpecEditor__Return_value);
        
        m_retValNameHC = TBControlText.createForMixed(builder, 
                                                      "Name of variable used to store stub return value. This name is needed in assignment\n" + //$NON-NLS-1$
                                                              "table below, to specify the value to be returned by stubbed\n" + //$NON-NLS-1$
                                                              "function.\n" + //$NON-NLS-1$
                                                              "\nExamples:\n" + //$NON-NLS-1$
                                                              "    retVal\n" + //$NON-NLS-1$
                                                              "    rv", 
                                                      "w 100:30%:, span, wrap", 
                                                      EStubSectionIds.E_SECTION_RET_VAL_NAME.swigValue(), 
                                                      m_nodeId, 
                                                      EHControlId.EStubRetValName, 
                                                      SWT.BORDER);

        builder.label(Messages.StubSpecEditor__Script);
        
        m_scriptFuncNameHC = TBControlText.createForMixed(builder, 
                                                          "Name of the script function to execute when the stubbed function is hit.\n"
                                                          + "Reserved script variable for test report info: self._isys_stubInfo\n"
                                                          + "Example:\n" 
                                                          + "    stubForMyFunc\n\n"
                                                          + "IMPORTANT: Script function is called AFTER the "
                                                          + "assignments from the table below are done!\n", 
                                                          "w 100:40%:, span, wrap", 
                                                          EStubSectionIds.E_SECTION_SCRIPT_FUNCTION.swigValue(), 
                                                          m_nodeId, 
                                                          EHControlId.EStubScriptFuncName, 
                                                          SWT.BORDER);
        
        m_scriptStatusDecoration = new StatusDecoration(m_scriptFuncNameHC.getControl(), 
                                                        SWT.LEFT | SWT.BOTTOM);
        m_hitLimitsControls = new HitLimits();
        m_hitLimitsControls.createHitLimitsControl(builder, ENodeId.STUBS_NODE);
        
        m_loggingControls = new LoggingControls();
        m_loggingControls.createLogControls(builder, m_nodeId);
        
        m_dynamicTable = new DynamicTable(Messages.StubSpecEditor__Actions_when_stub_is_hit,
                                          ETestResultSection.E_SECTION_STUB_RESULTS.swigValue(),
                                          CTestStub.EStubSectionIds.E_SECTION_ASSIGN_STEPS.swigValue());
        m_dynamicTable.createControls(builder, 
                                      m_parentView, 
                                      Messages.StubSpecEditor__Stub_results,
                                      ENodeId.STUBS_NODE);
        m_dynamicTable.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                            SWTBotConstants.STUBS_STEPS_KTABLE);
        
        addGlobalsProvider(m_dynamicTable);
        
        enableListItemControls(false);
        
        return getScrollableParent(mainPanel);
    }


    @Override
    protected void createItemIdControls(KGUIBuilder builder) {
        createFuncNameControls(builder);
    }

    
    @Override
    public boolean isError(CTestResult result) {
        return result.isStubError();
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
        
        CTestBaseList list = m_testSpec.getStubs(true);
        int numItems = (int)list.size();
        for (int i = 0; i < numItems; i++) {
            CTestStub stub = CTestStub.cast(list.get(i));
            if (stub.isActive()) {
                return true; // if one stub is active, mark section as active
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
        
        CTestStub stub = CTestStub.cast(testBase);
        m_stubParamNamesHC.setInput(stub, m_isInherited);
        m_retValNameHC.setInput(stub, m_isInherited);
        m_scriptFuncNameHC.setInput(stub, m_isInherited);
        
        CTestResult result = m_model.getResult(m_testSpec);

        String stubbedFuncName = stub.getFunctionName();
        setScriptStatusAndGetStepsStatus(m_scriptStatusDecoration,
                                         !stub.getScriptFunctionName().isEmpty(),
                                         result, 
                                         stubbedFuncName, 
                                         ETestResultSection.E_SECTION_STUB_RESULTS);
        
        m_loggingControls.setInput(stub.getLogConfig(false), m_isInherited);
        m_loggingControls.fillParamsAutoCompleteField(m_currentTestSpec,
                                                      stubbedFuncName,
                                                      m_currentCoreId);
        
        List<String> wizInput = getWizardInputFromStepAssignments(stub.getAssignmentSteps(true));
        m_loggingControls.setWizardInput(ESectionsLog.E_SECTION_AFTER, 
                                         wizInput);

        CTestMinMax hitLimits = stub.getHitLimits(false);
        CTestBaseList stubResults = result == null ? null : 
                                                     result.getStubResults(true);
        
        setHitLimitControlsInput(m_hitLimitsControls, stubbedFuncName, hitLimits, stubResults);
        
        m_dynamicTable.fillControls(stub, 
                                    m_testSpec,
                                    CTestStub.EStubSectionIds.E_SECTION_ASSIGN_STEPS.swigValue(),
                                    stubbedFuncName,
                                    !m_isInherited,
                                    result != null  &&  result.getStubResults(true).size() > 0,
                                    m_model);
    }
    
    
    @Override
    protected void clearListItemControls() {
        super.clearListItemControls();
        m_stubParamNamesHC.clearInput();
        m_retValNameHC.clearInput();
        m_scriptFuncNameHC.clearInput();
        m_loggingControls.clearInput();
        m_hitLimitsControls.clearInput();
        m_dynamicTable.clear(new CTestStub(), 
                             CTestStub.EStubSectionIds.E_SECTION_ASSIGN_STEPS.swigValue());
        
        m_scriptStatusDecoration.setDescriptionText("", EStatusType.INFO); //$NON-NLS-1$
    }
    
    
    @Override
    protected void enableSectionControls(boolean isEnabled) {
        super.enableSectionControls(isEnabled);
    }
    
    
    @Override
    protected void enableListItemControls(boolean isEnabled) {
        super.enableListItemControls(isEnabled);

        m_stubParamNamesHC.setEnabled(isEnabled);
        m_retValNameHC.setEnabled(isEnabled);
        m_scriptFuncNameHC.setEnabled(isEnabled);
        m_loggingControls.setEnabled(isEnabled);
        m_hitLimitsControls.setEnabled(isEnabled);
        m_dynamicTable.setEnabled(isEnabled);
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_STUBS.swigValue()};
    }
}