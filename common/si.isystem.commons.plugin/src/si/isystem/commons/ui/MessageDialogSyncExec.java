package si.isystem.commons.ui;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.ISysTaskExecutor;
import si.isystem.exceptions.SExceptionDialog;

public class MessageDialogSyncExec 
{
    public static void showInfo(final String title, final String message) {
        showInfo(null, title, message);
    }
    
    public static void showInfo(final Shell parent, final String title, final String message) {
        ISysTaskExecutor.syncExecSWT(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(getShell(parent), title, message);
            }
        });
    }

    public static boolean showConfirm(final String title, final String message) {
        return showConfirm(null, title, message);
    }
    
    public static boolean showConfirm(final Shell parent, final String title, final String message) {
        final MutableBoolean okRef = new MutableBoolean(false);
        ISysTaskExecutor.syncExecSWT(new Runnable() {
            @Override
            public void run() {
                okRef.setValue(MessageDialog.openConfirm(getShell(parent), title, message));
            }
        });
        return okRef.getValue();
    }

    public static boolean showQuestion(final String title, final String message) {
        return showQuestion(null, title, message);
    }
    
    public static boolean showQuestion(final Shell parent, final String title, final String message) {
        final MutableBoolean yesRef = new MutableBoolean(false);
        ISysTaskExecutor.syncExecSWT(new Runnable() {
            @Override
            public void run() {
                yesRef.setValue(MessageDialog.openQuestion(getShell(parent), title, message));
            }
        });
        return yesRef.getValue();
    }

    public static void showWarning(final String title, final String message) {
        showWarning(null, title, message);
    }
    
    public static void showWarning(final Shell parent, final String title, final String message) {
        ISysTaskExecutor.syncExecSWT(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openWarning(getShell(parent), title, message);
            }
        });
    }

    public static void showError(final String title, final String message) {
        showError(null, title, message);
    }
    
    public static void showError(final Shell parent, final String title, final String message) {
        ISysTaskExecutor.syncExecSWT(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openError(getShell(parent), title, message);
            }
        });
    }
    
    public static void showError(final String title, final Exception e) {
        showError(null, title, e);
    }

    public static void showError(final Shell parent, final String title, final Exception e) {
        ISysTaskExecutor.syncExecSWT(new Runnable() {
            @Override
            public void run() {
                SExceptionDialog.open(getShell(parent), title, e);
            }
        });
    }
    
    private static Shell getShell(final Shell shell) {
        return shell != null ? shell : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }
}
