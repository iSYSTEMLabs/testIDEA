package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EBorderLocation;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CProfilerTestResult;
import si.isystem.connect.CProfilerTestResult.ProfilerErrCode;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerSectionIds;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestProfilerStatistics.EProfilerStatisticsSectionId;
import si.isystem.connect.CTestProfilerTime;
import si.isystem.connect.CTestProfilerTime.EProfilerTimeSectionId;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.IconProvider.EOverlayId;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.SetCommentAction;
import si.isystem.itest.model.actions.sequence.SetSequenceItemAction;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.comp.TBControlCheckBox;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.swttableeditor.ITextFieldVerifier;
import si.isystem.tbltableeditor.IModelWithComment;
import si.isystem.tbltableeditor.TableMouseListener;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

public class ProfilerCodeAreaEditor extends ListEditorBase {

    public static class AreaMinMaxControls {

        private ValueAndCommentEditor m_areaTagEditor; // editor for area tags, like 'minTime', 
                                           // 'maxTime', 'totalTime', 'hits' 
        private TBControlText m_minHC;
        private TBControlText m_maxHC;
        Text m_resultTxt;
        private EProfilerStatisticsSectionId m_tagSectionId;
        
        AreaMinMaxControls(EProfilerStatisticsSectionId tagSectionId) {
            m_tagSectionId = tagSectionId;
        }
        
        public void setControls(TBControlText minTxt, TBControlText maxTxt) {
            
            m_minHC = minTxt;
            
            m_maxHC = maxTxt;
            
            m_areaTagEditor = ValueAndCommentEditor.newKey(m_tagSectionId.swigValue(), 
                                                           minTxt.getControl(), 
                                                           SWT.LEFT);
            m_minHC.setMainTagEditor(m_areaTagEditor);
        }
        
        void clear() {
            m_areaTagEditor.updateValueAndCommentFromTestBase(null);
            m_areaTagEditor.setEnabled(false);
            m_minHC.clearInput();
            m_maxHC.clearInput();
            m_resultTxt.setText("");
        }
        
        void markAsNoneditable() {
            m_minHC.markAsNoneditable();
            m_maxHC.markAsNoneditable();
            m_resultTxt.setBackground(ColorProvider.instance().getBkgNoneditableColor());
        }

        public void setInput(CTestProfilerStatistics profilerArea, boolean isMerged) {
            m_areaTagEditor.updateValueAndCommentFromTestBase(profilerArea);
            m_minHC.setInput(profilerArea, isMerged);
            m_maxHC.setInput(profilerArea, isMerged);
        }
        
        
        public void setResult(long result, CProfilerTestResult.ProfilerErrCode profilerErrCode) {
            if (result >= 0) {
                setResult(String.valueOf(result), profilerErrCode);
            } else {
                setResult("/", profilerErrCode);
            }
        }
        
        
        public void setResult(String result, CProfilerTestResult.ProfilerErrCode error) {
        
            m_resultTxt.setText(result);
            
            switch (error) {
            case ERR_NONE:
                m_resultTxt.setBackground(ColorProvider.instance().getBkgNoneditableColor());
                break;
            case ERR_MIN:
                m_resultTxt.setBackground(ColorProvider.instance().getErrorColor());
                m_minHC.markAsError();
                break;
            case ERR_MAX:
                m_resultTxt.setBackground(ColorProvider.instance().getErrorColor());
                m_maxHC.markAsError();
                break;
            case ERR_BOTH:
                m_resultTxt.setBackground(ColorProvider.instance().getErrorColor());
                m_minHC.markAsError();
                m_maxHC.markAsError();
                break;
            default:
                // program error, mark nothing
            }
        }
        
        public void setEnabled(boolean isEnabled) {
            m_minHC.setEnabled(isEnabled);
            m_maxHC.setEnabled(isEnabled);
        }
        
        public void addKeyListener(KeyListener listener) {
            m_minHC.addKeyListener(listener);
            m_maxHC.addKeyListener(listener);
        }

        public YamlScalar getTag() {
            return m_areaTagEditor.getScalarCopy();
        }

        public YamlScalar getMin() {
            return m_minHC.copyScalar();
        }

        public YamlScalar getMax() {
            return m_maxHC.copyScalar();
        }

        public void setMerged(boolean isMerged) {
            m_minHC.setMerged(isMerged);
            m_maxHC.setMerged(isMerged);
        }

        public EProfilerStatisticsSectionId getSectionId() {
            return m_tagSectionId;
        }
    }

    protected String m_statTableTooltip;
    private TBControlCheckBox m_measureAllFunctionsCb;
    
    private KTable m_statsTable;
    protected ProfilerStatsTableModel m_statsTableModel;
    private TableMouseListener m_mouseListener;

    protected AreaMinMaxControls m_hits; 
    
    private EAreaType m_areaType;
    private ISectionEditor m_parentEditor;

