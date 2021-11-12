package si.isystem.tbltableeditor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import si.isystem.itest.common.UiUtils;
import si.isystem.itest.dialogs.TagCommentDialog;
import si.isystem.itest.main.Activator;
import si.isystem.itest.model.YamlScalar;
import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableModel;
import de.kupzog.ktable.renderers.TextIconsCellRenderer;
import de.kupzog.ktable.renderers.TextIconsContent;
import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;

/**
 * Listener Class that implements testIDEA specific mouse actions.
 */
abstract public class KTableListenerForTooltips implements Listener {

    private Shell m_tooltipShell = null;
    private Label m_tooltipLabel = null;
    private boolean m_isEditable = true;
    

    protected KTable m_ktable;

    
    /** This listener closes the tooltip shell when clicked. */
    final Listener m_labelListener = new Listener () {
        @Override
        public void handleEvent (Event event) {
            Label label = (Label)event.widget;
            Shell shell = label.getShell ();
            // forward mouse events directly to the underlying KTable
            switch (event.type) {
            case SWT.MouseDown:
                Event e = new Event ();
                e.item = m_ktable;
                e.button = event.button;
                e.stateMask = event.stateMask;
                m_ktable.notifyListeners(SWT.MouseDown, e);
                // fall through
            default:
                shell.dispose ();
                break;
            }
        }
    };


    /**
     * Creates object AND attached it to the given KTable.
     */
    public KTableListenerForTooltips(KTable table) {
        m_ktable = table;
        m_ktable.addListener(SWT.Dispose, this);
        m_ktable.addListener(SWT.KeyDown, this);
        m_ktable.addListener(SWT.MouseDown, this);
        m_ktable.addListener(SWT.MouseUp, this);
        m_ktable.addListener(SWT.MouseDoubleClick, this);
        m_ktable.addListener(SWT.MouseMove, this);
        m_ktable.addListener(SWT.MouseHover, this);
        m_ktable.addListener(SWT.MouseExit, this);
    }
    

    /**
     * Derived class defines meaning of cell icons in this method.
     */
    abstract protected void processClicksOnCellIcons(Event event);

    /**
     * If derived class calls method editComment() in this class, then this
     * method is called - it should not be empty. 
     */
    abstract protected void setComment(int col,
                                       int row,
                                       String newNlComment,
                                       String newEolComment);
    abstract protected void mouseUp(Event e);

    
    public boolean isEditable() {
        return m_isEditable;
    }


    public void setEditable(boolean isEditable) {
        m_isEditable = isEditable;
    }


    @Override
    public void handleEvent (Event event) {
        switch (event.type) {
        case SWT.MouseDown:
            releaseTooltipShell();
            if (event.button == 1  &&  m_isEditable) {  // no SWT constants for event.button values :(
                processClicksOnCellIcons(event);
            } 
            break;
            
        case SWT.MouseUp:
            if (event.button == 1  &&  m_isEditable) {  // no SWT constants for event.button values :(
                mouseUp(event);
            }
            break;
            
        case SWT.KeyDown:
            if (m_tooltipShell != null) {
                // ESC key must close the tooltip shell, but not the dialog
                releaseTooltipShell();
            } else {
                // send traversal event, needed when table is in dialog, so that the
                // dialog gets ENTER and ESC keys - OK/CANCEL buttons.
                m_ktable.traverse(SWT.TRAVERSE_NONE, event);
            }
            break;
        case SWT.Dispose:
        case SWT.MouseDoubleClick:
        case SWT.MouseMove: 
        case SWT.Selection: // scrolling
            releaseTooltipShell();
            break;
        case SWT.MouseExit:
            // if tooltip is released on this event, then it is immediately released
            // when the table is smaller than tooltip so that it is drawn below the 
            // current mouse cursor.
            // releaseTooltipShell();
            //System.out.println("Mouse exit");
            break;
        case SWT.MouseHover:
            showTooltipShell(event);
            break;
        }
    }


