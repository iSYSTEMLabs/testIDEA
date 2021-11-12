package si.isystem.itest.ui.comp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;

import si.isystem.connect.CTestBase;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.EBool;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.BoolActionProvider;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class combines a concrete control (a tristate checkbox in this case) and 
 * a general ValueAndCommentEditor. It connects the control and test specification 
 * with ValueAndCommentEditor.  
 * 
 * @author markok
 *
 */
public class TBControlTristateCheckBox extends TBControl {

    private BoolActionProvider m_actionProvider;
    Button m_checkBoxBtn;
    private String m_off;
    private String m_grayed;
    private String m_on;
    
    
    public TBControlTristateCheckBox(KGUIBuilder builder, 
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
    
    
    public void setEditable(boolean isEditable) {
        m_checkBoxBtn.setEnabled(isEditable);
    }

    public boolean isSelected() {
        return m_checkBoxBtn.getSelection();
    }
    
    
    /**
     * If these strings are set, then label is adapted on each state change.
     * @param off when checkbox is not selected
     * @param grayed when checkbox is grayed
     * @param on when checkbox is selected
     */
    public void setStateLabels(String off, String grayed, String on) {
        m_off = off;
        m_grayed = grayed; 
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

    
    public void setActionProvider(BoolActionProvider actionProvider) {
        m_actionProvider = actionProvider;
    }
    
    
/*    @Override
    protected boolean isControlEmpty() {
        return m_checkBoxBtn.getGrayed();
    } */


    protected void value2ui() {
        
        String value = m_tagEditor.getValue();
        ETristate tristateValue = EBool.strToTristate(value);
        
        switch (tristateValue) {
        case E_DEFAULT:
            m_checkBoxBtn.setGrayed(true);
            m_checkBoxBtn.setSelection(true);
            break;
        case E_FALSE:
            m_checkBoxBtn.setGrayed(false);
            m_checkBoxBtn.setSelection(false);
            break;
        case E_TRUE:
            m_checkBoxBtn.setGrayed(false);
            m_checkBoxBtn.setSelection(true);
            break;
        default:
            break;
        }

        // set label, if configured
        if (m_checkBoxBtn.getGrayed()) {
            if (m_grayed != null) {
                m_checkBoxBtn.setText(m_grayed);
            }
        } else {
            if (m_off != null  &&  m_on != null) {
                m_checkBoxBtn.setText(m_checkBoxBtn.getSelection() ? m_on : m_off);
            }
        }
    }


    protected void ui2value() {
        String value;
        if (m_checkBoxBtn.getGrayed()) {
            value = EBool.tristate2Str(ETristate.E_DEFAULT);
        } else {
            value = EBool.tristate2Str(m_checkBoxBtn.getSelection() ? 
                                       ETristate.E_TRUE : 
                                       ETristate.E_FALSE); 
        }
        
        m_tagEditor.setValue(value);
    }
    
    
    class DefaultSelectionListener implements SelectionListener, ICommentChangedListener {
        @Override
        public void widgetSelected(SelectionEvent e) {
            
            // Actions on section:
            // cleared --> iterm. --> checked --> cleared
            //      none        clear        copy from base (it is usually more convenient
            //                                               to have base stubs as a 
            //                                               starting point than empty
            //                                               section. Furthermore, it is 
            //                                               easier to clear section
            //                                               with context menu than 
            //                                               copy/paste from base.
            //                                               See support 9148.)
            // SHIFT:
            // cleared --> checked --> iterm. --> cleared
            //      clear        none       copy from base
            GroupAction grpAction = new GroupAction("3-state check Box");
            AbstractAction action = null;
            
            if (e.stateMask == SWT.SHIFT) {
                // reverse state toggling for tristate checkbox - interm., not selected, selected
                if (!m_checkBoxBtn.getSelection()) {
                    if (m_checkBoxBtn.getGrayed()) {
                        // iterm. --> cleared
                        m_checkBoxBtn.setGrayed(false);
                        if (m_actionProvider != null) {
                            action = m_actionProvider.getClearAction();
                        }
                    } else {
                        // checked --> iterm
                        m_checkBoxBtn.setGrayed(true);
                        m_checkBoxBtn.setSelection(true);
                    }
                } else {
                    // cleared --> checked
                    if (m_actionProvider != null) {
                        action = m_actionProvider.getCopyAction();
                    }
                }
            } else {
                // state toggling for tristate checkbox - cleared, interm., checked
                if (m_checkBoxBtn.getSelection()) {
                    if (!m_checkBoxBtn.getGrayed()) {
                        // cleared --> iterm
                        m_checkBoxBtn.setGrayed(true);
                    }
                } else {
                    if (m_checkBoxBtn.getGrayed()) {
                        // iterm --> checked
                        m_checkBoxBtn.setGrayed(false);
                        m_checkBoxBtn.setSelection(true);
                        // clear section
                        if (m_actionProvider != null) {
                            action = m_actionProvider.getClearAction();
                        }
                    } else {
                        // checked --> cleared
                        if (m_actionProvider != null) {
                            // copy base section to derived test spec
                            action = m_actionProvider.getCopyAction();
                        }
                    }
                } 
            }
            
            sendAction(grpAction, action);
        }
        
        
        @Override
        public void commentChanged(YamlScalar scalar) {
            GroupAction grpAction = new GroupAction("Set check box comment");
            sendAction(grpAction, null);
        }

        
        private void sendAction(GroupAction grpAction, AbstractAction action) {
            ui2value();
            
            // perform additional actions if instructed from action provider, 
            // for example if this cb defines inheritance, copy items from
            // base test spec.
            if (action != null) {
                grpAction.add(action);
            }
            // set value of the checkbox in test spec.
            SetSectionAction setAction = new SetSectionAction(m_testBase, 
                                                              m_nodeId, 
                                                              m_tagEditor.getScalarCopy());
            grpAction.add(setAction);
            sendActionAndVerify(grpAction, true);
        }
        
        
        @Override public void widgetDefaultSelected(SelectionEvent e) {}

    };
}


