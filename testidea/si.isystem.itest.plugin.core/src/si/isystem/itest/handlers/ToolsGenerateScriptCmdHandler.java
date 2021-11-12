package si.isystem.itest.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CInitSequenceAction;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestEnvironmentConfig.EEnvConfigSections;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestReportConfig.ETestReportConfigSectionIds;
import si.isystem.connect.StrVector;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.ISysPathFileUtils;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.GenerateTestScriptDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.mk.utils.PlatformUtils;

public class ToolsGenerateScriptCmdHandler extends AbstractHandler {

    private static final String DEFAULT_PYTHON_TEMPLATE_FILE = "templates/pythonTestTemplate.py";
    // private static final String DEFAULT_PERL_TEMPLATE_FILE = "templates/perlTestTemplate.pl";

    private static final String PY_CONFIG_PREFIX = "#@";

    private static final String EXTENSION_OBJ_FACTORY = "createExtensionObject";
    
    private static GenerateTestScriptDialog m_dlg;

    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();

        try {

            if (m_dlg == null) {
                // lazy initialization, keeps changes in dialog persistent
                m_dlg = new GenerateTestScriptDialog(shell);
            }
            
            if (m_dlg.show()) {
                if (generateScript(m_dlg)) {
                    TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                    if (model != null  &&  model.isModelDirty()) {
                        boolean answer = MessageDialog.openQuestion(shell, 
                                                                    "Save", 
                                "The script is generated, but data is not saved. "
                                + "Do you want to save it now?");
                        if (answer) {
                            new FileSaveCmdHandler().execute(null);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Can not generate script!", 
                                  ex);
        }
        
        return null;
    }


    /** Returns true, if script was generated, false otherwise (user decided not to
     * overwrite existing script). 
     */
    @SuppressWarnings("resource")
    private boolean generateScript(GenerateTestScriptDialog dlg) throws IOException {
        
        // open template file
        FileInputStream istr = null;
        String templateFileName = dlg.getTemplateFile();
        
        if (templateFileName.isEmpty()) {
            templateFileName = DEFAULT_PYTHON_TEMPLATE_FILE;
        
            // if template file is not specified, open the one in plugin jar file
//            switch (dlg.getLanguage()) {
//            case EPython:
//                templateFileName = DEFAULT_PYTHON_TEMPLATE_FILE;
//                break;
//            case EPerl:
//                templateFileName = DEFAULT_PERL_TEMPLATE_FILE;
//                break;
//            default:
//                throw new SIllegalStateException("Unsupported language for script generation: " + dlg.getLanguage());
//            }
            
            try {
                istr = PlatformUtils.openFileInPlugin(templateFileName);
            } catch (Exception ex) {
                if (istr != null) {
                    istr.close();
                }
                throw new SIOException("Can not open script template file!", ex)
                          .add("templateFileName", templateFileName);
            }
            
        } else {
            istr = new FileInputStream(templateFileName);
        }
        
        // copy template file to the generated file
        String scriptFileName = dlg.getGeneratedFileName();
        scriptFileName = ISysPathFileUtils.getAbsPathFromWorkingDir(scriptFileName);
        
        if (!UiUtils.checkForFileOverwrite(Activator.getShell(), scriptFileName)) {
            istr.close();
            return false;
        }

        boolean isPrefixExists = false;

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(scriptFileName)))) {
        
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(istr))) {

                String line = reader.readLine();
                while (line != null) {
                    out.write(line);
                    out.newLine();

                    if (line.trim().startsWith(PY_CONFIG_PREFIX)) {
                        isPrefixExists = true;
                        writeConfiguration(out, dlg);

                        // skip lines until '#@' is reached again  
                        line = reader.readLine();
                        while (line != null) {
                            if (line.trim().startsWith(PY_CONFIG_PREFIX)) {
                                break;
                            }
                            line = reader.readLine();
                        }

                        if (line == null) {
                            reader.close();
                            out.close();
                            throw new SIOException("Premature end of template file! Missing the 'end of configuration' tag: " + 
                                    PY_CONFIG_PREFIX);
                        }
                        while (line != null) {
                            out.write(line);
                            out.newLine();
                            line = reader.readLine();
                        }
                        break;
                    }

                    line = reader.readLine();
                }
            }
        }

        File outFolder = new File(scriptFileName).getParentFile();
        if (dlg.isSaveReport()) {
            // copy XSLT and CSS files to the same folder as generated script
            // The script will later copy them to the report output folder
            TestSaveTestReportCmdHandler.copyXSLTAndCss(dlg.getTestReportConfig(),
                                                        outFolder,
                                                        true);
        }
        
        if (dlg.isExportForCobertura()) {
            // does nothing - remove this call in the future, see copyXSLTForCobertura()
            TestSaveTestReportCmdHandler.copyXSLTForCobertura(dlg.getTestReportConfig().getAbsReportFileName(),
                                                              outFolder);
        }
        
        if (!isPrefixExists) {
            MessageDialog.openWarning(Activator.getShell(),
                                      "No custom info saved", "The script was generated, but the given " +
                                      "template contains no markers for customized data, " +
                                      "so no information from the dialog was saved to the generated script.\n\n" +
                                      "Expected marker (on start and end line): " + PY_CONFIG_PREFIX + "\n" +
                                      "template file: " + dlg.getTemplateFile());
        }
        
        return true;
    }


    /**
     * This method writes configuration part of Python script according to
     * settings in the dialog.
     *  
     * @param out
     * @param dlg
     * @throws IOException 
     */
    private void writeConfiguration(BufferedWriter out,
                                    GenerateTestScriptDialog dlg) throws IOException {
        
        out.newLine();
        
        // write sys.path
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
        CScriptConfig scriptConfig = envConfig.getScriptConfig(true);
        LangaugeWriter writer = null;
        String reportCfgScope = null;
        
        writePythonImports(out, dlg, scriptConfig);
        writer = new PythonWriter(out);

        // extension class
        String extensionClass = scriptConfig.getExtensionClass();
        if (extensionClass.isEmpty()) {
            writer.writeFunctionDecl(EXTENSION_OBJ_FACTORY, "    return None",
                                     "connectionMgr");
        } else {
            writer.writeFunctionDecl(EXTENSION_OBJ_FACTORY,
                                     "    # IMPORTANT: If the next call fails, modify your extension object to\n"
                                     + "    # accept CMulticoreConnectionMgr as parameter in ctor: __init__(self, mccMgr)\n"
                                     + "    return " + extensionClass + "(connectionMgr)\n",
                                     "connectionMgr");
        }

        // monitor
        if (dlg.isUseProgressMonitor()) {
            out.write("progressMonitor = " + dlg.getMonitorClassName() + "()");
            out.newLine();
        } else {
            writer.writeAssignment("progressMonitor", null);
        }

        out.newLine();
        reportCfgScope = "ic.CTestReportConfig.";
//            break;
//        case EPerl:
//            writer = new PerlWriter(out);
//            reportCfgScope = "$isystemConnect::CTestReportConfig::";
//            break;
//        default:
//            throw new SIllegalStateException("Unsupported language for script generation: " + dlg.getLanguage());
//        }
        
        String testSpecFile = null;
        if (dlg.isUseCustomTestSpecFile()) {
            testSpecFile = dlg.getTestSpecificationFile();
        } else {
            testSpecFile = model.getModelFileName();
            // create absolute path to iyaml file, since if it is relative, 
            // it is relative to testIDEA working dir, which is in most cases not
            // a good idea, because it is where testIDEA exe resides.
            testSpecFile = new File(testSpecFile).getAbsolutePath();
        }
        
        if (testSpecFile == null  ||  testSpecFile.isEmpty()) {
            throw new SIllegalArgumentException("Name of file with test specifications is not defined.\n" +
                "Please perform save in testIDEA, or specify custom test spec. file in the 'Generate Test Script' dialog.");
        }
        writer.writeAssignment("testSpecificationFile", testSpecFile);
        
        out.newLine();
        writer.writeAssignment("isUseCustomWinIDEAWorkspace", dlg.isUseCustomWinIDEAWorkspace());
        writer.writeAssignment("winIDEAWorkspace", dlg.getWinIDEAWorkspace());

        // initialization
        writer.writeAssignment("isUseCustomInitSequence", dlg.isUseCustomInitSeq());
        
        CTestEnvironmentConfig initConfig = dlg.getRunConfiguration();
        writer.writeAssignment("initBeforeTest", initConfig.isAlwaysRunInitSeqBeforeRun());
        
        writer.writeValueAssignment("customInitActions", "[]");
        CTestBaseList initActions = initConfig.getTestBaseList(CTestEnvironmentConfig.EEnvConfigSections.E_SECTION_INIT_SEQUENCE.swigValue(), 
                                                 true);
        int numActions = (int) initActions.size();
        
        for (int idx = 0; idx < numActions; idx++) {
        
            CInitSequenceAction action = CInitSequenceAction.cast(initActions.get(idx));
            List<String> params = DataUtils.getSeqAsList(action, 
                CInitSequenceAction.EInitSequenceSectionIds.E_INIT_SEQ_PARAMS.swigValue());
            
            writer.writeFunctionCall(null, 
                                     "addCustomInitAction",
                                     "customInitActions", 
                                     "'" + action.getCoreId() + "'", 
                                     "ic.CInitSequenceAction." + action.getAction().toString(),
                                     '[' + DataUtils.listToQuotedStrings(params) + ']');
        }

        List<String> coreIds = 
                DataUtils.getSeqAsList(initConfig, 
                                       EEnvConfigSections.E_SECTION_CORE_IDS.swigValue());
        String strCoreIds = DataUtils.listToQuotedStrings(coreIds);
        writer.writeValueAssignment("coreIDs", '[' + strCoreIds + ']');

        out.newLine();
        
        // filter
        if (dlg.isUseTestFilter()) {
            writer.writeAssignment("filterId", dlg.getFilterId());
        } else { 
            writer.writeAssignment("filterId", null);
        }

        writer.writeAssignment("iconnectDllPath", dlg.getIconnectDllPath());
        
        // report
        boolean isSaveTestReportAsTestIDEA = dlg.isSaveReport();
        writer.writeAssignment("isSaveTestReportAsTestIDEA", isSaveTestReportAsTestIDEA);
        
        boolean isSaveTestReportAsJUnit = dlg.isSaveReportAsJUnit();
        writer.writeAssignment("isSaveTestReportAsJUnit", isSaveTestReportAsJUnit);
        writer.writeAssignment("isExportForCobertura", dlg.isExportForCobertura());
        writer.writeAssignment("exportForCoberturaTrdFName", dlg.getExportForCoberturaFName());

        boolean isUseCustomReportConfig = dlg.isUseCustomReportConfig();
        writer.writeAssignment("isUseCustomReportConfig", isUseCustomReportConfig);
        writer.writeAssignment("isOpenReportInBrowser", dlg.isOpenReportInBrowser());

        CTestReportConfig reportConfig = dlg.getTestReportConfig();

        if (isSaveTestReportAsTestIDEA) {
            if (isUseCustomReportConfig) {
                String reportFileName = reportConfig.getFileName();
                if (reportFileName == null  ||  reportFileName.isEmpty()) {
                    throw new SIllegalArgumentException("Option 'Save test report to file' was " +
                    " selected, but report file name is not defined in custom report configuration.");
                }
            } else {
                String reportFileName = model.getTestReportConfig().getFileName();
                if (reportFileName == null  ||  reportFileName.isEmpty()) {
                    throw new SIllegalArgumentException("Option 'Save test report to file' was " +
                        " selected, but report file name is not defined in testIDEA.\n" +
                        "Please open 'Test | Save Test Report' in testIDEA and enter 'Output File:' there.\n" +
                        "Then save your data in testIDEA (File | Save)!\n" +
                        "Alternatively you can specify report file name in custom report configuration in \n" + 
                        "the 'Generate Test Script' dialog.");
                }
            }
        }
        
        writer.writeFunctionCall("customReportConfig", "setReportContents", 
                                 reportCfgScope + reportConfig.getReportContents());
        
        writer.writeFunctionCall("customReportConfig", "setOutputFormat", 
                                 reportCfgScope + reportConfig.getOutputFormat());

        // file name must be quoted as: ("'file'"), double quotes are for Python, single 
        // ones for YAML, which has to treat file name a string because of ':' and spaces
        // in path
        writer.writeFunctionCall("customReportConfig", "setFileName", "\"'" + reportConfig.getFileName() + "'\"");
        writer.writeFunctionCall("customReportConfig", "setXsltForFullReport",  "\"'" + reportConfig.getXsltForFullReport() + "'\"");
        writer.writeFunctionCall("customReportConfig", "setXsltForErrors",  "\"'" + reportConfig.getXsltForErrors() + "'\"");
        writer.writeFunctionCall("customReportConfig", "setIncludeTestSpec",  reportConfig.isIncludeTestSpec());
        // writer.writeFunctionCall("customReportConfig", "setUseCustomTime",  reportConfig.isUseCustomTime());

        StrVector uinfoKeys = new StrVector();
        CMapAdapter uinfoMap = new CMapAdapter(reportConfig, 
                                               ETestReportConfigSectionIds.E_SECTION_TEST_INFO.swigValue(),
                                               true);
        int numItems = (int) uinfoKeys.size();
        for (int idx = 0; idx < numItems; idx++) {
            String key = uinfoKeys.get(idx);
            String value = uinfoMap.getValue(key);
            addUserInfo(writer, key, value);
        }
//        StrStrMap userInfo = new StrStrMap();
//        reportConfig.getUserInfo(userInfo);
//        addUserInfo(writer, SaveTestReportDialog.TAG_ENV_TESTER, userInfo);
//        addUserInfo(writer, SaveTestReportDialog.TAG_ENV_DATE, userInfo);
//        addUserInfo(writer, SaveTestReportDialog.TAG_ENV_TIME, userInfo);
//        addUserInfo(writer, SaveTestReportDialog.TAG_ENV_SW, userInfo);
//        addUserInfo(writer, SaveTestReportDialog.TAG_ENV_HW, userInfo);
//        addUserInfo(writer, SaveTestReportDialog.TAG_ENV_DESC, userInfo);
        
        out.newLine();
    }


    private void writePythonImports(BufferedWriter out,
                                    GenerateTestScriptDialog dlg,
                                    CScriptConfig scriptConfig) throws IOException {
        StrVector sysPaths = new StrVector();
        scriptConfig.getSysPaths(sysPaths);
        int numPaths = (int)sysPaths.size();
        if (numPaths > 0) {
            out.write("import sys"); out.newLine();
            
            for (int i = 0; i < numPaths; i++) {
                out.write("sys.path.append('" + sysPaths.get(i) + "')"); out.newLine();

            }
        }
        out.newLine();
        
        out.write("# imports specified in the script generation dialog (Tools | Generate Test Script)");
        out.newLine();
        
        String[] imports = dlg.getImports().split(", *");
        for (String module : imports) {
            module = module.trim();
            if (!module.isEmpty()) {
                out.write("import " + module); out.newLine();
            }
        }
        out.newLine();
        
        out.write("# imports specified in the project script configuration (File | Properties)");
        out.newLine();
        
        StrVector configImports = new StrVector();
        scriptConfig.getModules(configImports);
        int numModules = (int)configImports.size();
        for (int i = 0; i < numModules; i++) {
            out.write("import " + configImports.get(i));
            out.newLine();
        }
        
        
        if (dlg.isGenerateCallbackClass()) {
            // TODO
        }
    }

    
    private void addUserInfo(LangaugeWriter writer, String key, String userInfo) throws IOException {
        writer.writeFunctionCall("customReportConfig", "addUserInfo", "'" + key + "'", "\"'" + 
                                 userInfo + "'\"");
    }

    
    abstract class LangaugeWriter {
        
        protected BufferedWriter m_out;
        
        public LangaugeWriter(BufferedWriter out) {
            m_out = out;
        }
        
        abstract void writeAssignment(String varName, 
                                      String value) throws IOException;
        
        abstract void writeValueAssignment(String varName, 
                                           String value) throws IOException;
        
        abstract void writeAssignment(String varName, 
                                      boolean value) throws IOException;
        
        abstract void writeFunctionCall(String object, 
                                        String function, String ... params) throws IOException;

        abstract void writeFunctionCall(String object, 
                                        String function, boolean param) throws IOException;

        abstract String bool2Str(boolean value);
        
        abstract void writeFunctionDecl(String funcName, String body, String ...params) throws IOException;
        
        void writeFunctionParameters(String... params) throws IOException {
            m_out.write('(');
            m_out.write(StringUtils.join(params, ", "));
            m_out.write(')');
        }
    }
    
    
    class PythonWriter extends LangaugeWriter {

        public PythonWriter(BufferedWriter out) {
            super(out);
        }


        @Override
        /** Writes assignments like: counter = r"3" */
        void writeAssignment(String varName, String value) throws IOException {
            if (value == null) {
                m_out.write(varName); m_out.write(" = "); m_out.write("None");
            } else {
                m_out.write(varName); m_out.write(" = r\""); m_out.write(value); m_out.write('"');
            }
            m_out.newLine();
        }

        
        @Override
        /** Writes assignments like: counter = 3 */
        void writeValueAssignment(String varName, String value) throws IOException {
            if (value == null) {
                m_out.write(varName); m_out.write(" = "); m_out.write("None");
            } else {
                m_out.write(varName); m_out.write(" = "); m_out.write(value);
            }
            m_out.newLine();
        }

        
        @Override
        void writeAssignment(String varName, boolean value) throws IOException {
            m_out.write(varName); m_out.write(" = "); m_out.write(bool2Str(value));
            m_out.newLine();
        }

        
        @Override
        String bool2Str(boolean value) {
            return value ? "True" : "False";
        }

        @Override
        void writeFunctionCall(String object, String function,
                               String... params) throws IOException {
            
            if (object != null) {
                m_out.write(object); m_out.write(".");
            }
            m_out.write(function);

            // Prepend all string parameters with 'r', because of '\' in file paths
            // '\' as escape is not allowed.
            for (int i = 0; i < params.length; i++) {
                if (params[i].charAt(0) == '"') {
                    params[i] = 'r' + params[i];
                }
            } 
            
            writeFunctionParameters(params);
            m_out.newLine();
        }
        
        @Override
        void writeFunctionCall(String object, 
                               String function, boolean param) throws IOException {
            m_out.write(object); m_out.write("."); m_out.write(function);
            m_out.write('('); m_out.write(bool2Str(param)); m_out.write(')');
            m_out.newLine();
        }
        
        
        @Override
        void writeFunctionDecl(String funcName, String body, String ...params) throws IOException {
            m_out.write("def " + funcName);
            writeFunctionParameters(params);
            m_out.write(":\n");
            m_out.write(body);
            m_out.newLine();
            m_out.newLine();
        }
    }
    
    
    class PerlWriter extends LangaugeWriter {

        public PerlWriter(BufferedWriter out) {
            super(out);
        }


        @Override
        void writeAssignment(String varName, String value) throws IOException {
            if (value != null) { 
                m_out.write("my $");
                // escape single quotes
                value = value.replace("'", "\\'");
                m_out.write(varName); m_out.write(" = '"); m_out.write(value); m_out.write('\'');
                m_out.write(';');
                m_out.newLine();
            } else {
                // null value
                m_out.write("my $");
                m_out.write(varName); m_out.write(" = undef;");
                m_out.newLine();
            }
        }

        
        @Override
        /** Not implemented! Perl is no longer supported. */
        void writeValueAssignment(String varName, String value) throws IOException {
            throw new SIllegalArgumentException("writeValueAssignment() for Perl not implemented!");
        }
        
        @Override
        void writeAssignment(String varName, boolean value) throws IOException {
            m_out.write("my $");
            m_out.write(varName); m_out.write(" = "); m_out.write(bool2Str(value));
            m_out.write(';');
            m_out.newLine();
        }
        
        
        @Override
        String bool2Str(boolean value) {
            return value ? "1" : "0";
        }

        
        @Override
        void writeFunctionCall(String object, String function,
                               String... params) throws IOException {
            
            m_out.write('$');
            m_out.write(object); m_out.write("->"); m_out.write(function);
            
            // in Perl the only escape sequences in single quoted string are \'
            // and \\. So we'll covnert all ' to \' (for YAML), and " to ' -
            // equivalent to Python raw strings.
            
            for (int i = 0; i < params.length; i++) {
                if (params[i].charAt(0) == '"') { // is it literal string?
                    params[i] = params[i].replace("'", "\\'");
                    // replace double quotes at start and end with single quotes
                    params[i] = '\'' + params[i].substring(1, params[i].length() - 1) + '\'';
                }
            } 
            
            writeFunctionParameters(params);
            m_out.write(';');
            m_out.newLine();
        }
        
        
        @Override
        void writeFunctionCall(String object, 
                               String function, boolean param) throws IOException {
            m_out.write('$');
            m_out.write(object); m_out.write("->"); m_out.write(function);
            m_out.write('('); m_out.write(bool2Str(param)); m_out.write(')');
            m_out.write(';');
            m_out.newLine();
        }


        @Override
        void writeFunctionDecl(String funcName, String body, String... params) throws IOException {
            throw new IllegalStateException("Perl is no longer supported");
        }
}
}
