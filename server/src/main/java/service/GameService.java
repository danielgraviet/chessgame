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

    public void makeMove(int gameID, ChessMove move,
                         String username,
                         ChessGame.TeamColor playerColor) throws DataAccessException, InvalidMoveException {
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
            boolean isPlayer = (whitePlayerName != null && whitePlayerName.equals(username)
                    || blackPlayerName != null && blackPlayerName.equals(username));
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

    public void leaveGame(int gameID, String username) throws DataAccessException, InvalidMoveException {
        GameData currentGameData;
        try {
            currentGameData = gameDAO.getGameByID(gameID);
        } catch (DataAccessException e) {
            // Adding detail about the specific operation failing
            System.err.println("ERROR [GameService - leaveGame]: Failed to retrieve game data for gameID "
                    + gameID + ". Details: " + e.getMessage());
            throw e; // Re-throw original exception
        }

        if (currentGameData == null) {
            System.err.println("ERROR [GameService - leaveGame]: Attempted to leave non-existent game. GameID: "
                    + gameID + ", User: " + username);
            throw new DataAccessException("Game with ID " + gameID + " not found");
        }

        ChessGame game = currentGameData.game();
        // Check if game is over AFTER confirming game exists
//        if (game != null && game.isGameOver()) {
//            System.err.println("ERROR [GameService - leaveGame]: User '" + username +
//                    "' attempted to leave game " + gameID + ", but the game is already over.");
//            throw new InvalidMoveException("Cannot leave game; the game is already over.");
//            // Consider if InvalidMoveException is the right type here, maybe IllegalStateException? But sticking to your current signature.
//        }

        String whitePlayerName = currentGameData.whiteUsername();
        String blackPlayerName = currentGameData.blackUsername();
        GameData updatedGameData = null; // To store the potentially modified game data

        System.out.println("INFO [GameService - leaveGame]: Processing leave request for user '"
                + username + "' in game " + gameID + ".");
        System.out.println("INFO [GameService - leaveGame]: Current players - White: " +
                (whitePlayerName != null ? whitePlayerName : "[Empty]") + ", Black: " +
                (blackPlayerName != null ? blackPlayerName : "[Empty]"));


        if (username.equals(whitePlayerName)) {
            // White player is leaving
            System.out.println("INFO [GameService - leaveGame]: User '" + username +
                    "' is the WHITE player. Preparing to clear their slot in game " + gameID + ".");
            updatedGameData = new GameData(
                    currentGameData.gameID(),
                    null, // Clear white username
                    blackPlayerName,
                    currentGameData.gameName(),
                    currentGameData.game() // Keep the existing game state object
            );
        } else if (username.equals(blackPlayerName)) {
            // Black player is leaving
            System.out.println("INFO [GameService - leaveGame]: User '"
                    + username + "' is the BLACK player. Preparing to clear their slot in game "
                    + gameID + ".");
            updatedGameData = new GameData(
                    currentGameData.gameID(),
                    whitePlayerName,
                    null, // Clear black username
                    currentGameData.gameName(),
                    currentGameData.game() // Keep the existing game state object
            );
        } else {
            // User is not a player (observer or other)
            System.out.println("INFO [GameService - leaveGame]: User '" +
                    username + "' is not an assigned player (Observer or other) in game " +
                    gameID + ". No persistent game data change needed.");
            // No update needed, so we exit the method successfully.
            return;
        }

        // Only proceed to update if updatedGameData was actually created (meaning a player left)
        if (updatedGameData != null) {
            try {
                System.out.println("INFO [GameService - leaveGame]: Attempting to update database for game " +
                        gameID + " after player '" + username + "' left.");
                gameDAO.updateGame(updatedGameData);
                System.out.println("SUCCESS [GameService - leaveGame]: Game " + gameID +
                        " data successfully updated in database after player '" + username + "' left.");
            } catch (DataAccessException e) {
                // Provide context for the update failure
                System.err.println("ERROR [GameService - leaveGame]: Failed to update game data in database for gameID "
                        + gameID + " after user '" + username + "' left. Details: " + e.getMessage());
                // Print stack trace to standard error for more debugging info
                e.printStackTrace(System.err);
                // Re-throw the original exception type as expected by the method signature
                throw new DataAccessException("Failed to update game state after player left: " + e.getMessage());
                // Avoid wrapping in RuntimeException unless that's the desired contract
            }
        }
    }

    public void clear() throws DataAccessException {
        gameDAO.clear();
    }
}
