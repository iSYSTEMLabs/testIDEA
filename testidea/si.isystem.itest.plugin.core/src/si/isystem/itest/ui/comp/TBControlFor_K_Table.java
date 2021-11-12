package si.isystem.itest.ui.comp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import de.kupzog.ktable.ICommandListener;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellSelectionListener;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.renderers.TextIconsContent;
import si.isystem.connect.CTestBase;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.common.ktableutils.KTableEditorModel;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.SetCommentAction;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.ColorProvider;

public class TBControlFor_K_Table extends TBControl {

    private KTable m_ktable;
    private KTableEditorModel m_tableModel;
    
    
    /**
     * This class aggregates TableEditorPanel with decorations for YAML comments, 
     * and controls its appearance and contents depending on merge status.
     * Use this ctor when there is only one table per page.
     * 
     * @param parent
     *
     * @param tableEditor table with one column if sequence is edited, or 
     *                    two columns if user mapping is edited.
     *                    
     * @param sectionId id of mapping or list section in CTestBase
     *  
     * @param isCheckMergeStatus only tables, which contain all section data, for example HIL and
     *            options should check for merge status based on the model, tables like
     *            assignment table in stubs section should not check for merged status!
    public TBControlFor_K_Table(Composite parent,
                                int tableId,
                                int sectionId,
                                String migLayoutData,
                                ENodeId editorNodeId,
                                KTableModelForTB tableModel) {
        
        this(0, ktable, sectionId, editorNodeId, tableModel);
    }
     */
    
    
    /**
     * This class aggregates TableEditorPanel with decorations for YAML comments, 
     * and controls its appearance and contents depending on merge status.
     * Use this ctor when there is more than one table per page.
     * 
     * @param parent
     *
     * @param tableId id of table on the page, used when there is more than one table
     *                on the page, for example in persistent vars editor. 
     * 
     * @param tableEditor table with one column if sequence is edited, or 
     *                    two columns if user mapping is edited.
     *                    
     * @param sectionId id of mapping or list section in CTestBase
     *  
     * @param isCheckMergeStatus only tables, which contain all section data, for example HIL and
     *            options should check for merge status based on the model, tables like
     *            assignment table in stubs section should not check for merged status!
     */
    public TBControlFor_K_Table(Composite parent,
                                String tableId,    // SWTBot table ID
                                int sectionId,
                                String migLayoutData,
                                ENodeId editorNodeId,
                                KTableEditorModel tableModel) {
        super(editorNodeId);
        m_tableModel = tableModel;
        
        m_ktable = new KTable(parent, 
                              true, 
                              SWT.H_SCROLL | SWT.V_SCROLL | SWTX.EDIT_ON_KEY | 
                              SWTX.MARK_FOCUS_HEADERS | 
                              SWTX.FILL_WITH_LASTCOL | SWT.BORDER | SWT.MULTI);
        
        m_ktable.setData(SWTBotConstants.SWT_BOT_ID_KEY, tableId);
        
        m_ktable.setLayoutData(migLayoutData);

        createModelListener(tableModel);
        
        final ValueAndCommentEditor tagEditor = ValueAndCommentEditor.newKey(sectionId, 
                                                                             m_ktable);
        
        m_ktable.setModel(m_tableModel);
        
        tagEditor.setCommentChangedListener(new ICommentChangedListener() {

            @Override
            public void commentChanged(YamlScalar scalar) {
                
                SetCommentAction action = new SetCommentAction(m_testBase, 
                                                               m_nodeId, 
                                                               tagEditor.getScalarCopy());
                sendActionAndVerify(action, false);
            }
        });
        
        m_tableModel.addAllDefaultListeners(m_ktable);
        
        configure(m_ktable, tagEditor, null);
    }


    public KTable getKtable() {
        return m_ktable;
    }


    @Override
    public void setInput(CTestBase testBase, 
                         boolean isMerged) {
        super.setInput(testBase, isMerged);
        fillControls();
    }
    
    
    private KTableModel createModelListener(KTableEditorModel model) {

        model.addModelChangedListener(new IKTableModelChangedListener() {
            
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                
                if (action == null) {
                    return;
                }
                
                try {
                    sendActionAndVerify(action, false);
                    m_tagEditor.setEnabled(!isModelEmpty());
                } catch (Exception ex) {
                    // normally will not get here, as exception is caught in setActionAndVerify() above 
                    ex.printStackTrace();
                    SExceptionDialog.open(Activator.getShell(), "Can not change data!", ex);
                }
                if (isRedrawNeeded) {
                    m_ktable.redraw();
                }
            }
        });

        return model;
    }
    

    private void fillControls() {
        
        m_ktable.redraw();

        // done in base class: m_tagEditor.updateValueAndCommentFromTestBase(m_testBase);

        if (m_isMerged) {
            setEnabled(false);
            m_ktable.setBackground(ColorProvider.instance().getColor(ColorProvider.MERGED_BKG_COLOR));
        } else {
            setEnabled(true);
            m_ktable.setBackground(null);
        }
        m_tagEditor.getValueAndUpdateDecoration();
    }


    @Override
    public void setEnabled(boolean isEnabled) {
        if (m_isMerged) {
            m_ktable.setEnabled(true); // should be enabled, so that it receives
                                       // mouse events so that user can select
                                       // line in table to show test results in 
                                       // expressions and pre-conditions sections
            m_tableModel.setEnabled(false);
        } else {
            m_tableModel.setEnabled(true);
            m_ktable.setEnabled(isEnabled);
        }
        
        // disable comment editing always when editing control is disabled
        // If the control is enabled, but has no text, comment editing is still disabled
        m_tagEditor.setEnabled(isEnabled  &&  !isModelEmpty());
    }

    
    @Override
    public boolean isMerged() {
        return m_isMerged;
    }

    
    /**
     * Selects line in table.
     * @param lineNo number of row in model, without table header.
     */
    public void setSelection(int lineNo) {
        
        KTableModel tableModel = m_ktable.getModel();
        
        int numHeaderRows = tableModel.getFixedHeaderRowCount();
        int row = numHeaderRows + lineNo;
        
        m_ktable.setSelection(0, row, true);
    }

    
    /**
     * Selects a cell in table.
     * 
     * @param col body column number
     * @param row body row number
     */
    public void setSelection(int col, int row) {
        
        KTableModel tableModel = m_ktable.getModel();
        int numHeaderColumns = tableModel.getFixedHeaderColumnCount();
        int numHeaderRows = tableModel.getFixedHeaderRowCount();
        
        m_ktable.setSelection(numHeaderColumns + col, 
                              numHeaderRows + row, true);
    }

    
    private boolean isModelEmpty() {
        return m_tableModel.getRowCount() <= 2;  // header row + row with + sign
                                                 // are always present
    }

    
    public void addTableSelectionListener(KTableCellSelectionListener listener) {
        m_ktable.addCellSelectionListener(listener);
    }


    public Point[] getSelection() {
        return m_ktable.getCellSelection();
    }
    
    
    public TextIconsContent getContentAt(int col, int row) {
        Object content = m_tableModel.getContentAt(col, row);
        if (content instanceof TextIconsContent) {
            return (TextIconsContent)content;
        }
        return null;
    }


    public void addCommandListener(ICommandListener kTableCmdListener) {
        m_ktable.setCommandListener(kTableCmdListener);
    }
}
