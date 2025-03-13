package client;

import model.game.GameData;

import java.util.Collection;
import java.util.HashSet;

public class ServerFacade {
    private final ServerCommunicator serverCommunicator;
    private String authToken;
    private final String serverName;

    // this class acts like a mayor and tells the http communicator what to do and to carry out tasks.
    public ServerFacade(String serverName) {
        this.serverName = serverName;
        this.serverCommunicator = new HttpCommunicator(this, serverName);
    }

    public boolean register(String username, String password, String email){
        return serverCommunicator.register(username, password, email);
    }

    public boolean login(String username, String password){
        return serverCommunicator.login(username, password);
    }

    public boolean logout(){
        return serverCommunicator.logout();
    }

    public void reset() {
        serverCommunicator.reset();
    }

    public int createGame(String gameName){
        return serverCommunicator.createGame(gameName);
    }

    public HashSet<GameData> listGames() {
        return serverCommunicator.listGames();
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
