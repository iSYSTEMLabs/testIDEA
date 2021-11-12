package si.isystem.itest.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.ETristate;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EDerivedTestResultStatus;
import si.isystem.itest.model.ModelOutlineNode;

public class ProjectExplorerLabelProvider implements ILabelProvider {

    List<ILabelProviderListener> m_listeners = new ArrayList<>();
    
    @Override
    public void addListener(ILabelProviderListener listener) {
        System.out.println("ProjectExplorerLabelProvider.addListener()");
        m_listeners.add(listener);
    }


    @Override
    public void dispose() {
    }


    @Override
    public boolean isLabelProperty(Object element, String property) {
        System.out.println("ProjectExplorerLabelProvider.isLabelProperty(): " + property);
        return false;
    }


    @Override
    public void removeListener(ILabelProviderListener listener) {
        m_listeners.remove(listener);
    }


    @Override
    public Image getImage(Object element) {
        if (element instanceof ModelOutlineNode) {
            ModelOutlineNode node = (ModelOutlineNode) element;
            return node.getIcon();
            
        } else if (element instanceof CTestSpecification) {
            
            CTestSpecification testSpec = (CTestSpecification)element;
            return IconProvider.INSTANCE.getTreeViewIcon(testSpec.getRunFlag() != ETristate.E_FALSE, 
                                                         testSpec.getMergedTestScope(),
                                                         null,
                                                         EDerivedTestResultStatus.NO_RESULTS);
        }
        
        return null;
    }


    @Override
    public String getText(Object element) {
        
        if (element instanceof ModelOutlineNode) {
            return element.toString();
            
        } else if (element instanceof CTestSpecification) {
            return ((CTestSpecification)element).getUILabel();
        }
        
        return "/";
    }

    
    public void fireEvents() {
        for (ILabelProviderListener listener : m_listeners) {
            listener.labelProviderChanged(new LabelProviderChangedEvent(this));
        }
    }
}
