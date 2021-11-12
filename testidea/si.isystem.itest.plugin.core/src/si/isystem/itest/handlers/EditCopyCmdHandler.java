package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.tbltableeditor.handlers.CopyFromTableHandler;


public class EditCopyCmdHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            if (UiUtils.isStructuredSelection()) {

                CTestBench containerNode = 
                        UiUtils.getSelectedOutlineNodes(UiUtils.getStructuredSelection(), true);

                if (containerNode != null) {
                    // if groups, which own test case are selected, then copy test case, not the group
                    CTestGroup rootGrp = containerNode.getGroup(true);
                    CTestSpecification rootTestSpec = containerNode.getTestSpecification(false);
                    
                    CTestBaseList selectedGroups = rootGrp.getChildren(true);
                    int noOfSelected = (int) selectedGroups.size();
                    
                    for (int idx = 0; idx < noOfSelected; idx++) {
                        
                        CTestGroup group = CTestGroup.cast(selectedGroups.get(idx));
                        if (group.isTestSpecOwner()) {
                            rootTestSpec.addDerivedTestSpec(-1, group.getOwnedTestSpec());
                            selectedGroups.remove(idx);
                            idx--;
                            noOfSelected--;
                        }
                    }
                    
                    String strTestSpec = UiUtils.testSpecToTextEditorString(containerNode);
                    // System.err.println(strTestSpec + "\n\n --- \n\n");
                    copyToClipboard(strTestSpec);
                }
            } else if (UiUtils.getKTableInFocus() != null) {
                CopyFromTableHandler handler = new CopyFromTableHandler();
                handler.execute(event);
            } else {
                Text text = UiUtils.getTextSelection();
                if (text != null) {
                    text.copy();
                }
                Combo combo = UiUtils.getComboBoxSelection();
                if (combo != null) {
                    combo.copy();
                }
                StyledText styleText = UiUtils.getStyleTextSelection();
                if (styleText != null) {
                    styleText.copy();
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Can not copy test specification to clipboard!", ex);
        }
        
        return null;
    }

    
    protected void copyToClipboard(String strTestSpec) {
        Clipboard cb = new Clipboard(Display.getDefault());
        // YamlTestSpecTransfer transfer = YamlTestSpecTransfer.instance();
        TextTransfer transfer = TextTransfer.getInstance();
        
        // make line separator OS consistent - could be done also in YAML emitter
        // (see yaml_emitter_t.line_break and yaml_break_t in yaml.h), but then
        // files should be opened as binary files all over application :-( 
        String lineSeparator = System.getProperty("line.separator");
        if (!lineSeparator.equals("\n")) {  // '\n' is used by YAML emitter
            strTestSpec = strTestSpec.replace("\n", lineSeparator);
        }
        cb.setContents(new Object[] {strTestSpec},
                new Transfer[] {transfer});
        cb.dispose();
    }
    
    
//    @Override
//    public void setEnabled(Object context) {
//        System.out.println("setEnabled(Object context) " + context);
//        setBaseEnabled(true);
//    }
//    
//
//    @Override
//    public boolean isEnabled() {
//        return super.isEnabled();
//    }
    
}
