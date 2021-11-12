package si.isystem.itest.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.common.UiUtils;
import si.isystem.itest.main.Activator;
import si.isystem.itest.wizards.TCGenOccur.EOccurenceType;
import si.isystem.ui.utils.AsystContentProposalProvider;
import si.isystem.ui.utils.ColorProvider;
import si.isystem.ui.utils.FontProvider;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableModelAdapter;
import de.kupzog.ktable.editors.ContentProposalConfig;
import de.kupzog.ktable.editors.KTableCellEditorComboText;
import de.kupzog.ktable.editors.KTableCellEditorText2;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;


interface IModelChangedListener {
    void modelChanged(boolean isRedrawNeeded);
}

/**
 * Model for KTable which shows identifiers (func. parameters, variables, 
 * options, ...).
 * 
 * @author markok
 *
 */
class TCGenTableModel extends KTableModelAdapter {

    // column indices
    protected final static int COL_HEADER = 0;
    protected final static int COL_VALUES = 1;
    protected final static int COL_VALUES_2 = 2;
    private final static int COL_RANGE_START = 3;
    private final static int COL_RANGE_END = 4;
    private final static int COL_RANGE_STEP = 5;
    private final static int COL_CUSTOM_OCCURRENCE = 6;
    
    // this item contains data to be shown
    protected TCGenSection m_genSection;
    
    private final static String [] COL_TITLES = 
            new String[]{"Parameters", // set later in ctor 
                         "List of values", 
                         "Occurrence", 
                         "Range start", 
                         "Range end", 
                         "Range step", 
                         "Occurrence"};
    
    private int m_colWidths[];
    
    // the last line in table which contains + sign
    protected TextIconsContent m_addRowCellContent;
    // cells with '+' and 'x' icons (ident. name and custom value name)
    protected TextIconsContent m_inserRemoveCellContent = new TextIconsContent();
    // other editable cells
    protected TextIconsContent m_editableCellContent = new TextIconsContent();

    // renderers
    protected TextCellRenderer m_tableHeaderRenderer = new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
    
