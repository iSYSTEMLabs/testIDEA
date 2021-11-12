package si.isystem.itest.dialogs;

import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swt.MigLayout;

import org.apache.poi.hssf.util.HSSFColor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import si.isystem.connect.CTestSpecification;
import si.isystem.connect.IntVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.xls.HSSFColorTableModel;
import si.isystem.itest.xls.TableExporter;
import si.isystem.itest.xls.TableImporter.EImportScope;
import si.isystem.itest.xls.XLSExportLookAndFeel;
import si.isystem.mk.utils.PlatformUtils;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FileNameBrowser;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.SWTX;

/**
 * This class implements methods common to both EXportImportDialog used in
 * tesIDEA RCP, and Export/Import Wizards used in testIDEA plug-in.
 */
public class ExportImportUIControls {

    private static final String SUFFIX_COLOR = "_color";
    private static final String SUFFIX_IS_VISIBLE = "_isVisible";
    private static final String EXPORT_TABLE_CONFIG_NODE = "exportTableConfig";
    protected static final String EXPORT_FMT_KEY = "exportFormat";
    protected static final String SELECTED_EXPORT_FORMAT = "si.isystem.itest.SelectedExportFormat";
    protected static final String IS_OPEN_DEFAULT_APP = "si.isystem.itest.isOpenDefaultAppAfterExport";
    private static final String IMPORT_SCOPE = "si.isystem.itest.importScope";
    
    private Button m_isOpenDefaultApplicationCb;
    private String m_fileName = "";
    private EExportFormats m_selectedFormat = EExportFormats.XLSX;
    private boolean m_isOpenDefaultApplication = false;
    private Map<EExportFormats, FileNameBrowser> m_fileNameTxts = new TreeMap<>();
    private Map<EExportFormats, Button> m_formatBtns = new TreeMap<>();
    private EImportScope m_importScope;
    // private Button m_isImportOnlyToSelectedTestSpecsCb;
    
    private boolean m_isExportDialog;
    private String m_textCaseId;

    private XLSExportLookAndFeel m_xlsExportLookAndFeel = 
            new XLSExportLookAndFeel((short)0, true, true, true, true, 3);
    private Text m_xlsAngleTxt;
    private Button m_xlsIsUseColors;
    private Button m_xlsIsFreezeHeaderRows;
    private Button m_xlsIsFreezeTestIdColumn;
    private Button m_xlsIsBottomBorder;
    private Text m_xlsBorderStep;
    private HSSFColorTableModel m_model;
    private Button[] m_importScopeBtns;
    private KTable m_xlsColorsTable;
    private Button m_xlsColorDownBtn;
    private Button m_xlsColorUpBtn;
    private Label m_xlsTextAngleLbl;
    
    private static final String ORDER_KEY = "order";
    
    public enum EExportFormats {XLSX("Excel - &XLSX", "xlsx"),
                                XLS("Excel - X&LS", "xls"), 
                                CTE("&Testona", "cte"), 
                                CSV("C&SV", "csv");
                                   
                                String m_uiText;
                                String m_extension;
                                
                                EExportFormats(String uiText, String extension) {
                                    m_uiText = uiText;
                                    m_extension = extension;
                                }
                                    
                                public String getUIText() {
                                    return m_uiText;
                                }

                                public String getFileNameExtension() {
                                    return m_extension;
                                }

