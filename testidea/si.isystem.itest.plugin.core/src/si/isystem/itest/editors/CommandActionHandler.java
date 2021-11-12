//package si.isystem.itest.editors;
//
//import org.eclipse.core.commands.AbstractHandler;
//import org.eclipse.core.commands.ExecutionException;
//import org.eclipse.jface.action.Action;
//import org.eclipse.ui.IActionBars;
//import org.eclipse.ui.actions.ActionFactory;
//import org.eclipse.ui.ide.IDEActionFactory;
//
//import si.isystem.exceptions.SExceptionDialog;
//import si.isystem.itest.common.ReferenceStorage;
//import si.isystem.itest.handlers.EditCopyCmdHandler;
//import si.isystem.itest.handlers.EditCutCmdHandler;
//import si.isystem.itest.handlers.EditPasteCmdHandler;
//import si.isystem.itest.handlers.EditRedoCmdHandler;
//import si.isystem.itest.handlers.EditSelectAllCmdHandler;
//import si.isystem.itest.handlers.EditUndoCmdHandler;
//import si.isystem.itest.handlers.FilePropertiesCmdHandler;
//import si.isystem.itest.main.Activator;
//
///**
// * This class maps Command Handlers to Actions - needed to map existing handler 
// * to Eclipse Actions in Edit and File menus.
// */
//public class CommandActionHandler extends Action {
// 
//    private static CommandActionHandler m_undoHandler = new CommandActionHandler(new EditUndoCmdHandler());
//    private static CommandActionHandler m_redoHandler = new CommandActionHandler(new EditRedoCmdHandler());
//    
//    private static CommandActionHandler m_cutHandler = new CommandActionHandler(new EditCutCmdHandler());
//    // private static CommandActionHandler m_copyHandler = new CommandActionHandler(new EditCopyCmdHandler());
//    private static CommandActionHandler m_pasteHandler = new CommandActionHandler(new EditPasteCmdHandler());
//    
//    private static CommandActionHandler m_selectAllHandler = new CommandActionHandler(new EditSelectAllCmdHandler());
//    
//    private static CommandActionHandler m_propertiesHandler = new CommandActionHandler(new FilePropertiesCmdHandler());
//    
//    
//    private AbstractHandler m_handler;
//
//    public CommandActionHandler(AbstractHandler commandHandler) {
//        m_handler = commandHandler; 
//    }
//    
//
//    public static void enableAll() {
////        m_copyHandler.m_handler.setEnabled(null);
//    }
//    
//    
//    @Override
//    public void run() {
//        try {
//            // if you need event, see method runWithEvent()
//            m_handler.execute(null);
//        } catch (ExecutionException ex) {
//            SExceptionDialog.open(Activator.getShell(), "Execution failed!", ex);
//        }
//    }
//    
//    
//    public CommandActionHandler init(Object tsEditor) {
//        
//        if (tsEditor == null) {
//            return null;
//        }
//        
//        setEnabled(m_handler.isEnabled());
//        return this;
//    }
//    
//    
//    public static void setActionHandlers(IActionBars actionBars,
//                                         Object tsEditor) {
//        // register actions, so that model can change enabled state
//        ReferenceStorage.instance().addAction(ActionFactory.UNDO.getId(), 
//                                              m_undoHandler);
//        ReferenceStorage.instance().addAction(ActionFactory.REDO.getId(), 
//                                              m_redoHandler);
//        
//        actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
//                                          m_undoHandler.init(tsEditor));
//
//        actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
//                                          m_redoHandler.init(tsEditor));
//
//        actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
//                                          m_cutHandler.init(tsEditor));
//
//       // actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
////                                          m_copyHandler.init(tsEditor));
//
//        actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
//                                          m_pasteHandler.init(tsEditor));
//
//        actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(),
//                                          m_propertiesHandler.init(tsEditor));
//
//        // these actions are not yet implemented in testIDEA forms view
//        // Select all - currently only single section may be selected
//        // Find - makes sense to search for string in file
//        // Bookmark - makes sense to record a bookmark with Alt+<number>
//        actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
//                                          m_selectAllHandler);
//        
//        actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
//                                          null);
//        
//        actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
//                                          null);
//    }
//}