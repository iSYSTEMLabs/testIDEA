package si.isystem.itest.handlers;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import si.isystem.commons.utils.ISysFileUtils;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestBench.ETestBenchSectionIds;
import si.isystem.connect.CTestReportConfig;
import si.isystem.connect.CTestReportConfig.EOutputFormat;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestReportStatistic;
import si.isystem.connect.CTestResult;
import si.isystem.connect.EmitterFactory;
import si.isystem.connect.IEmitter;
import si.isystem.connect.StrVector;
import si.isystem.connect.connect;
import si.isystem.connect.utils.OsUtils;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.ReferenceStorage;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.SaveTestReportDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.ExtensionScriptInfo;
import si.isystem.itest.run.Script;
import si.isystem.itest.run.TestScriptResult;
import si.isystem.itest.ui.spec.StatusView;

public class TestSaveTestReportCmdHandler extends AbstractHandler {

    private static final String TEMPLATES_REPORTS_DIR = "templates/reports"; 
    private static String m_winIDEAWorkspaceDir = "";
    private static String m_winIDEAWorkspaceFile = "";
    private static String m_defaultDownloadFile = "";

    public TestSaveTestReportCmdHandler() {
        ReferenceStorage.setSaveReportCmdHandler(this);
    }

    
    // Sets values used for saving, so that report can be saved even if winIDEA
    // crashed.
    public static void setLastTestRunEnv(String winIDEAWorkspaceDir,
                                         String winIDEAWorkspaceFile,
                                         String defaultDownloadFile) {
        m_winIDEAWorkspaceDir = winIDEAWorkspaceDir;
        m_winIDEAWorkspaceFile = winIDEAWorkspaceFile;
        m_defaultDownloadFile = defaultDownloadFile;
    }

    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        Shell shell = Activator.getShell();

        try {
            
            TestSpecificationModel model = TestSpecificationModel.getActiveModel();
            
            String modelFileName = model.getModelFileName(); 
            if (modelFileName == null  ||  modelFileName.isEmpty()) {
                throw new SIllegalArgumentException("Report should contain name of 'iyaml' file. "
                        + "Please save test cases first!");
            }

            
            CTestReportConfig modelReportConfig = model.getTestReportConfig();

            CTestReportConfig tmpReportConfig = new CTestReportConfig();
            tmpReportConfig.assign(modelReportConfig);
            
            execGetReportCustomDataScript(tmpReportConfig,
                                          model.getTestEnvConfig().getScriptConfig(true),
                                          modelFileName);
            
            SaveTestReportDialog dlg = new SaveTestReportDialog(shell, 
                                                                "Save Test Report",
                                                                tmpReportConfig);
            
            if (dlg.show()) {
                
                boolean isOpenReportBrowser = dlg.isOpenBrowserAfterSave();
                
                CTestReportConfig newReportConfig = dlg.getTestReportConfig();
                
                tmpReportConfig.assign(newReportConfig);
                tmpReportConfig.clearDynamicUserItems();                
                
                if (!modelReportConfig.equalsData(tmpReportConfig)) {
                    modelReportConfig.assign(tmpReportConfig);
                    model.setModelDirty(true);
                }

                saveTestReport(isOpenReportBrowser,
                               model.getTestReportContainer(),
                               newReportConfig,
                               modelFileName);
                
                execAfterReportSaveScript(newReportConfig, 
                                          model.getTestEnvConfig().getScriptConfig(true), 
                                          modelFileName);
            }
        } catch (Exception ex) {
            SExceptionDialog.open(shell, "Can not save test report!", ex);
        }

