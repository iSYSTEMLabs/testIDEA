package si.isystem.swtbot.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuFinder;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;

//MK: This class can be used for submenus of context menus. See FirstContextMenuFinder 
//    for link to source.

/**
 * A context menu wrapper to handle click correctly.
 * @author mchauvin
 * @author jbangerter
 */
public class SWTBotContextMenu {

    
    private Control m_control;
    
    private SWTBotTreeItem treeItem;
    
    /**
     * .
     * @param treeItem .
     */
    public SWTBotContextMenu(final SWTBotTreeItem treeItem) {
        this.treeItem = treeItem;
        this.m_control = treeItem.widget.getParent();
    }
    
    /**
     * .
     * @param tree .
     */
    public SWTBotContextMenu(final SWTBotTree tree) {
        this.m_control = tree.widget;
    }
    
    /**
     * Click on the first menu item matching the text.
     * 
     * @param text the text on the context menu.
     * @return the context menu
     */
    @SuppressWarnings("unchecked")
    // varargs and generics doesn't mix well!
    public SWTBotContextMenu click(final String text) {
        showContextMenu(text);
        
        Matcher<MenuItem> withMnemonic = WidgetMatcherFactory.withRegex(text);
        final Matcher<MenuItem> matcher =
            WidgetMatcherFactory.allOf(WidgetMatcherFactory.widgetOfType(MenuItem.class),
                                       withMnemonic);
        final ContextMenuFinder menuFinder = new FirstContextMenuFinder(m_control);
        
        final List<MenuItem> items = new ArrayList<MenuItem>();

        new SWTBot().waitUntil(new DefaultCondition() {
            public String getFailureMessage() {
                return "Could not find context menu with text: " + text; //$NON-NLS-1$
            }

            public boolean test() throws Exception {
                items.addAll(menuFinder.findMenus(matcher));
                return !items.isEmpty();
            }
        });
        
        MenuItem menuItem = items.get(0);
        if (!menuItem.isDisposed()) {
             new SWTBotMenu(menuItem, matcher).click();
        } else {
            throw new WidgetNotFoundException("Context menu with text: '" + text + "' was disposed!"); //$NON-NLS-1$
        }
        return this;
    }

    /**
     * This helps to ensure that the context menu for this tree or
     * tree item is available when it is sought.
     * 
     * @param text the text on the context menu option
     */
    private void showContextMenu(final String text) {
        if(treeItem != null){
            try {
                treeItem.contextMenu(text);
            }
            catch (SWTException e){
                //The whole point of this code is to circumvent the
                //exception caused by calling contextMenu(String).
                //This code invokes some private methods within
                //treeItem that help in finding context menus.
            }
        }
    }   
}