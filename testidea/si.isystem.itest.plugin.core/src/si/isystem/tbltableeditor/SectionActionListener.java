package si.isystem.tbltableeditor;

import si.isystem.connect.CTestBase;
import si.isystem.itest.model.AbstractAction;

/**
 * This listener can be used to modify action for setting table cell content.
 * For example, if cell content change requires refresh of Outline view, this
 * listener can configure action properly.  
 *   
 * @author markok
 */
public interface SectionActionListener {

    /** Called when test base value cell is modified. 
     * @param cTestBase */
    void onTestBaseCellModified(AbstractAction action, CTestBase cTestBase);
}
