package si.isystem.itest.common;

import org.apache.commons.lang3.ArrayUtils;

import si.isystem.commons.connect.IConnectionProvider;
import si.isystem.commons.globals.IGlobalsConfiguration;
import si.isystem.commons.globals.VariablesGlobalsProvider;
import si.isystem.connect.CMapAdapter;
import si.isystem.connect.CTestCase;
import si.isystem.connect.CTestPersistentVars;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.StrStrMapIterator;
import si.isystem.connect.StrVector;
import si.isystem.connect.CTestPersistentVars.EPersistVarsSections;

/**
 * This class provides proposals for target global variables, macros, and
 * test local variables. 
 */
public class VarsMacrosLocalsGlobalsProvider extends VariablesGlobalsProvider {
    
    private CTestSpecification m_testSpec;
    private boolean m_isAddRetValName;
    private boolean m_isAddLocals;
    private boolean m_isAddPersistentVars;
    private boolean m_isAddHostVars;


    public VarsMacrosLocalsGlobalsProvider(IConnectionProvider conProvider,
                                           String coreId,
                                           IGlobalsConfiguration globalsCfg,
                                           CTestSpecification testSpec,
                                           boolean isAddRetValName,
                                           boolean isAddLocals,
                                           boolean isAddPersitentVars,
                                           boolean isAddHostVars) {
        
        super(conProvider, coreId, globalsCfg);
        
        m_testSpec = testSpec;
        
        m_isAddRetValName = isAddRetValName;
        m_isAddLocals = isAddLocals;
        m_isAddPersistentVars = isAddPersitentVars;
        m_isAddHostVars = isAddHostVars;
    }

    
    @Override
    public String[] refreshGlobals() {
        super.refreshGlobals();

        int numProposals = 0;
        
        if (m_isAddRetValName) {
            numProposals++;
        }
        
        StrStrMap localVars = null;
        if (m_isAddLocals) {
            localVars = new StrStrMap(); 
            m_testSpec.getLocalVariables(localVars);
            numProposals += (int) localVars.size();
        }

        CMapAdapter persistVarsMap = null;
        if (m_isAddPersistentVars) {
            CTestPersistentVars persistVars = m_testSpec.getPersistentVars(true);
            persistVarsMap = new CMapAdapter(persistVars, 
                                             EPersistVarsSections.E_SECTION_DECL.swigValue(), 
                                             true);
            numProposals += persistVarsMap.size();
        }

        StrStrMap initVars = null;
        if (m_isAddHostVars) {
            initVars = new StrStrMap(); 
            m_testSpec.getInitMap(initVars); 

            numProposals += initVars.size();
        }
        
        String[] varNames = new String[numProposals];
        String[] varTypes = new String[numProposals];
        int idx = 0;
        
        if (m_isAddRetValName) {
            // add return value name to the list of proposals
            String retValName = m_testSpec.getFunctionUnderTest(true).getRetValueName();
            if (!retValName.isEmpty()) {
                varNames[idx] = retValName;
            } else {
                varNames[idx] = CTestCase.getISystemRetValName();
            }
            varTypes[idx] = "Function return value";
            idx++;
        }

        if (localVars != null) {
            StrStrMapIterator iter = new StrStrMapIterator(localVars);
            while (iter.isValid()) {
                String key = iter.key();
                varNames[idx] = key;
                varTypes[idx] = iter.value();
                idx++;
                iter.inc();
            }
        }
        
        // add persistent variables
        if (persistVarsMap != null) {
            StrVector keys = new StrVector();
            persistVarsMap.getKeys(keys);
            int numKeys = (int) keys.size();
            for (int keyidx = 0; keyidx < numKeys; keyidx++) {
                String persistVar = keys.get(keyidx); 
                varNames[idx] = persistVar;
                varTypes[idx] = persistVarsMap.getValue(persistVar);
                idx++;
            }
        }
        
        // add host vars
        if (initVars != null) {
            StrStrMapIterator initIter = new StrStrMapIterator(initVars);
            while (initIter.isValid()) {
                String key = initIter.key();
                if (UiUtils.isHostVar(key)) {
                    varNames[idx] = key;
                    varTypes[idx] = initIter.value();
                    idx++;
                }
                initIter.inc();
            }
            
        }
        
        m_cachedGlobals = (String [])ArrayUtils.addAll(m_cachedGlobals, varNames);
        m_cachedDescriptions = (String [])ArrayUtils.addAll(m_cachedDescriptions, varTypes); 

        return m_cachedGlobals;
    }
}


