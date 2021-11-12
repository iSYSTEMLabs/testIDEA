package si.isystem.exceptions;

import java.util.Map;

/**
 * This class provides utility methods to obtain specific information from 
 * exceptions. Returned strings are formatted according to YAML specification,
 * to improve readability.
 */
public class SEFormatter {

    /**
     *  This utility class has only static methods and should not be 
     * instantiated. 
     */
    private SEFormatter() {}

    
    /**
     * Returns context information (data set with method SException.add()) of the given 
     * exception in YAML format:
     * <pre>
     *   &lt;attrName&gt;: &lt;attrValue&gt;
     * </pre>
     * 
     * @param ex exception with data
     * @return StringBuilder with 'data:' YAML tag and attributes, or empty
     * StringBuilder if there is no data in 'ex'.
     * @see #getContextInfo(SException, StringBuilder)
     */
    public static StringBuilder getContextInfo(SException ex) {
        if (ex.getData() == null) {
            return new StringBuilder();
        }
        
        StringBuilder yamlData = new StringBuilder("  data:\n");
        getContextInfo(ex, yamlData);
        return yamlData;
    }

    /**
     * Returns context information (data set with method SException.add()) of the given 
     * exception in YAML format:
     * <pre>
     *   &lt;attrName&gt;: &lt;attrValue&gt;
     * </pre>
     * 
     * It should 
     * be used, when StringBuilder object is already used to build exception 
     * description.
     * 
     * @param ex exception which contains attributes in map
     * @param yamlData object to receive output, may not be null.
     */
    public static void getContextInfo(SException ex, StringBuilder yamlData) {
        getContextInfo(ex, yamlData, true);
    }
    
    public static void getContextInfo(SException ex, StringBuilder yamlData, boolean isMarkdown) {

        Map<String, Object> exceptionData = ex.getData();
        if (exceptionData == null) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : exceptionData.entrySet()) {
            
            String key = entry.getKey();
            
            Object value = exceptionData.get(key);
            String strValue;
            if (value != null) {
                strValue = value.toString().trim();
            } else {
                strValue = "null";
            }

            // prepend spaces to each line to keep indentation for YAML format.
            strValue = strValue.replace("\n", "\n    ");
            yamlData.append("    ");
            if (isMarkdown) {
                yamlData.append("__").append(key).append("__: ");
            } else {
                yamlData.append(key).append(": ");
            }
            
            yamlData.append(strValue).append("\n");
        }
    }

    
    /**
     * Extracts stack trace from <code>t</code>, and puts it to StringBuilder
     * in YAML format. 
     *
     * @param t object, which contains stack trace
     * @param stackLevel level of stack info to be presented. 0 means no stack 
     * info, 1 means only the immediate call, ... -1 means complete stack info.
     *  
     * @return StringBuilder with stack trace in YAML format.
     */
    public static StringBuilder getStackTrace(Throwable t, int stackLevel) {
        
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = t.getStackTrace();
        int idx = 0;
        
        if (stackLevel != 0  &&  stackTrace.length > 0) {
            sb.append("  stack:\n");
        }
        while (stackLevel != 0) {
            if (idx >= stackTrace.length) {
                break;
            }
            stackLevel--;
            StackTraceElement stackElement = stackTrace[idx];
            sb.append("  - [").
               append(stackElement.getClassName()).append(".").
               // formatting of source location must match the one of original
               // printStackTrace(), so that Eclipse console can parse location
               // and create links to source code. See also SExceptionDialog.setPrintToStdOut().
               append(stackElement.getMethodName()).append("(), (").
               append(stackElement.getFileName()).append(":").
               append(stackElement.getLineNumber()).append(")]\n");
            
            idx++;
        }
        
        return sb;
    }

    
    /**
     * Returns complete exception info (class name, message, context info, and stack trace), 
     * including nested exceptions. 
     * The return string is usually in YAML format, but
     * since it also depends on arguments passed to exception, the format can not be 
     * guaranteed. YAML format is used for readability reasons (formatting), and
     * should not be parsed to obtain exception info. If this is required, use 
     * {@link SException#getData(String)}  method.  
     * 
     * @param exception exception to get data from
     * @param stackLevel level of stack info to be presented. 0 means no stack 
     * info, 1 means only the immediate call, ... -1 means complete stack info.
     */
    public static String getInfoWithStackTrace(Throwable exception, int stackLevel) {
        return getInfoWithStackTrace(exception, stackLevel, true);
    }

    
    public static String getInfoWithStackTrace(Throwable exception, int stackLevel, 
                                               boolean isMarkdown) {

        StringBuilder sb = new StringBuilder();

        while (exception != null) {
            String exMsg = exception.getMessage();
            if (isMarkdown) {
                sb.append("__Description__:\n");
                sb.append("  __class__: ").append(exception.getClass().getSimpleName()).append('\n');
                sb.append("  __msg__: ").append(exMsg).append("\n");
            } else {
                sb.append("Description:\n");
                sb.append("  class: ").append(exception.getClass().getSimpleName()).append('\n');
                sb.append("  msg: ").append(exMsg).append("\n");
            }
            if (exception instanceof SException) {
                SException e = (SException)exception;
                getContextInfo(e, sb, isMarkdown);
            } 
            sb.append(getStackTrace(exception, stackLevel));

            Throwable[] suppressed = exception.getSuppressed();
            for (Throwable supEx : suppressed) {
                sb.append("\n**===== Secondary Exception =====**\n");
                sb.append(getInfoWithStackTrace(supEx, stackLevel));
                sb.append("**===== End of Secondary Exception =====**\n\n");
            }
            
            exception = exception.getCause();
        }

        return sb.toString();
    }
    
    
    /**
     * Returns exception message, and context info, without class 
     * name and stack trace, but including all nested exceptions. 
     * 
     * @param exception exception to get data from
     */
    public static String getInfo(Throwable exception) {
        return getInfo(exception, true);
    }
    
    public static String getInfo(Throwable exception, boolean isMarkdown) {

        StringBuilder sb = new StringBuilder();

        while (exception != null) {
            String className = exception.getClass().getSimpleName();
            if (isMarkdown) {
                sb.append("__").append(className).append("__: ");
            } else {
                sb.append(className).append(": ");
            }
            
            sb.append(exception.getMessage()).append("     \n");
            if (exception instanceof SException) {
                SException e = (SException)exception;
                getContextInfo(e, sb, isMarkdown);
            }
            
            // handle NPE specially, as it provides completely useless message
            // by default. On the other hand it is never thrown intentionally,
            // so stack trace is always welcome in case of NPE. 
            if (exception instanceof NullPointerException) {
                sb.append(getStackTrace(exception, 3));
            } 
            
            Throwable[] suppressed = exception.getSuppressed();
            for (Throwable supEx : suppressed) {
                sb.append("\n**===== Secondary Exception =====**\n");
                sb.append(getInfo(supEx));
                sb.append("**===== End of Secondary Exception =====**\n\n");
            }
            
            exception = exception.getCause();
        }

        return sb.toString();
    }
    
    
    /**
     * Returns error messages of this and nested exceptions as string, one message per line.
     * Exception class name and context information are not returned.
     * Use this method, when to much information may confuse user.
     *  
     * @param exception exception with optionally nested exceptions to get error 
     *        messages from
     * @return multiline string with error messages.
     */
    public static String getErrorMessages(Throwable exception) {

        StringBuilder sb = new StringBuilder();

        while (exception != null) {
            sb.append("- ").append(exception.getMessage()).append('\n');
            exception = exception.getCause();
        }

        return sb.toString();
    }
}
