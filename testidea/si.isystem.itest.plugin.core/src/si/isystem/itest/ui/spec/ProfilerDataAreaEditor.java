package si.isystem.itest.ui.spec;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import si.isystem.commons.globals.GlobalsProvider;
import si.isystem.connect.CProfilerStatistics2;
import si.isystem.connect.CProfilerTestResult;
import si.isystem.connect.CTestAnalyzerProfiler;
import si.isystem.connect.CTestAnalyzerProfiler.EAreaType;
import si.isystem.connect.CTestAnalyzerProfiler.EProfilerSectionIds;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestProfilerStatistics;
import si.isystem.connect.CTestProfilerStatistics.EProfilerStatisticsSectionId;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.ktableutils.CTestBaseIdAdapter;
import si.isystem.itest.common.ktableutils.KTableModelForListItemIds;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetSectionAction;
import si.isystem.itest.ui.comp.ValueAndCommentEditor;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.data.VariablesContentProposal;
import si.isystem.swttableeditor.ITextFieldVerifier;
import si.isystem.ui.utils.KGUIBuilder;

public class ProfilerDataAreaEditor extends ProfilerCodeAreaEditor {

    
    // comment for 'value:' tag in 'dataAreas:' sections, left bottom to 'Area:' label
    protected ValueAndCommentEditor m_areaValueTagEditor;  

    public ProfilerDataAreaEditor() {
        super(EAreaType.DATA_AREA);

        setListCommentSectionId(EProfilerSectionIds.E_SECTION_DATA_AREAS.swigValue());
        
        m_uiStrings.m_tableTitle = "Variables";
        m_uiStrings.m_tableTooltip = "Names of variables to be profiled. "
                                     + "Select variable to view\n" 
                                     + "settings on the right.";
        
        m_statTableTooltip = "This table contains expected limits for profiler results.\n\n"
                + "Net time - time when the state is active.\n"
                // removed 31.8.2015 on Mkr's request + "Gross time - time when the state is " + 
                // "active plus time when it was preempted.\n"
                + "Inactive time - time spent outside a state - when the state is inactive.\n"                
                + "Period time - time between consecutive entries / writes.\n"
                + "\nLow, High - interval boundaries. Because measured times may vary for various reasons, we can specify\n"
                + "            interval of expected values.\n"
                
                + "\nMin - minimum time of all function invocations or state values.\n"
                + "Max - maximum time of all function invocations or state values.\n"
                + "Average - average time of all function invocations or state values.\n"
                + "Total - sum of all times for function invocations or state values.\n"
                + "Min Start/End - when the minimum time was recorded.\n"
                + "Max Start/End - when the maximum time was recorded.\n\n"
                
                + "All times may be entered with units, underscores may be used for better readability, for example:\n"
                + "  123 ns\n"
                + "  123 ms\n"
                + "  1_234_567\n"
                + "The following units are allowed: 'ns', 'us', 'ms', and 's'.\n"
                + "If no unit is specified, nano seconds are assumed.";
        
        CTestBaseIdAdapter adapter = 
                new CTestBaseIdAdapter(EProfilerStatisticsSectionId.E_SECTION_AREA_NAME.swigValue(),
                                       ENodeId.PROFILER_DATA_AREAS_NODE) {
            
            @Override
            public AbstractAction createSetIdAction(CTestBase testBase, String newId) {

                try {
                    String[] nameValue = splitAreaName(newId);

                    GroupAction grpAction = new GroupAction("Set profiler area name");
                    grpAction.add(super.createSetIdAction(testBase, nameValue[0]));

                    // set also value
                    YamlScalar value = 
                            YamlScalar.newValue(EProfilerStatisticsSectionId.E_SECTION_AREA_VALUE.swigValue());
                    if (nameValue.length == 2) { 
                        value.setValue(nameValue[1]);
                    } else {
                        value.setValue("");
                    }

                    SetSectionAction action = new SetSectionAction(testBase, 
                                                                   ENodeId.PROFILER_DATA_AREAS_NODE, 
                                                                   value);
                    grpAction.add(action);
                    return grpAction;
                } catch (Exception ex) {
                    SExceptionDialog.open(Activator.getShell(), 
                                          "Invalid variable name!", ex);
                    throw ex;
                }
            }
            
            
            @Override
            public String getId(CTestBase testBase) {
                CTestProfilerStatistics area = CTestProfilerStatistics.cast(testBase);
                return TestSpecificationModel.getFullProfilerAreaName(area, 
                                                                      EAreaType.DATA_AREA);
            }
            
            
            @Override
            public CTestBase createNew(CTestBase parentTestBase) {
                return new CTestProfilerStatistics(parentTestBase);
            }

            
            @Override
            public CTestBaseList getItems(boolean isConst) {
                if (m_currentTestSpec == null) {
                    return EMPTY_CTB_LIST;
                }
                return m_currentTestSpec.getAnalyzer(isConst)
                        .getProfiler(isConst).getAreas(EAreaType.DATA_AREA, isConst);
            }


            @Override
            public Boolean isError(int dataRow) {
                return isErrorInCodeProfilerStat(dataRow);
            }
        };
        
        setIdAdapter(adapter);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        Composite panel = super.createPartControl(parent);
        
        m_areaValueTagEditor = 
            ValueAndCommentEditor.newMixed(EProfilerStatisticsSectionId.E_SECTION_AREA_VALUE.swigValue(), 
                                           m_listItemIdLabel, 
                                           SWT.LEFT | SWT.BOTTOM);
        m_areaValueTagEditor.setCommentChangedListener(this);
        
        return panel;
    }


