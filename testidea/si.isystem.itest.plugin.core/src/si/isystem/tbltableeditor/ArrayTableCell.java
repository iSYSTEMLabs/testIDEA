package si.isystem.tbltableeditor;

import java.util.List;

import de.kupzog.ktable.renderers.TextIconsContent.EEditorType;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.ETristate;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.SetCommentAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.itest.model.actions.mapping.RemoveFromUserMappingAction;
import si.isystem.itest.model.actions.mapping.ReplaceMappingKeyAction;
import si.isystem.itest.model.actions.sequence.InsertToSequenceAction;
import si.isystem.itest.model.actions.sequence.RemoveFromSeqAction;
import si.isystem.itest.model.actions.sequence.SetSequenceItemAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

/**
 * This class contains data required to obtain table cell value from test 
 * specification model.
 * 
 * @author markok
 *
 */
public class ArrayTableCell {
    
    final private CTestBase m_testBase;
    private HeaderNode m_headerNode;
    private boolean m_isReadOnly; // used when section is inherited

    
    /** Ctor. for test base scalars. */
    ArrayTableCell(CTestBase testBase, HeaderNode headerNode, boolean isReadOnly) {
        m_testBase = testBase;
        m_headerNode = headerNode;
        m_isReadOnly = isReadOnly;
    }
    
    /** Ctor for mapping values. */
//    public ArrayTableCell(CTestBase tb, int sectionId, ENodeId nodeId, 
//                          String key, boolean isReadOnly) {
//        this(tb, sectionId, nodeId, isReadOnly);
//        m_key = key;
//        m_editorType = EEditorType.EText;
//    }

    /** Ctor for seq. values. */
//    public ArrayTableCell(CTestBase tb, int sectionId, ENodeId nodeId, int seqIdx, boolean isReadOnly) {
//        this(tb, sectionId, nodeId, isReadOnly);
//        m_seqIdx = seqIdx;
//        m_editorType = EEditorType.EText;
//    }

    
//    ArrayTableCell(CTestBase testBase, int sectionId, ENodeId nodeId, boolean defaultForTristate, boolean isReadOnly) {
//        this(testBase, sectionId, nodeId, isReadOnly);
//        m_editorType = EEditorType.ETristate;
//        m_defaultForTristate = defaultForTristate;
//    }
    

//    ArrayTableCell(CTestBase testBase, int sectionId, ENodeId nodeId, 
//                   String[]comboItems, String defaultEnumValue, boolean isReadOnly) {
//        this(testBase, sectionId, nodeId, isReadOnly);
//        m_editorType = EEditorType.ECombo;
//        m_comboItems = comboItems;
//        m_defaultEnumValue = defaultEnumValue;
//    }
    

    public CTestBase getTestBase() {
        return m_testBase;
    }


    public int getSection() {
        return m_headerNode.getSectionId();
    }

    
    public String getKey() {
        return m_headerNode.getName();
    }

    
    /** Returns value from model. */
    public String getValue() {
        
        if (m_testBase == null) {
            return "";
        }
        
        if (m_headerNode.isUserMappingLeafNode()) {
            // mapping
            CMapAdapter map = new CMapAdapter(m_testBase, getSection(), true);
            String key = m_headerNode.getName();
            if (map.contains(key)) { // not all mappings must contain all items 
                                       // with keys in table header
                return map.getValue(key);
            } 
            return "";
            
        } else if (m_headerNode.isUserSeqenceLeafNode()) {
            // sequence
            CSequenceAdapter seq = new CSequenceAdapter(m_testBase, getSection(), true);
            int seqIdx = Integer.parseInt(m_headerNode.getName());
            if (seqIdx < seq.size()) {
                return seq.getValue(seqIdx);
            } 
            return "";
           
        } else if (m_headerNode.isBoolNode()) {
            return m_testBase.getTagValue(getSection());
            
        } else {
            // this is scalar
            return m_testBase.getTagValue(getSection());
        } 
    }
    

    /** Returns tristate value as enum string, or normal string for non-tristate cells. */ 
    public String getTristateValue() {
        
        if (m_testBase == null) {
            if (m_headerNode.isBoolNode()) {
                return m_headerNode.getDefaultBool() ? ETristate.E_TRUE.toString() : 
                                                       ETristate.E_FALSE.toString() ;
            }
            return "";
        }
        
        if (m_headerNode.isBoolNode()) {
            return m_testBase.getBoolTagValue(getSection()).toString();
        }
        
        return m_testBase.getTagValue(getSection());
    }
    
    
    public String[] getComments() {

        if (m_testBase == null) {
            return new String[]{"", ""};
        }

        YamlScalar scalar = getYamlScalar();
        scalar.dataFromTestSpec(m_testBase);
        return new String[]{scalar.getNewLineComment(), scalar.getEndOfLineComment()};
    }
    
    
    public String getComment() {
        String[] comments = getComments();
        return YamlScalar.getToolTipText(comments[0], comments[1]);
    }
    

    public EEditorType getEditorType() {
        if (m_headerNode.isEnumNode()) {
            return EEditorType.ECombo;
        } else if (m_headerNode.isBoolNode()) {
            return EEditorType.ETristate;
        }
        
        return EEditorType.EText;
    }

    
    /**
     * @return the comboItems
     */
    public String[] getComboItems() {
        return m_headerNode.getEnumItems();
    }

    /**
     * @return the defaultEnumValue
     */
    public String getDefaultEnumValue() {
        return m_headerNode.getDefaultEnumValue();
    }
    
    
    /**
     * @return the defaultForTristate
     */
    public boolean getDefaultForTristate() {
        return m_headerNode.getDefaultBool();
    }