    public ProfilerCodeAreaEditor() {
        this(EAreaType.CODE_AREA);
        
        setListCommentSectionId(EProfilerSectionIds.E_SECTION_CODE_AREAS.swigValue());

        m_uiStrings.m_tableTitle = "Functions";
        m_uiStrings.m_tableTooltip = "Names of functions to be profiled. "
                                     + "Select function to view\n" 
                                     + "settings on the right.";
        m_uiStrings.m_funcNameLabel = "Function: "; 

        m_statTableTooltip = "This table contains expected limits for profiler results.\n\n"
                + "Net time - time spent for code inside function body, without sub-functions called from this function.\n"
                + "Gross time - time spent for code inside function body, AND sub-functions called from this function.\n"
                + "Call time - gross time plus time spent in other contexts (tasks, interrupts) between entry/exit of a function.\n" 
                + "Period time - time between function invocation. Total time is not defined for this type of time.\n"
                
                + "\nLow, High - interval boundaries. Because measured times may vary for various reasons, we can specify\n"
                + "            interval of expected values.\n"
                
                + "\nMin - minimum time of all function invocations or state values.\n"
                + "Max - maximum time of all function invocations or state values.\n"
                + "Average - average time of all function invocations or state values.\n"
                + "Total - sum of all times for function invocations or state values.\n"
                + "Min Start/End - when the minimum time was recorded.\n"
                + "Max Start/End - when the maximum time was recorded.\n\n"
                
                + "All times may be entered with units, underscores may be used for better readability, for example:\n"
                + "  123 ns\n"
                + "  123 ms\n"
                + "  1_234_567\n"
                + "The following units are allowed: 'ns', 'us', 'ms', and 's'.\n"
                + "If no unit is specified, nano seconds are assumed.";
        
        CTestBaseIdAdapter adapter = 
                new CTestBaseIdAdapter(EProfilerStatisticsSectionId.E_SECTION_AREA_NAME.swigValue(),
                                       ENodeId.PROFILER_CODE_AREAS_NODE) {
            
            
            @Override
            public String getId(CTestBase testBase) {
                CTestProfilerStatistics area = CTestProfilerStatistics.cast(testBase);
                return TestSpecificationModel.getFullProfilerAreaName(area, 
                                                                      EAreaType.CODE_AREA);
            }
            
            
            @Override
            public CTestBase createNew(CTestBase parentTestBase) {
                return new CTestProfilerStatistics(parentTestBase);
            }

            
            @Override
            public CTestBaseList getItems(boolean isConst) {
                if (m_currentTestSpec == null) {
                    return EMPTY_CTB_LIST;
                }
                return m_currentTestSpec.getAnalyzer(isConst)
                        .getProfiler(isConst).getAreas(EAreaType.CODE_AREA, isConst);
            }


            @Override
            public Boolean isError(int dataRow) {
                return isErrorInCodeProfilerStat(dataRow);
            }
        };
        
        setIdAdapter(adapter);
    }
    
    
    protected ProfilerCodeAreaEditor(EAreaType areaType) {
        super(areaType == EAreaType.CODE_AREA ? ENodeId.PROFILER_CODE_AREAS_NODE :
                                                ENodeId.PROFILER_DATA_AREAS_NODE,
              SectionIds.E_SECTION_ANALYZER);
        
        m_areaType = areaType;
    }

    
    public void setParentEditor(ISectionEditor profilerSpecEditor) {
        m_parentEditor = profilerSpecEditor;        
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);
        mainPanel.setLayout(new MigLayout("fill"));
        
        Composite areasPanel = super.createPartControl(mainPanel, -1, -1, ""); 
        areasPanel.setLayoutData("growx, growy");

        areasPanel.setLayout(new MigLayout("fill", 
                                           // area names table, separator, label, name controls
                                           "[pref!][min!][min!][fill]", 
                                           "[min!][min!][min!][min!][min!][fill]"));    

        KGUIBuilder builder = new KGUIBuilder(areasPanel);

