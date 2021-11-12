package si.isystem.itest.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestAnalyzerCoverage;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestAssert.ESectionAssert;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestCoverageStatistics;
import si.isystem.connect.CTestEvalAssignStep;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestHIL;
import si.isystem.connect.CTestHIL.ETestHILSections;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestPersistentVars.EPersistVarsSections;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestStub;
import si.isystem.connect.CTestStub.EStubSectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.ReplaceMappingKeyAction;
import si.isystem.itest.model.actions.mapping.SetSectionMappingAction;
import si.isystem.itest.model.actions.sequence.SetSequenceItemAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.spec.PersistentVarsEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.VariablesSpecEditor;
import si.isystem.ui.utils.KGUIBuilder;


/**
 * This class implements rename functionality. It supports selection of data
 * type to rename, for example functions, variables, ...
 * All renaming functionality is implemented in this class.
 */
public class RenameDialog extends Dialog {

    private final static int COLUMN_VAR_TYPES = 2;
    
    private enum ERenameSections {
        E_NULL,  // used to indicate the start of rename for category
        E_FUNCTIONS,
        E_RET_VAL,
        E_DECL_VARS,
        E_INIT_VARS,
        E_STUBS,
        E_COVERAGE,
        E_PROFILER,
        E_SCRIPT,
        E_TAGS,
        E_FILTER, E_EXPECTED, E_TEST_POINTS, E_PERSIST_VARS, E_DELETED_PERSIST_VARS
    };
    
    // this class is used to store data on the stack, which replaces recursive 
    // method call for walking the test specification tree. 
    private static class StackStruct {
        CTestSpecification m_testSpec;
        int m_derivedIdx;

        public StackStruct(CTestSpecification testSpec, int derivedIdx) {
            m_testSpec = testSpec;
            m_derivedIdx = derivedIdx;
        }
    }
    
    Stack<StackStruct> m_tsStack = new Stack<StackStruct>();
    private TestSpecificationModel m_model = TestSpecificationModel.getActiveModel();

    // button indices
    private static final int FIND_NEXT_BTN = 0;
    private static final int REPLACE_BTN = 1;
    private static final int RENAME_ALL_BTN = 2;
    private static final int CLOSE_BTN = 3;
    
    // UI controls
    private Combo m_categoryCombo;
    private GlobalsSelectionControl m_oldNameTxt;
    private GlobalsSelectionControl m_newNameTxt;
    private Button m_renameBtn;
    
    // UI related data
    private int m_comboSelectionIdx = 0;
    private ItemCategory m_category;
    
    // processing vars
    private CTestSpecification m_containerTestSpec;
    private ENodeId m_testEditorNodeId;
    private ERenameSections m_currentSearchSection;
    private int m_currentSearchIdx;

    private GroupAction m_groupAction;

    private YamlScalar m_newValue;

    private AbstractAction m_action;
    private CSequenceAdapter m_expectedExpressions;
    private boolean m_isCheckFilters;
    private boolean m_isSelectLineInTable;
    private int m_currentTableId;
    private Button m_findNextBtn;
    private Button m_renameAllBtn;
    private boolean m_isStubScriptFunc;
    private int m_tableRow;
    @SuppressWarnings("unused")
    private int m_tableColumn;
    
    // completion providers
    /* private static GlobalsProvider m_funcGlobalsProvider; 
    private static GlobalsProvider m_varsGlobalsProvider; 
    private static GlobalsProvider m_typesGlobalsProvider; 
    private static GlobalsProvider m_hilGlobalsProvider; 
    private static GlobalsProvider m_tagsGlobalsProvider;
    */ 

    // This enum contains all data types, which can be renamed.
    public enum ItemCategory {Functions {
                                  @Override
                                  public String toString() { return "Functions";}
                                  @Override
                                  public String getProvider() { return GlobalsContainer.GC_ALL_FUNCTIONS;}
                              },
                              Variables {
                                  @Override
                                  public String toString() { return "Variables";}
                                  @Override
                                  public String getProvider() { return GlobalsContainer.GC_ALL_VARS;}
                              }, 
                              Types {
                                  @Override
                                  public String toString() { return "Types";}
                                  @Override
                                  public String getProvider() { return GlobalsContainer.GC_ALL_TYPES;}
                              }, 
                              HILParameters {
                                  @Override
                                  public String toString() { return "HIL parameters";}
                                  @Override
                                  public String getProvider() { return GlobalsContainer.GC_HIL;}
                              },
                              Tags {
                                  @Override
                                  public String toString() { return "Test tags";}
                                  @Override
                                  public String getProvider() { return GlobalsContainer.GC_TAGS;}
                              };
                              
                         public String getProvider() { return null; };     
                         };

                         
                         
