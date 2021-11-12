package si.isystem.itest.wizards;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestSpecification;
import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

/**
 * This class implements wizard page for iTools menu, table editor toolbar
 * script methods, and script methods for report generation. 
 * @author markok
 *
 */
public class ExtScriptToolbarPage extends WizardPage implements IExtScriptPage{

    private static final String PREFS_PREFIX = "itestScriptExt.toolbar.";
    private static final String PREFS_TOOLBAR_METHOD_NAME = "itoolbarScriptMethod";
    private static final String PREFS_TABLE_EDITOR_METHOD_NAME = "tableEditorScriptMethod";
    
    private Button m_isCreateiToolsMethodCb;
    private Button m_isCreateTableEditorMethodCb;
    private Button m_isCreateTestReportDataMethodCb;
    private Button m_isCreateAfterReportSaveMethodCb;

    private Text m_iToolsMethodNameTxt;
    private Text m_tableEditorMethodNameTxt;

    private Button m_isAddExampleForiToolsMenuCb;
    private Button m_isAddExampleForTableEditorCb;
    private Button m_isAddExampleForReportDataCb;

    private Text m_previewTxt;

    private ModifyListener m_txtModilfyListener = new ModifyListener() {
        
        @Override
        public void modifyText(ModifyEvent e) {
            showPreview();
            setPageComplete(isMyPageComplete());
        }
    };
    
    private SelectionAdapter m_selListener = new SelectionAdapter() {
        
        @Override
        public void widgetSelected(SelectionEvent e) {
            showPreview();
        }
    };
    
    
    public ExtScriptToolbarPage() {
        super("Create script methods for iTools menu, table editor and reports");
        setTitle("General Script Extension Methods");
        setDescription("Create script methods for iTools menu, table editor (toolbar button) and reports.");
    }


    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        container.setLayout(new MigLayout("fillx"));

        KGUIBuilder builder = new KGUIBuilder(container);

        addiToolsMenuMethod(builder);

        addTableEditorMethod(builder);
        
        addTestReportCustomDataMethod(builder);
        
        addAfterTestReportSaveMethod(builder);
        
        builder.label("Preview of the generated methods:", "gaptop 15, wrap");
        m_previewTxt = builder.text("span 3, grow, pushy, w 300::, h 100:100:", 
                                    SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | 
                                    SWT.V_SCROLL);
        m_previewTxt.setEditable(false);
        m_previewTxt.setFont(FontProvider.instance().getFixedWidthControlFont(m_previewTxt));
        UiTools.setToolTip(m_previewTxt, "This area contains preview of the methods, which will be generated.");

        initControls();
        
