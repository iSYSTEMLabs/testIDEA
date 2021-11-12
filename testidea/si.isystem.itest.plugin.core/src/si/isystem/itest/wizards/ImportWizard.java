package si.isystem.itest.wizards;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import si.isystem.cte.CteImporter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.xls.CsvImporter;
import si.isystem.itest.xls.TableImporter.EImportScope;
import si.isystem.itest.xls.XlsImporter;

public class ImportWizard extends ExportWizard implements IImportWizard {

    String [] m_warnings;
    private IStructuredSelection m_selection;

    public ImportWizard() {
        super();
        m_isValidateSelectionForExport = false;
    }
    
    
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        initModel();
        m_selection = selection;
    }

    
    @Override
    public String getWindowTitle() {
        return "Import Test Cases";
    }
    
    
    @Override
    public void addPages() {
        m_wizardPage = new ExportImportWizardPage(ExportImportWizardPage.IMPORT_TITLE,
                                                  ExportImportWizardPage.IMPORT_PAGE_DESCRIPTION,
                                                  m_errMessage, false, null);
        addPage(m_wizardPage);
    }
    

    @Override
    public boolean performFinish() {
        
        m_wizardPage.okPressed();
        
        EImportScope importScope = m_wizardPage.getImportScope();

        if (importScope == EImportScope.EToSelectedTestCases) {
            try {
                m_containerTestSpec = getSelection(m_selection);
            } catch (Exception ex) {
                MessageDialog.openError(getShell(), "Import error!", "Can not import to selected test cases, if none are selected!");
                return false;
            }
        } else {
            m_containerTestSpec = m_model.getRootTestSpecification();
        }
        
        if (m_containerTestSpec == null) {
            MessageDialog.openError(Activator.getShell(), "Data error",
                    "No test case is selected!\n" +
                    "Please change import scope or close this dialog and select at least one \n"
                    + "test case to import data into.");
            return false;
        }

        try {
            final String fileName = m_wizardPage.getFileName();
            final String extension = UiUtils.getExtension(fileName).toUpperCase();

            if (extension.equals("XLSX")  ||  extension.equals("XLS")) {
                XlsImporter importer = new XlsImporter();
                m_warnings = importer.importFromFile(m_containerTestSpec, 
                                                     fileName,
                                                     importScope);
            } else if (extension.equals("CTE")) {
                CteImporter cteImporter = new CteImporter();
                cteImporter.importFromFile(m_containerTestSpec, fileName);
            } else {
                CsvImporter csvImporter = new CsvImporter();
                m_warnings = csvImporter.importFromFile(m_containerTestSpec, 
                                                        fileName,
                                                        importScope);
            }

            if (m_warnings != null  &&  m_warnings.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String wrn : m_warnings) {
                    sb.append(wrn).append('\n');
                    // limit the length of warnings to avoid gigantic dialog
                    if (sb.length() > 1000) {
                        sb.append("...\n");
                        break;
                    }
                }
                MessageDialog.openWarning(Activator.getShell(), "Import warnings", sb.toString());
            }
            
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Import failed!", ex);
        } catch (Error err) {
            err.printStackTrace();
            SExceptionDialog.open(Activator.getShell(), "Fatal error!", new Exception(err));
        }
        
        return true;
    }
}


