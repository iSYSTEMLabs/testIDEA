package si.isystem.itest.wizards.newtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.SWTX;
import de.kupzog.ktable.editors.ContentProposalConfig;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.commons.globals.VariablesGlobalsProvider;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.ETristate;
import si.isystem.connect.IVariable.EType;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.common.ktableutils.IKTableModelChangedListener;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.mapping.InsertToUserMappingAction;
import si.isystem.itest.ui.spec.data.VariablesSelectionTree;
import si.isystem.tbltableeditor.CellEditorTristate;
import si.isystem.tbltableeditor.TristateCellRenderer;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.FontProvider;
import si.isystem.ui.utils.KGUIBuilder;

public class NewTCVariablesPage extends GlobalsWizardDataPage {

    private KTable m_varsTable;
    private DeclTableModel m_varsTableModel;

    private VariablesSelectionTree m_varSelectionTree;
    
    public static final String PAGE_TITLE = "Variables";


    public NewTCVariablesPage(NewTCWizardDataModel ntcModel) {
        super(PAGE_TITLE);
        setTitle(PAGE_TITLE);
        setDescription("Select variables to be declared as test local variables "
                + "(used as f. parameters), and select variables to be initialized. "
                + "Initialization values should be entered later in section 'Variables'.");
        
        m_ntcModel = ntcModel;
    }
   

    @Override
    public void createControl(Composite parent) {
        setControl(createPage(parent));
    }
    
    
    @Override
    public Composite createPage(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);

        KGUIBuilder builder = new KGUIBuilder(container);

        // wizard dialog size is set in handler
        container.setLayout(new MigLayout("fill", "", "[min!][fill][min!]"));

        builder.label("Declarations of test local variables:");
        builder.label("Variables for initialization section:", "wrap");
        m_varsTable = new KTable(container, true, SWT.H_SCROLL | SWT.V_SCROLL | 
                                 SWTX.EDIT_ON_KEY | 
                                 SWTX.MARK_FOCUS_HEADERS | 
                                 SWTX.FILL_WITH_DUMMYCOL | 
                                 SWT.BORDER);

        int minCellHeight = FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
        int cellWidthForCreateCol = FontProvider.instance().getTextWidth(m_varsTable, 
                "_Create_");
        int typCellWidth = FontProvider.instance().getTextWidth(m_varsTable, 
                "_ Return val. _") * 3;

        m_varsTableModel = new DeclTableModel(m_ntcModel,
                                              minCellHeight, 
                                              cellWidthForCreateCol,
                                              typCellWidth);

        m_varsTableModel.addModelChangedListener(new IKTableModelChangedListener() {
            @Override
            public void modelChanged(AbstractAction action, CTestBase testBase, boolean isRedrawNeeded) {
                StrStrMap testLocalVarsMap = 
                        DataUtils.listsToStrStrMap(m_ntcModel.m_varNames, 
                                                   m_ntcModel.m_varTypes);
                m_varSelectionTree.setLocalVars(testLocalVarsMap);

                // TODO add also global vars to this list
                m_varSelectionTree.refreshHierarchy(m_ntcModel.m_varNames);        
            }
        });

        m_varsTable.setModel(m_varsTableModel);
        m_varsTable.setLayoutData("gapright 20, wmin 55%");

