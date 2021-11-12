package si.isystem.itest.common;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.ContentOutline;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.editors.ContentProposalConfig;
import net.miginfocom.swt.MigLayout;
import si.isystem.commons.connect.ConnectionPool;
import si.isystem.commons.connect.JConnection;
import si.isystem.commons.globals.FunctionGlobalsProvider;
import si.isystem.commons.utils.ISysUIUtils;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CScriptConfig;
import si.isystem.connect.CStringStream;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestBase.CommentType;
import si.isystem.connect.CTestBaseList;
import si.isystem.connect.CTestBench;
import si.isystem.connect.CTestEnvironmentConfig;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestSpecification;
import si.isystem.connect.CTestTreeNode;
import si.isystem.connect.ETristate;
import si.isystem.connect.EmitterFactory;
import si.isystem.connect.IEmitter;
import si.isystem.connect.StrVector;
import si.isystem.connect.data.JFunction;
import si.isystem.exceptions.SEFormatter;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.handlers.FileSaveCmdHandler;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.TestSpecificationModel;
import si.isystem.itest.preferences.UIPrefsPage;
import si.isystem.itest.run.Script;
import si.isystem.itest.ui.spec.ISectionEditor;
import si.isystem.itest.ui.spec.TestTreeOutline;
import si.isystem.itest.ui.spec.data.EditorSectionNode;
import si.isystem.itest.ui.spec.data.TableRowComment;
import si.isystem.itest.ui.spec.data.TreeNode;
import si.isystem.ui.utils.AsystTextContentAdapter;
import si.isystem.ui.utils.KGUIBuilder;


public class UiUtils {

    /**
     *  This utility class has only static methods and should not be 
     * instantiated. 
     */
    private UiUtils() {}


    public static final String TEST_ID_ALLOWED_CHARS = "Spaces and some symbols are not allowed in test ID. Allowed symbols: ./-_\n";
    public static final String MARKDOWN_HELP = "For better readability three markdown tags can be used:\n"
      + "  **<bold text>** - show text **in bold**, may be used inside wo**r**d, to emphasize part of word.\n"
      + "  __<bold text>__ - show text __in bold__, but applied only on word bou__n__dary ('n' is not bold, '__' are preserved.\n"
      + "  *<italic text>* - show text *in italic*, may be used inside wo*r*d, to emphasize part of word.\n"
      + "  _<italic text>_ - show text _in italic_, but applied only on word bou_n_dary ('n' is not italic, '_' are preserved.\n"
      + "  `<code text>`   - show text in monospace font. Other markdown tags are ignored inside these tags.";
;
    
    public static String getExtension(String fileName) {
        // see also JUnit test of this method
        File file = new File(fileName);
        String fName = file.getName();  // remove directory from fileName
        
        if (fName.isEmpty()) {  // empty file names can not get extension
            return "";
        }

        if (fName.contains(".")) {
            int fromIndex = 0;
            // search for last '.' in complete fileName. Since fName contains '.',
            // fileName contains it too.
            while (true) {
                int idx = fName.indexOf('.', fromIndex);
                if (idx == -1) {
                    break;
                }
                fromIndex = idx + 1;
            }
            
            return fName.substring(fromIndex);
        } else {
            return "";
        }
    }
    
    /**
     * Adds extension to the given file name, if it does not already have it.      * 
     *  
     * @param fileName file name to get extension. If empty, extension is not added.
     * @param extension extension to be appended to file name, without dot, for
     *                  example 'trd'.
     * @param exactMatch if true, file must have the exact extension. If fileName
     *                   ends with some other extension, the given extension is still
     *                   appended to it. If <code>exactMatch</code> is false, extension
     *                   is added only if fileName has no extension.
     * @param isCaseSensitive defines if exiting extension is compared with the 
     *                   given one as case sensitive or not. Used only 
     *                   if <code>exactMatch</code> is <code>true</code>.  
     * @return file name with extension
     */
    public static String addExtension(String fileName, 
                                      String extension, 
                                      boolean exactMatch, 
                                      boolean isCaseSensitive) {
        String unquotedFName = fileName.trim();
        boolean isQuoted = false;
        if (unquotedFName.length() > 1  &&
            unquotedFName.charAt(0) == '"'  &&  
            unquotedFName.charAt(fileName.length() - 1) == '"') {
            
            isQuoted = true;
            unquotedFName = fileName.substring(1, fileName.length() - 1);
        }

        if (unquotedFName.isEmpty()) {  // empty file names can not get extension
            return fileName;
        }

        // see also JUnit test of this method
        String dotExtension = '.' + extension;
        File file = new File(unquotedFName);
        String fName = file.getName();  // remove directory from fileName
        
        if (fName.isEmpty()) {  // empty file names can not get extension
            return fileName;
        }

        if (exactMatch) {
            if (isCaseSensitive) {
                if (!fName.endsWith(dotExtension)) {
                    unquotedFName += dotExtension;
                }
            } else {
                if (!fName.toUpperCase().endsWith(dotExtension.toUpperCase())) {
                    unquotedFName += dotExtension;
                }
            }
        } else {
            if (!fName.contains(".")) {
                unquotedFName += dotExtension;
            }
        }
        
        if (isQuoted) {
            unquotedFName = '"' + unquotedFName + '"';
        }
        return unquotedFName;  // OK, fileName already has extension
    }
    
    
    /**
     * Replaces any existing extension with the give one. If fileName
     * has no extension, the given extension is appended to it.
     * 
     * @param fileName file name to get extension. If empty, extension is not added.
     * @param extension extension to be appended to file name, without dot, for
     *                  example 'trd'.
     * @return
     */
    public static String replaceExtension(String fileName, 
                                          String extension) {
        // see also JUnit test of this method
        String dotExtension = '.' + extension;
        File file = new File(fileName);
        String fName = file.getName();  // remove directory from fileName
        
        if (fName.isEmpty()) {  // empty file names can not get extension
            return fileName;
        }

        if (fName.contains(".")) {
            int fromIndex = 0;
            // search for last '.' in complete fileName. Since fName contains '.',
            // fileNAme contains it too.
            while (true) {
                int idx = fileName.indexOf('.', fromIndex);
                if (idx == -1) {
                    break;
                }
                fromIndex = idx + 1;
            }
            
            return fileName.substring(0, fromIndex - 1) + dotExtension;
        } else {
            return fileName + dotExtension;
        }
    }
    
    
    /**
     * This method replaces exiting extension, if exists, with the new one. If
     * the existing extension does not exist, nothing happens.
     * 
     * @param fileName
     * @param oldExtension
     * @param newExtension
     * @return
     */
    public static String replaceExtension(String fileName, 
                                      String oldExtension, 
                                      String newExtension, 
                                      boolean isCaseSensitive) {
        String dotOldExtension = '.' + oldExtension;
        String dotNewExtension = '.' + newExtension;
        
        File file = new File(fileName);
        String fName = file.getName();  // remove directory from fileName
        
        if (fName.isEmpty()) {  // empty file names can not get extension
            return fileName;
        }

        if (isCaseSensitive) {
            if (fName.endsWith(dotOldExtension)) {
                fileName = fileName.substring(0, fileName.length() - dotOldExtension.length());
                return fileName + dotNewExtension;
            }
        } else {
            if (fName.toUpperCase().endsWith(dotOldExtension.toUpperCase())) {
                fileName = fileName.substring(0, fileName.length() - dotOldExtension.length());
                return fileName + dotNewExtension;
            }
        }
        
        return fileName;  // OK, fileName already has extension
    }
    

