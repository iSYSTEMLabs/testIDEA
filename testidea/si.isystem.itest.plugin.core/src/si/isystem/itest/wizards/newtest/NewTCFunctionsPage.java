package si.isystem.itest.wizards.newtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.editors.ContentProposalConfig;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.connect.CAddressController;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.EAnalyzerSectionId;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerSectionIds;
import si.isystem.connect.CTestAnalyzerCoverage.ECoverageSectionId;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestUserStub;
import si.isystem.connect.ETristate;
import si.isystem.connect.IConnectDebug.ESymbolFlags;
import si.isystem.connect.data.JFunction;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.EBool;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.TSUtils;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.model.actions.testBaseList.InsertToTestBaseListAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.wizards.newtest.NewTCWizardDataModel.FuncCfg;
import si.isystem.itest.wizards.newtest.NewTCWizardDataModel.SectionCfg;
import si.isystem.tbltableeditor.CellEditorTristate;
import si.isystem.tbltableeditor.TristateCellRenderer;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;

public class NewTCFunctionsPage extends GlobalsWizardDataPage {

    static final String FUNC_CALL_LEVEL_SEPARATOR = "---";
    private KTable m_funcsTable;
    private FuncTableModel m_funcsTableModel;
    
    private static final String PAGE_TITLE = "Functions";


    public NewTCFunctionsPage(NewTCWizardDataModel ntcModel) {
        super(PAGE_TITLE);
        setTitle(PAGE_TITLE);
        setDescription("Select functions to be stubbed, measured, ...");
        
        m_ntcModel = ntcModel;
    }
   

