package client;

import javax.websocket.Endpoint;
import javax.websocket.Session;

// chess imports
import chess.ChessGame;
import com.google.gson.Gson;
import model.game.GameData;

import ui.PostLoginREPL;
import websocket.messages.*;

import javax.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import ui.GameHandlerUI;

public class WebSocketCommunicator extends Endpoint {
    Session session;
    private final GameHandlerUI uiHandler;

    public WebSocketCommunicator(String serverDomain, GameHandlerUI uiHandler) throws Exception {
        if (uiHandler == null) {
            throw new IllegalArgumentException("uiHandler cannot be null");
        }
        this.uiHandler = uiHandler;

        try  {
            URI uri = new URI("ws://" + serverDomain + "/ws");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, uri);

            this.session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }
            });
        } catch (DeploymentException | IOException | URISyntaxException ex) {
            throw new Exception("Failed to connect to WebSocket server at ws://" + serverDomain + "/ws. Error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {

    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        uiHandler.displayError("WebSocket connection closed: " + closeReason.getReasonPhrase());
    }

    @Override
    public void onError(Session session, Throwable thr) {
        uiHandler.displayError("WebSocket error: " + (thr.getMessage() != null ? thr.getMessage() : "Unknown error"));
    }

    private void handleMessage(String message) {
        System.out.println("DEBUG WS [RECV] << Raw: " + message);
        try {
            ServerMessage baseMessage = new Gson().fromJson(message, ServerMessage.class);

            switch (baseMessage.getServerMessageType()) {
                case NOTIFICATION:
                    NotificationMessage notif = new Gson().fromJson(message, NotificationMessage.class);
                    uiHandler.displayNotification(notif.getMessage());
                    break;
                case ERROR:
                    ErrorMessage error = new Gson().fromJson(message, ErrorMessage.class);
                    // Use getErrorMessage() as defined in ErrorMessage class
                    uiHandler.displayError("Server Error: " + error.getErrorMessage());
                    break;
                case LOAD_GAME:
                    LoadGameMessage loadGame = new Gson().fromJson(message, LoadGameMessage.class);
                    GameData gameData = loadGame.getGame(); // Get GameData object

                    // --- Extract ChessGame from GameData ---
                    if (gameData != null && gameData.game() != null) {
                        ChessGame chessGame = gameData.game(); // gameData.game() returns ChessGame
                        uiHandler.updateBoard(chessGame);
                    } else {
                        // Handle cases where game data or the inner game is null
                        uiHandler.displayError("Error: Received incomplete game data from server.");
                    }
                    break;
                default:
                    // Should not happen if server message types are handled correctly
                    uiHandler.displayError("Error: Received unknown message type from server: " + baseMessage.getServerMessageType());
                    break;
            }
        } catch (Exception e) {
            // Catch potential Gson parsing errors or other issues
            uiHandler.displayError("Error processing server message: " + e.getMessage());
            // e.printStackTrace(); // Log for debugging
        } finally {
            // Always redraw the prompt after handling a message
            uiHandler.redrawPrompt();
        }
    }

    public void sendMessage(String jsonMessage) throws IOException {
        if (this.session != null && this.session.isOpen()) {
            try {
                // Using basic remote: synchronous, throws IOException on failure
                this.session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                // Log or handle the send failure
                throw new IOException("Failed to send message via WebSocket: " + e.getMessage(), e);
            }
        } else {
            // Handle cases where the session is closed or not initialized
            throw new IOException("Cannot send message: WebSocket session is not open.");
        }
    }

    public void close() {
        if (this.session != null && this.session.isOpen()) {
            try {
                // Close the session with a normal closure reason
                this.session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client logging out or closing game"));
            } catch (IOException e) {
                // Log or handle error during closure
                System.err.println("Error closing WebSocket session: " + e.getMessage());
            } finally {
                this.session = null; // Nullify the session reference
            }
        }
    }
}