        setControl(container);
        setPageComplete(isMyPageComplete());
    }


    private boolean isMyPageComplete() {
        return !(m_isCreateiToolsMethodCb.getSelection()  &&  m_iToolsMethodNameTxt.getText().trim().isEmpty() 
                ||  m_isCreateTableEditorMethodCb.getSelection()  &&  m_tableEditorMethodNameTxt.getText().trim().isEmpty());
    }

    
    private KGUIBuilder addiToolsMenuMethod(KGUIBuilder builder) {
        KGUIBuilder iToolsMethodGrp = builder.group("Method for iTools menu", 
                                                  "grow x, wrap", true,
                                                  "fillx", "[min!][min!][fill]", "");
        
        m_isCreateiToolsMethodCb = iToolsMethodGrp.checkBox("Create script method to be listed in iTools menu", 
                                                            "span 3, wrap");
        iToolsMethodGrp.label("Method name:  ", "");
        Label lbl = iToolsMethodGrp.label(CScriptConfig.getEXT_METHOD_CUSTOM_PREFIX());
        lbl.setFont(FontProvider.instance().getFixedWidthControlFont(lbl));

        m_iToolsMethodNameTxt = iToolsMethodGrp.text("wrap", SWT.BORDER);
        UiTools.setToolTip(m_iToolsMethodNameTxt, 
          "Enter name of the script method. Prefix required for method to be shown in iTools menu "
          + "will be added automatically");

        m_isAddExampleForiToolsMenuCb = iToolsMethodGrp.checkBox("Add example for simple print method", 
                                                                 "skip, span 2, wrap");
        UiTools.setToolTip(m_isAddExampleForiToolsMenuCb, 
                           "This example prints data to testIDEA Status view.");

        m_isCreateiToolsMethodCb.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCreate = m_isCreateiToolsMethodCb.getSelection();
                m_iToolsMethodNameTxt.setEnabled(isCreate);
                m_isAddExampleForiToolsMenuCb.setEnabled(isCreate);
                showPreview();
                setPageComplete(isMyPageComplete());
            }
        });
        
        m_isAddExampleForiToolsMenuCb.addSelectionListener(m_selListener);
        m_iToolsMethodNameTxt.addModifyListener(m_txtModilfyListener);
        
        return iToolsMethodGrp;
    }

    
    private void addTableEditorMethod(KGUIBuilder builder) {

        KGUIBuilder tableEditorMethodGrp = builder.group("Method for table editor", 
                                                         "grow x, wrap", true,
                                                         "fillx", "[min!][min!][fill]", "");

        m_isCreateTableEditorMethodCb = tableEditorMethodGrp.checkBox("Create script method, which enters data into table", 
                                                                      "span 3, wrap");

        tableEditorMethodGrp.label("Method name:  ", "");
        Label lbl = tableEditorMethodGrp.label(CScriptConfig.getEXT_METHOD_TABLE_PREFIX());
        lbl.setFont(FontProvider.instance().getFixedWidthControlFont(lbl));
        
        m_tableEditorMethodNameTxt = tableEditorMethodGrp.text("wrap", SWT.BORDER);
        UiTools.setToolTip(m_tableEditorMethodNameTxt, "Enter name of the script method. Prefix required for method to be shown in iTools menu "
                + "will be added automatically");

        m_isAddExampleForTableEditorCb = tableEditorMethodGrp.checkBox("Add example for geometric series", 
                                                                       "skip, span2, wrap");
        UiTools.setToolTip(m_isAddExampleForTableEditorCb, 
                           "This example contains code constructs for typical table method: checks input parameters,\n"
                                   + "loops over cells, evaluates new values, and returns result.");

        m_isCreateTableEditorMethodCb.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCreate = m_isCreateTableEditorMethodCb.getSelection();
                m_tableEditorMethodNameTxt.setEnabled(isCreate);
                m_isAddExampleForTableEditorCb.setEnabled(isCreate);
                showPreview();
                setPageComplete(isMyPageComplete());
            }
        });

        m_isAddExampleForTableEditorCb.addSelectionListener(m_selListener);

        m_tableEditorMethodNameTxt.addModifyListener(m_txtModilfyListener);
        
    }

    
    private void addTestReportCustomDataMethod(KGUIBuilder builder) {

        KGUIBuilder tableEditorMethodGrp = builder.group("Method for providing custom data to test report", 
                                                         "grow x, wrap", true,
                                                         "fillx", "[min!][fill][min!]", "");

        m_isCreateTestReportDataMethodCb = tableEditorMethodGrp.checkBox("Create script method 'isys_getTestReportCustomData()'", 
                                                                         "wrap");

        m_isAddExampleForReportDataCb = tableEditorMethodGrp.checkBox("Add example for simple data", "wrap");
        UiTools.setToolTip(m_isAddExampleForReportDataCb, 
                           "This example creates and returns two key-value pairs.");
        
        m_isCreateTestReportDataMethodCb.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCreate = m_isCreateTestReportDataMethodCb.getSelection();
                m_isAddExampleForReportDataCb.setEnabled(isCreate);
                showPreview();
                setPageComplete(isMyPageComplete());
            }
        });
        
        m_isAddExampleForReportDataCb.addSelectionListener(m_selListener);
    }

    
    private void addAfterTestReportSaveMethod(KGUIBuilder builder) {

        KGUIBuilder tableEditorMethodGrp = builder.group("Method to be called after test report is saved", 
                                                         "grow x, wrap", true,
                                                         "fillx", "[min!][fill][min!]", "");

        m_isCreateAfterReportSaveMethodCb = 
                tableEditorMethodGrp.checkBox("Create script method 'isys_afterReportSave()'", "");

//        m_isAddExampleForReportSaveCb = tableEditorMethodGrp.checkBox("Add example with simple print statement", "skip, wrap");
//        UiTools.setToolTip(m_isAddExampleForReportSaveCb, 
//                           "This example only prints text to testIDEA Status view, to show it has been called.");
        m_isCreateAfterReportSaveMethodCb.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                showPreview();
                setPageComplete(isMyPageComplete());
            }
        });
    }

    
    private void initControls() {
    
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        
        m_iToolsMethodNameTxt.setText(prefs.getString(PREFS_PREFIX + PREFS_TOOLBAR_METHOD_NAME));
        m_tableEditorMethodNameTxt.setText(prefs.getString(PREFS_PREFIX + PREFS_TABLE_EDITOR_METHOD_NAME));
        
    }

    
    public void saveToPreferences() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        
        prefs.setValue(PREFS_PREFIX + PREFS_TOOLBAR_METHOD_NAME, m_iToolsMethodNameTxt.getText());
        prefs.setValue(PREFS_PREFIX + PREFS_TABLE_EDITOR_METHOD_NAME, m_tableEditorMethodNameTxt.getText());
    }

    
    private void showPreview() {
        
        String methods = generateScriptMethod(null);
        m_previewTxt.setText(methods);
    }
    
    
    @Override
    public String generateScriptMethod(CTestSpecification testSpec) {
        StringBuilder sb = new StringBuilder();
        
        if (m_isCreateiToolsMethodCb.getSelection()) {
            generateToolbarMethod(sb);
        }
        
        if (m_isCreateTableEditorMethodCb.getSelection()) {
            generateTableEditorMethod(sb);
        }
        
        if (m_isCreateTestReportDataMethodCb.getSelection()) {
            generateReportCustomDataMethod(sb);
        }
        
        if (m_isCreateAfterReportSaveMethodCb.getSelection()) {
            generateAfterReportSaveMethod(sb);
        }        

        return sb.toString();
    }


    private void generateToolbarMethod(StringBuilder sb) {
        
        sb.append("    def ").append(CScriptConfig.getEXT_METHOD_CUSTOM_PREFIX() + 
                                     m_iToolsMethodNameTxt.getText()).append("(self, iyamlFile):\n");

        if (m_isAddExampleForiToolsMenuCb.getSelection()) {
            sb.append(
              "        \"\"\"\n"
            + "        This method prints data to testIDEA Status view. It appears in\n"
            + "        testIDEA iTools menu after 'Refresh' command in testIDEA is\n"
            + "        executed. Prefix must be 'isys_cmd_', and parameters must be\n"
            + "        defined as in this method.\n"
            + "\n"
            + "        Parameters:\n"
            + "        iyamlFile - name of iyaml file active in testIDEA when this\n"
            + "                    method was called.\n"
            + "        \"\"\"\n"
            + "\n"
            + "        print('HI! Script method executed, parameter = ', iyamlFile)\n\n\n");
        } else {
            sb.append("        pass\n\n\n");
        }
    }


    private void generateTableEditorMethod(StringBuilder sb) {
        sb.append("    def ").append(CScriptConfig.getEXT_METHOD_TABLE_PREFIX() + 
                                     m_tableEditorMethodNameTxt.getText()).append("(self, selectedCells):\n");

        if (m_isAddExampleForTableEditorCb.getSelection()) {
            sb.append("        \"\"\"\n"
                    + "        This method is listed in table editor toolbar. It generates\n" 
                    + "        geometric series based on the first two elements.\n"
                    + "\n"
                    + "        Parameters:\n"
                    + "        - selectedCells - list of tuples [(column, row, value), ... ]\n"
                    + "        'column' and 'row' are integers defining cell position in the\n"
                    + "        table, while 'value' is cell content given as string.\n"
                    + "        List of tuples is used instead of map to preserve cell order.\n"
                    + "        \"\"\"\n"
                    + "\n"
                    + "        if len(selectedCells) < 3:\n"
                    + "            return 'There should be at least three cells provided.'\n"
                    + "\n"
                    + "        if not selectedCells[0][2] or not selectedCells[1][2]:\n"
                    + "            return 'The first two cells must have value defined!'\n"
                    + "\n"
                    + "        firstVal = float(selectedCells[0][2])\n"
                    + "        value = float(selectedCells[1][2])\n"
                    + "        ratio = value / firstVal\n"
                    + "\n"
                    + "        outValues = [] # list to contain tuples (column, row, value)\n"
                    + "        for idx in range(2, len(selectedCells)):\n"
                    + "\n"
                    + "            inCell = selectedCells[idx]\n"
                    + "\n"
                    + "            column = inCell[0]\n"
                    + "            row = inCell[1]\n"
                    + "            value *= ratio\n"
                    + "\n"
                    + "            outCell = (column, row, value)\n"
                    + "            outValues.append(outCell)\n"
                    + "\n"
                    + "        # This variable will be processed by testIDEA. It should\n"
                    + "        # contain data as string.\n"
                    + "        self._isys_tableInfo = str(outValues)\n"
                    + "\n\n"
                    );
        } else {
            sb.append("        pass\n\n\n");
        }
    }


    private void generateReportCustomDataMethod(StringBuilder sb) {
        sb.append("    def ").append(CScriptConfig.getEXT_METHOD_GET_TEST_REPORT_CUSTOM_DATA()) 
                             .append("(self, reportConfig):\n");

        if (m_isAddExampleForReportDataCb.getSelection()) {
            sb.append("        \"\"\"\n"
                      + "        This method is called before test report is saved. Custom data must\n"
                      + "        be in <key>: <value> pairs separated by new lines, for example:\n"
                      + "\n"
                      + "          _appRev: 12355\n"
                      + "          bootloaderRev: 1.05g\n"
                      + "\n"
                      + "        \"\"\"\n\n"
                      + "        # define the data in mapping\n"
                      + "        data = {'_appRev': 12355, 'bootloaderRev': '1.05g'}\n"
                      + "\n"
                      + "        # convert data to list of key-value pairs\n"
                      + "        dataAsList = [(k + ': ' + str(v)) for k, v in data.items()]\n"
                      + "        self._isys_customScriptMethodInfo = '\\n'.join(dataAsList)\n"
                    + "\n\n"
                    );
        } else {
            sb.append("        pass\n\n\n");
        }
    }


    private void generateAfterReportSaveMethod(StringBuilder sb) {
        sb.append("    def ").append(CScriptConfig.getEXT_METHOD_AFTER_REPORT_SAVE())
                             .append("(self, reportConfig):\n");

        sb.append("        \"\"\"\n"
                + "        This method is called after test report is saved. It can be used to\n"
                + "        copy, send, or transform the report, for example.\n"
                + "        \"\"\"\n\n"
                + "        print('afterReportSave() called with reportConfig = ', reportConfig)\n"
                + "\n\n");
    }

   
    @Override
    public String generateScriptMethodName(CTestSpecification testSpec) {
        return "dummyName";
    }

}
