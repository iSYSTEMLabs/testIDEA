
package si.isystem.commons.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

import si.isystem.commons.ISysCommonConstants;
import si.isystem.commons.log.ISysLog;
import si.isystem.exceptions.SException;

import java.awt.Desktop;

public class ISysWorkbenchUtils {
    private static final ISysLog s_log = ISysLog.instance();
    
    //
    // Get editors
    //

    /**
     * Returns all editors with specified editor ID in all workbench windows.
     * @return
     */
    public static List<IEditorReference> getEditors(String tId) {
        List<IEditorReference> editors = new ArrayList<>();
        IWorkbench wb = PlatformUI.getWorkbench();
        for (IWorkbenchWindow wbw : wb.getWorkbenchWindows()) {
            for (IWorkbenchPage page : wbw.getPages()) {
                for (IEditorReference er : page.getEditorReferences()) {
                    if (tId == null  ||  tId.equals(er.getId())) {
                        editors.add(er);
                    }
                }
            }
        }
        return editors;
    }
    
    public static int getEditorCount(String tId) {
        int count = 0;
        IWorkbench wb = PlatformUI.getWorkbench();
        for (IWorkbenchWindow wbw : wb.getWorkbenchWindows()) {
            for (IWorkbenchPage page : wbw.getPages()) {
                for (IEditorReference er : page.getEditorReferences()) {
                    if (tId == null  ||  tId.equals(er.getId())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
    
    public static boolean hasEditor(String tId) {
        IWorkbench wb = PlatformUI.getWorkbench();
        for (IWorkbenchWindow wbw : wb.getWorkbenchWindows()) {
            for (IWorkbenchPage page : wbw.getPages()) {
                for (IEditorReference er : page.getEditorReferences()) {
                    if (tId == null  ||  tId.equals(er.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns all editors with specified editor ID in the given workbench window.
     * @param wbw
     * @return
     */
    public static List<IEditorReference> getEditors(IWorkbenchWindow wbw, String editorId) {
        List<IEditorReference> editors = new ArrayList<>();
        for (IWorkbenchPage page : wbw.getPages()) {
            for (IEditorReference er : page.getEditorReferences()) {
                if (editorId.equals(er.getId())) {
                    editors.add(er);
                }
            }
        }
        return editors;
    }

    public static String getEditorId(File file) {
        return getEditorId(file.getAbsolutePath());
    }
    
    private static String getEditorId(String path) {
        final String lowPath = path.toLowerCase();
        
        for (int i = 0; i < ISysCommonConstants.FILE_TYPE_EXTENSIONS.length; i++) {
            String ext = ISysCommonConstants.FILE_TYPE_EXTENSIONS[i];
            if (lowPath.endsWith(ext)) {
                return ISysCommonConstants.FILE_EDITOR_IDS[i];
            }
        }
        
        String msg = String.format(
                "Failed to open file '%s' - unsupported file extension!", 
                path);
        throw new SException(msg);
    }

    public static IEditorPart openFileInEditor(final String inputPath) {
        final File thisFile = new Path(inputPath).toFile();
        s_log.c("Opening file '%s'", inputPath);
        
        final String pathStr = thisFile.getAbsolutePath();
        final String editorId = getEditorId(pathStr);
        
        IWorkbench wb = PlatformUI.getWorkbench();
        IEditorReference editorRef = getEditorRef(thisFile, editorId);
        if (editorRef != null) {
            IEditorPart editor = editorRef.getEditor(false);
            if (editor != null) {
                final IWorkbenchPage page = editor.getSite().getPage();
                page.showEditor(editorRef);
                s_log.c("Editor for document '%s' already opened - just focusing.",
                        inputPath);
                return null;
            }
        }
        
        IFileStore store = EFS.getLocalFileSystem().getStore(new Path(inputPath));
        FileStoreEditorInput ei = new FileStoreEditorInput(store);
        IEditorPart newEditor = null;
        try {
            IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
            if (wbw == null) {
                s_log.e("Failed to open file '%s' - no active workbench window.", 
                        inputPath);
                return null;
            }
            
            // This wrapper prevents the editor from opening in a new window.
            IWorkbenchPage ap = wbw.getActivePage();
            if (ap == null) {
                s_log.e("Failed to open file '%s' as there is no active page.");
                return null;
            }
            s_log.c("Opening editor for document '%s'", inputPath);
            newEditor = ap.openEditor(ei, editorId);
        }
        catch (WorkbenchException e) {
            s_log.e(e, "Failed to open an editor for '%s'", inputPath);
        }
        
        return newEditor;
    }

    public static IEditorReference getEditorRef(File file) {
        String editorId = getEditorId(file.getAbsolutePath());
        return getEditorRef(file, editorId);
    }
    
    public static IEditorReference getEditorRef(File file, String editorId) {
        IWorkbench wb = PlatformUI.getWorkbench();
        for (IWorkbenchWindow wbw : wb.getWorkbenchWindows()) {
            for (IWorkbenchPage page : wbw.getPages()) {
                for (IEditorReference er : page.getEditorReferences()) {
                    if (editorId.equals(er.getId())) {
                        IEditorInput input;
                        try {
                            input = er.getEditorInput();
                            if (input instanceof FileStoreEditorInput) {
                                File f = getFile(er);
                                if (f.equals(file)) {
                                    return er;
                                }
                            }
                        }
                        catch (PartInitException e) {
                        }
                    }
                }
            }
        }
        return null;
    }

    private static File getFile(IEditorReference editorRef) {
        try {
            FileStoreEditorInput input = (FileStoreEditorInput)editorRef.getEditorInput();
            File file = new File(input.getURI());
            return file;
        }
        catch (PartInitException e) {
            return null;
        }
    }
    
    //
    // Preference store
    // 

    public static void setFocus(Composite cp) {
        final Shell sh = cp.getShell();
        final Control fc = Display.getDefault().getFocusControl();
        if (    sh != null  &&  !sh.isDisposed()  &&
                fc != null  &&  !fc.isDisposed()  &&
                fc.getShell() != null  &&  !fc.getShell().isDisposed() &&
                sh == fc.getShell()) {
            cp.setFocus();
        }
    }
    
    public static boolean openFile(File file) {
        if (!checkOpeningFile(file, false)) {
            return false;
        }
        
        IFileSystem fileSystem = EFS.getLocalFileSystem();
        URI uri = file.toURI();
        IFileStore fileStore = fileSystem.getStore(uri);
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = wbw.getActivePage();
        
        try {
            IDE.openEditorOnFileStore( page, fileStore );
            return true;
        } 
        catch ( PartInitException e) {
            s_log.e(e, "Failed to open file '%s'.", file);
            return false;
        }
    }
    
    public static boolean openFileExternally(File file) {
        if (!checkOpeningFile(file, false)) {
            return false;
        }
        
        if (!Desktop.isDesktopSupported()) {
            s_log.e("Failed to open file in an external editor: Desktop isn't supported.");
            return false;
        }
        
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(file);
            return true;
        } catch (IOException e) {
            s_log.e(e, "Failed to open file '%s' in external editor.", file.getAbsolutePath());
            return false;
        }    
    }

    private static boolean checkOpeningFile(File file, boolean isThrowException) throws SException {
        String errMsg = null;
        if (file == null) {
            errMsg = "Can't open NULL file.";
        }
        else if (!file.exists()) {
            errMsg = String.format("File '%s' doesn't exist.", file.getAbsolutePath());
        }
        else if (!file.isFile()) {
            errMsg = String.format("'%s' isn't a file.", file.getAbsolutePath());
        }
        
        if (errMsg == null) {
            return true;
        }
        else {
            if (isThrowException) {
                throw new SException(errMsg);
            }
            else {
                return false;
            }
        }
    }
}