    /** 
     * If file name has an extension, it is replaced. If it does not have 
     * an extension, it is added.
     * 
     * @param fileName
     * @param newExtension extension without dot, for example 'iyaml'.
     * @return
     */
    public static String replaceOrAddExtension(String fileName, 
                                               String newExtension) {
        
        int extensionIdx = fileName.lastIndexOf(".");
        if (extensionIdx <= 0) {
            // no extension or dot is the first char in name, for example .emacs
            return fileName + '.' + newExtension;
        } else {
            return fileName.substring(0, extensionIdx) + '.' + newExtension;
        }
    }


    /**
     * Replaces all chars, which are not allowed in identifier names with 
     * underscores.
     * 
     * @param funcName
     * @return
     */
    public static String replaceNonAlphanumChars(String identifierName) {
        
        return identifierName.replaceAll("\\W", "_");
    }
    
    /** 
     * The given postFix string is inserted to the file name before extension.
     * Example: addFileNamePosfix("name.txt", "-one") 
     * Returns: "name-one.txt"
     * Example: addFileNamePosfix("name", "-one") 
     * Returns: "name-one"
     * Example: addFileNamePosfix(".name", "-one") 
     * Returns: ".name-one"
     * 
     */
    public static String addFileNamePostfix(String fileName, 
                                            String postFix) {
        
        int extensionIdx = fileName.lastIndexOf(".");
        if (extensionIdx <= 0) {
            // no extension or dot is the first char in name, for example .emacs
            return fileName + postFix;
        } else {
            return fileName.substring(0, extensionIdx) + postFix + fileName.substring(extensionIdx);
        }
    }


