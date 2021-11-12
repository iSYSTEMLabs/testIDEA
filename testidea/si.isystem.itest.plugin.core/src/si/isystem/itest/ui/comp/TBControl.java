package si.isystem.itest.ui.comp;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;

import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SEFormatter;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.AbstractAction.EFireEvent;
import si.isystem.itest.model.IActionExecutioner;
import si.isystem.itest.model.ModelVerifier;
import si.isystem.itest.model.ModelVerifier.SectionStatus;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.ColorProvider;


/**
 * This class encapsulates SWT control, its decoration(s) for comment(s), and
 * automatically creates and executes action, which updates the underlying model.
 * Merged status is also shown and control is made uneditable.
 * 
 * Status returned by ModelVerifier is also shown with BOTTOM RIGHT decoration. 
 */
public abstract class TBControl {

    // components do not have influence on this variable
    // these editors state (enabled/disabled) depends on this TBControl
    
    /**
     * IDs for controls used to update syntax error status. Add only controls,
     * for which ModelVerifier can report invalid status.
     *  
     * @author markok
     *
     */
    public enum EHControlId {ETestId, ETags, ETestDescription,
        EFuncUnderTestName, EFuncUnderTestParams, EFuncUnderTestRetValName,
        EStubParams, EStubRetValName, EStubReplFuncName, EStubScriptFuncName, EStubScriptParams, EStubAssignments,
        ETestPointCondCount, ETestPointCondExpr, ETestPointScriptFunc, 
        EScriptInitTargetFName, EScriptInitFuncFName, EScriptEndFuncFName, EScriptRestoreTargetFName,
        EScriptInitTargetParams, EScriptInitFuncParams, EScriptEndFuncParams, EScriptRestoreTargetParams,
        EScriptGroupInitFuncName, EScriptGroupEndFuncName,
        ECoverageBitmap, ECoverageBranches, ECoverageObjCode, ECoverageSrcCode, ECoverageBranchesTaken, 
        ECoverageBranchesNotTaken, ECoverageBranchesBoth, ECoverageMcDc, 
        ETraceDocFileName, ETraceTriggerName, ETraceExportFormat, ETraceExportFileName, 
        EProfilerDocFileName, EProfilerTriggerName, EProfilerExportFormat, EProfilerExportFileName, 
        ECoverageDocFileName, ECoverageTriggerName, ECoverageExportFormat, ECoverageExportFileName, 
        ECoverageFmtVariant, ECoverageModulesFilter, ECoverageFunctionsFilter, 
        ELocationResName, ELocationLineNumber, ELocationRange, ELocationPattern, ELocationLineOffset, ELocationNumSteps, 
        EMaxStackUsage, ECoreId, ETestCaseTimeout, EDryRunProfMultiplier, EDryRunProfOffset, 
        EDiagramsTable, // KTAble currently does not have syntax status implemented - add it to rows!
    }


    protected Control m_control;
    protected ENodeId m_nodeId;

    private ControlDecoration m_syntaxStatusDecoration;
    protected ValueAndCommentEditor m_tagEditor;
    protected ValueAndCommentEditor m_mainTagEditor; // editor for comments, which are attached
                                       // to tag in parent class, for example 'func:', 'trace',
                                       // not to section tag such as function name or trigger name
    protected boolean m_isMerged; // true if this component is merged 
    private Color m_syntaxStatusColor;
    private TBControl.EHControlId m_controlId;
    protected CTestBase m_testBase;
    protected IActionExecutioner m_actionExecutioner;
    protected boolean m_isTestTreeRefreshNeeded = false;
    
    /** Method <code>configure()</code> must be called before any other method. */ 
    public TBControl(ENodeId nodeId) {
        m_nodeId = nodeId;
        m_syntaxStatusColor = null; //ColorProvider.instance().getBkgColor();
    }


    /**
     * Call this method from ctor od derived class, after controls are created.
     * 
     * @param control
     * @param tagEditor
     * @param controlId section for which ModelVerifier can provide UI status.
     *                        May be null.
     */
    public void configure(Control control, 
                          ValueAndCommentEditor tagEditor, 
                          TBControl.EHControlId controlId) {
        m_control = control;
        m_tagEditor = tagEditor;
        m_controlId = controlId;
    }
    
    
    public void setMainTagEditor(ValueAndCommentEditor editor) {
        m_mainTagEditor = editor;
    }


    public Control getControl() {
        return m_control;
    }

    
    public YamlScalar copyScalar() {
        return m_tagEditor.getScalarCopy();
    }
    
    /** Contents of control is cleared. */    
    public void clearInput() {
        setInput(null, false);
    }
    
    
    /** 
     * Contents of control is set according to testBase. 
     * 
     * @param isUpdateUIControl if false, UI control is not updated - this is 
     * needed when user presses key in control, and we want to avoid
     * updating UI controls - see StubSpecEditor.
     */    
    public void setInput(CTestBase testBase, boolean isMerged) {
        
        setInput(testBase, isMerged, TestSpecificationModel.getActiveModel());
    }


