package si.isystem.commons.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class ISysAppUtils 
{
    public static Long getProcessId() {
        final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        final String jvmName = mxBean.getName();
        final int index = jvmName.indexOf('@');
        final String pidStr;
        if (index > 0) {
            pidStr = jvmName.substring(0, index);
        }
        else {
            pidStr = jvmName;
        }
        return Long.parseLong(pidStr);
    }
}
