package server.WSHandlerFunctions;

import chess.ChessGame;
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

public class HandleConnect {

    private final GameService gameService;
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final ConnectionManager connectionManager;
    private final HelperFunctions helperFunctions;
    private final Gson gson = new Gson();

    public HandleConnect(GameService gameService, AuthDAO authDAO, GameDAO gameDAO, ConnectionManager connectionManager, HelperFunctions helperFunctions) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.connectionManager = connectionManager;
        this.helperFunctions = helperFunctions;
    }

    public void handle(Session session, UserGameCommand command) throws IOException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken() != null ? command.getAuthToken() : "anonymous";

        if (this.helperFunctions.invalidTokenAndID(gameID, authToken, session)) {
            return;
        }

        connectionManager.addConnection(gameID, authToken, session);

        ChessGame game = null;
        GameData gameData = null;
        AuthData authData = null;
        String username = "UnknownUser";
        boolean connectionAdded = false;

        try {

            authData = authDAO.getUser(authToken);
            if (authData == null) {
                this.helperFunctions.sendError(session, "Error: Invalid or expired authentication token.");
                return;
            }
            username = authData.username();
            System.out.println("User '" + username + "' attempting to connect to game " + gameID);


            gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                this.helperFunctions.sendError(session, "Error: Game ID " + gameID + " does not exist.");
                return;
            }

            // null pointer exception here.
            game = gameService.getGame(gameID);
            if (game == null) {
                System.err.println("CRITICAL: GameData found but GameService could not load game " + gameID);
                this.helperFunctions.sendError(session, "Error: Could not load game logic/state for game ID " + gameID + ".");
                return;
            }

            connectionManager.addConnection(gameID, authToken, session);
            connectionAdded = true; // Set flag indicating connection was added
            System.out.println("INFO [WSHandler - handleConnect]: Connection added for user '" + username + "' to game " + gameID);

            String playerRole = "Observer";
            if (gameData.whiteUsername() != null && username.equals(gameData.whiteUsername())) {
                playerRole = "WHITE";
                // is this just supposed to do on or the other? or both?
            } else if (gameData.blackUsername() != null && username.equals(gameData.blackUsername())) {
                playerRole = "BLACK";
            }
            System.out.println("User '" + username + "' assigned role: " + playerRole + " for game " + gameID);


            LoadGameMessage loadGameMessage = new LoadGameMessage(gameData);
            String loadGameJson = gson.toJson(loadGameMessage);
            this.helperFunctions.sendMessage(session, loadGameJson);
            System.out.println("Sent LOAD_GAME to connecting client: " + username);

            String eventType = "JOIN";
            String notificationTeamColor;

            switch (playerRole) {
                case "WHITE":
                    notificationTeamColor = "WHITE";
                    break;
                case "BLACK":
                    notificationTeamColor = "BLACK";
                    break;
                default: // Observer
                    notificationTeamColor = null;
                    break;
            }

            String notificationText = String.format("'%s' has joined the game as %s.", username, playerRole);
            NotificationMessage notification = new NotificationMessage(notificationText);
            String notificationJson = gson.toJson(notification);
            this.helperFunctions.broadcastMessage(notificationJson, gameID, authToken);
            System.out.println("Broadcast JOIN NOTIFICATION to game " + gameID + ": user=" + username + ", role=" + playerRole);

        } catch (DataAccessException e) {
            System.err.println("Data access error during connect for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            this.helperFunctions.sendError(session, "Error connecting to game due to data access issue: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error during connect/broadcast for game " + gameID + ", user '" + username + "': " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during connect for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            this.helperFunctions.sendError(session, "An unexpected server error occurred while connecting.");
            if (connectionAdded) {
                connectionManager.removeConnection(gameID, authToken);
            }
        }
    }
}