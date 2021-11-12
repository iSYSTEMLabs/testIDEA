package si.isystem.itest.dialogs;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestSpecification;
import si.isystem.itest.common.FilterConfigPage;
import si.isystem.itest.common.FilterConfigPage.ContainerType;
import si.isystem.itest.common.UiUtils;
import si.isystem.ui.utils.KGUIBuilder;

public class NewTestGroupDialog extends Dialog {

    private Text m_groupIdTxt;
//    private Button m_generateIdCb;
//    private Button m_useCoreCb;
//    private Button m_usePartitionCb;
//    private Button m_useModuleCb;
    
//    private static boolean m_isGenerateId = true;
//    private static boolean m_isUseCore = true;
//    private static boolean m_isUsePartition = true;
//    private static boolean m_isUseModule = true;
    
    private String m_groupId;
    private CTestSpecification m_containerTestSpec;
    private CTestFilter m_filter;
    private FilterConfigPage m_filterConfigPage;
    private CTestFilter[] m_parentFilters;
    private static IDialogSettings m_dialogSettings = new DialogSettings("NewGroupDialog");


    public NewTestGroupDialog(Shell shell,
                              CTestSpecification containerTestSpec,
                              CTestFilter [] parentFilters) {
        super(shell);

        m_containerTestSpec = containerTestSpec;
        m_filter = new CTestFilter(null);
        m_parentFilters = parentFilters;
        
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        MigLayout layout = new MigLayout("fill", "[min!][min!][min!][min!][fill]", "[min!][min!][fill]");

        String dlgTitle = "New test group";
        int widthHint = 600;
        
        KGUIBuilder builder = UiUtils.initDialogPanel(composite,
                                                      layout,
                                                      dlgTitle,
                                                      widthHint);

        builder.label("Group ID:", "gapbottom 7");
        m_groupIdTxt = builder.text("span 4, growx, gapbottom 7, wrap", SWT.BORDER);
//        m_generateIdCb = builder.checkBox("Generate ID:");
//        m_useCoreCb = builder.checkBox("Use core ID");
//        m_usePartitionCb = builder.checkBox("Use partition");
//        m_useModuleCb = builder.checkBox("Use module", "wrap");
//
//        m_generateIdCb.setSelection(m_isGenerateId);
//        m_useCoreCb.setSelection(m_isUseCore);
//        m_usePartitionCb.setSelection(m_isUsePartition);
//        m_useModuleCb.setSelection(m_isUseModule);
        
//        addListeners();
        m_filterConfigPage = new FilterConfigPage(ContainerType.E_TREE, false);
        Composite filterPanel = builder.composite(SWT.NONE, "span 5, wmin 0, growx, gaptop 10");
        filterPanel.setLayout(new MigLayout("fill"));
        
        m_filterConfigPage.createMainPanel(filterPanel);
        m_filterConfigPage.setInput(m_containerTestSpec, 
                                    m_filter, 
                                    null,
                                    m_parentFilters);
        
        m_filterConfigPage.refreshGlobals();
        m_filterConfigPage.fillControls();

//        KeyListener keyListener = new KeyListener() {
//
//            @Override
//            public void keyReleased(KeyEvent e) {}
//
//            @Override
//            public void keyPressed(KeyEvent e) {
//                // update also on control chars, so that also pasting updates id
//                // update also when e == null, if 'Use built-in filter' is clicked
//                generateGroupId();
//            }
//        };
        
//        m_filterConfigPage.addKeyListenerForCorePartitionModule(keyListener);
        
        return composite;
    }
    
    
//    private void addListeners() {
//        
//        m_generateIdCb.addSelectionListener(new SelectionAdapter() {
//            
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//
//                boolean isSelected = m_generateIdCb.getSelection(); 
//                
//                setEnabledGeneratorCBs(isSelected);
//                
//                if (isSelected) {
//                    generateGroupId();
//                }
//
//            }
//        });
//        
//        SelectionAdapter listener = new SelectionAdapter() {
//            
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                generateGroupId();
//            }
//        };
//        
//        m_useCoreCb.addSelectionListener(listener);
//        m_usePartitionCb.addSelectionListener(listener);
//        m_useModuleCb.addSelectionListener(listener);
//    }
//
//
//    private void setEnabledGeneratorCBs(boolean isEnabled) {
//        m_useCoreCb.setEnabled(isEnabled);
//        m_usePartitionCb.setEnabled(isEnabled);
//        m_useModuleCb.setEnabled(isEnabled);
//    }
//    
//    
//    private void generateGroupId() {
//        if (m_filterConfigPage.isUseBuiltInfilter()) {
//            generateGroupIdForBuiltInFilter();
//            m_generateIdCb.setEnabled(true);
//            setEnabledGeneratorCBs(true);
//        } else {
//            // impl when requested by customer - use function name 
//            // generateGroupIdForScriptFilter();
//            m_generateIdCb.setEnabled(false);
//            setEnabledGeneratorCBs(false);
//        }
//    }
    
   
//    private void generateGroupIdForBuiltInFilter() {
//        final char itemSeparator = '-';
//        final char typeSeparator = '_';
//        final char illegalCharReplacement = '.';
//
//        if (!m_generateIdCb.getSelection()) {
//            return;
//        }
//        
//        StringBuilder sb = new StringBuilder();
//        
//        StrVector items = new StrVector(); 
//        
//        if (m_useCoreCb.getSelection()) {
//            m_filter.getCoreIds(items);
//            List<String> listItems = DataUtils.strVector2List(items);
//            sb.append(StringUtils.join(listItems, itemSeparator));
//        }
//        
//        if (m_usePartitionCb.getSelection()) {
//            m_filter.getPartitions(items);
//            if (sb.length() > 0  &&  items.size() > 0) {
//                sb.append(typeSeparator); // core ids are present, add separator
//            }
//            List<String> listItems = DataUtils.strVector2List(items);
//            sb.append(StringUtils.join(listItems, itemSeparator));
//        }
//        
//        if (m_useModuleCb.getSelection()) {
//            m_filter.getModules(items);
//            if (sb.length() > 0  &&  items.size() > 0) {
//                sb.append(typeSeparator); // core ids or partitions are present, add separator
//            }
//            List<String> listItems = DataUtils.strVector2List(items);
//            sb.append(StringUtils.join(listItems, itemSeparator));
//        }
//
//        // replace chars illegal in groupID 
//        String allowedSymbols = CYAMLUtil.getSymbolsAllowedTestId();
//        for (int idx = 0; idx < sb.length(); idx++) {
//            char ch = sb.charAt(idx);
//            if (allowedSymbols.indexOf(ch) <= 0  &&  !Character.isAlphabetic(ch)  &&
//                !Character.isDigit(ch)) {
//                sb.setCharAt(idx, illegalCharReplacement);
//            }
//        }
//        m_groupIdTxt.setText(sb.toString());
//    }

    
    public boolean show() {
        
        return open() == Window.OK;
    }


    @Override
    public IDialogSettings getDialogBoundsSettings() {
        return m_dialogSettings;
    }
    
    
    @Override
    public int getDialogBoundsStrategy() {
        return DIALOG_PERSISTLOCATION | DIALOG_PERSISTSIZE;
    }
    
    
    @Override
    protected void okPressed() {
        m_groupId = m_groupIdTxt.getText();
        
//        m_isGenerateId = m_generateIdCb.getSelection();
//        
//        m_isUseCore = m_useCoreCb.getSelection();
//        m_isUsePartition = m_usePartitionCb.getSelection();
//        m_isUseModule = m_useModuleCb.getSelection();
        
        super.okPressed();  // after that call the data from widgets is no longer available
    }
    
    
    public String getGroupId() {
        return m_groupId;
    }


    public CTestFilter getFilter() {
        return m_filter;
    }
}
