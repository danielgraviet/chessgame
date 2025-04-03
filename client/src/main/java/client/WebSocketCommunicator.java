package client;

import javax.websocket.Endpoint;
import javax.websocket.Session;

// chess imports
import chess.ChessGame;
import chess.ChessPosition;
import chess.ChessPiece;
import com.google.gson.Gson;
import model.game.GameData;

import websocket.messages.*;

import javax.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import ui.GameHandlerUI;

public class WebSocketCommunicator extends Endpoint {
    Session session;
    private final GameHandlerUI uiHandler;
    private final Gson gson = new Gson();

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
            ServerMessage baseMessage = gson.fromJson(message, ServerMessage.class);
            System.out.println("DEBUG WS [RECV] << Type: " + baseMessage.getServerMessageType());

            switch (baseMessage.getServerMessageType()) {
                case NOTIFICATION:
                    NotificationMessage notif = gson.fromJson(message, NotificationMessage.class);
                    uiHandler.displayNotification(notif.getMessage());
                    break;
                case ERROR:
                    ErrorMessage error = gson.fromJson(message, ErrorMessage.class);
                    uiHandler.displayError("Server Error: " + error.getErrorMessage());
                    break;
                case LOAD_GAME:
                    LoadGameMessage loadGame = gson.fromJson(message, LoadGameMessage.class);
                    GameData gameData = loadGame.getGame();

                    // check game data
                    if (gameData != null && gameData.game() != null) {
                        ChessGame chessGame = gameData.game();

                        ChessPosition checkPos = new ChessPosition(3, 5); // test
                        ChessPiece pieceAtCheckPos = chessGame.getBoard().getPiece(checkPos);
                        String pieceStr = (pieceAtCheckPos != null) ? pieceAtCheckPos.toString() : "null";
                        System.out.println("DEBUG WS [RECV] << Parsed LOAD_GAME. Piece at " + checkPos + ": " + pieceStr);

                        uiHandler.updateBoard(chessGame);
                    } else {
                        // handlel cases where game data or the inner game is null
                        uiHandler.displayError("Error: Received incomplete game data from server.");
                    }
                    break;
                default:
                    // should not happen if server message types are handled
                    uiHandler.displayError("Error: Received unknown message type from server: " + baseMessage.getServerMessageType());
                    break;
            }
        } catch (Exception e) {
            //  potential Gson parsing errors or other issues
            uiHandler.displayError("Error processing server message: " + e.getMessage());
            // e.printStackTrace();
        } finally {
            // redraw the prompt after handling a message
            uiHandler.redrawPrompt();
        }
    }

    public void sendMessage(String jsonMessage) throws IOException {
        if (this.session != null && this.session.isOpen()) {
            try {
                this.session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                throw new IOException("Failed to send message via WebSocket: " + e.getMessage(), e);
            }
        } else {
            throw new IOException("Cannot send message: WebSocket session is not open.");
        }
    }

    public void close() {
        if (this.session != null && this.session.isOpen()) {
            try {
                this.session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,
                        "Client logging out or closing game"));
            } catch (IOException e) {
                System.err.println("Error closing WebSocket session: " + e.getMessage());
            } finally {
                this.session = null;
            }
        }
    }
}

