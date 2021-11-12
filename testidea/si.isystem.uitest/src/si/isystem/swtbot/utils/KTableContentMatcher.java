package si.isystem.swtbot.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.hamcrest.Description;

import de.kupzog.ktable.KTable;


/**
 * Matches KTable if the text in cell at (x, y) matches the specified 
 * regular expression.
 * 
 * @author markok;
 */
public class KTableContentMatcher<T extends Widget> extends AbstractMatcher<T> {

    /** The text */
    protected Pattern m_regEx;

    private ETextType m_textType;

    private int m_col;
    private int m_row;

    public enum ETextType {
        GET_TEXT, GET_TOOLTIP
    }

    /**
     * Constructs this matcher with the given text. Method getContentAt(col, row)
     * is called on the ktable to get comparison string.
     * 
     * @param col table column, left column has index 0
     * @param row table column, top row has index 0
     * @param regEx the reg ex to match on the {@link org.eclipse.swt.widgets.Widget}
     */
    public KTableContentMatcher(int col, int row, String regEx) {
        this(col, row, regEx, ETextType.GET_TEXT);
    }

    /**
     * Constructs this matcher with the given text. Method getContentAt(col, row)
     * is called on the ktable to get comparison string.
     * 
     * @param col table column, left column has index 0
     * @param col table column, top row has index 0
     * @param regEx the reg ex to match on the {@link org.eclipse.swt.widgets.Widget}
     */
    public KTableContentMatcher(int col, int row, String regEx, ETextType textType) {
        
        m_col = col;
        m_row = row;
        regEx = regEx.replaceAll("\\r\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        regEx = regEx.replaceAll("\\r", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        m_regEx = Pattern.compile(regEx);
        m_textType = textType;
    }

    
    protected boolean doMatch(Object obj) {
        try {
            if (obj instanceof KTable) {
                KTable ktable = (KTable)obj;
                String text = null;
                switch (m_textType) {
                case GET_TEXT:
                    text = getContentAt(ktable, m_col, m_row);
                    break;
                case GET_TOOLTIP:
                    text = getToolTip(obj);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid text type: " + m_textType);
                }

                // System.out.println(">>> " + obj.getClass().getSimpleName() + ": " + text);

                Matcher matcher = m_regEx.matcher(text);
                return matcher.matches();
            }
            
            return false;
            
        } catch (Exception e) {
            // do nothing
        }
        return false;
    }

    
    /**
     * Gets the text of the object using the getToolTipText() method. If the 
     * object doesn't contain a get text method an exception is thrown.
     * 
     * @param obj any object to get the text from.
     * @return the return value of obj#getText()
     * @throws NoSuchMethodException if the method "getToolTipText" does not exist on the object.
     * @throws IllegalAccessException if the java access control does not allow invocation.
     * @throws InvocationTargetException if the method "getText" throws an exception.
     * @see Method#invoke(Object, Object[])
     */
    private static String getToolTip(Object obj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return ((String) SWTUtils.invokeMethod(obj, "getToolTipText")).replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    
    private String getContentAt(final KTable ktable, final int col, final int row) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        
        Object result = UIThreadRunnable.syncExec(ktable.getDisplay(), new Result<Object>() {
            public Object run() {
                try {
                    return ktable.getModel().getContentAt(col, row);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        });
        
        return ((String)result).replaceAll(Text.DELIMITER, "\n").trim();
        
//        return ((String) invokeMethod(obj, 
//                                      "getContentAt", 
//                                      new Object[]{Integer.valueOf(col),
//                                                   Integer.valueOf(row)}))
//                .replaceAll(Text.DELIMITER, "\n").trim(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    
    /**
     * Invokes the specified methodName on the object, and returns the result, or <code>null</code> if the method
     * returns void.
     * 
     * @param object the object
     * @param methodName the method name
     * @return the result of invoking the method on the object
     * @throws NoSuchMethodException if the method methodName does not exist.
     * @throws IllegalAccessException if the java access control does not allow invocation.
     * @throws InvocationTargetException if the method methodName throws an exception.
     * @see Method#invoke(Object, Object[])
     * @since 1.0
     */
    private Object invokeMethod(final Object object, 
                                String methodName,
                                final Object [] params) 
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        
        int idx = 0;
        Class<?>[] paramTypes = new Class[params.length];
        for (Object param : params) {
            paramTypes[idx++] = param.getClass();
        }
        
        final Method method = object.getClass().getMethod(methodName, paramTypes);
        Widget widget = null;
        final Object result;
        if (object instanceof Widget) {
            widget = (Widget) object;
            result = UIThreadRunnable.syncExec(widget.getDisplay(), new Result<Object>() {
                public Object run() {
                    try {
                        return method.invoke(object, params);
                    } catch (Exception niceTry) {
                    }
                    return null;
                }
            });
        } else
            result = method.invoke(object, new Object[0]);

        return result;
    }


    public void describeTo(Description description) {
        description.appendText("with reg ex '").appendText(m_regEx.pattern()).appendText("'"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

