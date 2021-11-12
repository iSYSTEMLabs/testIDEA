package si.isystem.itest.ui.spec;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;

import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestResult;

abstract public class AbstractSectionEditor implements ISectionEditor {

    protected String NODE_PATH = null; // valid setting for all leaf nodes, either in CTestSpecification
                                       // or in coverage and profiler.
    
    /**
     * This method is called once at creation time - it creates all controls and lays 
     * them out. It also adds listeners to controls.
     * 
     * @param parent composite
     * @return composite containing all UI components
     */
    abstract public Composite createPartControl(Composite parent);
    

    @Override 
    public boolean isMerged() {
        return false;
    }
    
    
    @Override
    public boolean hasErrorStatus() {
        return false;
    }

    
    @Override
    public boolean isError(CTestResult result) {
        return false;
    }

    
    @Override
    public boolean isError(CTestGroupResult result) {
        return false;
    }

    
    /**
     * Should return true, if the section is active, for example coverage
     * must be also started, it is not enough that the section is not empty.
     */
    @Override
    public boolean isActive() {
        return true;
    }
    

    public void refresh() {
        fillControlls();
    }

    
    // Methods for section editors with table
    
    @Override
    public void selectLineInTable(int tableId, int lineNo) {
    }


    protected ScrolledComposite configureScrolledComposite(final ScrolledComposite scrolledPanel,
                                                           final Composite mainPanel) {
        scrolledPanel.setContent(mainPanel);
        scrolledPanel.setExpandHorizontal(true);
        scrolledPanel.setExpandVertical(true);
        
        scrolledPanel.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
              scrolledPanel.setMinSize(mainPanel.computeSize(SWT.DEFAULT,
                                                              SWT.DEFAULT));
            }
        });
        
        return scrolledPanel;
    }
    
    
    @Override
    public String getNodePath() {
        return NODE_PATH;
    }
}
