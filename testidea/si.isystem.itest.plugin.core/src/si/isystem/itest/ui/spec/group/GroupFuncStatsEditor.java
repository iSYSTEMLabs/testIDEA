package si.isystem.itest.ui.spec.group;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.renderers.BarDiagramCellRenderer;
import de.kupzog.ktable.renderers.BarDiagramContent;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestGroupResult.EGroupResultSection;
import si.isystem.connect.CTestGrpFuncStat;
import si.isystem.connect.CTestGrpFuncStat.ESectionFuncTestStats;
import si.isystem.connect.data.JFunction;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;

public class GroupFuncStatsEditor extends GroupSectionEditor {

    private KTable m_funcStatsTable;
    private Button m_isShowAllFunctionsCb;

    private Label m_minTestCasesPerFuncInGrpLbl;
    private Label m_maxTestCasesPerFuncInGrpLbl;
    private Label m_averageTestCasesPerFuncInGrpLbl; 
    
    
    public GroupFuncStatsEditor(ENodeId nodeId, ESectionCTestGroup ... sectionId) {
        super(nodeId, sectionId);
    }

    @Override
    public Composite createPartControl(Composite parent) {
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        
        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);

        MigLayout mig = new MigLayout("fill", "[fill][min!][min!][min!][min!][min!][min!][min!]", "[min!][fill]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        m_isShowAllFunctionsCb = builder.checkBox("Show all functions", "");
        m_isShowAllFunctionsCb.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillControlls();
            }
        });

        Label lbl = builder.label("Test cases / func:", "gapleft 30, al right");
        lbl.setFont(FontProvider.instance().getBoldControlFont(lbl));
        
        builder.label("  Min:", "al right");
        m_minTestCasesPerFuncInGrpLbl = builder.label("", "w 60:60:60, al right", SWT.BORDER);
        m_minTestCasesPerFuncInGrpLbl.setToolTipText("Each function in this group has at least this number of test cases.");
        
        builder.label("  Max:", "al right");
        m_maxTestCasesPerFuncInGrpLbl = builder.label("", "w 60:60:60, al right", SWT.BORDER);
        m_maxTestCasesPerFuncInGrpLbl.setToolTipText("Each function in this group has at most this number of test cases.");
                
        builder.label("  Average:", "al right");
        m_averageTestCasesPerFuncInGrpLbl = builder.label("", "w 60:60:60, al right, wrap", SWT.BORDER);
        m_averageTestCasesPerFuncInGrpLbl.setToolTipText("Functions in this group have on average this number of test cases.");
                
        m_funcStatsTable = createTable(builder,
            new String[]{"Partition", 
                         "Module", 
                         "Function",
                         
                         "# test cases",
                         "Pass/Fail/Error",
                         
                         "Code cvrg.",
                         "Cond. cvrg.",
                         "Exec count"},
                         
            new String[]{"Name of download file", 
                         "Name of source code file", 
                         "Name of function",
                         
                         "Number of test cases for function",
                         "Number of passed, failed and error test cases for function",
                         
                         "Object code coverage for function",
                         "Object code condition coverage for function. Any execution is counted.",
                         
                         "Execution count - how many times function was called.\n"
                         + "May be greater than the number of test cases if it was called "
                         + "from other tested functions."});
        
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }

    
    private KTable createTable(KGUIBuilder builder, String [] colHeaders, String [] tooltips) {
        
        KTable table = new KTable(builder.getParent(), 
                                  true, 
                                  SWTX.FILL_WITH_LASTCOL | SWTX.AUTO_SCROLL);

        FuncStatTableModel model = new FuncStatTableModel(colHeaders);
        model.setHeaderTooltips(tooltips);
        
        model.configureCellWH(table, "_ xx.x% (100.0/99999) _");
                                                                     // "_ # test cases _");
        table.setModel(model);
        table.setLayoutData("gaptop 10, hmin 85, grow, span 8"); // 85 should be close to 
        
        return table;
    }
    
    
    @Override
    public void fillControlls() {
        
        CTestGroupResult groupResult = m_model.getGroupResult(m_testGroup); 
        
        if (groupResult == null) {
            CTestBaseList funcStatList = new CTestBaseList();
            ((FuncStatTableModel)m_funcStatsTable.getModel()).setContent(funcStatList, 0);
        } else {
            CTestBaseList funcStatList = 
                groupResult.getTestBaseList(EGroupResultSection.E_FUNC_STATS.swigValue(), 
                                            true);

            float avgTestsPerFunc = 0;
            String avgTests = groupResult.getTagValue(EGroupResultSection.E_AVG_TCS_FOR_FUNCTION.swigValue());
            if (!avgTests.isEmpty()) {
                avgTestsPerFunc = Float.parseFloat(avgTests);
            }
            String maxTestsPerFunction = groupResult.getTagValue(EGroupResultSection.E_MAX_TCS_FOR_FUNCTION.swigValue());

            m_minTestCasesPerFuncInGrpLbl.setText(" " + groupResult.getTagValue(EGroupResultSection.E_MIN_TCS_FOR_FUNCTION.swigValue()));
            m_averageTestCasesPerFuncInGrpLbl.setText(String.format(" %.1f", avgTestsPerFunc));
            m_maxTestCasesPerFuncInGrpLbl.setText(" " + maxTestsPerFunction);

            int maxTests = 1;
            if (!maxTestsPerFunction.isEmpty()) {
                maxTests = Integer.parseInt(maxTestsPerFunction);
            }
            ((FuncStatTableModel)m_funcStatsTable.getModel()).setContent(funcStatList, 
                                                                         maxTests);
        }
        
        m_funcStatsTable.redraw();
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{};
    }
}


