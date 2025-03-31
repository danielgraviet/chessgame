package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.SqlGameDAO;
import model.auth.AuthData;
import model.game.GameData;
import org.eclipse.jetty.server.Authentication;

import java.util.Collection;

public class GameService {
    AuthDAO authDAO;
    GameDAO gameDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        if (gameDAO == null) {
            throw new IllegalArgumentException("userDAO is null");
        }
        if (authDAO == null) {
            throw new IllegalArgumentException("authDAO is null");
        }
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        if (authDAO.getUser(authToken) == null) {
            throw new DataAccessException("Invalid token.");
        }

        return gameDAO.createGame(authToken, gameName);
    }

    // create the joinGame
    public void joinGame(String authToken, int gameID, ChessGame.TeamColor teamColor) throws DataAccessException {
        AuthData user = authDAO.getUser(authToken);
        if (user == null) {
            throw new DataAccessException("Invalid token.");
        }

        GameData game = gameDAO.getGameByID(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found.");
        }

        String username = user.username();

        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("White user already exists.");
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    username,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );

            System.out.println("DEBUG/ Before update: gameID=" + gameID + ", whiteUsername=" + username);
            gameDAO.updateGame(updatedGame);
            System.out.println("DEBUG/ After update: whiteUsername=" + gameDAO.getGameByID(gameID).whiteUsername());
        } else if (teamColor == ChessGame.TeamColor.BLACK) {
            if (game.blackUsername() != null) {
                throw new DataAccessException("Black user already exists.");
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    username,
                    game.gameName(),
                    game.game()
            );
            gameDAO.updateGame(updatedGame);
        } else {
            throw new DataAccessException("Invalid team color.");
        }
    }

    public Collection<GameData> getAllGames(String authToken) throws DataAccessException {
        if (authDAO.getUser(authToken) == null) {
            throw new DataAccessException("Invalid token.");
        } else {
            return gameDAO.getAllGames();
        }
    }

    public ChessGame getGame(int gameID) throws DataAccessException {
        GameData gameData = gameDAO.getGameByID(gameID);
        if (gameData == null) {
            throw new DataAccessException("Game with ID " + gameID + " not found");
        }
        ChessGame game = gameData.game();
        if (game == null) {
            throw new DataAccessException("Game state not initialized for ID " + gameID);
        }

        return game;
    }

    public void makeMove(int gameID, ChessMove move, String username, ChessGame.TeamColor playerColor) throws DataAccessException, InvalidMoveException {
        GameData currentGameData = gameDAO.getGameByID(gameID);
        if (currentGameData == null) {
            throw new DataAccessException("Game not found with ID: " + gameID);
        }

        ChessGame currentGame = currentGameData.game();
        if (currentGame == null) {
            throw new DataAccessException("Game state (ChessGame) is missing for game ID: " + gameID);
        }

        if (currentGame.isGameOver()) {
            throw new InvalidMoveException("Cannot make move; the game is already over.");
        }

        if (currentGame.getTeamTurn() != playerColor) {
            throw new InvalidMoveException("It is currently " + currentGame.getTeamTurn() + "'s turn, not " + playerColor + "'s.");
        }

        String expectedUsername = (playerColor == ChessGame.TeamColor.WHITE) ? currentGameData.whiteUsername() : currentGameData.blackUsername();
        if (!username.equals(expectedUsername)) {
            System.err.printf("Authorization Error: User '%s' attempting move as %s, but expected user is '%s' for game %d%n",
                    username, playerColor, expectedUsername, gameID);
            throw new InvalidMoveException("User '" + username + "' is not authorized to make moves for " + playerColor + " in this game.");
        }

        currentGame.makeMove(move); // This modifies the currentGame object in memory

        GameData updatedGameData = new GameData(
                currentGameData.gameID(),
                currentGameData.whiteUsername(),
                currentGameData.blackUsername(),
                currentGameData.gameName(),
                currentGame // Pass the game object that was just modified by currentGame.makeMove(move)
        );

        gameDAO.updateGame(updatedGameData);
        System.out.printf("GameService: Move %s successful for user '%s' in game %d. State saved.%n", move, username, gameID);
    }

    public void resignGame(int gameID, String username) throws DataAccessException, InvalidMoveException {
        // ends the game, no more moves can be made
        GameData currentGameData = null;
        ChessGame currentGame = null;

        // retrieve game data
        try {
            GameData gameData = gameDAO.getGameByID(gameID);
            // validate the game exists
            if (gameData == null) {
                throw new DataAccessException("Game with ID " + gameID + " not found");
            }

            // check if the game is over
            currentGame = gameData.game();

            if (currentGame == null) {
                System.err.println("CRITICAL ERROR: Game state (ChessGame) is null for game ID: " + gameID + " during resignation attempt.");
                throw new DataAccessException("Game state (Game) is missing for game ID: " + gameID);
            }
            if (currentGame.isGameOver()) {
                throw new InvalidMoveException("Cannot resign game; the game is already over.");
            }

            currentGameData = gameDAO.getGameByID(gameID);

            // authorize the user
            String whitePlayerName = currentGameData.whiteUsername();
            String blackPlayerName = currentGameData.blackUsername();
            boolean isPlayer = (whitePlayerName != null && whitePlayerName.equals(username) || blackPlayerName != null && blackPlayerName.equals(username));
            if (!isPlayer) {
                throw new IllegalStateException("User '" + username + "' is not authorized to resign game.");
            }

            System.out.printf("GameService: User '%s' is resigning from game %d.%n", username, gameID);
            currentGame.setResigned(true);

            GameData updatedGameData = new GameData(
                    currentGameData.gameID(),
                    currentGameData.whiteUsername(),
                    currentGameData.blackUsername(),
                    currentGameData.gameName(),
                    currentGame
            );

            gameDAO.updateGame(updatedGameData);
            System.out.println("Send updated game due to resignation attempt: " + updatedGameData);

        } catch (DataAccessException e) {
            throw new DataAccessException("Error with getting game." + e.getMessage());
        }
    }

    public void clear() throws DataAccessException {
        gameDAO.clear();
    }
}
