package si.isystem.itest.wizards;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import si.isystem.itest.wizards.TCGenOccur.EOccurenceType;
import si.isystem.ui.utils.ColorProvider;
import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;


/**
 * Model for KTable which shows section, their values and generated test cases.
 * @author markok
 *
 */
class TCGenTestCaseTableModel extends TCGenTableModel {

    // column indices
    private final static int TC_COL_CUSTOM_OCCURRENCE = 3;
    
    private final static String [] COL_TITLES = 
            new String[]{"Sections        ", // make column a bit wider 
                         "List of values", 
                         "Occurrence", 
                         "Occurrence"};

    protected TextCellRenderer m_identRowCellRenderer = 
                    new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS);

    
    TCGenTestCaseTableModel(Control control) {

        super(control, COL_TITLES[0], COL_TITLES);
        
        m_identRowCellRenderer.setBackground(ColorProvider.instance().getColor(ColorProvider.LIGHT_GRAY));
    }
    
    
    @Override
    void addIdentifier() {
        // no addition in test case vectors page
    }
    
    
    @Override
    public int doGetRowCount() {
        return m_genSection.getNumTableRows() + 1; // 1 for main table header
    }

    
    @Override
    public int doGetColumnCount() {
        if (m_genSection.getOccurrence().getOccurrenceType() == EOccurenceType.CUSTOM) {
            return TC_COL_CUSTOM_OCCURRENCE + 1;
        }
        return TC_COL_CUSTOM_OCCURRENCE;
    }
 
    
    @Override
    public Point belongsToCell(int col, int row) {

        // the first row
        if (row == 0  &&  col == COL_VALUES_2) {
            return new Point(COL_VALUES, row);
        }
        
        MutableInt mrow = new MutableInt(row - 1);
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType());
        if (identifier == null) {
            return null;  
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
    
    
    @Override
    protected Object getCellContent(int col, int row) {
        System.out.println("getCellContent: " + col + ", " + row);
        if (row == 0) {
            return COL_TITLES[col];
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
            case TC_COL_CUSTOM_OCCURRENCE:
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
        
        if (row == 0) {
            return null; // first row has no editors
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
            case COL_VALUES:
                return null;
            case TC_COL_CUSTOM_OCCURRENCE:
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
        
        MutableInt mrow = new MutableInt(row - 1);
        MutableBoolean isEvenIdx = new MutableBoolean();
        TCGenIdentifier identifier = m_genSection.getIdentifier(mrow, getSecOType(), isEvenIdx);
        if (identifier == null) {
            new Throwable().printStackTrace();
            return m_evenRowRenderer; // return something on invalid 
                                       // program state - still better then NPE 
        }
        
        KTableCellRenderer renderer = isEvenIdx.isTrue() ?
                m_evenRowRenderer : m_oddRowRenderer;
        
        if (mrow.intValue() == 0) {
            if (col == COL_HEADER  ||  col == COL_VALUES) {
                // return isEvenIdx.isTrue() ? m_evenInsertDelRenderer : m_oddInsertDelRenderer;
                return m_identRowCellRenderer;
            }
            return renderer;               
        } else if (col < COL_VALUES  ||  col > COL_VALUES_2) {
                return renderer; // unused cells left and right of custom occurrences  
        } else {

            int identifierRows = identifier.getNumRows(getSecOType());
            
            if (mrow.intValue() == 1) {  // custom values / occurrence header
                return m_valueOccurHeaderRenderer;
            } else if (mrow.intValue() == identifierRows - 1) {
                // custom values / occurrence + sign
                return isEvenIdx.isTrue() ? m_evenRowRenderer : m_oddRowRenderer; 
            } else {  
                // custom values / occurrence editable cells 
                return renderer;
            }
        }
    }
    
    
    @Override
    public void doSetContentAt(int col, int row, Object value) {
        
        System.out.println("doSetContentAt");
        boolean isRedrawNeeded = false;

        if (row == 0) {
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
            case COL_VALUES:
                break;
            case TC_COL_CUSTOM_OCCURRENCE:
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
    @Override
    public void addRemoveItem(int col,
                              int row,
                              Rectangle cellRect,
                              int x,
                              int y) {
        
        if (row == 0) {
            return;
        }
        
        if (row == 0) {
            return ;
        }
        
        if (isAddValueOccurrence(col, row)) {
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
        
        if (mrow.intValue() > 1  &&  col == COL_VALUES) {
            
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