class FuncStatTableModel extends KTableModelAdapter {

    private static final int COL_PARTITION = 0;
    private static final int COL_MODULE = 1;
    private static final int COL_FUNCTION = 2;
    private static final int COL_NO_OF_TESTS = 3;
    private static final int COL_PASS_FAIL_ERROR = 4;
    private static final int COL_CVRG_CODE = 5;
    private static final int COL_CVRG_COND = 6;
    private static final int COL_CVRG_EXEC_COUNT = 7;
    // private static final int COL_CVRG_COND_BOTH = 8;
    // private static final int COL_CVRG_LINES = 6;
    
    private String[] m_columnHeaders;
    private String[] m_columnTooltips;
    private int m_minCellHeight;
    private int m_typCellWidth;
    
    private int m_maxNoOfTestsPerFunc;
    private CTestBaseList m_rows = new CTestBaseList();

    private BarDiagramCellRenderer m_barDiagramRenderer = 
            new BarDiagramCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
    
    private Color[] m_coverageBarColor = new Color[]{ 
                  ColorProvider.instance().getColor(0xa2ffa2)};

    
    FuncStatTableModel(String [] columnHeaders) {
        
        m_columnHeaders = columnHeaders;
    
        m_barDiagramRenderer.setForeground(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
    }
    
    
    @Override
    public int getFixedHeaderColumnCount() {
        return 3;
    }

    
    void setContent(CTestBaseList rows, int maxNoOfTestsPerFunc) {
        m_rows = rows;
        m_maxNoOfTestsPerFunc = maxNoOfTestsPerFunc;
    }
    
    
    void setHeaderTooltips(String [] tooltips) {
        m_columnTooltips = tooltips;
    }
    
    
    void clear() {
        m_rows.clear();
    }
    
    
    public void configureCellWH(Control control, String typicalColumnText) {
        
        m_minCellHeight = FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
        m_typCellWidth = FontProvider.instance().getTextWidth(control, typicalColumnText);
    }
    
    @Override
    public Object doGetContentAt(int col, int row) {

        if (row == 0) {
            return m_columnHeaders[col];
        }
        
        ColorProvider colorProvider = ColorProvider.instance();
        CTestGrpFuncStat tableRow = CTestGrpFuncStat.cast(m_rows.get(row - 1));
        int noOfTestCases = tableRow.getNoOfTestCases();
        
        String qualFuncName = tableRow.getTagValue(ESectionFuncTestStats.E_QUAL_FUNC_NAME.swigValue());
        StringBuilder module = new StringBuilder();
        StringBuilder scopedFuncName = new StringBuilder();
        StringBuilder signature = new StringBuilder();
        StringBuilder partition = new StringBuilder();
        JFunction.parseQualifiedName(qualFuncName, module, scopedFuncName, signature, partition);
        
        switch (col) {
        case COL_PARTITION:
            return partition.toString();
        case COL_MODULE:
            return module.toString();
        case COL_FUNCTION:
            scopedFuncName.append(signature);
            return scopedFuncName.toString();
        case COL_NO_OF_TESTS:
            BarDiagramContent bar = new 
            BarDiagramContent(m_maxNoOfTestsPerFunc > 0 ? (float)noOfTestCases / m_maxNoOfTestsPerFunc : 0,
                              Integer.toString(noOfTestCases));
            bar.setBarColors(new Color[]{colorProvider.getColor(ColorProvider.BLUE_D0)});
            return bar;
        case COL_PASS_FAIL_ERROR:
            
            long testsPassed = tableRow.getIntValue(ESectionFuncTestStats.E_TESTS_PASSED.swigValue());
            long testsFailed = tableRow.getIntValue(ESectionFuncTestStats.E_TESTS_FAILED.swigValue());
            long testsError = tableRow.getIntValue(ESectionFuncTestStats.E_TESTS_ERROR.swigValue());
            
            StringBuilder text = new StringBuilder();
            text.append(testsPassed).append(" / ").append(testsFailed)
                .append(" / ").append(testsError);
            
            BarDiagramContent statusBar;
            if (noOfTestCases > 0) {
                statusBar = 
                    new BarDiagramContent(new float[]{
                            (float) testsPassed / noOfTestCases,
                            (float) testsFailed / noOfTestCases,
                            (float) testsError / noOfTestCases},
                            
                            new Color[]{colorProvider.getColor(ColorProvider.GREEN),
                            colorProvider.getColor(ColorProvider.LIGHT_RED),
                            colorProvider.getColor(ColorProvider.RED)});
                statusBar.setText(text.toString());
            } else {
                statusBar = new BarDiagramContent(0, text.toString());
            }
            return statusBar;
        }
        
            switch (col) {
            case COL_CVRG_CODE:
                return createBarContent(tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_BYTES_EXECUTED),
                                        tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_BYTES_ALL));
            case COL_CVRG_COND:
                return createBarContent(tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_COND_FALSE),
                                        tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_COND_TRUE),
                                        tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_COND_BOTH),
                                        tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_COND_ALL));
            case COL_CVRG_EXEC_COUNT:
                return Long.toString(tableRow.getCoverage(ESectionFuncTestStats.E_CVRG_EXECUTION_COUNT));
            default:
                new Throwable().printStackTrace();
            }
 
        return "?";
    }


    protected Object createBarContent(int covered, int all) {
        if (all == 0) {
            return new BarDiagramContent(0, "/");
        }
        
        float fraction = (float)covered / all;
        BarDiagramContent bar = new BarDiagramContent(fraction, 
                                                      String.format("%.1f", fraction * 100) + "% (" + covered + '/' + all + ')');
        bar.setBarColors(m_coverageBarColor );
        return bar; 
    }

    
    protected Object createBarContent(int condFalse, int condTrue, int condBoth, int all) {
        if (all == 0) {
            return new BarDiagramContent(0, "/");
        }
        
        int outcomes = all * 2 ;
        
        float fraction = (float)(condFalse + condTrue + condBoth * 2) / outcomes;
        BarDiagramContent bar = new BarDiagramContent(fraction, 
                                                      String.format("%.1f", fraction * 100) + "% (" + condFalse + "f, " + condTrue + "t, " + condBoth + "b) / " + outcomes);
        bar.setBarColors(m_coverageBarColor );
        return bar; 
    }

    
    @Override
    public String doGetTooltipAt(int col, int row) {
        
        if (row == 0) {
            return m_columnTooltips[col];
        }
        
        return null;
    }
    
    
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        
        if (row == 0) {
            return m_headerCellRenderer;
        }

        if (col <= COL_FUNCTION) {
            return m_headerCellRenderer; // the first three columns are header columns
                                         // to differentiate them from result data
        }
        
        switch (col) {
        case COL_NO_OF_TESTS:
        case COL_PASS_FAIL_ERROR:
        case COL_CVRG_CODE:
        case COL_CVRG_COND:
            return m_barDiagramRenderer;
        case COL_CVRG_EXEC_COUNT:
            return m_textCellRenderer;
        default:
            System.out.println("col = " + col);
            new Throwable().printStackTrace();
        }
        
        return m_textCellRenderer;
    }

    
    @Override
    public int doGetRowCount() {
        return (int)m_rows.size() + 1;
    }

    @Override
    public int doGetColumnCount() {
        return m_columnHeaders.length;
    }
    
    
    @Override
    public int getInitialRowHeight(int row) {
        return m_minCellHeight;
    }

    
    @Override
    public int getInitialColumnWidth(int column) {
        return m_typCellWidth;
    }
}