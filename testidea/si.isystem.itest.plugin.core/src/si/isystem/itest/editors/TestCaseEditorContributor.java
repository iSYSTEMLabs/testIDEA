//package si.isystem.itest.editors;
//
//import org.eclipse.jface.action.IAction;
//import org.eclipse.jface.action.IMenuManager;
//import org.eclipse.jface.action.IToolBarManager;
//import org.eclipse.ui.IActionBars;
//import org.eclipse.ui.IEditorPart;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.actions.ActionFactory;
//import org.eclipse.ui.ide.IDEActionFactory;
//import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
//import org.eclipse.ui.texteditor.ITextEditor;
//import org.eclipse.ui.texteditor.ITextEditorActionConstants;
//
//import si.isystem.itest.common.ReferenceStorage;
//import si.isystem.itest.ui.spec.TestSpecificationEditorView;
//
///**
// * Manages the installation/uninstallation of global actions for multi-page editors.
// * Responsible for the redirection of global actions to the active editor.
// * Multi-page contributor replaces the contributors for the individual editors in the multi-page editor.
// */
//public class TestCaseEditorContributor extends MultiPageEditorActionBarContributor {
//	private IEditorPart m_activeEditorPart;
//    
//	/**
//	 * Creates a multi-page contributor.
//	 */
//	public TestCaseEditorContributor() {
//		super();
//		createActions();
//	}
//	
//	
//	@Override
//	public void init(IActionBars bars, IWorkbenchPage page) {
//	    super.init(bars, page);
//	    // bars.setGlobalActionHandler(ActionFactory.CUT.getId(), m_handler);
//	}
//	
//	
//	/**
//	 * Returns the action registed with the given text editor.
//	 * @return IAction or null if editor is null.
//	 */
//	protected IAction getAction(ITextEditor editor, String actionID) {
//		return (editor == null ? null : editor.getAction(actionID));
//	}
//	
//	
//	@Override
//	public void setActiveEditor(IEditorPart editor) {
//	    super.setActiveEditor(editor);
//	    // handler4.setActiveEditor(editor);
//	    // handler5.setActiveEditor(editor);
//	}
//	
//	
//	/* (non-JavaDoc)
//	 * Method declared in AbstractMultiPageEditorActionBarContributor.
//	 */
//	@Override
//	public void setActivePage(IEditorPart part) {
//		if (m_activeEditorPart == part)
//			return;
//
//		m_activeEditorPart = part;
//
//		IActionBars actionBars = getActionBars();
//		if (actionBars != null) {
//
//            ReferenceStorage.instance().addAction(ActionFactory.UNDO.getId(), 
//                                                  null);
//            ReferenceStorage.instance().addAction(ActionFactory.REDO.getId(), 
//                                                  null);
//		    
//            ITextEditor editor = (part instanceof ITextEditor) ? (ITextEditor) part : null;
//            TestSpecificationEditorView tsEditor = (part instanceof TestSpecificationEditorView) ? (TestSpecificationEditorView) part : null;
//
//            if (editor != null) {
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.DELETE.getId(),
//    				getAction(editor, ITextEditorActionConstants.DELETE));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.UNDO.getId(),
//    				getAction(editor, ITextEditorActionConstants.UNDO));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.REDO.getId(),
//    				getAction(editor, ITextEditorActionConstants.REDO));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.CUT.getId(),
//    				getAction(editor, ITextEditorActionConstants.CUT));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.COPY.getId(),
//    				getAction(editor, ITextEditorActionConstants.COPY));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.PASTE.getId(),
//    				getAction(editor, ITextEditorActionConstants.PASTE));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.SELECT_ALL.getId(),
//    				getAction(editor, ITextEditorActionConstants.SELECT_ALL));
//    			actionBars.setGlobalActionHandler(
//    				ActionFactory.FIND.getId(),
//    				getAction(editor, ITextEditorActionConstants.FIND));
//    			actionBars.setGlobalActionHandler(
//    				IDEActionFactory.BOOKMARK.getId(),
//    				getAction(editor, IDEActionFactory.BOOKMARK.getId()));
//    			
//            } else if (tsEditor != null) {
//                CommandActionHandler.enableAll();
//                CommandActionHandler.setActionHandlers(actionBars, tsEditor);
//            } else {
//                ReferenceStorage.instance().addAction(ActionFactory.UNDO.getId(), 
//                                                      null);
//                
//                ReferenceStorage.instance().addAction(ActionFactory.REDO.getId(), 
//                                                      null);
//                
//                actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
//                                                  null);
//
//                actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
//                                                  null);
//
//                actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
//                                                  null);
//
//                actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
//                                                  null);
//
//                actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
//                                                  null);
//
//                actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(),
//                                                  null);
//
//                actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
//                                                  null);
//                
//                actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
//                                                  null);
//                
//                actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
//                                                  null);
//            }
//
//            actionBars.updateActionBars();
//		}
//	}
//
//
//	private void createActions() {
//	    // m_cutAction = new RetargetAction(IReadmeConstants.RETARGET2, MessageUtil.getString("Editor_Action2"));
//		/* sampleAction = new Action() {
//			public void run() {
//				MessageDialog.openInformation(null, "iSYSTEM testIDEA", "Sample Action Executed");
//			}
//		};
//		sampleAction.setText("Sample Action");
//		sampleAction.setToolTipText("Sample Action tool tip");
//		sampleAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//				getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
//				*/
//	}
//	
//	
//	@Override
//	public void contributeToMenu(IMenuManager manager) {
//	    /*
//		IMenuManager menu = new MenuManager("Editor &Menu");
//		manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
//		menu.add(sampleAction);
//		*/
//	}
//	
//	
//	@Override
//	public void contributeToToolBar(IToolBarManager manager) {
//	    /*
//		manager.add(new Separator());
//		manager.add(sampleAction);
//		*/
//	}
//}
//
