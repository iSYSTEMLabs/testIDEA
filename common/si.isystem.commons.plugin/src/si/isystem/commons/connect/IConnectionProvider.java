package si.isystem.commons.connect;

/**
 * This interface should be implemented by client class, which provides 
 * connections for the plug-in. Normally this class will get connection
 * configuration from plug-in settings, and then open a connection with 
 * ConnectCommandHandler. See package doc for an example.
 */
public interface IConnectionProvider {

    JConnection getDefaultConnection();
    JConnection getConnectionForId(String connectionId);
}
