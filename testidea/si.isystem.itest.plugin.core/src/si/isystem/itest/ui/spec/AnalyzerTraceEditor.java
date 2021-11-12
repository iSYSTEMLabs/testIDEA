package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerTrace;
import si.isystem.connect.CTestAnalyzerTrace.ETraceSectionId;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CTraceController;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.comp.IAsistListener;
import si.isystem.itest.ui.comp.TBControlCombo;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.tbltableeditor.HeaderPath;
import si.isystem.tbltableeditor.SectionNames;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;

public class AnalyzerTraceEditor extends SectionEditorAdapter {

    private CommonAnalyzerControls m_commonControls;


    public AnalyzerTraceEditor() {
        super(ENodeId.ANAL_TRACE_NODE, SectionIds.E_SECTION_ANALYZER);

        NODE_PATH = SectionNames.ANALYZER_NODE_PATH + HeaderPath.SEPARATOR +
                    SectionNames.TRACE_NODE_NAME;
        
        m_commonControls = new CommonAnalyzerControls();
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        
        Composite mainPanel = createScrollable(parent);

        mainPanel.setLayout(new MigLayout("fillx", "[min!][min!][min!][min!][grow]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        m_commonControls.createIsActiveButtons(builder, m_nodeId, 
                                               ETraceSectionId.E_SECTION_IS_ACTIVE.swigValue(),
                                               CTestAnalyzer.EAnalyzerSectionId.E_SECTION_TRACE.swigValue());
        
        m_commonControls.createControls(builder, m_nodeId,
                                        new String[]{CTraceController.getExportTrcAsText(),
                                                     CTraceController.getExportTrcAsCSV(),
                                                     CTraceController.getExportTrcAsXML(),
                                                     CTraceController.getExportTrcAsBinary()},
                                        ETraceSectionId.E_SECTION_EXPORT_FORMAT.swigValue(),
                                        ETraceSectionId.E_SECTION_EXPORT_FILE.swigValue(),
                                        "Select the format for exported data.",
                                        new String[]{"txt", "csv", "xml", "bin"},
                                        HostVarsUtils.getHostVarsForAnalyzerFileName());
        
        return getScrollableParent(mainPanel);
    }

    
    @Override
    public void fillControlls() {
        boolean isEnabled = m_testSpec != null;

        m_commonControls.setEnabled(isEnabled);
        
        if (!isEnabled) {
            return;
        }

        
        setCurrentTS(m_testSpecSectionIds[0]);
        CTestBase analyzer = m_currentTestSpec.getTestBase(m_testSpecSectionIds[0].swigValue(), 
                                                             false);
        
        CTestAnalyzerTrace trace = CTestAnalyzer.cast(analyzer).getTrace(false);
        
        m_commonControls.setInput(analyzer, trace, m_isInherited);
    }
    
    
    @Override
    public boolean isEmpty() {
        if (m_testSpec == null) {
            return true;
        }
        
        return m_testSpec.getAnalyzer(true).getTrace(true).isEmpty();
    }

    
    @Override
    public boolean isActive() {
        
        if (m_testSpec == null) {
            return false;
        }
        
        CTestAnalyzer analyzer = getCurrentTS().getAnalyzer(true);
        return analyzer.getRunMode() == CTestAnalyzer.ERunMode.M_START  &&
               analyzer.getTrace(true).isActive() == ETristate.E_TRUE;
    }
    
    
    @Override
    public void copySection(CTestTreeNode destTestSpec) {
        
        CTestObject testObj = m_testSpec.getAnalyzer(true).getTrace(true);
        if (!testObj.isEmpty()) {
            CTestSpecification.cast(destTestSpec).getAnalyzer(false).getTrace(false).assign(testObj);
        }
    }
    
    
    @Override
    public void clearSection() {
        SetTestObjectAction action = new SetTestObjectAction(m_testSpec.getAnalyzer(false), 
                                                             CTestAnalyzer.EAnalyzerSectionId.E_SECTION_TRACE.swigValue(), 
                                                             null, 
                                                             m_nodeId);
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }


   @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestAnalyzer.EAnalyzerSectionId.E_SECTION_TRACE.swigValue()};
    }
}


class CommonAnalyzerControls {
    
    private TBControlRadio m_isActiveBtnsTB;
    private ValueAndCommentEditor m_sectionTagEditor;  // comment to main YAML tag for 'trace', 'profiler' or 'coverage' section.
    // private TBControlText m_triggerHC;
    private TBControlCombo m_exportFmtHC;
    private TBControlText m_exportFileHC;
 
