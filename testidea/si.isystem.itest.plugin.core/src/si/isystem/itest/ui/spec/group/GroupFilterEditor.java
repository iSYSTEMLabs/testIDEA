package si.isystem.itest.ui.spec.group;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.itest.common.FilterConfigPage;
import si.isystem.itest.model.AbstractAction;
import si.isystem.itest.model.ModelChangedEvent;
import si.isystem.itest.model.ModelChangedEvent.EventType;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.ui.spec.TestSpecificationEditorView.ENodeId;
import si.isystem.ui.utils.SelectionAdapter;

public class GroupFilterEditor extends GroupSectionEditor {

    private FilterConfigPage m_filterConfigPage;
    
    
    public GroupFilterEditor(ENodeId nodeId, ESectionCTestGroup ... sectionId) {
        super(nodeId, sectionId);
    }

    
    @Override
    public Composite createPartControl(Composite parent) {
        
        ScrolledComposite scrolledPanel = new ScrolledComposite(parent, SWT.V_SCROLL | 
                                                                        SWT.H_SCROLL);

        Composite mainPanel = new Composite(scrolledPanel, SWT.NONE);
        mainPanel.setLayoutData("wmin 0");
        
        MigLayout mig = new MigLayout("fill");
        mainPanel.setLayout(mig);
        
        // KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        m_filterConfigPage = new FilterConfigPage(FilterConfigPage.ContainerType.E_TREE,
                                                  true);
        m_filterConfigPage.createMainPanel(mainPanel);
        
        m_filterConfigPage.addApplyBtnListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                model.refreshGroups();
                model.getEventDispatcher().fireEvent(new ModelChangedEvent(EventType.TEST_SPEC_TREE_STRUCTURE_CHANGED));
            }
        });
        
        return configureScrolledComposite(scrolledPanel, mainPanel);
    }

    
    @Override
    public void fillControlls() {
        
        boolean isEnabled = m_testGroup != null;
        
        if (!isEnabled) {
            m_filterConfigPage.setInput(null, null, null);
            return;
        }
        
        // use also filters of parent groups for preview.
        List<CTestFilter> filterList = new ArrayList<>();
        CTestGroup parentGrp = m_testGroup.getParentGroup();

        while(parentGrp != null) {
        
            CTestFilter filter = parentGrp.getFilter(true);
            
            // skip empty filter as everything passes it
            if (!filter.isEmpty()) {
                filterList.add(filter);
            }
            parentGrp = parentGrp.getParentGroup();
        }
        
        m_filterConfigPage.setInput(m_model.getRootTestSpecification(), 
                                    m_testGroup.getFilter(false), 
                                    m_model,
                                    filterList.toArray(new CTestFilter[0]));
        
        m_filterConfigPage.refreshGlobals();
        m_filterConfigPage.fillControls();
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
        return new int[]{CTestGroup.ESectionCTestGroup.E_SECTION_FILTER.swigValue()};
    }
}