        m_statsTable = new KTable(builder.getParent(), true, 
                                  SWT.MULTI
                                  | SWTX.AUTO_SCROLL 
                                  | SWTX.MARK_FOCUS_HEADERS 
                                  | SWTX.EDIT_ON_KEY);
        m_statsTable.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                             SWTBotConstants.PROFILER_STATS_KTABLE);
            
        m_statsTableModel = new ProfilerStatsTableModel(m_statsTable, m_areaType, m_statTableTooltip);

        m_statsTable.setModel(m_statsTableModel);

        m_statsTable.setLayoutData("wmin 0, spanx 3, growx, gaptop 20, wrap");
        
        // makes table exactly as high as needed - no scroller dangling below.
        m_statsTable.setNumRowsVisibleInPreferredSize(m_statsTableModel.getRowCount());

        m_mouseListener = new TableMouseListener(m_statsTable);

        AreaMinMaxControls minMaxTuple = new AreaMinMaxControls(EProfilerStatisticsSectionId.E_SECTION_HITS);
        m_hits = createHitsControls(builder, minMaxTuple, "Number of hits", "Number of hits");
        
        // m_statsListTableEditor.setSelection(0); // maybe it is better to
        // leave the first selection
        // to the customer

        enableListItemControls(false);

        return getScrollableParent(mainPanel);
    }


    @Override
    protected void createSectionSpecificCheckBox(KGUIBuilder builder) {
        if (m_areaType == EAreaType.CODE_AREA) {
            m_measureAllFunctionsCb = 
                    new TBControlCheckBox(builder, 
                                          CoverageStatisticsEditor.MEASURE_ALL_FUNCTIONS, 
                                          "If checked all functions are measured,\n"
                                          + "if unchecked, only functions listed below are measured.\n"
                                          + "Disabled, when predefined trigger is used (see section 'Analyzer').", 
                                          "gapleft 8, gaptop 3, gapbottom 5, split 2", 
                                          CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_IS_MEASURE_ALL_FUNCTIONS.swigValue(), 
                                          ENodeId.PROFILER_CODE_AREAS_NODE, null);
        }
    }

    
    @Override
    protected void createItemIdControls(KGUIBuilder builder) {
        createFuncNameControls(builder);
    }

    
    private AreaMinMaxControls createHitsControls(KGUIBuilder builder,
                                                  AreaMinMaxControls minMaxTuple,
                                                  String groupTitle,
                                                  String itemTooltip) {
        
        KGUIBuilder builderMin = builder.group(groupTitle, "span, wrap, gaptop 20", 
                                               new MigLayout("fillx", "[min!][min!][min!][fill]"),
                                               SWT.NONE);
        
        TBControlText minTxt = createLabelTextPrettyLabel(builderMin,
                                                          "Lower limit", itemTooltip,
                                                          minMaxTuple.getSectionId(),
                                                          0);
        
        minTxt.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                    "low " + groupTitle + m_areaType);

        // we should create only simple Text control here, but for code reuse reasons,
        // and this control is not editable so no events should be triggered,
        // a TBControlText is created
        minMaxTuple.m_resultTxt = (Text)(createLabelTextPrettyLabel(builderMin, 
                                                                    "Result", itemTooltip, 
                                                                    minMaxTuple.getSectionId(),
                                                                    2).getControl());
        minMaxTuple.m_resultTxt.setEditable(false);
        
        
        TBControlText maxTxt = createLabelTextPrettyLabel(builderMin, 
                                                          "Upper limit", itemTooltip, 
                                                          minMaxTuple.getSectionId(),
                                                          1);
        maxTxt.getControl().setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                                    "up " + groupTitle + m_areaType);
        minMaxTuple.setControls(minTxt, maxTxt);
        
        return minMaxTuple;
    }

    
    private TBControlText createLabelTextPrettyLabel(KGUIBuilder builder,
                                                     String mainLblString,
                                                     String itemTooltip,
                                                     EProfilerStatisticsSectionId eProfilerAreaSectionId,
                                                     int indexInList) {
        builder.label(mainLblString + ":");
        
        TBControlText tbText = 
            TBControlText.createForListElement(builder, 
                                               itemTooltip + ", " + mainLblString + ". -1 means 'Not Executed'.", 
                                               "wmin 120, gapleft 7, gapright 5, wrap", 
                                               eProfilerAreaSectionId.swigValue(), 
                                               m_nodeId, 
                                               null, 
                                               SWT.BORDER,
                                               indexInList,
                                               true);
        return tbText;
    }


    @Override
    public boolean isEmpty() {
        if (m_testSpec == null) {
            return true;
        }

        return (m_testSpec.getAnalyzer(true).getProfiler(true).getAreas(m_areaType, true).size() == 0);
    }
    
    @Override
    public boolean hasErrorStatus() {
        return isActive();
    }

    
    @Override
    public boolean isActive() {
        return m_parentEditor.isActive();
    }

    
    @Override
    public boolean isError(CTestResult result) {
        return result.isProfilerCodeError();
    }

        
    @Override
    public void copySection(CTestTreeNode destTestSpec) {
        CTestAnalyzerProfiler destProfiler = CTestSpecification.cast(destTestSpec).getAnalyzer(false).getProfiler(false);
        CTestAnalyzerProfiler srcProfiler = m_testSpec.getAnalyzer(true).getProfiler(true);
        destProfiler.assignCodeAreas(srcProfiler);
    }

    
    @Override
    public void clearSection() {
        CTestAnalyzerProfiler profiler = m_testSpec.getAnalyzer(false).getProfiler(false);
        
        int sectionId = m_areaType == EAreaType.CODE_AREA ? 
                        EProfilerSectionIds.E_SECTION_CODE_AREAS.swigValue() :
                        EProfilerSectionIds.E_SECTION_DATA_AREAS.swigValue();
                        
        SetTestObjectAction action = new SetTestObjectAction(profiler, 
                                                             sectionId, 
                                                             null, 
                                                             m_nodeId);

        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
        
        m_testSpec.clearEmptySections();
    }
    
    
    protected ITextFieldVerifier createVerifier() {
        
        ITextFieldVerifier verifier = new ITextFieldVerifier() {

            @Override
            public String verify(String[] data) {
                if (data[0].trim().length() == 0) {
                    return "Area name must not be empty!";
                }
                return null;
            }
            
            @Override
            public String format(String[] data) {
                return null;
            }            
        };
        
        return verifier;
    }


    @Override
    protected void fillSectionControls() {
        super.fillSectionControls();
        
        m_listCommentEditor.updateValueAndCommentFromTestBase(m_currentTestSpec.getAnalyzer(false).getProfiler(false));
        
        CTestAnalyzer analyzer = m_currentTestSpec.getAnalyzer(true);
        
        if (m_measureAllFunctionsCb != null) {
            CTestAnalyzerProfiler profiler = analyzer.getProfiler(true);
            if (analyzer.isPredefinedTrigger() == ETristate.E_TRUE) {
                m_measureAllFunctionsCb.setInput(null, false);
            } else {
                m_measureAllFunctionsCb.setInput(profiler, m_isInherited);
            }
        }
    }

    
    @Override
    protected void fillListItemControls(CTestBase testBase) {
        super.fillListItemControls(testBase);
        
        m_commentChangedListener.setTestBase(m_currentTestSpec.getAnalyzer(false).getProfiler(false));
        
        CTestProfilerStatistics stat = CTestProfilerStatistics.cast(testBase);
        fillAreaValueTag(stat.getAreaName(), stat);

        m_statsTableModel.setInput(stat);
        m_hits.setInput(stat, m_isInherited);
        m_statsTable.redraw();
        
        // now set results, if available
        CTestResult testCaseResult = m_model.getResult(m_testSpec);

        boolean isResultSet = false;
        
        CTestAnalyzer analyzer = m_currentTestSpec.getAnalyzer(true);
        if (testCaseResult != null  &&  analyzer.getRunMode() == ERunMode.M_START) {
            isResultSet = fillResultNumbers(stat, testCaseResult);
        }
        
        if (!isResultSet) {
            m_hits.setResult("/", CProfilerTestResult.ProfilerErrCode.ERR_NONE);
            m_statsTableModel.setResult(null);
        }
    }
    
    
    @Override
    protected void clearSectionControls() {
        super.clearSectionControls();

        if (m_measureAllFunctionsCb != null) {
            m_measureAllFunctionsCb.setInput(null, false);
        }
    }
    
    
    @Override
    protected void clearListItemControls() {

        super.clearListItemControls();
        m_hits.setInput(null, false);
        
        m_statsTableModel.setInput(null);
        m_statsTableModel.setResult(null);

        fillAreaValueTag("", null);
    }
    
    
    @Override
    protected void enableSectionControls(boolean isEnabled) {
        super.enableSectionControls(isEnabled);
    }
    
    
    @Override
    protected void enableListItemControls(boolean isEnabled) {
        super.enableListItemControls(isEnabled);
        
        m_statsTable.setEnabled(isEnabled);
        m_hits.setEnabled(isEnabled);
        m_mouseListener.setEditable(isEnabled);
    }


    /**
     * @param areaName name of profiler area  
     * @param area object with area stats
     */
    protected void fillAreaValueTag(String areaName, CTestProfilerStatistics area) {
        // implemented in derived class
    }


    protected boolean fillResultNumbers(CTestProfilerStatistics area,
                                        CTestResult testCaseResult) {
        
        boolean isResultSet = false;
        String areaName = area.getQualifiedAreaName(testCaseResult.getDefaultDownloadFile());

        CProfilerTestResult result = testCaseResult.getProfilerCodeResult(areaName);
        // it may happen, that user adds area after test run, and this new area is not
        // found in result
        if (result != null) {
            isResultSet = true;
            m_statsTableModel.setResult(result);
            m_hits.setResult(result.getMeasuredHits(), result.validateHits());
        }
        
        return isResultSet;
    }


    private Boolean isErrorInCodeProfilerStat(int dataRow) {

        CTestResult result = m_model.getResult(m_testSpec);

        if (result == null) {
            return null;
        }

        CTestBase cvrgStatTB = 
                m_currentTestSpec.getAnalyzer(true).getProfiler(true).getCodeAreas(true).get(dataRow);

        CTestProfilerStatistics area = CTestProfilerStatistics.cast(cvrgStatTB);
        String defaultDlFile = result.getDefaultDownloadFile();
        String qualifiedAreaName = area.getQualifiedAreaName(defaultDlFile);

        CProfilerTestResult areaResult = result.getProfilerCodeResult(qualifiedAreaName);
        return getResultStatus(areaResult);  
    }
    
    
    protected Boolean getResultStatus(CProfilerTestResult areaResult) {
        Boolean retVal = null; // can be null, if no result for this function point was found
        if (areaResult != null) {
            if (areaResult.isResultSet()) {  
                if (areaResult.isError()) {
                    retVal = Boolean.TRUE;
                } else {
                    retVal = Boolean.FALSE;
                }
            } else {
                if (areaResult.validateHits() != ProfilerErrCode.ERR_NONE) {
                    retVal = Boolean.TRUE;
                } 
            }
        }

        return retVal; // can be null, if no result for this function point was found 
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_CODE_AREAS.swigValue()};
    }
}



