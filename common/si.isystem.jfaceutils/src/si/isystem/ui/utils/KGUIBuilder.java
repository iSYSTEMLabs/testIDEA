package si.isystem.ui.utils;

import java.io.File;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;


/**
 * This class contains utility methods for creation of GUI components. It is 
 * specialized for Mig layout.
 * 
 *  Example for views:
 *  <pre>
 *      public Composite createPartControl(Composite parent) {
 *      
 *        Composite tracePanel = new Composite(parent, SWT.NONE);
 *        FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(tracePanel);
 *
 *        // labels in the first column should occupy their min width, text fiedls should extend
 *        tracePanel.setLayout(new MigLayout("fillx", "[min!][grow]"));
 *        
 *        KGUIBuilder builder = new KGUIBuilder(tracePanel);
 *        
 *        builder.label("Timeout:");
 *        Text toutText = builder.text("wrap", SWT.BORDER);
 *
 *        builder.label("Limit:");
 *        Text limitText = builder.text("wrap", SWT.BORDER);
 *  </pre>
 *  Example for dialogs:
 *  <pre>
 *    protected Composite createDialogArea(Composite parent) {
 *  
 *        Composite composite = (Composite) super.createDialogArea(parent);
 *
 *        composite.getShell().setText("My dialog title");
 *        
 *        Composite mainDlgPanel = new Composite(composite, SWT.NONE);
 *        // required to get resizable composite
 *        GridData gridData = new GridData();
 *        gridData.verticalAlignment = GridData.FILL;
 *        gridData.horizontalAlignment = GridData.FILL;
 *        gridData.horizontalSpan = 1;
 *        gridData.grabExcessHorizontalSpace = true;
 *        gridData.grabExcessVerticalSpace = false;
 *        gridData.heightHint = 510;  // sets initial dialog size
 *        gridData.widthHint = 640;
 *        mainDlgPanel.setLayoutData(gridData);
 *        
 *        mainDlgPanel.setLayout(new MigLayout("fill", "[fill]"));
 *        
 *        KGUIBuilder builder = new KGUIBuilder(mainDlgPanel);
 *
 *        ... the rest is the same as in views example above ...
 *  </pre>
 * 
 * @author markok
 */
public class KGUIBuilder {

    private Composite m_parent;

    /** 
     * Creates builder, which will place components into the given <i>Composite</i>.
     * @param parent
     */
    public KGUIBuilder(Composite parent) {
        m_parent = parent;
    }

