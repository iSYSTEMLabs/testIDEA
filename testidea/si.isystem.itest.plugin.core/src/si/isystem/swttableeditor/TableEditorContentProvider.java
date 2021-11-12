package si.isystem.swttableeditor;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class TableEditorContentProvider implements IStructuredContentProvider {
    private ITableEditorModel m_input;

    @Override
    public void dispose() {
        // nothing to to
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        m_input = (ITableEditorModel)newInput;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (m_input == null) {
            return new Object[]{};
        }
        
        return ((ITableEditorModel)inputElement).getRows().toArray();
    }
}