    /**
     * Returns FocusListener, which listens for focus lost on Text component,
     * and adds extension to file name in this component.
     * 
     * @param extension
     * @return
     */
    public static FocusListener createExtensionFocusListener(final String extension) {
        return new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                Text fNameTxt = (Text)e.getSource();
                String fName = fNameTxt.getText().trim();
                
                fNameTxt.setText(UiUtils.addExtension(fName, extension, true, false));
            }
            @Override
            public void focusGained(FocusEvent e) {
                // nothing to do 
            }
        };        
    }
    
    
    /**
     * Shows file selection dialog with iyaml extension.
     * 
     * @param shell
     * @param path directory to be displayed in file dialog. If null,
     *        system default dir will be shown.
     * @param secondaryPath if 'path' == null or empty, the secondary path is used, otherwise
     *                      it is ignored. May also be null or empty. 
     * @param isFile if true, the last item in 'path' is file name and should be trimmed.
     * @return selected file name or null if canceled
     */
    public static String showOpenIYamlFileDialog(Shell shell, 
                                                 String path, 
                                                 String secondaryPath, 
                                                 boolean isFile) {
        
        FileDialog fd = new FileDialog(shell, SWT.OPEN);
        fd.setText("Open");
        
        if (path == null  ||  path.isEmpty()) {
            path = secondaryPath;
        }
        
        if (path != null  &&  !path.isEmpty()) {
            if (isFile) {
                File p = new File(path);
                path = p.getParent();
            }
            
            fd.setFilterPath(path);
        }

        String[] filterExt = {"*.iyaml", "*.c;*.cpp;*.h;*.hpp", "*.*"};
        fd.setFilterExtensions(filterExt);
        String fileName = fd.open();

        return fileName;
    }
    
    
    /**
     * Returns list of files in the given directory. If extension is specified,
     * only files with this extension are returned.
     * 
     * @param dirName directory to scan
     * 
     * @param extension if not null or empty string, it is used as a filter. If 
     * null or empty string, all files are returned. Should not contain dot.
     * 
     * @param isCaseSensitive if true, case of extension must also match
     * 
     * @return list of files
     */
    public static File[] listdir(String dirName, final String extension, boolean isCaseSensitive) {
        File dir = new File(dirName);
        
        FilenameFilter filter = null;
        
        if (extension != null) {
            if (isCaseSensitive) {
                filter = new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String fileName) {
                        return fileName.endsWith('.' + extension);
                    }
                };
            } else {
                filter = new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String fileName) {
                        return fileName.toUpperCase().endsWith('.' + extension.toUpperCase());
                    }
                };
            }
        }
        
        File[] files = dir.listFiles(filter);
        return files;
    }

    
    public static boolean isTestTreeActive() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        IWorkbenchPage page = window.getActivePage();

        IWorkbenchPart part = page.getActivePart();

        return part instanceof ContentOutline; // TODO find at runtime which class is Outline view
        /*

        IStructuredSelection structSelection = getStructuredSelection();
        if (structSelection == null) {
            return false;
        }

        * This test for active part fails, when there are no test specs selected,
        * for example when the model is empty (after File | New)

        @SuppressWarnings("rawtypes")
        Iterator iter = structSelection.iterator();
         
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof CTestSpecification) {
                return true;
            } 
        }
        return false;
        */
    }

    
    public static boolean isSectionTreeActive() {
        IStructuredSelection structSelection = getStructuredSelection();
        if (structSelection == null) {
            return false;
        }
            
        @SuppressWarnings("rawtypes")
        Iterator iter = structSelection.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof TreeNode) {
                return true;
            } 
        }
        
        return false;
    }
    
    
    public static Text getTextSelection() {
        
        final Control focusControl = Display.getCurrent().getFocusControl();
        
        if (focusControl instanceof Text) {
            return (Text)focusControl;
        }
        return null;
    }

    
    public static Combo getComboBoxSelection() {
        
        final Control focusControl = Display.getCurrent().getFocusControl();
        
        if (focusControl instanceof Combo) {
            return (Combo)focusControl;
        }
        return null;
    }
    
    
    public static StyledText getStyleTextSelection() {
        
        final Control focusControl = Display.getCurrent().getFocusControl();
        
        if (focusControl instanceof StyledText) {
            return (StyledText)focusControl;
        }
        return null;
    }
    
    
    /** Returns KTable if it has focus, or null if other component has focus. */
    public static KTable getKTableInFocus() {
        
        final Control focusControl = Display.getCurrent().getFocusControl();
        
        if (focusControl instanceof KTable) {
            return (KTable)focusControl;
        }
        return null;
    }
    
    
    public static IStructuredSelection getStructuredSelection() {
        
        final Control focusControl = Display.getCurrent().getFocusControl();
        if (focusControl instanceof Tree) {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            
            IWorkbenchPage page = window.getActivePage();

            IWorkbenchPart part = page.getActivePart();
            IWorkbenchPartSite site = part.getSite();
            ISelectionProvider selectionProvider = site.getSelectionProvider();
            if (selectionProvider == null) {
                return null;
            }
            ISelection selection = selectionProvider.getSelection();

            if (!(selection instanceof IStructuredSelection)) {
                throw new SIllegalStateException("Invalid selection!\nSelect test " +
                "specification or one of test sections to copy contents to clipboard.")
                .add("selectionType", selection.getClass().getSimpleName());
            }

            IStructuredSelection structSelection = (IStructuredSelection) selection;
            return structSelection;
        }
        
        return null;
    }

    
    private enum NodeType {SPECIFICATION, SECTION}; 

    public static boolean isStructuredSelection() {
        return Display.getCurrent().getFocusControl() instanceof Tree;
    }

    
    /**
     * Preferred method for obtaining test cases selected in Outline view. 
     * 
     * @return list of test specifications selected in the test spec. tree view.
     */
    public static List<CTestSpecification> getSelectedTestTreeSpecifications() {
        List<CTestSpecification> testSpecList = new ArrayList<CTestSpecification>();

        TestTreeOutline outline = TestCaseEditorPart.getOutline();

        if (outline != null) {
            ISelection selection = outline.getSelection();

            if (selection != null  &&  selection instanceof IStructuredSelection) {
                IStructuredSelection structSelection = (IStructuredSelection) selection;

                @SuppressWarnings("rawtypes")
                Iterator iter = structSelection.iterator();

                while (iter.hasNext()) {
                    CTestTreeNode selectedNode = (CTestTreeNode)iter.next();
                    if (selectedNode.isGroup()) {
                        CTestGroup grp = CTestGroup.cast(selectedNode);
                        if (grp.isTestSpecOwner()) {
                            testSpecList.add(grp.getOwnedTestSpec());
                        }
                    } else {
                        CTestSpecification selectedTestSpec = CTestSpecification.cast(selectedNode); 
                        // test spec. in the 'Test tree' view is selected
                        testSpecList.add(selectedTestSpec);
                    }
                }
            }
        }

        return testSpecList;
    }
    
    
    /**
     * Preferred method for obtaining nodes (groups and test cases) selected in 
     * Outline view. 
     * 
     * @return list of test specifications selected in the test spec. tree view.
     */
    public static List<CTestTreeNode> getSelectedTestTreeNodes() {
        List<CTestTreeNode> testSpecList = new ArrayList<CTestTreeNode>();

        TestTreeOutline outline = TestCaseEditorPart.getOutline();

        if (outline != null) {
            ISelection selection = outline.getSelection();

            if (selection != null  &&  selection instanceof IStructuredSelection) {
                IStructuredSelection structSelection = (IStructuredSelection) selection;

                @SuppressWarnings("rawtypes")
                Iterator iter = structSelection.iterator();

                while (iter.hasNext()) {
                    CTestTreeNode selectedNode = (CTestTreeNode)iter.next();
                    testSpecList.add(selectedNode);
                }
            }
        }

        return testSpecList;
    }
    
    
    /**
     * This method returns all test specifications or test sections selected 
     * in UI, as children of the returned test specification. If none test 
     * specifications are selected, null is returned. Test specifications
     * returned in containerTestSpec, are NOT reparented - they internally keep
     * references to the original parent, not containerTestSpec.
     */  
    public static CTestSpecification getSelectedTestSpecifications() {
        IStructuredSelection structSelection = getStructuredSelection();
        return getSelectedTestSpecifications(structSelection);
    }
    
    
    /**
     * Returns only selected test specifications, no CTestGroups
     * @param structSelection
     * @return
     */
    public static CTestSpecification getSelectedTestSpecifications(IStructuredSelection structSelection) {
        if (structSelection == null) {
            return null;
        }
            
        @SuppressWarnings("rawtypes")
        Iterator iter = structSelection.iterator();
        CTestSpecification containerTestSpec = new CTestSpecification();
        CTestSpecification selectedSectionTestSpec = null;
        
        NodeType nodeType = null;
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof CTestTreeNode) {
                // test spec. in the 'Test tree' view is selected
                if (nodeType == null) {
                    nodeType = NodeType.SPECIFICATION;
                } else if (nodeType != NodeType.SPECIFICATION) {
                    throw new SIllegalStateException("Select only one type of nodes - either test specifications or sections!");
                }
                
                CTestTreeNode testNode = (CTestTreeNode)item;
                if (!testNode.isGroup()) {
                    CTestSpecification testSpec = CTestSpecification.cast(testNode);
                    containerTestSpec.getChildren(false).add(-1, testSpec);
                }
                
            } else if (item instanceof TreeNode) {
                // test section in the 'Editor' view is selected
                if (nodeType == null) {
                    nodeType = NodeType.SECTION;
                    selectedSectionTestSpec = new CTestSpecification();
                } else if (nodeType != NodeType.SECTION) {
                    throw new SIllegalStateException("Select only one type of nodes - either test specifications or sections!");
                }
                
                @SuppressWarnings("unchecked")
                TreeNode<EditorSectionNode> node = (TreeNode<EditorSectionNode>)item;
                node.getData().getSectionEditor().copySection(selectedSectionTestSpec);
            }
        }
        
        if (selectedSectionTestSpec != null) {
            containerTestSpec.getChildren(false).add(-1, selectedSectionTestSpec);
        }
        
        return containerTestSpec;
    }


