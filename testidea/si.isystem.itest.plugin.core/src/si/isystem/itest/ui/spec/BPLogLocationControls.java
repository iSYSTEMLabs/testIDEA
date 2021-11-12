package si.isystem.itest.ui.spec;

import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CAddressController;
import si.isystem.connect.CLineDescription.EFileLocation;
import si.isystem.connect.CLineLocation;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestLog;
import si.isystem.connect.CTestPoint.ETestPointSections;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.ETristate;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.itest.dialogs.SourceLocationDialog;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.AssignTestObjectAction;
import si.isystem.itest.ui.comp.TBControl.EHControlId;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;

// Test point ID should be obtained dynamically, as user may edit ID in the
// list table end then immediately click Edit button.
interface LocationIDProvider {
    String getId();
}


public class BPLogLocationControls {
    
    private static final String HIT_COUNT_TOOLTIP = "Defines, how many times condition must be true to "
       + "stop the target.\n"
       + "For example, if hit count is set to 3, "
       + "breakpoint will stop execution 4th time "
       + "when hit and condition is true.\n"
       + "Empty condition always evaluates to true.";
    private CTestLocation m_location;
    private LocationIDProvider m_tpOrTcIdProvider = new LocationIDProvider() {
        @Override
        public String getId() {  // dummy implementation used during UI creation
            return "";
        }
    };
    
    protected String m_selectedItemId = "";
    protected Button m_showSourceBtn;
    
    private TBControlText m_conditionCountHC;
    private TBControlText m_conditionExprHC;
    private Label m_resourceNameLbl;
    private Text m_resourceNameTxt;
    private Text m_lineOrSearchTxt;
    private Text m_locationOffsetTxt;
 
    private Button m_editBtn;
    private LoggingControls m_loggingControls;
    private TBControlText m_condCountTPC;
    private TBControlText m_condExprTPC;
    
    private String m_coreId;
    private IIConnectOperation m_showSourceOperation;
    
    public BPLogLocationControls() {
    }


    /**
     * This method MUST be called each time coreID changes, usually in 
     * editor's fillControlls() method.
     */
    public void setCoreId(String coreId) {
        m_coreId = coreId;
        m_showSourceOperation.setData(new Object[]{m_location, 
                                                   m_tpOrTcIdProvider.getId(), 
                                                   m_coreId});
    }


    void createBPConditionControlsTPC(KGUIBuilder builder, 
                                      ENodeId nodeId, 
                                      int condCountSection,
                                      int condExprSection) {
        builder.label("Hit count:");
        m_condCountTPC = TBControlText.createForMixed(builder, 
                                           HIT_COUNT_TOOLTIP, 
                                           "w 55:55:55, split, span, growx, wmin 0, wrap", 
                                           condCountSection, 
                                           nodeId, 
                                           null,
                                           SWT.BORDER);
        
        builder.label("Cond. expr.:");
        m_condExprTPC = TBControlText.createForMixed(builder, 
                                          "Defines condition, which must be true to " +
                                          "stop the target on breakpoint.\n"
                                          + "If condition is not specified, it evaluates to true.\n"
                                          + "Host variables may be used in expression.", 
                                          "wmin 100, split, span, growx, wrap", 
                                          condExprSection, 
                                          nodeId, 
                                          null,
                                          SWT.BORDER);
    }
    
    
    void createBPConditionControls(KGUIBuilder builder, ENodeId nodeId) {
        builder.label("Hit count:");
        
        m_conditionCountHC = TBControlText.createForMixed(builder, 
                                                          HIT_COUNT_TOOLTIP, 
                                                          "w 55:55:55, split 2", // split to get cells for the label and text control
                                                          ETestPointSections.E_SECTION_CONDITION_COUNT.swigValue(), 
                                                          nodeId, 
                                                          EHControlId.ETestPointCondCount, 
                                                          SWT.BORDER);
        
        builder.label("Cond. expr.:", "gapleft 50");
        
        m_conditionExprHC = TBControlText.createForMixed(builder, 
                                                         "Defines condition, which must be true to " +
                                                                 "stop the target on test point.", 
                                                         "wmin 100, span, growx, wrap", 
                                                         ETestPointSections.E_SECTION_CONDITION_EXPR.swigValue(), 
                                                         nodeId, 
                                                         EHControlId.ETestPointCondExpr, 
                                                         SWT.BORDER);
    }

    
    void createLocationControls(KGUIBuilder parentBuilder, 
                                final ENodeId nodeId) {
        
        KGUIBuilder builder = parentBuilder.group("Location", 
                                                  "w 200::, span 3, growx, gaptop 10, wrap",
                                                  true, "fillx", "[min!][grow, fill][min!][min!]", null);
        
        m_resourceNameLbl = builder.label("Function:"); // may be modified to File in fillControls()
        m_resourceNameTxt = builder.text("growx", SWT.BORDER);
        m_resourceNameTxt.setEditable(false);
        m_editBtn = builder.button("Edit");
        
        m_editBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                CTestLocation locationCopy = new CTestLocation();
                locationCopy.assign(m_location);
                SourceLocationDialog dlg = new SourceLocationDialog(Activator.getShell(),
                                                                    locationCopy,
                                                                    m_tpOrTcIdProvider.getId(),
                                                                    nodeId == ENodeId.TEST_POINT_NODE,
                                                                    m_coreId);
                if (dlg.open() == Window.OK) {
                    AssignTestObjectAction action = new AssignTestObjectAction(m_location, 
                                                                               locationCopy, 
                                                                               nodeId);
                    if (action.isModified()) {
                        action.addDataChangedEvent(nodeId, m_location);
                        action.addFireEventType(EFireEvent.EXEC);
                        action.addFireEventType(EFireEvent.UNDO);
                        TestSpecificationModel.getActiveModel().execAction(action);
                        m_showSourceOperation.setData(new Object[]{m_location, 
                                                                   m_tpOrTcIdProvider.getId(), 
                                                                   m_coreId});
                    }
                }
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        m_showSourceOperation = 
                createShowTestPointSourceOperation(m_tpOrTcIdProvider.getId(),
                                                   m_location,
                                                   m_coreId);
        
        m_showSourceBtn = ISysUIUtils.createShowSourceButton(builder, 
                                                             m_showSourceOperation, 
                                                             "wrap",
                                                             ConnectionProvider.instance());

        m_lineOrSearchTxt = builder.text("span 4, growx, wrap", SWT.BORDER);
        m_lineOrSearchTxt.setEditable(false);
        
        m_locationOffsetTxt = builder.text("span 4, growx, wrap", SWT.BORDER);
        m_locationOffsetTxt.setEditable(false);
    }


