package dataaccess;

import chess.ChessGame;
import model.game.GameData;
import dataaccess.MemoryAuthDAO;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;


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
        String blackUsername = null;
        String whiteUsername = null;

        // pass in and set the game name,

        // create a new chessGame Object.
        ChessGame chessGame = new ChessGame();
        GameData game = new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame);
        gameStorage.add(game);
        return gameID;
    }

    // READ
    public int getGame(String authToken, String gameName) throws DataAccessException {
        for (GameData game: gameStorage) {
            if (game.gameName().equals(gameName)) {
                return game.gameID();
            }
        }
        throw new DataAccessException("Game not found");
    }

    public GameData getGameByID(int gameID) throws DataAccessException {
        for (GameData game: gameStorage) {
            if (game.gameID() == gameID) {
                return game;
            }
        }
        throw new DataAccessException("Game not found");
    }

    // UPDATE
    public void updateGame(GameData gameData) throws DataAccessException {
        boolean removed = gameStorage.removeIf(game -> game.gameID() == gameData.gameID());
        if (removed) {
            gameStorage.add(gameData);
        } else {
            throw new DataAccessException("Game not found for update.");
        }
    }

    /* TODO:
    * implement the getAllGames function inside the gameService.java
    * how does can it use the DAO's to do that?
    * will it return a collection of GameData objects?
    * do I need to make a function in this file, to return the GameData objects
    * */

    public Collection<GameData> getAllGames() throws DataAccessException {
        return gameStorage;
    }
    // DELETE

    // CLEAR
    public void clear() throws DataAccessException {
        gameStorage.clear();
        nextGameID.set(1);
    }

}
