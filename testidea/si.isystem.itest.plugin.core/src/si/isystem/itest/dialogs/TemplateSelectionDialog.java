package si.isystem.itest.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import si.isystem.connect.CSourceCodeFile;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecComparer;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.TestSpecificationModel.SrcFileFormat;
import si.isystem.swttableeditor.ITableEditorRow;
import si.isystem.swttableeditor.ITableEditorRowDialog;
import si.isystem.swttableeditor.ITextFieldVerifier;
import si.isystem.swttableeditor.TableEditorCellModifier;
import si.isystem.swttableeditor.TableEditorContentProvider;
import si.isystem.swttableeditor.TableEditorLabelProvider;
import si.isystem.swttableeditor.TableEditorModelAdapter;
import si.isystem.swttableeditor.TableEditorPanel;
import si.isystem.swttableeditor.TableEditorRowAdapter;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class TemplateSelectionDialog  extends Dialog {

    private static final String TEMPLATE_FILES = "templateFiles";
    private static final String TEMPLATE_SELECTION_IDX = "templateSelectionIdx";
    private static final String TEMPLATE_ADD_AS_DERIVED_CB = "templateAddAsDerived";
    private TableEditorPanel m_fileListTable;
    private String m_selectedFile;
    private TreeViewer m_testSpecTree;
    private TableEditorModelAdapter m_fileListModel;
    private Button m_addAsDerivedCB;
    private String m_selectedTestSpecsAsStr;
    private boolean m_isAddAsDerived;
    
    public TemplateSelectionDialog(Shell parentShell) {
        super(parentShell);
        
        // make a dialog resizable, see also layout setting in createDialogArea() 
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }


    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Select template test specifications to add");

        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        mainDlgPanel.setLayoutData(gridData);

        mainDlgPanel.setLayout(new MigLayout("fill", "fill, 400", "[min!][fill][min!][fill, 200][min!]"));

        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        builder.label("Template files:", "wrap");

        createFileListTable(mainDlgPanel);

        builder.label("Select test specification to use as a template:", "gaptop 15, gapbottom 5, wrap");

        createTemplateTree(mainDlgPanel);
        
        m_addAsDerivedCB = builder.checkBox("Add as derived tests", 
                                                 "gaptop 15, gapbottom 15, wrap");
        UiTools.setToolTip(m_addAsDerivedCB, "If checked, selected templates will be added " +
        		"as derived tests\nof tests specifications selected in the test tree.");

        builder.separator("span2, growx", SWT.HORIZONTAL);

        Preferences templateNode = getPrefsNode();
        int selIdx = templateNode.getInt(TEMPLATE_SELECTION_IDX, 0);
        if (m_fileListModel.getRows().size() > selIdx) {
            m_selectedFile = m_fileListModel.getRows().get(selIdx).getItem(0);
            m_fileListTable.setSelection(selIdx);
        }

        if (m_selectedFile != null) {
            m_testSpecTree.setInput(loadTestSpecFile(m_selectedFile));
        }
        
        boolean isAddAsDerived = templateNode.getBoolean(TEMPLATE_ADD_AS_DERIVED_CB, false);
        m_addAsDerivedCB.setSelection(isAddAsDerived);
        
        return composite; 
    }


    private void createFileListTable(Composite parentPanel) {

        m_fileListTable = new TableEditorPanel(
                                  new String[]{"File name"}, 
                                  new String[]{"Select or add file with isystem.test test specifications."},
                                  new int[]{100}, 
                                  null,
                                  false);

        m_fileListTable.setInsertButtonText("Add", "Opens the 'File open' dialog.");
        m_fileListTable.setAddDialog(new AddFileDialog(getShell(), m_selectedFile));
        TableEditorModelAdapter model = createFileListModel(m_fileListTable);

        ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                try {
                    StructuredSelection selection = (StructuredSelection)event.getSelection();
                    TableEditorRowAdapter row = (TableEditorRowAdapter)selection.getFirstElement();

                    if (row != null) {  // can happen when item is deleted
                        m_selectedFile = row.getItem(0);
                        m_testSpecTree.setInput(loadTestSpecFile(m_selectedFile));
                    }
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), "Can not show selected stub!" , ex);
                }
            }
        }; 
        
        
        Composite panel = m_fileListTable.createPartControl(parentPanel,
                                                            null,
                                                            selectionChangedListener,
                                                            SWT.NONE);
        panel.setLayoutData("grow, wmin 0, hmin 0, wrap");

        m_fileListTable.setProviders(new TableEditorContentProvider(), 
                                     TableEditorLabelProvider.createProviders(1), 
                                     new TableEditorCellModifier(m_fileListTable.getViewer()));

        m_fileListTable.setInput(model);
    }
    
    
    public TableEditorModelAdapter createFileListModel(TableEditorPanel panel) {

        m_fileListModel = new TableEditorModelAdapter(false);
        
        Preferences templateFiles = getPrefsNode().node(TEMPLATE_FILES);
        
        String [] keys = new String[0];
        try {
            keys = templateFiles.keys();// childrenNames();
        } catch (BackingStoreException ex) {
            SExceptionDialog.open(getShell(), "Can't read file list from preferences!", ex);
        }

        
        for (String key : keys) {
            String fName = templateFiles.get(key, "");
            String columns[] = new String[]{fName};
            m_fileListModel.add(panel.createRow(columns));
        }

        return m_fileListModel;
    }


    private Preferences getPrefsNode() {
        // the following line equals to  
        // new ConfigurationScope().getNode(Activator.PLUGIN_ID);
        return Platform.getPreferencesService().getRootNode().
                                  node(ConfigurationScope.SCOPE).node(Activator.PLUGIN_ID);
    }


    private void createTemplateTree(Composite mainDlgPanel) {
        m_testSpecTree = new TreeViewer(mainDlgPanel, SWT.H_SCROLL | SWT.V_SCROLL);
        
        m_testSpecTree.setComparer(new TestSpecComparer());
        
        m_testSpecTree.setContentProvider(new TestSpecTreeContentProvider());
        
        m_testSpecTree.setLabelProvider(new TestSpecTreeLabelProvider());

        m_testSpecTree.getControl().setLayoutData("grow, wmin 0, hmin 0, wrap"); // hmin and wmin
        // are very important - if not present, scroll-bars disappear on panel resize! 
    }

    
    private CTestSpecification loadTestSpecFile(String yamlFile) {
        if (yamlFile.trim().isEmpty()) {
            return null;
        }
        try {
            CTestBench testBench;
            SrcFileFormat fileFormat = TestSpecificationModel.getFileFormatFromFileName(yamlFile);
            if (fileFormat == SrcFileFormat.SRC_YAML) {
                testBench = CTestBench.load(yamlFile, 0);
            } else {
                CSourceCodeFile srcFile = new CSourceCodeFile();
                testBench = srcFile.load(yamlFile);
            }
            return testBench.getTestSpecification(false);
        } catch (Exception ex) {
            SExceptionDialog.open(getShell(), "Can't open test specification file: " + yamlFile, ex);
        }
        
        return null;
    }


    @Override
    protected void okPressed() {
        storeFileListToPreferences();
        
        IStructuredSelection selection = (IStructuredSelection)m_testSpecTree.getSelection();
        
        @SuppressWarnings("rawtypes")
        Iterator iter = selection.iterator();
        CTestSpecification containerTestSpec = new CTestSpecification();
        int idx = 0;
        
        while (iter.hasNext()) {
            Object item = iter.next();
            if (item instanceof CTestSpecification) {
                CTestSpecification testSpec = (CTestSpecification)item;
                containerTestSpec.addDerivedTestSpec(idx++, testSpec);
            } else {
                throw new SIllegalStateException("Invalid selection detected - should be CTestSpecification!")
                .add("selectionType", item.getClass().getSimpleName());
            }
        }
        
        m_selectedTestSpecsAsStr = UiUtils.testBaseToString(containerTestSpec);
            
        m_isAddAsDerived = m_addAsDerivedCB.getSelection();
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }

    
    public boolean isAddAsDerived() {
        return m_isAddAsDerived;
    }
    
    public String getSelectedTestSpecifications() {
        return m_selectedTestSpecsAsStr;
    }
    
    private void storeFileListToPreferences() {

        // remove existing nodes first
        Preferences templateFiles = getPrefsNode().node(TEMPLATE_FILES);
        
        try {
            String [] files = templateFiles.keys();
            for (String file : files) {
                templateFiles.remove(file);
            }
        } catch (BackingStoreException ex) {
            SExceptionDialog.open(getShell(), "Can't read file list from preferences!", ex);
        }
        
        // store new entries
        List<ITableEditorRow> rows = m_fileListModel.getRows();
        
        int idx = 0;
        for (ITableEditorRow row : rows) {
            String pathName = row.getItem(0);
            if (!pathName.isEmpty()) {
                templateFiles.put("file" + idx, pathName);
                idx++;
            }
        }
        
        try {
            templateFiles.flush();
        } catch (BackingStoreException ex1) {
            SExceptionDialog.open(getShell(), "Can not store list of files to preferences.", ex1);
        }
        
        // store selection index
        int selectionIdx = m_fileListTable.getSelectionIndex();
        if (selectionIdx < 0) {
            selectionIdx = 0;
        }
        Preferences templateNode = getPrefsNode();
        templateNode.putInt(TEMPLATE_SELECTION_IDX, selectionIdx);

        // store 'Add as derived' check box
        templateNode.putBoolean(TEMPLATE_ADD_AS_DERIVED_CB, m_addAsDerivedCB.getSelection());
        
        try {
            templateNode.flush();
        } catch (BackingStoreException ex) {
            SExceptionDialog.open(getShell(), "Can not store template idx to preferences.", ex);
        }
        
        
    }
}




