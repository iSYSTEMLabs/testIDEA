package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzer.EAnalyzerSectionId;
import si.isystem.connect.CTestAnalyzer.ERunMode;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.handlers.TestShowSourceCode;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.comp.IAsistListener;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;

public class AnalyzerEditor extends SectionEditorAdapter {

    private ValueAndCommentEditor m_sectionTagEditor;  // comment to main YAML tag for 'trace', 'profiler' or 'coverage' section.
    private TBControlRadio m_runModeBtnsHC;
    private TBControlText m_docFileHC;
    private TBControlRadio m_openModeBtnsHC;
    private TBControlText m_triggerHC;
    private TBControlRadio m_predefTriggerHC;
    
    private TBControlRadio m_slowRunHC;
    private TBControlRadio m_saveBtnsHC;
    private TBControlRadio m_closeBtnsHC;

    public final static String ANALYZER_DOC_EXTENSION = "trd";
    private TBControlTristateCheckBox m_isInheritTB;

    public AnalyzerEditor() {
        super(ENodeId.ANALYZER_NODE, SectionIds.E_SECTION_ANALYZER);
        NODE_PATH = SectionNames.ANALYZER_NODE_PATH;
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        Composite mainPanel = createScrollable(parent);

        mainPanel.setLayout(new MigLayout("fillx", "[min!][min!][min!][min!][grow][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        m_isInheritTB = createTristateInheritanceButton(builder, "gapleft 2, skip, wrap");
        m_isInheritTB.setActionProvider(new InheritedActionProvider(m_testSpecSectionIds[0]));
        
        builder.label("Run mode:");
        CTestAnalyzer analyzer = new CTestAnalyzer(null);
        int runModeSection = EAnalyzerSectionId.E_SECTION_RUN_MODE.swigValue();
        String runModeOff = analyzer.enum2Str(runModeSection, ERunMode.M_OFF.swigValue());
        final String runModeStart = analyzer.enum2Str(runModeSection, ERunMode.M_START.swigValue());
        
        m_runModeBtnsHC = new TBControlRadio(builder, 
                                             new String[]{"Off", "Start", "Default (Off)"},
                                             new String[]{"Analyzer will not start.", 
                                                          "Analyzer will start.", 
                                                          "Default mode (off). This setting is stored as unspecified item in YAML output."},
                                             new String[]{runModeOff, runModeStart, ""},                                               
                                             "wrap", 
                                             runModeSection,
                                             m_nodeId, 
                                             null);
        
        m_runModeBtnsHC.addSelectionListener(new SelectionAdapter() {
            
            /**
             * Sets doc file name as configured in properties, if it is empty.
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button src = (Button)e.getSource();
                
                if (src.getSelection()) {
                    String value = (String)(src.getData(TBControlRadio.ENUM_TXT));
                
                    if (value.equals(runModeStart)) {
                        System.out.println("src = " + value +" / " + src.toString() + " / " + src.getSelection());
                        Text txt = ((Text)m_docFileHC.getControl());
                        if (txt.getText().trim().isEmpty()) {
                            String analyzerDocFName = m_model.getCEnvironmentConfiguration().
                                                      getToolsConfig(true).getAnalyzerFName();
                            txt.setText(analyzerDocFName);
                            txt.setFocus(); // set focus because value is saved only on focus lost.
                        }
                    }
                }
            }
        });            
        
        builder.label("Document file:", "gapright 10");
        
        m_docFileHC = TBControlText.createForMixed(builder, 
                                        "Name of file to store analyzer results. Extension is " +
                                                "automatically added if not specified.\n" +
                                                "Example:\n" +
                                                "    analyzerResult\n" +
                                                "The left comment is attached to the analyzer tag ('trace', 'profiler', or 'coverage') in test specification.\n" +
                                                "It is also exported as 'Description' (Block comment) and 'Comment' (End of line comment) in coverage exports.",
                                                "pushx, growx, span 4", 
                                        EAnalyzerSectionId.E_SECTION_DOC_FILE_NAME.swigValue(), 
                                        m_nodeId, 
                                        null, 
                                        SWT.BORDER);

        m_docFileHC.addAsistListener(new IAsistListener() {
            @Override
            public String onFocusLost(String fName) {
                return UiUtils.addExtension(fName, ANALYZER_DOC_EXTENSION, true, false);
            }
        });

        Control analyzerFNameTxt = m_docFileHC.getControl();

        HostVarsUtils.setContentProposals(analyzerFNameTxt, 
                                          HostVarsUtils.getHostVarsForAnalyzerFileName());
        
        m_sectionTagEditor = ValueAndCommentEditor.newKey(m_testSpecSectionIds[0].swigValue(), 
                                                          m_docFileHC.getControl(), SWT.LEFT);
        m_docFileHC.setMainTagEditor(m_sectionTagEditor);
        
        ISysUIUtils.createShowSourceButton(builder, 
                                           "gapleft 10, wrap", 
                                           new SelectionAdapter() {
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
        
        builder.label("Open mode:");
        
        m_openModeBtnsHC = new TBControlRadio(builder, 
                    new String[]{"Update", "Write", "Append", "Default (Write)"}, 
                    new String[]{"File must exist to be opened. Error is reported otherwise.\n" +
                                     "Contents is cleared on analyzer start.",
                                 "File is created if it does not exist. Contents is always cleared.",
                                 "File is created if it does not exist. Content is NOT cleared on open.\n" +
                                     "Coverage will append data to existing data. Profiler and trace\n" +
                                     "will clear exiting contents on start, but this will change in future versions.",
                                 "Default mode (Write) is used. This setting is not saved, which means that default will be used for execution."},
                   new String[]{"u", "w", "a", ""},
                   "wrap", 
                   EAnalyzerSectionId.E_SECTION_OPEN_MODE.swigValue(), 
                   m_nodeId, 
                   null);
        
        builder.label("Trigger name:", "gaptop 10");

        m_triggerHC = TBControlText.createForMixed(builder, 
                                        "Name of analyzer trigger. Copy paste it " +
                                                "from winIDEA trace document settings.", 
                                        "width 40%, span, gaptop 10, wrap", 
                                        EAnalyzerSectionId.E_SECTION_TRIGGER.swigValue(), 
                                        m_nodeId, 
                                        null, 
                                        SWT.BORDER);
        
        builder.label("Use predef. trig.:", "gapbottom 10");
        
        m_predefTriggerHC = new TBControlRadio(builder, 
                        new String[]{"No", "Yes", "Default (No)"}, 
                        new String[]{"Trigger will be configured by testIDEA according to settings in Coverage and Profiler sections.",
                                     "Trigger should already be defined in trd file. testIDEA will make no changes.\n"
                                     + "Checkbox '" + CoverageStatisticsEditor.MEASURE_ALL_FUNCTIONS + "' in coverage and profiler subsections is disabled!",
                                     "Default setting (no). This setting is stored as unspecified item in YAML output."},
                        "gapbottom 10, wrap", 
                        EAnalyzerSectionId.E_SECTION_IS_PREDEF_TRIGGER.swigValue(), 
                        m_nodeId, 
                        null);
        
        
        builder.label("Use slow run:");
        
        m_slowRunHC = new TBControlRadio(builder, 
                        new String[]{"No", "Yes", "Default (No)"}, 
                        new String[]{"Normal analyzer run will be used. Target must support trace.",
                                     "Slow run analyzer mode will be used. Runs also on targets without trace capability.",
                                     "Default setting (no). This setting is stored as unspecified item in YAML output."},
                        "wrap", 
                        EAnalyzerSectionId.E_SECTION_IS_SLOW_RUN.swigValue(), 
                        m_nodeId, 
                        null);
        
        
        builder.label("Save after test:");
        
        m_saveBtnsHC = new TBControlRadio(builder, 
                         new String[]{"No", "Yes", "Default (No)"}, 
                         new String[]{"Trace file will NOT be saved after test.",
                                      "Trace file will be saved after test.",
                                      "Default setting (no). This setting is stored as unspecified item in YAML output."},
                         "wrap", 
                         EAnalyzerSectionId.E_SECTION_IS_SAVE_AFTER_TEST.swigValue(), 
                         m_nodeId, 
                         null);
        
        builder.label("Close after test:");

        m_closeBtnsHC = new TBControlRadio(builder, 
                         new String[]{"No", "Yes", "Default (No)"}, 
                         new String[]{"Trace file will NOT be closed after test.",
                                      "Trace file will be closed after test.",
                                      "Default setting (no). This setting is stored as unspecified item in YAML output."},
                         "wrap", 
                         EAnalyzerSectionId.E_SECTION_IS_CLOSE_AFTER_TEST.swigValue(), 
                         m_nodeId, 
                         null);
        
        return getScrollableParent(mainPanel);
    }

    
    @Override
    public void fillControlls() {
        boolean isEnabled = m_testSpec != null;

        m_runModeBtnsHC.setEnabled(isEnabled);
        
        m_docFileHC.setEnabled(isEnabled);

        m_openModeBtnsHC.setEnabled(isEnabled);
        m_triggerHC.setEnabled(isEnabled);
        m_predefTriggerHC.setEnabled(isEnabled);
        m_slowRunHC.setEnabled(isEnabled);
        m_saveBtnsHC.setEnabled(isEnabled);
        m_closeBtnsHC.setEnabled(isEnabled);
        
        m_sectionTagEditor.setEnabled(isEnabled);
        
        if (!isEnabled) {
            setInputForInheritCb(null, m_isInheritTB);

            return;
        }

        
        setCurrentTS(m_testSpecSectionIds[0]);
        CTestBase analyzer = m_currentTestSpec.getTestBase(m_testSpecSectionIds[0].swigValue(), 
                                                         false);
        
        setInputForInheritCb(m_testSpecSectionIds[0], m_isInheritTB);
        
        m_sectionTagEditor.updateValueAndCommentFromTestBase(m_currentTestSpec);
        
        m_runModeBtnsHC.setInput(analyzer, m_isInherited);
        m_docFileHC.setInput(analyzer, m_isInherited);
        m_openModeBtnsHC.setInput(analyzer, m_isInherited);
        m_triggerHC.setInput(analyzer, m_isInherited);
        m_predefTriggerHC.setInput(analyzer, m_isInherited);
        m_slowRunHC.setInput(analyzer, m_isInherited);
        m_saveBtnsHC.setInput(analyzer, m_isInherited);
        m_closeBtnsHC.setInput(analyzer, m_isInherited);
    }
    
    
    @Override
    public boolean hasErrorStatus() {
        return true;
    }

    
    @Override
    public boolean isError(CTestResult result) {
        return result.isCodeCoverageError()  ||  result.isProfilerCodeError()  ||
               result.isProfilerDataError();
    }
    
    
    @Override
    public boolean isActive() {
        
        if (m_testSpec == null) {
            return false;
        }
        
        return m_testSpec.getAnalyzer(true).getRunMode() != ERunMode.M_OFF;
    }
    
    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestSpecification.SectionIds.E_SECTION_ANALYZER.swigValue()};
    }
}
