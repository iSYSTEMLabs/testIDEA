package si.isystem.itest.model;

import si.isystem.itest.model.StatusTableLine.StatusType;

public class StatusModelEvent {

    public enum EEventType {
        ERefresh,
        EAppend, 
        ESetDetailPane
    }
    
    public enum ETextFormat {
        EPlainText,
        EMarkdownText,
    }
    
    private EEventType m_eventType;
    private StatusTableLine m_statusLine;
    private String m_detailPaneText;
    private StatusType m_severity;
    private int m_scrollToLine = -1;
    private ETextFormat m_textFormat = ETextFormat.EMarkdownText;
    
    
    public StatusModelEvent(EEventType evType, StatusTableLine statusLine) {
        m_eventType = evType;
        m_statusLine = statusLine;
        if (statusLine != null) {
            m_detailPaneText = statusLine.getMessage();
            m_severity = statusLine.getStatusType();
        } else {
            m_detailPaneText = "";
            m_severity = StatusType.OK;
        }
    }

    
    public StatusModelEvent(EEventType evType, String detailPaneText, 
                            StatusTableLine.StatusType severity) {
        m_eventType = evType;
        m_detailPaneText = detailPaneText;
        m_severity = severity;
    }

    
    public StatusModelEvent(EEventType evType, String detailPaneText, 
                            StatusTableLine.StatusType severity,
                            int scrollToLine) {
        m_eventType = evType;
        m_detailPaneText = detailPaneText;
        m_severity = severity;
        m_scrollToLine = scrollToLine;
    }

    
    public EEventType getEventType() {
        return m_eventType;
    }

    
    public StatusTableLine getStatusLine() {
        return m_statusLine;
    }

    
    public String getDetailPaneText() {
        return m_detailPaneText == null ? "" : m_detailPaneText;
    }


    public StatusType getSeverity() {
        return m_severity;
    }


    public int getScrollToLine() {
        return m_scrollToLine;
    }


    public ETextFormat getTextFormat() {
        return m_textFormat;
    }


    public void setTextFormat(ETextFormat textFormat) {
        m_textFormat = textFormat;
    }
}
