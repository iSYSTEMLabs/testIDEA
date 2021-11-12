package si.isystem.itest.dialogs;

import java.util.ArrayList;
import java.util.List;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.FilterConfigPage;
import si.isystem.itest.common.FilterConfigPage.ContainerType;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.swttableeditor.ITableEditorModel;
import si.isystem.swttableeditor.ITableEditorModelChangedListener;
import si.isystem.swttableeditor.ITableEditorModelChangedListener.ChangeType;
import si.isystem.swttableeditor.ITableEditorRow;
import si.isystem.swttableeditor.ITextFieldVerifier;
import si.isystem.swttableeditor.TableEditorCellModifier;
import si.isystem.swttableeditor.TableEditorContentProvider;
import si.isystem.swttableeditor.TableEditorLabelProvider;
import si.isystem.swttableeditor.TableEditorPanel;
import si.isystem.swttableeditor.TableEditorRowAdapter;
import si.isystem.swttableeditor.TableEditorRowDialog;
import si.isystem.ui.utils.KGUIBuilder;


/**
 * This class implements dialog for entering criteria for filtering of tests for 
 * execution.
 * <p>
 * 
 * Example:
 * <pre>
 * TestExecutionFilterDialog dlg = new TestExecutionFilterDialog(getShell());
 * if (dlg.show()) {
 *     TestExecutionFilter filter = new TestExecutionFilterDialog(dlg.getExpression());
 * }
 * </pre>
 * 
 * @author markok
 *
 */
public class TestExecutionFilterDialog extends Dialog {

    private CTestFilter m_currentFilter;
    private static int s_lastSelectedFilterIndex = 0;
    private static String s_lastSelectedFilterId = null;
    
    private TableEditorPanel m_filterTableEditor;
    private CTestBaseList m_testFilters;
    private FilterConfigPage m_filterConfigPage;
    

    /**
     * Creates dialog.
     * 
     * @param parentShell parent shell
     */
    public TestExecutionFilterDialog(Shell parentShell, CTestBaseList testFilters) {
        super(parentShell);
        
        // make dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);

        m_testFilters = testFilters;
    }

    
    @Override
    public boolean isResizable() {
        return true;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Test filters");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.heightHint = 610;  // sets initial dialog size
        gridData.widthHint = 800;
        mainDlgPanel.setLayoutData(gridData);

        
        mainDlgPanel.setLayout(new MigLayout("fill"));
        
        SashForm sash = new SashForm(mainDlgPanel, SWT.HORIZONTAL);
        sash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        
        sash.setLayoutData("grow, wmin 0");  // IMPORTANT - set wmin on SashForm!
        
        createFiltersListPanel(sash);

        // int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        m_filterConfigPage = new FilterConfigPage(ContainerType.E_TREE, false);
        
        m_filterConfigPage.createMainPanel(sash);
        sash.setWeights(new int[]{25, 75});
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        builder.separator("newline, spanx 4, growx, gaptop 20", SWT.HORIZONTAL);

        m_currentFilter = null;
        // Try to select the last selected filter. If this is not possible, because
        // the model has changed since last invocation of this dialog, for example
        // because the user loaded another file, select the first filter.
        if (m_testFilters.size() > s_lastSelectedFilterIndex  &&  s_lastSelectedFilterId != null) {
            CTestFilter filter = CTestFilter.cast(m_testFilters.get(s_lastSelectedFilterIndex));
            String testFilterId = filter.getFilterId();
            if (testFilterId.equals(s_lastSelectedFilterId)) {
                m_currentFilter = filter;
                fillControls(m_currentFilter);
                m_filterTableEditor.setSelection(s_lastSelectedFilterIndex);
            }
        } else if (m_currentFilter == null  &&  m_testFilters.size() > 0) {
            // if filter was not selected above, try to select the first one
            m_currentFilter = CTestFilter.cast(m_testFilters.get(0));
            fillControls(m_currentFilter);
            m_filterTableEditor.setSelection(0);
        } else {
            fillControls(null);
        }
        
        return composite;
    }
    
    
    private Composite createFiltersListPanel(Composite parent) {

        m_filterTableEditor = 
            new TableEditorPanel(new String[]{"Filter ID"}, 
                                 new String[]{
                    "ID of the filter, which is used to select test cases for execution.\n" +
                    "Select filter name to view and edit filtering critetia.\n" +
                    "Use buttons to add/delete functions."}, 
                                 new int[]{100}, 
                                 null, // use default column ids
                                 false); // non-editable table
        
        TableEditorRowDialog addDlg = 
            new TableEditorRowDialog(parent.getShell(),
                                     "Filter ID",
                                     new String[]{"Filter ID:"}, 
                                     new String[]{"ID of the filter, which can be used for " +
                                     		      "filtering of test cases."});
        
        ITextFieldVerifier verifier = new ITextFieldVerifier() {
            
            @Override
            public String verify(String[] data) {
                if (data[0].trim().length() == 0) {
                    return "Function name must not be empty!";
                }
                return null;
            }

            @Override
            public String format(String[] data) {
                return null;
            }

        };
        
        addDlg.setVerifier(verifier);
        
        m_filterTableEditor.setAddDialog(addDlg);
        
        ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                try {
                    int selectedIdx = m_filterTableEditor.getSelectionIndex();
                    if (selectedIdx >= 0) {
                        m_currentFilter = CTestFilter.cast(m_testFilters.get(selectedIdx));
                    } else {
                        m_currentFilter = null;
                    }
                    fillControls(m_currentFilter);  // if filterId == "", controls are emptied
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), "Can not show selected filter!" , ex);
                }
            }
        }; 
        
        Composite filtersTablePanel = m_filterTableEditor.createPartControl(parent, 
                                                                            null,
                                                                            selectionChangedListener,
                                                                            SWT.NONE);
        filtersTablePanel.setLayoutData("h 100%, grow, wmin 100");
        
        m_filterTableEditor.setProviders(new TableEditorContentProvider(), 
                                         TableEditorLabelProvider.createProviders(1), 
                                         new TableEditorCellModifier(m_filterTableEditor.getViewer()));

        ITableEditorModel model = new FilterListTableModel(m_filterTableEditor, m_testFilters); 
        
        m_filterTableEditor.setInput(model);

        return filtersTablePanel;
        
    }

    
    private void fillControls(CTestFilter testFilter) {

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            m_filterConfigPage.setInput(model.getRootTestSpecification(), testFilter, null);
        } else {
            // it seems no itest editor is selected - provide empty test spec.
            m_filterConfigPage.setInput(new CTestSpecification(), testFilter, null);
        }
        
        m_filterConfigPage.refreshGlobals();
        m_filterConfigPage.fillControls();
    }


    @Override
    protected void okPressed() {
        s_lastSelectedFilterIndex = m_filterTableEditor.getSelectionIndex();
        if (s_lastSelectedFilterIndex < 0) {  // if there is no item selected
            s_lastSelectedFilterIndex = 0;
        }
        if (m_currentFilter != null) {
            s_lastSelectedFilterId = m_currentFilter.getFilterId();
        } else {
            // if there is no item selected
            s_lastSelectedFilterId = null;
        }
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }


    public CTestFilter getSelectedFilter() {
        return m_currentFilter;
    }
}



