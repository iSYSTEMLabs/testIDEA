package si.isystem.itest.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import si.isystem.commons.connect.JConnection;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CIDEController.EPathType;
import si.isystem.connect.ConnectionMgr;
import si.isystem.connect.utils.OsUtils;
import si.isystem.connect.utils.WinIDEAManager;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.editors.TestCaseEditorPart;
import si.isystem.itest.ipc.ConnectionProvider;
import si.isystem.itest.model.TestSpecificationModel;

/**
 * This class contains utility methods for file handling.
 *  
 * @author markok
 *
 */
public class ISysPathFileUtils {

    /**
     * Returns absolute directory, where iyaml file, which is opened in currently selected 
     * editor, is located.
     * @return
     */
    public static String getIYAMLDir() {
        TestSpecificationModel activeModel = TestCaseEditorPart.getActiveModel();
        if (activeModel == null) {
            throw new SIllegalStateException("No editor with test file is selected. Please open and select one.");
        }
        String workingDir = new File(activeModel.getModelFileName()).getAbsoluteFile().getParent();
        return workingDir;
    }
    
    
    public static String getReportDirFromTestSpec() {
        TestSpecificationModel activeModel = TestCaseEditorPart.getActiveModel();
        if (activeModel == null) {
            throw new SIllegalStateException("No editor with test file is selected. Please open and select one.");
        }
        
        return activeModel.getTestReportConfig().getFileName();
    }
    
    
    public static String getAbsReportDir() {
        String absReportFName = getAbsReportFName();
        
        if (Files.isDirectory(Paths.get(absReportFName))) {
            return absReportFName; // if report file name is not specified, 
                 // absReportFName already contains directory, not file name 
        }
        
        return Paths.get(absReportFName).getParent().toString();
    }
     
    
    /**
     * If report file is given with relative path, it means relative to IYAML
     * file dir.
     *  
     * @return
     */
    public static String getAbsReportFName() {
        TestSpecificationModel activeModel = TestCaseEditorPart.getActiveModel();
        if (activeModel == null) {
            throw new SIllegalStateException("No editor with test file is selected. Please open and select one.");
        }
        
        String reportFileName = activeModel.getTestReportConfig().getFileName();
        String workingDir = new File(activeModel.getModelFileName()).getAbsoluteFile().getParent();

        return getAbsPathFromDir(workingDir, reportFileName);
    }
    
    
    /**
     * If the given file name is absolute, it is returned unchanged. If it is 
     * relative, it is made absolute by prepending working dir (iyaml file dir)
     * to it.
     * 
     * @param fileName abs or relative file name
     * @see #getIYAMLDir()
     */
    public static String getAbsPathFromWorkingDir(String fileName) {

        String workingDir = getIYAMLDir();
        
        Path path = Paths.get(fileName);
        
        // path.isAbsolute() returns false on Windows for paths like '\tmp\file.txt' 
        if (!path.isAbsolute()  &&  fileName.length() > 0  &&  fileName.charAt(0) != File.separatorChar) {
            path = Paths.get(workingDir, fileName);
            fileName = path.toString();
        }

        return fileName;
    }


    /**
     * If the given file name is absolute, it is returned unchanged. If it is 
     * relative, it is made absolute by prepending the given dir.
     * 
     * @param fileName abs or relative file name
     */
    public static String getAbsPathFromDir(String dir, String fileName) {

        Path path = Paths.get(fileName);
        if (!path.isAbsolute()) {
            path = Paths.get(dir, fileName);
            fileName = path.toString();
        }

        return fileName;
    }


    public static String getWinIDEAWorkspaceDir() {
        
        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();
        
        String wsDir = WinIDEAManager.getWinIDEAWorkspaceDir(jCon.getPrimaryCMgr());
        return OsUtils.winPathToNativePath(wsDir);
    }

/*
    private static JConnection getOrCreateConnection() {
        TestSpecificationModel model = TestSpecificationModel.getActiveModel();
        if (model == null) {
            throw new SIOException("No active model found! " + 
                                   "Please select editor with iyaml file, which "
                                    + "contains connection information.");
        }
        
        return ConnectionProvider.connectToWinIdea(model.getCEnvironmentConfiguration());
    }
    */
    
    public static String getDotExeDir() {

        if (OsUtils.isLinux()) {
        	return ""; // use system installed graphwiz 'dot' on Linux
        }
    	
        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();
        
        String dotExeDir = WinIDEAManager.getWinIDEAExeDir(jCon.getPrimaryCMgr());
        Path p = Paths.get(dotExeDir, "graphwiz/bin");

        return p.toString();
    }
    

    /**
     * Requires connection to winIDEA and selected testIDEA editor.
     * Must be run from UI thread.
     * 
     * @return
     */
    public static ISysDirs getISysDirs() {
        
        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();
        ConnectionMgr cmgr = jCon.getPrimaryCMgr();
        CIDEController ide = new CIDEController(cmgr);
        
        String wsDir = ide.getPath(EPathType.WORKSPACE_DIR);
        String wsFName = ide.getPath(EPathType.WORKSPACE_FILE_NAME);
        String defaultDlFile = "";
        try {
            defaultDlFile = ide.getDefaultDownloadFile();
        } catch (Exception ex) {
            if (!(ex instanceof FileNotFoundException)) {
                throw ex;
            }
            // FNFE is thrown only when there are no download files with symbols, or
            // `Load Symbols` in winIDEA dialog 'Files for download' is not checked.
            // However, user may still want to run system tests with Python script
            // to get nice testIDEA reports :-| B023891.
        }
        String iyamlDir = getIYAMLDir();
        String reportDir = getAbsReportDir();
        String dotExeDir = getDotExeDir();
        
        ISysDirs dirs = new ISysDirs(OsUtils.winPathToNativePath(wsDir),
                                     OsUtils.winPathToNativePath(wsFName),
                                     OsUtils.winPathToNativePath(defaultDlFile),
                                     iyamlDir,
                                     reportDir,
                                     dotExeDir);
        return dirs;
    }
    

    /** 
     * Requires connection to winIDEA.
     * 
     * @return
     */
    public static ISysDirs getISysDirs(String iyamlFileName, String reportFileName) {

        JConnection jCon = ConnectionProvider.instance().getDefaultConnection();
        ConnectionMgr cmgr = jCon.getPrimaryCMgr();
        CIDEController ide = new CIDEController(cmgr);

        String iyamlDir = new File(iyamlFileName).getParent();
        String reportDir = new File(reportFileName).getParent();
        String dotExeDir = getDotExeDir();
        
        ISysDirs dirs = new ISysDirs(ide.getPath(EPathType.WORKSPACE_DIR),
                                     ide.getPath(EPathType.WORKSPACE_FILE_NAME),
                                     ide.getDefaultDownloadFile(),
                                     iyamlDir,
                                     reportDir,
                                     dotExeDir);
        return dirs;
    }
}
