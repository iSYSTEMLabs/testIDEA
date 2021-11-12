package si.isystem.itest.ui.comp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import si.isystem.connect.CTestBase;
import si.isystem.connect.ETristate;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.EBool;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class TBControlRadio extends TBControl 
                                   implements ICommentChangedListener {

    private Button m_radioButtons[];
    private String[] m_states;
    public static final String ENUM_TXT = "enumText";
    private boolean m_isBoolRadios = false;
    

    /**
     * Ctor. to be used when other controls need to be updated on button click,
     * for example icons and sections tree when test scope is changed.
     */
    public TBControlRadio(KGUIBuilder builder, 
                          String []labels,
                          String []tooltips,
                          String []states,
                          String migLayoutData,
                          int sectionId, 
                          ENodeId nodeId,
                          TBControl.EHControlId controlId,
                          boolean isSendEventOnExec) {
    
        super(nodeId);
        m_states = states;
        m_nodeId = nodeId;
        
        Composite radioGroup = new Composite(builder.getParent(), SWT.NONE);
        m_radioButtons = builder.radio(labels, radioGroup, migLayoutData);
        if (tooltips != null) {
            for (int i = 0; i < tooltips.length; i++) {
                UiTools.setToolTip(m_radioButtons[i], tooltips[i]);
            }
        }
        
        ValueAndCommentEditor tagEditor = 
                ValueAndCommentEditor.newMixed(sectionId, m_radioButtons[m_radioButtons.length - 1]);

        configure(m_radioButtons[0], tagEditor, controlId);
    
        assignDataToButtons();

        if (isSendEventOnExec) {
            addDefaultFocusListenerWithExec();
        } else {        
            addDefaultFocusListener();
        }
    }


    /** Preferred ctor. */
    public TBControlRadio(KGUIBuilder builder, 
                          String []labels,
                          String []tooltips,
                          String []states,
                          String migLayoutData,
                          int sectionId, 
                          ENodeId nodeId,
                          TBControl.EHControlId controlId) {
        
        this(builder, labels, tooltips, 
             states, migLayoutData, sectionId, nodeId, controlId, false);
    }
    
    
    /** Ctor for bool radios. */
    public TBControlRadio(KGUIBuilder builder, 
                          String []labels,
                          String []tooltips,
                          String migLayoutData,
                          int sectionId, 
                          ENodeId nodeId,
                          TBControl.EHControlId controlId) {

        this(builder, labels, tooltips, 
             new String[]{EBool.tristate2Str(ETristate.E_FALSE), 
                EBool.tristate2Str(ETristate.E_TRUE), 
                EBool.tristate2Str(ETristate.E_DEFAULT)},
                migLayoutData, sectionId, nodeId, controlId);
    }


    @Override
    public void setInput(CTestBase testBase, boolean isMerged) {
        super.setInput(testBase, isMerged);
        
        setSelection(m_tagEditor.getValue());
    }

    
    @Override
    public void setInput(CTestBase testBase, boolean isMerged, IActionExecutioner actionExecutioner) {
        super.setInput(testBase, isMerged, actionExecutioner);
        
        setSelection(m_tagEditor.getValue());
    }

    
    public void setEditable(boolean isEditable) {
        for (Button radio : m_radioButtons) {
            radio.setEnabled(isEditable);
        }
    }


    private void assignDataToButtons() {
        int idx = 0;
        for (Button radio : m_radioButtons) {
            radio.setData(ENUM_TXT, m_states[idx++]);
        }
    }

   
    private void addDefaultFocusListener() {
        RadioSectionFocusListener listener = new RadioSectionFocusListener();
        
        for (Button radioBtn : m_radioButtons) {
            radioBtn.addFocusListener(listener);
        }
        m_tagEditor.setCommentChangedListener(listener);
    }
    
    
    private void addDefaultFocusListenerWithExec() {
        RadioSectionFocusListener listener = new RadioSectionFocusListener();
        listener.setEventOnExec(true);
        
        for (Button radioBtn : m_radioButtons) {
            radioBtn.addFocusListener(listener);
        }
        m_tagEditor.setCommentChangedListener(listener);
    }
    
    
    public void addSelectionListener(SelectionListener selectionLister) {
        for (Button btn : m_radioButtons) {
            btn.addSelectionListener(selectionLister);
        }
    }

    
    public void addSelectionListener(SelectionListener selectionLister, int btnIdx) {
        
        m_radioButtons[btnIdx].addSelectionListener(selectionLister);
    }

    
    @Override
    public void addFocusListener(FocusListener selectionLister) {
        for (Button btn : m_radioButtons) {
            btn.addFocusListener(selectionLister);
        }
    }
    
    
    /**
     * Compares 'value' to 'states' and selects the radio button, which matches.
     * 
     * @param value
     * @param states
     */
    private void setSelection(String value) {
        
        if (m_isBoolRadios) {
            if (value.equals("0")  ||  value.equals("false") ||  value.equals("no")) {
                m_radioButtons[0].setSelection(true);
                m_radioButtons[1].setSelection(false);
                m_radioButtons[2].setSelection(false);
            } else if (value.equals("1")  ||  value.equals("true") ||  value.equals("yes")) {
                m_radioButtons[0].setSelection(false);
                m_radioButtons[1].setSelection(true);
                m_radioButtons[2].setSelection(false);
            } else if (value.isEmpty()) {
                m_radioButtons[0].setSelection(false);
                m_radioButtons[1].setSelection(false);
                m_radioButtons[2].setSelection(true);
            }
        } else {
            int idx = 0;
            for (String state : m_states) {
                boolean isSelected = value.equals(state);
                m_radioButtons[idx].setSelection(isSelected);
                idx++;
            }
        }
    }


    public String getSelection() {
        
        int idx = 0;
        for (Button button : m_radioButtons) {
            if (button.getSelection()) {
                return m_states[idx];
            }
            idx++;
        }
        
        throw new SIllegalStateException("No button is selected!")
                     .add("numButtons", m_radioButtons.length)
                     .add("numStates", m_states.length);
    }

    
    @Override
    public void setEnabled(boolean isEnabled) {
        
        super.setEnabled(isEnabled);
        
        for (Button button : m_radioButtons) {
            button.setEnabled(isEnabled);
        }
    }

    
    @Override
    protected void setBackgroundColor() {
        Color color = null;
        if (m_isMerged) {
            color = ColorProvider.instance().getColor(ColorProvider.MERGED_BKG_COLOR);
        } 
        for (Button btn : m_radioButtons) {
            btn.setBackground(color);
        }
    }
    
    
    class RadioSectionFocusListener implements FocusListener, ICommentChangedListener {

        private boolean m_isSendEventOnExec = false;

        
        public void setEventOnExec(boolean isSendEventOnExec) {
            m_isSendEventOnExec = isSendEventOnExec; 
        }


        @Override
        public void focusLost(FocusEvent e) {}


        @Override
        public void focusGained(FocusEvent e) {
            
            String state = (String)((Button)e.getSource()).getData(ENUM_TXT);
            m_tagEditor.setValue(state);
            m_tagEditor.setEnabled(!state.isEmpty());
            
            sendAction();
        }


        @Override
        public void commentChanged(YamlScalar scalar) {
            sendAction();
        }
        
        
        private void sendAction() {
            if (m_testBase != null) {
                // can be null when control is cleared - see clearInput()
                SetSectionAction action = new SetSectionAction(m_testBase,
                                                               m_nodeId,
                                                               m_tagEditor.getScalarCopy());
                sendActionAndVerify(action, m_isSendEventOnExec);
            }
        }
    }


    @Override
    public void commentChanged(YamlScalar scalar) {
        
        if (m_testBase != null) {
            SetSectionAction action = new SetSectionAction(m_testBase,
                                                           m_nodeId,
                                                           scalar.copy());
            
            // no model verification would be necessary for comments, but it also 
            // doesn't hurt
            sendActionAndVerify(action, false);
        }
    }
}
