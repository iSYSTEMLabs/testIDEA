package si.isystem.commons.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;

import si.isystem.commons.lambda.IIndexedGetter;
import si.isystem.commons.lambda.ISysNamedIndexedGetter;

public class ISysLogUtil {
    
    private static String[] UNITS = new String[] {"ns", "us", "ms", "s", "min",  "h", "d", "w"};
    private static long[] UNIT_DIVS = new long[] {1000, 1000, 1000,  60,    60,   24,     7, 100000};

    /**
     * Pretty time format.
     * @param t
     * @return
     */
    public static String timeToStr(long t) {
        boolean isNeg = t < 0;
        if (isNeg) {
            t = -t;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < UNITS.length && t > 0; i++) {
            long unit = UNIT_DIVS[i];
            long val = t % unit;
            if (val != 0) {
                sb.insert(0, String.format("%d%s ", val, UNITS[i]));
            }
            t = t/unit;
        }
        
        if (sb.length() > 0) {
            return String.format("%s%s", (isNeg ? "-" : ""), sb.substring(0, sb.length()-1));
        }
        else {
            return "0s";
        }
    }
    
    public static String toString(long[] a, String format) {
        if (a == null) {
            return "null";
        }
        if (a.length == 0) {
            return "[]";
        }
        
        StringBuilder b = new StringBuilder();
        b.append("[").append(String.format(format, a[0]));
        for (int i = 1; i < a.length; i++) {
            b.append(", ").append(String.format(format, a[i]));
        }
        b.append("]");
        return b.toString();
    }
    
    public static String toString(int[] a, String format) {
        if (a == null) {
            return "null";
        }
        if (a.length == 0) {
            return "[]";
        }
        
        StringBuilder b = new StringBuilder();
        b.append("[").append(String.format(format, a[0]));
        for (int i = 1; i < a.length; i++) {
            b.append(", ").append(String.format(format, a[i]));
        }
        b.append("]");
        return b.toString();
    }
    
    public static String toString(boolean[] a, String format) {
        if (a == null) {
            return "null";
        }
        if (a.length == 0) {
            return "[]";
        }
        
        StringBuilder b = new StringBuilder();
        b.append("[").append(String.format(format, a[0]));
        for (int i = 1; i < a.length; i++) {
            b.append(", ").append(String.format(format, a[i]));
        }
        b.append("]");
        return b.toString();
    }
    
    public static String toString(Object[] a, String format) {
        if (a == null) {
            return "null";
        }
        if (a.length == 0) {
            return "[]";
        }
        
        StringBuilder b = new StringBuilder();
        b.append("[").append(String.format(format, a[0]));
        for (int i = 1; i < a.length; i++) {
            b.append(", ").append(String.format(format, a[i]));
        }
        b.append("]");
        return b.toString();
    }
    
    public static String floatFormat(double value, int decimalCount) {
        String format = String.format("%%.%df", decimalCount);
        return String.format(format, value);
    }
    
    public static int maxStrLen(Object... objs) {
        int res = 0;
        for (Object obj : objs) {
            if (obj != null) {
                res = Math.max(res, obj.toString().length());
            }
        }
        return res;
    }
    
    public static int maxStrLen(List<Object> values) {
        int res = 0;
        for (Object v : values) {
            if (v != null) {
                res = Math.max(res, v.toString().length());
            }
        }
        return res;
    }
    
    @SuppressWarnings("rawtypes")
    public static String toString(int idx1, int idx2, ISysNamedIndexedGetter... getters) {
        // Initialize buffers
        int titleLength = maxStrLen((Object[]) getters);
        final String titleFormat = String.format("%%%ds:", titleLength);
        List<StringBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < getters.length; i++) {
            buffers.add(new StringBuffer(String.format(titleFormat, getters[i])));
        }
        
        // Values
        List<Object> values = new ArrayList<>();
        for (int i = idx1; i <= idx2; i++) {
            values.clear();
            for (ISysNamedIndexedGetter g : getters) {
                values.add(g.get(i));
            }
            int charCount = maxStrLen(values);
            final String format = (i == idx1 ? " " : ", ") + String.format("%%%ds", charCount);
            for (int vi = 0; vi < values.size(); vi++) {
                buffers.get(vi).append(String.format(format, values.get(vi)));
            }
        }
        
        // Glue together
        StringBuffer res = new StringBuffer();
        for (StringBuffer b : buffers) {
            res.append(b).append('\n');
        }
        