        m_varSelectionTree = new VariablesSelectionTree(m_ntcModel.m_coreId);
        m_varSelectionTree.createControl(builder, 
                                         "w 45%, wrap", 
                                         "gaptop 5, skip");
        return container;
    }        
    
    
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }


    @Override
    public void dataToModel() {
        m_ntcModel.m_initVars = m_varSelectionTree.getData();
    }


    @Override
    public void dataFromModel() {

        m_varsTableModel.configureContentProposals();
        m_ntcModel.m_varNames.clear();
        m_ntcModel.m_varTypes.clear();
        m_ntcModel.m_isDeclVar.clear();
        
        VariablesGlobalsProvider varsGlobalsProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getVarsGlobalsProvider(m_ntcModel.m_coreId);
        
        FunctionGlobalsProvider funcGlobalProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getFuncGlobalsProvider(m_ntcModel.m_coreId);
        
        JFunction jFunc = testGlobalFuncExistence(funcGlobalProvider, 
                                                  m_ntcModel.m_funcUnderTestName);
        JVariable[] funcParams = jFunc.getParameters();
        
        List<String> paramsList = DataUtils.splitToList(m_ntcModel.m_parameters);
        int paramIdx = 0;
        Map<String, String> varsTree = new TreeMap<>();
        StrStrMap testLocalVarsMap = new StrStrMap();
        
        for (String parameter : paramsList) {

            if (!parameter.isEmpty()  &&  !Character.isDigit(parameter.charAt(0))) {

                Map<String, JVariable> varsMap = varsGlobalsProvider.getCachedVariablesMap();
                JVariable jVariable = varsMap.get(parameter);
                if (jVariable == null  &&  paramIdx < funcParams.length) {
                    // there is no global var with that name, so get parameter type
                    jVariable = funcParams[paramIdx];
                    String paramType = jVariable.getVarTypeName();
                    // there is no global var with this name, so we'll suggest user to 
                    // declare a test local var
                    m_ntcModel.m_varNames.add(parameter);
                    m_ntcModel.m_varTypes.add(paramType);
                    m_ntcModel.m_isDeclVar.add(Boolean.TRUE);
                    testLocalVarsMap.put(parameter, paramType);
                }

                // add globals and test locals for func params to vars tree 
                if (jVariable != null) {
                    String paramType = jVariable.getVarTypeName();
                    EType paramTypeAsEnum = jVariable.getType();

                    if (paramTypeAsEnum != EType.tFunction  &&  paramTypeAsEnum != EType.tSimple) {
                        varsTree.put(parameter, paramType);
                    }
                } 
            }
            
            paramIdx++;
        }
        
        m_varSelectionTree.setLocalVars(testLocalVarsMap);
        
        // TODO add also global vars to this list
        m_varSelectionTree.refreshHierarchy(m_ntcModel.m_varNames);        
    }


    @Override
    public AbstractAction createModelChangeAction(CTestSpecification testSpec) {
        
        GroupAction grpAction = new GroupAction("Add test local var declarations");
        for (int idx = 0; idx < m_ntcModel.m_varNames.size(); idx++) {
            if (m_ntcModel.m_isDeclVar.get(idx)) {
                int sectionId = SectionIds.E_SECTION_LOCALS.swigValue();
                YamlScalar pair = YamlScalar.newUserMapping(sectionId, 
                                                            m_ntcModel.m_varNames.get(idx));
                pair.setValue(m_ntcModel.m_varTypes.get(idx));

                grpAction.add(new InsertToUserMappingAction(testSpec, pair, null));
            }            
        }
        
        for (int idx = 0; idx < m_ntcModel.m_initVars.size(); idx++) {
            
        }
        for (String initVar : m_ntcModel.m_initVars) {
            int sectionId = SectionIds.E_SECTION_INIT.swigValue();
            YamlScalar pair = YamlScalar.newUserMapping(sectionId, initVar);
            pair.setValue("0");
            grpAction.add(new InsertToUserMappingAction(testSpec, pair, null));
        }

        return grpAction;
    }

    
    static JFunction testGlobalFuncExistence(FunctionGlobalsProvider funcGlobalProvider,
                                             String funcName) {
        try {
            JFunction jFunc = funcGlobalProvider.getCachedFunction(funcName);
            if (jFunc == null) {
                String msg = "Function not found in symbols: \n    " + 
                        funcName + "\n\nMake sure symbols are refreshed (iTools | Refresh)!";
                MessageDialog.openError(Activator.getShell(), 
                                        "Can not get data for wizard!", msg);
                throw new SIllegalStateException(msg);
            }
            return jFunc;
        } catch (Exception ex) {
            MessageDialog.openError(Activator.getShell(), 
                                    "Can not get data for wizard!", SEFormatter.getInfo(ex));
            throw ex;
        }
    }
}


class DeclTableModel extends KTableModelAdapter {

    final private String[] m_headers = {"Variable", "Type", "Create"};
    final private int NUM_COLS = 3;
    final private int NUM_HDR_ROWS = 1;
    final private int NUM_HDR_COLS = 1;
    final private int m_minCellHeight;
    final private int m_cellWidthForCreateCol;
    final private int m_typCellWidth;

    final private int VAR_NAME_COL_IDX = 0;
    final private int VAR_TYPE_COL_IDX = 1;
    final private int CREATE_COL_IDX = 2;
    private CellEditorTristate m_tristateEditor;
    private AsystContentProposalProvider m_typeProposals;

