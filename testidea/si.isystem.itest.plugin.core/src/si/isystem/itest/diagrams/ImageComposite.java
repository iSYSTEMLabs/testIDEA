package si.isystem.itest.diagrams;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;


public class ImageComposite extends ViewerComposite {

    protected static final int BORDER = 10;

    protected static final double MIN_SCALE = 0.001;

    protected static final int BUTTON1 = 1;

    protected static final int PAN_OFFSET = 10;
    
    private double m_scale = 1;

    private Image m_seqDiagImg;

    private FocusableComposite m_imagePanel;

    private ScrolledComposite m_scrolledPanel;
    
    private Point m_dragStartPoint = null;
    private Point m_origin;

    
    /**
     * Used for editors which contain single image.
     * @param parent
     * @return
     */
    @Override
    public ScrolledComposite createComposite(Composite parent) {
        
        m_scrolledPanel = new ScrolledComposite(parent, 
                                                SWT.V_SCROLL | SWT.H_SCROLL);
        // m_scrolledPanel.setLayoutData("wmin 0, hmin 0");
        
        m_imagePanel = new FocusableComposite(m_scrolledPanel, SWT.BORDER);
        
        m_imagePanel.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                GC gc = e.gc;
                // System.out.println("paint image, scale = " + m_scale);
                if (m_seqDiagImg != null) {
                    int w = m_seqDiagImg.getBounds().width;
                    int h = m_seqDiagImg.getBounds().height;
                    gc.drawImage(m_seqDiagImg, 0, 0, w, h,
                                 BORDER, BORDER, 
                                 (int)(w * m_scale) + BORDER, 
                                 (int)(h * m_scale) + BORDER);
                } 
            }});
        
        
        m_imagePanel.addMouseWheelListener(new MouseWheelListener() {
            
            @Override
            public void mouseScrolled(MouseEvent e) {
                if ((e.stateMask & SWT.CTRL) != 0  &&  e.button == 0) {
                    if (e.count > 0) {
                        m_scale *= ViewerComposite.ZOOM_IN_SCALE;
                    } else {
                        m_scale /= ViewerComposite.ZOOM_IN_SCALE;
                    }
                    
                    if (m_scale < MIN_SCALE) {
                        m_scale = MIN_SCALE;
                    }
                    configureScrolledPanel();
                } 
                
                m_imagePanel.redraw();
            }
        });
        
        m_scrolledPanel.addMouseWheelListener(new MouseWheelListener() {
            
            @Override
            public void mouseScrolled(MouseEvent e) {
                if ((e.stateMask & SWT.SHIFT) != 0  &&  e.button == 0) {
                    ScrollBar horizontalBar = m_scrolledPanel.getHorizontalBar();
                    int selection;
                    int increment = horizontalBar.getIncrement(); 
                    if (e.count > 0) {
                        selection = horizontalBar.getSelection() + increment;
                    } else {
                        selection = horizontalBar.getSelection() - increment;
                    }

                    Point origin = m_scrolledPanel.getOrigin();
                    m_scrolledPanel.setOrigin(selection, origin.y);
                }
            }
        });
        

       m_imagePanel.addMouseMoveListener(new MouseMoveListener() {
            
            @Override
            public void mouseMove(MouseEvent e) {
                if (m_dragStartPoint != null) {
                    
                    // Dragging has the problem that coordinate system is moving.
                    // There exist two solutions - calculation of relative offset
                    // and mapping to abs. coordinate system - currently commented.
                    // See also mouseDown() in the listener below.
                    
                    // Display display = Display.getCurrent();
                    //  Point mapped = display.map(m_imagePanel, 
                    //                            m_scrolledPanel, 
                    //                            new Point(e.x, e.y));
                    // int deltaX = m_dragStartPoint.x - mapped.x;
                    // int deltaY = m_dragStartPoint.y - mapped.y;
                    
                    Point origin = m_scrolledPanel.getOrigin();
                    int deltaX = m_dragStartPoint.x - (e.x - origin.x);
                    int deltaY = m_dragStartPoint.y - (e.y - origin.y);
                    
                    m_scrolledPanel.setOrigin(m_origin.x + deltaX, 
                                              m_origin.y + deltaY);
                }
            }
        });
 
        
        m_imagePanel.addMouseListener(new MouseListener() {
            

            @Override
            public void mouseUp(MouseEvent event) {
                if (event.button == BUTTON1) {
                    m_dragStartPoint = null;
                }
            }
            
            
            @Override
            public void mouseDown(MouseEvent event)
            {
                ((Composite)event.widget).setFocus();
                
                if (event.button == BUTTON1) {
                    Point origin = m_scrolledPanel.getOrigin();
                    m_origin = new Point(origin.x, origin.y);
                    m_dragStartPoint = new Point(event.x - origin.x,
                                                 event.y - origin.y);
                    
                    // Dragging has the problem that coordinate system is moving.
                    // There exist two solutions - calculation of relative offset
                    // and mapping to abs. coordinate system - currently commented.
                    // See also mouseMove() in the listener above.
                    
                    // Display display = Display.getCurrent();
                    // Point mapped = display.map(m_imagePanel, 
                    //                            m_scrolledPanel, 
                    //                           new Point(event.x, event.y));
                    //m_dragStartPoint = new Point(mapped.x, mapped.y);
                }
            }
            
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        
        m_imagePanel.addKeyListener(new KeyListener() {
            
            @Override
            public void keyReleased(KeyEvent e) {
                
                boolean isRedrawNeeded = false;
                
                if (Character.toLowerCase(e.character) == ViewerComposite.KEY_CMD_RESET) {
                    m_scrolledPanel.setOrigin(0, 0);
                    m_scale = 1;
                    isRedrawNeeded = true;
                } else if (e.character == ViewerComposite.KEY_CMD_ZOOM_IN) {
                    m_scale *= ViewerComposite.ZOOM_IN_SCALE;
                    isRedrawNeeded = true;
                } else if (e.character == ViewerComposite.KEY_CMD_ZOOM_OUT) {
                    m_scale /= ViewerComposite.ZOOM_IN_SCALE;
                    isRedrawNeeded = true;
                } else if (e.keyCode == SWT.ARROW_LEFT) {
                    Point origin = m_scrolledPanel.getOrigin();
                    m_scrolledPanel.setOrigin(origin.x - getScaledW()/PAN_OFFSET, 
                                              origin.y);
                    isRedrawNeeded = true;
                } else if (e.keyCode == SWT.ARROW_RIGHT) {
                    Point origin = m_scrolledPanel.getOrigin();
                    m_scrolledPanel.setOrigin(origin.x + getScaledW()/PAN_OFFSET, 
                                              origin.y);
                    isRedrawNeeded = true;
                } else if (e.keyCode == SWT.ARROW_UP) {
                    Point origin = m_scrolledPanel.getOrigin();
                    m_scrolledPanel.setOrigin(origin.x, 
                                              origin.y - getScaledH()/PAN_OFFSET);
                    isRedrawNeeded = true;
                } else if (e.keyCode == SWT.ARROW_DOWN) {
                    Point origin = m_scrolledPanel.getOrigin();
                    m_scrolledPanel.setOrigin(origin.x, 
                                              origin.y + getScaledH()/PAN_OFFSET);
                    isRedrawNeeded = true;
                }

                if (isRedrawNeeded) {
                    if (m_scale < MIN_SCALE) {
                        m_scale = MIN_SCALE;
                    }
                    configureScrolledPanel();
                    m_imagePanel.redraw();
                }
            }                
            
            
            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        
        
        m_scrolledPanel.setExpandHorizontal(true);
        m_scrolledPanel.setExpandVertical(true);
        m_scrolledPanel.setContent(m_imagePanel);

        // redundant listener?
        m_scrolledPanel.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                configureScrolledPanel();
            }
        });

        configureScrolledPanel();

        return m_scrolledPanel;
    }
    

    @Override
    public void setFile(File file) {
        m_fileName = file.getAbsolutePath();
        m_isFileUpdated = true;
    }
    

    @Override
    public void openFileInCanvas() {
        if (!m_isFileUpdated) {
            // do not reload image if already loaded, sine this method is called from
            // PaintListener
            return;
        }
        m_isFileUpdated = false;
        
        if (m_seqDiagImg != null) {
            m_seqDiagImg.dispose();
        }
        
        m_seqDiagImg = new Image(Display.getDefault(), m_fileName);

        // if UI component is not created yet, postpone drawing of image 
        if (m_scrolledPanel != null) {
            configureScrolledPanel();
            m_imagePanel.redraw();
        }
    }
    
    
    @Override
    public void clear() {
        dispose();
        m_imagePanel.redraw();
    }
    

    @Override
    public void dispose() {
        if (m_seqDiagImg != null) {
            m_seqDiagImg.dispose();
        }
        m_seqDiagImg = null;
        m_scrolledPanel.dispose();
    }

    
    private void configureScrolledPanel() {
        m_scrolledPanel.setMinSize(getScaledW() + 2 * BORDER, 
                                   getScaledH() + 2 * BORDER);
        m_scrolledPanel.getHorizontalBar().setIncrement(m_scrolledPanel.getSize().x / 10);
        m_scrolledPanel.getVerticalBar().setIncrement(m_scrolledPanel.getSize().y / 10);
    }

    
    private int getScaledW() {
        if (m_seqDiagImg == null) {
            return 100;
        }
        return (int)(m_seqDiagImg.getBounds().width * m_scale);
    }

    
    private int getScaledH() {
        if (m_seqDiagImg == null) {
            return 100;
        }
        return (int)(m_seqDiagImg.getBounds().height * m_scale);
    }
}


/**
 * This class together with mouse listener attached to it above is used to 
 * make composite focusable. If composite in scrollable panel 
 * has no focusable component inside (for example Button), it can not receive
 * Mouse wheel events :-(
 * 
 * @author markok
 *
 */
class FocusableComposite extends Composite {

    FocusableComposite(Composite parent, int style) {
        super(parent, style);
    }
    
    
    @Override
    public boolean setFocus()
    {
        return super.forceFocus();
    }
}
