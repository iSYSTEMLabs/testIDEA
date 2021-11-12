package si.isystem.itest.ui.spec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CLogResult;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.LogViewDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.TBModelEventDispatcher;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.comp.IAsistListener;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class MetaSpecEditor extends SectionEditorAdapter {

    public static final String EDIT_LOG_ITEMS_BTN_TOOLTIP = "Opens wizard for adding test inputs to log.";
    public static final String VIEW_LOG_BTN_TOOLTIP = "Opens dialog with values of logged expressions.";
    public static final String LOG_ITEMS_DLG_TITLE = "Log items";
    // This is not consistent with other flags, which also have the Default
    // button, but the 'Default' button for run flag does not fit to GUI nicely.
    private Button m_isConcreteTestCb;
    private ValueAndCommentEditor m_isConcreteTest;
    
    private TBControlText m_idControl;
    private TBControlText m_tagsHierarchyControl;
    private TBControlText m_descriptionHierarchyControl;
    
    private TBControlRadio m_testTypeRBtnsHC;
    
    private TBControlTristateCheckBox m_isInheritScopeTB;
    private TBControlTristateCheckBox m_isInheritIdTB;
    private TBControlTristateCheckBox m_isInheritDescTB;
    private TBControlTristateCheckBox m_isInheritTagsTB;
    private TBControlText m_logBeforeHierarchyControl;
    private TBControlText m_logAfterHierarchyControl;
    private TBControlTristateCheckBox m_isInheritLogTB;
    private Button m_viewLogBtn;
    private AsystContentProposalProvider m_idProposals;
    private AsystContentProposalProvider m_tagsProposals;
    private Text m_resultCommentTxt;
    private Button m_showMarkdownBtn;
    private Button m_logWizardBtn;


    public MetaSpecEditor() {
        super(ENodeId.META_NODE, SectionIds.E_SECTION_RUN, SectionIds.E_SECTION_TEST_SCOPE, 
                                 SectionIds.E_SECTION_ID, SectionIds.E_SECTION_DESC,
                                 SectionIds.E_SECTION_TAGS, SectionIds.E_SECTION_LOG);
    }
    
    
    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        
        Composite metaPanel = new Composite(scrolledPanel, SWT.NONE);
        
        MigLayout mig = new MigLayout("fill", "[min!][fill][min!]", 
                                      "[min!][min!][min!][min!][fill][min!][min!][min!][min!]");
        metaPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(metaPanel);
        
        m_isConcreteTestCb = builder.checkBox("Execute", "skip, wmax pref, gaptop 10, gapbottom 10, wrap"); // gapleft 7, 
        UiTools.setToolTip(m_isConcreteTestCb, "Check this box to enable the test. If unchecked, test will not be executed\n" +
                               "regardless of filters. It should be unchecked for test specifications, which\n" +
                               "are used as base for derived exceptions only, and are not" +
                               " intended for execution.");
        m_isConcreteTest = ValueAndCommentEditor.newMixed(SectionIds.E_SECTION_RUN.swigValue(), 
                                                          m_isConcreteTestCb);

        builder.label("Scope:");
        
        m_testTypeRBtnsHC = new TBControlRadio(builder, 
                                               new String[]{"Unit", "System", "Default (Unit)"}, 
                                               new String[]{"This is test specification for unit tests - single function will be tested.",
                                                            "This is test specification for system tests - target will run until stop condition is met.",
                                                            "Default setting (Unit). This setting is stored as unspecified item in YAML output."},        
                                               new String[]{"unitTest", "systemTest", ""}, 
                                               "wmax pref", 
                                               SectionIds.E_SECTION_TEST_SCOPE.swigValue(), 
                                               m_nodeId,
                                               null,
                                               true);
        
        // add focus listener to update test spec tree, because icon may change
        m_testTypeRBtnsHC.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
            }
            
            
            @Override
            public void focusGained(FocusEvent e) {
                if (m_testSpec != null) {
                    try {
                        TBModelEventDispatcher dispatcher = 
                                m_model.getEventDispatcher();
                        dispatcher.fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_REFRESH_REQUIRED, 
                                                                   null, 
                                                                   m_testSpec));
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), "Can not set test type!", ex);
                    }
                }
            }
        });
        
        
        m_isInheritScopeTB = createTristateInheritanceButton(builder, 
                                                             "gapleft 15, align right, gapright 10, wrap");
        m_isInheritScopeTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_TEST_SCOPE));

        
        builder.label("ID:");
        
        m_idControl = TBControlText.createForMixed(builder, 
                           "Test id should be descriptive and unique, so that we can " 
                           + "map test results to test specification.\n" 
                           + UiUtils.TEST_ID_ALLOWED_CHARS 
                           + "Other symbols are automatically replaced with '_'.\n\n"
                           + "Examples:\n" 
                           + "    invalidArgs1, coverage4, module.A.test-12, ...", 
                           "", 
                           SectionIds.E_SECTION_ID.swigValue(), 
                           m_nodeId, 
                           null, 
                           SWT.BORDER);
        
        m_idProposals = ISysUIUtils.addContentProposalsAdapter(m_idControl.getControl(), 
                                                               ContentProposalAdapter.PROPOSAL_REPLACE,
                                                               UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        
        /* m_idAutoComplete = new AsystAutoCompleteField(m_idControl.getControl(), 
                                                       new TextContentAdapter(), 
                                                       new String[0], null, 
                                                       ContentProposalAdapter.PROPOSAL_REPLACE); */

        m_idControl.setTestTreeRefreshNeeded(true);
        
        // add listener to update label in the test tree view, when user enters new test id
        m_idControl.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                if (m_testSpec != null) {
                    try {
                        m_model.addAutocompletionId(m_testSpec.getTestId());
                        /* action must contain event, otherwise Undo/Redo do not refresh test tree
                           model.fireEvent(new ModelChangedEvent(ModelChangedEvent.EventType.TEST_SPEC_TREE_REFRESH_REQUIRED, 
                                                              null, 
                                                              m_testSpec)); */
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), "Can not set test ID!", ex);
                    }
                }
            }
            
            @Override public void focusGained(FocusEvent e) {}
        });

        m_idControl.addAsistListener(new IAsistListener() {
            
            @Override
            public String onFocusLost(String content) {
                // Fix user's entry automatically. There were users, which ignored 
                // the warning, saved iyaml file, but could not open it later due 
                // to errors.
                String allowedSymbols = CYAMLUtil.getSymbolsAllowedTestId();
                return DataUtils.fixRestrictedTextScalar(content, 
                                                               allowedSymbols);
            }
        });
        
        m_isInheritIdTB = createTristateInheritanceButton(builder, "gapleft 15, align right, gapright 10, wrap");
        m_isInheritIdTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_ID));
        
        
        m_isInheritDescTB = createTristateInheritanceButton(builder, "skip, split 2, gaptop 15, w min:pref:pref");
        m_isInheritDescTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_DESC));

        m_showMarkdownBtn = builder.checkBox("View / Edit", "gaptop 15, gapleft 20, wrap");
        UiTools.setToolTip(m_showMarkdownBtn, "If selected, description is shown according "
                + "to markdown tags. Editing is not possible in this mode.\n"
                + "Deselect the button to enable editing. See tooltip of text field below for help on supported markdown.");
        m_showMarkdownBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.getSource() instanceof Button) {
                    boolean isStyledText = ((Button)e.getSource()).getSelection();
                    m_descriptionHierarchyControl.showStyledText(isStyledText);
                }
            }
        });
        
        builder.label("Description:", "");
        
        m_descriptionHierarchyControl = TBControlText.createForStyledTextMixed(builder, 
            "Human readable test description.\n" + UiUtils.MARKDOWN_HELP,
            "wmin 100, hmin 150, growy, span 2, gapright 5, wrap", 
            SectionIds.E_SECTION_DESC.swigValue(), 
            m_nodeId, 
            EHControlId.ETestDescription, 
            SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        
        builder.label("Result comment:", "");
        m_resultCommentTxt = builder.text("span 2,  gaptop 7, split 2, hmin 80, growx", 
                                          SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        Text rcDescTxtLbl = builder.text("gaptop 10, wrap", SWT.MULTI);
        rcDescTxtLbl.setEditable(false);
        rcDescTxtLbl.setText("This text refers to specific\n"
                           + "test run. It is stored to\n"  
                           + "results and report only,\n"
                           + "and will be lost on next run!");
        
        m_resultCommentTxt.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                CTestResult testResult = m_model.getResult(m_currentTestSpec);
                if (testResult != null) {
                    testResult.setResultComment(m_resultCommentTxt.getText());
                }
            }
            
            @Override
            public void focusGained(FocusEvent e) {}
        });
        
        // string is no longer verified - it is quoted and escaped by StringValidator when assigned to the model
        //m_descriptionText.addFocusListener(new FocusChangedComplexScalarVerifier(this, m_descriptionText));

        builder.label("Tags:", "gaptop 15");
        
        m_tagsHierarchyControl = 
                TBControlText.createForList(builder, 
                     "This field should contain tags, which will be used " 
                     + "for test filtering in scripts.\n"
                     + "Tags should be words separated by commas. "
                     + "They may contain letters, digits and the\n"
                     + "following symbols: " 
                     + CYAMLUtil.getSymbolsAllowedTestId()
                     + "\nOther symbols are automatically replaced with '_'.\n\n"
                     + "Example:\n" 
                     + "    init, calculation-1, profiler", 
                     "gaptop 15", 
                     SectionIds.E_SECTION_TAGS.swigValue(), 
                     m_nodeId, 
                     EHControlId.ETags, 
                     SWT.BORDER); 

        m_tagsHierarchyControl.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                if (m_testSpec != null) {
                    StrVector vTags = new StrVector();
                    m_testSpec.getTags(vTags);
                    m_model.addAutocompletionTags(vTags);
                }
            }
            
            @Override
            public void focusGained(FocusEvent e) {}
        });
       
        m_tagsHierarchyControl.addAsistListener(new IAsistListener() {
            
            @Override
            public String onFocusLost(String content) {
                // Fix user's entry automatically. There were users, which ignored 
                // the warning, saved iyaml file, but could not open it later due 
                // to errors.
                String[] tags = content.split(",");
                String allowedSymbols = CYAMLUtil.getSymbolsAllowedTestId();
                for (int idx = 0; idx < tags.length; idx++) {
                    tags[idx] = DataUtils.fixRestrictedTextScalar(tags[idx].trim(), 
                                                                        allowedSymbols);
                }
                
                return StringUtils.join(tags, ", ");
            }
        });
        
        
        m_tagsProposals = ISysUIUtils.addContentProposalsAdapter(m_tagsHierarchyControl.getControl(), 
                                                                 ContentProposalAdapter.PROPOSAL_INSERT,
                                                                 UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace());
        /* m_tagsAutoComplete = new AsystAutoCompleteField(m_tagsHierarchyControl.getControl(), 
                                                         new AsystTextContentAdapter(), 
                                                         new String[0], null, 
                                                         ContentProposalAdapter.PROPOSAL_INSERT);
                                                         */
        
        m_isInheritTagsTB = createTristateInheritanceButton(builder, "gapleft 15, gapright 10, gaptop 15, wrap");
        m_isInheritTagsTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_TAGS));
        
        m_isConcreteTestCb.addSelectionListener(new SelectionListener() {
            // this listener triggers change of 'run' icon in tree view
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (m_testSpec != null) {
                    try {
                        m_isConcreteTest.setValue(m_isConcreteTestCb.getSelection() ? "" : 
                                                                                      "false");
                        SetSectionAction action = new SetSectionAction(m_testSpec, 
                                                            ENodeId.META_NODE, 
                                                            m_isConcreteTest.getScalarCopy());
                        if (action.isModified()) {
                            action.addDataChangedEvent();
                            action.addTreeChangedEvent();
                            action.addAllFireEventTypes();
                            TestSpecificationModel.getActiveModel().execAction(action); 
                        }
                    } catch (Exception ex) {
                        SExceptionDialog.open(Activator.getShell(), 
                                              "Can not set run flag!", ex);
                    }
                }
            }
            
        
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        
        builder.label("Log before:", "gaptop 15");
        m_logBeforeHierarchyControl = 
                TBControlText.createForList(builder, 
                                            "This field may contain variables, which should " 
                                            + "be evaluated and logged BEFORE test.\nEvaluations "
                                            + "have no effect on test result, but may be used for "
                                            + "debugging,\nor documentation purposes (they are "
                                            + "stored in test report).\nExample:\n" +
                                              "    g_counter, ${myPath}", 
                                            "gaptop 15", 
                                            CTestLog.ESectionsLog.E_SECTION_BEFORE.swigValue(), 
                                            m_nodeId, 
                                            null, 
                                            SWT.BORDER); 

        m_viewLogBtn = builder.button("View", "split 2, gapleft 15, gaptop 7");
        UiTools.setToolTip(m_viewLogBtn, VIEW_LOG_BTN_TOOLTIP);
        m_viewLogBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                CTestResult testResult = m_model.getResult(m_currentTestSpec);
                if (testResult != null) {
                    LogViewDialog tableDialog = new LogViewDialog(Activator.getShell(), 
                                                                  testResult.getLogResult(true));
                    tableDialog.open();
                }
            }
        });

        m_logWizardBtn = createWizardBtn(builder, "gapleft 7, gaptop 7, wrap", 
                                         EDIT_LOG_ITEMS_BTN_TOOLTIP);
        
        builder.label("Log after:", "gaptop 5");
        m_logAfterHierarchyControl = 
                TBControlText.createForList(builder, 
                                            "This field may contain variables, which should " 
                                            + "be evaluated and logged AFTER test.\nEvaluations "
                                            + "have no effect on test result, but may be used for "
                                            + "debugging,\n or documentation purposes (they are "
                                            + "stored in test report).\nExample:\n" +
                                              "    g_counter, ${myPath}", 
                                            "gaptop 5", 
                                            CTestLog.ESectionsLog.E_SECTION_AFTER.swigValue(), 
                                            m_nodeId, 
                                            null, 
                                            SWT.BORDER); 
        
        m_isInheritLogTB = createTristateInheritanceButton(builder, 
                                                           "gapleft 15, gapright 10");
        m_isInheritLogTB.setActionProvider(new InheritedActionProvider(SectionIds.E_SECTION_LOG));
        
        return configureScrolledComposite(scrolledPanel, metaPanel);
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
        wizardBtn.setToolTipText(tooltip); // set tooltip this way to be visible to SWT Bot
        wizardBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                // keep order of insertion, but remove duplicates
                LinkedHashSet<String> items = new LinkedHashSet<>();

                CTestLog log = m_testSpec.getLog(false);
                
                List<String> exitingItems = addExistingLogItems(log, items);
                
                addFuncParams(items);
                
                addInitVars(items);
                
                ListSelectionDialog dlg = new ListSelectionDialog(Activator.getShell(), 
                                                                  items, 
                                                                  new StringListContentProvider(), 
                                                                  new StringLabelProvider(), 
                                                                  "Select items to be logged");
                dlg.setTitle(LOG_ITEMS_DLG_TITLE);
                dlg.setInitialElementSelections(exitingItems);
                
                if (dlg.open() == Window.OK) {
                    Object[] selectedItems = dlg.getResult();
                    if (selectedItems != null) {
                        String logItems = StringUtils.join(selectedItems, ", ");
                        YamlScalar value = YamlScalar.newList(CTestLog.ESectionsLog.E_SECTION_BEFORE.swigValue());
                        value.setValue(logItems);
                        SetSectionAction action = new SetSectionAction(log, 
                                                                       ENodeId.META_NODE, 
                                                                       value);
                        action.addAllFireEventTypes();
                        action.addDataChangedEvent(ENodeId.META_NODE, m_testSpec);
                        m_model.execAction(action);
                    }
                }
            }

            
            private List<String> addExistingLogItems(CTestLog log, 
                                                          LinkedHashSet<String> items) {
                CSequenceAdapter oldItems = new CSequenceAdapter(log, 
                                                                 CTestLog.ESectionsLog.E_SECTION_BEFORE.swigValue(),
                                                                 true);
                List<String> existingItems = new ArrayList<>();
                for (int idx = 0; idx < oldItems.size(); idx++) {
                    existingItems.add(oldItems.getValue(idx));
                }
                
                items.addAll(existingItems);
                
                return existingItems;
            }

            
            private void addInitVars(LinkedHashSet<String> items) {
                setCurrentTS(SectionIds.E_SECTION_INIT);
                StrVector vars = m_currentTestSpec.getInitKeys();
                for (int idx = 0; idx < vars.size(); idx++) {
                    items.add(vars.get(idx));
                }
            }

            
            private void addFuncParams(LinkedHashSet<String> items) {
                setCurrentTS(SectionIds.E_SECTION_FUNC);
                CTestFunction func = m_currentTestSpec.getFunctionUnderTest(true);
                StrVector params = new StrVector();
                func.getPositionParams(params);
                for (int idx = 0; idx < params.size(); idx++) {
                    String param = params.get(idx);
                    if (!param.isEmpty()  &&  !Character.isDigit(param.charAt(0))) {
                        
                        // address of item is usually not interesting, but it's value is
                        if (param.charAt(0) == '&') {
                            param = param.substring(1);
                        }
                        items.add(param);
                    }
                }
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        wizardBtn.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                          SWTBotConstants.FUNCTION_INPUTS_WIZARD);
        
        return wizardBtn;
    }

    
    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testSpec != null;
        m_isConcreteTest.setEnabled(isEnabled);
        m_testTypeRBtnsHC.setEnabled(isEnabled);
        m_idControl.setEnabled(isEnabled);
        m_descriptionHierarchyControl.setEnabled(isEnabled);
        m_tagsHierarchyControl.setEnabled(isEnabled);
        m_logBeforeHierarchyControl.setEnabled(isEnabled);
        m_logAfterHierarchyControl.setEnabled(isEnabled);

        if (!isEnabled) {
            setInputForInheritCb(null, m_isInheritScopeTB);
            setInputForInheritCb(null, m_isInheritIdTB);
            setInputForInheritCb(null, m_isInheritDescTB);
            setInputForInheritCb(null, m_isInheritTagsTB);
            setInputForInheritCb(null, m_isInheritLogTB);

            
            m_isConcreteTestCb.setSelection(false);
            m_idControl.clearInput();
            m_descriptionHierarchyControl.clearInput();
            m_tagsHierarchyControl.clearInput();
            m_logBeforeHierarchyControl.clearInput();
            m_logAfterHierarchyControl.clearInput();
            m_viewLogBtn.setEnabled(false);
            m_logWizardBtn.setEnabled(false);
            return;            
        }
        
        setInputForInheritCb(SectionIds.E_SECTION_TEST_SCOPE, m_isInheritScopeTB);
        setInputForInheritCb(SectionIds.E_SECTION_ID, m_isInheritIdTB);
        setInputForInheritCb(SectionIds.E_SECTION_DESC, m_isInheritDescTB);
        setInputForInheritCb(SectionIds.E_SECTION_TAGS, m_isInheritTagsTB);
        setInputForInheritCb(SectionIds.E_SECTION_LOG, m_isInheritLogTB);
        
        // The following lines are a workaround for bug in ContentProposalAdapter(),
        // because of which content proposal window pops up on setText(). If we
        // set it to empty string first, it does not pop-up. To reproduce:
        // comment the next line, then select function name with keyboard, and
        // while focus (cursor) is till in the function combo, click test spec in the
        // test spec tree. See also ContentProposalAdapter, line 1794 (case SWT.Modify:)
        m_idControl.clearInput(); // see comment before: ((Combo)m_funcHierachyControl.getControl()).setText("");
        m_tagsHierarchyControl.clearInput(); // see comment before: ((Combo)m_funcHierachyControl.getControl()).setText("");
        m_logBeforeHierarchyControl.clearInput();
        m_logAfterHierarchyControl.clearInput();

        setCurrentTS(SectionIds.E_SECTION_ID);
        m_idControl.setInput(m_currentTestSpec, m_isInherited); 
        
        String [] proposals = m_model.getAutocompletionIds();
        m_idProposals.setProposals(proposals, null);
        
        m_isConcreteTest.updateValueAndCommentFromTestBase(m_testSpec);
        m_isConcreteTestCb.setSelection(!m_isConcreteTest.getValue().equals("false"));

        setCurrentTS(SectionIds.E_SECTION_TEST_SCOPE);
        m_testTypeRBtnsHC.setInput(m_currentTestSpec, m_isInherited);
        
        setCurrentTS(SectionIds.E_SECTION_DESC);
        m_descriptionHierarchyControl.showStyledText(false); // reset the state to normal
        m_descriptionHierarchyControl.setInput(m_currentTestSpec, m_isInherited);
        m_descriptionHierarchyControl.showStyledText(m_showMarkdownBtn.getSelection());

        setCurrentTS(SectionIds.E_SECTION_TAGS);
        m_tagsHierarchyControl.setInput(m_currentTestSpec, m_isInherited);
        
        setCurrentTS(SectionIds.E_SECTION_LOG);
        m_logBeforeHierarchyControl.setInput(m_currentTestSpec.getLog(false), 
                                             m_isInherited);
        m_logAfterHierarchyControl.setInput(m_currentTestSpec.getLog(false), 
                                            m_isInherited);
        
        proposals = m_model.getAutocompletionTags();
        m_tagsProposals.setProposals(proposals, null);
        
        CTestResult testResult = m_model.getResult(m_currentTestSpec);
        boolean isLogViewBtnEnabled = false;
        if (testResult != null) {
            CLogResult logRes = testResult.getLogResult(true);
            if (!logRes.isEmpty()) {
                isLogViewBtnEnabled = true;
            }
            m_resultCommentTxt.setText(testResult.getResultComment());
        } else {
            m_resultCommentTxt.setText("");
        }
        
        m_viewLogBtn.setEnabled(isLogViewBtnEnabled);
        m_logWizardBtn.setEnabled(!m_testSpec.isInheritSection(SectionIds.E_SECTION_LOG));
        
        m_resultCommentTxt.setEnabled(testResult != null);
    }
    
    
    @Override 
    public void copySection(CTestTreeNode testNode) {
        CTestSpecification testSpec = CTestSpecification.cast(testNode);
        String id = m_testSpec.getTestId();
        testSpec.setTestId(id);
        
        ETestScope scope = m_testSpec.getTestScope();
        testSpec.setTestScope(scope);
        
        String description = m_testSpec.getDescription();
        testSpec.setDescription(description);
        
        StrVector tags = new StrVector(); 
        m_testSpec.getTags(tags);
        testSpec.setTags(tags);
    }
    
    @Override
    public void clearSection() {
        
        AbstractAction action = createClearSectionAction(m_testSpecSectionIds);
        action.addAllFireEventTypes();
        action.addTreeChangedEvent(null, null); // required so that test ID in Outline view changes
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }

    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_ID.swigValue(),
                         SectionIds.E_SECTION_DESC.swigValue(),
                         SectionIds.E_SECTION_TAGS.swigValue()
        };
    }
}


class StringListContentProvider implements ITreeContentProvider {

    private LinkedHashSet<String> m_input;


    @SuppressWarnings("unchecked")
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        
        m_input = (LinkedHashSet<String>)newInput;
    }

    
    @Override
    public Object[] getElements(Object inputElement) {
        return m_input.toArray(new String[0]);
    }

    
    @Override
    public Object[] getChildren(Object parentElement) {
        return null;
    }

    
    @Override
    public Object getParent(Object element) {
        return null;
    }

    
    @Override
    public boolean hasChildren(Object element) {
        return false;
    }
}


class StringLabelProvider implements ILabelProvider {

    @Override
    public void addListener(ILabelProviderListener listener) {
    }


    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Image getImage(Object element) {
        return null;
    }

    @Override
    public String getText(Object element) {
        return (String)element;
    }
    
    @Override
    public void dispose() {
    }
}