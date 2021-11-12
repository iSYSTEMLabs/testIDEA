package si.isystem.itest.wizards;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestPoint;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestStub;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.spec.BPLogLocationControls;
import si.isystem.itest.wizards.TCGenOccur.EOccurenceType;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.SWTX;

/**
 * This class implements wizard page for all sections but dry run. Sections are 
 * the same with exception of stubs and test points, which have few additional
 * controls at the bottom of the page. Only one stub and test point can be 
 * created by test case generator at the moment.
 *  
 * @author markok
 *
 */
public class TCGenIdentifiersPage extends WizardPage {

    enum TCGenPageType {
        GENERAL, FUNCTION, STUBS, TEST_POINTS, TEST_CASES
    }
    
    private TCGenSection m_tcGenSection;

    private CTestSpecification m_testSpec;
    private String m_coreId;
    
    // UI controls
    private Button[] m_sectionOccurrenceRadios;
    private Text m_valueOccurrenceTxt;
    private TCGenTableModel m_valuesTableModel;

    private KTable m_identifiersTable;

//    private KTable m_addedVectorsTable;
//    private KTable m_removedVectorsTable;

    private KTable m_vectorTable;

    private TCGenVectorsTableModel m_vectorsTableModel;

    private TCGenPageType m_pageType;

    // stub and test point page specific items
    private GlobalsSelectionControl m_stubbTpGSC;
    private Text m_stubTPStepIdxTxt;
    private Map<String, CTestLocation> m_locations;
    
    // test case page specific items
    private Button m_isCopyCoverageCb;
    private Button m_isAppendModeCb;
    private Button m_isCopyProfilerCb;
    private Button m_isCopyTraceCb;
    


    private IModelChangedListener m_modelChangedListener;


    FocusListener m_focusListener = new FocusListener() {
        
        @Override
        public void focusLost(FocusEvent e) {
            m_modelChangedListener.modelChanged(false); // no need to redraw identifiers table
        }
        
        @Override
        public void focusGained(FocusEvent e) {}
    };
    
    
    FocusListener m_nTimesFocusListener = new FocusListener() {
        
        @Override
        public void focusLost(FocusEvent e) {
            String nStr = m_valueOccurrenceTxt.getText();
            m_valuesTableModel.setSectionOccurrenceAsNTimesValue(nStr);
            m_modelChangedListener.modelChanged(false); // no need to redraw identifiers table
        }
        
        @Override
        public void focusGained(FocusEvent e) {}
    };
    

    private String[] m_identifierProposals = {};
    private String[] m_identifierDescriptions = {};
    private String m_col0Title = "Identifiers";

    private Button m_isShowAllPagesBtn;
    private static boolean m_isShowAllPages; // testIDEA instance persistence
    private Button m_isDeleteExistingDerivedTestCasesBtn;

    private static boolean m_isDeleteExistingDerivedTestCases; // // testIDEA instance persistence
    

    public TCGenIdentifiersPage(String title,
                                String description,
                                CTestSpecification testSpec, 
                                TCGenSection tcGenSection) {
        this(title, description, testSpec, tcGenSection, TCGenPageType.GENERAL);
    }

    
    /** This ctor is to be used for stub and test point pages. */
    public TCGenIdentifiersPage(String title,
                                String description,
                                CTestSpecification testSpec, 
                                TCGenSection tcGenSection,
                                TCGenPageType pageType) {
        super(title);
        setTitle(title);
        setDescription(description);
        
        m_tcGenSection = tcGenSection;
        m_pageType = pageType;
        m_testSpec = testSpec;
    }

    
    void setCoreId(String coreId) {
        m_coreId = coreId;
    }
    

    public void setAutoCompleteProposals(String [] identifiers, String [] descriptions) {
        m_identifierProposals = identifiers;
        m_identifierDescriptions = descriptions;
    }
    
    
    public void setColumn0Title(String col0Title) {
        m_col0Title = col0Title;
    }
    
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        // wizard dialog size is set in handler
        container.setLayout(new MigLayout("fill", "[min!][fill][min!][min!]", "[min!][fill][min!][min!]"));

