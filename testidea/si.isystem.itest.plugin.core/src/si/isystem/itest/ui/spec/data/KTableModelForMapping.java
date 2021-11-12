package si.isystem.itest.ui.spec.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.ktableutils.KTableEditorModel;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.itest.model.actions.mapping.MovePairAction;
import si.isystem.itest.model.actions.mapping.RemoveFromUserMappingAction;
import si.isystem.itest.model.actions.mapping.ReplaceMappingKeyAction;
import si.isystem.itest.model.actions.mapping.SetSectionMappingAction;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.IModelWithComment;

public class KTableModelForMapping extends KTableEditorModel implements IModelWithComment {

    private CTestBase m_testBase;
    private int m_sectionId;
    private ENodeId m_nodeId;

    private final int COL_LINE_NO = 0;
    private final int COL_KEYS = 1;
    private final int COL_VALUES = 2;

    private final int COL_BODY_KEYS = 0;
    private final int COL_BODY_VALUES = 1;
    
//    private IContentProposalProvider m_keyProposalProvider;
//    private IContentProposalProvider m_valueProposalProvider;
    
    /**
     * 
     * @param sectionId
     * @param nodelId
     * @param columnTitles text for column headers. Text for column 0 must be 
     *                     present, but it is currently not shown. Example:
     *                     new String[]{"", "Type", "Name"} 
     */
    public KTableModelForMapping(int sectionId,
                                 ENodeId nodelId,
                                 String[] columnTitles) {
        super();

        m_sectionId = sectionId;
        m_nodeId = nodelId;
        
        super.setColumnTitles(columnTitles);
    }

    
    @Override
    public TextIconsContent getBodyCellContent(int bodyCol, int bodyRow) {

        if (m_testBase == null) {
            return null;
        }
        
        TextIconsContent cellContent = new TextIconsContent();
        cellContent.setEditable(m_isEnabled);
        
        CMapAdapter mapping = new CMapAdapter(m_testBase, 
                                              m_sectionId,
                                              true);
        
        String key = mapping.getKey(bodyRow);
        
        if (bodyCol == COL_BODY_KEYS) {
            cellContent.setText(key);
        } else {
            cellContent.setText(mapping.getValue(key));
        }
        
        // handle comments - they are shown only in the first column, value
        // has end of line column, which is shown already here.
        if (bodyCol == COL_BODY_KEYS) {
            String nlComment = m_testBase.getComment(m_sectionId, key,
                                                     CommentType.NEW_LINE_COMMENT);
            String eolComment = m_testBase.getComment(m_sectionId, key,
                                                      CommentType.END_OF_LINE_COMMENT);

            doGetComment(cellContent, nlComment, eolComment);
//            cellContent.setNlComment(nlComment);
//            cellContent.setEolComment(eolComment);
//
//            boolean isCommentSet = !(nlComment + eolComment).trim().isEmpty();
//            cellContent.setIcon(EIconPos.ETopLeft, 
//                                getCommentIcon(cellContent.getText(), isCommentSet), 
//                                true);
//            
//            setTooltipFromComment(cellContent, nlComment, eolComment, isCommentSet);
        }
        
        setBackground(cellContent, m_isEnabled);
        
        return cellContent;
    }


//    @Override
//    protected KTableCellEditor getBodyCellEditor(int bodyCol, 
//                                                 int bodyRow) {
//        
//        if (m_isEnabled) {
//            ContentProposalConfig cfg = new ContentProposalConfig(new String[0]);
//            UiUtils.setContentProposalsConfig(cfg);
//            cfg.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
//            
//            if (bodyCol == COL_BODY_KEYS  &&  m_keyProposalProvider != null) {
//                cfg.setProposalProvider(m_keyProposalProvider);
//            } else if (bodyCol == COL_BODY_VALUES  &&  m_valueProposalProvider != null) {
//                cfg.setProposalProvider(m_valueProposalProvider);
//            }
//            
//            return new KTableCellEditorText2(cfg);
//        }
//        
//        return null;
//    }
    

    @Override
    protected void setBodyContentAt(int dataCol, int dataRow, String text) {

        AbstractAction action = createSetCellAction(dataCol, dataRow, text, null, null);
              
        if (action != null) {
            notifyListeners(action, null, true);
        }
    }


