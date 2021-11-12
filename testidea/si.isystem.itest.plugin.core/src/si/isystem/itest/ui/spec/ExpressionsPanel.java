package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestAssert.ESectionAssert;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestPersistentVars.EPersistVarsSections;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrStrMapIterator;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.comp.TBControlFor_K_Table;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.SectionEditorAdapter.InheritedActionProvider;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.KTableModelForAsserts;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import de.kupzog.ktable.KTableCellSelectionListener;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;

/**
 * This class handles expressions table and text area, which displays 
 * expressions results. It is used by ExpectedEditor and PreConditionsEditor. 
 * 
 * @author markok
 */
class ExpressionsPanel {
    
    private static final int EXPRESSION_COLUMN = 1;
    private KTableModelForAsserts m_tableModel;
    private TBControlTristateCheckBox m_isInheritExprTB;
    private TBControlFor_K_Table m_exprTBControl;
    private Text m_resultsText;

    private VariablesContentProposal m_exprContentProposalProvider;
    
    
    public ExpressionsPanel(Composite parent,
                            String migLayout, 
                            String tableId, 
                            ENodeId nodeId, 
                            TBControlTristateCheckBox isInheritTsBtn, 
                            InheritedActionProvider inheritedActionProvider) {
        
        int sectionId = ESectionAssert.E_SECTION_ASSERT_EXPRESSIONS.swigValue();
        m_tableModel = new KTableModelForAsserts(sectionId, nodeId);
        
        m_exprTBControl = 
                new TBControlFor_K_Table(parent,
                                         tableId,
                                         sectionId,
                                         migLayout,
                                         nodeId, 
                                         m_tableModel);
        
        m_isInheritExprTB = isInheritTsBtn;
        m_isInheritExprTB.setActionProvider(inheritedActionProvider);

        // set autocompleters
        m_exprContentProposalProvider = new VariablesContentProposal(); 
        
        m_tableModel.setAutoCompleteProposals(EXPRESSION_COLUMN,
                                              m_exprContentProposalProvider,
                                              ContentProposalAdapter.PROPOSAL_INSERT);
        
        m_resultsText = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        m_resultsText.setLayoutData("span 6, height 120:120:120, wmin 0, growx");
        m_resultsText.setEditable(false);

        configureListenerForResultDisplay();
    }
    
    
    public void setInput(CTestTreeNode origTestSpecHashCode, 
                         CTestAssert tAssert,
                         boolean isAssert,
                         boolean isMerged) {
        configureKTableModel(tAssert, isAssert, origTestSpecHashCode);
        m_exprTBControl.setInput(tAssert, isMerged);

        selectionToTextInResultsView();
    }
    
    
    public void setEnabled(boolean isEnabled) {
        m_exprTBControl.setEnabled(isEnabled);
    }
    
    
    protected void setTextInResultsView(TextIconsContent cellContent) {
        if (cellContent == null  ||  cellContent.getTooltip(EIconPos.EBottomLeft) == null) {
            m_resultsText.setText("");
        } else {
            m_resultsText.setText(cellContent.getTooltip(EIconPos.EBottomLeft));
        }
    }

    
    public void selectionToTextInResultsView() {
        
        Point[] selection = m_exprTBControl.getSelection();
        
        if (selection != null  &&  selection.length > 1) {
            TextIconsContent cellContent = m_exprTBControl.getContentAt(1, selection[0].y);
            setTextInResultsView(cellContent);
        } else {
            setTextInResultsView(null); // clear text
        }
    }
    
    
    protected void configureListenerForResultDisplay() {
        
        m_exprTBControl.addTableSelectionListener(new KTableCellSelectionListener() {
            
            @Override
            public void fixedCellSelected(int col, int row, int statemask) {}
            
            
            @Override
            public void cellSelected(int col, int row, int statemask) {
                TextIconsContent cellContent = m_exprTBControl.getContentAt(1, row);
                setTextInResultsView(cellContent);
            }
        });
    }

    
    public TBControlTristateCheckBox getInheritExprTB() {
        return m_isInheritExprTB;
    }


    /**
     * @param tableId table ID is used in sections with more than one table. 
     */
    protected void selectLineInTable(int tableId, int lineNo) {
        m_exprTBControl.setSelection(lineNo);
    }