    /** 
     * Returns container (Composite, Group) of this object. 
     */
    public Composite getParent() {
        return m_parent;
    }

    
    /**
     * Creates label with icon.
     * 
     * @param imgDescriptor icon image descriptor
     * @param imageFilePath
     *            the relative path of the image file, relative to the root of
     *            the plug-in, for example "icons/cut_edit.gif"; the path must be legal   
     * @param layoutData Mig layout string
     * 
     * @return label with icon set
     */
    public Label iconLabel(ImageDescriptor imgDescriptor, String imageFilePath, String layoutData) {
        
        // doc for imageDescriptorFromPlugin() states, that null is returned if 
        // image can not be found
        if (imgDescriptor == null) {
            throw new IllegalArgumentException("Can not create icon for plugin: " +
                                               "imgDescriptor == null" +
                                              "\n  imageFile: " + imageFilePath);
        }
        Image icon = imgDescriptor.createImage();
        
        Label lbl = new Label(m_parent, SWT.NONE);
        lbl.setImage(icon);
        lbl.setLayoutData(layoutData);
        
        return lbl;
    }

    
    /**
     * Creates label with system icon.
     *  
     * @param iconId should be one of the icon constants
     *        specified in class <code>SWT</code>, for example SWT.ICON_ERROR.
     *         
     * @param layoutData Mig layout string
     *
     * @see SWT#ICON_ERROR
     * @see SWT#ICON_INFORMATION
     * @see SWT#ICON_QUESTION
     * @see SWT#ICON_WARNING
     * @see SWT#ICON_WORKING
     */
    public Label systemIconLabel(int iconId, String layoutData) {
        
        Image icon = m_parent.getDisplay().getSystemImage(SWT.ICON_ERROR);

        if (icon == null) {
            throw new IllegalArgumentException("Can not get icon!" +
                                               "\n  iconId: " + iconId);
        }
        
        Label lbl = new Label(m_parent, SWT.NONE);
        lbl.setImage(icon);
        lbl.setLayoutData(layoutData);
        
        return lbl;
    }
    
    
    /** 
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     * @return new composite, which is child of the current parent composite.
     */
    public Composite composite(int style, Object layoutData) {
        Composite child = new Composite(m_parent, style);
        
        if (layoutData != null) {
            child.setLayoutData(layoutData);
        }
        
        return child;
    }
    
    
    /**
     * Creates new Composite, sets its layout to be MigLayout and returns instance 
     * of this class aggregating the new Composite.
     * 
     * @param constraints parameter for MigLayout constructor
     * @param layoutData Mig layout string for the new Composite
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public KGUIBuilder newPanel(String constraints, String layoutData, int style) {
        Composite child = new Composite(m_parent, style);
        child.setLayout(new MigLayout(constraints));
        child.setLayoutData(layoutData);
        return new KGUIBuilder(child);
    }
    
    
    public KGUIBuilder newPanel(String constraints,
                                String columnConstraints,
                                String rowConstaints,
                                String layoutData, 
                                int style) {
        Composite child = new Composite(m_parent, style);
        child.setLayout(new MigLayout(constraints, columnConstraints, rowConstaints));
        child.setLayoutData(layoutData);
        return new KGUIBuilder(child);
    }
    
    
    /** 
     * Creates new <i>Label</i> with the given text. No layout data is attached.
     * 
     * @param text test in the label 
     */
    public Label label(String text) {
        
        Label lbl = new Label(m_parent, SWT.NONE);
        lbl.setText(text);
        
        return lbl;
    }