                                public String[] getFileNameExtensions() {
                                    return new String[]{"*." + m_extension, "*.*"};
                                }

                              };

                              
    public ExportImportUIControls(boolean isExportDialog, String textCaseId) {
        m_isExportDialog = isExportDialog;
        m_textCaseId = textCaseId;
    }
    
    
    public boolean isContentValid() {
        return !m_fileNameTxts.get(m_selectedFormat).getText().trim().isEmpty();
    }
    
    
    public Composite createControl(Composite composite, 
                                   final WizardPage wizardPage) {

        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 750;
        mainDlgPanel.setLayoutData(gridData);

        
        mainDlgPanel.setLayout(new MigLayout("fill", "[fill]", "[min!][fill][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
        createFormatRadios(builder, wizardPage);

        if (m_isExportDialog) {
            KGUIBuilder exportSettingsGrp = builder.group("XLS && XLSX Look and Feel", 
                                                          "gaptop 20, grow, wrap", 
                                                          new MigLayout("fill", 
                                                                        "[min!][min!][min!][fill]", 
                                                                        "[min!][min!][min!][min!][min!][fill]"), 
                                                          SWT.NONE); 

            m_xlsTextAngleLbl = exportSettingsGrp.label("Text angle:", "gaptop 10, gapbottom 5");
            m_xlsAngleTxt = exportSettingsGrp.text("w 50:50:70, gaptop 10, gapbottom 5", 
                                                   SWT.BORDER);
            UiTools.setToolTip(m_xlsAngleTxt, "Angle for the first line in Excel file, in degrees." +
                    "\n0 means horizontal, 90 means vertical.");

            exportSettingsGrp.separator("spany 6, gapleft 30, growy", SWT.VERTICAL);
            createSectionConfigPanel(exportSettingsGrp);
            
            m_xlsIsFreezeHeaderRows = exportSettingsGrp.checkBox("Freeze header rows", "wrap");
            UiTools.setToolTip(m_xlsIsFreezeHeaderRows, "If selected, the first three rows in " +
                                                        "spreadsheed do not scroll up.");
            
            m_xlsIsFreezeTestIdColumn = exportSettingsGrp.checkBox("Freeze test ID column", "wrap");
            UiTools.setToolTip(m_xlsIsFreezeTestIdColumn, "If selected, the first column in " +
                                                        "spreadsheed does not scroll left.");
            
            m_xlsIsUseColors = exportSettingsGrp.checkBox("Use colors (see table)", "wrap");
            UiTools.setToolTip(m_xlsIsUseColors, "If selected, colors are used in exported " +
                                                 "spreadsheed to improve readability.");

            
            m_xlsIsBottomBorder = exportSettingsGrp.checkBox("Use row border. Step:", "gaptop 10, gapbottom 50");
            UiTools.setToolTip(m_xlsIsBottomBorder, "If selected, border is added to every N-th row. " +
                                                    "N is defined by step.");
            m_xlsBorderStep = exportSettingsGrp.text("w 50:50:70, gaptop 10, gapbottom 50", SWT.BORDER);
            UiTools.setToolTip(m_xlsBorderStep, "Number of rows between cells with border.\n" +
                                                "Numbers 2-5 give best results.");

            m_isOpenDefaultApplicationCb = builder.checkBox("&Open with default application", 
                    "span 2, gaptop 15, wrap");

            m_isOpenDefaultApplicationCb.setToolTipText("If selected, the saved file is " +
                    "opened with system default application for the selected type of file.");
        
        } else {
            KGUIBuilder importScopeGroup = builder.group("Import scope");
            // Composite group = new Composite(builder.getParent(), SWT.BORDER);
            m_importScopeBtns = 
                    importScopeGroup.radio(new String[]{"Create new test cases",
                                       "If test IDs match, import to existing test case, otherwise create new one",
                                       "Import only to test cases which test IDs match",
                                       "Import only to selected test cases"}, 
                                       importScopeGroup.getParent(), 
                                       "wrap");

            UiTools.setToolTip(m_importScopeBtns[0], "Each sheet in Excel file is imported as new test case, even if test case with the ID already exists.");
            UiTools.setToolTip(m_importScopeBtns[1], "If test case with ID found in Excel file already exists, its derived tests are deleted and new ones imported.\n"
                                                 + "Otherwise new test cases are created.");
            UiTools.setToolTip(m_importScopeBtns[2], "Only those test cases, which have test ID the same as existing test case in testIDEA are imported.");
            UiTools.setToolTip(m_importScopeBtns[3], "Only those test cases, which have test ID the same as one of selected test cases are imported.");

            m_importScopeBtns[0].setLayoutData("wrap");
            m_importScopeBtns[1].setLayoutData("wrap");
            m_importScopeBtns[2].setLayoutData("wrap");
            
            m_importScopeBtns[0].setData(EImportScope.ECreateNewTestCases);
            m_importScopeBtns[1].setData(EImportScope.EToExistingAndNew);
            m_importScopeBtns[2].setData(EImportScope.EToExistingTestCases);
            m_importScopeBtns[3].setData(EImportScope.EToSelectedTestCases);
            
            /*
            m_isImportOnlyToSelectedTestSpecsCb = builder.checkBox("&Import data only " +
                    "to selected test specifications", "span2, gaptop 15, wrap");

            m_isImportOnlyToSelectedTestSpecsCb.setToolTipText("If checked, only test " +
                    "specifications which you have selected in test tree will be imported.\n" +
                    "Otherwise all test specifications in external file will be imported.");
                    */
        }
        
        initControls();
        
        return mainDlgPanel;
    }

    
    private void createFormatRadios(KGUIBuilder builder, final WizardPage wizardPage) {
        
        SelectionListener listener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button rb = (Button)e.getSource();
                m_selectedFormat = (EExportFormats) rb.getData(EXPORT_FMT_KEY);
                FileNameBrowser fBrowser = m_fileNameTxts.get(m_selectedFormat);
                fBrowser.setEnabled(rb.getSelection());
                
                boolean isValid = isContentValid();
                wizardPage.setPageComplete(isValid);
                // getButton(IDialogConstants.OK_ID).setEnabled(isValid);
                
                enableImportOnlyToSelectedCb(rb.getSelection());
                
                enableExportLookAndFeelControls(m_selectedFormat == EExportFormats.XLS  ||  
                                                m_selectedFormat == EExportFormats.XLSX,
                                                m_selectedFormat == EExportFormats.CSV);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        };
        
        KGUIBuilder group = builder.group("File", "gapbottom 25, wrap", false, "fill", "[min!][fill][min!]", "");
        
        for (EExportFormats fmt : EExportFormats.values()) {
            Button radioBtn = group.radio(fmt.getUIText(), "");
            m_formatBtns.put(fmt, radioBtn);
            radioBtn.setData(EXPORT_FMT_KEY, fmt);
            radioBtn.addSelectionListener(listener);
            
            FileNameBrowser fileNameInput = 
                    group.createFileNameInput("Browse", "growx", "wrap", "Browse", 
                                              fmt.getFileNameExtensions(), true,
                                              m_isExportDialog ? SWT.SAVE : SWT.OPEN);
            m_fileNameTxts.put(fmt, fileNameInput);
            fileNameInput.setToolTipText("The name of the file to save exported test specifications to.");
            fileNameInput.getInputField().addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    Text fNameTxt = (Text)e.getSource();
                    boolean isValid = !fNameTxt.getText().trim().isEmpty();
                    
                    wizardPage.setPageComplete(isValid);
                    /* Button okButton = getButton(IDialogConstants.OK_ID);
                    if (okButton != null) { // is null at dialog creation, before it is created
                        okButton.setEnabled(isValid);
                    } */
                }
            });
            
