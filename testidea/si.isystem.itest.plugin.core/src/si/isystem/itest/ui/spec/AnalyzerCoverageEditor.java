package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CAnalyzerDocController;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerCoverage.ECoverageSectionId;
import si.isystem.connect.CTestAnalyzerCoverage.EMergeScope;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.ETristate;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.EBool;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.FilterDialog;
import si.isystem.itest.handlers.TestShowSourceCode;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.comp.IAsistListener;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class AnalyzerCoverageEditor extends SectionEditorAdapter {

    private CommonAnalyzerControls m_commonControls;
    
    private TBControlRadio m_isProvideAsmInfoBtnsHC;

    private TBControlText m_fmtVariantHC; 
    private TBControlRadio m_isExportModuleLinesBtnsHC;
    private TBControlRadio m_isExportFunctionLinesBtnsHC;
    private TBControlRadio m_isExportSourcesBtnsHC;
    private TBControlRadio m_isExportAsmBtnsHC;
    private TBControlRadio m_isExportRangesBtnsHC;
    private TBControlRadio m_isLaunchViewerBtnsHC;
    private TBControlText m_modulesFilterHC;
    private TBControlText m_functionsFilterHC;

    private TBControlRadio m_mergeScopeBtnsHC;

    private Text m_mergeFilterTxt;

    private TBControlRadio m_isIgnoreUnreachableCodeHC;

    private boolean m_isTestSpecPage = true; // if false, it is CTestGroup page

    // used for tag editor - comments of the parent section
    private int m_parentSectionId;

    private TBControlRadio m_closeBtnsHC;

    private TBControlText m_mergedAnalyzerFileTB;

    private Button m_showAnalFileBtn;

    
    public AnalyzerCoverageEditor() {
        super(ENodeId.ANAL_COVERAGE_NODE, SectionIds.E_SECTION_ANALYZER);

        m_parentSectionId = CTestAnalyzer.EAnalyzerSectionId.E_SECTION_TRACE.swigValue();
        m_commonControls = new CommonAnalyzerControls();
        NODE_PATH = SectionNames.COVERAGE_NODE_PATH;
    }

    
    public AnalyzerCoverageEditor(ENodeId nodeId) {
        super(nodeId);

        m_parentSectionId = CTestGroup.ESectionCTestGroup.E_SECTION_COVERAGE_EXPORT.swigValue();
        
        m_commonControls = new CommonAnalyzerControls();
        m_isTestSpecPage = false;
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);

        String rowConstraints = "[min!][min!][min!][min!][fill]";
        if (m_isTestSpecPage) {
            rowConstraints = "[min!][min!][fill]";
        }
        
        mainPanel.setLayout(new MigLayout("fillx", 
                                          "[min!][min!][min!][min!][grow][min!]",
                                          rowConstraints));
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);

        m_commonControls.createIsActiveButtons(builder, m_nodeId, 
                                               ECoverageSectionId.E_SECTION_IS_ACTIVE.swigValue(),
                                               m_parentSectionId);
        
        String [] hostVarsForExportFileName;
        if (!m_isTestSpecPage) {
            createGroupMergedAnalyzerFileNameCtrls(builder);
            m_commonControls.addActiveButtonsListener(createAnalyzerFileNameListener());
            createCloseAfterTestBtns(builder);
            hostVarsForExportFileName = HostVarsUtils.getHostVarsForGroupAnalyzerFileName();
        } else {
            hostVarsForExportFileName = HostVarsUtils.getHostVarsForAnalyzerFileName();
        }
        
        KGUIBuilder exportBuilder = m_commonControls.createControls(builder, m_nodeId, 
                                        new String[]{CAnalyzerDocController.getExportCCAsHTML(),
                                                     CAnalyzerDocController.getExportCCAsText(), 
                                                     CAnalyzerDocController.getExportCCAsCSV(), 
                                                     CAnalyzerDocController.getExportCCAsXML()},
                                        ECoverageSectionId.E_SECTION_EXPORT_FORMAT.swigValue(),
                                        ECoverageSectionId.E_SECTION_EXPORT_FILE.swigValue(),
                                        "Select the format for exported data.",
                                        new String[]{"html", "txt", "csv", "xml"},
                                        hostVarsForExportFileName);

        exportBuilder.label("Variant:");
        m_fmtVariantHC = TBControlText.createForMixed(exportBuilder, 
                                           "Variant for export format. If empty, 'default' is used. "
                                           + "See winIDEA coverage export dialog, field 'Variant' for possible values.", 
                                           "width 40%, span, wrap", 
                                           ECoverageSectionId.E_SECTION_EXFMT_VARIANT.swigValue(), 
                                           m_nodeId, 
                                           EHControlId.ECoverageFmtVariant, 
                                           SWT.BORDER);

        exportBuilder.label("Ignore unreachable code:");

        m_isIgnoreUnreachableCodeHC = new TBControlRadio(exportBuilder, 
                                                         new String[]{"No", "Yes", "Default (No)"}, 
                                                         new String[]{"The code inside the function which is not reachable by sequential\n"
                                                                 + "or (conditional) direct branch flow is NOT ignored for coverage statistics.",
                                                                 "The code inside the function which is not reachable by sequential\n"
                                                                 + "or (conditional) direct branch flow is ignored for coverage statistics.\n\n"
                                                                 + "Check this item if compiler puts data into object code (for example branch addresses)\n"
                                                                 + "and you want to get 100% coverage.",
                                                                 "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                         "wrap", 
                                                         ECoverageSectionId.E_SECTION_IS_IGNORE_UNREACHABLE_CODE.swigValue(), 
                                                         m_nodeId, 
                                                         null);
        
        exportBuilder.label("Assembler info:");
        
        m_isProvideAsmInfoBtnsHC = new TBControlRadio(exportBuilder, 
                                                      new String[]{"No", "Yes", "Default (No)"}, 
                                                      new String[]{"Information about assembly level coverage will NOT be provided.",
                                                                   "Information about assembly level coverage will be provided.",
                                                                   "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                      "wrap", 
                                                      ECoverageSectionId.E_SECTION_IS_ASSEMBLER_INFO.swigValue(), 
                                                      m_nodeId, 
                                                      null);
        
        exportBuilder.label("Module lines:");
        
        m_isExportModuleLinesBtnsHC = new TBControlRadio(exportBuilder, 
                                                         new String[]{"No", "Yes", "Default (No)"}, 
                                                         new String[]{"Lines coverage for modules will NOT be exported.",
                                                                      "Lines coverage for modules will be exported.",
                                                                      "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                         "gapright 20", 
                                                         ECoverageSectionId.E_SECTION_IS_EXPORT_MODULE_LINES.swigValue(), 
                                                         m_nodeId, 
                                                         null);
        
        exportBuilder.label("Function lines:");
        
        m_isExportFunctionLinesBtnsHC = new TBControlRadio(exportBuilder, 
                                                           new String[]{"No", "Yes", "Default (No)"}, 
                                                           new String[]{"Lines coverage for functions will NOT be exported.",
                                                                        "Lines coverage for functions will be exported.",
                                                                        "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                           "wrap", 
                                                           ECoverageSectionId.E_SECTION_IS_EXPORT_FUNCTION_LINES.swigValue(), 
                                                           m_nodeId, 
                                                           null);
        
        
        exportBuilder.label("Sources:");
        
        m_isExportSourcesBtnsHC = new TBControlRadio(exportBuilder, 
                                                     new String[]{"No", "Yes", "Default (No)"}, 
                                                     new String[]{"Source line coverage will NOT be exported.",
                                                                  "Source line coverage will be exported. It contains the same information with markers as shown in winDIDEA source file.",
                                                                  "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                     "gapright 20", 
                                                     ECoverageSectionId.E_SECTION_IS_EXPORT_SOURCES.swigValue(), 
                                                     m_nodeId, 
                                                     null);
                                                       
        
        exportBuilder.label("Asm:");

        m_isExportAsmBtnsHC = new TBControlRadio(exportBuilder, 
                                                 new String[]{"No", "Yes", "Default (No)"}, 
                                                 new String[]{"Coverage of assembler level instructions will NOT be exported.",
                                                              "Coverage of assembler level instructions will be exported.",
                                                              "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                 "wrap", 
                                                 ECoverageSectionId.E_SECTION_IS_EXPORT_ASM.swigValue(), 
                                                 m_nodeId, 
                                                 null);
                                                                                                
                                                 
        exportBuilder.label("Ranges:");

        m_isExportRangesBtnsHC = new TBControlRadio(exportBuilder, 
                                                    new String[]{"No", "Yes", "Default (No)"}, 
                                                    new String[]{"Coverage of ranges without source info will NOT be exported.",
                                                                 "Coverage of ranges without source info will be exported.",
                                                                 "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                    "gapright 20", 
                                                    ECoverageSectionId.E_SECTION_IS_EXPORT_RANGES.swigValue(), 
                                                    m_nodeId, 
                                                    null);
                                                                                                
                                                 
        exportBuilder.label("Launch viewer:");
        
        m_isLaunchViewerBtnsHC = new TBControlRadio(exportBuilder, 
                                                    new String[]{"No", "Yes", "Default (No)"}, 
                                                    new String[]{"Default system viewer for exported files will NOT be launched.",
                                                                 "Default system viewer for exported files will be launched.",
                                                                 "Default setting (no). This setting is stored as unspecified item in YAML output."}, 
                                                    "wrap", 
                                                    ECoverageSectionId.E_SECTION_IS_LAUNCH_VIEWER.swigValue(), 
                                                    m_nodeId, 
                                                    null);
        
        
        exportBuilder.label("Modules filter:");

        m_modulesFilterHC = TBControlText.createForMixed(exportBuilder, 
                                                          "Filter for modules to be exported. See winIDEA coverage export dialog for more info.", 
                                                          "gapright 20, growx", 
                                                          ECoverageSectionId.E_SECTION_EXPORT_MODULES_FILTER.swigValue(), 
                                                          m_nodeId, 
                                                          EHControlId.ECoverageModulesFilter, 
                                                          SWT.BORDER);
        
        
        exportBuilder.label("Functions filter:");
        
        m_functionsFilterHC = TBControlText.createForMixed(exportBuilder, 
                                                          "Filter for functions to be exported. See winIDEA coverage export dialog for more info.", 
                                                          "growx, wrap", 
                                                          ECoverageSectionId.E_SECTION_EXPORT_FUNCTIONS_FILTER.swigValue(), 
                                                          m_nodeId, 
                                                          EHControlId.ECoverageFunctionsFilter, 
                                                          SWT.BORDER);

        if (m_isTestSpecPage) {
            createCoverageMergeControls(builder);
        }

        return getScrollableParent(mainPanel);
    }


    private void createCloseAfterTestBtns(KGUIBuilder builder) {
        builder.label("Close after test:");

        m_closeBtnsHC = new TBControlRadio(builder, 
                         new String[]{"No", "Yes", "Default (No)"}, 
                         new String[]{"Analyzer file will NOT be closed after test.",
                                      "Analyzer file will be closed after test.",
                                      "Default setting (no). This setting is stored as unspecified item in YAML output."},
                         "wrap", 
                         CTestGroup.ESectionCTestGroup.E_SECTION_CLOSE_ANALYZER_FILE.swigValue(), 
                         m_nodeId, 
                         null);
    }


    private void createCoverageMergeControls(KGUIBuilder builder) {
        KGUIBuilder mergeBuilder = builder.group("Merge configuration", 
                                                 "wmin 0, split, span, grow, gaptop 15", 
                                                 true, "fill", "[min!][min!][min!][min!][fill][min!]", 
                                                 "[min!][min!][fill]");
        
        
        mergeBuilder.label("Merge scope:");
        CTestAnalyzerCoverage cvrg = new CTestAnalyzerCoverage();
        m_mergeScopeBtnsHC = new TBControlRadio(mergeBuilder, 
                                                new String[]{"None", "Siblings only", "Siblings and parent", "All", "Default (None)"}, 
                                                      new String[]{"No merging of coverage info will be performed.",
                                                                   "Only coverage from test cases with the same immediate parent will be merged, and of their derived test cases.",
                                                                   "Only coverage from parent and test cases with the same immediate parent will be merged, and of their derived test cases.",
                                                                   "Coverage info form all test cases executed before this test case in the same run will be merged.",
                                                                   "Default setting (no). This setting is stored as unspecified item in YAML output."},
                                                      new String[]{cvrg.enum2Str(ECoverageSectionId.E_SECTION_MERGE_SCOPE.swigValue(), EMergeScope.ENone.swigValue()),
                                                                   cvrg.enum2Str(ECoverageSectionId.E_SECTION_MERGE_SCOPE.swigValue(), EMergeScope.ESiblingsOnly.swigValue()),
                                                                   cvrg.enum2Str(ECoverageSectionId.E_SECTION_MERGE_SCOPE.swigValue(), EMergeScope.ESiblingsAndParent.swigValue()),
                                                                   cvrg.enum2Str(ECoverageSectionId.E_SECTION_MERGE_SCOPE.swigValue(), EMergeScope.EAll.swigValue()),
                                                                   ""},             
                                                      "growy, wrap", 
                                                      ECoverageSectionId.E_SECTION_MERGE_SCOPE.swigValue(), 
                                                      m_nodeId, 
                                                      null);
        
        mergeBuilder.label("Filter:");
        m_mergeFilterTxt = mergeBuilder.text("hmin 100, spanx 5, spany 2, grow", 
                                             SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        m_mergeFilterTxt.setEditable(false);
        Button editBtn = mergeBuilder.button("Edit");
        editBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                CTestAnalyzerCoverage coverage = m_testSpec.getAnalyzer(true).getCoverage(true);
                CTestFilter oldFilter = coverage.getMergeFilter(true);
                CTestFilter newFilter = new CTestFilter(null);
                newFilter.assign(oldFilter);
                
                FilterDialog dlg = new FilterDialog(Activator.getShell(), 
                                                    CTestBench.getCvrgFilterCandidates(m_testSpec.merge()), 
                                                    newFilter);
                
                if (dlg.show()) {
                    
                    SetTestObjectAction action = new SetTestObjectAction(coverage, 
                                                                         ECoverageSectionId.E_SECTION_MERGE_FILTER.swigValue(), 
                                                                         newFilter, 
                                                                         ENodeId.ANAL_COVERAGE_NODE);
                    action.addDataChangedEvent(ENodeId.ANAL_COVERAGE_NODE, coverage);
                    action.addAllFireEventTypes();
                    TestSpecificationModel.getActiveModel().execAction(action);
                }
            }
        });
    }


    // used on CTestGroup coverage config page
    private void createGroupMergedAnalyzerFileNameCtrls(KGUIBuilder builder) {
        
        builder.label("Merged analyzer file:");
        
        m_mergedAnalyzerFileTB = TBControlText.createForMixed(builder, 
                                                              "Name of analayzer file, which will contain merged coverage of all test cases in a group.", 
                                                              "pushx, growx, span 4", 
                                                              ESectionCTestGroup.E_SECTION_MERGED_ANALYZER_FILE.swigValue(), 
                                                              ENodeId.GRP_CVRG_CONFIG, 
                                                              null, 
                                                              SWT.BORDER); 

        m_mergedAnalyzerFileTB.addAsistListener(new IAsistListener() {
            @Override
            public String onFocusLost(String fName) {
                return UiUtils.addExtension(fName, 
                                            AnalyzerEditor.ANALYZER_DOC_EXTENSION, 
                                            true, 
                                            false);
            }
        });

        Control analyzerFNameTxt = m_mergedAnalyzerFileTB.getControl();

        HostVarsUtils.setContentProposals(analyzerFNameTxt, 
                                          HostVarsUtils.getHostVarsForGroupAnalyzerFileName());
        
        m_showAnalFileBtn = builder.button(IconProvider.INSTANCE.getIcon(EIconId.ELinkToEditor), 
                                           "gapleft 10, wrap");
        UiTools.setToolTip(m_showAnalFileBtn, ISysUIUtils.LINK_TO_EDITOR_BTN_TOOLTIP);
        if (m_isTestSpecPage) {
            addShowAnalFileBtnListener(new SelectionAdapter() {
                
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TestShowSourceCode cmdHandler = new TestShowSourceCode();
                    CTestBench testBench = new CTestBench();
                    testBench.getTestSpecification(false).addChildAndSetParent(-1, 
                                                                               m_currentTestSpec);
                    IIConnectOperation showSourceOperation = 
                                        cmdHandler.showAnalyzerFile(testBench);
   
                    ISysUIUtils.execWinIDEAOperation(showSourceOperation, 
                                                     Activator.getShell(),
                                                     ConnectionProvider.instance().getDefaultConnection());
                }
            });
        }
    }

    
    public void addShowAnalFileBtnListener(SelectionListener listener) {
        m_showAnalFileBtn.addSelectionListener(listener);
    }
    
    
    private SelectionListener createAnalyzerFileNameListener() {
        return new SelectionAdapter() {
            /**
             * Sets analyzer doc file name as configured in properties, if it is empty.
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button src = (Button)e.getSource();
                
                if (src.getSelection()) {
                    String value = (String)(src.getData(TBControlRadio.ENUM_TXT));
                
                    //CTestAnalyzerCoverage coverage = new CTestAnalyzerCoverage();
                    final String isActiveYesTxt = //coverage.enum2Str(CTestAnalyzerCoverage.ECoverageSectionId.E_SECTION_IS_ACTIVE.swigValue(), 
                                                  //                  ETristate.E_TRUE.swigValue());
                                                EBool.tristate2Str(ETristate.E_TRUE);
                    
                    if (value.equals(isActiveYesTxt)) {
                        // System.out.println("src = " + value +" / " + src.toString() + " / " + src.getSelection());
                        Text txt = ((Text)m_mergedAnalyzerFileTB.getControl());
                        if (txt.getText().trim().isEmpty()) {
                            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                            if (model != null) {
                                String analyzerDocFName = model.getCEnvironmentConfiguration().
                                        getToolsConfig(true).getAnalyzerFName();
                                // groups don't have ${_testId}, while test specs don't have ${_groupId}
                                if (m_isTestSpecPage) {
                                    analyzerDocFName = 
                                        analyzerDocFName.replace(CTestHostVars.getRESERVED_GROUP_ID(),
                                                                 CTestHostVars.getRESERVED_TEST_ID());
                                } else {
                                    analyzerDocFName = 
                                        analyzerDocFName.replace(CTestHostVars.getRESERVED_TEST_ID(),
                                                                 CTestHostVars.getRESERVED_GROUP_ID());
                                }
                                txt.setText(analyzerDocFName);
                                txt.setFocus(); // set focus because value is saved only on focus lost.
                            }
                        }
                    }
                }
            }
        };            
    }
    
    
    @Override
    public void fillControlls() {
        boolean isEnabled = m_testSpec != null;

        setControlsEnabled(isEnabled);
        
        if (!isEnabled) {
            return;
        }

        setCurrentTS(m_testSpecSectionIds[0]);
        CTestAnalyzer analyzer = m_currentTestSpec.getAnalyzer(false);
        CTestAnalyzerCoverage cvrg = analyzer.getCoverage(false);

        setInputForExportControls(analyzer, cvrg, m_isInherited);

        if (m_isTestSpecPage) {
            m_mergeScopeBtnsHC.setInput(cvrg, m_isInherited);
            
            CTestFilter filter = cvrg.getMergeFilter(true);
            StringBuilder sb = new StringBuilder();
            StrVector list = new StrVector();
            filter.getIncludedFunctions(list);
            appendFilterSection(sb, "Unconditionally included functions: ", list);

            filter.getExcludedFunctions(list);
            appendFilterSection(sb, "\nExcluded functions: ", list);

            filter.getIncludedIds(list);
            appendFilterSection(sb, "\nUnconditionally included test IDs: ", list);

            filter.getExcludedIds(list);
            appendFilterSection(sb, "\nExcluded test IDs: ", list);

            if (sb.length() > 0) {
                sb.append('\n');
            }

            filter.getMustHaveAllTags(list);
            appendFilterSection(sb, "\nMust have all tags: ", list);

            filter.getMustHaveOneOfTags(list);
            getLogicalOperator(filter.isOrTags1(), list, sb);
            appendFilterSection(sb, "Must have one of tags: ", list);

            filter.getMustNotHaveAllTags(list);
            getLogicalOperator(filter.isOrTags2(), list, sb);
            appendFilterSection(sb, "Must NOT have tags: ", list);

            filter.getMustNotHaveOneOfTags(list);
            getLogicalOperator(filter.isOrTags3(), list, sb);
            appendFilterSection(sb, "Must NOT have one of tags: ", list);

            m_mergeFilterTxt.setText(sb.toString());
        }
    }


    public void setInputForGroupSpecificControls(CTestGroup testGroup) {
        m_mergedAnalyzerFileTB.setInput(testGroup, false);
        m_closeBtnsHC.setInput(testGroup, false);
    }
    

    public void setInputForExportControls(CTestBase analyzer,
                                   CTestAnalyzerCoverage cvrg,
                                   boolean isInherited) {
        
        m_commonControls.setInput(analyzer, cvrg, isInherited);
        m_isIgnoreUnreachableCodeHC.setInput(cvrg, isInherited);
        
        m_isProvideAsmInfoBtnsHC.setInput(cvrg, isInherited);
        m_fmtVariantHC.setInput(cvrg, isInherited);
        m_isExportModuleLinesBtnsHC.setInput(cvrg, isInherited);
        m_isExportFunctionLinesBtnsHC.setInput(cvrg, isInherited);
        m_isExportSourcesBtnsHC.setInput(cvrg, isInherited);
        m_isExportAsmBtnsHC.setInput(cvrg, isInherited);
        m_isExportRangesBtnsHC.setInput(cvrg, isInherited);
        m_isLaunchViewerBtnsHC.setInput(cvrg, isInherited);
        m_modulesFilterHC.setInput(cvrg, isInherited);
        m_functionsFilterHC.setInput(cvrg, isInherited);
    }


    public void setControlsEnabled(boolean isEnabled) {
        m_commonControls.setEnabled(isEnabled);
        
        m_isIgnoreUnreachableCodeHC.setEnabled(isEnabled);
        m_isProvideAsmInfoBtnsHC.setEnabled(isEnabled);
        m_fmtVariantHC.setEnabled(isEnabled);
        m_isExportModuleLinesBtnsHC.setEnabled(isEnabled);
        m_isExportFunctionLinesBtnsHC.setEnabled(isEnabled);
        m_isExportSourcesBtnsHC.setEnabled(isEnabled);
        m_isExportAsmBtnsHC.setEnabled(isEnabled);
        m_isExportRangesBtnsHC.setEnabled(isEnabled);
        m_isLaunchViewerBtnsHC.setEnabled(isEnabled);
        m_modulesFilterHC.setEnabled(isEnabled);
        m_functionsFilterHC.setEnabled(isEnabled);
        
        if (m_isTestSpecPage) {
            m_mergeScopeBtnsHC.setEnabled(isEnabled);
            m_mergeFilterTxt.setEnabled(isEnabled);
        } else {
            m_mergedAnalyzerFileTB.setEnabled(isEnabled);
            m_closeBtnsHC.setEnabled(isEnabled);
        }
    }


    private void getLogicalOperator(boolean isOr, StrVector list, StringBuilder sb) {
        if (!list.isEmpty()) {
            if (isOr) {
                sb.append("\nOR "); 
            } else {
                sb.append("\nAND ");
            }
        }
    }


    private void appendFilterSection(StringBuilder sb, String desc, StrVector list) {
        if (!list.isEmpty()) {
            sb.append(desc)
              .append(CYAMLUtil.strVector2Str(list));
        }
    }

    
    @Override
    public boolean isEmpty() {
        if (m_testSpec == null) {
            return true;
        }
        
        return m_testSpec.getAnalyzer(true).getCoverage(true).isEmpty();
    }

    
    @Override
    public boolean isActive() {

        if (m_testSpec == null) {
            return false;
        }
        
        CTestAnalyzer analyzer = getCurrentTS().getAnalyzer(true);
        return analyzer.getRunMode() == CTestAnalyzer.ERunMode.M_START  &&
               analyzer.getCoverage(true).isActive() == ETristate.E_TRUE;
    }
    
    
    @Override
    public boolean hasErrorStatus() {
        return isActive();
    }
    
        
    @Override
    public boolean isError(CTestResult result) {
        return result.isCodeCoverageError();
    }

    
    @Override
    public void copySection(CTestTreeNode destTestSpec) {
        
        CTestObject testObj = m_testSpec.getAnalyzer(true).getCoverage(true);
        if (!testObj.isEmpty()) {
            CTestSpecification.cast(destTestSpec).getAnalyzer(false).getCoverage(false).assign(testObj);
        }
    }
    
    
    @Override
    public void clearSection() {
        SetTestObjectAction action = new SetTestObjectAction(m_testSpec.getAnalyzer(false), 
                                                             CTestAnalyzer.EAnalyzerSectionId.E_SECTION_COVERAGE.swigValue(), 
                                                             null, 
                                                             m_nodeId);
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestAnalyzer.EAnalyzerSectionId.E_SECTION_COVERAGE.swigValue()};
    }
}
