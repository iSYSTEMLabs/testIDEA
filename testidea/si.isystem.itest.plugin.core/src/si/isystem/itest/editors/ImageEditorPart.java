package si.isystem.itest.editors;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EViewerType;
import si.isystem.itest.diagrams.ViewerComposite;
import si.isystem.ui.utils.SelectionAdapter;

/**
 * This class displays diagrams in multi-page editor and also in single page 
 * editor - see m. init(), instance of editorInput.
 * 
 * @author markok
 *
 */
public class ImageEditorPart extends MultiPageEditorPart 
implements IResourceChangeListener {

    public static final String ID = "si.isystem.itest.editors.ImageEditorPart";

    private List<ViewerComposite> m_imageComposites = new ArrayList<>();

    private boolean m_isNewEditor;

    private boolean IS_DEBUG = false; // the sequence of image panel creation 
                                     // is a bit complex because of multipage
                                     // editors and Eclipse callbacks.
    public ImageEditorPart() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }


    /**
     * @return currently active instance of this class, else null.
     */
    public static ImageEditorPart getActive() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null) {
            IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
            if (activeWindow != null) {
                IWorkbenchPage activePage = activeWindow.getActivePage();
                if (activePage != null) {
                    IEditorPart activeEditor = activePage.getActiveEditor();
                    if (activeEditor instanceof ImageEditorPart) {
                        return (ImageEditorPart)activeEditor;
                    }
                }
            }
        }
        
        return null;
    }

    
    private void addLastCompositeToPage() {
        
        int lastIdx = m_imageComposites.size() - 1;
        ViewerComposite imageComposite = m_imageComposites.get(lastIdx);
        Composite imagePanel;
        if (getEditorInput() instanceof MultiImageEditorInput) {
            if (IS_DEBUG) System.out.println("Multipage image composite");
            // imagePanel = imageComposite.createComposite(getContainer());
            imagePanel = imageComposite.createCompositeWithFileNameAndCloseButton(getContainer());
            imageComposite.setCloseListener(new SelectionAdapter() {
                
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int idx = getActivePage();
                    if (idx >= 0) {
                        removePage(idx);
                        m_imageComposites.remove(idx);
                    }
                }
            });
        } else {
            if (IS_DEBUG) System.out.println("Single page image composite");
            imagePanel = imageComposite.createComposite(getContainer());
        }
        
        int pageIndex = addPage(imagePanel);

        // get last part of file name without extension
        String tabText = imageComposite.getFileName();
        
        // if '-' is not found, lastIndexOf() returns -1, which results in 0.
        int dashIndex = tabText.lastIndexOf('-') + 1;
        int dotindex = tabText.lastIndexOf('.');
        if (dotindex == -1) {
            dotindex = tabText.length();
        }
        tabText = tabText.substring(dashIndex, dotindex);
        setPageText(pageIndex, tabText);
        setActivePage(pageIndex);
    }
    
    
    private void openSVGFile() {
        int lastIdx = m_imageComposites.size() - 1;
        if (IS_DEBUG) System.out.println("openSVGFile(), lastIdx = " + lastIdx);
        ViewerComposite imageComposite = m_imageComposites.get(lastIdx);
        imageComposite.openFileInCanvas();
    }
    
    
    @Override
    protected void createPages() {
        if (IS_DEBUG) System.out.println("createPages()");
        addLastCompositeToPage();
    }
    
    
    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        for (ViewerComposite composite : m_imageComposites) {
            composite.dispose();
        }
        
        super.dispose();
    }
    
    
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }
    
    
    @Override
    public boolean isDirty() {
        return false;
    }
    
    
    /**
     * Saves the multi-page editor's document.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    
    @Override
    public void doSaveAs() {
    }

    
    /**
     * This method is called by framework only when editor is created. If it
     * already exists, this method is not called.
     */
    @Override
    public void init(IEditorSite site, IEditorInput editorInput)
                                                     throws PartInitException {
        
        if (IS_DEBUG) System.out.println("init()");
        
        if (editorInput instanceof FileStoreEditorInput) {
            FileStoreEditorInput fileEditorInput = (FileStoreEditorInput)editorInput;
            setPartName(fileEditorInput.getName());

            URI uri = fileEditorInput.getURI();
            
            addImageComposite(uri, null);
        } else if (editorInput instanceof MultiImageEditorInput) {
            MultiImageEditorInput imageEditorInput = (MultiImageEditorInput)editorInput;
            CTestDiagramConfig diagConfig = imageEditorInput.getDiagConfig();
            URI uri = imageEditorInput.getImageURI();
            File file = URIUtil.toFile(uri);

            if (diagConfig.getViewerType() == EViewerType.ESinglePage) {
                setPartName(file.getName());
                addImageComposite(uri, diagConfig);
            } else {
                setPartName(imageEditorInput.getName());
                addImageComposite(uri, diagConfig);
            }
        }
        
        m_isNewEditor = true;
        
        super.init(site, editorInput); // if super.init() is not called ==> Error: Site is incorrect
    }


    /**
     * This method creates image composite, but page it is not added to editor. 
     * @see #createPages()
     * @see #addImagePage(URI, CTestDiagramConfig)
     * */ 
    private void addImageComposite(URI uri, CTestDiagramConfig diagConfig) {
        
        if (IS_DEBUG) System.out.println("addImageComposite()");
        
        File file = URIUtil.toFile(uri);

        ViewerComposite imageComposite = ViewerComposite.create(file, 
                                                                diagConfig);
        m_imageComposites.add(imageComposite);
        imageComposite.setFile(file);
    }
    
    
    public void addImagePage(URI uri, CTestDiagramConfig diagConfig) {
        
        // this state is required, because editor can not be created without 
        // at least one page - so this page must be created in init(), and then createPages()
        // is automatically called by framework. This does not happen, when subsequent pages
        // are added with this call, though.
        if (m_isNewEditor) {
            m_isNewEditor = false;
            // if new editor was just opened, composite was already created in
            // init() and createPages() methods of this class. 
            if (IS_DEBUG) System.out.println("addImagePage(), newEditor");
            openSVGFile();
            return;
        }
        
        if (IS_DEBUG) System.out.println("addImagePage(), existing Editor");
        
        // if file is already opened, activate the page showing it
        int pageIdx = 0;
        for (ViewerComposite composite : m_imageComposites) {
             
            String existingCompositeFileName = composite.getFileName();
            String addedCompositeFileName = URIUtil.toFile(uri).toString();
            
            if (existingCompositeFileName.equals(addedCompositeFileName)) {
                setActivePage(pageIdx);
                return;
            }
            pageIdx++;
        }
        
        // open new page
        addImageComposite(uri, diagConfig);
        addLastCompositeToPage();
        openSVGFile();
    }
    
    
    /**
     * Calculates the contents of page 2 when it is activated.
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
    }
    
    
    /**
     * Closes all project files on project close.
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event){
        if(event.getType() == IResourceChangeEvent.PRE_CLOSE){
            
            Display.getDefault().asyncExec(new Runnable(){
                @Override
                public void run(){
            
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
                    
                    for (int i = 0; i<pages.length; i++){
//                        if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
//                            IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
//                            pages[i].closeEditor(editorPart,true);
//                        }
                    }
                }            
            });
        }
    }
    
    
    /**
     * This method is called whenever user selects the editor - it becomes active.
     */
    @Override
    public void setFocus() {
        super.setFocus();
    }
}

