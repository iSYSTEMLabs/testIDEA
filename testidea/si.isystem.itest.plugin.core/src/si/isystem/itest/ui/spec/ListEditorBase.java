package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellSelectionListener;
import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestMinMax;
import si.isystem.connect.CTestPointResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.StrVector;
import si.isystem.connect.CTestResult.ETestResultSection;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.exceptions.SEFormatter;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.common.ktableutils.KTableFactory;
import si.isystem.itest.common.ktableutils.KTableModelForListItemIds;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.comp.CommentSetListener;
import si.isystem.itest.ui.comp.DynamicTable;
import si.isystem.itest.ui.comp.HitLimits;
import si.isystem.itest.ui.comp.ICommentChangedListener;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.itest.wizards.newtest.NewTCFunctionsPage;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This class provides common functionality for test case data which comes 
 * in CTestBaseList-s, for example stubs, user stubs, and test points. It implements table
 * with function names / tpIDs / ... on the left, where users can select list items.
 * Controls on the right are implemented by each derived class.
 * 
 * @author markok
 *
 */
abstract public class ListEditorBase extends SectionEditorAdapter 
                                         implements ICommentChangedListener {

    protected static final String SELECT_ITEM_ON_THE_LEFT = "Please select item from table on the left!";

    private static final int MAX_DECORATION_STR_LEN = 200;

    protected KTableModelForListItemIds m_listTableModel;
    private KTable m_listTable;
    
    private int m_listCommentSectionId;
    protected ValueAndCommentEditor m_listCommentEditor;  // comment for 'stubs:' or 'testPoints:' sections

    // item ID is function name for stubs and test point ID for test points
    protected Text m_listItemIdLabel;
    protected Text m_functionHeaderTxt;  // stubbed function header for stubs
    protected Button m_showSourceBtn;

    // section specific components
    private TBControlTristateCheckBox m_isInheritTB;
    private TBControlRadio m_isActiveHC;
    private TBControlRadio m_isCustomActivationHC;

    protected CommentSetListener m_commentChangedListener;

    protected FunctionGlobalsProvider m_globalFunctionsProvider;
    private VariablesContentProposal m_varsProvider;

    private CTestBaseIdAdapter m_idAdapter;
    
    /**
     * Contains strings, which are different for each type of list editor (stubs, user 
     * stubs, test points
     * */
    class UIStrings {
        String m_tableTitle = "Stubbed functions";
        String m_funcNameLabel = "Stubbed func.: "; 

        String m_tableTooltip = "Name of function to be stubbed. Select stub function to view\n" +
                                "settings on the right.";
        
        String m_isActiveBtn_false = "Function will NOT be stubbed during test run.";
        String m_isActiveBtn_true = "Function will be stubbed during test run.";
        String m_isActiveBtn_default = "Test specification does not contain this tag, so default value is used - stub is active.";

        String m_isCustomActivationBtn_false = "Stub will be activated by testIDEA.";
        String m_isCustomActivationBtn_true = "Stub should be activated by custom script.";
        String m_isCustomActivationBtn_default = "Stub will be activated by testIDEA.";
    }
    
    protected UIStrings m_uiStrings = new UIStrings();

    
    abstract protected void createItemIdControls(KGUIBuilder builder);

    
    public ListEditorBase(ENodeId nodeId, SectionIds sectionId) {
        super(nodeId, sectionId);
        setListCommentSectionId(m_testSpecSectionIds[0].swigValue());
    }

    
    protected void setListCommentSectionId(int listCommentSectionId) {
        m_listCommentSectionId = listCommentSectionId;
    }
    
    
    public void setIdAdapter(CTestBaseIdAdapter idAdapter) {
        m_idAdapter = idAdapter;
    }
    
    
    public Composite createPartControl(Composite parent, 
                                       int isActiveSectionId, 
                                       int isCustomActivationSectionId, 
                                       String rowLayout) {

        final Composite listEditorPanel = new Composite(parent, SWT.NONE);

        listEditorPanel.setLayout(new MigLayout("fill", 
                                           // table, vert.separator, tp controls in 3 columns
                                           "[min!][min!][min!][min!][fill]",
                                           rowLayout));
        listEditorPanel.setLayoutData("wmin 0");
        
        KGUIBuilder builder = new KGUIBuilder(listEditorPanel);
        createSectionSpecificCheckBox(builder);
        
        // It is currently not possible to get list of globals used in functions, 
        // so don't show wizard for profiler data areas.
        if (m_nodeId != ENodeId.PROFILER_DATA_AREAS_NODE) {
            createWizardBtn(builder, "gapleft push, gapright 7, wrap",
                            "Opens wizard for adding stubs, test points, and coverage and profiler areas.\n"
                                    + "Functions called from function under test are listed.",
                            m_nodeId,
                            new NewTCFunctionsPage(null));
        }
        
        Composite listPanel = createListPanel(listEditorPanel);
        listPanel.setLayoutData("spany, growy, gapright 7, wmin 300");

        builder.separator("spany, growy, gapright 10", SWT.SEPARATOR | SWT.VERTICAL);
        
        // creates controls for stubbed function and its header, or test point iD  
        createItemIdControls(builder);
        
        if (isActiveSectionId >= 0) {
            builder.label("Is active:");
            
            m_isActiveHC = new TBControlRadio(builder, 
                                              new String[]{"No", "Yes", "Default (Yes)"}, 
                                              new String[]{m_uiStrings.m_isActiveBtn_false,
                                                      m_uiStrings.m_isActiveBtn_true,
                                                      m_uiStrings.m_isActiveBtn_default},                                          
                                              "wrap", 
                                              isActiveSectionId, 
                                              m_nodeId, 
                                              null);
        }
        
        if (isCustomActivationSectionId >= 0) {
            builder.label("Is custom act.:");
        
            m_isCustomActivationHC = new TBControlRadio(builder, 
                                       new String[]{"No", "Yes", "Default (No)"}, 
                                       new String[]{m_uiStrings.m_isCustomActivationBtn_false,
                                                    m_uiStrings.m_isCustomActivationBtn_true,
                                                    m_uiStrings.m_isCustomActivationBtn_default},                                          
                                       "wrap", 
                                       isCustomActivationSectionId, 
                                       m_nodeId, 
                                       null);
        }
        return listEditorPanel;
    }
    

    protected void createSectionSpecificCheckBox(KGUIBuilder builder) {
        m_isInheritTB = createTristateInheritanceButton(builder, "gapleft 7, split 2");
        m_isInheritTB.setActionProvider(new InheritedActionProvider(m_testSpecSectionIds[0]));
    }


    protected void addGlobalsProvider(DynamicTable dynamicTable) {
        GlobalsProvider vars = GlobalsConfiguration.instance().getGlobalContainer().getAllVarsProvider();
        m_varsProvider = new VariablesContentProposal(vars.getCachedGlobals(),
                                                      vars.getCachedDescriptions());
        m_varsProvider.setFiltering(true);
        m_varsProvider.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
        
        dynamicTable.addContentProvider("expect", m_varsProvider);
        dynamicTable.addContentProvider("assign", m_varsProvider);
    }

    
    // used by StubSpecEditor and UserStubSpecEditor
    protected void createFuncNameControls(KGUIBuilder builder) {
        builder.label(m_uiStrings.m_funcNameLabel, "gapright 5"); 
        m_listItemIdLabel = builder.text("wmin 100, split, span, growx", 
                                         SWT.BORDER);
        m_listItemIdLabel.setEditable(false);
        m_listItemIdLabel.setText(SELECT_ITEM_ON_THE_LEFT); 
        
        IIConnectOperation operation = new IIConnectOperation() {
            @Override
            public void exec(JConnection jCon) {
                WinIDEAManager.showFunctionInEditor(jCon.getMccMgr().getConnectionMgr(m_currentCoreId), 
                                                    m_listItemIdLabel.getText());
            }

            @Override
            public void setData(Object data) {}
        };

        m_showSourceBtn = ISysUIUtils.createShowSourceButton(builder, 
                                                             operation, 
                                                             "wrap",
                                                             ConnectionProvider.instance());
        
        m_functionHeaderTxt = builder.text("wmin 100, skip, span, growx, wrap", SWT.BORDER);
        m_functionHeaderTxt.setEditable(false);
    }
    

    /**
     * Creates panel on the left, which contains list of all stubbed functions.
     * 
     * @param parent
     * @return
     */
    private Composite createListPanel(Composite parent) {
        
        m_listTableModel = new KTableModelForListItemIds(m_idAdapter);
        
        m_listTableModel.setColumnTitles(new String[]{m_uiStrings.m_tableTitle});
        m_listTableModel.setMainTooltip(m_uiStrings.m_tableTooltip);
        
        m_listTableModel.addModelChangedListener(new IKTableModelChangedListener() {
            
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                action.addDataChangedEvent(m_nodeId, testBase);
                
                action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
                
                if (action.isModified()) {
                    try {
                        Point[] selection = m_listTable.getCellSelection();
                        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                        if (model != null) {
                            model.execAction(action);
                        }
                        if (m_listCommentEditor != null) {
                            m_listCommentEditor.setEnabled(!testBase.isEmpty());
                        }
                        
                        // Let's select cell in the same row but in column 1,
                        // so that when user clicks up/down/+/x icons he can begin 
                        // entering function name without selecting cell to the right.
                        // Selection is also important for updating controls on the right,
                        // when user modifies item in table. 
                        if (selection.length > 0) {
                            m_listTable.setSelection(KTableModelForListItemIds.DATA_COL_IDX, 
                                                     selection[0].y, true);
                        }
                        
                        if (isRedrawNeeded) {
                            m_listTable.redraw();
                        }
                    } catch (Exception ex) {
                        // if dialog is opened here, it appears twice, because focusLost()
                        // event is also triggered on editor
                        //  SExceptionDialog.open(Activator.getShell(), 
                        //                   "Can not set test case data!", ex);
                        
                        StatusView.getView().flashDetailPaneText(StatusType.FATAL,
                                                               "Can not set test case data!\n" + 
                                SEFormatter.getInfoWithStackTrace(ex, 0));
                        System.err.println("\n\nSee status view for this Exception:\n");
                        ex.printStackTrace();
                    }
                }
            }
        });
        
        
        m_listTable = KTableFactory.createTable(parent, m_listTableModel, 
                                                "", // layout is overwritten by caller, do not set it here 
                                                true, false);
        
        m_listTable.addCellSelectionListener(new KTableCellSelectionListener() {
            
            @Override
            public void fixedCellSelected(int col, int row, int statemask) {
                clearListItemControls();
            }
            
            @Override
            public void cellSelected(int col, int row, int statemask) {
                int dataRow = row - KTableModelForListItemIds.NUM_HDR_ROWS;
                
                if (m_currentTestSpec != null  &&  
                        dataRow >= 0  &&  
                        dataRow < m_idAdapter.getItems(true).size()) {
                    
                    CTestBase tb = getItem(dataRow, false);
                    fillListItemControls(tb);
                } else {
                    clearListItemControls();
                }
            }
        });
        
        m_listCommentEditor = ValueAndCommentEditor.newKey(m_listCommentSectionId, 
                                                           m_listTable);
        m_commentChangedListener = new CommentSetListener(m_nodeId);
        m_listCommentEditor.setCommentChangedListener(m_commentChangedListener);
        
        return m_listTable;
    }

    
    protected CTestBase getSelectedItem(boolean isConst) {
        Point[] selection = m_listTable.getCellSelection();
        if (selection.length == 0) {
            return null;
        } else {
            return getItem(selection[0].y - KTableModelForListItemIds.NUM_HDR_ROWS, 
                           isConst);
        }
    }
        
    
    @Override
    public void fillControlls() {

        setCurrentTS(m_testSpecSectionIds[0]);

        if (m_currentTestSpec == null) {
            clearSectionControls();
            clearListItemControls();
            return;
        }

        CTestBaseList items = m_idAdapter.getItems(false);
        m_listTableModel.setData(m_currentTestSpec);
        
        if (items.size() == 0) {
            clearListItemControls();
            fillSectionControls();
            return;
        }

        int selectedDataRow = getSelectedDataRow();
        m_listTable.setSelection(KTableModelForListItemIds.DATA_COL_IDX, 
                                       selectedDataRow + m_listTableModel.getFixedRowCount(), 
                                       true);

        if (m_varsProvider != null) {
            m_varsProvider.setTestSpec(m_currentTestSpec);
        }
        
        // tb should never be null, as selection is limited to num items in the model
        CTestBase tb = getItem(selectedDataRow, true);
        
        fillSectionControls();
        fillListItemControls(tb);
    }
    
    
    protected void clearSectionControls() {
        setInheritCb(null);
        enableSectionControls(false);
    }
    
    
    protected void clearListItemControls() {
        
        m_listItemIdLabel.setText(SELECT_ITEM_ON_THE_LEFT);
        
        if (m_functionHeaderTxt != null) {
            m_functionHeaderTxt.setText("");
        }

        // enable it only when there is at least one item in the list
        m_listCommentEditor.updateValueAndCommentFromTestBase(null); 
        
        if (m_isActiveHC != null) {
            m_isActiveHC.clearInput();
        }
        
        if (m_isCustomActivationHC != null) {
            m_isCustomActivationHC.clearInput();
        }
        
        enableListItemControls(false);
    }
    
    
    protected void fillSectionControls() {

        setInheritCb(m_testSpec.getImports(false).getSectionSources(m_testSpecSectionIds[0], 
                                                                    false));
        enableSectionControls(!m_isInherited);

        if (m_isInheritTB != null) {
            m_isInheritTB.setEnabled(true); // should always be enabled when testSpec != null
        }
    }

    
    protected void fillListItemControls(CTestBase testBase) {
        
        if (m_functionHeaderTxt != null) {
            UiUtils.setFuncParams(m_functionHeaderTxt, m_globalFunctionsProvider, 
                                  m_idAdapter.getId(testBase));
        }

        String itemId = m_idAdapter.getId(testBase);

        enableListItemControls(!m_isInherited);

        m_listItemIdLabel.setText(itemId);
        
        if (m_isActiveHC != null) {
            m_isActiveHC.setInput(testBase, m_isInherited);
        }
        if (m_isCustomActivationHC != null) {
            m_isCustomActivationHC.setInput(testBase, m_isInherited);
        }
    }


    protected void enableSectionControls(boolean isEnabled) {

        if (m_isInheritTB != null) {
            m_isInheritTB.setEnabled(isEnabled);
        }
        
        m_listTable.setEnabled(isEnabled);
        
        if (!isEnabled) { // without this condition stup/tp/... selection is erased 
                          // whenever user clicks test case
            m_listTable.setSelection(null, false);
        }
        
        m_listTableModel.setEnabled(isEnabled);
        m_listTable.setBackground(isEnabled ?
                null
                :
                ColorProvider.instance().getColor(ColorProvider.MERGED_BKG_COLOR));
    }

    
    protected void enableListItemControls(boolean isEnabled) {
        
        if (m_showSourceBtn != null) {
            m_showSourceBtn.setEnabled(isEnabled);
        }
        if (m_isActiveHC != null) {
            m_isActiveHC.setEnabled(isEnabled);
        }
        if (m_isCustomActivationHC != null) {
            m_isCustomActivationHC.setEnabled(isEnabled);
        }
        m_listCommentEditor.setEnabled(isEnabled);
    }

    
    
    // this method may only be called, when there is at least one item in the table
    private int getSelectedDataRow() {
        
        Point[] selection = m_listTable.getCellSelection();
        int headerRowCount = m_listTableModel.getFixedRowCount();
        int dataRowCount = m_listTableModel.getRowCount() - headerRowCount 
                                   - KTableModelForListItemIds.NUM_TAIL_ROWS;

        if (dataRowCount < 1) {
            throw new IllegalStateException("There should be at least one item in the list: " + dataRowCount);
        }
        
        if (selection.length == 0) {
            // if nothing is selected and data is available, auto select the first item
            return 0;
        } else {
            int selectedDataRow = selection[0].y - headerRowCount;

            if (selectedDataRow < 0) {
                selectedDataRow = 0;
            }

            if (selectedDataRow >= dataRowCount) {
                selectedDataRow = dataRowCount - 1;
            }
            
            return selectedDataRow;
        }
    }
    
    
    
