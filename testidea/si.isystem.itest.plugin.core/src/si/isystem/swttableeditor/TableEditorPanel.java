package si.isystem.swttableeditor;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.TextCellEditorWithAutoCompletion;


/*
 * For advanced tables I've investigated: KTable, Agile Grid, and Nat Table.
 * The latest seems the only still active project with the best documentation,
 * so I decided for it. The next on the list was KTable, while Agile Grid has
 * almost no activity and small number of downloads. Later it turned out, that
 * Nat table has very complicated (broken) design. KTable is much simpler to 
 * used and modify source code, so it the ine used now. 
 */

/**
 * This class is implementation of table editor. Only string editing is 
 * currently supported. If some part of functionality is not sufficient, consider
 * modifications of TableEditor...Adapter classes.  
 * 
 * Example:
 * <pre>
 * m_localsSpecEditor = new TableEditorPanel(
 *             new String[]{"Type", "Variable Name", "Value"}, 
 *             new String[]{"Variable type, for example 'int', 'char *', 'MyStruct', ...",
 *                          "Variable name, for example 'counter', 'mode', ...",
 *                          "Value of variable. Expressions are allowed. for " +
 *                              "example '20', \"my text\" (for arrays and pointer), " +
 *                              "&&globalStruct, ..."}, 
 *             new int[]{33, 33, 34}, 
 *             null);
 *        
 * TableEditorModelAdapter model = createLocalsModel(testStep, m_localsSpecEditor);
 *        
 * Composite localsPanel = m_localsSpecEditor.createPartControl(m_editPanel);
 *        
 * m_localsSpecEditor.setProviders(new TableEditorContentProvider(), 
 *                                 new TableEditorLabelProvider(), 
 *                                 new TableEditorCellModifier(m_localsSpecEditor.getViewer()));
 *                                        
 * m_localsSpecEditor.setInput(model);
 * </pre>
 * 
 * where
 * 
 * <pre>
 * public TableEditorModelAdapter createLocalsModel(TableEditorPanel panel) {
 *
 *     TableEditorModelAdapter myModel = new TableEditorModelAdapter(true);
 *     for (int i = 0; i < 3; i++) {
 *         String columns[] = new String[]{"item 1", "item 2", "item 3"};
 *         myModel.add(panel.createRow(columns));
 *     }
 *     
 *     // The empty row at the end enables the user to add new items easily.
 *     myModel.add(panel.createEmptyRow());
 *     
 *     return myModel;
 * }
 *
 * </pre>
 * 
 * @author markok
 */
public class TableEditorPanel {

    static final String COLUMN_PROP_PREFIX = "Column_";
    private ITableEditorModel m_dataModel;
    private TableViewer m_tableViewer;
    private Button m_insertBtn;
    private Button m_deleteBtn;
    private Button m_upBtn;
    private Button m_downBtn;

    private int m_rowHeight = -1;
    private double m_rowScale = -1;

