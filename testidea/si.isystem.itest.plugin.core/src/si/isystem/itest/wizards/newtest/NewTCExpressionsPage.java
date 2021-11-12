package si.isystem.itest.wizards.newtest;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.connect.CTestAssert;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.IVariable.EType;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.data.JFunction;
import si.isystem.connect.data.JVariable;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.sequence.InsertToSequenceAction;
import si.isystem.itest.ui.spec.data.VariablesSelectionTree;
import si.isystem.ui.utils.KGUIBuilder;

public class NewTCExpressionsPage extends GlobalsWizardDataPage {

    private static final String PAGE_TITLE = "Expressions";

    private VariablesSelectionTree m_varSelectionTree;
    

    public NewTCExpressionsPage(NewTCWizardDataModel ntcModel) {
        super(PAGE_TITLE);
        setTitle(PAGE_TITLE);
        setDescription("Select variables to be used for test verification. "
                + "Expressions can be finished later in section 'Expected'.");
        
        m_ntcModel = ntcModel;
    }
   

    @Override
    public void createControl(Composite parent) {
        setControl(createPage(parent));
    }
    
    
    @Override
    public Composite createPage(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        // wizard dialog size is set in handler
        container.setLayout(new MigLayout("fill", "[fill]", "[fill][min!]"));

        KGUIBuilder builder = new KGUIBuilder(container);

        m_varSelectionTree = new VariablesSelectionTree(m_ntcModel.m_coreId);
        m_varSelectionTree.createControl(builder, 
                                         "wrap", 
                                         "gaptop 5");
        return container;        
    }
    
    
    @Override
    public void setErrorMessage(String message) {
        super.setErrorMessage(message);
        setPageComplete(message == null);
    }


    @Override
    public void dataToModel() {
        m_ntcModel.m_varsForExpressions = m_varSelectionTree.getData();
    }


    @Override
    public void dataFromModel() {
        FunctionGlobalsProvider funcGlobalProvider = GlobalsConfiguration.instance().
            getGlobalContainer().getFuncGlobalsProvider(m_ntcModel.m_coreId);
        
        JFunction jFunc = NewTCVariablesPage.testGlobalFuncExistence(funcGlobalProvider, 
                                                                     m_ntcModel.m_funcUnderTestName);
        
        String retValType = jFunc.getReturnTypeName();
        StrStrMap localVars = new StrStrMap();
        localVars.put(m_ntcModel.m_retValVarName, retValType);
        
        List<String> allFuncVars = new ArrayList<>();
        allFuncVars.add(m_ntcModel.m_retValVarName);
        addOutParams(jFunc.getParameters(), localVars, allFuncVars);

        // TODO add global variables with write access
        m_varSelectionTree.setLocalVars(localVars);
        m_varSelectionTree.refreshHierarchy(allFuncVars);
    }
    
    
    private void addOutParams(JVariable[] jParams, 
                              StrStrMap localVarsMap, 
                              List<String> allFuncVars) {
        
        List<String> paramsList = DataUtils.splitToList(m_ntcModel.m_parameters);

        int numParams = Math.min(jParams.length, paramsList.size());
        for (int paramIdx = 0; paramIdx < numParams; paramIdx++) {
            JVariable jParam = jParams[paramIdx];
            EType paramType = jParam.getType();
            if (paramType == EType.tArray  
                    ||  paramType == EType.tPointer  
                    ||  paramType == EType.tReference) {
                
                String paramName = paramsList.get(paramIdx);
                localVarsMap.put(paramName, jParam.getVarTypeName());
                allFuncVars.add(paramName);
            }
        }
    }
    
    
    @Override
    public AbstractAction createModelChangeAction(CTestSpecification testSpec) {
        GroupAction grpAction = new GroupAction("Add variables for expressions");
        CTestAssert asserts = testSpec.getAssert(false);
        
        for (String var : m_ntcModel.m_varsForExpressions) {
            YamlScalar scalar = YamlScalar.newListElement(CTestAssert.ESectionAssert.E_SECTION_ASSERT_EXPRESSIONS.swigValue(), 
                                                          -1);
            scalar.setValue(var);
            grpAction.add(new InsertToSequenceAction(asserts, scalar));            
        }
        
        return grpAction;
    }
}
