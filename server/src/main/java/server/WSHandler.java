package server;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dataaccess.DataAccessException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.*;
import websocket.responses.WebSocketResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WSHandler {
    private static final Gson gson = new Gson();
    private static GameService gameService;

    public static void setGameService(GameService service) {
        gameService = service;
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
            UserGameCommand command;
            try {
                command = gson.fromJson(message, UserGameCommand.class);
            } catch (JsonSyntaxException e) {
                sendMessage(session, "Error: Invalid command format - " + e.getMessage());
                return;
            }

            // Validate command fields
            if (command.getCommandType() == null) {
                sendMessage(session, "Error: Missing or invalid commandType");
                return;
            }
            if (command.getGameID() == null) {
                sendMessage(session, "Error: Missing or invalid gameID");
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT:
                    handleConnect(session, command);
                    break;
                default:
                    sendMessage(session, "Command not yet implemented: " + command.getCommandType());
            }
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
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

    private void handleConnect(Session session, UserGameCommand command) throws IOException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken() != null ? command.getAuthToken() : "anonymous";

        Server.sessions.computeIfAbsent(gameID, k -> new ArrayList<>()).add(session);

        // initialize game
        ChessGame game;
        try {
            game = Server.gameService.getGame(gameID);
            if (game == null) {
                sendError(session, "Game ID " + gameID + " does not exist");
                return;
            }
        } catch (DataAccessException e) {
            sendError(session, "Error retrieving game: " + e.getMessage());
            return;
        }

        LoadGameMessage loadGameMessage = new LoadGameMessage(game);
        String jsonResponse = gson.toJson(loadGameMessage);
        sendMessage(session, jsonResponse);
        System.out.println("Sent to client: " + jsonResponse);
    }
}
