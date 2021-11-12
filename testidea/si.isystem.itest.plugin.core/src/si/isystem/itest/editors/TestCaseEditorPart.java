package si.isystem.itest.editors;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.services.ISourceProviderService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.handlers.FileSaveAsCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.TestSpecModelListenerAdapter;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.TestSpecificationEditorView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.TestTreeOutline;
import si.isystem.itest.ui.spec.sections.SectionTreeModel;
import si.isystem.itest.ui.spec.table.TestSpecificationTableEditor;

/**
 * This class contains editors for test specifications. Currently the design is 
 * flawed, as part of functionality is shared between form and table editors,
 * especially the one related to sections tree. This limits sections tree for one
 * editor to be more fine grained (for example for table editor), and presents
 * potential bugs in case of code changes in one editor. On refactoring try to:
 * - make sections tree independent of form editors. Currently label provider
 *   calls section editor for empty/merge status.
 * - consider implementing base class for form and table pages, with common 
 *   functionality (especially the one related to sections tree) in the base
 *   class
 * - if different sections tree will be needed for form and table editor pages,
 *   then create completely new sections tree for table editor. This tree does not
 *   have to show merge information, only empty/not empty or active state.
 *   The tree should also reflect CTestSpecification sections one to one, with
 *   paths which define the section in CTestSpecification data tree (like it
 *   is done for table headers, for example '/testSpec/analyzer/coverage/stat').
 * <ul>
 * <li>page 0 contains a form editor for test cases.
 * <li>page 1 contains table editor for test case and derived test cases
 * </ul>
 */