//    public static CTestBench getSelectedOutineNodes(IStructuredSelection structSelection) {
//        return getSelectedOutineNodes(structSelection, true);
//    }
    
    /**
     * 
     * @param structSelection
     * @param isCreateCopyForSection if true, and active selection is in section editor,
     *        not in outline view, then the returned object contains COPY of selected
     *        node in outline view, and only section selected in editor is copied to 
     *        the new node.
     *        If false, reference to the node selected in Outline view is returned, even if
     *        the outline view does not have focus, but editor displays its contents. 
     * @return
     */
    public static CTestBench getSelectedOutlineNodes(IStructuredSelection structSelection,
                                                    boolean isCreateCopyForSection) {
        
        if (structSelection == null) {
            return null;
        }
            
        @SuppressWarnings("rawtypes")
        Iterator iter = structSelection.iterator();
        CTestBench containerTB = new CTestBench();
        
        NodeType nodeType = null;
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof CTestTreeNode) {
                // test spec. in the 'Test tree' view is selected
                if (nodeType == null) {
                    nodeType = NodeType.SPECIFICATION;
                } else if (nodeType != NodeType.SPECIFICATION) {
                    throw new SIllegalStateException("Select only one type of nodes - either test specifications or sections!");
                }
                
                CTestTreeNode testNode = (CTestTreeNode)item;
                if (testNode.isGroup()) {
                    containerTB.getGroup(false).getChildren(false).add(-1, testNode);
                } else {
                    containerTB.getTestSpecification(false).getChildren(false).add(-1, testNode);
                }
                
            } else if (item instanceof TreeNode) {
                // test section in the 'Editor' view is selected
                if (nodeType == null) {
                    nodeType = NodeType.SECTION;
                    @SuppressWarnings("unchecked")
                    TreeNode<EditorSectionNode> node = (TreeNode<EditorSectionNode>)item;
                    ISectionEditor sectionEditor = node.getData().getSectionEditor();
                    CTestTreeNode selectedNode;
                    
                    if (isCreateCopyForSection) {
                        selectedNode = sectionEditor.createTestTreeNode();
                        sectionEditor.copySection(selectedNode);
                    } else {
                        selectedNode = sectionEditor.getTestTreeNode();
                    }
                    
                    if (selectedNode.isGroup()) {
                        // do not set parent to containerTB - this test spec in 
                        // part of test tree and must remain so 
                        containerTB.getGroup(false).getChildren(false).add(-1, selectedNode);
                    } else {
                        containerTB.getTestSpecification(false).getChildren(false).add(-1, selectedNode);
                    }
                    
                } else if (nodeType != NodeType.SECTION) {
                    throw new SIllegalStateException("Select only one type of nodes - either test specifications or sections!");
                }
                
            }
        }
        
        return containerTB;
    }


    // add test case not members of groups
    private static void addDerived(CTestSpecification container, 
                                   CTestSpecification testSpec,
                                   boolean isAddDerived, 
                                   Set<Long> duplicateDetector) {
        
        Long hashCode = Long.valueOf(testSpec.hashCodeAsPtr());
        if (!duplicateDetector.contains(hashCode)) {
            container.addDerivedTestSpec(-1, testSpec);
            duplicateDetector.add(hashCode);
        }
        
        if (isAddDerived) {
            int numDerived = testSpec.getNoOfDerivedSpecs();
            for (int idx = 0; idx < numDerived; idx++) {
                addDerived(container, testSpec.getDerivedTestSpec(idx), isAddDerived, duplicateDetector);
            }
        }
    }
    
    
    private static void addChildTestSpecs(CTestSpecification container, 
                                          CTestGroup testGroup,
                                          boolean isAddDerived, 
                                          Set<Long> duplicateDetector) {

        // first add all immediate test spec owner groups
        if (testGroup.isTestSpecOwner()) {
            
            if (testGroup.isBelongsToFilterGroup()) {
                CTestSpecification testSpec = testGroup.getOwnedTestSpec();
                Long hashCode = Long.valueOf(testSpec.hashCodeAsPtr());
                if (!duplicateDetector.contains(hashCode)) {
                    container.addDerivedTestSpec(-1, testSpec);
                    duplicateDetector.add(hashCode);
                }
            }
            
            if (isAddDerived) {
                int numTests = (int) testGroup.getTestOwnerGroupsSize();
                for (int idx = 0; idx < numTests; idx++) {
                    addChildTestSpecs(container, 
                                      testGroup.getTestOwnerGroup(idx), 
                                      isAddDerived,
                                      duplicateDetector);
                }
            }
        } else {
            // add test cases in child groups before immediate test cases, 
            // because group appear above immediate test cases in group, and
            // users may prefer top-down execution order.
            if (isAddDerived) {
                CTestBaseList childGroups = testGroup.getChildren(true);
                int numGrps = (int) childGroups.size();
                for (int idx = 0; idx < numGrps; idx++) {
                    addChildTestSpecs(container, 
                                      CTestGroup.cast(childGroups.get(idx)), 
                                      isAddDerived,
                                      duplicateDetector);
                }                
            }
            
            int numTests = (int) testGroup.getTestOwnerGroupsSize();
            for (int idx = 0; idx < numTests; idx++) {
                addChildTestSpecs(container, 
                                  testGroup.getTestOwnerGroup(idx), 
                                  isAddDerived,
                                  duplicateDetector);
            }
        }
    }
    
    
    /**
     * This method copies all selected test cases to one container. Also test 
     * cases from selected groups are copied. All test cases to be executed 
     * are direct descendants of container.
     * 
     * @param isAddDerived if false, only selected test cases and immediate 
     * children of selected groups are added. If false, all derived test cases 
     * are also added to the returned container as immediate children.
     *  
     * @return
     */
    public static CTestSpecification getDirectTestsAndInSelectedGroups(boolean isAddDerived) {
        
        CTestSpecification containerTestSpec = new CTestSpecification();
        TestTreeOutline outlineView = TestCaseEditorPart.getOutline();
        IStructuredSelection structSelection = (IStructuredSelection)outlineView.getSelection();
        Set<Long> duplicateDetector = new TreeSet<>();

        if (structSelection == null) {
            return containerTestSpec;
        }
            
        @SuppressWarnings("rawtypes")
        Iterator iter = structSelection.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof CTestTreeNode) {
                
                CTestTreeNode testNode = (CTestTreeNode)item;
                if (testNode.isGroup()) {
                    CTestGroup group = CTestGroup.cast(testNode);
                    addChildTestSpecs(containerTestSpec, group, isAddDerived, duplicateDetector);
                } else {
                    CTestSpecification testSpec = CTestSpecification.cast(testNode);
                    addDerived(containerTestSpec, testSpec, isAddDerived, duplicateDetector);
                }
            }
        }

        return containerTestSpec;
    }
    
    
    public static CTestGroup getSelectedGroups() {
        
        CTestGroup containerGroup = new CTestGroup();
        TestTreeOutline outlineView = TestCaseEditorPart.getOutline();
        IStructuredSelection structSelection = (IStructuredSelection)outlineView.getSelection();

        if (structSelection == null) {
            return containerGroup;
        }
            
        @SuppressWarnings("rawtypes")
        Iterator iter = structSelection.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            
            if (item instanceof CTestTreeNode) {
                
                CTestTreeNode testNode = (CTestTreeNode)item;
                if (testNode.isGroup()) {
                    containerGroup.getChildren(false).add(-1, testNode);
                } 
            }
        }

        return containerGroup;
    }
    
    
    /**
     * This method converts the given test specification to string. If there is 
     * more than one test specification, the string contains them as derived 
     * test specs. 
     * 
     * @param containerTestSpec
     * @return
     */
    /**
     * This method converts the given test specification to string. It tries to 
     * mimic the user, when he is pasting test specification(s) from text editor,
     * so that single test spec. is not prepended by '-', and tag 'tests:' does
     * not need to be copied when pasting multiple test specs.
     * 
     * @param containerTestBench
     * @return
     */
    public static String testSpecToTextEditorString(CTestBench containerTestBench) {

        boolean isGroup = containerTestBench.getGroup(true).hasChildren();
        boolean isTestSpec = containerTestBench.getTestSpecification(true).hasChildren();
        if (!isGroup  &&  !isTestSpec) {
            return "{}";  // no groups or test cases
        }

        // if there are no groups, return test specs as list, to preserve compatibility 
        if (!isGroup  &&  isTestSpec) {
            CTestBaseList list = containerTestBench.getTestSpecification(true).getChildren(true);
            
            String strTestSpec = list.toString();
            if (list.size() == 1) {
                // replace leading '-' with ' ', because there is only one test spec, not a list of them
                strTestSpec = ' ' + strTestSpec.substring(1);
            }
            return strTestSpec;
        }
        
        String strTestSpec = testBaseToString(containerTestBench);
        
        // code till the end of this function modifies string depending on the
        // number of test specifications to copy - one test spec. does not need 
        // to be in the list, if there are more of them, they have to form YAML 
        // list.
        
        // skip 'tests:\n'
        // strTestSpec = strTestSpec.substring(strTestSpec.indexOf(':') + 2);
        
        // if there are no groups and only one test spec selected, do not copy it as derived 
        // test spec 
//        if (!isGroup  &&
//            containerTestBench.getTestSpecification(true).getChildren(true).size() == 1) {
//            // replace leading '-' with ' ', because there is only one test spec, not a list of them
//            strTestSpec = ' ' + strTestSpec.substring(1);
//        }
        return strTestSpec;
    }


    public static String testBaseToString(CTestBase testBase) {

        CStringStream strStream = new CStringStream();
        
        if (testBase != null) {
            IEmitter emitter = EmitterFactory.createYamlEmitter(strStream);
            emitter.startStream();
            emitter.startDocument(true);
            testBase.serialize(emitter);
            emitter.endDocument(true);
            emitter.endStream();
        }
        
        String strTestSpec = strStream.getString();
        return strTestSpec;
    }


    /* public static void setPositionParams(CTestSpecification testSpec,
                                         String paramsNew) {
        // params may be specified either as part of function spec or standalone
        // section in test specification. If none of parameters are set, they are
        // set in function specification
        if (!testSpec.getFunctionUnderTest(true).getName().isEmpty() && !testSpec.hasPositionParams()) {
            // func params are stored to function only if they are not present in test spec AND
            // function name is specified. If function name is NOT specified, function tag is NOT saved.
            
            testSpec.getFunctionUnderTest(false).setPositionParameters('[' + paramsNew + ']');
        } else {
            // clear possible existing params in function. This may happen, when
            // user clears function name to get it merged with base test spec, so
            // params must be moved from function spec to test spec
            if (!testSpec.getFunctionUnderTest(true).isEmpty()) {
                testSpec.getFunctionUnderTest(false).setPositionParameters("[]");
            }
            testSpec.setPositionParams('[' + paramsNew + ']');
        }
    } */

    
    /**
     * Returns true, if any of characters, which can change the text is pressed.
     * This also includes Ctrl-X for Cut and Ctrl-V for Paste operations.
     * @param e
     * @return
     */
    public static boolean isDataChar(KeyEvent e) {
        // System.err.println((int)e.character + " " + e.keyCode);
        return (int)e.character != 0  &&   // ignore all meta and modifier keys  
                (int)e.character != SWT.ESC  &&
                (int)e.character != SWT.TAB;
    }

    
    /** Returns true, if Ctrl-V or Ctrl-X was pressed. Should be called from 
     * keyReleased() method. */
    public static boolean isCutPaste(KeyEvent e) {
        return e.stateMask == SWT.CONTROL  &&  (e.keyCode == 'v'  ||  e.keyCode == 'x');
    }

    
    /**
     * If the model is modified, this method shows a dialog with three choices.
     * If the user selects 'Save' button, the model is saved by this method. 
     * 
     * @return 0 if the user selects Save, 1 if Discard is selected, 2 if Cancel is selceted.
     * @throws ExecutionException 
     */
    public static int askForModelSave() throws ExecutionException {
        int answer = 0; 
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model != null  && model.isModelDirty()) {

            MessageDialog dlg = new MessageDialog(Activator.getShell(),  
                                                  "Save resources", 
                                                  null,
                                                  "The data has been modified. Save changes?",
                                                  MessageDialog.QUESTION,
                                                  new String[] {"Save", "Discard", "Cancel"},
                                                  0);
            answer = dlg.open();
            
            if (answer == 0) {
                try {
                    new FileSaveCmdHandler().execute(null);
                } catch (Exception ex) {
                    // if saving failed or the used clicked the 'Cancel' button, do not exit!
                    answer = 2; // simulate the 'Cancel' btn
                }
            }
        }
        
        return answer;
    }

    
    private static TestSpecificationModel ms_reloadModel = null;
    
    public static boolean checkForReload() {

        TestCaseEditorPart activeEditor = TestCaseEditorPart.getActive();

        if (activeEditor == null) {
            return false;
        }
        
        TestSpecificationModel model = activeEditor.getModel();
        
        // System.err.println("-- checkForReload: " + activeEditor);
        
        if (model == ms_reloadModel) {
            
            // when testIDEA already has focus, and user selects editor, then this 
            // method is called twice from the same thread - first time from ...Editor.setFocus(),
            // and when user click Cancel button for the second time from 
            // windowActivated(IWorkbenchWindow window) (it seems recursively). 
            // Only then execution continues after the 
            // MessageDialog.openQuestion(shell, "testIDEA", ... 
            // in method isReloadRequired() below. To avoid double showing of
            // the reload dialog this guard was introduced.
            ms_reloadModel = null;
            
            // Could no longer reproduce (20.Apr.2015, Java 1.8, target platform 3.7.2), 
            // so I removed the guard.
            // Furthermore, it could happen, that file was not reloaded due to this
            // guard - not reliable to reproduce, just try to change iyaml file in 
            // Emacs, then switch to testIDEA. Even the problem above reappears,
            // solution has to be different.
            
            // return false;
        }
        
        ms_reloadModel = model;
        
        if (model != null  &&
            UiUtils.isReloadRequired(Activator.getShell(), model.hasFileChanged(), !model.isModelDirty())) {
            
            // System.err.println("-- isReloadRequired: true" );
            
            try {
                // when reloading, try to keep showing the same test specification
                // define by set of indices of derived test specifications
                TestTreeOutline outline = TestCaseEditorPart.getOutline();
                CTestTreeNode testSpec = outline.getTestSpecSelection();
                List<Integer> idxPath = null;
                if (testSpec != null) {
                    idxPath = model.getIndexPath(testSpec);
                }
                model.reload();
                if (idxPath != null) {
                    testSpec = model.getTestSpec(idxPath);
                    activeEditor.setFormInput(testSpec);
                    outline.setSelection(model, testSpec);
                } else {
                    activeEditor.setFormInput(null);
                }
                return true; // model was reloaded!
            } catch (Exception ex) {
                SExceptionDialog.open(Activator.getShell(), "Can't reload the model!", ex);
                // avoid future reloads, if something went wrong
                model.updateFileAttributes();
            } finally {
                ms_reloadModel = null;
            }
        }
        
        return false;
    }
    
    
    public static boolean isReloadRequired(Shell shell,
                                           boolean hasFileChanged, 
                                           boolean isModelSaved) {
        if (!hasFileChanged) {
            return false;
        }
        
        if (isModelSaved) {
            return true; // if the model has been saved, automatically reload
        }
        
        // System.out.println("before oq" + Thread.currentThread().getName());
        boolean ans = 
            MessageDialog.openQuestion(shell, "testIDEA", 
                                       "The file with model has been modified outside of testIDEA.\n" +
                                       "Do you want to reload it?");
        // System.out.println("after oq");
        
        if (ans) {
            return true;
        }
        
        // if file attributes are updated, it will not ask again, if the file 
        // should be reloaded
        TestSpecificationModel.getActiveModel().updateFileAttributes();
        
        return false;
    }


    public static void setFuncParams(Text label, 
                                     FunctionGlobalsProvider globalsProvider, 
                                     String functionName) {
        if (globalsProvider != null) {
            try {
                JFunction jFunc = globalsProvider.getCachedFunction(functionName);
                if (jFunc != null) {
                    String types = " " + jFunc.getPrototype();
                    // escape '&', because labels on Windows use it as mnemonic indicator
                    // so next char after '&' is underlined.
                    // types = types.replace("&", "&&"); // no longer true, not sure why
                    label.setText(types);
                    return;
                }
            } catch (Exception ex) {
                String msg = SEFormatter.getInfo(ex);
                label.setText(msg);
                return;
            }
        }
        label.setText("");
    }


    /**
     * Returns false if the file exists and the user decided NOT to overwrite it.
     * In all other cases true is returned.
     * @param shell
     * @param outFileName
     * @return
     */
    public static boolean checkForFileOverwrite(Shell shell, String outFileName) {
        File outFile = new File(outFileName);
        
        if (outFile.exists()) {
            boolean isOverwrite = MessageDialog.openConfirm(shell, 
                                                    "Confirm file overwrite", 
                                                    "File '" + outFileName + "' already exists.\n" +
                                                    "Do you want to replace it?");
            return isOverwrite;
        }
        return true;
    }
    
    
    public static List<TableRowComment> createCommentsList(StrVector keys,
                                                           int sectionId, 
                                                           CTestBase testBase) {
        
        List<TableRowComment> comments = new ArrayList<TableRowComment>();
        for (int i = 0; i < keys.size(); i++) {
            comments.add(createMapRowComment(keys.get(i), sectionId, testBase));
        }
        
        return comments;
    }
    
    
    public static TableRowComment createMapRowComment(String key, 
                                                      int sectionId, 
                                                      CTestBase testBase) {
        
        String nlComment = testBase.getComment(sectionId, key, 
                                               CommentType.NEW_LINE_COMMENT);
        String eolComment = testBase.getComment(sectionId, key, 
                                                CommentType.END_OF_LINE_COMMENT);

        TableRowComment rowComment = new TableRowComment(nlComment, eolComment, "", "");
        rowComment.setMappingKey(key);
        return rowComment;
    }
    
    /**
     * Converts list of strings to single string, where elements are separated by 
     * newline char. The last item does not end with newline char.
     * 
     * @param list
     * @param indent string to be prepended to each list item when appended to 
     * the result string.
     * @return
     */
    public static String list2Str(List<String> list, String indent) {
        
        StringBuilder sb = new StringBuilder();
        
        if (list != null) {
            for (String s : list) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(indent).append(s);
            }
        }
        
        return sb.toString();
    }

    
    public static void putYamlToClipboard(String strTestSpec) {
        Clipboard cb = new Clipboard(Display.getDefault());
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
    }
    
    
    public static String getYamlFromClipboard() {
        // read data in YAML format from clipboard
        Clipboard cb = new Clipboard(Display.getDefault());
        TextTransfer transfer = TextTransfer.getInstance();

        String yamlSpec = (String)cb.getContents(transfer);
        cb.dispose();
        return yamlSpec;
    }


    /**
     * Returns workspace path of winIDEA, which this iTB is connected to.  
     * Deprecated - call ISysFileUtils.getWinIDEAWorkspaceDir() instead.
    public static String getActivewinIDEAWorkspaceDir() {
        CIDEController ideCtrl = ControllerPool.instance().getCIDEController(null);
        if (ideCtrl != null) {
            return ideCtrl.getPath(CIDEController.EPathType.WORKSPACE_DIR);
        }
        return "Can not get winIDEA workspace folder. Connection to winIDEA is not established!"; 
    }
     */
    
    
    public static String getActivewinIDEAWorkspaceFile() {
        CIDEController ideCtrl = Activator.CP.getConnection(ConnectionPool.DEFAULT_CONNECTION).getCIDEController(null);
        if (ideCtrl != null) {
            return ideCtrl.getPath(CIDEController.EPathType.WORKSPACE_FILE_NAME);
        }
        return "Can not get winIDEA workspace file. Connection to winIDEA is not established!"; 
    }

    
    public static String tristate2String(ETristate value) {
        switch (value) {
        case E_DEFAULT:
            return "";
        case E_FALSE:
            return "false";
        case E_TRUE:
            return "true";
        }
        return "";
    }
    
    
    /**
     * Adds YAML comment char '#' in from of each line. Comment char is preceeded
     * by 'indent' spaces. For example, for 'indent = 4', the prepended string is:
     * '    # '.
     * 
     * @param comment multiline string with '\n' as line separator
     * @param indent number of spaces to prepend to each line.
     * @return
     */
    public static String addCommentChar(String comment, int indent) {
        
        if (comment.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder indentSb = new StringBuilder();
        indentSb.ensureCapacity(indent);
        for (;indent > 0; indent--) {
            indentSb.append(' ');
        }
        
        String indentStr = indentSb.toString();
        
        String [] lines = comment.split("\n");
        StringBuilder sb = new StringBuilder();
        
        for (String line : lines) {
            sb.append(indentStr).append("# ").append(line.trim()).append('\n');
        }
        
        return sb.toString();
        
    }
    
    
    /**
     * Prepends each line in the given string with the given number os spaces.
     * @param str
     * @return
     */
    public static String indentMultilineString(String str, int numSpaces) {
        char sp[] = new char[numSpaces + 1];
        Arrays.fill(sp, ' ');
        sp[0] = '\n';
        String spaces = new String(sp);
        
        // indent the first line
        str = new String(sp, 1, numSpaces) + str;
        
        String result = str.replace("\n", spaces); 
        return result;
    }
    
    
    /**
     * Creates label with icon from file.
     * 
     * @param pluginId plug-in ID, for example "com.mydomain.project"
     * @param imageFilePath
     *            the relative path of the image file, relative to the root of
     *            the plug-in, for example "icons/cut_edit.gif"; the path must be legal   
     * @param layoutData Mig layout string
     * @return label with icon set
     */
    public Label iconLabel(KGUIBuilder builder, String pluginId, String imageFilePath, String layoutData) {
        
        ImageDescriptor imgDescriptor = AbstractUIPlugin.
            imageDescriptorFromPlugin(pluginId, imageFilePath);
        
        // doc for imageDescriptorFromPlugin() states, that null is returned if 
        // image can not be found
        if (imgDescriptor == null) {
            throw new SIllegalArgumentException("Can not create icon for plugin!").
                                               add("pluginId", pluginId).
                                               add("imageFile", imageFilePath);
        }
        
        return builder.iconLabel(imgDescriptor, imageFilePath, layoutData);
    }
    
    
    // not tested
    public IProject getSelectedProject() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null)
        {
            // window.getActivePage().getActivePart().getSite().getSelectionProvider().getSelection();
            IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
            Object firstElement = selection.getFirstElement();
            if (firstElement instanceof IAdaptable)
            {
                IProject project = (IProject)((IAdaptable)firstElement).getAdapter(IProject.class);
                return project;
            }
        }
        
        return null;
    }
    

    // tested
    public static IProject getSelectedProjectInProjectExplorer() {
        
        IResource resource = null;

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null)
        {
            IWorkbenchPage activePage = window.getActivePage();
            if (activePage != null) {
                // this works in PDE eclipse, but not in CDT Eclipse
                ISelection selection = 
                        activePage.getSelection(ProjectExplorer.VIEW_ID);
                
                if (selection == null) { // fallback in CDT Eclipse
                    selection = activePage.getSelection();
                }
                
                /* IViewPart[] views = activePage.getViews();
                for (IViewPart view : views) {
                    System.out.println("view = " + view.getTitle());
                } */                
                
                if (selection != null  &&  selection instanceof IStructuredSelection) {
                    IStructuredSelection structSel = (IStructuredSelection)selection;
                    Object item = structSel.getFirstElement();
                    if (item instanceof IResource) {
                        resource = (IResource) item;
                    } else if (item instanceof IAdaptable) {
                        IAdaptable adaptable = (IAdaptable) item;
                        resource = (IResource) adaptable.getAdapter(IResource.class);
                    }
                }

                if (resource != null) {
                    // System.out.println("res = " + resource.getClass().getSimpleName());
                    return resource.getProject();
                }
            }
        }
        return null;
    }

    
    public String getProjectPath(IProject project) {
        IPath path = project.getLocation();

        return path.toOSString();
    }
    

    /**
     * This method returns editor for the given file-name. First it searches in 
     * workspace (usual plug-in use-case), then in file system (the only RCP use-case).
     * 
     * @param fileName
     * @return
     * @throws URISyntaxException
     * @throws CoreException
     */
    public static IEditorPart findAndActivateEditorForFile(String fileName) 
                                                           throws URISyntaxException, 
                                                                  CoreException {
        
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        URI uri = new URI("file", "//" + fileName, "");
        IEditorPart editor = null;
        IEditorInput editorInput = null; 

        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        
        IFile[] files = wsRoot.findFilesForLocationURI(uri);

        if (files.length > 0) {

            IFile fileInput = files[0];
            if (fileInput == null) {
                return null;
            }

            editorInput = new FileEditorInput(fileInput);

            editor = page.findEditor(editorInput);

            if (editor != null) {
                page.bringToTop(editor);
                return editor;  // editor was found for file in workspace
                // this is usual case for plug-in testIDEA
            }
        } else {


            // No editor in workspace was found, try abs. file path - the only way
            // in RCP.

            IFileStore fileStore = EFS.getStore(uri);
            if (fileStore == null) {
                throw new SIllegalStateException("Can't find file in workspace!").
                add("file", fileName).
                add("numFiles", files.length);
            }
            
            editorInput = new FileStoreEditorInput(fileStore);
        }
        
        editor = page.findEditor(editorInput);
        
        if (editor != null) {
            page.bringToTop(editor);
        }
        
        return editor;
    }

    
    /**
     * Sets items used all over testIDEA.
     */
    public static void setContentProposalsConfig(ContentProposalConfig cfg) {

        char [] autoActivationCharacters = ISysUIUtils.ALPHA_CONTENT_PROPOSAL_KEYS;
        
        if (UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace()) {
            autoActivationCharacters = null;
        }
        
        cfg.setAutoActivationCharacters(autoActivationCharacters);
        cfg.setControlContentAdapter(new AsystTextContentAdapter());
        cfg.setKeyStroke(KeyStroke.getInstance(SWT.CTRL, SWT.SPACE));
    }


    public static boolean isHostVar(String key) {
        
        return key.length() > 3  &&  
               key.charAt(0) == '$'  &&  
               key.charAt(1) == '{'  &&
               key.charAt(key.length() - 1) == '}';
    }
    
    
