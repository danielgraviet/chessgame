package websocket.messages;
// TODO start implementing the notifications to pass the 2nd test.
public class NotificationMessage extends ServerMessage {
    private String eventType;  // e.g., "JOIN_PLAYER", "MOVE", "CHECK"
    private String playerName; // Name of the player/observer involved
    private String teamColor;  // "WHITE", "BLACK", or null for observers
    private String moveDescription; // For move notifications, null otherwise

    public NotificationMessage(String eventType, String playerName, String teamColor, String moveDescription) {
        super(ServerMessageType.NOTIFICATION);
        this.eventType = eventType;
        this.playerName = playerName;
        this.teamColor = teamColor; // Null for observers or events not tied to a side
        this.moveDescription = moveDescription; // Null for non-move events
    }

    public String getEventType() {
        return eventType;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getTeamColor() {
        return teamColor;
    }

    public String getMoveDescription() {
        return moveDescription;
    }
}