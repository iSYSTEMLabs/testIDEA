package si.isystem.itest.ui.spec;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import net.miginfocom.swt.MigLayout;
import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestSpecification;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIOException;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.SWTBotConstants;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.StatusModel;
import si.isystem.itest.model.StatusModelEvent;
import si.isystem.itest.model.StatusModelEvent.ETextFormat;
import si.isystem.itest.model.StatusTableLine;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.status.IStatusModelListener;
import si.isystem.itest.ui.spec.data.ITestStatusLine;
import si.isystem.itest.ui.spec.data.MarkdownParser;
import si.isystem.itest.ui.spec.data.TestStatusViewDropListener;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FDGUIBuilder;
import si.isystem.ui.utils.KGUIBuilder;

public class StatusView extends ViewPart implements IStatusModelListener {

    public static final String ID = "si.isystem.itest.ui.spec.errorsView";
    private StyledText m_detailPaneTxt;
    private TableViewer m_errorsTable;
            
    private Image m_okIcon;
    private Image m_infoIcon;
    private Image m_warningIcon;
    private Image m_errorIcon;
    private Image m_fatalIcon;
    private Label m_winIDEAConnectionStatusLbl;
    private Label m_stdProVersionIcon;
    private MarkdownParser m_mdParser;


    public StatusView() {
        ImageDescriptor descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
            		                                   IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/ok_obj.gif");
        m_okIcon = descriptor.createImage();
        
        descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
            		                                   IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/info_obj.gif");
            m_infoIcon = descriptor.createImage();
            
        descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
            		                                   IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/warning_obj.gif");
        m_warningIcon = descriptor.createImage();

        descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
            		                                   IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/error_obj.gif");
        m_errorIcon = descriptor.createImage();

        descriptor = 
            AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
            		                                   IconProvider.COMMONS_PLUGIN_ICONS_PATH + "/fatalerror_obj.gif");
        m_fatalIcon = descriptor.createImage();
        
