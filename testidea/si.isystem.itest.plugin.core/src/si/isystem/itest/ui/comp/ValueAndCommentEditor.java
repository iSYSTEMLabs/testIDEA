package si.isystem.itest.ui.comp;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;

import si.isystem.connect.CTestBase;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.TagCommentDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.YamlScalar.KeyEditor;
import si.isystem.itest.model.YamlScalar.ListEditor;
import si.isystem.itest.model.YamlScalar.ListElementEditor;
import si.isystem.itest.model.YamlScalar.MixedEditor;
import si.isystem.itest.model.YamlScalar.UserMappingKeyValueEditor;
import si.isystem.itest.model.YamlScalar.ValueEditor;


/**
 * This class adds decoration for comment editing to the given control and 
 * provides a dialog for comment editing. It provides general access to 
 * test specification value, and is used in HierarchyContol classes.
 * 
 * @author markok
 */
public class ValueAndCommentEditor {

    public static final int DEFAULT_EOL_INDENT = 4;
    public static final int DEFAULT_NEW_LINE_INDENT_STEP = 2;

    private YamlScalar m_scalar;  // tag value with comments

    private ControlDecoration m_commentDecoration;
    private boolean m_isEnabled;

    private int m_yamlHierarchyLevel; // how deep in YAML tree is this item - used for
                                      // new line comment indentation.
    private ICommentChangedListener m_commentChangedListener;

