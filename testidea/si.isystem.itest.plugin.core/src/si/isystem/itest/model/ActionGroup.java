package si.isystem.itest.model;


/**
 * This class contains a list of actions, which should be executed in single 
 * undo/redo step. For example, if one dialog changes several properties, more
 * than one action may be required to update the model. If user executes undo
 * after such operation he may perform only partial undo of the last dialog and 
 * possibly even put the model into inconsistent state.
 * 
 * @author markok
 *
public class ActionGroup extends AbstractAction {

    List<AbstractAction> m_actions = new ArrayList<AbstractAction>();
    
    public void add(AbstractAction action) {
        m_actions.add(action);
    }
    
    
    @Override
    public void exec() {
        for (AbstractAction action : m_actions) {
            action.exec();
        }
    }

    /
     * @return true, if all actions can be undone, false otherwise
     /
    @Override
    public boolean isUndoable() {
        for (AbstractAction action : m_actions) {
            if (!action.isUndoable()) {
                return false;
            }
        }
        
        return true;
    }

    
    /
     * @return true, if action can be redone.
     /
    @Override
    public boolean isRedoable() {
        for (AbstractAction action : m_actions) {
            if (!action.isRedoable()) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void undo() {
        // perform undo in reverse order
        ListIterator<AbstractAction> it = m_actions.listIterator(m_actions.size());
        while (it.hasPrevious()) {
            AbstractAction action = it.previous();
            action.undo();
        }
    }
    
    
    @Override
    public void redo() {
        exec();
    }
}
*/
