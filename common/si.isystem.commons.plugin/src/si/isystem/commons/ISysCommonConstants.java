package si.isystem.commons;

import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ISysCommonConstants {

    public static final String COMMONS_PLUGIN_ID = "si.isystem.commons.plugin";
    public static final String ANALYZER_PLUGIN_ID = "si.isystem.analyzer";
    public static final String DEBUG_PLUGIN_ID = "si.isystem.eclipse.debug";

    public static final String EXT_POINT_CONNECTOR_FACTORY_ID = "si.isystem.analyzer.ep.connector";
    
    public static final String JANALYZER_PERSPECTIVE_ID = "si.isystem.analyzer.perspective";
    
    // keep length below 32 chars, as this is limitation in response if
    // UDP enumeration is used. See SiConnectUDPEnumeratorResponse in 
    // iConnectUDPEnumeratorDefs.h
    public static final String WINIDEA_INSTANCE_ID = "si.isystem.eclipse.debug.id.wI";
    
    public static final String IPC_ARBITRATION_FILE_NAME = "iDaemonConfigPush.cfg";

    private static final String TRD_EDITOR_EXTENSION = "trd";
    public static final String ISYSTEM_EDITOR_ID_PREFIX = "si.isystem.editor";
    
    public static final String TRD_EDITOR_ID = String.format("%s.%s", ISYSTEM_EDITOR_ID_PREFIX, TRD_EDITOR_EXTENSION);
    
    public static final String[] FILE_EDITOR_IDS = new String[] {
            TRD_EDITOR_ID
    };
    public static final String[] FILE_TYPE_EXTENSIONS = new String[] {
            TRD_EDITOR_EXTENSION
    };
    public static final String[] SUPPORTED_PERSPECTIVE_IDS = new String[] {
            JANALYZER_PERSPECTIVE_ID
    };

    public static final int AXIS_LABEL_ANGLE = 0;

    //
    // Big Decimal math context for various operations.
    //
    
    private static final int DIVISION_SCALE = 18;
    public static final MathContext mathCtx = new MathContext(DIVISION_SCALE, RoundingMode.HALF_UP);
    public static final MathContext mathCtxCeil = new MathContext(DIVISION_SCALE, RoundingMode.CEILING);
    public static final MathContext mathCtxFloor = new MathContext(DIVISION_SCALE, RoundingMode.FLOOR);
    
    public static final NumberFormat BIG_DECIMAL_FORMATTER_1 = NumberFormat.getNumberInstance();
    public static final NumberFormat BIG_DECIMAL_FORMATTER_2 = new DecimalFormat("#,###.#########");
    
    /**
     * Use this string as part of the tooltip for text fields, where input is
     * treated as regular expression. Usually you should add a statement before
     * this description of regular expression syntax, which describes intent of the
     * text field.  
     * IMPORTANT: Use si.isystem.ui.utils.UITools.setTooltip() for tool-tip setting,
     * because system tool-tips are hidden too soon.
     */
    public static final String REG_EX_TOOLTIP_POSTFIX = 
        "REGULAR EXPRESSION BASICS\n" +
        "=========================\n\n" +
        "If you are used to wildcard characters * and ?, then use:\n" +
        "  .* instead of *\n" +
        "  . instead of ?\n" +
        "\n" +
        "Meaning of some special regex characters:\n" +
        "  . - matches any single character\n" +
        "  * - matches preceding element 0 or more times\n" +
        "  ? - matches preceding element 0 or one time\n" +
        "  + - matches preceding element 1 or more times\n" +
        "  [ ] - matches any single character inside brackets\n" +
        "  ^ - start of string\n" +
        "  $ - end of string\n" +
        "  \n" +
        "Examples:\n" +
        "  'add.*' matches 'add', 'addx', 'addInt', 'addFloat', 'addXyZ', ...\n" +
        "  'Func[0-9]+' matches 'Func0', 'Func1', ..., 'Func10', 'Func12', ..., but not 'Func'.\n" +
        "  'gr[ae]y' matches 'gray' and 'grey'";
}
