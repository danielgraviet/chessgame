package websocket.messages;
// TODO start implementing the notifications to pass the 2nd test.
public class NotificationMessage extends ServerMessage {
    private String message;

    public NotificationMessage(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}