    public static IIConnectOperation createShowTestPointSourceOperation(String testPointId,
                                                                        CTestLocation location,
                                                                        String coreId) {
        
        IIConnectOperation showSourceOperation = new IIConnectOperation() {
            
            private CTestLocation m_sourceLoc;
            private String m_tpId;
            private String m_coreId;
            
            @Override
            public void exec(JConnection jCon) {
                String resourceName = m_sourceLoc.getResourceName();
                if (resourceName.isEmpty()) {
                    return;
                }

                CAddressController addrCtrl = new CAddressController(jCon.getMccMgr().getConnectionMgr(m_coreId));
                CLineLocation lineLoc = addrCtrl.getSourceLocation(m_sourceLoc.getLineDescription(),
                                                                   m_tpId);
                
                WinIDEAManager.showSourceInEditor(jCon.getMccMgr().getConnectionMgr(m_coreId), 
                                                  lineLoc.getFileName(), 
                                                  (int)lineLoc.getLineNumber());
            }

            
            @Override
            public void setData(Object data) {
                Object[] parameters = (Object [])data;
                m_sourceLoc = (CTestLocation) parameters[0];
                m_tpId = (String)parameters[1];
                m_coreId = (String)parameters[2];                    
            }
        };
        
        showSourceOperation.setData(new Object[]{location, testPointId, coreId});
        
        return showSourceOperation;
    }


    void createLogControls(KGUIBuilder parentBuilder, ENodeId nodeId) {
        m_loggingControls = new LoggingControls();
        m_loggingControls.createLogControls(parentBuilder, nodeId);
    }


    public void fillAutoCompleteFields(CTestSpecification testSpec,
                                       String funcName,
                                       String coreId) {
        m_loggingControls.fillParamsAutoCompleteField(testSpec,
                                                      funcName,
                                                      coreId);
    }


    public void setWizardInput(CTestLog.ESectionsLog section, 
                               List<String> wizInput) {
        m_loggingControls.setWizardInput(section, wizInput);
    }

    
    /**
     * 
     * @param stopConditionOrTestPoint CTestPoint for unit tests, CTestStopCondition for system tests.
     * @param isMergedTestSpec
     */
    public void setBPInput(CTestBase stopConditionOrTestPoint,
                           boolean isMerged) {
        if (m_condCountTPC != null) {
            m_condCountTPC.setInput(stopConditionOrTestPoint, isMerged);
            m_condExprTPC.setInput(stopConditionOrTestPoint, isMerged);
        }
        
        if (m_conditionCountHC != null) {
            m_conditionCountHC.setInput(stopConditionOrTestPoint, isMerged);
            m_conditionExprHC.setInput(stopConditionOrTestPoint, isMerged);
        }
    }
    
    
    public void setLoggingInput(CTestLog logConfig, boolean isMerged) {
        m_loggingControls.setInput(logConfig, isMerged);
    }
    
    
    public void setLocationInput(CTestLocation testLocation, 
                                 LocationIDProvider testPointOrTestCaseIdProvider) {
        
        m_location = testLocation;
        m_tpOrTcIdProvider = testPointOrTestCaseIdProvider;
        m_showSourceOperation.setData(new Object[]{m_location, 
                                                   m_tpOrTcIdProvider.getId(), 
                                                   m_coreId});
        
        fillLocationSummary(testLocation, m_tpOrTcIdProvider.getId());
    }


