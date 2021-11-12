package si.isystem.itest.ui.spec.group;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.comp.TBControlText;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

public class GroupMetaEditor extends GroupSectionEditor {


    private TBControlText m_idControl;
    private TBControlText m_descriptionHierarchyControl;
    private Text m_resultCommentTxt;
    private Button m_showMarkdownBtn;
    // private TBControlCheckBox m_isExecuteCb;

    public GroupMetaEditor(ENodeId nodeId, ESectionCTestGroup ... sectionId) {
        super(nodeId, sectionId);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                SWT.H_SCROLL);
        
        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);

        MigLayout mig = new MigLayout("fillx", "[min!][fill][min!]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);

        // currently not used, because test cases can be in more than one group.
//        m_isExecuteCb = new TBControlCheckBox(builder, "Execute", 
//                                              "Check this box to enable the group. If unchecked,\n"
//                                              + "test in this group will not be executed during test run.", 
//                                              "skip, wmax pref, gaptop 10, gapbottom 10, wrap", 
//                                              ESectionCTestGroup.E_SECTION_IS_EXECUTE.swigValue(), 
//                                              ENodeId.GRP_META, null);

        builder.label("ID:", "gaptop 15");
        
        m_idControl = TBControlText.createForMixed(builder, 
                                                   "Group id should be descriptive and unique, so that we can " 
                                                   + "map test results to it.\n" 
                                                   + UiUtils.TEST_ID_ALLOWED_CHARS 
                                                   + "Examples:\n" 
                                                   + "    mathlib, strlib, loader, ...", 
                                                   "gapbottom 10, gaptop 15, wrap", 
                                                   ESectionCTestGroup.E_SECTION_GROUP_ID.swigValue(), 
                                                   m_nodeId, 
                                                   null, 
                                                   SWT.BORDER);
        
        m_idControl.setTestTreeRefreshNeeded(true);
        
        m_showMarkdownBtn = builder.checkBox("View / Edit", "skip, gaptop 15, wrap");
        UiTools.setToolTip(m_showMarkdownBtn, "If selected, description is shown according "
                + "to markdown tags. Editing is not possible in this mode.\n"
                + "Deselect the button to enable editing. See tooltip of text field below for help on supported markdown.");
        m_showMarkdownBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.getSource() instanceof Button) {
                    boolean isStyledText = ((Button)e.getSource()).getSelection();
                    m_descriptionHierarchyControl.showStyledText(isStyledText);
                }
            }
        });

        builder.label("Description:");
        m_descriptionHierarchyControl = 
                TBControlText.createForStyledTextMixed(builder, 
                       "Human readable test description.\n" + UiUtils.MARKDOWN_HELP,
                       "wmin 100, h 150:50%:, gapright 5, gapbottom 10, wrap, span 2", 
                       ESectionCTestGroup.E_SECTION_DESCRIPTION.swigValue(), 
                       m_nodeId, 
                       null, 
                       SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        
        builder.label("Result comment:", "");
        m_resultCommentTxt = builder.text("growx, hmin 80", 
                                          SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        Text rcDescTxtLbl = builder.text("", SWT.MULTI);
        rcDescTxtLbl.setEditable(false);
        rcDescTxtLbl.setText("This text refers to specific\n"
                           + "test run. It is stored to\n"  
                           + "results and report only,\n"
                           + "and will be lost on next run!");
        
        m_resultCommentTxt.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                CTestGroupResult testResult = m_model.getGroupResult(m_testGroup);
                if (testResult != null) {
                    testResult.setTagValue(CTestGroupResult.EGroupResultSection.E_SECTION_RESULT_COMMENT.swigValue(),
                                           m_resultCommentTxt.getText());
                }
            }
            
            @Override
            public void focusGained(FocusEvent e) {}
        });

        return configureScrolledComposite(scrolledPanel, mainPanel);
   }

    
    @Override
    public void fillControlls() {

        boolean isEnabled = m_testGroup != null;
        
        // m_isExecuteCb.setEnabled(isEnabled);
        m_idControl.setEnabled(isEnabled);
        m_descriptionHierarchyControl.setEnabled(isEnabled);
        m_resultCommentTxt.setEnabled(isEnabled);

        if (!isEnabled) {
            // m_isExecuteCb.clearInput();
            m_idControl.clearInput();
            m_descriptionHierarchyControl.clearInput();
            m_resultCommentTxt.setText("");
            return;            
        }

        m_idControl.setInput(m_testGroup, false);
        // m_isExecuteCb.setInput(m_testGroup, false);
 
        m_descriptionHierarchyControl.showStyledText(false); // reset the state to normal
        m_descriptionHierarchyControl.setInput(m_testGroup, false); 
        m_descriptionHierarchyControl.showStyledText(m_showMarkdownBtn.getSelection());

        CTestGroupResult testResult = m_model.getGroupResult(m_testGroup);
        if (testResult != null) {
            m_resultCommentTxt.setText(testResult.getResultComment());
        } else {
            m_resultCommentTxt.setText("");
        }
        
        m_resultCommentTxt.setEnabled(testResult != null);
        
    }
    
    
    @Override
    public void clearSection() {
        
        AbstractAction action = createClearGroupSectionAction();
        action.addAllFireEventTypes();
        action.addTreeChangedEvent(null, null); // required so that test ID in Outline view changes
        action.addDataChangedEvent(m_nodeId, m_testGroup);
        TestSpecificationModel.getActiveModel().execAction(action);
    }


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{ESectionCTestGroup.E_SECTION_GROUP_ID.swigValue(),
                ESectionCTestGroup.E_SECTION_DESCRIPTION.swigValue(),
                ESectionCTestGroup.E_SECTION_IS_EXECUTE.swigValue(),
                ESectionCTestGroup.E_SECTION_MERGED_ANALYZER_FILE.swigValue()};
    }
}