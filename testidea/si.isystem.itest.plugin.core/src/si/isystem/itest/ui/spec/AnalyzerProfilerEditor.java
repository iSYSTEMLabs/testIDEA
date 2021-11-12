package si.isystem.itest.ui.spec;

import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CProfilerController2;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerSectionIds;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.ETristate;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.tbltableeditor.HeaderPath;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.ui.utils.KGUIBuilder;

public class AnalyzerProfilerEditor extends SectionEditorAdapter {

    public final static String PROFILER_NODE_PATH = 
            SectionNames.ANALYZER_NODE_PATH + HeaderPath.SEPARATOR +
            SectionNames.PROFILER_NODE_NAME;

    private CommonAnalyzerControls m_commonControls;
    private TBControlRadio m_exportActOnlyBtnsHC;
    private TBControlRadio m_saveHistoryBtnsHC;
    private TBControlRadio m_profileAUXBtnsHC;

    public AnalyzerProfilerEditor() {
        super(ENodeId.ANAL_PROFILER_NODE, SectionIds.E_SECTION_ANALYZER);
        NODE_PATH = PROFILER_NODE_PATH;
        
        m_commonControls = new CommonAnalyzerControls();
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);

        mainPanel.setLayout(new MigLayout("fillx", "[min!][min!][min!][min!][grow]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);

        m_commonControls.createIsActiveButtons(builder, m_nodeId, 
                                               EProfilerSectionIds.E_SECTION_IS_ACTIVE.swigValue(),
                                               CTestAnalyzer.EAnalyzerSectionId.E_SECTION_TRACE.swigValue());
        
        builder.label("Profile AUX:");
        
        m_profileAUXBtnsHC = new TBControlRadio(builder, 
                                                 new String[]{"No", "Yes", "Default (No)"}, 
                                                 new String[]{"AUX signals will NOT be recorded.",
                                                              "AUX signals will be recorded.",
                                                              "Default(No). This setting is not saved, which " +
                                                              "means that default will be used for execution."},
                                                 "wrap", 
                                                 EProfilerSectionIds.E_SECTION_IS_PROFILE_AUX.swigValue(), 
                                                 m_nodeId,
                                                 null);
        
        KGUIBuilder exportBuilder = 
        m_commonControls.createControls(builder, m_nodeId,
                                        new String[]{CProfilerController2.getExportProfilerAsXML(),
                                                     CProfilerController2.getExportProfilerAsText1(),
                                                     CProfilerController2.getExportProfilerAsBTF()},
                                        EProfilerSectionIds.E_SECTION_EXPORT_FORMAT.swigValue(),
                                        EProfilerSectionIds.E_SECTION_EXPORT_FILE.swigValue(),
                                        "Select the format for exported data.\n"
                                        + "'Binary' format exports statistics as XML, while timeline is exported in binary format.",
                                        new String[]{"txt", "csv", "xml", "bin", "txt", "btf"},
                                        HostVarsUtils.getHostVarsForAnalyzerFileName());
        
        exportBuilder.label("Save timeline:");

        m_saveHistoryBtnsHC = new TBControlRadio(exportBuilder, 
                                                 new String[]{"No", "Yes", "Default (No)"}, 
                                                 new String[]{"Profiler timeline results will NOT be saved after test.",
                                                              "Profiler timeline results will be saved after test.",
                                                              "Default(No). This setting is not saved, which means that default will be used for execution."},
                                                 "wrap", 
                                                 EProfilerSectionIds.E_SECTION_IS_SAVE_HISTORY.swigValue(), 
                                                 m_nodeId,
                                                 null);
        
        exportBuilder.label("Export act. only:");
        
        m_exportActOnlyBtnsHC = new TBControlRadio(exportBuilder, 
                                                   new String[]{"No", "Yes", "Default (No)"}, 
                                                   new String[]{"All configured areas will be exported.",
                                                   "Only areas with recoded activity (functions executed and data modified) will be exported.",
                                                   "Default(No). This setting is not saved, which means that default will be used for execution."},
                                                   "wrap", 
                                                   EProfilerSectionIds.E_SECTION_IS_EXPORT_ACTIVE_AREAS_ONLY.swigValue(), 
                                                   m_nodeId,
                                                   null);

        
        // temporary controls as proof of concept only
//        Button createSeqDiagBtn = builder.button("Sequence diagram", "skip, gaptop 20, wrap");
//        createSeqDiagBtn.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                DiagramUtils.openSeqOrCallDiag(m_commonControls.getExportFileName(),
//                                                 true, //m_isOpenCb.getSelection(),
//                                                 DiagramUtils.EDiagramType.SEQUENCE_DIAG);
//            }
//        });
//        
//        Button callGraphBtn = builder.button("Call graph", "skip, gaptop 10");
//        callGraphBtn.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                DiagramUtils.openSeqOrCallDiag(m_commonControls.getExportFileName(),
//                                                 true, //m_isOpenCb.getSelection(),
//                                                 DiagramUtils.EDiagramType.CALL_GRAPH);
//            }
//        });
        
        
        return getScrollableParent(mainPanel);
    }

    
    @Override
    public void fillControlls() {
        boolean isEnabled = m_testSpec != null;

        m_commonControls.setEnabled(isEnabled);
        m_exportActOnlyBtnsHC.setEnabled(isEnabled);
        m_saveHistoryBtnsHC.setEnabled(isEnabled);
        m_profileAUXBtnsHC.setEnabled(isEnabled);

        if (!isEnabled) {
            return;
        }

        setCurrentTS(m_testSpecSectionIds[0]);
        
        CTestAnalyzer analyzer = m_currentTestSpec.getAnalyzer(false);
        CTestBase profiler = analyzer.getProfiler(false);

        m_commonControls.setInput(analyzer, profiler, m_isInherited);
        
        m_exportActOnlyBtnsHC.setInput(profiler, m_isInherited);
        m_saveHistoryBtnsHC.setInput(profiler, m_isInherited);
        m_profileAUXBtnsHC.setInput(profiler, m_isInherited);
    }
    

    @Override
    public boolean isEmpty() {
        if (m_testSpec == null) {
            return true;
        }
        
        return m_testSpec.getAnalyzer(true).getProfiler(true).isEmpty();
    }

    
    @Override
    public boolean isActive() {

        if (m_testSpec == null) {
            return false;
        }
        
        CTestAnalyzer analyzer = getCurrentTS().getAnalyzer(true);
        return analyzer.getRunMode() == CTestAnalyzer.ERunMode.M_START  &&
               analyzer.getProfiler(true).isActive() == ETristate.E_TRUE;
    }
    
    
    @Override
    public boolean hasErrorStatus() {
        return isActive();
    }
    
    
    @Override
    public boolean isError(CTestResult result) {
        return result.isProfilerCodeError()  ||  result.isProfilerDataError();
    }
    
    
    @Override
    public void copySection(CTestTreeNode destTestSpec) {
        
        CTestObject testObj = m_testSpec.getAnalyzer(true).getProfiler(true);
        if (!testObj.isEmpty()) {
            CTestSpecification.cast(destTestSpec).getAnalyzer(false).getProfiler(false).assign(testObj);
        }
    }
    
    
    @Override
    public void clearSection() {
        SetTestObjectAction action = new SetTestObjectAction(m_testSpec.getAnalyzer(false), 
                                                             CTestAnalyzer.EAnalyzerSectionId.E_SECTION_PROFILER.swigValue(), 
                                                             null, 
                                                             m_nodeId);
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }
    
    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestAnalyzer.EAnalyzerSectionId.E_SECTION_PROFILER.swigValue()};
    }
}
