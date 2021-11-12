package si.isystem.itest.ui.comp;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;

import si.isystem.connect.CTestBase;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.SetCommentAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class combines a concrete control (a Combo in this case) and a general
 * ValueAndCommentEditor. It connects the control and test specification 
 * with ValueAndCommentEditor.  
 */
public class TBControlCombo extends TBControl {

    private static String m_defaultValue;
    private SingleControlPerSectionFocusListener m_defaultFocuslistener;
    private Combo m_combo;
    private Map<String, String> m_uiStringsForEnum;

    
    /**
     * This ctor may be used for YamlScalar or YamlEnum types. When used for 
     * YamlEnum options[] must contain string enum values. To use other values 
     * for UI than enum values
     *  
     * @param builder
     * @param options
     * @param tooltip
     * @param migLayoutData
     * @param section
     * @param nodeId
     * @param controlId
     * @param swtStyle
     */
    private TBControlCombo(KGUIBuilder builder,
                          String [] options,
                          String tooltip,
                          String migLayoutData,
                          int section, 
                          ENodeId nodeId,
                          TBControl.EHControlId controlId,
                          int swtStyle) {
    
        super(nodeId);
    
        m_combo = builder.combo(options, migLayoutData, swtStyle);
        UiTools.setToolTip(m_combo, tooltip);
        ValueAndCommentEditor valCmtEditor = 
                ValueAndCommentEditor.newMixed(section, m_combo);

        configure(m_combo, valCmtEditor, controlId);
    
        addDefaultFocusListener(m_nodeId);
    }

    
    public TBControlCombo(Combo combo,
                          String tooltip,
                          int section, 
                          ENodeId nodeId,
                          TBControl.EHControlId controlId) {
    
        super(nodeId);
    
        m_combo = combo;
        UiTools.setToolTip(m_combo, tooltip);
        ValueAndCommentEditor valCmtEditor = 
                ValueAndCommentEditor.newMixed(section, m_combo);

        configure(m_combo, valCmtEditor, controlId);
    
        addDefaultFocusListener(m_nodeId);
    }


    /**
     * 
     * @param builder
     * @param uiStrings - indices must match enum values
     * @param tooltip
     * @param migLayoutData
     * @param section
     * @param nodeId
     * @param controlId
     * @param swtStyle
     * @return
     */
    public static TBControlCombo createForEnum(KGUIBuilder builder,
                                               String [] uiStrings,
                                               CTestBase emptyTestBase,
                                               String tooltip,
                                               String migLayoutData,
                                               int section, 
                                               ENodeId nodeId,
                                               TBControl.EHControlId controlId,
                                               int swtStyle) {

        StrVector values = new StrVector();
        emptyTestBase.getEnumValues(section, values);
        
        if (values.size() != uiStrings.length) {
            throw new SIllegalArgumentException("The number of UI strings and enum values must be the same!");
        }
        
        TBControlCombo tbControlCombo = new TBControlCombo(builder, uiStrings, tooltip, migLayoutData, section, nodeId, controlId, swtStyle);
        tbControlCombo.m_uiStringsForEnum = new TreeMap<>();
        for (int idx = 0; idx < uiStrings.length; idx++) {
            tbControlCombo.m_uiStringsForEnum.put(values.get(idx), uiStrings[idx]);
        }
        
        m_defaultValue = uiStrings[0];
        
        return tbControlCombo;
    }
    
    
    public static TBControlCombo createForText(KGUIBuilder builder,
                                               String [] options,
                                               String tooltip,
                                               String migLayoutData,
                                               int section, 
                                               ENodeId nodeId,
                                               TBControl.EHControlId controlId,
                                               int swtStyle) {
    
        return new TBControlCombo(builder, options, tooltip, migLayoutData, section, nodeId, controlId, swtStyle);
    }
                  
                  
    /*
    public TBControlText(Control control, ValueAndCommentEditor tagEditor, 
                         TBControl.EHControlId hControlId) {
        
        configure(control, tagEditor, hControlId);
        
        if (m_control instanceof Text) {
            Text text = (Text) m_control;
            if ((text.getStyle()  &  SWT.MULTI) != 0) {
                m_isMultilineText = true;
            }
        }
    } */

    
    @Override
    public void setInput(CTestBase testBase, boolean isMerged) {
        super.setInput(testBase, isMerged);
        setComboText();
    }
    
    
    private void setComboText() {

        String value = m_tagEditor.getValue();
        
        if (m_uiStringsForEnum != null) {
            value = m_uiStringsForEnum.get(value);
            if (value == null) { // no entry exists, use default one 
                value = m_defaultValue;
            }
        }
        
        m_combo.setText(value);
    }


