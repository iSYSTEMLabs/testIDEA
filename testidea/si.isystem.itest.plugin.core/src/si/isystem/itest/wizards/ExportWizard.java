package si.isystem.itest.wizards;

import java.awt.Desktop;
import java.io.File;
import java.util.TreeSet;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import si.isystem.connect.CTestSpecification;
import si.isystem.cte.CteExporter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.ExportImportUIControls;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.xls.CsvExporter;
import si.isystem.itest.xls.TableImporter.EImportScope;
import si.isystem.itest.xls.XLSExportLookAndFeel;
import si.isystem.itest.xls.XlsExporter;

public class ExportWizard extends Wizard implements IExportWizard {

    protected ExportImportWizardPage m_wizardPage;
    protected CTestSpecification m_containerTestSpec;
    protected String m_errMessage;
    protected boolean m_isValidateSelectionForExport = true;
    protected TestSpecificationModel m_model;

    public ExportWizard() {
        setNeedsProgressMonitor(true);
    }


    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {

        if (initModel() == null) {
            return;
        }
        
        try {
            m_containerTestSpec = getSelection(selection);
        } catch (SIllegalArgumentException ex) {
            m_errMessage = ex.getMessage();
            ex.printStackTrace();
        } catch (Exception ex) {
            m_errMessage = ex.getMessage();
            Shell shell = workbench.getDisplay().getActiveShell();
            SExceptionDialog.open(shell, "Unexpecected error!", ex);
        }
    }


    public TestSpecificationModel initModel() {
        m_model = TestSpecificationModel.getActiveModel();
        
        if (m_model == null) {
            m_errMessage = "Please select editor with testIDEA iyaml file opened!";
        }
        return m_model;
    }


    @Override
    public void addPages() {
        
        String testCaseId = "";
        if (m_containerTestSpec != null  &&  m_containerTestSpec.hasChildren()) {
            testCaseId = m_containerTestSpec.getDerivedTestSpec(0).getTestId();
        }
        
        m_wizardPage = new ExportImportWizardPage(ExportImportWizardPage.EXPORT_TITLE,
                                                  ExportImportWizardPage.EXPORT_PAGE_DESCRIPTION,
                                                  m_errMessage, true,
                                                  testCaseId);
        addPage(m_wizardPage);
    }
    
    
    @Override
    public boolean performFinish() {
        
        m_wizardPage.okPressed();
        
        String outFileName = m_wizardPage.getFileName();
        XLSExportLookAndFeel xlsLookAndFeel = m_wizardPage.getXlsLookAndFeel();
        boolean isOpenDefault = m_wizardPage.isOpenDefault();

        boolean res = exportSelection(m_wizardPage.getShell(),
                                      outFileName,
                                      xlsLookAndFeel,
                                      isOpenDefault);
        
        return res;
    }

    
    @Override
    public String getWindowTitle() {
        return "Export Test Cases";
    }
    
    
    protected CTestSpecification getSelection(IStructuredSelection selection) {
        
        if (selection == null  ||  selection.isEmpty()) {
            throw new SIllegalArgumentException("Nothing selected!\n" + 
                                                "Please, select at least one test specification.");
        }

        CTestSpecification containerTestSpec = UiUtils.getSelectedTestSpecifications(selection);
        
        if (m_isValidateSelectionForExport ) {
            if (containerTestSpec != null  &&  containerTestSpec.getNoOfDerivedSpecs() > 0) {
                verifyTestIds(containerTestSpec);
            } else {
                throw new SIllegalArgumentException("Nothing selected!\n" + 
                        "Please, select at least one test specification in Outline view.");
            }
        }
        
        return containerTestSpec;
    }


    private static void verifyTestIds(CTestSpecification testSpec) {
        TreeSet<String> idSet = new TreeSet<String>();
        for (int i = 0; i < testSpec.getNoOfDerivedSpecs(); i++) {
            CTestSpecification selectedTS = testSpec.getDerivedTestSpec(i);

            String testId = selectedTS.getTestId();
            if (testId.isEmpty()) {
                throw new SIllegalArgumentException("One of test specification does not have " +
                        "test ID assigned. Please assign all test IDs.")
                .add("function", selectedTS.getFunctionUnderTest(true).getName());
            }
            if (idSet.contains(testId)) {
                throw new SIllegalArgumentException("Test specifications with the same IDs are not allowed. Please assign different test IDs.")
                .add("function", selectedTS.getFunctionUnderTest(true).getName())
                .add("testID", testId);
            }
            idSet.add(testId);
        }
    }
    