class FilterListTableModel implements ITableEditorModel {

    private TableEditorPanel m_tableEditor;
    private CTestBaseList m_testFilters;

    
    public FilterListTableModel(TableEditorPanel editor, CTestBaseList testFilters) {
        m_tableEditor = editor;
        m_testFilters = testFilters;
    }
    

    @Override
    public ITableEditorModel copy() {
        throw new SIllegalStateException("Filter table model does not support copy method!");
    }
        
    
    @Override
    public void addModelChangedListener(ITableEditorModelChangedListener listener) {
        // implement when needed
        SExceptionDialog.open(Activator.getShell(), "Listeners are not supported by this model!", new Exception());
    }

    @Override
    public int size() {
        return (int)m_testFilters.size();
    }

    @Override
    public void insert(int selectedIdx, ITableEditorRow row) {
        add(row);  // rows are always added at the end
    }

    @Override
    public int find(ITableEditorRow row) {
        String filterName = row.getItem(0);

        int numFilters = (int) m_testFilters.size();
        for (int i = 0; i < numFilters; i++) {
            CTestFilter filter = CTestFilter.cast(m_testFilters.get(i));
            if (filter.getFilterId().equals(filterName)) {
                return i;
            }
        }
        return -1;
    }
    
    
    @Override
    public boolean isEmpty() {
        return m_testFilters.isEmpty();
    }

    
    @Override
    public void add(ITableEditorRow row) {
        if (find(row) >= 0) {
            throw new IllegalArgumentException("Duplicate elements are not allowed: " + 
                                               row.toString());
        }

        try {
            // undo/redo is not done in dialog, because the user may cancel everything
            String testFilterId = row.getItem(0);
            CTestFilter testFilter = new CTestFilter(null);
            testFilter.setFilterId(testFilterId);
            m_testFilters.add(-1, testFilter);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Error when adding statistics! ", ex);
            
            // m_testCoverage.removeStatistics((int)m_testCoverage.getStatisticsSize() - 1);
            // throw new RuntimeException(ex);
        }
        
        row.setParent(this);    
    }

    
    @Override
    public ITableEditorRow remove(int selectedIdx) {
        ITableEditorRow row = get(selectedIdx);
        m_testFilters.remove(selectedIdx);
        
        return row;
    }

    @Override
    public ITableEditorRow get(int i) {
        if (i < 0) {
            i = (int)m_testFilters.size() + i;
        }
        
        if (i < 0 ||  i >= (int)m_testFilters.size()) {
            return null;
        }
        
        CTestFilter testFilter =  CTestFilter.cast(m_testFilters.get(i));
        String filterName = testFilter.getFilterId();
        TableEditorRowAdapter row = m_tableEditor.createRow(new String[]{filterName});
        row.setParent(this);
        return row;
    }

    @Override
    public boolean isAutoAddLastEmptyRow() {
        return false;
    }

    @Override
    public void swap(int first, int second) {
        CTestFilter filter1 = CTestFilter.cast(m_testFilters.get(first));
        CTestFilter filter2 = CTestFilter.cast(m_testFilters.get(second));
        m_testFilters.remove(first);
        m_testFilters.add(first, filter2);
        
        m_testFilters.remove(second);
        m_testFilters.add(second, filter1);
    }


    @Override
    public List<ITableEditorRow> getRows() {
        int noOfFilters = (int)m_testFilters.size();
        List<ITableEditorRow> rows = new ArrayList<ITableEditorRow>();
        
        for (int i = 0; i < noOfFilters; i++) {
            CTestFilter testFilter = CTestFilter.cast(m_testFilters.get(i));
            String filterName = testFilter.getFilterId();

            TableEditorRowAdapter row = m_tableEditor.createRow(new String[]{filterName});
            row.setParent(this);
            rows.add(row);
        }
        return rows;
    }

    @Override
    // rename is done here, but only if the table is editable, otherwise this is never called
    public void modelChanged(ChangeType changeType,
                             int columnIdx,
                             int rowIdx,
                             ITableEditorRow row,
                             String newName) {
    }

}