    @Override
    public void createControl(Composite parent) {
        setControl(createPage(parent));
    }
    
    
    @Override
    public Composite createPage(Composite parent) {
        
        Composite container = new Composite(parent, SWT.NULL);

        // wizard dialog size is set in handler
        container.setLayout(new MigLayout("fill", "fill", "fill"));

        // KGUIBuilder builder = new KGUIBuilder(container);
        
        m_funcsTable = new KTable(container, true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                  SWTX.EDIT_ON_KEY | 
                                  SWTX.MARK_FOCUS_HEADERS | 
                                  SWTX.FILL_WITH_DUMMYCOL | 
                                  SWT.BORDER);
        
        int minCellHeight = FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
        int cellWidthForCreateCol = FontProvider.instance().getTextWidth(m_funcsTable, 
                                                                        "_Create_");
        int typCellWidth = FontProvider.instance().getTextWidth(m_funcsTable, 
                                                                "_ Return val. _");

        m_funcsTableModel = new FuncTableModel(m_ntcModel, 
                                               minCellHeight, 
                                               new int[]{cellWidthForCreateCol, 
                                                         typCellWidth, 
                                                         typCellWidth});
        m_funcsTable.setModel(m_funcsTableModel);
        m_funcsTable.setLayoutData("");
        
        fillControls();
        return container;
    }
    
    
    private void fillControls() {
    }
    
    
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }


    @Override
    public void dataToModel() {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void dataFromModel() {
        FuncCfg funcCfg = m_ntcModel.new FuncCfg(m_ntcModel.m_funcUnderTestName);
        m_ntcModel.m_functionConfigs.clear();
        m_ntcModel.m_functionConfigs.add(funcCfg);
        m_funcsTableModel.configureContentProposals();
        
        List<String> unknownFuncs = new ArrayList<>(); // funcs called but have 
                                                       // no symbol info - called funcs
                                                       // are not available. Currently
                                                       // ignored, because we can do
                                                       // nothing about it - or 
                                                       // should we provide address?
        List<String> calledFunctions = getCalledFunctions(unknownFuncs);
        for (String calledFunc : calledFunctions) {
            m_ntcModel.m_functionConfigs.add(m_ntcModel.new FuncCfg(calledFunc));
        }
    }

    
    @Override
    public AbstractAction createModelChangeAction(CTestSpecification testSpec) {
        
        GroupAction grpAction = new GroupAction("Add stubs, test points, analyzer areas");
        
        for (FuncCfg funcCfg : m_ntcModel.m_functionConfigs) {
        
            if (funcCfg.m_stub.m_isCreated) {
                
                CTestStub stub = TSUtils.createStub(testSpec, 
                                                    funcCfg.m_functionName, 
                                                    funcCfg.m_stub.m_itemParams);
                
                CTestBaseList stubsList = testSpec.getStubs(false);
                grpAction.add(new InsertToTestBaseListAction(stubsList, stub , -1));
            }
            
            if (funcCfg.m_userStub.m_isCreated) {
                
                CTestUserStub userStub = TSUtils.createUserStub(testSpec, 
                                                                funcCfg.m_functionName, 
                                                                funcCfg.m_userStub.m_itemParams);

                CTestBaseList stubsList = testSpec.getUserStubs(false);
                grpAction.add(new InsertToTestBaseListAction(stubsList, userStub , -1));
            }
            
            if (funcCfg.m_testPoint.m_isCreated) {
                CTestPoint testPoint = TSUtils.createTestPoint(testSpec, 
                                                               funcCfg.m_functionName, 
                                                               funcCfg.m_testPoint.m_itemParams);
                CTestBaseList tpList = testSpec.getTestPoints(false);
                grpAction.add(new InsertToTestBaseListAction(tpList, testPoint , -1));
            }
            
            if (funcCfg.m_coverage.m_isCreated) {
                CTestAnalyzer analyzer = configureAnalyzer(testSpec, grpAction);      
                configureCoverage(analyzer, funcCfg, grpAction);
            }
            
            if (funcCfg.m_profiler.m_isCreated) {
                CTestAnalyzer analyzer = configureAnalyzer(testSpec, grpAction);      
                configureProfiler(analyzer, funcCfg, grpAction);
            }
        }

        return grpAction;
    }


    /**
     * When wizard page is invoked from existing test case,
     * then analyzer may already have been set and it is not a good idea to 
     * reconfigure it, so only empty doc name is set. 
     */
    private CTestAnalyzer configureAnalyzer(CTestSpecification testSpec, GroupAction grpAction) {
        CTestAnalyzer analyzer = testSpec.getAnalyzer(false);
        int runModeSection = EAnalyzerSectionId.E_SECTION_RUN_MODE.swigValue();
        YamlScalar value = YamlScalar.newMixed(runModeSection);
        String runModeStart = analyzer.enum2Str(runModeSection, ERunMode.M_START.swigValue());
        value.setValue(runModeStart);
        SetSectionAction action = new SetSectionAction(analyzer, 
                                                       ENodeId.ANALYZER_NODE, 
                                                       value);
        grpAction.add(action);
        
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null  &&  analyzer.getDocumentFileName().isEmpty()) {
            String analyzerDocFName = model.getCEnvironmentConfiguration().
                                      getToolsConfig(true).getAnalyzerFName();
            value = YamlScalar.newMixed(EAnalyzerSectionId.E_SECTION_DOC_FILE_NAME.swigValue());
            value.setValue(analyzerDocFName);
            action = new SetSectionAction(analyzer, 
                                          ENodeId.ANALYZER_NODE, 
                                          value);
            grpAction.add(action);
        }
        
        return analyzer;
    }


    private void configureCoverage(CTestAnalyzer analyzer, FuncCfg funcCfg, GroupAction grpAction) {

        CTestAnalyzerCoverage coverage = analyzer.getCoverage(false);
        YamlScalar value = YamlScalar.newMixed(ECoverageSectionId.E_SECTION_IS_ACTIVE.swigValue());
        value.setValue(EBool.tristate2Str(ETristate.E_TRUE));
        SetSectionAction action = new SetSectionAction(coverage, 
                                                       ENodeId.ANAL_COVERAGE_NODE, 
                                                       value);
        grpAction.add(action);
        
        CTestCoverageStatistics cvrgStat = TSUtils.createCoverageStats(coverage,
                                                                   funcCfg.m_functionName, 
                                                                   funcCfg.m_coverage.m_itemParams);
        CTestBaseList statList = coverage.getStatisticsList(false);
        grpAction.add(new InsertToTestBaseListAction(statList, cvrgStat , -1));
    }


    private void configureProfiler(CTestAnalyzer analyzer, 
                                   FuncCfg funcCfg, 
                                   GroupAction grpAction) {
        CTestAnalyzerProfiler profiler = analyzer.getProfiler(false);
        YamlScalar value = YamlScalar.newMixed(EProfilerSectionIds.E_SECTION_IS_ACTIVE.swigValue());
        value.setValue(EBool.tristate2Str(ETristate.E_TRUE));
        SetSectionAction action = new SetSectionAction(profiler, 
                                                       ENodeId.ANAL_PROFILER_NODE, 
                                                       value);
        grpAction.add(action);
        
        CTestProfilerStatistics profStat = TSUtils.createProfilerStats(profiler,
                                                                       funcCfg.m_functionName, 
                                                                       funcCfg.m_profiler.m_itemParams);
        
        CTestBaseList profStatList = profiler.getAreas(EAreaType.CODE_AREA, false);
        grpAction.add(new InsertToTestBaseListAction(profStatList, profStat , -1));
    }

    
    /** 
     * This method is duplicated from CAddressController to avoid double
     * conversion of parameter types.
     */
    String []getFunctionNames(CAddressController addrCtrl,
                              long []addresses, 
                              boolean isUseAddressIfUnknown)
    {
        String[] funcNames = new String[addresses.length];

        int idx = 0;
        for (long address : addresses) {
            String funcName = addrCtrl.getAnySymbolAtAddress(ESymbolFlags.sFunctions.swigValue() |
                                                             ESymbolFlags.sLabels.swigValue(),
                                                             (short)0, 
                                                             address,
                                                             ESymbolFlags.sScopeWide);
            if (isUseAddressIfUnknown  &&  funcName.isEmpty()) {
                funcName = "0x" + Long.toHexString(address);
            }
            funcNames[idx++] = funcName;
        }
        
        return funcNames;
    }

    
    private void walkFuncTree(String functionName,
                              CAddressController addrCtrl, 
                              FunctionGlobalsProvider globalFunctionsProvider,
                              Set<String> allCalledFuncsSet,
                              List<String> allCalledFuncsList,
                              List<String> unknownFuncs) {

        try {
            JFunction jFunc = globalFunctionsProvider.getCachedFunction(functionName);

            if (jFunc != null) {

                long[] calledFuncs = jFunc.getCalledFunctions();

                String[] funcNames = getFunctionNames(addrCtrl, calledFuncs, true);
                // eliminate duplicates, when the same function is called at two addresses
                Set<String> uniqFuncNames = new TreeSet<>(); 
                uniqFuncNames.addAll(Arrays.asList(funcNames));

                Set<String> addedFuncNames = new TreeSet<>(); 
                for (String uniqFuncName : uniqFuncNames) {
                    // System.out.println("'" + uniqFuncName + "'" );
                    if (!allCalledFuncsSet.contains(uniqFuncName)) {
                        allCalledFuncsList.add(uniqFuncName);
                        allCalledFuncsSet.add(uniqFuncName);
                        addedFuncNames.add(uniqFuncName);
                    }
                }

                if (!addedFuncNames.isEmpty()) {
                    allCalledFuncsList.add(FUNC_CALL_LEVEL_SEPARATOR);
                }

                for (String addedFuncName : addedFuncNames) {
                    walkFuncTree(addedFuncName, addrCtrl, globalFunctionsProvider,  
                                 allCalledFuncsSet, allCalledFuncsList, unknownFuncs);
                }
            } else {
                unknownFuncs.add(functionName);
            }
        } catch (SException ex) {
            SExceptionDialog.open(getShell(), 
                                  "Error when traversing functions call tree!", 
                                  ex);
        }
    }
    
    
    private List<String> getCalledFunctions(List<String> unknownFuncs) {

        FunctionGlobalsProvider globalFunctionsProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getFuncGlobalsProvider(m_ntcModel.m_coreId);

        Set<String> allCalledFuncsSet = new TreeSet<>();
        List<String> allCalledFuncsList = new ArrayList<>();

        JConnection jConn = ConnectionProvider.instance().getDefaultConnection();
        CAddressController addrCtrl = jConn.getAddrController(m_ntcModel.m_coreId);
        walkFuncTree(m_ntcModel.m_funcUnderTestName, addrCtrl, 
                     globalFunctionsProvider, allCalledFuncsSet, 
                     allCalledFuncsList, unknownFuncs);
        
        // remove separator if it is the last item in the list 
        int lastItemIdx = allCalledFuncsList.size() - 1;
        if (lastItemIdx >= 0  &&  
                allCalledFuncsList.get(lastItemIdx).equals(FUNC_CALL_LEVEL_SEPARATOR)) {
            allCalledFuncsList.remove(lastItemIdx);
        }
        return allCalledFuncsList;
    }
}