        return res.toString();
    }
    
    public static String toString(int idx1, int idx2, String format, IIndexedGetter<Object[]> getter) {
        List<Object[]> valuesList = new ArrayList<>();
        for (int i = idx1; i <= idx2; i++) {
            valuesList.add(getter.get(i));
        }

        List<Integer> columnSizes = new ArrayList<>();
        int col = 0;
        boolean hasData = true;
        while (hasData) {
            hasData = false;
            int maxStrLen = -1;
            for (Object[] row : valuesList) {
                if (col < row.length) {
                    Object value = row[col];
                    String strVal = (value != null ? value.toString() : "");
                    int strLen = strVal.length();
                    hasData = true;
                    maxStrLen = Math.max(maxStrLen, strLen);
                }
            }
            if (hasData) {
                columnSizes.add(maxStrLen);
            }
            col++;
        }
        
        for (int size : columnSizes) {
            format = format.replaceFirst("%s", String.format("%%%ds", Math.max(size, 1)));
        }
        
        StringBuilder sb = new StringBuilder();
        for (Object[] values : valuesList) {
            sb.append(String.format(format, values)).append('\n');
        }
        return sb.toString();
    }
    
    //
    // Atack trace
    //
    
    private static final String STACK_TRACE_HIDDEN_CALL = String.format("%s.getStackTrace", ISysLogUtil.class.getName());
        
    public static String getStackTrace(String packagePrefix) {
        return getStackTrace(packagePrefix, null);
    }

    /**
     * Creates a trimmed stack trace that shows only frames that start with
     * <code>packagePrefix</code>.
     * We can ignore first n frames if they start with <code>startExcludeStr</code>
     * @param packagePrefix
     * @param startExcludeStr
     * @return
     */
    public static String getStackTrace(String packagePrefix, String startExcludeStr) {
        Throwable t = new Throwable();
        return getStackTrace(t, packagePrefix, startExcludeStr);
    }

    public static String getStackTrace(Throwable t, String packagePrefix, String startExcludeStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack trace of runnable execution.\n");

        int hitCount = 0;
        int otherCount = 0;
        boolean isStartExclude = (startExcludeStr != null); // Deleting top of stack trace?
        for (StackTraceElement el : t.getStackTrace()) {
            final String stringPres = el.toString();
            if (stringPres.startsWith(STACK_TRACE_HIDDEN_CALL)) {
                continue;
            }
            if (    isStartExclude  &&
                    stringPres.startsWith(startExcludeStr)) {
                continue;
            }
            isStartExclude = false;
            if (el.getClassName().startsWith(packagePrefix)) {
                if (otherCount > 0) {
                    sb.append(String.format("\t\t... [%d]\n", otherCount));
                    otherCount = 0;
                }
                sb.append("\tat ").append(stringPres).append('\n');
                hitCount++;
            }
            else {
                otherCount++;
            }
        }

        if (otherCount > 0) {
            sb.append(String.format("\t\t... [%d]\n", otherCount));
        }
        
        if (hitCount > 0) {
            return sb.toString();
        }
        else {
            return getStackTrace();
        }
    }

    public static String getStackTrace() {
        Throwable t = new Throwable();
        return getStackTrace(t);
    }
    
    public static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            final String stringPres = el.toString();
            if (stringPres.startsWith(STACK_TRACE_HIDDEN_CALL)) {
                continue;
            }
            sb.append("\tat ").append(el.toString()).append('\n');
        }
        return sb.toString();
    }

    public static String swtEvent(int eventType) {
        switch (eventType) {
            case SWT.None:                 return "None";
            case SWT.KeyDown:              return "KeyDown";
            case SWT.KeyUp:                return "KeyUp";
            case SWT.MouseDown:            return "MouseDown";
            case SWT.MouseUp:              return "MouseUp";
            case SWT.MouseMove:            return "MouseMove";
            case SWT.MouseEnter:           return "MouseEnter";
            case SWT.MouseExit:            return "MouseExit";
            case SWT.MouseDoubleClick:     return "MouseDoubleClick";
            case SWT.Paint:                return "Paint";
            case SWT.Move:                 return "Move";
            case SWT.Resize:               return "Resize";
            case SWT.Dispose:              return "Dispose";
            case SWT.Selection:            return "Selection";
            case SWT.DefaultSelection:     return "DefaultSelection";
            case SWT.FocusIn:              return "FocusIn";
            case SWT.FocusOut:             return "FocusOut";
            case SWT.Expand:               return "Expand";
            case SWT.Collapse:             return "Collapse";
            case SWT.Iconify:              return "Iconify";
            case SWT.Deiconify:            return "Deiconify";
            case SWT.Close:                return "Close";
            case SWT.Show:                 return "Show";
            case SWT.Hide:                 return "Hide";
            case SWT.Modify:               return "Modify";
            case SWT.Verify:               return "Verify";
            case SWT.Activate:             return "Activate";
            case SWT.Deactivate:           return "Deactivate";
            case SWT.Help:                 return "Help";
            case SWT.DragDetect:           return "DragDetect";
            case SWT.Arm:                  return "Arm";
            case SWT.Traverse:             return "Traverse";
            case SWT.MouseHover:           return "MouseHover";
            case SWT.HardKeyDown:          return "HardKeyDown";
            case SWT.HardKeyUp:            return "HardKeyUp";
            case SWT.MenuDetect:           return "MenuDetect";
            case SWT.SetData:              return "SetData";
            case SWT.MouseVerticalWheel:   return "MouseVerticalWheel";
            case SWT.MouseHorizontalWheel: return "MouseHorizontalWheel";
            case SWT.Settings:             return "Settings";
            case SWT.EraseItem:            return "EraseItem";
            case SWT.MeasureItem:          return "MeasureItem";
            case SWT.PaintItem:            return "PaintItem";
            case SWT.ImeComposition:       return "ImeComposition";
            case SWT.OrientationChange:    return "OrientationChange";
            case SWT.Skin:                 return "Skin";
            case SWT.OpenDocument:         return "OpenDocument";
            case SWT.Touch:                return "Touch";
            case SWT.Gesture:              return "Gesture";
            default: return String.format("Unknown SWT event type: %d", eventType);
        }        
    }
    
    public static String boundsStr(Rectangle r) {
        if (r == null) {
            return "null";
        }
        
        return String.format("Bounds [%d, %d, %d, %d]",
                r.x, 
                r.y, 
                r.x + r.width, 
                r.y + r.height);
    }
}
