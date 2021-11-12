
package si.isystem.exceptions;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.KGUIBuilder;


/**
 * This class implements dialog for displaying of error messages when SException
 * is thrown. 
 * <p>
 * 
 * Example:
 * 
 * <pre>
 * // Show error messages also in Eclipse console - they contain links to sources.
 * // Put this line to application startup code. 
 * SExceptionDialog.setPrintToStdOut(true);
 *
 * ...
 * 
 * try {
 *     ...
 * } catch (SException ex) {
 *     SExceptionDialog.open(shell, "Can not do it!", ex) == 1);
 * }
 * </pre>
 *
 * @author markok
 *
 */
public class SExceptionDialog extends Dialog {

    private Exception m_sException;
    private String m_msg;
    private StyledText m_errText;
    private int m_stackLevelIdx = 5;
    private static boolean s_isPrintToStdOut;

    public static final int DETAILS = 100;
    /**
     * Creates dialog. Use static method <code>open()</code> instead of this
     * ctor.
     * 
     * @param parentShell parent shell
     * @param msg the main error message shown in the upper region of the dialog
     * @param ex exception with error info 
     */
    public SExceptionDialog(Shell parentShell, String msg, Exception ex) {
        super(parentShell);
        
        if (msg == null) { // this happens for example with NullPointerException
            msg = "/";
        }
        
        // make dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_msg = msg;
        m_sException = ex;
    }

    
    /**
     * Factory method for opening the error dialog.
     * 
     * @param shell parent shell
     * @param msg the main error message shown in the upper region of the dialog
     * @param ex exception with error info 
     * @return currently this method always returns Window.OK
     */
    public static int open(Shell shell, String msg, Exception ex) {
        SExceptionDialog dlg = new SExceptionDialog(shell, msg, ex);
        return dlg.open();
    }
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Error");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fill", "", "[min!][fill]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
        builder.systemIconLabel(SWT.ICON_ERROR, "gapright 20");
        
        Label errMsgLbl = builder.label(m_msg, "growx, pad 0 0 2 0, wrap", SWT.BORDER);
        errMsgLbl.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_RED));

        m_errText = new StyledText(mainDlgPanel, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        m_errText.setLayoutData("cell 1 1, wmin 350, hmin 200, growx, growy, wrap");
        m_errText.setText(SEFormatter.getInfo(m_sException)); // ex2Yaml() provides not very readable info
        m_errText.setEditable(false);

        builder.separator("span, growx, gaptop 20", SWT.HORIZONTAL);

        return composite;
    }
    
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalSpan = 3;
        gridData.grabExcessHorizontalSpace = true;
        // gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = SWT.CENTER;

        parent.setLayoutData(gridData);
        Button okButton = createButton(parent, OK, "OK", true);
        okButton.setFocus();

        // create 'Details' button
        Button detailsButton = createButton(parent, DETAILS, "Details >>>", false);
        detailsButton.setToolTipText("The first click adds info about root exceptions, every" +
        		" additional click adds one line of stack info.");
        // Add a SelectionListener
        detailsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setReturnCode(DETAILS);
                String text = SEFormatter.getInfoWithStackTrace(m_sException, m_stackLevelIdx);
                m_errText.setText(text);
                if (s_isPrintToStdOut) {
                    System.out.println(text);
                }
                m_stackLevelIdx += 5;
            }
        });
        
        // Create Cancel button
        /* Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setReturnCode(CANCEL);
                close();
            }
        }); */
    }

    protected Button createOkButton(Composite parent, int id, String label,
            boolean defaultButton) {
        // increment the number of columns in the button bar
        ((GridLayout) parent.getLayout()).numColumns++;
        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        button.setFont(JFaceResources.getDialogFont());
        button.setData(Integer.valueOf(id));
        /* button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                if (isValidInput()) {
                    okPressed();
                }
            }
        }); */
        if (defaultButton) {
            Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(button);
            }
        }
        setButtonLayoutData(button);
        return button;
    }

    
    /**
     * Returns setting of flag, which controls printing of details to stdout.
     * Printing to stdout is useful when debugging from eclipse, since console 
     * view parses the output, so source code location of call stack is 
     * accessible by clicking links in console window.   
     */
    public static boolean isPrintToStdOut() {
        return s_isPrintToStdOut;
    }


    /**
     * Sets the flag, which controls printing of details to stdout.
     * Printing to stdout is useful when debugging from eclipse, since console 
     * view parses the output, so source code location of call stack is 
     * accessible by clicking links in console window.
     * This setting is global for application.   
     */
    public static void setPrintToStdOut(boolean isPrintToStdOut) {
        s_isPrintToStdOut = isPrintToStdOut;
    }

}

