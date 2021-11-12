package si.isystem.itest.preferences;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CYAMLUtil;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.AutoIdGenerator;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.ui.spec.data.HostVarsUtils;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class AutoIdFormatDialog extends Dialog {

    
    private static final String EXAMPLE_FUNC_NAME = "min_int";
    private static final String EXAMPLE_PAR1 = "20";
    private static final String EXAMPLE_PAR2 = "30";

    private static final String EXAMPLE_TAG1 = "alpha";
    private static final String EXAMPLE_TAG2 = "beta";
    
    private String m_inputFormat;  // format given to ctor
    private final int NUM_FIELDS = 4;
    private Text m_text[] = new Text[NUM_FIELDS];
    private Combo m_combo[] = new Combo[NUM_FIELDS];
    private Text m_tail;
    private Text m_finalFmtTxt;
    private Text m_resultTxt;

    private String m_formatString;
    AutoIdGenerator m_autoIdGenerator = new AutoIdGenerator();
    
    private Map<String, String> m_exampleVars = new TreeMap<String, String>();
    
    private SelectionListener m_comboListener = new SelectionListener() {
        
        @Override
        public void widgetSelected(SelectionEvent e) {
            setExampleStrings();
        }
        
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {}
    };

    private KeyListener m_keyListener = new KeyListener() {
        
        @Override
        public void keyReleased(KeyEvent e) {
            try {
                setExampleStrings();
            } catch (Exception ex) {
                SExceptionDialog.open(getParentShell(), "Error in format string!", ex);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {}
    };
    private Text m_errorTxt;


    public AutoIdFormatDialog(Shell parentShell, String idFormat) {
        super(parentShell);

        // make a dialog resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);

        // create data used as an example - uids should be realistic, but fixed,
        // to simplify testing
        m_exampleVars.put(HostVarsUtils.$_UID, "2x45gdf");        
        m_exampleVars.put(HostVarsUtils.$_NID, "3f5gd4.3.2");        
        m_exampleVars.put(HostVarsUtils.$_DID, "1.2");        
        m_exampleVars.put(HostVarsUtils.$_UUID, "be224ac6-28a2-4af1-ac8a-9880232799f1");        
        m_exampleVars.put(HostVarsUtils.$_SEQ, "21");        
        if (HostVarsUtils.IS_CORE_ID_PART_OF_TEST_ID) {
            m_exampleVars.put(HostVarsUtils.$_CORE_ID, "core-1");
        }
        m_exampleVars.put(HostVarsUtils.$_FUNCTION, EXAMPLE_FUNC_NAME);        
        m_exampleVars.put(HostVarsUtils.$_PARAMS, EXAMPLE_PAR1 + 
                          CTestHostVars.LIST_SEPARATOR + EXAMPLE_PAR2);        
        m_exampleVars.put(HostVarsUtils.$_TAGS, EXAMPLE_TAG1 + 
                          CTestHostVars.LIST_SEPARATOR + EXAMPLE_TAG2);

        m_inputFormat = idFormat;
    }

    
    @Override
    protected Control createContents(Composite parent) {
        Control ctrl = super.createContents(parent);
        
        // getButton(IDialogConstants.OK_ID).setEnabled(!m_fileNameTxt.getText().trim().isEmpty());
        
        return ctrl;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Auto-ID Format");
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        // gridData.heightHint = 510;  // sets initial dialog size
        // gridData.widthHint = 400;
        mainDlgPanel.setLayoutData(gridData);

        
        mainDlgPanel.setLayout(new MigLayout("fill", ""));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
        KGUIBuilder fmtGroup = builder.group("Format", "span, gapbottom 15, wrap");
        Group g = (Group)fmtGroup.getParent();
        
        String []hostVars = HostVarsUtils.getHostVarsForAutoTestID(true);

        for (int i = 0; i < NUM_FIELDS; i++) {
            m_text[i] = fmtGroup.text("wmin 70");
            m_text[i].addKeyListener(m_keyListener);
            UiTools.setToolTip(m_text[i], "This field contains constant text. Use it as separators between variable fields\n"
            		                      + "It is recommended tu use '/' on boths sides of 'uid' and 'seq' fields (see command\n"
            		                      + "'Tools | Set Test IDs')\n"
            		                      + UiUtils.TEST_ID_ALLOWED_CHARS);
            
            m_combo[i] = fmtGroup.combo(hostVars, 
                                        "", SWT.NONE); // no SWT.READ_ONLY, combo should
                                           // be editable, because users may define their vars
                                           // to be used in scripts.
            
            m_combo[i].addSelectionListener(m_comboListener);
            m_combo[i].addKeyListener(m_keyListener);
            UiTools.setToolTip(m_combo[i], "Select one of variables from the list, or leave the field empty.");
        }
        
        m_tail = fmtGroup.text("wmin 70");
        m_tail.addKeyListener(m_keyListener);
        UiTools.setToolTip(m_tail, "This field contains constant text. Use it as separators between variable fields\n" +
                                   "It is recommended tu use '/' on boths sides of 'uid' and 'seq' fields (see command\n" +
                                   "'Tools | Set Test IDs')");
        
        builder.label("Syntax:");
        m_errorTxt = builder.text("growx, gapright 5, gapbottom 10, wrap");
        m_errorTxt.setEditable(false);
        
        Label lbl = builder.label("Format string:");
        Font boldFont = FontProvider.instance().getBoldControlFont(lbl);
        lbl.setFont(boldFont);
        g.setFont(boldFont);
        m_finalFmtTxt = builder.text("span, growx, gapright 5, gapbottom 10, wrap", SWT.BORDER);
        m_finalFmtTxt.setEditable(false); // non-editable, but users can copy it
        
        builder.label("Example:").setFont(boldFont);
        
        m_resultTxt = builder.text("pushx, growx 100, gapbottom 10", SWT.BORDER);
        m_resultTxt.setEditable(false);
        builder.label("(func: '" + EXAMPLE_FUNC_NAME + "(" + EXAMPLE_PAR1 + ", " + 
                      EXAMPLE_PAR2 + ")', tags: '" + EXAMPLE_TAG1 + "', '" + EXAMPLE_TAG2 + "')", 
                      "ax right, gapright 5, wrap");
        
        /* builder.label("  Derived:");
        m_derivedTxt = builder.text("pushx, growx 100", SWT.BORDER);
        m_derivedTxt.setEditable(false);
        builder.label("(func: 'min_int(-4, -5)', tags: 'alpha', 'beta')", 
                      "ax right, gapright 5, wrap"); */
        
        builder.label("Description of variables:", "span, wrap").setFont(boldFont);
        
        for (String hostVar : hostVars) {
            if (!hostVar.isEmpty()) {
                builder.label("    " + hostVar + ":  "); 
                builder.label(HostVarsUtils.getDesc(hostVar), "span, wrap");
            }
        }
        

        builder.label("\nNote:").setFont(boldFont);
        builder.label("\nIt is recommended to use '/' as separators for fields 'id' and 'seq'. " +
        		      "This way you can later automatically change only part of ID.", "span, wrap");
        builder.label("${nid} and ${seq} variables may not provide unique ID when only few test cases " +
        		"get ID assigned. If we assign IDs to all test cases", "skip, span, wrap");
        builder.label("in one step, they are unique.", "skip, span, wrap");
        
        builder.separator("span, growx, gaptop 15", SWT.HORIZONTAL);

        parseFormatString(m_inputFormat);
    
        setExampleStrings();
        
        return composite;
    }

    
    private void setExampleStrings() {
        String fmtstr = composeFormatString();
        m_finalFmtTxt.setText(fmtstr);
        try {
            String id = m_autoIdGenerator.createTestId(fmtstr, m_exampleVars);
            m_resultTxt.setText(id);
            verifyid(id);
        } catch (Exception ex) {
            m_errorTxt.setText("Invalid host variable in test ID format string!");
            m_errorTxt.setBackground(ColorProvider.instance().getErrorColor());
            m_resultTxt.setText("Invalid host variable in test ID format string!");
        }
    }
    
    
    private void verifyid(String id) {
        for (int i = 0; i < id.length(); i++) {
            if (!CYAMLUtil.isAllowedCharForTestId(id.charAt(i))) {
                m_errorTxt.setText("Character '" + id.charAt(i) + "' is not allowed in test ID.");
                m_errorTxt.setBackground(ColorProvider.instance().getErrorColor());
                return;
            }
        }
        m_errorTxt.setText("OK");
        m_errorTxt.setBackground(ColorProvider.instance().getBkgNoneditableColor());
    }


    @Override
    protected void okPressed() {
    
        m_formatString = composeFormatString();
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }


    private String composeFormatString() {
        StringBuilder sb = new StringBuilder();
    
        for (int i = 0; i < NUM_FIELDS; i++) {
            sb.append(m_text[i].getText());
            sb.append(m_combo[i].getText());
        }
        
        sb.append(m_tail.getText());
        
        return sb.toString();
    }
    

    public boolean show() {
        return open() == Window.OK;
    }


    public String getFormat() {
        return m_formatString;
    }

    
    // next: 
    // add Tool menu option
    // create command with options dialog and run it
    // add checkbox 'auto test ID' to New test dialog - persistent for testIDEA installation, not project
    // add context menu option - Set test IDs

    private void parseFormatString(String fmtStr) {
        Pattern pat = Pattern.compile("(\\$\\{\\w+\\})");
        Matcher matcher = pat.matcher(fmtStr);
        //System.out.println("-----");
        int beginIndex = 0;
        int i = 0;
        for (; matcher.find()  &&  i < NUM_FIELDS; i++) {
            m_text[i].setText(fmtStr.substring(beginIndex, matcher.start()));
            beginIndex = matcher.end();
            m_combo[i].setText(matcher.group(0));
            /* System.out.println("~~~: " + matcher.groupCount());
            for (int j = 0; j < matcher.groupCount(); j++) {
                System.out.println(matcher.group(j) + "  : "+ matcher.start() + ", " + matcher.end());
            } */
        }
        
        if (i < NUM_FIELDS) {
            m_text[i].setText(fmtStr.substring(beginIndex));
        } else if (i == NUM_FIELDS) {
            m_tail.setText(fmtStr.substring(beginIndex));
        }
    }
}