    public RenameDialog(Shell parentShell) {
        super(parentShell);

        // make a dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MODELESS);
        
        /* m_funcGlobalsProvider = GlobalsContainer.instance().getAllFunctionsProvider(); 
        m_varsGlobalsProvider = GlobalsContainer.instance().getAllVarsProvider();
        m_typesGlobalsProvider = GlobalsContainer.instance().getAllTypesProvider();
        m_hilGlobalsProvider = GlobalsContainer.instance().getHilGlobalsProvider();
        m_tagsGlobalsProvider = GlobalsContainer.instance().getTagsGlobalsProvider(); */
    }

    
    /**
     * Moves dialog to the bottom right corner of the main window.
     */
    @Override
    public Point getInitialLocation(Point pt) {
        Composite shell = getShell().getParent(); 
        Rectangle bounds = shell.getBounds();
        // System.out.println("bounds = " + bounds + "   dlgSize = " + pt);
        // position dialog into the right bottom corner of the main window, where 
        // it less likely to cover selected items
        return new Point(bounds.x + bounds.width - pt.x, bounds.y + bounds.height - pt.y);
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Rename");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 500;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("", "[min!][fill]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
        builder.label("Category:");

        List<String> options = new ArrayList<String>();
        for (ItemCategory cat : ItemCategory.values()) {
            options.add(cat.toString());
        }
        
        String [] optionsStr = options.toArray(new String[0]);
        m_categoryCombo = builder.combo(optionsStr, "pad 0 7, width ::40%, wrap", SWT.BORDER | SWT.READ_ONLY);
        m_categoryCombo.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                setGlobalsProvider();
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        m_categoryCombo.select(m_comboSelectionIdx);
        
        builder.label("Old name:");
        m_oldNameTxt = new GlobalsSelectionControl(mainDlgPanel, 
                                                         "width 100%, wrap", 
                                                         null, 
                                                         null, 
                                                         SWT.NONE, 
                                                         GlobalsContainer.GC_ALL_FUNCTIONS,
                                                         null,
                                                         true,
                                                         true,
                                                         ContentProposalAdapter.PROPOSAL_REPLACE,
                                                         UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                         GlobalsConfiguration.instance().getGlobalContainer(),
                                                         ConnectionProvider.instance());
        
        m_oldNameTxt.setToolTipText("Old name of identifier, which you want to rename.");
        m_oldNameTxt.getControl().addKeyListener(new KeyListener() {
            @Override public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
                boolean isOldTextDefined = !m_oldNameTxt.getControl().getText().trim().isEmpty();
                // disable rename button, because the current selection (m_testBase) is no longer valid
                if (UiUtils.isDataChar(e)) {
                    m_renameBtn.setEnabled(false);
                } else {
                    m_renameBtn.setEnabled(m_renameBtn.isEnabled()  &&  isOldTextDefined);
                }
                 
                m_findNextBtn.setEnabled(isOldTextDefined);
                m_renameAllBtn.setEnabled(isOldTextDefined);
            }
        });
        
        builder.label("New name:");
        m_newNameTxt = new GlobalsSelectionControl(mainDlgPanel, 
                                                       "width 100%, wrap", 
                                                       null,
                                                       null,
                                                       SWT.NONE, 
                                                       GlobalsContainer.GC_ALL_FUNCTIONS,
                                                       null,
                                                       true,
                                                       true,
                                                       ContentProposalAdapter.PROPOSAL_REPLACE,
                                                       UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                       GlobalsConfiguration.instance().getGlobalContainer(),
                                                       ConnectionProvider.instance());