    public void setInput(CTestBase testBase, 
                         boolean isMerged, 
                         IActionExecutioner actionExecutioner) {
        
        m_testBase = testBase;
        m_isMerged = isMerged;
        m_actionExecutioner = actionExecutioner;

        m_tagEditor.updateValueAndCommentFromTestBase(testBase);
        
        applySyntaxStatus();
        
        setMerged(m_isMerged);
    }

    
    public void setMerged(boolean isMerged) {
        m_isMerged = isMerged;
        
        setBackgroundColor();

        // disable comment editing always when editing control is disabled
        // If the control is enabled, but has no text, comment editing is still disabled
        if (m_isMerged) {

            setEnabled(false);
            
        } else {
            setEnabled(true);
        }
    }

    
    public void setTestTreeRefreshNeeded(boolean isTestTreeRefreshNeeded) {
        m_isTestTreeRefreshNeeded = isTestTreeRefreshNeeded;
    }


    public void setEnabled(boolean isEnabled) {
        
        if (!isEnabled  ||  m_isMerged  ||  m_testBase == null) {
        
            m_control.setEnabled(false);
            m_tagEditor.setEnabled(false);
            
            if (m_mainTagEditor != null) {
                m_mainTagEditor.setEnabled(false);
            }
            
        } else {
            
            m_control.setEnabled(true);
            m_tagEditor.setEnabled(!m_tagEditor.getValue().isEmpty());
            
            if (m_mainTagEditor != null) {
                m_mainTagEditor.setEnabled(!m_testBase.isEmpty());
            }
        }
    }
    
    
    public boolean isMerged() {
        return m_isMerged;
    }

    
/*    public void text2tag(String text) {
        m_tagEditor.setValue(text);
    }
  */  
    
    protected Color getBackgroundColor() {
        if (isMerged()) {
            return ColorProvider.instance().getColor(ColorProvider.MERGED_BKG_COLOR);
        }
        
        return m_syntaxStatusColor;
    }
    

    /** 
     * Retrieves syntax status from ModelVerifier and updates the component
     * background and decoration accordingly.
     */
    public void applySyntaxStatus() {
        
        if (m_controlId == null) {
            return;
        }
        
        SectionStatus syntaxStatus = ModelVerifier.INSTANCE.getSectionStatus(m_controlId);
        FieldDecoration fieldDecoration;
        
        if (syntaxStatus != null) {
            switch (syntaxStatus.getSeverity()) {
            case IStatus.ERROR:
                if (m_syntaxStatusDecoration == null) {
                    m_syntaxStatusDecoration = new ControlDecoration(m_control, SWT.RIGHT | SWT.BOTTOM);
                }
                
                fieldDecoration = FieldDecorationRegistry.getDefault()
                        .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
                m_syntaxStatusDecoration.setImage(fieldDecoration.getImage());
                m_syntaxStatusDecoration.setDescriptionText(syntaxStatus.getDescription());
                
                m_syntaxStatusColor = ColorProvider.instance().getErrorColor();
                return;
            case IStatus.WARNING:
                if (m_syntaxStatusDecoration == null) {
                    m_syntaxStatusDecoration = new ControlDecoration(m_control, SWT.RIGHT | SWT.BOTTOM);
                }
                
                fieldDecoration = FieldDecorationRegistry.getDefault()
                        .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING);
                
                m_syntaxStatusDecoration.setImage(fieldDecoration.getImage());
                m_syntaxStatusDecoration.setDescriptionText(syntaxStatus.getDescription());
                
                m_syntaxStatusColor = ColorProvider.instance().getWarningColor();
                return;
            }
        }
        
        if (m_syntaxStatusDecoration != null) {
            m_syntaxStatusDecoration.setImage(null);
        }
        
        m_syntaxStatusColor = null; //ColorProvider.instance().getBkgColor();
    }
    
    
    public void setFocus() {
        m_control.setFocus();
    }
    

    protected void setBackgroundColor() {
        m_control.setBackground(getBackgroundColor());
    }
    

    public void addFocusListener(FocusListener listener) {
        m_control.addFocusListener(listener);
    }

    
    protected void sendActionAndVerify(AbstractAction action, boolean isFireOnExec) {
        if (m_isTestTreeRefreshNeeded) {
            CTestTreeNode containerTestNode = m_testBase.getContainerTestNode();
            if (containerTestNode != null) { // m_testBase used in dialog has no parent
                action.addTreeChangedEvent(containerTestNode.getParentNode(), 
                                           containerTestNode);
            }
        }
        action.addDataChangedEvent(m_nodeId, m_testBase);
        
        if (isFireOnExec) {
            action.addAllFireEventTypes();
        } else {
            action.addFireEventTypes(EFireEvent.UNDO, EFireEvent.REDO);
        }
        
        if (action.isModified()) {
            try {
                m_actionExecutioner.execAction(action);
                if (m_mainTagEditor != null) {
                    m_mainTagEditor.setEnabled(!m_testBase.isEmpty());
                }
            } catch (Exception ex) {
                // if dialog is opened here, it appears twice, because focusLost()
                // event is also triggered on editor
//                SExceptionDialog.open(Activator.getShell(), 
//                                      "Can not set test case data!", 
//                                      ex);
                
                StatusView.getView().flashDetailPaneText(StatusType.FATAL,
                                                       "Can not set test case data!\n" + 
                        SEFormatter.getInfoWithStackTrace(ex, 0));
                System.err.println("\n\nSee status view for this Exception:\n");
                ex.printStackTrace();
            }
        }
    }
}