//    protected CTestBase getListSectionItem(int itemIdx, boolean isConst) {
//        
//        if (m_currentTestSpec == null  ||  itemIdx < 0) {
//            return null;
//        }
//
//        CTestBaseList items = m_idAdapter.getItems(m_currentTestSpec, isConst);
//        if (itemIdx >= items.size()) {
//            return null;
//        }
//        
//        return items.get(itemIdx);
//    }
    
    
    private CTestBase getItem(int itemIdx, boolean isConst) {
        
        CTestBaseList items = m_idAdapter.getItems(isConst);
        if (itemIdx < 0  ||  itemIdx >= items.size()) {
            throw new IllegalStateException("List item at index '" 
                                            + itemIdx + "' not found!");
        }

        return items.get(itemIdx);
    }


    private void setInheritCb(CTestBase listImports) {
        if (m_isInheritTB != null){
            if (listImports != null) {
                m_isInheritTB.setInput(listImports, false);
                m_isInheritTB.setEnabled(true);
            } else {
                m_isInheritTB.clearInput();
                m_isInheritTB.setEnabled(false);
            }
        }
        
    }
    
    
    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        m_listTable.setSelection(1, lineNo, true);
    }


    // called from adapter in StubEditor and TestPointEditor
    protected Boolean isErrorInStubOrTpResult(int dataRow, boolean isStubResult) {
        
        CTestResult result = m_model.getResult(m_testSpec);
        
        if (result == null) {
            return null;
        }
        
        Boolean retVal = null;
        
        CTestBase item = m_idAdapter.getItems(true).get(dataRow);
        String itemId = m_idAdapter.getId(item);
        
        CTestBaseList itemResults = isStubResult ? result.getStubResults(true) :
                                                   result.getTestPointResults(true);
        int numResults = (int) itemResults.size();
        for (int idx = 0; idx < numResults; idx++) {
            CTestPointResult tpResult = CTestPointResult.cast(itemResults.get(idx));
            if (tpResult.getId().equals(itemId)) {  // result found
                if (tpResult.isError()) {  
                    return Boolean.TRUE;    // error
                } else {
                    retVal = Boolean.FALSE; // no error
                }
            }
        }
        
        return retVal; // can be null, if no result for this stub/test point was found 
    }
    
    
    /**
     * This method decorates script control with OK/ERROR decoration icon, or 
     * nothing if there was no result for the script. 
     * 
     * @param decoration decoration control
     * @param result
     * @param stubFuncOrTpId stub func name or test point ID
     * @param section should be E_SECTION_STUB_RESULTS or E_SECTION_TEST_POINT_RESULTS
     */
    protected void setScriptStatusAndGetStepsStatus(StatusDecoration decoration,
                                                    boolean isScriptFuncDefined,
                                                    CTestResult result,
                                                    String stubFuncOrTpId,
                                                    CTestResult.ETestResultSection section) {
        if (result != null) {
            
            MutableObject<EStatusType> status = new MutableObject<>();
            StringBuilder sb = extractStubTPScriptResult(result, 
                                                         stubFuncOrTpId,
                                                         section,
                                                         status);
            
            if (sb != null) {  // if result was found 
                if (sb.length() == 0  &&  isScriptFuncDefined) {
                    sb.append(" "); // mark that script was executed, but there was no output
                }
            
                decoration.setDescriptionText(sb.toString(), status.getValue());
            } else {
                decoration.setDescriptionText("", EStatusType.INFO);  // no result for stub
            }
        } else {
            decoration.setDescriptionText("", EStatusType.INFO); // no result for test case
        }
    }

    
    protected EStatusType getHitsResult(String stubFuncOrTpId,
                                        CTestMinMax hitLimits, 
                                        CTestBaseList stubOrTpResults, 
                                        StringBuilder hits,
                                        StringBuilder desc) {
        
        int numResults = (int) stubOrTpResults.size();
        
        int noOfHits = 0;
        
        for (int idx = numResults - 1; idx >= 0; idx--) {
            CTestPointResult stepResult = CTestPointResult.cast(stubOrTpResults.get(idx));
            if (stepResult.getId().equals(stubFuncOrTpId)) {
                noOfHits = stepResult.getHitNo() + 1;
                break;
            }
        }
        
        hits.append(noOfHits);
        
        if (noOfHits < hitLimits.getMin()) {
            desc.append("Not enough hits!");
            return EStatusType.ERROR;
        }
        
        if (noOfHits > hitLimits.getMax()) {
            desc.append("Too many hits!");
            return EStatusType.ERROR;
        }
        
        desc.append("OK"); // must not be empty to see green icon
        return EStatusType.INFO;
    }

    
    protected void setHitLimitControlsInput(HitLimits hitLimitsControls, 
                                       String stubbOrTpId, 
                                       CTestMinMax hitLimits, 
                                       CTestBaseList stubOrTpResults) {
          
          StringBuilder hits = new StringBuilder();
          EStatusType errCode = EStatusType.INFO;
          StringBuilder desc = new StringBuilder();
          if (stubOrTpResults != null) {
              errCode = getHitsResult(stubbOrTpId,
                                      hitLimits, 
                                      stubOrTpResults,
                                      hits,
                                      desc);
          }
          
          hitLimitsControls.setInput(hitLimits, m_isInherited, 
                                     hits.toString(), desc.toString(), errCode);
    }

    
    public static StringBuilder extractStubTPScriptResult(CTestResult result,
                                                          String stubFuncOrTpId,
                                                          ETestResultSection section,
                                                          MutableObject<EStatusType> status) {
        
        CTestBaseList results = result.getTestBaseList(section.swigValue(), true);
        int numResults = (int) results.size();
        StringBuilder sb = null;
        status.setValue(EStatusType.INFO);
        for (int idx = 0; idx < numResults; idx++) {
            CTestPointResult stepResult = CTestPointResult.cast(results.get(idx));
            if (stepResult.getId().equals(stubFuncOrTpId)) {

                if (sb == null) {
                    sb = new StringBuilder(); // lazy init indicates that result was found
                }
                int stepIdx = stepResult.getStepIdx();
                int hitNo = stepResult.getHitNo();
                
                String scriptInfo = stepResult.getScriptInfoVar();
                String scriptError = stepResult.getScriptRetVal();

                if (sb.length() < MAX_DECORATION_STR_LEN) {
                    if (sb.length() > 0) {
                        sb.append("\n--------------------------\n\n");
                    }

                    if (!scriptInfo.isEmpty()  ||  !scriptError.isEmpty()) {
                        sb.append("Step: ").append(stepIdx)
                        .append("  Hit: ").append(hitNo);
                        if (!scriptInfo.isEmpty()) {
                            sb.append("\n  Script info: ").append(scriptInfo);
                        }
                        if (!scriptError.isEmpty()) {
                            sb.append("\n  Script error: ").append(scriptError);
                        }
                    }
                    
                    if (sb.length() >= MAX_DECORATION_STR_LEN) {
                        sb.append("\n\n...\n"); // indicate there is more data
                    }
                }
                
                if (!scriptError.isEmpty()) {
                    status.setValue(EStatusType.ERROR);
                }
            }
        }
        
        return sb;
    }


    @Override
    public void commentChanged(YamlScalar scalar) {
        if (m_currentTestSpec != null) {
            // can be null when control is cleared - see clearInput()
            CTestBase testBase = getSelectedItem(false);
            
            SetSectionAction action = new SetSectionAction(testBase,
                                                           m_nodeId,
                                                           scalar.copy());
            action.addDataChangedEvent();
            action.addAllFireEventTypes();
            
            if (action.isModified()) {
                try {
                    TestSpecificationModel.getActiveModel().execAction(action);
                    // no model verification is necessary for comments
                } catch (Exception ex) {
                    StatusView.getView().setDetailPaneText(StatusType.ERROR,
                                                           "Can not set test case data!\n" + 
                                                           SEFormatter.getInfoWithStackTrace(ex, 0));
                }
            }
        }
    }
    
    
    protected ArrayList<String> getWizardInputFromStepAssignments(CTestBaseList assignmentSteps) {
        
        LinkedHashSet<String> varsSet = new LinkedHashSet<>();
        for (int idx = 0; idx < assignmentSteps.size(); idx++) {
            CTestEvalAssignStep step = CTestEvalAssignStep.cast(assignmentSteps.get(idx));
            CMapAdapter assignments = step.getAssignments(true);
            StrVector vars = new StrVector();
            assignments.getKeys(vars);
            for (int j = 0; j < vars.size(); j++) {
                varsSet.add(vars.get(j));
            }
        }
        
        return new ArrayList<>(varsSet);
    }

    
    public void refreshGlobals() {
        
        if (m_listTableModel == null) {
            return; // does not exist yet because of lazy init.
        }
                
        if (m_varsProvider != null) {
            GlobalsProvider vars = GlobalsConfiguration.instance().getGlobalContainer().getAllVarsProvider();

            m_varsProvider.setProposals(vars.getCachedGlobals(), 
                                        vars.getCachedDescriptions());
        }
        
        m_globalFunctionsProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getFuncGlobalsProvider(m_currentCoreId);
      
        m_listTableModel.setAutoCompleteProposals(KTableModelForListItemIds.DATA_COL_IDX, 
                                                  new AsystContentProposalProvider(m_globalFunctionsProvider.getCachedGlobals(), 
                                                                                   m_globalFunctionsProvider.getCachedDescriptions()), 
                                                  ContentProposalAdapter.PROPOSAL_REPLACE);
    }
}
