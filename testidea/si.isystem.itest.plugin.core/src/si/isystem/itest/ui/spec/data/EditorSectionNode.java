package si.isystem.itest.ui.spec.data;

import org.eclipse.swt.widgets.Composite;

import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ui.spec.AbstractSectionEditor;
import si.isystem.itest.ui.spec.ISectionEditor;

public class EditorSectionNode {

    private Composite m_panel;
    private AbstractSectionEditor m_sectionEditor;

    
    public EditorSectionNode(AbstractSectionEditor sectionEditor) {
        m_sectionEditor = sectionEditor;
    }

    
    public Composite getPanel(Composite editPanel) {
        if (m_panel == null) {
            m_panel = m_sectionEditor.createPartControl(editPanel);
        }
        return m_panel;
    }


    // called from sections, which are executed after lazy init or when other
    // editor info is required (eg. test result), not GUI panel. 
    public ISectionEditor getSectionEditor() {
        if (m_panel == null) {
            // m_panel should be lazily initialized, to optimize time needed
            // for opening new document. If it is still null here, some lazy init
            // is not performed on some path.
            // Activator.log(Status.INFO, "Panel in section editor should be constructed so far!", null);
        }
        return m_sectionEditor;
    }
    
    // called from locations in TestSpecificationEditorView, where parent panel 
    // is available and editor should be constructed.
    public ISectionEditor getSectionEditor(Composite editPanel) {
        if (m_panel == null) {
            // create editor part only when shown
            m_panel = m_sectionEditor.createPartControl(editPanel);
            
            // refresh also globals in lazy initialized editor part
            TestCaseEditorPart activeEditor = TestCaseEditorPart.getActive();
            if (activeEditor != null) {
                activeEditor.refreshGlobals();
            }
        }
        return m_sectionEditor;
    }
}
