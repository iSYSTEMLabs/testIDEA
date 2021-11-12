package si.isystem.itest.preferences;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.preferences.controls.PrefBooleanEditor;
import si.isystem.ui.utils.AsystContentProposalProvider;

public class UIPrefsPage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

    private final static String PAGE_DESCRIPTION = 
            "Settings on this page define behavior of testIDEA user interface.\n" +
            "These settings are not part of the project - they are stored in testIDEA installation settings.";

    private static final String UI_PREFS_IS_SHOW_CONTENT_PROPOSALS_ON_EXPLICIT_CTRL_SPACE = 
                         "uiprefs.isShowContentProposalsOnExplicitCtrlSpace";

    private static final String UI_PREFS_IS_MAKE_DEFAULT_SELECTION_IN_CONTENT_PROPOSALS = 
                         "uiprefs.isMakeDefaultSelectionInContentProposals";
    
    private Composite m_fieldEditorPanel;

    private PrefBooleanEditor m_isShowContentProposalsOnExplicitCtrlSpace;
    private PrefBooleanEditor m_isMakeDefaultSelectionInContentProposals;
    
    /**
     * This ctor initializes the page from Eclipse preference store - it is called
     * by RCP when user edits preferences - used as a plugin.
     */
    public UIPrefsPage() {
        super(GRID);
        
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        
        setDescription(PAGE_DESCRIPTION +
                       "\nSettings can be modified only in the professional version of testIDEA!");
    }

    
    @Override
    protected Control createContents(Composite parent) {
        
        m_fieldEditorPanel = new Composite(parent, SWT.BORDER);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        gridData.widthHint = 400;
        m_fieldEditorPanel.setLayoutData(gridData);
        
        m_fieldEditorPanel.setLayout(new MigLayout("fillx", "[min!][fill][min!]"));
        
        createFieldEditors();

        initialize();
        checkState();
        return m_fieldEditorPanel;
    }


    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        // see the doc of FieldEditor class for the list of all available field editors 
        
        // builder.separator("span 3, growx, gaptop 15, gapbottom 15, wrap", SWT.HORIZONTAL);

        m_isShowContentProposalsOnExplicitCtrlSpace = 
                new PrefBooleanEditor(UI_PREFS_IS_SHOW_CONTENT_PROPOSALS_ON_EXPLICIT_CTRL_SPACE, 
                                      "Show content proposals only on Ctrl-Space key stroke",
                                      BooleanFieldEditor.DEFAULT,
                                      parent,
                                      "wrap",
                                      "If checked, content proposals for identifiers will be shown only when you\n"
                                      + "press Ctrl-Space, not on any keypress.");
        addField(m_isShowContentProposalsOnExplicitCtrlSpace);

        m_isMakeDefaultSelectionInContentProposals = new PrefBooleanEditor(UI_PREFS_IS_MAKE_DEFAULT_SELECTION_IN_CONTENT_PROPOSALS, 
                                                              "Make default selection on content proposals.",
                                                              BooleanFieldEditor.DEFAULT,
                                                              parent,
                                                              "wrap",
                                                              "If checked, then the first item in content proposals list is selected when the\n"
                                                              + "list opens. Pressing ENTER inserts the selected item into input field.\n"
                                                              + "If not checked, nothing is selected by default. Pressing ENTER does not insert\n"
                                                              + "anything. Use arrow keys to select an item.");
        addField(m_isMakeDefaultSelectionInContentProposals);

        Label lbl = new Label(parent, SWT.NONE);
        lbl.setText("Note: testIDEA may need to be restarted for settings on this page to be applied for all components.");
        lbl.setLayoutData("gaptop 20");

        enableComponents(true);
    }


    private void enableComponents(boolean isEnabled) {
        final Composite parent = getFieldEditorParent();
        m_isShowContentProposalsOnExplicitCtrlSpace.setEnabled(isEnabled, parent);
        m_isMakeDefaultSelectionInContentProposals.setEnabled(isEnabled, parent);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    
    @Override
    protected Composite getFieldEditorParent() {
        return m_fieldEditorPanel;
    }

    
    @Override
    protected void performApply() {
        AsystContentProposalProvider.setMakeDefaultSelectionInContentProposals(
                  m_isMakeDefaultSelectionInContentProposals.getBooleanValue());
        super.performApply();
    }
    

    public static boolean isShowContentProposalsOnExplicitCtrlSpace() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();

        if (prefs != null) {
            return prefs.getBoolean(UI_PREFS_IS_SHOW_CONTENT_PROPOSALS_ON_EXPLICIT_CTRL_SPACE);
        }
        return false;
    }


    public static boolean isMakeDefaultSelectionInContentProposals() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();

        if (prefs != null) {
            return prefs.getBoolean(UI_PREFS_IS_MAKE_DEFAULT_SELECTION_IN_CONTENT_PROPOSALS);
        }
        return false;
    }
}