        KGUIBuilder builder = new KGUIBuilder(container);
        
        createOccurrenceControl(builder);

        createTablesSash(builder);

        if (m_pageType == TCGenPageType.STUBS  ||  m_pageType == TCGenPageType.TEST_POINTS) { 
            createStubsTestPointSpecificControls(builder);
        }

        if (m_pageType == TCGenPageType.TEST_CASES) {
            createTestCaseSpecificControls(builder);
        }
        
        setControl(container);

        fillControls();
    }


    /**
     * This sash contains identifiers table and table with generated vectors 
     * below it.
     * 
     * @param builder
     */
    private void createTablesSash(KGUIBuilder builder) {
        
        SashForm sash = new SashForm(builder.getParent(), SWT.VERTICAL);
        sash.SASH_WIDTH = 3;
        // if artifacts on top of the bottom table are too annoying, try to
        // replace MigLayout with FormLayout. See usage of Sash in TestSpecificationEditorView. 
        // FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(sash);
        sash.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_GRAY));

        // wizard dialog size is set in handler
        sash.setLayoutData("hmin 400, grow, span 4, gapbottom 15, wrap");

        Composite inputPanel = new Composite(sash, SWT.NONE);
        inputPanel.setLayout(new MigLayout("fill, ins 0", "[fill]", "[fill]"));
        
        m_identifiersTable = new KTable(inputPanel, true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                                          SWTX.EDIT_ON_KEY | 
                                                          SWTX.MARK_FOCUS_HEADERS | 
                                                          SWTX.FILL_WITH_DUMMYCOL | SWT.BORDER);
        m_identifiersTable.setLayoutData("gapbottom 10");  // wmin 800, h 200:50%:50%, wrap 

        if (m_pageType == TCGenPageType.TEST_CASES) {
            m_valuesTableModel = new TCGenTestCaseTableModel(m_identifiersTable);
        } else {
            m_valuesTableModel = new TCGenTableModel(m_identifiersTable, m_col0Title);
        }
        
        m_valuesTableModel.setData(m_tcGenSection);
        m_valuesTableModel.setAutoCompleteProposals(m_identifierProposals, 
                                                    m_identifierDescriptions);
        
        m_modelChangedListener = new IModelChangedListener() {
            // this listener is required so that table gets repainted when 
            // occurrence changed from Custom to other value and the other way 
            // around and user presses Enter or Tab after entering the value. 
            @Override
            public void modelChanged(boolean isRedrawNeeded) {
                if (isRedrawNeeded) {
                    m_identifiersTable.redraw();
                }
                pageSpecificDataToModel();
                String errMsg = m_valuesTableModel.verifyModel();
                setErrorMessage(errMsg);
                if (errMsg == null) {
                    generateVectors();
                } else {
                    m_vectorsTableModel.clearVectors();
                    m_vectorTable.redraw();
                }
            }
        };
        
        m_valuesTableModel.setModelChangedListener(m_modelChangedListener);
        
        m_identifiersTable.setModel(m_valuesTableModel);
        
        m_identifiersTable.addMouseListener(new MouseListener() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {}

            @Override
            public void mouseDown(MouseEvent e) {
            }

            @Override
            public void mouseUp(MouseEvent e) {
                Point selection = m_identifiersTable.getCellForCoordinates(e.x, e.y);

                m_valuesTableModel.addRemoveItem(selection.x, selection.y,
                                                 m_identifiersTable.getCellRect(selection.x, 
                                                                                selection.y),
                                                                                e.x, e.y);

//                if (row == m_valuesTableModel.getRowCount() - 1) { // add identifier is clicked
//                    m_valuesTableModel.addIdentifier();
//                } else if (m_valuesTableModel.isAddValueOccurrence(selection.x,
//                                                                   selection.y)) {
//                    m_valuesTableModel.addValueOccurrence(selection.y);
//                }
            }
        });

        /*
         * The idea of having two additional tables, where users could enter
         * vectors, which will be added to generated set and which will be 
         * deleted from the generated set is temporarily put on hold.
         * Cons: additional tables clutter UI, users can add/delete vectors 
         * manually later. 
         * 
         * Pros: All test cases are generated in the same step, no need to later
         * manually check vectors in editor.
         * 
        m_addedVectorsTable = new KTable(builder.getParent(), true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                        SWTX.EDIT_ON_KEY | 
                                        SWTX.MARK_FOCUS_HEADERS | SWT.FULL_SELECTION |
                                        SWTX.FILL_WITH_DUMMYCOL);
        m_addedVectorsTable.setLayoutData("hmin 100, gaptop 15");
        
        m_removedVectorsTable = new KTable(builder.getParent(), true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                         SWTX.EDIT_ON_KEY | 
                                         SWTX.MARK_FOCUS_HEADERS | SWT.FULL_SELECTION |
                                         SWTX.FILL_WITH_DUMMYCOL);
        m_removedVectorsTable.setLayoutData("hmin 100, gaptop 15, wrap");
         */
        
        // additional panel removes artifacts (bold top border) and sash move  
        Composite outerPanel = new Composite(sash, SWT.NONE);
        outerPanel.setLayout(new MigLayout("fill"));
        
        Composite vectorsPanel = new Composite(outerPanel, SWT.NONE);
        vectorsPanel.setLayoutData("growx, growy");
        vectorsPanel.setLayout(new MigLayout("fill, ins 0", "fill", "[min!][fill]"));  
        // vectorsPanel.setBackground(builder.getParent().getDisplay().getSystemColor(SWT.COLOR_GREEN));
        
        KGUIBuilder vectorsBuilder = new KGUIBuilder(vectorsPanel);
        
        vectorsBuilder.label("Generated vectors:", "gaptop 5, wrap");

        m_vectorTable = new KTable(vectorsPanel, true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                           SWTX.EDIT_ON_KEY | 
                                           SWTX.MARK_FOCUS_HEADERS | SWT.FULL_SELECTION |
                                           SWTX.FILL_WITH_DUMMYCOL | SWT.BORDER);
        m_vectorTable.setLayoutData("");

        m_vectorsTableModel = new TCGenVectorsTableModel();
        m_vectorsTableModel.setData(m_tcGenSection);
        m_vectorTable.setModel(m_vectorsTableModel);
    }
    
    
    private void createOccurrenceControl(KGUIBuilder builder) {
        
        String wrap = "";
        if (m_pageType != TCGenPageType.FUNCTION  &&  m_pageType != TCGenPageType.TEST_CASES) {
            wrap = ", wrap";
        }


        KGUIBuilder radioBuilder = builder.group("Default occurrence of values", 
                                                 "span 2" + wrap);
        m_sectionOccurrenceRadios = new Button[EOccurenceType.values().length];
        
        m_sectionOccurrenceRadios[EOccurenceType.ONE.ordinal()] =
               radioBuilder.radio(EOccurenceType.ONE.getUIString(), "gapright 10");
                      
        m_sectionOccurrenceRadios[EOccurenceType.TWO.ordinal()] =
                radioBuilder.radio(EOccurenceType.TWO.getUIString(), "gapright 10");
        
        m_sectionOccurrenceRadios[EOccurenceType.THREE.ordinal()] =
                radioBuilder.radio(EOccurenceType.THREE.getUIString(), "gapright 10");
        
        m_sectionOccurrenceRadios[EOccurenceType.N_TIMES.ordinal()] =
                radioBuilder.radio("", "");
        
        m_valueOccurrenceTxt = radioBuilder.text("w 40:40:40, gapright 10", SWT.BORDER);
        
        m_sectionOccurrenceRadios[EOccurenceType.MAX.ordinal()] =
                radioBuilder.radio(EOccurenceType.MAX.getUIString(), "gapright 10");
        
        m_sectionOccurrenceRadios[EOccurenceType.CUSTOM.ordinal()] =
                radioBuilder.radio(EOccurenceType.CUSTOM.getUIString(), "gapright 10");
        
        UiTools.setToolTip(m_sectionOccurrenceRadios[EOccurenceType.ONE.ordinal()], "Use each value at least once");
        UiTools.setToolTip(m_sectionOccurrenceRadios[EOccurenceType.TWO.ordinal()], "Use each value at least twice");
        UiTools.setToolTip(m_sectionOccurrenceRadios[EOccurenceType.THREE.ordinal()], "Use each value at least three-times");
        UiTools.setToolTip(m_sectionOccurrenceRadios[EOccurenceType.N_TIMES.ordinal()], "Use each value at least N-times");
        UiTools.setToolTip(m_sectionOccurrenceRadios[EOccurenceType.MAX.ordinal()], "Use each value in all possible combinations with other values.");
        UiTools.setToolTip(m_sectionOccurrenceRadios[EOccurenceType.CUSTOM.ordinal()], "Specify occurrence count independently for each value.");

        SelectionAdapter sectionOccurenceListener = new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button radio = (Button)e.getSource();
                EOccurenceType occurrenceType = (EOccurenceType)radio.getData();
                m_valuesTableModel.setSectionOccurrence(occurrenceType);
                m_valueOccurrenceTxt.setEnabled(occurrenceType == EOccurenceType.N_TIMES);
                m_identifiersTable.redraw();
                generateVectors();
            }
        };

        for (EOccurenceType oType : EOccurenceType.values()) {
            Button radioBtn = m_sectionOccurrenceRadios[oType.ordinal()];
            radioBtn.setData(oType);
            radioBtn.addSelectionListener(sectionOccurenceListener);
        }
        
        m_valueOccurrenceTxt.addFocusListener(m_nTimesFocusListener);
        
        if (m_pageType == TCGenPageType.FUNCTION) {
            m_isShowAllPagesBtn = builder.checkBox("Show all pages");
            UiTools.setToolTip(m_isShowAllPagesBtn, "If checked, pages for HIL, winIDEA Options, Script functions, Expected expressions, and Dry run are shown.\n"
                    + "If not checked, these pages are skipped, and corresonding sections will be inherited from the base test case.");
            m_isShowAllPagesBtn.setSelection(m_isShowAllPages);
            m_isShowAllPagesBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_isShowAllPages = m_isShowAllPagesBtn.getSelection();
                }
            });
        }

        if (m_pageType == TCGenPageType.TEST_CASES) {
            m_isDeleteExistingDerivedTestCasesBtn = builder.checkBox("Delete existing derived test cases",
                                                                     "skip, wrap");
            UiTools.setToolTip(m_isDeleteExistingDerivedTestCasesBtn, 
                               "If checked, existing derived test cases of the selected test case are deleted, and\n"
                    + "generated ones are added. If not checked, the generated test cases are added to existing ones.");
            m_isDeleteExistingDerivedTestCasesBtn.setSelection(m_isDeleteExistingDerivedTestCases);
            m_isDeleteExistingDerivedTestCasesBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_isDeleteExistingDerivedTestCases = m_isDeleteExistingDerivedTestCasesBtn.getSelection();
                }
            });
        }
        
        if (m_pageType == TCGenPageType.FUNCTION) {

            Button resetBtn = builder.button("Clear", "align right, wrap");
            UiTools.setToolTip(resetBtn, "If clicked, all data is cleared from wizard.\n"
                    + "This action can not be undone.");
            resetBtn.addSelectionListener(TestCaseGeneratorWizard.m_resetButtonSelectionListener);
        }
    }


    private void createStubsTestPointSpecificControls(KGUIBuilder builder) {

        String label;
        String tooltip;
        String [] proposals;
        boolean isStdSourceBtn = true;
        String emptyListText;
        
        if (m_pageType == TCGenPageType.STUBS) {
            label = "Stubbed function:";
            tooltip = "Enter name of stubbed function, that will get "
                    + "assignments from the table above.\n"
                    + "Stub for this function should be defined in the base test case.\n"
                    + "It will be copied from there and then step assignments modified.";
            CTestBaseList stubs = m_testSpec.getStubs(true);
            proposals = new String[(int)stubs.size()];
            
            for (int idx = 0; idx < stubs.size(); idx++) {
                CTestStub stub = CTestStub.cast(stubs.get(idx));
                proposals[idx] = stub.getFunctionName();
            }
            emptyListText = "---  Parent test case has no stubs ---";
        } else {
            label = "Test point ID:";
            tooltip = "Enter ID of test-point. Test point with the same ID should "
                    + "be defined in the base test case."
                    + "It will be copied from there and then step assignments modified.";
            CTestBaseList tps = m_testSpec.getTestPoints(true);
            proposals = new String[(int)tps.size()];
            m_locations = new TreeMap<>();
            
            for (int idx = 0; idx < tps.size(); idx++) {
                CTestPoint tp = CTestPoint.cast(tps.get(idx));
                proposals[idx] = tp.getId();
                m_locations.put(tp.getId(), tp.getLocation(true));
            }
            isStdSourceBtn = false;
            emptyListText = "---  Parent test case has no test points ---";
        }
            
        builder.label(label);
            
        m_stubbTpGSC = new GlobalsSelectionControl(builder.getParent(), 
                                                   "w 370:100%:100%, split",
                                                   SWT.NONE,
                                                   GlobalsContainer.GC_FUNCTIONS,
                                                   "",
                                                   false,
                                                   isStdSourceBtn,
                                                   UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                   GlobalsConfiguration.instance().getGlobalContainer(),
                                                   ConnectionProvider.instance());
        
        m_stubbTpGSC.setEmptyComboListText(emptyListText);
        UiTools.setToolTip(m_stubbTpGSC.getControl(), tooltip);
        m_stubbTpGSC.getControl().addFocusListener(m_focusListener);
        
        GlobalsProvider provider = GlobalsConfiguration.instance().
                                getGlobalContainer().getCustomGlobalsProvider();
        provider.setProposals(proposals, null);
        m_stubbTpGSC.setGlobalsProvider(GlobalsContainer.GC_CUSTOM_PROVIDER, null);
        m_stubbTpGSC.refreshProposals();
        
        if (!isStdSourceBtn) {
            SelectionAdapter listener = new SelectionAdapter() {
                
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String tpId = m_stubbTpGSC.getControl().getText();
                    if (!tpId.isEmpty()) {
                        CTestLocation location = m_locations.get(tpId);
                        if (location == null) {
                            MessageDialog.openError(getShell(), 
                                                    "Test point not found!", 
                                                    "Test point with id '" + tpId + "' does not exist!\n"
                                                    + "Change ID or create test point in the base test case!");
                            return;
                        }
                        
                        final IIConnectOperation showSourceOperation = 
                                BPLogLocationControls.createShowTestPointSourceOperation(tpId,
                                                                                         location,
                                                                                         m_coreId);

                        ISysUIUtils.execWinIDEAOperation(showSourceOperation, 
                                                         getShell(),
                                                         ConnectionProvider.instance().getDefaultConnection());
                    }
                }
            };
            
            ISysUIUtils.createShowSourceButton(builder, 
                                               "w 50::",
                                               listener);
        }

        builder.label("Step index:", "gapleft 10");
        m_stubTPStepIdxTxt = builder.text("wmin 50, wrap", SWT.BORDER);
        UiTools.setToolTip(m_stubTPStepIdxTxt, "Enter index for the step, which will get "
                           + "assignments from the table above.\n"
                           + "If no steps exist, one will be created. If index is out of range, the last step is modified.");
        
        m_stubTPStepIdxTxt.addFocusListener(m_focusListener);
    }
    
    
    private void createTestCaseSpecificControls(KGUIBuilder builder) {
        
            
        m_isCopyCoverageCb = builder.checkBox("Copy section 'Coverage' from base test case",
                                              "split");
        UiTools.setToolTip(m_isCopyCoverageCb, "If selected, analyzer and coverage sections "
                + "from base test case are copied to each of the derived test cases.");
        m_isCopyCoverageCb.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_isAppendModeCb.setEnabled(m_isCopyCoverageCb.getSelection());
            }
        });

        m_isAppendModeCb = builder.checkBox("Set open mode to 'Append'", 
                "gapleft 20, wrap");
        UiTools.setToolTip(m_isAppendModeCb, "If selected, open mode for analyzer "
                + "document is set to 'Append' after copy in generated test cases.");

        m_isCopyProfilerCb = builder.checkBox("Copy section 'Profiler' from base test case", 
                "wrap");
        UiTools.setToolTip(m_isCopyProfilerCb, "If selected, analyzer and profiler sections " 
                + "from base test case are copied to each of the derived test cases.");

        m_isCopyTraceCb = builder.checkBox("Copy section 'Trace' from base test case",
                                           "wrap");
        UiTools.setToolTip(m_isCopyTraceCb, "If selected, analyzer and trace sections "
                + "from base test case are copied to each of the derived test cases.");
        
        builder.label("Checkboxes above are disabled for sections, which are empty in the base test case.");
        
        m_isCopyCoverageCb.setEnabled(!m_testSpec.getCoverage(true).isEmpty());
        m_isAppendModeCb.setEnabled(!m_testSpec.getCoverage(true).isEmpty());
        
        m_isCopyProfilerCb.setEnabled(!m_testSpec.getProfiler(true).isEmpty());
        m_isCopyTraceCb.setEnabled(!m_testSpec.getTrace(true).isEmpty());
        
        m_isCopyCoverageCb.addFocusListener(m_focusListener);
        m_isAppendModeCb.addFocusListener(m_focusListener);
        m_isCopyProfilerCb.addFocusListener(m_focusListener);
        m_isCopyTraceCb.addFocusListener(m_focusListener);
    }
    
    
    private void generateVectors() {
        try {
            List<String[]> vectors = m_vectorsTableModel.generateVectors();
            m_vectorTable.redraw();
            verifyStubAndTestPoint(!vectors.isEmpty());
            setErrorMessage(null);
        } catch (NullPointerException | IndexOutOfBoundsException ex) {
            // these two exception mean bug, print stack trace
            String msg = ex.getMessage();
            if (msg == null) {
                msg = ""; 
            }
            setErrorMessage(ex.getClass().getSimpleName() + ": " + msg + ": " + 
                                                          ExceptionUtils.getStackFrames(ex)[1] +
                                                          '\n' + ExceptionUtils.getStackFrames(ex)[2] +
                                                          ' ' + ExceptionUtils.getStackFrames(ex)[3] );
            m_vectorsTableModel.clearVectors();
            m_vectorTable.redraw();
        } catch (Exception ex) {
            // other exception mean error in input data, stack trace is disturbing
            String msg = ex.getMessage();
            if (msg == null) {
                msg = ""; 
            }
            setErrorMessage(ex.getClass().getSimpleName() + ": " + msg);
            m_vectorsTableModel.clearVectors();
            m_vectorTable.redraw();
        }
    }

    
    private void verifyStubAndTestPoint(boolean isVectorsDefined) {
        
        if (!isVectorsDefined) {
            return;
        }
        
        if (m_pageType == TCGenPageType.STUBS) {
            String stubbedFuncName = m_stubbTpGSC.getControl().getText();
            if (stubbedFuncName.isEmpty()) {
                throw new IllegalArgumentException("Please specify name of stubbed function below!");
            }
            
            CTestStub stub = m_testSpec.getStub(stubbedFuncName);
            if (stub == null  ||  stub.isEmpty()) {
                throw new IllegalArgumentException("Stub for function "
                    + "specified below is not defined in base test case! Please define it there first.");
            }
        } else if (m_pageType == TCGenPageType.TEST_POINTS) {
            String tpId = m_stubbTpGSC.getControl().getText();
            if (tpId.isEmpty()) {
                throw new IllegalArgumentException("Please specify ID of test point below!");
            }
            
            CTestPoint tp = m_testSpec.getTestPoint(tpId);
            if (tp == null  ||  tp.isEmpty()) {
                throw new IllegalArgumentException("Test point with ID "
                    + "specified below is not defined in base test case! Please define it there first.");
            }
        }
    }


    List<String[]> getVectors() {
        return m_vectorsTableModel.generateVectors();
    }


    boolean isShowAllPages() {
        if (m_isShowAllPagesBtn != null) {
            return m_isShowAllPagesBtn.getSelection();
        }
        
        return false;
    }
    
    
    boolean isDeleteExistingDerivedTestCases() {
        if (m_isDeleteExistingDerivedTestCasesBtn != null) {
            return m_isDeleteExistingDerivedTestCases;
        }
        
        return false;
    }
    
    
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }
    
    
    void pageSpecificDataToModel() {
        
        if (m_stubbTpGSC != null) { // if not null, it is stub or TP page
            
            String funcOrTPId = m_stubbTpGSC.getControl().getText();
            String stepIdx = m_stubTPStepIdxTxt.getText();
            
            m_tcGenSection.setStubOrTPInfo(funcOrTPId, stepIdx);
        }
        
        if (m_isCopyCoverageCb != null) {
            m_tcGenSection.setAnalyzerInfo(m_isCopyCoverageCb.getSelection(),
                                           m_isAppendModeCb.getSelection(),
                                           m_isCopyProfilerCb.getSelection(),
                                           m_isCopyTraceCb.getSelection());
        }
    }
    
    
    void setNewData(TCGenSection tcGenSection) {
        m_tcGenSection = tcGenSection;
        m_valuesTableModel.setData(tcGenSection);
        m_vectorsTableModel.setData(tcGenSection);
        fillControls();
    }
    
    
    private void fillControls() {

        TCGenOccur occurrence = m_tcGenSection.getOccurrence();
        m_sectionOccurrenceRadios[occurrence.getOccurrenceType().ordinal()].setSelection(true);
        m_valueOccurrenceTxt.setText(occurrence.getNTimesValue());

        m_valueOccurrenceTxt.setEnabled(occurrence.getOccurrenceType() == EOccurenceType.N_TIMES);
        
        if (m_stubbTpGSC != null) {
            m_stubbTpGSC.setText(m_tcGenSection.getStubbedFuncOrTestPointId());
            m_stubTPStepIdxTxt.setText(m_tcGenSection.getStubOrTpStepIndex());
        }
        
        if (m_isCopyCoverageCb != null) {
            m_isCopyCoverageCb.setSelection(m_tcGenSection.isCopyCoverage()  &&  m_isCopyCoverageCb.isEnabled());
            m_isAppendModeCb.setSelection(m_tcGenSection.isAppendModeOnCopy()  &&  m_isCopyCoverageCb.isEnabled());
            m_isCopyProfilerCb.setSelection(m_tcGenSection.isCopyProfiler()  &&  m_isCopyProfilerCb.isEnabled());
            m_isCopyTraceCb.setSelection(m_tcGenSection.isCopyTrace()  &&  m_isCopyTraceCb.isEnabled());
            
            m_isAppendModeCb.setEnabled(m_isCopyCoverageCb.getSelection());
        }
        
        m_modelChangedListener.modelChanged(true); // verifies the model
    }
    
   
    @Override
    public boolean canFlipToNextPage() {
        
        return super.canFlipToNextPage();
    }
}