        return null;
    }

    
    public void saveTestReportWithScripts(boolean isOpenReportBrowser,
                                          CTestReportContainer results,
                                          CTestReportConfig reportConfig, // gets modified during saving!
                                          CScriptConfig scriptConfig,
                                          String modelFileName) 
                                                  throws IOException, SaxonApiException {

        execGetReportCustomDataScript(reportConfig,
                                      scriptConfig,
                                      modelFileName);
        
        saveTestReport(isOpenReportBrowser, results, reportConfig, 
                       modelFileName);
        
        execAfterReportSaveScript(reportConfig, scriptConfig, modelFileName);
    }

    
    private void saveTestReport(boolean isOpenReportBrowser,
                                CTestReportContainer results,
                                CTestReportConfig reportConfig, // gets modified during saving!
                                String modelFileName) throws IOException, SaxonApiException {
        
        // this info is used when creating link to profiler output file 
        String winIDEAPath = m_winIDEAWorkspaceDir.trim();
        if (winIDEAPath.endsWith("\\")) {
            // remove the trailing path separator
            winIDEAPath = winIDEAPath.substring(0, winIDEAPath.length() - 1);
        }
        
        CTestBench.addUserInfoToReportConfig(reportConfig, modelFileName, 
                                             winIDEAPath, m_winIDEAWorkspaceFile,
                                             m_defaultDownloadFile);

        EOutputFormat outputFormat = reportConfig.getOutputFormat();
        String outFName = reportConfig.getAbsReportFileName();
        String htmlFName = UiUtils.replaceExtension(outFName, "html");
        
        switch (outputFormat) {
        case FMT_XML:
            CTestBench.createDirIfNotExists(reportConfig.getAbsReportFileName());
            String xsltFileName = copyXSLTAndCss(reportConfig, null, true);
            CTestBench.saveXML(results, reportConfig);
            
            if (reportConfig.isCreateHtml()) {
                createHTML(xsltFileName, outFName, htmlFName);
            }
            break;
        case FMT_YAML:
            CTestBench.saveYAML(results, reportConfig);
            break;
        case FMT_CSV:
            CTestBench.saveCSV(results, reportConfig);
            break;
        case FMT_XLS: {
                Workbook wb = new HSSFWorkbook();
                saveXLS(wb, reportConfig, results);
            } break;
        case FMT_XLSX: {
                Workbook wb = new XSSFWorkbook();
                saveXLS(wb, reportConfig, results);
            } break;
        default:
            throw new SIllegalArgumentException("Unknown format for test report!")
            .add("format", outputFormat);
        }
            
        if (isOpenReportBrowser) {
            try {
                File file = outputFormat == EOutputFormat.FMT_XML  &&  reportConfig.isCreateHtml() ?
                        new File(htmlFName) :
                        new File(outFName);
                        
                // Because and function in Desktop class crashes testIDEA on Linux, 
                // open report only on Windows. https://bugs.openjdk.java.net/browse/JDK-8184155
                // says it will be fixed in Java 9.
                if (OsUtils.isWindows()  &&  Desktop.isDesktopSupported()) {
                	Desktop desktop = Desktop.getDesktop();
                	desktop.browse(file.toURI());
                } else {
                	String fName = file.getAbsolutePath();
                    if (OsUtils.isLinux()) {
                    	try {
                    		if (OsUtils.startProcess("kde-open", fName)) {
                    			return;
                    		}
                    	} catch (Exception ex) {
                    		try {
                    			if (OsUtils.startProcess("gnome-open", fName)) {
                    				return;
                    			}
                    		} catch (Exception ex1) {
                    			try {
                    				if (OsUtils.startProcess("xdg-open", fName)) {
                    					return;
                    				}
                    			} catch (Exception ex2) {
                                	MessageDialog.openWarning(Activator.getShell(), "Error opening report!", 
                                			"Java on this platform does not support starting default browser.\n"
                                			+ "Open report manually from file browser.");
                    			}
                    		}
                    	}
                    } else {

                    	// if (os.isMac()) {
                    	//     if (runCommand("open", "%s", what)) return true;
                    	// }

                    	if (OsUtils.isWindows()) {
                    		try {
                    			if (OsUtils.startProcess("explorer", fName)) {
                    				return;
                    			}
                    		} catch (Exception ex2) {
                    			MessageDialog.openWarning(Activator.getShell(), "Error opening report!", 
                    					"Java on this platform does not support starting default browser.\n"
                    							+ "Open report manually from file browser.");
                    		}
                    	}
                    }
                }
                
            } catch (Exception ex) {
                // must throw exception here, not open a dialog, because this
                // method is also called from non-ui thread when iyaml file is run from 
                // context menu 'Run As | testIDEA launch'
                // in Eclipse Project explorer (testIDEA running as Eclipse plug-in).
                throw new SIOException("Can not launch system viewer for file '" + 
                        outFName + "'!\n" +
                        "Check the file extension!", ex);
            }
        }
    }


    /**
     * Runs PYthon script to get custom data for test report.
     * @param reportConfig
     */
    private void execGetReportCustomDataScript(CTestReportConfig reportConfig, 
                                               CScriptConfig scriptConfig, 
                                               String modelFileName) {
        try {
            if (scriptConfig.getExtensionClass().isEmpty()) {
                return; // Not configured, do not write annoying warnings to users who
                        // do not use scripts at all.
            }
            Script script = UiUtils.initScript(scriptConfig, modelFileName);
            ExtensionScriptInfo extensionInfo = script.getExtScriptInfo();
            if (extensionInfo.isGetTestReportCustomDataImplemented()) {
                TestScriptResult result = script.callFunction(null, 
                                    CScriptConfig.getEXT_METHOD_TYPE(), 
                                    CScriptConfig.getEXT_METHOD_GET_TEST_REPORT_CUSTOM_DATA(), 
                                    // use multiline string quotes to avoid problems because of quotes in
                                    // report config data, and raw string because of '\' in file names
                                    new String[]{"r\"\"\"" + reportConfig.toString() + "\"\"\""});

                if (result.isError()) {
                    throw new SIllegalStateException("Error when running script to get custom data for test report!").
                        add("desc", result.toUIString());
                }

                StatusModel.instance().appendDetailPaneText(StatusType.INFO, result.toUIString());

                for (String line : result.getFuncInfo()) {
                    
                    if (!line.trim().isEmpty()) { // empty lines are ignored
                        
                        int idx = line.indexOf(':');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String value = line.substring(idx + 1).trim();
                            reportConfig.addUserInfo(key, value);
                        } else {
                            StatusModel.instance().appendDetailPaneText(StatusType.INFO, 
                                    "ERROR: Invalid line in data returned from '"
                                            + CScriptConfig.getEXT_METHOD_GET_TEST_REPORT_CUSTOM_DATA() 
                                            + "'. Missing separator ':' between key : value pair.\n"
                                            + "Line: " + line);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            String msg = "Can not run script extension function to get test report custom data: '" 
                         + CScriptConfig.getEXT_METHOD_GET_TEST_REPORT_CUSTOM_DATA() + "()'\n"
                         + ex.getMessage();
            reportError(msg);
        }
    }


    private void execAfterReportSaveScript(CTestReportConfig reportConfig, 
                                           CScriptConfig scriptConfig, 
                                           String modelFileName) {
        
        try {
            if (scriptConfig.getExtensionClass().isEmpty()) {
                return; // Not configured, do not write annoying warnings to users who
                        // do not use scripts at all.
            }
            
            Script script = UiUtils.initScript(scriptConfig, modelFileName);
            ExtensionScriptInfo extensionInfo = script.getExtScriptInfo();
            if (extensionInfo.isAfterTestImplemented()) {
                TestScriptResult result = script.callFunction(null, 
                                    CScriptConfig.getEXT_METHOD_TYPE(), 
                                    CScriptConfig.getEXT_METHOD_AFTER_REPORT_SAVE(), 
                                    // use multiline string quotes to avoid problems because of quotes in
                                    // report config data, and raw string because of '\' in file names
                                    new String[]{"r\"\"\"" + reportConfig.toString() + "\"\"\""});

                if (result.isError()) {
                    throw new SIllegalStateException("Error when running script after test report saving!").
                    add("desc", result.toUIString());
                }

                StatusModel.instance().appendDetailPaneText(StatusType.INFO, result.toUIString());

            }
        } catch (Exception ex) {
            String msg = "Can not run script after test report saving: '"
                    + CScriptConfig.getEXT_METHOD_AFTER_REPORT_SAVE() + "()'\n"
                    + ex.getMessage();
            reportError(msg);
        }
    }


    private void reportError(String msg) {
        // msg dialog is too annoying, because it appears for empty projects
        // or project which do not use scripts.
        StatusView view = StatusView.getView();
        // view can be null when called from non-UI thread from Eclipse Launcher.
        if (view != null) {
            view.setDetailPaneText(StatusType.WARNING, msg);
        } else {
            StatusModel.instance().appendDetailPaneTextFromNonUIThread(StatusType.WARNING,
                                                                       msg);
        }
    }

    
    /**
     * Saves test report to Excel file.
     * 
     * @param reportConfig
     * @param results
     * @throws IOException 
     */
    public void saveXLS(Workbook wb, CTestReportConfig reportConfig,
                        CTestReportContainer results) throws IOException {
        
        // write configuration
        Sheet configSheet = wb.createSheet("Configuration");

        // parameters don't really care in this case - they are not used by C++ class
        IEmitter emitter = EmitterFactory.createCSVEmitter(null, "", false);
        emitter.startStream();
        emitter.startDocument(false);
        emitter.mapStart();
        
        CTestBench tb = new CTestBench();
        String reportConfigTag = 
                tb.getTagName(ETestBenchSectionIds.E_SECTION_REPORT_CONFIG.swigValue());
        emitter.writeString(reportConfigTag);
        reportConfig.serialize(emitter);
        emitter.mapEnd();
        emitter.endDocument(false);
        emitter.endStream();

        header2XLSTable(wb, configSheet, emitter, reportConfig.isXLSVerticalHeader());
        data2XLSTable(configSheet, emitter);
        
        // write statistics of test results
        CTestReportStatistic reportStats = new CTestReportStatistic();
        results.resetTestResultIterator();
        while (results.hasNextTestResult()) {
            CTestResult testResult = results.nextTestResult();
            reportStats.analyzeResult(testResult);
        }
        
        emitter = EmitterFactory.createCSVEmitter(null, "", false);
        emitter.startStream();
        emitter.startDocument(false);
        reportStats.serialize(emitter);
        emitter.endDocument(false);
        emitter.endStream();
        
        Sheet statsSheet = wb.createSheet("Statistics");
        header2XLSTable(wb, statsSheet, emitter, reportConfig.isXLSVerticalHeader());
        data2XLSTable(statsSheet, emitter);

        // write results
        emitter = EmitterFactory.createCSVEmitter(null, "", false);
        emitter.startStream();
        
        results.resetTestResultIterator();
        while (results.hasNextTestResult()) {
            CTestResult testResult = results.nextTestResult();
            emitter.startDocument(false);
            testResult.serialize(emitter, reportConfig);
            emitter.endDocument(false);
        }
        emitter.endStream();
        
        Sheet resultsSheet = wb.createSheet("Results");
        header2XLSTable(wb, resultsSheet, emitter, reportConfig.isXLSVerticalHeader());
        data2XLSTable(resultsSheet, emitter);

        
        // Write the output to a file
        String reportFileName = reportConfig.getAbsReportFileName();
        
        try (FileOutputStream fileOut = new FileOutputStream(reportFileName)) {
            wb.write(fileOut);
        }
    }


    private void data2XLSTable(Sheet sheet, IEmitter emitter) {
        int numRows = emitter.getLineNumber();
        for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
            StrVector row = new StrVector();
            emitter.getRow(row, rowIdx);

            Row xlsRow = sheet.createRow(rowIdx + 1); // row 0 is header

            StrVector header = new StrVector();
            emitter.getRow(header, rowIdx);
            int numColumns = (int)header.size();
            for (int colIdx = 0; colIdx < numColumns; colIdx++) {
                Cell cell = xlsRow.createCell(colIdx);
                String cellValue = header.get(colIdx);
                try {
                    double dblVal = Double.parseDouble(cellValue);
                    cell.setCellValue(dblVal);
                } catch (NumberFormatException ex) {
                    cell.setCellValue(cellValue);
                }
            }

        }
    }


    private void header2XLSTable(Workbook wb, Sheet sheet, IEmitter emitter, 
                                 boolean isXLSVerticalHeader) {
        
        Row headerRow = sheet.createRow(0);

        StrVector header = new StrVector();
        emitter.getRow(header, -1);
        int numColumns = (int)header.size();
        for (int i = 0; i < numColumns; i++) {
            Cell cell = headerRow.createCell(i);
            CellStyle style = createHeaderCellStyle(wb, isXLSVerticalHeader);
            cell.setCellStyle(style);
            cell.setCellValue(header.get(i));
        }
    }


    private CellStyle createHeaderCellStyle(Workbook wb, boolean isXLSVerticalHeader) {
        CellStyle style = wb.createCellStyle();

        Font font = wb.createFont();
        // font.setFontHeightInPoints((short)12);
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        // font.setFontName("Courier New");
        // font.setStrikeout(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);

        style.setBorderBottom((short)1);
        
        if (isXLSVerticalHeader) {
            style.setAlignment(CellStyle.ALIGN_CENTER);
            style.setRotation((short)90);
        } else {
            style.setAlignment(CellStyle.ALIGN_RIGHT);
        }
        
        return style;
    }


    @Override
    public boolean isEnabled() {
        boolean isEnabled = TestSpecificationModel.getActiveModel().getTestReportContainer().getNoOfTestResults() > 0;
        return isEnabled;
    }

    
    @Override
    public boolean isHandled() {
        return isEnabled();
    }

    public void fireEnabledStateChangedEvent() {
        fireHandlerChanged(new HandlerEvent(this, true, true));
    }

    
    public static String copyXSLTAndCss(CTestReportConfig reportConfig, 
                                        File outFolder, 
                                        boolean isModal) {
        try {   
            String rawXsltFileName = reportConfig.getSelectedXsltFileName();
            boolean isEmbeddedXsltAndCss = reportConfig.isEmbedXsltCss();
            final String prefix = connect.getBUILT_IN_XSLT_PREFIX();
                        
            if (rawXsltFileName.startsWith(prefix)) {
                
                String fileName = getFileFromRawFileName(rawXsltFileName);
                rawXsltFileName =
                    copyResource2ReportDir(outFolder, 
                                           reportConfig.getAbsReportFileName(), 
                                           fileName,
                                           isEmbeddedXsltAndCss);
            }
            
            
            String rawCssFileName = reportConfig.getCssFile();
            
            if (rawCssFileName.startsWith(prefix)) {
                
                String fileName = getFileFromRawFileName(rawCssFileName);
                copyResource2ReportDir(outFolder, 
                                       reportConfig.getAbsReportFileName(), 
                                       fileName,
                                       isEmbeddedXsltAndCss);
            }
            
            return rawXsltFileName;
        } catch (IOException ex) {
            
            String msg = "Can not copy XSLT (XML style sheet template) file '" + 
                         reportConfig.getSelectedXsltFileName() + 
                         "' to '" + 
                         reportConfig.getAbsReportFileName() + "'!";
            
            if (isModal) {
                Shell shell = Activator.getShell();
                SExceptionDialog.open(shell, msg, ex);
            } else {
                throw new SIOException(msg, ex);
            }
        }
        
        return null;
    }


    public static void copyXSLTForCobertura(String reportFileName, File outFolder) {

        // currently disabled, as it is better to provide xslt with Python distro.
        // This way there is one file less for each report. 
//        String xsltFileName = connect.getCVRG_TO_COBERTURA_XSLT_FNAME();
//        
//        try {   
//            copyResource2ReportDir(outFolder, reportFileName, xsltFileName, false);
//        } catch (IOException ex) {
//            
//            String msg = "Can not copy XSLT (XML style sheet template) file '" + 
//                         xsltFileName + 
//                         "' to report dir = '" + 
//                         reportFileName + "', outFolder = '" + outFolder + "'!";
//            
//            Shell shell = Activator.getShell();
//            SExceptionDialog.open(shell, msg, ex);
//        }
    }

    
    /** removes XSLT built-in prefix from file name, if specified. */
    private static String getFileFromRawFileName(String rawXsltFileName) {
        
        String xsltFileName = rawXsltFileName; 
        final String prefix = connect.getBUILT_IN_XSLT_PREFIX();
        
        if (rawXsltFileName.startsWith(prefix)) {
            xsltFileName = rawXsltFileName.substring(prefix.length()).trim();
        }
        return xsltFileName;
    }

    
    /**
     * @param outFolder output folder for XSLT and CSS files. If null, the same
     *                  folder is used as for report.
     * @param reportFileName used to get path where to copy XSLT and CSS to, 
     *                       when outFolder == null
     * @param srcFileName name of file to copy, without path
     * @param isEmbeddedXsltAndCss 
     * 
     * @return absolute path of destination file
     * 
     * @throws IOException
     */
    private static String copyResource2ReportDir(File outFolder, 
                                               String reportFileName, 
                                               String srcFileName, 
                                               boolean isEmbeddedXsltAndCss) throws IOException {
        
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        URL uri = bundle.getEntry(TEMPLATES_REPORTS_DIR + '/' + srcFileName);
        if (uri == null) {
            throw new IllegalArgumentException("Template file not found: '" + srcFileName + "'");
        }
        
        File parentFolder;
        if (outFolder == null) {
            File reportFile = new File(reportFileName);
            // the next line is required when user enters file name without path, 
            // for example: 'myReport.xml'
            // reportFile.getParentFile() returns null in such case.
            File reportFileWithAbsPath = new File(reportFile.getAbsolutePath());
            parentFolder = reportFileWithAbsPath.getParentFile();
        } else {
            parentFolder = outFolder;
        }
        
        String destFile = parentFolder.getAbsolutePath() + '/' + srcFileName;
        if (isEmbeddedXsltAndCss) {
            destFile += connect.getTMP_XSLT_CSS_EXTENSION();
        }
        
        ISysFileUtils.copyFileFromPlugin(uri, destFile);
        
        return destFile;
    }
    
    
    /**
     * This method uses Saxon as processor. I've decided for Saxon vs Xalan, 
     * because Saxon supports XSLT2.0.
     * @param xsltPath
     * @param xmlPath
     * @param htmlPath
     * @throws SaxonApiException
     */
    private void createHTML(String xsltPath, String xmlPath, String htmlPath) throws SaxonApiException {
        
        Processor proc = new Processor(false);
        XdmNode source = proc.newDocumentBuilder().build(new StreamSource(new File(xmlPath)));
        Serializer out = proc.newSerializer();
        out.setOutputProperty(Serializer.Property.METHOD, "html");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputFile(new File(htmlPath));
        
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable xslt = comp.compile(new StreamSource(new File(xsltPath)));
        XsltTransformer trans = xslt.load();
        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();
    }

    
/*    private void generateHTMLBuiltIn(String xsltPath, String xmlPath, String htmlPath) 
                                     throws TransformerException {
        
        File xmlFile = new File(xmlPath);
        File xsltFile = new File(xsltPath);
        File htmlFile = new File(htmlPath);
        javax.xml.transform.Source xmlSource =
        new javax.xml.transform.stream.StreamSource(xmlFile);
        javax.xml.transform.Source xsltSource =
                new javax.xml.transform.stream.StreamSource(xsltFile);
        javax.xml.transform.Result result =
                new javax.xml.transform.stream.StreamResult(htmlFile);

        // create an instance of TransformerFactory
        javax.xml.transform.TransformerFactory transFact =
                javax.xml.transform.TransformerFactory.newInstance( );

        javax.xml.transform.Transformer trans =
                transFact.newTransformer(xsltSource);

        trans.transform(xmlSource, result);
    }
    */
}
