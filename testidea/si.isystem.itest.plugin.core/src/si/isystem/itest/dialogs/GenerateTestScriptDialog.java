package si.isystem.itest.dialogs;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.Preferences;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestReportConfig;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.handlers.RunConfigurationCmdHandler;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.ui.utils.FileNameBrowser;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class GenerateTestScriptDialog extends Dialog {

    private static final String PREF_NODE_SCRIPT = "scriptTemplate";
    private static final String PREF_KEY_SCRIPT_TEMPLATE_FILE = "scriptTemplateFile";
    private static final String PREF_KEY_SCRIPT_TEMPLATE_FILE_DEFAULT = "isUseDefaultTemplate";
    private static final String PREF_KEY_SCRIPT_IMPORTS = "scriptImports";
    private static final String PREF_KEY_SAVE_REPORT_AS_TEST_IDEA = "saveReportAsTestIDEA";
    private static final String PREF_KEY_SAVE_REPORT_AS_JUNIT = "saveReportAsJUnit";
    private static final String PREF_KEY_EXPORT_TRD_IN_COBERTURA_FMT = "isExportTrdInCoberturaFmt";
    private static final String PREF_KEY_EXPORT_TRD_IN_COBERTURA_FNAME = "exportTrdInCoberturaFName";
    private static final String PREF_KEY_OPEN_REPORT_IN_BROWSER = "openReportInBrowser";
    private static final String PREF_KEY_ICONNECT_DLL_PATH = "iconnectDllPath";
    //  private static final String PREF_LANGUAGE_TYPE = "languageType";

    private static final String DEFAULT_MONITOR_CLASS_NAME = "Monitor";

    public enum ELanguageType { EPython, EPerl};
    
    private FileNameBrowser m_generatedFileNameTxt;
    private Button m_useCustomScriptTemplateCheck;
    private FileNameBrowser m_templateFileTxt;
    private Button m_useCustomTestSpecFileCheck;
    private FileNameBrowser m_testSpecificationFileTxt; 
    private Button m_useCustomWinIDEAWorkspaceCheck;
    private FileNameBrowser m_winIDEAWorkspaceTxt;
    
    private Text m_importsTxt; // comma or space separated list of modules to import
    private Button m_useInitSequenceFromTestSpecCheck;

    private Button m_useProgressMonitorCheck;
    private Button m_useDefaultProgressMonitorCheck;
    private Text m_monitorClassNameTxt;
    
    private Button m_useTestFilterCheck;
    private Combo m_filterIdCombo;
    
    private Button m_saveReportCheck;
    private Button m_saveReportAsJUnitCheck;
    private Button m_openReportInBrowserCheck;
    private Button m_useReportConfigFromTestSpecCheck;

    
    private String m_generatedFileName;
    private boolean m_useCustomScriptTemplate;
    private String m_templateFile;
    private boolean m_isUseCustomTestSpecFile = false; 
    private String m_testSpecificationFile;
    private boolean m_isUseCustomWinIDEAWorkspace = false; 
    private String m_winIDEAWorkspace;
    
    private String m_imports; // comma or space separated list of modules to import
    
    private boolean m_isUseCustomSeq = false;
    
    private boolean m_isUseProgressMonitor = true;
    private boolean m_isUseDefaultProgressMonitor = true;
    private String m_monitorClassName;
    
    private boolean m_isGenerateCallbackClass = false;
    // private String m_callbackClassName;
    
    private boolean m_isUseTestFilter = false;
    private String m_filterId;
    private int m_filterSelectionIndex = 0; // selection in combo box
    
    private boolean m_isSaveReport = true;
    private boolean m_isSaveAsJUnit = false;
    private boolean m_isExportForCobertura = false;
    private String m_exportForCoberturaFName;
    private boolean m_isOpenReportInBrowser = true;
    private boolean m_isUseCustomReportConfig = false;

    
    private CTestEnvironmentConfig m_runConfiguration; // persistent in one application session 
    private CTestReportConfig m_testReportConfig;
    private Text m_iconnectDllPathTxt;
    private String m_iconnectDllPath;
    private Button m_isExportForCoberturaCb;
//    private Button m_pythonRb;
//    private Button m_perlRb;
//    private ELanguageType m_languageType;
    private Text m_exportForCoberturaFNameTxt;
    
    
    public GenerateTestScriptDialog(Shell parentShell) {
        super(parentShell);

        // make a dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        // this one is not modified here`
        m_runConfiguration = TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration();
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Generate Test Script");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 630;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fill", "[fill]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
//        KGUIBuilder languageGrp = builder.group("Language", "gapbottom 20, wrap", true, 
//                                                "fill", "[min!][min!][fill]", null);
//        
//        m_pythonRb = languageGrp.radio("Python", "");
//        UiTools.setToolTip(m_pythonRb, "Generates test script in Python language.");
//        m_perlRb = languageGrp.radio("Perl", "gapleft 30");
//        UiTools.setToolTip(m_perlRb, "Generates test script in Perl language.");

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        
        KGUIBuilder filesGrp = builder.group("Files", "gapbottom 20, wrap", true, 
                                             "fill", "[min!][fill][min!]", null);
        createFileGroup(filesGrp);
        
        KGUIBuilder execConfigGrp = builder.group("Execution configuration", "gapbottom 20, wrap", true, 
                                                  "fill", "[min!][fill][min!]", null);
        Button editTargetInitSeqBtn = createExecConfigGroup(model, execConfigGrp);
        
        KGUIBuilder reportGrp = builder.group("Report", "wrap", true, 
                                              "fill", "[fill]", null);
        Button editReportFormatBtn = createReportGroup(model, reportGrp);

        builder.separator("span, growx, gaptop 20, wrap", SWT.HORIZONTAL);

        addSelectionListeners(editTargetInitSeqBtn, editReportFormatBtn);
        
        initControls();
        
        return composite;
    }


    private void createFileGroup(KGUIBuilder filesGrp) {
        filesGrp.label("Generated script file:", "gapbottom 20");
        m_generatedFileNameTxt = filesGrp.createFileNameInput("Browse", 
                                                              "gapbottom 20, w 100:100:1000", 
                                                              "wrap, gapbottom 20", 
                                                              "Generated Script File", 
                                                              new String[]{"*.py", "*.*"}, 
                                                              true,
                                                              SWT.SAVE);
        m_generatedFileNameTxt.setToolTipText("This field contains name of the output python script file.");
        
        m_useCustomScriptTemplateCheck = filesGrp.checkBox("Use custom script template:", 
                                                           "gaptop 10");
        UiTools.setToolTip(m_useCustomScriptTemplateCheck, "If checked, you can specify your own template file, otherwise the default one is used.");
        
        // filesGrp.label("Template file:", "gapleft 30");
        m_templateFileTxt = filesGrp.createFileNameInput("Browse", 
                                                         "w 100:100:1000", 
                                                         "wrap", 
                                                         "Template Script File", 
                                                         new String[]{"*.py", "*.*"}, 
                                                         true,
                                                         SWT.OPEN);
        m_templateFileTxt.setToolTipText("This file is used as a template when generating output script.\n" +
                                         "If no file name is specified, the default template is used.\n" +
                                         "If you want to create new template, generate a sample script,\n" +
                                         "modify it, and specify its file name here.");
        
        
        m_useCustomTestSpecFileCheck = filesGrp.checkBox("Use custom test spec. file:", 
                                                              "gaptop 10");
        UiTools.setToolTip(m_useCustomTestSpecFileCheck, "If this button is checked, you can specify test specification file.\n" +
                                                         "Otherwise the test specification file currently opened in testIDEA is used in the generated script.");

        // filesGrp.label("Test spec. file:", "gapleft 30");
        m_testSpecificationFileTxt = filesGrp.createFileNameInput("Browse", 
                                                                  "w 100:100:1000", 
                                                                  "wrap", 
                                                                  "Test Specification File", 
                                                                  new String[]{"*.iyaml", "*.*"}, 
                                                                  true,
                                                                  SWT.OPEN);
        m_testSpecificationFileTxt.setToolTipText("The generated script opens this file and reads test specifications from it.\n" +
                                                  "By deafult this file name is the same as the file currently opened in testIDEA,\n" +
                                                  "but we can overrride this setting here.\n" +
                                                  "This field mut not be empty!");

        m_useCustomWinIDEAWorkspaceCheck = filesGrp.checkBox("Use custom winIDEA worksp.:", 
                                                           "gaptop 10");
        UiTools.setToolTip(m_useCustomWinIDEAWorkspaceCheck, "If this button is checked, you can specify winIDEA workspace here.\n" +
                                                      "Otherwise the winIDEA workspace file currently configured in testIDEA is used in the generated script.");

        // filesGrp.label("winIDEA worksp.:", "gapleft 30");
        m_winIDEAWorkspaceTxt = filesGrp.createFileNameInput("Browse", 
                                                             "w 100:100:1000", 
                                                             "wrap", 
                                                             "winIDEA Workspace File", 
                                                             new String[]{"*.xjrf", "*.*"}, 
                                                             true,
                                                             SWT.OPEN);
        m_winIDEAWorkspaceTxt.setToolTipText("This file name is used when the generated script connects to winIDEA.\n" +
                                             "By deafult this file name is taken from 'File | Properties' dialog,\n" +
                                             "but we can overrride this setting here. If empty, the generated script\n" +
        "will connect to the most recently used winIDEA.");
    }


    private Button createExecConfigGroup(TestSpecificationModel model, KGUIBuilder execConfigGrp) {
        execConfigGrp.label("Imports:");
        m_importsTxt = execConfigGrp.text("wrap, gapbottom 10", SWT.BORDER);
        UiTools.setToolTip(m_importsTxt, 
                                 "Specify Python modules to be imported to the generated script,\n" +
                                 "for example the module which contains the class used for monitor. These modules are\n" +
                                 "imported in addition to modules specified in 'File | Properties | Scripts' configuration.\n" +
                                 "Names of modules should be separated by commas, for example:\n" +
                                 "moduleA, moduleB, moduleC");
        

        m_useInitSequenceFromTestSpecCheck = execConfigGrp.checkBox("Use custom init sequence", "span 2, split");
        UiTools.setToolTip(m_useInitSequenceFromTestSpecCheck, "If checked, than init sequence can be specified here, otherwise the script reads it from test specification file.\n" +
        "See option 'Run | Configuration' for setting initialization sequence.");
        
        Button editTargetInitSeqBtn = execConfigGrp.button("Edit script init sequence", "w ::pref, wrap, gapbottom 10");
        UiTools.setToolTip(editTargetInitSeqBtn, "This button open dialog for editing target init steps used in the generated script.\n" +
                                            "Changes in this dialog DO NOT change settings in testIDEA. Setings are persistent\n" +
                                            "in one application session. On startup they are copied from testIDEA project.");
        editTargetInitSeqBtn.setEnabled(m_isUseCustomSeq);

        // filter controls
        m_useTestFilterCheck = execConfigGrp.checkBox("Use filter       ID:", "");
        UiTools.setToolTip(m_useTestFilterCheck, "If checked, only test specifications matching filtering condition\n" +
                                            "are executed.");

        // execConfigGrp.label("Filter ID:", "gapleft 30");
         CTestBaseList filtersVector = model.getTestFilters();
        
        int numFilters = (int)filtersVector.size(); 
        String [] filterIds = new String[numFilters];
        
        for (int i = 0; i < numFilters; i++) {
            CTestFilter filter = CTestFilter.cast(filtersVector.get(i));
            filterIds[i] = filter.getFilterId();
        }
        m_filterIdCombo = execConfigGrp.combo(filterIds, "wrap", SWT.BORDER);
        UiTools.setToolTip(m_filterIdCombo, "ID of the filter settings. Filters can be specified in 'Run | Run with Filter' dialog.");

        
        // progress monitor controls
        m_useProgressMonitorCheck = execConfigGrp.checkBox("Use progress monitor", "span 2, gaptop 10, wrap");
        UiTools.setToolTip(m_useProgressMonitorCheck, "Progress monitor is object, which gets informed abot test execution.\n" +
                                                 "This way it can show information about the currnt number of executed tests to the user.");

        m_useDefaultProgressMonitorCheck = execConfigGrp.checkBox("Use default monitor", "span 2, gapleft 30, wrap");
        UiTools.setToolTip(m_useDefaultProgressMonitorCheck, "If checked, default monitor object is added to test script and used.\n" +
                                                        "It prints the number of executed tests to standard output.");
        
        execConfigGrp.label("Monitor class:", "gapleft 30, gapbottom 10");
        m_monitorClassNameTxt = execConfigGrp.text("wrap, gapbottom 10", SWT.BORDER);
        UiTools.setToolTip(m_monitorClassNameTxt, "Name of the class to use as progress monitor. If class is in another module,\n" +
                                             "prepend also module name, for example 'mymodule.MyProgressMonitor'.");

        execConfigGrp.label("Path to isystem.connect dll:", "split 2, span 2");
        m_iconnectDllPathTxt = execConfigGrp.text("grow x, wrap", SWT.BORDER);
        UiTools.setToolTip(m_iconnectDllPathTxt, "Set this only, if you are going to run this script as a system service or daemon,\n"
                           + "for example from CI server like Jenkins. Enter path to iconnect DLL provided with winIDEA.\n"
                           + "Alternativley you can specify OS environment variable, see Help, search for 'Jenkins'.\n"
                           + "This setting is not saved to iyaml file, but to user preferences.\n"
                           + "Example:\n"
                           + "   C:/winIDEA/2012/iConnect64.dll");
        
        return editTargetInitSeqBtn;
    }

    
    private Button createReportGroup(TestSpecificationModel model, KGUIBuilder reportGrp) {
        m_saveReportCheck = reportGrp.checkBox("Save test report in testIDEA format", "wrap");
        UiTools.setToolTip(m_saveReportCheck, "If checked, test results are saved to file as " +
                                         "specified in 'Test | Save Test Report' or 'Test | Configure Test Report'.");

        m_saveReportAsJUnitCheck = reportGrp.checkBox("Save test report in JUnit format", "wrap");
        UiTools.setToolTip(m_saveReportAsJUnitCheck, "If checked, test results are saved to file in " +
                                         "JUnit report format, which can be used by Jenkins.\n"
                                         + "The same file name is used as for testIDEA file, but "
                                         + "extension is changed to '.junit.xml'.");

        // trd file is used instead of test or group id, because this way files 
        // produced by scripts can also be specified by the user. The disadvantage
        // is, that analyzer file name may not use macros which include time stamp. 
        m_isExportForCoberturaCb = reportGrp.checkBox("Export coverage in Cobertura format. Analyzer (trd) file: ", 
                                                         "split");
        UiTools.setToolTip(m_isExportForCoberturaCb, 
                           "If checked, the given analyzer file will be exported in XML format for "
                           + "Jenkins Cobertura plugin. Two files will be generated:\n"
                           + "- <trdFileName>-isys.xml - export in iSYSTEM coverage export format\n"
                           + "- <trdFileName>-cobertura.xml - export in Cobertura format\n\n"
                           + "The first file will be exported to analyzer file directory, the second\n"
                           + "file to Jenkins workspace.");
        m_exportForCoberturaFNameTxt = reportGrp.text("growx, wrap", SWT.BORDER);
        UiTools.setToolTip(m_exportForCoberturaFNameTxt, 
                           "Enter name of trd file generated during tests (section 'Analyzer' in test cases,\n"
                           + "section 'Coverage confg.' in groups).\n"
                           + "This file may also be produced by extension scripts.");
        
        m_openReportInBrowserCheck = reportGrp.checkBox("Open report in browser", "wrap");
        UiTools.setToolTip(m_openReportInBrowserCheck, "If checked, test report is opened in the system default browser,\n" +
                                                  "after testing is finished. This setting overrides the one in report\n" +
                                                  "configuration dialog.");

        m_useReportConfigFromTestSpecCheck = reportGrp.checkBox("Use custom report configuration", "split 3");
        UiTools.setToolTip(m_useReportConfigFromTestSpecCheck, "If checked, than report configuration can be specified here, otherwise it is read from test specification file.\n" +
                "See option 'File | Save Test Report' to specify report configuration, which is saved to test specificatin file.");

        Button editReportFormatBtn = reportGrp.button("Edit report configuration", "w ::pref, gapright rel:push, wrap");
        UiTools.setToolTip(editReportFormatBtn, "This button opens dialog for editing script specific report configuration.\n" +
                                           "Changes in this dialog DO NOT change settings in testIDEA, only in the generated script.");
        editReportFormatBtn.setEnabled(m_isUseCustomReportConfig);
        
        if (!m_isUseCustomReportConfig && model.getTestReportConfig().getFileName().isEmpty()  ||
             m_isUseCustomReportConfig && m_testReportConfig.getFileName().isEmpty()) {
            reportGrp.label("WARNING: File name for report file is empty. If you want to save test report to file, set it ", "gapleft 20, wrap");
            reportGrp.label("with 'File | Save Test Report' or in custom report configuration.", "gapleft 20");
        }
        return editReportFormatBtn;
    }


    private void addSelectionListeners(final Button editTargetInitSeqBtn, 
                                       final Button editReportFormatBtn) {

//        m_pythonRb.addSelectionListener(new SelectionListener() {
//            
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                if (((Button)e.getSource()).getSelection()) {
//                    replaceExtensions("py");
//                    m_importsTxt.setEnabled(true);
//                    m_useProgressMonitorCheck.setEnabled(true);
//                } 
//            }
//            
//            @Override public void widgetDefaultSelected(SelectionEvent e) {}
//        });
//        
//        
//        m_perlRb.addSelectionListener(new SelectionListener() {
//            
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                if (((Button)e.getSource()).getSelection()) {
//                    replaceExtensions("pl");
//                    m_importsTxt.setEnabled(false);
//                    m_useProgressMonitorCheck.setSelection(false);
//                    m_useDefaultProgressMonitorCheck.setSelection(false);
//                    m_useProgressMonitorCheck.setEnabled(false);
//                } 
//            }
//            
//            @Override public void widgetDefaultSelected(SelectionEvent e) {}
//        });
        
        
        m_useCustomScriptTemplateCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (((Button)e.getSource()).getSelection()) {
                    m_templateFileTxt.setEnabled(true);
                    Preferences preferences = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
                    Preferences prefs = preferences.node(PREF_NODE_SCRIPT);
                    String templateFile = prefs.get(PREF_KEY_SCRIPT_TEMPLATE_FILE, "");
                    m_templateFileTxt.setText(templateFile);
                } else {
                    m_templateFileTxt.setText("");  // default template will be used
                    m_templateFileTxt.setEnabled(false);
                }
            }
        
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });        
        
        
        m_useCustomTestSpecFileCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (((Button)e.getSource()).getSelection()) {
                    m_testSpecificationFileTxt.setEnabled(true);
                } else {
                    String testSpecFileName = 
                        TestSpecificationModel.getActiveModel().getModelFileName();
                    m_testSpecificationFileTxt.setText(testSpecFileName);
                    m_testSpecificationFileTxt.setEnabled(false);
                }
            }
        
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });        

        
        m_useCustomWinIDEAWorkspaceCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (((Button)e.getSource()).getSelection()) {
                    m_winIDEAWorkspaceTxt.setEnabled(true);
                } else {
                    String workspacefileName = 
                        TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration().getWorkspace();
                    m_winIDEAWorkspaceTxt.setText(workspacefileName);
                    m_winIDEAWorkspaceTxt.setEnabled(false);
                }
            }
        
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });        
        
        
        m_useProgressMonitorCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_useDefaultProgressMonitorCheck.setEnabled(m_useProgressMonitorCheck.getSelection());
                m_monitorClassNameTxt.setEnabled(m_useProgressMonitorCheck.getSelection() &&
                                                 !m_useDefaultProgressMonitorCheck.getSelection());
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        m_useDefaultProgressMonitorCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_useDefaultProgressMonitorCheck.setEnabled(m_useProgressMonitorCheck.getSelection());
                m_monitorClassNameTxt.setEnabled(m_useProgressMonitorCheck.getSelection() &&
                                                 !m_useDefaultProgressMonitorCheck.getSelection());
                if (m_useDefaultProgressMonitorCheck.getSelection()) {
                    m_monitorClassNameTxt.setText(DEFAULT_MONITOR_CLASS_NAME);
                }
            }
        
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });

        
        m_useTestFilterCheck.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                m_filterIdCombo.setEnabled(((Button)e.getSource()).getSelection());
            }
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        m_useInitSequenceFromTestSpecCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                editTargetInitSeqBtn.setEnabled(m_useInitSequenceFromTestSpecCheck.getSelection());
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        m_useReportConfigFromTestSpecCheck.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                editReportFormatBtn.setEnabled(m_useReportConfigFromTestSpecCheck.getSelection());
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });

        m_saveReportCheck.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isSave = m_saveReportCheck.getSelection();
                m_openReportInBrowserCheck.setEnabled(isSave);
                m_openReportInBrowserCheck.setSelection(isSave  &&  m_isOpenReportInBrowser);
                m_useReportConfigFromTestSpecCheck.setEnabled(isSave);
                m_useReportConfigFromTestSpecCheck.setSelection(isSave  &&  m_isUseCustomReportConfig);
                editReportFormatBtn.setEnabled(isSave  &&  m_isUseCustomReportConfig);
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });

        
        editTargetInitSeqBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                RunConfigurationCmdHandler runConfigCmd = new RunConfigurationCmdHandler();
                runConfigCmd.useNewEnvConfig();
                try {
                    runConfigCmd.execute(null);
                    m_runConfiguration = runConfigCmd.getEnvConfig();
                } catch (ExecutionException ex) {
                    SExceptionDialog.open(getShell(), "Can't edit run configuration!", ex);
                }
                
                /*
                RunConfigurationDialog dlg = new RunConfigurationDialog(getShell(), m_runConfiguration);
                if (dlg.show()) {
                    m_runConfiguration = dlg.getRunConfiguration();
                } */
            }
        
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        editReportFormatBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                SaveTestReportDialog dlg = new SaveTestReportDialog(getShell(),
                                                                    "Configure Test Report",
                                                                    m_testReportConfig);
                if (dlg.show()) {
                    m_testReportConfig.assign(dlg.getTestReportConfig());
                }
            }
            
            @Override public void widgetDefaultSelected(SelectionEvent e) {}
        });
    }


    // replaces extensions of files, which depend on the selected script language: Python, Perl, ...
