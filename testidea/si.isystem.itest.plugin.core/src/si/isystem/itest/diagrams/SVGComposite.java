package si.isystem.itest.diagrams;

import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.io.File;

import javax.swing.JScrollBar;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.JSVGScrollPane;
import org.apache.batik.swing.gvt.InteractorAdapter;
import org.apache.batik.swing.gvt.JGVTComponent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import si.isystem.exceptions.SIOException;

/*
 * JSVGCanvas shortcuts (from javadoc):
 * - Ctrl + LMB drag: zoom selection
 * - Shift + RMB: realtime zoom
 * - Shift + LMB: pan
 * - Ctrl + RMB: rotate
 * - Ctrl + Shift + RMB - reset
 * 
 * Asyst shortcuts:
 * - mouse wheel - scroll up/down
 * - Shift + mouse wheel - pan left/right
 * - Ctrl + mouse wheel - realtime zoom
 * - LMB: pan
 * - r - reset
 * - + - zoom in
 * - - - zoom out
 */

public class SVGComposite extends ViewerComposite {

    private JSVGCanvas m_svgCanvas;
    private Composite m_mainPanel;
    
    // This one is used as a workaround for missing MouseWheelListener and
    // corresponding interactor in JSVGCanvas. It is modified in 
    // MouseWheelListener of scroll pane, and then read in ImageZoomListener
    // where canvas scale is set. The drawback is initial delay until key-press
    // is repeated.
    private MutableInt m_svgCanvasScale = new MutableInt(ImageZoomInteractor.SCALE_FACTOR);
    private java.awt.Frame m_fileTableFrame;
    
    @SuppressWarnings("unchecked")
    @Override
    public Composite createComposite(Composite parent) {
        m_mainPanel = new Composite(parent, SWT.EMBEDDED);
        //mainPanel.setLayout(new GridLayout());
        //mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        // mainPanel.setLayoutData("wmin 0, hmin 0");
        //FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(m_mainPanel);
        m_mainPanel.setLayout(new FillLayout());

        m_fileTableFrame = SWT_AWT.new_Frame(m_mainPanel);
        java.awt.Panel awtPanel = new java.awt.Panel(new java.awt.BorderLayout());
        awtPanel.setLayout(new BorderLayout());
        
        m_svgCanvas = new JSVGCanvas();

        AsystJSVGScrollPane svgScrolPanel = new AsystJSVGScrollPane(m_svgCanvas);
        
        m_svgCanvas.getInteractors().add(new PanInteractor());
        m_svgCanvas.getInteractors().add(new ImageZoomInteractor(m_svgCanvasScale));
        m_svgCanvas.getInteractors().add(new KeyInteractor());
        
        awtPanel.add(svgScrolPanel);
        
        m_fileTableFrame.add(awtPanel);
        m_fileTableFrame.validate();
        m_fileTableFrame.pack();

        return m_mainPanel;
    }

    
    @Override
    public void setFile(File file) {
        m_fileName = file.toString();
        m_isFileUpdated = true;
    }


    @Override
    public void openFileInCanvas() {
        try {
            if (!m_isFileUpdated) {
                // do not reload image if already loaded, sine this method is called from
                // PaintListener
                return;
            }
            m_isFileUpdated = false;
            
            // if setURI() is not called in separate thread, then testIDEA
            // sometimes freezes in EventQueue$1AWTInvocationLock
            Job job = new Job("SVG Job") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    String uri;
                    try {
                        uri = new File(m_fileName).toURI().toURL().toString();
                        m_svgCanvas.setURI(uri);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        } catch (Exception ex) {
            throw new SIOException("Can not open diagram in SVG format: " + m_fileName,
                                   ex);
        }
    }
    

    // these two methods will be used if view will save transform, so that when
    // user returns to testCase, diagrams will be shown the same way as he left it -
    // currently they are redrawn, which means fit to view. TestIDEA editor view should
    // read view settings before switching to other test case, and restore it
    // when switching back.
    public void setRenderingTransform(AffineTransform rt) {
        m_svgCanvas.setRenderingTransform(rt, true);
    }
    public AffineTransform getRenderingTransform() {
        return m_svgCanvas.getRenderingTransform();
    }
    
    
//    public void setFocus() {
//        // if this method is not called, then user has to click in canvas first,
//        // otherwise shortcut keys do not work, and zoom with mouse wheel does 
//        // not redraw image after zooming (ire remains of low quality).   
//        m_svgCanvas.requestFocusInWindow();
//    }
    
    
    @Override
    public void clear() {
    }
    
    
    @Override
    public void dispose() {
        m_svgCanvas.dispose();  // Memory and handle leak, if this one is not disposed!!! 
        // m_fileTableFrame.dispose(); // freezes when closing single editor page
                                       //if this one is disposed
        m_mainPanel.dispose();
    }
}


/**
 * When 'R' is pressed image transform is reset, '+' and '-' are used for zooming
 * in and out.
 */
class KeyInteractor extends InteractorAdapter {
    
