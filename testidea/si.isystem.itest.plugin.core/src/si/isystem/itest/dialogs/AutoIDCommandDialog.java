package si.isystem.itest.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestHostVars;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.handlers.FilePropertiesCmdHandler;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.ui.utils.KGUIBuilder;

/**
 * This dialog is used to select which IDs to auto generate.
 * 
 * @author markok
 *
 */
public class AutoIDCommandDialog extends Dialog {

    public enum ESequenceStart {
        EZero, 
        EOne, 
        ENoOfTestCases, 
        ECustom;
        
        private Button m_button;
        // static member is not as flexible as external listener (see previous
        // commit in SVN), but if the same enum is not used for two groups of
        // radio buttons it works fine.
        static ESequenceStart m_selected = EZero;
        
        // enum has selection to the button, so that it can select previous values
        // on dialog creation.
        void setButton(Button button) {
            m_button = button;
            m_button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_selected = ESequenceStart.this;
                }
                
                @Override public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
        
        // used to maintain persistence between invocations
        static void setSelection() {
            m_selected.m_button.setSelection(true);
        }
    };

    
    public enum ETestIdSettingType {
        EAllIds, 
        EOnlyEmpty, 
        EOnlyUid, 
        EOnlyNonUid;
        
        private Button m_button;
        // static member is not as flexible as external listener (see previous
        // commit in SVN), but if the same enum is not used for two groups of
        // radio buttons it works fine.
        static ETestIdSettingType m_selected = EAllIds;
        
        // enum has selection to the button, so that it can select previous values
        // on dialog creation.
        void setButton(Button button) {
            m_button = button;
            m_button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_selected = ETestIdSettingType.this;
                }
                
                @Override public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
        
        // used to maintain persistence between invocations
        static void setSelection() {
            m_selected.m_button.setSelection(true);
        }
    };

    
    public enum ETestIdSettingScope {
        EAll, 
        ESelected, 
        ESelectedAndDerived;
        
        private Button m_button;
        static ETestIdSettingScope m_selected = EAll;
        
        void setButton(Button button) {
            m_button = button;
            m_button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_selected = ETestIdSettingScope.this;
                }
                
                @Override public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
        
        static void setSelection() {
            m_selected.m_button.setSelection(true);
        }
    }


    private Label m_formatStringLbl;
    private Text m_seqStartTxt;
    private String m_customStart = "";
    
    
    public AutoIDCommandDialog(Shell parentShell) {
        super(parentShell);

        // make a dialog resizeable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Set auto-generated testIDs");
        
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

        
        mainDlgPanel.setLayout(new MigLayout("fill", "[min!][fill]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        /* builder.label("Select which IDs to set and the scope. Format of the automatically " +
        		"generated IDs can be set in project Properties dialog, tab 'General'.", "gapbottom 10, wrap"); */
        
        builder.label("Auto ID format:", "gapright 10, split");
        m_formatStringLbl = builder.label(TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration().getAutoIdFormatString(), "gapright 20, growx", SWT.BORDER);
        
        builder.button("Modify", "wrap").addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                FilePropertiesCmdHandler cmd = new FilePropertiesCmdHandler(FilePropertiesCmdHandler.ENV_PREFS_PAGE_ID);
                try {
                    cmd.execute(null);
                    m_formatStringLbl.setText(TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration().getAutoIdFormatString());
                } catch (Exception ex) {
                    SExceptionDialog.open(getParentShell(), "Error!", ex);
                }
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        KGUIBuilder seqGroup = builder.group("Start for " + CTestHostVars.getRESERVED_SEQ() + ", and " + CTestHostVars.getRESERVED_DID(), 
                                             "gaptop 10, growx, wrap");
        ESequenceStart.EZero.setButton(seqGroup.radio("&0", ""));
        ESequenceStart.EOne.setButton(seqGroup.radio("&1", "gapleft 15"));
        ESequenceStart.ENoOfTestCases.setButton(seqGroup.radio("N&o. of test cases", "gapleft 15"));
        ESequenceStart.ECustom.setButton(seqGroup.radio("&Custom:", "gapleft 15"));
        m_seqStartTxt = seqGroup.text("w 100:100:100", SWT.BORDER);
        
        KGUIBuilder group = builder.group("Test ID Update Mode", "gaptop 15, wrap");
        
        ETestIdSettingType.EAllIds.setButton(group.radio("Set &all IDs", "wrap"));
        
        ETestIdSettingType.EOnlyEmpty.setButton(group.radio("Set only &empty IDs", "wrap"));
        
        ETestIdSettingType.EOnlyUid.setButton(group.radio("Set only &uid/uuid/seq/nid part. ('/' has to be used as a separator " +
                                   "for uid/uuid/seq/nid variables for this setting to work properly)", 
                                   "wrap"));
        
        ETestIdSettingType.EOnlyNonUid.setButton(group.radio("Set only &NON uid/uuid/seq/nid part. ('/' has to be used as a separator " +
                                      "for uid/uuid/seq/nid variables for this setting to work properly)", 
                                      "wrap"));
        
        
        KGUIBuilder scopeGroup = builder.group("Test ID Update Scope", "gaptop 15, growx, wrap");
        
        ETestIdSettingScope.EAll.setButton(scopeGroup.radio("All test cases in a &project", "wrap"));
        ETestIdSettingScope.ESelected.setButton(scopeGroup.radio("Se&lected test cases", "wrap"));
        ETestIdSettingScope.ESelectedAndDerived.setButton(scopeGroup.radio("Selected and &derived test cases", "wrap"));

        selectButtons();
        m_seqStartTxt.setText(m_customStart);
        
        builder.separator("span 2, growx, gaptop 15", SWT.HORIZONTAL);

        return composite;
    }
    
    
    private void selectButtons() {
        ESequenceStart.setSelection();
        ETestIdSettingType.setSelection();
        ETestIdSettingScope.setSelection();
    }


    public boolean show() {
        return open() == Window.OK;
    }


    @Override
    protected void okPressed() {
        if (ESequenceStart.m_selected == ESequenceStart.ECustom) {
            m_customStart = m_seqStartTxt.getText();
            try {
                Integer.parseInt(m_customStart);
                super.okPressed();
            } catch (Exception ex) {
                MessageDialog.openError(getShell(), "Invalid custom value for sequence start!", 
                                        "Custom value must be an integer, but it is: '" + m_customStart + "'");
            }
        } else {
            super.okPressed();
        }
    }


    public ESequenceStart getSequenceStartType() {
        return ESequenceStart.m_selected;
    }
    
    
    public String getCustomStart() {
        return m_customStart;
    }


    public ETestIdSettingType getTestIdSettingType() {
        return ETestIdSettingType.m_selected;
    }


    public ETestIdSettingScope getTestIdSettingScope() {
        return ETestIdSettingScope.m_selected;
    }
    
}
