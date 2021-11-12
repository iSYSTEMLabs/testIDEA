package si.isystem.itest.ui.spec.data;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.handlers.EditPasteCmdHandler;
import si.isystem.itest.handlers.FileOpenCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;


public class TestSpecificationTreeDropListener extends ViewerDropAdapter {

    private TestSpecificationTreeDragListener m_treeDragListener;
    private enum ETransferType {ETestSpecOrGroup, EFile};
    private ETransferType m_transferType;

    public TestSpecificationTreeDropListener(TreeViewer viewer, 
                                             TestSpecificationTreeDragListener treeDragListener) {
        super(viewer);
        // the following does not show insertion line on Linux when dropping between test cases
        //        setFeedbackEnabled(true);
        //        setSelectionFeedbackEnabled(true);
        m_treeDragListener = treeDragListener;
    }

    /**
     * This method is called on drop action. The super.drop() method then calls
     * performDrop(), which is implemented below.  
     */
    @Override
    public void drop(DropTargetEvent event) {
        try {
            super.drop(event);
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not perform drag and drop operation!", ex);
        }
    }

    // This method performs the actual drop
    // We simply add the String we receive to the model and trigger a refresh of the 
    // viewer by calling its setInput method.
    @Override
    public boolean performDrop(Object data) {
        switch (m_transferType) {
        case EFile:
            return dropFile((String[])data);
        case ETestSpecOrGroup:
            return dropYamlString((String)data);
        default:
            SExceptionDialog.open(Activator.getShell(), 
                                  "Invalid type of dropped object!", 
                                  new Exception());
            break;
        }
        return false;
    }
    
    
    private boolean dropFile(String[] data) {
        if (data.length != 1) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Only one file may be dropped, " + data.length + 
                                  " files were dropped!", new Exception());
            return false;
        }
        
        String fileName = data[0];
        try {
            try {
                FileOpenCmdHandler.openEditor(fileName);

                Activator.setTitle(fileName);

            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), 
                                      "Can not open test specification file: " + fileName, 
                                      ex);
            }
                
            return true;
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not open file!", ex);
        }
        
        return false;
    }

    
    private boolean dropYamlString(String yamlTestSpec) {
        
        CTestTreeNode targetTreeNode = (CTestTreeNode)getCurrentTarget();
        
        if (targetTreeNode == null) {
            return false;
        }

        if (targetTreeNode.isGroup()) {
            CTestGroup targetGroup = CTestGroup.cast(targetTreeNode);
            if (targetGroup.isTestSpecOwner()  &&  m_treeDragListener.isFilterGroupDragged()) {
                System.out.println("Can not drop filter group on test case in group!");
                return false;
            }

            return dropOnGroupOrTestSpec(targetTreeNode, 
                                         yamlTestSpec,
                                         !targetGroup.isTestSpecOwner());
        } else if (m_treeDragListener.isTestSpecDragged() || m_treeDragListener.isOwnerGroupDragged()) {
            return dropOnGroupOrTestSpec(targetTreeNode, yamlTestSpec, false);
        }
        
        return false;
    }
        
    
    private boolean dropOnGroupOrTestSpec(CTestTreeNode treeNode, 
                                          String yamlTestSpec,
                                          boolean isTargetFilterGroup) {
        
        CTestTreeNode parentTreeNode = CTestTreeNode.cast(treeNode.getParent());
        int pasteIdx = parentTreeNode.getChildren(true).find(treeNode);

        int location = getCurrentLocation();

        switch (location) {
        case LOCATION_BEFORE:
            break;
        case LOCATION_AFTER:
            pasteIdx++;
            break;
        case LOCATION_ON:
            parentTreeNode = treeNode;
            pasteIdx = -1;
            break;
        case LOCATION_NONE:
            return false;
        default:
            return false;
        }

        CTestBench draggedContainer = m_treeDragListener.getDraggedElements();
        CTestSpecification draggedElements = draggedContainer.getTestSpecification(true);

        // draggedTestSpec != null if D&D is done inside the same app.  
        if (draggedElements != null  &&  isDraggedTestSpecDroppedToDerivedOfItself(draggedElements, 
                                                                                   parentTreeNode)) {
            MessageDialog.openError(Activator.getShell(), 
                                    "Invalid drag & drop operation!", 
                                    "It is not allowed do drop test specification as a child of itself!");
            return false;
        }
        
        // use EditPasteCmdHandler to correctly insert also when multiple test 
        // specifications are dragged
        EditPasteCmdHandler pasteHandler = new EditPasteCmdHandler();
        AbstractAction action = 
                pasteHandler.createPasteActionMultiple(parentTreeNode, pasteIdx, 
                                                       yamlTestSpec, false);
        
        GroupAction grpAction = new GroupAction("Drag-n-Drop");
        grpAction.add(action);
        
        if (draggedElements != null) {
            if (isTargetFilterGroup) {
                // when test case is dropped on group, the original test case 
                // must NOT be deleted - remove them from dragged list
                draggedContainer.setSectionValue(CTestBench.ETestBenchSectionIds.E_SECTION_TEST_CASES.swigValue(), 
                                                 null);
            }
            DeleteTestTreeNodeAction.fillGroupAction(grpAction, draggedContainer);
        }
        TestSpecificationModel.getActiveModel().execAction(grpAction);
        
        return true;
    }
    