class TestSpecTreeContentProvider implements ITreeContentProvider {
    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getElements(Object parent) {

        CTestSpecification testSpec = (CTestSpecification)parent;
        
        int noOfDerivedSpecs = (int)testSpec.getNoOfDerivedSpecs();
        Object []children = new Object[noOfDerivedSpecs];
        
        for (int i = 0; i < noOfDerivedSpecs; i++) {
            children[i] = testSpec.getDerivedTestSpec(i);
        }
        
        return children;
    }

    @Override
    public Object[] getChildren(Object parent) {
        return getElements(parent);
    }

    @Override
    public Object getParent(Object element) {
        return ((CTestSpecification)element).getParentTestSpecification();
    }

    @Override
    public boolean hasChildren(Object element) {
        return ((CTestSpecification)element).hasChildren();
    }
}


class TestSpecTreeLabelProvider implements ILabelProvider {

    private List<ILabelProviderListener> m_listeners = new ArrayList<ILabelProviderListener>();
    
    @Override
    public String getText(Object element) {
        return ((CTestSpecification)element).getUILabel();
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        m_listeners.add(listener);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        m_listeners.remove(listener);
    }

    @Override
    public Image getImage(Object element) {
        return null;
    }
}


class AddFileDialog implements ITableEditorRowDialog {

    private Shell m_parentShell;
    private String m_fileName;
    private String m_selectedFileName;

    AddFileDialog(Shell parentShell, String fileName) {
        m_parentShell = parentShell;
        m_fileName = fileName;
    }
    
    
    @Override
    public boolean show() {

        String openDir = ".";
        if (m_fileName != null) {
            int lastSeparatorIndex = m_fileName.lastIndexOf(File.separator);
            if (lastSeparatorIndex != -1) {
                // set initial directory, if present in file name
                openDir = m_fileName.substring(0, lastSeparatorIndex);
            }
        }
        
        String res = UiUtils.showOpenIYamlFileDialog(m_parentShell, 
                                                     openDir, 
                                                     null,
                                                     false);
        if (res == null) {
            return false;
        }
        
        // check that the file really exists
        File file = new File(res.trim());
        if (!file.exists()) {
            MessageDialog.openError(m_parentShell, "File not found", "File '" + 
                                    res.trim() + "' does not exist!");
            return false;
        }
        
        m_selectedFileName = res.trim();
        return true;
    }

    
    @Override
    public String[] getData() {
        return new String[]{m_selectedFileName};
    }


    @Override
    public void setVerifier(ITextFieldVerifier verifier) {
        // no verification is done currently
    }
}