    /**
     *  Utility method for derived classes, which have comment editing icons.
     */
    protected void editComment(int col, int row, TextIconsContent cellTIContent) {
        
        String oldNlComment = cellTIContent.getNlComment();
        String oldEolComment = cellTIContent.getEolComment();

        // remove trailing \n, because it is always part of string returned by YAML
        // parser, but split() then returns one array element to much (the last one, which is empty)
        String nlComment = StringUtils.stripEnd(oldNlComment, null);
        String eolComment = StringUtils.stripEnd(oldEolComment, null);

        int nlCommentIndent = YamlScalar.getCommentIndent(nlComment);
        int eolCommentIndent = YamlScalar.getCommentIndent(eolComment);

        TagCommentDialog dlg = new TagCommentDialog(Activator.getShell());
        dlg.setNewLineComment(YamlScalar.stripCommentChars(nlComment));
        dlg.setEndOfLineComment(YamlScalar.stripCommentChars(eolComment));

        if (dlg.show()) {

            if (nlCommentIndent == 0) {
                nlCommentIndent = 4; 
            }
            if (eolCommentIndent == 0) {
                eolCommentIndent = 4;
            }

            String newNlComment = UiUtils.addCommentChar(dlg.getNewLineComment(), 
                                                         nlCommentIndent);
            String newEolComment = UiUtils.addCommentChar(dlg.getEndOfLineComment(), 
                                                          eolCommentIndent);

            if (!newNlComment.equals(oldNlComment)  ||  
                    !newEolComment.equals(oldEolComment)) {

                setComment(col, row, newNlComment, newEolComment);
            }
        }
    }

    
    protected EIconPos getIconPos(Event event, Point cell) {
        Rectangle rect = m_ktable.getCellRect(cell.x, cell.y);
        return TextIconsCellRenderer.getIconPos(rect, event.x, event.y);
    }
    
    
    private void showTooltipShell(Event event) {
        if (m_tooltipShell != null  &&  !m_tooltipShell.isDisposed()) {
            m_tooltipShell.dispose();
        }

        Point cell = m_ktable.getCellForCoordinates(event.x, event.y);
        
        KTableModel kModel = m_ktable.getModel();
        String tooltip = null;
        if (cell.x >= 0 && cell.x < kModel.getColumnCount() 
                && cell.y >= 0 && cell.y < kModel.getRowCount()) {
            
            Object cellContent = kModel.getContentAt(cell.x, cell.y);
            if (cellContent instanceof TextIconsContent) {
                TextIconsContent cellTIContent = (TextIconsContent)cellContent;
                EIconPos iconPos = getIconPos(event, cell);
                
                tooltip = cellTIContent.getTooltip(iconPos);
            }
        }

        // check if there is something to show, and abort otherwise:
        if (tooltip == null || tooltip.isEmpty() || cell.x == -1 || cell.y == -1) {
            m_tooltipShell = null;
            m_tooltipLabel = null;
            return;
        }

        m_tooltipShell = new Shell (Activator.getShell(), SWT.ON_TOP);
        GridLayout gl = new GridLayout();
        gl.marginWidth=2;
        gl.marginHeight=2;
        m_tooltipShell.setLayout (gl);
        Display display = m_tooltipShell.getDisplay();
        m_tooltipShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        m_tooltipLabel = new Label(m_tooltipShell, SWT.NONE);                    
        m_tooltipLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_tooltipLabel.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        m_tooltipLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        m_tooltipLabel.setText (tooltip);
        m_tooltipLabel.addListener(SWT.MouseExit, m_labelListener);
        m_tooltipLabel.addListener(SWT.MouseDown, m_labelListener);
        m_tooltipLabel.addListener(SWT.MouseMove, m_labelListener);
        Point size = m_tooltipShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        int y = 15; // make the top table header row visible 
        int x = 10;

        // the following does not work well for large tooltips on small tables - 
        // they cover the entire table in which case they can't be removed if
        // the table did not have focus before showing the tooltip (no events
        // can be sent to table if it does not have focus and is completely
        // covered by tooltip shell.
/*        // place the shell under the mouse, but check that the
        // bounds of the table are not overlapped.
        Rectangle tableBounds = m_table.getBounds();

        if (event.x + x + size.x > tableBounds.x + tableBounds.width) {
            event.x -= event.x + x + size.x - tableBounds.x - tableBounds.width;
        }

        if (event.y + y + size.y > tableBounds.y + tableBounds.height) {
            event.y -= event.y + y + size.y - tableBounds.y - tableBounds.height;
        }
*/
        Point pt = m_ktable.toDisplay(event.x + x, event.y + y);
        m_tooltipShell.setBounds(pt.x, pt.y, size.x, size.y);
        m_tooltipShell.setVisible(true);
    }


    private void releaseTooltipShell() {
        if (m_tooltipShell != null) {
            m_tooltipShell.dispose ();
            m_tooltipShell = null;
            m_tooltipLabel = null;
        }
    }
}
