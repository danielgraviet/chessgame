package dataaccess;

import chess.ChessGame;
import model.game.GameData;
import java.util.Collection;

public interface GameDAO {
    // basic crud operations
    int createGame(String authToken, String gameName) throws DataAccessException;
    GameData getGameByID(int gameID) throws DataAccessException;
    void updateGame(GameData gameData) throws DataAccessException;
    Collection<GameData> getAllGames() throws DataAccessException;
    void clear() throws DataAccessException;
}
