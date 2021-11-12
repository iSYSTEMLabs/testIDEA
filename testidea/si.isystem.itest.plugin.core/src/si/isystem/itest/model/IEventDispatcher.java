package si.isystem.itest.model;

public interface IEventDispatcher {

    public void fireEvent(ModelChangedEvent event);
}
