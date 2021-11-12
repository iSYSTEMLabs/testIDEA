package si.isystem.commons.utils;

import java.lang.reflect.InvocationTargetException;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.utils.LongWinIDEAOperation.OpEnum;
import si.isystem.connect.CExecutionController;
import si.isystem.connect.CLoaderController;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.ui.utils.KGUIBuilder;


/**
 * This dialog pops-up when winIDEA operation fails and there may be recovery
 * possible - either to stop a target or download symbols. It appeared to not 
 * be that helpful as planned - consider removing it.
 *  
 * @author markok
 *
 */
class DownloadDialog extends Dialog {

    private static final int DOWNLOAD_BTN = 100;
    private static final int DOWNLOAD_SYMOLS_BTN = 101;
    private static final int STOP_TARGET_BTN = 102;
    private String m_message;
    private IIConnectOperation m_operation;
    private Exception m_ex;
    private JConnection m_jCon;


    /**
     * 
     * @param parentShell
     * @param message
     * @param ex may be null
     */
    protected DownloadDialog(Shell parentShell, 
                             String message, 
                             Exception ex,
                             JConnection jCon) {
        super(parentShell);
        
        m_message = message;
        m_ex = ex;
        m_jCon = jCon;
    }

    public static boolean open(String message, Exception ex, JConnection jCon) {
        DownloadDialog dlg = new DownloadDialog(ISysUIUtils.getShell(), message, ex, jCon);
        return dlg.open() != Window.CANCEL;
    }
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Download");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        
        mainDlgPanel.setLayout(new MigLayout("fillx", ""));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        builder.label(m_message + "\n\nPossible reasons:\n" +
                      "- symbol is not defined in your project\n" +
                      "- symbols are not downloaded to the taget\n" +
                      "- winIDEA is not in state 'STOP'\n" +
                      "- in multicore configurations connections may not be established to all cores", 
                      "wrap");
        
        builder.label("winIDEA error:\n- " + SEFormatter.getInfo(m_ex), "wrap");
        
        builder.separator("growx, gaptop 20", SWT.HORIZONTAL);

        return composite;
    }
    

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, DOWNLOAD_BTN, "Download",
                true);
        
        createButton(parent, DOWNLOAD_SYMOLS_BTN, "Download Symbols",
                     false);
        
        createButton(parent, STOP_TARGET_BTN, "Stop Target",
                     false);
        
        createButton(parent, IDialogConstants.CANCEL_ID,
                             IDialogConstants.CANCEL_LABEL, false);
                             
    }
    

    @Override
    protected void buttonPressed(int buttonId) {
        
        m_operation = null;
        
        switch (buttonId) {
        case DOWNLOAD_BTN:
            m_operation = new LongWinIDEAOperation(getShell(), OpEnum.Download);
            break;
        case DOWNLOAD_SYMOLS_BTN:
            m_operation = new LongWinIDEAOperation(getShell(), OpEnum.DownloadSymbolsOnly);
            break;
        case STOP_TARGET_BTN:
            m_operation = new LongWinIDEAOperation(getShell(), OpEnum.Stop);
            break;
        case IDialogConstants.CANCEL_ID:
        default:
            m_operation = null;
            break;
        }

        if (m_operation != null) {
            ISysUIUtils.execWinIDEAOperation(m_operation, getShell(), m_jCon);
        }
        
        if (buttonId == IDialogConstants.CANCEL_ID) {
            super.buttonPressed(buttonId);  // close is called here
        } else {
            close();
        }
    }
}


class LongWinIDEAOperation implements IIConnectOperation {
    
    private Exception m_ex;
    private Shell m_shell;
    private OpEnum m_op;
    
    enum OpEnum {Download, DownloadSymbolsOnly, Stop}; 
    
    LongWinIDEAOperation(Shell shell, OpEnum operation) {
        m_shell = shell;
        m_op = operation;
    }
    
    @Override
    public void exec(final JConnection jCon) {
        
        m_ex = null;
        try {
            PlatformUI.getWorkbench().getProgressService()
            .busyCursorWhile(new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {

                    monitor.beginTask("Executing operation in winIDEA...", IProgressMonitor.UNKNOWN);
                    try {
                        CLoaderController loader = new CLoaderController(jCon.getPrimaryCMgr());
                        switch (m_op) {
                        case Download:
                            loader.download();
                            break;
                        case DownloadSymbolsOnly:
                            loader.downloadWithoutCode();
                            break;
                        case Stop:
                            CExecutionController exec = new CExecutionController(jCon.getPrimaryCMgr());
                            exec.stop();
                            break;
                        default:
                            // ignored - user will not see internal failure :-|
                        }
                    } catch (Exception ex) {
                        // can not report ex here, because this is non-gui thread.
                        m_ex = ex;
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (Exception ex) {
            SExceptionDialog.open(m_shell, "winIDEA operation failed!", ex);
        }

        if (m_ex != null) {
            SExceptionDialog.open(m_shell, "Execution of winIDEA operation failed!", m_ex);
        }
    }

    
    @Override
    public void setData(Object data) {}
}
