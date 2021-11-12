package si.isystem.itest.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.miginfocom.swt.MigLayout;
import si.isystem.commons.utils.ISysFileUtils;
import si.isystem.connect.CTestFilter;
import si.isystem.connect.CTestFilter.ETestFilterSectionIds;
import si.isystem.connect.CTestGlobalsContainer;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroup.ESectionCTestGroup;
import si.isystem.connect.StrVector;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.dialogs.GNode.ENodeType;
import si.isystem.itest.handlers.ToolsRefreshGlobalsCmdHandler;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;
import si.isystem.ui.utils.UiTools;

/**
 * This dialog shows check-box tree of partitions, modules, and functions.
 * By selecting items in the tree user creates groups with filter configured for 
 * the selected item.
 *   
 * @author markok
 *
 */
public class GroupCreationDialog extends Dialog {
    
    private static final String DEFAULT_CORE = "<default core>";

    public static String DLG_TITLE = "Create groups of test cases";

    private CheckboxTreeViewer m_treeViewer;

    private CTestGroup m_rootGroup;

    private GNode m_rootNode;

    private Button m_isCreateEmptyGroupsCb;

    private boolean m_isCreateEmptyGroups;

    private GNodeFilter m_filter;

    private MutableInt m_grpCounter;

    
    public GroupCreationDialog(Shell parentShell, long noOfGroups) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        //try to make unique group ID by increasing start index each time
        m_grpCounter = new MutableInt(noOfGroups);
    }

    
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);

        getShell().setText(DLG_TITLE);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.heightHint = 600;  // sets initial dialog size
        gridData.widthHint = 500;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(new MigLayout("fill", "[min!][fill][min!]", "[min!][min!][fill][min!]"));
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);

        builder.label("Select items for groups:", "span 2");
        Button refreshButton = builder.button(IconProvider.INSTANCE.getIcon(EIconId.ERefresh), 
                                                                            "al right, wrap");
        
        refreshButton.setToolTipText("Refresh symbols");
        
        refreshButton.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                ToolsRefreshGlobalsCmdHandler refreshHandler = new ToolsRefreshGlobalsCmdHandler();
                refreshHandler.execute(null);
                fillData();
            }
        });
        
        builder.label("Modules filter:");
        
        Text txt = builder.text("span 2, wrap", SWT.BORDER);
        UiTools.setToolTip(txt, "Enter part of module name to show only modules which match it.");
        txt.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                try {
                    m_filter.setRegEx(((Text)e.widget).getText());
                    m_treeViewer.refresh();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });
        
        m_treeViewer = new CheckboxTreeViewer(builder.getParent());
        m_treeViewer.getTree().setLayoutData("grow, wmin 0, hmin 0, span 3, wrap");
        GroupMemberContentProvider treeContentProvider = new GroupMemberContentProvider();
        m_treeViewer.setContentProvider(treeContentProvider);
        
        m_treeViewer.setLabelProvider(new GroupMemberLabelProvider());

        // when user clicks a checkbox in the tree, check/uncheck all its children
        m_treeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                // check/uncheck all children
                GNode vNode = (GNode)event.getElement();
                m_treeViewer.setSubtreeChecked(vNode, event.getChecked());
                m_treeViewer.setGrayed(vNode, false);
                
                if (vNode.m_parent != null) {
                    // updateStateOfParent(m_treeViewer, vNode.m_parent, EChildrenState.ALL_UNCHECKED);
                }
            }
        });

        m_filter = new GNodeFilter();
        m_treeViewer.addFilter(m_filter);
        
        m_isCreateEmptyGroupsCb = builder.checkBox("Create also empty function groups", "span 2, gaptop 7");
        UiTools.setToolTip(m_isCreateEmptyGroupsCb, 
                           "If checked, groups with filter based on function name, for which there exist no test cases in the current document, will also be created.\n"
                           + "Usage example: If test cases already exist, and you just want to group them, then uncheck this button.\n"
                           + "If you are creating groups in a new document, leave this button checked.");
        
        // Create all groups by default - user has to avoid creation explicitly.
        m_isCreateEmptyGroupsCb.setSelection(true);
        
        fillData();
        
        return composite;
    }
    
    
    private void fillData() {
        CTestGlobalsContainer globalsContainer = 
                GlobalsConfiguration.instance().getGlobalContainer().getCGlobalsContainer();
        
        StrVector coreIds = new StrVector();
        globalsContainer.getCores(coreIds);
        m_rootNode = new GNode("rootNode", null, null);
        
        int numCores = (int) coreIds.size();
        for (int coreIdx = 0; coreIdx < numCores; coreIdx++) {
            
            String coreId = coreIds.get(coreIdx);
            String coreName = coreId;
            if (coreId.isEmpty()) {
                coreName = DEFAULT_CORE;
            }
            GNode coreNode = m_rootNode.addChild(coreName, ENodeType.CORE);
            
            StrVector partitions = new StrVector();
            globalsContainer.getPartitions(coreId, false, partitions);
            
            int numPartitions = (int) partitions.size();
            for (int partIdx = 0; partIdx < numPartitions; partIdx++) {
                String partitionName = partitions.get(partIdx);
                GNode partitionNode = coreNode.addChild(partitionName, ENodeType.PARTITION);
                
                StrVector modules = new StrVector();
                globalsContainer.getModules(coreId, partIdx, true, modules);
                
                int numModules = (int) modules.size();
                for (int moduleIdx = 0; moduleIdx < numModules; moduleIdx++) {
                    
                    StrVector functions = new StrVector();
                    globalsContainer.getFunctionsForModule(coreId, partIdx, moduleIdx, functions);
                    int numFunctions = (int) functions.size();

                    // add only module, which contain functions. For example, header
                    // files are also modules, but usually contain no functions.
                    if (numFunctions > 0) {
                        String moduleName = ISysFileUtils.pathForRegEx(modules.get(moduleIdx));
                        GNode moduleNode = partitionNode.addChild(moduleName, ENodeType.MODULE);
                        // Only modules are filterable, since there may be many of them, much more than
                        // cores or partitions (few hundreds in case of Hella, Rom.). Functions are at 
                        // the moment not filterable, since 
                        // 100 functions per module are manageable. If functions are also a problem,
                        // add additional filter for functions.
                        moduleNode.setFilterable(true);
                        
                        for (int funcIdx = 0; funcIdx < numFunctions; funcIdx++) {
                            moduleNode.addChild(functions.get(funcIdx), ENodeType.FUNCTION);
                        }
                    }
                }
            }
            
            coreNode.sort();
        }
        
        m_treeViewer.setInput(m_rootNode);
        
        // By default expand all cores, and the first partition. If we want to have
        // elements expanded, all elements on the path from root must be expanded.
        List<GNode> initiallyExpandedElements = new ArrayList<>();
        initiallyExpandedElements.addAll(m_rootNode.getChildren());  // cores
        
        if (!m_rootNode.getChildren().isEmpty()) {
            GNode partitions = m_rootNode.getChildren().get(0);
            initiallyExpandedElements.addAll(partitions.getChildren());

            // do not expand partitions - enable later if needed.
//            if (!partitions.getChildren().isEmpty()) {
//                GNode modules = partitions.getChildren().get(0);
//                initiallyExpandedElements.addAll(modules.getChildren());
//            }
        }
        
        m_treeViewer.setExpandedElements(initiallyExpandedElements.toArray());
    }
    

    private void saveData() {
        m_rootGroup = new CTestGroup();
        
        MutableObject<CTestGroup> currentParentGrp = new MutableObject<>(m_rootGroup); 
        List<GNode> cores = m_rootNode.getChildren();
        CTestGroup parentForCores = currentParentGrp.getValue();
        
        for (GNode core : cores) {
            
            CTestFilter filter = createGroupIfChecked(core, currentParentGrp, m_grpCounter, null);
            if (filter != null) {
                String coreId = core.getName();
                if (coreId.equals(DEFAULT_CORE)) {
                    coreId = ""; // hack to revert replacement performed above for better UI look
                }
                filter.setTagValue(ETestFilterSectionIds.E_SECTION_CORE_ID.swigValue(),
                                   coreId);
            }
            
            CTestGroup parentCoreGrp = currentParentGrp.getValue();
            List<GNode> partitions = core.getChildren();
            for (GNode partition : partitions) {
                createGroupIfChecked(partition, currentParentGrp, m_grpCounter, 
                                     ETestFilterSectionIds.E_SECTION_PARTITIONS);
                
                List<GNode> modules = partition.getChildren();
                CTestGroup parentPartitionGrp = currentParentGrp.getValue();
                for (GNode module : modules) {
                    createGroupIfChecked(module, currentParentGrp, m_grpCounter, 
                                         ETestFilterSectionIds.E_SECTION_MODULES);
                    
                    List<GNode> functions = module.getChildren();
                    CTestGroup parentModuleGrp = currentParentGrp.getValue();
                    for (GNode function : functions) {
                        createGroupIfChecked(function, currentParentGrp, m_grpCounter, 
                                             ETestFilterSectionIds.E_SECTION_INCLUDED_FUNCTIONS);
                        currentParentGrp.setValue(parentModuleGrp);
                    }
                    
                    currentParentGrp.setValue(parentPartitionGrp);
                }
                
                currentParentGrp.setValue(parentCoreGrp);
            }
            
            currentParentGrp.setValue(parentForCores);
        }
        
        m_isCreateEmptyGroups = m_isCreateEmptyGroupsCb.getSelection();
    }
    

    private CTestFilter createGroupIfChecked(GNode node, 
                                         MutableObject<CTestGroup> currentParentGrp, 
                                         MutableInt grpCounter,
                                         ETestFilterSectionIds sectionId) {
        
        if (m_treeViewer.getChecked(node)) {
            CTestGroup group = new CTestGroup();
            group.setTagValue(ESectionCTestGroup.E_SECTION_GROUP_ID.swigValue(), 
                              "g" + grpCounter);
            grpCounter.increment();
            currentParentGrp.getValue().addChildAndSetParent(-1, group);
            // If core node is not checked, but child nodes are, the child 
            // groups will be added directly to root.
            currentParentGrp.setValue(group);
            
            CTestFilter filter = group.getFilter(false);
            if (sectionId != null) {
                filter.setTagValue(sectionId.swigValue(), -1, node.getName());
            }
            
            return filter;
        }

        return null;
    }
    
    
