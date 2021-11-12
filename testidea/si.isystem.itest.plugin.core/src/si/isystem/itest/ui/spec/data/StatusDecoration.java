package si.isystem.itest.ui.spec.data;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.widgets.Control;

import si.isystem.itest.common.IconProvider;

/**
 * This class is used when a control (for example Text) needs test result
 * indicator - for example that test failed.
 * 
 * @author markok
 */
public class StatusDecoration {

    private ControlDecoration m_statusDecoration;

    public enum EStatusType {ERROR, INFO};
    
    public StatusDecoration(Control uiControl, Integer iconPosition) {
        m_statusDecoration = new ControlDecoration(uiControl, iconPosition);
        m_statusDecoration.setImage(null);
    }


    /**
     * No decoration means there was no error, but also no script output 
     * (reserved var was not set).
     * @param desc
     * @param statusType
     */
    public void setDescriptionText(String desc, EStatusType statusType) {
        
        if (m_statusDecoration == null) {
            // when there is no decoration for the given control
            return;
        }

        m_statusDecoration.setDescriptionText(desc);
        
        if (statusType == EStatusType.ERROR) {
            m_statusDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.TEST_ERR_OVERLAY));
        } else if (statusType == EStatusType.INFO) {
            if (!desc.isEmpty()) {
                m_statusDecoration.setImage(IconProvider.getOverlay(IconProvider.EOverlayId.SCRIPT_STATUS_INFO));
            } else {
                m_statusDecoration.setImage(null);
            }
        }
    }
}
