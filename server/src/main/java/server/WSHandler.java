package server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dataaccess.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import server.wshandlerfunctions.*;
import service.GameService;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

import java.io.IOException;

@WebSocket
public class WSHandler {
    private static final Gson GSON = new Gson();
    private static GameService gameService;
    private static AuthDAO authDAO = new SqlAuthDAO();
    private static GameDAO gameDAO = new SqlGameDAO();
    private static ConnectionManager connectionManager;
    private static HandleConnect handleConnect;
    private static HandleMakeMove handleMakeMove;
    private static HandleLeave handleLeave;
    private static HandleResign handleResign;
    private static HelperFunctions helperFunctions;

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

    public static void setHandleMakeMove(HandleMakeMove handleMakeMove) {
        WSHandler.handleMakeMove = handleMakeMove;
    }

    public static void setHandleLeave(HandleLeave handleLeave) {
        WSHandler.handleLeave = handleLeave;
    }

    public static void setHandleResign(HandleResign handleResign) {
        WSHandler.handleResign = handleResign;
    }

    public static void setHelperFunctions(HelperFunctions helperFunctions) { WSHandler.helperFunctions = helperFunctions; }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        if (session == null) {
            System.err.println("onConnect: Session is null");
            return;
        }
        System.out.println("INFO [WSHandler - onConnect]: WebSocket connection opened. Session ID: " +
                session.hashCode() + ", Remote: " + session.getRemoteAddress());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        if (session == null) {
            System.err.println("onMessage: Session is null");
            return;
        }

        System.out.println("Message received: " + message);

        try {
            if (helperFunctions == null || handleConnect == null || handleMakeMove == null || handleLeave == null || handleResign == null) {
                System.err.println("CRITICAL ERROR [WSHandler - onMessage]: Required handlers or helpers not injected!");
                // Cannot rely on helperFunctions.sendError if helperFunctions itself is null
                sendErrorLocal(session, "Server configuration error. Please try again later.");
                return;
            }
            UserGameCommand baseCommand;
            try {
                baseCommand = GSON.fromJson(message, UserGameCommand.class);
                if (baseCommand.getCommandType() == null) {
                    helperFunctions.sendError(session, "Error: Missing or invalid commandType field in message.");
                    return;
                }
            } catch (JsonSyntaxException e) {
                helperFunctions.sendError(session, "Error: Invalid command format - " + e.getMessage());
                return;
            }
            switch (baseCommand.getCommandType()) {
                case CONNECT:
                    handleConnect.handle(session, baseCommand);
                    break;
                case MAKE_MOVE:
                    try {
                        // Deserialize the SAME message string, but now into the specific subclass
                        MakeMoveCommand makeMoveCommand = GSON.fromJson(message, MakeMoveCommand.class);
                        // Basic check if move field deserialized correctly (might be null if JSON was wrong)
                        if (makeMoveCommand.getMove() == null) {
                            helperFunctions.sendError(session, "Error: Missing or invalid 'move' field for MAKE_MOVE command.");
                            return;
                        }
                        // Now pass the correctly typed object
                        handleMakeMove.handle(session, makeMoveCommand);
                    } catch (JsonSyntaxException e) {
                        System.err.println("Error deserializing MAKE_MOVE: " + e.getMessage());
                        helperFunctions.sendError(session, "Error: Invalid format for MAKE_MOVE command.");
                    }
                    break;
                case LEAVE:
                    handleLeave.handle(session, baseCommand);
                    break;
                case RESIGN:
                    handleResign.handle(session, baseCommand);
                    break;
                default:
                    System.err.println("Unknown command type received: " + baseCommand.getCommandType());
                    helperFunctions.sendError(session, "Error: Unknown command type '" + baseCommand.getCommandType() + "'");
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        } catch (Exception e) {
            // Catch unexpected errors during command processing
            System.err.println("Unexpected error processing message: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            try {
                helperFunctions.sendError(session, "An unexpected server error occurred.");
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

    private void sendErrorLocal(Session session, String message) {
        try {
            if (session != null && session.isOpen()) {
                ErrorMessage error = new ErrorMessage(message);
                session.getRemote().sendString(GSON.toJson(error));
            }
        } catch (IOException e) {
            System.err.println("ERROR [WSHandler - sendErrorLocal]: Failed to send error: " + e.getMessage());
        }
    }
}
