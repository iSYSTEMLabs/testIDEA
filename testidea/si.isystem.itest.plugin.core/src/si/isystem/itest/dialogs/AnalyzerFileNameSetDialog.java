package si.isystem.itest.dialogs;

import java.util.EnumSet;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.handlers.FilePropertiesCmdHandler;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.ui.utils.KGUIBuilder;

public class AnalyzerFileNameSetDialog  extends Dialog {

    public enum EExistingState {
        EAll, 
        EOnlyEmpty;
        
        private Button m_button;
        // static member is not as flexible as external listener (see previous
        // commit in SVN), but if the same enum is not used for two groups of
        // radio buttons it works fine.
        static EExistingState m_selected = EAll;
        
        // enum has selection to the button, so that it can select previous values
        // on dialog creation.
        void setButton(Button button) {
            m_button = button;
            m_button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_selected = EExistingState.this;
                }
                
                @Override public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
        
        // used to maintain persistence between invocations
        static void setSelection() {
            m_selected.m_button.setSelection(true);
        }
    };

    
    public enum EScope {
        EAll, 
        ESelected, 
        ESelectedAndDerived;
        
        private Button m_button;
        static EScope m_selected = EAll;
        
        void setButton(Button button) {
            m_button = button;
            m_button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    m_selected = EScope.this;
                }
                
                @Override public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
        
        static void setSelection() {
            m_selected.m_button.setSelection(true);
        }
    }


    public enum EAnalyzerType {
        ETraceActive, 
        ECoverageActive, 
        EProfilerActrive;
        
        private Button m_button;
        static EnumSet<EAnalyzerType> m_selected = EnumSet.noneOf(EAnalyzerType.class);
        
        void setButton(Button button) {
            m_button = button;
            m_button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (m_button.getSelection()) {
                        m_selected.add(EAnalyzerType.this);
                    } else {
                        m_selected.remove(EAnalyzerType.this);
                    }
                }
                
                @Override public void widgetDefaultSelected(SelectionEvent e) {}
            });
        }
        
        static void setSelection() {
            for (EAnalyzerType analType : m_selected) {
                analType.m_button.setSelection(true);
            }
        }
    }


    private Label m_formatStringLbl;;
    

    public AnalyzerFileNameSetDialog(Shell parentShell) {
        super(parentShell);

        // make a dialog resizeable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.getShell().setText("Set analyzer file names");
        
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

        
        mainDlgPanel.setLayout(new MigLayout("fill", "[min!][fill][fill][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        /* builder.label("Select which IDs to set and the scope. Format of the automatically " +
                "generated IDs can be set in project Properties dialog, tab 'General'.", "gapbottom 10, wrap"); */
        
        builder.label("Analyzer file name:", "gapright 10");
        final CTestEnvironmentConfig envConfig = TestSpecificationModel.getActiveModel().getCEnvironmentConfiguration();
        m_formatStringLbl = builder.label(envConfig.getToolsConfig(true).getAnalyzerFName(), 
                                          "gapright 20, span 2, growx", SWT.BORDER);
        
        builder.button("Modify", "wrap").addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                FilePropertiesCmdHandler cmd = 
                        new FilePropertiesCmdHandler(FilePropertiesCmdHandler.TOOLS_PREFS_PAGE_ID);
                try {
                    cmd.execute(null);
                    m_formatStringLbl.setText(envConfig.getToolsConfig(true).getAnalyzerFName());
                } catch (Exception ex) {
                    SExceptionDialog.open(getParentShell(), "Error!", ex);
                }
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        KGUIBuilder scopeGroup = builder.group("Scope", "gaptop 15, span 2, growx");
        
        EScope.EAll.setButton(scopeGroup.radio("&All test cases", "wrap"));
        EScope.ESelected.setButton(scopeGroup.radio("Se&lected", "wrap"));
        EScope.ESelectedAndDerived.setButton(scopeGroup.radio("Selected and &derived", ""));

        
        KGUIBuilder group = builder.group("Set if existing file name is", "gaptop 15, wrap");
        EExistingState.EOnlyEmpty.setButton(group.radio("&Empty", "wrap"));
        EExistingState.EAll.setButton(group.radio("Empty or de&fined", "wrap"));
        group.label("");
        
        
        builder.label("Analyzer type:", "gaptop 15, gapright 10");
        
        EAnalyzerType.ETraceActive.setButton(builder.checkBox("&Trace", "gaptop 15"));
        EAnalyzerType.ECoverageActive.setButton(builder.checkBox("&Coverage", "gaptop 15, split 2"));
        EAnalyzerType.EProfilerActrive.setButton(builder.checkBox("&Profiler", 
                                                                  "gaptop 15, wrap"));

        selectButtons();
        
        builder.separator("span 4, growx, gaptop 15", SWT.HORIZONTAL);

        return composite;
    }
    
    
    private void selectButtons() {
        EExistingState.setSelection();
        EScope.setSelection();
        EAnalyzerType.setSelection();
    }


    public boolean show() {
        return open() == Window.OK;
    }


    public EExistingState getExistingStateCondition() {
        return EExistingState.m_selected;
    }


    public EScope getScope() {
        return EScope.m_selected;
    }

    
    public EnumSet<EAnalyzerType> getAnalyzerType() {
        return EAnalyzerType.m_selected;
    }
}
