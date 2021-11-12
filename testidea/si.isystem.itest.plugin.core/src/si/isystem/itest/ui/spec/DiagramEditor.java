package si.isystem.itest.ui.spec;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.WorkbenchPart;

import de.kupzog.ktable.KTableCellSelectionListener;
import net.miginfocom.swt.MigLayout;
import si.isystem.commons.utils.ISysFileUtils;
import si.isystem.connect.CProfilerController2;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerExportFormat;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EDiagType;
import si.isystem.connect.CTestDiagramConfig.ETestDiagramSections;
import si.isystem.connect.CTestDiagrams;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.ISysPathFileUtils;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.diagrams.DiagramUtils;
import si.isystem.itest.diagrams.ViewerComposite;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.model.actions.testBaseList.InsertToTestBaseListAction;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.ISysModelChangedListener;
import si.isystem.tbltableeditor.TestBaseListTable;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;


public class DiagramEditor extends SectionEditorAdapter {

    private static final String _ISYS_TEMPLATE_FOR_MATPLOTLIB_PY = "_isys_templateForMatplotlib.py";
    private static final String _ISYS_TEMPLATE_FOR_GRAPHWIZ_PY = "_isys_templateForGraphwiz.py";
    private static final String TIP_CLICK_BELOW_TO_ADD_A_DIAGRAM = "(click '+' below to add a diagram)";
    protected TBControlTristateCheckBox m_isInheritTB;
    private TestBaseListTable m_testBaseTable;

    private WorkbenchPart m_parentView;
    private Composite m_viewerParentComposite;
    private ViewerComposite m_viewerComposite;
    
    private TBControlTristateCheckBox m_isActiveTCb;
    private ValueAndCommentEditor m_sectionTagEditor;
    
    private Button m_createSelectedBtn;
    private Button m_showDiagramBtn;
    private Button m_autoConfigureProfilerBtn;
    
    // this test spec. is needed for diagram opening and creation, because
    // it contains all the information needed for these operations - from base and 
    // current test base.
    private CTestSpecification m_mergedOrCurrentTestSpec;
    private Label m_tipLbl;