    protected void setVarsContentProposals(CTestSpecification testSpec, 
                                           String coreId) {
        setVarsContentProposals(testSpec, coreId, m_exprContentProposalProvider, true);
    }
    
    
    /**
     *  @depreacted use VarsMacrosLocalsGlobalsProvider instead.
     *  
     * Adds also local vars, persist. vars, host vars, and ret. val name. */
    public static void setVarsContentProposals(CTestSpecification testSpec, 
                                               String coreId,
                                               VariablesContentProposal contentProposals,
                                               boolean isAddRetValName) {

        GlobalsProvider globalVarsProvider = GlobalsConfiguration.instance(). 
               getGlobalContainer().getVarsAndMacrosGlobalsProvider(coreId);
        
        String[] proposals = globalVarsProvider.getCachedGlobals();
        String[] descriptions = globalVarsProvider.getCachedDescriptions();

        if (proposals == null) {
            proposals = new String[0];
        }
        
        ArrayList<String> proposalsList = new ArrayList<String>(Arrays.asList(proposals));
        
        if (testSpec != null) {

            // now add local variables from the 'locals' section
            StrStrMap localVars = new StrStrMap(); 
            testSpec.getLocalVariables(localVars);
            StrStrMapIterator iter = new StrStrMapIterator(localVars);
            while (iter.isValid()) {
                String key = iter.key();
                proposalsList.add(key);
                iter.inc();
            }

            if (isAddRetValName) {
                // add return value name to the list of proposals
                String retValName = testSpec.getFunctionUnderTest(true).getRetValueName();
                if (!retValName.isEmpty()) {
                    proposalsList.add(retValName);
                } else {
                    proposalsList.add(CTestCase.getISystemRetValName());
                }
            }

            // add persistent variables
            CTestPersistentVars persistVars = testSpec.getPersistentVars(true);
            CMapAdapter persistVarsMap = new CMapAdapter(persistVars, 
                                                         EPersistVarsSections.E_SECTION_DECL.swigValue(), 
                                                         true);
            StrVector keys = new StrVector();
            persistVarsMap.getKeys(keys);
            int numKeys = (int) keys.size();
            for (int idx = 0; idx < numKeys; idx++) {
                String persistVar = persistVarsMap.getValue(keys.get(idx));
                proposalsList.add(persistVar);
            }
            
            // add host variables from Variables section
            // TODO should add all host variables in scope, not only current test spec
            StrStrMap initVars = new StrStrMap(); 
            testSpec.getInitMap(initVars); 
            StrStrMapIterator initIter = new StrStrMapIterator(initVars);
            while (initIter.isValid()) {
                String key = initIter.key();
                if (UiUtils.isHostVar(key)) {
                    proposalsList.add(key);
                }
                initIter.inc();
            }
            
            contentProposals.setTestSpec(testSpec);
            contentProposals.setProposals(proposalsList.toArray(new String[0]), 
                                                           descriptions);
        }
    }
    
    
    protected void refreshGlobals(CTestSpecification testSpec, String coreId) {
        
        if (testSpec != null) {
            setVarsContentProposals(testSpec, coreId);
        } else {
            // this should never execute
            GlobalsProvider varsProvider = GlobalsConfiguration.instance().
                   getGlobalContainer().getVarsAndMacrosGlobalsProvider(coreId);
            
            m_exprContentProposalProvider.setProposals(varsProvider.getCachedGlobals(), 
                                                       varsProvider.getCachedDescriptions());
        }
    }


    private void configureKTableModel(CTestAssert tAssert,
                                      boolean isAssert,
                                      CTestTreeNode originalTestSpecHashCode) {
        
        CTestBaseList exprResults = null;
        boolean isException = false;

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null  &&  originalTestSpecHashCode != null) {
            CTestResult result = model.getResult(originalTestSpecHashCode);
            if (result != null) {

                isException = result.isException();

                if (isAssert) {
                    exprResults = result.getAssertResults(true);
                } else {
                    exprResults = result.getPreConditionResults(true);
                }
            }
        }
        
        m_tableModel.setData(tAssert, exprResults, isException);
    }


    public void setCoreId(String coreId) {
        m_exprContentProposalProvider.setCoreId(coreId);
    }


    public void setTooltip(String tooltip) {
        m_tableModel.setMainTooltip(tooltip);
    }
}
