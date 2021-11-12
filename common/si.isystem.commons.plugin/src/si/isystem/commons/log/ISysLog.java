package si.isystem.commons.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ISysLog 
{
    private static ISysLog s_instance = null;
    
    private final long m_time0;
    private final ISysLogConfig m_cfg = new ISysLogConfig(ISysLogLevel.Info, null, false);
    private final List<PrintStream> m_printStreams = new LinkedList<>();
    private final Map<File, PrintStream> m_fileToStreamMap = new HashMap<>();

    public static ISysLog instance() {
        if (s_instance == null) {
            s_instance = new ISysLog();
        }
        return s_instance;
    }
    
    private ISysLog() {
        m_time0 = System.currentTimeMillis();
        m_cfg.setLevel(ISysLogLevel.Info);
    }
    
    public ISysLogConfig getConfig() {
        return m_cfg;
    }
    
    public boolean isEnabled(ISysLogLevel level) {
        return m_cfg.isLogSet(level);
    }
    
    public boolean isEnabled(ISysCustomLogLevel level) {
        return m_cfg.isLogSet(level);
    }
    
    public void flush() {
        for (PrintStream ps : m_printStreams) {
            ps.flush();
        }
    }

    public void applyLogConfig(String logLevels) {
        if (logLevels == null  ||  logLevels.trim().length() == 0) {
            return;
        }

        m_cfg.clearCustomLevels();
        StringTokenizer st = new StringTokenizer(logLevels, ",");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken().trim();
            ISysLogLevel level = ISysLogLevel.getByCmd(tok);
            if (level != null) {
                m_cfg.setLevel(level);
            }
            else {
                ISysCustomLogLevel customLevel = ISysCustomLogLevel.getByTag(tok);
                if (customLevel != null) {
                    m_cfg.setEnableCustomLevel(customLevel, true);
                }
            }
        }
    }
    
    public void addFile(File file) {
        if (file == null) {
            return;
        }
        
        try {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
            }
            @SuppressWarnings("resource")
            PrintStream ps = new PrintStream(file);
            m_fileToStreamMap.put(file, ps);
            addPrintStream(ps);
            ps.format("Opened log file at: %s\n\n", new java.util.Date());
            ps.flush();
        }
        catch (FileNotFoundException e) {
            e(e, "Failed to add log file: %s", file.getAbsolutePath());
        }
    }
    
    public void removeFile(File file) {
        if (file == null) {
            return;
        }
        
        @SuppressWarnings("resource")
        PrintStream ps = m_fileToStreamMap.remove(file);
        removeStream(ps);
    }
    
    public void addPrintStream(PrintStream ps) {
        m_printStreams.add(ps);
    }
    
    public void removeStream(PrintStream ps) {
        m_printStreams.remove(ps);
        ps.close();
    }
    
    public PrintStream[] getPrintStreams() {
        return m_printStreams.toArray(new PrintStream[m_printStreams.size()]);
    }
    
    public List<File> getFiles() {
        return new ArrayList<>(m_fileToStreamMap.keySet());
    }
    
    public void applyConfig(ISysLogConfig c) {
        m_cfg.setLevel(c.getLevel());
        m_cfg.clearCustomLevels();
        for (ISysCustomLogLevel level : ISysCustomLogLevel.values()) {
            m_cfg.setEnableCustomLevel(level, c.isLogSet(level));
        }
        m_cfg.setImmediateFlush(c.isImmediateFlush());
    }
    
    //
    // Logging methods
    //
    
    public void e(Throwable t, String msg, Object... args) {
        e(msg, args);
        e(t);
    }
    
    public void e(Throwable t) {
        t.printStackTrace(System.err);
        if (m_cfg.isImmediateFlush()) {
            System.err.flush();
        }
        for (PrintStream ps : m_printStreams) {
            t.printStackTrace(ps);
            if (m_cfg.isImmediateFlush()) {
                ps.flush();
            }
        }
    }
    
    public void e(String msg, Object... args) {
        String formattedMsg = format("ERROR", msg, args);
        System.err.print(formattedMsg);
        if (m_cfg.isImmediateFlush()) {
            System.err.flush();
        }
        for (PrintStream ps : m_printStreams) {
            ps.print(formattedMsg);
            if (m_cfg.isImmediateFlush()) {
                ps.flush();
            }
        }
    }
    
    public void w(Throwable t, String msg, Object... args) {
        w(msg, args);
        w(t);
    }
    
    public void w(String msg, Object... args) {
        log(ISysLogLevel.Warn, msg, args);
    }
    
    public void w(Throwable t) {
        for (PrintStream ps : m_printStreams) {
            t.printStackTrace(ps);
            if (m_cfg.isImmediateFlush()) {
                ps.flush();
            }
        }
    }
    
    public void c(String msg, Object... args) { log(ISysLogLevel.Config,  msg, args); }
    public void i(String msg, Object... args) { log(ISysLogLevel.Info,    msg, args); }
    public void v(String msg, Object... args) { log(ISysLogLevel.Verbose, msg, args); }
    public void d(String msg, Object... args) { log(ISysLogLevel.Debug,   msg, args); }
    
    public void cWork(String msg, Object... args)     { log(ISysCustomLogLevel.Workers,     msg, args); }
    public void cView(String msg, Object... args)     { log(ISysCustomLogLevel.View,        msg, args); }
    public void cEditor(String msg, Object... args)   { log(ISysCustomLogLevel.Editor,      msg, args); }
    public void cTree(String msg, Object... args)     { log(ISysCustomLogLevel.Tree,        msg, args); }
    public void cInput(String msg, Object... args)    { log(ISysCustomLogLevel.Input,       msg, args); }
    public void cGui(String msg, Object... args)      { log(ISysCustomLogLevel.Gui,         msg, args); }
    public void cDaemon(String msg, Object... args)   { log(ISysCustomLogLevel.IDaemon,     msg, args); }
    public void cIConn(String msg, Object... args)    { log(ISysCustomLogLevel.IConn,       msg, args); }
    public void cConcurr(String msg, Object... args)  { log(ISysCustomLogLevel.Concurrency, msg, args); }

    public void cIeCall(String msg, Object... args)     { log(ISysCustomLogLevel.ieCalls,    msg, args); }
    public void cIeCfg(String msg, Object... args)      { log(ISysCustomLogLevel.ieConfig,   msg, args); }
    public void cIeCore(String msg, Object... args)     { log(ISysCustomLogLevel.ieCore,     msg, args); }
    public void cIeTimeline(String msg, Object... args) { log(ISysCustomLogLevel.ieTimeline, msg, args); }
    public void cIeStat(String msg, Object... args)     { log(ISysCustomLogLevel.ieStat,     msg, args); }
    public void cIeTiming(String msg, Object... args)   { log(ISysCustomLogLevel.ieTiming,   msg, args); }
    public void cIeDocNot(String msg, Object... args)   { log(ISysCustomLogLevel.ieDocNot,   msg, args); }
    public void cIeFind(String msg, Object... args)     { log(ISysCustomLogLevel.ieFind,     msg, args); }
    public void cIeReqTrace(String msg, Object... args) { log(ISysCustomLogLevel.ieReqTrace, msg, args); }
    
    public final void log(ISysLogLevel level, String msg, Object... args) {
        if (isEnabled(level)) {
            log(level.getSignature(), msg, args);
        }
    }
    
    public final void log(ISysCustomLogLevel level, String msg, Object... args) {
        if (isEnabled(level)) {
            log(level.getSignature(), msg, args);
        }
    }

    private void log(String levelName, String msg, Object... args) {
        String formattedMsg = format(levelName, msg, args);
        for (PrintStream ps : m_printStreams) {
            ps.print(formattedMsg);
            if (m_cfg.isImmediateFlush()) {
                ps.flush();
            }
        }
    }
    
    private String format(String levelName, String msg, Object... args) {
        final String str = String.format(msg, args);
        final long time = System.currentTimeMillis() - m_time0;
        if (str.endsWith("\n")) {
            return String.format("[%s @ %,d] %s", levelName, time, str);
        }
        else {
            return String.format("[%s @ %,d] %s\n", levelName, time, str);
        }
    }
    
    public void test() {
        // Standard logging levels test
        e("enabled");
        w("enabled");
        c("enabled");
        i("enabled");
        v("enabled");
        d("enabled");
        
        // Custom logging types test
        cWork("enabled");
        cView("enabled");
        cEditor("enabled");
        cTree("enabled");
        cInput("enabled");
        cGui("enabled");
        cDaemon("enabled");
        cIConn("enabled");
        
        // iEngine logs
        cIeCall("enabled");
        cIeCfg("enabled");
        cIeCore("enabled");
        cIeTimeline("enabled");
        cIeStat("enabled");
        cIeTiming("enabled");
        cIeDocNot("enabled");
        cIeFind("enabled");
        cIeReqTrace("enabled");
    }

    public void closeLogging() {
        for (PrintStream ps : m_printStreams) {
            if (ps != null) {
                ps.flush();
                ps.close();
            }
        }
    }