//            CTestProfilerStatistics area = m_testProfiler.getArea(m_areaType, selectedIdx);
//            RemoveFromTestBaseListAction action;
//            if (area.isAreaValueSet()) {
//                int idx = m_testProfiler.findArea(area.getAreaName(), area.getAreaValue());
//                CTestBaseList areas = m_testProfiler.getAreas(EAreaType.DATA_AREA, false);
//
//                action = new RemoveFromTestBaseListAction(areas, idx);
//            } else {
//                int idx = m_testProfiler.findArea(m_areaType, area.getAreaName());
//                CTestBaseList areas = m_testProfiler.getAreas(m_areaType, false);
//
//                action = new RemoveFromTestBaseListAction(areas, idx);
//            }
//        
//            action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
//            if (m_areaType == EAreaType.CODE_AREA) {
//                action.addDataChangedEvent(ENodeId.PROFILER_CODE_AREAS_NODE, m_testProfiler);
//            } else {
//                action.addDataChangedEvent(ENodeId.PROFILER_DATA_AREAS_NODE, m_testProfiler);
//            }
//            TestSpecificationModel.getActiveModel().execAction(action);


class ProfilerStatsTableModel extends KTableModelAdapter implements IModelWithComment {

    private static final String LOWER_BOUND = "Low";
    private static final String UPPER_BOUND = "High";
    private static final int NUM_MODEL_HDR_ROWS = 1;
    private static final int NUM_MODEL_HDR_COLS = 2;
    
    private final String HELP_TOOLTIP; 
    
    private String [] m_timeScopeHeader;
    private EProfilerStatisticsSectionId[] m_timeScopes;

    private String [] m_scopeLimitsHeader;
    
    private String [] m_timeTypeHeader = {"Min", "Max", "Average", "Total", 
                                          "Min Start", "Min End", "Max Start", "Max End"};
    