            fileNameInput.getInputField().addFocusListener(UiUtils.
                     createExtensionFocusListener(fmt.getFileNameExtension()));
        }
    }
    
    
    private Control createSectionConfigPanel(KGUIBuilder parentBuilder) {
        
        Composite child = new Composite(parentBuilder.getParent(), SWT.BORDER);
        child.setLayout(new MigLayout("fill", "[pref!][min!][fill]", "[min!][min!][fill]"));
        child.setLayoutData("gapleft 40, spany 6, growy, wrap");
        KGUIBuilder builder = new KGUIBuilder(child);
        
        m_xlsColorsTable = new KTable(builder.getParent(), true, SWT.V_SCROLL  
                                      | SWTX.FILL_WITH_LASTCOL
                                      | SWTX.MARK_FOCUS_HEADERS 
                                      | SWTX.EDIT_ON_KEY);
        
        UiTools.setToolTip(m_xlsColorsTable, "Select test sections to be exported and background colors for "
                + "Excel table.\n"
                + "Note: If section is not empty in at least one of exported test cases,\n"
                + "then it will be shown regardless of the visibility flag for this section.");
        m_model = new HSSFColorTableModel();
        
        CTestSpecification testSpec = new CTestSpecification();
        IntVector sections = new IntVector();
        testSpec.getSectionIds(sections);
        for (int i = 0; i < sections.size(); i++) {
            int section = sections.get(i); 
            for (int j = 0; j < TableExporter.REMOVED_TEST_SPEC_SECTIONS.length; j++) {
                if (section == TableExporter.REMOVED_TEST_SPEC_SECTIONS[j]) {
                    sections.remove(i);
                    i--;
                    break;
                }
            }
        }
        
        Preferences prefs = PlatformUtils.getUserPrefs();
        
        Preferences node = prefs.node(EXPORT_TABLE_CONFIG_NODE);
        Map<Integer, HSSFColor> hssfColorMap = HSSFColor.getIndexHash();

        for (int idx = 0; idx < sections.size(); idx++) {
            String sectionName = testSpec.getTagName(sections.get(idx));
            boolean isVisible = node.getBoolean(sectionName + SUFFIX_IS_VISIBLE, 
                                                false); // make section invisible by default,
            // so that new sections do not clutter exports unless explicitly made visible.
            HSSFColor color = hssfColorMap.get(node.getInt(sectionName + SUFFIX_COLOR, 
                                                           HSSFColor.WHITE.index));
            
            m_model.addRow(sectionName, isVisible, color);
        }        

        m_model.initColumnWidths(m_xlsColorsTable);
        String orderList = node.get(ORDER_KEY, "");
        if (orderList.length() < 1) {
            // default order
            orderList = "id,testScope,run,desc,tags,preCondition,locals,init,func,assert,"
                    + "stackUsage,stubs,userStubs,testPoints,analyzer,"
                    + "beginStopCondition,endStopCondition,options,hil,"
                    + "initTargetFunc,initFunc,endFunc,restoreTargetFunc,"
                    + "log,dryRun,diagrams";
        }
        m_model.sort(orderList);
        final int colorsColumnWidth = m_model.getColorsColumnWidth();
        m_xlsColorsTable.setModel(m_model);
        m_xlsColorsTable.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseUp(MouseEvent e) {

                Point[] selection = m_xlsColorsTable.getCellSelection();
                
                if (selection.length == 1) {
                    
                    int col = selection[0].x;
                    int row = selection[0].y;

                    if (row > 0) {
                        if (col == HSSFColorTableModel.COL_IS_VISIBLE) {
                            Boolean value = (Boolean)m_model.getContentAt(col, row);
                            m_model.setContentAt(col, row, !value.booleanValue());
                            m_xlsColorsTable.redraw();
                        } else if (col == HSSFColorTableModel.COL_COLOR) {
                            HSSFColor color = HSSFColorDialog.show(colorsColumnWidth);
                            if (color != null) {
                                m_model.setContentAt(col, row, color);
                            }
                            m_xlsColorsTable.redraw();
                        }
                    }
                }
            }
            
            
            @Override
            public void mouseDown(MouseEvent e) {}
            
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {}
        });
        
        m_xlsColorsTable.setLayoutData("wmin 0, w " + (m_model.getWidth() + 30) + ", spany 3, growy");
        
        m_xlsColorUpBtn = builder.button("Up ^", "gaptop 40, gapleft 10, wrap");
        m_xlsColorUpBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_xlsColorsTable.getCellSelection();
                if (selection.length > 0) {
                    int row = selection[0].y;
                    if (selection.length == 1  &&  row > 1) {
                        m_model.swap(row - 2); // -1 for header row, -1 for swap up
                        m_xlsColorsTable.setSelection(new Point[]{new Point(1, row - 1)}, true);
                        // table.redraw();
                    }
                }
            }
        });
        
        m_xlsColorDownBtn = builder.button("Down v", "gapbottom 20, gapleft 10");
        m_xlsColorDownBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                Point[] selection = m_xlsColorsTable.getCellSelection();
                if (selection.length > 0) {
                    int row = selection[0].y;
                    if (selection.length == 1  &&  row > 0) {
                        m_model.swap(row - 1);  // -1 for header row
                        m_xlsColorsTable.setSelection(new Point[]{new Point(1, row + 1)}, true);
                        // table.redraw();
                    }
                }
            }
        });
            
        return builder.getParent();
    }

    
    private void initControls() {
        IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        String selectedFormat = prefs.get(SELECTED_EXPORT_FORMAT,
                                          // default is CSV 
                                          EExportFormats.CSV.toString());
        try {
            m_selectedFormat = EExportFormats.valueOf(selectedFormat);
        } catch (IllegalArgumentException ex) {
            m_selectedFormat = EExportFormats.XLSX;
            selectedFormat = m_selectedFormat.toString();
        }

        for (EExportFormats fmt : EExportFormats.values()) {
            Text fileTxt = m_fileNameTxts.get(fmt).getInputField();
            String fName = prefs.get(fmt.toString(), "");
            if (fName != null) {
                
                if (fmt == EExportFormats.CTE  &&  m_textCaseId != null) {
                    // for CTE exports add test case ID to file name, as CTE XL 
                    // supports only one test case per file, and users don't like
                    // overriding previous files. See mail from Mlinar, 2014-11-18, 14:53.
                    // ~ is used as a separator between testID in file name and 
                    // the rest of the file name.
                    int testCaseIdSeparatorIdx = fName.indexOf('~');
                    if (testCaseIdSeparatorIdx >= 0) {
                        fName = m_textCaseId + fName.substring(testCaseIdSeparatorIdx);
                    } else {
                        fName = m_textCaseId + '~' + fName;
                    }
                }
                
                fileTxt.setText(fName);
            }
            
            fileTxt.setEnabled(fmt.toString().equals(selectedFormat));
        }

        if (m_isOpenDefaultApplicationCb != null) {
            m_isOpenDefaultApplication = prefs.getBoolean(IS_OPEN_DEFAULT_APP, false);
            m_isOpenDefaultApplicationCb.setSelection(m_isOpenDefaultApplication);
        }
        
        if (m_importScopeBtns != null) {
            try {
                m_importScope = EImportScope.valueOf(prefs.get(IMPORT_SCOPE, EImportScope.EToExistingAndNew.name()));
            } catch (Exception ex) {
                m_importScope = EImportScope.EToExistingAndNew;
            }
            for (int i = 0; i < m_importScopeBtns.length; i++) {
                m_importScopeBtns[i].setSelection(m_importScope.ordinal() == i);
            }
        }
        
        m_formatBtns.get(m_selectedFormat).setSelection(true);
        enableImportOnlyToSelectedCb(true);
        
        if (m_isExportDialog) {
            
            m_xlsExportLookAndFeel.getFromPrefs(prefs);
            
            m_xlsAngleTxt.setText(m_xlsExportLookAndFeel.getIdentifiersTextAngleStr());
            m_xlsIsUseColors.setSelection(m_xlsExportLookAndFeel.isUseColors());
            m_xlsIsFreezeHeaderRows.setSelection(m_xlsExportLookAndFeel.isFreezeHeaderRows());
            m_xlsIsFreezeTestIdColumn.setSelection(m_xlsExportLookAndFeel.isFreezeTestIdColumn());
            m_xlsIsBottomBorder.setSelection(m_xlsExportLookAndFeel.isUseBottomCellBorder());
            m_xlsBorderStep.setText(String.valueOf(m_xlsExportLookAndFeel.getCellBorderRowStep()));
            
            enableExportLookAndFeelControls(m_selectedFormat == EExportFormats.XLS  ||  
                                            m_selectedFormat == EExportFormats.XLSX,
                                            m_selectedFormat == EExportFormats.CSV);
        }
    }


    private void enableImportOnlyToSelectedCb(boolean isRbSelected) {
        // ignore deselect events (first Rb listener of deselected Rb is called,
        // then listener of the selected Rb is called.
        if (m_importScopeBtns != null  &&  isRbSelected) {
            if (m_selectedFormat == EExportFormats.CTE) {
                buttonsToImportScope();
                for (int i = 0; i < m_importScopeBtns.length; i++) {
                    m_importScopeBtns[i].setSelection(EImportScope.EToSelectedTestCases.ordinal() == i);
                    m_importScopeBtns[i].setEnabled(false);
                }
            } else {
                for (int i = 0; i < m_importScopeBtns.length; i++) {
                    m_importScopeBtns[i].setSelection(m_importScope.ordinal() == i);
                    m_importScopeBtns[i].setEnabled(true);
                }
            }
        }
    }
    
    
    private void enableExportLookAndFeelControls(boolean isEnabledForXls, boolean isEnabledForCSV) {
        
        if (m_isExportDialog) {
            m_xlsTextAngleLbl.setEnabled(isEnabledForXls);
            m_xlsAngleTxt.setEnabled(isEnabledForXls);
            m_xlsIsUseColors.setEnabled(isEnabledForXls);
            m_xlsIsFreezeHeaderRows.setEnabled(isEnabledForXls);
            m_xlsIsFreezeTestIdColumn.setEnabled(isEnabledForXls);
            m_xlsIsBottomBorder.setEnabled(isEnabledForXls);
            m_xlsBorderStep.setEnabled(isEnabledForXls);
            
            m_xlsColorsTable.setEnabled(isEnabledForXls ||  isEnabledForCSV);
            m_xlsColorUpBtn.setEnabled(isEnabledForXls ||  isEnabledForCSV);
            m_xlsColorDownBtn.setEnabled(isEnabledForXls ||  isEnabledForCSV);
        }
    }
    
    
    private void buttonsToImportScope() {
        m_importScope = EImportScope.EToExistingAndNew;
        for (int i = 0; i < m_importScopeBtns.length; i++) {
            if (m_importScopeBtns[i].getSelection()) {
                m_importScope = (EImportScope) m_importScopeBtns[i].getData();
                break;
            }
        }
    }
    

    public void okPressed() {
        
        IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        
        prefs.put(SELECTED_EXPORT_FORMAT, m_selectedFormat.toString());

        if (m_isOpenDefaultApplicationCb != null) {
            m_isOpenDefaultApplication = m_isOpenDefaultApplicationCb.getSelection();
            prefs.putBoolean(IS_OPEN_DEFAULT_APP, m_isOpenDefaultApplication);
        }

        if (m_importScopeBtns != null) {
            buttonsToImportScope();
            prefs.put(IMPORT_SCOPE, m_importScope.name());
        }
        
        for (EExportFormats fmt : EExportFormats.values()) {
            Text fileTxt = m_fileNameTxts.get(fmt).getInputField();
            prefs.put(fmt.toString(), fileTxt.getText());
        }
        
        m_fileName = m_fileNameTxts.get(m_selectedFormat).getText();
        
        if (m_isExportDialog) {
            try {
                m_xlsExportLookAndFeel.setIdentifiersTextAngleStr(m_xlsAngleTxt.getText());
            } catch (Exception ex) {
                MessageDialog.openError(Activator.getShell(),
                                        "Can not parse number: " + m_xlsAngleTxt.getText(), 
                        "Angle should be a number in range 0-360!");
                throw ex;
            }
            
            m_xlsExportLookAndFeel.setUseColors(m_xlsIsUseColors.getSelection());
            m_xlsExportLookAndFeel.setFreezeHeaderRows(m_xlsIsFreezeHeaderRows.getSelection());
            m_xlsExportLookAndFeel.setFreezeTestIdColumn(m_xlsIsFreezeTestIdColumn.getSelection());
            m_xlsExportLookAndFeel.setUseBottomCellBorder(m_xlsIsBottomBorder.getSelection());
            try {
                if (m_xlsIsBottomBorder.getSelection()) {
                    m_xlsExportLookAndFeel.setCellBorderRowStep(m_xlsBorderStep.getText());
                }
            } catch (Exception ex) {
                MessageDialog.openError(Activator.getShell(),
                                        "Can not parse number: " + m_xlsBorderStep.getText(), 
                        "Row border step should be an integer number!");
                throw ex;
            }      

            saveVisibilityAndcolors();
            m_xlsExportLookAndFeel.setVisibilityAndColors(m_model);

            m_xlsExportLookAndFeel.storeToPrefs(prefs);
        }
        
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            SExceptionDialog.open(Activator.getShell(),
                                  "Can not save preferences!", ex);
        }
    }        
    
    private void saveVisibilityAndcolors() {

        Preferences prefs = PlatformUtils.getUserPrefs();
        
        Preferences node = prefs.node(EXPORT_TABLE_CONFIG_NODE);

        int numRows = m_model.getRowCount() - 1;  // -1 for header row
        StringBuilder order = new StringBuilder();
        
        for (int row = 0; row < numRows; row++) {
            String sectionName = m_model.getSectionName(row);
            Boolean isVisible = m_model.isVisible(row);
            HSSFColor color = m_model.getColor(row);
            
            order.append(sectionName).append(",");
            node.putBoolean(sectionName + SUFFIX_IS_VISIBLE, isVisible.booleanValue());
            node.putInt(sectionName + SUFFIX_COLOR, color.getIndex());
        }

        node.put(ORDER_KEY, order.toString());

        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            SExceptionDialog.open(Activator.getShell(),
                                  "Can not save preferences!", ex);
        }
    }

    
    public boolean isExportDialog() {
        return m_isExportDialog;
    }


