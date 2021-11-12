package si.isystem.itest.preferences.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.UiTools;

/**
 * This class displays the following control:
 *   'User text'  'font name' ['font previewer'] SelectionButton
 * This class has not been fully tested, especially regarding layout - font
 * previwer is to small for larger fonts. Finalize it, when youl' actually need it.
 * 
 */
public class PrefFontEditor extends FontFieldEditor {

    private String m_layoutData;
    private boolean m_isInitialized = false;
    private String m_tooltip;
    private String m_previewAreaText;
    private DefaultPreviewer m_previewer;

    /** Font data for the chosen font button, or <code>null</code> if none. */
    private FontData[] m_chosenFont;
    private Button m_changeFontButton;
    private Label m_valueControl;
    private String m_changeButtonText;

    
    /**
     * Creates a font field editor with an optional preview area.
     * 
     * @param prefName the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param previewAreaText the text used for the preview window. If it is
     * <code>null</code> there will be no preview area,
     * @param parent the parent of the field editor's control
     */
    public PrefFontEditor(String prefName, String labelText, 
                          String previewAreaText, Composite parent, 
                          String layoutData, String tooltip) {
        super(prefName, labelText, previewAreaText, parent);
        
        // workaround to prevent creation of controls before the layout can be set
        m_isInitialized = true;
        m_previewAreaText = previewAreaText;
        m_layoutData = layoutData;
        m_tooltip = tooltip;
        m_changeButtonText = JFaceResources.getString("openChange"); //$NON-NLS-1$
        
        createControl(parent);
    }

    
    @Override
    protected void createControl(Composite parent) {
        
        if (!m_isInitialized) {
            return;
        }
        
        Label lbl = getLabelControl(parent);
        lbl.setLayoutData("split 2");

        m_valueControl = getValueControl(parent);

        m_valueControl.setLayoutData("");
        if (m_previewAreaText != null) {
            m_previewer = new DefaultPreviewer(m_previewAreaText, parent);
            m_previewer.getControl().setLayoutData("");
            if (m_tooltip != null) {
                UiTools.setToolTip(m_previewer.getControl(), m_tooltip);                
            }
        }

        getChangeControl(parent);
        m_changeFontButton.setLayoutData(m_layoutData);
    }

    
    /**
     * Returns the change button for this field editor.
     *
     * @param parent The Composite to create the button in if required.
     * @return the change button
     */
    @Override
    protected Button getChangeControl(Composite parent) {
        
        if (m_changeFontButton == null) {
            
            m_changeFontButton = new Button(parent, SWT.PUSH);
            
            if (m_changeButtonText != null) {
                m_changeFontButton.setText(m_changeButtonText);
            }
            
            m_changeFontButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    FontDialog fontDialog = new FontDialog(m_changeFontButton
                            .getShell());
                    if (m_chosenFont != null) {
                        fontDialog.setFontList(m_chosenFont);
                    }
                    FontData font = fontDialog.open();
                    if (font != null) {
                        FontData[] oldFont = m_chosenFont;
                        if (oldFont == null) {
                            oldFont = JFaceResources.getDefaultFont()
                                    .getFontData();
                        }
                        setPresentsDefaultValue(false);
                        FontData[] newData = new FontData[1];
                        newData[0] = font;
                        updateFont(newData);
                        fireValueChanged(VALUE, oldFont[0], font);
                    }

                }
            });
            
            m_changeFontButton.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    m_changeFontButton = null;
                }
            });
            m_changeFontButton.setFont(parent.getFont());
            setButtonLayoutData(m_changeFontButton);
        } else {
            checkParent(m_changeFontButton, parent);
        }
        return m_changeFontButton;
    }

    
    /**
     * Sets the text of the change button.
     *
     * @param text the new text
     */
    @Override
    public void setChangeButtonText(String text) {
        Assert.isNotNull(text);
        m_changeButtonText = text;
        if (m_changeFontButton != null) {
            m_changeFontButton.setText(text);
        }
    }

    
    @Override
    protected void applyFont() {
        if (m_chosenFont != null && m_previewer != null) {
            m_previewer.setFont(m_chosenFont);
        }
    }
    
    
    @Override
    protected void doLoad() {
        if (m_changeFontButton == null) {
            return;
        }
        updateFont(PreferenceConverter.getFontDataArray(getPreferenceStore(),
                                                        getPreferenceName()));
    }


    @Override
    protected void doLoadDefault() {
        if (m_changeFontButton == null) {
            return;
        }
        updateFont(PreferenceConverter.getDefaultFontDataArray(
                getPreferenceStore(), getPreferenceName()));
    }


    @Override
    protected void doStore() {
        if (m_chosenFont != null) {
            PreferenceConverter.setValue(getPreferenceStore(),
                    getPreferenceName(), m_chosenFont);
        }
    }
    
    /**
     * Updates the change font button and the previewer to reflect the
     * newly selected font.
     * @param font The FontData[] to update with.
     */
    private void updateFont(FontData font[]) {
        if (m_valueControl == null) {
            return;
        }
        
        FontData[] bestFont =
            JFaceResources.getFontRegistry().filterData(font, 
                                                        m_valueControl.getDisplay());

        //if we have nothing valid do as best we can
        if (bestFont == null) {
            bestFont = m_valueControl.getDisplay().getSystemFont().getFontData();
        }

        //Now cache this value in the receiver
        m_chosenFont = bestFont;

        m_valueControl.setText(StringConverter.asString(m_chosenFont[0]));
        
        if (m_previewer != null) {
            m_previewer.setFont(bestFont);
        }
    }

    
    /**
     * Internal font previewer implementation. Private in the base class, so we
     * must reimplement it here.
     */
    private static class DefaultPreviewer {
        private Text text;

        private String string;

        private Font font;

        /**
         * Constructor for the previewer.
         * @param s
         * @param parent
         */
        public DefaultPreviewer(String s, Composite parent) {
            string = s;
            text = new Text(parent, SWT.READ_ONLY | SWT.BORDER);
            text.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    if (font != null) {
                        font.dispose();
                    }
                }
            });
            if (string != null) {
                text.setText(string);
            }
        }

        /**
         * @return the control the previewer is using
         */
        public Control getControl() {
            return text;
        }

        /**
         * Sets the font to use.
         * @param fontData
         */
        public void setFont(FontData[] fontData) {
            if (font != null) {
                font.dispose();
            }
            font = new Font(text.getDisplay(), fontData);
            text.setFont(font);
        }
    }
}