    public void setEnabled(boolean isEnabled) {
        
        if (m_conditionCountHC != null) {
            m_conditionCountHC.setEnabled(isEnabled);
            m_conditionExprHC.setEnabled(isEnabled);
        }
        
        if (m_condCountTPC != null) {
            m_condCountTPC.setEnabled(isEnabled);
            m_condExprTPC.setEnabled(isEnabled);
        }
        
        m_editBtn.setEnabled(isEnabled);
        
        if (m_loggingControls != null) {
            m_loggingControls.setEnabled(isEnabled);
        }
        
        m_showSourceBtn.setEnabled(isEnabled);
    }


    public void setEditable(boolean isEditable) {
        
        m_condCountTPC.setEditable(isEditable);
        m_condExprTPC.setEditable(isEditable);
        m_editBtn.setEnabled(isEditable);
        
        if (m_loggingControls != null) {
            m_loggingControls.setEnabled(isEditable);
        }
    }


    public void clearInput() {
        if (m_conditionCountHC != null) {
            m_conditionCountHC.clearInput();
            m_conditionExprHC.clearInput();
        }
        if (m_condCountTPC != null) {
            m_condCountTPC.clearInput();
            m_condExprTPC.clearInput();
        }
        
        m_resourceNameTxt.setText("");
        m_lineOrSearchTxt.setText("");
        m_locationOffsetTxt.setText("");
        
        if (m_loggingControls != null) {
            m_loggingControls.clearInput();
        }
    }


    private void fillLocationSummary(CTestLocation location, String testPointOrTestCaseId) {
        
        switch (location.getResourceType()) {
        case E_RESOURCE_FILE:
            m_resourceNameLbl.setText("File:");
            break;
        case E_RESOURCE_FUNCTION:
            m_resourceNameLbl.setText("Function:");
            break;
        case E_RESOURCE_ADDRESS:
            m_resourceNameLbl.setText("Address:");
            break;
        default:
            m_resourceNameLbl.setText("Unknown:");
        }
        
        String srcFileLocation = "";
        if (location.getSrcFileLocation() == EFileLocation.ELocalHost) {
            srcFileLocation = "     @local host";
        } else {
            srcFileLocation = "     @remote host";
        }
        
        m_resourceNameTxt.setText(location.getResourceName() + srcFileLocation);
        
        if (location.isSearch() == ETristate.E_TRUE) {
            // m_lineOrSearchLbl.setText("Search:");
            StringBuilder sb = new StringBuilder("Search ");
            sb.append("from line ").append(location.getLine()).append(", ");
            if (location.getLinesRange() > 0) {
                sb.append("next " + location.getLinesRange() + " lines, ");
            }
            
            switch (location.getSearchContext()) {
            case E_SEARCH_ANY:
                sb.append("code and comment, ");
                break;
            case E_SEARCH_CODE:
                sb.append("code only, ");
                break;
            case E_SEARCH_COMMENT:
                sb.append("comment only, ");
                break;
            default:
            }
            
            switch (location.getMatchingType()) {
            case E_MATCH_PLAIN:
                sb.append("plain text pattern '" + location.getSearchPattern() + "'");
                break;
            case E_MATCH_REG_EX:
                sb.append("reg. ex. '" + location.getSearchPattern() + "'");
                break;
            case E_MATCH_TEST_POINT_ID:
                sb.append("plain text with t.p. ID '" + 
                          CAddressController.getTestPointIdPrefix() + 
                          testPointOrTestCaseId + "'");
                break;
            default:
                break;
            }
            
            m_lineOrSearchTxt.setText(sb.toString());
        } else {
            // m_lineOrSearchLbl.setText("Line:");
            m_lineOrSearchTxt.setText("Line: " + String.valueOf(location.getLine() + location.getLineOffset()));
        }
        
        StringBuilder sb = new StringBuilder();
        if (location.getLineOffset() > 0) {
            sb.append("Add " + location.getLineOffset() + " to selected line. ");
        }
        
        if (location.getNumSteps() > 0) {
            sb.append("Step over " + location.getNumSteps() + "-times.");
        }
        m_locationOffsetTxt.setText(sb.toString());
    }
}
