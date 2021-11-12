package si.isystem.itest.main;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import si.isystem.itest.ui.spec.StatusView;

public class TestPerspective implements IPerspectiveFactory {

    /**
     * IMPORTANT: This method is called only if either or both of the following:
     * - workspace is cleared (see launch config, tab Main, check box Clear)  
     * - ApplicationWorkbenchAdvisor.initialize() does NOT contain 
     *   wbConfigurer.setSaveAndRestore(true);
     *   
     * See online API help for 'Interface IPageLayout', which contains example.
     */
	@Override
    public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		
		layout.setFixed(true); // must be located BEFORE addView() calls!

		layout.addView(IPageLayout.ID_OUTLINE,  IPageLayout.LEFT, 0.2f, editorArea);
        // layout.addView(TestSpecificationEditorView.ID,  IPageLayout.RIGHT, 0.8f, editorArea);
        
        /* use this folder when adding new tabs to the bottom area
         * IFolderLayout bottomLeft = layout.createFolder("bottomLeft", 
                                                       IPageLayout.BOTTOM, 
                                                       0.50f,
                                                       "topLeft");
        
        bottomLeft.addView(ErrorsView.ID); */
        layout.addView(StatusView.ID,  IPageLayout.BOTTOM, 0.75f, editorArea);
	}

}
