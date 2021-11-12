package si.isystem.swtbot.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.hamcrest.Description;


/**
 * Matches widgets if the specified text of the widget matches the specified 
 * regular expression.
 * 
 * @author markok;
 */
public class RegExWidgetMatcher<T extends Widget> extends AbstractMatcher<T> {

    /** The text */
    protected Pattern m_regEx;

    private ETextType m_textType;

    public enum ETextType {
        GET_TEXT, GET_TOOLTIP
    }

    /**
     * Constructs this matcher with the given text. Method getText() is called 
     * on the widget to get comparison string.
     * 
     * @param text the text to match on the {@link org.eclipse.swt.widgets.Widget}
     */
    public RegExWidgetMatcher(String text) {
        this(text, ETextType.GET_TEXT);
    }

    /**
     * Constructs this matcher with the given text.
     * 
     * @param regEx the text to match on the {@link org.eclipse.swt.widgets.Widget}
     * @param ignoreCase Determines if this should ignore case during the comparison.
     * @since 1.2
     */
    public RegExWidgetMatcher(String regEx, ETextType textType) {
        
        regEx = regEx.replaceAll("\\r\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        regEx = regEx.replaceAll("\\r", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        m_regEx = Pattern.compile(regEx);
        m_textType = textType;
    }

    
    // FIXME: optimize the if() code block, use strategy or something else.
    protected boolean doMatch(Object obj) {
        try {
            if (obj instanceof Text  ||  obj instanceof Combo  ||  
                    (m_textType == ETextType.GET_TOOLTIP &&  (obj instanceof Control  ||  obj instanceof ToolItem))) {
                
                String text = null;
                switch (m_textType) {
                case GET_TEXT:
                    text = getText(obj);
                    break;
                case GET_TOOLTIP:
                    text = getToolTip(obj);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid text type: " + m_textType);
                }

                // System.out.println(">>> " + obj.getClass().getSimpleName() + ": " + text);
                if (text == null) {
                    return false;
                }

                Matcher matcher = m_regEx.matcher(text);
                return matcher.matches();
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("RegExMatcher: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the text of the object using the getToolTipText() method. If the object doesn't contain a get text method an
     * exception is thrown.
     * 
     * @param obj any object to get the text from.
     * @return the return value of obj#getText()
     * @throws NoSuchMethodException if the method "getToolTipText" does not exist on the object.
     * @throws IllegalAccessException if the java access control does not allow invocation.
     * @throws InvocationTargetException if the method "getText" throws an exception.
     * @see Method#invoke(Object, Object[])
     */
    private static String getToolTip(Object obj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String toolTip = (String) SWTUtils.invokeMethod(obj, "getToolTipText");
        if (toolTip == null) {
            return null;
        }
        return toolTip.replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String getText(Object obj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return ((String) SWTUtils.invokeMethod(obj, "getText")).replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void describeTo(Description description) {
        description.appendText("with reg ex '").appendText(m_regEx.pattern()).appendText("'"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

