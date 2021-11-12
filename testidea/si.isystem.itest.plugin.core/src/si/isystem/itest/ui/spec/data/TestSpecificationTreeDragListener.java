package si.isystem.itest.ui.spec.data;

import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.common.UiUtils;

public class TestSpecificationTreeDragListener implements DragSourceListener {

    private CTestBench m_draggedElements;
    private boolean m_isFilterGroupDragged;
    private boolean m_isOwnerGroupDragged;
    private boolean m_isTestSpecDragged;


    // private final TreeViewer viewer;

    public TestSpecificationTreeDragListener() {
    }

    
    @Override
    public void dragStart(DragSourceEvent event) {
        
        m_isFilterGroupDragged = false;
        m_isOwnerGroupDragged = false;
        m_isTestSpecDragged = false;

        // indicate local drag and drop by saving reference
        m_draggedElements  = UiUtils.getSelectedOutlineNodes(UiUtils.getStructuredSelection(), false);
        
        CTestGroup group = m_draggedElements.getGroup(true);
        m_isFilterGroupDragged = group.hasChildren();
        CTestBaseList draggedGroups = group.getChildren(true);
        int numChildren = (int) draggedGroups.size();
        for (int idx = 0; idx < numChildren; idx++) {
            CTestTreeNode node = CTestTreeNode.cast(draggedGroups.get(idx));
            if (node.isGroup()) {
                CTestGroup childGroup = CTestGroup.cast(node);
                if (childGroup.isTestSpecOwner()) {
                    m_isOwnerGroupDragged = true;
                } else {
                    m_isFilterGroupDragged = true;
                }
            } 
        }
        
        m_isTestSpecDragged = m_draggedElements.getTestSpecification(true).hasChildren();
        
        System.out.println("isFilterGroupDragged = " + m_isFilterGroupDragged + 
                           "\nisOwnerGroupDragged = " + m_isOwnerGroupDragged +
                           "\nisTestSpecDragged = " + m_isTestSpecDragged);
    }
    
    
    @Override
    public void dragSetData(DragSourceEvent event) {

        if (TextTransfer.getInstance().isSupportedType(event.dataType)  &&
                isDragValid()) {
            String strTestSpec = UiUtils.testSpecToTextEditorString(m_draggedElements);
            event.data = strTestSpec; 
        }
    }
    
    
    @Override
    public void dragFinished(DragSourceEvent event) {
        m_draggedElements = null;  // mark end of drag!
    }


    public CTestBench getDraggedElements() {
        return m_draggedElements;
    }
    
    
    public boolean isDragValid() {
        // only test cases or only groups or only owner groups may be dragged at once
        int typeCounter = 0;
        typeCounter += m_isFilterGroupDragged ? 1 : 0;
        typeCounter += m_isTestSpecDragged ? 1 : 0;
        typeCounter += m_isOwnerGroupDragged ? 1 : 0;
        
        return typeCounter == 1;
    }
    
    
    public boolean isTestSpecDragged() {
        return m_isTestSpecDragged;
    }
    
    
    public boolean isFilterGroupDragged() {
        return m_isFilterGroupDragged;
    }


    public boolean isOwnerGroupDragged() {
        return m_isOwnerGroupDragged;
    }
    
}