    enum EDiagramOperations {E_CREATE, E_OPEN};
    
    
    public DiagramEditor(WorkbenchPart parentView) {
        super(ENodeId.DIAGRAMS, SectionIds.E_SECTION_DIAGRAMS);
        
        m_parentView = parentView;
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);

        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);
        MigLayout mig = new MigLayout("fill", "", "");
        mainPanel.setLayout(mig);

        SashForm sash = new SashForm(mainPanel, SWT.VERTICAL);
        //FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(sash);
        sash.setLayoutData("growy, growx");
        
        sash.SASH_WIDTH = 3;
        
        createTablePanel(sash);

        m_viewerParentComposite = new Composite(sash, SWT.BORDER);
        //m_viewerParentComposite.setLayout(new MigLayout("fill", "fill", "fill"));
        m_viewerParentComposite.setLayout(new FillLayout());
        
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }


    private void createTablePanel(SashForm sash) {
        Composite tablePanel = new Composite(sash, SWT.BORDER);
        
        tablePanel.setLayout(new MigLayout("fill", 
                                           "[min!][fill][min!]",
                                           "[min!][min!][fill]"));
        
        KGUIBuilder builder = new KGUIBuilder(tablePanel);
        
        m_isActiveTCb = 
                new TBControlTristateCheckBox(builder,
                                              "Is active",
                                              "If checked, diagrams in this section are created after test run.",
                                              "", // mig layout
                                              CTestDiagrams.ETestDiagramsSections.E_SECTION_DIAG_IS_ACTIVE.swigValue(),
                                              ENodeId.DIAGRAMS,
                                              null);
        
        m_sectionTagEditor = ValueAndCommentEditor.newKey(CTestSpecification.SectionIds.E_SECTION_DIAGRAMS.swigValue(), 
                                                          m_isActiveTCb.getControl(), 
                                                          SWT.LEFT);
        m_isActiveTCb.setMainTagEditor(m_sectionTagEditor);
        
        m_isInheritTB = createTristateInheritanceButton(builder, 
                                                        "skip, al right , gapright 5, wrap");
        m_isInheritTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_DIAGRAMS));
        
        builder.label("Diagrams:", "gapleft 8, gaptop 10");
        m_tipLbl = builder.label(TIP_CLICK_BELOW_TO_ADD_A_DIAGRAM, "gapleft 8, gaptop 10");
        
        createLocalToolbarButtons(builder);

        createTable(tablePanel);
    }


    public void createLocalToolbarButtons(KGUIBuilder builder) {
        // re-runs selected
        m_createSelectedBtn = builder.button("Create", "split 4, gapright 10");  
                                       // script, useful also if it is not marked as active
        UiTools.setToolTip(m_createSelectedBtn, "Creates diagrams selected below.\n"
                + "Note that for some diagrams (for example sequence diagram and\n"
                + "call graph) testcase has to be executed first.");
        
        m_createSelectedBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    processSelectedDiagrams(EDiagramOperations.E_CREATE);
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not create diagram!", 
                                          ex);
                }
            }
        });

        ControlDecoration helpDecoration = new ControlDecoration(m_createSelectedBtn, 
                                                                 SWT.TOP | SWT.RIGHT);
        helpDecoration.setImage(IconProvider.INSTANCE.getIcon(EIconId.EHelpContents_10x10));
        helpDecoration.setDescriptionText("Image manipulation shortcuts:\n\n"
                + "- mouse drag:          pan image around\n"
                + "- mouse wheel:         scroll up/down\n"
                + "- Shift + mouse wheel: pan left/right\n"
                + "- Ctrl + mouse wheel:  zoom\n"
                + "- key 'R': reset transform\n"
                + "- key '+': zoom in\n"
                + "- key '-': zoom out\n"
                + "");
        
        
        m_showDiagramBtn = builder.button("Show", "split 5");
        UiTools.setToolTip(m_showDiagramBtn, "Opens diagrams selected below in the configured viwer.");
        m_showDiagramBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    processSelectedDiagrams(EDiagramOperations.E_OPEN);
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Can not open diagram!", 
                                          ex);
                }
            }
        });

        m_autoConfigureProfilerBtn = builder.button("Auto-configure profiler", "");
        UiTools.setToolTip(m_autoConfigureProfilerBtn, "Changes profiler settings so that sequence diagram and\n"
                + "call graph can be drawn. Use Undo to revert te changes.");
        
        m_autoConfigureProfilerBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                autoConfigureProfiler();
            }
        });
        
        Button btn = builder.button("GW template", "");
        UiTools.setToolTip(btn, "Creates template script file for graphwiz graphs.\n"
                + "Graphwiz tool is deployed with winIDEA.");
        btn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                createTemplate(_ISYS_TEMPLATE_FOR_GRAPHWIZ_PY);
            }
        });

        btn = builder.button("MP template", "wrap");
        UiTools.setToolTip(btn, "Creates template script file for Matplotlib figures.\n"
                + "Matplotlib is part of winIDEA Python distribution.");
        btn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                createTemplate(_ISYS_TEMPLATE_FOR_MATPLOTLIB_PY);
            }
        });
    }

    
    protected void createTemplate(String fileName) {
        
        String winIDEAWorkspaceDir = ISysPathFileUtils.getWinIDEAWorkspaceDir();
        File destDir = new File(winIDEAWorkspaceDir);

        Path filePath = Paths.get(winIDEAWorkspaceDir, fileName);
        boolean isCopy = true;
        
        if (Files.exists(filePath)) {
            isCopy = MessageDialog.openQuestion(Activator.getShell(), 
                                                    "File already exists", 
                                                    "Script file\n    " + filePath.toString() 
                                                    + "\nalready exists.\n\n"
                                                    + "Do you want to overwrite it?");
        }
        
        if (isCopy) {
            ISysFileUtils.copyFileFromPlugin(DiagramUtils.DIAGRAM_SRC_DIR,
                                             destDir, 
                                             fileName, 
                                             StandardCopyOption.REPLACE_EXISTING,
                                             Activator.PLUGIN_ID);
        
            CTestDiagrams diagrams = m_currentTestSpec.getDiagrams(false);
            CTestBaseList diagList = diagrams.getConfigurations(false);
            CTestDiagramConfig insertedTb = new CTestDiagramConfig(diagrams);

            StrVector enumValues = new StrVector();
            insertedTb.getEnumValues(ETestDiagramSections.E_SECTION_DIAG_CFG_DIAG_TYPE.swigValue(), 
                                     enumValues);

            insertedTb.setTagValue(CTestDiagramConfig.ETestDiagramSections.E_SECTION_DIAG_CFG_DIAG_TYPE.swigValue(), 
                                   enumValues.get(EDiagType.ECustom.swigValue()));

            insertedTb.setTagValue(CTestDiagramConfig.ETestDiagramSections.E_SECTION_DIAG_CFG_SCRIPT.swigValue(), 
                                   fileName);

            insertedTb.setTagValue(CTestDiagramConfig.ETestDiagramSections.E_SECTION_DIAG_CFG_OUT_FILE_NAME.swigValue(), 
                                   getDefaultImageFileName());

            InsertToTestBaseListAction action = new InsertToTestBaseListAction(diagList, insertedTb, -1);
            TestSpecificationModel.getActiveModel().execAction(action);
            m_testBaseTable.refresh();

            MessageDialog.openInformation(Activator.getShell(), 
                                          "Template created", 
                                          "Template file\n    " + filePath.toString() + 
                    "\nhas been created.\n\nIt can be modified to draw custom diagrams.\n"
                    + "Renaming is recommended.");
        }
    }


    private void processSelectedDiagrams(EDiagramOperations operation) {
        Point[] selection = m_testBaseTable.getControl().getCellSelection();
        if (selection.length < 1) {
            MessageDialog.openWarning(Activator.getShell(), 
                "No diagram selected!", 
                "Please select at least one row with diagram configuration below.");
            return;
        }

        CTestBaseList diagConfigs = m_currentTestSpec.getDiagrams(true).getConfigurations(true);

        // get working dir now, because after the first diagram is opened in
        // editor, image editor becomes the active one so test model is no
        // longer available.
        String wiWorkspaceDir = ISysPathFileUtils.getWinIDEAWorkspaceDir();
        String reportDir = ISysPathFileUtils.getAbsReportDir();
        Set<Integer> processedDiagIdxSet = new TreeSet<>();
        
        for (int rowIdx = 0; rowIdx < selection.length; rowIdx++) {

            int diagIdx = getDiagramIndex(selection[rowIdx].y);

            if (diagIdx >= diagConfigs.size()) {
                MessageDialog.openWarning(Activator.getShell(), 
                    "Can not create diagram for row without configuration!",
                    "Invalid row selected: " + diagIdx + "\n" +
                    "Please select at least one row with diagram configuration below.");
                return;
            }
            
            if (processedDiagIdxSet.contains(diagIdx)) {
                // When multiple cells in one row are selected, do not create 
                // diagram in this row twice.
                continue;  
            }
            processedDiagIdxSet.add(diagIdx);
            
            CTestBase diagObj = diagConfigs.get(diagIdx);
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagObj);

            switch(operation) {
            case E_CREATE:
                // currently there is no timeout, as scripts should be written so that
                // they always return.
                TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                CTestResult result = model.getResult(m_testSpec);
                
                DiagramUtils.createDiagram(wiWorkspaceDir,
                                           reportDir,
                                           m_mergedOrCurrentTestSpec, 
                                           diagConfig,
                                           result,
                                           0);
                showDiagramInBottomPane(diagIdx);
                break;
            case E_OPEN:
                DiagramUtils.openDiagram(m_mergedOrCurrentTestSpec, diagConfig, wiWorkspaceDir);
                break;
            default:
                MessageDialog.openWarning(Activator.getShell(), 
                                          "Not Implemented!",
                                          "Internal error!");
                break;
            }
        }
    }

    
    private void createTable(Composite mainPanel) {
        
        m_testBaseTable = new TestBaseListTable(null, false);
        
        m_testBaseTable.setModelChangedListener(new ISysModelChangedListener() {
            
            @Override
            public void modelChanged() {
                verifyModel();
            }
        });
        
        Control tablePanel =
            m_testBaseTable.createControl(mainPanel,
                                          new CTestDiagrams(),
                                          CTestDiagrams.ETestDiagramsSections.E_SECTION_DIAG_CONFIGS.swigValue(),
                                          ENodeId.DIAGRAMS, // not really needed here
                                          m_parentView,
                                          true); // show tooltips
        
        tablePanel.setLayoutData("wmin 0, span, growx");

        m_testBaseTable.setTooltip("This table contains configuration for diagrams, graphs, and charts." 
                           + "\n\n" +

                           "Shortcuts:\n" +
                           "F2 - edit\n" +
                           "Esc - revert editing\n" +
                           "Del - delete cell contents\n" +
                           "Ctrl + num + - add column or row if column or row is selected\n" +
                           "Ctrl + num - - delete selected column or row\n" +
                           "Backspace - clear cell and start editing\n" +
                           "Ctrl + Space - column selection mode\n" +
                           "Ctrl + Space - row selection mode\n" +
                           "Ctrl + C, Ctrl + X,  Ctrl + V - standard clipboard shortcuts\n\n" +
                           "Static call graph accepts two optional arguments:\n" +
                           "-t : displays callers of a function (calls To the function)\n" +
                           "-d <n> : depth of call graph, default is infinite\n" +
                           "Example: -t -d 3\n\n" +
                           "Flame Graphs\n" +
                           "------------\n" +
                           "IMPORTANT: To understand flame graphs, please do read help section mentioned below!\n\n" +
                           "Flame graphs have interactive features implemented in javascript code, which are not\n" +
                           "implemented in testIDEA SVG viewer. It is recommended to open them in Firefox or\n" +
                           "Chrome web browser.\n\n" +
                           "Flame graph accepts the following optional parameters:\n" +
                           "-d <depth> - defines max number of calls on call stack to be shown\n" +
                           "-n <minTimeMs> - function calls shorter than this are ignored\n" +
                           "-f <regex> - show functions matching regex in green color.\n\n" +
                           "See also Help | Display Help | testIDEA User Guide | Concepts | Test Case Editor | Diagrams.");
        
        m_testBaseTable.getKModel().setHeaderTooltips(new String[][]{
            {"", "If selected, diagram is created, otherwise not.",
                 "Type of the diagram.",
                 "Custom script, which draws a diagram.\nUsed only if diagram type is 'custom' or 'customAsync'.",
                 "Script parameters. Used if script is used to draw a diagram.",
                 "Name of output file, which will contain diagram image.",
                 "If true, diagram image will be added to report. Output file must be kept with report file.",
                 "Viewer to be used when button 'Show' is clicked.\n"
                 + "Multipage viewer has tabs at the bottom to switch between diagrams.",
                 "Data format to be used for diagram image. 'byExtension' selects format according to output file extension.",
                 "Application to be used for diagram display when 'externalApp' is selected in column 'viewer'."}});
        
        // set table ID in caller (perfs page), not here
        Control tableControl = m_testBaseTable.getControl(); 
        tableControl.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                             SWTBotConstants.BOT_DIAGRAMS_KTABLE);
        
        m_testBaseTable.getControl().addCellSelectionListener(new KTableCellSelectionListener() {
            
            int m_prevDiagSelected = -1;
            
            @Override
            public void fixedCellSelected(int col, int row, int statemask) {
                int diagIdx = getDiagramIndex(row);
                showDiagramInBottomPane(diagIdx);
            }
            
            
            @Override
            public void cellSelected(int col, int row, int statemask) {
                int diagIdx = getDiagramIndex(row);
                // if image is redrawn on each click in the table it looks ugly,  
                // but it may also consume double click events (needed for starting  
                // editing of table cells).
                if (diagIdx != m_prevDiagSelected) {
                    showDiagramInBottomPane(diagIdx);
                    m_prevDiagSelected = diagIdx;
                }
                m_createSelectedBtn.setEnabled(diagIdx >= 0  &&  
                        diagIdx < (m_testBaseTable.getKModel().getRowCount() - 1  // -1 for '+' row
                                - m_testBaseTable.getKModel().getFixedHeaderRowCount()));
            }
        });
    }


    @Override
    public void fillControlls() {
        boolean isEnabled = m_testSpec != null;

        setEnabled(isEnabled);

        if (!isEnabled) {
            setInputForInheritCb(null, m_isInheritTB);
            m_testBaseTable.setInput(new CTestDiagrams(), 
                                  CTestDiagrams.ETestDiagramsSections.E_SECTION_DIAG_CONFIGS.swigValue());
        
            return;
        }
        
        m_testBaseTable.setActionExecutioner(m_model);
        
        setInputForInheritCb(SectionIds.E_SECTION_DIAGRAMS, m_isInheritTB);
        
        setCurrentTS(SectionIds.E_SECTION_DIAGRAMS);

        CTestDiagrams diagrams = m_currentTestSpec.getDiagrams(false);
        m_sectionTagEditor.updateValueAndCommentFromTestBase(m_testSpec);
        m_isActiveTCb.setInput(diagrams, m_isInherited);

        m_testBaseTable.setInput(diagrams, 
                                 CTestDiagrams.ETestDiagramsSections.E_SECTION_DIAG_CONFIGS.swigValue());
        
        if (m_isInherited) {
            m_mergedOrCurrentTestSpec = m_testSpec.merge();
        } else {
            m_mergedOrCurrentTestSpec = m_testSpec;
        }
        setEnabled(!m_isInherited);
        
        if (m_testBaseTable.getKModel().getDataRowCount() > 1  ||  m_isInherited) {
            m_tipLbl.setText("");
        } else {
            m_tipLbl.setText(TIP_CLICK_BELOW_TO_ADD_A_DIAGRAM);
        }
        
        Point[] selection = m_testBaseTable.getControl().getCellSelection();
        if (selection.length > 0) {
            int diagIdx = getDiagramIndex(selection[0].y);
            
            // 'if' statement was commented, because then diagrams were not refreshed 
            // after test execution, when they were generated by script in 
            // section Scripts.
            
            // if (m_prevTestSpec != null  &&  m_testSpec.hashCodeAsPtr() != m_prevTestSpec.hashCodeAsPtr()) {
            
              // do not refresh diagram if user only switched between sections,
              // but refresh it when he selects other test case. To preserve
              // line selected in table and transform in diagram, this info should
              // be attached to test case.
                showDiagramInBottomPane(diagIdx);
            // }
        }
        
        verifyModel();
    }
    
    
    private void setEnabled(boolean isEnabled) {
        setEnabledForButtons(isEnabled);
        
        m_autoConfigureProfilerBtn.setEnabled(isEnabled);
        m_testBaseTable.setEnabled(isEnabled);
        m_sectionTagEditor.setEnabled(isEnabled);
    }


    private void setEnabledForButtons(boolean isEnabled) {
        m_createSelectedBtn.setEnabled(isEnabled);
        m_showDiagramBtn.setEnabled(isEnabled);
    }
    
    
    private void showDiagramInBottomPane(int diagIdx) {
        // "D:\\bb\\trunk\\sdk\\mpc5554Sample\\add_int-flowChart.dot.png";
        String imgFileName = null;
        boolean isImageExists = false;
        
        if (m_viewerComposite != null) {
            m_viewerComposite.dispose();
            m_viewerComposite = null;
            m_viewerParentComposite.layout();
        }
        
        try {
            if (diagIdx >= 0) {
                imgFileName = DiagramUtils.getAbsImageFileName(m_mergedOrCurrentTestSpec, 
                                                            diagIdx);
                if (imgFileName != null) {
                    Path path = Paths.get(imgFileName);
                    if (Files.exists(path)) {
                        CTestDiagramConfig diagCfg = DiagramUtils.getDiagConfig(m_mergedOrCurrentTestSpec, 
                                                                                diagIdx);
                        m_viewerComposite = ViewerComposite.create(new File(imgFileName), 
                                                                   diagCfg);
                        
                        m_viewerComposite.createComposite(m_viewerParentComposite);
                        m_viewerComposite.setFile(path.toFile());
                        m_viewerComposite.openFileInCanvas();
                        m_viewerParentComposite.layout();
                        isImageExists = true;
                    } else {
                        StatusView.getView().setDetailPaneText(StatusType.WARNING, 
                                                               "\nDiagram does not exist:\n  "
                                                                       + imgFileName + '\n');
                    }
                } 
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not show diagram - Please check what the script generates.\n"
                                  + imgFileName, ex);
//            StatusModel.instance().appendDetailPaneText("Can not show diagram: "
//                                                        + imgFileName + "\n  "
//                                                        + ex.getMessage() + "\n"
//                                                        + "  Please check what the script really generates.\n", 
//                                                        StatusType.ERROR);
        }
        
        m_showDiagramBtn.setEnabled(isImageExists);
    }


    private void verifyModel() {
        CTestDiagrams diagrams = m_currentTestSpec.getDiagrams(false);

        CTestBaseList diagConfigs = diagrams.getConfigurations(false);
        int numConfigs = (int) diagConfigs.size();

        boolean isModified = false;
        
        // set file name is empty
        for (int idx = 0; idx < numConfigs; idx++) {
            CTestDiagramConfig diagConfig = CTestDiagramConfig.cast(diagConfigs.get(idx));
            if (diagConfig.getOutputFileName().isEmpty()) {
                YamlScalar value = 
                        YamlScalar.newMixed(CTestDiagramConfig.ETestDiagramSections.E_SECTION_DIAG_CFG_OUT_FILE_NAME.swigValue());
                value.setValue(getDefaultImageFileName());
                AbstractAction action = new SetSectionAction(diagConfig, m_nodeId, value);
                m_model.execAction(action);
                isModified = true;
            }
        }
        
        // enable 'Show' button, if at least one of selected rows has existing image,
        // disable it otherwise 
        Point[] selection = m_testBaseTable.getControl().getCellSelection();
        Set<Integer> rows = new TreeSet<>();
        for (Point cell : selection) {
            rows.add(cell.y);
        }
        
        boolean isAtLeastOneImageExists = false;
        for (Integer row : rows) {
            int diagIdx = getDiagramIndex(row);
            String imgFileName = DiagramUtils.getAbsImageFileName(m_mergedOrCurrentTestSpec, 
                                                               diagIdx);
            if (imgFileName != null) {
                Path path = Paths.get(imgFileName);
                if (Files.exists(path)) {
                    isAtLeastOneImageExists = true;
                }
            }
        }
        
        m_showDiagramBtn.setEnabled(isAtLeastOneImageExists);
        
        if (isModified) {
            m_testBaseTable.refresh();
        }
    }


    private String getDefaultImageFileName() {
        return CTestHostVars.getRESERVED_TEST_ID() + "-" + 
                       CTestHostVars.getRESERVED_FUNC_UNDER_TEST() + "-" + 
                       CTestHostVars.getRESERVED_DIAGRAM_TYPE() + ".svg";
    }
    

    // changes missing settings in profiler configuration, so that data for 
    // sequence diagram and call graph is provided by profiler. 
    private void autoConfigureProfiler() {
        
        StringBuilder changes = new StringBuilder();
        GroupAction grpAction = new GroupAction("Auto-Configure profiler for diagrams");
        
        CTestAnalyzer analyzer = m_currentTestSpec.getAnalyzer(false);
        if (analyzer.getRunMode() != CTestAnalyzer.ERunMode.M_START) {
            YamlScalar value = 
                YamlScalar.newMixed(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_RUN_MODE.swigValue());
            value.setValue("start");
            AbstractAction action = new SetSectionAction(analyzer, 
                                                         ENodeId.ANALYZER_NODE, 
                                                         value);
            grpAction.add(action);
            changes.append("- analyzer run mode will be set to 'Start'\n");
        }

        if (analyzer.getDocumentFileName().isEmpty()) {
            YamlScalar value = 
                YamlScalar.newMixed(CTestAnalyzer.EAnalyzerSectionId.E_SECTION_DOC_FILE_NAME.swigValue());
            String docName = m_currentTestSpec.getFunctionUnderTest(true).getName() + "-profiler.trd";
            value.setValue(docName);
            AbstractAction action = new SetSectionAction(analyzer, 
                                                         ENodeId.ANALYZER_NODE, 
                                                         value);
            grpAction.add(action);
            changes.append("- analyzer document name will be set to: " + docName + '\n');
        }

        CTestAnalyzerProfiler profiler = analyzer.getProfiler(false);
        if (profiler.isActive() != ETristate.E_TRUE) {
            YamlScalar value = 
                    YamlScalar.newMixed(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_IS_ACTIVE.swigValue());
            value.setValue("true");
            AbstractAction action = new SetSectionAction(profiler, 
                                                         ENodeId.PROFILER_CODE_AREAS_NODE, 
                                                         value);
            grpAction.add(action);
            changes.append("- profiler will be activated\n");
        }        
        
        
        EProfilerExportFormat exportFmt = profiler.getExportFormat();
        YamlScalar exportFmtScalar = 
                YamlScalar.newMixed(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_EXPORT_FORMAT.swigValue());
        exportFmtScalar.dataFromTestSpec(profiler);
        String exportFmtStr = exportFmtScalar.getValue();
        
        if (exportFmt != EProfilerExportFormat.EProfilerAsXML &&
                exportFmt != EProfilerExportFormat.EProfilerAsXMLBinaryTimeline  ||
                exportFmtStr.isEmpty()) {
            exportFmtScalar.setValue(CProfilerController2.getExportProfilerAsXML());
            AbstractAction action = new SetSectionAction(profiler, 
                                                         ENodeId.PROFILER_CODE_AREAS_NODE, 
                                                         exportFmtScalar);
            grpAction.add(action);
            changes.append("- profiler export format will be set to XML\n");
        }
        
        String exportFile = profiler.getExportFileName();
        if (exportFile.isEmpty()) {
            YamlScalar value = 
                    YamlScalar.newMixed(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_EXPORT_FILE.swigValue());
            String exportFileName = m_currentTestSpec.getFunctionUnderTest(true).getName() + "-profiler.xml";
            value.setValue(exportFileName);
            AbstractAction action = new SetSectionAction(profiler, 
                                                         ENodeId.PROFILER_CODE_AREAS_NODE, 
                                                         value);
            grpAction.add(action);
            changes.append("- profiler export file will be set to '" + exportFileName + "'\n");
        }
        
        boolean isTimeline = profiler.isSaveHistory() == ETristate.E_TRUE;
        if (!isTimeline) {
            YamlScalar value = 
                    YamlScalar.newMixed(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_IS_SAVE_HISTORY.swigValue());
            value.setValue("true");
            AbstractAction action = new SetSectionAction(profiler, 
                                                         ENodeId.PROFILER_CODE_AREAS_NODE, 
                                                         value);
            grpAction.add(action);
            changes.append("- profiler setting 'Save timeline' will be set to 'Yes'\n");
        }
        
        ETristate isMeasureAllFuncs = profiler.isMeasureAllFunctions();
        CTestBaseList codeAreas = profiler.getCodeAreas(false);
        if (isMeasureAllFuncs != ETristate.E_TRUE  &&  codeAreas.size() < 2) {
            YamlScalar value = 
                    YamlScalar.newMixed(CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_IS_MEASURE_ALL_FUNCTIONS.swigValue());
            value.setValue("true");
            AbstractAction action = new SetSectionAction(profiler, 
                                                         ENodeId.PROFILER_CODE_AREAS_NODE, 
                                                         value);
            grpAction.add(action);
            changes.append("- setting 'Measure all functions' in profiler code areas section will be enabled\n");
        }
        
        if (grpAction.isEmpty()) {
            MessageDialog.openInformation(Activator.getShell(), 
                                          "Auto-configure profiler",
                                          "No changes needed - profiler is already configured to record data for sequence diagram and call graph.");
        } else {
            if (MessageDialog.openConfirm(Activator.getShell(), 
                                          "Auto-configure profiler", 
                                          "The following changes will be performed:\n" +
                                                  changes +
                    "\nDo you wish to continue?")) {
                m_model.execAction(grpAction);
            }
        }
    }

    
    private int getDiagramIndex(int rowIndex) {
        return rowIndex - m_testBaseTable.getKModel().getFixedHeaderRowCount();
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestSpecification.SectionIds.E_SECTION_DIAGRAMS.swigValue()};
    }
}
