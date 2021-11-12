package si.isystem.itest.ui.comp;

import si.isystem.connect.CTestBase;
import si.isystem.exceptions.SEFormatter;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;

public class CommentSetListener  implements ICommentChangedListener {

    private CTestBase m_testBase;
    private ENodeId m_editorNodeId;
    
    
    public CommentSetListener(ENodeId editorNodeId) {
        m_editorNodeId = editorNodeId;
    }
    
    
    public void setTestBase(CTestBase testBase) {
        m_testBase = testBase;
    }



    @Override
    public void commentChanged(YamlScalar scalar) {
        if (m_testBase != null) {
            // can be null when control is cleared - see clearInput()
            SetSectionAction action = new SetSectionAction(m_testBase,
                                                           m_editorNodeId,
                                                           scalar.copy());
            action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
            action.addDataChangedEvent();

            if (action.isModified()) {
                try {
                    TestSpecificationModel.getActiveModel().execAction(action);
                    // no model verification is necessary for comments
                } catch (Exception ex) {
                    StatusView.getView().setDetailPaneText(StatusType.ERROR,
                                                           "Can not set test case data!\n" + 
                                                           SEFormatter.getInfoWithStackTrace(ex, 0));
                }
            }
        }
    }
}