    private EProfilerTimeSectionId [] m_timeTypes = {
                                     EProfilerTimeSectionId.E_SECTION_MIN_TIME,
                                     EProfilerTimeSectionId.E_SECTION_MAX_TIME,
                                     EProfilerTimeSectionId.E_SECTION_AVERAGE_TIME,
                                     EProfilerTimeSectionId.E_SECTION_TOTAL_TIME,
                                     EProfilerTimeSectionId.E_SECTION_MIN_START_TIME,
                                     EProfilerTimeSectionId.E_SECTION_MIN_END_TIME,
                                     EProfilerTimeSectionId.E_SECTION_MAX_START_TIME,
                                     EProfilerTimeSectionId.E_SECTION_MAX_END_TIME,
    };

    
    private int m_rowHeight;
    private int m_firstColWidth;
    private int m_resultColWidth;

    private TextIconsCellRenderer m_centeredHeaderRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
    
    private TextIconsCellRenderer m_leftHeaderRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, true);
    
    private TextIconsCellRenderer m_bodyRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);
    
    private KTableCellEditorText2 m_textCellEditor = new KTableCellEditorText2();
    private CTestProfilerStatistics m_area;
    private CProfilerTestResult m_result;
    private boolean m_isTimeScopeVertical = true;
    private ENodeId m_editorNodeId;

    
    ProfilerStatsTableModel(Control control, EAreaType areaType, String tableTooltip) {

        if (areaType == EAreaType.CODE_AREA) {
            m_timeScopeHeader = new String[]{"Net", "Gross", "Call", "Period"};
            m_timeScopes = new EProfilerStatisticsSectionId[]{
                    EProfilerStatisticsSectionId.E_SECTION_NET_TIME,
                    EProfilerStatisticsSectionId.E_SECTION_GROSS_TIME,
                    EProfilerStatisticsSectionId.E_SECTION_CALL_TIME,
                    EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME,
                    };
            m_editorNodeId = ENodeId.PROFILER_CODE_AREAS_NODE;
            m_scopeLimitsHeader = new String[]{LOWER_BOUND, "Result", UPPER_BOUND, 
                                               LOWER_BOUND, "Result", UPPER_BOUND, 
                                               LOWER_BOUND, "Result", UPPER_BOUND, 
                                               LOWER_BOUND, "Result", UPPER_BOUND};
        } else {
            m_timeScopeHeader = new String[]{"Net", "Inactive", "Period"};
            m_timeScopes = new EProfilerStatisticsSectionId[]{
                    EProfilerStatisticsSectionId.E_SECTION_NET_TIME,
                    EProfilerStatisticsSectionId.E_SECTION_OUTSIDE_TIME,
                    EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME,
                    };
            m_editorNodeId = ENodeId.PROFILER_DATA_AREAS_NODE;
            m_scopeLimitsHeader = new String[]{LOWER_BOUND, "Result", UPPER_BOUND, 
                                               LOWER_BOUND, "Result", UPPER_BOUND, 
                                               LOWER_BOUND, "Result", UPPER_BOUND};
        }

        HELP_TOOLTIP = tableTooltip;
        
        FontProvider fontProvider = FontProvider.instance();
        int fontHeight = fontProvider.getDefaultFontHeight(Activator.getShell());
        m_rowHeight = (int)(fontHeight * 1.3 + 6);
        
        GC gc = new GC(control);
        m_firstColWidth = calcColWidth(fontProvider, gc, m_isTimeScopeVertical ? m_timeScopeHeader : m_timeTypeHeader);
        
        m_firstColWidth += TextIconsCellRenderer.ICON_SPACE * 2 + 8; // leave some space for icons
        m_resultColWidth = fontProvider.getTextWidth(gc, "Max Start") + TextIconsCellRenderer.ICON_SPACE * 2 + 8;
        m_centeredHeaderRenderer.setAlignment(SWTX.ALIGN_HORIZONTAL_CENTER | 
                                              SWTX.ALIGN_VERTICAL_CENTER);
        m_bodyRenderer.setAlignment(SWTX.ALIGN_HORIZONTAL_RIGHT |
                                    SWTX.ALIGN_VERTICAL_CENTER);
        gc.dispose();
    }


    private int calcColWidth(FontProvider fontProvider, GC gc, String [] cellText) {

        int colWidth = 30;
        for (int idx = 0; idx < cellText.length; idx++) {
            String content = cellText[idx];
            int width = fontProvider.getTextWidth(gc, content);
            colWidth = Math.max(m_firstColWidth, width);
        }
        return colWidth;
    }
    
    
    public void setInput(CTestProfilerStatistics area) {
        m_area = area;
    }

    
    public void setOrientation(boolean isTimeScopeVertical) {
        m_isTimeScopeVertical = isTimeScopeVertical;
    }
    
    
    public void setResult(CProfilerTestResult result) {
        m_result = result;
    }


    @Override
    public int getFixedHeaderRowCount() {
        return getTableRow(NUM_MODEL_HDR_COLS, NUM_MODEL_HDR_ROWS); // m_isTimeScopeHorizontal ? 2 : 1;
    }
    

    @Override
    public int getFixedHeaderColumnCount() {
        return getTableCol(NUM_MODEL_HDR_COLS, NUM_MODEL_HDR_ROWS); // return m_isTimeScopeHorizontal ? 1 : 2;
    }
    

    @Override
    public int getInitialRowHeight(int row) {
        return m_rowHeight;
    }
   
    
    @Override
    public int getInitialColumnWidth(int column) {
        if (column == 0) {
            return m_firstColWidth;
        }
        return m_resultColWidth;
    }

    
    @Override
    public Point doBelongsToCell(int col, int row) {
        
        int modelCol = getModelCol(col, row);
        int modelRow = getModelRow(col, row);

        if (modelCol == 0  &&  modelRow > 0) {
            modelRow -= NUM_MODEL_HDR_ROWS;

            // header 'Net Time' and other times should occupy 3 cells (Min, Max, and Result)
            modelRow = NUM_MODEL_HDR_ROWS + (modelRow - modelRow % 3);
            
            return getTableCoord(modelCol, modelRow);
        }
        
        /*
        if (m_isTimeScopeVertical) {
            if (row == 0  &&  col > 0) {
                col--; // for the header column
                // header 'Net Time' and other times should occupy 3 cells (Min, Max, and Result)
                return new Point(1 + (col - col % 3), row);
            }
        } else {
            if (col == 0  &&  row > 0) {
                row--; // for the header column
                // header 'Net Time' and other times should occupy 3 cells (Min, Max, and Result)
                return new Point(col, 1 + (row - row % 3));
            }
        } */
        
        return new Point(col, row);
    }
    
    
    @Override
    public Object doGetContentAt(int col, int row) {

        int modelCol = getModelCol(col, row);
        int modelRow = getModelRow(col, row);
        
        TextIconsContent content = new TextIconsContent();

        // header cells
        if (modelRow == 0) {
            // top left cell
            if (modelCol == 0) {
                content.setTooltip(EIconPos.ETopLeft, HELP_TOOLTIP);
                content.setIcon(EIconPos.ETopLeft, 
                                IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_10x10),
                                true);
                
                setMainErrorStatus(content);
                return content;
            }
            
            // second top cell
            if (modelCol < NUM_MODEL_HDR_COLS) {
                return ""; // top left corner is empty
            }
            
            setTimeTypeHeaderErrorStatus(modelCol, content);
            content.setText(m_timeTypeHeader[modelCol - NUM_MODEL_HDR_COLS]);
            return content;
        }

        // time scopes header (net, gross, call, ...)
        if (modelCol == 0) {
            content.setText(m_timeScopeHeader[(modelRow - NUM_MODEL_HDR_ROWS)/ 3]);
            return content;
        }

        // limits header (lower bound, result, upper bound)
        if (modelCol == 1) {
            setTimeScopeHeaderErrorStatus(modelRow, content);
            content.setText(m_scopeLimitsHeader[modelRow - NUM_MODEL_HDR_ROWS]);
            return content;
        }

        // body cells
        EProfilerStatisticsSectionId scope = row2TimeScope(modelRow);
        EProfilerTimeSectionId timeType = col2TimeType(modelCol);
        int boundsIdx = getBoundsIdx(modelRow);
        
        if (boundsIdx == 1) {
            // cells with results should have gray bkg. since they are not editable
            setResultCellErrorStatus(content, scope, timeType);
        } else {
            if (scope == EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME  &&
                timeType == EProfilerTimeSectionId.E_SECTION_TOTAL_TIME) {
                
                // Period time is an exception, because it has no total time 
                // defined in profiler statistics 
                content.setBackground(ColorProvider.instance().getColor(ColorProvider.VERY_LIGHT_GRAY));
            } else {
                // set cell content here
                content.setBackground(ColorProvider.instance().getColor(ColorProvider.WHITE));
                content.setEditable(true);
                boundsIdx /= 2;  // maps 0 --> 0, 2 --> 1
                setValueAndComment(content, scope, timeType, boundsIdx);
                setMinMaxCellErrorStatus(content, scope, timeType, boundsIdx);
            }
        }
        
        if (modelRow % 3 == 0) {
            content.setBorder(m_isTimeScopeVertical ? EBorderLocation.EBottom :
                                                      EBorderLocation.ERight,
                              ColorProvider.instance().getColor(ColorProvider.BLACK),
                              0);
        }
        return content;
    }


    private void setValueAndComment(TextIconsContent content, 
                                    EProfilerStatisticsSectionId scope, 
                                    EProfilerTimeSectionId timeType, 
                                    int boundsIdx) {

        if (m_area == null) {
            content.setText("");
            return;
        } 
        
        CTestProfilerTime timeScope = m_area.getTime(scope, true);

        String value = timeScope.getTime(timeType, boundsIdx);
        content.setText(value);

        String nlComment = "";
        String eolComment = "";
        CSequenceAdapter seq = new CSequenceAdapter(timeScope, timeType.swigValue(), true);
        
        if (seq.size() > boundsIdx) {
            nlComment = timeScope.getCommentForSeqElement(timeType.swigValue(), 
                                                          boundsIdx, 
                                                          CommentType.NEW_LINE_COMMENT);
        
            eolComment = timeScope.getCommentForSeqElement(timeType.swigValue(), 
                                                           boundsIdx, 
                                                           CommentType.END_OF_LINE_COMMENT);

            if (!nlComment.isEmpty()  ||  !eolComment.isEmpty()) {
                content.setTooltip(EIconPos.ETopLeft, YamlScalar.getToolTipText(nlComment, eolComment));
                content.setNlComment(nlComment);
                content.setEolComment(eolComment);
            }
        }
        
        if (!value.isEmpty()) {
            if (!nlComment.isEmpty()  ||  !eolComment.isEmpty()) {
                content.setIcon(EIconPos.ETopLeft, 
                              IconProvider.getOverlay(EOverlayId.EDITABLE_INFO),
                              true);
            } else {
                content.setIcon(EIconPos.ETopLeft, 
                              IconProvider.getOverlay(EOverlayId.EDITABLE_NO_INFO),
                              true);
            }
        } else {
            /* comment icons in empty cells have no meaning - they are always
             * empty and can not be edited.
            content.setIcon(EIconPos.ETopLeft, 
                          IconProvider.getOverlay(EOverlayId.NONEDITABLE_NO_INFO),
                          true);
             */
        }
    }


    private void setMainErrorStatus(TextIconsContent content) {
        if (m_result != null) {
            if (m_result.validateAllTimes(ProfilerErrCode.ERR_NONE) != ProfilerErrCode.ERR_NONE) {
                content.setIcon(EIconPos.EBottomLeft, 
                                IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY),
                                true);
            } else {
                content.setIcon(EIconPos.EBottomLeft, 
                                IconProvider.getOverlay(EOverlayId.TEST_OK_OVERLAY),
                                true);
            }
        }
    }


    private void setTimeScopeHeaderErrorStatus(int modelRow,
                                               TextIconsContent content) {
        EProfilerStatisticsSectionId scope = row2TimeScope(modelRow);
        if (m_result != null) {
            ProfilerErrCode errCode = m_result.validateTimeScopeForAllTimeTypes(scope);
            int boundsIdx = getBoundsIdx(modelRow);
            if (boundsIdx == 0) {
                if (errCode == ProfilerErrCode.ERR_BOTH  ||  errCode == ProfilerErrCode.ERR_MIN) {
                    content.setIcon(EIconPos.EBottomLeft, 
                                    IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY),
                                    true);
                    content.setTooltip(EIconPos.EBottomLeft, errCode.toString());
                }
            } else if (boundsIdx == 2) {
                if (errCode == ProfilerErrCode.ERR_BOTH  ||  errCode == ProfilerErrCode.ERR_MAX) {
                    content.setIcon(EIconPos.EBottomLeft, 
                                    IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY),
                                    true);
                    content.setTooltip(EIconPos.EBottomLeft, errCode.toString());
                }
            }
        }
    }


    // time types header (min, max, average, total, ...)
    private void setTimeTypeHeaderErrorStatus(int modelCol,
                                              TextIconsContent content) {
        if (m_result != null) {
            EProfilerTimeSectionId timeType = col2TimeType(modelCol);
            ProfilerErrCode errCode = m_result.validateTimeTypeForAllScopes(timeType);
            if (errCode != ProfilerErrCode.ERR_NONE) {
                content.setIcon(EIconPos.EBottomLeft, 
                                IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY),
                                true);
                content.setTooltip(EIconPos.EBottomLeft, errCode.toString());
            }
        }
    }


    private void setResultCellErrorStatus(TextIconsContent content,
                                          EProfilerStatisticsSectionId scope,
                                          EProfilerTimeSectionId timeType) {
        if (m_result != null) {
            content.setText(m_result.getMeasuredTime(scope, timeType));
            if (m_result.validateError(scope, timeType) == ProfilerErrCode.ERR_NONE) {
                content.setBackground(ColorProvider.instance().getColor(ColorProvider.VERY_LIGHT_GRAY));
            } else {
                content.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_RED));
            }
        } else {
            content.setBackground(ColorProvider.instance().getColor(ColorProvider.VERY_LIGHT_GRAY));
        }
    }


    private void setMinMaxCellErrorStatus(TextIconsContent content,
                           EProfilerStatisticsSectionId scope,
                           EProfilerTimeSectionId timeType,
                           int boundsIdx) {
        
        if (m_result != null) {
            ProfilerErrCode errCode = m_result.validateError(scope, timeType);
            if (boundsIdx == 0) {
                if (errCode == ProfilerErrCode.ERR_BOTH  ||  errCode == ProfilerErrCode.ERR_MIN) {
                    content.setIcon(EIconPos.EBottomLeft, 
                                    IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY),
                                    true);
                    content.setTooltip(EIconPos.EBottomLeft, errCode.toString());
                }
            } else {
                if (errCode == ProfilerErrCode.ERR_BOTH  ||  errCode == ProfilerErrCode.ERR_MAX) {
                    content.setIcon(EIconPos.EBottomLeft, 
                                    IconProvider.getOverlay(EOverlayId.TEST_ERR_OVERLAY),
                                    true);
                    content.setTooltip(EIconPos.EBottomLeft, errCode.toString());
                }
            }
        }
    }
    
    /** Returns row, column in table according to orientation and time scope and type. */