    /** 
     * Creates new <i>Label</i> with the given text and layout data. 
     * 
     * @param text test in the label
     * @param layoutData defines placement of the new label in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Label label(String text, String layoutData) {
        
        Label lbl = new Label(m_parent, SWT.NONE);
        lbl.setText(text);
        lbl.setLayoutData(layoutData);
        
        return lbl;
    }

    
    /** 
     * Creates new <i>Label</i> with the given image and layout data. 
     * 
     * @param image image in the label
     * @param layoutData defines placement of the new label in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Label label(Image image, String layoutData) {
        
        Label lbl = new Label(m_parent, SWT.NONE);
        lbl.setImage(image);
        lbl.setLayoutData(layoutData);
        
        return lbl;
    }

    
    /** 
     * Creates new <i>Label</i> with the given text, layout data and style. 
     * 
     * @param text test in the label
     * @param layoutData defines placement of the new label in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public Label label(String text, String layoutData, int style) {
        
        Label lbl = new Label(m_parent, style);
        lbl.setText(text);
        lbl.setLayoutData(layoutData);
        
        return lbl;
    }

    /**
     * Creates horizontal or vertical separator.
     * 
     * @param layoutData defines placement of the new label in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style should be one of SWT.HORIZONTAL or SWT.VERTICAL
     * 
     * @return the created component
     */
    public Label separator(String layoutData, int style) {
        Label separator = new Label(m_parent, SWT.SEPARATOR | style);
        separator.setLayoutData(layoutData);
        return separator;
    }
    
    
    /** 
     * Creates new <i>Text</i> control with the given layout data. 
     * 
     * @param layoutData defines placement of the new control in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Text text(String layoutData) {
        Text txt = new Text(m_parent, SWT.NONE);
        txt.setLayoutData(layoutData);
        
        return txt;
    }
    
    /** 
     * Creates new <i>Text</i> control with the given layout data and style. 
     * 
     * @param layoutData defines placement of the new control in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public Text text(Object layoutData, int style) {
        Text txt = new Text(m_parent, style);
        txt.setLayoutData(layoutData);
        
        return txt;
    }
    

    /** 
     * Creates new <i>Text</i> control with the given layout data and style. 
     * 
     * @param layoutData defines placement of the new control in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public StyledText styledText(Object layoutData, int style) {
        StyledText txt = new StyledText(m_parent, style);
        txt.setLayoutData(layoutData);
        
        return txt;
    }
    

    /** 
     * Creates new <i>Button</i> with the given text. 
     * 
     * @param text text in the button
     */
    public Button button(String text) {
        
        Button btn = new Button(m_parent, SWT.NONE);
        btn.setText(text);
        
        return btn;
    }

    
    /** 
     * Creates new <i>Button</i> with the given text and layout data. 
     * 
     * @param text text in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Button button(String text, Object layoutData) {
        
        return button(text, layoutData, SWT.NONE);
    }

    
    /** 
     * Creates new <i>Button</i> with the given text, layout data and style. 
     * 
     * @param text text in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public Button button(String text, Object layoutData, int style) {
        
        Button btn = new Button(m_parent, style);
        btn.setText(text);
        btn.setLayoutData(layoutData);
        
        return btn;
    }
    
    
    /** 
     * Creates new <i>Button</i> with the given image and layout data. 
     * 
     * @param image image in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Button button(Image image, Object layoutData) {
        
        Button btn = new Button(m_parent, SWT.NONE);
        btn.setImage(image);
        btn.setLayoutData(layoutData);
        
        return btn;
    }

    /**
     * Creates group of radio buttons. Usage example:
     * <pre>
     *  Composite radioGroup = new Composite(coveragePanel, SWT.NONE);
     *  Button []radioBtns = builder.radio(new String[]{"Off", "Start", "Restart"}, 
     *                                     radioGroup, "wrap");
     *  radioBtns[0].setToolTipText("Off mode");
     *  radioBtns[1].setToolTipText("Start mode");
     *  radioBtns[2].setToolTipText("Restart mode");
     *  
     *  radioBtns[0].setLayoutData("wrap");
     *  radioBtns[1].setLayoutData("wrap");
     * </pre>
     * 
     * @param buttonsText text to appear next to radio button
     * @param group composite to contain all buttons. Only one button inside 
     *              this group may be selected.
     * @param groupLayoutData Mig layout data for the group
     * 
     * @return array of newly created radio buttons
     */
    public Button[] radio(String[] buttonsText, Composite group, String groupLayoutData) {
        return radio(buttonsText, SWT.NONE, group, groupLayoutData);
    }
    
    
    /**
     * Creates group of radio buttons. Usage example:
     * <pre>
     *  Composite radioGroup = new Composite(coveragePanel, SWT.NONE);
     *  Button []radioBtns = builder.radio(new String[]{"Off", "Start", "Restart"}, 
     *                                     radioGroup, "wrap");
     *  radioBtns[0].setToolTipText("Off mode");
     *  radioBtns[1].setToolTipText("Start mode");
     *  radioBtns[2].setToolTipText("Restart mode");
     *  
     *  radioBtns[0].setLayoutData("wrap");
     *  radioBtns[1].setLayoutData("wrap");
     * </pre>
     * 
     * @param buttonsText text to appear next to radio button. One string in array
     *                    creates one radio button.
     * @param buttonsStyle button style, which will be or-ed with SWT.RADIO constant. Example: SWT.BORDER
     * @param group composite to contain all buttons. Only one button inside 
     *              this group may be selected.
     * @param groupLayoutData Mig layout data for the group
     * @return array of newly created radio buttons
     */
    public Button[] radio(String[] buttonsText, int buttonsStyle, Composite group, String groupLayoutData) {
        
        Button[] buttons = new Button[buttonsText.length];
        
        group.setLayoutData(groupLayoutData);
        group.setLayout(new MigLayout("fillx"));
        
        int idx = 0;
        for (String text : buttonsText) {
            buttons[idx] = new Button(group, SWT.RADIO | buttonsStyle);
            buttons[idx].setText(text);
            idx++;
        }

        return buttons;
    }

    
    /** 
     * Creates single radio button with the given text and layout data. 
     * 
     * @param buttonText test in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Button radio(String buttonText, String layoutData) {
        Button button = new Button(m_parent, SWT.RADIO);
        button.setText(buttonText);
        button.setLayoutData(layoutData);
        return button;
    }
    
    
    /** 
     * Creates new check box button with the given text. 
     * 
     * @param text text in the button
     */
    public Button checkBox(String text) {
        Button btn = new Button(m_parent, SWT.CHECK);
        btn.setText(text);
        
        return btn;
    }


