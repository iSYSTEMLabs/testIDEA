package si.isystem.itest.ui.spec.group;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
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
import si.isystem.connect.CTestFilterController;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestGroupResult.EGroupResultSection;
import si.isystem.connect.StrSet;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SEFormatter;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This page contains statistics about test cases in a group - how many of them 
 * exist, how many functions they test, ... 
 * 
 * @author markok
 *
 */
public class GroupGroupStatsEditor extends GroupSectionEditor {

    private KTable m_testCaseStatsTable;
    private KTable m_grpResultsTable;
    private Label m_failedSectionLbl;

    public GroupGroupStatsEditor(ENodeId nodeId, ESectionCTestGroup ... sectionId) {
        super(nodeId, sectionId);
    }

    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        
        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);

        MigLayout mig = new MigLayout("fill", "[min!][fill]", "");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        m_testCaseStatsTable = createTable(builder, 
                                 "Group test case statistics",
                                 true,
                                 new String[]{"In group", 
                                              "With test cases", 
                                              "Without test cases",
                                              "Tests/item"},
                                 new String[]{"Number of items in group.",
                                              "Number of items with test cases.",
                                              "Number of items without test cases.",
                                              "Average number of tests per item ( = number of test cases / all items in the group)."},
                                 new String[]{"Test cases", 
                                              "Functions", 
                                              "Modules", 
                                              "Partitions"});

        m_grpResultsTable = createTable(builder, 
              "Group test results",
              true,
              new String[]{"Passed", "Failed", "Error", ""},
              new String[]{"Number of items for which all test cases completed successfully.",
                           "Number of items for which at least one test case results do not "
                           + "match expected ones, and there are no test cases with error status.",
                           "Number of items for which at least one test case did not execute "
                           + "because of an error.",
                           ""},
              new String[]{"Test cases", "Functions", "Modules", "Partitions"});

        builder.label("Failed sections:");
        m_failedSectionLbl = builder.label("", "wrap", SWT.BORDER);
        
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }
    
    
    private KTable createTable(KGUIBuilder parentBuilder,
                                         String groupTitle,
                                         boolean isWrap,
                                         String [] colHeaders,  
                                         String [] colTooltips,  
                                         String [] rowHeaders) {

        String groupLayoutData = "grow, gaptop 12, span 2";

        if (isWrap) {
            groupLayoutData += ", wrap";
        }

        KGUIBuilder groupStatGrp = parentBuilder.group(groupTitle, groupLayoutData, true, 
                                                       "fill", "", "fill");

        KTable table = new KTable(groupStatGrp.getParent(), 
                                  true, 
                                  SWTX.FILL_WITH_LASTCOL | SWTX.AUTO_SCROLL);

        GroupStatTableModel model = new GroupStatTableModel(colHeaders, 
                                                            rowHeaders);
        model.setHeaderTooltips(colTooltips);
        model.configureCellWH(table, "_ With test cases _");
        table.setModel(model);
        table.setLayoutData("hmin 85, grow"); // 85 should be close to 
        // optimal table height - the idea is to prevent scrollers in table - the
        // main section scroller is enough.

        return table;                
    }

             
    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testGroup != null;
        
        m_testCaseStatsTable.setEnabled(isEnabled);
        m_grpResultsTable.setEnabled(isEnabled);

        GroupStatTableModel testCaseStatsModel = (GroupStatTableModel)m_testCaseStatsTable.getModel();
        GroupStatTableModel grpResultsModel = (GroupStatTableModel)m_grpResultsTable.getModel();

        if (!isEnabled) {
            testCaseStatsModel.clear();
            grpResultsModel.clear();
            return;            
        }
        
        fillGroupTestCaseStatistics(testCaseStatsModel, grpResultsModel);
    }

    
    protected void fillGroupTestCaseStatistics(GroupStatTableModel testCaseStatsModel,
                                               GroupStatTableModel grpResultsModel) {
        
        CTestFilterController filterCtrl = GlobalsConfiguration.instance().getActiveFilterController();
        StrVector partitions = new StrVector();
        StrVector modules = new StrVector();
        StrVector functions = new StrVector();
        
        try {
            filterCtrl.getTestItemsForFilter(m_testGroup, partitions, modules, functions);
            
            int allFuncsInGrp = (int)functions.size();
            int allModsInGrp = (int)modules.size();
            int allPartitsInGrp = (int)partitions.size();
            int allTestCasesInGroup = m_testGroup.getNoOfTestCasesInGroup();
            
            testCaseStatsModel.setContent(0, 0, allTestCasesInGroup);
            testCaseStatsModel.setContent(0, 1, allFuncsInGrp);
            testCaseStatsModel.setContent(0, 2, allModsInGrp);
            testCaseStatsModel.setContent(0, 3, allPartitsInGrp);
            
            StrSet partitionsWTestInGroup = new StrSet();
            StrSet modulesWTestInGroup = new StrSet();
            StrSet functionsWTestInGroup = new StrSet();

            filterCtrl.countTestCasesInGroup(m_testGroup, 
                                             partitionsWTestInGroup, 
                                             modulesWTestInGroup, 
                                             functionsWTestInGroup);

            int funcsWithTests = (int)functionsWTestInGroup.size();
            int modsWTest = (int)modulesWTestInGroup.size();
            int partitsWTest = (int)partitionsWTestInGroup.size();
            
            testCaseStatsModel.setContent(1, 0, "/");
            
            setBarDiagramCell(1, 1, testCaseStatsModel,
                              funcsWithTests, allFuncsInGrp);

            setBarDiagramCell(1, 2, testCaseStatsModel,
                              modsWTest, allModsInGrp);

            setBarDiagramCell(1, 3, testCaseStatsModel,
                              partitsWTest, allPartitsInGrp);

            testCaseStatsModel.setContent(2, 0, "/");
            testCaseStatsModel.setContent(2, 1, allFuncsInGrp - funcsWithTests);
            testCaseStatsModel.setContent(2, 2, allModsInGrp - modsWTest);
            testCaseStatsModel.setContent(2, 3, allPartitsInGrp - partitsWTest);

            // average test cases per function
            testCaseStatsModel.setContent(3, 0, "/");
            testCaseStatsModel.setContent(3, 1, String.format("%.1f", (double)allTestCasesInGroup / allFuncsInGrp));
            testCaseStatsModel.setContent(3, 2, String.format("%.1f", (double)allTestCasesInGroup / allModsInGrp));
            testCaseStatsModel.setContent(3, 3, String.format("%.1f", (double)allTestCasesInGroup / allPartitsInGrp));

            fillGroupTestResults(grpResultsModel, allTestCasesInGroup, allFuncsInGrp, allModsInGrp, allPartitsInGrp);
        } catch (Exception ex) {
            StatusView.getView().setDetailPaneText(StatusType.ERROR,
                "Can not show group result - make sure symbols are loaded (click refresh button).\n" +
                        SEFormatter.getInfoWithStackTrace(ex, 5));
            ex.printStackTrace();
        }
    }

    
    private void fillGroupTestResults(GroupStatTableModel grpResultsModel, 
                           int allTestCasesInGrp, 
                           int allFuncsInGrp,
                           int allModulesInGrp,
                           int allPartitionsInGrp) {
        
        CTestGroupResult groupResult = m_model.getGroupResult(m_testGroup); 
        if (groupResult != null) {
            
            int [] noOfAllItems = {allTestCasesInGrp, allFuncsInGrp, allModulesInGrp, allPartitionsInGrp};
            
            EGroupResultSection [] passSections = {EGroupResultSection.E_PASSED_TCS,
                                                   EGroupResultSection.E_PASSED_FUNCTIONS,
                                                   EGroupResultSection.E_PASSED_MODULES,
                                                   EGroupResultSection.E_PASSED_PARTITIONS};
            setResultsColumn(groupResult, 0, passSections, noOfAllItems, grpResultsModel,
                             ColorProvider.GREEN);
            
            EGroupResultSection [] failSections = {EGroupResultSection.E_FAILED_TCS,
                                                   EGroupResultSection.E_FAILED_FUNCTIONS,
                                                   EGroupResultSection.E_FAILED_MODULES,
                                                   EGroupResultSection.E_FAILED_PARTITIONS}; 
            setResultsColumn(groupResult, 1, failSections, noOfAllItems, grpResultsModel,
                             ColorProvider.LIGHT_RED);

            EGroupResultSection [] errSections = {EGroupResultSection.E_ERROR_TCS,
                                                  EGroupResultSection.E_ERROR_FUNCTIONS,
                                                  EGroupResultSection.E_ERROR_MODULES,
                                                  EGroupResultSection.E_ERROR_PARTITIONS}; 
            setResultsColumn(groupResult, 2, errSections, noOfAllItems, grpResultsModel,
                             ColorProvider.RED);
         
            m_failedSectionLbl.setText(groupResult.getFailedSections());
        }
    }

    
    private void setResultsColumn(CTestGroupResult grpResult,
                                  int col, 
                                  EGroupResultSection [] sections,
                                  int [] noOfAllItems,
                                  GroupStatTableModel grpResultsModel,
                                  int barColor) {
    
        int row = 0;
        for (EGroupResultSection section : sections) {
            int resultValue = grpResult.getIntValue(section);
            setBarDiagramCell(col, row, grpResultsModel, resultValue, noOfAllItems[row], barColor);
            row++;
        }
    }
    
    
    private void setBarDiagramCell(int dataCol, int dataRow, 
                                   GroupStatTableModel testCaseStatsModel,
                                   int fractionOfItems,
                                   int allItems) {
        setBarDiagramCell(dataCol, dataRow, testCaseStatsModel, fractionOfItems, allItems, 
                          ColorProvider.LIGHT_CYAN);
    }
    
    
    private void setBarDiagramCell(int dataCol, int dataRow, 
                                     GroupStatTableModel testCaseStatsModel,
                                     int fractionOfItems,
                                     int allItems, 
                                     int barColor) {
        
        ColorProvider colorProvider = ColorProvider.instance();
        Color color = colorProvider.getColor(barColor);
        
        float fraction = (float)fractionOfItems / allItems ;
        String fractionStr = String.format("%.1f", fraction * 100);
        String text = fractionStr + "% (" + fractionOfItems + "/" + allItems +")";
        BarDiagramContent cellContent = new BarDiagramContent(fraction, 
                                                              text);
        cellContent.setBarColors(new Color[]{color});
        cellContent.setBorderColor(colorProvider.getColor(ColorProvider.LIGHT_GRAY));
        testCaseStatsModel.setContent(dataCol, dataRow, cellContent);
    }    

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{};
    }
}