    private String []m_columnTitles;
    private String[] m_columnToolTips;
    private int[] m_columnWeights;
    private String[] m_columnProperties;
    private ITableEditorRowDialog m_addDialog;
    private boolean m_isEditable = false;
    private TextCellEditor[] m_textCellEditors;
    private String m_insertBtnText;
    private String m_insertBtnTooltop;
    private IContentProposalProvider[] m_autoCompleters;
    private ButtonListener m_buttonListener;
    private TableEditorLabelProvider []m_columnLabelProviders;
    private TableViewerColumn[] m_tableColumns;

    
    /**
     * 
     * 
     * @param autoCompleters if not null, its size must match the size of columnTitles array.
     */
    public TableEditorPanel(String []columnTitles,
                            String[] columnToolTips,
                            int[] columnWeights,
                            String[] columnProperties,
                            IContentProposalProvider[] autoCompleters) {
        this(columnTitles, columnToolTips, columnWeights, columnProperties, true);
        m_autoCompleters = autoCompleters;
    }

    
    /**
     * @param isEditable is true, fields in the table are editable as text fields
     */
    public TableEditorPanel(String []columnTitles,
                            String[] columnToolTips,
                            int[] columnWeights,
                            String[] columnProperties,
                            boolean isEditable) {
        this(columnTitles, columnToolTips, columnWeights, columnProperties);
        m_isEditable = isEditable;
    }

    
    /**
     * Creates editable table editor. All array parameters must have the same number of elements.
     * 
     * @param columnTitles column titles to be written in table header 
     * @param columnToolTips tool tips for each table column
     * @param columnWeights weights for column widths
     * @param columnProperties table column properties, not visible to the user
     * but important for internal operation. Duplicate strings are not allowed!
     * If null, auto properties are assigned. 
     */
    public TableEditorPanel(String []columnTitles,
                            String[] columnToolTips,
                            int[] columnWeights,
                            String[] columnProperties) {
       
        if (columnTitles.length != columnToolTips.length  ||
                columnTitles.length != columnWeights.length  ||
                (columnProperties != null &&  (columnTitles.length != columnProperties.length))) {
            throw new IllegalArgumentException("All array parameters must have the same size!");
        }
        
        m_columnTitles = columnTitles;
        m_columnToolTips = columnToolTips;
        m_columnWeights = columnWeights;
        m_columnProperties = columnProperties;
        
        if (m_columnProperties == null) {
            m_columnProperties = new String[columnTitles.length];
            for (int i = 0; i < columnTitles.length; i++) {
                m_columnProperties[i] = COLUMN_PROP_PREFIX + i;
            }
        }
    }
    
    
    public void setAddDialog(ITableEditorRowDialog addDialog) {
        m_addDialog = addDialog;
    }

    /**
     * Sets text of the insert button. By default it is 'Insert' if add dialog 
     * is not set (see setAddDialog()), or 'Add' if the dialog is set.
     * 
     * @param text test to display on the button, may not be null
     * @param tooltip tooltip text, may not be null
     */
    public void setInsertButtonText(String text, String tooltip) {
        m_insertBtnText = text;
        m_insertBtnTooltop = tooltip;
    }
    
    public void setRowHeight(int rowHeight) {
        m_rowHeight = rowHeight;
    }
   
    /** 
     * Sets multiplier for default row height. If row height was set by 
     * setRowHeight(), this method has no effect - absolute height takes precedence.
     * 
     * @param rowScale number used to multiply default row height
     */
    public void setRowScale(float rowScale) {
        m_rowScale = rowScale;
    }
   
    
    /**
     * Sets listener, which is called when the user activates one of the
     * editing buttons, for example <code>Up</code> or <code>Down</code> button. 
     */
    public void setButtonListener(ButtonListener listener) {
        m_buttonListener = listener;
    }
    
    
    /**
     * Utility method to save caller from maintaining column properties for
     * model creation.
     *  
     * @param columns strings for columns. Array size must match 
     * 
     * @return table row with column properties set as defined in ctor.
     */
    public TableEditorRowAdapter createRow(String []columns) {
        return new TableEditorRowAdapter(columns, m_columnProperties);
    }
    
    /**
     * Utility method to get empty row with column properties set.
     */
    public TableEditorRowAdapter createEmptyRow() {
        return new TableEditorRowAdapter(m_columnProperties);
    }

    
    public Composite createPartControl(Composite parent) {
        return createPartControl(parent, null, null, SWT.NONE);
    }

    
    public Composite createPartControl(Composite parent, 
                                       SelectionListener []columnSelectionListeners,
                                       ISelectionChangedListener selectionChangedListener,
                                       int style) {
        return createPartControl(parent, columnSelectionListeners, selectionChangedListener, style, true);
    }
    
