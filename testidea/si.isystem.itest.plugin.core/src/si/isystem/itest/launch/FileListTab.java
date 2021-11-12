package si.isystem.itest.launch;

import java.util.ArrayList;
import java.util.List;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ITestSpecModelListener;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.TestSpecModelListenerAdapter;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableCellSelectionListener;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.editors.CellEditorComboBox;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EEditorType;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;

public class FileListTab extends AbstractLaunchConfigurationTab {

    public static final String ADD_ALL_FILES_IN_PROJECT_BTN = "  Add all (project)   ";
    
    // configuration attributes
    public static final String ATTR_TEST_INIT_SOURCE = "useTestConfig";
    public static final String ATTR_YAML_FILE_LIST = "yamlFileList";
    public static final String ATTR_IYAML_FILE_ROOT = "iyamlFileRoot";
    public static final String ATTR_IS_OPEN_REPORT_IN_BROWSER = "isOpenReportInBrowser";
    
    private KTable m_table;
    private FilesListTableModel m_fileListModel;
    private Combo m_projectsCombo;
    private Button m_addAllBtn;
    private Button m_isOpenReportInBrowserCb;
    private Button m_insertBtn;
    private Button m_delBtn;
    private Button m_upBtn;
    private Button m_downBtn;
    private Button m_browseBtn;
    private static final int NON_PROJ_ITEMS_IN_LIST = 2;
    private static final String ABS_PATH = "File system (abs. paths are added)";
    public static final String WORKSPACE_ROOT = "Workspace (workspace rel. paths are added)";
    public static final String WORKSPACE_LOC_VAR = "${workspace_loc}";
    public static final String PROJECT_LOC_VAR = "${project_loc}";

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        comp.setLayout(new MigLayout("fill", "[fill][min!]", "[min!][min!][fill][min!][min!][min!][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(comp);
        
        m_projectsCombo = createProjectsCombo(builder);
        
        builder.label("List of iyaml files to be run with this launch configuration:", 
                      "gaptop 10, wrap");
        
        m_table = new KTable(comp, false, SWT.H_SCROLL | SWT.V_SCROLL | 
                             SWTX.FILL_WITH_LASTCOL | SWTX.EDIT_ON_KEY | 
                             SWTX.MARK_FOCUS_HEADERS | SWT.FULL_SELECTION);
        
        m_fileListModel = new FilesListTableModel();
        m_fileListModel.setListener(new TestSpecModelListenerAdapter() {
            
            @Override
            public void modelChanged(ModelChangedEvent event) {
                // this call is essential to update Apply/Revert buttons!
                updateLaunchConfigurationDialog();
            }
        });

        
        m_table.setModel(m_fileListModel);

        m_table.setLayoutData("wmin 0, spanx 3, growx, growy, gaptop 5");

        m_table.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseUp(MouseEvent e) {

                Point[] selection = m_table.getCellSelection();
                
                if (selection.length == 1) {
                    
                    int row = selection[0].y;

                    if (row == m_fileListModel.getRowCount() - 1) { // add row is clicked
                        m_fileListModel.addRow(-1);
                        m_table.redraw();
                    }
                    /* if (row > 0) {
                        if (col == FilesListTableModel.COL_INIT_TYPE) {
                            Boolean value = (Boolean)m_model.getContentAt(col, row);
                            m_model.setContentAt(col, row, !value.booleanValue());
                            m_xlsColorsTable.redraw();
                        } else if (col == HSSFColorTableModel.COL_COLOR) {
                            HSSFColor color = HSSFColorDialog.show(colorsColumnWidth);
                            if (color != null) {
                                m_model.setContentAt(col, row, color);
                            }
                            m_xlsColorsTable.redraw();
                        }
                    } */
                }
            }
            
            
            @Override
            public void mouseDown(MouseEvent e) {}
            
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {}
        });
     
