package si.isystem.ui.utils;

import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

/**
 * This class contains utility methods for Eclipse Form UI layout. Methods are
 * designed in a way which enables chaining:
 * 
 * <pre>
        FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(component);
 * </pre>
 * 
 * @author markok
 *
 */
public class FDGUIBuilder {

    private FormData m_formData;
    
    // utility methods for FormLayout    
    public FDGUIBuilder() {
        m_formData = new FormData();
    }
    
    /** Factory method. */
    public static FDGUIBuilder _new() {
        return new FDGUIBuilder();
    }
    
    public FDGUIBuilder left(Control control, int offset) {
        m_formData.left = new FormAttachment(control, offset);
        return this;
    }
    
    public FDGUIBuilder left(Control control, int offset, int alignment) {
        m_formData.left = new FormAttachment(control, offset, alignment);
        return this;
    }
    
    public FDGUIBuilder left(int numerator, int offset) {
        m_formData.left = new FormAttachment(numerator, offset);
        return this;
    }
    
    public FDGUIBuilder left(int numerator, int denumerator, int offset) {
        m_formData.left = new FormAttachment(numerator, denumerator, offset);
        return this;
    }
    
    
    public FDGUIBuilder right(Control control, int offset) {
        m_formData.right = new FormAttachment(control, offset);
        return this;
    }
    
    public FDGUIBuilder right(Control control, int offset, int alignment) {
        m_formData.right = new FormAttachment(control, offset, alignment);
        return this;
    }
    
    public FDGUIBuilder right(int numerator, int offset) {
        m_formData.right = new FormAttachment(numerator, offset);
        return this;
    }
    
    public FDGUIBuilder right(int numerator, int denumerator, int offset) {
        m_formData.right = new FormAttachment(numerator, denumerator, offset);
        return this;
    }
    
    
    public FDGUIBuilder top(Control control, int offset) {
        m_formData.top = new FormAttachment(control, offset);
        return this;
    }
    
    public FDGUIBuilder top(Control control, int offset, int alignment) {
        m_formData.top = new FormAttachment(control, offset, alignment);
        return this;
    }
    
    public FDGUIBuilder top(int numerator, int offset) {
        m_formData.top = new FormAttachment(numerator, offset);
        return this;
    }
    
    public FDGUIBuilder top(int numerator, int denumerator, int offset) {
        m_formData.top = new FormAttachment(numerator, denumerator, offset);
        return this;
    }
    
    
    public FDGUIBuilder bottom(Control control, int offset) {
        m_formData.bottom = new FormAttachment(control, offset);
        return this;
    }
    
    public FDGUIBuilder bottom(Control control, int offset, int alignment) {
        m_formData.bottom = new FormAttachment(control, offset, alignment);
        return this;
    }
    
    public FDGUIBuilder bottom(int numerator, int offset) {
        m_formData.bottom = new FormAttachment(numerator, offset);
        return this;
    }
    
    public FDGUIBuilder bottom(int numerator, int denumerator, int offset) {
        m_formData.bottom = new FormAttachment(numerator, denumerator, offset);
        return this;
    }
    

    public void setData(Control control) {
        control.setLayoutData(m_formData);
    }

    // makes control fill the whole parent area
    public static void fill(Control control) {
        FormData fd = new FormData();
        fd.left = new FormAttachment(0, 0);
        fd.top = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        
        control.setLayoutData(fd);
    }
}
