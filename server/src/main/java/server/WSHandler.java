package server;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dataaccess.*;
import model.auth.AuthData;
import model.game.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.WSHandlerFunctions.HandleConnect;
import service.GameService;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WSHandler {
    private static final Gson gson = new Gson();
    private static final Logger log = LoggerFactory.getLogger(WSHandler.class);
    private static GameService gameService;
    private static AuthDAO authDAO = new SqlAuthDAO();
    private static GameDAO gameDAO = new SqlGameDAO();
    private static ConnectionManager connectionManager;
    private static HandleConnect handleConnect;

    public static void setGameService(GameService service) {
        gameService = service;
    }

    public static void setAuthDAO(AuthDAO dao) {
        authDAO = dao;
    }

    public static void setGameDAO(GameDAO dao) {
        gameDAO = dao;
    }

    public static void setConnectionManager(ConnectionManager manager) { // Add setter
        connectionManager = manager;
    }

    public static void setHandleConnect(HandleConnect handleConnect) {
        WSHandler.handleConnect = handleConnect;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        if (session == null) {
            System.err.println("onConnect: Session is null");
            return;
        }
        System.out.println("INFO [WSHandler - onConnect]: WebSocket connection opened. Session ID: " + session.hashCode() + ", Remote: " + session.getRemoteAddress());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        if (session == null) {
            System.err.println("onMessage: Session is null");
            return;
        }

        System.out.println("Message received: " + message);

        try {
            UserGameCommand baseCommand;
            try {
                baseCommand = gson.fromJson(message, UserGameCommand.class);
                if (baseCommand.getCommandType() == null) {
                    sendError(session, "Error: Missing or invalid commandType field in message.");
                    return;
                }
            } catch (JsonSyntaxException e) {
                sendMessage(session, "Error: Invalid command format - " + e.getMessage());
                return;
            }
            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    handleConnect.handle(session, baseCommand);
                    break;
                case MAKE_MOVE:
                    try {
                        // Deserialize the SAME message string, but now into the specific subclass
                        MakeMoveCommand makeMoveCommand = gson.fromJson(message, MakeMoveCommand.class);
                        // Basic check if move field deserialized correctly (might be null if JSON was wrong)
                        if (makeMoveCommand.getMove() == null) {
                            sendError(session, "Error: Missing or invalid 'move' field for MAKE_MOVE command.");
                            return;
                        }
                        // Now pass the correctly typed object
                        handleMakeMove(session, makeMoveCommand);
                    } catch (JsonSyntaxException e) {
                        System.err.println("Error deserializing MAKE_MOVE: " + e.getMessage());
                        sendError(session, "Error: Invalid format for MAKE_MOVE command.");
                    }
                    break;
                case LEAVE:
                    handleLeave(session, baseCommand);
                    break;
                case RESIGN:
                    handleResign(session, baseCommand);
                    break;
                default:
                    System.err.println("Unknown command type received: " + baseCommand.getCommandType());
                    sendError(session, "Error: Unknown command type '" + baseCommand.getCommandType() + "'");
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        } catch (Exception e) {
            // Catch unexpected errors during command processing
            System.err.println("Unexpected error processing message: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            try {
                sendError(session, "An unexpected server error occurred.");
            } catch (IOException ioEx) {
                System.err.println("Failed to send error message after unexpected error: " + ioEx.getMessage());
            }
        }
    }


    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        if (session == null) {
            System.err.println("onClose: Session is null");
            return;
        }

        String closeReason = (reason != null) ? reason : "Unknown reason";
        System.out.println("Client disconnected: " + closeReason + " (Status code: " + statusCode + ")");

        switch (statusCode) {
            case 1005:
                System.out.println("Note: 1005 means no status code was received from the client");
                break;
                case 1006:
                    System.out.println("Note: 1006 means weird closure where connection was lost.");
                    break;
        }

        if (connectionManager != null) {
            connectionManager.removeSession(session);
        } else {
            System.err.println("ERROR [WSHandler - onClose]: ConnectionManager is null. Cannot remove session " + session.hashCode());
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String errorMessage = (error != null && error.getMessage() != null) ? error.getMessage() : "Unknown error";
        String sessionId = (session != null) ? String.valueOf(session.hashCode()) : "[Unknown Session]";
        System.err.println("ERROR [WSHandler - onError]: WebSocket error for session " + sessionId + ": " + errorMessage);

        if (error != null) {
            error.printStackTrace(System.err); // Print stack trace to standard error
        }

        if (session != null && connectionManager != null) {
            System.out.println("INFO [WSHandler - onError]: Removing session " + sessionId + " from ConnectionManager due to error.");
            connectionManager.removeSession(session);
        } else if(connectionManager == null){
            System.err.println("ERROR [WSHandler - onError]: ConnectionManager is null. Cannot remove session " + sessionId);
        }
    }

    private void sendMessage(Session session, String message) throws IOException {
        if (session.isOpen()) {
            session.getRemote().sendString(message);
        }
    }

    private void sendError(Session session, String errorMessage) throws IOException {
        ErrorMessage error = new ErrorMessage(errorMessage);
        String jsonResponse = gson.toJson(error);
        sendMessage(session, jsonResponse);
        System.out.println("Sent error to client: " + jsonResponse);
    }

    private void broadcastMessage(String message, int gameID, String excludedAuthToken) {
        if (connectionManager == null) {
            System.err.println("broadcastMessage: connectionManager is null!");
            return;
        }

        ConcurrentHashMap<String, Session> gameConnections = connectionManager.getConnectionsForGame(gameID);

        // Check if there are any connections for this game
        if (gameConnections == null || gameConnections.isEmpty()) {
            System.out.println("INFO [WSHandler - broadcastMessage]: No active connections found for game " + gameID + ". Nothing broadcasted.");
            return;
        }

        String excludedTokenShort = (excludedAuthToken != null && excludedAuthToken.length() > 8) ? excludedAuthToken.substring(0, 8) + "..." : excludedAuthToken;
        System.out.println("DEBUG [WSHandler - broadcastMessage]: Broadcasting to game " + gameID + " (Excluding token: " + excludedTokenShort + "). Message glimpse: " + message.substring(0, Math.min(100, message.length())) + (message.length() > 100 ? "..." : ""));

        var exceptions = new ArrayList<IOException>();

        for (Map.Entry<String, Session> entry : gameConnections.entrySet()) {
            String currentAuthToken = entry.getKey();
            Session currentSession = entry.getValue();

            // Check if this client should be excluded based on authToken
            if (excludedAuthToken != null && excludedAuthToken.equals(currentAuthToken)) {
                // System.out.println("DEBUG [WSHandler - broadcastMessage]: Skipping excluded user token " + excludedTokenShort); // Can be noisy
                continue; // Skip this session
            }

            // Send the message if the session is open
            if (currentSession != null && currentSession.isOpen()) {
                try {
                    // System.out.println("DEBUG [WSHandler - broadcastMessage]: Sending message to session " + currentSession.hashCode() + " (Token: " + currentAuthToken.substring(0, Math.min(8, currentAuthToken.length())) + "...)"); // Can be noisy
                    currentSession.getRemote().sendString(message);
                } catch (IOException e) {
                    exceptions.add(e);
                    System.err.println("ERROR [WSHandler - broadcastMessage]: IOException sending to session " + currentSession.hashCode() + " (Token: " + currentAuthToken.substring(0, Math.min(8, currentAuthToken.length())) + "...): " + e.getMessage());
                    // Consider removing this specific broken connection?
                    // connectionManager.removeConnection(gameID, currentAuthToken); // Be careful with concurrent modification if not using iterator properly
                } catch (Exception e) {
                    // Catch unexpected errors during send
                    System.err.println("ERROR [WSHandler - broadcastMessage]: Unexpected error sending to session " + currentSession.hashCode() + " (Token: " + currentAuthToken.substring(0, Math.min(8, currentAuthToken.length())) + "...): " + e.getMessage());
                    e.printStackTrace(System.err);
                    exceptions.add(new IOException("Unexpected send error: " + e.getMessage(), e)); // Treat as IOException for reporting
                }
            } else {
                System.out.println("WARN [WSHandler - broadcastMessage]: Skipping closed or null session found in game " + gameID + " for token " + currentAuthToken.substring(0, Math.min(8, currentAuthToken.length())) + "...");
                // Optional: Clean up stale entries if session is closed but still in map
                // connectionManager.removeConnection(gameID, currentAuthToken);
            }
        }

        // Log if any errors occurred during the broadcast loop
        if (!exceptions.isEmpty()) {
            System.err.println("ERROR [WSHandler - broadcastMessage]: Encountered " + exceptions.size() + " IOExceptions during broadcast to game " + gameID);
            // You might choose to throw the first exception, or just log
            // throw exceptions.get(0); // Uncomment if you want the calling method to know about the failure
        } else {
            System.out.println("DEBUG [WSHandler - broadcastMessage]: Broadcast completed for game " + gameID);
        }
    }

    private void handleConnect(Session session, UserGameCommand command) throws IOException {
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

    private void handleMakeMove(Session session, MakeMoveCommand command) throws IOException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();

        if (invalidTokenAndID(gameID, authToken, session)) {
            return;
        };

        ChessMove move = command.getMove();

        ChessGame game = null;
        GameData gameData = null;
        String username = null;
        AuthData authData = null;

        if (gameID == null || authToken == null || authToken.isBlank() || move == null) {
            sendError(session, "Error: Missing required fields (gameID, authToken, move) for MAKE_MOVE.");
            return;
        }

        try {
            // authenticate the user
            authData = authDAO.getUser(authToken);
            if (authData == null) {
                sendError(session, "Error: Invalid or expired authentication token.");
                return;
            }

            // output the username
            username = authData.username();
            System.out.println("User '" + username + "' attempting to make a move in game " + gameID);

            // get the gameData
            gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                sendError(session, "Error: Game ID " + gameID + " does not exist.");
            }

            game = gameService.getGame(gameID);
            if (game == null) {
                System.err.println("CRITICAL: GameData found but GameService failed to load game " + gameID);
                sendError(session, "Error: Could not load game logic/state for game ID " + gameID + ".");
                return;
            }

            // authorize the move
            ChessGame.TeamColor playerColor = null;
            if (username.equals(gameData.whiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (username.equals(gameData.blackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            }

            if (playerColor == null) {
                sendError(session, "Error: Observers cannot make moves.");
                return;
            }

            if (game.getTeamTurn() != playerColor) {
                sendError(session, "Error: It's not your turn (" + game.getTeamTurn() + "'s turn).");
                return; // Stop processing
            }

            if (game.getTeamTurn() == null || game.isGameOver()) {
                sendError(session, "Error: The game is already over.");
                return;
            }

            // where the actual change of the game occurs.
            gameService.makeMove(gameID, move, username, playerColor);
            System.out.println("Move validated and executed successfully for game " + gameID);

            // assume it has been successful, so now we broadcast the updates.

            // get the updated game.
            ChessGame updatedGame = gameService.getGame(gameID);
            if (updatedGame == null) {
                System.err.println("CRITICAL: Game state is null after successful move and save for game " + gameID);
                sendError(session, "Internal server error after making move.");
                return;
            }

            LoadGameMessage loadGameMessage = new LoadGameMessage(gameData);
            String loadGameJson = gson.toJson(loadGameMessage);
            broadcastMessage(loadGameJson, gameID, null); // null = no exclusions
            System.out.println("Broadcast LOAD_GAME (after move) to all in game " + gameID);

            String moveDescription = String.format("'%s' (%s) moved %s from %s to %s%s.",
                    username,
                    playerColor,
                    updatedGame.getBoard().getPiece(move.getEndPosition()).getPieceType(),
                    move.getStartPosition().toString(),
                    move.getEndPosition().toString(),
                    move.getPromotionPiece() != null ? " promoting to " + move.getPromotionPiece() : ""); // Add promotion info

            NotificationMessage moveNotification = new NotificationMessage(moveDescription); // Use the simple Notification structure
            String moveNotificationJson = gson.toJson(moveNotification);
            broadcastMessage(moveNotificationJson, gameID, authToken); // Exclude the player who made the move
            System.out.println("Broadcast MOVE NOTIFICATION to others in game " + gameID);


            ChessGame.TeamColor opponentColor = (playerColor == ChessGame.TeamColor.WHITE) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            String opponentUsername = (playerColor == ChessGame.TeamColor.WHITE) ? gameData.blackUsername() : gameData.whiteUsername();
            opponentUsername = (opponentUsername == null) ? "[Opponent]" : "'" + opponentUsername + "'"; // Handle case where opponent spot might be empty

            String stateNotificationText = null;
            if (updatedGame.isInCheckmate(opponentColor)) {
                stateNotificationText = String.format("CHECKMATE! %s (%s) defeated %s (%s).", username, playerColor, opponentUsername, opponentColor);
            } else if (updatedGame.isInStalemate(opponentColor)) {
                stateNotificationText = "STALEMATE! The game is a draw.";
            } else if (updatedGame.isInCheck(opponentColor)) {
                stateNotificationText = String.format("CHECK! %s (%s) is in check.", opponentUsername, opponentColor);
            }

            if (stateNotificationText != null) {
                NotificationMessage stateNotification = new NotificationMessage(stateNotificationText);
                String stateNotificationJson = gson.toJson(stateNotification);
                broadcastMessage(stateNotificationJson, gameID, null); // Notify ALL
                System.out.println("Broadcast GAME STATE NOTIFICATION to all in game " + gameID + ": " + stateNotificationText);
            }

        } catch (InvalidMoveException e) {
            // Handle illegal chess move attempts
            System.out.println("Invalid move attempted by " + username + " in game " + gameID + ": " + e.getMessage());
            sendError(session, "Error: Invalid move - " + e.getMessage());
            // Do not proceed further

        } catch (DataAccessException e) {
            // Handle errors during database access (authentication, game load, game save)
            System.err.println("Data access error processing move for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Error processing move: A database error occurred. " + e.getMessage());
            // Do not proceed further

        } catch (IOException e) {
            // Catch IO exceptions specifically from sendError/broadcastMessage within the try
            System.err.println("IO error during make move processing/broadcast for game " + gameID + ": " + e.getMessage());
            // Session might be closed, often don't need to send another error

        } catch (Exception e) {
            // Catch any other unexpected errors during the process
            System.err.println("Unexpected error processing move for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            sendError(session, "An unexpected server error occurred while processing your move.");
        }
    }

    private void handleResign(Session session, UserGameCommand command) throws IOException, InvalidMoveException {
        // basic validation
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username = null;
        if (invalidTokenAndID(gameID, authToken, session)) {
            return;
        }

        try {
            AuthData authData = authDAO.getUser(authToken);
            if (authData == null) {
                sendError(session, "Error: invalid or expired auth.");
                return;
            }

            username = authData.username();

            GameData gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                sendError(session, "Error: Game ID " + gameID + "does not exist.");
                return;
            }

            gameService.resignGame(gameID, username);

            String notificationText = String.format("'%s' has resigned. The game is over.", username);
            NotificationMessage notification = new NotificationMessage(notificationText);
            String notificationJson = gson.toJson(notification);

            broadcastMessage(notificationJson, gameID, null);

        } catch (IllegalStateException e) {
            log.warn("IllegalStateException during resign for game {}, user '{}': {}", gameID, (username != null ? username : "[Auth Failed]"), e.getMessage());
            sendError(session, "Error: Cannot resign - " + e.getMessage());
        } catch (DataAccessException e) {
            log.error("DataAccessException during resign for game {}, user '{}': {}", gameID, (username != null ? username : "[Auth Failed]"), e.getMessage(), e);
            sendError(session, "Error resigning due to a data access issue.");
        } catch (IOException e) {
            log.error("IOException during resign processing/broadcast for game {}: {}", gameID, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during resign for game {}, user '{}': {}", gameID, (username != null ? username : "[Unknown]"), e.getMessage(), e);
            sendError(session, "An unexpected server error occurred while processing your resignation.");
        }
    }

    private void handleLeave(Session session, UserGameCommand command) throws IOException, InvalidMoveException {
        // basic validation with invalidTokenAndId function
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username = null;
        if (invalidTokenAndID(gameID, authToken, session)) {
            return;
        }

        // use a try catch block for potential errors
        try {
            // use authToken to get authData. and retrieve the user name.
            AuthData authData = authDAO.getUser(authToken);
            if (authData == null) {
                sendError(session, "Error: invalid or expired auth.");
                return;
            }

            // store the username
            username = authData.username();

            // fetch the game data
            GameData gameData = gameDAO.getGameByID(gameID);
            if (gameData == null) {
                sendError(session, "Error: Game ID " + gameID + "does not exist.");
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
            broadcastMessage(notificationJson, gameID, authToken);
            System.out.println("INFO [WSHandler - handleLeave]: Broadcasted leave notification to others in game " + gameID);

            System.out.println("SUCCESS [WSHandler - handleLeave]: User '" + username + "' successfully left game " + gameID + ".");

        } catch (InvalidMoveException e) { // Or IllegalStateException depending on your service
            System.err.println("WARN [WSHandler - handleLeave]: Invalid leave attempt by user '" + username + "' for game " + gameID + ": " + e.getMessage());
            sendError(session, "Error: Cannot leave game - " + e.getMessage());
        } catch (DataAccessException e) {
            System.err.println("ERROR [WSHandler - handleLeave]: Data access error processing leave for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace(System.err);
            sendError(session, "Error leaving game: Database error. " + e.getMessage());
        } catch (IOException e) {
            // Errors during WebSocket send/broadcast
            System.err.println("ERROR [WSHandler - handleLeave]: IOException during broadcast/send for user '" + username + "', game " + gameID + ": " + e.getMessage());
            // Don't try to sendError back if the connection is likely broken
        } catch (Exception e) { // Catch-all for unexpected issues
            System.err.println("ERROR [WSHandler - handleLeave]: Unexpected error processing leave for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace(System.err);
            try {
                // Try to inform the user if the session is still open
                sendError(session, "An unexpected server error occurred while processing your leave request.");
            } catch (IOException ioex) { /* If sending error fails, nothing more to do */ }
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
