package si.isystem.itest.ui.spec.data;

import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ActionQueue;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.IEventDispatcher;
import si.isystem.itest.model.ModelChangedEvent;


/**
 * This class can be used in dialogs, which do need undo-redo functionality.
 * 
 * @author markok
 */
public class ActionExecutioner implements IActionExecutioner {

    private ActionQueue m_actionQueue = new ActionQueue(new IEventDispatcher() {
        // no events need to be fired
        @Override public void fireEvent(ModelChangedEvent event) {}
    });
    
    @Override
    public void execAction(AbstractAction action) {
        m_actionQueue.exec(action);            
    }
}


