package si.isystem.itest.diagrams;

import java.io.File;

import net.miginfocom.swt.MigLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import si.isystem.connect.CTestDiagramConfig;
import si.isystem.connect.CTestDiagramConfig.EViewFormat;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.SelectionAdapter;

/**
 * This class defines a composite, which is used to display charts, 
 * diagrams, and other results of external scripts.
 * 
 * All subclasses must support creating of UI component without file name, 
 * and opening of a file (or simply remembering the file name) without UI
 * component.
 *  
 * @author markok
 */
public abstract class ViewerComposite {

    public final static char KEY_CMD_RESET = 'r';
    public final static char KEY_CMD_ZOOM_IN = '+';
    public final static char KEY_CMD_ZOOM_OUT = '-';
    public final static double ZOOM_IN_SCALE = 1.1;
    public final static double ZOOM_OUT_SCALE = 1/ZOOM_IN_SCALE;
    
    public final static String EXT_PNG = "png";
    public final static String EXT_JPG = "jpg";
    public final static String EXT_JPEG = "jpeg";
    public final static String EXT_BMP = "bmp";
    public final static String EXT_SVG = "svg";
    public final static String EXT_TXT = "txt";
    public final static String EXT_CSV = "csv";
            
    
    protected String m_fileName;
    protected boolean m_isFileUpdated;
    private SelectionAdapter m_closeListener;

    
    /**
     * Should return composite which contains only viewer component (image, table, ...)
     * To be used in multipage editors.
     * @param parent
     * @return
     */
    abstract public Composite createComposite(Composite parent);
    
    /** Opens file and shows its contents. */
    abstract public void setFile(File file);
    
    /**
     * Intended to be called later, when dimensions of parent composite are 
     * already known - this way JSVGCanvas can calculate correct view transform. 
     */
    abstract public void openFileInCanvas();
    
    // abstract public void setFocus();
    
    /** Clears viewer area - output is empty. */
    abstract public void clear();
    
    /** Override this method to disposes resources if required. */
    abstract public void dispose();
    
    
    public Composite createCompositeWithFileNameAndCloseButton(Composite parent) {
        Composite mainPanel = new Composite(parent, SWT.NONE);
        mainPanel.setLayoutData("wmin 0, hmin 0");

        //FDGUIBuilder._new().left(0, 0).top(0, 0).right(100, 0).bottom(100, 0).setData(mainPanel);

        String fNameLbl = m_fileName != null ? m_fileName : "";
        
        /* FormLayout layout = new FormLayout();
        mainPanel.setLayout(layout);
        
        Label lbl = new Label(mainPanel, SWT.NONE);
        lbl.setText(fNameLbl);
        FDGUIBuilder._new().left(0, 3).top(0, 3).setData(lbl); */
        MigLayout mig = new MigLayout("fill", 
                                      "[fill][min!]",
                                      "[min!][fill]");
        mainPanel.setLayout(mig);
        
        KGUIBuilder builder = new KGUIBuilder(mainPanel);
        
        builder.label(fNameLbl, "gapleft 8");
    
        Image buttonIcon = IconProvider.INSTANCE.getIcon(EIconId.ECloseTab);
        Label closeLbl = builder.label(buttonIcon, "wrap");
        /*Label closeLbl = new Label(mainPanel, SWT.NONE);
        closeLbl.setImage(buttonIcon);
        FDGUIBuilder._new().top(0, 0).right(100, 0).setData(closeLbl); */
        closeLbl.setToolTipText("Close page");
        
        closeLbl.addMouseListener(new MouseListener() {
            
            private boolean m_isDown;

            @Override
            public void mouseUp(MouseEvent e) {
                if (m_isDown  &&  m_closeListener != null) {
                    // SelectionEvent se = new SelectionEvent(new Event());
                    m_closeListener.widgetSelected(null);
                }
                m_isDown = false;
            }
            
            @Override
            public void mouseDown(MouseEvent e) {
                m_isDown = true;
            }
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {}
        });
        
        
        Composite diagPanel = createComposite(mainPanel);
        // set w min and hmin for scrollers to appear
        diagPanel.setLayoutData("span 2, wmin 0, hmin 0");
        // FDGUIBuilder._new().left(10, 0).top(10, 0).right(100, 0).bottom(100, 0).setData(diagPanel);
        
        return mainPanel;
    }

    
    public String getFileName() {
        return m_fileName;
    }

    
    public void setCloseListener(SelectionAdapter closeListener) {
        m_closeListener = closeListener;
    }
    
    
    /**
     * 
     * @param imgFileName
     * @param diagCfg if null, extension is used to open file with proper viewer.
     * @return
     */
    public static ViewerComposite create(File imgFile,
                                         CTestDiagramConfig diagCfg) {
        
        EViewFormat viewFormat = diagCfg == null ? EViewFormat.EByExtension :
                                                   diagCfg.getViewFormat();
        
        if (viewFormat == EViewFormat.EByExtension) {
            return newCompositeFromFileExtension(imgFile);
        } 
          
        return  newCompositeFromViewFormat(viewFormat);
    }
    
    
    private static ViewerComposite newCompositeFromFileExtension(File file) {
        String fileName = file.toString().toLowerCase();
        
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx < 0) {
            throw new SIllegalArgumentException("Can not get default viewer - "
                    + "extension is not specified.").add("file", fileName);
        }
        
        String ext = fileName.substring(dotIdx + 1);
        switch (ext) {
        case EXT_JPG:
        case EXT_JPEG:
        case EXT_BMP:
        case EXT_PNG:
            return new ImageComposite();
        case EXT_SVG:
            return new SVGComposite();
        case EXT_TXT:
            break; 
        case EXT_CSV:
            break;
        }
        
        throw new SIllegalArgumentException("No viewer is defined for file extension.").
            add("fileName", fileName).
            add("extension", ext);
    }
    
    
    private static ViewerComposite newCompositeFromViewFormat(CTestDiagramConfig.EViewFormat format) {

        switch (format) {
        case EBitmap:
            return new ImageComposite();
        case ESVG:
            return new SVGComposite();
//        case ECSV:
//            break;
//        case EText:
//            break; 
        default:
            // will throw exception below
        }
        
        throw new SIllegalArgumentException("No viewer is defined for the given view format.").
            add("format", format);
    }
}
