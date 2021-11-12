package si.isystem.itest.ui.spec;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.widgets.Composite;

import si.isystem.connect.CTestBase;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.common.ktableutils.KTableEditorModel;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.ui.comp.TBControlFor_K_Table;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.SectionEditorAdapter.InheritedActionProvider;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.KTableModelForMapping;
import si.isystem.ui.utils.AsystContentProposalProvider;

/**
 * This is base class for panels containing KTable for editing of CYAMLMap.
 * 
 * @author markok
 *
 */
public class MappingTableEditor {

    private static final int KEY_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;
    
    protected KTableModelForMapping m_tableModel;
    protected TBControlTristateCheckBox m_isInheritExprTB;
    protected TBControlFor_K_Table m_table;

    public MappingTableEditor(Composite parent,
                              String migLayoutData, 
                              String tableId, 
                              ENodeId nodeId,
                              int sectionId,
                              TBControlTristateCheckBox isInheritTsBtn, 
                              InheritedActionProvider inheritedActionProvider,
                              String[] columnHeaders) {
        
        m_tableModel = new KTableModelForMapping(sectionId, nodeId, columnHeaders);
        
        m_table = new TBControlFor_K_Table(parent,
                                           tableId,
                                           sectionId,
                                           migLayoutData,
                                           nodeId, 
                                           m_tableModel);

        m_tableModel.addDefaultKeyListener(m_table.getKtable());
        
        m_isInheritExprTB = isInheritTsBtn;
        if (m_isInheritExprTB != null) {
            m_isInheritExprTB.setActionProvider(inheritedActionProvider);
        }
    }


    public void addModelListener(IKTableModelChangedListener listener) {
        m_tableModel.addModelChangedListener(listener);
    }
    
    
    public void setInput(CTestBase testBase, boolean isMerged) {
        m_tableModel.setData(testBase);
        m_table.setInput(testBase, isMerged);
    }    

    
    public void setInput(CTestBase testBase, boolean isMerged, IActionExecutioner actionExecutioner) {
        m_tableModel.setData(testBase);
        m_table.setInput(testBase, isMerged, actionExecutioner);
    }    

    
    public void setEnabled(boolean isEnabled) {
        m_table.setEnabled(isEnabled);
    }
    
    
    public TBControlTristateCheckBox getInheritExprTB() {
        return m_isInheritExprTB;
    }


    public void setTooltip(String tooltip) {
        m_tableModel.setMainTooltip(tooltip + "\n\n" + KTableEditorModel.KTABLE_SHORTCUTS_TOOLTIP);
        
    }
    
    
    public void setContentProposals(AsystContentProposalProvider keyProposalProvider,
                                    AsystContentProposalProvider valueProposalProvider) {
        
        m_tableModel.setAutoCompleteProposals(KEY_COLUMN,
                                              keyProposalProvider,
                                              ContentProposalAdapter.PROPOSAL_INSERT);
        m_tableModel.setAutoCompleteProposals(VALUE_COLUMN,
                                              valueProposalProvider,
                                              ContentProposalAdapter.PROPOSAL_INSERT);
    }
    

    public void setSelection(int lineNo) {
        m_table.setSelection(lineNo);        
    }
}
