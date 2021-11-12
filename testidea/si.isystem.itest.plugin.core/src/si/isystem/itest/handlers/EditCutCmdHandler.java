package si.isystem.itest.handlers;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.CTestBench;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.DeleteTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.tbltableeditor.handlers.CutFromTableHandler;


public class EditCutCmdHandler extends EditCopyCmdHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            if (UiUtils.isStructuredSelection()) {

                CTestBench containerTestBench = 
                        UiUtils.getSelectedOutlineNodes(UiUtils.getStructuredSelection(), false);

                if (containerTestBench != null) {

                    if (UiUtils.isTestTreeActive()) {

                        String strTestSpec = UiUtils.testSpecToTextEditorString(containerTestBench);

                        copyToClipboard(strTestSpec);

                        GroupAction group = new GroupAction("Edit-Cut");
                        DeleteTestTreeNodeAction.fillGroupAction(group, containerTestBench);
                        TestSpecificationModel.getActiveModel().execAction(group);

                    } else if (UiUtils.isSectionTreeActive()) {
                        EditCopyCmdHandler copyHandler = new EditCopyCmdHandler();
                        copyHandler.execute(event);

                        ClearTestSectionCmdHandler clearSectionHandler = new ClearTestSectionCmdHandler();
                        clearSectionHandler.execute(event);
                    }
                }
            } else if (UiUtils.getKTableInFocus() != null) {
                CutFromTableHandler handler = new CutFromTableHandler();
                handler.execute(event);
            } else {
                Text text = UiUtils.getTextSelection();
                if (text != null) {
                    text.cut();
                }
                Combo combo = UiUtils.getComboBoxSelection();
                if (combo != null) {
                    combo.cut();
                }
                StyledText styleText = UiUtils.getStyleTextSelection();
                if (styleText != null) {
                    styleText.cut();
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can cut test specification to clipboard!", ex);
        }
        
        return null;
    }
}
