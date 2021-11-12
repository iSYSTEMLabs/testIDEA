package si.isystem.itest.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.model.ModelOutlineNode;
import si.isystem.itest.model.TestSpecificationModel;

public class ProjectExplorerContentProvider implements ITreeContentProvider {

    @Override
    public void dispose() {
    }


    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
/*        String oi = oldInput == null ? "null" : oldInput.getClass().getSimpleName();
        String ni = newInput == null ? "null" : newInput.getClass().getSimpleName();
        System.out.println("input changed: " + oi + " / " + ni);
        */
    }


    @Override
    public Object[] getElements(Object inputElement) {
        System.out.println("getElements " + inputElement.getClass().getName());
        return new String[]{"Ax", "B", "C"};
    }


    @Override
    public Object[] getChildren(Object parentElement) {

        System.out.println(hashCode() + "getChildren " + parentElement.getClass().getName());
        /*
        
        if (parentElement instanceof IFile) {
            System.out.println("getChildren " + ((IFile)parentElement).getLocation().toOSString());
        } else if (parentElement instanceof ModelOutline) {
            System.out.println("getChildren " + ((ModelOutline)parentElement).getLabel());
        } */
        
        Object[] children;
        
        if (parentElement instanceof IFile) {
            IFile resourceFile = (IFile) parentElement;
            
            IEditorPart editor = ResourceUtil.findEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), 
                                             resourceFile);
            
            if (editor instanceof TestCaseEditorPart) {
                // editor is opened, let's use it's model
                TestCaseEditorPart tcEditor = (TestCaseEditorPart)editor;
                TestSpecificationModel model = tcEditor.getModel();
                CTestSpecification rootNode = model.getRootTestSpecification();
                return getChildren(rootNode);
                
            } else {
                
                // Editor is not opened for this resource, we have to parse it now.
                // Special model is created and full model is discarded to save
                // resources
                String fName = resourceFile.getLocation().toOSString();
                TestSpecificationModel model = new TestSpecificationModel();
                try {
                    model.openTestSpec(fName, 0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    model.clearModel(); // nothing will be visible in project explorer
                }
                
                ModelOutlineNode rootNode = model.getOutline();
                model.clearModel();  // release memory
                model = null;
                
                children = rootNode.getChildren();
            }

            return children;
        
        } else if (parentElement instanceof ModelOutlineNode) {
            
            ModelOutlineNode node = (ModelOutlineNode)parentElement; 
            return node.getChildren();

        } else if (parentElement instanceof CTestSpecification) {
            
            return getTestSpecChildren((CTestSpecification)parentElement);
        }
        
        return null; // new String[]{"C1", "C2"};
    }


    private Object[] getTestSpecChildren(CTestSpecification testSpec) {
        int noOfDerivedSpecs = (int)testSpec.getNoOfDerivedSpecs();
        Object []children = new Object[noOfDerivedSpecs];
        
        for (int i = 0; i < noOfDerivedSpecs; i++) {
            children[i] = testSpec.getDerivedTestSpec(i);
        }
        
        return children;
    }


    @Override
    public Object getParent(Object element) {
        
        System.out.println("getParent: " + element.getClass().getSimpleName());
        if (element instanceof ModelOutlineNode) {
            // never called with this class, since it is called only when link
            // button in Project Explorer is pressed AND file is opened in editor - 
            // but at that time the model is switched to TestSpecificationModel. 
            // return ((ModelOutlineNode)element).getParent();
            
        } else if (element instanceof CTestSpecification) {
            CTestBase parent = ((CTestSpecification)element).getParent();
            if (parent != null  &&  parent.getParent() == null) {  // it is root test case
                return null;  // should return IFile here, but this method is 
                // never called if we provide full selection path in LinkHelper.findSelection()
            }
            return parent;
        }
        return null;
    }


    @Override
    public boolean hasChildren(Object element) {
        // System.out.println(hashCode() + "hasChildren " + element.getClass().getSimpleName());
        if (element instanceof ModelOutlineNode) {
            ModelOutlineNode node = (ModelOutlineNode)element;
            return node.getChildren().length > 0;
            
        } else if (element instanceof CTestSpecification) {
            CTestSpecification node = (CTestSpecification)element;
            return node.getNoOfDerivedSpecs() > 0;
            
        } else if (element instanceof IFile) {
            // System.out.println("hasChildren " + ((IFile)element).getLocation().toOSString());
            return true; // assume iyaml file has children, because in most 
                         // cases it has them. Redundant expansion icon in tree
                         // disappears after first click anyway.
        }
        return false;
    }

}
