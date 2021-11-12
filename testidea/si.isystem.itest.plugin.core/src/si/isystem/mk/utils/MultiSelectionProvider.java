package si.isystem.mk.utils;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Since workbench does not support dynamic change of selection provider,
 * this class can be used to be registered as selection provider. It
 * supports changing of current provider with setSelectionProviderDelegate().
 * This utility class may be used in workbench parts with 
 * multiple viewers.
 * 
 * See also: https://eclipse.org/articles/Article-WorkbenchSelections/article.html
 *   Be aware that the part's site accepts a single selection provider only, which 
 *   should be registered within the createPartControl() method only:
 *
 *     getSite().setSelectionProvider(provider);
 *
 *   Replacing the selection provider during the lifetime of the part is not 
 *   properly supported by the workbench. If a part contains multiple viewers 
 *   providing selections, like the "Java Hierarchy" view does, a intermediate 
 *   ISelectionProvider implementation has to be provided that allows dynamically 
 *   delegating to the currently active viewer within the part.
 *    
 * @author Marc R. Hoffmann, markok
 */
public class MultiSelectionProvider implements ISelectionProvider {

    private final ListenerList<ISelectionChangedListener> m_selectionListeners = new ListenerList<>();

    private final ListenerList<ISelectionChangedListener> m_postSelectionListeners = new ListenerList<>();

    private ISelectionProvider delegate;

    private ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            if (event.getSelectionProvider() == delegate) {
                fireSelectionChanged(event.getSelection());
            }
        }
    };

    private ISelectionChangedListener postSelectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            if (event.getSelectionProvider() == delegate) {
                firePostSelectionChanged(event.getSelection());
            }
        }
    };

    /**
     * Sets a new selection provider to delegate to. Selection listeners
     * registered with the previous delegate are removed before. 
     * 
     * @param newDelegate new selection provider
     */
    public void setSelectionProviderDelegate(ISelectionProvider newDelegate) {
        if (delegate == newDelegate) {
            return;
        }
        if (delegate != null) {
            delegate.removeSelectionChangedListener(selectionListener);
            if (delegate instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider)delegate).removePostSelectionChangedListener(postSelectionListener);
            }
        }
        delegate = newDelegate;
        if (newDelegate != null) {
            newDelegate.addSelectionChangedListener(selectionListener);
            if (newDelegate instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider)newDelegate).addPostSelectionChangedListener(postSelectionListener);
            }
            fireSelectionChanged(newDelegate.getSelection());
            firePostSelectionChanged(newDelegate.getSelection());
        }
    }

    protected void fireSelectionChanged(ISelection selection) {
        fireSelectionChanged(m_selectionListeners, selection);
    }

    protected void firePostSelectionChanged(ISelection selection) {
        fireSelectionChanged(m_postSelectionListeners, selection);
    }

    private void fireSelectionChanged(ListenerList<ISelectionChangedListener> list, ISelection selection) {
        SelectionChangedEvent event = new SelectionChangedEvent(delegate, selection);
        Object[] listeners = list.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
            listener.selectionChanged(event);
        }
    }

    // IPostSelectionProvider Implementation

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        m_selectionListeners.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(
            ISelectionChangedListener listener) {
        m_selectionListeners.remove(listener);
    }

//    @Override
//    public void addPostSelectionChangedListener(
//            ISelectionChangedListener listener) {
//        m_postSelectionListeners.add(listener);
//    }
//
//    @Override
//    public void removePostSelectionChangedListener(
//            ISelectionChangedListener listener) {
//        m_postSelectionListeners.remove(listener);
//    }

    @Override
    public ISelection getSelection() {
        return delegate == null ? null : delegate.getSelection();
    }

    @Override
    public void setSelection(ISelection selection) {
        if (delegate != null) {
            delegate.setSelection(selection);
        }
    }
}
