package client;

import com.google.gson.Gson;
import model.game.GameData;

import java.util.HashSet;

public class HttpCommunicator implements ServerCommunicator {
    private final String baseUrl;
    private final ServerFacade facade;
    private final Gson gson = new Gson();

    public HttpCommunicator(ServerFacade facade,  String serverName) {
        this.baseUrl = "http://" + serverName;
        this.facade = facade;
    }

    public boolean register(String username, String password, String email) {
        return false;
    }

    public boolean login(String username, String password) {
        return false;
    }

    public boolean logout(){
        return false;
    }

    public int createGame(String gameName) {
        return 0;
    }

    public HashSet<GameData> listGames() {
        return null;
    }

    public boolean joinGame(int gameId, String playerColor) {
        return false;
    }

}
