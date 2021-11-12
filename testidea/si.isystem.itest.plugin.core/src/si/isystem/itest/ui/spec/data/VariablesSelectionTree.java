package si.isystem.itest.ui.spec.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;

import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.VariablesGlobalsProvider;
import si.isystem.connect.CDataController;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CValueType;
import si.isystem.connect.IConnectDebug.EAccessFlags;
import si.isystem.connect.StrStrMap;
import si.isystem.connect.data.JCompoundType;
import si.isystem.connect.data.JVariable;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.common.GlobalsConfiguration;
import si.isystem.itest.common.SimpleProgressMonitor;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

/**
 * This class displays variables in a tree with check-boxes. User can select
 * variables or structure members. Selected items are returned as a string list.
 * It is intended to be used as part in other UI components. 
 */
public class VariablesSelectionTree {

    private static final String DEFAULT_ARRAY_DIM = "3";

    // Limit the number of elements in arrays, otherwise some array of 100000
    // elements would make the dialog unusable. Furthermore, even 100 elements
    // are a lot to enter manually - script should be used.
    private static final int MAX_ARRAY_LEN = 100;

    private static final String MORE_ELEMENTS_EXIST_STR = "...";
    
    private static final int MAX_ITEMS_IN_STRUCT = 10000;
    
    
    private enum EChildrenState {ALL_UNCHECKED, MIXED, ALL_CHECKED};

    private Button m_isAppendPartitionCb;
    private CheckboxTreeViewer m_treeViewer;
    private Job m_job;
    private IProgressMonitor m_progressMonitor = new SimpleProgressMonitor();

    private String m_coreId;

    private int m_recursionCount;
    private List<VNode> m_initVars;

    private StrStrMap m_localVars;

    private CDataController m_dataCtrl;
    private static boolean m_isAppendPartition = false; // keep setting during one testIDEA session

    /**
     * For test local vars we use a trick to get type info for non-existent 
     * variable, for example to get type info for 'MyStruct a', we use *(MyStruct *)0
     * Since original name must be entered into init table, this class contains
     * information for restoring original name after var type is traversed.  
     */
    class LocalVarAliases {
        String m_varName;
        String m_replacementString;  // string used in final result
        String m_replacementTarget;  // string in leaf node names to be replaced with m_replacementString 
        String m_rootNodeReplacementTarget; // string in root node names to be replaced with m_replacementString
        public ArrayList<Integer> m_dimensions;
    }
    
    public VariablesSelectionTree(String coreId, CTestSpecification testSpec) {
        m_coreId = coreId;
        m_localVars = new StrStrMap();
        testSpec.getLocalVariables(m_localVars);
    }
    
    
    public VariablesSelectionTree(String coreId) {
        m_coreId = coreId;
        m_localVars = new StrStrMap();
    }
    
        
    public void setLocalVars(StrStrMap localVars) {
        m_localVars = localVars;
    }