        m_table.addCellSelectionListener(new KTableCellSelectionListener() {
            
            @Override
            public void fixedCellSelected(int col, int row, int statemask) {
                enableButtons(false);
            }
            
            
            @Override
            public void cellSelected(int col, int row, int statemask) {
                if (col == 0  &&  row == 0) {
                    return;// no change in selection if top left corner cell is clicked
                }
                enableButtons(row >= m_fileListModel.getFixedHeaderRowCount()  &&
                              row < (m_fileListModel.getRowCount() - 1));
            }
        });
        
        
        Composite buttonPanel = builder.composite(SWT.NONE, "wrap");
        buttonPanel.setLayout(new MigLayout());
        addButtons(new KGUIBuilder(buttonPanel));
        
        builder.label("Description of values in column 'Test Init Source':", "wrap");
        builder.label("  No init - target is not initializaed before test", "wrap");
        builder.label("  File - target is initialized as defined in iyaml file (Project properties | Run Configuration)", "wrap");
        builder.label("  Launch config. - target is initialized as defined in this launch configuration, tab 'Test init'", "wrap");

        m_isOpenReportInBrowserCb = builder.checkBox("Open reports in default browser");
        m_isOpenReportInBrowserCb.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });
    }


    private Combo createProjectsCombo(KGUIBuilder parentBuilder) {
        
        KGUIBuilder builder = new KGUIBuilder(parentBuilder.composite(SWT.NONE, 
                                                                      "wrap"));
        builder.getParent().setLayout(new MigLayout("", "[min!][min!]"));
        
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        String[] projNames = new String[projects.length + NON_PROJ_ITEMS_IN_LIST];
        projNames[0] = ABS_PATH;
        projNames[1] = WORKSPACE_ROOT;
        int idx = NON_PROJ_ITEMS_IN_LIST;
        for (IProject project : projects) {
            projNames[idx++] = project.getName();
        }
        
        builder.label("Select root:");
        Combo projectsCombo = builder.combo(projNames, "wrap", SWT.NONE);
        builder.label("If project is selected, project relative paths are added.", 
                      "skip, wrap");

        projectsCombo.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAddAllBtnText();
                updateLaunchConfigurationDialog();
            }
        });
        
        return projectsCombo;
    }

    
    private void setAddAllBtnText() {
        if (m_projectsCombo.getSelectionIndex() < NON_PROJ_ITEMS_IN_LIST) {
            m_addAllBtn.setText("Add all (workspace)");
        } else {
            m_addAllBtn.setText(ADD_ALL_FILES_IN_PROJECT_BTN);
        }
    }
    
    
    private void addButtons(KGUIBuilder builder) {
    
        m_insertBtn = builder.button("Insert", "growx, wrap");
        UiTools.setToolTip(m_insertBtn, "Inserts line above selected line.");
        m_insertBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_table.getCellSelection();
                if (selection.length == 1) {
                    int modelRow = selection[0].y - m_fileListModel.getFixedHeaderRowCount();
                    m_fileListModel.addRow(modelRow);
                    m_table.redraw();
                }
            }
        });
        
        m_delBtn = builder.button("Delete", "growx, wrap");
        UiTools.setToolTip(m_delBtn, "Deletes selected line.");
        m_delBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_table.getCellSelection();
                if (selection.length == 1) {
                    int modelRow = selection[0].y - m_fileListModel.getFixedHeaderRowCount();
                    m_fileListModel.deleteRow(modelRow);
                    m_table.redraw();
                }
            }
        });
        
        m_upBtn = builder.button("Up", "growx, gaptop 15, wrap");
        UiTools.setToolTip(m_upBtn, "Moves selected line up.");
        m_upBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_table.getCellSelection();
                if (selection.length == 1) {
                    int modelRow = selection[0].y - m_fileListModel.getFixedHeaderRowCount();
                    int newRow = m_fileListModel.swapRows(modelRow, modelRow - 1) + 
                                 m_fileListModel.getFixedHeaderRowCount();
                    selection = new Point[]{new Point(selection[0].x, newRow)};
                    m_table.setSelection(selection, true);
                    m_table.redraw();
                }
            }
        });
        
        
        m_downBtn = builder.button("Down", "growx, wrap");
        UiTools.setToolTip(m_downBtn, "Moves selected line down.");
        m_downBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_table.getCellSelection();
                if (selection.length == 1) {
                    int modelRow = selection[0].y - m_fileListModel.getFixedHeaderRowCount();
                    int newRow = m_fileListModel.swapRows(modelRow, modelRow + 1) + 
                                 m_fileListModel.getFixedHeaderRowCount();
                    selection = new Point[]{new Point(selection[0].x, newRow)};
                    m_table.setSelection(selection, true);
                    m_table.redraw();
                }
            }
        });
        
        
        m_browseBtn = builder.button("Browse", "growx, gaptop 15, wrap");
        UiTools.setToolTip(m_browseBtn, "Opens file dialog to select iyaml file.");
        m_browseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_table.getCellSelection();
                if (selection.length == 1) {
                    String fileName = UiUtils.showOpenIYamlFileDialog(Activator.getShell(), 
                                                                      "",
                                                                      "",
                                                                      true);
                    if (fileName == null) {
                        return; // dialog canceled by user
                    }
                    int modelRow = selection[0].y - m_fileListModel.getFixedHeaderRowCount();
                    IPath absPath = new Path(fileName);
                    
                    int idx = m_projectsCombo.getSelectionIndex();
                    if (idx < NON_PROJ_ITEMS_IN_LIST) {
                        if (idx == 0) { 
                            // add abs. path, no change 
                        } else {
                            // add workspace relative path
                            IPath wsAbsPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
                            if (wsAbsPath.isPrefixOf(absPath)) {
                                IPath relPath = absPath.makeRelativeTo(wsAbsPath);
                                fileName = WORKSPACE_LOC_VAR + "/" + relPath.toPortableString();
                            } // else keep absolute path
                        }
                    } else {
                        // add project relative path, if selected file is part of project,
                        // else add absolute path 
                        
                        String projName = m_projectsCombo.getItem(idx);
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
                        IPath projectAbsPath = project.getLocation();
                        if (projectAbsPath.isPrefixOf(absPath)) {
                            IPath relPath = absPath.makeRelativeTo(projectAbsPath);
                            fileName = PROJECT_LOC_VAR + "/" + relPath.toPortableString();
                        } // else keep absolute path
                    }
                    
                    m_fileListModel.setRow(modelRow, fileName);
                    m_table.redraw();
                }
            }
        });
        
        m_addAllBtn = builder.button("Add all", "growx, wrap");
        UiTools.setToolTip(m_addAllBtn, "Adds all iyaml files found in the selected project at the end of the table.\n"
                                      + "Existing contents is not modified.");
        m_addAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                int idx = m_projectsCombo.getSelectionIndex();
                if (idx < NON_PROJ_ITEMS_IN_LIST) {
                    IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
                    visitResources(ws, idx);
                    
                } else {                
                    IProject project = UiUtils.getSelectedProjectInProjectExplorer();

                    visitResources(project, idx);
                }
                
                m_table.redraw();
            }

            
            private void visitResources(IResource project, 
                                        final int idx) {
                IResourceVisitor visitor = new IResourceVisitor() {

                    @Override
                    public boolean visit(IResource resource) throws CoreException {
                        String extension = resource.getFileExtension();
                        if (extension != null  &&  extension.equals("iyaml")) {
                            IPath path;
                            if (idx == 0) {
                                path = resource.getLocation();
                                m_fileListModel.addRow(path.toPortableString());
                            } else if (idx == 1) {
                                path = resource.getFullPath();
                                m_fileListModel.addRow(WORKSPACE_LOC_VAR + path.toPortableString());
                            } else {
                                path = resource.getProjectRelativePath();
                                m_fileListModel.addRow(PROJECT_LOC_VAR + "/" + path.toPortableString());
                            }
                        }
                        return true;
                    }
                };

                try {
                    if (project != null) {
                        project.accept(visitor, IResource.DEPTH_INFINITE, IResource.FILE);
                    } else {
                        SExceptionDialog.open(Activator.getShell(), "Selection error!", 
                                              new Exception("No project is selected! Please select one."));
                    }
                } catch (CoreException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
    }
    
    
    private void enableButtons(boolean isEnabled) {
        m_insertBtn.setEnabled(isEnabled);
        m_delBtn.setEnabled(isEnabled);
        m_upBtn.setEnabled(isEnabled);
        m_downBtn.setEnabled(isEnabled);
        m_browseBtn.setEnabled(isEnabled);
    }

    
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        // System.out.println("FileList.setDefaults()");
        if (m_fileListModel != null) {
            m_fileListModel.setData(new ArrayList<String>(), new ArrayList<String>());
            m_table.redraw();
        }
    }

    
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        // System.out.println("FileList.initializeFrom() " + hashCode());

        try {
            List<String> fileList = new ArrayList<>();
            List<String> configList = configuration.getAttribute(ATTR_YAML_FILE_LIST, fileList);
            // getAttribute() returns reference to the internal list, so copy is 
            // required!
            fileList.addAll(configList);

            List<String> useLaunchTestConfig = new ArrayList<>();
            
            useLaunchTestConfig = configuration.getAttribute(ATTR_TEST_INIT_SOURCE, 
                                                             useLaunchTestConfig);
            // just in case - make sure sizes match
            while (useLaunchTestConfig.size() < fileList.size()) {
                useLaunchTestConfig.add("");
            }

            m_fileListModel.setData(fileList, useLaunchTestConfig);

            IProject proj = UiUtils.getSelectedProjectInProjectExplorer();
            String defaultValue = WORKSPACE_ROOT;
            if (proj != null) {
                defaultValue = UiUtils.getSelectedProjectInProjectExplorer().getName();
            }
            
            String rootName = configuration.getAttribute(ATTR_IYAML_FILE_ROOT, defaultValue);
            for (int idx = 0; idx < m_projectsCombo.getItemCount(); idx++) {
                if (rootName.equals(m_projectsCombo.getItem(idx))) {
                    m_projectsCombo.select(idx);
                    break;
                }
            }
            
            setAddAllBtnText();

            boolean isOpenReportInBrowser = configuration.getAttribute(ATTR_IS_OPEN_REPORT_IN_BROWSER, 
                                                                       false);
            if (isOpenReportInBrowser) {
                m_isOpenReportInBrowserCb.setSelection(isOpenReportInBrowser);
            }
            
        } catch (Exception ex) {
            Activator.log(IStatus.ERROR, "Can not intialize iyaml file list!", ex);
            SExceptionDialog.open(Activator.getShell(), 
                                  "Internal error! Can not intialize iyaml file list!", 
                                  ex);
            ex.printStackTrace();
            m_fileListModel.setData(new ArrayList<String>(), new ArrayList<String>());
        }
    }
    

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        
        // System.out.println("FileList.performApply " + hashCode());
        configuration.setAttribute(ATTR_YAML_FILE_LIST, m_fileListModel.getFiles());
        configuration.setAttribute(ATTR_TEST_INIT_SOURCE, m_fileListModel.getTestInitSources());
        configuration.setAttribute(ATTR_IYAML_FILE_ROOT, m_projectsCombo.getText());
        configuration.setAttribute(ATTR_IS_OPEN_REPORT_IN_BROWSER, 
                                   m_isOpenReportInBrowserCb.getSelection());

    }

    
    @Override
    public String getName() {
        return "Test files";
    }


    public static boolean isAbsPaths(String rootName) {
        return rootName.equals(ABS_PATH);
    }

    
    public static boolean isWorkspaceRoot(String rootName) {
        return rootName.equals(WORKSPACE_ROOT);
    }

}


