package si.isystem.commons.connect;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.data.JVersion;
import si.isystem.connect.utils.WinIDEAEnumerator;
import si.isystem.connect.utils.WinIDEAEnumerator.WinIDEAInfo;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellDoubleClickListener;
import de.kupzog.ktable.KTableCellSelectionListener;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;

/**
 * This class creates connection dialog and fills it with enumerated winIDEA 
 * instances. Do not use this class directly, but use <b>RConnectCommandHandler</b> 
 * instead - it will pop-up this dialog and create connections to the selected 
 * winIDEA.
 * 
 * @author markok
 *
 */
public class ConnectionDialog extends Dialog {

    private KTable m_instanceTable;
    private List<WinIDEAInfo> m_instanceInfoList;
    private Text m_statusText;
    private Button m_startNewWinIDEABtn;
    private Button m_refreshBtn;
    
    private int m_selectedIdx = -1;
    private JVersion m_testIDEAVersion;
    private String m_configWorkspace;
    private boolean m_duplicatedInstance = false;
    private boolean m_isOKBtnEnabled = false;

    private static int s_idCounter = 0;

    private static final int WIN_IDEA_VERSION_COL = 0; 
    private static final int WIN_IDEA_ID_COL = 1; 
    private static final int WIN_IDEA_WORKSPACE_COL = 2; 
    
    
    /**
     * Creates a dialog.
     * 
     * @param parentShell parent shell
     * @param testIDEAVersion 
     * @param winIDEAWorkspace 
     */
    public ConnectionDialog(Shell parentShell, 
                            String winIDEAWorkspace, 
                            JVersion testIDEAVersion) {
        
        super(parentShell);
        
        setShellStyle(getShellStyle() | SWT.RESIZE);
        m_configWorkspace = winIDEAWorkspace;
        m_testIDEAVersion = testIDEAVersion;
        
        enumerateRunningWinIDEAInstances();
    }


    public boolean show() {
        return open() == Window.OK;
    }
    
    
    @Override
    protected void okPressed() {
        Point[] selection = m_instanceTable.getCellSelection();
        
        m_selectedIdx = selection[0].y - 1;  // -1 for header row
            
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    
    
    public int getSelectedIdx() {
        return m_selectedIdx;
    }


    public List<WinIDEAInfo> getInfoList() {
        return m_instanceInfoList;  
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Connect to winIDEA?");

        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = 600;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fillx", "[grow][min!]", "[min!][grow]"));
        
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        builder.label("testIDEA version: " + m_testIDEAVersion, "wrap");
        
        builder.label("Running winIDEA instances:", "gaptop 5, gapbottom 3, wrap");
        
        m_instanceTable = new KTable(mainDlgPanel, true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                                         SWT.FULL_SELECTION |
                                                         SWTX.FILL_WITH_LASTCOL);
                
        m_instanceTable.addCellSelectionListener(new KTableCellSelectionListener() {
            
            @Override
            public void fixedCellSelected(int col, int row, int statemask) {
                // ignore clicks on header row
            }
            
            
            @Override
            public void cellSelected(int col, int row, int statemask) {
                row--; // for header row
                if (row < 0) {
                    setDefaultStatusText();
                    setEnabledOKBtn(false);
                    return;
                }                
                
                WinIDEAInfo info = m_instanceInfoList.get(row);
                setEnabledOKBtn(true);
                
                JVersion winIDEAVer = new JVersion(info.getVerMajor(), info.getVerMinor(),
                                                   info.getVerBuild());
                switch (winIDEAVer.compareTo(m_testIDEAVersion)) {
                case -1:
                    m_statusText.setBackground(ColorProvider.instance().getWarningColor());
                    setStatusText("winIDEA version is lower than testIDEA version.\ntestIDEA may not function properly.");
                    break;
                case 1:
                    m_statusText.setBackground(ColorProvider.instance().getInfoColor());
                    setStatusText("winIDEA version is higher than testIDEA version.\nUsually there should be no problems.");
                    break;
                default:
                    m_statusText.setBackground(ColorProvider.instance().getBkgNoneditableColor());
                    setStatusText("winIDEA version matches testIDEA version.");
                }
            }
        });
        
        m_instanceTable.setModel(new WinIDEAsEnumModel(m_instanceInfoList));
        
        m_instanceTable.addCellDoubleClickListener(new KTableCellDoubleClickListener() {
            
            @Override
            public void fixedCellDoubleClicked(int col, int row, int statemask) {
                // ignore double clicks on header
            }
            
            
            @Override
            public void cellDoubleClicked(int col, int row, int statemask) {
                // close the dialog on double click, no need to press OK btn
                okPressed();
            }
        });        
        
        m_instanceTable.setLayoutData("wmin 0, hmin 150, grow, wrap");

        m_statusText = builder.text("height 60:60:60, growx, gapleft 5, gapright 10, wrap", SWT.MULTI);
        m_statusText.setEditable(false);
        setDefaultStatusText();

        m_startNewWinIDEABtn = builder.button("Start new winIDEA", "gapleft 5, split 2");
        UiTools.setToolTip(m_startNewWinIDEABtn, "Starts new instance of winIDEA from the same folder as this instance of testIDEA.\n"
                + "Workspace file specified in 'File | Properties | General' will be used. Currently is it set to:\n"
                + m_configWorkspace);
        m_startNewWinIDEABtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    startWinIDEA();
                    Thread.sleep(1000);
                    refresh();
                } catch (Exception ex) {
                    m_statusText.setBackground(ColorProvider.instance().getErrorColor());
                    setStatusText("Can not start winIDEA!\n" + ex.toString());
                    ex.printStackTrace();
                }
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        
        m_refreshBtn = builder.button("Refresh", "wrap");
        UiTools.setToolTip(m_refreshBtn, "Press this button if you have started winIDEA manually.");
        m_refreshBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                refresh();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        builder.separator("grow, gaptop 10, wrap", SWT.HORIZONTAL);
        
        selectSameVersion();
        return composite;
    }

    
    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        setEnabledOKBtn(m_isOKBtnEnabled);
        if (m_isOKBtnEnabled) {
            Button button = getButton(IDialogConstants.OK_ID);
            if (button != null) {
                button.setFocus();
            }
        }

