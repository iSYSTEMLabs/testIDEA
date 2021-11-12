package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestBase;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.comp.DynamicTable;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.TestBaseListTable;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This class opens dialog with editable TestBaseListTable.
 * 
 * @author markok
 *
 */
public class DynamicTableDialog extends Dialog {

    private String m_titleText;
    private int m_sectionId;
    private TestSpecificationModel m_model;
    private CTestBase m_testBase;
    private static IDialogSettings m_dialogBounds = new DialogSettings("dynamicTableDialog");
    private TestBaseListTable m_stepsTable;    
    private Button m_undoBtn;
    private Button m_redoBtn;
    private int m_numActions = 0;
    private int m_actionIdx = -1;
    private ActionExecutioner m_actionExecutioner = new ActionExecutioner();
    
    Listener m_undoRedoListener = new Listener() {

        private int CTRL_Z = 26;
        private int CTRL_Y = 25;
        
        @Override
        public void handleEvent(Event event) {
            // TODO read currently configured shortcuts for undo-redo
            // System.out.println("undo " + (int)(event.character) + " " + (event.stateMask & SWT.CTRL));
            if (event.character == CTRL_Z) {
                m_actionExecutioner.undo();
            } else if (event.character == CTRL_Y) {
                m_actionExecutioner.redo();
            }
        }
    };
    

    /**
     * Takes care that only actions executed in the dialog can be undone/redone.
     */
    class ActionExecutioner implements IActionExecutioner {

        @Override
        public void execAction(AbstractAction action) {
            m_model.execAction(action);
            m_stepsTable.refresh();
            m_actionIdx++;
            m_numActions = m_actionIdx + 1;
            setEnabledState();
        }
        
        void undo() {
            m_model.undo();
            m_stepsTable.refresh();
            m_actionIdx--;
            setEnabledState();
        }
        
        void redo() {
            m_model.redo();
            m_stepsTable.refresh();
            m_actionIdx++;
            setEnabledState();
        }

        private void setEnabledState() {
            m_undoBtn.setEnabled(m_model.isUndoable()  &&  m_actionIdx >= 0);
            m_redoBtn.setEnabled(m_model.isRedoable()  &&  (m_actionIdx + 1) < m_numActions);
        }
    }    

    
    public DynamicTableDialog(Shell parentShell, 
                              String title,
                              TestSpecificationModel model,
                              CTestBase testBase,
                              int section) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_titleText = title;
        m_model = model;
        m_testBase = testBase;
        m_sectionId = section;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);

        getShell().getDisplay().addFilter(SWT.KeyDown, m_undoRedoListener);
        // parent.addKeyListener(listener);
        
        composite.getShell().setText(m_titleText);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.heightHint = 600;  // sets initial dialog size
        gridData.widthHint = 800;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow][min!][min!]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        m_stepsTable = new TestBaseListTable(null, false);
        
        Control stepsTableControl = m_stepsTable.createControl(builder.getParent(), 
                                                               m_testBase,
                                                               m_sectionId,
                                                               ENodeId.TEST_POINT_NODE,
                                                               null);
        m_stepsTable.setActionExecutioner(m_actionExecutioner);
        m_stepsTable.setInput(m_testBase, m_sectionId);
        
        
        m_stepsTable.getKModel().adjustRowHeights();
        
        stepsTableControl.setLayoutData("wmin 0, wrap, grow");
        m_stepsTable.setTooltip(DynamicTable.STEPS_TABLE_TOOLTIP);
        
        return composite;
    }
    
    private final int UNDO_ID = 9010;
    private final int REDO_ID = 9011;
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        m_undoBtn = createButton(parent, UNDO_ID, "", false);
        m_undoBtn.setImage(IconProvider.INSTANCE.getIcon(EIconId.EUndo));
        m_undoBtn.setToolTipText("Undo");
        
        m_redoBtn = createButton(parent, REDO_ID, "", false);
        m_redoBtn.setImage(IconProvider.INSTANCE.getIcon(EIconId.ERedo));
        m_redoBtn.setToolTipText("Redo");
        
        // undo/redo are not enabled until some actions are done in the dialog
        m_undoBtn.setEnabled(false);
        m_redoBtn.setEnabled(false);
        
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL,
                     true);
        // createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL,
        //              false);
    }
    
    
    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case UNDO_ID:
            m_actionExecutioner.undo();
            break;
            
        case REDO_ID:
            m_actionExecutioner.redo();
            break;
        default:
            super.buttonPressed(buttonId);
        }
    }
    

    @Override
    public boolean close() {
        
        getShell().getDisplay().removeFilter(SWT.KeyDown, m_undoRedoListener);
        return super.close();
    }
    
    
    @Override 
    protected IDialogSettings getDialogBoundsSettings() {
        return m_dialogBounds;
    }
}