        /* editor updates status view when it becomes active
         * and when test execution is finished.
         *  
        TestSpecificationModel.getActiveModel().addListener(new TestSpecModelListenerAdapter() {
            
            @Override
            public void updateTestResults(ModelChangedEvent event) {
                
                m_errorsTableModel = getContent();
                m_errorsTable.setInput(m_errorsTableModel);
                
                ISelection selection;
                if (m_errorsTableModel.size() > 0) {
                    selection = new StructuredSelection(m_errorsTableModel.get(0));
                } else {
                    selection = new StructuredSelection();
                }
                    
                m_errorsTable.setSelection(selection);
            }
        
        });
        */
    }

    
    public static StatusView getView() {
        
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null) {
            IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
            if (activeWindow != null) {
                IWorkbenchPage activePage = activeWindow.getActivePage();
                if (activePage != null) {
                    return (StatusView)activePage.findView(StatusView.ID);
                }
            }
        }

        return null;
    }
    

    
    @Override
    public void dispose() {
        
        super.dispose();
        
        StatusModel.instance().removeListener(this);
        
        if (m_okIcon != null) {
            m_okIcon.dispose();
        }
        if (m_infoIcon != null) {
            m_infoIcon.dispose();
        }
        if (m_warningIcon != null) {
            m_warningIcon.dispose();
        }
        if (m_errorIcon != null) {
            m_errorIcon.dispose();
        }
        if (m_fatalIcon != null) {
            m_fatalIcon.dispose();
        }
    }
    
    
    @Override
    public void createPartControl(Composite parent) {
        
        setTitleToolTip("This view shows error messages, which terminated test exectution.\n" +
        		"No test results are available in such case.");
        
        Composite mainPanel = new Composite(parent, SWT.NONE);
        FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(mainPanel);

        MigLayout mig = new MigLayout("fill", "[min!][min!][fill][min!]", "[fill][min!]");
        mainPanel.setLayout(mig);
        
        SashForm sash = new SashForm(mainPanel, SWT.HORIZONTAL);
        sash.setLayoutData("span 4, grow, wmin 0, hmin 0, wrap");
        sash.SASH_WIDTH = 3;

        Composite tablePanel = new Composite(sash, SWT.NONE);
        m_errorsTable = new TableViewer(tablePanel,
                                        SWT.FULL_SELECTION | // to select whole row 
                                        SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        
        tablePanel.setLayoutData("wmin 0, hmin 0");
        addDropListener(m_errorsTable);
        m_errorsTable.addSelectionChangedListener(new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                // sets error text in the right text control, according to 
                // selected error in the table
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                StatusTableLine statusLine = (StatusTableLine)selection.getFirstElement();
                setDetailPaneText(new StatusModelEvent(null, statusLine));
            }
        });

        m_errorsTable.addDoubleClickListener(new IDoubleClickListener() {
            
            @Override
            public void doubleClick(DoubleClickEvent event) {
                // sets error text in the right text control, according to 
                // selected error in the table
                try {
                    IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                    StatusTableLine statusLine = (StatusTableLine)selection.getFirstElement();

                    if (statusLine == null) {
                        return;
                    }

                    String fileName = statusLine.getFileName();
                    TestCaseEditorPart editor = null;
                    if (fileName != null  &&  !fileName.isEmpty()) {
                        try {
                            TestCaseEditorPart activeEditor = TestCaseEditorPart.getActive();
                            IEditorPart editorPart = UiUtils.findAndActivateEditorForFile(fileName);
                            if (editorPart instanceof TestCaseEditorPart) {
                                editor = (TestCaseEditorPart) editorPart;
                                if (activeEditor != editor) {
                                    editor.setFocus();
                                }
                            }
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        editor = TestCaseEditorPart.getActive();
                    }

                    if (editor != null) {

                        if (statusLine.getResult() != null) { 
                            // not all status lines link to specific CTestResult instance,
                            // the 'Test summary' status line is one such example

                            CTestSpecification mergedTestSpec = 
                                    statusLine.getResult().getTestSpecification();

                            if (mergedTestSpec != null) {
                                // result has reference to merged test spec. Parent of merged test spec.
                                // is it's original test specification.
                                CTestSpecification testSpec = mergedTestSpec.getParentTestSpecification();
                                if (testSpec != null) {
                                    // System.out.println("StatusView.dblClick = " + (mergedTestSpec != null ? mergedTestSpec.hashCodeAsPtr() : 0));
                                    // System.out.println("StatusView.dblClick = " + (testSpec != null ? testSpec.hashCodeAsPtr() : 0));
                                    editor.setSelection(testSpec);
                                }
                            }
                        } else {
                            CTestGroupResult groupResult = statusLine.getGroupResult();
                            if (groupResult != null) {
                                CTestGroup group = CTestGroup.cast(groupResult.getParent());
                                editor.setSelection(group);
                            } else if (statusLine.getTestTreeNode() != null) {
                                // status line contains verifier status
                                editor.setSelection(statusLine.getTestTreeNode());
                            }
                        }
                    } else {
                        SExceptionDialog.open(getSite().getShell(), 
                                              "Can't find editor for file!", 
                                              new SIOException("").add("fileName", fileName));
                    }
                } catch (Exception ex) {
                    SExceptionDialog.open(getSite().getShell(), "Can't locate test case in Outline view!", ex);
                }
            }
        });
        
        TableViewerColumn iconColumn = new TableViewerColumn(m_errorsTable, SWT.NONE);

        TableViewerColumn testIdColumn = new TableViewerColumn(m_errorsTable, SWT.NONE);
        testIdColumn.getColumn().setText("ID");

        TableViewerColumn futColumn = new TableViewerColumn(m_errorsTable, SWT.NONE);
        futColumn.getColumn().setText("Function/label");

        TableViewerColumn msgColumn = new TableViewerColumn(m_errorsTable, SWT.NONE);
        msgColumn.getColumn().setText("Message");

        // the following three lines are important for column visibility and resizing
        TableColumnLayout layout = new TableColumnLayout();
        layout.setColumnData(iconColumn.getColumn(), new ColumnWeightData(5));
        layout.setColumnData(testIdColumn.getColumn(), new ColumnWeightData(15));
        layout.setColumnData(futColumn.getColumn(), new ColumnWeightData(15));
        layout.setColumnData(msgColumn.getColumn(), new ColumnWeightData(70));
        tablePanel.setLayout(layout);
        
        m_errorsTable.setContentProvider(new ErrorsContentProvider());
        m_errorsTable.setLabelProvider(new ErrorsLabelProvider(m_okIcon, 
                                                               m_infoIcon,
                                                               m_warningIcon, 
                                                               m_errorIcon,
                                                               m_fatalIcon));

        Table table = m_errorsTable.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        m_detailPaneTxt = new StyledText(sash, 
                               SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        m_detailPaneTxt.setLayoutData("growx, growy, wmin 0, hmin 0");
        m_detailPaneTxt.setEditable(false);
        m_detailPaneTxt.setData(SWTBotConstants.SWT_BOT_ID_KEY, SWTBotConstants.BOT_STATUS_TEXT);
        
        sash.setWeights(new int[]{50, 50});
        
        m_errorsTable.setInput(StatusModel.instance());
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        m_winIDEAConnectionStatusLbl = builder.label("");
        m_winIDEAConnectionStatusLbl.setImage(IconProvider.INSTANCE.getIcon(EIconId.EDisconnectedFromwinIDEA));

        m_stdProVersionIcon = builder.label("", "skip 2, al right");
        setLicenseIcon();
        
        StatusModel.instance().addListener(this);
        
        m_mdParser = new MarkdownParser(m_detailPaneTxt); // used for font creation
    }

    
    private void addDropListener(StructuredViewer viewer) {
        int operations = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transferTypes = new Transfer[]{FileTransfer.getInstance()};
        TestStatusViewDropListener dropListener = new TestStatusViewDropListener(viewer);
        viewer.addDropSupport(operations, transferTypes, dropListener);
    }


    @Override
    public void setFocus() {
        m_errorsTable.getControl().setFocus();

    }


    @Override
    public void appendLine(StatusModelEvent event) {

        m_errorsTable.add(event.getStatusLine());
    }


    @Override
    public void refresh(StatusModelEvent event) {
        IStructuredSelection selection;
        if (event.getStatusLine() != null) {
            selection = new StructuredSelection(event.getStatusLine());
        } else {
            selection = new StructuredSelection();
        }
        
        m_errorsTable.refresh();
        m_errorsTable.setSelection(selection);
    }

    
    /**
     * Call this method directly, if you want to set a message for current user's 
     * action. If you want the text to be remembered (for example during test run),
     * call StatusModel.appendDetailPaneText().
     */
    public void setDetailPaneText(StatusType status, String text) {
        setDetailPaneText(new StatusModelEvent(null, text, status));
    }
    
    /**
     * The same as setDetailPaneText(), but it flashes view background. 
     * @param status
     * @param text
     */
    public void flashDetailPaneText(StatusType status, String text) {

        StatusType flashStatus = StatusType.INFO; 
    
        if (status == StatusType.ERROR  ||  status == StatusType.FATAL) {
            flashStatus = StatusType.WARNING;
        }
        
        setDetailPaneText(new StatusModelEvent(null, text, flashStatus));
        
        try { Thread.sleep(500); } catch (InterruptedException ex1) {
            ex1.printStackTrace(); }

        setDetailPaneText(new StatusModelEvent(null, text, status));
    }
    
    
    @Override
    public void setDetailPaneText(StatusModelEvent event) {
        
        String text = event.getDetailPaneText();
        
        if (event.getTextFormat() == ETextFormat.EMarkdownText) {
            List<StyleRange> styles = new ArrayList<>();
            StringBuilder sb = m_mdParser.markdown2StyleRanges(text, styles);
        
            m_detailPaneTxt.setText(sb.toString());
            m_detailPaneTxt.setStyleRanges(styles.toArray(new StyleRange[0]));
        } else {
            m_detailPaneTxt.setText(text);
        }
            
        if (text.isEmpty()) {
            m_detailPaneTxt.setBackground(ColorProvider.instance().getBkgNoneditableColor());
        } else {
            Color color = null;
            
            switch (event.getSeverity()) {
            case OK:
                color = ColorProvider.instance().getColor(ColorProvider.LIGHT_GREEN);
                break;

            case INFO:
                color = ColorProvider.instance().getInfoColor();
                break;

            case WARNING:
                color = ColorProvider.instance().getColor(ColorProvider.LIGHT_ORANGE);
                break;
            
            case ERROR:
                color = ColorProvider.instance().getColor(ColorProvider.LIGHT_RED);
                break;
            
            case FATAL:
                color = ColorProvider.instance().getColor(ColorProvider.RED);
                break;
            default:
                break;
            }
            m_detailPaneTxt.setBackground(color);
        }
        
//        if (event.isScrollDetailedPaneToEnd()) {
//            // make the last row visible
//            int numLines = DataUtils.countLines(text);
//            m_detailPaneTxt.setTopIndex(numLines); 
//        }

        // make the first mline of the last message visible
        if (event.getScrollToLine() >= 0) {
            m_detailPaneTxt.setTopIndex(event.getScrollToLine()); 
        }
    }


    public void setConnectionStatus(ITestStatusLine.StatusImageId imageId) {
        switch (imageId) {
        case CONNECTED:
            m_winIDEAConnectionStatusLbl.setImage(IconProvider.INSTANCE.getIcon(EIconId.EConnectedToWinIDEA));
            break;
        case DISCONNECTED:
            m_winIDEAConnectionStatusLbl.setImage(IconProvider.INSTANCE.getIcon(EIconId.EDisconnectedFromwinIDEA));
            break;
        default:
            m_winIDEAConnectionStatusLbl.setImage(IconProvider.INSTANCE.getIcon(EIconId.EDisconnectedFromwinIDEA));
        }
    }


    public void setLicenseIcon() {
        
    	// m_stdProVersionIcon.setImage(IconProvider.INSTANCE.getIcon(EIconId.EProVersion));
    } 
}


class ErrorsLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    private Image m_okIcon;
    private Image m_infoIcon;
    private Image m_warningIcon;
    private Image m_errorIcon;
    private Image m_fatalIcon;

    
    public ErrorsLabelProvider(Image okIcon, Image infoIcon, Image warningIcon, 
                               Image errorIcon, Image fatalIcon) {
        super();
        m_okIcon = okIcon;
        m_infoIcon = infoIcon;
        m_warningIcon = warningIcon;
        m_errorIcon = errorIcon;
        m_fatalIcon = fatalIcon;
    }

    
    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        if (element instanceof StatusTableLine) {
            StatusTableLine tableLine = (StatusTableLine)element;

            if (columnIndex == 0) {
                switch (tableLine.getStatusType()) {
                case OK:
                    return m_okIcon;
                case INFO:
                    return m_infoIcon;
                case WARNING:
                    return m_warningIcon;
                case ERROR:
                    return m_errorIcon;
                case FATAL:
                    return m_fatalIcon;
                default:
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        StatusTableLine statusLine = (StatusTableLine)element;
        
        switch (columnIndex) {
        case 0:
            return ""; // column with icon
        case 1:
            return statusLine.getTestId();
        case 2:
            return statusLine.getFunctionName();
        case 3:
            return statusLine.getMessage();
        }
        
        return "invalid column index";
    }
}


class ErrorsContentProvider implements IStructuredContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
        
        if (inputElement instanceof StatusModel) {
            StatusModel statusModel = (StatusModel) inputElement;
            return statusModel.getElements();
        }
        
        return new Object[0];
    }

    
    @Override
    public void dispose() {
    }

    
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
}