class FuncTableModel extends KTableModelAdapter {

    private final static int NUM_HDR_ROWS = 2;
    private final static int NUM_HDR_COLUMNS = 1; 

    private final static String[] SECTION_HEADERS = {"Stub", 
                                                     "User Stub",
                                                     "Test Point",
                                                     "Coverage",
                                                     "Profiler"}; 
    
    private final static String[] PARAM_HEADERS = {"Create", "Return val.",
                                                   "Create", "Repl. func.",
                                                   "Create", "Test p. ID",
                                                   "Create", "Code cvrg.", "Cond. cvrg.",
                                                   "Create", "Min gross", "Max gross"};

    private final static String m_tooltip = 
            "This table lists functions called from tested function. Functions called\n"
            + "with indirect branching are not shown.\n"
            + "For each function you can specify sections, which should be created\n"
            + "in new test case by selecting check-box in column 'Create'.\n"
            + "Next column can be used to specify one or two of section parameters.";
    
    private static final int NUM_STUB_COLUMNS = 4; // 2 columns for stubs and 2 for user stubs 

    private TextIconsContent m_tooltipCellContent = new TextIconsContent();
    private TristateCellRenderer m_tristateCellRenderer = 
                new TristateCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
    private KTableCellEditorText2 m_textCellEditor;
    private CellEditorTristate m_tristateEditor;

