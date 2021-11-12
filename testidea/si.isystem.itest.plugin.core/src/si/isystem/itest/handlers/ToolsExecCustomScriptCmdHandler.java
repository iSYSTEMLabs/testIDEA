package si.isystem.itest.handlers;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CScriptConfig;
import si.isystem.exceptions.SException;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.CustomScriptsMenuOptions;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.run.Script;
import si.isystem.itest.run.TestScriptResult;
import si.isystem.python.Python;

public class ToolsExecCustomScriptCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        try {
            String iyamlFName = getIYamlFileName();

            String scriptOrMethodName = event.getParameter(CustomScriptsMenuOptions.SCRIPT_OR_METHOD_NAME);
            String isScriptStr = event.getParameter(CustomScriptsMenuOptions.IS_STANDALONE_SCRIPT_CMD_PARAM_ID);
            boolean isScript = Boolean.valueOf(isScriptStr).booleanValue();

            boolean isShift = false;
            boolean isCtrl = false;
            if (event.getTrigger() instanceof Event) {
                Event ev = (Event)event.getTrigger();
                if ((ev.stateMask & SWT.SHIFT) != 0) {
                    isShift = true;
                }
                if ((ev.stateMask & SWT.CTRL) != 0) {
                    isCtrl = true;
                }
            }

            if (isScript) {
                runScript(scriptOrMethodName, iyamlFName, isShift, isCtrl);
            } else {
                runMethodInExtensionScript(scriptOrMethodName, 
                                           CScriptConfig.getEXT_METHOD_TYPE(),
                                           "r\"" + iyamlFName + '"');
            }

        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Error when calling script extension!", 
                                  ex);
        }
        return null;
    }


    private void runScript(final String scriptName, 
                           final String iyamlFName, 
                           boolean isAsyncRun,
                           boolean isStartInteractively) {

        final JConnection jCon = ConnectionProvider.instance().getDefaultConnection();

        final String workingDir = Paths.get(iyamlFName).getParent().toString();
        final Python interpreter = Python.createInteractiveInstance(jCon, workingDir);
        
        if (isAsyncRun) {
            interpreter.execScriptAsync(jCon, workingDir, isStartInteractively, scriptName, iyamlFName);
        } else {

            final long scriptTimeout = 0;  // infinite timeout, user has an option to cancel in Progress monitor
            
            final StringBuilder stdout = new StringBuilder();
            final StringBuilder stderr = new StringBuilder();
            final MutableBoolean isTimeout = new MutableBoolean();
            final MutableBoolean isCanceled = new MutableBoolean();
            final MutableObject<SException> exception = new MutableObject<>();
            
            try {
                PlatformUI.getWorkbench().getProgressService()
                .busyCursorWhile(new IRunnableWithProgress() {

                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException,
                    InterruptedException {        

                        monitor.beginTask("Running external Python script", 
                                          IProgressMonitor.UNKNOWN);
                        try {
                            interpreter.execScript(jCon, monitor, workingDir, 
                                                   new String[]{scriptName, 
                                                           '"' + iyamlFName + '"'}, 
                                                   scriptTimeout, stdout, stderr, isTimeout, 
                                                   isCanceled, exception);
                        } finally {
                            monitor.done();
                        }
                    }
                });
            } catch (InvocationTargetException | InterruptedException ex) {
                ex.printStackTrace();
                SExceptionDialog.open(Activator.getShell(), 
                                      "Error executing external Python script!", 
                                      ex);
            }
            
            if (exception.getValue() != null) {
                SException ex = exception.getValue();
                ex.printStackTrace();
                SExceptionDialog.open(Activator.getShell(), "Exception caught when external Python script!", ex);
            }
            
            StatusType status = stderr.length() == 0 ? StatusType.OK : StatusType.ERROR;
            
            StatusModel.instance().appendDetailPaneText(status, "stdout:\n  " +
                                                             stdout + "\nsterr:\n  " + stderr + '\n');
        }
    }

    
    public static TestScriptResult runMethodInExtensionScript(String methodName,
                                                              String methodType,
                                                              String scriptParam) {
        StatusModel statusModel = StatusModel.instance();
        
        Script script = UiUtils.initScript(null);

        TestScriptResult result = script.callFunction(null, 
                                                      methodType, 
                                                      methodName, 
                                                      new String[]{scriptParam});

        if (result.isError()) {
            statusModel.appendDetailPaneText(StatusType.ERROR, result.toUIString());
        } else {
            statusModel.appendDetailPaneText(StatusType.INFO, result.toUIString());
        }

        return result;
    }

    
    private static String getIYamlFileName() {

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();

        if (model == null) {
            throw new SIllegalStateException("Can not run command for unknown iyaml file.\n"
                    + "Please open or select testIDEA iyaml file!");
        } 
          
        return model.getModelFileName();
    }
}