//    private void createGroupsRecursively(GNode node, CTestGroup parent, boolean isAppendPartition) {
//        
//        List<GNode> children = node.getChildren();
//        if (children == null) {
//            return;
//        }
//        
//        for (GNode child : children) {
//            if (child.getChildren() == null  &&  m_treeViewer.getChecked(child)) {
//                vars.add(child.m_qName);
//            } 
//            
//            createGroupsRecursively(child, vars, isAppendPartition);
//        }
//    }

    
    @Override
    protected void okPressed() {
        saveData(); // while the tree viewer is still available
        
        super.okPressed();
    }
    
    
    public boolean show() {
        return open() == Window.OK;
    }


    public CTestGroup getData() {
        return m_rootGroup;
    }
    
    
    public boolean isCreateEmptyGroups() {
        return m_isCreateEmptyGroups;
    }
}



class GNode implements Comparable<GNode> {

    public enum ENodeType {CORE, PARTITION, MODULE, FUNCTION}
    
    private ENodeType m_type;

    private String m_name;
    GNode m_parent;
    private List<GNode> m_children = new ArrayList<>();
    private boolean m_isFilterable;
    
    public GNode(String name,
                 ENodeType nodeType,
                 GNode parent) {
        m_name = name;
        m_type = nodeType;
        m_parent = parent;
        
    }


