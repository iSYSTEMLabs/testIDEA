package si.isystem.itest.common;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.action.Action;

import si.isystem.itest.handlers.EditRedoCmdHandler;
import si.isystem.itest.handlers.TestSaveTestReportCmdHandler;

/**
 * This class stores references to object, which Eclipse or RCP instantiate, but
 * are also needed by plugin code. Some objects, like Views and Editors can be
 * obtained through API, but for some objects, like command handlers, I didn't 
 * find the API. So references are stored here by object's ctor.
 * Current implementation is useful only for singletons. 
 *  
 * @author markok
 *
 */
public class ReferenceStorage {

    private static EditRedoCmdHandler m_editRedoCmdHandler;
    private static TestSaveTestReportCmdHandler m_saveTestReportCmdHandler;
    
    // used only for plugin testIDEA
    private Map<String, Action> m_actions = new TreeMap<>();
    
    private static ReferenceStorage INSTANCE = new ReferenceStorage();
    
    /**
     *  This utility class has only static methods and should not be 
     * instantiated. 
     */
    private ReferenceStorage() {}

    
    static public ReferenceStorage instance() {
        return INSTANCE;
    }
    

    public static EditRedoCmdHandler getEditRedoCmdHandler() {
        return m_editRedoCmdHandler;
    }
    
    
    public static void setEditRedoCmdHandler(EditRedoCmdHandler editRedoCmdHandler) {
        if (m_editRedoCmdHandler != null) {
            // it seems framework instantiates this one multiple times...
            // throw new IllegalStateException("Object already exists: EditRedoCmdHandler");
        }
        m_editRedoCmdHandler = editRedoCmdHandler;        
    }


    public static TestSaveTestReportCmdHandler getSaveReportCmdHandler() {
        return m_saveTestReportCmdHandler;
    }


    public static void setSaveReportCmdHandler(TestSaveTestReportCmdHandler testReportCmdHandler) {
        ReferenceStorage.m_saveTestReportCmdHandler = testReportCmdHandler;
    }
    
    
    // used only for plugin testIDEA
    public void addAction(String actionId, Action action) {
        m_actions.put(actionId, action);
    }
    
    
    public Action getAction(String actionId) {
        return m_actions.get(actionId);
    }
}