    private YamlScalar getYamlScalar() {
        YamlScalar value = null;
        if (m_headerNode.isUserSeqenceLeafNode()) {
            value = YamlScalar.newListElement(getSection(), Integer.parseInt(m_headerNode.getName()));
        } else if (m_headerNode.isUserMappingLeafNode()) {
            value = YamlScalar.newUserMapping(getSection(), m_headerNode.getName());
        } else {
            value = YamlScalar.newMixed(getSection());
        }
        return value;
    }

    
    public AbstractAction createSetValueAction(YamlScalar scalar, List<String> predecessors) {

        if (m_testBase == null) {
            return null;
        }

        AbstractAction action = null;
        ENodeId nodeId = m_headerNode.getUiNodeId();
        
        if (m_headerNode.isUserMappingLeafNode()) {
            if (scalar.getValue().isEmpty()) {
                // empty assignments are not possible (for example, statement 'x = '
                // makes no sense, so remove key if assignment is empty
                action = new RemoveFromUserMappingAction(m_testBase, 
                                                         scalar.getSectionId(), 
                                                         m_headerNode.getName());
            } else {
                action = new InsertToUserMappingAction(m_testBase,
                                                       scalar,
                                                       predecessors);
            }
        } else if (m_headerNode.isUserSeqenceLeafNode()) {
            action = new SetSequenceItemAction(m_testBase, nodeId, scalar);
        } else {
            action = new SetSectionAction(m_testBase, nodeId, scalar);
        }
        
        if (action.isModified()) {
            action.addDataChangedEvent(nodeId, m_testBase);
            action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
        }
        
        return action;
    }
    
    
    public AbstractAction createRemoveAction() {
        
        if (m_testBase == null) {
            return null;
        }

        AbstractAction action = null;
        if (m_headerNode.isUserMappingLeafNode()) {
            action = new RemoveFromUserMappingAction(m_testBase, getSection(), m_headerNode.getName());
        } else if (m_headerNode.isUserSeqenceLeafNode()) {
            action = new RemoveFromSeqAction(m_testBase, getSection(), Integer.parseInt(m_headerNode.getName()));
        } else {
            throw new SIllegalStateException("Can not create remove action for " +
            		"table cells which are not in sequence or user mapping!").
            		add("sectionId", getSection());
        }
        
        // action.addDataChangedEvent();
        // action.addFireEventType(EFireEvent.UNDO);
        return action;
    }

    
    public boolean existsInModel() {
        
        if (m_testBase == null) {
            return false;
        }
        
        if (m_headerNode.isUserMappingLeafNode()) {  // user mapping may not exist in model
            CMapAdapter userMapping = new CMapAdapter(m_testBase, 
                                                      getSection(), 
                                                      true);
            return userMapping.contains(m_headerNode.getName());
            
        } else if (m_headerNode.isUserSeqenceLeafNode()) { 
            // in some cases refresh on KTable may be called
            // between model update and ArrayModel update (on Undo)
            CSequenceAdapter seq = new CSequenceAdapter(m_testBase, getSection(), true);
            long seqIdx = Integer.parseInt(m_headerNode.getName());
            if (seqIdx >= seq.size()) {
                return false;
            }
        }
        
        return true; 
    }

    
    public AbstractAction createSeqInsertAction(int insertedCol) {

        if (m_testBase == null) {
            return null;
        }

        AbstractAction action = null;

        if (m_headerNode.isUserSeqenceLeafNode()) {
            YamlScalar value = YamlScalar.newListElement(getSection(), insertedCol);
            action = new InsertToSequenceAction(m_testBase, value);
        } else {
            throw new SIllegalStateException("Can not create insert action for " +
                    "table cells which are not in sequence!").
                    add("sectionId", getSection());
        }
        
        return action;
    }

    
    public AbstractAction createRenameAction(String newKey) {

        if (m_testBase == null) {
            return null;
        }

        if (m_headerNode.isUserMappingLeafNode()) {
            CMapAdapter map = new CMapAdapter(m_testBase, getSection(), true);
            String key = m_headerNode.getName(); // user mapping leaf node contains mapping key
            if (map.contains(key)) {
                return new ReplaceMappingKeyAction(m_testBase, getSection(), key, newKey);
            }
            return null;
        }
        
        throw new SIllegalStateException("Can not create rename user mapping key action for " +
                "table header cells which are not in user mapping!").
                add("sectionId", getSection())
                .add("oldKey", m_headerNode.getName())
                .add("newKey", newKey);
    }

    
    public AbstractAction createSetCommentAction(String nlComment, String eolComment) {

        if (m_testBase == null) {
            return null;
        }

        YamlScalar scalar = getYamlScalar();
        scalar.setNewLineComment(nlComment);
        scalar.setEndOfLineComment(eolComment);

        ENodeId nodeId = m_headerNode.getUiNodeId();
        SetCommentAction action = new SetCommentAction(m_testBase, nodeId, scalar);
        action.addDataChangedEvent(nodeId, m_testBase);
        action.addFireEventType(EFireEvent.UNDO);
        action.addFireEventType(EFireEvent.REDO);
        return action;
    }
    
    
    boolean isUserSeqCell() {
        return m_headerNode.isUserSeqenceLeafNode();
    }

    
    boolean isUserMappingCell() {
        return m_headerNode.isUserMappingLeafNode();
    }
    
    
    public boolean isTestBaseList() {
        return m_headerNode.isTestBaseList();
    }
    
    
    public boolean isTestBase() {
        return m_headerNode.isStructMapping();
    }

    
    boolean isReadOnlyCell() {
        return m_isReadOnly;
    }
}


