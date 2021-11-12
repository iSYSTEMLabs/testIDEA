package si.isystem.itest.ui.spec;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestImportSources;
import si.isystem.connect.CTestImports;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.dialogs.WizardPageDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelVerifier;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.model.actions.AssignTestObjectAction;
import si.isystem.itest.model.actions.GroupAction;
import si.isystem.itest.model.actions.testBase.SetTestObjectAction;
import si.isystem.itest.ui.comp.TBControlTristateCheckBox;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.itest.wizards.newtest.GlobalsWizardDataPage;
import si.isystem.itest.wizards.newtest.NewTCWizardDataModel;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This is an adapter class for section editors. It implements methods declared 
 * in ISectionEditor with the most common implementation. See interface
 * ISectionEditor for the list of methods, which section editor may override, and
 * then override only those, which do not match the default impl. in this class.  
 * 
 * @author markok
 *
 */
abstract public class SectionEditorAdapter extends AbstractSectionEditor {

    protected SectionIds [] m_testSpecSectionIds;
    protected ENodeId m_nodeId;

    protected TestSpecificationModel m_model;
    protected CTestSpecification m_testSpec;
    protected CTestSpecification m_mergedTestSpec;
    
    protected boolean m_isInherited;
    protected CTestSpecification m_currentTestSpec;
    protected String m_currentCoreId = "";
    
    
    public SectionEditorAdapter(ENodeId nodeId, SectionIds ... sectionIds) {
        m_nodeId = nodeId;
        m_testSpecSectionIds = sectionIds;
    }

    
    @Override
    public void setInputGroup(TestSpecificationModel model, CTestGroup testGroup) {
    }

    
    @Override
    public void setInputTestSpec(TestSpecificationModel model, 
                                 CTestSpecification testSpec, 
                                 CTestSpecification mergedTestSpec) {
        m_model = model;
        m_testSpec = testSpec;
        m_mergedTestSpec = mergedTestSpec;
    }

    
    @Override
    public boolean isEmpty() {
        if (m_testSpec == null) {
            return true;
        }
        
        // if any of test sections is not empty, section editor is not empty
        for (SectionIds id : m_testSpecSectionIds) {
            if (!m_testSpec.isSectionEmpty(id.swigValue())) {
                return false;
            }
        }
        return true;
    }

    
    @Override
    public boolean isMerged() {
        if (m_testSpec == null) {
            return false;
        }
        
        // if any of test sections is merged, section editor should be marked as merged
        for (SectionIds id : m_testSpecSectionIds) {
            if (m_testSpec.isInheritSection(id)) {
                return true;
            }
        }
        
        return false;
    }


    /**
     * Should be overridden in all CTestSpecification editors, which have complex 
     * sections.
     * 
     * @param testSpec
     */
    @Override
    public void copySection(CTestTreeNode destTestSpec) {
        
        if (m_testSpec == null) { // can happen if user executes export without file opened
            return;
        }
        
        for (SectionIds id : m_testSpecSectionIds) {
            int section = id.swigValue();
            CTestObject testObj = m_testSpec.getSectionValue(section, true);
            
            if (!testObj.isEmpty()) {
                destTestSpec.getSectionValue(section, false).assign(testObj);
                
                // copy also comment of the test spec tag
                DataUtils.copyComment(m_testSpec, section, destTestSpec);
            }
        }
    }
    
    
    @Override
    public void clearSection() {
        AbstractAction action = createClearSectionAction(m_testSpecSectionIds);
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }

    
    // Utility methods
    
    // Creates scrollable composite, which can be used to hold KTable.
    protected Composite createScrollable(Composite parent) {
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        // use additional outerPanel as workaround to scrolling problem
        Composite outerPanel = new Composite(scrolledPanel, SWT.NONE);
        outerPanel.setLayout(new MigLayout("fill"));
        Composite mainPanel = new Composite(outerPanel, SWT.NONE);
        mainPanel.setLayoutData("growx, growy");

        configureScrolledComposite(scrolledPanel, outerPanel);
        
        return mainPanel;
    }
    
    
    // Returns scrollable parent of panel created with createScrollable().
    protected Composite getScrollableParent(Composite mainPanel) {
        return mainPanel.getParent().getParent();
    }
    
    
    /** 
     * This method copies test spec section (for example Trace, stubs, ...)
     * from the currently selected test spec to the given test spec.
     * 
     * @param testSpec the test spec to receive the section.
     */
    public void copyMergedSection() {
        AbstractAction action = createCopyMergedSectionAction();
        action.addAllFireEventTypes();
        action.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(action);
    }
    
    
    protected void copySection(CTestSpecification destTestSpec, SectionIds ... ids) {
        GroupAction grp = new GroupAction("Copy section to dest test spec");
        
        for (SectionIds id : ids) {
            int section = id.swigValue();
            AssignTestObjectAction action = new AssignTestObjectAction(destTestSpec.getSectionValue(section, false), 
                                                                       m_mergedTestSpec.getSectionValue(section, true),
                                                                       m_nodeId);
            grp.add(action);
        }
        
        grp.addAllFireEventTypes();
        grp.addDataChangedEvent(m_nodeId, m_testSpec);
        TestSpecificationModel.getActiveModel().execAction(grp);
    }
    