/*    private Point scopeType2ModelRowCol(EProfilerStatisticsSectionId timeScope, 
                                        EProfilerTimeSectionId timeType, 
                                        int boundIdx) {
        int typeLine = -1;
        for (int typeIdx = 0; typeIdx < m_timeScopes.length; typeIdx++) {
            if (m_timeTypes[typeIdx] == timeType) {
                typeLine = typeIdx;
                break;
            }
        }
        
        int scopeLine = -1;
        for (int scopeIdx = 0; scopeIdx < m_timeScopes.length; scopeIdx++) {
            if (m_timeScopes[scopeIdx] == timeScope) {
                scopeLine = scopeIdx * 3 + boundIdx;
                break;
            }
        }
        
        typeLine += NUM_MODEL_HDR_COLS;
        scopeLine += NUM_MODEL_HDR_ROWS;

        return new Point(typeLine, scopeLine);
    } */
    
    
    private EProfilerStatisticsSectionId row2TimeScope(int modelRow) {
        return m_timeScopes[(modelRow - NUM_MODEL_HDR_ROWS) / 3];
    }

    
    private EProfilerTimeSectionId col2TimeType(int modelCol) {
        return m_timeTypes[modelCol - NUM_MODEL_HDR_COLS];
    }

    
    private int getModelRow(int col, int row) {
        if (m_isTimeScopeVertical) {
            return row; // vertical is the same as model
        }
        return col;     // horizontal view is different from model
    }
    
    
    private int getModelCol(int col, int row) {
        if (m_isTimeScopeVertical) {
            return col; // vertical is the same as model
        }
        return row;     // horizontal view is different from model
    }
    

    private Point getModelCoord(int col, int row) {
        if (m_isTimeScopeVertical) {
            return new Point(col, row); // vertical is the same as model
        }
        
        return new Point(row, col);     // horizontal view is different from model
    }
    
    
    private int getTableRow(int col, int row) {
        return getModelRow(col, row);     
    }
    
    
    private int getTableCol(int col, int row) {
        return getModelCol(col, row);     
    }
    

    private Point getTableCoord(int modelCol, int modelRow) {
        // the same as above - coords are switched or not, but for clarity of caller
        // code two methods exist
        return getModelCoord(modelCol, modelRow);     // horizontal view is different from model
    }
    
    
    @Override
    public int doGetRowCount() {
        return getTableRow(m_timeTypeHeader.length + NUM_MODEL_HDR_COLS, 
                           m_scopeLimitsHeader.length + NUM_MODEL_HDR_ROWS);
    }

    @Override
    public int doGetColumnCount() {
        return getTableCol(m_timeTypeHeader.length + NUM_MODEL_HDR_COLS, 
                           m_scopeLimitsHeader.length + NUM_MODEL_HDR_ROWS);
    }
 

    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {

        int modelCol = getModelCol(col, row);
        
        if (modelCol >= NUM_MODEL_HDR_COLS) {
            int boundsIdx = getBoundsIdx(getModelRow(col, row));

            // only lower and upper bound have editors, result is not editable
            if (boundsIdx == 0  ||  boundsIdx == 2) {
                int modelRow = getModelRow(col, row);
                EProfilerStatisticsSectionId scope = row2TimeScope(modelRow);
                EProfilerTimeSectionId timeType = col2TimeType(modelCol);
            
                // Period time has no total time value in profiler statistics, so
                // it may have no editor.
                if (scope != EProfilerStatisticsSectionId.E_SECTION_PERIOD_TIME  ||
                    timeType != EProfilerTimeSectionId.E_SECTION_TOTAL_TIME) {
                    return m_textCellEditor;
                }
            }
        }
        
        return null;
    }

    
    /**
     * @param modelRow
     * @return 0 for lower bound, 1 for result, 2 for upper bound
     */
    private int getBoundsIdx(int modelRow) {
        return (modelRow - NUM_MODEL_HDR_ROWS) % 3;
    }
   

    @Override
    public void doSetContentAt(int col, int row, Object value) {
        
        AbstractAction action = createSetContentAction(col, row, value.toString(),
                                                       null, null);
        
        if (action.isModified()) {
            action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
            action.addDataChangedEvent(m_editorNodeId, m_area);
            TestSpecificationModel.getActiveModel().execAction(action);
        }
    }

    
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        int modelCol = getModelCol(col, row);
        int modelRow = getModelRow(col, row);

        if (modelCol < NUM_MODEL_HDR_COLS) {
            return m_leftHeaderRenderer;
        } else if (modelRow < NUM_MODEL_HDR_ROWS) {
            return m_centeredHeaderRenderer;
        }
        return m_bodyRenderer;
    }


    @Override
    public void createSetCommentAction(int col,
                                      int row,
                                      String nlComment,
                                      String eolComment,
                                      KTable table) {
        int modelCol = getModelCol(col, row);
        int modelRow = getModelRow(col, row);

        EProfilerStatisticsSectionId scope = row2TimeScope(modelRow);
        EProfilerTimeSectionId timeType = col2TimeType(modelCol);
        int boundsIdx = getBoundsIdx(modelRow) / 2; // maps 0 --> 0, 2 --> 1 
        
        CTestProfilerTime profilerTime = m_area.getTime(scope, false);
        
        YamlScalar scalar = YamlScalar.newListElement(timeType.swigValue(), 
                                                      boundsIdx);
        
        scalar.setNewLineComment(nlComment);
        scalar.setEndOfLineComment(eolComment);

        SetCommentAction action = new SetCommentAction(profilerTime, 
                                                       ENodeId.ANAL_PROFILER_NODE, 
                                                       scalar);
        action.addDataChangedEvent(ENodeId.ANAL_PROFILER_NODE, profilerTime);
        action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
        TestSpecificationModel.getActiveModel().execAction(action);
        table.redraw(); // to change icon
    }
    
    
    @Override
    public AbstractAction createSetContentAction(int col, int row, String value, 
                                                 String nlComment, String eolComment) {
        
        KTableCellEditor editor = doGetCellEditor(col, row);
        if (editor == null) {
            return null;
        }
        int modelCol = getModelCol(col, row);
        int modelRow = getModelRow(col, row);

        EProfilerStatisticsSectionId scope = row2TimeScope(modelRow);
        EProfilerTimeSectionId timeType = col2TimeType(modelCol);
        int boundsIdx = getBoundsIdx(modelRow) / 2; // maps 0 --> 0, 2 --> 1 
        
        CTestProfilerTime profilerTime = m_area.getTime(scope, false);
        
        YamlScalar scalar = YamlScalar.newListElement(timeType.swigValue(), 
                                                      boundsIdx);
        scalar.setValue(value);
        
        if (nlComment != null) {
            scalar.setNewLineComment(nlComment);
        }
        
        if (eolComment != null) {
            scalar.setEndOfLineComment(eolComment);
        }
        
        SetSequenceItemAction action = new SetSequenceItemAction(profilerTime, 
                                                                 m_editorNodeId, 
                                                                 scalar);
        action.addDataChangedEvent(m_editorNodeId, profilerTime);
        return action;
    }
}