    @Override
    protected void createItemIdControls(KGUIBuilder builder) {
        builder.label("Variable:", "gapright 5"); 
        m_listItemIdLabel = builder.text("w 100::, span, growx, wrap", 
                                         SWT.BORDER);
        m_listItemIdLabel.setText(SELECT_ITEM_ON_THE_LEFT); 
        m_listItemIdLabel.setEditable(false);
    }

    
    @Override
    public boolean isError(CTestResult result) {
        return result.isProfilerDataError();
    }

    @Override
    public void copySection(CTestTreeNode destTestSpec) {
        
        CTestAnalyzerProfiler destProfiler = 
                CTestSpecification.cast(destTestSpec).getAnalyzer(false).getProfiler(false);
        
        CTestAnalyzerProfiler srcProfiler = m_testSpec.getAnalyzer(true).getProfiler(true); 
        destProfiler.assignDataAreas(srcProfiler);
    }
    
    
    @Override
    protected ITextFieldVerifier createVerifier() {
        
        ITextFieldVerifier verifier = new ITextFieldVerifier() {

            @Override
            public String verify(String[] data) {
                if (data[0].trim().length() == 0) {
                    return "Area name must not be empty!";
                }
                return null;
            }
            
            @Override
            public String format(String[] data) {
                
                String areaName = data[0].trim();
                // data area names may also specify value of the data, separated
                // by model.getDataAreaValueSeparator(), for example: 'myVar / 25'
                String nameValue[] = splitAreaName(areaName);
                
                switch (nameValue.length) {
                case 0:
                    return "Area name must not be empty!";
                case 1:
                    data[0] = areaName; // only trimmed
                    break;
                case 2:
                    String name = nameValue[0];
                    String value = nameValue[1];
                    if (name.length() == 0) {
                        return "Area name must not be empty!";
                    }
                    if (value.length() > 0) {
                        try {
                            long longValue = parseDecOrHex(value);
                            data[0] = name + " " + 
                                TestSpecificationModel.getDataAreaValueSeparator() 
                                + " " + longValue;
                        } catch (Exception ex) {
                            return "Value must be integer number!\n" + 
                                   ex.getMessage();
                        }
                    } else { // ignore separator
                        data[0] = name;
                    }
                    break;
                default:
                    return "Area may contain only one value separated by '" + 
                    TestSpecificationModel.getDataAreaValueSeparator() + "': '" +
                    areaName + "', but it contains " + nameValue.length + " items."; 
                }
                    
                return null;
            }            
        };
        
        return verifier;
    }


