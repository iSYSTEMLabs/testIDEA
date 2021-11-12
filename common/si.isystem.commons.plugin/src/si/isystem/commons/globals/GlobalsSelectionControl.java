package si.isystem.commons.globals;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import si.isystem.commons.ISysCommonsPlugin;
import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class creates panel with combo-box and Refresh
 * button. Combo-box has auto-complete field attached. This panel is used where
 * function or global variable name entry is required, for example in coverage,
 * profiler, stub and of course test function name setting. 
 *  
 */
public class GlobalsSelectionControl {

    private Combo m_comboBox;
    private Button m_refreshButon;
    private Button m_showSourceBtn;

    private String m_coreId = "";
    private String m_emptyComboListText = "--- Click 'Refresh' button ---";
    private String m_globalsProviderId;
    private IIConnectOperation m_showSourceOperation;
    private AsystContentProposalProvider m_functionsProposals;
    private GlobalsContainer m_globalsContainer;
    private IConnectionProvider m_conProvider;
    private Composite m_panel;
    private static Image m_refreshImg; 


    public GlobalsSelectionControl(Composite parent, 
                                   Object layoutData, 
                                   int style,
                                   String providerId,
                                   String coreId,
                                   boolean isShowRefreshButton,
                                   boolean isShowSourceButton,
                                   boolean isShowContentProposalsOnExplicitCtrlSpace,
                                   GlobalsContainer globalsContainer,
                                   IConnectionProvider conProvider) {
        
        this(parent, layoutData, null, null, style, providerId, coreId, 
             isShowRefreshButton, isShowSourceButton, ContentProposalAdapter.PROPOSAL_INSERT,
             isShowContentProposalsOnExplicitCtrlSpace, globalsContainer, conProvider);
    }

    
    public GlobalsSelectionControl(Composite parent, 
                                   Object layoutData, 
                                   String[] contents, 
                                   int style,
                                   String providerId,
                                   String coreId,
                                   boolean isShowRefreshButton,
                                   boolean isShowSourceButton,
                                   boolean isShowContentProposalsOnExplicitCtrlSpace,
                                   GlobalsContainer globalsContainer,
                                   IConnectionProvider jconProvider) {
        
        this(parent, layoutData, contents, null, style, providerId, coreId, 
             isShowRefreshButton, isShowSourceButton, ContentProposalAdapter.PROPOSAL_INSERT,
             isShowContentProposalsOnExplicitCtrlSpace, globalsContainer, jconProvider);
    }
    
