package si.isystem.itest.wizards.newtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import si.isystem.connect.CTestFunction;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.DataUtils;

public class NewTCWizardDataModel {

    // first page
    public ETestScope m_testScope;
    public static boolean m_isAutoCreateId = true;  // make this field session persistent
    public String m_coreId = "";
    public String m_funcUnderTestName = "";
    public String m_parameters = "";
    public String m_retValVarName = "";
    public static boolean m_isDefaultExpr; // remember user's selection between dialog invocations
    public String m_valForDefaultExpr = "";
    public String m_expectExpr = "";
    
    // functions page
    class SectionCfg {
        boolean m_isCreated; // true if stub / test point, ... section should be created
        String[] m_itemParams;  // ret. val. , tpid, repl.func., ...
        boolean m_isNeedsContentProvider; // UI rendering flag
        
        SectionCfg(int numItemParams) {
            this(numItemParams, false);
        }
        
        SectionCfg(int numItemParams, boolean isNeedsContentProvider) {
            m_itemParams = new String[numItemParams];
            m_isNeedsContentProvider = isNeedsContentProvider;
            Arrays.fill(m_itemParams, "");
        }
        
        int getNumParams() {
            return m_itemParams.length;
        }
        
        int getNumColumns() {
            return m_itemParams.length + 1; // +1 for checkbox (m_isCreated)
        }
    }
    
    
    class FuncCfg {
        String m_functionName;
        // when func. is stubbed, remove called functions, disable cvrg, ...
        SectionCfg m_stub = new SectionCfg(1);  
        SectionCfg m_userStub = new SectionCfg(1, true); // content provider for replacement functions
        SectionCfg m_testPoint = new SectionCfg(1);
        SectionCfg m_coverage = new SectionCfg(2);
        SectionCfg m_profiler = new SectionCfg(2);

        SectionCfg [] m_sections = new SectionCfg[5];

        FuncCfg(String funcName) {
            
            m_functionName = funcName;
            
            m_sections[0] = m_stub;
            m_sections[1] = m_userStub;
            m_sections[2] = m_testPoint;
            m_sections[3] = m_coverage;
            m_sections[4] = m_profiler;
        }
        
        public String getFunctionName() {
            return m_functionName;
        }

        SectionCfg [] getSections() {
            return m_sections;
        }
    }
    
    List<FuncCfg> m_functionConfigs = new ArrayList<>();
    
    // vars page
    List<Boolean> m_isDeclVar = new ArrayList<>(); // if 'true' item will be added to decl table in Vars section
    List<String> m_varNames = new ArrayList<>();   // test local vars to be declared
    List<String> m_varTypes = new ArrayList<>();   // types for test local vars

    List<String> m_initVars;    // variables to be entered into init table in Vars section
    
    // expressions page
    List<String> m_varsForExpressions;
    
    
    public NewTCWizardDataModel(String defaultRetValName, ETestScope testScope) {
        m_retValVarName = defaultRetValName;
        m_testScope = testScope;
    }

    
    public static NewTCWizardDataModel createFromTestCase(CTestSpecification testSpec) {
        
        CTestFunction functionUnderTest = testSpec.getFunctionUnderTest(true);
        NewTCWizardDataModel ntcModel = new NewTCWizardDataModel(null, null);
        ntcModel.m_coreId = testSpec.getCoreId();
        ntcModel.m_funcUnderTestName = functionUnderTest.getName();
        ntcModel.m_retValVarName = functionUnderTest.getRetValueName();
        StrVector positionParams = new StrVector();
        functionUnderTest.getPositionParams(positionParams);
        List<String> listParams = DataUtils.strVectorToList(positionParams);
        
        // remove '&' or '*' from parameter name
        for (int idx = 0; idx < listParams.size(); idx++) {
            String paramName = listParams.get(idx);
            if (!paramName.isEmpty()  &&  (paramName.charAt(0) == '*'  ||  paramName.charAt(0) == '&')) {
                listParams.set(idx, paramName.substring(1));
            }
        }
        ntcModel.m_parameters = StringUtils.join(listParams, ",");
        return ntcModel;
    }

    
    int getNumDataColumnsInFuncTable() {

        FuncCfg funcCfg = new FuncCfg("");
        SectionCfg[] sections = funcCfg.getSections();
        int numDataColumns = 0;
        for (SectionCfg section : sections) {
            numDataColumns += section.getNumColumns();
        }
        return numDataColumns;
    }
    
    
    int [] getBelongsToCellForHeaderInFuncTable() {
        int numDataCols = getNumDataColumnsInFuncTable();
        int [] belongsToCell = new int[numDataCols];

        int hdrIdx = 0;
        int colIdx = 0;
        FuncCfg funcCfg = new FuncCfg("");
        SectionCfg[] sections = funcCfg.getSections();
        for (SectionCfg section : sections) {
            for (int idx = 0; idx < section.getNumColumns(); idx++) {
                belongsToCell[colIdx++] = hdrIdx;
            }
            hdrIdx = colIdx;
        }
        
        return belongsToCell;
    }
}
