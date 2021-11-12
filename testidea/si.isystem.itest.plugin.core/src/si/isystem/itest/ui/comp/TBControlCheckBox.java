package si.isystem.itest.ui.comp;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;

import si.isystem.connect.CTestBase;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.EBool;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class combines a concrete control (a checkbox in this case) and a general
 * ValueAndCommentEditor. It connects the control and test specification 
 * with ValueAndCommentEditor.  
 */
public class TBControlCheckBox extends TBControl {

    private Button m_checkBoxBtn;
    private String m_off;
    private String m_on;
    private boolean m_defaultValue = false;
    

    /**
     * 
     * @param builder
     * @param label
     * @param tooltip
     * @param migLayoutData
     * @param section
     * @param nodeId
     * @param controlId section for which ModelVerifier can provide UI status.
     *                  May be null.
     */
    public TBControlCheckBox(KGUIBuilder builder, 
                             String label, 
                             String tooltip,
                             String migLayoutData,
                             int section, 
                             ENodeId nodeId,
                             TBControl.EHControlId controlId) {
        
        super(nodeId);
        
        m_checkBoxBtn = builder.checkBox(label, migLayoutData);
        UiTools.setToolTip(m_checkBoxBtn, tooltip);
        ValueAndCommentEditor valCmtEditor = 
                         ValueAndCommentEditor.newMixed(section, m_checkBoxBtn);

        configure(m_checkBoxBtn, valCmtEditor, controlId);
        
        addDefaultSelectionListeners();
    }

    
    @Override
    public void setInput(CTestBase testBase, boolean isMerged) {
        
        super.setInput(testBase, isMerged);
        value2ui();
    }

    
    @Override
    public void setInput(CTestBase testBase,
                         boolean isMerged, 
                         IActionExecutioner actionExecutioner) {
        
        super.setInput(testBase, isMerged, actionExecutioner);
        value2ui();
    }
    
    
    public void setEditable(boolean isEditable) {
        m_checkBoxBtn.setEnabled(isEditable);
    }

    public boolean isSelected() {
        return m_checkBoxBtn.getSelection();
    }
    
    public void setSelection(boolean isSelected) {
        m_checkBoxBtn.setSelection(isSelected);
        sendAction();
    }
    
    /** 
     * If default value (when item is not defined in test spec) is true, then
     * call this method with 'defaultValue == true'. 
     * The defaultValue is set to 'false' by default.
     * 
     * @param defaultValue
     */
    public void setDefaultValue(boolean defaultValue) {
        m_defaultValue = defaultValue;
    }
    
    
    /**
     * If these strings are set, then label is adapted on each state change.
     * @param off when checkbox is not selected
     * @param on when checkbox is selected
     */
    public void setStateLabels(String off, String on) {
        m_off = off;
        m_on = on;
    }
    
    
    private void addDefaultSelectionListeners() {
        
        DefaultSelectionListener defaultListener = new DefaultSelectionListener();
        m_checkBoxBtn.addSelectionListener(defaultListener);
        m_tagEditor.setCommentChangedListener(defaultListener);
    };
    
    
    public void addSelectionListener(SelectionListener selectionLister) {
        m_checkBoxBtn.addSelectionListener(selectionLister);
    }

    
/*    @Override
    protected boolean isControlEmpty() {
        return m_checkBoxBtn.getSelection() == m_defaultValue;
    }
*/


    protected void value2ui() {
        
        String value = m_tagEditor.getValue();
        ETristate tristateValue = EBool.strToTristate(value);
        
        switch (tristateValue) {
        case E_FALSE:
            m_checkBoxBtn.setSelection(false);
            break;
        case E_TRUE:
            m_checkBoxBtn.setSelection(true);
            break;
        case E_DEFAULT:
            m_checkBoxBtn.setSelection(m_defaultValue);
            break;
        default:
            break;
        }

        // set label, if configured
        if (m_off != null  &&  m_on != null) {
            m_checkBoxBtn.setText(m_checkBoxBtn.getSelection() ? m_on : m_off);
        }
    }


    private void sendAction() {
        String value = EBool.tristate2Str(m_checkBoxBtn.getSelection(), m_defaultValue); 
        m_tagEditor.setValue(value);
        
        // set value of the checkbox in test spec.
        SetSectionAction action = new SetSectionAction(m_testBase, 
                                                       m_nodeId, 
                                                       m_tagEditor.getScalarCopy());
        
        sendActionAndVerify(action, true);  // complete section may have to be redrawn 
                                            // if merged status changes
    }
    
    
    class DefaultSelectionListener implements SelectionListener, ICommentChangedListener {
        @Override
        public void widgetSelected(SelectionEvent e) {
            
            sendAction();
        }

        @Override
        public void commentChanged(YamlScalar scalar) {
            widgetSelected(null);
        }
        
        @Override public void widgetDefaultSelected(SelectionEvent e) {}
    };
}


