package si.isystem.itest.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.part.FileEditorInput;

import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;

public class ProjectExplorerActionProvider extends CommonActionProvider {

    private OpenAction m_openAction;

    public ProjectExplorerActionProvider() {
        m_openAction = new OpenAction();
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, m_openAction);
    }
}


class OpenAction extends Action {
    
    @Override
    public void run() {
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (activePage == null) {
            return;
        }
        
        ISelection selection = activePage.getSelection();
        
        /*
        if (selection == null || selection.isEmpty()) {
            return;
        }
        
        if (selection instanceof TreeSelection) {
            TreeSelection treeSel = (TreeSelection) selection;
            Iterator it = treeSel.iterator();
            while (it.hasNext()) {
                Object element = it.next();
                if (element instanceof ModelOutlineNode) {
                    TreePath[] paths = treeSel.getPathsFor(element);
                    for (TreePath path : paths) {
                        
                        List<Integer> indexPathToTestSpecNode = new ArrayList<>();
                        for (int segmentIdx = path.getSegmentCount() - 1; segmentIdx >= 0; segmentIdx--) {
                            Object segment = path.getSegment(segmentIdx);
                            if (segment instanceof ModelOutlineNode) {
                                ModelOutlineNode node = (ModelOutlineNode) segment;
                                indexPathToTestSpecNode.add(0, node.getSeqNo());
                                continue;
                            } else if (segment instanceof IFile) {
                                IFile yamlFile = (IFile)segment;
                                try {
                                    IEditorPart editor = activePage.openEditor(new FileEditorInput(yamlFile), 
                                                                               TestCaseEditorPart.ID);
                                    if (editor instanceof TestCaseEditorPart) {
                                        TestCaseEditorPart tcEditor = (TestCaseEditorPart) editor;
                                        tcEditor.setSelection(indexPathToTestSpecNode);
                                    }
                                } catch (PartInitException ex) {
                                    ex.printStackTrace();
                                    SExceptionDialog.open(Activator.getShell(), 
                                                          "Can't open editor!", 
                                                          ex);
                                }
                            } else {
                                break; // do not handle upper path segments
                            }
                        }
                    }
                }
            }
            
        } */

        List<Integer> indexPathToTestSpecNode = new ArrayList<>();
        CTestSpecification [] testSpecs = new CTestSpecification[1];
        IFile fileInput = TestIDEALinkHelper.getFileAndPath(selection, 
                                                    indexPathToTestSpecNode,
                                                    testSpecs);
        
        if (fileInput == null) {
            return;
        }
        
        try {
            IEditorPart editor = activePage.openEditor(new FileEditorInput(fileInput), 
                                                       TestCaseEditorPart.ID);
            if (editor instanceof TestCaseEditorPart) {
                TestCaseEditorPart tcEditor = (TestCaseEditorPart) editor;
                
                if (testSpecs[0] == null) {
                    tcEditor.setSelection(indexPathToTestSpecNode);
                } else {
                    tcEditor.setSelection(testSpecs[0]);
                }
            }
        } catch (PartInitException ex) {
            ex.printStackTrace();
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can't open editor!", 
                                  ex);
        }
        
    }
    
    
    @Override
    public void runWithEvent(Event event) {
        run();
    }
}