    /** 
     * Creates new check box button with the given text and layout data. 
     * 
     * @param text text in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     */
    public Button checkBox(String text, String layoutData) {
        Button btn = new Button(m_parent, SWT.CHECK);
        btn.setText(text);
        btn.setLayoutData(layoutData);
        
        return btn;
    }

    
    /** 
     * Creates new check box button with the given text, layout data and style. 
     * 
     * @param text text in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public Button checkBox(String text, String layoutData, int style) {
        Button btn = new Button(m_parent, SWT.CHECK | style);
        btn.setText(text);
        btn.setLayoutData(layoutData);
        
        return btn;
    }

    
    /** 
     * Creates new combo box button with the given items, layout data and style. 
     * 
     * @param options text in the button
     * @param layoutData defines placement of the new component in parent. See 
     *        MigLayout for details on layout specification format.   
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     */
    public Combo combo(String [] options, String layoutData, int style) {
        
        Combo combo = new Combo(m_parent, style);
        for (String text : options) {
            combo.add(text);
        }
        combo.setLayoutData(layoutData);
        
        return combo;
    }
    
    /**
     * Returns new builder, which can be used to add nested components into this group.
     * Layout is set to MigLayout.
     * 
     * @param text title of the group
     */
    public KGUIBuilder group(String text) {
        Group group = new Group(m_parent, SWT.NONE);
        group.setLayout(new MigLayout());
        group.setText(text);
        return new KGUIBuilder(group);
    }

    
    /**
     * Returns new builder, which can be used to add nested components into 
     * this group. Mig layout without parameters is used as layout manager.  
     * 
     * @param text title of the group
     * @param layoutData 'wmin 0' is highly recommended for Mig layout in SWT, 
     *        otherwise group grows on resize
     * @return new builder, which will add components to the created group
     */
    public KGUIBuilder group(String text, String layoutData) {
        return group(text, layoutData, false, null, null, null);
    }
    
    
    /**
     * Returns new builder, which can be used to add components into 
     * this group. Mig layout is used as layout manager.
     * 
     * @param text title of the group
     * @param layoutData 'wmin 0' is highly recommended for Mig layout in SWT, 
     *        otherwise group grows on resize
     * @param isBold if true, group title is shown in bold font
     * @param layoutConstraints layout constraints for MigLayout (the first parameter in ctor).
     *                          If null, all three constraints are ignored.
     * @param columnConstraints column constraints for MigLayout (the second parameter in ctor).
     *                          If null, column and row constraints are not set.
     * @param rowConstraints row constraints for MigLayout (the third parameter in ctor).
     *                          If null, row constraints are not set.
     * @return new builder, which will add components to new group
     */
    public KGUIBuilder group(String text, String layoutData, boolean isBold,
                             String layoutConstraints, String columnConstraints, String rowConstraints) {
        Group group = new Group(m_parent, SWT.NONE);
        if (layoutConstraints == null) {
            group.setLayout(new MigLayout());
        } else if (columnConstraints == null) {
            group.setLayout(new MigLayout(layoutConstraints));
        } else if (rowConstraints == null) {
            group.setLayout(new MigLayout(layoutConstraints, columnConstraints));
        } else {
            group.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));
        }
        group.setLayoutData(layoutData);
        
        if (text != null) {
            group.setText(text);
        }

        if (isBold) {
            group.setFont(FontProvider.instance().getBoldControlFont(group));
        }
        
        return new KGUIBuilder(group);
    }
    
    
    /**
     * Returns new builder, which can be used to add nested components into 
     * this group. Mig layout without is used as layout manager  
     * 
     * @param text title of the group
     * @param layoutData 'wmin 0' is highly recommended for Mig layout in SWT, 
     *                   otherwise group grow on resize.
     * @param layoutConfig configuration for Mig layout. It must contain one,
     * two or three strings. Appropriate Mig layout ctor is called depending on 
     * the number of strings.
     * 
     * @return builder with created group as a container
     */
    public KGUIBuilder group(String text, String layoutData, String []layoutConfig) {
        Group group = new Group(m_parent, SWT.NONE);
        switch (layoutConfig.length) {
        case 1:
            group.setLayout(new MigLayout(layoutConfig[0]));
            break;
        case 2:
            group.setLayout(new MigLayout(layoutConfig[0], layoutConfig[1]));
            break;
        case 3:
            group.setLayout(new MigLayout(layoutConfig[0], layoutConfig[1], layoutConfig[2]));
            break;
        default:
            throw new IllegalArgumentException("There should 1, 2, or 3 elements " +
            		"in the 'layoutConfig' array, but threre are: " + layoutConfig.length);
        }
        
        group.setLayoutData(layoutData);
        group.setText(text);
        return new KGUIBuilder(group);
    }
    
    
    /**
     * Returns new builder, which can be used to add nested components into this group.
     * @param text title of the group
     * @param layoutData 'wmin 0' is highly recommended for Mig layout in SWT, otherwise group grow on resize
     * @param layout layout manager to be used for this group
     * @param style one of SWT style constants, for example SWT.NONE, SWT.BORDER, ...
     * 
     * @return builder with created group as a container
     */
    public KGUIBuilder group(String text, String layoutData, Layout layout, int style) {
        Group group = new Group(m_parent, style);
        group.setLayout(layout);
        group.setLayoutData(layoutData);
        group.setText(text);
        return new KGUIBuilder(group);
    }
    
    

    /**
     * Creates text input field with 'Browse' button.
     * Example:
     * <pre>
     *  Composite container = new Composite(parent, SWT.NULL);
     *
     *  container.setLayout(new MigLayout("fillx", "[min!][fill][min!]"));
     *
     *  KGUIBuilder builder = new KGUIBuilder(container);
     *
     *  builder.label("&Script file:");
     *  m_scriptFileNameBrowser = builder.createFileNameInput("Browse", 
     *                                                        "", 
     *                                                        "wrap", 
     *                                                        "Browse for Python extension script", 
     *                                                        new String[]{"*.py", "*.*"}, 
     *                                                        true,
     *                                                        SWT.SAVE);
     *  m_scriptFileNameBrowser.setToolTipText( 
     *       "Name of the script file. There can be only one script file per testIDEA project.");
     * </pre>
     * 
     * @param buttonText text to be written on the button, for example "Browse"
     * @param textLayoutData passed to underlying layout manager. If null,
     *                       "split 2, growx" is used by default 
     * @param buttonLayoutData passed to underlying layout manager. Example for Mig 
     *                         layout: "wrap"
     * @param browseDialogTitle title of the browse dialog
     * @param filterExtensions extensions to be shown in the dialog, for example
     *                         new String[]{ "*.html;*.xhtml", "*.*" };
     * @param isAutoAddExtension if true, and the user does not enter extension,
     *                           the extension selected in the dialog is added to 
     *                           the entered file name.
     * @param swtSaveOrSwtOpenStyle defines dialog type, should be one of SWT.SAVE,
     *                              SWT.OPEN, or SWT.MULTI
     */
    public FileNameBrowser createFileNameInput(String buttonText,
                                    Object textLayoutData, 
                                    Object buttonLayoutData, 
                                    final String browseDialogTitle, 
                                    final String []filterExtensions,
                                    boolean isAutoAddExtension,
                                    int swtSaveOrSwtOpenStyle) {
        if (textLayoutData == null) {
            textLayoutData = "split 2, growx";
        }
        
        Text inputField = text(textLayoutData, SWT.BORDER);
        
        Button button = createBrowseButton(buttonText, 
                                           buttonLayoutData, 
                                           browseDialogTitle, 
                                           filterExtensions, 
                                           inputField, 
                                           isAutoAddExtension,
                                           swtSaveOrSwtOpenStyle);

        return new FileNameBrowser(inputField, button);
    }

    
    /**
     * Creates 'Browse' button - when it is clicked, file browse dialog is opened.
     * 
     * @param buttonText text to be written on the button, for example "Browse"
     * @param layoutData passed to underlying layout manager. Example for Mig 
     *                   layout: "wrap"
     * @param browseDialogTitle title of the browse dialog
     * @param filterExtensions extensions to be shown in the dialog, for example
     *                         new String[]{ "*.html;*.xhtml", "*.*" };
     * @param fileTxt component to receive the selected file name
     * @param isAutoAddExtension if true, and the user does not enter extension,
     *                           the extension selected in the dialog is added to 
     *                           the entered file name.
     * @param swtSaveOrSwtOpenStyle defines dialog type, should be one of SWT.SAVE,
     *                              SWT.OPEN, or SWT.MULTI
     *                              
     * @see #createBrowseButton(String, String, String, String [], Combo)
     */
    public Button createBrowseButton(String buttonText,
                                     Object layoutData, 
                                     final String browseDialogTitle, 
                                     final String []filterExtensions, 
                                     final Text fileTxt,
                                     final boolean isAutoAddExtension,
                                     final int swtSaveOrSwtOpenStyle) {
        
        Button browseButton = button(buttonText, layoutData);
        browseButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent evt) {
                FileDialog openDialog = new FileDialog(m_parent.getShell(), swtSaveOrSwtOpenStyle);
                
                openDialog.setFilterExtensions(filterExtensions);                
                
                openDialog.setText(browseDialogTitle);
                String wsFileName = fileTxt.getText().trim();
                
                int lastSeparatorIndex = wsFileName.lastIndexOf(File.separator);
                if (lastSeparatorIndex != -1) {
                    // set initial directory, if present in file name
                    openDialog.setFilterPath(wsFileName.substring(0, lastSeparatorIndex));
                }
                String res = openDialog.open();
                if (res == null) {
                    return;
                }
                
                res = res.trim();
                
                if (isAutoAddExtension) {
                    // add selected extension to the file name, if it was not entered by 
                    // the user 
                    int extensionIdx = openDialog.getFilterIndex();
                    if (extensionIdx > -1) {
                        String extension = filterExtensions[extensionIdx];
                        int dotIdx = extension.lastIndexOf('.');
                        extension = extension.substring(dotIdx);

                        File userFile = new File(res);
                        // if there is no '.' in file name, add selected extension
                        if (!userFile.getName().contains(".")) {
                            res += extension;
                        }
                    }
                }
                
                fileTxt.setText(res);
                fileTxt.setFocus(); // set focus to text field so that user can 
                                    // change it and contents gets transferred 
                                    // to TBControl.
            }
        });
        
        return browseButton;
    }

    
    /**
     * Creates a button, which opens file dialog when opened. If user clicks OK,
     * the selected file name is entered into the given combo box.
     */
    public Button createBrowseButton(String buttonText,
                                     String layoutData, 
                                     final String browseDialogTitle, 
                                     final String []filterExtensions, 
                                     final Combo fileTxt) {
        
        Button browseButton = button(buttonText, layoutData);
        browseButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent evt) {
                FileDialog openDialog = new FileDialog(m_parent.getShell(), SWT.NONE);
                
                openDialog.setFilterExtensions(filterExtensions);                
                
                openDialog.setText(browseDialogTitle);
                String wsFileName = fileTxt.getText().trim();
                int lastSeparatorIndex = wsFileName.lastIndexOf(File.separator);
                if (lastSeparatorIndex != -1) {
                    openDialog.setFilterPath(wsFileName.substring(0, lastSeparatorIndex));
                }
                String res = openDialog.open();
                if (res == null) {
                    return;
                }
                fileTxt.setText(res);
                fileTxt.setFocus(); // set focus to text field so that user can 
                                    // change it and contents gets transferred 
                                    // to TBControl.
            }
        });        
        
        return browseButton;
    }
    
    
    /**
     * This method sets menu item visibility. It works for items from the 
     * sub-menu of the given menu. For example,
     * if the main application menu contains menu 'File', an item from this menu
     * can be modified.
     * 
     * @param mainMenuManager main application menu manager. You can get it either with
     *        <pre>
     *        appMenuManager = getWindowConfigurer().getActionBarConfigurer().getMenuManager();
     *        </pre>
     *        in <code>ApplicationWorkbenchWindowAdvisor.createActionBarAdvisor()</code>, or
     *        <pre> 
     *        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
     *
     *        if(window instanceof WorkbenchWindow) {
     *            MenuManager menuManager = ((WorkbenchWindow)window).getMenuManager();
     *            ...
     *        </pre>
     *        but the later approach uses internal classes.
     *        
     *           
     * @param mainMenuItemId id of the main menu item, for example of menu item 
     *                       'File', 'Edit', ... Note than these menu must have IDs
     *                       assigned in the plugin.xml. By default these IDs are not 
     *                       required.
     * @param menuItemCommandId id of the command id of the menu item of the 'mainMenuItemId', for
     *                          example command id of menu option 'Save'.
     * @param isVisible      if true, item is made visible
     * 
     * @return true if item was found and its visibility state set, false if 
     * it was not found, either because the menu is not fully created yet or
     * one of the given IDs was invalid.
     */
    public static boolean setMenuItemVisibility(IMenuManager mainMenuManager, 
                                                String mainMenuItemId, 
                                                String menuItemCommandId, 
                                                boolean isVisible) {
        
        // IMenuManager mainMenuManager = ApplicationWorkbenchWindowAdvisor.getMainMenuManager();

        if (mainMenuManager == null) {
            return false;
        }
        
        // IContributionItem[] contributions = menuManager.getItems(); // we have main menu items now
        // MenuManager fileMenuMgr = (MenuManager)contributions[0];

        // if we need to remove items from the coolbar as well, finish the following code
        /* ICoolBarManager coolBarManager = null;
               // see comment ApplicationWorkbenchWindowAdvisor.getMainMenuManager() for
               // getting instance of 'window'.
               if(((WorkbenchWindow) window).getCoolBarVisible()) {
                   coolBarManager = ((WorkbenchWindow)window).getCoolBarManager2();
            }
         */

        MenuManager subMenuMgr = (MenuManager)mainMenuManager.find(mainMenuItemId);

        IContributionItem item = subMenuMgr.find(menuItemCommandId);

        if (item != null) {
            // subMenuMgr.remove(item);
            item.setVisible(isVisible); // if we'd like to enable it later, this is better than
                                        // removing
            subMenuMgr.update(true);
            return true;
        }
        return false;
    }
}