        m_newNameTxt.setToolTipText("New name of identifier, which you want to rename.");
        m_newNameTxt.getControl().addKeyListener(new KeyListener() {
            @Override public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
                // update the value for renaming
                if (m_newValue != null) {
                    m_newValue.setValue(m_newNameTxt.getControl().getText());
                }
            }
        });

        builder.separator("span 2, growx, gaptop 15", SWT.HORIZONTAL);
        
        initSearchVars();
        setGlobalsProvider();

        // this listener is activated when globals provider is set!
        m_oldNameTxt.getControl().addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                boolean isOldTextDefined = !m_oldNameTxt.getControl().getText().trim().isEmpty();
                m_findNextBtn.setEnabled(isOldTextDefined);
                m_renameAllBtn.setEnabled(isOldTextDefined);
            }
        });
        
        return composite;
    }


    protected void setGlobalsProvider() {
        int idx = m_categoryCombo.getSelectionIndex();
        ItemCategory category = ItemCategory.values()[idx];
        String provider = category.getProvider();
        m_oldNameTxt.setGlobalsProvider(provider, null);
        m_newNameTxt.setGlobalsProvider(provider, null);
        
        m_oldNameTxt.setEnbledShowSourceBtn(category == ItemCategory.Functions);
        m_newNameTxt.setEnbledShowSourceBtn(category == ItemCategory.Functions);
    }

    
    private void initSearchVars() {
        m_isSelectLineInTable = false;
        m_currentSearchIdx = 0;
        m_currentSearchSection = ERenameSections.E_NULL;
        m_isStubScriptFunc = false;

        m_isCheckFilters = true;
        m_tsStack.clear();
        m_tsStack.push(new StackStruct(m_containerTestSpec, 0));
    }
    
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        m_findNextBtn = createButton(parent, FIND_NEXT_BTN, "&Find Next", true);
        
        m_renameBtn = createButton(parent, REPLACE_BTN, "&Rename", false);
        
        m_renameAllBtn = createButton(parent, RENAME_ALL_BTN, "Rename &All", false);

        // disable buttons until some text is defined
        m_findNextBtn.setEnabled(false);
        m_renameBtn.setEnabled(false); // make it disabled until first item is found
        m_renameAllBtn.setEnabled(false);
        
        createButton(parent, CLOSE_BTN, IDialogConstants.CLOSE_LABEL, false);
    }
    

    /**
     * This is the main renaming method. It is called whenever one of the dialog 
     * buttons is pressed.
     */
    @Override
    protected void buttonPressed(int buttonId) {

        String oldName = m_oldNameTxt.getControl().getText();
        String newName = m_newNameTxt.getControl().getText();
        m_comboSelectionIdx  = m_categoryCombo.getSelectionIndex();
        m_category = ItemCategory.values()[m_comboSelectionIdx];
        
        switch (buttonId) {
        case REPLACE_BTN:
            if (m_action != null) {
                renameSingle(m_action);
            }
            // break intentionally omitted - after replacing, we want to  
            // see the next replacement candidate automatically
            // no break
        case FIND_NEXT_BTN:
            boolean isContinue = false;
            do {
                isContinue = false;
                m_action = nextItem(oldName, newName, false);
                if (m_action != null) {
                    // test == null only when tags in filters are renamed - nothing to
                    // select in UI in this case!
                    CTestTreeNode testNode = m_action.getContainerTreeNode();
                    if (testNode != null) {
                        m_renameBtn.setEnabled(true);

                        TestCaseEditorPart.getOutline().setSelection(m_model, 
                                                                     testNode);
                        TestCaseEditorPart editor = TestCaseEditorPart.getActive();
                        if (editor != null) {
                            if (testNode.isGroup()) {
                                editor.selectSection(CTestGroup.cast(testNode), m_testEditorNodeId);
                            } else {
                                editor.selectSection(CTestSpecification.cast(testNode), m_testEditorNodeId);
                            }
                            // -1 because it is incremented before return
                            if (m_isSelectLineInTable) {
                                editor.selectLineInTable(m_currentTableId,
                                                         m_tableRow);
                            }
                        }
                    }
                } else {
                    initSearchVars();
                    isContinue = MessageDialog.openConfirm(getShell(), 
                                                           "End of test specifications", 
                                                           "No more items for replacement found. " +
                                                           "Do you want to start from beginning?");
                }
            } while (isContinue);
            break;
        case RENAME_ALL_BTN:
            int renameCounter = 0;
            m_groupAction = new GroupAction("Rename");
            
            initSearchVars();
            
            do {
                m_action = nextItem(oldName, newName, true);
                if (m_action != null) {
                    m_groupAction.add(m_action);
                    renameCounter++;
                } 
            } while (m_action != null  &&  
                    renameCounter < 50000);  // renaming algorithm is complex and
            // may never end because of a bug - provide safety check here

            try {
                if (!m_groupAction.isEmpty()) {
                    execAndRefresh(m_groupAction, null);

                    MessageDialog.openConfirm(getShell(), "Rename finished!", 
                                              "Number of items renamed: " + Integer.toString(renameCounter));
                } else {
                    MessageDialog.openConfirm(getShell(), "Rename finished!", 
                                              "No matches found!");
                }
            } catch (Exception ex) {
                SExceptionDialog.open(getShell(), "Rename failed!", ex);
            }

            initSearchVars();
            break; // let's keep the dialog open - maybe user wants to rename something else 
        case CLOSE_BTN:
        default:
            close();
        }
    }


    private void execAndRefresh(AbstractAction action, CTestTreeNode testSpec) {
        
        action.addAllFireEventTypes();
        
        if (testSpec == null) {
            testSpec = m_model.getRootTestSpecification();
        }
        
        action.addDataChangedEvent(m_testEditorNodeId, testSpec);
        if (m_category == ItemCategory.Functions) {
            action.addTreeChangedEvent(m_containerTestSpec, testSpec);
        }
        m_model.execAction(action);
    }

    
    private AbstractAction nextItem(String oldName, String newName, boolean isRenameAll) {

        AbstractAction action = null;
        CTestSpecification testSpec = m_tsStack.lastElement().m_testSpec;
        
        while (!m_tsStack.isEmpty()) {
            
            action = createRenameAction(testSpec, oldName, newName, m_category, isRenameAll);
            
            if (action != null) {
                return action;
            }
            
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_NULL;
            
            // nothing for replacing found, let's advance to next test spec
            do {
                int tsIdx = m_tsStack.lastElement().m_derivedIdx;
                testSpec =  m_tsStack.lastElement().m_testSpec;
                
                if (tsIdx < testSpec.getNoOfDerivedSpecs()) {
                    testSpec = testSpec.getDerivedTestSpec(tsIdx);
                    m_tsStack.lastElement().m_derivedIdx++;
                    m_tsStack.push(new StackStruct(testSpec, 0));
                    break;
                }
                m_tsStack.pop();
            } while (!m_tsStack.isEmpty());
        }
        
        return null;
    }
    

    /*
     * This is the most complex method - according to category, m_currentSearchSection,
     * and m_currentSearchIdx it returns action, which renames the currently 
     * selected item. This method therefore selects items from test specification 
     * to be renamed.
     */
    private AbstractAction createRenameAction(CTestSpecification testSpec,
                                              String oldName,
                                              String newName,
                                              ItemCategory category,
                                              boolean isRenameAll) {
        
        switch (category) {
        case Functions:
            return renameFunction(testSpec, oldName, newName);

        case Variables:
            return renameVariable(testSpec, oldName, newName);
            
        case Types:
            return renameType(testSpec, oldName, newName);
            
        case HILParameters:
            return renameHILParameters(testSpec, oldName, newName);
            
        case Tags:
            return renameTags(testSpec, oldName, newName, isRenameAll);
        default:
            throw new SIllegalStateException("Unsupported category for rename!")
            .add("category", category);
        }
    }

    
    private AbstractAction renameFunction(CTestSpecification testSpec,
                                          String oldName,
                                          String newName) {
        int sectionId = -1;
        CTestBase testBase = null;
        CTestAnalyzer analyzer = testSpec.getAnalyzer(false);

        switch (m_currentSearchSection) {
        case E_NULL:
        case E_FUNCTIONS:
            // function under test
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_STUBS;
            sectionId = CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue();

            testBase = initScalarValue(testSpec.getFunctionUnderTest(false), 
                                       sectionId, 
                                       oldName,
                                       newName);
            if (testBase != null) {
                m_testEditorNodeId = ENodeId.FUNCTION_NODE;
                m_isSelectLineInTable = false;
                SetSectionAction action = new SetSectionAction(testBase, m_testEditorNodeId, m_newValue);
                return action;
            }
            // no break - if func. does not match, continue with stubs
        case E_STUBS:
            CTestBaseList stubs = testSpec.getStubs(true);
            
            while ((m_currentSearchIdx) < stubs.size()) {
                
                CTestStub stub = CTestStub.cast(stubs.get(m_currentSearchIdx));
                int searchIdx = m_currentSearchIdx;
                
                if (!m_isStubScriptFunc) {
                    // stubbed func name
                    m_isStubScriptFunc = true;
                    sectionId = EStubSectionIds.E_SECTION_STUBBED_FUNC.swigValue();
                    testBase = stub;
                } else {
                    // script func name
                    m_isStubScriptFunc = false;
                    m_currentSearchIdx++;
                    sectionId = EStubSectionIds.E_SECTION_SCRIPT_FUNCTION.swigValue();
                    testBase = stub;
                }
                
                testBase = initScalarValue(testBase, sectionId, oldName, newName);
                if (testBase != null) {
                    m_testEditorNodeId = ENodeId.STUBS_NODE;
                    m_isSelectLineInTable = true;
                    m_tableRow = searchIdx;
                    m_tableColumn = 0;
                    SetSectionAction action = new SetSectionAction(testBase, m_testEditorNodeId, m_newValue);
                    return action;
                }
            }
            m_isStubScriptFunc = false;
            m_currentSearchSection = ERenameSections.E_COVERAGE;
            m_currentSearchIdx = 0;
            // no break - if stubs do not match, continue with coverage
        case E_COVERAGE:
            // coverage
            CTestAnalyzerCoverage cvrg = analyzer.getCoverage(false);
            while (m_currentSearchIdx < cvrg.getStatisticsList(true).size()) {
                CTestCoverageStatistics stats = cvrg.getStatistics(m_currentSearchIdx);
                sectionId = CTestCoverageStatistics.ECoverageStatSectionId.E_SECTION_FUNC_NAME.swigValue();
                testBase = initScalarValue(stats, sectionId, oldName, newName);
                if (testBase != null) {
                    m_testEditorNodeId = ENodeId.COVERAGE_STATS_NODE;
                    m_isSelectLineInTable = true;
                    m_tableRow = m_currentSearchIdx;
                    m_tableColumn = 0;
                    m_currentSearchIdx++;
                    SetSectionAction action = new SetSectionAction(testBase, m_testEditorNodeId, m_newValue);
                    return action;
                }
                m_currentSearchIdx++;
            }
            m_currentSearchSection = ERenameSections.E_PROFILER;
            m_currentSearchIdx = 0;
            // no break - if coverage does not match, continue with profiler
        case E_PROFILER:
            // profiler code areas
            CTestAnalyzerProfiler profiler = analyzer.getProfiler(false);
            while (m_currentSearchIdx < profiler.getAreas(EAreaType.CODE_AREA, true).size()) {
                CTestProfilerStatistics area = profiler.getArea(EAreaType.CODE_AREA, m_currentSearchIdx);
                sectionId = CTestProfilerStatistics.EProfilerStatisticsSectionId.E_SECTION_AREA_NAME.swigValue();
                testBase = initScalarValue(area, sectionId, oldName, newName);
                if (testBase != null) {
                    m_testEditorNodeId = ENodeId.PROFILER_CODE_AREAS_NODE;
                    m_isSelectLineInTable = true;
                    m_tableRow = m_currentSearchIdx;
                    m_tableColumn = 0;
                    m_currentSearchIdx++;
                    SetSectionAction action = new SetSectionAction(testBase, m_testEditorNodeId, m_newValue);
                    return action;
                }
                m_currentSearchIdx++;
            }
            m_currentSearchSection = ERenameSections.E_SCRIPT;
            m_currentSearchIdx = 0;
            // no break - if profiler does not match, continue with scripts
        case E_SCRIPT:
            // scripts
            CTestFunction func = null;
            sectionId = CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue();

            while (m_currentSearchIdx < 4) {
                switch (m_currentSearchIdx) {
                case 0:
                    func = testSpec.getInitFunction(false);
                    break;
                case 1:
                    func = testSpec.getEndFunction(false);
                    break;
                case 2:
                    func = testSpec.getInitTargetFunction(false);
                    break;
                case 3:
                    func = testSpec.getRestoreTargetFunction(false);
                    break;
                }

                m_currentSearchIdx++;

                testBase = initScalarValue(func, sectionId, oldName, newName);
                if (testBase != null) {
                    m_testEditorNodeId = ENodeId.SCRIPT_NODE;
                    m_isSelectLineInTable = false;
                    SetSectionAction action = new SetSectionAction(testBase, m_testEditorNodeId, m_newValue);
                    return action;
                }
            }
            m_currentSearchSection = ERenameSections.E_NULL;
            m_currentSearchIdx = 0;
            break;
        default:
            String msg = "Invalid rename section when renaming function: " + m_currentSearchSection;
            Activator.log(Status.ERROR, msg, new Throwable());
        }
        
        return null;
    }


    private AbstractAction renameVariable(CTestSpecification testSpec,
                                          String oldName,
                                          String newName) {
        int sectionId = -1;
        // if new obj will be returned, there will be nothing to rename anyway
        CTestFunction functionUnderTest = testSpec.getFunctionUnderTest(true); 
        CTestAnalyzer analyzer = testSpec.getAnalyzer(false);
        
        switch (m_currentSearchSection) {
        case E_NULL:
            m_currentSearchSection = ERenameSections.E_FUNCTIONS;
        case E_FUNCTIONS:
            // function under test
            sectionId = CTestFunction.ESection.E_SECTION_FUNC_NAME.swigValue();

            int paramsSectionId = CTestFunction.ESection.E_SECTION_PARAMS.swigValue();
            CSequenceAdapter params = new CSequenceAdapter(functionUnderTest, 
                                                           paramsSectionId, 
                                                           false);
            while (m_currentSearchIdx < params.size()) {
                
                if (params.getValue(m_currentSearchIdx).equals(oldName)) {
                
                    m_testEditorNodeId = ENodeId.FUNCTION_NODE;
                    m_isSelectLineInTable = false;
                    
                    m_newValue = YamlScalar.newListElement(paramsSectionId, 
                                                           m_currentSearchIdx);
                    m_newValue.setValue(newName);
                    SetSectionAction action = new SetSectionAction(functionUnderTest, 
                                                                   m_testEditorNodeId, 
                                                                   m_newValue);
                    m_currentSearchIdx++;
                    return action;
                }
                m_currentSearchIdx++;
            }
            
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_DECL_VARS;
            // no break - if param name does not match, continue with var declarations
        case E_DECL_VARS:
            m_testEditorNodeId = ENodeId.VARS_NODE;
            m_currentTableId = VariablesSpecEditor.DECL_TABLE_ID;
            CMapAdapter decls = new CMapAdapter(testSpec, 
                                                SectionIds.E_SECTION_LOCALS.swigValue(),
                                                true);
            while (m_currentSearchIdx < decls.size()) {
                m_tableRow = m_currentSearchIdx;
                String varName = decls.getKey(m_currentSearchIdx++);
            
                String newVarName = replaceVar(varName, oldName, newName); 
                if (newVarName != null) {
                    AbstractAction action = new ReplaceMappingKeyAction(testSpec, 
                                                 SectionIds.E_SECTION_LOCALS.swigValue(), 
                                                 varName, 
                                                 newVarName);
                    m_isSelectLineInTable = true;
                    m_tableColumn = 1;
                    return action;
                }
            }
            
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_INIT_VARS;
            // no break
        case E_INIT_VARS:
            m_testEditorNodeId = ENodeId.VARS_NODE;
            m_currentTableId = VariablesSpecEditor.INIT_TABLE_ID;
            CMapAdapter inits = new CMapAdapter(testSpec, 
                                                SectionIds.E_SECTION_INIT.swigValue(),
                                                true);
            while (m_currentSearchIdx < inits.size()) {
                
                // even values of m_currentSearchIdx mean keys, odd mean values
                int mapIndex = m_currentSearchIdx / 2;
                m_currentSearchIdx++;
                String varName = inits.getKey(mapIndex);
                
                if ((m_currentSearchIdx - 1) % 2 == 0) {
            
                    // rename variables - lvalues
                    String newVarName = replaceVar(varName, oldName, newName); 
                    if (newVarName != null) {
                        AbstractAction action = new ReplaceMappingKeyAction(testSpec, 
                                                                            SectionIds.E_SECTION_INIT.swigValue(), 
                                                                            varName, 
                                                                            newVarName);
                        m_isSelectLineInTable = true;
                        m_tableRow = mapIndex;
                        return action;
                    }
                    
                } else {
                    // rename variables in assignments
                    String value = inits.getValue(varName);
                    String newVarValue = replaceVar(value, oldName, newName); 
                    if (newVarValue != null) {
                        YamlScalar yScalar = YamlScalar.newUserMapping(SectionIds.E_SECTION_INIT.swigValue(), 
                                                                       varName);
                        yScalar.dataFromTestSpec(testSpec);
                        yScalar.setValue(newVarValue);
                        AbstractAction action = new SetSectionMappingAction(testSpec, 
                                                                            m_testEditorNodeId,
                                                                            yScalar);
                        m_isSelectLineInTable = true;
                        m_tableRow = mapIndex;
                        m_tableColumn = 1;
                        return action;
                    }
                }
            }
            
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_RET_VAL;
            // no break
        case E_RET_VAL:
            // return values
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_EXPECTED;  // there is at most one return value, so section 2 will follow
            sectionId = CTestFunction.ESection.E_SECTION_RET_VAL_NAME.swigValue();

            CTestBase testBase = initScalarValue(functionUnderTest, sectionId, oldName, newName);
            if (testBase != null) {
                m_testEditorNodeId = ENodeId.FUNCTION_NODE;
                m_isSelectLineInTable = false;
                SetSectionAction action = new SetSectionAction(testBase, m_testEditorNodeId, m_newValue);
                return action;
            }
            // no break
        case E_EXPECTED:
            // expected expressions
            if (m_currentSearchIdx == 0) {
                m_expectedExpressions = new CSequenceAdapter(testSpec.getAssert(false), 
                                                             ESectionAssert.E_SECTION_ASSERT_EXPRESSIONS.swigValue(), 
                                                             true);
            }
            
            int numExpressions = (int)m_expectedExpressions.size();
            while (m_currentSearchIdx < numExpressions) {
                String oldExpr = m_expectedExpressions.getValue(m_currentSearchIdx);
                String newName1 = replaceVar(oldExpr, oldName, newName); 
                if (newName1 != null) {
                    
                    m_testEditorNodeId = ENodeId.EXPECTED_NODE;
                    YamlScalar scalar = YamlScalar.newListElement(ESectionAssert.E_SECTION_ASSERT_EXPRESSIONS.swigValue(),
                                                                  m_currentSearchIdx);
                    scalar.setValue(newName1);
                    SetSequenceItemAction action = new SetSequenceItemAction(testSpec.getAssert(false), 
                                                                             ENodeId.EXPECTED_NODE, 
                                                                             scalar);
                    m_isSelectLineInTable = true;
                    m_tableRow = m_currentSearchIdx;
                    m_tableColumn = 1;
                    m_currentSearchIdx++;
                    return action;
                }
                m_currentSearchIdx++;
            }
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_STUBS;
            // no break
        case E_STUBS:
            // variables in stub assignments
            CTestBaseList stubs = testSpec.getStubs(true);
            
            while ((m_currentSearchIdx) < stubs.size()) {
                
                CTestStub stub = CTestStub.cast(stubs.get(m_currentSearchIdx++));

                // rename variable in all steps
                CTestBaseList steps = stub.getAssignmentSteps(true);
                AbstractAction action = renameVarsInAssignmentSteps(oldName,
                                                                    newName,
                                                                    steps,
                                                                    ENodeId.STUBS_NODE);
                if (action != null) {
                    return action;
                }
            }

            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_TEST_POINTS;
            // no break
        case E_TEST_POINTS:
            // test point assignments
            CTestBaseList testPoints = testSpec.getTestPoints(true);
            
            while ((m_currentSearchIdx) < testPoints.size()) {
                
                CTestPoint testPoint = CTestPoint.cast(testPoints.get(m_currentSearchIdx++));

                // rename variable in all steps
                CTestBaseList steps = testPoint.getSteps(true);
                AbstractAction action = renameVarsInAssignmentSteps(oldName,
                                                                    newName,
                                                                    steps,
                                                                    ENodeId.TEST_POINT_NODE);
                if (action != null) {
                    return action;
                }
            }

            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_PERSIST_VARS;
            // no break
        case E_PERSIST_VARS:
            // persistent variables declarations
            GroupAction grpAction = new GroupAction("Rename persistent variables declaration");
            CTestPersistentVars persistentVars = testSpec.getPersistentVars(true);
            m_currentTableId = PersistentVarsEditor.DECL_TABLE_ID; // declarations table
            CMapAdapter declarations = new CMapAdapter(persistentVars, 
                                                       EPersistVarsSections.E_SECTION_DECL.swigValue(), 
                                                       true);
            StrVector keys = new StrVector();
            declarations.getKeys(keys);
            int numVars = (int) keys.size();
            
            while (m_currentSearchIdx < numVars) {
                
                m_tableRow = m_currentSearchIdx;
                String key = keys.get(m_currentSearchIdx++);
                String value = declarations.getValue(key);
                
                if (key.equals(oldName)) {
                    AbstractAction action = new ReplaceMappingKeyAction(persistentVars, 
                                                EPersistVarsSections.E_SECTION_DECL.swigValue(),
                                                key,
                                                newName);
                    grpAction.add(action);
                }
                
                if (value.equals(oldName)) {
                    m_newValue = YamlScalar.newUserMapping(sectionId, key);
                    m_newValue.dataFromTestSpec(persistentVars);
                    m_newValue.setValue(newName);

                    AbstractAction action = new SetSectionMappingAction(persistentVars, 
                                                                        ENodeId.PERSISTENT_VARS_NODE, 
                                                                        m_newValue);
                    grpAction.add(action);
                }
            
                if (!grpAction.isEmpty()) {
                    m_testEditorNodeId = ENodeId.PERSISTENT_VARS_NODE;
                    m_isSelectLineInTable = true;
                    m_tableColumn = 0;
                    return grpAction;
                }
            }
            
            
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_DELETED_PERSIST_VARS;
            // no break
        case E_DELETED_PERSIST_VARS:
            // deleted persistent variables
            persistentVars = testSpec.getPersistentVars(true);
            m_currentTableId = PersistentVarsEditor.DELETE_TABLE_ID; // declarations table
            
            CSequenceAdapter delVars = new CSequenceAdapter(persistentVars, 
                                           EPersistVarsSections.E_SECTION_DELETE.swigValue(), 
                                           true);
            numVars = (int) delVars.size();
            
            while (m_currentSearchIdx < numVars) {
                
                String varName = delVars.getValue(m_currentSearchIdx);
                
                if (varName.equals(oldName)) {
                    YamlScalar scalar = YamlScalar.newListElement(EPersistVarsSections.E_SECTION_DELETE.swigValue(),
                                                                  m_currentSearchIdx);
                    scalar.setValue(newName);
                    SetSequenceItemAction action = new SetSequenceItemAction(persistentVars, 
                                                                             ENodeId.PERSISTENT_VARS_NODE, 
                                                                             scalar);
                    m_testEditorNodeId = ENodeId.PERSISTENT_VARS_NODE;
                    m_isSelectLineInTable = true;
                    m_currentSearchIdx++;
                    m_tableRow = m_currentSearchIdx;
                    m_tableColumn = 0;
                    return action;
                }
                m_currentSearchIdx++;
            }
            
            m_currentTableId = 0;
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_PROFILER;
            // no break
        case E_PROFILER:
            // profiler data areas
            CTestAnalyzerProfiler profiler = analyzer.getProfiler(false);
            
            while (m_currentSearchIdx < profiler.getAreas(EAreaType.DATA_AREA, true).size()) {

                m_tableRow = m_currentSearchIdx;

                CTestProfilerStatistics area = 
                        profiler.getArea(EAreaType.DATA_AREA, m_currentSearchIdx++);
                
                sectionId = CTestProfilerStatistics.EProfilerStatisticsSectionId.E_SECTION_AREA_NAME.swigValue();
                CTestBase testProfBase = initScalarValue(area, sectionId, oldName, newName);
                
                if (testProfBase != null) {
                    m_testEditorNodeId = ENodeId.PROFILER_DATA_AREAS_NODE;
                    m_isSelectLineInTable = true;
                    m_tableColumn = 0;
                    SetSectionAction setAction = new SetSectionAction(testProfBase, m_testEditorNodeId, m_newValue);
                    return setAction;
                }
            }
            m_currentSearchIdx = 0;
            m_currentSearchSection = ERenameSections.E_NULL;
            break;
        default:
            String msg = "Invalid rename section when renaming variables: " + m_currentSearchSection;
            Activator.log(Status.ERROR, msg, new Throwable());
        }
        
        return null;
    }


    private AbstractAction renameVarsInAssignmentSteps(String oldName,
                                                       String newName,
                                                       CTestBaseList steps, 
                                                       ENodeId nodeId) {
        int sectionId;
        int numSteps = (int) steps.size();

        GroupAction grpAction = new GroupAction("Rename var in assignment steps");
        AbstractAction action = null;
        sectionId = CTestEvalAssignStep.EStepSectionIds.E_SECTION_ASSIGN.swigValue();
        for (int stepIdx = 0; stepIdx < numSteps; stepIdx++) {
            CTestEvalAssignStep step = CTestEvalAssignStep.cast(steps.get(stepIdx));
            CMapAdapter assignments = new CMapAdapter(step, sectionId, true);
            StrVector keys = new StrVector();
            assignments.getKeys(keys);

            int numVars = (int) keys.size();
            for (int assIdx = 0; assIdx < numVars; assIdx++) {
                String key = keys.get(assIdx);
                String value = assignments.getValue(key);
                
                if (key.equals(oldName)) {
                    action = new ReplaceMappingKeyAction(step, 
                                                         CTestEvalAssignStep.EStepSectionIds.E_SECTION_ASSIGN.swigValue(),
                                                         key,
                                                         newName);
                    
                    /* action.addEvent(new ModelChangedEvent(EventType.TEST_SPEC_DATA_CHANGED, 
                                                          testSpec, 
                                                          null, ENodeId.STUBS_NODE)); */

                    grpAction.add(action);
                }
                
                if (value.equals(oldName)) {
                    m_newValue = YamlScalar.newUserMapping(sectionId, key);
                    m_newValue.dataFromTestSpec(step);
                    m_newValue.setValue(newName);

                    action = new SetSectionMappingAction(step, 
                                                         nodeId, 
                                                         m_newValue);
                    grpAction.add(action);
                }
            }
        }
        
        if (action != null) {
            m_testEditorNodeId = nodeId;
            m_isSelectLineInTable = true;
            m_tableRow = 0;
            m_tableColumn = 1;

            return grpAction;
        }
        
        return null;
    }

    
    private AbstractAction renameType(CTestSpecification testSpec,
                                      String oldName,
                                      String newName) {

        m_testEditorNodeId = ENodeId.VARS_NODE;
        m_currentTableId = VariablesSpecEditor.DECL_TABLE_ID;
        CMapAdapter decls = new CMapAdapter(testSpec, 
                                            SectionIds.E_SECTION_LOCALS.swigValue(),
                                            true);
        
        while (m_currentSearchIdx < decls.size()) {
            m_tableRow = m_currentSearchIdx;
            String varName = decls.getKey(m_currentSearchIdx++);
            String typeName = decls.getValue(varName);
        
            String newTypeName = replaceVar(typeName, oldName, newName);
            
            if (newTypeName != null) {
                YamlScalar yScalar = YamlScalar.newUserMapping(SectionIds.E_SECTION_LOCALS.swigValue(), 
                                                               varName);
                yScalar.dataFromTestSpec(testSpec);
                yScalar.setValue(newTypeName);
                AbstractAction action = new SetSectionMappingAction(testSpec, 
                                                                    ENodeId.VARS_NODE,
                                                                    yScalar);
                m_isSelectLineInTable = true;
                m_tableColumn = COLUMN_VAR_TYPES;
                return action;
            }
        }
        
        m_currentSearchSection = ERenameSections.E_NULL;
        m_currentSearchIdx = 0;
        
        return null;
    }

    
    private AbstractAction renameHILParameters(CTestSpecification testSpec,
                                               String oldName,
                                               String newName) {
        CTestHIL hil = testSpec.getHIL(false);
        StrVector hilKeys = new StrVector();
        StrVector hilValues = new StrVector();
        hil.getHILParamKeys(hilKeys);
        hil.getHILParamValues(hilValues);
        long numKeys = hilKeys.size();
        while (m_currentSearchIdx < numKeys) {
            GroupAction groupAction = new GroupAction("Rename items in HIL settings");
            
            String key = hilKeys.get(m_currentSearchIdx);
            String value = hilValues.get(m_currentSearchIdx);

            String newKey = replaceVar(key, oldName, newName); 
            String newValue = replaceVar(value, oldName, newName); 

            m_testEditorNodeId = ENodeId.HIL_NODE;
            int section = ETestHILSections.E_SECTION_HIL_PARAMS.swigValue();
            if (newKey != null) {
                ReplaceMappingKeyAction replKeyAction = new ReplaceMappingKeyAction(hil, 
                                                                                    section, 
                                                                                    key, newKey);
                groupAction.add(replKeyAction);
            }
            
            if (newValue != null) {
                YamlScalar scalar = YamlScalar.newUserMapping(section, newKey);
                scalar.setValue(newValue);
                SetSectionMappingAction setValueaction = new SetSectionMappingAction(hil, 
                                                                                     m_testEditorNodeId, 
                                                                                     scalar);
                groupAction.add(setValueaction);
            }
            
            m_isSelectLineInTable = true;
            m_tableRow = m_currentSearchIdx++;
            m_tableColumn = 0;
            if (!groupAction.isEmpty()) {
                return groupAction;
            }
        }
        
        m_currentSearchSection = ERenameSections.E_NULL;
        m_currentSearchIdx = 0;
        
        return null;
    }

    
    private AbstractAction renameTags(CTestSpecification testSpec,
                                      String oldName,
                                      String newName,
                                      boolean isRenameAll) {
        switch (m_currentSearchSection) {
        case E_NULL:
        case E_TAGS:
            // tags
            StrVector tags = new StrVector();
            testSpec.getTags(tags);
            AbstractAction action = renameTags(testSpec, oldName, newName, tags,
                                               SectionIds.E_SECTION_TAGS.swigValue());

            m_testEditorNodeId = ENodeId.META_NODE;
            m_isSelectLineInTable = false;
            m_currentSearchSection = ERenameSections.E_FILTER; // advance section in any case
            m_currentSearchIdx = 0;

            if (action != null) {
                return action;
            }
            
        case E_FILTER:
            // filters
            if (!m_isCheckFilters) {
                break;
            }
            // rename for tags in filters should be done only once per cycle,
            // because there is one set of filters per model, not per test 
            // specification
            m_isCheckFilters = false;
            CTestBaseList filters = m_model.getTestFilters();
            int numFilters = (int)filters.size();
            
            while (m_currentSearchIdx < numFilters) {
                CTestFilter filter = CTestFilter.cast(filters.get(m_currentSearchIdx++));
                String filterId = filter.getFilterId();

                StrVector tagsHAT = new StrVector();
                StrVector tagsHOOT = new StrVector();
                StrVector tagsNHAT = new StrVector();
                StrVector tagsNHOOT = new StrVector();
                
                filter.getMustHaveAllTags(tagsHAT);
                AbstractAction isRenamedHAT = renameTags(filter, oldName, newName, tagsHAT,
                                                         CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_HAVE_ALL_TAGS.swigValue());
                
                filter.getMustHaveOneOfTags(tagsHOOT);
                AbstractAction isRenamedHOOT = renameTags(filter, oldName, newName, tagsHOOT,
                                                          CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_HAVE_ONE_OF_TAGS.swigValue());

                filter.getMustNotHaveAllTags(tagsNHAT);
                AbstractAction isRenamedNHAT = renameTags(filter, oldName, newName, tagsNHAT,
                                                          CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_NOT_HAVE_ALL_TAGS.swigValue());

                filter.getMustNotHaveOneOfTags(tagsNHOOT);
                AbstractAction isRenamedNHOOT = renameTags(filter, oldName, newName, tagsNHOOT,
                                                           CTestFilter.ETestFilterSectionIds.E_SECTION_MUST_NOT_HAVE_ONE_OF_TAGS.swigValue());
                
                if (isRenamedHAT != null  ||  isRenamedHOOT != null  ||  
                    isRenamedNHAT != null  ||  isRenamedNHOOT != null) {
                    
                    boolean res = false;
                    if (!isRenameAll) {
                        res = MessageDialog.openQuestion(getShell(), 
                                                         "Rename tags in filters", 
                                                         "Do you want to rename tags in filter '" + 
                                                         filterId + "'?");
                    }
                    GroupAction grpAction = new GroupAction("Rename tags in filters");
                    if (res  ||  isRenameAll) {
                        grpAction.add(isRenamedHAT);
                        grpAction.add(isRenamedHOOT);
                        grpAction.add(isRenamedNHAT);
                        grpAction.add(isRenamedNHOOT);
                        
                        if (!isRenameAll) {
                            // exec action here, as user has already confirmed it
                            // and Rename button is not active for filters
                            renameSingle(grpAction);
                        }
                        
                        m_isSelectLineInTable = false;
                        return grpAction;
                    }
                }
            }
            break;
        default:
            String msg = "Invalid rename section when renaming tags: " + m_currentSearchSection;
            Activator.log(Status.ERROR, msg, new Throwable());
        }
        
        return null;
    }

    
    private AbstractAction renameTags(CTestBase testBase, 
                                      String oldName,
                                      String newName,
                                      StrVector tags,
                                      int sectionId) {
        int numTags = (int)tags.size();
        // rename all tags, which match - it makes no sense to have 
        // one tag specified multiple times anyway.
        for (int i = 0; i < numTags; i++) {
            if (tags.get(i).equals(oldName)) {
                YamlScalar scalar = YamlScalar.newListElement(sectionId, i);
                scalar.setValue(newName);
                SetSequenceItemAction action = new SetSequenceItemAction(testBase, 
                                                                         m_testEditorNodeId, 
                                                                         scalar);
                return action;
            }
        }
        
        return null;
    }