    GNode addChild(String childName, ENodeType nodeType) {
        if (m_children == null) {
            m_children = new ArrayList<>();
        }
        GNode childNode = new GNode(childName, nodeType, this);
        m_children.add(childNode);
        return childNode;
    }
    
    
    public List<GNode> getChildren() {
        return m_children;
    }


    public ENodeType getType() {
        return m_type;
    }


    String getName() {
        
        return m_name;
    }
    
    
    public boolean isFilterable() {
        return m_isFilterable;
    }


    public void setFilterable(boolean isFilterable) {
        m_isFilterable = isFilterable;
    }


    void sort() {
        Collections.sort(m_children);
        
        for (GNode child : m_children) {
            child.sort();
        }
    }
    
    
    @Override
    public int compareTo(GNode o) {
        return m_name.compareTo(o.m_name);
    }
}


class GNodeFilter extends ViewerFilter {
    
//    private Pattern m_pattern = null;
    private String m_filterStr;

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        
        if (m_filterStr == null  ||  m_filterStr.isEmpty()) {
            return true;
        }
        
        GNode groupNode = (GNode)element;

        if (groupNode.isFilterable()) {
            String nodeName = groupNode.getName();
        
            return nodeName.toUpperCase().contains(m_filterStr);
        }
        
        return true;
    }
    
    
    void setRegEx(String regEx) {
        m_filterStr = regEx.toUpperCase();
    }
}


class GroupMemberContentProvider implements ITreeContentProvider {


    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    
    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    
    @Override
    public Object[] getChildren(Object parentElement) {
        GNode gnode = (GNode)parentElement;
        if (gnode.getChildren() == null) {
            return new Object[0];
        }
        return gnode.getChildren().toArray(new GNode[0]);
    }

    
    @Override
    public Object getParent(Object element) {
        GNode vnode = (GNode)element;
        return vnode.m_parent;
    }

    
    @Override
    public boolean hasChildren(Object element) {
        GNode vnode = (GNode)element;
        return vnode.getChildren() != null  &&  !vnode.getChildren().isEmpty();
    }


    @Override
    public void dispose() {
    }
}


class GroupMemberLabelProvider implements ILabelProvider {

    @Override
    public void addListener(ILabelProviderListener listener) {
    }


    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Image getImage(Object element) {
        GNode gnode = (GNode)element;
        IconProvider icons = IconProvider.INSTANCE;
        switch(gnode.getType()) {
        case CORE:
            return icons.getIcon(EIconId.ECore);
        case PARTITION:
            return icons.getIcon(EIconId.EPartition);
        case MODULE:
            return icons.getIcon(EIconId.EModule);
        case FUNCTION:
            return icons.getIcon(EIconId.EFunction);
        default:
            return null;
        }
    }

    
    @Override
    public String getText(Object element) {
        GNode gnode = (GNode)element;
        return gnode.getName();
    }
    @Override
    public void dispose() {
    }
}