    @Override
    public boolean startInteraction(InputEvent ie) {
        boolean isActive = isKeyCmdPressed(ie);
        // System.out.println("start key: " + isActive + " " + ie.toString());
        return isActive;
    }

    
    @Override
    public boolean endInteraction() {
        return true;
    }
    
    
    @Override
    public void keyTyped(KeyEvent ke) {
        if (Character.toLowerCase(ke.getKeyChar()) == ViewerComposite.KEY_CMD_RESET) {
            JGVTComponent comp = (JGVTComponent)ke.getSource();

            AffineTransform at = AffineTransform.getTranslateInstance(0, 0);
            comp.setRenderingTransform(at);
        } else if (Character.toLowerCase(ke.getKeyChar()) == ViewerComposite.KEY_CMD_ZOOM_IN) {
            scale(ke, ViewerComposite.ZOOM_IN_SCALE);
        } else if (Character.toLowerCase(ke.getKeyChar()) == ViewerComposite.KEY_CMD_ZOOM_OUT) {
            scale(ke, ViewerComposite.ZOOM_OUT_SCALE);
        } 
    }


    private void scale(KeyEvent ke, double scale) {
        JGVTComponent comp = (JGVTComponent)ke.getSource();

        int middleX = comp.getWidth() / 2;
        int middleY = comp.getHeight() / 2;
        
        double dx = middleX - middleX * scale;
        double dy = middleY - middleY * scale;
        
        AffineTransform at = (AffineTransform) comp.getRenderingTransform().clone();
        
        // move image so that it scales around position of mouse cursor
        at.translate(dx, dy);
        at.scale(scale, scale);
        
        comp.setRenderingTransform(at);
    }
    

    protected boolean isKeyCmdPressed(InputEvent ie) {
        if (ie instanceof KeyEvent) {
            KeyEvent me = (KeyEvent) ie;
            char keyChar = me.getKeyChar();
            return me.getID() == KeyEvent.KEY_TYPED
                    && (Character.toLowerCase(keyChar) == ViewerComposite.KEY_CMD_RESET
                        || keyChar == ViewerComposite.KEY_CMD_ZOOM_IN  
                        || keyChar == ViewerComposite.KEY_CMD_ZOOM_OUT);
        }
        return false;
    }
}


/**
 * This interactor works together with zooming in AsystJSVGScrollPane - the pane
 * performs fast real-time zoom, while this interactor redraws the image with
 * fine resolution when user releases CTRL key. 
 * 
 * @author markok
 *
 */
class ImageZoomInteractor extends InteractorAdapter {
    
    private boolean m_isActive = false;
    private JGVTComponent m_canvas;
    private MutableInt m_svgCanvasScale; 

    public final static int SCALE_FACTOR = 20;

    public ImageZoomInteractor(MutableInt svgCanvasScale) {
        m_svgCanvasScale = svgCanvasScale;
    }

    
    @Override
    public boolean startInteraction(InputEvent ie) {
        m_canvas = (JGVTComponent)ie.getSource();
        m_isActive = isCtrlPressed(ie);
        // System.out.println("start ie: " + m_isActive + " " + ie.toString());
        return m_isActive;
    }

    
    @Override
    public boolean endInteraction() {
        return !m_isActive;
    }
    
    
    @Override
    public void keyPressed(KeyEvent ke) {
        m_isActive = isCtrlPressed(ke);
    }
    