    public void createControl(KGUIBuilder builder, 
                              String treeLayoutData, 
                              String cboxLayoutData) {
        
        m_treeViewer = new CheckboxTreeViewer(builder.getParent());
        m_treeViewer.getTree().setLayoutData(treeLayoutData);
        MembersContentProvider treeContentProvider = new MembersContentProvider();
        // treeContentProvider.inputChanged(treeViewer, null, newInput);
        m_treeViewer.setContentProvider(treeContentProvider);
        m_treeViewer.setLabelProvider(new MembersLabelProvider());
        // List<VNode> newInput = new ArrayList<>(); 
        // treeViewer.setInput(newInput); // pass a non-null that will be ignored

        // when user clicks a checkbox in the tree, check/uncheck all of its children
        m_treeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                // check/uncheck all children
                VNode vNode = (VNode)event.getElement();
                m_treeViewer.setSubtreeChecked(vNode, event.getChecked());
                m_treeViewer.setGrayed(vNode, false);
                
                if (vNode.m_parent != null) {
                    updateStateOfParent(m_treeViewer, vNode.m_parent, EChildrenState.ALL_UNCHECKED);
                }
            }
        });
        
        createPartitionCheckbox(builder, cboxLayoutData);
    }
    

    private void createPartitionCheckbox(KGUIBuilder builder, String layoutData) {
        
        m_isAppendPartitionCb = builder.checkBox("Append download file name in table", 
                                                 layoutData);
        
        UiTools.setToolTip(m_isAppendPartitionCb, "If checked, name of download file will be appended to each member,\n"
                + "for example:  myStruct.counter,,out.elf\n"
                + "Use this setting when there is more than one download file, and variable is declared\n"
                + "in file which is not set as default one in winIDEA.");
        
        m_isAppendPartitionCb.setSelection(m_isAppendPartition);
    }
    
    
    /**
     * This method sets node state depending on the state of children: unchecked, 
     * grayed, or checked. This way user is informed about node state, even if it
     * is collapsed.
     *  
     * @param treeViewer
     * @param vNode
     * @param childState
     */
    private void updateStateOfParent(CheckboxTreeViewer treeViewer, 
                                     VNode vNode, 
                                     EChildrenState childState) {
        
        EChildrenState childrenState;
        
        if (childState == EChildrenState.MIXED) {
            // if one of children has some items checked and some unchecked, 
            // all parents have grayed state.
            treeViewer.setChecked(vNode, true);
            treeViewer.setGrayed(vNode, true);
            childrenState = EChildrenState.MIXED;
            
        } else {

            childrenState = getChildrenState(treeViewer, vNode);

            switch (childrenState) {
            case ALL_UNCHECKED:
                treeViewer.setChecked(vNode, false);
                treeViewer.setGrayed(vNode, false);
                break;
            case MIXED:
                treeViewer.setChecked(vNode, true);
                treeViewer.setGrayed(vNode, true);
                break;
            case ALL_CHECKED:
                treeViewer.setChecked(vNode, true);
                treeViewer.setGrayed(vNode, false);
                break;
            default:
                break;
            }
        }
        
        if (vNode.m_parent != null) {
            updateStateOfParent(treeViewer, vNode.m_parent, childrenState);
        }
    }
    
    
    private EChildrenState getChildrenState(CheckboxTreeViewer treeViewer, 
                                            VNode parentNode) {
        
        EChildrenState state = null;
        
        if (parentNode.m_children == null) {
            return null;
        }
        
        for (VNode child : parentNode.m_children) {
            if (treeViewer.getChecked(child)) {
                if (state == null) {
                    state = EChildrenState.ALL_CHECKED;
                } else {
                    if (state == EChildrenState.ALL_UNCHECKED) {
                        return EChildrenState.MIXED;
                    }
                }
            } else {
                if (state == null) {
                    state = EChildrenState.ALL_UNCHECKED;
                } else {
                    if (state == EChildrenState.ALL_CHECKED) {
                        return EChildrenState.MIXED;
                    }
                }
            } 
        
            EChildrenState childState = getChildrenState(treeViewer, child);
            
            if (childState != null) {
                if (childState == EChildrenState.MIXED) {
                    return childState; // immediately return is MIXED state
                }

                if (childState != state) {
                    return EChildrenState.MIXED;
                }
            }
            
            // not a mixed state, either ALL_CHECKED or ALL_UNCHECKED, continue
        }
        
        return state;
    }
    

    private void getTypeHierarchy(List<String> globalVarNames, List<VNode> expandedNodes) {
        
        VNode root = new VNode("", "", "", null);
        expandedNodes.add(root);
        
        JConnection jConnectionMgr = ConnectionProvider.instance().getDefaultConnection();
        if (jConnectionMgr != null) {
            m_dataCtrl = jConnectionMgr.getCDataController(m_coreId);
        } else {
            m_dataCtrl = null;
        }

        // if local var with this name exist, it has higher priority over
        // global var. Use the trick told by JU, '*(<type>*)0' for
        // variable without instance, when only type is known.
        List<LocalVarAliases> varNameAliases = new ArrayList<>();

        for (String globalVarName : globalVarNames) {

            LocalVarAliases lVarAliases = new LocalVarAliases();
            
            // local vars need special handling as there is no instance at the time
            // writing test case.
            if (m_localVars.containsKey(globalVarName)) {
                
                String varType = m_localVars.get(globalVarName);

                boolean isPointer = false;
                if (varType.endsWith("]")){ // arrays are not handled correctly with *(<type> *)0 trick
                    varType = parseArrayType(varType, lVarAliases);
                }  
                
                // handle pointer to struct by getting basic type and adding 
                // dereferencing operator to assignment expression
                if (varType.contains("*")) {
                    varType = varType.replace('*', ' ').trim();
                    lVarAliases.m_replacementString = '*' + globalVarName;
                    isPointer = true;
                } else {
                    lVarAliases.m_replacementString = globalVarName;
                }

                // because test local vars have no instance at time of writing test,
                // we need to use this hack with pointers to get members of a type.
                globalVarName = DataUtils.createDummyVarFromType(varType);
                lVarAliases.m_rootNodeReplacementTarget = globalVarName; // root node has no redundant ()  
                if (isPointer) {
                    lVarAliases.m_replacementTarget = globalVarName;
                } else {
                    // replace also parentheses, because they are not needed for non-pointer vars
                    lVarAliases.m_replacementTarget = '(' + globalVarName + ')'; 
                }
            } 

            lVarAliases.m_varName = globalVarName;
            varNameAliases.add(lVarAliases);
        }
        
        m_recursionCount = 0;
        
        VariablesGlobalsProvider varsGlobalsProvider = GlobalsConfiguration.instance(). 
                getGlobalContainer().getVarsGlobalsProvider(m_coreId);
                
        startHiearchyJob(varNameAliases,
                         expandedNodes,
                         root,
                         varsGlobalsProvider,
                         m_isAppendPartitionCb.getSelection());
    }


    /**
     * Handle arrays here, because the trick with *(<type> *)0
     * does not work for arrays. This feature as implemented here
     * works for simple cases like char [10] only. It does not 
     * support char[$HostVar] for example.
     * @return type without array dimensions, for example 'MyStruct'. Dimensions are
     * stored to lVarAliases.dimensions.
     */
    private String parseArrayType(String varType, LocalVarAliases lVarAliases) {
        Pattern regEx = Pattern.compile("\\[ *(\\w*) *\\]"); // matches: [ 10]
        
        Matcher m = regEx.matcher(varType);
        int start = -1;
        while (m.find()) {
            //System.out.println(m.group() + " , " + m.group(1) + ", " + m.start());
            if (start < 0) {
                start = m.start();
                varType = varType.substring(0, start); // remove array dim from type in array
                lVarAliases.m_dimensions = new ArrayList<>();
            }
            String dimensionStr = m.group(1).trim();
            if (!dimensionStr.isEmpty()) {
                char firstChar = dimensionStr.charAt(0);
                if (!Character.isDigit(firstChar)) {
                    if (firstChar == HostVarsUtils.HOST_VARX_PREFIX) {
                        dimensionStr = DEFAULT_ARRAY_DIM; // currently host vars are not known in design time,
                               // use some magic number. User can copy-paste additional or delete 
                               // existing elements
                    } else {
                        try {
                            if (m_dataCtrl != null) {
                                CValueType val = m_dataCtrl.evaluate(EAccessFlags.fMonitor, dimensionStr);
                                dimensionStr = val.getResult();
                            }
                        } catch (Exception ex) {
                            dimensionStr = DEFAULT_ARRAY_DIM; // dimension expr. is not known,
                            // use a magic number. User can copy-paste additional or delete 
                            // existing elements
                        }
                    }
                }
            } else {
                dimensionStr = "1"; // probably error in declaration, use something meaningful
            }
            
            lVarAliases.m_dimensions.add(Integer.valueOf(dimensionStr));
        }        
        
        return varType;
    }


    protected void startHiearchyJob(final List<LocalVarAliases> varNameAliases,
                                    final List<VNode> expandedNodes,
                                    final VNode root,
                                    final VariablesGlobalsProvider varsGlobalsProvider,
                                    final boolean isAppendPartition) {
        
        m_initVars = new ArrayList<>();

        // cancel previous job if exists
        if (m_job != null) {
            m_progressMonitor.setCanceled(true);
            while (m_job.getState() != Job.NONE);
        }
        
        // creata a new job with progress monitor
        m_job = new Job("Get struct hierarchy Job") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                
                try {
                    
                    for (LocalVarAliases varAliases : varNameAliases) {
                        VNode node = getHierarchyTree(varsGlobalsProvider, varAliases.m_varName, 
                                                      root, expandedNodes, monitor);
                        if (monitor.isCanceled()) {
                            break;
                        }

                        if (node != null) {
                            if (varAliases.m_dimensions != null  &&  !varAliases.m_dimensions.isEmpty()) {
                                String []dims = getIndexTuples(varAliases.m_dimensions);

                                for (String dim : dims) {
                                    VNode arrayNode;
                                    // Rename top node if test local var is used.
                                    // Root node does not have () like children returned by getExpressionInfo() do have,
                                    // for example: *(struct_td *)0, and not (*(struct_td *)0)
                                    if (dim != null) {
                                        arrayNode = node.createClone();
                                        renamePtrNode(arrayNode,
                                                      varAliases.m_rootNodeReplacementTarget,
                                                      varAliases.m_replacementString + dim,
                                                      isAppendPartition);

                                        arrayNode.m_replacementTarget = varAliases.m_replacementTarget; 
                                        arrayNode.m_replacementString = varAliases.m_replacementString + dim;
                                    } else {
                                        // more than MAX_ARRAY_LEN elements exist in the array
                                        arrayNode = new VNode(MORE_ELEMENTS_EXIST_STR, 
                                                              "", "", 
                                                              node);
                                    }

                                    m_initVars.add(arrayNode);
                                }
                            } else {
                                renamePtrNode(node,
                                              varAliases.m_rootNodeReplacementTarget,
                                              varAliases.m_replacementString,
                                              isAppendPartition);

                                node.m_replacementTarget = varAliases.m_replacementTarget; 
                                node.m_replacementString = varAliases.m_replacementString;

                                m_initVars.add(node);
                            }
                        } 
                    }

                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            
                            m_treeViewer.setInput(m_initVars);
                            
                            m_treeViewer.setExpandedElements(expandedNodes.toArray());
                            // check all items - usually user will add most of them 
                            if (!m_initVars.isEmpty()) {
                                for (VNode node : m_initVars) {
                                    m_treeViewer.setSubtreeChecked(node, true);
                                }
                            }
                        }
                    });
                    
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    Activator.log(IStatus.ERROR, "Can not get struct hierarchy", 
                                  ex);
                }
                
                return Status.OK_STATUS;
            }

        };

        IJobManager manager = Job.getJobManager();
        
        ProgressProvider provider = new ProgressProvider() {
            @Override
            public IProgressMonitor createMonitor(Job job) {
                return m_progressMonitor;
            }
        };
          
        manager.setProgressProvider(provider);
        m_progressMonitor.setCanceled(false);
        
        m_job.schedule();
    }
    
    
    /**
     * Retrieves hierarchy of type members, for example structure and its members.
     * Result is given as children of input parameter 'parent'.
     *  
     * @param varsGlobalsProvider
     * @param varName name of global variable or *(<type> *)0 for test local 
     *                     variables, to artificially 'create' an instance
     * @param parent this structure will contain type hierarchy, each node one element
     * @param expandedNodes nodes, which should be expanded in tree view. For example, 
     *                      it makes sense to expand structures, but not arrays, as the
     *                      later may have too many elements.
     * @param monitor
     * @return 
     */
    private VNode getHierarchyTree(VariablesGlobalsProvider varsGlobalsProvider,
                                  String varName, 
                                  VNode parent, 
                                  List<VNode> expandedNodes,
                                  IProgressMonitor monitor) {
        
        if (monitor.isCanceled()) {
            return null;
        }
        
        try {
            JCompoundType compoundInfo = 
                    varsGlobalsProvider.getExpressionTypeInfo(varName);
            
            if (compoundInfo == null) {
                return null; // there is no connection to winIDEA
            }
            
            // safety measure to prevent testIDEA crash, because it has no 
            // control on names of children returned by iconnect.
            m_recursionCount++;
            if (m_recursionCount > MAX_ITEMS_IN_STRUCT) {
                return null;
            }

            JVariable typeInfo = compoundInfo.getExpressionInfo();
            
            // cut away prefix, for example: "(g_struct).m_i" -->  "m_i"
            String memberName = typeInfo.getName();
            int dotIdx = memberName.lastIndexOf('.');
            if (dotIdx >= 0) {
                memberName = memberName.substring(dotIdx + 1, memberName.length());
            }
            
            VNode typeNode = parent.addChild(memberName, 
                                             typeInfo.getVarTypeName(),
                                             typeInfo.getQualifiedName());

            long numArrayElements = Math.min(typeInfo.getArrayLen(), MAX_ARRAY_LEN); 
            if (numArrayElements == 0) {
                expandedNodes.add(typeNode);
            }
            
            // add struct children
            JVariable[] children = compoundInfo.getChildren();
            
            for (JVariable child : children) {
                getHierarchyTree(varsGlobalsProvider, 
                                 child.getQualifiedName(), 
                                 typeNode, 
                                 expandedNodes,
                                 monitor);
            }
                
            // add array elements
            for (int idx = 0; idx < numArrayElements; idx++) {
                
                String elementType = typeInfo.getVarTypeName();
                int endOfNameIdx = elementType.lastIndexOf('[', Integer.MAX_VALUE);
                
                elementType = elementType.substring(0, endOfNameIdx).trim();
                String elementIdx = "[" + idx + "]";
                
                getHierarchyTree(varsGlobalsProvider,
                                 typeInfo.getName() + elementIdx, 
                                 typeNode,  // arrayNode
                                 expandedNodes,
                                 monitor);
            }
                    
            if (numArrayElements > 0  &&  typeInfo.getArrayLen() > numArrayElements) {
                typeNode.addChild(MORE_ELEMENTS_EXIST_STR, "", "");
            }

            return typeNode;
        } catch (Exception ex) {
            return null; // connection to winIDEA is established, but there is no 
                    // type info for the given expression
        }
    }

    
    protected void renamePtrNode(VNode vNode,
                                 String replacementTarget,
                                 String replacementString,
                                 boolean isAppendPartition) {
        if (replacementTarget != null) {
            
            // replace hack *(varType*)0 with test local varName, for example:
            // *(struct_td *)0.m_i --> myTestVar.m_i
            vNode.m_qName = vNode.m_qName.replace(replacementTarget, 
                                                  replacementString);
            
            vNode.m_name = vNode.m_name.replace(replacementTarget, 
                                                replacementString);
        }
        
        if (!isAppendPartition) {
            int dlFileIdx = vNode.m_qName.indexOf(",,");
            if (dlFileIdx >= 0) {
                vNode.m_qName = vNode.m_qName.substring(0, dlFileIdx);                            
            }
        }
    }


    /**
     * From array of dimension, for example [2, 3, 4] creates tuples for 
     * accessing all elements, for example: [0][0][0], [0][0][1], ..., [1][2][3]
     * 
     * @param dimensions
     * @return
     */
    private String[] getIndexTuples(ArrayList<Integer> dimensions) {
        
        int numTuples = 1;
        for (int dim : dimensions) {
            numTuples *= dim; 
        }
        
        boolean isMaxNodes = false;
        if (numTuples > MAX_ARRAY_LEN) {
            numTuples = MAX_ARRAY_LEN;
            isMaxNodes = true; 
        }

        // the last item will remain null to let the caller know that max nodes
        // are achieved
        String[] tuples = new String[numTuples + (isMaxNodes ? 1 : 0)];
        int []counters = new int[dimensions.size()];
        
        for (int tupleIdx = 0; tupleIdx < numTuples; tupleIdx++) {
            tuples[tupleIdx] = countersToStr(counters);
            incIndexCounters(dimensions, counters);
        }
        
        return tuples;
    }


    private void incIndexCounters(ArrayList<Integer> dimensions, int[] counters) {
        int digitIdx = counters.length - 1;
        counters[digitIdx]++;
        while (counters[digitIdx] >= dimensions.get(digitIdx).intValue()) {
            counters[digitIdx] = 0;
            digitIdx--;
            if (digitIdx < 0) {
                break; // overflow
            }
            counters[digitIdx]++;
        }
    }


    private String countersToStr(int[] counters) {
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < counters.length; idx++) {
            sb.append('[').append(counters[idx]).append(']');
        }
        return sb.toString();
    }

    
    private void walkVNodes(VNode node, List<String> vars, boolean isAppendPartition, 
                            String replacementTarget, String replacementString) {

        if (node.m_children == null  &&  m_treeViewer.getChecked(node)) {
            if (!node.m_qName.isEmpty()) {  // the last item of array longer than
                // MAX_ARRAY_LEN have name MORE_ELEMENTS_EXIST_STR
                // and empty qName - do not add them to init section.
                renamePtrNode(node, replacementTarget, replacementString, isAppendPartition);
                vars.add(node.m_qName);
            }
            return;
        }
        
        if (node.m_children != null) {
            for (VNode child : node.m_children) {
                walkVNodes(child, vars, isAppendPartition, replacementTarget, replacementString);
            }
        }
    }


    public List<String> getData() {

        List<String> initVarsStr = new ArrayList<>();
        boolean isAppendPartition = m_isAppendPartitionCb.getSelection();
        
        if (m_initVars != null) {
            for (VNode initVar : m_initVars) {
                
                walkVNodes(initVar, initVarsStr, isAppendPartition,
                           // top level nodes in m_initVars hold renaming information
                           // for local vars trick '*(<type> *)0'. 
                           initVar.m_replacementTarget, initVar.m_replacementString);
            }
        }
        
        m_isAppendPartition = m_isAppendPartitionCb.getSelection();
        
        return initVarsStr;
    }


    public void refreshHierarchy(String varName) {
        ArrayList<VNode> expandedNodes = new ArrayList<>();
        List<String> varNames = new ArrayList<>();
        varNames.add(varName);
        getTypeHierarchy(varNames, expandedNodes);
    }


    public void refreshHierarchy(List<String> varNames) {
        ArrayList<VNode> expandedNodes = new ArrayList<>();
        getTypeHierarchy(varNames, expandedNodes);
    }
}


