package si.isystem.connect.utils;

import si.isystem.connect.CDebugFacade;
import si.isystem.connect.CDocumentController;
import si.isystem.connect.CIDEController;
import si.isystem.connect.CIDEController.EPathType;
import si.isystem.connect.CLineLocation;
import si.isystem.connect.CMemAddress;
import si.isystem.connect.ConnectionMgr;

/**
 * This class encapsulates operations, which are called from Java application 
 * and executed in winIDEA.
 * 
 */
public class WinIDEAManager {

    
    /**
     * This method sets focus on the given function in the source code.
     * If the source file is not opened, it is opened by this call.
     * 
     * @param cmgr connection manager with established connection to winIDEA
     * @param funcName name of the C function to show in winIDEA
     */
    public static void showFunctionInEditor(ConnectionMgr cmgr, String funcName) {

            CDebugFacade debug = new CDebugFacade(cmgr);
            CMemAddress funcAddress = debug.getFunctionAddress(funcName);
            CLineLocation lineLoc = debug.getSourceLineAtAddress(funcAddress.getAddress());

            CDocumentController doc = new CDocumentController(cmgr, 
                                                              lineLoc.getFileName(), 
                                                              "r");

            doc.setFocus((int)lineLoc.getLineNumber());
            
            CIDEController ide = new CIDEController(cmgr);
            ide.bringWinIDEAToTop();
    }
    
    
    /**
     * This method sets focus on the given line in the source code.
     * If the source file is not opened, it is opened by this call.
     * See GlobalsSelectionControl for usage example.
     * 
     * @param cmgr connection manager with established connection to winIDEA
     * @param fileName name of the file to be shown in winIDEA
     * @param lineNumber line number to set focus to 
     */
    public static void showSourceInEditor(ConnectionMgr cmgr, String fileName, int lineNumber) {

            CDocumentController doc = new CDocumentController(cmgr, 
                                                              fileName, 
                                                              "r");

            doc.setFocus(lineNumber);
            
            CIDEController ide = new CIDEController(cmgr);
            ide.bringWinIDEAToTop();
    }
    
    
    /**
     * This method sets focus on the line in source code, which compiles to the
     * given address.
     * If the source file is not opened, it is opened by this call.
     * 
     * @param cmgr connection manager with established connection to winIDEA
     * @param address memory address in decimal or hex format (with prefix '0x').
     */
    public static void showSourceAtAddressInEditor(ConnectionMgr cmgr, String address) {

            CDebugFacade debug = new CDebugFacade(cmgr);
            long addr = 0;
            if (address.length() > 2  &&  address.charAt(1) == 'x') {
                addr = Long.parseLong(address.substring(2), 16);
            } else {
                addr = Long.parseLong(address, 10);
            }
                
            CLineLocation lineLoc = debug.getSourceLineAtAddress(addr);

            CDocumentController doc = new CDocumentController(cmgr, 
                                                              lineLoc.getFileName(), 
                                                              "r");

            doc.setFocus((int)lineLoc.getLineNumber());
            
            CIDEController ide = new CIDEController(cmgr);
            ide.bringWinIDEAToTop();
    }
    
    
    public static String getWinIDEAExeDir(ConnectionMgr cmgr) {
        
        CIDEController ide = new CIDEController(cmgr);
        return ide.getPath(EPathType.WINIDEA_EXE_DIR);
    }
    
    
    public static String getWinIDEAWorkspaceDir(ConnectionMgr cmgr) {
        
        CIDEController ide = new CIDEController(cmgr);
        return ide.getPath(EPathType.WORKSPACE_DIR);
    }
    

    public static String getWinIDEAWorkspaceFile(ConnectionMgr cmgr) {
        
        CIDEController ide = new CIDEController(cmgr);
        return ide.getPath(EPathType.WORKSPACE_FILE_NAME);
    }
    
    
    public static String getDefaultDownloadFile(ConnectionMgr cmgr) {
        
        CIDEController ide = new CIDEController(cmgr);
        return ide.getDefaultDownloadFile();
    }
}
