package si.isystem.itest.wizards;

import java.util.List;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;

import de.kupzog.ktable.KTable;
import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBase;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.common.ktableutils.KTableFactory;
import si.isystem.itest.common.ktableutils.KTableForStringsModel;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This class implements test case generator wizard for verification expressions
 * for pre-conditions, stubs, and test points.
 * 
 * @author markok
 */

public class TCGen_PreCond_Stub_TP_ExpressionsPage extends WizardPage {

    TCGenAsserts m_assertsConfig;
    
    private KTableForStringsModel m_expressionsTableModel; 
    private KTableForStringsModel m_preConditionsTableModel;
    private KTableForStringsModel m_stubsExprsTableModel;
    private KTableForStringsModel m_testPointsExprsTableModel;
    
    private String[] m_varPropposals; 
    private String[] m_varDescriptions;
            
    
    protected TCGen_PreCond_Stub_TP_ExpressionsPage(TCGenAsserts tcGenAsserts) {
      super("Expected expressions");
      setTitle("Expected expressions");
      setDescription("These expressions are used to verify target state before, during, and after test.");
      
      m_assertsConfig = tcGenAsserts;
    }
    

    public void setNewData(TCGenAsserts dryRunConfig) {
        m_assertsConfig = dryRunConfig;
        m_expressionsTableModel.setData(dryRunConfig.getExpressions());
        m_preConditionsTableModel.setData(dryRunConfig.getPreConditionExpressions());
        m_stubsExprsTableModel.setData(dryRunConfig.getStubExpressions());
        m_testPointsExprsTableModel.setData(dryRunConfig.getTestPointExpressions());
    }

    
    public void setAutoCompleteProposals(String [] varProposals, 
                                         String [] descriptions) {
        m_varPropposals = varProposals;
        m_varDescriptions = descriptions;
    }
    
    
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }
    
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        // wizard dialog size is set in handler
        container.setLayout(new MigLayout("fill", "[fill]", "[fill]"));

        KGUIBuilder builder = new KGUIBuilder(container);

        createTablesSash(builder);

        setControl(container);
    }

    
    private void createTablesSash(KGUIBuilder builder) {
        
        SashForm sash = new SashForm(builder.getParent(), SWT.VERTICAL);
        sash.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_GRAY));
        
        // wizard dialog size is set in handler
        sash.setLayoutData("");

        m_expressionsTableModel = createAssertExpressionsTable(sash);
        m_preConditionsTableModel = createPreConditionsTable(sash);
        m_stubsExprsTableModel = createStubsTable(sash);
        m_testPointsExprsTableModel = createTestPointsTable(sash);

        sash.setWeights(new int[]{40, 20, 20, 20});

        verifyModel();
    }


    private KTableForStringsModel createAssertExpressionsTable(SashForm sash) {
        
        Composite panel = new Composite(sash, SWT.BORDER);
        panel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[fill]"));
        
//        KGUIBuilder tableBuilder = new KGUIBuilder(panel);
//        tableBuilder.label("Section 'Expected' of each generated test case:",
//                           "wrap");
        return createTable(panel, 
                           "Expressions for section 'Expected'",
                           m_assertsConfig.getExpressions());
    }


    private KTableForStringsModel createPreConditionsTable(SashForm sash) {
        
        Composite preConditionsPanel = new Composite(sash, SWT.BORDER);
        preConditionsPanel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[fill]"));
        
//        KGUIBuilder preConditionsBuilder = new KGUIBuilder(preConditionsPanel);
//        preConditionsBuilder.label("Section 'Pre-conditions' of each generated test case:",
//                                   "gaptop 15, wrap");
        return createTable(preConditionsPanel, 
                           "Pre-condition expressions",
                           m_assertsConfig.getPreConditionExpressions());
    }


    private KTableForStringsModel createStubsTable(SashForm sash) {
        
        Composite stubsPanel = new Composite(sash, SWT.BORDER);
        stubsPanel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[fill]"));  

//        KGUIBuilder stubsBuilder = new KGUIBuilder(stubsPanel);
//        stubsBuilder.label("Expressions for stub step of each generated test case:",
//                           "gaptop 15, wrap");

        return createTable(stubsPanel,
                           "Stub step expressions",
                           m_assertsConfig.getStubExpressions());
    }


    private KTableForStringsModel createTestPointsTable(SashForm sash) {
        
        Composite stubsPanel = new Composite(sash, SWT.BORDER);
        stubsPanel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[fill]"));  

//        KGUIBuilder stubsBuilder = new KGUIBuilder(stubsPanel);
//        stubsBuilder.label("Expressions for test point step of each generated test case:",
//                           "gaptop 15, wrap");

        return createTable(stubsPanel,
                           "Test point step expressions",
                           m_assertsConfig.getTestPointExpressions());
    }


    private KTableForStringsModel createTable(Composite inputPanel,
                                              String columnTitle,
                                              List<String[]> data) {

        KTableForStringsModel tableModel = KTableFactory.createStringListModel();
        tableModel.setData(data);
        tableModel.setColumnTitles(new String[] {columnTitle});
        tableModel.setColumnWidths(new int[] {50, 550});
        tableModel.setAutoCompleteProposals(1, 
                                            new VariablesContentProposal(m_varPropposals, 
                                                                         m_varDescriptions),
                                            ContentProposalAdapter.PROPOSAL_INSERT);
        
        final KTable table = KTableFactory.createTable(inputPanel, 
                                                       tableModel, 
                                                       "", false, true);
        
        tableModel.addAllDefaultListeners(table);
        tableModel.setEnabled(true);
        
        tableModel.addModelChangedListener(new IKTableModelChangedListener() {
            
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                if (isRedrawNeeded) {
                    table.redraw();
                }

                verifyModel();
            }
        });
        
        return tableModel;
    }
    
    
    private void verifyModel() {
        
        String errMsg = m_expressionsTableModel.verifyModel();
        if (errMsg != null) {
            errMsg = "Table with expressions for section 'Expected': " + errMsg;
            setErrorMessage(errMsg);
            return;
        }
        
        errMsg = m_preConditionsTableModel.verifyModel();
        if (errMsg != null) {
            errMsg = "Pre-conditions table: " + errMsg;
            setErrorMessage(errMsg);
            return;
        }
        
        errMsg = m_stubsExprsTableModel.verifyModel();
        if (errMsg != null) {
            errMsg = "Stub step expressions table: " + errMsg;
            setErrorMessage(errMsg);
            return;
        }
        
        errMsg = m_testPointsExprsTableModel.verifyModel();
        if (errMsg != null) {
            errMsg = "Test points step expressions table: " + errMsg;
        }
        setErrorMessage(errMsg);
    }
}
