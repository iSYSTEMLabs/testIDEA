package si.isystem.itest.wizards;

import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.kupzog.ktable.KTable;
import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBase;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.common.ktableutils.KTableFactory;
import si.isystem.itest.common.ktableutils.KTableForStringsModel;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class TCGenDryRunPage extends WizardPage {

    public static final String PROFILER_DRY_RUN_OFFSET_TOOLTIP = 
            "When adjusting profiler statistic after "
            + "dry run, new_value = measured_value * (1 + multiplier) + offset\n"
            + "This value depends on you target (interrupts, ...).\n";

    public static final String PROFILER_DRY_RUN_MULTIPLIER_TOOLTIP = 
            "When adjusting profiler statistic after "
            + "dry run, upper_limit = measured_value * (1 + multiplier) + offset.\n"
            + "This value is typically less than 0.5, but depends on you target (interrupts, ...).";

    private static final int VAR_NAME_COLUMN = 1;
    private static final int VAR_VALUE_COLUMN = 2;

    TCGenDryRun m_dryRunConfig;
    
    private KTable m_dryRunVarsTable;

    private KTableForStringsModel m_dryRunVarsTableModel;
    
    private String[] m_varPropposals; 
    private String[] m_varDescriptions;

    private Button m_isUpdateCvrgOnDryRunCb;

    private Button m_isUpdateProfilerOnDryRunCb;

    private Text m_profilerStatMultiplier;

    private Text m_profilerStatOffset;

    
    protected TCGenDryRunPage(String title, 
                              String description, 
                              TCGenDryRun tcGenDryRun) {
      super(title);
      setTitle(title);
      setDescription(description);
      
      m_dryRunConfig = tcGenDryRun;
    }
    

    public void setNewData(TCGenDryRun dryRunConfig) {
        m_dryRunConfig = dryRunConfig;
        m_dryRunVarsTableModel.setData(dryRunConfig.getVarAssignments());
        fillControls();
    }

    
    public void setAutoCompleteProposals(String [] varProposals, String [] descriptions) {
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
        container.setLayout(new MigLayout("fill", "[min!][fill]", "[min!][min!][min!][fill][min!][min!][min!][min!]"));

        KGUIBuilder builder = new KGUIBuilder(container);

        createTablesSash(builder);

        m_isUpdateCvrgOnDryRunCb = builder.checkBox("Update required coverage statistics during dry run",
                                                    "wrap");
        UiTools.setToolTip(m_isUpdateCvrgOnDryRunCb, "If checked, dry run updates statistic items, which are already defined.\n"
                + "Statistic items, which are not defined, are not changed.");
        m_isUpdateProfilerOnDryRunCb = builder.checkBox("Update required profiler statistics during dry run",
                                                        "wrap");
        UiTools.setToolTip(m_isUpdateProfilerOnDryRunCb, "If checked, dry run updates statistic items, which are already defined.\n"
                + "Statistic items, which are not defined, are not changed.");
        m_isUpdateProfilerOnDryRunCb.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                enableControls(m_isUpdateProfilerOnDryRunCb.getSelection());
            }
        });

        KeyListener keyListener = new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                verifyModel();
            }
            
        };
        
        builder.label("Profiler statistic multiplier:", "gapleft 25, split");
        m_profilerStatMultiplier = builder.text("wmin 100, wrap", SWT.BORDER);
        m_profilerStatMultiplier.addKeyListener(keyListener);
        UiTools.setToolTip(m_profilerStatMultiplier, PROFILER_DRY_RUN_MULTIPLIER_TOOLTIP);
        
        builder.label("Profiler statistic offset:", "gapleft 25, split");
        m_profilerStatOffset = builder.text("wmin 100", SWT.BORDER);
        m_profilerStatOffset.addKeyListener(keyListener);
        builder.label("ns", "wrap");
        UiTools.setToolTip(m_profilerStatOffset, PROFILER_DRY_RUN_OFFSET_TOOLTIP);
        
        fillControls();
        verifyModel();
        
        setControl(container);
        
        
    }

    
    private void fillControls() {
        m_isUpdateCvrgOnDryRunCb.setSelection(m_dryRunConfig.isUpdateCoverage());
        m_isUpdateProfilerOnDryRunCb.setSelection(m_dryRunConfig.isUpdateProfiler());
        
        m_profilerStatMultiplier.setText(m_dryRunConfig.getProfilerStatsMultiplier());
        m_profilerStatOffset.setText(m_dryRunConfig.getProfilerStatsOffset());
        
        boolean isProfilerUpdate = m_isUpdateProfilerOnDryRunCb.getSelection();
        enableControls(isProfilerUpdate);
    }


    private void enableControls(boolean isProfilerUpdate) {
        m_profilerStatMultiplier.setEnabled(isProfilerUpdate);
        m_profilerStatOffset.setEnabled(isProfilerUpdate);
    }


    private void createTablesSash(KGUIBuilder builder) {
        /*
        SashForm sash = new SashForm(builder.getParent(), SWT.VERTICAL);
        sash.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_GRAY));

        // wizard dialog size is set in handler
        sash.setLayoutData("");

        Composite dryRunPanel = new Composite(sash, SWT.NONE);
        dryRunPanel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[min!][fill]"));
        
        KGUIBuilder dryRunBuilder = new KGUIBuilder(dryRunPanel);
        */
        builder.label("Important: Add information to this page only if each generated "
                + "test case should have this section defined. Usually it is OK to", "wrap");
        builder.label("define this section in the base class only. "
                    + "Modifications can then be done in one place.", "gapbottom 7, wrap");
        builder.label("Dry run assignments. Expressions are evaluated during dry run, and "
                      + "result used for assignments created in section 'Variables':",
                      "wrap");
        createVarsTable(builder.getParent());

        /*
        Composite expressionsPanel = new Composite(sash, SWT.NONE);
        expressionsPanel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[min!][fill]"));  

        KGUIBuilder exprsBuilder = new KGUIBuilder(expressionsPanel);
        exprsBuilder.label("Section 'Expected' of each generated test case:",
                           "gaptop 10, wrap");

        createExpressionsTable(expressionsPanel);
        */
    }


    private void createVarsTable(Composite inputPanel) {
        
        m_dryRunVarsTableModel = KTableFactory.createStringListModel();
        m_dryRunVarsTableModel.setColumnTitles(new String[] {"Variables", 
                                                             "Assigned expressions"});
        m_dryRunVarsTableModel.setColumnWidths(new int[] {50, 250, 300});
        m_dryRunVarsTableModel.setData(m_dryRunConfig.getVarAssignments());
        m_dryRunVarsTableModel.setAutoCompleteProposals(VAR_NAME_COLUMN,
                                                        new VariablesContentProposal(m_varPropposals, 
                                                                                     m_varDescriptions),
                                                        ContentProposalAdapter.PROPOSAL_INSERT);

        m_dryRunVarsTableModel.setAutoCompleteProposals(VAR_VALUE_COLUMN, 
                                                        new VariablesContentProposal(m_varPropposals, 
                                                                                     m_varDescriptions),
                                                        ContentProposalAdapter.PROPOSAL_INSERT);

        m_dryRunVarsTable = KTableFactory.createTable(inputPanel, 
                                                      m_dryRunVarsTableModel, 
                                                      "gapbottom 10, growx, span 2, wrap",
                                                      false, true);  
        m_dryRunVarsTableModel.addAllDefaultListeners(m_dryRunVarsTable);
        m_dryRunVarsTableModel.setEnabled(true);
        
        m_dryRunVarsTableModel.addModelChangedListener(new IKTableModelChangedListener() {
            
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                if (isRedrawNeeded) {
                    m_dryRunVarsTable.redraw();
                }

                verifyModel();
            }
        });
    }
    
