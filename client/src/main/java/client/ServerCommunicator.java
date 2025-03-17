package client;

import java.util.HashSet;
import model.game.GameData;

public interface ServerCommunicator {
    boolean register(String username, String password, String email);
    boolean login(String username, String password);
    boolean logout();
    void reset();
    int createGame(String gameName);
    HashSet<GameData> listGames();
    GameData getGame(int gameId);
    boolean joinGame(int gameId, String playerColor);
}
