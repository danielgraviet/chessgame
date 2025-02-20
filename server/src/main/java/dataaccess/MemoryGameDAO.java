package dataaccess;

import chess.ChessGame;
import model.game.GameData;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;


public class MemoryGameDAO implements GameDAO {

    // DAOs should only CRUD
    private final HashSet<GameData> gameStorage = new HashSet<>();
    private static final AtomicInteger NextGameId = new AtomicInteger(1);

    // CREATE
    public int createGame(String authToken, String gameName) throws DataAccessException {
        // game data requires ID, str white user, str black user, str gameName, ChessGame game object
        // generate a unique ID
        if (!gameStorage.isEmpty()) {
            for (GameData gameData : gameStorage) {
                if (gameData.gameName().equals(gameName)) {
                    throw new DataAccessException("Game name already exists");
                }
            }
        }

        int gameID = NextGameId.getAndIncrement();

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

    public Collection<GameData> getAllGames() {
        return gameStorage;
    }

    // CLEAR
    public void clear() throws DataAccessException {
        gameStorage.clear();
        NextGameId.set(1);
    }
}
