package si.isystem.tbltableeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;


/**
 * Instance of this class is registered with ViewPart to provde current selection
 * in KTable. Based on selection type the context menu is shown. 
 *   
 * @see CellSelctionListener
 * @see TestBaseListTable.createControl
 *  
 * @author markok
 *
 */
public class TestBaseTableSelectionProvider implements ISelectionProvider {

    private List<ISelectionChangedListener> m_selChangedListeners;
    private ISelection m_selection;
    
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        if (m_selChangedListeners == null) {
            m_selChangedListeners = new ArrayList<ISelectionChangedListener>();
        }
        m_selChangedListeners.add(listener);
    }


    @Override
    public ISelection getSelection() {
        return m_selection;
    }


    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        if (m_selChangedListeners != null) {
            m_selChangedListeners.remove(listener);
        }
    }


    @Override
    public void setSelection(ISelection selection) {
        
        m_selection = selection;

        if (m_selChangedListeners != null) {
            for (ISelectionChangedListener listener : m_selChangedListeners) {
                listener.selectionChanged(new SelectionChangedEvent(this, selection));
            }
        }
    }

}


