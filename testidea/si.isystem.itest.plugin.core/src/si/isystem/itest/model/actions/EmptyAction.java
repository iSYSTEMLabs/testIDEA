package si.isystem.itest.model.actions;

import si.isystem.itest.model.AbstractAction;

/**
 * This action class is used for actions, which are not part of undo/redo
 * functionality, but the model must be marked as modified. For example,
 * when user modifies filters in a dialog, he does not expect dialog to be undone,
 * only data in editor. Another example is dry run, where changes are done in 
 * C++ code. 
 *  
 * @author markok
 *
 */
public class EmptyAction extends AbstractAction {

    public EmptyAction(String cmdName) {
        super(cmdName);
    }


    @Override
    public void exec() {
    }


    @Override
    public void undo() {
    }


    @Override
    public void redo() {
    }

}
