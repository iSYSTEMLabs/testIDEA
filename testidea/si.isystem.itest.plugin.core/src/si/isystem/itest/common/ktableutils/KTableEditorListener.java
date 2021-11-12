//package si.isystem.itest.common.ktableutils;
//
//import org.eclipse.swt.graphics.Point;
//import org.eclipse.swt.graphics.Rectangle;
//import org.eclipse.swt.widgets.Event;
//
//import de.kupzog.ktable.KTable;
//import de.kupzog.ktable.renderers.TextIconsCellRenderer;
//import de.kupzog.ktable.renderers.TextIconsContent;
//import de.kupzog.ktable.renderers.TextIconsContent.EIconPos;
//import si.isystem.tbltableeditor.KTableListenerForTooltips;
//
//public class KTableEditorListener extends KTableListenerForTooltips {
//
//    private static final int COL_HEADER = 0;
//    private KTableEditorModel m_tableModel;
//
//    public KTableEditorListener(KTable ktable, KTableEditorModel tableModel) {
//        super(ktable);
//        m_tableModel = tableModel;
//    }
//    
//    @Override
//    public void mouseUp(Event e) {
//        Point selection = m_ktable.getCellForCoordinates(e.x, e.y);
//        Rectangle cellRect = m_ktable.getCellRect(selection.x, selection.y);
//        EIconPos iconPos = TextIconsCellRenderer.getIconPos(cellRect, e.x, e.y);
//
//        int itemIdx = selection.y - 1;
//
//        // handle '+' in the last row
//        if (selection.y == (m_tableModel.getRowCount() - 1)  &&  selection.x > 0) {
//            m_tableModel.addItem(itemIdx);
//            return;
//        }
//        
//        // handle icons in the header column (add, remove, up, down)
//        if (selection.y > 0  &&  selection.x == COL_HEADER) {
//            switch (iconPos) {
//            case ETopRight:
//                m_tableModel.addItem(itemIdx);
//                break;
//            case EBottomRight:
//                m_tableModel.removeItem(itemIdx);
//                break;
//            case ETopLeft:
//                m_tableModel.swapItems(itemIdx, itemIdx - 1);
//                break;
//            case EBottomLeft:
//                m_tableModel.swapItems(itemIdx, itemIdx + 1);
//                break;
//            default:
//                // ignore, other icons are not present
//                break;
//            }
//            return;
//        } 
//        
//        // handle comment icons in body cells
//        if (selection.y > 0  &&  selection.x > COL_HEADER) {
//            
//            if (iconPos == EIconPos.ETopLeft) {
//                TextIconsContent cellTIContent = (TextIconsContent)
//                        m_tableModel.getContentAt(selection.x, selection.y);
//                
//                if (!cellTIContent.getText().isEmpty()) {
//                    editComment(selection.x, selection.y, cellTIContent);
//                }
//            }
//        }
//    }
//
//    @Override
//    protected void processClicksOnCellIcons(Event event) {
//        // empty, event is handled in mouseUp() above
//    }
//
//    @Override
//    protected void setComment(int col,
//                              int row,
//                              String newNlComment,
//                              String newEolComment) {
//        
//        m_tableModel.setComment(col - 1, row - 1, newNlComment, newEolComment);
//    }
//}
