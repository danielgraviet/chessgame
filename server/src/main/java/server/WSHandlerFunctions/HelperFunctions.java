package server.WSHandlerFunctions;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import server.ConnectionManager;
import websocket.messages.ErrorMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HelperFunctions {
    private final ConnectionManager connectionManager;
    private final Gson gson = new Gson();

    public HelperFunctions(ConnectionManager connectionManager) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("ConnectionManager cannot be null");
        }
        this.connectionManager = connectionManager;
    }

    public void sendMessage(Session session, String message) throws IOException {
        if (session != null && session.isOpen()) {
            session.getRemote().sendString(message);
        } else {
            System.err.println("WARN [HandleConnect]: Attempted to send message to null or closed session.");
        }
    }

    public void sendError(Session session, String errorMessage) throws IOException {
        ErrorMessage error = new ErrorMessage(errorMessage);
        String jsonResponse = gson.toJson(error);
        sendMessage(session, jsonResponse);
        System.out.println("INFO [HandleConnect]: Sent error to client: " + jsonResponse);
    }

    public void broadcastMessage(String message, int gameID, String excludedAuthToken) throws IOException {
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

    public boolean invalidTokenAndID(Integer gameID, String authToken, Session session) throws IOException {
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
