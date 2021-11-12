package si.isystem.itest.model;

import java.util.ArrayList;
import java.util.List;

import si.isystem.itest.model.actions.EmptyAction;
import si.isystem.itest.model.actions.GroupAction;


/**
 * This class maintains the list of actions performed on model. Currently the
 * undo level is not limited. 
 */
public class ActionQueue {
    
    private final int SAVE_POINT_LOST = -2;
    private final int EMPTY_QUEUE_IDX = -1;
    private final int MAX_UNDO_LEVEL = 100; // make it settable if required.
    // However, undo must be limited because otherwise a long testIDEA session
    // may crash because of memory problems (we actually have a memory leak). 
    
    private List<AbstractAction> m_actionList;
    private IEventDispatcher m_eventDispatcher;
    private int m_saveIndex = EMPTY_QUEUE_IDX;
    private int m_currentActionIdx = EMPTY_QUEUE_IDX;

    public ActionQueue(IEventDispatcher dispatcher) {
        m_eventDispatcher = dispatcher;
        m_actionList = new ArrayList<AbstractAction>();
    }
    
    public void exec(AbstractAction action) {
        printAction("Exec", action);

        // If action has thrown exception, it is responsible for reverting any 
        // changes. It will also not be added to action queue.
        action.exec();
        
        // remove remaining of the list - actions which were undone
        for (int i = m_actionList.size() - 1; i > m_currentActionIdx; i--) { 
            m_actionList.remove(i);
        }
        m_actionList.add(action);
        m_currentActionIdx = m_actionList.size() - 1;

        if (m_saveIndex >= m_currentActionIdx) { // action at m_currentActionIdx is not saved!
            m_saveIndex = SAVE_POINT_LOST; // saved point is lost - action was undone and then new actions added
        }
        
        // limit the undo level
        if (m_actionList.size() > MAX_UNDO_LEVEL) {
            m_actionList.remove(0);
            m_currentActionIdx--;
            if (m_saveIndex != SAVE_POINT_LOST) {
                if (m_saveIndex == 0) {
                    m_saveIndex = SAVE_POINT_LOST;
                } else {
                    m_saveIndex--;
                }
            }
        }

        // now isModified() will return true
        action.fireEventsOnExec(m_eventDispatcher);
    }
    
    
    private void printAction(String type, AbstractAction cmd) {
        System.err.print(type + ": " + cmd.getName() + " (" + cmd.getClass().getSimpleName() + ")"); 
        if (cmd instanceof GroupAction) {
            GroupAction groupAction = (GroupAction)cmd;
            List<AbstractAction> actions = groupAction.getActions();
            for (AbstractAction action : actions) {
                System.err.print(", " + action.getName() + "(" + cmd.getClass().getSimpleName() + ")");
            }
        }
        System.err.println();
    }

    
    void rememberSavePoint() {
        m_saveIndex = m_currentActionIdx;
    }
    
    boolean isModified() {
        return m_saveIndex != m_currentActionIdx;
    }
    
    boolean isUndoable() {
        if (m_actionList.size() > 0  &&  m_currentActionIdx > EMPTY_QUEUE_IDX) {
            return m_actionList.get(m_currentActionIdx).isUndoable();
        }
        
        return false;
    }
    
    boolean isRedoable() {
        
        if (m_actionList.size() > 0  &&  (m_currentActionIdx + 1) < m_actionList.size()) {  
            return m_actionList.get(m_currentActionIdx + 1).isRedoable();
        }
        
        return false;
    }

    
    void undo() {
        if (m_currentActionIdx > EMPTY_QUEUE_IDX) {
            AbstractAction action = m_actionList.get(m_currentActionIdx);

            printAction("Undo", action);
            action.undo();
            m_currentActionIdx--;
            action.fireEventsOnUndo(m_eventDispatcher);
            
            if (action instanceof EmptyAction) {
                undo(); // undo again to undo the editor action. 'EmptyAction'
                        // is used only to indicate dirty model when it is 
                        // changed with dialogs, for example run config.
            }
        }
    }

    
    void redo() {
        m_currentActionIdx++;
        if (m_currentActionIdx < m_actionList.size()) {
            AbstractAction action = m_actionList.get(m_currentActionIdx);

            printAction("Redo", action);
            action.redo();
            action.fireEventsOnRedo(m_eventDispatcher);

            if (action instanceof EmptyAction) {
                redo(); // see comment for undo() above
            } 
        } else {
            m_currentActionIdx = m_actionList.size() - 1; // point to the end of action list
        }
    }

    
    /**
     * @return name of the next action to be undone, null if there is no such action 
     */
    String getUndoActionName() {
        if (m_currentActionIdx > EMPTY_QUEUE_IDX) {
            return m_actionList.get(m_currentActionIdx).getName();
        }
        return null; 
    }
    
    /**
     * @return name of the next action to be undone, null if there is no such action 
     */
    String getRedoActionName() {
        if ((m_currentActionIdx + 1) < m_actionList.size()) {
            return m_actionList.get(m_currentActionIdx + 1).getName();
        }
        return null; 
    }
    
    /** Clears all actions from the queue. */ 
    void clear() {
        m_actionList.clear();
        m_saveIndex = EMPTY_QUEUE_IDX;
        m_currentActionIdx = EMPTY_QUEUE_IDX;
    }
}
