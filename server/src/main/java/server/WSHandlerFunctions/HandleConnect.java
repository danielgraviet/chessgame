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
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HandleConnect {

    private final GameService gameService;
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final ConnectionManager connectionManager;
    private final Gson gson = new Gson();

    public HandleConnect(GameService gameService, AuthDAO authDAO, GameDAO gameDAO, ConnectionManager connectionManager) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
        this.connectionManager = connectionManager;
    }

    public void handle(Session session, UserGameCommand command) throws IOException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken() != null ? command.getAuthToken() : "anonymous";

        if (invalidTokenAndID(gameID, authToken, session)) {
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
                sendError(session, "Error: Invalid or expired authentication token.");
                return;
            }
            username = authData.username();
            System.out.println("User '" + username + "' attempting to connect to game " + gameID);


            gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                sendError(session, "Error: Game ID " + gameID + " does not exist.");
                return;
            }

            // null pointer exception here.
            game = gameService.getGame(gameID);
            if (game == null) {
                System.err.println("CRITICAL: GameData found but GameService could not load game " + gameID);
                sendError(session, "Error: Could not load game logic/state for game ID " + gameID + ".");
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
            sendMessage(session, loadGameJson);
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
            broadcastMessage(notificationJson, gameID, authToken);
            System.out.println("Broadcast JOIN NOTIFICATION to game " + gameID + ": user=" + username + ", role=" + playerRole);

        } catch (DataAccessException e) {
            System.err.println("Data access error during connect for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Error connecting to game due to data access issue: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error during connect/broadcast for game " + gameID + ", user '" + username + "': " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during connect for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            sendError(session, "An unexpected server error occurred while connecting.");
            if (connectionAdded) {
                connectionManager.removeConnection(gameID, authToken);
            }
        }
    }


    private void sendMessage(Session session, String message) throws IOException {
        if (session != null && session.isOpen()) {
            session.getRemote().sendString(message);
        } else {
            System.err.println("WARN [HandleConnect]: Attempted to send message to null or closed session.");
        }
    }

    private void sendError(Session session, String errorMessage) throws IOException {
        ErrorMessage error = new ErrorMessage(errorMessage);
        String jsonResponse = gson.toJson(error);
        sendMessage(session, jsonResponse);
        System.out.println("INFO [HandleConnect]: Sent error to client: " + jsonResponse);
    }

    private void broadcastMessage(String message, int gameID, String excludedAuthToken) throws IOException {
        if (this.connectionManager == null) {
            System.err.println("CRITICAL ERROR [HandleConnect - broadcastMessage]: ConnectionManager is null!");
            return;
        }
        ConcurrentHashMap<String, Session> gameConnections = this.connectionManager.getConnectionsForGame(gameID);
        if (gameConnections == null || gameConnections.isEmpty()) {
            return;
        }

        var exceptions = new ArrayList<IOException>();
        for (Map.Entry<String, Session> entry : gameConnections.entrySet()) {
            String currentAuthToken = entry.getKey();
            Session currentSession = entry.getValue();
            if (excludedAuthToken != null && excludedAuthToken.equals(currentAuthToken)) { continue; }
            try {
                this.sendMessage(currentSession, message);
            } catch (IOException e) {
                exceptions.add(e);
                System.err.println("ERROR [HandleConnect - broadcastMessage]: IOException sending to session " + currentSession.hashCode() + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("ERROR [HandleConnect - broadcastMessage]: Unexpected error sending to session " + currentSession.hashCode() + ": " + e.getMessage());
                e.printStackTrace(System.err);
                exceptions.add(new IOException("Unexpected send error", e));
            }
        }
        if (!exceptions.isEmpty()) {
            System.err.println("ERROR [HandleConnect - broadcastMessage]: Encountered " + exceptions.size() + " IOExceptions during broadcast to game " + gameID);
        }
    }

    private boolean invalidTokenAndID(Integer gameID, String authToken, Session session) throws IOException {
        if (authToken == null || authToken.isBlank()) {
            sendError(session, "Error: Missing or invalid authToken");
            return true;
        }
        if (gameID == null) {
            sendError(session, "Error: Missing or invalid gameID");
            return true;
        }
        return false;
    }

}