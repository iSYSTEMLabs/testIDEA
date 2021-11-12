package si.isystem.itest.launch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.launch.FilesListTableModel.ETestInitSource;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;

/**
 * This class launches testIDEA tests when default launch is selected from 
 * context menu in Package explorer or testIDEA editor.
 * 
 * See also: http://www.eclipse.org/articles/Article-Java-launch/launching-java.html
 * 
 * @author markok
 */
public class TestIDEALaunchShortcut implements ILaunchShortcut {

    private static final String DEFAULT_TI_LAUNCH_CONFIG_NAME = "Default testIDEA launch";
    private Exception m_exception;

    /**
     * Can be executed with from context menu in Project Explorer.
     * 
     * @param selection contains File element(s).
     */
    @Override
    public void launch(ISelection selection, final String mode) {
        
        m_exception = null;
        
        if (!(selection instanceof IStructuredSelection)) {
            MessageDialog.openError(Activator.getShell(), 
                                    "Invalid selection!", 
                    "Please select files in Project explorer or similar view");
        }

        IStructuredSelection structsel = (IStructuredSelection)selection;
        List<String> files = new ArrayList<>();

        @SuppressWarnings("rawtypes")
        Iterator it = structsel.iterator();
        while(it.hasNext()) {
            Object selectedItem = it.next();
            if (selectedItem instanceof IFile) {
                IFile file = (IFile) selectedItem;
                IPath absPath = file.getLocation();
                files.add(absPath.toPortableString());
            }
        }

        launchTest(mode, files);
    }


    /**
     * Context menu in editor's Section tree area gives option Run-As, which 
     * then calls this method. 
     */
    @Override
    public void launch(IEditorPart editor, String mode) {
        
        if (editor instanceof TestCaseEditorPart) {
            TestCaseEditorPart tcEditor = (TestCaseEditorPart) editor;
            List<String> files = new ArrayList<>();
            IFile fileName = (IFile)tcEditor.getEditorInput().getAdapter(IFile.class);
            files.add(fileName.getLocation().toPortableString());
            
            launchTest(mode, files);
        } else {
            SExceptionDialog.open(Activator.getShell(), "Internal Error: editor not instance of TestCaseEditorPart!", 
                                  new Exception("Editor is instance of " + editor.getClass().getName()));
        }
    }

    
    public void launchTest(final String mode, List<String> files) {
        try {
            final ILaunchConfiguration launchConfig = createLaunchConfiguration(files);
            
            final TestIDEALaunchDelegate delegate = new TestIDEALaunchDelegate();
            
            try {
                PlatformUI.getWorkbench().getProgressService()
                .busyCursorWhile(new IRunnableWithProgress() {

                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException,
                    InterruptedException {        
            
                        try {
                            delegate.launch(launchConfig, mode, null, monitor);
                        } catch (CoreException ex) {
                            m_exception = ex;
                        }
                }});
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), "Launching with shorcut failed!", ex);
            }
                    
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Launching with default testIDEA launch configuration failed!", 
                                  ex);
        } 
        
        if (m_exception != null) {
            SExceptionDialog.open(Activator.getShell(), "Launching of testIDEA file with shorcut failed!", m_exception);
        }
        
        StatusModel.instance().showTextFromNonUIThread();
    }


    private ILaunchConfiguration createLaunchConfiguration(List<String> files) throws CoreException {
        
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        
        // see plugin.xml, ext. point 'org.eclipse.debug.core.launchConfigurationTypes'
        // for config. type ID.
        ILaunchConfigurationType type =
                manager.getLaunchConfigurationType("si.isystem.itest.launchISystemTest");
        
        ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
     
        for (int i = 0; i < configurations.length; i++) {
            ILaunchConfiguration configuration = configurations[i];
            if (configuration.getName().equals(DEFAULT_TI_LAUNCH_CONFIG_NAME)) {
                configuration.delete();
                break;
            }
        }
        
        ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, 
                                                                       DEFAULT_TI_LAUNCH_CONFIG_NAME);

        // set files
        workingCopy.setAttribute(FileListTab.ATTR_YAML_FILE_LIST, files);
        
        List<String> initSeqSource = new ArrayList<>();
        while (files.size() > initSeqSource.size()) {
            initSeqSource.add(ETestInitSource.EFile.toString());
        }
        workingCopy.setAttribute(FileListTab.ATTR_TEST_INIT_SOURCE, initSeqSource);
        workingCopy.setAttribute(FileListTab.ATTR_IYAML_FILE_ROOT, FileListTab.WORKSPACE_ROOT);
        workingCopy.setAttribute(FileListTab.ATTR_IS_OPEN_REPORT_IN_BROWSER, true);
        
        return workingCopy;
    }
}
