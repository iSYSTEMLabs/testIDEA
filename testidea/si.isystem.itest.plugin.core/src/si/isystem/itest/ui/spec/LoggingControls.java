package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestLog.ESectionsLog;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrStrMapIterator;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SException;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.KGUIBuilder;

public class LoggingControls {

    public static final String WIZARD_BTN_TOOLTIP = "Select step assignments to be added to log.";
    private TBControlText m_logBeforeVarNamesHC;
    private TBControlText m_logAfterVarNamesHC;

    private Button m_refreshButton;
    private AsystContentProposalProvider m_logBeforeProposals;
    private AsystContentProposalProvider m_logAfterProposals;
    private List<String> m_wizardInputList;
    private CTestLog m_wizardInputLog;
    private ESectionsLog m_wizardLogSection;
    private ENodeId m_nodeId;
    private Button m_logWizardBtn;

    
    void createLogControls(KGUIBuilder parentBuilder, ENodeId nodeId) {
        
        m_nodeId = nodeId;
        
        KGUIBuilder builder = parentBuilder.group("Logging", 
                                                  "w 200::, span 3, growx, gaptop 10, wrap",
                                                  true, "fillx", "[min!][grow, fill]", null);
        builder.label("Before assignments:");
        
        m_logBeforeVarNamesHC = TBControlText.createForList(builder, 
                                                            "This field may contain comma separated list of variables,\n" +
                                                                    "which values we would like to have in test report. Values\n" +
                                                                    "are not verified, only logged. Evaluation ocurrs BEFORE\n" +
                                                                    "assignments are made or script function is called.\n\n" +
                                                                    "Example:\n" +
                                                                    "    counter, address, mode\n", 
                                                            "w 100::, split, span, growx", 
                                                            ESectionsLog.E_SECTION_BEFORE.swigValue(), 
                                                            nodeId, 
                                                            null, 
                                                            SWT.BORDER);
        m_logBeforeProposals = 
                ISysUIUtils.addContentProposalsAdapter(m_logBeforeVarNamesHC.getControl(), 
                                                       ContentProposalAdapter.PROPOSAL_INSERT,
                                                       UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        /* m_logBeforeAutoComplete = new AsystAutoCompleteField(m_logBeforeVarNamesHC.getControl(), 
                                                              new AsystTextContentAdapter(), 
                                                              new String[0], null, 
                                                              ContentProposalAdapter.PROPOSAL_INSERT);
                                                              */
        
        m_refreshButton = builder.button(IconProvider.INSTANCE.getIcon(EIconId.ERefresh), 
                                              "gapleft 7, wrap");
        m_refreshButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              // TODO  fillParamsAutoCompleteField(m_currentTestSpec);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        builder.label("After assignments:");
        
        m_logAfterVarNamesHC = TBControlText.createForList(builder, 
                                                            "This field may contain comma separated list of variables,\n" +
                                                                    "which values we would like to have in test report. Values\n" +
                                                                    "are not verified, only logged. Evaluation ocurrs AFTER\n" +
                                                                    "assignments are made or script function is called.\n\n" +
                                                                    "Example:\n" +
                                                                    "    counter, address, mode\n", 
                                                            "w 100::, split, span, growx", 
                                                            ESectionsLog.E_SECTION_AFTER.swigValue(), 
                                                            nodeId, 
                                                            null, 
                                                            SWT.BORDER);
        
        m_logWizardBtn = createWizardBtn(builder, "gapleft 7", WIZARD_BTN_TOOLTIP);
        
        m_logAfterProposals = 
                ISysUIUtils.addContentProposalsAdapter(m_logAfterVarNamesHC.getControl(), 
                                                   ContentProposalAdapter.PROPOSAL_INSERT,
                                                   UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        /* m_logAfterAutoComplete = new AsystAutoCompleteField(m_logAfterVarNamesHC.getControl(), 
                                                             new AsystTextContentAdapter(), 
                                                             new String[0], null, 
                                                             ContentProposalAdapter.PROPOSAL_INSERT);
                                                             */
    }
    
    
    public void setEnabled(boolean isEnabled) {
        m_logAfterVarNamesHC.setEnabled(isEnabled);
        m_logBeforeVarNamesHC.setEnabled(isEnabled);
        m_refreshButton.setEnabled(isEnabled);
        m_logWizardBtn.setEnabled(isEnabled);
    }
    
    
    public void setInput(CTestLog logConfig, boolean isMergedTestSpec) {

        m_wizardInputLog = logConfig;
        m_logBeforeVarNamesHC.setInput(logConfig, isMergedTestSpec);
        m_logAfterVarNamesHC.setInput(logConfig, isMergedTestSpec);
    }
    
    
    public void setWizardInput(CTestLog.ESectionsLog section, 
                               List<String> wizInput) {
        m_wizardLogSection = section;
        m_wizardInputList = wizInput;
    }
    
    
    public void clearInput() {
        m_logBeforeVarNamesHC.clearInput();
        m_logAfterVarNamesHC.clearInput();
    }
    
    
    void fillParamsAutoCompleteField(CTestSpecification testSpec,
                                     String funcName, // stubbed func or func with test point
                                     String coreId) {

        GlobalsContainer globalsContainer = GlobalsConfiguration.instance().getGlobalContainer();
        GlobalsProvider globalVarsProvider = globalsContainer.getVarsGlobalsProvider(coreId);
        String[] proposals = globalVarsProvider.getCachedGlobals();
        String[] descriptions = globalVarsProvider.getCachedDescriptions();

        if (proposals == null) {
            proposals = new String[0];
        }

        ArrayList<String> proposalsList = new ArrayList<String>();

        // local vars are put at the beginning of list, since they are more likely to be used.
        addLocalFunctionVariables(funcName,
                                  proposalsList, 
                                  coreId);

        // now add local variables from the 'locals' test case section
        if (testSpec != null) {
            StrStrMap localVars = new StrStrMap(); 
            testSpec.getLocalVariables(localVars);
            StrStrMapIterator iter = new StrStrMapIterator(localVars);
            
            if (!localVars.isEmpty()) {
                proposalsList.add("--- test case local variables ---");
            }
            
            while (iter.isValid()) {
                String key = iter.key();
                proposalsList.add(key);
                iter.inc();
            }
        }

        proposalsList.add("--- global variables ---");
        proposalsList.addAll(Arrays.asList(proposals));
        
        m_logBeforeProposals.setProposals(proposalsList.toArray(new String[0]), 
                                          descriptions);
        m_logAfterProposals.setProposals(proposalsList.toArray(new String[0]), 
                                         descriptions);
    }
    
    
    private void addLocalFunctionVariables(String funcName, // stubbed or func w test point
                                           ArrayList<String> proposalsList,
                                           String coreId) {
        
        FunctionGlobalsProvider globalFuncProvider = 
                GlobalsConfiguration.instance().getGlobalContainer().getFuncGlobalsProvider(coreId);
        
        try {
            proposalsList.add("--- function local variables ---");
            
            JFunction func = globalFuncProvider.getCachedFunction(funcName);

            if (func != null) {

                JVariable[] localVars = func.getLocalVars();
                for (JVariable localVar : localVars) {
                    proposalsList.add(localVar.getName());
                }
            } else {
                proposalsList.add("  ERROR: Function not found: " + funcName);
            }
        } catch (SException ex) {
            proposalsList.add(SEFormatter.getInfo(ex));
        }
    }
    
    
    /**
     * This wizard shows function parameters, which are not numbers, and all
     * items initialized in section Variables init. User selects items he 
     * wants to log.
     * 
     * @param builder
     * @param layout
     * @param tooltip
     * @return
     */
    private Button createWizardBtn(KGUIBuilder builder, 
                                   String layout,
                                   String tooltip) {
        
        Button wizardBtn = builder.button("", layout);
        wizardBtn.setImage(IconProvider.INSTANCE.getIcon(IconProvider.EIconId.EWizard));
        
        // IMPORTANT: use tooltip for control instead of JFace's tooltip, because SWT Bot  
        // works only with control tooltips 
        wizardBtn.setToolTipText(tooltip);
        
        wizardBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                // keep order of insertion, but remove duplicates
                LinkedHashSet<String> items = new LinkedHashSet<>();

                List<String> existingItems = new ArrayList<>();
                if (m_wizardInputLog != null) {
                    existingItems = addExistingLogItems(m_wizardInputLog, items);
                }
                items.addAll(m_wizardInputList);
                
                ListSelectionDialog dlg = new ListSelectionDialog(Activator.getShell(), 
                                                                  items, 
                                                                  new StringListContentProvider(), 
                                                                  new StringLabelProvider(), 
                                                                  "Select items to be logged");
                dlg.setTitle("Log items");
                dlg.setInitialElementSelections(existingItems);
                
                if (dlg.open() == Window.OK) {
                    Object[] selectedItems = dlg.getResult();
                    if (selectedItems != null) {
                        String logItems = StringUtils.join(selectedItems, ", ");
                        YamlScalar value = YamlScalar.newList(m_wizardLogSection.swigValue());
                        value.setValue(logItems);
                        SetSectionAction action = new SetSectionAction(m_wizardInputLog, 
                                                                       m_nodeId,
                                                                       value);
                        action.addAllFireEventTypes();
                        action.addDataChangedEvent();
                        TestSpecificationModel.getActiveModel().execAction(action);
                    }
                }
            }

            
            private List<String> addExistingLogItems(CTestLog log, 
                                                     LinkedHashSet<String> items) {
                CSequenceAdapter oldItems = new CSequenceAdapter(log, 
                                                                 m_wizardLogSection.swigValue(),
                                                                 true);
                List<String> existingItems = new ArrayList<>();
                for (int idx = 0; idx < oldItems.size(); idx++) {
                    existingItems.add(oldItems.getValue(idx));
                }
                
                items.addAll(existingItems);
                
                return existingItems;
            }

            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        wizardBtn.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                          SWTBotConstants.FUNCTION_INPUTS_WIZARD);
        
        return wizardBtn;
    }    
}
