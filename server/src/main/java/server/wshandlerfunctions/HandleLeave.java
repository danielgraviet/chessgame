package server.wshandlerfunctions;

import chess.ChessGame;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.*;
import model.auth.AuthData;
import model.game.GameData;
import org.eclipse.jetty.websocket.api.Session;
import service.GameService;
import server.ConnectionManager;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

import java.io.IOException;

public class HandleLeave {

    private final GameService gameService;
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final ConnectionManager connectionManager;
    private final HelperFunctions helperFunctions;
    private final Gson gson = new Gson();

    public HandleLeave(GameService gameService,
                       AuthDAO authDAO,
                       GameDAO gameDAO,
                       ConnectionManager connectionManager,
                       HelperFunctions helperFunctions) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.connectionManager = connectionManager;
        this.helperFunctions = helperFunctions;
    }

    public void handle(Session session, UserGameCommand command) throws IOException {
        String logPrefix = "[DEBUG HandleLeave] ";

        // basic validation with invalidTokenAndId function
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username = null;

        System.out.println(logPrefix + "Received LEAVE command. GameID: " + gameID + ", AuthToken ending: ..." + authToken);

        if (this.helperFunctions.invalidTokenAndID(gameID, authToken, session)) {
            System.err.println(logPrefix + "Validation failed (invalidTokenAndID). Aborting for GameID: " + gameID + ", AuthToken ending: ..." + authToken);
            return;
        }
        // use a try catch block for potential errors
        try {
            // use authToken to get authData. and retrieve the user name.
            AuthData authData = authDAO.getUser(authToken);
            if (authData == null) {
                System.err.println(logPrefix + "Auth token invalid or expired (ending: ..." + authToken + "). Sending error.");
                this.helperFunctions.sendError(session, "Error: invalid or expired auth.");
                return;
            }

            // store the username
            username = authData.username();
            System.out.println(logPrefix + "Auth token valid. Username: " + username);

            // fetch the game data
            System.out.println(logPrefix + "Fetching GameData for gameID: " + gameID);
            GameData gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                this.helperFunctions.sendError(session, "Error: Game ID " + gameID + "does not exist.");
                return;
            }
            System.out.println(logPrefix + "GameData found for gameID: " + gameID);

            System.out.println(logPrefix + "Attempting to remove connection for user '" + username + "', gameID: " + gameID);
            connectionManager.removeConnection(gameID, authToken);
            System.out.println(logPrefix + "Connection removed (or confirmed not present) for user '" + username + "', gameID: " + gameID);

            System.out.println(logPrefix + "Attempting to get ChessGame object from GameData.");
            ChessGame game = gameData.game();
            boolean isGameOver = false;

            // check if game exists
            if (game == null) {
                System.err.println(logPrefix + "WARNING: ChessGame object in GameData is NULL for gameID: " + gameID + ". Treating as effectively over.");
                isGameOver = true;
            } else {
                System.out.println(logPrefix + "ChessGame object obtained. Checking its 'isGameOver' status.");
                isGameOver = game.isGameOver();
                System.out.println(logPrefix + "Result of game.isGameOver() for gameID " + gameID + ": " + isGameOver);
            }

            // assume the game is active, bc resign does not affect it.
            if (!isGameOver) {
                System.out.println(logPrefix + "Game is determined to be ACTIVE. Calling gameService.leaveGame for user '" + username + "', gameID: " + gameID);
                try {
                    // leave the game
                    gameService.leaveGame(gameID, username);
                    System.out.println(logPrefix + "gameService.leaveGame completed successfully for user '" + username + "'.");
                } catch (InvalidMoveException | DataAccessException | IllegalStateException e) {
                    System.err.println(logPrefix + "ERROR during gameService.leaveGame for ACTIVE game " + gameID + " by user '" + username + "': " + e.getMessage());
                    this.helperFunctions.sendError(session, "Error: Failed to update game state on leave - " + e.getMessage());
                }
            } else {
                System.out.println(logPrefix + "Game is determined to be OVER. Skipping call to gameService.leaveGame.");
            }

            // moved broadcast message here, bc it should always send.
            String notificationText = String.format("'%s' has left the game.", username);
            NotificationMessage notification = new NotificationMessage(notificationText);
            String notificationJson = gson.toJson(notification);
            System.out.println(logPrefix + "Broadcasting leave notification to others for game " + gameID + " (User: '" + username + "')");
            this.helperFunctions.broadcastMessage(notificationJson, gameID, authToken);

            System.out.println(logPrefix + "Successfully finished processing LEAVE request pathway for user '" + username + "' and game " + gameID + ".");

        } catch (DataAccessException e) {
            System.err.println(logPrefix + "DataAccessException during initial fetch for game " + gameID + ", user '" + (username != null ? username : "UNKNOWN using token ending ..." + authToken) + "': " + e.getMessage());
            e.printStackTrace(System.err);
            this.helperFunctions.sendError(session, "Error leaving game: Database error during lookup. " + e.getMessage());
        } catch (IOException e) {
            System.err.println(logPrefix + "IOException during message send/broadcast for user '" + (username != null ? username : "UNKNOWN using token ending ..." + authToken) + "', game " + gameID + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println(logPrefix + "Unexpected error processing leave for game " + gameID + ", user '" + (username != null ? username : "UNKNOWN using token ending ..." + authToken) + "': " + e.getMessage());
            e.printStackTrace(System.err);
            try {
                if (session.isOpen()) {
                    this.helperFunctions.sendError(session, "An unexpected server error occurred while processing your leave request.");
                } else {
                    System.err.println(logPrefix + "Session was closed, cannot send final unexpected error message to client.");
                }
            } catch (IOException ioex) {
                System.err.println(logPrefix + "IOException trying to send final unexpected error message: " + ioex.getMessage());
            }
        } finally {
            System.out.println(logPrefix + "Finished handle method execution for LEAVE command. GameID: " + gameID + ", AuthToken ending: ..." + authToken);
        }
    }
}