/*
    private VariablesAction createVariablesAction(CTestSpecification testSpec) {
        YamlScalar localsKey = YamlScalar.newKey(SectionIds.E_SECTION_LOCALS.swigValue());
        YamlScalar initKey = YamlScalar.newKey(SectionIds.E_SECTION_INIT.swigValue());
        localsKey.dataFromTestSpec(testSpec);
        initKey.dataFromTestSpec(testSpec);
        
        CMapAdapter declMap = new CMapAdapter();
        CMapAdapter initMap = new CMapAdapter();
        
        int varIdx = 0;
        for (String[] row : m_variablesList) {
            String type = row[LocalsSpecEditor.COL_TYPES].trim();
            // quoting of var names is required, because {a[1]: 2} is not valid for YAML 
            // parser - '[' and ']' indicate YAML sequence 
            String var = row[LocalsSpecEditor.COL_VARIABLES].trim();
            String value = row[LocalsSpecEditor.COL_VALUES].trim();
            TableRowComment rowComment = m_commentsList.get(varIdx++);

            if (var.isEmpty()  &&  (!type.isEmpty()  ||  !value.isEmpty())) {
                MessageDialog.openWarning(getShell(), "Variable name is empty!", 
                                          "If type or value is defined, variable name must not be empty! Renaming skipped!" +
                                          " Type: '" + type + "', value: '" + value);
            }

            if (!type.isEmpty()) {
                declMap.setValue(var, type, rowComment.getLeftNlComment(), 
                                            rowComment.getLeftEolComment());
            }

            if (!value.isEmpty()) {
                initMap.setValue(var, value, rowComment.getRightNlComment(), 
                                            rowComment.getRightNlComment());
            }
        }
        
        VariablesAction action = new VariablesAction(testSpec, 
                                                     declMap, 
                                                     initMap,
                                                     localsKey, 
                                                     initKey,
                                                     false,
                                                     false);
        action.addAllFireEventTypes();

        m_testEditorNodeId = ENodeId.VARS_NODE;
        return action;
    }


    private void initVariablesList(CTestSpecification testSpec) {
        if (m_currentSearchIdx == 0) {
            StrStrMap localVars = new StrStrMap();
            StrVector localsVector = new StrVector();
            StrVector initVals = new StrVector();
            StrVector initKeys = testSpec.getInitKeys();
            testSpec.getInitValues(initVals);
            testSpec.getLocalVariables(localVars);
            testSpec.getLocalVariablesKeys(localsVector);
            
            m_variablesList = 
                LocalsSpecEditor.localsToStringArrayList(initKeys, 
                                                        initVals, 
                                                        localVars,
                                                        localsVector);
            
            // Save comments to local list. Even if variable will be renamed,
            // comment for its declaration or assignment should not change.
            // Because of renaming we are not allowed to use map.
            
            // use Set just to speed-up lookup
            Set<String> initKeysSet = new TreeSet<String>();
            int numInitKeys = (int)initKeys.size();
            for (int i = 0; i < numInitKeys; i++) {
                initKeysSet.add(initKeys.get(i));
            }

            m_commentsList = new ArrayList<TableRowComment>();

            for (String[] varsLine : m_variablesList) {
                String varName = varsLine[LocalsSpecEditor.COL_VARIABLES];
                m_commentsList.add(LocalsSpecEditor.buildRowCommentsObject(testSpec, 
                                                                           testSpec, 
                                                                           localVars, 
                                                                           initKeysSet, 
                                                                           varName));
            }
        }
    }
*/

    /**
     * This method replaces names of variables in string. Since variable initializations
     * may contain other chars than only var name, for example "counter[2]" or "*ptr",
     * other characters are preserved in the resulting string. On the other hand,
     * substrings are not modified. For example, if var name is 'baloon' and
     * user wants to replace var name 'on' with 'off', then 'baloon' does not change.
     *  
     * @param oldQualifiedName
     * @param oldName
     * @param newName
     * @return
     */
    protected static String replaceVar(String oldQualifiedName, String oldName, String newName) {

        String regex = "\\b" + oldName + "\\b";
        String newQualifiedName = oldQualifiedName.replaceAll(regex, "" + newName + "");
        
        if (newQualifiedName.equals(oldQualifiedName)) {
            return null;
        }
        
        return newQualifiedName;
    }


    private CTestBase initScalarValue(CTestBase testBase, 
                                      int sectionId, 
                                      String oldName,
                                      String newName) {
        
        String oldVal = testBase.getTagValue(sectionId);
        if (oldVal.equals(oldName)) {
            m_newValue = YamlScalar.newValue(sectionId);
            m_newValue.dataFromTestSpec(testBase);
            m_newValue.setValue(newName);
            
            return testBase;
        }
        return null;
    }


    private void renameSingle(AbstractAction action) {
        try {
            execAndRefresh(action, action.getContainerTreeNode());
        } catch (Exception ex) {
            SExceptionDialog.open(getShell(), "Rename failed!", ex);
        }
    }
    

    
    public void setTestSpec(CTestSpecification testSpec) {
        m_containerTestSpec = testSpec;
        initSearchVars();
    }
    
    
    public boolean show() {
        return open() == Window.OK;
    }
}