public class TestCaseEditorPart extends MultiPageEditorPart
                                implements IResourceChangeListener {

    public static final String ID = "si.isystem.itest.editors.TestCaseEditorPart";

    /** The text editor used in page 0. */
    private TextEditor editor;

    /** The font chosen in page 1. */
    private Font font;

    /** The text widget used in page 2. */
    private StyledText text;

    private TestSpecificationModel m_model;

    private TestTreeOutline m_outlineViewAdapter;

    private SectionTreeModel m_sectionTreeModel;

    private TestSpecificationEditorView m_formEditor;
    private TestSpecificationTableEditor m_tableEditor;

    private static final int FORM_EDITOR = 0;
    private static final int TABLE_EDITOR = 1;
    private int m_idxOfCurrentPage = 0;


    /**
     * Creates a multi-page editor example.
     */
    public TestCaseEditorPart() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        m_sectionTreeModel = new SectionTreeModel();
        Activator.log(Status.INFO, "TestCaseEditorPart Constructed!!", null);
    }


    public static TestSpecificationModel getActiveModel() {
        TestCaseEditorPart activeEditor = getActive();
        if (activeEditor != null) {
            return activeEditor.getModel();
        }
        return null;
    }


    public TestSpecificationModel getModel() {
        return m_model;
    }


    /**
     * WARNING: This method does not work as expected when editors are floating (detached).
     * Always returns reference to last active editor in main window, even if detached
     * editor is selected. Since it is called from many places, the problem should be
     * fixed in this method, not in all other places.
     * 
     * @return
     */
    public static TestCaseEditorPart getActive() {
        
//        IWorkbench workbench = PlatformUI.getWorkbench();
//        if (workbench != null) {
//            IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
//            
//            IWorkbenchWindow[] wws = workbench.getWorkbenchWindows();
//            for (IWorkbenchWindow ww : wws) {
//                System.out.println("ww = " + ww);
//            }
//            
//            if (activeWindow != null) {
//                IWorkbenchPage[] pages = activeWindow.getPages();
//                for (IWorkbenchPage p : pages) {
//                    System.out.println("page = " + p);
//
//                    // Returns refs to all editors, including detached ones.        
//                    IEditorReference[] editors = p.getEditorReferences();
//                    for (IEditorReference e : editors) {
//                        System.out.println("E editor : " + e);
//                    }
//                }
//            }
//
//        }
        
        
        
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null) {
            IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
            
            if (activeWindow != null) {
                // page is always only one - contains also detached editors, 
                // see page.getEditorReferences()
                IWorkbenchPage activePage = activeWindow.getActivePage();
                if (activePage != null) {
                    IEditorPart activeEditor = activePage.getActiveEditor(); // this one returns wrong editor
                    if (activeEditor instanceof TestCaseEditorPart) {
                        return (TestCaseEditorPart) activeEditor;
                    }
                }
            }
        }

        return null;
    }


    /**
     * Creates page of the multi-page editor, which enables editing with UI
     * controls.
     */
    void createFormPage() {

        m_formEditor = new TestSpecificationEditorView(this,
                                                       m_sectionTreeModel);

        try {
            int index = addPage(m_formEditor, getEditorInput());
            setPageText(index, "Form");
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                                  "Error creating nested text editor",
                                  null,
                                  e.getStatus());
        }
    }


    /**
     * Creates page of the multi-page editor, which shows the sorted text.
     */
    void createTableEditorPage() {

        try {

            m_tableEditor = new TestSpecificationTableEditor(this,
                                                             m_sectionTreeModel);

            int index = addPage(m_tableEditor, getEditorInput());
            setPageText(index, "Table");
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                                  "Error creating nested text editor",
                                  null,
                                  e.getStatus());
        }
    }


    /**
     * Creates page of the multi-page editor, which contains a text editor.
     */
    void createTextEditorPage() {
        try {
            editor = new TextEditor();
            int index = addPage(editor, getEditorInput());
            setPageText(index, "iyaml"); // editor.getTitle());
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                                  "Error creating nested text editor",
                                  null,
                                  e.getStatus());
        }
    }


    /**
     * Creates the pages of the multi-page editor.
     */
    @Override
    protected void createPages() {
        Activator.log(Status.INFO, "TestCaseEditorPart - createPages() start", null);
        
        createFormPage();
        createTableEditorPage();
        // createTextEditorPage(); // add synchronization with form editor
        // before enabling
        // setSelection(m_model.getFirstSelection());
        Activator.log(Status.INFO, "TestCaseEditorPart - createPages() end", null);
    }


    /**
     * The <code>MultiPageEditorPart</code> implementation of this
     * <code>IWorkbenchPart</code> method disposes all nested editors.
     * Subclasses may extend.
     */
    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
		/* TestSpecificationTreeView treeView = TestSpecificationTreeView.getView();
		if (treeView != null) {
		    treeView.setInput(null);
		} */
        if (m_outlineViewAdapter != null) {
            m_outlineViewAdapter.setInput(null, null);
        }
    }


    public static TestTreeOutline getOutline() {
        TestCaseEditorPart testEditor = getActive();
        if (testEditor != null) {
            Object adapter = testEditor.getAdapter(IContentOutlinePage.class);
            if (adapter instanceof TestTreeOutline) {
                TestTreeOutline outline = (TestTreeOutline) adapter;
                return outline;
            }
        }

        return null;
    }


    /*
     * (non-Javadoc) Method declared on IEditorPart.
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }


    @Override
    public boolean isDirty() {
        // System.out.println("DIIIIIIIIIIIIIIIIIRTY! " + m_model.isModelDirty()
        // + " " + hashCode());
        return m_model.isModelDirty();
    }


    /**
     * Saves the multi-page editor's document.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        m_model.saveModel();

        // Since the model is saved in C++ code, we can not call
        // file.setContents(),
        // so resource becomes unsynchronized - file time stamp changes, but
        // Eclipse's
        // resource time stmap doesn't.
        // An exception is thrown in Eclipse if
        // we right click the file in Project Explorer after save and printed to
        // parent Eclipse's console. To avoid this, we force resource refresh
        // here.
        IEditorInput editorInput = getEditorInput();
        if (editorInput instanceof IFileEditorInput) {
            IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            try {
                fileEditorInput.getFile().refreshLocal(IResource.DEPTH_ZERO,
                                                       null);
            } catch (CoreException ex) {
                SExceptionDialog.open(Activator.getShell(),
                                      "Can not save file "
                                              + fileEditorInput.getFile()
                                                               .getFullPath()
                                                               .toOSString(),
                                      ex);
                ex.printStackTrace();
            }
        }

        firePropertyChange(PROP_DIRTY);
    }


    /**
     * Saves the multi-page editor's document as another file. Also updates the
     * text for page 0's tab, and updates this multi-page editor's input to
     * correspond to the nested editor's.
     */
    @Override
    public void doSaveAs() {
        FileSaveAsCmdHandler handler = new FileSaveAsCmdHandler();
        handler.saveModelAs(m_model, getSite().getShell());

        String modelFileName = m_model.getModelFileName();
        Path path = new Path(modelFileName);

        // after saveAs() input is always FileStoreEditorInput
        IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
        FileStoreEditorInput fileEditorInput = new FileStoreEditorInput(fileStore);

        setInput(fileEditorInput);

        setPartName(path.lastSegment()); // editor tab contents
        // setPartName(fileStore.getName());

        firePropertyChange(PROP_DIRTY);

        // TextEditor editor = (TextEditor)getEditor(1);
        // editor.doSaveAs();
        // setPageText(0, editor.getTitle());
        // setInput(editor.getEditorInput());
    }


    /*
     * (non-Javadoc) Method declared on IEditorPart
     */
    public void gotoMarker(IMarker marker) {
        setActivePage(0);
        IDE.gotoMarker(getEditor(0), marker);
    }


    /**
     * The <code>MultiPageEditorExample</code> implementation of this method
     * checks that the input is an instance of <code>IFileEditorInput</code>.
     */
    @Override
    public void init(IEditorSite site,
                     IEditorInput editorInput) throws PartInitException {
        String editorTabText = "";
        String fileName = null;

        if (editorInput instanceof IFileEditorInput) {
            IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            fileName = fileEditorInput.getFile().getLocation().toOSString();
            editorTabText = fileEditorInput.getName();
        } else if (editorInput instanceof FileStoreEditorInput) {
            FileStoreEditorInput fileEditorInput = (FileStoreEditorInput) editorInput;
            URI uri = fileEditorInput.getURI();
            File file = URIUtil.toFile(uri);
            fileName = file.getAbsolutePath();
            editorTabText = fileEditorInput.getName();
        } else {
            throw new PartInitException("Invalid Input: Must be IFileEditorInput or FileStoreEditorInput!");
        }

        m_model = new TestSpecificationModel();

        if (fileName != null && Files.exists(Paths.get(fileName))) {
            try {
                m_model.openTestSpec(fileName, 0);
                setPartName(editorTabText);
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(),
                                      "Can not open test specification: "
                                              + fileName,
                                      ex);
                // editor will show error and this exception in Details, which
                // is
                // better than showing empty edit controls like nothing
                // happened.
                throw ex;
            }
        } else {
            // should never happen, this exception is displayed in an editor
            throw new SIOException("Can not open file! File does not exist: "
                    + fileName).add("fileName", fileName);
        }

        m_model.addListener(new ModelListener());

        super.init(site, editorInput);
        Activator.log(Status.INFO, "TestCaseEditor: Model loaded!!", null);
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {

        if (adapter == IContentOutlinePage.class) {
            if (m_outlineViewAdapter == null) {
                m_outlineViewAdapter = new TestTreeOutline();
                m_outlineViewAdapter.setInput(m_model, m_formEditor.getInput());
            }

            return (T) m_outlineViewAdapter;
        }

        return super.getAdapter(adapter);
    }


    public CTestTreeNode getActiveTestCase() {
        return m_formEditor.getInput();
    }


    /**
     * Calculates the contents of page 2 when it is activated.
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);

        // select the currently selected section also in editor page to be
        // shown, nice usability feature.
        if (newPageIndex == FORM_EDITOR) {
            ISelection selection = m_tableEditor.getTreeSelection();
            m_formEditor.setTreeSelection(selection);
            m_formEditor.setInput(m_model, m_tableEditor.getInput());
        } else {
            ISelection selection = m_formEditor.getTreeSelection();
            m_tableEditor.setTreeSelection(selection);
            m_tableEditor.setInput(m_model, m_formEditor.getInput());
        }

        m_idxOfCurrentPage = newPageIndex;
    }


    /**
     * Closes all project files on project close.
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {

        int eventType = event.getType();
        IResourceDelta delta = event.getDelta();
        IEditorInput editorInput = getEditorInput();
        IResource adapter = (IResource) editorInput.getAdapter(IResource.class);
        if (adapter == null) {
            return;
        }
        IPath projectRelativePath = adapter.getProjectRelativePath();
        IPath projectPath = adapter.getProject().getProjectRelativePath();

        if (delta == null) {
            // the following executes when user deletes project in Project
            // Explorer
            // containing file in this editor
            if (eventType == IResourceChangeEvent.PRE_DELETE) {
                IResource deletedResource = event.getResource();
                if (deletedResource.getProjectRelativePath()
                                   .equals(projectPath)) {
                    closeEditor();
                }
            }
            return;
        }

        // the following executes when user deletes file in Project Explorer
        IResourceDelta[] children;
        do {
            children = delta.getAffectedChildren();
            if (children.length > 0) {
                delta = children[0];
            }
        } while (children.length > 0);

        int deltaKind = delta.getKind();

        delta.getProjectRelativePath();

        // check that it is this editor's resource being deleted
        if (projectRelativePath.equals(delta.getProjectRelativePath())) {
            if (eventType == IResourceChangeEvent.POST_CHANGE) {
                switch (deltaKind) {
                case IResourceDelta.MOVED_TO:
                    // System.out.println("MOVED_TO: " + deltaKind);
                    closeEditor();
                    break;
                case IResourceDelta.REMOVED:
                    // System.out.println("REMOVED: " + deltaKind);
                    closeEditor();
                    break;

                default:
                    // System.out.println("Unhandled resource delta change: " +
                    // deltaKind);
                    break;
                }
            }

            if (eventType == IResourceChangeEvent.PRE_CLOSE) {
                closeEditor();
            }
        }
    }


    private void closeEditor() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchPage[] pages = getSite().getWorkbenchWindow()
                                                  .getPages();
                for (IWorkbenchPage page : pages) {

                    // if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
                    IEditorPart editorPart = page.findEditor(TestCaseEditorPart.this.getEditorInput());
                    if (editorPart != null) {
                        page.closeEditor(editorPart, true);
                    }
                    // }
                }
            }
        });
    }


    /**
     * Sets the font related data to be applied to the text in page 2.
     */
    void setFont() {
        FontDialog fontDialog = new FontDialog(getSite().getShell());
        fontDialog.setFontList(text.getFont().getFontData());
        FontData fontData = fontDialog.open();
        if (fontData != null) {
            if (font != null)
                font.dispose();
            font = new Font(text.getDisplay(), fontData);
            text.setFont(font);
        }
    }


    /**
     * This method is called whenever user selects the editor - it becomes
     * active.
     */
    @Override
    public void setFocus() {

        super.setFocus();

        GlobalsConfiguration.instance().setActiveModel(m_model);

        // model is reloaded also from
        // ApplicationWorkbenchWindowAdvisor().windowActivated(IWorkbenchWindow)
        boolean isReloaded = UiUtils.checkForReload();

        updateSaveReportMenuCmd();

        StatusModel.instance().updateTestResults(m_model);

        if (m_outlineViewAdapter != null) {
            // this if statement avoids reselection in Outline view, which
            // should not happen if multiple items are shown in table - then
            // all items are replaced with the single item selected by call to setInput()
            if (m_outlineViewAdapter.getInput() != m_model) {
                m_outlineViewAdapter.setInput(m_model,
                                              m_formEditor.getInput(),
                                              isReloaded);
            }
        }
    }


    protected void updateSaveReportMenuCmd() {
        ISourceProviderService sourceProvider = (ISourceProviderService) getSite().getWorkbenchWindow()
                                                                                  .getService(ISourceProviderService.class);

        SaveReportCmdEnabler reportCmdState = (SaveReportCmdEnabler) sourceProvider.getSourceProvider(SaveReportCmdEnabler.VAR_NAME);

        if (reportCmdState != null) {
            // System.out.println("sourceProvider " + reportCmdState);
            reportCmdState.sourceChanged(m_model.getTestReportContainer()
                                                .getNoOfTestResults() > 0);
        }
    }


    public void setFormInput(CTestTreeNode treeNode) {
        
        // always set information for each section editor, because they
        // define appearance of icons in sections tree
        m_formEditor.setInput(m_model, treeNode);
        
        if (m_idxOfCurrentPage == TABLE_EDITOR) {
            m_tableEditor.setInput(m_model, treeNode);
        }
    }

    
    public void setFormInputList(TreeSelection selection) {

        if (selection.size() > 0) {
            CTestTreeNode treeNode = (CTestTreeNode)selection.getFirstElement();

            Activator.log(Status.INFO, "TestCaseEditor.setFormInputList: " + treeNode.getClassName(), null);

            // It would be better to
            // show the last clicked element, but unfortunately TreeSelection
            // or SelectionChangedEvent do not provide this info. The order of
            // items in the selection seems to be the same as in the tree.
            // Even if there is more than one item selected, show editor of the first item
            // in tree view. It does not seem to be annoying, and solves problem of
            // selecting the first node in tree after multiple selection and clicking in 
            // editor view.
            
            // always set information for each section editor, because they
            // define appearance of icons in sections tree
            m_formEditor.setInput(m_model, treeNode);
            
            if (m_idxOfCurrentPage == TABLE_EDITOR) {
                
                if (selection.size() == 1) {
                    m_tableEditor.setInput(m_model, treeNode);
                } else {
                    try {
                        m_tableEditor.setInput(m_model, selection);
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), 
                                              "Can not show table!", 
                                              ex);
                    }
                }
            }
        } else {
            m_formEditor.setInput(m_model, null);
            m_tableEditor.setInput(m_model, (CTestTreeNode)null);
        }

        
    }


    // this method refreshes content providers in all sections
    public void refreshGlobals() {
        m_formEditor.refreshGlobals();
        m_tableEditor.refreshGlobals();
    }


    // notifies also outline view
    public void setSelection(CTestTreeNode testSpec) {

        // no need to set input here - is is set when Outline View notifies
        // editor about selection
        // m_formEditor.setInput(m_model, testSpec);

        if (m_outlineViewAdapter != null) {
            m_outlineViewAdapter.setInput(m_model, testSpec);
        }

        /* TestSpecificationTreeView treeView = TestSpecificationTreeView.getView();
        if (treeView != null) {
            treeView.getOutline().setSelection(m_model, testSpec);
        } */
    }


    /**
     * Selects test case as specified by path of indices - the first index
     * specifies test case on the first level, the second one specifies index of
     * derived test spec, ...
     * 
     * @param indexPath
     */
    public void setSelection(List<Integer> indexPath) {
        CTestSpecification testSpec = m_model.getRootTestSpecification();
        for (Integer idx : indexPath) {
            if (testSpec.getNoOfDerivedSpecs() > idx) {
                testSpec = testSpec.getDerivedTestSpec(idx);
            } else {
                new Throwable().printStackTrace(); // should never happen
                SExceptionDialog.open(Activator.getShell(),
                                      "Can not select test case! "
                                              + indexPath.toString(),
                                      new Exception());
                break;
            }
        }

        setSelection(testSpec);
    }


    public void selectSection(CTestTreeNode testNode, ENodeId nodeId) {
        m_formEditor.setTreeSelection(testNode, nodeId);
    }


    public void selectLineInTable(int currentTableId, int i) {
        m_formEditor.selectLineInTable(currentTableId, i);
    }

	
    class ModelListener extends TestSpecModelListenerAdapter {

        @Override
        public void testSpecTreeStructureChanged(ModelChangedEvent event) {
	        /* TestSpecificationTreeView treeView = TestSpecificationTreeView.getView();
	        if (treeView != null) {
	            treeView.testSpecTreeStructureChanged(event);
	        } */
            if (m_idxOfCurrentPage == FORM_EDITOR) {
                m_formEditor.testSpecTreeStructureChanged(m_model);
            } else {
                m_tableEditor.testSpecTreeStructureChanged(event);
            }

            CTestTreeNode parentTestSpec = event.getOldSpec();
            CTestTreeNode newTestSpec = event.getNewSpec();
            if (parentTestSpec != null) {
                if (m_outlineViewAdapter != null) {
                    m_outlineViewAdapter.refresh();
                }

                if (newTestSpec == null) {
                    // newTestSpec == null for Delete actions, show parent in
                    // such cases
                    newTestSpec = parentTestSpec;
                }

                if (m_outlineViewAdapter != null) {
                    m_outlineViewAdapter.getTreeViewer().reveal(newTestSpec);
                    m_outlineViewAdapter.setSelection(m_model, newTestSpec);
                }
            } else {
                if (m_outlineViewAdapter != null) {
                    m_outlineViewAdapter.refresh();
                }
            }


            /* The following code refreshes file state, but not if model changes
            IEditorInput input = getEditorInput();
            IFile file = ResourceUtil.getFile(input);
            try {
                file.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            } catch (CoreException ex) {
                // do not report to user - it may be more annoying than 
                ex.printStackTrace();
                Activator.log(IStatus.ERROR, 
                              "Can't refresh test case structure in Project Explorer!", 
                              ex);
            } */

            // refresh Project explorer
            IViewPart projectExplorerView = PlatformUI.getWorkbench()
                                                      .getActiveWorkbenchWindow()
                                                      .getActivePage()
                                                      .findView(ProjectExplorer.VIEW_ID);
            if (projectExplorerView instanceof ProjectExplorer) {
                ProjectExplorer explorer = (ProjectExplorer) projectExplorerView;
                explorer.getCommonViewer().refresh(); // use more specific
                // overloads for specific cases (items added/removed, only label
                // changes)
                // if performance will be an issue.
            }

        }


        @Override
        public void testSpecDataChanged(ModelChangedEvent event) {
            /* TestSpecificationTreeView treeView = TestSpecificationTreeView.getView();
            if (treeView != null) {
                treeView.testSpecDataChanged(event);
            }*/

            if (m_idxOfCurrentPage == FORM_EDITOR) {
                m_formEditor.testSpecDataChanged(m_model, event);
            } else {
                m_tableEditor.testSpecDataChanged();
            }

            if (m_outlineViewAdapter != null) {
                CTestTreeNode currentSelection = m_outlineViewAdapter.getTestSpecSelection();
                CTestTreeNode testSpecToSelect = event.getNewSpec();

                if (testSpecToSelect != null && (currentSelection == null
                        || currentSelection.hashCodeAsPtr() != testSpecToSelect.hashCodeAsPtr())) {

                    if (m_idxOfCurrentPage == FORM_EDITOR) {

                        m_outlineViewAdapter.setSelection(m_model,
                                                          testSpecToSelect);

                    } else if (m_idxOfCurrentPage == TABLE_EDITOR) {
                        // since editor normally reflects selection in 
                        // Outline view, is is best to not select anything.
                        // For example, if test ID in table is modified and
                        // multiple test cases are selected, then it is not a 
                        // good itea to change selection to the changed one and 
                        // remove all other test cases from the table as a
                        // consequence.
//                        // in table editor it is parent test specification,
//                        // which shows selected
//                        // test spec., unless the first row is selected
//                        long parentAddr = testSpecToSelect.getParentNode()
//                                                          .hashCodeAsPtr();
//                        long addrCurrent = currentSelection.hashCodeAsPtr();
//
//                        // if current selection is parent of test spec to
//                        // select, select again to
//                        // reflect changes in UI (table needs refresh)
//                        if (addrCurrent == parentAddr) {
//                            m_outlineViewAdapter.setSelection(m_model,
//                                                              testSpecToSelect.getParentNode());
//                        } else {
//                            m_outlineViewAdapter.setSelection(m_model,
//                                                              testSpecToSelect);
//                        }
                    } else {
                        throw new SIllegalStateException("Unknown editor idx: "
                                + m_idxOfCurrentPage);
                    }
                }
            }
        }


        @Override
        public void newInput(ModelChangedEvent event) {
            /* TestSpecificationTreeView treeView = TestSpecificationTreeView.getView();
            if (treeView != null) {
                treeView.setInput(m_model);
            } */
            m_formEditor.setInput(m_model, event.getNewSpec());

            if (m_outlineViewAdapter != null) {
                m_outlineViewAdapter.setInput(m_model,
                                              m_formEditor.getInput(),
                                              true);
            }
        }


        @Override
        public void testSpecTreeRefreshRequired(ModelChangedEvent event) {
            if (m_outlineViewAdapter != null) {
                m_outlineViewAdapter.refresh();
            }
        }


        @Override
        public void testSpecTreeSelectionChanged(ModelChangedEvent event) {
            m_formEditor.setInput(m_model, event.getNewSpec());
        }


        @Override
        public void updateTestResults(ModelChangedEvent event) {

            StatusModel.instance().updateTestResults(m_model);

            updateSaveReportMenuCmd();
        }


        @Override
        public void modelChanged(ModelChangedEvent event) {
            firePropertyChange(PROP_DIRTY);
        }

    }


    public void setTableEditorScriptMenu(List<String> rangeMethods) {
        m_tableEditor.configureScriptFunctionsMenu(rangeMethods);
    }
}