    // odd identifiers have different background color than even ones
    protected TextIconsCellRenderer m_evenRowRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);
    
    protected TextIconsCellRenderer m_oddRowRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);
    
    protected TextCellRenderer m_valueOccurHeaderRenderer = 
                    new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);
    
    protected TextCellRenderer m_noneditableCellRenderer = 
                    new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);

    protected TextIconsCellRenderer m_lastRowRenderer = 
            new TextIconsCellRenderer(DefaultCellRenderer.INDICATION_FOCUS, false);
    
    // editors
    protected KTableCellEditorComboText m_occurIdentComboEditor = new KTableCellEditorComboText();
    protected KTableCellEditorComboText m_occurValueComboEditor = new KTableCellEditorComboText();
    protected KTableCellEditorComboText m_valueComboEditor = new KTableCellEditorComboText();
    
    protected IModelChangedListener m_modelChangedListener;
    protected AsystContentProposalProvider m_contentProposals;
    private String[] m_columnTitles;

    TCGenTableModel(Control control, String identifiersType) {
        this(control, identifiersType, Arrays.copyOf(COL_TITLES, COL_TITLES.length));
    }
    
    TCGenTableModel(Control control, String identifiersType, String [] colTitles) {
        
        int idx = 0;
        m_columnTitles = colTitles;
        
        colTitles[0] = identifiersType;
        m_colWidths = new int [colTitles.length];
        for (String colTxt : colTitles) {
            m_colWidths[idx++] = (int)(FontProvider.instance().getTextWidth(control, colTxt) * 1.3);
        }
        
        // m_colWidths[0] *= 2; // identifier names may be long
        m_colWidths[1] *= 2; // make list of values the widest field, because it
                             // may contain the longest string
        
        m_addRowCellContent = new TextIconsContent();
        Image icon = IconProvider.INSTANCE.getIcon(EIconId.EAddItem);
        m_addRowCellContent.setIcon(EIconPos.EMiddleMiddle, icon, true);
        m_addRowCellContent.setEditable(true);  // to get white background
        
        m_inserRemoveCellContent.setIcon(EIconPos.ETopRight, 
                                         IconProvider.INSTANCE.getIcon(EIconId.EAddTableColumn), 
                                         true);
        m_inserRemoveCellContent.setIcon(EIconPos.EBottomRight, 
                                         IconProvider.INSTANCE.getIcon(EIconId.EDeleteTableColumn), 
                                         true);
        m_inserRemoveCellContent.setIcon(EIconPos.ETopLeft, 
                                         IconProvider.INSTANCE.getIcon(EIconId.EUpInTable), 
                                         true);
        m_inserRemoveCellContent.setIcon(EIconPos.EBottomLeft, 
                                         IconProvider.INSTANCE.getIcon(EIconId.EDownInTable), 
                                         true);
        m_inserRemoveCellContent.setEditable(true);
        
        m_editableCellContent.setEditable(true);
        
        List<String> enumsText = new ArrayList<>();
        for (TCGenOccur.EOccurenceType occurType : TCGenOccur.EOccurenceType.values()) {
            if (occurType != EOccurenceType.N_TIMES) { // user has to enter its own value
                enumsText.add(occurType.getUIString());
            }
        }
        
        m_occurIdentComboEditor.setItems(enumsText.toArray(new String[0]));
        
        enumsText.remove(enumsText.size() - 1); // there is no 'Custom' setting for value
        m_occurValueComboEditor.setItems(enumsText.toArray(new String[0]));
        
        m_tableHeaderRenderer.setBackground(ColorProvider.instance().getColor(0xd0d0ff));
        
        // m_evenEditableCellRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
        m_evenRowRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_CYAN));
        
        // m_oddEditableCellRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_GREEN));
        m_oddRowRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_GREEN));
        
        m_valueOccurHeaderRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.VERY_LIGHT_GRAY));
        m_noneditableCellRenderer.setBackground(DefaultCellRenderer.COLOR_LINE_LIGHTGRAY);
    }
    
    
    public void setData(TCGenSection tcGenSection) {
        m_genSection = tcGenSection;        
    }
    
    
    public void setAutoCompleteProposals(String [] identifiers, String [] descriptions) {
        m_contentProposals = new AsystContentProposalProvider(identifiers, descriptions);
        m_contentProposals.setFiltering(true);
    }
    
    
    /**
     * Verifies consistency of data in the model. 
     * @return null if everything is OK, String describing the error in case
     * of error.
     */
    public String verifyModel() {
        
        return m_genSection.verifyData();
    }


    public void addValueOccurrence(int row) {
        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return; // should never happen
        }
        
        identifier.addAutoValueOccurrence(-1);
        m_modelChangedListener.modelChanged(true);
    }


    protected EOccurenceType getSecOType() {
        return m_genSection.getOccurrence().getOccurrenceType();
    }


    public boolean isAddValueOccurrence(int col, int row) {

        if (row == 0) {
            return false;
        }
        
        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return false;
        }
        
        if (mrow.intValue() > 0  &&  mrow.intValue() == identifier.getNumRows(getSecOType()) - 1
                &&  (col == COL_VALUES  ||  col == COL_VALUES_2)) {
            return true;
        }
        
        return false;
    }


    void setModelChangedListener(IModelChangedListener listener) {
        m_modelChangedListener = listener;
    }
    
    
    void setSectionOccurrenceAsNTimesValue(String sectionOccurrence) {
        TCGenOccur occurrence = m_genSection.getOccurrence();
        occurrence.setNTimesValue(sectionOccurrence); 
    }
    
 
    void setSectionOccurrence(EOccurenceType oType) {
        TCGenOccur occurrence = m_genSection.getOccurrence();
        occurrence.setOccurrenceType(oType);
    }
    
 
    void addIdentifier() {
        m_genSection.addIdentifier(-1, "");
        m_modelChangedListener.modelChanged(true);
    }
    
    
    @Override
    public Object doGetContentAt(int col, int row) {
        Object val = getCellContent(col, row);
        if (val == null) {
            return "";
        }
        return val;
    }
    
    
    @Override
    public int getFixedHeaderColumnCount() {
        return 0;
    }
    
    
    @Override
    public int doGetRowCount() {
        return m_genSection.getNumTableRows() + 2; // 1 for main table header, 1 for last row with + sign
    }

    
    @Override
    public int doGetColumnCount() {
        if (m_genSection.getOccurrence().getOccurrenceType() == EOccurenceType.CUSTOM) {
            return COL_CUSTOM_OCCURRENCE + 1;
        }
        return COL_CUSTOM_OCCURRENCE;
    }
 
    
    @Override
    public int getInitialColumnWidth(int column) {
        return m_colWidths[column];
    }

    
    @Override
    public int getInitialRowHeight(int row) {
        return FontProvider.instance().getDefaultTableRowHeight(Activator.getShell());
    }
    
    
    @Override
    public Point belongsToCell(int col, int row) {

        // the first row
        if (row == 0  &&  col == COL_VALUES_2) {
            return new Point(COL_VALUES, row);
        }
        
        // the last row has only header column and all other cells with + sign
        if (row == getRowCount() - 1) {
            return new Point(COL_HEADER, row);
        }

        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return null;  // happens on the last row with + sign
        }
        
        int numRows = identifier.getNumRows(getSecOType());
        // the first identifier row
        if (mrow.intValue() == 0  &&  col == COL_VALUES_2) {
            return new Point(COL_VALUES, row);
        }
        
        if (numRows > 1  &&  mrow.intValue() == numRows - 1  &&  col == COL_VALUES_2) {
            // the last row in custom values (with + sign) 
            return new Point(COL_VALUES, row);
        } 
        
        return null;
    }
    
    
    protected Object getCellContent(int col, int row) {
        
        if (row == 0) {
            return m_columnTitles[col];
        }
        
        if (row == getRowCount() - 1) {
            return m_addRowCellContent;
        }
        
        
        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return "+"; // should never happen
        }
        
        if (mrow.intValue() == 0) {
            switch (col) {
            case COL_HEADER:
                m_inserRemoveCellContent.setText(identifier.getIdentifierName());
                return m_inserRemoveCellContent;
            case COL_VALUES:
                m_editableCellContent.setText(identifier.getValuesAsString());
                return m_editableCellContent;
            case COL_RANGE_START:
                m_editableCellContent.setText(identifier.getRangeStart());
                return m_editableCellContent;
            case COL_RANGE_END:
                m_editableCellContent.setText(identifier.getRangeEnd());
                return m_editableCellContent;
            case COL_RANGE_STEP:
                m_editableCellContent.setText(identifier.getRangeStep());
                return m_editableCellContent;
            case COL_CUSTOM_OCCURRENCE:
                m_editableCellContent.setText(identifier.getOccurrence().getUIString());
                return m_editableCellContent;
            // default is null
            }
        } else {
            
            int identifierRows = identifier.getNumRows(getSecOType());
            
            if (identifierRows > 1) {
                if (mrow.intValue() == 1) {

                    switch (col) {
                    case COL_VALUES:
                        return "Value";
                    case COL_VALUES_2:
                        return "Occurrence";
                        // default is null
                    }
                } else if (mrow.intValue() < identifierRows - 1) {
                    int valueRow = mrow.intValue() - 2;
                    
                    TCGenValue customValueOccur = identifier.getCustomValueOccurrence(valueRow);

                    switch (col) {
                    case COL_VALUES:
                        m_inserRemoveCellContent.setText(customValueOccur.getValue());
                        return m_inserRemoveCellContent;
                    case COL_VALUES_2:
                        m_editableCellContent.setText(customValueOccur.getOccurrence().getUIString());
                        return m_editableCellContent;
                    }
                } else {
                    if (col == COL_VALUES) {
                        return m_addRowCellContent;
                    } else {
                        return null;
                    }
                }
            }
        }
        
        return null;
    }

    
    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {
        
        if (row == 0  ||  row == getRowCount() - 1) {
            return null; // first and last row have no editors
        }
        
        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return null;
        }
        
        int identifierRows = identifier.getNumRows(getSecOType());
        
        if (mrow.intValue() == 0) {
            switch (col) {
            case COL_HEADER:
                ContentProposalConfig cfg = new ContentProposalConfig(m_contentProposals,
                                                                      null);
                UiUtils.setContentProposalsConfig(cfg);
                return new KTableCellEditorText2(cfg); 
            case COL_VALUES:
            case COL_RANGE_START:
            case COL_RANGE_END:
            case COL_RANGE_STEP:
                return new KTableCellEditorText2();
            case COL_CUSTOM_OCCURRENCE:
                return m_occurIdentComboEditor;
            }
        } else if (identifierRows > 1) {
            if (mrow.intValue() == 1) {
                return null; // header row for custom values has no editors
                
            } else if (mrow.intValue() < identifierRows - 1) {
                
                switch (col) {
                case COL_VALUES:
                    List<String> items = identifier.getAllValuesForCombo();
                    items.add(TCGenIdentifier.OTHER_VALUES_STR);
                    m_valueComboEditor.setItems(items.toArray(new String[0]));
                    return m_valueComboEditor;
                case COL_VALUES_2:
                    return m_occurValueComboEditor;
                }
            } else {
                // the last row in a table is handled above
            }
        }
        
        return null;
    }

   
    @Override
    public KTableCellRenderer doGetCellRenderer(int col, int row) {
        
        if (row == 0) {
            return m_tableHeaderRenderer;                    
        }
        
        if (row == getRowCount() - 1) {
            return m_lastRowRenderer;
        }

        MutableInt mrow = new MutableInt(row - 1);
        MutableBoolean isEvenIdx = new MutableBoolean();
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType(), isEvenIdx);
        if (identifier == null) {
            return null;
        }
        
        KTableCellRenderer renderer = isEvenIdx.isTrue() ?
                m_evenRowRenderer : m_oddRowRenderer;
              // m_evenEditableCellRenderer : m_oddEditableCellRenderer;
        
        if (mrow.intValue() == 0) {
            if (col == COL_HEADER) {
                return isEvenIdx.isTrue() ? m_evenRowRenderer : m_oddRowRenderer;
                //return isEvenIdx.isTrue() ? m_evenEditableCellRenderer : m_oddEditableCellRenderer;
            }
            return renderer;               
        } else if (col < COL_VALUES  ||  col > COL_VALUES_2) {
                return m_noneditableCellRenderer; // unused cells left and right of custom occurrences  
        } else {

            int identifierRows = identifier.getNumRows(getSecOType());
            
            if (mrow.intValue() == 1) {  // custom values / occurrence header
                return m_valueOccurHeaderRenderer;
            } else if (mrow.intValue() == identifierRows - 1) {
                // custom values / occurrence + sign
                return isEvenIdx.isTrue() ? m_evenRowRenderer : m_oddRowRenderer; 
                //return isEvenIdx.isTrue() ? m_evenEditableCellRenderer : m_oddEditableCellRenderer;
            } else {  
                // custom values / occurrence editable cells 
                return renderer;
            }
        }
    }
    
    
    @Override
    public void doSetContentAt(int col, int row, Object value) {
        
        // System.out.println("doSetContentAt");
        boolean isRedrawNeeded = false;

        if (row == 0  ||  row == getRowCount() - 1) {
            return;
        }
        
        String strVal = value == null ? "" : value.toString().trim();
        
        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return;
        }
        
        int identifierRows = identifier.getNumRows(getSecOType());
        
        if (mrow.intValue() == 0) {
            switch (col) {
            case COL_HEADER:
                identifier.setIdentifierName(strVal);
                break;
            case COL_VALUES:
                identifier.setValues(strVal);
                break;
            case COL_RANGE_START:
                identifier.setRangeStart(strVal);
                break;
            case COL_RANGE_END:
                identifier.setRangeEnd(strVal);
                break;
            case COL_RANGE_STEP:
                identifier.setRangeStep(strVal);
                break;
            case COL_CUSTOM_OCCURRENCE:
                    identifier.setOccurrence(strVal);
                    isRedrawNeeded = true;
                break;
            }
        } else if (identifierRows > 1) {
            if (mrow.intValue() == 1) {
                // do not set anything - it is values header row
            } else if (mrow.intValue() < identifierRows - 1) {
                int valueRow = mrow.intValue() - 2;
                
                TCGenValue customValueOccur = identifier.getCustomValueOccurrence(valueRow);

                switch (col) {
                case COL_VALUES:
                    customValueOccur.setValue(strVal);
                    break;
                case COL_VALUES_2:
                    customValueOccur.getOccurrence().setValue(strVal);
                    break;
                }
            } else {
                // the last row in the table is handled above
            }
        }
        
        m_modelChangedListener.modelChanged(isRedrawNeeded);
    }

    
    /**
     * Adds or removes identifier or value-occurrence, depending on the cell
     * and area inside cell clicked.
     * 
     * @param col cell column
     * @param row cell row
     * @param cellRect cell coordinates in pixels
     * @param x click coordinate
     * @param y click coordinate
     * 
     * @return false if model was not changed - no redraw needed, true if table
     *         redraw is required
     */
    public void addRemoveItem(int col,
                                 int row,
                                 Rectangle cellRect,
                                 int x,
                                 int y) {
        
        if (row == 0) {
            return ;
        }
        
        if (row == getRowCount() - 1) { // add identifier is clicked
            addIdentifier();
            return;
        } else if (isAddValueOccurrence(col, row)) {
            addValueOccurrence(row);
            return;
        }        
        
        MutableInt mrow = new MutableInt(row - 1);
        MutableInt identIdx = new MutableInt();
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType(), identIdx);
        if (identifier == null) {
            return;
        }
        
        boolean isModelChanged = true;
        
        if (mrow.intValue() == 0  &&  col == COL_HEADER) {
            EIconPos iconPos = TextIconsCellRenderer.getIconPos(cellRect, x, y);
            int idx = identIdx.intValue();
            switch (iconPos) {
            case ETopRight:
                m_genSection.addIdentifier(idx, "");
                break;
            case EBottomRight:
                m_genSection.removeIdentifier(idx);
                break;
            case ETopLeft:
                m_genSection.swapIdentifiers(idx, idx - 1);
                break;
            case EBottomLeft:
                m_genSection.swapIdentifiers(idx, idx + 1);
                break;
            default:
                isModelChanged = false;
            } 
            
        } else if (mrow.intValue() > 1  &&  col == COL_VALUES) {
            
            int valueIdx = mrow.intValue() - 2;
            EIconPos iconPos = TextIconsCellRenderer.getIconPos(cellRect, x, y);
            switch (iconPos) {
            case ETopRight:
                identifier.addAutoValueOccurrence(valueIdx);
                break;
            case EBottomRight:
                identifier.removeValueOccurrence(valueIdx);
                break;
            case ETopLeft:
                identifier.swapValueOccurrence(valueIdx, valueIdx - 1);
                break;
            case EBottomLeft:
                identifier.swapValueOccurrence(valueIdx, valueIdx + 1);
                break;
            default:
                isModelChanged = false;
            } 
        } else {
            isModelChanged = false;
        }
        
        
        if (isModelChanged) {
            m_modelChangedListener.modelChanged(true);
        }
    }
}