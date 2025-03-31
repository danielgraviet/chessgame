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
import service.GameService;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WSHandler {
    private static final Gson gson = new Gson();
    private static final Logger log = LoggerFactory.getLogger(WSHandler.class);
    private static GameService gameService;
    private static AuthDAO authDAO = new SqlAuthDAO();
    private static GameDAO gameDAO = new SqlGameDAO();

    public static void setGameService(GameService service) {
        gameService = service;
    }

    public static void setAuthDAO(AuthDAO dao) {
        authDAO = dao;
    }

    public static void setGameDAO(GameDAO dao) {
        gameDAO = dao;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        if (session == null) {
            System.err.println("onConnect: Session is null");
            return;
        }
        System.out.println("Client connected: " + session.getRemoteAddress().getAddress());
        if (Server.sessions == null) {
            System.err.println("onConnect: Server.sessions is null - reinitializing");
            Server.sessions = new ConcurrentHashMap<>();
        }
        System.out.println("Total sessions: " + Server.sessions.size());
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
                    handleConnect(session, baseCommand);
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
                    // handleLeave(session, command); // TODO: Implement later
                    sendError(session, "Command not yet implemented: LEAVE");
                    break;
                case RESIGN:
                    // handleResign(session, command); // TODO: Implement later
                    sendError(session, "Command not yet implemented: RESIGN");
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

        if (Server.sessions != null) {
            Server.sessions.remove(session);
        }
        System.out.println("Total sessions: " + (Server.sessions != null ? Server.sessions.size() : 0));
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String errorMessage = (error != null && error.getMessage() != null) ? error.getMessage() : "Unknown error";
        System.err.println("WebSocket Error: " + errorMessage);

        if (error != null) {
            error.printStackTrace();
        }

        if (session != null && Server.sessions != null) {
            Server.sessions.remove(session);
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

    private void broadcastMessage(String message, int gameID, Session excludedSession) {
        if (Server.sessions == null) {
            System.err.println("broadcastMessage: Server.sessions is null!");
            return;
        }

        List<Session> gameSessions = Server.sessions.get(gameID);
        if (gameSessions != null) {
            // create a copy for protect from unwanted changes to original
            List<Session> sessionsToSendTo;
            synchronized (gameSessions) {
                sessionsToSendTo = new ArrayList<>(gameSessions);
            }

            System.out.println("Broadcasting to " + (sessionsToSendTo.size() -1) + " clients in game " + gameID + " (excluding sender)");

            for (Session currentSession : sessionsToSendTo) {
                // prevent duplicate messages being sent.
                if (!currentSession.equals(excludedSession)) {
                    try {
                        sendMessage(currentSession, message);
                        System.out.println("Broadcast sent to " + currentSession.getRemoteAddress());
                    } catch (IOException e) {
                        System.err.println("IOException broadcasting to session " + currentSession.getRemoteAddress() + ": " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Unexpected error broadcasting to session " + currentSession.getRemoteAddress() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("No sessions found for game " + gameID + " to broadcast to.");
        }
    }

    private void handleConnect(Session session, UserGameCommand command) throws IOException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken() != null ? command.getAuthToken() : "anonymous";

        if (invalidTokenAndID(gameID, authToken, session)) {
            sendError(session, "Error: Missing or invalid authToken/gameID");
            return;
        }

        List<Session> gameSessions = Server.sessions.computeIfAbsent(gameID, k -> new ArrayList<>());

        ChessGame game = null;
        GameData gameData = null;
        AuthData authData = null;
        String username = "UnknownUser";
        String playerRole = "Observer";

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

            synchronized (gameSessions) {
                if (!gameSessions.contains(session)) {
                    gameSessions.add(session);
                    System.out.println("Session for user '" + username + "' added successfully to game " + gameID);
                } else {
                    System.out.println("Session for user '" + username + "' already exists in game " + gameID);
                }
            }


            if (gameData.whiteUsername() != null && username.equals(gameData.whiteUsername())) {
                playerRole = "WHITE";
                // is this just supposed to do on or the other? or both?
            } else if (gameData.blackUsername() != null && username.equals(gameData.blackUsername())) {
                playerRole = "BLACK";
            }
            System.out.println("User '" + username + "' assigned role: " + playerRole + " for game " + gameID);


            LoadGameMessage loadGameMessage = new LoadGameMessage(game);
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
            broadcastMessage(notificationJson, gameID, session);
            System.out.println("Broadcast JOIN NOTIFICATION to game " + gameID + ": user=" + username + ", role=" + playerRole);


        } catch (DataAccessException e) {
            System.err.println("Data access error during connect for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Error connecting to game due to data access issue: " + e.getMessage());


        } catch (IOException e) {
            System.err.println("IO error during connect/broadcast for game " + gameID + ", user '" + username + "': " + e.getMessage());
            synchronized(gameSessions) { gameSessions.remove(session); }

        } catch (Exception e) {
            System.err.println("Unexpected error during connect for game " + gameID + ", user '" + username + "': " + e.getMessage());
            e.printStackTrace();
            sendError(session, "An unexpected server error occurred while connecting.");
            synchronized(gameSessions) { gameSessions.remove(session); }
        }
    }

    // TODO: Implement handleMakeMove, handleLeave, handleResign methods
    private void handleMakeMove(Session session, MakeMoveCommand command) throws IOException, InvalidMoveException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();

        if (invalidTokenAndID(gameID, authToken, session)) {
            System.err.println("Error: No session list found for game " + gameID + " during makeMove.");
            sendError(session, "Error: Missing or invalid authToken/gameID");
            return;
        };

        List<Session> gameSessions = Server.sessions.get(gameID);
        if (gameSessions == null) {
            sendError(session, "Error: GameID: " + gameID + " has NULL sessions.");
        }

        // issue here, cannot be cast
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

            if (game.getTeamTurn() == null || game.isGameOver()) { // Add isGameOver() to your ChessGame if needed
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

            LoadGameMessage loadGameMessage = new LoadGameMessage(updatedGame);
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
            broadcastMessage(moveNotificationJson, gameID, session); // Exclude the player who made the move
            System.out.println("Broadcast MOVE NOTIFICATION to others in game " + gameID);


            ChessGame.TeamColor opponentColor = (playerColor == ChessGame.TeamColor.WHITE) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            String opponentUsername = (playerColor == ChessGame.TeamColor.WHITE) ? gameData.blackUsername() : gameData.whiteUsername();
            opponentUsername = (opponentUsername == null) ? "[Opponent]" : "'" + opponentUsername + "'"; // Handle case where opponent spot might be empty

            String stateNotificationText = null;
            if (updatedGame.isInCheckmate(opponentColor)) {
                stateNotificationText = String.format("CHECKMATE! %s (%s) defeated %s (%s).", username, playerColor, opponentUsername, opponentColor);
                // Optionally update game status in DB to reflect game over
            } else if (updatedGame.isInStalemate(opponentColor)) {
                stateNotificationText = "STALEMATE! The game is a draw.";
                // Optionally update game status in DB
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
    // These will follow a similar pattern: validate auth, get game data, perform action,
    // update game state (via GameService/DAO), and broadcast relevant messages (LOAD_GAME/NOTIFICATION).

}
