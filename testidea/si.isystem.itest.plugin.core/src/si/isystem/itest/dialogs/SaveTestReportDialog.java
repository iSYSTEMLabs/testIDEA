package si.isystem.itest.dialogs;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.Preferences;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestReportConfig.EOutputFormat;
import si.isystem.connect.CTestReportConfig.ETestReportConfigSectionIds;
import si.isystem.connect.StrVector;
import si.isystem.connect.connect;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.comp.TBControlCheckBox;
import si.isystem.itest.ui.comp.TBControlCombo;
import si.isystem.itest.ui.comp.TBControlFileName;
import si.isystem.itest.ui.comp.TBControlRadio;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.MappingTableEditor;
import si.isystem.itest.ui.spec.data.ActionExecutioner;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.mk.utils.PlatformUtils;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class SaveTestReportDialog extends Dialog {

    private static final String FOLDER_TEMPLATES_FULL = "templates/reports";
    private static final String FOLDER_TEMPLATES_CSS = "templates/reports";
    
    private static final String PREF_NODE_REPORT_CONFIG_DLG = "reportConfigurationDialog";
    private static final String PREF_KEY_MRU_TEMPLATE_FULL = "lastUsedTemplateFull";
    private static final String PREF_KEY_MRU_TEMPLATE_CSS = "lastUsedTemplateCSS";

    public static final String XSLT_PATTERN = "*.xslt";
    public static final String CSS_PATTERN = "*.css";
    
    public static final String ASYST_URL = "http://www.asystelectronic.si/itest";
    public static final String PREF_KEY_OPEN_DEF_BROWSER = "isOpenDefaultBrowserAfterReportSave";
    public static final String TAG_ENV_TESTER = "tester";
    public static final String TAG_ENV_DATE = "date";
    public static final String TAG_ENV_TIME = "time";
    public static final String TAG_ENV_HW = "hardware";
    public static final String TAG_ENV_SW = "software";
    public static final String TAG_ENV_DESC = "description";
    private static final String EXT_XML = "xml";
    private static final String EXT_YAML = "yaml";
    private static final String EXT_CSV = "csv";
    private static final String EXT_XLS = "xls";
    private static final String EXT_XLSX = "xlsx";
    

    // opening browser after save is testIDEA specific (NA from Python), so do 
    // not save it into Test Report config. The state is stored to preferences.
    private Button m_isOpenBrowserCb;
    private boolean m_isOpenBrowser = false;

    private CTestReportConfig m_testReportConfig;
    private String[] m_fullReportXSLTs;
    private String[] m_reportCSSs;
    private Composite m_csvComposite;
    private Composite m_xmlComposite;
    private Composite m_yamlComposite;
    private Composite m_xlsComposite;
    private StackLayout m_exportConfigLayout;
    private Composite m_exportConfigGroup;
    private String m_title;

    private ActionExecutioner m_actionExecutioner = new ActionExecutioner();
    private TBControlFileName m_fileName;
    private TBControlCombo m_xsltURLCombo;
    private TBControlCombo m_cssURLCombo;
    private TBControlFileName m_logoFileTb;
    private TBControlText m_csvSeparatorTxtTb;
    private TBControlCheckBox m_isCSVHeaderCbTb;
    private TBControlCheckBox m_isVerticalColumnHeadersXlsCbTb;
    private TBControlCheckBox m_isAbsPathToLinkTb;
    private TBControlCheckBox m_isIncludeTestSpecTb;
    private TBControlRadio m_outFormatRadiosTb;
    private TBControlText m_headerTextTb;
    private TBControlCheckBox m_embedXsltCss;
    private TBControlCombo m_isShowErrosOnlyInHTML;
    private TBControlCheckBox m_isCreateHtml;

    class ExportFormatRadioListener implements SelectionListener {
        
        Composite m_topComposite;
        String m_extension;
        
        public ExportFormatRadioListener(Composite topComposite,
                                         String extension) {
            super();
            m_topComposite = topComposite;
            m_extension = extension;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            if ( ((Button)e.getSource()).getSelection()) {
                fillCfgGroupAndOutFName(m_topComposite, m_extension);
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {}
    }   
    
    
    /**
     * Created dialog.
     * 
     * @param parentShell parent shell
     * @param title text displayed in dialog title bar
     */
    public SaveTestReportDialog(Shell parentShell, 
                                String title,
                                CTestReportConfig testReportConfig) {
        super(parentShell);
        m_title = title;
        
        // make a dialog resizable, see also layout setting in createDialogArea() 
        setShellStyle(getShellStyle() | SWT.RESIZE);

        m_testReportConfig = new CTestReportConfig(null); 
        m_testReportConfig.assign(testReportConfig);
        // set default separator. It is very unlikely to have CSV file without separator
        if (m_testReportConfig.getCSVSeparator().isEmpty()) {
            m_testReportConfig.setCsvSeparator(",");
        }
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText(m_title);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.widthHint = 650;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fill", "[min!][fill][min!]",
                                             "[min!][min!][min!][min!][min!][min!][fill][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        // createReportContentsControls(builder);
        
        createOutputFormatRadios(builder);

        createOutputFormatConfigControls(builder);

        createOutputFileControl(builder);
        
        createConfigCheckBoxes(builder);
        
        builder.separator("span 3, grow, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);
        
        createTestEnvControls(builder);

        builder.separator("span 3, growx", SWT.HORIZONTAL);

        try {
            fillControls();
        } catch (Exception ex) {
            SExceptionDialog.open(getShell(), "Setting of values failed!", ex);
        }
        
        return composite;
    }

    
    protected void createOutputFormatRadios(KGUIBuilder builder) {
        builder.label("Output format:", "gapbottom 10");
        KGUIBuilder fmtBuilder = builder.group("", "span 2, gapbottom 15, wrap");
        
        StrVector enumValues = new StrVector(); 
        int sectionId = ETestReportConfigSectionIds.E_SECTION_OUTPUT_FORMAT.swigValue();
        m_testReportConfig.getEnumValues(sectionId, enumValues);
        m_outFormatRadiosTb = 
                new TBControlRadio(fmtBuilder, 
                              new String []{"XML", "YAML", "CSV", "XLS", "XLSX"},
                              new String []{"Save test report as XML.",
                                            "Save test report in human readable and machine parseable text format.",
                                            "Save test report in Comma Separated Values format.",
                                            "Save test report in the old Excel format (this format is limited to 65536 lines per sheet).",
                                            "Save test report in the new Excel format (this is recommended option for Excel export)."},
                              DataUtils.strVectorToList(enumValues).toArray(new String[0]),
                              "gapright 10", // button migLayoutData
                              sectionId, 
                              null,
                              null);
    }


    protected void createOutputFormatConfigControls(KGUIBuilder builder) {
        KGUIBuilder exportConfigBuilder = builder.group("Output format configuration", 
                                                        "span 3, growx, gapbottom 15, wrap");
        m_exportConfigLayout = new StackLayout();
        m_exportConfigGroup = exportConfigBuilder.getParent(); 
        m_exportConfigGroup.setLayout(m_exportConfigLayout);
        
        m_xmlComposite = exportConfigBuilder.composite(SWT.NONE, null);
        m_yamlComposite = exportConfigBuilder.composite(SWT.NONE, null);
        m_csvComposite = exportConfigBuilder.composite(SWT.NONE, null);
        m_xlsComposite = exportConfigBuilder.composite(SWT.NONE, null);

        m_xmlComposite.setLayout(new MigLayout("fill", "[min!][fill][min!]"));
        m_yamlComposite.setLayout(new MigLayout());
        m_csvComposite.setLayout(new MigLayout());
        m_xlsComposite.setLayout(new MigLayout());

        KGUIBuilder csvBuilder = new KGUIBuilder(m_csvComposite);
        csvBuilder.label("CSV Separator:");
        m_csvSeparatorTxtTb = TBControlText.createForMixed(csvBuilder, 
                                                           "The string used as a separator between fields. Usually it is comma, but Excel prefers semicoln.", 
                                                           "gapright 20, wmin 50", 
                                                           ETestReportConfigSectionIds.E_SECTION_CSV_SEPARATOR.swigValue(), 
                                                           null, 
                                                           null, 
                                                           SWT.BORDER);
        m_isCSVHeaderCbTb = new TBControlCheckBox(csvBuilder, 
                                                  "CSV header", 
                                                  "If checked, the first line in CSV export contains names of test report sections.", 
                                                  "", 
                                                  ETestReportConfigSectionIds.E_SECTION_CSV_IS_HEADER_LINE.swigValue(), 
                                                  null, 
                                                  null); 

        KGUIBuilder xlsBuilder = new KGUIBuilder(m_xlsComposite);
        
        m_isVerticalColumnHeadersXlsCbTb = new TBControlCheckBox(xlsBuilder, 
                                                  "Vertical column headers", 
                                                  "If checked, the text in the first row of Excel table is written vertically.", 
                                                  "", 
                                                  ETestReportConfigSectionIds.E_SECTION_XLS_IS_VERTICAL_HEADER.swigValue(), 
                                                  null, 
                                                  null); 
        
        m_exportConfigLayout.topControl = m_xmlComposite;
        
        createXMLConfigControls();
        
        m_outFormatRadiosTb.addSelectionListener(new ExportFormatRadioListener(m_xmlComposite, EXT_XML), 
                                                 0);
        m_outFormatRadiosTb.addSelectionListener(new ExportFormatRadioListener(m_yamlComposite, EXT_YAML),
                                                 1);
        m_outFormatRadiosTb.addSelectionListener(new ExportFormatRadioListener(m_csvComposite, EXT_CSV),
                                                 2);
        m_outFormatRadiosTb.addSelectionListener(new ExportFormatRadioListener(m_xlsComposite, EXT_XLS),
                                                 3);
        m_outFormatRadiosTb.addSelectionListener(new ExportFormatRadioListener(m_xlsComposite, EXT_XLSX),
                                                 4);
    }


    protected void createXMLConfigControls() {
        KGUIBuilder xmlBuilder = new KGUIBuilder(m_xmlComposite);
        xmlBuilder.label("XSLT:");

        m_xsltURLCombo = TBControlCombo.createForText(xmlBuilder, 
                                                      new String[]{"itestResult.xslt"}, 
                                                      "Stylesheet template for XML output.\n" +
                                                      "Stylesheet describes how to render XML file in web browser.", 
                                                      "w 300:300:max, growx", 
                                                      ETestReportConfigSectionIds.E_SECTION_XML_XSLT_FOR_FULL_REPORT.swigValue(),                                        
                                                      null, null, SWT.NONE);

        xmlBuilder.createBrowseButton("Browse", "gapleft 10, wrap", "Report XML Stylesheet file", 
                                      new String[]{"*.xslt", "*.*"}, 
                                      (Combo)m_xsltURLCombo.getControl());

        xmlBuilder.label("CSS:");
        m_cssURLCombo = TBControlCombo.createForText(xmlBuilder, 
                                                     new String[]{"blue"}, 
                                                     "CSS file used when rendering XML as HTML. Mostly specifies colors.", 
                                                     "w 300:300:max, growx", 
                                                     ETestReportConfigSectionIds.E_SECTION_CSS_FILE.swigValue(),
                                                     null, null, SWT.NONE);
        
        xmlBuilder.createBrowseButton("Browse", "gapleft 10, wrap", "Report XML Stylesheet file", 
                                      new String[]{"*.css", "*.*"}, 
                                      (Combo)m_cssURLCombo.getControl());
        
        xmlBuilder.label("Logo image URL:");
        m_logoFileTb = new TBControlFileName(xmlBuilder, 
                                             "Browse", 
                                             "If this filed contains name of file with image, the image will\n"
                                                     + "be shown in top left corner in the report.\n"
                                                     + "May be empty.\n"
                                                     + "Example:\n   file:///D:\\images\\logo.jpg", 
                                             ETestReportConfigSectionIds.E_SECTION_XML_LOGO_IMAGE.swigValue(), 
                                             "w 300:300:max", "gapleft 10, wrap", null, 
                                             "Logo image file", 
                                             new String[]{"*.jpg", "*.png", "*.*"}, 
                                             true, 
                                             null, 
                                             SWT.OPEN); 
        
        xmlBuilder.label("Report title:");
        int lineHeight = FontProvider.instance().getDefaultFontHeight(getShell()) * 15/10;
        
        m_headerTextTb = TBControlText.createForMixed(xmlBuilder, 
                                                      "Enter text to be shown as report title, for example: 'Test Report'.\n"
                                                             // uncomment if saxon will be used, see comment below
                                                              + "HTML tags can be used, for example:\n"
                                                              + "<h1 align='center'>Test Report</h1>\n"
                                                              + "<h2 align='center'>Unit Tests</h2>\n"
                                                              + "<b>Department:<b> <i>Embedded</i>\n\n"
                                                              + "If 'Create HTML' below is not selected, Firefox will not show html tags properly.",
                                                      "hmin " + lineHeight * 3, 
                                                      ETestReportConfigSectionIds.E_SECTION_XML_REPORT_HEADER.swigValue(), 
                                                      null, 
                                                      null, 
                                                      SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        /* Unfortunately Firefox does not support 'disable-output-escaping="yes"' in XSLT, so
        HTML tags can not be used. Reenable this code if export to HTML via saxon will be
        implemented. */
        
        Button wizardBtn = xmlBuilder.button(IconProvider.INSTANCE.getIcon(EIconId.EDefaultText), 
                                             "gapleft 10, wrap");
        UiTools.setToolTip(wizardBtn, "Adds default report header HTML template to text field on the left.\n"
                + "If 'Create HTML' below is not selected, Firefox will not show html tags properly.");
        
        wizardBtn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                ((Text)m_headerTextTb.getControl()).setText("<h1 align='center'>Test Report</h1>\n"
                                                          + "<h2 align='center'>Unit tests</h2>\n"
                                                          + "<p align='center'>Draft</p>");
                m_headerTextTb.getControl().setFocus();
            }
        });
        
        m_isCreateHtml = new TBControlCheckBox(xmlBuilder, 
                                               "Create HTML", 
                                               "If selected, XML report is automatically processed by testIDEA and\n"
                                               + "report file in HTML format is created.\n"
                                               + "File name is the same as specified in 'Output file:' field below, "
                                               + "but extension is '.html'\n\n"
                                               + "Recommended for compatibility with all browsers.", 
                                               "skip, w min:min:min, gaptop 5, split 4", 
                                               ETestReportConfigSectionIds.E_SECTION_IS_CREATE_HTML.swigValue(), 
                                               null, null);
        m_isCreateHtml.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isSelected = ((Button)e.getSource()).getSelection();
                if (isSelected) {
                    // Saxon does not handle embedded CSS properly, so embedding
                    // is not supported if HTML should be generated.
                    m_embedXsltCss.setSelection(false);
                }
                m_embedXsltCss.setEnabled(!isSelected);
            }
        });
                                           
        m_embedXsltCss = new TBControlCheckBox(xmlBuilder, 
            "Embed XSLT/CSS",  
            "If checked, XSLT and CSS are embedded into report XML. Larger XML file is created, "
            + "but everything is stored into one file.\n\n"
            + "If NOT checked, three files are created:\n"
            + "- XML with report data\n"
            + "- XSLT file with information for transformation to HTML\n"
            + "- CSS file with colors and HTML styles\n"
            + "Use unchecked, if the same XSLT and CSS are used by multiple reports.\n\n"
            + "Browser compatibility: Internet Explorer does not show embedded version properly, while Chrome has problems \n"
            + "with standalone files. Firefox properly shows report in both cases, but can not handle HTML tags in 'Report title:'"
            + "field above.", 
            "w min:min:min, gapleft 15, gaptop 5", 
            ETestReportConfigSectionIds.E_SECTION_IS_EMBED_XML_XSLT_CSS.swigValue(), 
            null, null);

        xmlBuilder.label("HTML content:", "gapleft 15, gaptop 5");
        
        m_isShowErrosOnlyInHTML = TBControlCombo.createForEnum(xmlBuilder,
                                                               new String[]{"All results", "Not passed results only"},
                                                               new CTestReportConfig(),
                                                               "Select which results for tests cases should be shown in HTML.\n" +
                                                               "XML file always contains results for all executed test cases.",
                                                               "gaptop 5",
                                                               ETestReportConfigSectionIds.E_SECTION_HTML_VIEW_MODE.swigValue(),
                                                               null, null, SWT.NONE);
    }


    protected void createOutputFileControl(KGUIBuilder builder) {
        builder.label("Output file:");
        KGUIBuilder fNamePanel = builder.newPanel("fill, insets 0 0 0 n", "span 2, wrap", SWT.NONE);
        m_fileName = new TBControlFileName(fNamePanel, 
                                           "Browse", 
                                           "The name of the file to save report to.\nType '.' as "
                                           + "the last char if you don't want extension to be automatically added.\n"
                                           + "Browser will only open registered file names.",
                                           ETestReportConfigSectionIds.E_SECTION_FILE_NAME.swigValue(), 
                                           "growx, push, w 300:300:max",
                                           "gapleft 10, w 0:min:min", 
                                           null, 
                                           "Report file name",  
                                           new String[]{"*.xml", "*.yaml", "*.csv", "*.xls", "*.xlsx", "*.*"},
                                           true,
                                           null, 
                                           SWT.SAVE);
        
        Control fNameTxt = m_fileName.getTextField().getControl();
        HostVarsUtils.setContentProposals(fNameTxt, 
                                          getTestReportHostVarsProposals());
        
    }


    protected void createConfigCheckBoxes(KGUIBuilder builder) {
        
        m_isAbsPathToLinkTb = new TBControlCheckBox(builder, 
                                                    "Use absolute links to export files", 
                                                    "If checked, links in test report are written as absolute paths.\n"
                                                            + "Use this option, when test report is not saved in the same folder as "
                                                            + "analyzer export files.", 
                                                    "span 2, gaptop 5", 
                                                    ETestReportConfigSectionIds.E_SECTION_IS_ABS_PATHS_FOR_LINK.swigValue(), 
                                                    null, 
                                                    null); 

        m_isIncludeTestSpecTb = new TBControlCheckBox(builder, 
                                                      "Include test specifications", 
                                                      "If checked, test specifications are included to test report.", 
                                                      "span 2, gaptop 5, wrap", 
                                                      ETestReportConfigSectionIds.E_SECTION_IS_INCLUDE_TEST_SPEC.swigValue(), 
                                                      null, 
                                                      null); 
        
        m_isOpenBrowserCb = builder.checkBox("Open default browser after save", "span 2, gaptop 5, wrap");

        Preferences preferences = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        Preferences sub1 = preferences.node(PREF_NODE_REPORT_CONFIG_DLG);
        m_isOpenBrowserCb.setSelection(sub1.getBoolean(PREF_KEY_OPEN_DEF_BROWSER, false));
        UiTools.setToolTip(m_isOpenBrowserCb, "If checked, the result file is opened in the system's default " +
                "browser for the given type.\nIf no browser is registered for the output file, error is reported.\n" +
                "Register web browser for XML files, and text editor (notepad) for YAML files in Windows Explorer.");
    }

    
    private void createTestEnvControls(KGUIBuilder builder) {
        KGUIBuilder envGrp = builder.group("Test Environment", 
                                           "span 3, grow, gapbottom 15, wrap",
                                           new String[]{"fill"});
        
        MappingTableEditor tableEditor = new MappingTableEditor(envGrp.getParent(),
                                             "hmin 200, grow",
                                             "",
                                             null,
                                             CTestReportConfig.ETestReportConfigSectionIds.E_SECTION_TEST_INFO.swigValue(),
                                             null,
                                             null,
                                             new String[]{"Attribute", "Value"});
        
        tableEditor.setTooltip("Enter information, which you want to be present in report, as\n"
                               + "key, value pairs. You can use items from content proposals,\n"
                               + "or enter additional items.");
        
        tableEditor.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        AsystContentProposalProvider keyProposalProvider = 
                new AsystContentProposalProvider(new String[]{"",
                                                              "Test Engineer",
                                                              "Date",
                                                              "Time",
                                                              "Software",
                                                              "Hardware",
                                                              "Description",
                                                              "Subversion rev."}, 
                                                 null);
        
        String[] proposalsWDesc = getTestReportHostVarsProposals();
        AsystContentProposalProvider valueProposalProvider = 
            new AsystContentProposalProvider(proposalsWDesc, 
                                             HostVarsUtils.getHostVarsDescriptions(proposalsWDesc));
        
        tableEditor.setContentProposals(keyProposalProvider, valueProposalProvider);
    }

    
    private String[] getTestReportHostVarsProposals() {
        return new String[]{"",
                            CTestHostVars.getRESERVED_DATE(),
                            CTestHostVars.getRESERVED_ISO_TIME(),
                            CTestHostVars.getRESERVED_USER(),
                            CTestHostVars.getRESERVED_WINIDEA_WORKSPACE_DIR(),
                            CTestHostVars.getRESERVED_WINIDEA_WORKSPACE_FILE(),
                            CTestHostVars.getRESERVED_DEFAULT_DL_DIR(),
                            CTestHostVars.getRESERVED_DEFAULT_DL_FILE(),
                            CTestHostVars.getRESERVED_SVN_REVISION()};
    }


    public void fillControls() {
        
        if (m_testReportConfig == null) {
            return;
        }

        m_outFormatRadiosTb.setInput(m_testReportConfig, false, m_actionExecutioner);

        fillFormatSensitiveControls();
        
        m_fullReportXSLTs = findAvailableTemplatesInPlugin(FOLDER_TEMPLATES_FULL, 
                                                           PREF_KEY_MRU_TEMPLATE_FULL,
                                                           XSLT_PATTERN);
        ((Combo)m_xsltURLCombo.getControl()).setItems(m_fullReportXSLTs);
        m_xsltURLCombo.setInput(m_testReportConfig, false, m_actionExecutioner);

//        m_errorReportXSLTs = configureAvailableXSLTs(FOLDER_TEMPLATES_ERRORS,
//                                                     PREF_KEY_MRU_TEMPLATE_ERRORS);
//        ((Combo)m_xsltErrorsOnlyURLCombo.getControl()).setItems(m_errorReportXSLTs);
//        m_xsltErrorsOnlyURLCombo.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_reportCSSs = findAvailableTemplatesInPlugin(FOLDER_TEMPLATES_CSS, 
                                                      PREF_KEY_MRU_TEMPLATE_CSS, 
                                                      CSS_PATTERN); 
        ((Combo)m_cssURLCombo.getControl()).setItems(m_reportCSSs);
        m_cssURLCombo.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_logoFileTb.getTextField().setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_headerTextTb.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_isCreateHtml.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_embedXsltCss.setInput(m_testReportConfig, false, m_actionExecutioner);
        if (m_isCreateHtml.isSelected()) {
            m_embedXsltCss.setSelection(false); // saxon does not handle embedded
            m_embedXsltCss.setEnabled(false);   // CSS correctly.
        }
        
        m_csvSeparatorTxtTb.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_isCSVHeaderCbTb.setInput(m_testReportConfig, false, m_actionExecutioner);

        m_isVerticalColumnHeadersXlsCbTb.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_isAbsPathToLinkTb.setInput(m_testReportConfig, false, m_actionExecutioner);
        m_isIncludeTestSpecTb.setInput(m_testReportConfig, false, m_actionExecutioner);
        
        m_isShowErrosOnlyInHTML.setInput(m_testReportConfig, false, m_actionExecutioner);
    }

    
    private void fillFormatSensitiveControls() {
        
        String extension = "";
        EOutputFormat outputFormat = m_testReportConfig.getOutputFormat();
        Composite topControl = null;
        switch (outputFormat) {
        case FMT_XML:
            topControl = m_xmlComposite;
            extension = EXT_XML;
            break;
        case FMT_YAML:
            topControl = m_yamlComposite;
            extension = EXT_YAML;
            break;
        case FMT_CSV:
            topControl = m_csvComposite;
            extension = EXT_CSV;
            break;
        case FMT_XLS:
            topControl = m_xlsComposite;
            extension = EXT_XLS;
            break;
        case FMT_XLSX:
            topControl = m_xlsComposite;
            extension = EXT_XLSX;
            break;
        default:
            MessageDialog.openError(getParentShell(), "Unsupported test report format!", 
                                    "Contact iSYSTEM support. "
                                    + "Internal error, format = " + outputFormat);
            break;
        }
        
        fillCfgGroupAndOutFName(topControl, extension);
    }

    private void fillCfgGroupAndOutFName(Composite topControl, String extension) {
        m_exportConfigLayout.topControl = topControl;
        m_exportConfigGroup.layout();
        
        String oldFName = m_testReportConfig.getFileName();
        String newFName = UiUtils.replaceExtension(oldFName, extension);
        m_testReportConfig.setFileName(newFName);
        m_fileName.getTextField().setInput(m_testReportConfig, false, m_actionExecutioner);
    }
    
    
    private String[] findAvailableTemplatesInPlugin(String folder, 
                                                    String mruTemplateKey,
                                                    String extension) {
        Preferences preferences = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        Preferences sub1 = preferences.node(PREF_NODE_REPORT_CONFIG_DLG);
        String lastTemplate = sub1.get(mruTemplateKey, null);
        
        List<String> templateList = new ArrayList<String>();
        
        final String prefix = connect.getBUILT_IN_XSLT_PREFIX();
        
        if (lastTemplate != null  &&  !lastTemplate.startsWith(prefix)  &&  
                !lastTemplate.startsWith(ASYST_URL)) {
            templateList.add(lastTemplate);
        }
        
        List<URL> urls = PlatformUtils.listTemplatesInPlugin(folder, extension, false); 
        for (URL url : urls) {
            String [] tFiles = url.getFile().split("/");
            if (tFiles.length > 0) {
                templateList.add(prefix + " " + tFiles[tFiles.length - 1]);
            } else {
                templateList.add(prefix + " " + url.getFile());
            }
        }
        
        /* for (URL url : urls) {  web XSLTs have been removed, because they must be 
         * in the same domain as XML. Only IE shows report in this case, while Chrome and
         * Firefox do not, as required by standard.
            templateList.add(ASYST_URL + url.getFile());
        } */
        return templateList.toArray(new String[]{});
    }
    
    
    private void gui2data() {
        
        // store for next runs of tI
        Preferences preferences = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        
        Preferences sub1 = preferences.node(PREF_NODE_REPORT_CONFIG_DLG);
        boolean isOpenBrowser = m_isOpenBrowserCb.getSelection();
        sub1.putBoolean(PREF_KEY_OPEN_DEF_BROWSER, isOpenBrowser);
        m_isOpenBrowser = isOpenBrowser;
        
        try {
            preferences.flush();
        } catch (org.osgi.service.prefs.BackingStoreException ex) {
            SExceptionDialog.open(getShell(), 
                                  "Can not save browser setting into preferences!", 
                                  ex);
        }
    }

    
    @Override
    protected void okPressed() {
        if (getOutFileName().isEmpty()) {
            MessageDialog.openError(getShell(), "Invalid file name!", 
                                    "Output file name should not be empty!");
            return;
        }

        gui2data();
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }

    
    public boolean show() {
        return open() == Window.OK;
    }

    public CTestReportConfig getTestReportConfig() {
        return m_testReportConfig;
    }
    
    public boolean isOpenBrowserAfterSave() {
        return m_isOpenBrowser;
    }
    
    
    private String getOutFileName() {
        String fName = m_fileName.getTextField().getText().trim();
        if (fName.endsWith(".")) {
            return fName.substring(0, fName.length() - 1);
        }
        return fName;
    }
}