    /**
     * Creates combo box with given proposals as contents and optional descriptions.
     * Optionally also shows buttons 'Refresh' and 'Source'.
     * 
     * @param parent parent control
     * @param layoutData MIG layout data
     * @param contents contents of combo box and list of proposals for auto-completion. 
     *        May be null, in which case initial proposals are taken from GlobalsProvider.
     * @param descriptions list of descriptions for items in parameter <i>contents</i>. 
     *        Must be the same size as <i>contents</i>. May be null , in which 
     *        case initial descriptions are taken from GlobalsProvider.
     * @param style SWT style constants
     * @param globalsProvider 
     * @param providerId used to get the list of global items. If 'contents == null' 
     *        globals are retrieved in ctor, otherwise only on click to Refresh button.
     *        Must be one of GlobalsContainer.GC_... constants
     * @param coreId identifies core for core globals providers. Should be null
     *        if globals provider specified in <i>providerId</i> is not core specific.
     *        See class GlobalsContainer for more info on available providers.
     * @param isShowRefreshButton if true, button <i>Refresh</i> is shown next to 
     *        combo. 
     * @param isShowRefreshButton if true, button <i>Source</i> is shown next to 
     *        combo. This parameter should be true only for function providers,
     *        as winIDEA can currently not show source for types, vars and macros.
     * @param proposalAcceptanceStyle should be <i>ContentProposalAdapter.PROPOSAL_REPLACE</i>
     *        for proposal selected by user to replace existing content, or
     *        <i>ContentProposalAdapter.PROPOSAL_INSERT</i> for proposal selected 
     *        by user to be inserted at cursor position.    
     */
    public GlobalsSelectionControl(Composite parent, 
                                   Object layoutData, 
                                   String[] contents, 
                                   String[] descriptions,
                                   int style,
                                   String providerId,
                                   String coreId,
                                   boolean isShowRefreshButton,
                                   boolean isShowSourceButton,
                                   int proposalAcceptanceStyle,
                                   boolean isShowContentProposalsOnExplicitCtrlSpace,
                                   GlobalsContainer globalsContainer,
                                   IConnectionProvider conProvider) {

        m_globalsProviderId = providerId;
        m_coreId = coreId;
        m_globalsContainer = globalsContainer;
        m_conProvider = conProvider;
        
        m_panel = new Composite(parent, style);
        m_panel.setLayoutData(layoutData);

        MigLayout mig = new MigLayout("", "[grow, pref, pref]");
        m_panel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(m_panel);
        
        
        GlobalsProvider globalsProvider = m_globalsContainer.getProvider(m_globalsProviderId, 
                                                                         m_coreId);
        
        if (contents == null) {
            contents = getCachedGlobals(globalsProvider);
        }

        if (descriptions == null) {
            descriptions = getCachedDescriptions(globalsProvider);
        }
        
        
        m_comboBox = builder.combo(contents, "split, span, gapright 10, width 0:150, growx", SWT.BORDER | SWT.DROP_DOWN);
        m_comboBox.setVisibleItemCount(15);
        // this is a workaround for bug in SWT - if combo has no proposals and
        // the user tries to drop down the list, and then clicks button outside
        // the combo, only focus is transferred on the first click, and the second
        // click is required to activate the button.
        if (contents == null  ||  contents.length == 0) {
            m_comboBox.setItems(new String[]{m_emptyComboListText});
        }

        // see Eclipse help, Platform Plug-in Developer Guide | JFace UI Framework | Field Assist
        m_functionsProposals = ISysUIUtils.addComboContentProposalsAdapter(m_comboBox, 
                                                                           contents, 
                                                                           descriptions, 
                                                                           proposalAcceptanceStyle,
                                                                           isShowContentProposalsOnExplicitCtrlSpace);
        /* m_autocompleter = new AsystAutoCompleteField(m_comboBox,
                                                     new AsystComboContentAdapter(),
                                                      // new ComboContentAdapter(), 
                                                      contents,
                                                      descriptions,
                                                      proposalAcceptanceStyle); */ 
        if (isShowRefreshButton) {
            m_refreshButon = builder.button("", "");
            
            m_refreshButon.setImage(getRefreshIcon());
            m_refreshButon.setToolTipText("Refresh globals.\nPress this button to get the latest list of items from winIDEA.\n" +
            "Press it also after recompiling the changed source code.");

            final IIConnectOperation refreshOperation = new IIConnectOperation() {

                @Override
                public void exec(JConnection jCon) {
                    // refresh all global items, so that user doesn't have to click Refresh 
                    // several times. If it will be a performance problem in the future,
                    // fine tune it.
                    m_globalsContainer.refresh();
                    
                    GlobalsProvider globalsProvider = 
                            m_globalsContainer.getProvider(m_globalsProviderId, m_coreId);
                    
                    if (globalsProvider != null) {

                        String [] globals = globalsProvider.getCachedGlobals(); 
                        setProposals(globals, globalsProvider.getCachedDescriptions());
                    }
                }
                
                @Override
                public void setData(Object data) {}
            };

            m_refreshButon.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    JConnection jCon = m_conProvider.getDefaultConnection();
                    ISysUIUtils.execWinIDEAOperation(refreshOperation, 
                                                     m_refreshButon.getShell(), 
                                                     jCon);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
        }
        
        m_showSourceOperation = new IIConnectOperation() {
            
            boolean m_isFunction = true;
            
            @Override
            public void exec(JConnection jCon) {
                String text = m_comboBox.getText().trim();
                if (!text.isEmpty()) { 
                    String coreId = "";
                    if (m_coreId != null) { // m_coreId is null, when globals provider is not core specific 
                        coreId = m_coreId;
                    }
                    if (Character.isDigit(text.charAt(0))) {
                        WinIDEAManager.showSourceAtAddressInEditor(jCon.getMccMgr().getConnectionMgr(coreId),
                                                                   text);
                    } else {
                        if (m_isFunction) {
                            WinIDEAManager.showFunctionInEditor(jCon.getMccMgr().getConnectionMgr(coreId), 
                                                                text);
                        } else { 
                            WinIDEAManager.showSourceInEditor(jCon.getMccMgr().getConnectionMgr(coreId), 
                                                              text, 1);
                        }
                    }
                }
            }
            
            @Override
            public void setData(Object data) {
                if (data instanceof Boolean) {
                    m_isFunction = ((Boolean) data).booleanValue();
                }
            }
        };
        
        if (isShowSourceButton) {
            m_showSourceBtn = ISysUIUtils.createShowSourceButton(builder, m_showSourceOperation, 
                                                                 "",
                                                                 conProvider);
        }
    }

    
    private Image getRefreshIcon() {
        
        if (m_refreshImg == null) {
            ImageDescriptor descriptor = 
                    AbstractUIPlugin.imageDescriptorFromPlugin(ISysCommonsPlugin.PLUGIN_ID,
                            "resources/icons/enabled/refresh.gif");

            if (descriptor != null) {
                m_refreshImg = descriptor.createImage();
            }
        }
        
        return m_refreshImg;
    }
    
    
    private String[] getCachedDescriptions(GlobalsProvider globalsProvider) {
        String[] descriptions;
        if (globalsProvider != null) {
            try {
                // autofill contents
                descriptions = globalsProvider.getCachedDescriptions();
            } catch (Exception ex) {
                // Exception is ignored here. User may press Refresh button 
                // later to see the error message. It is important to show
                // the dialog, even if the combo is empty.
                descriptions = new String[0];
            }
        } else {
            descriptions = new String[0];
        }

        if (descriptions == null) {
            descriptions = new String[0];
        }
        return descriptions;
    }

    
    private String[] getCachedGlobals(GlobalsProvider globalsProvider) {
        String[] contents;
        if (globalsProvider != null) {
            try {
                // autofill contents
                contents = globalsProvider.getCachedGlobals();
            } catch (Exception ex) {
                // Exception is ignored here. User may press Refresh button 
                // later to see the error message. It is important to show
                // the dialog, even if the combo is empty.
                contents = new String[0];
            }
        } else {
            contents = new String[0];
        }
        
        if (contents == null) {
            contents = new String[0];
        }
        
        return contents;
    }

    
    public void setEnbledShowSourceBtn(boolean isEnabled) {
        m_showSourceBtn.setEnabled(isEnabled);
    }

    
    public void setGlobalsProvider(String providerId, String coreId) {
        m_globalsProviderId = providerId;
        m_coreId = coreId;
        
        GlobalsProvider globalsProvider = 
                m_globalsContainer.getProvider(m_globalsProviderId, m_coreId);
        
        String [] globals = getCachedGlobals(globalsProvider); 
        setProposals(globals, getCachedDescriptions(globalsProvider));
    }
    
    
    public void setToolTipText(String string) {
        UiTools.setToolTip(m_comboBox, string);
    }
    
    
    public void setText(String text) {
        m_comboBox.setText(text);
    }

    
    /**
     * Reads globals from globals provider and sets them as new proposals.
     */
    public void refreshProposals() {
        
        GlobalsProvider globalsProvider = m_globalsContainer.getProvider(m_globalsProviderId, 
                                                                         m_coreId);

        if (globalsProvider != null) {
            setProposals(globalsProvider.getCachedGlobals(), 
                         globalsProvider.getCachedDescriptions());
        }
    }
    
    
    public void setProposals(String []proposals, String [] descriptions) {
        if (proposals == null  ||  proposals.length == 0) {
            // this is a workaround for bug in SWT - if combo has no proposals and
            // the user tries to drop down the list, and then clicks button outside
            // the combo, only focus is transferred on the first click, and the second
            // click is required to activate the button.
            String oldText = m_comboBox.getText();  // store text, because it is cleared during setItems() 
            m_comboBox.setItems(new String[]{m_emptyComboListText});
            m_comboBox.setText(oldText);            // restore the original text
            m_functionsProposals.setProposals(proposals, descriptions);
            return;
        }
        m_functionsProposals.setProposals(proposals, descriptions);
        String oldText = m_comboBox.getText();  // store text, because it is cleared during setItems() 
        m_comboBox.setItems(proposals);
        m_comboBox.setText(oldText);            // restore the original text
    }

    
    /**
     * 
     */
    public void reverseFocusOrder() {
       m_panel.setTabList(new Control[]{m_showSourceBtn, m_refreshButon, m_comboBox}); 
    }
    
    public Composite getPanel() {
        return m_panel;
    }


    /** Returns the underlying combo box control. */
    public Combo getControl() {
        return m_comboBox;
    }


    public Button getRefreshButton() {
        return m_refreshButon;
    }
    
    
    public void setCoreId(String coreId) {
        m_coreId = coreId;
    }


    public void setEmptyComboListText(String emptyComboListText) {
        m_emptyComboListText = emptyComboListText;
    }


    public void setShowFunctionSource(boolean isShowFunction) {
        m_showSourceOperation.setData(Boolean.valueOf(isShowFunction));
    }
}
