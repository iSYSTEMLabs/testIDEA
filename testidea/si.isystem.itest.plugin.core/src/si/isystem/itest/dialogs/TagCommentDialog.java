package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.itest.common.Messages;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class TagCommentDialog extends Dialog {

    private Text m_blockCommentTa;
    private Text m_endOfLineCommentT;
    private String m_blockComment;
    private String m_endOfLineComment;


    public TagCommentDialog(Shell parentShell) {
        super(parentShell);

        // make a dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText(Messages.TagCommentDialog_Dialog_title);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 510;  // sets initial dialog size
        // gridData.widthHint = 400;
        mainDlgPanel.setLayoutData(gridData);

        
        mainDlgPanel.setLayout(new MigLayout("", "fill, grow", "[pref!][pref!][fill, grow][pref!][pref!]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        builder.label("Information from this dialog is stored as comment in test specification file.", "wrap"); //$NON-NLS-1$ //$NON-NLS-2$
        builder.label(Messages.TagCommentDialog_Block_comment, "grow, wrap"); //$NON-NLS-2$
        m_blockCommentTa = builder.text("width 100:500:, height 60:100:1000, growx, growy, wrap",  //$NON-NLS-1$
                                        SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        UiTools.setToolTip(m_blockCommentTa, "Text from this field will appear BEFORE the tag:\n\n" + //$NON-NLS-1$
                                        "  # block comment line 1\n" + //$NON-NLS-1$
                                        "  # block comment line 2\n" + //$NON-NLS-1$
                                        "  tag: value"); //$NON-NLS-1$
        
        // make TAB key select the next control, instead of adding \t to the comment
        m_blockCommentTa.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            }
        });
        
        
        builder.label(Messages.TagCommentDialog_End_of_line_comment, "wrap"); //$NON-NLS-2$
        m_endOfLineCommentT = builder.text("wrap", SWT.BORDER); //$NON-NLS-1$
        UiTools.setToolTip(m_endOfLineCommentT, "Text from this field will appear at the same line as the tag:\n\n" + //$NON-NLS-1$
                                            "  tag: value   # end of line comment"); //$NON-NLS-1$

        Button clearAllBtn = builder.button(Messages.TagCommentDialog_Clear_all_fields, "w pref!, alignx right, gaptop 5, wrap"); //$NON-NLS-2$
        UiTools.setToolTip(clearAllBtn, "Deletes text in both fields."); //$NON-NLS-1$
        clearAllBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_blockCommentTa.setText(""); //$NON-NLS-1$
                m_endOfLineCommentT.setText(""); //$NON-NLS-1$
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        
        m_blockCommentTa.setText(m_blockComment);
        m_endOfLineCommentT.setText(m_endOfLineComment);
        
        builder.separator("span 2, growx, gaptop 15", SWT.HORIZONTAL); //$NON-NLS-1$
        
        return composite;
    }

    
    @Override
    protected void okPressed() {
        m_blockComment = m_blockCommentTa.getText();
        m_endOfLineComment = m_endOfLineCommentT.getText();
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    

    public boolean show() {
        return open() == Window.OK;
    }


    public String getNewLineComment() {
        return m_blockComment;
    }


    public String getEndOfLineComment() {
        return m_endOfLineComment;
    }


    public void setNewLineComment(String blockComment) {
        m_blockComment = blockComment;
    }


    public void setEndOfLineComment(String endOfLineComment) {
        m_endOfLineComment = endOfLineComment;
    }
}
