package si.isystem.itest.ui.comp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;

import si.isystem.connect.CTestBase;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.SetCommentAction;
import si.isystem.itest.model.actions.sequence.SetSequenceItemAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.MarkdownParser;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This class combines a concrete control (a Text in this case) and a general
 * ValueAndCommentEditor. It connects the control and test specification 
 * with ValueAndCommentEditor.  
 */
public class TBControlText extends TBControl {

    private boolean m_isMultilineText = false;
    private SingleControlPerSectionFocusListener m_defaultFocuslistener;
    private List<IAsistListener> m_asistListeners;
    private ITextControl m_text;
    private String m_markdownText = null;


    /** 
     * Use factory method createForMixed() instead
     * Creates control for mixed element - key/value pair. 
     */
    private TBControlText(ITextControl txtCtrl,
                          int section, 
                          ENodeId nodeId,
                          TBControl.EHControlId controlId, 
                          int swtStyle) {
    
        super(nodeId);
        m_text = txtCtrl;
        createControl(swtStyle);

        ValueAndCommentEditor valCmtEditor = 
                               ValueAndCommentEditor.newMixed(section, m_text.getControl());

        configure(m_text.getControl(), valCmtEditor, controlId);
    
        addDefaultFocusListener();
    }


    private TBControlText(ITextControl txtCtrl,
                          ENodeId nodeId,
                          int swtStyle) {
        super(nodeId);
        m_text = txtCtrl;                
        createControl(swtStyle);
    }
    

    /** Creates control for mixed element - key/value pair. */
    public static TBControlText createForMixed(KGUIBuilder builder, 
                                               String tooltip,
                                               String migLayoutData,
                                               int section, 
                                               ENodeId nodeId,
                                               TBControl.EHControlId controlId, 
                                               int swtStyle) {
        
        TextAdapter textCtrl = new TextAdapter(builder, tooltip, migLayoutData, swtStyle);
        TBControlText tbText = new TBControlText(textCtrl,
                                                 section, 
                                                 nodeId,
                                                 controlId,
                                                 swtStyle);
        
        return tbText;
    }
    

    /** 
     * Creates control for list, for example tags and parameters.
     */
    public static TBControlText createForList(KGUIBuilder builder,
                                              String tooltip,
                                              String migLayoutData,
                                              int section,
                                              ENodeId nodeId,
                                              EHControlId controlId,
                                              int swtStyle) {
        
        TextAdapter textCtrl = new TextAdapter(builder, tooltip, migLayoutData, swtStyle);
        TBControlText tbText = new TBControlText(textCtrl, nodeId, swtStyle);
        
        ValueAndCommentEditor valCmtEditor = 
                          ValueAndCommentEditor.newList(section, tbText.m_text.getControl());

        tbText.configure(tbText.m_text.getControl(), valCmtEditor, controlId);

        tbText.addDefaultFocusListener();
        
        return tbText;
    }

    
    /** 
     * Creates control for list element, for example 'min' value of profiler 
     * min-max range.
     */
    public static TBControlText createForListElement(KGUIBuilder builder, 
                                                     String tooltip,
                                                     String migLayoutData,
                                                     int section, 
                                                     ENodeId nodeId,
                                                     TBControl.EHControlId controlId, 
                                                     int swtStyle,
                                                     int index,
                                                     boolean isAllowIndexOutOfRangeOnRead) {
        
        TextAdapter textCtrl = new TextAdapter(builder, tooltip, migLayoutData, swtStyle);
        TBControlText tbText = new TBControlText(textCtrl, nodeId, swtStyle);
        
        ValueAndCommentEditor valCmtEditor = 
                ValueAndCommentEditor.newListElement(section, tbText.m_text.getControl(), 
                                                     index, isAllowIndexOutOfRangeOnRead);

        tbText.configure(tbText.m_text.getControl(), valCmtEditor, controlId);

        tbText.addDefaultFocusListenerForSeq();
        
        return tbText;
    }
    

    /** Creates StyledText control for mixed element - key/value pair. */
    public static TBControlText createForStyledTextMixed(KGUIBuilder builder, 
                                                         String tooltip,
                                                         String migLayoutData,
                                                         int section, 
                                                         ENodeId nodeId,
                                                         TBControl.EHControlId controlId, 
                                                         int swtStyle) {
        
        StyledTextAdapter textCtrl = new StyledTextAdapter(builder, tooltip, 
                                                           migLayoutData, swtStyle);
        TBControlText tbText = new TBControlText(textCtrl,
                                                 section, 
                                                 nodeId,
                                                 controlId,
                                                 swtStyle);
        
        return tbText;
    }
    
    
    private void createControl(int swtStyle) {

        if ((swtStyle  &  SWT.MULTI) != 0) {
            m_isMultilineText = true;
        }
        
        m_asistListeners = new ArrayList<>();
    }


    @Override
    public void setInput(CTestBase testBase, boolean isMerged) {
        super.setInput(testBase, isMerged);
        
        valueToControl();
    }


    @Override
    public void setInput(CTestBase testBase, 
                         boolean isMerged, 
                         IActionExecutioner actionExecutioner) {
        
        super.setInput(testBase, isMerged, actionExecutioner);
        
        valueToControl();
    }

    
    private void valueToControl() {
        String value = m_tagEditor.getValue();
        setTextInControl(value);
    }


    public void setEditable(boolean isEditable) {
        m_text.setEditable(isEditable);
    }
    
    
    /** Returns text from control. */
    public String getText() {

        String str = m_text.getText().trim();
        if (m_isMultilineText) { 
            str = fixLineEndings(str);
        }
        return str;
    }


