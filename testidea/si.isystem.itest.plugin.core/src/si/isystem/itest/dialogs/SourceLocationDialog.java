package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.commons.ISysCommonConstants;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CAddressController;
import si.isystem.connect.CLineDescription;
import si.isystem.connect.CLineDescription.EMatchingType;
import si.isystem.connect.CLineDescription.EResourceType;
import si.isystem.connect.CLineLocation;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestLocation.ETestLocationSections;
import si.isystem.connect.ETristate;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.EBool;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlCombo;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class SourceLocationDialog extends Dialog {

    private CTestLocation m_location;
    private String m_testPointOrTestCaseId;
    private boolean m_isTestPoint;

    private TBControlRadio m_resTypeBtnsHC;
    private TBControlCombo m_resourceNameHC;
    private Text m_funcParamsText;
    private TBControlText m_lineNumberHC;
    private TBControlRadio m_isSearchHC;
    private TBControlText m_linesRangeHC;
    private TBControlRadio m_searchContextBtnsHC;
    private TBControlRadio m_matchTypeBtnsHC;
    private TBControlText m_patternHC;
    private TBControlText m_lineOffsetHC;
    private TBControlText m_numStepsHC;

    private Label m_resNameLbl;

    private GlobalsSelectionControl m_resourceNameGSC;
    private String m_coreId;
    private KGUIBuilder m_searchGroup;
    private Label m_lineLbl;
    private Label m_searchLineLbl;
    private Label m_searchPatternLbl;
    private Label m_linesRangeLbl;
    private Label m_searchContextLbl;
    private Label m_matchTypeLbl;
    private Label m_lineOffsetLbl;
    private Label m_numStepsLbl;
    private Button m_showSrcBtn;
    private TBControlRadio m_srcFileLocation;

    private static final String FILE_NAME_LBL = "File name:";
    private static final String FUNCTION_NAME_LBL = "Function name:";
    private static final String ADDRESS_LBL = "Address:";
    
    private static final String SEARCH_CONFIGURATION_TXT = "Search configuration";
    
    private static final String FUNCTION_BTN_TXT = "Function";
    private static final String FILE_BTN_TXT = "File";
    private static final String ADDRESS_BTN_TXT = "Address";

    private static int FUNCTION_ENUM_VALUE = CLineDescription.EResourceType.E_RESOURCE_FUNCTION.swigValue();
    private static int FILE_ENUM_VALUE = CLineDescription.EResourceType.E_RESOURCE_FILE.swigValue();
    private static int ADDRESS_ENUM_VALUE = CLineDescription.EResourceType.E_RESOURCE_ADDRESS.swigValue();

    private static int SRC_FILE_LOC_LOCAL_HOST_ENUM_VALUE = CLineDescription.EFileLocation.ELocalHost.swigValue(); 
    private static int SRC_FILE_LOC_winIDEA_HOST_ENUM_VALUE = CLineDescription.EFileLocation.EWinIDEAHost.swigValue(); 
    
    private static int SEARCH_ANY_ENUM_VALUE = CLineDescription.ESearchContext.E_SEARCH_ANY.swigValue();
    private static int SEARCH_CODE_ENUM_VALUE = CLineDescription.ESearchContext.E_SEARCH_CODE.swigValue();
    private static int SEARCH_CPMMENT_ENUM_VALUE = CLineDescription.ESearchContext.E_SEARCH_COMMENT.swigValue();
    
    private static int MATCH_PLAIN_ENUM_VALUE = CLineDescription.EMatchingType.E_MATCH_PLAIN.swigValue();
    private static int MATCH_REG_EX_ENUM_VALUE = CLineDescription.EMatchingType.E_MATCH_REG_EX.swigValue();
    private static int MATCH_TP_ID_ENUM_VALUE = CLineDescription.EMatchingType.E_MATCH_TEST_POINT_ID.swigValue();
    
    private static final String FUNCTION_BTN_DATA = 
            new CTestLocation().enum2Str(CTestLocation.ETestLocationSections.E_SECTION_RESOURCE_TYPE.swigValue(),
                                     FUNCTION_ENUM_VALUE);
    private static final String FILE_BTN_DATA = 
            new CTestLocation().enum2Str(CTestLocation.ETestLocationSections.E_SECTION_RESOURCE_TYPE.swigValue(),
                                     FILE_ENUM_VALUE);
    private static final String ADDRESS_BTN_DATA = 
            new CTestLocation().enum2Str(CTestLocation.ETestLocationSections.E_SECTION_RESOURCE_TYPE.swigValue(),
                                     ADDRESS_ENUM_VALUE);

    
    public SourceLocationDialog(Shell parentShell, CTestLocation location, 
                                String testPointOrTestCaseId, boolean isTestPoint,
                                String coreId) {
        super(parentShell);
        m_location = location;
        m_testPointOrTestCaseId = testPointOrTestCaseId;
        m_isTestPoint = isTestPoint;
        m_coreId = coreId;
        
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);

        composite.getShell().setText("Source code location");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 600;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fillx", "[min!][grow]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        createLocationControls(builder);
        fillControls();
        
        builder.separator("span, growx, gaptop 15, wrap", SWT.HORIZONTAL);
        
        return composite;
    }
    
    
    private void createLocationControls(KGUIBuilder builder) {
        
        // resource type
        builder.label("Resource type:");
        
        int resTypeSection = ETestLocationSections.E_SECTION_RESOURCE_TYPE.swigValue();
        m_resTypeBtnsHC = new TBControlRadio(builder, 
                 new String[]{FUNCTION_BTN_TXT, 
                              FILE_BTN_TXT,
                              ADDRESS_BTN_TXT,
                              "Default (Function)"}, 
                 new String[]{
                     "Function is used for test point location - line numbers are function relative.",
                     "File is used for test point location - line numbers are file relative.",
                     "Address in decimal or hex format is used for test point location. Use this setting only when code under test\n"
                     + "does not change, and there is no other possibility to define test point location.",
                     "Default setting - resource type is not set in test specification file."}, 
                 new String[]{FUNCTION_BTN_DATA, 
                              FILE_BTN_DATA, 
                              ADDRESS_BTN_DATA, 
                              ""}, 
                 "wrap", 
                 resTypeSection, 
                 ENodeId.TEST_POINT_NODE, 
                 null);
        
        m_resTypeBtnsHC.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String value = (String)((Button)e.getSource()).getData(TBControlRadio.ENUM_TXT);
                setEnabled(value);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        
        // resource name
        m_resNameLbl = builder.label(FUNCTION_NAME_LBL);
        
        EResourceType resType = m_location.getResourceType();
        String providerId = resType == EResourceType.E_RESOURCE_FILE ? 
                GlobalsContainer.GC_MODULES : GlobalsContainer.GC_FUNCTIONS;
        
        m_resourceNameGSC = new GlobalsSelectionControl(builder.getParent(), 
                                                        "w 100::, split, span, growx, wrap",
                                                        null,
                                                        null,
                                                        SWT.NONE,
                                                        providerId,
                                                        m_coreId,
                                                        true,
                                                        true,
                                                        ContentProposalAdapter.PROPOSAL_REPLACE,
                                                        UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                        GlobalsConfiguration.instance().getGlobalContainer(),
                                                        ConnectionProvider.instance());


        m_resourceNameGSC.getControl().addVerifyListener(new VerifyListener() {
            
            @Override
            public void verifyText(VerifyEvent e) {
                e.doit = true;
                String functionName = e.text;
                try {
                    String resType = m_resTypeBtnsHC.getSelection();
                    boolean isResourceFunction = resType.equals(FUNCTION_BTN_DATA)  ||
                                                 resType.isEmpty();  // default selection is function
                    
                    if (isResourceFunction) {
                        FunctionGlobalsProvider globalFunctionsProvider = GlobalsConfiguration.instance().getGlobalContainer().getFuncGlobalsProvider(m_coreId);
                        UiUtils.setFuncParams(m_funcParamsText, 
                                              globalFunctionsProvider, 
                                              functionName);
                    } else {
                        // file or address
                        m_funcParamsText.setText("");
                    }
                } catch (SIllegalStateException ex) {
                    // ignore, since this happens only during ui testing, because
                    // events triggered by SWTBot are slightly different than events
                    // sent in normal usage.
                    m_funcParamsText.setText("");
                }
            }
        });
        
        m_resourceNameHC = new TBControlCombo(m_resourceNameGSC.getControl(), 
                                              "Name of a target function or source file or address (depends on resource type above),\n" +
                                                      "where test point will be set.", 
                                              ETestLocationSections.E_SECTION_RESOURCE_NAME.swigValue(), 
                                              ENodeId.TEST_POINT_NODE, 
                                              EHControlId.ELocationResName);
        
        m_funcParamsText = builder.text("skip, split, span, growx, gapleft 6, gapbottom 5, wrap", 
                                            SWT.BORDER);
        m_funcParamsText.setEditable(false);

        // source location
        builder.label("Source file loc.:");
        int srcFileLocSection = ETestLocationSections.E_SECTION_SRC_FILE_LOCATION.swigValue();
        m_srcFileLocation =  new TBControlRadio(builder, 
                                                new String[]{"Local", "Remote", "Default (Local)"}, 
                                                new String[]{"Source file is located on host running "
                                                             + "test script.",
                                                             "Source file is located on host running "
                                                             + "winIDEA. Select this item only if "
                                                             + "winIDEA is running on remote host "
                                                             + "and sources are located there.",
                                                             "Default location (Local host) is used."},

                                                new String[]{new CTestLocation().enum2Str(srcFileLocSection, 
                                                                                          SRC_FILE_LOC_LOCAL_HOST_ENUM_VALUE),
                                                             new CTestLocation().enum2Str(srcFileLocSection, 
                                                                                          SRC_FILE_LOC_winIDEA_HOST_ENUM_VALUE),
                                                             ""}, 
                                                             "wrap", 
                                                             srcFileLocSection, 
                                                ENodeId.TEST_POINT_NODE, 
                                                null);
        
        // line number
        m_lineLbl = builder.label("Line:");
        
        m_lineNumberHC = TBControlText.createForMixed(builder, 
                                                      "Defines line number where test point is set, or search " +
                                                              "starts, if search parameters are defined below.", 
                                                      "w 55:55:55, split, gapleft 6, wrap", 
                                                      ETestLocationSections.E_SECTION_LINE.swigValue(), 
                                                      ENodeId.TEST_POINT_NODE, 
                                                      EHControlId.ELocationLineNumber, 
                                                      SWT.BORDER);
        
        m_searchLineLbl = builder.label("Search line:", "gaptop 25");
        
        m_isSearchHC = new TBControlRadio(builder, 
                                          new String[]{"No", "Yes", "Default (No)"}, 
                                          new String[]{"The line is defined by the 'Line' entry above.",
                                                       "The line is determined by searching source code as defined below.",
                                                       "The same as 'No', but there is no tag in source file (default is used)."},       
                                          "gaptop 25, wrap", 
                                          ETestLocationSections.E_SECTION_IS_SEARCH.swigValue(), 
                                          ENodeId.TEST_POINT_NODE, 
                                          null);
        
        m_isSearchHC.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                String value = (String)((Button)e.getSource()).getData(TBControlRadio.ENUM_TXT);
                enableSearchControls(value.equals(EBool.tristate2Str(ETristate.E_TRUE)));
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        // focus listener is preferred to selection listener, because SWTBot selection
        // triggers event between setting selection, when the old button has lost selection,
        // but the new has not obtained it yet.
        // m_isActiveHC.addSelectionListener(m_selectionListener);
        // m_isSeActiveHC.addFocusListener(m_rbFocusListener);
        
        m_searchGroup = builder.group(SEARCH_CONFIGURATION_TXT, 
                                      "span, growx, gapbottom 25, wrap",
                                      true,
                                      "fillx", "[min!][grow, fill]", "");
        
        // lines range
        m_linesRangeLbl = m_searchGroup.label("Lines range:");
        
        m_linesRangeHC = TBControlText.createForMixed(m_searchGroup, 
                                                      "Defines range of lines to be used when " +
                                                              "searching for the location line.\nIf empty or " +
                                                              "set to 0, all lines till the end of file are searched for pattern.", 
                                                      "w 55:55:55, wrap", 
                                                      ETestLocationSections.E_SECTION_LINES_RANGE.swigValue(), 
                                                      ENodeId.TEST_POINT_NODE, 
                                                      EHControlId.ELocationRange, 
                                                      SWT.BORDER);
        
        // search context
        m_searchContextLbl = m_searchGroup.label("Search context:");
        
        int searchContextSection = ETestLocationSections.E_SECTION_SEARCH_CONTEXT.swigValue();
        m_searchContextBtnsHC = new TBControlRadio(m_searchGroup, 
                                                   new String[]{"Any", "Code", "Comment (// only)",
                                                                "Default (Comment)"}, 
                                                   new String[]{"Complete line is searched for pattern.",
                                                                "Only code is searched for pattern. Single line comment (//) is removed.",
                                                   "Only comment is searched (single line comment is recognized only (//)).",
                                                   "Default mode (Comment) is used. This setting is not saved, which means that default will be used for execution."},
                                                   
                                                   new String[]{new CTestLocation().enum2Str(searchContextSection, 
                                                                                             SEARCH_ANY_ENUM_VALUE),
                                                                new CTestLocation().enum2Str(searchContextSection, 
                                                                                             SEARCH_CODE_ENUM_VALUE),
                                                                new CTestLocation().enum2Str(searchContextSection, 
                                                                                             SEARCH_CPMMENT_ENUM_VALUE),
                                                                ""}, 
                                                   "wrap", 
                                                   searchContextSection, 
                                                   ENodeId.TEST_POINT_NODE, 
                                                   null);
        
        // match type
        m_matchTypeLbl = m_searchGroup.label("Match type:");
        
        int matchingTypeSection = ETestLocationSections.E_SECTION_MATCH_TYPE.swigValue();
        m_matchTypeBtnsHC = 
             new TBControlRadio(m_searchGroup, 
                                new String[]{"Plain text", "Reg. exp.",
                                             m_isTestPoint ? "Test p. ID" : "Test case ID",
                                             "Default (Test p. ID)"}, 
                                new String[]{"Pattern is used for search literally.", 
                                             "Pattern is interpreted as regular expression.\n"
                                             + "It must match all text in line (code or comment or both, depending on 'Search context' above).\n"
                                             + "For example, to match line\n"
                                             + "    return delay;\n"
                                             + "the pattern should be '.*return.*', not only return.",
                                             "Search pattern is composed of string 'TID: ' " +
                                             "and Test Point ID.\nPattern setting is ignored.",
                                             "Default mode (Test point ID) is used. This setting is not saved, which means that default will be used for execution."},
                                new String[]{new CTestLocation().enum2Str(matchingTypeSection, 
                                                                          MATCH_PLAIN_ENUM_VALUE),
                                             new CTestLocation().enum2Str(matchingTypeSection, 
                                                                          MATCH_REG_EX_ENUM_VALUE),
                                             new CTestLocation().enum2Str(matchingTypeSection, 
                                                                          MATCH_TP_ID_ENUM_VALUE),
                                             ""},               
                                "wrap", 
                                matchingTypeSection, 
                                ENodeId.TEST_POINT_NODE, 
                                null);
        
        addSelectionListeners(m_matchTypeBtnsHC);
        
        // pattern
        m_searchPatternLbl = m_searchGroup.label("Search pattern:");
        
        m_patternHC = TBControlText.createForMixed(m_searchGroup, 
                                                   "Defines pattern for search algorithm. Depending on matching type\n" +
                                                           "it can be either regular expression or plain text.\n\n" +
                                                           ISysCommonConstants.REG_EX_TOOLTIP_POSTFIX, 
                                                   "w 100::, split, span, growx, wrap", 
                                                   ETestLocationSections.E_SECTION_PATTERN.swigValue(), 
                                                   ENodeId.TEST_POINT_NODE, 
                                                   EHControlId.ELocationPattern, 
                                                   SWT.BORDER);

        // line offset - text
        m_lineOffsetLbl = builder.label("Line offset:");
        
        m_lineOffsetHC = TBControlText.createForMixed(builder, 
                                                      "This value is added to the line number defined above or found by search.\n" +
                                                              "It can be used, when there is no specific string in the line where we want\n" +
                                                              "to define a location. Negative numbers are allowed.", 
                                                      "width 55:55:55, split", 
                                                      ETestLocationSections.E_SECTION_LINE_OFFSET.swigValue(), 
                                                      ENodeId.TEST_POINT_NODE, 
                                                      EHControlId.ELocationLineOffset, 
                                                      SWT.BORDER);

        IIConnectOperation showSourceOperation = new IIConnectOperation() {
            
            @Override
            public void exec(JConnection jCon) {
                String resourceName = m_location.getResourceName();
                if (resourceName.isEmpty()) {
                    return;
                }

                CAddressController addrCtrl = new CAddressController(jCon.getMccMgr().getConnectionMgr(m_coreId));
                CLineLocation lineLoc = addrCtrl.getSourceLocation(m_location.getLineDescription(),
                                                                   m_testPointOrTestCaseId);
                
                WinIDEAManager.showSourceInEditor(jCon.getMccMgr().getConnectionMgr(m_coreId), 
                                                  lineLoc.getFileName(), 
                                                  (int)lineLoc.getLineNumber());
            }
            
            
            @Override
            public void setData(Object data) {}
        };
        
        m_showSrcBtn = ISysUIUtils.createShowSourceButton(builder,  
                                                          showSourceOperation, 
                                                          "skip, align right, wrap",
                                                          ConnectionProvider.instance());
        
        UiTools.setToolTip(m_showSrcBtn, "Shows the source line, which matches the location settings.");
        m_showSrcBtn.setEnabled(m_location != null);
        
        // numSteps - text
        m_numStepsLbl = builder.label("Num. steps:");
        
        m_numStepsHC = TBControlText.createForMixed(builder, 
                                                    "Number of execution steps (step over in source code) to execute after test point is hit,\n" +
                                                            "before logging and assignments are performed.", 
                                                    "width 55:55:55, wrap", 
                                                    ETestLocationSections.E_SECTION_NUM_STEPS.swigValue(), 
                                                    ENodeId.TEST_POINT_NODE, 
                                                    EHControlId.ELocationNumSteps, 
                                                    SWT.BORDER);
    }

    
    private void setPatternText(EMatchingType matchingType, boolean isSearch, boolean isSendAction) {
        
        Text patternTxt = (Text) m_patternHC.getControl();
        m_patternHC.setEnabled(isSearch);
        
        switch (matchingType) {
        case E_MATCH_PLAIN:
        case E_MATCH_REG_EX:
            m_patternHC.setEditable(isSearch);
            if (isSendAction) {
                m_patternHC.sendSetSectionAction();
            }
            break;
        case E_MATCH_TEST_POINT_ID:
        default:
            if (isSendAction) {
                patternTxt.setText("");
                m_patternHC.sendSetSectionAction(); // clear the pattern
            }
            
            patternTxt.setText(CAddressController.getTestPointIdPrefix() + 
                               m_testPointOrTestCaseId);
            m_patternHC.setEditable(false);
        }
    }

    
    private void addSelectionListeners(TBControlRadio matchTypeTBR) {
        SelectionListener plainAndRegExListener = new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                setPatternText(EMatchingType.E_MATCH_PLAIN, true, true);
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        };
        
        matchTypeTBR.addSelectionListener(plainAndRegExListener, 0);
        matchTypeTBR.addSelectionListener(plainAndRegExListener, 1);
        
        SelectionListener testIdListener = new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                setPatternText(EMatchingType.E_MATCH_TEST_POINT_ID, true, true);
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        };
        
        matchTypeTBR.addSelectionListener(testIdListener, 2);
        matchTypeTBR.addSelectionListener(testIdListener, 3);
    }


    private void fillControls() {
        
        m_resTypeBtnsHC.setInput(m_location, false);
        m_resourceNameHC.setInput(m_location, false);
        m_srcFileLocation.setInput(m_location, false);
        m_lineNumberHC.setInput(m_location, false);
        m_isSearchHC.setInput(m_location, false);
        m_linesRangeHC.setInput(m_location, false);
        m_searchContextBtnsHC.setInput(m_location, false);
        m_matchTypeBtnsHC.setInput(m_location, false);
        m_patternHC.setInput(m_location, false);
        m_lineOffsetHC.setInput(m_location, false);
        m_numStepsHC.setInput(m_location, false);

        boolean isSearch = m_location.isSearch() == ETristate.E_TRUE;
        setPatternText(m_location.getMatchingType(), isSearch, false);

        setEnabled();
    }

    
    private void setEnabled() {
        
        String resBtnData = m_resTypeBtnsHC.getSelection();

        setEnabled(resBtnData);
    }

    
    private void setEnabled(String value) {
        if (value.equals(FILE_BTN_DATA)) {
            fileResourceUIConfig();

        } else if (value.equals(FUNCTION_BTN_DATA)) {
            functionResourceUIConfig();
        
        } else if (value.equals(ADDRESS_BTN_DATA)) {
            addressResourceUIConfig();
        
        } else {
            functionResourceUIConfig();
        }
    }

    
    private void fileResourceUIConfig() {
            m_resNameLbl.setText(FILE_NAME_LBL);
            m_resourceNameGSC.setGlobalsProvider(GlobalsContainer.GC_MODULES, 
                                                 m_coreId);
            m_resourceNameGSC.setShowFunctionSource(false);

            enableSrcLineSpecificControls(true);
    } 
    
    
    private void functionResourceUIConfig() {
        m_resNameLbl.setText(FUNCTION_NAME_LBL);
        m_resourceNameGSC.setGlobalsProvider(GlobalsContainer.GC_ALL_FUNCTIONS, 
                                             null);
        m_resourceNameGSC.setShowFunctionSource(true);

        enableSrcLineSpecificControls(true);
    }
    
    
    private void addressResourceUIConfig() {

        m_resNameLbl.setText(ADDRESS_LBL);
        m_resourceNameGSC.setGlobalsProvider(GlobalsContainer.GC_CUSTOM_PROVIDER, 
                                             null);
        m_resourceNameGSC.setShowFunctionSource(true);

        enableSrcLineSpecificControls(false);
    }
    
    
    private void enableSrcLineSpecificControls(boolean isEnabled) {

        if (isEnabled) {
            boolean isSearch = m_location.isSearch() == ETristate.E_TRUE;
            enableSearchControls(isSearch);
        } else {
            enableSearchControls(false);
        }
        
        m_isSearchHC.setEnabled(isEnabled);
        m_lineNumberHC.setEnabled(isEnabled);
        m_lineOffsetHC.setEnabled(isEnabled);
        m_numStepsHC.setEnabled(isEnabled);
        m_lineLbl.setEnabled(isEnabled);
        m_searchLineLbl.setEnabled(isEnabled);
        m_lineOffsetLbl.setEnabled(isEnabled);
        m_numStepsLbl.setEnabled(isEnabled);

        m_showSrcBtn.setEnabled(isEnabled);
    }
    
    
    private void enableSearchControls(boolean isSearch) {
        if (isSearch) {
            ((Group)(m_searchGroup.getParent())).setText(SEARCH_CONFIGURATION_TXT + " - ON");
        } else {
            ((Group)(m_searchGroup.getParent())).setText(SEARCH_CONFIGURATION_TXT + " - OFF");
        }
        m_linesRangeLbl.setEnabled(isSearch);
        m_searchContextLbl.setEnabled(isSearch);
        m_matchTypeLbl.setEnabled(isSearch);
        m_searchPatternLbl.setEnabled(isSearch);
        
        m_searchGroup.getParent().setEnabled(isSearch);
        m_linesRangeHC.setEnabled(isSearch);
        m_searchContextBtnsHC.setEnabled(isSearch);
        m_matchTypeBtnsHC.setEnabled(isSearch);
        
        setPatternText(m_location.getMatchingType(), 
                       isSearch, false);
    }
}