    @Override
    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
            m_isActive = false;
            
            // after quick zooming is finished, repaint the image to get 
            // qood quality (setRenderingTransform() vs. setPaintingTransform())
            AffineTransform pt = m_canvas.getPaintingTransform();

            AffineTransform rt = (AffineTransform)m_canvas.getRenderingTransform().clone();
            if (pt != null) {
                rt.preConcatenate(pt);
                m_canvas.setRenderingTransform(rt);
            }
            
            m_svgCanvasScale.setValue(SCALE_FACTOR);
        };
        // System.out.println("ker = " + m_isActive + " / " + ke);
    }

    
    protected boolean isCtrlPressed(InputEvent ie) {
        if (ie instanceof KeyEvent) {
            KeyEvent me = (KeyEvent) ie;
            return me.getID() == KeyEvent.KEY_PRESSED
                    && me.getKeyCode() == KeyEvent.VK_CONTROL;
        }
        return false;
    }
}


/**
 * Pans image on mouse drag.
 * 
 * @author markok
 */
class PanInteractor extends InteractorAdapter {

    private int m_xStart;
    private int m_yStart;
    private boolean m_isActive = false; 


    @Override
    public void mousePressed(MouseEvent ie) {
        //System.out.println("lmb pressed");
        m_isActive = true;
        m_xStart = ie.getX();
        m_yStart = ie.getY();
    }

    
    @Override
    public void  mouseDragged(MouseEvent e) {
        //System.out.println("lmb dragged");

        AffineTransform at;
        JGVTComponent c = (JGVTComponent)e.getSource();
        int dx = e.getX() - m_xStart;
        int dy = e.getY() - m_yStart;
        at = AffineTransform.getTranslateInstance(0, 0);
        
        at.translate(dx, dy);
        
        c.setPaintingTransform(at);
    }

    
    @Override
    public void mouseReleased(MouseEvent e) {
        
        //System.out.println("released");
        m_isActive = false;

        JGVTComponent c = (JGVTComponent)e.getSource();
        
        AffineTransform pt = c.getPaintingTransform();

        if (pt != null) {
            AffineTransform rt = (AffineTransform)c.getRenderingTransform().clone();
            rt.preConcatenate(pt);
            c.setRenderingTransform(rt);
        }
    }
    
    
    @Override
    public boolean startInteraction(InputEvent ie) {
        m_isActive = isClick(ie);
        //System.out.println("start ie: " + m_isActive + " " + ie.toString());
        return m_isActive;
    }

    
    @Override
    public boolean endInteraction() {
        //System.out.println("end " + !m_isActive);
        return !m_isActive;
    }
    
    
    protected boolean isClick(InputEvent ie) {
        if (ie instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) ie;
            return me.getID() == MouseEvent.MOUSE_PRESSED
                    && me.getButton() == MouseEvent.BUTTON1;
        }
        return false;
    }
}


/*
 * Centers image, currently not used.
 
class CenterInteractor extends InteractorAdapter {

    protected boolean isDoubleClick(InputEvent ie) {
        if (ie instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) ie;
            return me.getID() == MouseEvent.MOUSE_CLICKED
                    && me.getButton() == MouseEvent.BUTTON1
                    && me.getClickCount() == 2;
        }
        return false;
    }

    
    public boolean startInteraction(InputEvent ie) {
        return isDoubleClick(ie);
    }

    
    public boolean endInteraction() {
        return true;
    }
    
    
    public void mouseClicked(MouseEvent me) {
        if (isDoubleClick(me)) {
            JSVGCanvas c = (JSVGCanvas) me.getSource();
            int cx = c.getWidth() / 2;
            int cy = c.getHeight() / 2;
            int x = me.getX();
            int y = me.getY();
            AffineTransform at =
                AffineTransform.getTranslateInstance(cx - x, cy - y);
            AffineTransform rt =
                (AffineTransform) c.getRenderingTransform().clone();
            rt.preConcatenate(at);
            c.setRenderingTransform(rt);
        }
    }
}
*/


