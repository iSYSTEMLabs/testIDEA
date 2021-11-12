package si.isystem.itest.preferences.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.WorkbenchPart;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CYAMLUtil;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ActionQueue;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.IEventDispatcher;
import si.isystem.itest.model.ITestSpecModelListener;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.tbltableeditor.TestBaseListTable;

/**
 * This class provides support for CTestBase table editor (like the one for stub 
 * steps and test init sequence).
 * 
 * @author markok
 */
public class PrefsTBTableEditor extends FieldEditor implements IActionExecutioner,
                                                               IEventDispatcher {

    private boolean m_isInitialized = false;
    private String m_layoutData;
    private String m_tooltip;
    private TestBaseListTable m_testBaseTable;
    private ActionQueue m_actionQueue = new ActionQueue(this);
    private CTestBase m_editorTestBase; // tmp test base for dialog
    private int m_section;
    private WorkbenchPart m_viewPart;
    private List<ITestSpecModelListener> m_listeners;
    private IValidator m_validator;
    private boolean m_isValid;
    private Map<String, IContentProposalProvider> m_contentProvidersMap;
    
    public PrefsTBTableEditor(int name, String labelText, 
                              Composite parent, 
                              CTestBase testBase,
                              int section,
                              String layout,
                              String tooltip,
                              WorkbenchPart viewPart,
                              Map<String, IContentProposalProvider> contentProvidersMap) {
        this(String.valueOf(name), labelText, parent, testBase, section, layout, 
                            tooltip, viewPart, contentProvidersMap);
    }

    
    public PrefsTBTableEditor(String name, String labelText, 
                              Composite parent, 
                              CTestBase testBase,
                              int section,
                              String layout,
                              String tooltip,
                              WorkbenchPart viewPart,
                              Map<String, IContentProposalProvider> contentProvidersMap) {
        
        super(name, labelText, parent);
        
        m_editorTestBase = testBase.createInstance(null);
        m_editorTestBase.assign(testBase);
        
        m_section = section;
        m_isInitialized = true;
        m_layoutData = layout;
        m_tooltip = tooltip;
        m_viewPart = viewPart;
        m_contentProvidersMap = contentProvidersMap;
        
        createControl(parent);
    }
    
    
    // methods, which override StringFieldEditor
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        throw new IllegalStateException("Method should not be called - Mig layout is used!");
    }
    
    
    @Override
    protected void adjustForNumColumns(int numColumns) {
        if (numColumns > 1) {
            m_testBaseTable.getControl().setLayoutData(" span " + numColumns + ", " + m_layoutData);
        }
    }


    @Override
    public int getNumberOfControls() {
        return 2;  // label and table
    }

    
    @Override
    protected void createControl(Composite parent) {
        if (!m_isInitialized) {
            return;
        }
        getLabelControl(parent);

        m_testBaseTable = new TestBaseListTable(this, false);
        
        Control stepsTableControl = 
                m_testBaseTable.createControl(parent,
                                             m_editorTestBase,
                                             m_section,
                                             ENodeId.FUNCTION_NODE, // not really needed here
                                             m_viewPart);
        
        for (Entry<String, IContentProposalProvider> pair : m_contentProvidersMap.entrySet()) {
            m_testBaseTable.addContentProvider(pair.getKey(), pair.getValue());
        }
        
        stepsTableControl.setLayoutData(m_layoutData);

        m_testBaseTable.setTooltip(m_tooltip + "\n\n" +

                           "Shortcuts:\n" +
                           "F2 - edit\n" +
                           "Esc - revert editing\n" +
                           "Del - delete cell contents\n" +
                           "Ctrl + num + - add column or row if column or row is selected\n" +
                           "Ctrl + num - - delete selected column or row\n" +
                           "Backspace - clear cell and start editing\n" +
                           "Ctrl + Space - column selection mode\n" +
                           "Ctrl + Space - row selection mode\n" +
                           "Ctrl + C, Ctrl + X,  Ctrl + V - standard clipboard shortcuts");
        
        // set table ID in caller (perfs page), not here
        // stepsTableControl.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
        //                          SWTBotConstants.PREFS_KTABLE_ID);
    }
    
    
    public Control getControl() {
        return m_testBaseTable.getControl();
    }
    
    
    public void setValidator(IValidator validator) {
        m_validator = validator;
    }


    @Override
    public boolean isValid() {
        return m_isValid;
    }
    
    
    private boolean checkState() {
        
        if (m_validator != null) {
            String result = m_validator.validate(m_editorTestBase, m_section);

            if (result == null) {
                clearErrorMessage();
                return true;
            }
            showErrorMessage(result);
            return false;
        }
        return true;
    }


    @Override
    protected void refreshValidState() {
        boolean oldState = m_isValid; 
        m_isValid = checkState();
        
        fireStateChanged(IS_VALID, oldState, m_isValid);
    }

    
    @Override
    protected void doLoad() {
        String yamlSpec = getPreferenceStore().getString(getPreferenceName());
        try {
            CYAMLUtil.parseTestBaseList(yamlSpec, m_editorTestBase, m_section);
            m_testBaseTable.refresh();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    
    @Override
    protected void doLoadDefault() {
        String yamlSpec = getPreferenceStore().getDefaultString(getPreferenceName());
        CYAMLUtil.parseTestBaseList(yamlSpec, m_editorTestBase, m_section);
        m_testBaseTable.refresh();
    }


    @Override
    protected void doStore() {
        String yamlSpec = m_editorTestBase.getTestBaseList(m_section, false).toString();
        getPreferenceStore().setValue(getPreferenceName(), yamlSpec);
    }
    
    
    @Override // from interface ActionExecutioner
    public void execAction(AbstractAction action) {
        m_actionQueue.exec(action);
        m_testBaseTable.refresh(); // also empty sequence elements get refreshed.
        
        fireEvent(new ModelChangedEvent(null));
        
        refreshValidState();
    }

    
    @Override
    public void fireEvent(ModelChangedEvent event) {
        
        if (m_listeners == null) {
            return;
        }
        
        for (ITestSpecModelListener listener : m_listeners) {
            listener.modelChanged(event);
        }
    }
    
    
    public void addModifyListener(ITestSpecModelListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<>();
        }
        m_listeners.add(listener);
    }
    
    
    public void removeModifyListener(ITestSpecModelListener listener) {

        if (m_listeners == null) {
            return;
        }
        
        m_listeners.remove(listener);
    }
}
