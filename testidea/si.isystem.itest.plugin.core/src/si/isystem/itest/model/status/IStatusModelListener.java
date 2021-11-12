package si.isystem.itest.model.status;

import si.isystem.itest.model.StatusModelEvent;

public interface IStatusModelListener {

    void appendLine(StatusModelEvent event);
    void refresh(StatusModelEvent event);
    void setDetailPaneText(StatusModelEvent event);
}