    NewTCWizardDataModel m_ntcModel;
    private int m_numDataColumns;
    private int[] m_belongsToCellHeader;
    private int m_minCellHeight;
    private int[] m_dataColWidths;
    private AsystContentProposalProvider m_funcProposals;

    // next: 
    // - list of all called functions
    FuncTableModel(NewTCWizardDataModel ntcModel, int minCellHeight, 
                   int []dataColWidths) {
        m_ntcModel = ntcModel;
        m_minCellHeight = minCellHeight;
        m_dataColWidths = dataColWidths;

        // model should always have at least one item - function under test.
        m_numDataColumns = m_ntcModel.getNumDataColumnsInFuncTable();
        if (m_numDataColumns != PARAM_HEADERS.length) {
            throw new SIllegalStateException("Num columns in sections and num items in header do not match!").
                add("numDataColumns", m_numDataColumns).add("hdrLen", PARAM_HEADERS.length);
        }
        m_belongsToCellHeader = m_ntcModel.getBelongsToCellForHeaderInFuncTable();
        
        m_tooltipCellContent.setIcon(EIconPos.ETopLeft, 
                                     IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_10x10),
                                     true);
        m_tooltipCellContent.setTooltip(EIconPos.ETopLeft, m_tooltip);
        
        m_textCellEditor = new KTableCellEditorText2();
        Rectangle bounds = TristateCellRenderer.IMAGE_CHECKED.getBounds();
        m_tristateEditor = new CellEditorTristate(new Point(bounds.width, 
                                                            bounds.height), // approx. size 
                                                  SWTX.ALIGN_HORIZONTAL_CENTER, 
                                                  SWTX.ALIGN_VERTICAL_CENTER);
        
    }
    
    
    @Override
    public int getInitialRowHeight(int row) {
        return m_minCellHeight;
    }
    
    
    @Override
    public int getInitialColumnWidth(int column) {
        if (column < NUM_HDR_COLUMNS) {
            return m_dataColWidths[0] * 4; // estimate for function names 
        } else { // data columns
            int dataCol = column - NUM_HDR_COLUMNS;
            MutableInt paramIdx = new MutableInt();
            getSectionIdx(dataCol, paramIdx);
            return m_dataColWidths[paramIdx.intValue() + 1]; // +1 for isCreated column
        }
    }
    
    
    @Override
    public int getFixedHeaderRowCount() {
        return NUM_HDR_ROWS;
    }
    
    
    @Override
    public int doGetRowCount() {
        return m_ntcModel.m_functionConfigs.size() + NUM_HDR_ROWS;
    }


    @Override
    public int doGetColumnCount() {
        return m_numDataColumns + NUM_HDR_COLUMNS;
    }
    
    
    @Override
    public Object doGetContentAt(int col, int row) {

        int dataRow = row - NUM_HDR_ROWS;
        int dataCol = col - NUM_HDR_COLUMNS;
        
        if (col < NUM_HDR_COLUMNS) {
            if (row == 0) {
                return m_tooltipCellContent;
            }
            if (row >= NUM_HDR_ROWS) {
                return m_ntcModel.m_functionConfigs.get(dataRow).getFunctionName();
            }
            return "";
        }

        if (dataRow == 0  &&  dataCol < NUM_STUB_COLUMNS) {
            return ""; // no text for stubs for function under test 
        }

        switch (row) {
        case 0:
            return SECTION_HEADERS[getSectionIdx(dataCol, null)];  
        case 1:
            return PARAM_HEADERS[dataCol];  
        default:
            if (m_ntcModel.m_functionConfigs.get(dataRow).getFunctionName() == 
                    NewTCFunctionsPage.FUNC_CALL_LEVEL_SEPARATOR) {
                return "";
            }

            MutableInt paramIdx = new MutableInt();
            int sectionIdx = getSectionIdx(dataCol, paramIdx);
            FuncCfg funcCfg = m_ntcModel.m_functionConfigs.get(dataRow);
            SectionCfg sectionCfg = funcCfg.getSections()[sectionIdx];
            if (paramIdx.intValue() < 0) {
                TextIconsContent value = new TextIconsContent();
                value.setTristateValue(sectionCfg.m_isCreated ? 
                                           ETristate.E_TRUE.name() : ETristate.E_FALSE.name());
                return value;
            }
            return sectionCfg.m_itemParams[paramIdx.intValue()];
        }
    }
    
    
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        // System.out.println("renderer: " + col + ", " + row);
        if (col == 0  &&  row == 0) {
            return new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
        }
        if (col < getFixedHeaderColumnCount()  ||  row < getFixedHeaderRowCount()) {
            if (row == NUM_HDR_ROWS) {
                // mark function under test with special background color
                FixedCellRenderer renderer = 
                        new FixedCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, 
                                              false);
                renderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
                return renderer;
            } 
            return m_headerCellRenderer;
        }
        
        int dataCol = col - NUM_HDR_COLUMNS;
        int dataRow = row - NUM_HDR_ROWS;
        
        if (m_ntcModel.m_functionConfigs.get(dataRow).getFunctionName() == 
                NewTCFunctionsPage.FUNC_CALL_LEVEL_SEPARATOR) {
            return m_textCellRenderer; // no check-boxes in separator lines 
        }
        
        if (dataRow == 0  &&  dataCol < NUM_STUB_COLUMNS) {
            TextCellRenderer renderer = new TextCellRenderer(SWT.NONE);
            renderer.setBackground(ColorProvider.instance().getColor(ColorProvider.VERY_LIGHT_GRAY));
            return renderer; // no check-boxes for stubs for function under test 
        }
        
        MutableInt paramIdx = new MutableInt();
        getSectionIdx(dataCol, paramIdx);
        
        if (paramIdx.intValue() < 0) {
            return m_tristateCellRenderer;
        }
        
        return m_textCellRenderer;
    }

    
    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {
        
        int dataCol = col - NUM_HDR_COLUMNS;
        int dataRow = row - NUM_HDR_ROWS;

        if (m_ntcModel.m_functionConfigs.get(dataRow).getFunctionName() == 
                NewTCFunctionsPage.FUNC_CALL_LEVEL_SEPARATOR) {
            return null; 
        }
        
        if (dataRow == 0  &&  dataCol < NUM_STUB_COLUMNS) {
            return null; // no editor for stubs for function under test 
        }
        
        MutableInt paramIdx = new MutableInt();
        int sectionIdx = getSectionIdx(dataCol, paramIdx);
        
        if (paramIdx.intValue() < 0) {
            return m_tristateEditor;
        }
        
        FuncCfg funcCfg = m_ntcModel.m_functionConfigs.get(dataRow);
        SectionCfg sectionCfg = funcCfg.getSections()[sectionIdx];
        if (sectionCfg.m_isNeedsContentProvider) {
            ContentProposalConfig cfg = new ContentProposalConfig(new String[0]);
            UiUtils.setContentProposalsConfig(cfg);
            cfg.setProposalProvider(m_funcProposals);
            cfg.setProposalsAcceptanceStyle(m_funcProposals.getProposalsAcceptanceStyle());
            
            return new KTableCellEditorText2(cfg);
            
        }
        
        return m_textCellEditor;
    }

    
    @Override
    public String doGetTooltipAt(int col, int row) {
        if (col == 0  &&  row == 0) {
            return m_tooltipCellContent.getTooltip(EIconPos.ETopLeft);
        }
        return null;
    }


    @Override
    public Point doBelongsToCell(int col, int row) {

        if (row == 1  &&  col == 0) {
            return new Point(0, 0);
        }
        
        if (row == 0  &&  col > NUM_HDR_COLUMNS) {
            col = m_belongsToCellHeader[col - NUM_HDR_COLUMNS] + NUM_HDR_COLUMNS;
        }
        
        return new Point(col, row);
    }

    
    @Override
    public void doSetContentAt(int col, int row, Object value) {
        int dataCol = col - NUM_HDR_COLUMNS;
        int dataRow = row - NUM_HDR_ROWS;
        MutableInt paramIdx = new MutableInt();
        int sectionIdx = getSectionIdx(dataCol, paramIdx);
        
        FuncCfg funcCfg = m_ntcModel.m_functionConfigs.get(dataRow);
        SectionCfg sectionCfg = funcCfg.getSections()[sectionIdx];
        if (paramIdx.intValue() < 0) {
            sectionCfg.m_isCreated = Boolean.parseBoolean(value.toString());
        } else {
            sectionCfg.m_itemParams[paramIdx.intValue()] = value.toString(); 
        }
    }

    
    /**
     * Returns section index in this table from data table column. 
     * 
     * @param paramIdx if not null, it will contain index of section column 
     * on return, which means -1 for column with check-box, 0, 1, ... for parameters.
     */
    private int getSectionIdx(int dataCol, MutableInt paramIdx) {
        
        FuncCfg funcCfg = m_ntcModel.new FuncCfg("");
        SectionCfg[] sections = funcCfg.getSections();
        int sectionIdx = 0;
        
        for (SectionCfg section : sections) {
            
            if (paramIdx != null) {
                paramIdx.setValue(dataCol - 1);
            }
            
            dataCol -= section.getNumColumns();
            if (dataCol < 0) {
                return sectionIdx;
            }
            sectionIdx++;
        }
        
        return sectionIdx;
    }
    
    
    void configureContentProposals() {
        FunctionGlobalsProvider globalFunctionsProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getFuncGlobalsProvider(m_ntcModel.m_coreId);

        m_funcProposals = 
            new AsystContentProposalProvider(globalFunctionsProvider.getCachedGlobals(), 
                                             globalFunctionsProvider.getCachedDescriptions());
        m_funcProposals.setFiltering(true);
        m_funcProposals.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);        
    }
}
