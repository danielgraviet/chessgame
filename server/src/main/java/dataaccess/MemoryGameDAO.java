package dataaccess;

import chess.ChessGame;
import model.game.GameData;
import dataaccess.MemoryAuthDAO;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


public class MemoryGameDAO implements GameDAO {

    // DAOs should only CRUD
    private final HashSet<GameData> gameStorage = new HashSet<>();
    private static final AtomicInteger nextGameID = new AtomicInteger(1);

    // CREATE
    public int createGame(String authToken, String gameName) throws DataAccessException {
        // game data requires ID, str white user, str black user, str gameName, ChessGame game object
        // generate a unique ID
        int gameID = nextGameID.getAndIncrement();

        // enter names for white and black user
        String blackUser = "";
        String whiteUser = "";

        // pass in and set the game name,

        // create a new chessGame Object.
        ChessGame chessGame = new ChessGame();
        GameData game = new GameData(gameID, whiteUser, blackUser, gameName, chessGame);
        gameStorage.add(game);
        return gameID;
    }

    // READ
    // UPDATE
    // DELETE
}
