package si.isystem.itest.common.ktableutils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.SWTX;

public class KTableFactory {


    public static KTableForStringsModel createStringListModel() {
        return new KTableForStringsModel();
    }
    

    /**
     * Only one of isFillWithLastColumn or isFillWithDummyColumn may be true.
     * @param composite
     * @param model
     * @param layoutData
     * @param isFillWithLastColumn 
     * @param isFillWithDummyColumn
     * @return
     */
    public static KTable createTable(Composite composite, 
                                     final KTableSimpleModelBase model,
                                     Object layoutData,
                                     boolean isFillWithLastColumn,
                                     boolean isFillWithDummyColumn) {
        
        int flags = 0;
        if (isFillWithLastColumn) {
            flags |= SWTX.FILL_WITH_LASTCOL;
        }
        if (isFillWithDummyColumn) {
            flags |= SWTX.FILL_WITH_DUMMYCOL;
        }
        
        final KTable table = new KTable(composite, true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                        SWTX.EDIT_ON_KEY | 
                                        SWTX.MARK_FOCUS_HEADERS | 
                                        flags | 
                                        SWT.BORDER);
        table.setLayoutData(layoutData); 

        table.setModel(model);

        model.addAllDefaultListeners(table);
        
        return table;
    }
    
}
