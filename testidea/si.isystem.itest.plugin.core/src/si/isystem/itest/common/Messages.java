package si.isystem.itest.common;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "si.isystem.itest.common.messages"; //$NON-NLS-1$
    public static String StubSpecEditor__Parameters;
    public static String StubSpecEditor__Return_value;
    public static String StubSpecEditor__Script;
    public static String StubSpecEditor__Actions_when_stub_is_hit;
    public static String StubSpecEditor__Stub_results;
    public static String TagCommentDialog_Block_comment;
    public static String TagCommentDialog_Clear_all_fields;
    public static String TagCommentDialog_Dialog_title;
    public static String TagCommentDialog_End_of_line_comment;
    
    public static String PropEvaluator_isOverride;
    public static String PropEvaluator_isIntInHex;
    public static String PropEvaluator_isMemAreaInPtrProto;
    public static String PropEvaluator_isCharArrayAsStr;
    public static String PropEvaluator_isDerefCharArray;
    public static String PropEvaluator_isArrayAndStructValues;
    public static String PropEvaluator_fpPrecision;
    public static String PropEvaluator_ChrFmtASCII;
    public static String PropEvaluator_ChrFmtInt;
    public static String PropEvaluator_ChrFmtASCIIAndInt;

    public static String PropEvaluator_isCharANSI;
    public static String PropEvaluator_isHexFmtWOPrefix;
    public static String PropEvaluator_isHexFmtWPrefix;
    public static String PropEvaluator_isBinFmtWOB;
    public static String PropEvaluator_isBinFmtWB;
    public static String PropEvaluator_EnumEnumOnly;
    public static String PropEvaluator_EnumIntOnly;
    public static String PropEvaluator_EnumEnumAndInt;
            
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }


    private Messages() {
    }
}
