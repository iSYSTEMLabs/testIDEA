package de.kupzog.ktable;

import org.eclipse.swt.graphics.Rectangle;

/**
 * This interface allows interception of links on cells, given their exact
 * coordinates and preventing other processing of the click, such as for
 * selection. However, this is true only for listeners handled by KTable.
 * If you call Control.addMouseListener(), then these methods are called regardless
 * of value returned by this method in the following order:
 * KTableClickInterceptionListener.cellClicked(), MouseListener.mouseDown(), 
 * MouseListener.mouseUp(). It is advised to use only one of these listeners (usually
 * action is performed on mouse up event, usage of MouseListener is preferred).
 * 
 * @author Magnus von Koeller, Capgemini sd&amp;m AG
 */
public interface KTableClickInterceptionListener {

    /**
     * Is called when a cell is clicked.
     * 
     * @param col
     *            The column index of the clicked cell.
     * @param row
     *            The row index of the clicked cell.
     * @param cellRect
     *            The rectangle in which the clicked cell is painted.
     * @param x
     *            The horizontal position of the mouse at the time of the click.
     * @param y
     *            The vertical position of the mouse at the time of the click.
     * @param button
     *            With which button the click was performed (1 = first button, 2
     *            = second button, etc.)
     * @param table
     *            The table itself.
     *            
     * @return <code>true</code> if the click shall be treated as consumed, i.e.
     *         if it shall not be processed further by KTable, for example for
     *         the purpose of selection. <code>false</code> if normal processing
     *         of the click shall continue.
     */
    public boolean cellClicked(int col,
                               int row,
                               Rectangle cellRect,
                               int x,
                               int y,
                               int button,
                               KTable table);

}
