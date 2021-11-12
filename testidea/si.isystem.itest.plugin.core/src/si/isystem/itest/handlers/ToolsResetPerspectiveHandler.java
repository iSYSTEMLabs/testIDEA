package si.isystem.itest.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

public class ToolsResetPerspectiveHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().resetPerspective();
        removeCoolbarButtonsFromEclipsePlugins();
        
        return null;
    }
    
    
    public static void removeCoolbarButtonsFromEclipsePlugins() {
        // For alternative options to customize perspective see also:
        // http://wiki.eclipse.org/Product_Customization
        // http://www.eclipsezone.com/eclipse/forums/t112106.html
        // use Alt+Shift+F2 to get Menu Spy and click the unwanted button.
        String [] actionSetsToRemoveFromCoolbar = {"org.eclipse.search.searchActionSet",        
                                                   "org.eclipse.mylyn.tasks.ui.navigation",
                                                   "org.eclipse.egit.ui.navigation",
                                                   "org.eclipse.ui.edit.text.actionSet.navigation",
                                                   "org.eclipse.ui.externaltools.ExternalToolsSet",
                                                   "org.eclipse.ui.navigate.next",
                                                   "org.eclipse.ui.edit.text.actionSet.annotationNavigation"};

        for (String actionSet : actionSetsToRemoveFromCoolbar) {
            
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().
                                      getActivePage().hideActionSet(actionSet);
        }
    }
}