    /*
     * End of methods, which may be overridden.
     */


    protected AbstractAction createClearSectionAction(SectionIds ... ids) {
        GroupAction grp = new GroupAction("Clear sections in node " + m_nodeId);
        
        for (SectionIds id : ids) {
            SetTestObjectAction action = new SetTestObjectAction(m_testSpec, 
                                                             id.swigValue(), 
                                                             null, 
                                                             m_nodeId);
            grp.add(action);
        }
        
        return grp;
    }
    
    
    protected AbstractAction createCopyMergedSectionAction(SectionIds ... ids) {
        GroupAction grp = new GroupAction("Copy sections in node " + m_nodeId);
        
        for (SectionIds id : ids) {
            int section = id.swigValue();
            AssignTestObjectAction action = new AssignTestObjectAction(m_testSpec.getSectionValue(section, false), 
                                                                       m_mergedTestSpec.getSectionValue(section, true),
                                                                       m_nodeId);
            grp.add(action);
        }
    
        return grp;
    }
    
    
    public void verifyTestSpec(CTestSpecification testSpec) {
        try {
            ModelVerifier.INSTANCE.verifyTestTreeNodeAndSetStatus(testSpec.merge());
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), 
                                  "Verification failed!", ex);
        }
}


    void setInputForInheritCb(SectionIds section, TBControlTristateCheckBox cb) {
        
        if (section != null) {
            CTestImports imports = m_testSpec.getImports(false);
            CTestImportSources importsSrc = imports.getSectionSources(section, false);
            cb.setInput(importsSrc, false);
        } else {
            cb.clearInput();
            cb.setEnabled(false);
        }
    }
    
    
    /** Call this method from editors, which contain only one test section. */
    CTestSpecification getCurrentTS() {

        return getCurrentTS(m_testSpecSectionIds[0]);
    }
    

    /** Call this method from editors, which contain more than one test section. */
    CTestSpecification getCurrentTS(SectionIds id) {
        return getCurrentTS(id, new MutableBoolean());
    }
    
    
    CTestSpecification getCurrentTS(SectionIds id, MutableBoolean isInherited) {
        
        isInherited.setValue(false);
        
        if (m_testSpec != null) {
            
            boolean isInh = m_testSpec.isInheritSection(id);
            isInherited.setValue(isInh);
            
            if (isInh) {
                return m_mergedTestSpec;
            }
            return m_testSpec;
        }
        
        return null;
    }

    
    void setCurrentTS(SectionIds id) {
        
        if (m_testSpec != null) {
            m_isInherited = m_testSpec.isInheritSection(id);
            if (m_testSpec.isInheritSection(SectionIds.E_SECTION_CORE_ID)) {
                m_currentCoreId = m_mergedTestSpec.getCoreId();
            } else {
                m_currentCoreId = m_testSpec.getCoreId();
            }
            m_currentCoreId = m_model.getConfiguredCoreID(m_currentCoreId);
            
        } else {
            m_isInherited = false;
        }
        
        m_currentTestSpec = m_isInherited ? m_mergedTestSpec : m_testSpec;
    }
    
    
    protected String getCoreId() {
        String coreId = m_testSpec.getCoreId();
        if (m_testSpec.isInheritSection(SectionIds.E_SECTION_CORE_ID)) {
            coreId = m_mergedTestSpec.getCoreId();
        }

        coreId = m_model.getConfiguredCoreID(coreId);
        return coreId;
    }

    
    /**
     * 
     *   inherit  base  current  |  checkBox  Editing   Test Spec
     *   flag     sect. section  |   state    possible  section shown
     *   ============================================================
     *     0       0       0     |     0         1        current 
     *     0       0       1     |     0         1        current 
     *     0       1       0     |     0         1        current
     *     0       1       1     |     0         1        current
     *     1       0       0     |     1         0        Merged 
     *     1       0       1     |     invalid state - forbidden
     *     1       1       0     |     1         0        Merged 
     *     1       1       1     |     invalid state - forbidden 
     *    default  0       0     |   interm.     1        current 
     *    default  0       1     |   interm.     1        current
     *    default  1       0     |   interm.     0        Merged 
     *    default  1       1     |   interm.     1        current
     *    
     *    It is evident, that the flag could have only two values - false and
     *    not defined, which means old behavior. However, this setting would not
     *    make much sense in text, where specifying true is possible. This way 
     *    the intention about inheritance can also be clearly stated and gives an
     *    opportunity for comments.
     *    Maybe in the future the checked value should be considered as default
     *    one so we keep three states in test spec but only two button states - 
     *    but then again, how can we keep test spec contents on  1:1 between UI and 
     *    test?
     *      
     * @param builder
     * @param nodeId
     * @return
     */