//    public static void main(String[] args) {
//        StringHolder inputHolder = new StringHolder();
//        StringHolder debugHolder = new StringHolder();
//        
//        ArgParser parser = new ArgParser("");
//        parser.addOption("--input=%s #TRD file to open upon startup.", inputHolder);
//        parser.addOption("--debug=%s #Debug options (comma separated values):\n"
//                + "Basic debug flags: w, c, i, v, d\n"
//                + String.format("Additional logs: %s", Arrays.toString(s_customLogMap.keySet().toArray())), debugHolder);
//        parser.matchAllArgs(args);
//        
//        System.out.format("input = %s\n", inputHolder.value);
//        System.out.format("debug = %s\n", debugHolder.value);
//
//        applyLogConfig(debugHolder.value);
//        addPrintStream(System.out);
//        test();
//    }
    
//    public static void main(String[] args) {
//        ISysLog.setLevel(LEVEL_INFO);
//        ISysLog.setCustomLog(CUSTOM_LEVEL_VIEW | CUSTOM_LEVEL_WORKERS | CUSTOM_LEVEL_IE_CONFIG);
//        
//        ISysLog.addPrintStream(System.out);
//        
//        ISysLog.e("ERROR\n");
//        ISysLog.w("WARN\n");
//        ISysLog.i("INFO\n");
//        ISysLog.v("VER\n");
//        ISysLog.d("DBG\n");
//        
//        ISysLog.cIeCfg("IE-config");
//        ISysLog.cIeData("IE-data");
//        ISysLog.cIeStat("IE-stat");
//        ISysLog.cView("view");
//        ISysLog.cWork("work");
//    }
}
