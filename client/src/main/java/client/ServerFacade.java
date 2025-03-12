package client;

public class ServerFacade {
    private final ServerCommunicator serverCommunicator;
    private String authToken;
    private final String serverName;

    public ServerFacade(String serverName) {
        this.serverName = serverName;
        this.serverCommunicator = new HttpCommunicator(this, serverName);
    }

    public boolean register(String username, String password, String email){
        return serverCommunicator.register(username, password, email);
    }
}
