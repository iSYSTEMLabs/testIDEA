package si.isystem.itest.ui.comp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestMinMax.EMinMaxSectionIds;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.StatusDecoration;
import si.isystem.itest.ui.spec.data.StatusDecoration.EStatusType;
import si.isystem.ui.utils.KGUIBuilder;

public class HitLimits {

    private TBControlText m_minHits;
    private Text m_resultHits;
    private StatusDecoration m_statusDecoration;
    private TBControlText m_maxHits;

    public void createHitLimitsControl(KGUIBuilder builder, ENodeId nodeId) {
        builder.label("Hits:", "gaptop 10");
        m_minHits = TBControlText.createForMixed(builder,
                                                 "Minimum value for hits.",
                                                 "split 5, span, gaptop 10, wmin 50",
                                                 EMinMaxSectionIds.E_SECTION_MIN.swigValue(),
                                                 nodeId,
                                                 null,
                                                 SWT.BORDER);
        builder.label("<=", "gaptop 10");
        m_resultHits = builder.text("wmin 100, gaptop 10, gapleft 10, gapright 10", SWT.BORDER);
        m_resultHits.setEditable(false);
        
        m_statusDecoration = new StatusDecoration(m_resultHits, 
                                                        SWT.LEFT | SWT.BOTTOM);

        
        builder.label("<=", "gaptop 10");
        m_maxHits = TBControlText.createForMixed(builder,
                                                 "Maximum value for hits.",
                                                 "gaptop 10, wmin 50, wrap",
                                                 EMinMaxSectionIds.E_SECTION_MAX.swigValue(),
                                                 nodeId,
                                                 null,
                                                 SWT.BORDER);
    }
    
    
    public void setInput(CTestBase input, boolean isMerged, String hits, 
                         String statusDesc, EStatusType statusType) {
        m_minHits.setInput(input, isMerged);
        m_maxHits.setInput(input, isMerged);
        m_resultHits.setText(hits);
        m_statusDecoration.setDescriptionText(statusDesc, statusType);
    }
    
    
    public void clearInput() {
        m_minHits.setInput(null, false);
        m_maxHits.setInput(null, false);
        m_resultHits.setText("");
        m_statusDecoration.setDescriptionText("", EStatusType.INFO);
    }
    
    
    public void setEnabled(boolean isEnabled) {
        m_minHits.setEnabled(isEnabled);
        m_maxHits.setEnabled(isEnabled);
    }
}