class GroupStatTableModel extends KTableModelAdapter {

    private String[] m_columnHeaders;
    private String[] m_columnTooltips;
    private String[] m_rowHeaders;
    private Object[][] m_content;
    private int m_minCellHeight;
    private int m_typCellWidth;
    
    private BarDiagramCellRenderer m_barDiagramRenderer = 
            new BarDiagramCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);

    
    GroupStatTableModel(String [] columnHeaders, String [] rowHeaders) {
        
        m_columnHeaders = columnHeaders;
        m_rowHeaders = rowHeaders;
        m_content = new Object[columnHeaders.length][rowHeaders.length];
        for (Object [] row : m_content) {
            Arrays.fill(row, "");
        }
    
        m_barDiagramRenderer.setForeground(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
    }
    
    
    void setContent(int dataCol, int dataRow, int value) {
        m_content[dataCol][dataRow] = Integer.toString(value);
    }
    
    
    void setContent(int dataCol, int dataRow, Float value) {
        m_content[dataCol][dataRow] = value;
    }
    
    
    void setContent(int dataCol, int dataRow, BarDiagramContent value) {
        m_content[dataCol][dataRow] = value;
    }
    
    
    void setContent(int dataCol, int dataRow, String value) {
        m_content[dataCol][dataRow] = value;
    }
    
    
    void setHeaderTooltips(String [] tooltips) {
        m_columnTooltips = tooltips;
    }
    
    
    void clear() {
        for (int colIdx = 0; colIdx < m_content.length; colIdx++) {
            Object[] row = m_content[colIdx];
            for (int rowIdx = 0; rowIdx < row.length; rowIdx++) {
                // keep type of contents
                if (m_content[colIdx][rowIdx] instanceof Float) {
                    m_content[colIdx][rowIdx] = new Float(0);
                } else {
                    m_content[colIdx][rowIdx] = "";
                }
            }
        }
    }
    
    
    public void configureCellWH(Control control, String typicalColumnText) {
        
        m_minCellHeight = FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
        m_typCellWidth = FontProvider.instance().getTextWidth(control, typicalColumnText);
    }
    
    @Override
    public Object doGetContentAt(int col, int row) {

        if (col == 0  &&  row == 0) {
            return "";  // top left cell is empty
        }
        
        if (col == 0) {
            return m_rowHeaders[row - 1];
        }
        
        if (row == 0) {
            return m_columnHeaders[col - 1];
        }
        
        return m_content[col - 1][row - 1];
    }

    
    @Override
    public String doGetTooltipAt(int col, int row) {
        
        if (col == 0  &&  row == 0) {
            return null;  // top left cell is empty
        }
        
        if (row == 0) {
            return m_columnTooltips[col - 1];
        }
        
        return null;
    }
    
    
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        // System.out.println("renderer: " + col + ", " + row);
        if (col < getFixedHeaderColumnCount()  ||  row < getFixedHeaderRowCount()) {
            return m_headerCellRenderer;
        }
        
        Object content = m_content[col-1][row -1];
        if (content instanceof Float  ||  content instanceof BarDiagramContent) {
            return m_barDiagramRenderer;
        }
        
        return m_textCellRenderer;
    }

    
    @Override
    public int doGetRowCount() {
        return m_rowHeaders.length + 1;
    }

    @Override
    public int doGetColumnCount() {
        return m_columnHeaders.length + 1;
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