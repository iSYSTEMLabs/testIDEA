package si.isystem.itest.ui.comp;

/**
 * This interface should be implemented by classes, which modify contents
 * of controls to help user. For example, to add correct extension to
 * analyzer file name.  
 *  
 * @author markok
 *
 */
public interface IAsistListener {

    String onFocusLost(String content);
}
