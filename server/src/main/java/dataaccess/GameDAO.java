package dataaccess;

import chess.ChessGame;
import model.game.GameData;

public interface GameDAO {
    // basic crud operations
    int createGame(String authToken, String gameName) throws DataAccessException;
    GameData getGameByID(int gameID) throws DataAccessException;
    void updateGame(GameData gameData) throws DataAccessException;
}