        return ctrl;
    }


    private void enumerateRunningWinIDEAInstances() {
        WinIDEAEnumerator enumerator = new WinIDEAEnumerator();
        m_instanceInfoList = enumerator.enumerate();

        // detect winIDEAS with the same ID (or no ID) and workspace opened
        Set<String> instances = new TreeSet<>();
        m_duplicatedInstance = false;
        for (WinIDEAInfo info : m_instanceInfoList) {
            String idWs = info.getInstanceId() + info.getWorkspace();
            if (instances.contains(idWs)) {
                m_duplicatedInstance = true;
                return;
            }
            instances.add(idWs);
        }
    }
    
    
    private void selectSameVersion() {
        int idx = 0;
        for (WinIDEAInfo info : m_instanceInfoList) {
            JVersion winIDEAVer = new JVersion(info.getVerMajor(), 
                                               info.getVerMinor(), 
                                               info.getVerBuild());
            Path winIDEAPath = Paths.get(info.getWorkspace());
            Path configPath = Paths.get(m_configWorkspace);
            
            // prefer winIDEA with the same version and workspace
            if (winIDEAVer.equals(m_testIDEAVersion)  &&  
                    (m_configWorkspace.isEmpty()  ||  configPath.equals(winIDEAPath))) {
                
                m_instanceTable.setSelection(0, idx + 1, true);
                return;
            }
            idx++;
        }
            
        
        // check for workspace match
        idx = 0;
        for (WinIDEAInfo info : m_instanceInfoList) {
            Path winIDEAPath = Paths.get(info.getWorkspace());
            Path configPath = Paths.get(m_configWorkspace);
            
            if (!m_configWorkspace.isEmpty()  &&  configPath.equals(winIDEAPath)) {
                m_instanceTable.setSelection(0, idx + 1, true);
                return;
            }
            idx++;
        }
        
        
        // at least version should match
        idx = 0;
        for (WinIDEAInfo info : m_instanceInfoList) {
            JVersion winIDEAVer = new JVersion(info.getVerMajor(), 
                                               info.getVerMinor(), 
                                               info.getVerBuild());
            if (winIDEAVer.equals(m_testIDEAVersion)) {
                m_instanceTable.setSelection(0, idx + 1, true);
                return;
            }
            
            idx++;
        }
        
        // nothing is selected
        setEnabledOKBtn(false);
    }


    private void refresh() {
        enumerateRunningWinIDEAInstances();
        m_instanceTable.setModel(new WinIDEAsEnumModel(m_instanceInfoList));
        setDefaultStatusText(); // if there was error message before and no 
        // instances are found, this erases previous status.
        selectSameVersion();    
    }
    

    private void setDefaultStatusText() {
        m_statusText.setBackground(ColorProvider.instance().getBkgNoneditableColor());
        setStatusText("Please select winiDEA from the list above.");
    }

    
    private void startWinIDEA() throws IOException {
        URL url = Platform.getInstallLocation().getURL();
        String winIDEAExeName = url.getPath() + "\\winIDEA.exe";
        if (!m_configWorkspace.isEmpty()) {
            // make sure path is valid OS path (correct path separator is used)
            winIDEAExeName += " " + Paths.get(m_configWorkspace).toString();
        }
        winIDEAExeName += " /id:ti_" + s_idCounter++;

        Runtime.getRuntime().exec(winIDEAExeName, null);
    }
    
    
    private void setStatusText(String text) {
        if (m_duplicatedInstance) {
            text += "\nWARNING: Two or more instances with the same id and workspace found!\n" +
                    "    Connection will be made to one of instances with the selected workspace and ID - version is ignored!";
        }
        m_statusText.setText(text);
    }
    
    
    private void setEnabledOKBtn(boolean isEnabled) {
        m_isOKBtnEnabled  = isEnabled;
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(isEnabled);
        }
    }
    
    
    /*
     * Model for KTable showing list of winIDEA instances.
     */
    class WinIDEAsEnumModel extends KTableModelAdapter {

        private List<WinIDEAEnumerator.WinIDEAInfo> m_instances;

        
        WinIDEAsEnumModel(List<WinIDEAEnumerator.WinIDEAInfo> instances) {
            m_instances = instances;
        }
        
        
        @Override
        public Object doGetContentAt(int col, int row) {
            if (row == 0) {
                switch (col){
                case WIN_IDEA_VERSION_COL:
                    return "Version";
                case WIN_IDEA_ID_COL:
                    return "winIDEA ID";
                case WIN_IDEA_WORKSPACE_COL:
                    return "Workspace file";
                }
            }
            
            WinIDEAInfo winIDEAInfo = m_instances.get(row - 1);
            switch (col) {
            case WIN_IDEA_VERSION_COL:
                String version = winIDEAInfo.getVerMajor() + "." + 
                                 winIDEAInfo.getVerMinor() + "." +
                                 winIDEAInfo.getVerBuild();
                return version;
            case WIN_IDEA_ID_COL:
                return winIDEAInfo.getInstanceId();
            case WIN_IDEA_WORKSPACE_COL:
                return winIDEAInfo.getWorkspace();
            }
            
            return "?"; // should never get here
        }

        
        @Override
        public int getFixedHeaderColumnCount() {
            return 0;
        }

        
        @Override
        public int doGetRowCount() {
            return m_instances.size() + 1;  // + 1 for header row
        }

        
        @Override
        public int doGetColumnCount() {
            return 3; // winIDEA version, winIDEA ID, and winIDEA Workspace;
        }

        
        @Override
        public int getInitialRowHeight(int row) {
            int defaultTableRowHeight = FontProvider.instance().getDefaultTableRowHeight(getShell());
            if (row == 0) {
                return defaultTableRowHeight * 12 / 10; // make header row a bit higher
            }
            return defaultTableRowHeight;
        }
        
        
        @Override
        public int getInitialColumnWidth(int column) {
            
            switch (column){
            case WIN_IDEA_VERSION_COL:
                return FontProvider.instance().getTextWidth(m_instanceTable, "9.12.100000");
            case WIN_IDEA_ID_COL:
                return FontProvider.instance().getTextWidth(m_instanceTable, "D:\\some\\folder\\winIDEA ID");
            case WIN_IDEA_WORKSPACE_COL:
                // the last column wil use remaining space anyway
                return FontProvider.instance().getTextWidth(m_instanceTable, "Workspace file");
            }
            
            return 50; // should never get here
        }
    }
}