    private AbstractAction createSetCellAction(int dataCol,
                                               int dataRow,
                                               String text,
                                               String nlComment,
                                               String eolComment) {
        if (m_testBase == null) {
            return null;
        }

        AbstractAction action = null; 
        CMapAdapter mapping = new CMapAdapter(m_testBase, 
                                              m_sectionId,
                                              true);
        
        switch (dataCol) {
        case COL_BODY_KEYS: {
            StrVector keys = new StrVector();
            mapping.getKeys(keys);
            
            if (mapping.contains(text)) {
                StatusView.getView().flashDetailPaneText(StatusType.FATAL, 
                    "Can not add line - item with empty name already exists: '" + text + "'");
                
                return null; // setting another key to empty value just throws exception
                             // and makes everything slow
            }
            
            if (dataRow < keys.size()) {
                String oldKey = keys.get(dataRow);
                action = new ReplaceMappingKeyAction(m_testBase, m_sectionId, oldKey, text); 
                
            } else {
                action = createNewMappingRowAction(text);
            }
        } break;
        
        case COL_BODY_VALUES: {
            YamlScalar scalar = YamlScalar.newUserIndexMapping(m_sectionId, dataRow);
            scalar.setValue(text);
            if (nlComment != null) {
                scalar.setNewLineComment(nlComment);
            }
            if (eolComment != null) {
                scalar.setEndOfLineComment(eolComment);
            }
            action = new SetSectionMappingAction(m_testBase, m_nodeId, scalar);
        } break;
        default:
            // bug, but do nothing as throwing exception makes table invalid
        }
        
        if (action != null) {
            action.addDataChangedEvent(m_nodeId, m_testBase);
        }
        
        return action;
    }

    
/*    private AbstractAction createAddRowAction(int dataCol,
                                               int dataRow,
                                               String text,
                                               String nlComment,
                                               String eolComment) {
        AbstractAction action = null; 
        CMapAdapter mapping = new CMapAdapter(m_testBase, 
                                              m_sectionId,
                                              true);
        
        if (dataCol == COL_BODY_KEYS) {
            StrVector keys = new StrVector();
            mapping.getKeys(keys);
            
            if (mapping.contains(text)) {
                return null; // setting another key to empty value just throws exception
                             // and makes everything slow
            }
            
            if (dataRow < keys.size()) {
                String oldKey = keys.get(dataRow);
                action = new ReplaceMappingKeyAction(m_testBase, m_sectionId, oldKey, text); 
                
            } else {
                throw new IllegalStateException("Internal error: add() should be called");
            }
            // mapScalar.setValue(value);
            // action = new InsertToUserMappingAction(m_testBase, mapScalar, predecessors); 
        } else {
            // cell in value column alone can not be added, as there is no meaningful key
        }
        
        if (action != null) {
            action.addDataChangedEvent(m_nodeId, m_testBase);
        }
        
        return action;
    }
*/
    
