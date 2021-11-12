//package si.isystem.itest.common;
//
//
//import org.eclipse.swt.dnd.ByteArrayTransfer;
//import org.eclipse.swt.dnd.DND;
//import org.eclipse.swt.dnd.TextTransfer;
//import org.eclipse.swt.dnd.TransferData;
//// import org.eclipse.swt.internal.ole.win32.COM;
//
//
//@SuppressWarnings("restriction")
//public class YamlTransfer extends ByteArrayTransfer {
//
//    TextTransfer m_textTransfer;
//    private static final String CF_UNICODETEXT = "CF_UNICODETEXT"; //$NON-NLS-1$
//    private static final String CF_TEXT = "CF_TEXT"; //$NON-NLS-1$
//    private static final int CF_UNICODETEXTID = 13; // COM.CF_UNICODETEXT;
//    private static final int CF_TEXTID = 1; //COM.CF_TEXT;
//    private static final int CF_YAML_ID = 155;
//    private static final String CF_YAML = "CF_YAML";
//    static YamlTransfer m_instance = new YamlTransfer();
//
//    protected YamlTransfer() {
//        m_textTransfer = TextTransfer.getInstance();
//    }
//
//    public static YamlTransfer instance() {
//        return m_instance;
//    }
//
//    @Override
//    protected int[] getTypeIds() {
//        return new int[] {CF_UNICODETEXTID, CF_TEXTID, CF_YAML_ID};
//    }
//
//
//    @Override
//    protected String[] getTypeNames() {
//        return new String[] {CF_UNICODETEXT, CF_TEXT, CF_YAML};
//    }
//
//
//    @Override
//    protected void javaToNative(Object object, TransferData transferData) {
//        if (transferData.type == CF_UNICODETEXTID  ||  transferData.type == CF_TEXTID) {
//            m_textTransfer.javaToNative(object, transferData);
//        } else 
//            if (transferData.type == CF_YAML_ID) {
//                transferData.type = CF_TEXTID;
//                m_textTransfer.javaToNative(object, transferData);
//            } else {
//                DND.error(DND.ERROR_INVALID_DATA);
//                /* throw new SIllegalArgumentException("Invalid data type!")
//                                               .add("dataType", transferData.type);
//                 */
//            }
//    }
//
//
//    @Override
//    protected Object nativeToJava(TransferData transferData) {
//        if (transferData.type == CF_UNICODETEXTID  ||  transferData.type == CF_TEXTID) {
//            return m_textTransfer.nativeToJava(transferData);
//        } else 
//            if (transferData.type == CF_YAML_ID) {
//                // System.err.println("YAML!!!");
//                transferData.type = CF_TEXTID;
//                return m_textTransfer.nativeToJava(transferData);
//            } 
//        // else {
//        //    DND.error(DND.ERROR_INVALID_DATA);
//        // throw new SIllegalArgumentException("Invalid data type!")
//        //                                .add("dataType", transferData.type);
//        //}
//        return null;
//    }
//}
//
