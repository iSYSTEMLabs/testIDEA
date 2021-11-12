package si.isystem.itest.ui.spec;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.itest.model.TestSpecificationModel;

/**
 * This interface defines methods to be implemented by section editors. It 
 * serves as a documentation about required methods. Many of these methods are 
 * implemented by SectonEditorAdapter. However, SectonEditorAdapter contains also
 * some other utility methods, which do not need to be overridden.
 * 
 * It is important to be aware, that one editor may contain one or more test 
 * specification sections. For example, test points editor contains only test 
 * points section, while meta editor contains test id, description, and tags.
 *
 * When creating a new section editor class, it is recommended to first implement
 * this interface, create methods to implement, and then change the new editor
 * to extend SectionEditorAdapter. Finally see for all methods which need to 
 * override methods from SectionEditorAdapter, and which are OK in SectionEditorAdapter. 
 * 
 * @author markok
 *
 */
public interface ISectionEditor {


    /** Sets contents of all controls in section editor. */
    void fillControlls();

    /**
     * This method should select line in editor's table, if it has a table.
     * The line should also get a distinct background color. If editor does
     * not have a table, this method should be empty.
     * 
     * @param lineNo line in the table to get selected. This line must exist,
     * otherwise an exception is thrown.
     */
    void selectLineInTable(int tableId, int lineNo);
    
    
    /**
     * This method is called whenever user selects other test specification or
     * section.
     * 
     * @param testSpec original test spec.
     * @param mergedTestSpec merged test spec - it's sections are used in case of
     *                       merging.
     */
    void setInputTestSpec(TestSpecificationModel model,
                          CTestSpecification testSpec, 
                          CTestSpecification mergedTestSpec);
    
    /**
     * The same as setInput but for test group editors.
     * 
     * @param model
     * @param inputGroup
     */
    void setInputGroup(TestSpecificationModel model, CTestGroup inputGroup);
    
    /* 
     * @deprecated 
     * Stores data from controls to test spec. Remove this method when all 
     * editors will use TBControls. 
     * */
    // void copyGUIDataToTestSpec();

    /** 
     * Returns true is the section is empty. 
     */
    boolean isEmpty();
    
    /**
     *  Returns true, if at least one of sections for the node is merged. 
     */ 
    boolean isMerged();

    /** 
     * Should return true, only if error status depends on results of test. 
     * Such classes should implement method isError(). 
     */
    boolean hasErrorStatus();
    
    /** 
     * Returns test result status. Method hasErrorStatus() must return true 
     * for this method to be called. */
    boolean isError(CTestResult result);
    
    /** 
     * Returns test result status. Method hasErrorStatus() must return true 
     * for this method to be called. */
    boolean isError(CTestGroupResult result);
    
    /** 
     * Returns false for analyzer sections when run mode is OFF, and for stubs,
     * users tubs and test points when ALL items are inactive. 
     */
    boolean isActive();

    /**
     * This method should copy section(s) shown in section editor to the given
     * test spec. Used for copy to clipboard commands. Should copy directly,
     * not with Action class, see default impl. in {@link SectionEditorAdapter}.
     * 
     * @param destTestSpec
     */
    void copySection(CTestTreeNode destTestSpec);

    
    /**
     * This method should clear all sections shown by section editor. Used by 
     * the 'clear section' command.
     */
    void clearSection();

    
    /**
     * @return path of CTestBase sections, which are shown in this node, null for
     *              sections in root CTestSpecification. 
     */
    String getNodePath();
    
    
    /**
     * @return IDs of sections contained int his editor to be shown in table editor
     */
    int [] getSectionIdsForTableEditor();
    
    /** Should create CTestSpecification or CTestGroup. */
    CTestTreeNode createTestTreeNode();
    
    /** Returns currently selected node. */
    public CTestTreeNode getTestTreeNode();
}
