package server.WSHandlerFunctions;

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

public class HandleResign {

    private final GameService gameService;
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final ConnectionManager connectionManager;
    private final HelperFunctions helperFunctions;
    private final Gson gson = new Gson();

    public HandleResign(GameService gameService, AuthDAO authDAO, GameDAO gameDAO, ConnectionManager connectionManager, HelperFunctions helperFunctions) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.connectionManager = connectionManager;
        this.helperFunctions = helperFunctions;
    }

    public void handle(Session session, UserGameCommand command) throws IOException {
        // basic validation
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username = null;
        if (this.helperFunctions.invalidTokenAndID(gameID, authToken, session)) {
            return;
        }


        try {
            AuthData authData = authDAO.getUser(authToken);
            if (authData == null) {
                this.helperFunctions.sendError(session, "Error: invalid or expired auth.");
                return;
            }

            username = authData.username();

            GameData gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                this.helperFunctions.sendError(session, "Error: Game ID " + gameID + "does not exist.");
                return;
            }

            gameService.resignGame(gameID, username);

            String notificationText = String.format("'%s' has resigned. The game is over.", username);
            NotificationMessage notification = new NotificationMessage(notificationText);
            String notificationJson = gson.toJson(notification);

            this.helperFunctions.broadcastMessage(notificationJson, gameID, null);

        } catch (IllegalStateException e) {
            System.err.printf("WARN [HandleResign]: IllegalStateException during resign for game %d, user '%s': %s%n",
                    gameID,
                    (username != null ? username : "[Auth Failed]"),
                    e.getMessage());
            this.helperFunctions.sendError(session, "Error: Cannot resign - " + e.getMessage());

        } catch (DataAccessException e) {
            System.err.printf("ERROR [HandleResign]: DataAccessException during resign for game %d, user '%s': %s%n",
                    gameID,
                    (username != null ? username : "[Auth Failed]"),
                    e.getMessage());
            e.printStackTrace(System.err);
            this.helperFunctions.sendError(session, "Error resigning due to a data access issue.");

        } catch (IOException e) {
            System.err.printf("ERROR [HandleResign]: IOException during resign processing/broadcast for game %d: %s%n",
                    gameID,
                    e.getMessage());

        } catch (Exception e) {
            System.err.printf("ERROR [HandleResign]: Unexpected error during resign for game %d, user '%s': %s%n",
                    gameID,
                    (username != null ? username : "[Unknown]"),
                    e.getMessage());
            e.printStackTrace(System.err);
            try {
                this.helperFunctions.sendError(session, "An unexpected server error occurred while processing your resignation.");
            } catch (IOException sendEx) {
                System.err.println("ERROR [HandleResign]: Failed to send error message back to client after unexpected error: " + sendEx.getMessage());
            }
        }
    }
}