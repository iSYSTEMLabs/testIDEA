package si.isystem.itest.ui.spec;

import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestSpecification.SectionIds;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.KGUIBuilder;

public class PreConditionsEditor  extends SectionEditorAdapter {

    private ExpressionsPanel m_exprPanel;

    public PreConditionsEditor() {
        super(ENodeId.PRE_CONDITIONS_NODE, SectionIds.E_SECTION_PRE_CONDITION);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {

        Composite mainPanel = createScrollable(parent);

        MigLayout mig = new MigLayout("fill", "fill", "[min!][fill][min!]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        m_exprPanel = new ExpressionsPanel(builder.getParent(),
                                           "gaptop 10, wmin 0, growx, growy, wrap",
                                           SWTBotConstants.BOT_PRE_CONDITIONS_TABLE,
                                           ENodeId.PRE_CONDITIONS_NODE,
                                           createTristateInheritanceButton(builder, "wrap"),
                                           new InheritedActionProvider(SectionIds.E_SECTION_PRE_CONDITION));
        
        m_exprPanel.setTooltip("Enter expressions, which must return true for test to execute.\n" +
                "It not all expressions evaluate to true, test will not be executed.\n" +
                "Use this section to terminate test case early and make error cause easier to find.\n" +
                "Examples:\n    retVal == 0\n    g_counter < 1020  &&&&  g_counter > 910\n" +
                "To specify format, append '@@ <format specifier>', for example:\n" +
                "    rv == 4  ||  g_cnt == 300  ||  i < 2 @@ d h b\n" +
                "writes 'rv' in decimal, 'g_cnt' in hex format and 'i' in binary format.\n" +
                "Use prefix '@' for registers, for example: @R1 ==42\n" +
                "Use prefix '`' (backquote) for IO module ports, for example: `DigitalIn.DIN0 == 1" 
                + "\n\nIMPORTANT: Host variables used as parameters for this script function\n"
                + "    must be defined in test case which executes before this one,\n"
                + "    because Variables section is processed AFTER pre-conditions are evaluated.\n"
                + "    is called.");
        
        return getScrollableParent(mainPanel);
    }

    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testSpec != null;
        m_exprPanel.setEnabled(isEnabled);
        
        if (!isEnabled) {
            setInputForInheritCb(null, m_exprPanel.getInheritExprTB());

            m_exprPanel.setInput(null, null, false, false);

            return;            
        }

        setInputForInheritCb(SectionIds.E_SECTION_PRE_CONDITION, 
                             m_exprPanel.getInheritExprTB());
        
        m_exprPanel.setVarsContentProposals(m_testSpec, m_currentCoreId);
        
        setCurrentTS(SectionIds.E_SECTION_PRE_CONDITION);
        m_exprPanel.setInput(m_testSpec,
                             m_currentTestSpec.getPrecondition(false),
                             false,
                             m_isInherited);

        m_exprPanel.selectionToTextInResultsView();

        m_exprPanel.setCoreId(m_currentCoreId);
    }


    @Override
    public boolean isError(CTestResult result) {
        return result.isPreConditionError()  ||  result.isStackUsageError();
    }

    
    @Override
    public boolean hasErrorStatus() {
        return !isEmpty();
    }
    
    
    @Override
    public void selectLineInTable(int tableId, int lineNo) {
        m_exprPanel.selectLineInTable(tableId, lineNo);
    }


    public void refreshGlobals(String coreId) {
        
        if (m_exprPanel == null) {
            return; // does not exist yet because of lazy init.
        }
        
        m_exprPanel.refreshGlobals(m_currentTestSpec, coreId);
    }    


    @Override
    public int [] getSectionIdsForTableEditor() {
        return new int[]{SectionIds.E_SECTION_PRE_CONDITION.swigValue()};
    }
}