    protected ValueAndCommentEditor(Control uiControl, Integer iconPosition, YamlScalar tag)
    {
        m_scalar = tag;
        
        // if icon position is not specified, do not create decoration
        if (iconPosition == null) {
            return;
        }
        
        m_commentDecoration = new ControlDecoration(uiControl, iconPosition);
        m_commentDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.NONEDITABLE_NO_INFO));
        m_commentDecoration.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (m_isEnabled) {
                    TagCommentDialog dlg = new TagCommentDialog(Activator.getShell());
                    String oldNlComment = m_scalar.getNewLineComment();
                    String oldEolComment = m_scalar.getEndOfLineComment();
                    
                    // remove trailing \n, because it is always part of string returned by YAML
                    // parser, but split() then returns one array element to much (the last one, which is empty)
                    String nlComment = StringUtils.stripEnd(oldNlComment, null);
                    String eolComment = StringUtils.stripEnd(oldEolComment, null);
                    
                    int nlCommentIndent = YamlScalar.getCommentIndent(nlComment);
                    int eolCommentIndent = YamlScalar.getCommentIndent(eolComment);

                    dlg.setNewLineComment(YamlScalar.stripCommentChars(nlComment));
                    dlg.setEndOfLineComment(YamlScalar.stripCommentChars(eolComment));
                    
                    if (dlg.show()) {

                        if (nlCommentIndent == 0) {
                            nlCommentIndent = m_yamlHierarchyLevel * DEFAULT_NEW_LINE_INDENT_STEP; 
                        }
                        if (eolCommentIndent == 0) {
                            eolCommentIndent = DEFAULT_EOL_INDENT;
                        }

                        String newNlComment = UiUtils.addCommentChar(dlg.getNewLineComment(), 
                                                                     nlCommentIndent);
                        String newEolComment = UiUtils.addCommentChar(dlg.getEndOfLineComment(), 
                                                                      eolCommentIndent);
                        
                        if (!newNlComment.equals(oldNlComment)) {
                            m_scalar.setNewLineComment(newNlComment);
                        }
                        
                        if (!newEolComment.equals(oldEolComment)) {
                            m_scalar.setEndOfLineComment(newEolComment);
                        }
                        
                        if (!newNlComment.equals(oldNlComment)  ||  
                            !newEolComment.equals(oldEolComment)) {
                            
                            if (m_commentChangedListener != null) {
                                m_commentChangedListener.commentChanged(m_scalar);
                            }
                            TestSpecificationModel.getActiveModel().setSectionEditorDirty(true);
                        }

                        setDecorationText();
                    }
                }
            }
        
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }

    
    // factory methods
    /** @see KeyEditor  */
    static public ValueAndCommentEditor newKey(int sectionId, Control uiControl) {
        return newKey(sectionId, uiControl, SWT.RIGHT | SWT.TOP);
    }
    
    /** @see KeyEditor  */
    static public ValueAndCommentEditor newKey(int sectionId, Control uiControl, int iconPosition) {
        YamlScalar tag = YamlScalar.newKey(sectionId);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, iconPosition, tag);
        return editor;
    }
    
    /** @see ListEditor  */
    static public ValueAndCommentEditor newList(int sectionId, Control uiControl) {
        YamlScalar tag = YamlScalar.newList(sectionId);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, SWT.RIGHT | SWT.TOP, tag);
        return editor;
    }
    
    /** @see ListElementEditor  */
    static public ValueAndCommentEditor newListElement(int sectionId, Control uiControl, int index) {
        YamlScalar tag = YamlScalar.newListElement(sectionId, index);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, SWT.RIGHT | SWT.TOP, tag);
        return editor;
    }
    
    /** @see ListElementEditor  */
    static public ValueAndCommentEditor newListElement(int sectionId, Control uiControl, int index, int iconPos) {
        YamlScalar tag = YamlScalar.newListElement(sectionId, index);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, iconPos, tag);
        return editor;
    }
    
    /** @see ListElementEditor(boolean)  */
    static public ValueAndCommentEditor newListElement(int sectionId, Control uiControl, int index, 
                                                       boolean isAllowIndexOutOfRangeOnRead) {
        
        YamlScalar tag = YamlScalar.newListElement(sectionId, index, isAllowIndexOutOfRangeOnRead);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, SWT.RIGHT | SWT.TOP, tag);
        return editor;
    }

    /** @see UserMappingKeyValueEditor  */
    static public ValueAndCommentEditor newUserMapping(int sectionId, Control uiControl, String key, int iconPos) {
        YamlScalar tag = YamlScalar.newUserMapping(sectionId, key);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, iconPos, tag);
        return editor;
    }
    
    
    /** @see ValueEditor  */
    static public ValueAndCommentEditor newValue(int sectionId, Control uiControl) {
        YamlScalar tag = YamlScalar.newValue(sectionId);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, SWT.RIGHT | SWT.TOP, tag);
        return editor;
    }
    
    /** @see ValueEditor  */
    static public ValueAndCommentEditor newValue(int sectionId, Control uiControl, Integer iconPosition) {
        YamlScalar tag = YamlScalar.newValue(sectionId);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, iconPosition, tag);
        return editor;
    }
    
    /** @see MixedEditor  */
    static public ValueAndCommentEditor newMixed(int sectionId, Control uiControl) {
        YamlScalar tag = YamlScalar.newMixed(sectionId);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, SWT.RIGHT | SWT.TOP, tag);
        return editor;
    }
    
    /** @see MixedEditor  */
    static public ValueAndCommentEditor newMixed(int sectionId, Control uiControl, Integer iconPosition) {
        YamlScalar tag = YamlScalar.newMixed(sectionId);
        ValueAndCommentEditor editor = new ValueAndCommentEditor(uiControl, iconPosition, tag);
        return editor;
    }
    
    
    /** Creates a new instance of scalar tag, which is copy of the existing one. */
    public YamlScalar getScalarCopy() {
        return m_scalar.copy();
    }


    /* public void setValue(String value, EQuotingType quotingType) {
        value = YamlScalar.quoteValue(value, quotingType);
        m_scalar.setValue(value);
        setDecorationText();
    } */
    
    
    public void setValue(String value) {
        m_scalar.setValue(value);
        setDecorationText();
    }
    
    
    public String getValueAndUpdateDecoration() {
        setDecorationText();
        return m_scalar.getValue();
    }

    
    public String getValue() {
        return m_scalar.getValue();
    }
    

    /*
    public String getTagValueFromTestSpec(CTestBase testBase) {
        m_yamlHierarchyLevel = testBase.getHierarchyLevel();
        return m_scalar.getTagValueFromTestSpec(testBase);
    } */

    
    public void updateValueAndCommentFromTestBase(CTestBase testBase) {
        if (testBase != null) {
            m_yamlHierarchyLevel = testBase.getHierarchyLevel();
            m_scalar.dataFromTestSpec(testBase);
        } else {
            m_scalar.setValue("");
            m_scalar.setNewLineComment("");
            m_scalar.setEndOfLineComment("");
        }
        setDecorationText();
    }
    
    /*
     * Should never be done directly, but only via Action, which uses YAMLScalar.
    public void writeValueAndCommentToTestSpec(CTestBase testBase) {
        m_scalar.dataToTestSpec(testBase);
    } */

    
    public String getCommentsAsToolTipText() {
        return m_scalar.getToolTipText();
    }
    
    
    public void setEnabled(boolean isEnabled) {
        
        m_isEnabled = isEnabled;
        setDecorationText();
    }


    int getSectionId() {
        return m_scalar.getSectionId();
    }
    
    public void setNewLineComment(String comment) {
        m_scalar.setNewLineComment(comment);
    }
    
    public void setEndOfLineComment(String comment) {
        m_scalar.setEndOfLineComment(comment);
    }
    
    public String getNewLineComment() {
        return m_scalar.getNewLineComment();
    }
    
    public String getEndOfLineComment() {
        return m_scalar.getEndOfLineComment();
    }
    
    public void setCommentChangedListener(ICommentChangedListener listener) {
        m_commentChangedListener = listener;
    }
    
    
    private void setDecorationText() {
        
        if (m_commentDecoration == null) {
            // when there is no decoration for the given control
            return;
        }

        String comment = m_scalar.getToolTipText();
        
        if (!comment.isEmpty()) {
            m_commentDecoration.setDescriptionText(comment);
            if (m_isEnabled) {
                m_commentDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.EDITABLE_INFO));
            } else {
                m_commentDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.NONEDITABLE_INFO));
            }
        } else {
            m_commentDecoration.setDescriptionText("");
            if (m_isEnabled) {
                m_commentDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.EDITABLE_NO_INFO));
            } else {
                m_commentDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.NONEDITABLE_NO_INFO));
            }
        }
    }
}