    @Override
    public int getBodyRowCount() {
        if (m_testBase != null) {
            CMapAdapter mapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId,
                                                  true);
            return (int)mapping.size();
        }
        return 0;
    }

    
    @Override
    public int getInitialColumnWidth(int column) {
        
        switch (column) {
        
        case COL_LINE_NO:
            return 50;
            
        case COL_KEYS:
            return 250;
            
        case COL_VALUES:
            return 250;
        }
        
        return 100;
    }

    
    public void setData(CTestBase testBase) {
        m_testBase = testBase;
    }

    
    @Override
    public void addRow(int row) {
        try {
            final String NEW_KEY = ""; // add empty row - user will enter the key
            InsertToUserMappingAction action = createNewMappingRowAction(NEW_KEY); 
            if (action != null) {
                notifyListeners(action, null, true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore exceptions here - they will be reported later
            throw ex;
        }
    }


    protected InsertToUserMappingAction createNewMappingRowAction(final String newKey) {
        if (m_testBase == null) {
            return null;
        }

        CMapAdapter mapping = new CMapAdapter(m_testBase, m_sectionId, true);
        
        if (mapping.contains(newKey)) {
            StatusView.getView().flashDetailPaneText(StatusType.FATAL, 
                                 "Can not add line - item with empty name already exists!");
            return null;
        }
        
        YamlScalar yScalar = YamlScalar.newUserMapping(m_sectionId, newKey);
        
        return new InsertToUserMappingAction(m_testBase, yScalar, null);
    }


    @Override
    public void removeRow(int row) {

        if (m_testBase == null) {
            return;
        }

        int dataRow = row - NUM_HDR_ROWS;
        try {
            CMapAdapter mapping = new CMapAdapter(m_testBase, 
                                                  m_sectionId,
                                                  true);
            String key = mapping.getKey(dataRow);
            AbstractAction action = new RemoveFromUserMappingAction(m_testBase,
                                                                    m_sectionId,
                                                                    key);
            notifyListeners(action, null, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore exceptions here - they will be reported later
            throw ex;
        }
    }


    public void removeRows(List<Integer> rows) {
        
        if (m_testBase == null) {
            return;
        }

        GroupAction groupAction = new GroupAction("Remove mapping table rows");
        Comparator<Object> comparator = Collections.reverseOrder();
        Collections.sort(rows, comparator);
        
        try {
            for (Integer row : rows) {
                CMapAdapter mapping = new CMapAdapter(m_testBase, 
                                                      m_sectionId,
                                                      true);
                String key = mapping.getKey(row);
                AbstractAction action = new RemoveFromUserMappingAction(m_testBase,
                                                                        m_sectionId,
                                                                        key);
                groupAction.add(action);
            }
            
            notifyListeners(groupAction, null, true);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore exceptions here - they will be reported later
            throw ex;
        }
    }


    @Override
    public void swapRows(int first, int second) {

        if (m_testBase == null) {
            return;
        }

        first -= NUM_HDR_ROWS;
        second -= NUM_HDR_ROWS;
        
        if (first == second  ||  first < 0  ||  second < 0) {
            return;
        }
        
        CMapAdapter mapping = new CMapAdapter(m_testBase, m_sectionId, true);
        
        if (first >= mapping.size()  ||  second >= mapping.size()) {
            return;
        }
        
        MovePairAction action = new MovePairAction(m_testBase, m_sectionId, 
                                                   mapping.getKey(first),
                                                   second);
        
        notifyListeners(action, null, true);
    }

    
    @Override
    public void setCellComment(int col,
                               int row,
                               String nlComment,
                               String eolComment) {
        if (m_testBase == null) {
            return;
        }

        CMapAdapter mapping = new CMapAdapter(m_testBase, m_sectionId, true);
        int dataRow = row - NUM_HDR_ROWS;
        String key = mapping.getKey(dataRow);
        
        YamlScalar value = YamlScalar.newUserMapping(m_sectionId, key);
        value.dataFromTestSpec(m_testBase);
        value.setNewLineComment(nlComment);
        value.setEndOfLineComment(eolComment);

        AbstractAction action = 
                      new SetSectionMappingAction(m_testBase, m_nodeId, value);
        
        notifyListeners(action, null, true);
    }
    
    
//    public void setContentProposals(AsystContentProposalProvider keyProposalProvider,
//                                    AsystContentProposalProvider valueProposalProvider) {
//        m_keyProposalProvider = keyProposalProvider;
//        m_valueProposalProvider = valueProposalProvider;
//    }


    @Override
    public void createSetCommentAction(int col,
                                       int row,
                                       String nlComment,
                                       String eolComment,
                                       KTable table) {
    }


    @Override
    public AbstractAction createSetContentAction(int col,
                                                 int row,
                                                 String value,
                                                 String nlComment,
                                                 String eolComment) {
        
        int dataCol = col - getFixedColumnCount();
        int dataRow = row - getFixedHeaderRowCount();

        value = value.trim();
        int idx = value.indexOf('\n');
        if (idx >= 0) {
            value = value.substring(idx);
        }
        
        // allow rows beyond current table size - they will be added
        if (dataRow < 0  ||  dataCol < COL_BODY_KEYS  ||  dataCol > COL_BODY_VALUES) {
            return null;
        }
        
        return createSetCellAction(dataCol, dataRow, value, nlComment, eolComment);
    }
}