    private boolean exportSelection(Shell shell,
                                    String outFileName,
                                    XLSExportLookAndFeel xlsLookAndFeel,
                                    boolean isOpenDefault) {

        if (!UiUtils.checkForFileOverwrite(shell, outFileName)) {
            return false;
        }

        try {

            // EExportFormats exportFormat = ExportDialog.getSelectedFormat();
            String extension = UiUtils.getExtension(outFileName).toUpperCase();

            if (extension.equals("XLSX")) {
                XlsExporter exporter = new XlsExporter();
                exporter.exportXLSX(m_containerTestSpec, outFileName, xlsLookAndFeel);
            } else if (extension.equals("XLS")) {
                XlsExporter exporter = new XlsExporter();
                exporter.exportXLS(m_containerTestSpec, outFileName, xlsLookAndFeel);
            } else if (extension.equals("CTE")) {
                CteExporter exporter = new CteExporter();
                exporter.export(m_containerTestSpec, 
                                outFileName);
            } else {
                CsvExporter csvExporter = new CsvExporter();
                csvExporter.export(m_containerTestSpec, outFileName, xlsLookAndFeel);
            }

        } catch (Exception ex) {
            SExceptionDialog.open(shell, "Export failed!", ex);
            return false;
        }

        if (isOpenDefault) {
            try {
                File file = new File(outFileName);

                Desktop desktop = Desktop.getDesktop();
                desktop.browse(file.toURI());
            } catch (Exception ex) {
                SExceptionDialog.open(shell, "Can not launch system application for file '" + 
                        outFileName + "'!\n" +
                        "Check the file extension!", ex);
                return true; // export has been done, ignore failed launching
            }
        }

        return true;
    }
}


/**
 * The main functionality of this page is implemented in ExportImportUIControls,
 * which is common with Import wizard. 
 * @author markok
 *
 */
class ExportImportWizardPage extends WizardPage {

    static final String EXPORT_TITLE = "Export testIDEA test cases";
    static final String EXPORT_PAGE_DESCRIPTION = "Export test cases for use in another application.";
    
    static final String IMPORT_TITLE = "Import testIDEA test cases";
    static final String IMPORT_PAGE_DESCRIPTION = "Import test cases from another application.";
    
    private ExportImportUIControls m_uiPage;
    private String m_errMessage;

    protected ExportImportWizardPage(String title, 
                                     String description,
                                     String errMessage, 
                                     boolean isExportPage,
                                     String testCaseId) {
        super(title);
        setTitle(title);
        setDescription(description);
        m_uiPage = new ExportImportUIControls(isExportPage, testCaseId);
        m_errMessage = errMessage;
    }

    
    @Override
    public void createControl(Composite parent) {

        if (m_errMessage != null) {
            setErrorMessage(m_errMessage);
            createEmptyPanel(parent);
            setPageComplete(false);
            return;
        }

        Composite composite = m_uiPage.createControl(parent, this);
        setControl(composite);  // this call is essential!
        setPageComplete(m_uiPage.isContentValid());
    }


    private void createEmptyPanel(Composite parent) {
        Composite mainDlgPanel = new Composite(parent, SWT.NONE);
        setControl(mainDlgPanel);  // this call is essential!
    }
    
    
    public void okPressed() {
        m_uiPage.okPressed();
    }
    
    
    public boolean isOpenDefault() {
        return m_uiPage.isOpenDefault();
    }


    public XLSExportLookAndFeel getXlsLookAndFeel() {
        return m_uiPage.getXlsLookAndFeel();
    }


    public String getFileName() {
        return m_uiPage.getFileName();
    }


    public EImportScope getImportScope() {
        return m_uiPage.getImportScope();
    }
}
