package server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WSHandler {

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
        Server.sessions.put(session, 0);
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
            session.getRemote().sendString("Echo:" + message);
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
    }

}
