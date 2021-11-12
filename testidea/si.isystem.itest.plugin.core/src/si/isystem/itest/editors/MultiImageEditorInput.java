package si.isystem.itest.editors;

import java.net.URI;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EViewerType;

public class MultiImageEditorInput implements IEditorInput {

    // private IEditorReference m_editorReference;
    private String m_testId;
    private URI m_imageURI;
    private CTestDiagramConfig m_diagConfig;
    private EViewerType m_viewerType;


    public MultiImageEditorInput(String testId, URI imageURI, CTestDiagramConfig diagConfig) {
        m_testId = testId;
        m_imageURI = imageURI;
        
        // save viewer type, as setting in referenced diagConfig may be later 
        // changed by user 
        m_viewerType = diagConfig.getViewerType();
        
        m_diagConfig = diagConfig;
    }
    
    
    
/*    public MultiImageEditorInput(IEditorReference editorReference) {
        super();
        m_editorReference = editorReference;
    } */


    public URI getImageURI() {
        return m_imageURI;
    }


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }


    @Override
    public boolean exists() {
        // this editor should not appear in the MRU File list
        return false;
    }


    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.getMissingImageDescriptor();
    }


    @Override
    public String getName() {
        return m_testId;
    }


    @Override
    public IPersistableElement getPersistable() {
        // can not perist this editor input - all images already exist as 
        // unmodifiable files
        return null;
    }


    @Override
    public String getToolTipText() {
        return m_testId;
    }

    
    @Override
    public boolean equals(Object other) {
        if (other != null  &&  other instanceof MultiImageEditorInput) {
            MultiImageEditorInput otherEditorInput = (MultiImageEditorInput)other;
            
            // viewers must be of the same type
            if (m_viewerType != otherEditorInput.m_viewerType) {
                return false;
            }
            
            if (m_diagConfig.getViewerType() == EViewerType.ESinglePage) {
                return m_imageURI.equals(otherEditorInput.m_imageURI);
            }
            
            return otherEditorInput.m_testId.equals(m_testId);
        }
        
        return false;
    }



    public CTestDiagramConfig getDiagConfig() {
        return m_diagConfig;
    }
}