    static String fixLineEndings(String str) {
        // Windows use '\r\n' as EOL *$#&@!
        str = str.replace("\r", "");
        // if text contains tabs or spaces at the end of lines, then
        // YAML emitter changes literal style to escaped quoted style,
        // which is much more difficult to read in source 
        str = str.replace("\t", "        ");
        str = str.replaceAll("[ \t]+\n", "\n");
        return str;
    }
    

    protected boolean isControlEmpty() {
        return getText().isEmpty(); 
    }

    
    public void setTextInControl(String contents) {
        if (contents == null) {
            return;  // happens for linked controls, when not all of them are initialized
        }

        setBackgroundColor();
        
        m_text.setText(contents);
    }

    
    private void addDefaultFocusListener() {
        m_defaultFocuslistener = 
            new SingleControlPerSectionFocusListener(m_nodeId);
        
        m_control.addFocusListener(m_defaultFocuslistener);
        m_tagEditor.setCommentChangedListener(m_defaultFocuslistener);
    }
    
    
    public void addDefaultFocusListenerForSeq() {
        SeqFocusListener seqFocuslistener = new SeqFocusListener(m_nodeId);
            
        m_control.addFocusListener(seqFocuslistener);
        m_tagEditor.setCommentChangedListener(seqFocuslistener);
    }

    
    public void addAsistListener(IAsistListener listener) {
        m_asistListeners.add(listener);
    }
    
    
    public void sendSetSectionAction() {
        m_defaultFocuslistener.focusLost(null);
    }
    
    
    public void addKeyListener(KeyListener listener) {
        m_control.addKeyListener(listener);
    }

    
    @Override
    public void addFocusListener(FocusListener listener) {
        m_control.addFocusListener(listener);
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
                    m_actionExecutioner.execAction(action);
                }
            }
        });
    }
    
    
    public void markAsEditable() {
        m_control.setBackground(ColorProvider.instance().getBkgColor());
    }

    
    public void markAsNoneditable() {
        m_control.setBackground(ColorProvider.instance().getBkgNoneditableColor());
    }
    
    
    public void markAsError() {
        m_control.setBackground(ColorProvider.instance().getErrorColor());
    }

    
    public void showStyledText(boolean isShowStyled) {
        
        if (isShowStyled) {
            // If this m. is called twice with isShowStyled == true, do not update 
            // m_markdownText, because formatting tags would be lost on second call to
            // getText().
            if (m_markdownText == null) {
                setEditable(false);
                markAsNoneditable();
                m_markdownText = getText();
                MarkdownParser mdParser = new MarkdownParser(getControl()); // used for font creation

                List<StyleRange> styles = new ArrayList<>();
                StringBuilder sb = mdParser.markdown2StyleRanges(m_markdownText, styles);

                m_text.setText(sb.toString());
                m_text.setStyleRanges(styles.toArray(new StyleRange[0]));
            }
        } else {
            if (m_markdownText != null) {
                setEditable(true);
                markAsEditable();
                setTextInControl(m_markdownText);
                m_markdownText = null;
            }
        }
    }

        
    class SingleControlPerSectionFocusListener implements FocusListener, 
                                                          ICommentChangedListener {
        protected ENodeId m_editorNodeId;

        SingleControlPerSectionFocusListener(ENodeId editorNodeId) {
            m_editorNodeId = editorNodeId;
        }

        @Override
        public void focusGained(FocusEvent e) {
        }


        @Override
        public void focusLost(FocusEvent e) {
            // This 'if' statement  prevents setting of StyledText, when it is in view mode. There is no markup
            // in view mode, and if the user clicks StyledText control and then in another control,
            // this m. is called, content is different (no markup chars), so send action would
            // be called and set new text without markup. See B019770. 
            if (m_text.isEditable()) {
                sendAction();
            }
        }


        @Override
        public void commentChanged(YamlScalar scalar) {
            sendAction();
        }


        protected void sendAction() {
            
            String value = getText();
            // modifies existing text before action is sent, for example analyzer file 
            // extension 'trd' is added this way
            for (IAsistListener asistListener : m_asistListeners) {
                value = asistListener.onFocusLost(value);
            }
            
            String origValue = m_text.getText().trim();
            if (!value.equals(origValue)) {
                int pos = m_text.getCaretPosition();
                setTextInControl(value);
                m_text.setSelection(pos);
            }
            
            m_tagEditor.setValue(value);
            m_tagEditor.setEnabled(!value.isEmpty()); // it is not merged if we get here

            if (m_testBase != null) {
                // can be null when control is cleared - see clearInput()
                SetSectionAction action = new SetSectionAction(m_testBase,
                                                               m_editorNodeId,
                                                               m_tagEditor.getScalarCopy());
                sendActionAndVerify(action, m_isTestTreeRefreshNeeded);
            }
        }
    }
    
    
    
    class SeqFocusListener extends SingleControlPerSectionFocusListener {
        

        SeqFocusListener(ENodeId editorNodeId) {
            super(editorNodeId);
        }

        
        @Override
        protected void sendAction() {
            
            String value = getText(); 
            m_tagEditor.setValue(value);
            m_tagEditor.setEnabled(!value.isEmpty()); // it is not merged if we get here
            
            if (m_testBase != null) {
                // can be null when control is cleared - see clearInput()
                SetSequenceItemAction action = new SetSequenceItemAction(m_testBase,
                                                                         m_editorNodeId,
                                                                         m_tagEditor.getScalarCopy());
                // if test tree needs to be refreshed, it has to be refreshed also on EXEC,
                // for example when function name and test id are changed.
                sendActionAndVerify(action, m_isTestTreeRefreshNeeded);
            }
        }
    }
}