    @Override
    protected void fillAreaValueTag(String areaName, CTestProfilerStatistics area) {
        
        if (area == null) {
            m_areaValueTagEditor.updateValueAndCommentFromTestBase(null);
            m_areaValueTagEditor.setEnabled(false);
            return;
        }

        boolean isEnabled = false;
        if (!m_isInherited) {
            // is area name does not have value, then value comment icon must not be
            // enabled.
            isEnabled = !area.getAreaValue().isEmpty();
        }
        
        m_areaValueTagEditor.updateValueAndCommentFromTestBase(area);
        m_areaValueTagEditor.setEnabled(isEnabled);
    }
    
    
    @Override
    protected boolean fillResultNumbers(CTestProfilerStatistics area,
                                        CTestResult testCaseResult) {
        
        boolean isResultSet = false;
        
        if (area.isAreaValueSet()) {
            try {
                String name = area.getQualifiedAreaNameForStateVar(testCaseResult.getDefaultDownloadFile());
                CProfilerTestResult profilerResult = testCaseResult.getProfilerDataResult(name);
                if (profilerResult != null) {
                    if (profilerResult.isResultSet()) {
                        CProfilerStatistics2 profStatistic = profilerResult.getMeasuredResult();
                        m_statsTableModel.setResult(profilerResult);
                        m_hits.setResult(profStatistic.getNumHits(), profilerResult.validateHits());
                    } else {
                        // data areas with value always  
                        // have exactly one measurement, so this should never happen
                        m_statsTableModel.setResult(profilerResult);
                        m_hits.setResult(0, profilerResult.validateHits());                        
                    }
                    return true;
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                return false; // // invalid area name, no result to be set
            }
            
        } else {
            // Handle areas without specific value set. In Profiler 2
            // there is only one such area for state variable.
            String name = area.getQualifiedAreaName(testCaseResult.getDefaultDownloadFile());
            CProfilerTestResult result = testCaseResult.getProfilerDataResult(name);
            if (result != null) {
                isResultSet = true;
                m_statsTableModel.setResult(result);
                m_hits.setResult(result.getMeasuredHits(), result.validateHits());
            }
        }
        
        return isResultSet;
    }
    

    private Boolean isErrorInCodeProfilerStat(int dataRow) {

        CTestResult result = m_model.getResult(m_testSpec);

        if (result == null) {
            return null;
        }

        CTestBase cvrgStatTB = 
                m_currentTestSpec.getAnalyzer(true).getProfiler(true).getDataAreas(true).get(dataRow);

        CTestProfilerStatistics area = CTestProfilerStatistics.cast(cvrgStatTB);
        String defaultDlFile = result.getDefaultDownloadFile();
        String qualifiedAreaName = area.getQualifiedAreaNameForStateVar(defaultDlFile);

        CProfilerTestResult areaResult = result.getProfilerDataResult(qualifiedAreaName);
        return getResultStatus(areaResult);
    }


    @Override
    public void refreshGlobals() {
        
        if (m_listTableModel == null) {
            return; // does not exist yet because of lazy init.
        }
        
        GlobalsProvider vars = GlobalsConfiguration.instance().getGlobalContainer().getAllVarsProvider();

        m_listTableModel.setAutoCompleteProposals(KTableModelForListItemIds.DATA_COL_IDX,
                                                  new VariablesContentProposal(vars.getCachedGlobals(), 
                                                                               vars.getCachedDescriptions()), 
                                                  // insert to keep value
                                                  ContentProposalAdapter.PROPOSAL_INSERT);
    }
    
    
    public static long parseDecOrHex(String nameValue) {
        if (nameValue.startsWith("0x")) {
            return Long.parseLong(nameValue.substring(2), 16);
        } 
        
        return Long.parseLong(nameValue);
    }


    /** 
     * Returns array with one or two elements, the first one is area name, the
     * second one is area value, if present in input parameter.
     */
    public static String[] splitAreaName(String areaName) {
        
        areaName = areaName.trim();
        // data area names may also specify value of the data, separated
        // by TestSpecificationModel.getDataAreaValueSeparator(), for example: 'myVar / 25'
        String nameValue[] = areaName.split(TestSpecificationModel.getDataAreaValueSeparator());
        
        switch (nameValue.length) {
        case 1:
            nameValue[0] = nameValue[0].trim();
            break;
        case 2:
            nameValue[0] = nameValue[0].trim();
            nameValue[1] = nameValue[1].trim();
            break;
        default:
            throw new IllegalArgumentException("Data area should have name and optional " +
                    "value separated by '" + TestSpecificationModel.getDataAreaValueSeparator() + 
                    "', for example: 'myVar " + TestSpecificationModel.getDataAreaValueSeparator() + " 23'");
        }
        
        return nameValue;
    }
    
    
    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{CTestAnalyzerProfiler.EProfilerSectionIds.E_SECTION_DATA_AREAS.swigValue()};
    }
}
