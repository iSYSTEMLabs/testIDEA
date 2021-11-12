package si.isystem.itest.handlers;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFilter.ETestFilterSectionIds;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.AutoIdGenerator;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.YamlScalar;
import si.isystem.itest.model.actions.AddTestTreeNodeAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.sequence.InsertToSequenceAction;
import si.isystem.itest.ui.spec.StatusView;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.ui.spec.TestTreeOutline;
import si.isystem.tbltableeditor.handlers.PasteToTableHandler;


public class EditPasteCmdHandler extends AbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            
            if (UiUtils.isTestTreeActive()) {
                // test tree or section have focus

                int pasteIdx = 0;
                CTestTreeNode selectedParentTestSpecOrGrp = null;
                CTestTreeNode selectedTreeNode = getNodesSelectedInTestTree();
                
                if (selectedTreeNode != null) {

                    selectedParentTestSpecOrGrp  = CTestTreeNode.cast(selectedTreeNode.getParent());
                    pasteIdx  = selectedParentTestSpecOrGrp.getChildren(true).find(selectedTreeNode) + 1;

                } else {
                    // if nothing is selected, paste at the end
                    selectedParentTestSpecOrGrp = TestSpecificationModel.getActiveModel().getRootTestSpecification();
                    pasteIdx = -1;
                }
            
                pasteFromClipboard(selectedParentTestSpecOrGrp, pasteIdx);
            } else if (UiUtils.isSectionTreeActive()) {
                EditPasteAndOverwriteCmdHandler overwriteHandler = new EditPasteAndOverwriteCmdHandler();
                overwriteHandler.execute(event);
            } else if (UiUtils.getKTableInFocus() != null) {
                PasteToTableHandler handler = new PasteToTableHandler();
                handler.execute(event);
            } else {
                // gui data must NOT be saved in case of focus on text or combo component  
                Text text = UiUtils.getTextSelection();
                Combo combo = UiUtils.getComboBoxSelection();
                StyledText styleText = UiUtils.getStyleTextSelection();
                
                
                if (text != null) {
                    text.paste();
                } else if (combo != null) {
                    combo.paste();
                } else if (styleText != null) {
                    styleText.paste();
                } else {
                    MessageDialog.openInformation(Activator.getShell(), 
                                                  "Paste failed", 
                                                  "Please select test case in outline view, section in editor or input field.");
                }
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Paste failed!", ex);
        }
        
        return null;
    }

    
    public CTestTreeNode getNodesSelectedInTestTree() {
        
        TestTreeOutline outline = TestCaseEditorPart.getOutline();
        
        CTestTreeNode selectedTreeNode = outline.getTestSpecSelection();
        
        if (selectedTreeNode != null) {

            if (selectedTreeNode.isGroup()) {
                CTestGroup grp = CTestGroup.cast(selectedTreeNode);
                if (grp.isTestSpecOwner()) {
                    return grp.getOwnedTestSpec();
                }
            }
        }

        return selectedTreeNode;
    }

    
    protected void pasteFromClipboard(CTestTreeNode selectedParentTestSpec,
                                      int pasteIdx) {
        String yamlSpec = readTestSpecFromClipboard();
        pasteStringTestSpec(selectedParentTestSpec, pasteIdx, yamlSpec);
    }

    
    private void pasteStringTestSpec(CTestTreeNode selectedParentTestSpec,
                                    int pasteIdx, 
                                    String yamlSpec) {
    
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            boolean isSetTestId = model.getCEnvironmentConfiguration().getToolsConfig(true).isSetTestIdOnPaste();
            AbstractAction action = createPasteActionMultiple(selectedParentTestSpec, 
                                                              pasteIdx, 
                                                              yamlSpec,
                                                              isSetTestId);
            paste(action);
        }
    }


    public void pasteTestSpec(String yamlSpec, 
                              int pasteIdx, 
                              CTestSpecification selectedParentTestSpec,
                              boolean isPastedMultiple) {

        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null) {
            boolean isSetTestId = model.getCEnvironmentConfiguration().getToolsConfig(true).isSetTestIdOnPaste();

            AbstractAction action = createPasteAction(yamlSpec, 
                                                      pasteIdx, 
                                                      selectedParentTestSpec, 
                                                      isPastedMultiple,
                                                      isSetTestId);
            paste(action);
        }
    }


    private void paste(AbstractAction action) {
        if (action != null) {
            
            action.addEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED));
            action.addAllFireEventTypes();

            try {
                TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                model.execAction(action);
            } catch (Exception ex) {
                // ignored - do nothing, if there is invalid test data on the clipboard
                SExceptionDialog.open(Activator.getShell(), "Paste Error!", ex);
            }
        }
    }


    /** 
     * If the string test spec contains a list of test specs, the string is adapted
     * so that it contains parent test spec.
     * @param selectedParentTreeNode
     * @param pasteIdx
     * @param yamlSpec
     * @param isSetTestId if true, test ID will be set and counter incremented.
     *                    This param should be false when called from drop listener -
     *                    testID should be preserved on D&D.
     * @return
     */
    public AbstractAction createPasteActionMultiple(CTestTreeNode selectedParentTreeNode,
                                                    int pasteIdx, 
                                                    String yamlSpec,
                                                    boolean isSetTestId) {
    
        boolean isPastedMultiple = false;
        if (yamlSpec != null) {
            // it there is a list of test specs, prepend 'tests:'
            if (yamlSpec.charAt(0) == '-') {
                yamlSpec = "tests:\n" + yamlSpec;
                isPastedMultiple = true;
            }

            return createPasteAction(yamlSpec, pasteIdx, selectedParentTreeNode,
                                     isPastedMultiple, isSetTestId);
        }
        
        return null;
    }


    private AbstractAction createPasteAction(String yamlSpec, 
                                            int pasteIdx, 
                                            CTestTreeNode selectedParentTreeNode,
                                            boolean isPastedMultiple,
                                            boolean isSetTestId) {

        if (yamlSpec != null) {
            try {
                CTestBench pastedTestBench = UiUtils.parseTestCasesAndGroups(yamlSpec, isPastedMultiple);
                TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                
                GroupAction groupAction = new GroupAction("Paste multiple");

                if (selectedParentTreeNode.isGroup()) {
                    CTestGroup parentTestGroup = CTestGroup.cast(selectedParentTreeNode);
                    if (parentTestGroup.isTestSpecOwner()) {
                        pasteToTestSpec(model, pastedTestBench, pasteIdx, parentTestGroup.getOwnedTestSpec(), groupAction,
                                        isSetTestId);
                    } else {
                        pasteToGroup(model, pastedTestBench, pasteIdx, parentTestGroup, groupAction);
                    }
                } else {
                    pasteToTestSpec(model, pastedTestBench, pasteIdx, selectedParentTreeNode, groupAction,
                                    isSetTestId);
                }
                
                return groupAction;
            } catch (Exception ex) {
                // ignored - do nothing, if there is invalid test data on the clipboard
                SExceptionDialog.open(Activator.getShell(), "Paste Error!", ex);
            }
        }
        
        return null;
    }
        

    public void pasteToGroup(TestSpecificationModel model,
                            CTestBench container,
                            int pasteIdx, 
                            CTestGroup selectedParentGroup, 
                            GroupAction groupAction) {

        CTestGroup selectedGroup = null;
        if (pasteIdx > 0) { // pasteIdx is incremented by caller to indicate paste index,
                            // which is after the selected item in a tree
            selectedGroup = CTestGroup.cast(selectedParentGroup.getChildren(false).get(pasteIdx - 1));
        }
            
        CTestGroup containerTG = container.getGroup(true);
        
        int noOfGroups = (int) containerTG.getChildren(true).size();
        
        for (int i = 0; i < noOfGroups; i++) {
            CTestGroup derivedTestGroup = CTestGroup.cast(containerTG.getChildren(true).get(i));
            AddTestTreeNodeAction newTestAction = new AddTestTreeNodeAction(model,
                                                                            selectedParentGroup, 
                                                                            pasteIdx, 
                                                                            derivedTestGroup);
            // pasteIdx == -1 means that no test spec is selected, and the pasted
            // test specs should be added to the end - so no increment of 
            // the pasteIdx.
            if (pasteIdx >= 0) {
                pasteIdx++;
            }
            groupAction.add(newTestAction);
        }
        
        // add test case IDs to the destination group
        if (selectedGroup != null) {
            CTestSpecification containterTS = container.getTestSpecification(true);
            int noOfTestSpecs = (int) containterTS.getNoOfDerivedSpecs();
            for (int idx = 0; idx < noOfTestSpecs; idx++) {
                
                CTestSpecification testSpec = CTestSpecification.cast(containterTS.getDerivedTestSpec(idx));
                CTestFilter grpFilter = selectedGroup.getFilter(false);
                String pastedTestId = testSpec.getTestId();
                
                // if pasted ID already exists, do not throw exception, as there may be 
                // other test IDs to be pasted
                if (!isPastedTestIdAlreadyInFilter(grpFilter, pastedTestId)) {
                
                    YamlScalar value = 
                            YamlScalar.newListElement(CTestFilter.ETestFilterSectionIds.E_SECTION_INCLUDED_IDS.swigValue(),
                                                      -1);

                    value.setValue(pastedTestId);

                    InsertToSequenceAction action = new InsertToSequenceAction(grpFilter, 
                                                                               value);
                    action.addDataChangedEvent(ENodeId.GRP_FILTER, selectedGroup);
                    groupAction.add(action);
                } else {
                    StatusView.getView().setDetailPaneText(StatusType.WARNING, 
                                                           "Test ID '" + pastedTestId + "' is already set in filter.");
                }
            }
        }
    }


    private boolean isPastedTestIdAlreadyInFilter(CTestFilter grpFilter, String pastedTestId) {
        CSequenceAdapter filterTestIDs = new CSequenceAdapter(grpFilter, 
                                                              ETestFilterSectionIds.E_SECTION_INCLUDED_IDS.swigValue(),
                                                              false);
        for (int filterIdIdx = 0; filterIdIdx < filterTestIDs.size(); filterIdIdx++) {
            String existingFilterTestId = filterTestIDs.getValue(filterIdIdx);
            if (existingFilterTestId.equals(pastedTestId)) {
                return true;
            }
        }
        
        return false;
    }
    
    
    private void pasteToTestSpec(TestSpecificationModel model,
                               CTestBench container,
                               int pasteIdx, 
                               CTestTreeNode selectedParentTestSpec, 
                               GroupAction groupAction,
                               boolean isSetTestId) {

        CTestSpecification containerTS = container.getTestSpecification(true);
        
        int noOfTestSpecs = containerTS.getNoOfDerivedSpecs();
        
        CTestSpecification parentTestSpec = null;
        // if group is selected, paste test cases to root
        if (selectedParentTestSpec.isGroup()) {
            parentTestSpec = model.getRootTestSpecification();
        } else {
            parentTestSpec = CTestSpecification.cast(selectedParentTestSpec);
        }
        
        for (int i = 0; i < noOfTestSpecs; i++) {
            CTestSpecification derivedTestSpec = containerTS.getDerivedTestSpec(i);
            if (isSetTestId) {
                AutoIdGenerator autoIdGen = new AutoIdGenerator();
                // let the test sequence start from max number, so that probability
                // of duplicated ID is smaller.
                int noOfTestCases = model.getNoOfTestCases();
                autoIdGen.setTestCounter(noOfTestCases);
                
                NewBaseTestCmdHandler.autoGenerateTestId(derivedTestSpec, autoIdGen);
            }
            AbstractAction newTestAction = new AddTestTreeNodeAction(model,
                                                                     parentTestSpec, 
                                                                     pasteIdx, 
                                                                     derivedTestSpec);
            // pasteIdx == -1 means that no test spec is selected, and the pasted
            // test specs should be added to the end - so no increment of 
            // the pasteIdx.
            if (pasteIdx >= 0) {
                pasteIdx++;
            }
            groupAction.add(newTestAction);
        }
    }

    
    protected String readTestSpecFromClipboard() {
        // read test spec from clipboard
        Clipboard cb = new Clipboard(Display.getDefault());
        TextTransfer transfer = TextTransfer.getInstance();

        String yamlSpec = (String)cb.getContents(transfer);
        cb.dispose();
        return yamlSpec;
    }

}
