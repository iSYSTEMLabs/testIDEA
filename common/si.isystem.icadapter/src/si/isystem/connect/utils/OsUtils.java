package si.isystem.connect.utils;

import java.io.IOException;
import java.util.Locale;

public class OsUtils {

    private static Boolean m_isWindows = null;

    // Default drive used by Wine as Linux root (Z:\  == /)
    // Since it is configurable in Wine, so users may have set it to other value), 
    // this value is reassigned in winPathToNativePath(). The only condition is, that 
    // winPathToNativePath() is called before 
    // nativePathToWinPath(). If necessary, add call to 
    // winPathToNativePath() in debug plug-in startup code, get Wine path
    // by calling CIDEController.getPath(WORKSPACE_DIR).
    private static char m_defaultWineRootDrive = 'Z';

    public static boolean isWindows() {

        if (m_isWindows == null) {
            String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            m_isWindows = Boolean.valueOf(os.startsWith("windows"));
        }

        return m_isWindows.booleanValue();
    }


    public static boolean isLinux() {
        return !m_isWindows.booleanValue();
    }


    /**
     * Starts process. IO streams are currently not read, which might present 
     * problem on Windows. Improve if needed.
     * 
     * @param cmdAndArgs
     * @throws IOException
     */
    public static boolean startProcess(String ... cmdAndArgs) throws IOException {

        ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
        Process process = pb.start();

        return process != null;
    }


    /**
     * Converts paths returned by winIDEA, which are in Windows format with drive and '\'
     * into Linux format when running on on Linux.
     * @param fnameOrDir path to convert, does not need to exist in file system.
     * 
     * @return converted path on Linux, original path on Windows.
     */
    public static String winPathToNativePath(String fnameOrDir) {

        if (isLinux()) {

            fnameOrDir = fnameOrDir.replace('\\', '/');

            if (fnameOrDir.length() > 1) {
                if (fnameOrDir.charAt(1) == ':') {
                    m_defaultWineRootDrive = fnameOrDir.charAt(0); 
                    fnameOrDir = fnameOrDir.substring(2);
                }
            }
        }

        return fnameOrDir;
    }


    /**
     * Converts native path provided by client (eg. Eclipse for setting breskpoint)
     * to windows path expected by winIDEA.
     * 
     * @param fnameOrDir path to convert, does not need to exist in file system.
     * 
     * @return converted path on Linux, original path on Windows.
     */
    public static String nativePathToWinPath(String fnameOrDir) {

        if (isLinux()) {
            fnameOrDir = m_defaultWineRootDrive + ":" + fnameOrDir.replace('/', '\\');
        }

        return fnameOrDir;
    }
}