class VNode {
    String m_name;  // member name shown in variable list in a dialog, for example 'm_cnt'
    String m_type;
    String m_qName; // full name used for var init, for example: 'myStruct.m_cnt'
    String m_replacementString;
    String m_replacementTarget;
    
    VNode m_parent;
    List<VNode> m_children;
    
    
    public VNode(String name,
                 String type,
                 String qualifiedName,
                 VNode parent) {
        m_name = name;
        m_type = type;
        m_qName = qualifiedName;
        m_parent = parent;
        
    }


    VNode createClone() {
        VNode clone = new VNode(m_name, m_type, m_qName, m_parent);
        clone.m_replacementString = m_replacementString;
        clone.m_replacementTarget = m_replacementTarget;
        
        if (m_children != null) {
            clone.m_children = new ArrayList<>();
            for (VNode child : m_children) {
                clone.m_children.add(child.createClone());
            }
        }
        
        return clone;
    }

    
    VNode addChild(String childName, String childType, String childQualifiedName) {
        if (m_children == null) {
            m_children = new ArrayList<>();
        }
        VNode childNode = new VNode(childName, childType, childQualifiedName, this);
        m_children.add(childNode);
        return childNode;
    }
    

//    VNode addClonedChild() {
//        VNode child = addChild(m_name, m_type, m_qName);
//
//        child.m_replacementString = m_replacementString;
//        child.m_replacementTarget = m_replacementTarget;
//        
//        if (m_children != null) {
//            child.m_children = new ArrayList<>();
//            for (VNode child : m_children) {
//                clone.m_children.add(child.createClone());
//            }
//        }
//        
//        return child;
//    }
    
    
    String getUIString() {
        
        StringBuilder sb = new StringBuilder(m_name);
        
        if (!m_type.isEmpty()) {
            sb.append(": ").append(m_type);
        }
        
        return sb.toString();
    }
}


class MembersContentProvider implements ITreeContentProvider {

    private List<VNode> m_input;


    @SuppressWarnings("unchecked")
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        
        m_input = (List<VNode>)newInput;
    }

    
    @Override
    public Object[] getElements(Object inputElement) {
        return m_input.toArray(new VNode[0]);
    }

    
    @Override
    public Object[] getChildren(Object parentElement) {
        VNode vnode = (VNode)parentElement;
        if (vnode.m_children == null) {
            return null;
        }
        return vnode.m_children.toArray(new VNode[0]);
    }

    
    @Override
    public Object getParent(Object element) {
        VNode vnode = (VNode)element;
        return vnode.m_parent;
    }

    
    @Override
    public boolean hasChildren(Object element) {
        VNode vnode = (VNode)element;
        return vnode.m_children != null  &&  !vnode.m_children.isEmpty();
    }

    
    @Override
    public void dispose() {
    }
}


class MembersLabelProvider implements ILabelProvider {

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
        return null;
    }

    @Override
    public String getText(Object element) {
        VNode vnode = (VNode)element;
        return vnode.getUIString();
    }
    @Override
    public void dispose() {
    }
}