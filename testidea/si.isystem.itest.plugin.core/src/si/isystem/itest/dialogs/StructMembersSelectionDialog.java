package si.isystem.itest.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.GlobalsContainer;
import si.isystem.commons.globals.GlobalsSelectionControl;
import si.isystem.connect.CTestSpecification;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.VarsMacrosLocalsGlobalsProvider;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.ui.spec.data.VariablesSelectionTree;
import si.isystem.ui.utils.KGUIBuilder;

public class StructMembersSelectionDialog extends Dialog {

    public static String DLG_TITLE = "Add variable initializations";

    private static final String VARS_ALL_GLOBALS_PROVIDER = "macrosLocalsRvPersisHost";


    private List<String> m_initVarsStr = new ArrayList<>();
    private GlobalsSelectionControl m_varNameField;

    private VariablesSelectionTree m_varSelectionTree;

    private String m_coreId;
    private CTestSpecification m_testSpec;

    private VarsMacrosLocalsGlobalsProvider m_varsProvider;

    
    public StructMembersSelectionDialog(Shell parentShell, 
                                        String coreId,
                                        CTestSpecification testSpec) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        m_coreId = coreId;
        m_testSpec = testSpec;
    }
    
    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);

        getShell().setText(DLG_TITLE);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.heightHint = 600;  // sets initial dialog size
        gridData.widthHint = 500;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fill", 
                                             "[min!][fill]", 
                                             "[min!][min!][fill][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
        builder.label("Variable:");
        
        m_varsProvider = 
                new VarsMacrosLocalsGlobalsProvider(ConnectionProvider.instance(),
                                                    null,
                                                    GlobalsConfiguration.instance(),
                                                    m_testSpec,
                                                    false,  // no ret val
                                                    true,   // locals
                                                    true,   // persist vars 
                                                    false); // no host vars
        m_varsProvider.refreshGlobals();
        
        GlobalsContainer globalContainer = GlobalsConfiguration.instance().getGlobalContainer();
        globalContainer.addProvider(VARS_ALL_GLOBALS_PROVIDER, m_varsProvider);
        
        m_varNameField = new GlobalsSelectionControl(mainDlgPanel, 
                                                     "wrap",
                                                     null,  // proposals
                                                     null,  // descriptions
                                                     SWT.NONE,
                                                     VARS_ALL_GLOBALS_PROVIDER,
                                                     null,
                                                     true,  // show refresh button
                                                     false, // do not show source button
                                                     ContentProposalAdapter.PROPOSAL_REPLACE,
                                                     UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
                                                     globalContainer,
                                                     ConnectionProvider.instance());
        m_varNameField.setToolTipText("Enter name of variable of complex type, "
                                      + "to get list of members below.");

//        m_isAppendPartitionCb = builder.checkBox("Append download file name in table", 
//                                                 "skip, gapleft 10, wrap");
//        UiTools.setToolTip(m_isAppendPartitionCb, "If checked, name of download file will be appended to each member,\n"
//                + "for example:  myStruct.counter,,out.elf\n"
//                + "Use this setting when there is more than one download file, and variable is declared\n"
//                + "in file which is not set as default one in winIDEA.");
        
        builder.separator("span 2, growx, gaptop 5, gapbottom 10, wrap", SWT.HORIZONTAL);
        
        m_varSelectionTree = new VariablesSelectionTree(m_coreId, m_testSpec);
        m_varSelectionTree.createControl(builder, 
                                         "grow, wmin 0, hmin 0, span 2, wrap",
                                         "gaptop 10, span 2, gapleft 10");
                
//        m_treeViewer = new CheckboxTreeViewer(builder.getParent());
//        m_treeViewer.getTree().setLayoutData("grow, wmin 0, hmin 0, span 2");
//        MembersContentProvider treeContentProvider = new MembersContentProvider();
//        // treeContentProvider.inputChanged(treeViewer, null, newInput);
//        m_treeViewer.setContentProvider(treeContentProvider);
//        m_treeViewer.setLabelProvider(new MembersLabelProvider());
//        // List<VNode> newInput = new ArrayList<>(); 
//        // treeViewer.setInput(newInput); // pass a non-null that will be ignored
//
//        // when user clicks a checkbox in the tree, check/uncheck all its children
//        m_treeViewer.addCheckStateListener(new ICheckStateListener() {
//            public void checkStateChanged(CheckStateChangedEvent event) {
//                // check/uncheck all children
//                VNode vNode = (VNode)event.getElement();
//                m_treeViewer.setSubtreeChecked(vNode, event.getChecked());
//                m_treeViewer.setGrayed(vNode, false);
//                
//                if (vNode.m_parent != null) {
//                    updateStateOfParent(m_treeViewer, vNode.m_parent, EChildrenState.ALL_UNCHECKED);
//                }
//            }
//        });
        
        final Combo varNameCtrl = m_varNameField.getControl();

        varNameCtrl.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                String varName = varNameCtrl.getText().trim();
                m_varSelectionTree.refreshHierarchy(varName);
            }
        });
        
        return composite;
    }
    
    
    @Override
    protected void okPressed() {
        m_initVarsStr = m_varSelectionTree.getData(); // save while the tree viewer is still available
        
        super.okPressed();
    }
    
    
    public boolean show() {
        return open() == Window.OK;
    }

    
    public List<String> getInitVars() {
        return m_initVarsStr;
    }
}
