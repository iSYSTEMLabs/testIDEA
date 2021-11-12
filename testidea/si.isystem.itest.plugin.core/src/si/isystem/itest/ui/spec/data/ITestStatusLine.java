package si.isystem.itest.ui.spec.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

import si.isystem.itest.common.IconProvider;
import si.isystem.itest.common.IconProvider.EIconId;
import si.isystem.itest.main.Activator;
import si.isystem.itest.ui.spec.StatusView;

/**
 * Uses standard RCP status line, which may not be the best for itest, because
 * it has predefined areas, which do not match itest requirements. For example,
 * Connection status should be presented as text item with colored text and 
 * background on the right side of the status bar. Consider implementing it as
 * as standard View. 
 * The advantage of this implementation is compatibility with Eclipse, because
 * Eclipse has status line sensitive to selected view
 */
public class ITestStatusLine {

    
    public enum StatusImageId {
        DISCONNECTED, CONNECTED
    }

    
    public void setMessage(String msg) {
    
        IStatusLineManager[] mgrs = getStatusLineManagers();
        
        for (IStatusLineManager mgr : mgrs) {
            mgr.setMessage(msg);
        }
    }

    
    public void setMessage(StatusImageId imageId, String msg) {

        IStatusLineManager[] mgrs = getStatusLineManagers();
    
        for (IStatusLineManager mgr : mgrs) {
            switch (imageId) {
            case CONNECTED:
                mgr.setMessage(IconProvider.INSTANCE.getIcon(EIconId.EConnectedToWinIDEA), msg);
                break;
            case DISCONNECTED:
                mgr.setMessage(IconProvider.INSTANCE.getIcon(EIconId.EDisconnectedFromwinIDEA), msg);
                break;
            default:
                mgr.setMessage(null, msg);
            }
        }
        

        StatusView statusView = StatusView.getView();
        if (statusView != null) {
            statusView.setConnectionStatus(imageId);
        }
    }

    
    /**
     * @param msg message to set 
     */
    public void setErrorMessage(String msg) {
        throw new IllegalStateException("setErrorMessage() has not been implemented yet!");
        //getStatusLineManager().setErrorMessage(msg);
    }

    
    /**
     * @param imageId image to set
     * @param msg message to set
     */
    public void setErrorMessage(StatusImageId imageId, String msg) {
        throw new IllegalStateException("setErrorMessage() has not been implemented yet!");
    }

    
    private IStatusLineManager[] getStatusLineManagers() {
        
        List<IStatusLineManager> statusMgrs = new ArrayList<IStatusLineManager>();
        IWorkbenchPage activePage = 
            Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        
/*        IStatusLineManager statusMgr = activePage.findView(TestSpecificationEditorView.ID)
                                       .getViewSite().getActionBars().getStatusLineManager();

        if (statusMgr != null) {
            statusMgrs.add(statusMgr);
        }
  */      
        IViewPart statusView = activePage.findView(StatusView.ID);
        
        if (statusView != null) {
            IStatusLineManager statusMgr = statusView.getViewSite().getActionBars().getStatusLineManager();
        
            if (statusMgr != null) {
                statusMgrs.add(statusMgr);
            }
        }
        
        /* statusMgr = activePage.findView(TestSpecificationTreeView.ID)
                        .getViewSite().getActionBars().getStatusLineManager();
        
        if (statusMgr != null) {
            statusMgrs.add(statusMgr);
        } */
        
        /* IViewReference[] viewRefs = Activator.getDefault().getWorkbench().
        getActiveWorkbenchWindow().getActivePage().getViewReferences();
        IActionBars actionBars = viewRefs[0].getView(false).getViewSite().getActionBars(); */

        return statusMgrs.toArray(new IStatusLineManager[0]);
    }
}