/**
 * Extends JSVGScrollPane to get access to scrollbar for mouse wheel listener.
 * In JSVGScrollPane it is unfortunately commented out, and this one also
 * sets scale factor m_svgCanvasScale.
 * 
 * @author markok
 */
@SuppressWarnings("serial")
class AsystJSVGScrollPane extends JSVGScrollPane {

    public  AsystJSVGScrollPane(JSVGCanvas canvas) {
        super(canvas);
        
        addMouseWheelListener(new WheelListener());
    }

    
    /**
     * This method performs fast real-time zoom, while ImageZoomInteractor 
     * performs fine redraw when user releases CTRL key.
     * 
     * @param unitsToScroll
     * @param clickedX
     * @param clickedY
     */
    private void scaleImage(int unitsToScroll, int clickedX, int clickedY) {

        double scale = 0; //m_svgCanvasScale.intValue() / (double)ImageZoomInteractor.SCALE_FACTOR;
        if (unitsToScroll < 0) {
            scale = ViewerComposite.ZOOM_IN_SCALE;
        } else {
            scale = ViewerComposite.ZOOM_OUT_SCALE;
        }
        
        /* int scale = m_svgCanvasScale.intValue() - unitsToScroll;
        scale = Math.max(1, scale);
        m_svgCanvasScale.setValue(scale); */
        
        double dx = clickedX - clickedX * scale;
        double dy = clickedY - clickedY * scale;
        
        AffineTransform at = null;
        AffineTransform pt = canvas.getPaintingTransform();

        if (pt == null) {
            at = AffineTransform.getTranslateInstance(0, 0);
        } else {
            at = (AffineTransform) pt.clone();
        }

        // move image so that it scales around position of mouse cursor
        at.translate(dx, dy);
        at.scale(scale, scale);
        
        canvas.setPaintingTransform(at);
    }


/*    public static void debugCanvasInfo(String caller, JSVGCanvas canvas) {
        AffineTransform it = canvas.getInitialTransform();

        System.out.println("\n\n--- " + caller + " / Thread: " + Thread.currentThread().hashCode()
                           + " / " + Thread.currentThread().getName());
        System.out.println("dim: " + canvas.getWidth() + ", " + canvas.getHeight());
        
        if (it != null) {
            System.out.println("init scale: " + it.getScaleX() + ", " + it.getScaleY() + 
                               ", " + it.getTranslateX() + ", " + it.getTranslateY());
        }
        
        AffineTransform vbt = canvas.getViewBoxTransform();
        AffineTransform vt = canvas.getViewingTransform();
        if (vbt != null) {
            System.out.println("view box: " + vbt.getScaleX() + ", " + vbt.getScaleY() + 
                               ", " + vbt.getTranslateX() + ", " + vbt.getTranslateY());
        }
        
        if (vt != null) {
            System.out.println("viewT: " + vt.getScaleX() + ", " + vt.getScaleY() + 
                               ", " + vt.getTranslateX() + ", " + vt.getTranslateY());
        }
    }
  */  
    
    /**
     * This class was copied from JSVGScrollPane sources, since it is commented
     * there (because of compatibility with Java 1.3). See http://grepcode.com/
     * for quick source access.
     *  
     * @author markok
     *
     */
    protected class WheelListener implements MouseWheelListener
    {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            int modifiers = e.getModifiers();
            // System.out.println("x, y = " + e.getX() + ", " + e.getY());
            if ((modifiers & MouseEvent.CTRL_MASK) != 0) {
                if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL  ||
                        e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {

                    scaleImage(e.getUnitsToScroll(), e.getX(), e.getY());
                } 
                return; // CTRL should zoom, not pan 
            }
            
            JScrollBar sb = null;
            if ((modifiers & MouseEvent.SHIFT_MASK) != 0) {
                sb = horizontal;
            } else {
                sb = (vertical.isVisible()) ?
                        vertical : horizontal;        // vertical is preferred
            }
            
            if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                final int amt = e.getUnitsToScroll() * sb.getUnitIncrement();
                sb.setValue(sb.getValue() + amt);
            } else if(e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                final int amt = e.getWheelRotation() * sb.getBlockIncrement();
                sb.setValue(sb.getValue() + amt);
            }
        }
    }
}