/*    public EExportFormats getSelectedFormat() {
        return m_selectedFormat;
    } */
    
    // called also from ImportDialog
    public String getFileName() {
        return m_fileName;
    }

    
    public boolean isOpenDefault() {
        return m_isOpenDefaultApplication;
    }

    
    public EImportScope getImportScope() {
        return m_importScope;
    }

    
    public XLSExportLookAndFeel getXlsLookAndFeel() {
        return m_xlsExportLookAndFeel;
    }
    
    //
    // combo box is deprecated - no longer used
    //
    
/*    public EExportFormats getCurrentFormat(Combo formatCb) {
        String id = formatCb.getItem(formatCb.getSelectionIndex());
        return (EExportFormats)formatCb.getData(id);
    }
  */  
    
/*    public Combo createFormatCombo(KGUIBuilder builder) {
        
        // items must be added in the same order as they are defined in enum, because ordinals
        // are used in ImportDialog as selection indices
        final Combo formatCb = builder.combo(new String[]{EExportFormats.XLSX.getUIText(), 
                                       EExportFormats.XLS.getUIText(), 
                                       EExportFormats.CSV.getUIText()}, 
                                       "wmax 90, wrap", SWT.BORDER | SWT.READ_ONLY);
        
        formatCb.setData(EExportFormats.XLSX.getUIText(), EExportFormats.XLSX);
        formatCb.setData(EExportFormats.XLS.getUIText(), EExportFormats.XLS);
        formatCb.setData(EExportFormats.CSV.getUIText(), EExportFormats.CSV);

        formatCb.setToolTipText("Selection in this combo box defines file format.");
        formatCb.select(0);
        
        return formatCb;
    }
  */  
    