    /**
     * Creates Composite, which contains table editor and buttons. Layout data
     * for the main panel should be set by caller.
     * 
     * @param parent parent composite
     * 
     * @param columnSelectionListeners listeners which get called when the user 
     *        clicks column header. If there are less listeners
     *        than columns in the table, only the first columns get selection listener 
     *        assigned. This parameter may be null. Example:
     *        <pre>
     *        SelectionListener [] selectionListeners = new SelectionListener [] {
     *            new SelectionAdapter() {
     *                @Override
     *                public void widgetSelected(SelectionEvent e) {
     *                     System.err.println(e);
     *                }
     *            }
     *        }; 
     *        </pre>
     *        
     * @param selectionChangedListener listener, which gets called when selection
     *                                 in the table viewer changes. May be null.
     * 
     * @param style one of SWT.* style constants, for example SWT.NONE, SWT.BORDER, ...
     *
     * @param isCreateModificationButtons if true, buttons (Insert/Add, Delete, Up, and Down are created)
     * 
     * @return new composite containing table editor 
     */
    public Composite createPartControl(Composite parent, 
                                       SelectionListener []columnSelectionListeners,
                                       ISelectionChangedListener selectionChangedListener,
                                       int style, boolean isCreateModificationButtons) {
        
        Composite tableEditorPanel = new Composite(parent, style);

        tableEditorPanel.setLayout(new MigLayout("flowy, fill, insets 0 7 0 5"));
        
        Composite tablePanel = new Composite(tableEditorPanel, SWT.NONE);

        m_tableViewer = new TableViewer(tablePanel, 
                                        SWT.FULL_SELECTION | // to select whole row, essential when each cell is editable 
                                        SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

        if (selectionChangedListener != null) {
            m_tableViewer.addSelectionChangedListener(selectionChangedListener);
        }
        
        // 'wmin 0' is very important in SWT, because SWT does 
        // not have notion of min size. If not specified,
        // table resizes when window is resized and pushes part of buttons over the window edge.
        // If 'hmin 0' is not specified, then scrollbars disappear on panel resize.
        // m_tableViewer.getControl().setLayoutData("push, grow, wrap, wmin 50, hmin 50");
        // hmax 95% is required if we want the table to shrink and display scrollers on resize!
        // Don't know why it has to be 95%. 99% does not work at all???  
        // It seems to be the issue of Mig layout, so it may make sense to rewrite this
        // class with FlowLayout?
        // Specifies "gapright 10" to make some space for control decorations
        tablePanel.setLayoutData("push, grow, gapright 10, wrap, wmin 0, hmin 0, hmax 95%");
        
        final Table table = m_tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        // increase row height to be more readable
        if (m_rowHeight >= 0) {
            table.addListener(SWT.MeasureItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    event.height = m_rowHeight;
                    // int clientWidth = table.getClientArea().width;
                    // event.width = clientWidth * 2;
                }
            }); 
        } else if (m_rowScale >= 0) {
            table.addListener(SWT.MeasureItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    event.height = (int)(event.gc.getFontMetrics().getHeight() * m_rowScale);
                    // int clientWidth = table.getClientArea().width;
                    // event.width = clientWidth * 2;
                }
            }); 
        }
        
        ColumnViewerToolTipSupport.enableFor(m_tableViewer, ToolTip.NO_RECREATE);
        TableColumnLayout layout = new TableColumnLayout();
        m_tableColumns = new TableViewerColumn[getNoOfColumns()];
        int idx = 0;
        for (String title : m_columnTitles) {
            addTableColumn(m_tableViewer, title, layout, columnSelectionListeners, idx);
            idx++;
        }
        tablePanel.setLayout(layout);

        m_tableViewer.setColumnProperties(m_columnProperties);
        
        if (m_isEditable) {
            int numColumns = m_columnTitles.length;
            m_textCellEditors = new TextCellEditor[numColumns];
            for (int i = 0; i < numColumns; i++) {
                if (m_autoCompleters != null  &&  m_autoCompleters[i] != null) {
                    m_textCellEditors[i] = new TextCellEditorWithAutoCompletion(table, 
                                                                                m_autoCompleters[i], 
                                                                                null, 
                                                                                null);
                } else {
                    m_textCellEditors[i] = new TextCellEditor(table);
                }
            }
            
            m_tableViewer.setCellEditors(m_textCellEditors);
        
            ColumnViewerEditorActivationStrategy editorActivationStrategy = new
            ColumnViewerEditorActivationStrategy(m_tableViewer) { 
                // this method is required because of F2 editing activation key
                @Override
                protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
                    return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL || 
                    event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION || 
                    (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && 
                            (event.keyCode == SWT.CR  ||  event.keyCode == SWT.F2)) || 
                            event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC; }
            };

            // editorActivationStrategy.setEnableEditorActivationWithKeyboard(true);

            // Without focus highlighter, keyboard events will not be delivered to
            // ColumnViewerEditorActivationStragety#isEditorActivationEvent(...) (see above)
            FocusCellHighlighter focusCellHighlighter = new FocusCellOwnerDrawHighlighter(m_tableViewer);

            TableViewerFocusCellManager focusCellManager = 
                new TableViewerFocusCellManager(m_tableViewer, focusCellHighlighter);         

            TableViewerEditor.create(m_tableViewer, 
                                     focusCellManager,
                                     editorActivationStrategy, 
                                     ColumnViewerEditor.TABBING_HORIZONTAL |
                                     ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | 
                                     ColumnViewerEditor.KEYBOARD_ACTIVATION); 
        }
        
       
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL) {
                    deleteSelectedRow();
                } else if (e.keyCode == SWT.INSERT) {
                    insertRow();
                } else if (e.keyCode == SWT.ARROW_UP  && (e.stateMask & SWT.SHIFT) != 0) {
                    moveSelectedRowUp();
                    e.doit = false; // prevent handling of up arrow by TableViewer
                } else if (e.keyCode == SWT.ARROW_DOWN  && (e.stateMask & SWT.SHIFT) != 0) {
                    moveSelectedRowDown();
                    e.doit = false; // prevent handling of down arrow by TableViewer
                }
                
            }
        });
        
        if (isCreateModificationButtons) {
            KGUIBuilder builder = new KGUIBuilder(tableEditorPanel);

            if (m_insertBtnText == null) {
                if (m_addDialog == null) {
                    m_insertBtnText = "Insert";
                    m_insertBtnTooltop = "Inserts new row above the current one.";
                } else {
                    m_insertBtnText = "Add";
                    m_insertBtnTooltop = "Adds new row.";
                }
            }

            m_insertBtn = builder.button(m_insertBtnText, "growx, split");
            m_insertBtn.setToolTipText(m_insertBtnTooltop + "   <Insert>");

            m_deleteBtn = builder.button("Delete", "growx");
            m_deleteBtn.setToolTipText("Deletes the current row.   <Delete>");

            m_upBtn = builder.button("Up", "growx");
            m_upBtn.setToolTipText("Moves the current row up.   <SHIFT + Up>");

            m_downBtn = builder.button("Down", "growx");
            m_downBtn.setToolTipText("Moves the current row down.    <SHIFT + Down>");

            m_insertBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    try {
                        insertRow();
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), "Can not insert row!", ex);
                    }
                }
            });

            m_deleteBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    try {
                        deleteSelectedRow();
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), "Can not delete row!", ex);
                    }
                }
            });

            m_upBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    try {
                        moveSelectedRowUp();
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), "Can not move row up!", ex);
                    }
                }
            });

            m_downBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    try {
                        moveSelectedRowDown();
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), "Can not move row down!", ex);
                    }
                }
            });
        }
        
        return tableEditorPanel;
    }


    /*
     * Adds column to the table. It is important that parameter idx strats with
     * 0 and is increased by one for each next column!
     */
    private void addTableColumn(TableViewer tableViewer, 
                                String title, 
                                TableColumnLayout layout, 
                                SelectionListener[] columnSelectionListeners,
                                int idx) {

        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
        m_tableColumns[idx] = column;
        
        // if label providers have already been set, apply them now
        if (m_columnLabelProviders != null  &&  idx < m_columnLabelProviders.length) {
            column.setLabelProvider(m_columnLabelProviders[idx]);
        }
        
        // set column header text and tooltip
        column.getColumn().setText(title);
        column.getColumn().setToolTipText(m_columnToolTips[idx]);
        
        // these tooltips do not work for table columns
        // use column.getColumn().getParent() to set tooltip on whole table
        // use column.getViewer().getControl() to set tooltip on whole table
        // Control toolTipControl = column.getViewer().getControl();
        // DefaultToolTip tooltip = new DefaultToolTip(toolTipControl); 
        // Tooltip on a table is annoying and is not column specific.
        // tooltip.setText("Table tooltip" /* m_columnToolTips[idx] */);
        // tooltip.setHideDelay(UiUtils.DEFAULT_TOOLTIP_DELAY);
        
        if (columnSelectionListeners != null) {
            column.getColumn().addSelectionListener(columnSelectionListeners[idx]);
        }
        layout.setColumnData(column.getColumn(), new ColumnWeightData(m_columnWeights[idx]));
    }


    public TableViewer getViewer() {
        return m_tableViewer;
    }

    
    public void setProviders(IStructuredContentProvider contentProvider,
                             TableEditorLabelProvider labelProviders[],
                             ICellModifier cellModifier) {
        
        m_tableViewer.setContentProvider(contentProvider);

        m_columnLabelProviders = labelProviders;
        if (labelProviders != null  &&  m_tableColumns != null) {
            for (int i = 0; i < labelProviders.length; i++) {
                m_tableColumns[i].setLabelProvider(m_columnLabelProviders[i]);
            } 
        }
        
        
        if (m_isEditable) {
            m_tableViewer.setCellModifier(cellModifier);
        }
    }
    
    /*
    public void setColumEditStatus(int idx, boolean isEditable) {
         EditingSupport es = new EditingSupport() {
        }; 
        m_tableColumns[idx].setEditingSupport(null);
    } */
    
    
    public void setInput(ITableEditorModel model) {
        m_dataModel = model;
        m_tableViewer.setInput(model);
    }
    
    
    public void setSelection(int rowIndex) {
        if (rowIndex >= m_dataModel.size()) {
            rowIndex = m_dataModel.size() - 1;
        }
        if (rowIndex < 0) {
            return;
        }
        m_tableViewer.getTable().setFocus();
        TableItem item = m_tableViewer.getTable().getItem(rowIndex);
        m_tableViewer.setSelection(new StructuredSelection(item.getData()), true);
        m_tableViewer.getTable().showSelection();
    }
    

    private void insertRow() {

        if (m_addDialog != null) {
            
            while (true) { // repeat as long as there is an error or user canceled the dialog
                try {
                    ITableEditorRow newRow = createEmptyRow();
                    
                    if (!getNewRow(newRow)) { // user canceled the dialog 
                        return;
                    }
                    
                    boolean isLastRowEmpty = false;
                    if (m_dataModel.size() > 0) {
                        if (m_dataModel.get(m_dataModel.size() - 1).isEmpty()) {
                            isLastRowEmpty  = true;
                        }
                    } 

                    // if the last row is empty, insert new entry before it. This
                    // is useful if table is editable and the last empty row
                    // is automatically added.
                    if (!isLastRowEmpty) {
                        m_dataModel.insert(-1, newRow);
                        m_tableViewer.refresh();
                        setSelection(m_dataModel.size());
                    } else {
                        m_dataModel.insert(/* m_dataModel.size()*/ -1, newRow);
                        m_tableViewer.refresh();
                        setSelection(m_dataModel.size() - 2);
                    }
                    
                    break;
                } catch (Exception ex) {
                    SExceptionDialog.open(getViewer().getControl().getShell(), 
                                          "Invalid item", 
                                          ex);
                }
            }
            
            return;
        }
        
        IStructuredSelection selection = (IStructuredSelection)m_tableViewer.getSelection();
        if (selection.size() == 0) {
            //m_tableViewer.getTable().setSelection(m_localsModel.size() - 1);
            MessageDialog.openInformation(getViewer().getControl().getShell(), 
                                          "Insertion info", 
                                          "Select the row, where you want to make insertion.");
        } else {
            ITableEditorRow row = (ITableEditorRow)selection.getFirstElement();
            int selectedIdx = m_dataModel.find(row);
            if (selectedIdx >= 0) {
                if (selectedIdx < m_dataModel.size() - 1) {
                    ITableEditorRow newRow = row.createEmpty();
                    m_dataModel.insert(selectedIdx, newRow);
                    m_tableViewer.refresh();

                    // puts Table and TableViewer selections in sync. If
                    // arg is for example 'selectedIdx' editing (SHIFT-TAB, F2)
                    // edits one cell below the shown cell!
                    m_tableViewer.getTable().setFocus();
                    TableItem item = m_tableViewer.getTable().getItem(selectedIdx + 1);
                    m_tableViewer.setSelection(new StructuredSelection(item.getData()));
                    m_tableViewer.getTable().setSelection(selectedIdx + 1);
                    
                } /* else {
                    // do not report error - the user has empty row available, and the message is confusing 
                     * MessageDialog.openInformation(getViewer().getControl().getShell(), 
                                                  "Insertion info", 
                                                  "The first empty row is already editable. Click in the first empty cell."); 
                } */
            } else {
                MessageDialog.openError(getViewer().getControl().getShell(), 
                                        "Model Error", 
                                        "The sected row was not found in the model!");
            }
        }
    }

    
    public int getNoOfColumns() {
        return m_columnTitles.length;
    }
    
    
    /**
     * 
     * @param row
     * @return true, if user clicked the OK button, false otherwise
     */
    private boolean getNewRow(ITableEditorRow row) {
        
        if (m_addDialog.show()) {
            // it is API user's responsibility that size of table row and dialog match!
            String []data = m_addDialog.getData();
            
            int idx = 0;
            for (String item : data) {
                row.setItem(idx, item.trim());
                idx++;
            }
            return true;
        }
        
        return false;
    }


    private void deleteSelectedRow() {
        IStructuredSelection selection = (IStructuredSelection)m_tableViewer.getSelection();
        if (selection.size() > 0) {
            ITableEditorRow row = (ITableEditorRow)selection.getFirstElement();
            int selectedIdx = m_dataModel.find(row);
            if (selectedIdx >= 0) {
                setSelection(selectedIdx + 1);
                m_dataModel.remove(selectedIdx);
                m_tableViewer.refresh();
            }
        }
    }
   
    
    private void moveSelectedRowUp() {
        IStructuredSelection selection = (IStructuredSelection)m_tableViewer.getSelection();
        if (selection.size() > 0) {
            ITableEditorRow row = (ITableEditorRow)selection.getFirstElement();
            int selectedIdx = m_dataModel.find(row);
            
            int maxIdx = m_dataModel.size();
            if (m_dataModel.isAutoAddLastEmptyRow()) {
                maxIdx--; // do not move the last (empty) row
            }
            
            // do not move the first row up
            if (selectedIdx > 0  &&  selectedIdx < maxIdx) {
                if (m_buttonListener!= null  && !m_buttonListener.upButtonPressed(selectedIdx)) {
                    return;  // user's listener may cancel the move
                }
                m_dataModel.swap(selectedIdx - 1, selectedIdx);
                // m_localsModel.modelChanged(null);
                m_tableViewer.refresh();
                m_tableViewer.getTable().setSelection(selectedIdx - 1);
            }
        }
    }

    
    private void moveSelectedRowDown() {
        IStructuredSelection selection = (IStructuredSelection)m_tableViewer.getSelection();
        if (selection.size() > 0) {
            ITableEditorRow row = (ITableEditorRow)selection.getFirstElement();
            int selectedIdx = m_dataModel.find(row);

            int maxIdx = m_dataModel.size() - 1; // do not move the last row
            if (m_dataModel.isAutoAddLastEmptyRow()) {
                maxIdx--; // do not move the row before the last (empty) row
            }
             
            if (selectedIdx >= 0  &&  selectedIdx < maxIdx) {
                if (m_buttonListener!= null  && !m_buttonListener.downButtonPressed(selectedIdx)) {
                    return;  // user's listener may cancel the move
                }
                m_dataModel.swap(selectedIdx + 1, selectedIdx);
                // m_localsModel.modelChanged(null);
                m_tableViewer.refresh();
                m_tableViewer.getTable().setSelection(selectedIdx + 1);
            }
        }
    }

    
    /** Returns index of the selected item or -1 if there is no item selected. */
    public int getSelectionIndex() {
        int selectedIdx = -1;
        IStructuredSelection selection = (IStructuredSelection)m_tableViewer.getSelection();
        if (selection.size() > 0) {
            ITableEditorRow row = (ITableEditorRow)selection.getFirstElement();
            selectedIdx = m_dataModel.find(row);
        }
        return selectedIdx;
    }

    /** 
     * Returns selected row or null if no row is selected. If there is more than
     * one row selected, the first one is returned. 
     */
    public ITableEditorRow getSelectedRow() {
        IStructuredSelection selection = (IStructuredSelection)m_tableViewer.getSelection();
        if (selection.size() > 0) {
            ITableEditorRow row = (ITableEditorRow)selection.getFirstElement();
            return row;
        }
        return null;
    }
    
    public void setEnabled(boolean enabled) {
        m_tableViewer.getTable().setEnabled(enabled);
        
        if (m_insertBtn != null) {
            m_insertBtn.setEnabled(enabled);
            m_deleteBtn.setEnabled(enabled);
            m_upBtn.setEnabled(enabled);
            m_downBtn.setEnabled(enabled);
        }        
    }

    
    public void setBackground(Color color) {
        m_tableViewer.getTable().setBackground(color);        
    }
    
    
    public void setColumnBackground(int columnIndex, Color color) {
        Table table = m_tableViewer.getTable();
        int rowCount = table.getItemCount();
        
        for (int i = 0; i < rowCount; i++) {
            TableItem row = table.getItem(i);
            row.setBackground(columnIndex, color);
        }
    }

    
    public void setRowBackground(Color color, int idx) {
        TableItem tableRow = m_tableViewer.getTable().getItem(idx);
        tableRow.setBackground(color);
    }
    
        
    /** This listener is called, when user moves between cells or adds/deletes them. */
    public void addTableKeyListener(KeyListener listener) {
        m_tableViewer.getTable().addKeyListener(listener);
    }

    /** Adds key listener for cell editor. It is called, when the user presses keys
     * while editing the test in a cell, including arrow keys and Esc. It is NOT
     * called hen user moves between cells in a table or deletes/inserts cells. 
     * @param columnIdx
     * @param listener
     */
    public void addCellEditorKeyListeners(int columnIdx, KeyListener listener) {
        m_textCellEditors[columnIdx].getControl().addKeyListener(listener);
    }
    
    /**
     * Adds focus listener to a cell editor.
     * @param columnIdx
     * @param listener
     */
    public void addCellEditorFocusListener(int columnIdx, FocusListener listener) {
        m_textCellEditors[columnIdx].getControl().addFocusListener(listener);
    }
    
    /**
     * Adds focut listener for a table. It is called when the table looses focus,
     * which happens also in the case when user clicks the cell to edit its 
     * contents.
     *   
     * @param listener
     */
    public void addFocusListener(FocusListener listener) {
        m_tableViewer.getTable().addFocusListener(listener);
    }

    
    public void addTableTraversalListener(TraverseListener listener) {
        m_tableViewer.getTable().addTraverseListener(listener);
    }
    
    
    public void addCellTraversalListener(int columnIdx, TraverseListener listener) {
        m_textCellEditors[columnIdx].getControl().addTraverseListener(listener);
    }
    
    public void addTablePaintListener(PaintListener listener) {
        m_tableViewer.getTable().addPaintListener(listener);
    }
    
    
    public void addCellPaintListener(int columnIdx, PaintListener listener) {
        m_textCellEditors[columnIdx].getControl().addPaintListener(listener);
    }
    
    
    public void addDoubleClickListener(IDoubleClickListener doubleClickListener) {
        m_tableViewer.addDoubleClickListener(doubleClickListener);
    }


    public void addSelectionListener(SelectionListener listener) {
        m_tableViewer.getTable().addSelectionListener(listener);
    }
    

    /**
     * Adds listener of mouse movements. This way you can trigger actions when
     * mouse cursor is moved over specific table cell.
     * @param listener
     */
    public void addMouseTrackListener(MouseTrackListener listener) {
        m_tableViewer.getTable().addMouseTrackListener(listener);
    }
    
    
    public void addMouseMoveListener(MouseMoveListener listener) {
        m_tableViewer.getTable().addMouseMoveListener(listener);
    }
}