/*    protected TBControlTristateCheckBox createTristateInheritanceButton(KGUIBuilder builder, ENodeId nodeId) {
        
        return createTristateCheckBoxTB(builder, "Inherit from base test", 
                                "This tristate checkbox defines inheritance of base class section:\n" +
                                "unchecked   - section is not inherited even when defined in one of base classes\n" +
                                "intermediate - section is inherited only when it is defined in one of base tests, but not in current test\n" +
                                "checked     - section is always inherited, current section is cleared\n\n" +
                                "SHIFT key changes direction of state switching.",
                                "wrap",
                                CTestImportSources.ESectionSources.E_SECTION_IS_INHERIT.swigValue(), 
                                nodeId);
    } */

    
    protected TBControlTristateCheckBox createTristateInheritanceButton(KGUIBuilder builder, String migLayout) {
        
        return createTristateCheckBoxTB(builder, "Inherit", 
                                "This tristate checkbox defines inheritance of base class section:\n" +
                                "unchecked   - section is not inherited even when defined in one of base classes\n" +
                                "intermediate - section is inherited only when it is defined in one of base tests, but not in current test\n" +
                                "checked     - section is always inherited, current section is cleared\n\n" +
                                "SHIFT key changes direction of state switching.",
                                migLayout,
                                CTestImportSources.ESectionSources.E_SECTION_IS_INHERIT.swigValue(), 
                                m_nodeId);
    }

    
    private TBControlTristateCheckBox createTristateCheckBoxTB(KGUIBuilder builder, 
                                                               String label, 
                                                               String tooltip,
                                                               String layout,
                                                               int section, 
                                                               ENodeId nodeId) {
        
        return new TBControlTristateCheckBox(builder, label, tooltip, layout,
                                             section, nodeId, null);
    }
    
    
 /*   protected TBControlCheckBox createInheritanceCB(KGUIBuilder builder) {
        
        TBControlCheckBox isInheritTBCheckBox = 
                new TBControlCheckBox(builder, 
                                      "Inherit from base test",
                                      
                                      "If checked, and this section is empty in this test, but defined in one of base tests, then it is inherited from base test.\n" +
                                      "If unchecked, this section is never inherited.\n\n" +
                                      "If you want the section to be inherited, but it is not empty, run the 'Clear section' command\n" +
                                      "from context menu in sections tree on the left.",
                                      
                                      "gapleft 8, wrap", 
                                      CTestImportSources.ESectionSources.E_SECTION_IS_INHERIT.swigValue(), 
                                      ENodeId.OPTIONS_NODE, 
                                      null);
        isInheritTBCheckBox.setDefaultValue(true);
        
        return isInheritTBCheckBox;
    } */
    
    
    class InheritedActionProvider implements BoolActionProvider {

        SectionIds m_sectionId;
        
        InheritedActionProvider(SectionIds sectionId) {
            m_sectionId = sectionId;
        }
        
        @Override
        public AbstractAction getClearAction() {
            return createClearSectionAction(m_sectionId);
        }
        
        @Override
        public AbstractAction getCopyAction() {
            return createCopyMergedSectionAction(m_sectionId);
        }
    }
    
    
    @Override
    public CTestTreeNode createTestTreeNode() {
        return new CTestSpecification();
    }
    
    
    @Override
    public CTestTreeNode getTestTreeNode() {
        return m_testSpec;
    }
    
    
    protected Button createWizardBtn(KGUIBuilder builder, 
                                     String layout,
                                     String tooltip,
                                     final ENodeId nodeId,
                                     final GlobalsWizardDataPage wizardPage) {
        
        Button wizardBtn = builder.button("", layout);
        wizardBtn.setImage(IconProvider.INSTANCE.getIcon(IconProvider.EIconId.EWizard));
        UiTools.setToolTip(wizardBtn, tooltip);
        wizardBtn.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                addItemsWithWizardPage(wizardPage, nodeId);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        wizardBtn.setData(SWTBotConstants.SWT_BOT_ID_KEY, 
                          SWTBotConstants.GLOBAL_FUNCTIONS_WIZARD);
        
        return wizardBtn;
    }


    private void addItemsWithWizardPage(GlobalsWizardDataPage wizardPage, ENodeId nodeId) {

        setCurrentTS(SectionIds.E_SECTION_FUNC);  // function params are needed
        
        NewTCWizardDataModel ntcModel = 
                    NewTCWizardDataModel.createFromTestCase(m_currentTestSpec);
        
        // NewTCFunctionsPage page = new NewTCFunctionsPage(ntcModel);
        wizardPage.setModel(ntcModel);
        WizardPageDialog dlg = new WizardPageDialog(Activator.getShell(), wizardPage);
        if (dlg.show()) {
            AbstractAction action = wizardPage.createModelChangeAction(m_testSpec);

            action.addAllFireEventTypes();
            action.addDataChangedEvent(nodeId, m_testSpec);
            
            m_model.execAction(action);
        }
    }
}