/*    public void addSelectionListener(final Combo formatCb, final FileNameBrowser fileNameTxt) {
    
        formatCb.addSelectionListener(new SelectionListener() {
        
            @Override
            public void widgetSelected(SelectionEvent e) {
                String extension = getCurrentFormat(formatCb).getFileNameExtension();
                String fileName = fileNameTxt.getText().trim();
                fileName = UiUtils.replaceExtension(fileName, extension);
                fileNameTxt.setText(fileName);
            }


            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }
  */  
}


class HSSFColorDialog extends Dialog {

    private HSSFColor m_color;
    private int m_colorsColumnWidth;


    protected HSSFColorDialog(Shell parentShell, int colorsColumnWidth) {
        super(parentShell);
        
        m_colorsColumnWidth = colorsColumnWidth;
    }
    
 
    public static HSSFColor show(int colorsColumnWidth) {
        HSSFColorDialog dlg = new HSSFColorDialog(Activator.getShell(), colorsColumnWidth);
        if (dlg.open() != Window.CANCEL) {
            return dlg.getColor();
        }
        return null;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Select section color");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        
        mainDlgPanel.setLayout(new MigLayout("wrap 6"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        builder.label("Click color to select:", "gapbottom 10, wrap");
        FontProvider fontProvider = FontProvider.instance();
        int fontHeight = fontProvider.getDefaultFontHeight(Activator.getShell()) * 2;
        
        Map<Integer, HSSFColor> hssfColorMap = HSSFColor.getIndexHash();
        TreeMap<Integer, HSSFColor> hssfColorSortedMap = new TreeMap<>();
        float[] hsbvals = new float[3];

        for (Map.Entry<Integer, HSSFColor> entry : hssfColorMap.entrySet()) {
            HSSFColor color = entry.getValue();
            short[] triplet = color.getTriplet();
            java.awt.Color.RGBtoHSB(triplet[0] & 0xff, triplet[1] & 0xff, triplet[2] & 0xff, hsbvals);
            /* String colorHex = String.format("  %02x : %02x : %02x", 
                                            triplet[0] & 0xff, triplet[1] & 0xff, triplet[2] & 0xff);
            System.out.printf("%s: %f,  %f, %f\n", colorHex, hsbvals[0], hsbvals[1], hsbvals[2]); */
            hssfColorSortedMap.put(Math.round(hsbvals[0] * 1_000_000_000 + hsbvals[1] * 1_000_000 + hsbvals[2] * 1000), 
                                   color);
        }

        for (Map.Entry<Integer, HSSFColor> entry : hssfColorSortedMap.entrySet()) {
            
            HSSFColor color = entry.getValue();
            
            Label btn = builder.label(color.getClass().getSimpleName(), 
                                      "w " + m_colorsColumnWidth + ", h " + fontHeight, SWT.BORDER);
            
            btn.setData(color);
            short[] triplet = color.getTriplet();
            btn.setBackground(ColorProvider.instance().getColor(triplet[0], triplet[1], triplet[2]));
            
            btn.addMouseListener(new MouseListener() {
                
                @Override
                public void mouseUp(MouseEvent e) {
                    m_color = (HSSFColor)((Label)e.getSource()).getData();
                    okPressed();
                }
                
                
                @Override
                public void mouseDown(MouseEvent e) {}
                
                @Override
                public void mouseDoubleClick(MouseEvent e) {}
            });
        }
        
        
        builder.separator("newline, span 6, growx, gaptop 20", SWT.HORIZONTAL);

        return composite;
    }


     @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // only Cancel button is present, user should click color to select
        createButton(parent, 
                     IDialogConstants.CANCEL_ID,
                     IDialogConstants.CANCEL_LABEL, 
                     false);
    }
    

    @Override
    protected void okPressed() {
 
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    
    
    public HSSFColor getColor() {
        return m_color;
    }
    
    
    
}