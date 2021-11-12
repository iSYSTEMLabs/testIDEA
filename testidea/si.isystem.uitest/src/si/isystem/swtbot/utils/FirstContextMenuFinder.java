package si.isystem.swtbot.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuFinder;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.ListResult;
import org.hamcrest.Matcher;


/* MK: This class can be used for submenus of context menus.
   Another solution:
   
   public static SWTBotMenu getSubMenuItem(final SWTBotMenu parentMenu, 
final String itemText)
      throws WidgetNotFoundException {
 
  MenuItem menuItem = UIThreadRunnable.syncExec(new WidgetResult<MenuItem>() {
    public MenuItem run() {
        Menu bar = parentMenu.widget.getMenu();
            if (bar != null) {
            for (MenuItem item : bar.getItems()) {
                if (item.getText().equals(itemText)) {
                    return item;
                }
            }
        }
        return null;
    }
  });
 
  if (menuItem == null) {
    throw new WidgetNotFoundException("MenuItem \"" + itemText + "\" not found.");
  } else {
    return new SWTBotMenu(menuItem);
  }
}


Usage:
  WizardTestUtil.getSubMenuItem(xmlNode.contextMenu("New"), "&Other...  Ctrl+N").click();

from:

http://www.prait.ch/wordpress/?p=218

Another link:

http://www.eclipse.org/forums/index.php/mv/tree/167465/#page_top


/**
 * A context menu finder, which stops to search when he found one result.
 * @author mchauvin
 * 
 * See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=338555
 * 
 * Mariot Chauvin 2011-03-01 11:28:30 EST
 *
 * Here are some code I want to share.
 * The code allows one to click on a Tree item context menu, and avoid the common
 * "widget is disposed" error.
 * Code license is EPL.
 */
public class FirstContextMenuFinder extends ContextMenuFinder {

    /**
     * Constructs the context menu finder for the given control to be searched.
     *
     * @param control the control that has a context menu.
     */
    public FirstContextMenuFinder(Control control) {
        super(control);
    }

    
    /**
     * Finds all the menus using the given matcher in the set of shells provided. If recursive is set, it will attempt
     * to find the controls recursively in each of the menus it that is found.
     *
     * @param shells the shells to probe for menus.
     * @param matcher the matcher that can match menus and menu items.
     * @param recursive if set to true, will find sub-menus as well.
     * @return all menus in the specified shells that match the matcher.
     */
    public List<MenuItem> findMenus(Shell[] shells, Matcher<MenuItem> matcher, boolean recursive) {
        LinkedHashSet<MenuItem> result = new LinkedHashSet<MenuItem>();
        for (Shell shell : shells)  {
            result.addAll(findMenus(shell, matcher, recursive));
            if (!result.isEmpty())
                break;
        }   
        return new ArrayList<MenuItem>(result);
    }

    
    
    /**
     * {@inheritDoc}
     * @see org.eclipse.swtbot.swt.finder.finders.MenuFinder#findMenus(org.eclipse.swt.widgets.Menu, org.hamcrest.Matcher, boolean)
     */
    public List<MenuItem> findMenus(final Menu bar, final Matcher<MenuItem> matcher, final boolean recursive) {
        return UIThreadRunnable.syncExec(display, new ListResult<MenuItem>() {
            public List<MenuItem> run() {
                return findMenuInternal(bar, matcher, recursive);
            }
        });
    }
    
    /**
     * @param bar
     * @param matcher
     * @param recursive
     * @return
     */
    private List<MenuItem> findMenuInternal(final Menu bar, final Matcher<MenuItem> matcher, final boolean recursive) {
        LinkedHashSet<MenuItem> result = new LinkedHashSet<MenuItem>();
        if (bar != null) {
            bar.notifyListeners(SWT.Show, new Event());
            MenuItem[] items = bar.getItems();
            for (MenuItem menuItem : items) {
                // System.out.println("MMMM " + menuItem.getText());
                if (isSeparator(menuItem)) {
                    continue;
                }
                if (matcher.matches(menuItem)) {
                    result.add(menuItem);
                    /* we found one, do not continue*/
                    break;
                }
                if (recursive) {
                    result.addAll(findMenuInternal(menuItem.getMenu(), matcher, recursive));
                    if (!result.isEmpty())
                        break;
                }
                    
            }
            bar.notifyListeners(SWT.Hide, new Event());
        }
        return new ArrayList<MenuItem>(result);
    }

    private boolean isSeparator(MenuItem menuItem) {
        return (menuItem.getStyle() & SWT.SEPARATOR) != 0;
    }
    
}