class FilesListTableModel extends KTableModelAdapter {

    protected static final int COL_INIT_TYPE = 1;
    protected static final int COL_FILE_NAME = 2;

    private static final int NUM_COLUMNS = 3;
    
    private List<String> m_fileList = new ArrayList<>();
    private List<ETestInitSource> m_testInitConfigSrc = new ArrayList<>();
    private int m_colWidth[] = new int[NUM_COLUMNS];
 
    CellEditorComboBox m_initSourceComboEditor = new CellEditorComboBox();

    private TextIconsCellRenderer m_iconRenderer = new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, 
                                                                             false);
    private TextIconsContent m_addRowCellContent;
    private TextIconsContent m_initCellContent;
    private TextIconsContent m_fileCellContent;
    
    private ITestSpecModelListener m_listener;

    public enum ETestInitSource {
        ENoInit {
            @Override
            String getUIText() {
                return "No init";
            }
        }, 
        
        EFile {
            @Override
            String getUIText() {
                return "File";
            }
        }, 
        
        ELaunchConfig {
            @Override
            String getUIText() {
                return "Launch config.";
            }
        };
        
        abstract String getUIText();
    };
    
    
    public FilesListTableModel() {
        m_colWidth[0] = 40; 
        m_colWidth[1] = 100; 
        m_colWidth[2] = 150;
        
        m_initSourceComboEditor.setItems(new String[]{ETestInitSource.ENoInit.getUIText(),
                                         ETestInitSource.EFile.getUIText(),
                                         ETestInitSource.ELaunchConfig.getUIText()}, 
                                         null);
        
        m_iconRenderer.setEnabled(true);
        m_addRowCellContent = new TextIconsContent();
        Image icon = IconProvider.INSTANCE.getIcon(EIconId.EAddItem);
        m_addRowCellContent.setIcon(EIconPos.EMiddleMiddle, icon, true);
        m_addRowCellContent.setEditable(true);  // to get white background
        
        m_initCellContent = new TextIconsContent();
        m_initCellContent.setEditorType(EEditorType.ECombo);
        m_initCellContent.setEditable(true);
        
        m_fileCellContent = new TextIconsContent();
        m_fileCellContent.setEditable(true);
    }
    
    
    public void setData(List<String> files, List<String> testInitConfigSrc) {
        System.out.println("FileList.setData()");
        m_fileList = files;
        
        for (String initSrc : testInitConfigSrc) {
            if (initSrc.isEmpty()) {
                initSrc = ETestInitSource.ENoInit.toString();
            }
            m_testInitConfigSrc.add(ETestInitSource.valueOf(initSrc));
        }
    }
    
    
    public List<String> getFiles() {
        return m_fileList;
    }
    
    
    public List<String> getTestInitSources() {
        
        List<String> strList = new ArrayList<>();
        
        for (ETestInitSource initSrc : m_testInitConfigSrc) {
            strList.add(initSrc.toString());
        }
        
        return strList;
    }
    
    
    void setListener(ITestSpecModelListener listener) {
        m_listener = listener;
    }
    
    
    @Override
    public String getTooltipAt(int col, int row) {
        return null;
    }

    @Override
    public KTableCellEditor getCellEditor(int col, int row) {
        switch (col) {
        case COL_INIT_TYPE:
            return m_initSourceComboEditor;
        case COL_FILE_NAME:
            return new KTableCellEditorText2();
        }
        return null;
    }

    
    @Override
    public void setContentAt(int col, int row, Object value) {
        
        int modelRow = row - getFixedHeaderRowCount();
        
        switch (col) {
        case COL_INIT_TYPE:
            for (ETestInitSource enumItem : ETestInitSource.values()) {
                if (value.equals(enumItem.getUIText())) {
                    m_testInitConfigSrc.set(modelRow, enumItem);
                    break;
                }
            }
            break;
        case COL_FILE_NAME:
            if (value instanceof String) {
                String element = (String)value;
                m_fileList.set(modelRow, element);
            }
            break;
        }      
    
        if (m_listener != null) {
            m_listener.modelChanged(null);
        }
    }

    
    @Override
    public Point belongsToCell(int col, int row) {
        if (row == getRowCount() - 1  &&  col != 0) {
            // the last row has only header column and all other cells with + sign
            return new Point(1, row);
        }
        return null;
    }


    @Override
    public int getColumnWidth(int col) {
        return m_colWidth[col];
    }

    
    @Override
    public boolean isColumnResizable(int col) {
        
        return col != 0;
    }

    
    @Override
    public void setColumnWidth(int col, int width) {
        m_colWidth[col] = width;
    }
    

    @Override
    public int getRowHeight(int row) {
        return 20;
    }


    @Override
    public int getRowHeightMinimum() {
        return 20;
    }

    @Override
    public void setRowHeight(int row, int value) {
    }


    @Override
    public Object doGetContentAt(int col, int row) {
        
        if (row == 0) {
            switch (col) {
            case COL_INIT_TYPE:
                return "Test Init Source";
            case COL_FILE_NAME:
                return "File (*.iyaml)";
            }
            return "";
        }
        
        if (row == getRowCount() - 1  &&  col > 0) {
            return m_addRowCellContent;
        }
        
        int modelRow = row - getFixedHeaderRowCount();
        switch (col) {
        case 0:
            return String.valueOf(row);
        case COL_INIT_TYPE:
            m_initCellContent.setText(m_testInitConfigSrc.get(modelRow).getUIText());
            return m_initCellContent;
        case COL_FILE_NAME:
            m_fileCellContent.setText(m_fileList.get(modelRow));
            return m_fileCellContent;
        }        
        
        return "";
    }


    @Override
    public int doGetRowCount() {
        return m_fileList.size() + getFixedHeaderRowCount() + 1; // one for row with + sign
    }


    @Override
    public int doGetColumnCount() {
        return 2 + getFixedHeaderColumnCount();
    }
    
    
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        
        if (col == 0  ||  row == 0) {
            return super.doGetCellRenderer(col, row);                    
        }
        
        return m_iconRenderer;
        
        /*
        switch (col) {
        case 1:
            return new CheckableCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
        case 2:
        }
        return null; */
    }
    
    
    void addRow(int modelRow) {
        if (modelRow < 0  ||  modelRow >= m_fileList.size()) {
            m_testInitConfigSrc.add(ETestInitSource.ENoInit);
            m_fileList.add("");
        } else {
            m_testInitConfigSrc.add(modelRow, ETestInitSource.ENoInit);
            m_fileList.add(modelRow, "");
        }
    }


    /**
     * Adds file name to the model after the last existing row.
     * @param fileName
     */
    void addRow(String fileName) {
        m_testInitConfigSrc.add(ETestInitSource.ENoInit);
        m_fileList.add(fileName);
    }
    
    
    public void deleteRow(int modelRow) {
        if (modelRow >= 0  &&  modelRow < m_fileList.size()) {
            m_testInitConfigSrc.remove(modelRow);
            m_fileList.remove(modelRow);
        }
    }
    
    
    public int swapRows(int first, int second) {
        
        if (first < 0 ||  first >= m_fileList.size()  ||  second < 0  ||  second >= m_fileList.size()) {
            return first; // out of range, nothing to do
        }
        
        String file = m_fileList.get(first);
        ETestInitSource initSrc = m_testInitConfigSrc.get(first);

        m_fileList.set(first, m_fileList.get(second));
        m_testInitConfigSrc.set(first, m_testInitConfigSrc.get(second));
        
        m_fileList.set(second, file);
        m_testInitConfigSrc.set(second, initSrc);
        
        return second;
    }


    public void setRow(int modelRow, String fileName) {
        if (modelRow >= 0  &&  modelRow < m_fileList.size()) {
            m_fileList.set(modelRow, fileName);
        }
    }
}