package si.isystem.itest.wizards.newtest;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import si.isystem.connect.CTestSpecification;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.actions.GroupAction;

public abstract class GlobalsWizardDataPage extends WizardPage{

    protected NewTCWizardDataModel m_ntcModel;
    
    
    public GlobalsWizardDataPage(String pageName) {
        super(pageName);
    }
    
    /** Creates page in parent. */
    abstract public Composite createPage(Composite parent);
    
    public void setModel(NewTCWizardDataModel ntcModel) {
        m_ntcModel = ntcModel;
    }
    
    /**
     * Should copy data from UI controls to model.
     */
    abstract public void dataToModel();
    abstract public void dataFromModel();
    
    /**
     * @param testSpec  
     */
    public AbstractAction createModelChangeAction(CTestSpecification testSpec) {
        return new GroupAction("Dummy action - derived class should implement this method!");
    }
    
}
