package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import si.isystem.commons.connect.IIConnectOperation;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CTestAnalyzer;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestHostVars;
import si.isystem.connect.CTestLocation;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.connect.CTestStopCondition;
import si.isystem.connect.StrVector;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.BPLogLocationControls;
import si.isystem.itest.ui.spec.TestTreeOutline;


/**
 * This class shows source code corresponding to current selection in Outline 
 * view. If unit test case is selected, then function is shown. For system
 * tests breakpoint location of Start/End stop condition is shown, if defined.
 * For groups the first module is opened, if defined. 
 * 
 * @author markok
 *
 */
public class TestShowSourceCode extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        try {
            TestTreeOutline outlineView = TestCaseEditorPart.getOutline();
            IStructuredSelection structSelection = (IStructuredSelection)outlineView.getSelection();

            CTestBench containerNode = UiUtils.getSelectedOutlineNodes(structSelection, 
                                                                       true);

            if (containerNode == null) {
                return null; // nothing is selected in Outline view
            }

            Event ev = (Event)event.getTrigger();
            boolean isShowAnalyzerFile = (ev.stateMask & SWT.SHIFT) != 0;
            IIConnectOperation showSourceOperation;

            if (isShowAnalyzerFile) {
                showSourceOperation = showAnalyzerFile(containerNode);
            } else {
                showSourceOperation = showSourceCode(containerNode);
            }

            ISysUIUtils.execWinIDEAOperation(showSourceOperation, 
                                             Activator.getShell(),
                                             ConnectionProvider.instance().getDefaultConnection());
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not show source code of selected item.", ex);
        }
        
        return null;
    }


    /** Creates operation for showing C/C++ source code, either function or file. */
    public IIConnectOperation showSourceCode(CTestBench containerNode) {
        IIConnectOperation showSourceOperation = null;

        CTestSpecification rootTestSpec = containerNode.getTestSpecification(false);
        if (rootTestSpec.hasChildren()) {
            CTestSpecification firstTs = rootTestSpec.getDerivedTestSpec(0);
            showSourceOperation = showSourceCodeForTestCase(firstTs.merge());
        } else {
            // if no test case is selected, try to get module from selected group
            CTestGroup rootGroup = containerNode.getGroup(true);
            if (rootGroup.hasChildren()) { 
                CTestBase childTb = rootGroup.getChildren(true).get(0);
                CTestGroup childGrp = CTestGroup.cast(childTb);
                showSourceOperation = showSourceCodeForTestGroup(childGrp);
            }
        }
        return showSourceOperation;
    }


    public IIConnectOperation showAnalyzerFile(CTestBench containerNode) {
        
        IIConnectOperation showSourceOperation = null;

        CTestSpecification rootTestSpec = containerNode.getTestSpecification(false);
        if (rootTestSpec.hasChildren()) {
            CTestSpecification firstTs = rootTestSpec.getDerivedTestSpec(0);
            CTestAnalyzer analyzer = firstTs.getAnalyzer(true); 
            String analFName = analyzer.getDocumentFileName();
            CTestHostVars hostVars = CTestHostVars.createTcAnalyzerFNameVars(firstTs);
            analFName = hostVars.replaceHostVars(analFName);
            showSourceOperation = createOpForShowFile(firstTs.getCoreId(),
                                                      analFName);
        } else {
            // if no test case is selected, try to get module from selected group
            CTestGroup rootGroup = containerNode.getGroup(true);
            if (rootGroup.hasChildren()) { 
                CTestBase childTb = rootGroup.getChildren(true).get(0);
                CTestGroup childGrp = CTestGroup.cast(childTb);
                String analFName = childGrp.getMergedAnalyzerFileName();
                CTestHostVars hostVars = CTestHostVars.createGrpAnalyzerFNameVars(childGrp);
                analFName = hostVars.replaceHostVars(analFName);
                showSourceOperation = createOpForShowFile(childGrp.getFilter(true).getCoreId(), 
                                                          analFName);
            }
        }
        return showSourceOperation;
    }

    
    private IIConnectOperation showSourceCodeForTestCase(CTestSpecification firstTs) {
        String coreId = firstTs.getCoreId();

        if (firstTs.getTestScope() == ETestScope.E_UNIT_TEST) {
            // show funcion
            String funcName = firstTs.getFunctionUnderTest(true).getName();
            if (!funcName.isEmpty()) {
                return createOpForFuncSourceCode(coreId, 
                                                         funcName);
            }
        } else {
            // for system tests show breakpoint locations if defined   
            CTestStopCondition stopCond = firstTs.getBeginStopCondition(true);
            CTestLocation loc = stopCond.getBreakpointLocation(true);
            String testId = firstTs.getTestId();

            if (!loc.isEmpty()) {
                return createOpForSourceCodeAtLocation(coreId, testId, loc);
            } else {
                stopCond = firstTs.getEndStopCondition(true);
                loc = stopCond.getBreakpointLocation(true);
                if (!loc.isEmpty()) {
                    return createOpForSourceCodeAtLocation(coreId, testId, loc);
                }
            }
        }
        
        return null;
    }

    
    private IIConnectOperation showSourceCodeForTestGroup(CTestGroup childGrp) {
        
        if (childGrp.isTestSpecOwner()) {
            CTestSpecification testSpec = childGrp.getOwnedTestSpec();
            return showSourceCodeForTestCase(testSpec);
            
        } else {
            
            CTestFilter filter = childGrp.getFilter(true);

            String coreId = filter.getCoreId();

            StrVector functions = new StrVector();
            filter.getIncludedFunctions(functions);
            if (functions.size() > 0) {
                return createOpForFuncSourceCode(coreId, functions.get(0));
            } else {
                // if functions are not defined, try with module
                StrVector modules = new StrVector();
                filter.getModules(modules);
                if (modules.size() > 0) {
                    return createOpForShowFile(coreId, modules.get(0));
                }
            }
        }
        
        return null;
    }


    private IIConnectOperation createOpForFuncSourceCode(final String coreId, final String funcName) {
        return new IIConnectOperation() {
            
            @Override
            public void exec(JConnection jCon) {
                if (!funcName.isEmpty()) { 
                    if (Character.isDigit(funcName.charAt(0))) {
                        WinIDEAManager.showSourceAtAddressInEditor(jCon.getMccMgr().getConnectionMgr(coreId),
                                                                   funcName);
                    } else {
                        WinIDEAManager.showFunctionInEditor(jCon.getMccMgr().getConnectionMgr(coreId), 
                                                            funcName);
                    }
                }
            }
            
            @Override
            public void setData(Object data) {}
        };
    }
    
    
    private IIConnectOperation createOpForSourceCodeAtLocation(final String coreId,
                                                               final String testId,
                                                               final CTestLocation location) {

        return BPLogLocationControls.createShowTestPointSourceOperation(testId, 
                                                                        location, 
                                                                        coreId);
    }

    
    private IIConnectOperation createOpForShowFile(final String coreId, 
                                                  final String moduleName) {

        return new IIConnectOperation() {
            
            @Override
            public void exec(JConnection jCon) {
                if (!moduleName.isEmpty()) { 
                    WinIDEAManager.showSourceInEditor(jCon.getMccMgr().getConnectionMgr(coreId), 
                                                      moduleName, 1);
                }
            }
            
            @Override
            public void setData(Object data) {}
        };
        
    }
}
