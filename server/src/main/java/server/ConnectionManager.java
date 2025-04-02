package server;

import org.eclipse.jetty.websocket.api.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Session>> connections = new ConcurrentHashMap<>();

    public void addConnection(int gameId, String authToken, Session session) {
        ConcurrentHashMap<String, Session> gameConnections = connections.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        gameConnections.put(authToken, session);

        String shortToken = authToken != null && authToken.length() > 8 ? authToken.substring(0, 8) + "..." : authToken;
        System.out.println("DEBUG [ConnectionManager]: Added connection for token " + shortToken + " to game " + gameId + " (Session: " + session.hashCode() + ")");
    }

    public void removeConnection(int gameId, String authToken) {
        String shortToken = authToken != null && authToken.length() > 8 ? authToken.substring(0, 8) + "..." : authToken;
        ConcurrentHashMap<String, Session> gameConnections = connections.get(gameId);
        if (gameConnections != null) {
            Session removedSession = gameConnections.remove(authToken);
            if (removedSession != null) {
                System.out.println("DEBUG [ConnectionManager]: Removed connection for token " + shortToken + " from game " + gameId + " (Session: " + removedSession.hashCode() + ")");
                if (gameConnections.isEmpty()) {
                    connections.remove(gameId);
                    System.out.println("DEBUG [ConnectionManager]: Game " + gameId + " connection map removed (empty).");
                }
            } else {
                System.out.println("DEBUG [ConnectionManager]: Attempted to remove token " + shortToken + " from game " + gameId + ", but it was not found.");
            }
        } else {
            System.out.println("DEBUG [ConnectionManager]: Attempted to remove connection from game " + gameId + " (token: " + shortToken + "), but the game has no connections registered.");
        }
    }

    public void removeSession(Session session) {
        System.out.println("DEBUG [ConnectionManager]: Removing session " + session.hashCode() + " from all games.");
        boolean removed = false;
        for (Map.Entry<Integer, ConcurrentHashMap<String, Session>> gameEntry : connections.entrySet()) {
            int gameId = gameEntry.getKey();
            ConcurrentHashMap<String, Session> gameConnections = gameEntry.getValue();

            String tokenToRemove = null;
            for (Map.Entry<String, Session> userEntry : gameConnections.entrySet()) {

                if (userEntry.getValue().equals(session)) {
                    tokenToRemove = userEntry.getKey();
                    break;
                }
            }


            if (tokenToRemove != null) {
                String shortToken = tokenToRemove.length() > 8 ? tokenToRemove.substring(0, 8) + "..." : tokenToRemove;
                if (gameConnections.remove(tokenToRemove) != null) {
                    removed = true;
                    System.out.println("DEBUG [ConnectionManager]: Removed session " + session.hashCode() + " (token: " + shortToken + ") from game " + gameId);
                    if (gameConnections.isEmpty()) {
                        connections.remove(gameId);
                        System.out.println("DEBUG [ConnectionManager]: Game " + gameId + " connection map removed (empty due to session removal).");
                    }
                }
            }
        }
        if (!removed) {
            System.out.println("DEBUG [ConnectionManager]: Session " + session.hashCode() + " was not found in any active game connections upon removal request.");
        }
    }

    public ConcurrentHashMap<String, Session> getConnectionsForGame(int gameId) {
        return connections.get(gameId);
    }
}