/*    
    private void createExpressionsTable(Composite inputPanel) {

        m_expectedExprsTableModel = KTableFactory.createModel();
        m_expectedExprsTableModel.setData(m_dryRunConfig.getExpressions());
        m_expectedExprsTableModel.setColumnTitles(new String[] {"Expressions"});
        m_expectedExprsTableModel.setColumnWidths(new int[] {50, 550});
        m_expectedExprsTableModel.setAutoCompleteProposals(0, 
                                                           m_varPropposals, 
                                                           m_varDescriptions,
                                                           ContentProposalAdapter.PROPOSAL_INSERT);
        
        m_expectedExprsTable = KTableFactory.createTable(inputPanel, 
                                                         m_expectedExprsTableModel, 
                                                         "");  // wmin 800, h 200:50%:50%, wrap
        
        m_expectedExprsTableModel.setModelChangedListener(new KTableModelChangedListener() {
            
            @Override
            public void modelChanged(boolean isRedrawNeeded) {
                if (isRedrawNeeded) {
                    m_expectedExprsTable.redraw();
                }

                verifyModel();
            }
        });
    }
*/    

    void pageSpecificDataToModel() {
    
        m_dryRunConfig.setAnalyzerInfo(m_isUpdateCvrgOnDryRunCb.getSelection(),
                                       m_isUpdateProfilerOnDryRunCb.getSelection(),
                                       m_profilerStatMultiplier.getText(),
                                       m_profilerStatOffset.getText());
    }
    
    
    private void verifyModel() {
        
        String errMsg = m_dryRunVarsTableModel.verifyModel();
        
        if (errMsg != null) {
            errMsg = "Dry Run asignments table: " + errMsg;
            setErrorMessage(errMsg);
            return;
        }
        
        String multiplier = m_profilerStatMultiplier.getText().trim();
        if (!multiplier.isEmpty()  &&  !NumberUtils.isNumber(multiplier)) {
            setErrorMessage("Profiler multiplier must be a number!");
            return;
        }
        
        String offset = m_profilerStatOffset.getText();
        if (!offset.isEmpty()  &&  !NumberUtils.isNumber(offset)) {
            setErrorMessage("Profiler offset must be a number!");
            return;
        }
        
        /*
        errMsg = m_expectedExprsTableModel.verifyModel();
        if (errMsg != null) {
            errMsg = "Expressions table: " + errMsg;
        }*/
        setErrorMessage(errMsg);
        
    }
}