    private NewTCWizardDataModel m_ntcModel;
    private List<IKTableModelChangedListener> m_modelChangedListener = new ArrayList<>();
    

    DeclTableModel(NewTCWizardDataModel ntcModel,
                   int minCellHeight, int cellWidthForCreateCol, int typCellWidth) {
        
        m_ntcModel = ntcModel;
        m_minCellHeight = minCellHeight;
        m_cellWidthForCreateCol = cellWidthForCreateCol;
        m_typCellWidth = typCellWidth;
        
        Rectangle bounds = TristateCellRenderer.IMAGE_CHECKED.getBounds();
        m_tristateEditor = new CellEditorTristate(new Point(bounds.width, 
                                                            bounds.height), 
                                                  SWTX.ALIGN_HORIZONTAL_CENTER, 
                                                  SWTX.ALIGN_VERTICAL_CENTER);
   }
    
    
    @Override
    public int doGetRowCount() {
        return NUM_HDR_ROWS + m_ntcModel.m_varNames.size();
    }

    
    @Override
    public int doGetColumnCount() {
        return NUM_COLS;
    }
    
    
    @Override
    public int getInitialColumnWidth(int column) {
        if (column == CREATE_COL_IDX) {
            return m_cellWidthForCreateCol;
        }
        return m_typCellWidth;
    }

    
    @Override
    public int getInitialRowHeight(int row) {
        return m_minCellHeight;
    }
    
    
    @Override
    public Object doGetContentAt(int col, int row) {
        if (row == 0) {
            return m_headers[col];
        }
        
        int dataRow = row - NUM_HDR_ROWS;
        switch (col) {
        case VAR_NAME_COL_IDX:
            return m_ntcModel.m_varNames.get(dataRow);
        case VAR_TYPE_COL_IDX:
            return m_ntcModel.m_varTypes.get(dataRow);
        case CREATE_COL_IDX:
            TextIconsContent value = new TextIconsContent();
            value.setTristateValue(m_ntcModel.m_isDeclVar.get(dataRow).booleanValue() ? 
                                       ETristate.E_TRUE.name() : ETristate.E_FALSE.name());
            return value;
        }
        
        return "";
    }

    
    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {
        if (col == VAR_TYPE_COL_IDX) {
            ContentProposalConfig cfg = new ContentProposalConfig(new String[0]);
            UiUtils.setContentProposalsConfig(cfg);
            cfg.setProposalProvider(m_typeProposals);
            cfg.setProposalsAcceptanceStyle(m_typeProposals.getProposalsAcceptanceStyle());
            
            return new KTableCellEditorText2(cfg);
        }
        if (col == CREATE_COL_IDX) {
            return m_tristateEditor;
        }
        return null;
    }

    
    @Override
    public void doSetContentAt(int col, int row, Object value) {
        int dataRow = row - NUM_HDR_ROWS;
        switch (col) {
        case VAR_NAME_COL_IDX:
            break; // parameter name can not be changed here, only on the first page
        case VAR_TYPE_COL_IDX:
            m_ntcModel.m_varTypes.set(dataRow, value.toString());
            break;
        case CREATE_COL_IDX:
            m_ntcModel.m_isDeclVar.set(dataRow, Boolean.parseBoolean(value.toString()));
        }
        
        notifyListeners();
    }

    
    private void notifyListeners() {
        for (IKTableModelChangedListener listener : m_modelChangedListener) {
            listener.modelChanged(null, null, true);
        }
    }


    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        // var name is not editable, since it was defines on the first page as
        // parameter name
        if (col < NUM_HDR_COLS  ||  row < getFixedHeaderRowCount()) {
            return m_headerCellRenderer;
        }
        
        if (col == CREATE_COL_IDX) {
            return new TristateCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
        }
        
        return m_textCellRenderer;
    }
    
    
    void configureContentProposals() {
        GlobalsProvider globalTypeProvider = GlobalsConfiguration.instance().
                getGlobalContainer().getTypesGlobalsProvider(m_ntcModel.m_coreId);

        m_typeProposals = 
            new AsystContentProposalProvider(globalTypeProvider.getCachedGlobals(), 
                                             globalTypeProvider.getCachedDescriptions());
        m_typeProposals.setFiltering(true);
        m_typeProposals.setProposalsAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

        
    }
    
    
    public void addModelChangedListener(IKTableModelChangedListener listener) {
        m_modelChangedListener.add(listener);
    }
}