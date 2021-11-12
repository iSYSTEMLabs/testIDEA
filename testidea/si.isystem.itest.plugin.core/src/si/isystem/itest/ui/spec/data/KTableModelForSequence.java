package si.isystem.itest.ui.spec.data;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.itest.common.ktableutils.KTableEditorModel;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.sequence.InsertToSequenceAction;
import si.isystem.itest.model.actions.sequence.RemoveFromSeqAction;
import si.isystem.itest.model.actions.sequence.SetSequenceItemAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.IModelWithComment;

public class KTableModelForSequence extends KTableEditorModel implements IModelWithComment {

    // column indices
    protected final static int COL_IDX = 1;
    
    protected CTestBase m_testBase;
    protected int m_sectionId;
    protected ENodeId m_nodeId;
    

    public KTableModelForSequence(int sectionId, ENodeId nodelId, String columnTitle) {
        super();

        m_sectionId = sectionId;
        m_nodeId = nodelId;
        
        super.setColumnTitles(new String[]{columnTitle});
    }

    
    @Override
    public TextIconsContent getBodyCellContent(int col, int row) {
        
        CSequenceAdapter sequence = new CSequenceAdapter(m_testBase, 
                                                         m_sectionId,
                                                         true);
        
        String text = sequence.getValue(row);
        m_editableCellContent.setText(text);

        // handle comments
        String nlComment = m_testBase.getCommentForSeqElement(m_sectionId,
                                                              row,
                                                              CommentType.NEW_LINE_COMMENT);
        String eolComment = m_testBase.getCommentForSeqElement(m_sectionId,
                                                               row,
                                                               CommentType.END_OF_LINE_COMMENT);

        doGetComment(m_editableCellContent, nlComment, eolComment);
//        m_editableCellContent.setNlComment(nlComment);
//        m_editableCellContent.setEolComment(eolComment);
//        
//        boolean isCommentSet = !(nlComment + eolComment).trim().isEmpty();
//        m_editableCellContent.setIcon(EIconPos.ETopLeft, 
//                                      getCommentIcon(text, isCommentSet), 
//                                      true);
//        
//        setTooltipFromComment(m_editableCellContent, nlComment, eolComment, isCommentSet);
        
        setBackground(m_editableCellContent, m_isEnabled);

        return m_editableCellContent;
    }
    

//    @Override
//    protected KTableCellEditor getBodyCellEditor(int itemInRow, 
//                                                 int itemIdx) {
//        
//        if (m_isEnabled) {
//            ContentProposalConfig cfg = new ContentProposalConfig(new String[0]);
//            UiUtils.setContentProposalsConfig(cfg);
//            cfg.setProposalProvider(m_contentProposalProvider);
//            cfg.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
//            
//            return new KTableCellEditorText2(cfg);
//        }
//        
//        return null;
//    }
    

    @Override
    protected void setBodyContentAt(int bodyCol, int bodyRow, String text) {
        
        AbstractAction action = createSetCellAction(bodyRow, text, null, null);
        
        notifyListeners(action, null, true);
    }


    private AbstractAction createSetCellAction(int bodyRow, String text, String nlComment, String eolComment) {
        YamlScalar value = YamlScalar.newListElement(m_sectionId, bodyRow);
        value.setValue(text);

        if (nlComment != null) {
            value.setNewLineComment(nlComment);
        }
        
        if (eolComment != null) {
            value.setEndOfLineComment(eolComment);
        }
        
        SetSequenceItemAction action = new SetSequenceItemAction(m_testBase, m_nodeId, value);
        
        action.addDataChangedEvent(m_nodeId, m_testBase);
        
        return action;
    }

    
    @Override
    public int getBodyRowCount() {
        if (m_testBase != null) {
            CSequenceAdapter expressions = new CSequenceAdapter(m_testBase, 
                                                                m_sectionId,
                                                                true);
            return (int)expressions.size();
        }
        return 0;
    }

    
    @Override
    public int getInitialColumnWidth(int column) {
        return 50 + 450 * column;
    }

    
    public void setData(CTestBase testBase) {
        m_testBase = testBase;
    }

    
    @Override
    public void addRow(int row) {
        int dataRow = row - NUM_HDR_ROWS;
        try {
            YamlScalar value = YamlScalar.newListElement(m_sectionId, dataRow);
            value.setValue("");
            
            AbstractAction action = new InsertToSequenceAction(m_testBase, value);
            
            notifyListeners(action, null, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore exceptions here - they will be reported later
            throw ex;
        }
    }


    @Override
    public void removeRow(int row) {
        int dataRow = row - NUM_HDR_ROWS;
        try {
            AbstractAction action = new RemoveFromSeqAction(m_testBase,
                                                            m_sectionId,
                                                            dataRow);
            notifyListeners(action, null, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore exceptions here - they will be reported later
            throw ex;
        }
    }


    @Override
    public void swapRows(int firstIdx, int secondIdx) {

        firstIdx -= NUM_HDR_ROWS;
        secondIdx -= NUM_HDR_ROWS;
        
        if (firstIdx == secondIdx  ||  firstIdx < 0  ||  secondIdx < 0) {
            return;
        }
        
        CSequenceAdapter seqAdapter = new CSequenceAdapter(m_testBase, 
                                                           m_sectionId, 
                                                           true);
        
        if (firstIdx >= seqAdapter.size()  ||  secondIdx >= seqAdapter.size()) {
            return;
        }
        
        GroupAction grpAction = new GroupAction("Swap items in CTestAssert: " + 
                                                firstIdx + ", " + secondIdx);
        
        YamlScalar firstVal = YamlScalar.newListElement(m_sectionId, firstIdx);
        firstVal.dataFromTestSpec(m_testBase);

        YamlScalar secondVal = YamlScalar.newListElement(m_sectionId, secondIdx);
        secondVal.dataFromTestSpec(m_testBase);

        firstVal.setIndex(secondIdx);
        secondVal.setIndex(firstIdx);

        AbstractAction action = 
                new SetSequenceItemAction(m_testBase, m_nodeId, firstVal);
        grpAction.add(action);

        action = new SetSequenceItemAction(m_testBase, m_nodeId, secondVal);
        grpAction.add(action);
        
        notifyListeners(grpAction, null, true);
    }

    
    @Override
    public void setCellComment(int col,
                               int row,
                               String nlComment,
                               String eolComment) {
        
        int dataRow = row - NUM_HDR_ROWS;
        YamlScalar value = YamlScalar.newListElement(m_sectionId, dataRow);
        value.dataFromTestSpec(m_testBase);
        value.setNewLineComment(nlComment);
        value.setEndOfLineComment(eolComment);

        AbstractAction action = 
                new SetSequenceItemAction(m_testBase, m_nodeId, value);
        
        notifyListeners(action, null, true);
    }
    
    
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
        
        // allow dataRow >= getBodyRowCount(), cells will be added
        if (dataRow < 0  ||  dataCol != 0) {
            return null;
        }

        return createSetCellAction(dataRow, value, nlComment, eolComment);
    }
    
}