// TODO delete this one - replaced by the above method
//    private boolean dropTestSpec(CTestTreeNode treeeNode, String yamlTestSpec) {
//        
//        CTestSpecification selectedTestSpec = CTestSpecification.cast(treeeNode);
//        
//        CTestSpecification parentTestSpec = selectedTestSpec.getParentTestSpecification();
//        int pasteIdx = parentTestSpec.findDerivedTestSpec(selectedTestSpec);
//
//        int location = getCurrentLocation();
//        switch (location) {
//        case LOCATION_BEFORE:
//            break;
//        case LOCATION_AFTER:
//            pasteIdx++;
//            break;
//        case LOCATION_ON:
//            parentTestSpec = selectedTestSpec;
//            pasteIdx = -1;
//            break;
//        case LOCATION_NONE:
//            return false;
//        default:
//            return false;
//        }
//
//        CTestBench draggedContainer = m_treeDragListener.getDraggedElements();
//        CTestSpecification draggedElements = draggedContainer.getTestSpecification(true);
//
//        // draggedTestSpec != null if D&D is done inside the same app.  
//        if (draggedElements != null  &&  isDraggedTestSpecDroppedToDerivedOfItself(draggedElements, 
//                                                                                   parentTestSpec)) {
//            MessageDialog.openError(Activator.getShell(), 
//                                    "Invalid drag & drop operation!", 
//                                    "It is no allowed do drop test specification as a child of itself!");
//            return false;
//        }
//        
//        // use EditPasteCmdHandler to correctly insert also when multiple test 
//        // specifications are dragged
//        EditPasteCmdHandler pasteHandler = new EditPasteCmdHandler();
//        AbstractAction action = 
//                pasteHandler.createPasteActionMultiple(parentTestSpec, pasteIdx, yamlTestSpec);
//        
//        GroupAction group = new GroupAction("Drag-n-Drop");
//        group.add(action);
//        
//        if (draggedElements != null) {
//            DeleteTestSpecAction.fillGroupAction(group, draggedContainer);
//        }
//        TestSpecificationModel.getActiveModel().execAction(group);
//        
//        return true;
//    }

    
//    private boolean isDraggedTestSpecDroppedToDerivedOfItself(CTestSpecification containerOfDragged, 
//                                                              CTestSpecification parentOfDropped) {
//        
//        int numDragged = containerOfDragged.getNoOfDerivedSpecs();
//        for (int i = 0; i < numDragged; i++) {
//            CTestSpecification draggedTestSpec = containerOfDragged.getDerivedTestSpec(i);
//            for (CTestSpecification parent = parentOfDropped; parent != null; 
//                 parent = parent.getParentTestSpecification()) {
//                
//                if (draggedTestSpec.hashCodeAsPtr() == parent.hashCodeAsPtr()) {
//                    return true; 
//                }
//            } 
//        }
//        
//        return false;
//    }
    
    
    private boolean isDraggedTestSpecDroppedToDerivedOfItself(CTestTreeNode containerOfDragged, 
                                                              CTestTreeNode parentOfDropped) {
        
        CTestBaseList draggedElements = containerOfDragged.getChildren(true);
        int numDragged = (int) draggedElements.size();
        for (int i = 0; i < numDragged; i++) {
            CTestTreeNode draggedTestSpec = CTestTreeNode.cast(draggedElements.get(i));
            for (CTestTreeNode parent = parentOfDropped; parent != null; parent = CTestTreeNode.cast(parent.getParent())) {
                
                if (draggedTestSpec.hashCodeAsPtr() == parent.hashCodeAsPtr()) {
                    return true; 
                }
            } 
        }
        
        return false;
    }
    
    
//    @Override 
//	public void dragOver(DropTargetEvent event) {
//    	super.dragOver(event);
//    	event.feedback = DND.FEEDBACK_INSERT_BEFORE;
//    }
    
    @Override
    public boolean validateDrop(Object target, int operation, TransferData transferData) {
        // Drop is valid, when:
        // - filter group is dragged on filter group (but not on test spec owner group)
        
        // - test spec is dragged on test spec
        // - test spec is dragged on filter group (test ID is added to filter group)
        // - test spec is dragged on owner group (test spec is added to owned test spec)
        
        // - owner group is dragged on test spec
        // - owner group is dragged on filter group (test ID of owned test spec is added to filter group)
        // - owner group is dragged on owner group (owned test spec is added to owned test spec)
        if (TextTransfer.getInstance().isSupportedType(transferData)  &&  m_treeDragListener.isDragValid()) {
            if (target instanceof CTestTreeNode) {
                CTestTreeNode targetTreeNode = (CTestTreeNode)target;
                
                if (targetTreeNode.isGroup()) {
                    CTestGroup targetGroup = CTestGroup.cast(targetTreeNode);
                    if (targetGroup.isTestSpecOwner()  &&  m_treeDragListener.isFilterGroupDragged()) {
                        // it is not possible do drop group on test spec owner group
                        return false;
                    }
                    
                    int location = getCurrentLocation();
                    // test case may be dropped only directly on group, not between
                    if ((m_treeDragListener.isTestSpecDragged()  ||  m_treeDragListener.isOwnerGroupDragged()) &&
                         location != LOCATION_ON) {
                        return false;
                    }

                    m_transferType = ETransferType.ETestSpecOrGroup;
                    return true;
                }
                
                // test spec is target
                if (m_treeDragListener.isTestSpecDragged()  ||  m_treeDragListener.isOwnerGroupDragged()) {
                    m_transferType = ETransferType.ETestSpecOrGroup;
                    return true;
                }
            }
        }
        
        if (FileTransfer.getInstance().isSupportedType(transferData)) {
            m_transferType = ETransferType.EFile;
            return true;
        }
        
        return false;
    }
}