    @Override
    public void setInput(CTestBase testBase, boolean isMerged, IActionExecutioner actionExecutioner) {
        super.setInput(testBase, isMerged, actionExecutioner);

        setComboText();
    }


    public void setEditable(boolean isEditable) {
        m_combo.setEnabled(isEditable);
    }
    
    
    protected boolean isControlEmpty() {
        return getCurrentText().trim().isEmpty(); 
    }

    /** Returns text from control. */
    public String getText() {
        return m_combo.getText();
    }
    
    
    /**
     * Returns text from control regarding to merged status - if it was not edited,
     * empty string is returned, which means that parent text is not overridden.
     *  
     * @return
     */
    public String getCurrentText() {
        String currentText = "";
        if (!isMerged()) {
            currentText = getText();
        }
        return currentText;
    }

    
    public void setTextInControl(String contents) {
        if (contents == null) {
            return;  // happens for linked controls, when not all of them are initialized
        }

        setBackgroundColor();
        
        m_combo.setText(contents);
    }

    
    // combo box specific controls - consider separating class for combo and text
    public void addSelectionListener(SelectionListener listener) {
            m_combo.addSelectionListener(listener);
    }
    
    
    public int getSelectionIndex() {
        return m_combo.getSelectionIndex();
    }
    
    
    public void select(int idx) {
        m_combo.select(idx);
    }
    
    
    @Override
    public void setFocus() {
        m_control.setFocus();
    }
    
    
    public void addDefaultFocusListener(ENodeId editorNodeId) {
        m_defaultFocuslistener = 
            new SingleControlPerSectionFocusListener(editorNodeId);
        
        m_control.addFocusListener(m_defaultFocuslistener);
        m_tagEditor.setCommentChangedListener(m_defaultFocuslistener);
    }
    
    
    @Override
    public void setMainTagEditor(ValueAndCommentEditor editor) {
        super.setMainTagEditor(editor);
        editor.setCommentChangedListener(new ICommentChangedListener() {
            
            @Override
            public void commentChanged(YamlScalar scalar) {
                YamlScalar commentScalar = m_mainTagEditor.getScalarCopy();

                SetCommentAction action = new SetCommentAction(m_testBase.getParent(), 
                                                               m_nodeId, 
                                                               commentScalar);
                
                action.addDataChangedEvent(m_nodeId, m_testBase.getContainerTestNode());
                action.addAllFireEventTypes();
                    
                if (action.isModified()) {
                    TestSpecificationModel.getActiveModel().execAction(action);
                }
            }
        });
    }
    
    
    public void sendSetSectionAction() {
        m_defaultFocuslistener.focusLost(null);
    }
    
    public void setEnabledDecoration(boolean isEnabled) {
        m_tagEditor.setEnabled(isEnabled);
    }
    
    
    class SingleControlPerSectionFocusListener implements FocusListener, 
                                                          ICommentChangedListener {
        protected ENodeId m_editorNodeId;
        protected String m_textOnEntry = "";

        SingleControlPerSectionFocusListener(ENodeId editorNodeId) {
            m_editorNodeId = editorNodeId;
        }

        @Override
        public void focusGained(FocusEvent e) {
            m_textOnEntry = getText().trim();
        }


        @Override
        public void focusLost(FocusEvent e) {
            sendAction();
        }


        @Override
        public void commentChanged(YamlScalar scalar) {
            sendAction();
        }


        protected void sendAction() {

            String value = m_combo.getText().trim();
            
            if (m_uiStringsForEnum != null) {
                // search for value in map - bad performance, but here it is not critical
                // as combo box with enum typically has less than 10 items, end it is not
                // changed that often.
                for (Map.Entry<String, String> pair : m_uiStringsForEnum.entrySet()) {
                    if (pair.getValue().equals(value)) {
                        value = pair.getKey();
                        break;
                    }
                }
            } 
            
            m_tagEditor.setValue(value);
            
            if (m_testBase != null) {
                // can be null when control is cleared - see clearInput()
                SetSectionAction action = new SetSectionAction(m_testBase,
                                                               m_editorNodeId,
                                                               m_tagEditor.getScalarCopy());
                // if test tree needs to be refreshed, it has to be refreshed also on EXEC,
                // for example when function name and test id are changed.
                sendActionAndVerify(action, m_isTestTreeRefreshNeeded);
            }
        }
    }
}