//    private void replaceExtensions(String newExtension) {
//        String fileName = m_generatedFileNameTxt.getText();
//        String newFileName = UiUtils.replaceExtension(fileName, newExtension);
//        m_generatedFileNameTxt.setText(newFileName);
//        
//        if (m_useCustomScriptTemplateCheck.getSelection()) {
//            fileName = m_templateFileTxt.getText();
//            newFileName = UiUtils.replaceExtension(fileName, newExtension);
//            m_templateFileTxt.setText(newFileName);
//        }
//
//    }

    
    private void initControls() {

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        Preferences preferences = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        Preferences prefs = preferences.node(PREF_NODE_SCRIPT);
        
//        if (m_languageType == null) {
//            m_languageType = ELanguageType.valueOf(prefs.get(PREF_LANGUAGE_TYPE, "EPython"));
//        }
        
        String scriptExtension = "py";
//        switch (m_languageType) {
//        case EPython:
//            m_pythonRb.setSelection(true);
//            m_perlRb.setSelection(false);
//            scriptExtension = "py";
//            m_importsTxt.setEnabled(true);
//            m_useProgressMonitorCheck.setEnabled(true);
//            break;
//        case EPerl:
//            m_pythonRb.setSelection(false);
//            m_perlRb.setSelection(true);
//            scriptExtension = "pl";
//            m_importsTxt.setEnabled(false);
//            // No progress monitor is available for Perl.
//            m_useDefaultProgressMonitorCheck.setSelection(false);
//            m_useProgressMonitorCheck.setSelection(false);
//            m_useProgressMonitorCheck.setEnabled(false);
//            break;
//        }
        
        // if not initialized, init items based on settings already present in testIDEA 
        // model or preferences
        if (m_generatedFileName == null) {
            String testSpecFileName = model.getModelFileName();
            if (testSpecFileName != null) {
                m_generatedFileName = UiUtils.replaceExtension(testSpecFileName , scriptExtension);
            } else {
                m_generatedFileName = "testIDEAScript." + scriptExtension;
            }
        } else {
            m_generatedFileName = UiUtils.replaceExtension(m_generatedFileName, scriptExtension);
        }
        
        if (m_templateFile == null) {
            m_templateFile = prefs.get(PREF_KEY_SCRIPT_TEMPLATE_FILE, "");
        }

        if (m_testSpecificationFile == null) {
            m_testSpecificationFile = model.getModelFileName();
            if (m_testSpecificationFile == null) {
                m_testSpecificationFile = "";
            }
        }
        
        if (m_isUseCustomWinIDEAWorkspace) {
            if (m_winIDEAWorkspace == null) {
                m_winIDEAWorkspace = model.getCEnvironmentConfiguration().getWorkspace();
            }
        } else {
            m_winIDEAWorkspace = model.getCEnvironmentConfiguration().getWorkspace();
        }

        if (m_imports == null) {
            m_imports = prefs.get(PREF_KEY_SCRIPT_IMPORTS, "");
        }

        if (m_monitorClassName == null) {
            m_monitorClassName = DEFAULT_MONITOR_CLASS_NAME;
        }
        
        if (m_filterId == null) {
            m_filterId = "";
        }

        if (m_testReportConfig == null  ||  !m_isUseCustomReportConfig) {
            // update copy of report config from the one from model, because
            // it may have been modified between two invocations of this dialog
            CTestReportConfig reportConfig = model.getTestReportConfig();

            m_testReportConfig = new CTestReportConfig(null); 
            m_testReportConfig.assign(reportConfig);
            m_testReportConfig.setIYamlFileName(model.getModelFileName());
        }
        
        m_useCustomScriptTemplate = prefs.getBoolean(PREF_KEY_SCRIPT_TEMPLATE_FILE_DEFAULT, false);
        m_isSaveReport = prefs.getBoolean(PREF_KEY_SAVE_REPORT_AS_TEST_IDEA, true);
        m_isSaveAsJUnit = prefs.getBoolean(PREF_KEY_SAVE_REPORT_AS_JUNIT, false);
        m_isExportForCobertura = prefs.getBoolean(PREF_KEY_EXPORT_TRD_IN_COBERTURA_FMT, false);
        m_exportForCoberturaFName =  prefs.get(PREF_KEY_EXPORT_TRD_IN_COBERTURA_FNAME, "");
        m_isOpenReportInBrowser = prefs.getBoolean(PREF_KEY_OPEN_REPORT_IN_BROWSER, true);
        m_iconnectDllPath = prefs.get(PREF_KEY_ICONNECT_DLL_PATH, "");
        
        m_generatedFileNameTxt.setText(m_generatedFileName);
        m_useCustomScriptTemplateCheck.setSelection(m_useCustomScriptTemplate);
        m_templateFileTxt.setText(m_templateFile);
        m_templateFileTxt.setEnabled(m_useCustomScriptTemplate);
        
        m_testSpecificationFileTxt.setText(m_testSpecificationFile);
        m_testSpecificationFileTxt.setEnabled(m_isUseCustomTestSpecFile);
        m_useCustomTestSpecFileCheck.setSelection(m_isUseCustomTestSpecFile);
        
        m_winIDEAWorkspaceTxt.setText(m_winIDEAWorkspace);
        m_winIDEAWorkspaceTxt.setEnabled(m_isUseCustomWinIDEAWorkspace);
        m_useCustomWinIDEAWorkspaceCheck.setSelection(m_isUseCustomWinIDEAWorkspace);
        
        
        m_importsTxt.setText(m_imports);
        
        m_useInitSequenceFromTestSpecCheck.setSelection(m_isUseCustomSeq);
        
        m_useProgressMonitorCheck.setSelection(m_isUseProgressMonitor);
        m_useDefaultProgressMonitorCheck.setSelection(m_isUseDefaultProgressMonitor);
        m_useDefaultProgressMonitorCheck.setEnabled(m_isUseProgressMonitor);
        m_monitorClassNameTxt.setText(m_isUseDefaultProgressMonitor ? 
                                      DEFAULT_MONITOR_CLASS_NAME : m_monitorClassName);
        m_monitorClassNameTxt.setEnabled(m_isUseProgressMonitor && !m_isUseDefaultProgressMonitor);
        
        // m_generateCallbackClassCheck.setSelection(m_isGenerateCallbackClass);
        // m_callbackClassNameTxt.setText(m_callbackClassName);

        m_useTestFilterCheck.setSelection(m_isUseTestFilter);
        m_filterIdCombo.setEnabled(m_isUseTestFilter);
        m_filterIdCombo.select(m_filterSelectionIndex);
        
        m_iconnectDllPathTxt.setText(m_iconnectDllPath);

        m_saveReportCheck.setSelection(m_isSaveReport);
        m_saveReportAsJUnitCheck.setSelection(m_isSaveAsJUnit);
        m_isExportForCoberturaCb.setSelection(m_isExportForCobertura);
        m_exportForCoberturaFNameTxt.setText(m_exportForCoberturaFName);
        m_openReportInBrowserCheck.setSelection(m_isOpenReportInBrowser & m_isSaveReport);
        m_openReportInBrowserCheck.setEnabled(m_isSaveReport);
        m_useReportConfigFromTestSpecCheck.setSelection(m_isSaveReport && m_isUseCustomReportConfig);
        m_useReportConfigFromTestSpecCheck.setEnabled(m_isSaveReport);
    }


    @Override
    protected void okPressed() {

//        m_languageType = ELanguageType.EPython;
//        if (m_pythonRb.getSelection()) {
//            m_languageType =  ELanguageType.EPython;
//        }
//        if (m_perlRb.getSelection()) {
//            m_languageType = ELanguageType.EPerl;
//        }

        m_generatedFileName = m_generatedFileNameTxt.getText().trim();
        m_templateFile = m_templateFileTxt.getText().trim();
        m_useCustomScriptTemplate = m_useCustomScriptTemplateCheck.getSelection();
        m_testSpecificationFile = m_testSpecificationFileTxt.getText().trim();
        m_isUseCustomTestSpecFile = m_useCustomTestSpecFileCheck.getSelection();
        m_winIDEAWorkspace = m_winIDEAWorkspaceTxt.getText().trim();
        m_isUseCustomWinIDEAWorkspace = m_useCustomWinIDEAWorkspaceCheck.getSelection();

        m_imports = m_importsTxt.getText().trim();
        
        m_isUseCustomSeq = m_useInitSequenceFromTestSpecCheck.getSelection();

        m_isUseProgressMonitor = m_useProgressMonitorCheck.getSelection();
        m_isUseDefaultProgressMonitor = m_useDefaultProgressMonitorCheck.getSelection();
        m_monitorClassName = m_monitorClassNameTxt.getText().trim();

        // m_isGenerateCallbackClass = m_generateCallbackClassCheck.getSelection();
        // m_callbackClassName = m_runConfiguration.getScriptConfig().getExtensionClass();

        m_isUseTestFilter = m_useTestFilterCheck.getSelection();
        m_filterId = m_filterIdCombo.getText().trim();
        m_filterSelectionIndex = m_filterIdCombo.getSelectionIndex();
        m_iconnectDllPath = m_iconnectDllPathTxt.getText().trim();

        m_isSaveReport = m_saveReportCheck.getSelection();
        m_isSaveAsJUnit = m_saveReportAsJUnitCheck.getSelection();
        m_isExportForCobertura = m_isExportForCoberturaCb.getSelection();
        m_exportForCoberturaFName = m_exportForCoberturaFNameTxt.getText().trim();
        m_isOpenReportInBrowser = m_openReportInBrowserCheck.getSelection();
        m_isUseCustomReportConfig = m_useReportConfigFromTestSpecCheck.getSelection();

        Preferences preferences = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        Preferences prefs = preferences.node(PREF_NODE_SCRIPT);

        // prefs.put(PREF_LANGUAGE_TYPE, m_languageType.toString());
        
        prefs.putBoolean(PREF_KEY_SCRIPT_TEMPLATE_FILE_DEFAULT, m_useCustomScriptTemplate);
        prefs.put(PREF_KEY_SCRIPT_TEMPLATE_FILE, m_templateFile);
        prefs.put(PREF_KEY_SCRIPT_IMPORTS, m_imports);
        
        prefs.putBoolean(PREF_KEY_SAVE_REPORT_AS_TEST_IDEA, m_isSaveReport);
        prefs.putBoolean(PREF_KEY_SAVE_REPORT_AS_JUNIT, m_isSaveAsJUnit);
        prefs.putBoolean(PREF_KEY_EXPORT_TRD_IN_COBERTURA_FMT, m_isExportForCobertura);
        prefs.put(PREF_KEY_EXPORT_TRD_IN_COBERTURA_FNAME, m_exportForCoberturaFName);
        prefs.putBoolean(PREF_KEY_OPEN_REPORT_IN_BROWSER, m_isOpenReportInBrowser);
        
        prefs.put(PREF_KEY_ICONNECT_DLL_PATH, m_iconnectDllPath);
        
        try {
            prefs.flush();
        } catch (org.osgi.service.prefs.BackingStoreException ex) {
            SExceptionDialog.open(getShell(), 
                                  "Can not save the name of template file into preferences!", 
                                  ex);
        }
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }

    
//    public ELanguageType getLanguage() {
//        return m_languageType;
//    }

    
    public String getGeneratedFileName() {
        return m_generatedFileName;
    }


    public String getTemplateFile() {
        return m_templateFile;
    }


    public String getTestSpecificationFile() {
        return m_testSpecificationFile;
    }


    public boolean isUseCustomTestSpecFile() {
        return m_isUseCustomTestSpecFile;
    }


    public String getWinIDEAWorkspace() {
        return m_winIDEAWorkspace;
    }


    public boolean isUseCustomWinIDEAWorkspace() {
        return m_isUseCustomWinIDEAWorkspace;
    }


    public String getImports() {
        return m_imports;
    }


    public boolean isUseCustomInitSeq() {
        return m_isUseCustomSeq;
    }


    public boolean isUseProgressMonitor() {
        return m_isUseProgressMonitor;
    }


    public boolean isUseDefaultProgressMonitor() {
        return m_isUseDefaultProgressMonitor;
    }


    public String getMonitorClassName() {
        return m_monitorClassName;
    }


    public boolean isGenerateCallbackClass() {
        return m_isGenerateCallbackClass;
    }


    /* public String getCallbackClassName() {
        return m_callbackClassName;
    } */


    public boolean isUseTestFilter() {
        return m_isUseTestFilter;
    }


    public String getFilterId() {
        return m_filterId;
    }

    
    public String getIconnectDllPath() {
        return m_iconnectDllPath;
    }


    public boolean isSaveReport() {
        return m_isSaveReport;
    }


    public boolean isSaveReportAsJUnit() {
        return m_isSaveAsJUnit;
    }


    public boolean isExportForCobertura() {
        return m_isExportForCobertura;
    }


    public String getExportForCoberturaFName() {
        return m_exportForCoberturaFName;
    }


    public boolean isOpenReportInBrowser() {
        return m_isOpenReportInBrowser;
    }


    public boolean isUseCustomReportConfig() {
        return m_isUseCustomReportConfig;
    }


    public CTestEnvironmentConfig getRunConfiguration() {
        return m_runConfiguration;
    }


    public CTestReportConfig getTestReportConfig() {
        return m_testReportConfig;
    }
}
