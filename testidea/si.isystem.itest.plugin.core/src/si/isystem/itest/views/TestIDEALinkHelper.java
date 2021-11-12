package si.isystem.itest.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;

import si.isystem.connect.CTestSpecification;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.model.ModelOutlineNode;


/**
 * This class opens editor and moves position to the given selection.
 * 
 * IMPORTANT: This class is used only when the 'link' button (two arrows) in 
 * Project Explorer is pressed. It is not used on Double click - 
 * see ProjectExplorerActionProvider for this case!
 *  
 * @author markok
 *
 */
public class TestIDEALinkHelper implements ILinkHelper {

    public TestIDEALinkHelper() {
    }
    
    
    @Override
    public IStructuredSelection findSelection(IEditorInput input) {

        // System.out.println("findSelection");
        
        /* do not delete - the following commented code returns correct selection
         * path, but IResourceHelper in caller method adds IFile, which should 
         * not be selected, because it scrolls tree so that the file is visible, 
         * while selected test case may get out out view below.
         * Since the behavior is the same for Java and C/C++, returning empty
         * selection is OK - IFile is selected. Use the code below if CNF behaviour
         * is changed. 
         * 
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (activePage == null) {
            return null;
        }

        IEditorPart editor = activePage.findEditor(input);
        if (editor != null) {
            if (editor instanceof TestCaseEditorPart) {
                TestCaseEditorPart tcEditor = (TestCaseEditorPart) editor;
                CTestSpecification testSpec = tcEditor.getActiveTestCase();
                if (testSpec != null) {
                    
                    TreePath[] treePaths = new TreePath[1];
                    List<Object> segments = new ArrayList<>();

                    // segments[0] = new ModelOutlineNode("XXX", "", true, null, 1, 15, 0, file);
                    // First element in list should be File, the last one test spec. node.
                    while (testSpec != null) {
                        segments.add(0, testSpec);
                        testSpec = testSpec.getParentTestSpecification();
                    }
                    
                    IFile file = ResourceUtil.getFile(input);
                    segments.remove(0);
                    // segments.set(0, file);  // removes root test spec, which is 
                    // the last one added in the loop above, 
                    // and which is not shown in tree
                    
                    treePaths[0] = new TreePath(segments.toArray());
                    
                    return new TreeSelection(treePaths);
                }
            }
        }
         */

        return StructuredSelection.EMPTY;
    }


    @Override
    public void activateEditor(IWorkbenchPage page,
                               IStructuredSelection selection) {

        // System.out.println("activateEditor.selection: " + selection.getFirstElement().getClass().getSimpleName());
        List<Integer> indexPathToTestSpecNode = new ArrayList<>();
        CTestSpecification [] testSpecs = new CTestSpecification[1];
        IFile fileInput = getFileAndPath(selection, 
                                         indexPathToTestSpecNode,
                                         testSpecs);

        if (fileInput == null) {
            return;
        }
        
        FileEditorInput input = new FileEditorInput(fileInput);
        IEditorPart editor = page.findEditor(input);
        if (editor != null) {
            page.bringToTop(editor);

            if (editor instanceof TestCaseEditorPart) {
                TestCaseEditorPart tcEditor = (TestCaseEditorPart) editor;
                
                if (testSpecs[0] == null) {
                    tcEditor.setSelection(indexPathToTestSpecNode);
                } else {
                    tcEditor.setSelection(testSpecs[0]);
                }
            }
        }
    }
    

    /**
     * If selection contains ModelOutlineNode, this method returns file
     * and index path to the item. If selection contains more than one item,
     * only the first one is returned, others are ignored.
     * 
     * @param selection
     * @return null if path and/or file can't be found
     */
    public static IFile getFileAndPath(ISelection selection, 
                                       List<Integer> indexPathToTestSpecNode,
                                       CTestSpecification [] testSpecs) {
        
        if (selection == null || selection.isEmpty()) {
            return null;
        }
        
        indexPathToTestSpecNode.clear();
        testSpecs[0] = null;
        
        if (selection instanceof TreeSelection) {
            TreeSelection treeSel = (TreeSelection) selection;
            // only one editor can be selected, so take the first selection
            @SuppressWarnings("rawtypes") 
            Iterator it = treeSel.iterator();
            
            while (it.hasNext()) {
                Object element = it.next();
                if (element instanceof ModelOutlineNode) {
                    TreePath[] paths = treeSel.getPathsFor(element);
                    for (TreePath path : paths) {

                        indexPathToTestSpecNode.clear();
                        for (int segmentIdx = path.getSegmentCount() - 1; segmentIdx >= 0; segmentIdx--) {
                            Object segment = path.getSegment(segmentIdx);
                            if (segment instanceof ModelOutlineNode) {
                                ModelOutlineNode node = (ModelOutlineNode) segment;
                                indexPathToTestSpecNode.add(0, node.getSeqNo());
                                continue;
                            } else if (segment instanceof IFile) {
                                IFile yamlFile = (IFile)segment;

                                return yamlFile;
                            } else {
                                break; // do not handle upper path segments
                            }
                        }
                    }
                } else if (element instanceof CTestSpecification) {
                    
                    testSpecs[0] = (CTestSpecification)element;
                    
                    TreePath[] paths = treeSel.getPathsFor(element);
                    for (TreePath path : paths) {
                        for (int segmentIdx = path.getSegmentCount() - 1; segmentIdx >= 0; segmentIdx--) {
                            Object segment = path.getSegment(segmentIdx);
                            if (segment instanceof CTestSpecification) {
                                continue;
                            } else if (segment instanceof IFile) {
                                return (IFile)segment;
                            } else {
                                break; // do not handle upper path segments
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
