package si.isystem.commons.utils;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import si.isystem.ui.utils.SelectionAdapter;

public class ISysSwtUtils 
{
    public static Label createLabel(Composite parent, String text) {
        return createLabel(parent, text, SWT.NONE);
    }
    
    public static Label createLabel(Composite parent, String text, int style) {
        Label l = new Label(parent, style);
        l.setText(text);
        return l;
    }
    
    public static Text createTextField(Composite parent, String text, boolean editable) {
        return createTextField(parent, text, editable, SWT.NONE);
    }
    
    public static Text createTextField(Composite parent, String text, boolean editable, int style) {
        return createTextField(parent, text, editable, style, null);
    }
    
    public static Text createTextField(Composite parent, String text, boolean editable, int style, ModifyListener modListener) {
        Text f = new Text(parent, style);
        f.setText(text);
        f.setEditable(editable);
        if (modListener != null) {
            f.addModifyListener(modListener);
        }
        return f;
    }

    public static Combo createCombo(Composite main, String[] items, int style) {
        return createCombo(main, items, style, 0, null);
    }

    public static Combo createCombo(Composite main, String[] items, int style, int selectedIndex, SelectionListener listener) {
        Combo c = new Combo(main, style);
        c.setItems(items);
        c.select(selectedIndex);
        if (listener != null) {
            c.addSelectionListener(listener);
        }
        return c;
    }
    
    public static Button createButton(Composite parent, String text, SelectionListener selectionAdapter) {
        return createButtonInternal(parent, text, null, SWT.PUSH, selectionAdapter);
    }
    
    public static Button createButton(Composite parent, Image image, SelectionListener selectionAdapter) {
        return createButtonInternal(parent, null, image, SWT.PUSH, selectionAdapter);
    }
    
    public static Button createButton(Composite parent, String text, int style, SelectionListener selectionAdapter) {
        return createButtonInternal(parent, text, null, style, selectionAdapter);
    }
    
    public static Button createButton(Composite parent, Image image, int style, SelectionListener selectionAdapter) {
        return createButtonInternal(parent, null, image, style, selectionAdapter);
    }

    private static Button createButtonInternal(Composite parent, String text, Image image, int style, SelectionListener selectionAdapter) {
        Button b = new Button(parent, style);
        if (image != null) {
            b.setImage(image);
        }
        else {
            b.setText(text);
        }
        if (selectionAdapter != null) {
            b.addSelectionListener(selectionAdapter);
        }
        return b;
    }
    
    public static Spinner createSpinner(Composite parent, int min, int max, int initialValue, int style) {
        return createSpinner(parent, min, max, initialValue, style, null);
    }
    
    public static Spinner createSpinner(Composite parent, int min, int max, int initialValue, int style, ModifyListener modifyListener) {
        Spinner s = new Spinner(parent, style);
        s.setMinimum(min);
        s.setMaximum(max);
        s.setSelection(initialValue);
        s.setIncrement(1);
        s.addModifyListener(modifyListener);
        return s;
    }

    public static Label createSeparator(Composite parent, int style) {
        return new Label(parent, SWT.SEPARATOR | style);
    }
    
    public static TableViewer createTable(Composite parent, int style, IStructuredContentProvider cp) {
        TableViewer table = new TableViewer(parent, style);
        table.setContentProvider(cp);
        table.setInput("");
        return table;
    }
    
    public static Table createTable(Composite parent, int style, String[] strs) {
        Table table = new Table(parent, style);
        for (String str : strs) {
            new TableItem(table, SWT.NONE).setText(str);
        }
        return table;
    }
    
    public static Group createGroup(Composite parent, String text, int style) {
        Group g = new Group(parent, style);
        g.setText(text);
        return g;
    }

    public static MenuItem createMenuItem(
            Menu menu, int style, String title, boolean enabled, 
            final Runnable runnable) {
        MenuItem item = createMenuItem(menu, style, title, enabled);
        if (enabled  &&  runnable != null) {
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    runnable.run();
                }
            });
        }
        return item;
    }
    
    public static MenuItem createMenuItem(Menu menu, int style, String title, boolean enabled) {
        MenuItem item = new MenuItem(menu, style);
        item.setText(title);
        item.setEnabled(enabled);
        return item;
    }
    
    public static MenuItem createSeparator(Menu menu) {
        int itemCount = menu.getItemCount();
        if (itemCount == 0) {
            return null;
        }
        
        MenuItem lastItem = menu.getItem(itemCount-1);
        if ((lastItem.getStyle() & SWT.SEPARATOR) == 0) {
            return new MenuItem(menu, SWT.SEPARATOR);
        }
        else {
            return null;
        }
    }
}
