package si.isystem.itest.ui.spec;

import si.isystem.itest.model.AbstractAction;

// This interface is used by HierarchyControlCheckBox, to get additional actions
// on checkbox click, for example actions to clear section when inheritance is 
// set with check box.
public interface BoolActionProvider {

    AbstractAction getClearAction();
    AbstractAction getCopyAction();
}
