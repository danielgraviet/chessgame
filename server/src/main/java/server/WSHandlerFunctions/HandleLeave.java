package server.WSHandlerFunctions;

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

    public HandleLeave(GameService gameService, AuthDAO authDAO, GameDAO gameDAO, ConnectionManager connectionManager, HelperFunctions helperFunctions) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.connectionManager = connectionManager;
        this.helperFunctions = helperFunctions;
    }

    public void handle(Session session, UserGameCommand command) throws IOException {
        // basic validation with invalidTokenAndId function
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username = null;
        if (this.helperFunctions.invalidTokenAndID(gameID, authToken, session)) {
            return;
        }


        // use a try catch block for potential errors
        try {
            // use authToken to get authData. and retrieve the user name.
            AuthData authData = authDAO.getUser(authToken);
            if (authData == null) {
                this.helperFunctions.sendError(session, "Error: invalid or expired auth.");
                return;
            }

            // store the username
            username = authData.username();

            // fetch the game data
            GameData gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                this.helperFunctions.sendError(session, "Error: Game ID " + gameID + "does not exist.");
            }

            // update the game. crucial part
            gameService.leaveGame(gameID, username);
            System.out.println("INFO [WSHandler - handleLeave]: GameService successfully processed leave for user '" + username + "', gameID: " + gameID);

            connectionManager.removeConnection(gameID, authToken);

            // notify if successful
            String notificationText = String.format("'%s' has left the game.", username);
            NotificationMessage notification = new NotificationMessage(notificationText);
            String notificationJson = gson.toJson(notification);

            // Use the updated broadcastMessage, excluding the leaver by authToken
            this.helperFunctions.broadcastMessage(notificationJson, gameID, authToken);
            System.out.println("INFO [WSHandler - handleLeave]: Broadcasted leave notification to others in game " + gameID);

            System.out.println("SUCCESS [WSHandler - handleLeave]: User '" + username + "' successfully left game " + gameID + ".");

        } catch (InvalidMoveException e) { // Or IllegalStateException depending on your service
            System.err.println("WARN [WSHandler - handleLeave]: Invalid leave attempt by user '" + username + "' for game " + gameID + ": " + e.getMessage());
            this.helperFunctions.sendError(session, "Error: Cannot leave game - " + e.getMessage());
        } catch (DataAccessException e) {
            System.err.println("ERROR [WSHandler - handleLeave]: Data access error processing leave for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace(System.err);
            this.helperFunctions.sendError(session, "Error leaving game: Database error. " + e.getMessage());
        } catch (IOException e) {
            // Errors during WebSocket send/broadcast
            System.err.println("ERROR [WSHandler - handleLeave]: IOException during broadcast/send for user '" + username + "', game " + gameID + ": " + e.getMessage());
            // Don't try to sendError back if the connection is likely broken
        } catch (Exception e) { // Catch-all for unexpected issues
            System.err.println("ERROR [WSHandler - handleLeave]: Unexpected error processing leave for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace(System.err);
            try {
                // Try to inform the user if the session is still open
                this.helperFunctions.sendError(session, "An unexpected server error occurred while processing your leave request.");
            } catch (IOException ioex) { /* If sending error fails, nothing more to do */ }
        }
    }
}