    void createIsActiveButtons(KGUIBuilder builder, ENodeId nodeId, 
                               int isActiveId, int parentTagSection) {

        builder.label("Is active:");

        m_isActiveBtnsTB = new TBControlRadio(builder, 
                                              new String[]{"No", "Yes", "Default (No)"}, 
                                              new String[]{"This analyzer analysis will NOT be performed after recording.",
                                                           "This analyzer analysis will be performed after recording.",
                                                           "Default(No). This setting is not saved, which means that default will be used for execution."},
                                              "wrap", 
                                              isActiveId, 
                                              nodeId,
                                              null);

        m_sectionTagEditor = ValueAndCommentEditor.newKey(parentTagSection, 
                                                          m_isActiveBtnsTB.getControl(), 
                                                          SWT.LEFT);
        m_isActiveBtnsTB.setMainTagEditor(m_sectionTagEditor);
    }
    
    
    // can be used to set analyzer file name on selection of 'Yes' button.
    void addActiveButtonsListener(SelectionListener listener) {
        m_isActiveBtnsTB.addSelectionListener(listener);
    }
    
    
    KGUIBuilder createControls(KGUIBuilder builder, ENodeId nodeId, String [] exportFormats,
                               int exportFmtId, 
                               int exportFileId,
                               String formatComboTooltip,
                               final String []extensions,
                               String[] hostVarsForAnalyzerFileName) {
        
        KGUIBuilder exportBuilder = builder.group("Export configuration", 
                                                  "wmin 0, split, span, growx, gaptop 15, wrap", 
                                                  true, "", "[min!][][min!][]", "[min!][min!][min!][min!]");
        
        exportBuilder.label("Export format:");

        m_exportFmtHC = TBControlCombo.createForText(exportBuilder,
                                                     exportFormats,
                                                     formatComboTooltip, 
                                                     "wrap", 
                                                     exportFmtId, 
                                                     nodeId, 
                                                     null,
                                                     SWT.DROP_DOWN);

        m_exportFmtHC.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                String fName = m_exportFileHC.getText();
                int formatIdx = m_exportFmtHC.getSelectionIndex();
                String newFName = UiUtils.replaceExtension(fName, 
                                                           extensions[formatIdx]);
                m_exportFileHC.setTextInControl(newFName);
                m_exportFileHC.sendSetSectionAction();
            }
        });
        
        exportBuilder.label("Export file:");
        
        m_exportFileHC = TBControlText.createForMixed(exportBuilder, 
                                           "File name to export trace data to. If empty, no export is performed.\n"
                                           + "File format is set above.", 
                                           "span 3, growx, wrap", 
                                           exportFileId, 
                                           nodeId, 
                                           null, 
                                           SWT.BORDER);
        
        m_exportFileHC.addAsistListener(new IAsistListener() {
            @Override
            public String onFocusLost(String fName) {
                int selectionIndex = m_exportFmtHC.getSelectionIndex();
                if (selectionIndex >= 0) { // is -1 when combo is empty
                    return UiUtils.addExtension(fName, extensions[selectionIndex], false, false);
                }
                return fName;
            }
        });
        
        Control exportFNameTxt = m_exportFileHC.getControl();

        HostVarsUtils.setContentProposals(exportFNameTxt, 
                                          hostVarsForAnalyzerFileName);
        
        return exportBuilder;
    }


    public void setEnabled(boolean isEnabled) {
        
        m_isActiveBtnsTB.setEnabled(isEnabled);
        // m_triggerHC.setEnabled(isEnabled);
        m_exportFmtHC.setEnabled(isEnabled);
        m_exportFileHC.setEnabled(isEnabled);
            
        m_sectionTagEditor.setEnabled(isEnabled);
    }
    
    
    public void setInput(CTestBase analyzer, CTestBase trcCvrgProf, boolean isInherited) {
 
        m_sectionTagEditor.updateValueAndCommentFromTestBase(analyzer);
        
        m_isActiveBtnsTB.setInput(trcCvrgProf, isInherited);
        // m_triggerHC.setInput(trcCvrgProf, isInherited);
        m_exportFmtHC.setInput(trcCvrgProf, isInherited); 
        m_exportFileHC.setInput(trcCvrgProf, isInherited);
    }
    
    
    public String getExportFileName() {
        return m_exportFileHC.getText();
    }
}