package si.isystem.commons.utils;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.ISysCommonConstants;
import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.ui.utils.AsystComboContentAdapter;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.AsystTextContentAdapter;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class ISysUIUtils {
    
    public static final String LINK_TO_EDITOR_BTN_TOOLTIP = "Show source. Opens file with the specified item in editor.\n" +
                                      "Also brings WinIDEA window to the top,\n" +
                                      "if enabled in Windows settings.";
    
    public final static char [] ALPHA_CONTENT_PROPOSAL_KEYS = 
            new char[]{'a', 'b', 'c', 'd', 'e',
                       'f', 'g', 'h', 'i', 'j',
                       'k', 'l', 'm', 'n', 'o',
                       'p', 'q', 'r', 's', 't',
                       'u', 'v', 'w', 'x', 'y', 'z',
                       
                       'A', 'B', 'C', 'D', 'E',
                       'F', 'G', 'H', 'I', 'J',
                       'K', 'L', 'M', 'N', 'O',
                       'P', 'Q', 'R', 'S', 'T',
                       'U', 'V', 'W', 'X', 'Y', 'Z',
                       
                       '$', '{',            // for host variables
                       '_', '.', '>'};  // . and -> for structs and pointers

    
    public static AsystContentProposalProvider 
    addComboContentProposalsAdapter(Control control,
                                    String[] proposals,
                                    String[] descriptions,
                                    int proposalAcceptanceStyle,
                                    boolean isShowContentProposalsOnExplicitCtrlSpace) {

        AsystContentProposalProvider proposalsProvider = 
                new AsystContentProposalProvider(proposals, descriptions);
        proposalsProvider.setFiltering(true);

        addContentProposalsAdapter(control, 
                                   proposalsProvider, 
                                   new AsystComboContentAdapter(),
                                   proposalAcceptanceStyle,
                                   isShowContentProposalsOnExplicitCtrlSpace);
        return proposalsProvider;
    }


    /**
     * UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()
     * @param control
     * @param proposalProvider
     * @param proposalAcceptanceStyle ContentProposalAdapter.PROPOSAL_INSERT or
     * ContentProposalAdapter.PROPOSAL_REPLACE
     */
    public static void addContentProposalsAdapter(Control control,
                                                  IContentProposalProvider proposalProvider,
                                                  IControlContentAdapter contentAdapter,
                                                  int proposalAcceptanceStyle,
                                                  boolean isShowContentProposalsOnExplicitCtrlSpace) {
        
        char [] autoActivationCharacters = ALPHA_CONTENT_PROPOSAL_KEYS;
        
        if (isShowContentProposalsOnExplicitCtrlSpace) {
            autoActivationCharacters = null;
        }
        
        ContentProposalAdapter adapter = 
                new ContentProposalAdapter(control, 
                                           contentAdapter, 
                                           proposalProvider, 
                                           KeyStroke.getInstance(SWT.CTRL, SWT.SPACE), 
                                           autoActivationCharacters); 
        adapter.setPropagateKeys(true);
        adapter.setProposalAcceptanceStyle(proposalAcceptanceStyle);
    }


    public static AsystContentProposalProvider addContentProposalsAdapter(Control control,
                                                                          String[] proposals,
                                                                          String[] descriptions,
                                                                          int proposalAcceptanceStyle,
                                                                          boolean isShowContentProposalsOnExplicitCtrlSpace) {

        AsystContentProposalProvider proposalsProvider = 
                      new AsystContentProposalProvider(proposals, descriptions);
        proposalsProvider.setFiltering(true);

        addContentProposalsAdapter(control, 
                                   proposalsProvider, 
                                   new AsystTextContentAdapter(),
                                   proposalAcceptanceStyle,
                                   isShowContentProposalsOnExplicitCtrlSpace);
        return proposalsProvider;
    }


    public static AsystContentProposalProvider createContentProposalsAdapter(GlobalsProvider provider,
                                                                             boolean isInsertAcceptanceStyle) {

        AsystContentProposalProvider proposalsProvider = 
                new AsystContentProposalProvider(provider.getCachedGlobals(), 
                                                 provider.getCachedDescriptions());
        proposalsProvider.setFiltering(true);
        proposalsProvider.setProposalsAcceptanceStyle(isInsertAcceptanceStyle ?
                                                          ContentProposalAdapter.PROPOSAL_INSERT :
                                                          ContentProposalAdapter.PROPOSAL_REPLACE);

        return proposalsProvider;
    }


    /**
     * 
     * @param control
     * @param proposalProvider
     * @param proposalAcceptanceStyle ContentProposalAdapter.PROPOSAL_INSERT or
     * ContentProposalAdapter.PROPOSAL_REPLACE
     */
    public static AsystContentProposalProvider addContentProposalsAdapter(Control control,
                                                                          int proposalAcceptanceStyle,
                                                                          boolean isShowContentProposalsOnExplicitCtrlSpace) {
        
        return addContentProposalsAdapter(control, 
                                          new String[0], null, 
                                          proposalAcceptanceStyle,
                                          isShowContentProposalsOnExplicitCtrlSpace);
    }
    
    
/*    public static Button createShowSourceButton(KGUIBuilder builder,
                                                final IIConnectOperation operation,
                                                String migLayoutData,
                                                final IConnectionProvider conProvider) {
        
        return createShowSourceButton(builder, operation, migLayoutData, conProvider);
    }
    */
    
    public static Button createShowSourceButton(final KGUIBuilder builder,
                                                final IIConnectOperation operation,
                                                String migLayoutData,
                                                final IConnectionProvider conProvider) {
        
        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Shell parent = builder.getParent().getShell();
                JConnection jCon = conProvider.getDefaultConnection();
                execWinIDEAOperation(operation, parent, jCon);
            }
        };
        
        return createShowSourceButton(builder, migLayoutData, listener);
    }
    
    
    public static Button createShowSourceButton(KGUIBuilder builder,
                                                String migLayoutData,
                                                SelectionListener selectionListener) {
        
        Image icon = ISysResourceUtils.getImage("resources/icons/enabled/linkToEditor.png", 
                                                ISysCommonConstants.COMMONS_PLUGIN_ID);
        final Button showSourceBtn = builder.button(icon, migLayoutData);
        UiTools.setToolTip(showSourceBtn, LINK_TO_EDITOR_BTN_TOOLTIP);

        showSourceBtn.addSelectionListener(selectionListener);
        
        return showSourceBtn;
    }
    
    
    public static void execWinIDEAOperation(final IIConnectOperation operation,
                                            Shell shell,
                                            JConnection jCon) {
        execWinIDEAOperation(operation, shell, true, jCon);
    }
   
   
    public static void execWinIDEAOperation(final IIConnectOperation operation,
                                            Shell shell, 
                                            boolean isShowErrorMessage,
                                            JConnection jCon) {

        if (operation == null) {
            return;
        }
        
        try {
            if (jCon != null) {
                operation.exec(jCon);
            } 
        } catch (Exception ex) {
            if (isShowErrorMessage) {
                ex.printStackTrace();
                if (DownloadDialog.open("Connection is OK, but can not perform requested operation!", 
                                        ex,
                                        jCon)) {
                    // give user a chance to stop the target or download
                    try {
                        operation.exec(jCon);
                    } catch (Exception ez) {
                        // we've tried everything now, give up!
                        SExceptionDialog.open(shell, "Operation failed!", ez);
                    }
                }
            }
        }
    }
    
    
    public static Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }
}
