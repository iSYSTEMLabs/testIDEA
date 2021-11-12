package si.isystem.itest.ui.spec.group;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.ETristate;
import si.isystem.itest.handlers.TestShowSourceCode;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.AnalyzerCoverageEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.SelectionAdapter;

public class GroupCoverageConfigEditor extends GroupSectionEditor {

    private AnalyzerCoverageEditor m_cvrgExportEditor;

    public GroupCoverageConfigEditor() {
        super(ENodeId.GRP_CVRG_CONFIG, 
              ESectionCTestGroup.E_SECTION_COVERAGE_EXPORT,
              ESectionCTestGroup.E_SECTION_MERGED_ANALYZER_FILE,
              ESectionCTestGroup.E_SECTION_CLOSE_ANALYZER_FILE);
        
        m_cvrgExportEditor = new AnalyzerCoverageEditor(ENodeId.GRP_CVRG_CONFIG);
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {
        Composite panel = m_cvrgExportEditor.createPartControl(parent);
        m_cvrgExportEditor.addShowAnalFileBtnListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                TestShowSourceCode cmdHandler = new TestShowSourceCode();
                CTestBench testBench = new CTestBench();
                testBench.getGroup(false).addChildAndSetParent(-1, 
                                                               m_testGroup);
                IIConnectOperation showSourceOperation = 
                                         cmdHandler.showAnalyzerFile(testBench);
                
                ISysUIUtils.execWinIDEAOperation(showSourceOperation, 
                                                 Activator.getShell(),
                                                 ConnectionProvider.instance().getDefaultConnection());
            }
        });
        
        return panel;
    }

    
    @Override
    public boolean isEmpty() {
        return m_testGroup.getCoverageExportConfig(true).isEmpty()  &&  
                m_testGroup.getMergedAnalyzerFileName().isEmpty();
    }
    
    
    @Override
    public boolean isActive() {
        return m_testGroup.getCoverageExportConfig(true).isActive() == ETristate.E_TRUE;
    }
    
    
    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testGroup != null;
        
        m_cvrgExportEditor.setControlsEnabled(isEnabled);
        
        if (!isEnabled) {
            return;
        }

        CTestAnalyzerCoverage cvrg = m_testGroup.getCoverageExportConfig(false);
        m_cvrgExportEditor.setInputForExportControls(m_testGroup, cvrg, false);

        m_cvrgExportEditor.setInputForGroupSpecificControls(m_testGroup);
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestGroup.ESectionCTestGroup.E_SECTION_COVERAGE_EXPORT.swigValue()};
    }
}