//    public static StringBuilder scriptResult2StatusViewText(TestScriptResult scriptResult,
//                                                            String scriptFuncName) {
//        
//        StringBuilder sb = new StringBuilder();
//        
//        sb.append("Script function '" + scriptFuncName + "', " + scriptResult.getFuncType() + ":\n");
//        sb.append("    Return value:\n      ")
//        .append(StringUtils.join(scriptResult.getFuncRetVal(), "\n      "))
//        .append("\n    Stdout:\n      ")
//        .append(StringUtils.join(scriptResult.getStdout(), "\n      "));
//        
//        String callerInfo = scriptResult.getMetaData(); 
//        if (callerInfo != null  && !callerInfo.isEmpty()) {
//            sb.append("\n    Caller info:\n      ").append(callerInfo);
//        }
//        
//        sb.append("\n\n");
//
//        return sb;
//    }

    
    public static boolean isVarName(String value) {
        
        if (!value.isEmpty()) {
            char ch = value.charAt(0);
            return Character.isLetter(ch)  ||  ch == '_';
        }
        
        return false;
    }
    
    
    public static KGUIBuilder initDialogPanel(Composite composite,
                                              MigLayout layout,
                                              String dlgTitle,
                                              int widthHint) {
        
        composite.getShell().setText(dlgTitle);
        
        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
        // required to get resizable composite
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        // gridData.heightHint = 510;  // sets initial dialog size
        gridData.widthHint = widthHint;
        mainDlgPanel.setLayoutData(gridData);
        
        mainDlgPanel.setLayout(layout);
        
        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
        
        return builder;
    }
    
    
    public static CTestBench parseTestCasesAndGroups(String yamlSpec, boolean isPastedMultiple) {
        
        String trimmed = yamlSpec.trim(); 
        if (trimmed.startsWith("groups:")  ||  trimmed.startsWith("testCases:")) {
            CTestBench testBench = CTestBench.parse(yamlSpec);
            return testBench;
        }
        
        // if it was not CTestBench, it should be CTestCpecification
        CTestBench testBench = new CTestBench();
        CTestSpecification testSpec = CTestSpecification.parseTestSpec(yamlSpec);
        CTestSpecification rootTestSpec = testBench.getTestSpecification(false);
        
        if (isPastedMultiple) {
            rootTestSpec.getChildren(false).assign(testSpec.getChildren(true));
        } else {
            rootTestSpec.addChildAndSetParent(-1, testSpec);
        }
        
        return testBench;
    }
    
    
    public static Script initScript(TestSpecificationModel model) {
        
        if (model == null) {
            model = TestSpecificationModel.getActiveModel();
        }
        
        if (model == null) {
            throw new IllegalStateException("Please select testIDEA editor!");
        }

        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();
        CTestEnvironmentConfig envConfig = model.getCEnvironmentConfiguration();
        CScriptConfig scriptConfig = envConfig.getScriptConfig(true);
        return new Script(jCon, scriptConfig, model.getModelFileName());
    }

    
    public static Script initScript(CScriptConfig scriptConfig, String modelFileName) {
        
        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();
        return new Script(jCon, scriptConfig, modelFileName);
    }
}

